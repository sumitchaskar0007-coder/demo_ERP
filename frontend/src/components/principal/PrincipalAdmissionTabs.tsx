import { ClipboardCheck, FileText, FolderOpen } from "lucide-react";
import { NavLink } from "react-router-dom";
import { ROUTES } from "@/lib/constants";
import { cn } from "@/lib/utils";

const tabs = [
  { label: "Admission Records", to: ROUTES.studentSectionAdmissions, icon: FileText },
  { label: "Uploaded Documents", to: ROUTES.studentSectionDocuments, icon: FolderOpen },
  { label: "Final Admission Review", to: ROUTES.principalReviewReady, icon: ClipboardCheck },
];

export function PrincipalAdmissionTabs() {
  return (
    <nav
      className="mb-5 flex flex-wrap gap-2 rounded-2xl border border-slate-200 bg-white p-2 shadow-sm"
      aria-label="Admission workspace"
    >
      {tabs.map(({ label, to, icon: Icon }) => (
        <NavLink
          key={to}
          to={to}
          className={({ isActive }) =>
            cn(
              "flex h-10 items-center gap-2 rounded-xl px-4 text-sm font-semibold transition",
              isActive
                ? "bg-blue-600 text-white shadow-sm"
                : "text-slate-600 hover:bg-blue-50 hover:text-blue-700",
            )
          }
        >
          <Icon className="h-4 w-4" />
          {label}
        </NavLink>
      ))}
    </nav>
  );
}
