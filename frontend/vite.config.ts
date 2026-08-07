import { defineConfig } from "vitest/config";
import react from "@vitejs/plugin-react";
import path from "node:path";

export default defineConfig({
  plugins: [react()],
  test: {
    globals: true,
    environment: "jsdom",
    include: ["src/**/*.{test,spec}.{ts,tsx}"],
    exclude: ["e2e/**", "node_modules/**"],
    setupFiles: ["./src/test/setup.ts"],
    css: false,
  },
  resolve: {
    alias: {
      "@": path.resolve(__dirname, "./src"),
    },
  },
  server: {
    host: "0.0.0.0",
    port: 5173,
    strictPort: true,
    proxy: {
      "/api": {
        target: "http://127.0.0.1:8081",
        changeOrigin: true,
        // Browsers on the same Wi-Fi use this dev server's LAN address. Normalize
        // the proxied Origin so Spring's local-only CORS allowlist accepts it.
        headers: { Origin: "http://localhost:5173" },
      },
    },
  },
  preview: {
    host: "0.0.0.0",
    port: 5173,
    strictPort: true,
  },
  build: {
    // xlsx-js-style is isolated in a lazy export-only chunk. Its upstream payload is
    // intentionally larger than Vite's generic threshold and is not on the startup path.
    chunkSizeWarningLimit: 900,
    rollupOptions: {
      output: {
        manualChunks: {
          react: ["react", "react-dom", "react-router-dom"],
          charts: ["recharts"],
          spreadsheets: ["xlsx-js-style"],
          pdf: ["jspdf", "jspdf-autotable"],
        },
      },
    },
  },
});
