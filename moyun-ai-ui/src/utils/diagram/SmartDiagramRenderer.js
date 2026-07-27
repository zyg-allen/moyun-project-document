/**
 * 智能图表渲染器
 * 基于 Canvas + ELK 实现智能布局和边路由
 */

import ELK from 'elkjs/lib/elk.bundled.js'

// 节点样式配置
const NodeStyles = {
  default: { fill: '#fff', stroke: '#333', radius: 8 },
  server: { fill: '#dae8fc', stroke: '#6c8ebf', radius: 8 },
  service: { fill: '#dae8fc', stroke: '#6c8ebf', radius: 8 },
  database: { fill: '#f5f5f5', stroke: '#666', radius: 0, shape: 'cylinder' },
  cache: { fill: '#d5e8d4', stroke: '#82b366', radius: 0, shape: 'cylinder' },
  queue: { fill: '#fff2cc', stroke: '#d6b656', radius: 4 },
  gateway: { fill: '#ffe6cc', stroke: '#d79b00', radius: 8 },
  loadbalancer: { fill: '#ffe6cc', stroke: '#d79b00', radius: 8 },
  user: { fill: '#e1d5e7', stroke: '#9673a6', radius: 8 },
  actor: { fill: '#e1d5e7', stroke: '#9673a6', radius: 8 },
  cloud: { fill: '#f8cecc', stroke: '#b85450', radius: 20 },
  cdn: { fill: '#f8cecc', stroke: '#b85450', radius: 8 },
  decision: { fill: '#fff2cc', stroke: '#d6b656', radius: 4, shape: 'diamond' },
  start: { fill: '#d5e8d4', stroke: '#82b366', radius: 50, shape: 'ellipse' },
  end: { fill: '#f8cecc', stroke: '#b85450', radius: 50, shape: 'ellipse' },
  // AWS 图标
  aws_ec2: { fill: '#ff9900', stroke: '#cc7a00', radius: 8 },
  aws_rds: { fill: '#3b48cc', stroke: '#2d3a9e', radius: 8 },
  aws_s3: { fill: '#569a31', stroke: '#3d6b22', radius: 8 },
  aws_lambda: { fill: '#ff9900', stroke: '#cc7a00', radius: 8 },
  aws_api_gateway: { fill: '#ff4f8b', stroke: '#cc3f6f', radius: 8 }
}

// 边样式配置
const EdgeStyles = {
  solid: { stroke: '#333', width: 1, dash: null },
  dashed: { stroke: '#666', width: 1, dash: [5, 3] },
  dotted: { stroke: '#999', width: 1, dash: [2, 2] }
}

/**
 * 智能图表渲染器类
 */
export class SmartDiagramRenderer {
  constructor(container, options = {}) {
    this.container = container
    this.options = {
      theme: 'light',
      padding: 50,
      nodeWidth: 140,
      nodeHeight: 50,
      ...options
    }
    
    this.elk = new ELK()
    this.canvas = null
    this.ctx = null
    this.nodes = []
    this.edges = []
    this.groups = []
    this.scale = 1
    this.offset = { x: 0, y: 0 }
    this.isDragging = false
    this.dragStart = { x: 0, y: 0 }
    this.dpr = 1
    
    // 绑定方法以便正确移除事件监听
    this._handleResize = this.resizeCanvas.bind(this)
    this._handleWheel = this._onWheel.bind(this)
    this._handleMouseDown = this._onMouseDown.bind(this)
    this._handleMouseMove = this._onMouseMove.bind(this)
    this._handleMouseUp = this._onMouseUp.bind(this)
    this._handleClick = this._onClick.bind(this)
    
    this.init()
  }

  init() {
    // 创建 Canvas
    this.canvas = document.createElement('canvas')
    this.canvas.style.width = '100%'
    this.canvas.style.height = '100%'
    this.canvas.style.cursor = 'grab'
    this.container.appendChild(this.canvas)
    
    this.ctx = this.canvas.getContext('2d')
    
    // 设置画布尺寸
    this.resizeCanvas()
    window.addEventListener('resize', this._handleResize)
    
    // 添加交互事件
    this.canvas.addEventListener('wheel', this._handleWheel, { passive: false })
    this.canvas.addEventListener('mousedown', this._handleMouseDown)
    this.canvas.addEventListener('mousemove', this._handleMouseMove)
    this.canvas.addEventListener('mouseup', this._handleMouseUp)
    this.canvas.addEventListener('mouseleave', this._handleMouseUp)
    this.canvas.addEventListener('click', this._handleClick)
  }

