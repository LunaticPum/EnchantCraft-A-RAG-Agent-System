import { BrowserRouter, Routes, Route, NavLink, Navigate, Outlet } from "react-router-dom";
import DocumentPage from "./pages/DocumentPage";
import AgentPage from "./pages/AgentPage";
import SearchPage from "./pages/SearchPage";
import WelcomePage from "./pages/WelcomePage";
import { AuthProvider, useAuth } from "./lib/mockAuth";
import { AgentStateProvider, useAgentReset } from "./lib/agentStore";

/** Pixelarticons SVG paths */
const ICON_ARCHIVE = "M3 3h18v4H3V3zm1 1v2h16V4H4zm-1 5h18v4H3V9zm1 1v2h16v-2H4zm-1 5h18v4H3v-4zm1 1v2h16v-2H4z";
const ICON_SPARKLES = "M12 1l3 7 7 1-5 5 1 8-6-4-6 4 1-8-5-5 7-1z";
const ICON_SEARCH = "M10 2a8 8 0 100 16 8 8 0 000-16zm0 2a6 6 0 110 12 6 6 0 010-12zm8 12l4 4-2 2-4-4v-2z";

const nav = [
  { to: "/documents", icon: ICON_ARCHIVE, label: "资料库" },
  { to: "/agent", icon: ICON_SPARKLES, label: "精灵对话" },
  { to: "/search", icon: ICON_SEARCH, label: "检索" },
];

function AppShell() {
  const { logout } = useAuth();
  const resetAgent = useAgentReset();
  const handleLogout = () => { resetAgent(); logout(); window.location.href = "/"; };

  return (
    <div className="h-screen flex flex-col p-6 overflow-hidden">
      <div className="flex-1 min-h-0 flex flex-col max-w-[1480px] mx-auto w-full gap-4">
        <header className="mc-nav">
          <div className="mc-nav-brand">
            <span style={{ fontSize: 16 }}>◈</span>
            EnchantCraft
          </div>
          <nav className="mc-nav-links">
            {nav.map(({ to, icon, label }) => (
              <NavLink
                key={to}
                to={to}
                className={({ isActive }) =>
                  `mc-nav-link flex items-center gap-2 ${isActive ? "active" : ""}`
                }
              >
                <svg viewBox="0 0 24 24" style={{ width: 15, height: 15, fill: "currentColor" }}>
                  <path d={icon} />
                </svg>
                {label}
              </NavLink>
            ))}
          </nav>
          <div className="mc-nav-right">
            <span>◈ pgvector</span>
            <span>◈ DeepSeek</span>
            <button onClick={handleLogout}>▸ 退出</button>
          </div>
        </header>

        <div className="flex-1 min-h-0 flex flex-col glass-lg overflow-hidden">
          <Outlet />
        </div>

        <footer style={{ fontFamily: "var(--font-mc)", fontSize: 10, color: "#8a7a5a", opacity: 0.8, letterSpacing: "0.1em", textAlign: "center" }}>
          EnchantCraft · 附魔工坊 · v7.0
        </footer>
      </div>
    </div>
  );
}

function ProtectedRoute() {
  const { authed } = useAuth();
  return authed ? <AppShell /> : <Navigate to="/" replace />;
}

export default function App() {
  return (
    <AuthProvider>
      <AgentStateProvider>
      <BrowserRouter>
        <Routes>
          <Route path="/" element={<WelcomePage />} />
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
