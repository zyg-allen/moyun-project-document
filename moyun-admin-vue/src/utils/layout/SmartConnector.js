/**
 * 智能连接推断器
 * 根据节点类型和标签，智能推断缺失的连接关系
 */

/**
 * 节点类型层级定义
 * 层级越低，在架构中位置越靠前（用户侧）
 */
const NodeTypeHierarchy = {
  // 第0层：接入终端（Web/App/小程序等）
  'terminal': 0,
  'web': 0,
  'app': 0,
  'client': 0,
  'browser': 0,
  
  // 第1层：接入层/网关
  'gateway': 1,
  'cdn': 1,
  'loadbalancer': 1,
  'nginx': 1,
  'aws_cloudfront': 1,
  'aws_elb': 1,
  'aws_api_gateway': 1,
  'actor': 1,  // 如果有用户图标，放在接入层
  'user': 1,
  
  // 第2层：应用服务层
  'server': 2,
  'service': 2,
  'aws_ec2': 2,
  'aws_lambda': 2,
  'aws_ecs': 2,
  'aws_eks': 2,
  
  // 第3层：数据/中间件层
  'database': 3,
  'cache': 3,
  'queue': 3,
  'aws_rds': 3,
  'aws_dynamodb': 3,
  'aws_elasticache': 3,
  'aws_sqs': 3,
  'aws_s3': 3,
  
  // 第4层：基础设施
  'cloud': 4,
  'aws_vpc': 4,
  'monitor': 4,
  'log': 4
}

/**
 * 节点类型连接规则
 * 定义哪些类型的节点可以连接
 */
const ConnectionRules = {
  // 用户可以连接到网关和服务
  'actor': ['gateway', 'server', 'aws_api_gateway', 'aws_cloudfront', 'aws_elb'],
  
  // 网关可以连接到服务
  'gateway': ['server', 'aws_ec2', 'aws_lambda', 'aws_ecs'],
  'aws_api_gateway': ['aws_lambda', 'aws_ec2', 'aws_ecs'],
  'aws_cloudfront': ['aws_s3', 'aws_elb', 'aws_api_gateway'],
  'aws_elb': ['aws_ec2', 'aws_ecs', 'aws_eks'],
  
  // 服务可以连接到数据层
  'server': ['database', 'cache', 'queue', 'aws_rds', 'aws_dynamodb', 'aws_elasticache', 'aws_sqs', 'aws_s3'],
  'aws_ec2': ['aws_rds', 'aws_dynamodb', 'aws_elasticache', 'aws_sqs', 'aws_s3'],
  'aws_lambda': ['aws_dynamodb', 'aws_sqs', 'aws_s3', 'aws_rds'],
  
  // 缓存和队列通常被服务使用
  'cache': [],
  'queue': [],
  'database': []
}

/**
 * 标签关键词映射到节点类型
 */
const LabelToTypeMapping = [
  // 接入终端（优先匹配）
  { pattern: /web端|web前端|浏览器|browser/i, type: 'web' },
  { pattern: /移动端|app端|ios|android|移动应用/i, type: 'app' },
  { pattern: /小程序|miniapp|微信/i, type: 'app' },
  { pattern: /客户端|终端|client|terminal/i, type: 'terminal' },
  // 接入层
  { pattern: /gateway|网关|api网关|zuul|kong/i, type: 'gateway' },
  { pattern: /nginx|负载均衡|lb|elb|alb/i, type: 'loadbalancer' },
  { pattern: /cdn|cloudfront/i, type: 'cdn' },
  // 用户图标（如果明确是用户）
  { pattern: /^用户$|^user$/i, type: 'actor' },
  // 服务层
  { pattern: /service|服务|server|svc/i, type: 'server' },
  // 数据层
  { pattern: /mysql|postgres|oracle|sqlserver|数据库|db|rds|mongodb/i, type: 'database' },
  { pattern: /redis|memcache|cache|缓存|elasticache/i, type: 'cache' },
  { pattern: /kafka|rabbitmq|mq|queue|队列|sqs|rocketmq/i, type: 'queue' },
  { pattern: /s3|oss|存储|storage|minio/i, type: 'storage' },
  { pattern: /elastic|es|搜索|solr/i, type: 'database' },
  // AWS
  { pattern: /lambda|函数|serverless/i, type: 'aws_lambda' },
  { pattern: /ec2|实例|instance/i, type: 'aws_ec2' },
  // 基础设施
  { pattern: /监控|monitor|prometheus|grafana/i, type: 'monitor' },
  { pattern: /日志|log|elk|kibana/i, type: 'log' }
]

/**
 * 智能连接推断器类
 */
