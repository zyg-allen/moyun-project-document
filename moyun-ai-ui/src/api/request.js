/**
 * 统一请求模块
 * 
 * 封装axios，提供统一的请求方法、错误处理和响应拦截
 */
import axios from 'axios'
import { ElMessage } from 'element-plus'
import router from '@/router'

// 创建axios实例
const service = axios.create({
  baseURL: import.meta.env.VITE_API_BASE_URL || '/api',
  timeout: 60000, // 请求超时时间
  headers: {
    'Content-Type': 'application/json'
  }
})

// 请求拦截器
service.interceptors.request.use(
  (config) => {
    // 添加token
    const token = localStorage.getItem('token')
    if (token) {
      config.headers['Authorization'] = token
    }
    return config
  },
  (error) => {
    console.error('请求错误:', error)
    return Promise.reject(error)
  }
)

// 响应拦截器
service.interceptors.response.use(
  (response) => {
    const res = response.data
    
    // 如果返回的是文件流，直接返回
    if (response.config.responseType === 'blob') {
      return response
    }
    
    // 业务错误处理
    if (res.success === false) {
      ElMessage.error(res.message || '操作失败')
      return Promise.reject(new Error(res.message || '操作失败'))
    }
    
    return res
  },
  (error) => {
    // HTTP错误处理
    if (error.response) {
      const status = error.response.status
      
      switch (status) {
        case 401:
          // 未授权，清除token并跳转登录
          localStorage.removeItem('token')
          localStorage.removeItem('username')
          localStorage.removeItem('nickname')
          ElMessage.warning('登录已过期，请重新登录')
          router.push('/login')
          break
        case 403:
          ElMessage.error('没有权限访问该资源')
          break
        case 404:
          ElMessage.error('请求的资源不存在')
          break
        case 500:
          ElMessage.error('服务器内部错误')
          break
        default:
          ElMessage.error(error.response.data?.message || `请求失败(${status})`)
      }
    } else if (error.request) {
      ElMessage.error('无法连接到服务器，请检查网络')
    } else {
      ElMessage.error('请求配置错误')
    }
    
    return Promise.reject(error)
  }
)

// 导出请求方法
export default service

// 便捷方法
export const get = (url, params, config = {}) => {
  return service.get(url, { params, ...config })
}

export const post = (url, data, config = {}) => {
  return service.post(url, data, config)
}

export const put = (url, data, config = {}) => {
  return service.put(url, data, config)
}

export const del = (url, config = {}) => {
  return service.delete(url, config)
}

// 文件上传
export const upload = (url, file, onProgress) => {
  const formData = new FormData()
  formData.append('file', file)
  
  return service.post(url, formData, {
    headers: {
      'Content-Type': 'multipart/form-data'
    },
    onUploadProgress: (progressEvent) => {
      if (onProgress) {
        const percent = Math.round((progressEvent.loaded * 100) / progressEvent.total)
        onProgress(percent)
      }
    }
  })
}

// 文件下载
export const download = (url, params) => {
  return service.get(url, {
    params,
    responseType: 'blob'
  })
}
