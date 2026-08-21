# College ERP Backend

A secure, multi-college ERP REST API built with Spring Boot and PostgreSQL.

The current backend includes college management, department management, JWT
authentication, role-based authorization, Super Admin provisioning, and Principal
account management.

## Features

- Multi-college management with activation and deactivation
- Departments scoped to colleges with per-college unique codes
- JWT login and stateless Spring Security
- BCrypt password hashing
- Role-based access for Super Admin and Principal
- Idempotent role and Super Admin seeding
- Paginated search and filtering
- Validation and structured API errors
- Audit timestamps
- Request logging and health checks
- Postman collections and Mockito tests

## Technology

- Java 17
- Spring Boot 3.4
- Spring Web, Data JPA, Security, Validation, and Actuator
- PostgreSQL
- JJWT
- Maven
- JUnit 5 and Mockito

## Architecture

```text
backend/src/main/java/com/college-erp/erp
|-- auth/          JWT login, profile, filter, and user details
|-- bootstrap/     Idempotent role and Super Admin seeding
|-- college/       College management
|-- common/        API responses, auditing, errors, filters, and pagination
|-- config/        Security and CORS configuration
|-- department/    Department management
`-- user/          Roles, users, and Principal management
```

Each feature follows a layered controller, service, repository, entity, DTO, and
mapper structure.

## Local setup

### Prerequisites

- JDK 17+
- Maven 3.9+
- PostgreSQL 15+ (other supported PostgreSQL versions should also work)

### Database

```sql
CREATE DATABASE college_erp;
```

From the `backend` directory, copy the environment template and provide your local values:

```powershell
Copy-Item .env.example .env
```

Spring does not automatically load `.env`; export the variables in your shell or
configure them in your IDE. Defaults in `application.properties` are intended only
for localhost development.

```powershell
$env:DB_URL="jdbc:postgresql://localhost:5432/college_erp"
$env:DB_USERNAME="postgres"
$env:DB_PASSWORD="postgres"
$env:SERVER_PORT="8081"
$env:JWT_SECRET="replace-with-a-long-random-secret"
$env:SUPER_ADMIN_PASSWORD="replace-this-password"
mvn spring-boot:run
```

The API starts at `http://localhost:8081`.

## Production authentication

Browser authentication uses `POST /api/v1/auth/login`, `POST /api/v1/auth/refresh`,
and `POST /api/v1/auth/logout`. Access and refresh credentials are delivered only
as HttpOnly cookies; JWTs are never returned in JSON or stored by the frontend.
Refresh tokens are opaque random values and only their SHA-256 hashes are stored
in PostgreSQL. Rotation revokes the old database record before issuing a new token.

Production deployment requirements:

- Set `AUTH_COOKIE_SECURE=true` and terminate HTTPS only at a trusted reverse proxy.
- Supply a randomly generated `JWT_SECRET` of at least 32 bytes through a secret manager.
- Set `JPA_DDL_AUTO=validate` and manage the schema with Flyway/Liquibase migrations.
- Set `RATE_LIMIT_REDIS_ENABLED=true` and configure a private, authenticated Redis service.
- Keep the frontend and API same-site; use `SameSite=Lax` only when the deployment topology requires it.
- Allow only exact production frontend origins in CORS and never use wildcard origins with credentials.
- Forward client IP headers only from trusted proxies and restrict PostgreSQL/Redis to private networks.
- Rotate secrets, use short log retention, monitor refresh-token reuse, and revoke active sessions after account compromise.

The frontend first calls `GET /api/v1/auth/csrf`; mutating requests must echo the
readable `XSRF-TOKEN` cookie in the `X-XSRF-TOKEN` header. Authentication cookies
remain HttpOnly and cannot be read by JavaScript.

### Tests

```powershell
mvn test
```

The test suite uses mocks and does not require a real PostgreSQL database.

## Authentication

Local development seeds all roles and one Super Admin:

