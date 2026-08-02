import { ArrowLeft, Download } from "lucide-react";
import { useCallback, useEffect, useState, type ReactNode } from "react";
import { Link, useParams } from "react-router-dom";
import { toast } from "sonner";
import { Button } from "@/components/common/Button";
import { Card } from "@/components/common/Card";
import { Loader } from "@/components/common/Loader";
import { handleApiError } from "@/lib/handleApiError";
import * as api from "@/features/admissions/api";
import type {
  AdmissionDocumentCustody,
  AdmissionDocumentRequirement,
  AdmissionPrintResponse,
  StudentSectionAdmissionResponse,
} from "@/features/admissions/types";

const PDF_PAGE_WIDTH_MM = 210;
const PDF_PAGE_HEIGHT_MM = 297;

export function AdmissionPrintPage() {
  const { admissionId = "" } = useParams();
  const id = Number(admissionId);
  const studentOwned = !admissionId;
  const [data, setData] = useState<AdmissionPrintResponse | null>(null);
  const [loading, setLoading] = useState(true);
  const [downloading, setDownloading] = useState(false);
  const [photoUrl, setPhotoUrl] = useState<string | null>(null);
  const [photoLoading, setPhotoLoading] = useState(false);
  const [admission, setAdmission] = useState<StudentSectionAdmissionResponse | null>(null);
  const [requirements, setRequirements] = useState<AdmissionDocumentRequirement[]>([]);
  const [custody, setCustody] = useState<AdmissionDocumentCustody[]>([]);

  const load = useCallback(() => {
    setLoading(true);
    (studentOwned ? api.getMyAdmissionPrintData() : api.getAdmissionPrintData(id))
      .then(setData)
      .catch((err) => toast.error(handleApiError(err).message))
      .finally(() => setLoading(false));
  }, [id, studentOwned]);

  useEffect(() => {
    load();
  }, [load]);

  useEffect(() => {
    const admissionRequest = studentOwned
      ? api.getMyAdmission()
      : api.getStudentSectionAdmission(id);
    const requirementsRequest = studentOwned
      ? api.getMyAdmissionDocumentRequirements()
      : api.getAdmissionDocumentRequirements(id);
    const custodyRequest = studentOwned ? Promise.resolve([]) : api.getDocumentCustody(id);
    Promise.all([admissionRequest, requirementsRequest, custodyRequest])
      .then(([admissionData, requirementData, custodyData]) => {
        setAdmission(admissionData);
        setRequirements(requirementData.filter((item) => item.active));
        setCustody(custodyData);
      })
      .catch((error) => toast.error(handleApiError(error).message));
  }, [id, studentOwned]);

  useEffect(() => {
    if (!data?.student.hasPhoto) {
      setPhotoUrl(null);
      return;
    }
    let active = true;
    let objectUrl: string | null = null;
    setPhotoLoading(true);
    (studentOwned ? api.getMyAdmissionPhoto() : api.getAdmissionPhoto(id))
      .then((url) => {
        objectUrl = url;
        if (active) setPhotoUrl(url);
      })
      .catch(() => {
        if (active) setPhotoUrl(null);
      })
      .finally(() => {
        if (active) setPhotoLoading(false);
      });
    return () => {
      active = false;
      if (objectUrl) URL.revokeObjectURL(objectUrl);
    };
  }, [data?.student.hasPhoto, id, studentOwned]);

  const downloadPdf = async () => {
    if (!data) return;
    setDownloading(true);
    try {
      await document.fonts.ready;
      const printablePages = Array.from(
        document.querySelectorAll<HTMLElement>("[data-admission-pdf-page]"),
      );
      await Promise.all(
        printablePages.flatMap((page) =>
          Array.from(page.querySelectorAll("img")).map(async (image) => {
            if (image.complete) {
              await image.decode().catch(() => undefined);
              return;
            }
            await new Promise<void>((resolve) => {
              image.addEventListener("load", () => resolve(), { once: true });
              image.addEventListener("error", () => resolve(), { once: true });
            });
          }),
        ),
      );
      const [{ default: JsPdf }, { default: html2canvas }] = await Promise.all([
        import("jspdf"),
        import("html2canvas"),
      ]);
      const pdf = new JsPdf({
        orientation: "portrait",
        unit: "mm",
        format: "a4",
        compress: true,
        hotfixes: ["px_scaling"],
      });
      for (let index = 0; index < printablePages.length; index += 1) {
        const page = printablePages[index];
        const pageWidth = page.offsetWidth;
        const pageHeight = page.offsetHeight;
        const canvas = await html2canvas(page, {
          // Two output pixels per CSS pixel keeps small text sharp without
          // creating the previous 70+ MB, slow-to-open PDF files.
          scale: 2,
          backgroundColor: "#ffffff",
          useCORS: true,
          imageTimeout: 15_000,
          logging: false,
          removeContainer: true,
          scrollX: 0,
          scrollY: -window.scrollY,
          width: pageWidth,
          height: pageHeight,
          windowWidth: Math.max(document.documentElement.clientWidth, pageWidth),
          windowHeight: Math.max(document.documentElement.clientHeight, pageHeight),
          onclone: (_clonedDocument, clonedPage) => {
            // Lock the cloned node to the exact dimensions shown in the UI.
            // This prevents html2canvas from applying a responsive reflow while
            // it creates the off-screen export document.
            clonedPage.style.width = `${pageWidth}px`;
            clonedPage.style.minWidth = `${pageWidth}px`;
            clonedPage.style.maxWidth = `${pageWidth}px`;
            clonedPage.style.height = `${pageHeight}px`;
            clonedPage.style.minHeight = `${pageHeight}px`;
            clonedPage.style.maxHeight = `${pageHeight}px`;
          },
        });
        if (index > 0) pdf.addPage("a4", "portrait");
        pdf.addImage(
          canvas.toDataURL("image/jpeg", 0.96),
          "JPEG",
          0,
          0,
          PDF_PAGE_WIDTH_MM,
          PDF_PAGE_HEIGHT_MM,
          `admission-page-${index + 1}`,
          "MEDIUM",
        );
        canvas.width = 1;
        canvas.height = 1;
      }
      pdf.save(`admission-${data.admissionReferenceNumber}.pdf`);
      if (!studentOwned) {
        await api.markAdmissionPrinted(id, { remarks: "Admission PDF downloaded" });
      }
      toast.success("Admission PDF downloaded");
    } catch (err) {
      toast.error(handleApiError(err).message);
    } finally {
      setDownloading(false);
    }
  };

  if (loading) return <Loader label="Loading print format..." />;
  if (!data) return null;

  const courseCode = data.academic.departmentCode || "COURSE";
  const address = [
    data.student.addressLine1,
    data.student.addressLine2,
    data.student.city,
    data.student.state,
    data.student.pincode,
  ]
    .filter(Boolean)
    .join(", ");
  const correspondenceAddress = [
    data.student.correspondenceAddress,
    data.student.correspondenceCity,
    data.student.correspondenceState,
    data.student.correspondencePincode,
  ]
    .filter(Boolean)
    .join(", ");
  const [academicStart = "", academicEnd = ""] = data.academic.academicYear.split("-");
  const academicYearShort = `${academicStart.slice(-2)} - ${academicEnd.slice(-2)}`;

  return (
    <div className="page-container print-page-bg overflow-x-auto">
      <div className="no-print mb-4 flex flex-wrap gap-2">
        <Link to={studentOwned ? "/student/admission" : `/student-section/admissions/${id}`}>
          <Button variant="secondary">
            <ArrowLeft className="h-4 w-4" />
            Back
          </Button>
        </Link>
        <Button
          onClick={downloadPdf}
          loading={downloading || photoLoading}
          disabled={downloading || photoLoading}
        >
          <Download className="h-4 w-4" />
          Download PDF
        </Button>
      </div>

      <Card
        data-admission-pdf-page
        className="print-container admission-form-sheet mx-auto h-[297mm] min-h-[297mm] w-[210mm] min-w-[210mm] max-w-none overflow-hidden p-[10mm] text-black"
      >
        <InstituteHeader data={data} />

        <div className="mt-3 grid grid-cols-[minmax(0,1fr)_35mm] gap-5 border-t-2 border-slate-700 pt-3">
          <div>
            <h2 className="text-[24px] leading-tight font-extrabold uppercase tracking-wide">
              Admission Form
            </h2>
            <div className="mt-2 grid grid-cols-3 gap-4">
              <CourseValue label="Department" value={courseCode} />
              <CourseValue
                label="Course Year"
                value={formatCourseYear(
                  data.academic.courseYearDisplayName || admission?.courseYearDisplayName,
                )}
              />
              <CourseValue label="Admission year" value={data.academic.academicYear} />
            </div>
            <div className="mt-3 grid grid-cols-[auto_minmax(0,1fr)_auto] items-end gap-2 text-[11px]">
              <b className="pb-1">Form Number</b>
              <PrintField value={data.admissionReferenceNumber} compact />
              <b className="pb-1">Academic Session: {academicYearShort}</b>
            </div>
          </div>
          <div className="grid h-[45mm] w-[35mm] place-items-center overflow-hidden border border-slate-700 bg-white text-center text-[9px] text-slate-500">
            {photoUrl ? (
              <img
                src={photoUrl}
                alt="Student uploaded document"
                className="h-full w-full bg-white object-cover"
              />
            ) : (
              <span className="px-2">Passport Size Photo</span>
            )}
          </div>
        </div>

        <PersonalInformationSection
          data={data}
          permanentAddress={address}
          correspondenceAddress={correspondenceAddress || address}
        />
      </Card>

      <Card
        data-admission-pdf-page
        className="print-container admission-form-sheet admission-form-page-break mx-auto mt-8 h-[297mm] min-h-[297mm] w-[210mm] min-w-[210mm] max-w-none overflow-hidden p-[10mm] text-black"
      >
        <ContinuationHeader
          collegeName={data.college.collegeName}
          admissionReferenceNumber={data.admissionReferenceNumber}
          title="Academic and Entrance Details"
        />
        <AcademicRecordTable data={data} />
        <EntranceDetailsSection data={data} />

        <div className="mt-12 grid grid-cols-[1fr_1fr_1.35fr] items-end gap-8 text-[11px]">
          <PrintField label="Date" value="" />
          <PrintField label="Place" value="" />
          <SignatureLine label="Signature of Applicant" />
        </div>
      </Card>

      <Card
        data-admission-pdf-page
        className="print-container admission-form-sheet admission-form-page-break mx-auto mt-8 h-[297mm] min-h-[297mm] w-[210mm] min-w-[210mm] max-w-none overflow-hidden p-[10mm] text-black"
      >
        <DocumentChecklist admission={admission} requirements={requirements} custody={custody} />
      </Card>

      <Card
        data-admission-pdf-page
        className="print-container admission-form-sheet admission-form-page-break mx-auto mt-8 h-[297mm] min-h-[297mm] w-[210mm] min-w-[210mm] max-w-none overflow-hidden p-[10mm] text-black"
      >
        <DeclarationSection declarations={data.declarations} />
        <UndertakingSection />
      </Card>
    </div>
  );
}

