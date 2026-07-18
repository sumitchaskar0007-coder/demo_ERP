import { zodResolver } from "@hookform/resolvers/zod";
import { ArrowLeft, BookOpen, CalendarDays, FileDown, GraduationCap, Printer, Sheet, UserRound } from "lucide-react";
import { useCallback, useEffect, useMemo, useState } from "react";
import { useForm } from "react-hook-form";
import { Link, useNavigate, useParams } from "react-router-dom";
import { toast } from "sonner";
import { z } from "zod";
import { Badge, StatusBadge } from "@/components/common/Badge";
import { Button } from "@/components/common/Button";
import { Card } from "@/components/common/Card";
import { EmptyState } from "@/components/common/EmptyState";
import { Input } from "@/components/common/Input";
import { Loader } from "@/components/common/Loader";
import { Pagination } from "@/components/common/Pagination";
import { Select } from "@/components/common/Select";
import { DataTable, type Column } from "@/components/table/DataTable";
import * as academicApi from "@/features/academic/api";
import type { StudentRosterItem } from "@/features/academic/api";
import type { CourseYear, Division } from "@/features/academic/types";
import { weeklyTimetableApi, type WeeklyTimetable } from "@/features/academics/api";
import { exportWeeklyTimetableExcel, exportWeeklyTimetablePdf } from "@/features/academics/weeklyTimetableExport";
import { useAuth } from "@/features/auth/authStore";
import { searchDepartments } from "@/features/departments/api";
import type { Department } from "@/features/departments/types";
import { handleApiError } from "@/lib/handleApiError";
import { PAGE_SIZE, ROUTES, STATUS_OPTIONS } from "@/lib/constants";
import { courseYearSchema, divisionSchema } from "@/lib/validators";
import type { PageResponse } from "@/types/api";

const DEFAULT_ACADEMIC_YEAR = "2026-2027";
const DAYS = ["MONDAY", "TUESDAY", "WEDNESDAY", "THURSDAY", "FRIDAY", "SATURDAY"];
const DAY_LABELS: Record<string, string> = {
  MONDAY: "Monday", TUESDAY: "Tuesday", WEDNESDAY: "Wednesday",
  THURSDAY: "Thursday", FRIDAY: "Friday", SATURDAY: "Saturday",
};
const TIMETABLE_COLORS = [
  "border-blue-200 bg-blue-50 text-blue-950",
  "border-emerald-200 bg-emerald-50 text-emerald-950",
  "border-orange-200 bg-orange-50 text-orange-950",
  "border-violet-200 bg-violet-50 text-violet-950",
  "border-cyan-200 bg-cyan-50 text-cyan-950",
  "border-rose-200 bg-rose-50 text-rose-950",
];
const displayTime = (time: string) => {
  const [hours, minutes] = time.split(":").map(Number);
  return new Intl.DateTimeFormat("en-IN", { hour: "2-digit", minute: "2-digit", hour12: true })
    .format(new Date(2000, 0, 1, hours, minutes));
};
const yearOptions = ["FIRST_YEAR", "SECOND_YEAR", "THIRD_YEAR", "FOURTH_YEAR", "FIFTH_YEAR"].map(
  (value) => ({ label: value.replaceAll("_", " "), value }),
);
const emptyPage = <T,>(): PageResponse<T> => ({
  content: [], page: 0, size: PAGE_SIZE, totalElements: 0, totalPages: 0, last: true,
});
const uniqueById = <T extends { id: number }>(rows: T[]) =>
  [...new Map(rows.map((row) => [row.id, row])).values()];
const uniqueDepartments = (rows: Department[]) =>
  [...new Map(rows.map((row) => [`${row.collegeId}:${row.code.trim().toUpperCase()}`, row])).values()];

function useDepartments() {
  const { user } = useAuth();
  const [departments, setDepartments] = useState<Department[]>([]);
  useEffect(() => {
    if (!user?.collegeId) { setDepartments([]); return; }
    searchDepartments({ collegeId: user.collegeId, status: "ACTIVE", page: 0, size: 100 })
      .then((result) => setDepartments(uniqueDepartments(result.content)))
      .catch((error) => toast.error(handleApiError(error).message));
  }, [user?.collegeId]);
  return departments;
}

