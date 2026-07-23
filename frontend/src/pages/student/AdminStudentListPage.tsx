import { Building2, Eye, GraduationCap, Mail, Phone, Search, X } from "lucide-react";
import { useCallback, useEffect, useState } from "react";
import { toast } from "sonner";
import { Badge } from "@/components/common/Badge";
import { Card } from "@/components/common/Card";
import { EmptyState } from "@/components/common/EmptyState";
import { Input } from "@/components/common/Input";
import { Loader } from "@/components/common/Loader";
import { Pagination } from "@/components/common/Pagination";
import { Select } from "@/components/common/Select";
import { Button } from "@/components/common/Button";
import { Modal } from "@/components/common/Modal";
import { DataTable, type Column } from "@/components/table/DataTable";
import { getActiveColleges } from "@/features/colleges/api";
import type { College } from "@/features/colleges/types";
import { handleApiError } from "@/lib/handleApiError";
import { PAGE_SIZE, ROLES } from "@/lib/constants";
import { formatDate } from "@/lib/utils";
import type { PageResponse } from "@/types/api";
import * as api from "@/features/student/api";
import { useAuth } from "@/features/auth/authStore";
import type {
  AdminStudentDetails,
  StudentProfileResponse,
  StudentStatus,
} from "@/features/student/types";

const emptyPage: PageResponse<StudentProfileResponse> = {
  content: [],
  page: 0,
  size: PAGE_SIZE,
  totalElements: 0,
  totalPages: 0,
  last: true,
};

const studentStatusOptions = [
  { label: "All student statuses", value: "" },
  { label: "Admission Submitted", value: "ADMISSION_SUBMITTED" },
  { label: "Under Review", value: "UNDER_REVIEW" },
  { label: "Admission Approved", value: "ADMISSION_APPROVED" },
  { label: "Admission Rejected", value: "ADMISSION_REJECTED" },
  { label: "Active", value: "ACTIVE" },
  { label: "Inactive", value: "INACTIVE" },
];

