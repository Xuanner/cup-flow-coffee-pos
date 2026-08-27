import { create } from "zustand";

import type { CurrentUser } from "./auth-model";

interface AuthState {
  currentUser: CurrentUser | null;
  setCurrentUser: (currentUser: CurrentUser) => void;
  clearCurrentUser: () => void;
}

export const useAuthStore = create<AuthState>((set) => ({
  currentUser: null,
  setCurrentUser: (currentUser) => set({ currentUser }),
  clearCurrentUser: () => set({ currentUser: null }),
}));
