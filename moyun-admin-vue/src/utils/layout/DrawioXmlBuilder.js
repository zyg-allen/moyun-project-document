/**
 * Draw.io XML 生成器
 * 将布局后的图数据转换为 Draw.io XML 格式
 */

import { StyleRegistry } from './StyleRegistry.js'

/**
 * XML 生成器类
 */
export class DrawioXmlBuilder {
  /**
   * 构建完整的 Draw.io XML
   * @param {Object} graph - 布局后的图数据
   * @returns {string} - Draw.io XML 字符串
   */
  static build(graph) {
    // 检查是否是分层展示模式
    if (graph.displayMode === 'layered') {
      return this.buildLayeredDisplay(graph)
    }

    const cells = []
    let idCounter = 2
    const idMap = new Map() // 原始ID -> 数字ID

    // 1. 先处理分组（作为父容器）
    const groupBoundsMap = new Map()
    if (graph.groups && graph.groups.length > 0) {
      for (const group of graph.groups) {
        const groupId = idCounter++
        idMap.set(group.id, groupId)

        // 使用 ELK 计算的分组位置，或者回退到自动计算
        let bounds
        if (group.x !== undefined && group.y !== undefined) {
          bounds = {
            x: Math.round(group.x),
            y: Math.round(group.y),
            width: Math.round(group.width || 200),
            height: Math.round(group.height || 100)
          }
        } else {
          bounds = this.calculateGroupBounds(group, graph.nodes)
        }
        groupBoundsMap.set(group.id, bounds)
        
        const style = StyleRegistry.buildGroupStyle(group)
        cells.push(this.buildGroupXml(groupId, group, bounds, style))
      }
    }

    // 2. 生成节点
    for (const node of graph.nodes) {
      const cellId = idCounter++
      idMap.set(node.id, cellId)

      // 查找父容器
      const parentGroupId = this.findParentGroupId(node.id, graph.groups)
      const parentId = parentGroupId ? (idMap.get(parentGroupId) || 1) : 1

      // 构建样式
      const style = StyleRegistry.buildNodeStyle(node)

      // 如果在分组内，坐标需要相对于分组
      let x = node.x
      let y = node.y
      if (parentGroupId && groupBoundsMap.has(parentGroupId)) {
        const parentBounds = groupBoundsMap.get(parentGroupId)
        x = node.x - parentBounds.x
        y = node.y - parentBounds.y
      }

      cells.push(this.buildNodeXml(cellId, node, x, y, style, parentId))
    }

    // 3. 生成边
    for (const edge of (graph.edges || [])) {
      const cellId = idCounter++
      const sourceId = idMap.get(edge.from)
      const targetId = idMap.get(edge.to)

      if (sourceId && targetId) {
        const style = StyleRegistry.buildEdgeStyle(
          edge,
          edge.exitX ?? 0.5,
          edge.exitY ?? 1,
          edge.entryX ?? 0.5,
          edge.entryY ?? 0
        )
        cells.push(this.buildEdgeXml(cellId, edge, sourceId, targetId, style))
      }
    }

    // 4. 组装完整 XML
    return this.wrapRoot(cells)
  }

  /**
   * 构建节点 XML
   */
  static buildNodeXml(id, node, x, y, style, parentId = 1) {
    const value = this.escapeXml(node.label || node.id)
    const width = node.width || 120
    const height = node.height || 50

    return `    <mxCell id="${id}" value="${value}" style="${style}" vertex="1" parent="${parentId}">
      <mxGeometry x="${x}" y="${y}" width="${width}" height="${height}" as="geometry"/>
    </mxCell>`
  }

  /**
   * 构建分组 XML
   */
  static buildGroupXml(id, group, bounds, style) {
    const value = this.escapeXml(group.label || group.id)
    return `    <mxCell id="${id}" value="${value}" style="${style}" vertex="1" connectable="0" parent="1">
      <mxGeometry x="${bounds.x}" y="${bounds.y}" width="${bounds.width}" height="${bounds.height}" as="geometry"/>
    </mxCell>`
  }

  /**
   * 构建边 XML
   * 使用 orthogonalEdgeStyle，让 Draw.io 自动计算正交路径
   * exitX/exitY/entryX/entryY 已在样式中指定连接点
   * 标签位置设置偏移避免遮挡
   */
  static buildEdgeXml(id, edge, sourceId, targetId, style) {
    const value = edge.label ? this.escapeXml(edge.label) : ''
    
    // 如果有标签，添加标签位置偏移（向上偏移避免遮挡线条）
    if (value) {
      return `    <mxCell id="${id}" value="${value}" style="${style}" edge="1" parent="1" source="${sourceId}" target="${targetId}">
      <mxGeometry relative="1" as="geometry">
        <mxPoint as="offset" y="-10"/>
      </mxGeometry>
    </mxCell>`
    }

    return `    <mxCell id="${id}" value="${value}" style="${style}" edge="1" parent="1" source="${sourceId}" target="${targetId}">
      <mxGeometry relative="1" as="geometry"/>
    </mxCell>`
  }

