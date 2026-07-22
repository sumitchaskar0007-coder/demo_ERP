# API access matrix

This matrix is the server-side authorization contract. UI route guards are convenience only and must never be treated as authorization.

## Scope rules

| Role | Read scope | Write scope |
|---|---|---|
| Super Admin | Global governance and explicitly allowed global reports | Colleges, principals, global fee setup, and governance only; no academic or attendance operations |
| Principal | Own college | Own-college operations explicitly assigned to Principal |
| HOD | Own college and assigned department(s) | Academic records in assigned department(s) only |
| Student Section | Own college admissions and student records | Own-college admission verification operations only |
| Fee Section | Own college fee accounts and payments | Own-college fee verification operations only |
| Class Teacher | Assigned division/class | Assigned timetable and attendance operations only |
| Subject Teacher | Assigned subjects/divisions | Attendance for assigned scheduled lectures only |
| Student | Own account, admission, fees, timetable, and attendance | Own profile and allowed self-service submissions only |

Every resource lookup must combine role checks with college, department, assignment, or ownership scope. A resource outside scope is denied even when its numeric ID is known.

## Endpoint groups

| API group | Allowed roles | Additional scope |
|---|---|---|
| `/api/dashboard/super-admin` | Super Admin | Global |
| `/api/dashboard/principal` | Principal | Own college |
| `/api/dashboard/student-section` | Student Section | Own college |
| `/api/dashboard/fee-section` | Fee Section | Own college |
| `/api/dashboard/hod` | HOD | Assigned department |
| `/api/dashboard/teacher` | Class Teacher, Subject Teacher | Assigned subjects/classes |
| `/api/dashboard/student` | Student | Own student profile |
| `/api/reports/admissions`, analytics and export | Super Admin, Principal, HOD, Student Section | Principal/Student Section: own college; HOD: assigned department |
| `/api/reports/fees` and export | Super Admin, Principal, Fee Section | Own college unless Super Admin |
| `/api/reports/students` and export | Super Admin, Principal, HOD, Student Section | HOD: assigned department; others own college unless Super Admin |
| `/api/reports/attendance` and export | Super Admin, Principal, HOD | HOD: assigned department; Principal: own college |
| `/api/audit-logs/**` | Super Admin, Principal | Principal: own college |
| `/api/academic/classes/**` | Principal, HOD | Own college; HOD assigned department |
| `/api/academic/sections/**` | Principal, HOD | Own college; HOD assigned department |
| `/api/academic/subjects/**` | Principal, HOD | Own college; HOD assigned department |
| `/api/academic/timetable/**` | Principal, HOD | Own college; HOD assigned department |
| `/api/academic/attendance/**` | Class Teacher, Subject Teacher | Assigned class/subject/session only |
| `/api/weekly-timetables/**` | Principal, HOD, Class Teacher | Principal/HOD read/review in scope; Class Teacher edits assigned division |
| `/api/teacher/timetable/**` | Class Teacher, Subject Teacher | Authenticated teacher only |
| `/api/teacher/attendance/**` | Class Teacher, Subject Teacher | Assigned scheduled lecture only |
| `/api/student/academic/**`, `/api/student/fees/**` | Student | Own records only |
| `/api/fee-section/**` | Fee Section | Own college |
| `/api/principal/fee-structures/**` | Principal | Own college |
| `/api/super-admin/fee-structures/**` | Super Admin | Global governance |
| `/api/super-admin/users/**` | Super Admin | Global governance |
| `/api/account/**` | Any authenticated user | Own account only |

## Required negative tests

- Anonymous access returns `401`.
- An authenticated wrong role returns `403`.
- Cross-college and cross-department IDs return `403` or a scoped `404`.
- Students cannot read another student's record.
- Teachers cannot read or mutate unassigned classes, subjects, timetable entries, or attendance sessions.
- Forbidden mutations leave persistent state unchanged.

