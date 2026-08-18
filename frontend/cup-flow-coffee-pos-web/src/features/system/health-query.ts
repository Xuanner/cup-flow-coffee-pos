import { useQuery } from "@tanstack/react-query";

import { getHealth } from "./health-api";

export const healthQueryKey = ["system", "health"] as const;

export function useHealthQuery() {
  return useQuery({
    queryFn: ({ signal }) => getHealth(signal),
    queryKey: healthQueryKey,
    refetchOnWindowFocus: false,
    retry: false,
    staleTime: 0,
  });
}
