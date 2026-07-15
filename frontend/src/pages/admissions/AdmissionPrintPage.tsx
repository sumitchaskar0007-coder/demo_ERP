import { ArrowLeft, Printer } from "lucide-react";
import { useCallback, useEffect, useState, type ReactNode } from "react";
import { Link, useParams } from "react-router-dom";
import { toast } from "sonner";
import { Button } from "@/components/common/Button";
import { Card } from "@/components/common/Card";
import { Loader } from "@/components/common/Loader";
import { handleApiError } from "@/lib/handleApiError";
import * as api from "@/features/admissions/api";
import type { AdmissionPrintResponse } from "@/features/admissions/types";

const documents = [
  "10th Mark Sheet",
  "12th Mark Sheet",
  "Graduation Mark Sheet - Eligible Criteria (If Applicable)",
  "Provisional Certificate",
  "Transfer Certificate (If Applicable)",
  "Migration Certificate Master Degree (if any)",
  "Gap Certificate (If Applicable)",
  "MH-CET / CMAT / ATMA / Score Card",
  "Nationality Certificate",
  "Domicile Certificate",
  "Caste Certificate (If Applicable)",
  "Caste Validity (If Applicable)",
  "Non - Creamy Layer Certificate (If Applicable)",
  "5 - Passport size Photographs",
  "Aadhar Card Xerox Copy",
  "Name Change if any (Gazette) (If Applicable)",
  "Income Certificate (If Applicable)",
  "Performa - O (Only for Minority)",
];

