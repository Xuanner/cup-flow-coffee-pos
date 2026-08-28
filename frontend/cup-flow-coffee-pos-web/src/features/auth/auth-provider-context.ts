import { createContext, useContext } from "react";

export interface AuthProviderValue {
  retrySessionCheck: () => void;
}

export const AuthContext = createContext<AuthProviderValue | null>(null);

export function useAuthProvider(): AuthProviderValue {
  const context = useContext(AuthContext);
  if (!context)
    throw new Error("useAuthProvider must be used within AuthProvider");
  return context;
}
