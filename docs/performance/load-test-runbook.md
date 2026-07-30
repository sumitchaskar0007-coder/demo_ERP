# Controlled 5,000-user load-test runbook

This runbook applies to `load-tests/k6`. It is intentionally designed to refuse known production endpoints and to keep every write journey disabled by default.

Execution status as of 2026-07-29: **not run**. The k6 binary is not installed
in the audited local environment, no isolated target has been approved and no
5,000-user synthetic account set exists. Creating scripts is not capacity
evidence.

The clean PostgreSQL 16 migration harness has applied all 20 migrations through
`V25` and passed Hibernate schema validation locally. That does not satisfy the
prerequisite below: the approved isolated target must still use the
RDS-owner-reviewed migration and exact release configuration.

## Absolute safety rules

- Never target production.
- Do not target `jadhavaredu.com`, `www.jadhavaredu.com`, `api.jadhavaredu.com`, the current public CloudFront distribution, the current staging ALB or either standalone public EC2 address.
- Use an isolated staging/load-test environment with synthetic data only.
- Use one unique synthetic account per peak virtual user.
- Disable external email, real payments, real notifications and third-party side effects.
- Do not spoof `X-Forwarded-For`. Model campus NAT through controlled generator egress addresses.
- Keep attendance writes, async reports, file transfers and token-expiry checks disabled until their individual gates are approved.
- Stop immediately on an integrity, tenant-isolation, external-side-effect or safety-alarm concern.

The script additionally requires an exact expected hostname, an isolated-environment label, a run ID and a long-form confirmation string.

## Prerequisites

1. Obtain explicit approval for the isolated staging test and its AWS spend.
2. Record target ownership, region, VPC, database, cache and deployment digest.
3. Confirm the target has no production data or integrations.
4. Populate at least 5,000 unique synthetic accounts for the full test.
5. Distribute roles and colleges realistically; include multiple campus/NAT groups.
6. Seed production-sized synthetic student, attendance, fee, notice and report data.
7. Confirm migrations, backups and cleanup ownership.
8. Confirm CloudWatch dashboard retention and a working alert subscription.
9. Size multiple load generators so generator CPU, memory, sockets and bandwidth remain below 70%.
10. Synchronize clocks and record the test window.

Passwords must be supplied in a git-ignored file or an approved temporary secret mount. Never commit the populated user file.

## Preflight

From `load-tests/k6`:

```bash
cp users.example.json users.synthetic.json
```

Populate only synthetic accounts, then configure environment values from `.env.example`. The minimum safety values are:

```bash
export BASE_URL="https://isolated-loadtest.example.invalid"
export EXPECTED_HOST="isolated-loadtest.example.invalid"
export TARGET_ENVIRONMENT="isolated-staging"
export CONFIRM_NON_PRODUCTION="YES_I_HAVE_VERIFIED_THIS_IS_ISOLATED_STAGING"
export RUN_ID="loadtest-20260728-001"
export USERS_FILE="./users.synthetic.json"
export SYNTHETIC_EMAIL_SUFFIX="example.invalid"
```

Run a functional smoke test first:

```bash
k6 run -e PROFILE=smoke 5000-users.js
```

The setup phase fails if the hostname is a known public/production endpoint, does not exactly match `EXPECTED_HOST`, the environment label is unsafe, accounts are duplicated, emails do not use the synthetic suffix, or there are fewer unique users than peak VUs.

## Journey coverage

The account fixture configures role-appropriate paths. Across the population, the suite covers:

- login and CSRF cookie behavior;
- dashboard/authenticated profile;
- unread-count retrieval;
- paged student search;
- a negative cross-tenant student-detail probe using an existing synthetic record;
- attendance read;
- fee read;
- optional idempotent attendance draft write;
- optional async report initiation, status polling and result download;
- optional presigned upload, completion and checksum-verified download;
- logout; and
- optional access-token expiry behavior.

Async CSV report and admission-document presigned-transfer endpoints now exist
in the local working tree, with backend/frontend tests. They are not deployed.
Their journeys remain configuration-gated and must not be enabled until:

- the exact local API contract is deployed to isolated staging;
- Flyway `V23` and `V25` are applied by the approved migration owner;
- the private S3/CORS/KMS, SQS, worker and tenant-isolation contracts are
  verified;