export function AdminStudentListPage() {
  const { isRole } = useAuth();
  const principal = isRole([ROLES.PRINCIPAL]);
  const [result, setResult] = useState(emptyPage);
  const [colleges, setColleges] = useState<College[]>([]);
  const [keyword, setKeyword] = useState("");
  const [status, setStatus] = useState("");
  const [collegeId, setCollegeId] = useState<number | "">("");
  const [page, setPage] = useState(0);
  const [loading, setLoading] = useState(true);
  const [details, setDetails] = useState<AdminStudentDetails | null>(null);
  const [detailsLoading, setDetailsLoading] = useState(false);

  const openDetails = async (id: number) => {
    setDetailsLoading(true);
    try {
      setDetails(await api.getStudentDetails(id, principal));
    } catch (error) {
      toast.error(handleApiError(error).message);
    } finally {
      setDetailsLoading(false);
    }
  };

  useEffect(() => {
    if (principal) return;
    getActiveColleges()
      .then(setColleges)
      .catch(() => setColleges([]));
  }, [principal]);

  const load = useCallback(async () => {
    setLoading(true);
    try {
      setResult(
        await api.searchStudents(
          {
            keyword: keyword || undefined,
            collegeId: principal ? undefined : Number(collegeId) || undefined,
            status: status as StudentStatus | "",
            page,
            size: PAGE_SIZE,
            sortBy: "createdAt",
            sortDir: "desc",
          },
          principal,
        ),
      );
    } catch (error) {
      toast.error(handleApiError(error).message);
    } finally {
      setLoading(false);
    }
  }, [collegeId, keyword, page, principal, status]);

  useEffect(() => {
    const timer = setTimeout(load, 250);
    return () => clearTimeout(timer);
  }, [load]);

  const columns: Column<StudentProfileResponse>[] = [
    {
      key: "student",
      header: "Student",
      className: "lg:w-[23%]",
      render: (row) => (
        <div className="flex min-w-[210px] items-center gap-3">
          <div className="grid h-11 w-11 shrink-0 place-items-center rounded-2xl bg-emerald-50 text-emerald-600 ring-1 ring-emerald-100">
            <GraduationCap className="h-5 w-5" />
          </div>
          <div className="min-w-0">
            <p className="truncate font-bold text-slate-900" title={row.fullName}>
              {row.fullName}
            </p>
            <p className="mt-0.5 font-mono text-xs font-medium text-slate-400">
              {row.admissionNumber}
            </p>
          </div>
        </div>
      ),
    },
    {
      key: "contact",
      header: "Contact",
      className: "lg:w-[22%]",
      render: (row) => (
        <div className="min-w-[190px] space-y-2">
          <a
            href={`mailto:${row.email}`}
            className="flex min-w-0 items-center gap-2 text-slate-700 hover:text-brand-700"
            title={row.email}
          >
            <Mail className="h-4 w-4 shrink-0 text-slate-400" />
            <span className="truncate">{row.email}</span>
          </a>
          <a
            href={`tel:${row.phone}`}
            className="flex items-center gap-2 whitespace-nowrap font-medium tabular-nums text-slate-600 hover:text-brand-700"
          >
            <Phone className="h-4 w-4 shrink-0 text-slate-400" />
            {row.phone}
          </a>
        </div>
      ),
    },
    {
      key: "academic",
      header: "Academic placement",
      className: "lg:w-[30%]",
      render: (row) => (
        <div className="min-w-[240px] space-y-2.5">
          <div className="flex min-w-0 items-center gap-2">
            <Building2 className="h-4 w-4 shrink-0 text-brand-500" />
            <p className="min-w-0 truncate font-semibold text-slate-800" title={row.collegeName}>
              {row.collegeName}
            </p>
            <span className="shrink-0 rounded-md bg-slate-100 px-2 py-0.5 text-[11px] font-bold text-slate-600">
              {row.collegeCode}
            </span>
          </div>
          <div className="flex min-w-0 items-center gap-2 pl-6">
            <p className="min-w-0 truncate text-xs text-slate-500" title={row.departmentName}>
              {row.departmentName}
            </p>
            <span className="shrink-0 rounded-md border border-slate-200 px-2 py-0.5 text-[11px] font-bold text-slate-500">
              {row.departmentCode}
            </span>
          </div>
        </div>
      ),
    },
    {
      key: "status",
      header: "Status",
      className: "whitespace-nowrap lg:w-[11%]",
      render: (row) => (
        <Badge
          tone={
            row.status.includes("REJECTED")
              ? "danger"
              : row.status === "UNDER_REVIEW"
                ? "warning"
                : "success"
          }
        >
          {row.status.replaceAll("_", " ")}
        </Badge>
      ),
    },
    {
      key: "created",
      header: "Created",
      className: "lg:w-[12%]",
      render: (row) => (
        <time
          className="block min-w-[115px] text-sm leading-5 text-slate-600"
          dateTime={row.createdAt}
        >
          {formatDate(row.createdAt)}
        </time>
      ),
    },
    {
      key: "actions",
      header: "",
      className: "lg:w-[1%] lg:whitespace-nowrap",
      render: (row) => (
        <Button
          variant="secondary"
          className="h-9 whitespace-nowrap px-3"
          onClick={() => void openDetails(row.id)}
          aria-label={`View ${row.fullName}`}
        >
          <Eye className="h-4 w-4" />
          View
        </Button>
      ),
    },
  ];

  return (
    <div className="page-container">
      <div>
        <h1 className="page-title">Students</h1>
        <p className="page-subtitle">
          {principal
            ? "View student profiles, academic placement, and attendance for your college."
            : "View student profiles separately from staff accounts."}
        </p>
      </div>
      <Card className="mt-6 overflow-hidden">
        <div className="flex flex-col gap-4 border-b border-slate-200 bg-slate-50/60 p-4 sm:p-5">
          <div className="flex flex-wrap items-center justify-between gap-2">
            <div>
              <h2 className="font-bold text-slate-900">Student directory</h2>
              <p className="text-xs text-slate-500">
                {loading
                  ? "Updating records…"
                  : `${result.totalElements} student${result.totalElements === 1 ? "" : "s"} found`}
              </p>
            </div>
            {(keyword || (!principal && collegeId) || status) && (
              <Button
                variant="ghost"
                className="h-9 px-3"
                onClick={() => {
                  setKeyword("");
                  setCollegeId("");
                  setStatus("");
                  setPage(0);
                }}
              >
                <X className="h-4 w-4" />
                Clear filters
              </Button>
            )}
          </div>
          <div
            className={`grid min-w-0 gap-3 ${
              principal
                ? "lg:grid-cols-[minmax(280px,1fr)_minmax(210px,280px)]"
                : "lg:grid-cols-[minmax(280px,1fr)_minmax(220px,300px)_minmax(210px,240px)]"
            }`}
          >
            <Input
              label="Search students"
              placeholder="Search name, email, admission no..."
              icon={<Search className="h-4 w-4" />}
              value={keyword}
              onChange={(event) => {
                setKeyword(event.target.value);
                setPage(0);
              }}
            />
            {!principal && (
              <Select
                label="College"
                options={[
                  { label: "All colleges", value: "" },
                  ...colleges.map((college) => ({ label: college.name, value: college.id })),
                ]}
                value={collegeId}
                onChange={(event) => {
                  setCollegeId(event.target.value ? Number(event.target.value) : "");
                  setPage(0);
                }}
                aria-label="Filter by college"
              />
            )}
            <Select
              label="Student status"
              options={studentStatusOptions}
              value={status}
              onChange={(event) => {
                setStatus(event.target.value);
                setPage(0);
              }}
              aria-label="Filter by student status"
            />
          </div>
        </div>
        {loading ? (
          <Loader label="Loading students..." />
        ) : result.content.length ? (
          <>
            <DataTable columns={columns} data={result.content} rowKey={(row) => row.id} />
            <div className="border-t p-4">
              <Pagination
                page={result.page}
                totalPages={result.totalPages}
                totalElements={result.totalElements}
                onChange={setPage}
              />
            </div>
          </>
        ) : (
          <EmptyState
            title="No students found"
            description="Submitted student admissions will appear here."
          />
        )}
      </Card>
      <Modal
        open={Boolean(details) || detailsLoading}
        onClose={() => {
          setDetails(null);
          setDetailsLoading(false);
        }}
        title="Student details"
        size="xl"
      >
        {detailsLoading ? (
          <Loader label="Loading complete student record…" />
        ) : (
          details && <StudentDetailContent details={details} />
        )}
      </Modal>
    </div>
  );
}

