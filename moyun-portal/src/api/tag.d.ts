import type { Tag } from '@/types/api';
export declare const getHotTags: (limit?: number) => Promise<import("./client").ApiResponse<Tag[]>>;
export declare const searchTagList: (keyword: string) => Promise<import("./client").ApiResponse<Tag[]>>;
export declare const createNewTag: (name: string) => Promise<import("./client").ApiResponse<Tag>>;
export declare const getRecommendTags: (title: string, category: string) => Promise<import("./client").ApiResponse<Tag[]>>;
