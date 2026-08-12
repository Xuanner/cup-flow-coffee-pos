import { ReceiptText } from "lucide-react";

import { ModulePlaceholder } from "../../components/layout/ModulePlaceholder";

export function OrdersPage() {
  return (
    <ModulePlaceholder
      description="订单列表、详情、状态筛选与订单操作入口。"
      icon={ReceiptText}
      title="订单"
    />
  );
}
