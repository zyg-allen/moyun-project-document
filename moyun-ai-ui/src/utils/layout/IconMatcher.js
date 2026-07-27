/**
 * 图标智能匹配器 (RAG 简化版)
 * 基于关键词匹配，自动为节点选择最合适的图标和颜色
 */

/**
 * 图标知识库
 * 包含关键词、图标、颜色、描述等信息
 */
const IconKnowledgeBase = {
  // ========== 数据库类 ==========
  databases: [
    { keywords: ['mysql', 'mariadb'], icon: 'database', color: 'blue', description: 'MySQL 关系型数据库' },
    { keywords: ['postgresql', 'postgres', 'pg'], icon: 'database', color: 'blue', description: 'PostgreSQL' },
    { keywords: ['mongodb', 'mongo'], icon: 'database', color: 'green', description: 'MongoDB 文档数据库' },
    { keywords: ['oracle'], icon: 'database', color: 'red', description: 'Oracle 数据库' },
    { keywords: ['sqlserver', 'mssql'], icon: 'database', color: 'blue', description: 'SQL Server' },
    { keywords: ['sqlite'], icon: 'database', color: 'gray', description: 'SQLite' },
    { keywords: ['database', 'db', '数据库'], icon: 'database', color: 'blue', description: '数据库' }
  ],

  // ========== 缓存类 ==========
  caches: [
    { keywords: ['redis'], icon: 'cache', color: 'red', description: 'Redis 内存数据库' },
    { keywords: ['memcached', 'memcache'], icon: 'cache', color: 'green', description: 'Memcached' },
    { keywords: ['cache', '缓存'], icon: 'cache', color: 'yellow', description: '缓存服务' },
    { keywords: ['elasticache'], icon: 'aws_elasticache', color: 'blue', description: 'AWS ElastiCache' }
  ],

  // ========== 消息队列类 ==========
  queues: [
    { keywords: ['kafka'], icon: 'queue', color: 'gray', description: 'Apache Kafka' },
    { keywords: ['rabbitmq', 'rabbit'], icon: 'queue', color: 'orange', description: 'RabbitMQ' },
    { keywords: ['rocketmq'], icon: 'queue', color: 'orange', description: 'RocketMQ' },
    { keywords: ['activemq'], icon: 'queue', color: 'red', description: 'ActiveMQ' },
    { keywords: ['sqs'], icon: 'aws_sqs', color: 'orange', description: 'AWS SQS' },
    { keywords: ['sns'], icon: 'aws_sns', color: 'purple', description: 'AWS SNS' },
    { keywords: ['mq', 'queue', '队列', '消息'], icon: 'queue', color: 'green', description: '消息队列' }
  ],

  // ========== 服务器/计算类 ==========
  servers: [
    { keywords: ['nginx'], icon: 'server', color: 'green', description: 'Nginx Web 服务器' },
    { keywords: ['apache', 'httpd'], icon: 'server', color: 'red', description: 'Apache HTTP Server' },
    { keywords: ['tomcat'], icon: 'server', color: 'yellow', description: 'Apache Tomcat' },
    { keywords: ['node', 'nodejs'], icon: 'server', color: 'green', description: 'Node.js' },
    { keywords: ['spring', 'springboot'], icon: 'server', color: 'green', description: 'Spring Boot' },
    { keywords: ['ec2', 'instance'], icon: 'aws_ec2', color: 'orange', description: 'AWS EC2' },
    { keywords: ['lambda', '函数'], icon: 'aws_lambda', color: 'orange', description: 'AWS Lambda' },
    { keywords: ['server', '服务器', '服务'], icon: 'server', color: 'blue', description: '服务器' }
  ],

  // ========== 网关/负载均衡类 ==========
  gateways: [
    { keywords: ['gateway', '网关', 'api网关'], icon: 'server', color: 'purple', description: 'API 网关' },
    { keywords: ['apigateway', 'api-gateway'], icon: 'aws_api_gateway', color: 'purple', description: 'AWS API Gateway' },
    { keywords: ['elb', 'alb', 'nlb', '负载均衡', 'loadbalancer'], icon: 'aws_elb', color: 'orange', description: '负载均衡' },
    { keywords: ['cloudfront', 'cdn'], icon: 'aws_cloudfront', color: 'purple', description: 'CDN/CloudFront' }
  ],

  // ========== 存储类 ==========
  storage: [
    { keywords: ['s3', 'oss', '对象存储'], icon: 'aws_s3', color: 'green', description: '对象存储' },
    { keywords: ['minio'], icon: 'aws_s3', color: 'red', description: 'MinIO' },
    { keywords: ['file', '文件', 'storage', '存储'], icon: 'document', color: 'blue', description: '文件存储' }
  ],

  // ========== 容器/编排类 ==========
  containers: [
    { keywords: ['docker', '容器'], icon: 'server', color: 'blue', description: 'Docker' },
    { keywords: ['kubernetes', 'k8s'], icon: 'cloud', color: 'blue', description: 'Kubernetes' },
    { keywords: ['ecs'], icon: 'aws_ecs', color: 'orange', description: 'AWS ECS' },
    { keywords: ['eks'], icon: 'aws_eks', color: 'orange', description: 'AWS EKS' }
  ],

  // ========== 搜索/分析类 ==========
  search: [
    { keywords: ['elasticsearch', 'elastic', 'es'], icon: 'database', color: 'yellow', description: 'Elasticsearch' },
    { keywords: ['solr'], icon: 'database', color: 'orange', description: 'Apache Solr' },
    { keywords: ['weaviate'], icon: 'database', color: 'purple', description: 'Weaviate 向量数据库' },
    { keywords: ['milvus'], icon: 'database', color: 'blue', description: 'Milvus 向量数据库' },
    { keywords: ['pinecone'], icon: 'database', color: 'teal', description: 'Pinecone' }
  ],

  // ========== 接入终端类（不使用 actor 图标） ==========
  terminals: [
    { keywords: ['web端', 'web前端', 'browser', '浏览器'], icon: 'default', color: 'blue', description: 'Web端' },
    { keywords: ['移动端', 'app端', 'mobile', 'ios', 'android'], icon: 'default', color: 'purple', description: '移动端' },
    { keywords: ['小程序', 'miniapp', '微信'], icon: 'default', color: 'green', description: '小程序' },
    { keywords: ['客户端', 'client', '终端', 'terminal'], icon: 'default', color: 'blue', description: '客户端' },
    { keywords: ['admin', '管理员', '后台', '管理端'], icon: 'default', color: 'orange', description: '管理后台' },
    { keywords: ['pc端', 'pc', '桌面'], icon: 'default', color: 'gray', description: 'PC端' }
  ],

  // ========== 用户图标（仅在明确需要时使用） ==========
  users: [
    { keywords: ['用户'], icon: 'actor', color: 'blue', description: '用户' }
  ],

  // ========== 云服务/VPC ==========
  cloud: [
    { keywords: ['vpc', '私有网络'], icon: 'aws_vpc', color: 'green', description: 'VPC' },
    { keywords: ['cloud', '云'], icon: 'cloud', color: 'blue', description: '云服务' },
    { keywords: ['aws'], icon: 'cloud', color: 'orange', description: 'AWS' },
    { keywords: ['azure'], icon: 'cloud', color: 'blue', description: 'Azure' },
    { keywords: ['gcp', 'google'], icon: 'cloud', color: 'red', description: 'GCP' },
    { keywords: ['aliyun', '阿里云'], icon: 'cloud', color: 'orange', description: '阿里云' }
  ],

  // ========== 流程图特殊节点 ==========
  flowchart: [
    { keywords: ['开始', 'start', 'begin'], icon: 'start', color: 'green', description: '开始节点' },
    { keywords: ['结束', 'end', 'finish'], icon: 'end', color: 'red', description: '结束节点' },
    { keywords: ['判断', 'decision', '条件', 'if', '是否'], icon: 'decision', color: 'orange', description: '判断节点' }
  ]
}

