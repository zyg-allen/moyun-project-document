/**
 * 架构图渲染器 - 将 JSON 数据转换为 Draw.io XML
 * 参考 ai-process-admin 项目实现
 */

// ==================== 常量配置 ====================

// 普通风格层颜色
const LAYER_COLORS_NORMAL = [
  { border: '#42A5F5', title: '#1976D2' },
  { border: '#66BB6A', title: '#388E3C' },
  { border: '#FFCA28', title: '#F9A825' },
  { border: '#AB47BC', title: '#7B1FA2' },
  { border: '#EF5350', title: '#D32F2F' },
  { border: '#FF7043', title: '#E64A19' },
]

// 企业级风格颜色
const COLORS_ENTERPRISE = {
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
  },
  nodeBg: '#FFFFFF',
  nodeBorder: '#D9D9D9',
  nodeText: '#333333',
  sidebarBg: '#E6F4FF',
  sidebarBorder: '#91CAFF',
  sidebarNodeBg: '#1890FF',
  sidebarNodeText: '#FFFFFF',
}

// ==================== 工具函数 ====================

// 用于生成唯一 ID 的计数器
let nodeIdCounter = 1

/**
 * 确保节点有有效的 ID
 */
function ensureNodeId(node, prefix = 'node') {
  if (!node.id || node.id === 'undefined') {
    node.id = `${prefix}-${nodeIdCounter++}`
  }
  return node.id
}

/**
 * 重置 ID 计数器（每次渲染前调用）
 */
function resetNodeIdCounter() {
  nodeIdCounter = 1
}

/**
 * XML 特殊字符转义
 */
