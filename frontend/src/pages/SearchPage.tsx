import { useState } from "react";
import { Search, Zap, Link2, ChevronDown } from "lucide-react";

const strategies = ["HYBRID", "SEMANTIC", "KEYWORD"];
const results = [
  { chunkId: "c1", titlePath: "MySQL 存储引擎 > InnoDB > 索引结构", content: "InnoDB 采用 B+Tree 作为其主索引结构，数据按主键顺序聚簇存储...", score: 0.892, documentId: "d1", source: "语义+关键词" },
  { chunkId: "c2", titlePath: "MySQL 存储引擎 > InnoDB > 特点", content: "InnoDB 支持事务、行级锁、MVCC 和外键约束，是 MySQL 5.5 起的默认引擎...", score: 0.765, documentId: "d1", source: "语义" },
  { chunkId: "c3", titlePath: "MySQL 存储引擎 > 概述", content: "MySQL 采用可插拔存储引擎架构，常见引擎包括 InnoDB、MyISAM、Memory...", score: 0.678, documentId: "d1", source: "关键词" },
  { chunkId: "c5", titlePath: "数据库原理 > 索引 > B+Tree", content: "B+Tree 是一种平衡多路搜索树，所有数据仅存储在叶子节点，非叶子节点只作索引...", score: 0.543, documentId: "d3", source: "语义" },
];

export default function SearchPage() {
  const [strategy, setStrategy] = useState("HYBRID");
  const [query, setQuery] = useState("");
  const [hasSearched, setHasSearched] = useState(false);
  const handleSearch = () => { if (query.trim()) setHasSearched(true); };

  return (
    <div className="h-full flex flex-col min-h-[600px]">
      <div className="p-8 pb-4">
        <h2 className="serif text-[28px] font-semibold text-[var(--color-ink)] mb-5">
          检索实验室
        </h2>
        <div className="flex items-center gap-3">
          <div className="flex-1 relative">
            <Search size={16} className="absolute left-4 top-1/2 -translate-y-1/2 text-[var(--color-ink-faint)]" />
            <input
              value={query} onChange={(e) => setQuery(e.target.value)}
              onKeyDown={(e) => e.key === "Enter" && handleSearch()}
              placeholder="输入检索关键词，如 InnoDB 存储引擎..."
              className="input pl-11 pr-20"
            />
            <span className="absolute right-4 top-1/2 -translate-y-1/2 text-[11px] text-[var(--color-ink-faint)]">
              RRF k=60
            </span>
          </div>
          <button onClick={handleSearch} className="pill pill--dark">
            <Zap size={14} /> 检索
          </button>
          <div className="flex items-center gap-1">
            {strategies.map((s) => (
              <button
                key={s}
                onClick={() => setStrategy(s)}
                className={`pill text-[11px] px-3 py-1.5 ${
                  strategy === s ? "pill--dark" : "pill--ghost"
                }`}
              >{s}</button>
            ))}
          </div>
        </div>
      </div>

      <div className="flex-1 overflow-auto px-8 pb-8">
        {hasSearched ? (
          <div className="space-y-3">
            <p className="text-xs text-[var(--color-ink-faint)] mb-4">
              检索 "<span className="text-[var(--color-ink-soft)]">{query}</span>"
              — <span className="text-[var(--color-accent)]">{strategy}</span>
              — 命中 <span className="text-[var(--color-ink)]">{results.length}</span> 条
            </p>
            {results.map((r, i) => (
              <div key={r.chunkId} className="p-5 rounded-3xl border border-[var(--color-line-strong)] bg-[var(--color-bg-card)] hover:border-[var(--color-line)] transition-colors cursor-pointer">
                <div className="flex items-start gap-4">
                  <div className="w-9 h-9 rounded-full bg-[var(--color-accent-soft)] flex items-center justify-center flex-shrink-0 text-xs font-semibold text-[var(--color-accent)]">
                    {i + 1}
                  </div>
                  <div className="flex-1 min-w-0">
                    <div className="flex items-center gap-2 mb-2">
                      <Link2 size={12} className="text-[var(--color-cite)]" />
                      <p className="text-xs text-[var(--color-cite)] font-mono truncate">{r.titlePath}</p>
                    </div>
                    <p className="serif text-[15px] leading-relaxed text-[var(--color-ink-soft)]">{r.content}</p>
                    <div className="flex items-center gap-3 mt-2 text-[11px] text-[var(--color-ink-faint)]">
                      <span>文档 #{r.documentId.slice(0, 8)}</span>
                      <span>·</span>
                      <span>{r.source}</span>
                    </div>
                  </div>
                  <div className="flex-shrink-0 text-right">
                    <span className="text-xl font-semibold text-[var(--color-accent)]">{r.score.toFixed(3)}</span>
                    <p className="text-[10px] text-[var(--color-ink-faint)]">score</p>
                  </div>
                </div>
              </div>
            ))}
          </div>
        ) : (
          <div className="flex flex-col items-center justify-center h-[400px] text-[var(--color-ink-faint)] space-y-3">
            <Search size={48} className="opacity-20" />
            <p className="text-sm">输入关键词查看多路检索召回效果</p>
            <p className="text-xs">支持 SEMANTIC / KEYWORD / HYBRID 三种策略</p>
          </div>
        )}
      </div>
    </div>
  );
}