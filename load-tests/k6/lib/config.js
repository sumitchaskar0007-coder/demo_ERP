const PROFILE_STAGES = Object.freeze({
  smoke: [
    { duration: "15s", target: 5 },
    { duration: "30s", target: 5 },
    { duration: "15s", target: 0 },
  ],
  500: [
    { duration: "5m", target: 500 },
    { duration: "10m", target: 500 },
    { duration: "5m", target: 0 },
  ],
  1000: [
    { duration: "5m", target: 1000 },
    { duration: "10m", target: 1000 },
    { duration: "5m", target: 0 },
  ],
  2500: [
    { duration: "10m", target: 2500 },
    { duration: "15m", target: 2500 },
    { duration: "10m", target: 0 },
  ],
  5000: [
    { duration: "15m", target: 5000 },
    { duration: "20m", target: 5000 },
    { duration: "10m", target: 0 },
  ],
  ladder: [
    { duration: "5m", target: 500 },
    { duration: "10m", target: 500 },
    { duration: "5m", target: 1000 },
    { duration: "10m", target: 1000 },
    { duration: "10m", target: 2500 },
    { duration: "15m", target: 2500 },
    { duration: "15m", target: 5000 },
    { duration: "20m", target: 5000 },
    { duration: "10m", target: 0 },
  ],
});

const KNOWN_BLOCKED_HOSTS = Object.freeze([
  "jadhavaredu.com",
  "www.jadhavaredu.com",
  "api.jadhavaredu.com",
  "ddsxcqz27a5q8.cloudfront.net",
  "jadhavr-erp-staging-414296957.ap-south-1.elb.amazonaws.com",
  "3.82.155.92",
  "3.108.219.13",
]);

function envFlag(name) {
  return (__ENV[name] || "").trim().toUpperCase() === "YES";
}

function envInteger(name, fallback, minimum, maximum) {
  const raw = (__ENV[name] || "").trim();
  if (!raw) return fallback;
  const value = Number.parseInt(raw, 10);
  if (!Number.isInteger(value) || value < minimum || value > maximum) {
    throw new Error(`${name} must be an integer from ${minimum} to ${maximum}`);
  }
  return value;
}

