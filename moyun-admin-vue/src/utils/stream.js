import axios from 'axios'
import { getToken } from '@/utils/auth'
import errorCode from '@/utils/errorCode'
import { tansParams, blobValidate } from '@/utils/ruoyi'
import cache from '@/plugins/cache'
import { ElMessage } from 'element-plus'

const baseURL = import.meta.env.VITE_APP_BASE_API

export function fetchStream(url, options = {}) {
  const {
    method = 'POST',
    data = {},
    params = {},
    onMessage = null,
    onDone = null,
    onError = null,
    headers = {}
  } = options

  const controller = new AbortController()

  const fullUrl = url.startsWith('http')
    ? url
    : baseURL + url + (Object.keys(params).length === 0 ? '' : '?' + tansParams(params))

  const requestHeaders = {
    'Content-Type': 'application/json',
    ...headers
  }

  const isToken = (config.headers || {}).isToken === false
  if (getToken() && !isToken) {
    requestHeaders['Authorization'] = 'Bearer ' + getToken()
  }

  axios({
    method,
    url: fullUrl,
    data,
    headers: requestHeaders,
    signal: controller.signal,
    responseType: 'text',
    adapter: async (config) => {
      const { data, ...restConfig } = config
      const response = await fetch(fullUrl, {
        method: config.method,
        headers: config.headers,
        body: JSON.stringify(data),
        signal: config.signal
      })

      if (!response.ok) {
        throw new Error(`HTTP ${response.status}: ${response.statusText}`)
      }

      const reader = response.body.getReader()
      const decoder = new TextDecoder('utf-8')
      let buffer = ''

      while (true) {
        const { value, done } = await reader.read()
        if (done) break

        buffer += decoder.decode(value, { stream: true })

        const lines = buffer.split('\n')
        buffer = lines.pop() || ''

        for (const line of lines) {
          const trimmed = line.trim()
          if (!trimmed) continue

          if (trimmed.startsWith('data:')) {
            const chunk = trimmed.slice(5).trim()
            if (chunk === '[DONE]') {
              continue
            }
            try {
              const parsed = JSON.parse(chunk)
              if (parsed.content || parsed.text || parsed.chunk) {
                const text = parsed.content || parsed.text || parsed.chunk
                if (onMessage && text) {
                  onMessage(text)
                }
              } else if (typeof parsed === 'string') {
                if (onMessage) {
                  onMessage(parsed)
                }
              }
            } catch (e) {
              if (onMessage) {
                onMessage(trimmed.startsWith('data:') ? trimmed.slice(5).trim() : trimmed)
              }
            }
          } else if (trimmed.startsWith('event:')) {
            continue
          } else {
            if (onMessage) {
              onMessage(trimmed)
            }
          }
        }
      }

      if (onDone) {
        onDone()
      }

      return {
        data: null,
        status: response.status,
        statusText: response.statusText,
        headers: response.headers,
        config,
        request: {}
      }
    }
  }).catch((err) => {
    if (err.name === 'AbortError' || err.message?.includes('aborted')) {
      return
    }
    if (onError) {
      onError(err)
    } else {
      console.error('Stream error:', err)
      ElMessage.error(err.message || '流式请求失败')
    }
  })

  return {
    abort: () => {
      controller.abort()
    }
  }
}

export default { fetchStream, createAuthEventSource }

/**
 * 创建带鉴权的 EventSource 替代方案
 *
 * 原生 EventSource 无法携带自定义 Authorization header，导致 SSE 接口无法通过 JWT 鉴权。
 * 本函数基于 fetch + ReadableStream 实现，API 兼容 EventSource：
 *   - addEventListener(eventType, handler)：监听命名事件（event: xxx）
 *   - close()：关闭连接
 *   - onerror = fn：连接级错误回调（如 401、网络断开）
 *
 * @param {string} url 请求地址（相对路径会拼接 baseURL）
 * @param {object} options { headers, isToken }
 * @returns {EventSourceLike}
 */
export function createAuthEventSource(url, options = {}) {
  const { headers = {}, isToken = true } = options
  const listeners = {}
  let controller = new AbortController()
  let closed = false
  let onerrorHandler = null

  const on = (eventType, handler) => {
    if (!listeners[eventType]) listeners[eventType] = []
    listeners[eventType].push(handler)
  }

  const dispatch = (eventType, data) => {
    const evt = { data }
    ;(listeners[eventType] || []).forEach((fn) => {
      try {
        fn(evt)
      } catch (e) {
        console.error('[AuthEventSource] 事件处理器错误:', e)
      }
    })
  }

  const triggerConnectionError = (err) => {
    if (onerrorHandler) {
      try {
        onerrorHandler(err)
      } catch (e) {
        console.error('[AuthEventSource] onerror 回调异常:', e)
      }
    }
  }

  const close = () => {
    closed = true
    try {
      controller.abort()
    } catch (e) {
      /* ignore */
    }
  }

  ;(async () => {
    try {
      const token = getToken()
      const fullUrl = url.startsWith('http') ? url : baseURL + url

      const requestHeaders = {
        Accept: 'text/event-stream',
        ...headers
      }
      if (token && isToken) {
        requestHeaders['Authorization'] = 'Bearer ' + token
      }

      const response = await fetch(fullUrl, {
        method: 'GET',
        headers: requestHeaders,
        signal: controller.signal
      })

      if (!response.ok) {
        triggerConnectionError(new Error(`HTTP ${response.status}: ${response.statusText}`))
        return
      }

      const reader = response.body.getReader()
      const decoder = new TextDecoder('utf-8')
      let buffer = ''

      while (!closed) {
        const { done, value } = await reader.read()
        if (done) break

        buffer += decoder.decode(value, { stream: true })

        // SSE 事件块之间用空行分隔
        const events = buffer.split('\n\n')
        buffer = events.pop() || ''

        for (const block of events) {
          if (!block.trim()) continue

          let eventType = 'message'
          const dataLines = []

          for (const line of block.split('\n')) {
            if (line.startsWith('event:')) {
              eventType = line.slice(6).trim()
            } else if (line.startsWith('data:')) {
              // 保留 data: 后内容（仅去掉一个前导空格，符合 SSE 规范）
              dataLines.push(line.slice(5).replace(/^\s/, ''))
            }
          }

          dispatch(eventType, dataLines.join('\n'))
        }
      }
    } catch (err) {
      if (err.name === 'AbortError' || closed) return
      triggerConnectionError(err)
    }
  })()

  const obj = {
    addEventListener: on,
    close
  }
  Object.defineProperty(obj, 'onerror', {
    set(fn) {
      onerrorHandler = fn
    },
    get() {
      return onerrorHandler
    }
  })
  return obj
}
