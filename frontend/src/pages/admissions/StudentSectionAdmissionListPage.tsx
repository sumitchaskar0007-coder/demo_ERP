import { Eye, FileText, Search } from "lucide-react";
import { useCallback, useEffect, useState } from "react";
import { useNavigate } from "react-router-dom";
import { toast } from "sonner";
import { Button } from "@/components/common/Button";
import { Card } from "@/components/common/Card";
import { EmptyState } from "@/components/common/EmptyState";
import { Input } from "@/components/common/Input";
import { Loader } from "@/components/common/Loader";
import { Pagination } from "@/components/common/Pagination";
import { Select } from "@/components/common/Select";
import { DataTable, type Column } from "@/components/table/DataTable";
import { ADMISSION_STATUS_OPTIONS, PAGE_SIZE } from "@/lib/constants";
import { handleApiError } from "@/lib/handleApiError";
import { formatDate } from "@/lib/utils";
import type { PageResponse } from "@/types/api";
import { AdmissionStatusBadge } from "@/components/admissions/components";
import { useAuth } from "@/features/auth/authStore";
import * as api from "@/features/admissions/api";
import type { AdmissionStatus, StudentSectionAdmissionResponse } from "@/features/admissions/types";
import { ROLES } from "@/lib/constants";

const emptyPage: PageResponse<StudentSectionAdmissionResponse> = {
  content: [],
  page: 0,
  size: PAGE_SIZE,
  totalElements: 0,
  totalPages: 0,
  last: true,
};

export function StudentSectionAdmissionListPage() {
  const navigate = useNavigate();
  const { isRole } = useAuth();
  const canManage = isRole([ROLES.STUDENT_SECTION]);
  const [result, setResult] = useState(emptyPage);
  const [keyword, setKeyword] = useState("");
  const [status, setStatus] = useState("");
  const [page, setPage] = useState(0);
  const [loading, setLoading] = useState(true);
  const load = useCallback(async () => {
    setLoading(true);
    try {
      setResult(
        await api.searchStudentSectionAdmissions({
          keyword: keyword || undefined,
          status: status as AdmissionStatus | "",
          page,
          size: PAGE_SIZE,
        }),
      );
    } catch (err) {
      toast.error(handleApiError(err).message);
    } finally {
      setLoading(false);
    }
  }, [keyword, page, status]);
  useEffect(() => {
    const timer = setTimeout(load, 250);
    return () => clearTimeout(timer);
  }, [load]);
  const columns: Column<StudentSectionAdmissionResponse>[] = [
    {
      key: "ref",
      header: "Reference",
      render: (row) => (
        <div>
          <p className="font-semibold">{row.admissionReferenceNumber}</p>
          <p className="text-xs text-slate-400">{row.admissionNumber}</p>
        </div>
      ),
    },
    {
      key: "name",
      header: "Student",
      render: (row) => (
        <div>
          <p>{row.fullName}</p>
          <p className="text-xs text-slate-400">{row.email}</p>
        </div>
      ),
    },
    { key: "phone", header: "Phone", render: (row) => row.phone },
    { key: "department", header: "Department", render: (row) => row.departmentCode },
    {
      key: "status",
      header: "Status",
      render: (row) => <AdmissionStatusBadge status={row.status} />,
    },
    { key: "submitted", header: "Submitted", render: (row) => formatDate(row.submittedAt) },
    { key: "print", header: "Prints", render: (row) => row.printCount || 0 },
    {
      key: "actions",
      header: "",
      render: (row) => (
        <div className="flex flex-wrap gap-2">
          <Button
            variant="secondary"
            onClick={() => navigate(`/student-section/admissions/${row.id}`)}
          >
            <Eye className="h-4 w-4" />
            View
          </Button>
          {canManage && ["SUBMITTED", "STUDENT_SECTION_REVIEW_PENDING"].includes(row.status) && (
            <Button onClick={() => navigate(`/student-section/admissions/${row.id}`)}>
              Review
            </Button>
          )}
          {canManage && row.status === "STUDENT_SECTION_APPROVED" && (
            <Button
              variant="secondary"
              onClick={() => navigate(`/student-section/admissions/${row.id}/print`)}
            >
              <FileText className="h-4 w-4" />
              Print
            </Button>
          )}
        </div>
      ),
    },
  ];
  return (
    <div className="page-container">
      <div>
        <h1 className="page-title">Admission Records</h1>
        <p className="page-subtitle">
          {canManage
            ? "Verify submitted applications and track every admission phase."
            : "View admission records and their current progress across every phase."}
        </p>
      </div>
      <Card className="mt-6">
        <div className="grid gap-3 border-b p-4 md:grid-cols-[1fr_260px]">
          <Input
            placeholder="Search reference, student, email..."
            icon={<Search className="h-4 w-4" />}
            value={keyword}
            onChange={(event) => {
              setKeyword(event.target.value);
              setPage(0);
            }}
          />
          <Select
            options={ADMISSION_STATUS_OPTIONS}
            value={status}
            onChange={(event) => {
              setStatus(event.target.value);
              setPage(0);
            }}
            aria-label="Status"
          />
        </div>
        {loading ? (
          <Loader label="Loading admissions..." />
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
            title="No admissions found"
            description="Submitted admissions will appear here."
          />
        )}
      </Card>
    </div>
  );
}
