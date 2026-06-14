import { useState, useRef, useCallback, type ReactNode } from "react";

interface McModalProps {
  open: boolean;
  onClose: () => void;
  title: string;
  children: ReactNode;
}

export default function McModal({ open, onClose, title, children }: McModalProps) {
  const [pos, setPos] = useState({ x: 0, y: 0 });
  const dragging = useRef(false);
  const offset = useRef({ x: 0, y: 0 });
  const modalRef = useRef<HTMLDivElement>(null);

  const onMouseDown = useCallback((e: React.MouseEvent) => {
    dragging.current = true;
    offset.current = { x: e.clientX - pos.x, y: e.clientY - pos.y };
    const onMove = (ev: MouseEvent) => {
      if (!dragging.current) return;
      setPos({ x: ev.clientX - offset.current.x, y: ev.clientY - offset.current.y });
    };
    const onUp = () => { dragging.current = false; window.removeEventListener("mousemove", onMove); window.removeEventListener("mouseup", onUp); };
    window.addEventListener("mousemove", onMove);
    window.addEventListener("mouseup", onUp);
  }, [pos]);

  if (!open) return null;

  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center" style={{ background: "rgba(0,0,0,0.5)", pointerEvents: "auto" }} onClick={onClose}>
      <div
        ref={modalRef}
        className="mc-modal"
        style={{ transform: `translate(${pos.x}px, ${pos.y}px)` }}
        onClick={(e) => e.stopPropagation()}
      >
        {/* 标题栏（可拖拽） */}
        <div className="mc-modal-header" onMouseDown={onMouseDown}>
          <span>{title}</span>
          <button onClick={onClose} className="mc-modal-close">✕</button>
        </div>
        {/* 内容 */}
        <div className="mc-modal-body">
          {children}
        </div>
      </div>
    </div>
  );
}
