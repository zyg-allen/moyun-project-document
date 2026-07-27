/**
 * ELK 布局引擎
 * 负责将语义化的图表数据转换为带精确坐标的布局
 */
import ELK from 'elkjs/lib/elk.bundled.js'

export class ELKLayoutEngine {
  constructor() {
    this.elk = new ELK()
    this.defaultNodeWidth = 140
    this.defaultOptions = {
      'elk.algorithm': 'layered',
      'elk.direction': 'DOWN',
      // 🔥🔥🔥 强制使用正交路由（只有水平和垂直线段，避免斜线交叉）
      'elk.edgeRouting': 'ORTHOGONAL',
      // 🔥🔥🔥 超大间距！
      'elk.layered.spacing.nodeNodeBetweenLayers': '200',  // 层间距 200px
      'elk.spacing.nodeNode': '150',  // 节点间距 150px
      'elk.layered.spacing.edgeNode': '80',  // 边与节点距离 80px
      'elk.spacing.edgeEdge': '50',  // 边与边间距 50px
      // 🔥 优化分层策略
      'elk.layered.layering.strategy': 'NETWORK_SIMPLEX',  // 更好的分层
      // 🔥🔥🔥 最大化交叉最小化
      'elk.layered.crossingMinimization.strategy': 'LAYER_SWEEP',
      'elk.layered.crossingMinimization.greedySwitch.type': 'TWO_SIDED',
      'elk.layered.thoroughness': '50',  // 最大优化迭代
      // 🔥 节点放置
      'elk.layered.nodePlacement.strategy': 'NETWORK_SIMPLEX',
      'elk.layered.nodePlacement.bk.fixedAlignment': 'BALANCED',
      'elk.layered.nodePlacement.bk.edgeStraightening': 'IMPROVE_STRAIGHTNESS',
      // 🔥🔥🔥 关键：避免边触碰节点
      'elk.allowEdgeToNodeTouches': 'false',
      'elk.spacing.edgeLabel': '20',
      'elk.spacing.portPort': '40',
      // 🔥 边路由优化
      'elk.layered.edgeRouting.selfLoopDistribution': 'EQUALLY',
      'elk.layered.edgeRouting.slotAssignmentByNodeSize': 'true',
      'elk.layered.mergeEdges': 'false',
      'elk.separateConnectedComponents': 'false',
      'elk.layered.compaction.connectedComponents': 'true',
      // 🔥 强制边使用不同的端口，避免重叠
      'elk.layered.unnecessaryBendpoints': 'false',
      'elk.layered.feedbackEdges': 'true'
    }
  }
  
  /**
   * 对图表进行布局
   * @param {Object} graph - AI 输出的语义化图表 {nodes, edges, groups}
   * @returns {Promise<Object>} - 带坐标的图表
   */
  async layout(graph) {
    console.log('[ELK] 开始布局计算...', graph)
    
    try {
      // 1. 构建 ELK 图
      const elkGraph = this.buildELKGraph(graph)
      
      // 2. 调用 ELK 布局
      const layoutedGraph = await this.elk.layout(elkGraph)
      
      // 3. 转换回标准格式
      const result = this.convertFromELK(layoutedGraph, graph)
      
      console.log('[ELK] 布局计算完成', result)
      return result
      
    } catch (error) {
      console.error('[ELK] 布局失败:', error)
      throw new Error(`ELK 布局失败: ${error.message}`)
    }
  }
  
  /**
   * 构建 ELK 图结构
   */
  buildELKGraph(graph) {
    const { nodes = [], edges = [], groups = [] } = graph
    
    // 根据图的特征智能选择配置
    const config = this.getAdaptiveConfig(graph)
    
    // 构建节点映射（包括是否在分组内）
    const nodeParentMap = new Map()
    groups.forEach(group => {
      (group.contains || []).forEach(nodeId => {
        nodeParentMap.set(nodeId, group.id)
      })
    })
    
    // 如果有分组，使用层级布局
    if (groups.length > 0) {
      return this.buildHierarchicalGraph(graph, config, nodeParentMap)
    }
    
    // 无分组：扁平布局
    return {
      id: 'root',
      layoutOptions: config,
      children: nodes.map(node => ({
        id: node.id,
        width: node.width || this.calculateNodeWidth(node.label),
        height: node.height || 50,
        labels: node.label ? [{ text: node.label }] : [],
        // 使用 layer 属性作为 ELK 的层级提示
        // layerConstraint 用于强制节点在特定层
        layoutOptions: node.layer !== undefined ? {
          'layerConstraint': `LAYER_${node.layer}`
        } : {}
      })),
      edges: edges.map((edge, index) => ({
        id: edge.id || `e${index}`,
        sources: [edge.from],
        targets: [edge.to],
        labels: edge.label ? [{ text: edge.label }] : []
      }))
    }
  }
  
