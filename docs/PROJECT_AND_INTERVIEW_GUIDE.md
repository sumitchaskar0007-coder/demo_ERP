# Jadhavar ERP: Complete Project and Interview Guide

This document is the central technical guide for understanding, presenting, maintaining, and interviewing about the Jadhavar ERP project. It describes the implemented system as it exists in this repository. Detailed operational procedures remain in the linked runbooks.

## Contents

1. Project summary
2. Technology stack
3. High-level architecture
4. Users and roles
5. Major modules
6. Core business workflows
7. Database and transaction design
8. Security model
9. Frontend engineering decisions
10. Coding concepts and design patterns
11. Production and deployment
12. Testing strategy
13. How to explain the project in an interview
14. Interview questions and model answers
15. Likely coding-round tasks
16. Questions to ask the interviewer
17. Resume-ready points
18. Recommended study order

## 1. Project summary

Jadhavar ERP is a multi-college education management platform. It manages the lifecycle from public admission through fee verification, principal approval, academic allocation, timetable, attendance, notices, reporting, and audit.

The application is designed around these principles:

- Every business record belongs to a college tenant.
- Authorization is enforced by the backend, not only by hidden frontend menus.
- Important state changes are transactional and audited.
- Browser authentication uses secure cookies, CSRF protection, refresh-token rotation, and server-side revocation.
- Validation errors have a consistent API structure and can be displayed beside the exact form field.
- Production uses PostgreSQL, Redis, AWS object storage, health checks, immutable containers, and automated deployment gates.

## 2. Technology stack

| Layer             | Technology                                                   | Why it is used                                                                          |
| ----------------- | ------------------------------------------------------------ | --------------------------------------------------------------------------------------- |
| Backend language  | Java 17                                                      | Mature ecosystem, strong typing, concurrency support, and long-term support             |
| Backend framework | Spring Boot 3.5                                              | REST APIs, dependency injection, validation, security, configuration, and observability |
| Persistence       | Spring Data JPA/Hibernate                                    | Repository abstraction, transactions, entity relationships, and optimistic locking      |
| Database          | PostgreSQL                                                   | ACID transactions, indexes, constraints, row locking, and production reliability        |
| Schema migration  | Flyway                                                       | Versioned, repeatable database evolution instead of automatic production DDL            |
| Cache/rate limits | Redis                                                        | Shared state across multiple backend instances                                          |
| Authentication    | Spring Security, JJWT, BCrypt                                | Cookie JWT sessions, role authorization, token signing, and password hashing            |
| Object storage    | Local storage or AWS S3                                      | Development simplicity and production durability for private documents                  |
| Email             | Spring Mail with transactional outbox                        | Reliable asynchronous account and security notifications                                |
| Async jobs        | Database workers and optional AWS SQS                        | Durable report/email processing with retries                                            |
| Frontend          | React 18 and TypeScript                                      | Component UI with compile-time type checking                                            |
| Build tooling     | Vite 6                                                       | Fast development and optimized production bundles                                       |
| Routing           | React Router                                                 | Lazy-loaded routes and role-aware navigation                                            |
| API client        | Axios                                                        | Cookies, CSRF headers, timeouts, retry after token refresh, and normalized errors       |
| Forms             | React Hook Form and Zod                                      | Efficient form state and shared declarative validation                                  |
| Styling           | Tailwind CSS                                                 | Consistent responsive user interface                                                    |
| Testing           | JUnit 5, Mockito, Vitest, Testing Library, Playwright        | Unit, integration, component, and browser-level coverage                                |
| Infrastructure    | Docker, Terraform, AWS ECS, RDS, ElastiCache, S3, CloudFront | Reproducible production infrastructure and deployments                                  |
| CI/CD             | GitHub Actions                                               | Build, tests, migrations, security scans, immutable images, and controlled release      |

The exact dependency versions are defined in `backend/pom.xml` and `frontend/package.json`; those files are the source of truth when versions change.

## 3. High-level architecture

