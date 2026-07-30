# Jadhavr ERP controlled k6 suite

This suite models realistic authenticated activity while refusing known production/public endpoints. It has no default target. All data-changing journeys are disabled by default.

Read `docs/performance/load-test-runbook.md` before using it.

## Requirements

- k6 0.49 or later;
- an approved isolated staging/load-test target;
- one unique synthetic account per peak VU;
- production-sized synthetic data;
- multiple load generators for high stages; and
- working application/AWS telemetry and an alert recipient.

Do not commit populated user files or passwords.

## Minimum invocation

Populate a synthetic fixture outside version control, then run:

```bash
k6 run \
  -e PROFILE=smoke \
  -e BASE_URL=https://isolated-loadtest.example.invalid \
  -e EXPECTED_HOST=isolated-loadtest.example.invalid \
  -e TARGET_ENVIRONMENT=isolated-staging \
  -e CONFIRM_NON_PRODUCTION=YES_I_HAVE_VERIFIED_THIS_IS_ISOLATED_STAGING \
  -e RUN_ID=loadtest-20260728-001 \
  -e USERS_FILE=./users.synthetic.json \
  -e SYNTHETIC_EMAIL_SUFFIX=example.invalid \
  5000-users.js
```

The test fails before load begins when:

- the hostname is known production/public infrastructure;
- `EXPECTED_HOST` differs from `BASE_URL`;
- the confirmation or target-environment label is missing;
- the readiness safety probe does not return 200;
- accounts are duplicated, non-synthetic or fewer than peak VUs; or
- required read journeys have no fixture coverage.

An optional environment marker can strengthen the probe:

```bash
-e SAFETY_ENV_HEADER_NAME=X-Jadhavr-Environment \
-e SAFETY_ENV_HEADER_VALUE=isolated-load-test
```

## Profiles

Use `smoke`, `500`, `1000`, `2500`, `5000` or `ladder`. Run the numeric profiles separately and review each gate. The suite enforces at least as many unique fixture accounts as peak VUs.

The `ladder` profile performs the complete 500 → 1,000 → 2,500 → 5,000 progression and is intended only for a final approved rehearsal.

## User fixture

Each entry contains credentials for one synthetic account and role-appropriate request specifications:

```json
{
  "email": "principal-load-0001@example.invalid",
  "password": "SYNTHETIC_SECRET_FROM_TEMPORARY_FILE",
  "role": "PRINCIPAL",
  "expectedTenantId": 900001,
  "journeys": {
    "dashboard": {
      "path": "/api/v1/auth/me",
      "expectedStatuses": [200],
      "captureBody": true,
      "tenantJsonPath": "data.collegeId"
    },
    "unread": {
      "path": "/api/notices/unread-count",
      "expectedStatuses": [200]
    },
    "studentSearch": {
      "path": "/api/principal/students/search?keyword=load&page=0&size=20",
      "expectedStatuses": [200]
    },
    "attendanceRead": {
      "path": "/api/principal/attendance/dashboard",
      "expectedStatuses": [200]
    },
    "feeRead": {
      "path": "/api/principal/fees/collections?page=0&size=20",
      "expectedStatuses": [200]
    },
    "tenantIsolationProbe": {
      "path": "/api/principal/students/${CROSS_TENANT_STUDENT_ID}/details",
      "expectedStatuses": [403, 404],
      "tenantIsolationNegative": true
    }
  }
}
```

String paths are also accepted for simple reads. Across the complete file, at least one fixture must cover dashboard, unread, student search, attendance read, fee read and a negative tenant-isolation probe. The probe must set `tenantIsolationNegative: true`. `crossTenantStudentId` must identify an existing synthetic record owned by a different synthetic college; using a nonexistent ID is not valid isolation evidence.

## Safe attendance draft writes

Writes require `ENABLE_ATTENDANCE_WRITES=YES` and a fixture explicitly marked as a safe draft upsert:

