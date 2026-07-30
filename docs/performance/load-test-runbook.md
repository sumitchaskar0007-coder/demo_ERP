# 5,000-user load-test runbook

## Safety gate

- Use an isolated staging environment and synthetic accounts.
- Disable external email, payments and real notifications.
- Confirm backups and cleanup ownership.
- Confirm CloudWatch alarms and an operator contact.
- Never run against production.
- Set `CONFIRM_NON_PRODUCTION=YES`; the script refuses to start otherwise.

## Data

Create unique synthetic accounts covering every role and multiple colleges. Never reuse production exports. Passwords belong in a git-ignored file. Validate tenant-isolation assertions before performance testing.

## Execution ladder

Run `load-tests/k6/5000-users.js` and review each plateau before continuing:

1. 500 concurrent users
2. 1,000 concurrent users
3. 2,500 concurrent users
4. 5,000 concurrent users

Use separate controlled egress addresses to represent campus NAT groups. Do not spoof `X-Forwarded-For`; the application intentionally trusts forwarding information only from known infrastructure proxies.

## Acceptance

- Read p95 < 1 second
- Write p95 < 2 seconds
- Error rate < 1%
- No incorrect legitimate 429 response
- ECS CPU < 70%
- RDS CPU and connections < 70%
- No Hikari timeout
- No ECS restart
- No Redis eviction
- No cross-tenant result
- No failed or duplicate background job

Stop immediately if errors rise sharply, database integrity is uncertain, a safety alarm fires, or synthetic operations reach an external recipient.

## Evidence

Archive the k6 summary, exact image digest/task definition, Terraform plan, dataset size, CloudWatch dashboard export, RDS Performance Insights snapshot and all threshold results.