export function CourseYearListPage() {
  const departments = useDepartments();
  const [result, setResult] = useState(emptyPage<CourseYear>());
  const [availableYears, setAvailableYears] = useState<CourseYear[]>([]);
  const [departmentId, setDepartmentId] = useState("");
  const [courseYearName, setCourseYearName] = useState("");
  const [status, setStatus] = useState("");
  const [page, setPage] = useState(0);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    academicApi.searchCourseYears({ departmentId: departmentId || undefined, page: 0, size: 100 })
      .then((response) => setAvailableYears([...new Map(response.content.map((row) => [row.yearName, row])).values()]))
      .catch(() => setAvailableYears([]));
  }, [departmentId]);

  const load = useCallback(async () => {
    setLoading(true);
    try {
      setResult(await academicApi.searchCourseYears({
        departmentId: departmentId || undefined,
        yearName: courseYearName || undefined,
        status: status || undefined,
        page,
        size: PAGE_SIZE,
      }));
    } catch (error) {
      toast.error(handleApiError(error).message);
    } finally {
      setLoading(false);
    }
  }, [courseYearName, departmentId, page, status]);
  useEffect(() => { const timer = setTimeout(load, 250); return () => clearTimeout(timer); }, [load]);

  const toggle = async (row: CourseYear) => {
    try {
      await academicApi.setCourseYearStatus(row.id, row.status !== "ACTIVE");
      toast.success("Course Year status updated");
      await load();
    } catch (error) { toast.error(handleApiError(error).message); }
  };
  const columns: Column<CourseYear>[] = [
    { key: "department", header: "Department", render: (r) => `${r.departmentCode} - ${r.departmentName}` },
    { key: "academicYear", header: "Academic Year", render: (r) => r.academicYear },
    { key: "year", header: "Year", render: (r) => r.yearName.replaceAll("_", " ") },
    { key: "displayName", header: "Display Name", render: (r) => r.displayName },
    { key: "code", header: "Code", render: (r) => <Badge>{r.code}</Badge> },
    { key: "status", header: "Status", render: (r) => <StatusBadge status={r.status} /> },
    { key: "divisions", header: "Divisions", render: (r) => r.divisionCount },
    { key: "actions", header: "Actions", render: (r) => <div className="flex gap-2"><Link to={`/principal/course-years/${r.id}/edit`}><Button variant="secondary">Edit</Button></Link><Button variant={r.status === "ACTIVE" ? "danger" : "secondary"} onClick={() => toggle(r)}>{r.status === "ACTIVE" ? "Deactivate" : "Activate"}</Button></div> },
  ];

  return <div className="page-container">
    <div className="flex items-start justify-between"><div><h1 className="page-title">Course Years</h1><p className="page-subtitle">Create each academic level inside a department.</p></div><Link to={ROUTES.createCourseYear}><Button>Create Course Year</Button></Link></div>
    <Card className="mt-6">
      <div className="grid gap-3 border-b p-4 md:grid-cols-3">
        <Select aria-label="Department" options={[{ label: "All departments", value: "" }, ...departments.map((d) => ({ label: `${d.code} - ${d.name}`, value: d.id }))]} value={departmentId} onChange={(e) => { setDepartmentId(e.target.value); setCourseYearName(""); setPage(0); }} />
        <Select aria-label="Course Year" disabled={!departmentId} options={[{ label: departmentId ? "All Course Years" : "Select department first", value: "" }, ...availableYears.map((year) => ({ label: year.yearName.replaceAll("_", " "), value: year.yearName }))]} value={courseYearName} onChange={(e) => { setCourseYearName(e.target.value); setPage(0); }} />
        <Select aria-label="Status" options={STATUS_OPTIONS} value={status} onChange={(e) => { setStatus(e.target.value); setPage(0); }} />
      </div>
      {loading ? <Loader label="Loading Course Years..." /> : result.content.length ? <><DataTable columns={columns} data={result.content} rowKey={(r) => r.id} /><div className="border-t p-4"><Pagination page={result.page} totalPages={result.totalPages} totalElements={result.totalElements} onChange={setPage} /></div></> : <EmptyState title="No Course Years" description="Create the first Course Year for a department." />}
    </Card>
  </div>;
}