```text
Browser / mobile browser
        |
        | HTTPS, JSON, HttpOnly cookies, XSRF header
        v
React + TypeScript frontend
        |
        | /api reverse proxy
        v
Spring Security filter chain
        |
        +-- request ID and safe error boundary
        +-- distributed rate limit and backoff
        +-- JWT authentication
        +-- role and admission access checks
        v
REST controller -> service -> repository -> PostgreSQL
                         |
                         +-- Redis: limits/cache/shared authorization state
                         +-- S3/local storage: private files
                         +-- SMTP/outbox: email
                         +-- DB worker/SQS: asynchronous reports
```

### Backend package pattern

Most modules follow this structure:

```text
feature/
|-- controller/    HTTP contract and authorization boundary
|-- dto/           validated request and safe response models
|-- entity/        database mappings
|-- enums/         controlled business states
|-- mapper/        entity-to-response conversion
|-- repository/    database access and scoped queries
`-- service/       transactions and business rules
```

Controllers should remain thin. Business decisions belong in services. Repositories should perform bounded, tenant-scoped data access. Entities are not returned directly to the browser.

### Frontend feature pattern

```text
src/
|-- app/           router and application composition
|-- components/    reusable UI and domain components
|-- features/      domain API clients, types, and feature screens
|-- pages/         route-level screens
|-- routes/        authentication, role, and admission guards
|-- lib/           API client, validators, constants, and error handling
`-- types/         shared API and UI types
```

Routes are lazy-loaded to reduce the initial bundle. Frontend guards improve user experience, while backend security remains authoritative.

## 4. Users and roles

| Role              | Main responsibility                                                       |
| ----------------- | ------------------------------------------------------------------------- |
| `SUPER_ADMIN`     | Colleges, principals, governance, global fee visibility, analytics        |
| `ADMIN`           | Administrative compatibility role where explicitly permitted              |
| `PRINCIPAL`       | College operations, staff, fees, academics, and final admission decisions |
| `HOD`             | Department-scoped academic planning and teacher work                      |
| `STUDENT_SECTION` | Admission verification and student records                                |
| `FEE_SECTION`     | Fee accounts and payment verification                                     |
| `CLASS_TEACHER`   | Assigned class/division, timetable, and attendance work                   |
| `SUBJECT_TEACHER` | Assigned subject timetable and attendance work                            |
| `GENERAL_STAFF`   | Limited staff functions where configured                                  |
| `STUDENT`         | Own admission, documents, fees, timetable, attendance, and notices        |

Important interview point: a role is not the same as a tenant. The role says what a user may do; the college and department scope say where the user may do it.

## 5. Major modules

| Module                 | Responsibilities                                                                                                         |
| ---------------------- | ------------------------------------------------------------------------------------------------------------------------ |
| Authentication/account | Login, refresh rotation, logout, CSRF, password changes, recovery, verification                                          |
| College/department     | Multi-college setup, branding, payment QR, department configuration                                                      |
| Admission              | Public registration, detailed form, drafts, photos/documents, verification, print, history                               |
| Student/staff/user     | Profiles, role creation, status management, department/college assignment                                                |
| Fee                    | Category/gender/year fee structures, fee accounts, proof submission, verification, scholarship and adjustments, receipts |
| Academic               | Years, terms, course years, classes, divisions, subjects, enrollment, teacher assignments                                |
| Timetable              | Periods, rooms, draft/review/approval, conflict checks, student and teacher views                                        |
| Attendance             | Sessions, marking, submission, reports, student summaries                                                                |
| Notices                | Role/college-targeted communication and live notice streams                                                              |
| Email                  | Transactional outbox, encrypted template data, retry worker, admin monitoring                                            |
| Reports/analytics      | Scoped operational reports, CSV exports, durable asynchronous jobs                                                       |
| Audit                  | Actor, tenant, action, target, time, and safe metadata for important changes                                             |
| Storage                | Validated private uploads, S3/local implementations, presigned transfers, cleanup                                        |

## 6. Core business workflows

### 6.1 Authentication flow