function InstituteHeader({ data }: { data: AdmissionPrintResponse }) {
  return (
    <header className="grid grid-cols-[80px_minmax(0,1fr)_80px] items-center gap-4 text-center">
      <div>
        <div className="mx-auto grid h-16 w-16 place-items-center text-[10px]">
          {data.college.logoUrl ? (
            <img
              src={data.college.logoUrl}
              alt={`${data.college.collegeName} logo`}
              className="h-16 w-16 object-contain"
            />
          ) : (
            ""
          )}
        </div>
      </div>
      <div>
        <p className="text-[15px] leading-5 font-bold uppercase">Jadhavar Group of Institutes</p>
        <p className="text-xs leading-5">Aditya Educational Foundation's</p>
        <h1 className="text-[22px] leading-7 font-extrabold">{data.college.collegeName}</h1>
        <p className="text-[13px] leading-5">
          {[data.college.address, data.college.city, data.college.state].filter(Boolean).join(", ")}
        </p>
        <p className="text-[13px] leading-5">
          Email: {data.college.contactEmail || "-"} | Phone: {data.college.contactPhone || "-"}
        </p>
      </div>
      <div aria-hidden />
    </header>
  );
}

function ContinuationHeader({
  collegeName,
  admissionReferenceNumber,
  title,
}: {
  collegeName: string;
  admissionReferenceNumber: string;
  title: string;
}) {
  return (
    <header className="border-b-2 border-slate-700 pb-3">
      <p className="text-[13px] font-bold uppercase tracking-wide text-slate-700">{collegeName}</p>
      <div className="mt-1 flex items-end justify-between gap-6">
        <h2 className="text-[22px] leading-7 font-extrabold uppercase">{title}</h2>
        <p className="shrink-0 text-[11px] font-semibold">Form No: {admissionReferenceNumber}</p>
      </div>
    </header>
  );
}

