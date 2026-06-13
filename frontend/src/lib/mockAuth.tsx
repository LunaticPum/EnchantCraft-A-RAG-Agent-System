import { createContext, useContext, useState, type ReactNode } from "react";

export interface UserInfo {
  userId: string;
  username: string;
  role: string;      // ADMIN / USER
}

interface AuthCtx {
  authed: boolean;
  user: UserInfo | null;
  login: (u: UserInfo) => void;
  logout: () => void;
}

const AuthContext = createContext<AuthCtx>({
  authed: false,
  user: null,
  login: () => {},
  logout: () => {},
});

export function AuthProvider({ children }: { children: ReactNode }) {
  const [authed, setAuthed] = useState(false);
  const [user, setUser] = useState<UserInfo | null>(null);
  return (
    <AuthContext.Provider value={{
      authed, user,
      login: (u) => { setAuthed(true); setUser(u); },
      logout: () => { setAuthed(false); setUser(null); setAuthToken(""); },
    }}>
      {children}
    </AuthContext.Provider>
  );
}

export function useAuth() {
  return useContext(AuthContext);
}