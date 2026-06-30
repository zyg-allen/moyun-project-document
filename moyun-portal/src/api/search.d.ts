import type { SearchParams, SearchResponse } from '@/types/api';
export declare const search: (params: SearchParams) => Promise<import("./client").ApiResponse<SearchResponse>>;
