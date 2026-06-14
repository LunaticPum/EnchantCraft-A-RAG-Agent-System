import { useEffect, useRef, useState } from "react";
import { useNavigate } from "react-router-dom";
import { useAuth } from "../lib/mockAuth";
import { api, setAuthToken } from "../lib/api";
import McModal from "../lib/McModal";

/** 随机 splash 词库 */
const SPLASH_POOL = [
  "RAG混合检索",
  "DeepSeek-v4-pro 驱动",
  "阿里云百炼 Embedding",
  "Bagu Skill - 生成你的八股",
  "pgvector HNSW加速",
  "gte-rerank-v2 重排序",
  "2种Agent检索模式",
  "zhparser中文分词",
  "Kafka异步索引",
  "附魔你的笔记吧",
  "语义+关键词双路检索",
  "格利摩尔在召唤",
];

/** Monocraft SGA 符文 (U+EB40~EB59 = A~Z) */
function randomRuneWord(): string {
  const len = 3 + Math.floor(Math.random() * 5); // 3~7个符文
  let s = "";
  for (let i = 0; i < len; i++) {
    s += String.fromCodePoint(0xEB40 + Math.floor(Math.random() * 26));
  }
  return s;
}

/** Pixelarticons 像素图标 SVG */
const ICON_BOOK_OPEN = "M2 3h9v2H2zM0 19h11v2H0zM13 3h9v2h-9zm0 16h11v2H13zM11 5h2v18h-2zM0 5h2v14H0zm22 0h2v14h-2zm-7 2h5v2h-5zm0 4h5v2h-5zm0 4h2v2h-2z";
const ICON_AI_VIEW = "M2 15h2v2H2zm0 4h2v-2H2zm20-4h-2v2h2zm0 4h-2v-2h2zM4 13h4v2H4zm0 8h4v-2H4zm16-8h-4v2h4zm0 8h-4v-2h4zM8 11h8v2H8zm0 12h8v-2H8zm2-8h4v4h-4zm1-6V5h2v4zM3 7V5h2v2zm2 2V7h2v2zm14-2V5h2v2zm-2 2V7h2v2zM9 5V3h2v2zM1 5V3h2v2zm16 0V3h2v2zm-6-2V1h2v2zM3 3V1h2v2zm16 0V1h2v2zm-6 2V3h2v2zM5 5V3h2v2zm16 0V3h2v2z";
const ICON_CALCULATOR = "M5 2h14v2H5zm0 18h14v2H5zM3 4h2v16H3zm16 0h2v16h-2zM7 6h10v4H7zm0 6h2v2H7zm4 0h2v2h-2zm4 0h2v2h-2zm-8 4h2v2H7zm4 0h2v2h-2zm4 0h2v2h-2z";
const ICON_COFFEE = "M4 4h16v2H4zm0 2h2v8H4zm2 8h10v2H6zm14-8h2v4h-2zm-2 4h2v2h-2zm-2-4h2v8h-2zM2 18h18v2H2z";

const ICON_SEARCH = "M22 22h-2v-2h2v2Zm-2-2h-2v-2h2v2Zm-6-2H6v-2h8v2Zm4 0h-2v-2h2v2ZM6 16H4v-2h2v2Zm10 0h-2v-2h2v2ZM4 14H2V6h2v8Zm14 0h-2V6h2v8ZM6 6H4V4h2v2Zm10 0h-2V4h2v2Zm-2-2H6V2h8v2Z";

const menuItems = [
  { icon: ICON_COFFEE, label: "进入工坊", to: "/documents", primary: true },
  { icon: ICON_BOOK_OPEN, label: "资料库", to: "/documents" },
  { icon: ICON_AI_VIEW, label: "精灵对话", to: "/agent" },
  { icon: ICON_SEARCH, label: "知识检索", to: "/search" },
  { icon: ICON_CALCULATOR, label: "八股出题", to: "/bagu" },
];

const STATUS_TECHS = [
  "DeepSeek-v4-pro 已连接",
  "DashScope Embedding 就绪",
  "pgvector HNSW 索引健康",
  "阿里云百炼 Rerank 在线",
  "Kafka 消息队列正常",
  "zhparser 中文分词已加载",
];

