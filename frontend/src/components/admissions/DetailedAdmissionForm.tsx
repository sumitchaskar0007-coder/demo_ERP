import { Check, CheckCircle2, Circle, FileCheck2, ImagePlus, UploadCloud, X } from "lucide-react";
import { useEffect, useId, useMemo, useState } from "react";
import { toast } from "sonner";
import { Button } from "@/components/common/Button";
import { Card } from "@/components/common/Card";
import { Input } from "@/components/common/Input";
import { Select } from "@/components/common/Select";
import { Textarea } from "@/components/common/Textarea";
import * as api from "@/features/admissions/api";
import type {
  AcademicRecord,
  AdmissionCourseYearOption,
  AdmissionDocumentType,
  DetailedAdmissionRequest,
  StudentSectionAdmissionResponse,
} from "@/features/admissions/types";
import { handleApiError } from "@/lib/handleApiError";
import { cn } from "@/lib/utils";

const qualifications: AcademicRecord["qualification"][] = ["10TH", "12TH", "DIPLOMA", "GRADUATION"];

const documentDefinitions: { type: AdmissionDocumentType; label: string; required: boolean }[] = [
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

function initialValues(a: StudentSectionAdmissionResponse): DetailedAdmissionRequest {
  const records = qualifications.map(
    (qualification) =>
      a.academicRecords?.find((record) => record.qualification === qualification) ?? {
        qualification,
        instituteName: "",
        boardUniversity: "",
        yearOfPassing: "",
        totalMarks: undefined,
        obtainedMarks: undefined,
        marksPercentage: undefined,
      },
  );
  return {
    courseYearId: a.courseYearId ?? 0,
    fullName: a.fullName,
    email: a.email,
    phone: a.phone,
    dateOfBirth: a.dateOfBirth,
    gender: a.gender,
    placeOfBirth: a.placeOfBirth ?? "",
    maritalStatus: a.maritalStatus ?? "UNMARRIED",
    aadhaarNumber: a.aadhaarNumber ?? "",
    apaarId: a.apaarId ?? "",
    nationality: a.nationality ?? "Indian",
    religion: a.religion ?? "",
    caste: a.caste ?? "",
    studentCategory: a.studentCategory,
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
    qualifyingEntranceSeatNumber: a.qualifyingEntranceSeatNumber ?? "",
    qualifyingEntranceTotalScore: a.qualifyingEntranceTotalScore ?? undefined,
    lastGraduationCollegeName: a.lastGraduationCollegeName ?? "",
    lastGraduationCollegeAddress: a.lastGraduationCollegeAddress ?? "",
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
  const [photo, setPhoto] = useState<File | null>(null);
  const [documents, setDocuments] = useState<Partial<Record<AdmissionDocumentType, File>>>({});
  const [courseYears, setCourseYears] = useState<AdmissionCourseYearOption[]>([]);
  const [courseYearsLoading, setCourseYearsLoading] = useState(true);
  const [sameAddress, setSameAddress] = useState(false);
  const [saving, setSaving] = useState(false);
  const [preview, setPreview] = useState<string | null>(null);
  const aadhaarValid = /^\d{12}$/.test(values.aadhaarNumber);
  const permanentPinValid = /^\d{6}$/.test(values.pincode);
  const correspondencePinValid = /^\d{6}$/.test(values.correspondencePincode);

  const formSteps = useMemo(() => {
    const has = (...fields: (keyof DetailedAdmissionRequest)[]) =>
      fields.every((field) => String(values[field] ?? "").trim().length > 0);
    const requiredDocumentsReady = documentDefinitions
      .filter((item) => item.required)
      .every(
        (item) => admission.uploadedDocuments?.includes(item.type) || Boolean(documents[item.type]),
      );
    const academicReady = values.academicRecords.some(
      (record) =>
        Boolean(record.instituteName?.trim()) &&
        Boolean(record.boardUniversity?.trim()) &&
        Boolean(record.yearOfPassing?.trim()),
    );
    return [
      { label: "Photo", complete: admission.photoAvailable || Boolean(photo) },
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
    admission.photoAvailable,
    admission.uploadedDocuments,
    correspondencePinValid,
    documents,
    permanentPinValid,
    photo,
    values,
  ]);
  const completedSteps = formSteps.filter((step) => step.complete).length;
  const completionPercentage = Math.round((completedSteps / formSteps.length) * 100);

  useEffect(() => setValues(initialValues(admission)), [admission]);
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

  const set = (name: keyof DetailedAdmissionRequest, value: string | number | undefined) =>
    setValues((current) => ({ ...current, [name]: value }));
  const updateRecord = (
    index: number,
    name: keyof AcademicRecord,
    value: string | number | undefined,
  ) =>
    setValues((current) => ({
      ...current,
      academicRecords: current.academicRecords.map((record, recordIndex) => {
        if (recordIndex !== index) return record;
        const updated = { ...record, [name]: value };
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

  const copyPermanentAddress = (checked: boolean) => {
    setSameAddress(checked);
    if (!checked) return;
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

  const save = async (event: React.FormEvent) => {
    event.preventDefault();
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
    if (!admission.photoAvailable && !photo) {
      toast.error("Passport-size photo is required");
      return;
    }
    if (!values.courseYearId) {
      toast.error("Select FY, SY, or TY for the chosen department");
      return;
    }
    const missing = documentDefinitions.filter(
      (item) =>
        item.required && !admission.uploadedDocuments?.includes(item.type) && !documents[item.type],
    );
    if (missing.length) {
      toast.error(`Upload required documents: ${missing.map((item) => item.label).join(", ")}`);
      return;
    }
    setSaving(true);
    try {
      if (photo) {
        if (studentOwned) await api.uploadMyAdmissionPhoto(photo);
        else await api.uploadAdmissionPhoto(admission.id, photo);
      }
      await Promise.all(
        Object.entries(documents).map(([type, file]) =>
          studentOwned
            ? api.uploadMyAdmissionDocument(type as AdmissionDocumentType, file)
            : api.uploadAdmissionDocument(admission.id, type as AdmissionDocumentType, file),
        ),
      );
      if (studentOwned) await api.submitMyAdmissionDetails(values);
      else await api.updateAdmissionDetails(admission.id, values);
      toast.success(
        studentOwned ? "Admission form submitted for review" : "Detailed admission form saved",
      );
      setPhoto(null);
      setDocuments({});
      await onSaved();
    } catch (error) {
      const apiError = handleApiError(error);
      toast.error(Object.values(apiError.fieldErrors)[0] ?? apiError.message);
    } finally {
      setSaving(false);
    }
  };

  return (
    <form onSubmit={save} className="space-y-5">
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
                photo
                  ? "border-blue-200 bg-blue-50"
                  : admission.photoAvailable
                    ? "border-emerald-200 bg-emerald-50"
                    : "border-dashed border-slate-300 bg-slate-50",
              )}
            >
              <div className="flex items-start gap-3">
                {photo || admission.photoAvailable ? (
                  <CheckCircle2 className="mt-0.5 h-5 w-5 shrink-0 text-emerald-600" />
                ) : (
                  <UploadCloud className="mt-0.5 h-5 w-5 shrink-0 text-slate-400" />
                )}
                <div className="min-w-0 flex-1">
                  <p className="font-semibold text-slate-800">
                    {photo
                      ? "New photo ready to upload"
                      : admission.photoAvailable
                        ? "Photo uploaded"
                        : "Choose a passport photo"}
                  </p>
                  <p className="mt-1 truncate text-xs text-slate-500">
                    {photo?.name ?? "JPEG, PNG or WebP, maximum 2 MB"}
                  </p>
                </div>
              </div>
            </div>
            <input
              id="passport-photo"
              type="file"
              accept="image/jpeg,image/png,image/webp"
              onChange={(event) => setPhoto(event.target.files?.[0] ?? null)}
              className="sr-only"
            />
            <div className="mt-3 flex flex-wrap gap-2">
              <label
                htmlFor="passport-photo"
                className="inline-flex h-10 cursor-pointer items-center gap-2 rounded-xl bg-blue-600 px-4 text-sm font-semibold text-white transition hover:bg-blue-700"
              >
                <UploadCloud className="h-4 w-4" />
                {photo || admission.photoAvailable ? "Change photo" : "Choose photo"}
              </label>
              {photo && (
                <button
                  type="button"
                  onClick={() => setPhoto(null)}
                  className="inline-flex h-10 items-center gap-2 rounded-xl border border-slate-200 px-4 text-sm font-semibold text-slate-600 hover:bg-slate-50"
                >
                  <X className="h-4 w-4" /> Remove selection
                </button>
              )}
            </div>
          </div>
        </div>
      </Section>

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
            label="Course year"
            required
            disabled={courseYearsLoading || courseYears.length === 0}
            value={values.courseYearId || ""}
            onChange={(event) => set("courseYearId", Number(event.target.value))}
            options={[
              { label: courseYearsLoading ? "Loading years..." : "Select FY / SY / TY", value: "" },
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
            label="Full name"
            required
            value={values.fullName}
            onChange={(e) => set("fullName", e.target.value)}
          />
          <Input
            label="Applicant mobile"
            required
            value={values.phone}
            onChange={(e) => set("phone", e.target.value)}
          />
          <Input
            label="Applicant email"
            type="email"
            required
            readOnly={studentOwned}
            className={studentOwned ? "bg-slate-100" : undefined}
            value={values.email}
            onChange={(e) => set("email", e.target.value)}
          />
          <Input
            label="Date of birth"
            type="date"
            required
            value={values.dateOfBirth}
            onChange={(e) => set("dateOfBirth", e.target.value)}
          />
          <Select
            label="Gender"
            value={values.gender}
            onChange={(e) => set("gender", e.target.value)}
            options={[
              { label: "Select gender", value: "" },
              { label: "Male", value: "MALE" },
              { label: "Female", value: "FEMALE" },
              { label: "Other", value: "OTHER" },
            ]}
          />
          <Input
            label="Place of birth"
            required
            value={values.placeOfBirth}
            onChange={(e) => set("placeOfBirth", e.target.value)}
          />
          <Select
            label="Marital status"
            value={values.maritalStatus}
            onChange={(e) => set("maritalStatus", e.target.value)}
            options={[
              { label: "Unmarried", value: "UNMARRIED" },
              { label: "Married", value: "MARRIED" },
              { label: "Other", value: "OTHER" },
            ]}
          />
          <Input
            id="aadhaar-number"
            label="Aadhaar card number"
            required
            inputMode="numeric"
            maxLength={12}
            value={values.aadhaarNumber}
            onChange={(e) => set("aadhaarNumber", e.target.value.replace(/\D/g, ""))}
            error={
              values.aadhaarNumber && !aadhaarValid
                ? `${values.aadhaarNumber.length}/12 digits entered`
                : undefined
            }
          />
          <Input
            label="APAAR ID"
            value={values.apaarId ?? ""}
            onChange={(e) => set("apaarId", e.target.value)}
          />
          <Input
            label="Nationality"
            required
            value={values.nationality}
            onChange={(e) => set("nationality", e.target.value)}
          />
          <Input
            label="Religion"
            required
            value={values.religion}
            onChange={(e) => set("religion", e.target.value)}
          />
          <Input
            label="Caste"
            required
            value={values.caste}
            onChange={(e) => set("caste", e.target.value)}
          />
          <Select
            label="Student category"
            value={values.studentCategory}
            onChange={(e) => set("studentCategory", e.target.value)}
            options={["OPEN", "OBC", "SC", "ST", "SBC", "VJNT", "EWS", "OTHER"].map((value) => ({
              label: value,
              value,
            }))}
          />
        </Grid>
      </Section>

      <Section
        title="Father / guardian details"
        description="Provide the primary guardian contact for admission communication."
        complete={formSteps[3].complete}
      >
        <Grid>
          <Input
            label="Father / guardian name"
            required
            value={values.parentName}
            onChange={(e) => set("parentName", e.target.value)}
          />
          <Input
            label="Mobile number"
            required
            value={values.parentPhone}
            onChange={(e) => set("parentPhone", e.target.value)}
          />
          <Input
            label="Email"
            type="email"
            value={values.parentEmail ?? ""}
            onChange={(e) => set("parentEmail", e.target.value)}
          />
        </Grid>
      </Section>

      <Section title="Permanent address" complete={formSteps[4].complete}>
        <Grid>
          <Textarea
            label="Address"
            required
            value={values.addressLine1}
            onChange={(e) => set("addressLine1", e.target.value)}
          />
          <Textarea
            label="Address line 2"
            value={values.addressLine2 ?? ""}
            onChange={(e) => set("addressLine2", e.target.value)}
          />
          <Input
            label="City"
            required
            value={values.city}
            onChange={(e) => set("city", e.target.value)}
          />
          <Input
            id="permanent-pin"
            label="PIN code"
            required
            inputMode="numeric"
            maxLength={6}
            value={values.pincode}
            onChange={(e) => set("pincode", e.target.value.replace(/\D/g, ""))}
            error={
              values.pincode && !permanentPinValid
                ? `${values.pincode.length}/6 digits entered`
                : undefined
            }
          />
          <Input
            label="State"
            required
            value={values.state}
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
            label="Address"
            required
            value={values.correspondenceAddress}
            onChange={(e) => set("correspondenceAddress", e.target.value)}
          />
          <Input
            label="City"
            required
            value={values.correspondenceCity}
            onChange={(e) => set("correspondenceCity", e.target.value)}
          />
          <Input
            id="correspondence-pin"
            label="PIN code"
            required
            inputMode="numeric"
            maxLength={6}
            value={values.correspondencePincode}
            onChange={(e) => set("correspondencePincode", e.target.value.replace(/\D/g, ""))}
            error={
              values.correspondencePincode && !correspondencePinValid
                ? `${values.correspondencePincode.length}/6 digits entered`
                : undefined
            }
          />
          <Input
            label="State"
            required
            value={values.correspondenceState}
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
                <th>Total marks</th>
                <th>Obtained marks</th>
                <th>Percentage</th>
              </tr>
            </thead>
            <tbody>
              {values.academicRecords.map((record, index) => (
                <tr className="border-b" key={record.qualification}>
                  <td className="p-2 font-semibold">{record.qualification}</td>
                  <td className="p-2">
                    <input
                      className="h-10 w-full rounded-lg border px-2"
                      value={record.instituteName ?? ""}
                      onChange={(e) => updateRecord(index, "instituteName", e.target.value)}
                    />
                  </td>
                  <td className="p-2">
                    <input
                      className="h-10 w-full rounded-lg border px-2"
                      value={record.boardUniversity ?? ""}
                      onChange={(e) => updateRecord(index, "boardUniversity", e.target.value)}
                    />
                  </td>
                  <td className="p-2">
                    <input
                      className="h-10 w-full rounded-lg border px-2"
                      maxLength={4}
                      value={record.yearOfPassing ?? ""}
                      onChange={(e) =>
                        updateRecord(index, "yearOfPassing", e.target.value.replace(/\D/g, ""))
                      }
                    />
                  </td>
                  <td className="p-2">
                    <input
                      type="number"
                      min="0.01"
                      step="0.01"
                      className="h-10 w-28 rounded-lg border px-2"
                      value={record.totalMarks ?? ""}
                      onChange={(e) =>
                        updateRecord(
                          index,
                          "totalMarks",
                          e.target.value === "" ? undefined : Number(e.target.value),
                        )
                      }
                    />
                  </td>
                  <td className="p-2">
                    <input
                      type="number"
                      min="0"
                      max={record.totalMarks ?? undefined}
                      step="0.01"
                      className="h-10 w-28 rounded-lg border px-2"
                      value={record.obtainedMarks ?? ""}
                      onChange={(e) =>
                        updateRecord(
                          index,
                          "obtainedMarks",
                          e.target.value === "" ? undefined : Number(e.target.value),
                        )
                      }
                    />
                  </td>
                  <td className="p-2">
                    <input
                      readOnly
                      className="h-10 w-28 rounded-lg border bg-slate-50 px-2 font-semibold"
                      value={record.marksPercentage == null ? "" : `${record.marksPercentage}%`}
                    />
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      </Section>

      <Section title="Entrance test and last graduation">
        <Grid>
          <Input
            label="Seat number of qualifying entrance test"
            value={values.qualifyingEntranceSeatNumber ?? ""}
            onChange={(e) => set("qualifyingEntranceSeatNumber", e.target.value)}
          />
          <Input
            label="Total score in test"
            type="number"
            min="0"
            step="0.01"
            value={values.qualifyingEntranceTotalScore ?? ""}
            onChange={(e) =>
              set(
                "qualifyingEntranceTotalScore",
                e.target.value === "" ? undefined : Number(e.target.value),
              )
            }
          />
          <Input
            label="Last graduation college name"
            value={values.lastGraduationCollegeName ?? ""}
            onChange={(e) => set("lastGraduationCollegeName", e.target.value)}
          />
          <Textarea
            label="Last graduation college address"
            value={values.lastGraduationCollegeAddress ?? ""}
            onChange={(e) => set("lastGraduationCollegeAddress", e.target.value)}
          />
        </Grid>
      </Section>

      <Section
        title="Admission documents"
        description="Files turn blue when selected and green after they have been uploaded."
        complete={formSteps[7].complete}
      >
        <p className="mb-4 text-sm text-slate-500">
          Upload PDF, JPEG, PNG, or WebP files up to 5 MB each. Required documents must be uploaded
          before submission.
        </p>
        <div className="grid gap-4 md:grid-cols-2">
          {documentDefinitions.map((item) => (
            <DocumentUpload
              key={item.type}
              label={item.label}
              required={item.required}
              available={admission.uploadedDocuments?.includes(item.type) ?? false}
              file={documents[item.type] ?? null}
              onChange={(file) => setDocuments((current) => ({ ...current, [item.type]: file }))}
            />
          ))}
        </div>
      </Section>

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
                Selected files are uploaded when you submit this form.
              </p>
            </div>
          </div>
          <Button type="submit" loading={saving} className="w-full sm:w-auto">
            {studentOwned
              ? admission.status === "STUDENT_SECTION_REJECTED" ||
                admission.status === "PRINCIPAL_REJECTED"
                ? "Resubmit admission form"
                : "Submit admission form"
              : "Save detailed admission form"}
          </Button>
        </div>
      </Card>
    </form>
  );
}

export function DetailedAdmissionView({
  admission,
  principal = false,
  studentOwned = false,
}: {
  admission: StudentSectionAdmissionResponse;
  principal?: boolean;
  studentOwned?: boolean;
}) {
  const [photoUrl, setPhotoUrl] = useState<string | null>(null);
  const [openingDocument, setOpeningDocument] = useState<AdmissionDocumentType | null>(null);
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
      ["Entrance seat number", admission.qualifyingEntranceSeatNumber],
      ["Entrance total score", admission.qualifyingEntranceTotalScore],
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
        : await api.getAdmissionDocument(admission.id, type);
      const link = document.createElement("a");
      link.href = url;
      link.target = "_blank";
      link.rel = "noopener noreferrer";
      link.click();
      window.setTimeout(() => URL.revokeObjectURL(url), 60_000);
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
              <img src={photoUrl} alt="Student passport" className="h-full w-full object-cover" />
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
                <th>Total marks</th>
                <th>Obtained marks</th>
                <th>Percentage</th>
              </tr>
            </thead>
            <tbody>
              {admission.academicRecords?.map((record) => (
                <tr className="border-b" key={record.qualification}>
                  <td className="p-2 font-semibold">{record.qualification}</td>
                  <td>{record.instituteName || "-"}</td>
                  <td>{record.boardUniversity || "-"}</td>
                  <td>{record.yearOfPassing || "-"}</td>
                  <td>{record.totalMarks ?? "-"}</td>
                  <td>{record.obtainedMarks ?? "-"}</td>
                  <td>{record.marksPercentage == null ? "-" : `${record.marksPercentage}%`}</td>
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
                {openingDocument === type
                  ? "Opening..."
                  : (documentDefinitions.find((item) => item.type === type)?.label ?? type)}
              </button>
            ))
          ) : (
            <p className="text-sm text-slate-500">No documents uploaded.</p>
          )}
        </div>
      </Section>
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
  onChange,
}: {
  label: string;
  required: boolean;
  available: boolean;
  file: File | null;
  onChange: (file: File | null) => void;
}) {
  const inputId = useId();
  const state = file ? "selected" : available ? "uploaded" : "empty";
  return (
    <div
      className={cn(
        "rounded-2xl border-2 p-4 transition-all",
        state === "selected" && "border-blue-300 bg-blue-50 shadow-sm",
        state === "uploaded" && "border-emerald-200 bg-emerald-50/70",
        state === "empty" && required && "border-dashed border-rose-200 bg-rose-50/40",
        state === "empty" && !required && "border-dashed border-slate-200 bg-slate-50/70",
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
          {file && (
            <p className="mt-1 text-[11px] text-slate-500">
              {formatFileSize(file.size)} · Ready to upload on submit
            </p>
          )}
        </div>
        <span
          className={cn(
            "shrink-0 rounded-full px-2.5 py-1 text-[10px] font-bold uppercase",
            state === "selected" && "bg-blue-600 text-white",
            state === "uploaded" && "bg-emerald-600 text-white",
            state === "empty" && "bg-white text-slate-500",
          )}
        >
          {state === "selected" ? "Ready" : state === "uploaded" ? "Uploaded" : "Pending"}
        </span>
      </div>
      <input
        id={inputId}
        type="file"
        accept="application/pdf,image/jpeg,image/png,image/webp"
        required={required && !available && !file}
        className="sr-only"
        onChange={(event) => onChange(event.target.files?.[0] ?? null)}
      />
      <div className="mt-4 flex flex-wrap gap-2 border-t border-current/10 pt-3">
        <label
          htmlFor={inputId}
          className="inline-flex h-9 cursor-pointer items-center gap-2 rounded-lg bg-white px-3 text-xs font-bold text-slate-700 shadow-sm ring-1 ring-slate-200 hover:bg-slate-50"
        >
          <UploadCloud className="h-3.5 w-3.5" />
          {available || file ? "Replace file" : "Choose file"}
        </label>
        {file && (
          <button
            type="button"
            onClick={() => onChange(null)}
            className="inline-flex h-9 items-center gap-2 rounded-lg px-3 text-xs font-bold text-rose-600 hover:bg-rose-100"
          >
            <X className="h-3.5 w-3.5" /> Clear
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
