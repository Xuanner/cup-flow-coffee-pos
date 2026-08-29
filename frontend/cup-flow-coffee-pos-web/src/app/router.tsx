import {
  createBrowserRouter,
  createMemoryRouter,
  Navigate,
} from "react-router";

import { AppShell } from "../components/layout/AppShell";
import { AuthPage } from "../features/auth/AuthPage";
import { RoleBoundary } from "../features/auth/RoleBoundary";
import {
  ProtectedSessionRoute,
  SessionGate,
} from "../features/auth/SessionBoundary";
import { DashboardPage } from "../features/dashboard/DashboardPage";
import { OrdersPage } from "../features/orders/OrdersPage";
import { PosPage } from "../features/pos/PosPage";
import { ProductsPage } from "../features/products/ProductsPage";
import { ComponentsPage } from "../features/system/ComponentsPage";
import { ForbiddenPage } from "../features/system/ForbiddenPage";
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
              {
                element: <RoleBoundary requiredRole="CASHIER" />,
                children: [
                  { path: "pos", Component: PosPage },
                  { path: "orders", Component: OrdersPage },
                ],
              },
              {
                element: <RoleBoundary requiredRole="ADMIN" />,
                children: [
                  { path: "products", Component: ProductsPage },
                  { path: "dashboard", Component: DashboardPage },
                ],
              },
              { path: "forbidden", Component: ForbiddenPage },
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
