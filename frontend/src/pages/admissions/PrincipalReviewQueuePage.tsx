import { ClipboardCheck, Eye, Search } from "lucide-react";
import { useCallback, useEffect, useState } from "react";
import { useNavigate } from "react-router-dom";
import { toast } from "sonner";
import { Button } from "@/components/common/Button";
import { Card } from "@/components/common/Card";
import { EmptyState } from "@/components/common/EmptyState";
import { Input } from "@/components/common/Input";
import { Loader } from "@/components/common/Loader";
import { Pagination } from "@/components/common/Pagination";
import { DataTable, type Column } from "@/components/table/DataTable";
import { PAGE_SIZE } from "@/lib/constants";
import { handleApiError } from "@/lib/handleApiError";
import { formatDate } from "@/lib/utils";
import type { PageResponse } from "@/types/api";
import { AdmissionStatusBadge } from "@/components/admissions/components";
import * as api from "@/features/admissions/api";
import type { StudentSectionAdmissionResponse } from "@/features/admissions/types";
import { PrincipalAdmissionTabs } from "@/components/principal/PrincipalAdmissionTabs";

const emptyPage: PageResponse<StudentSectionAdmissionResponse> = {
  content: [],
  page: 0,
  size: PAGE_SIZE,
  totalElements: 0,
  totalPages: 0,
  last: true,
};

export function PrincipalReviewQueuePage() {
  const navigate = useNavigate();
  const [result, setResult] = useState(emptyPage);
  const [keyword, setKeyword] = useState("");
  const [page, setPage] = useState(0);
  const [loading, setLoading] = useState(true);
  const load = useCallback(async () => {
    setLoading(true);
    try {
      setResult(
        await api.getPrincipalReviewReadyAdmissions({
          keyword: keyword || undefined,
          page,
          size: PAGE_SIZE,
        }),
      );
    } catch (err) {
      toast.error(handleApiError(err).message);
    } finally {
      setLoading(false);
    }
  }, [keyword, page]);
  useEffect(() => {
    const timer = setTimeout(load, 250);
    return () => clearTimeout(timer);
  }, [load]);
  const columns: Column<StudentSectionAdmissionResponse>[] = [
    {
      key: "ref",
      header: "Reference",
      className: "w-[220px]",
      render: (row) => (
        <div className="min-w-0">
          <p className="font-semibold leading-5 text-slate-900">{row.admissionReferenceNumber}</p>
          <p className="mt-0.5 text-xs text-slate-400">{row.admissionNumber}</p>
        </div>
      ),
    },
    {
      key: "name",
      header: "Student",
      className: "w-[180px]",
      render: (row) => <span className="font-medium text-slate-800">{row.fullName}</span>,
    },
    {
      key: "department",
      header: "Department",
      className: "w-[170px]",
      render: (row) => row.departmentName,
    },
    {
      key: "status",
      header: "Status",
      className: "w-[210px]",
      render: (row) => <AdmissionStatusBadge status={row.status} />,
    },
    {
      key: "verified",
      header: "Verified At",
      className: "w-[190px]",
      render: (row) => (
        <span className="whitespace-nowrap">{formatDate(row.studentSectionVerifiedAt)}</span>
      ),
    },
    {
      key: "print",
      header: "Prints",
      className: "w-[80px] text-center",
      render: (row) => row.printCount || 0,
    },
    {
      key: "actions",
      header: "",
      className: "w-[130px] text-right",
      render: (row) => (
        <Button
          className="min-w-[108px] whitespace-nowrap px-3"
          variant="secondary"
          onClick={() => navigate(`/principal/admissions/${row.id}`)}
        >
          <Eye className="h-4 w-4" />
          Review
        </Button>
      ),
    },
  ];
  return (
    <div className="page-container">
      <PrincipalAdmissionTabs />
      <div className="flex flex-col gap-4 sm:flex-row sm:items-end sm:justify-between">
        <div>
          <div className="mb-2 flex items-center gap-2 text-sm font-semibold text-brand-600">
            <ClipboardCheck className="h-4 w-4" />
            Principal review queue
          </div>
          <h1 className="page-title">Final Admission Review</h1>
          <p className="page-subtitle">
            Review fee-qualified admissions in detail, then approve or reject the final admission.
          </p>
        </div>
        {!loading && (
          <div className="inline-flex w-fit items-center gap-2 rounded-xl border border-brand-100 bg-brand-50 px-3.5 py-2 text-sm font-semibold text-brand-700">
            <span className="grid h-6 min-w-6 place-items-center rounded-md bg-white px-1.5 text-xs shadow-sm">
              {result.totalElements}
            </span>
            Awaiting review
          </div>
        )}
      </div>
      <Card className="mt-6 overflow-hidden">
        <div className="border-b bg-slate-50/70 p-4">
          <div className="max-w-xl">
            <Input
              placeholder="Search by student, reference or admission number..."
              icon={<Search className="h-4 w-4" />}
              value={keyword}
              onChange={(event) => {
                setKeyword(event.target.value);
                setPage(0);
              }}
            />
          </div>
        </div>
        {loading ? (
          <Loader label="Loading review queue..." />
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
            title="No admissions awaiting final review"
            description="Admissions appear here after the required fee amount is verified."
          />
        )}
      </Card>
    </div>
  );
}
