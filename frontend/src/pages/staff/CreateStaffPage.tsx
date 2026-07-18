import { zodResolver } from "@hookform/resolvers/zod";
import { BriefcaseBusiness, Building2, Check } from "lucide-react";
import { useEffect, useState } from "react";
import { useForm } from "react-hook-form";
import { useNavigate } from "react-router-dom";
import { toast } from "sonner";
import { z } from "zod";
import { Button } from "@/components/common/Button";
import { Card } from "@/components/common/Card";
import { Input } from "@/components/common/Input";
import { searchDepartments } from "@/features/departments/api";
import type { Department } from "@/features/departments/types";
import { createStaff } from "@/features/staff/api";
import type { StaffType } from "@/features/staff/types";
import { useAuth } from "@/features/auth/authStore";
import { handleApiError } from "@/lib/handleApiError";
import { ROUTES } from "@/lib/constants";
import { createStaffSchema } from "@/lib/validators";

type Values = z.infer<typeof createStaffSchema>;
const TEACHING_ROLES: StaffType[] = ["HOD", "TEACHER", "CLASS_TEACHER", "SUBJECT_TEACHER"];
const ROLE_OPTIONS: Array<{ value: StaffType; label: string; description: string }> = [
  {
    value: "SUBJECT_TEACHER",
    label: "Teacher",
    description: "Teach assigned subjects and view a personal timetable",
  },
  {
    value: "CLASS_TEACHER",
    label: "Class Teacher",
    description: "Manage an assigned division and its timetable",
  },
  { value: "HOD", label: "HOD", description: "Lead one academic department" },
  {
    value: "STUDENT_SECTION",
    label: "Student Section",
    description: "Review admissions and student records",
  },
  {
    value: "FEE_SECTION",
    label: "Fee Section",
    description: "Verify payments and manage fee records",
  },
  { value: "GENERAL_STAFF", label: "General Staff", description: "Non-teaching staff access" },
];

