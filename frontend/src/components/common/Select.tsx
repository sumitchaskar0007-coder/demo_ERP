import {
  forwardRef,
  useEffect,
  useMemo,
  useRef,
  useState,
  type ChangeEvent,
  type SelectHTMLAttributes,
} from "react";
import { createPortal } from "react-dom";
import { Check, ChevronDown, Search, X } from "lucide-react";
import type { SelectOption } from "@/types/common";
import { cn } from "@/lib/utils";

interface SelectProps extends SelectHTMLAttributes<HTMLSelectElement> {
  label?: string;
  error?: string;
  options: SelectOption[];
}

export const Select = forwardRef<HTMLSelectElement, SelectProps>(
  (
    {
      label,
      error,
      options,
      className,
      id,
      value,
      defaultValue,
      disabled,
      onChange,
      ...props
    },
    forwardedRef,
  ) => {
    const selectId = id || props.name;
    const nativeSelectRef = useRef<HTMLSelectElement | null>(null);
    const [open, setOpen] = useState(false);
    const [query, setQuery] = useState("");
    const [uncontrolledValue, setUncontrolledValue] = useState(() =>
      String(defaultValue ?? ""),
    );
    const selectedValue = String(value ?? uncontrolledValue);
    const selectedOption = options.find(
      (option) => String(option.value) === selectedValue,
    );
    const filteredOptions = useMemo(() => {
      const search = query.trim().toLowerCase();
      if (!search) return options;
      return options.filter((option) => option.label.toLowerCase().includes(search));
    }, [options, query]);

    useEffect(() => {
      if (!open) return;
      const previousOverflow = document.body.style.overflow;
      document.body.style.overflow = "hidden";
      const closeOnEscape = (event: KeyboardEvent) => {
        if (event.key === "Escape") setOpen(false);
      };
      window.addEventListener("keydown", closeOnEscape);
      return () => {
        document.body.style.overflow = previousOverflow;
        window.removeEventListener("keydown", closeOnEscape);
      };
    }, [open]);

    const setRefs = (node: HTMLSelectElement | null) => {
      nativeSelectRef.current = node;
      if (typeof forwardedRef === "function") forwardedRef(node);
      else if (forwardedRef) forwardedRef.current = node;
    };

    const handleNativeChange = (event: ChangeEvent<HTMLSelectElement>) => {
      setUncontrolledValue(event.target.value);
      onChange?.(event);
    };

    const choose = (nextValue: string) => {
      const select = nativeSelectRef.current;
      if (!select) return;
      select.value = nextValue;
      setUncontrolledValue(nextValue);
      onChange?.({
        target: select,
        currentTarget: select,
        type: "change",
      } as ChangeEvent<HTMLSelectElement>);
      setOpen(false);
      setQuery("");
    };

    return (
      <div className="min-w-0 space-y-1.5">
        {label && (
          <label htmlFor={selectId} className="text-sm font-medium text-slate-700">
            {label}
          </label>
        )}

        <select
          ref={setRefs}
          id={selectId}
          value={value}
          defaultValue={defaultValue}
          disabled={disabled}
          onChange={handleNativeChange}
          className={cn(
            "hidden h-11 w-full rounded-xl border bg-white px-3 text-sm focus:border-brand-500 focus:ring-2 focus:ring-brand-100 sm:block",
            error && "border-red-400",
            className,
          )}
          {...props}
        >
          {options.map((option) => (
            <option key={option.value} value={option.value}>
              {option.label}
            </option>
          ))}
        </select>

        <button
          type="button"
          disabled={disabled}
          aria-haspopup="listbox"
          aria-expanded={open}
          aria-label={label ? `Choose ${label}` : "Choose an option"}
          onClick={() => {
            setQuery("");
            setOpen(true);
          }}
          className={cn(
            "flex h-12 w-full min-w-0 items-center justify-between gap-3 rounded-xl border bg-white px-4 text-left text-base text-slate-800 shadow-sm transition active:scale-[0.99] sm:hidden",
            "focus:border-brand-500 focus:ring-2 focus:ring-brand-100",
            disabled && "cursor-not-allowed bg-slate-100 text-slate-400 shadow-none",
            error && "border-red-400",
            className,
          )}
        >
          <span className="truncate font-medium">
            {selectedOption?.label || "Select an option"}
          </span>
          <ChevronDown className="h-5 w-5 shrink-0 text-slate-500" />
        </button>

        {error && <p className="text-xs text-red-600">{error}</p>}

        {open &&
          createPortal(
            <div className="fixed inset-0 z-[100] flex items-end sm:hidden">
              <button
                type="button"
                aria-label="Close options"
                className="absolute inset-0 bg-slate-950/55 backdrop-blur-[2px]"
                onClick={() => setOpen(false)}
              />
              <section
                role="dialog"
                aria-modal="true"
                aria-label={label || "Choose an option"}
                className="relative flex max-h-[86dvh] w-full flex-col overflow-hidden rounded-t-[28px] bg-white shadow-2xl"
              >
                <div className="mx-auto mt-3 h-1.5 w-12 rounded-full bg-slate-300" />
                <div className="flex items-center justify-between gap-4 px-5 pb-4 pt-3">
                  <div className="min-w-0">
                    <p className="text-xs font-semibold uppercase tracking-wider text-brand-600">
                      Select option
                    </p>
                    <h2 className="truncate text-xl font-bold text-slate-900">
                      {label || "Choose an option"}
                    </h2>
                  </div>
                  <button
                    type="button"
                    aria-label="Close"
                    onClick={() => setOpen(false)}
                    className="grid h-11 w-11 shrink-0 place-items-center rounded-full bg-slate-100 text-slate-600 active:bg-slate-200"
                  >
                    <X className="h-5 w-5" />
                  </button>
                </div>

                {options.length > 7 && (
                  <div className="px-5 pb-3">
                    <div className="relative">
                      <Search className="pointer-events-none absolute left-4 top-1/2 h-5 w-5 -translate-y-1/2 text-slate-400" />
                      <input
                        autoFocus
                        value={query}
                        onChange={(event) => setQuery(event.target.value)}
                        placeholder={`Search ${(label || "options").toLowerCase()}...`}
                        className="h-12 w-full rounded-xl border border-slate-200 bg-slate-50 pl-12 pr-4 text-base outline-none focus:border-brand-500 focus:bg-white focus:ring-2 focus:ring-brand-100"
                      />
                    </div>
                  </div>
                )}

                <div role="listbox" className="overflow-y-auto overscroll-contain px-3 pb-[max(20px,env(safe-area-inset-bottom))]">
                  {filteredOptions.map((option) => {
                    const active = String(option.value) === selectedValue;
                    return (
                      <button
                        type="button"
                        role="option"
                        aria-selected={active}
                        key={option.value}
                        onClick={() => choose(String(option.value))}
                        className={cn(
                          "my-1 flex min-h-14 w-full items-center gap-3 rounded-2xl px-4 py-3 text-left text-base font-medium transition active:scale-[0.99]",
                          active
                            ? "bg-brand-600 text-white shadow-md shadow-brand-600/20"
                            : "text-slate-700 active:bg-slate-100",
                        )}
                      >
                        <span className="min-w-0 flex-1 break-words">{option.label}</span>
                        {active && <Check className="h-5 w-5 shrink-0" />}
                      </button>
                    );
                  })}
                  {!filteredOptions.length && (
                    <div className="px-4 py-10 text-center text-sm text-slate-500">
                      No matching options found.
                    </div>
                  )}
                </div>
              </section>
            </div>,
            document.body,
          )}
      </div>
    );
  },
);
Select.displayName = "Select";
