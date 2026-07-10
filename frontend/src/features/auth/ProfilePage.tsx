import { Building2, KeyRound, Mail, Phone, ShieldCheck, UserRound } from "lucide-react";
import { useEffect, useState } from "react";
import { Link } from "react-router-dom";
import { Badge, StatusBadge } from "@/components/common/Badge";
import { Button } from "@/components/common/Button";
import { Card } from "@/components/common/Card";
import { Loader } from "@/components/common/Loader";
import { ROUTES } from "@/lib/constants";
import { useAuth } from "./authStore";

export function ProfilePage() {
  const { user, refreshProfile } = useAuth();
  const [loading, setLoading] = useState(true);
  useEffect(() => { refreshProfile().finally(() => setLoading(false)); }, [refreshProfile]);
  if (loading || !user) return <div className="page-container"><Loader label="Loading your profile…" /></div>;
  const details = [
    { label: "Email", value: user.email, icon: Mail },
    { label: "Phone", value: user.phone || "Not provided", icon: Phone },
    { label: "College", value: user.collegeName ? `${user.collegeName} (${user.collegeCode})` : "System-wide access", icon: Building2 },
  ];
  return (
    <div className="page-container">
      <h1 className="page-title">My Profile</h1><p className="page-subtitle">Your identity and access information.</p>
      <Card className="mt-6 overflow-hidden">
        <div className="h-32 bg-gradient-to-r from-brand-600 to-indigo-700" />
        <div className="px-6 pb-8 sm:px-10">
          <div className="-mt-14 flex flex-col gap-5 sm:flex-row sm:items-end">
            <div className="grid h-28 w-28 place-items-center rounded-3xl border-4 border-white bg-blue-100 text-brand-700 shadow-lg"><UserRound className="h-12 w-12" /></div>
            <div className="flex-1 pb-2"><h2 className="text-2xl font-bold">{user.fullName}</h2><div className="mt-2 flex flex-wrap gap-2"><StatusBadge status={user.status} />{user.roles.map((role) => <Badge key={role} tone="info">{role.replaceAll("_", " ")}</Badge>)}</div></div>
          </div>
          <div className="mt-8 grid gap-4 md:grid-cols-3">{details.map(({ label, value, icon: Icon }) => <div key={label} className="rounded-2xl border bg-slate-50 p-5"><div className="mb-3 inline-flex rounded-xl bg-white p-2 text-brand-600 shadow-sm"><Icon className="h-5 w-5" /></div><p className="text-xs font-semibold uppercase tracking-wide text-slate-400">{label}</p><p className="mt-1 font-medium text-slate-800">{value}</p></div>)}</div>
          <div className="mt-6 flex flex-col gap-4 rounded-xl bg-emerald-50 p-4 text-sm text-emerald-700 sm:flex-row sm:items-center sm:justify-between"><div className="flex items-center gap-3"><ShieldCheck className="h-5 w-5" />Your password and authentication token are never displayed here.</div><Link to={ROUTES.changePassword}><Button variant="secondary"><KeyRound className="h-4 w-4" />Change Password</Button></Link></div>
        </div>
      </Card>
    </div>
  );
}
