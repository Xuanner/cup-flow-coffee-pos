import type { CurrentUser } from "./auth-model";

export type ApplicationRole = CurrentUser["roles"][number];

export function hasRole(
  user: Pick<CurrentUser, "roles">,
  requiredRole: ApplicationRole,
): boolean {
  return (
    user.roles.includes(requiredRole) ||
    (requiredRole === "CASHIER" && user.roles.includes("ADMIN"))
  );
}
