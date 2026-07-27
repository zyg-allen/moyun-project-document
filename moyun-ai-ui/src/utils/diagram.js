/**
 * 架构图工具函数
 * 提供通用的文本处理、尺寸计算、数据校验等功能
 */

import {
  ICON_MAP,
  ICON_COLOR_MAP,
  STYLE_MAP,
  DEFAULT_STYLE,
  VALID_COLORS,
  BLOCK_AUTO_COLORS,
  VALIDATION_CONFIG,
  TERM_CORRECTIONS,
} from '@/constants/diagram'

import { logger } from '@/utils/logger'

// ==================== 文本处理 ====================

/**
 * 截断文本，超出长度添加省略号
 * @param {string} text - 原始文本
 * @param {number} maxLen - 最大长度，默认 8
 * @returns {string} 截断后的文本
 */
export function truncateText(text, maxLen = 8) {
  if (!text) return ''
  return text.length > maxLen ? text.slice(0, maxLen - 2) + '..' : text
}

/**
 * XML 特殊字符转义（防止 XSS/XML注入）
 * @param {string} text - 原始文本
 * @returns {string} 转义后的文本
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
 * 中文字符按 11px 计算，英文按 5.5px 计算
 * @param {string} label - 节点标签
 * @param {number} minWidth - 最小宽度，默认 55
 * @param {number} padding - 内边距，默认 12
 * @returns {number} 计算后的宽度
 */
export function calcNodeWidth(label, minWidth = 55, padding = 12) {
  let textWidth = 0
  for (const char of (label || '')) {
    textWidth += /[\u4e00-\u9fa5]/.test(char) ? 11 : 5.5
  }
  return Math.max(minWidth, textWidth + padding)
}

/**
 * 术语纠正 - 修复 AI 常见的术语错误
 * @param {string} text - 原始文本
 * @returns {string} 纠正后的文本
 */
export function fixTerminology(text) {
  if (!text) return text
  let fixed = text
  for (const [wrong, correct] of Object.entries(TERM_CORRECTIONS)) {
    if (fixed.includes(wrong)) {
      logger.log(`🔧 术语纠正: "${wrong}" → "${correct}"`)
      fixed = fixed.replace(new RegExp(wrong, 'g'), correct)
    }
  }
  return fixed
}

// ==================== 图标处理 ====================

/**
 * 获取图标信息（图标字符 + 颜色）
 * @param {string} iconName - 图标名称
 * @returns {{ icon: string, color: string }} 图标信息
 */
export function getIconInfo(iconName) {
  const icon = ICON_MAP[iconName] || ICON_MAP['default']
  const color = ICON_COLOR_MAP[iconName] || ICON_COLOR_MAP['default']
  return { icon, color }
}

/**
 * 获取 Draw.io 样式信息
 * @param {string} iconName - 图标名称
 * @returns {{ color: string, icon: string }} 样式信息
 */
export function getStyleInfo(iconName) {
  return STYLE_MAP[iconName] || DEFAULT_STYLE
}

// ==================== 颜色处理 ====================

/**
 * 修复无效颜色值
 * @param {string} color - 颜色名称
 * @param {string} defaultColor - 默认颜色，默认 'blue'
 * @returns {string} 有效的颜色名称
 */
export function fixColor(color, defaultColor = 'blue') {
  if (!color) return defaultColor
  if (VALID_COLORS.includes(color)) return color
  return defaultColor
}

/**
 * 为 blocks 自动分配颜色
 * @param {Array} layers - 层数据
 * @returns {Array} 处理后的层数据
 */
export function autoAssignBlockColors(layers) {
  let colorIndex = 0
  layers.forEach(layer => {
    const blocks = layer.blocks || layer.groups
    if (blocks) {
      blocks.forEach(block => {
        if (!block.color || block.color === 'blue') {
          block.color = BLOCK_AUTO_COLORS[colorIndex % BLOCK_AUTO_COLORS.length]
          colorIndex++
        }
      })
    }
  })
  return layers
}

// ==================== ID 生成 ====================

/**
 * 生成唯一 ID
 * @param {string} prefix - ID 前缀，默认 'node'
 * @returns {string} 唯一 ID
 */
export function generateId(prefix = 'node') {
  return `${prefix}_${Date.now()}_${Math.random().toString(36).substr(2, 6)}`
}

