import {
  Check,
  CheckCircle2,
  Circle,
  FileCheck2,
  ImagePlus,
  Plus,
  Trash2,
  UploadCloud,
  X,
} from "lucide-react";
import { useCallback, useEffect, useId, useMemo, useRef, useState } from "react";
import { toast } from "sonner";
import { Button } from "@/components/common/Button";
import { Card } from "@/components/common/Card";
import { DocumentViewer } from "@/components/common/DocumentViewer";
import { Input } from "@/components/common/Input";
import { Select } from "@/components/common/Select";
import { Textarea } from "@/components/common/Textarea";
import {
  AdmissionDocumentsStep,
  AdmissionInformationStep,
  AdmissionStepNavigation,
  AdmissionValidationSummary,
  type AdmissionFormStep,
} from "@/components/admissions/AdmissionFormSteps";
import {
  useAdmissionDocumentUploadQueue,
  type AdmissionUploadQueueState,
} from "@/components/admissions/useAdmissionDocumentUploadQueue";
import * as api from "@/features/admissions/api";
import {
  validateAdmissionDocumentFile,
  validateAdmissionPhotoFile,
} from "@/features/admissions/fileValidation";
import type {
  AcademicRecord,
  AdmissionCourseYearOption,
  AdmissionDocumentRequirement,
  AdmissionDocumentType,
  AdmissionDocumentTransferStage,
  DetailedAdmissionRequest,
  EntranceExam,
  StudentSectionAdmissionResponse,
} from "@/features/admissions/types";
import { handleApiError } from "@/lib/handleApiError";
import { cn } from "@/lib/utils";
import { detailedAdmissionInformationSchema } from "@/lib/validators";

const qualifications: AcademicRecord["qualification"][] = ["10TH", "12TH", "DIPLOMA", "GRADUATION"];

const defaultDocumentDefinitions: {
  type: AdmissionDocumentType;
  label: string;
  required: boolean;
}[] = [
  { type: "TENTH_MARKSHEET", label: "10th marksheet", required: true },
  { type: "TWELFTH_MARKSHEET", label: "12th marksheet", required: true },
  { type: "PROVISIONAL_CERTIFICATE", label: "Provisional certificate", required: true },
  { type: "TRANSFER_CERTIFICATE", label: "Transfer certificate", required: true },
  { type: "NATIONALITY_CERTIFICATE", label: "Nationality certificate", required: true },
  { type: "DOMICILE_CERTIFICATE", label: "Domicile certificate", required: true },
  { type: "AADHAAR_CARD", label: "Aadhaar card", required: true },
  { type: "GRADUATION_MARKSHEET", label: "Graduation marksheet", required: false },
  { type: "MIGRATION_CERTIFICATE", label: "Migration certificate", required: false },
  { type: "GAP_CERTIFICATE", label: "Gap certificate", required: false },
  { type: "ENTRANCE_SCORE_CARD", label: "MH-CET / CMAT / ATMA score card", required: false },
  { type: "CASTE_CERTIFICATE", label: "Caste certificate", required: false },
  { type: "CASTE_VALIDITY", label: "Caste validity", required: false },
  { type: "NON_CREAMY_LAYER_CERTIFICATE", label: "Non-Creamy Layer certificate", required: false },
  { type: "NAME_CHANGE_CERTIFICATE", label: "Name change proof (if any)", required: false },
  { type: "INCOME_CERTIFICATE", label: "Income certificate", required: false },
  { type: "FORM_O_MINORITY", label: "Proforma-O (Only for Minority)", required: false },
];

const yearLabels = { FIRST_YEAR: "FY", SECOND_YEAR: "SY", THIRD_YEAR: "TY" } as const;

type DocumentTransferState = {
  stage: "queued" | AdmissionDocumentTransferStage | "error" | "cancelled";
  message?: string;
};

const documentTransferLabels: Record<DocumentTransferState["stage"], string> = {
  queued: "Waiting for an upload slot…",
  hashing: "Checking file integrity…",
  "requesting-upload": "Preparing secure upload…",
  uploading: "Uploading directly to secure storage…",
  verifying: "Verifying uploaded document…",
  "multipart-fallback": "Uploading through the application…",
  completed: "Upload verified",
  error: "Upload failed",
  cancelled: "Upload cancelled",
};

function normalizedSubmission(values: DetailedAdmissionRequest): DetailedAdmissionRequest {
  return {
    ...values,
    entranceExams: values.entranceExams
      .filter((exam) => exam.examName.trim() && exam.result.trim())
      .map((exam) => ({ examName: exam.examName.trim(), result: exam.result.trim() })),
  };
}

function zodFieldErrors(error: { issues: Array<{ path: PropertyKey[]; message: string }> }) {
  const errors: Record<string, string> = {};
  for (const issue of error.issues) {
    const field = issue.path.reduce<string>(
      (path, part) =>
        typeof part === "number"
          ? `${path}[${part}]`
          : path
            ? `${path}.${String(part)}`
            : String(part),
      "",
    );
    if (field && !errors[field]) errors[field] = issue.message;
  }
  return errors;
}

function initialValues(a: StudentSectionAdmissionResponse): DetailedAdmissionRequest {
  const records = qualifications.map((qualification) => {
    const saved = a.academicRecords?.find((record) => record.qualification === qualification);
    return saved
      ? {
          ...saved,
          gradingType: saved.gradingType ?? (saved.cgpa != null ? "CGPA" : "PERCENTAGE"),
        }
      : {
          qualification,
          instituteName: "",
          boardUniversity: "",
          yearOfPassing: "",
          totalMarks: undefined,
          obtainedMarks: undefined,
          gradingType: "PERCENTAGE" as const,
          marksPercentage: undefined,
          cgpa: undefined,
        };
  });
  return {
    courseYearId: a.courseYearId ?? 0,
    fullName: a.fullName,
    email: a.email,
    phone: a.phone,
    dateOfBirth: a.dateOfBirth,
    gender: a.gender?.toUpperCase() ?? "",
    placeOfBirth: a.placeOfBirth ?? "",
    maritalStatus: a.maritalStatus ?? "UNMARRIED",
    aadhaarNumber: a.aadhaarNumber ?? "",
    apaarId: a.apaarId ?? "",
    nationality: a.nationality ?? "Indian",
    religion: a.religion ?? "",
    caste: a.caste ?? "",
    studentCategory: a.studentCategory,
    customCategoryName: a.customCategoryName ?? "",
    parentName: a.parentName,
    parentPhone: a.parentPhone,
    parentEmail: a.parentEmail ?? "",
    addressLine1: a.addressLine1 ?? "",
    addressLine2: a.addressLine2 ?? "",
    city: a.city ?? "",
    pincode: a.pincode ?? "",
    state: a.state ?? "",
    correspondenceAddress: a.correspondenceAddress ?? a.addressLine1 ?? "",
    correspondenceCity: a.correspondenceCity ?? a.city ?? "",
    correspondencePincode: a.correspondencePincode ?? a.pincode ?? "",
    correspondenceState: a.correspondenceState ?? a.state ?? "",
    academicRecords: records,
    entranceExams: a.entranceExams?.length
      ? a.entranceExams
      : a.qualifyingEntranceSeatNumber || a.qualifyingEntranceTotalScore != null
        ? [
            {
              examName: a.qualifyingEntranceSeatNumber || "Qualifying entrance test",
              result: a.qualifyingEntranceTotalScore?.toString() || "Not specified",
            },
          ]
        : [{ examName: "", result: "" }],
    qualifyingEntranceSeatNumber: a.qualifyingEntranceSeatNumber ?? "",
    qualifyingEntranceTotalScore: a.qualifyingEntranceTotalScore ?? undefined,
    lastGraduationCollegeName: a.lastGraduationCollegeName ?? "",
    lastGraduationCollegeAddress: a.lastGraduationCollegeAddress ?? "",
  };
}

function restoredDraftValues(
  admission: StudentSectionAdmissionResponse,
  draft: Partial<DetailedAdmissionRequest> | null,
): DetailedAdmissionRequest {
  const initial = initialValues(admission);
  if (!draft) return initial;
  const draftRecords = Array.isArray(draft.academicRecords) ? draft.academicRecords : [];
  return {
    ...initial,
    ...draft,
    academicRecords: initial.academicRecords.map((record) => {
      const saved = draftRecords.find((item) => item.qualification === record.qualification);
      if (!saved) return record;
      const gradingType = saved.gradingType ?? (saved.cgpa != null ? "CGPA" : "PERCENTAGE");
      return {
        ...record,
        ...saved,
        gradingType,
        totalMarks: gradingType === "CGPA" ? undefined : saved.totalMarks,
        obtainedMarks: gradingType === "CGPA" ? undefined : saved.obtainedMarks,
        marksPercentage: gradingType === "CGPA" ? undefined : saved.marksPercentage,
        cgpa: gradingType === "CGPA" ? saved.cgpa : undefined,
      };
    }),
  };
}

