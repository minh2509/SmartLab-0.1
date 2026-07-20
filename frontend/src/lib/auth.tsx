import {
  createContext,
  useCallback,
  useContext,
  useEffect,
  useMemo,
  useState,
  type ReactNode,
} from "react";
import {
  DEMO_PASSWORD,
  getUserByEmail,
  getUserById,
  recordLogin,
  roleLabel,
  subscribeToUsers,
  type Role,
  type UserAccount,
} from "@/lib/users-data";

export { DEMO_PASSWORD, roleLabel, type Role, type UserAccount };

type Session = { userId: string; activeRole: Role };

type AuthContextValue = {
  user: UserAccount | null;
  activeRole: Role | null;
  ready: boolean;
  signIn: (email: string, password: string) => Promise<void>;
  signOut: () => void;
  setActiveRole: (role: Role) => void;
};

const AuthContext = createContext<AuthContextValue | null>(null);
const STORAGE_KEY = "smart.session.v1";

function isRecord(value: unknown): value is Record<string, unknown> {
  return typeof value === "object" && value !== null;
}

function readString(value: unknown) {
  return typeof value === "string" ? value : "";
}

function parseSession(value: unknown): Session | null {
  if (!isRecord(value)) return null;
  const userId = readString(value.userId) || readString(value.id);
  const activeRole = readString(value.activeRole) as Role;
  if (!userId) return null;
  const account = getUserById(userId);
  if (!account || account.status === "locked") return null;
  return {
    userId: account.id,
    activeRole: account.roles.includes(activeRole) ? activeRole : account.roles[0],
  };
}

export function AuthProvider({ children }: { children: ReactNode }) {
  const [session, setSession] = useState<Session | null>(null);
  const [ready, setReady] = useState(false);
  const [, forceUserRefresh] = useState(0);

  useEffect(() => {
    try {
      const raw = localStorage.getItem(STORAGE_KEY);
      const parsed = raw ? parseSession(JSON.parse(raw)) : null;
      if (parsed) setSession(parsed);
      else if (raw) localStorage.removeItem(STORAGE_KEY);
    } catch {
      localStorage.removeItem(STORAGE_KEY);
    }
    setReady(true);
  }, []);

  useEffect(
    () =>
      subscribeToUsers(() => {
        forceUserRefresh((value) => value + 1);
        setSession((current) => {
          if (!current) return current;
          const account = getUserById(current.userId);
          if (!account || account.status === "locked") {
            try {
              localStorage.removeItem(STORAGE_KEY);
            } catch {
              // ignore
            }
            return null;
          }
          if (!account.roles.includes(current.activeRole)) {
            const next = { ...current, activeRole: account.roles[0] };
            try {
              localStorage.setItem(STORAGE_KEY, JSON.stringify(next));
            } catch {
              // ignore
            }
            return next;
          }
          return current;
        });
      }),
    [],
  );

  const persist = (s: Session | null) => {
    setSession(s);
    try {
      if (s) localStorage.setItem(STORAGE_KEY, JSON.stringify(s));
      else localStorage.removeItem(STORAGE_KEY);
    } catch {
      // ignore
    }
  };

  const signIn = useCallback(async (email: string, password: string) => {
    await new Promise((resolve) => setTimeout(resolve, 450));
    const account = getUserByEmail(email);
    if (!account) throw new Error("Invalid email or password.");
    if (password !== DEMO_PASSWORD) throw new Error("Invalid email or password.");
    if (account.status === "locked") {
      throw new Error("This account is locked. Contact a lab administrator.");
    }
    recordLogin(account.id);
    persist({ userId: account.id, activeRole: account.roles[0] });
  }, []);

  const signOut = useCallback(() => persist(null), []);

  const setActiveRole = useCallback(
    (role: Role) => {
      if (!session) return;
      const account = getUserById(session.userId);
      if (!account || account.status === "locked" || !account.roles.includes(role)) return;
      persist({ ...session, activeRole: role });
    },
    [session],
  );

  const value = useMemo<AuthContextValue>(() => {
    const account = session ? (getUserById(session.userId) ?? null) : null;
    const user = account && account.status === "active" ? account : null;
    const activeRole =
      user && session && user.roles.includes(session.activeRole)
        ? session.activeRole
        : (user?.roles[0] ?? null);
    return {
      user,
      activeRole,
      ready,
      signIn,
      signOut,
      setActiveRole,
    };
  }, [session, ready, signIn, signOut, setActiveRole]);

  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>;
}

export function useAuth() {
  const ctx = useContext(AuthContext);
  if (!ctx) throw new Error("useAuth must be used inside AuthProvider");
  return ctx;
}
