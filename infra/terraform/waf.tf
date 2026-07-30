resource "aws_wafv2_ip_set" "trusted_admin" {
  provider           = aws.us_east_1
  name               = "${local.name}-trusted-admin"
  scope              = "CLOUDFRONT"
  ip_address_version = "IPV4"
  addresses          = []
}

resource "aws_wafv2_web_acl" "main" {
  provider = aws.us_east_1
  name     = local.name
  scope    = "CLOUDFRONT"

  default_action {
    allow {}
  }

  rule {
    name     = "AWSCommonRules"
    priority = 10
    override_action {
      count {}
    }
    statement {
      managed_rule_group_statement {
        name        = "AWSManagedRulesCommonRuleSet"
        vendor_name = "AWS"
      }
    }
    visibility_config {
      cloudwatch_metrics_enabled = true
      metric_name                = "common-rules"
      sampled_requests_enabled   = true
    }
  }

  rule {
    name     = "KnownBadInputs"
    priority = 20
    override_action {
      none {}
    }
    statement {
      managed_rule_group_statement {
        name        = "AWSManagedRulesKnownBadInputsRuleSet"
        vendor_name = "AWS"
      }
    }
    visibility_config {
      cloudwatch_metrics_enabled = true
      metric_name                = "known-bad-inputs"
      sampled_requests_enabled   = true
    }
  }

  rule {
    name     = "SqlInjection"
    priority = 30
    override_action {
      count {}
    }
    statement {
      managed_rule_group_statement {
        name        = "AWSManagedRulesSQLiRuleSet"
        vendor_name = "AWS"
      }
    }
    visibility_config {
      cloudwatch_metrics_enabled = true
      metric_name                = "sqli"
      sampled_requests_enabled   = true
    }
  }

  rule {
    name     = "IpReputation"
    priority = 40
    override_action {
      none {}
    }
    statement {
      managed_rule_group_statement {
        name        = "AWSManagedRulesAmazonIpReputationList"
        vendor_name = "AWS"
      }
    }
    visibility_config {
      cloudwatch_metrics_enabled = true
      metric_name                = "ip-reputation"
      sampled_requests_enabled   = true
    }
  }

  rule {
    name     = "PublicAuthRateLimit"
    priority = 50
    action {
      block {}
    }
    statement {
      rate_based_statement {
        # The application separately rate-limits normalized account + trusted client IP.
        # This edge limit prevents floods without blocking a normal campus NAT login wave.
        limit              = 3000
        aggregate_key_type = "IP"
        scope_down_statement {
          regex_match_statement {
            regex_string = "^/api/(v1/auth/login|auth/password/(forgot|reset)|auth/email-verification/confirm|public/admissions/)"
            field_to_match {
              uri_path {}
            }
            text_transformation {
              priority = 0
              type     = "LOWERCASE"
            }
          }
        }
      }
    }
    visibility_config {
      cloudwatch_metrics_enabled = true
      metric_name                = "public-auth-rate-limit"
      sampled_requests_enabled   = true
    }
  }

  visibility_config {
    cloudwatch_metrics_enabled = true
    metric_name                = local.name
    sampled_requests_enabled   = true
  }
}
