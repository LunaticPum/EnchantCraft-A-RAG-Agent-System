import { useState } from "react";
import { Send, Sparkles, Link2, Bot, User, ToggleLeft, ToggleRight } from "lucide-react";

type Mode = "FORCE" | "TOOL";

const messages = [
  { role: "USER", content: "InnoDB 的特点是什么？" },
  { role: "ASSISTANT", content: "根据《MySQL 存储引擎》笔记，InnoDB 是 MySQL 的默认存储引擎，具有以下核心特点：\n\n1. **事务支持**：完全遵循 ACID 模型，保证数据操作的原子性、一致性、隔离性和持久性。\n2. **行级锁**：对单条记录加锁而非整表，大幅提高高并发场景下的读写性能。\n3. **MVCC**：通过多版本并发控制实现非阻塞读取，进一步提升并发能力。\n4. **外键约束**：支持外键和级联操作，保证数据完整性与关联关系正确性。\n5. **B+Tree 索引**：采用聚簇索引组织数据，主键查询效率极高。",
    citations: [
      { chunkId: "c1", titlePath: "MySQL 存储引擎 > InnoDB > 索引结构", snippet: "InnoDB 采用 B+Tree 作为其主索引结构，数据按主键顺序聚簇存储..." },
      { chunkId: "c2", titlePath: "MySQL 存储引擎 > InnoDB > 特点", snippet: "InnoDB 支持事务、行级锁、MVCC 和外键约束..." },
    ] as const,
  },
];

export default function AgentPage() {
  const [mode, setMode] = useState<Mode>("FORCE");
  const [input, setInput] = useState("");

  return (
    <div className="h-full flex flex-col min-h-[600px]">
      {/* Header */}
      <div className="flex items-center justify-between px-6 py-3 border-b border-[var(--color-line)]">
        <div className="flex items-center gap-2.5">
          <Bot size={18} className="text-[var(--color-accent)]" />
          <span className="text-sm font-medium text-[var(--color-ink)]">AI 助手</span>
          <span className="w-1.5 h-1.5 rounded-full bg-[var(--color-pass)] animate-pulse" />
        </div>
        <button
          onClick={() => setMode((m) => (m === "FORCE" ? "TOOL" : "FORCE"))}
          className="flex items-center gap-2 text-xs text-[var(--color-ink-soft)] hover:text-[var(--color-accent)] transition-colors"
        >
          {mode === "FORCE" ? (
            <><ToggleLeft size={18} className="text-[var(--color-accent)]" /> 强制检索</>
          ) : (
            <><ToggleRight size={18} className="text-[var(--color-accent)]" /> Tool Calling</>
          )}
        </button>
      </div>

      {/* Messages */}
      <div className="flex-1 overflow-auto p-6 space-y-5">
        <div className="text-center text-[11px] text-[var(--color-ink-faint)] py-2">
          <Sparkles size={13} className="inline mr-1 text-[var(--color-accent)]" />
          会话已开始 — {mode === "FORCE" ? "每次对话自动检索知识库" : "AI 自主判断是否需要检索"}
        </div>
        {messages.map((m, i) => (
          <div key={i} className={`flex gap-3 ${m.role === "USER" ? "flex-row-reverse" : ""}`}>
            <div
              className={`w-9 h-9 rounded-full flex items-center justify-center flex-shrink-0 ${
                m.role === "USER"
                  ? "bg-[var(--color-bg-input)]"
                  : "bg-[var(--color-accent-soft)]"
              }`}
            >
              {m.role === "USER" ? (
                <User size={14} className="text-[var(--color-ink-faint)]" />
              ) : (
                <Bot size={14} className="text-[var(--color-accent)]" />
              )}
            </div>
            <div className={`max-w-[68%] ${m.role === "USER" ? "items-end" : ""}`}>
              <div
                className={`p-4 rounded-2xl text-sm leading-relaxed ${
                  m.role === "USER"
                    ? "bg-[var(--color-bg-input)] text-[var(--color-ink)] rounded-br-md"
                    : "prose max-w-none"
                }`}
              >
                {m.content}
              </div>
              {"citations" in m && m.citations && (
                <div className="mt-2.5 space-y-1.5">
                  {m.citations.map((c) => (
                    <div
                      key={c.chunkId}
                      className="flex items-start gap-2 p-2.5 rounded-xl bg-[var(--color-bg-card)] border border-[var(--color-line)] text-xs"
                    >
                      <Link2 size={12} className="mt-0.5 text-[var(--color-cite)] flex-shrink-0" />
                      <div>
                        <p className="text-[var(--color-cite)] font-mono text-[10px]">{c.titlePath}</p>
                        <p className="text-[var(--color-ink-faint)] mt-0.5 line-clamp-1">{c.snippet}</p>
                      </div>
                    </div>
                  ))}
                </div>
              )}
            </div>
          </div>
        ))}
      </div>

      {/* Input */}
      <div className="p-4 border-t border-[var(--color-line)]">
        <div className="flex items-center gap-3">
          <div className="flex-1 relative">
            <input
              value={input}
              onChange={(e) => setInput(e.target.value)}
              placeholder="输入你的问题..."
              className="input pr-40"
              onKeyDown={(e) => e.key === "Enter" && setInput("")}
            />
            <span className="absolute right-4 top-1/2 -translate-y-1/2 text-[11px] text-[var(--color-ink-faint)]">
              {mode === "FORCE" ? "强制检索" : "Tool Calling"} · Enter 发送
            </span>
          </div>
          <button className="w-10 h-10 rounded-full bg-[var(--color-pill-dark)] text-[var(--color-pill-text)] flex items-center justify-center hover:brightness-110 transition-all flex-shrink-0 shadow-[var(--shadow-btn)]">
            <Send size={16} />
          </button>
        </div>
      </div>
    </div>
  );
}