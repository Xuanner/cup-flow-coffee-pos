import { create } from "zustand";

interface AppUiState {
  isNavigationOpen: boolean;
  closeNavigation: () => void;
  toggleNavigation: () => void;
}

export const useAppUiStore = create<AppUiState>((set) => ({
  isNavigationOpen: false,
  closeNavigation: () => set({ isNavigationOpen: false }),
  toggleNavigation: () =>
    set((state) => ({ isNavigationOpen: !state.isNavigationOpen })),
}));
