/**
 * 智能图优化器
 * 在布局前对图进行智能优化，提升最终效果
 */

import { SmartConnector } from './SmartConnector.js'
import { IconMatcher } from './IconMatcher.js'
import { GraphCluster } from './GraphCluster.js'

/**
 * 图优化器类
 */
export class GraphOptimizer {
  /**
   * 完整的智能优化流程
   * @param {Object} graph - 原始图数据
   * @param {Object} options - 优化选项
   * @returns {Object} - 优化后的图数据
   */
  static optimize(graph, options = {}) {
    const {
      enhanceIcons = true,
      inferLayers = true,
      autoGroup = true,
      validateConnections = true,
      verbose = false
    } = options

    let optimizedGraph = { ...graph }
    const report = {
      steps: [],
      warnings: [],
      enhancements: []
    }

    // 步骤1: 图标智能匹配
    if (enhanceIcons) {
      const before = optimizedGraph.nodes.filter(n => n.icon && n.icon !== 'default').length
      optimizedGraph.nodes = IconMatcher.enhanceNodes(optimizedGraph.nodes)
      const after = optimizedGraph.nodes.filter(n => n.icon && n.icon !== 'default').length
      
      if (after > before) {
        report.steps.push(`图标匹配: 增强了 ${after - before} 个节点的图标`)
        report.enhancements.push({
          type: 'icon_enhancement',
          count: after - before
        })
      }
    }

    // 步骤2: 智能推断层级
    if (inferLayers) {
      const nodesWithoutLayer = optimizedGraph.nodes.filter(n => n.layer === undefined).length
      optimizedGraph.nodes = SmartConnector.assignLayers(optimizedGraph.nodes)
      
      if (nodesWithoutLayer > 0) {
        report.steps.push(`层级推断: 为 ${nodesWithoutLayer} 个节点分配了层级`)
        report.enhancements.push({
          type: 'layer_inference',
          count: nodesWithoutLayer
        })
      }
    }

    // 步骤3: 验证连接合理性
    if (validateConnections && optimizedGraph.edges) {
      const warnings = SmartConnector.validateConnections(
        optimizedGraph.nodes,
        optimizedGraph.edges
      )
      
      if (warnings.length > 0) {
        report.warnings.push(...warnings.map(w => w.message))
        if (verbose) {
          console.warn('[GraphOptimizer] 连接警告:', warnings)
        }
      }

      // 步骤3.5: 智能边优化（过滤不合理的边）
      const edgeOptResult = SmartConnector.optimizeEdges(
        optimizedGraph.nodes,
        optimizedGraph.edges
      )
      
      if (edgeOptResult.removed.length > 0) {
        optimizedGraph.edges = edgeOptResult.edges
        report.steps.push(`边优化: 移除了 ${edgeOptResult.removed.length} 条不合理的边`)
        report.warnings.push(...edgeOptResult.warnings)
        if (verbose) {
          console.log('[GraphOptimizer] 边优化结果:', edgeOptResult)
        }
      }

      // 完整性分析
      const completeness = SmartConnector.analyzeCompleteness(
        optimizedGraph.nodes,
        optimizedGraph.edges
      )
      
      report.completenessScore = completeness.score
      if (completeness.issues.length > 0) {
        report.warnings.push(...completeness.issues)
      }
    }

    // 步骤4: 自动分组
    if (autoGroup && (!optimizedGraph.groups || optimizedGraph.groups.length === 0)) {
      const autoGroups = GraphCluster.smartGrouping(
        optimizedGraph.nodes,
        optimizedGraph.edges || [],
        { minGroupSize: 2 }
      )
      
      if (autoGroups.length > 0) {
        optimizedGraph.groups = autoGroups
        report.steps.push(`自动分组: 生成了 ${autoGroups.length} 个分组`)
        report.enhancements.push({
          type: 'auto_grouping',
          count: autoGroups.length
        })
      }
    }

    // 步骤5: 优化节点顺序（按层级排序）
    optimizedGraph.nodes = this.sortNodesByLayer(optimizedGraph.nodes)

    // 步骤6: 去重和清理
    optimizedGraph = this.cleanupGraph(optimizedGraph)

    if (verbose) {
      console.log('[GraphOptimizer] 优化报告:', report)
    }

    return {
      graph: optimizedGraph,
      report
    }
  }