export class SmartConnector {
  /**
   * 分析节点，推断其类型层级
   */
  static inferNodeType(node) {
    // 1. 如果已有 icon，使用 icon 作为类型
    if (node.icon && NodeTypeHierarchy[node.icon] !== undefined) {
      return node.icon
    }
    
    // 2. 根据标签推断
    const label = (node.label || node.id || '').toLowerCase()
    for (const mapping of LabelToTypeMapping) {
      if (mapping.pattern.test(label)) {
        return mapping.type
      }
    }
    
    // 3. 默认为服务类型
    return 'server'
  }

  /**
   * 获取节点的层级
   */
  static getNodeLayer(node) {
    const type = this.inferNodeType(node)
    return NodeTypeHierarchy[type] ?? 2 // 默认应用层
  }

  /**
   * 检查两个节点是否可以连接
   */
  static canConnect(sourceNode, targetNode) {
    const sourceType = this.inferNodeType(sourceNode)
    const targetType = this.inferNodeType(targetNode)
    
    // 检查连接规则
    const allowedTargets = ConnectionRules[sourceType] || []
    if (allowedTargets.includes(targetType)) {
      return true
    }
    
    // 层级规则：只能连接相邻层或同层
    const sourceLayer = this.getNodeLayer(sourceNode)
    const targetLayer = this.getNodeLayer(targetNode)
    const layerDiff = Math.abs(sourceLayer - targetLayer)
    
    return layerDiff <= 1
  }

  /**
   * 智能补全节点的 layer 属性
   */
  static assignLayers(nodes) {
    return nodes.map(node => {
      if (node.layer === undefined) {
        return {
          ...node,
          layer: this.getNodeLayer(node),
          _layerInferred: true
        }
      }
      return node
    })
  }

  /**
   * 验证连接合理性，返回警告
   */
  static validateConnections(nodes, edges) {
    const warnings = []
    const nodeMap = new Map(nodes.map(n => [n.id, n]))

    for (const edge of edges) {
      const source = nodeMap.get(edge.from)
      const target = nodeMap.get(edge.to)
      
      if (!source || !target) continue

      const sourceLayer = this.getNodeLayer(source)
      const targetLayer = this.getNodeLayer(target)

      // 检查是否跨层连接（跳过中间层）
      if (Math.abs(sourceLayer - targetLayer) > 1) {
        warnings.push({
          type: 'cross_layer',
          message: `连接 "${source.label || source.id}" → "${target.label || target.id}" 跨越了多个层级`,
          edge: edge,
          suggestion: '考虑添加中间层组件'
        })
      }

      // 检查用户是否直连数据库
      if (this.inferNodeType(source) === 'actor' && 
          ['database', 'cache', 'aws_rds', 'aws_dynamodb'].includes(this.inferNodeType(target))) {
        warnings.push({
          type: 'security_risk',
          message: `用户 "${source.label}" 直接连接到数据层 "${target.label}"，存在安全风险`,
          edge: edge,
          suggestion: '应该通过网关或服务层访问数据'
        })
      }
    }

    return warnings
  }

  /**
   * 智能推断缺失的连接
   * 基于节点类型和层级，推断可能缺失的关键连接
   */
  static inferMissingConnections(nodes, edges) {
    const suggestions = []
    const existingConnections = new Set(edges.map(e => `${e.from}->${e.to}`))
    const nodeMap = new Map(nodes.map(n => [n.id, n]))

    // 按层级分组
    const layers = new Map()
    for (const node of nodes) {
      const layer = this.getNodeLayer(node)
      if (!layers.has(layer)) {
        layers.set(layer, [])
      }
      layers.get(layer).push(node)
    }

    // 检查每一层是否有连接到下一层
    const sortedLayers = [...layers.keys()].sort((a, b) => a - b)
    
    for (let i = 0; i < sortedLayers.length - 1; i++) {
      const currentLayer = layers.get(sortedLayers[i])
      const nextLayer = layers.get(sortedLayers[i + 1])

      for (const source of currentLayer) {
        // 检查是否有到下一层的连接
        const hasConnectionToNextLayer = edges.some(e => 
          e.from === source.id && 
          nextLayer.some(n => n.id === e.to)
        )

        if (!hasConnectionToNextLayer && nextLayer.length > 0) {
          // 推荐连接到下一层的第一个节点
          const target = nextLayer[0]
          const connectionKey = `${source.id}->${target.id}`
          
          if (!existingConnections.has(connectionKey)) {
            suggestions.push({
              from: source.id,
              to: target.id,
              reason: `${source.label || source.id} 可能需要连接到 ${target.label || target.id}`,
              confidence: 0.7
            })
          }
        }
      }
    }

    return suggestions
  }

