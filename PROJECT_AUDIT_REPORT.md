# College ERP — Backend and Frontend Audit Report

**Audit date:** 21 July 2026  
**Scope:** Static code review, configuration review, automated checks, local runtime health, and PostgreSQL connectivity.  
**Overall result:** The project runs and the production builds compile, but it is **not ready for production** until backend authorization gaps are fixed and regression tests are clean.

## Executive summary

| Area | Status | Summary |
|---|---|---|
| Local runtime | Good | PostgreSQL, Spring Boot, and Vite are running and reachable. |
| Backend packaging | Good | The Spring Boot JAR packages successfully with Java 17. |
| Backend tests | Needs work | 91 of 92 tests pass; one timetable test fails. |
| Frontend build | Good | TypeScript and the Vite production build complete successfully. |
| Frontend lint | Failing | 2 errors and 18 warnings. |
| Frontend formatting | Failing | Prettier reports 62 files with style differences. |
| Authentication foundations | Good | HttpOnly cookie sessions, refresh rotation, CSRF handling, exact CORS origins, and security headers are present. |
| Authorization | Critical | Several authenticated API groups do not enforce the role matrix on the server. |
| Tenant isolation | Mixed | Many services scope by college/department, but some legacy endpoints are overly broad or globally unscoped. |
| Performance | Needs work | Large frontend bundles and multiple unbounded backend `findAll()` operations will not scale. |
| Maintainability | Needs work | Several very large/minified source files, duplicate routes, and tracked dependencies increase risk. |
| Automated frontend coverage | Missing | No frontend unit/component/end-to-end tests were found. |

## Commands and observed results

| Check | Result |
|---|---|
| `PGPASSWORD=postgres psql ... -d college_erp` | Passed; PostgreSQL 18.4 and database `college_erp` are reachable. |
| `GET /actuator/health` | `200`, status `UP`. |
| `GET /api/health` | `200`, application health response is `UP`. |
| `GET /` on backend | `401 Authentication is required`; expected because `/` is protected. |
| Frontend at port 5173 | `200 OK`. |
| `mvn -DskipTests package` with JDK 17 | Passed; executable Spring Boot JAR created. |
| `mvn test` with JDK 17 outside the sandbox | 92 tests, 91 passed, 1 failed, 0 errors. |
| `npm run build` | Passed; 2,912 modules transformed. |
| `npm run lint` | Failed; 2 errors and 18 warnings. |
| `npm run format:check` | Failed; 62 files need formatting. |
| `npm ls --depth=0` | Passed; installed top-level dependency tree is consistent. |

The first sandboxed backend test attempts failed because Mockito could not attach its Byte Buddy agent. The authoritative result above is from the rerun outside the sandbox with the project's declared Java 17 runtime.

## What is working well

### Backend

- The application starts successfully and connects to PostgreSQL.
- The packaged application builds successfully using the declared Java 17 version.
- The codebase has broad business coverage: colleges, departments, admissions, staff, fees, academics, timetable, attendance, reports, audit history, notices, accounts, and email notifications.
- The automated backend suite covers important service rules, including tenant checks, duplicate prevention, admission workflow, payment locking, staff provisioning, JWT handling, and timetable behavior.
- Authentication uses credentialed HttpOnly cookies instead of browser local storage.
- Access tokens include a session version, allowing sessions to be invalidated after account changes.
- CSRF protection uses the cookie/header pattern, and the frontend obtains and sends the token for unsafe requests.
- CORS uses explicit origins with credentials instead of a wildcard.
- Security headers include CSP, frame denial, content-type protection, referrer policy, and permissions policy.
- Actuator exposure is limited to health and hides detailed health information by default.
- Production configuration enables secure cookies, Redis-backed rate limiting, Flyway, schema validation, storage health, and JDBC batching.
- Most domain services perform college or department scoping, and several sensitive controllers use `@PreAuthorize`.
- Fee verification includes pessimistic/transactional safeguards covered by tests.
- Uploaded profile and college images validate file signatures as well as declared media types.
- Errors use a consistent JSON response shape and database constraint messages avoid returning raw SQL details.

