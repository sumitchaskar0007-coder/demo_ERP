import type { PropsWithChildren } from "react";
import { Toaster } from "sonner";
import { AuthProvider } from "@/features/auth/authStore";

export function AppProviders({ children }: PropsWithChildren) {
  return (
    <AuthProvider>
      {children}
      <Toaster richColors position="top-right" closeButton />
    </AuthProvider>
  );
}
