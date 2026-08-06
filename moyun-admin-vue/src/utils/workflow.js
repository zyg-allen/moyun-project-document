/**
 * 工作流相关工具函数
 */

/**
 * 节点类型定义
 */
export const NODE_TYPES = {
  // 基础节点
  START: 'start',
  END: 'end',
  
  // AI 节点
  LLM: 'llm',
  AGENT: 'agent',
  KNOWLEDGE: 'knowledge',
  QUESTION: 'question',
  CLASSIFIER: 'classifier',
  EXTRACTOR: 'extractor',
  
  // 流程控制
  CONDITION: 'condition',
  LOOP: 'loop',
  ITERATOR: 'iterator',
  WHILE: 'while',
  PARALLEL: 'parallel',
  MERGE: 'merge',
  AGGREGATOR: 'aggregator',
  DELAY: 'delay',
  SUBFLOW: 'subflow',
  
  // 数据处理
  SETVAR: 'setvar',
  TEXT: 'text',
  TEMPLATE: 'template',
  CODE: 'code',
  HTTP: 'http',
  TOOL: 'tool'
}

/**
 * 节点颜色映射
 */
export const NODE_COLORS = {
  [NODE_TYPES.START]: '#67c23a',
  [NODE_TYPES.END]: '#909399',
  [NODE_TYPES.LLM]: '#409eff',
  [NODE_TYPES.AGENT]: '#e6a23c',
  [NODE_TYPES.KNOWLEDGE]: '#9b59b6',
  [NODE_TYPES.CONDITION]: '#f56c6c',
  [NODE_TYPES.LOOP]: '#00bcd4',
  [NODE_TYPES.PARALLEL]: '#ff9800',
  [NODE_TYPES.MERGE]: '#795548',
  [NODE_TYPES.CODE]: '#607d8b',
  [NODE_TYPES.HTTP]: '#3f51b5',
  [NODE_TYPES.TOOL]: '#009688',
  [NODE_TYPES.TEMPLATE]: '#8bc34a',
  [NODE_TYPES.SETVAR]: '#673ab7'
}

/**
 * 获取节点颜色
 */
export function getNodeColor(type) {
  return NODE_COLORS[type] || '#909399'
}

/**
 * 验证工作流
 * @param {Array} nodes 节点数组
 * @param {Array} edges 边数组
 * @returns {Object} { valid: boolean, errors: Array, warnings: Array }
 */
export function validateWorkflow(nodes, edges) {
  const errors = []
  const warnings = []
  
  if (!nodes || nodes.length === 0) {
    errors.push({ message: '工作流没有节点', nodeId: null })
    return { valid: false, errors, warnings }
  }
  
  // 检查开始节点
  const startNodes = nodes.filter(n => n.type === NODE_TYPES.START)
  if (startNodes.length === 0) {
    errors.push({ message: '缺少开始节点', nodeId: null })
  } else if (startNodes.length > 1) {
    errors.push({ message: '有多个开始节点', nodeId: null })
  }
  
  // 检查结束节点
  const endNodes = nodes.filter(n => n.type === NODE_TYPES.END)
  if (endNodes.length === 0) {
    warnings.push({ message: '没有结束节点', nodeId: null })
  }
  
  // 检查孤立节点
  const connectedIds = new Set()
  if (edges) {
    edges.forEach(edge => {
      connectedIds.add(edge.source)
      connectedIds.add(edge.target)
    })
  }
  
  nodes.forEach(node => {
    if (node.type !== NODE_TYPES.START && !connectedIds.has(node.id)) {
      warnings.push({ 
        message: `节点 "${node.data?.label || node.id}" 没有任何连接`,
        nodeId: node.id 
      })
    }
  })
  
  // 检查必需配置
  nodes.forEach(node => {
    const config = node.data?.config || {}
    
    switch (node.type) {
      case NODE_TYPES.LLM:
        if (!config.modelId) {
          errors.push({ message: `LLM节点 "${node.data?.label}" 未配置模型`, nodeId: node.id })
        }
        break
        
      case NODE_TYPES.CONDITION:
        if (!config.expression) {
          errors.push({ message: `条件节点 "${node.data?.label}" 未配置表达式`, nodeId: node.id })
        }
        break
        
      case NODE_TYPES.HTTP:
        if (!config.url) {
          errors.push({ message: `HTTP节点 "${node.data?.label}" 未配置URL`, nodeId: node.id })
        }
        break
        
      case NODE_TYPES.CODE:
        if (!config.code) {
          errors.push({ message: `代码节点 "${node.data?.label}" 未配置代码`, nodeId: node.id })
        }
        break
    }
  })
  
  return {
    valid: errors.length === 0,
    errors,
    warnings
  }
}