  /**
   * 计算分组边界
   */
  static calculateGroupBounds(group, nodes) {
    const containedNodes = nodes.filter(n => (group.contains || []).includes(n.id))

    if (containedNodes.length === 0) {
      return { x: 0, y: 0, width: 200, height: 100 }
    }

    const padding = 30
    const headerHeight = 40

    const minX = Math.min(...containedNodes.map(n => n.x)) - padding
    const minY = Math.min(...containedNodes.map(n => n.y)) - padding - headerHeight
    const maxX = Math.max(...containedNodes.map(n => n.x + (n.width || 120))) + padding
    const maxY = Math.max(...containedNodes.map(n => n.y + (n.height || 50))) + padding

    return {
      x: Math.round(minX),
      y: Math.round(minY),
      width: Math.round(maxX - minX),
      height: Math.round(maxY - minY)
    }
  }

  /**
   * 查找节点所属的父分组ID
   */
  static findParentGroupId(nodeId, groups) {
    if (!groups) return null

    for (const group of groups) {
      if ((group.contains || []).includes(nodeId)) {
        return group.id
      }
    }
    return null
  }

  /**
   * 包装为完整的 mxGraphModel
   */
  static wrapRoot(cells) {
    return `<mxGraphModel>
  <root>
    <mxCell id="0"/>
    <mxCell id="1" parent="0"/>
${cells.join('\n')}
  </root>
</mxGraphModel>`
  }

  /**
   * XML 转义
   */
  static escapeXml(text) {
    if (!text) return ''
    return String(text)
      .replace(/&/g, '&amp;')
      .replace(/</g, '&lt;')
      .replace(/>/g, '&gt;')
      .replace(/"/g, '&quot;')
      .replace(/'/g, '&apos;')
  }

  /**
   * 构建分层展示图（类似系统架构图）
   * 每层是一个带颜色的横条，节点在横条内排列
   */
  static buildLayeredDisplay(graph) {
    const cells = []
    let idCounter = 2
    const idMap = new Map()

    // 1. 为每一层创建背景矩形和左侧标签
    for (const group of (graph.groups || [])) {
      const color = group.layerColor || { fill: '#f5f5f5', stroke: '#cccccc' }
      
      // 左侧标签（更简洁的样式）
      if (group.labelX !== undefined) {
        const labelId = idCounter++
        const labelStyle = `rounded=0;whiteSpace=wrap;html=1;fillColor=${color.stroke};strokeColor=none;fontStyle=1;fontSize=11;fontColor=#FFFFFF;align=center;verticalAlign=middle;`
        cells.push(`    <mxCell id="${labelId}" value="${this.escapeXml(group.label || '')}" style="${labelStyle}" vertex="1" parent="1">
      <mxGeometry x="${group.labelX}" y="${group.labelY}" width="${group.labelWidth}" height="${group.labelHeight}" as="geometry"/>
    </mxCell>`)
      }

      // 层背景矩形（透明度更低，不抢眼）
      const layerId = idCounter++
      idMap.set(group.id, layerId)
      const layerStyle = `rounded=0;whiteSpace=wrap;html=1;fillColor=${color.fill};strokeColor=${color.stroke};strokeWidth=1;`
      cells.push(`    <mxCell id="${layerId}" value="" style="${layerStyle}" vertex="1" parent="1">
      <mxGeometry x="${group.x}" y="${group.y}" width="${group.width}" height="${group.height}" as="geometry"/>
    </mxCell>`)
    }

    // 2. 生成节点（简洁的矩形样式）
    for (const node of (graph.nodes || [])) {
      const cellId = idCounter++
      idMap.set(node.id, cellId)

      // 分层展示图使用简洁的白色矩形，支持自动换行
      const nodeColor = this.getNodeColor(node.color)
      const style = `rounded=1;whiteSpace=wrap;html=1;fillColor=#FFFFFF;strokeColor=${nodeColor};strokeWidth=1;fontSize=10;fontColor=#333333;shadow=0;verticalAlign=middle;`
      const value = this.escapeXml(node.label || node.id)
      const x = Math.round(node.x || 0)
      const y = Math.round(node.y || 0)
      const width = Math.round(node.width || 90)
      const height = Math.round(node.height || 36)

      cells.push(`    <mxCell id="${cellId}" value="${value}" style="${style}" vertex="1" parent="1">
      <mxGeometry x="${x}" y="${y}" width="${width}" height="${height}" as="geometry"/>
    </mxCell>`)
    }

    // 3. 不生成边（分层展示图不需要边）

    return this.wrapRoot(cells)
  }

  /**
   * 获取节点颜色
   */
  static getNodeColor(color) {
    const colors = {
      blue: '#4A90D9',
      green: '#5CB85C',
      orange: '#E89B3C',
      red: '#D9534F',
      purple: '#9B59B6',
      gray: '#95A5A6',
      yellow: '#F39C12'
    }
    return colors[color] || '#666666'
  }
}
