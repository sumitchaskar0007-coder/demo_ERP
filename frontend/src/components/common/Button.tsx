import { LoaderCircle } from "lucide-react";
import { forwardRef, type ButtonHTMLAttributes } from "react";
import { cn } from "@/lib/utils";

type Variant = "primary" | "secondary" | "danger" | "ghost";

interface ButtonProps extends ButtonHTMLAttributes<HTMLButtonElement> {
  variant?: Variant;
  loading?: boolean;
}

export const Button = forwardRef<HTMLButtonElement, ButtonProps>(
  ({ className, variant = "primary", loading, disabled, children, ...props }, ref) => {
    const styles: Record<Variant, string> = {
      primary: "bg-brand-600 text-white hover:bg-brand-700 shadow-sm",
      secondary: "border bg-white text-slate-700 hover:bg-slate-50",
      danger: "bg-red-600 text-white hover:bg-red-700",
      ghost: "text-slate-600 hover:bg-slate-100",
    };
    return (
      <button
        ref={ref}
        disabled={disabled || loading}
        className={cn(
          "inline-flex h-10 items-center justify-center gap-2 rounded-xl px-4 text-sm font-semibold transition disabled:cursor-not-allowed disabled:opacity-60",
          styles[variant],
          className,
        )}
        {...props}
      >
        {loading && <LoaderCircle className="h-4 w-4 animate-spin" />}
        {children}
      </button>
    );
  },
);
Button.displayName = "Button";
