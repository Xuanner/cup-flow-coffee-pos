import { Package } from "lucide-react";

import { ModulePlaceholder } from "../../components/layout/ModulePlaceholder";

export function ProductsPage() {
  return (
    <ModulePlaceholder
      description="分类、商品、规格、加料和售卖状态维护入口。"
      icon={Package}
      title="商品管理"
    />
  );
}
