/**
 * 性能管理器
 * 根据图表规模选择合适的处理策略
 */

import { PerformanceThresholds } from './types.js'

/**
 * 处理策略类型
 */
export const ProcessingStrategy = {
  DIRECT: 'direct',           // 主线程直接计算
  WORKER: 'worker',           // Web Worker 后台计算
  SIMPLIFIED: 'simplified',   // 简化布局算法
  REJECT: 'reject'            // 拒绝处理
}

/**
 * 性能管理器类
 */
export class PerformanceManager {
  /**
   * 根据图规模选择处理策略
   * @param {number} nodeCount - 节点数量
   * @param {number} edgeCount - 边数量
   * @returns {Object} - 策略信息
   */
  static selectStrategy(nodeCount, edgeCount = 0) {
    if (nodeCount <= PerformanceThresholds.DIRECT) {
      return {
        type: ProcessingStrategy.DIRECT,
        description: '主线程计算',
        estimatedTime: nodeCount * 10, // 约 10ms/节点
        canProceed: true
      }
    }

    if (nodeCount <= PerformanceThresholds.WORKER) {
      return {
        type: ProcessingStrategy.WORKER,
        description: 'Web Worker 后台计算',
        estimatedTime: nodeCount * 15,
        canProceed: true,
        warning: '图表较大，正在后台计算布局...'
      }
    }

    if (nodeCount <= PerformanceThresholds.SIMPLIFIED) {
      return {
        type: ProcessingStrategy.SIMPLIFIED,
        description: '简化布局算法',
        estimatedTime: nodeCount * 5,
        canProceed: true,
        warning: '图表较大，将使用简化布局算法'
      }
    }

    return {
      type: ProcessingStrategy.REJECT,
      description: '图表过大',
      canProceed: false,
      error: `节点数 ${nodeCount} 超过限制 (${PerformanceThresholds.REJECT})，请简化图表或分拆为多个图表`
    }
  }

  /**
   * 判断是否需要重新布局
   * @param {Object} oldGraph - 旧图数据
   * @param {Object} newGraph - 新图数据
   * @param {boolean} hasUserEdits - 是否有用户编辑
   * @returns {string|boolean} - true=全量布局, 'incremental'=增量布局, false=不需要
   */
  static shouldRelayout(oldGraph, newGraph, hasUserEdits = false) {
    // 新图表：必须布局
    if (!oldGraph || !oldGraph.nodes || oldGraph.nodes.length === 0) {
      return true
    }

    const oldNodeCount = oldGraph.nodes.length
    const newNodeCount = newGraph.nodes?.length || 0

    // 节点数量变化大于 30%：重新布局
    const nodeChange = Math.abs(newNodeCount - oldNodeCount) / oldNodeCount
    if (nodeChange > 0.3) {
      return true
    }

    // 检测是否有新增节点
    const oldNodeIds = new Set(oldGraph.nodes.map(n => n.id))
    const newNodeIds = new Set((newGraph.nodes || []).map(n => n.id))
    const addedNodes = [...newNodeIds].filter(id => !oldNodeIds.has(id))

    // 有新增节点且用户有编辑：增量布局
    if (addedNodes.length > 0 && hasUserEdits) {
      return 'incremental'
    }

    // 有新增节点但用户无编辑：全量布局更好
    if (addedNodes.length > 0) {
      return true
    }

    // 只是样式变化：不需要重新布局
    return false
  }
}

/**
 * 分层展示布局
 * 专门用于分层架构图，每层水平排列，类似系统架构图
 */
