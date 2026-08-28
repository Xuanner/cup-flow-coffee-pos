import {
  BarChart3,
  Coffee,
  Menu,
  Package,
  ReceiptText,
  Settings,
  ShoppingCart,
  LogOut,
  X,
} from "lucide-react";
import { useState } from "react";
import { NavLink, Outlet } from "react-router";

import { logout } from "../../features/auth/auth-api";
import { useAuthStore } from "../../features/auth/auth-store";
import { ApiError } from "../../lib/api/api-error";
import { useAppUiStore } from "../../state/app-ui-store";
import { Button } from "../ui/Button";

const navigation = [
  { label: "收银台", to: "/pos", icon: ShoppingCart },
  { label: "订单", to: "/orders", icon: ReceiptText },
  { label: "商品", to: "/products", icon: Package },
  { label: "经营看板", to: "/dashboard", icon: BarChart3 },
  { label: "系统状态", to: "/system", icon: Settings },
];

export function AppShell() {
  const { closeNavigation, isNavigationOpen, toggleNavigation } =
    useAppUiStore();
  const currentUser = useAuthStore((state) => state.currentUser);
  const clearCurrentUser = useAuthStore((state) => state.clearCurrentUser);
  const [loggingOut, setLoggingOut] = useState(false);
  const [logoutError, setLogoutError] = useState("");

  async function handleLogout() {
    if (loggingOut) return;
    setLoggingOut(true);
    setLogoutError("");
    try {
      await logout();
    } catch (error) {
      setLogoutError(
        error instanceof ApiError
          ? error.message
          : "暂时无法退出，请检查网络后重试。",
      );
      setLoggingOut(false);
      return;
    }
    clearCurrentUser();
  }

  return (
    <div className="min-h-screen bg-canvas text-primary">
      <a
        className="sr-only z-50 rounded-control bg-action-primary p-3 text-on-action focus:not-sr-only focus:fixed focus:top-4 focus:left-4"
        href="#main-content"
      >
        跳到主要内容
      </a>

      <header className="sticky top-0 z-30 flex h-16 items-center justify-between border-b border-subtle-border bg-surface px-4 lg:hidden">
        <Brand />
        <Button
          aria-expanded={isNavigationOpen}
          aria-label={isNavigationOpen ? "关闭导航" : "打开导航"}
          onClick={toggleNavigation}
          variant="ghost"
        >
          {isNavigationOpen ? (
            <X aria-hidden="true" />
          ) : (
            <Menu aria-hidden="true" />
          )}
        </Button>
      </header>

      {isNavigationOpen ? (
        <button
          aria-label="关闭导航遮罩"
          className="fixed inset-0 z-30 bg-black/30 lg:hidden"
          onClick={closeNavigation}
          type="button"
        />
      ) : null}

      <aside
        className={`fixed inset-y-0 left-0 z-40 flex w-64 flex-col border-r border-subtle-border bg-surface transition-transform lg:translate-x-0 ${isNavigationOpen ? "translate-x-0" : "-translate-x-full"}`}
      >
        <div className="flex h-20 items-center border-b border-subtle-border px-6">
          <Brand />
        </div>
        <nav aria-label="主导航" className="flex-1 space-y-1 p-4">
          {navigation.map(({ icon: Icon, label, to }) => (
            <NavLink
              className={({ isActive }) =>
                `flex min-h-touch items-center gap-3 rounded-control px-3 text-sm font-medium transition-colors ${
                  isActive
                    ? "bg-selected text-primary"
                    : "text-secondary hover:bg-subtle hover:text-primary"
                }`
              }
              key={to}
              onClick={closeNavigation}
              to={to}
            >
              <Icon aria-hidden="true" size={20} />
              {label}
            </NavLink>
          ))}
        </nav>
        <div className="border-t border-subtle-border p-4">
          <p className="truncate text-sm font-medium text-primary">
            {currentUser?.displayName}
          </p>
          <p className="mt-1 text-xs text-tertiary">
            {currentUser?.roles
              .map((role) => (role === "ADMIN" ? "管理员" : "收银员"))
              .join("、")}
          </p>
          {logoutError ? (
            <p className="mt-3 text-xs text-error" role="alert">
              {logoutError}
            </p>
          ) : null}
          <Button
            className="mt-3 w-full"
            loading={loggingOut}
            loadingLabel="正在退出"
            onClick={handleLogout}
            size="compact"
            variant="secondary"
          >
            <LogOut aria-hidden="true" className="size-icon-sm" />
            退出登录
          </Button>
        </div>
      </aside>

      <main className="lg:pl-64" id="main-content">
        <Outlet />
      </main>
    </div>
  );
}

function Brand() {
  return (
    <div className="flex items-center gap-3" aria-label="Cup Flow Coffee POS">
      <span className="grid size-10 place-items-center rounded-full bg-brand-soft text-brand">
        <Coffee aria-hidden="true" size={22} />
      </span>
      <span>
        <strong className="block text-sm font-semibold tracking-tight">
          Cup Flow
        </strong>
        <span className="block text-xs text-tertiary">Coffee POS</span>
      </span>
    </div>
  );
}
