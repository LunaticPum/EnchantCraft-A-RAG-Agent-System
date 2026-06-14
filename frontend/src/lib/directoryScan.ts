export interface FileEntry {
  file: File;
  relativePath: string; // e.g. "MySQL/存储引擎"
}

/**
 * 使用 File System Access API 打开目录，递归扫描所有 .md 文件
 * 需要安全上下文（HTTPS 或 localhost）
 */
async function scanViaShowDirectoryPicker(): Promise<FileEntry[]> {
  const dirHandle = await (window as any).showDirectoryPicker();
  const rootName = dirHandle.name || "";
  const entries: FileEntry[] = [];
  await scanRecursive(dirHandle, rootName, entries);
  return entries.filter((e) => e.file.name.endsWith(".md"));
}

async function scanRecursive(
  dirHandle: any,
  parentPath: string,
  entries: FileEntry[]
): Promise<void> {
  for await (const [name, handle] of dirHandle.entries()) {
    if (handle.kind === "file" && name.endsWith(".md")) {
      const file = await handle.getFile();
      entries.push({ file, relativePath: parentPath || "" });
    } else if (handle.kind === "directory") {
      const childPath = parentPath ? `${parentPath}/${name}` : name;
      await scanRecursive(handle, childPath, entries);
    }
  }
}

/**
 * 降级方案：使用 <input type="file" webkitdirectory>
 * 兼容所有 Chrome 版本，无需安全上下文（HTTP 可用）
 * webkitRelativePath 示例: "MySQL/存储引擎/InnoDB.md"
 */
function scanViaInput(): Promise<FileEntry[]> {
  return new Promise((resolve, reject) => {
    const input = document.createElement("input");
    input.type = "file";
    input.webkitdirectory = true;
    input.multiple = true;
    input.style.display = "none";
    document.body.appendChild(input);

    input.onchange = () => {
      const files = input.files;
      const entries: FileEntry[] = [];
      if (files) {
        for (let i = 0; i < files.length; i++) {
          const f = files[i];
          if (!f.name.endsWith(".md")) continue;
          // webkitRelativePath: "MySQL/存储引擎/InnoDB.md" → relativePath: "MySQL/存储引擎"
          const parts = (f as any).webkitRelativePath?.split("/") || [];
          const relativePath = parts.length > 1 ? parts.slice(0, -1).join("/") : "";
          entries.push({ file: f, relativePath });
        }
      }
      document.body.removeChild(input);
      resolve(entries);
    };

    input.oncancel = () => {
      document.body.removeChild(input);
      reject(new Error("用户取消了选择"));
    };

    // 兼容处理：某些情况下没有 oncancel 事件
    input.addEventListener("cancel", () => {
      document.body.removeChild(input);
      reject(new Error("用户取消了选择"));
    });

    input.click();
  });
}

/**
 * 扫描本地目录中的所有 .md 文件
 * - 安全上下文（HTTPS/localhost）：使用 showDirectoryPicker
 * - HTTP 环境：自动降级为 <input webkitdirectory>
 */
export async function scanDirectory(): Promise<FileEntry[]> {
  // 优先使用 File System Access API（可递归子目录、更友好）
  if ((window as any).showDirectoryPicker) {
    return scanViaShowDirectoryPicker();
  }
  // 降级：<input webkitdirectory>（Chrome 全版本支持，无需 HTTPS）
  return scanViaInput();
}

/**
 * 选择单个或多个 .md 文件（非目录模式）
 * 使用 <input type="file" accept=".md" multiple>
 */
export function scanFiles(): Promise<FileEntry[]> {
  return new Promise((resolve, reject) => {
    const input = document.createElement("input");
    input.type = "file";
    input.accept = ".md";
    input.multiple = true;
    input.style.display = "none";
    document.body.appendChild(input);

    input.onchange = () => {
      const files = input.files;
      const entries: FileEntry[] = [];
      if (files) {
        for (let i = 0; i < files.length; i++) {
          const f = files[i];
          if (!f.name.endsWith(".md")) continue;
          entries.push({ file: f, relativePath: "" });
        }
      }
      document.body.removeChild(input);
      resolve(entries);
    };

    input.oncancel = () => {
      document.body.removeChild(input);
      reject(new Error("用户取消了选择"));
    };

    input.click();
  });
}

/**
 * 批量上传文件（目录扫描模式）
 * @returns 成功上传的数量
 */
export async function batchUploadFiles(
  files: FileEntry[],
  uploadFn: (file: File, dirPath: string) => Promise<any>,
  onProgress?: (current: number, total: number, fileName: string) => void
): Promise<number> {
  let success = 0;
  for (let i = 0; i < files.length; i++) {
    const { file, relativePath } = files[i];
    try {
      onProgress?.(i + 1, files.length, file.name);
      await uploadFn(file, relativePath);
      success++;
    } catch (e) {
      console.warn(`[批量上传] 跳过 "${file.name}": ${e}`);
    }
  }
  return success;
}
