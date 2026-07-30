import { Building2, KeyRound, Mail, Phone, Save, UserRound } from "lucide-react";
import { useEffect, useState } from "react";
import { Link } from "react-router-dom";
import { toast } from "sonner";
import { Card } from "@/components/common/Card";
import { Input } from "@/components/common/Input";
import { Button } from "@/components/common/Button";
import { Loader } from "@/components/common/Loader";
import { handleApiError } from "@/lib/handleApiError";
import { getMyAccount, updateMyAccount, type AccountProfile } from "@/features/account/api";
export function AccountPage() {
  const [p, setP] = useState<AccountProfile | null>(null),
    [saving, setSaving] = useState(false);
  useEffect(() => {
    getMyAccount()
      .then(setP)
      .catch((e) => toast.error(handleApiError(e).message));
  }, []);
  if (!p) return <Loader />;
  const save = async () => {
    if (p.fullName.trim().length < 2) return toast.error("Full name is required");
    setSaving(true);
    try {
      setP(await updateMyAccount({ fullName: p.fullName, phone: p.phone }));
      toast.success("Profile updated");
    } catch (e) {
      toast.error(handleApiError(e).message);
    } finally {
      setSaving(false);
    }
  };
  return (
    <div className="page-container pb-10">
      <div className="border-b border-slate-200 pb-6">
        <h1 className="page-title">My Account</h1>
        <p className="page-subtitle">Keep your personal information and password up to date.</p>
      </div>

      <Card className="mt-6 max-w-3xl overflow-hidden">
        <div className="bg-gradient-to-r from-brand-600 to-indigo-700 px-6 py-7 text-white sm:px-8">
          <div className="flex items-center gap-4">
            <span className="grid h-14 w-14 shrink-0 place-items-center rounded-2xl bg-white/15 ring-1 ring-white/25">
              <UserRound className="h-6 w-6" />
            </span>
            <div className="min-w-0">
              <h2 className="truncate text-xl font-bold">{p.fullName}</h2>
              <p className="mt-1 truncate text-sm text-white/75">{p.email}</p>
            </div>
          </div>
        </div>

        <div className="p-6 sm:p-8">
          <div className="mb-6">
            <h3 className="font-bold text-slate-900">Personal details</h3>
            <p className="mt-1 text-sm text-slate-500">
              Update the contact information associated with your account.
            </p>
          </div>

          <div className="grid gap-5 sm:grid-cols-2">
            <Input
              label="Full name"
              icon={<UserRound className="h-4 w-4" />}
              value={p.fullName}
              onChange={(e) => setP({ ...p, fullName: e.target.value })}
            />
            <Input
              label="Email address"
              icon={<Mail className="h-4 w-4" />}
              value={p.email}
              disabled
            />
            <Input
              label="Phone number"
              icon={<Phone className="h-4 w-4" />}
              value={p.phone || ""}
              onChange={(e) => setP({ ...p, phone: e.target.value })}
            />
            <Input
              label="College"
              icon={<Building2 className="h-4 w-4" />}
              value={p.collegeName || "System"}
              disabled
            />
          </div>

          <div className="mt-7 flex flex-col-reverse gap-3 border-t border-slate-200 pt-6 sm:flex-row sm:justify-end">
            <Link to="/account/change-password" className="sm:mr-auto">
              <Button variant="secondary" className="w-full sm:w-auto">
                <KeyRound className="h-4 w-4" />
                Change password
              </Button>
            </Link>
            <Button loading={saving} onClick={save}>
              <Save className="h-4 w-4" />
              Save changes
            </Button>
          </div>
        </div>
      </Card>
    </div>
  );
}
