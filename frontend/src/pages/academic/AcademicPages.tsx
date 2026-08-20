import { useCallback, useEffect, useMemo, useState } from "react";
import { useNavigate, useParams } from "react-router-dom";
import { BookOpen, CalendarDays, Filter, Plus } from "lucide-react";
import { toast } from "sonner";
import { Card } from "@/components/common/Card";
import { Button } from "@/components/common/Button";
import { Input } from "@/components/common/Input";
import { EmptyState } from "@/components/common/EmptyState";
import { Loader } from "@/components/common/Loader";
import { Select } from "@/components/common/Select";
import { handleApiError } from "@/lib/handleApiError";
import * as api from "@/features/academic/api";
import { useAuth } from "@/features/auth/authStore";
import { searchDepartments } from "@/features/departments/api";
import type { Department } from "@/features/departments/types";
import type { AcademicClass, Section, Subject } from "@/features/academic/types";
import type { WeeklyTimetable } from "@/features/academics/api";

const preferredDepartments = (rows: Department[], classes: AcademicClass[]) => {
  const classCounts = new Map<number, number>();
  classes.forEach((item) => {
    classCounts.set(item.department.id, (classCounts.get(item.department.id) ?? 0) + 1);
  });
  const preferred = new Map<string, Department>();
  rows.forEach((row) => {
    const key = `${row.collegeId}:${row.code.trim().toUpperCase()}`;
    const current = preferred.get(key);
    const rowCount = classCounts.get(row.id) ?? 0;
    const currentCount = current ? (classCounts.get(current.id) ?? 0) : -1;
    if (!current || rowCount > currentCount || (rowCount === currentCount && row.id > current.id)) {
      preferred.set(key, row);
    }
  });
  return [...preferred.values()];
};

const uniqueAcademicClasses = (rows: AcademicClass[]) => [
  ...new Map(
    rows.map((row) => [`${row.department.id}:${row.academicYear}:${row.yearName}`, row]),
  ).values(),
];

const semesterNumbersFor = (yearName?: string) => {
  const year = (
    {
      FIRST_YEAR: 1,
      SECOND_YEAR: 2,
      THIRD_YEAR: 3,
      FOURTH_YEAR: 4,
      FIFTH_YEAR: 5,
    } as Record<string, number>
  )[yearName ?? ""];
  return year ? [year * 2 - 1, year * 2] : [];
};

