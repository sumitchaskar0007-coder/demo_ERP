import { zodResolver } from "@hookform/resolvers/zod";
import { GraduationCap, LogIn } from "lucide-react";
import { useEffect, useState } from "react";
import { useForm } from "react-hook-form";
import { Link, useParams } from "react-router-dom";
import { toast } from "sonner";
import { z } from "zod";
import { Button } from "@/components/common/Button";
import { Card } from "@/components/common/Card";
import { Input } from "@/components/common/Input";
import { Loader } from "@/components/common/Loader";
import { Select } from "@/components/common/Select";
import { handleApiError } from "@/lib/handleApiError";
import { publicAdmissionSchema } from "@/lib/validators";
import * as api from "@/features/admissions/api";
import type {
  PublicAdmissionInfoResponse,
  SubmitAdmissionResponse,
} from "@/features/admissions/types";

type FormValues = z.infer<typeof publicAdmissionSchema>;

export function PublicAdmissionPage() {
  const { collegeCode = "" } = useParams();
  const [info, setInfo] = useState<PublicAdmissionInfoResponse | null>(null);
  const [result, setResult] = useState<SubmitAdmissionResponse | null>(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState("");
  const {
    register,
    handleSubmit,
    formState: { errors, isSubmitting },
  } = useForm<FormValues>({
    resolver: zodResolver(publicAdmissionSchema),
    defaultValues: {
      gender: "",
      middleName: "",
      addressLine1: "",
      addressLine2: "",
      city: "",
      state: "",
      pincode: "",
    },
  });

  useEffect(() => {
    setLoading(true);
    api
      .getPublicAdmissionInfo(collegeCode)
      .then(setInfo)
      .catch((err) => setError(handleApiError(err).message))
      .finally(() => setLoading(false));
  }, [collegeCode]);

  const onSubmit = async (values: FormValues) => {
    try {
      const submitted = await api.submitAdmission(collegeCode, values);
      setResult(submitted);
      toast.success("Admission submitted successfully");
    } catch (err) {
      toast.error(handleApiError(err).message);
    }
  };

  if (loading)
    return (
      <div className="min-h-screen bg-slate-50">
        <Loader label="Loading admission form..." />
      </div>
    );
  if (error || !info)
    return (
      <div className="grid min-h-screen place-items-center bg-slate-50 p-4">
        <Card className="max-w-lg p-8 text-center">
          <h1 className="text-2xl font-bold">Admission unavailable</h1>
          <p className="mt-2 text-slate-500">
            {error || "College admission information could not be loaded."}
          </p>
        </Card>
      </div>
    );

  if (result) {
    return (
      <div className="min-h-screen bg-slate-50 px-4 py-10">
        <Card className="mx-auto max-w-2xl p-7">
          <h1 className="text-2xl font-bold text-slate-900">Admission submitted</h1>
          <p className="mt-2 text-sm text-slate-500">
            Please save these credentials. Temporary password is shown only once.
          </p>
          <div className="mt-6 grid gap-3 rounded-2xl bg-blue-50 p-5 text-sm">
            <p>
              <b>Reference:</b> {result.admissionReferenceNumber}
            </p>
            <p>
              <b>Admission No:</b> {result.admissionNumber}
            </p>
            <p>
              <b>Email:</b> {result.email}
            </p>
            <p>
              <b>Temporary Password:</b>{" "}
              <span className="font-mono text-blue-800">{result.temporaryPassword}</span>
            </p>
          </div>
          <Button className="mt-6" onClick={() => window.location.assign("/login")}>
            <LogIn className="h-4 w-4" />
            Go to Login
          </Button>
        </Card>
      </div>
    );
  }

  const departments = [
    { label: "Select department", value: "" },
    ...info.departments.map((department) => ({
      label: `${department.name} (${department.code})`,
      value: department.id,
    })),
  ];
  return (
    <div className="min-h-screen bg-slate-50 px-4 py-8">
      <div className="mx-auto max-w-5xl">
        <Card className="mb-6 p-6">
          <div className="flex flex-col gap-4 sm:flex-row sm:items-center">
            <div className="grid h-14 w-14 place-items-center rounded-2xl bg-brand-50 text-brand-700">
              {info.logoUrl ? (
                <img src={info.logoUrl} alt="" className="h-10 w-10 object-contain" />
              ) : (
                <GraduationCap />
              )}
            </div>
            <div>
              <h1 className="text-2xl font-bold">{info.collegeName}</h1>
              <p className="text-sm text-slate-500">
                {[info.address, info.city, info.state].filter(Boolean).join(", ")}
              </p>
              <p className="mt-1 text-sm font-semibold text-brand-700">
                Academic Year {info.academicYear}
              </p>
            </div>
          </div>
        </Card>
        <form onSubmit={handleSubmit(onSubmit)} className="space-y-6">
          <FormSection title="Course / Department">
            <Select
              label="Department"
              options={departments}
              error={errors.departmentId?.message}
              {...register("departmentId")}
            />
          </FormSection>
          <FormSection title="Student Basic Details">
            <Input
              label="First name"
              error={errors.firstName?.message}
              {...register("firstName")}
            />
            <Input
              label="Middle name"
              error={errors.middleName?.message}
              {...register("middleName")}
            />
            <Input label="Last name" error={errors.lastName?.message} {...register("lastName")} />
            <Input
              label="Email"
              type="email"
              error={errors.email?.message}
              {...register("email")}
            />
            <Input label="Phone" error={errors.phone?.message} {...register("phone")} />
            <Input
              label="Date of birth"
              type="date"
              error={errors.dateOfBirth?.message}
              {...register("dateOfBirth")}
            />
            <Select
              label="Gender"
              options={[
                { label: "Select gender", value: "" },
                { label: "Male", value: "Male" },
                { label: "Female", value: "Female" },
                { label: "Other", value: "Other" },
              ]}
              error={errors.gender?.message}
              {...register("gender")}
            />
          </FormSection>
          <FormSection title="Address Details">
            <Input
              label="Address line 1"
              error={errors.addressLine1?.message}
              {...register("addressLine1")}
            />
            <Input
              label="Address line 2"
              error={errors.addressLine2?.message}
              {...register("addressLine2")}
            />
            <Input label="City" error={errors.city?.message} {...register("city")} />
            <Input label="State" error={errors.state?.message} {...register("state")} />
            <Input label="Pincode" error={errors.pincode?.message} {...register("pincode")} />
          </FormSection>
          <div className="flex flex-col gap-3 sm:flex-row sm:justify-end">
            <Link
              to="/login"
              className="inline-flex h-10 items-center justify-center rounded-xl px-4 text-sm font-semibold text-slate-600 hover:bg-slate-100"
            >
              Already have credentials?
            </Link>
            <Button type="submit" loading={isSubmitting}>
              Submit Admission Form
            </Button>
          </div>
        </form>
      </div>
    </div>
  );
}

function FormSection({ title, children }: { title: string; children: React.ReactNode }) {
  return (
    <Card className="p-5">
      <h2 className="mb-4 text-lg font-bold">{title}</h2>
      <div className="grid gap-4 md:grid-cols-2 xl:grid-cols-3">{children}</div>
    </Card>
  );
}
