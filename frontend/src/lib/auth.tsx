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
  clearStoredSession,
  persistSession,
  readStoredSession,
  refreshCurrentUser,
  signInWithBackend,
  type BackendSession,
} from "@/lib/backend-api";
import { hydrateCurrentUser, roleLabel, type Role, type UserAccount } from "@/lib/users-data";

export { roleLabel, type Role, type UserAccount };

type AuthContextValue = {
  user: UserAccount | null;
  activeRole: Role | null;
  ready: boolean;
  signIn: (email: string, password: string) => Promise<void>;
  signOut: () => void;
  setActiveRole: (role: Role) => void;
};

const AuthContext = createContext<AuthContextValue | null>(null);

export function AuthProvider({ children }: { children: ReactNode }) {
  const [session, setSession] = useState<BackendSession | null>(null);
  const [ready, setReady] = useState(false);

  useEffect(() => {
    const stored = readStoredSession();
    if (!stored) {
      setReady(true);
      return;
    }

    setSession(stored);
    hydrateCurrentUser(stored);
    refreshCurrentUser(stored.authorization)
      .then((user) => {
        const activeRole = user.roles.includes(stored.activeRole)
          ? stored.activeRole
          : user.roles[0];
        const next = { authorization: stored.authorization, user, activeRole };
        persistSession(next);
        hydrateCurrentUser(next);
        setSession(next);
      })
      .catch(() => {
        clearStoredSession();
        setSession(null);
      })
      .finally(() => setReady(true));
  }, []);

  const signIn = useCallback(async (email: string, password: string) => {
    const next = await signInWithBackend(email, password);
    persistSession(next);
    hydrateCurrentUser(next);
    setSession(next);
  }, []);

  const signOut = useCallback(() => {
    clearStoredSession();
    setSession(null);
  }, []);

  const setActiveRole = useCallback(
    (role: Role) => {
      if (!session || !session.user.roles.includes(role)) return;
      const next = { ...session, activeRole: role };
      persistSession(next);
      setSession(next);
    },
    [session],
  );

  const value = useMemo<AuthContextValue>(() => {
    if (!session || session.user.status !== "active") {
      return {
        user: null,
        activeRole: null,
        ready,
        signIn,
        signOut,
        setActiveRole,
      };
    }
    const user = session.user;
    const activeRole = user.roles.includes(session.activeRole) ? session.activeRole : user.roles[0];
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