```json
{
  "attendanceWrite": {
    "safe": true,
    "mode": "draft-upsert",
    "request": {
      "method": "PUT",
      "path": "/api/load-test/attendance/${RUN_ID}/${VU}",
      "body": {
        "runId": "${RUN_ID}",
        "virtualUser": "${VU}",
        "iteration": "${ITER}",
        "submit": false
      },
      "expectedStatuses": [200, 201]
    }
  }
}
```

The runner adds a per-run/VU/iteration `Idempotency-Key` and rejects `submit: true`. The endpoint itself must be staging-only, idempotent and scoped to run-owned synthetic records.

## Async report contract

`ENABLE_REPORTS=YES` activates only fixtures with `safe: true`. This example
matches the implemented student CSV export:

```json
{
  "report": {
    "safe": true,
    "initiate": {
      "method": "POST",
      "path": "/api/reports/students/jobs",
      "body": {},
      "expectedStatuses": [200, 202]
    },
    "jobIdJsonPath": "data.id",
    "statusPathTemplate": "/api/reports/jobs/${JOB_ID}",
    "statusJsonPath": "data.status",
    "downloadRequest": {
      "method": "GET",
      "path": "/api/reports/jobs/${JOB_ID}/download"
    },
    "authorizedDownloadUrlJsonPath": "data.url",
    "successStatuses": ["COMPLETED"],
    "failureStatuses": ["FAILED", "CANCELLED", "EXPIRED"],
    "pollSeconds": 2,
    "maxPolls": 20
  }
}
```

Use only an authorized role and synthetic tenant scope. The worker and SQS
transport must be enabled in the isolated environment before this journey.

## Presigned transfer contract

`ENABLE_FILE_TRANSFERS=YES` requires a safe fixture with an authorization
request and JSON paths for the private URLs. This student example matches the
implemented admission-document flow:

```json
{
  "fileTransfer": {
    "safe": true,
    "contentType": "application/pdf",
    "initiate": {
      "method": "POST",
      "path": "/api/student/admissions/me/documents/GAP_CERTIFICATE/presign",
      "body": {
        "originalFilename": "k6-${RUN_ID}-${VU}-${ITER}.pdf",
        "fileSize": "${FILE_SIZE}",
        "sha256": "${FILE_SHA256}",
        "contentType": "${CONTENT_TYPE}"
      },
      "expectedStatuses": [200]
    },
    "uploadUrlJsonPath": "data.uploadUrl",
    "uploadIdJsonPath": "data.uploadId",
    "requiredHeadersJsonPath": "data.requiredHeaders",
    "complete": {
      "method": "POST",
      "path": "/api/student/admissions/me/documents/GAP_CERTIFICATE/complete",
      "body": {
        "uploadId": "${UPLOAD_ID}"
      },
      "expectedStatuses": [200]
    },
    "downloadRequest": {
      "method": "GET",
      "path": "/api/student/admissions/me/documents/GAP_CERTIFICATE/download-url"
    },
    "downloadUrlJsonPath": "data.downloadUrl"
  }
}
```

The runner uploads the harmless synthetic PDF using the exact server-signed S3
headers, completes the upload, downloads it through a short-lived URL and
verifies SHA-256 equality. Use one synthetic student per VU; this journey
replaces only that account's synthetic `GAP_CERTIFICATE`.

## Token expiry

Token expiry is allowed only with `PROFILE=smoke` and `ENABLE_TOKEN_EXPIRY=YES`:

```json
{
  "tokenExpiry": {
    "waitSeconds": 65,
    "probePath": "/api/v1/auth/me",
    "expectedStatuses": [401]
  }
}
```

Use a dedicated short-lived-token staging configuration. Waiting for normal production TTLs is not a capacity test.

## Evidence export

Use k6’s summary export and a time-series output appropriate to the approved test:

```bash
k6 run --summary-export ./artifacts/loadtest-20260728-001-summary.json 5000-users.js
```

Keep generated artifacts and populated fixtures out of version control.
