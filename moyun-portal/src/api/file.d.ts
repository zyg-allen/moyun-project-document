import type { FileInfo } from '@/types/api';
export declare const uploadPortalFile: (file: File, businessType?: string, businessId?: string) => Promise<import("./client").ApiResponse<FileInfo>>;