  /**
   * 构建层级图（带分组）
   */
  buildHierarchicalGraph(graph, config, nodeParentMap) {
    const { nodes = [], edges = [], groups = [] } = graph
    const graphType = graph.type || 'architecture'
    
    // 🔥 根据图表类型设置分组内部布局
    const getGroupLayoutOptions = () => {
      if (graphType === 'flowchart' || graphType === 'sequence') {
        // 流程图分组：紧凑布局，节点横向排列
        return {
          'elk.algorithm': 'layered',
          'elk.direction': 'RIGHT',  // 分组内部横向排列
          'elk.padding': '[top=40,left=15,bottom=15,right=15]',
          'elk.spacing.nodeNode': '40',  // 紧凑间距
          'elk.layered.spacing.nodeNodeBetweenLayers': '60',
          'elk.layered.nodePlacement.strategy': 'SIMPLE'
        }
      }
      // 架构图分组：宽松布局
      return {
        'elk.algorithm': 'layered',
        'elk.direction': 'DOWN',
        'elk.padding': '[top=50,left=20,bottom=20,right=20]',
        'elk.spacing.nodeNode': '60',
        'elk.layered.spacing.nodeNodeBetweenLayers': '80'
      }
    }
    
    const groupLayoutOptions = getGroupLayoutOptions()
    
    // 构建分组容器
    const groupNodes = groups.map(group => ({
      id: group.id,
      labels: group.label ? [{ text: group.label }] : [],
      layoutOptions: groupLayoutOptions,
      children: nodes
        .filter(n => nodeParentMap.get(n.id) === group.id)
        .map(node => ({
          id: node.id,
          width: node.width || this.calculateNodeWidth(node.label),
          height: node.height || 50,
          labels: node.label ? [{ text: node.label }] : [],
          layoutOptions: node.layer !== undefined ? {
            'layerConstraint': `LAYER_${node.layer}`
          } : {}
        }))
    }))
    
    // 不在任何分组内的节点
    const orphanNodes = nodes
      .filter(n => !nodeParentMap.has(n.id))
      .map(node => ({
        id: node.id,
        width: node.width || this.calculateNodeWidth(node.label),
        height: node.height || 50,
        labels: node.label ? [{ text: node.label }] : [],
        layoutOptions: node.layer !== undefined ? {
          'layerConstraint': `LAYER_${node.layer}`
        } : {}
      }))
    
    return {
      id: 'root',
      layoutOptions: config,
      children: [...groupNodes, ...orphanNodes],
      edges: edges.map((edge, index) => ({
        id: edge.id || `e${index}`,
        sources: [edge.from],
        targets: [edge.to],
        labels: edge.label ? [{ text: edge.label }] : []
      }))
    }
  }
  
  /**
   * 根据文字计算节点宽度
   */
  calculateNodeWidth(label) {
    if (!label) return this.defaultNodeWidth
    
    // 中文字符宽度约 14px，英文约 8px，括号等符号约 10px
    let width = 0
    for (const char of label) {
      if (/[\u4e00-\u9fa5]/.test(char)) {
        width += 14  // 中文
      } else if (/[（）()【】\[\]]/.test(char)) {
        width += 10  // 括号
      } else {
        width += 8   // 英文/数字
      }
    }
    
    // 最大宽度 280px，避免长文字被截断
    return Math.max(this.defaultNodeWidth, Math.min(width + 40, 280))
  }
  