/**
 * 收集上游变量
 * @param {String} nodeId 当前节点ID
 * @param {Array} nodes 所有节点
 * @param {Array} edges 所有边
 * @returns {Array} 可用变量列表
 */
export function collectUpstreamVariables(nodeId, nodes, edges) {
  const variables = new Set(['input'])
  const nodeMap = new Map(nodes.map(n => [n.id, n]))
  
  // BFS 查找所有上游节点
  const visited = new Set()
  const queue = []
  
  // 找到所有指向当前节点的边
  edges.forEach(edge => {
    if (edge.target === nodeId) {
      queue.push(edge.source)
    }
  })
  
  while (queue.length > 0) {
    const currentId = queue.shift()
    if (visited.has(currentId)) continue
    visited.add(currentId)
    
    const node = nodeMap.get(currentId)
    if (!node) continue
    
    // 根据节点类型添加输出变量
    const config = node.data?.config || {}
    
    switch (node.type) {
      case NODE_TYPES.START:
        // 开始节点的输入参数
        if (config.inputs) {
          config.inputs.forEach(input => {
            if (input.name) variables.add(input.name)
          })
        }
        break
        
      case NODE_TYPES.LLM:
        variables.add(config.outputVariable || 'llm_output')
        break
        
      case NODE_TYPES.CODE:
        variables.add(config.outputVariable || 'code_result')
        break
        
      case NODE_TYPES.HTTP:
        variables.add(config.outputVariable || 'http_response')
        break
        
      case NODE_TYPES.KNOWLEDGE:
        variables.add(config.outputVariable || 'knowledge_result')
        break
        
      case NODE_TYPES.TEMPLATE:
        variables.add(config.outputVariable || 'template_output')
        break
        
      case NODE_TYPES.SETVAR:
        if (config.variableName) {
          variables.add(config.variableName)
        }
        break
        
      case NODE_TYPES.LOOP:
      case NODE_TYPES.ITERATOR:
        variables.add(config.itemVariable || 'item')
        variables.add(config.indexVariable || 'index')
        variables.add(config.outputVariable || 'loop_results')
        break
        
      case NODE_TYPES.MERGE:
        variables.add(config.outputVariable || 'merged_result')
        break
    }
    
    // 继续向上游查找
    edges.forEach(edge => {
      if (edge.target === currentId && !visited.has(edge.source)) {
        queue.push(edge.source)
      }
    })
  }
  
  return Array.from(variables).sort()
}

/**
 * 生成唯一节点ID
 */
export function generateNodeId(type) {
  return `${type}_${Date.now()}_${Math.random().toString(36).substr(2, 9)}`
}

/**
 * 深拷贝节点
 */
export function cloneNode(node) {
  return JSON.parse(JSON.stringify(node))
}

/**
 * 格式化执行时间
 */
export function formatDuration(ms) {
  if (ms < 1000) return `${ms}ms`
  if (ms < 60000) return `${(ms / 1000).toFixed(1)}s`
  return `${(ms / 60000).toFixed(1)}min`
}

/**
 * 获取节点默认配置
 */
export function getDefaultNodeConfig(type) {
  const defaults = {
    [NODE_TYPES.START]: {
      inputs: []
    },
    [NODE_TYPES.END]: {
      outputs: []
    },
    [NODE_TYPES.LLM]: {
      modelId: null,
      systemPrompt: '',
      userPrompt: '{{input}}',
      outputVariable: 'llm_output',
      temperature: 0.7,
      maxTokens: 2000
    },
    [NODE_TYPES.CONDITION]: {
      expression: ''
    },
    [NODE_TYPES.CODE]: {
      language: 'javascript',
      code: 'return input;',
      outputVariable: 'code_result'
    },
    [NODE_TYPES.HTTP]: {
      method: 'GET',
      url: '',
      headers: '{"Content-Type": "application/json"}',
      body: '',
      timeout: 30,
      outputVariable: 'http_response'
    },
    [NODE_TYPES.LOOP]: {
      listVariable: '',
      itemVariable: 'item',
      indexVariable: 'index',
      outputVariable: 'loop_results',
      maxIterations: 100
    },
    [NODE_TYPES.TEMPLATE]: {
      template: '',
      outputVariable: 'template_output'
    },
    [NODE_TYPES.SETVAR]: {
      variableName: '',
      value: '',
      valueType: 'string'
    },
    [NODE_TYPES.PARALLEL]: {
      mode: 'all',
      timeout: 60,
      outputVariable: 'parallel_results'
    },
    [NODE_TYPES.MERGE]: {
      mode: 'object',
      outputVariable: 'merged_result'
    }
  }
  
  return defaults[type] || {}
}

export default {
  NODE_TYPES,
  NODE_COLORS,
  getNodeColor,
  validateWorkflow,
  collectUpstreamVariables,
  generateNodeId,
  cloneNode,
  formatDuration,
  getDefaultNodeConfig
}