  // 事件处理方法
  _onWheel(e) {
    e.preventDefault()
    const delta = e.deltaY > 0 ? 0.9 : 1.1
    this.scale = Math.max(0.1, Math.min(3, this.scale * delta))
    this.draw()
  }

  _onMouseDown(e) {
    this.isDragging = true
    this.canvas.style.cursor = 'grabbing'
    this.dragStart = { x: e.clientX - this.offset.x, y: e.clientY - this.offset.y }
  }

  _onMouseMove(e) {
    if (this.isDragging) {
      this.offset.x = e.clientX - this.dragStart.x
      this.offset.y = e.clientY - this.dragStart.y
      this.draw()
    }
  }

  _onMouseUp() {
    this.isDragging = false
    this.canvas.style.cursor = 'grab'
  }

  _onClick(e) {
    if (this.isDragging) return
    
    const rect = this.canvas.getBoundingClientRect()
    const x = (e.clientX - rect.left - this.offset.x) / this.scale
    const y = (e.clientY - rect.top - this.offset.y) / this.scale
    
    const clickedNode = this.findNodeAt(x, y)
    if (clickedNode && this.options.onNodeClick) {
      this.options.onNodeClick(clickedNode)
    }
  }

  resizeCanvas() {
    const rect = this.container.getBoundingClientRect()
    if (rect.width === 0 || rect.height === 0) return
    
    const dpr = window.devicePixelRatio || 1
    
    this.canvas.width = rect.width * dpr
    this.canvas.height = rect.height * dpr
    this.canvas.style.width = rect.width + 'px'
    this.canvas.style.height = rect.height + 'px'
    
    // 重置变换并设置 DPR 缩放
    this.ctx.setTransform(dpr, 0, 0, dpr, 0, 0)
    this.dpr = dpr
    this.draw()
  }

  findNodeAt(x, y) {
    for (const node of this.nodes) {
      if (x >= node.x && x <= node.x + node.width &&
          y >= node.y && y <= node.y + node.height) {
        return node
      }
    }
    return null
  }

  /**
   * 渲染图表
   */
  async render(graphData) {
    if (!graphData || !graphData.nodes || graphData.nodes.length === 0) {
      console.warn('[SmartDiagramRenderer] 无效的图数据')
      return { nodes: [], edges: [], groups: [] }
    }
    
    console.log('[SmartDiagramRenderer] 开始渲染, 节点数:', graphData.nodes.length)
    
    try {
      // 1. 使用 ELK 计算布局
      const layoutResult = await this.calculateLayout(graphData)
      
      // 2. 保存布局结果
      this.nodes = layoutResult.nodes
      this.edges = layoutResult.edges
      this.groups = layoutResult.groups || []
      
      // 3. 自动适应视图
      this.fitView()
      
      // 4. 绘制
      this.draw()
      
      // 5. 回调
      if (this.options.onLayoutComplete) {
        this.options.onLayoutComplete(layoutResult)
      }
      
      return layoutResult
    } catch (error) {
      console.error('[SmartDiagramRenderer] 渲染失败:', error)
      throw error
    }
  }

