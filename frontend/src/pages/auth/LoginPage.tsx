import { zodResolver } from "@hookform/resolvers/zod";
import {
  ArrowRight,
  Check,
  Eye,
  EyeOff,
  GraduationCap,
  LockKeyhole,
  Mail,
  ShieldCheck,
} from "lucide-react";
import { useState } from "react";
import { useForm } from "react-hook-form";
import { Link, Navigate, useLocation, useNavigate } from "react-router-dom";
import { toast } from "sonner";
import { z } from "zod";
import { BrandLogo } from "@/components/common/BrandLogo";
import { Button } from "@/components/common/Button";
import { Input } from "@/components/common/Input";
import { useAuth } from "@/features/auth/authStore";
import { APP_NAME, ROUTES, defaultRouteForRoles, isRouteAllowedForRoles } from "@/lib/constants";
import { handleApiError } from "@/lib/handleApiError";
import { loginSchema } from "@/lib/validators";

type LoginForm = z.infer<typeof loginSchema>;

export function LoginPage() {
  const [rememberedEmail] = useState(
    () => window.localStorage.getItem("erp.rememberedEmail") || "",
  );
  const [showPassword, setShowPassword] = useState(false);
  const [rememberMe, setRememberMe] = useState(true);
  const { login, isAuthenticated, user } = useAuth();
  const navigate = useNavigate();
  const location = useLocation();
  const {
    register,
    handleSubmit,
    formState: { errors, isSubmitting },
  } = useForm<LoginForm>({
    resolver: zodResolver(loginSchema),
    defaultValues: { email: rememberedEmail, password: "" },
  });

  if (isAuthenticated)
    return (
      <Navigate
        to={user?.mustChangePassword ? ROUTES.changePassword : defaultRouteForRoles(user?.roles)}
        replace
      />
    );

  const onSubmit = async (values: LoginForm) => {
    try {
      if (rememberMe) window.localStorage.setItem("erp.rememberedEmail", values.email);
      else window.localStorage.removeItem("erp.rememberedEmail");

      const authenticatedUser = await login(values);
      toast.success(`Welcome to ${APP_NAME}`);
      if (authenticatedUser.mustChangePassword) {
        navigate(ROUTES.changePassword, { replace: true });
        return;
      }
      const from = (location.state as { from?: string } | null)?.from;
      const target = defaultRouteForRoles(authenticatedUser.roles);
      navigate(from && isRouteAllowedForRoles(from, authenticatedUser.roles) ? from : target, {
        replace: true,
      });
    } catch (error) {
      toast.error(handleApiError(error).message);
    }
  };

  return (
    <main className="min-h-[100dvh] bg-[#f3f0ff] lg:grid lg:place-items-center lg:p-6">
      <section className="relative mx-auto grid min-h-[100dvh] w-full max-w-[1280px] overflow-hidden bg-white shadow-[0_30px_90px_rgba(54,28,145,.2)] lg:h-[calc(100dvh-3rem)] lg:min-h-[660px] lg:max-h-[820px] lg:grid-cols-[1.02fr_0.98fr] lg:rounded-[30px] lg:border lg:border-violet-200/70">
        <aside className="relative hidden min-h-0 overflow-hidden bg-gradient-to-br from-[#30208a] via-[#5621d7] to-[#741df0] p-10 text-white lg:flex lg:flex-col xl:p-14">
          <div className="pointer-events-none absolute inset-0" aria-hidden="true">
            <div className="absolute -left-52 -top-60 h-[620px] w-[620px] rounded-full border-[96px] border-white/[0.045]" />
            <div className="absolute -bottom-72 -right-72 h-[700px] w-[700px] rounded-full border-[110px] border-white/[0.06]" />
            <div className="absolute right-10 top-20 h-36 w-52 opacity-20 [background-image:radial-gradient(circle,white_2px,transparent_2px)] [background-size:26px_26px]" />
            <div className="absolute bottom-24 left-10 h-28 w-28 rounded-full bg-fuchsia-300/10 blur-2xl" />
          </div>

          <div className="relative flex h-full flex-col">
            <div className="inline-flex w-fit rounded-2xl bg-white px-4 py-3 shadow-xl shadow-violet-950/20">
              <BrandLogo className="w-44 xl:w-48" />
            </div>

            <div className="my-auto py-10">
              <span className="inline-flex items-center gap-2 rounded-full border border-white/20 bg-white/10 px-4 py-2 text-xs font-bold uppercase tracking-[0.15em] backdrop-blur-sm">
                <GraduationCap className="h-4 w-4" /> Smarter learning
              </span>

              <h1 className="mt-7 max-w-lg text-4xl font-black leading-[1.08] tracking-tight xl:text-5xl">
                One workspace for a <span className="text-violet-200">smarter campus.</span>
              </h1>
              <p className="mt-5 max-w-md text-sm leading-7 text-violet-100 xl:text-base">
                Manage your everyday academic and administrative work securely from one connected
                ERP platform.
              </p>

              <div className="mt-8 grid gap-3 text-sm font-semibold text-violet-50">
                {[
                  "Student records and admissions",
                  "Academics, attendance, and fees",
                  "Secure access for every role",
                ].map((feature) => (
                  <div key={feature} className="flex items-center gap-3">
                    <span className="grid h-6 w-6 shrink-0 place-items-center rounded-full bg-white/15 ring-1 ring-white/20">
                      <Check className="h-3.5 w-3.5" />
                    </span>
                    {feature}
                  </div>
                ))}
              </div>
            </div>

            <div className="flex items-center gap-3 border-t border-white/15 pt-6 text-sm text-violet-100">
              <ShieldCheck className="h-5 w-5 text-white" />
              <span>Secure workspace for College ERP Group of Institutes</span>
            </div>
          </div>
        </aside>

        <div className="relative flex min-h-[100dvh] flex-col bg-gradient-to-br from-[#35208f] via-[#5b20e8] to-[#741df0] pb-5 lg:min-h-0 lg:bg-none lg:bg-white lg:p-8 xl:p-12">
          <div
            className="pointer-events-none absolute inset-x-0 top-0 h-56 overflow-hidden lg:hidden"
            aria-hidden="true"
          >
            <div className="absolute -right-20 -top-24 h-72 w-72 rounded-full border-[48px] border-white/[0.06]" />
            <div className="absolute -left-16 top-10 h-48 w-48 rounded-full bg-fuchsia-300/10 blur-2xl" />
          </div>

          <div className="relative flex h-[230px] shrink-0 flex-col items-center justify-center px-6 pb-12 pt-7 text-center text-white sm:h-[250px] lg:hidden">
            <div className="rounded-[22px] border border-white/20 bg-white/95 px-4 py-3 shadow-2xl shadow-violet-950/25 backdrop-blur-sm">
              <BrandLogo className="w-36 sm:w-40" />
            </div>
            <h1 className="mt-4 text-[1.75rem] font-black tracking-tight sm:text-3xl">
              Welcome back!
            </h1>
            <p className="mt-1 text-sm text-violet-100">Sign in to continue to your workspace</p>
          </div>

          <div className="relative -mt-8 mx-3 flex flex-none items-start rounded-[28px] border border-white/60 bg-white px-5 py-7 shadow-[0_24px_70px_rgba(27,8,83,.28)] sm:mx-8 sm:px-9 sm:py-9 lg:mx-0 lg:mt-0 lg:flex-1 lg:items-center lg:rounded-none lg:border-0 lg:px-4 lg:py-8 lg:shadow-none xl:px-8">
            <div className="mx-auto w-full max-w-[460px]">
              <div className="hidden items-start justify-between gap-4 lg:flex">
                <div>
                  <p className="text-xs font-black uppercase tracking-[0.18em] text-violet-600">
                    Secure ERP access
                  </p>
                  <h2 className="mt-3 text-4xl font-black tracking-tight text-slate-950">
                    Welcome back!
                  </h2>
                  <p className="mt-2 text-base text-slate-500">
                    Sign in to continue to your dashboard
                  </p>
                </div>
                <div className="grid h-14 w-14 shrink-0 place-items-center rounded-2xl border border-violet-100 bg-violet-50 text-violet-600 shadow-sm">
                  <ShieldCheck className="h-6 w-6" />
                </div>
              </div>

              <form
                onSubmit={handleSubmit(onSubmit)}
                className="space-y-4 sm:mt-6 sm:space-y-5 lg:mt-7"
              >
                <Input
                  label="Email address"
                  type="email"
                  placeholder="Enter your email"
                  autoComplete="username"
                  icon={<Mail className="h-5 w-5" />}
                  error={errors.email?.message}
                  className="h-[52px] rounded-xl border-slate-200 pl-11 shadow-sm focus:border-violet-600 focus:ring-violet-100 sm:h-14"
                  {...register("email")}
                />

                <Input
                  label="Password"
                  type={showPassword ? "text" : "password"}
                  placeholder="Enter your password"
                  autoComplete="current-password"
                  icon={<LockKeyhole className="h-5 w-5" />}
                  trailing={
                    <button
                      type="button"
                      onClick={() => setShowPassword((value) => !value)}
                      className="grid h-10 w-10 place-items-center rounded-lg text-slate-400 transition hover:bg-violet-50 hover:text-violet-700"
                      aria-label={showPassword ? "Hide password" : "Show password"}
                    >
                      {showPassword ? <EyeOff className="h-5 w-5" /> : <Eye className="h-5 w-5" />}
                    </button>
                  }
                  error={errors.password?.message}
                  className="h-[52px] rounded-xl border-slate-200 pl-11 shadow-sm focus:border-violet-600 focus:ring-violet-100 sm:h-14"
                  {...register("password")}
                />

                <div className="flex items-center justify-between gap-2">
                  <label className="flex cursor-pointer items-center gap-2 text-[13px] text-slate-600 sm:gap-2.5 sm:text-sm">
                    <span
                      className={`grid h-5 w-5 place-items-center rounded-md border transition ${
                        rememberMe
                          ? "border-violet-600 bg-violet-600 text-white"
                          : "border-slate-300 bg-white"
                      }`}
                    >
                      {rememberMe && <Check className="h-3.5 w-3.5" />}
                    </span>
                    <input
                      type="checkbox"
                      checked={rememberMe}
                      onChange={(event) => setRememberMe(event.target.checked)}
                      className="sr-only"
                    />
                    Remember me
                  </label>
                  <Link
                    to={ROUTES.forgotPassword}
                    className="whitespace-nowrap text-[13px] font-bold text-violet-600 transition hover:text-violet-800 sm:text-sm"
                  >
                    Forgot password?
                  </Link>
                </div>

                <Button
                  type="submit"
                  loading={isSubmitting}
                  className="h-[52px] w-full rounded-xl bg-gradient-to-r from-violet-700 via-violet-600 to-fuchsia-600 text-sm shadow-lg shadow-violet-500/25 hover:from-violet-800 hover:to-fuchsia-700 sm:h-14 sm:text-base"
                >
                  Sign in securely <ArrowRight className="h-5 w-5" />
                </Button>
              </form>

              <p className="mt-6 hidden items-center justify-center gap-2 text-center text-xs text-slate-400 sm:flex">
                <ShieldCheck className="h-4 w-4" /> © 2026 College ERP. All rights reserved.
              </p>
            </div>
          </div>
        </div>
      </section>
    </main>
  );
}
