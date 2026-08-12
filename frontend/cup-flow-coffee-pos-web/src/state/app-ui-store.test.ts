import { useAppUiStore } from "./app-ui-store";

describe("应用 UI store", () => {
  beforeEach(() => useAppUiStore.setState({ isNavigationOpen: false }));

  it("可以切换和关闭移动端导航", () => {
    useAppUiStore.getState().toggleNavigation();
    expect(useAppUiStore.getState().isNavigationOpen).toBe(true);

    useAppUiStore.getState().closeNavigation();
    expect(useAppUiStore.getState().isNavigationOpen).toBe(false);
  });
});
