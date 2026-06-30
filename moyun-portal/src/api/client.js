import { showAuthExpiredDialog, handleUnauthorized } from '@/utils/authDialog';
const API_BASE_URL = import.meta.env.VITE_API_BASE_URL || '/api';
// Token管理
const TOKEN_KEY = 'moyun_token';
const REFRESH_TOKEN_KEY = 'moyun_refresh_token';
export const getToken = () => {
    return localStorage.getItem(TOKEN_KEY);
};
export const setToken = (token) => {
    localStorage.setItem(TOKEN_KEY, token);
};
export const removeToken = () => {
    localStorage.removeItem(TOKEN_KEY);
};
export const getRefreshToken = () => {
    return localStorage.getItem(REFRESH_TOKEN_KEY);
};
export const setRefreshToken = (token) => {
    localStorage.setItem(REFRESH_TOKEN_KEY, token);
};
export const removeRefreshToken = () => {
    localStorage.removeItem(REFRESH_TOKEN_KEY);
};
// 请求拦截器
const request = async (url, options = {}) => {
    const token = getToken();
    const headers = {
        ...(options.headers || {}),
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
    const data = await response.json();
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
        data: data.data,
    };
};
// HTTP方法
export const httpGet = (url, params) => {
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
    return request(`${url}${query}`, {
        method: 'GET',
    });
};
// 获取分页数据的专用方法
export const httpGetList = (url, params) => {
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
            const headers = {
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
            const data = await response.json();
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
            const pageData = data.data || {};
            const list = pageData.records || pageData.rows || pageData.list || [];
            const total = pageData.total || 0;
            const current = pageData.current || pageData.page || params?.page || 1;
            const size = pageData.size || pageData.pageSize || params?.pageSize || 10;
            resolve({
                code: data.code,
                message: data.msg,
                data: {
                    list: list,
                    total: total,
                    page: current,
                    pageSize: size,
                },
            });
        }
        catch (error) {
            reject(error);
        }
    });
};
export const httpPost = (url, data) => {
    return request(url, {
        method: 'POST',
        body: data instanceof FormData ? data : (data ? JSON.stringify(data) : undefined),
    });
};
export const httpPut = (url, data) => {
    return request(url, {
        method: 'PUT',
        body: data instanceof FormData ? data : (data ? JSON.stringify(data) : undefined),
    });
};
export const httpDelete = (url) => {
    return request(url, {
        method: 'DELETE',
    });
};
export const httpUpload = (url, file) => {
    const formData = new FormData();
    formData.append('file', file);
    const token = getToken();
    const headers = {};
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
        const data = await response.json();
        // 处理业务 code 401（后端 Controller 主动返回的未登录/登录过期）
        if (data.code === 401) {
            await handleUnauthorized(data.msg);
            throw new Error(data.msg || '请先登录');
        }
        return {
            code: data.code,
            message: data.msg,
            data: data.data,
        };
    });
};
