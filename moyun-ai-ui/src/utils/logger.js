/**
 * 日志工具模块
 * 生产环境自动禁用调试日志
 */

const IS_DEV = import.meta.env.DEV

/**
 * 调试日志（仅开发环境输出）
 */
export const logger = {
  log: (...args) => {
    if (IS_DEV) console.log(...args)
  },
  
  warn: (...args) => {
    if (IS_DEV) console.warn(...args)
  },
  
  error: (...args) => {
    // 错误日志始终输出
    console.error(...args)
  },
  
  info: (...args) => {
    if (IS_DEV) console.info(...args)
  },
  
  debug: (...args) => {
    if (IS_DEV) console.debug(...args)
  },
  
  // 分组日志
  group: (label) => {
    if (IS_DEV) console.group(label)
  },
  
  groupEnd: () => {
    if (IS_DEV) console.groupEnd()
  },
  
  // 表格日志
  table: (data) => {
    if (IS_DEV) console.table(data)
  }
}

export default logger
