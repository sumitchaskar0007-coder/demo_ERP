import { Link } from "react-router-dom";
import { Button } from "@/components/common/Button";
import { ROUTES } from "@/lib/constants";
export function NotFoundPage() {
  return <div className="grid min-h-screen place-items-center bg-slate-50 p-6 text-center"><div><p className="text-7xl font-black text-brand-600">404</p><h1 className="mt-4 text-2xl font-bold">Page not found</h1><p className="mt-2 text-slate-500">The page you requested does not exist.</p><Link to={ROUTES.dashboard}><Button className="mt-6">Go to dashboard</Button></Link></div></div>;
}
