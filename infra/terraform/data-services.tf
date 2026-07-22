resource "aws_db_subnet_group" "main" {
  name       = local.name
  subnet_ids = aws_subnet.data[*].id
}

resource "aws_db_instance" "postgres" {
  identifier                      = "${local.name}-postgres"
  engine                          = "postgres"
  engine_version                  = "17.5"
  instance_class                  = var.db_instance_class
  allocated_storage               = 50
  max_allocated_storage           = 500
  storage_type                    = "gp3"
  storage_encrypted               = true
  db_name                         = var.db_name
  username                        = var.db_master_username
  manage_master_user_password     = true
  multi_az                        = true
  publicly_accessible             = false
  db_subnet_group_name            = aws_db_subnet_group.main.name
  vpc_security_group_ids          = [aws_security_group.database.id]
  backup_retention_period         = 14
  backup_window                   = "18:00-19:00"
  maintenance_window              = "sun:19:30-sun:20:30"
  auto_minor_version_upgrade      = true
  deletion_protection             = true
  skip_final_snapshot             = false
  final_snapshot_identifier       = "${local.name}-final"
  copy_tags_to_snapshot           = true
  performance_insights_enabled    = true
  monitoring_interval             = 60
  monitoring_role_arn             = aws_iam_role.rds_monitoring.arn
  enabled_cloudwatch_logs_exports = ["postgresql", "upgrade"]

  lifecycle {
    prevent_destroy = true
  }
}

resource "aws_iam_role" "rds_monitoring" {
  name = "${local.name}-rds-monitoring"
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
  role       = aws_iam_role.rds_monitoring.name
  policy_arn = "arn:aws:iam::aws:policy/service-role/AmazonRDSEnhancedMonitoringRole"
}

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
  subnet_ids = aws_subnet.data[*].id
}

resource "aws_elasticache_replication_group" "redis" {
  replication_group_id       = "${local.name}-redis"
  description                = "${local.name} distributed cache and rate limits"
  engine                     = "valkey"
  engine_version             = "8.0"
  node_type                  = "cache.t4g.small"
  num_cache_clusters         = 2
  automatic_failover_enabled = true
  multi_az_enabled           = true
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
