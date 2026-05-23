import { httpGet } from './client';
import type {
  SearchParams,
  SearchResponse,
} from '@/types/api';

// 搜索
export const search = (params: SearchParams) => {
  return httpGet<SearchResponse>('/search', params);
};