function DeclarationSection({ declarations }: { declarations: string[] }) {
  const items = declarations.length
    ? declarations
    : [
        "I hereby declare that the information given above is true to the best of my knowledge and the certificates furnished by me are relevant to my application.",
        "I fully understand that admission once granted in particular category will be final and I will not be entitled to admission in any other category.",
        "I hereby agree to abide by all Rules, Regulation, Acts and Laws enforced by Government / University / Institute.",
        "I fully understand that the Director of the institute will have full liberty to expel me from the institute for infringement of rules of conduct, discipline, attendance and the information given above.",
      ];
  return (
    <section className="text-[12px] leading-6">
      <SectionHeading>Declaration</SectionHeading>
      <div className="mt-4">
        <StatementList items={items} />
      </div>
      <div className="ml-auto mt-14 w-64">
        <SignatureLine label="Signature of Applicant" />
      </div>
    </section>
  );
}

function UndertakingSection() {
  return (
    <section className="mt-14 text-[12px] leading-6">
      <SectionHeading>Undertaking</SectionHeading>
      <div className="mt-4">
        <StatementList
          items={[
            "I undertake to observe full attendance as per the University/Institute Rules and failing which I am aware that my terms will not be granted.",
            "So long as I am a student of Institute, I will do nothing either inside or outside the Institute which may result in disciplinary action against me under the Rules, Act and Laws.",
            "I agree and undertake that if the fees and other charges decided by the Institute are more than the current academic year fees, then I will pay the difference to the Institute on demand.",
          ]}
        />
      </div>
      <div className="mt-16 grid grid-cols-2 items-end gap-x-16 gap-y-10">
        <PrintField label="Date" value="" />
        <SignatureLine label="Signature of Applicant" />
        <PrintField label="Place" value="" />
        <SignatureLine label="Signature of Parent / Guardian" />
      </div>
    </section>
  );
}

