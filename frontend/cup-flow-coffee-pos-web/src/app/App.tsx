import { type DataRouter, RouterProvider } from "react-router";

import { AppProviders } from "./providers";
import { appRouter } from "./router";

interface AppProps {
  router?: DataRouter;
}

export function App({ router = appRouter }: AppProps) {
  return (
    <AppProviders>
      <RouterProvider router={router} />
    </AppProviders>
  );
}
