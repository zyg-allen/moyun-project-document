import { httpGet } from './client';
import type {
  Category,
  CategoryListParams,
} from '@/types/api';

// 后台响应类型（与client.ts保持一致）
interface BackendResponse<T = any> {
  code: number;
  msg: string;
  data?: T;
}

// 分类数据缓存
let categoryTreeCache: Category[] | null = null;
let categoryTreeLoading: Promise<Category[] | null> | null = null;

/**
 * 判断分类是否应该展示（统一过滤逻辑）
 * 过滤条件：
 * 1. status 字段：0 展示  1 隐藏（后端维护）
 * 2. 名称黑名单：排除首页、读书空间、面试指南（由前端硬编码菜单）
 * @param cat 分类对象
 * @returns true=展示 false=隐藏
 */
export function shouldShowCategory(cat: Category | null | undefined): boolean {
  if (!cat) return false;

  // 条件1：status 字段控制 - 0 展示 1 隐藏（同时兼容字符串和数字类型）
  const status = String(cat.status ?? '0');
  if (status === '1') return false;

  // 条件2：名称过滤（排除首页、读书空间、面试指南等由前端硬编码的分类）
  const name = (cat.name || '').trim();
  if (!name) return false;
  const blockedNames = ['首页', '读书空间', '面试指南'];
  if (blockedNames.includes(name)) return false;

  return true;
}

/**
 * 过滤分类树（同时过滤父节点和子节点）
 * @param categories 原始分类数组
 * @returns 过滤后的分类数组
 */
export function filterCategoryTree(categories: Category[] | null | undefined): Category[] {
  if (!categories || !Array.isArray(categories)) return [];
  return categories
    .filter(shouldShowCategory)
    .map(cat => ({
      ...cat,
      children: cat.children && cat.children.length > 0
        ? filterCategoryTree(cat.children)
        : []
    }));
}

/**
 * 根据分类计算跳转目标
 * @param cat 分类对象
 * @returns { type: 'internal' | 'external', path: string }
 */
export function getCategoryTarget(cat: Category): { type: 'internal' | 'external'; path: string } {
  // linkType=1 表示外部链接
  const linkType = String(cat.linkType ?? '0');
  if (linkType === '1' && cat.externalUrl) {
    return { type: 'external', path: cat.externalUrl };
  }
  // 默认：本地路由跳转到栏目
  return { type: 'internal', path: `/category/${encodeURIComponent(cat.name)}` };
}

// 获取分类列表
export const getCategoryList = (params?: CategoryListParams) => {
  return httpGet<Category[]>('/portal/category/list', params);
};

// 获取树形分类（带缓存）
export const getCategoryTree = async (params?: CategoryListParams): Promise<BackendResponse<Category[]>> => {
  // 如果已有缓存，直接返回
  if (categoryTreeCache) {
    return { code: 200, msg: '操作成功', data: categoryTreeCache };
  }

  // 如果正在加载，返回同一个Promise
  if (categoryTreeLoading) {
    const data = await categoryTreeLoading;
    return { code: 200, msg: '操作成功', data };
  }

  // 开始加载
  categoryTreeLoading = new Promise(async (resolve) => {
    try {
      const response = await httpGet<Category[]>('/portal/category/public/tree', params);
      if (response.code === 200) {
        categoryTreeCache = response.data || [];
        resolve(categoryTreeCache);
      } else {
        resolve(null);
      }
    } catch (error) {
      console.error('加载分类树失败:', error);
      resolve(null);
    } finally {
      categoryTreeLoading = null;
    }
  });

  const data = await categoryTreeLoading;
  return { code: 200, msg: '操作成功', data };
};

// 清除缓存（用于刷新场景）
export const clearCategoryTreeCache = () => {
  categoryTreeCache = null;
};

// 获取分类详情
export const getCategoryById = (id: string) => {
  return httpGet<Category>(`/portal/category/${id}`);
};
