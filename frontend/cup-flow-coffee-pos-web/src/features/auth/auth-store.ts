import { create } from "zustand";

import type { CurrentUser } from "./auth-model";

interface AuthState {
  currentUser: CurrentUser | null;
  sessionStatus: "checking" | "authenticated" | "anonymous" | "error";
  sessionError: string | null;
  setCurrentUser: (currentUser: CurrentUser) => void;
  clearCurrentUser: () => void;
  beginSessionCheck: () => void;
  failSessionCheck: (message: string) => void;
}

export const useAuthStore = create<AuthState>((set) => ({
  currentUser: null,
  sessionStatus: "checking",
  sessionError: null,
  setCurrentUser: (currentUser) =>
    set({ currentUser, sessionError: null, sessionStatus: "authenticated" }),
  clearCurrentUser: () =>
    set({ currentUser: null, sessionError: null, sessionStatus: "anonymous" }),
  beginSessionCheck: () =>
    set({ currentUser: null, sessionError: null, sessionStatus: "checking" }),
  failSessionCheck: (sessionError) =>
    set({ currentUser: null, sessionError, sessionStatus: "error" }),
}));
