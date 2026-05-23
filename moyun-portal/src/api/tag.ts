import { httpGet, httpPost } from './client';
import type {
  Tag,
  TagListParams,
  PaginationResponse,
} from '@/types/api';
import { getTags, getHotTags as getMockHotTags, searchTags, createTag as createMockTag, getTagSuggestions } from '@/data/mockData';

// 获取标签列表
export const getTagList = (params?: TagListParams) => {
  return httpGet<PaginationResponse<Tag>>('/tag/list', params);
};

// 获取热门标签
export const getHotTags = (limit = 20) => {
  return httpGet<Tag[]>('/tag/hot', { limit });
};

// 获取标签详情
export const getTagById = (id: string) => {
  return httpGet<Tag>(`/tag/${id}`);
};

// 搜索标签
export const searchTagList = (keyword: string) => {
  return httpGet<Tag[]>('/tag/search', { keyword });
};

// 创建新标签
export const createNewTag = (name: string) => {
  return httpPost<Tag>('/tag/create', { name });
};

// 获取标签建议
export const getRecommendTags = (title: string, category: string) => {
  return httpGet<Tag[]>('/tag/recommend', { title, category });
};

// ================== 模拟API实现 ==================

// 模拟获取热门标签
export const mockGetHotTags = (limit = 20) => {
  return Promise.resolve({
    code: 200,
    data: getMockHotTags(limit),
    message: 'success'
  });
};

// 模拟搜索标签
export const mockSearchTags = (keyword: string) => {
  return Promise.resolve({
    code: 200,
    data: searchTags(keyword),
    message: 'success'
  });
};

// 模拟创建标签
export const mockCreateTag = (name: string) => {
  return Promise.resolve({
    code: 200,
    data: createMockTag(name),
    message: 'success'
  });
};

// 模拟获取标签建议
export const mockGetRecommendTags = (title: string, category: string) => {
  return Promise.resolve({
    code: 200,
    data: getTagSuggestions(title, category),
    message: 'success'
  });
};