type CourseYearValues = z.infer<typeof courseYearSchema>;
export function CourseYearFormPage() {
  const { id } = useParams();
  const editId = id ? Number(id) : null;
  const navigate = useNavigate();
  const departments = useDepartments();
  const { register, reset, handleSubmit, formState: { errors, isSubmitting } } = useForm<CourseYearValues>({
    resolver: zodResolver(courseYearSchema),
    defaultValues: { departmentId: 0, academicYear: DEFAULT_ACADEMIC_YEAR, yearName: "FIRST_YEAR", displayName: "", code: "" },
  });
  useEffect(() => {
    if (editId) academicApi.getCourseYear(editId)
      .then((r) => reset({ departmentId: r.departmentId, academicYear: r.academicYear, yearName: r.yearName, displayName: r.displayName, code: r.code }))
      .catch((e) => toast.error(handleApiError(e).message));
  }, [editId, reset]);
  const submit = async (values: CourseYearValues) => {
    try {
      if (editId) await academicApi.updateCourseYear(editId, { displayName: values.displayName, code: values.code });
      else await academicApi.createCourseYear({ ...values, academicYear: DEFAULT_ACADEMIC_YEAR });
      toast.success(`Course Year ${editId ? "updated" : "created"}`);
      navigate(ROUTES.courseYears);
    } catch (error) { toast.error(handleApiError(error).message); }
  };
  return <div className="page-container"><h1 className="page-title">{editId ? "Edit" : "Create"} Course Year</h1><p className="page-subtitle">Department and academic identity cannot be changed after creation.</p><Card className="mt-6 p-6"><form className="grid gap-4 md:grid-cols-2" onSubmit={handleSubmit(submit)}><Select label="Department" disabled={Boolean(editId)} options={[{ label: "Select department", value: "" }, ...departments.map((d) => ({ label: `${d.code} - ${d.name}`, value: d.id }))]} error={errors.departmentId?.message} {...register("departmentId")} /><Input label="Academic Year" readOnly className="cursor-not-allowed bg-slate-100" error={errors.academicYear?.message} {...register("academicYear")} /><Select label="Year Name" disabled={Boolean(editId)} options={yearOptions} error={errors.yearName?.message} {...register("yearName")} /><Input label="Display Name" error={errors.displayName?.message} {...register("displayName")} /><Input label="Code" error={errors.code?.message} {...register("code")} /><div className="md:col-span-2"><Button type="submit" loading={isSubmitting}>Save Course Year</Button></div></form></Card></div>;
}

