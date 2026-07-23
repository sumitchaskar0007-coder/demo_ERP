import { zodResolver } from "@hookform/resolvers/zod";
import {
  CheckCircle2,
  Circle,
  Eye,
  EyeOff,
  KeyRound,
  LockKeyhole,
  ShieldCheck,
} from "lucide-react";
import { useState } from "react";
import { useForm } from "react-hook-form";
import { useNavigate } from "react-router-dom";
import { toast } from "sonner";
import { z } from "zod";
import { Button } from "@/components/common/Button";
import { BrandLogo } from "@/components/common/BrandLogo";
import { Input } from "@/components/common/Input";
import { defaultRouteForRoles } from "@/lib/constants";
import { handleApiError } from "@/lib/handleApiError";
import * as api from "@/features/auth/api";
import { useAuth } from "@/features/auth/authStore";
import { ROLES } from "@/lib/constants";

const schema = z
  .object({
    currentPassword: z.string().min(1, "Current password is required"),
    newPassword: z
      .string()
      .min(8)
      .max(100)
      .regex(
        /^(?=.*[a-z])(?=.*[A-Z])(?=.*\d)(?=.*[@$!%*?&.#_-])[A-Za-z\d@$!%*?&.#_-]{8,100}$/,
        "Include uppercase, lowercase, number, and special character",
      ),
    confirmPassword: z.string().min(1, "Confirm your password"),
  })
  .refine((values) => values.newPassword === values.confirmPassword, {
    path: ["confirmPassword"],
    message: "Passwords do not match",
  });

type FormValues = z.infer<typeof schema>;

export function ChangePasswordPage() {
  const { user, refreshProfile } = useAuth();
  const isStudent = Boolean(user?.roles.includes(ROLES.STUDENT));
  const navigate = useNavigate();
  const [showPasswords, setShowPasswords] = useState(false);
  const {
    register,
    handleSubmit,
    watch,
    formState: { errors, isSubmitting },
  } = useForm<FormValues>({ resolver: zodResolver(schema) });
  const newPassword = watch("newPassword", "");
  const requirements = [
    [newPassword.length >= 8, "At least 8 characters"],
    [/[A-Z]/.test(newPassword) && /[a-z]/.test(newPassword), "Uppercase and lowercase"],
    [/\d/.test(newPassword), "At least one number"],
    [/@|\$|!|%|\*|\?|&|\.|#|_|-/.test(newPassword), "At least one special character"],
  ] as const;
  const submit = async ({ currentPassword, newPassword, confirmPassword }: FormValues) => {
    try {
      await api.changePassword({ currentPassword, newPassword, confirmPassword });
      await refreshProfile();
      toast.success("Password changed successfully");
      navigate(defaultRouteForRoles(user?.roles), { replace: true });
    } catch (error) {
      toast.error(handleApiError(error).message);
    }
  };
  return (
    <main className="relative grid min-h-screen place-items-center overflow-hidden bg-slate-50 p-4 sm:p-6">
      <div className="pointer-events-none absolute inset-0" aria-hidden="true">
        <div className="absolute -left-40 -top-40 h-96 w-96 rounded-full bg-blue-200/35 blur-3xl" />
        <div className="absolute -bottom-48 -right-32 h-[30rem] w-[30rem] rounded-full bg-indigo-200/30 blur-3xl" />
        <div className="absolute inset-0 opacity-[0.025] [background-image:radial-gradient(#2563eb_1px,transparent_1px)] [background-size:24px_24px]" />
      </div>

      <section className="relative grid w-full max-w-5xl overflow-hidden rounded-[28px] border border-slate-200/80 bg-white shadow-[0_30px_80px_rgba(15,23,42,0.12)] lg:grid-cols-[0.9fr_1.1fr]">
        <aside className="relative overflow-hidden bg-gradient-to-br from-blue-700 via-brand-600 to-indigo-700 p-8 text-white sm:p-10 lg:p-12">
          <div className="absolute -right-24 -top-24 h-72 w-72 rounded-full border-[42px] border-white/10" />
          <div className="absolute -bottom-20 -left-16 h-56 w-56 rounded-full bg-white/5" />
          <div className="relative flex h-full flex-col">
            <div className="w-fit rounded-2xl bg-white px-4 py-3 shadow-lg shadow-blue-950/20">
              <BrandLogo className="w-40" />
            </div>

            <div className="my-auto py-12">
              <span className="grid h-14 w-14 place-items-center rounded-2xl bg-white/15 ring-1 ring-white/20 backdrop-blur">
                <ShieldCheck className="h-7 w-7" />
              </span>
              <p className="mt-7 text-xs font-bold uppercase tracking-[0.2em] text-blue-100">
                Account protection
              </p>
              <h1 className="mt-3 text-3xl font-bold leading-tight sm:text-4xl">
                {isStudent ? "Secure your student account" : "Secure your staff account"}
              </h1>
              <p className="mt-4 max-w-sm text-sm leading-6 text-blue-100">
                {isStudent
                  ? "Your admission is approved. Create a private password before entering your student dashboard."
                  : "Your staff account is ready. Replace the temporary phone-number password before entering your dashboard."}
              </p>

              <div className="mt-8 space-y-4 text-sm text-blue-50">
                {[
                  "Your temporary phone-number password will be replaced",
                  "Your dashboard stays locked until this step is complete",
                  "Your new password is securely encrypted",
                ].map((item) => (
                  <div key={item} className="flex items-start gap-3">
                    <CheckCircle2 className="mt-0.5 h-4 w-4 shrink-0 text-cyan-200" />
                    <span>{item}</span>
                  </div>
                ))}
              </div>
            </div>

            <div className="rounded-2xl border border-white/15 bg-white/10 p-4 backdrop-blur">
              <p className="text-xs uppercase tracking-wider text-blue-200">Signed in as</p>
              <p className="mt-1 font-bold">
                {user?.fullName || (isStudent ? "Student" : "Staff member")}
              </p>
              <p className="mt-0.5 truncate text-xs text-blue-100">{user?.email}</p>
            </div>
          </div>
        </aside>

        <div className="p-7 sm:p-10 lg:p-12">
          <div className="mx-auto max-w-md">
            <div className="inline-flex rounded-2xl bg-brand-50 p-3 text-brand-700 ring-1 ring-brand-100">
              <LockKeyhole className="h-6 w-6" />
            </div>
            <p className="mt-6 text-xs font-bold uppercase tracking-[0.18em] text-brand-600">
              Required security step
            </p>
            <h2 className="mt-2 text-3xl font-bold tracking-tight text-slate-950">
              Change your password
            </h2>
            <p className="mt-3 text-sm leading-6 text-slate-500">
              Set a strong password you will use for all future logins.
            </p>

            <form onSubmit={handleSubmit(submit)} className="mt-8 space-y-5">
              <Input
                label="Current temporary password"
                type={showPasswords ? "text" : "password"}
                autoComplete="current-password"
                icon={<KeyRound className="h-4 w-4" />}
                error={errors.currentPassword?.message}
                {...register("currentPassword")}
              />
              <Input
                label="New password"
                type={showPasswords ? "text" : "password"}
                autoComplete="new-password"
                icon={<LockKeyhole className="h-4 w-4" />}
                error={errors.newPassword?.message}
                {...register("newPassword")}
              />

              <div className="grid gap-2 rounded-xl border border-slate-200 bg-slate-50 p-3 sm:grid-cols-2">
                {requirements.map(([met, label]) => {
                  const Icon = met ? CheckCircle2 : Circle;
                  return (
                    <div
                      key={label}
                      className={`flex items-center gap-2 text-xs font-medium ${met ? "text-emerald-700" : "text-slate-500"}`}
                    >
                      <Icon className="h-3.5 w-3.5 shrink-0" />
                      {label}
                    </div>
                  );
                })}
              </div>

              <Input
                label="Confirm new password"
                type={showPasswords ? "text" : "password"}
                autoComplete="new-password"
                icon={<LockKeyhole className="h-4 w-4" />}
                error={errors.confirmPassword?.message}
                {...register("confirmPassword")}
              />

              <button
                type="button"
                onClick={() => setShowPasswords((visible) => !visible)}
                className="inline-flex items-center gap-2 text-sm font-semibold text-slate-600 transition hover:text-brand-700"
              >
                {showPasswords ? <EyeOff className="h-4 w-4" /> : <Eye className="h-4 w-4" />}
                {showPasswords ? "Hide passwords" : "Show passwords"}
              </button>

              <Button type="submit" loading={isSubmitting} className="h-12 w-full text-base">
                Change password and open dashboard
              </Button>
              <p className="text-center text-xs leading-5 text-slate-400">
                For your protection, this step cannot be skipped.
              </p>
            </form>
          </div>
        </div>
      </section>
    </main>
  );
}
