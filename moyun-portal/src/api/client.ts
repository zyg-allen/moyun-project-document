import type { ApiResponse } from '@/types/api';

const API_BASE_URL = import.meta.env.VITE_API_BASE_URL || '/api';

// Token管理
const TOKEN_KEY = 'moyun_token';
const REFRESH_TOKEN_KEY = 'moyun_refresh_token';

export const getToken = (): string | null => {
  return localStorage.getItem(TOKEN_KEY);
};

export const setToken = (token: string): void => {
  localStorage.setItem(TOKEN_KEY, token);
};

export const removeToken = (): void => {
  localStorage.removeItem(TOKEN_KEY);
};

export const getRefreshToken = (): string | null => {
  return localStorage.getItem(REFRESH_TOKEN_KEY);
};

export const setRefreshToken = (token: string): void => {
  localStorage.setItem(REFRESH_TOKEN_KEY, token);
};

export const removeRefreshToken = (): void => {
  localStorage.removeItem(REFRESH_TOKEN_KEY);
};

// 请求拦截器
const request = async <T>(
  url: string,
  options: RequestInit = {}
): Promise<ApiResponse<T>> => {
  const token = getToken();
  const headers: Record<string, string> = {
    'Content-Type': 'application/json',
    ...(options.headers as Record<string, string> || {}),
  };

  if (token) {
    headers['Authorization'] = `Bearer ${token}`;
  }

  const response = await fetch(`${API_BASE_URL}${url}`, {
    ...options,
    headers,
  });

  const data: ApiResponse<T> = await response.json();

  if (!response.ok || data.code !== 200) {
    throw new Error(data.message || '请求失败');
  }

  return data;
};

// HTTP方法
export const httpGet = <T>(
  url: string,
  params?: Record<string, any>
): Promise<ApiResponse<T>> => {
  let query = '';
  if (params) {
    const searchParams = new URLSearchParams();
    Object.entries(params).forEach(([key, value]) => {
      if (value !== undefined && value !== null) {
        searchParams.append(key, String(value));
      }
    });
    query = `?${searchParams.toString()}`;
  }
  return request<T>(`${url}${query}`, {
    method: 'GET',
  });
};

export const httpPost = <T>(
  url: string,
  data?: Record<string, any>
): Promise<ApiResponse<T>> => {
  return request<T>(url, {
    method: 'POST',
    body: data ? JSON.stringify(data) : undefined,
  });
};

export const httpPut = <T>(
  url: string,
  data?: Record<string, any>
): Promise<ApiResponse<T>> => {
  return request<T>(url, {
    method: 'PUT',
    body: data ? JSON.stringify(data) : undefined,
  });
};

export const httpDelete = <T>(
  url: string
): Promise<ApiResponse<T>> => {
  return request<T>(url, {
    method: 'DELETE',
  });
};

export const httpUpload = <T>(
  url: string,
  file: File
): Promise<ApiResponse<T>> => {
  const formData = new FormData();
  formData.append('file', file);

  const token = getToken();
  const headers: Record<string, string> = {};
  
  if (token) {
    headers['Authorization'] = `Bearer ${token}`;
  }

  return fetch(`${API_BASE_URL}${url}`, {
    method: 'POST',
    headers,
    body: formData,
  }).then((response) => response.json());
};
