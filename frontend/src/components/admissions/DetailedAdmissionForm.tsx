import { useEffect, useMemo, useState } from "react";
import { toast } from "sonner";
import { Button } from "@/components/common/Button";
import { Card } from "@/components/common/Card";
import { Input } from "@/components/common/Input";
import { Select } from "@/components/common/Select";
import { Textarea } from "@/components/common/Textarea";
import * as api from "@/features/admissions/api";
import type {
  AcademicRecord,
  DetailedAdmissionRequest,
  StudentSectionAdmissionResponse,
} from "@/features/admissions/types";
import { handleApiError } from "@/lib/handleApiError";

const qualifications: AcademicRecord["qualification"][] = [
  "10TH",
  "12TH",
  "DIPLOMA",
  "GRADUATION",
];

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
    permanentPhone: a.permanentPhone ?? a.phone,
    permanentEmail: a.permanentEmail ?? a.email,
    correspondenceAddress: a.correspondenceAddress ?? a.addressLine1 ?? "",
    correspondenceCity: a.correspondenceCity ?? a.city ?? "",
    correspondencePincode: a.correspondencePincode ?? a.pincode ?? "",
    correspondenceState: a.correspondenceState ?? a.state ?? "",
    correspondencePhone: a.correspondencePhone ?? "",
    correspondenceMobile: a.correspondenceMobile ?? a.phone,
    correspondenceEmail: a.correspondenceEmail ?? a.email,
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
}: {
  admission: StudentSectionAdmissionResponse;
  onSaved: () => Promise<void>;
}) {
  const [values, setValues] = useState(() => initialValues(admission));
  const [photo, setPhoto] = useState<File | null>(null);
  const [documents, setDocuments] = useState<Partial<Record<api.AdmissionDocumentType, File>>>({});
  const [saving, setSaving] = useState(false);
  const [preview, setPreview] = useState<string | null>(null);

  useEffect(() => setValues(initialValues(admission)), [admission]);
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
    api.getMyAdmissionPhoto().then((value) => {
      url = value;
      if (active) setPreview(value);
    }).catch(() => setPreview(null));
    return () => {
      active = false;
      if (url) URL.revokeObjectURL(url);
    };
  }, [admission.id, admission.photoAvailable, photo]);

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
        const next = { ...record, [name]: value };
        if (name === "totalMarks" || name === "obtainedMarks") {
          const total = Number(next.totalMarks || 0), obtained = Number(next.obtainedMarks || 0);
          next.marksPercentage = total > 0 && obtained >= 0 && obtained <= total
            ? Number(((obtained / total) * 100).toFixed(2)) : undefined;
        }
        return next;
      }),
    }));

  const save = async (event: React.FormEvent) => {
    event.preventDefault();
    if (!admission.photoAvailable && !photo) {
      toast.error("Passport-size photo is required");
      return;
    }
    const missingDocument = documentDefinitions.find((item) => item.required && !admission[item.available] && !documents[item.type]);
    if (missingDocument) { toast.error(`${missingDocument.label} is required`); return; }
    const requiredRecords = values.academicRecords.filter((record) => ["10TH", "12TH"].includes(record.qualification));
    if (requiredRecords.some((record) => !record.totalMarks || record.obtainedMarks === undefined || Number(record.obtainedMarks) > Number(record.totalMarks))) {
      toast.error("Enter valid total and obtained marks for both 10th and 12th");
      return;
    }
    setSaving(true);
    try {
      if (photo) await api.uploadMyAdmissionPhoto(photo);
      for (const [type, file] of Object.entries(documents)) await api.uploadMyAdmissionDocument(type as api.AdmissionDocumentType, file);
      await api.submitMyDetailedAdmission(values);
      toast.success(admission.status === "STUDENT_SECTION_REJECTED" ? "Corrected admission form resubmitted to Student Section" : "Detailed admission form submitted to Student Section");
      setPhoto(null);
      setDocuments({});
      await onSaved();
    } catch (error) {
      toast.error(handleApiError(error).message);
    } finally {
      setSaving(false);
    }
  };

  return (
    <form onSubmit={save} className="space-y-5">
      <Section title="Passport-size photo">
        <div className="flex flex-wrap items-center gap-5">
          <div className="grid h-40 w-32 place-items-center overflow-hidden rounded-xl border bg-slate-50">
            {preview ? (
              <img src={preview} alt="Student passport" className="h-full w-full object-cover" />
            ) : (
              <span className="px-2 text-center text-xs text-slate-400">No photo uploaded</span>
            )}
          </div>
          <div>
            <input
              type="file"
              accept="image/jpeg,image/png,image/webp"
              onChange={(event) => setPhoto(event.target.files?.[0] ?? null)}
              className="block text-sm"
            />
            <p className="mt-2 text-xs text-slate-500">JPEG, PNG or WebP, maximum 2 MB.</p>
          </div>
        </div>
      </Section>

      <Section title="Admission documents">
        <div className="grid gap-4 md:grid-cols-2 xl:grid-cols-3">{documentDefinitions.map((item) => <DocumentUpload key={item.type} label={item.label} required={item.required} available={admission[item.available]} file={documents[item.type] || null} setFile={(file) => setDocuments((current) => ({ ...current, [item.type]: file || undefined }))}/>)}</div>
        <p className="mt-3 text-xs text-slate-500">Upload PDF, JPEG, PNG, or WebP files up to 5 MB each.</p>
      </Section>

      <Section title="Applicant details">
        <Grid>
          <Input label="Full name" required value={values.fullName} onChange={(e) => set("fullName", e.target.value)} />
          <Input label="Applicant mobile" required value={values.phone} onChange={(e) => set("phone", e.target.value)} />
          <Input label="Applicant email" type="email" required value={values.email} onChange={(e) => set("email", e.target.value)} />
          <Input label="Date of birth" type="date" required value={values.dateOfBirth} onChange={(e) => set("dateOfBirth", e.target.value)} />
          <Select label="Gender" value={values.gender} onChange={(e) => set("gender", e.target.value)} options={[
            { label: "Select gender", value: "" },
            { label: "Male", value: "MALE" },
            { label: "Female", value: "FEMALE" },
            { label: "Other", value: "OTHER" },
          ]} />
          <Input label="Place of birth" required value={values.placeOfBirth} onChange={(e) => set("placeOfBirth", e.target.value)} />
          <Select label="Marital status" value={values.maritalStatus} onChange={(e) => set("maritalStatus", e.target.value)} options={[
            { label: "Unmarried", value: "UNMARRIED" },
            { label: "Married", value: "MARRIED" },
            { label: "Other", value: "OTHER" },
          ]} />
          <Input label="Aadhaar card number" required inputMode="numeric" maxLength={12} value={values.aadhaarNumber} onChange={(e) => set("aadhaarNumber", e.target.value.replace(/\D/g, ""))} />
          <Input label="APAAR ID" value={values.apaarId ?? ""} onChange={(e) => set("apaarId", e.target.value)} />
          <Input label="Nationality" required value={values.nationality} onChange={(e) => set("nationality", e.target.value)} />
          <Input label="Religion" required value={values.religion} onChange={(e) => set("religion", e.target.value)} />
          <Input label="Caste" required value={values.caste} onChange={(e) => set("caste", e.target.value)} />
          <Select label="Student category" value={values.studentCategory} onChange={(e) => set("studentCategory", e.target.value)} options={[
            "OPEN", "OBC", "SC", "ST", "SBC", "VJNT", "EWS", "OTHER",
          ].map((value) => ({ label: value, value }))} />
        </Grid>
      </Section>

      <Section title="Father / guardian details">
        <Grid>
          <Input label="Father / guardian name" required value={values.parentName} onChange={(e) => set("parentName", e.target.value)} />
          <Input label="Mobile number" required value={values.parentPhone} onChange={(e) => set("parentPhone", e.target.value)} />
          <Input label="Email" type="email" value={values.parentEmail ?? ""} onChange={(e) => set("parentEmail", e.target.value)} />
        </Grid>
      </Section>

      <Section title="Permanent address">
        <Grid>
          <Textarea label="Address" required value={values.addressLine1} onChange={(e) => set("addressLine1", e.target.value)} />
          <Textarea label="Address line 2" value={values.addressLine2 ?? ""} onChange={(e) => set("addressLine2", e.target.value)} />
          <Input label="City" required value={values.city} onChange={(e) => set("city", e.target.value)} />
          <Input label="PIN code" required maxLength={6} value={values.pincode} onChange={(e) => set("pincode", e.target.value.replace(/\D/g, ""))} />
          <Input label="State" required value={values.state} onChange={(e) => set("state", e.target.value)} />
          <Input label="Phone" value={values.permanentPhone ?? ""} onChange={(e) => set("permanentPhone", e.target.value)} />
          <Input label="Email" type="email" value={values.permanentEmail ?? ""} onChange={(e) => set("permanentEmail", e.target.value)} />
        </Grid>
      </Section>

      <Section title="Correspondence address">
        <Grid>
          <Textarea label="Address" required value={values.correspondenceAddress} onChange={(e) => set("correspondenceAddress", e.target.value)} />
          <Input label="City" required value={values.correspondenceCity} onChange={(e) => set("correspondenceCity", e.target.value)} />
          <Input label="PIN code" required maxLength={6} value={values.correspondencePincode} onChange={(e) => set("correspondencePincode", e.target.value.replace(/\D/g, ""))} />
          <Input label="State" required value={values.correspondenceState} onChange={(e) => set("correspondenceState", e.target.value)} />
          <Input label="Phone" value={values.correspondencePhone ?? ""} onChange={(e) => set("correspondencePhone", e.target.value)} />
          <Input label="Mobile" value={values.correspondenceMobile ?? ""} onChange={(e) => set("correspondenceMobile", e.target.value)} />
          <Input label="Email" type="email" value={values.correspondenceEmail ?? ""} onChange={(e) => set("correspondenceEmail", e.target.value)} />
        </Grid>
      </Section>

      <Section title="Academic record">
        <div className="responsive-table">
          <table>
            <thead><tr className="border-b text-left text-slate-500">
              <th className="p-2">Qualification</th><th>School / College / Institute</th>
              <th>Board / University</th><th>Year of passing</th><th>Total marks</th><th>Obtained marks</th><th>Percentage</th>
            </tr></thead>
            <tbody>{values.academicRecords.map((record, index) => (
              <tr className="border-b" key={record.qualification}>
                <td className="p-2 font-semibold">{record.qualification}</td>
                <td className="p-2"><input className="h-10 w-full rounded-lg border px-2" value={record.instituteName ?? ""} onChange={(e) => updateRecord(index, "instituteName", e.target.value)} /></td>
                <td className="p-2"><input className="h-10 w-full rounded-lg border px-2" value={record.boardUniversity ?? ""} onChange={(e) => updateRecord(index, "boardUniversity", e.target.value)} /></td>
                <td className="p-2"><input className="h-10 w-full rounded-lg border px-2" maxLength={4} value={record.yearOfPassing ?? ""} onChange={(e) => updateRecord(index, "yearOfPassing", e.target.value.replace(/\D/g, ""))} /></td>
                <td className="p-2"><input type="number" min="0.01" step="0.01" className="h-10 w-28 rounded-lg border px-2" value={record.totalMarks ?? ""} onChange={(e) => updateRecord(index, "totalMarks", e.target.value === "" ? undefined : Number(e.target.value))} /></td>
                <td className="p-2"><input type="number" min="0" step="0.01" className="h-10 w-28 rounded-lg border px-2" value={record.obtainedMarks ?? ""} onChange={(e) => updateRecord(index, "obtainedMarks", e.target.value === "" ? undefined : Number(e.target.value))} /></td>
                <td className="p-2"><input readOnly className="h-10 w-28 rounded-lg border bg-slate-50 px-2 font-semibold" value={record.marksPercentage === undefined || record.marksPercentage === null ? "" : `${record.marksPercentage}%`} /></td>
              </tr>
            ))}</tbody>
          </table>
        </div>
      </Section>

      <Section title="Entrance test and last graduation">
        <Grid>
          <Input label="Seat number of qualifying entrance test" value={values.qualifyingEntranceSeatNumber ?? ""} onChange={(e) => set("qualifyingEntranceSeatNumber", e.target.value)} />
          <Input label="Total score in test" type="number" min="0" step="0.01" value={values.qualifyingEntranceTotalScore ?? ""} onChange={(e) => set("qualifyingEntranceTotalScore", e.target.value === "" ? undefined : Number(e.target.value))} />
          <Input label="Last graduation college name" value={values.lastGraduationCollegeName ?? ""} onChange={(e) => set("lastGraduationCollegeName", e.target.value)} />
          <Textarea label="Last graduation college address" value={values.lastGraduationCollegeAddress ?? ""} onChange={(e) => set("lastGraduationCollegeAddress", e.target.value)} />
        </Grid>
      </Section>

      <Button type="submit" loading={saving}>{admission.status === "STUDENT_SECTION_REJECTED" ? "Resubmit corrected form" : "Submit form to Student Section"}</Button>
    </form>
  );
}

