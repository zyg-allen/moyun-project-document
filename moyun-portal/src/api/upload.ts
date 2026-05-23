import { httpUpload } from './client';
import type {
  UploadFileResponse,
} from '@/types/api';

// 上传文件
export const uploadFile = (file: File) => {
  return httpUpload<UploadFileResponse>('/upload/file', file);
};

// 上传图片
export const uploadImage = (file: File) => {
  return httpUpload<UploadFileResponse>('/upload/image', file);
};
