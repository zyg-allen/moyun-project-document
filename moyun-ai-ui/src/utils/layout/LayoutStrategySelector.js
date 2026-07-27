/**
 * 智能布局策略选择器
 * 根据图的特征自动选择最佳布局参数
 */

import { DiagramType } from './types.js'
import { SmartConnector } from './SmartConnector.js'

/**
 * 图特征分析结果
 */
class GraphCharacteristics {
  constructor() {
    this.nodeCount = 0
    this.edgeCount = 0
    this.groupCount = 0
    this.maxLayer = 0
    this.layerDistribution = {}
    this.averageDegree = 0
    this.density = 0
    this.isTree = false
    this.isLinear = false
    this.hasCycles = false
    this.dominantNodeType = null
  }
}

/**
 * 布局策略类型
 */
const LayoutStrategy = {
  LAYERED_VERTICAL: 'layered_vertical',    // 分层-垂直
  LAYERED_HORIZONTAL: 'layered_horizontal', // 分层-水平
  TREE: 'tree',                            // 树形
  FORCE: 'force',                          // 力导向
  COMPACT: 'compact',                      // 紧凑
  SPARSE: 'sparse'                         // 稀疏
}

/**
 * 基础边路由配置（所有策略共享）
 */
const BaseEdgeRoutingConfig = {
  'elk.edgeRouting': 'ORTHOGONAL',
  'elk.layered.crossingMinimization.strategy': 'LAYER_SWEEP',
  'elk.portConstraints': 'FIXED_SIDE',
  'elk.spacing.edgeNode': '25',
  'elk.layered.spacing.edgeNodeBetweenLayers': '30',
  'elk.layered.considerModelOrder.strategy': 'NODES_AND_EDGES',
  // 紧凑模式
  'elk.layered.compaction.connectedComponents': 'true',
  'elk.layered.compaction.postCompaction.strategy': 'EDGE_LENGTH',
  // 直线边优化
  'elk.layered.nodePlacement.favorStraightEdges': 'true',
  // 分离连通分量
  'elk.separateConnectedComponents': 'true'
}

/**
 * 布局参数预设
 */
const StrategyParams = {
  [LayoutStrategy.LAYERED_VERTICAL]: {
    ...BaseEdgeRoutingConfig,
    'elk.algorithm': 'layered',
    'elk.direction': 'DOWN',
    'elk.layered.spacing.nodeNodeBetweenLayers': '50',
    'elk.spacing.nodeNode': '40',
    'elk.layered.nodePlacement.strategy': 'BRANDES_KOEPF'
  },
  
  [LayoutStrategy.LAYERED_HORIZONTAL]: {
    ...BaseEdgeRoutingConfig,
    'elk.algorithm': 'layered',
    'elk.direction': 'RIGHT',
    'elk.layered.spacing.nodeNodeBetweenLayers': '80',
    'elk.spacing.nodeNode': '50',
    'elk.layered.nodePlacement.strategy': 'BRANDES_KOEPF'
  },
  
  [LayoutStrategy.TREE]: {
    'elk.algorithm': 'mrtree',
    'elk.direction': 'RIGHT',
    'elk.spacing.nodeNode': '40'
  },
  
  [LayoutStrategy.FORCE]: {
    'elk.algorithm': 'force',
    'elk.force.iterations': '300',
    'elk.spacing.nodeNode': '80'
  },
  
  [LayoutStrategy.COMPACT]: {
    ...BaseEdgeRoutingConfig,
    'elk.algorithm': 'layered',
    'elk.direction': 'DOWN',
    'elk.layered.spacing.nodeNodeBetweenLayers': '60',
    'elk.spacing.nodeNode': '40'
  },
  
  [LayoutStrategy.SPARSE]: {
    ...BaseEdgeRoutingConfig,
    'elk.algorithm': 'layered',
    'elk.direction': 'DOWN',
    'elk.layered.spacing.nodeNodeBetweenLayers': '100',
    'elk.spacing.nodeNode': '80'
  }
}

/**
 * 智能布局策略选择器
 */
