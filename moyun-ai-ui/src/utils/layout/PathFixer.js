/**
 * 路径修复器
 * 修复 ELK 布局后仍然存在的边穿透问题
 */

export class PathFixer {
  /**
   * 修复穿透的边
   * @param {Object} graph - 布局后的图表
   * @param {Array} penetrationErrors - 穿透错误列表
   * @returns {Object} - 修复后的图表
   */
  static fixPenetrations(graph, penetrationErrors) {
    if (!penetrationErrors || penetrationErrors.length === 0) {
      return graph
    }

    console.log(`[PathFixer] 开始修复 ${penetrationErrors.length} 个穿透问题`)

    const fixedGraph = { ...graph }
    const nodeMap = new Map(graph.nodes.map(n => [n.id, n]))
    const fixedEdges = []

    graph.edges.forEach(edge => {
      // 检查这条边是否有穿透问题
      const edgeErrors = penetrationErrors.filter(err => 
        err.edgeId === edge.id || err.edgeId === `${edge.from}-${edge.to}`
      )

      if (edgeErrors.length > 0) {
        // 尝试修复这条边
        const fixedEdge = this.fixEdge(edge, edgeErrors, nodeMap, graph.nodes)
        fixedEdges.push(fixedEdge)
        console.log(`[PathFixer] 修复边: ${edge.from} → ${edge.to}`)
      } else {
        fixedEdges.push(edge)
      }
    })

    fixedGraph.edges = fixedEdges
    return fixedGraph
  }

  /**
   * 修复单条边
   */
  static fixEdge(edge, errors, nodeMap, allNodes) {
    const sourceNode = nodeMap.get(edge.from)
    const targetNode = nodeMap.get(edge.to)

    if (!sourceNode || !targetNode) {
      return edge
    }

    // 收集被穿透的节点
    const penetratedNodes = errors.map(err => nodeMap.get(err.nodeId)).filter(Boolean)

    if (penetratedNodes.length === 0) {
      return edge
    }

    // 策略1：使用 A* 算法寻找绕行路径
    const newPath = this.findAvoidancePath(sourceNode, targetNode, penetratedNodes, allNodes)

    if (newPath && newPath.length > 0) {
      return {
        ...edge,
        points: newPath,
        fixed: true
      }
    }

    // 策略2：简单绕行（向外偏移）
    const simpleBypass = this.createSimpleBypass(sourceNode, targetNode, penetratedNodes)
    return {
      ...edge,
      points: simpleBypass,
      fixed: true
    }
  }

  /**
   * A* 寻找避障路径（增强版）
   */
  static findAvoidancePath(source, target, obstacles, allNodes) {
    const startX = source.x + (source.width || 120) / 2
    const startY = source.y + (source.height || 50)
    const endX = target.x + (target.width || 120) / 2
    const endY = target.y

    // 多策略尝试，选择最优路径
    const strategies = []

    // 策略1：经典曼哈顿路径（先水平后垂直）
    strategies.push(this.tryManhattanPath(startX, startY, endX, endY, obstacles, 'HV'))
    
    // 策略2：经典曼哈顿路径（先垂直后水平）
    strategies.push(this.tryManhattanPath(startX, startY, endX, endY, obstacles, 'VH'))

    // 策略3：曲线绕行（左侧）
    strategies.push(this.tryCurvedPath(startX, startY, endX, endY, obstacles, 'left'))
    
    // 策略4：曲线绕行（右侧）
    strategies.push(this.tryCurvedPath(startX, startY, endX, endY, obstacles, 'right'))

    // 策略5：多段折线（灵活绕行）
    strategies.push(this.tryMultiSegmentPath(startX, startY, endX, endY, obstacles, allNodes))

    // 选择最优策略（无碰撞 > 碰撞少 > 路径短）
    const validPaths = strategies.filter(s => s && s.collisions === 0)
    if (validPaths.length > 0) {
      // 选择最短的无碰撞路径
      return validPaths.sort((a, b) => a.length - b.length)[0].points
    }

    // 如果都有碰撞，选择碰撞最少的
    const bestPath = strategies.filter(s => s).sort((a, b) => 
      a.collisions - b.collisions || a.length - b.length
    )[0]

    return bestPath ? bestPath.points : []
  }

  /**
   * 尝试曼哈顿路径
   */
  static tryManhattanPath(startX, startY, endX, endY, obstacles, order) {
    const points = []
    
    if (order === 'HV') {
      // 先水平后垂直
      points.push({ x: endX, y: startY })
    } else {
      // 先垂直后水平
      points.push({ x: startX, y: endY })
    }

    // 检查碰撞
    const collisions = this.countPathCollisions([
      { x: startX, y: startY },
      ...points,
      { x: endX, y: endY }
    ], obstacles)

    return {
      points,
      collisions,
      length: Math.abs(endX - startX) + Math.abs(endY - startY)
    }
  }

  /**
   * 尝试曲线绕行
   */
  static tryCurvedPath(startX, startY, endX, endY, obstacles, direction) {
    const midX = (startX + endX) / 2
    const midY = (startY + endY) / 2
    const offset = direction === 'left' ? -120 : 120

    const points = [
      { x: midX + offset, y: midY }
    ]

    const collisions = this.countPathCollisions([
      { x: startX, y: startY },
      ...points,
      { x: endX, y: endY }
    ], obstacles)

    const length = Math.sqrt(Math.pow(startX - points[0].x, 2) + Math.pow(startY - points[0].y, 2)) +
                   Math.sqrt(Math.pow(endX - points[0].x, 2) + Math.pow(endY - points[0].y, 2))

    return { points, collisions, length }
  }

