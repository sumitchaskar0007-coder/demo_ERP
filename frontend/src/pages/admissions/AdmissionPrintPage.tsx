import { ArrowLeft, Download } from "lucide-react";
import { useCallback, useEffect, useState, type ReactNode } from "react";
import { Link, useParams } from "react-router-dom";
import { toast } from "sonner";
import { Button } from "@/components/common/Button";
import { Card } from "@/components/common/Card";
import { Loader } from "@/components/common/Loader";
import { handleApiError } from "@/lib/handleApiError";
import * as api from "@/features/admissions/api";
import type { AdmissionPrintResponse } from "@/features/admissions/types";

export function AdmissionPrintPage() {
  const { admissionId = "" } = useParams();
  const id = Number(admissionId);
  const [data, setData] = useState<AdmissionPrintResponse | null>(null);
  const [loading, setLoading] = useState(true);
  const [downloading, setDownloading] = useState(false);
  const [photoUrl, setPhotoUrl] = useState<string | null>(null);
  const [photoLoading, setPhotoLoading] = useState(false);

  const load = useCallback(() => {
    setLoading(true);
    api
      .getAdmissionPrintData(id)
      .then(setData)
      .catch((err) => toast.error(handleApiError(err).message))
      .finally(() => setLoading(false));
  }, [id]);

  useEffect(() => {
    load();
  }, [load]);

  useEffect(() => {
    if (!data?.student.hasPhoto) {
      setPhotoUrl(null);
      return;
    }
    let active = true;
    let objectUrl: string | null = null;
    setPhotoLoading(true);
    api
      .getAdmissionPhoto(id)
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
  }, [data?.student.hasPhoto, id]);

  const downloadPdf = async () => {
    if (!data) return;
    setDownloading(true);
    try {
      const [{ default: JsPdf }, { default: html2canvas }] = await Promise.all([
        import("jspdf"),
        import("html2canvas"),
      ]);
      const pages = Array.from(document.querySelectorAll<HTMLElement>("[data-admission-pdf-page]"));
      const pdf = new JsPdf({ orientation: "portrait", unit: "mm", format: "a4" });
      for (let index = 0; index < pages.length; index += 1) {
        const canvas = await html2canvas(pages[index], {
          scale: 2,
          backgroundColor: "#ffffff",
          useCORS: true,
          logging: false,
        });
        if (index > 0) pdf.addPage("a4", "portrait");
        const maxWidth = 190;
        const maxHeight = 277;
        const ratio = Math.min(maxWidth / canvas.width, maxHeight / canvas.height);
        const width = canvas.width * ratio;
        const height = canvas.height * ratio;
        pdf.addImage(
          canvas.toDataURL("image/jpeg", 0.96),
          "JPEG",
          (210 - width) / 2,
          10,
          width,
          height,
        );
      }
      pdf.save(`admission-${data.admissionReferenceNumber}.pdf`);
      await api.markAdmissionPrinted(id, { remarks: "Admission PDF downloaded" });
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
    <div className="page-container print-page-bg">
      <div className="no-print mb-4 flex flex-wrap gap-2">
        <Link to={`/student-section/admissions/${id}`}>
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
        className="print-container admission-form-sheet mx-auto min-h-[297mm] w-[210mm] max-w-full p-[10mm] text-black"
      >
        <InstituteHeader data={data} />

        <div className="mt-3 grid grid-cols-[minmax(0,1fr)_30mm] gap-4 border-t border-black pt-3">
          <div>
            <div className="grid grid-cols-[minmax(0,1fr)_auto_auto] items-center gap-3">
              <h2 className="text-[22px] font-extrabold uppercase tracking-wide">Admission Form</h2>
              <CourseBox>{courseCode}</CourseBox>
              <CourseBox>{data.academic.academicYear}</CourseBox>
            </div>
            <div className="mt-4 grid grid-cols-[auto_minmax(0,1fr)_auto] items-end gap-2 text-xs">
              <b>Form No.</b>
              <ValueLine value={data.admissionReferenceNumber} />
              <b>/ {academicYearShort}</b>
            </div>
          </div>
          <div className="grid h-[36mm] place-items-center overflow-hidden border border-black text-xs">
            {photoUrl ? (
              <img
                src={photoUrl}
                alt="Student uploaded document"
                className="h-full w-full bg-white object-contain"
              />
            ) : (
              "Photo"
            )}
          </div>
        </div>

        <section className="mt-3 text-[11px] leading-4">
          <h3 className="mb-1.5 border-b border-black pb-1 text-xs font-bold uppercase tracking-wide">
            Personal Information
          </h3>
          <div className="space-y-1.5">
            <FormRow number={1}>
              <FormField
                label="Full Name of Applicant: Mr. / Ms. / Mrs."
                value={data.student.fullName}
              />
              <p className="mt-0.5 text-[9px] text-slate-700">
                (In block letters beginning with surname)
              </p>
            </FormRow>
            <FormRow number={2}>
              <div className="flex flex-wrap items-center gap-x-5 gap-y-1">
                <b>Gender:</b>
                <Check label="Male" checked={data.student.gender?.toLowerCase() === "male"} />
                <Check label="Female" checked={data.student.gender?.toLowerCase() === "female"} />
                <Check label="Other" checked={data.student.gender?.toLowerCase() === "other"} />
                <span className="ml-auto text-[9px]">Tick the appropriate box</span>
              </div>
            </FormRow>
            <FormRow number={3}>
              <div className="grid grid-cols-3 gap-3">
                <FormField label="Date of Birth" value={shortDate(data.student.dateOfBirth)} />
                <FormField label="Place of Birth" value={data.student.placeOfBirth} />
                <FormField label="State" value={data.student.state} />
              </div>
            </FormRow>
            <FormRow number={4}>
              <div className="grid grid-cols-[1.35fr_1fr] items-center gap-4">
                <div className="flex items-center gap-2">
                  <b className="shrink-0">Aadhaar Card No.</b>
                  <BoxLine boxes={12} value={data.student.aadhaarNumber} />
                </div>
                <div className="flex items-center gap-3">
                  <b>Marital Status:</b>
                  <Check
                    label="Married"
                    checked={data.student.maritalStatus?.toLowerCase() === "married"}
                  />
                  <Check
                    label="Unmarried"
                    checked={data.student.maritalStatus?.toLowerCase() === "unmarried"}
                  />
                </div>
              </div>
            </FormRow>
            <FormRow number={5}>
              <FormField label="APAAR ID" value={data.student.apaarId} />
            </FormRow>
            <FormRow number={6}>
              <div className="grid grid-cols-3 gap-3">
                <FormField label="Nationality" value={data.student.nationality || "Indian"} />
                <FormField label="Religion" value={data.student.religion} />
                <FormField label="Caste" value={data.student.caste} />
              </div>
            </FormRow>
            <FormRow number={7}>
              <FormField
                label="Applicant Mobile No. & Email ID"
                value={`${data.student.phone} / ${data.student.email}`}
              />
            </FormRow>
            <FormRow number={8}>
              <FormField label="Father / Guardian Name" value={data.parent.parentName} />
              <div className="mt-1 grid grid-cols-2 gap-3">
                <FormField label="Mobile No." value={data.parent.parentPhone} />
                <FormField label="Email" value={data.parent.parentEmail} />
              </div>
            </FormRow>
            <FormRow number={9}>
              <FormField label="Permanent Address" value={address} />
              <div className="mt-1 grid grid-cols-3 gap-3">
                <FormField label="PIN" value={data.student.pincode} />
                <FormField label="State" value={data.student.state} />
                <FormField label="City" value={data.student.city} />
              </div>
              <div className="mt-1 grid grid-cols-2 gap-3">
                <FormField
                  label="Mobile No."
                  value={data.student.permanentPhone || data.student.phone}
                />
                <FormField
                  label="Email"
                  value={data.student.permanentEmail || data.student.email}
                />
              </div>
            </FormRow>
            <FormRow number={10}>
              <FormField label="Correspondence Address" value={correspondenceAddress || address} />
              <div className="mt-1 grid grid-cols-3 gap-3">
                <FormField
                  label="PIN"
                  value={data.student.correspondencePincode || data.student.pincode}
                />
                <FormField
                  label="State"
                  value={data.student.correspondenceState || data.student.state}
                />
                <FormField
                  label="City"
                  value={data.student.correspondenceCity || data.student.city}
                />
              </div>
              <div className="mt-1 grid grid-cols-2 gap-3">
                <FormField
                  label="Mobile No."
                  value={data.student.correspondenceMobile || data.student.correspondencePhone}
                />
                <FormField label="Email" value={data.student.correspondenceEmail} />
              </div>
            </FormRow>
            <FormRow number={11}>
              <b>Academic Record:</b>
              <AcademicTable data={data} />
            </FormRow>
            <FormRow number={12}>
              <FormField
                label="Seat No. of Qualifying Entrance Test"
                value={data.academic.qualifyingEntranceSeatNumber}
              />
            </FormRow>
            <FormRow number={13}>
              <FormField
                label="Total Score in the Test (CET) - Written"
                value={data.academic.qualifyingEntranceTotalScore}
              />
            </FormRow>
            <FormRow number={14}>
              <FormField
                label="Last Graduation College Name & Address"
                value={[
                  data.academic.lastGraduationCollegeName,
                  data.academic.lastGraduationCollegeAddress,
                ]
                  .filter(Boolean)
                  .join(", ")}
              />
            </FormRow>
          </div>
        </section>

        <div className="mt-7 grid grid-cols-[1fr_1fr_1.4fr] items-end gap-5 text-xs">
          <FormField label="Date" />
          <FormField label="Place" />
          <b className="border-t border-black pt-1 text-center italic">
            Signature of the Applicant
          </b>
        </div>
      </Card>

      <Card
        data-admission-pdf-page
        className="print-container admission-form-sheet admission-form-page-break mx-auto mt-8 min-h-[297mm] w-[210mm] max-w-full p-[14mm] text-black"
      >
        <DeclarationSection declarations={data.declarations} />
        <UndertakingSection />
      </Card>
    </div>
  );
}

function InstituteHeader({ data }: { data: AdmissionPrintResponse }) {
  return (
    <header className="grid grid-cols-[120px_1fr_170px] items-center gap-4 text-center">
      <div className="text-[11px] font-semibold uppercase">
        <div className="mx-auto mb-1 grid h-16 w-16 place-items-center rounded-full border border-black text-[10px]">
          {data.college.logoUrl ? (
            <img src={data.college.logoUrl} alt="" className="h-14 w-14 object-contain" />
          ) : (
            "AIMS"
          )}
        </div>
        {data.college.collegeName}
      </div>
      <div>
        <p className="text-sm font-bold uppercase">Jadhavar Group of Institutes</p>
        <p className="text-[11px]">Aditya Educational Foundation's</p>
        <h1 className="text-xl font-extrabold">{data.college.collegeName}</h1>
        <p className="text-xs">
          {[data.college.address, data.college.city, data.college.state].filter(Boolean).join(", ")}
        </p>
        <p className="text-xs">
          Email: {data.college.contactEmail || "-"} | Phone: {data.college.contactPhone || "-"}
        </p>
      </div>
      <div className="text-4xl font-serif">Jadhavar</div>
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
    <section className="text-[13px] leading-6">
      <h3 className="mb-4 border-b border-black pb-1 text-base font-bold">Declaration</h3>
      <StatementList items={items} />
      <p className="ml-auto mt-20 w-56 border-t border-black pt-1 text-center font-semibold">
        Signature of Applicant
      </p>
    </section>
  );
}

function UndertakingSection() {
  return (
    <section className="mt-12 text-[13px] leading-6">
      <h3 className="mb-4 border-b border-black pb-1 text-base font-bold">Undertaking</h3>
      <StatementList
        items={[
          "I undertake to observe full attendance as per the University/Institute Rules and failing which I am aware that my terms will not be granted.",
          "So long as I am a student of Institute, I will do nothing either inside or outside the Institute which may result in disciplinary action against me under the Rules, Act and Laws.",
          "I agree and undertake that if the fees and other charges decided by the Institute are more than the current academic year fees, then I will pay the difference to the Institute on demand.",
        ]}
      />
      <div className="mt-20 grid grid-cols-2 gap-x-16 gap-y-10">
        <FormField label="Date" />
        <b className="border-t border-black pt-1 text-center">Signature of Applicant</b>
        <FormField label="Place" />
        <b className="border-t border-black pt-1 text-center">
          Signature of the Parents / Guardian
        </b>
      </div>
    </section>
  );
}

function AcademicTable({ data }: { data: AdmissionPrintResponse }) {
  const savedRows = data.academic.academicRecords?.map((record) => [
    qualificationLabel(record.qualification),
    record.instituteName || "",
    record.boardUniversity || "",
    record.yearOfPassing || "",
    record.marksPercentage ?? "",
  ]);
  const rows = savedRows?.length
    ? savedRows
    : [
        ["10th", "", "", "", ""],
        [
          data.academic.previousClassName || "12th / Graduation",
          data.academic.previousSchoolName || "",
          "",
          "",
          data.academic.previousPercentage ?? "",
        ],
      ];
  while (rows.length < 4) rows.push(["", "", "", "", ""]);
  return (
    <table className="mt-1 w-full table-fixed border-collapse text-center text-[9px] leading-3">
      <thead>
        <tr>
          {[
            "Qualification",
            "School/College/Institute",
            "Board/University",
            "Year of Passing",
            "Marks Obtained (%)",
          ].map((head) => (
            <th key={head} className="border border-black px-1 py-1.5 font-semibold">
              {head}
            </th>
          ))}
        </tr>
      </thead>
      <tbody>
        {rows.map((row, index) => (
          <tr key={index}>
            {row.map((cell, cellIndex) => (
              <td key={cellIndex} className="h-6 border border-black px-1 py-1">
                {cell}
              </td>
            ))}
          </tr>
        ))}
      </tbody>
    </table>
  );
}

function CourseBox({ children }: { children: ReactNode }) {
  return (
    <span className="min-w-20 border border-black px-2 py-1 text-center text-sm font-bold uppercase">
      {children}
    </span>
  );
}

function FormRow({ number, children }: { number: number; children: ReactNode }) {
  return (
    <div className="grid break-inside-avoid grid-cols-[18px_minmax(0,1fr)] gap-1">
      <b>{number}.</b>
      <div className="min-w-0">{children}</div>
    </div>
  );
}

function FormField({ label, value }: { label: string; value?: unknown }) {
  return (
    <div className="grid min-w-0 grid-cols-[auto_minmax(0,1fr)] items-end gap-1.5">
      <b className="whitespace-nowrap">{label}:</b>
      <ValueLine value={value} />
    </div>
  );
}

function ValueLine({ value }: { value?: unknown }) {
  return (
    <span className="min-h-4 min-w-0 border-b border-black px-1 font-medium">
      {value !== null && value !== undefined && value !== "" ? String(value) : "\u00a0"}
    </span>
  );
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

function Check({ label, checked }: { label: string; checked?: boolean }) {
  return (
    <span className="inline-flex items-center gap-1.5 whitespace-nowrap">
      {label && <span>{label}</span>}
      <span className="grid h-4 w-4 place-items-center border border-black text-xs font-bold leading-none">
        {checked ? "X" : ""}
      </span>
    </span>
  );
}

function BoxLine({ boxes, value }: { boxes: number; value?: string | null }) {
  const characters = value?.replace(/\s/g, "").slice(0, boxes).split("") ?? [];
  return (
    <span className="inline-flex align-middle">
      {Array.from({ length: boxes }).map((_, index) => (
        <span
          key={index}
          className="grid h-5 w-5 place-items-center border border-black text-[10px]"
        >
          {characters[index] || ""}
        </span>
      ))}
    </span>
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
