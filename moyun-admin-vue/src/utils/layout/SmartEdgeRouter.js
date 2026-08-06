/**
 * 智能边路由器
 * 自动计算避障路径，避免边穿透节点和分组
 */

/**
 * 计算两点之间的正交路径，避开障碍物
 */
export class SmartEdgeRouter {
  /**
   * 计算所有边的智能路径
   * @param {Object} graph - 布局后的图数据（包含节点坐标）
   * @returns {Object} - 包含路径点的图数据
   */
  static routeEdges(graph) {
    const { nodes, edges, groups } = graph
    
    // 如果没有边，直接返回
    if (!edges || edges.length === 0) {
      return graph
    }
    
    // 构建障碍物列表（节点 + 分组边界）
    const obstacles = this.buildObstacles(nodes, groups || [])
    
    // 为每条边计算路径（仅处理没有路径点的边，保留 ELK 已计算的路径）
    const routedEdges = edges.map(edge => {
      // 🔥 如果 ELK 已经计算了路径点，保留它们
      if (edge.points && edge.points.length > 0) {
        console.log(`[SmartEdgeRouter] 保留 ELK 路径: ${edge.from} → ${edge.to}, ${edge.points.length} 个点`)
        return edge
      }
      
      const sourceNode = nodes.find(n => n.id === edge.from)
      const targetNode = nodes.find(n => n.id === edge.to)
      
      if (!sourceNode || !targetNode) return edge
      
      // 计算最佳连接点和路径（仅用于 ELK 没有计算路径的边）
      const route = this.calculateRoute(sourceNode, targetNode, obstacles, nodes)
      
      return {
        ...edge,
        points: route.points,
        exitX: route.exitX,
        exitY: route.exitY,
        entryX: route.entryX,
        entryY: route.entryY
      }
    })
    
    return {
      ...graph,
      edges: routedEdges
    }
  }

  /**
   * 构建障碍物列表
   */
  static buildObstacles(nodes, groups) {
    const obstacles = []
    
    // 节点作为障碍物
    for (const node of nodes) {
      obstacles.push({
        id: node.id,
        type: 'node',
        x: node.x,
        y: node.y,
        width: node.width || 120,
        height: node.height || 50,
        padding: 10  // 边与节点的最小距离
      })
    }
    
    // 分组边界作为障碍物（只对分组外的边有效）
    if (groups) {
      for (const group of groups) {
        const containedNodes = nodes.filter(n => (group.contains || []).includes(n.id))
        if (containedNodes.length > 0) {
          const bounds = this.calculateBounds(containedNodes)
          obstacles.push({
            id: group.id,
            type: 'group',
            x: bounds.x - 30,
            y: bounds.y - 50,
            width: bounds.width + 60,
            height: bounds.height + 80,
            padding: 5,
            contains: group.contains
          })
        }
      }
    }
    
    return obstacles
  }

  /**
   * 计算边界
   */
  static calculateBounds(nodes) {
    const xs = nodes.map(n => n.x)
    const ys = nodes.map(n => n.y)
    const rights = nodes.map(n => n.x + (n.width || 120))
    const bottoms = nodes.map(n => n.y + (n.height || 50))
    
    return {
      x: Math.min(...xs),
      y: Math.min(...ys),
      width: Math.max(...rights) - Math.min(...xs),
      height: Math.max(...bottoms) - Math.min(...ys)
    }
  }

