data "aws_availability_zones" "available" {
  state = "available"
}

resource "aws_vpc" "main" {
  count                = local.manage_network ? 1 : 0
  cidr_block           = "10.42.0.0/16"
  enable_dns_hostnames = true
  enable_dns_support   = true
}

resource "aws_internet_gateway" "main" {
  count  = local.manage_network ? 1 : 0
  vpc_id = aws_vpc.main[0].id
}

resource "aws_subnet" "public" {
  count                   = local.manage_network ? 2 : 0
  vpc_id                  = aws_vpc.main[0].id
  availability_zone       = data.aws_availability_zones.available.names[count.index]
  cidr_block              = cidrsubnet(aws_vpc.main[0].cidr_block, 8, count.index)
  map_public_ip_on_launch = false

  tags = { Name = "${local.name}-public-${count.index + 1}" }
}

resource "aws_subnet" "application" {
  count             = local.manage_network ? 2 : 0
  vpc_id            = aws_vpc.main[0].id
  availability_zone = data.aws_availability_zones.available.names[count.index]
  cidr_block        = cidrsubnet(aws_vpc.main[0].cidr_block, 8, 10 + count.index)

  tags = { Name = "${local.name}-application-${count.index + 1}" }
}

resource "aws_subnet" "data" {
  count             = local.manage_network ? 2 : 0
  vpc_id            = aws_vpc.main[0].id
  availability_zone = data.aws_availability_zones.available.names[count.index]
  cidr_block        = cidrsubnet(aws_vpc.main[0].cidr_block, 8, 20 + count.index)

  tags = { Name = "${local.name}-data-${count.index + 1}" }
}

resource "aws_eip" "nat" {
  count  = local.manage_network ? 2 : 0
  domain = "vpc"

  depends_on = [aws_internet_gateway.main]
}

resource "aws_nat_gateway" "main" {
  count         = local.manage_network ? 2 : 0
  allocation_id = aws_eip.nat[count.index].id
  subnet_id     = aws_subnet.public[count.index].id
}

resource "aws_route_table" "public" {
  count  = local.manage_network ? 1 : 0
  vpc_id = aws_vpc.main[0].id

  route {
    cidr_block = "0.0.0.0/0"
    gateway_id = aws_internet_gateway.main[0].id
  }
}

resource "aws_route_table_association" "public" {
  count          = local.manage_network ? 2 : 0
  subnet_id      = aws_subnet.public[count.index].id
  route_table_id = aws_route_table.public[0].id
}

resource "aws_route_table" "application" {
  count  = local.manage_network ? 2 : 0
  vpc_id = aws_vpc.main[0].id

  route {
    cidr_block     = "0.0.0.0/0"
    nat_gateway_id = aws_nat_gateway.main[count.index].id
  }
}

resource "aws_route_table_association" "application" {
  count          = local.manage_network ? 2 : 0
  subnet_id      = aws_subnet.application[count.index].id
  route_table_id = aws_route_table.application[count.index].id
}

resource "aws_route_table" "data" {
  count  = local.manage_network ? 1 : 0
  vpc_id = aws_vpc.main[0].id
}

resource "aws_route_table_association" "data" {
  count          = local.manage_network ? 2 : 0
  subnet_id      = aws_subnet.data[count.index].id
  route_table_id = aws_route_table.data[0].id
}

data "aws_ec2_managed_prefix_list" "cloudfront" {
  name = "com.amazonaws.global.cloudfront.origin-facing"
}

resource "aws_security_group" "alb" {
  name        = "${local.name}-alb"
  description = "Backend origin only from CloudFront origin-facing addresses"
  vpc_id      = local.vpc_id

  ingress {
    from_port       = var.temporary_domain ? 80 : 443
    to_port         = var.temporary_domain ? 80 : 443
    protocol        = "tcp"
    prefix_list_ids = [data.aws_ec2_managed_prefix_list.cloudfront.id]
  }

  egress {
    from_port   = 8081
    to_port     = 8081
    protocol    = "tcp"
    cidr_blocks = [local.vpc_cidr]
  }
}

resource "aws_security_group" "ecs" {
  name        = "${local.name}-ecs"
  description = "Backend tasks accept traffic only from the ALB"
  vpc_id      = local.vpc_id

  ingress {
    from_port       = 8081
    to_port         = 8081
    protocol        = "tcp"
    security_groups = [aws_security_group.alb.id]
  }

  egress {
    from_port   = 0
    to_port     = 0
    protocol    = "-1"
    cidr_blocks = ["0.0.0.0/0"]
  }
}

resource "aws_security_group" "database" {
  count       = local.manage_database ? 1 : 0
  name        = "${local.name}-database"
  description = "PostgreSQL only from backend tasks"
  vpc_id      = local.vpc_id

  ingress {
    from_port       = 5432
    to_port         = 5432
    protocol        = "tcp"
    security_groups = [aws_security_group.ecs.id]
  }
}

resource "aws_security_group" "redis" {
  name        = "${local.name}-redis"
  description = "Redis TLS only from backend tasks"
  vpc_id      = local.vpc_id

  ingress {
    from_port       = 6379
    to_port         = 6379
    protocol        = "tcp"
    security_groups = [aws_security_group.ecs.id]
  }
}

moved {
  from = aws_vpc.main
  to   = aws_vpc.main[0]
}

moved {
  from = aws_internet_gateway.main
  to   = aws_internet_gateway.main[0]
}

moved {
  from = aws_route_table.public
  to   = aws_route_table.public[0]
}

moved {
  from = aws_route_table.data
  to   = aws_route_table.data[0]
}

moved {
  from = aws_security_group.database
  to   = aws_security_group.database[0]
}