  /**
   * 使用 ELK 计算布局
   */
  async calculateLayout(graphData) {
    const { nodes, edges, groups } = graphData
    
    // 构建 ELK 图结构
    const elkGraph = {
      id: 'root',
      layoutOptions: {
        'elk.algorithm': 'layered',
        'elk.direction': 'DOWN',
        'elk.layered.spacing.nodeNodeBetweenLayers': '80',
        'elk.spacing.nodeNode': '50',
        'elk.edgeRouting': 'ORTHOGONAL',
        'elk.layered.crossingMinimization.strategy': 'LAYER_SWEEP',
        'elk.layered.nodePlacement.strategy': 'NETWORK_SIMPLEX',
        'elk.portConstraints': 'FIXED_SIDE',
        'elk.separateConnectedComponents': 'true'
      },
      children: [],
      edges: []
    }

    // 处理分组
    const groupMap = new Map()
    if (groups && groups.length > 0) {
      for (const group of groups) {
        const groupNode = {
          id: group.id,
          labels: [{ text: group.label || group.id }],
          layoutOptions: {
            'elk.algorithm': 'layered',
            'elk.direction': 'DOWN',
            'elk.padding': '[top=40,left=20,bottom=20,right=20]',
            'elk.spacing.nodeNode': '30'
          },
          children: []
        }
        groupMap.set(group.id, { groupNode, contains: group.contains || [] })
        elkGraph.children.push(groupNode)
      }
    }

    // 处理节点
    const nodeMap = new Map()
    for (const node of nodes) {
      const width = node.width || this.calculateNodeWidth(node.label)
      const height = node.height || this.options.nodeHeight
      
      const elkNode = {
        id: node.id,
        labels: [{ text: node.label || node.id }],
        width,
        height
      }
      
      nodeMap.set(node.id, { ...node, width, height })
      
      // 检查是否属于某个分组
      let addedToGroup = false
      for (const [groupId, groupInfo] of groupMap) {
        if (groupInfo.contains.includes(node.id)) {
          groupInfo.groupNode.children.push(elkNode)
          addedToGroup = true
          break
        }
      }
      
      if (!addedToGroup) {
        elkGraph.children.push(elkNode)
      }
    }

    // 处理边
    for (let i = 0; i < (edges || []).length; i++) {
      const edge = edges[i]
      elkGraph.edges.push({
        id: `edge_${i}`,
        sources: [edge.from],
        targets: [edge.to],
        labels: edge.label ? [{ text: edge.label }] : undefined
      })
    }

    // 执行 ELK 布局
    const layoutedGraph = await this.elk.layout(elkGraph)
    
    // 提取布局结果
    return this.extractLayoutResult(layoutedGraph, graphData, nodeMap)
  }

  /**
   * 提取 ELK 布局结果
   */
  extractLayoutResult(elkResult, original, nodeMap) {
    const nodes = []
    const edges = []
    const groups = []
    
    // 递归提取节点
    const extractNodes = (elkNode, offsetX = 0, offsetY = 0) => {
      if (elkNode.children) {
        for (const child of elkNode.children) {
          const x = (child.x || 0) + offsetX
          const y = (child.y || 0) + offsetY
          
          // 检查是否是分组
          if (child.children && child.children.length > 0) {
            groups.push({
              id: child.id,
              label: child.labels?.[0]?.text || child.id,
              x,
              y,
              width: child.width || 200,
              height: child.height || 100
            })
            // 递归提取分组内的节点
            extractNodes(child, x, y)
          } else {
            // 普通节点
            const originalNode = nodeMap.get(child.id)
            if (originalNode) {
              nodes.push({
                ...originalNode,
                x,
                y,
                width: child.width || originalNode.width,
                height: child.height || originalNode.height
              })
            }
          }
        }
      }
    }
    
    extractNodes(elkResult)
    
    // 创建节点位置映射
    const nodePositions = new Map()
    for (const node of nodes) {
      nodePositions.set(node.id, node)
    }

    // 提取边的路径点
    for (let i = 0; i < (original.edges || []).length; i++) {
      const originalEdge = original.edges[i]
      const elkEdge = elkResult.edges?.find(e => e.id === `edge_${i}`)
      
      let points = []
      if (elkEdge?.sections) {
        for (const section of elkEdge.sections) {
          if (section.startPoint) points.push(section.startPoint)
          if (section.bendPoints) points.push(...section.bendPoints)
          if (section.endPoint) points.push(section.endPoint)
        }
      }
      
      // 如果 ELK 没有返回路径点，手动计算
      if (points.length < 2) {
        const sourceNode = nodePositions.get(originalEdge.from)
        const targetNode = nodePositions.get(originalEdge.to)
        
        if (sourceNode && targetNode) {
          points = this.calculateEdgePath(sourceNode, targetNode)
        }
      }
      
      edges.push({
        ...originalEdge,
        points
      })
    }
    
    return { nodes, edges, groups }
  }

