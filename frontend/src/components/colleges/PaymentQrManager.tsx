import { useEffect, useState } from "react";
import { CheckCircle2, QrCode, Upload } from "lucide-react";
import { toast } from "sonner";
import { Button } from "@/components/common/Button";
import { Input } from "@/components/common/Input";
import { handleApiError } from "@/lib/handleApiError";
import type { PaymentQrSettings } from "@/features/colleges/types";
import { getPaymentQrSettings, updatePaymentQr } from "@/features/colleges/api";

export function PaymentQrManager({ collegeId }: { collegeId?: number }) {
  const [settings, setSettings] = useState<PaymentQrSettings | null>(null);
  const [file, setFile] = useState<File | null>(null);
  const [accountName, setAccountName] = useState("");
  const [saving, setSaving] = useState(false);
  const [previewVersion, setPreviewVersion] = useState(Date.now());

  useEffect(() => {
    getPaymentQrSettings(collegeId)
      .then((current) => {
        setSettings(current);
        setAccountName(current.accountName ?? "");
      })
      .catch((error) => toast.error(handleApiError(error).message));
  }, [collegeId]);

  const choose = (next: File | undefined) => {
    if (!next) return setFile(null);
    if (!["image/jpeg", "image/png"].includes(next.type)) {
      toast.error("Choose a JPG or PNG QR-code image");
      return;
    }
    if (next.size > 5 * 1024 * 1024) {
      toast.error("QR-code image must not exceed 5 MB");
      return;
    }
    setFile(next);
  };

  const save = async () => {
    if (!file) return;
    const normalizedAccountName = accountName.trim();
    if (!normalizedAccountName) {
      toast.error("Enter the exact account name shown after scanning the QR code");
      return;
    }
    setSaving(true);
    try {
      const updated = await updatePaymentQr(file, normalizedAccountName, collegeId);
      setSettings(updated);
      setAccountName(updated.accountName ?? normalizedAccountName);
      setFile(null);
      setPreviewVersion(Date.now());
      toast.success("Payment QR code updated successfully");
    } catch (error) {
      toast.error(handleApiError(error).message);
    } finally {
      setSaving(false);
    }
  };

  return (
    <div className="grid gap-6 lg:grid-cols-[260px_1fr]">
      <div className="grid min-h-64 place-items-center rounded-2xl border bg-slate-50 p-4">
        {settings?.qrCodeUrl ? (
          <img
            src={`${settings.qrCodeUrl}&v=${previewVersion}`}
            alt="Current college payment QR code"
            className="h-56 w-56 rounded-xl bg-white object-contain p-2 shadow-sm"
          />
        ) : (
          <div className="text-center text-slate-400">
            <QrCode className="mx-auto h-12 w-12" />
            <p className="mt-2 text-sm">No payment QR configured</p>
          </div>
        )}
      </div>
      <div>
        <div className="flex items-start gap-3">
          <div className="rounded-xl bg-blue-50 p-2.5 text-blue-700">
            <QrCode className="h-5 w-5" />
          </div>
          <div>
            <h3 className="text-lg font-bold">College payment QR code</h3>
            <p className="mt-1 text-sm text-slate-500">
              Students will see this QR code while submitting fee payment proof.
            </p>
          </div>
        </div>
        <Input
          className="mt-5"
          label="QR account name"
          maxLength={150}
          value={accountName}
          onChange={(event) => setAccountName(event.target.value)}
          placeholder="Exact name shown after scanning the QR code"
        />
        <p className="mt-1 text-xs text-amber-700">
          Students will be warned to pay only when this exact account name appears.
        </p>
        <label className="mt-5 flex cursor-pointer flex-col items-center rounded-2xl border-2 border-dashed border-blue-200 bg-blue-50/50 p-7 text-center hover:border-blue-400">
          <Upload className="h-6 w-6 text-blue-600" />
          <span className="mt-2 text-sm font-bold">Choose new QR-code image</span>
          <span className="mt-1 text-xs text-slate-500">JPG or PNG - maximum 5 MB</span>
          <input
            type="file"
            accept="image/jpeg,image/png"
            className="sr-only"
            onChange={(event) => choose(event.target.files?.[0])}
          />
        </label>
        {file && (
          <div className="mt-4 flex flex-wrap items-center justify-between gap-3 rounded-xl border border-emerald-200 bg-emerald-50 p-4">
            <div className="flex min-w-0 items-center gap-2 text-sm">
              <CheckCircle2 className="h-5 w-5 shrink-0 text-emerald-600" />
              <span className="truncate font-semibold">{file.name}</span>
            </div>
            <Button loading={saving} disabled={!accountName.trim()} onClick={() => void save()}>
              Update QR Code
            </Button>
          </div>
        )}
      </div>
    </div>
  );
}