function Shell({
  title,
  subtitle,
  children,
}: {
  title: string;
  subtitle: string;
  children: React.ReactNode;
}) {
  return (
    <div className="page-container">
      <h1 className="page-title">{title}</h1>
      <p className="page-subtitle">{subtitle}</p>
      <Card className="mt-6 p-4 sm:p-5">{children}</Card>
    </div>
  );
}
type Kind = "classes" | "sections" | "subjects";
export function AcademicListPage({ kind }: { kind: Kind }) {
  const navigate = useNavigate();
  const { user } = useAuth();
  const [rows, setRows] = useState<(AcademicClass | Section | Subject)[]>([]),
    [loading, setLoading] = useState(true);
  const [departments, setDepartments] = useState<Department[]>([]);
  const [yearOptions, setYearOptions] = useState<string[]>([]);
  const [deptFilter, setDeptFilter] = useState("");
  const [yearFilter, setYearFilter] = useState("");

  useEffect(() => {
    if (kind !== "subjects") return;
    if (!user?.collegeId) {
      setDepartments([]);
      return;
    }
    Promise.all([
      searchDepartments({ collegeId: user.collegeId, status: "ACTIVE", page: 0, size: 100 }),
      api.searchAcademicClasses(),
    ])
      .then(([result, classes]) => setDepartments(preferredDepartments(result.content, classes)))
      .catch((e) => toast.error(handleApiError(e).message));
  }, [kind, user?.collegeId]);

  const loadYearOptions = async (departmentId: string) => {
    if (!departmentId) {
      setYearOptions([]);
      return;
    }
    try {
      const classes = await api.searchAcademicClasses({ departmentId: Number(departmentId) });
      const unique = [...new Set(classes.map((c) => c.yearName))].filter(Boolean);
      setYearOptions(unique);
    } catch {
      setYearOptions([]);
    }
  };

  const handleDeptChange = (v: string) => {
    setDeptFilter(v);
    setYearFilter("");
    loadYearOptions(v);
  };

  const load = useCallback(() => {
    setLoading(true);
    const p =
      kind === "classes"
        ? api.searchAcademicClasses()
        : kind === "sections"
          ? api.searchSections()
          : api.searchSubjects({
              ...(deptFilter ? { departmentId: Number(deptFilter) } : {}),
              ...(yearFilter ? { yearName: yearFilter } : {}),
            });
    p.then(setRows)
      .catch((e) => toast.error(handleApiError(e).message))
      .finally(() => setLoading(false));
  }, [deptFilter, kind, yearFilter]);
  useEffect(() => {
    load();
  }, [load]);
  return (
    <Shell title={kind[0].toUpperCase() + kind.slice(1)} subtitle={`Manage academic ${kind}.`}>
      {kind === "subjects" ? (
        <div className="mb-6 space-y-4">
          <div className="flex flex-col gap-4 rounded-2xl bg-gradient-to-r from-brand-700 to-brand-500 p-5 text-white sm:flex-row sm:items-center sm:justify-between sm:p-6">
            <div className="flex min-w-0 items-center gap-4">
              <div className="grid h-12 w-12 shrink-0 place-items-center rounded-xl bg-white/15">
                <BookOpen className="h-6 w-6" />
              </div>
              <div className="min-w-0">
                <h2 className="text-lg font-semibold sm:text-xl">Subject catalogue</h2>
                <p className="mt-1 text-sm text-white/80">
                  Organise subjects by department and course year.
                </p>
              </div>
            </div>
            <Button
              className="w-full shrink-0 bg-white text-brand-700 hover:bg-brand-50 sm:w-auto"
              onClick={() => navigate("/academic/subjects/create")}
            >
              <Plus className="h-4 w-4" /> Create subject
            </Button>
          </div>
          <div className="rounded-2xl border border-slate-200 bg-slate-50/70 p-4">
            <div className="mb-3 flex items-center gap-2 text-sm font-semibold text-slate-700">
              <Filter className="h-4 w-4 text-brand-600" /> Filter subjects
            </div>
            <div className="grid min-w-0 gap-3 sm:grid-cols-2">
              <Select
                label="Department"
                value={deptFilter}
                onChange={(e) => handleDeptChange(e.target.value)}
                options={[
                  { label: "All departments", value: "" },
                  ...departments.map((d) => ({
                    label: `${d.code} - ${d.name}`,
                    value: String(d.id),
                  })),
                ]}
              />
              <Select
                label="Course year"
                value={yearFilter}
                onChange={(e) => setYearFilter(e.target.value)}
                disabled={!deptFilter}
                options={[
                  {
                    label: deptFilter ? "All course years" : "Select a department first",
                    value: "",
                  },
                  ...yearOptions.map((y) => ({ label: y.replaceAll("_", " "), value: y })),
                ]}
              />
            </div>
          </div>
        </div>
      ) : (
        <div className="mb-4">
          <Button onClick={() => navigate(`/academic/${kind}/create`)}>
            <Plus className="h-4 w-4" /> Create new
          </Button>
        </div>
      )}
      {loading ? (
        <Loader />
      ) : rows.length ? (
        <div className="grid gap-3 md:grid-cols-2">
          {rows.map((r) => (
            <div
              key={r.id}
              className="min-w-0 rounded-xl border border-slate-200 p-4 transition hover:border-brand-200 hover:shadow-sm"
            >
              <div className="flex min-w-0 flex-col gap-4 sm:flex-row sm:items-start sm:justify-between">
                <div className="min-w-0">
                  <b className="block break-words text-slate-900">{r.name}</b>
                  <p className="mt-1 break-words text-sm text-slate-500">
                    {r.code} · {r.academicYear}
                    {"subjectType" in r && r.subjectType ? (
                      <span className="ml-2 rounded-full bg-blue-50 px-2 py-0.5 text-xs text-blue-700">
                        {r.subjectType}
                      </span>
                    ) : null}
                    {"semesterNumber" in r && r.semesterNumber ? (
                      <span className="ml-2 rounded-full bg-emerald-50 px-2 py-0.5 text-xs text-emerald-700">
                        Semester {r.semesterNumber}
                      </span>
                    ) : null}
                  </p>
                </div>
                {kind === "subjects" && (
                  <div className="table-action-group sm:w-auto">
                    <Button
                      className="h-9 flex-1 px-3 text-xs sm:flex-none"
                      onClick={() => navigate(`/academic/subjects/${r.id}/edit`)}
                    >
                      Edit
                    </Button>
                    <Button
                      className="h-9 flex-1 px-3 text-xs sm:flex-none"
                      variant="danger"
                      onClick={async () => {
                        if (
                          !confirm(
                            "Remove this subject? Existing timetable and attendance history will be preserved.",
                          )
                        )
                          return;
                        try {
                          await api.deleteSubject(r.id);
                          toast.success("Subject removed");
                          load();
                        } catch (e) {
                          toast.error(handleApiError(e).message);
                        }
                      }}
                    >
                      Delete
                    </Button>
                  </div>
                )}
              </div>
            </div>
          ))}
        </div>
      ) : (
        <EmptyState
          title={`No ${kind} found`}
          description={
            kind === "subjects"
              ? "Create a subject or adjust the department and course-year filters."
              : "Create the first record to begin."
          }
          action={
            kind === "subjects" ? (
              <Button onClick={() => navigate("/academic/subjects/create")}>
                <Plus className="h-4 w-4" /> Create subject
              </Button>
            ) : undefined
          }
        />
      )}
    </Shell>
  );
}
export function AcademicCreatePage({ kind }: { kind: Kind }) {
  const navigate = useNavigate();
  const { user } = useAuth();
  const [v, setV] = useState<Record<string, string>>({ academicYear: "2026-27" });
  const [departments, setDepartments] = useState<Department[]>([]);
  const [academicClasses, setAcademicClasses] = useState<AcademicClass[]>([]);

  const input = (name: string, label: string) => (
    <Input
      label={label}
      value={v[name] || ""}
      onChange={(e) => setV({ ...v, [name]: e.target.value })}
    />
  );

  useEffect(() => {
    if (kind !== "subjects") return;
    if (!user?.collegeId) {
      setDepartments([]);
      return;
    }
    Promise.all([
      searchDepartments({ collegeId: user.collegeId, status: "ACTIVE", page: 0, size: 100 }),
      api.searchAcademicClasses(),
    ])
      .then(([result, classes]) => setDepartments(preferredDepartments(result.content, classes)))
      .catch((error) => toast.error(handleApiError(error).message));
  }, [kind, user?.collegeId]);

  const loadAcademicClasses = async (departmentId: string) => {
    if (!departmentId) {
      setAcademicClasses([]);
      return;
    }
    try {
      const items = await api.searchAcademicClasses({ departmentId: Number(departmentId) });
      setAcademicClasses(uniqueAcademicClasses(items.filter((item) => item.status === "ACTIVE")));
    } catch (error) {
      setAcademicClasses([]);
      toast.error(handleApiError(error).message);
    }
  };

  const handleDepartmentChange = (departmentId: string) => {
    setV({
      ...v,
      departmentId,
      academicClassId: "",
      academicYear: "",
      semesterNumber: "",
    });
    loadAcademicClasses(departmentId);
  };

  const handleClassChange = (academicClassId: string) => {
    const selectedClass = academicClasses.find((item) => item.id === Number(academicClassId));
    setV({
      ...v,
      academicClassId,
      academicYear: selectedClass?.academicYear ?? "",
      semesterNumber: "",
    });
  };

  const save = async () => {
    try {
      if (kind === "classes")
        await api.createAcademicClass({
          collegeId: +v.collegeId,
          departmentId: +v.departmentId,
          academicYear: v.academicYear,
          name: v.name,
          code: v.code,
          description: v.description,
        });
      else if (kind === "sections")
        await api.createSection({
          academicClassId: +v.academicClassId,
          academicYear: v.academicYear,
          name: v.name,
          code: v.code,
          capacity: +v.capacity,
        });
      else
        await api.createSubject({
          academicClassId: +v.academicClassId,
          semesterNumber: +v.semesterNumber,
          academicYear: v.academicYear,
          name: v.name,
          code: v.code,
          credits: +v.credits || 0,
          description: v.description,
          subjectType: v.subjectType || undefined,
        });
      toast.success("Created successfully");
      navigate(`/academic/${kind}`, { replace: true });
    } catch (e) {
      toast.error(handleApiError(e).message);
    }
  };

  return (
    <Shell title={`Create ${kind.slice(0, -1)}`} subtitle="Codes are normalized to uppercase.">
      <div className="grid gap-4 md:grid-cols-2">
        {kind === "classes" ? (
          <>
            {input("collegeId", "College ID")}
            {input("departmentId", "Department ID")}
          </>
        ) : kind === "subjects" ? (
          <>
            <Select
              label="Department"
              value={v.departmentId || ""}
              onChange={(e) => handleDepartmentChange(e.target.value)}
              options={[
                { label: "Select department", value: "" },
                ...departments.map((item) => ({
                  label: `${item.code} - ${item.name}`,
                  value: item.id,
                })),
              ]}
            />
            <Select
              label="Year / Class"
              value={v.academicClassId || ""}
              onChange={(e) => handleClassChange(e.target.value)}
              options={[
                { label: "Select year", value: "" },
                ...academicClasses.map((item) => ({
                  label: `${item.academicYear} - ${item.name}`,
                  value: item.id,
                })),
              ]}
            />
            <Select
              label="Semester"
              value={v.semesterNumber || ""}
              disabled={!v.academicClassId}
              onChange={(e) => setV({ ...v, semesterNumber: e.target.value })}
              options={[
                {
                  label: v.academicClassId ? "Select semester" : "Select year/class first",
                  value: "",
                },
                ...semesterNumbersFor(
                  academicClasses.find((item) => item.id === Number(v.academicClassId))?.yearName,
                ).map((number) => ({ label: `Semester ${number}`, value: String(number) })),
              ]}
            />
          </>
        ) : (
          input("academicClassId", "Academic class ID")
        )}
        <Input
          label="Academic year"
          value={v.academicYear || ""}
          disabled={kind === "subjects"}
          onChange={(e) => setV({ ...v, academicYear: e.target.value })}
        />
        {input("name", "Name")}
        {input("code", "Code")}
        {kind === "sections" ? (
          input("capacity", "Capacity")
        ) : kind === "subjects" ? (
          <>
            {input("credits", "Credits")}
            <Select
              label="Subject Type"
              value={v.subjectType || ""}
              onChange={(e) => setV({ ...v, subjectType: e.target.value })}
              options={[
                { label: "Select type", value: "" },
                { label: "Theory", value: "THEORY" },
                { label: "Practical", value: "PRACTICAL" },
                { label: "Other (Soft Skill etc.)", value: "OTHER" },
              ]}
            />
          </>
        ) : (
          input("description", "Description")
        )}
      </div>
      <Button
        className="mt-5"
        disabled={kind === "subjects" && (!v.academicClassId || !v.semesterNumber)}
        onClick={save}
      >
        Create
      </Button>
    </Shell>
  );
}
export function SubjectEditPage() {
  const navigate = useNavigate();
  const { id } = useParams<{ id: string }>();
  const [v, setV] = useState<Record<string, string>>({});
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    if (!id) return;
    api
      .searchSubjects()
      .then((rows) => {
        const sub = rows.find((s) => s.id === Number(id));
        if (sub) {
          setV({
            name: sub.name,
            code: sub.code,
            description: sub.description || "",
            credits: String(sub.credits ?? ""),
            subjectType: sub.subjectType || "",
          });
        }
      })
      .finally(() => setLoading(false));
  }, [id]);

  const save = async () => {
    try {
      await api.updateSubject(Number(id), {
        name: v.name,
        code: v.code,
        description: v.description,
        credits: +v.credits || 0,
        subjectType: v.subjectType || undefined,
      });
      toast.success("Subject updated");
      navigate("/academic/subjects", { replace: true });
    } catch (e) {
      toast.error(handleApiError(e).message);
    }
  };

  if (loading)
    return (
      <Shell title="Edit Subject" subtitle="Loading...">
        <Loader />
      </Shell>
    );

  return (
    <Shell title="Edit Subject" subtitle="Update subject details.">
      <div className="grid gap-4 md:grid-cols-2">
        <Input
          label="Name"
          value={v.name || ""}
          onChange={(e) => setV({ ...v, name: e.target.value })}
        />
        <Input
          label="Code"
          value={v.code || ""}
          onChange={(e) => setV({ ...v, code: e.target.value })}
        />
        <Input
          label="Credits"
          value={v.credits || ""}
          onChange={(e) => setV({ ...v, credits: e.target.value })}
        />
        <Select
          label="Subject Type"
          value={v.subjectType || ""}
          onChange={(e) => setV({ ...v, subjectType: e.target.value })}
          options={[
            { label: "Select type", value: "" },
            { label: "Theory", value: "THEORY" },
            { label: "Practical", value: "PRACTICAL" },
            { label: "Other (Soft Skill etc.)", value: "OTHER" },
          ]}
        />
        <Input
          label="Description"
          value={v.description || ""}
          onChange={(e) => setV({ ...v, description: e.target.value })}
        />
      </div>
      <Button className="mt-5" onClick={save}>
        Update
      </Button>
    </Shell>
  );
}

