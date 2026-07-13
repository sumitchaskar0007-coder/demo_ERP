import { ArrowLeft, Building2, Clock, Mail, Pencil, Phone, UserRound } from "lucide-react";
import { useEffect, useState } from "react";
import { Link, useParams } from "react-router-dom";
import { toast } from "sonner";
import { Badge, StatusBadge } from "@/components/common/Badge";
import { Button } from "@/components/common/Button";
import { Card } from "@/components/common/Card";
import { Loader } from "@/components/common/Loader";
import { handleApiError } from "@/lib/handleApiError";
import { formatDate } from "@/lib/utils";
import { getUser } from "./api";
import type { User } from "./types";

export function UserDetailsPage() {
  const { id } = useParams();
  const [user, setUser] = useState<User | null>(null);
  useEffect(() => { getUser(Number(id)).then(setUser).catch((error) => toast.error(handleApiError(error).message)); }, [id]);
  if (!user) return <div className="page-container"><Loader label="Loading user…" /></div>;
  const principal = user.roles.includes("PRINCIPAL");
  const details = [
    { icon: Mail, label: "Email", value: user.email },
    { icon: Phone, label: "Phone", value: user.phone || "Not provided" },
    { icon: Building2, label: "College", value: user.collegeName ? `${user.collegeName} (${user.collegeCode})` : "System-wide" },
    { icon: Clock, label: "Last login", value: formatDate(user.lastLoginAt) },
  ];
  return (
    <div className="page-container pb-10">
      <div className="flex items-center justify-between gap-4"><Link to="/users"><Button variant="ghost"><ArrowLeft className="h-4 w-4" />Back to users</Button></Link>{principal && <Link to={`/users/principals/${user.id}/edit`}><Button><Pencil className="h-4 w-4" />Edit Principal</Button></Link>}</div>
      <Card className="mt-4 overflow-hidden">
        <div className="erp-welcome-banner rounded-none p-8">
          <div className="relative flex items-center gap-5"><div className="grid h-20 w-20 place-items-center rounded-2xl bg-white/15"><UserRound className="h-9 w-9" /></div><div><div className="flex flex-wrap items-center gap-3"><h1 className="text-3xl font-bold">{user.fullName}</h1><StatusBadge status={user.status} /></div><div className="mt-2 flex flex-wrap gap-2">{user.roles.map((role) => <Badge key={role} tone="info">{role.replaceAll("_", " ")}</Badge>)}</div></div></div>
        </div>
        <div className="grid gap-5 p-6 md:grid-cols-2">{details.map(({ icon: Icon, label, value }) => <div key={label} className="rounded-2xl border border-slate-100 bg-slate-50 p-5"><Icon className="mb-3 h-5 w-5 text-brand-600" /><p className="text-xs font-bold uppercase text-slate-400">{label}</p><p className="mt-1 font-medium">{value}</p></div>)}</div>
        <div className="grid gap-4 border-t p-6 text-sm sm:grid-cols-2"><p><span className="text-slate-400">Created:</span> {formatDate(user.createdAt)}</p><p><span className="text-slate-400">Updated:</span> {formatDate(user.updatedAt)}</p></div>
      </Card>
    </div>
  );
}