- fixtures use the real deployed endpoint paths and response JSON paths; and
- cleanup, recipient suppression and job/object ownership are assigned.

The async report implementation currently covers CSV jobs for admissions,
fees, attendance and students. It is not evidence that all PDF/Excel report
workflows are asynchronous.

## Progressive execution gates

Run each profile separately. Archive and review results before starting the next profile:

```bash
k6 run -e PROFILE=500 5000-users.js
k6 run -e PROFILE=1000 5000-users.js
k6 run -e PROFILE=2500 5000-users.js
k6 run -e PROFILE=5000 5000-users.js
```

The `ladder` profile exists for a final controlled rehearsal, but it must not replace the separate approval gates.

| Profile |       Ramp |       Hold |  Ramp down | Entry condition                 |
| ------- | ---------: | ---------: | ---------: | ------------------------------- |
| 500     |  5 minutes | 10 minutes |  5 minutes | Smoke and functional tests pass |
| 1,000   |  5 minutes | 10 minutes |  5 minutes | 500-user evidence accepted      |
| 2,500   | 10 minutes | 15 minutes | 10 minutes | 1,000-user evidence accepted    |
| 5,000   | 15 minutes | 20 minutes | 10 minutes | 2,500-user evidence accepted    |

Use distributed execution for high stages. Partition unique users between generators; do not let two generators share the same account.

## Optional write gates

Attendance writes require all of the following:

```bash
export ENABLE_ATTENDANCE_WRITES="YES"
```

- an account-specific `attendanceWrite` request;
- `safe: true`;
- `mode: "draft-upsert"`;
- a synthetic lecture/record owned by the run; and
- an endpoint that honors the generated idempotency key.

Never configure `submit: true`, correction of shared records, payment writes or a destructive path.

Reports require `ENABLE_REPORTS=YES`, a deployed safe async job contract and a
worker that is isolated from the API service. File transfers require
`ENABLE_FILE_TRANSFERS=YES`, a deployed private tenant-bound object contract and
a cleanup owner. Keep both flags off merely because the source implementation
exists locally. Token-expiry checks require `ENABLE_TOKEN_EXPIRY=YES` and
should run only in the smoke profile because deliberate waiting distorts
capacity stages.

## Acceptance criteria

All of these must pass:

- read API p95 below 1 second;
- write API p95 below 2 seconds;
- request failure rate below 1%;
- check success rate above 99%;
- zero incorrect legitimate 429 responses;
- zero tenant-isolation violations;
- zero failed/duplicate background jobs;
- zero uploaded/downloaded checksum mismatches;
- ECS CPU below 70%;
- ECS memory below the approved safety threshold;
- RDS CPU and connections below 70% of the approved budget;
- no Hikari timeout or sustained pending connection;
- no ECS task restart or failed deployment;
- no Valkey eviction;
- no cross-tenant data;
- no real email, payment, message or notification; and
- load generators below 70% CPU/memory/network/socket capacity.

Passing k6 thresholds alone is insufficient; AWS and application evidence must agree.

## Stop conditions

Abort the current test and do not advance when:

- errors rise sharply or remain at/above 1%;
- read/write p95 breaches persist for two observation windows;
- any safety, billing, tenant or data-integrity alarm fires;
- RDS or ECS exceeds the approved safety envelope;
- Hikari timeouts, task restarts or Valkey evictions occur;
- a background job fails or duplicates;
- a request reaches a real external recipient; or
- a generator saturates.

Use `Ctrl-C` once for a graceful k6 stop so summaries are emitted, then scale test-only generators down under the approved procedure.

## Evidence package

Archive:

- run ID, operator, approval and timestamps;
- k6 version, command, profile and summary export;
- sanitized fixture counts and role/college/NAT distribution;
- backend image digest, task definition and frontend version;
- Terraform plan/state version without secrets;
- CloudWatch dashboard export;
- ALB, ECS, JVM, Hikari, RDS, Valkey, SQS and SES metrics;
- RDS Performance Insights snapshot;
- generator health;
- failed-check samples without credentials or personal data;
- cleanup confirmation; and
- go/no-go decision.

Repeat the final 5,000-user run on the unchanged release candidate. Capacity may be claimed only after both runs pass.