  /**
   * 按层级排序节点
   */
  static sortNodesByLayer(nodes) {
    return [...nodes].sort((a, b) => {
      const layerA = a.layer ?? SmartConnector.getNodeLayer(a)
      const layerB = b.layer ?? SmartConnector.getNodeLayer(b)
      return layerA - layerB
    })
  }

  /**
   * 清理图数据
   */
  static cleanupGraph(graph) {
    const nodeIds = new Set(graph.nodes.map(n => n.id))

    return {
      ...graph,
      // 去重节点
      nodes: graph.nodes.filter((node, index, self) =>
        index === self.findIndex(n => n.id === node.id)
      ),
      // 过滤无效边
      edges: (graph.edges || []).filter(edge =>
        nodeIds.has(edge.from) && 
        nodeIds.has(edge.to) && 
        edge.from !== edge.to
      ),
      // 过滤无效分组
      groups: (graph.groups || []).map(group => ({
        ...group,
        contains: (group.contains || []).filter(id => nodeIds.has(id))
      })).filter(group => group.contains && group.contains.length > 0)
    }
  }

  /**
   * 智能合并两个图（用于增量更新）
   */
  static mergeGraphs(baseGraph, newGraph, options = {}) {
    const { preferNew = true } = options
    
    // 合并节点
    const nodeMap = new Map()
    
    // 先添加基础图的节点
    for (const node of (baseGraph.nodes || [])) {
      nodeMap.set(node.id, node)
    }
    
    // 再添加/覆盖新图的节点
    for (const node of (newGraph.nodes || [])) {
      if (preferNew || !nodeMap.has(node.id)) {
        nodeMap.set(node.id, node)
      }
    }

    // 合并边（去重）
    const edgeSet = new Set()
    const edges = []
    
    for (const edge of [...(baseGraph.edges || []), ...(newGraph.edges || [])]) {
      const key = `${edge.from}->${edge.to}`
      if (!edgeSet.has(key)) {
        edgeSet.add(key)
        edges.push(edge)
      }
    }

    // 合并分组
    const groupMap = new Map()
    for (const group of [...(baseGraph.groups || []), ...(newGraph.groups || [])]) {
      if (!groupMap.has(group.id)) {
        groupMap.set(group.id, group)
      }
    }

    return {
      type: newGraph.type || baseGraph.type,
      title: newGraph.title || baseGraph.title,
      nodes: Array.from(nodeMap.values()),
      edges,
      groups: Array.from(groupMap.values())
    }
  }

  /**
   * 生成图的摘要信息
   */
  static generateSummary(graph) {
    const layers = new Map()
    for (const node of graph.nodes) {
      const layer = node.layer ?? SmartConnector.getNodeLayer(node)
      if (!layers.has(layer)) {
        layers.set(layer, [])
      }
      layers.get(layer).push(node)
    }

    const layerNames = {
      0: '用户层',
      1: '接入层',
      2: '应用层',
      3: '数据层',
      4: '基础设施层'
    }

    const summary = {
      totalNodes: graph.nodes.length,
      totalEdges: graph.edges?.length || 0,
      totalGroups: graph.groups?.length || 0,
      layers: {}
    }

    for (const [layer, nodes] of layers) {
      summary.layers[layerNames[layer] || `第${layer}层`] = nodes.length
    }

    return summary
  }

  /**
   * 检测图的类型
   */
  static detectGraphType(graph) {
    const nodes = graph.nodes || []
    
    // 检测 AWS 图
    const awsNodeCount = nodes.filter(n => 
      n.icon?.startsWith('aws_') || 
      (n.label || '').toLowerCase().includes('aws')
    ).length
    
    if (awsNodeCount >= nodes.length * 0.3) {
      return 'aws'
    }

    // 检测流程图（有开始/结束节点）
    const hasFlowNodes = nodes.some(n => 
      ['start', 'end', 'decision'].includes(n.icon) ||
      /开始|结束|判断/.test(n.label || '')
    )
    
    if (hasFlowNodes) {
      return 'flowchart'
    }

    // 默认为架构图
    return 'architecture'
  }
}
