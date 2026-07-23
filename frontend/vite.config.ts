// @lovable.dev/vite-tanstack-config already includes the following — do NOT add them manually
// or the app will break with duplicate plugins:
//   - TanStack devtools (dev-only, first), tanstackStart, viteReact, tailwindcss,
//     nitro (build-only using cloudflare as a default target), VITE_* env injection, @ path alias,
//     React/TanStack dedupe, error logger plugins, and sandbox detection (port/host/strictPort).
// You can pass additional config via defineConfig({ vite: { ... }, etc... }) if needed.
import { defineConfig as defineLovableConfig } from "@lovable.dev/vite-tanstack-config";
import type { ConfigEnv, PluginOption } from "vite";

const legacyTsconfigPathPluginNames = new Set([
  "vite-tsconfig-paths",
  "vite-plugin-tsconfig-paths",
]);

function isLegacyTsconfigPathsPlugin(plugin: PluginOption): boolean {
  return Boolean(
    plugin &&
    typeof plugin === "object" &&
    !Array.isArray(plugin) &&
    "name" in plugin &&
    typeof plugin.name === "string" &&
    legacyTsconfigPathPluginNames.has(plugin.name),
  );
}

const createViteConfig = defineLovableConfig({
  tanstackStart: {
    // Redirect TanStack Start's bundled server entry to src/server.ts (our SSR error wrapper).
    // nitro/vite builds from this
    server: { entry: "server" },
  },
  vite: {
    resolve: {
      tsconfigPaths: true,
    },
    server: {
      port: 5173,
      strictPort: false,
      proxy: {
        "/api": {
          target: "http://localhost:8080",
          changeOrigin: true,
        },
        "/actuator": {
          target: "http://localhost:8080",
          changeOrigin: true,
        },
      },
    },
  },
});

export default async (environment: ConfigEnv) => {
  const config = await createViteConfig(environment);

  return {
    ...config,
    plugins: config.plugins?.filter((plugin) => !isLegacyTsconfigPathsPlugin(plugin)),
  };
};