  /**
   * 根据图的特征自适应配置
   */
  getAdaptiveConfig(graph) {
    const nodeCount = graph.nodes?.length || 0
    const edgeCount = graph.edges?.length || 0
    const groupCount = graph.groups?.length || 0
    const density = nodeCount > 0 ? edgeCount / nodeCount : 0
    const maxLayer = Math.max(...(graph.nodes || []).map(n => n.layer || 0))
    const graphType = graph.type || 'architecture'
    
    const config = { ...this.defaultOptions }
    
    // 🔥🔥🔥 流程图专用配置
    if (graphType === 'flowchart' || graphType === 'sequence') {
      console.log('[ELK] 检测到流程图类型，使用流程图专用配置')
      
      // 流程图使用纵向布局，更紧凑
      config['elk.direction'] = 'DOWN'
      
      // 🔥 关键：限制每层节点数，避免横向过宽
      config['elk.aspectRatio'] = '0.8'  // 偏向纵向布局
      
      // 🔥 流程图间距：层间距大，节点间距适中
      config['elk.layered.spacing.nodeNodeBetweenLayers'] = '120'  // 层间距
      config['elk.spacing.nodeNode'] = '80'  // 节点间距
      config['elk.layered.spacing.edgeNode'] = '60'  // 边-节点间距
      config['elk.spacing.edgeEdge'] = '40'  // 边间距
      
      // 🔥 强制线性流程：优先顺序排列
      config['elk.layered.layering.strategy'] = 'LONGEST_PATH'
      config['elk.layered.nodePlacement.strategy'] = 'SIMPLE'
      
      // 🔥 最大化边对齐，减少交叉
      config['elk.layered.crossingMinimization.strategy'] = 'LAYER_SWEEP'
      config['elk.layered.crossingMinimization.greedySwitch.type'] = 'TWO_SIDED'
      config['elk.layered.thoroughness'] = '100'
      
      // 🔥 边直线化
      config['elk.layered.nodePlacement.bk.edgeStraightening'] = 'IMPROVE_STRAIGHTNESS'
      
      // 如果分组很多，增加分组间距
      if (groupCount > 3) {
        config['elk.layered.spacing.nodeNodeBetweenLayers'] = '150'
        config['elk.spacing.nodeNode'] = '100'
      }
      
    } else {
      // 🔥🔥🔥 架构图配置：超大间距
      config['elk.layered.spacing.nodeNodeBetweenLayers'] = '250'
      config['elk.spacing.nodeNode'] = '200'
      config['elk.layered.spacing.edgeNode'] = '100'
      config['elk.spacing.edgeEdge'] = '60'
      
      // 🔥 根据边密度进一步增加间距
      if (density > 1.5) {
        config['elk.layered.spacing.nodeNodeBetweenLayers'] = '300'
        config['elk.spacing.nodeNode'] = '250'
        config['elk.layered.spacing.edgeNode'] = '120'
        config['elk.spacing.edgeEdge'] = '80'
        config['elk.layered.thoroughness'] = '100'
      } else if (density > 1) {
        config['elk.layered.spacing.nodeNodeBetweenLayers'] = '280'
        config['elk.spacing.nodeNode'] = '220'
        config['elk.layered.spacing.edgeNode'] = '110'
        config['elk.spacing.edgeEdge'] = '70'
      }
    }
    
    console.log('[ELK] 自适应配置:', {
      graphType,
      nodeCount,
      edgeCount,
      groupCount,
      density: density.toFixed(2),
      maxLayer,
      config: {
        direction: config['elk.direction'],
        nodeSpacing: config['elk.spacing.nodeNode'],
        layerSpacing: config['elk.layered.spacing.nodeNodeBetweenLayers'],
        edgeNodeSpacing: config['elk.layered.spacing.edgeNode']
      }
    })
    
    return config
  }
  
