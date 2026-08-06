/**
 * 架构图常量配置
 * 统一管理布局参数、颜色、图标等配置
 */

// ==================== 布局配置 ====================

/**
 * 企业级风格布局参数
 */
export const LAYOUT_CONFIG = {
  // 左侧标签
  labelWidth: 85,
  labelGap: 2,
  
  // 节点尺寸
  nodeHeight: 24,
  nodeGapX: 6,
  nodeGapY: 4,
  
  // 层级配置
  layerPaddingX: 8,
  layerPaddingY: 8,
  layerGap: 3,
  maxRowWidth: 700,
  
  // Block 配置
  blockGapX: 8,
  blockPaddingX: 6,
  blockPaddingY: 5,
  blockTitleH: 18,
  blockMaxRowWidth: 220,
  
  // 侧边栏配置
  sidebarWidth: 100,
  sidebarNodeH: 36,
  sidebarNodeGap: 2,
  sidebarPadding: 4,
  sidebarTitleH: 28,
}

/**
 * 普通风格布局参数
 */
export const LAYOUT_CONFIG_NORMAL = {
  canvasWidth: 900,
  layerPadding: 25,
  nodeH: 40,
  nodeGapX: 12,
  nodeGapY: 8,
  maxRowWidth: 700,
  layerGap: 30,
}

/**
 * 组织架构图布局参数
 */
export const LAYOUT_CONFIG_ORG = {
  nodeWidth: 85,
  nodeHeight: 50,
  minNodeSpace: 120,
  gapY: 80,
}

// ==================== 颜色配置 ====================

/**
 * 企业级统一配色 - 蓝色调
 */
export const COLORS_ENTERPRISE = {
  layerBg: '#F0F7FF',
  layerBorder: '#ADC6FF',
  labelBg: '#1890FF',
  labelText: '#FFFFFF',
  blockBg: '#FFFFFF',
  blockBorder: '#E8E8E8',
  blockTitleBg: {
    blue: '#1890FF',
    green: '#52C41A',
    yellow: '#FAAD14',
    purple: '#722ED1',
    pink: '#EB2F96',
    gray: '#8C8C8C',
    orange: '#FA8C16',
    cyan: '#13C2C2',
    red: '#F5222D',
  },
  blockTitleText: '#FFFFFF',
  nodeBg: '#FFFFFF',
  nodeBorder: '#D9D9D9',
  nodeText: '#333333',
  sidebarBg: '#E6F4FF',
  sidebarBorder: '#91CAFF',
  sidebarNodeBg: '#1890FF',
  sidebarNodeText: '#FFFFFF',
}

/**
 * 普通风格分层颜色
 */
export const LAYER_COLORS_NORMAL = [
  { border: '#42A5F5', title: '#1976D2' },
  { border: '#66BB6A', title: '#388E3C' },
  { border: '#FFCA28', title: '#F9A825' },
  { border: '#66BB6A', title: '#388E3C' },
  { border: '#EF5350', title: '#D32F2F' },
  { border: '#AB47BC', title: '#7B1FA2' },
]

/**
 * 流程图阶段颜色
 */
export const FLOW_STAGE_COLORS = [
  '#1976D2', '#388E3C', '#F57C00', '#C2185B', '#7B1FA2', '#0097A7'
]

/**
 * Block 自动分配颜色
 */
export const BLOCK_AUTO_COLORS = [
  'blue', 'green', 'orange', 'purple', 'cyan', 'pink'
]

/**
 * 有效颜色列表（用于数据校验）
 */
export const VALID_COLORS = [
  'blue', 'green', 'yellow', 'purple', 'pink', 'gray', 'orange', 'cyan', 'red'
]

// ==================== 图标配置 ====================

/**
 * 图标映射 - 语义化图标
 */
