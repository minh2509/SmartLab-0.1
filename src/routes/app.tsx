import { createFileRoute } from "@tanstack/react-router";
import { AppShell } from "@/components/app/AppShell";

export const Route = createFileRoute("/app")({
  head: () => ({
    meta: [{ title: "Workspace — Nova Research Lab" }, { name: "robots", content: "noindex" }],
  }),
  component: AppShell,
});
