# Jadhavr ERP

Jadhavr ERP is a secure, modular college management platform.

## Repository structure

```text
Jadhavr-ERP/
|-- backend/          Spring Boot REST API, tests, and Postman collections
|-- frontend/         React, TypeScript, Vite, and Tailwind admin portal
|-- CONTRIBUTING.md   Contribution guidelines
`-- SECURITY.md       Security policy and deployment guidance
```

The current implementation includes:

- College management
- Department management
- JWT authentication and role-based authorization
- Super Admin and Principal account management

See the [backend documentation](backend/README.md) for architecture, environment
configuration, API endpoints, credentials, testing, and local setup. See the
[frontend documentation](frontend/README.md) for UI setup and role-aware workflows.

## Quick start

```powershell
Set-Location backend
Copy-Item .env.example .env
mvn test
mvn spring-boot:run
```

In a second terminal:

```powershell
Set-Location frontend
npm install
npm run dev
```

The backend runs at `http://localhost:8081`; the frontend runs at
`http://localhost:5173`.
