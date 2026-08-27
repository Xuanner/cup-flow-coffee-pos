import { safeReturnPath } from "./safe-return-path";

const cashier = { roles: ["CASHIER"], defaultPath: "/pos" as const };
const admin = { roles: ["ADMIN"], defaultPath: "/dashboard" as const };

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