### Frontend

- TypeScript compilation and the production Vite build pass.
- Authentication state is restored from the server and is not persisted in local storage.
- The Axios client supports credentials, CSRF, one-at-a-time token refresh, a single retry, and unauthorized-session events.
- Protected routes and role-aware routes prevent users from navigating to irrelevant screens in the UI.
- The frontend covers a large ERP surface: approximately 114 route declarations/occurrences across 105 page and feature source files.
- API calls commonly use abort signals during navigation, reducing stale updates.
- Validation libraries, structured API types, reusable form controls, and error handling utilities are present.
- No general use of TypeScript `any`, `@ts-ignore`, or debug console statements was found in application source.
- The development proxy keeps API requests same-origin, which simplifies cookie and mobile/LAN testing.

## Critical findings — fix before production

### P0-1: Server-side role enforcement is incomplete

Frontend role guards are not a security boundary. Several backend routes fall through to `.anyRequest().authenticated()` in `SecurityConfig`, so any logged-in user can invoke them unless the controller/service adds its own role check.

Confirmed high-risk examples:

1. `DashboardController` has no `@PreAuthorize` annotations and `/api/dashboard/**` has no role matchers.
   - A logged-in non-admin can call `/api/dashboard/super-admin` and receive global counts.
   - Users can call dashboard variants belonging to other roles.

2. `ReportController` has no role annotation and `/api/reports/**` has no role matchers.
   - A student or unrelated staff account can request admissions, fees, students, and attendance reports for its college.
   - Responses include names, emails, phone numbers, admission numbers, fee balances, and statuses.

3. `AcademicController` exposes many create/update/delete operations without role annotations.
   - `AcademicServiceImpl.visible()` permits almost every same-college role except that HODs are additionally department-scoped.
   - A same-college student or unrelated staff user can potentially create classes/sections/subjects, change statuses, assign class teachers, edit timetables, or submit attendance if request IDs are known and downstream checks happen to pass.

4. `AcademicServiceImpl.listAssignments()` reads all active subject-teacher assignments without tenant filtering.

5. `AcademicServiceImpl.attendanceSummary(studentId)` does not verify that the requested student belongs to the caller or the caller's tenant.

6. `AuditLogController.search()` is available to any authenticated user. Its `search()` path applies college scope but not the stricter role scope used by the audit dashboard, exposing college activity, actor names/emails, and descriptions more broadly than the UI suggests.

**Recommendation:** Define an explicit backend access matrix, add path-level rules for every API group, add method-level `@PreAuthorize` for sensitive operations, and keep tenant checks in services as defense in depth. Add integration tests that authenticate as every role and assert both allowed and forbidden endpoints.

### P0-2: Production can silently use development secrets

`application.properties` provides defaults for:

- JWT signing secret
- Super Admin password
- PostgreSQL username/password
- insecure cookies

The production profile enables secure cookies but does not make the JWT secret, admin password, and database credentials mandatory. A deployment with missing environment variables can start using well-known repository defaults.

**Recommendation:** In production, remove fallback values for all secrets and fail startup when values are absent or equal to development defaults. Provision the initial administrator through a one-time secret or migration workflow.

### P0-3: Production migrations do not contain the foundational schema

Flyway migrations start at `V3`. That migration immediately references tables such as `colleges`, `users`, and `student_profiles`, but no `V1`/`V2` migrations creating those tables are present. Production sets `ddl-auto=validate` and Flyway enabled, so a clean database deployment is not reproducible from the repository and is likely to fail.

**Recommendation:** Add a complete baseline migration for every existing table, constraint, index, and seed required for bootstrapping. Verify it by creating a fresh empty PostgreSQL database in CI and starting the application with the production profile.

## High-priority findings

### P1-1: Backend test suite is not fully green

`WeeklyTimetableServiceTest.saveRejectsTeacherNotAssignedToSubject` expects `BadRequestException`, but the service now rejects the call earlier with `AccessDeniedException` (`You have read-only timetable access`).