  /**
   * 计算两个节点之间的最佳路由
   */
  static calculateRoute(source, target, obstacles, allNodes) {
    const sourceCenter = {
      x: source.x + (source.width || 120) / 2,
      y: source.y + (source.height || 50) / 2
    }
    const targetCenter = {
      x: target.x + (target.width || 120) / 2,
      y: target.y + (target.height || 50) / 2
    }
    
    // 判断相对位置
    const dx = targetCenter.x - sourceCenter.x
    const dy = targetCenter.y - sourceCenter.y
    
    // 计算最佳出口和入口
    let exitX, exitY, entryX, entryY
    let exitPoint, entryPoint
    
    // 主要是垂直方向
    if (Math.abs(dy) > Math.abs(dx)) {
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
    
    // 计算出口和入口的实际坐标
    exitPoint = {
      x: source.x + (source.width || 120) * exitX,
      y: source.y + (source.height || 50) * exitY
    }
    entryPoint = {
      x: target.x + (target.width || 120) * entryX,
      y: target.y + (target.height || 50) * entryY
    }
    
    // 计算中间路径点（避障）
    const bendPoints = this.calculateBendPoints(
      exitPoint, entryPoint, source, target, obstacles
    )
    
    // 组装完整路径
    const points = [exitPoint, ...bendPoints, entryPoint]
    
    return { points, exitX, exitY, entryX, entryY }
  }

  /**
   * 计算弯折点（避障）
   */
  static calculateBendPoints(start, end, sourceNode, targetNode, obstacles) {
    const points = []
    
    // 检查直线是否穿过障碍物
    const blockers = this.findBlockingObstacles(start, end, sourceNode, targetNode, obstacles)
    
    if (blockers.length === 0) {
      // 没有障碍物，使用简单的正交路径
      if (Math.abs(end.x - start.x) > 10 && Math.abs(end.y - start.y) > 10) {
        // 需要拐弯
        const midY = (start.y + end.y) / 2
        points.push({ x: start.x, y: midY })
        points.push({ x: end.x, y: midY })
      }
    } else {
      // 有障碍物，计算绕行路径
      const route = this.calculateAvoidanceRoute(start, end, blockers, obstacles)
      points.push(...route)
    }
    
    return points
  }

  /**
   * 查找阻挡的障碍物
   */
  static findBlockingObstacles(start, end, sourceNode, targetNode, obstacles) {
    const blockers = []
    
    for (const obs of obstacles) {
      // 跳过源和目标节点
      if (obs.id === sourceNode.id || obs.id === targetNode.id) continue
      
      // 如果是分组，检查源和目标是否都在该分组内
      if (obs.type === 'group' && obs.contains) {
        const sourceInGroup = obs.contains.includes(sourceNode.id)
        const targetInGroup = obs.contains.includes(targetNode.id)
        // 如果都在分组内，跳过该分组
        if (sourceInGroup && targetInGroup) continue
        // 如果只有一个在分组内，这是跨分组的边，分组边界是障碍物
      }
      
      // 检查线段是否与障碍物相交
      if (this.lineIntersectsRect(start, end, obs)) {
        blockers.push(obs)
      }
    }
    
    return blockers
  }

  /**
   * 检查线段是否与矩形相交
   */
  static lineIntersectsRect(p1, p2, rect) {
    const left = rect.x - rect.padding
    const right = rect.x + rect.width + rect.padding
    const top = rect.y - rect.padding
    const bottom = rect.y + rect.height + rect.padding
    
    // 简化检查：检查线段是否穿过矩形区域
    const minX = Math.min(p1.x, p2.x)
    const maxX = Math.max(p1.x, p2.x)
    const minY = Math.min(p1.y, p2.y)
    const maxY = Math.max(p1.y, p2.y)
    
    // 如果线段完全在矩形外，不相交
    if (maxX < left || minX > right || maxY < top || minY > bottom) {
      return false
    }
    
    // 检查线段是否穿过矩形内部
    // 这里使用简化的检查
    const centerX = rect.x + rect.width / 2
    const centerY = rect.y + rect.height / 2
    
    // 计算线段到矩形中心的距离
    const dist = this.pointToLineDistance(centerX, centerY, p1.x, p1.y, p2.x, p2.y)
    const threshold = Math.min(rect.width, rect.height) / 2
    
    return dist < threshold
  }

  /**
   * 点到线段的距离
   */
  static pointToLineDistance(px, py, x1, y1, x2, y2) {
    const dx = x2 - x1
    const dy = y2 - y1
    const len = Math.sqrt(dx * dx + dy * dy)
    
    if (len === 0) return Math.sqrt((px - x1) ** 2 + (py - y1) ** 2)
    
    const t = Math.max(0, Math.min(1, ((px - x1) * dx + (py - y1) * dy) / (len * len)))
    const nearestX = x1 + t * dx
    const nearestY = y1 + t * dy
    
    return Math.sqrt((px - nearestX) ** 2 + (py - nearestY) ** 2)
  }

  /**
   * 计算绕行路径（只返回中间的弯折点，不包含起点和终点）
   */
  static calculateAvoidanceRoute(start, end, blockers, allObstacles) {
    const points = []
    
    // 找到主要障碍物
    const mainBlocker = blockers[0]
    
    // 决定绕行方向（上下或左右）
    const dx = end.x - start.x
    const dy = end.y - start.y
    
    // 计算障碍物的边界
    const obsLeft = mainBlocker.x - mainBlocker.padding - 15
    const obsRight = mainBlocker.x + mainBlocker.width + mainBlocker.padding + 15
    const obsTop = mainBlocker.y - mainBlocker.padding - 15
    const obsBottom = mainBlocker.y + mainBlocker.height + mainBlocker.padding + 15
    
    if (Math.abs(dy) > Math.abs(dx)) {
      // 主要是垂直移动，需要水平绕行
      // 选择更短的绕行方向
      const distToLeft = Math.abs(start.x - obsLeft)
      const distToRight = Math.abs(start.x - obsRight)
      const offset = distToLeft < distToRight ? obsLeft : obsRight
      
      // 中间两个拐点
      points.push({ x: offset, y: start.y })
      points.push({ x: offset, y: end.y })
    } else {
      // 主要是水平移动，需要垂直绕行
      // 选择更短的绕行方向
      const distToTop = Math.abs(start.y - obsTop)
      const distToBottom = Math.abs(start.y - obsBottom)
      const offset = distToTop < distToBottom ? obsTop : obsBottom
      
      // 中间两个拐点
      points.push({ x: start.x, y: offset })
      points.push({ x: end.x, y: offset })
    }
    
    return points
  }
}

/**
 * 智能节点尺寸计算器
 * 根据文字内容动态计算节点尺寸
 */
export class SmartNodeSizer {
  // 字符宽度估算（中文约12px，英文约7px）
  static CHAR_WIDTH_CN = 12
  static CHAR_WIDTH_EN = 7
  static LINE_HEIGHT = 18
  static PADDING_H = 16
  static PADDING_V = 10
  static MIN_WIDTH = 70
  static MIN_HEIGHT = 36
  static MAX_WIDTH = 140