```text
Email: admin@erp.com
Password: Admin@12345
```

Change both the password and JWT secret outside localhost.

```http
POST /api/auth/login
Content-Type: application/json

{
  "email": "admin@erp.com",
  "password": "Admin@12345"
}
```

Use the returned token:

```http
Authorization: Bearer <token>
```

## Access matrix

| Role | Access |
|---|---|
| Public | Login and health endpoints |
| Public admissions | `/api/public/admissions/**` |
| Authenticated user | Own authentication profile |
| Student | `/api/student/**` |
| Principal | `/api/principal/**` |
| Super Admin | `/api/super-admin/**` and `/api/principal/**` |

## API overview

### Public and authentication

| Method | Path | Description |
|---|---|---|
| POST | `/api/auth/login` | Log in and receive JWT |
| GET | `/api/auth/profile` | Get the authenticated profile |
| GET | `/api/health` | Application health |
| GET | `/actuator/health` | Actuator health |

### Public admissions

Each active college can expose a public admission link such as
`http://localhost:5173/admission/ABC001`. The frontend calls the backend by
college code to fetch college details and active departments, then submits the
student's basic admission form.

| Method | Path | Description |
|---|---|---|
| GET | `/api/public/admissions/college/{collegeCode}/info` | Public college admission info and active departments |
| POST | `/api/public/admissions/college/{collegeCode}/submit` | Submit public admission form and auto-create student account |

Submission creates a `STUDENT` user, stores only a BCrypt password hash, creates a
student profile, creates an admission form, and returns the temporary password
once for localhost MVP testing.

Current admission status after submission: `SUBMITTED`.

Student Section verification will be added in the next module.

Production security note: In production, credentials should be sent through
email/SMS and never returned in API response.

Example public admission request:

```http
POST /api/public/admissions/college/ABC001/submit
Content-Type: application/json

{
  "departmentId": 1,
  "firstName": "Aarav",
  "middleName": "Rajesh",
  "lastName": "Patil",
  "email": "aarav.patil@example.com",
  "phone": "9876543210",
  "dateOfBirth": "2007-05-14",
  "gender": "Male",
  "addressLine1": "Shivaji Nagar",
  "addressLine2": "Near Bus Stand",
  "city": "Pune",
  "state": "Maharashtra",
  "pincode": "411001",
  "parentName": "Rajesh Patil",
  "parentPhone": "9876500001",
  "parentEmail": "rajesh.patil@example.com",
  "previousSchoolName": "ABC Junior College",
  "previousClassName": "12th Science",
  "previousPercentage": 78.50
}
```

Example response data:

```json
{
  "admissionReferenceNumber": "ADM-ABC001-2026-000001",
  "admissionNumber": "STU-ABC001-2026-000001",
  "status": "SUBMITTED",
  "studentUserId": 10,
  "studentProfileId": 5,
  "collegeName": "ABC College of Computer Science",
  "collegeCode": "ABC001",
  "departmentName": "Bachelor of Computer Applications",
  "departmentCode": "BCA",
  "studentName": "Aarav Rajesh Patil",
  "email": "aarav.patil@example.com",
  "temporaryPassword": "Stu@48291Ab",
  "loginUrl": "/login",
  "message": "Admission submitted successfully. Please save your login credentials."
}
```

Students log in through the existing auth endpoint:

```http
POST /api/auth/login
Content-Type: application/json

{
  "email": "aarav.patil@example.com",
  "password": "<temporaryPassword>"
}
```

### Student self-service

| Method | Path | Description |
|---|---|---|
| GET | `/api/student/admissions/me` | Get the authenticated student's latest admission |
| GET | `/api/student/profile/me` | Get the authenticated student's profile |

### Staff and Student Section verification

Principal users can create Student Section staff for their own college. Super Admin
users can create Student Section staff for any active college. Staff accounts use
the existing `/api/auth/login` endpoint and receive the `STUDENT_SECTION` role.

