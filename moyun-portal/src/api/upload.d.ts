import type { UploadFileResponse } from '@/types/api';
export declare const uploadFile: (file: File) => Promise<import("./client").ApiResponse<UploadFileResponse>>;
export declare const uploadImage: (file: File) => Promise<import("./client").ApiResponse<UploadFileResponse>>;
