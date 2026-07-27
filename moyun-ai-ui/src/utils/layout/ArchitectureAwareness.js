/**
 * 架构感知层
 * 实现需求理解、图表类型决策、布局策略选择、内容范围确定
 */

import { ProjectKnowledgeBase } from './IconMatcher.js'

/**
 * 图表类型配置
 */
const DiagramTypeConfig = {
  architecture: {
    name: '系统架构图',
    keywords: ['架构', 'architecture', '系统', '微服务', 'microservice', '组件'],
    layout: 'layered',
    direction: 'DOWN',
    description: '展示系统组件及其关系'
  },
  flowchart: {
    name: '流程图',
    keywords: ['流程', 'flow', '步骤', 'process', '工作流', 'workflow'],
    layout: 'layered',
    direction: 'DOWN',
    description: '展示业务或技术流程'
  },
  sequence: {
    name: '时序图',
    keywords: ['时序', 'sequence', '调用链', '交互', 'interaction'],
    layout: 'layered',
    direction: 'RIGHT',
    description: '展示组件间的时序交互'
  },
  aws: {
    name: 'AWS架构图',
    keywords: ['aws', 'amazon', 'ec2', 's3', 'lambda', 'cloudfront'],
    layout: 'layered',
    direction: 'DOWN',
    description: 'AWS 云架构图'
  },
  swimlane: {
    name: '泳道图',
    keywords: ['泳道', 'swimlane', '部门', '角色', '职责'],
    layout: 'layered',
    direction: 'RIGHT',
    description: '按角色或部门划分的流程图'
  },
  mindmap: {
    name: '思维导图',
    keywords: ['思维导图', 'mindmap', '脑图', '大纲'],
    layout: 'mrtree',
    direction: 'RIGHT',
    description: '树形结构的思维导图'
  }
}

/**
 * 架构复杂度级别
 */
const ComplexityLevel = {
  SIMPLE: { name: '简单', minNodes: 3, maxNodes: 6 },
  MEDIUM: { name: '中等', minNodes: 7, maxNodes: 15 },
  DETAILED: { name: '详细', minNodes: 16, maxNodes: 30 },
  COMPREHENSIVE: { name: '全面', minNodes: 31, maxNodes: 50 }
}

/**
 * 架构感知层类
 */
export class ArchitectureAwareness {
  /**
   * 分析用户需求，返回增强的上下文信息
   * @param {string} userMessage - 用户输入
   * @returns {Object} - 分析结果
   */
  static analyze(userMessage) {
    const lowerMessage = userMessage.toLowerCase()

    return {
      // 检测知名项目
      project: this.detectProject(lowerMessage),
      // 推断图表类型
      diagramType: this.inferDiagramType(lowerMessage),
      // 推断复杂度
      complexity: this.inferComplexity(lowerMessage),
      // 推断布局策略
      layoutStrategy: this.inferLayoutStrategy(lowerMessage),
      // 生成上下文增强 Prompt
      contextPrompt: this.generateContextPrompt(lowerMessage)
    }
  }

  /**
   * 检测是否提到知名项目
   */
  static detectProject(message) {
    for (const [projectName, projectInfo] of Object.entries(ProjectKnowledgeBase)) {
      if (message.includes(projectName)) {
        return {
          name: projectName,
          found: true,
          info: projectInfo,
          suggestedComponents: projectInfo.components,
          suggestedEdges: projectInfo.edges,
          suggestedType: projectInfo.suggestedType
        }
      }
    }

    return { found: false }
  }

  /**
   * 推断图表类型
   */
  static inferDiagramType(message) {
    // 计算每种类型的匹配分数
    const scores = {}

    for (const [type, config] of Object.entries(DiagramTypeConfig)) {
      let score = 0
      for (const keyword of config.keywords) {
        if (message.includes(keyword.toLowerCase())) {
          score += keyword.length // 更长的关键词权重更高
        }
      }
      scores[type] = score
    }

    // 找出最高分
    const sorted = Object.entries(scores).sort((a, b) => b[1] - a[1])
    
    if (sorted[0][1] > 0) {
      const bestType = sorted[0][0]
      return {
        type: bestType,
        confidence: Math.min(sorted[0][1] / 10, 1),
        config: DiagramTypeConfig[bestType]
      }
    }

    // 默认返回架构图
    return {
      type: 'architecture',
      confidence: 0.5,
      config: DiagramTypeConfig.architecture
    }
  }

