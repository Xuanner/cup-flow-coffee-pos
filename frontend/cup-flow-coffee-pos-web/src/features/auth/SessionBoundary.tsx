import { Navigate, Outlet, useLocation } from "react-router";

import { Button } from "../../components/ui/Button";
import { Spinner } from "../../components/ui/Loading";
import { useAuthProvider } from "./auth-provider-context";
import { useAuthStore } from "./auth-store";

export function SessionGate() {
  const sessionStatus = useAuthStore((state) => state.sessionStatus);
  const sessionError = useAuthStore((state) => state.sessionError);
  const { retrySessionCheck } = useAuthProvider();

  if (sessionStatus === "checking") {
    return (
      <main className="grid min-h-screen place-items-center bg-canvas p-4">
        <Spinner label="正在确认登录状态…" />
      </main>
    );
  }
  if (sessionStatus === "error") {
    return (
      <main className="grid min-h-screen place-items-center bg-canvas p-4">
        <section className="w-full max-w-md rounded-panel border border-subtle-border bg-surface p-8 text-center shadow-sm">
          <h1 className="text-xl font-semibold">暂时无法确认登录状态</h1>
          <p className="mt-3 text-sm text-secondary" role="alert">
            {sessionError}
          </p>
          <Button className="mt-6" onClick={retrySessionCheck}>
            重新检查
          </Button>
        </section>
      </main>
    );
  }
  return <Outlet />;
}

export function ProtectedSessionRoute() {
  const sessionStatus = useAuthStore((state) => state.sessionStatus);
  const location = useLocation();
  if (sessionStatus !== "authenticated") {
    const from = `${location.pathname}${location.search}${location.hash}`;
    return <Navigate replace state={{ from }} to="/login" />;
  }
  return <Outlet />;
}
