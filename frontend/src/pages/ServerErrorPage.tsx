import { ServerCrash } from "lucide-react";
import { Button } from "@/components/common/Button";
export function ServerErrorPage() {
  return (
    <div className="grid min-h-screen place-items-center bg-slate-50 p-6 text-center">
      <div>
        <ServerCrash className="mx-auto h-16 w-16 text-red-500" />
        <h1 className="mt-5 text-2xl font-bold">Something went wrong</h1>
        <p className="mt-2 text-slate-500">The server could not complete your request.</p>
        <Button className="mt-6" onClick={() => window.location.assign("/dashboard")}>
          Try again
        </Button>
      </div>
    </div>
  );
}
