import { useEffect, useMemo, useRef, useState } from "react";
import { useNavigate, useParams } from "react-router-dom";
import { toast } from "sonner";
import { Card } from "@/components/common/Card";
import { Button } from "@/components/common/Button";
import { EmptyState } from "@/components/common/EmptyState";
import { Input } from "@/components/common/Input";
import { Pagination } from "@/components/common/Pagination";
import { Select } from "@/components/common/Select";
import { StatusBadge } from "@/components/common/Badge";
import { useAuth } from "@/features/auth/authStore";
import { weeklyTimetableApi, type WeeklyDivision } from "@/features/academics/api";
import { searchDepartments } from "@/features/departments/api";
import type { Department } from "@/features/departments/types";
import { handleApiError } from "@/lib/handleApiError";
import * as api from "@/features/fees/api";
import type { CreateFeeStructureRequest, FeeStructureResponse } from "@/features/fees/types";

const ACADEMIC_YEAR = "2026-2027";
const categoryOptions = ["OPEN", "OBC", "SC", "ST", "SBC", "VJNT", "EWS", "OTHER"].map((value) => ({
  label: value,
  value,
}));
const uniqueDepartments = (rows: Department[]) => [
  ...new Map(
    rows.map((row) => [`${row.collegeId}:${row.code.trim().toUpperCase()}`, row]),
  ).values(),
];