export function StudentAcademicPage({ attendance = false }: { attendance?: boolean }) {
  const [data, setData] = useState<Record<string, number> | null>(null);
  useEffect(() => {
    if (!attendance) return;
    api
      .getMyStudentAttendanceSummary()
      .then(setData)
      .catch((e) => toast.error(handleApiError(e).message));
  }, [attendance]);
  if (!attendance) return <StudentWeeklyTimetable />;
  return (
    <Shell title="My Attendance" subtitle="Your current academic section information.">
      {!data ? (
        <Loader />
      ) : (
        <pre className="overflow-auto rounded-xl bg-slate-50 p-4 text-sm">
          {JSON.stringify(data, null, 2)}
        </pre>
      )}
    </Shell>
  );
}

const STUDENT_TIMETABLE_DAYS = ["MONDAY", "TUESDAY", "WEDNESDAY", "THURSDAY", "FRIDAY", "SATURDAY"];
const STUDENT_TIMETABLE_COLORS = [
  "border-blue-200 bg-blue-50 text-blue-950",
  "border-emerald-200 bg-emerald-50 text-emerald-950",
  "border-orange-200 bg-orange-50 text-orange-950",
  "border-violet-200 bg-violet-50 text-violet-950",
  "border-cyan-200 bg-cyan-50 text-cyan-950",
];

