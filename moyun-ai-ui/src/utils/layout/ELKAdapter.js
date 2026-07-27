/**
 * ELK 布局适配器
 * 负责调用 ELK 库计算节点和边的布局
 */

import ELK from 'elkjs/lib/elk.bundled.js'
import { DiagramType, DefaultNodeSizes } from './types.js'
import { LayoutStrategySelector } from './LayoutStrategySelector.js'

// ========== 布局预设 ==========
const LayoutPresets = {
  // 系统架构图：分层布局，从上到下
  [DiagramType.ARCHITECTURE]: {
    'elk.algorithm': 'layered',
    'elk.direction': 'DOWN',
    // 间距配置 - 更紧凑
    'elk.layered.spacing.nodeNodeBetweenLayers': '60',
    'elk.layered.spacing.edgeNodeBetweenLayers': '30',
    'elk.spacing.nodeNode': '40',
    'elk.spacing.edgeNode': '20',
    'elk.spacing.componentComponent': '60',
    // 分离连通分量
    'elk.separateConnectedComponents': 'true',
    // 正交路由
    'elk.edgeRouting': 'ORTHOGONAL',
    // 交叉最小化
    'elk.layered.crossingMinimization.strategy': 'LAYER_SWEEP',
    // 节点放置策略
    'elk.layered.nodePlacement.strategy': 'NETWORK_SIMPLEX',
    'elk.layered.nodePlacement.favorStraightEdges': 'true',
    // 端口约束
    'elk.portConstraints': 'FIXED_SIDE',
    // 考虑模型顺序
    'elk.layered.considerModelOrder.strategy': 'NODES_AND_EDGES',
    // 紧凑模式
    'elk.layered.compaction.connectedComponents': 'true',
    'elk.layered.compaction.postCompaction.strategy': 'EDGE_LENGTH'
  },

  // 流程图：紧凑分层
  [DiagramType.FLOWCHART]: {
    'elk.algorithm': 'layered',
    'elk.direction': 'DOWN',
    'elk.layered.spacing.nodeNodeBetweenLayers': '80',
    'elk.spacing.nodeNode': '40',
    'elk.edgeRouting': 'ORTHOGONAL',
    'elk.layered.nodePlacement.strategy': 'BRANDES_KOEPF',
    'elk.portConstraints': 'FIXED_SIDE'
  },

  // 时序图：从左到右
  [DiagramType.SEQUENCE]: {
    'elk.algorithm': 'layered',
    'elk.direction': 'RIGHT',
    'elk.layered.spacing.nodeNodeBetweenLayers': '150',
    'elk.spacing.nodeNode': '30',
    'elk.edgeRouting': 'POLYLINE',
    'elk.portConstraints': 'FIXED_SIDE'
  },

  // 泳道图：分区分层
  [DiagramType.SWIMLANE]: {
    'elk.algorithm': 'layered',
    'elk.direction': 'RIGHT',
    'elk.partitioning.activate': 'true',
    'elk.layered.spacing.nodeNodeBetweenLayers': '80',
    'elk.spacing.nodeNode': '50',
    'elk.edgeRouting': 'ORTHOGONAL'
  },

  // 思维导图：树形布局
  [DiagramType.MINDMAP]: {
    'elk.algorithm': 'mrtree',
    'elk.direction': 'RIGHT',
    'elk.spacing.nodeNode': '30'
  },

  // AWS 架构图
  [DiagramType.AWS]: {
    'elk.algorithm': 'layered',
    'elk.direction': 'DOWN',
    'elk.layered.spacing.nodeNodeBetweenLayers': '120',
    'elk.spacing.nodeNode': '80',
    'elk.edgeRouting': 'ORTHOGONAL',
    'elk.layered.crossingMinimization.strategy': 'LAYER_SWEEP',
    'elk.portConstraints': 'FIXED_SIDE'
  }
}

/**
 * ELK 布局适配器类
 */
export class ELKAdapter {
  constructor() {
    this.elk = new ELK()
  }

  /**
   * 执行布局计算 (智能增强版)
   * @param {Object} graph - 语义化图数据
   * @param {Object} options - 布局选项
   * @returns {Promise<Object>} - 布局后的图数据
   */
  async layout(graph, options = {}) {
    const { useSmartStrategy = true } = options
    
    let layoutParams
    
    if (useSmartStrategy) {
      // 使用智能策略选择器
      const strategyResult = LayoutStrategySelector.getLayoutParams(graph, {
        diagramType: graph.type,
        optimize: true
      })
      layoutParams = strategyResult.params
    } else {
      // 使用预设
      layoutParams = LayoutPresets[graph.type] || LayoutPresets[DiagramType.ARCHITECTURE]
    }
    
    const elkGraph = this.toELKGraph(graph, layoutParams)

    console.log('[ELK] 开始布局，节点数:', graph.nodes?.length || 0, '边数:', graph.edges?.length || 0)
    const startTime = performance.now()

    try {
      const result = await this.elk.layout(elkGraph)
      console.log('[ELK] 布局完成，耗时:', (performance.now() - startTime).toFixed(2), 'ms')
      return this.toLayoutedGraph(result, graph)
    } catch (error) {
      console.error('[ELK] 布局失败:', error)
      throw error
    }
  }

