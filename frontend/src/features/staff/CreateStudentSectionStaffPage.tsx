import { zodResolver } from "@hookform/resolvers/zod";
import { useEffect, useState } from "react";
import { useForm } from "react-hook-form";
import { toast } from "sonner";
import { z } from "zod";
import { Button } from "@/components/common/Button";
import { Card } from "@/components/common/Card";
import { Input } from "@/components/common/Input";
import { Select } from "@/components/common/Select";
import { useAuth } from "@/features/auth/authStore";
import { getActiveColleges } from "@/features/colleges/api";
import type { College } from "@/features/colleges/types";
import { handleApiError } from "@/lib/handleApiError";
import { ROLES } from "@/lib/constants";
import { createStudentSectionStaffSchema } from "@/lib/validators";
import * as api from "./api";
import type { StaffResponse } from "./types";

type FormValues = z.infer<typeof createStudentSectionStaffSchema>;

export function CreateStudentSectionStaffPage() {
  const { user, isRole } = useAuth();
  const admin = isRole([ROLES.SUPER_ADMIN]);
  const [colleges, setColleges] = useState<College[]>([]);
  const [created, setCreated] = useState<StaffResponse | null>(null);
  const { register, handleSubmit, setValue, formState: { errors, isSubmitting } } = useForm<FormValues>({
    resolver: zodResolver(createStudentSectionStaffSchema),
    defaultValues: { collegeId: user?.collegeId || 0, phone: "", joiningDate: "" },
  });
  useEffect(() => {
    if (admin) getActiveColleges().then(setColleges).catch(() => setColleges([]));
    else if (user?.collegeId) setValue("collegeId", user.collegeId);
  }, [admin, setValue, user?.collegeId]);
  const onSubmit = async (values: FormValues) => {
    try {
      const staff = await api.createStudentSectionStaff(values);
      setCreated(staff);
      toast.success("Student Section staff created");
    } catch (err) {
      toast.error(handleApiError(err).message);
    }
  };
  const collegeOptions = [{ label: "Select college", value: "" }, ...colleges.map((college) => ({ label: college.name, value: college.id }))];
  return (
    <div className="page-container">
      <div><h1 className="page-title">Create Student Section Staff</h1><p className="page-subtitle">Create a login account for admission verification staff.</p></div>
      <div className="mt-6 grid gap-6 xl:grid-cols-[1fr_420px]">
        <Card className="p-6">
          <form onSubmit={handleSubmit(onSubmit)} className="grid gap-4 md:grid-cols-2">
            {admin ? <Select label="College" options={collegeOptions} error={errors.collegeId?.message} {...register("collegeId")} /> : <Input label="College" value={user?.collegeName || ""} readOnly />}
            <Input label="Full name" error={errors.fullName?.message} {...register("fullName")} />
            <Input label="Email" type="email" error={errors.email?.message} {...register("email")} />
            <Input label="Phone" error={errors.phone?.message} {...register("phone")} />
            <p className="rounded-xl bg-blue-50 p-3 text-sm text-blue-800">First password: the staff member's phone number. A password change is required at first login.</p>
            <Input label="Joining date" type="date" error={errors.joiningDate?.message} {...register("joiningDate")} />
            <div className="md:col-span-2"><Button type="submit" loading={isSubmitting}>Create Staff</Button></div>
          </form>
        </Card>
        <Card className="p-6">
          <h2 className="font-bold">Created staff</h2>
          {created ? <div className="mt-4 space-y-2 text-sm"><p><b>Employee Code:</b> {created.employeeCode}</p><p><b>Name:</b> {created.fullName}</p><p><b>Email:</b> {created.email}</p><p><b>College:</b> {created.collegeName}</p><p className="text-slate-500">Password is not shown after creation.</p></div> : <p className="mt-4 text-sm text-slate-500">Staff details will appear here after successful creation.</p>}
        </Card>
      </div>
    </div>
  );
}
