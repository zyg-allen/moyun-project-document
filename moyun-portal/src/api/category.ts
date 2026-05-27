import { httpGet } from './client';
import type {
  Category,
  CategoryListParams,
} from '@/types/api';

// 获取分类列表
export const getCategoryList = (params?: CategoryListParams) => {
  return httpGet<Category[]>('/portal/category/list', params);
};

// 获取分类详情
export const getCategoryById = (id: string) => {
  return httpGet<Category>(`/portal/category/${id}`);
};
