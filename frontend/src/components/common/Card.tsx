import type { HTMLAttributes } from "react";
import { cn } from "@/lib/utils";

export function Card({ className, ...props }: HTMLAttributes<HTMLDivElement>) {
  return <div className={cn("rounded-2xl border border-blue-100/70 bg-white shadow-card transition-all duration-200 ease-out hover:-translate-y-0.5 hover:border-blue-200 hover:shadow-xl", className)} {...props} />;
}
