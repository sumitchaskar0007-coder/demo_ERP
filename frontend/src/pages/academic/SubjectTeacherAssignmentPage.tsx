import { useCallback, useEffect, useState } from "react";
import { Check, Search, UserRound, X } from "lucide-react";
import { toast } from "sonner";
import { Button } from "@/components/common/Button";
import { Card } from "@/components/common/Card";
import { EmptyState } from "@/components/common/EmptyState";
import { Input } from "@/components/common/Input";
import { Loader } from "@/components/common/Loader";
import { Select } from "@/components/common/Select";
import { handleApiError } from "@/lib/handleApiError";
import { useAuth } from "@/features/auth/authStore";
import { searchStaff } from "@/features/staff/api";
import type { StaffResponse } from "@/features/staff/types";
import { searchDepartments } from "@/features/departments/api";
import type { Department } from "@/features/departments/types";
import {
  listSubjectTeacherAssignments,
  assignSubjectTeacher,
  unassignSubjectTeacher,
  searchAcademicClasses,
  searchSubjects,
} from "@/features/academic/api";
import type { Subject, SubjectTeacherAssignment } from "@/features/academic/types";

const TEACHING_TYPES = ["HOD", "TEACHER", "CLASS_TEACHER", "SUBJECT_TEACHER"];

