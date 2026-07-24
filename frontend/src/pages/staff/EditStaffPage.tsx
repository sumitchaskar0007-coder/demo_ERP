import { zodResolver } from "@hookform/resolvers/zod";
import { ArrowLeft, BriefcaseBusiness, Building2, Check, ShieldCheck } from "lucide-react";
import { useEffect, useState } from "react";
import { useForm } from "react-hook-form";
import { Link, useNavigate, useParams } from "react-router-dom";
import { toast } from "sonner";
import { z } from "zod";
import { Badge } from "@/components/common/Badge";
import { Button } from "@/components/common/Button";
import { Card } from "@/components/common/Card";
import { Loader } from "@/components/common/Loader";
import { useAuth } from "@/features/auth/authStore";
import { searchDepartments } from "@/features/departments/api";
import type { Department } from "@/features/departments/types";
import { getStaffById, updateStaffAssignment } from "@/features/staff/api";
import type { StaffResponse, StaffType } from "@/features/staff/types";
import { handleApiError } from "@/lib/handleApiError";
import { ROUTES } from "@/lib/constants";
import { updateStaffAssignmentSchema } from "@/lib/validators";

type Values = z.infer<typeof updateStaffAssignmentSchema>;
type EditableRole = Values["staffTypes"][number];

const TEACHING_ROLES: EditableRole[] = ["HOD", "TEACHER", "SUBJECT_TEACHER"];
const ROLE_OPTIONS: Array<{ value: EditableRole; label: string; description: string }> = [
  {
    value: "TEACHER",
    label: "Teacher",
    description: "Teach assigned subjects across selected departments",
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

function editableRoles(staff: StaffResponse): EditableRole[] {
  if (staff.staffType === "HOD" || staff.roles.includes("HOD")) return ["HOD"];
  if (staff.roles.includes("STUDENT_SECTION")) return ["STUDENT_SECTION"];
  if (staff.roles.includes("FEE_SECTION")) return ["FEE_SECTION"];
  if (staff.roles.includes("GENERAL_STAFF")) return ["GENERAL_STAFF"];
  return ["TEACHER"];
}

export function EditStaffPage() {
  const { id } = useParams();
  const navigate = useNavigate();
  const { user } = useAuth();
  const [staff, setStaff] = useState<StaffResponse | null>(null);
  const [departments, setDepartments] = useState<Department[]>([]);
  const [loading, setLoading] = useState(true);
  const {
    watch,
    setValue,
    reset,
    handleSubmit,
    formState: { errors, isSubmitting },
  } = useForm<Values>({
    resolver: zodResolver(updateStaffAssignmentSchema),
    defaultValues: { staffTypes: [], departmentIds: [] },
  });
  const staffTypes = watch("staffTypes");
  const departmentIds = watch("departmentIds");
  const showDepartments = staffTypes.some((type) => TEACHING_ROLES.includes(type));

  useEffect(() => {
    if (!id || !user?.collegeId) return;
    setLoading(true);
    Promise.all([
      getStaffById(Number(id)),
      searchDepartments({
        collegeId: user.collegeId,
        status: "ACTIVE",
        page: 0,
        size: 100,
      }),
    ])
      .then(([staffResponse, departmentPage]) => {
        setStaff(staffResponse);
        setDepartments(
          [
            ...new Map(
              departmentPage.content.map((department) => [
                `${department.collegeId}:${department.code.trim().toUpperCase()}`,
                department,
              ]),
            ).values(),
          ],
        );
        reset({
          staffTypes: editableRoles(staffResponse),
          departmentIds: staffResponse.departmentIds,
        });
      })
      .catch((error) => toast.error(handleApiError(error).message))
      .finally(() => setLoading(false));
  }, [id, reset, user?.collegeId]);

  const toggleRole = (role: EditableRole) => {
    let next = staffTypes.includes(role)
      ? staffTypes.filter((item) => item !== role)
      : [...staffTypes, role];
    if (role === "HOD" && !staffTypes.includes(role)) next = ["HOD"];
    if (!TEACHING_ROLES.includes(role) && !staffTypes.includes(role)) next = [role];
    setValue("staffTypes", next, { shouldDirty: true, shouldValidate: true });
    if (!next.some((type) => TEACHING_ROLES.includes(type))) {
      setValue("departmentIds", [], { shouldDirty: true, shouldValidate: true });
    }
  };

  const toggleDepartment = (departmentId: number) => {
    const oneDepartmentOnly = staffTypes.includes("HOD");
    setValue(
      "departmentIds",
      departmentIds.includes(departmentId)
        ? departmentIds.filter((item) => item !== departmentId)
        : oneDepartmentOnly
          ? [departmentId]
          : [...departmentIds, departmentId],
      { shouldDirty: true, shouldValidate: true },
    );
  };

  const submit = async (values: Values) => {
    if (!staff) return;
    try {
      await updateStaffAssignment(staff.id, {
        staffTypes: values.staffTypes as StaffType[],
        departmentIds: values.departmentIds,
      });
      toast.success("Staff roles and departments updated");
      navigate(`/staff/${staff.id}`);
    } catch (error) {
      toast.error(handleApiError(error).message);
    }
  };

  if (loading || !staff) {
    return (
      <div className="page-container">
        <Loader label="Loading staff assignment..." />
      </div>
    );
  }

  return (
    <div className="page-container">
      <Link to={`/staff/${staff.id}`}>
        <Button variant="ghost">
          <ArrowLeft className="h-4 w-4" />
          Back to staff details
        </Button>
      </Link>

      <div className="mt-4">
        <h1 className="page-title">Edit Staff Assignment</h1>
        <p className="page-subtitle">
          Change access roles and department scope for this staff member.
        </p>
      </div>

      <Card className="mt-6 p-5 sm:p-6">
        <div className="flex flex-col gap-4 sm:flex-row sm:items-center">
          <span className="grid h-12 w-12 shrink-0 place-items-center rounded-xl bg-blue-50 font-bold text-blue-700">
            {staff.fullName
              .split(/\s+/)
              .map((part) => part[0])
              .join("")
              .slice(0, 2)
              .toUpperCase()}
          </span>
          <div className="min-w-0 flex-1">
            <h2 className="truncate text-lg font-bold text-slate-900">{staff.fullName}</h2>
            <p className="truncate text-sm text-slate-500">{staff.email}</p>
          </div>
          <Badge>{staff.employeeCode}</Badge>
        </div>
      </Card>

      <form className="mt-5 space-y-5" onSubmit={handleSubmit(submit)}>
        <Card className="p-5 sm:p-6">
          <div className="mb-4 flex items-center gap-3">
            <span className="rounded-xl bg-brand-50 p-2 text-brand-600">
              <BriefcaseBusiness className="h-5 w-5" />
            </span>
            <div>
              <h2 className="font-bold text-slate-900">Roles</h2>
              <p className="text-sm text-slate-500">
                Class Teacher access remains controlled by the HOD’s division assignment.
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
                  className={`flex items-start gap-3 rounded-xl border p-4 text-left transition ${
                    selected
                      ? "border-brand-500 bg-brand-50 ring-1 ring-brand-500"
                      : "border-slate-200 hover:border-brand-300 hover:bg-slate-50"
                  }`}
                >
                  <span
                    className={`mt-0.5 grid h-5 w-5 shrink-0 place-items-center rounded border ${
                      selected ? "border-brand-600 bg-brand-600 text-white" : "border-slate-300"
                    }`}
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
          <Card className="p-5 sm:p-6">
            <div className="mb-4 flex items-center gap-3">
              <span className="rounded-xl bg-emerald-50 p-2 text-emerald-600">
                <Building2 className="h-5 w-5" />
              </span>
              <div>
                <h2 className="font-bold text-slate-900">Departments</h2>
                <p className="text-sm text-slate-500">
                  {staffTypes.includes("HOD")
                    ? "An HOD can lead exactly one department."
                    : "Select every department where this teacher can work."}
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
                    className={`flex items-center gap-3 rounded-xl border p-4 text-left transition ${
                      selected
                        ? "border-emerald-500 bg-emerald-50 ring-1 ring-emerald-500"
                        : "border-slate-200 hover:border-emerald-300"
                    }`}
                  >
                    <span
                      className={`grid h-5 w-5 shrink-0 place-items-center rounded border ${
                        selected
                          ? "border-emerald-600 bg-emerald-600 text-white"
                          : "border-slate-300"
                      }`}
                    >
                      {selected && <Check className="h-3.5 w-3.5" />}
                    </span>
                    <span>
                      <b className="text-sm text-slate-900">{department.code}</b>
                      <span className="block text-xs text-slate-500">{department.name}</span>
                    </span>
                  </button>
                );
              })}
            </div>
            {errors.departmentIds?.message && (
              <p className="mt-3 text-sm text-rose-600">{errors.departmentIds.message}</p>
            )}
          </Card>
        )}

        <Card className="flex items-start gap-3 border-blue-100 bg-blue-50 p-4 text-sm text-blue-800">
          <ShieldCheck className="mt-0.5 h-5 w-5 shrink-0" />
          <p>
            Existing class and subject assignments are preserved. A department cannot be removed
            until its active teaching assignments have been reassigned.
          </p>
        </Card>

        <div className="flex flex-col-reverse gap-3 sm:flex-row sm:justify-end">
          <Button type="button" variant="secondary" onClick={() => navigate(ROUTES.staff)}>
            Cancel
          </Button>
          <Button type="submit" loading={isSubmitting}>
            Save Assignment
          </Button>
        </div>
      </form>
    </div>
  );
}
