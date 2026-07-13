import { GraduationCap, Search } from "lucide-react";
import { useCallback, useEffect, useState } from "react";
import { toast } from "sonner";
import { Badge } from "@/components/common/Badge";
import { Card } from "@/components/common/Card";
import { EmptyState } from "@/components/common/EmptyState";
import { Input } from "@/components/common/Input";
import { Loader } from "@/components/common/Loader";
import { Pagination } from "@/components/common/Pagination";
import { Select } from "@/components/common/Select";
import { DataTable, type Column } from "@/components/table/DataTable";
import { getActiveColleges } from "@/features/colleges/api";
import type { College } from "@/features/colleges/types";
import { handleApiError } from "@/lib/handleApiError";
import { PAGE_SIZE } from "@/lib/constants";
import { formatDate } from "@/lib/utils";
import type { PageResponse } from "@/types/api";
import * as api from "@/features/student/api";
import type { StudentProfileResponse, StudentStatus } from "@/features/student/types";

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
  const [result, setResult] = useState(emptyPage);
  const [colleges, setColleges] = useState<College[]>([]);
  const [keyword, setKeyword] = useState("");
  const [status, setStatus] = useState("");
  const [collegeId, setCollegeId] = useState<number | "">("");
  const [page, setPage] = useState(0);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    getActiveColleges()
      .then(setColleges)
      .catch(() => setColleges([]));
  }, []);

  const load = useCallback(async () => {
    setLoading(true);
    try {
      setResult(
        await api.searchStudents({
          keyword: keyword || undefined,
          collegeId: Number(collegeId) || undefined,
          status: status as StudentStatus | "",
          page,
          size: PAGE_SIZE,
          sortBy: "createdAt",
          sortDir: "desc",
        }),
      );
    } catch (error) {
      toast.error(handleApiError(error).message);
    } finally {
      setLoading(false);
    }
  }, [collegeId, keyword, page, status]);

  useEffect(() => {
    const timer = setTimeout(load, 250);
    return () => clearTimeout(timer);
  }, [load]);

  const columns: Column<StudentProfileResponse>[] = [
    {
      key: "student",
      header: "Student",
      render: (row) => (
        <div className="flex items-center gap-3">
          <div className="grid h-10 w-10 place-items-center rounded-xl bg-emerald-50 text-emerald-600">
            <GraduationCap className="h-5 w-5" />
          </div>
          <div>
            <p className="font-semibold text-slate-900">{row.fullName}</p>
            <p className="text-xs text-slate-400">{row.admissionNumber}</p>
          </div>
        </div>
      ),
    },
    { key: "email", header: "Email", render: (row) => row.email },
    { key: "phone", header: "Phone", render: (row) => row.phone },
    {
      key: "college",
      header: "College",
      render: (row) => (
        <div>
          <p>{row.collegeName}</p>
          <Badge>{row.collegeCode}</Badge>
        </div>
      ),
    },
    {
      key: "department",
      header: "Department",
      render: (row) => (
        <div>
          <p>{row.departmentName}</p>
          <Badge>{row.departmentCode}</Badge>
        </div>
      ),
    },
    {
      key: "status",
      header: "Status",
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
    { key: "created", header: "Created", render: (row) => formatDate(row.createdAt) },
  ];

  return (
    <div className="page-container">
      <div>
        <h1 className="page-title">Students</h1>
        <p className="page-subtitle">View student profiles separately from staff accounts.</p>
      </div>
      <Card className="mt-6">
        <div className="grid gap-3 border-b p-4 lg:grid-cols-[1fr_220px_240px]">
          <Input
            placeholder="Search name, email, admission no..."
            icon={<Search className="h-4 w-4" />}
            value={keyword}
            onChange={(event) => {
              setKeyword(event.target.value);
              setPage(0);
            }}
          />
          <Select
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
          <Select
            options={studentStatusOptions}
            value={status}
            onChange={(event) => {
              setStatus(event.target.value);
              setPage(0);
            }}
            aria-label="Filter by student status"
          />
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
    </div>
  );
}
