import { Eye, Search } from "lucide-react";
import { useCallback, useEffect, useState } from "react";
import { useNavigate } from "react-router-dom";
import { toast } from "sonner";
import { Badge } from "@/components/common/Badge";
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
      render: (row) => (
        <div>
          <p className="font-semibold">{row.admissionReferenceNumber}</p>
          <p className="text-xs text-slate-400">{row.admissionNumber}</p>
        </div>
      ),
    },
    { key: "name", header: "Student", render: (row) => row.fullName },
    { key: "department", header: "Department", render: (row) => row.departmentName },
    {
      key: "status",
      header: "Status",
      render: (row) => (
        <div className="space-y-1">
          <AdmissionStatusBadge status={row.status} />
          <Badge tone="warning">Ready for Principal Review</Badge>
        </div>
      ),
    },
    {
      key: "verified",
      header: "Verified At",
      render: (row) => formatDate(row.studentSectionVerifiedAt),
    },
    { key: "print", header: "Prints", render: (row) => row.printCount || 0 },
    {
      key: "actions",
      header: "",
      render: (row) => (
          <Button variant="secondary" onClick={() => navigate(`/principal/admissions/${row.id}`)}>
            <Eye className="h-4 w-4" />
            Review
        </Button>
      ),
    },
  ];
  return (
    <div className="page-container">
      <div>
        <h1 className="page-title">Final Admission Review</h1>
        <p className="page-subtitle">
          Review fee-qualified admissions in detail, then approve or reject the final admission.
        </p>
      </div>
      <Card className="mt-6">
        <div className="border-b p-4">
          <Input
            placeholder="Search review-ready admissions..."
            icon={<Search className="h-4 w-4" />}
            value={keyword}
            onChange={(event) => {
              setKeyword(event.target.value);
              setPage(0);
            }}
          />
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
