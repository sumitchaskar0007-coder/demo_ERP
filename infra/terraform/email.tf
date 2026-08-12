locals {
  mail_from_domain  = "mail.${var.domain_name}"
  mail_from_address = "noreply@${var.domain_name}"
}

resource "aws_sesv2_email_identity" "application" {
  count          = var.temporary_domain ? 0 : 1
  email_identity = var.domain_name
}

resource "aws_route53_record" "ses_dkim" {
  count   = var.temporary_domain ? 0 : 3
  zone_id = var.route53_zone_id
  name    = "${aws_sesv2_email_identity.application[0].dkim_signing_attributes[0].tokens[count.index]}._domainkey.${var.domain_name}"
  type    = "CNAME"
  ttl     = 300
  records = ["${aws_sesv2_email_identity.application[0].dkim_signing_attributes[0].tokens[count.index]}.dkim.amazonses.com"]
}

resource "aws_sesv2_email_identity_mail_from_attributes" "application" {
  count                  = var.temporary_domain ? 0 : 1
  email_identity         = aws_sesv2_email_identity.application[0].email_identity
  mail_from_domain       = local.mail_from_domain
  behavior_on_mx_failure = "REJECT_MESSAGE"
}

resource "aws_route53_record" "ses_mail_from_mx" {
  count   = var.temporary_domain ? 0 : 1
  zone_id = var.route53_zone_id
  name    = local.mail_from_domain
  type    = "MX"
  ttl     = 300
  records = ["10 feedback-smtp.${var.aws_region}.amazonses.com"]
}

resource "aws_route53_record" "ses_mail_from_spf" {
  count   = var.temporary_domain ? 0 : 1
  zone_id = var.route53_zone_id
  name    = local.mail_from_domain
  type    = "TXT"
  ttl     = 300
  records = ["v=spf1 include:amazonses.com -all"]
}

resource "aws_route53_record" "ses_dmarc" {
  count   = var.temporary_domain ? 0 : 1
  zone_id = var.route53_zone_id
  name    = "_dmarc.${var.domain_name}"
  type    = "TXT"
  ttl     = 300
  records = ["v=DMARC1; p=quarantine; rua=mailto:admin@${var.domain_name}; adkim=s; aspf=s"]
}

# Preproduction creates isolated SMTP credentials and stores them in its encrypted
# Terraform state. Production mail credentials remain independently managed.
resource "aws_iam_user" "ses_smtp" {
  count = local.external_production ? 0 : 1
  name  = "${local.name}-ses-smtp"
}

resource "aws_iam_user_policy" "ses_smtp" {
  count = local.external_production ? 0 : 1
  name  = "send-email-only"
  user  = aws_iam_user.ses_smtp[0].name
  policy = jsonencode({
    Version = "2012-10-17"
    Statement = [{
      Effect   = "Allow"
      Action   = ["ses:SendEmail", "ses:SendRawEmail"]
      Resource = var.temporary_domain ? "*" : aws_sesv2_email_identity.application[0].arn
    }]
  })
}

resource "aws_iam_access_key" "ses_smtp" {
  count = local.external_production ? 0 : 1
  user  = aws_iam_user.ses_smtp[0].name
}

resource "aws_secretsmanager_secret" "mail" {
  name                    = "${local.name}/mail"
  recovery_window_in_days = 30
}

resource "aws_secretsmanager_secret_version" "mail" {
  count     = local.external_production ? 0 : 1
  secret_id = aws_secretsmanager_secret.mail.id
  secret_string = jsonencode({
    MAIL_HOST         = "email-smtp.${var.aws_region}.amazonaws.com"
    MAIL_USERNAME     = aws_iam_access_key.ses_smtp[0].id
    MAIL_PASSWORD     = aws_iam_access_key.ses_smtp[0].ses_smtp_password_v4
    MAIL_FROM_ADDRESS = local.mail_from_address
  })
}

# Preserve the existing non-production resource addresses while ensuring a
# separate production state never creates SMTP credentials or secret values.
moved {
  from = aws_iam_user.ses_smtp
  to   = aws_iam_user.ses_smtp[0]
}

moved {
  from = aws_iam_user_policy.ses_smtp
  to   = aws_iam_user_policy.ses_smtp[0]
}

moved {
  from = aws_iam_access_key.ses_smtp
  to   = aws_iam_access_key.ses_smtp[0]
}

moved {
  from = aws_secretsmanager_secret_version.mail
  to   = aws_secretsmanager_secret_version.mail[0]
}
