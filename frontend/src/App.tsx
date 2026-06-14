import { useEffect, useRef } from "react";
import { BrowserRouter, Routes, Route, NavLink, Navigate, Outlet } from "react-router-dom";
import DocumentPage from "./pages/DocumentPage";
import AgentPage from "./pages/AgentPage";
import SearchPage from "./pages/SearchPage";
import WelcomePage from "./pages/WelcomePage";
import { AuthProvider, useAuth } from "./lib/mockAuth";
import { AgentStateProvider, useAgentReset } from "./lib/agentStore";

/** Pixelarticons SVG paths */
const ICON_BOOK = "M2 3h9v2H2zM0 19h11v2H0zM13 3h9v2h-9zm0 16h11v2H13zM11 5h2v18h-2zM0 5h2v14H0zm22 0h2v14h-2zm-7 2h5v2h-5zm0 4h5v2h-5zm0 4h2v2h-2z";
const ICON_AI = "M2 15h2v2H2zm0 4h2v-2H2zm20-4h-2v2h2zm0 4h-2v-2h2zM4 13h4v2H4zm0 8h4v-2H4zm16-8h-4v2h4zm0 8h-4v-2h4zM8 11h8v2H8zm0 12h8v-2H8zm2-8h4v4h-4zm1-6V5h2v4zM3 7V5h2v2zm2 2V7h2v2zm14-2V5h2v2zm-2 2V7h2v2zM9 5V3h2v2zM1 5V3h2v2zm16 0V3h2v2zm-6-2V1h2v2zM3 3V1h2v2zm16 0V1h2v2zm-6 2V3h2v2zM5 5V3h2v2zm16 0V3h2v2z";
const ICON_SEARCH = "M22 22h-2v-2h2v2Zm-2-2h-2v-2h2v2Zm-6-2H6v-2h8v2Zm4 0h-2v-2h2v2ZM6 16H4v-2h2v2Zm10 0h-2v-2h2v2ZM4 14H2V6h2v8Zm14 0h-2V6h2v8ZM6 6H4V4h2v2Zm10 0h-2V4h2v2Zm-2-2H6V2h8v2Z";
const ICON_CALC = "M5 2h14v2H5zm0 18h14v2H5zM3 4h2v16H3zm16 0h2v16h-2zM7 6h10v4H7zm0 6h2v2H7zm4 0h2v2h-2zm4 0h2v2h-2zm-8 4h2v2H7zm4 0h2v2h-2zm4 0h2v2h-2z";

const nav = [
  { to: "/documents", icon: ICON_BOOK, label: "资料库" },
  { to: "/agent", icon: ICON_AI, label: "精灵对话" },
  { to: "/search", icon: ICON_SEARCH, label: "检索" },
  { to: "/bagu", icon: ICON_CALC, label: "八股出题" },
];