function hostnameFrom(url) {
  const match = /^https?:\/\/([^/:?#]+)(?::\d+)?(?:[/?#]|$)/i.exec(url);
  return match ? match[1].toLowerCase() : "";
}

function maxTarget(stages) {
  return stages.reduce((maximum, stage) => Math.max(maximum, stage.target), 0);
}

const profile = (__ENV.PROFILE || "smoke").trim().toLowerCase();
if (!Object.prototype.hasOwnProperty.call(PROFILE_STAGES, profile)) {
  throw new Error(
    `Unknown PROFILE '${profile}'. Use smoke, 500, 1000, 2500, 5000, or ladder.`,
  );
}

const stages = PROFILE_STAGES[profile];
const baseUrl = (__ENV.BASE_URL || "").trim().replace(/\/+$/, "");
const expectedHost = (__ENV.EXPECTED_HOST || "").trim().toLowerCase();
const targetEnvironment = (__ENV.TARGET_ENVIRONMENT || "").trim().toLowerCase();

export const SETTINGS = Object.freeze({
  profile,
  stages,
  maxVus: maxTarget(stages),
  baseUrl,
  hostname: hostnameFrom(baseUrl),
  expectedHost,
  targetEnvironment,
  confirmation: (__ENV.CONFIRM_NON_PRODUCTION || "").trim(),
  runId: (__ENV.RUN_ID || "").trim(),
  usersFile: (__ENV.USERS_FILE || "./users.example.json").trim(),
  syntheticEmailSuffix: (__ENV.SYNTHETIC_EMAIL_SUFFIX || "example.invalid")
    .trim()
    .toLowerCase()
    .replace(/^@/, ""),
  safetyProbePath: (
    __ENV.SAFETY_PROBE_PATH || "/actuator/health/readiness"
  ).trim(),
  safetyExpectedText: (__ENV.SAFETY_PROBE_EXPECTED_TEXT || "").trim(),
  safetyHeaderName: (__ENV.SAFETY_ENV_HEADER_NAME || "").trim(),
  safetyHeaderValue: (__ENV.SAFETY_ENV_HEADER_VALUE || "").trim(),
  csrfPath: (__ENV.CSRF_PATH || "/api/v1/auth/csrf").trim(),
  csrfCookieName: (__ENV.CSRF_COOKIE_NAME || "XSRF-TOKEN").trim(),
  csrfHeaderName: (__ENV.CSRF_HEADER_NAME || "X-XSRF-TOKEN").trim(),
  enableAttendanceWrites: envFlag("ENABLE_ATTENDANCE_WRITES"),
  enableReports: envFlag("ENABLE_REPORTS"),
  enableFileTransfers: envFlag("ENABLE_FILE_TRANSFERS"),
  enableTokenExpiry: envFlag("ENABLE_TOKEN_EXPIRY"),
  attendanceWritePercent: envInteger("ATTENDANCE_WRITE_PERCENT", 5, 1, 100),
  reportPercent: envInteger("REPORT_VU_PERCENT", 1, 1, 100),
  fileTransferPercent: envInteger("FILE_TRANSFER_VU_PERCENT", 1, 1, 100),
  tokenExpiryPercent: envInteger("TOKEN_EXPIRY_VU_PERCENT", 1, 1, 100),
  thinkTimeMinSeconds: envInteger("THINK_TIME_MIN_SECONDS", 1, 0, 60),
  thinkTimeMaxSeconds: envInteger("THINK_TIME_MAX_SECONDS", 5, 0, 120),
  blockedHosts: Object.freeze([
    ...KNOWN_BLOCKED_HOSTS,
    ...(__ENV.PRODUCTION_HOST_DENYLIST || "")
      .split(",")
      .map((host) => host.trim().toLowerCase())
      .filter(Boolean),
  ]),
});

export const K6_OPTIONS = Object.freeze({
  stages,
  gracefulRampDown: "30s",
  discardResponseBodies: true,
  noConnectionReuse: false,
  batch: 10,
  batchPerHost: 6,
  userAgent: `jadhavr-erp-k6/${profile}`,
  setupTimeout: "2m",
  teardownTimeout: "1m",
  summaryTrendStats: ["avg", "min", "med", "p(90)", "p(95)", "p(99)", "max"],
  systemTags: ["status", "method", "url", "name", "group", "scenario"],
  thresholds: {
    checks: ["rate>0.99"],
    http_req_failed: ["rate<0.01"],
    read_api_duration: ["p(95)<1000"],
    write_api_duration: ["p(95)<2000"],
    legitimate_429: ["rate==0"],
    tenant_isolation_violations: ["count==0"],
    background_job_failures: ["count==0"],
    file_integrity_failures: ["count==0"],
  },
});

function requestExists(account, name) {
  return Boolean(account && account.journeys && account.journeys[name]);
}

function containsSubmitTrue(value) {
  if (value === null || value === undefined) return false;
  if (Array.isArray(value)) return value.some(containsSubmitTrue);
  if (typeof value !== "object") return false;
  if (value.submit === true) return true;
  return Object.values(value).some(containsSubmitTrue);
}

function validateOptionalJourneys(accounts) {
  const attendanceWrites = accounts
    .map((account) => account.journeys && account.journeys.attendanceWrite)
    .filter(Boolean);
  const reports = accounts
    .map((account) => account.journeys && account.journeys.report)
    .filter(Boolean);
  const files = accounts
    .map((account) => account.journeys && account.journeys.fileTransfer)
    .filter(Boolean);
  const expiries = accounts
    .map((account) => account.journeys && account.journeys.tokenExpiry)
    .filter(Boolean);

  if (SETTINGS.enableAttendanceWrites) {
    if (attendanceWrites.length === 0) {
      throw new Error(
        "ENABLE_ATTENDANCE_WRITES=YES requires at least one attendanceWrite fixture",
      );
    }
    attendanceWrites.forEach((write) => {
      if (write.safe !== true || write.mode !== "draft-upsert") {
        throw new Error(
          "Every enabled attendanceWrite must set safe=true and mode=draft-upsert",
        );
      }
      if (containsSubmitTrue(write.request && write.request.body)) {
        throw new Error(
          "Attendance load tests must not submit or lock attendance",
        );
      }
    });
  }

  if (SETTINGS.enableReports) {
    if (reports.length === 0)
      throw new Error("ENABLE_REPORTS=YES requires a report fixture");
    reports.forEach((report) => {
      if (
        report.safe !== true ||
        !report.initiate ||
        !report.statusPathTemplate ||
        (!report.downloadUrlJsonPath &&
          !report.downloadRequest &&
          !report.downloadPathTemplate)
      ) {
        throw new Error(
          "Every enabled report must be safe and define initiation, status, and private download authorization",
        );
      }
    });
  }

  if (SETTINGS.enableFileTransfers) {
    if (files.length === 0)
      throw new Error(
        "ENABLE_FILE_TRANSFERS=YES requires a fileTransfer fixture",
      );
    files.forEach((file) => {
      if (
        file.safe !== true ||
        !file.initiate ||
        !file.uploadUrlJsonPath ||
        !file.uploadIdJsonPath ||
        !file.requiredHeadersJsonPath ||
        !file.complete ||
        (!file.downloadUrlJsonPath && !file.downloadRequest)
      ) {
        throw new Error(
          "Every enabled fileTransfer must define signed upload headers, completion, and private download authorization",
        );
      }
    });
  }

  if (SETTINGS.enableTokenExpiry) {
    if (SETTINGS.profile !== "smoke") {
      throw new Error(
        "Token-expiry checks are allowed only with PROFILE=smoke",
      );
    }
    if (expiries.length === 0) {
      throw new Error(
        "ENABLE_TOKEN_EXPIRY=YES requires at least one tokenExpiry fixture",
      );
    }
  }
}

export function validateSafety(accounts) {
  if (!/^https?:\/\/[^/?#]+$/i.test(SETTINGS.baseUrl) || !SETTINGS.hostname) {
    throw new Error(
      "BASE_URL must be an explicit origin-only http(s) URL; there is no default target",
    );
  }
  if (!SETTINGS.expectedHost || SETTINGS.expectedHost !== SETTINGS.hostname) {
    throw new Error(
      "EXPECTED_HOST must exactly equal the hostname parsed from BASE_URL",
    );
  }
  if (SETTINGS.blockedHosts.includes(SETTINGS.hostname)) {
    throw new Error(
      `Refusing known production/public host '${SETTINGS.hostname}'`,
    );
  }
  if (
    !["isolated-staging", "load-test", "local"].includes(
      SETTINGS.targetEnvironment,
    )
  ) {
    throw new Error(
      "TARGET_ENVIRONMENT must be isolated-staging, load-test, or local",
    );
  }
  if (
    SETTINGS.targetEnvironment !== "local" &&
    !SETTINGS.baseUrl.toLowerCase().startsWith("https://")
  ) {
    throw new Error("Non-local load tests require HTTPS");
  }
  if (
    SETTINGS.confirmation !== "YES_I_HAVE_VERIFIED_THIS_IS_ISOLATED_STAGING"
  ) {
    throw new Error(
      "Set the full CONFIRM_NON_PRODUCTION safety confirmation after verification",
    );
  }
  if (!/^[a-z0-9][a-z0-9._-]{5,80}$/i.test(SETTINGS.runId)) {
    throw new Error("RUN_ID must be a unique 6-81 character identifier");
  }
  if (
    !SETTINGS.safetyProbePath.startsWith("/") ||
    SETTINGS.safetyProbePath.startsWith("//")
  ) {
    throw new Error(
      "SAFETY_PROBE_PATH must be a relative path beginning with one slash",
    );
  }
  if (!Array.isArray(accounts) || accounts.length < SETTINGS.maxVus) {
    throw new Error(
      `USERS_FILE requires at least ${SETTINGS.maxVus} unique synthetic accounts for PROFILE=${SETTINGS.profile}`,
    );
  }

  const seen = new Set();
  accounts.forEach((account, index) => {
    const email = String(account && account.email ? account.email : "")
      .trim()
      .toLowerCase();
    const password = String(
      account && account.password ? account.password : "",
    );
    if (!email.endsWith(`@${SETTINGS.syntheticEmailSuffix}`)) {
      throw new Error(
        `Synthetic account ${index + 1} does not use @${SETTINGS.syntheticEmailSuffix}`,
      );
    }
    if (!password || /^replace|^changeme|^password$/i.test(password)) {
      throw new Error(
        `Synthetic account ${index + 1} still has a placeholder password`,
      );
    }
    if (seen.has(email))
      throw new Error(`Duplicate synthetic account '${email}'`);
    seen.add(email);

    const isolation = account.journeys && account.journeys.tenantIsolationProbe;
    if (
      isolation &&
      (isolation.tenantIsolationNegative !== true ||
        account.crossTenantStudentId === undefined)
    ) {
      throw new Error(
        `Synthetic account ${index + 1} has an invalid tenant-isolation probe`,
      );
    }
  });

  [
    "dashboard",
    "unread",
    "studentSearch",
    "attendanceRead",
    "feeRead",
    "tenantIsolationProbe",
  ].forEach((journey) => {
    if (!accounts.some((account) => requestExists(account, journey))) {
      throw new Error(
        `USERS_FILE has no coverage for required '${journey}' journey`,
      );
    }
  });

  validateOptionalJourneys(accounts);
}

export function shouldSample(percent) {
  return (__VU + __ITER) % 100 < percent;
}
