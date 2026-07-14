import { Building2, Plus, Search } from "lucide-react";
import { useCallback, useEffect, useState } from "react";
import { useNavigate } from "react-router-dom";
import { toast } from "sonner";
import { Button } from "@/components/common/Button";
import { Card } from "@/components/common/Card";
import { ConfirmDialog } from "@/components/common/ConfirmDialog";
import { EmptyState } from "@/components/common/EmptyState";
import { Input } from "@/components/common/Input";
import { Loader } from "@/components/common/Loader";
import { Modal } from "@/components/common/Modal";
import { Pagination } from "@/components/common/Pagination";
import { Select } from "@/components/common/Select";
import { StatusBadge } from "@/components/common/Badge";
import { DataTable, type Column } from "@/components/table/DataTable";
import { TableActions } from "@/components/table/TableActions";
import { handleApiError } from "@/lib/handleApiError";
import { formatDate } from "@/lib/utils";
import { PAGE_SIZE, STATUS_OPTIONS } from "@/lib/constants";
import type { PageResponse } from "@/types/api";
import type { College, CollegeFormValues } from "@/features/colleges/types";
import * as api from "@/features/colleges/api";
import { CollegeForm } from "@/components/colleges/CollegeForm";

const emptyPage: PageResponse<College> = {
  content: [],
  page: 0,
  size: PAGE_SIZE,
  totalElements: 0,
  totalPages: 0,
  last: true,
};

export function CollegeListPage() {
  const navigate = useNavigate();
  const [result, setResult] = useState(emptyPage);
  const [loading, setLoading] = useState(true);
  const [keyword, setKeyword] = useState("");
  const [status, setStatus] = useState("");
  const [page, setPage] = useState(0);
  const [formOpen, setFormOpen] = useState(false);
  const [editing, setEditing] = useState<College | null>(null);
  const [confirming, setConfirming] = useState<College | null>(null);
  const [actionLoading, setActionLoading] = useState(false);

  const load = useCallback(async () => {
    setLoading(true);
    try {
      setResult(
        await api.searchColleges({
          keyword: keyword || undefined,
          status: status as "ACTIVE" | "INACTIVE" | "",
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
  }, [keyword, page, status]);

  useEffect(() => {
    const timer = setTimeout(load, 250);
    return () => clearTimeout(timer);
  }, [load]);
  const openCreate = () => {
    setEditing(null);
    setFormOpen(true);
  };
  const submit = async (values: CollegeFormValues | Omit<CollegeFormValues, "code">) => {
    try {
      const response = editing
        ? await api.updateCollege(editing.id, values as Omit<CollegeFormValues, "code">)
        : await api.createCollege(values as CollegeFormValues);
      toast.success(response.message);
      setFormOpen(false);
      setEditing(null);
      await load();
    } catch (error) {
      toast.error(handleApiError(error).message);
    }
  };
  const toggle = async () => {
    if (!confirming) return;
    setActionLoading(true);
    try {
      const response = await api.setCollegeStatus(confirming.id, confirming.status !== "ACTIVE");
      toast.success(response.message);
      setConfirming(null);
      await load();
    } catch (error) {
      toast.error(handleApiError(error).message);
    } finally {
      setActionLoading(false);
    }
  };
  const columns: Column<College>[] = [
    {
      key: "name",
      header: "College",
      render: (row) => (
        <div className="flex items-center gap-3">
          <div className="grid h-10 w-10 place-items-center rounded-xl bg-blue-50 text-brand-600">
            <Building2 className="h-5 w-5" />
          </div>
          <div>
            <p className="font-semibold text-slate-900">{row.name}</p>
            <p className="text-xs text-slate-400">{row.code}</p>
          </div>
        </div>
      ),
    },
    {
      key: "city",
      header: "Location",
      render: (row) => <span>{[row.city, row.state].filter(Boolean).join(", ") || "—"}</span>,
    },
    { key: "email", header: "Contact email", render: (row) => row.contactEmail || "—" },
    { key: "status", header: "Status", render: (row) => <StatusBadge status={row.status} /> },
    { key: "created", header: "Created", render: (row) => formatDate(row.createdAt) },
    {
      key: "actions",
      header: "",
      className: "w-16 text-right",
      render: (row) => (
        <TableActions
          active={row.status === "ACTIVE"}
          onView={() => navigate(`/colleges/${row.id}`)}
          onEdit={() => {
            setEditing(row);
            setFormOpen(true);
          }}
          onToggle={() => setConfirming(row)}
        />
      ),
    },
  ];
  return (
    <div className="page-container">
      <div className="flex flex-col justify-between gap-4 sm:flex-row sm:items-center">
        <div>
          <h1 className="page-title">Colleges</h1>
          <p className="page-subtitle">Manage every college in your ERP network.</p>
        </div>
        <Button onClick={openCreate}>
          <Plus className="h-4 w-4" />
          Add college
        </Button>
      </div>
      <Card className="mt-6">
        <div className="grid gap-3 border-b p-4 sm:grid-cols-[1fr_220px]">
          <Input
            placeholder="Search name, code, city, state or email…"
            icon={<Search className="h-4 w-4" />}
            value={keyword}
            onChange={(event) => {
              setKeyword(event.target.value);
              setPage(0);
            }}
          />
          <Select
            options={STATUS_OPTIONS}
            value={status}
            onChange={(event) => {
              setStatus(event.target.value);
              setPage(0);
            }}
            aria-label="Filter by status"
          />
        </div>
        {loading ? (
          <Loader label="Loading colleges…" />
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
            title="No colleges found"
            description="Try another filter or add your first college."
            action={
              <Button onClick={openCreate}>
                <Plus className="h-4 w-4" />
                Add college
              </Button>
            }
          />
        )}
      </Card>
      <Modal
        open={formOpen}
        onClose={() => setFormOpen(false)}
        title={editing ? "Edit college" : "Create college"}
        description={
          editing ? "College code cannot be changed." : "Add a college to the ERP network."
        }
        size="xl"
      >
        <CollegeForm college={editing} onSubmit={submit} onCancel={() => setFormOpen(false)} />
      </Modal>
      <ConfirmDialog
        open={Boolean(confirming)}
        onClose={() => setConfirming(null)}
        onConfirm={toggle}
        loading={actionLoading}
        tone={confirming?.status === "ACTIVE" ? "danger" : "primary"}
        title={`${confirming?.status === "ACTIVE" ? "Deactivate" : "Activate"} college?`}
        description={`${confirming?.name || "This college"} will become ${confirming?.status === "ACTIVE" ? "inactive" : "active"}. Existing records will not be deleted.`}
        confirmLabel={confirming?.status === "ACTIVE" ? "Deactivate" : "Activate"}
      />
    </div>
  );
}
