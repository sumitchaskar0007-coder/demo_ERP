import { CircleAlert } from "lucide-react";

export function ErrorMessage({ message }: { message: string }) {
  return (
    <div className="flex items-start gap-3 rounded-xl border border-red-200 bg-red-50 p-4 text-sm text-red-700">
      <CircleAlert className="mt-0.5 h-4 w-4 shrink-0" />
      <span>{message}</span>
    </div>
  );
}
