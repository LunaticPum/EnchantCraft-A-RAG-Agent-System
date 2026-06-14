import { createContext, useContext, useState, type ReactNode } from "react";
import { setAuthToken } from "./api";

export interface UserInfo {
  userId: string;
  username: string;
  role: string;
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

  const login = (u: UserInfo) => { setUser(u); setAuthed(true); };
  const logout = () => { setUser(null); setAuthed(false); setAuthToken(""); };

  return (
    <AuthContext.Provider value={{ authed, user, login, logout }}>
      {children}
    </AuthContext.Provider>
  );
}

export function useAuth() {
  return useContext(AuthContext);
}