function PersonalInformationSection({
  data,
  permanentAddress,
  correspondenceAddress,
}: {
  data: AdmissionPrintResponse;
  permanentAddress: string;
  correspondenceAddress: string;
}) {
  return (
    <section className="mt-5 text-[11px] leading-[15px]">
      <SectionHeading showRule={false}>Personal Information</SectionHeading>
      <div className="mt-4 grid grid-cols-6 gap-x-5 gap-y-3">
        <PrintField
          label="Full Name of Applicant"
          value={data.student.fullName}
          className="col-span-6"
        />
        <PrintField label="Gender" value={data.student.gender} className="col-span-2" />
        <PrintField
          label="Date of Birth"
          value={shortDate(data.student.dateOfBirth)}
          className="col-span-2"
        />
        <PrintField
          label="Place of Birth"
          value={data.student.placeOfBirth}
          className="col-span-2"
        />
        <PrintField
          label="Aadhaar Card Number"
          value={data.student.aadhaarNumber}
          className="col-span-2"
        />
        <PrintField label="APAAR ID" value={data.student.apaarId} className="col-span-2" />
        <PrintField
          label="Marital Status"
          value={data.student.maritalStatus}
          className="col-span-2"
        />
        <PrintField
          label="Nationality"
          value={data.student.nationality || "Indian"}
          className="col-span-2"
        />
        <PrintField label="Religion" value={data.student.religion} className="col-span-2" />
        <PrintField label="Caste" value={data.student.caste} className="col-span-2" />
        <PrintField
          label="Applicant Mobile Number"
          value={data.student.phone}
          className="col-span-2"
        />
        <PrintField label="Applicant Email ID" value={data.student.email} className="col-span-4" />
        <PrintField
          label="Father / Mother / Guardian Name"
          value={data.parent.parentName}
          className="col-span-3"
        />
        <PrintField
          label="Guardian Mobile Number"
          value={data.parent.parentPhone}
          className="col-span-3"
        />
        <PrintField label="Guardian Email" value={data.parent.parentEmail} className="col-span-6" />
        <PrintField label="Permanent Address" value={permanentAddress} className="col-span-6" />
        <PrintField label="PIN Code" value={data.student.pincode} className="col-span-2" />
        <PrintField label="State" value={data.student.state} className="col-span-2" />
        <PrintField label="City" value={data.student.city} className="col-span-2" />
        <PrintField
          label="Correspondence Address"
          value={correspondenceAddress}
          className="col-span-6"
        />
        <PrintField
          label="Correspondence PIN Code"
          value={data.student.correspondencePincode || data.student.pincode}
          className="col-span-2"
        />
        <PrintField
          label="Correspondence State"
          value={data.student.correspondenceState || data.student.state}
          className="col-span-2"
        />
        <PrintField
          label="Correspondence City"
          value={data.student.correspondenceCity || data.student.city}
          className="col-span-2"
        />
        <PrintField
          label="Correspondence Mobile Number"
          value={data.student.correspondenceMobile || data.student.correspondencePhone}
          className="col-span-3"
        />
        <PrintField
          label="Correspondence Email"
          value={data.student.correspondenceEmail}
          className="col-span-3"
        />
      </div>
    </section>
  );
}

