import { useState, useEffect, useMemo } from "react";
import { api, type DocumentItem, type ChunkItem } from "../lib/api";
import { scanFiles, batchUploadFiles } from "../lib/directoryScan";
import { MdViewer } from "../lib/markdown";
import { useAuth } from "../lib/mockAuth";

/** Pixelarticons SVG paths */
const ICON_BOOK_OPEN = "M2 3h9v2H2zM0 19h11v2H0zM13 3h9v2h-9zm0 16h11v2H13zM11 5h2v18h-2zM0 5h2v14H0zm22 0h2v14h-2zm-7 2h5v2h-5zm0 4h5v2h-5zm0 4h2v2h-2z";
const ICON_LIST_BOX = "M4 2h16v2H4zm2 5h2v2H6zm4 0h8v2h-8zm-4 4h2v2H6zm4 0h8v2h-8zm-4 4h2v2H6zm4 0h8v2h-8zm-6 5h16v2H4zM2 4h2v16H2zm18 0h2v16h-2z";
const ICON_MESSAGE = "M2 3h20v2H2zm0 4h14v2H2zm0 4h10v2H2zm0 4h16v2H2zm0 4h12v2H2z";

const BOOK_COLORS = ["#c83838", "#3858b8", "#389050", "#784828", "#683878", "#c89838", "#388888", "#c87038"];

const TABS = [
  { key: "knowledge", icon: ICON_BOOK_OPEN, label: "知识库" },
  { key: "qasets", icon: ICON_LIST_BOX, label: "题目集" },
  { key: "chat", icon: ICON_MESSAGE, label: "对话" },
];

/** 从 directoryPath 提取根目录名 */
function rootDir(doc: DocumentItem): string {
  const p = doc.directoryPath || "";
  const idx = p.indexOf("/");
  return idx > 0 ? p.slice(0, idx) : (p || "其他");
}