export function DetailedAdmissionView({
  admission,
  principal = false,
}: {
  admission: StudentSectionAdmissionResponse;
  principal?: boolean;
}) {
  const [photoUrl, setPhotoUrl] = useState<string | null>(null);
  useEffect(() => {
    if (!admission.photoAvailable) return;
    let url = "";
    api.getAdmissionPhoto(admission.id, principal).then((value) => {
      url = value;
      setPhotoUrl(value);
    }).catch(() => setPhotoUrl(null));
    return () => { if (url) URL.revokeObjectURL(url); };
  }, [admission.id, admission.photoAvailable, principal]);
  const rows = useMemo(() => [
    ["Place of birth", admission.placeOfBirth],
    ["Marital status", admission.maritalStatus],
    ["Aadhaar", admission.aadhaarNumber],
    ["APAAR ID", admission.apaarId],
    ["Nationality", admission.nationality],
    ["Religion", admission.religion],
    ["Caste", admission.caste],
    ["Correspondence address", [admission.correspondenceAddress, admission.correspondenceCity, admission.correspondenceState, admission.correspondencePincode].filter(Boolean).join(", ")],
    ["Entrance seat number", admission.qualifyingEntranceSeatNumber],
    ["Entrance total score", admission.qualifyingEntranceTotalScore],
    ["Last graduation college", admission.lastGraduationCollegeName],
    ["Last graduation address", admission.lastGraduationCollegeAddress],
  ], [admission]);
  return (
    <div className="space-y-5">
      <Section title="Complete admission details">
        <div className="grid gap-6 lg:grid-cols-[140px_1fr]">
          <div className="h-44 overflow-hidden rounded-xl border bg-slate-50">
            {photoUrl ? <img src={photoUrl} alt="Student passport" className="h-full w-full object-cover" /> : <div className="grid h-full place-items-center text-xs text-slate-400">No photo</div>}
          </div>
          <dl className="grid gap-4 sm:grid-cols-2">
            {rows.map(([label, value]) => <div key={String(label)}><dt className="text-xs font-semibold uppercase text-slate-400">{label}</dt><dd className="mt-1 text-sm">{value || "-"}</dd></div>)}
          </dl>
        </div>
      </Section>
      <Section title="Academic record">
        <div className="responsive-table"><table><thead><tr className="border-b text-left text-slate-500"><th className="p-2">Qualification</th><th>Institute</th><th>Board / University</th><th>Year</th><th>Total</th><th>Obtained</th><th>Percentage</th></tr></thead><tbody>{admission.academicRecords?.map((record) => <tr className="border-b" key={record.qualification}><td className="p-2 font-semibold">{record.qualification}</td><td>{record.instituteName || "-"}</td><td>{record.boardUniversity || "-"}</td><td>{record.yearOfPassing || "-"}</td><td>{record.totalMarks ?? "-"}</td><td>{record.obtainedMarks ?? "-"}</td><td>{record.marksPercentage === null || record.marksPercentage === undefined ? "-" : `${record.marksPercentage}%`}</td></tr>)}</tbody></table></div>
      </Section>
    </div>
  );
}

