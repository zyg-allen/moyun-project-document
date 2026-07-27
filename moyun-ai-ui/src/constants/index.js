/**
 * 系统常量定义
 */

// 模型提供商（与后端 ModelConfigServiceImpl 保持一致）
export const MODEL_PROVIDERS = {
  OPENAI: 'openai',
  OLLAMA: 'ollama',
  DASHSCOPE: 'dashscope'  // 通义千问
}

// 模型类型（只有 chat 和 embedding 两种，多模态模型属于 chat 类型）
export const MODEL_TYPES = {
  CHAT: 'chat',
  EMBEDDING: 'embedding'
}

// 文件类型
export const FILE_TYPES = {
  PDF: 'pdf',
  WORD: ['doc', 'docx'],
  EXCEL: ['xls', 'xlsx'],
  PPT: ['ppt', 'pptx'],
  TEXT: ['txt', 'md'],
  CSV: 'csv'
}

// 支持的文件扩展名
export const SUPPORTED_EXTENSIONS = [
  '.pdf', '.doc', '.docx', 
  '.xls', '.xlsx', '.ppt', '.pptx',
  '.txt', '.md', '.csv'
]

// 文件大小限制 (字节)
export const FILE_SIZE_LIMITS = {
  DEFAULT: 50 * 1024 * 1024,  // 50MB
  IMAGE: 10 * 1024 * 1024,    // 10MB
  DOCUMENT: 100 * 1024 * 1024 // 100MB
}

// 数据源类型
export const DATASOURCE_TYPES = {
  MYSQL: 'mysql',
  ELASTICSEARCH: 'elasticsearch',
  POSTGRESQL: 'postgresql'
}

// 工作流节点类型
export const WORKFLOW_NODE_TYPES = {
  START: 'start',
  END: 'end',
  LLM: 'llm',
  CONDITION: 'condition',
  LOOP: 'loop',
  TOOL: 'tool',
  HTTP: 'http',
  CODE: 'code',
  KNOWLEDGE: 'knowledge',
  AGENT: 'agent',
  PARALLEL: 'parallel',
  MERGE: 'merge'
}

// 工作流执行状态
export const WORKFLOW_STATUS = {
  PENDING: 'pending',
  RUNNING: 'running',
  COMPLETED: 'completed',
  FAILED: 'failed',
  CANCELLED: 'cancelled'
}

// 消息类型
export const MESSAGE_TYPES = {
  USER: 'user',
  ASSISTANT: 'assistant',
  SYSTEM: 'system'
}

// 分页默认配置
export const PAGINATION = {
  DEFAULT_PAGE: 1,
  DEFAULT_SIZE: 10,
  PAGE_SIZES: [10, 20, 50, 100]
}

// 本地存储键
export const STORAGE_KEYS = {
  TOKEN: 'token',
  USERNAME: 'username',
  NICKNAME: 'nickname',
  THEME: 'theme',
  LANGUAGE: 'language'
}

// API响应码
export const RESPONSE_CODES = {
  SUCCESS: 0,
  UNAUTHORIZED: 401,
  FORBIDDEN: 403,
  NOT_FOUND: 404,
  SERVER_ERROR: 500
}

// 正则表达式
export const REGEX = {
  EMAIL: /^[^\s@]+@[^\s@]+\.[^\s@]+$/,
  PHONE: /^1[3-9]\d{9}$/,
  URL: /^https?:\/\/.+/,
  IP: /^(\d{1,3}\.){3}\d{1,3}$/
}
