import { useEffect, useState } from "react";
import { toast } from "sonner";
import { Card } from "@/components/common/Card";
import { Button } from "@/components/common/Button";
import { Input } from "@/components/common/Input";
import { EmptyState } from "@/components/common/EmptyState";
import { Loader } from "@/components/common/Loader";
import { Select } from "@/components/common/Select";
import { handleApiError } from "@/lib/handleApiError";
import * as api from "@/features/academic/api";
import { searchDepartments } from "@/features/departments/api";
import type { Department } from "@/features/departments/types";
import type {
  AcademicClass,
  FinalAdmission,
  Section,
  Subject,
  TimetableEntry,
} from "@/features/academic/types";
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
      <Card className="mt-6 p-5">{children}</Card>
    </div>
  );
}
export function FinalAdmissionsPage() {
  const [rows, setRows] = useState<FinalAdmission[]>([]),
    [loading, setLoading] = useState(true);
  const load = () =>
    api
      .getFinalAdmissionQueue()
      .then(setRows)
      .catch((e) => toast.error(handleApiError(e).message))
      .finally(() => setLoading(false));
  useEffect(() => {
    void load();
  }, []);
  const act = async (id: number, approve: boolean) => {
    const text = window.prompt(approve ? "Approval remarks (optional)" : "Rejection reason");
    if (!approve && (!text || text.length < 5)) return;
    try {
      if (approve) {
        await api.approveFinalAdmission(id, { remarks: text });
      } else {
        await api.rejectFinalAdmission(id, { rejectionReason: text });
      }
      toast.success(approve ? "Admission approved" : "Admission rejected");
      load();
    } catch (e) {
      toast.error(handleApiError(e).message);
    }
  };
  return (
    <Shell
      title="Principal Final Admissions"
      subtitle="Approve fee-verified students and activate their academic profile."
    >
      {loading ? (
        <Loader />
      ) : rows.length ? (
        <div className="w-full overflow-hidden">
          <table className="w-full table-fixed text-xs sm:text-sm">
            <thead>
              <tr className="text-left text-slate-500">
                <th className="p-3">Student</th>
                <th>Department</th>
                <th>Year</th>
                <th>Actions</th>
              </tr>
            </thead>
            <tbody>
              {rows.map((r) => (
                <tr className="border-t" key={r.id}>
                  <td className="p-3">
                    <b>{r.fullName}</b>
                    <div>{r.admissionReferenceNumber}</div>
                  </td>
                  <td>{r.departmentName}</td>
                  <td>{r.academicYear}</td>
                  <td className="space-x-2">
                    <Button onClick={() => act(r.id, true)}>Approve</Button>
                    <Button variant="danger" onClick={() => act(r.id, false)}>
                      Reject
                    </Button>
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      ) : (
        <EmptyState
          title="No admissions ready"
          description="Fee-verified admissions will appear here."
        />
      )}
    </Shell>
  );
}
type Kind = "classes" | "sections" | "subjects";
export function AcademicListPage({ kind }: { kind: Kind }) {
  const [rows, setRows] = useState<(AcademicClass | Section | Subject)[]>([]),
    [loading, setLoading] = useState(true);
  const load = () => {
    setLoading(true);
    const p =
      kind === "classes"
        ? api.searchAcademicClasses()
        : kind === "sections"
          ? api.searchSections()
          : api.searchSubjects();
    p.then(setRows)
      .catch((e) => toast.error(handleApiError(e).message))
      .finally(() => setLoading(false));
  };
  useEffect(() => {
    load();
  }, [kind]);
  return (
    <Shell title={kind[0].toUpperCase() + kind.slice(1)} subtitle={`Manage academic ${kind}.`}>
      <div className="mb-4">
        <Button onClick={() => (location.href = `/academic/${kind}/create`)}>Create new</Button>
      </div>
      {loading ? (
        <Loader />
      ) : rows.length ? (
        <div className="grid gap-3 md:grid-cols-2">
          {rows.map((r) => (
            <div key={r.id} className="rounded-xl border p-4">
              <b>{r.name}</b>
              <p className="text-sm text-slate-500">
                {r.code} · {r.academicYear}
              </p>
            </div>
          ))}
        </div>
      ) : (
        <EmptyState title={`No ${kind}`} description="Create the first record to begin." />
      )}
    </Shell>
  );
}
export function AcademicCreatePage({ kind }: { kind: Kind }) {
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
    searchDepartments({ status: "ACTIVE", page: 0, size: 100 })
      .then((result) => setDepartments(result.content))
      .catch((error) => toast.error(handleApiError(error).message));
  }, [kind]);

  const loadAcademicClasses = async (departmentId: string) => {
    if (!departmentId) {
      setAcademicClasses([]);
      return;
    }
    try {
      const items = await api.searchAcademicClasses({ departmentId: Number(departmentId) });
      setAcademicClasses(items);
    } catch (error) {
      setAcademicClasses([]);
      toast.error(handleApiError(error).message);
    }
  };

  const handleDepartmentChange = (departmentId: string) => {
    setV({ ...v, departmentId, academicClassId: "", academicYear: "" });
    loadAcademicClasses(departmentId);
  };

  const handleClassChange = (academicClassId: string) => {
    const selectedClass = academicClasses.find((item) => item.id === Number(academicClassId));
    setV({
      ...v,
      academicClassId,
      academicYear: selectedClass?.academicYear ?? "",
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
          academicYear: v.academicYear,
          name: v.name,
          code: v.code,
          credits: +v.credits || 0,
          description: v.description,
        });
      toast.success("Created successfully");
      location.href = `/academic/${kind}`;
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
          input("credits", "Credits")
        ) : (
          input("description", "Description")
        )}
      </div>
      <Button className="mt-5" onClick={save}>
        Create
      </Button>
    </Shell>
  );
}
export function StudentAcademicPage({ attendance = false }: { attendance?: boolean }) {
  const [data, setData] = useState<TimetableEntry[] | Record<string, number> | null>(null);
  useEffect(() => {
    (attendance ? api.getMyStudentAttendanceSummary() : api.getMyStudentTimetable())
      .then(setData)
      .catch((e) => toast.error(handleApiError(e).message));
  }, [attendance]);
  return (
    <Shell
      title={attendance ? "My Attendance" : "My Timetable"}
      subtitle="Your current academic section information."
    >
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