  /**
   * 尝试多段折线路径
   */
  static tryMultiSegmentPath(startX, startY, endX, endY, obstacles, allNodes) {
    // 计算安全区域（避开所有节点）
    const safeX = this.findSafeX(startX, endX, allNodes)
    const safeY = (startY + endY) / 2

    const points = [
      { x: safeX, y: startY },
      { x: safeX, y: safeY },
      { x: endX, y: safeY }
    ]

    const collisions = this.countPathCollisions([
      { x: startX, y: startY },
      ...points,
      { x: endX, y: endY }
    ], obstacles)

    const length = Math.abs(safeX - startX) + Math.abs(safeY - startY) +
                   Math.abs(endX - safeX) + Math.abs(endY - safeY)

    return { points, collisions, length }
  }

  /**
   * 寻找安全X坐标（远离所有节点）
   */
  static findSafeX(startX, endX, allNodes) {
    const minX = Math.min(startX, endX)
    const maxX = Math.max(startX, endX)
    const midX = (startX + endX) / 2

    // 检查中间位置是否安全
    const candidates = [
      midX,
      midX - 150,
      midX + 150,
      minX - 100,
      maxX + 100
    ]

    for (const x of candidates) {
      const isSafe = allNodes.every(node => {
        const nodeX = node.x + (node.width || 120) / 2
        return Math.abs(x - nodeX) > 80
      })
      if (isSafe) return x
    }

    return midX // 降级方案
  }

  /**
   * 计算路径碰撞次数
   */
  static countPathCollisions(pathPoints, obstacles) {
    let collisions = 0

    for (let i = 0; i < pathPoints.length - 1; i++) {
      const from = pathPoints[i]
      const to = pathPoints[i + 1]

      for (const obs of obstacles) {
        if (this.segmentIntersectsNode(from, to, obs)) {
          collisions++
        }
      }
    }

    return collisions
  }

  /**
   * 线段是否穿透节点
   */
  static segmentIntersectsNode(from, to, node) {
    const padding = 15
    const left = node.x - padding
    const right = node.x + (node.width || 120) + padding
    const top = node.y - padding
    const bottom = node.y + (node.height || 50) + padding

    // 检查线段是否与矩形相交
    return this.lineIntersectsRect(from.x, from.y, to.x, to.y, left, right, top, bottom)
  }

  /**
   * 线段与矩形相交检测（简化版）
   */
  static lineIntersectsRect(x1, y1, x2, y2, left, right, top, bottom) {
    // 端点在矩形内
    if ((x1 >= left && x1 <= right && y1 >= top && y1 <= bottom) ||
        (x2 >= left && x2 <= right && y2 >= top && y2 <= bottom)) {
      return true
    }

    // 线段与矩形四条边相交检测
    return this.lineSegmentsIntersect(x1, y1, x2, y2, left, top, right, top) ||  // 上边
           this.lineSegmentsIntersect(x1, y1, x2, y2, right, top, right, bottom) || // 右边
           this.lineSegmentsIntersect(x1, y1, x2, y2, right, bottom, left, bottom) || // 下边
           this.lineSegmentsIntersect(x1, y1, x2, y2, left, bottom, left, top)  // 左边
  }

  /**
   * 两条线段是否相交
   */
  static lineSegmentsIntersect(x1, y1, x2, y2, x3, y3, x4, y4) {
    const denom = (y4 - y3) * (x2 - x1) - (x4 - x3) * (y2 - y1)
    if (denom === 0) return false

    const ua = ((x4 - x3) * (y1 - y3) - (y4 - y3) * (x1 - x3)) / denom
    const ub = ((x2 - x1) * (y1 - y3) - (y2 - y1) * (x1 - x3)) / denom

    return ua >= 0 && ua <= 1 && ub >= 0 && ub <= 1
  }

  /**
   * 创建简单绕行路径
   */
  static createSimpleBypass(source, target, obstacles) {
    const sourceX = source.x + (source.width || 120) / 2
    const sourceY = source.y + (source.height || 50)
    const targetX = target.x + (target.width || 120) / 2
    const targetY = target.y

    const midX = (sourceX + targetX) / 2
    const midY = (sourceY + targetY) / 2

    // 计算需要绕行的方向
    let offset = 80

    // 根据障碍物位置决定绕行方向
    if (obstacles.length > 0) {
      const firstObstacle = obstacles[0]
      const obsX = firstObstacle.x + (firstObstacle.width || 120) / 2

      if (obsX < midX) {
        // 障碍物在左侧，向右绕行
        offset = 100
      } else {
        // 障碍物在右侧，向左绕行
        offset = -100
      }
    }

    return [
      { x: midX + offset, y: midY }
    ]
  }

  /**
   * 点是否在矩形内
   */
  static pointInRect(x, y, rect) {
    const padding = 20
    return x >= rect.x - padding &&
           x <= rect.x + (rect.width || 120) + padding &&
           y >= rect.y - padding &&
           y <= rect.y + (rect.height || 50) + padding
  }
}

export default PathFixer
