import { useEffect, useState } from "react";
import { toast } from "sonner";
import { Loader } from "@/components/common/Loader";
import { handleApiError } from "@/lib/handleApiError";
import { formatDate } from "@/lib/utils";
import * as admissionsApi from "@/features/admissions/api";
import { DetailSection } from "@/features/admissions/components";
import type { StudentProfileResponse } from "./types";

export function StudentProfilePage() {
  const [profile, setProfile] = useState<StudentProfileResponse | null>(null);
  const [loading, setLoading] = useState(true);
  useEffect(() => {
    admissionsApi.getStudentProfile().then(setProfile).catch((err) => toast.error(handleApiError(err).message)).finally(() => setLoading(false));
  }, []);
  if (loading) return <Loader label="Loading student profile..." />;
  if (!profile) return null;
  return (
    <div className="page-container space-y-5">
      <div><h1 className="page-title">My Profile</h1><p className="page-subtitle">Your student account and college details.</p></div>
      <DetailSection title="Student" rows={[["Name", profile.fullName], ["Email", profile.email], ["Phone", profile.phone], ["Date of Birth", formatDate(profile.dateOfBirth)], ["Gender", profile.gender], ["Status", profile.status]]} />
      <DetailSection title="College and Department" rows={[["Admission No", profile.admissionNumber], ["College", profile.collegeName], ["College Code", profile.collegeCode], ["Department", profile.departmentName], ["Department Code", profile.departmentCode], ["Created", formatDate(profile.createdAt)]]} />
      <DetailSection title="Parent" rows={[["Parent Name", profile.parentName], ["Parent Phone", profile.parentPhone]]} />
    </div>
  );
}
