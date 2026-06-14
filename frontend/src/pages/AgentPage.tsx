import { useState, useRef, useEffect } from "react";
import { ChevronDown } from "lucide-react";
import { api } from "../lib/api";
import { MdViewer } from "../lib/markdown";
import { useAgentStore } from "../lib/agentStore";

/** Pixelarticons */
const ICON_AI = "M2 15h2v2H2zm0 4h2v-2H2zm20-4h-2v2h2zm0 4h-2v-2h2zM4 13h4v2H4zm0 8h4v-2H4zm16-8h-4v2h4zm0 8h-4v-2h4zM8 11h8v2H8zm0 12h8v-2H8zm2-8h4v4h-4zm1-6V5h2v4zM3 7V5h2v2zm2 2V7h2v2zm14-2V5h2v2zm-2 2V7h2v2zM9 5V3h2v2zM1 5V3h2v2zm16 0V3h2v2zm-6-2V1h2v2zM3 3V1h2v2zm16 0V1h2v2zm-6 2V3h2v2zM5 5V3h2v2zm16 0V3h2v2z";
const ICON_BOOK = "M2 3h9v2H2zM0 19h11v2H0zM13 3h9v2h-9zm0 16h11v2H13zM11 5h2v18h-2zM0 5h2v14H0zm22 0h2v14h-2zm-7 2h5v2h-5zm0 4h5v2h-5zm0 4h2v2h-2z";
const ICON_SEND = "M2 2h20v2H2zm0 4h14v2H2zm0 4h10v2H2zm0 4h16v2H2zm0 4h12v2H2z";

type Mode = "FORCE" | "TOOL";
const MODE_LABEL: Record<Mode, string> = { FORCE: "强制检索", TOOL: "智能检索" };

