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
}

const AgentContext = createContext<AgentCtx>({
  messages: [], setMessages: () => {},
  sending: false, setSending: () => {},
  sessionId: "", setSessionId: () => {},
});

export function AgentStateProvider({ children }: { children: ReactNode }) {
  const [messages, setMessages] = useState<Msg[]>([]);
  const [sending, setSending] = useState(false);
  const [sessionId, setSessionId] = useState("");
  return (
    <AgentContext.Provider value={{ messages, setMessages, sending, setSending, sessionId, setSessionId }}>
      {children}
    </AgentContext.Provider>
  );
}

export function useAgentStore() { return useContext(AgentContext); }