/**
 * 图标匹配器类
 */
export class IconMatcher {
  /**
   * 为节点匹配最合适的图标
   * @param {Object} node - 节点对象
   * @returns {Object|null} - 匹配结果 { icon, color, description, confidence }
   */
  static matchIcon(node) {
    const searchText = [
      node.label || '',
      node.id || '',
      node.description || ''
    ].join(' ').toLowerCase()

    if (!searchText.trim()) {
      return null
    }

    // 遍历所有类别
    const allCategories = Object.values(IconKnowledgeBase)
    
    for (const category of allCategories) {
      for (const item of category) {
        for (const keyword of item.keywords) {
          if (searchText.includes(keyword.toLowerCase())) {
            return {
              icon: item.icon,
              color: item.color,
              description: item.description,
              matchedKeyword: keyword,
              confidence: this.calculateConfidence(searchText, keyword)
            }
          }
        }
      }
    }

    return null
  }

  /**
   * 计算匹配置信度
   */
  static calculateConfidence(text, keyword) {
    // 完全匹配（作为独立词）
    const wordBoundary = new RegExp(`\\b${keyword}\\b`, 'i')
    if (wordBoundary.test(text)) {
      return 1.0
    }
    
    // 包含匹配
    if (text.includes(keyword)) {
      return 0.8
    }

    return 0.5
  }

