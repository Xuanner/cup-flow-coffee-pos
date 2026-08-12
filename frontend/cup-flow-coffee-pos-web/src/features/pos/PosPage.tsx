import { ShoppingCart } from "lucide-react";

import { ModulePlaceholder } from "../../components/layout/ModulePlaceholder";

export function PosPage() {
  return (
    <ModulePlaceholder
      description="商品浏览、定制、购物车、收款与找零的工作区。"
      icon={ShoppingCart}
      title="收银台"
    />
  );
}
