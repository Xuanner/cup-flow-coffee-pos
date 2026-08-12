import { BarChart3 } from "lucide-react";

import { ModulePlaceholder } from "../../components/layout/ModulePlaceholder";

export function DashboardPage() {
  return (
    <ModulePlaceholder
      description="销售额、订单数、客单价、取消数据与商品销量排行。"
      icon={BarChart3}
      title="经营看板"
    />
  );
}