  /**
   * 批量增强节点图标
   * @param {Array} nodes - 节点列表
   * @returns {Array} - 增强后的节点列表
   */
  static enhanceNodes(nodes) {
    let enhancedCount = 0

    const enhanced = nodes.map(node => {
      // 如果已经有明确指定的图标，跳过
      if (node.icon && node.icon !== 'default') {
        return node
      }

      const match = this.matchIcon(node)
      if (match) {
        enhancedCount++
        return {
          ...node,
          icon: match.icon,
          color: node.color || match.color,
          _iconMatched: true,
          _matchInfo: {
            keyword: match.matchedKeyword,
            confidence: match.confidence,
            description: match.description
          }
        }
      }

      return node
    })

    if (enhancedCount > 0) {
      console.log(`[IconMatcher] 智能匹配了 ${enhancedCount} 个节点的图标`)
    }

    return enhanced
  }

  /**
   * 获取指定类别的所有图标
   */
  static getIconsByCategory(category) {
    return IconKnowledgeBase[category] || []
  }

  /**
   * 搜索图标
   */
  static searchIcons(query) {
    const results = []
    const lowerQuery = query.toLowerCase()

    for (const [category, items] of Object.entries(IconKnowledgeBase)) {
      for (const item of items) {
        const matchScore = item.keywords.some(k => k.includes(lowerQuery)) ||
                          item.description.toLowerCase().includes(lowerQuery)
        if (matchScore) {
          results.push({
            ...item,
            category
          })
        }
      }
    }

    return results
  }

  /**
   * 获取统计信息
   */
  static getStats() {
    const stats = {
      totalCategories: Object.keys(IconKnowledgeBase).length,
      totalIcons: 0,
      totalKeywords: 0,
      categories: {}
    }

    for (const [category, items] of Object.entries(IconKnowledgeBase)) {
      stats.categories[category] = items.length
      stats.totalIcons += items.length
      stats.totalKeywords += items.reduce((sum, item) => sum + item.keywords.length, 0)
    }

    return stats
  }
}

/**
 * 项目架构知识库
 * 存储知名开源项目的组件信息
 */
