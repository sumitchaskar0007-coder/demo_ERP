import { zodResolver } from "@hookform/resolvers/zod";
import { useEffect, useState } from "react";
import { useForm } from "react-hook-form";
import { z } from "zod";
import { Button } from "@/components/common/Button";
import { Input } from "@/components/common/Input";
import { Textarea } from "@/components/common/Textarea";
import { createCollegeSchema, updateCollegeSchema } from "@/lib/validators";
import type { College } from "@/features/colleges/types";
import { uploadCollegeImage } from "@/features/colleges/api";
import { toast } from "sonner";
import { handleApiError } from "@/lib/handleApiError";

type CreateForm = z.infer<typeof createCollegeSchema>;
type UpdateForm = z.infer<typeof updateCollegeSchema>;

const defaults: CreateForm = {
  name: "",
  code: "",
  address: "",
  city: "",
  state: "",
  pincode: "",
  contactEmail: "",
  contactPhone: "",
  logoUrl: "",
  qrCodeUrl: "",
  paymentQrAccountName: "",
};

export function CollegeForm({
  college,
  onSubmit,
  onCancel,
}: {
  college?: College | null;
  onSubmit: (values: CreateForm | UpdateForm) => Promise<void>;
  onCancel: () => void;
}) {
  const editing = Boolean(college);
  const schema = editing ? updateCollegeSchema : createCollegeSchema;
  const {
    register,
    setValue,
    watch,
    reset,
    handleSubmit,
    formState: { errors, isSubmitting },
  } = useForm<CreateForm>({
    resolver: zodResolver(schema),
    defaultValues: defaults,
  });
  const [uploading, setUploading] = useState<"logo" | "qr-code" | null>(null);
  const logoUrl = watch("logoUrl");
  const qrCodeUrl = watch("qrCodeUrl");
  const upload = async (file: File | undefined, kind: "logo" | "qr-code") => {
    if (!file) return;
    setUploading(kind);
    try {
      const url = await uploadCollegeImage(file, kind);
      setValue(kind === "logo" ? "logoUrl" : "qrCodeUrl", url, { shouldDirty: true });
      toast.success(`${kind === "logo" ? "Logo" : "QR code"} uploaded`);
    } catch (error) {
      toast.error(handleApiError(error).message);
    } finally {
      setUploading(null);
    }
  };
  useEffect(() => {
    if (!college) return reset(defaults);
    reset({
      name: college.name,
      code: college.code,
      address: college.address || "",
      city: college.city || "",
      state: college.state || "",
      pincode: college.pincode || "",
      contactEmail: college.contactEmail || "",
      contactPhone: college.contactPhone || "",
      logoUrl: college.logoUrl || "",
      qrCodeUrl: college.qrCodeUrl || "",
      paymentQrAccountName: college.paymentQrAccountName || "",
    });
  }, [college, reset]);
  return (
    <form onSubmit={handleSubmit(onSubmit)} className="space-y-5">
      <div className="grid gap-4 sm:grid-cols-2">
        <Input label="College name *" error={errors.name?.message} {...register("name")} />
        <Input
          label="College code *"
          disabled={editing}
          error={errors.code?.message}
          {...register("code")}
        />
      </div>
      <Textarea label="Address" error={errors.address?.message} {...register("address")} />
      <div className="grid gap-4 sm:grid-cols-3">
        <Input label="City" error={errors.city?.message} {...register("city")} />
        <Input label="State" error={errors.state?.message} {...register("state")} />
        <Input label="Pincode" error={errors.pincode?.message} {...register("pincode")} />
      </div>
      <div className="grid gap-4 sm:grid-cols-2">
        <Input
          label="Contact email"
          type="email"
          error={errors.contactEmail?.message}
          {...register("contactEmail")}
        />
        <Input
          label="Contact phone"
          error={errors.contactPhone?.message}
          {...register("contactPhone")}
        />
      </div>
      <div className="grid gap-4 sm:grid-cols-2">
        <ImageUpload
          label="College logo"
          value={logoUrl}
          loading={uploading === "logo"}
          onFile={(file) => void upload(file, "logo")}
        />
        <ImageUpload
          label="Payment QR code"
          value={qrCodeUrl}
          loading={uploading === "qr-code"}
          onFile={(file) => void upload(file, "qr-code")}
        />
        <input type="hidden" {...register("logoUrl")} />
        <input type="hidden" {...register("qrCodeUrl")} />
      </div>
      <Input
        label="QR account name"
        placeholder="Exact account name shown after scanning the QR code"
        error={errors.paymentQrAccountName?.message}
        {...register("paymentQrAccountName")}
      />
      <div className="flex justify-end gap-3 border-t pt-5">
        <Button type="button" variant="secondary" onClick={onCancel}>
          Cancel
        </Button>
        <Button type="submit" loading={isSubmitting}>
          {editing ? "Save changes" : "Create college"}
        </Button>
      </div>
    </form>
  );
}

function ImageUpload({
  label,
  value,
  loading,
  onFile,
}: {
  label: string;
  value?: string;
  loading: boolean;
  onFile: (file?: File) => void;
}) {
  return (
    <label className="block rounded-xl border border-dashed border-slate-300 p-4 text-sm transition hover:border-brand-400">
      <span className="font-semibold text-slate-700">{label}</span>
      <span className="mt-1 block text-xs text-slate-400">JPG or PNG, maximum 5 MB</span>
      <input
        className="mt-3 block w-full text-xs"
        type="file"
        accept="image/jpeg,image/png"
        disabled={loading}
        onChange={(event) => onFile(event.target.files?.[0])}
      />
      {loading && <span className="mt-2 block text-xs text-brand-600">Uploading…</span>}
      {value && (
        <img
          src={value}
          alt={`${label} preview`}
          className="mt-3 h-20 max-w-full rounded-lg border bg-white object-contain p-1"
        />
      )}
    </label>
  );
}
