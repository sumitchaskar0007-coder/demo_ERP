import { zodResolver } from "@hookform/resolvers/zod";
import { Eye, EyeOff, LockKeyhole, Mail, ShieldCheck, UserRound } from "lucide-react";
import { useState } from "react";
import { useForm } from "react-hook-form";
import { Navigate, useLocation, useNavigate } from "react-router-dom";
import { toast } from "sonner";
import { z } from "zod";
import { Button } from "@/components/common/Button";
import { BrandLogo } from "@/components/common/BrandLogo";
import { Input } from "@/components/common/Input";
import { Select } from "@/components/common/Select";
import { useAuth } from "./authStore";
import { handleApiError } from "@/lib/handleApiError";
import { loginSchema } from "@/lib/validators";
import { APP_NAME, defaultRouteForRoles, isRouteAllowedForRoles } from "@/lib/constants";

type LoginForm = z.infer<typeof loginSchema>;

const LOGIN_CATEGORY_OPTIONS = [
  { label: "Admin", value: "admin" },
  { label: "Principal", value: "principal" },
  { label: "Teacher", value: "teacher" },
  { label: "Student", value: "student" },
  { label: "Accountant", value: "accountant" },
];

export function LoginPage() {
  const [showPassword, setShowPassword] = useState(false);
  const [loginCategory, setLoginCategory] = useState("admin");
  const { login, isAuthenticated, user } = useAuth();
  const navigate = useNavigate();
  const location = useLocation();
  const { register, handleSubmit, formState: { errors, isSubmitting } } = useForm<LoginForm>({
    resolver: zodResolver(loginSchema),
    defaultValues: { email: "admin@erp.com", password: "Admin@12345" },
  });

  if (isAuthenticated) return <Navigate to={defaultRouteForRoles(user?.roles)} replace />;

  const onSubmit = async (values: LoginForm) => {
    try {
      // Login category is visual guidance only. The backend determines access
      // securely from the authenticated account's assigned roles.
      const authenticatedUser = await login(values);
      toast.success(`Welcome to ${APP_NAME}`);
      const from = (location.state as { from?: string } | null)?.from;
      const target = defaultRouteForRoles(authenticatedUser.roles);
      navigate(from && isRouteAllowedForRoles(from, authenticatedUser.roles) ? from : target, { replace: true });
    } catch (error) {
      toast.error(handleApiError(error).message);
    }
  };

  return (
    <main className="grid min-h-screen bg-white lg:grid-cols-[1.08fr_0.92fr]">
      <section className="relative hidden min-h-screen overflow-hidden bg-gradient-to-br from-blue-700 via-indigo-700 to-violet-900 px-12 py-10 text-white lg:flex lg:flex-col lg:justify-between xl:px-20">
        <div className="absolute -left-20 top-1/4 h-80 w-80 rounded-full bg-cyan-300/20 blur-3xl" />
        <div className="absolute -right-24 -top-20 h-96 w-96 rounded-full bg-fuchsia-300/20 blur-3xl" />
        <div className="absolute bottom-0 right-0 h-2/3 w-2/3 rounded-tl-[100%] bg-white/[0.05]" />
        <div className="relative inline-flex w-fit rounded-2xl bg-white px-4 py-2 shadow-xl"><BrandLogo className="w-48" /></div>
        <div className="relative max-w-2xl py-12">
          <span className="inline-flex items-center gap-2 rounded-full border border-white/20 bg-white/10 px-4 py-2 text-sm font-semibold backdrop-blur"><ShieldCheck className="h-4 w-4" />Secure. Centralized. Efficient.</span>
          <h1 className="mt-6 text-5xl font-bold leading-[1.08] tracking-tight xl:text-6xl">Smarter campuses.<br />Brighter futures.</h1>
          <p className="mt-3 text-xl font-semibold text-white">Where education meets innovation.</p>
          <p className="mt-6 max-w-xl text-lg leading-8 text-blue-100">A secure digital workspace for administrators, principals, teachers, students and college teams.</p>
          <div className="mt-10 rounded-3xl border border-white/15 bg-white/10 p-6 backdrop-blur-md"><p className="text-lg font-bold">Everything your college needs</p><p className="mt-2 text-sm leading-6 text-blue-100">Manage colleges, departments, users and admissions through role-based access.</p></div>
        </div>
        <p className="relative text-sm text-blue-200">© 2026 Jadhavr. Built for modern education by Unseen Studio.</p>
      </section>

      <section className="flex min-h-screen items-center justify-center bg-slate-50 px-5 py-10 sm:px-10">
        <div className="w-full max-w-lg">
          <div className="mb-8 flex justify-center lg:hidden"><BrandLogo className="w-52" /></div>
          <div className="rounded-[28px] border border-slate-200 bg-white p-7 shadow-[0_24px_70px_rgba(15,23,42,0.10)] sm:p-10">
            <div className="grid h-14 w-14 place-items-center rounded-2xl bg-blue-50 text-brand-600"><UserRound className="h-7 w-7" /></div>
            <h2 className="mt-5 text-3xl font-bold tracking-tight text-slate-900">Welcome back</h2>
            <p className="mt-2 text-sm leading-6 text-slate-500">Choose your category and enter your registered credentials.</p>
            <form onSubmit={handleSubmit(onSubmit)} className="mt-8 space-y-5">
              <Select label="Login category" value={loginCategory} onChange={(event) => setLoginCategory(event.target.value)} options={LOGIN_CATEGORY_OPTIONS} className="h-12" aria-label="Login category" />
              <Input label="Email address" type="email" placeholder="Enter your email" icon={<Mail className="h-4 w-4" />} error={errors.email?.message} {...register("email")} />
              <div className="relative">
                <Input label="Password" type={showPassword ? "text" : "password"} placeholder="Enter your password" icon={<LockKeyhole className="h-4 w-4" />} error={errors.password?.message} className="pr-11" {...register("password")} />
                <button type="button" onClick={() => setShowPassword((value) => !value)} className="absolute right-3 top-[38px] text-slate-400 hover:text-slate-600" aria-label={showPassword ? "Hide password" : "Show password"}>{showPassword ? <EyeOff className="h-5 w-5" /> : <Eye className="h-5 w-5" />}</button>
              </div>
              <Button type="submit" loading={isSubmitting} className="h-[52px] w-full rounded-2xl text-base">Sign in securely</Button>
            </form>
            <div className="mt-6 rounded-xl border border-blue-100 bg-blue-50 p-4"><p className="text-xs font-bold uppercase tracking-wide text-blue-700">Demo Super Admin</p><p className="mt-2 text-sm text-blue-900">admin@erp.com</p><p className="text-sm text-blue-900">Admin@12345</p></div>
          </div>
          <p className="mt-6 text-center text-xs text-slate-400">Secure access powered by Jadhavr ERP</p>
        </div>
      </section>
    </main>
  );
}