1. The browser submits email and password to `/api/v1/auth/login`.
2. Rate limits are checked by IP and normalized account; repeated failures also receive exponential backoff.
3. Spring Security verifies the BCrypt password and active account state.
4. The backend issues a short-lived access JWT and an opaque rotating refresh token in HttpOnly cookies.
5. Only a hash of the refresh token is stored in PostgreSQL.
6. The frontend holds the user profile in memory, not local storage.
7. On an expired access token, Axios performs one synchronized refresh and retries the original request once.
8. Refresh-token reuse, revocation, inactive users, expired tokens, or tenant mismatch cause rejection.

Why cookies instead of local storage? HttpOnly cookies cannot be read by injected JavaScript, reducing token theft through XSS. Because cookies are sent automatically, unsafe authenticated requests also require CSRF protection.

### 6.2 Admission flow

```text
Public form
  -> student account and profile
  -> detailed information and document upload
  -> Student Section review
  -> fee account/payment requirement
  -> Principal review
  -> approved active student
  -> academic class/division allocation
```

Admission states include `STUDENT_DETAILS_PENDING`, `SUBMITTED`, Student Section pending/approved/rejected, Principal pending/approved/rejected, and `CANCELLED`.

The public form validates required fields locally and on the backend. Backend field errors use an `errors` map, for example:

```json
{
  "success": false,
  "message": "Admission information validation failed",
  "errors": {
    "email": "This email is already registered. Use another email or sign in."
  }
}
```

The frontend maps this response to the exact input, shows a summary, and focuses the first invalid field. Public admission submissions are limited to 10 attempts per minute per IP and per normalized email.

### 6.3 Fee flow

1. The Principal configures an active fee structure for department, academic year, course year, category, custom category where applicable, and gender.
2. Admission approval creates or updates the correct fee account.
3. A student submits payment details and proof.
4. Fee Section staff verifies or rejects the payment.
5. Verified transactions update paid and remaining balances atomically.
6. Scholarship, category change, adjustment, removal, and refund operations create explicit transaction history rather than silently rewriting money records.
7. The receipt service creates the official receipt view/PDF from verified data.

Money should use `BigDecimal`, never floating-point `double`, because decimal currency calculations must be exact.

### 6.4 Academic and timetable flow

The Principal/HOD configures academic years, terms, course years, classes, divisions, subjects, rooms, and periods. Students and teachers are assigned to scoped academic records. Timetable conflict rules prevent overlapping use of teachers, rooms, divisions, and periods. A timetable moves through draft/review/approval before students can see it.

### 6.5 Attendance flow

A permitted teacher creates or opens an assigned session, marks students, and submits it. Submitted attendance is treated as controlled academic data. Students can only read their own attendance; staff reports remain tenant and role scoped.

### 6.6 Email and report jobs

Email uses the transactional outbox pattern: the business transaction stores an email job in PostgreSQL, then a worker claims and sends it. This avoids losing an email when the database commits but SMTP fails. Workers use durable state, retry limits, next-attempt timestamps, and safe failure information.

Large report exports are represented as jobs rather than keeping an HTTP request open. Job claiming and retry logic allow database polling or optional SQS transport.

## 7. Database and transaction design

- Flyway owns production schema changes; Hibernate uses `ddl-auto=validate` in production.
- Foreign keys protect relationships and indexes support common tenant/status/date queries.
- Service methods use `@Transactional`; read-only paths use `@Transactional(readOnly = true)` where appropriate.
- Unique constraints and service checks protect identifiers such as email, college code, admission number, and configured structures.
- Mutable workflows use optimistic locking with `@Version`. A stale writer receives a conflict instead of silently overwriting a newer update.
- Work queues use database locking/claim patterns so two workers do not process the same job.
- Status deactivation is preferred where historical records must remain auditable.

Interview answer: application checks produce friendly messages, while database constraints provide the final race-safe guarantee. Both layers are required.

## 8. Security model

### Implemented controls

