import type { Category, CategoryListParams } from '@/types/api';
interface BackendResponse<T = any> {
    code: number;
    msg: string;
    data?: T;
}
/**
 * 判断分类是否应该展示（统一过滤逻辑）
 * 过滤条件：
 * 1. status 字段：0 展示  1 隐藏（后端维护）
 * 2. 名称黑名单：排除首页、读书空间、面试指南（由前端硬编码菜单）
 * @param cat 分类对象
 * @returns true=展示 false=隐藏
 */
export declare function shouldShowCategory(cat: Category | null | undefined): boolean;
/**
 * 过滤分类树（同时过滤父节点和子节点）
 * @param categories 原始分类数组
 * @returns 过滤后的分类数组
 */
export declare function filterCategoryTree(categories: Category[] | null | undefined): Category[];
/**
 * 根据分类计算跳转目标
 * @param cat 分类对象
 * @returns { type: 'internal' | 'external', path: string }
 */
export declare function getCategoryTarget(cat: Category): {
    type: 'internal' | 'external';
    path: string;
};
export declare const getCategoryTree: (params?: CategoryListParams) => Promise<BackendResponse<Category[]>>;
export declare const clearCategoryTreeCache: () => void;
export {};