function AcademicRecordTable({ data }: { data: AdmissionPrintResponse }) {
  const savedRows = data.academic.academicRecords?.map((record) => [
    qualificationLabel(record.qualification),
    record.instituteName || "",
    record.boardUniversity || "",
    record.yearOfPassing || "",
    record.totalMarks ?? "",
    record.obtainedMarks ?? "",
    record.marksPercentage ?? "",
  ]);
  const rows = savedRows?.length
    ? savedRows
    : [
        ["10th", "", "", "", "", "", ""],
        [
          data.academic.previousClassName || "12th / Graduation",
          data.academic.previousSchoolName || "",
          "",
          "",
          "",
          "",
          data.academic.previousPercentage ?? "",
        ],
      ];
  while (rows.length < 4) rows.push(["", "", "", "", "", "", ""]);
  return (
    <section className="mt-7 break-inside-avoid">
      <SectionHeading>Academic Record</SectionHeading>
      <table className="admission-pdf-table mt-4 w-full table-fixed text-center text-[10px] leading-[14px]">
        <colgroup>
          <col className="w-[13%]" />
          <col className="w-[25%]" />
          <col className="w-[20%]" />
          <col className="w-[11%]" />
          <col className="w-[10%]" />
          <col className="w-[11%]" />
          <col className="w-[10%]" />
        </colgroup>
        <thead className="table-header-group">
          <tr className="bg-slate-100">
            {[
              "Qualification",
              "School / College / Institute",
              "Board / University",
              "Year of Passing",
              "Total Marks",
              "Obtained Marks",
              "Percentage",
            ].map((head) => (
              <th
                key={head}
                className="whitespace-normal px-2 py-4 align-middle text-[9px] leading-[12px] font-bold"
              >
                {head}
              </th>
            ))}
          </tr>
        </thead>
        <tbody>
          {rows.map((row, index) => (
            <tr key={index} className="break-inside-avoid">
              {row.map((cell, cellIndex) => (
                <td key={cellIndex} className="whitespace-normal px-2 py-4 align-middle">
                  {safeValue(cell)}
                </td>
              ))}
            </tr>
          ))}
        </tbody>
      </table>
    </section>
  );
}

