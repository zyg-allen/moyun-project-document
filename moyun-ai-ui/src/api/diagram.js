/**
 * 架构图 API
 */
import axios from 'axios'

/**
 * 生成架构图
 * @param {string} content - 架构描述
 * @param {string} style - 图表风格: normal(普通) / enterprise(企业级)
 * @returns {Promise} API响应 { code, data, message }
 */
export async function generateDiagram(content, style = 'normal') {
  try {
    const response = await axios.post('/api/diagram/generate', { content, style })
    const res = response.data
    // 转换后端响应格式为前端期望的格式
    return {
      code: res.success ? 200 : 500,
      data: res.data,
      message: res.message
    }
  } catch (error) {
    console.error('生成架构图失败:', error)
    return {
      code: 500,
      data: null,
      message: error.response?.data?.message || error.message || '生成失败'
    }
  }
}

/**
 * 智能分析用户输入，识别系统类型和需求（前端本地分析）
 * @param {string} content - 用户输入内容
 * @returns {Object} 分析结果
 */
export function analyzeUserInput(content) {
  const text = content.toLowerCase()
  
  // 系统类型识别（优先级从高到低）
  const systemTypes = {
    agent: ['智能体', 'agent平台', 'agent', '自主agent', '多agent', 'ai平台', 'llm平台'],
    ai: ['ai', 'llm', 'dify', 'chatgpt', 'gpt', '大模型', '智能', 'rag', '向量', '对话', '问答', '知识库'],
    ecommerce: ['电商', '商城', '购物', '订单', '支付', '商品', '库存', '物流', '淘宝', '京东'],
    microservice: ['微服务', '分布式', 'spring cloud', 'dubbo', 'k8s', 'kubernetes', '容器'],
    iot: ['iot', '物联网', '传感器', '设备', '边缘', 'mqtt'],
    bigdata: ['大数据', 'hadoop', 'spark', 'flink', '数据仓库', '数据湖', 'etl'],
    cms: ['cms', '内容管理', '博客', '网站', '门户'],
    erp: ['erp', '企业资源', '人力资源', 'hr', 'oa', '办公'],
    gaming: ['游戏', '游戏服务器', 'mmo', '匹配'],
  }
  
  let detectedType = 'general'
  for (const [type, keywords] of Object.entries(systemTypes)) {
    if (keywords.some(kw => text.includes(kw))) {
      detectedType = type
      break
    }
  }
  
  // 复杂度识别
  const detailKeywords = ['详细', '完整', '完善', '全面', '技术架构', '企业级', '生产级', '深入']
  const simpleKeywords = ['简单', '简洁', '基础', '入门', '示例', '演示']
  
  let complexity = 'normal'
  if (detailKeywords.some(kw => text.includes(kw))) {
    complexity = 'detailed'
  } else if (simpleKeywords.some(kw => text.includes(kw))) {
    complexity = 'simple'
  }
  
  // 侧边栏需求识别
  const needLeftSidebar = ['监控', '日志', '追踪', '告警', '指标', 'prometheus', 'grafana'].some(kw => text.includes(kw))
  const needRightSidebar = ['治理', '配置', '注册', 'nacos', 'apollo', 'consul', '服务发现'].some(kw => text.includes(kw))
  
  // 特殊需求识别
  const needEdges = ['流程', '调用', '关系', '数据流', '链路'].some(kw => text.includes(kw))
  const needBlocks = ['模块', '分组', '域', '服务组'].some(kw => text.includes(kw))
  
  return {
    systemType: detectedType,
    complexity,
    needLeftSidebar,
    needRightSidebar,
    needEdges,
    needBlocks,
    inputLength: content.length
  }
}

/**
 * 根据分析结果生成智能提示
 * @param {Object} analysis - 分析结果
 * @returns {string} 智能提示
 */
export function generateSmartHints(analysis) {
  const hints = []
  
  // 系统类型特定提示
  const typeHints = {
    agent: '检测到 AI 智能体平台，将生成完整技术架构：接入层、应用层（对话/Agent/工作流）、能力层（Prompt/RAG/工具）、模型层、数据层、基础设施层',
    ai: '检测到 AI/LLM 系统，将包含：模型层、RAG、Agent 等 AI 特色组件',
    ecommerce: '检测到电商系统，将包含：商品、订单、支付、库存等核心模块',
    microservice: '检测到微服务架构，将包含：网关、服务注册、配置中心等基础设施',
    iot: '检测到物联网系统，将包含：设备层、边缘计算、数据采集等组件',
    bigdata: '检测到大数据系统，将包含：数据采集、处理、存储、分析等层级',
  }
  
  if (typeHints[analysis.systemType]) {
    hints.push(typeHints[analysis.systemType])
  }
  
  if (analysis.needLeftSidebar) hints.push('将添加监控体系侧边栏')
  if (analysis.needRightSidebar) hints.push('将添加服务治理侧边栏')
  if (analysis.complexity === 'detailed') hints.push('详细模式：每层 4-6 个节点，使用 blocks 分组')
  
  return hints.join('；')
}