export const ICON_MAP = {
  // ===== 用户/角色 =====
  user: '👤',
  admin: '👨‍💻',
  operator: '👷',
  developer: '💻',
  merchant: '🏪',
  channel: '📡',
  finance: '💰',
  ops: '🔧',
  
  // ===== 终端/客户端 =====
  browser: '🖥️',
  mobile: '📱',
  wechat: '🟢',
  terminal: '⌨️',
  
  // ===== 网关/入口 =====
  gateway: '🚪',
  nginx: '🔀',
  api: '🔗',
  
  // ===== 认证/安全 =====
  auth: '🔐',
  security: '🛡️',
  
  // ===== 服务/微服务 =====
  server: '🖧',
  service: '⚙️',
  config: '🔧',
  registry: '📍',
  
  // ===== AI/智能 =====
  ai: '🤖',
  chat: '💬',
  bot: '🤖',
  knowledge: '📚',
  intent: '🎯',
  memory: '🧠',
  tool: '🔧',
  
  // ===== 数据处理 =====
  search: '🔍',
  analytics: '📊',
  etl: '🔄',
  
  // ===== 存储 =====
  database: '🗃️',
  mysql: '🗃️',
  redis: '⚡',
  cache: '⚡',
  elasticsearch: '🔎',
  storage: '💾',
  file: '📁',
  oss: '☁️',
  zk: '🔗',
  
  // ===== 消息队列 =====
  mq: '📬',
  kafka: '📬',
  rabbitmq: '🐰',
  
  // ===== 监控/日志 =====
  monitor: '📈',
  log: '📝',
  alert: '🔔',
  
  // ===== 容器/部署 =====
  docker: '🐳',
  k8s: '☸️',
  cloud: '☁️',
  
  // ===== 业务流程 =====
  start: '▶️',
  end: '⏹️',
  decision: '❓',
  process: '▶️',
  input: '📥',
  output: '📤',
  check: '✅',
  error: '❌',
  wait: '⏳',
  loop: '🔄',
  
  // ===== 业务 =====
  order: '🛒',
  product: '🏷️',
  pay: '💳',
  inventory: '📦',
  
  // ===== 通用 =====
  module: '📦',
  component: '🧩',
  default: '◼️',
  
  // ===== 组织架构 =====
  ceo: '👔',
  cto: '💻',
  cpo: '📱',
  cmo: '📢',
  coo: '⚙️',
  cfo: '💰',
  director: '👤',
  manager: '👥',
  leader: '🎯',
  member: '👤',
  team: '👥',
  dept: '🏢',
}

/**
 * 图标颜色映射
 */
export const ICON_COLOR_MAP = {
  // 组织架构
  ceo: '#D32F2F',
  cto: '#1976D2',
  cpo: '#7B1FA2',
  cmo: '#F57C00',
  coo: '#388E3C',
  cfo: '#FBC02D',
  director: '#5D4037',
  manager: '#0097A7',
  leader: '#7CB342',
  member: '#78909C',
  team: '#AB47BC',
  dept: '#5C6BC0',
  
  // 通用
  user: '#607D8B',
  admin: '#455A64',
  ai: '#00C853',
  bot: '#8BC34A',
  database: '#3498DB',
  redis: '#E74C3C',
  server: '#3F51B5',
  service: '#6C5CE7',
  
  // 默认
  default: '#667eea',
}

/**
 * Draw.io 节点样式映射（带颜色和图标）
 */
