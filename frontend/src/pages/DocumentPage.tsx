import { useState, useEffect, useMemo } from "react";
import { Upload, FolderTree, FileText, Clock, ChevronRight, Folder, File, RefreshCw, Loader2 } from "lucide-react";
import { api, type DocumentItem, type ChunkItem } from "../lib/api";
import { scanDirectory, batchUploadFiles } from "../lib/directoryScan";
import { MdViewer } from "../lib/markdown";
import { useAuth } from "../lib/mockAuth";

type Tab = "repository" | "upload";

interface TreeNode {
  name: string;
  children: TreeNode[];
  docs: DocumentItem[];
}

/** Recursive tree node renderer */
function RenderTreeNode({ node, depth, selectedDocId, onSelect, expanded, toggle }: {
  node: TreeNode; depth: number; selectedDocId?: string;
  onSelect: (d: DocumentItem) => void; expanded: Set<string>;
  toggle: (name: string) => void;
}) {
  const isOpen = expanded.has(node.name) || node.name === "";
  const indent = depth * 16;
  return (
    <div>
      {node.name && (
        <button onClick={() => toggle(node.name)}
          className="w-full flex items-center gap-2 px-2.5 py-1.5 rounded-xl text-xs text-[var(--color-ink-soft)] hover:text-[var(--color-ink)] hover:bg-[var(--color-bg-input)] transition-colors"
          style={{ paddingLeft: 8 + indent }}>
          <ChevronRight size={12} className={`transition-transform flex-shrink-0 ${isOpen ? "rotate-90" : ""}`} />
          <Folder size={13} className="text-[var(--color-accent)] flex-shrink-0" />
          <span className="font-medium truncate">{node.name}</span>
        </button>
      )}
      {isOpen && (
        <div className={node.name ? "ml-3 border-l border-[var(--color-line)] pl-2 space-y-0.5 mt-0.5" : "space-y-0.5"}>
          {node.docs.map((d) => (
            <button key={d.id} onClick={() => onSelect(d)}
              className={`w-full flex items-center gap-2 px-2.5 py-1.5 rounded-xl text-xs transition-colors ${
                selectedDocId === d.id ? "bg-[var(--color-accent-soft)] text-[var(--color-accent)]"
                : "text-[var(--color-ink-soft)] hover:text-[var(--color-ink)] hover:bg-[var(--color-bg-input)]"}`}
              style={{ paddingLeft: 8 + indent + (node.name ? 8 : 0) }}>
              <File size={12} className="flex-shrink-0" /> <span className="truncate">{d.fileName}</span>
            </button>
          ))}
          {node.children.map((child) => (
            <RenderTreeNode key={child.name} node={child} depth={depth + 1}
              selectedDocId={selectedDocId} onSelect={onSelect} expanded={expanded} toggle={toggle} />
          ))}
        </div>
      )}
    </div>
  );
}

function buildTree(docs: DocumentItem[]): TreeNode[] {
  const root: TreeNode[] = [];
  for (const doc of docs) {
    const path = doc.directoryPath || "";
    const parts = path ? path.split("/") : [];
    let current = root;
    for (let i = 0; i < parts.length; i++) {
      let node = current.find((n) => n.name === parts[i]);
      if (!node) {
        node = { name: parts[i], children: [], docs: [] };
        current.push(node);
      }
      if (i === parts.length - 1) {
        node.docs.push(doc);
      } else {
        current = node.children;
      }
    }
    if (parts.length === 0) {
      root.push({ name: "", children: [], docs: [doc] });
    }
  }
  return root;
}