export class LayoutStrategySelector {
  /**
   * 分析图特征
   */
  static analyzeGraph(graph) {
    const chars = new GraphCharacteristics()
    
    const nodes = graph.nodes || []
    const edges = graph.edges || []
    const groups = graph.groups || []
    
    chars.nodeCount = nodes.length
    chars.edgeCount = edges.length
    chars.groupCount = groups.length
    
    if (nodes.length === 0) return chars
    
    // 1. 分析层级分布
    const layers = {}
    for (const node of nodes) {
      const layer = node.layer ?? SmartConnector.getNodeLayer(node)
      layers[layer] = (layers[layer] || 0) + 1
      chars.maxLayer = Math.max(chars.maxLayer, layer)
    }
    chars.layerDistribution = layers
    
    // 2. 计算平均度数和密度
    if (nodes.length > 0) {
      chars.averageDegree = (edges.length * 2) / nodes.length
      const maxEdges = (nodes.length * (nodes.length - 1)) / 2
      chars.density = maxEdges > 0 ? edges.length / maxEdges : 0
    }
    
    // 3. 检测是否为树形结构
    chars.isTree = edges.length === nodes.length - 1 && this.isConnected(nodes, edges)
    
    // 4. 检测是否为线性结构
    chars.isLinear = chars.averageDegree <= 2 && !this.hasBranches(nodes, edges)
    
    // 5. 分析主导节点类型
    const typeCount = {}
    for (const node of nodes) {
      const type = node.icon || SmartConnector.inferNodeType(node)
      typeCount[type] = (typeCount[type] || 0) + 1
    }
    const sortedTypes = Object.entries(typeCount).sort((a, b) => b[1] - a[1])
    if (sortedTypes.length > 0) {
      chars.dominantNodeType = sortedTypes[0][0]
    }
    
    return chars
  }
  
  /**
   * 检查图是否连通
   */
  static isConnected(nodes, edges) {
    if (nodes.length <= 1) return true
    
    const nodeIds = new Set(nodes.map(n => n.id))
    const adj = new Map()
    
    for (const id of nodeIds) {
      adj.set(id, new Set())
    }
    
    for (const edge of edges) {
      if (nodeIds.has(edge.from) && nodeIds.has(edge.to)) {
        adj.get(edge.from).add(edge.to)
        adj.get(edge.to).add(edge.from)
      }
    }
    
    // BFS
    const visited = new Set()
    const queue = [nodes[0].id]
    visited.add(nodes[0].id)
    
    while (queue.length > 0) {
      const current = queue.shift()
      for (const neighbor of adj.get(current) || []) {
        if (!visited.has(neighbor)) {
          visited.add(neighbor)
          queue.push(neighbor)
        }
      }
    }
    
    return visited.size === nodes.length
  }
  
  /**
   * 检查是否有分支
   */
  static hasBranches(nodes, edges) {
    const outDegree = new Map()
    
    for (const edge of edges) {
      outDegree.set(edge.from, (outDegree.get(edge.from) || 0) + 1)
    }
    
    for (const [, degree] of outDegree) {
      if (degree > 1) return true
    }
    
    return false
  }
  
  /**
   * 根据图特征选择最佳策略
   */
  static selectStrategy(graph, diagramType = null) {
    const chars = this.analyzeGraph(graph)
    
    // 1. 如果有明确指定的图表类型，优先使用
    if (diagramType) {
      return this.getStrategyForDiagramType(diagramType, chars)
    }
    
    // 2. 根据图特征智能选择
    
    // 树形结构 → 树形布局
    if (chars.isTree) {
      return {
        strategy: LayoutStrategy.TREE,
        params: StrategyParams[LayoutStrategy.TREE],
        reason: '检测到树形结构'
      }
    }
    
    // 线性结构 → 水平分层
    if (chars.isLinear) {
      return {
        strategy: LayoutStrategy.LAYERED_HORIZONTAL,
        params: StrategyParams[LayoutStrategy.LAYERED_HORIZONTAL],
        reason: '检测到线性流程'
      }
    }
    
    // 节点很多 → 紧凑布局
    if (chars.nodeCount > 20) {
      return {
        strategy: LayoutStrategy.COMPACT,
        params: StrategyParams[LayoutStrategy.COMPACT],
        reason: '节点数量较多，使用紧凑布局'
      }
    }
    
    // 节点较少 → 稀疏布局
    if (chars.nodeCount <= 5) {
      return {
        strategy: LayoutStrategy.SPARSE,
        params: StrategyParams[LayoutStrategy.SPARSE],
        reason: '节点数量较少，使用稀疏布局'
      }
    }
    
    // 有明确的层级结构 → 垂直分层
    if (Object.keys(chars.layerDistribution).length >= 3) {
      return {
        strategy: LayoutStrategy.LAYERED_VERTICAL,
        params: StrategyParams[LayoutStrategy.LAYERED_VERTICAL],
        reason: '检测到多层结构'
      }
    }
    
    // 默认 → 垂直分层
    return {
      strategy: LayoutStrategy.LAYERED_VERTICAL,
      params: StrategyParams[LayoutStrategy.LAYERED_VERTICAL],
      reason: '默认布局策略'
    }
  }
  