export function FeeStructureListPage() {
  const { user } = useAuth();
  const collegeId = user?.collegeId ?? 0;
  const [rows, setRows] = useState<FeeStructureResponse[]>([]);
  const [departments, setDepartments] = useState<Department[]>([]);
  const [divisions, setDivisions] = useState<WeeklyDivision[]>([]);
  const [saving, setSaving] = useState(false);
  const [editingId, setEditingId] = useState<number | null>(null);
  const [page, setPage] = useState(0);
  const [totalPages, setTotalPages] = useState(0);
  const [totalElements, setTotalElements] = useState(0);
  const latestRequest = useRef(0);
  const [filter, setFilter] = useState({ departmentId: "", studentCategory: "" });
  const [values, setValues] = useState({
    departmentId: "",
    courseYear: "",
    studentCategory: "OPEN",
    totalFee: "",
    minimumAmountForAdmission: "",
  });

  const load = (requestedPage = page) => {
    if (!collegeId) return Promise.resolve();
    const request = ++latestRequest.current;
    return api
      .searchFeeStructures({
        collegeId,
        departmentId: filter.departmentId || undefined,
        studentCategory: filter.studentCategory || undefined,
        page: requestedPage,
        size: 10,
      })
      .then((response) => {
        if (request !== latestRequest.current) return;
        setRows(response.content);
        setPage(response.page);
        setTotalPages(response.totalPages);
        setTotalElements(response.totalElements);
      })
      .catch((error) => toast.error(handleApiError(error).message));
  };

  useEffect(() => {
    if (!collegeId) return;
    searchDepartments({ collegeId, status: "ACTIVE", page: 0, size: 100 })
      .then((response) => setDepartments(uniqueDepartments(response.content)))
      .catch((error) => toast.error(handleApiError(error).message));
    weeklyTimetableApi
      .divisions()
      .then(setDivisions)
      .catch(() => setDivisions([]));
  }, [collegeId]);
  useEffect(() => {
    void load(0);
  }, [collegeId, filter]);

  const courseYears = useMemo(
    () => [
      ...new Set(
        divisions
          .filter((division) => division.departmentId === Number(values.departmentId))
          .map((division) => division.year),
      ),
    ],
    [divisions, values.departmentId],
  );

  const resetForm = () => {
    setEditingId(null);
    setValues({
      departmentId: "",
      courseYear: "",
      studentCategory: "OPEN",
      totalFee: "",
      minimumAmountForAdmission: "",
    });
  };
  const save = async () => {
    if (!collegeId || !values.departmentId || !values.courseYear)
      return toast.error("Select department and course year");
    const totalFee = Number(values.totalFee);
    const minimum = Number(values.minimumAmountForAdmission);
    if (totalFee <= 0 || minimum < 0 || minimum > totalFee)
      return toast.error("Check total and minimum fee amounts");
    setSaving(true);
    try {
      const payload = {
        collegeId,
        departmentId: Number(values.departmentId),
        academicYear: ACADEMIC_YEAR,
        courseYear: values.courseYear,
        studentCategory: values.studentCategory as CreateFeeStructureRequest["studentCategory"],
        title: `${values.courseYear} ${values.studentCategory} Fee`,
        totalFee,
        minimumAmountForAdmission: minimum,
        admissionFee: 0,
        tuitionFee: totalFee,
        examFee: 0,
        libraryFee: 0,
        otherFee: 0,
      };
      if (editingId) await api.updateFeeStructure(editingId, payload);
      else await api.createFeeStructure(payload);
      toast.success(editingId ? "Fee structure updated" : "Fee structure created");
      resetForm();
      await load(0);
    } catch (error) {
      toast.error(handleApiError(error).message);
    } finally {
      setSaving(false);
    }
  };

  return (
    <div className="page-container">
      <h1 className="page-title">Fee Setup</h1>
      <p className="page-subtitle">
        Set fees by department, course year, academic year and category.
      </p>
      <Card className="mt-6 p-5">
        <div className="grid gap-4 md:grid-cols-3">
          <Input
            label="College"
            value={user?.collegeName || "Assigned college"}
            readOnly
            className="cursor-not-allowed bg-slate-100"
          />
          <Select
            label="Department / Course"
            value={values.departmentId}
            disabled={Boolean(editingId)}
            options={[
              { label: "Select department", value: "" },
              ...departments.map((department) => ({
                label: `${department.code} - ${department.name}`,
                value: department.id,
              })),
            ]}
            onChange={(event) =>
              setValues({ ...values, departmentId: event.target.value, courseYear: "" })
            }
          />
          <Select
            label="Course Year"
            value={values.courseYear}
            disabled={!values.departmentId || Boolean(editingId)}
            options={[
              {
                label: values.departmentId ? "Select course year" : "Select department first",
                value: "",
              },
              ...courseYears.map((year) => ({ label: year, value: year })),
            ]}
            onChange={(event) => setValues({ ...values, courseYear: event.target.value })}
          />
          <Input
            label="Academic Year"
            value={ACADEMIC_YEAR}
            readOnly
            className="cursor-not-allowed bg-slate-100"
          />
          <Select
            label="Category"
            options={categoryOptions}
            value={values.studentCategory}
            disabled={Boolean(editingId)}
            onChange={(event) => setValues({ ...values, studentCategory: event.target.value })}
          />
          <Input
            label="Total Fee"
            type="number"
            min="1"
            value={values.totalFee}
            onChange={(event) => setValues({ ...values, totalFee: event.target.value })}
          />
          <Input
            label="Minimum Admission Fee"
            type="number"
            min="0"
            value={values.minimumAmountForAdmission}
            onChange={(event) =>
              setValues({ ...values, minimumAmountForAdmission: event.target.value })
            }
          />
        </div>
        <div className="mt-4 flex gap-2">
          <Button loading={saving} onClick={save}>
            {editingId ? "Update Fee Structure" : "Create Fee Structure"}
          </Button>
          {editingId && (
            <Button variant="secondary" onClick={resetForm}>
              Cancel
            </Button>
          )}
        </div>
      </Card>
      <Card className="mt-6 p-5">
        <h2 className="font-bold">Configured fees</h2>
        <div className="mt-4 grid gap-4 md:grid-cols-2">
          <Select
            label="Filter by department"
            options={[
              { label: "All departments", value: "" },
              ...departments.map((department) => ({
                label: `${department.code} - ${department.name}`,
                value: department.id,
              })),
            ]}
            value={filter.departmentId}
            onChange={(event) => setFilter({ ...filter, departmentId: event.target.value })}
          />
          <Select
            label="Filter by caste/category"
            options={[{ label: "All categories", value: "" }, ...categoryOptions]}
            value={filter.studentCategory}
            onChange={(event) => setFilter({ ...filter, studentCategory: event.target.value })}
          />
        </div>
        <div className="mt-4 space-y-2">
          {rows.map((row) => (
            <div
              key={row.id}
              className="flex flex-col gap-3 rounded-xl border p-3 lg:flex-row lg:items-center lg:justify-between"
            >
              <div className="flex gap-2">
                <Button
                  variant="secondary"
                  onClick={() => {
                    setEditingId(row.id);
                    setValues({
                      departmentId: String(row.departmentId),
                      courseYear: row.courseYear || "",
                      studentCategory: row.studentCategory || "OPEN",
                      totalFee: String(row.totalFee),
                      minimumAmountForAdmission: String(row.minimumAmountForAdmission),
                    });
                  }}
                >
                  Edit
                </Button>
                <Button
                  variant="danger"
                  onClick={async () => {
                    if (!window.confirm(`Delete fee structure "${row.title}"?`)) return;
                    try {
                      await api.deleteFeeStructure(row.id);
                      toast.success("Fee structure deleted");
                      await load();
                    } catch (error) {
                      toast.error(handleApiError(error).message);
                    }
                  }}
                >
                  Delete
                </Button>
              </div>
              <span className="text-sm">
                {row.departmentName} · {row.courseYear || "Course year not set"} ·{" "}
                {row.studentCategory || "OPEN"}
              </span>
              <b>₹{row.totalFee.toLocaleString("en-IN")}</b>
            </div>
          ))}
        </div>
        {!rows.length && (
          <EmptyState
            title="No fee structures"
            description="No configured fees match these filters."
          />
        )}
        <div className="mt-4">
          <Pagination
            page={page}
            totalPages={totalPages}
            totalElements={totalElements}
            onChange={(next) => void load(next)}
          />
        </div>
      </Card>
    </div>
  );
}

