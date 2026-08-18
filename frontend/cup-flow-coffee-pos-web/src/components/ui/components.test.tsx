import { render, screen } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { useState } from "react";
import { describe, expect, it, vi } from "vitest";

import { Button } from "./Button";
import { Dialog, DialogClose } from "./Dialog";
import { Input } from "./Input";
import { Pagination } from "./Pagination";
import { Search } from "./Search";
import { Select } from "./Select";
import { ToastMessage, ToastProvider } from "./Toast";

describe("通用组件状态", () => {
  it("Button 支持键盘焦点、禁用和加载状态", async () => {
    const user = userEvent.setup();
    const onClick = vi.fn();
    render(
      <div>
        <Button onClick={onClick}>保存</Button>
        <Button disabled>不可用</Button>
        <Button loading onClick={onClick}>
          提交
        </Button>
      </div>,
    );

    const saveButton = screen.getByRole("button", { name: "保存" });
    await user.tab();
    expect(saveButton).toHaveFocus();
    expect(saveButton.className).toContain("focus-visible:ring-2");
    expect(screen.getByRole("button", { name: "不可用" })).toBeDisabled();
    const loadingButton = screen.getByRole("button", { name: "处理中" });
    expect(loadingButton).toBeDisabled();
    expect(loadingButton).toHaveAttribute("aria-busy", "true");
    await user.click(loadingButton);
    expect(onClick).not.toHaveBeenCalled();
  });

  it("Input 错误说明与控件关联，且保留原生禁用语义", () => {
    render(
      <>
        <Input error="名称最多 20 个字" label="商品名称" />
        <Input disabled label="订单号" />
      </>,
    );

    const invalidInput = screen.getByLabelText("商品名称");
    const error = screen.getByRole("alert");
    expect(invalidInput).toHaveAttribute("aria-invalid", "true");
    expect(invalidInput).toHaveAttribute(
      "aria-describedby",
      expect.stringContaining(error.id),
    );
    expect(screen.getByLabelText("订单号")).toBeDisabled();
  });

  it("Search 暴露加载、清除与错误状态", async () => {
    const user = userEvent.setup();
    const onClear = vi.fn();
    const { rerender } = render(
      <Search label="搜索商品" loading value="拿铁" readOnly />,
    );
    expect(screen.getByRole("searchbox", { name: "搜索商品" })).toHaveAttribute(
      "aria-busy",
      "true",
    );
    expect(
      screen.getByRole("status", { name: "正在搜索" }),
    ).toBeInTheDocument();

    rerender(
      <Search
        error="搜索失败，请重试"
        label="搜索商品"
        onClear={onClear}
        value="拿铁"
        readOnly
      />,
    );
    await user.click(screen.getByRole("button", { name: "清除搜索内容" }));
    expect(onClear).toHaveBeenCalledOnce();
    expect(screen.getByRole("searchbox")).toHaveAttribute(
      "aria-invalid",
      "true",
    );
  });

  it("Select 错误和禁用状态使用 ARIA 与原生 Radix 语义", () => {
    const { rerender } = render(
      <Select
        error="此项为必选"
        label="订单状态"
        options={[{ label: "已支付", value: "paid" }]}
      />,
    );
    const trigger = screen.getByRole("combobox", { name: "订单状态" });
    expect(trigger).toHaveAttribute("aria-invalid", "true");
    expect(trigger).toHaveAttribute("aria-describedby");

    rerender(
      <Select
        disabled
        label="订单状态"
        options={[{ label: "已支付", value: "paid" }]}
      />,
    );
    expect(screen.getByRole("combobox", { name: "订单状态" })).toBeDisabled();
  });

  it("Dialog 使用 Esc 关闭并将焦点归还触发按钮", async () => {
    const user = userEvent.setup();
    render(
      <Dialog
        footer={
          <DialogClose asChild>
            <Button variant="secondary">取消</Button>
          </DialogClose>
        }
        title="编辑商品"
        trigger={<Button>打开编辑</Button>}
      >
        <Input autoFocus label="商品名称" />
      </Dialog>,
    );
    const trigger = screen.getByRole("button", { name: "打开编辑" });
    await user.click(trigger);
    expect(
      screen.getByRole("dialog", { name: "编辑商品" }),
    ).toBeInTheDocument();
    expect(screen.getByLabelText("商品名称")).toHaveFocus();
    await user.keyboard("{Escape}");
    expect(screen.queryByRole("dialog")).not.toBeInTheDocument();
    expect(trigger).toHaveFocus();
  });

  it("Pagination 标记当前页并禁用边界按钮", () => {
    render(
      <Pagination
        onPageChange={vi.fn()}
        page={1}
        pageCount={3}
        summary="第 1–20 条，共 50 条"
      />,
    );
    expect(
      screen.getByRole("navigation", { name: "分页" }),
    ).toBeInTheDocument();
    expect(screen.getByRole("button", { name: "上一页" })).toBeDisabled();
    expect(screen.getByRole("button", { name: "第 1 页" })).toHaveAttribute(
      "aria-current",
      "page",
    );
  });

  it("Toast 提供语义通知、可忽略动作和关闭按钮", () => {
    function Example() {
      const [open, setOpen] = useState(true);
      return (
        <ToastProvider>
          <ToastMessage
            action="重试"
            actionAltText="重试保存"
            description="修改尚未保存"
            onOpenChange={setOpen}
            open={open}
            title="保存失败"
            tone="error"
          />
        </ToastProvider>
      );
    }
    render(<Example />);
    expect(screen.getByText("保存失败")).toBeInTheDocument();
    expect(screen.getByRole("button", { name: "重试" })).toHaveAttribute(
      "data-radix-toast-announce-alt",
      "重试保存",
    );
    expect(
      screen.getByRole("button", { name: "关闭通知" }),
    ).toBeInTheDocument();
  });
});
