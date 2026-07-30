import http from "k6/http";
import { check } from "k6";
import { Counter, Rate, Trend } from "k6/metrics";
import { SETTINGS } from "./config.js";

export const readApiDuration = new Trend("read_api_duration", true);
export const writeApiDuration = new Trend("write_api_duration", true);
export const legitimate429 = new Rate("legitimate_429");
export const tenantIsolationViolations = new Counter(
  "tenant_isolation_violations",
);
export const backgroundJobFailures = new Counter("background_job_failures");
export const fileIntegrityFailures = new Counter("file_integrity_failures");
const responseCallbacks = new Map();

export function seedSafetyMetrics() {
  tenantIsolationViolations.add(0);
  backgroundJobFailures.add(0);
  fileIntegrityFailures.add(0);
}

export function jsonPath(value, path) {
  if (!path) return value;
  const parts = path
    .replace(/\[(\d+)\]/g, ".$1")
    .split(".")
    .filter(Boolean);
  let current = value;
  for (const part of parts) {
    if (current === null || current === undefined) return undefined;
    current = current[part];
  }
  return current;
}

export function responseJson(response) {
  if (!response || !response.body) return undefined;
  try {
    return response.json();
  } catch (_error) {
    return undefined;
  }
}

export function renderTemplate(value, variables) {
  if (Array.isArray(value))
    return value.map((item) => renderTemplate(item, variables));
  if (value && typeof value === "object") {
    return Object.fromEntries(
      Object.entries(value).map(([key, item]) => [
        key,
        renderTemplate(item, variables),
      ]),
    );
  }
  if (typeof value !== "string") return value;

  const exact = /^\$\{([A-Z0-9_]+)\}$/.exec(value);
  if (exact && Object.prototype.hasOwnProperty.call(variables, exact[1])) {
    return variables[exact[1]];
  }
  return value.replace(/\$\{([A-Z0-9_]+)\}/g, (_match, name) => {
    if (!Object.prototype.hasOwnProperty.call(variables, name)) {
      throw new Error(`Fixture references unknown template variable ${name}`);
    }
    return String(variables[name]);
  });
}

function expectedStatuses(spec) {
  const configured = spec && spec.expectedStatuses;
  return Array.isArray(configured) && configured.length > 0
    ? configured
    : [200];
}

function responseCallbackFor(statuses) {
  const key = statuses.join(",");
  if (!responseCallbacks.has(key)) {
    responseCallbacks.set(key, http.expectedStatuses(...statuses));
  }
  return responseCallbacks.get(key);
}

function backendUrl(path) {
  if (typeof path !== "string" || !path.startsWith("/") || /^\/\//.test(path)) {
    throw new Error(
      `Backend request path must start with one slash: '${path}'`,
    );
  }
  return `${SETTINGS.baseUrl}${path}`;
}

function bodyFor(spec, variables) {
  if (spec.body === undefined || spec.body === null) return null;
  const rendered = renderTemplate(spec.body, variables);
  return typeof rendered === "string" ? rendered : JSON.stringify(rendered);
}

function responseParams(spec, journey, kind, headers) {
  const statuses = expectedStatuses(spec);
  return {
    headers,
    redirects: spec.redirects === undefined ? 0 : spec.redirects,
    responseType: spec.captureBody ? spec.responseType || "text" : "none",
    responseCallback: responseCallbackFor(statuses),
    tags: {
      journey,
      kind,
      name: journey,
      ...(spec.tags || {}),
    },
  };
}

function record(response, spec, journey, kind) {
  const statuses = expectedStatuses(spec);
  const accepted = statuses.includes(response.status);
  (kind === "write" ? writeApiDuration : readApiDuration).add(
    response.timings.duration,
    {
      journey,
    },
  );
  legitimate429.add(response.status === 429 && !statuses.includes(429), {
    journey,
  });
  check(response, {
    [`${journey}: expected status`]: () => accepted,
  });
  return accepted;
}

function verifyTenant(response, account, spec, journey) {
  if (spec.tenantIsolationNegative === true) {
    const denied = expectedStatuses(spec).includes(response.status);
    if (!denied) tenantIsolationViolations.add(1, { journey });
    return;
  }
  if (!spec.tenantJsonPath || account.expectedTenantId === undefined) return;
  const actual = jsonPath(responseJson(response), spec.tenantJsonPath);
  const matches = String(actual) === String(account.expectedTenantId);
  check(response, {
    [`${journey}: tenant marker matches fixture`]: () => matches,
  });
  if (!matches) tenantIsolationViolations.add(1, { journey });
}

export function csrfHeaders(account, context) {
  backendRequest(
    account,
    {
      method: "GET",
      path: SETTINGS.csrfPath,
      expectedStatuses: [200],
      captureBody: false,
    },
    context,
    "read",
    "csrf",
    false,
  );
  const cookies = http.cookieJar().cookiesForURL(SETTINGS.baseUrl);
  const values = cookies[SETTINGS.csrfCookieName];
  if (!values || values.length === 0) return {};
  return {
    [SETTINGS.csrfHeaderName]: decodeURIComponent(values[0]),
  };
}

export function backendRequest(
  account,
  rawSpec,
  context,
  kind,
  journey,
  includeCsrf = false,
) {
  const spec = typeof rawSpec === "string" ? { path: rawSpec } : { ...rawSpec };
  const method = String(spec.method || "GET").toUpperCase();
  const variables = {
    ...context.variables,
    EMAIL: account.email,
    ROLE: account.role || "",
  };
  const path = renderTemplate(spec.path, variables);
  const renderedHeaders = renderTemplate(spec.headers || {}, variables);
  const headers = {
    Accept: spec.accept || "application/json",
    ...(spec.body !== undefined && spec.contentType !== null
      ? { "Content-Type": spec.contentType || "application/json" }
      : {}),
    ...renderedHeaders,
    ...(includeCsrf ? csrfHeaders(account, context) : {}),
  };
  const response = http.request(
    method,
    backendUrl(path),
    bodyFor(spec, variables),
    responseParams(spec, journey, kind, headers),
  );
  record(response, spec, journey, kind);
  verifyTenant(response, account, spec, journey);
  return response;
}

export function externalRequest({
  url,
  method = "GET",
  body = null,
  headers = {},
  expected = [200],
  journey,
  kind,
  responseType = "none",
}) {
  if (!/^https:\/\//i.test(url))
    throw new Error(`${journey} requires an HTTPS presigned URL`);
  const spec = { expectedStatuses: expected };
  const response = http.request(method, url, body, {
    headers,
    redirects: 0,
    responseType,
    responseCallback: responseCallbackFor(expected),
    tags: { journey, kind, name: journey, external: "true" },
  });
  record(response, spec, journey, kind);
  return response;
}
