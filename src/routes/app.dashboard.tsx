import { createFileRoute } from "@tanstack/react-router";
import { useAuth } from "@/lib/auth";
import { AdminDashboard } from "@/components/app/dashboards/AdminDashboard";
import { LeaderDashboard } from "@/components/app/dashboards/LeaderDashboard";
import { MemberDashboard } from "@/components/app/dashboards/MemberDashboard";

export const Route = createFileRoute("/app/dashboard")({
  component: DashboardRouter,
});

function DashboardRouter() {
  const { user, activeRole } = useAuth();
  if (!user || !activeRole) return null;
  if (activeRole === "admin" && user.roles.includes("admin")) return <AdminDashboard />;
  if (activeRole === "leader" && user.roles.includes("leader")) return <LeaderDashboard />;
  return <MemberDashboard />;
}