function EntranceDetailsSection({ data }: { data: AdmissionPrintResponse }) {
  const exams = data.academic.entranceExams?.length
    ? data.academic.entranceExams
    : data.academic.qualifyingEntranceSeatNumber ||
        data.academic.qualifyingEntranceTotalScore != null
      ? [
          {
            examName: data.academic.qualifyingEntranceSeatNumber || "Qualifying entrance test",
            result: String(data.academic.qualifyingEntranceTotalScore ?? "Not specified"),
          },
        ]
      : [];
  return (
    <section className="mt-8 break-inside-avoid text-[11px]">
      <SectionHeading>Entrance and Final Details</SectionHeading>
      {exams.length > 0 && (
        <table className="admission-pdf-table mt-4 w-full table-fixed text-[10px] leading-[14px]">
          <thead>
            <tr>
              <th className="w-12 px-2 py-2">No.</th>
              <th className="px-2 py-2">Entrance Exam Name</th>
              <th className="px-2 py-2">Result / Score</th>
            </tr>
          </thead>
          <tbody>
            {exams.map((exam, index) => (
              <tr key={`${exam.examName}-${index}`}>
                <td className="px-2 py-2 text-center">{index + 1}</td>
                <td className="px-2 py-2">{safeValue(exam.examName)}</td>
                <td className="px-2 py-2">{safeValue(exam.result)}</td>
              </tr>
            ))}
          </tbody>
        </table>
      )}
      <div className="mt-4 grid grid-cols-2 gap-x-6 gap-y-4">
        <PrintField
          label="Last Graduation College Name and Address"
          value={[
            data.academic.lastGraduationCollegeName,
            data.academic.lastGraduationCollegeAddress,
          ]
            .filter(Boolean)
            .join(", ")}
          className="col-span-2"
        />
      </div>
    </section>
  );
}

function CourseValue({ label, value }: { label: string; value: ReactNode }) {
  return (
    <span className="min-w-20 text-center">
      <span className="block text-[9px] leading-4 font-semibold uppercase tracking-wide text-slate-600">
        {label}
      </span>
      <span className="block px-1 pt-0.5 pb-1 text-[14px] leading-5 font-bold uppercase">
        {value}
      </span>
    </span>
  );
}

function SectionHeading({
  children,
  showRule = true,
}: {
  children: ReactNode;
  showRule?: boolean;
}) {
  return (
    <div className="break-inside-avoid">
      <h3 className="text-[14px] leading-5 font-bold uppercase tracking-[0.08em] text-slate-800">
        {children}
      </h3>
      {showRule && <div aria-hidden className="mt-2 border-t border-slate-700" />}
    </div>
  );
}

function PrintField({
  label,
  value,
  className = "",
  compact = false,
}: {
  label?: string;
  value?: unknown;
  className?: string;
  compact?: boolean;
}) {
  return (
    <div className={`min-w-0 break-inside-avoid ${className}`}>
      {label && (
        <div className="mb-1 text-[9px] leading-[12px] font-bold uppercase tracking-wide text-slate-600">
          {label}
        </div>
      )}
      <div
        className={`min-w-0 whitespace-normal border-b border-slate-500 px-0.5 font-medium text-slate-950 ${
          compact
            ? "min-h-6 pb-1.5 text-[11px] leading-[15px]"
            : "min-h-7 pb-1.5 text-[11px] leading-[15px]"
        }`}
      >
        {safeValue(value) || "\u00a0"}
      </div>
    </div>
  );
}