export const ProjectKnowledgeBase = {
  'dify': {
    description: 'Dify 是一个 LLM 应用开发平台',
    components: [
      { id: 'api', label: 'API Server', icon: 'server', layer: 1 },
      { id: 'web', label: 'Web 前端', icon: 'actor', layer: 0 },
      { id: 'worker', label: 'Worker', icon: 'server', layer: 2 },
      { id: 'sandbox', label: 'Sandbox', icon: 'server', layer: 2 },
      { id: 'postgres', label: 'PostgreSQL', icon: 'database', layer: 3 },
      { id: 'redis', label: 'Redis', icon: 'cache', layer: 3 },
      { id: 'weaviate', label: 'Weaviate', icon: 'database', layer: 3 },
      { id: 'celery', label: 'Celery', icon: 'queue', layer: 2 }
    ],
    edges: [
      { from: 'web', to: 'api' },
      { from: 'api', to: 'worker' },
      { from: 'api', to: 'postgres' },
      { from: 'api', to: 'redis' },
      { from: 'worker', to: 'weaviate' },
      { from: 'worker', to: 'sandbox' },
      { from: 'celery', to: 'redis' }
    ],
    suggestedType: 'architecture'
  },

  'langchain': {
    description: 'LangChain 是一个 LLM 应用开发框架',
    components: [
      { id: 'app', label: 'Application', icon: 'server' },
      { id: 'llm', label: 'LLM Provider', icon: 'cloud' },
      { id: 'vectorstore', label: 'Vector Store', icon: 'database' },
      { id: 'memory', label: 'Memory', icon: 'cache' },
      { id: 'tools', label: 'Tools', icon: 'server' }
    ],
    suggestedType: 'architecture'
  },

  'kubernetes': {
    description: 'Kubernetes 容器编排平台',
    components: [
      { id: 'apiserver', label: 'API Server', icon: 'server' },
      { id: 'etcd', label: 'etcd', icon: 'database' },
      { id: 'scheduler', label: 'Scheduler', icon: 'server' },
      { id: 'controller', label: 'Controller Manager', icon: 'server' },
      { id: 'kubelet', label: 'Kubelet', icon: 'server' },
      { id: 'kube-proxy', label: 'Kube Proxy', icon: 'server' }
    ],
    suggestedType: 'architecture'
  },

  'springcloud': {
    description: 'Spring Cloud 微服务架构',
    components: [
      { id: 'gateway', label: 'API Gateway', icon: 'server', layer: 1 },
      { id: 'eureka', label: 'Eureka/Nacos', icon: 'server', layer: 2 },
      { id: 'config', label: 'Config Server', icon: 'server', layer: 2 },
      { id: 'svc_a', label: 'Service A', icon: 'server', layer: 2 },
      { id: 'svc_b', label: 'Service B', icon: 'server', layer: 2 },
      { id: 'db', label: 'Database', icon: 'database', layer: 3 },
      { id: 'redis', label: 'Redis', icon: 'cache', layer: 3 }
    ],
    suggestedType: 'architecture'
  },

  '电商': {
    description: '电商系统分布式架构',
    components: [
      { id: 'user', label: '用户端', icon: 'actor', layer: 0 },
      { id: 'merchant', label: '商家端', icon: 'actor', layer: 0 },
      { id: 'gateway', label: 'API网关', icon: 'server', layer: 1 },
      { id: 'user_svc', label: '用户服务', icon: 'server', layer: 2 },
      { id: 'product_svc', label: '商品服务', icon: 'server', layer: 2 },
      { id: 'order_svc', label: '订单服务', icon: 'server', layer: 2 },
      { id: 'pay_svc', label: '支付服务', icon: 'server', layer: 2 },
      { id: 'mysql', label: 'MySQL', icon: 'database', layer: 3 },
      { id: 'redis', label: 'Redis', icon: 'cache', layer: 3 },
      { id: 'es', label: 'Elasticsearch', icon: 'database', layer: 3 },
      { id: 'mq', label: '消息队列', icon: 'queue', layer: 3 }
    ],
    suggestedType: 'architecture'
  },

  'kafka': {
    description: 'Kafka 分布式消息队列',
    components: [
      { id: 'producer', label: 'Producer', icon: 'server', layer: 0 },
      { id: 'broker1', label: 'Broker 1', icon: 'queue', layer: 1 },
      { id: 'broker2', label: 'Broker 2', icon: 'queue', layer: 1 },
      { id: 'broker3', label: 'Broker 3', icon: 'queue', layer: 1 },
      { id: 'zk', label: 'ZooKeeper', icon: 'server', layer: 2 },
      { id: 'consumer', label: 'Consumer Group', icon: 'server', layer: 2 }
    ],
    suggestedType: 'architecture'
  },

  'redis': {
    description: 'Redis 高可用架构',
    components: [
      { id: 'client', label: 'Client', icon: 'actor', layer: 0 },
      { id: 'sentinel', label: 'Redis Sentinel', icon: 'server', layer: 1 },
      { id: 'master', label: 'Master', icon: 'cache', layer: 2 },
      { id: 'slave1', label: 'Slave 1', icon: 'cache', layer: 2 },
      { id: 'slave2', label: 'Slave 2', icon: 'cache', layer: 2 }
    ],
    suggestedType: 'architecture'
  }
}
