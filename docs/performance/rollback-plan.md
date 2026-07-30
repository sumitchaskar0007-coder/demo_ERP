# Performance-hardening rollback plan

Status date: 2026-07-29

This plan is a review artifact, not authorization to modify AWS or production data.

ECS/Fargate behind an ALB is the authoritative production target.
`infra/terraform-ec2` is retained only as a legacy review path; its single
instance is non-HA and is not a rollback target for a 5,000-user release.

## Preconditions before any staging change

- Record the immutable backend image digest, ECS task-definition revision and frontend artifact version.
- Export the reviewed Terraform plan and current state reference without secrets.
- Record current autoscaling, WAF, alarms and queue configuration.
- Confirm database migration compatibility and backup ownership.
- Confirm the external RDS owner and their independent rollback procedure.
- Confirm who can stop the load generators and who can approve rollback.
- Do not use the account root user for deployment or rollback.

## Load-test abort

1. Gracefully stop k6 so threshold and timing summaries are retained.
2. Stop scheduled scaling and test-only generators using the approved change identity.
3. Disable write/report/file feature flags at the test runner.
4. Confirm no producer is adding email/report work.
5. Preserve logs, queue messages and failed synthetic objects.
6. Record the stop reason and metric timestamp.
7. Clean only run-owned synthetic data after evidence is captured.

Never purge a DLQ, delete shared data or clear observability evidence as part of an emergency stop.

## Backend application rollback

1. Let the ECS deployment circuit breaker perform its configured automatic rollback when health checks fail.
2. For a functional regression, deploy the previously recorded immutable digest/task revision.
3. Wait for readiness targets to become healthy in every availability zone.
4. Verify login, authorization, tenant isolation, DB/cache health and queue consumption.
5. Keep additive database schema changes unless the migration owner approves a separately tested reversal.

The ECS workflow traps normal shell errors and termination signals and verifies
the prior task revision. A runner hard-kill can bypass those traps. Before a
retry, use read-only ECS inspection to reconcile the API, worker and any
reported migration task ARN. A migration still running after the workflow's
30-minute reconciliation window must be allowed/handled by the RDS owner; do
not change/retry the commit-and-approval tuple until it is reconciled. The
deployment role intentionally has no `ecs:StopTask`.

Do not redirect production to either standalone EC2 instance as an emergency
rollback. If a future approved architecture decision introduces EC2, it must
first provide equivalent immutable artifacts, health-gated multi-instance
replacement and tested rollback.

## Frontend rollback

1. Restore the previous versioned frontend artifact.
2. Invalidate only the required CloudFront paths.
3. Verify login/refresh/logout compatibility with the rolled-back backend.
4. Confirm the service worker/browser cache, if introduced later, does not retain the failed release.

## Database and migrations

- Prefer backward-compatible expand/contract migrations.
- Do not drop new columns or indexes during incident rollback merely to match older code.
- Stop application writers before any exceptional data repair.
- The application team must not resize, restore or modify the externally owned production RDS instance.
- Before an approved external RDS class/parameter change, the RDS owner records:
  - original class and parameter group;
  - snapshot/PITR position;
  - expected maintenance interruption;
  - rollback class and compatibility;
  - connection drain procedure; and
  - decision deadline.

Flyway migration failure blocks deployment. Repair or reversal must be reviewed against a clone, not improvised on production.

## Valkey and authorization cache

1. Disable authorization snapshot reads with the documented feature flag if cache behavior is unsafe.
2. Retain PostgreSQL as the fail-closed source of truth.
3. Invalidate affected user/session keys.
4. Do not flush the entire shared cache unless separately approved.
5. Preserve distributed rate limiting unless it is the demonstrated cause; use a reviewed emergency threshold instead of removing protection.

## SSE and notice delivery

- Disable the SSE feature and return clients to unread-count-only fallback polling.
- Keep fallback at 60–120 seconds and pause it in hidden tabs.
- Do not re-enable 15-second full-inbox polling.
- Verify connection cleanup after rollback.

## WAF rollback

1. Preserve the pre-change Web ACL JSON/lock token and Terraform state reference.
2. Revert only the reviewed scoped auth rule.
3. Keep IP-reputation and known-bad-input protections.
4. Monitor blocked/allowed login traffic and application 429 outcomes.
5. Do not replace a bad rate rule with no brute-force protection.

## Autoscaling rollback

- Disable the new scheduled actions.
- Restore the last known minimum/maximum and target-tracking configuration.
- Keep at least two healthy tasks across availability zones.
- Ensure the restored maximum cannot exceed the external database connection budget.
- Watch scale-in stabilization, deregistration and in-flight requests.

The point-in-time staging baseline was minimum 2, maximum 6, CPU target 60% and memory target 70%.

## Email/report queue rollback

1. Stop producers before workers when a job format or side effect is unsafe.
2. Let in-flight idempotent work reach a defined boundary.
3. Preserve queue and DLQ messages.
4. Roll the dedicated async ECS worker and API service back to compatible task
   definitions built from the same immutable image digest.
5. Keep worker database polling enabled when it is required to recover durable
   stale/retry report jobs; disable it only with an explicit reconciliation
   procedure.
6. Reprocess only after the cause, idempotency key and recipient safety are verified.
7. Do not fall back to synchronous SES/report generation in important request threads.

## Presigned-transfer rollback

- Disable new presign issuance.
- Allow already issued short-lived URLs to expire.
- Keep incomplete objects quarantined and unavailable.
- Preserve upload metadata for reconciliation.
- Delete only run-owned synthetic objects through the approved cleanup job.
- Return to the previous private transfer path only if it is compatible and within its safe size limit.

## Malware protection rollback

- GuardDuty Malware Protection for S3 is disabled by default and must remain
  disabled until the application clean/threat promotion state machine is
  deployed and tested.
- If a later approved rollout emits incorrect or missing results, stop new
  presign issuance, keep pending objects unavailable and preserve EventBridge,
  GuardDuty and application evidence.
- Disabling the scanner does not make pending objects safe; reconcile every
  upload state and delete only approved run-owned synthetic objects.
- Do not make the bucket public or bypass tenant/checksum controls to restore
  availability.

## Infrastructure-loss response

The prior production stack was deleted by a root console session. If deletion activity recurs:

1. stop all Terraform applies;
2. end/revoke the uncontrolled session;
3. capture CloudTrail evidence;
4. inventory remaining resources;
5. create a fresh plan rather than applying a stale saved plan; and
6. obtain explicit approval before recreation.

Never recreate resources repeatedly while an active actor is deleting them.

## Rollback completion

Rollback is complete only when health checks pass, synthetic functional tests succeed, queues are accounted for, no tenant/integrity issue remains, observability is restored and the incident/change owner records the final state.