function SignatureLine({ label }: { label: string }) {
  return (
    <div className="break-inside-avoid pt-7 text-center">
      <div className="border-t border-slate-700 pt-2 text-[10px] leading-4 font-semibold">
        {label}
      </div>
    </div>
  );
}

function safeValue(value: unknown): string {
  if (value === null || value === undefined) return "";
  if (typeof value === "string") return value.trim();
  if (typeof value === "number") return Number.isFinite(value) ? String(value) : "";
  if (typeof value === "boolean") return value ? "Yes" : "No";
  return "";
}

function formatCourseYear(value?: string | null): string {
  return value?.trim().replaceAll("_", " ").replace(/\s+/g, " ").toUpperCase() ?? "";
}

function DocumentChecklist({
  admission,
  requirements,
  custody,
}: {
  admission: StudentSectionAdmissionResponse | null;
  requirements: AdmissionDocumentRequirement[];
  custody: AdmissionDocumentCustody[];
}) {
  const custodyByType = new Map(custody.map((item) => [item.documentType, item]));
  const required = requirements.filter((item) => item.required);
  const optional = requirements.filter((item) => !item.required);
  const submittedRequired = required.filter((item) =>
    admission?.uploadedDocuments.includes(item.documentKey),
  ).length;
  const pending = required.filter(
    (item) => !admission?.uploadedDocuments.includes(item.documentKey),
  );
  return (
    <section className="text-[11px] leading-[17px]">
      <h2 className="text-center text-lg font-extrabold uppercase">
        Department Document Checklist
      </h2>
      <div className="mt-2 h-0.5 bg-slate-700" />
      <p className="mt-2 text-center text-xs font-semibold">
        {admission?.departmentName || "Department"} - {admission?.academicYear || ""}
      </p>
      <div className="mt-5 grid grid-cols-3 gap-x-5 gap-y-3">
        {[
          ["Total documents", requirements.length],
          ["Required", required.length],
          ["Optional", optional.length],
          ["Required submitted", submittedRequired],
          ["Required pending", pending.length],
          ["Verification", pending.length ? "Pending" : "Complete"],
        ].map(([label, value]) => (
          <div key={String(label)} className="border-b border-slate-500 pb-1">
            <span className="block text-[9px] leading-3 font-bold uppercase tracking-wide text-slate-600">
              {label}
            </span>
            <b className="text-xs">{value}</b>
          </div>
        ))}
      </div>
      <table className="admission-pdf-table mt-5 w-full table-fixed text-[9px] leading-[13px]">
        <colgroup>
          <col className="w-[5%]" />
          <col className="w-[27%]" />
          <col className="w-[12%]" />
          <col className="w-[9%]" />
          <col className="w-[9%]" />
          <col className="w-[9%]" />
          <col className="w-[9%]" />
          <col className="w-[20%]" />
        </colgroup>
        <thead className="table-header-group">
          <tr className="bg-slate-100">
            {[
              "No.",
              "Document",
              "Requirement",
              "Submitted",
              "Original",
              "Xerox",
              "Verified",
              "Remark",
            ].map((heading) => (
              <th
                key={heading}
                className="whitespace-normal px-1 py-3.5 align-middle text-[8px] leading-[11px] font-bold"
              >
                {heading}
              </th>
            ))}
          </tr>
        </thead>
        <tbody>
          {requirements.map((requirement, index) => {
            const documentCustody = custodyByType.get(requirement.documentKey);
            const submitted = Boolean(
              admission?.uploadedDocuments.includes(requirement.documentKey),
            );
            return (
              <tr key={requirement.documentKey} className="break-inside-avoid">
                <td className="px-1 py-3 text-center align-middle">{index + 1}</td>
                <td className="whitespace-normal px-2 py-3 align-middle font-semibold [overflow-wrap:normal] [word-break:normal]">
                  {requirement.documentName}
                </td>
                <td className="px-1 py-3 text-center align-middle">
                  {requirement.required ? "Required" : "Optional"}
                </td>
                <ReliableCheckCell checked={submitted} />
                <ReliableCheckCell checked={documentCustody?.originalReceived} />
                <ReliableCheckCell checked={documentCustody?.xeroxReceived} />
                <ReliableCheckCell checked={documentVerified(admission, requirement.documentKey)} />
                <td className="whitespace-normal px-2 py-3 align-middle [overflow-wrap:normal] [word-break:normal]">
                  {documentCustody?.returnedToStudent ? "Returned to student" : ""}
                </td>
              </tr>
            );
          })}
          {!requirements.length && (
            <tr>
              <td colSpan={8} className="p-6 text-center">
                No admission documents are configured for this department.
              </td>
            </tr>
          )}
        </tbody>
      </table>
      {pending.length > 0 && (
        <div className="mt-5 border-l-2 border-slate-700 bg-slate-50 p-3">
          <h3 className="font-bold uppercase">Pending Required Documents</h3>
          <ul className="mt-2 list-inside list-disc">
            {pending.map((item) => (
              <li key={item.documentKey}>{item.documentName}</li>
            ))}
          </ul>
        </div>
      )}
      <div className="mt-3 grid grid-cols-2 gap-x-16 gap-y-3">
        {[
          "Document verification officer",
          "Admission officer",
          "Head of department",
          "Principal / authorized officer",
        ].map((label) => (
          <SignatureLine key={label} label={label} />
        ))}
      </div>
    </section>
  );
}

