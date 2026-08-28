import type { CurrentUser } from "./auth-model";
import { safeReturnPath } from "./safe-return-path";

const cashier = {
  roles: ["CASHIER"],
  defaultPath: "/pos",
} satisfies Pick<CurrentUser, "roles" | "defaultPath">;
const admin = {
  roles: ["ADMIN"],
  defaultPath: "/dashboard",
} satisfies Pick<CurrentUser, "roles" | "defaultPath">;

describe("safeReturnPath", () => {
  it("允许当前角色可访问的站内已知路径", () => {
    expect(safeReturnPath("/orders?status=open#latest", cashier)).toBe(
      "/orders?status=open#latest",
    );
    expect(safeReturnPath("/products", admin)).toBe("/products");
  });

  it.each([
    "https://evil.example/steal",
    "//evil.example/steal",
    "/\\evil",
    "/%2f%2fevil.example",
    "/unknown",
    "/dashboard",
    "/orders?sessionToken=secret",
  ])("拒绝不安全、未知或无权路径 %s", (candidate) => {
    expect(safeReturnPath(candidate, cashier)).toBe("/pos");
  });
});