  /**
   * 根据文字内容计算节点尺寸
   */
  static calculateSize(label, icon = 'default') {
    if (!label) {
      return { width: this.MIN_WIDTH, height: this.MIN_HEIGHT }
    }

    // 计算文字宽度
    let textWidth = 0
    for (const char of label) {
      textWidth += this.isChinese(char) ? this.CHAR_WIDTH_CN : this.CHAR_WIDTH_EN
    }

    // 计算需要的宽度和高度
    let width = textWidth + this.PADDING_H * 2
    let height = this.LINE_HEIGHT + this.PADDING_V * 2

    // 如果太宽，考虑换行
    if (width > this.MAX_WIDTH) {
      const lines = Math.ceil(width / this.MAX_WIDTH)
      width = this.MAX_WIDTH
      height = this.LINE_HEIGHT * lines + this.PADDING_V * 2
    }

    // 应用最小值
    width = Math.max(width, this.MIN_WIDTH)
    height = Math.max(height, this.MIN_HEIGHT)

    // 特殊图标的固定尺寸（覆盖文字计算）
    if (icon === 'actor' || icon === 'user' || icon === 'client') {
      // 小人图标使用固定小尺寸
      return { width: 40, height: 50 }
    } else if (icon === 'database' || icon === 'cache') {
      width = Math.min(width, 80)
      height = Math.max(height, 60)
    } else if (icon === 'decision') {
      // 菱形
      width = Math.max(width, 80)
      height = Math.max(height, 50)
    } else if (icon === 'start' || icon === 'end') {
      // 圆形
      return { width: 50, height: 50 }
    }

    return { 
      width: Math.round(width), 
      height: Math.round(height) 
    }
  }

  /**
   * 判断是否是中文字符
   */
  static isChinese(char) {
    return /[\u4e00-\u9fa5]/.test(char)
  }

  /**
   * 为图中所有节点计算自适应尺寸
   */
  static enhanceNodes(nodes) {
    if (!nodes || nodes.length === 0) return nodes || []
    
    return nodes.map(node => {
      const size = this.calculateSize(node.label, node.icon)
      return {
        ...node,
        width: node.width || size.width,
        height: node.height || size.height
      }
    })
  }
}
