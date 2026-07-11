import { ChevronLeft, ChevronRight } from "lucide-react";
import { Button } from "./Button";

export function Pagination({
  page,
  totalPages,
  totalElements,
  onChange,
}: {
  page: number;
  totalPages: number;
  totalElements: number;
  onChange: (page: number) => void;
}) {
  if (totalPages <= 1)
    return totalElements ? (
      <p className="text-sm text-slate-500">
        {totalElements} result{totalElements === 1 ? "" : "s"}
      </p>
    ) : null;
  return (
    <div className="flex flex-col items-center justify-between gap-3 sm:flex-row">
      <p className="text-sm text-slate-500">
        {totalElements} results · Page {page + 1} of {totalPages}
      </p>
      <div className="flex gap-2">
        <Button variant="secondary" disabled={page === 0} onClick={() => onChange(page - 1)}>
          <ChevronLeft className="h-4 w-4" /> Previous
        </Button>
        <Button
          variant="secondary"
          disabled={page + 1 >= totalPages}
          onClick={() => onChange(page + 1)}
        >
          Next <ChevronRight className="h-4 w-4" />
        </Button>
      </div>
    </div>
  );
}
