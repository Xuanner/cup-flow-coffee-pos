import { QueryClientProvider } from "@tanstack/react-query";
import type { PropsWithChildren } from "react";

import { ToastProvider } from "../components/ui/Toast";
import { queryClient } from "../lib/query/query-client";

export function AppProviders({ children }: PropsWithChildren) {
  return (
    <QueryClientProvider client={queryClient}>
      <ToastProvider>{children}</ToastProvider>
    </QueryClientProvider>
  );
}