function AppShell() {
  const { logout } = useAuth();
  const resetAgent = useAgentReset();
  const handleLogout = () => { resetAgent(); logout(); window.location.href = "/"; };
  const starRef = useRef<HTMLDivElement>(null);

  /* 动态生成星空覆盖全宽 */
  useEffect(() => {
    const el = starRef.current;
    if (!el) return;
    const w = window.innerWidth;
    const h = window.innerHeight;
    const shadows1: string[] = [];
    const shadows2: string[] = [];
    for (let i = 0; i < 120; i++) {
      const x = Math.floor(Math.random() * w);
      const y = Math.floor(Math.random() * h);
      shadows1.push(`${x}px ${y}px #addfff`);
    }
    for (let i = 0; i < 60; i++) {
      const x = Math.floor(Math.random() * w);
      const y = Math.floor(Math.random() * h);
      shadows2.push(`${x}px ${y}px rgba(173,223,255,0.5)`);
    }
    el.style.boxShadow = shadows1.join(",");
    const a2 = el.nextElementSibling as HTMLElement;
    if (a2) a2.style.boxShadow = shadows2.join(",");
  }, []);

  return (
    <div className="h-screen flex flex-col p-4 overflow-hidden doc-scene">
      {/* 星空层 */}
      <div ref={starRef} style={{ position: "absolute", inset: 0, width: 1, height: 1, zIndex: 0, animation: "starsUp 120s linear infinite" }} />
      <div style={{ position: "absolute", inset: 0, width: 1, height: 1, zIndex: 0, animation: "starsUp 200s linear infinite" }} />
      {/* MC 像素月亮 (16x16) */}
      <div className="pixel-moon">
        <svg viewBox="0 0 16 16" xmlns="http://www.w3.org/2000/svg" shapeRendering="crispEdges">
          <rect x="5" y="1" width="6" height="1" fill="#ffe8c0"/>
          <rect x="3" y="2" width="10" height="1" fill="#ffe8c0"/>
          <rect x="2" y="3" width="12" height="1" fill="#ffe8c0"/>
          <rect x="1" y="4" width="14" height="8" fill="#ffe8c0"/>
          <rect x="3" y="5" width="10" height="6" fill="#f0d880"/>
          <rect x="4" y="6" width="8" height="4" fill="#e8d070"/>
          <rect x="2" y="12" width="12" height="1" fill="#ffe8c0"/>
          <rect x="3" y="13" width="10" height="1" fill="#ffe8c0"/>
          <rect x="5" y="14" width="6" height="1" fill="#ffe8c0"/>
        </svg>
      </div>
      {/* MC 像素云朵 (方块风) */}
      <div className="pixel-cloud" style={{ top: 40, right: 160, animationDuration: "70s", opacity: 0.45 }}>
        <svg viewBox="0 0 80 24" width="120" height="36" xmlns="http://www.w3.org/2000/svg" shapeRendering="crispEdges">
          <rect x="16" y="16" width="48" height="8" fill="#d0d0e0"/>
          <rect x="12" y="12" width="8" height="4" fill="#d0d0e0"/>
          <rect x="20" y="8" width="12" height="8" fill="#d0d0e0"/>
          <rect x="36" y="4" width="16" height="12" fill="#d0d0e0"/>
          <rect x="52" y="8" width="12" height="8" fill="#d0d0e0"/>
          <rect x="64" y="12" width="8" height="4" fill="#d0d0e0"/>
        </svg>
      </div>
      <div className="pixel-cloud" style={{ top: 70, left: 60, animationDuration: "90s", opacity: 0.35 }}>
        <svg viewBox="0 0 80 24" width="96" height="28" xmlns="http://www.w3.org/2000/svg" shapeRendering="crispEdges">
          <rect x="16" y="16" width="48" height="8" fill="#d0d0e0"/>
          <rect x="12" y="12" width="8" height="4" fill="#d0d0e0"/>
          <rect x="20" y="8" width="12" height="8" fill="#d0d0e0"/>
          <rect x="36" y="4" width="16" height="12" fill="#d0d0e0"/>
          <rect x="52" y="8" width="12" height="8" fill="#d0d0e0"/>
          <rect x="64" y="12" width="8" height="4" fill="#d0d0e0"/>
        </svg>
      </div>
      <div className="pixel-cloud" style={{ top: 90, right: 340, animationDuration: "80s", opacity: 0.3 }}>
        <svg viewBox="0 0 80 24" width="80" height="24" xmlns="http://www.w3.org/2000/svg" shapeRendering="crispEdges">
          <rect x="16" y="16" width="48" height="8" fill="#d0d0e0"/>
          <rect x="12" y="12" width="8" height="4" fill="#d0d0e0"/>
          <rect x="20" y="8" width="12" height="8" fill="#d0d0e0"/>
          <rect x="36" y="4" width="16" height="12" fill="#d0d0e0"/>
          <rect x="52" y="8" width="12" height="8" fill="#d0d0e0"/>
          <rect x="64" y="12" width="8" height="4" fill="#d0d0e0"/>
        </svg>
      </div>

      <div className="flex-1 min-h-0 flex flex-col max-w-[1600px] mx-auto w-full gap-3" style={{ zIndex: 2, position: "relative" }}>
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

        <div className="flex-1 min-h-0 flex flex-col overflow-hidden">
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
            <Route path="/bagu" element={<DocumentPage />} />
          </Route>
        </Routes>
      </BrowserRouter>
      </AgentStateProvider>
    </AuthProvider>
  );
}
