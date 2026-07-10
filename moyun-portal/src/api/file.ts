import { httpDelete, httpPost } from './client';
import type {
  FileInfo,
  UploadFileParams,
} from '@/types/api';

// 上传文件
export const uploadPortalFile = (file: File, businessType?: string, businessId?: string) => {
  const formData = new FormData();
  formData.append('file', file);
  if (businessType) {
    formData.append('businessType', businessType);
  }
  if (businessId) {
    formData.append('businessId', businessId);
  }
  return httpPost<FileInfo>('/portal/file/upload', formData);
};

/**
 * 删除文件（存储 + sys_file 记录）
 * 用于前端「删除/替换附件」时清理 MinIO / 本地文件，避免脏数据堆积。
 * 后端校验文件归属本人，非本人上传会返回错误；记录不存在视为已删除（幂等）。
 *
 * @param fileUrl 文件访问 URL（上传时返回的 fileUrl）
 */
export const deletePortalFile = (fileUrl: string) => {
  return httpDelete<boolean>(`/portal/file?fileUrl=${encodeURIComponent(fileUrl)}`);
};