export function SubjectTeacherAssignmentPage() {
  const { user } = useAuth();
  const [teachers, setTeachers] = useState<StaffResponse[]>([]);
  const [selectedTeacher, setSelectedTeacher] = useState<StaffResponse | null>(null);
  const [subjects, setSubjects] = useState<Subject[]>([]);
  const [assignments, setAssignments] = useState<SubjectTeacherAssignment[]>([]);
  const [departments, setDepartments] = useState<Department[]>([]);
  const [loading, setLoading] = useState(true);
  const [saving, setSaving] = useState<number | null>(null);
  const [search, setSearch] = useState("");
  const [departmentId, setDepartmentId] = useState<number | "">("");
  const [yearName, setYearName] = useState("");
  const [yearOptions, setYearOptions] = useState<string[]>([]);

  const loadDepartments = useCallback(async () => {
    try {
      const result = await searchDepartments({
        collegeId: user?.collegeId ?? undefined,
        size: 100,
        status: "ACTIVE" as never,
      });
      setDepartments([
        ...new Map(result.content.map((department) => [
          `${department.collegeId}:${department.code.trim().toUpperCase()}`,
          department,
        ])).values(),
      ]);
    } catch {
      setDepartments([]);
    }
  }, [user?.collegeId]);

  const loadYears = useCallback(async (deptId: number | "") => {
    setYearName("");
    if (!deptId) {
      setYearOptions([]);
      return;
    }
    try {
      const classes = await searchAcademicClasses({ departmentId: Number(deptId) });
      setYearOptions([
        ...new Set(
          classes
            .filter((item) => item.status === "ACTIVE")
            .map((item) => item.yearName),
        ),
      ]);
    } catch (error) {
      setYearOptions([]);
      toast.error(handleApiError(error).message);
    }
  }, []);

  const loadTeachers = useCallback(async () => {
    setLoading(true);
    try {
      const result = await searchStaff({
        staffType: "" as never,
        status: "ACTIVE" as never,
        size: 100,
      });
      const teaching = result.content.filter((s) => TEACHING_TYPES.includes(s.staffType));
      setTeachers(teaching);
    } catch (e) {
      toast.error(handleApiError(e).message);
    } finally {
      setLoading(false);
    }
  }, []);

  const loadSubjects = useCallback(async (deptId?: number | "", yName?: string) => {
    try {
      const params: Record<string, unknown> = {};
      if (deptId) params.departmentId = Number(deptId);
      if (yName) params.yearName = yName;
      const data = await searchSubjects(params);
      setSubjects(data as Subject[]);
    } catch {
      setSubjects([]);
    }
  }, []);

  const loadAssignments = useCallback(async (teacherId: number) => {
    try {
      const data = await listSubjectTeacherAssignments({ teacherId });
      setAssignments(data as SubjectTeacherAssignment[]);
    } catch {
      setAssignments([]);
    }
  }, []);

  useEffect(() => {
    void loadDepartments();
    void loadTeachers();
    void loadSubjects();
  }, [loadDepartments, loadTeachers, loadSubjects]);

  useEffect(() => {
    void loadTeachers();
    void loadSubjects(departmentId, yearName);
  }, [departmentId, yearName, loadTeachers, loadSubjects]);

  useEffect(() => {
    if (selectedTeacher) void loadAssignments(selectedTeacher.id);
  }, [selectedTeacher, loadAssignments]);

  const isAssigned = (subjectId: number) => assignments.some((a) => a.subjectId === subjectId);

  const toggle = async (subject: Subject) => {
    if (!selectedTeacher) return;
    const assigned = isAssigned(subject.id);
    setSaving(subject.id);
    try {
      if (assigned) {
        await unassignSubjectTeacher(subject.id, selectedTeacher.id);
        setAssignments((prev) =>
          prev.filter((a) => !(a.subjectId === subject.id && a.teacherId === selectedTeacher.id)),
        );
        toast.success(`${subject.name} unassigned from ${selectedTeacher.fullName}`);
      } else {
        await assignSubjectTeacher(subject.id, { staffProfileId: selectedTeacher.id });
        setAssignments((prev) => [
          ...prev,
          {
            id: Date.now(),
            subjectId: subject.id,
            subjectName: subject.name,
            subjectCode: subject.code,
            teacherId: selectedTeacher.id,
            teacherName: selectedTeacher.fullName,
            academicYear: subject.academicYear,
          },
        ]);
        toast.success(`${subject.name} assigned to ${selectedTeacher.fullName}`);
      }
    } catch (e) {
      toast.error(handleApiError(e).message);
    } finally {
      setSaving(null);
    }
  };

  const filteredTeachers = teachers.filter(
    (t) =>
      t.fullName.toLowerCase().includes(search.toLowerCase()) ||
      t.email.toLowerCase().includes(search.toLowerCase()),
  );

  if (loading) return <Loader label="Loading subject-teacher assignments…" />;

  return (
    <div className="page-container">
      <div className="mb-6">
        <div className="mb-2 flex items-center gap-2 text-sm font-semibold text-brand-600">
          <UserRound className="h-4 w-4" /> Academic Planning
        </div>
        <h1 className="page-title">Teaching Assignments</h1>
        <p className="page-subtitle">
          Assign subjects to teaching staff. One teacher can teach multiple subjects, and one
          subject can have multiple teachers.
        </p>
      </div>

      <Card className="mb-6 p-4">
        <div className="grid gap-4 sm:grid-cols-2">
          <Select
            label="Department"
            value={departmentId}
            onChange={(e) => {
              const val = e.target.value === "" ? "" : Number(e.target.value);
              setDepartmentId(val);
              void loadYears(val);
            }}
            options={[
              { label: "All Departments", value: "" },
              ...departments.map((d) => ({ label: `${d.code} - ${d.name}`, value: d.id })),
            ]}
          />
          <Select
            label="Year"
            value={yearName}
            onChange={(e) => setYearName(e.target.value)}
            disabled={!departmentId}
            options={[
              {
                label: departmentId ? "All created course years" : "Select a department first",
                value: "",
              },
              ...yearOptions.map((y) => ({ label: y.replaceAll("_", " "), value: y })),
            ]}
          />
        </div>
      </Card>

      <div className="grid gap-6 lg:grid-cols-[380px_1fr]">
        <Card className="flex max-h-[calc(100vh-16rem)] flex-col p-0">
          <div className="border-b px-4 py-3">
            <Input
              placeholder="Search teachers..."
              value={search}
              onChange={(e) => setSearch(e.target.value)}
              icon={<Search className="h-4 w-4" />}
            />
          </div>
          <div className="flex-1 divide-y overflow-y-auto">
            {filteredTeachers.length === 0 && (
              <EmptyState
                title="No teachers found"
                description="No active teaching staff available."
              />
            )}
            {filteredTeachers.map((teacher) => (
              <button
                key={teacher.id}
                onClick={() => setSelectedTeacher(teacher)}
                className={`flex w-full items-center gap-3 px-4 py-3 text-left transition hover:bg-slate-50 ${
                  selectedTeacher?.id === teacher.id ? "bg-brand-50" : ""
                }`}
              >
                <div className="flex h-9 w-9 shrink-0 items-center justify-center rounded-full bg-brand-100 text-sm font-bold text-brand-700">
                  {teacher.fullName.charAt(0)}
                </div>
                <div className="min-w-0 flex-1">
                  <p className="truncate text-sm font-semibold text-slate-800">
                    {teacher.fullName}
                  </p>
                  <p className="truncate text-xs text-slate-400">
                    {teacher.departmentName || "No department"} &middot;{" "}
                    {teacher.staffType.replace("_", " ")}
                  </p>
                </div>
                {selectedTeacher?.id === teacher.id && (
                  <div className="h-2 w-2 shrink-0 rounded-full bg-brand-500" />
                )}
              </button>
            ))}
          </div>
        </Card>

        <Card className="p-0">
          {!selectedTeacher ? (
            <div className="flex h-full min-h-[20rem] items-center justify-center">
              <EmptyState
                title="Select a teacher"
                description="Click on a teacher from the list to manage their subject assignments."
              />
            </div>
          ) : (
            <>
              <div className="border-b px-5 py-4">
                <div className="flex items-center justify-between">
                  <div>
                    <h2 className="text-lg font-bold text-slate-900">{selectedTeacher.fullName}</h2>
                    <p className="text-sm text-slate-500">
                      {selectedTeacher.departmentName || "No department"} &middot;{" "}
                      {selectedTeacher.staffType.replace("_", " ")} &middot; {assignments.length}{" "}
                      subject{assignments.length !== 1 ? "s" : ""} assigned
                    </p>
                  </div>
                  <Button variant="secondary" onClick={() => setSelectedTeacher(null)}>
                    <X className="h-4 w-4" />
                  </Button>
                </div>
              </div>
              <div className="divide-y">
                {subjects.length === 0 && (
                  <EmptyState
                    title="No subjects"
                    description="Select a department and year, or create subjects first."
                  />
                )}
                {subjects.map((subject) => {
                  const assigned = isAssigned(subject.id);
                  return (
                    <button
                      key={subject.id}
                      onClick={() => toggle(subject)}
                      disabled={saving === subject.id}
                      className="flex w-full items-center gap-4 px-5 py-3.5 text-left transition hover:bg-slate-50"
                    >
                      <div
                        className={`flex h-6 w-6 shrink-0 items-center justify-center rounded-md border-2 transition ${
                          assigned ? "border-brand-500 bg-brand-500" : "border-slate-300"
                        }`}
                      >
                        {assigned && <Check className="h-4 w-4 text-white" />}
                      </div>
                      <div className="min-w-0 flex-1">
                        <p className="text-sm font-semibold text-slate-800">
                          {subject.code} - {subject.name}
                        </p>
                        <p className="text-xs text-slate-400">
                          {subject.academicClass?.name || "N/A"} &middot;{" "}
                          {subject.subjectType || "General"}
                        </p>
                      </div>
                      {saving === subject.id && (
                        <span className="text-xs text-slate-400">Saving…</span>
                      )}
                    </button>
                  );
                })}
              </div>
            </>
          )}
        </Card>
      </div>
    </div>
  );
}