export function CreateStaffPage() {
  const navigate = useNavigate();
  const { user } = useAuth();
  const [departments, setDepartments] = useState<Department[]>([]);
  const {
    register,
    watch,
    setValue,
    handleSubmit,
    formState: { errors, isSubmitting },
  } = useForm<Values>({
    resolver: zodResolver(createStaffSchema),
    defaultValues: {
      fullName: "",
      email: "",
      phone: "",
      password: "",
      staffTypes: ["SUBJECT_TEACHER"],
      departmentIds: [],
      joiningDate: "",
    },
  });
  const staffTypes = watch("staffTypes");
  const departmentIds = watch("departmentIds");
  const showDepartments = staffTypes.some((type) => TEACHING_ROLES.includes(type));

  useEffect(() => {
    if (!user?.collegeId) { setDepartments([]); return; }
    searchDepartments({ collegeId: user.collegeId, status: "ACTIVE", page: 0, size: 100 })
      .then((page) => setDepartments([
        ...new Map(page.content.map((department) => [
          `${department.collegeId}:${department.code.trim().toUpperCase()}`,
          department,
        ])).values(),
      ]))
      .catch((error) => toast.error(handleApiError(error).message));
  }, [user?.collegeId]);

  const toggleRole = (role: StaffType) => {
    const next = staffTypes.includes(role)
      ? staffTypes.filter((item) => item !== role)
      : [...staffTypes, role];
    setValue("staffTypes", next, { shouldValidate: true });
    if (!next.some((type) => TEACHING_ROLES.includes(type))) {
      setValue("departmentIds", [], { shouldValidate: true });
    }
  };

  const toggleDepartment = (id: number) => {
    setValue(
      "departmentIds",
      departmentIds.includes(id)
        ? departmentIds.filter((item) => item !== id)
        : [...departmentIds, id],
      { shouldValidate: true },
    );
  };

  const submit = async (values: Values) => {
    try {
      await createStaff({
        ...values,
        staffType: values.staffTypes[0],
        departmentId: values.departmentIds[0],
      });
      toast.success("Staff account created with all selected roles and departments");
      navigate(ROUTES.staff);
    } catch (error) {
      toast.error(handleApiError(error).message);
    }
  };

  return (
    <div className="page-container">
      <h1 className="page-title">Create Staff</h1>
      <p className="page-subtitle">
        Create one account with multiple teaching roles and departments.
      </p>
      <form className="mt-6 space-y-6" onSubmit={handleSubmit(submit)}>
        <Card className="grid gap-4 p-6 md:grid-cols-2">
          <Input label="Full Name" error={errors.fullName?.message} {...register("fullName")} />
          <Input label="Email" type="email" error={errors.email?.message} {...register("email")} />
          <Input label="Phone (optional)" error={errors.phone?.message} {...register("phone")} />
          <Input
            label="Password"
            type="password"
            error={errors.password?.message}
            {...register("password")}
          />
          <Input
            label="Joining Date"
            type="date"
            error={errors.joiningDate?.message}
            {...register("joiningDate")}
          />
        </Card>

        <Card className="p-6">
          <div className="mb-4 flex items-center gap-3">
            <div className="rounded-xl bg-brand-50 p-2 text-brand-600">
              <BriefcaseBusiness className="h-5 w-5" />
            </div>
            <div>
              <h2 className="font-bold">Roles</h2>
              <p className="text-sm text-slate-500">
                Teacher and Class Teacher can be combined. HOD and operational roles are standalone.
              </p>
            </div>
          </div>
          <div className="grid gap-3 md:grid-cols-2 xl:grid-cols-3">
            {ROLE_OPTIONS.map((role) => {
              const selected = staffTypes.includes(role.value);
              return (
                <button
                  key={role.value}
                  type="button"
                  onClick={() => toggleRole(role.value)}
                  className={`flex items-start gap-3 rounded-xl border p-4 text-left transition ${selected ? "border-brand-500 bg-brand-50 ring-1 ring-brand-500" : "border-slate-200 hover:border-brand-300 hover:bg-slate-50"}`}
                >
                  <span
                    className={`mt-0.5 grid h-5 w-5 shrink-0 place-items-center rounded border ${selected ? "border-brand-600 bg-brand-600 text-white" : "border-slate-300"}`}
                  >
                    {selected && <Check className="h-3.5 w-3.5" />}
                  </span>
                  <span>
                    <b className="text-sm text-slate-900">{role.label}</b>
                    <span className="mt-1 block text-xs leading-5 text-slate-500">
                      {role.description}
                    </span>
                  </span>
                </button>
              );
            })}
          </div>
          {errors.staffTypes?.message && (
            <p className="mt-3 text-sm text-rose-600">{errors.staffTypes.message}</p>
          )}
        </Card>

        {showDepartments && (
          <Card className="p-6">
            <div className="mb-4 flex items-center gap-3">
              <div className="rounded-xl bg-emerald-50 p-2 text-emerald-600">
                <Building2 className="h-5 w-5" />
              </div>
              <div>
                <h2 className="font-bold">Departments</h2>
                <p className="text-sm text-slate-500">
                  Select every department where this teacher can teach students.
                </p>
              </div>
            </div>
            <div className="grid gap-3 sm:grid-cols-2 xl:grid-cols-3">
              {departments.map((department) => {
                const selected = departmentIds.includes(department.id);
                return (
                  <button
                    key={department.id}
                    type="button"
                    onClick={() => toggleDepartment(department.id)}
                    className={`flex items-center gap-3 rounded-xl border p-4 text-left transition ${selected ? "border-emerald-500 bg-emerald-50 ring-1 ring-emerald-500" : "border-slate-200 hover:border-emerald-300"}`}
                  >
                    <span
                      className={`grid h-5 w-5 shrink-0 place-items-center rounded border ${selected ? "border-emerald-600 bg-emerald-600 text-white" : "border-slate-300"}`}
                    >
                      {selected && <Check className="h-3.5 w-3.5" />}
                    </span>
                    <span>
                      <b className="text-sm">{department.code}</b>
                      <span className="block text-xs text-slate-500">{department.name}</span>
                    </span>
                  </button>
                );
              })}
            </div>
            {!departments.length && (
              <p className="rounded-xl bg-slate-50 p-4 text-sm text-slate-500">
                Create an active department before adding teaching staff.
              </p>
            )}
            {errors.departmentIds?.message && (
              <p className="mt-3 text-sm text-rose-600">{errors.departmentIds.message}</p>
            )}
          </Card>
        )}

        <div className="flex justify-end">
          <Button type="submit" loading={isSubmitting}>
            Create Staff Account
          </Button>
        </div>
      </form>
    </div>
  );
}
