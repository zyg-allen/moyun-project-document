/**
 * 格式探测器
 * 智能识别 AI 输出格式，支持新旧格式兼容
 */

/**
 * 输入格式类型
 */
export const InputFormat = {
  GRAPH_JSON: 'graph_json',       // 新格式：语义化 JSON
  XML_DISPLAY: 'xml_display',     // 现有格式：[DISPLAY_DIAGRAM] XML
  XML_EDIT: 'xml_edit',           // 编辑格式：[EDIT_DIAGRAM] JSON
  LEGACY_JSON: 'legacy_json',     // 旧格式：```json 代码块
  UNKNOWN: 'unknown'
}

/**
 * 格式探测器类
 */
export class FormatDetector {
  /**
   * 智能探测 AI 输出格式
   * @param {string} aiResponse - AI 响应内容
   * @returns {Object} - { format, content, confidence }
   */
  static detect(aiResponse) {
    if (!aiResponse || typeof aiResponse !== 'string') {
      return { format: InputFormat.UNKNOWN, content: null, confidence: 0 }
    }

    // 1. 检测新的 JSON 格式 [GRAPH_DATA]
    const graphMatch = aiResponse.match(/\[GRAPH_DATA\]([\s\S]*?)\[\/GRAPH_DATA\]/)
    if (graphMatch) {
      let rawContent = graphMatch[1].trim()
      console.log('[FormatDetector] 找到 GRAPH_DATA，内容长度:', rawContent.length)
      
      // 🔥 移除 JSON 中的注释（AI 有时会添加注释）
      rawContent = this.removeJsonComments(rawContent)
      
      try {
        const json = JSON.parse(rawContent)
        if (json.nodes && Array.isArray(json.nodes)) {
          console.log('[FormatDetector] 解析成功，节点数:', json.nodes.length)
          return { format: InputFormat.GRAPH_JSON, content: json, confidence: 1.0 }
        } else {
          console.warn('[FormatDetector] JSON 缺少 nodes 数组')
        }
      } catch (e) {
        console.warn('[FormatDetector] GRAPH_DATA JSON 解析失败:', e.message)
        console.warn('[FormatDetector] 清理后内容前200字符:', rawContent.substring(0, 200))
      }
    }

    // 2. 检测 XML 格式 [DISPLAY_DIAGRAM]（现有格式）
    const displayMatch = aiResponse.match(/\[DISPLAY_DIAGRAM\]([\s\S]*?)\[\/DISPLAY_DIAGRAM\]/)
    if (displayMatch) {
      const xml = displayMatch[1].trim()
      if (xml.includes('<mxCell') || xml.includes('<root>')) {
        return { format: InputFormat.XML_DISPLAY, content: xml, confidence: 1.0 }
      }
    }

    // 3. 检测 [EDIT_DIAGRAM]（编辑指令）
    const editMatch = aiResponse.match(/\[EDIT_DIAGRAM\]([\s\S]*?)\[\/EDIT_DIAGRAM\]/)
    if (editMatch) {
      try {
        const edits = JSON.parse(editMatch[1].trim())
        if (edits.edits && Array.isArray(edits.edits)) {
          return { format: InputFormat.XML_EDIT, content: edits, confidence: 1.0 }
        }
      } catch (e) {
        console.warn('[FormatDetector] EDIT_DIAGRAM JSON 解析失败:', e.message)
      }
    }

    // 4. 检测旧的 JSON 格式（```json 代码块）
    const legacyMatch = aiResponse.match(/```json\s*([\s\S]*?)\s*```/)
    if (legacyMatch) {
      try {
        const json = JSON.parse(legacyMatch[1])
        // 检查是否是图表数据
        if (json.layers || json.nodes || json.type) {
          return { format: InputFormat.LEGACY_JSON, content: json, confidence: 0.9 }
        }
      } catch (e) {
        console.warn('[FormatDetector] Legacy JSON 解析失败:', e.message)
      }
    }

    // 5. 未识别
    return { format: InputFormat.UNKNOWN, content: aiResponse, confidence: 0 }
  }