export function DivisionListPage() {
  const departments = useDepartments();
  const [years, setYears] = useState<CourseYear[]>([]);
  const [result, setResult] = useState(emptyPage<Division>());
  const [departmentId, setDepartmentId] = useState("");
  const [courseYearId, setCourseYearId] = useState("");
  const [status, setStatus] = useState("");
  const [page, setPage] = useState(0);
  const [loading, setLoading] = useState(true);
  useEffect(() => {
    academicApi.searchCourseYears({ departmentId: departmentId || undefined, status: "ACTIVE", page: 0, size: 100 })
      .then((r) => setYears(uniqueById(r.content))).catch(() => setYears([]));
  }, [departmentId]);
  const load = useCallback(async () => {
    setLoading(true);
    try { setResult(await academicApi.searchDivisions({ departmentId: departmentId || undefined, courseYearId: courseYearId || undefined, status: status || undefined, page, size: PAGE_SIZE })); }
    catch (error) { toast.error(handleApiError(error).message); }
    finally { setLoading(false); }
  }, [courseYearId, departmentId, page, status]);
  useEffect(() => { void load(); }, [load]);
  const toggle = async (row: Division) => {
    try { await academicApi.setDivisionStatus(row.id, row.status !== "ACTIVE"); await load(); }
    catch (error) { toast.error(handleApiError(error).message); }
  };
  const columns: Column<Division>[] = [
    { key: "department", header: "Department", render: (r) => r.departmentName },
    { key: "courseYear", header: "Course Year", render: (r) => r.courseYearDisplayName },
    { key: "division", header: "Division", render: (r) => <Link className="font-semibold text-brand-600 hover:underline" to={`/principal/divisions/${r.id}`}>{r.name} ({r.code})</Link> },
    { key: "capacity", header: "Capacity", render: (r) => r.capacity },
    { key: "teacher", header: "Class Teacher", render: (r) => r.classTeacherName || "Not assigned" },
    { key: "status", header: "Status", render: (r) => <StatusBadge status={r.status} /> },
    { key: "actions", header: "Actions", render: (r) => <div className="flex flex-wrap gap-2"><Link to={`/principal/divisions/${r.id}/edit`}><Button variant="secondary">Edit</Button></Link><Button variant="secondary" onClick={() => toggle(r)}>{r.status === "ACTIVE" ? "Deactivate" : "Activate"}</Button></div> },
  ];
  return <div className="page-container"><div className="flex items-start justify-between"><div><h1 className="page-title">Divisions</h1><p className="page-subtitle">Browse and manage the divisions in your college.</p></div><Link to={ROUTES.createDivision}><Button>Create Division</Button></Link></div><Card className="mt-6"><div className="grid gap-3 border-b p-4 md:grid-cols-3"><Select aria-label="Department" options={[{ label: "All departments", value: "" }, ...departments.map((d) => ({ label: d.name, value: d.id }))]} value={departmentId} onChange={(e) => { setDepartmentId(e.target.value); setCourseYearId(""); setPage(0); }} /><Select aria-label="Course Year" options={[{ label: "All Course Years", value: "" }, ...years.map((year) => ({ label: `${year.displayName} (${year.academicYear})`, value: year.id }))]} value={courseYearId} onChange={(e) => { setCourseYearId(e.target.value); setPage(0); }} /><Select aria-label="Status" options={STATUS_OPTIONS} value={status} onChange={(e) => { setStatus(e.target.value); setPage(0); }} /></div>{loading ? <Loader label="Loading Divisions..." /> : result.content.length ? <><DataTable columns={columns} data={result.content} rowKey={(r) => r.id} /><div className="border-t p-4"><Pagination page={result.page} totalPages={result.totalPages} totalElements={result.totalElements} onChange={setPage} /></div></> : <EmptyState title="No Divisions" description="Create a Division under an active Course Year." />}</Card></div>;
}

type DivisionValues = z.infer<typeof divisionSchema>;
export function DivisionFormPage() {
  const { id } = useParams();
  const editId = id ? Number(id) : null;
  const navigate = useNavigate();
  const departments = useDepartments();
  const [years, setYears] = useState<CourseYear[]>([]);
  const { register, watch, reset, setValue, handleSubmit, formState: { errors, isSubmitting } } = useForm<DivisionValues>({ resolver: zodResolver(divisionSchema), defaultValues: { departmentId: 0, courseYearId: 0, name: "", code: "", capacity: 60 } });
  const departmentId = watch("departmentId");
  const departmentField = register("departmentId");
  useEffect(() => {
    if (!departmentId) { setYears([]); return; }
    academicApi.searchCourseYears({ departmentId, status: "ACTIVE", page: 0, size: 100 })
      .then((r) => setYears(uniqueById(r.content))).catch(() => setYears([]));
  }, [departmentId]);
  useEffect(() => {
    if (editId) academicApi.getDivision(editId)
      .then((r) => reset({ departmentId: r.departmentId, courseYearId: r.courseYearId, name: r.name, code: r.code, capacity: r.capacity }))
      .catch((e) => toast.error(handleApiError(e).message));
  }, [editId, reset]);
  const submit = async (values: DivisionValues) => {
    try {
      if (editId) await academicApi.updateDivision(editId, { courseYearId: values.courseYearId, name: values.name, code: values.code, capacity: values.capacity });
      else await academicApi.createDivision({ courseYearId: values.courseYearId, name: values.name, code: values.code, capacity: values.capacity });
      toast.success(`Division ${editId ? "updated" : "created"}`);
      navigate(ROUTES.divisions);
    } catch (error) { toast.error(handleApiError(error).message); }
  };
  return <div className="page-container"><h1 className="page-title">{editId ? "Edit" : "Create"} Division</h1><p className="page-subtitle">Choose a Department and one of its active Course Years.</p><Card className="mt-6 p-6"><form className="grid gap-4 md:grid-cols-2" onSubmit={handleSubmit(submit)}><Select label="Department" options={[{ label: "Select department", value: "" }, ...departments.map((d) => ({ label: d.name, value: d.id }))]} error={errors.departmentId?.message} {...departmentField} onChange={(event) => { departmentField.onChange(event); setValue("courseYearId", 0); }} /><Select label="Course Year" options={[{ label: departmentId ? "Select Course Year" : "Select department first", value: "" }, ...years.map((year) => ({ label: `${year.displayName} (${year.academicYear})`, value: year.id }))]} error={errors.courseYearId?.message} {...register("courseYearId")} /><Input label="Division Name" error={errors.name?.message} {...register("name")} /><Input label="Division Code" error={errors.code?.message} {...register("code")} /><Input label="Capacity" type="number" error={errors.capacity?.message} {...register("capacity")} /><div className="md:col-span-2"><Button type="submit" loading={isSubmitting}>Save Division</Button></div></form></Card></div>;
}

