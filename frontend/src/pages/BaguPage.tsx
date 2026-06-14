import { useState, useEffect } from "react";
import { api, type BaguSetResponse, type BaguItemResponse } from "../lib/api";
import { MdViewer } from "../lib/markdown";

const ICON_CALC = "M5 2h14v2H5zm0 18h14v2H5zM3 4h2v16H3zm16 0h2v16h-2zM7 6h10v4H7zm0 6h2v2H7zm4 0h2v2h-2zm4 0h2v2h-2zm-8 4h2v2H7zm4 0h2v2h-2zm4 0h2v2h-2z";

type StatusMap = Record<string, { level: string; comment: string }>;

export default function BaguPage() {
  const [sets, setSets] = useState<BaguSetResponse[]>([]);
  const [selectedSet, setSelectedSet] = useState<BaguSetResponse | null>(null);
  const [activeItemIdx, setActiveItemIdx] = useState(0);
  const [userAnswer, setUserAnswer] = useState("");
  const [evaluating, setEvaluating] = useState(false);
  const [statuses, setStatuses] = useState<StatusMap>(() => {
    try { return JSON.parse(localStorage.getItem("bagu_statuses") || "{}"); } catch { return {}; }
  });
  const [showStandard, setShowStandard] = useState<Record<string, boolean>>({});

  useEffect(() => { api.baguListSets().then(setSets).catch(() => {}); }, []);

  const handleSelectSet = async (set: BaguSetResponse) => {
    const full = await api.baguGetSet(set.id);
    setSelectedSet(full);
    setActiveItemIdx(0);
    setUserAnswer("");
  };

  const handleEvaluate = async () => {
    if (!selectedSet || !userAnswer.trim()) return;
    const item = selectedSet.items?.[activeItemIdx];
    if (!item) return;
    setEvaluating(true);
    try {
      const raw = await api.baguEvaluate(item.question, item.answer, userAnswer);
      const parsed = JSON.parse(raw);
      const newStatus = { level: parsed.level || "OK", comment: parsed.comment || "" };
      setStatuses(s => {
        const updated = { ...s, [item.id]: newStatus };
        localStorage.setItem("bagu_statuses", JSON.stringify(updated));
        return updated;
      });
      setShowStandard(s => ({ ...s, [item.id]: true }));
    } catch {
      setStatuses(s => ({ ...s, [item.id]: { level: "OK", comment: "评价失败" } }));
    } finally { setEvaluating(false); }
  };

  const getLevelColor = (level: string) => {
    if (level === "GOOD") return "#7ea88b";
    if (level === "POOR") return "#c87a6a";
    return "#c9a050"; // OK
  };

  const activeItem: BaguItemResponse | undefined = selectedSet?.items?.[activeItemIdx];

  return (
    <div className="h-full flex gap-4 items-stretch" style={{ zIndex: 2, position: "relative", padding: "4px 0" }}>
      {/* 左侧题目集列表 */}
      <div className="shelf-panel">
        <div className="shelf-tabs">
          <div className="shelf-tab active" style={{ flex: 1 }}>
            <svg viewBox="0 0 24 24" style={{ width: 13, height: 13, fill: "currentColor" }}><path d={ICON_CALC} /></svg>
            题目集
          </div>
        </div>
        <div className="shelf-cab" style={{ flex: 1 }}>
          <div className="shelf-cab-top" />
          <div className="shelf-cab-inner" style={{ overflow: "visible" }}>
            <div className="shelf-cab-scroll">
              {sets.length === 0 ? (
                <div style={{ fontFamily: "var(--font-mc)", fontSize: 11, color: "#8a6a4a", textAlign: "center", padding: 30 }}>
                  暂无题目集<br/><span style={{ fontSize: 9 }}>在知识库中生成八股吧</span>
                </div>
              ) : (
                sets.map((set) => (
                  <div key={set.id}
                    onClick={() => handleSelectSet(set)}
                    style={{
                      fontFamily: "var(--font-mc)", fontSize: 11, color: selectedSet?.id === set.id ? "#f5e050" : "#c8b090",
                      padding: "10px 12px", cursor: "pointer", marginBottom: 4,
                      background: selectedSet?.id === set.id ? "rgba(200,160,80,0.1)" : "transparent",
                      border: selectedSet?.id === set.id ? "2px solid #6b5020" : "2px solid transparent",
                    }}>
                    <div style={{ display: "flex", justifyContent: "space-between", alignItems: "center" }}>
                      <span style={{ fontSize: 12 }}>{set.title}</span>
                      <span onClick={(e) => { e.stopPropagation(); if (confirm("删除此题目集？")) { api.baguDeleteSet(set.id).then(() => { setSets(s => s.filter(x => x.id !== set.id)); if (selectedSet?.id === set.id) setSelectedSet(null); }).catch(() => {}); } }}
                        style={{ fontSize: 12, color: "#6b4020", cursor: "pointer", padding: "0 4px" }}>✕</span>
                    </div>
                    <div style={{ fontSize: 9, color: "#8a6a4a", marginTop: 2 }}>{set.description} · {set.itemCount}题</div>
                  </div>
                ))
              )}
            </div>
          </div>
        </div>
      </div>

      {/* 右侧附魔书 */}
      <div className="grim">
        <div className="grim-cover">
          <div className="grim-metal grim-m1" /><div className="grim-metal grim-m2" />
          <div className="grim-metal grim-m3" /><div className="grim-metal grim-m4" />
          <div className="grim-parch">
            <div className="grim-hdr">
              {selectedSet ? selectedSet.title : "选择一个题目集"}
            </div>
            <div className="grim-body">
              <div className="grim-side">
                {selectedSet?.items?.map((item, i) => {
                  const s = statuses[item.id];
                  const color = s ? getLevelColor(s.level) : "#6b6560";
                  return (
                    <div key={item.id}
                      onClick={() => { setActiveItemIdx(i); setUserAnswer(""); }}
                      className={`doc-item ${activeItemIdx === i ? "sel" : ""}`}
                      style={{ gap: 6 }}>
                      <span style={{ width: 10, height: 10, borderRadius: "50%", background: color, flexShrink: 0, display: "inline-block" }} />
                      <span style={{ flex: 1 }}>Q{i + 1}. {item.question.slice(0, 20)}...</span>
                    </div>
                  );
                })}
                {!selectedSet && (
                  <div className="doc-item" style={{ color: "#8b7355", cursor: "default" }}>← 选择题目集</div>
                )}
              </div>
              <div className="grim-main">
                {activeItem ? (
                  <div>
                    <div style={{ fontFamily: "var(--font-mc)", fontSize: 12, color: "#1a0804", marginBottom: 8, fontWeight: "bold" }}>
                      Q{activeItemIdx + 1}. {activeItem.question}
                      <span style={{ fontSize: 8, color: "#8b7355", marginLeft: 8, background: "rgba(139,115,85,0.1)", padding: "1px 6px" }}>{activeItem.difficulty}</span>
                    </div>

                    {/* 答题区 */}
                    <textarea
                      value={userAnswer}
                      onChange={(e) => setUserAnswer(e.target.value)}
                      placeholder="输入你的回答..."
                      disabled={evaluating}
                      rows={5}
                      style={{
                        width: "100%", padding: 10, fontFamily: "var(--font-mc)", fontSize: 11, color: "#e8dcc8",
                        background: "rgba(20,14,8,0.8)", border: "2px solid #5a3018", outline: "none", resize: "vertical"
                      }}
                    />
                    <button onClick={handleEvaluate} disabled={evaluating || !userAnswer.trim()}
                      className="mc-btn" style={{ marginTop: 8, padding: "8px 20px", fontSize: 11 }}>
                      {evaluating ? "评判中..." : "提交评判"}
                    </button>

                    {/* 评价结果 */}
                    {statuses[activeItem.id] && (
                      <div style={{ marginTop: 12, padding: 10, background: "rgba(139,115,85,0.08)", border: "1px solid rgba(139,115,85,0.15)" }}>
                        <p style={{ fontFamily: "var(--font-mc)", fontSize: 11, color: getLevelColor(statuses[activeItem.id].level), margin: "0 0 4px" }}>
                          {statuses[activeItem.id].level === "GOOD" ? "✓ 回答准确" : statuses[activeItem.id].level === "POOR" ? "✗ 需要改进" : "△ 大体正确"}
                          {" · "}{statuses[activeItem.id].comment}
                        </p>
                      </div>
                    )}

                    {/* 标准答案 */}
                    {showStandard[activeItem.id] && (
                      <div style={{ marginTop: 12, padding: 10, background: "rgba(126,168,139,0.06)", border: "1px solid rgba(126,168,139,0.2)" }}>
                        <p style={{ fontFamily: "var(--font-mc)", fontSize: 10, color: "#7ea88b", margin: "0 0 6px" }}>📖 标准答案</p>
                        <MdViewer content={activeItem.answer.replace(/\\n/g, "\n")} />
                      </div>
                    )}

                    {/* 导航 */}
                    <div style={{ display: "flex", gap: 8, marginTop: 16, justifyContent: "space-between" }}>
                      <button onClick={() => { setActiveItemIdx(Math.max(0, activeItemIdx - 1)); setUserAnswer(""); }}
                        disabled={activeItemIdx === 0}
                        className="mc-menu-vert" style={{ width: "auto", fontSize: 10, padding: "6px 14px", justifyContent: "center", borderBottom: "2px solid #6b5020" }}>
                        ← 上一题
                      </button>
                      <button onClick={() => { setActiveItemIdx(Math.min((selectedSet?.items?.length || 1) - 1, activeItemIdx + 1)); setUserAnswer(""); }}
                        disabled={activeItemIdx >= (selectedSet?.items?.length || 1) - 1}
                        className="mc-menu-vert" style={{ width: "auto", fontSize: 10, padding: "6px 14px", justifyContent: "center", borderBottom: "2px solid #6b5020" }}>
                        下一题 →
                      </button>
                    </div>
                  </div>
                ) : (
                  <div style={{ display: "flex", alignItems: "center", justifyContent: "center", height: "100%", color: "#8b7355", fontFamily: "var(--font-mc)", fontSize: 10 }}>
                    {selectedSet ? "从左侧选择一道题目" : "← 选择题目集"}
                  </div>
                )}
              </div>
            </div>
          </div>
        </div>
      </div>
    </div>
  );
}
