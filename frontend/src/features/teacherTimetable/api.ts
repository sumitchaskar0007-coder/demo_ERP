import { apiClient } from "@/lib/apiClient";

export interface TeacherPeriod {
  key: string;
  position: number;
  label: string;
  startTime: string;
  endTime: string;
  kind: "TEACHING" | "SHORT_BREAK" | "LUNCH_BREAK";
}

export interface TeacherLecture {
  id: number;
  dayOfWeek: string;
  periodKey: string;
  periodLabel: string;
  startTime: string;
  endTime: string;
  subjectId: number;
  subject: string;
  department: string;
  year: string;
  division: string;
  lectureType: string;
  remarks?: string;
}

export interface TeacherTimetable {
  teacherName: string;
  employeeId: string;
  totalWeeklyLectures: number;
  todayLectureCount: number;
  currentDay: string;
  periods: TeacherPeriod[];
  lectures: TeacherLecture[];
}

export interface TeacherDay {
  day: string;
  periods: TeacherPeriod[];
  lectures: TeacherLecture[];
}

export interface NextLecture {
  lecture?: TeacherLecture | null;
  startsInMinutes?: number | null;
}

const unwrap = <T>(response: { data: { data: T } }) => response.data.data;

export const teacherTimetableApi = {
  get: async () => unwrap<TeacherTimetable>(await apiClient.get("/api/teacher/timetable")),
  today: async () => unwrap<TeacherDay>(await apiClient.get("/api/teacher/timetable/today")),
  next: async () => unwrap<NextLecture>(await apiClient.get("/api/teacher/timetable/next")),
  day: async (day: string) =>
    unwrap<TeacherDay>(await apiClient.get(`/api/teacher/timetable/day/${day}`)),
};