  /**
   * 带锁定节点的增量布局 (智能增强版)
   * @param {Object} graph - 语义化图数据
   * @param {Map} lockedPositions - 锁定的节点位置 Map<nodeId, {x, y}>
   */
  async layoutWithLocks(graph, lockedPositions) {
    // 使用智能策略选择器
    const strategyResult = LayoutStrategySelector.getLayoutParams(graph, {
      diagramType: graph.type,
      optimize: true
    })
    const elkGraph = this.toELKGraph(graph, strategyResult.params)

    // 为锁定节点添加位置约束
    for (const child of elkGraph.children) {
      if (lockedPositions.has(child.id)) {
        const pos = lockedPositions.get(child.id)
        child.x = pos.x
        child.y = pos.y
        child.layoutOptions = {
          ...child.layoutOptions,
          'elk.position': `(${pos.x}, ${pos.y})`
        }
      }
    }

    const result = await this.elk.layout(elkGraph)
    return this.toLayoutedGraph(result, graph)
  }

  /**
   * 转换为 ELK 格式
   */
  toELKGraph(graph, layoutOptions) {
    const children = []
    const groupMap = new Map() // 用于处理分组

    // 处理分组
    if (graph.groups && graph.groups.length > 0) {
      for (const group of graph.groups) {
        groupMap.set(group.id, group)
      }
    }

    // 创建节点
    for (const node of graph.nodes) {
      const size = this.getNodeSize(node)
      const elkNode = {
        id: node.id,
        labels: [{ text: node.label || node.id }],
        width: size.width,
        height: size.height,
        layoutOptions: {},
        // 保存原始 layer 信息，用于后续排序
        _layer: node.layer
      }

      children.push(elkNode)
    }

    // 按 layer 排序子节点，帮助 ELK 更好地分层
    children.sort((a, b) => (a._layer ?? 99) - (b._layer ?? 99))

    // 处理分组：将子节点移入分组，分组也参与布局
    if (graph.groups && graph.groups.length > 0) {
      for (let i = 0; i < graph.groups.length; i++) {
        const group = graph.groups[i]
        const groupNode = {
          id: group.id,
          labels: [{ text: group.label || group.id }],
          layoutOptions: {
            // 分组内部使用水平排列（分层架构的每层节点水平排列）
            'elk.algorithm': 'layered',
            'elk.direction': 'RIGHT',  // 水平方向，节点从左到右排列
            'elk.padding': '[top=50,left=20,bottom=20,right=20]',
            'elk.spacing.nodeNode': '20',
            'elk.layered.spacing.nodeNodeBetweenLayers': '30',
            // 紧凑布局
            'elk.layered.compaction.postCompaction.strategy': 'EDGE_LENGTH',
            // 保持节点顺序
            'elk.layered.considerModelOrder.strategy': 'NODES_AND_EDGES'
          },
          children: []
        }

        // 移动子节点到分组内
        for (const containedId of group.contains || []) {
          const idx = children.findIndex(c => c.id === containedId)
          if (idx !== -1) {
            const childNode = children[idx]
            // 清除子节点的 _layer，让分组内部自由布局
            delete childNode._layer
            groupNode.children.push(childNode)
            children.splice(idx, 1)
          }
        }

        // 如果分组有子节点，计算估计尺寸（水平排列）
        if (groupNode.children.length > 0) {
          const childCount = groupNode.children.length
          // 水平排列：宽度 = 节点数 × 单节点宽度 + 间距
          const avgNodeWidth = 120
          const spacing = 20
          groupNode.width = childCount * avgNodeWidth + (childCount - 1) * spacing + 40
          groupNode.height = 80  // 单行高度
        }

        children.push(groupNode)
      }
    }

    // 创建边
    const edges = (graph.edges || []).map((edge, index) => ({
      id: `edge_${index}`,
      sources: [edge.from],
      targets: [edge.to],
      labels: edge.label ? [{ text: edge.label }] : undefined
    }))

    return {
      id: 'root',
      layoutOptions,
      children,
      edges
    }
  }