- BCrypt password hashes; plaintext passwords are never stored.
- Signed access JWTs and opaque rotating refresh tokens.
- HttpOnly, Secure, SameSite cookies in production.
- CSRF cookie/header validation for unsafe authenticated requests.
- Exact CORS origins with credentials; no production wildcard.
- Route and method authorization with Spring Security.
- Tenant checks in services and repository queries.
- Redis-backed distributed limits with fail-closed production behavior.
- HMAC-pseudonymized rate-limit keys rather than raw email/token keys.
- Request body size limits, upload size/type/signature validation, and private object storage.
- CSP, frame denial, no-sniff, referrer, and permissions security headers.
- Sanitized API messages and request correlation IDs; stack traces are logged, not returned.
- Secret validation and environment/secret-manager configuration.
- Audit trails for important business and security actions.
- Dependency, image, Terraform, and secret scans in CI.

### Rate-limit policy

Different endpoints need different limits. Setting every API request to 10 would break normal dashboards, which load multiple resources.

| Traffic                  | Default intent                                           |
| ------------------------ | -------------------------------------------------------- |
| Login                    | Strict per account, broader per IP, plus failure backoff |
| Public admission signup  | 10/minute per IP and 10/minute per email                 |
| Password/token endpoints | Strict account/IP limits and backoff                     |
| Anonymous reads          | Moderate per-IP limit                                    |
| Authenticated APIs       | Higher per-user limit for normal application use         |

Production runs the limiter in Redis so all ECS tasks share the same counters. If required Redis dependencies are unavailable, startup/runtime behavior fails closed rather than silently disabling protection.

## 9. Frontend engineering decisions

- TypeScript types mirror API request/response contracts.
- React Hook Form avoids re-rendering the entire form on every keystroke.
- Zod provides readable client-side errors, while backend validation remains authoritative.
- Shared `Input`, `Select`, and `Textarea` components render consistent labels and errors.
- `handleApiError` filters unsafe server details and normalizes field errors.
- Axios has a finite timeout, sends cookies, obtains CSRF tokens, and prevents infinite refresh loops.
- Route modules are lazy-loaded.
- Responsive selects and focus handling support desktop and mobile browsers.
- Toasts communicate operation results; persistent inline messages are used when a user must correct a field.

## 10. Coding concepts and design patterns used

### Object-oriented programming

| Concept       | Project example                                                                 |
| ------------- | ------------------------------------------------------------------------------- |
| Encapsulation | Entities and services protect state changes behind methods and workflows        |
| Abstraction   | `ObjectStorageService` hides local/S3 implementation details                    |
| Polymorphism  | Different storage or queue implementations satisfy the same contract            |
| Inheritance   | Shared audited entity behavior can be inherited from base entity types          |
| Composition   | Services are assembled from repositories, mappers, validators, and integrations |

Prefer composition over deep inheritance. Composition makes each dependency visible and easier to replace in tests.

### SOLID principles

- Single responsibility: controllers handle HTTP, services handle business rules, repositories handle persistence.
- Open/closed: new storage/provider implementations can be added behind an interface.
- Liskov substitution: an implementation must preserve its interface contract, including failure behavior.
- Interface segregation: focused service/repository interfaces are easier to use than one large ERP interface.
- Dependency inversion: business services depend on abstractions such as storage or notification services rather than AWS/SMTP details.

### Patterns visible in the project

| Pattern                  | Use                                                                         |
| ------------------------ | --------------------------------------------------------------------------- |
| Layered architecture     | Controller, service, repository, and database separation                    |
| Repository               | Encapsulates database access                                                |
| DTO and mapper           | Keeps API models separate from persistence entities                         |
| Adapter/strategy         | Local vs S3 storage and database vs SQS transport                           |
| Transactional outbox     | Reliably schedules email alongside business changes                         |
| State machine            | Admission, payment, timetable, and job statuses have controlled transitions |
| Optimistic locking       | Detects stale concurrent updates                                            |
| Dependency injection     | Supplies collaborators and environment-specific implementations             |
| Circuit/failure boundary | Safe API exception boundary prevents implementation leakage                 |
| Idempotent consumer/job  | Makes retries safe for asynchronous work                                    |

### Data structures and complexity to revise

