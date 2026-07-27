# Backup, restore, and incident runbook

## Targets

Production targets are RPO 15 minutes (RDS point-in-time recovery) and RTO 60
minutes. These are objectives, not a substitute for a restore drill.

## RDS backup and point-in-time restore

The production RDS owner controls encryption, topology, backups, monitoring,
deletion protection, restore drills, and Flyway validation. This application
Terraform neither creates nor modifies those controls. Before a production
release, obtain evidence that the agreed backup retention and recovery targets
are active.

For a restore drill, the RDS owner restores to a *separate* instance in private
subnets, supplies temporary runtime and migration secret ARNs, and completes
their Flyway validation. The application team then executes read-only smoke
tests. Record the restore timestamp, duration, and row/checksum verification;
the RDS owner removes the drill instance according to the change ticket.

```sh
aws rds describe-db-instances --db-instance-identifier "$DB_INSTANCE" \
  --query 'DBInstances[0].{MultiAZ:MultiAZ,Encrypted:StorageEncrypted,Backup:BackupRetentionPeriod}'
aws rds describe-db-instances --db-instance-identifier "$RESTORED_INSTANCE" \
  --query 'DBInstances[0].DBInstanceStatus'
```

## Upload recovery

The uploads bucket is private, encrypted, versioned, and protected from public
access. Recover an accidentally deleted object by selecting the prior version
ID and copying it to the generated tenant-scoped key. Never make the bucket or
object public; access remains through the authorized application endpoint or a
short-lived presigned URL.

## Failure procedures

- **Migration failure:** stop the service deployment, preserve logs, and notify
  the RDS owner. Do not retry or alter production Flyway state without their
  approved recovery plan.
- **Redis outage:** rate limiting and distributed session features fail closed
  in production. Restore the Valkey replication group or fail over; do not
  enable an unauthenticated local cache as a workaround.
- **S3 outage:** return a safe temporary error, retain database metadata, and
  retry after service recovery. Do not fall back to container-local storage.
- **Compromised account:** revoke refresh tokens/sessions, rotate the affected
  secret, review security audit events and WAF logs, and force a password reset.
- **Suspicious bulk export:** stop the export job, preserve audit evidence,
  revoke the actor's sessions, and review tenant/role authorization.
- **Secret rotation:** write the new value to Secrets Manager, deploy a new
  task revision, verify health and mail/database connectivity, then revoke the
  old value. JWT and email-encryption keys are separate; retain only the
  documented previous encryption key during its rotation window.
