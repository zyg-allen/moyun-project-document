/**
 * 几何校验器
 * 用于检测图表中的几何问题：节点重叠、边穿透节点等
 */
export class GeometryValidator {
  /**
   * 验证图表几何合法性
   * @param {Object} graph - 图表数据 {nodes, edges}
   * @returns {Object} - {valid: boolean, errors: Array}
   */
  validate(graph) {
    const errors = []
    
    // 检查 1：节点坐标完整性
    const coordinateErrors = this.validateNodeCoordinates(graph.nodes || [])
    errors.push(...coordinateErrors)
    
    // 检查 2：节点重叠
    const overlapErrors = this.validateNodeOverlap(graph.nodes || [])
    errors.push(...overlapErrors)
    
    // 检查 3：边穿透节点
    const penetrationErrors = this.validateEdgePenetration(graph.edges || [], graph.nodes || [])
    errors.push(...penetrationErrors)
    
    // 检查 4：跨层直连
    const crossLayerErrors = this.validateCrossLayerConnections(graph.edges || [], graph.nodes || [])
    errors.push(...crossLayerErrors)
    
    return {
      valid: errors.length === 0,
      errors,
      summary: this.generateSummary(errors)
    }
  }
  
  /**
   * 验证节点坐标完整性
   */
  validateNodeCoordinates(nodes) {
    const errors = []
    
    nodes.forEach(node => {
      if (node.x === undefined || node.x === null || isNaN(node.x)) {
        errors.push({
          type: 'MISSING_COORDINATE',
          severity: 'ERROR',
          nodeId: node.id,
          message: `节点 ${node.id} 缺少有效的 x 坐标`
        })
      }
      
      if (node.y === undefined || node.y === null || isNaN(node.y)) {
        errors.push({
          type: 'MISSING_COORDINATE',
          severity: 'ERROR',
          nodeId: node.id,
          message: `节点 ${node.id} 缺少有效的 y 坐标`
        })
      }
      
      if (node.width === undefined || node.width <= 0) {
        errors.push({
          type: 'INVALID_SIZE',
          severity: 'WARNING',
          nodeId: node.id,
          message: `节点 ${node.id} 缺少有效的 width`
        })
      }
      
      if (node.height === undefined || node.height <= 0) {
        errors.push({
          type: 'INVALID_SIZE',
          severity: 'WARNING',
          nodeId: node.id,
          message: `节点 ${node.id} 缺少有效的 height`
        })
      }
    })
    
    return errors
  }
  
  /**
   * 验证节点重叠
   */
  validateNodeOverlap(nodes) {
    const errors = []
    const padding = 5 // 最小间距
    
    for (let i = 0; i < nodes.length; i++) {
      for (let j = i + 1; j < nodes.length; j++) {
        const node1 = nodes[i]
        const node2 = nodes[j]
        
        if (this.nodesOverlap(node1, node2, padding)) {
          errors.push({
            type: 'NODE_OVERLAP',
            severity: 'ERROR',
            nodeId: node1.id,
            targetNodeId: node2.id,
            message: `节点 ${node1.id} 与 ${node2.id} 重叠`
          })
        }
      }
    }
    
    return errors
  }
  
  /**
   * 检测两个节点是否重叠
   */
  nodesOverlap(node1, node2, padding = 0) {
    const left1 = node1.x - padding
    const right1 = node1.x + (node1.width || 120) + padding
    const top1 = node1.y - padding
    const bottom1 = node1.y + (node1.height || 50) + padding
    
    const left2 = node2.x - padding
    const right2 = node2.x + (node2.width || 120) + padding
    const top2 = node2.y - padding
    const bottom2 = node2.y + (node2.height || 50) + padding
    
    return !(right1 < left2 || left1 > right2 || bottom1 < top2 || top1 > bottom2)
  }
  
  /**
   * 验证边穿透节点
   */
  validateEdgePenetration(edges, nodes) {
    const errors = []
    
    edges.forEach(edge => {
      // 构建边的路径点列表
      const sourceNode = nodes.find(n => n.id === edge.from)
      const targetNode = nodes.find(n => n.id === edge.to)
      
      if (!sourceNode || !targetNode) return
      
      // 计算起点和终点坐标
      const sourcePoint = this.getEdgePoint(sourceNode, edge.exitX, edge.exitY)
      const targetPoint = this.getEdgePoint(targetNode, edge.entryX, edge.entryY)
      
      // 构建完整路径
      const pathPoints = [
        sourcePoint,
        ...(edge.points || []),
        targetPoint
      ]
      
      // 检查每一段是否穿透其他节点
      for (let i = 0; i < pathPoints.length - 1; i++) {
        const from = pathPoints[i]
        const to = pathPoints[i + 1]
        
        nodes.forEach(node => {
          // 跳过边的起点和终点节点
          if (node.id === edge.from || node.id === edge.to) return
          
          if (this.segmentIntersectsRect(from, to, node)) {
            errors.push({
              type: 'EDGE_PENETRATION',
              severity: 'ERROR',
              edgeId: edge.id || `${edge.from}-${edge.to}`,
              nodeId: node.id,
              message: `边 ${edge.from}→${edge.to} 穿透节点 ${node.id}`
            })
          }
        })
      }
    })
    
    return errors
  }
  
