import {
  BarChart3,
  ChevronRight,
  CreditCard,
  GraduationCap,
  Settings2,
  ShieldCheck,
  UserRoundCog,
  Users,
  WalletCards,
} from "lucide-react";
import { Link } from "react-router-dom";
import { Card } from "@/components/common/Card";
import { ROUTES } from "@/lib/constants";

type AdminWorkspaceKind = "people" | "fees" | "insights" | "administration";

const workspaces = {
  people: {
    eyebrow: "Organization",
    title: "User Management",
    description: "Manage principals, staff members, and students across all colleges.",
    icon: Users,
    items: [
      {
        title: "Principals",
        description: "View and manage college principal accounts.",
        to: ROUTES.principals,
        icon: UserRoundCog,
      },
      {
        title: "Staff",
        description: "Browse staff records and teaching responsibilities.",
        to: ROUTES.staff,
        icon: Users,
      },
      {
        title: "Students",
        description: "Review student profiles and academic placement.",
        to: ROUTES.students,
        icon: GraduationCap,
      },
    ],
  },
  fees: {
    eyebrow: "Financial operations",
    title: "Fees",
    description: "Configure fees and monitor collections and outstanding balances.",
    icon: WalletCards,
    items: [
      {
        title: "Fee Setup",
        description: "Configure college, department, and category-wise fee structures.",
        to: ROUTES.adminFeeSetup,
        icon: CreditCard,
      },
      {
        title: "Fee Collection",
        description: "Review verified collections and student fee accounts.",
        to: ROUTES.adminFeeCollection,
        icon: WalletCards,
      },
      {
        title: "Pending Fees",
        description: "Track unpaid and partially paid student balances.",
        to: ROUTES.adminPendingFees,
        icon: CreditCard,
      },
    ],
  },
  insights: {
    eyebrow: "Insights center",
    title: "Analytics",
    description: "Review institution performance and teacher workload across colleges.",
    icon: BarChart3,
    items: [
      {
        title: "Institution Analytics",
        description: "Compare admissions, academics, collections, and pending fees.",
        to: ROUTES.adminAnalytics,
        icon: BarChart3,
      },
      {
        title: "Staff Lecture Load",
        description: "Review each teacher's weekly schedule and assigned lecture load.",
        to: ROUTES.adminLectureLoad,
        icon: GraduationCap,
      },
    ],
  },
  administration: {
    eyebrow: "Account & security",
    title: "Administration",
    description: "Manage your administrator account and security settings.",
    icon: ShieldCheck,
    items: [
      {
        title: "Account",
        description: "Review account information and security controls.",
        to: ROUTES.account,
        icon: Settings2,
      },
    ],
  },
} satisfies Record<
  AdminWorkspaceKind,
  {
    eyebrow: string;
    title: string;
    description: string;
    icon: typeof Users;
    items: Array<{
      title: string;
      description: string;
      to: string;
      icon: typeof Users;
    }>;
  }
>;

const accents = [
  "border-blue-200 bg-blue-50 text-blue-700",
  "border-violet-200 bg-violet-50 text-violet-700",
  "border-emerald-200 bg-emerald-50 text-emerald-700",
];

export function AdminWorkspacePage({ kind }: { kind: AdminWorkspaceKind }) {
  const workspace = workspaces[kind];
  const HeroIcon = workspace.icon;

  return (
    <div className="page-container space-y-6 pb-10">
      <section className="relative overflow-hidden rounded-3xl border border-blue-200 bg-gradient-to-br from-blue-50 via-sky-50 to-cyan-50 px-6 py-7 shadow-sm sm:px-8">
        <div
          className="pointer-events-none absolute -right-16 -top-24 h-64 w-64 rounded-full bg-white/70"
          aria-hidden="true"
        />
        <div className="relative flex items-start gap-4">
          <span className="grid h-14 w-14 shrink-0 place-items-center rounded-2xl bg-blue-600 text-white shadow-sm">
            <HeroIcon className="h-7 w-7" />
          </span>
          <div>
            <p className="text-xs font-bold uppercase tracking-[0.16em] text-blue-600">
              {workspace.eyebrow}
            </p>
            <h1 className="mt-1 text-3xl font-black tracking-tight text-slate-950">
              {workspace.title}
            </h1>
            <p className="mt-2 max-w-2xl text-sm leading-6 text-slate-600">
              {workspace.description}
            </p>
          </div>
        </div>
      </section>

      <div>
        <p className="text-xs font-bold uppercase tracking-[0.16em] text-blue-600">
          Workspace tools
        </p>
        <h2 className="mt-1 text-xl font-bold text-slate-950">What would you like to manage?</h2>
      </div>

      <div className="grid gap-4 md:grid-cols-2 xl:grid-cols-3">
        {workspace.items.map(({ title, description, to, icon: Icon }, index) => (
          <Link key={to} to={to} className="group">
            <Card className="h-full border-slate-200 p-5 transition duration-200 hover:-translate-y-0.5 hover:border-blue-200 hover:shadow-lg hover:shadow-blue-100/60">
              <div className="flex items-start justify-between gap-4">
                <span
                  className={`grid h-11 w-11 place-items-center rounded-xl border ${accents[index % accents.length]}`}
                >
                  <Icon className="h-5 w-5" />
                </span>
                <span className="grid h-8 w-8 place-items-center rounded-full border border-slate-200 text-slate-400 transition group-hover:border-blue-200 group-hover:bg-blue-50 group-hover:text-blue-600">
                  <ChevronRight className="h-4 w-4" />
                </span>
              </div>
              <h3 className="mt-5 text-lg font-bold text-slate-950">{title}</h3>
              <p className="mt-2 min-h-10 text-sm leading-5 text-slate-500">{description}</p>
              <div className="mt-5 border-t border-slate-100 pt-4 text-xs font-bold uppercase tracking-[0.12em] text-blue-600">
                Open module
              </div>
            </Card>
          </Link>
        ))}
      </div>
    </div>
  );
}