- `ArrayList`: ordered access `O(1)`, middle insertion/removal `O(n)`.
- `HashMap`/`HashSet`: average lookup/insert `O(1)`; useful for grouping, uniqueness, and duplicate detection.
- `LinkedHashMap`: retains insertion order, useful for stable validation-error ordering.
- `TreeMap`/sorted collections: ordered operations generally `O(log n)`.
- Queue/deque: appropriate for breadth-first processing and in-memory work ordering.
- Database index: conceptually improves lookup but adds write/storage cost; actual performance must be measured with query plans.
- Pagination: prevents unbounded memory and response size; keyset pagination is better than high offsets for very large ordered data.

### REST and HTTP knowledge

| Status | Meaning in this project                                      |
| ------ | ------------------------------------------------------------ |
| `200`  | Successful read/update or enumeration-safe public response   |
| `201`  | Resource created where the controller uses create semantics  |
| `400`  | Invalid request/business validation                          |
| `401`  | Missing or invalid authentication                            |
| `403`  | Authenticated but not authorized/CSRF rejected               |
| `404`  | Scoped resource not found                                    |
| `409`  | Duplicate or optimistic-lock conflict                        |
| `413`  | Upload/request body too large                                |
| `425`  | Uploaded object is still awaiting a required scan            |
| `429`  | Rate limited; client should respect `Retry-After`            |
| `500`  | Unexpected internal failure with safe message and request ID |
| `503`  | Required dependency/service unavailable                      |

Use nouns for resources, HTTP methods for actions where practical, stable JSON contracts, pagination for lists, and idempotency for retryable operations.

### Clean-code rules for this repository

- Use meaningful domain names rather than generic `data`, `obj`, or `temp`.
- Keep methods focused and extract repeated validation/business rules.
- Do not expose entities, credentials, stack traces, or tenant-unscoped repository results.
- Avoid floating-point money, unbounded queries, and destructive hard deletes of audited records.
- Write a regression test whenever fixing a bug.
- Comments should explain why a non-obvious rule exists, not restate the code.
- Prefer explicit state transitions and early validation over deeply nested conditionals.

## 11. Production and deployment

The primary production path is AWS ECS. The older EC2 workflow is intentionally disabled.

```text
CloudFront/S3 frontend
        |
        v
Load balancer -> ECS backend/worker containers
                    |-- RDS PostgreSQL
                    |-- ElastiCache Redis
                    |-- private S3 document bucket
                    |-- SMTP/SES and optional SQS
```

The deployment workflow:

1. Requires an approved `main` commit and explicit production confirmation.
2. Runs backend and frontend verification.
3. Validates Flyway migrations against PostgreSQL.
4. Builds an immutable backend image and resolves its ECR digest.
5. Applies the approved migration task.
6. Deploys ECS services and waits for health/stability checks.
7. Builds and publishes the frontend to S3/CloudFront.
8. Uses Terraform for network, data, storage, observability, WAF, and IAM infrastructure.

Production is not only “the code builds.” It also requires backups, restore drills, monitoring/alerts, capacity tests, secret rotation, rollback evidence, DNS/TLS, and an incident owner. See:

- `docs/production-readiness.md`
- `docs/runbooks/aws-deployment.md`
- `docs/runbooks/backup-restore.md`
- `docs/performance/load-test-runbook.md`
- `SECURITY.md`

## 12. Testing strategy

| Test type               | Examples                                   | Purpose                                     |
| ----------------------- | ------------------------------------------ | ------------------------------------------- |
| Unit                    | Service and validator tests                | Business rules in isolation                 |
| Controller/security     | MockMvc authorization tests                | Status codes, roles, and safe API contracts |
| JPA integration         | Optimistic locking and repository behavior | Real persistence semantics                  |
| Frontend unit/component | Vitest and Testing Library                 | Form errors, API behavior, UI state         |
| Browser                 | Playwright                                 | Important user journeys in a real browser   |
| Migration               | PostgreSQL + Flyway startup task           | Production database compatibility           |
| Performance             | k6                                         | Capacity, latency, and regression evidence  |
| Security                | npm audit, Trivy, Gitleaks, Terraform scan | Vulnerabilities, secrets, images, and IaC   |

