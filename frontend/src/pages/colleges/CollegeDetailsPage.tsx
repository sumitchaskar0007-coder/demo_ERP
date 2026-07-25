import { ArrowLeft, Building2, Mail, MapPin, Phone, Settings2 } from "lucide-react";
import { useEffect, useState } from "react";
import { Link, useParams } from "react-router-dom";
import { toast } from "sonner";
import { Badge, StatusBadge } from "@/components/common/Badge";
import { Button } from "@/components/common/Button";
import { Card } from "@/components/common/Card";
import { Loader } from "@/components/common/Loader";
import { handleApiError } from "@/lib/handleApiError";
import { formatDate } from "@/lib/utils";
import type { College } from "@/features/colleges/types";
import { getCollege } from "@/features/colleges/api";
import { PaymentQrManager } from "@/components/colleges/PaymentQrManager";

export function CollegeDetailsPage() {
  const { id } = useParams();
  const [college, setCollege] = useState<College | null>(null);
  const [section, setSection] = useState<"overview" | "features">("overview");
  useEffect(() => {
    getCollege(Number(id))
      .then(setCollege)
      .catch((error) => toast.error(handleApiError(error).message));
  }, [id]);
  if (!college)
    return (
      <div className="page-container">
        <Loader label="Loading college…" />
      </div>
    );
  const details = [
    {
      icon: MapPin,
      label: "Address",
      value:
        [college.address, college.city, college.state, college.pincode]
          .filter(Boolean)
          .join(", ") || "Not provided",
    },
    { icon: Mail, label: "Email", value: college.contactEmail || "Not provided" },
    { icon: Phone, label: "Phone", value: college.contactPhone || "Not provided" },
  ];
  return (
    <div className="page-container">
      <Link to="/colleges">
        <Button variant="ghost">
          <ArrowLeft className="h-4 w-4" />
          Back to colleges
        </Button>
      </Link>
      <div className="mt-4 flex gap-2 rounded-xl border bg-white p-2">
        <Button
          variant={section === "overview" ? "primary" : "ghost"}
          onClick={() => setSection("overview")}
        >
          <Building2 className="h-4 w-4" /> Overview
        </Button>
        <Button
          variant={section === "features" ? "primary" : "ghost"}
          onClick={() => setSection("features")}
        >
          <Settings2 className="h-4 w-4" /> Other Features
        </Button>
      </div>
      {section === "overview" ? (
        <Card className="mt-4 overflow-hidden">
          <div className="bg-gradient-to-r from-brand-700 to-indigo-800 p-8 text-white">
            <div className="flex flex-col gap-5 sm:flex-row sm:items-center">
              <div className="grid h-20 w-20 place-items-center rounded-2xl bg-white/15">
                <Building2 className="h-9 w-9" />
              </div>
              <div>
                <div className="flex flex-wrap items-center gap-3">
                  <h1 className="text-3xl font-bold">{college.name}</h1>
                  <StatusBadge status={college.status} />
                </div>
                <Badge tone="info">{college.code}</Badge>
              </div>
            </div>
          </div>
          <div className="grid gap-5 p-6 md:grid-cols-3">
            {details.map(({ icon: Icon, label, value }) => (
              <div key={label} className="rounded-2xl bg-slate-50 p-5">
                <Icon className="mb-3 h-5 w-5 text-brand-600" />
                <p className="text-xs font-bold uppercase text-slate-400">{label}</p>
                <p className="mt-1 text-sm font-medium">{value}</p>
              </div>
            ))}
          </div>
          <div className="grid gap-4 border-t p-6 text-sm sm:grid-cols-2">
            <p>
              <span className="text-slate-400">Created:</span> {formatDate(college.createdAt)}
            </p>
            <p>
              <span className="text-slate-400">Updated:</span> {formatDate(college.updatedAt)}
            </p>
          </div>
        </Card>
      ) : (
        <Card className="mt-4 p-6">
          <div className="mb-6">
            <h2 className="text-xl font-bold">Other Features</h2>
            <p className="mt-1 text-sm text-slate-500">Manage college-level payment settings.</p>
          </div>
          <PaymentQrManager collegeId={college.id} />
        </Card>
      )}
    </div>
  );
}
