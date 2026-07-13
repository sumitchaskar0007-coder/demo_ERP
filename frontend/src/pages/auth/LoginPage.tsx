import { zodResolver } from "@hookform/resolvers/zod";
import { Eye, EyeOff, GraduationCap, LockKeyhole, Mail, ShieldCheck } from "lucide-react";
import { useState } from "react";
import { useForm } from "react-hook-form";
import { Link, Navigate, useLocation } from "react-router-dom";
import { toast } from "sonner";
import { z } from "zod";
import { Button } from "@/components/common/Button";
import { Input } from "@/components/common/Input";
import { useAuth } from "@/features/auth/authStore";
import { handleApiError } from "@/lib/handleApiError";
import { loginSchema } from "@/lib/validators";
import { APP_NAME, ROUTES, defaultRouteForRoles, isRouteAllowedForRoles } from "@/lib/constants";

type LoginForm = z.infer<typeof loginSchema>;

export function LoginPage() {
  const [showPassword, setShowPassword] = useState(false);
  const { login, isAuthenticated, user } = useAuth();
  const location = useLocation();
  const {
    register,
    handleSubmit,
    formState: { errors, isSubmitting },
  } = useForm<LoginForm>({
    resolver: zodResolver(loginSchema),
    defaultValues: { email: "admin@erp.com", password: "Admin@12345" },
  });
  if (isAuthenticated) return <Navigate to={defaultRouteForRoles(user?.roles)} replace />;
  const onSubmit = async (values: LoginForm) => {
    try {
      const user = await login(values);
      toast.success("Welcome back");
      if (user.mustChangePassword) {
        window.location.replace(ROUTES.changePassword);
        return;
      }
      const from = (location.state as { from?: string } | null)?.from;
      const target = defaultRouteForRoles(user.roles);
      window.location.replace(from && isRouteAllowedForRoles(from, user.roles) ? from : target);
    } catch (error) {
      toast.error(handleApiError(error).message);
    }
  };
  return (
    <div className="grid min-h-screen bg-white lg:grid-cols-[1.15fr_0.85fr]">
      <div className="relative hidden overflow-hidden bg-gradient-to-br from-blue-700 via-brand-700 to-indigo-900 p-12 text-white lg:flex lg:flex-col lg:justify-between">
        <div className="absolute -right-24 -top-24 h-96 w-96 rounded-full bg-white/10 blur-3xl" />
        <div className="absolute -bottom-32 -left-20 h-96 w-96 rounded-full bg-cyan-400/15 blur-3xl" />
        <div className="relative flex items-center gap-3">
          <div className="grid h-12 w-12 place-items-center rounded-2xl bg-white/15 backdrop-blur">
            <GraduationCap />
          </div>
          <div>
            <p className="text-xl font-bold">{APP_NAME}</p>
            <p className="text-xs text-blue-100">College Management Platform</p>
          </div>
        </div>
        <div className="relative max-w-xl">
          <span className="mb-6 inline-flex items-center gap-2 rounded-full bg-white/10 px-4 py-2 text-sm backdrop-blur">
            <ShieldCheck className="h-4 w-4" />
            Secure. Centralized. Efficient.
          </span>
          <h1 className="text-5xl font-bold leading-tight">
            One platform to manage your college ecosystem.
          </h1>
          <p className="mt-6 max-w-lg text-lg leading-8 text-blue-100">
            A modern, role-aware workspace for administrators and principals to manage colleges,
            departments, and teams.
          </p>
        </div>
        <p className="relative text-sm text-blue-200">
          © 2026 Jadhavr ERP. Built for modern education.
        </p>
      </div>
      <div className="flex items-center justify-center bg-slate-50 px-5 py-12">
        <div className="w-full max-w-md">
          <div className="mb-8 flex items-center gap-3 lg:hidden">
            <div className="grid h-11 w-11 place-items-center rounded-xl bg-brand-600 text-white">
              <GraduationCap />
            </div>
            <p className="text-xl font-bold">{APP_NAME}</p>
          </div>
          <div className="rounded-3xl border bg-white p-7 shadow-card sm:p-9">
            <div>
              <h2 className="text-3xl font-bold tracking-tight">Sign in to your account</h2>
              <p className="mt-2 text-sm text-slate-500">
                Enter your credentials to access the ERP portal.
              </p>
            </div>
            <form onSubmit={handleSubmit(onSubmit)} className="mt-8 space-y-5">
              <Input
                label="Email address"
                type="email"
                icon={<Mail className="h-4 w-4" />}
                error={errors.email?.message}
                {...register("email")}
              />
              <div className="relative">
                <Input
                  label="Password"
                  type={showPassword ? "text" : "password"}
                  icon={<LockKeyhole className="h-4 w-4" />}
                  error={errors.password?.message}
                  className="pr-11"
                  {...register("password")}
                />
                <button
                  type="button"
                  onClick={() => setShowPassword((value) => !value)}
                  className="absolute right-3 top-[38px] text-slate-400 hover:text-slate-600"
                  aria-label={showPassword ? "Hide password" : "Show password"}
                >
                  {showPassword ? <EyeOff className="h-5 w-5" /> : <Eye className="h-5 w-5" />}
                </button>
              </div>
              <Button type="submit" loading={isSubmitting} className="h-12 w-full">
                Sign in securely
              </Button>
              <div className="text-right">
                <Link
                  to={ROUTES.forgotPassword}
                  className="text-sm font-semibold text-blue-600 hover:text-blue-700"
                >
                  Forgot password?
                </Link>
              </div>
            </form>
            <div className="mt-6 rounded-xl border border-blue-100 bg-blue-50 p-4">
              <p className="text-xs font-bold uppercase tracking-wide text-blue-700">
                Demo Super Admin
              </p>
              <p className="mt-2 text-sm text-blue-900">admin@erp.com</p>
              <p className="text-sm text-blue-900">Admin@12345</p>
            </div>
          </div>
        </div>
      </div>
    </div>
  );
}