  /**
   * 推断期望的复杂度
   */
  static inferComplexity(message) {
    // 检测复杂度关键词
    if (message.includes('详细') || message.includes('完整') || 
        message.includes('detailed') || message.includes('comprehensive')) {
      return ComplexityLevel.DETAILED
    }

    if (message.includes('简单') || message.includes('简化') || 
        message.includes('simple') || message.includes('basic')) {
      return ComplexityLevel.SIMPLE
    }

    if (message.includes('全面') || message.includes('所有') || 
        message.includes('complete') || message.includes('full')) {
      return ComplexityLevel.COMPREHENSIVE
    }

    // 默认中等复杂度
    return ComplexityLevel.MEDIUM
  }

  /**
   * 推断布局策略
   */
  static inferLayoutStrategy(message) {
    const diagramType = this.inferDiagramType(message)
    
    return {
      algorithm: diagramType.config.layout,
      direction: diagramType.config.direction,
      // 根据预期复杂度调整间距
      spacing: this.inferComplexity(message).name === '详细' ? 'compact' : 'normal'
    }
  }

  /**
   * 生成上下文增强 Prompt
   */
  static generateContextPrompt(message) {
    const parts = []
    
    // 1. 项目信息
    const project = this.detectProject(message)
    if (project.found) {
      parts.push(`这是关于 ${project.name} 的架构图。`)
      parts.push(`${project.name} ${project.info.description}。`)
      parts.push(`核心组件包括：${project.info.components.map(c => c.label).join('、')}。`)
    }

    // 2. 复杂度要求
    const complexity = this.inferComplexity(message)
    parts.push(`期望的复杂度：${complexity.name}（${complexity.minNodes}-${complexity.maxNodes} 个节点）。`)

    // 3. 图表类型
    const diagramType = this.inferDiagramType(message)
    if (diagramType.confidence > 0.5) {
      parts.push(`建议使用 ${diagramType.config.name} 类型。`)
    }

    return parts.join(' ')
  }

  /**
   * 决策：选择最佳图表类型
   */
  static decideChartType(analysis) {
    // 如果检测到知名项目，使用项目建议的类型
    if (analysis.project.found && analysis.project.suggestedType) {
      return analysis.project.suggestedType
    }

    // 否则使用推断的类型
    return analysis.diagramType.type
  }

  /**
   * 决策：选择最佳布局参数
   */
  static decideLayoutParams(analysis) {
    const baseParams = {
      'elk.algorithm': analysis.layoutStrategy.algorithm,
      'elk.direction': analysis.layoutStrategy.direction
    }

    // 根据复杂度调整参数
    if (analysis.complexity.name === '详细' || analysis.complexity.name === '全面') {
      return {
        ...baseParams,
        'elk.spacing.nodeNode': '40',
        'elk.layered.spacing.nodeNodeBetweenLayers': '60'
      }
    }

    return {
      ...baseParams,
      'elk.spacing.nodeNode': '60',
      'elk.layered.spacing.nodeNodeBetweenLayers': '100'
    }
  }

  /**
   * 生成图表数据建议
   * 基于分析结果，生成推荐的节点和边
   */
  static generateSuggestions(analysis) {
    // 如果检测到知名项目，返回项目模板
    if (analysis.project.found) {
      return {
        type: analysis.project.suggestedType,
        nodes: analysis.project.suggestedComponents,
        edges: analysis.project.suggestedEdges || [],
        groups: []
      }
    }

    // 否则返回空建议
    return null
  }

  /**
   * 完整的分析和决策流程
   */
  static processUserRequest(userMessage) {
    // 1. 分析
    const analysis = this.analyze(userMessage)

    // 2. 决策
    const decisions = {
      chartType: this.decideChartType(analysis),
      layoutParams: this.decideLayoutParams(analysis),
      suggestions: this.generateSuggestions(analysis)
    }

    // 3. 返回结果
    return {
      analysis,
      decisions,
      contextPrompt: analysis.contextPrompt,
      // 是否有高置信度的分析结果
      hasStrongSignal: analysis.project.found || analysis.diagramType.confidence > 0.7
    }
  }
}