// ==================== 智能补全 ====================

// 层名称到默认节点的映射（智能增强版）
const LAYER_DEFAULT_NODES = {
  // 基础层级
  '接入层': ['Web端', '移动端', 'API接口'],
  '网关层': ['Nginx', 'API网关', '负载均衡'],
  '应用层': ['核心服务', '业务逻辑', '接口服务'],
  '服务层': ['业务服务', '基础服务', '公共服务'],
  '能力层': ['核心能力', '公共服务', '中间件'],
  '数据层': ['MySQL', 'Redis', '消息队列'],
  '存储层': ['数据库', '对象存储', '缓存'],
  
  // AI/LLM 相关
  '模型层': ['LLM服务', '模型推理', '模型管理'],
  '向量层': ['向量数据库', '向量索引', '相似度检索'],
  'AI层': ['AI服务', '模型调用', '智能分析'],
  'RAG层': ['文档解析', '向量检索', '知识召回'],
  'Agent层': ['Agent执行', '工具调用', '任务编排'],
  
  // 电商相关
  '商品层': ['商品管理', '库存管理', '价格管理'],
  '订单层': ['订单管理', '订单处理', '订单查询'],
  '支付层': ['支付网关', '支付渠道', '对账结算'],
  '物流层': ['物流调度', '配送管理', '轨迹追踪'],
  
  // 微服务相关
  '注册层': ['服务注册', '服务发现', '健康检查'],
  '配置层': ['配置中心', '动态配置', '配置推送'],
  '监控层': ['链路追踪', '日志中心', '告警通知'],
  '治理层': ['限流熔断', '负载均衡', '服务路由'],
  
  // 大数据相关
  '采集层': ['数据采集', 'Flume', 'Kafka'],
  '处理层': ['Spark', 'Flink', '流处理'],
  '分析层': ['数据分析', 'BI报表', '数据挖掘'],
  '计算层': ['批处理', '实时计算', '机器学习'],
  
  // 物联网相关
  '设备层': ['传感器', '终端设备', '边缘网关'],
  '边缘层': ['边缘计算', '本地处理', '数据缓存'],
  '协议层': ['MQTT', 'CoAP', '协议转换'],
}

/**
 * 根据层名称生成默认节点
 * @param {string} layerName - 层名称
 * @param {number} layerIndex - 层索引
 * @returns {Array} 默认节点数组
 */
function getDefaultNodesForLayer(layerName, layerIndex) {
  // 尝试匹配层名称
  for (const [key, labels] of Object.entries(LAYER_DEFAULT_NODES)) {
    if (layerName.includes(key.replace('层', ''))) {
      return labels.map((label, idx) => ({
        id: `auto_${layerIndex}_${idx}`,
        label: label
      }))
    }
  }
  
  // 默认返回通用节点
  return [
    { id: `auto_${layerIndex}_0`, label: '模块A' },
    { id: `auto_${layerIndex}_1`, label: '模块B' }
  ]
}

// ==================== 数据校验 ====================

/**
 * 智能校验和修复架构图数据
 * @param {Object} data - AI 生成的原始数据
 * @returns {Object} 校验和修复后的数据
 */