export default function AgentPage() {
  const { messages: msgs, setMessages: setMsgs, sending, setSending, sessionId: sid, setSessionId: setSid, mode, setMode } = useAgentStore();
  const [input, setInput] = useState("");
  const bottom = useRef<HTMLDivElement>(null);

  useEffect(() => { if (sending) bottom.current?.scrollIntoView({ behavior: "smooth" }); }, [msgs.length]);

  const send = () => {
    if (!input.trim() || sending) return;
    const q = input;
    setMsgs((p) => [...p, { role: "USER", content: q }, { role: "ASSISTANT", content: "" }]);
    setInput(""); setSending(true);
    api.agentChat({ sessionId: sid, message: q, retrievalMode: mode },
      (c) => setMsgs((p) => p.map((m, i) => i === p.length - 1 && m.role === "ASSISTANT" ? { ...m, citations: c } : m)),
      (t) => setMsgs((p) => p.map((m, i) => i === p.length - 1 && m.role === "ASSISTANT" ? { ...m, content: m.content + t.replace(/\[BR\]/g, "\n") } : m)),
      (s) => { if (s) setSid(s); setMsgs((p) => p.map((m, i) => i === p.length - 1 && m.role === "ASSISTANT" ? { ...m, done: true } : m)); setSending(false); },
      (e) => { setMsgs((p) => p.map((m, i) => i === p.length - 1 && m.role === "ASSISTANT" ? { ...m, content: `*(错误: ${e})*`, done: true } : m)); setSending(false); }
    );
  };

  return (
    <div className="h-full flex flex-col" style={{ background: "rgba(0,0,0,0.2)" }}>
      {/* Header */}
      <div className="flex-shrink-0 flex items-center px-5 py-3" style={{ background: "rgba(20,14,8,0.8)", borderBottom: "2px solid #3a1a08" }}>
        <svg viewBox="0 0 24 24" style={{ width: 18, height: 18, fill: "#c9a050", marginRight: 8 }}><path d={ICON_AI} /></svg>
        <span style={{ fontFamily: "var(--font-mc)", fontSize: 13, color: "#e8dcc8", letterSpacing: "0.06em" }}>精灵对话</span>
        {sending && <span className="w-1.5 h-1.5 rounded-full bg-[var(--color-pass)] animate-pulse ml-2" />}
      </div>

      {/* Messages */}
      <div className="flex-1 min-h-0 overflow-y-auto px-5 py-4 space-y-4" style={{ scrollbarWidth: "thin", scrollbarColor: "rgba(139,115,85,0.2) transparent" }}>
        {msgs.length === 0 && (
          <div className="text-center py-8">
            <p style={{ fontFamily: "var(--font-mc)", fontSize: 13, color: "#c0b090", margin: "0 0 12px" }}>
              <svg viewBox="0 0 24 24" style={{ width: 16, height: 16, fill: "#c9a050", display: "inline", marginRight: 4, verticalAlign: "middle" }}><path d={ICON_AI} /></svg>
              提出问题，精灵将从知识库查找证据并回答
            </p>
            <span
              onClick={() => setMode(mode === "FORCE" ? "TOOL" : "FORCE")}
              style={{ fontFamily: "var(--font-mc)", fontSize: 12, color: "#e8dcc8", cursor: "pointer", background: "rgba(60,40,18,0.5)", border: "2px solid #6b5020", padding: "6px 16px", display: "inline-block", letterSpacing: "0.04em" }}
            >
              当前模式：<strong style={{ color: "#f5e050" }}>{MODE_LABEL[mode]}</strong> · 点击切换
            </span>
          </div>
        )}
        {msgs.map((m, i) => (
          <div key={i} className={`flex gap-3 ${m.role === "USER" ? "flex-row-reverse" : ""}`}>
            <div className={`w-8 h-8 flex items-center justify-center flex-shrink-0`}
              style={{ border: m.role === "USER" ? "2px solid #5a3018" : "2px solid #6b5020", background: m.role === "USER" ? "rgba(42,22,10,0.6)" : "rgba(60,40,20,0.6)" }}>
              {m.role === "USER" ? (
                <span style={{ fontFamily: "var(--font-mc)", fontSize: 10, color: "#8a7a5a" }}>你</span>
              ) : (
                <svg viewBox="0 0 24 24" style={{ width: 14, height: 14, fill: "#c9a050" }}><path d={ICON_AI} /></svg>
              )}
            </div>
            <div className={`max-w-[72%] ${m.role === "USER" ? "items-end" : ""}`}>
              {m.role === "USER" ? (
                <div className="p-3.5 text-sm" style={{ background: "rgba(42,22,10,0.5)", border: "2px solid #3a1a08", color: "#e8dcc8", fontFamily: "var(--font-mc)", fontSize: 12 }}>{m.content}</div>
              ) : m.done ? (
                <div className="p-3.5" style={{ background: "rgba(20,14,8,0.6)", border: "2px solid #2a1a0a" }}>
                  <MdViewer content={m.content} maxLen={99999} />
                </div>
              ) : (
                <div className="p-3.5 text-sm" style={{ background: "rgba(20,14,8,0.6)", border: "2px solid #2a1a0a", color: "#a8a39b", fontFamily: "var(--font-mc)", fontSize: 12, whiteSpace: "pre-wrap" }}>
                  {m.content}<span className="animate-pulse" style={{ color: "#c9a050" }}>▌</span>
                </div>
              )}
              {m.done && m.citations && m.citations.length > 0 && (
                <div className="mt-2">
                  <button
                    onClick={() => setMsgs((p) => p.map((x, j) => j === i ? { ...x, showCitations: !x.showCitations } : x))}
                    style={{ fontFamily: "var(--font-mc)", fontSize: 10, color: "#8a7a5a", background: "none", border: "none", cursor: "pointer", display: "flex", alignItems: "center", gap: 4 }}
                  >
                    <ChevronDown size={11} className={`transition-transform ${m.showCitations ? "rotate-180" : ""}`} />
                    引用 ({m.citations.length})
                  </button>
                  {m.showCitations && (
                    <div className="mt-1.5 space-y-1 pl-3" style={{ borderLeft: "2px solid #3a1a08" }}>
                      {m.citations.map((c) => (
                        <div key={c.chunkId} className="p-2 text-xs" style={{ background: "rgba(42,22,10,0.3)", border: "1px solid #2a1a0a" }}>
                          <p style={{ color: "#a898c4", fontFamily: "var(--font-mc)", fontSize: 9, margin: 0 }}>{c.titlePath}</p>
                          <p style={{ color: "#6b6560", fontSize: 10, margin: "2px 0 0" }}>{c.snippet}</p>
                        </div>
                      ))}
                    </div>
                  )}
                </div>
              )}
            </div>
          </div>
        ))}
        <div ref={bottom} />
      </div>

      {/* MC 风格输入框 */}
      <div className="flex-shrink-0 p-4" style={{ background: "rgba(20,14,8,0.8)", borderTop: "2px solid #3a1a08" }}>
        <div className="flex items-center gap-3">
          <div className="flex-1 relative">
            <input value={input} onChange={(e) => setInput(e.target.value)}
              placeholder="输入问题..." className="mc-input" disabled={sending}
              style={{ paddingRight: 80 }}
              onKeyDown={(e) => e.key === "Enter" && !e.shiftKey && send()} />
            <span style={{ position: "absolute", right: 14, top: "50%", transform: "translateY(-50%)", fontFamily: "var(--font-mc)", fontSize: 10, color: "#8a7a5a", pointerEvents: "none" }}>
              Enter 发送
            </span>
          </div>
          <button
            onClick={() => setMode(mode === "FORCE" ? "TOOL" : "FORCE")}
            className="mc-btn" style={{ padding: "10px 16px", fontSize: 12, whiteSpace: "nowrap" }}
          >
            {MODE_LABEL[mode]}
          </button>
          <button onClick={send} disabled={sending} className="mc-btn" style={{ padding: "10px 16px", fontSize: 12 }}>
            <svg viewBox="0 0 24 24" style={{ width: 14, height: 14, fill: "currentColor" }}><path d={ICON_SEND} /></svg>
            发送
          </button>
        </div>
      </div>
    </div>
  );
}