This may be either a stale test setup or an unintended authorization-order change. Decide the intended contract, then update the implementation or test. Do not simply change the assertion without confirming the expected role behavior.

### P1-2: Attendance CSV export returns the wrong data

`ReportController.export()` handles `admissions` and `fees`, then sends every other allowed type through the default student export. Because the route explicitly allows `attendance`, `/api/reports/attendance/export` produces student rows rather than attendance rows.

### P1-3: Report exports are unbounded and vulnerable to spreadsheet formula injection

- Export uses repository `findAll()` and filters in memory, which can exhaust memory or produce very slow requests on large databases.
- CSV quoting escapes quotes but does not neutralize cell values beginning with `=`, `+`, `-`, or `@`. User-controlled names or identifiers can become formulas when opened in spreadsheet software.

**Recommendation:** Stream or page exports through database projections, enforce export limits/background jobs, and prefix dangerous spreadsheet cells safely.

### P1-4: Admission photo validation trusts the HTTP content type

`AdmissionPhotoService` limits size and generates safe names, but unlike profile and college image storage it does not verify the file signature. A client can submit non-image bytes with an image content type.

**Recommendation:** Reuse one image validation component for every upload path and validate decoded image content/signatures.

### P1-5: Frontend lint fails

Errors:

- `src/pages/admin/AdminPages.tsx:65`
- `src/pages/admin/AdminPages.tsx:1317`

Both are `no-unexpected-multiline` errors caused by placing computed property access on a new line after `api`. This is confusing and potentially fragile automatic-semicolon-insertion style.

There are also 18 hook/Fast Refresh warnings, mainly missing dependencies in `useEffect`/`useCallback`. Missing hook dependencies can cause stale filters, stale selections, or effects that fail to react to updated state.

### P1-6: No frontend automated tests

No frontend `*.test.*` or `*.spec.*` files and no test script were found. The role matrix, refresh/CSRF behavior, high-value forms, fee workflows, and route guards can regress without detection.

**Recommendation:** Add Vitest + React Testing Library for components/hooks and Playwright for a small role-based smoke suite.

## Medium-priority findings

### P2-1: Frontend initial bundle is too large

The production build reports:

- Main application chunk: **1,434.39 kB** minified / **395.34 kB** gzip
- Spreadsheet chunk: **870.25 kB** minified / **323.04 kB** gzip
- jsPDF chunk: **390.79 kB** minified / **128.83 kB** gzip

The router statically imports nearly every page, and no React route-level lazy loading was found. Spreadsheet/PDF libraries should load only when a user starts an export.

**Recommendation:** Use route-level `React.lazy`, lazy-load export libraries, and define sensible Rollup chunks. Measure before and after rather than only increasing Vite's warning limit.

### P2-2: Backend aggregation loads full datasets

Examples include report exports, admission analytics, audit dashboard analytics, academic option lists, and attendance calculations. Several methods call `findAll()` or load an entire filtered result and aggregate in Java.

This is acceptable for small local data but conflicts with the repository's large-data seeder and will become a latency/memory problem at scale.

**Recommendation:** Move counts/sums/grouping to database projections, page all detail tables, and add query-count/performance tests on representative volumes.

### P2-3: Large and compressed source files reduce reviewability

Notable frontend files:

- `AdminPages.tsx`: 1,750 lines
- `features/academics/TimetablePage.tsx`: 1,201 lines
- `AdmissionPrintPage.tsx`: 551 lines
- `AcademicPages.tsx`: 538 lines

Several backend controllers/services are stored as very long single-line declarations, including `ReportController`, `AccountController`, and `AuditLogController`. This makes diffs, stack traces, coverage review, and security review harder.

**Recommendation:** Split by feature/use case and apply the repository formatter in CI.

### P2-4: Formatting check fails on 62 frontend files

Formatting drift is widespread. Run `npm run format`, review the diff, and enforce `format:check` in CI.

### P2-5: Repository tracks installed dependencies

