import { zodResolver } from "@hookform/resolvers/zod";
import { LockKeyhole } from "lucide-react";
import { useForm } from "react-hook-form";
import { useNavigate } from "react-router-dom";
import { toast } from "sonner";
import { z } from "zod";
import { Button } from "@/components/common/Button";
import { Card } from "@/components/common/Card";
import { Input } from "@/components/common/Input";
import { defaultRouteForRoles } from "@/lib/constants";
import { handleApiError } from "@/lib/handleApiError";
import * as api from "./api";
import { useAuth } from "./authStore";

const schema = z.object({
  currentPassword: z.string().min(1, "Current password is required"),
  newPassword: z.string().min(8).max(100).regex(/^(?=.*[a-z])(?=.*[A-Z])(?=.*\d)(?=.*[@$!%*?&.#_-])[A-Za-z\d@$!%*?&.#_-]{8,100}$/, "Include uppercase, lowercase, number, and special character"),
  confirmPassword: z.string().min(1, "Confirm your password"),
}).refine((values) => values.newPassword === values.confirmPassword, { path: ["confirmPassword"], message: "Passwords do not match" });

type FormValues = z.infer<typeof schema>;

export function ChangePasswordPage() {
  const { user, refreshProfile } = useAuth();
  const navigate = useNavigate();
  const { register, handleSubmit, formState: { errors, isSubmitting } } = useForm<FormValues>({ resolver: zodResolver(schema) });
  const submit = async ({ currentPassword, newPassword }: FormValues) => {
    try {
      await api.changePassword({ currentPassword, newPassword });
      await refreshProfile();
      toast.success("Password changed successfully");
      navigate(defaultRouteForRoles(user?.roles), { replace: true });
    } catch (error) { toast.error(handleApiError(error).message); }
  };
  return <div className="grid min-h-screen place-items-center bg-slate-50 p-4"><Card className="w-full max-w-md p-7"><div className="mb-5 inline-flex rounded-2xl bg-brand-50 p-3 text-brand-700"><LockKeyhole /></div><h1 className="text-2xl font-bold">Change your password</h1><p className="mt-2 text-sm text-slate-500">For security, you must replace your phone-number password before using the platform.</p><form onSubmit={handleSubmit(submit)} className="mt-6 space-y-4"><Input label="Current password" type="password" error={errors.currentPassword?.message} {...register("currentPassword")} /><Input label="New password" type="password" error={errors.newPassword?.message} {...register("newPassword")} /><Input label="Confirm new password" type="password" error={errors.confirmPassword?.message} {...register("confirmPassword")} /><Button type="submit" loading={isSubmitting} className="w-full">Change password and continue</Button></form></Card></div>;
}