export function AdmissionPrintPage() {
  const { admissionId = "" } = useParams();
  const id = Number(admissionId);
  const [data, setData] = useState<AdmissionPrintResponse | null>(null);
  const [loading, setLoading] = useState(true);

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

  const markPrinted = async () => {
    try {
      await api.markAdmissionPrinted(id, { remarks: "Printed from frontend" });
      toast.success("Marked as printed");
      load();
    } catch (err) {
      toast.error(handleApiError(err).message);
    }
  };

  if (loading) return <Loader label="Loading print format..." />;
  if (!data) return null;

  const currentYear = new Date().getFullYear();
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

  return (
    <div className="page-container print-page-bg">
      <div className="no-print mb-4 flex flex-wrap gap-2">
        <Link to={`/student-section/admissions/${id}`}>
          <Button variant="secondary">
            <ArrowLeft className="h-4 w-4" />
            Back
          </Button>
        </Link>
        <Button onClick={() => window.print()}>
          <Printer className="h-4 w-4" />
          Print
        </Button>
        <Button variant="secondary" onClick={markPrinted}>
          Mark as Printed
        </Button>
      </div>

      <Card className="print-container admission-form-sheet mx-auto max-w-[820px] p-4 text-black sm:p-8">
        <InstituteHeader data={data} />

        <div className="mt-3 grid grid-cols-[1fr_112px] gap-4">
          <div>
            <div className="flex flex-wrap items-center gap-8 border-t border-black pt-3">
              <h2 className="text-[26px] font-extrabold uppercase tracking-wide">Admission Form</h2>
              <CourseBox>{courseCode}</CourseBox>
              <CourseBox>{data.academic.academicYear}</CourseBox>
            </div>
            <p className="mt-4 text-center text-sm">
              Form No. <Line value={data.admissionReferenceNumber} width="190px" /> /{" "}
              {String(currentYear).slice(2)} - {String(currentYear + 1).slice(2)}
            </p>
          </div>
          <div className="grid h-32 place-items-center border border-black text-sm">Photo</div>
        </div>

        <section className="mt-2 text-[14px] leading-8">
          <h3 className="mb-1 font-bold">Personal Information</h3>
          <NumberedLine number={1}>
            Full Name of Applicant: Mr. / Ms./Mrs. <Line value={data.student.fullName} />
            <div className="-mt-1 ml-7 text-xs">(In block letters beginning with surname)</div>
          </NumberedLine>
          <NumberedLine number={2}>
            Gender:
            <Check label="Male" checked={data.student.gender?.toLowerCase() === "male"} />
            <Check label="Female" checked={data.student.gender?.toLowerCase() === "female"} />
            <span className="ml-auto flex items-center gap-2">
              <Check checked label="" /> <span className="text-xs">Tick in appropriate Box</span>
            </span>
          </NumberedLine>
          <NumberedLine number={3}>
            Date of Birth <Line value={shortDate(data.student.dateOfBirth)} width="170px" />
            Place of Birth <Line width="170px" />
            State <Line value={data.student.state} width="150px" />
          </NumberedLine>
          <NumberedLine number={4}>
            Aadhaar Card No.: <BoxLine boxes={12} />
            <span className="ml-4">Marital Status:</span>
            <Check label="Married" />
            <Check label="Unmarried" />
          </NumberedLine>
          <NumberedLine number={5}>
            APAAR ID: <Line width="250px" />
          </NumberedLine>
          <NumberedLine number={6}>
            Nationality: <Line value="Indian" width="220px" />
            Religion & Caste: <Line width="260px" />
          </NumberedLine>
          <NumberedLine number={7}>
            Applicant's Mobile no. & Email ID:{" "}
            <Line value={`${data.student.phone} / ${data.student.email}`} />
          </NumberedLine>
          <NumberedLine number={8}>
            Applicant's Father / Guardian's Name: <Line value={data.parent.parentName} />
            <div className="ml-7 flex gap-3">
              Mobile no: <Line value={data.parent.parentPhone} width="230px" />
              Email: <Line value={data.parent.parentEmail} />
            </div>
          </NumberedLine>
          <NumberedLine number={9}>
            Permanent Address: <Line value={address} />
            <div className="grid grid-cols-1 gap-3 sm:ml-7 sm:grid-cols-3">
              <span>
                Pin: <Line value={data.student.pincode} />
              </span>
              <span>
                State: <Line value={data.student.state} />
              </span>
              <span>
                City: <Line value={data.student.city} />
              </span>
            </div>
            <div className="grid grid-cols-1 gap-3 sm:ml-7 sm:grid-cols-2">
              <span>
                Mobile no.: <Line value={data.student.phone} />
              </span>
              <span>
                Email: <Line value={data.student.email} />
              </span>
            </div>
          </NumberedLine>
          <NumberedLine number={10}>
            Correspondence Address: <Line value={address} />
            <div className="grid grid-cols-1 gap-3 sm:ml-7 sm:grid-cols-3">
              <span>
                Pin: <Line value={data.student.pincode} />
              </span>
              <span>
                State: <Line value={data.student.state} />
              </span>
              <span>
                City: <Line value={data.student.city} />
              </span>
            </div>
          </NumberedLine>
          <NumberedLine number={11}>
            Academic Record:
            <AcademicTable data={data} />
          </NumberedLine>
          <NumberedLine number={12}>
            Seat No. of Qualifying Entrance Test: <BoxLine boxes={12} />
          </NumberedLine>
          <NumberedLine number={13}>
            Total Score in the Test (CET): Written: <Line width="520px" />
          </NumberedLine>
          <NumberedLine number={14}>
            Last Graduation College Name & Address:{" "}
            <Line value={data.academic.previousSchoolName} />
          </NumberedLine>
        </section>

        <div className="mt-14 grid grid-cols-[1fr_1fr_1.4fr] items-end gap-5 text-sm">
          <span>
            Date: <Line width="150px" />
          </span>
          <span>
            Place: <Line width="180px" />
          </span>
          <b className="text-right italic">Signature of the Applicant</b>
        </div>
      </Card>

      <Card className="print-container admission-form-sheet admission-form-page-break mx-auto mt-8 max-w-[820px] p-4 text-black sm:p-8">
        <DeclarationSection declarations={data.declarations} />
        <UndertakingSection />
        <DocumentChecklist />
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
    <section className="text-[14px] leading-7">
      <h3 className="mb-4 font-bold underline">Declaration</h3>
      <ol className="list-decimal space-y-4 pl-8">
        {items.map((item) => (
          <li key={item}>{item}</li>
        ))}
      </ol>
      <p className="mt-16 text-right font-semibold">Signature of Applicant</p>
    </section>
  );
}

