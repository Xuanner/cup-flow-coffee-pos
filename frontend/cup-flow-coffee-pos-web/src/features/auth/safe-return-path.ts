import type { CurrentUser } from "./auth-model";

const CASHIER_PATHS = new Set(["/pos", "/orders"]);
const ADMIN_PATHS = new Set(["/pos", "/orders", "/products", "/dashboard"]);
const UNSAFE_VALUE = /[\\%]/;
const SENSITIVE_PARAMETER = /(?:csrf|xsrf|session|token|credential)/i;

export function safeReturnPath(
  candidate: unknown,
  user: Pick<CurrentUser, "roles" | "defaultPath">,
): string {
  if (typeof candidate !== "string" || !isSafeCandidate(candidate)) {
    return user.defaultPath;
  }

  const parsed = new URL(candidate, "https://cup-flow.invalid");
  const allowed = user.roles.includes("ADMIN") ? ADMIN_PATHS : CASHIER_PATHS;
  if (!allowed.has(parsed.pathname)) return user.defaultPath;
  if (
    [...parsed.searchParams.keys()].some((key) => SENSITIVE_PARAMETER.test(key))
  ) {
    return user.defaultPath;
  }
  return `${parsed.pathname}${parsed.search}${parsed.hash}`;
}

function isSafeCandidate(candidate: string): boolean {
  return (
    candidate.startsWith("/") &&
    !candidate.startsWith("//") &&
    !UNSAFE_VALUE.test(candidate) &&
    ![...candidate].some((character) => {
      const codePoint = character.codePointAt(0) ?? 0;
      return codePoint < 32 || codePoint === 127;
    })
  );
}
