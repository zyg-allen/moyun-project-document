import { httpPost } from './client';
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