export function DetailedAdmissionForm({
  admission,
  onSaved,
  studentOwned = false,
}: {
  admission: StudentSectionAdmissionResponse;
  onSaved: () => Promise<void>;
  studentOwned?: boolean;
}) {
  const [values, setValues] = useState(() => initialValues(admission));
  const [activeStep, setActiveStep] = useState<AdmissionFormStep>("information");
  const [informationValidated, setInformationValidated] = useState(false);
  const [fieldErrors, setFieldErrors] = useState<Record<string, string>>({});
  const [validatingInformation, setValidatingInformation] = useState(false);
  const [photo, setPhoto] = useState<File | null>(null);
  const [photoUploaded, setPhotoUploaded] = useState(admission.photoAvailable);
  const [photoTransfer, setPhotoTransfer] = useState<DocumentTransferState | undefined>();
  const [documents, setDocuments] = useState<Partial<Record<AdmissionDocumentType, File>>>({});
  const [locallyUploadedDocuments, setLocallyUploadedDocuments] = useState<AdmissionDocumentType[]>(
    [],
  );
  const [documentTransfers, setDocumentTransfers] = useState<
    Partial<Record<AdmissionDocumentType, DocumentTransferState>>
  >({});
  const [courseYears, setCourseYears] = useState<AdmissionCourseYearOption[]>([]);
  const [categoryOptions, setCategoryOptions] = useState<
    Awaited<ReturnType<typeof api.getPublicAdmissionCategories>>
  >(() =>
    admission.studentCategory === "OTHER" && admission.customCategoryName
      ? [
          {
            category: "OTHER",
            customCategoryName: admission.customCategoryName,
            label: admission.customCategoryName,
          },
        ]
      : [],
  );
  const [documentDefinitions, setDocumentDefinitions] = useState(defaultDocumentDefinitions);
  const [courseYearsLoading, setCourseYearsLoading] = useState(true);
  const [sameAddress, setSameAddress] = useState(false);
  const [saving, setSaving] = useState(false);
  const [canCancelUploads, setCanCancelUploads] = useState(false);
  const [preview, setPreview] = useState<string | null>(null);
  const [draftReady, setDraftReady] = useState(!studentOwned);
  const [draftStatus, setDraftStatus] = useState<
    "loading" | "idle" | "saving" | "saved" | "conflict" | "error"
  >(studentOwned ? "loading" : "idle");
  const draftVersion = useRef(0);
  const draftBlocked = useRef(false);
  const lastSavedDraft = useRef("");
  const uploadAbortController = useRef<AbortController | null>(null);
  const photoUploadController = useRef<AbortController | null>(null);
  const rememberUploadedDocument = useCallback((type: AdmissionDocumentType) => {
    setLocallyUploadedDocuments((current) =>
      current.includes(type) ? current : [...current, type],
    );
  }, []);
  const uploadStudentDocument = useCallback(
    (
      type: AdmissionDocumentType,
      file: File,
      options: import("@/features/admissions/types").AdmissionDocumentTransferOptions,
    ) => api.uploadMyAdmissionDocument(type, file, options),
    [],
  );
  const {
    transfers: studentDocumentTransfers,
    enqueue: enqueueStudentDocument,
    cancel: cancelStudentDocument,
    retry: retryStudentDocument,
    clear: clearStudentDocument,
    reset: resetStudentDocumentQueue,
    hasPendingUploads: hasPendingDocumentUploads,
    hasFailedUploads: hasFailedDocumentUploads,
  } = useAdmissionDocumentUploadQueue({
    upload: uploadStudentDocument,
    onUploaded: rememberUploadedDocument,
    maxConcurrent: 2,
  });
  const uploadedDocumentTypes = useMemo(
    () => new Set([...(admission.uploadedDocuments ?? []), ...locallyUploadedDocuments]),
    [admission.uploadedDocuments, locallyUploadedDocuments],
  );
  const missingRequiredDocuments = useMemo(
    () =>
      documentDefinitions.filter(
        (item) =>
          item.required &&
          !uploadedDocumentTypes.has(item.type) &&
          (!documents[item.type] || studentOwned),
      ),
    [documentDefinitions, documents, studentOwned, uploadedDocumentTypes],
  );
  const aadhaarValid = /^\d{12}$/.test(values.aadhaarNumber);
  const permanentPinValid = /^\d{6}$/.test(values.pincode);
  const correspondencePinValid = /^\d{6}$/.test(values.correspondencePincode);

  const formSteps = useMemo(() => {
    const has = (...fields: (keyof DetailedAdmissionRequest)[]) =>
      fields.every((field) => String(values[field] ?? "").trim().length > 0);
    const requiredDocumentsReady = documentDefinitions
      .filter((item) => item.required)
      .every((item) => uploadedDocumentTypes.has(item.type) || Boolean(documents[item.type]));
    const academicReady = values.academicRecords.some(
      (record) =>
        Boolean(record.instituteName?.trim()) &&
        Boolean(record.boardUniversity?.trim()) &&
        Boolean(record.yearOfPassing?.trim()),
    );
    return [
      { label: "Photo", complete: photoUploaded || (!studentOwned && Boolean(photo)) },
      { label: "Course", complete: values.courseYearId > 0 },
      {
        label: "Applicant",
        complete:
          has(
            "fullName",
            "phone",
            "email",
            "dateOfBirth",
            "gender",
            "placeOfBirth",
            "aadhaarNumber",
            "nationality",
            "religion",
            "caste",
          ) && aadhaarValid,
      },
      { label: "Guardian", complete: has("parentName", "parentPhone") },
      {
        label: "Permanent",
        complete: has("addressLine1", "city", "pincode", "state") && permanentPinValid,
      },
      {
        label: "Correspondence",
        complete:
          has(
            "correspondenceAddress",
            "correspondenceCity",
            "correspondencePincode",
            "correspondenceState",
          ) && correspondencePinValid,
      },
      { label: "Academic", complete: academicReady },
      { label: "Documents", complete: requiredDocumentsReady },
    ];
  }, [
    aadhaarValid,
    photoUploaded,
    correspondencePinValid,
    documentDefinitions,
    documents,
    permanentPinValid,
    photo,
    studentOwned,
    uploadedDocumentTypes,
    values,
  ]);
  const completedSteps = formSteps.filter((step) => step.complete).length;
  const completionPercentage = Math.round((completedSteps / formSteps.length) * 100);

  useEffect(() => {
    if (!studentOwned) setValues(initialValues(admission));
  }, [admission, studentOwned]);
  useEffect(() => {
    setActiveStep("information");
    setInformationValidated(false);
    setFieldErrors({});
    setPhotoUploaded(admission.photoAvailable);
    setPhotoTransfer(undefined);
    photoUploadController.current?.abort();
    resetStudentDocumentQueue();
  }, [admission.id, admission.photoAvailable, resetStudentDocumentQueue]);
  useEffect(() => {
    if (!studentOwned) return;
    let active = true;
    setDraftReady(false);
    setDraftStatus("loading");
    draftBlocked.current = false;
    api
      .getMyAdmissionDetailDraft()
      .then((draft) => {
        if (!active) return;
        draftVersion.current = draft.version;
        const restored = restoredDraftValues(admission, draft.values);
        setValues(restored);
        lastSavedDraft.current = JSON.stringify(restored);
        setDraftStatus(draft.values ? "saved" : "idle");
        setDraftReady(true);
      })
      .catch((error) => {
        if (!active) return;
        setDraftStatus("error");
        toast.error(`Draft could not be restored: ${handleApiError(error).message}`);
      });
    return () => {
      active = false;
    };
  }, [admission, admission.id, studentOwned]);

  useEffect(() => {
    if (!studentOwned || !draftReady || draftBlocked.current || saving) return;
    const serialized = JSON.stringify(values);
    if (serialized === lastSavedDraft.current) return;
    const timer = window.setTimeout(() => {
      setDraftStatus("saving");
      api
        .saveMyAdmissionDetailDraft(values, draftVersion.current)
        .then((draft) => {
          draftVersion.current = draft.version;
          lastSavedDraft.current = serialized;
          setDraftStatus("saved");
        })
        .catch((error) => {
          const detail = handleApiError(error);
          if (detail.status === 409) {
            draftBlocked.current = true;
            setDraftStatus("conflict");
            toast.error(
              "This form was updated in another tab. Reload before continuing to avoid overwriting it.",
            );
          } else {
            setDraftStatus("error");
          }
        });
    }, 1200);
    return () => window.clearTimeout(timer);
  }, [draftReady, saving, studentOwned, values]);
  useEffect(() => {
    setLocallyUploadedDocuments([]);
    setDocumentTransfers({});
  }, [admission.id]);
  useEffect(
    () => () => {
      uploadAbortController.current?.abort();
      photoUploadController.current?.abort();
    },
    [],
  );
  useEffect(() => {
    let active = true;
    setCourseYearsLoading(true);
    const request = studentOwned
      ? api.getMyAdmissionCourseYears()
      : api.getAdmissionCourseYears(admission.id);
    request
      .then((options) => {
        if (!active) return;
        setCourseYears(options);
        setValues((current) => ({
          ...current,
          courseYearId: current.courseYearId || options[0]?.id || 0,
        }));
      })
      .catch((error) => toast.error(handleApiError(error).message))
      .finally(() => active && setCourseYearsLoading(false));
    return () => {
      active = false;
    };
  }, [admission.id, studentOwned]);
  useEffect(() => {
    let active = true;
    const savedCategory =
      admission.studentCategory === "OTHER" && admission.customCategoryName
        ? [
            {
              category: "OTHER" as const,
              customCategoryName: admission.customCategoryName,
              label: admission.customCategoryName,
            },
          ]
        : [];
    setCategoryOptions(savedCategory);
    api
      .getPublicAdmissionCategories(admission.collegeCode, admission.departmentId, {
        gender: values.gender || undefined,
        academicYear: admission.academicYear,
        courseYear: courseYears.find((year) => year.id === Number(values.courseYearId))
          ?.displayName,
      })
      .then((options) => {
        if (active) setCategoryOptions(options);
      })
      .catch((error) => {
        if (!active) return;
        setCategoryOptions(savedCategory);
        toast.error(`Fee categories could not be loaded: ${handleApiError(error).message}`);
      });
    return () => {
      active = false;
    };
  }, [
    admission.collegeCode,
    admission.customCategoryName,
    admission.departmentId,
    admission.studentCategory,
    admission.academicYear,
    courseYears,
    values.courseYearId,
    values.gender,
  ]);
  useEffect(() => {
    let active = true;
    const request = studentOwned
      ? api.getMyAdmissionDocumentRequirements()
      : api.getAdmissionDocumentRequirements(admission.id);
    request
      .then((items) => {
        if (!active) return;
        setDocumentDefinitions(
          items.map((item) => ({
            type: item.documentKey,
            label: item.documentName,
            required: item.required,
          })),
        );
      })
      .catch((error) => toast.error(handleApiError(error).message));
    return () => {
      active = false;
    };
  }, [admission.id, studentOwned]);
  useEffect(() => {
    if (photo) {
      const url = URL.createObjectURL(photo);
      setPreview(url);
      return () => URL.revokeObjectURL(url);
    }
    if (!admission.photoAvailable) {
      setPreview(null);
      return;
    }
    let active = true;
    let url = "";
    const photoRequest = studentOwned
      ? api.getMyAdmissionPhoto()
      : api.getAdmissionPhoto(admission.id);
    photoRequest
      .then((value) => {
        url = value;
        if (active) setPreview(value);
      })
      .catch(() => setPreview(null));
    return () => {
      active = false;
      if (url) URL.revokeObjectURL(url);
    };
  }, [admission.id, admission.photoAvailable, photo, studentOwned]);

  const clearFieldError = (field: string) =>
    setFieldErrors((current) => {
      if (!current[field]) return current;
      const next = { ...current };
      delete next[field];
      return next;
    });
  const markInformationChanged = (field: string) => {
    clearFieldError(field);
    if (studentOwned) setInformationValidated(false);
  };
  const set = (name: keyof DetailedAdmissionRequest, value: string | number | undefined) => {
    markInformationChanged(String(name));
    setValues((current) => ({ ...current, [name]: value }));
  };
  const updateRecord = (
    index: number,
    name: keyof AcademicRecord,
    value: string | number | undefined,
  ) => {
    markInformationChanged(`academicRecords[${index}].${String(name)}`);
    setValues((current) => ({
      ...current,
      academicRecords: current.academicRecords.map((record, recordIndex) => {
        if (recordIndex !== index) return record;
        const updated = { ...record, [name]: value };
        if (name === "gradingType") {
          if (value === "PERCENTAGE") updated.cgpa = undefined;
          if (value === "CGPA") {
            updated.totalMarks = undefined;
            updated.obtainedMarks = undefined;
            updated.marksPercentage = undefined;
          }
        }
        if (name === "totalMarks" || name === "obtainedMarks") {
          const total = Number(updated.totalMarks);
          const obtained = Number(updated.obtainedMarks);
          updated.marksPercentage =
            total > 0 && obtained >= 0 && obtained <= total
              ? Math.round((obtained / total) * 10000) / 100
              : undefined;
        }
        return updated;
      }),
    }));
  };

  const updateEntranceExam = (index: number, name: keyof EntranceExam, value: string) => {
    markInformationChanged(`entranceExams[${index}].${String(name)}`);
    setValues((current) => ({
      ...current,
      entranceExams: current.entranceExams.map((exam, examIndex) =>
        examIndex === index ? { ...exam, [name]: value } : exam,
      ),
    }));
  };

  const addEntranceExam = () => {
    setInformationValidated(false);
    setValues((current) => ({
      ...current,
      entranceExams:
        current.entranceExams.length >= 10
          ? current.entranceExams
          : [...current.entranceExams, { examName: "", result: "" }],
    }));
  };

  const removeEntranceExam = (index: number) => {
    setInformationValidated(false);
    setFieldErrors((current) =>
      Object.fromEntries(
        Object.entries(current).filter(([field]) => !field.startsWith("entranceExams[")),
      ),
    );
    setValues((current) => ({
      ...current,
      entranceExams:
        current.entranceExams.length === 1
          ? [{ examName: "", result: "" }]
          : current.entranceExams.filter((_, examIndex) => examIndex !== index),
    }));
  };

  const copyPermanentAddress = (checked: boolean) => {
    setSameAddress(checked);
    if (!checked) return;
    setInformationValidated(false);
    for (const field of [
      "correspondenceAddress",
      "correspondenceCity",
      "correspondencePincode",
      "correspondenceState",
    ]) {
      clearFieldError(field);
    }
    setValues((current) => ({
      ...current,
      correspondenceAddress: [current.addressLine1, current.addressLine2]
        .filter(Boolean)
        .join("\n"),
      correspondenceCity: current.city,
      correspondencePincode: current.pincode,
      correspondenceState: current.state,
    }));
  };

  const focusInformationField = (field: string) => {
    const named = document.getElementsByName(field)[0];
    const element = named ?? document.getElementById(field);
    if (!element) return;
    element.scrollIntoView({ behavior: "smooth", block: "center" });
    window.setTimeout(() => {
      if (element instanceof HTMLSelectElement && element.offsetParent === null) {
        element.parentElement?.querySelector<HTMLButtonElement>("button")?.focus();
      } else {
        element.focus();
      }
    }, 350);
  };

  const validateInformation = async () => {
    setValidatingInformation(true);
    setFieldErrors({});
    const parsed = detailedAdmissionInformationSchema.safeParse(values);
    if (!parsed.success) {
      const errors = zodFieldErrors(parsed.error);
      setFieldErrors(errors);
      const first = Object.keys(errors)[0];
      if (first) focusInformationField(first);
      toast.error("Please correct the highlighted information before continuing.");
      setValidatingInformation(false);
      return;
    }
    try {
      await api.validateMyAdmissionDetails(normalizedSubmission(values));
      setInformationValidated(true);
      setActiveStep("documents");
      window.scrollTo({ top: 0, behavior: "smooth" });
    } catch (error) {
      const detail = handleApiError(error);
      setFieldErrors(detail.fieldErrors);
      const first = Object.keys(detail.fieldErrors)[0];
      if (first) focusInformationField(first);
      toast.error(first ? detail.fieldErrors[first] : detail.message);
    } finally {
      setValidatingInformation(false);
    }
  };

  const uploadPhotoImmediately = async (selected: File) => {
    setPhoto(selected);
    if (!studentOwned) return;
    photoUploadController.current?.abort();
    const controller = new AbortController();
    photoUploadController.current = controller;
    setPhotoTransfer({ stage: "uploading" });
    try {
      await api.uploadMyAdmissionPhoto(selected, controller.signal);
      if (controller.signal.aborted) return;
      setPhotoUploaded(true);
      setPhotoTransfer({ stage: "completed" });
    } catch (error) {
      const cancelled = controller.signal.aborted;
      setPhotoTransfer({
        stage: cancelled ? "cancelled" : "error",
        message: cancelled
          ? "Upload cancelled. You can retry when ready."
          : handleApiError(error).message,
      });
    } finally {
      if (photoUploadController.current === controller) photoUploadController.current = null;
    }
  };

  const save = async (event: React.FormEvent) => {
    event.preventDefault();
    if (studentOwned && activeStep === "information") {
      await validateInformation();
      return;
    }
    if (studentOwned && !informationValidated) {
      setActiveStep("information");
      toast.error("Review and validate your information before submitting documents.");
      return;
    }
    const invalidField = !aadhaarValid
      ? { id: "aadhaar-number", message: "Aadhaar number must contain exactly 12 digits" }
      : !permanentPinValid
        ? {
            id: "permanent-pin",
            message: "Permanent-address PIN code must contain exactly 6 digits",
          }
        : !correspondencePinValid
          ? {
              id: "correspondence-pin",
              message: "Correspondence-address PIN code must contain exactly 6 digits",
            }
          : null;
    if (invalidField) {
      toast.error(invalidField.message);
      const field = document.getElementById(invalidField.id);
      field?.scrollIntoView({ behavior: "smooth", block: "center" });
      window.setTimeout(() => field?.focus(), 350);
      return;
    }
    if (!photoUploaded && (!photo || studentOwned)) {
      toast.error("Passport-size photo is required");
      return;
    }
    if (!values.courseYearId) {
      toast.error("Select FY, SY, or TY for the chosen department");
      return;
    }
    if (values.studentCategory === "OTHER" && !values.customCategoryName?.trim()) {
      toast.error("Select an Other category");
      document.getElementById("other-category")?.scrollIntoView({
        behavior: "smooth",
        block: "center",
      });
      return;
    }
    const categoryConfigured = categoryOptions.some(
      (option) =>
        option.category === values.studentCategory &&
        (values.studentCategory !== "OTHER" ||
          option.customCategoryName?.toUpperCase() ===
            values.customCategoryName?.trim().toUpperCase()),
    );
    if (!categoryConfigured) {
      toast.error("Select an active fee category for this course year and gender");
      document.getElementById("student-category")?.scrollIntoView({
        behavior: "smooth",
        block: "center",
      });
      return;
    }
    const incompleteExam = values.entranceExams.find(
      (exam) => Boolean(exam.examName.trim()) !== Boolean(exam.result.trim()),
    );
    if (incompleteExam) {
      toast.error("Enter both entrance exam name and result, or remove the incomplete row");
      document
        .getElementById("entrance-exams")
        ?.scrollIntoView({ behavior: "smooth", block: "center" });
      return;
    }
    if (missingRequiredDocuments.length) {
      toast.error(
        `Upload required documents: ${missingRequiredDocuments.map((item) => item.label).join(", ")}`,
      );
      document
        .getElementById("required-documents")
        ?.scrollIntoView({ behavior: "smooth", block: "center" });
      return;
    }
    if (studentOwned && hasPendingDocumentUploads) {
      toast.error("Wait for the selected documents to finish uploading.");
      return;
    }
    if (studentOwned && hasFailedDocumentUploads) {
      toast.error("Retry or replace the documents whose upload failed.");
      return;
    }
    if (studentOwned && photoTransfer?.stage === "uploading") {
      toast.error("Wait for the passport photo to finish uploading.");
      return;
    }
    if (
      studentOwned &&
      (photoTransfer?.stage === "error" || photoTransfer?.stage === "cancelled")
    ) {
      toast.error("Retry the passport photo upload or remove the failed replacement.");
      return;
    }
    setSaving(true);
    const controller = new AbortController();
    uploadAbortController.current = controller;
    try {
      if (!studentOwned && photo) await api.uploadAdmissionPhoto(admission.id, photo);
      const selectedDocuments = studentOwned
        ? []
        : (Object.entries(documents) as [AdmissionDocumentType, File][]);
      setCanCancelUploads(selectedDocuments.length > 0);
      for (const [type, file] of selectedDocuments) {
        const onProgress = ({ stage }: { stage: AdmissionDocumentTransferStage }) =>
          setDocumentTransfers((current) => ({ ...current, [type]: { stage } }));
        try {
          await api.uploadAdmissionDocument(admission.id, type, file, {
            signal: controller.signal,
            onProgress,
          });
          setLocallyUploadedDocuments((current) =>
            current.includes(type) ? current : [...current, type],
          );
          setDocuments((current) => {
            const next = { ...current };
            delete next[type];
            return next;
          });
        } catch (error) {
          const cancelled = controller.signal.aborted;
          setDocumentTransfers((current) => ({
            ...current,
            [type]: {
              stage: cancelled ? "cancelled" : "error",
              message: cancelled
                ? "Choose Submit again when you are ready to retry."
                : handleApiError(error).message,
            },
          }));
          throw error;
        }
      }
      setCanCancelUploads(false);
      const submission = normalizedSubmission(values);
      if (studentOwned) await api.submitMyAdmissionDetails(submission);
      else await api.updateAdmissionDetails(admission.id, submission);
      toast.success(
        studentOwned ? "Admission form submitted for review" : "Detailed admission form saved",
      );
      setPhoto(null);
      setDocuments({});
      await onSaved();
    } catch (error) {
      if (controller.signal.aborted) {
        toast.error("Document upload cancelled. No automatic retry was attempted.");
      } else {
        const apiError = handleApiError(error);
        if (studentOwned && Object.keys(apiError.fieldErrors).length > 0) {
          setFieldErrors(apiError.fieldErrors);
          setInformationValidated(false);
          setActiveStep("information");
          const first = Object.keys(apiError.fieldErrors)[0];
          window.setTimeout(() => focusInformationField(first), 0);
        }
        toast.error(Object.values(apiError.fieldErrors)[0] ?? apiError.message);
      }
    } finally {
      if (uploadAbortController.current === controller) uploadAbortController.current = null;
      setCanCancelUploads(false);
      setSaving(false);
    }
  };

  return (
    <form onSubmit={save} noValidate={studentOwned} className="space-y-5">
      <Card className="overflow-hidden border-blue-100 bg-gradient-to-br from-blue-50 via-white to-indigo-50 p-5 sm:p-6">
        <div className="flex flex-col gap-5 lg:flex-row lg:items-end lg:justify-between">
          <div>
            <p className="text-xs font-bold uppercase tracking-[0.18em] text-blue-600">
              Admission application
            </p>
            <h2 className="mt-2 text-2xl font-bold text-slate-950">Complete your detailed form</h2>
            <p className="mt-1 max-w-2xl text-sm text-slate-600">
              Complete every required section and attach the documents marked required before
              submitting.
            </p>
            {studentOwned && (
              <p
                className={cn(
                  "mt-2 text-xs font-semibold",
                  draftStatus === "conflict" || draftStatus === "error"
                    ? "text-rose-600"
                    : "text-emerald-700",
                )}
              >
                {draftStatus === "loading" && "Restoring saved draft…"}
                {draftStatus === "saving" && "Saving draft…"}
                {draftStatus === "saved" && "Draft saved securely"}
                {draftStatus === "idle" && "Draft autosave is ready"}
                {draftStatus === "conflict" && "Newer changes exist in another tab—reload required"}
                {draftStatus === "error" && "Draft autosave failed—keep this tab open and retry"}
              </p>
            )}
          </div>
          <div className="min-w-48 rounded-2xl border border-white/80 bg-white/80 p-4 shadow-sm">
            <div className="flex items-center justify-between text-sm">
              <span className="font-semibold text-slate-600">Form progress</span>
              <span className="font-bold text-blue-700">{completionPercentage}%</span>
            </div>
            <div className="mt-2 h-2 overflow-hidden rounded-full bg-slate-200">
              <div
                className="h-full rounded-full bg-gradient-to-r from-blue-600 to-indigo-500 transition-all duration-500"
                style={{ width: `${completionPercentage}%` }}
              />
            </div>
            <p className="mt-2 text-xs text-slate-500">
              {completedSteps} of {formSteps.length} sections ready
            </p>
          </div>
        </div>
        <div className="mt-5 grid grid-cols-2 gap-2 sm:grid-cols-4 lg:grid-cols-8">
          {formSteps.map((step) => (
            <div
              key={step.label}
              className={cn(
                "flex items-center gap-2 rounded-xl border px-3 py-2 text-xs font-semibold transition-colors",
                step.complete
                  ? "border-emerald-200 bg-emerald-50 text-emerald-700"
                  : "border-slate-200 bg-white/70 text-slate-500",
              )}
            >
              {step.complete ? (
                <CheckCircle2 className="h-4 w-4" />
              ) : (
                <Circle className="h-4 w-4" />
              )}
              {step.label}
            </div>
          ))}
        </div>
      </Card>

      {studentOwned && (
        <AdmissionStepNavigation
          activeStep={activeStep}
          informationComplete={informationValidated}
        />
      )}

      <AdmissionDocumentsStep active={!studentOwned || activeStep === "documents"}>
        <Section
          title="Passport-size photo"
          description="Use a recent, clear, front-facing photograph."
          complete={formSteps[0].complete}
        >
          <div className="flex flex-col gap-5 sm:flex-row sm:items-center">
            <div
              className={cn(
                "grid h-44 w-36 shrink-0 place-items-center overflow-hidden rounded-2xl border-2 bg-slate-50 shadow-sm",
                preview ? "border-emerald-300" : "border-dashed border-slate-300",
              )}
            >
              {preview ? (
                <img src={preview} alt="Student passport" className="h-full w-full object-cover" />
              ) : (
                <div className="px-3 text-center text-slate-400">
                  <ImagePlus className="mx-auto h-8 w-8" />
                  <span className="mt-2 block text-xs">No photo selected</span>
                </div>
              )}
            </div>
            <div className="flex-1">
              <div
                className={cn(
                  "rounded-2xl border p-4 transition-colors",
                  photoTransfer?.stage === "error" || photoTransfer?.stage === "cancelled"
                    ? "border-rose-200 bg-rose-50"
                    : photo
                      ? "border-blue-200 bg-blue-50"
                      : photoUploaded
                        ? "border-emerald-200 bg-emerald-50"
                        : "border-dashed border-slate-300 bg-slate-50",
                )}
              >
                <div className="flex items-start gap-3">
                  {photoUploaded ? (
                    <CheckCircle2 className="mt-0.5 h-5 w-5 shrink-0 text-emerald-600" />
                  ) : (
                    <UploadCloud className="mt-0.5 h-5 w-5 shrink-0 text-slate-400" />
                  )}
                  <div className="min-w-0 flex-1">
                    <p className="font-semibold text-slate-800">
                      {photoTransfer?.stage === "uploading"
                        ? "Uploading photo…"
                        : photoTransfer?.stage === "error" || photoTransfer?.stage === "cancelled"
                          ? "Photo upload needs attention"
                          : photoUploaded
                            ? "Photo uploaded"
                            : photo
                              ? "New photo ready to upload"
                              : "Choose a passport photo"}
                    </p>
                    {photoTransfer && (
                      <p
                        role={photoTransfer.stage === "error" ? "alert" : "status"}
                        className={cn(
                          "mt-2 text-xs font-semibold",
                          photoTransfer.stage === "completed" && "text-emerald-700",
                          (photoTransfer.stage === "error" ||
                            photoTransfer.stage === "cancelled") &&
                            "text-rose-700",
                          photoTransfer.stage === "uploading" && "text-blue-700",
                        )}
                      >
                        {documentTransferLabels[photoTransfer.stage]}
                        {photoTransfer.message ? ` ${photoTransfer.message}` : ""}
                      </p>
                    )}
                    <p className="mt-1 truncate text-xs text-slate-500">
                      {photo?.name ?? "JPEG or PNG, maximum 2 MB"}
                    </p>
                  </div>
                </div>
              </div>
              <input
                id="passport-photo"
                type="file"
                accept=".jpg,.jpeg,.png,image/jpeg,image/png"
                onChange={(event) => {
                  const selected = event.target.files?.[0] ?? null;
                  const validationError = selected ? validateAdmissionPhotoFile(selected) : null;
                  if (validationError) {
                    toast.error(validationError);
                    event.target.value = "";
                    setPhoto(null);
                    return;
                  }
                  if (selected) void uploadPhotoImmediately(selected);
                }}
                disabled={photoTransfer?.stage === "uploading"}
                className="sr-only"
              />
              <div className="mt-3 flex flex-wrap gap-2">
                <label
                  htmlFor="passport-photo"
                  aria-disabled={photoTransfer?.stage === "uploading"}
                  className={cn(
                    "inline-flex h-10 items-center gap-2 rounded-xl bg-blue-600 px-4 text-sm font-semibold text-white transition hover:bg-blue-700",
                    photoTransfer?.stage === "uploading"
                      ? "pointer-events-none cursor-not-allowed opacity-60"
                      : "cursor-pointer",
                  )}
                >
                  <UploadCloud className="h-4 w-4" />
                  {photoUploaded || photo ? "Change photo" : "Choose photo"}
                </label>
                {photo &&
                  (!studentOwned ||
                    ["error", "cancelled"].includes(photoTransfer?.stage ?? "")) && (
                    <button
                      type="button"
                      onClick={() => {
                        setPhoto(null);
                        setPhotoTransfer(undefined);
                      }}
                      className="inline-flex h-10 items-center gap-2 rounded-xl border border-slate-200 px-4 text-sm font-semibold text-slate-600 hover:bg-slate-50"
                    >
                      <X className="h-4 w-4" /> Remove selection
                    </button>
                  )}
                {studentOwned &&
                  photo &&
                  ["error", "cancelled"].includes(photoTransfer?.stage ?? "") && (
                    <Button
                      type="button"
                      variant="secondary"
                      onClick={() => void uploadPhotoImmediately(photo)}
                    >
                      Retry upload
                    </Button>
                  )}
                {studentOwned && photoTransfer?.stage === "uploading" && (
                  <Button
                    type="button"
                    variant="danger"
                    onClick={() => photoUploadController.current?.abort()}
                  >
                    Cancel upload
                  </Button>
                )}
              </div>
            </div>
          </div>
        </Section>
      </AdmissionDocumentsStep>

      <AdmissionInformationStep active={!studentOwned || activeStep === "information"}>
        <AdmissionValidationSummary errors={fieldErrors} onSelect={focusInformationField} />
        <Section
          title="Department and course year"
          description="Confirm your department and select the year you are applying for."
          complete={formSteps[1].complete}
        >
          <Grid>
            <Input
              label="Selected department"
              value={admission.departmentName}
              readOnly
              className="bg-slate-100"
            />
            <Select
              name="courseYearId"
              label="Course year"
              required
              disabled={courseYearsLoading || courseYears.length === 0}
              value={values.courseYearId || ""}
              error={fieldErrors.courseYearId}
              onChange={(event) => set("courseYearId", Number(event.target.value))}
              options={[
                {
                  label: courseYearsLoading ? "Loading years..." : "Select FY / SY / TY",
                  value: "",
                },
                ...courseYears.map((year) => ({
                  label: `${yearLabels[year.yearName]} — ${year.displayName} (${year.academicYear})`,
                  value: year.id,
                })),
              ]}
            />
          </Grid>
          {!courseYearsLoading && courseYears.length === 0 && (
            <p className="mt-3 text-sm text-amber-700">
              No active FY, SY, or TY has been created for this department. Ask the Principal or HOD
              to create one.
            </p>
          )}
        </Section>

        <Section
          title="Applicant details"
          description="Enter personal details exactly as they appear on official documents."
          complete={formSteps[2].complete}
        >
          <Grid>
            <Input
              name="fullName"
              label="Full name"
              required
              value={values.fullName}
              error={fieldErrors.fullName}
              onChange={(e) => set("fullName", e.target.value)}
            />
            <Input
              name="phone"
              label="Applicant mobile"
              required
              value={values.phone}
              error={fieldErrors.phone}
              onChange={(e) => set("phone", e.target.value)}
            />
            <Input
              name="email"
              label="Applicant email"
              type="email"
              required
              readOnly={studentOwned}
              className={studentOwned ? "bg-slate-100" : undefined}
              value={values.email}
              error={fieldErrors.email}
              onChange={(e) => set("email", e.target.value)}
            />
            <Input
              name="dateOfBirth"
              label="Date of birth"
              type="date"
              required
              value={values.dateOfBirth}
              error={fieldErrors.dateOfBirth}
              onChange={(e) => set("dateOfBirth", e.target.value)}
            />
            <Select
              name="gender"
              label="Gender"
              value={values.gender}
              error={fieldErrors.gender}
              onChange={(e) => set("gender", e.target.value)}
              options={[
                { label: "Select gender", value: "" },
                { label: "Male", value: "MALE" },
                { label: "Female", value: "FEMALE" },
              ]}
            />
            <Input
              name="placeOfBirth"
              label="Place of birth"
              required
              value={values.placeOfBirth}
              error={fieldErrors.placeOfBirth}
              onChange={(e) => set("placeOfBirth", e.target.value)}
            />
            <Select
              name="maritalStatus"
              label="Marital status"
              value={values.maritalStatus}
              error={fieldErrors.maritalStatus}
              onChange={(e) => set("maritalStatus", e.target.value)}
              options={[
                { label: "Unmarried", value: "UNMARRIED" },
                { label: "Married", value: "MARRIED" },
                { label: "Other", value: "OTHER" },
              ]}
            />
            <Input
              id="aadhaar-number"
              name="aadhaarNumber"
              label="Aadhaar card number"
              required
              inputMode="numeric"
              maxLength={12}
              value={values.aadhaarNumber}
              onChange={(e) => set("aadhaarNumber", e.target.value.replace(/\D/g, ""))}
              error={
                fieldErrors.aadhaarNumber ||
                (values.aadhaarNumber && !aadhaarValid
                  ? `${values.aadhaarNumber.length}/12 digits entered`
                  : undefined)
              }
            />
            <Input
              name="apaarId"
              label="APAAR ID"
              value={values.apaarId ?? ""}
              error={fieldErrors.apaarId}
              onChange={(e) => set("apaarId", e.target.value)}
            />
            <Input
              name="nationality"
              label="Nationality"
              required
              value={values.nationality}
              error={fieldErrors.nationality}
              onChange={(e) => set("nationality", e.target.value)}
            />
            <Input
              name="religion"
              label="Religion"
              required
              value={values.religion}
              error={fieldErrors.religion}
              onChange={(e) => set("religion", e.target.value)}
            />
            <Input
              name="caste"
              label="Caste"
              required
              value={values.caste}
              error={fieldErrors.caste}
              onChange={(e) => set("caste", e.target.value)}
            />
            <Select
              id="student-category"
              name="studentCategory"
              label="Student category"
              value={values.studentCategory}
              error={fieldErrors.studentCategory}
              onChange={(e) => {
                set("studentCategory", e.target.value);
                if (e.target.value !== "OTHER") set("customCategoryName", "");
              }}
              options={[
                {
                  label: categoryOptions.length
                    ? "Select category"
                    : "No active fee category configured",
                  value: "",
                },
                ...categoryOptions
                  .filter(
                    (option, index, all) =>
                      all.findIndex((candidate) => candidate.category === option.category) ===
                      index,
                  )
                  .map((option) => ({ label: option.category, value: option.category })),
              ]}
            />
            {values.studentCategory === "OTHER" && (
              <div id="other-category" className="space-y-1.5">
                <Select
                  id="custom-category"
                  name="customCategoryName"
                  label="Other category"
                  value={values.customCategoryName ?? ""}
                  error={fieldErrors.customCategoryName}
                  onChange={(e) => set("customCategoryName", e.target.value)}
                  options={[
                    { label: "Select category", value: "" },
                    ...categoryOptions
                      .filter((option) => option.category === "OTHER" && option.customCategoryName)
                      .filter(
                        (option, index, all) =>
                          all.findIndex(
                            (candidate) =>
                              candidate.customCategoryName?.toUpperCase() ===
                              option.customCategoryName?.toUpperCase(),
                          ) === index,
                      )
                      .map((option) => ({
                        label: option.customCategoryName!,
                        value: option.customCategoryName!,
                      })),
                  ]}
                />
                {!categoryOptions.some(
                  (option) => option.category === "OTHER" && option.customCategoryName,
                ) && (
                  <p className="text-xs text-amber-700">
                    No Other category is available for this department.
                  </p>
                )}
              </div>
            )}
          </Grid>
        </Section>

        <Section
          title="Father / guardian details"
          description="Provide the primary guardian contact for admission communication."
          complete={formSteps[3].complete}
        >
          <Grid>
            <Input
              name="parentName"
              label="Father / guardian name"
              required
              value={values.parentName}
              error={fieldErrors.parentName}
              onChange={(e) => set("parentName", e.target.value)}
            />
            <Input
              name="parentPhone"
              label="Mobile number"
              required
              value={values.parentPhone}
              error={fieldErrors.parentPhone}
              onChange={(e) => set("parentPhone", e.target.value)}
            />
            <Input
              name="parentEmail"
              label="Email"
              type="email"
              value={values.parentEmail ?? ""}
              error={fieldErrors.parentEmail}
              onChange={(e) => set("parentEmail", e.target.value)}
            />
          </Grid>
        </Section>

        <Section title="Permanent address" complete={formSteps[4].complete}>
          <Grid>
            <Textarea
              name="addressLine1"
              label="Address"
              required
              value={values.addressLine1}
              error={fieldErrors.addressLine1}
              onChange={(e) => set("addressLine1", e.target.value)}
            />
            <Textarea
              name="addressLine2"
              label="Address line 2"
              value={values.addressLine2 ?? ""}
              error={fieldErrors.addressLine2}
              onChange={(e) => set("addressLine2", e.target.value)}
            />
            <Input
              name="city"
              label="City"
              required
              value={values.city}
              error={fieldErrors.city}
              onChange={(e) => set("city", e.target.value)}
            />
            <Input
              id="permanent-pin"
              name="pincode"
              label="PIN code"
              required
              inputMode="numeric"
              maxLength={6}
              value={values.pincode}
              onChange={(e) => set("pincode", e.target.value.replace(/\D/g, ""))}
              error={
                fieldErrors.pincode ||
                (values.pincode && !permanentPinValid
                  ? `${values.pincode.length}/6 digits entered`
                  : undefined)
              }
            />
            <Input
              name="state"
              label="State"
              required
              value={values.state}
              error={fieldErrors.state}
              onChange={(e) => set("state", e.target.value)}
            />
          </Grid>
        </Section>

        <Section
          title="Correspondence address"
          description="This address will be used for official correspondence."
          complete={formSteps[5].complete}
        >
          <label className="mb-4 flex cursor-pointer items-center gap-3 text-sm font-semibold text-slate-700">
            <input
              type="checkbox"
              checked={sameAddress}
              onChange={(event) => copyPermanentAddress(event.target.checked)}
              className="h-4 w-4 rounded border-slate-300 text-brand-600"
            />
            Correspondence address is the same as permanent address
          </label>
          <Grid>
            <Textarea
              name="correspondenceAddress"
              label="Address"
              required
              value={values.correspondenceAddress}
              error={fieldErrors.correspondenceAddress}
              onChange={(e) => set("correspondenceAddress", e.target.value)}
            />
            <Input
              name="correspondenceCity"
              label="City"
              required
              value={values.correspondenceCity}
              error={fieldErrors.correspondenceCity}
              onChange={(e) => set("correspondenceCity", e.target.value)}
            />
            <Input
              id="correspondence-pin"
              name="correspondencePincode"
              label="PIN code"
              required
              inputMode="numeric"
              maxLength={6}
              value={values.correspondencePincode}
              onChange={(e) => set("correspondencePincode", e.target.value.replace(/\D/g, ""))}
              error={
                fieldErrors.correspondencePincode ||
                (values.correspondencePincode && !correspondencePinValid
                  ? `${values.correspondencePincode.length}/6 digits entered`
                  : undefined)
              }
            />
            <Input
              name="correspondenceState"
              label="State"
              required
              value={values.correspondenceState}
              error={fieldErrors.correspondenceState}
              onChange={(e) => set("correspondenceState", e.target.value)}
            />
          </Grid>
        </Section>

        <Section
          title="Academic record"
          description="Add the qualifications that apply to you; percentage is calculated automatically."
          complete={formSteps[6].complete}
        >
          <div className="responsive-table">
            <table>
              <thead>
                <tr className="border-b text-left text-slate-500">
                  <th className="p-2">Qualification</th>
                  <th>School / College / Institute</th>
                  <th>Board / University</th>
                  <th>Year of passing</th>
                  <th>Total Marks</th>
                  <th>Obtained Marks</th>
                  <th>Result type</th>
                  <th>Percentage / CGPA</th>
                </tr>
              </thead>
              <tbody>
                {values.academicRecords.map((record, index) => (
                  <tr className="border-b" key={record.qualification}>
                    <td className="p-2 font-semibold">{record.qualification}</td>
                    <td className="p-2">
                      <input
                        name={`academicRecords[${index}].instituteName`}
                        aria-invalid={Boolean(
                          fieldErrors[`academicRecords[${index}].instituteName`],
                        )}
                        className="h-10 w-full rounded-lg border px-2"
                        value={record.instituteName ?? ""}
                        onChange={(e) => updateRecord(index, "instituteName", e.target.value)}
                      />
                      {fieldErrors[`academicRecords[${index}].instituteName`] && (
                        <p className="mt-1 text-xs text-rose-600">
                          {fieldErrors[`academicRecords[${index}].instituteName`]}
                        </p>
                      )}
                    </td>
                    <td className="p-2">
                      <input
                        name={`academicRecords[${index}].boardUniversity`}
                        aria-invalid={Boolean(
                          fieldErrors[`academicRecords[${index}].boardUniversity`],
                        )}
                        className="h-10 w-full rounded-lg border px-2"
                        value={record.boardUniversity ?? ""}
                        onChange={(e) => updateRecord(index, "boardUniversity", e.target.value)}
                      />
                      {fieldErrors[`academicRecords[${index}].boardUniversity`] && (
                        <p className="mt-1 text-xs text-rose-600">
                          {fieldErrors[`academicRecords[${index}].boardUniversity`]}
                        </p>
                      )}
                    </td>
                    <td className="p-2">
                      <input
                        name={`academicRecords[${index}].yearOfPassing`}
                        aria-invalid={Boolean(
                          fieldErrors[`academicRecords[${index}].yearOfPassing`],
                        )}
                        className="h-10 w-full rounded-lg border px-2"
                        maxLength={4}
                        value={record.yearOfPassing ?? ""}
                        onChange={(e) =>
                          updateRecord(index, "yearOfPassing", e.target.value.replace(/\D/g, ""))
                        }
                      />
                      {fieldErrors[`academicRecords[${index}].yearOfPassing`] && (
                        <p className="mt-1 text-xs text-rose-600">
                          {fieldErrors[`academicRecords[${index}].yearOfPassing`]}
                        </p>
                      )}
                    </td>
                    <td className="p-2">
                      {record.gradingType === "PERCENTAGE" ? (
                        <>
                          <input
                            name={`academicRecords[${index}].totalMarks`}
                            aria-label={`${record.qualification} total marks`}
                            aria-invalid={Boolean(
                              fieldErrors[`academicRecords[${index}].totalMarks`],
                            )}
                            type="number"
                            min="0.01"
                            step="0.01"
                            className="h-10 w-28 rounded-lg border px-2"
                            value={record.totalMarks ?? ""}
                            onChange={(event) =>
                              updateRecord(
                                index,
                                "totalMarks",
                                event.target.value === "" ? undefined : Number(event.target.value),
                              )
                            }
                          />
                          {fieldErrors[`academicRecords[${index}].totalMarks`] && (
                            <p className="mt-1 text-xs text-rose-600">
                              {fieldErrors[`academicRecords[${index}].totalMarks`]}
                            </p>
                          )}
                        </>
                      ) : (
                        <span className="text-slate-400">—</span>
                      )}
                    </td>
                    <td className="p-2">
                      {record.gradingType === "PERCENTAGE" ? (
                        <>
                          <input
                            name={`academicRecords[${index}].obtainedMarks`}
                            aria-label={`${record.qualification} obtained marks`}
                            aria-invalid={Boolean(
                              fieldErrors[`academicRecords[${index}].obtainedMarks`],
                            )}
                            type="number"
                            min="0"
                            max={record.totalMarks ?? undefined}
                            step="0.01"
                            className="h-10 w-28 rounded-lg border px-2"
                            value={record.obtainedMarks ?? ""}
                            onChange={(event) =>
                              updateRecord(
                                index,
                                "obtainedMarks",
                                event.target.value === "" ? undefined : Number(event.target.value),
                              )
                            }
                          />
                          {fieldErrors[`academicRecords[${index}].obtainedMarks`] && (
                            <p className="mt-1 text-xs text-rose-600">
                              {fieldErrors[`academicRecords[${index}].obtainedMarks`]}
                            </p>
                          )}
                        </>
                      ) : (
                        <span className="text-slate-400">—</span>
                      )}
                    </td>
                    <td className="p-2">
                      <select
                        name={`academicRecords[${index}].gradingType`}
                        aria-label={`${record.qualification} result type`}
                        className="h-10 w-32 rounded-lg border bg-white px-2"
                        value={record.gradingType}
                        onChange={(event) =>
                          updateRecord(
                            index,
                            "gradingType",
                            event.target.value as AcademicRecord["gradingType"],
                          )
                        }
                      >
                        <option value="PERCENTAGE">Percentage</option>
                        <option value="CGPA">CGPA</option>
                      </select>
                    </td>
                    <td className="p-2">
                      <input
                        name={`academicRecords[${index}].${record.gradingType === "CGPA" ? "cgpa" : "marksPercentage"}`}
                        aria-invalid={Boolean(
                          fieldErrors[
                            `academicRecords[${index}].${record.gradingType === "CGPA" ? "cgpa" : "marksPercentage"}`
                          ],
                        )}
                        type="number"
                        min="0"
                        max={record.gradingType === "CGPA" ? 10 : 100}
                        step="0.01"
                        className="h-10 w-28 rounded-lg border px-2"
                        placeholder={record.gradingType === "CGPA" ? "0–10" : "0–100"}
                        value={
                          record.gradingType === "CGPA"
                            ? (record.cgpa ?? "")
                            : (record.marksPercentage ?? "")
                        }
                        readOnly={record.gradingType === "PERCENTAGE"}
                        onChange={(e) =>
                          record.gradingType === "CGPA" &&
                          updateRecord(
                            index,
                            "cgpa",
                            e.target.value === "" ? undefined : Number(e.target.value),
                          )
                        }
                      />
                      {fieldErrors[
                        `academicRecords[${index}].${record.gradingType === "CGPA" ? "cgpa" : "marksPercentage"}`
                      ] && (
                        <p className="mt-1 text-xs text-rose-600">
                          {
                            fieldErrors[
                              `academicRecords[${index}].${record.gradingType === "CGPA" ? "cgpa" : "marksPercentage"}`
                            ]
                          }
                        </p>
                      )}
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        </Section>

        <Section title="Entrance exams and last graduation">
          <div id="entrance-exams" className="mb-6 space-y-3">
            <div className="flex flex-wrap items-center justify-between gap-3">
              <div>
                <p className="text-sm font-semibold text-slate-800">Entrance exam results</p>
                <p className="text-xs text-slate-500">
                  Add every exam attempted by the student (maximum 10).
                </p>
              </div>
              <Button
                type="button"
                variant="secondary"
                onClick={addEntranceExam}
                disabled={values.entranceExams.length >= 10}
              >
                <Plus className="h-4 w-4" /> Add entrance exam
              </Button>
            </div>
            {values.entranceExams.map((exam, index) => (
              <div
                key={index}
                className="grid gap-3 rounded-xl border border-slate-200 bg-slate-50/70 p-4 md:grid-cols-[1fr_1fr_auto] md:items-end"
              >
                <Input
                  name={`entranceExams[${index}].examName`}
                  label={`Entrance exam name ${index + 1}`}
                  maxLength={120}
                  placeholder="e.g. MH-CET, JEE, NEET"
                  value={exam.examName}
                  error={fieldErrors[`entranceExams[${index}].examName`]}
                  onChange={(event) => updateEntranceExam(index, "examName", event.target.value)}
                />
                <Input
                  name={`entranceExams[${index}].result`}
                  label="Result / score"
                  maxLength={100}
                  placeholder="e.g. 92.5 percentile, Rank 120"
                  value={exam.result}
                  error={fieldErrors[`entranceExams[${index}].result`]}
                  onChange={(event) => updateEntranceExam(index, "result", event.target.value)}
                />
                <Button
                  type="button"
                  variant="danger"
                  className="px-3"
                  onClick={() => removeEntranceExam(index)}
                  aria-label={`Remove entrance exam ${index + 1}`}
                >
                  <Trash2 className="h-4 w-4" /> <span className="md:hidden">Remove</span>
                </Button>
              </div>
            ))}
          </div>
          <Grid>
            <Input
              name="lastGraduationCollegeName"
              label="Last graduation college name"
              value={values.lastGraduationCollegeName ?? ""}
              error={fieldErrors.lastGraduationCollegeName}
              onChange={(e) => set("lastGraduationCollegeName", e.target.value)}
            />
            <Textarea
              name="lastGraduationCollegeAddress"
              label="Last graduation college address"
              value={values.lastGraduationCollegeAddress ?? ""}
              error={fieldErrors.lastGraduationCollegeAddress}
              onChange={(e) => set("lastGraduationCollegeAddress", e.target.value)}
            />
          </Grid>
        </Section>
      </AdmissionInformationStep>

      <AdmissionDocumentsStep active={!studentOwned || activeStep === "documents"}>
        {studentOwned && (!photoUploaded || missingRequiredDocuments.length > 0) && (
          <Card className="border-amber-300 bg-amber-50 p-4" role="status">
            <p className="font-bold text-amber-900">Required items still need to be uploaded</p>
            <p className="mt-1 text-sm text-amber-800">
              Upload only the items listed below before submitting your admission form.
            </p>
            <ul className="mt-3 list-disc space-y-1 pl-5 text-sm font-semibold text-amber-900">
              {!photoUploaded && <li>Passport-size photo</li>}
              {missingRequiredDocuments.map((item) => (
                <li key={item.type}>{item.label}</li>
              ))}
            </ul>
          </Card>
        )}
        <div id="required-documents">
          <Section
            title="Admission documents"
            description="Files turn blue when selected and green after they have been uploaded."
            complete={formSteps[7].complete}
          >
            <p className="mb-4 text-sm text-slate-500">
              Upload PDF, JPEG, or PNG files up to 2 MB each. Required documents must be uploaded
              before submission.
            </p>
            <div className="grid gap-4 md:grid-cols-2">
              {documentDefinitions.map((item) => (
                <DocumentUpload
                  key={item.type}
                  label={item.label}
                  required={item.required}
                  available={uploadedDocumentTypes.has(item.type)}
                  file={
                    studentOwned
                      ? (studentDocumentTransfers[item.type]?.file ?? null)
                      : (documents[item.type] ?? null)
                  }
                  transfer={
                    studentOwned
                      ? studentDocumentTransfers[item.type]
                      : documentTransfers[item.type]
                  }
                  disabled={saving}
                  onChange={(file) => {
                    const validationError = file ? validateAdmissionDocumentFile(file) : null;
                    if (validationError) {
                      toast.error(validationError);
                      return;
                    }
                    if (studentOwned) {
                      if (file) enqueueStudentDocument(item.type, file);
                      else clearStudentDocument(item.type);
                      return;
                    }
                    setDocuments((current) => {
                      const next = { ...current };
                      if (file) next[item.type] = file;
                      else delete next[item.type];
                      return next;
                    });
                    setDocumentTransfers((current) => {
                      const next = { ...current };
                      delete next[item.type];
                      return next;
                    });
                  }}
                  onRetry={studentOwned ? () => retryStudentDocument(item.type) : undefined}
                  onCancel={studentOwned ? () => cancelStudentDocument(item.type) : undefined}
                />
              ))}
            </div>
          </Section>
        </div>
      </AdmissionDocumentsStep>

      <Card className="sticky bottom-3 z-20 border-slate-200 bg-white/95 p-4 shadow-xl backdrop-blur sm:p-5">
        <div className="flex flex-col gap-4 sm:flex-row sm:items-center sm:justify-between">
          <div className="flex items-center gap-3">
            <div
              className={cn(
                "grid h-10 w-10 shrink-0 place-items-center rounded-full text-xs font-bold",
                completionPercentage === 100
                  ? "bg-emerald-100 text-emerald-700"
                  : "bg-blue-100 text-blue-700",
              )}
            >
              {completionPercentage === 100 ? (
                <Check className="h-5 w-5" />
              ) : (
                `${completionPercentage}%`
              )}
            </div>
            <div>
              <p className="text-sm font-bold text-slate-900">
                {completionPercentage === 100
                  ? "Your form is ready"
                  : "Complete the remaining sections"}
              </p>
              <p className="text-xs text-slate-500">
                {studentOwned
                  ? activeStep === "information"
                    ? "Your information is checked before the document step opens."
                    : "Each selected document uploads immediately and can be retried separately."
                  : "Selected files are uploaded when you submit this form."}
              </p>
            </div>
          </div>
          <div className="flex w-full gap-2 sm:w-auto">
            {studentOwned && activeStep === "documents" && (
              <Button
                type="button"
                variant="secondary"
                className="flex-1 sm:flex-none"
                onClick={() => {
                  setActiveStep("information");
                  window.scrollTo({ top: 0, behavior: "smooth" });
                }}
              >
                Back to information
              </Button>
            )}
            {canCancelUploads && (
              <Button
                type="button"
                variant="danger"
                className="flex-1 sm:flex-none"
                onClick={() => uploadAbortController.current?.abort()}
              >
                Cancel upload
              </Button>
            )}
            <Button
              type="submit"
              loading={saving || validatingInformation}
              className="flex-1 sm:flex-none"
            >
              {studentOwned
                ? activeStep === "information"
                  ? validatingInformation
                    ? "Checking information..."
                    : "Next: documents"
                  : admission.status === "STUDENT_SECTION_REJECTED" ||
                      admission.status === "PRINCIPAL_REJECTED"
                    ? "Resubmit admission form"
                    : "Submit admission form"
                : "Save detailed admission form"}
            </Button>
          </div>
        </div>
      </Card>
    </form>
  );
}

export function DetailedAdmissionView({
  admission,
  documentRequirements = [],
  principal = false,
  studentOwned = false,
}: {
  admission: StudentSectionAdmissionResponse;
  documentRequirements?: AdmissionDocumentRequirement[];
  principal?: boolean;
  studentOwned?: boolean;
}) {
  const [photoUrl, setPhotoUrl] = useState<string | null>(null);
  const [openingDocument, setOpeningDocument] = useState<AdmissionDocumentType | null>(null);
  const [documentPreview, setDocumentPreview] = useState<{
    url: string;
    contentType: string;
    title: string;
  } | null>(null);
  const documentNames = useMemo(
    () => new Map(documentRequirements.map((item) => [item.documentKey, item.documentName])),
    [documentRequirements],
  );
  const documentLabel = (type: AdmissionDocumentType) =>
    documentNames.get(type) ??
    defaultDocumentDefinitions.find((item) => item.type === type)?.label ??
    type;
  useEffect(
    () => () => {
      if (documentPreview && documentPreview.url !== photoUrl) {
        URL.revokeObjectURL(documentPreview.url);
      }
    },
    [documentPreview, photoUrl],
  );
  useEffect(() => {
    if (!admission.photoAvailable) return;
    let url = "";
    const photoRequest = studentOwned
      ? api.getMyAdmissionPhoto()
      : api.getAdmissionPhoto(admission.id, principal);
    photoRequest
      .then((value) => {
        url = value;
        setPhotoUrl(value);
      })
      .catch(() => setPhotoUrl(null));
    return () => {
      if (url) URL.revokeObjectURL(url);
    };
  }, [admission.id, admission.photoAvailable, principal, studentOwned]);
  const rows = useMemo(
    () => [
      ["Place of birth", admission.placeOfBirth],
      ["Marital status", admission.maritalStatus],
      ["Aadhaar", admission.aadhaarNumber],
      ["APAAR ID", admission.apaarId],
      ["Nationality", admission.nationality],
      ["Religion", admission.religion],
      ["Caste", admission.caste],
      ["Course year", admission.courseYearDisplayName],
      [
        "Correspondence address",
        [
          admission.correspondenceAddress,
          admission.correspondenceCity,
          admission.correspondenceState,
          admission.correspondencePincode,
        ]
          .filter(Boolean)
          .join(", "),
      ],
      [
        "Entrance exams",
        admission.entranceExams?.map((exam) => `${exam.examName}: ${exam.result}`).join("; "),
      ],
      ["Last graduation college", admission.lastGraduationCollegeName],
      ["Last graduation address", admission.lastGraduationCollegeAddress],
    ],
    [admission],
  );
  const openDocument = async (type: AdmissionDocumentType) => {
    setOpeningDocument(type);
    try {
      const url = studentOwned
        ? await api.getMyAdmissionDocument(type)
        : await api.getAdmissionDocument(admission.id, type, { principal });
      const blob = await fetch(url).then((response) => response.blob());
      URL.revokeObjectURL(url);
      setDocumentPreview({
        url: URL.createObjectURL(blob),
        contentType: blob.type,
        title: defaultDocumentDefinitions.find((item) => item.type === type)?.label ?? type,
      });
    } catch (error) {
      toast.error(handleApiError(error).message);
    } finally {
      setOpeningDocument(null);
    }
  };
  return (
    <div className="space-y-5">
      <Section title="Complete admission details">
        <div className="grid gap-6 lg:grid-cols-[140px_1fr]">
          <div className="h-44 overflow-hidden rounded-xl border bg-slate-50">
            {photoUrl ? (
              <button
                type="button"
                className="relative h-full w-full cursor-zoom-in"
                onClick={() =>
                  setDocumentPreview({
                    url: photoUrl,
                    contentType: "image/jpeg",
                    title: `Student photograph — ${admission.fullName}`,
                  })
                }
                aria-label="Inspect student photograph"
              >
                <img src={photoUrl} alt="Student passport" className="h-full w-full object-cover" />
                <span className="absolute inset-x-0 bottom-0 bg-slate-950/75 px-2 py-1 text-xs font-semibold text-white">
                  Click to inspect
                </span>
              </button>
            ) : (
              <div className="grid h-full place-items-center text-xs text-slate-400">No photo</div>
            )}
          </div>
          <dl className="grid gap-4 sm:grid-cols-2">
            {rows.map(([label, value]) => (
              <div key={String(label)}>
                <dt className="text-xs font-semibold uppercase text-slate-400">{label}</dt>
                <dd className="mt-1 text-sm">{value || "-"}</dd>
              </div>
            ))}
          </dl>
        </div>
      </Section>
      <Section title="Academic record">
        <div className="responsive-table">
          <table>
            <thead>
              <tr className="border-b text-left text-slate-500">
                <th className="p-2">Qualification</th>
                <th>Institute</th>
                <th>Board / University</th>
                <th>Year</th>
                <th>Total Marks</th>
                <th>Obtained Marks</th>
                <th>Result type</th>
                <th>Result</th>
              </tr>
            </thead>
            <tbody>
              {admission.academicRecords?.map((record) => (
                <tr className="border-b" key={record.qualification}>
                  <td className="p-2 font-semibold">{record.qualification}</td>
                  <td>{record.instituteName || "-"}</td>
                  <td>{record.boardUniversity || "-"}</td>
                  <td>{record.yearOfPassing || "-"}</td>
                  <td>{record.gradingType === "CGPA" ? "-" : (record.totalMarks ?? "-")}</td>
                  <td>{record.gradingType === "CGPA" ? "-" : (record.obtainedMarks ?? "-")}</td>
                  <td>{record.gradingType === "CGPA" ? "CGPA" : "Percentage"}</td>
                  <td>
                    {record.gradingType === "CGPA"
                      ? record.cgpa == null
                        ? "-"
                        : `${record.cgpa} CGPA`
                      : record.marksPercentage == null
                        ? "-"
                        : `${record.marksPercentage}%`}
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      </Section>
      <Section title="Uploaded documents">
        <div className="flex flex-wrap gap-2">
          {admission.uploadedDocuments?.length ? (
            admission.uploadedDocuments.map((type) => (
              <button
                key={type}
                type="button"
                disabled={openingDocument === type}
                onClick={() => void openDocument(type)}
                className="rounded-full bg-emerald-50 px-3 py-1 text-xs font-semibold text-emerald-700 hover:bg-emerald-100 disabled:opacity-60"
              >
                {openingDocument === type ? "Opening..." : documentLabel(type)}
              </button>
            ))
          ) : (
            <p className="text-sm text-slate-500">No documents uploaded.</p>
          )}
        </div>
      </Section>
      {documentPreview && (
        <DocumentViewer
          open
          url={documentPreview.url}
          contentType={documentPreview.contentType}
          title={documentPreview.title}
          filename={`${documentPreview.title.toLowerCase().replace(/[^a-z0-9]+/g, "-")}`}
          onClose={() => setDocumentPreview(null)}
        />
      )}
    </div>
  );
}

function Section({
  title,
  description,
  complete,
  children,
}: {
  title: string;
  description?: string;
  complete?: boolean;
  children: React.ReactNode;
}) {
  return (
    <Card
      className={cn(
        "overflow-hidden p-5 transition-colors sm:p-6",
        complete && "border-emerald-200",
      )}
    >
      <div className="mb-5 flex items-start justify-between gap-4 border-b border-slate-100 pb-4">
        <div>
          <h2 className="text-lg font-bold text-slate-900">{title}</h2>
          {description && <p className="mt-1 text-sm text-slate-500">{description}</p>}
        </div>
        {complete !== undefined && (
          <span
            className={cn(
              "inline-flex shrink-0 items-center gap-1.5 rounded-full px-2.5 py-1 text-xs font-bold",
              complete ? "bg-emerald-50 text-emerald-700" : "bg-amber-50 text-amber-700",
            )}
          >
            {complete ? (
              <CheckCircle2 className="h-3.5 w-3.5" />
            ) : (
              <Circle className="h-3.5 w-3.5" />
            )}
            {complete ? "Complete" : "In progress"}
          </span>
        )}
      </div>
      {children}
    </Card>
  );
}
function Grid({ children }: { children: React.ReactNode }) {
  return <div className="grid gap-4 md:grid-cols-2">{children}</div>;
}

function DocumentUpload({
  label,
  required,
  available,
  file,
  transfer,
  disabled,
  onChange,
  onRetry,
  onCancel,
}: {
  label: string;
  required: boolean;
  available: boolean;
  file: File | null;
  transfer?: DocumentTransferState | AdmissionUploadQueueState;
  disabled?: boolean;
  onChange: (file: File | null) => void;
  onRetry?: () => void;
  onCancel?: () => void;
}) {
  const inputId = useId();
  const state =
    available || transfer?.stage === "completed" ? "uploaded" : file ? "selected" : "empty";
  const transferActive = Boolean(
    transfer && !["completed", "error", "cancelled"].includes(transfer.stage),
  );
  return (
    <div
      className={cn(
        "rounded-2xl border-2 p-4 transition-all",
        state === "selected" && "border-blue-300 bg-blue-50 shadow-sm",
        state === "uploaded" && "border-emerald-200 bg-emerald-50/70",
        state === "empty" && required && "border-dashed border-rose-200 bg-rose-50/40",
        state === "empty" && !required && "border-dashed border-slate-200 bg-slate-50/70",
        (transfer?.stage === "error" || transfer?.stage === "cancelled") &&
          "border-rose-300 bg-rose-50",
      )}
    >
      <div className="flex items-start gap-3">
        <div
          className={cn(
            "grid h-10 w-10 shrink-0 place-items-center rounded-xl",
            state === "selected" && "bg-blue-100 text-blue-700",
            state === "uploaded" && "bg-emerald-100 text-emerald-700",
            state === "empty" && "bg-white text-slate-400 shadow-sm",
          )}
        >
          {state === "uploaded" ? (
            <FileCheck2 className="h-5 w-5" />
          ) : (
            <UploadCloud className="h-5 w-5" />
          )}
        </div>
        <div className="min-w-0 flex-1">
          <div className="flex flex-wrap items-center gap-2">
            <p className="font-semibold text-slate-800">{label}</p>
            <span
              className={cn(
                "rounded-full px-2 py-0.5 text-[10px] font-bold uppercase tracking-wide",
                required ? "bg-rose-100 text-rose-700" : "bg-slate-200 text-slate-600",
              )}
            >
              {required ? "Required" : "Optional"}
            </span>
          </div>
          <p
            className={cn(
              "mt-1 truncate text-xs font-medium",
              state === "selected" && "text-blue-700",
              state === "uploaded" && "text-emerald-700",
              state === "empty" && "text-slate-500",
            )}
          >
            {file?.name ?? (available ? "Uploaded successfully" : "No file attached yet")}
          </p>
          {file && <p className="mt-1 text-[11px] text-slate-500">{formatFileSize(file.size)}</p>}
          {transfer && (
            <p
              role={
                transfer.stage === "error" || transfer.stage === "cancelled" ? "alert" : "status"
              }
              className={cn(
                "mt-2 text-xs font-semibold",
                transfer.stage === "completed" && "text-emerald-700",
                (transfer.stage === "error" || transfer.stage === "cancelled") && "text-rose-700",
                !["completed", "error", "cancelled"].includes(transfer.stage) && "text-blue-700",
              )}
            >
              {documentTransferLabels[transfer.stage]}
              {transfer.message ? ` ${transfer.message}` : ""}
            </p>
          )}
        </div>
        {state !== "selected" && (
          <span
            className={cn(
              "shrink-0 rounded-full px-2.5 py-1 text-[10px] font-bold uppercase",
              state === "uploaded" && "bg-emerald-600 text-white",
              state === "empty" && "bg-white text-slate-500",
            )}
          >
            {state === "uploaded" ? "Uploaded" : "Pending"}
          </span>
        )}
      </div>
      <input
        id={inputId}
        type="file"
        accept=".pdf,.jpg,.jpeg,.png,application/pdf,image/jpeg,image/png"
        disabled={disabled}
        className="sr-only"
        onChange={(event) => {
          onChange(event.target.files?.[0] ?? null);
          event.target.value = "";
        }}
      />
      <div className="mt-4 flex flex-wrap gap-2 border-t border-current/10 pt-3">
        <label
          htmlFor={inputId}
          aria-disabled={disabled}
          className={cn(
            "inline-flex h-9 items-center gap-2 rounded-lg bg-white px-3 text-xs font-bold text-slate-700 shadow-sm ring-1 ring-slate-200 hover:bg-slate-50",
            disabled ? "pointer-events-none cursor-not-allowed opacity-60" : "cursor-pointer",
          )}
        >
          <UploadCloud className="h-3.5 w-3.5" />
          {available || file ? "Replace file" : "Choose file"}
        </label>
        {file && state === "selected" && !transferActive && (
          <button
            type="button"
            disabled={disabled}
            onClick={() => onChange(null)}
            className="inline-flex h-9 items-center gap-2 rounded-lg px-3 text-xs font-bold text-rose-600 hover:bg-rose-100 disabled:cursor-not-allowed disabled:opacity-60"
          >
            <X className="h-3.5 w-3.5" /> Clear
          </button>
        )}
        {(transfer?.stage === "error" || transfer?.stage === "cancelled") && onRetry && (
          <button
            type="button"
            disabled={disabled}
            onClick={onRetry}
            className="inline-flex h-9 items-center gap-2 rounded-lg bg-blue-600 px-3 text-xs font-bold text-white hover:bg-blue-700 disabled:opacity-60"
          >
            Retry upload
          </button>
        )}
        {transferActive && onCancel && (
          <button
            type="button"
            disabled={disabled}
            onClick={onCancel}
            className="inline-flex h-9 items-center gap-2 rounded-lg px-3 text-xs font-bold text-rose-600 hover:bg-rose-100 disabled:opacity-60"
          >
            Cancel upload
          </button>
        )}
      </div>
    </div>
  );
}

function formatFileSize(bytes: number) {
  if (bytes < 1024 * 1024) return `${Math.max(1, Math.round(bytes / 1024))} KB`;
  return `${(bytes / (1024 * 1024)).toFixed(1)} MB`;
}
