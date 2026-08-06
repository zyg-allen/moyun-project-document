/**
 * ELK 布局引擎 - 统一入口 (V3+ 智能增强版)
 * 
 * 核心职责：
 * 1. 智能识别 AI 输出格式
 * 2. Schema 校验确保数据合法
 * 3. 图标智能匹配 (RAG)
 * 4. 智能层级推断
 * 5. 连接验证与优化
 * 6. 自动分组 (Graph Clustering)
 * 7. 调用 ELK 计算布局
 * 8. 生成 Draw.io XML
 * 9. 性能管理和降级
 */

import { ELKAdapter, elkAdapter } from './ELKAdapter.js'
import { DrawioXmlBuilder } from './DrawioXmlBuilder.js'
import { StyleRegistry } from './StyleRegistry.js'
import { FormatDetector, InputFormat } from './FormatDetector.js'
import { EditTracker, editTracker } from './EditTracker.js'
import { PerformanceManager, ProcessingStrategy, SimplifiedLayout, LayeredDisplayLayout } from './PerformanceManager.js'
import { DiagramType } from './types.js'
// V3 增强模块
import { SchemaValidator } from './SchemaValidator.js'
import { GraphCluster } from './GraphCluster.js'
import { IconMatcher, ProjectKnowledgeBase } from './IconMatcher.js'
import { ArchitectureAwareness } from './ArchitectureAwareness.js'
// V3+ 智能模块
import { SmartConnector } from './SmartConnector.js'
import { GraphOptimizer } from './GraphOptimizer.js'
import { LayoutStrategySelector, LayoutStrategy } from './LayoutStrategySelector.js'
import { SmartEdgeRouter, SmartNodeSizer } from './SmartEdgeRouter.js'

/**
 * 布局引擎类
 */
export class LayoutEngine {
  constructor() {
    this.elkAdapter = elkAdapter
    this.cache = new Map()
    this.maxCacheSize = 10
  }

  /**
   * 处理语义化图数据，返回 Draw.io XML (V3+ 智能增强版)
   * @param {Object} graph - 语义化图数据
   * @param {Object} options - 处理选项
   * @returns {Promise<string>} - Draw.io XML
   */
  async process(graph, options = {}) {
    const { skipEnhancement = false, verbose = false } = options

    if (!graph || !graph.nodes || graph.nodes.length === 0) {
      throw new Error('图表必须包含至少一个节点')
    }

    console.log('[LayoutEngine] 开始处理，节点数:', graph.nodes.length)

    // 1. Schema 校验
    const validation = SchemaValidator.validateAndFix(graph)
    if (!validation.valid) {
      console.error('[LayoutEngine] Schema 校验失败:', validation.result.errors)
      throw new Error('图数据校验失败: ' + validation.result.errors[0]?.message)
    }
    let enhancedGraph = validation.graph

    // 2. 性能策略选择
    const strategy = PerformanceManager.selectStrategy(enhancedGraph.nodes.length, enhancedGraph.edges?.length || 0)
    console.log('[LayoutEngine] 策略:', strategy.type)

    if (!strategy.canProceed) {
      throw new Error(strategy.error)
    }

    // 3. 缓存检查
    const cacheKey = JSON.stringify(enhancedGraph)
    if (this.cache.has(cacheKey)) {
      console.log('[LayoutEngine] 命中缓存')
      return this.cache.get(cacheKey)
    }

    // 4. 智能优化（使用 GraphOptimizer 统一处理）
    if (!skipEnhancement) {
      const optimizeResult = GraphOptimizer.optimize(enhancedGraph, {
        enhanceIcons: true,
        inferLayers: true,
        autoGroup: true,
        validateConnections: true,
        verbose
      })
      
      enhancedGraph = optimizeResult.graph
      
      // 输出优化报告
      if (optimizeResult.report.warnings.length > 0) {
        console.warn('[LayoutEngine] 优化警告:', optimizeResult.report.warnings)
      }
      if (optimizeResult.report.completenessScore !== undefined) {
        console.log('[LayoutEngine] 完整性评分:', optimizeResult.report.completenessScore)
      }
    }

    // 5. 智能节点尺寸计算（根据文字内容自适应）
    enhancedGraph.nodes = SmartNodeSizer.enhanceNodes(enhancedGraph.nodes)
    console.log('[LayoutEngine] 节点尺寸已自适应')

    // 6. 修复图数据
    const fixedGraph = this.fixGraph(enhancedGraph)

    // 7. 执行布局（计算节点位置）
    let layoutedGraph
    try {
      // 判断是否使用分层展示布局
      // 条件：有分组 + (类型是架构图 或 边很少/没有)
      const hasGroups = fixedGraph.groups && fixedGraph.groups.length >= 2
      const isArchitectureType = fixedGraph.type === DiagramType.ARCHITECTURE
      const hasMinimalEdges = !fixedGraph.edges || fixedGraph.edges.length <= 3
      const useLayeredDisplay = hasGroups && (isArchitectureType || hasMinimalEdges)
      
      if (useLayeredDisplay) {
        console.log('[LayoutEngine] 检测到架构图/分层展示结构，使用分层展示布局')
        layoutedGraph = LayeredDisplayLayout.layout(fixedGraph)
      } else if (strategy.type === ProcessingStrategy.SIMPLIFIED) {
        layoutedGraph = SimplifiedLayout.layout(fixedGraph)
      } else {
        layoutedGraph = await this.elkAdapter.layout(fixedGraph)
      }
    } catch (error) {
      console.warn('[LayoutEngine] 布局失败，使用降级布局:', error)
      layoutedGraph = SimplifiedLayout.layout(fixedGraph)
    }

    // 8. 智能边路由（计算避障路径）
    layoutedGraph = SmartEdgeRouter.routeEdges(layoutedGraph)
    console.log('[LayoutEngine] 边路由已计算')

    // 9. 生成 XML
    const xml = DrawioXmlBuilder.build(layoutedGraph)

    // 10. 缓存
    if (this.cache.size >= this.maxCacheSize) {
      const firstKey = this.cache.keys().next().value
      this.cache.delete(firstKey)
    }
    this.cache.set(cacheKey, xml)

    console.log('[LayoutEngine] 处理完成，XML 长度:', xml.length)
    return xml
  }