  /**
   * 分析图的完整性
   */
  static analyzeCompleteness(nodes, edges) {
    const analysis = {
      score: 100,
      issues: [],
      suggestions: []
    }

    // 1. 检查是否有用户入口
    const hasUserEntry = nodes.some(n => 
      this.inferNodeType(n) === 'actor' || 
      (n.label || '').toLowerCase().includes('用户')
    )
    if (!hasUserEntry) {
      analysis.issues.push('缺少用户入口节点')
      analysis.score -= 10
    }

    // 2. 检查是否有数据层
    const hasDataLayer = nodes.some(n => 
      ['database', 'cache', 'aws_rds', 'aws_dynamodb', 'aws_s3'].includes(this.inferNodeType(n))
    )
    if (!hasDataLayer) {
      analysis.issues.push('缺少数据层组件（数据库、缓存等）')
      analysis.score -= 10
    }

    // 3. 检查连接合理性
    const connectionWarnings = this.validateConnections(nodes, edges)
    for (const warning of connectionWarnings) {
      analysis.issues.push(warning.message)
      analysis.score -= 5
    }

    // 4. 检查是否有孤立节点
    const connectedNodes = new Set()
    for (const edge of edges) {
      connectedNodes.add(edge.from)
      connectedNodes.add(edge.to)
    }
    const isolatedNodes = nodes.filter(n => !connectedNodes.has(n.id))
    if (isolatedNodes.length > 0) {
      analysis.issues.push(`存在 ${isolatedNodes.length} 个孤立节点`)
      analysis.score -= 5 * isolatedNodes.length
    }

    // 5. 推断缺失连接
    analysis.suggestions = this.inferMissingConnections(nodes, edges)

    analysis.score = Math.max(0, analysis.score)
    return analysis
  }

  /**
   * 智能优化边：过滤不合理的连接，减少交叉
   * @param {Array} nodes - 节点列表
   * @param {Array} edges - 边列表
   * @returns {Object} - { edges: 优化后的边, removed: 被移除的边, warnings: 警告 }
   */
  static optimizeEdges(nodes, edges) {
    if (!edges || edges.length === 0) {
      return { edges: [], removed: [], warnings: [] }
    }

    const nodeMap = new Map(nodes.map(n => [n.id, n]))
    const warnings = []
    const removed = []
    const optimized = []

    // 统计每个节点的出入度
    const outDegree = new Map()
    const inDegree = new Map()
    
    for (const edge of edges) {
      const sourceNode = nodeMap.get(edge.from)
      const targetNode = nodeMap.get(edge.to)
      
      if (!sourceNode || !targetNode) {
        removed.push({ ...edge, reason: '源或目标节点不存在' })
        continue
      }

      const sourceLayer = this.getNodeLayer(sourceNode)
      const targetLayer = this.getNodeLayer(targetNode)
      
      // 规则1：检查是否是回连（从下层连到上层）
      if (targetLayer < sourceLayer) {
        removed.push({ ...edge, reason: '回连（从下层连到上层）' })
        warnings.push(`移除回连: ${sourceNode.label || sourceNode.id} → ${targetNode.label || targetNode.id}`)
        continue
      }
      
      // 规则2：检查是否跨越太多层
      if (targetLayer - sourceLayer > 2) {
        removed.push({ ...edge, reason: '跨层太多（超过2层）' })
        warnings.push(`移除跨层边: ${sourceNode.label || sourceNode.id} → ${targetNode.label || targetNode.id}`)
        continue
      }
      
      // 规则3：同层互连检查（警告但保留）
      if (sourceLayer === targetLayer) {
        warnings.push(`同层互连: ${sourceNode.label || sourceNode.id} ↔ ${targetNode.label || targetNode.id}`)
      }
      
      // 统计出入度
      outDegree.set(edge.from, (outDegree.get(edge.from) || 0) + 1)
      inDegree.set(edge.to, (inDegree.get(edge.to) || 0) + 1)
      
      optimized.push(edge)
    }

    // 规则4：限制单个节点的出度（最多5条）
    const finalEdges = []
    const nodeEdgeCount = new Map()
    
    for (const edge of optimized) {
      const count = nodeEdgeCount.get(edge.from) || 0
      if (count < 5) {
        finalEdges.push(edge)
        nodeEdgeCount.set(edge.from, count + 1)
      } else {
        removed.push({ ...edge, reason: '出度过高（超过5条）' })
        warnings.push(`限制出度: ${edge.from} 已有5条出边`)
      }
    }

    console.log(`[SmartConnector] 边优化: ${edges.length} → ${finalEdges.length}, 移除 ${removed.length} 条`)
    
    return {
      edges: finalEdges,
      removed,
      warnings
    }
  }
}
