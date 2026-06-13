import { BrowserRouter, Routes, Route, NavLink, Navigate, Outlet } from "react-router-dom";
import { Files, MessageSquare, Search, LogOut, Database, Brain } from "lucide-react";
import DocumentPage from "./pages/DocumentPage";
import AgentPage from "./pages/AgentPage";
import SearchPage from "./pages/SearchPage";
import WelcomePage from "./pages/WelcomePage";
import LoginPage from "./pages/LoginPage";
import RegisterPage from "./pages/RegisterPage";
import { AuthProvider, useAuth } from "./lib/mockAuth";
import { AgentStateProvider, useAgentReset } from "./lib/agentStore";

const nav = [
  { to: "/documents", icon: Files, label: "仓库" },
  { to: "/agent", icon: MessageSquare, label: "Agent" },
  { to: "/search", icon: Search, label: "检索" },
];

function AppShell() {
  const { logout } = useAuth();
  const resetAgent = useAgentReset();
  const handleLogout = () => { resetAgent(); logout(); };
  return (
    <div className="h-screen flex flex-col p-6 overflow-hidden">
      <div className="flex-1 min-h-0 flex flex-col max-w-[1480px] mx-auto w-full gap-4">
        <header className="flex items-center px-5 py-2.5 glass rounded-full">
          <div className="flex items-center gap-2.5 w-[220px] flex-shrink-0">
            <img src="/obsidian-icon.png" alt="QA Agent" className="w-8 h-8" />
            <span className="font-semibold text-sm tracking-wide">QA Agent</span>
          </div>
          <nav className="flex-1 flex items-center justify-center gap-1">
            {nav.map(({ to, icon: Icon, label }) => (
              <NavLink
                key={to}
                to={to}
                className={({ isActive }) =>
                  `flex items-center gap-2 px-4 py-2 rounded-full text-xs font-medium transition-all duration-180 ${
                    isActive
                      ? "bg-[var(--color-pill-dark)] text-[var(--color-pill-text)] shadow-[var(--shadow-btn)]"
                      : "text-[var(--color-ink-soft)] hover:text-[var(--color-ink)]"
                  }`
                }
              >
                <Icon size={15} />{label}
              </NavLink>
            ))}
          </nav>
          <div className="flex items-center gap-3 justify-end w-[240px] flex-shrink-0">
            <span className="text-[11px] text-[var(--color-ink-faint)] flex items-center gap-1.5"><Database size={11} /> pgvector</span>
            <span className="text-[11px] text-[var(--color-ink-faint)] flex items-center gap-1.5"><Brain size={11} /> DeepSeek</span>
            <button onClick={handleLogout} className="ml-2 flex items-center gap-1.5 text-[11px] text-[var(--color-ink-faint)] hover:text-[var(--color-danger)] transition-colors">
              <LogOut size={13} />退出
            </button>
          </div>
        </header>
        <div className="flex-1 min-h-0 flex flex-col glass-lg overflow-hidden"><Outlet /></div>
        <footer className="text-center text-[10px] text-[var(--color-ink-faint)] opacity-40">QA Agent · React + Vite + TypeScript</footer>
      </div>
    </div>
  );
}

function ProtectedRoute() {
  const { authed } = useAuth();
  return authed ? <AppShell /> : <Navigate to="/login" replace />;
}

export default function App() {
  return (
    <AuthProvider>
      <AgentStateProvider>
      <BrowserRouter>
        <Routes>
          <Route path="/" element={<WelcomePage />} />
          <Route path="/login" element={<LoginPage />} />
          <Route path="/register" element={<RegisterPage />} />
          <Route element={<ProtectedRoute />}>
            <Route path="/documents" element={<DocumentPage />} />
            <Route path="/agent" element={<AgentPage />} />
            <Route path="/search" element={<SearchPage />} />
          </Route>
        </Routes>
      </BrowserRouter>
      </AgentStateProvider>
    </AuthProvider>
  );
}