/**
 * 表单验证工具
 */

/**
 * 必填验证
 */
export function required(message = '此字段为必填项') {
  return {
    required: true,
    message,
    trigger: ['blur', 'change']
  }
}

/**
 * 字符串长度验证
 */
export function length(min, max, message) {
  return {
    min,
    max,
    message: message || `长度应在 ${min} 到 ${max} 个字符之间`,
    trigger: ['blur', 'change']
  }
}

/**
 * 邮箱验证
 */
export function email(message = '请输入有效的邮箱地址') {
  return {
    type: 'email',
    message,
    trigger: ['blur', 'change']
  }
}

/**
 * URL 验证
 */
export function url(message = '请输入有效的URL') {
  return {
    pattern: /^https?:\/\/.+/,
    message,
    trigger: ['blur', 'change']
  }
}

/**
 * 数字范围验证
 */
export function range(min, max, message) {
  return {
    validator: (rule, value, callback) => {
      if (value === '' || value === null || value === undefined) {
        callback()
        return
      }
      const num = Number(value)
      if (isNaN(num)) {
        callback(new Error('请输入有效的数字'))
        return
      }
      if (num < min || num > max) {
        callback(new Error(message || `数值应在 ${min} 到 ${max} 之间`))
        return
      }
      callback()
    },
    trigger: ['blur', 'change']
  }
}

/**
 * 正则验证
 */
export function pattern(regex, message = '格式不正确') {
  return {
    pattern: regex,
    message,
    trigger: ['blur', 'change']
  }
}

/**
 * 变量名验证 (字母、数字、下划线，不能以数字开头)
 */
export function variableName(message = '变量名只能包含字母、数字和下划线，且不能以数字开头') {
  return pattern(/^[a-zA-Z_][a-zA-Z0-9_]*$/, message)
}

/**
 * JSON 格式验证
 */
export function json(message = '请输入有效的JSON格式') {
  return {
    validator: (rule, value, callback) => {
      if (!value) {
        callback()
        return
      }
      try {
        JSON.parse(value)
        callback()
      } catch (e) {
        callback(new Error(message))
      }
    },
    trigger: 'blur'
  }
}

/**
 * 自定义验证器
 */
export function custom(validatorFn, message = '验证失败') {
  return {
    validator: (rule, value, callback) => {
      if (validatorFn(value)) {
        callback()
      } else {
        callback(new Error(message))
      }
    },
    trigger: ['blur', 'change']
  }
}

/**
 * 创建表单规则
 * @example
 * const rules = createRules({
 *   name: [required(), length(2, 50)],
 *   email: [required(), email()],
 *   url: [url()]
 * })
 */
export function createRules(rulesConfig) {
  const rules = {}
  for (const [field, fieldRules] of Object.entries(rulesConfig)) {
    rules[field] = Array.isArray(fieldRules) ? fieldRules : [fieldRules]
  }
  return rules
}

/**
 * 验证单个值
 */
export async function validateValue(value, rules) {
  for (const rule of rules) {
    if (rule.required && (value === '' || value === null || value === undefined)) {
      return { valid: false, message: rule.message }
    }
    
    if (rule.pattern && value && !rule.pattern.test(value)) {
      return { valid: false, message: rule.message }
    }
    
    if (rule.min !== undefined && value && value.length < rule.min) {
      return { valid: false, message: rule.message }
    }
    
    if (rule.max !== undefined && value && value.length > rule.max) {
      return { valid: false, message: rule.message }
    }
    
    if (rule.validator) {
      try {
        await new Promise((resolve, reject) => {
          rule.validator(rule, value, (error) => {
            if (error) reject(error)
            else resolve()
          })
        })
      } catch (e) {
        return { valid: false, message: e.message }
      }
    }
  }
  
  return { valid: true, message: '' }
}

export default {
  required,
  length,
  email,
  url,
  range,
  pattern,
  variableName,
  json,
  custom,
  createRules,
  validateValue
}
