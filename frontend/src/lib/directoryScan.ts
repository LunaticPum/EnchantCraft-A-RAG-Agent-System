export interface FileEntry {
  file: File;
  relativePath: string; // e.g. "MySQL/存储引擎"
}

/**
 * 使用 File System Access API 打开目录，递归扫描所有 .md 文件
 */
export async function scanDirectory(): Promise<FileEntry[]> {
  const dirHandle = await (window as any).showDirectoryPicker();
  // 以选中的根目录名作为顶层路径
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