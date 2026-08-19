import { ROLES, ROUTES } from "@/lib/constants";

export type LeadershipWorkspaceMode = "leadership" | "teaching";

export function isLeadershipAccount(roles: string[] = []) {
  return roles.includes(ROLES.PRINCIPAL) || roles.includes(ROLES.HOD);
}

export function workspaceModeForPath(pathname: string): LeadershipWorkspaceMode | null {
  if (
    pathname.startsWith("/teacher/") ||
    pathname === ROUTES.classTeacherClass ||
    pathname === ROUTES.attendanceReport
  ) {
    return "teaching";
  }
  if (
    pathname === ROUTES.dashboard ||
    pathname.startsWith("/principal/") ||
    pathname.startsWith("/hod") ||
    pathname === ROUTES.staff ||
    pathname.startsWith("/staff/") ||
    pathname === ROUTES.students ||
    pathname === ROUTES.timetable ||
    pathname === ROUTES.subjectTeacherAssignments ||
    pathname === ROUTES.studentAllocation
  ) {
    return "leadership";
  }
  return null;
}

export function workspaceHome(mode: LeadershipWorkspaceMode, roles: string[] = []) {
  if (mode === "teaching") return ROUTES.teacherWorkspace;
  return roles.includes(ROLES.PRINCIPAL) ? ROUTES.dashboard : ROUTES.hodWorkspace;
}
