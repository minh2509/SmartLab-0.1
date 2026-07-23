import { useMemo, useState } from "react";
import { UserPlus, X } from "lucide-react";
import {
  addAdminProjectMember,
  adminProjectErrorMessage,
  removeAdminProjectMember,
  updateAdminProjectMemberRole,
  useAdminProjectMembers,
  type AdminProjectMember,
} from "@/lib/admin-projects-api";
import { useUsers } from "@/lib/users-data";

export function AdminProjectMembersPanel({
  token,
  projectId,
  onChanged,
}: {
  token: string;
  projectId: string;
  onChanged: () => void;
}) {
  const members = useAdminProjectMembers(token, projectId, true);
  const users = useUsers(token);
  const [selectedUserId, setSelectedUserId] = useState("");
  const [selectedRole, setSelectedRole] = useState<AdminProjectMember["projectRole"]>(
    "PROJECT_MEMBER",
  );
  const [pendingUserId, setPendingUserId] = useState<string | null>(null);
  const [error, setError] = useState<string | null>(null);
  const activeIds = useMemo(() => new Set(members.data.map((member) => member.userId)), [members.data]);
  const candidates = users.users.filter(
    (candidate) => candidate.status === "active" && !activeIds.has(candidate.id),
  );

  const refresh = () => {
    members.retry();
    onChanged();
  };
  const run = async (userId: string, action: () => Promise<unknown>) => {
    setPendingUserId(userId);
    setError(null);
    try {
      await action();
      refresh();
    } catch (cause) {
      setError(adminProjectErrorMessage(cause, "Project membership could not be updated."));
    } finally {
      setPendingUserId(null);
    }
  };

  if (members.loading) return <div className="text-xs text-ink-soft">Loading members...</div>;
  if (members.error) {
    return (
      <div className="rounded-lg border border-hairline p-3 text-xs text-ink-soft">
        <p>{members.error}</p>
        <button type="button" onClick={members.retry} className="mt-2 font-medium text-ink">
          Retry
        </button>
      </div>
    );
  }

  return (
    <div className="space-y-3">
      <div className="grid gap-2 rounded-lg border border-hairline p-3 sm:grid-cols-[1fr_auto_auto]">
        <select
          value={selectedUserId}
          onChange={(event) => setSelectedUserId(event.target.value)}
          disabled={users.loading || Boolean(users.loadError) || pendingUserId !== null}
          className="rounded-md border border-hairline bg-surface-elev px-2.5 py-2 text-xs text-ink"
          aria-label="User to add"
        >
          <option value="">{users.loading ? "Loading users..." : "Select an active user"}</option>
          {candidates.map((candidate) => (
            <option key={candidate.id} value={candidate.id}>
              {candidate.fullName} · {candidate.email}
            </option>
          ))}
        </select>
        <select
          value={selectedRole}
          onChange={(event) =>
            setSelectedRole(event.target.value as AdminProjectMember["projectRole"])
          }
          disabled={pendingUserId !== null}
          className="rounded-md border border-hairline bg-surface-elev px-2.5 py-2 text-xs text-ink"
          aria-label="Project role"
        >
          <option value="PROJECT_MEMBER">Member</option>
          <option value="PROJECT_LEADER">Leader</option>
        </select>
        <button
          type="button"
          disabled={!selectedUserId || pendingUserId !== null}
          onClick={() =>
            void run(selectedUserId, async () => {
              await addAdminProjectMember(token, projectId, selectedUserId, selectedRole);
              setSelectedUserId("");
            })
          }
          className="inline-flex items-center justify-center gap-1.5 rounded-md bg-primary px-3 py-2 text-xs font-medium text-primary-foreground disabled:cursor-not-allowed disabled:opacity-50"
        >
          <UserPlus className="h-3.5 w-3.5" /> Add
        </button>
      </div>

      {users.loadError ? <p className="text-xs text-[color:var(--destructive)]">{users.loadError}</p> : null}
      {error ? <p className="text-xs text-[color:var(--destructive)]">{error}</p> : null}

      <div className="grid gap-3 sm:grid-cols-2">
        {members.data.map((member) => (
          <div key={member.membershipId} className="flex items-center gap-3 rounded-lg border border-hairline p-3">
            <div className="grid h-9 w-9 shrink-0 place-items-center rounded-full bg-primary text-xs font-semibold text-primary-foreground">
              {member.fullName
                .split(/\s+/)
                .slice(0, 2)
                .map((part) => part[0])
                .join("")
                .toUpperCase()}
            </div>
            <div className="min-w-0 flex-1">
              <div className="truncate text-sm font-medium text-ink">{member.fullName}</div>
              <div className="truncate text-[11px] text-ink-soft">{member.email}</div>
            </div>
            <select
              value={member.projectRole}
              disabled={pendingUserId !== null}
              onChange={(event) =>
                void run(member.userId, () =>
                  updateAdminProjectMemberRole(
                    token,
                    projectId,
                    member.userId,
                    event.target.value as AdminProjectMember["projectRole"],
                  ),
                )
              }
              className="rounded-md border border-hairline bg-surface-elev px-2 py-1 text-[11px] text-ink"
              aria-label={`Role for ${member.fullName}`}
            >
              <option value="PROJECT_MEMBER">Member</option>
              <option value="PROJECT_LEADER">Leader</option>
            </select>
            <button
              type="button"
              disabled={pendingUserId !== null}
              onClick={() =>
                void run(member.userId, () =>
                  removeAdminProjectMember(token, projectId, member.userId),
                )
              }
              className="rounded-md p-1.5 text-ink-soft hover:bg-muted hover:text-[color:var(--destructive)] disabled:opacity-40"
              aria-label={`Remove ${member.fullName}`}
              title="Remove member"
            >
              <X className="h-3.5 w-3.5" />
            </button>
          </div>
        ))}
        {members.data.length === 0 ? (
          <div className="text-xs text-ink-soft">No active project members.</div>
        ) : null}
      </div>
    </div>
  );
}
