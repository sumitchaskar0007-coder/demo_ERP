import {
  BookOpen,
  Award,
  Building2,
  CalendarDays,
  Eye,
  GraduationCap,
  Hash,
  Mail,
  Phone,
  Search,
  UserRound,
  UsersRound,
  X,
} from "lucide-react";
import { useCallback, useEffect, useState } from "react";
import { toast } from "sonner";
import { Badge } from "@/components/common/Badge";
import { Card } from "@/components/common/Card";
import { EmptyState } from "@/components/common/EmptyState";
import { Input } from "@/components/common/Input";
import { Loader } from "@/components/common/Loader";
import { Pagination } from "@/components/common/Pagination";
import { Select } from "@/components/common/Select";
import { Textarea } from "@/components/common/Textarea";
import { Button } from "@/components/common/Button";
import { Modal } from "@/components/common/Modal";
import { DataTable, type Column } from "@/components/table/DataTable";
import { getActiveColleges } from "@/features/colleges/api";
import type { College } from "@/features/colleges/types";
import { handleApiError } from "@/lib/handleApiError";
import { PAGE_SIZE, ROLES } from "@/lib/constants";
import { formatDate, initials } from "@/lib/utils";
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
        title="Student profile"
        size="xl"
      >
        {detailsLoading ? (
          <Loader label="Loading complete student record…" />
        ) : (
          details && (
            <StudentDetailContent
              details={details}
              principal={principal}
              onScholarshipApproved={() => void openDetails(details.profile.id)}
            />
          )
        )}
      </Modal>
    </div>
  );
}