  /**
   * 将 ELK 结果转换回标准格式
   */
  convertFromELK(elkResult, originalGraph) {
    // 递归提取所有节点（包括分组内的节点）
    const extractNodes = (parent, offsetX = 0, offsetY = 0) => {
      const nodes = []
      
      if (parent.children) {
        parent.children.forEach(child => {
          if (child.children && child.children.length > 0) {
            // 这是一个分组容器
            const groupNodes = extractNodes(child, child.x || 0, child.y || 0)
            nodes.push(...groupNodes)
          } else {
            // 这是一个普通节点
            const originalNode = originalGraph.nodes.find(n => n.id === child.id) || {}
            nodes.push({
              ...originalNode,
              x: Math.round((child.x || 0) + offsetX),
              y: Math.round((child.y || 0) + offsetY),
              width: Math.round(child.width || 120),
              height: Math.round(child.height || 50)
            })
          }
        })
      }
      
      return nodes
    }
    
    const nodes = extractNodes(elkResult)
    
    // 提取分组信息（包括坐标）
    const extractGroups = (parent) => {
      const groups = []
      
      if (parent.children) {
        parent.children.forEach(child => {
          if (child.children && child.children.length > 0) {
            // 这是一个分组容器
            const originalGroup = originalGraph.groups?.find(g => g.id === child.id) || {}
            groups.push({
              ...originalGroup,
              id: child.id,
              x: Math.round(child.x || 0),
              y: Math.round(child.y || 0),
              width: Math.round(child.width || 200),
              height: Math.round(child.height || 100)
            })
          }
        })
      }
      
      return groups
    }
    
    const groups = extractGroups(elkResult)
    
    const edges = elkResult.edges.map(elkEdge => {
      // 找到原始边数据
      const originalEdge = originalGraph.edges.find(e => 
        (e.id && e.id === elkEdge.id) || 
        (e.from === elkEdge.sources[0] && e.to === elkEdge.targets[0])
      ) || {}
      
      // 提取路径点
      const bendPoints = this.extractBendPoints(elkEdge)
      
      // 计算连接点
      const connectionPoints = this.calculateConnectionPoints(elkEdge, nodes)
      
      return {
        ...originalEdge,
        from: elkEdge.sources[0],
        to: elkEdge.targets[0],
        ...connectionPoints,
        points: bendPoints.length > 0 ? bendPoints : undefined
      }
    })
    
    return {
      ...originalGraph,
      nodes,
      edges,
      groups: groups.length > 0 ? groups : originalGraph.groups,
      layoutEngine: 'elk',
      layoutTimestamp: new Date().toISOString()
    }
  }
  
  /**
   * 提取 ELK 边的路径点
   */
  extractBendPoints(elkEdge) {
    const points = []
    
    if (elkEdge.sections && elkEdge.sections.length > 0) {
      elkEdge.sections.forEach(section => {
        // 添加起点
        if (section.startPoint) {
          points.push({
            x: Math.round(section.startPoint.x),
            y: Math.round(section.startPoint.y)
          })
        }
        
        // 添加弯折点
        if (section.bendPoints) {
          section.bendPoints.forEach(bp => {
            points.push({
              x: Math.round(bp.x),
              y: Math.round(bp.y)
            })
          })
        }
        
        // 添加终点
        if (section.endPoint) {
          points.push({
            x: Math.round(section.endPoint.x),
            y: Math.round(section.endPoint.y)
          })
        }
      })
    }
    
    return points
  }
  
  /**
   * 计算边的连接点（exitX, exitY, entryX, entryY）
   */
  calculateConnectionPoints(elkEdge, nodes) {
    const sourceNode = nodes.find(n => n.id === elkEdge.sources[0])
    const targetNode = nodes.find(n => n.id === elkEdge.targets[0])
    
    if (!sourceNode || !targetNode) {
      return {
        exitX: 0.5,
        exitY: 1,
        entryX: 0.5,
        entryY: 0
      }
    }
    
    // 从 ELK 的路径信息推断连接点
    const section = elkEdge.sections?.[0]
    if (!section) {
      // 默认：从下方出，从上方入
      return {
        exitX: 0.5,
        exitY: 1,
        entryX: 0.5,
        entryY: 0
      }
    }
    
    // 计算起点相对于源节点的位置
    const startPoint = section.startPoint
    if (startPoint) {
      const exitX = (startPoint.x - sourceNode.x) / sourceNode.width
      const exitY = (startPoint.y - sourceNode.y) / sourceNode.height
      
      const endPoint = section.endPoint || section.bendPoints?.[section.bendPoints.length - 1]
      if (endPoint) {
        const entryX = (endPoint.x - targetNode.x) / targetNode.width
        const entryY = (endPoint.y - targetNode.y) / targetNode.height
        
        return {
          exitX: Math.max(0, Math.min(1, exitX)),
          exitY: Math.max(0, Math.min(1, exitY)),
          entryX: Math.max(0, Math.min(1, entryX)),
          entryY: Math.max(0, Math.min(1, entryY))
        }
      }
    }
    
    // 降级：根据节点位置推断
    if (sourceNode.y < targetNode.y) {
      // 从上到下
      return { exitX: 0.5, exitY: 1, entryX: 0.5, entryY: 0 }
    } else if (sourceNode.x < targetNode.x) {
      // 从左到右
      return { exitX: 1, exitY: 0.5, entryX: 0, entryY: 0.5 }
    } else {
      // 从右到左
      return { exitX: 0, exitY: 0.5, entryX: 1, entryY: 0.5 }
    }
  }
}

export default new ELKLayoutEngine()
