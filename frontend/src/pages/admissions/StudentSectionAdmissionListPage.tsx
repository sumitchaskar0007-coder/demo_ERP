import { Download, Eye, Search } from "lucide-react";
import { useCallback, useEffect, useState } from "react";
import { useNavigate } from "react-router-dom";
import { toast } from "sonner";
import { Button } from "@/components/common/Button";
import { Card } from "@/components/common/Card";
import { ConfirmDialog } from "@/components/common/ConfirmDialog";
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
import * as api from "@/features/admissions/api";
import type { AdmissionStatus, StudentSectionAdmissionResponse } from "@/features/admissions/types";

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
  const [result, setResult] = useState(emptyPage);
  const [keyword, setKeyword] = useState("");
  const [status, setStatus] = useState("");
  const [page, setPage] = useState(0);
  const [loading, setLoading] = useState(true);
  const [action, setAction] = useState<{
    type: "start" | "approve";
    admission: StudentSectionAdmissionResponse;
  } | null>(null);
  const [actionLoading, setActionLoading] = useState(false);
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
  const runAction = async () => {
    if (!action) return;
    setActionLoading(true);
    try {
      if (action.type === "start") await api.startAdmissionReview(action.admission.id);
      else
        await api.approveAdmission(action.admission.id, {
          studentCategory: action.admission.studentCategory,
          remarks: "Student data verified successfully",
        });
      toast.success("Admission updated");
      setAction(null);
      await load();
    } catch (err) {
      toast.error(handleApiError(err).message);
    } finally {
      setActionLoading(false);
    }
  };
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
        <div className="table-action-group">
          <Button
            variant="secondary"
            onClick={() => navigate(`/student-section/admissions/${row.id}`)}
          >
            <Eye className="h-4 w-4" />
            View
          </Button>
          {row.status === "SUBMITTED" && (
            <Button
              variant="secondary"
              onClick={() => setAction({ type: "start", admission: row })}
            >
              Start
            </Button>
          )}
          {["SUBMITTED", "STUDENT_SECTION_REVIEW_PENDING"].includes(row.status) && (
            <Button onClick={() => setAction({ type: "approve", admission: row })}>Approve</Button>
          )}
          <Button
            variant="secondary"
            onClick={() => navigate(`/student-section/admissions/${row.id}/print`)}
          >
            <Download className="h-4 w-4" />
            PDF
          </Button>
        </div>
      ),
    },
  ];
  return (
    <div className="page-container">
      <div>
        <h1 className="page-title">Student Section Admissions</h1>
        <p className="page-subtitle">Search and verify public admission forms.</p>
      </div>
      <Card className="mt-6 overflow-hidden">
        <div className="filter-grid md:grid-cols-[minmax(0,1fr)_260px]">
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
      <ConfirmDialog
        open={Boolean(action)}
        onClose={() => setAction(null)}
        onConfirm={runAction}
        loading={actionLoading}
        title={`${action?.type === "start" ? "Start review" : "Approve admission"}?`}
        description={
          action?.type === "approve"
            ? `This confirms student category ${action.admission.studentCategory} and creates the matching fee account.`
            : "This will record a status history entry."
        }
        confirmLabel={action?.type === "start" ? "Start Review" : "Approve"}
      />
    </div>
  );
}