function StudentDetailContent({
  details,
  principal,
  onScholarshipApproved,
}: {
  details: AdminStudentDetails;
  principal: boolean;
  onScholarshipApproved: () => void;
}) {
  const p = details.profile;
  const [scholarshipAmount, setScholarshipAmount] = useState("");
  const [scholarshipRemarks, setScholarshipRemarks] = useState("");
  const [approvingScholarship, setApprovingScholarship] = useState(false);
  const attendanceTone =
    details.attendance.percentage >= 75
      ? "bg-emerald-500"
      : details.attendance.percentage >= 60
        ? "bg-amber-500"
        : "bg-rose-500";
  const statusTone = p.status.includes("REJECTED")
    ? "danger"
    : p.status === "UNDER_REVIEW"
      ? "warning"
      : "success";
  const approveScholarship = async () => {
    const amount = Number(scholarshipAmount);
    if (!details.fees || !Number.isFinite(amount) || amount <= 0) {
      toast.error("Enter a valid scholarship amount");
      return;
    }
    if (amount > details.fees.remainingAmount) {
      toast.error("Scholarship cannot exceed the remaining fee");
      return;
    }
    if (
      !window.confirm(
        `Approve ₹${amount.toLocaleString("en-IN")} scholarship for ${p.fullName}?`,
      )
    )
      return;
    setApprovingScholarship(true);
    try {
      await api.approveScholarship(p.id, {
        amount,
        remarks: scholarshipRemarks.trim() || undefined,
      });
      setScholarshipAmount("");
      setScholarshipRemarks("");
      toast.success("Scholarship approved and student notified");
      onScholarshipApproved();
    } catch (error) {
      toast.error(handleApiError(error).message);
    } finally {
      setApprovingScholarship(false);
    }
  };

  return (
    <div className="space-y-4 sm:space-y-5">
      <section className="relative overflow-hidden rounded-2xl border border-blue-100 bg-gradient-to-br from-blue-700 via-blue-600 to-indigo-700 p-5 text-white shadow-lg shadow-blue-900/10 sm:p-6">
        <div
          className="pointer-events-none absolute -right-16 -top-20 h-52 w-52 rounded-full border-[34px] border-white/10"
          aria-hidden="true"
        />
        <div className="relative flex flex-col gap-5 sm:flex-row sm:items-center">
          <div className="grid h-20 w-20 shrink-0 place-items-center rounded-2xl border border-white/25 bg-white/15 text-2xl font-black shadow-inner backdrop-blur">
            {initials(p.fullName)}
          </div>
          <div className="min-w-0 flex-1">
            <div className="flex flex-wrap items-center gap-2">
              <span className="rounded-full border border-white/20 bg-white/10 px-2.5 py-1 text-[10px] font-bold uppercase tracking-[0.14em] text-blue-50">
                Student profile
              </span>
              <Badge tone={statusTone}>{p.status.replaceAll("_", " ")}</Badge>
            </div>
            <h2 className="mt-3 break-words text-2xl font-black tracking-tight sm:text-3xl">
              {p.fullName}
            </h2>
            <p className="mt-1 flex items-center gap-2 text-sm font-medium text-blue-100">
              <Hash className="h-4 w-4 shrink-0" />
              <span className="break-all">{p.admissionNumber}</span>
            </p>
          </div>
        </div>
        <div className="relative mt-5 grid gap-2 sm:grid-cols-2">
          <a
            href={`mailto:${p.email}`}
            className="flex min-w-0 items-center gap-3 rounded-xl border border-white/30 bg-white px-3.5 py-3 text-sm font-bold text-blue-700 shadow-sm transition hover:bg-blue-50"
          >
            <span className="grid h-9 w-9 shrink-0 place-items-center rounded-lg bg-blue-100 text-blue-700">
              <Mail className="h-4 w-4" />
            </span>
            <span className="min-w-0 break-all">{p.email}</span>
          </a>
          <a
            href={`tel:${p.phone}`}
            className="flex min-w-0 items-center gap-3 rounded-xl border border-white/30 bg-white px-3.5 py-3 text-sm font-bold text-blue-700 shadow-sm transition hover:bg-blue-50"
          >
            <span className="grid h-9 w-9 shrink-0 place-items-center rounded-lg bg-blue-100 text-blue-700">
              <Phone className="h-4 w-4" />
            </span>
            <span className="font-mono tracking-wide">{p.phone}</span>
          </a>
        </div>
      </section>

      <div className="grid gap-4 md:grid-cols-2">
        <Card className="overflow-hidden">
          <div className="flex items-center gap-3 border-b border-slate-100 bg-slate-50/70 px-4 py-4 sm:px-5">
            <span className="grid h-10 w-10 place-items-center rounded-xl bg-blue-100 text-blue-700">
              <UserRound className="h-5 w-5" />
            </span>
            <div>
              <h3 className="font-bold text-slate-900">Personal information</h3>
              <p className="text-xs text-slate-500">Student identity and placement</p>
            </div>
          </div>
          <div className="grid gap-4 p-4 sm:grid-cols-2 sm:p-5">
            <ProfileField
              icon={<Building2 className="h-4 w-4" />}
              label="College"
              value={p.collegeName}
            />
            <ProfileField
              icon={<BookOpen className="h-4 w-4" />}
              label="Department"
              value={p.departmentName}
            />
            <ProfileField
              icon={<CalendarDays className="h-4 w-4" />}
              label="Date of birth"
              value={p.dateOfBirth ? formatDate(p.dateOfBirth) : "Not available"}
            />
            <ProfileField
              icon={<UserRound className="h-4 w-4" />}
              label="Gender"
              value={p.gender || "Not available"}
            />
          </div>
        </Card>

        <Card className="overflow-hidden">
          <div className="flex items-center gap-3 border-b border-slate-100 bg-slate-50/70 px-4 py-4 sm:px-5">
            <span className="grid h-10 w-10 place-items-center rounded-xl bg-indigo-100 text-indigo-700">
              <UsersRound className="h-5 w-5" />
            </span>
            <div>
              <h3 className="font-bold text-slate-900">Parent / guardian</h3>
              <p className="text-xs text-slate-500">Emergency contact information</p>
            </div>
          </div>
          <div className="p-4 sm:p-5">
            <p className="text-lg font-bold text-slate-900">{p.parentName || "Not available"}</p>
            {p.parentPhone ? (
              <a
                href={`tel:${p.parentPhone}`}
                className="mt-3 flex w-fit max-w-full items-center gap-2 rounded-xl bg-blue-50 px-3 py-2.5 font-mono text-sm font-bold tracking-wide text-blue-700 ring-1 ring-blue-100 transition hover:bg-blue-100"
              >
                <Phone className="h-4 w-4 shrink-0" />
                <span className="break-all">{p.parentPhone}</span>
              </a>
            ) : (
              <p className="mt-2 text-sm text-slate-500">Contact number not available</p>
            )}
          </div>
        </Card>
      </div>

      <div className="grid gap-4 md:grid-cols-[1.1fr_0.9fr]">
        <Card className="overflow-hidden">
          <div className="flex items-center gap-3 border-b border-slate-100 bg-slate-50/70 px-4 py-4 sm:px-5">
            <span className="grid h-10 w-10 place-items-center rounded-xl bg-violet-100 text-violet-700">
              <GraduationCap className="h-5 w-5" />
            </span>
            <div>
              <h3 className="font-bold text-slate-900">Admission & academics</h3>
              <p className="text-xs text-slate-500">Current admission and class placement</p>
            </div>
          </div>
          <div className="grid gap-4 p-4 sm:grid-cols-2 sm:p-5">
            <ProfileField
              label="Reference"
              value={details.admission?.referenceNumber || "Not available"}
            />
            <ProfileField
              label="Admission status"
              value={details.admission?.status?.replaceAll("_", " ") || "Not available"}
            />
            <ProfileField
              label="Academic year"
              value={
                details.academic?.academicYear || details.admission?.academicYear || "Not allocated"
              }
            />
            <ProfileField
              label="Course year / Division"
              value={
                details.academic
                  ? `${details.academic.courseYear} · ${details.academic.division}`
                  : "Not allocated"
              }
            />
            {details.academic?.rollNumber && (
              <ProfileField label="Roll number" value={details.academic.rollNumber} />
            )}
          </div>
        </Card>

        <Card className="overflow-hidden p-4 sm:p-5">
          <div className="flex items-start justify-between gap-3">
            <div>
              <p className="text-xs font-bold uppercase tracking-[0.12em] text-slate-400">
                Attendance
              </p>
              <p className="mt-2 text-4xl font-black tracking-tight text-slate-900">
                {details.attendance.percentage}%
              </p>
            </div>
            <span className={`mt-1 h-3 w-3 rounded-full ${attendanceTone}`} aria-hidden="true" />
          </div>
          <div className="mt-4 h-2.5 overflow-hidden rounded-full bg-slate-100">
            <div
              className={`h-full rounded-full ${attendanceTone}`}
              style={{ width: `${Math.min(100, Math.max(0, details.attendance.percentage))}%` }}
            />
          </div>
          <div className="mt-5 grid grid-cols-2 gap-2">
            <AttendanceMetric label="Lectures" value={details.attendance.totalLectures} />
            <AttendanceMetric
              label="Present"
              value={details.attendance.present}
              tone="text-emerald-700"
            />
            <AttendanceMetric
              label="Absent"
              value={details.attendance.absent}
              tone="text-rose-700"
            />
            <AttendanceMetric
              label="Late / Leave"
              value={`${details.attendance.late} / ${details.attendance.leave}`}
              tone="text-amber-700"
            />
          </div>
        </Card>
      </div>

      <Card className="overflow-hidden">
        <div className="flex items-center gap-3 border-b border-slate-100 bg-emerald-50/60 px-4 py-4 sm:px-5">
          <span className="grid h-10 w-10 place-items-center rounded-xl bg-emerald-100 text-emerald-700">
            <Award className="h-5 w-5" />
          </span>
          <div>
            <h3 className="font-bold text-slate-900">Fee & scholarship</h3>
            <p className="text-xs text-slate-500">
              Scholarship reduces the payable balance without changing verified payments.
            </p>
          </div>
        </div>
        {details.fees ? (
          <div className="space-y-4 p-4 sm:p-5">
            <div className="grid gap-3 sm:grid-cols-4">
              <FeeMetric label="Total fee" value={details.fees.totalFee} />
              <FeeMetric label="Paid" value={details.fees.paidAmount} tone="text-blue-700" />
              <FeeMetric
                label="Scholarship"
                value={details.fees.scholarshipAmount}
                tone="text-emerald-700"
              />
              <FeeMetric
                label="Remaining"
                value={details.fees.remainingAmount}
                tone="text-rose-700"
              />
            </div>
            {principal && details.fees.remainingAmount > 0 && (
              <div className="grid gap-3 rounded-2xl border border-emerald-100 bg-emerald-50/40 p-4 md:grid-cols-[220px_1fr_auto] md:items-end">
                <Input
                  label="Scholarship amount"
                  type="number"
                  min={1}
                  max={details.fees.remainingAmount}
                  step="0.01"
                  value={scholarshipAmount}
                  onChange={(event) => setScholarshipAmount(event.target.value)}
                />
                <Textarea
                  label="Remarks (optional)"
                  rows={2}
                  maxLength={500}
                  value={scholarshipRemarks}
                  onChange={(event) => setScholarshipRemarks(event.target.value)}
                />
                <Button
                  loading={approvingScholarship}
                  onClick={() => void approveScholarship()}
                >
                  Confirm scholarship
                </Button>
              </div>
            )}
          </div>
        ) : (
          <p className="p-5 text-sm text-slate-500">
            Fee account has not been generated for this student.
          </p>
        )}
      </Card>
    </div>
  );
}

