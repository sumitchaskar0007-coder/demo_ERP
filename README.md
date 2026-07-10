# Jadhavr ERP

## Day 3: Manual Fee Management

The ERP now supports department/year fee structures, Fee Section staff, automatic student fee accounts after Student Section approval, manual QR payment-proof submission, Fee Section verification/rejection, paid/remaining balance tracking, fee transactions, and fee-gated principal review.

Key routes include `/api/principal/fee-structures`, `/api/student/fees`, and `/api/fee-section`. The frontend adds `/fee-structures`, `/student/fees`, `/fee-section/dashboard`, `/fee-section/fee-accounts`, and `/fee-section/payments` with role protection.

This version intentionally has no online gateway or file storage. Students provide a proof URL and Fee Section staff manually validate it. Principal final approval remains a future module.

Postman collection: `backend/postman/Day3-Fee-Module.postman_collection.json`.

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
- Public admissions with student account auto creation
- Student Section verification, admission print data, and Principal review queue
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
