import { useEffect, useRef, useState } from "react";
import { CheckCircle2, Circle } from "lucide-react";
import { Link, useNavigate, useSearchParams } from "react-router-dom";
import { toast } from "sonner";
import { Card } from "@/components/common/Card";
import { Button } from "@/components/common/Button";
import { Input } from "@/components/common/Input";
import { handleApiError } from "@/lib/handleApiError";
import { ROUTES } from "@/lib/constants";
import * as api from "@/features/auth/api";
import { isValidAccountPassword, passwordRequirements } from "@/features/auth/passwordRules";

export function ForgotPasswordPage() {
  const [email, setEmail] = useState("");
  const [done, setDone] = useState(false);
  const [loading, setLoading] = useState(false);
  const submit = async () => {
    if (!/^\S+@\S+\.\S+$/.test(email)) return toast.error("Enter a valid email");
    setLoading(true);
    try {
      await api.forgotPassword(email);
      setDone(true);
    } catch (e) {
      toast.error(handleApiError(e).message);
    } finally {
      setLoading(false);
    }
  };
  return (
    <AuthCard title="Forgot password" subtitle="Enter your email to receive a secure reset link.">
      {done ? (
        <>
          <p className="rounded-xl bg-emerald-50 p-4 text-sm text-emerald-700">
            If an account exists for this email address, a password reset link will be sent.
          </p>
          <Link to={ROUTES.login}>
            <Button className="mt-4 w-full">Back to login</Button>
          </Link>
        </>
      ) : (
        <>
          <Input
            label="Email"
            type="email"
            autoComplete="email"
            value={email}
            onChange={(e) => setEmail(e.target.value)}
          />
          <Button className="mt-5 w-full" loading={loading} onClick={submit}>
            Send reset link
          </Button>
        </>
      )}
    </AuthCard>
  );
}
export function ResetPasswordPage() {
  const [p] = useSearchParams();
  const nav = useNavigate();
  const token = p.get("token") || "";
  const [password, setPassword] = useState("");
  const [confirm, setConfirm] = useState("");
  const [loading, setLoading] = useState(false);
  const requirements = passwordRequirements(password);
  const submit = async () => {
    if (!token) return toast.error("Reset token is missing");
    if (password !== confirm) return toast.error("Passwords do not match");
    if (!isValidAccountPassword(password)) {
      return toast.error("Password does not meet all the requirements shown below");
    }
    setLoading(true);
    try {
      await api.resetPassword(token, password);
      window.history.replaceState({}, "", ROUTES.resetPassword);
      toast.success("Password reset successfully");
      nav(ROUTES.login, { replace: true });
    } catch (e) {
      toast.error(handleApiError(e).message);
    } finally {
      setLoading(false);
    }
  };
  return (
    <AuthCard title="Reset password" subtitle="Choose a strong new password for your account.">
      <Input
        label="New password"
        type="password"
        autoComplete="new-password"
        value={password}
        onChange={(e) => setPassword(e.target.value)}
      />
      <div
        aria-label="Password requirements"
        className="mt-3 space-y-2 rounded-xl border border-slate-200 bg-slate-50 p-3"
      >
        <p className="text-xs font-bold text-slate-700">Your password must contain:</p>
        {requirements.map(({ label, met }) => {
          const Icon = met ? CheckCircle2 : Circle;
          return (
            <div
              key={label}
              className={`flex items-start gap-2 text-xs font-medium ${met ? "text-emerald-700" : "text-slate-500"}`}
            >
              <Icon className="mt-0.5 h-3.5 w-3.5 shrink-0" aria-hidden="true" />
              <span>{label}</span>
            </div>
          );
        })}
      </div>
      <Input
        className="mt-4"
        label="Confirm password"
        type="password"
        autoComplete="new-password"
        value={confirm}
        onChange={(e) => setConfirm(e.target.value)}
      />
      <Button className="mt-5 w-full" loading={loading} onClick={submit}>
        Reset password
      </Button>
    </AuthCard>
  );
}
export function VerifyEmailPage() {
  const [p] = useSearchParams();
  const token = p.get("token") || "";
  const [status, setStatus] = useState("Verifying your email…");
  const once = useRef(false);
  useEffect(() => {
    if (once.current) return;
    once.current = true;
    if (!token) {
      setStatus("Verification token is missing.");
      return;
    }
    api
      .confirmEmailVerification(token)
      .then(() => setStatus("Your email has been verified successfully."))
      .catch((e) => setStatus(handleApiError(e).message))
      .finally(() => window.history.replaceState({}, "", ROUTES.verifyEmail));
  }, [token]);
  return (
    <AuthCard title="Email verification" subtitle={status}>
      <Link to={ROUTES.login}>
        <Button className="mt-5 w-full">Continue to login</Button>
      </Link>
    </AuthCard>
  );
}
function AuthCard({
  title,
  subtitle,
  children,
}: {
  title: string;
  subtitle: string;
  children: React.ReactNode;
}) {
  return (
    <div className="grid min-h-[100dvh] place-items-center bg-slate-50 p-3 sm:p-4">
      <Card className="w-full max-w-md p-5 sm:p-7">
        <h1 className="text-xl font-black sm:text-2xl">{title}</h1>
        <p className="mb-6 mt-2 text-sm text-slate-500">{subtitle}</p>
        {children}
      </Card>
    </div>
  );
}
