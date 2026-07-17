# Attendance and Timetable System

This module provides tenant-isolated academic setup, timetable assignment, conflict validation, timetable publication, automatic lecture sessions, and manual attendance. RFID, biometric, and thumb-device attendance are intentionally excluded.

## Roles

- `SUPER_ADMIN`, `PRINCIPAL`: full academic, timetable, publish, archive, attendance, and correction access for their authenticated institution.
- `HOD`: academic setup, teacher assignment, timetable management, attendance, and reports.
- `CLASS_TEACHER`: view timetables and mark/correct attendance for assigned classes.
- `SUBJECT_TEACHER`: view assigned timetables and mark/correct assigned lecture sessions.
- `STUDENT`: view published timetable, own sessions, and own attendance report.

Tenant IDs are never accepted in request DTOs. Services obtain `collegeId` from `CustomUserDetails`; entity lookup and repository queries include that value.

## API summary

| Method | Endpoint | Purpose |
|---|---|---|
| GET/POST | `/api/academic/masters?type=...` | List/create academic master records |
| POST | `/api/academic/assignments` | Assign class or subject teacher |
| POST | `/api/academic/enrollments` | Enroll a student in class/section |
| GET/POST | `/api/timetables` | Role-scoped timetable list/create |
| POST | `/api/timetables/{id}/entries` | Add a conflict-checked entry |
| POST | `/api/timetables/{id}/publish` | Publish and generate sessions transactionally |
| POST | `/api/timetables/copy` | Copy a timetable to another week/term |
| GET | `/api/attendance/sessions?from=&to=` | Role-scoped session calendar |
| POST | `/api/attendance/sessions/{id}/submit` | Submit manual attendance transactionally |
| POST | `/api/attendance/records/{id}/correct` | Correct with mandatory reason/audit |
| GET | `/api/attendance/reports/students/{id}` | Attendance percentage report |
| GET | `/api/attendance/reports/students/{id}/excel` | Excel-compatible report export |
| GET | `/api/attendance/reports/students/{id}/pdf` | PDF report export |

## Configuration

`app.attendance.minimum-percentage=75` controls the low-attendance threshold. Session locking defaults to 24 hours after a lecture ends. PostgreSQL migration: `backend/src/main/resources/db/migration/V3__academic_timetable_attendance.sql`.
