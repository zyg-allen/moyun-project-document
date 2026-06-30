import { httpGet, httpPost } from './client';
import type {
  Tag,
} from '@/types/api';

// 获取热门标签
export const getHotTags = (limit = 20) => {
  return httpGet<Tag[]>('/portal/tag/hot', { limit });
};

// 搜索标签
export const searchTagList = (keyword: string) => {
  return httpGet<Tag[]>('/portal/tag/search', { keyword });
};

// 创建新标签
export const createNewTag = (name: string) => {
  return httpPost<Tag>('/portal/tag/create', { name });
};

// 获取标签建议
export const getRecommendTags = (title: string, category: string) => {
  return httpGet<Tag[]>('/portal/tag/recommend', { title, category });
};