Common commands:

```bash
cd backend
./mvnw test
./mvnw verify

cd ../frontend
npm ci
npm run lint
npm run test:unit
npm run build
npm run test:e2e
npm run audit:production
```

## 13. How to explain the project in an interview

### 30-second answer

“Jadhavar ERP is a multi-tenant college management platform built with Spring Boot, PostgreSQL, Redis, React, and TypeScript. It covers admission, fees, academic allocation, timetable, attendance, communication, reporting, and audit. I designed the backend around transactional services and tenant-scoped authorization, used secure rotating cookie sessions with CSRF protection, and prepared it for AWS ECS using Docker, Terraform, Flyway, health checks, and automated security/testing gates.”

### Two-minute answer

“The main challenge was that every operation has both a role boundary and a college or department boundary. I used Spring Security for coarse route/method permissions, then service and repository checks for tenant ownership. The admission workflow creates an account, gathers details/documents, passes Student Section review, creates fee obligations, and then reaches Principal approval before academic allocation. Financial and workflow changes are transactional and auditable. PostgreSQL constraints and optimistic locking protect races. Redis provides distributed rate limiting and shared runtime state across ECS instances. On the frontend, React Hook Form and Zod provide immediate validation, but the backend remains authoritative and returns field-level errors. Production uses HttpOnly rotating tokens, CSRF, private S3 storage, Flyway migrations, CI security scans, and immutable ECS deployments.”

### Strong STAR example

- Situation: concurrent staff actions could overwrite admission, payment, or timetable state.
- Task: preserve correctness without locking every record for a long time.
- Action: added version columns, `@Version`, transactional boundaries, status preconditions, and a global 409 conflict response.
- Result: stale updates are rejected clearly, the user reloads current state, and newer data is not silently lost.

## 14. Interview questions and model answers

### Project and architecture

1. **What problem does the project solve?** It replaces disconnected college admission, fee, academic, and attendance processes with one role- and tenant-aware workflow.
2. **Why a modular monolith instead of microservices?** The domains need strong transactions and are managed by one team. A modular monolith reduces operational cost while keeping package boundaries that can be extracted later.
3. **What is multi-tenancy here?** One application/database serves multiple colleges, and tenant identifiers plus authorization restrict every scoped operation.
4. **Where is business logic placed?** In services, because controllers should translate HTTP and repositories should access data.
5. **Why use DTOs?** They define safe API contracts, prevent mass assignment/entity leakage, and separate validation from persistence.
6. **What is the most complex flow?** Admission-to-fee-to-principal-to-academic activation because it crosses roles, state transitions, documents, money, and concurrency.
7. **How would you split this into microservices later?** Start with asynchronous reports/email/storage; then consider bounded contexts such as fees only after defining idempotent APIs and event consistency.

### Java and Spring

8. **What does dependency injection provide?** Loose coupling, testability, lifecycle/configuration management, and substitutable implementations.
9. **Why constructor injection?** Required dependencies are explicit, immutable, and easy to test; optional integrations use clearly marked setters only where necessary.
10. **What does `@Transactional` do?** It creates a transaction boundary through a Spring proxy and commits or rolls back the unit of work.
11. **Why can self-invocation break `@Transactional`?** Calling an annotated method from the same instance bypasses the Spring proxy.
12. **What is `readOnly = true`?** A transaction hint that communicates intent and can reduce unnecessary persistence work; it is not an authorization control.
13. **How are exceptions converted to API responses?** `@RestControllerAdvice` maps expected exceptions to stable status codes and sanitized `ErrorResponse` bodies.
14. **Why use records for DTOs?** They are concise immutable data carriers with generated accessors, equality, and constructors.
15. **Why use enums for status?** They constrain valid states and make transitions explicit, readable, and testable.
16. **How do you validate requests?** Bean Validation validates backend DTOs; Zod validates frontend forms; backend validation is authoritative.
17. **What is the N+1 problem?** Loading a list and then lazily querying each relation. Solve it using fetch joins, projections, entity graphs, or bounded batch queries.

### JPA and database

