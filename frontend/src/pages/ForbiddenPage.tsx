import { ShieldX } from "lucide-react";
import { Link } from "react-router-dom";
import { Button } from "@/components/common/Button";
import { ROUTES } from "@/lib/constants";

export function ForbiddenPage() {
  return <div className="grid min-h-screen place-items-center bg-slate-50 p-6 text-center"><div><div className="mx-auto grid h-20 w-20 place-items-center rounded-3xl bg-red-50 text-red-600"><ShieldX className="h-10 w-10" /></div><h1 className="mt-6 text-3xl font-bold">Access denied</h1><p className="mt-2 text-slate-500">Your role does not have permission to view this page.</p><Link to={ROUTES.dashboard}><Button className="mt-6">Return to dashboard</Button></Link></div></div>;
}
