import { zodResolver } from "@hookform/resolvers/zod";
import { useEffect, useState } from "react";
import { useForm } from "react-hook-form";
import { useNavigate } from "react-router-dom";
import { toast } from "sonner";
import { z } from "zod";
import { Button } from "@/components/common/Button";
import { Card } from "@/components/common/Card";
import { Input } from "@/components/common/Input";
import { Select } from "@/components/common/Select";
import { searchDepartments } from "@/features/departments/api";
import type { Department } from "@/features/departments/types";
import { createStaff } from "@/features/staff/api";
import { handleApiError } from "@/lib/handleApiError";
import { ROUTES, STAFF_TYPE_OPTIONS } from "@/lib/constants";
import { createStaffSchema } from "@/lib/validators";

type Values = z.infer<typeof createStaffSchema>;
const departmentRequired = ["HOD", "TEACHER", "CLASS_TEACHER", "SUBJECT_TEACHER"];

export function CreateStaffPage() {
  const navigate = useNavigate();
  const [departments, setDepartments] = useState<Department[]>([]);
  const { register, watch, handleSubmit, formState: { errors, isSubmitting } } = useForm<Values>({
    resolver: zodResolver(createStaffSchema),
    defaultValues: { fullName: "", email: "", phone: "", password: "", staffType: "TEACHER", joiningDate: "" },
  });
  const staffType = watch("staffType");
  const showDepartment = departmentRequired.includes(staffType);
  useEffect(() => {
    searchDepartments({ status: "ACTIVE", page: 0, size: 100 })
      .then((page) => setDepartments(page.content))
      .catch((error) => toast.error(handleApiError(error).message));
  }, []);
  const submit = async (values: Values) => {
    try {
      await createStaff({ ...values, departmentId: showDepartment ? values.departmentId : undefined });
      toast.success("Staff created successfully");
      navigate(ROUTES.staff);
    } catch (error) {
      toast.error(handleApiError(error).message);
    }
  };
  return (
    <div className="page-container">
      <h1 className="page-title">Create Staff</h1>
      <p className="page-subtitle">Create every staff type from one secure form.</p>
      <Card className="mt-6 p-6">
        <form className="grid gap-4 md:grid-cols-2" onSubmit={handleSubmit(submit)}>
          <Input label="Full Name" error={errors.fullName?.message} {...register("fullName")} />
          <Input label="Email" type="email" error={errors.email?.message} {...register("email")} />
          <Input label="Phone (optional)" error={errors.phone?.message} {...register("phone")} />
          <Input label="Password" type="password" error={errors.password?.message} {...register("password")} />
          <Select label="Staff Type" options={STAFF_TYPE_OPTIONS.slice(1)} error={errors.staffType?.message} {...register("staffType")} />
          {showDepartment && (
            <Select
              label="Department"
              options={[{ label: "Select department", value: "" }, ...departments.map((item) => ({ label: `${item.code} - ${item.name}`, value: item.id }))]}
              error={errors.departmentId?.message}
              {...register("departmentId")}
            />
          )}
          <Input label="Joining Date" type="date" error={errors.joiningDate?.message} {...register("joiningDate")} />
          <div className="md:col-span-2">
            <Button type="submit" loading={isSubmitting}>Create Staff</Button>
          </div>
        </form>
      </Card>
    </div>
  );
}