18. **Optimistic vs pessimistic locking?** Optimistic locking detects conflicting commits with a version and works well when collisions are uncommon. Pessimistic locking blocks rows and is appropriate for short, high-contention claims.
19. **Why both application validation and database constraints?** Application validation gives friendly feedback; constraints protect correctness under races or other clients.
20. **Why PostgreSQL?** It provides strong transactions, constraints, indexing, row locks, JSON capability, and reliable production tooling.
21. **Why Flyway instead of Hibernate schema update?** Migrations are reviewed, versioned, repeatable across environments, and support rollback planning.
22. **How is money stored/calculated?** With database decimal types and Java `BigDecimal` using explicit rounding rules.
23. **What should be indexed?** Tenant foreign keys combined with frequent status, date, identifier, and search filters—not every column.
24. **How do workers avoid processing one job twice?** Atomically claim eligible rows with transactional locking/status changes and use idempotent processing keys.
25. **What is transaction isolation?** The database guarantee controlling which concurrent changes a transaction can observe. Choose it based on anomalies and contention rather than always selecting the strictest level.

### Authentication and security

26. **JWT vs session?** JWTs allow stateless access verification, while stored opaque refresh-token hashes provide revocation and rotation. This project intentionally combines both properties.
27. **Why hash refresh tokens?** A database leak should not expose a usable bearer credential.
28. **Why BCrypt?** It is salted and deliberately expensive, slowing password cracking.
29. **What does CSRF protect?** It prevents another site from causing a victim’s browser to submit an authenticated unsafe request.
30. **Why does an HttpOnly cookie not solve CSRF?** The browser still sends it automatically; HttpOnly only prevents JavaScript from reading it.
31. **How is XSS reduced?** React escaping, no token in local storage, CSP, input/output discipline, and avoiding unsafe HTML.
32. **What is RBAC?** Permissions based on roles. This project combines RBAC with tenant and resource ownership checks.
33. **Why rate limit by both IP and account?** IP-only limits hurt shared networks and can be bypassed with multiple IPs; account-only limits allow one IP to attack many accounts.
34. **Why Redis for rate limits?** All backend instances must see the same counter and backoff state.
35. **What does fail closed mean?** When a required security dependency fails, protected traffic is rejected instead of silently bypassing the control.
36. **Why sanitize errors?** Stack traces, paths, SQL, and constraint names reveal implementation details useful to attackers.

### React and TypeScript

37. **Why TypeScript?** It catches contract and state errors before runtime and improves refactoring/navigation.
38. **Controlled vs uncontrolled forms?** Controlled inputs store each value in React state. React Hook Form primarily uses uncontrolled inputs to reduce renders.
39. **Why Zod?** It provides composable runtime validation with inferred TypeScript types.
40. **Why still validate on the backend?** Browser code is untrusted and can be bypassed.
41. **How are API errors shown?** Normalize the safe API error, map `errors[field]` into form state, render it under the field, and focus the first invalid control.
42. **Why lazy-load routes?** Users download code for the current workflow instead of the entire ERP immediately.
43. **How is refresh concurrency handled?** All failed requests share one refresh promise; each original request retries at most once.
44. **Why not store auth data in local storage?** XSS can read it and steal the session.
45. **What causes stale closures in hooks?** A callback captures values from its render; correct dependencies or functional state updates prevent stale reads.

### Production and system design

46. **How does the app scale horizontally?** Backend instances are stateless for access verification; shared data, refresh tokens, rate limits, jobs, and files live in PostgreSQL, Redis, queues, and S3.
47. **What are health/readiness checks?** Liveness says the process is alive; readiness says it can safely receive traffic and its required dependencies are usable.
48. **How are secrets managed?** Environment injection from a secret manager, never committed or exposed through `VITE_` variables.
49. **What is an immutable deployment?** Deploy a specific image digest rather than modifying servers or reusing a mutable tag.
50. **How would you roll back?** Redeploy the previous known-good image; database migrations require backward-compatible design and a separately rehearsed remediation plan.
51. **Why use a CDN for the frontend?** Low-latency static delivery, caching, TLS integration, and reduced backend load.
52. **What should be monitored?** Error and latency percentiles, authentication failures, 429/5xx rates, database/Redis saturation, queue age, failed jobs, storage health, and business workflow backlogs.
53. **How do you prove capacity?** Run repeatable k6 scenarios with production-like data, record throughput/latency/error evidence, and connect results to scaling thresholds.
54. **What is idempotency?** Repeating the same logical request produces one effect; essential for retries, payments, email, and queue consumers.