  /**
   * 检测是否正在生成中（未完成的标记）
   * @param {string} content - 内容
   * @returns {Object} - { isGenerating, type }
   */
  static detectGenerating(content) {
    if (!content) return { isGenerating: false, type: null }

    // 检测未完成的 GRAPH_DATA
    if (content.includes('[GRAPH_DATA]') && !content.includes('[/GRAPH_DATA]')) {
      return { isGenerating: true, type: 'graph_json' }
    }

    // 检测未完成的 DISPLAY_DIAGRAM
    if (content.includes('[DISPLAY_DIAGRAM]') && !content.includes('[/DISPLAY_DIAGRAM]')) {
      return { isGenerating: true, type: 'xml_display' }
    }

    // 检测未完成的 EDIT_DIAGRAM
    if (content.includes('[EDIT_DIAGRAM]') && !content.includes('[/EDIT_DIAGRAM]')) {
      return { isGenerating: true, type: 'xml_edit' }
    }

    // 检测未完成的 json 代码块
    const jsonStart = content.indexOf('```json')
    if (jsonStart !== -1) {
      const afterStart = content.substring(jsonStart + 7)
      if (!afterStart.includes('```')) {
        return { isGenerating: true, type: 'legacy_json' }
      }
    }

    return { isGenerating: false, type: null }
  }

  /**
   * 提取显示文本（去除图表数据）
   * @param {string} content - 原始内容
   * @returns {string} - 显示文本
   */
  static extractDisplayText(content) {
    if (!content) return ''

    let text = content

    // 移除完整的标记
    text = text.replace(/\[GRAPH_DATA\][\s\S]*?\[\/GRAPH_DATA\]/g, '')
    text = text.replace(/\[DISPLAY_DIAGRAM\][\s\S]*?\[\/DISPLAY_DIAGRAM\]/g, '')
    text = text.replace(/\[EDIT_DIAGRAM\][\s\S]*?\[\/EDIT_DIAGRAM\]/g, '')
    text = text.replace(/```json[\s\S]*?```/g, '')

    // 移除未完成的标记
    text = text.replace(/\[GRAPH_DATA\][\s\S]*/g, '')
    text = text.replace(/\[DISPLAY_DIAGRAM\][\s\S]*/g, '')
    text = text.replace(/\[EDIT_DIAGRAM\][\s\S]*/g, '')

    return text.trim()
  }

  /**
   * 移除 JSON 中的注释
   * AI 有时会在 JSON 中添加单行或多行注释
   */
  static removeJsonComments(jsonStr) {
    if (!jsonStr) return jsonStr
    
    let result = ''
    let inString = false
    let stringChar = ''
    let i = 0
    
    while (i < jsonStr.length) {
      const char = jsonStr[i]
      const nextChar = jsonStr[i + 1]
      
      // 处理字符串
      if ((char === '"' || char === "'") && (i === 0 || jsonStr[i - 1] !== '\\')) {
        if (!inString) {
          inString = true
          stringChar = char
        } else if (char === stringChar) {
          inString = false
        }
        result += char
        i++
        continue
      }
      
      // 在字符串外检测注释
      if (!inString) {
        // 单行注释 //
        if (char === '/' && nextChar === '/') {
          // 跳过到行尾
          while (i < jsonStr.length && jsonStr[i] !== '\n') {
            i++
          }
          continue
        }
        // 多行注释 /* */
        if (char === '/' && nextChar === '*') {
          i += 2
          while (i < jsonStr.length - 1 && !(jsonStr[i] === '*' && jsonStr[i + 1] === '/')) {
            i++
          }
          i += 2
          continue
        }
      }
      
      result += char
      i++
    }
    
    // 清理可能产生的语法问题
    result = result.replace(/,(\s*,)+/g, ',')      // 连续逗号
    result = result.replace(/,(\s*[\]\}])/g, '$1') // 末尾逗号
    
    return result
  }
}
