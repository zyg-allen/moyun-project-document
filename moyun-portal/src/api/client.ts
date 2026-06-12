// 后台响应类型
interface BackendResponse<T = any> {
  code: number;
  msg: string;
  data?: T;
  rows?: T[];
  total?: number;
}

export interface ApiResponse<T = any> {
  code: number;
  message: string;
  data: T;
}

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
    ...(options.headers as Record<string, string> || {}),
  };

  // 只有当不是 FormData 时才设置 Content-Type
  if (!(options.body instanceof FormData)) {
    headers['Content-Type'] = 'application/json';
  }

  if (token) {
    headers['Authorization'] = `Bearer ${token}`;
  }

  const response = await fetch(`${API_BASE_URL}${url}`, {
    ...options,
    headers,
  });

  // 处理401未授权
  if (response.status === 401) {
    removeToken();
    window.location.href = '/login';
    throw new Error('登录已过期，请重新登录');
  }

  const data: BackendResponse<T> = await response.json();

  // 转换响应格式：msg -> message
  if (!response.ok || data.code !== 200) {
    throw new Error(data.msg || '请求失败');
  }

  return {
    code: data.code,
    message: data.msg,
    data: data.data as T,
  };
};

// 分页响应格式
export interface PaginationResponse<T> {
  list: T[];
  total: number;
  page: number;
  pageSize: number;
}

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

// 获取分页数据的专用方法
export const httpGetList = <T>(
  url: string,
  params?: Record<string, any>
): Promise<ApiResponse<PaginationResponse<T>>> => {
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
  
  // 这里直接处理分页响应
  return new Promise(async (resolve, reject) => {
    try {
      const token = getToken();
      const headers: Record<string, string> = {
        'Content-Type': 'application/json',
      };

      if (token) {
        headers['Authorization'] = `Bearer ${token}`;
      }

      const response = await fetch(`${API_BASE_URL}${url}${query}`, {
        method: 'GET',
        headers,
      });

      const data: BackendResponse<T> = await response.json();

      if (!response.ok || data.code !== 200) {
        reject(new Error(data.msg || '请求失败'));
        return;
      }

      // 兼容后端两种返回格式：
      // 1. TableDataInfo格式（后台）：rows, total
      // 2. MyBatis-Plus Page对象（前台）：records, total, current, size, pages
      const pageData = (data as any).data || {};
      const list = pageData.records || pageData.rows || pageData.list || [];
      const total = pageData.total || 0;
      const current = pageData.current || pageData.page || params?.page || 1;
      const size = pageData.size || pageData.pageSize || params?.pageSize || 10;

      resolve({
        code: data.code,
        message: data.msg,
        data: {
          list: list as T[],
          total: total,
          page: current,
          pageSize: size,
        },
      });
    } catch (error) {
      reject(error);
    }
  });
};

export const httpPost = <T>(
  url: string,
  data?: Record<string, any> | FormData
): Promise<ApiResponse<T>> => {
  return request<T>(url, {
    method: 'POST',
    body: data instanceof FormData ? data : (data ? JSON.stringify(data) : undefined),
  });
};

export const httpPut = <T>(
  url: string,
  data?: Record<string, any> | FormData
): Promise<ApiResponse<T>> => {
  return request<T>(url, {
    method: 'PUT',
    body: data instanceof FormData ? data : (data ? JSON.stringify(data) : undefined),
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
  }).then((response) => response.json() as Promise<ApiResponse<T>>);
};
