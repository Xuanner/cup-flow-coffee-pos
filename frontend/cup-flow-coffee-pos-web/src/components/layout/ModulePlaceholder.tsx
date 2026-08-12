import type { LucideIcon } from "lucide-react";
import type { ReactNode } from "react";

interface ModulePlaceholderProps {
  title: string;
  description: string;
  icon: LucideIcon;
  children?: ReactNode;
}

export function ModulePlaceholder({
  children,
  description,
  icon: Icon,
  title,
}: ModulePlaceholderProps) {
  return (
    <section className="mx-auto max-w-screen-2xl p-4 sm:p-6 lg:p-8">
      <header className="mb-6">
        <p className="mb-2 text-sm font-medium text-brand">
          Cup Flow Coffee POS
        </p>
        <h1 className="text-2xl font-semibold tracking-tight sm:text-3xl">
          {title}
        </h1>
        <p className="mt-2 max-w-2xl text-sm leading-6 text-secondary sm:text-base">
          {description}
        </p>
      </header>
      <div className="grid min-h-80 place-items-center rounded-panel border border-dashed border-default bg-surface p-8 text-center">
        <div className="max-w-md">
          <span className="mx-auto grid size-14 place-items-center rounded-panel bg-subtle text-secondary">
            <Icon aria-hidden="true" size={28} />
          </span>
          <h2 className="mt-4 text-base font-semibold">模块边界已就绪</h2>
          <p className="mt-2 text-sm leading-6 text-secondary">
            本 Sprint 仅提供可运行的页面入口，业务功能将在后续用户故事中实现。
          </p>
          {children}
        </div>
      </div>
    </section>
  );
}
