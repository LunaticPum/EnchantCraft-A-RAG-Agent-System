const BASE = "/api/v1";

export interface DocumentItem {
  id: string;
  fileName: string;
  fileType: string;
  directoryPath: string;
  rawContent: string;
  refCount: number;
  createdAt: string;
  updatedAt: string;
}

export interface ChunkItem {
  id: string;
  documentId: string;
  chunkIndex: number;
  titlePath: string;
  content: string;
  moduleTags: string[];
  createdAt: string;
}

export interface CitationItem {
  chunkId: string; documentId: string; titlePath: string; snippet: string;
}
export interface SearchResultItem {
  chunkId: string; documentId: string; titlePath: string; content: string; score: number;
}
export interface ApiResponse<T> { code: string; info: string; data: T; }

let storedToken = localStorage.getItem("auth_token") || "";
export function setAuthToken(t: string) { storedToken = t; localStorage.setItem("auth_token", t); }
export function getAuthToken() { return storedToken; }

async function request<T>(url: string, options?: RequestInit): Promise<T> {
  const headers: Record<string, string> = { ...(options?.headers as Record<string, string> || {}) };
  if (storedToken) headers["Authorization"] = "Bearer " + storedToken;
  const res = await fetch(BASE + url, { ...options, headers });
  if (!res.ok) throw new Error(`请求失败 (${res.status})`);
  const text = await res.text();
  try {
    const json: ApiResponse<T> = JSON.parse(text);
    if (json.code !== "0000") throw new Error(json.info);
    return json.data;
  } catch (e: any) {
    if (e.message?.includes("JSON")) throw new Error("服务器返回异常，请稍后重试");
    throw e;
  }
}

/* Document APIs */
export const api = {
  listDocuments: () => request<DocumentItem[]>("/document/list"),

  getDocument: (id: string) => request<DocumentItem>(`/document/${id}`),

  getChunks: (id: string) => request<ChunkItem[]>(`/document/${id}/chunks`),

  upload: async (file: File, directoryPath: string, onProgress?: (pct: number) => void) => {
    const form = new FormData();
    form.append("file", file);
    form.append("directoryPath", directoryPath);

    return new Promise<DocumentItem>((resolve, reject) => {
      const xhr = new XMLHttpRequest();
      xhr.upload.onprogress = (e) => {
        if (e.lengthComputable && onProgress) onProgress(Math.round((e.loaded / e.total) * 100));
      };
      xhr.onload = () => {
        const json: ApiResponse<DocumentItem> = JSON.parse(xhr.responseText);
        if (json.code === "0000") resolve(json.data);
        else reject(new Error(json.info));
      };
      xhr.onerror = () => reject(new Error("上传失败"));
      xhr.open("POST", BASE + "/document/upload");
      if (storedToken) xhr.setRequestHeader("Authorization", "Bearer " + storedToken);
      xhr.send(form);
    });
  },

  /* Agent SSE chat */
  agentChat: (
    body: { sessionId?: string; message: string; retrievalMode: string },
    onCitation: (c: CitationItem[]) => void,
    onToken: (t: string) => void,
    onDone: (sid: string) => void,
    onError: (err: string) => void,
  ) => {
    const headers: Record<string, string> = { "Content-Type": "application/json" };
    if (storedToken) headers["Authorization"] = "Bearer " + storedToken;
    fetch(BASE + "/agent/chat", {
      method: "POST",
      headers,
      body: JSON.stringify(body),
    }).then(async (res) => {
      if (!res.ok || !res.body) { onError("请求失败"); return; }
      const reader = res.body.getReader();
      const dec = new TextDecoder();
      let buf = "", eType = "";
      while (true) {
        const { done, value } = await reader.read();
        if (done) break;
        buf += dec.decode(value, { stream: true });
        const lines = buf.split("\n");
        buf = lines.pop() || "";
        for (const line of lines) {
          const l = line.replace(/\r$/, "");
          if (l === "") continue;
          if (l.startsWith("event:")) { eType = l.slice(6).trim(); }
          else if (l.startsWith("data:") && eType) {
            const raw = l.slice(5);
            const d = eType === "token" ? raw : raw.replace(/^\s/, "");
            try {
              if (eType === "citation") onCitation(JSON.parse(d));
              else if (eType === "token") onToken(d);
              else if (eType === "done") onDone(d);
              else if (eType === "error") onError(d);
            } catch {}
            eType = "";
          }
        }
      }
    }).catch((e) => onError(e.message || "对话失败"));
  },

  search: (keyword: string, topK = 5, strategy = "HYBRID", rerank = true) =>
    request<SearchResultItem[]>(
      `/document/search?keyword=${encodeURIComponent(keyword)}&topK=${topK}&strategy=${strategy}&rerank=${rerank}`
    ),

  deleteDocument: (id: string) =>
    request<void>(`/document/${id}`, { method: "DELETE" }),

  vectorHealth: () => request<number>("/document/vector-health"),
  reEmbedAll: () => request<void>("/document/re-embed-all", { method: "POST" }),
  quota: () => request<{search: number; chat: number}>("/document/quota"),

  /* Auth */
  login: (username: string, password: string) =>
    request<AuthResponse>("/auth/login", {
      method: "POST",
      headers: { "Content-Type": "application/json" },
      body: JSON.stringify({ username, password }),
    }),

  register: (username: string, password: string, email: string) =>
    request<void>("/auth/register", {
      method: "POST",
      headers: { "Content-Type": "application/json" },
      body: JSON.stringify({ username, password, email }),
    }),

  /* Bagu Skill */
  baguGenerate: (shelfName: string, documentIds: string[]) =>
    request<BaguSetResponse>("/bagu/generate", {
      method: "POST",
      headers: { "Content-Type": "application/json" },
      body: JSON.stringify({ shelfName, documentIds }),
    }),
  baguListSets: () => request<BaguSetResponse[]>("/bagu/sets"),
  baguGetSet: (id: string) => request<BaguSetResponse>(`/bagu/sets/${id}`),
  baguEvaluate: (question: string, standardAnswer: string, userAnswer: string) =>
    request<string>("/bagu/evaluate", {
      method: "POST",
      headers: { "Content-Type": "application/json" },
      body: JSON.stringify({ question, standardAnswer, userAnswer }),
    }),
};

export interface BaguItemResponse {
  id: string; question: string; answer: string; difficulty: string; sortOrder: number;
}
export interface BaguSetResponse {
  id: string; title: string; description: string; itemCount: number; items: BaguItemResponse[]; createdAt: string;
}

export interface AuthResponse {
  token: string;
  userId: string;
  username: string;
  role: string;
}