  /**
   * 计算边的路径（当 ELK 没有返回路径时使用）
   */
  calculateEdgePath(sourceNode, targetNode) {
    const sourceCenter = {
      x: sourceNode.x + sourceNode.width / 2,
      y: sourceNode.y + sourceNode.height / 2
    }
    const targetCenter = {
      x: targetNode.x + targetNode.width / 2,
      y: targetNode.y + targetNode.height / 2
    }
    
    const dx = targetCenter.x - sourceCenter.x
    const dy = targetCenter.y - sourceCenter.y
    
    // 计算出口和入口点
    let startPoint, endPoint
    
    if (Math.abs(dy) > Math.abs(dx)) {
      // 主要是垂直方向
      if (dy > 0) {
        startPoint = { x: sourceNode.x + sourceNode.width / 2, y: sourceNode.y + sourceNode.height }
        endPoint = { x: targetNode.x + targetNode.width / 2, y: targetNode.y }
      } else {
        startPoint = { x: sourceNode.x + sourceNode.width / 2, y: sourceNode.y }
        endPoint = { x: targetNode.x + targetNode.width / 2, y: targetNode.y + targetNode.height }
      }
    } else {
      // 主要是水平方向
      if (dx > 0) {
        startPoint = { x: sourceNode.x + sourceNode.width, y: sourceNode.y + sourceNode.height / 2 }
        endPoint = { x: targetNode.x, y: targetNode.y + targetNode.height / 2 }
      } else {
        startPoint = { x: sourceNode.x, y: sourceNode.y + sourceNode.height / 2 }
        endPoint = { x: targetNode.x + targetNode.width, y: targetNode.y + targetNode.height / 2 }
      }
    }
    
    // 添加正交拐点
    const points = [startPoint]
    
    if (Math.abs(startPoint.x - endPoint.x) > 10 && Math.abs(startPoint.y - endPoint.y) > 10) {
      const midY = (startPoint.y + endPoint.y) / 2
      points.push({ x: startPoint.x, y: midY })
      points.push({ x: endPoint.x, y: midY })
    }
    
    points.push(endPoint)
    return points
  }

  /**
   * 根据文字计算节点宽度
   */
  calculateNodeWidth(label) {
    if (!label) return this.options.nodeWidth
    
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
    
    // 🔥 增加最大宽度限制到 280px，避免长文字被截断
    return Math.max(this.options.nodeWidth, Math.min(width + 40, 280))
  }

  /**
   * 绘制图表
   */
  draw() {
    if (!this.ctx) return
    
    const { width, height } = this.canvas
    const dpr = window.devicePixelRatio || 1
    
    // 清空画布
    this.ctx.clearRect(0, 0, width / dpr, height / dpr)
    
    // 保存状态
    this.ctx.save()
    
    // 应用变换
    this.ctx.translate(this.offset.x, this.offset.y)
    this.ctx.scale(this.scale, this.scale)
    
    // 绘制分组
    for (const group of this.groups) {
      this.drawGroup(group)
    }
    
    // 绘制边
    for (const edge of this.edges) {
      this.drawEdge(edge)
    }
    
    // 绘制节点
    for (const node of this.nodes) {
      this.drawNode(node)
    }
    
    // 恢复状态
    this.ctx.restore()
  }

  /**
   * 绘制分组
   */
  drawGroup(group) {
    const ctx = this.ctx
    const { x, y, width, height, label } = group
    
    ctx.save()
    
    // 绘制背景
    ctx.fillStyle = 'rgba(240, 240, 240, 0.5)'
    ctx.strokeStyle = '#ccc'
    ctx.lineWidth = 1
    ctx.setLineDash([5, 3])
    
    this.roundRect(x, y, width, height, 8)
    ctx.fill()
    ctx.stroke()
    
    // 绘制标签
    ctx.fillStyle = '#666'
    ctx.font = 'bold 12px Arial'
    ctx.textAlign = 'left'
    ctx.textBaseline = 'top'
    ctx.fillText(label, x + 10, y + 10)
    
    ctx.restore()
  }

  /**
   * 绘制节点
   */
  drawNode(node) {
    const ctx = this.ctx
    const { x, y, width, height, label, icon } = node
    
    const style = NodeStyles[icon] || NodeStyles.default
    
    ctx.save()
    
    // 绘制阴影
    ctx.shadowColor = 'rgba(0, 0, 0, 0.1)'
    ctx.shadowBlur = 8
    ctx.shadowOffsetX = 2
    ctx.shadowOffsetY = 2
    
    // 绘制节点形状
    ctx.fillStyle = style.fill
    ctx.strokeStyle = style.stroke
    ctx.lineWidth = 1.5
    
    if (style.shape === 'cylinder') {
      this.drawCylinder(x, y, width, height)
    } else if (style.shape === 'ellipse') {
      this.drawEllipse(x, y, width, height)
    } else if (style.shape === 'diamond') {
      this.drawDiamond(x, y, width, height)
    } else {
      this.roundRect(x, y, width, height, style.radius)
      ctx.fill()
      ctx.stroke()
    }
    
    // 重置阴影
    ctx.shadowColor = 'transparent'
    
    // 绘制文字
    ctx.fillStyle = '#333'
    ctx.font = '12px Arial'
    ctx.textAlign = 'center'
    ctx.textBaseline = 'middle'
    ctx.fillText(label || node.id, x + width / 2, y + height / 2)
    
    ctx.restore()
  }

