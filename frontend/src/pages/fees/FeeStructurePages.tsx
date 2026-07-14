import { useEffect, useState } from "react";
import { Link, useNavigate, useParams } from "react-router-dom";
import { toast } from "sonner";
import { Card } from "@/components/common/Card";
import { Button } from "@/components/common/Button";
import { Input } from "@/components/common/Input";
import { StatusBadge } from "@/components/common/Badge";
import { DataTable, type Column } from "@/components/table/DataTable";
import { useAuth } from "@/features/auth/authStore";
import { searchDepartments } from "@/features/departments/api";
import { handleApiError } from "@/lib/handleApiError";
import * as api from "@/features/fees/api";
import type { CreateFeeStructureRequest, FeeStructureResponse } from "@/features/fees/types";
export function FeeStructureListPage() {
  const [data, setData] = useState<FeeStructureResponse[]>([]);
  const [query, setQuery] = useState("");
  const load = () =>
    api
      .searchFeeStructures({ keyword: query || undefined, size: 50 })
      .then((x) => setData(x.content))
      .catch((e) => toast.error(handleApiError(e).message));
  useEffect(() => {
    load();
  }, []);
  const cols: Column<FeeStructureResponse>[] = [
    { key: "title", header: "Title", render: (r) => r.title },
    { key: "college", header: "College", render: (r) => r.collegeName },
    { key: "department", header: "Department", render: (r) => r.departmentName },
    { key: "year", header: "Academic Year", render: (r) => r.academicYear },
    { key: "total", header: "Total Fee", render: (r) => `₹${r.totalFee.toLocaleString("en-IN")}` },
    {
      key: "minimum",
      header: "Minimum",
      render: (r) => `₹${r.minimumAmountForAdmission.toLocaleString("en-IN")}`,
    },
    { key: "status", header: "Status", render: (r) => <StatusBadge status={r.status} /> },
    {
      key: "actions",
      header: "Actions",
      render: (r) => (
        <div className="flex gap-2">
          <Link to={`/fee-structures/${r.id}`}>
            <Button variant="secondary">View</Button>
          </Link>
          <Link to={`/fee-structures/${r.id}/edit`}><Button variant="secondary">Edit</Button></Link>
          <Button variant="danger" onClick={async () => {
            if (!window.confirm(`Delete fee structure "${r.title}"?`)) return;
            try { await api.deleteFeeStructure(r.id); toast.success("Fee structure deleted"); load(); }
            catch (e) { toast.error(handleApiError(e).message); }
          }}>Delete</Button>
          <Button
            variant="ghost"
            onClick={() =>
              api
                .setFeeStructureStatus(r.id, r.status === "ACTIVE" ? "INACTIVE" : "ACTIVE")
                .then(load)
            }
          >
            {r.status === "ACTIVE" ? "Deactivate" : "Activate"}
          </Button>
        </div>
      ),
    },
  ];
  return (
    <div className="page-container space-y-5">
      <div className="flex justify-between">
        <div>
          <h1 className="page-title">Fee Structures</h1>
          <p className="page-subtitle">Manage department fee plans without deleting history.</p>
        </div>
        <Link to="/fee-structures/create">
          <Button>Create Fee Structure</Button>
        </Link>
      </div>
      <Card className="p-4">
        <div className="flex gap-3">
          <Input
            placeholder="Search fee structures"
            value={query}
            onChange={(e) => setQuery(e.target.value)}
          />
          <Button onClick={load}>Search</Button>
        </div>
      </Card>
      <DataTable columns={cols} data={data} rowKey={(r) => r.id} />
    </div>
  );
}
const initial: CreateFeeStructureRequest = {
  collegeId: 0,
  departmentId: 0,
  academicYear: "2026-2027",
  title: "",
  description: "",
  totalFee: 0,
  minimumAmountForAdmission: 0,
  admissionFee: 0,
  tuitionFee: 0,
  examFee: 0,
  libraryFee: 0,
  otherFee: 0,
};
export function FeeStructureFormPage() {
  const { id } = useParams();
  const { user } = useAuth();
  const nav = useNavigate();
  const [v, setV] = useState({ ...initial, collegeId: user?.collegeId || 0 });
  const [departments, setDepartments] = useState<{ id: number; name: string }[]>([]);
  useEffect(() => {
    if (id) api.getFeeStructureById(Number(id)).then((x) => setV(x));
  }, [id]);
  useEffect(() => {
    if (v.collegeId)
      searchDepartments({ collegeId: v.collegeId, status: "ACTIVE", size: 100 }).then((x) =>
        setDepartments(x.content),
      );
  }, [v.collegeId]);
  const set = (k: keyof CreateFeeStructureRequest, x: string | number) =>
    setV((p) => ({ ...p, [k]: x }));
  const submit = async () => {
    if (v.totalFee <= 0 || v.minimumAmountForAdmission > v.totalFee)
      return toast.error("Check total and minimum fee amounts");
    try {
      if (id) await api.updateFeeStructure(Number(id), v);
      else await api.createFeeStructure(v);
      toast.success(id ? "Fee structure updated" : "Fee structure created");
      nav("/fee-structures");
    } catch (e) {
      toast.error(handleApiError(e).message);
    }
  };
  return (
    <div className="page-container">
      <Card className="mx-auto max-w-4xl p-6">
        <h1 className="page-title">{id ? "Edit Fee Structure" : "Create Fee Structure"}</h1>
        <div className="mt-6 grid gap-4 md:grid-cols-2">
          <Input
            label="College ID"
            type="number"
            value={v.collegeId}
            disabled={Boolean(user?.collegeId) || Boolean(id)}
            onChange={(e) => set("collegeId", Number(e.target.value))}
          />
          <label className="text-sm font-semibold">
            Department
            <select
              className="mt-2 h-10 w-full rounded-xl border px-3"
              value={v.departmentId}
              disabled={Boolean(id)}
              onChange={(e) => set("departmentId", Number(e.target.value))}
            >
              <option value={0}>Select department</option>
              {departments.map((d) => (
                <option key={d.id} value={d.id}>
                  {d.name}
                </option>
              ))}
            </select>
          </label>
          <Input
            label="Academic Year"
            value={v.academicYear}
            disabled={Boolean(id)}
            onChange={(e) => set("academicYear", e.target.value)}
          />
          <Input label="Title" value={v.title} onChange={(e) => set("title", e.target.value)} />
          {(
            [
              "totalFee",
              "minimumAmountForAdmission",
              "admissionFee",
              "tuitionFee",
              "examFee",
              "libraryFee",
              "otherFee",
            ] as const
          ).map((k) => (
            <Input
              key={k}
              label={k.replace(/([A-Z])/g, " $1")}
              type="number"
              value={v[k]}
              onChange={(e) => set(k, Number(e.target.value))}
            />
          ))}
        </div>
        <Button className="mt-6" onClick={submit}>
          {id ? "Update Fee Structure" : "Create Fee Structure"}
        </Button>
      </Card>
    </div>
  );
}
export function FeeStructureDetailsPage() {
  const { id } = useParams();
  const [x, setX] = useState<FeeStructureResponse | null>(null);
  useEffect(() => {
    if (id) api.getFeeStructureById(Number(id)).then(setX);
  }, [id]);
  if (!x) return <div className="page-container">Loading…</div>;
  return (
    <div className="page-container">
      <Card className="p-7">
        <div className="flex justify-between">
          <h1 className="page-title">{x.title}</h1>
          <StatusBadge status={x.status} />
        </div>
        <div className="mt-6 grid gap-4 md:grid-cols-3">
          {Object.entries({
            College: x.collegeName,
            Department: x.departmentName,
            "Academic Year": x.academicYear,
            "Total Fee": `₹${x.totalFee}`,
            "Minimum Admission Fee": `₹${x.minimumAmountForAdmission}`,
            Description: x.description || "—",
          }).map(([k, v]) => (
            <div key={k} className="rounded-xl bg-slate-50 p-4">
              <p className="text-xs font-bold uppercase text-slate-400">{k}</p>
              <p className="mt-1 font-semibold">{v}</p>
            </div>
          ))}
        </div>
      </Card>
    </div>
  );
}
