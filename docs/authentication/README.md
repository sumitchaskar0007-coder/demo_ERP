# Authentication Flowcharts

This directory documents the College ERP cookie-based JWT authentication system.

| Document | Purpose |
|---|---|
| [01-system-architecture.md](01-system-architecture.md) | Components and trust boundaries |
| [02-login-flow.md](02-login-flow.md) | Login, token generation, cookies, and persistence |
| [03-protected-request-flow.md](03-protected-request-flow.md) | JWT filter and protected API authorization |
| [04-refresh-rotation-flow.md](04-refresh-rotation-flow.md) | Access expiry and refresh-token rotation |
| [05-logout-flow.md](05-logout-flow.md) | Session revocation and cookie deletion |
| [06-frontend-session-flow.md](06-frontend-session-flow.md) | React startup and Axios 401 retry behavior |
| [07-csrf-and-cookie-flow.md](07-csrf-and-cookie-flow.md) | CSRF protection and cookie scope |
| [08-authorization-tenant-flow.md](08-authorization-tenant-flow.md) | RBAC, permissions, and tenant isolation |
| [09-rate-limit-and-errors.md](09-rate-limit-and-errors.md) | Login throttling and standardized errors |
| [10-deployment-flow.md](10-deployment-flow.md) | Production request topology and controls |

## Token summary

| Credential | Lifetime | Browser location | Database |
|---|---:|---|---|
| Access JWT | 15 minutes | `erp_access`, HttpOnly cookie, path `/` | Not stored |
| Refresh token | 7 days | `erp_refresh`, HttpOnly cookie, path `/api/v1/auth/refresh` | SHA-256 hash only |
| CSRF token | Session-dependent | `XSRF-TOKEN`, readable cookie | Not stored |

No access token, refresh token, or user profile is saved in localStorage or sessionStorage.
