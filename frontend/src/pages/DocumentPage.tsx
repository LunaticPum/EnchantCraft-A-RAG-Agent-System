import { useState } from "react";
import { Upload, FolderTree, FileText, Layers, Clock, Tag, ChevronRight, Folder, File, Hash, Eye } from "lucide-react";

type Tab = "repository" | "upload";

/* Mock document tree (like Obsidian hierarchy) */
const documentTree = [
  { name: "MySQL", type: "folder" as const, children: [
    { name: "存储引擎.md", id: "d1", chunks: 13 },
    { name: "索引优化.md", id: "d2", chunks: 8 },
    { name: "事务与锁.md", id: "d3", chunks: 11 },
  ]},
  { name: "Redis", type: "folder" as const, children: [
    { name: "数据类型.md", id: "d4", chunks: 9 },
    { name: "持久化.md", id: "d5", chunks: 6 },
  ]},
  { name: "JVM", type: "folder" as const, children: [
    { name: "内存模型.md", id: "d6", chunks: 18 },
  ]},
];

type FlatDoc = { id: string; name: string; chunks: number; created: string };

const uploadedDocs: FlatDoc[] = [
  { id: "d1", name: "MySQL 存储引擎.md", chunks: 13, created: "2026-06-12 14:00" },
  { id: "d4", name: "Redis 数据类型.md", chunks: 9, created: "2026-06-12 10:30" },
  { id: "d6", name: "JVM 内存模型.md", chunks: 18, created: "2026-06-11 22:15" },
  { id: "d2", name: "MySQL 索引优化.md", chunks: 8, created: "2026-06-11 16:00" },
];