export function DivisionDetailsPage() {
  const { id } = useParams();
  const divisionId = Number(id);
  const [division, setDivision] = useState<Division | null>(null);
  const [students, setStudents] = useState<StudentRosterItem[]>([]);
  const [timetable, setTimetable] = useState<WeeklyTimetable | null>(null);
  const [loading, setLoading] = useState(true);
  useEffect(() => {
    if (!Number.isFinite(divisionId)) return;
    Promise.all([
      academicApi.getDivision(divisionId),
      academicApi.sectionStudents(divisionId),
      weeklyTimetableApi.get(divisionId),
    ]).then(([divisionResponse, roster, timetableResponse]) => {
      setDivision(divisionResponse); setStudents(roster); setTimetable(timetableResponse);
    }).catch((error) => toast.error(handleApiError(error).message)).finally(() => setLoading(false));
  }, [divisionId]);
  const studentColumns: Column<StudentRosterItem>[] = [
    { key: "roll", header: "Roll Number", render: (row) => row.rollNumber || "—" },
    { key: "admission", header: "Admission Number", render: (row) => row.admissionNumber },
    { key: "name", header: "Student", render: (row) => row.fullName },
    { key: "contact", header: "Contact", render: (row) => <div><div>{row.email}</div><div className="text-xs text-slate-500">{row.phone}</div></div> },
  ];
  if (loading) return <div className="page-container"><Loader label="Loading division details..." /></div>;
  if (!division) return <div className="page-container"><EmptyState title="Division not found" description="The requested division could not be loaded." /></div>;
  return <div className="page-container">
    <Link to={ROUTES.divisions} className="mb-4 inline-flex items-center gap-2 text-sm font-semibold text-brand-600"><ArrowLeft className="h-4 w-4" />Back to Divisions</Link>
    <div className="flex flex-wrap items-start justify-between gap-4"><div><div className="flex items-center gap-3"><h1 className="page-title">{division.name} ({division.code})</h1><StatusBadge status={division.status} /></div><p className="page-subtitle">{division.departmentName} · {division.courseYearDisplayName} · {division.academicYear}</p></div><Link to={`/principal/divisions/${division.id}/edit`}><Button variant="secondary">Edit Division</Button></Link></div>
    <div className="mt-6 grid gap-4 md:grid-cols-3">
      <Card className="p-5"><UserRound className="h-5 w-5 text-brand-600" /><p className="mt-3 text-xs font-bold uppercase tracking-wide text-slate-500">Class Teacher</p><p className="mt-1 font-semibold text-slate-900">{division.classTeacherName || "Not assigned"}</p><p className="text-sm text-slate-500">{division.classTeacherEmail || "No teacher assigned"}</p></Card>
      <Card className="p-5"><GraduationCap className="h-5 w-5 text-brand-600" /><p className="mt-3 text-xs font-bold uppercase tracking-wide text-slate-500">Students</p><p className="mt-1 text-2xl font-bold text-slate-900">{students.length}</p><p className="text-sm text-slate-500">Capacity {division.capacity}</p></Card>
      <Card className="p-5"><BookOpen className="h-5 w-5 text-brand-600" /><p className="mt-3 text-xs font-bold uppercase tracking-wide text-slate-500">Course Year</p><p className="mt-1 font-semibold text-slate-900">{division.courseYearDisplayName}</p><p className="text-sm text-slate-500">{division.academicYear}</p></Card>
    </div>
    <DivisionTimetable table={timetable} />
    <Card className="mt-6 overflow-hidden"><div className="flex items-center gap-2 border-b p-5"><GraduationCap className="h-5 w-5 text-brand-600" /><h2 className="font-bold text-slate-900">Student List</h2></div>{students.length ? <DataTable columns={studentColumns} data={students} rowKey={(row) => row.studentProfileId} /> : <EmptyState title="No students assigned" description="Students assigned to this division will appear here." />}</Card>
  </div>;
}