  /**
   * 带锁定节点的增量布局
   * @param {Object} graph - 图数据
   * @param {Map} lockedPositions - 锁定的节点位置
   */
  async processWithLocks(graph, lockedPositions) {
    // Schema 校验
    const validation = SchemaValidator.validateAndFix(graph)
    if (!validation.valid) {
      throw new Error('图数据校验失败: ' + validation.result.errors[0]?.message)
    }
    let enhancedGraph = validation.graph

    // 图标智能匹配
    enhancedGraph.nodes = IconMatcher.enhanceNodes(enhancedGraph.nodes)

    // 智能节点尺寸计算
    enhancedGraph.nodes = SmartNodeSizer.enhanceNodes(enhancedGraph.nodes)

    // 修复并布局
    const fixedGraph = this.fixGraph(enhancedGraph)
    let layoutedGraph = await this.elkAdapter.layoutWithLocks(fixedGraph, lockedPositions)

    // 智能边路由
    layoutedGraph = SmartEdgeRouter.routeEdges(layoutedGraph)

    return DrawioXmlBuilder.build(layoutedGraph)
  }

  /**
   * 修复图数据中的常见问题
   */
  fixGraph(graph) {
    const nodeIds = new Set((graph.nodes || []).map(n => n.id))

    return {
      ...graph,
      type: graph.type || DiagramType.ARCHITECTURE,
      // 去除重复节点
      nodes: (graph.nodes || []).filter((node, index, self) =>
        index === self.findIndex(n => n.id === node.id)
      ),
      // 过滤无效边
      edges: (graph.edges || []).filter(edge =>
        nodeIds.has(edge.from) && nodeIds.has(edge.to) && edge.from !== edge.to
      )
    }
  }

  /**
   * 清除缓存
   */
  clearCache() {
    this.cache.clear()
  }
}

// 导出单例
export const layoutEngine = new LayoutEngine()

/**
 * 统一处理器
 * 自动识别格式并处理
 */
