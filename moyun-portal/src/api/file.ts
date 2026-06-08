import { httpGet, httpPost, httpDelete } from './client';
import type {
  FileInfo,
  FileListParams,
  UploadFileParams,
} from '@/types/api';

// 获取我的文件列表
export const getMyFileList = (params?: FileListParams) => {
  return httpGet<any>('/portal/file/list', params);
};

// 获取文件详情
export const getFileDetail = (id: string) => {
  return httpGet<FileInfo>(`/portal/file/${id}`);
};

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

// 删除文件
export const deletePortalFile = (id: string) => {
  return httpDelete(`/portal/file/${id}`);
};
