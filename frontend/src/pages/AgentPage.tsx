import { useState, useRef, useEffect } from "react";
import { Send, Sparkles, Link2, Bot, User, ToggleLeft, ToggleRight, ChevronDown } from "lucide-react";
import { api } from "../lib/api";
import { MdViewer } from "../lib/markdown";
import { useAgentStore } from "../lib/agentStore";

type Mode = "FORCE" | "TOOL";

export default function AgentPage() {
  const { messages: msgs, setMessages: setMsgs, sending, setSending, sessionId: sid, setSessionId: setSid } = useAgentStore();
  const [mode, setMode] = useState<Mode>("FORCE");
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
    <div className="h-full flex flex-col">
      {/* Fixed header */}
      <div className="flex-shrink-0 flex items-center justify-between px-6 py-3 border-b border-[var(--color-line)]">
        <div className="flex items-center gap-2.5">
          <Bot size={18} className="text-[var(--color-accent)]" />
          <span className="text-sm font-medium text-[var(--color-ink)]">AI 助手</span>
          {sending && <span className="w-1.5 h-1.5 rounded-full bg-[var(--color-pass)] animate-pulse" />}
        </div>
        <button onClick={() => setMode((m) => (m === "FORCE" ? "TOOL" : "FORCE"))}
          className="flex items-center gap-2 text-xs text-[var(--color-ink-soft)] hover:text-[var(--color-accent)] transition-colors">
          {mode === "FORCE" ? <><ToggleLeft size={18} className="text-[var(--color-accent)]" /> 强制检索</>
          : <><ToggleRight size={18} className="text-[var(--color-accent)]" /> Tool Calling</>}
        </button>
      </div>

      {/* Scrollable messages */}
      <div className="flex-1 min-h-0 overflow-y-auto px-6 py-4 space-y-4">
        {msgs.length === 0 && (
          <div className="text-center text-[13px] text-[var(--color-ink-faint)] py-8">
            <Sparkles size={16} className="inline mr-1.5 text-[var(--color-accent)]" />
            提出问题，AI 将从知识库查找证据并回答
          </div>
        )}
        {msgs.map((m, i) => (
          <div key={i} className={`flex gap-3 ${m.role === "USER" ? "flex-row-reverse" : ""}`}>
            <div className={`w-8 h-8 rounded-full flex items-center justify-center flex-shrink-0 ${
              m.role === "USER" ? "bg-[var(--color-bg-input)]" : "bg-[var(--color-accent-soft)]"}`}>
              {m.role === "USER" ? <User size={14} className="text-[var(--color-ink-faint)]" />
              : <Bot size={14} className="text-[var(--color-accent)]" />}
            </div>
            <div className={`max-w-[72%] ${m.role === "USER" ? "items-end" : ""}`}>
              {m.role === "USER" ? (
                <div className="p-3.5 rounded-2xl rounded-br-md bg-[var(--color-bg-input)] text-sm text-[var(--color-ink)]">{m.content}</div>
              ) : m.done ? (
                <div className="p-3.5 rounded-2xl rounded-bl-md bg-[var(--color-bg-card)] border border-[var(--color-line)]">
                  <MdViewer content={m.content} maxLen={99999} />
                </div>
              ) : (
                <div className="p-3.5 rounded-2xl rounded-bl-md bg-[var(--color-bg-card)] border border-[var(--color-line)] text-sm text-[var(--color-ink-soft)] whitespace-pre-wrap">
                  {m.content}<span className="animate-pulse text-[var(--color-accent)]">▌</span>
                </div>
              )}
              {/* Collapsible citations */}
              {m.done && m.citations && m.citations.length > 0 && (
                <div className="mt-2">
                  <button
                    onClick={() => setMsgs((p) => p.map((x, j) => j === i ? { ...x, showCitations: !x.showCitations } : x))}
                    className="flex items-center gap-1.5 text-[11px] text-[var(--color-ink-faint)] hover:text-[var(--color-accent)] transition-colors"
                  >
                    <ChevronDown size={11} className={`transition-transform ${m.showCitations ? "rotate-180" : ""}`} />
                    引用 ({m.citations.length})
                  </button>
                  {m.showCitations && (
                    <div className="mt-1.5 space-y-1 border-l-2 border-[var(--color-line)] pl-3">
                      {m.citations.map((c) => (
                        <div key={c.chunkId} className="p-2 rounded-lg bg-[var(--color-bg-input)] text-xs">
                          <p className="text-[var(--color-cite)] font-mono text-[10px] truncate">{c.titlePath}</p>
                          <p className="text-[var(--color-ink-faint)] mt-0.5 line-clamp-1">{c.snippet}</p>
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

      {/* Fixed input bar */}
      <div className="flex-shrink-0 p-4 border-t border-[var(--color-line)]">
        <div className="flex items-center gap-3">
          <div className="flex-1 relative">
            <input value={input} onChange={(e) => setInput(e.target.value)}
              placeholder="输入问题..." className="input pr-36" disabled={sending}
              onKeyDown={(e) => e.key === "Enter" && !e.shiftKey && send()} />
            <span className="absolute right-4 top-1/2 -translate-y-1/2 text-[11px] text-[var(--color-ink-faint)]">
              {mode === "FORCE" ? "强制检索" : "Tool Calling"} · Enter
            </span>
          </div>
          <button onClick={send} disabled={sending}
            className="w-10 h-10 rounded-full bg-[var(--color-pill-dark)] text-[var(--color-pill-text)] flex items-center justify-center hover:brightness-110 flex-shrink-0 shadow-[var(--shadow-btn)] disabled:opacity-40">
            <Send size={16} />
          </button>
        </div>
      </div>
    </div>
  );
}