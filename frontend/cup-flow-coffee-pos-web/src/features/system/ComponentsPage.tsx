import { SearchIcon } from "lucide-react";
import { useState, type ReactNode } from "react";

import {
  Badge,
  Button,
  Dialog,
  DialogClose,
  EmptyState,
  Input,
  Pagination,
  Progress,
  Search,
  Select,
  Skeleton,
  Spinner,
  Table,
  TableBody,
  TableCell,
  TableHead,
  TableHeader,
  TableRow,
  ToastMessage,
} from "../../components/ui";

export function ComponentsPage() {
  const [query, setQuery] = useState("拿铁");
  const [page, setPage] = useState(1);
  const [toastOpen, setToastOpen] = useState(false);

  return (
    <section className="mx-auto grid max-w-desktop gap-cf-lg p-cf-md sm:p-cf-lg lg:p-cf-xl">
      <header>
        <p className="mb-cf-xs font-medium text-brand">US-S1-FE-03</p>
        <h1 className="text-page-title font-semibold">通用组件与交互状态</h1>
        <p className="mt-cf-xs text-secondary">
          组件样式来自 Cup Flow Components v1.0，所有视觉值通过 tokens.css
          映射。
        </p>
      </header>

      <ComponentSection title="Button">
        <div className="flex flex-wrap gap-cf-xs">
          <Button>确认收款</Button>
          <Button variant="secondary">取消</Button>
          <Button variant="ghost">稍后处理</Button>
          <Button variant="danger">取消订单</Button>
          <Button disabled>禁用</Button>
          <Button loading>保存</Button>
        </div>
      </ComponentSection>

      <ComponentSection title="Input / Search / Select">
        <div className="grid gap-cf-md md:grid-cols-3">
          <Input
            error="名称最多 20 个字"
            label="商品名称"
            placeholder="请输入商品名称"
          />
          <Search
            error="搜索失败，请重试"
            label="搜索商品名称或订单号"
            onChange={(event) => setQuery(event.target.value)}
            onClear={() => setQuery("")}
            value={query}
          />
          <Select
            error="此项为必选"
            label="订单状态"
            options={[
              { label: "待支付", value: "pending" },
              { label: "已支付", value: "paid" },
              { disabled: true, label: "已归档（不可用）", value: "archived" },
            ]}
            placeholder="请选择状态"
          />
        </div>
      </ComponentSection>

      <ComponentSection title="Badge / Table / Pagination">
        <div className="mb-cf-md flex flex-wrap gap-cf-xs">
          <Badge dot>待支付</Badge>
          <Badge tone="brand">Cup Flow</Badge>
          <Badge dot tone="success">
            已完成
          </Badge>
          <Badge dot tone="warning">
            需确认
          </Badge>
          <Badge dot tone="error">
            已取消
          </Badge>
          <Badge dot tone="info">
            已同步
          </Badge>
        </div>
        <Table>
          <caption className="sr-only">订单示例</caption>
          <TableHeader>
            <TableRow>
              <TableHead>订单号</TableHead>
              <TableHead>状态</TableHead>
              <TableHead>金额</TableHead>
              <TableHead>操作</TableHead>
            </TableRow>
          </TableHeader>
          <TableBody>
            <TableRow>
              <TableCell>CF-20260807-018</TableCell>
              <TableCell>
                <Badge tone="success">已完成</Badge>
              </TableCell>
              <TableCell>¥ 32.00</TableCell>
              <TableCell>
                <Button
                  aria-label="查看订单 CF-20260807-018"
                  size="compact"
                  variant="ghost"
                >
                  查看
                </Button>
              </TableCell>
            </TableRow>
          </TableBody>
        </Table>
        <Pagination
          className="mt-cf-md"
          onPageChange={setPage}
          page={page}
          pageCount={12}
          summary="第 1–20 条，共 228 条"
        />
      </ComponentSection>

      <ComponentSection title="Dialog / Toast">
        <div className="flex flex-wrap gap-cf-xs">
          <Dialog
            description="修改商品信息，保存后仅影响新订单。"
            error="修改尚未保存，请检查后重试"
            footer={
              <>
                <DialogClose asChild>
                  <Button variant="secondary">取消</Button>
                </DialogClose>
                <Button loading>保存</Button>
              </>
            }
            title="编辑商品"
            trigger={<Button variant="secondary">打开对话框</Button>}
          >
            <Input defaultValue="海盐拿铁" label="商品名称" />
          </Dialog>
          <Button onClick={() => setToastOpen(true)}>显示通知</Button>
          <ToastMessage
            action="重试"
            actionAltText="重试保存"
            description="修改尚未保存，请重试"
            onOpenChange={setToastOpen}
            open={toastOpen}
            title="保存失败"
            tone="error"
          />
        </div>
      </ComponentSection>

      <ComponentSection title="EmptyState / Loading">
        <div className="grid gap-cf-md md:grid-cols-2">
          <EmptyState
            description="尝试更换关键词或清除筛选条件"
            icon={<SearchIcon aria-hidden="true" className="size-icon-lg" />}
            primaryAction={<Button variant="secondary">清除筛选</Button>}
            title="未找到匹配结果"
          />
          <div className="grid content-center gap-cf-lg rounded-panel border border-subtle-border bg-surface p-cf-xl">
            <Spinner />
            <Progress label="正在导入" value={68} />
            <div className="grid gap-cf-xs" aria-label="内容加载占位">
              <Skeleton className="h-5 w-1/3" />
              <Skeleton className="h-4 w-full" />
              <Skeleton className="h-4 w-4/5" />
            </div>
          </div>
        </div>
      </ComponentSection>
    </section>
  );
}

function ComponentSection({
  children,
  title,
}: {
  children: ReactNode;
  title: string;
}) {
  return (
    <section className="rounded-panel border border-subtle-border bg-surface p-cf-lg shadow-sm">
      <h2 className="mb-cf-md text-module-title font-semibold">{title}</h2>
      {children}
    </section>
  );
}
