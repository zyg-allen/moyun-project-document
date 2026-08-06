/**
 * Schema 校验器
 * 确保 AI 生成的图数据 100% 合法，杜绝死链和非法数据
 */

/**
 * 校验结果
 */
export class ValidationResult {
  constructor() {
    this.valid = true
    this.errors = []
    this.warnings = []
    this.fixes = []
  }

  addError(message, context = {}) {
    this.valid = false
    this.errors.push({ message, ...context })
  }

  addWarning(message, context = {}) {
    this.warnings.push({ message, ...context })
  }

  addFix(message, context = {}) {
    this.fixes.push({ message, ...context })
  }
}

/**
 * 允许的枚举值
 */
const AllowedEnums = {
  // 图表类型
  diagramTypes: ['architecture', 'flowchart', 'sequence', 'swimlane', 'mindmap', 'aws'],
  
  // 节点图标
  nodeIcons: [
    'default', 'actor', 'database', 'cache', 'queue', 'server', 'cloud', 
    'document', 'decision', 'start', 'end',
    'aws_ec2', 'aws_s3', 'aws_lambda', 'aws_rds', 'aws_dynamodb', 
    'aws_sqs', 'aws_sns', 'aws_api_gateway', 'aws_cloudfront', 'aws_elb', 
    'aws_vpc', 'aws_elasticache', 'aws_ecs', 'aws_eks'
  ],
  
  // 颜色主题
  colors: ['blue', 'green', 'orange', 'red', 'purple', 'gray', 'yellow', 'teal', 'pink'],
  
  // 边类型
  edgeTypes: ['solid', 'dashed', 'dotted', 'animated', 'bidirectional'],
  
  // 分组样式
  groupStyles: ['container', 'swimlane', 'region', 'aws_vpc']
}

/**
 * Schema 校验器类
 */