  /**
   * 计算边的连接点坐标
   */
  getEdgePoint(node, exitX = 0.5, exitY = 0.5) {
    return {
      x: node.x + (node.width || 120) * exitX,
      y: node.y + (node.height || 50) * exitY
    }
  }
  
  /**
   * 检测线段是否与矩形相交（Liang-Barsky 算法）
   */
  segmentIntersectsRect(from, to, rect) {
    const padding = 5
    const left = rect.x - padding
    const right = rect.x + (rect.width || 120) + padding
    const top = rect.y - padding
    const bottom = rect.y + (rect.height || 50) + padding
    
    // 如果线段端点在矩形内，认为相交
    if (this.pointInRect(from, left, right, top, bottom) ||
        this.pointInRect(to, left, right, top, bottom)) {
      return true
    }
    
    // Cohen-Sutherland 线段裁剪算法
    return this.cohenSutherlandIntersect(from.x, from.y, to.x, to.y, left, right, top, bottom)
  }
  
  /**
   * 点是否在矩形内
   */
  pointInRect(point, left, right, top, bottom) {
    return point.x >= left && point.x <= right && point.y >= top && point.y <= bottom
  }
  
  /**
   * Cohen-Sutherland 线段与矩形相交检测
   */
  cohenSutherlandIntersect(x1, y1, x2, y2, left, right, top, bottom) {
    const INSIDE = 0
    const LEFT = 1
    const RIGHT = 2
    const BOTTOM = 4
    const TOP = 8
    
    const computeCode = (x, y) => {
      let code = INSIDE
      if (x < left) code |= LEFT
      else if (x > right) code |= RIGHT
      if (y < top) code |= BOTTOM
      else if (y > bottom) code |= TOP
      return code
    }
    
    let code1 = computeCode(x1, y1)
    let code2 = computeCode(x2, y2)
    
    while (true) {
      if (!(code1 | code2)) {
        // 两点都在矩形内
        return true
      } else if (code1 & code2) {
        // 两点在矩形同一侧外
        return false
      } else {
        // 需要裁剪
        const codeOut = code1 ? code1 : code2
        let x, y
        
        if (codeOut & TOP) {
          x = x1 + (x2 - x1) * (bottom - y1) / (y2 - y1)
          y = bottom
        } else if (codeOut & BOTTOM) {
          x = x1 + (x2 - x1) * (top - y1) / (y2 - y1)
          y = top
        } else if (codeOut & RIGHT) {
          y = y1 + (y2 - y1) * (right - x1) / (x2 - x1)
          x = right
        } else {
          y = y1 + (y2 - y1) * (left - x1) / (x2 - x1)
          x = left
        }
        
        if (codeOut === code1) {
          x1 = x
          y1 = y
          code1 = computeCode(x1, y1)
        } else {
          x2 = x
          y2 = y
          code2 = computeCode(x2, y2)
        }
      }
    }
  }
  
  /**
   * 验证跨层直连
   */
  validateCrossLayerConnections(edges, nodes) {
    const errors = []
    
    edges.forEach(edge => {
      const sourceNode = nodes.find(n => n.id === edge.from)
      const targetNode = nodes.find(n => n.id === edge.to)
      
      if (!sourceNode || !targetNode) return
      
      const sourceLayer = sourceNode.layer || 0
      const targetLayer = targetNode.layer || 0
      
      // 检查是否跨层直连（跨越多于1层）
      if (Math.abs(targetLayer - sourceLayer) > 1) {
        errors.push({
          type: 'CROSS_LAYER_CONNECTION',
          severity: 'WARNING',
          edgeId: edge.id || `${edge.from}-${edge.to}`,
          message: `边 ${edge.from}(层${sourceLayer})→${edge.to}(层${targetLayer}) 跨层直连`
        })
      }
      
      // 检查反向连接（从高层连接到低层）
      if (targetLayer < sourceLayer) {
        errors.push({
          type: 'REVERSE_CONNECTION',
          severity: 'WARNING',
          edgeId: edge.id || `${edge.from}-${edge.to}`,
          message: `边 ${edge.from}(层${sourceLayer})→${edge.to}(层${targetLayer}) 反向连接`
        })
      }
    })
    
    return errors
  }
  
  /**
   * 生成验证摘要
   */
  generateSummary(errors) {
    const errorCount = errors.filter(e => e.severity === 'ERROR').length
    const warningCount = errors.filter(e => e.severity === 'WARNING').length
    
    return {
      total: errors.length,
      errors: errorCount,
      warnings: warningCount,
      byType: this.groupByType(errors)
    }
  }
  
  /**
   * 按类型分组错误
   */
  groupByType(errors) {
    const grouped = {}
    errors.forEach(error => {
      if (!grouped[error.type]) {
        grouped[error.type] = 0
      }
      grouped[error.type]++
    })
    return grouped
  }
}

export default new GeometryValidator()
