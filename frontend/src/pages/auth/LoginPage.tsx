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
    <main className="min-h-[100dvh] bg-[#eeebff] lg:grid lg:place-items-center lg:p-6">
      <section className="relative mx-auto grid min-h-[100dvh] w-full max-w-[1536px] overflow-hidden bg-gradient-to-br from-[#35208f] via-[#5b20e8] to-[#35108e] shadow-[0_32px_90px_rgba(54,28,145,.28)] lg:min-h-[calc(100dvh-3rem)] lg:grid-cols-[1.08fr_0.92fr] lg:rounded-[30px] lg:border lg:border-violet-300/30">
        <div className="pointer-events-none absolute inset-0 overflow-hidden" aria-hidden="true">
          <div className="absolute -left-48 -top-56 h-[620px] w-[620px] rounded-full border-[100px] border-white/[0.035]" />
          <div className="absolute -bottom-52 left-[28%] h-[780px] w-[780px] rounded-full border-[110px] border-white/[0.05]" />
          <div className="absolute right-[44%] top-16 h-24 w-24 rounded-full bg-violet-300/10 blur-sm" />
          <div className="absolute right-[45%] top-24 h-36 w-52 opacity-20 [background-image:radial-gradient(circle,white_2px,transparent_2px)] [background-size:26px_26px]" />
        </div>

        <aside className="relative z-10 hidden min-h-0 flex-col justify-between p-10 text-white lg:flex xl:p-14 2xl:p-16">
          <div>
            <div className="inline-flex rounded-2xl bg-white px-4 py-3 shadow-xl shadow-violet-950/20">
              <BrandLogo className="w-44 xl:w-48" />
            </div>

            <span className="mt-10 inline-flex items-center gap-2 rounded-full border border-white/20 bg-white/10 px-4 py-2 text-xs font-bold uppercase tracking-[0.15em] backdrop-blur-sm">
              <GraduationCap className="h-4 w-4" /> Smarter learning
            </span>

            <h1 className="mt-7 max-w-xl text-5xl font-black leading-[1.05] tracking-tight 2xl:text-6xl">
              Empowering Education with <span className="text-violet-200">Smart ERP</span>
            </h1>
          </div>
        </aside>

        <div className="relative z-10 flex min-h-[100dvh] flex-col pb-7 lg:min-h-0 lg:p-6 xl:p-8">
          <div className="flex h-[320px] shrink-0 flex-col items-center justify-center px-6 pb-16 pt-8 text-center text-white sm:h-[360px] lg:hidden">
            <div className="rounded-[22px] border border-white/20 bg-white/95 px-4 py-3 shadow-2xl shadow-violet-950/25 backdrop-blur-sm">
              <BrandLogo className="w-36 sm:w-44" />
            </div>
            <h1 className="mt-5 text-[1.9rem] font-black tracking-tight sm:text-3xl">
              Welcome back!
            </h1>
            <p className="mt-1 text-sm text-violet-100 sm:text-lg">Sign in to continue</p>
          </div>

          <div className="-mt-12 mx-4 flex flex-none items-start rounded-[30px] border border-white/50 bg-white px-5 py-7 shadow-[0_24px_70px_rgba(27,8,83,.28)] sm:mx-8 sm:px-10 sm:py-10 lg:mx-0 lg:mt-0 lg:flex-1 lg:items-center lg:rounded-[26px] lg:px-10 lg:py-10 lg:shadow-[0_24px_70px_rgba(24,10,79,.22)] xl:px-14 2xl:px-16">
            <div className="mx-auto w-full max-w-[590px]">
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
                className="space-y-4 sm:mt-6 sm:space-y-5 lg:mt-8"
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

                <div className="relative">
                  <Input
                    label="Password"
                    type={showPassword ? "text" : "password"}
                    placeholder="Enter your password"
                    autoComplete="current-password"
                    icon={<LockKeyhole className="h-5 w-5" />}
                    error={errors.password?.message}
                    className="h-[52px] rounded-xl border-slate-200 pl-11 pr-12 shadow-sm focus:border-violet-600 focus:ring-violet-100 sm:h-14"
                    {...register("password")}
                  />
                  <button
                    type="button"
                    onClick={() => setShowPassword((value) => !value)}
                    className="absolute right-3 top-[41px] grid h-8 w-8 place-items-center rounded-lg text-slate-400 transition hover:bg-violet-50 hover:text-violet-700 sm:top-[43px]"
                    aria-label={showPassword ? "Hide password" : "Show password"}
                  >
                    {showPassword ? <EyeOff className="h-5 w-5" /> : <Eye className="h-5 w-5" />}
                  </button>
                </div>

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
                <ShieldCheck className="h-4 w-4" /> © 2026 Jadhavar ERP. All rights reserved.
              </p>
            </div>
          </div>
        </div>
      </section>
    </main>
  );
}