export class LayeredDisplayLayout {
  /**
   * 分层展示布局
   * @param {Object} graph - 图数据（需要有 groups）
   * @returns {Object} - 布局后的图数据
   */
  static layout(graph) {
    const { nodes = [], edges = [], groups = [] } = graph
    
    if (groups.length === 0) {
      return SimplifiedLayout.layout(graph)
    }

    // 配置 - 优化参数
    const config = {
      canvasWidth: 900,       // 画布宽度（适中，避免超出可视区）
      labelWidth: 85,         // 左侧标签宽度
      layerHeight: 75,        // 每层最小高度
      layerPadding: 12,       // 层内边距
      nodeSpacing: 10,        // 节点间距
      layerSpacing: 4,        // 层间距
      startX: 20,             // 起始X（留出边距）
      startY: 20,             // 起始Y（留出边距）
      nodeHeight: 45,         // 节点高度
      minNodeWidth: 75,       // 最小节点宽度
      maxNodeWidth: 120       // 最大节点宽度（减小避免拥挤）
    }
    
    // 根据文字计算节点宽度
    const calcNodeWidth = (label) => {
      if (!label) return config.minNodeWidth
      // 中文字符约 12px，英文约 7px
      let width = 0
      for (const char of label) {
        width += /[\u4e00-\u9fa5]/.test(char) ? 12 : 7
      }
      // 加上内边距
      width += 20
      return Math.max(config.minNodeWidth, Math.min(width, config.maxNodeWidth))
    }

    const nodeMap = new Map(nodes.map(n => [n.id, { ...n }]))
    const layoutedGroups = []
    let currentY = config.startY

    // 按层级顺序处理每个分组
    const sortedGroups = [...groups].sort((a, b) => (a.layer ?? 99) - (b.layer ?? 99))

    // 层颜色方案（柔和专业的配色）
    const layerColors = [
      { fill: '#E8F4FD', stroke: '#4A90D9' },  // 浅蓝
      { fill: '#FFF4E5', stroke: '#E89B3C' },  // 浅橙
      { fill: '#E8F5E9', stroke: '#5CB85C' },  // 浅绿
      { fill: '#FDE8E8', stroke: '#D9534F' },  // 浅红
      { fill: '#F3E8FD', stroke: '#9B59B6' },  // 浅紫
      { fill: '#E8FDFD', stroke: '#17A2B8' },  // 浅青
      { fill: '#FDFDE8', stroke: '#C9A227' },  // 浅黄
    ]

    for (let gi = 0; gi < sortedGroups.length; gi++) {
      const group = sortedGroups[gi]
      const containedNodes = (group.contains || [])
        .map(id => nodeMap.get(id))
        .filter(Boolean)

      // 即使没有节点，也显示空层
      const nodeCount = containedNodes.length
      
      // 计算层的布局（加入起始偏移）
      const contentWidth = config.canvasWidth - config.labelWidth
      const groupX = config.startX + config.labelWidth
      const groupY = currentY
      
      // 计算每个节点的自适应宽度
      const nodeWidths = containedNodes.map(n => calcNodeWidth(n.label || n.id))
      
      // 根据节点数量动态调整层高度
      const avgNodeWidth = nodeWidths.length > 0 ? nodeWidths.reduce((a, b) => a + b, 0) / nodeWidths.length : config.minNodeWidth
      const nodesPerRow = Math.floor((contentWidth - config.layerPadding * 2) / (avgNodeWidth + config.nodeSpacing))
      const rows = Math.max(1, Math.ceil(nodeCount / Math.max(1, nodesPerRow)))
      const groupHeight = Math.max(config.layerHeight, rows * (config.nodeHeight + 10) + config.layerPadding * 2)
      const totalWidth = nodeWidths.reduce((sum, w) => sum + w, 0) + 
                        (containedNodes.length - 1) * config.nodeSpacing
      
      // 节点水平居中
      let nodeX = groupX + Math.max(config.layerPadding, (contentWidth - totalWidth) / 2)
      // 节点垂直居中
      const nodeY = groupY + (groupHeight - config.nodeHeight) / 2

      for (let i = 0; i < containedNodes.length; i++) {
        const node = containedNodes[i]
        const width = nodeWidths[i]
        
        node.x = nodeX
        node.y = nodeY
        node.width = width
        node.height = config.nodeHeight
        
        nodeX += width + config.nodeSpacing
      }

      // 保存分组信息（使用层颜色）
      const colorIndex = gi % layerColors.length
      layoutedGroups.push({
        ...group,
        x: groupX,
        y: groupY,
        width: contentWidth,
        height: groupHeight,
        // 标记为层级展示样式
        displayStyle: 'layer',
        layerColor: layerColors[colorIndex],
        // 左侧标签（加入 startX 偏移）
        labelX: config.startX,
        labelY: groupY,
        labelWidth: config.labelWidth,
        labelHeight: groupHeight
      })

      currentY += groupHeight + config.layerSpacing
    }

    // 处理不在任何分组中的节点（放在最后一行）
    const groupedNodeIds = new Set(groups.flatMap(g => g.contains || []))
    const ungroupedNodes = nodes.filter(n => !groupedNodeIds.has(n.id))
    
    if (ungroupedNodes.length > 0) {
      // 为孤立节点创建一个"其他"分组
      const otherGroupHeight = config.layerHeight
      const contentWidth = config.canvasWidth - config.labelWidth
      const groupX = config.startX + config.labelWidth
      
      // 计算孤立节点布局
      const nodeWidths = ungroupedNodes.map(n => Math.min(n.width || config.nodeWidth, 110))
      const totalWidth = nodeWidths.reduce((sum, w) => sum + w, 0) + 
                        (ungroupedNodes.length - 1) * config.nodeSpacing
      
      let nodeX = groupX + Math.max(config.layerPadding, (contentWidth - totalWidth) / 2)
      const nodeY = currentY + (otherGroupHeight - config.nodeHeight) / 2
      
      for (let i = 0; i < ungroupedNodes.length; i++) {
        const node = nodeMap.get(ungroupedNodes[i].id)
        if (node) {
          node.x = nodeX
          node.y = nodeY
          node.width = nodeWidths[i]
          node.height = config.nodeHeight
          nodeX += nodeWidths[i] + config.nodeSpacing
        }
      }
      
      // 添加其他分组
      const colorIndex = layoutedGroups.length % layerColors.length
      layoutedGroups.push({
        id: '_other_',
        label: '其他',
        contains: ungroupedNodes.map(n => n.id),
        x: groupX,
        y: currentY,
        width: contentWidth,
        height: otherGroupHeight,
        displayStyle: 'layer',
        layerColor: layerColors[colorIndex],
        labelX: config.startX,
        labelY: currentY,
        labelWidth: config.labelWidth,
        labelHeight: otherGroupHeight
      })
    }

    // 不生成边（纯展示图）
    return {
      ...graph,
      nodes: Array.from(nodeMap.values()),
      edges: [],  // 清空边
      groups: layoutedGroups,
      // 标记为分层展示模式
      displayMode: 'layered'
    }
  }
}

