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

export interface ApiResponse<T> {
  code: string;
  info: string;
  data: T;
}

async function request<T>(url: string, options?: RequestInit): Promise<T> {
  const res = await fetch(BASE + url, options);
  const json: ApiResponse<T> = await res.json();
  if (json.code !== "0000") throw new Error(json.info);
  return json.data;
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
      xhr.send(form);
    });
  },
};