## 15. Likely coding-round tasks based on this project

### Task 1: Find the first duplicate safely

Given normalized emails, return duplicates without changing input order. Discuss `HashSet` time complexity: average `O(n)` time and `O(n)` space.

### Task 2: Implement an admission state transition

Check the current state, role, tenant, and required data inside one transaction. Update the state, append history, and rely on optimistic locking to reject a stale version.

### Task 3: Write a tenant-scoped query

```sql
SELECT id, admission_reference_number, status, created_at
FROM admission_forms
WHERE college_id = :college_id
  AND status = :status
ORDER BY created_at DESC
LIMIT :limit OFFSET :offset;
```

Explain why `college_id` must be part of the query and why an index beginning with tenant/status may help.

### Task 4: Calculate a fee balance

```java
BigDecimal remaining = total
        .subtract(verifiedPayments)
        .subtract(scholarships)
        .add(adjustments)
        .setScale(2, RoundingMode.HALF_UP);
```

Clarify the sign convention for adjustments and prevent a negative balance unless refunds/credits explicitly support it.

### Task 5: Map backend field errors

Normalize the response, allow only known field paths, call the form library’s `setError`, show a persistent summary for non-field errors, and focus the first invalid field. Never render arbitrary server/exception text.

### Task 6: Design a distributed rate limiter

Use an atomic Redis script to increment a key, apply expiry on its first use, and return remaining retry time after the maximum. Hash sensitive subjects and define fail-open only for local development.

### Task 7: Prevent double job processing

Claim pending rows in a short transaction, set a processing owner/time, commit, process outside the claim transaction, and record success or retry in a new transaction. Add recovery for expired claims.

## 16. Questions to ask the interviewer

Good interviews are two-way. Ask questions such as:

- How are domain and tenant boundaries enforced in your services?
- Which reliability metrics define a successful release?
- How does the team manage database migrations and backward compatibility?
- What is the expected balance between feature delivery and technical debt work?
- Which parts of the system have the highest traffic or correctness risk?
- How are code reviews, automated tests, and production approvals handled?
- What does incident response and on-call ownership look like?
- How are junior developers supported when learning the domain?

## 17. Resume-ready points

- Built a multi-tenant college ERP covering admission, fees, academics, timetable, attendance, reporting, and audit.
- Implemented secure rotating cookie authentication with CSRF protection, revocation, role authorization, and tenant isolation.
- Designed transactional fee/admission workflows with PostgreSQL constraints and optimistic locking.
- Added Redis-backed distributed rate limiting, exponential backoff, safe errors, and correlation IDs.
- Built responsive React/TypeScript forms with Zod validation and field-level backend error mapping.
- Prepared AWS ECS infrastructure and CI/CD with Flyway, Terraform, immutable images, health gates, and security scans.

Only claim work you personally understand and can demonstrate. In an interview, explain one concrete decision, its trade-off, and how it was tested rather than listing technologies without depth.

## 18. Recommended study order

1. Read this guide and practice the 30-second and two-minute explanations.
2. Trace login using `docs/authentication/README.md`.
3. Trace one admission request from React form to controller, service, repository, and database.
4. Trace one fee verification transaction and its audit/history records.
5. Study `SecurityConfig`, the JWT/rate-limit filters, and `GlobalExceptionHandler`.
6. Study one `@Version` entity and its conflict test.
7. Run unit tests and explain what each test layer proves.
8. Read the AWS deployment and backup/restore runbooks.
9. Practice the coding tasks without looking at the answers.
10. Prepare two failure stories: one validation/concurrency bug and one production/deployment risk.