export const STYLE_MAP = {
  // 用户相关
  user: { color: '#FF6B6B', icon: '👤' },
  admin: { color: '#E91E63', icon: '👨‍💼' },
  operator: { color: '#9C27B0', icon: '👷' },
  
  // 终端设备
  browser: { color: '#4ECDC4', icon: '🌐' },
  mobile: { color: '#45B7D1', icon: '📱' },
  wechat: { color: '#07C160', icon: '💬' },
  terminal: { color: '#607D8B', icon: '💻' },
  
  // 网关入口
  gateway: { color: '#FF9F43', icon: '🚪' },
  nginx: { color: '#009688', icon: '⚖️' },
  api: { color: '#A29BFE', icon: '🔌' },
  
  // 安全认证
  auth: { color: '#EE5A24', icon: '🔐' },
  security: { color: '#F44336', icon: '🛡️' },
  
  // 服务组件
  server: { color: '#3F51B5', icon: '🖥️' },
  service: { color: '#6C5CE7', icon: '⚙️' },
  config: { color: '#FF5722', icon: '⚙️' },
  registry: { color: '#795548', icon: '📋' },
  
  // AI 智能
  ai: { color: '#00C853', icon: '🤖' },
  chat: { color: '#00BCD4', icon: '💭' },
  bot: { color: '#8BC34A', icon: '🤖' },
  knowledge: { color: '#9C27B0', icon: '📚' },
  intent: { color: '#673AB7', icon: '🎯' },
  memory: { color: '#3F51B5', icon: '🧠' },
  tool: { color: '#FF9800', icon: '🔧' },
  
  // 数据处理
  search: { color: '#FFC107', icon: '🔍' },
  analytics: { color: '#E91E63', icon: '📊' },
  etl: { color: '#9E9E9E', icon: '🔄' },
  
  // 存储
  database: { color: '#3498DB', icon: '🗄️' },
  redis: { color: '#E74C3C', icon: '⚡' },
  elasticsearch: { color: '#FDD835', icon: '🔎' },
  file: { color: '#8BC34A', icon: '📁' },
  oss: { color: '#27AE60', icon: '📦' },
  storage: { color: '#78909C', icon: '💾' },
  
  // 消息队列
  mq: { color: '#9B59B6', icon: '📨' },
  kafka: { color: '#E67E22', icon: '📊' },
  rabbitmq: { color: '#FF6F00', icon: '🐰' },
  
  // 监控运维
  monitor: { color: '#E91E63', icon: '📈' },
  log: { color: '#00BCD4', icon: '📝' },
  alert: { color: '#F44336', icon: '🚨' },
  
  // 容器部署
  k8s: { color: '#326CE5', icon: '☸️' },
  docker: { color: '#2496ED', icon: '🐳' },
  cloud: { color: '#03A9F4', icon: '☁️' },
  
  // 业务流程
  start: { color: '#4CAF50', icon: '▶️' },
  end: { color: '#F44336', icon: '⏹️' },
  decision: { color: '#FF9800', icon: '❓' },
  process: { color: '#2196F3', icon: '⚙️' },
  input: { color: '#9C27B0', icon: '📥' },
  output: { color: '#00BCD4', icon: '📤' },
  check: { color: '#8BC34A', icon: '✅' },
  error: { color: '#F44336', icon: '❌' },
  wait: { color: '#607D8B', icon: '⏳' },
  loop: { color: '#FF5722', icon: '🔄' },
  
  // 业务
  order: { color: '#FF4081', icon: '📋' },
  product: { color: '#7C4DFF', icon: '📦' },
  pay: { color: '#FFD600', icon: '💳' },
  inventory: { color: '#4CAF50', icon: '📊' },
  
  // 通用
  module: { color: '#607D8B', icon: '📦' },
  component: { color: '#9E9E9E', icon: '🧩' },
  
  // 组织架构
  ceo: { color: '#D32F2F', icon: '👔' },
  cto: { color: '#1976D2', icon: '💻' },
  cpo: { color: '#7B1FA2', icon: '📱' },
  cmo: { color: '#F57C00', icon: '📢' },
  coo: { color: '#388E3C', icon: '⚙️' },
  cfo: { color: '#FBC02D', icon: '💰' },
  director: { color: '#5D4037', icon: '👤' },
  manager: { color: '#0097A7', icon: '👥' },
  leader: { color: '#7CB342', icon: '🎯' },
  member: { color: '#78909C', icon: '👤' },
  team: { color: '#AB47BC', icon: '👥' },
  dept: { color: '#5C6BC0', icon: '🏢' },
}

/**
 * 默认样式
 */
export const DEFAULT_STYLE = { color: '#6C5CE7', icon: '📦' }

// ==================== 数据校验配置 ====================

/**
 * 数据校验配置（专业版 v3）
 * 支持详细专业的技术架构，不截断标签
 */
