# Jadhavr ERP

## Final Super Admin Scope

Super Admin is a governance role. It can create colleges and principals, view all staff and students, configure category-aware fees globally, review fee collection/pending balances, and view global analytics. It cannot create operational staff, verify admissions/payments, or manage academic operations. Principal owns Student Section, Fee Section, HOD, Class Teacher, and Subject Teacher creation plus college operations.

The Super Admin menu is limited to Dashboard, Colleges, Principals, Staff, Students, Fee Setup, Fee Collection, Pending Fees, Analytics, and Account. Global APIs are under `/api/super-admin/staff`, `/students`, `/fee-structures`, `/fees`, and `/analytics`. Fee categories are `OPEN`, `OBC`, `SC`, `ST`, `SBC`, `VJNT`, `EWS`, and `OTHER`; older fee requests default to `OPEN`.

## Day 5: Dashboards, Reports, Audit and Account Security

Day 5 adds smart role dashboards under `/api/dashboard`, authenticated profile and password management under `/api/account`, college-scoped audit history at `/api/audit-logs`, and admission, fee, attendance, and student reports under `/api/reports`. Each report provides a matching `/export` CSV endpoint. The frontend adds `/account`, `/account/change-password`, `/audit-logs`, and `/reports/*`; `/dashboard` automatically loads the correct role view.

Audit records never contain passwords, JWTs, or payment-proof contents. Principals see only their college audit/report data, while other report roles are restricted by backend security and college scope. CSV files contain operational fields only.

Demo workflow: start the backend and frontend, sign in as Super Admin, review `/dashboard`, reports and audit logs, update the account, then repeat dashboard checks for Principal, Student Section, Fee Section, HOD, Teacher, and Student accounts. Verify restricted pages return 403 and export both admission and fee CSV reports.

Known limitations: analytics are presentation-oriented operational summaries rather than a BI engine; large production datasets should replace several aggregate list scans with database projection queries. SMS, online payments, and mobile applications are intentionally excluded.

## Day 4: Academic Management

The academic module adds principal final admission approval/rejection, automatic student activation and college/year roll-number generation, HOD/class-teacher/subject-teacher creation, academic classes, sections, subjects, student and teacher assignments, clash-safe timetables, attendance sessions, and student timetable/attendance views.

Main backend routes:

- `/api/principal/final-admissions`
- `/api/principal/staff/hod`, `/class-teacher`, `/subject-teacher`
- `/api/academic/classes`, `/sections`, `/subjects`, `/timetable`, `/attendance`
- `/api/hod/dashboard`
- `/api/student/academic`

Role scope is enforced server-side: principals are limited to their college, HODs to their department, teachers to assigned academic work, and students to their own enrollment. Records are status-deactivated rather than physically deleted. Submitted attendance is read-only.

Frontend routes include `/principal/final-admissions`, `/academic/classes`, `/academic/sections`, `/academic/subjects`, `/student/academic/timetable`, and `/student/academic/attendance`.

End-to-end workflow: verify the minimum fee, approve the admission as Principal, create academic staff, create a class and section, enroll the active student, create and assign a subject teacher, create a timetable entry, mark and submit attendance, then sign in as the student to view timetable and attendance.

Run backend verification with `cd backend; mvn test` and frontend verification with `cd frontend; npm run build`.

Known MVP limitations: attendance reopen, bulk import, exams/results, advanced analytics, and multiple active teachers per subject are not included.

## Day 3: Manual Fee Management

The ERP now supports department/year fee structures, Fee Section staff, automatic student fee accounts after Student Section approval, manual QR payment-proof submission, Fee Section verification/rejection, paid/remaining balance tracking, fee transactions, and fee-gated principal review.

Key routes include `/api/principal/fee-structures`, `/api/student/fees`, and `/api/fee-section`. The frontend adds `/fee-structures`, `/student/fees`, `/fee-section/dashboard`, `/fee-section/fee-accounts`, and `/fee-section/payments` with role protection.

This version intentionally has no online gateway or file storage. Students provide a proof URL and Fee Section staff manually validate it. Principal final approval remains a future module.

Postman collection: `backend/postman/Day3-Fee-Module.postman_collection.json`.

## Transactional Email Notifications

Important account events are written to the `email_notifications` outbox in the same database transaction. A scheduled worker safely claims rows using PostgreSQL `FOR UPDATE SKIP LOCKED`, delivers them through the replaceable SMTP provider, and records retry/sent/failed state. Reset and verification tokens are random, hashed in their own tables, expiring, revoked on replacement, and single-use. Sensitive template data is AES-GCM encrypted at rest.

Public endpoints:

- `POST /api/auth/password/forgot`
- `POST /api/auth/password/reset`
- `POST /api/auth/email-verification/confirm`

Authenticated users can call `POST /api/auth/email-verification/request`. Super Admin can monitor and retry/cancel jobs under `/api/super-admin/email-notifications` or at `/admin/email-notifications` in the frontend.

### Local email testing

1. Run `docker compose -f docker-compose.mailpit.yml up -d`.
2. Set `MAIL_ENABLED=true`, `MAIL_HOST=localhost`, `MAIL_PORT=1025`, `MAIL_SMTP_AUTH=false`, and TLS variables to `false`.
3. Start the backend with `mvn spring-boot:run`.
4. View captured mail at `http://localhost:8025`.

All variables are documented in `.env.example`. Never commit real credentials. For production, use a verified transactional sender/domain with SPF, DKIM and DMARC, such as AWS SES SMTP credentials, and set authentication/TLS variables to `true`. Backend credentials must never use `VITE_` variables or be exposed to React.

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
See the [authentication flowcharts](docs/authentication/README.md) for the complete
cookie JWT, refresh rotation, CSRF, authorization, tenant, and deployment flows.

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