export default function DocumentPage() {
  const [tab, setTab] = useState<Tab>("repository");
  const [selectedDocId, setSelectedDocId] = useState<string | null>(null);
  const [expandedFolders, setExpandedFolders] = useState<Set<string>>(new Set(["MySQL", "Redis", "JVM"]));
  const [expandedChunk, setExpandedChunk] = useState<number | null>(null);

  const selectedDoc = uploadedDocs.find((d) => d.id === selectedDocId);

  const toggleFolder = (name: string) => {
    setExpandedFolders((prev) => {
      const next = new Set(prev);
      next.has(name) ? next.delete(name) : next.add(name);
      return next;
    });
  };

  return (
    <div className="h-full flex min-h-[600px]">
      {/* Left sidebar */}
      <div className="w-[340px] flex-shrink-0 border-r border-[var(--color-line)] flex flex-col">
        {/* Tab switcher */}
        <div className="flex gap-1 p-3 border-b border-[var(--color-line)]">
          {([
            { key: "repository", icon: FolderTree, label: "资料库" },
            { key: "upload", icon: Upload, label: "上传管理" },
          ] as const).map(({ key, icon: Icon, label }) => (
            <button
              key={key}
              onClick={() => setTab(key)}
              className={`flex items-center gap-2 px-4 py-2 rounded-full text-xs font-medium transition-all ${
                tab === key
                  ? "bg-[var(--color-pill-dark)] text-[var(--color-pill-text)] shadow-[var(--shadow-btn)]"
                  : "text-[var(--color-ink-soft)] hover:text-[var(--color-ink)]"
              }`}
            >
              <Icon size={14} />{label}
            </button>
          ))}
        </div>

        {tab === "repository" ? (
          /* Repository: directory tree */
          <div className="flex-1 overflow-auto p-3 space-y-0.5">
            {documentTree.map((folder) => (
              <div key={folder.name}>
                <button
                  onClick={() => toggleFolder(folder.name)}
                  className="w-full flex items-center gap-2 px-2.5 py-1.5 rounded-xl text-xs text-[var(--color-ink-soft)] hover:text-[var(--color-ink)] hover:bg-[var(--color-bg-input)] transition-colors"
                >
                  <ChevronRight
                    size={12}
                    className={`transition-transform ${expandedFolders.has(folder.name) ? "rotate-90" : ""}`}
                  />
                  <Folder size={13} className="text-[var(--color-accent)]" />
                  <span className="font-medium">{folder.name}</span>
                  <span className="ml-auto text-[10px] text-[var(--color-ink-faint)]">{folder.children.length}</span>
                </button>
                {expandedFolders.has(folder.name) && (
                  <div className="ml-5 border-l border-[var(--color-line)] pl-3 space-y-0.5 mt-0.5">
                    {folder.children.map((file: any) => (
                      <button
                        key={file.id}
                        onClick={() => setSelectedDocId(file.id)}
                        className={`w-full flex items-center gap-2 px-2.5 py-1.5 rounded-xl text-xs transition-colors ${
                          selectedDocId === file.id
                            ? "bg-[var(--color-accent-soft)] text-[var(--color-accent)]"
                            : "text-[var(--color-ink-soft)] hover:text-[var(--color-ink)] hover:bg-[var(--color-bg-input)]"
                        }`}
                      >
                        <File size={12} />
                        <span className="truncate">{file.name}</span>
                      </button>
                    ))}
                  </div>
                )}
              </div>
            ))}
          </div>
        ) : (
          /* Upload: drag zone + flat list */
          <div className="flex-1 flex flex-col overflow-hidden">
            <div className="p-3 border-b border-[var(--color-line)]">
              <div className="border-2 border-dashed border-[var(--color-line-strong)] rounded-2xl p-5 text-center cursor-pointer hover:border-[var(--color-accent-border)] hover:bg-[var(--color-accent-soft)] transition-all group">
                <Upload size={20} className="mx-auto mb-1.5 text-[var(--color-ink-faint)] group-hover:text-[var(--color-accent)] transition-colors" />
                <p className="text-[11px] text-[var(--color-ink-soft)]">拖拽 Markdown 文件到此处</p>
                <p className="text-[10px] text-[var(--color-ink-faint)] mt-0.5">或点击选择文件</p>
              </div>
            </div>
            <div className="flex-1 overflow-auto p-3 space-y-1">
              <p className="text-[10px] text-[var(--color-ink-faint)] uppercase tracking-wider px-1 mb-2">
                已上传 ({uploadedDocs.length})
              </p>
              {uploadedDocs.map((d) => (
                <button
                  key={d.id}
                  onClick={() => setSelectedDocId(d.id)}
                  className={`w-full text-left p-3 rounded-2xl transition-all border ${
                    selectedDocId === d.id
                      ? "border-[var(--color-accent-border)] bg-[var(--color-accent-soft)]"
                      : "border-transparent hover:border-[var(--color-line-strong)] hover:bg-[var(--color-bg-card)]"
                  }`}
                >
                  <div className="flex items-center gap-2">
                    <FileText size={14} className={selectedDocId === d.id ? "text-[var(--color-accent)]" : "text-[var(--color-ink-faint)]"} />
                    <span className="text-xs font-medium truncate">{d.name}</span>
                  </div>
                  <div className="flex items-center gap-3 mt-1 text-[10px] text-[var(--color-ink-faint)]">
                    <span className="flex items-center gap-1"><Layers size={9} /> {d.chunks} chunks</span>
                    <span className="flex items-center gap-1"><Clock size={9} /> {d.created}</span>
                  </div>
                </button>
              ))}
            </div>
          </div>
        )}
      </div>

      {/* Right panel */}
      <div className="flex-1 overflow-auto p-8">
        {selectedDoc ? (
          <div>
            <h3 className="serif text-2xl font-semibold mb-1">{selectedDoc.name}</h3>
            <div className="flex items-center gap-3 text-xs text-[var(--color-ink-faint)] mb-8">
              <span className="flex items-center gap-1"><FileText size={12} /> MARKDOWN</span>
              <span>·</span>
              <span className="flex items-center gap-1"><Hash size={12} /> {selectedDoc.id}</span>
              <span>·</span>
              <span className="flex items-center gap-1"><Layers size={12} /> {selectedDoc.chunks} 个分块</span>
            </div>

            {tab === "repository" ? (
              /* Markdown preview */
              <div className="space-y-4">
                <div className="prose max-w-none">
                  <h2 style={{ fontFamily: "var(--font-serif)", fontSize: 22, fontWeight: 600, color: "var(--color-ink)", marginBottom: 16 }}>
                    MySQL 存储引擎
                  </h2>
                  <p style={{ marginBottom: 12 }}>
                    存储引擎是 MySQL 用于处理<strong>数据存储</strong>、<strong>索引管理</strong>以及<strong>数据查询与更新操作</strong>的核心组件。
                    不同存储引擎拥有不同的数据组织方式、索引实现机制以及锁策略。
                  </p>
                  <h3 style={{ fontFamily: "var(--font-serif)", fontSize: 17, fontWeight: 600, color: "var(--color-ink)", marginTop: 24, marginBottom: 10 }}>
                    InnoDB 引擎
                  </h3>
                  <p style={{ marginBottom: 12 }}>
                    InnoDB 是 MySQL 中一种兼顾<strong>高可靠性</strong>与<strong>高性能</strong>的通用存储引擎，自 MySQL 5.5 起被设为默认存储引擎。
                  </p>
                  <ul style={{ paddingLeft: 20, marginBottom: 12 }}>
                    <li style={{ marginBottom: 6 }}><strong>事务支持</strong>：完全遵循 ACID 模型</li>
                    <li style={{ marginBottom: 6 }}><strong>行级锁</strong>：提高高并发环境下的读写性能</li>
                    <li style={{ marginBottom: 6 }}><strong>外键约束</strong>：支持外键和级联操作</li>
                    <li style={{ marginBottom: 6 }}><strong>MVCC</strong>：提供非阻塞的读取操作</li>
                  </ul>
                  <p style={{ marginBottom: 12 }}>
                    InnoDB 的索引采用 <strong>B+Tree</strong> 数据结构，主键索引为聚簇索引，二级索引为非聚簇索引。
                    聚簇索引的叶子节点存储完整行数据，二级索引的叶子节点存储主键值。
                  </p>
                  <blockquote style={{ borderLeft: "3px solid var(--color-accent)", padding: "12px 16px", margin: "16px 0", background: "var(--color-bg-card)", borderRadius: "0 12px 12px 0", fontStyle: "italic" }}>
                    存储引擎是作用于数据表级别的，而非数据库级别。同一个数据库中的不同数据表可以选择不同的存储引擎。
                  </blockquote>
                  <p>在实际应用中，大部分 MySQL 业务表都使用 InnoDB 存储引擎。MyISAM 适合读多写少的场景，Memory 适合临时缓存。</p>
                </div>
              </div>
            ) : (
              /* Chunk list with expandable detail */
              <div>
                <p className="text-[11px] text-[var(--color-ink-faint)] uppercase tracking-wider mb-4">
                  文档分块 ({selectedDoc.chunks})
                </p>
                <div className="space-y-2">
                  {Array.from({ length: Math.min(selectedDoc.chunks, 8) }).map((_, i) => (
                    <div key={i}>
                      <button
                        onClick={() => setExpandedChunk(expandedChunk === i ? null : i)}
                        className={`w-full text-left p-4 rounded-2xl border transition-all ${
                          expandedChunk === i
                            ? "border-[var(--color-accent-border)] bg-[var(--color-bg-card)]"
                            : "border-[var(--color-line)] bg-[var(--color-bg-card)] hover:border-[var(--color-line-strong)]"
                        }`}
                      >
                        <div className="flex items-center gap-2 mb-2">
                          <span className="text-[10px] text-[var(--color-ink-faint)] font-mono">#{i + 1}</span>
                          <p className="text-xs text-[var(--color-accent)] font-mono">
                            MySQL 存储引擎 &gt; InnoDB 引擎 &gt; 第 {i + 1} 节
                          </p>
                          <span className="ml-auto text-[10px] text-[var(--color-ink-faint)]">
                            {expandedChunk === i ? "收起 ▲" : "展开 ▼"}
                          </span>
                        </div>
                        <p className="serif text-sm leading-relaxed text-[var(--color-ink-soft)] line-clamp-2">
                          {expandedChunk === i
                            ? ""
                            : "InnoDB 是 MySQL 的默认存储引擎，支持事务、行级锁、MVCC 和外键约束。采用 B+Tree 索引结构..."}
                        </p>
                        <div className="flex items-center gap-1.5 mt-3">
                          {["mysql", "innodb", "索引"].map((tag) => (
                            <span key={tag} className="text-[10px] px-2.5 py-0.5 rounded-full bg-[var(--color-accent-soft)] text-[var(--color-accent)]">{tag}</span>
                          ))}
                        </div>
                      </button>

                      {/* Expanded chunk detail */}
                      {expandedChunk === i && (
                        <div className="mt-2 p-5 rounded-2xl border border-[var(--color-line)] bg-[var(--color-surface-1)] fade-in">
                          <div className="prose max-w-none serif text-sm leading-relaxed text-[var(--color-ink-soft)]">
                            <h3 style={{ fontFamily: "var(--font-serif)", fontSize: 18, fontWeight: 600, color: "var(--color-ink)", marginBottom: 12 }}>
                              InnoDB 存储引擎
                            </h3>
                            <p style={{ marginBottom: 10 }}>
                              InnoDB 是 MySQL 中一种兼顾<strong>高可靠性</strong>与<strong>高性能</strong>的通用存储引擎，
                              自 MySQL 5.5 起被设为默认存储引擎。
                            </p>
                            <h4 style={{ fontFamily: "var(--font-serif)", fontSize: 15, fontWeight: 600, color: "var(--color-ink)", marginTop: 18, marginBottom: 8 }}>
                              核心特点
                            </h4>
                            <ul style={{ paddingLeft: 18, marginBottom: 10 }}>
                              <li style={{ marginBottom: 4 }}><strong>事务支持</strong>：完全遵循 ACID 模型，保证 DML 操作的原子性、一致性、隔离性和持久性。</li>
                              <li style={{ marginBottom: 4 }}><strong>行级锁</strong>：通过对单条记录加锁，提高高并发环境下的读写性能和数据一致性。</li>
                              <li style={{ marginBottom: 4 }}><strong>外键约束</strong>：支持外键和级联操作，保证数据的完整性与关联关系正确性。</li>
                              <li style={{ marginBottom: 4 }}><strong>MVCC</strong>：提供非阻塞的读取操作，进一步提升并发性能。</li>
                            </ul>
                            <h4 style={{ fontFamily: "var(--font-serif)", fontSize: 15, fontWeight: 600, color: "var(--color-ink)", marginTop: 18, marginBottom: 8 }}>
                              索引结构
                            </h4>
                            <p style={{ marginBottom: 10 }}>
                              InnoDB 的索引采用 <strong>B+Tree</strong> 数据结构实现。主键索引（聚簇索引）的叶子节点存储完整行数据，
                              二级索引（非聚簇索引）的叶子节点存储主键值，查询时需要通过回表获取完整记录。
                            </p>
                            <blockquote style={{ borderLeft: "3px solid var(--color-accent)", padding: "10px 14px", margin: "14px 0", background: "var(--color-bg-card)", borderRadius: "0 10px 10px 0", fontSize: 13, fontStyle: "italic" }}>
                              聚簇索引决定了数据的物理存储顺序，因此每张表只能有一个聚簇索引。选择合适的聚簇索引键对性能至关重要。
                            </blockquote>
                            <p>
                              在绝大多数业务场景下，使用 InnoDB 即可满足需求。对于只读或读多写少的场景可以考虑 MyISAM，
                              对于高速临时数据访问可以使用 Memory 引擎。
                            </p>
                          </div>
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