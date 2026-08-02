import { zodResolver } from "@hookform/resolvers/zod";
import { useEffect } from "react";
import { useForm } from "react-hook-form";
import { z } from "zod";
import { Button } from "@/components/common/Button";
import { Input } from "@/components/common/Input";
import { Select } from "@/components/common/Select";
import { Textarea } from "@/components/common/Textarea";
import type { College } from "@/features/colleges/types";
import { createDepartmentSchema, updateDepartmentSchema } from "@/lib/validators";
import type { AuthUser } from "@/features/auth/types";
import type { Department } from "@/features/departments/types";

type CreateForm = z.infer<typeof createDepartmentSchema>;

export function DepartmentForm({
  department,
  colleges,
  user,
  onSubmit,
  onCancel,
}: {
  department?: Department | null;
  colleges: College[];
  user: AuthUser;
  onSubmit: (values: CreateForm) => Promise<void>;
  onCancel: () => void;
}) {
  const editing = Boolean(department);
  const principal = user.roles.includes("PRINCIPAL");
  const {
    register,
    reset,
    handleSubmit,
    formState: { errors, isSubmitting },
  } = useForm<CreateForm>({
    resolver: zodResolver(editing ? updateDepartmentSchema : createDepartmentSchema),
    defaultValues: {
      collegeId: user.collegeId || 0,
      name: "",
      code: "",
      description: "",
      admissionFormFee: 0,
    },
  });
  useEffect(() => {
    reset(
      department
        ? {
            collegeId: department.collegeId,
            name: department.name,
            code: department.code,
            description: department.description || "",
            admissionFormFee: department.admissionFormFee,
          }
        : {
            collegeId: user.collegeId || 0,
            name: "",
            code: "",
            description: "",
            admissionFormFee: 0,
          },
    );
  }, [department, reset, user.collegeId]);
  const options = [
    { label: "Select college", value: 0 },
    ...colleges.map((college) => ({
      label: `${college.name} (${college.code})`,
      value: college.id,
    })),
  ];
  return (
    <form onSubmit={handleSubmit(onSubmit)} className="space-y-5">
      {principal ? (
        <Input label="College" value={`${user.collegeName} (${user.collegeCode})`} disabled />
      ) : (
        <Select
          label="College *"
          options={options}
          disabled={editing}
          error={errors.collegeId?.message}
          {...register("collegeId")}
        />
      )}
      <div className="grid gap-4 sm:grid-cols-2">
        <Input label="Department name *" error={errors.name?.message} {...register("name")} />
        <Input
          label="Department code *"
          disabled={editing}
          error={errors.code?.message}
          {...register("code")}
        />
      </div>
      <Textarea
        label="Description"
        error={errors.description?.message}
        {...register("description")}
      />
      <Input
        label="Admission form fee *"
        type="number"
        min="0.01"
        step="0.01"
        error={errors.admissionFormFee?.message}
        {...register("admissionFormFee")}
      />
      <div className="flex justify-end gap-3 border-t pt-5">
        <Button type="button" variant="secondary" onClick={onCancel}>
          Cancel
        </Button>
        <Button type="submit" loading={isSubmitting}>
          {editing ? "Save changes" : "Create department"}
        </Button>
      </div>
    </form>
  );
}
