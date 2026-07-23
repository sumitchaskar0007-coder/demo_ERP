import { CheckCircle2, Download, FileText, FolderOpen, Search, UserRound } from "lucide-react";
import { useEffect, useMemo, useState } from "react";
import { toast } from "sonner";
import { Button } from "@/components/common/Button";
import { Card } from "@/components/common/Card";
import { Input } from "@/components/common/Input";
import { Loader } from "@/components/common/Loader";
import { Pagination } from "@/components/common/Pagination";
import { Select } from "@/components/common/Select";
import { downloadAdmissionDocument, getStudentSectionAdmission } from "@/features/admissions/api";
import type {
  AdmissionDocumentType,
  StudentSectionAdmissionResponse,
} from "@/features/admissions/types";
import { getAdmissionAnalytics, type AdmissionAnalytics } from "@/features/reports/api";
import { handleApiError } from "@/lib/handleApiError";
import { cn } from "@/lib/utils";

const ACADEMIC_YEAR = "2026-2027";
const STUDENTS_PER_PAGE = 8;

const documentLabels: Record<AdmissionDocumentType, string> = {
  TENTH_MARKSHEET: "10th marksheet",
  TWELFTH_MARKSHEET: "12th marksheet",
  PROVISIONAL_CERTIFICATE: "Provisional certificate",
  TRANSFER_CERTIFICATE: "Transfer certificate",
  NATIONALITY_CERTIFICATE: "Nationality certificate",
  DOMICILE_CERTIFICATE: "Domicile certificate",
  AADHAAR_CARD: "Aadhaar card",
  GRADUATION_MARKSHEET: "Graduation marksheet",
  MIGRATION_CERTIFICATE: "Migration certificate",
  GAP_CERTIFICATE: "Gap certificate",
  ENTRANCE_SCORE_CARD: "Entrance score card",
  CASTE_CERTIFICATE: "Caste certificate",
  CASTE_VALIDITY: "Caste validity",
  NON_CREAMY_LAYER_CERTIFICATE: "Non-creamy layer certificate",
  NAME_CHANGE_CERTIFICATE: "Name-change certificate",
  INCOME_CERTIFICATE: "Income certificate",
  FORM_O_MINORITY: "Proforma-O minority document",
};

