import { useEffect, useState } from "react";
import { toast } from "sonner";
import { Button } from "@/components/common/Button";
import { Card } from "@/components/common/Card";
import { EmptyState } from "@/components/common/EmptyState";
import { Loader } from "@/components/common/Loader";
import { Select } from "@/components/common/Select";
import { handleApiError } from "@/lib/handleApiError";
import * as api from "@/features/academic/api";
import type { AcademicClass, Section } from "@/features/academic/types";

function Page({
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

export function StudentAllocationPage() {
  const [classes, setClasses] = useState<AcademicClass[]>([]);
  const [sections, setSections] = useState<Section[]>([]);
  const [classId, setClassId] = useState("");
  const [sectionId, setSectionId] = useState("");
  const [students, setStudents] = useState<api.StudentRosterItem[]>([]);
  const [loading, setLoading] = useState(false);
  useEffect(() => {
    api
      .searchAcademicClasses()
      .then(setClasses)
      .catch((e) => toast.error(handleApiError(e).message));
  }, []);
  useEffect(() => {
    if (!classId) {
      setSections([]);
      setStudents([]);
      return;
    }
    api
      .searchSections({ classId: Number(classId) })
      .then(setSections)
      .catch((e) => toast.error(handleApiError(e).message));
    api
      .eligibleStudentsForClass(Number(classId))
      .then(setStudents)
      .catch((e) => toast.error(handleApiError(e).message));
  }, [classId]);
  const assign = async (studentProfileId: number) => {
    if (!sectionId) return toast.error("Select a division first");
    setLoading(true);
    try {
      await api.assignStudentToSection(Number(sectionId), { studentProfileId });
      toast.success("Student allocated to division");
      setStudents((rows) => rows.filter((s) => s.studentProfileId !== studentProfileId));
    } catch (e) {
      toast.error(handleApiError(e).message);
    } finally {
      setLoading(false);
    }
  };
  return (
    <Page
      title="Allocate Students"
      subtitle="Choose a course year and division, then place each confirmed student into that division."
    >
      <div className="grid gap-3 md:grid-cols-2">
        <Select
          label="Course year / class"
          value={classId}
          onChange={(e) => {
            setClassId(e.target.value);
            setSectionId("");
          }}
          options={[
            { label: "Select class", value: "" },
            ...classes.map((c) => ({
              label: `${c.department.name} · ${c.name} (${c.academicYear})`,
              value: c.id,
            })),
          ]}
        />
        <Select
          label="Division"
          value={sectionId}
          onChange={(e) => setSectionId(e.target.value)}
          options={[
            { label: "Select division", value: "" },
            ...sections.map((s) => ({
              label: `${s.name} (${s.code}) · capacity ${s.capacity}`,
              value: s.id,
            })),
          ]}
        />
      </div>
      {classId && (
        <div className="mt-6 w-full overflow-hidden">
          <table className="w-full table-fixed text-xs sm:text-sm">
            <thead>
              <tr className="text-left text-slate-500">
                <th className="p-3">Student</th>
                <th>Admission / Roll</th>
                <th>Email</th>
                <th />
              </tr>
            </thead>
            <tbody>
              {students.map((s) => (
                <tr className="border-t" key={s.studentProfileId}>
                  <td className="p-3 font-medium">{s.fullName}</td>
                  <td>
                    {s.admissionNumber}
                    {s.rollNumber ? ` · ${s.rollNumber}` : ""}
                  </td>
                  <td>{s.email}</td>
                  <td>
                    <Button
                      disabled={!sectionId || loading}
                      onClick={() => void assign(s.studentProfileId)}
                    >
                      Assign
                    </Button>
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
          {!students.length && (
            <EmptyState
              title="No unallocated students"
              description="All active students in this course year are already allocated."
            />
          )}
        </div>
      )}
    </Page>
  );
}

function ClassDetails({ data }: { data: Record<string, unknown> }) {
  const students = (data.students as api.StudentRosterItem[] | undefined) ?? [];
  return (
    <>
      <div className="grid gap-3 text-sm md:grid-cols-3">
        <div>
          <b>Class</b>
          <p>{String(data.className ?? "—")}</p>
        </div>
        <div>
          <b>Division</b>
          <p>
            {String(data.division ?? "—")} ({String(data.divisionCode ?? "—")})
          </p>
        </div>
        <div>
          <b>Department</b>
          <p>{String(data.department ?? "—")}</p>
        </div>
        <div>
          <b>Academic year</b>
          <p>{String(data.academicYear ?? "—")}</p>
        </div>
        <div>
          <b>Class teacher</b>
          <p>{String(data.classTeacher ?? "—")}</p>
        </div>
      </div>
      {data.students !== undefined && (
        <div className="mt-6 w-full overflow-hidden">
          <table className="w-full table-fixed text-xs sm:text-sm">
            <thead>
              <tr className="text-left text-slate-500">
                <th className="p-3">Student</th>
                <th>Admission number</th>
                <th>Roll number</th>
              </tr>
            </thead>
            <tbody>
              {students.map((s) => (
                <tr className="border-t" key={s.studentProfileId}>
                  <td className="p-3">{s.fullName}</td>
                  <td>{s.admissionNumber}</td>
                  <td>{s.rollNumber || "—"}</td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      )}
    </>
  );
}
export function MyClassRosterPage() {
  const [data, setData] = useState<Record<string, unknown> | null>(null);
  useEffect(() => {
    api
      .getMyClassRoster()
      .then(setData)
      .catch((e) => toast.error(handleApiError(e).message));
  }, []);
  return (
    <Page title="My Class" subtitle="Students allocated to your assigned class.">
      {data ? <ClassDetails data={data} /> : <Loader />}
    </Page>
  );
}
export function StudentClassPage() {
  const [data, setData] = useState<Record<string, unknown> | null>(null);
  const [error, setError] = useState(false);
  useEffect(() => {
    api
      .getMyStudentClass()
      .then(setData)
      .catch((e) => {
        setError(true);
        toast.error(handleApiError(e).message);
      });
  }, []);
  return (
    <Page title="My Class" subtitle="Your assigned course year, division, and class teacher.">
      {data ? (
        <ClassDetails data={data} />
      ) : error ? (
        <EmptyState
          title="Class not assigned yet"
          description="Your HOD will assign you to a division after your admission is confirmed."
        />
      ) : (
        <Loader />
      )}
    </Page>
  );
}
