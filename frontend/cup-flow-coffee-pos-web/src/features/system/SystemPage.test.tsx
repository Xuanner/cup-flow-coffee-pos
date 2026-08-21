import { render, screen } from "@testing-library/react";
import userEvent from "@testing-library/user-event";

import {
  apiFailureResponse,
  apiSuccessResponse,
} from "../../test/api-response-fixture";
import { createQueryWrapper } from "../../test/query-wrapper";
import { SystemPage } from "./SystemPage";

function renderSystemPage() {
  const Wrapper = createQueryWrapper();
  return render(
    <Wrapper>
      <SystemPage />
    </Wrapper>,
  );
}

describe("SystemPage 健康检查", () => {
  it("TC-S1-HEALTH-201 请求期间显示加载状态", () => {
    vi.spyOn(globalThis, "fetch").mockImplementationOnce(
      () => new Promise(() => undefined),
    );

    renderSystemPage();

    expect(screen.getByRole("status")).toHaveTextContent("正在检查后端服务");
  });

  it("TC-S1-HEALTH-202 显示健康检查成功状态", async () => {
    vi.spyOn(globalThis, "fetch").mockResolvedValueOnce(
      apiSuccessResponse({ application: "UP", database: "UP" }),
    );

    renderSystemPage();

    expect(await screen.findByText("后端服务运行正常")).toBeInTheDocument();
    expect(
      screen.getByText("应用服务与数据库连接均正常。"),
    ).toBeInTheDocument();
    expect(screen.getAllByText("UP")).toHaveLength(2);
  });

  it("TC-S1-HEALTH-203 显示失败状态并允许重新检查", async () => {
    const user = userEvent.setup();
    vi.spyOn(globalThis, "fetch")
      .mockResolvedValueOnce(
        apiFailureResponse(503, "COMMON-500-001", "内部详情"),
      )
      .mockResolvedValueOnce(
        apiSuccessResponse({ application: "UP", database: "UP" }),
      );

    renderSystemPage();

    expect(await screen.findByRole("alert")).toHaveTextContent("服务异常");
    expect(screen.getByRole("alert")).not.toHaveTextContent("内部详情");

    await user.click(screen.getByRole("button", { name: "重新检查" }));

    expect(await screen.findByRole("status")).toHaveTextContent(
      "后端服务运行正常",
    );
    expect(fetch).toHaveBeenCalledTimes(2);
  });
});
