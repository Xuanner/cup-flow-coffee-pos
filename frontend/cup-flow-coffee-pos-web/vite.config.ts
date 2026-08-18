import tailwindcss from "@tailwindcss/vite";
import react from "@vitejs/plugin-react";
import { defineConfig } from "vite";

export default defineConfig({
  plugins: [react(), tailwindcss()],
  server: {
    port: 5173,
    proxy: {
      "/api/v1": {
        changeOrigin: true,
        target: "http://localhost:8080",
      },
    },
    strictPort: true,
  },
  preview: {
    port: 4173,
    strictPort: true,
  },
});