function UndertakingSection() {
  return (
    <section className="mt-12 text-[14px] leading-7">
      <h3 className="mb-4 font-bold underline">Undertaking</h3>
      <ol className="list-decimal space-y-4 pl-8">
        <li>
          I undertake to observe full attendance as per the University/Institute Rules and failing
          which I am aware that my terms will not be granted.
        </li>
        <li>
          So long as I am a student of Institute, I will do nothing either inside or outside the
          Institute which may result in disciplinary action against me under the Rules, Act and
          Laws.
        </li>
        <li>
          I agree and undertake that if the fees and other charges decided by the Institute are more
          than the current academic year fees, then I will pay the difference to the Institute on
          demand.
        </li>
      </ol>
      <div className="mt-16 grid grid-cols-1 gap-8 sm:grid-cols-2">
        <span>
          Date: <Line width="160px" />
        </span>
        <b className="text-right">Signature of Applicant</b>
        <span>
          Place: <Line width="160px" />
        </span>
        <b className="text-right">Signature of the Parents / Guardian</b>
      </div>
    </section>
  );
}

function DocumentChecklist() {
  return (
    <section className="mt-10 text-[13px]">
      <h3 className="mb-4 text-base font-extrabold">
        Documents Original ( for Verification ) & Set of attested xerox copies ( Attached with form
        )
      </h3>
      <div className="grid grid-cols-1 gap-y-2 sm:grid-cols-2 sm:gap-x-10">
        {documents.map((document, index) => (
          <div key={document} className="grid grid-cols-[1fr_22px] items-center gap-3">
            <span>
              {String(index + 1).padStart(2, "0")}.{document}
            </span>
            <span className="h-5 w-5 border border-black" />
          </div>
        ))}
      </div>
    </section>
  );
}

function AcademicTable({ data }: { data: AdmissionPrintResponse }) {
  const rows = [
    ["10th", "", "", "", ""],
    [
      data.academic.previousClassName || "12th / Graduation",
      data.academic.previousSchoolName || "",
      "",
      "",
      data.academic.previousPercentage ?? "",
    ],
    ["", "", "", "", ""],
    ["", "", "", "", ""],
  ];
  return (
    <table className="mt-2 w-full border-collapse text-center text-[13px] leading-5">
      <thead>
        <tr>
          {[
            "Qualification",
            "School/College/Institute",
            "Board/University",
            "Year of Passing",
            "Marks Obtained (%)",
          ].map((head) => (
            <th key={head} className="border border-black px-2 py-2 font-semibold">
              {head}
            </th>
          ))}
        </tr>
      </thead>
      <tbody>
        {rows.map((row, index) => (
          <tr key={index}>
            {row.map((cell, cellIndex) => (
              <td key={cellIndex} className="h-8 border border-black px-2">
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
    <span className="min-w-24 border border-black px-4 py-1 text-center text-xl font-bold uppercase">
      {children}
    </span>
  );
}

function NumberedLine({ number, children }: { number: number; children: ReactNode }) {
  return (
    <div className="grid grid-cols-[28px_1fr] gap-1">
      <b>{number}.</b>
      <div>{children}</div>
    </div>
  );
}

function Line({ value, width = "100%" }: { value?: unknown; width?: string }) {
  return (
    <span
      className="inline-block border-b border-black px-1 align-baseline"
      style={{ minWidth: width }}
    >
      {value ? String(value) : "\u00a0"}
    </span>
  );
}

function Check({ label, checked }: { label: string; checked?: boolean }) {
  return (
    <span className="ml-5 inline-flex items-center gap-2">
      {label && <span>{label}</span>}
      <span className="grid h-5 w-8 place-items-center border border-black text-xl leading-none">
        {checked ? "/" : ""}
      </span>
    </span>
  );
}

function BoxLine({ boxes }: { boxes: number }) {
  return (
    <span className="inline-flex align-middle">
      {Array.from({ length: boxes }).map((_, index) => (
        <span key={index} className="h-6 w-8 border border-black" />
      ))}
    </span>
  );
}

function shortDate(value?: string | null) {
  if (!value) return "";
  return new Date(value).toLocaleDateString("en-IN");
}
