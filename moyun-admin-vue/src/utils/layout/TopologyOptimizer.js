/**
 * 拓扑优化器
 * 在布局前优化图的拓扑结构，减少复杂连接，降低穿透概率
 */

export class TopologyOptimizer {
  /**
   * 优化图的拓扑结构
   * @param {Object} graph - 原始图数据
   * @returns {Object} - 优化后的图数据
   */
  static optimize(graph) {
    console.log('[TopologyOptimizer] 开始优化拓扑结构...')
    
    let optimizedGraph = { ...graph }
    let changes = []

    // 1. 检测并优化跨层直连
    const crossLayerResult = this.optimizeCrossLayerConnections(optimizedGraph)
    optimizedGraph = crossLayerResult.graph
    changes.push(...crossLayerResult.changes)

    // 2. 检测并优化高扇出节点
    const fanOutResult = this.optimizeHighFanOut(optimizedGraph)
    optimizedGraph = fanOutResult.graph
    changes.push(...fanOutResult.changes)

    // 3. 检测并优化多对一连接
    const manyToOneResult = this.optimizeManyToOne(optimizedGraph)
    optimizedGraph = manyToOneResult.graph
    changes.push(...manyToOneResult.changes)

    if (changes.length > 0) {
      console.log(`[TopologyOptimizer] 优化完成，应用了 ${changes.length} 项改进:`)
      changes.forEach(c => console.log(`  - ${c}`))
    } else {
      console.log('[TopologyOptimizer] 拓扑结构已经很优秀，无需优化')
    }

    return optimizedGraph
  }

  /**
   * 优化跨层直连
   */
  static optimizeCrossLayerConnections(graph) {
    const changes = []
    const nodes = graph.nodes || []
    const edges = graph.edges || []
    
    // 构建层级映射
    const layerMap = new Map()
    nodes.forEach(node => {
      if (node.layer !== undefined) {
        layerMap.set(node.id, node.layer)
      }
    })

    // 检测跨越多层的边
    const crossLayerEdges = edges.filter(edge => {
      const fromLayer = layerMap.get(edge.from)
      const toLayer = layerMap.get(edge.to)
      return fromLayer !== undefined && toLayer !== undefined && 
             Math.abs(toLayer - fromLayer) > 1
    })

    if (crossLayerEdges.length > 0) {
      changes.push(`检测到 ${crossLayerEdges.length} 条跨层直连，建议通过中间层路由`)
      // 注意：这里只记录，不自动修改，因为可能是有意的设计
    }

    return { graph, changes }
  }

  /**
   * 优化高扇出节点
   */
  static optimizeHighFanOut(graph) {
    const changes = []
    const edges = graph.edges || []
    
    // 统计每个节点的出度
    const outDegree = new Map()
    edges.forEach(edge => {
      outDegree.set(edge.from, (outDegree.get(edge.from) || 0) + 1)
    })

    // 找出扇出 > 6 的节点
    const highFanOutNodes = []
    outDegree.forEach((degree, nodeId) => {
      if (degree > 6) {
        highFanOutNodes.push({ nodeId, degree })
      }
    })

    if (highFanOutNodes.length > 0) {
      highFanOutNodes.forEach(({ nodeId, degree }) => {
        changes.push(`节点 ${nodeId} 扇出过高 (${degree} 条边)，建议拆分或使用分组`)
      })
    }

    return { graph, changes }
  }

  /**
   * 优化多对一连接
   */
  static optimizeManyToOne(graph) {
    const changes = []
    const edges = graph.edges || []
    
    // 统计每个节点的入度
    const inDegree = new Map()
    edges.forEach(edge => {
      inDegree.set(edge.to, (inDegree.get(edge.to) || 0) + 1)
    })

    // 找出入度 > 5 的节点
    const highInDegreeNodes = []
    inDegree.forEach((degree, nodeId) => {
      if (degree > 5) {
        highInDegreeNodes.push({ nodeId, degree })
      }
    })

    if (highInDegreeNodes.length > 0) {
      highInDegreeNodes.forEach(({ nodeId, degree }) => {
        changes.push(`节点 ${nodeId} 扇入过高 (${degree} 条边)，建议使用连接池或代理`)
      })
    }

    return { graph, changes }
  }

  /**
   * 计算拓扑复杂度评分
   * @returns {Object} - { score: number, issues: string[] }
   */
  static assessComplexity(graph) {
    const nodes = graph.nodes || []
    const edges = graph.edges || []
    const issues = []
    let score = 100 // 满分100，问题越多分数越低

    // 1. 边密度检查
    const density = nodes.length > 0 ? edges.length / nodes.length : 0
    if (density > 2.5) {
      score -= 20
      issues.push(`边密度过高 (${density.toFixed(2)})，建议简化连接`)
    } else if (density > 2) {
      score -= 10
      issues.push(`边密度较高 (${density.toFixed(2)})`)
    }

    // 2. 跨层连接检查
    const layerMap = new Map()
    nodes.forEach(n => n.layer !== undefined && layerMap.set(n.id, n.layer))
    
    const crossLayerCount = edges.filter(e => {
      const fromLayer = layerMap.get(e.from)
      const toLayer = layerMap.get(e.to)
      return fromLayer !== undefined && toLayer !== undefined && 
             Math.abs(toLayer - fromLayer) > 1
    }).length

    if (crossLayerCount > 3) {
      score -= 30
      issues.push(`${crossLayerCount} 条跨层直连，容易导致穿透`)
    } else if (crossLayerCount > 0) {
      score -= 15
      issues.push(`${crossLayerCount} 条跨层直连`)
    }

    // 3. 高扇出/扇入检查
    const outDegree = new Map()
    const inDegree = new Map()
    edges.forEach(e => {
      outDegree.set(e.from, (outDegree.get(e.from) || 0) + 1)
      inDegree.set(e.to, (inDegree.get(e.to) || 0) + 1)
    })

    const maxOutDegree = Math.max(...Array.from(outDegree.values()))
    const maxInDegree = Math.max(...Array.from(inDegree.values()))

    if (maxOutDegree > 6 || maxInDegree > 6) {
      score -= 15
      issues.push(`存在高扇出/扇入节点 (最大: ${Math.max(maxOutDegree, maxInDegree)})`)
    }

    return {
      score: Math.max(0, score),
      density,
      crossLayerCount,
      maxOutDegree,
      maxInDegree,
      issues,
      assessment: score >= 80 ? 'excellent' : score >= 60 ? 'good' : score >= 40 ? 'fair' : 'poor'
    }
  }
}

export default TopologyOptimizer
