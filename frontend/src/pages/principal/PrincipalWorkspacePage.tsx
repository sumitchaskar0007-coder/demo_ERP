import {
  BarChart3,
  BookOpen,
  CalendarDays,
  ChevronRight,
  CreditCard,
  FileText,
  GraduationCap,
  LibraryBig,
  Settings2,
  Users,
  WalletCards,
} from "lucide-react";
import { Link } from "react-router-dom";
import { Card } from "@/components/common/Card";
import { ROUTES } from "@/lib/constants";
import { cn } from "@/lib/utils";

type WorkspaceKind = "academics" | "fees" | "reports" | "administration";

const workspaces = {
  academics: {
    title: "Academics",
    subtitle: "Manage the college structure, subjects, divisions, and weekly schedule.",
    label: "Academic operations",
    heroIcon: GraduationCap,
    gradient: "from-blue-100 via-sky-100 to-cyan-100",
    items: [
      {
        title: "Departments",
        description: "Manage college departments and their academic scope.",
        to: ROUTES.departments,
        icon: LibraryBig,
      },
      {
        title: "Course Years",
        description: "Configure course years and academic progression.",
        to: ROUTES.courseYears,
        icon: GraduationCap,
      },
      {
        title: "Divisions",
        description: "Organize class divisions and class-teacher assignments.",
        to: ROUTES.divisions,
        icon: Users,
      },
      {
        title: "Subjects",
        description: "Create subjects and review teaching assignments.",
        to: ROUTES.academicSubjects,
        icon: BookOpen,
      },
      {
        title: "Weekly Timetable",
        description: "Build and review the weekly lecture schedule.",
        to: ROUTES.timetable,
        icon: CalendarDays,
      },
    ],
  },
  fees: {
    title: "Fees",
    subtitle: "Configure fees and monitor collections and outstanding balances.",
    label: "Financial operations",
    heroIcon: WalletCards,
    gradient: "from-blue-100 via-cyan-100 to-emerald-100",
    items: [
      {
        title: "Fee Structures",
        description: "Configure category-aware fees by department and year.",
        to: ROUTES.feeStructures,
        icon: CreditCard,
      },
      {
        title: "Fee Collection",
        description: "Review collected payments and student fee accounts.",
        to: ROUTES.principalFeeCollection,
        icon: WalletCards,
      },
      {
        title: "Pending Fees",
        description: "Track students with unpaid or partially paid balances.",
        to: ROUTES.principalPendingFees,
        icon: CreditCard,
      },
    ],
  },
  reports: {
    title: "Reports & Analytics",
    subtitle: "Open operational reports, attendance insights, and college analytics.",
    label: "Insights center",
    heroIcon: BarChart3,
    gradient: "from-blue-100 via-indigo-100 to-violet-100",
    items: [
      {
        title: "College Analytics",
        description: "Review key operational, academic, and financial indicators.",
        to: ROUTES.principalAnalytics,
        icon: BarChart3,
      },
      {
        title: "Attendance Reports",
        description: "Analyze attendance by student, teacher, class, and subject.",
        to: ROUTES.attendanceReport,
        icon: CalendarDays,
      },
      {
        title: "Admission Report",
        description: "Review and export admission activity.",
        to: ROUTES.admissionReport,
        icon: FileText,
      },
    ],
  },
  administration: {
    title: "Administration",
    subtitle: "Review governance history and manage your account settings.",
    label: "Governance & security",
    heroIcon: Settings2,
    gradient: "from-slate-100 via-blue-100 to-sky-100",
    items: [
      {
        title: "Audit Logs",
        description: "Review important activity and record changes across the college.",
        to: ROUTES.auditLogs,
        icon: FileText,
      },
      {
        title: "Profile",
        description: "Update your personal and contact information.",
        to: ROUTES.profile,
        icon: Users,
      },
    ],
  },
} satisfies Record<
  WorkspaceKind,
  {
    title: string;
    subtitle: string;
    label: string;
    heroIcon: typeof FileText;
    gradient: string;
    items: Array<{
      title: string;
      description: string;
      to: string;
      icon: typeof FileText;
    }>;
  }
>;

const moduleStyles = [
  {
    bar: "bg-blue-500",
    icon: "bg-blue-50 text-blue-700 ring-blue-100",
    number: "text-blue-100",
    hover: "group-hover:border-blue-200",
  },
  {
    bar: "bg-violet-500",
    icon: "bg-violet-50 text-violet-700 ring-violet-100",
    number: "text-violet-100",
    hover: "group-hover:border-violet-200",
  },
  {
    bar: "bg-emerald-500",
    icon: "bg-emerald-50 text-emerald-700 ring-emerald-100",
    number: "text-emerald-100",
    hover: "group-hover:border-emerald-200",
  },
  {
    bar: "bg-amber-500",
    icon: "bg-amber-50 text-amber-700 ring-amber-100",
    number: "text-amber-100",
    hover: "group-hover:border-amber-200",
  },
  {
    bar: "bg-cyan-500",
    icon: "bg-cyan-50 text-cyan-700 ring-cyan-100",
    number: "text-cyan-100",
    hover: "group-hover:border-cyan-200",
  },
];

