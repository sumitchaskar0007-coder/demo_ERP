import { zodResolver } from "@hookform/resolvers/zod";
import { ArrowLeft, ShieldCheck, UserPlus } from "lucide-react";
import { useEffect, useState } from "react";
import { useForm } from "react-hook-form";
import { Link, useNavigate } from "react-router-dom";
import { toast } from "sonner";
import { z } from "zod";
import { Button } from "@/components/common/Button";
import { Card } from "@/components/common/Card";
import { Input } from "@/components/common/Input";
import { Select } from "@/components/common/Select";
import { getActiveColleges } from "@/features/colleges/api";
import type { College } from "@/features/colleges/types";
import { handleApiError } from "@/lib/handleApiError";
import { createPrincipalSchema } from "@/lib/validators";
import { createPrincipal } from "./api";

type FormValues = z.infer<typeof createPrincipalSchema>;

export function CreatePrincipalPage() {
  const [colleges, setColleges] = useState<College[]>([]);
  const navigate = useNavigate();
  const { register, handleSubmit, formState: { errors, isSubmitting } } = useForm<FormValues>({ resolver: zodResolver(createPrincipalSchema), defaultValues: { collegeId: 0, fullName: "", email: "", phone: "" } });
  useEffect(() => { getActiveColleges().then(setColleges).catch((error) => toast.error(handleApiError(error).message)); }, []);
  const submit = async (values: FormValues) => { try { const response = await createPrincipal(values); toast.success(`${response.message}. Account setup email has been queued.`); navigate(`/users/${response.data.id}`); } catch (error) { toast.error(handleApiError(error).message); } };
  const options = [{ label: "Select an active college", value: 0 }, ...colleges.map((college) => ({ label: `${college.name} (${college.code})`, value: college.id }))];
  return <div className="page-container"><Link to="/users"><Button variant="ghost"><ArrowLeft className="h-4 w-4" />Back to users</Button></Link><div className="mt-4 grid gap-6 xl:grid-cols-[1fr_340px]"><Card className="p-6 sm:p-8"><div className="mb-7"><div className="mb-4 inline-flex rounded-2xl bg-brand-50 p-3 text-brand-600"><UserPlus className="h-6 w-6" /></div><h1 className="text-2xl font-bold">Create Principal account</h1><p className="mt-1 text-sm text-slate-500">Assign one active Principal to an active college.</p></div><form onSubmit={handleSubmit(submit)} className="space-y-5"><Select label="College *" options={options} error={errors.collegeId?.message} {...register("collegeId")} /><Input label="Full name *" error={errors.fullName?.message} {...register("fullName")} /><div className="grid gap-4 sm:grid-cols-2"><Input label="Email *" type="email" error={errors.email?.message} {...register("email")} /><Input label="Phone *" error={errors.phone?.message} {...register("phone")} /></div><p className="rounded-xl bg-blue-50 p-4 text-sm text-blue-800">The first password is the principal's phone number. They must change it after signing in.</p><div className="flex justify-end border-t pt-5"><Button type="submit" loading={isSubmitting}>Create Principal</Button></div></form></Card><Card className="h-fit p-6"><ShieldCheck className="h-7 w-7 text-emerald-600" /><h2 className="mt-4 font-bold">First-login security</h2><ul className="mt-3 space-y-2 text-sm leading-6 text-slate-500"><li>• Phone number is stored only as a BCrypt password hash.</li><li>• The principal must choose a strong new password on first login.</li><li>• The account cannot access the platform until that change is complete.</li></ul></Card></div></div>;
}