function ReliableCheckCell({ checked }: { checked?: boolean }) {
  return (
    <td className="px-1 py-3 text-center align-middle">
      <span
        role="img"
        aria-label={checked ? "Checked" : "Not checked"}
        className="relative mx-auto block h-4 w-4 border border-slate-800 bg-white"
      >
        {checked && (
          <span className="absolute left-[4px] top-[1px] block h-[9px] w-[5px] rotate-45 border-b-2 border-r-2 border-slate-900" />
        )}
      </span>
    </td>
  );
}

function documentVerified(admission: StudentSectionAdmissionResponse | null, key: string) {
  if (!admission) return false;
  const fields: Record<string, boolean> = {
    TENTH_MARKSHEET: admission.tenthMarksheetVerified,
    TWELFTH_MARKSHEET: admission.twelfthMarksheetVerified,
    LEAVING_CERTIFICATE: admission.leavingCertificateVerified,
    AADHAAR_CARD: admission.aadhaarCardVerified,
    GRADUATION_PG_CERTIFICATE: admission.graduationPgCertificateVerified,
    MIGRATION_CERTIFICATE: admission.migrationCertificateVerified,
    GAP_AFFIDAVIT: admission.gapAffidavitVerified,
    CASTE_CERTIFICATE: admission.casteCertificateVerified,
    INCOME_PROOF: admission.incomeProofVerified,
    NAME_CHANGE_CERTIFICATE: admission.nameChangeCertificateVerified,
  };
  return Boolean(fields[key]);
}

function StatementList({ items }: { items: string[] }) {
  return (
    <div className="space-y-3">
      {items.map((item, index) => (
        <div key={item} className="grid grid-cols-[20px_minmax(0,1fr)] gap-2">
          <b>{index + 1}.</b>
          <p>{item}</p>
        </div>
      ))}
    </div>
  );
}

function qualificationLabel(value: string) {
  return value
    .replace("10TH", "10th")
    .replace("12TH", "12th")
    .replace("GRADUATION", "Graduation")
    .replace("DIPLOMA", "Diploma");
}

function shortDate(value?: string | null) {
  if (!value) return "";
  return new Date(value).toLocaleDateString("en-IN");
}