function StudentWeeklyTimetable() {
  const [table, setTable] = useState<WeeklyTimetable | null>(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState("");
  useEffect(() => {
    api
      .getMyStudentTimetable()
      .then(setTable)
      .catch((e) => setError(handleApiError(e).message))
      .finally(() => setLoading(false));
  }, []);
  const entries = useMemo(
    () =>
      new Map(
        (table?.entries ?? []).map((entry) => [`${entry.dayOfWeek}:${entry.periodId}`, entry]),
      ),
    [table],
  );
  return (
    <Shell title="My Timetable" subtitle="Your weekly timetable for the allocated division.">
      {loading ? (
        <Loader />
      ) : error || !table ? (
        <EmptyState
          title="Timetable not available"
          description={error || "A timetable has not been created for your division yet."}
        />
      ) : (
        <div className="space-y-4">
          <div className="flex flex-wrap items-center justify-between gap-3 rounded-2xl border border-blue-100 bg-blue-50 p-4">
            <div className="flex items-center gap-3">
              <div className="grid h-11 w-11 place-items-center rounded-xl bg-blue-600 text-white">
                <CalendarDays className="h-5 w-5" />
              </div>
              <div>
                <h2 className="font-bold text-slate-900">
                  {table.year} - Division {table.division}
                </h2>
                <p className="text-xs text-slate-500">
                  {table.department} | {table.academicYear}
                </p>
              </div>
            </div>
            <span className="rounded-full bg-white px-3 py-1 text-xs font-semibold text-blue-700">
              Class teacher: {table.classTeacher}
            </span>
          </div>
          {!table.periods.length ? (
            <EmptyState
              title="No timetable configured"
              description="Periods have not been configured yet."
            />
          ) : (
            <div className="overflow-x-auto rounded-2xl border bg-white shadow-sm">
              <div className="min-w-[1080px]">
                <div className="grid grid-cols-[145px_repeat(6,minmax(150px,1fr))] bg-slate-100 text-center text-xs font-bold uppercase tracking-wide text-slate-500">
                  <div className="p-3 text-left">Period</div>
                  {STUDENT_TIMETABLE_DAYS.map((day) => (
                    <div className="border-l p-3" key={day}>
                      {day.slice(0, 3)}
                    </div>
                  ))}
                </div>
                {table.periods.map((period) => (
                  <div
                    key={period.id}
                    className={`grid grid-cols-[145px_repeat(6,minmax(150px,1fr))] border-t ${period.kind !== "TEACHING" ? "bg-amber-50/70" : ""}`}
                  >
                    <div
                      className={`p-3 ${period.kind !== "TEACHING" ? "bg-amber-50" : "bg-white"}`}
                    >
                      <b className="text-sm text-slate-800">{period.label}</b>
                      <p className="mt-1 text-[10px] text-slate-400">
                        {period.startTime.slice(0, 5)}-{period.endTime.slice(0, 5)}
                      </p>
                    </div>
                    {period.kind !== "TEACHING" ? (
                      <div className="col-span-6 flex items-center justify-center border-l p-4 text-xs font-bold tracking-[.18em] text-amber-700">
                        {period.label.toUpperCase()}
                      </div>
                    ) : (
                      STUDENT_TIMETABLE_DAYS.map((day) => {
                        const entry = entries.get(`${day}:${period.id}`);
                        return (
                          <div className="min-h-24 border-l p-2" key={day}>
                            {entry ? (
                              <div
                                className={`h-full rounded-xl border p-2.5 shadow-sm ${STUDENT_TIMETABLE_COLORS[entry.subjectId % STUDENT_TIMETABLE_COLORS.length]}`}
                              >
                                <b className="text-xs leading-5">{entry.subject}</b>
                                {entry.substituted && (
                                  <span className="mt-1 block text-[10px] font-bold uppercase tracking-wide text-violet-700">
                                    Substitute today
                                  </span>
                                )}
                                <p className="mt-1 truncate text-[10px] opacity-75">
                                  {entry.teacher}
                                </p>
                                <p className="mt-1 text-[10px] font-semibold">
                                  {entry.lectureType}
                                  {entry.room ? ` - ${entry.room}` : ""}
                                </p>
                              </div>
                            ) : (
                              <div className="grid h-full place-items-center text-xs text-slate-300">
                                -
                              </div>
                            )}
                          </div>
                        );
                      })
                    )}
                  </div>
                ))}
              </div>
            </div>
          )}
        </div>
      )}
    </Shell>
  );
}
