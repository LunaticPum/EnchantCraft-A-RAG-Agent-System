import { createContext, useContext, useState, type ReactNode } from "react";

interface AuthCtx {
  authed: boolean;
  login: () => void;
  logout: () => void;
}

const AuthContext = createContext<AuthCtx>({
  authed: false,
  login: () => {},
  logout: () => {},
});

export function AuthProvider({ children }: { children: ReactNode }) {
  const [authed, setAuthed] = useState(false);
  return (
    <AuthContext.Provider value={{ authed, login: () => setAuthed(true), logout: () => setAuthed(false) }}>
      {children}
    </AuthContext.Provider>
  );
}

export function useAuth() {
  return useContext(AuthContext);
}