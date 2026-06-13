import { createContext, useContext, useState, type ReactNode } from "react";
import type { CitationItem } from "./api";

export interface Msg { role: "USER" | "ASSISTANT"; content: string; citations?: CitationItem[]; done?: boolean; showCitations?: boolean }

interface AgentCtx {
  messages: Msg[];
  setMessages: React.Dispatch<React.SetStateAction<Msg[]>>;
  sending: boolean;
  setSending: (v: boolean) => void;
  sessionId: string;
  setSessionId: (v: string) => void;
  mode: string;
  setMode: (v: string) => void;
}

const AgentContext = createContext<AgentCtx>({
  messages: [], setMessages: () => {},
  sending: false, setSending: () => {},
  sessionId: "", setSessionId: () => {},
  mode: "FORCE", setMode: () => {},
});

export function AgentStateProvider({ children }: { children: ReactNode }) {
  const [messages, setMessages] = useState<Msg[]>([]);
  const [sending, setSending] = useState(false);
  const [sessionId, setSessionId] = useState("");
  const [mode, setMode] = useState("FORCE");
  return (
    <AgentContext.Provider value={{ messages, setMessages, sending, setSending, sessionId, setSessionId, mode, setMode }}>
      {children}
    </AgentContext.Provider>
  );
}

export function useAgentReset() {
  const { setMessages, setSending, setSessionId } = useContext(AgentContext);
  return () => { setMessages([]); setSending(false); setSessionId(""); };
}

export function useAgentStore() { return useContext(AgentContext); }