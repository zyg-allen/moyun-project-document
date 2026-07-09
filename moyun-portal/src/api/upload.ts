import { httpUpload } from './client';
import type { FileInfo } from '@/types/api';

// 统一文件上传入口：与 api/file.ts 的 uploadPortalFile 指向同一后端接口
// /portal/file/upload，后端返回 SysFile 对象（结构同 FileInfo），字段为 fileUrl。
export const uploadFile = (file: File, extra?: Record<string, string>) => {
  return httpUpload<FileInfo>('/portal/file/upload', file, extra);
};

export const uploadImage = (file: File, extra?: Record<string, string>) => {
  return httpUpload<FileInfo>('/portal/file/upload', file, extra);
};
