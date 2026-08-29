import { Navigate, Outlet } from "react-router";

import type { ApplicationRole } from "./authorization";
import { hasRole } from "./authorization";
import { useAuthStore } from "./auth-store";

interface RoleBoundaryProps {
  requiredRole: ApplicationRole;
}

export function RoleBoundary({ requiredRole }: RoleBoundaryProps) {
  const currentUser = useAuthStore((state) => state.currentUser);
  const sessionStatus = useAuthStore((state) => state.sessionStatus);

  if (sessionStatus !== "authenticated" || !currentUser) {
    return <Navigate replace to="/login" />;
  }
  if (!hasRole(currentUser, requiredRole)) {
    return <Navigate replace to="/forbidden" />;
  }
  return <Outlet />;
}