| Method | Path | Description |
|---|---|---|
| POST | `/api/principal/staff/student-section` | Create Student Section staff |
| GET | `/api/principal/staff/search` | Search staff |
| GET | `/api/principal/staff/{id}` | Get staff detail |
| PATCH | `/api/principal/staff/{id}/activate` | Activate staff and linked user |
| PATCH | `/api/principal/staff/{id}/deactivate` | Deactivate staff and linked user |

Example create Student Section staff request:

```http
POST /api/principal/staff/student-section
Authorization: Bearer <principalToken>
Content-Type: application/json

{
  "collegeId": 1,
  "fullName": "Student Section Staff",
  "email": "student.section@example.com",
  "phone": "9876543210",
  "password": "Staff@123",
  "joiningDate": "2026-07-10"
}
```

Student Section users can search, review, approve, reject, print, and view history
for admissions in their college. Principal and Super Admin users have read access
to these endpoints for operational visibility.

| Method | Path | Description |
|---|---|---|
| GET | `/api/student-section/admissions` | Search scoped admissions |
| GET | `/api/student-section/admissions/{admissionId}` | Get admission detail |
| PATCH | `/api/student-section/admissions/{admissionId}/start-review` | Move submitted admission into review |
| PATCH | `/api/student-section/admissions/{admissionId}/approve` | Approve admission data at Student Section level |
| PATCH | `/api/student-section/admissions/{admissionId}/reject` | Reject admission data with reason |
| GET | `/api/student-section/admissions/{admissionId}/history` | Get status history |
| GET | `/api/student-section/admissions/{admissionId}/print-data` | Get print-ready admission form data |
| PATCH | `/api/student-section/admissions/{admissionId}/mark-printed` | Increment print audit fields |

Example approve request:

```http
PATCH /api/student-section/admissions/1/approve
Authorization: Bearer <studentSectionToken>
Content-Type: application/json

{
  "remarks": "Student data verified successfully"
}
```

Example reject request:

```http
PATCH /api/student-section/admissions/1/reject
Authorization: Bearer <studentSectionToken>
Content-Type: application/json

{
  "rejectionReason": "Parent phone number is invalid"
}
```

Example print data request:

```http
GET /api/student-section/admissions/1/print-data
Authorization: Bearer <studentSectionToken>
```

Print data returns `college`, `student`, `parent`, `academic`, `verification`,
`declarations`, and `signatureLabels` sections. `mark-printed` updates
`lastPrintedAt`, `lastPrintedBy`, and `printCount`, and records an
`ADMISSION_FORM_PRINTED` history action while leaving status as
`STUDENT_SECTION_APPROVED`.

Status transitions:

| From | To |
|---|---|
| `SUBMITTED` | `STUDENT_SECTION_REVIEW_PENDING` |
| `SUBMITTED` | `STUDENT_SECTION_APPROVED` |
| `SUBMITTED` | `STUDENT_SECTION_REJECTED` |
| `STUDENT_SECTION_REVIEW_PENDING` | `STUDENT_SECTION_APPROVED` |
| `STUDENT_SECTION_REVIEW_PENDING` | `STUDENT_SECTION_REJECTED` |

Invalid transitions include `STUDENT_SECTION_APPROVED` to
`STUDENT_SECTION_REJECTED` and `STUDENT_SECTION_REJECTED` to
`STUDENT_SECTION_APPROVED`.

### Principal review preparation

Principal review is read-only in this module. Final Principal approval and
rejection are intentionally not implemented yet.

| Method | Path | Description |
|---|---|---|
| GET | `/api/principal/admissions/review-ready` | List `STUDENT_SECTION_APPROVED` admissions |
| GET | `/api/principal/admissions/{admissionId}` | Get scoped admission detail |
| GET | `/api/principal/admissions/{admissionId}/history` | Get scoped admission history |

Example principal review queue request:

```http
GET /api/principal/admissions/review-ready
Authorization: Bearer <principalToken>
```

