resource "aws_db_subnet_group" "main" {
  count      = local.manage_database ? 1 : 0
  name       = local.name
  subnet_ids = aws_subnet.data[*].id
}

resource "aws_kms_key" "database" {
  count                   = local.external_production ? 1 : 0
  description             = "${local.name} RDS storage, Performance Insights, and snapshot encryption"
  deletion_window_in_days = 30
  enable_key_rotation     = true

  lifecycle {
    prevent_destroy = true
  }
}

resource "aws_kms_alias" "database" {
  count         = local.external_production ? 1 : 0
  name          = "alias/${local.name}-database"
  target_key_id = aws_kms_key.database[0].key_id
}

resource "aws_db_instance" "postgres" {
  count                           = local.manage_database ? 1 : 0
  identifier                      = "${local.name}-postgres"
  engine                          = "postgres"
  engine_version                  = "17.5"
  instance_class                  = var.db_instance_class
  allocated_storage               = var.db_allocated_storage_gib
  max_allocated_storage           = 500
  storage_type                    = "gp3"
  storage_encrypted               = true
  kms_key_id                      = local.external_production ? aws_kms_key.database[0].arn : null
  db_name                         = var.db_name
  username                        = var.db_master_username
  manage_master_user_password     = true
  multi_az                        = var.db_multi_az
  publicly_accessible             = false
  db_subnet_group_name            = aws_db_subnet_group.main[0].name
  vpc_security_group_ids          = [aws_security_group.database[0].id]
  backup_retention_period         = 14
  backup_window                   = "18:00-19:00"
  maintenance_window              = "sun:19:30-sun:20:30"
  auto_minor_version_upgrade      = true
  deletion_protection             = true
  skip_final_snapshot             = false
  final_snapshot_identifier       = "${local.name}-final"
  copy_tags_to_snapshot           = true
  performance_insights_enabled    = var.db_performance_insights_enabled
  performance_insights_kms_key_id = local.external_production && var.db_performance_insights_enabled ? aws_kms_key.database[0].arn : null
  monitoring_interval             = var.db_enhanced_monitoring_enabled ? 60 : 0
  monitoring_role_arn             = var.db_enhanced_monitoring_enabled ? aws_iam_role.rds_monitoring[0].arn : null
  enabled_cloudwatch_logs_exports = ["postgresql", "upgrade"]

  lifecycle {
    prevent_destroy = true
  }
}

resource "aws_iam_role" "rds_monitoring" {
  count = local.manage_database && var.db_enhanced_monitoring_enabled ? 1 : 0
  name  = "${local.name}-rds-monitoring"
  assume_role_policy = jsonencode({
    Version = "2012-10-17"
    Statement = [{
      Effect    = "Allow"
      Principal = { Service = "monitoring.rds.amazonaws.com" }
      Action    = "sts:AssumeRole"
    }]
  })
}

resource "aws_iam_role_policy_attachment" "rds_monitoring" {
  count      = local.manage_database && var.db_enhanced_monitoring_enabled ? 1 : 0
  role       = aws_iam_role.rds_monitoring[0].name
  policy_arn = "arn:aws:iam::aws:policy/service-role/AmazonRDSEnhancedMonitoringRole"
}

# Historical non-production behavior: Terraform generates the managed Valkey auth
# token, which places that value in encrypted Terraform state. Do not extend
# this pattern to externally owned production secrets; production secret values
# must be populated directly through the approved Secrets Manager process.
resource "random_password" "redis" {
  length  = 48
  special = false
}

resource "aws_secretsmanager_secret" "redis" {
  name                    = "${local.name}/redis"
  recovery_window_in_days = 30
}

resource "aws_secretsmanager_secret_version" "redis" {
  secret_id     = aws_secretsmanager_secret.redis.id
  secret_string = jsonencode({ auth_token = random_password.redis.result })
}

resource "aws_elasticache_subnet_group" "main" {
  name       = local.name
  subnet_ids = local.cache_subnet_ids
}

resource "aws_elasticache_replication_group" "redis" {
  replication_group_id       = "${local.name}-redis"
  description                = "${local.name} distributed cache and rate limits"
  engine                     = "valkey"
  engine_version             = "8.0"
  node_type                  = var.cache_node_type
  num_cache_clusters         = var.cache_cluster_count
  automatic_failover_enabled = var.cache_cluster_count > 1
  multi_az_enabled           = var.cache_cluster_count > 1
  at_rest_encryption_enabled = true
  transit_encryption_enabled = true
  auth_token                 = random_password.redis.result
  subnet_group_name          = aws_elasticache_subnet_group.main.name
  security_group_ids         = [aws_security_group.redis.id]
  snapshot_retention_limit   = 7
  apply_immediately          = false
}

resource "aws_secretsmanager_secret" "application" {
  name                    = "${local.name}/application"
  recovery_window_in_days = 30

  lifecycle {
    prevent_destroy = true
  }
}

moved {
  from = aws_db_subnet_group.main
  to   = aws_db_subnet_group.main[0]
}

moved {
  from = aws_db_instance.postgres
  to   = aws_db_instance.postgres[0]
}

moved {
  from = aws_iam_role.rds_monitoring
  to   = aws_iam_role.rds_monitoring[0]
}

moved {
  from = aws_iam_role_policy_attachment.rds_monitoring
  to   = aws_iam_role_policy_attachment.rds_monitoring[0]
}