function DivisionTimetable({ table }: { table: WeeklyTimetable | null }) {
  const entries = useMemo(
    () => new Map((table?.entries ?? []).map((entry) => [`${entry.dayOfWeek}:${entry.periodId}`, entry])),
    [table],
  );
  return <section className="mt-6">
    <div className="mb-4 flex flex-wrap items-center justify-between gap-3">
      <div className="flex items-center gap-2"><CalendarDays className="h-5 w-5 text-brand-600" /><h2 className="text-lg font-bold text-slate-900">Weekly Timetable</h2></div>
      {table && <div className="flex flex-wrap gap-2 print:hidden">
        <Button variant="secondary" onClick={() => window.print()}><Printer className="h-4 w-4" />Print</Button>
        <Button variant="secondary" onClick={() => void exportWeeklyTimetablePdf(table)}><FileDown className="h-4 w-4" />PDF</Button>
        <Button variant="secondary" onClick={() => void exportWeeklyTimetableExcel(table)}><Sheet className="h-4 w-4" />Excel</Button>
      </div>}
    </div>
    {!table || !table.periods.length ? <Card><EmptyState title="No timetable available" description="The timetable for this division has not been configured yet." /></Card> :
      <div className="overflow-x-auto rounded-2xl border bg-white shadow-sm">
        <div className="min-w-[1120px]">
          <div className="grid grid-cols-[150px_repeat(6,minmax(155px,1fr))] bg-slate-100 text-center text-xs font-bold uppercase tracking-wide text-slate-500">
            <div className="sticky left-0 z-10 bg-slate-100 p-3 text-left">Period</div>
            {DAYS.map((day) => <div className="border-l p-3" key={day}>{DAY_LABELS[day]}</div>)}
          </div>
          {table.periods.map((period) => <div key={period.id} className={`grid grid-cols-[150px_repeat(6,minmax(155px,1fr))] border-t ${period.kind !== "TEACHING" ? "bg-amber-50/70" : ""}`}>
            <div className={`sticky left-0 z-10 flex min-w-0 flex-col justify-center p-3 ${period.kind !== "TEACHING" ? "bg-amber-50" : "bg-white"}`}>
              <b className="text-sm text-slate-800">{period.label}</b>
              <span className="mt-1 text-[10px] text-slate-400">{displayTime(period.startTime)}-{displayTime(period.endTime)}</span>
            </div>
            {period.kind !== "TEACHING" ? <div className="col-span-6 flex items-center justify-center border-l p-4 text-xs font-bold tracking-[.2em] text-amber-700">{period.kind === "SHORT_BREAK" ? "SHORT BREAK" : "LUNCH BREAK"}</div> : DAYS.map((day) => {
              const entry = entries.get(`${day}:${period.id}`);
              return <div key={day} className="min-h-24 min-w-0 border-l p-2 text-left">
                {entry ? <div className={`h-full min-w-0 rounded-xl border p-2.5 shadow-sm ${TIMETABLE_COLORS[entry.subjectId % TIMETABLE_COLORS.length]}`} title={`${entry.subject}\n${entry.teacher}\n${entry.room ?? "No room"}`}>
                  <b className="break-words text-xs leading-5">{entry.subject}</b>
                  <p className="mt-1 truncate text-[10px] opacity-75">{entry.teacher}</p>
                  <p className="mt-1 text-[10px] font-semibold">{entry.lectureType}{entry.room ? ` - ${entry.room}` : ""}</p>
                </div> : <div className="grid h-full min-h-20 place-items-center text-xs text-slate-300">-</div>}
              </div>;
            })}
          </div>)}
        </div>
      </div>}
  </section>;
}
