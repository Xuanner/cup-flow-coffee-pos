import { create } from "zustand";

import type { CurrentUser } from "./auth-model";

interface AuthState {
  currentUser: CurrentUser | null;
  sessionStatus: "checking" | "authenticated" | "anonymous" | "error";
  sessionError: string | null;
  sessionExpired: boolean;
  setCurrentUser: (currentUser: CurrentUser) => void;
  clearCurrentUser: () => void;
  beginSessionCheck: () => void;
  failSessionCheck: (message: string) => void;
  expireSession: () => void;
}

export const useAuthStore = create<AuthState>((set) => ({
  currentUser: null,
  sessionStatus: "checking",
  sessionError: null,
  sessionExpired: false,
  setCurrentUser: (currentUser) =>
    set({
      currentUser,
      sessionError: null,
      sessionExpired: false,
      sessionStatus: "authenticated",
    }),
  clearCurrentUser: () =>
    set({
      currentUser: null,
      sessionError: null,
      sessionExpired: false,
      sessionStatus: "anonymous",
    }),
  beginSessionCheck: () =>
    set({
      currentUser: null,
      sessionError: null,
      sessionExpired: false,
      sessionStatus: "checking",
    }),
  failSessionCheck: (sessionError) =>
    set({
      currentUser: null,
      sessionError,
      sessionExpired: false,
      sessionStatus: "error",
    }),
  expireSession: () =>
    set((state) => {
      if (state.sessionStatus !== "authenticated") return state;
      return {
        currentUser: null,
        sessionError: null,
        sessionExpired: true,
        sessionStatus: "anonymous",
      };
    }),
}));