export function escapeXml(text) {
  if (!text) return ''
  return String(text)
    .replace(/&/g, '&amp;')
    .replace(/</g, '&lt;')
    .replace(/>/g, '&gt;')
    .replace(/"/g, '&quot;')
    .replace(/'/g, '&#39;')
}

/**
 * 计算节点宽度（根据文本内容自适应）
 */
function calcNodeWidth(label, minWidth = 55, padding = 12) {
  let textWidth = 0
  for (const char of (label || '')) {
    textWidth += /[\u4e00-\u9fa5]/.test(char) ? 11 : 5.5
  }
  return Math.max(minWidth, textWidth + padding)
}

/**
 * 将节点按行分组
 */
function groupNodesIntoRows(nodes, maxRowWidth, nodeGapX = 12) {
  const nodeWidths = nodes.map(n => calcNodeWidth(n.label))
  const rows = []
  let currentRow = []
  let currentRowWidths = []
  let currentRowWidth = 0
  
  nodes.forEach((node, idx) => {
    const w = nodeWidths[idx]
    if (currentRowWidth + w + (currentRow.length > 0 ? nodeGapX : 0) > maxRowWidth && currentRow.length > 0) {
      rows.push({ nodes: currentRow, widths: currentRowWidths, totalWidth: currentRowWidth })
      currentRow = [node]
      currentRowWidths = [w]
      currentRowWidth = w
    } else {
      currentRow.push(node)
      currentRowWidths.push(w)
      currentRowWidth += w + (currentRow.length > 1 ? nodeGapX : 0)
    }
  })
  
  if (currentRow.length > 0) {
    rows.push({ nodes: currentRow, widths: currentRowWidths, totalWidth: currentRowWidth })
  }
  
  return rows
}

// ==================== ERS 智能连线系统 V2 (Edge Routing System) ====================
// 核心优化：
// 1. 总线通道（Bus Routing）- 跨多层连线使用统一垂直/水平通道
// 2. 智能锚点分配 - 8方向锚点，多边自动分散
// 3. 标签碰撞检测 - 自动调整标签位置避免重叠
// 4. 最短路径优先 - 尽量减少折点和绕路距离

/**
 * 完整配置系统 V2
 */
const ROUTING_CONFIG = {
  // 网格设置
  GRID: {
    SIZE: 25,
    ADAPTIVE: true,
  },
  
  // 避障参数 - 增大间距
  AVOIDANCE: {
    NODE_PADDING: 35,        // 节点周围安全距离（增大）
    CHANNEL_GAP: 60,         // 层间通道间距（增大）
    MAX_DETOUR_RATIO: 2.0,
    BUS_MARGIN: 80,          // 总线通道距离画布边缘的距离
  },
  
  // 锚点配置 - 8方向锚点
  ANCHOR: {
    SPREAD_FACTOR: 0.15,     // 多连线分散系数（减小，更均匀）
    POINTS: {                // 8方向锚点
      top: { x: 0.5, y: 0 },
      topRight: { x: 0.85, y: 0 },
      right: { x: 1, y: 0.5 },
      bottomRight: { x: 0.85, y: 1 },
      bottom: { x: 0.5, y: 1 },
      bottomLeft: { x: 0.15, y: 1 },
      left: { x: 0, y: 0.5 },
      topLeft: { x: 0.15, y: 0 },
    },
    TYPE_RULES: {
      database: { prefer: 'top', avoid: 'left' },
      service: { prefer: 'bottom', avoid: null },
      gateway: { prefer: 'right', avoid: null },
      client: { prefer: 'bottom', avoid: 'top' },
    }
  },
  
  // 连线优先级
  PRIORITY: {
    CRITICAL: 10,
    IMPORTANT: 5,
    NORMAL: 1,
  },
  
  // 总线配置
  BUS: {
    ENABLED: true,           // 启用总线路由
    CROSS_LAYER_THRESHOLD: 2, // 跨越超过2层使用总线
    CHANNEL_SPACING: 15,     // 总线内多条线的间距
  }
}

/**
 * 分层路由器（Hierarchical Router）
 * 替代Manhattan Router，更适合架构图场景
 */
class HierarchicalRouter {
  constructor(allNodePositions, excludeIds = []) {
    this.padding = ROUTING_CONFIG.AVOIDANCE.NODE_PADDING
    this.channelGap = ROUTING_CONFIG.AVOIDANCE.CHANNEL_GAP
    this.obstacles = []
    this.nodePositions = allNodePositions
    
    // 收集所有障碍物（节点矩形 + padding）
    for (const [nodeId, pos] of Object.entries(allNodePositions)) {
      if (!excludeIds.includes(nodeId) && pos) {
        this.obstacles.push({
          id: nodeId,
          x: pos.x - this.padding,
          y: pos.y - this.padding,
          width: pos.width + this.padding * 2,
          height: pos.height + this.padding * 2,
          centerX: pos.x + pos.width / 2,
          centerY: pos.y + pos.height / 2
        })
      }
    }
  }
  
  /**
   * 检查线段是否与矩形碰撞
   */
  lineHitsRect(x1, y1, x2, y2, rect) {
    // 简化检测：检查线段的几个采样点是否在矩形内
    const steps = Math.max(Math.abs(x2 - x1), Math.abs(y2 - y1)) / 10
    for (let i = 0; i <= steps; i++) {
      const t = i / steps
      const px = x1 + (x2 - x1) * t
      const py = y1 + (y2 - y1) * t
      if (px >= rect.x && px <= rect.x + rect.width &&
          py >= rect.y && py <= rect.y + rect.height) {
        return true
      }
    }
    return false
  }
  
  /**
   * 检查路径段是否有碰撞
   */
  hasCollision(x1, y1, x2, y2) {
    for (const obs of this.obstacles) {
      if (this.lineHitsRect(x1, y1, x2, y2, obs)) {
        return obs
      }
    }
    return null
  }
  
  /**
   * 分层路由主函数
   * Level 0: 直连
   * Level 1: 单折点
   * Level 2: 双折点
   * Level 3: 通道路由
   */
  findPath(srcX, srcY, tgtX, tgtY) {
    // Level 0: 尝试直连
    if (!this.hasCollision(srcX, srcY, tgtX, tgtY)) {
      return []  // 无需waypoint
    }
    
    // Level 1: 尝试单折点（L型路径）
    const level1 = this.trySingleBend(srcX, srcY, tgtX, tgtY)
    if (level1) return level1
    
    // Level 2: 尝试双折点（Z型路径）
    const level2 = this.tryDoubleBend(srcX, srcY, tgtX, tgtY)
    if (level2) return level2
    
    // Level 3: 通道路由
    const level3 = this.tryChannelRoute(srcX, srcY, tgtX, tgtY)
    if (level3) return level3
    
    // 降级：返回简单中点折线
    return this.fallbackRoute(srcX, srcY, tgtX, tgtY)
  }
  
  /**
   * Level 1: 单折点路由（L型）
   * 尝试两种L型路径，选择无碰撞的
   */
  trySingleBend(srcX, srcY, tgtX, tgtY) {
    // 方案A: 先水平后垂直
    const midA = { x: tgtX, y: srcY }
    if (!this.hasCollision(srcX, srcY, midA.x, midA.y) &&
        !this.hasCollision(midA.x, midA.y, tgtX, tgtY)) {
      return [midA]
    }
    
    // 方案B: 先垂直后水平
    const midB = { x: srcX, y: tgtY }
    if (!this.hasCollision(srcX, srcY, midB.x, midB.y) &&
        !this.hasCollision(midB.x, midB.y, tgtX, tgtY)) {
      return [midB]
    }
    
    return null
  }
  
  /**
   * Level 2: 双折点路由（Z型或U型）- 优化版
   * 优先选择最短绕路路径
   */
  tryDoubleBend(srcX, srcY, tgtX, tgtY) {
    const gap = this.channelGap
    const candidates = []
    
    // 判断主方向
    const isVertical = Math.abs(tgtY - srcY) > Math.abs(tgtX - srcX)
    
    if (isVertical) {
      // 垂直方向：尝试左右绕行
      // 优先选择距离两端更近的通道
      const midX_center = (srcX + tgtX) / 2
      const offsets = [
        midX_center - srcX > 0 ? -gap : gap,  // 靠近源
        midX_center - srcX > 0 ? gap : -gap,  // 靠近目标
        -gap * 1.5,
        gap * 1.5
      ]
      
      for (const offset of offsets) {
        const midX = srcX + offset
        const p1 = { x: midX, y: srcY }
        const p2 = { x: midX, y: tgtY }
        
        if (!this.hasCollision(srcX, srcY, p1.x, p1.y) &&
            !this.hasCollision(p1.x, p1.y, p2.x, p2.y) &&
            !this.hasCollision(p2.x, p2.y, tgtX, tgtY)) {
          const pathLen = Math.abs(offset) + Math.abs(tgtY - srcY) + Math.abs(tgtX - midX)
          candidates.push({ path: [p1, p2], length: pathLen })
        }
      }
    } else {
      // 水平方向：尝试上下绕行
      const midY_center = (srcY + tgtY) / 2
      const offsets = [
        midY_center - srcY > 0 ? -gap : gap,
        midY_center - srcY > 0 ? gap : -gap,
        -gap * 1.5,
        gap * 1.5
      ]
      
      for (const offset of offsets) {
        const midY = srcY + offset
        const p1 = { x: srcX, y: midY }
        const p2 = { x: tgtX, y: midY }
        
        if (!this.hasCollision(srcX, srcY, p1.x, p1.y) &&
            !this.hasCollision(p1.x, p1.y, p2.x, p2.y) &&
            !this.hasCollision(p2.x, p2.y, tgtX, tgtY)) {
          const pathLen = Math.abs(offset) + Math.abs(tgtX - srcX) + Math.abs(tgtY - midY)
          candidates.push({ path: [p1, p2], length: pathLen })
        }
      }
    }
    
    // 选择最短路径
    if (candidates.length > 0) {
      candidates.sort((a, b) => a.length - b.length)
      return candidates[0].path
    }
    
    return null
  }
  
  /**
   * Level 3: 通道路由 - 优化版
   * 智能选择最优通道，避免过长绕路
   */
  tryChannelRoute(srcX, srcY, tgtX, tgtY) {
    if (this.obstacles.length === 0) return null
    
    // 计算所有障碍物的边界
    let minX = Infinity, maxX = -Infinity
    let minY = Infinity, maxY = -Infinity
    
    this.obstacles.forEach(obs => {
      minX = Math.min(minX, obs.x)
      maxX = Math.max(maxX, obs.x + obs.width)
      minY = Math.min(minY, obs.y)
      maxY = Math.max(maxY, obs.y + obs.height)
    })
    
    const gap = this.channelGap
    const candidates = []
    
    // 计算直线距离作为基准
    const directDist = Math.sqrt(Math.pow(tgtX - srcX, 2) + Math.pow(tgtY - srcY, 2))
    
    // 尝试左右通道
    const leftX = minX - gap
    const rightX = maxX + gap
    
    // 左通道路径
    const leftPath = [{ x: leftX, y: srcY }, { x: leftX, y: tgtY }]
    const leftDist = Math.abs(srcX - leftX) + Math.abs(tgtY - srcY) + Math.abs(tgtX - leftX)
    if (leftDist < directDist * 2.5) {  // 不超过直线距离的2.5倍
      candidates.push({ path: leftPath, length: leftDist, type: 'left' })
    }
    
    // 右通道路径
    const rightPath = [{ x: rightX, y: srcY }, { x: rightX, y: tgtY }]
    const rightDist = Math.abs(srcX - rightX) + Math.abs(tgtY - srcY) + Math.abs(tgtX - rightX)
    if (rightDist < directDist * 2.5) {
      candidates.push({ path: rightPath, length: rightDist, type: 'right' })
    }
    
    // 尝试上下通道
    const topY = minY - gap
    const bottomY = maxY + gap
    
    // 上通道路径
    const topPath = [{ x: srcX, y: topY }, { x: tgtX, y: topY }]
    const topDist = Math.abs(srcY - topY) + Math.abs(tgtX - srcX) + Math.abs(tgtY - topY)
    if (topDist < directDist * 2.5) {
      candidates.push({ path: topPath, length: topDist, type: 'top' })
    }
    
    // 下通道路径
    const bottomPath = [{ x: srcX, y: bottomY }, { x: tgtX, y: bottomY }]
    const bottomDist = Math.abs(srcY - bottomY) + Math.abs(tgtX - srcX) + Math.abs(tgtY - bottomY)
    if (bottomDist < directDist * 2.5) {
      candidates.push({ path: bottomPath, length: bottomDist, type: 'bottom' })
    }
    
    // 选择最短的有效通道
    if (candidates.length > 0) {
      candidates.sort((a, b) => a.length - b.length)
      return candidates[0].path
    }
    
    // 如果没有合适的通道，使用默认策略
    const isVertical = Math.abs(tgtY - srcY) > Math.abs(tgtX - srcX)
    if (isVertical) {
      const channelX = Math.abs(srcX - leftX) < Math.abs(srcX - rightX) ? leftX : rightX
      return [{ x: channelX, y: srcY }, { x: channelX, y: tgtY }]
    } else {
      const channelY = Math.abs(srcY - topY) < Math.abs(srcY - bottomY) ? topY : bottomY
      return [{ x: srcX, y: channelY }, { x: tgtX, y: channelY }]
    }
  }
  
  /**
   * 降级路由：简单中点折线
   */
  fallbackRoute(srcX, srcY, tgtX, tgtY) {
    const midY = (srcY + tgtY) / 2
    return [
      { x: srcX, y: midY },
      { x: tgtX, y: midY }
    ]
  }
}

/**
 * 推断连线方向
 */
function inferEdgeDirection(srcPos, tgtPos) {
  const dx = tgtPos.x + tgtPos.width / 2 - (srcPos.x + srcPos.width / 2)
  const dy = tgtPos.y + tgtPos.height / 2 - (srcPos.y + srcPos.height / 2)
  
  if (Math.abs(dy) > Math.abs(dx) * 1.2) {
    return dy > 0 ? 'down' : 'up'
  } else {
    return dx > 0 ? 'right' : 'left'
  }
}

/**
 * 根据方向计算锚点 - V2 智能8方向锚点
 * @param direction 主方向 (up/down/left/right)
 * @param exitIndex 出口索引（用于同节点多边分散）
 * @param exitTotal 该节点总出边数
 * @param entryIndex 入口索引
 * @param entryTotal 该节点总入边数
 */
function calculateAnchors(direction, exitIndex = 0, exitTotal = 1, entryIndex = 0, entryTotal = 1) {
  const spread = ROUTING_CONFIG.ANCHOR.SPREAD_FACTOR
  
  // 计算偏移量：让多条边均匀分布
  const getOffset = (index, total) => {
    if (total <= 1) return 0
    // 均匀分布在 [-spread, spread] 范围内
    return ((index / (total - 1)) - 0.5) * 2 * spread
  }
  
  const exitOffset = getOffset(exitIndex, exitTotal)
  const entryOffset = getOffset(entryIndex, entryTotal)
  
  // 8方向锚点映射
  const anchorMap = {
    'down': {
      exit: { x: 0.5 + exitOffset, y: 1 },
      entry: { x: 0.5 + entryOffset, y: 0 }
    },
    'up': {
      exit: { x: 0.5 + exitOffset, y: 0 },
      entry: { x: 0.5 + entryOffset, y: 1 }
    },
    'right': {
      exit: { x: 1, y: 0.5 + exitOffset },
      entry: { x: 0, y: 0.5 + entryOffset }
    },
    'left': {
      exit: { x: 0, y: 0.5 + exitOffset },
      entry: { x: 1, y: 0.5 + entryOffset }
    },
    // 斜向锚点（用于特殊布局）
    'down-right': {
      exit: { x: 0.85, y: 1 },
      entry: { x: 0.15, y: 0 }
    },
    'down-left': {
      exit: { x: 0.15, y: 1 },
      entry: { x: 0.85, y: 0 }
    },
    'up-right': {
      exit: { x: 0.85, y: 0 },
      entry: { x: 0.15, y: 1 }
    },
    'up-left': {
      exit: { x: 0.15, y: 0 },
      entry: { x: 0.85, y: 1 }
    }
  }
  
  const anchors = anchorMap[direction] || anchorMap['down']
  
  // 确保锚点在有效范围内 [0.05, 0.95]
  anchors.exit.x = Math.max(0.05, Math.min(0.95, anchors.exit.x))
  anchors.exit.y = Math.max(0.05, Math.min(0.95, anchors.exit.y))
  anchors.entry.x = Math.max(0.05, Math.min(0.95, anchors.entry.x))
  anchors.entry.y = Math.max(0.05, Math.min(0.95, anchors.entry.y))
  
  return anchors
}

/**
 * 智能选择最佳锚点方向
 * 考虑节点相对位置、已有连线、避免交叉
 */
function selectBestAnchorDirection(srcPos, tgtPos, existingEdges = []) {
  const dx = (tgtPos.x + tgtPos.width / 2) - (srcPos.x + srcPos.width / 2)
  const dy = (tgtPos.y + tgtPos.height / 2) - (srcPos.y + srcPos.height / 2)
  
  const absDx = Math.abs(dx)
  const absDy = Math.abs(dy)
  
  // 判断主方向
  if (absDy > absDx * 1.5) {
    // 主要是垂直方向
    return dy > 0 ? 'down' : 'up'
  } else if (absDx > absDy * 1.5) {
    // 主要是水平方向
    return dx > 0 ? 'right' : 'left'
  } else {
    // 斜向 - 选择合适的复合方向
    if (dy > 0) {
      return dx > 0 ? 'down-right' : 'down-left'
    } else {
      return dx > 0 ? 'up-right' : 'up-left'
    }
  }
}

/**
 * 计算连线的实际起点和终点坐标
 */
function calculateEdgeEndpoints(srcPos, tgtPos, anchors) {
  const srcX = srcPos.x + srcPos.width * anchors.exit.x
  const srcY = srcPos.y + srcPos.height * anchors.exit.y
  const tgtX = tgtPos.x + tgtPos.width * anchors.entry.x
  const tgtY = tgtPos.y + tgtPos.height * anchors.entry.y
  return { srcX, srcY, tgtX, tgtY }
}

/**
 * 检测连线路径上的障碍物
 */
function detectObstacles(srcX, srcY, tgtX, tgtY, allNodePositions, excludeIds) {
  const obstacles = []
  
  for (const [nodeId, pos] of Object.entries(allNodePositions)) {
    if (excludeIds.includes(nodeId)) continue
    if (lineIntersectsRect(srcX, srcY, tgtX, tgtY, pos)) {
      obstacles.push({ id: nodeId, ...pos })
    }
  }
  
  return obstacles
}

/**
 * Level 1: 生成主方向基础路线（禁止斜线）
 * 只允许 "竖直→水平→竖直" 或 "水平→竖直→水平" 的折线
 */
function generateBaseRoute(srcX, srcY, tgtX, tgtY, direction) {
  const waypoints = []
  
  if (direction === 'down' || direction === 'up') {
    // 上下方向：竖直→水平→竖直
    // source.bottom → (srcX, midY) → (tgtX, midY) → target.top
    const midY = (srcY + tgtY) / 2
    if (Math.abs(srcX - tgtX) > 10) {  // 如果水平有偏移
      waypoints.push({ x: srcX, y: midY })
      waypoints.push({ x: tgtX, y: midY })
    }
  } else {
    // 左右方向：水平→竖直→水平
    // source.right → (midX, srcY) → (midX, tgtY) → target.left
    const midX = (srcX + tgtX) / 2
    if (Math.abs(srcY - tgtY) > 10) {  // 如果垂直有偏移
      waypoints.push({ x: midX, y: srcY })
      waypoints.push({ x: midX, y: tgtY })
    }
  }
  
  return waypoints
}

/**
 * Level 2: 中继点避障（Waypoint Routing）
 * 优先垂直偏移，再水平偏移
 */
function applyObstacleAvoidance(waypoints, srcX, srcY, tgtX, tgtY, allNodes, excludeIds, direction) {
  if (waypoints.length === 0) {
    return waypoints
  }
  
  let result = [...waypoints]
  
  // 构建完整路径用于检测
  const buildFullPath = (wps) => [
    { x: srcX, y: srcY },
    ...wps,
    { x: tgtX, y: tgtY }
  ]
  
  // 获取障碍物节点
  const obstacleNodes = Object.entries(allNodes)
    .filter(([id]) => !excludeIds.includes(id))
    .map(([id, pos]) => ({ id, ...pos }))
  
  // Step 1: 优先垂直偏移（最自然）
  for (let shift = 0; shift < ERS_CONFIG.MAX_VERTICAL_SHIFTS; shift++) {
    let hasCollision = false
    let collidedNode = null
    const fullPath = buildFullPath(result)
    
    for (let i = 0; i < fullPath.length - 1; i++) {
      const p1 = fullPath[i]
      const p2 = fullPath[i + 1]
      
      for (const node of obstacleNodes) {
        if (lineIntersectsRect(p1.x, p1.y, p2.x, p2.y, node)) {
          hasCollision = true
          collidedNode = node
          break
        }
      }
      if (hasCollision) break
    }
    
    if (hasCollision && collidedNode) {
      // 垂直偏移：根据障碍物位置决定方向
      const offsetY = collidedNode.height + ERS_CONFIG.VERTICAL_OFFSET
      // 判断障碍物在waypoint的上方还是下方
      const waypointY = result.length > 0 ? result[0].y : (srcY + tgtY) / 2
      const nodeCenterY = collidedNode.y + collidedNode.height / 2
      // 如果障碍物在waypoint下方，向上偏移；否则向下偏移
      const goUp = nodeCenterY > waypointY
      result = result.map(wp => ({
        x: wp.x,
        y: wp.y + (goUp ? -offsetY : offsetY)
      }))
    } else {
      break
    }
  }
  
  // Step 2: 仍然碰撞 → 水平偏移（备选）
  for (let shift = 0; shift < ERS_CONFIG.MAX_HORIZONTAL_SHIFTS; shift++) {
    let hasCollision = false
    let collidedNode = null
    const fullPath = buildFullPath(result)
    
    for (let i = 0; i < fullPath.length - 1; i++) {
      const p1 = fullPath[i]
      const p2 = fullPath[i + 1]
      
      for (const node of obstacleNodes) {
        if (lineIntersectsRect(p1.x, p1.y, p2.x, p2.y, node)) {
          hasCollision = true
          collidedNode = node
          break
        }
      }
      if (hasCollision) break
    }
    
    if (hasCollision && collidedNode) {
      // 水平偏移：根据障碍物位置决定方向
      const offsetX = collidedNode.width + ERS_CONFIG.HORIZONTAL_OFFSET
      // 判断障碍物在waypoint的左侧还是右侧
      const waypointX = result.length > 0 ? result[0].x : (srcX + tgtX) / 2
      const nodeCenterX = collidedNode.x + collidedNode.width / 2
      // 如果障碍物在waypoint右侧，向左偏移；否则向右偏移
      const goLeft = nodeCenterX > waypointX
      result = result.map(wp => ({
        x: wp.x + (goLeft ? -offsetX : offsetX),
        y: wp.y
      }))
    } else {
      break
    }
  }
  
  return result
}

/**
 * Level 3: 通道避障（Channel Routing）
 * 为复杂的跨层连线计算通道位置
 * 适用于：连线需要穿越多个节点层的情况
 */
function calculateChannelRoute(srcPos, tgtPos, allNodes, excludeIds) {
  // 计算源和目标之间的所有节点
  const obstacleNodes = Object.entries(allNodes)
    .filter(([id]) => !excludeIds.includes(id))
    .map(([id, pos]) => ({ id, ...pos }))
  
  // 判断主要方向
  const srcCenterY = srcPos.y + srcPos.height / 2
  const tgtCenterY = tgtPos.y + tgtPos.height / 2
  const isVertical = Math.abs(tgtCenterY - srcCenterY) > Math.abs(tgtPos.x - srcPos.x)
  
  if (isVertical) {
    // 垂直方向：通道在左右两侧
    const minY = Math.min(srcPos.y + srcPos.height, tgtPos.y)
    const maxY = Math.max(srcPos.y + srcPos.height, tgtPos.y)
    
    // 找到在这个区域内的所有节点
    const nodesInBetween = obstacleNodes.filter(node => 
      node.y + node.height > minY && node.y < maxY
    )
    
    if (nodesInBetween.length === 0) {
      return null
    }
    
    // 找到通道位置
    let leftMost = Infinity
    let rightMost = -Infinity
    
    nodesInBetween.forEach(node => {
      leftMost = Math.min(leftMost, node.x)
      rightMost = Math.max(rightMost, node.x + node.width)
    })
    
    const srcCenterX = srcPos.x + srcPos.width / 2
    const tgtCenterX = tgtPos.x + tgtPos.width / 2
    
    // 选择更近的通道，确保通道位置合理（不能太靠边）
    const leftChannel = Math.max(20, leftMost - ERS_CONFIG.CHANNEL_GAP)
    const rightChannel = rightMost + ERS_CONFIG.CHANNEL_GAP
    
    const useLeftChannel = Math.abs(srcCenterX - leftChannel) + Math.abs(tgtCenterX - leftChannel) <
                           Math.abs(srcCenterX - rightChannel) + Math.abs(tgtCenterX - rightChannel)
    
    const channelX = useLeftChannel ? leftChannel : rightChannel
    
    // 根据方向确定Y坐标
    const srcY = srcCenterY < tgtCenterY ? srcPos.y + srcPos.height : srcPos.y
    const tgtY = srcCenterY < tgtCenterY ? tgtPos.y : tgtPos.y + tgtPos.height
    
    return {
      channelX,
      waypoints: [
        { x: channelX, y: srcY },
        { x: channelX, y: tgtY }
      ]
    }
  } else {
    // 水平方向：通道在上下两侧
    const minX = Math.min(srcPos.x + srcPos.width, tgtPos.x)
    const maxX = Math.max(srcPos.x + srcPos.width, tgtPos.x)
    
    const nodesInBetween = obstacleNodes.filter(node => 
      node.x + node.width > minX && node.x < maxX
    )
    
    if (nodesInBetween.length === 0) {
      return null
    }
    
    let topMost = Infinity
    let bottomMost = -Infinity
    
    nodesInBetween.forEach(node => {
      topMost = Math.min(topMost, node.y)
      bottomMost = Math.max(bottomMost, node.y + node.height)
    })
    
    const srcCenterX = srcPos.x + srcPos.width / 2
    const tgtCenterX = tgtPos.x + tgtPos.width / 2
    
    const topChannel = Math.max(20, topMost - ERS_CONFIG.CHANNEL_GAP)
    const bottomChannel = bottomMost + ERS_CONFIG.CHANNEL_GAP
    
    const srcCenterYPos = srcPos.y + srcPos.height / 2
    const useTopChannel = Math.abs(srcCenterYPos - topChannel) < Math.abs(srcCenterYPos - bottomChannel)
    
    const channelY = useTopChannel ? topChannel : bottomChannel
    
    const srcX = srcCenterX < tgtCenterX ? srcPos.x + srcPos.width : srcPos.x
    const tgtX = srcCenterX < tgtCenterX ? tgtPos.x : tgtPos.x + tgtPos.width
    
    return {
      channelY,
      waypoints: [
        { x: srcX, y: channelY },
        { x: tgtX, y: channelY }
      ]
    }
  }
}

/**
 * ERS主函数：使用分层路由器计算连线路由
 */
function calculateEdgeRoute(srcId, tgtId, allNodePositions, edgeConfig = {}) {
  const srcPos = allNodePositions[srcId]
  const tgtPos = allNodePositions[tgtId]
  
  if (!srcPos || !tgtPos) {
    return { valid: false }
  }
  
  // Step 1: 确定方向
  const direction = edgeConfig.direction || inferEdgeDirection(srcPos, tgtPos)
  
  // Step 2: 计算锚点（使用偏移量分散多条边）
  const exitOffset = edgeConfig.exitOffset || 0
  const entryOffset = edgeConfig.entryOffset || 0
  const anchors = calculateAnchors(direction, exitOffset, entryOffset)
  
  // Step 3: 计算端点坐标
  const endpoints = calculateEdgeEndpoints(srcPos, tgtPos, anchors)
  const { srcX, srcY, tgtX, tgtY } = endpoints
  
  // Step 4: 使用分层路由器（替代Manhattan Router）
  const router = new HierarchicalRouter(allNodePositions, [srcId, tgtId])
  let waypoints = router.findPath(srcX, srcY, tgtX, tgtY)
  
  return {
    valid: true,
    direction,
    anchors,
    endpoints,
    waypoints,
    hasDetour: waypoints.length > 0
  }
}

/**
 * 生成waypoints的XML
 */
function generateWaypointsXml(waypoints) {
  if (!waypoints || waypoints.length === 0) {
    return ''
  }
  
  const points = waypoints.map(wp => 
    `<mxPoint x="${Math.round(wp.x)}" y="${Math.round(wp.y)}"/>`
  ).join('\n          ')
  
  return `<Array as="points">
          ${points}
        </Array>`
}

// ==================== 主转换函数 ====================

/**
 * 验证并修复 JSON 数据
 */
function validateAndFixData(data) {
  if (!data) return null
  
  // 确保基本结构存在
  if (data.nodes) {
    // 修复节点：确保每个节点有 id 和 label
    data.nodes = data.nodes.filter(n => n && (n.id || n.label)).map((n, i) => ({
      ...n,
      id: n.id || `node-${i}`,
      label: n.label || n.id || `节点${i + 1}`
    }))
    
    // 验证 edges 中的 source/target 是否存在
    if (data.edges) {
      const nodeIds = new Set(data.nodes.map(n => n.id))
      const validEdges = data.edges.filter(e => {
        const src = e.source || e.from
        const tgt = e.target || e.to
        if (!nodeIds.has(src)) {
          console.warn(`[验证] 边的 source "${src}" 不存在，已跳过`)
          return false
        }
        if (!nodeIds.has(tgt)) {
          console.warn(`[验证] 边的 target "${tgt}" 不存在，已跳过`)
          return false
        }
        return true
      })
      
      if (validEdges.length !== data.edges.length) {
        console.log(`[验证] 修复了 ${data.edges.length - validEdges.length} 条无效边`)
        data.edges = validEdges
      }
    }
    
    // 验证 groups 中的 children 是否存在
    if (data.groups) {
      const nodeIds = new Set(data.nodes.map(n => n.id))
      data.groups = data.groups.map(g => ({
        ...g,
        children: (g.children || g.nodes || []).filter(id => nodeIds.has(id))
      }))
    }
  }
  
  return data
}

/**
 * 将 JSON 数据转换为 Draw.io XML
 * 根据 data.type 智能选择渲染器
 * @param {Object} data - 架构图 JSON 数据
 * @param {string} style - 风格：'normal' | 'enterprise'（仅对 layered 有效）
 * @returns {string} Draw.io XML
 */
export function convertToDrawioXml(data, style = 'normal') {
  if (!data) return getEmptyDiagram()
  
  // 验证并修复数据
  data = validateAndFixData(data)
  if (!data) return getEmptyDiagram()
  
  const type = data.type || 'layered'
  console.log('[渲染器] 图表类型:', type)
  
  // 根据类型选择渲染器
  switch (type) {
    case 'smart':
      return convertSmartToDrawio(data)
    case 'flow':
      return convertFlowToDrawio(data)
    case 'mindmap':
      return convertMindmapToDrawio(data)
    case 'network':
      return convertNetworkToDrawio(data)
    case 'layered':
    default:
      // 分层架构 - 检查是否有数据
      if (!data.layers || data.layers.length === 0) {
        return getEmptyDiagram()
      }
      // 检测是否有侧边栏
      const hasSidebar = data.leftSidebar || data.rightSidebar
      if (style === 'normal' && !hasSidebar) {
        return convertLayeredToDrawioNormal(data)
      } else {
        if (hasSidebar) {
          return convertLayeredSidebarToDrawio(data)
        }
        return convertLayeredToDrawioEnterprise(data)
      }
  }
}

/**
 * 获取空白图表
 */
export function getEmptyDiagram() {
  return `<mxGraphModel>
    <root>
      <mxCell id="0"/>
      <mxCell id="1" parent="0"/>
    </root>
  </mxGraphModel>`
}

// ==================== 普通风格渲染器 ====================

/**
 * 普通风格：多彩虚线边框、居中标题、层间连线
 */
function convertLayeredToDrawioNormal(data) {
  resetNodeIdCounter()  // 重置 ID 计数器
  
  const { layers } = data
  if (!layers || !layers.length) return getEmptyDiagram()
  
  const cfg = {
    canvasWidth: 900,
    layerPadding: 25,
    nodeH: 40,
    nodeGapX: 12,
    nodeGapY: 8,
    maxRowWidth: 700,
    layerGap: 30,
  }
  
  let cells = ''
  let currentY = 30
  let prevLayerBottomY = 0
  let edgeId = 1000
  
  layers.forEach((layer, layerIndex) => {
    // 提取所有节点
    let allNodes = []
    const layerBlocks = layer.blocks || layer.groups || []
    
    if (layer.nodes?.length > 0) {
      allNodes = [...layer.nodes]
    } else if (layerBlocks.length > 0) {
      layerBlocks.forEach(g => {
        (g.nodes || []).forEach(n => allNodes.push(n))
      })
    }
    
    if (allNodes.length === 0) return
    
    // 分行
    const nodeRows = groupNodesIntoRows(allNodes, cfg.maxRowWidth, cfg.nodeGapX)
    
    const color = LAYER_COLORS_NORMAL[layerIndex % LAYER_COLORS_NORMAL.length]
    const maxRowWidth = nodeRows.length > 0 ? Math.max(...nodeRows.map(r => r.totalWidth)) : 200
    const layerContentH = Math.max(nodeRows.length, 1) * cfg.nodeH + Math.max(nodeRows.length - 1, 0) * cfg.nodeGapY
    const layerHeight = 35 + layerContentH + 15
    const layerWidth = maxRowWidth + cfg.layerPadding * 2
    const layerX = (cfg.canvasWidth - layerWidth) / 2
    
    // 层间连线（垂直虚线箭头）
    if (layerIndex > 0 && prevLayerBottomY > 0) {
      cells += `<mxCell id="edge-${edgeId++}" value="" style="endArrow=classic;html=1;strokeColor=#8C8C8C;strokeWidth=1;dashed=1;dashPattern=8 4;endSize=6;" edge="1" parent="1">
        <mxGeometry relative="1" as="geometry">
          <mxPoint x="${cfg.canvasWidth / 2}" y="${prevLayerBottomY + 5}" as="sourcePoint"/>
          <mxPoint x="${cfg.canvasWidth / 2}" y="${currentY - 5}" as="targetPoint"/>
        </mxGeometry>
      </mxCell>`
    }
    
    // 层背景（虚线边框）
    cells += `<mxCell id="layer-${layerIndex}" value="" style="rounded=1;whiteSpace=wrap;html=1;fillColor=none;strokeColor=${color.border};strokeWidth=2;dashed=1;dashPattern=8 4;arcSize=6;" vertex="1" parent="1">
      <mxGeometry x="${layerX}" y="${currentY}" width="${layerWidth}" height="${layerHeight}" as="geometry"/>
    </mxCell>`
    
    // 层标题
    cells += `<mxCell id="layer-title-${layerIndex}" value="${escapeXml(layer.name || '')}" style="text;html=1;strokeColor=none;fillColor=none;align=center;verticalAlign=middle;fontSize=14;fontStyle=1;fontColor=${color.title};" vertex="1" parent="1">
      <mxGeometry x="${layerX}" y="${currentY + 5}" width="${layerWidth}" height="25" as="geometry"/>
    </mxCell>`
    
    // 渲染节点
    let rowY = currentY + 35
    nodeRows.forEach(row => {
      const rowStartX = layerX + (layerWidth - row.totalWidth) / 2
      let nodeX = rowStartX
      
      row.nodes.forEach((node, idx) => {
        const nodeId = ensureNodeId(node, 'normal-node')
        const nodeW = row.widths[idx]
        cells += `<mxCell id="${nodeId}" value="${escapeXml(node.label)}" style="rounded=1;whiteSpace=wrap;html=1;fillColor=#FFFFFF;strokeColor=#E0E0E0;strokeWidth=1;fontSize=12;fontColor=#424242;shadow=1;arcSize=15;" vertex="1" parent="1">
          <mxGeometry x="${nodeX}" y="${rowY}" width="${nodeW}" height="${cfg.nodeH}" as="geometry"/>
        </mxCell>`
        nodeX += nodeW + cfg.nodeGapX
      })
      rowY += cfg.nodeH + cfg.nodeGapY
    })
    
    prevLayerBottomY = currentY + layerHeight
    currentY += layerHeight + cfg.layerGap
  })
  
  return `<mxGraphModel dx="1326" dy="796" grid="1" gridSize="10" guides="1" tooltips="1" connect="1" arrows="1" fold="1" page="1" pageScale="1" pageWidth="827" pageHeight="1169">
    <root>
      <mxCell id="0"/>
      <mxCell id="1" parent="0"/>
      ${cells}
    </root>
  </mxGraphModel>`
}

// ==================== 企业级风格渲染器 ====================

/**
 * 企业级风格：左侧标签、支持 blocks 分组
 */
function convertLayeredToDrawioEnterprise(data) {
  resetNodeIdCounter()  // 重置 ID 计数器
  
  const { layers } = data
  if (!layers || !layers.length) return getEmptyDiagram()
  
  const colors = COLORS_ENTERPRISE
  const cfg = {
    labelWidth: 85,
    labelGap: 2,
    nodeHeight: 24,
    nodeGapX: 6,
    nodeGapY: 4,
    maxRowWidth: 700,
    blockGapX: 8,
    blockPaddingX: 6,
    blockPaddingY: 5,
    blockTitleH: 18,
    layerPaddingX: 8,
    layerPaddingY: 8,
    layerGap: 3,
  }
  
  let cells = ''
  
  // 第一遍：计算每层布局
  const layerInfos = []
  let maxLayerWidth = 400
  
  layers.forEach((layer) => {
    const layerBlocks = layer.blocks || layer.groups || []
    const hasBlocks = layerBlocks.length > 0
    
    let layerWidth = 0
    let layerHeight = 0
    let blockInfos = []
    let nodeRows = []
    
    if (hasBlocks) {
      layerBlocks.forEach(block => {
        const nodes = block.nodes || []
        if (nodes.length === 0) return
        
        const blockRows = groupNodesIntoRows(nodes, 220, cfg.nodeGapX)
        const maxRowW = blockRows.length > 0 ? Math.max(...blockRows.map(r => r.totalWidth)) : 100
        const blockW = maxRowW + cfg.blockPaddingX * 2
        const blockH = cfg.blockTitleH + blockRows.length * cfg.nodeHeight + 
                       (blockRows.length - 1) * cfg.nodeGapY + cfg.blockPaddingY * 2
        blockInfos.push({ block, blockRows, width: blockW, height: blockH })
        layerWidth += blockW + cfg.blockGapX
      })
      
      if (blockInfos.length > 0) {
        layerWidth -= cfg.blockGapX
        layerWidth += cfg.layerPaddingX * 2
        layerHeight = Math.max(...blockInfos.map(b => b.height)) + cfg.layerPaddingY * 2
      } else {
        layerWidth = 400
        layerHeight = 50
      }
    } else {
      let allNodes = layer.nodes || []
      nodeRows = groupNodesIntoRows(allNodes, cfg.maxRowWidth, cfg.nodeGapX)
      const maxRowWidth = nodeRows.length > 0 ? Math.max(...nodeRows.map(r => r.totalWidth)) : 0
      layerWidth = Math.max(maxRowWidth, 100) + cfg.layerPaddingX * 2
      layerHeight = Math.max(nodeRows.length, 1) * cfg.nodeHeight + 
                    Math.max(nodeRows.length - 1, 0) * cfg.nodeGapY + cfg.layerPaddingY * 2
    }
    
    maxLayerWidth = Math.max(maxLayerWidth, layerWidth)
    layerInfos.push({ layer, hasBlocks, blockInfos, nodeRows, width: layerWidth, height: layerHeight })
  })
  
  const contentStartX = cfg.labelWidth + cfg.labelGap
  const canvasWidth = contentStartX + maxLayerWidth + 20
  let currentY = 15
  
  // 渲染每层
  layerInfos.forEach((info, layerIndex) => {
    const { layer, hasBlocks, blockInfos, nodeRows, height: layerHeight } = info
    
    // 层背景
    cells += `<mxCell id="layer-bg-${layerIndex}" value="" style="rounded=0;whiteSpace=wrap;html=1;fillColor=${colors.layerBg};strokeColor=${colors.layerBorder};strokeWidth=1;" vertex="1" parent="1">
      <mxGeometry x="${contentStartX}" y="${currentY}" width="${maxLayerWidth}" height="${layerHeight}" as="geometry"/>
    </mxCell>`
    
    // 左侧标签
    cells += `<mxCell id="layer-label-${layerIndex}" value="${escapeXml(layer.name || '')}" style="rounded=0;whiteSpace=wrap;html=1;fillColor=${colors.labelBg};strokeColor=none;align=center;verticalAlign=middle;fontSize=11;fontStyle=1;fontColor=${colors.labelText};" vertex="1" parent="1">
      <mxGeometry x="0" y="${currentY}" width="${cfg.labelWidth}" height="${layerHeight}" as="geometry"/>
    </mxCell>`
    
    if (hasBlocks) {
      let blockX = contentStartX + cfg.layerPaddingX
      const blockY = currentY + cfg.layerPaddingY
      
      blockInfos.forEach((bInfo, bIdx) => {
        const { block, blockRows, width: blockW, height: blockH } = bInfo
        const blockColor = colors.blockTitleBg[block.color] || colors.blockTitleBg.blue
        
        cells += `<mxCell id="block-bg-${layerIndex}-${bIdx}" value="" style="rounded=1;whiteSpace=wrap;html=1;fillColor=${colors.blockBg};strokeColor=${colors.blockBorder};strokeWidth=1;" vertex="1" parent="1">
          <mxGeometry x="${blockX}" y="${blockY}" width="${blockW}" height="${blockH}" as="geometry"/>
        </mxCell>`
        
        cells += `<mxCell id="block-title-${layerIndex}-${bIdx}" value="${escapeXml(block.name || '')}" style="rounded=1;whiteSpace=wrap;html=1;fillColor=${blockColor};strokeColor=none;align=center;verticalAlign=middle;fontSize=9;fontStyle=1;fontColor=#FFFFFF;" vertex="1" parent="1">
          <mxGeometry x="${blockX}" y="${blockY}" width="${blockW}" height="${cfg.blockTitleH}" as="geometry"/>
        </mxCell>`
        
        let rowY = blockY + cfg.blockTitleH + cfg.blockPaddingY
        blockRows.forEach(row => {
          let nodeX = blockX + cfg.blockPaddingX
          row.nodes.forEach((node, nIdx) => {
            const nodeId = ensureNodeId(node, `b${layerIndex}-${bIdx}-node`)
            const nodeW = row.widths[nIdx]
            cells += `<mxCell id="${nodeId}" value="${escapeXml(node.label)}" style="rounded=0;whiteSpace=wrap;html=1;fillColor=${colors.nodeBg};strokeColor=${colors.nodeBorder};strokeWidth=1;fontSize=9;fontColor=${colors.nodeText};" vertex="1" parent="1">
              <mxGeometry x="${nodeX}" y="${rowY}" width="${nodeW}" height="${cfg.nodeHeight}" as="geometry"/>
            </mxCell>`
            nodeX += nodeW + cfg.nodeGapX
          })
          rowY += cfg.nodeHeight + cfg.nodeGapY
        })
        
        blockX += blockW + cfg.blockGapX
      })
    } else {
      let rowY = currentY + cfg.layerPaddingY
      nodeRows.forEach(row => {
        let nodeX = contentStartX + cfg.layerPaddingX
        row.nodes.forEach((node, idx) => {
          const nodeId = ensureNodeId(node, `e${layerIndex}-node`)
          const nodeW = row.widths[idx]
          cells += `<mxCell id="${nodeId}" value="${escapeXml(node.label)}" style="rounded=0;whiteSpace=wrap;html=1;fillColor=${colors.nodeBg};strokeColor=${colors.nodeBorder};strokeWidth=1;fontSize=9;fontColor=${colors.nodeText};" vertex="1" parent="1">
            <mxGeometry x="${nodeX}" y="${rowY}" width="${nodeW}" height="${cfg.nodeHeight}" as="geometry"/>
          </mxCell>`
          nodeX += nodeW + cfg.nodeGapX
        })
        rowY += cfg.nodeHeight + cfg.nodeGapY
      })
    }
    
    currentY += layerHeight + cfg.layerGap
  })
  
  return `<mxGraphModel dx="1326" dy="796" grid="1" gridSize="10" guides="1" tooltips="1" connect="1" arrows="1" fold="1" page="1" pageScale="1" pageWidth="827" pageHeight="1169">
    <root>
      <mxCell id="0"/>
      <mxCell id="1" parent="0"/>
      ${cells}
    </root>
  </mxGraphModel>`
}

// ==================== 带侧边栏渲染器 ====================

/**
 * 带侧边栏的企业级架构 - 两阶段渲染
 * 阶段1: 计算所有内容的布局
 * 阶段2: 根据布局统一渲染
 */
function convertLayeredSidebarToDrawio(data) {
  resetNodeIdCounter()
  
  const { layers } = data
  const leftSidebar = data.leftSidebar
  const rightSidebar = data.rightSidebar
  
  if (!layers || !layers.length) return getEmptyDiagram()
  
  const colors = COLORS_ENTERPRISE
  const cfg = {
    sidebarWidth: 90,
    sidebarNodeH: 28,
    sidebarNodeGap: 3,
    sidebarPadding: 5,
    sidebarTitleH: 22,
    mainPaddingX: 8,
    nodeHeight: 24,
    nodeGapX: 6,
    nodeGapY: 4,
    maxRowWidth: 500,
    layerPaddingY: 6,
    layerGap: 2,
    labelWidth: 60,
  }
  
  const leftNodes = leftSidebar?.nodes || []
  const rightNodes = rightSidebar?.nodes || []
  const hasLeft = leftNodes.length > 0
  const hasRight = rightNodes.length > 0
  
  // ========== 阶段1: 预计算布局 ==========
  console.log('[渲染器] 阶段1: 计算布局...')
  
  // 1.1 计算主区域每层的尺寸
  const layerInfos = []
  let maxMainWidth = 350
  
  layers.forEach((layer) => {
    let allNodes = layer.nodes || []
    const nodeRows = groupNodesIntoRows(allNodes, cfg.maxRowWidth, cfg.nodeGapX)
    const maxRowWidth = nodeRows.length > 0 ? Math.max(...nodeRows.map(r => r.totalWidth)) : 0
    const layerWidth = Math.max(maxRowWidth, 80) + cfg.mainPaddingX * 2
    const layerHeight = Math.max(nodeRows.length, 1) * cfg.nodeHeight + 
                        Math.max(nodeRows.length - 1, 0) * cfg.nodeGapY + cfg.layerPaddingY * 2
    
    maxMainWidth = Math.max(maxMainWidth, layerWidth)
    layerInfos.push({ layer, nodeRows, width: layerWidth, height: layerHeight })
  })
  
  // 1.2 计算主区域总高度
  let mainContentHeight = 0
  layerInfos.forEach(info => {
    mainContentHeight += info.height + cfg.layerGap
  })
  mainContentHeight -= cfg.layerGap  // 最后一层不需要间隙
  
  // 1.3 计算侧边栏内容高度
  const leftContentH = hasLeft ? leftNodes.length * cfg.sidebarNodeH + (leftNodes.length - 1) * cfg.sidebarNodeGap : 0
  const rightContentH = hasRight ? rightNodes.length * cfg.sidebarNodeH + (rightNodes.length - 1) * cfg.sidebarNodeGap : 0
  const sidebarContentH = Math.max(leftContentH, rightContentH)
  const sidebarContentWithTitle = cfg.sidebarTitleH + sidebarContentH + cfg.sidebarPadding * 2
  
  // 1.4 统一高度 - 取主区域和侧边栏的最大值
  const unifiedHeight = Math.max(mainContentHeight, sidebarContentWithTitle)
  const sidebarH = unifiedHeight
  
  console.log('[渲染器] 主区域高度:', mainContentHeight, '侧边栏高度:', sidebarContentWithTitle, '统一高度:', unifiedHeight)
  
  // 1.5 计算位置
  const mainStartX = cfg.labelWidth + 2 + (hasLeft ? cfg.sidebarWidth + 8 : 0)
  const canvasWidth = mainStartX + maxMainWidth + 15 + (hasRight ? cfg.sidebarWidth + 8 : 0)
  
  // ========== 阶段2: 统一渲染 ==========
  console.log('[渲染器] 阶段2: 开始渲染...')
  
  let cells = ''
  const startY = 10
  
  // 2.1 渲染左侧边栏（垂直居中对齐节点）
  if (hasLeft) {
    const sidebarX = cfg.labelWidth + 2
    // 计算节点垂直居中的起始位置
    const leftNodesHeight = leftContentH
    const leftStartY = startY + (unifiedHeight - leftNodesHeight - cfg.sidebarTitleH) / 2
    
    cells += `<mxCell id="left-sidebar-bg" value="" style="rounded=1;whiteSpace=wrap;html=1;fillColor=${colors.sidebarBg};strokeColor=${colors.sidebarBorder};strokeWidth=1;" vertex="1" parent="1">
      <mxGeometry x="${sidebarX}" y="${startY}" width="${cfg.sidebarWidth}" height="${unifiedHeight}" as="geometry"/>
    </mxCell>`
    
    cells += `<mxCell id="left-sidebar-title" value="${escapeXml(leftSidebar.title || '监控')}" style="text;html=1;strokeColor=none;fillColor=none;align=center;verticalAlign=middle;fontSize=10;fontStyle=1;fontColor=#1890FF;" vertex="1" parent="1">
      <mxGeometry x="${sidebarX}" y="${startY}" width="${cfg.sidebarWidth}" height="${cfg.sidebarTitleH}" as="geometry"/>
    </mxCell>`
    
    let nodeY = startY + cfg.sidebarTitleH + cfg.sidebarPadding
    leftNodes.forEach((node, idx) => {
      const nodeId = ensureNodeId(node, 'left-node')
      cells += `<mxCell id="${nodeId}" value="${escapeXml(node.label)}" style="rounded=1;whiteSpace=wrap;html=1;fillColor=${colors.sidebarNodeBg};strokeColor=none;fontSize=9;fontColor=${colors.sidebarNodeText};" vertex="1" parent="1">
        <mxGeometry x="${sidebarX + cfg.sidebarPadding}" y="${nodeY}" width="${cfg.sidebarWidth - cfg.sidebarPadding * 2}" height="${cfg.sidebarNodeH}" as="geometry"/>
      </mxCell>`
      nodeY += cfg.sidebarNodeH + cfg.sidebarNodeGap
    })
  }
  
  // 2.2 渲染右侧边栏（垂直居中对齐节点）
  if (hasRight) {
    const sidebarX = mainStartX + maxMainWidth + 8
    // 计算节点垂直居中的起始位置
    const rightNodesHeight = rightContentH
    const rightStartY = startY + (unifiedHeight - rightNodesHeight - cfg.sidebarTitleH) / 2
    
    cells += `<mxCell id="right-sidebar-bg" value="" style="rounded=1;whiteSpace=wrap;html=1;fillColor=${colors.sidebarBg};strokeColor=${colors.sidebarBorder};strokeWidth=1;" vertex="1" parent="1">
      <mxGeometry x="${sidebarX}" y="${startY}" width="${cfg.sidebarWidth}" height="${unifiedHeight}" as="geometry"/>
    </mxCell>`
    
    cells += `<mxCell id="right-sidebar-title" value="${escapeXml(rightSidebar.title || '治理')}" style="text;html=1;strokeColor=none;fillColor=none;align=center;verticalAlign=middle;fontSize=10;fontStyle=1;fontColor=#1890FF;" vertex="1" parent="1">
      <mxGeometry x="${sidebarX}" y="${startY}" width="${cfg.sidebarWidth}" height="${cfg.sidebarTitleH}" as="geometry"/>
    </mxCell>`
    
    let nodeY = startY + cfg.sidebarTitleH + cfg.sidebarPadding
    rightNodes.forEach((node, idx) => {
      const nodeId = ensureNodeId(node, 'right-node')
      cells += `<mxCell id="${nodeId}" value="${escapeXml(node.label)}" style="rounded=1;whiteSpace=wrap;html=1;fillColor=${colors.sidebarNodeBg};strokeColor=none;fontSize=9;fontColor=${colors.sidebarNodeText};" vertex="1" parent="1">
        <mxGeometry x="${sidebarX + cfg.sidebarPadding}" y="${nodeY}" width="${cfg.sidebarWidth - cfg.sidebarPadding * 2}" height="${cfg.sidebarNodeH}" as="geometry"/>
      </mxCell>`
      nodeY += cfg.sidebarNodeH + cfg.sidebarNodeGap
    })
  }
  
  // 2.3 渲染主区域层级
  let currentY = startY
  layerInfos.forEach((info, layerIndex) => {
    const { layer, nodeRows, height: layerHeight } = info
    
    // 层背景
    cells += `<mxCell id="layer-bg-${layerIndex}" value="" style="rounded=0;whiteSpace=wrap;html=1;fillColor=${colors.layerBg};strokeColor=${colors.layerBorder};strokeWidth=1;" vertex="1" parent="1">
      <mxGeometry x="${mainStartX}" y="${currentY}" width="${maxMainWidth}" height="${layerHeight}" as="geometry"/>
    </mxCell>`
    
    // 左侧标签
    cells += `<mxCell id="layer-label-${layerIndex}" value="${escapeXml(layer.name || '')}" style="rounded=0;whiteSpace=wrap;html=1;fillColor=${colors.labelBg};strokeColor=none;align=center;verticalAlign=middle;fontSize=11;fontStyle=1;fontColor=${colors.labelText};" vertex="1" parent="1">
      <mxGeometry x="0" y="${currentY}" width="${cfg.labelWidth}" height="${layerHeight}" as="geometry"/>
    </mxCell>`
    
    // 渲染节点
    let rowY = currentY + cfg.layerPaddingY
    nodeRows.forEach(row => {
      let nodeX = mainStartX + cfg.mainPaddingX
      row.nodes.forEach((node, idx) => {
        const nodeId = ensureNodeId(node, `l${layerIndex}-node`)
        const nodeW = row.widths[idx]
        cells += `<mxCell id="${nodeId}" value="${escapeXml(node.label)}" style="rounded=0;whiteSpace=wrap;html=1;fillColor=${colors.nodeBg};strokeColor=${colors.nodeBorder};strokeWidth=1;fontSize=9;fontColor=${colors.nodeText};" vertex="1" parent="1">
          <mxGeometry x="${nodeX}" y="${rowY}" width="${nodeW}" height="${cfg.nodeHeight}" as="geometry"/>
        </mxCell>`
        nodeX += nodeW + cfg.nodeGapX
      })
      rowY += cfg.nodeHeight + cfg.nodeGapY
    })
    
    currentY += layerHeight + cfg.layerGap
  })
  
  return `<mxGraphModel dx="1326" dy="796" grid="1" gridSize="10" guides="1" tooltips="1" connect="1" arrows="1" fold="1" page="1" pageScale="1" pageWidth="827" pageHeight="1169">
    <root>
      <mxCell id="0"/>
      <mxCell id="1" parent="0"/>
      ${cells}
    </root>
  </mxGraphModel>`
}

// ==================== JSON 提取 ====================

/**
 * 从文本中提取 JSON
 */
export function extractJsonFromContent(content) {
  if (!content) return null
  
  console.log('[extractJson] 开始提取，内容长度:', content.length)
  
  // 方法1：尝试提取 ```json 代码块
  const jsonBlockRegex = /```json\s*([\s\S]*?)```/i
  const match = content.match(jsonBlockRegex)
  
  if (match) {
    console.log('[extractJson] 找到 json 代码块，长度:', match[1].length)
    try {
      const parsed = JSON.parse(match[1].trim())
      console.log('[extractJson] 代码块解析成功')
      return parsed
    } catch (e) {
      console.warn('[extractJson] 代码块解析失败:', e.message)
    }
  }
  
  // 方法2：找到关键字，然后向前后扩展找到完整 JSON
  // 支持多种类型
  let keywordIdx = content.indexOf('"type"')
  if (keywordIdx === -1) keywordIdx = content.indexOf('"layers"')
  if (keywordIdx === -1) keywordIdx = content.indexOf('"nodes"')
  if (keywordIdx === -1) keywordIdx = content.indexOf('"root"')
  
  if (keywordIdx === -1) {
    console.log('[extractJson] 未找到有效关键字')
    return null
  }
  
  // 向前找 { 开始
  let startIdx = content.lastIndexOf('{', keywordIdx)
  if (startIdx === -1) {
    console.log('[extractJson] 未找到 JSON 开始位置')
    return null
  }
  
  // 向后找匹配的 } 结束
  let braceCount = 0
  let endIdx = -1
  let inString = false
  let escaped = false
  
  for (let i = startIdx; i < content.length; i++) {
    const char = content[i]
    
    if (escaped) {
      escaped = false
      continue
    }
    if (char === '\\') {
      escaped = true
      continue
    }
    if (char === '"' && !escaped) {
      inString = !inString
      continue
    }
    if (!inString) {
      if (char === '{') braceCount++
      else if (char === '}') braceCount--
      
      if (braceCount === 0) {
        endIdx = i + 1
        break
      }
    }
  }
  
  if (endIdx > startIdx) {
    const jsonStr = content.substring(startIdx, endIdx)
    console.log('[extractJson] 提取 JSON 字符串，长度:', jsonStr.length)
    try {
      const parsed = JSON.parse(jsonStr)
      console.log('[extractJson] JSON 解析成功')
      return parsed
    } catch (e) {
      console.warn('[extractJson] JSON 解析失败:', e.message)
    }
  }
  
  return null
}

/**
 * 检查 JSON 是否完整 - 支持多种图表类型
 */
export function isJsonComplete(content) {
  if (!content) return false
  
  // 方法1：检查是否有完整的 json 代码块 (```json ... ```)
  const jsonBlockMatch = content.match(/```json([\s\S]*?)```/)
  if (jsonBlockMatch) {
    console.log('[isJsonComplete] 找到完整 json 代码块')
    return true
  }
  
  // 方法2：检查是否有完整的 JSON 对象（括号平衡）
  // 支持多种类型：layers（分层）、nodes（流程/网络）、root（思维导图）
  let keywordIdx = content.indexOf('"type"')
  if (keywordIdx === -1) {
    keywordIdx = content.indexOf('"layers"')
  }
  if (keywordIdx === -1) {
    keywordIdx = content.indexOf('"nodes"')
  }
  if (keywordIdx === -1) {
    keywordIdx = content.indexOf('"root"')
  }
  if (keywordIdx === -1) return false
  
  // 向前找到 { 开始位置
  let jsonStart = content.lastIndexOf('{', keywordIdx)
  if (jsonStart === -1) return false
  
  let braceCount = 0
  let inString = false
  let escaped = false
  
  for (let i = jsonStart; i < content.length; i++) {
    const char = content[i]
    
    if (escaped) {
      escaped = false
      continue
    }
    
    if (char === '\\') {
      escaped = true
      continue
    }
    
    if (char === '"' && !escaped) {
      inString = !inString
      continue
    }
    
    if (!inString) {
      if (char === '{') braceCount++
      else if (char === '}') braceCount--
      
      if (braceCount === 0 && i > jsonStart) {
        console.log('[isJsonComplete] JSON 括号平衡，完整')
        return true
      }
    }
  }
  
  return false
}

// ==================== 流程图渲染器 ====================

/**
 * 流程图渲染 - 支持 nodes + edges
 */
function convertFlowToDrawio(data) {
  resetNodeIdCounter()
  
  const nodes = data.nodes || []
  const edges = data.edges || []
  const title = data.title || '流程图'
  
  if (nodes.length === 0) return getEmptyDiagram()
  
  // 配置
  const cfg = {
    nodeWidth: 100,
    nodeHeight: 40,
    circleSize: 36,
    diamondSize: 50,
    gapX: 80,
    gapY: 60,
    startX: 100,
    startY: 60,
  }
  
  // 形状样式映射
  const shapeStyles = {
    rect: 'rounded=1;whiteSpace=wrap;html=1;fillColor=#E3F2FD;strokeColor=#1976D2;strokeWidth=2;',
    circle: 'ellipse;whiteSpace=wrap;html=1;fillColor=#E8F5E9;strokeColor=#388E3C;strokeWidth=2;',
    diamond: 'rhombus;whiteSpace=wrap;html=1;fillColor=#FFF3E0;strokeColor=#F57C00;strokeWidth=2;',
  }
  
  // 简单布局：从左到右排列
  const nodePositions = {}
  let currentX = cfg.startX
  
  // 按顺序计算节点位置
  nodes.forEach((node, idx) => {
    const shape = node.shape || 'rect'
    let width, height
    
    if (shape === 'circle') {
      width = height = cfg.circleSize
    } else if (shape === 'diamond') {
      width = height = cfg.diamondSize
    } else {
      width = cfg.nodeWidth
      height = cfg.nodeHeight
    }
    
    nodePositions[node.id] = {
      x: currentX,
      y: cfg.startY,
      width,
      height,
      centerX: currentX + width / 2,
      centerY: cfg.startY + height / 2
    }
    
    currentX += width + cfg.gapX
  })
  
  let cells = ''
  
  // 渲染标题
  cells += `<mxCell id="title" value="${escapeXml(title)}" style="text;html=1;strokeColor=none;fillColor=none;align=center;verticalAlign=middle;fontSize=16;fontStyle=1;fontColor=#333333;" vertex="1" parent="1">
    <mxGeometry x="50" y="10" width="${currentX}" height="30" as="geometry"/>
  </mxCell>`
  
  // 渲染节点
  nodes.forEach((node) => {
    const nodeId = ensureNodeId(node, 'flow-node')
    const pos = nodePositions[node.id]
    const shape = node.shape || 'rect'
    const style = shapeStyles[shape] || shapeStyles.rect
    
    cells += `<mxCell id="${nodeId}" value="${escapeXml(node.label)}" style="${style}fontSize=11;fontColor=#333333;" vertex="1" parent="1">
      <mxGeometry x="${pos.x}" y="${pos.y}" width="${pos.width}" height="${pos.height}" as="geometry"/>
    </mxCell>`
  })
  
  // 渲染连线 - 智能避障
  edges.forEach((edge, idx) => {
    const fromPos = nodePositions[edge.from]
    const toPos = nodePositions[edge.to]
    if (!fromPos || !toPos) return
    
    // 计算节点中心和最佳出入点
    const srcCX = fromPos.x + fromPos.width/2
    const srcCY = fromPos.y + fromPos.height/2
    const tgtCX = toPos.x + toPos.width/2
    const tgtCY = toPos.y + toPos.height/2
    const dx = tgtCX - srcCX
    const dy = tgtCY - srcCY
    
    // 检测是否需要绕行
    const needsDetour = nodes.some(n => {
      if (n.id === edge.from || n.id === edge.to) return false
      const pos = nodePositions[n.id]
      if (!pos) return false
      const nodeCX = pos.x + pos.width/2
      const nodeCY = pos.y + pos.height/2
      const minX = Math.min(srcCX, tgtCX) - 10
      const maxX = Math.max(srcCX, tgtCX) + 10
      const minY = Math.min(srcCY, tgtCY) - 10
      const maxY = Math.max(srcCY, tgtCY) + 10
      return nodeCX > minX && nodeCX < maxX && nodeCY > minY && nodeCY < maxY
    })
    
    let exitX = 0.5, exitY = 1, entryX = 0.5, entryY = 0
    if (Math.abs(dx) > Math.abs(dy)) {
      if (dx > 0) { exitX = 1; exitY = 0.5; entryX = 0; entryY = 0.5 }
      else { exitX = 0; exitY = 0.5; entryX = 1; entryY = 0.5 }
    } else {
      if (dy > 0) { exitX = 0.5; exitY = 1; entryX = 0.5; entryY = 0 }
      else { exitX = 0.5; exitY = 0; entryX = 0.5; entryY = 1 }
    }
    
    // 智能路径点计算
    let waypoints = ''
    if (needsDetour) {
      const detourOffset = 50
      if (Math.abs(dx) > Math.abs(dy)) {
        const detourY = Math.min(srcCY, tgtCY) - detourOffset
        waypoints = `<Array as="points"><mxPoint x="${srcCX}" y="${detourY}"/><mxPoint x="${tgtCX}" y="${detourY}"/></Array>`
      } else {
        const detourX = Math.min(srcCX, tgtCX) - detourOffset
        waypoints = `<Array as="points"><mxPoint x="${detourX}" y="${srcCY}"/><mxPoint x="${detourX}" y="${tgtCY}"/></Array>`
      }
    } else if (Math.abs(dx) > 50 && Math.abs(dy) > 50) {
      if (Math.abs(dx) > Math.abs(dy)) {
        waypoints = `<Array as="points"><mxPoint x="${srcCX + dx/2}" y="${srcCY}"/></Array>`
      } else {
        waypoints = `<Array as="points"><mxPoint x="${srcCX}" y="${srcCY + dy/2}"/></Array>`
      }
    }
    
    const edgeLabel = edge.label || ''
    cells += `<mxCell id="edge-${idx}" value="${escapeXml(edgeLabel)}" style="edgeStyle=orthogonalEdgeStyle;rounded=1;orthogonalLoop=1;html=1;exitX=${exitX};exitY=${exitY};exitDx=0;exitDy=0;entryX=${entryX};entryY=${entryY};entryDx=0;entryDy=0;jumpStyle=arc;jumpSize=8;strokeColor=#8C8C8C;strokeWidth=1;endArrow=classic;endFill=1;endSize=5;fontSize=9;labelBackgroundColor=#FFFFFF;" edge="1" parent="1" source="${edge.from}" target="${edge.to}">
      <mxGeometry relative="1" as="geometry">${waypoints}</mxGeometry>
    </mxCell>`
  })
  
  return `<mxGraphModel dx="1326" dy="796" grid="1" gridSize="10" guides="1" tooltips="1" connect="1" arrows="1" fold="1" page="1" pageScale="1" pageWidth="827" pageHeight="1169">
    <root>
      <mxCell id="0"/>
      <mxCell id="1" parent="0"/>
      ${cells}
    </root>
  </mxGraphModel>`
}

// ==================== 思维导图渲染器 ====================

/**
 * 思维导图渲染 - 树形结构
 */
function convertMindmapToDrawio(data) {
  resetNodeIdCounter()
  
  const root = data.root
  const title = data.title || '思维导图'
  
  if (!root) return getEmptyDiagram()
  
  const cfg = {
    rootWidth: 120,
    rootHeight: 50,
    nodeWidth: 90,
    nodeHeight: 32,
    levelGapX: 150,
    siblingGapY: 15,
    startX: 50,
    startY: 200,
  }
  
  const colors = ['#1890FF', '#52C41A', '#FAAD14', '#722ED1', '#EB2F96', '#13C2C2']
  
  let cells = ''
  let edgeId = 0
  
  // 递归渲染节点
  const renderNode = (node, x, y, level, parentId = null, colorIdx = 0) => {
    const nodeId = ensureNodeId(node, `mind-${level}`)
    const isRoot = level === 0
    const width = isRoot ? cfg.rootWidth : cfg.nodeWidth
    const height = isRoot ? cfg.rootHeight : cfg.nodeHeight
    const color = colors[colorIdx % colors.length]
    
    const style = isRoot 
      ? `rounded=1;whiteSpace=wrap;html=1;fillColor=${color};strokeColor=none;fontColor=#FFFFFF;fontStyle=1;fontSize=14;`
      : `rounded=1;whiteSpace=wrap;html=1;fillColor=#FFFFFF;strokeColor=${color};strokeWidth=2;fontColor=#333333;fontSize=11;`
    
    cells += `<mxCell id="${nodeId}" value="${escapeXml(node.label)}" style="${style}" vertex="1" parent="1">
      <mxGeometry x="${x}" y="${y}" width="${width}" height="${height}" as="geometry"/>
    </mxCell>`
    
    // 连线到父节点（从右边出发到左边进入，适合思维导图横向布局）
    if (parentId) {
      cells += `<mxCell id="mind-edge-${edgeId++}" style="edgeStyle=elbowEdgeStyle;elbow=horizontal;rounded=1;html=1;exitX=1;exitY=0.5;exitDx=0;exitDy=0;entryX=0;entryY=0.5;entryDx=0;entryDy=0;jumpStyle=arc;jumpSize=6;strokeColor=${color};strokeWidth=2;endArrow=none;" edge="1" parent="1" source="${parentId}" target="${nodeId}">
        <mxGeometry relative="1" as="geometry"/>
      </mxCell>`
    }
    
    // 渲染子节点
    const children = node.children || []
    if (children.length > 0) {
      const childX = x + cfg.levelGapX
      const totalHeight = children.length * cfg.nodeHeight + (children.length - 1) * cfg.siblingGapY
      let childY = y + height / 2 - totalHeight / 2
      
      children.forEach((child, idx) => {
        renderNode(child, childX, childY, level + 1, nodeId, level === 0 ? idx : colorIdx)
        childY += cfg.nodeHeight + cfg.siblingGapY
      })
    }
    
    return { x, y, width, height }
  }
  
  // 从根节点开始渲染
  renderNode(root, cfg.startX, cfg.startY, 0)
  
  // 标题
  cells += `<mxCell id="mindmap-title" value="${escapeXml(title)}" style="text;html=1;strokeColor=none;fillColor=none;align=left;verticalAlign=middle;fontSize=16;fontStyle=1;fontColor=#333333;" vertex="1" parent="1">
    <mxGeometry x="50" y="10" width="300" height="30" as="geometry"/>
  </mxCell>`
  
  return `<mxGraphModel dx="1326" dy="796" grid="1" gridSize="10" guides="1" tooltips="1" connect="1" arrows="1" fold="1" page="1" pageScale="1" pageWidth="827" pageHeight="1169">
    <root>
      <mxCell id="0"/>
      <mxCell id="1" parent="0"/>
      ${cells}
    </root>
  </mxGraphModel>`
}

// ==================== 网络拓扑渲染器 ====================

/**
 * 网络拓扑渲染 - 按组分区布局
 */
function convertNetworkToDrawio(data) {
  resetNodeIdCounter()
  
  const nodes = data.nodes || []
  const edges = data.edges || []
  const title = data.title || '网络拓扑'
  
  if (nodes.length === 0) return getEmptyDiagram()
  
  const cfg = {
    nodeWidth: 80,
    nodeHeight: 36,
    groupGapX: 200,
    nodeGapY: 50,
    startX: 100,
    startY: 80,
  }
  
  const groupColors = {
    '入口': '#1890FF',
    '核心': '#52C41A', 
    '存储': '#722ED1',
    'default': '#666666'
  }
  
  // 按 group 分组
  const groups = {}
  nodes.forEach(node => {
    const group = node.group || 'default'
    if (!groups[group]) groups[group] = []
    groups[group].push(node)
  })
  
  // 计算节点位置
  const nodePositions = {}
  let groupX = cfg.startX
  
  Object.entries(groups).forEach(([groupName, groupNodes]) => {
    let nodeY = cfg.startY + 30
    
    groupNodes.forEach(node => {
      nodePositions[node.id] = {
        x: groupX,
        y: nodeY,
        group: groupName
      }
      nodeY += cfg.nodeHeight + cfg.nodeGapY
    })
    
    groupX += cfg.groupGapX
  })
  
  let cells = ''
  
  // 标题
  cells += `<mxCell id="network-title" value="${escapeXml(title)}" style="text;html=1;strokeColor=none;fillColor=none;align=left;verticalAlign=middle;fontSize=16;fontStyle=1;fontColor=#333333;" vertex="1" parent="1">
    <mxGeometry x="50" y="10" width="400" height="30" as="geometry"/>
  </mxCell>`
  
  // 渲染组标签和节点
  groupX = cfg.startX
  Object.entries(groups).forEach(([groupName, groupNodes]) => {
    const color = groupColors[groupName] || groupColors.default
    
    // 组标签
    cells += `<mxCell id="group-${groupName}" value="${escapeXml(groupName)}" style="text;html=1;strokeColor=none;fillColor=none;align=center;verticalAlign=middle;fontSize=12;fontStyle=1;fontColor=${color};" vertex="1" parent="1">
      <mxGeometry x="${groupX}" y="${cfg.startY}" width="${cfg.nodeWidth}" height="25" as="geometry"/>
    </mxCell>`
    
    // 组内节点
    groupNodes.forEach(node => {
      const nodeId = ensureNodeId(node, 'net-node')
      const pos = nodePositions[node.id]
      
      cells += `<mxCell id="${nodeId}" value="${escapeXml(node.label)}" style="rounded=1;whiteSpace=wrap;html=1;fillColor=#FFFFFF;strokeColor=${color};strokeWidth=2;fontSize=10;fontColor=#333333;" vertex="1" parent="1">
        <mxGeometry x="${pos.x}" y="${pos.y}" width="${cfg.nodeWidth}" height="${cfg.nodeHeight}" as="geometry"/>
      </mxCell>`
    })
    
    groupX += cfg.groupGapX
  })
  
  // 渲染连线 - 智能避障
  const allNodes = Object.values(groups).flat()
  edges.forEach((edge, idx) => {
    const fromPos = nodePositions[edge.from]
    const toPos = nodePositions[edge.to]
    if (!fromPos || !toPos) return
    
    // 计算节点中心和最佳出入点
    const srcCX = fromPos.x + cfg.nodeWidth/2
    const srcCY = fromPos.y + cfg.nodeHeight/2
    const tgtCX = toPos.x + cfg.nodeWidth/2
    const tgtCY = toPos.y + cfg.nodeHeight/2
    const dx = tgtCX - srcCX
    const dy = tgtCY - srcCY
    
    // 检测是否需要绕行
    const needsDetour = Object.keys(nodePositions).some(nodeId => {
      if (nodeId === edge.from || nodeId === edge.to) return false
      const pos = nodePositions[nodeId]
      if (!pos) return false
      const nodeCX = pos.x + cfg.nodeWidth/2
      const nodeCY = pos.y + cfg.nodeHeight/2
      const minX = Math.min(srcCX, tgtCX) - 10
      const maxX = Math.max(srcCX, tgtCX) + 10
      const minY = Math.min(srcCY, tgtCY) - 10
      const maxY = Math.max(srcCY, tgtCY) + 10
      return nodeCX > minX && nodeCX < maxX && nodeCY > minY && nodeCY < maxY
    })
    
    let exitX = 0.5, exitY = 1, entryX = 0.5, entryY = 0
    if (Math.abs(dx) > Math.abs(dy)) {
      if (dx > 0) { exitX = 1; exitY = 0.5; entryX = 0; entryY = 0.5 }
      else { exitX = 0; exitY = 0.5; entryX = 1; entryY = 0.5 }
    } else {
      if (dy > 0) { exitX = 0.5; exitY = 1; entryX = 0.5; entryY = 0 }
      else { exitX = 0.5; exitY = 0; entryX = 0.5; entryY = 1 }
    }
    
    // 智能路径点计算
    let waypoints = ''
    if (needsDetour) {
      const detourOffset = 50
      if (Math.abs(dx) > Math.abs(dy)) {
        const detourY = Math.min(srcCY, tgtCY) - detourOffset
        waypoints = `<Array as="points"><mxPoint x="${srcCX}" y="${detourY}"/><mxPoint x="${tgtCX}" y="${detourY}"/></Array>`
      } else {
        const detourX = Math.min(srcCX, tgtCX) - detourOffset
        waypoints = `<Array as="points"><mxPoint x="${detourX}" y="${srcCY}"/><mxPoint x="${detourX}" y="${tgtCY}"/></Array>`
      }
    } else if (Math.abs(dx) > 50 && Math.abs(dy) > 50) {
      if (Math.abs(dx) > Math.abs(dy)) {
        waypoints = `<Array as="points"><mxPoint x="${srcCX + dx/2}" y="${srcCY}"/></Array>`
      } else {
        waypoints = `<Array as="points"><mxPoint x="${srcCX}" y="${srcCY + dy/2}"/></Array>`
      }
    }
    
    cells += `<mxCell id="net-edge-${idx}" style="edgeStyle=orthogonalEdgeStyle;rounded=1;orthogonalLoop=1;html=1;exitX=${exitX};exitY=${exitY};exitDx=0;exitDy=0;entryX=${entryX};entryY=${entryY};entryDx=0;entryDy=0;jumpStyle=arc;jumpSize=8;strokeColor=#8C8C8C;strokeWidth=1;endArrow=classic;endSize=5;dashed=1;dashPattern=8 4;" edge="1" parent="1" source="${edge.from}" target="${edge.to}">
      <mxGeometry relative="1" as="geometry">${waypoints}</mxGeometry>
    </mxCell>`
  })
  
  return `<mxGraphModel dx="1326" dy="796" grid="1" gridSize="10" guides="1" tooltips="1" connect="1" arrows="1" fold="1" page="1" pageScale="1" pageWidth="827" pageHeight="1169">
    <root>
      <mxCell id="0"/>
      <mxCell id="1" parent="0"/>
      ${cells}
    </root>
  </mxGraphModel>`
}

// ==================== 智能自动布局渲染器 ====================

/**
 * 智能布局渲染 - 根据语义自动计算位置
 */
function convertSmartToDrawio(data) {
  resetNodeIdCounter()
  
  const nodes = data.nodes || []
  const edges = data.edges || []
  const groups = data.groups || []
  const title = data.title || ''
  const layout = data.layout || {}
  
  if (nodes.length === 0) {
    console.log('[智能渲染] 没有节点，返回空图')
    return getEmptyDiagram()
  }
  
  console.log('[智能渲染] 开始渲染，节点数:', nodes.length, '边数:', edges.length)
  
  // ========== 统一配色方案（专业架构图标准） ==========
  // 按照整改文档要求的配色方案
  const COLOR_PALETTE = {
    // 服务类 - 蓝色系
    service: '#1890FF',
    // 数据类 - 绿色
    database: '#52C41A',
    data: '#52C41A',
    // 中间件 - 青色
    middleware: '#13C2C2',
    cache: '#13C2C2',
    queue: '#13C2C2',
    // 网关 - 橙色
    gateway: '#FA8C16',
    // 第三方/外部 - 紫色
    external: '#722ED1',
    thirdparty: '#722ED1',
    // 基础设施 - 灰色
    infra: '#8C8C8C',
    infrastructure: '#8C8C8C',
    // 用户/客户端 - 蓝绿色
    client: '#1890FF',
    user: '#1890FF',
    // 主色系备选（用于服务节点）
    primary: ['#1890FF', '#40A9FF', '#69C0FF', '#36CFC9'],
    // 分组背景色
    groupBg: ['#F0F5FF', '#F6FFED', '#FFF7E6', '#F9F0FF', '#E6FFFB'],
    // 边框和连线
    border: '#D9D9D9',
    edge: '#8C8C8C',
  }
  
  // 根据节点类型/样式智能分配颜色
  const getNodeColor = (node, index) => {
    // 1. 如果节点指定了颜色，直接使用
    if (node.color) return node.color
    
    // 2. 根据节点的 type 属性分配颜色
    const nodeType = (node.type || '').toLowerCase()
    if (nodeType.includes('database') || nodeType.includes('db') || nodeType.includes('data')) {
      return COLOR_PALETTE.database
    }
    if (nodeType.includes('cache') || nodeType.includes('redis')) {
      return COLOR_PALETTE.cache
    }
    if (nodeType.includes('queue') || nodeType.includes('mq') || nodeType.includes('kafka') || nodeType.includes('middleware')) {
      return COLOR_PALETTE.middleware
    }
    if (nodeType.includes('gateway') || nodeType.includes('api')) {
      return COLOR_PALETTE.gateway
    }
    if (nodeType.includes('external') || nodeType.includes('third') || nodeType.includes('外部')) {
      return COLOR_PALETTE.external
    }
    if (nodeType.includes('infra') || nodeType.includes('监控') || nodeType.includes('日志')) {
      return COLOR_PALETTE.infra
    }
    if (nodeType.includes('client') || nodeType.includes('user') || nodeType.includes('用户') || nodeType.includes('前端')) {
      return COLOR_PALETTE.client
    }
    if (nodeType.includes('service') || nodeType.includes('服务')) {
      return COLOR_PALETTE.service
    }
    
    // 3. 根据样式类型分配颜色
    const style = node.style || 'rounded'
    if (style === 'database' || style === 'cylinder' || style === 'storage') {
      return COLOR_PALETTE.database
    }
    if (style === 'hexagon') {
      return COLOR_PALETTE.gateway
    }
    if (style === 'cloud') {
      return COLOR_PALETTE.external
    }
    if (style === 'cache' || style === 'queue' || style === 'eventbus') {
      return COLOR_PALETTE.middleware
    }
    
    // 4. 默认使用主色系循环
    return COLOR_PALETTE.primary[index % COLOR_PALETTE.primary.length]
  }
  
  // ========== 第一步：自动布局计算 ==========
  // 如果 AI 没有指定布局类型，使用 'auto' 触发智能推断
  const layoutType = layout.type || layout.direction || 'auto'
  console.log('[智能渲染] 布局类型:', layoutType, layout.type ? '(AI指定)' : '(智能推断)')
  
  const nodePositions = calculateAutoLayout(nodes, edges, groups, layoutType, layout)
  console.log('[智能渲染] 布局计算完成，位置数:', Object.keys(nodePositions).length)
  
  // ========== 第二步：渲染图表 ==========
  // 丰富的形状样式映射 - 使用稳定的形状避免渲染问题
  const styleMap = {
    rect: 'rounded=0;whiteSpace=wrap;html=1;',
    rounded: 'rounded=1;whiteSpace=wrap;html=1;arcSize=20;',
    // 圆形和椭圆使用高圆角矩形模拟，避免 ellipse 渲染问题
    circle: 'rounded=1;whiteSpace=wrap;html=1;arcSize=50;',
    ellipse: 'rounded=1;whiteSpace=wrap;html=1;arcSize=50;',
    oval: 'rounded=1;whiteSpace=wrap;html=1;arcSize=50;',
    diamond: 'rhombus;whiteSpace=wrap;html=1;',
    // 数据库使用标准圆柱体
    database: 'shape=cylinder3;whiteSpace=wrap;html=1;boundedLbl=1;backgroundOutline=1;size=12;',
    cylinder: 'shape=cylinder3;whiteSpace=wrap;html=1;boundedLbl=1;backgroundOutline=1;size=12;',
    cloud: 'rounded=1;whiteSpace=wrap;html=1;arcSize=40;glass=1;',  // 云形用玻璃效果圆角
    hexagon: 'shape=hexagon;perimeter=hexagonPerimeter2;whiteSpace=wrap;html=1;fixedSize=1;size=15;',
    parallelogram: 'shape=parallelogram;perimeter=parallelogramPerimeter;whiteSpace=wrap;html=1;fixedSize=1;size=15;',
    trapezoid: 'shape=trapezoid;perimeter=trapezoidPerimeter;whiteSpace=wrap;html=1;fixedSize=1;size=15;',
    process: 'rounded=1;whiteSpace=wrap;html=1;arcSize=40;',
    document: 'shape=document;whiteSpace=wrap;html=1;boundedLbl=1;size=0.2;',
    card: 'shape=card;whiteSpace=wrap;html=1;size=20;',
    note: 'shape=note;whiteSpace=wrap;html=1;backgroundOutline=1;size=20;',
    actor: 'shape=umlActor;verticalLabelPosition=bottom;verticalAlign=top;html=1;',
    queue: 'shape=cylinder3;whiteSpace=wrap;html=1;boundedLbl=1;backgroundOutline=1;size=12;',  // 消息队列用标准圆柱
    // 新增：存储类型
    storage: 'shape=cylinder3;whiteSpace=wrap;html=1;boundedLbl=1;backgroundOutline=1;size=12;',
    cache: 'rounded=1;whiteSpace=wrap;html=1;arcSize=30;dashed=1;dashPattern=5 2;',  // 缓存用虚线圆角
    eventbus: 'rounded=1;whiteSpace=wrap;html=1;arcSize=25;',  // 事件总线用圆角
  }
  
  // 图标 Unicode 映射（使用特殊字符作为简单图标）
  const iconMap = {
    server: '🖥️',
    database: '🗄️',
    user: '👤',
    cloud: '☁️',
    gateway: '🚪',
    service: '⚙️',
    cache: '📦',
    queue: '📬',
    file: '📄',
    api: '🔌',
    web: '🌐',
    mobile: '📱',
    container: '📦',
    kubernetes: '☸️',
    docker: '🐳',
    lock: '🔒',
    key: '🔑',
    mail: '📧',
    search: '🔍',
    chart: '📊',
  }
  
  const edgeStyleMap = {
    solid: 'strokeWidth=2;',
    dashed: 'strokeWidth=2;dashed=1;dashPattern=8 4;',
    dotted: 'strokeWidth=2;dashed=1;dashPattern=2 2;',
  }
  
  let cells = ''
  
  // 渲染标题
  if (title) {
    cells += `<mxCell id="smart-title" value="${escapeXml(title)}" style="text;html=1;strokeColor=none;fillColor=none;align=left;verticalAlign=middle;fontSize=16;fontStyle=1;fontColor=#333333;" vertex="1" parent="1">
      <mxGeometry x="50" y="15" width="600" height="30" as="geometry"/>
    </mxCell>`
  }
  
  // 渲染分组背景 - 专业样式
  const groupColors = [
    { bg: '#FFF9E6', border: '#FAAD14', text: '#D48806' },  // 黄色系
    { bg: '#F0F5FF', border: '#597EF7', text: '#2F54EB' },  // 蓝色系
    { bg: '#F6FFED', border: '#73D13D', text: '#389E0D' },  // 绿色系
    { bg: '#FFF0F6', border: '#F759AB', text: '#C41D7F' },  // 粉色系
    { bg: '#E6FFFB', border: '#36CFC9', text: '#08979C' },  // 青色系
    { bg: '#F9F0FF', border: '#B37FEB', text: '#722ED1' },  // 紫色系
  ]
  
  const groupPositions = calculateGroupBounds(groups, nodes, nodePositions)
  groups.forEach((group, idx) => {
    const gpos = groupPositions[group.id] || groupPositions[idx] || {}
    const colorScheme = groupColors[idx % groupColors.length]
    const gcolor = group.color || colorScheme.bg
    const gborder = group.borderColor || colorScheme.border
    const gtextColor = colorScheme.text
    const gdashed = group.style === 'dashed' ? 'dashed=1;dashPattern=8 4;' : ''
    
    cells += `<mxCell id="group-bg-${idx}" value="${escapeXml(group.label || '')}" style="rounded=1;whiteSpace=wrap;html=1;fillColor=${gcolor};strokeColor=${gborder};strokeWidth=2;${gdashed}verticalAlign=top;align=left;spacingLeft=12;spacingTop=10;fontSize=13;fontStyle=1;fontColor=${gtextColor};" vertex="1" parent="1">
      <mxGeometry x="${gpos.x || 50}" y="${gpos.y || 50}" width="${gpos.width || 200}" height="${gpos.height || 150}" as="geometry"/>
    </mxCell>`
  })
  
  // 渲染节点 - 使用统一配色方案
  nodes.forEach((node, nodeIndex) => {
    // 使用原始 node.id 查找位置（在 validateAndFixData 中已确保有 ID）
    const nodeId = node.id
    const pos = nodePositions[nodeId] || { x: 100 + nodeIndex * 150, y: 100, width: 120, height: 50 }
    
    // 如果位置查找失败，记录日志
    if (!nodePositions[nodeId]) {
      console.warn(`[渲染] 节点 "${nodeId}" 位置未找到，使用默认位置`)
    }
    const color = getNodeColor(node, nodeIndex)  // 使用统一配色
    const nodeStyle = node.style || 'rounded'
    const icon = node.icon ? iconMap[node.icon] : ''
    
    // 组合标签（图标 + 文字）
    const label = icon ? `${icon} ${node.label}` : node.label
    
    let style = styleMap[nodeStyle] || styleMap.rounded
    
    // 统一的样式方案：深色填充 + 白色文字，特殊形状用浅色背景
    const lightBg = smartAdjustColor(color, 70)  // 更浅的背景
    const darkStroke = smartAdjustColor(color, -20)
    
    if (nodeStyle === 'database' || nodeStyle === 'cylinder') {
      // 数据库：浅绿背景 + 深色边框
      style += `fillColor=${lightBg};strokeColor=${color};strokeWidth=2;fontColor=#333333;fontSize=10;`
    } else if (nodeStyle === 'diamond') {
      // 网关/决策：紫色调
      style += `fillColor=#F9F0FF;strokeColor=${color};strokeWidth=2;fontColor=#333333;fontSize=10;`
    } else if (nodeStyle === 'cloud') {
      // 外部系统：灰色调
      style += `fillColor=#FAFAFA;strokeColor=${color};strokeWidth=1;fontColor=#333333;fontSize=10;`
    } else if (nodeStyle === 'hexagon') {
      // 消息队列：橙色调
      style += `fillColor=#FFF7E6;strokeColor=${color};strokeWidth=2;fontColor=#333333;fontSize=10;`
    } else if (nodeStyle === 'circle') {
      // 圆形：实色填充
      style += `fillColor=${color};strokeColor=${darkStroke};fontColor=#FFFFFF;fontSize=11;fontStyle=1;`
    } else {
      // 默认矩形：蓝色实色填充 + 白色文字
      style += `fillColor=${color};strokeColor=${darkStroke};strokeWidth=1;fontColor=#FFFFFF;fontSize=11;shadow=1;`
    }
    
    cells += `<mxCell id="${nodeId}" value="${escapeXml(label)}" style="${style}" vertex="1" parent="1">
      <mxGeometry x="${pos.x}" y="${pos.y}" width="${pos.width}" height="${pos.height}" as="geometry"/>
    </mxCell>`
  })
  
  // ========== 第三步：专业连线渲染 ==========
  const nodeIdSet = new Set(nodes.map(n => n.id))
  
  const validEdges = edges.filter(edge => {
    const source = edge.source || edge.from
    const target = edge.target || edge.to
    return source && target && nodeIdSet.has(source) && nodeIdSet.has(target)
  })
  
  console.log('[连线] 渲染', validEdges.length, '条边')
  
  // 统计每个节点的出入边数量
  const edgeStats = {}
  nodes.forEach(n => {
    edgeStats[n.id] = { outCount: 0, inCount: 0, outIdx: 0, inIdx: 0 }
  })
  
  validEdges.forEach(edge => {
    const src = edge.source || edge.from
    const tgt = edge.target || edge.to
    if (edgeStats[src]) edgeStats[src].outCount++
    if (edgeStats[tgt]) edgeStats[tgt].inCount++
  })
  
  // 渲染连线
  validEdges.forEach((edge, idx) => {
    const source = edge.source || edge.from
    const target = edge.target || edge.to
    const srcPos = nodePositions[source]
    const tgtPos = nodePositions[target]
    if (!srcPos || !tgtPos) return
    
    const label = edge.label || ''
    const edgeColor = edge.color || '#666666'
    const lineStyle = edge.style || 'solid'
    
    // 计算锚点位置（分散多条边）
    const srcStats = edgeStats[source] || { outCount: 1, outIdx: 0 }
    const tgtStats = edgeStats[target] || { inCount: 1, inIdx: 0 }
    
    const exitX = getAnchorOffset(srcStats.outIdx, srcStats.outCount)
    const entryX = getAnchorOffset(tgtStats.inIdx, tgtStats.inCount)
    
    srcStats.outIdx++
    tgtStats.inIdx++
    
    // 判断连接方向
    const dx = tgtPos.x + tgtPos.width/2 - (srcPos.x + srcPos.width/2)
    const dy = tgtPos.y - srcPos.y
    const isDownward = dy > 50  // 明显向下
    const isUpward = dy < -50   // 明显向上
    const isHorizontal = Math.abs(dx) > Math.abs(dy) * 2  // 主要水平方向
    
    // 简洁连线样式 - 使用 entityRelationEdgeStyle 更清晰
    let style = 'edgeStyle=entityRelationEdgeStyle;rounded=1;orthogonalLoop=1;html=1;'
    
    // 根据方向设置锚点
    if (isHorizontal) {
      // 水平连接：从右边出，到左边入
      if (dx > 0) {
        style += `exitX=1;exitY=0.5;exitDx=0;exitDy=0;`
        style += `entryX=0;entryY=0.5;entryDx=0;entryDy=0;`
      } else {
        style += `exitX=0;exitY=0.5;exitDx=0;exitDy=0;`
        style += `entryX=1;entryY=0.5;entryDx=0;entryDy=0;`
      }
    } else if (isDownward) {
      // 向下连接
      style += `exitX=${exitX};exitY=1;exitDx=0;exitDy=0;`
      style += `entryX=${entryX};entryY=0;entryDx=0;entryDy=0;`
    } else if (isUpward) {
      // 向上连接
      style += `exitX=${exitX};exitY=0;exitDx=0;exitDy=0;`
      style += `entryX=${entryX};entryY=1;entryDx=0;entryDy=0;`
    } else {
      // 默认：从下到上
      style += `exitX=0.5;exitY=1;exitDx=0;exitDy=0;`
      style += `entryX=0.5;entryY=0;entryDx=0;entryDy=0;`
    }
    
    // 线条样式 - 更细更专业
    style += `strokeColor=${edgeColor};strokeWidth=1.5;`
    
    if (lineStyle === 'dashed') {
      style += 'dashed=1;dashPattern=6 3;'
    }
    
    // 箭头 - 更小更精致
    style += 'endArrow=classic;endFill=1;endSize=5;'
    
    // 标签 - 更清晰
    if (label) {
      style += 'fontSize=9;fontColor=#666666;labelBackgroundColor=#FFFFFF;labelPadding=3;'
    }
    
    cells += `<mxCell id="edge-${idx}" value="${escapeXml(label)}" style="${style}" edge="1" parent="1" source="${source}" target="${target}">
      <mxGeometry relative="1" as="geometry"/>
    </mxCell>`
  })
  
  // 辅助函数：计算锚点偏移
  function getAnchorOffset(idx, total) {
    if (total <= 1) return 0.5
    if (total === 2) return idx === 0 ? 0.35 : 0.65
    // 在 [0.2, 0.8] 范围内均匀分布
    return 0.2 + (0.6 * idx) / (total - 1)
  }
  
  return `<mxGraphModel dx="1326" dy="796" grid="1" gridSize="10" guides="1" tooltips="1" connect="1" arrows="1" fold="1" page="1" pageScale="1" pageWidth="827" pageHeight="1169">
    <root>
      <mxCell id="0"/>
      <mxCell id="1" parent="0"/>
      ${cells}
    </root>
  </mxGraphModel>`
}

/**
 * 智能推断最佳布局方向
 */
function inferBestLayoutDirection(nodes, edges) {
  if (nodes.length <= 1) return 'horizontal'
  
  // 分析边的方向性
  let horizontalScore = 0
  let verticalScore = 0
  
  // 检查是否有明确的层级结构
  const hasExplicitLayers = nodes.some(n => n.layer !== undefined)
  if (hasExplicitLayers) {
    const layers = [...new Set(nodes.map(n => n.layer || 0))]
    if (layers.length >= 3) {
      verticalScore += 30  // 多层级倾向垂直布局
    }
  }
  
  // 分析边的流向
  edges.forEach(e => {
    const src = nodes.find(n => n.id === (e.source || e.from))
    const tgt = nodes.find(n => n.id === (e.target || e.to))
    if (src && tgt && src.layer !== undefined && tgt.layer !== undefined) {
      if (src.layer < tgt.layer) {
        verticalScore += 5  // 从上到下的流向
      }
    }
  })
  
  // 分析节点数量
  if (nodes.length > 10) {
    verticalScore += 10  // 节点多时垂直布局更清晰
  }
  
  // 检查是否有中心节点（入度和出度都高）
  const nodeConnections = {}
  nodes.forEach(n => nodeConnections[n.id] = { in: 0, out: 0 })
  edges.forEach(e => {
    const src = e.source || e.from
    const tgt = e.target || e.to
    if (nodeConnections[src]) nodeConnections[src].out++
    if (nodeConnections[tgt]) nodeConnections[tgt].in++
  })
  
  const hasCentralHub = Object.values(nodeConnections).some(c => c.in >= 3 && c.out >= 3)
  if (hasCentralHub) {
    return 'radial'  // 有中心枢纽节点时使用放射布局
  }
  
  console.log('[布局推断] 水平分:', horizontalScore, '垂直分:', verticalScore)
  return verticalScore > horizontalScore ? 'vertical' : 'horizontal'
}

/**
 * 检测并修复节点重叠 - 增强版
 */
function detectAndFixOverlaps(positions, padding = 20) {
  const nodeIds = Object.keys(positions)
  if (nodeIds.length < 2) return positions
  
  let hasOverlap = true
  let iterations = 0
  const maxIterations = 100  // 增加迭代次数
  
  while (hasOverlap && iterations < maxIterations) {
    hasOverlap = false
    iterations++
    
    for (let i = 0; i < nodeIds.length; i++) {
      for (let j = i + 1; j < nodeIds.length; j++) {
        const a = positions[nodeIds[i]]
        const b = positions[nodeIds[j]]
        
        if (!a || !b) continue
        
        // 检测重叠（使用更大的间距）
        const minGap = padding
        const overlapX = (a.x - minGap < b.x + b.width) && (a.x + a.width + minGap > b.x)
        const overlapY = (a.y - minGap < b.y + b.height) && (a.y + a.height + minGap > b.y)
        
        if (overlapX && overlapY) {
          hasOverlap = true
          
          // 计算分离向量
          const centerAX = a.x + a.width / 2
          const centerAY = a.y + a.height / 2
          const centerBX = b.x + b.width / 2
          const centerBY = b.y + b.height / 2
          
          let dx = centerBX - centerAX
          let dy = centerBY - centerAY
          const dist = Math.sqrt(dx * dx + dy * dy)
          
          // 如果两节点几乎重合，随机分离方向
          if (dist < 1) {
            dx = (Math.random() - 0.5) * 2
            dy = (Math.random() - 0.5) * 2
          }
          
          const normDist = dist || 1
          
          // 计算需要的最小分离距离
          const requiredSepX = (a.width + b.width) / 2 + minGap
          const requiredSepY = (a.height + b.height) / 2 + minGap
          
          // 分离力度更大
          const separateForce = Math.max(padding, 30)
          const separateX = (dx / normDist) * separateForce
          const separateY = (dy / normDist) * separateForce
          
          a.x -= separateX
          a.y -= separateY
          b.x += separateX
          b.y += separateY
        }
      }
    }
  }
  
  if (iterations > 1) {
    console.log('[重叠检测] 修复重叠，迭代次数:', iterations)
  }
  
  return positions
}

/**
 * 智能约束布局算法 - 支持 AI 指定的布局约束
 */
function calculateAutoLayout(nodes, edges, groups, layoutType, layoutConfig) {
  const positions = {}
  const constraints = layoutConfig.constraints || {}
  
  // 智能间距调整：使用更大的默认间距
  let rankSep = constraints.spacing?.vertical || layoutConfig.rankSep || 120  // 增大默认层间距
  let nodeSep = constraints.spacing?.horizontal || layoutConfig.nodeSep || 80  // 增大默认节点间距
  
  // 根据节点数量微调间距
  const nodeCount = nodes.length
  if (nodeCount > 20) {
    // 节点非常多时适当减小间距
    rankSep = Math.max(80, rankSep * 0.8)
    nodeSep = Math.max(60, nodeSep * 0.8)
    console.log('[布局] 节点较多，微调间距:', { rankSep, nodeSep })
  } else if (nodeCount < 5) {
    // 节点很少时增大间距
    rankSep = Math.min(180, rankSep * 1.3)
    nodeSep = Math.min(120, nodeSep * 1.3)
    console.log('[布局] 节点较少，增大间距:', { rankSep, nodeSep })
  }
  
  // 如果没有指定布局类型，智能推断
  if (!layoutType || layoutType === 'auto') {
    layoutType = inferBestLayoutDirection(nodes, edges)
    console.log('[布局] 智能推断布局方向:', layoutType)
  }
  
  // 计算节点尺寸 - 根据样式和内容自适应
  const getNodeSize = (node) => {
    const size = node.size || 'medium'
    const style = node.style || 'rounded'
    const hasIcon = !!node.icon
    
    // 基础尺寸映射 - 确保所有形状有合适的尺寸
    const sizeMap = {
      circle: { large: { w: 80, h: 50 }, medium: { w: 65, h: 42 }, small: { w: 50, h: 34 } },
      ellipse: { large: { w: 100, h: 50 }, medium: { w: 85, h: 42 }, small: { w: 70, h: 34 } },
      oval: { large: { w: 100, h: 50 }, medium: { w: 85, h: 42 }, small: { w: 70, h: 34 } },
      diamond: { large: { w: 90, h: 90 }, medium: { w: 75, h: 75 }, small: { w: 60, h: 60 } },
      database: { large: { w: 80, h: 70 }, medium: { w: 65, h: 58 }, small: { w: 52, h: 46 } },
      cylinder: { large: { w: 80, h: 70 }, medium: { w: 65, h: 58 }, small: { w: 52, h: 46 } },
      hexagon: { large: { w: 100, h: 70 }, medium: { w: 85, h: 60 }, small: { w: 70, h: 50 } },
      parallelogram: { large: { w: 130, h: 50 }, medium: { w: 110, h: 42 }, small: { w: 90, h: 34 } },
      cloud: { large: { w: 120, h: 55 }, medium: { w: 100, h: 46 }, small: { w: 80, h: 38 } },
      queue: { large: { w: 80, h: 70 }, medium: { w: 65, h: 58 }, small: { w: 52, h: 46 } },
      storage: { large: { w: 80, h: 70 }, medium: { w: 65, h: 58 }, small: { w: 52, h: 46 } },
      cache: { large: { w: 100, h: 50 }, medium: { w: 85, h: 42 }, small: { w: 70, h: 34 } },
      eventbus: { large: { w: 100, h: 50 }, medium: { w: 85, h: 42 }, small: { w: 70, h: 34 } },
    }
    
    if (sizeMap[style]) {
      return sizeMap[style][size] || sizeMap[style].medium
    }
    
    // rounded, rect 等根据 label 长度计算（增大宽度）
    const labelLen = (node.label || '').length
    // 中文字符占用更多宽度
    const chineseCount = ((node.label || '').match(/[\u4e00-\u9fa5]/g) || []).length
    const baseW = Math.max(100, Math.min(220, labelLen * 8 + chineseCount * 6 + 50))
    const iconExtra = hasIcon ? 24 : 0
    
    if (size === 'large') return { w: baseW + 40 + iconExtra, h: 60 }
    if (size === 'small') return { w: Math.max(80, baseW - 20), h: 36 }
    return { w: baseW + iconExtra, h: 46 }
  }
  
  // 构建邻接表
  const adj = {}
  const inDegree = {}
  nodes.forEach(n => {
    adj[n.id] = []
    inDegree[n.id] = 0
  })
  edges.forEach(e => {
    const src = e.source || e.from
    const tgt = e.target || e.to
    if (adj[src]) adj[src].push(tgt)
    if (inDegree[tgt] !== undefined) inDegree[tgt]++
  })
  
  // 智能分层：优先使用 AI 指定的 layer，否则拓扑排序
  let layers = []
  const hasExplicitLayers = nodes.some(n => n.layer !== undefined)
  
  if (hasExplicitLayers) {
    // 使用 AI 指定的层级
    const layerMap = {}
    nodes.forEach(n => {
      const layer = n.layer || 0
      if (!layerMap[layer]) layerMap[layer] = []
      layerMap[layer].push(n.id)
    })
    // 按层级排序
    const sortedKeys = Object.keys(layerMap).map(Number).sort((a, b) => a - b)
    layers = sortedKeys.map(k => layerMap[k])
    console.log('[布局] 使用 AI 指定层级，共', layers.length, '层，节点分布:', layers.map(l => l.length))
  } else {
    // 拓扑排序自动分层
    const visited = new Set()
    const startNodes = nodes.filter(n => inDegree[n.id] === 0).map(n => n.id)
    let queue = [...startNodes]
    
    console.log('[布局] 拓扑排序开始，起始节点:', startNodes.length)
    
    // 如果没有起始节点（所有节点都有入边），选择第一个节点
    if (queue.length === 0 && nodes.length > 0) {
      queue = [nodes[0].id]
    }
    
    while (visited.size < nodes.length) {
      if (queue.length === 0) {
        const unvisited = nodes.find(n => !visited.has(n.id))
        if (unvisited) queue.push(unvisited.id)
        else break
      }
      
      const layer = []
      const nextQueue = []
      
      // 处理当前层的所有节点
      const currentQueue = [...queue]
      queue = []
      
      currentQueue.forEach(nodeId => {
        if (visited.has(nodeId)) return
        visited.add(nodeId)
        layer.push(nodeId)
        
        ;(adj[nodeId] || []).forEach(next => {
          inDegree[next]--
          if (inDegree[next] <= 0 && !visited.has(next)) {
            nextQueue.push(next)
          }
        })
      })
      
      if (layer.length > 0) layers.push(layer)
      queue = nextQueue
    }
    console.log('[布局] 拓扑排序分层，共', layers.length, '层，节点分布:', layers.map(l => l.length))
  }
  
  // 应用分组约束：将分组内的节点放在同一层
  if (groups && groups.length > 0) {
    groups.forEach(group => {
      const children = group.children || group.nodes || []
      if (children.length < 2) return
      
      // 找到第一个子节点所在的层
      const firstChild = children[0]
      const targetLayer = layers.findIndex(l => l.includes(firstChild))
      if (targetLayer === -1) return
      
      // 将其他子节点移动到同一层
      children.slice(1).forEach(childId => {
        const currentLayer = layers.findIndex(l => l.includes(childId))
        if (currentLayer !== -1 && currentLayer !== targetLayer) {
          layers[currentLayer] = layers[currentLayer].filter(id => id !== childId)
          if (!layers[targetLayer].includes(childId)) {
            layers[targetLayer].push(childId)
          }
        }
      })
    })
    console.log('[布局] 应用分组约束')
  }
  
  // 应用对齐约束
  const alignments = constraints.alignments || []
  alignments.forEach(alignment => {
    const alignNodes = alignment.nodes || []
    if (alignNodes.length < 2) return
    
    // 找到这些节点所在的层，合并到同一层
    const targetLayer = layers.findIndex(l => l.includes(alignNodes[0]))
    if (targetLayer === -1) return
    
    alignNodes.forEach(nodeId => {
      const currentLayer = layers.findIndex(l => l.includes(nodeId))
      if (currentLayer !== -1 && currentLayer !== targetLayer) {
        // 移动到目标层
        layers[currentLayer] = layers[currentLayer].filter(id => id !== nodeId)
        if (!layers[targetLayer].includes(nodeId)) {
          layers[targetLayer].push(nodeId)
        }
      }
    })
  })
  
  // 清理空层
  layers = layers.filter(l => l.length > 0)
  
  // 如果没有节点，直接返回
  if (layers.length === 0) {
    console.log('[布局] 没有有效节点')
    return positions
  }
  
  // 根据布局类型计算位置
  const startX = 80
  const startY = 60
  const isHorizontal = layoutType === 'horizontal' || layoutType === 'LR' || layoutType === 'RL'
  
  // 先计算每层的尺寸信息
  const layerSizes = layers.map((layer, layerIdx) => {
    let totalW = 0, totalH = 0, maxW = 0, maxH = 0
    const nodeSizes = []
    layer.forEach(nodeId => {
      const node = nodes.find(n => n.id === nodeId)
      if (!node) {
        console.warn(`[布局] 层${layerIdx}中的节点 "${nodeId}" 未找到`)
        return
      }
      const size = getNodeSize(node)
      nodeSizes.push({ nodeId, w: size.w, h: size.h })
      totalW += size.w
      totalH += size.h
      maxW = Math.max(maxW, size.w)
      maxH = Math.max(maxH, size.h)
    })
    console.log(`[布局] 层${layerIdx}统计: ${nodeSizes.length}节点, 总宽=${totalW}, 最大高=${maxH}`)
    return { layer, nodeSizes, totalW, totalH, maxW, maxH }
  })
  
  // 计算整体尺寸用于居中（防止空数组错误）
  let totalWidth = 0, totalHeight = 0
  if (layerSizes.length === 0) {
    return positions
  }
  
  if (isHorizontal) {
    totalWidth = layerSizes.reduce((sum, l) => sum + l.maxW, 0) + Math.max(0, layers.length - 1) * rankSep
    const heights = layerSizes.map(l => l.totalH + Math.max(0, l.nodeSizes.length - 1) * nodeSep)
    totalHeight = heights.length > 0 ? Math.max(...heights) : 0
  } else {
    const widths = layerSizes.map(l => l.totalW + Math.max(0, l.nodeSizes.length - 1) * nodeSep)
    totalWidth = widths.length > 0 ? Math.max(...widths) : 0
    totalHeight = layerSizes.reduce((sum, l) => sum + l.maxH, 0) + Math.max(0, layers.length - 1) * rankSep
  }
  
  if (isHorizontal) {
    // 水平布局：从左到右，每层内垂直居中
    let x = startX
    layerSizes.forEach((layerInfo, layerIdx) => {
      const layerHeight = layerInfo.totalH + (layerInfo.nodeSizes.length - 1) * nodeSep
      let y = startY + (totalHeight - layerHeight) / 2  // 垂直居中
      
      layerInfo.nodeSizes.forEach(({ nodeId, w, h }) => {
        const xOffset = (layerInfo.maxW - w) / 2  // 水平居中在列内
        positions[nodeId] = { x: x + xOffset, y, width: w, height: h }
        y += h + nodeSep
      })
      
      x += layerInfo.maxW + rankSep
    })
  } else if (layoutType === 'radial') {
    // 放射布局：中心向外
    const centerX = 400
    const centerY = 300
    const ringGap = 120
    
    layerSizes.forEach((layerInfo, layerIdx) => {
      if (layerIdx === 0) {
        // 中心节点
        layerInfo.nodeSizes.forEach(({ nodeId, w, h }) => {
          positions[nodeId] = { x: centerX - w / 2, y: centerY - h / 2, width: w, height: h }
        })
      } else {
        // 外围节点 - 均匀分布
        const radius = ringGap * layerIdx
        const angleStep = (2 * Math.PI) / layerInfo.nodeSizes.length
        
        layerInfo.nodeSizes.forEach(({ nodeId, w, h }, i) => {
          const angle = angleStep * i - Math.PI / 2
          const x = centerX + radius * Math.cos(angle) - w / 2
          const y = centerY + radius * Math.sin(angle) - h / 2
          positions[nodeId] = { x, y, width: w, height: h }
        })
      }
    })
  } else {
    // 垂直布局：从上到下，每层内水平排列
    let y = startY
    
    // 计算画布总宽度（取最宽层的宽度）
    const maxLayerWidth = Math.max(...layerSizes.map(l => 
      l.totalW + Math.max(0, l.nodeSizes.length - 1) * nodeSep
    ), 400)
    
    layerSizes.forEach((layerInfo, layerIdx) => {
      const nodeCount = layerInfo.nodeSizes.length
      if (nodeCount === 0) return
      
      // 计算这一层的总宽度
      const layerWidth = layerInfo.totalW + (nodeCount - 1) * nodeSep
      // 从左侧开始，水平居中
      let x = startX + Math.max(0, (maxLayerWidth - layerWidth) / 2)
      
      console.log(`[布局] 层${layerIdx}: ${nodeCount}个节点, layerWidth=${layerWidth}, 起始x=${x}`)
      
      layerInfo.nodeSizes.forEach(({ nodeId, w, h }, nodeIdx) => {
        const yOffset = (layerInfo.maxH - h) / 2
        positions[nodeId] = { x, y: y + yOffset, width: w, height: h }
        console.log(`[布局]   节点[${nodeIdx}] ${nodeId}: x=${Math.round(x)}, y=${Math.round(y + yOffset)}, w=${w}`)
        // 关键：每个节点后 x 必须增加
        x = x + w + nodeSep
      })
      
      y += layerInfo.maxH + rankSep
    })
  }
  
  // 应用相对位置约束
  const positionConstraints = constraints.positions || []
  positionConstraints.forEach(pc => {
    const nodePos = positions[pc.node]
    if (!nodePos) return
    
    if (pc.relativeTo && pc.direction) {
      const refPos = positions[pc.relativeTo]
      if (!refPos) return
      
      const gap = 20  // 相对位置间距
      switch (pc.direction) {
        case 'below':
          nodePos.y = refPos.y + refPos.height + gap
          nodePos.x = refPos.x + (refPos.width - nodePos.width) / 2  // 水平居中
          break
        case 'above':
          nodePos.y = refPos.y - nodePos.height - gap
          nodePos.x = refPos.x + (refPos.width - nodePos.width) / 2
          break
        case 'right':
          nodePos.x = refPos.x + refPos.width + gap
          nodePos.y = refPos.y + (refPos.height - nodePos.height) / 2  // 垂直居中
          break
        case 'left':
          nodePos.x = refPos.x - nodePos.width - gap
          nodePos.y = refPos.y + (refPos.height - nodePos.height) / 2
          break
      }
    }
  })
  
  // 最后一步：检测并修复节点重叠（使用更大的间距）
  detectAndFixOverlaps(positions, 40)
  
  return positions
}

/**
 * 计算分组边界（不修改节点位置，只计算包围盒）
 */
function calculateGroupBounds(groups, nodes, nodePositions) {
  const bounds = {}
  
  groups.forEach((group, idx) => {
    const children = group.children || group.nodes || []
    if (children.length === 0) {
      bounds[group.id || idx] = { x: 50, y: 50, width: 200, height: 150 }
      return
    }
    
    // 计算分组内节点的边界（不修改位置）
    const padding = 25
    let minX = Infinity, minY = Infinity, maxX = -Infinity, maxY = -Infinity
    
    children.forEach(childId => {
      const pos = nodePositions[childId]
      if (pos) {
        minX = Math.min(minX, pos.x)
        minY = Math.min(minY, pos.y)
        maxX = Math.max(maxX, pos.x + pos.width)
        maxY = Math.max(maxY, pos.y + pos.height)
      }
    })
    
    if (minX !== Infinity) {
      bounds[group.id || idx] = {
        x: minX - padding,
        y: minY - padding - 30, // 留出标题空间
        width: maxX - minX + padding * 2,
        height: maxY - minY + padding * 2 + 30
      }
    } else {
      bounds[group.id || idx] = { x: 50, y: 50, width: 200, height: 150 }
    }
  })
  
  return bounds
}

// 辅助函数：调整颜色亮度
function smartAdjustColor(hex, amount) {
  try {
    const num = parseInt(hex.replace('#', ''), 16)
    const r = Math.min(255, Math.max(0, (num >> 16) + amount))
    const g = Math.min(255, Math.max(0, ((num >> 8) & 0x00FF) + amount))
    const b = Math.min(255, Math.max(0, (num & 0x0000FF) + amount))
    return '#' + (0x1000000 + r * 0x10000 + g * 0x100 + b).toString(16).slice(1)
  } catch (e) {
    return hex
  }
}

// 辅助函数：判断是否为亮色
function smartIsLightColor(hex) {
  try {
    const num = parseInt(hex.replace('#', ''), 16)
    const r = num >> 16
    const g = (num >> 8) & 0x00FF
    const b = num & 0x0000FF
    const luminance = (0.299 * r + 0.587 * g + 0.114 * b) / 255
    return luminance > 0.5
  } catch (e) {
    return false
  }
}
