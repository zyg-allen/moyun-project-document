import { httpUpload } from './client';
import type {
  UploadFileResponse,
} from '@/types/api';

export const uploadFile = (file: File, extra?: Record<string, string>) => {
  return httpUpload<UploadFileResponse>('/portal/file/upload', file, extra);
};

export const uploadImage = (file: File, extra?: Record<string, string>) => {
  return httpUpload<UploadFileResponse>('/portal/file/upload', file, extra);
};