export function StudentDocumentsPage() {
  const [catalog, setCatalog] = useState<AdmissionAnalytics>();
  const [loading, setLoading] = useState(true);
  const [query, setQuery] = useState("");
  const [departmentId, setDepartmentId] = useState("");
  const [yearKey, setYearKey] = useState("");
  const [divisionId, setDivisionId] = useState("");
  const [page, setPage] = useState(0);
  const [selectedId, setSelectedId] = useState<number>();
  const [admission, setAdmission] = useState<StudentSectionAdmissionResponse>();
  const [detailLoading, setDetailLoading] = useState(false);
  const [downloading, setDownloading] = useState<AdmissionDocumentType>();

  useEffect(() => {
    let active = true;
    const load = async () => {
      try {
        const first = await getAdmissionAnalytics({
          academicYear: ACADEMIC_YEAR,
          page: 0,
          size: 100,
          sort: "newest",
        });
        const remaining = await Promise.all(
          Array.from({ length: Math.max(0, first.totalPages - 1) }, (_, index) =>
            getAdmissionAnalytics({
              academicYear: ACADEMIC_YEAR,
              page: index + 1,
              size: 100,
              sort: "newest",
            }),
          ),
        );
        if (active) {
          setCatalog({
            ...first,
            rows: [...first.rows, ...remaining.flatMap((page) => page.rows)],
          });
        }
      } catch (error) {
        toast.error(handleApiError(error).message);
      } finally {
        if (active) setLoading(false);
      }
    };
    load();
    return () => {
      active = false;
    };
  }, []);

  const yearOptions = useMemo(
    () => (catalog?.years ?? []).filter((year) => !departmentId || year.parentKey === departmentId),
    [catalog, departmentId],
  );
  const selectedYear = yearOptions.find((year) => year.key === yearKey);
  const divisionOptions = useMemo(
    () =>
      (catalog?.divisions ?? []).filter((division) => {
        if (yearKey) return division.parentKey === yearKey;
        if (departmentId) return division.parentKey.startsWith(`${departmentId}|`);
        return true;
      }),
    [catalog, departmentId, yearKey],
  );
  const students = useMemo(() => {
    const search = query.trim().toLowerCase();
    const hasCriteria = Boolean(search || departmentId || yearKey || divisionId);
    if (!hasCriteria) return [];
    return (catalog?.rows ?? []).filter(
      (row) =>
        Boolean(row.divisionId) &&
        (!search || row.studentName.toLowerCase().includes(search)) &&
        (!departmentId || String(row.departmentId) === departmentId) &&
        (!selectedYear || row.year === selectedYear.label) &&
        (!divisionId || String(row.divisionId) === divisionId),
    );
  }, [catalog, departmentId, divisionId, query, selectedYear, yearKey]);
  const totalPages = Math.ceil(students.length / STUDENTS_PER_PAGE);
  const visibleStudents = students.slice(page * STUDENTS_PER_PAGE, (page + 1) * STUDENTS_PER_PAGE);
  const hasCriteria = Boolean(query.trim() || departmentId || yearKey || divisionId);

  useEffect(() => {
    setPage(0);
    setSelectedId(undefined);
    setAdmission(undefined);
  }, [departmentId, divisionId, query, yearKey]);

  const selectStudent = async (id: number) => {
    setSelectedId(id);
    setAdmission(undefined);
    setDetailLoading(true);
    try {
      setAdmission(await getStudentSectionAdmission(id));
    } catch (error) {
      toast.error(handleApiError(error).message);
    } finally {
      setDetailLoading(false);
    }
  };

  const download = async (type: AdmissionDocumentType) => {
    if (!admission) return;
    setDownloading(type);
    try {
      const file = await downloadAdmissionDocument(admission.id, type);
      const url = URL.createObjectURL(file.blob);
      const anchor = document.createElement("a");
      anchor.href = url;
      anchor.download = `${filenamePart(admission.fullName)}_${filenamePart(documentLabels[type])}.${file.format.toLowerCase()}`;
      document.body.appendChild(anchor);
      anchor.click();
      anchor.remove();
      URL.revokeObjectURL(url);
      toast.success(`${documentLabels[type]} downloaded`);
    } catch (error) {
      toast.error(handleApiError(error).message);
    } finally {
      setDownloading(undefined);
    }
  };

  return (
    <div className="page-container space-y-6 pb-10">
      <header>
        <p className="text-xs font-bold uppercase tracking-[0.18em] text-brand-600">
          Student records
        </p>
        <h1 className="page-title mt-1">Documents</h1>
        <p className="page-subtitle">
          Find a student and securely download submitted admission documents.
        </p>
      </header>

      <Card className="p-5">
        <div className="grid gap-4 lg:grid-cols-4">
          <Input
            label="Search student"
            placeholder="Enter student name..."
            icon={<Search className="h-4 w-4" />}
            value={query}
            onChange={(event) => setQuery(event.target.value)}
          />
          <Select
            label="Department"
            value={departmentId}
            onChange={(event) => {
              setDepartmentId(event.target.value);
              setYearKey("");
              setDivisionId("");
            }}
            options={[
              { label: "All departments", value: "" },
              ...(catalog?.departments ?? []).map((item) => ({
                label: item.label,
                value: item.key,
              })),
            ]}
          />
          <Select
            label="Year"
            value={yearKey}
            onChange={(event) => {
              setYearKey(event.target.value);
              setDivisionId("");
            }}
            options={[
              { label: "All years", value: "" },
              ...yearOptions.map((item) => ({ label: item.label, value: item.key })),
            ]}
          />
          <Select
            label="Division"
            value={divisionId}
            onChange={(event) => setDivisionId(event.target.value)}
            options={[
              { label: "All divisions", value: "" },
              ...divisionOptions.map((item) => ({ label: item.label, value: item.key })),
            ]}
          />
        </div>
      </Card>

      {loading ? (
        <Loader label="Loading student documents..." />
      ) : (
        <div className="grid gap-6 xl:grid-cols-[0.9fr_1.1fr]">
          <Card className="overflow-hidden">
            <div className="flex items-center justify-between border-b px-5 py-4">
              <div>
                <h2 className="font-bold">Students</h2>
                <p className="mt-1 text-xs text-slate-500">Select a student to view documents</p>
              </div>
              <span className="rounded-full bg-brand-50 px-3 py-1 text-xs font-bold text-brand-700">
                {students.length} found
              </span>
            </div>
            <div className="max-h-[620px] space-y-2 overflow-y-auto p-3">
              {visibleStudents.map((student) => (
                <button
                  key={student.id}
                  onClick={() => selectStudent(student.id)}
                  className={cn(
                    "flex w-full items-center gap-3 rounded-xl border p-3 text-left transition",
                    selectedId === student.id
                      ? "border-brand-300 bg-brand-50 shadow-sm"
                      : "border-transparent bg-slate-50 hover:border-slate-200 hover:bg-white",
                  )}
                >
                  <span className="grid h-10 w-10 shrink-0 place-items-center rounded-xl bg-white text-brand-600 shadow-sm">
                    <UserRound className="h-5 w-5" />
                  </span>
                  <span className="min-w-0 flex-1">
                    <span className="block truncate text-sm font-bold text-slate-900">
                      {student.studentName}
                    </span>
                    <span className="mt-0.5 block truncate text-xs text-slate-500">
                      {student.departmentCode} · {student.year || "Year not allocated"} ·{" "}
                      {student.division || "No division"}
                    </span>
                  </span>
                  <span className="text-xs font-semibold text-slate-400">
                    {student.admissionNumber}
                  </span>
                </button>
              ))}
              {!hasCriteria ? (
                <div className="py-14 text-center">
                  <Search className="mx-auto h-8 w-8 text-slate-300" />
                  <p className="mt-3 text-sm font-semibold text-slate-600">
                    Search or apply a filter
                  </p>
                  <p className="mt-1 text-xs text-slate-400">
                    Allocated students will appear here.
                  </p>
                </div>
              ) : !students.length ? (
                <div className="py-14 text-center text-sm text-slate-500">
                  No allocated students match these filters.
                </div>
              ) : null}
            </div>
            {students.length > 0 && (
              <div className="border-t p-4">
                <Pagination
                  page={page}
                  totalPages={totalPages}
                  totalElements={students.length}
                  onChange={setPage}
                />
              </div>
            )}
          </Card>

          <Card className="min-h-[420px] overflow-hidden">
            {detailLoading ? (
              <Loader label="Loading document list..." />
            ) : admission ? (
              <>
                <div className="border-b bg-gradient-to-r from-brand-50 to-white px-6 py-5">
                  <div className="flex flex-wrap items-start justify-between gap-3">
                    <div>
                      <p className="text-xs font-bold uppercase tracking-wider text-brand-600">
                        Selected student
                      </p>
                      <h2 className="mt-1 text-xl font-bold">{admission.fullName}</h2>
                      <p className="mt-1 text-sm text-slate-500">
                        {admission.admissionNumber} · {admission.departmentName} ·{" "}
                        {admission.courseYearDisplayName || "Year not allocated"}
                      </p>
                      <p className="mt-2 text-xs font-medium text-slate-500">
                        Downloads are saved in their original PDF, PNG, or JPG format.
                      </p>
                    </div>
                    <span className="rounded-full bg-emerald-50 px-3 py-1.5 text-xs font-bold text-emerald-700">
                      {admission.uploadedDocuments.length} documents
                    </span>
                  </div>
                </div>
                <div className="grid gap-3 p-5 sm:grid-cols-2">
                  {admission.uploadedDocuments.map((type) => (
                    <div
                      key={type}
                      className="rounded-xl border border-slate-200 bg-white p-4 shadow-sm transition hover:border-brand-200 hover:shadow-md"
                    >
                      <div className="flex items-start gap-3">
                        <span className="grid h-10 w-10 shrink-0 place-items-center rounded-xl bg-emerald-50 text-emerald-600">
                          <FileText className="h-5 w-5" />
                        </span>
                        <div className="min-w-0 flex-1">
                          <p className="text-sm font-bold text-slate-900">{documentLabels[type]}</p>
                          <p className="mt-1 flex items-center gap-1 text-xs font-medium text-emerald-700">
                            <CheckCircle2 className="h-3.5 w-3.5" /> Uploaded
                          </p>
                        </div>
                      </div>
                      <Button
                        variant="secondary"
                        className="mt-4 w-full"
                        loading={downloading === type}
                        onClick={() => download(type)}
                      >
                        <Download className="h-4 w-4" />
                        Download
                      </Button>
                    </div>
                  ))}
                  {!admission.uploadedDocuments.length && (
                    <div className="col-span-full py-14 text-center text-sm text-slate-500">
                      This student has not uploaded any documents.
                    </div>
                  )}
                </div>
              </>
            ) : (
              <div className="grid min-h-[420px] place-items-center p-8 text-center">
                <div>
                  <span className="mx-auto grid h-16 w-16 place-items-center rounded-2xl bg-slate-100 text-slate-400">
                    <FolderOpen className="h-8 w-8" />
                  </span>
                  <h2 className="mt-4 font-bold">Select a student</h2>
                  <p className="mt-2 text-sm text-slate-500">
                    The uploaded document list will appear here.
                  </p>
                </div>
              </div>
            )}
          </Card>
        </div>
      )}
    </div>
  );
}

function filenamePart(value: string) {
  return value
    .trim()
    .toLowerCase()
    .replace(/[^a-z0-9]+/g, "_")
    .replace(/^_+|_+$/g, "");
}
