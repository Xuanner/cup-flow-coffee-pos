import { ShieldAlert } from "lucide-react";
import { Link } from "react-router";

import { ModulePlaceholder } from "../../components/layout/ModulePlaceholder";
import { Button } from "../../components/ui/Button";

export function ForbiddenPage() {
  return (
    <ModulePlaceholder
      description="当前账号没有访问此页面的权限。"
      icon={ShieldAlert}
      title="403 · 没有访问权限"
    >
      <Button asChild className="mt-6">
        <Link to="/pos">返回收银台</Link>
      </Button>
    </ModulePlaceholder>
  );
}
