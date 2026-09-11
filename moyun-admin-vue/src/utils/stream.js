import { getToken } from '@/utils/auth'
import { tansParams } from '@/utils/ruoyi'
import { ElMessage } from 'element-plus'

const baseURL = import.meta.env.VITE_APP_BASE_API

/**
 * 流式请求（纯 fetch 实现）
 *
 * 说明：此前版本基于 axios + 自定义 adapter 包装 fetch，存在两个缺陷：
 *   1. adapter 外层引用了不存在的 config 变量导致 ReferenceError，请求发不出去；
 *   2. axios transformRequest 已将 data 序列化为 JSON 字符串，adapter 内再次
 *      JSON.stringify 会双重编码，后端收到 JSON 字符串字面量而反序列化失败。
 * 故重构为直接使用 fetch，不再依赖 axios。
 *
 * @param {string} url 请求地址（相对路径自动拼接 baseURL）
 * @param {object} options { method, data, params, onMessage, onDone, onError, headers }
 * @returns {{ abort: Function }} 中断句柄
 */
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

  const isToken = (headers || {}).isToken === false
  if (getToken() && !isToken) {
    requestHeaders['Authorization'] = 'Bearer ' + getToken()
  }

  /**
   * 解析一行流式内容：支持 data: 前缀的 SSE 格式（JSON 或纯文本）与纯文本行
   */
  const dispatchLine = (line) => {
    const trimmed = line.trim()
    if (!trimmed || trimmed === 'data:[DONE]' || trimmed.startsWith('event:')) return

    if (trimmed.startsWith('data:')) {
      const chunk = trimmed.slice(5).trim()
      if (!chunk || chunk === '[DONE]') return
      try {
        const parsed = JSON.parse(chunk)
        const text = typeof parsed === 'string' ? parsed : (parsed.content || parsed.text || parsed.chunk)
        if (onMessage && text) onMessage(text)
      } catch (e) {
        if (onMessage) onMessage(chunk)
      }
    } else if (onMessage) {
      onMessage(trimmed)
    }
  }

  ;(async () => {
    try {
      const response = await fetch(fullUrl, {
        method: method.toUpperCase(),
        headers: requestHeaders,
        body: method.toUpperCase() === 'GET' ? undefined : JSON.stringify(data),
        signal: controller.signal
      })

      if (!response.ok) {
        // 非 2xx：读取后端 JSON 错误信息（如全局异常处理器返回的 msg）
        let detail = `${response.status} ${response.statusText}`
        try {
          const body = await response.text()
          const parsed = JSON.parse(body)
          if (parsed.msg) detail = parsed.msg
        } catch (e) { /* 保留默认信息 */ }
        throw new Error(detail)
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
          dispatchLine(line)
        }
      }

      // 流结束后刷出残留缓冲（最后一段可能没有结尾换行）
      dispatchLine(buffer)

      if (onDone) {
        onDone()
      }
    } catch (err) {
      if (err.name === 'AbortError' || err.message?.includes('aborted') || err.message?.includes('abort')) {
        return
      }
      if (onError) {
        onError(err)
      } else {
        console.error('Stream error:', err)
        ElMessage.error(err.message || '流式请求失败')
      }
    }
  })()

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
