import {
  createBrowserRouter,
  createMemoryRouter,
  Navigate,
} from "react-router";

import { AppShell } from "../components/layout/AppShell";
import { AuthPage } from "../features/auth/AuthPage";
import {
  ProtectedSessionRoute,
  SessionGate,
} from "../features/auth/SessionBoundary";
import { DashboardPage } from "../features/dashboard/DashboardPage";
import { OrdersPage } from "../features/orders/OrdersPage";
import { PosPage } from "../features/pos/PosPage";
import { ProductsPage } from "../features/products/ProductsPage";
import { ComponentsPage } from "../features/system/ComponentsPage";
import { NotFoundPage } from "../features/system/NotFoundPage";
import { SystemPage } from "../features/system/SystemPage";

export const appRoutes = [
  {
    Component: SessionGate,
    children: [
      { path: "/login", Component: AuthPage },
      {
        Component: ProtectedSessionRoute,
        children: [
          {
            path: "/",
            Component: AppShell,
            children: [
              { index: true, element: <Navigate to="/pos" replace /> },
              { path: "pos", Component: PosPage },
              { path: "orders", Component: OrdersPage },
              { path: "products", Component: ProductsPage },
              { path: "dashboard", Component: DashboardPage },
              { path: "system", Component: SystemPage },
              { path: "system/components", Component: ComponentsPage },
              { path: "*", Component: NotFoundPage },
            ],
          },
        ],
      },
    ],
  },
];

export const appRouter = createBrowserRouter(appRoutes);

export function createTestRouter(initialEntries: string[]) {
  return createMemoryRouter(appRoutes, { initialEntries });
}
