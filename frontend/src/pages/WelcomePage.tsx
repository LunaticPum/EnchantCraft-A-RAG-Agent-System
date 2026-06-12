import { useNavigate } from "react-router-dom";
import { ArrowRight, BookOpen, Brain, Sparkles, Zap } from "lucide-react";

const features = [
  {
    index: "01",
    title: "上传学习笔记",
    icon: BookOpen,
    copy: "把你的技术笔记、面试资料或项目总结放进系统，支持 Markdown 文件上传，自动分块和索引。",
  },
  {
    index: "02",
    title: "AI 自动生成题库",
    icon: Brain,
    copy: "Agent 基于你的资料提炼出结构化面试问答集，涵盖基础知识、源码分析和场景追问。",
  },
  {
    index: "03",
    title: "智能检索与对话",
    icon: Zap,
    copy: "通过 RAG 检索 + LLM 对话，随时就学习资料提问，Agent 从你的笔记中找证据并给出带来源引用的回答。",
  },
];

export default function WelcomePage() {
  const navigate = useNavigate();

  return (
    <div className="min-h-screen flex flex-col items-center justify-center p-8" style={{ gap: 48 }}>
      {/* Hero */}
      <div className="text-center flex flex-col items-center" style={{ gap: 24 }}>
        <img src="/obsidian-icon.png" alt="QA Agent" className="w-20 h-20" />
        <div>
          <h1 className="serif font-bold tracking-tight" style={{
            fontSize: "clamp(48px, 7vw, 80px)",
            lineHeight: 1.06,
            letterSpacing: "-0.04em",
            color: "var(--color-ink)",
          }}>
            QA Agent
          </h1>
          <p className="serif font-bold" style={{
            fontSize: "clamp(36px, 5vw, 64px)",
            color: "var(--color-ink-soft)",
            marginTop: 8,
          }}>
            从个人笔记到面试题库
          </p>
        </div>
        <p className="serif text-center max-w-[680px]" style={{
          fontSize: 17,
          lineHeight: 1.75,
          color: "var(--color-ink-soft)",
        }}>
          基于你的真实资料，QA Agent 构建高可信面试题库，自动总结知识笔记和标准回答，
          支持智能检索与多轮对话，把每轮练习沉淀为可回看、可修正、可持续迭代的成长闭环。
        </p>
        <button
          onClick={() => navigate("/login")}
          className="pill pill--dark text-sm px-8 py-3.5 flex items-center gap-2 shadow-xl"
        >
          <Sparkles size={16} />
          开始使用
          <ArrowRight size={15} />
        </button>
      </div>

      {/* Feature cards */}
      <div className="grid max-w-[960px] mx-auto w-full" style={{
        gridTemplateColumns: "repeat(auto-fit, minmax(280px, 1fr))",
        gap: 16,
      }}>
        {features.map(({ index, title, copy, icon: Icon }) => (
          <div
            key={index}
            className="p-6 rounded-3xl border border-[var(--color-line)] bg-[var(--color-bg-card)] hover:border-[var(--color-line-strong)] transition-colors"
          >
            <div className="flex items-center gap-3 mb-3">
              <span className="text-[11px] text-[var(--color-ink-faint)] font-mono">{index}</span>
              <Icon size={18} className="text-[var(--color-accent)]" />
            </div>
            <h3 className="serif text-lg font-semibold text-[var(--color-ink)] mb-2">{title}</h3>
            <p className="text-[13px] leading-relaxed text-[var(--color-ink-soft)]">{copy}</p>
          </div>
        ))}
      </div>
    </div>
  );
}