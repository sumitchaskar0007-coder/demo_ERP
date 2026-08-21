import { zodResolver } from "@hookform/resolvers/zod";
import {
  AlertCircle,
  ArrowRight,
  BadgeCheck,
  BookOpen,
  CalendarDays,
  Check,
  CheckCircle2,
  GraduationCap,
  HelpCircle,
  LockKeyhole,
  LogIn,
  Mail,
  MapPin,
  Phone,
  ShieldCheck,
  Sparkles,
  UserRound,
  type LucideIcon,
} from "lucide-react";
import { useEffect, useState } from "react";
import { useForm, type FieldErrors } from "react-hook-form";
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

const publicAdmissionFields = new Set<keyof FormValues>([
  "departmentId",
  "studentCategory",
  "customCategoryName",
  "firstName",
  "middleName",
  "lastName",
  "email",
  "phone",
  "dateOfBirth",
  "gender",
]);

function focusAdmissionField(field: string) {
  const element = document.getElementsByName(field)[0] ?? document.getElementById(field);
  if (!(element instanceof HTMLElement)) return;

  element.scrollIntoView?.({ behavior: "smooth", block: "center" });
  if (element instanceof HTMLSelectElement && element.offsetParent === null) {
    element.parentElement?.querySelector<HTMLButtonElement>("button")?.focus();
    return;
  }
  element.focus();
}

