import { zodResolver } from "@hookform/resolvers/zod";
import { ArrowRight, CheckCircle2, ChevronLeft, ChevronRight, Eye, EyeOff, GraduationCap, LockKeyhole, Mail, ShieldCheck } from "lucide-react";
import { useEffect, useState } from "react";
import { useForm } from "react-hook-form";
import { Link, Navigate, useLocation, useNavigate } from "react-router-dom";
import { toast } from "sonner";
import { z } from "zod";
import { Button } from "@/components/common/Button";
import { BrandLogo } from "@/components/common/BrandLogo";
import { Input } from "@/components/common/Input";
import { useAuth } from "@/features/auth/authStore";
import { handleApiError } from "@/lib/handleApiError";
import { loginSchema } from "@/lib/validators";
import { APP_NAME, ROUTES, defaultRouteForRoles, isRouteAllowedForRoles } from "@/lib/constants";

type LoginForm = z.infer<typeof loginSchema>;

const LOGIN_CATEGORY_OPTIONS = [
  { label: "Admin", value: "admin" },
  { label: "Principal", value: "principal" },
  { label: "Teacher", value: "teacher" },
  { label: "Student", value: "student" },
  { label: "Accountant", value: "accountant" },
];

const LOGIN_SLIDES = [
  { image: "/assets/login/campus.jpg", eyebrow: "Connected campuses", title: "One workspace for every college", description: "Bring admissions, academics, staff, fees and reporting together in one secure ERP." },
  { image: "/assets/login/classroom.jpg", eyebrow: "Smarter learning", title: "Built for modern education", description: "Give every role the right tools while keeping institutional data protected and organized." },
  { image: "/assets/login/library.jpg", eyebrow: "Knowledge that grows", title: "Make every decision clearer", description: "Turn daily operations into reliable insights for administrators, teachers and students." },
];

