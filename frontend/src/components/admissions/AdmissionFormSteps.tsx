import { Check, FileText, Info } from "lucide-react";
import { Card } from "@/components/common/Card";
import { cn } from "@/lib/utils";

export type AdmissionFormStep = "information" | "documents";

const fieldLabels: Record<string, string> = {
  courseYearId: "Course year",
  fullName: "Student name",
  email: "Email",
  phone: "Mobile number",
  dateOfBirth: "Date of birth",
  gender: "Gender",
  placeOfBirth: "Place of birth",
  maritalStatus: "Marital status",
  aadhaarNumber: "Aadhaar number",
  nationality: "Nationality",
  religion: "Religion",
  caste: "Caste",
  studentCategory: "Student category",
  customCategoryName: "Selected category",
  parentName: "Father/Guardian name",
  parentPhone: "Father/Guardian mobile",
  addressLine1: "Permanent address",
  city: "Permanent-address city",
  pincode: "Permanent-address PIN code",
  state: "Permanent-address state",
  correspondenceAddress: "Correspondence address",
  correspondenceCity: "Correspondence city",
  correspondencePincode: "Correspondence PIN code",
  correspondenceState: "Correspondence state",
};

function friendlyFieldLabel(field: string) {
  const academic = field.match(/^academicRecords\[(\d+)]\.(.+)$/);
  if (academic) {
    const qualification = ["10th", "12th", "Diploma", "Graduation"][Number(academic[1])];
    const detail = fieldLabels[academic[2]] ?? academic[2].replace(/([A-Z])/g, " $1");
    return `${qualification ?? "Academic record"} ${detail}`;
  }
  const entranceExam = field.match(/^entranceExams\[(\d+)]\.(.+)$/);
  if (entranceExam) {
    const detail = entranceExam[2] === "examName" ? "exam name" : "result";
    return `Entrance exam ${Number(entranceExam[1]) + 1} ${detail}`;
  }
  return (
    fieldLabels[field] ?? field.replace(/([A-Z])/g, " $1").replace(/^./, (c) => c.toUpperCase())
  );
}

export function AdmissionInformationStep({
  active,
  children,
}: {
  active: boolean;
  children: React.ReactNode;
}) {
  return active ? <>{children}</> : null;
}

export function AdmissionDocumentsStep({
  active,
  children,
}: {
  active: boolean;
  children: React.ReactNode;
}) {
  return active ? <>{children}</> : null;
}

export function AdmissionStepNavigation({
  activeStep,
  informationComplete,
}: {
  activeStep: AdmissionFormStep;
  informationComplete: boolean;
}) {
  const steps = [
    { id: "information" as const, label: "Information", icon: Info },
    { id: "documents" as const, label: "Documents", icon: FileText },
  ];
  return (
    <ol aria-label="Admission form steps" className="grid grid-cols-2 gap-3">
      {steps.map((step, index) => {
        const active = activeStep === step.id;
        const complete = step.id === "information" && informationComplete;
        const Icon = complete ? Check : step.icon;
        return (
          <li
            key={step.id}
            aria-current={active ? "step" : undefined}
            className={cn(
              "flex min-w-0 items-center gap-3 rounded-2xl border px-4 py-3",
              active && "border-blue-500 bg-blue-50 text-blue-800",
              complete && !active && "border-emerald-300 bg-emerald-50 text-emerald-800",
              !active && !complete && "border-slate-200 bg-white text-slate-500",
            )}
          >
            <span className="grid h-9 w-9 shrink-0 place-items-center rounded-full bg-current/10">
              <Icon className="h-4 w-4" />
            </span>
            <span className="min-w-0">
              <span className="block text-[10px] font-bold uppercase tracking-wider">
                Step {index + 1}
              </span>
              <span className="block truncate text-sm font-bold">{step.label}</span>
            </span>
          </li>
        );
      })}
    </ol>
  );
}

export function AdmissionValidationSummary({
  errors,
  onSelect,
}: {
  errors: Record<string, string>;
  onSelect: (field: string) => void;
}) {
  const entries = Object.entries(errors);
  if (!entries.length) return null;
  return (
    <Card className="border-rose-300 bg-rose-50 p-4" role="alert" aria-live="assertive">
      <p className="font-bold text-rose-900">Please correct the highlighted information</p>
      <p className="mt-1 text-sm text-rose-700">
        {entries.length} {entries.length === 1 ? "field needs" : "fields need"} attention before
        documents can be uploaded.
      </p>
      <ul className="mt-3 list-disc space-y-1 pl-5 text-sm text-rose-800">
        {entries.map(([field, message]) => (
          <li key={field}>
            <button type="button" className="text-left underline" onClick={() => onSelect(field)}>
              <span className="font-semibold">{friendlyFieldLabel(field)}:</span> {message}
            </button>
          </li>
        ))}
      </ul>
    </Card>
  );
}