  /**
   * 绘制边
   */
  drawEdge(edge) {
    const ctx = this.ctx
    const { points, label, type } = edge
    
    if (!points || points.length < 2) return
    
    const style = EdgeStyles[type] || EdgeStyles.solid
    
    ctx.save()
    
    ctx.strokeStyle = style.stroke
    ctx.lineWidth = style.width
    if (style.dash) {
      ctx.setLineDash(style.dash)
    }
    
    // 绘制路径
    ctx.beginPath()
    ctx.moveTo(points[0].x, points[0].y)
    
    for (let i = 1; i < points.length; i++) {
      ctx.lineTo(points[i].x, points[i].y)
    }
    
    ctx.stroke()
    
    // 绘制箭头
    const lastPoint = points[points.length - 1]
    const prevPoint = points[points.length - 2]
    this.drawArrow(prevPoint.x, prevPoint.y, lastPoint.x, lastPoint.y)
    
    // 绘制标签
    if (label) {
      const midIndex = Math.floor(points.length / 2)
      const midPoint = points[midIndex]
      
      // 计算标签宽度
      ctx.font = '10px Arial'
      const textWidth = ctx.measureText(label).width
      const padding = 6
      const bgWidth = textWidth + padding * 2
      const bgHeight = 16
      
      // 绘制背景
      ctx.fillStyle = 'rgba(255, 255, 255, 0.9)'
      ctx.strokeStyle = '#ddd'
      ctx.lineWidth = 1
      ctx.setLineDash([])
      
      const bgX = midPoint.x - bgWidth / 2
      const bgY = midPoint.y - bgHeight / 2
      ctx.fillRect(bgX, bgY, bgWidth, bgHeight)
      ctx.strokeRect(bgX, bgY, bgWidth, bgHeight)
      
      // 绘制文字
      ctx.fillStyle = '#666'
      ctx.textAlign = 'center'
      ctx.textBaseline = 'middle'
      ctx.fillText(label, midPoint.x, midPoint.y)
    }
    
    ctx.restore()
  }

  /**
   * 绘制箭头
   */
  drawArrow(fromX, fromY, toX, toY) {
    const ctx = this.ctx
    const headLength = 10
    const angle = Math.atan2(toY - fromY, toX - fromX)
    
    ctx.save()
    ctx.fillStyle = ctx.strokeStyle
    
    ctx.beginPath()
    ctx.moveTo(toX, toY)
    ctx.lineTo(
      toX - headLength * Math.cos(angle - Math.PI / 6),
      toY - headLength * Math.sin(angle - Math.PI / 6)
    )
    ctx.lineTo(
      toX - headLength * Math.cos(angle + Math.PI / 6),
      toY - headLength * Math.sin(angle + Math.PI / 6)
    )
    ctx.closePath()
    ctx.fill()
    
    ctx.restore()
  }

  /**
   * 绘制圆角矩形
   */
  roundRect(x, y, width, height, radius) {
    const ctx = this.ctx
    ctx.beginPath()
    ctx.moveTo(x + radius, y)
    ctx.lineTo(x + width - radius, y)
    ctx.quadraticCurveTo(x + width, y, x + width, y + radius)
    ctx.lineTo(x + width, y + height - radius)
    ctx.quadraticCurveTo(x + width, y + height, x + width - radius, y + height)
    ctx.lineTo(x + radius, y + height)
    ctx.quadraticCurveTo(x, y + height, x, y + height - radius)
    ctx.lineTo(x, y + radius)
    ctx.quadraticCurveTo(x, y, x + radius, y)
    ctx.closePath()
  }

