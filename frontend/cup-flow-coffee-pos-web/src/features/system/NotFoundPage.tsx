import { CircleHelp } from "lucide-react";
import { Link } from "react-router";

import { ModulePlaceholder } from "../../components/layout/ModulePlaceholder";
import { Button } from "../../components/ui/Button";

export function NotFoundPage() {
  return (
    <ModulePlaceholder
      description="你访问的页面不存在或已经移动。"
      icon={CircleHelp}
      title="404 · 页面不存在"
    >
      <Button asChild className="mt-6">
        <Link to="/pos">返回收银台</Link>
      </Button>
    </ModulePlaceholder>
  );
}
