import ReactMarkdown from "react-markdown";
import remarkGfm from "remark-gfm";

/** Obsidian callout → 普通 blockquote 文本转换 */
const CALLOUT_RE = /^\[!(\w+)\]\s*(.*)/;

function convertCallouts(md: string): string {
  const lines = md.split("\n");
  const out: string[] = [];
  let i = 0;
  while (i < lines.length) {
    const line = lines[i];
    const m = line.match(/^>\s*(.*)/);
    if (m) {
      const inner = m[1];
      const cm = inner.match(CALLOUT_RE);
      if (cm) {
        // 发现 callout 头：> [!quote] 标题 → 输出为 > 💬 **标题**
        const type = cm[1].toLowerCase();
        const title = cm[2] || type.charAt(0).toUpperCase() + type.slice(1);
        const emoji = calloutEmoji[type] || "📌";
        out.push(`> ${emoji} **${title}**`);
        out.push("> ");
        i++;
        // 后续连续的 > 行保持原样
        while (i < lines.length && lines[i].startsWith(">")) {
          out.push(lines[i]);
          i++;
        }
        out.push("");
        continue;
      }
    }
    out.push(line);
    i++;
  }
  return out.join("\n");
}

const calloutEmoji: Record<string, string> = {
  note: "📝", info: "ℹ️", todo: "✅", tip: "💡", abstract: "📋",
  question: "❓", quote: "💬", example: "📖", success: "✅",
  warning: "⚠️", failure: "❌", danger: "⚡", bug: "🐛",
};

export function truncateContent(md: string, maxLen: number): string {
  if (!md || md.length <= maxLen) return md || "";
  return md.slice(0, maxLen) + `\n\n> *(内容过长，仅展示前${maxLen}字)*`;
}

interface MdViewerProps { content: string; maxLen?: number; }

export function MdViewer({ content, maxLen = 3000 }: MdViewerProps) {
  const md = truncateContent(convertCallouts(content || ""), maxLen);
  return (
    <div className="md-content">
      <ReactMarkdown remarkPlugins={[remarkGfm]}>{md}</ReactMarkdown>
    </div>
  );
}