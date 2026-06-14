import { useEffect, useRef } from "react";
import { useNavigate } from "react-router-dom";

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

/** 符文字符集 */
const RUNES = "ᔑʖᓵ↸ᒷ⎓⊣⍑╎⋮ꖌꖎᒲリ𝙹!ᑑ∷ᓭℸ⚍⍊∴̇|⨅⊑";
const RUNE_WORDS = ["⍑ᒷꖎꖎ𝙹", "⍊╎ʖ∷ᔑ", "ᒲᔑ⊣╎ᓵ", "ᓵ∷||!ℸ", "ᒷリᓵ⍑ᔑ", "!¡𝙹ℸ╎𝙹", "∴╎ᓭ↸𝙹", "⎓ꖎ𝙹∴ᒷ∷"];

/** Pixelarticons 像素图标 SVG */
const ICON_BOOK_OPEN = "M2 3h9v2H2zM0 19h11v2H0zM13 3h9v2h-9zm0 16h11v2H13zM11 5h2v18h-2zM0 5h2v14H0zm22 0h2v14h-2zm-7 2h5v2h-5zm0 4h5v2h-5zm0 4h2v2h-2z";
const ICON_AI_VIEW = "M2 15h2v2H2zm0 4h2v-2H2zm20-4h-2v2h2zm0 4h-2v-2h2zM4 13h4v2H4zm0 8h4v-2H4zm16-8h-4v2h4zm0 8h-4v-2h4zM8 11h8v2H8zm0 12h8v-2H8zm2-8h4v4h-4zm1-6V5h2v4zM3 7V5h2v2zm2 2V7h2v2zm14-2V5h2v2zm-2 2V7h2v2zM9 5V3h2v2zM1 5V3h2v2zm16 0V3h2v2zm-6-2V1h2v2zM3 3V1h2v2zm16 0V1h2v2zm-6 2V3h2v2zM5 5V3h2v2zm16 0V3h2v2z";
const ICON_CALCULATOR = "M5 2h14v2H5zm0 18h14v2H5zM3 4h2v16H3zm16 0h2v16h-2zM7 6h10v4H7zm0 6h2v2H7zm4 0h2v2h-2zm4 0h2v2h-2zm-8 4h2v2H7zm4 0h2v2h-2zm4 0h2v2h-2z";
const ICON_LIST_BOX = "M4 2h16v2H4zm2 5h2v2H6zm4 0h8v2h-8zm-4 4h2v2H6zm4 0h8v2h-8zm-4 4h2v2H6zm4 0h8v2h-8zm-6 5h16v2H4zM2 4h2v16H2zm18 0h2v16h-2z";

const menuItems = [
  { icon: ICON_BOOK_OPEN, label: "资料库" },
  { icon: ICON_AI_VIEW, label: "精灵对话" },
  { icon: ICON_CALCULATOR, label: "八股出题" },
  { icon: ICON_LIST_BOX, label: "我的题库" },
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
  const splashRef = useRef<HTMLDivElement>(null);
  const statusHealthRef = useRef<HTMLSpanElement>(null);
  const runeContainerRef = useRef<HTMLDivElement>(null);

  /* Splash 随机轮播 */
  useEffect(() => {
    let idx = 0;
    const el = splashRef.current;
    if (!el) return;
    el.textContent = SPLASH_POOL[0];

    const timer = setInterval(() => {
      el.style.opacity = "0";
      setTimeout(() => {
        idx = (idx + 1) % SPLASH_POOL.length;
        el.textContent = SPLASH_POOL[idx];
        el.style.opacity = "1";
      }, 500);
    }, 6000);
    return () => clearInterval(timer);
  }, []);

  /* 状态栏轮播 */
  useEffect(() => {
    const el = statusHealthRef.current;
    if (!el) return;
    const timer = setInterval(() => {
      el.textContent = STATUS_TECHS[Math.floor(Math.random() * STATUS_TECHS.length)];
    }, 8000);
    return () => clearInterval(timer);
  }, []);

  /* 附魔符文背景 */
  useEffect(() => {
    const container = runeContainerRef.current;
    if (!container) return;

    // 符文
    for (let i = 0; i < 26; i++) {
      const span = document.createElement("span");
      span.className = "mc-rune";
      span.textContent = Math.random() > 0.6
        ? RUNE_WORDS[Math.floor(Math.random() * RUNE_WORDS.length)]
        : RUNES[Math.floor(Math.random() * RUNES.length)];
      span.style.left = Math.random() * 100 + "%";
      span.style.bottom = Math.random() * 100 + "%";
      span.style.fontSize = (10 + Math.random() * 14) + "px";
      span.style.animationDuration = (8 + Math.random() * 16) + "s";
      span.style.animationDelay = Math.random() * 14 + "s";
      span.style.color = Math.random() > 0.5 ? "#3a2a50" : "#4a3060";
      container.appendChild(span);
    }
    // 金色粒子
    for (let i = 0; i < 30; i++) {
      const p = document.createElement("div");
      p.className = "mc-particle";
      p.style.left = Math.random() * 100 + "%";
      p.style.top = Math.random() * 100 + "%";
      p.style.animationDuration = (6 + Math.random() * 12) + "s";
      p.style.animationDelay = Math.random() * 8 + "s";
      const size = 1 + Math.random() * 2;
      p.style.width = p.style.height = size + "px";
      container.parentElement?.appendChild(p);
    }
  }, []);

  return (
    <div className="min-h-screen flex flex-col items-center justify-center p-8">
      <div className="mc-scene w-full max-w-[720px]" style={{ minHeight: 460 }}>
        {/* 符文背景层 */}
        <div className="mc-runes" ref={runeContainerRef} />

        {/* Logo 区 */}
        <div className="flex flex-col items-center pt-20 pb-6 relative z-[3]">
          <div className="relative flex flex-col items-center">
            {/* Splash */}
            <div
              ref={splashRef}
              className="mc-splash"
              style={{
                position: "absolute",
                left: 2, top: -2,
                transform: "translate(-50%, -50%) rotate(-18deg)",
                transformOrigin: "center",
              }}
            />
            <h1 className="mc-title">EnchantCraft</h1>
          </div>

          <p className="mc-sub mt-2">附 魔 工 坊</p>
          <p className="mc-desc mt-1.5">基于 RAG 检索的 Agent 智能问答系统</p>

          {/* 主按钮 */}
          <button onClick={() => navigate("/login")} className="mc-btn mt-6">
            ▣ 进入工坊
          </button>
        </div>

        {/* 功能菜单 */}
        <div className="flex justify-center relative z-[3] mt-8">
          <div className="mc-menu">
            {menuItems.map(({ icon, label }) => (
              <div key={label} className="mc-menu-item">
                <span className="mc-icon">
                  <svg viewBox="0 0 24 24" xmlns="http://www.w3.org/2000/svg">
                    <path d={icon} />
                  </svg>
                </span>
                {label}
              </div>
            ))}
          </div>
        </div>

        {/* 状态栏 */}
        <div className="flex justify-center relative z-[3] mt-5 pb-8">
          <div className="mc-status">
            <span>12本附魔书已入库</span>
            <span ref={statusHealthRef}>{STATUS_TECHS[0]}</span>
            <span>v7.0 附魔台版本</span>
          </div>
        </div>
      </div>
    </div>
  );
}