export function validateAndFixDiagramData(data) {
  if (!data) return data
  
  logger.log('=== 开始智能数据校验 ===')
  
  const config = VALIDATION_CONFIG
  const stats = {
    truncatedLabels: 0,
    fixedIds: 0,
    removedEmptyBlocks: 0,
    fixedColors: 0,
    fixedTerms: 0,
    warnings: []
  }
  
  // 已使用的 ID 集合（检测重复）
  const seenIds = new Set()
  const validIds = new Set()
  
  // ===== 处理单个节点 =====
  const processNode = (node, prefix = 'node', maxLabelLen = config.maxLabelLength) => {
    // 如果 node 是字符串，转换为对象
    if (typeof node === 'string') {
      node = { id: node, label: node }
      stats.fixedIds++
    }
    
    // 如果 node 不是对象，跳过
    if (!node || typeof node !== 'object') {
      return { id: generateId(prefix), label: '未知节点' }
    }
    
    // 确保有 ID
    if (!node.id) {
      node.id = generateId(prefix)
      stats.fixedIds++
    }
    // 修复重复 ID
    if (seenIds.has(node.id)) {
      node.id = generateId(prefix)
      stats.fixedIds++
    }
    seenIds.add(node.id)
    validIds.add(node.id)
    
    // 术语纠正
    if (node.label) {
      const original = node.label
      node.label = fixTerminology(node.label)
      if (original !== node.label) stats.fixedTerms++
    }
    
    // 截断过长标签（保留原始值供 tooltip 使用）
    if (node.label && node.label.length > maxLabelLen) {
      logger.log(`📝 截断标签: "${node.label}" → "${truncateText(node.label, maxLabelLen)}"`)
      node.fullLabel = node.label  // 保留完整标签用于 tooltip
      node.label = truncateText(node.label, maxLabelLen)
      node.truncated = true  // 标记已截断
      stats.truncatedLabels++
    }
    
    return node
  }
  
  // ===== 处理 Block =====
  const processBlock = (block, layerIndex, blockIndex) => {
    if (!block.name) {
      block.name = `模块${blockIndex + 1}`
    }
    block.name = fixTerminology(block.name)
    
    if (block.name.length > config.maxBlockNameLength) {
      block.name = truncateText(block.name, config.maxBlockNameLength)
    }
    
    block.color = fixColor(block.color)
    
    if (!block.nodes) block.nodes = []
    
    if (block.nodes.length > config.maxNodesPerBlock) {
      stats.warnings.push(`Block "${block.name}" 节点过多 (${block.nodes.length}>${config.maxNodesPerBlock})`)
    }
    
    // 使用 map 替换数组，处理字符串节点转换为对象的情况
    block.nodes = block.nodes.map((node, nIdx) => processNode(node, `b${layerIndex}_${blockIndex}_n${nIdx}`))
    
    return block
  }
  
  // ===== 0. 确保有 layers =====
  if (!data.layers || !Array.isArray(data.layers)) {
    logger.warn('⚠️ 数据缺少 layers，创建默认层')
    data.layers = [{ id: 'layer-1', name: '默认层', nodes: [] }]
  }
  
  if (data.layers.length > config.maxLayers) {
    stats.warnings.push(`层数过多 (${data.layers.length}>${config.maxLayers})，可能影响显示`)
  }
  
  // ===== 1. 处理每一层 =====
  data.layers.forEach((layer, layerIndex) => {
    if (!layer.id) layer.id = `layer-${layerIndex + 1}`
    if (!layer.name) layer.name = `第${layerIndex + 1}层`
    
    layer.name = fixTerminology(layer.name)
    
    if (layer.name.length > config.maxLayerNameLength) {
      layer.name = truncateText(layer.name, config.maxLayerNameLength)
    }
    
    if (layer.color) {
      layer.color = fixColor(layer.color)
    }
    
    // 处理 blocks/groups
    const layerBlocks = layer.blocks || layer.groups
    if (layerBlocks && Array.isArray(layerBlocks)) {
      if (layerBlocks.length > config.maxBlocksPerLayer) {
        stats.warnings.push(`层 "${layer.name}" blocks 过多 (${layerBlocks.length}>${config.maxBlocksPerLayer})`)
      }
      
      // 移除空 blocks
      const validBlocks = layerBlocks.filter((block, bIdx) => {
        if (!block.nodes || block.nodes.length === 0) {
          stats.removedEmptyBlocks++
          logger.log(`🗑️ 移除空 block: 层${layerIndex} block${bIdx}`)
          return false
        }
        return true
      })
      
      validBlocks.forEach((block, bIdx) => processBlock(block, layerIndex, bIdx))
      
      if (layer.blocks) layer.blocks = validBlocks
      if (layer.groups) layer.groups = validBlocks
      
    } else if (layer.systems && Array.isArray(layer.systems)) {
      // 处理 systems 结构
      layer.systems.forEach((sys, sIdx) => {
        if (!sys.name) sys.name = `系统${sIdx + 1}`
        if (sys.name.length > config.maxBlockNameLength) {
          sys.name = truncateText(sys.name, config.maxBlockNameLength)
        }
        
        (sys.subsystems || []).forEach((ss, ssIdx) => {
          if (!ss.name) ss.name = `子系统${ssIdx + 1}`
          if (ss.name.length > config.maxBlockNameLength) {
            ss.name = truncateText(ss.name, config.maxBlockNameLength)
          }
          
          ss.nodes = (ss.nodes || []).map((node, nIdx) => processNode(node, `s${layerIndex}_${sIdx}_${ssIdx}_n${nIdx}`))
        })
      })
    } else {
      // 简单层
      if (!layer.nodes) layer.nodes = []
      
      // 自动补全空层（根据层名智能生成默认节点）
      if (layer.nodes.length === 0) {
        logger.warn(`⚠️ 层 "${layer.name}" 为空，自动补全默认节点`)
        const defaultNodes = getDefaultNodesForLayer(layer.name, layerIndex)
        layer.nodes = defaultNodes
      }
      
      if (layer.nodes.length > config.maxNodesPerLayer) {
        // 截断过多节点
        layer.nodes = layer.nodes.slice(0, config.maxNodesPerLayer)
        stats.warnings.push(`层 "${layer.name}" 节点过多，已截断至 ${config.maxNodesPerLayer} 个`)
      }
      
      layer.nodes = layer.nodes.map((node, nIdx) => processNode(node, `l${layerIndex}_n${nIdx}`))
    }
  })
  
  // ===== 2. 修复无效的 parent 引用 =====
  data.layers.forEach(layer => {
    (layer.nodes || []).forEach(node => {
      if (node.parent && !validIds.has(node.parent)) {
        logger.warn(`⚠️ 修复无效 parent: ${node.id} → ${node.parent}`)
        node.parent = null
      }
    })
  })
  
  // ===== 3. 处理侧边栏 =====
  const processSidebar = (sidebar, name) => {
    if (!sidebar) return
    if (!sidebar.nodes) sidebar.nodes = []
    
    if (sidebar.title && sidebar.title.length > 8) {
      sidebar.title = truncateText(sidebar.title, 8)
    }
    
    if (sidebar.nodes.length > config.maxSidebarNodes) {
      stats.warnings.push(`${name} 节点过多 (${sidebar.nodes.length}>${config.maxSidebarNodes})`)
    }
    
    sidebar.nodes = sidebar.nodes.map((node, nIdx) => processNode(node, `sb_${nIdx}`, config.maxSidebarLabelLength))
  }
  
  processSidebar(data.leftSidebar, '左侧边栏')
  processSidebar(data.rightSidebar, '右侧边栏')
  processSidebar(data.leftMonitor, '左侧监控栏')
  
  if (data.rightSidebars && Array.isArray(data.rightSidebars)) {
    data.rightSidebars.forEach((sb, i) => processSidebar(sb, `右侧边栏${i + 1}`))
  }
  
  // ===== 4. 处理 topUsers 和 bottomInfra =====
  if (data.topUsers && Array.isArray(data.topUsers)) {
    data.topUsers = data.topUsers.map((node, nIdx) => processNode(node, `user_${nIdx}`, 8))
  }
  processSidebar(data.bottomInfra, '底部基础设施')
  
  // ===== 5. 校验和修复 edges（数据流） =====
  const validEdgeTypes = ['sync', 'async', 'event']
  if (data.edges && Array.isArray(data.edges)) {
    data.edges = data.edges.filter(edge => {
      // 校验 source 和 target 是否存在
      const validSource = validIds.has(edge.source)
      const validTarget = validIds.has(edge.target)
      if (!validSource || !validTarget) {
        logger.warn(`⚠️ 移除无效 edge: ${edge.source} → ${edge.target} (节点不存在)`)
        return false
      }
      return true
    }).map(edge => {
      // 确保 type 有效
      if (!edge.type || !validEdgeTypes.includes(edge.type)) {
        edge.type = 'sync' // 默认同步
      }
      // 确保 label 存在且不过长
      if (edge.label && edge.label.length > 10) {
        edge.label = truncateText(edge.label, 10)
      }
      return edge
    })
    
    // 限制 edges 数量防止 DoS
    const maxEdges = 50
    if (data.edges.length > maxEdges) {
      logger.warn(`⚠️ edges 过多 (${data.edges.length}>${maxEdges})，截断至 ${maxEdges} 条`)
      data.edges = data.edges.slice(0, maxEdges)
    }
    
    logger.log(`📊 数据流: ${data.edges.length} 条有效连接`)
  } else {
    // 如果没有 edges，初始化为空数组
    data.edges = []
    logger.log('📊 数据流: 无 edges 定义')
  }
  
  // ===== 6. 移除空层 =====
  data.layers = data.layers.filter((layer, idx) => {
    const hasContent = (layer.nodes?.length > 0) ||
                       (layer.blocks?.length > 0) ||
                       (layer.groups?.length > 0) ||
                       (layer.systems?.length > 0)
    if (!hasContent) {
      logger.warn(`⚠️ 移除空层: ${layer.name || `层${idx}`}`)
      return false
    }
    return true
  })
  
  // ===== 7. 自动分配 Block 颜色 =====
  autoAssignBlockColors(data.layers)
  
  // ===== 8. 自动推断类型 =====
  if (!data.type) {
    if (data.leftSidebar || data.rightSidebar || data.leftMonitor || data.rightSidebars) {
      data.type = 'layered-sidebar'
    } else {
      data.type = 'layered'
    }
    logger.log(`📌 自动推断类型: ${data.type}`)
  }
  
  // ===== 9. 标题处理 =====
  if (data.title && data.title.length > config.maxTitleLength) {
    data.title = truncateText(data.title, config.maxTitleLength)
  }
  
  // ===== 10. 安全检查：总节点数限制（防止 DoS）=====
  const maxTotalNodes = config.maxTotalNodes || 50
  const totalNodes = validIds.size
  if (totalNodes > maxTotalNodes) {
    logger.warn(`⚠️ 安全警告: 总节点数过多 (${totalNodes}>${maxTotalNodes})，截断处理`)
    stats.warnings.push(`总节点数过多 (${totalNodes}>${maxTotalNodes})`)
    // 对过多的层进行截断
    if (data.layers.length > config.maxLayers) {
      data.layers = data.layers.slice(0, config.maxLayers)
      logger.warn(`⚠️ 层数过多，截断至 ${config.maxLayers} 层`)
    }
  }
  
  // ===== 输出统计 =====
  logger.log('=== 智能校验完成 ===')
  logger.log(`📊 统计: 术语纠正=${stats.fixedTerms}, 截断标签=${stats.truncatedLabels}, 修复ID=${stats.fixedIds}, 移除空块=${stats.removedEmptyBlocks}`)
  logger.log(`📊 安全统计: 总节点数=${totalNodes}, edges数=${data.edges?.length || 0}`)
  logger.log(`📊 最终数据: ${data.layers.length}层, 类型=${data.type}`)
  
  if (stats.warnings.length > 0) {
    logger.warn('⚠️ 警告:')
    stats.warnings.forEach(w => logger.warn(`  - ${w}`))
  }
  
  // 智能评估结果（附加到数据上）
  data._evaluation = evaluateDiagramQuality(data, stats)
  
  return data
}