export const VALIDATION_CONFIG = {
  // 标签长度限制（增大以避免截断）
  maxLabelLength: 20,         // 节点标签最大长度（不截断）
  maxSidebarLabelLength: 12,  // 侧边栏节点标签长度
  maxLayerNameLength: 12,     // 层名称长度
  maxBlockNameLength: 15,     // Block 名称长度
  maxTitleLength: 30,         // 标题最大长度
  maxEdgeLabelLength: 10,     // 边标签最大长度
  
  // 数量限制（支持详细架构）
  maxNodesPerLayer: 12,       // 每层最大节点数
  maxNodesPerBlock: 8,        // 每个 Block 最大节点数
  maxSidebarNodes: 10,        // 侧边栏最大节点数
  maxLayers: 10,              // 最大层数
  maxBlocksPerLayer: 6,       // 每层最大 Block 数
  maxEdges: 20,               // 最大边数
  maxTotalNodes: 100,         // 总节点数限制
}

// ==================== 数据流配置 ====================

/**
 * Edge 类型配置
 */
export const EDGE_TYPES = {
  sync: { color: '#1890FF', style: 'solid', label: '同步' },
  async: { color: '#52C41A', style: 'dashed', label: '异步' },
  event: { color: '#FA8C16', style: 'dotted', label: '事件' },
}

/**
 * 图例配置
 */
export const LEGEND_CONFIG = {
  enabled: true,
  position: 'bottom-right', // bottom-right, bottom-left, top-right, top-left
  showDescription: true,
  description: '系统逻辑架构图',
  items: [
    { type: 'edge', key: 'sync', label: '同步调用', color: '#1890FF', style: 'solid' },
    { type: 'edge', key: 'async', label: '异步/消息', color: '#52C41A', style: 'dashed' },
    { type: 'edge', key: 'event', label: '事件触发', color: '#FA8C16', style: 'dotted' },
  ]
}

/**
 * 颜色语义配置（用于说明）
 */
export const COLOR_SEMANTICS = {
  接入层: { color: '#52C41A', label: '绿色 - 接入组件' },
  应用层: { color: '#1890FF', label: '蓝色 - 业务应用' },
  能力层: { color: '#FA8C16', label: '橙色 - 核心能力' },
  模型层: { color: '#722ED1', label: '紫色 - AI模型' },
  数据层: { color: '#8C8C8C', label: '灰色 - 基础设施' },
  监控: { color: '#1890FF', label: '蓝色 - 可观测性' },
  治理: { color: '#FA8C16', label: '橙色 - 服务治理' },
}

/**
 * 术语纠正映射
 */
export const TERM_CORRECTIONS = {
  '模数层': '模型层',
  'Webnook': 'Webhook',
  'webnook': 'Webhook',
  'Elastics.': 'Elasticsearch',
  'Elastics': 'Elasticsearch',
  '智能体引脚': 'Agent调用',
  '插件端': '插件扩展',
  '模数': '模型',
  '接口层': '接入层',
  '引擎层': '服务层',
}

// ==================== Draw.io 颜色映射 ====================

/**
 * Draw.io 统一颜色映射
 * 用于节点、Block、层级等元素的颜色配置
 */
export const DRAWIO_COLOR_MAP = {
  blue: { bg: '#E6F7FF', border: '#69C0FF', node: '#1890FF', text: '#FFFFFF' },
  purple: { bg: '#F9F0FF', border: '#B37FEB', node: '#722ED1', text: '#FFFFFF' },
  orange: { bg: '#FFF7E6', border: '#FFD591', node: '#FA8C16', text: '#FFFFFF' },
  yellow: { bg: '#FFFBE6', border: '#FFE58F', node: '#FAAD14', text: '#000000' },
  green: { bg: '#F6FFED', border: '#95DE64', node: '#52C41A', text: '#FFFFFF' },
  pink: { bg: '#FFF0F6', border: '#FFADD2', node: '#EB2F96', text: '#FFFFFF' },
  gray: { bg: '#FAFAFA', border: '#D9D9D9', node: '#8C8C8C', text: '#FFFFFF' },
  cyan: { bg: '#E6FFFB', border: '#87E8DE', node: '#13C2C2', text: '#FFFFFF' },
  red: { bg: '#FFF1F0', border: '#FFA39E', node: '#F5222D', text: '#FFFFFF' },
}

/**
 * 获取 Draw.io 颜色配置
 * @param {string} color - 颜色名称
 * @returns {Object} 颜色配置对象
 */
export function getDrawioColor(color) {
  return DRAWIO_COLOR_MAP[color] || DRAWIO_COLOR_MAP.gray
}
