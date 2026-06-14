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
  const [authed, setAuthed] = useState(() => {
    return !!localStorage.getItem("auth_user");
  });
  const [user, setUser] = useState<UserInfo | null>(() => {
    const saved = localStorage.getItem("auth_user");
    return saved ? JSON.parse(saved) : null;
  });

  const login = (u: UserInfo) => { setUser(u); setAuthed(true); localStorage.setItem("auth_user", JSON.stringify(u)); };
  const logout = () => { setUser(null); setAuthed(false); setAuthToken(""); localStorage.removeItem("auth_user"); localStorage.removeItem("auth_token"); };

  return (
    <AuthContext.Provider value={{ authed, user, login, logout }}>
      {children}
    </AuthContext.Provider>
  );
}

export function useAuth() {
  return useContext(AuthContext);
}