export class UnifiedProcessor {
  /**
   * 处理 AI 响应
   * @param {string} aiResponse - AI 响应内容
   * @param {string} currentXml - 当前图表 XML（用于编辑）
   * @param {Object} options - 选项
   * @returns {Promise<Object>} - { success, xml, method, error }
   */
  static async process(aiResponse, currentXml = '', options = {}) {
    const detection = FormatDetector.detect(aiResponse)

    console.log('[UnifiedProcessor] 检测到格式:', detection.format, '置信度:', detection.confidence)

    switch (detection.format) {
      case InputFormat.GRAPH_JSON:
        // 新格式：使用 ELK 布局
        try {
          const graph = detection.content

          // 判断是否需要增量布局
          const hasEdits = editTracker.hasManualEdits()
          let xml

          if (hasEdits && options.preserveEdits) {
            const lockedPositions = editTracker.getLockedPositions()
            xml = await layoutEngine.processWithLocks(graph, lockedPositions)
          } else {
            xml = await layoutEngine.process(graph)
          }

          // 返回处理结果
          return { success: true, xml, graphData: graph, method: 'elk', format: 'graph_json' }
        } catch (error) {
          return { success: false, error: error.message, method: 'elk' }
        }

      case InputFormat.XML_DISPLAY:
        // 现有 XML 格式：直接使用
        let xml = detection.content
        if (xml.startsWith('<root>') && !xml.includes('<mxGraphModel>')) {
          xml = `<mxGraphModel>${xml}</mxGraphModel>`
        }
        return { success: true, xml, method: 'direct', format: 'xml_display' }

      case InputFormat.XML_EDIT:
        // 编辑指令：应用到现有 XML
        if (!currentXml) {
          return { success: false, error: '没有现有图表可编辑', method: 'edit' }
        }
        const newXml = this.applyEdits(currentXml, detection.content)
        if (newXml === currentXml) {
          return { success: false, error: '编辑未生效，搜索模式不匹配', method: 'edit' }
        }
        return { success: true, xml: newXml, method: 'edit', format: 'xml_edit' }

      case InputFormat.LEGACY_JSON:
        // 旧格式：尝试转换为新格式处理
        try {
          const legacyGraph = this.convertLegacyFormat(detection.content)
          const xml = await layoutEngine.process(legacyGraph)
          return { success: true, xml, method: 'elk_legacy', format: 'legacy_json' }
        } catch (error) {
          return { success: false, error: '旧格式转换失败: ' + error.message, method: 'legacy' }
        }

      default:
        return { success: false, error: '无法识别的输出格式', method: 'unknown' }
    }
  }

  /**
   * 应用编辑指令
   */
  static applyEdits(xml, edits) {
    if (!xml || !edits || !edits.edits) return xml

    let result = xml

    for (const edit of edits.edits) {
      if (!edit.search || edit.replace === undefined) continue

      // 策略1：精确匹配
      if (result.includes(edit.search)) {
        result = result.replace(edit.search, edit.replace)
        continue
      }

      // 策略2：去除空格匹配
      const trimmedSearch = edit.search.trim()
      if (result.includes(trimmedSearch)) {
        result = result.replace(trimmedSearch, edit.replace.trim())
        continue
      }

      // 策略3：通过 ID 匹配
      const idMatch = edit.search.match(/id="([^"]+)"/)
      if (idMatch) {
        const searchId = idMatch[1]
        const escapedId = searchId.replace(/[.*+?^${}()|[\]\\]/g, '\\$&')
        const cellRegex = new RegExp(
          `<mxCell[^>]*id="${escapedId}"[^>]*(?:\\/>|>[\\s\\S]*?<\\/mxCell>)`,
          'g'
        )
        const newResult = result.replace(cellRegex, edit.replace)
        if (newResult !== result) {
          result = newResult
        }
      }
    }

    return result
  }

  /**
   * 转换旧格式为新格式
   */
  static convertLegacyFormat(legacy) {
    // 处理分层格式
    if (legacy.layers && Array.isArray(legacy.layers)) {
      const nodes = []
      const edges = []

      legacy.layers.forEach((layer, layerIndex) => {
        const layerNodes = layer.nodes || []
        layerNodes.forEach(node => {
          nodes.push({
            id: node.id || node.name,
            label: node.label || node.name || node.id,
            icon: node.icon || node.type,
            layer: layerIndex
          })
        })
      })

      // 从 connections 提取边
      if (legacy.connections) {
        legacy.connections.forEach(conn => {
          edges.push({
            from: conn.from || conn.source,
            to: conn.to || conn.target,
            label: conn.label
          })
        })
      }

      return { type: DiagramType.ARCHITECTURE, nodes, edges }
    }

    // 处理 nodes/edges 格式
    if (legacy.nodes) {
      return {
        type: legacy.type || DiagramType.ARCHITECTURE,
        nodes: legacy.nodes.map(n => ({
          id: n.id || n.name,
          label: n.label || n.name || n.id,
          icon: n.icon || n.type
        })),
        edges: (legacy.edges || []).map(e => ({
          from: e.from || e.source,
          to: e.to || e.target,
          label: e.label
        }))
      }
    }

    throw new Error('无法识别的旧格式结构')
  }
}

// 导出所有模块
export {
  // 核心模块
  ELKAdapter,
  elkAdapter,
  DrawioXmlBuilder,
  StyleRegistry,
  FormatDetector,
  InputFormat,
  EditTracker,
  editTracker,
  PerformanceManager,
  ProcessingStrategy,
  SimplifiedLayout,
  DiagramType,
  // V3 增强模块
  SchemaValidator,
  GraphCluster,
  IconMatcher,
  ProjectKnowledgeBase,
  ArchitectureAwareness,
  // V3+ 智能模块
  SmartConnector,
  GraphOptimizer,
  LayoutStrategySelector,
  LayoutStrategy,
  SmartEdgeRouter,
  SmartNodeSizer
}
