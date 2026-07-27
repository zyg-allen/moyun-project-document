/**
 * ELK 布局引擎 - 类型定义和常量
 */

// ========== 图表类型 ==========
export const DiagramType = {
  ARCHITECTURE: 'architecture',
  FLOWCHART: 'flowchart',
  SEQUENCE: 'sequence',
  SWIMLANE: 'swimlane',
  MINDMAP: 'mindmap',
  AWS: 'aws'
}

// ========== 节点图标类型 ==========
export const NodeIcon = {
  DEFAULT: 'default',
  ACTOR: 'actor',
  DATABASE: 'database',
  CACHE: 'cache',
  QUEUE: 'queue',
  SERVER: 'server',
  CLOUD: 'cloud',
  DOCUMENT: 'document',
  DECISION: 'decision',
  START: 'start',
  END: 'end',
  // AWS 图标
  AWS_EC2: 'aws_ec2',
  AWS_S3: 'aws_s3',
  AWS_LAMBDA: 'aws_lambda',
  AWS_RDS: 'aws_rds',
  AWS_DYNAMODB: 'aws_dynamodb',
  AWS_SQS: 'aws_sqs',
  AWS_SNS: 'aws_sns',
  AWS_API_GATEWAY: 'aws_api_gateway',
  AWS_CLOUDFRONT: 'aws_cloudfront',
  AWS_ELB: 'aws_elb',
  AWS_VPC: 'aws_vpc',
  AWS_ELASTICACHE: 'aws_elasticache',
  AWS_ECS: 'aws_ecs',
  AWS_EKS: 'aws_eks'
}

// ========== 边类型 ==========
export const EdgeType = {
  SOLID: 'solid',
  DASHED: 'dashed',
  DOTTED: 'dotted',
  ANIMATED: 'animated',
  BIDIRECTIONAL: 'bidirectional'
}

// ========== 颜色主题 ==========
export const ColorTheme = {
  BLUE: 'blue',
  GREEN: 'green',
  ORANGE: 'orange',
  RED: 'red',
  PURPLE: 'purple',
  GRAY: 'gray',
  YELLOW: 'yellow',
  TEAL: 'teal',
  PINK: 'pink'
}

// ========== 分组样式 ==========
export const GroupStyle = {
  CONTAINER: 'container',
  SWIMLANE: 'swimlane',
  REGION: 'region',
  AWS_VPC: 'aws_vpc'
}

// ========== 默认节点尺寸 ==========
export const DefaultNodeSizes = {
  default: { width: 140, height: 50 },
  actor: { width: 40, height: 50 },       // 小人图标，更小
  user: { width: 40, height: 50 },        // 用户图标，同 actor
  client: { width: 40, height: 50 },      // 客户端图标，同 actor
  database: { width: 80, height: 60 },
  cache: { width: 80, height: 60 },
  queue: { width: 100, height: 45 },
  server: { width: 120, height: 45 },
  decision: { width: 100, height: 60 },
  start: { width: 50, height: 50 },
  end: { width: 50, height: 50 },
  aws_icon: { width: 64, height: 64 }
}

// ========== 性能阈值 ==========
export const PerformanceThresholds = {
  DIRECT: 20,      // 主线程直接计算
  WORKER: 100,     // Web Worker
  SIMPLIFIED: 200, // 简化算法
  REJECT: 500      // 拒绝
}