The root repository tracks approximately **910 files under `node_modules/`**. The root `.gitignore` does not ignore `node_modules/` or generic frontend build artifacts.

**Recommendation:** Add `node_modules/`, `dist/`, and relevant generated directories to `.gitignore`, then remove already tracked dependency files from the Git index in a dedicated cleanup commit. Do not delete a developer's local dependencies as part of the cleanup.

### P2-6: Production error logging uses raw stack traces

`GlobalExceptionHandler` prints unexpected exceptions with `System.err` and `printStackTrace()`. The client response is safely generic, but operational logs can become noisy and harder to structure or redact.

**Recommendation:** Use the application logger with a request/correlation ID and an environment-appropriate exception policy.

### P2-7: Duplicate frontend route declaration

The `staff` route is declared twice for Super Admin/Principal route groups. It currently renders the same component but adds ambiguity and should be consolidated.

### P2-8: Dependency/tool versions differ from manifest ranges

Installed versions are newer within declared semver ranges in several cases (for example Vite, Axios, ESLint, React Router, and Zod). This is normal with a lockfile, but reproducible builds require using `npm ci` in CI and committing the intended lockfile.

## Lower-priority observations

- The running backend currently uses Java 26 even though the project targets Java 17. It starts, but emits JVM deprecation warnings. Use JDK 17 for predictable local and CI behavior.
- The Vite build warns that the timetable API is both statically and dynamically imported, so the dynamic import does not create a separate chunk.
- A React Fast Refresh warning is present because one admission component module exports non-component values.
- The global API client retries one unsafe `403` as a possible CSRF failure. Since authorization failures also use `403`, this adds one unnecessary request before returning a genuine forbidden error.
- Local development uses Hibernate `ddl-auto=update` with Flyway disabled. Convenient locally, but it can hide migration gaps unless a production-profile database test runs in CI.
- The project has both newer weekly timetable/attendance modules and legacy academic timetable/attendance endpoints. Their overlapping responsibilities increase authorization and maintenance risk; deprecate one path deliberately.

## Recommended remediation order

### Phase 1 — security and correctness

1. Lock down dashboard, reports, audit, and legacy academic endpoints with explicit server-side roles.
2. Add negative authorization integration tests for every role and API group.
3. Make production secrets mandatory and reject development defaults.
4. Build and test a complete Flyway baseline on a fresh database.
5. Fix attendance export behavior and CSV formula injection.
6. Add signature/content validation to admission photo upload.

### Phase 2 — make the quality gates green

1. Resolve the timetable test contract; reach 92/92 passing tests.
2. Fix the 2 ESLint errors and review all 18 hook warnings.
3. Apply Prettier and enforce lint, formatting, backend tests, frontend build, and migration smoke tests in CI.
4. Add frontend unit tests and a small end-to-end role/access suite.

### Phase 3 — performance and maintainability

1. Replace unbounded `findAll()` analytics/exports with database projections, paging, or streaming.
2. Add route-level code splitting and lazy export dependencies.
3. Split oversized frontend and compressed backend source files.
4. Remove tracked `node_modules` content from Git and strengthen `.gitignore`.
5. Consolidate duplicate routes and retire overlapping legacy modules.

## Suggested release gate

Do not label the current revision production-ready. A reasonable minimum release gate is:

- No known P0/P1 authorization issue.
- Every role has automated allow/deny API tests.
- 100% backend test pass rate.
- Frontend build, lint, and formatting checks pass.
- Fresh-database production-profile startup passes.
- Health/readiness checks include PostgreSQL, Redis, and shared storage in production.
- Export and analytics paths have bounded resource usage.
- Basic end-to-end login, admission, fee, timetable, attendance, report, and logout workflows pass.

## Audit limitations

- This was a code/configuration and local runtime audit, not a penetration test.
- No destructive workflow was executed against the existing local database.
- No cross-browser, visual regression, accessibility-tool, load, backup/restore, SMTP delivery, Redis failover, or clean production deployment test was performed.
- Dependency vulnerability databases were not queried; use automated Maven/npm dependency scanning in CI.
