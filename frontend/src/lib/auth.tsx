import {
  createContext,
  useCallback,
  useContext,
  useEffect,
  useMemo,
  useRef,
  useState,
  type ReactNode,
} from "react";
import { apiRequest } from "@/lib/api-client";
import { roleLabel, type Role, type UserAccount } from "@/lib/users-data";

export { roleLabel, type Role, type UserAccount };

type AuthContextValue = {
  user: UserAccount | null;
  activeRole: Role | null;
  accessToken: string | null;
  ready: boolean;
  signIn: (email: string, password: string) => Promise<void>;
  signOut: () => void;
  setActiveRole: (role: Role) => void;
};

type StoredSession = {
  accessToken: string;
  expiresAt: number;
  activeRole: Role;
};

type BackendRole = "SUPER_ADMIN" | "ADMIN" | "LEADER" | "MEMBER";

type BackendUser = {
  id: string;
  labId: string;
  email: string;
  fullName: string;
  accountStatus: string;
  roles: BackendRole[];
};

type LoginResponse = {
  accessToken: string;
  tokenType: "Bearer";
  expiresIn: number;
  user: BackendUser;
};

const AuthContext = createContext<AuthContextValue | null>(null);
const STORAGE_KEY = "smart.auth.v2";
const LEGACY_STORAGE_KEY = "smart.session.v1";
const FALLBACK_TIMESTAMP = "2026-01-01T00:00:00.000Z";
const FRONTEND_ROLE_ORDER: readonly Role[] = ["admin", "leader", "member"];

function isRecord(value: unknown): value is Record<string, unknown> {
  return typeof value === "object" && value !== null;
}

function readString(value: unknown) {
  return typeof value === "string" ? value : "";
}

function isRole(value: unknown): value is Role {
  return value === "admin" || value === "leader" || value === "member";
}

function isBackendRole(value: unknown): value is BackendRole {
  return value === "SUPER_ADMIN" || value === "ADMIN" || value === "LEADER" || value === "MEMBER";
}

function parseStoredSession(value: unknown): StoredSession | null {
  if (!isRecord(value)) return null;
  const accessToken = readString(value.accessToken);
  const expiresAt = typeof value.expiresAt === "number" ? value.expiresAt : Number.NaN;
  const activeRole = value.activeRole;
  if (!accessToken || !Number.isFinite(expiresAt) || !isRole(activeRole)) return null;
  return { accessToken, expiresAt, activeRole };
}

function readStoredSession(): StoredSession | null {
  if (typeof window === "undefined") return null;
  try {
    localStorage.removeItem(LEGACY_STORAGE_KEY);
    const raw = localStorage.getItem(STORAGE_KEY);
    if (!raw) return null;
    const parsed = parseStoredSession(JSON.parse(raw));
    if (!parsed || parsed.expiresAt <= Date.now()) {
      localStorage.removeItem(STORAGE_KEY);
      return null;
    }
    return parsed;
  } catch {
    localStorage.removeItem(STORAGE_KEY);
    return null;
  }
}

function writeStoredSession(session: StoredSession | null) {
  if (typeof window === "undefined") return;
  try {
    localStorage.removeItem(LEGACY_STORAGE_KEY);
    if (session) {
      localStorage.setItem(STORAGE_KEY, JSON.stringify(session));
    } else {
      localStorage.removeItem(STORAGE_KEY);
    }
  } catch {
    // Storage failures should not expose tokens or break the in-memory session.
  }
}

function makeInitials(name: string) {
  const parts = name.trim().split(/\s+/).filter(Boolean);
  if (parts.length === 0) return "?";
  return parts
    .slice(0, 2)
    .map((part) => part[0]?.toUpperCase() ?? "")
    .join("");
}

function mapBackendRole(role: BackendRole): Role {
  if (role === "SUPER_ADMIN" || role === "ADMIN") return "admin";
  if (role === "LEADER") return "leader";
  return "member";
}

function mapRoles(roles: BackendRole[]): Role[] {
  const mapped = new Set(roles.map(mapBackendRole));
  return FRONTEND_ROLE_ORDER.filter((role) => mapped.has(role));
}

function titleForRole(role: Role) {
  if (role === "admin") return "Lab Administrator";
  if (role === "leader") return "Project Leader";
  return "Lab Member";
}

function normalizeBackendUser(value: unknown): BackendUser | null {
  if (!isRecord(value)) return null;
  const roles = Array.isArray(value.roles)
    ? value.roles.filter((role): role is BackendRole => isBackendRole(role))
    : [];
  const id = readString(value.id);
  const labId = readString(value.labId);
  const email = readString(value.email).trim().toLowerCase();
  const fullName = readString(value.fullName);
  const accountStatus = readString(value.accountStatus);
  if (!id || !labId || !email || !fullName || !accountStatus || roles.length === 0) return null;
  return { id, labId, email, fullName, accountStatus, roles };
}

