import { cn } from "@/lib/utils";

export function BrandLogo({
  compact = false,
  className,
}: {
  compact?: boolean;
  className?: string;
}) {
  if (compact) {
    return (
      <div
        className={cn("relative h-11 w-11 shrink-0 overflow-hidden rounded-xl bg-white", className)}
      >
        <img
          src="/assets/college-erp-logo.png"
          alt="College ERP"
          className="absolute -left-[10px] -top-[5px] h-[110px] w-auto max-w-none"
        />
      </div>
    );
  }
  return (
    <img
      src="/assets/college-erp-logo.png"
      alt="College ERP — The Symbol of Success"
      className={cn("h-auto w-44 object-contain", className)}
    />
  );
}