export default function DocumentPage() {
  const { user } = useAuth();
  const isAdmin = user?.role === "ADMIN";
  const [tab, setTab] = useState("knowledge");
  const [docs, setDocs] = useState<DocumentItem[]>([]);
  const [chunks, setChunks] = useState<ChunkItem[]>([]);
  const [selectedDoc, setSelectedDoc] = useState<DocumentItem | null>(null);
  const [selectedShelf, setSelectedShelf] = useState<string | null>(null);
  const [deleteMode, setDeleteMode] = useState(false);
  const [selectedShelves, setSelectedShelves] = useState<Set<string>>(new Set());
  const [deleteConfirm, setDeleteConfirm] = useState(false);
  const [uploading, setUploading] = useState(false);
  const [uploadMsg, setUploadMsg] = useState("");
  const [generating, setGenerating] = useState(false);
  const [genDone, setGenDone] = useState(false);
  const [genError, setGenError] = useState("");
  const [genResult, setGenResult] = useState<import("../lib/api").BaguSetResponse | null>(null);
  const [qaSets, setQaSets] = useState<import("../lib/api").BaguSetResponse[]>([]);
  const [selectedSetId, setSelectedSetId] = useState<string | null>(null);

  const loadDocs = () => { api.listDocuments().then(setDocs).catch(() => {}); };
  const loadChunks = (id: string) => { api.getChunks(id).then(setChunks).catch(() => {}); };

  useEffect(() => { loadDocs(); api.baguListSets().then(setQaSets).catch(() => {}); }, []);

  /* 文档按根目录分组 */
  const shelves = useMemo(() => {
    const map = new Map<string, DocumentItem[]>();
    for (const d of docs) {
      const key = rootDir(d);
      if (!map.has(key)) map.set(key, []);
      map.get(key)!.push(d);
    }
    return Array.from(map.entries()).sort(([a], [b]) => {
      if (a === "其他") return 1;
      if (b === "其他") return -1;
      return a.localeCompare(b);
    });
  }, [docs]);

  /* 选中书架下的文档 */
  const shelfDocs = useMemo(() => {
    if (!selectedShelf) return [];
    return docs.filter((d) => rootDir(d) === selectedShelf);
  }, [docs, selectedShelf]);

  /* 处理书架点击 */
  const handleShelfClick = (name: string) => {
    if (deleteMode) {
      setSelectedShelves((prev) => {
        const next = new Set(prev);
        next.has(name) ? next.delete(name) : next.add(name);
        return next;
      });
    } else {
      setSelectedShelf(name);
      setSelectedDoc(null);
      setChunks([]);
    }
  };

  /* 处理文档点击 */
  const handleDocClick = (d: DocumentItem) => {
    setSelectedDoc(d);
    loadChunks(d.id);
  };

  /* 删除选中书架 */
  const handleDelete = async () => {
    const idsToDelete: string[] = [];
    for (const name of selectedShelves) {
      docs.filter((d) => rootDir(d) === name).forEach((d) => idsToDelete.push(d.id));
    }
    for (const id of idsToDelete) {
      try { await api.deleteDocument(id); } catch {}
    }
    setSelectedShelves(new Set());
    setDeleteConfirm(false);
    setDeleteMode(false);
    if (selectedShelf && selectedShelves.has(selectedShelf)) {
      setSelectedShelf(null);
      setSelectedDoc(null);
      setChunks([]);
    }
    loadDocs();
  };

  /* 生成八股 */
  const handleBaguGenerate = async () => {
    if (!selectedShelf) return;
    const ids = shelfDocs.map(d => d.id);
    if (ids.length === 0) return;
    setGenerating(true); setGenDone(false); setGenError(""); setGenResult(null);
    try {
      const result = await api.baguGenerate(selectedShelf, ids);
      setGenResult(result); setGenDone(true);
      api.baguListSets().then(setQaSets).catch(() => {});
    } catch (e: any) {
      setGenError(e.message || "未知错误");
    } finally {
      setGenerating(false);
    }
  };

  /* 上传 */
  const handleScanFolder = async () => {
    try {
      setUploading(true);
      setUploadMsg("扫描目录中...");
      const files = await scanFiles();
      if (files.length === 0) { setUploadMsg("未选择任何 .md 文件"); return; }
      setUploadMsg(`已选择 ${files.length} 个 .md 文件，开始批量上传...`);
      const ok = await batchUploadFiles(files, (file, dirPath) => api.upload(file, dirPath),
        (cur, total, name) => setUploadMsg(`上传中 (${cur}/${total}) ${name}`));
      setUploadMsg(`完成！成功 ${ok}/${files.length} 个文件`);
      loadDocs();
    } catch (e: any) {
      if (e.name === "AbortError") return;
      setUploadMsg(`错误: ${e.message || e}`);
    } finally {
      setTimeout(() => setUploadMsg(""), 4000);
      setUploading(false);
    }
  };

  /* 书架方块内书本渲染（确定性——基于书架名hash，不随render变化） */
  const renderShelfBooks = (name: string, count: number) => {
    const filled = Math.min(6, Math.ceil(count / 2));
    const slots = [];
    // 用书架名的简单hash生成稳定伪随机数
    const hash = (s: string, seed: number) => {
      let h = seed;
      for (let i = 0; i < s.length; i++) h = ((h << 5) - h + s.charCodeAt(i)) | 0;
      return Math.abs(h);
    };
    for (let i = 0; i < 6; i++) {
      if (i < filled) {
        const h = 60 + (hash(name, i * 7 + 3) % 40);
        const ci = hash(name, i * 13 + 5) % BOOK_COLORS.length;
        slots.push(
          <div key={i} className="cs-slot">
            <span className="cs-book" style={{ height: h + "%", background: BOOK_COLORS[ci] }} />
          </div>
        );
      } else {
        slots.push(<div key={i} className="cs-slot empty"><span className="cs-book" /></div>);
      }
    }
    return slots;
  };

  return (
    <div className="h-full flex gap-4 items-stretch" style={{ zIndex: 2, position: "relative", padding: "4px 0" }}>
      {/* 左侧书架面板 */}
      <div className="shelf-panel">
        {/* 分页标签 */}
        <div className="shelf-tabs">
          {TABS.map(({ key, icon, label }) => (
            <div key={key} className={`shelf-tab ${tab === key ? "active" : ""}`} onClick={() => setTab(key)}>
              <svg viewBox="0 0 24 24"><path d={icon} /></svg>
              {label}
            </div>
          ))}
        </div>
        {/* 书架主体 */}
        <div className="shelf-cab">
          <div className="shelf-cab-top" />
          <div className="shelf-toolbar">
            {isAdmin && (<>
              {deleteMode ? (
                <>
                  <span style={{ color: "#c84040" }}>选择要删除的资料库</span>
                  <span onClick={() => { setDeleteMode(false); setSelectedShelves(new Set()); }}>取消</span>
                  {selectedShelves.size > 0 && (
                    <span onClick={() => setDeleteConfirm(true)} style={{ color: "#f5e050", marginLeft: "auto" }}>确认删除 ({selectedShelves.size})</span>
                  )}
                </>
              ) : (
                <>
                  <span onClick={() => { setDeleteMode(true); setSelectedShelves(new Set()); }}>🗑 删除</span>
                  <span onClick={async () => { try { const c = await api.vectorHealth(); alert(`向量数据: ${c} 条`); } catch { alert("检查失败"); } }}>🔍 向量检查</span>
                </>
              )}
            </>)}
          </div>
          <div className="shelf-cab-inner">
            {uploadMsg && (
              <p style={{ fontFamily: "var(--font-mc)", fontSize: 9, color: "#c8a050", textAlign: "center", marginBottom: 8 }}>{uploadMsg}</p>
            )}
            <div className="shelf-cab-scroll">
            <div className="shelf-grid">
              {tab === "qasets" ? (
                qaSets.length === 0 ? (
                  <div style={{ gridColumn: "1 / -1", display: "flex", alignItems: "center", justifyContent: "center", height: 200, fontFamily: "var(--font-mc)", fontSize: 12, color: "#8a6a4a" }}>
                    暂无题目集，在知识库中生成八股吧
                  </div>
                ) : (
                  qaSets.map((set) => (
                    <div key={set.id} className={`cs-block ${selectedSetId === set.id ? "sel" : ""}`}
                      onClick={() => { setSelectedSetId(set.id); api.baguGetSet(set.id).then(setGenResult).catch(() => {}); }}>
                      <span className="cs-tip" style={{ zIndex: 20 }}>{set.title} · {set.itemCount}题</span>
                      <div style={{ fontFamily: "var(--font-mc)", fontSize: 9, color: "#c8b090", textAlign: "center", padding: 8 }}>
                        📋<br/>{set.title.slice(0, 8)}
                      </div>
                      <span className="cs-lbl">{set.title}</span>
                      <span className="cs-cnt">{set.itemCount}</span>
                    </div>
                  ))
                )
              ) : tab !== "knowledge" ? (
                <div style={{ gridColumn: "1 / -1", display: "flex", alignItems: "center", justifyContent: "center", height: 200, fontFamily: "var(--font-mc)", fontSize: 12, color: "#8a6a4a" }}>
                  {"对话记录功能开发中..."}
                </div>
              ) : (
                shelves.map(([name, shelfDocs]) => (
                <div
                  key={name}
                  className={`cs-block ${!deleteMode && selectedShelf === name ? "sel" : ""} ${deleteMode && selectedShelves.has(name) ? "deleting" : ""}`}
                  onClick={() => handleShelfClick(name)}
                >
                  <span className="cs-tip" style={{ zIndex: 20 }}>{name} · {shelfDocs.length}文档</span>
                  <div className="cs-slots">{renderShelfBooks(name, shelfDocs.length)}</div>
                  <span className="cs-lbl">{name}</span>
                  <span className="cs-cnt">{shelfDocs.length}</span>
                </div>
              ))
              )}
              {tab === "knowledge" && isAdmin && (
                <div className="cs-add" onClick={handleScanFolder} title="选择上传的笔记目录或单个 md 文件">
                  <span className="cs-add-ic">{uploading ? "⏳" : "+"}</span>
                  <span className="cs-add-lbl">{uploading ? "上传中..." : "新建/上传"}</span>
                </div>
              )}
            </div>
            </div>
          </div>
        </div>
      </div>

      {/* 右侧附魔书 */}
      <div className="grim">
        <div className="grim-cover">
          <div className="grim-metal grim-m1" />
          <div className="grim-metal grim-m2" />
          <div className="grim-metal grim-m3" />
          <div className="grim-metal grim-m4" />
          <div className="grim-parch">
            <div className="grim-hdr">
              {selectedShelf ? `📚 ${selectedShelf} 资料库` : "选择一个资料库"}
            </div>
            <div className="grim-body">
              <div className="grim-side">
                <div className="grim-drag-hint">⋮⋮ 拖拽文档到书架方块可迁移</div>
                {shelfDocs.map((d) => (
                  <div
                    key={d.id}
                    className={`doc-item ${selectedDoc?.id === d.id ? "sel" : ""}`}
                    onClick={() => handleDocClick(d)}
                    draggable
                    onDragStart={(e) => e.dataTransfer.setData("text/plain", d.id)}
                  >
                    ⋮⋮ 📄 {d.fileName.split("/").pop()?.replace(/\.md$/i, "") || d.fileName}
                  </div>
                ))}
                {shelfDocs.length === 0 && selectedShelf && (
                  <div className="doc-item" style={{ color: "#8b7355", cursor: "default" }}>暂无文档</div>
                )}
                {!selectedShelf && (
                  <div className="doc-item" style={{ color: "#8b7355", cursor: "default" }}>点击左侧书架方块</div>
                )}
                {selectedShelf && shelfDocs.length > 0 && (
                  <div style={{ marginTop: "auto", paddingTop: 10, borderTop: "1px solid rgba(139,115,85,0.2)" }}>
                    <button onClick={handleBaguGenerate} disabled={generating}
                      className="mc-btn" style={{ width: "100%", padding: "8px 12px", fontSize: 11 }}>
                      {generating ? "生成中..." : "⚒️ 生成八股"}
                    </button>
                  </div>
                )}

                {/* 生成弹窗 */}
                {(generating || genDone || genError) && (
                  <div style={{position:"fixed",inset:0,zIndex:100,background:"rgba(0,0,0,0.6)",display:"flex",alignItems:"center",justifyContent:"center"}} onClick={() => { if (genDone || genError) { setGenDone(false); setGenError(""); } }}>
                    <div onClick={e => e.stopPropagation()} style={{background:"#1a1410",border:"3px solid #5a3018",padding:"30px 36px",maxWidth:380,width:"90%",fontFamily:"var(--font-mc)"}}>
                      <div style={{display:"flex",justifyContent:"space-between",alignItems:"center",marginBottom:20}}>
                        <span style={{fontSize:14,color:"#e8dcc8"}}>
                          {generating ? "⚒️ 正在生成问答集" : genError ? "❌ 生成失败" : "✅ 生成完成"}
                        </span>
                        <span onClick={() => { setGenDone(false); setGenError(""); }} style={{cursor:"pointer",color:"#8a7a5a",fontSize:18}}>✕</span>
                      </div>
                      {generating && (
                        <div style={{textAlign:"center",padding:"20px 0"}}>
                          <div style={{fontSize:28,marginBottom:12,animation:"pulse 1.5s ease-in-out infinite"}}>⚒️</div>
                          <p style={{fontSize:11,color:"#8a7a4a",margin:"0 0 4px"}}>正在根据学习资料生成面试题集...</p>
                          <p style={{fontSize:9,color:"#5a4020",margin:0}}>DeepSeek 推理中，请耐心等待</p>
                        </div>
                      )}
                      {genError && (
                        <div style={{textAlign:"center",padding:"10px 0"}}>
                          <p style={{fontSize:12,color:"#c87a6a",margin:"0 0 12px"}}>{genError}</p>
                          <button onClick={() => setGenError("")} className="mc-btn" style={{padding:"6px 16px",fontSize:10}}>关闭</button>
                        </div>
                      )}
                      {genDone && genResult && (
                        <div style={{textAlign:"center"}}>
                          <p style={{fontSize:12,color:"#c8b090",margin:"0 0 8px"}}>{genResult.title}</p>
                          <p style={{fontSize:10,color:"#8a6a4a",margin:"0 0 16px"}}>{genResult.itemCount} 题 · {genResult.description}</p>
                          <button onClick={() => { setTab("qasets"); setGenDone(false); }} className="mc-btn" style={{padding:"8px 20px",fontSize:11}}>前往题目集 →</button>
                        </div>
                      )}
                    </div>
                  </div>
                )}
              </div>
              <div className="grim-main">
                {genResult ? (
                  <div>
                    <h4 style={{ fontFamily: "var(--font-mc)", fontSize: 14, color: "#1a0804", margin: "0 0 4px" }}>{genResult.title}</h4>
                    <p style={{ fontFamily: "var(--font-mc)", fontSize: 10, color: "#8b7355", margin: "0 0 12px" }}>{genResult.description} · {genResult.itemCount}题</p>
                    {genResult.items.map((item, i) => (
                      <div key={item.id || i} style={{ marginBottom: 16, padding: "10px 12px", background: "rgba(139,115,85,0.06)", border: "1px solid rgba(139,115,85,0.12)" }}>
                        <p style={{ fontFamily: "var(--font-mc)", fontSize: 11, color: "#1a0804", fontWeight: "bold", margin: "0 0 6px" }}>
                          Q{i + 1}. {item.question}
                          <span style={{ fontSize: 8, color: "#8b7355", marginLeft: 8, background: "rgba(139,115,85,0.1)", padding: "1px 6px" }}>{item.difficulty}</span>
                        </p>
                        <div style={{ fontFamily: "Georgia, serif", fontSize: 11, color: "#2a1808", lineHeight: 1.7 }}>
                          <MdViewer content={item.answer.replace(/\\n/g, "\n")} />
                        </div>
                      </div>
                    ))}
                    <button onClick={() => setGenResult(null)} style={{ fontFamily: "var(--font-mc)", fontSize: 9, color: "#8b7355", background: "none", border: "1px solid rgba(139,115,85,0.2)", padding: "4px 10px", cursor: "pointer" }}>← 返回文档</button>
                  </div>
                ) : selectedDoc ? (
                  <>
                    <div>
                      <span className="drop-cap">{selectedDoc.fileName.charAt(0).toUpperCase()}</span>
                      <strong>{selectedDoc.fileName}</strong>
                      <span className="source-tag">
                        📍 {selectedDoc.directoryPath || "根目录"} · {chunks.length} 分块 · {selectedDoc.createdAt?.slice(0, 10)}
                      </span>
                    </div>
                    <div style={{ marginTop: 12 }}>
                      <MdViewer content={selectedDoc.rawContent || ""} />
                    </div>
                  </>
                ) : (
                  <div style={{ display: "flex", alignItems: "center", justifyContent: "center", height: "100%", color: "#8b7355", fontFamily: "var(--font-mc)", fontSize: 10 }}>
                    {selectedShelf ? "从左侧选择一篇文档" : "← 选择资料库"}
                  </div>
                )}
              </div>
            </div>
          </div>
        </div>
      </div>

      {/* 删除确认弹窗 */}
      {deleteConfirm && (
        <div className="fixed inset-0 z-50 flex items-center justify-center" style={{ background: "rgba(0,0,0,0.5)" }} onClick={() => setDeleteConfirm(false)}>
          <div className="mc-modal" onClick={(e) => e.stopPropagation()}>
            <div className="mc-modal-header"><span>确认删除</span><button onClick={() => setDeleteConfirm(false)} className="mc-modal-close">✕</button></div>
            <div className="mc-modal-body">
              <p style={{ fontFamily: "var(--font-mc)", fontSize: 11, color: "#e8dcc8", margin: "0 0 12px" }}>
                将删除选中的 {selectedShelves.size} 个资料库及其所有文档、分块和向量数据，不可恢复。是否确认删除？
              </p>
              <div style={{ display: "flex", gap: 8 }}>
                <button onClick={handleDelete} className="mc-btn" style={{ padding: "8px 20px", fontSize: 12 }}>确认删除</button>
                <button onClick={() => setDeleteConfirm(false)} className="mc-menu-vert" style={{ width: "auto", justifyContent: "center", borderBottom: "2px solid #6b5020", fontSize: 12, padding: "8px 20px" }}>取消</button>
              </div>
            </div>
          </div>
        </div>
      )}
    </div>
  );
}