export function PublicAdmissionPage() {
  const { collegeCode = "" } = useParams();
  const [info, setInfo] = useState<PublicAdmissionInfoResponse | null>(null);
  const [result, setResult] = useState<SubmitAdmissionResponse | null>(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState("");
  const [submitError, setSubmitError] = useState("");
  const [categoryOptions, setCategoryOptions] = useState<
    Array<{
      category: FormValues["studentCategory"];
      customCategoryName?: string | null;
      label: string;
    }>
  >([]);
  const {
    register,
    getValues,
    setValue,
    watch,
    handleSubmit,
    clearErrors,
    setError: setFieldError,
    formState: { errors, isSubmitting },
  } = useForm<FormValues>({
    resolver: zodResolver(publicAdmissionSchema),
    defaultValues: {
      gender: "",
      studentCategory: "OPEN",
      middleName: "",
      addressLine1: "",
      addressLine2: "",
      city: "",
      state: "",
      pincode: "",
    },
  });
  const departmentId = watch("departmentId");
  const studentCategory = watch("studentCategory");
  const gender = watch("gender");

  useEffect(() => {
    if (!departmentId) {
      setCategoryOptions([]);
      setValue("customCategoryName", "");
      return;
    }
    let active = true;
    api
      .getPublicAdmissionCategories(collegeCode, Number(departmentId), {
        gender: gender || undefined,
        academicYear: info?.academicYear,
      })
      .then((options) => {
        if (!active) return;
        setCategoryOptions(options);

        const selectedCustomCategory = getValues("customCategoryName")?.trim();
        if (
          getValues("studentCategory") === "OTHER" &&
          selectedCustomCategory &&
          !options.some(
            (option) =>
              option.category === "OTHER" &&
              option.customCategoryName?.toUpperCase() === selectedCustomCategory.toUpperCase(),
          )
        ) {
          setValue("customCategoryName", "");
          setFieldError("customCategoryName", {
            type: "manual",
            message: "Selected Other category is not available for this department and gender",
          });
        }
      })
      .catch(() => {
        // Keep the last successful options and the user's selection on a transient refresh error.
      });
    return () => {
      active = false;
    };
  }, [collegeCode, departmentId, gender, getValues, info?.academicYear, setFieldError, setValue]);

  useEffect(() => {
    setLoading(true);
    api
      .getPublicAdmissionInfo(collegeCode)
      .then(setInfo)
      .catch((err) => setError(handleApiError(err).message))
      .finally(() => setLoading(false));
  }, [collegeCode]);

  const onSubmit = async (values: FormValues) => {
    setSubmitError("");
    clearErrors();
    const categoryConfigured = categoryOptions.some(
      (option) =>
        option.category === values.studentCategory &&
        (values.studentCategory !== "OTHER" ||
          option.customCategoryName?.toUpperCase() ===
            values.customCategoryName?.trim().toUpperCase()),
    );
    if (!categoryConfigured) {
      const message = "Select an active fee category configured for this department and gender";
      setFieldError("studentCategory", { type: "manual", message });
      setSubmitError("Please correct the highlighted field below.");
      focusAdmissionField("studentCategory");
      toast.error(message);
      return;
    }
    try {
      const submitted = await api.submitAdmission(collegeCode, values);
      setResult(submitted);
      toast.success("Registration completed successfully");
    } catch (err) {
      const apiError = handleApiError(err);
      let firstInvalidField: string | undefined;
      Object.entries(apiError.fieldErrors).forEach(([field, message]) => {
        if (!publicAdmissionFields.has(field as keyof FormValues)) return;
        firstInvalidField ??= field;
        setFieldError(field as keyof FormValues, { type: "server", message });
      });

      setSubmitError(
        firstInvalidField ? "Please correct the highlighted field below." : apiError.message,
      );
      if (firstInvalidField) focusAdmissionField(firstInvalidField);
      toast.error(apiError.message);
    }
  };

  const onInvalid = (formErrors: FieldErrors<FormValues>) => {
    const firstInvalidField = Object.keys(formErrors)[0];
    setSubmitError("Please complete the highlighted required fields.");
    if (firstInvalidField) focusAdmissionField(firstInvalidField);
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
      <div className="relative grid min-h-screen place-items-center overflow-hidden bg-slate-50 px-4 py-10">
        <div className="pointer-events-none absolute -left-32 -top-32 h-96 w-96 rounded-full bg-brand-100/60 blur-3xl" />
        <div className="pointer-events-none absolute -bottom-40 -right-24 h-96 w-96 rounded-full bg-blue-100/70 blur-3xl" />
        <Card className="relative mx-auto w-full max-w-2xl overflow-hidden border-emerald-100">
          <div className="bg-gradient-to-r from-emerald-600 to-teal-600 px-6 py-8 text-center text-white sm:px-10">
            <div className="mx-auto grid h-16 w-16 place-items-center rounded-full bg-white/15 ring-8 ring-white/10">
              <CheckCircle2 className="h-8 w-8" />
            </div>
            <h1 className="mt-5 text-2xl font-bold sm:text-3xl">Student account created</h1>
            <p className="mx-auto mt-2 max-w-lg text-sm text-emerald-50">
              Your first registration step is complete. Check your email for the one-time login
              password.
            </p>
          </div>
          <div className="p-6 sm:p-8">
            <div className="grid gap-3 rounded-2xl border border-slate-200 bg-slate-50 p-5 text-sm sm:grid-cols-2">
              <Credential label="Application reference" value={result.admissionReferenceNumber} />
              <Credential label="Admission number" value={result.admissionNumber} />
              <Credential label="Login email" value={result.email} />
            </div>
            <div className="mt-5 flex gap-3 rounded-xl border border-blue-100 bg-blue-50 p-4">
              <ShieldCheck className="mt-0.5 h-5 w-5 shrink-0 text-blue-600" />
              <div>
                <p className="text-sm font-semibold text-blue-950">What happens next?</p>
                <p className="mt-1 text-xs leading-5 text-blue-800">
                  Open the account email, sign in with the one-time password, create a private
                  password, then complete the detailed admission form and required documents.
                </p>
              </div>
            </div>
            <Button
              className="mt-6 h-12 w-full text-base sm:h-12"
              onClick={() => window.location.assign("/login")}
            >
              Continue to student login
              <ArrowRight className="h-4 w-4" />
            </Button>
          </div>
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
    <div className="relative min-h-screen overflow-hidden bg-[#f5f8fc]">
      <div className="pointer-events-none absolute -left-40 top-24 h-96 w-96 rounded-full bg-brand-100/50 blur-3xl" />
      <div className="pointer-events-none absolute -right-36 top-0 h-96 w-96 rounded-full bg-blue-100/60 blur-3xl" />

      <header className="relative border-b border-slate-200/80 bg-white/90 backdrop-blur">
        <div className="mx-auto flex max-w-7xl flex-col gap-5 px-4 py-5 sm:px-6 lg:flex-row lg:items-center lg:justify-between lg:px-8">
          <div className="flex min-w-0 items-center gap-4">
            <div className="grid h-14 w-14 shrink-0 place-items-center overflow-hidden rounded-2xl border border-brand-100 bg-brand-50 text-brand-700 shadow-sm">
              {info.logoUrl ? (
                <img
                  src={info.logoUrl}
                  alt={`${info.collegeName} logo`}
                  className="h-11 w-11 object-contain"
                />
              ) : (
                <GraduationCap className="h-7 w-7" />
              )}
            </div>
            <div className="min-w-0">
              <p className="text-xs font-bold uppercase tracking-[0.16em] text-brand-600">
                Online Admissions
              </p>
              <h1 className="truncate text-xl font-bold text-slate-950 sm:text-2xl">
                {info.collegeName}
              </h1>
              <p className="mt-0.5 flex items-center gap-1.5 truncate text-xs text-slate-500 sm:text-sm">
                <MapPin className="h-3.5 w-3.5 shrink-0" />
                {[info.address, info.city, info.state].filter(Boolean).join(", ")}
              </p>
            </div>
          </div>
          <div className="flex flex-wrap items-center gap-2">
            <div className="inline-flex items-center gap-2 rounded-xl border border-brand-100 bg-brand-50 px-3.5 py-2 text-sm font-semibold text-brand-700">
              <CalendarDays className="h-4 w-4" />
              Academic Year {info.academicYear}
            </div>
            <Link
              to="/login"
              className="inline-flex items-center gap-2 rounded-xl border border-slate-200 bg-white px-3.5 py-2 text-sm font-semibold text-slate-700 transition hover:border-brand-200 hover:text-brand-700"
            >
              <LogIn className="h-4 w-4" />
              Student login
            </Link>
          </div>
        </div>
      </header>

      <main className="relative mx-auto max-w-7xl px-4 py-8 sm:px-6 lg:px-8 lg:py-10">
        <div className="mb-7 max-w-3xl">
          <div className="mb-3 inline-flex items-center gap-2 rounded-full border border-brand-100 bg-white px-3 py-1.5 text-xs font-bold text-brand-700 shadow-sm">
            <Sparkles className="h-3.5 w-3.5" />
            Start your application
          </div>
          <h2 className="text-3xl font-bold tracking-tight text-slate-950 sm:text-4xl">
            Create your student admission account
          </h2>
          <p className="mt-3 max-w-2xl text-sm leading-6 text-slate-600 sm:text-base">
            Enter your basic details to receive login credentials. You can complete the full
            application and upload documents after signing in.
          </p>
        </div>

        <div className="mb-7 grid max-w-3xl grid-cols-3">
          <ProgressStep number="1" label="Create account" active />
          <ProgressStep number="2" label="Complete application" />
          <ProgressStep number="3" label="Verification" last />
        </div>

        <div className="grid items-start gap-6 lg:grid-cols-[minmax(0,1fr)_330px]">
          <form
            noValidate
            onChange={() => setSubmitError("")}
            onSubmit={handleSubmit(onSubmit, onInvalid)}
          >
            {submitError && (
              <div
                role="alert"
                className="mb-4 flex items-start gap-3 rounded-xl border border-red-200 bg-red-50 px-4 py-3 text-sm font-medium text-red-800"
              >
                <AlertCircle className="mt-0.5 h-4 w-4 shrink-0" />
                <span>{submitError}</span>
              </div>
            )}
            <Card className="overflow-hidden">
              <FormSection
                step="01"
                icon={BookOpen}
                title="Choose your department"
                description="Select the programme or department you want to apply for."
              >
                <div className="md:col-span-2">
                  <Select
                    label="Department"
                    options={departments}
                    error={errors.departmentId?.message}
                    {...register("departmentId")}
                  />
                </div>
              </FormSection>

              <div className="border-t border-slate-100" />

              <FormSection
                step="02"
                icon={UserRound}
                title="Student basic details"
                description="Use the same name and contact details as your official documents."
              >
                <Select
                  id="student-category"
                  name="studentCategory"
                  label="Student category"
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
                  value={studentCategory}
                  onChange={(event) => {
                    setValue(
                      "studentCategory",
                      event.target.value as FormValues["studentCategory"],
                      { shouldValidate: true },
                    );
                    if (event.target.value !== "OTHER") {
                      setValue("customCategoryName", "", { shouldValidate: true });
                    }
                  }}
                  error={errors.studentCategory?.message}
                />
                {studentCategory === "OTHER" && (
                  <div className="space-y-1.5">
                    <Select
                      id="custom-category"
                      name="customCategoryName"
                      label="Other category"
                      options={[
                        { label: "Select category", value: "" },
                        ...categoryOptions
                          .filter(
                            (option) => option.category === "OTHER" && option.customCategoryName,
                          )
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
                      value={watch("customCategoryName") ?? ""}
                      onChange={(event) =>
                        setValue("customCategoryName", event.target.value, {
                          shouldValidate: true,
                        })
                      }
                      error={errors.customCategoryName?.message}
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
                <Input
                  label="First name"
                  placeholder="Enter first name"
                  error={errors.firstName?.message}
                  {...register("firstName")}
                />
                <Input
                  label="Middle name"
                  placeholder="Enter middle name"
                  error={errors.middleName?.message}
                  {...register("middleName")}
                />
                <Input
                  label="Last name"
                  placeholder="Enter last name"
                  error={errors.lastName?.message}
                  {...register("lastName")}
                />
                <Input
                  label="Email address"
                  type="email"
                  placeholder="student@example.com"
                  error={errors.email?.message}
                  {...register("email")}
                />
                <Input
                  label="Mobile number"
                  type="tel"
                  inputMode="tel"
                  placeholder="10-digit mobile number"
                  error={errors.phone?.message}
                  {...register("phone")}
                />
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

              <div className="border-t border-slate-100 bg-slate-50/80 px-5 py-5 sm:px-7">
                <div className="flex flex-col gap-4 sm:flex-row sm:items-center sm:justify-between">
                  <div className="flex items-start gap-2 text-xs leading-5 text-slate-500">
                    <LockKeyhole className="mt-0.5 h-4 w-4 shrink-0 text-emerald-600" />
                    <span>
                      Your information is encrypted and used only for admission processing.
                    </span>
                  </div>
                  <Button
                    type="submit"
                    loading={isSubmitting}
                    className="h-12 shrink-0 px-6 text-base sm:h-12"
                  >
                    Create student login
                    <ArrowRight className="h-4 w-4" />
                  </Button>
                </div>
              </div>
            </Card>
          </form>

          <aside className="space-y-4 lg:sticky lg:top-6">
            <Card className="overflow-hidden border-brand-100">
              <div className="bg-brand-600 px-5 py-4 text-white">
                <div className="flex items-center gap-2">
                  <BadgeCheck className="h-5 w-5" />
                  <h3 className="font-bold">Simple admission process</h3>
                </div>
                <p className="mt-1 text-xs text-blue-100">Complete each step at your own pace.</p>
              </div>
              <div className="space-y-4 p-5">
                <NextStep
                  icon={Check}
                  title="Create your account"
                  text="Provide basic personal and course details."
                  current
                />
                <NextStep
                  icon={BookOpen}
                  title="Complete application"
                  text="Add education details and upload documents."
                />
                <NextStep
                  icon={ShieldCheck}
                  title="College verification"
                  text="Track review and admission status online."
                />
              </div>
            </Card>

            <Card className="p-5">
              <div className="flex items-center gap-2">
                <HelpCircle className="h-5 w-5 text-brand-600" />
                <h3 className="font-bold text-slate-900">Need help?</h3>
              </div>
              <p className="mt-2 text-sm leading-6 text-slate-500">
                Contact the college admission office for assistance with registration.
              </p>
              <div className="mt-4 space-y-2">
                {info.contactPhone && (
                  <a
                    href={`tel:${info.contactPhone}`}
                    className="flex items-center gap-2 text-sm font-semibold text-slate-700 hover:text-brand-700"
                  >
                    <Phone className="h-4 w-4 text-brand-600" />
                    {info.contactPhone}
                  </a>
                )}
                {info.contactEmail && (
                  <a
                    href={`mailto:${info.contactEmail}`}
                    className="flex items-center gap-2 break-all text-sm font-semibold text-slate-700 hover:text-brand-700"
                  >
                    <Mail className="h-4 w-4 shrink-0 text-brand-600" />
                    {info.contactEmail}
                  </a>
                )}
                {!info.contactPhone && !info.contactEmail && (
                  <p className="text-xs text-slate-400">
                    Contact details will be provided by the college.
                  </p>
                )}
              </div>
            </Card>

            <div className="flex items-center justify-center gap-2 px-3 text-center text-xs text-slate-400">
              <ShieldCheck className="h-4 w-4 text-emerald-500" />
              Secure admission portal powered by College ERP
            </div>
          </aside>
        </div>
      </main>
    </div>
  );
}

function FormSection({
  step,
  icon: Icon,
  title,
  description,
  children,
}: {
  step: string;
  icon: LucideIcon;
  title: string;
  description: string;
  children: React.ReactNode;
}) {
  return (
    <section className="p-5 sm:p-7">
      <div className="mb-6 flex items-start gap-3">
        <div className="grid h-11 w-11 shrink-0 place-items-center rounded-xl bg-brand-50 text-brand-700 ring-1 ring-brand-100">
          <Icon className="h-5 w-5" />
        </div>
        <div>
          <p className="text-[11px] font-bold uppercase tracking-[0.16em] text-brand-600">
            Step {step}
          </p>
          <h3 className="mt-0.5 text-lg font-bold text-slate-950">{title}</h3>
          <p className="mt-1 text-sm text-slate-500">{description}</p>
        </div>
      </div>
      <div className="grid gap-x-4 gap-y-5 md:grid-cols-2">{children}</div>
    </section>
  );
}

function ProgressStep({
  number,
  label,
  active,
  last,
}: {
  number: string;
  label: string;
  active?: boolean;
  last?: boolean;
}) {
  return (
    <div className="relative flex flex-col items-center text-center">
      {!last && (
        <div
          className={`absolute left-1/2 top-5 h-px w-full ${
            active ? "bg-brand-300" : "bg-slate-200"
          }`}
        />
      )}
      <div
        className={`relative z-10 grid h-10 w-10 place-items-center rounded-full border-4 border-[#f5f8fc] text-sm font-bold ${
          active
            ? "bg-brand-600 text-white shadow-md shadow-brand-600/20"
            : "bg-slate-200 text-slate-500"
        }`}
      >
        {number}
      </div>
      <p className={`mt-2 text-xs font-semibold ${active ? "text-brand-700" : "text-slate-500"}`}>
        {label}
      </p>
    </div>
  );
}

function NextStep({
  icon: Icon,
  title,
  text,
  current,
}: {
  icon: LucideIcon;
  title: string;
  text: string;
  current?: boolean;
}) {
  return (
    <div className="flex gap-3">
      <div
        className={`grid h-8 w-8 shrink-0 place-items-center rounded-full ${
          current ? "bg-brand-600 text-white" : "bg-slate-100 text-slate-500"
        }`}
      >
        <Icon className="h-4 w-4" />
      </div>
      <div>
        <p className="text-sm font-bold text-slate-800">{title}</p>
        <p className="mt-0.5 text-xs leading-5 text-slate-500">{text}</p>
      </div>
    </div>
  );
}

function Credential({ label, value, accent }: { label: string; value: string; accent?: boolean }) {
  return (
    <div className="rounded-xl bg-white p-3 ring-1 ring-slate-200">
      <p className="text-[11px] font-bold uppercase tracking-wide text-slate-400">{label}</p>
      <p
        className={`mt-1 break-all text-sm font-semibold ${
          accent ? "font-mono text-brand-700" : "text-slate-800"
        }`}
      >
        {value}
      </p>
    </div>
  );
}
