import crypto from "k6/crypto";
import { group, sleep } from "k6";
import { SETTINGS, shouldSample } from "./config.js";
import {
  backgroundJobFailures,
  backendRequest,
  externalRequest,
  fileIntegrityFailures,
  jsonPath,
  renderTemplate,
  responseJson,
} from "./http.js";

function think(
  minimum = SETTINGS.thinkTimeMinSeconds,
  maximum = SETTINGS.thinkTimeMaxSeconds,
) {
  const low = Math.min(minimum, maximum);
  const high = Math.max(minimum, maximum);
  sleep(low + Math.random() * (high - low));
}

function idempotencyKey(context, type) {
  return `${context.variables.RUN_ID}-${type}-${__VU}-${__ITER}`;
}

export function contextFor(account) {
  return {
    variables: {
      RUN_ID: SETTINGS.runId,
      VU: __VU,
      ITER: __ITER,
      TODAY: new Date().toISOString().slice(0, 10),
      EXPECTED_TENANT_ID:
        account.expectedTenantId === undefined ? "" : account.expectedTenantId,
      CROSS_TENANT_STUDENT_ID:
        account.crossTenantStudentId === undefined
          ? ""
          : account.crossTenantStudentId,
    },
  };
}

export function login(account, context) {
  return group("01 login", () =>
    backendRequest(
      account,
      {
        method: "POST",
        path:
          (account.journeys.login && account.journeys.login.path) ||
          "/api/v1/auth/login",
        body: { email: account.email, password: account.password },
        expectedStatuses: [200],
        captureBody: false,
      },
      context,
      "write",
      "login",
      false,
    ),
  );
}

export function coreReads(account, context) {
  const journeys = account.journeys || {};
  const ordered = [
    ["dashboard", journeys.dashboard],
    ["unread", journeys.unread],
    ["student-search", journeys.studentSearch],
    ["attendance-read", journeys.attendanceRead],
    ["fee-read", journeys.feeRead],
    ["tenant-isolation-negative", journeys.tenantIsolationProbe],
  ];

  ordered.forEach(([name, spec]) => {
    if (!spec) return;
    think();
    group(name, () =>
      backendRequest(account, spec, context, "read", name, false),
    );
  });
}

export function attendanceWrite(account, context) {
  const write = account.journeys && account.journeys.attendanceWrite;
  if (
    !SETTINGS.enableAttendanceWrites ||
    !write ||
    !shouldSample(SETTINGS.attendanceWritePercent)
  ) {
    return;
  }

  think();
  group("attendance-safe-write", () => {
    const request = {
      ...write.request,
      headers: {
        ...(write.request.headers || {}),
        "Idempotency-Key": idempotencyKey(context, "attendance"),
      },
    };
    backendRequest(
      account,
      request,
      context,
      "write",
      "attendance-safe-write",
      true,
    );
  });
}

function reportFailure(reason) {
  backgroundJobFailures.add(1, { reason });
}

