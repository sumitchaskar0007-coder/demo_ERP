import { CheckCircle2, Download, Eye, FileText, FolderOpen, Search, UserRound } from "lucide-react";
import { useCallback, useEffect, useMemo, useState } from "react";
import { toast } from "sonner";
import { AdmissionStatusBadge } from "@/components/admissions/components";
import { Button } from "@/components/common/Button";
import { Card } from "@/components/common/Card";
import { DocumentViewer } from "@/components/common/DocumentViewer";
import { Input } from "@/components/common/Input";
import { Loader } from "@/components/common/Loader";
import { Pagination } from "@/components/common/Pagination";
import { PrincipalAdmissionTabs } from "@/components/principal/PrincipalAdmissionTabs";
import {
  downloadAdmissionDocument,
  getAdmissionDocumentRequirements,
  getStudentSectionAdmission,
  searchStudentSectionAdmissions,
} from "@/features/admissions/api";
import type {
  AdmissionDocumentRequirement,
  AdmissionDocumentType,
  StudentSectionAdmissionResponse,
} from "@/features/admissions/types";
import { useAuth } from "@/features/auth/authStore";
import { PAGE_SIZE, ROLES } from "@/lib/constants";
import { handleApiError } from "@/lib/handleApiError";
import { cn } from "@/lib/utils";
import type { PageResponse } from "@/types/api";

const STUDENTS_PER_PAGE = Math.min(PAGE_SIZE, 10);

