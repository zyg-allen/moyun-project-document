import { showAuthExpiredDialog, handleUnauthorized } from '@/utils/authDialog';

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

// ==================== v10.23：AI 慢请求追踪（离开页面/关页提醒） ====================

/** 进行中的 AI 慢请求 key 集合（key = url|时间戳|随机数，避免同 URL 并发覆盖） */
const pendingAiRequests = new Set<string>();

/**
 * AI 慢接口判定：POST 且会调用 LLM / 大模型（耗时数秒到分钟级）的接口。
 * 提交类快操作（如 /portal/ai/task/submit）与 GET 轮询不在此列。
 */
export function isAiSlowUrl(url: string): boolean {
  // 附件简历上传解析（multipart，上传 + 同步解析落库）
  if (url.includes('/portal/interview/resume/user/parse')) return true;
  // 字段级 AI 实时辅助
  if (url.includes('/portal/resume/optimize/ai-assist')) return true;
  // AI 填充空字段草稿（同步版）
  if (url.includes('/portal/resume/optimize/ai-draft/')) return true;
  // 岗位匹配同步接口（/match/{resumeId}/{jobTargetId}）
  if (url.includes('/portal/resume/optimize/match/')) return true;
  // 深度优化同步接口（排除 /async 提交与 /apply 采纳两个快操作）
  if (url.includes('/portal/resume/optimize/deep/') && !url.endsWith('/async') && !url.endsWith('/apply')) return true;
  // 语音面试：开始面试（LLM 出题）
  if (url.endsWith('/portal/interview/voice/start')) return true;
  // 语音面试：结束面试（LLM 生成报告）
  if (/\/portal\/interview\/voice\/[^/]+\/finish$/.test(url)) return true;
  // 语音面试：提交答案（SSE 流式评分+反馈，经 submitVoiceAnswer 手动登记）
  if (/\/portal\/interview\/voice\/[^/]+\/answer$/.test(url)) return true;
  return false;
}

/** 手动登记一个 AI 慢请求（用于不经 request() 的直连 fetch，如 SSE），返回 key */
export function trackAiSlowRequest(url: string): string {
  const key = `${url}|${Date.now()}|${Math.random().toString(36).slice(2, 8)}`;
  pendingAiRequests.add(key);
  return key;
}

/** 结束 AI 慢请求登记（请求成功/失败都需调用，key 为 trackAiSlowRequest 返回值） */
export function untrackAiSlowRequest(key: string): void {
  pendingAiRequests.delete(key);
}

/** 是否有 AI 慢请求进行中 */
export function hasPendingAiRequests(): boolean {
  return pendingAiRequests.size > 0;
}

/** AI 慢请求数量 */
export function getPendingAiCount(): number {
  return pendingAiRequests.size;
}

// 请求拦截器
const request = async <T>(
    url: string,
    options: RequestInit = {}
): Promise<ApiResponse<T>> => {
  // v10.23：POST 慢接口登记进行中（用于路由离开/关页提醒），finally 统一清理
  const aiTrackKey = (options.method || '').toUpperCase() === 'POST' && isAiSlowUrl(url)
    ? trackAiSlowRequest(url)
    : null;
  try {
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

    // 处理401未授权（HTTP 状态码 401，Spring Security 拦截）
    if (response.status === 401) {
      showAuthExpiredDialog();
      throw new Error('登录已过期，请重新登录');
    }

    let data: BackendResponse<T>;
    try {
      data = await response.json();
    } catch {
      // 非 JSON 响应：典型为代理/后端不可用时返回的纯文本（如 Internal Server Error）
      throw new Error(`服务暂时不可用（HTTP ${response.status}），请稍后重试`);
    }

    // 处理业务 code 401（后端 Controller 主动返回的未登录/登录过期）
    if (data.code === 401) {
      await handleUnauthorized(data.msg);
      throw new Error(data.msg || '请先登录');
    }

    // 转换响应格式：msg -> message
    if (!response.ok || data.code !== 200) {
      throw new Error(data.msg || '请求失败');
    }

    return {
      code: data.code,
      message: data.msg,
      data: data.data as T,
    };
  } finally {
    if (aiTrackKey) {
      untrackAiSlowRequest(aiTrackKey);
    }
  }
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

      // 处理401未授权（HTTP 状态码 401，Spring Security 拦截）
      if (response.status === 401) {
        showAuthExpiredDialog();
        reject(new Error('登录已过期，请重新登录'));
        return;
      }

      const data: BackendResponse<T> = await response.json();

      // 处理业务 code 401（后端 Controller 主动返回的未登录/登录过期）
      if (data.code === 401) {
        await handleUnauthorized(data.msg);
        reject(new Error(data.msg || '请先登录'));
        return;
      }

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
    file: File | FormData,
    extraFields?: Record<string, string>
): Promise<ApiResponse<T>> => {
  const formData = file instanceof FormData ? file : new FormData();
  if (!(file instanceof FormData)) {
    formData.append('file', file);
  }
  if (extraFields) {
    Object.entries(extraFields).forEach(([k, v]) => formData.append(k, v));
  }

  const token = getToken();
  const headers: Record<string, string> = {};

  if (token) {
    headers['Authorization'] = `Bearer ${token}`;
  }

  return fetch(`${API_BASE_URL}${url}`, {
    method: 'POST',
    headers,
    body: formData,
  }).then(async (response) => {
    // 处理401未授权（HTTP 状态码 401，Spring Security 拦截）
    if (response.status === 401) {
      showAuthExpiredDialog();
      throw new Error('登录已过期，请重新登录');
    }

    const data = await response.json() as BackendResponse<T>;

    // 处理业务 code 401（后端 Controller 主动返回的未登录/登录过期）
    if (data.code === 401) {
      await handleUnauthorized(data.msg);
      throw new Error(data.msg || '请先登录');
    }

    return {
      code: data.code,
      message: data.msg,
      data: data.data as T,
    };
  });
};
