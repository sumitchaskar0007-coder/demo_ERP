# Backup, restore, and incident runbook

## Targets

Production targets are RPO 15 minutes (RDS point-in-time recovery) and RTO 60
minutes. These are objectives, not a substitute for a restore drill.

## RDS backup and point-in-time restore

The production Terraform owns the isolated RDS instance, subnet group,
security group, KMS encryption, backups, monitoring, and deletion protection.
Before a production release, capture `terraform plan` evidence and live AWS
evidence that the configured backup retention, encryption, Multi-AZ topology,
and recovery targets are active.

For a restore drill, an authorized operator restores to a *separate* instance
in the production private database subnets and attaches the production database
security group. Never overwrite or repoint the live database. Create temporary
runtime and migration secrets for the restored endpoint, run Flyway validation,
and execute read-only application smoke tests from a one-off ECS task. Record
the recovery point, start/end times, row counts, schema/Flyway checksums, and
test results. Delete the drill database and temporary secrets only after the
evidence is approved and the change record authorizes cleanup.

```sh
aws rds describe-db-instances --db-instance-identifier "$DB_INSTANCE" \
  --query 'DBInstances[0].{MultiAZ:MultiAZ,Encrypted:StorageEncrypted,Backup:BackupRetentionPeriod}'
aws rds describe-db-instances --db-instance-identifier "$RESTORED_INSTANCE" \
  --query 'DBInstances[0].DBInstanceStatus'
aws rds describe-db-instances --db-instance-identifier "$RESTORED_INSTANCE" \
  --query 'DBInstances[0].{SubnetGroup:DBSubnetGroup.DBSubnetGroupName,VpcSecurityGroups:VpcSecurityGroups[*].VpcSecurityGroupId,KmsKeyId:KmsKeyId}'
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