const documentLabels: Record<string, string> = {
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

const emptyPage: PageResponse<StudentSectionAdmissionResponse> = {
  content: [],
  page: 0,
  size: STUDENTS_PER_PAGE,
  totalElements: 0,
  totalPages: 0,
  last: true,
};

export function StudentDocumentsPage() {
  const { isRole } = useAuth();
  const principal = isRole([ROLES.PRINCIPAL]);
  const [result, setResult] = useState(emptyPage);
  const [loading, setLoading] = useState(true);
  const [query, setQuery] = useState("");
  const [page, setPage] = useState(0);
  const [selectedId, setSelectedId] = useState<number>();
  const [admission, setAdmission] = useState<StudentSectionAdmissionResponse>();
  const [requirements, setRequirements] = useState<AdmissionDocumentRequirement[]>([]);
  const [detailLoading, setDetailLoading] = useState(false);
  const [downloading, setDownloading] = useState<AdmissionDocumentType>();
  const [previewing, setPreviewing] = useState<AdmissionDocumentType>();
  const [preview, setPreview] = useState<{
    url: string;
    contentType: string;
    title: string;
    filename: string;
  }>();

  const loadAdmissions = useCallback(async () => {
    setLoading(true);
    try {
      setResult(
        await searchStudentSectionAdmissions({
          keyword: query.trim() || undefined,
          page,
          size: STUDENTS_PER_PAGE,
          sortBy: "createdAt",
          sortDir: "desc",
        }),
      );
    } catch (error) {
      toast.error(handleApiError(error).message);
      setResult(emptyPage);
    } finally {
      setLoading(false);
    }
  }, [page, query]);

  useEffect(() => {
    const timer = window.setTimeout(() => void loadAdmissions(), 250);
    return () => window.clearTimeout(timer);
  }, [loadAdmissions]);

  useEffect(() => {
    setPage(0);
    setSelectedId(undefined);
    setAdmission(undefined);
    setRequirements([]);
  }, [query]);

  useEffect(
    () => () => {
      if (preview) URL.revokeObjectURL(preview.url);
    },
    [preview],
  );

  const requirementNames = useMemo(
    () => new Map(requirements.map((item) => [item.documentKey, item.documentName])),
    [requirements],
  );
  const documentLabel = (type: AdmissionDocumentType) =>
    requirementNames.get(type) ?? documentLabels[type] ?? humanizeDocumentType(type);

  const selectStudent = async (id: number) => {
    setSelectedId(id);
    setAdmission(undefined);
    setRequirements([]);
    setDetailLoading(true);
    try {
      const [detail, configuredDocuments] = await Promise.all([
        getStudentSectionAdmission(id),
        getAdmissionDocumentRequirements(id),
      ]);
      setAdmission(detail);
      setRequirements(configuredDocuments);
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
      const file = await downloadAdmissionDocument(admission.id, type, { principal });
      const url = URL.createObjectURL(file.blob);
      const anchor = document.createElement("a");
      anchor.href = url;
      anchor.download = `${filenamePart(admission.fullName)}_${filenamePart(documentLabel(type))}.${file.format.toLowerCase()}`;
      document.body.appendChild(anchor);
      anchor.click();
      anchor.remove();
      URL.revokeObjectURL(url);
      toast.success(`${documentLabel(type)} downloaded`);
    } catch (error) {
      toast.error(handleApiError(error).message);
    } finally {
      setDownloading(undefined);
    }
  };

  const view = async (type: AdmissionDocumentType) => {
    if (!admission) return;
    setPreviewing(type);
    try {
      const file = await downloadAdmissionDocument(admission.id, type, { principal });
      if (preview) URL.revokeObjectURL(preview.url);
      setPreview({
        url: URL.createObjectURL(file.blob),
        contentType: file.blob.type,
        title: `${documentLabel(type)} — ${admission.fullName}`,
        filename: file.filename,
      });
    } catch (error) {
      toast.error(handleApiError(error).message);
    } finally {
      setPreviewing(undefined);
    }
  };

  return (
    <div className="page-container space-y-6 pb-10">
      {principal && <PrincipalAdmissionTabs />}
      <header>
        <p className="text-xs font-bold uppercase tracking-[0.18em] text-brand-600">
          Admission records
        </p>
        <h1 className="page-title mt-1">Uploaded Documents</h1>
        <p className="page-subtitle">
          View every admission student and the documents uploaded before class or division
          allocation.
        </p>
      </header>

      <Card className="p-5">
        <Input
          label="Search student"
          placeholder="Student name, email, phone, reference or admission number..."
          icon={<Search className="h-4 w-4" />}
          value={query}
          onChange={(event) => setQuery(event.target.value)}
        />
      </Card>

      <div className="grid gap-6 xl:grid-cols-[0.9fr_1.1fr]">
        <Card className="overflow-hidden">
          <div className="flex items-center justify-between border-b px-5 py-4">
            <div>
              <h2 className="font-bold">Admission Students</h2>
              <p className="mt-1 text-xs text-slate-500">
                Students appear here as soon as an admission is created.
              </p>
            </div>
            <span className="rounded-full bg-brand-50 px-3 py-1 text-xs font-bold text-brand-700">
              {result.totalElements} found
            </span>
          </div>
          {loading ? (
            <Loader label="Loading admission students..." />
          ) : (
            <>
              <div className="max-h-[620px] space-y-2 overflow-y-auto p-3">
                {result.content.map((student) => (
                  <button
                    key={student.id}
                    type="button"
                    onClick={() => void selectStudent(student.id)}
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
                        {student.fullName}
                      </span>
                      <span className="mt-0.5 block truncate text-xs text-slate-500">
                        {student.departmentCode} · {student.admissionReferenceNumber}
                      </span>
                      <span className="mt-1 block">
                        <AdmissionStatusBadge status={student.status} />
                      </span>
                    </span>
                    <span className="rounded-full bg-emerald-50 px-2 py-1 text-xs font-bold text-emerald-700">
                      {student.uploadedDocuments?.length ?? 0} docs
                    </span>
                  </button>
                ))}
                {!result.content.length && (
                  <div className="py-14 text-center text-sm text-slate-500">
                    No admission students match this search.
                  </div>
                )}
              </div>
              {result.totalPages > 1 && (
                <div className="border-t p-4">
                  <Pagination
                    page={result.page}
                    totalPages={result.totalPages}
                    totalElements={result.totalElements}
                    onChange={setPage}
                  />
                </div>
              )}
            </>
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
                      {admission.courseYearDisplayName || "Year not selected"}
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
                        <p className="text-sm font-bold text-slate-900">{documentLabel(type)}</p>
                        <p className="mt-1 flex items-center gap-1 text-xs font-medium text-emerald-700">
                          <CheckCircle2 className="h-3.5 w-3.5" /> Uploaded
                        </p>
                      </div>
                    </div>
                    <div className="mt-4 grid grid-cols-2 gap-2">
                      <Button
                        className="w-full"
                        loading={previewing === type}
                        onClick={() => void view(type)}
                      >
                        <Eye className="h-4 w-4" />
                        View
                      </Button>
                      <Button
                        variant="secondary"
                        className="w-full"
                        loading={downloading === type}
                        onClick={() => void download(type)}
                      >
                        <Download className="h-4 w-4" />
                        Download
                      </Button>
                    </div>
                  </div>
                ))}
                {!admission.uploadedDocuments.length && (
                  <div className="col-span-full py-14 text-center text-sm text-slate-500">
                    This student has not uploaded any documents yet.
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

      {preview && (
        <DocumentViewer
          open
          url={preview.url}
          contentType={preview.contentType}
          title={preview.title}
          filename={preview.filename}
          onClose={() => setPreview(undefined)}
        />
      )}
    </div>
  );
}

function humanizeDocumentType(type: string) {
  return type
    .toLowerCase()
    .split("_")
    .filter(Boolean)
    .map((part) => part.charAt(0).toUpperCase() + part.slice(1))
    .join(" ");
}

function filenamePart(value: string) {
  return value
    .trim()
    .toLowerCase()
    .replace(/[^a-z0-9]+/g, "_")
    .replace(/^_+|_+$/g, "");
}