  /**
   * 从 ELK 结果转换回我们的格式
   */
  toLayoutedGraph(elkResult, original) {
    const nodeMap = new Map()

    // 递归提取节点坐标
    const extractNodes = (elkNode, offsetX = 0, offsetY = 0) => {
      if (elkNode.children) {
        for (const child of elkNode.children) {
          const x = (child.x || 0) + offsetX
          const y = (child.y || 0) + offsetY

          // 查找原始节点信息
          const originalNode = original.nodes.find(n => n.id === child.id)
          if (originalNode) {
            nodeMap.set(child.id, {
              ...originalNode,
              x,
              y,
              width: child.width || 120,
              height: child.height || 50
            })
          }

          // 递归处理分组内的子节点
          if (child.children) {
            extractNodes(child, x, y)
          }
        }
      }
    }
    extractNodes(elkResult)

    // 递归收集所有边（包括分组内的边）
    const allElkEdges = []
    const collectEdges = (node, offsetX = 0, offsetY = 0) => {
      if (node.edges) {
        for (const edge of node.edges) {
          allElkEdges.push({ ...edge, offsetX, offsetY })
        }
      }
      if (node.children) {
        for (const child of node.children) {
          const childOffsetX = offsetX + (child.x || 0)
          const childOffsetY = offsetY + (child.y || 0)
          collectEdges(child, childOffsetX, childOffsetY)
        }
      }
    }
    collectEdges(elkResult)
    
    console.log(`[ELK] 收集到 ${allElkEdges.length} 条边路径`)

    // 提取边的路径
    const layoutedEdges = (original.edges || []).map((edge, index) => {
      const elkEdge = allElkEdges.find(e => e.id === `edge_${index}`)
      const points = []

      if (elkEdge?.sections) {
        const { offsetX = 0, offsetY = 0 } = elkEdge
        for (const section of elkEdge.sections) {
          // 应用偏移量（分组内的边需要加上分组的位置）
          if (section.startPoint) {
            points.push({ 
              x: section.startPoint.x + offsetX, 
              y: section.startPoint.y + offsetY 
            })
          }
          if (section.bendPoints && section.bendPoints.length > 0) {
            for (const bp of section.bendPoints) {
              points.push({ x: bp.x + offsetX, y: bp.y + offsetY })
            }
          }
          if (section.endPoint) {
            points.push({ 
              x: section.endPoint.x + offsetX, 
              y: section.endPoint.y + offsetY 
            })
          }
        }
      }
      
      // 调试日志
      if (points.length > 0) {
        console.log(`[ELK] 边 ${edge.from} → ${edge.to}: ${points.length} 个路径点`)
      } else {
        console.log(`[ELK] 边 ${edge.from} → ${edge.to}: 无路径点 (将由 SmartEdgeRouter 处理)`)
      }

      // 计算出入口位置（基于节点相对位置，更准确）
      const sourceNode = nodeMap.get(edge.from)
      const targetNode = nodeMap.get(edge.to)

      // 默认值：从底部出，从顶部入（垂直布局）
      let exitX = 0.5, exitY = 1, entryX = 0.5, entryY = 0

      if (sourceNode && targetNode) {
        // 计算源和目标的中心点
        const srcCenterX = sourceNode.x + sourceNode.width / 2
        const srcCenterY = sourceNode.y + sourceNode.height / 2
        const tgtCenterX = targetNode.x + targetNode.width / 2
        const tgtCenterY = targetNode.y + targetNode.height / 2
        
        // 计算方向向量
        const dx = tgtCenterX - srcCenterX
        const dy = tgtCenterY - srcCenterY
        
        // 根据主要方向确定连接点
        if (Math.abs(dy) > Math.abs(dx)) {
          // 主要是垂直方向
          if (dy > 0) {
            // 目标在下方：从底部出，从顶部入
            exitX = 0.5; exitY = 1
            entryX = 0.5; entryY = 0
          } else {
            // 目标在上方：从顶部出，从底部入
            exitX = 0.5; exitY = 0
            entryX = 0.5; entryY = 1
          }
        } else {
          // 主要是水平方向
          if (dx > 0) {
            // 目标在右边：从右边出，从左边入
            exitX = 1; exitY = 0.5
            entryX = 0; entryY = 0.5
          } else {
            // 目标在左边：从左边出，从右边入
            exitX = 0; exitY = 0.5
            entryX = 1; entryY = 0.5
          }
        }
      }

      return {
        ...edge,
        points,
        exitX,
        exitY,
        entryX,
        entryY
      }
    })

    // 提取分组位置
    const layoutedGroups = (original.groups || []).map(group => {
      // 在 ELK 结果中查找分组节点
      const elkGroup = elkResult.children?.find(c => c.id === group.id)
      if (elkGroup) {
        return {
          ...group,
          x: elkGroup.x || 0,
          y: elkGroup.y || 0,
          width: elkGroup.width || 200,
          height: elkGroup.height || 100
        }
      }
      return group
    })

    return {
      ...original,
      nodes: Array.from(nodeMap.values()),
      edges: layoutedEdges,
      groups: layoutedGroups
    }
  }

  /**
   * 获取节点尺寸
   * 优先使用节点上已有的尺寸（由 SmartNodeSizer 计算）
   */
  getNodeSize(node) {
    // 优先使用已计算的尺寸
    if (node.width && node.height) {
      return { width: node.width, height: node.height }
    }

    // AWS 图标
    if (node.icon?.startsWith('aws_')) {
      return DefaultNodeSizes.aws_icon
    }

    // 特定图标
    if (node.icon && DefaultNodeSizes[node.icon]) {
      return DefaultNodeSizes[node.icon]
    }

    // 默认尺寸
    return DefaultNodeSizes.default
  }
}

// 导出单例
export const elkAdapter = new ELKAdapter()
