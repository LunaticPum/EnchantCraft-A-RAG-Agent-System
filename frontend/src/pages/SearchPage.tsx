import { useState } from "react";
import { Search, Zap, Link2, Loader2, ChevronDown } from "lucide-react";
import { api, type SearchResultItem } from "../lib/api";
import { MdViewer } from "../lib/markdown";

const strategies = ["HYBRID", "SEMANTIC", "KEYWORD"];

export default function SearchPage() {
  const [strategy, setStrategy] = useState("HYBRID");
  const [query, setQuery] = useState("");
  const [results, setResults] = useState<SearchResultItem[] | null>(null);
  const [searching, setSearching] = useState(false);
  const [expandedIdx, setExpandedIdx] = useState<number | null>(null);

  const handleSearch = async () => {
    if (!query.trim() || searching) return;
    setSearching(true);
    try {
      setResults(await api.search(query, 5, strategy, true));
    } catch {
      setResults([]);
    } finally {
      setSearching(false);
    }
  };

  return (
    <div className="h-full flex flex-col">
      {/* Search bar */}
      <div className="p-8 pb-4">
        <h2 className="serif text-[28px] font-semibold text-[var(--color-ink)] mb-5">检索实验室</h2>
        <div className="flex items-center gap-3">
          <div className="flex-1 relative">
            <Search size={16} className="absolute left-4 top-1/2 -translate-y-1/2 text-[var(--color-ink-faint)] pointer-events-none" />
            <input
              value={query} onChange={(e) => setQuery(e.target.value)}
              onKeyDown={(e) => e.key === "Enter" && handleSearch()}
              placeholder="输入检索关键词，如 InnoDB 存储引擎..."
              className="input input-has-icon pr-20" disabled={searching}
            />
            <span className="absolute right-4 top-1/2 -translate-y-1/2 text-[11px] text-[var(--color-ink-faint)]">RRF k=60</span>
          </div>
          <button onClick={handleSearch} disabled={searching} className="pill pill--dark">
            {searching ? <Loader2 size={14} className="animate-spin" /> : <Zap size={14} />}
            {searching ? "检索中" : "检索"}
          </button>
          <div className="flex items-center gap-1">
            {strategies.map((s) => (
              <button key={s} onClick={() => setStrategy(s)}
                className={`pill text-[11px] px-3 py-1.5 ${strategy === s ? "pill--dark" : "pill--ghost"}`}>
                {s}
              </button>
            ))}
          </div>
        </div>
      </div>

      {/* Results */}
      <div className="flex-1 overflow-y-auto px-8 pb-8">
        {results === null ? (
          <div className="flex flex-col items-center justify-center h-[400px] text-[var(--color-ink-faint)] space-y-3">
            <Search size={48} className="opacity-20" />
            <p className="text-sm">输入关键词查看多路检索召回效果</p>
            <p className="text-xs">支持 SEMANTIC / KEYWORD / HYBRID 三种策略</p>
          </div>
        ) : results.length === 0 ? (
          <p className="text-sm text-[var(--color-ink-faint)] text-center py-12">未找到匹配内容</p>
        ) : (
          <div className="space-y-3">
            <p className="text-xs text-[var(--color-ink-faint)] mb-4">
              检索 "<span className="text-[var(--color-ink-soft)]">{query}</span>"
              — <span className="text-[var(--color-accent)]">{strategy}</span>
              — 命中 <span className="text-[var(--color-ink)]">{results.length}</span> 条
            </p>
            {results.map((r, i) => (
              <div key={r.chunkId}>
                <button
                  onClick={() => setExpandedIdx(expandedIdx === i ? null : i)}
                  className={`w-full text-left p-5 rounded-3xl border transition-all ${
                    expandedIdx === i
                      ? "border-[var(--color-accent-border)] bg-[var(--color-bg-card)]"
                      : "border-[var(--color-line-strong)] bg-[var(--color-bg-card)] hover:border-[var(--color-line)]"
                  }`}>
                  <div className="flex items-start gap-4">
                    <div className="w-9 h-9 rounded-full bg-[var(--color-accent-soft)] flex items-center justify-center flex-shrink-0 text-xs font-semibold text-[var(--color-accent)]">{i + 1}</div>
                    <div className="flex-1 min-w-0">
                      <div className="flex items-center gap-2 mb-2">
                        <Link2 size={12} className="text-[var(--color-cite)]" />
                        <p className="text-xs text-[var(--color-cite)] font-mono truncate">{r.titlePath}</p>
                      </div>
                      <p className="serif text-[15px] leading-relaxed text-[var(--color-ink-soft)] line-clamp-3">{r.content}</p>
                      <div className="flex items-center gap-3 mt-2 text-[11px] text-[var(--color-ink-faint)]">
                        <span>文档 #{r.documentId?.slice(0, 8)}</span>
                        <span className="flex items-center gap-1"><ChevronDown size={10} className={`transition-transform ${expandedIdx === i ? "rotate-180" : ""}`} /> {expandedIdx === i ? "收起" : "展开全文"}</span>
                      </div>
                    </div>
                    <div className="flex-shrink-0 text-right">
                      <span className="text-xl font-semibold text-[var(--color-accent)]">{r.score.toFixed(3)}</span>
                      <p className="text-[10px] text-[var(--color-ink-faint)]">score</p>
                    </div>
                  </div>
                </button>
                {expandedIdx === i && (
                  <div className="mt-2 p-5 rounded-3xl border border-[var(--color-line)] bg-[var(--color-surface-1)] fade-in max-h-[400px] overflow-y-auto">
                    <MdViewer content={r.content} maxLen={99999} />
                  </div>
                )}
              </div>
            ))}
          </div>
        )}
      </div>
    </div>
  );
}