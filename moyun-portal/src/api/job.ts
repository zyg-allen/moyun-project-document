import { httpGet, httpGetList } from './client';
import type {
  JobVO,
  JobQuery,
  JobListItemVO,
} from '@/types/api';

/**
 * 职位列表（公开，分页）
 * GET /portal/job/list
 */
export const getJobList = (params?: JobQuery) => {
  return httpGetList<JobListItemVO>('/portal/job/list', params);
};

/**
 * 职位详情（公开）
 * GET /portal/job/{id}
 */
export const getJobDetail = (id: string | number) => {
  return httpGet<JobVO>(`/portal/job/${id}`);
};