export default function WelcomePage() {
  const navigate = useNavigate();
  const { login: authLogin, authed } = useAuth();
  const [splashIdx, setSplashIdx] = useState(0);
  const [statusTech, setStatusTech] = useState(STATUS_TECHS[0]);
  const runeContainerRef = useRef<HTMLDivElement>(null);

  /* 弹窗状态 */
  const [modalOpen, setModalOpen] = useState(false);
  const [modalTab, setModalTab] = useState<"login" | "register">("login");
  const [username, setUsername] = useState("");
  const [password, setPassword] = useState("");
  const [email, setEmail] = useState("");
  const [authError, setAuthError] = useState("");
  const [authOk, setAuthOk] = useState("");
  const [authLoading, setAuthLoading] = useState(false);

  const openModal = (tab: "login" | "register") => {
    setModalTab(tab);
    setAuthError(""); setAuthOk("");
    setUsername(""); setPassword(""); setEmail("");
    setModalOpen(true);
  };

  const handleAuth = async () => {
    setAuthError(""); setAuthOk("");
    if (!username || username.length < 3) { setAuthError("用户名至少 3 位"); return; }
    if (!password || password.length < 4) { setAuthError("密码至少 4 位"); return; }
    setAuthLoading(true);
    try {
      if (modalTab === "login") {
        const res = await api.login(username, password);
        setAuthToken(res.token);
        authLogin({ userId: res.userId, username: res.username, role: res.role });
        setModalOpen(false);
        navigate("/documents", { replace: true });
      } else {
        await api.register(username, password, email);
        setAuthOk("注册成功，请登录");
        setModalTab("login");
        setPassword("");
      }
    } catch (e: any) { setAuthError(e.message || "操作失败"); }
    finally { setAuthLoading(false); }
  };

  /* Splash 轮播 — 用 state 驱动，不用 ref 操作 DOM */
  useEffect(() => {
    const timer = setInterval(() => {
      setSplashIdx(i => (i + 1) % SPLASH_POOL.length);
    }, 6000);
    return () => clearInterval(timer);
  }, []);

  /* 状态栏轮播 */
  useEffect(() => {
    const timer = setInterval(() => {
      setStatusTech(STATUS_TECHS[Math.floor(Math.random() * STATUS_TECHS.length)]);
    }, 8000);
    return () => clearInterval(timer);
  }, []);

  /* 附魔符文背景 */
  useEffect(() => {
    const container = runeContainerRef.current;
    if (!container) return;

    // 符文
    for (let i = 0; i < 12; i++) {
      const span = document.createElement("span");
      span.className = "mc-rune";
      span.textContent = randomRuneWord();
      span.style.left = Math.random() * 100 + "%";
      span.style.top = Math.random() * 100 + "%";
      span.style.fontSize = (14 + Math.random() * 14) + "px";
      span.style.animationDuration = (4 + Math.random() * 4) + "s";
      span.style.animationDelay = Math.random() * 8 + "s";
      span.style.color = Math.random() > 0.5 ? "#6a4a80" : "#7a5a95";
      container.appendChild(span);
    }
    // 荧光粒子
    for (let i = 0; i < 40; i++) {
      const p = document.createElement("div");
      p.className = "mc-particle";
      p.style.left = Math.random() * 100 + "%";
      p.style.top = Math.random() * 100 + "%";
      p.style.animationDuration = (6 + Math.random() * 10) + "s";
      p.style.animationDelay = Math.random() * 8 + "s";
      p.style.background = Math.random() > 0.5 ? "#f5e080" : "#ffe8a0";
      const size = 1.5 + Math.random() * 2.5;
      p.style.width = p.style.height = size + "px";
      container.parentElement?.appendChild(p);
    }
  }, []);

  return (
    <div className="min-h-screen flex flex-col">
      <div className="flex-1 flex items-center justify-center" style={{ padding: "12px 24px" }}>
        <div className="mc-scene w-full flex flex-col items-center" style={{ maxWidth: "95vw", minHeight: "88vh", paddingTop: 80, paddingBottom: 64 }}>
        {/* 符文背景层 */}
        <div className="mc-runes" ref={runeContainerRef} />

        {/* Logo 区 */}
        <div className="flex flex-col items-center pt-4 relative z-[3]">
          <div style={{ position: "relative", display: "inline-block" }}>
            <span
              className="mc-splash"
              style={{
                position: "absolute",
                left: 0, top: 10,
                zIndex: 4,
              }}
            >
              {SPLASH_POOL[splashIdx]}
            </span>
            <h1 className="mc-title" style={{ position: "relative", zIndex: 3 }}>EnchantCraft</h1>
          </div>
          <p className="mc-sub mt-1">附 魔 工 坊</p>
          <p className="mc-desc mt-1">基于 RAG 检索的 Agent 智能问答系统</p>
        </div>

        {/* MC 竖排菜单 */}
        <div className="flex flex-col items-center gap-0 relative z-[3] mt-16 mb-8" style={{ width: 380 }}>
          {menuItems.map(({ icon, label, to, primary }) => (
            <button
              key={label}
              onClick={() => {
                if (primary) { openModal("login"); return; }
                if (authed) { navigate(to); } else { openModal("login"); }
              }}
              className={`mc-menu-vert ${primary ? "mc-menu-vert--primary" : ""}`}
            >
              <span className="mc-icon">
                <svg viewBox="0 0 24 24" xmlns="http://www.w3.org/2000/svg">
                  <path d={icon} />
                </svg>
              </span>
              <span>{label}</span>
            </button>
          ))}
        </div>

        {/* 状态栏 */}
        <div className="flex justify-center relative z-[3] mt-6">
          <div className="mc-status">
            <span>12本附魔书已入库</span>
            <span>{statusTech}</span>
            <span>v7.0 附魔台版本</span>
          </div>
        </div>
      </div>
      </div>

      {/* 页脚 */}
      <footer className="text-center" style={{ fontFamily: "var(--font-mc)", fontSize: 10, color: "#8a7a5a", letterSpacing: "0.08em", opacity: 0.8, padding: "4px 0 8px" }}>
        EnchantCraft · 开源协议 MIT · 联系邮箱 1637435385@qq.com
      </footer>

      {/* MC 弹窗 */}
      <McModal open={modalOpen} onClose={() => setModalOpen(false)} title={modalTab === "login" ? "登录" : "注册"}>
        <div style={{ display: "flex", flexDirection: "column", gap: 10 }}>
          <input
            className="mc-input" type="text" value={username}
            onChange={(e) => setUsername(e.target.value)}
            placeholder="用户名"
            onKeyDown={(e) => e.key === "Enter" && handleAuth()}
          />
          <input
            className="mc-input" type="password" value={password}
            onChange={(e) => setPassword(e.target.value)}
            placeholder="密码"
            onKeyDown={(e) => e.key === "Enter" && handleAuth()}
          />
          {modalTab === "register" && (
            <input
              className="mc-input" type="text" value={email}
              onChange={(e) => setEmail(e.target.value)}
              placeholder="邮箱（选填）"
            />
          )}
          {authError && (
            <p className="text-xs" style={{ color: "var(--color-danger)", background: "rgba(200,122,106,0.1)", padding: "6px 10px", borderRadius: 2 }}>{authError}</p>
          )}
          {authOk && (
            <p className="text-xs" style={{ color: "var(--color-pass)", background: "rgba(126,168,139,0.1)", padding: "6px 10px", borderRadius: 2 }}>{authOk}</p>
          )}
          <button onClick={handleAuth} disabled={authLoading} className="mc-menu-vert mc-menu-vert--primary" style={{ width: "100%", justifyContent: "center", borderBottom: "2px solid #6b5020" }}>
            {authLoading ? "处理中..." : modalTab === "login" ? "登录" : "注册"}
          </button>
          <p style={{ textAlign: "center", fontFamily: "var(--font-mc)", fontSize: 10, color: "#8a7a5a", cursor: "pointer", margin: 0 }}
            onClick={() => { setModalTab(modalTab === "login" ? "register" : "login"); setAuthError(""); setAuthOk(""); }}>
            {modalTab === "login" ? "没有账号？去注册" : "已有账号？去登录"}
          </p>
        </div>
      </McModal>
    </div>
  );
}