export function asyncReport(account, context) {
  const report = account.journeys && account.journeys.report;
  if (
    !SETTINGS.enableReports ||
    !report ||
    !shouldSample(SETTINGS.reportPercent)
  )
    return;

  think();
  group("async-report", () => {
    const initiation = backendRequest(
      account,
      {
        ...report.initiate,
        captureBody: true,
        headers: {
          ...(report.initiate.headers || {}),
          "Idempotency-Key": idempotencyKey(context, "report"),
        },
      },
      context,
      "write",
      "report-initiate",
      true,
    );
    const jobId = jsonPath(
      responseJson(initiation),
      report.jobIdJsonPath || "data.id",
    );
    if (jobId === undefined || jobId === null || jobId === "") {
      reportFailure("missing_job_id");
      return;
    }

    const successful = report.successStatuses || ["COMPLETED", "SUCCEEDED"];
    const failed = report.failureStatuses || ["FAILED", "CANCELLED", "EXPIRED"];
    const maximumPolls = Math.min(Number(report.maxPolls || 20), 60);
    const pollSeconds = Math.max(Number(report.pollSeconds || 2), 1);
    let statusPayload;
    let completed = false;

    for (let attempt = 0; attempt < maximumPolls; attempt += 1) {
      sleep(pollSeconds);
      const statusPath = renderTemplate(report.statusPathTemplate, {
        ...context.variables,
        JOB_ID: jobId,
      });
      const statusResponse = backendRequest(
        account,
        {
          method: "GET",
          path: statusPath,
          expectedStatuses: [200],
          captureBody: true,
        },
        context,
        "read",
        "report-status",
        false,
      );
      statusPayload = responseJson(statusResponse);
      const status = String(
        jsonPath(statusPayload, report.statusJsonPath || "data.status") || "",
      ).toUpperCase();
      if (failed.includes(status)) {
        reportFailure(status.toLowerCase());
        return;
      }
      if (successful.includes(status)) {
        completed = true;
        break;
      }
    }

    if (!completed) {
      reportFailure("poll_timeout");
      return;
    }

    const statusDownloadUrl = jsonPath(
      statusPayload,
      report.downloadUrlJsonPath || "data.downloadUrl",
    );
    if (statusDownloadUrl) {
      externalRequest({
        url: String(statusDownloadUrl),
        expected: [200],
        journey: "report-download",
        kind: "read",
      });
      return;
    }

    const downloadRequest = report.downloadRequest || {};
    const downloadPathTemplate =
      downloadRequest.path || report.downloadPathTemplate;
    if (downloadPathTemplate) {
      const path = renderTemplate(downloadPathTemplate, {
        ...context.variables,
        JOB_ID: jobId,
      });
      const authorization = backendRequest(
        account,
        {
          ...downloadRequest,
          method: downloadRequest.method || "GET",
          path,
          expectedStatuses: [200],
          captureBody: true,
        },
        context,
        "read",
        "report-download-authorize",
        false,
      );
      const authorizedUrl = jsonPath(
        responseJson(authorization),
        report.authorizedDownloadUrlJsonPath || "data.url",
      );
      if (!authorizedUrl) {
        reportFailure("missing_download_url");
        return;
      }
      externalRequest({
        url: String(authorizedUrl),
        expected: [200],
        journey: "report-download",
        kind: "read",
      });
      return;
    }

    reportFailure("missing_download_contract");
  });
}

function resolveDownloadUrl(
  file,
  initiationPayload,
  completionPayload,
  account,
  context,
) {
  const direct = file.downloadUrlJsonPath
    ? jsonPath(completionPayload, file.downloadUrlJsonPath) ||
      jsonPath(initiationPayload, file.downloadUrlJsonPath)
    : "";
  if (direct) return String(direct);
  if (!file.downloadRequest) return "";

  const response = backendRequest(
    account,
    { ...file.downloadRequest, captureBody: true },
    context,
    "read",
    "presigned-download-authorize",
    false,
  );
  return String(
    jsonPath(
      responseJson(response),
      file.downloadUrlJsonPath || "data.downloadUrl",
    ) || "",
  );
}

