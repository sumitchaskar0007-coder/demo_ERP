# Backup, restore, and incident runbook

## Targets

Production targets are RPO 15 minutes (RDS point-in-time recovery) and RTO 60
minutes. These are objectives, not a substitute for a restore drill.

## RDS backup and point-in-time restore

Terraform enables encrypted Multi-AZ RDS, 14 days of automated backups,
performance insights, and deletion protection. Verify those settings after
each infrastructure change. For a restore drill, restore to a *separate*
instance in private subnets, use a temporary Secrets Manager entry, run the
Flyway validation task, and execute read-only application smoke tests. Record
the restore timestamp, duration, and row/checksum verification, then destroy
the drill instance according to the change ticket.

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

- **Migration failure:** stop the service deployment, preserve logs, identify
  the failed version, and restore/fix data before retrying the one-time task.
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