export class SchemaValidator {
  /**
   * 完整校验图数据
   * @param {Object} graph - 图数据
   * @param {Object} options - 校验选项
   * @returns {ValidationResult}
   */
  static validate(graph, options = {}) {
    const result = new ValidationResult()
    const { autoFix = true, strict = false } = options

    // 1. 基础结构校验
    if (!graph) {
      result.addError('图数据不能为空')
      return result
    }

    if (!graph.nodes || !Array.isArray(graph.nodes)) {
      result.addError('nodes 必须是数组')
      return result
    }

    if (graph.nodes.length === 0) {
      result.addError('图表必须包含至少一个节点')
      return result
    }

    // 2. 类型校验
    if (graph.type && !AllowedEnums.diagramTypes.includes(graph.type)) {
      if (strict) {
        result.addError(`无效的图表类型: ${graph.type}`, { field: 'type', value: graph.type })
      } else {
        result.addWarning(`未知的图表类型 "${graph.type}"，将使用默认值 "architecture"`)
        if (autoFix) {
          graph.type = 'architecture'
          result.addFix('已将图表类型修正为 "architecture"')
        }
      }
    }

    // 3. 节点校验
    const nodeIds = new Set()
    const duplicateIds = []

    for (let i = 0; i < graph.nodes.length; i++) {
      const node = graph.nodes[i]
      
      // 3.1 ID 必须存在
      if (!node.id) {
        result.addError(`节点 #${i + 1} 缺少 id`, { nodeIndex: i })
        continue
      }

      // 3.2 ID 不能重复
      if (nodeIds.has(node.id)) {
        duplicateIds.push(node.id)
        result.addWarning(`节点 ID "${node.id}" 重复`, { nodeId: node.id })
      }
      nodeIds.add(node.id)

      // 3.3 标签校验
      if (!node.label) {
        result.addWarning(`节点 "${node.id}" 缺少 label，将使用 id 作为标签`)
        if (autoFix) {
          node.label = node.id
          result.addFix(`已为节点 "${node.id}" 添加默认标签`)
        }
      }

      // 3.4 图标校验
      if (node.icon && !AllowedEnums.nodeIcons.includes(node.icon)) {
        if (strict) {
          result.addError(`节点 "${node.id}" 使用了无效的图标: ${node.icon}`)
        } else {
          result.addWarning(`节点 "${node.id}" 的图标 "${node.icon}" 不在标准列表中`)
        }
      }

      // 3.5 颜色校验
      if (node.color && !AllowedEnums.colors.includes(node.color)) {
        result.addWarning(`节点 "${node.id}" 的颜色 "${node.color}" 不在标准列表中`)
      }

      // 3.6 层级校验
      if (node.layer !== undefined && (typeof node.layer !== 'number' || node.layer < 0)) {
        result.addWarning(`节点 "${node.id}" 的 layer 值无效: ${node.layer}`)
        if (autoFix) {
          delete node.layer
          result.addFix(`已移除节点 "${node.id}" 的无效 layer 值`)
        }
      }
    }

    // 4. 边校验
    if (graph.edges && Array.isArray(graph.edges)) {
      const invalidEdges = []
      const selfLoops = []

      for (let i = 0; i < graph.edges.length; i++) {
        const edge = graph.edges[i]

        // 4.1 from/to 必须存在
        if (!edge.from || !edge.to) {
          result.addError(`边 #${i + 1} 缺少 from 或 to`, { edgeIndex: i })
          invalidEdges.push(i)
          continue
        }

        // 4.2 from/to 必须引用存在的节点
        if (!nodeIds.has(edge.from)) {
          result.addError(`边 #${i + 1} 的 from "${edge.from}" 引用了不存在的节点`, {
            edgeIndex: i,
            invalidRef: edge.from
          })
          invalidEdges.push(i)
        }

        if (!nodeIds.has(edge.to)) {
          result.addError(`边 #${i + 1} 的 to "${edge.to}" 引用了不存在的节点`, {
            edgeIndex: i,
            invalidRef: edge.to
          })
          invalidEdges.push(i)
        }

        // 4.3 检测自环
        if (edge.from === edge.to) {
          selfLoops.push(i)
          result.addWarning(`边 #${i + 1} 是自环 (${edge.from} → ${edge.to})`)
        }

        // 4.4 边类型校验
        if (edge.type && !AllowedEnums.edgeTypes.includes(edge.type)) {
          result.addWarning(`边 #${i + 1} 的类型 "${edge.type}" 不在标准列表中`)
        }
      }

      // 自动修复：移除无效边
      if (autoFix && invalidEdges.length > 0) {
        graph.edges = graph.edges.filter((_, i) => !invalidEdges.includes(i))
        result.addFix(`已移除 ${invalidEdges.length} 条无效边`)
      }

      // 自动修复：移除自环
      if (autoFix && selfLoops.length > 0) {
        graph.edges = graph.edges.filter((_, i) => !selfLoops.includes(i))
        result.addFix(`已移除 ${selfLoops.length} 条自环边`)
      }
    }

    // 5. 分组校验
    if (graph.groups && Array.isArray(graph.groups)) {
      for (let i = 0; i < graph.groups.length; i++) {
        const group = graph.groups[i]

        // 5.1 ID 必须存在
        if (!group.id) {
          result.addError(`分组 #${i + 1} 缺少 id`, { groupIndex: i })
          continue
        }

        // 5.2 contains 必须是数组
        if (!group.contains || !Array.isArray(group.contains)) {
          result.addWarning(`分组 "${group.id}" 缺少 contains 数组`)
          if (autoFix) {
            group.contains = []
            result.addFix(`已为分组 "${group.id}" 添加空 contains 数组`)
          }
          continue
        }

        // 5.3 contains 中的节点必须存在
        const invalidContains = group.contains.filter(id => !nodeIds.has(id))
        if (invalidContains.length > 0) {
          result.addWarning(`分组 "${group.id}" 引用了不存在的节点: ${invalidContains.join(', ')}`)
          if (autoFix) {
            group.contains = group.contains.filter(id => nodeIds.has(id))
            result.addFix(`已从分组 "${group.id}" 移除无效节点引用`)
          }
        }

        // 5.4 分组样式校验
        if (group.style && !AllowedEnums.groupStyles.includes(group.style)) {
          result.addWarning(`分组 "${group.id}" 的样式 "${group.style}" 不在标准列表中`)
        }
      }

      // 移除空分组
      if (autoFix) {
        const originalLength = graph.groups.length
        graph.groups = graph.groups.filter(g => g.contains && g.contains.length > 0)
        if (graph.groups.length < originalLength) {
          result.addFix(`已移除 ${originalLength - graph.groups.length} 个空分组`)
        }
      }
    }

    // 6. 统计信息
    result.stats = {
      nodeCount: graph.nodes.length,
      edgeCount: graph.edges?.length || 0,
      groupCount: graph.groups?.length || 0,
      errorCount: result.errors.length,
      warningCount: result.warnings.length,
      fixCount: result.fixes.length
    }

    return result
  }

  /**
   * 快速校验（只检查关键项）
   */
  static quickValidate(graph) {
    if (!graph || !graph.nodes || graph.nodes.length === 0) {
      return { valid: false, error: '图数据无效' }
    }

    const nodeIds = new Set(graph.nodes.map(n => n.id))
    
    if (graph.edges) {
      for (const edge of graph.edges) {
        if (!nodeIds.has(edge.from) || !nodeIds.has(edge.to)) {
          return { valid: false, error: `边引用了不存在的节点` }
        }
      }
    }

    return { valid: true }
  }

  /**
   * 校验并自动修复
   */
  static validateAndFix(graph) {
    // 第一次校验并自动修复
    const result = this.validate(graph, { autoFix: true, strict: false })
    
    if (result.fixes.length > 0) {
      console.log('[SchemaValidator] 自动修复:', result.fixes)
    }
    
    if (result.warnings.length > 0) {
      console.warn('[SchemaValidator] 校验警告:', result.warnings)
    }

    // 如果有修复，重新验证修复后的数据
    if (result.fixes.length > 0) {
      const revalidate = this.validate(graph, { autoFix: false, strict: false })
      
      if (revalidate.errors.length > 0) {
        console.error('[SchemaValidator] 修复后仍有错误:', revalidate.errors)
        return {
          valid: false,
          graph: graph,
          result: revalidate
        }
      }
      
      console.log('[SchemaValidator] 修复成功，图数据有效')
      return {
        valid: true,
        graph: graph,
        result: result
      }
    }
    
    // 没有修复，直接返回原始结果
    if (result.errors.length > 0) {
      console.error('[SchemaValidator] 校验错误:', result.errors)
    }

    return {
      valid: result.valid,
      graph: graph,
      result: result
    }
  }
}