export function presignedTransfer(account, context, payload) {
  const file = account.journeys && account.journeys.fileTransfer;
  if (
    !SETTINGS.enableFileTransfers ||
    !file ||
    !shouldSample(SETTINGS.fileTransferPercent)
  ) {
    return;
  }

  think();
  group("presigned-file-transfer", () => {
    const checksum = crypto.sha256(payload, "hex");
    const fileVariables = {
      ...context.variables,
      FILE_SHA256: checksum,
      FILE_SIZE: payload.byteLength,
      CONTENT_TYPE: file.contentType || "application/pdf",
    };
    const fileContext = { variables: fileVariables };
    const initiation = backendRequest(
      account,
      {
        ...file.initiate,
        captureBody: true,
        headers: {
          ...(file.initiate.headers || {}),
          "Idempotency-Key": idempotencyKey(context, "file"),
        },
      },
      fileContext,
      "write",
      "presigned-upload-authorize",
      true,
    );
    const initiationPayload = responseJson(initiation);
    const uploadUrl = jsonPath(initiationPayload, file.uploadUrlJsonPath);
    if (!uploadUrl) {
      fileIntegrityFailures.add(1, { reason: "missing_upload_url" });
      return;
    }

    const signedHeaders = {};
    const requiredHeaders = jsonPath(
      initiationPayload,
      file.requiredHeadersJsonPath || "data.requiredHeaders",
    );
    if (
      !requiredHeaders ||
      typeof requiredHeaders !== "object" ||
      Array.isArray(requiredHeaders)
    ) {
      fileIntegrityFailures.add(1, { reason: "missing_signed_headers" });
      return;
    }
    let signedHeadersValid = true;
    Object.entries(requiredHeaders).forEach(([name, values]) => {
      if (!name || !Array.isArray(values) || values.length === 0) {
        fileIntegrityFailures.add(1, { reason: "invalid_signed_headers" });
        signedHeadersValid = false;
        return;
      }
      signedHeaders[name] = values.join(",");
    });
    if (!signedHeadersValid) return;

    const upload = externalRequest({
      url: String(uploadUrl),
      method: file.uploadMethod || "PUT",
      body: payload,
      headers: {
        ...(file.uploadHeaders || {}),
        ...signedHeaders,
      },
      expected: file.uploadExpectedStatuses || [200, 204],
      journey: "presigned-upload",
      kind: "write",
    });
    if (!(file.uploadExpectedStatuses || [200, 204]).includes(upload.status)) {
      fileIntegrityFailures.add(1, { reason: "upload_failed" });
      return;
    }

    let completionPayload;
    const objectKey = jsonPath(
      initiationPayload,
      file.objectKeyJsonPath || "data.objectKey",
    );
    const uploadId = jsonPath(
      initiationPayload,
      file.uploadIdJsonPath || "data.uploadId",
    );
    const objectContext = {
      variables: {
        ...fileVariables,
        OBJECT_KEY: objectKey || "",
        UPLOAD_ID: uploadId || "",
      },
    };
    if (file.complete) {
      const completion = backendRequest(
        account,
        {
          ...file.complete,
          captureBody: true,
          headers: {
            ...(file.complete.headers || {}),
            "Idempotency-Key": idempotencyKey(context, "file-complete"),
          },
        },
        objectContext,
        "write",
        "presigned-upload-complete",
        true,
      );
      completionPayload = responseJson(completion);
    }

    const downloadUrl = resolveDownloadUrl(
      file,
      initiationPayload,
      completionPayload,
      account,
      objectContext,
    );
    if (!downloadUrl) {
      fileIntegrityFailures.add(1, { reason: "missing_download_url" });
      return;
    }
    const download = externalRequest({
      url: downloadUrl,
      expected: [200],
      journey: "presigned-download",
      kind: "read",
      responseType: "binary",
    });
    if (!download.body || crypto.sha256(download.body, "hex") !== checksum) {
      fileIntegrityFailures.add(1, { reason: "checksum_mismatch" });
    }
  });
}

export function tokenExpiry(account, context) {
  const expiry = account.journeys && account.journeys.tokenExpiry;
  if (
    !SETTINGS.enableTokenExpiry ||
    !expiry ||
    !shouldSample(SETTINGS.tokenExpiryPercent)
  ) {
    return;
  }

  group("token-expiry", () => {
    sleep(Math.max(Number(expiry.waitSeconds || 1), 1));
    backendRequest(
      account,
      {
        method: "GET",
        path: expiry.probePath || "/api/v1/auth/me",
        expectedStatuses: expiry.expectedStatuses || [401],
        captureBody: false,
      },
      context,
      "read",
      "token-expiry",
      false,
    );
  });
}

export function logout(account, context) {
  think(0, 2);
  group("99 logout", () =>
    backendRequest(
      account,
      {
        method: "POST",
        path:
          (account.journeys.logout && account.journeys.logout.path) ||
          "/api/v1/auth/logout",
        expectedStatuses: [200, 204],
        captureBody: false,
      },
      context,
      "write",
      "logout",
      true,
    ),
  );
}