function StudentDetailContent({ details }: { details: AdminStudentDetails }) {
  const p = details.profile;
  return (
    <div className="space-y-5">
      <section className="grid gap-3 rounded-2xl bg-slate-50 p-5 sm:grid-cols-2">
        <Detail label="Student" value={p.fullName} />
        <Detail label="Admission number" value={p.admissionNumber} />
        <Detail label="Email" value={p.email} />
        <Detail label="Phone" value={p.phone} />
        <Detail label="College" value={p.collegeName} />
        <Detail label="Department" value={p.departmentName} />
        <Detail label="Parent" value={`${p.parentName} · ${p.parentPhone}`} />
        <Detail label="Status" value={p.status.replaceAll("_", " ")} />
      </section>
      <div className="grid gap-4 md:grid-cols-2">
        <Card className="p-5">
          <h3 className="font-bold">Admission & academics</h3>
          <div className="mt-4 space-y-3">
            <Detail
              label="Reference"
              value={details.admission?.referenceNumber || "Not available"}
            />
            <Detail
              label="Admission status"
              value={details.admission?.status?.replaceAll("_", " ") || "Not available"}
            />
            <Detail
              label="Academic year"
              value={
                details.academic?.academicYear || details.admission?.academicYear || "Not allocated"
              }
            />
            <Detail
              label="Course year / Division"
              value={
                details.academic
                  ? `${details.academic.courseYear} · ${details.academic.division}`
                  : "Not allocated"
              }
            />
          </div>
        </Card>
        <Card className="p-5">
          <h3 className="font-bold">Attendance</h3>
          <p className="mt-4 text-4xl font-black text-brand-600">
            {details.attendance.percentage}%
          </p>
          <div className="mt-4 grid grid-cols-2 gap-2 text-sm">
            <Detail label="Total lectures" value={details.attendance.totalLectures} />
            <Detail label="Present" value={details.attendance.present} />
            <Detail label="Absent" value={details.attendance.absent} />
            <Detail
              label="Late / Leave"
              value={`${details.attendance.late} / ${details.attendance.leave}`}
            />
          </div>
        </Card>
      </div>
    </div>
  );
}
function Detail({ label, value }: { label: string; value: React.ReactNode }) {
  return (
    <div>
      <p className="text-[11px] font-bold uppercase tracking-wide text-slate-400">{label}</p>
      <p className="mt-1 text-sm font-semibold text-slate-700">{value}</p>
    </div>
  );
}
