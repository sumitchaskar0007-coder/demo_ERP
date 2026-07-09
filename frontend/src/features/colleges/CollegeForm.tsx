import { zodResolver } from "@hookform/resolvers/zod";
import { useEffect } from "react";
import { useForm } from "react-hook-form";
import { z } from "zod";
import { Button } from "@/components/common/Button";
import { Input } from "@/components/common/Input";
import { Textarea } from "@/components/common/Textarea";
import { createCollegeSchema, updateCollegeSchema } from "@/lib/validators";
import type { College } from "./types";

type CreateForm = z.infer<typeof createCollegeSchema>;
type UpdateForm = z.infer<typeof updateCollegeSchema>;

const defaults: CreateForm = {
  name: "", code: "", address: "", city: "", state: "", pincode: "",
  contactEmail: "", contactPhone: "", logoUrl: "", qrCodeUrl: "",
};

export function CollegeForm({ college, onSubmit, onCancel }: {
  college?: College | null;
  onSubmit: (values: CreateForm | UpdateForm) => Promise<void>;
  onCancel: () => void;
}) {
  const editing = Boolean(college);
  const schema = editing ? updateCollegeSchema : createCollegeSchema;
  const { register, reset, handleSubmit, formState: { errors, isSubmitting } } = useForm<CreateForm>({
    resolver: zodResolver(schema),
    defaultValues: defaults,
  });
  useEffect(() => {
    if (!college) return reset(defaults);
    reset({
      name: college.name, code: college.code, address: college.address || "",
      city: college.city || "", state: college.state || "", pincode: college.pincode || "",
      contactEmail: college.contactEmail || "", contactPhone: college.contactPhone || "",
      logoUrl: college.logoUrl || "", qrCodeUrl: college.qrCodeUrl || "",
    });
  }, [college, reset]);
  return (
    <form onSubmit={handleSubmit(onSubmit)} className="space-y-5">
      <div className="grid gap-4 sm:grid-cols-2">
        <Input label="College name *" error={errors.name?.message} {...register("name")} />
        <Input label="College code *" disabled={editing} error={errors.code?.message} {...register("code")} />
      </div>
      <Textarea label="Address" error={errors.address?.message} {...register("address")} />
      <div className="grid gap-4 sm:grid-cols-3">
        <Input label="City" error={errors.city?.message} {...register("city")} />
        <Input label="State" error={errors.state?.message} {...register("state")} />
        <Input label="Pincode" error={errors.pincode?.message} {...register("pincode")} />
      </div>
      <div className="grid gap-4 sm:grid-cols-2">
        <Input label="Contact email" type="email" error={errors.contactEmail?.message} {...register("contactEmail")} />
        <Input label="Contact phone" error={errors.contactPhone?.message} {...register("contactPhone")} />
      </div>
      <div className="grid gap-4 sm:grid-cols-2">
        <Input label="Logo URL" error={errors.logoUrl?.message} {...register("logoUrl")} />
        <Input label="QR code URL" error={errors.qrCodeUrl?.message} {...register("qrCodeUrl")} />
      </div>
      <div className="flex justify-end gap-3 border-t pt-5">
        <Button type="button" variant="secondary" onClick={onCancel}>Cancel</Button>
        <Button type="submit" loading={isSubmitting}>{editing ? "Save changes" : "Create college"}</Button>
      </div>
    </form>
  );
}
