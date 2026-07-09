# Security Policy

## Reporting a vulnerability

Do not disclose suspected vulnerabilities in a public issue. Contact the repository
owner privately with a description, reproduction steps, affected endpoints, and
potential impact.

## Deployment guidance

- Replace all localhost credentials and JWT defaults.
- Store secrets outside source control.
- Use HTTPS.
- Use a restricted PostgreSQL account.
- Replace automatic schema updates with versioned migrations.
- Review CORS origins and token expiration for the deployment environment.
- Rotate secrets immediately if exposure is suspected.
