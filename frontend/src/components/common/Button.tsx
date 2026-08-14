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
      primary:
        "bg-blue-600 text-white hover:-translate-y-0.5 hover:bg-blue-700 hover:shadow-md shadow-sm",
      secondary:
        "border border-blue-100 bg-blue-50 text-blue-700 hover:-translate-y-0.5 hover:bg-blue-100",
      danger: "bg-red-50 text-red-700 hover:bg-red-100",
      ghost: "text-slate-600 hover:bg-blue-50 hover:text-blue-700",
    };
    return (
      <button
        ref={ref}
        disabled={disabled || loading}
        className={cn(
          "inline-flex h-11 shrink-0 items-center justify-center gap-2 whitespace-nowrap rounded-xl px-4 text-sm font-semibold transition-all duration-200 ease-out [word-break:normal] [&_svg]:shrink-0 disabled:cursor-not-allowed disabled:opacity-60 sm:h-10",
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