export default function DocumentPage() {
  const { user } = useAuth();
  const isAdmin = user?.role === "ADMIN";
  const [tab, setTab] = useState<Tab>("repository");
  const [docs, setDocs] = useState<DocumentItem[]>([]);
  const [selectedDoc, setSelectedDoc] = useState<DocumentItem | null>(null);
  const [chunks, setChunks] = useState<ChunkItem[]>([]);
  const [expandedChunk, setExpandedChunk] = useState<number | null>(null);
  const [expandedFolders, setExpandedFolders] = useState<Set<string>>(new Set());
  const [uploading, setUploading] = useState(false);
  const [uploadMsg, setUploadMsg] = useState("");
  const [selectedIds, setSelectedIds] = useState<Set<string>>(new Set());
  const [deleteConfirm, setDeleteConfirm] = useState(false);
  const [deleteMode, setDeleteMode] = useState(false);

  const loadDocs = () => {
    api.listDocuments().then(setDocs).catch(() => {});
  };
  const loadChunks = (id: string) => {
    api.getChunks(id).then(setChunks).catch(() => {});
  };

  useEffect(() => { loadDocs(); }, []);

  const handleDelete = async () => {
    for (const id of selectedIds) {
      try { await api.deleteDocument(id); } catch {}
    }
    setSelectedIds(new Set());
    setDeleteConfirm(false);
    setDeleteMode(false);
    loadDocs();
    setSelectedDoc(null);
  };

  const toggleSelect = (id: string) => {
    setSelectedIds((prev) => {
      const next = new Set(prev);
      next.has(id) ? next.delete(id) : next.add(id);
      return next;
    });
  };

  const tree = useMemo(() => buildTree(docs), [docs]);

  /* ---- Directory scan ----- */
  const handleScanFolder = async () => {
    try {
      setUploading(true);
      setUploadMsg("扫描目录中...");
      const files = await scanDirectory();
      setUploadMsg(`找到 ${files.length} 个 .md 文件，开始批量上传...`);
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

  /* ---- Single file drop ----- */
  const handleDrop = async (e: React.DragEvent) => {
    e.preventDefault();
    const f = e.dataTransfer.files[0];
    if (!f || !f.name.endsWith(".md")) return;
    try {
      setUploading(true);
      setUploadMsg(`上传中... ${f.name}`);
      await api.upload(f, "");
      setUploadMsg("上传完成");
      loadDocs();
    } catch (err: any) {
      setUploadMsg(err.message);
    } finally {
      setTimeout(() => setUploadMsg(""), 3000);
      setUploading(false);
    }
  };

  return (
    <div className="h-full flex min-h-[600px]">
      {/* Left sidebar */}
      <div className="w-[340px] flex-shrink-0 border-r border-[var(--color-line)] flex flex-col max-h-[calc(100vh-160px)]">
        <div className="flex gap-1 p-3 border-b border-[var(--color-line)]">
          {([
            { key: "repository", icon: FolderTree, label: "资料库" },
            ...(isAdmin ? [{ key: "upload" as const, icon: Upload, label: "上传管理" }] : []),
          ]).map(({ key, icon: Icon, label }) => (
            <button key={key} onClick={() => setTab(key as Tab)}
              className={`flex items-center gap-2 px-4 py-2 rounded-full text-xs font-medium transition-all ${
                tab === key ? "bg-[var(--color-pill-dark)] text-[var(--color-pill-text)] shadow-[var(--shadow-btn)]"
                : "text-[var(--color-ink-soft)] hover:text-[var(--color-ink)]"}`}>
              <Icon size={14} />{label}
            </button>
          ))}
        </div>

        {tab === "repository" ? (
          <div className="flex-1 min-h-0 overflow-y-auto p-3 space-y-0">
            <div className="flex items-center gap-2 mb-2">
              <button onClick={loadDocs} className="text-[10px] text-[var(--color-ink-faint)] hover:text-[var(--color-accent)] flex items-center gap-1">
                <RefreshCw size={10} /> 刷新
              </button>
              <button onClick={async () => {
                try {
                  const count = await api.vectorHealth();
                  if (count > 0) alert(`向量数据正常，共 ${count} 条`);
                  else if (confirm("向量数据异常，是否重新执行 Embedding？"))
                    await api.reEmbedAll();
                } catch { alert("检查失败"); }
              }} className="text-[10px] text-[var(--color-ink-faint)] hover:text-[var(--color-accent)]">
                向量检查
              </button>
            </div>
            {tree.length === 0 ? (
              <div>
                <p className="text-xs text-[var(--color-ink-faint)] px-2">暂无文档</p>
                {!isAdmin && (
                  <p className="text-[11px] text-[var(--color-ink-faint)] px-2 mt-3 opacity-60">
                    需要上传权限？联系管理员 admin@qa-agent.com
                  </p>
                )}
              </div>
            ) : (
              tree.map((node) => (
                <RenderTreeNode key={node.docs[0]?.id || node.name || `root-${Math.random()}`} node={node} depth={0}
                  selectedDocId={selectedDoc?.id} onSelect={(d) => { setSelectedDoc(d); loadChunks(d.id); }}
                  expanded={expandedFolders} toggle={(name) => setExpandedFolders(p => {
                    const n = new Set(p); n.has(name) ? n.delete(name) : n.add(name); return n;
                  })} />
              ))
            )}
            {!isAdmin && tree.length > 0 && (
              <p className="text-[11px] text-[var(--color-ink-faint)] px-2 mt-3 opacity-60">
                需要上传权限？联系管理员 admin@qa-agent.com
              </p>
            )}
          </div>
        ) : (
          <div className="flex-1 flex flex-col overflow-hidden">
            <div className="p-3 border-b border-[var(--color-line)] space-y-2">
              <button onClick={handleScanFolder} disabled={uploading}
                className="w-full py-3 rounded-2xl border-2 border-dashed border-[var(--color-line-strong)] text-center cursor-pointer hover:border-[var(--color-accent-border)] hover:bg-[var(--color-accent-soft)] transition-all group">
                {uploading ? <Loader2 size={20} className="mx-auto mb-1 animate-spin text-[var(--color-accent)]" />
                : <Upload size={20} className="mx-auto mb-1 text-[var(--color-ink-faint)] group-hover:text-[var(--color-accent)] transition-colors" />}
                <p className="text-[11px] text-[var(--color-ink-soft)]">选择本地文件夹</p>
                <p className="text-[10px] text-[var(--color-ink-faint)] mt-0.5">递归扫描所有 .md 并上传</p>
              </button>
              <div
                onDrop={handleDrop} onDragOver={(e) => e.preventDefault()}
                className="py-2 rounded-2xl border border-[var(--color-line)] text-center cursor-pointer hover:border-[var(--color-line-strong)] transition-colors">
                <p className="text-[10px] text-[var(--color-ink-faint)]">或拖拽单个 .md 文件</p>
              </div>
              {uploadMsg && (
                <p className="text-[11px] text-[var(--color-accent)] text-center">{uploadMsg}</p>
              )}
            </div>
            <div className="flex-1 overflow-auto p-3 space-y-1">
              <div className="flex items-center justify-between px-1 mb-2">
                <span className="text-[10px] text-[var(--color-ink-faint)] uppercase tracking-wider">已上传 ({docs.length})</span>
                <div className="flex items-center gap-2">
                  <button onClick={() => { setDeleteMode(!deleteMode); setSelectedIds(new Set()); }}
                    className="text-[10px] text-[var(--color-ink-faint)] hover:text-[var(--color-accent)]">
                    {deleteMode ? "取消" : "批量删除"}</button>
                  {deleteMode && (
                    <label className="flex items-center gap-1 text-[10px] text-[var(--color-ink-faint)] cursor-pointer">
                      <input type="checkbox" className="w-3 h-3 accent-[var(--color-accent)]"
                        checked={selectedIds.size === docs.length && docs.length > 0}
                        onChange={() => {
                          if (selectedIds.size === docs.length) setSelectedIds(new Set());
                          else setSelectedIds(new Set(docs.map(d => d.id)));
                        }} />
                      全选
                    </label>
                  )}
                  {selectedIds.size > 0 && (
                    <button onClick={() => setDeleteConfirm(true)}
                      className="text-[10px] text-[var(--color-danger)] hover:underline">删除 ({selectedIds.size})</button>
                  )}
                  <button onClick={loadDocs} className="text-[10px] text-[var(--color-ink-faint)] hover:text-[var(--color-accent)]">
                    <RefreshCw size={10} />
                  </button>
                </div>
              </div>
              {docs.map((d) => (
                <div key={d.id} className="flex items-center gap-1">
                  {deleteMode && (
                    <input type="checkbox" checked={selectedIds.has(d.id)}
                      onChange={() => toggleSelect(d.id)}
                      className="w-3.5 h-3.5 accent-[var(--color-accent)] flex-shrink-0" />
                  )}
                  <button onClick={() => { setSelectedDoc(d); loadChunks(d.id); }}
                    className={`flex-1 text-left p-3 rounded-2xl transition-all border ${
                      selectedDoc?.id === d.id ? "border-[var(--color-accent-border)] bg-[var(--color-accent-soft)]"
                      : "border-transparent hover:border-[var(--color-line-strong)] hover:bg-[var(--color-bg-card)]"}`}>
                    <div className="flex items-center gap-2">
                      <FileText size={14} className={selectedDoc?.id === d.id ? "text-[var(--color-accent)]" : "text-[var(--color-ink-faint)]"} />
                      <span className="text-xs font-medium truncate">{d.fileName}</span>
                    </div>
                    <div className="flex items-center gap-3 mt-1 text-[10px] text-[var(--color-ink-faint)]">
                      <span className="flex items-center gap-1"><Clock size={9} /> {d.createdAt?.slice(0, 16)}</span>
                      {d.directoryPath && <span className="flex items-center gap-1"><Folder size={9} /> {d.directoryPath}</span>}
                    </div>
                  </button>
                </div>
              ))}
            </div>
            {deleteConfirm && (
              <div className="p-3 border-t border-[var(--color-line)]">
                <p className="text-xs text-[var(--color-ink-soft)] mb-2">
                  将删除选中的 {selectedIds.size} 个文档及其分块、向量数据，不可恢复。确认？
                </p>
                <div className="flex gap-2">
                  <button onClick={handleDelete}
                    className="pill text-[11px] px-3 py-1.5 bg-[var(--color-danger)] text-white">确认删除</button>
                  <button onClick={() => { setDeleteConfirm(false); setSelectedIds(new Set()); }}
                    className="pill pill--ghost text-[11px] px-3 py-1.5">取消</button>
                </div>
              </div>
            )}
          </div>
        )}
      </div>

      {/* Right panel */}
      <div className="flex-1 min-h-0 overflow-y-auto p-8 pr-4 max-h-[calc(100vh-160px)]">
        {selectedDoc ? (
          <div>
            <h3 className="serif text-2xl font-semibold mb-1">{selectedDoc.fileName}</h3>
            <div className="flex items-center gap-3 text-xs text-[var(--color-ink-faint)] mb-8">
              <span>{selectedDoc.fileType}</span><span>·</span>
              <span>ID: {selectedDoc.id?.slice(0, 8)}</span><span>·</span>
              <span>{chunks.length} 个分块</span>
              {selectedDoc.directoryPath && <><span>·</span><span>{selectedDoc.directoryPath}</span></>}
            </div>

            {tab === "repository" ? (
              <MdViewer content={selectedDoc.rawContent || ""} />
            ) : (
              <div>
                <p className="text-[11px] text-[var(--color-ink-faint)] uppercase tracking-wider mb-4">分块列表 ({chunks.length})</p>
                <div className="space-y-2">
                  {chunks.map((c, i) => (
                    <div key={c.id}>
                      <button onClick={() => setExpandedChunk(expandedChunk === i ? null : i)}
                        className={`w-full text-left p-4 rounded-2xl border transition-all ${
                          expandedChunk === i ? "border-[var(--color-accent-border)] bg-[var(--color-bg-card)]"
                          : "border-[var(--color-line)] bg-[var(--color-bg-card)] hover:border-[var(--color-line-strong)]"}`}>
                        <div className="flex items-center gap-2 mb-2">
                          <span className="text-[10px] text-[var(--color-ink-faint)] font-mono">#{c.chunkIndex}</span>
                          <p className="text-xs text-[var(--color-accent)] font-mono truncate">{c.titlePath || "(无标题)"}</p>
                          <span className="ml-auto text-[10px] text-[var(--color-ink-faint)]">{expandedChunk === i ? "收起 ▲" : "展开 ▼"}</span>
                        </div>
                        <p className={`serif text-sm leading-relaxed text-[var(--color-ink-soft)] ${expandedChunk === i ? "" : "line-clamp-2"}`}>
                          {expandedChunk !== i ? c.content?.slice(0, 150) + "..." : ""}
                        </p>
                        <div className="flex items-center gap-1.5 mt-2">
                          {(c.moduleTags || []).map((tag: string) => (
                            <span key={tag} className="text-[10px] px-2 py-0.5 rounded-full bg-[var(--color-accent-soft)] text-[var(--color-accent)]">{tag}</span>
                          ))}
                        </div>
                      </button>
                      {expandedChunk === i && (
                        <div className="mt-2 p-5 rounded-2xl border border-[var(--color-line)] bg-[var(--color-surface-1)] fade-in">
                          <MdViewer content={c.content} />
                        </div>
                      )}
                    </div>
                  ))}
                </div>
              </div>
            )}
          </div>
        ) : (
          <div className="flex flex-col items-center justify-center h-full text-[var(--color-ink-faint)]">
            <File size={36} className="mb-3 opacity-25" />
            <p className="text-sm">{tab === "repository" ? "从左侧目录树选择一篇笔记" : "从左侧列表选择一个已上传文档"}</p>
          </div>
        )}
      </div>
    </div>
  );
}