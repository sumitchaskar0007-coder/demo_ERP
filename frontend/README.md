# Jadhavr ERP Frontend

Production-oriented React administration portal for the Jadhavr College ERP backend.

## Technology

- React 18 and TypeScript
- Vite
- React Router
- Axios
- React Hook Form and Zod
- Tailwind CSS
- Lucide icons
- Sonner notifications

## Backend requirement

The Spring Boot backend must be running at `http://localhost:8081`.

From the repository root:

```powershell
Set-Location backend
mvn spring-boot:run
```

## Environment

Copy `.env.example` to `.env`:

```powershell
Copy-Item .env.example .env
```

```dotenv
VITE_API_BASE_URL=http://localhost:8081
```

Only public frontend configuration belongs in Vite environment variables. Never put
database credentials, JWT signing secrets, or user passwords in frontend environment
files.

## Install and run

```powershell
npm install
npm run dev
```

Open `http://localhost:5173`.

Production checks:

```powershell
npm run lint
npm run build
```

## Default login

```text
Email: admin@erp.com
Password: Admin@12345
```

These credentials are only for localhost development.

## Routes

| Route | Access |
|---|---|
| `/login` | Public |
| `/dashboard` | Super Admin, Principal |
| `/profile` | Super Admin, Principal |
| `/colleges` | Super Admin |
| `/colleges/:id` | Super Admin |
| `/departments` | Super Admin, Principal |
| `/departments/:id` | Super Admin, Principal |
| `/users` | Super Admin |
| `/users/:id` | Super Admin |
| `/users/principals/create` | Super Admin |
| `/admission/:collegeCode` | Public |
| `/student/dashboard` | Student |
| `/student/profile` | Student |
| `/student/admission` | Student |
| `/student-section/dashboard` | Student Section |
| `/student-section/admissions` | Student Section, Principal, Super Admin |
| `/student-section/admissions/:admissionId` | Student Section, Principal, Super Admin |
| `/student-section/admissions/:admissionId/print` | Student Section, Principal, Super Admin |
| `/staff` | Principal, Super Admin |
| `/staff/student-section/create` | Principal, Super Admin |
| `/principal/admissions/review-ready` | Principal, Super Admin |
| `/principal/admissions/:admissionId` | Principal, Super Admin |
| `/forbidden` | Public error page |

## Security behavior

- JWT and the minimal user session are stored in local storage for this localhost MVP.
- Axios attaches the Bearer token to API requests.
- Passwords are never persisted.
- Tokens are never printed to the console.
- HTTP 401 clears the session and redirects to login.
- HTTP 403 redirects to the forbidden page.
- Route guards prevent Principal access to Super Admin screens.
- Backend authorization remains the source of truth.

For a public production deployment, prefer secure HTTP-only cookies backed by an
appropriate server-side token flow.

## Available workflows

Super Admin:

- Dashboard metrics
- Create, search, inspect, update, activate, and deactivate colleges
- Create, search, inspect, update, activate, and deactivate departments
- Create Principals and manage user status

Principal:

- College-specific dashboard metrics
- Create and manage departments only for the assigned college
- View profile

Day 2:

- Public admission link format: `http://localhost:5173/admission/ABC001`
- Public admission form submission shows temporary student credentials once.
- Student users can view dashboard, profile, and admission status.
- Principal and Super Admin can create Student Section staff.
- Student Section users can search admissions, start review, approve, reject, view history, open print data, and mark printed.
- Principal users can view the read-only review-ready queue.

Testing workflow:

1. Start backend: `mvn spring-boot:run`
2. Start frontend: `npm run dev`
3. Login Super Admin: `admin@erp.com` / `Admin@12345`
4. Create a college and department if needed.
5. Create a Principal if needed.
6. Login Principal and create Student Section staff.
7. Open `http://localhost:5173/admission/ABC001`.
8. Submit admission and save the temporary credentials.
9. Login as Student and check dashboard/admission/profile.
10. Login as Student Section staff and approve or reject the admission.
11. Open print page, print, and mark printed.
12. Login Principal and view the review-ready queue.

Future ERP modules are visual placeholders only and do not call nonexistent APIs.