export function LoginPage() {
  const [showPassword, setShowPassword] = useState(false);
  const [loginCategory, setLoginCategory] = useState("admin");
  const [slide, setSlide] = useState(0);
  const { login, isAuthenticated, user } = useAuth();
  const navigate = useNavigate();
  const location = useLocation();
  const { register, handleSubmit, formState: { errors, isSubmitting } } = useForm<LoginForm>({
    resolver: zodResolver(loginSchema),
    defaultValues: { email: "admin@erp.com", password: "Admin@12345" },
  });

  useEffect(() => {
    const timer = window.setInterval(() => setSlide((current) => (current + 1) % LOGIN_SLIDES.length), 6000);
    return () => window.clearInterval(timer);
  }, []);

  if (isAuthenticated) return <Navigate to={defaultRouteForRoles(user?.roles)} replace />;

  const onSubmit = async (values: LoginForm) => {
    try {
      // Login category is visual guidance only. The backend determines access
      // securely from the authenticated account's assigned roles.
      const authenticatedUser = await login(values);
      toast.success(`Welcome to ${APP_NAME}`);
      if (authenticatedUser.mustChangePassword) {
        navigate(ROUTES.changePassword, { replace: true });
        return;
      }
      const from = (location.state as { from?: string } | null)?.from;
      const target = defaultRouteForRoles(authenticatedUser.roles);
      navigate(from && isRouteAllowedForRoles(from, authenticatedUser.roles) ? from : target, { replace: true });
    } catch (error) {
      toast.error(handleApiError(error).message);
    }
  };

  const activeSlide = LOGIN_SLIDES[slide];
  const changeSlide = (direction: number) => setSlide((current) => (current + direction + LOGIN_SLIDES.length) % LOGIN_SLIDES.length);

  return (
    <main className="relative min-h-screen overflow-x-hidden bg-slate-950 px-3 py-3 sm:px-6 sm:py-6 lg:grid lg:place-items-center lg:px-10 lg:py-10">
      <div className="absolute inset-0">
        {LOGIN_SLIDES.map((item, index) => <img key={item.image} src={item.image} alt="" className={`absolute inset-0 h-full w-full object-cover transition-all duration-1000 ${index===slide?"scale-100 opacity-100":"scale-105 opacity-0"}`} />)}
        <div className="absolute inset-0 bg-slate-950/65" />
        <div className="absolute inset-0 bg-[radial-gradient(circle_at_20%_20%,rgba(37,99,235,.28),transparent_32%),radial-gradient(circle_at_85%_80%,rgba(20,184,166,.22),transparent_30%)]" />
      </div>

      <section className="relative mx-auto grid w-full max-w-6xl overflow-hidden rounded-[22px] border border-white/15 bg-white/95 shadow-[0_35px_100px_rgba(2,6,23,.55)] backdrop-blur-xl sm:rounded-[28px] lg:min-h-[680px] lg:grid-cols-[0.88fr_1.12fr]">
        <aside className="relative min-h-[250px] overflow-hidden bg-gradient-to-br from-blue-700 via-blue-600 to-teal-500 p-5 text-white sm:min-h-[320px] sm:p-10 lg:flex lg:min-h-0 lg:flex-col lg:justify-between lg:p-12">
          <div className="absolute inset-0 opacity-30 [background-image:linear-gradient(135deg,rgba(255,255,255,.12)_25%,transparent_25%,transparent_50%,rgba(255,255,255,.12)_50%,rgba(255,255,255,.12)_75%,transparent_75%,transparent)] [background-size:18px_18px]" />
          <div className="absolute -bottom-28 -right-24 h-72 w-72 rounded-full bg-orange-400/50 blur-3xl" />
          <div className="absolute -left-28 top-1/3 h-64 w-64 rounded-full bg-cyan-300/25 blur-3xl" />
          <div className="relative"><div className="inline-flex rounded-xl bg-white px-3 py-2 shadow-lg sm:rounded-2xl sm:px-4"><BrandLogo className="w-36 sm:w-44" /></div><span className="mt-5 inline-flex items-center gap-2 rounded-full border border-white/25 bg-white/10 px-3 py-1.5 text-[10px] font-bold uppercase tracking-[.14em] sm:mt-8 sm:text-xs sm:tracking-[.16em]"><GraduationCap className="h-4 w-4" />{activeSlide.eyebrow}</span><h1 className="mt-3 max-w-lg text-2xl font-black leading-tight sm:mt-5 sm:text-4xl lg:text-5xl">{activeSlide.title}</h1><p className="mt-3 max-w-md text-xs leading-5 text-blue-50 sm:mt-4 sm:text-base sm:leading-6">{activeSlide.description}</p></div>
          <div className="relative mt-8 hidden lg:block"><div className="space-y-3 text-sm">{["Role-based secure access","Real-time college operations","One trusted source of data"].map((item)=><p key={item} className="flex items-center gap-2"><CheckCircle2 className="h-4 w-4 text-cyan-200" />{item}</p>)}</div><div className="mt-8 flex items-center gap-2">{LOGIN_SLIDES.map((_,index)=><button key={index} onClick={()=>setSlide(index)} aria-label={`Show slide ${index+1}`} className={`h-2 rounded-full transition-all ${index===slide?"w-8 bg-white":"w-2 bg-white/45 hover:bg-white/70"}`} />)}<div className="ml-auto flex gap-2"><button onClick={()=>changeSlide(-1)} className="grid h-9 w-9 place-items-center rounded-full border border-white/25 bg-white/10 hover:bg-white/20" aria-label="Previous slide"><ChevronLeft className="h-4 w-4" /></button><button onClick={()=>changeSlide(1)} className="grid h-9 w-9 place-items-center rounded-full border border-white/25 bg-white/10 hover:bg-white/20" aria-label="Next slide"><ChevronRight className="h-4 w-4" /></button></div></div></div>
        </aside>

        <div className="flex items-center bg-white p-5 sm:p-10 lg:p-14">
          <div className="mx-auto w-full max-w-md">
            <div className="flex items-start justify-between gap-3"><div><p className="text-[10px] font-bold uppercase tracking-[.16em] text-brand-600 sm:text-xs sm:tracking-[.18em]">Secure ERP access</p><h2 className="mt-2 text-2xl font-black tracking-tight text-slate-900 sm:text-3xl">Welcome back</h2><p className="mt-2 text-xs leading-5 text-slate-500 sm:text-sm sm:leading-6">Select your workspace role and enter your registered credentials.</p></div><div className="grid h-10 w-10 shrink-0 place-items-center rounded-xl bg-blue-50 text-brand-600 sm:h-12 sm:w-12 sm:rounded-2xl"><ShieldCheck className="h-5 w-5 sm:h-6 sm:w-6" /></div></div>

            <div className="mt-5 grid grid-cols-2 gap-2 sm:mt-7 sm:grid-cols-5">{LOGIN_CATEGORY_OPTIONS.map((option)=><button type="button" key={option.value} onClick={()=>setLoginCategory(option.value)} className={`min-w-0 rounded-xl border px-2 py-2.5 text-xs font-semibold transition ${loginCategory===option.value?"border-brand-500 bg-brand-50 text-brand-700 shadow-sm":"border-slate-200 text-slate-500 hover:border-brand-200 hover:bg-slate-50"}`}>{option.label}</button>)}</div>

            <form onSubmit={handleSubmit(onSubmit)} className="mt-5 space-y-4 sm:mt-7 sm:space-y-5">
              <Input label="Email address" type="email" placeholder="Enter your email" icon={<Mail className="h-4 w-4" />} error={errors.email?.message} className="h-12" {...register("email")} />
              <div className="relative"><Input label="Password" type={showPassword ? "text" : "password"} placeholder="Enter your password" icon={<LockKeyhole className="h-4 w-4" />} error={errors.password?.message} className="h-12 pr-11" {...register("password")} /><button type="button" onClick={() => setShowPassword((value) => !value)} className="absolute right-3 top-[39px] text-slate-400 hover:text-slate-600" aria-label={showPassword ? "Hide password" : "Show password"}>{showPassword ? <EyeOff className="h-5 w-5" /> : <Eye className="h-5 w-5" />}</button></div>
              <div className="flex flex-wrap items-center justify-between gap-3"><label className="flex items-center gap-2 text-xs text-slate-500 sm:text-sm"><input type="checkbox" className="h-4 w-4 rounded border-slate-300 text-brand-600 focus:ring-brand-500" />Remember me</label><Link to={ROUTES.forgotPassword} className="text-xs font-semibold text-brand-600 hover:text-brand-700 sm:text-sm">Forgot password?</Link></div>
              <Button type="submit" loading={isSubmitting} className="h-[52px] w-full rounded-2xl text-base">Sign in securely<ArrowRight className="h-4 w-4" /></Button>
            </form>

            <div className="mt-6 flex items-center gap-3 rounded-2xl border border-blue-100 bg-gradient-to-r from-blue-50 to-teal-50 p-4"><div className="grid h-10 w-10 shrink-0 place-items-center rounded-xl bg-white text-brand-600 shadow-sm"><LockKeyhole className="h-5 w-5" /></div><div className="min-w-0"><p className="text-xs font-bold uppercase tracking-wide text-blue-700">Demo Super Admin</p><p className="mt-1 truncate text-sm text-slate-700">admin@erp.com · Admin@12345</p></div></div>
            <p className="mt-6 text-center text-xs text-slate-400">© 2026 Jadhavr ERP · Secure access for modern education</p>
          </div>
        </div>
      </section>
    </main>
  );
}