/**
 * 简化布局算法
 * 用于大图表的快速布局
 */
export class SimplifiedLayout {
  /**
   * 快速分层布局
   * @param {Object} graph - 图数据
   * @returns {Object} - 布局后的图数据
   */
  static layout(graph) {
    // 1. 拓扑排序确定层级
    const layers = this.topologicalSort(graph)

    // 2. 计算每层节点位置
    const nodePositions = new Map()
    const nodeMap = new Map((graph.nodes || []).map(n => [n.id, n]))
    const layerHeight = 100
    const defaultWidth = 140
    const defaultHeight = 50
    const nodeSpacing = 40
    const canvasWidth = 1200

    layers.forEach((layerNodes, layerIndex) => {
      // 计算该层的总宽度
      let layerWidth = 0
      layerNodes.forEach(nodeId => {
        const node = nodeMap.get(nodeId)
        layerWidth += (node?.width || defaultWidth) + nodeSpacing
      })
      layerWidth -= nodeSpacing

      let currentX = (canvasWidth - layerWidth) / 2 // 居中

      layerNodes.forEach((nodeId) => {
        const node = nodeMap.get(nodeId)
        const width = node?.width || defaultWidth
        nodePositions.set(nodeId, {
          x: Math.round(currentX),
          y: Math.round(50 + layerIndex * layerHeight)
        })
        currentX += width + nodeSpacing
      })
    })

    // 3. 构建结果（保留节点原有尺寸）
    const layoutedNodes = (graph.nodes || []).map(node => {
      const pos = nodePositions.get(node.id) || { x: 0, y: 0 }
      return {
        ...node,
        x: pos.x,
        y: pos.y,
        width: node.width || defaultWidth,
        height: node.height || defaultHeight
      }
    })

    // 4. 简单边路径（直线，无拐点）
    const layoutedEdges = (graph.edges || []).map(edge => ({
      ...edge,
      points: [],
      exitX: 0.5,
      exitY: 1,
      entryX: 0.5,
      entryY: 0
    }))

    return {
      ...graph,
      nodes: layoutedNodes,
      edges: layoutedEdges
    }
  }

  /**
   * 拓扑排序（BFS 分层）
   */
  static topologicalSort(graph) {
    const nodes = graph.nodes || []
    const edges = graph.edges || []

    // 构建邻接表
    const inDegree = new Map()
    const outEdges = new Map()

    nodes.forEach(n => {
      inDegree.set(n.id, 0)
      outEdges.set(n.id, [])
    })

    edges.forEach(e => {
      if (inDegree.has(e.to)) {
        inDegree.set(e.to, inDegree.get(e.to) + 1)
      }
      if (outEdges.has(e.from)) {
        outEdges.get(e.from).push(e.to)
      }
    })

    // BFS 分层
    const layers = []
    let currentLayer = nodes
      .filter(n => inDegree.get(n.id) === 0)
      .map(n => n.id)

    const assigned = new Set()

    while (currentLayer.length > 0) {
      layers.push([...currentLayer])
      currentLayer.forEach(id => assigned.add(id))

      const nextLayer = []

      for (const nodeId of currentLayer) {
        for (const targetId of outEdges.get(nodeId) || []) {
          if (!assigned.has(targetId)) {
            const newDegree = inDegree.get(targetId) - 1
            inDegree.set(targetId, newDegree)
            if (newDegree <= 0 && !nextLayer.includes(targetId)) {
              nextLayer.push(targetId)
            }
          }
        }
      }

      currentLayer = nextLayer
    }

    // 处理未分配的节点（可能有环或孤立节点）
    const unassigned = nodes.filter(n => !assigned.has(n.id)).map(n => n.id)
    if (unassigned.length > 0) {
      layers.push(unassigned)
    }

    return layers
  }
}