  /**
   * 根据图表类型获取策略
   */
  static getStrategyForDiagramType(diagramType, chars) {
    switch (diagramType) {
      case DiagramType.ARCHITECTURE:
      case DiagramType.AWS:
        return {
          strategy: chars.nodeCount > 15 ? LayoutStrategy.COMPACT : LayoutStrategy.LAYERED_VERTICAL,
          params: chars.nodeCount > 15 
            ? StrategyParams[LayoutStrategy.COMPACT]
            : StrategyParams[LayoutStrategy.LAYERED_VERTICAL],
          reason: `${diagramType} 类型，${chars.nodeCount > 15 ? '紧凑' : '标准'}布局`
        }
        
      case DiagramType.FLOWCHART:
        return {
          strategy: LayoutStrategy.LAYERED_VERTICAL,
          params: {
            ...StrategyParams[LayoutStrategy.LAYERED_VERTICAL],
            'elk.layered.spacing.nodeNodeBetweenLayers': '80',
            'elk.spacing.nodeNode': '40'
          },
          reason: '流程图类型，使用紧凑垂直布局'
        }
        
      case DiagramType.SEQUENCE:
        return {
          strategy: LayoutStrategy.LAYERED_HORIZONTAL,
          params: {
            ...StrategyParams[LayoutStrategy.LAYERED_HORIZONTAL],
            'elk.layered.spacing.nodeNodeBetweenLayers': '150',
            'elk.spacing.nodeNode': '30'
          },
          reason: '时序图类型，使用水平布局'
        }
        
      case DiagramType.MINDMAP:
        return {
          strategy: LayoutStrategy.TREE,
          params: StrategyParams[LayoutStrategy.TREE],
          reason: '思维导图类型，使用树形布局'
        }
        
      case DiagramType.SWIMLANE:
        return {
          strategy: LayoutStrategy.LAYERED_HORIZONTAL,
          params: {
            ...StrategyParams[LayoutStrategy.LAYERED_HORIZONTAL],
            'elk.partitioning.activate': 'true'
          },
          reason: '泳道图类型，使用分区水平布局'
        }
        
      default:
        return {
          strategy: LayoutStrategy.LAYERED_VERTICAL,
          params: StrategyParams[LayoutStrategy.LAYERED_VERTICAL],
          reason: '默认布局策略'
        }
    }
  }
  
  /**
   * 根据节点数量动态调整间距
   */
  static adjustSpacing(params, nodeCount) {
    const adjusted = { ...params }
    
    if (nodeCount > 30) {
      // 节点很多，减少间距
      adjusted['elk.layered.spacing.nodeNodeBetweenLayers'] = '50'
      adjusted['elk.spacing.nodeNode'] = '25'
    } else if (nodeCount > 20) {
      adjusted['elk.layered.spacing.nodeNodeBetweenLayers'] = '70'
      adjusted['elk.spacing.nodeNode'] = '40'
    } else if (nodeCount < 6) {
      // 节点很少，增加间距
      adjusted['elk.layered.spacing.nodeNodeBetweenLayers'] = '150'
      adjusted['elk.spacing.nodeNode'] = '100'
    }
    
    return adjusted
  }
  
  /**
   * 获取完整的布局参数
   */
  static getLayoutParams(graph, options = {}) {
    const { diagramType = null, optimize = true } = options
    
    // 1. 选择策略
    const selection = this.selectStrategy(graph, diagramType)
    
    // 2. 获取基础参数
    let params = { ...selection.params }
    
    // 3. 根据节点数量调整间距
    if (optimize) {
      params = this.adjustSpacing(params, graph.nodes?.length || 0)
    }
    
    console.log(`[LayoutStrategySelector] 选择策略: ${selection.strategy}, 原因: ${selection.reason}`)
    
    return {
      params,
      strategy: selection.strategy,
      reason: selection.reason
    }
  }
}

// 导出策略常量
export { LayoutStrategy, GraphCharacteristics }