function Section({ title, children }: { title: string; children: React.ReactNode }) {
  return <Card className="p-5"><h2 className="mb-4 text-lg font-bold">{title}</h2>{children}</Card>;
}
function Grid({ children }: { children: React.ReactNode }) {
  return <div className="grid gap-4 md:grid-cols-2">{children}</div>;
}

function DocumentUpload({ label, required, available, file, setFile }: { label: string; required: boolean; available: boolean; file: File | null; setFile: (file: File | null) => void }) {
  return <label className="rounded-xl border border-dashed border-slate-300 p-4">
    <span className="block font-semibold">{label} <span className={required ? "text-rose-600" : "text-slate-400"}>{required ? "* Required" : "(Optional)"}</span></span>
    <span className={`mt-1 block text-xs ${available ? "text-emerald-600" : "text-amber-600"}`}>
      {file ? file.name : available ? "Already uploaded — choose a file to replace" : "Required before submission"}
    </span>
    <input className="mt-3 block w-full text-sm" type="file" accept="application/pdf,image/jpeg,image/png,image/webp" onChange={(event) => setFile(event.target.files?.[0] ?? null)} />
  </label>;
}

const documentDefinitions: { type: api.AdmissionDocumentType; label: string; required: boolean; available: keyof Pick<StudentSectionAdmissionResponse, "tenthMarksheetAvailable" | "twelfthMarksheetAvailable" | "graduationPgCertificateAvailable" | "leavingCertificateAvailable" | "migrationCertificateAvailable" | "gapAffidavitAvailable" | "casteCertificateAvailable" | "incomeProofAvailable" | "nameChangeCertificateAvailable" | "aadhaarCardAvailable"> }[] = [
  { type: "TENTH_MARKSHEET", label: "10th marksheet", required: true, available: "tenthMarksheetAvailable" },
  { type: "TWELFTH_MARKSHEET", label: "12th marksheet", required: true, available: "twelfthMarksheetAvailable" },
  { type: "LEAVING_CERTIFICATE", label: "Leaving certificate", required: true, available: "leavingCertificateAvailable" },
  { type: "AADHAAR_CARD", label: "Aadhaar card", required: true, available: "aadhaarCardAvailable" },
  { type: "GRADUATION_PG_CERTIFICATE", label: "Graduation / PG certificate", required: false, available: "graduationPgCertificateAvailable" },
  { type: "MIGRATION_CERTIFICATE", label: "Migration certificate", required: false, available: "migrationCertificateAvailable" },
  { type: "GAP_AFFIDAVIT", label: "GAP affidavit", required: false, available: "gapAffidavitAvailable" },
  { type: "CASTE_CERTIFICATE", label: "Caste certificate", required: false, available: "casteCertificateAvailable" },
  { type: "INCOME_PROOF", label: "Income proof", required: false, available: "incomeProofAvailable" },
  { type: "NAME_CHANGE_CERTIFICATE", label: "Marriage certificate / name-change proof", required: false, available: "nameChangeCertificateAvailable" },
];