function FeeMetric({
  label,
  value,
  tone = "text-slate-900",
}: {
  label: string;
  value: number;
  tone?: string;
}) {
  return (
    <div className="rounded-xl bg-slate-50 p-3 ring-1 ring-slate-100">
      <p className={`text-xl font-black ${tone}`}>₹{Number(value).toLocaleString("en-IN")}</p>
      <p className="mt-1 text-[10px] font-bold uppercase tracking-wide text-slate-400">{label}</p>
    </div>
  );
}

function ProfileField({
  icon,
  label,
  value,
}: {
  icon?: React.ReactNode;
  label: string;
  value: React.ReactNode;
}) {
  return (
    <div className="min-w-0">
      <p className="flex items-center gap-1.5 text-[11px] font-bold uppercase tracking-wide text-slate-400">
        {icon && <span className="text-slate-400">{icon}</span>}
        {label}
      </p>
      <p className="mt-1.5 break-words text-sm font-semibold leading-5 text-slate-700">{value}</p>
    </div>
  );
}

function AttendanceMetric({
  label,
  value,
  tone = "text-slate-900",
}: {
  label: string;
  value: React.ReactNode;
  tone?: string;
}) {
  return (
    <div className="rounded-xl bg-slate-50 p-3 ring-1 ring-slate-100">
      <p className={`text-lg font-black ${tone}`}>{value}</p>
      <p className="mt-0.5 text-[10px] font-bold uppercase tracking-wide text-slate-400">{label}</p>
    </div>
  );
}
