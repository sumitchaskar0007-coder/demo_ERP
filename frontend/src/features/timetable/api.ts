import { apiClient } from "@/lib/apiClient";
import type {
  Timetable,
  TimetableEntry,
  PeriodInfo,
  SubjectInfo,
  TeacherInfo,
  RoomInfo,
  SectionInfo,
} from "./types";

const BASE = "/api/timetables";
const CT = "/api/class-teacher/timetable";

export const timetableApi = {
  list: async () => (await apiClient.get<Timetable[]>(BASE)).data,

  create: async (body: Record<string, unknown>) =>
    (await apiClient.post<Timetable>(BASE, body)).data,

  addEntry: async (id: number, body: Record<string, unknown>) =>
    (await apiClient.post<TimetableEntry>(`${BASE}/${id}/entries`, body)).data,

  updateEntry: async (id: number, entryId: number, body: Record<string, unknown>) =>
    (await apiClient.put<TimetableEntry>(`${BASE}/${id}/entries/${entryId}`, body)).data,

  deleteEntry: async (id: number, entryId: number) =>
    (await apiClient.delete(`${BASE}/${id}/entries/${entryId}`)),

  publish: async (id: number) =>
    (await apiClient.post<Timetable>(`${BASE}/${id}/publish`)).data,

  archive: async (id: number) =>
    (await apiClient.post<Timetable>(`${BASE}/${id}/archive`)).data,

  copyDay: async (id: number, body: Record<string, unknown>) =>
    (await apiClient.post<Timetable>(`${BASE}/${id}/copy-day`, body)).data,

  copyWeek: async (id: number, body: Record<string, unknown>) =>
    (await apiClient.post<Timetable>(`${BASE}/${id}/copy-week`, body)).data,

  search: async (params: Record<string, unknown>) =>
    (await apiClient.get<TimetableEntry[]>(`${BASE}/search`, { params })).data,

  rooms: async () =>
    (await apiClient.get<RoomInfo[]>(`${BASE}/rooms`)).data,
};

export const classTeacherApi = {
  mySection: async () =>
    (await apiClient.get<SectionInfo>(`${CT}/my-section`)).data,

  subjects: async () =>
    (await apiClient.get<SubjectInfo[]>(`${CT}/subjects`)).data,

  teachers: async (subjectId: number) =>
    (await apiClient.get<TeacherInfo[]>(`${CT}/teachers`, { params: { subjectId } })).data,

  periods: async () =>
    (await apiClient.get<PeriodInfo[]>(`${CT}/periods`)).data,
};