export function PrincipalWorkspacePage({ kind }: { kind: WorkspaceKind }) {
  const workspace = workspaces[kind];
  const HeroIcon = workspace.heroIcon;
  const featureFirst = workspace.items.length > 3;
  return (
    <div className="page-container space-y-7">
      <section
        className={cn(
          "relative overflow-hidden rounded-3xl border border-blue-200 bg-gradient-to-br px-6 py-7 shadow-[0_18px_45px_rgba(37,99,235,0.12)] sm:px-8 sm:py-9",
          workspace.gradient,
        )}
      >
        <div
          className="pointer-events-none absolute -right-16 -top-24 h-72 w-72 rounded-full border border-blue-200/80 bg-white/35 blur-sm"
          aria-hidden="true"
        />
        <div
          className="pointer-events-none absolute -bottom-24 right-48 h-48 w-48 rounded-full bg-blue-300/35 blur-3xl"
          aria-hidden="true"
        />
        <div className="relative">
          <div className="flex items-start gap-4 sm:gap-5">
            <span className="grid h-14 w-14 shrink-0 place-items-center rounded-2xl bg-blue-600 text-white shadow-md shadow-blue-300/50 ring-1 ring-blue-500 sm:h-16 sm:w-16">
              <HeroIcon className="h-7 w-7 sm:h-8 sm:w-8" />
            </span>
            <div>
              <p className="text-xs font-bold uppercase tracking-[0.18em] text-blue-600">
                {workspace.label}
              </p>
              <h1 className="mt-2 text-3xl font-black tracking-tight text-slate-950 sm:text-4xl">
                {workspace.title}
              </h1>
              <p className="mt-3 max-w-2xl text-sm leading-6 text-slate-600 sm:text-base">
                {workspace.subtitle}
              </p>
            </div>
          </div>
        </div>
      </section>

      <div className="flex flex-col gap-2 sm:flex-row sm:items-end sm:justify-between">
        <div>
          <p className="text-xs font-bold uppercase tracking-[0.16em] text-blue-600">
            Workspace tools
          </p>
          <h2 className="mt-1 text-xl font-bold text-slate-950">What would you like to manage?</h2>
        </div>
        <p className="text-sm text-slate-500">Select a module to continue</p>
      </div>

      <div className="grid gap-4 md:grid-cols-2 xl:grid-cols-3">
        {workspace.items.map(({ title, description, to, icon: Icon }, index) => {
          const style = moduleStyles[index % moduleStyles.length];
          const featured = featureFirst && index === 0;
          return (
          <Link
            key={to}
            to={to}
            className={cn("group", featured && "md:col-span-2 xl:col-span-2")}
          >
            <Card
              className={cn(
                "relative flex h-full min-h-52 overflow-hidden p-0 transition duration-300 group-hover:-translate-y-1 group-hover:shadow-[0_18px_45px_rgba(15,23,42,0.10)]",
                style.hover,
                featured && "bg-gradient-to-br from-white via-white to-blue-50/60",
              )}
            >
              <div className={cn("absolute inset-x-0 top-0 h-1", style.bar)} />
              <span
                className={cn(
                  "pointer-events-none absolute -right-2 -top-7 text-[92px] font-black leading-none opacity-70",
                  style.number,
                )}
                aria-hidden="true"
              >
                {String(index + 1).padStart(2, "0")}
              </span>
              <div className="relative flex w-full flex-col p-5 sm:p-6">
              <div className="flex items-start justify-between gap-3">
                <span
                  className={cn(
                    "grid h-12 w-12 place-items-center rounded-2xl ring-1 ring-inset",
                    style.icon,
                  )}
                >
                  <Icon className="h-5 w-5" strokeWidth={2.2} />
                </span>
                <span className="grid h-9 w-9 place-items-center rounded-full border border-slate-200 bg-white text-slate-400 shadow-sm transition group-hover:translate-x-0.5 group-hover:border-blue-200 group-hover:text-blue-700">
                  <ChevronRight className="h-4 w-4" />
                </span>
              </div>
              <div className="mt-5 flex-1">
                <h3 className="text-lg font-bold text-slate-950">{title}</h3>
                <p className="mt-2 max-w-xl text-sm leading-6 text-slate-500">{description}</p>
              </div>
              <div className="mt-5 flex items-center gap-2 border-t border-slate-100 pt-4 text-xs font-bold uppercase tracking-[0.12em] text-slate-400 transition group-hover:text-blue-700">
                Open module
                <ChevronRight className="h-3.5 w-3.5 transition group-hover:translate-x-0.5" />
              </div>
              </div>
            </Card>
          </Link>
          );
        })}
      </div>
    </div>
  );
}