### Colleges — Super Admin

| Method | Path | Description |
|---|---|---|
| POST | `/api/super-admin/colleges` | Create college |
| GET | `/api/super-admin/colleges` | List colleges |
| GET | `/api/super-admin/colleges/search` | Search and paginate |
| GET | `/api/super-admin/colleges/active` | List active colleges |
| GET | `/api/super-admin/colleges/{id}` | Get by ID |
| GET | `/api/super-admin/colleges/code/{code}` | Get by code |
| PUT | `/api/super-admin/colleges/{id}` | Update college |
| PATCH | `/api/super-admin/colleges/{id}/activate` | Activate |
| PATCH | `/api/super-admin/colleges/{id}/deactivate` | Deactivate |

### Users — Super Admin

| Method | Path | Description |
|---|---|---|
| POST | `/api/super-admin/users/principals` | Create Principal |
| GET | `/api/super-admin/users/search` | Search users |
| GET | `/api/super-admin/users/{id}` | Get user |
| GET | `/api/super-admin/users/college/{collegeId}` | List college users |
| PATCH | `/api/super-admin/users/{id}/activate` | Activate user |
| PATCH | `/api/super-admin/users/{id}/deactivate` | Deactivate user |

### Departments — Principal or Super Admin

| Method | Path | Description |
|---|---|---|
| POST | `/api/principal/departments` | Create department |
| GET | `/api/principal/departments` | List departments |
| GET | `/api/principal/departments/search` | Search and paginate |
| GET | `/api/principal/departments/{id}` | Get by ID |
| GET | `/api/principal/departments/college/{collegeId}` | List by college |
| GET | `/api/principal/departments/college/{collegeId}/active` | List active |
| GET | `/api/principal/departments/college/{collegeId}/code/{code}` | Get by code |
| PUT | `/api/principal/departments/{id}` | Update department |
| PATCH | `/api/principal/departments/{id}/activate` | Activate |
| PATCH | `/api/principal/departments/{id}/deactivate` | Deactivate |

## Business rules

- College codes are globally unique and stored uppercase.
- Department codes are uppercase and unique within their college.
- Inactive colleges cannot receive or reactivate departments or Principals.
- Inactive colleges and inactive departments cannot receive public admissions.
- Public admission emails are normalized lowercase and must not already exist as users.
- Temporary student passwords are returned only once by the submit response and are never stored in plain text.
- Student Section staff belong to one college and can verify only admissions from that college.
- Student Section approval moves admissions to `STUDENT_SECTION_APPROVED` and student profiles to `UNDER_REVIEW`.
- Student Section rejection requires a reason and moves student profiles to `ADMISSION_REJECTED`.
- Print data is available only after Student Section approval; marking printed does not change admission status.
- A college can have only one active Principal.
- Codes and parent relationships are immutable after creation.
- Deactivation is a status change; records are not physically deleted.
- User emails are globally unique and passwords are never returned by the API.

## API responses

```json
{
  "success": true,
  "message": "Operation completed successfully",
  "data": {},
  "timestamp": "2026-07-09T17:00:00"
}
```

Errors use appropriate HTTP statuses without exposing stack traces.

## Postman

Import collections from [`postman/`](postman):

- College Module
- Department Module
- Auth and Users Module
- Admission Module 1
- Day2 Module2-3 StudentSection Print PrincipalReview

Run the Super Admin login request first; the Auth collection stores the returned token
in a collection variable.

## Configuration

See [`.env.example`](.env.example) for all supported environment variables.
Production deployments should use a secrets manager, database migrations, HTTPS, and
a non-default JWT secret.

## Roadmap

- Staff and HOD management
- Student admissions
- Fees and payments
- Attendance
- Timetables
- Analytics and reporting

## Contributing and security

See [CONTRIBUTING.md](CONTRIBUTING.md) before submitting changes. Report security
issues according to [SECURITY.md](SECURITY.md).
#   J a d h a v r - E R P  
 
