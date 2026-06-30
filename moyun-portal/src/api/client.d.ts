export interface ApiResponse<T = any> {
    code: number;
    message: string;
    data: T;
}
export declare const getToken: () => string | null;
export declare const setToken: (token: string) => void;
export declare const removeToken: () => void;
export declare const getRefreshToken: () => string | null;
export declare const setRefreshToken: (token: string) => void;
export declare const removeRefreshToken: () => void;
export interface PaginationResponse<T> {
    list: T[];
    total: number;
    page: number;
    pageSize: number;
}
export declare const httpGet: <T>(url: string, params?: Record<string, any>) => Promise<ApiResponse<T>>;
export declare const httpGetList: <T>(url: string, params?: Record<string, any>) => Promise<ApiResponse<PaginationResponse<T>>>;
export declare const httpPost: <T>(url: string, data?: Record<string, any> | FormData) => Promise<ApiResponse<T>>;
export declare const httpPut: <T>(url: string, data?: Record<string, any> | FormData) => Promise<ApiResponse<T>>;
export declare const httpDelete: <T>(url: string) => Promise<ApiResponse<T>>;
export declare const httpUpload: <T>(url: string, file: File) => Promise<ApiResponse<T>>;