const initial: CreateFeeStructureRequest = {
  collegeId: 0,
  departmentId: 0,
  academicYear: ACADEMIC_YEAR,
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
  const [values, setValues] = useState({ ...initial, collegeId: user?.collegeId || 0 });
  const [departments, setDepartments] = useState<Department[]>([]);
  useEffect(() => {
    if (id) api.getFeeStructureById(Number(id)).then(setValues);
  }, [id]);
  useEffect(() => {
    if (values.collegeId)
      searchDepartments({ collegeId: values.collegeId, status: "ACTIVE", size: 100 }).then(
        (response) => setDepartments(uniqueDepartments(response.content)),
      );
  }, [values.collegeId]);
  const set = (key: keyof CreateFeeStructureRequest, value: string | number) =>
    setValues((current) => ({ ...current, [key]: value }));
  const submit = async () => {
    try {
      if (id) await api.updateFeeStructure(Number(id), values);
      else await api.createFeeStructure(values);
      toast.success(id ? "Fee structure updated" : "Fee structure created");
      nav("/fee-structures");
    } catch (error) {
      toast.error(handleApiError(error).message);
    }
  };
  return (
    <div className="page-container">
      <Card className="mx-auto max-w-4xl p-6">
        <h1 className="page-title">{id ? "Edit Fee Structure" : "Create Fee Structure"}</h1>
        <div className="mt-6 grid gap-4 md:grid-cols-2">
          <Input label="College" value={user?.collegeName || "Assigned college"} readOnly />
          <Select
            label="Department"
            value={values.departmentId}
            disabled={Boolean(id)}
            options={[
              { label: "Select department", value: 0 },
              ...departments.map((department) => ({
                label: department.name,
                value: department.id,
              })),
            ]}
            onChange={(event) => set("departmentId", Number(event.target.value))}
          />
          <Input label="Academic Year" value={values.academicYear} readOnly />
          {(
            [
              "title",
              "totalFee",
              "minimumAmountForAdmission",
              "admissionFee",
              "tuitionFee",
              "examFee",
              "libraryFee",
              "otherFee",
            ] as const
          ).map((key) => (
            <Input
              key={key}
              label={key.replace(/([A-Z])/g, " $1")}
              type={key === "title" ? "text" : "number"}
              value={values[key]}
              onChange={(event) =>
                set(key, key === "title" ? event.target.value : Number(event.target.value))
              }
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
  const [structure, setStructure] = useState<FeeStructureResponse | null>(null);
  useEffect(() => {
    if (id) api.getFeeStructureById(Number(id)).then(setStructure);
  }, [id]);
  if (!structure) return <div className="page-container">Loading...</div>;
  return (
    <div className="page-container">
      <Card className="p-7">
        <div className="flex justify-between">
          <h1 className="page-title">{structure.title}</h1>
          <StatusBadge status={structure.status} />
        </div>
        <div className="mt-6 grid gap-4 md:grid-cols-3">
          {Object.entries({
            College: structure.collegeName,
            Department: structure.departmentName,
            "Academic Year": structure.academicYear,
            "Course Year": structure.courseYear || "Not set",
            Category: structure.studentCategory || "OPEN",
            "Total Fee": `₹${structure.totalFee.toLocaleString("en-IN")}`,
            "Minimum Admission Fee": `₹${structure.minimumAmountForAdmission.toLocaleString("en-IN")}`,
            Description: structure.description || "-",
          }).map(([key, value]) => (
            <div key={key} className="rounded-xl bg-slate-50 p-4">
              <p className="text-xs font-bold uppercase text-slate-400">{key}</p>
              <p className="mt-1 font-semibold">{value}</p>
            </div>
          ))}
        </div>
      </Card>
    </div>
  );
}
