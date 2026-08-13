# AWS cost reduction phase 1: Valkey right-sizing

Status: scheduled on 2026-08-13; awaiting the ElastiCache maintenance window.

Execution record:

- Saved-plan SHA-256:
  `4A659484BCBB1AA705F484ABA7BCC2A11CFD3859B36DC06B7C2EC89410CC8BBC`
- Terraform result: one guardrail record added, one in-place Valkey update,
  zero destroyed.
- Both nodes report pending `CacheNodeType = cache.t4g.micro`.
- Maintenance window: Saturday 20:30–21:30 UTC (Sunday 02:00–03:00 IST).
- Immediate post-schedule checks: RDS available with no pending change, two
  healthy API targets in separate AZs, ECS desired/running counts matched, and
  public readiness returned HTTP 200.

Do not rerun or regenerate the plan while the node-level pending modification
exists. Revalidate the website and AWS health after the maintenance window.

## Scope

Phase 1 changes the production Valkey replication group from two
`cache.t3.small` nodes to two `cache.t4g.micro` nodes. It preserves the primary
and replica topology, automatic failover, Multi-AZ placement, encryption,
authentication, snapshots, RDS, ECS desired count, load balancer, frontend,
DNS, and networking.

The 2026-08-13 read-only baseline showed both cache nodes below 1% engine CPU,
below 2% database memory, and at zero evictions. It also showed two healthy API
targets in separate Availability Zones. Recollect these measurements before
approval because this evidence is time-sensitive.

## Hard safety gates

Do not apply unless all of the following are true:

1. RDS is `available`, Multi-AZ is enabled, PITR is current, and
   `PendingModifiedValues` is empty.
2. Both ECS API targets are healthy in different Availability Zones.
3. Both current Valkey nodes are available, automatic failover and Multi-AZ are
   enabled, memory is below 40%, and there were no recent evictions.
4. The application has been tested through a Valkey primary failover and
   reconnects without a user-visible error.
5. The saved Terraform plan changes only
   `aws_elasticache_replication_group.redis.node_type` in place. Any RDS, ECS,
   subnet, route, NAT, security-group, secret, KMS, S3, CloudFront, WAF, Route
   53, replacement, deletion, or task-count change rejects the plan.
6. A named operator is present for the entire change and observation window.

## Prepare and validate

Copy the reviewed private production variable file into
`infra/terraform/production.tfvars`. Never commit it. Then copy
`production.phase1-cost.tfvars.example` to
`production.phase1-cost.tfvars`; this file is also ignored by Git.

```powershell
terraform -chdir=infra/terraform init -reconfigure -backend-config=backend.production.hcl
terraform -chdir=infra/terraform fmt -check
terraform -chdir=infra/terraform validate
terraform -chdir=infra/terraform plan `
  -var-file=production.tfvars `
  -var-file=production.phase1-cost.tfvars `
  -out=production-phase1.tfplan
terraform -chdir=infra/terraform show production-phase1.tfplan
```

Do not use `-target`, and do not apply an unsaved or newly regenerated plan.
The plan is rejected unless its complete change summary satisfies safety gate
5. Keep `apply_immediately = false`; this prevents an unreviewed immediate node
replacement.

## Controlled execution

Execution requires a separate explicit approval after reviewing the saved
plan, current metrics, maintenance timing, and rollback operator. During the
change, continuously watch target health, API 5xx, latency, Redis connections,
CPU, memory, and evictions. Do not deploy application or database changes in
the same window.

Observe for at least 60 minutes after both nodes report available. Continue
daily observation for seven days before starting another cost phase.

## Rollback

Rollback uses the same reviewed production inputs with
`cache_node_type = "cache.t3.small"`. Generate and review a new saved plan;
never reuse the forward plan. Roll back if either node remains unhealthy, cache
memory exceeds 70%, evictions occur, backend 5xx or latency increases, or the
application does not reconnect cleanly.