  /**
   * 绘制圆柱体（数据库）
   */
  drawCylinder(x, y, width, height) {
    const ctx = this.ctx
    const ellipseHeight = 15
    
    // 顶部椭圆
    ctx.beginPath()
    ctx.ellipse(x + width / 2, y + ellipseHeight, width / 2, ellipseHeight, 0, 0, Math.PI * 2)
    ctx.fill()
    ctx.stroke()
    
    // 底部椭圆
    ctx.beginPath()
    ctx.ellipse(x + width / 2, y + height - ellipseHeight, width / 2, ellipseHeight, 0, 0, Math.PI)
    ctx.stroke()
    
    // 侧面
    ctx.beginPath()
    ctx.moveTo(x, y + ellipseHeight)
    ctx.lineTo(x, y + height - ellipseHeight)
    ctx.moveTo(x + width, y + ellipseHeight)
    ctx.lineTo(x + width, y + height - ellipseHeight)
    ctx.stroke()
    
    // 填充侧面
    ctx.fillRect(x, y + ellipseHeight, width, height - ellipseHeight * 2)
  }

  /**
   * 绘制椭圆
   */
  drawEllipse(x, y, width, height) {
    const ctx = this.ctx
    ctx.beginPath()
    ctx.ellipse(x + width / 2, y + height / 2, width / 2, height / 2, 0, 0, Math.PI * 2)
    ctx.fill()
    ctx.stroke()
  }

  /**
   * 绘制菱形
   */
  drawDiamond(x, y, width, height) {
    const ctx = this.ctx
    const centerX = x + width / 2
    const centerY = y + height / 2
    
    ctx.beginPath()
    ctx.moveTo(centerX, y)
    ctx.lineTo(x + width, centerY)
    ctx.lineTo(centerX, y + height)
    ctx.lineTo(x, centerY)
    ctx.closePath()
    ctx.fill()
    ctx.stroke()
  }

  /**
   * 适应视图
   */
  fitView() {
    if (this.nodes.length === 0) return
    
    // 计算边界
    let minX = Infinity, minY = Infinity, maxX = -Infinity, maxY = -Infinity
    
    for (const node of this.nodes) {
      minX = Math.min(minX, node.x)
      minY = Math.min(minY, node.y)
      maxX = Math.max(maxX, node.x + node.width)
      maxY = Math.max(maxY, node.y + node.height)
    }
    
    for (const group of this.groups) {
      minX = Math.min(minX, group.x)
      minY = Math.min(minY, group.y)
      maxX = Math.max(maxX, group.x + group.width)
      maxY = Math.max(maxY, group.y + group.height)
    }
    
    const graphWidth = maxX - minX + this.options.padding * 2
    const graphHeight = maxY - minY + this.options.padding * 2
    
    const rect = this.container.getBoundingClientRect()
    const scaleX = rect.width / graphWidth
    const scaleY = rect.height / graphHeight
    
    this.scale = Math.min(scaleX, scaleY, 1)
    this.offset.x = (rect.width - graphWidth * this.scale) / 2 - minX * this.scale + this.options.padding * this.scale
    this.offset.y = (rect.height - graphHeight * this.scale) / 2 - minY * this.scale + this.options.padding * this.scale
    
    this.draw()
  }

  zoomIn() {
    this.scale = Math.min(3, this.scale * 1.2)
    this.draw()
  }

  zoomOut() {
    this.scale = Math.max(0.1, this.scale * 0.8)
    this.draw()
  }

  /**
   * 导出图片
   */
  async exportImage() {
    return this.canvas.toDataURL('image/png')
  }

  /**
   * 销毁 - 清理所有事件监听器和资源
   */
  destroy() {
    // 移除窗口事件
    window.removeEventListener('resize', this._handleResize)
    
    // 移除 Canvas 事件
    if (this.canvas) {
      this.canvas.removeEventListener('wheel', this._handleWheel)
      this.canvas.removeEventListener('mousedown', this._handleMouseDown)
      this.canvas.removeEventListener('mousemove', this._handleMouseMove)
      this.canvas.removeEventListener('mouseup', this._handleMouseUp)
      this.canvas.removeEventListener('mouseleave', this._handleMouseUp)
      this.canvas.removeEventListener('click', this._handleClick)
      this.canvas.remove()
    }
    
    // 清理引用
    this.canvas = null
    this.ctx = null
    this.nodes = []
    this.edges = []
    this.groups = []
  }
}
