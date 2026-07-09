import { ArrowLeft, Building2, LibraryBig } from "lucide-react";
import { useEffect, useState } from "react";
import { Link, useParams } from "react-router-dom";
import { toast } from "sonner";
import { Badge, StatusBadge } from "@/components/common/Badge";
import { Button } from "@/components/common/Button";
import { Card } from "@/components/common/Card";
import { Loader } from "@/components/common/Loader";
import { handleApiError } from "@/lib/handleApiError";
import { formatDate } from "@/lib/utils";
import { getDepartment } from "./api";
import type { Department } from "./types";

export function DepartmentDetailsPage() {
  const { id } = useParams();
  const [item, setItem] = useState<Department | null>(null);
  useEffect(() => { getDepartment(Number(id)).then(setItem).catch((error) => toast.error(handleApiError(error).message)); }, [id]);
  if (!item) return <div className="page-container"><Loader label="Loading department…" /></div>;
  return <div className="page-container"><Link to="/departments"><Button variant="ghost"><ArrowLeft className="h-4 w-4" />Back to departments</Button></Link><Card className="mt-4 overflow-hidden"><div className="bg-gradient-to-r from-indigo-700 to-blue-700 p-8 text-white"><div className="flex items-center gap-5"><div className="grid h-20 w-20 place-items-center rounded-2xl bg-white/15"><LibraryBig className="h-9 w-9" /></div><div><div className="flex flex-wrap items-center gap-3"><h1 className="text-3xl font-bold">{item.name}</h1><StatusBadge status={item.status} /></div><Badge tone="info">{item.code}</Badge></div></div></div><div className="grid gap-5 p-6 md:grid-cols-2"><div className="rounded-2xl bg-slate-50 p-5"><Building2 className="mb-3 h-5 w-5 text-brand-600" /><p className="text-xs font-bold uppercase text-slate-400">College</p><p className="mt-1 font-medium">{item.collegeName} ({item.collegeCode})</p></div><div className="rounded-2xl bg-slate-50 p-5"><p className="text-xs font-bold uppercase text-slate-400">Description</p><p className="mt-2 text-sm leading-6">{item.description || "No description provided."}</p></div></div><div className="grid gap-4 border-t p-6 text-sm sm:grid-cols-2"><p><span className="text-slate-400">Created:</span> {formatDate(item.createdAt)}</p><p><span className="text-slate-400">Updated:</span> {formatDate(item.updatedAt)}</p></div></Card></div>;
}