function normalizeLoginResponse(value: unknown): LoginResponse | null {
  if (!isRecord(value)) return null;
  const accessToken = readString(value.accessToken);
  const tokenType = readString(value.tokenType);
  const expiresIn = typeof value.expiresIn === "number" ? value.expiresIn : Number.NaN;
  const user = normalizeBackendUser(value.user);
  if (
    !accessToken ||
    tokenType !== "Bearer" ||
    !Number.isFinite(expiresIn) ||
    expiresIn <= 0 ||
    !user
  ) {
    return null;
  }
  return { accessToken, tokenType, expiresIn, user };
}

function adaptUser(user: BackendUser): UserAccount {
  const roles = mapRoles(user.roles);
  if (roles.length === 0) {
    throw new Error("Your account does not have a supported SmartLab workspace role.");
  }
  return {
    id: user.id,
    labId: user.labId,
    email: user.email,
    fullName: user.fullName,
    initials: makeInitials(user.fullName),
    title: titleForRole(roles[0]),
    roles,
    status: user.accountStatus === "ACTIVE" ? "active" : "locked",
    isMainAdmin: user.roles.includes("SUPER_ADMIN"),
    createdAt: FALLBACK_TIMESTAMP,
    updatedAt: FALLBACK_TIMESTAMP,
  };
}

function selectActiveRole(user: UserAccount, preferredRole: Role | null): Role {
  return preferredRole && user.roles.includes(preferredRole) ? preferredRole : user.roles[0];
}

function clearAuthState(
  setSession: React.Dispatch<React.SetStateAction<StoredSession | null>>,
  setUser: React.Dispatch<React.SetStateAction<UserAccount | null>>,
) {
  writeStoredSession(null);
  setSession(null);
  setUser(null);
}

export function AuthProvider({ children }: { children: ReactNode }) {
  const [session, setSession] = useState<StoredSession | null>(null);
  const [user, setUser] = useState<UserAccount | null>(null);
  const [ready, setReady] = useState(false);
  const mountedRef = useRef(false);

  const persist = useCallback((nextSession: StoredSession | null, nextUser: UserAccount | null) => {
    if (!mountedRef.current) return;
    writeStoredSession(nextSession);
    setSession(nextSession);
    setUser(nextUser);
  }, []);

  useEffect(() => {
    mountedRef.current = true;
    return () => {
      mountedRef.current = false;
    };
  }, []);

  useEffect(() => {
    let mounted = true;

    async function restore() {
      const stored = readStoredSession();
      if (!stored) {
        if (mounted) setReady(true);
        return;
      }

      try {
        const response = await apiRequest("/api/auth/me", { token: stored.accessToken });
        const backendUser = normalizeBackendUser(response);
        if (!backendUser) {
          throw new Error("The saved session could not be restored.");
        }
        const account = adaptUser(backendUser);
        if (account.status !== "active") {
          throw new Error("This account is not active.");
        }
        const activeRole = selectActiveRole(account, stored.activeRole);
        const nextSession = { ...stored, activeRole };
        if (!mounted) return;
        persist(nextSession, account);
      } catch {
        if (!mounted) return;
        clearAuthState(setSession, setUser);
      } finally {
        if (mounted) setReady(true);
      }
    }

    void restore();

    return () => {
      mounted = false;
    };
  }, [persist]);

  useEffect(() => {
    if (!session) return undefined;
    const remaining = session.expiresAt - Date.now();
    if (remaining <= 0) {
      clearAuthState(setSession, setUser);
      return undefined;
    }
    const timeout = window.setTimeout(() => {
      clearAuthState(setSession, setUser);
    }, remaining);
    return () => window.clearTimeout(timeout);
  }, [session]);

  const signIn = useCallback(
    async (email: string, password: string) => {
      const response = await apiRequest("/api/auth/login", {
        method: "POST",
        body: { email: email.trim(), password },
      });
      const login = normalizeLoginResponse(response);
      if (!login) {
        throw new Error("The backend returned an unexpected authentication response.");
      }
      const account = adaptUser(login.user);
      if (account.status !== "active") {
        throw new Error("This account is not active.");
      }
      const activeRole = selectActiveRole(account, null);
      persist(
        {
          accessToken: login.accessToken,
          expiresAt: Date.now() + login.expiresIn * 1000,
          activeRole,
        },
        account,
      );
    },
    [persist],
  );

  const signOut = useCallback(() => persist(null, null), [persist]);

  const setActiveRole = useCallback(
    (role: Role) => {
      if (!session || !user || !user.roles.includes(role)) return;
      persist({ ...session, activeRole: role }, user);
    },
    [persist, session, user],
  );

  const value = useMemo<AuthContextValue>(() => {
    const activeRole = user && session ? selectActiveRole(user, session.activeRole) : null;
    return {
      user,
      activeRole,
      accessToken: session?.accessToken ?? null,
      ready,
      signIn,
      signOut,
      setActiveRole,
    };
  }, [session, ready, signIn, signOut, setActiveRole, user]);

  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>;
}

export function useAuth() {
  const ctx = useContext(AuthContext);
  if (!ctx) throw new Error("useAuth must be used inside AuthProvider");
  return ctx;
}
