import { zodResolver } from "@hookform/resolvers/zod";
import { ArrowLeft, Eye, EyeOff, LockKeyhole, Pencil, ShieldCheck } from "lucide-react";
import { useEffect, useState } from "react";
import { useForm } from "react-hook-form";
import { Link, useNavigate, useParams } from "react-router-dom";
import { toast } from "sonner";
import { z } from "zod";
import { Button } from "@/components/common/Button";
import { Card } from "@/components/common/Card";
import { Input } from "@/components/common/Input";
import { Loader } from "@/components/common/Loader";
import { handleApiError } from "@/lib/handleApiError";
import { updatePrincipalSchema } from "@/lib/validators";
import { getUser, updatePrincipal } from "./api";
import type { User } from "./types";

type FormValues = z.infer<typeof updatePrincipalSchema>;

export function EditPrincipalPage() {
  const { id } = useParams();
  const principalId = Number(id);
  const navigate = useNavigate();
  const [principal, setPrincipal] = useState<User | null>(null);
  const [loading, setLoading] = useState(true);
  const [showPassword, setShowPassword] = useState(false);
  const {
    register,
    reset,
    handleSubmit,
    formState: { errors, isSubmitting },
  } = useForm<FormValues>({
    resolver: zodResolver(updatePrincipalSchema),
    defaultValues: { phone: "", password: "" },
  });

  useEffect(() => {
    getUser(principalId)
      .then((user) => {
        if (!user.roles.includes("PRINCIPAL")) throw new Error("This account is not a Principal");
        setPrincipal(user);
        reset({ phone: user.phone || "", password: "" });
      })
      .catch((error) => toast.error(handleApiError(error).message))
      .finally(() => setLoading(false));
  }, [principalId, reset]);

  const submit = async (values: FormValues) => {
    try {
      const response = await updatePrincipal(principalId, values);
      toast.success(response.message);
      navigate(`/users/${principalId}`);
    } catch (error) {
      toast.error(handleApiError(error).message);
    }
  };

  if (loading || !principal)
    return (
      <div className="page-container">
        <Loader label="Loading Principal profile…" />
      </div>
    );
  return (
    <div className="page-container pb-10">
      <Link to={`/users/${principalId}`}>
        <Button variant="ghost">
          <ArrowLeft className="h-4 w-4" />
          Back to profile
        </Button>
      </Link>
      <div className="mt-4 grid gap-6 xl:grid-cols-[1fr_340px]">
        <Card className="overflow-hidden">
          <div className="erp-panel-header">
            <div>
              <h1 className="text-xl font-bold">Edit Principal access details</h1>
              <p className="mt-1 text-sm text-slate-500">
                Only phone and password can be changed by Super Admin.
              </p>
            </div>
            <div className="grid h-11 w-11 place-items-center rounded-xl bg-blue-50 text-brand-600">
              <Pencil className="h-5 w-5" />
            </div>
          </div>
          <div className="grid gap-4 border-b bg-slate-50/70 p-6 sm:grid-cols-2">
            <div>
              <p className="text-xs font-bold uppercase text-slate-400">Name — locked</p>
              <p className="mt-1 font-semibold">{principal.fullName}</p>
            </div>
            <div>
              <p className="text-xs font-bold uppercase text-slate-400">Email — locked</p>
              <p className="mt-1 font-semibold">{principal.email}</p>
            </div>
            <div>
              <p className="text-xs font-bold uppercase text-slate-400">College — locked</p>
              <p className="mt-1 font-semibold">{principal.collegeName}</p>
            </div>
            <div>
              <p className="text-xs font-bold uppercase text-slate-400">Role — locked</p>
              <p className="mt-1 font-semibold">PRINCIPAL</p>
            </div>
          </div>
          <form onSubmit={handleSubmit(submit)} className="space-y-5 p-6 sm:p-8">
            <Input label="Phone number" error={errors.phone?.message} {...register("phone")} />
            <div className="relative">
              <Input
                label="New password (optional)"
                type={showPassword ? "text" : "password"}
                placeholder="Leave blank to keep current password"
                icon={<LockKeyhole className="h-4 w-4" />}
                error={errors.password?.message}
                className="pr-12"
                {...register("password")}
              />
              <button
                type="button"
                className="absolute right-3 top-[38px] text-slate-400"
                onClick={() => setShowPassword((value) => !value)}
                aria-label="Toggle password"
              >
                {showPassword ? <EyeOff className="h-5 w-5" /> : <Eye className="h-5 w-5" />}
              </button>
            </div>
            <div className="flex justify-end gap-3 border-t pt-5">
              <Link to={`/users/${principalId}`}>
                <Button type="button" variant="secondary">
                  Cancel
                </Button>
              </Link>
              <Button type="submit" loading={isSubmitting}>
                Save changes
              </Button>
            </div>
          </form>
        </Card>
        <Card className="h-fit p-6">
          <ShieldCheck className="h-7 w-7 text-emerald-600" />
          <h2 className="mt-4 font-bold">Protected identity</h2>
          <p className="mt-3 text-sm leading-6 text-slate-500">
            Name, email, user ID, college, role and account status cannot be changed here. This
            protects the Principal’s identity and access scope.
          </p>
        </Card>
      </div>
    </div>
  );
}
