import { Coffee } from "lucide-react";
import { Link } from "react-router";

import { Button } from "../../components/ui/Button";

export function AuthPage() {
  return (
    <main className="grid min-h-screen place-items-center bg-canvas p-4">
      <section className="w-full max-w-md rounded-panel border border-subtle-border bg-surface p-8 shadow-sm">
        <span className="grid size-12 place-items-center rounded-full bg-brand-soft text-brand">
          <Coffee aria-hidden="true" />
        </span>
        <p className="mt-6 text-sm font-medium text-brand">
          Cup Flow Coffee POS
        </p>
        <h1 className="mt-2 text-2xl font-semibold">登录模块</h1>
        <p className="mt-3 text-sm leading-6 text-secondary">
          登录体验将在后续用户故事中实现。本页面用于验证独立布局与路由边界。
        </p>
        <Button asChild className="mt-8 w-full">
          <Link to="/pos">进入前端骨架</Link>
        </Button>
      </section>
    </main>
  );
}