/**
 * 智能评估架构图质量
 * @param {Object} data - 架构图数据
 * @param {Object} stats - 校验统计
 * @returns {Object} 评估结果
 */
export function evaluateDiagramQuality(data, stats = {}) {
  const evaluation = {
    score: 100,           // 质量分数（满分100）
    level: 'excellent',   // 质量等级
    issues: [],           // 问题列表
    suggestions: [],      // 改进建议
    highlights: []        // 亮点
  }
  
  // 检查层数
  const layerCount = data.layers?.length || 0
  if (layerCount < 2) {
    evaluation.score -= 20
    evaluation.issues.push('层数过少，架构不够清晰')
    evaluation.suggestions.push('建议至少包含 3 个层级（接入层、应用层、数据层）')
  } else if (layerCount >= 4) {
    evaluation.highlights.push(`包含 ${layerCount} 个层级，结构清晰`)
  }
  
  // 检查节点数
  let totalNodes = 0
  let emptyLayers = 0
  data.layers?.forEach(layer => {
    const nodeCount = (layer.nodes?.length || 0) + 
      (layer.blocks?.reduce((sum, b) => sum + (b.nodes?.length || 0), 0) || 0)
    totalNodes += nodeCount
    if (nodeCount === 0) emptyLayers++
  })
  
  if (totalNodes < 5) {
    evaluation.score -= 15
    evaluation.issues.push('节点数过少，内容不够丰富')
  } else if (totalNodes > 30) {
    evaluation.score -= 10
    evaluation.suggestions.push('节点数较多，建议使用 blocks 分组')
  }
  
  if (emptyLayers > 0) {
    evaluation.score -= 10 * emptyLayers
    evaluation.issues.push(`存在 ${emptyLayers} 个空层`)
  }
  
  // 检查是否有数据流
  const edgeCount = data.edges?.length || 0
  if (edgeCount === 0) {
    evaluation.suggestions.push('可以添加 edges 展示数据流向')
  } else if (edgeCount >= 3) {
    evaluation.highlights.push(`包含 ${edgeCount} 条数据流连接`)
  }
  
  // 检查侧边栏
  if (data.leftSidebar || data.rightSidebar) {
    evaluation.highlights.push('包含侧边栏，展示了监控/治理能力')
  }
  
  // 检查 blocks 分组
  const hasBlocks = data.layers?.some(l => l.blocks?.length > 0)
  if (hasBlocks) {
    evaluation.highlights.push('使用了 blocks 分组，结构更清晰')
  }
  
  // 统计修复情况
  if (stats.fixedTerms > 0) {
    evaluation.suggestions.push(`已自动纠正 ${stats.fixedTerms} 处术语`)
  }
  if (stats.truncatedLabels > 0) {
    evaluation.suggestions.push(`已自动截断 ${stats.truncatedLabels} 个过长标签`)
  }
  
  // 计算等级
  if (evaluation.score >= 90) {
    evaluation.level = 'excellent'
  } else if (evaluation.score >= 70) {
    evaluation.level = 'good'
  } else if (evaluation.score >= 50) {
    evaluation.level = 'fair'
  } else {
    evaluation.level = 'poor'
  }
  
  return evaluation
}

