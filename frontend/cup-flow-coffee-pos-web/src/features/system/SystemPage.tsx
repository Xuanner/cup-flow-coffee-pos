import { Settings } from "lucide-react";

import { ModulePlaceholder } from "../../components/layout/ModulePlaceholder";

export function SystemPage() {
  return (
    <ModulePlaceholder
      description="健康检查、权限、会话以及全局异常状态入口。"
      icon={Settings}
      title="系统状态"
    />
  );
}
