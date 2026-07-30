# 5,000-user readiness audit

Date: 2026-07-28
Status: **not 5,000-user ready; staging load test has not run**

## Verified baseline

AWS identity was verified before inspection:

- Account: `814645955631`
- Region: `ap-south-1`
- Active identity: account root user (critical governance finding; replace with an IAM Identity Center/role session before any change)
- ECS service: `jadhavr-erp-staging-backend`, two running Fargate tasks
- Task size: 1 vCPU / 2 GiB
- Live autoscaling at audit time: minimum 2, maximum 6, CPU 60%, memory 70%
- RDS: PostgreSQL 17.5, `db.t4g.medium`, Multi-AZ, private, encrypted gp3, Performance Insights enabled
- Valkey: two `cache.t4g.small` nodes, Multi-AZ, automatic failover, transit/at-rest encryption
- CloudFront: deployed with HTTP/2 and AWS WAF
- SES: sandbox, 200 messages/day and 1 message/second
- CloudWatch: nine infrastructure alarms; no SNS subscription

No secret values were read.

## Material risks

| Priority | Finding | Expected failure |
|---|---|---|
| P0 | API rate limiting was source-IP based | Campus NAT users receive incorrect 429 responses |
| P0 | JWT filter loaded the user from PostgreSQL on every request | Database connection pressure and elevated latency |
| P0 | Each dashboard requested the full notice inbox every 15 seconds | About 333 requests/second at 5,000 open dashboards |
| P0 | Six tasks expose only 120 default Hikari connections | Pool waits and 30-second timeouts under DB-heavy load |
| P0 | SES remains sandboxed | Account and password emails throttle or fail |
| P1 | `db.t4g.medium` is burstable | CPU-credit depletion and inconsistent database latency |
| P1 | Multiple services call unbounded `findAll()` and filter in Java | Heap growth, table scans and tenant-scale latency |
| P1 | Upload/download paths buffer complete files in the web task | Memory pressure during concurrent file traffic |
| P1 | Reports run in request threads | Thread exhaustion during export bursts |
| P1 | No controlled load-test suite existed | Capacity claims could not be validated |
| P1 | Alarms had no subscriber | Failures would not notify an operator |
| P2 | Most mutable business records lacked optimistic locking | Lost updates during concurrent edits |

## Repository versus live AWS

- The live service was manually configured for 2–6 task autoscaling, while the committed baseline had no application-autoscaling resources.
- Terraform now proposes 2–12 tasks and optional scheduled pre-scaling, but this has not been applied.
- Terraform proposes SQS email/report queues; no queues existed at audit time.
- Terraform proposes a campus-safe WAF edge threshold; live WAF remains unchanged.
- The live backend revision contains manually deployed fixes that must be reconciled with source before the next deployment.

## Strengths already present

- Private Multi-AZ RDS with encryption, backups, deletion protection and Performance Insights
- Private encrypted Multi-AZ Valkey
- Two ECS tasks behind ALB with deployment rollback
- CloudFront, private frontend S3 origin and WAF
- Private upload bucket and tenant-shaped keys
- Redis-backed shared application rate-limit primitive
- Flyway migrations and production schema validation

## Verification boundary

Static inspection and read-only AWS inspection do not prove capacity. Readiness requires an isolated staging environment, production-sized synthetic data, successful 500/1,000/2,500/5,000-user tests, CloudWatch correlation and acceptance-criteria sign-off.