// ==================== 布局计算 ====================

/**
 * 将节点按行分组（支持自动换行）
 * @param {Array} nodes - 节点数组
 * @param {number} maxRowWidth - 每行最大宽度
 * @param {number} nodeGapX - 节点水平间距
 * @returns {Array} 分行后的节点组
 */
export function groupNodesIntoRows(nodes, maxRowWidth, nodeGapX = 6) {
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

/**
 * 计算树形结构的后代数量（用于组织架构图）
 * @param {string} nodeId - 节点 ID
 * @param {Array} allNodes - 所有节点
 * @returns {number} 后代数量
 */
export function getDescendantCount(nodeId, allNodes) {
  const children = allNodes.filter(n => n.parent === nodeId)
  if (children.length === 0) return 1
  return children.reduce((sum, c) => sum + getDescendantCount(c.id, allNodes), 0)
}

// ==================== 类型检测 ====================

/**
 * 智能检测图表类型
 * @param {Object} data - 图表数据
 * @param {string} userInput - 用户输入内容
 * @returns {string} 图表类型
 */
export function detectDiagramType(data, userInput = '') {
  // 优先使用 AI 返回的 type
  if (data.type && ['org', 'flow', 'topology', 'layered', 'layered-sidebar', 'enterprise-full'].includes(data.type)) {
    return data.type
  }
  
  // 基于关键词权重判断
  const scores = { org: 0, flow: 0, layered: 0, topology: 0 }
  
  // 组织架构
  if (/组织架构|公司架构|人员架构|团队结构/.test(userInput)) scores.org += 10
  if (/CEO|CTO|CFO|COO|CPO|总裁|总监/.test(userInput)) scores.org += 5
  if (/├─|└─|下属|负责/.test(userInput)) scores.org += 3
  if (/部门|团队|小组/.test(userInput)) scores.org += 2
  
  // 流程图
  if (/流程图|业务流程|工作流/.test(userInput)) scores.flow += 10
  if (/步骤|阶段|环节/.test(userInput)) scores.flow += 3
  if (/第[一二三四五]步|然后|接着/.test(userInput)) scores.flow += 2
  
  // 拓扑图
  if (/拓扑|调用关系|依赖关系|网络/.test(userInput)) scores.topology += 10
  if (/微服务|服务调用/.test(userInput)) scores.topology += 3
  
  // 分层架构
  if (/架构图|分层|层级|技术栈/.test(userInput)) scores.layered += 5
  if (/系统架构|平台架构/.test(userInput)) scores.layered += 3
  
  const maxType = Object.entries(scores).reduce((a, b) => a[1] > b[1] ? a : b)
  return maxType[1] > 0 ? maxType[0] : 'layered'
}
