<template>
  <div class="diagram-chat">
    <!-- 左侧编辑器 -->
    <div class="editor-panel">
      <!-- Draw.io 编辑器 -->
      <iframe 
        ref="drawioFrame"
        class="drawio-frame"
        :src="drawioUrl"
        frameborder="0"
      ></iframe>
      
      <!-- 拖拽时的遮罩层，防止 iframe 捕获鼠标事件 -->
      <div v-if="isResizing" class="iframe-overlay"></div>
    </div>

    <!-- 可拖拽分隔条 -->
    <div 
      class="resize-handle"
      @mousedown="startResize"
    >
      <div class="resize-handle-icon"></div>
    </div>

    <!-- 右侧聊天面板 -->
    <div class="chat-panel" :style="{ width: chatPanelWidth + 'px' }">
      <div class="chat-header">
        <i class="fa-solid fa-comments"></i>
        <span>AI 架构师助手</span>
      </div>

      <!-- 消息列表 -->
      <div class="message-list" ref="messageList" @scroll="handleMessageListScroll">
        <!-- 历史版本面板 -->
        <div class="history-panel" v-if="showHistoryPanel && diagramHistory.length > 0">
          <div class="history-panel-header">
            <span><i class="fa-solid fa-clock-rotate-left"></i> 历史版本 ({{ diagramHistory.length }})</span>
            <i class="fa-solid fa-xmark history-close" @click="showHistoryPanel = false"></i>
          </div>
          <div class="history-list">
            <div
              v-for="(item, idx) in diagramHistory"
              :key="idx"
              class="history-item"
              @click="restoreFromHistory(idx); showHistoryPanel = false"
            >
              <span class="history-action">{{ item.action }}</span>
              <span class="history-time">{{ formatHistoryTime(item.timestamp) }}</span>
            </div>
          </div>
        </div>

        <!-- 欢迎消息 -->
        <div class="message assistant" v-if="messages.length === 0">
          <div class="message-avatar">
            <i class="fa-solid fa-robot"></i>
          </div>
          <div class="message-content">
            <div class="message-text welcome-text">
              <div class="welcome-title">👋 你好！我是 AI 架构师助手</div>
              <p>告诉我你想设计什么，我会帮你生成<strong>专业的架构图表</strong>。</p>

              <div class="welcome-section">
                <div class="section-title">📊 支持的图表类型</div>
                <ul>
                  <li><strong>系统架构图</strong> - 电商、微服务、AI系统、SaaS后台...</li>
                  <li><strong>流程图</strong> - 业务流程、审批流程、状态机...</li>
                  <li><strong>AWS/云架构</strong> - AWS、阿里云、Azure 服务架构</li>
                  <li><strong>泳道图</strong> - 跨部门协作、多角色流程</li>
                  <li><strong>时序图</strong> - API调用、服务交互时序</li>
                  <li><strong>ER图</strong> - 数据库表结构设计</li>
                </ul>
              </div>

              <div class="welcome-section">
                <div class="section-title">💡 使用示例</div>
                <div class="example-list">
                  <span class="example-item">"设计一个电商系统架构"</span>
                  <span class="example-item">"画个用户注册流程图"</span>
                  <span class="example-item">"AI智能客服系统架构"</span>
                </div>
              </div>

              <div class="welcome-tip">
                ✨ 生成后可以说 <code>添加Redis缓存</code>、<code>修改颜色</code> 来编辑
              </div>
            </div>
          </div>
        </div>

        <!-- 对话消息 -->
        <div 
          v-for="(msg, index) in messages" 
          :key="index"
          class="message"
          :class="msg.role"
        >
          <div class="message-avatar">
            <i :class="msg.role === 'user' ? 'fa-solid fa-user' : 'fa-solid fa-robot'"></i>
          </div>
          <div class="message-content">
            <div class="message-text" v-html="formatMessage(msg.content)"></div>
            <!-- 渲染状态 -->
            <div class="render-status" v-if="msg.role === 'assistant' && msg.hasXml">
              <i class="fa-solid fa-check-circle"></i>
              <span>架构图已渲染到左侧编辑器</span>
            </div>
          </div>
        </div>

        <!-- 正在输入 -->
        <div class="message assistant" v-if="isGenerating">
          <div class="message-avatar">
            <i class="fa-solid fa-robot"></i>
          </div>
          <div class="message-content">
            <div class="message-text">
              <span v-html="formatMessage(streamingContent)"></span>
              <span class="typing-cursor">|</span>
            </div>
          </div>
        </div>
      </div>

      <!-- 输入区域 -->
      <div class="input-area">
        <!-- 操作按钮 -->
        <div class="action-bar">
          <div class="tips">
            <i class="fa-solid fa-lightbulb"></i>
            <span>支持：系统架构、流程图、AWS架构等</span>
          </div>
          <!-- 历史版本按钮 -->
          <div
            class="action-btn history-btn"
            :class="{ disabled: isGenerating || diagramHistory.length === 0 }"
            @click="!isGenerating && diagramHistory.length > 0 && (showHistoryPanel = !showHistoryPanel)"
            title="历史版本"
          >
            <i class="fa-solid fa-clock-rotate-left"></i>
            <span>历史</span>
          </div>
          <!-- 清空对话按钮 -->
          <div
            class="action-btn clear-btn"
            :class="{ disabled: isGenerating || messages.length === 0 }"
            @click="!isGenerating && messages.length > 0 && clearChat()"
            title="清空对话"
          >
            <i class="fa-solid fa-trash-can"></i>
            <span>清空</span>
          </div>
        </div>
        
        <!-- 输入框 -->
        <div class="input-box">
          <el-input
            v-model="userInput"
            type="textarea"
            :rows="2"
            placeholder="描述你想要的图表，或说'添加xxx'来修改现有图表..."
            resize="none"
            @keydown.enter.ctrl="sendMessage"
            :disabled="isGenerating"
          />
          <el-button 
            type="primary" 
            :loading="isGenerating"
            :disabled="!userInput.trim() || isGenerating"
            @click="sendMessage"
            class="send-btn"
          >
            <i class="fa-solid fa-paper-plane"></i>
          </el-button>
        </div>
        <div class="input-tip">Ctrl + Enter 发送</div>
      </div>
    </div>
  </div>
</template>

<script setup>
import useUserStore from '@/store/modules/user'
import cache from '@/plugins/cache'

import { ref, onMounted, onUnmounted, nextTick } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { convertToDrawioXml, extractJsonFromContent, isJsonComplete } from '@/utils/diagramRenderer'
// ELK 布局引擎
import { 
  UnifiedProcessor, 
  FormatDetector, 
  editTracker,
  PerformanceManager 
} from '@/utils/layout'

const userStore = useUserStore()

// Draw.io URL 参数说明:
// embed=1: 嵌入模式
// proto=json: 使用 JSON 协议通信
// spin=1: 加载时显示旋转动画
// ui=atlas: 使用 Atlas UI 主题
// libraries=1: 启用形状库
// nav=1: 启用导航
// panning=1: 启用平移
// saveAndExit=0: 禁用保存并退出按钮
// noSaveBtn=1: 隐藏保存按钮
// noExitBtn=1: 隐藏退出按钮
const drawioUrl = 'https://embed.diagrams.net/?embed=1&proto=json&spin=1&ui=atlas&libraries=1&nav=1&panning=1&saveAndExit=0&noSaveBtn=1&noExitBtn=1'

// 状态
const userInput = ref('')
const messages = ref([])
const isGenerating = ref(false)
const streamingContent = ref('')
const currentDiagramXml = ref('')

// 图表历史（用于版本回退）
const diagramHistory = ref([])
const MAX_HISTORY = 20
const showHistoryPanel = ref(false)

// 格式化历史时间
const formatHistoryTime = (timestamp) => {
  const date = new Date(timestamp)
  const now = new Date()
  const diff = now - date
  
  if (diff < 60000) return '刚刚'
  if (diff < 3600000) return `${Math.floor(diff / 60000)} 分钟前`
  if (diff < 86400000) return `${Math.floor(diff / 3600000)} 小时前`
  return date.toLocaleString('zh-CN', { month: 'short', day: 'numeric', hour: '2-digit', minute: '2-digit' })
}

// Router
const router = useRouter()

// Refs
const drawioFrame = ref(null)
const messageList = ref(null)
const drawioReady = ref(false)

// 面板宽度（可拖拽调整）- 支持屏幕 15%-50%
const chatPanelWidth = ref(400)
const isResizing = ref(false)
const getMinWidth = () => Math.max(250, window.innerWidth * 0.15)  // 最小 15%
const getMaxWidth = () => Math.min(900, window.innerWidth * 0.5)   // 最大 50%

// 拖拽调整宽度
const startResize = (e) => {
  isResizing.value = true
  // 添加拖拽时的样式
  document.body.style.cursor = 'col-resize'
  document.body.style.userSelect = 'none'
  
  document.addEventListener('mousemove', doResize)
  document.addEventListener('mouseup', stopResize)
  // 防止拖拽时选中文本
  document.addEventListener('selectstart', preventSelect)
  e.preventDefault()
}

const preventSelect = (e) => {
  e.preventDefault()
}

const doResize = (e) => {
  if (!isResizing.value) return
  const containerWidth = window.innerWidth
  const newWidth = containerWidth - e.clientX
  chatPanelWidth.value = Math.max(getMinWidth(), Math.min(getMaxWidth(), newWidth))
}

const stopResize = () => {
  if (!isResizing.value) return
  isResizing.value = false
  // 恢复样式
  document.body.style.cursor = ''
  document.body.style.userSelect = ''
  
  document.removeEventListener('mousemove', doResize)
  document.removeEventListener('mouseup', stopResize)
  document.removeEventListener('selectstart', preventSelect)
}

// Draw.io 安全域名
const DRAWIO_ALLOWED_ORIGINS = ['https://embed.diagrams.net', 'https://app.diagrams.net']


// 清空对话
const clearChat = () => {
  messages.value = []
  currentDiagramXml.value = ''
  diagramHistory.value = []  // 清空历史
  showHistoryPanel.value = false  // 关闭历史面板
  // 重置编辑追踪
  editTracker.resetForNewDiagram()
  // 清空 Draw.io 编辑器
  if (drawioReady.value) {
    sendToDrawio({
      action: 'load',
      xml: '<mxGraphModel><root><mxCell id="0"/><mxCell id="1" parent="0"/></root></mxGraphModel>'
    })
  }
  ElMessage.success('对话已清空')
}

// 发送消息
const sendMessage = async () => {
  const text = userInput.value.trim()
  if (!text || isGenerating.value) return
  
  // 先获取历史消息（不包含当前消息）
  const historyMessages = messages.value.slice(-6).map(m => ({
    role: m.role,
    // 过滤掉图表数据，避免发送过多数据
    content: m.content
      .replace(/\[DISPLAY_DIAGRAM\][\s\S]*?\[\/DISPLAY_DIAGRAM\]/g, '[图表XML已省略]')
      .replace(/\[EDIT_DIAGRAM\][\s\S]*?\[\/EDIT_DIAGRAM\]/g, '[编辑指令已省略]')
      .replace(/```json[\s\S]*?```/g, '[图表数据已省略]')
      .substring(0, 500)
  }))
  
  // 添加用户消息
  messages.value.push({
    role: 'user',
    content: text
  })
  userInput.value = ''
  scrollToBottom()
  
  // 开始流式生成
  isGenerating.value = true
  streamingContent.value = ''
  
  // 重置渲染状态
  lastRenderedJson = false
  lastRenderedLayerCount = 0
  
  // 超时控制器（5分钟超时）
  const controller = new AbortController()
  const timeoutId = setTimeout(() => {
    controller.abort()
    console.warn('[SSE] 请求超时，已取消')
  }, 5 * 60 * 1000)
  
  try {
    // 获取认证 Token
    const token = userStore.token
    
    const response = await fetch('/cms/ai/diagram/chat/stream', {
      signal: controller.signal,
      method: 'POST',
      headers: {
        'Content-Type': 'application/json',
        'Authorization': token ? 'Bearer ' + token : ''
      },
      body: JSON.stringify({
        message: text,
        history: historyMessages,
        currentDiagramXml: currentDiagramXml.value
      })
    })
    
    if (!response.ok) {
      // 401 未授权，清除token并跳转登录页
      if (response.status === 401) {
        userStore.logOut()
        cache.local.remove('username')
        cache.local.remove('nickname')
        ElMessage.warning('登录已过期，请重新登录')
        router.push('/login')
        return
      }
      const errMsg = response.status === 429 ? '请求过于频繁，请稍后再试'
                   : response.status >= 500 ? '服务器错误，请稍后再试'
                   : '请求失败 (' + response.status + ')'
      throw new Error(errMsg)
    }
    
    // 读取 SSE 流
    const reader = response.body.getReader()
    const decoder = new TextDecoder()
    let buffer = ''
    
    let receivedDone = false
    
    while (true) {
      const { done, value } = await reader.read()
      if (done) {
        console.log('[SSE] 流结束')
        break
      }
      
      buffer += decoder.decode(value, { stream: true })
      
      // SSE 事件之间用 \n\n 分隔
      const events = buffer.split('\n\n')
      buffer = events.pop() || ''  // 最后一个可能不完整，保留
      
      for (const eventBlock of events) {
        if (!eventBlock.trim()) continue
        
        const lines = eventBlock.split('\n')
        let eventType = 'message'
        let eventData = ''
        
        for (const line of lines) {
          if (line.startsWith('event:')) {
            eventType = line.slice(6).trim()
          } else if (line.startsWith('data:')) {
            eventData = line.slice(5)  // 不 trim，保留空格
          }
        }
        
        console.log('[SSE] 事件:', eventType, '数据长度:', eventData.length)
        
        if (eventType === 'done' || eventData === '[DONE]') {
          // 完成
          receivedDone = true
          finishMessage()
        } else if (eventType === 'error') {
          // 错误
          ElMessage.error(eventData || '生成失败')
          isGenerating.value = false
        } else if (eventType === 'message' && eventData) {
          // 解析 JSON 格式数据
          let content = eventData
          try {
            const json = JSON.parse(eventData)
            if (json.content) {
              content = json.content
            }
          } catch (e) {
            // 如果不是 JSON 格式，直接使用原始数据
          }
          
          // 追加内容
          streamingContent.value += content
          
          // 每500个字符打印一次日志
          if (streamingContent.value.length % 500 < content.length) {
            console.log('[SSE] 累计内容长度:', streamingContent.value.length)
          }
          
          scrollToBottom()
          
          // 实时检测并渲染 JSON
          tryRenderJson()
        }
      }
    }
    
    // 如果流正常结束但没有收到 [DONE] 信号，也要完成
    if (!receivedDone && streamingContent.value) {
      finishMessage()
    }
    
  } catch (error) {
    const isAborted = error.name === 'AbortError'
    console.error('流式请求失败:', isAborted ? '请求超时' : error.message)
    ElMessage.error(isAborted ? '生成超时，请重试' : '生成失败: ' + error.message)
    isGenerating.value = false
    
    // 如果有部分内容，保存到消息中
    if (streamingContent.value && streamingContent.value.length > 50) {
      messages.value.push({
        role: 'assistant',
        content: streamingContent.value + '\n\n⚠️ (生成被中断)',
        hasXml: false
      })
    }
    streamingContent.value = ''
  } finally {
    // 清除超时定时器
    clearTimeout(timeoutId)
  }
}

// ========== XML 渲染逻辑 ==========
let renderDebounceTimer = null
const RENDER_DEBOUNCE_MS = 300
let lastTryRenderTime = 0
const RENDER_THROTTLE_MS = 200
let lastRenderedLayerCount = 0
let lastRenderedJson = false

/**
 * 验证 XML 是否有效
 * 参考 next-ai-draw-io 的 validateMxCellStructure
 */
const validateXml = (xml) => {
  if (!xml || typeof xml !== 'string') return false
  
  // 基本检查：必须包含 mxCell
  if (!xml.includes('<mxCell')) {
    console.warn('[validateXml] XML 中没有 mxCell 元素')
    return false
  }
  
  try {
    const parser = new DOMParser()
    const doc = parser.parseFromString(xml, 'text/xml')
    
    // 检查 XML 语法错误
    const parseError = doc.querySelector('parsererror')
    if (parseError) {
      console.warn('[validateXml] XML 语法错误:', parseError.textContent?.substring(0, 200))
      return false
    }
    
    const cells = doc.querySelectorAll('mxCell')
    const cellIds = new Set()
    const duplicateIds = []
    const nestedCells = []
    
    cells.forEach(cell => {
      const id = cell.getAttribute('id')
      
      // 检查重复 ID
      if (id) {
        if (cellIds.has(id)) {
          duplicateIds.push(id)
        } else {
          cellIds.add(id)
        }
      }
      
      // 检查嵌套 mxCell（mxCell 的父元素不应该是 mxCell）
      if (cell.parentElement?.tagName === 'mxCell') {
        nestedCells.push(id || 'unknown')
      }
    })
    
    // 检查必要的根 mxCell
    if (!cellIds.has('0') || !cellIds.has('1')) {
      console.warn('[validateXml] 缺少必要的根 mxCell (id=0, id=1)')
      return false
    }
    
    // 检查嵌套 mxCell
    if (nestedCells.length > 0) {
      console.warn('[validateXml] 发现嵌套的 mxCell:', nestedCells.slice(0, 3).join(', '))
      return false
    }
    
    // 检查重复 ID
    if (duplicateIds.length > 0) {
      console.warn('[validateXml] 发现重复的 ID:', duplicateIds.slice(0, 3).join(', '))
      return false
    }
    
    // 验证边的连接
    const edges = doc.querySelectorAll('mxCell[edge="1"]')
    for (const edge of edges) {
      const source = edge.getAttribute('source')
      const target = edge.getAttribute('target')
      if (source && !cellIds.has(source)) {
        console.warn('[validateXml] 边的 source 引用不存在的节点:', source)
        // 不返回 false，只是警告
      }
      if (target && !cellIds.has(target)) {
        console.warn('[validateXml] 边的 target 引用不存在的节点:', target)
      }
    }
    
    return true
  } catch (e) {
    console.warn('[validateXml] XML 验证失败:', e)
    return false
  }
}

/**
 * 从 AI 输出中提取 DISPLAY_DIAGRAM 标记内的 XML
 */
const extractDisplayDiagramXml = (content) => {
  const match = content.match(/\[DISPLAY_DIAGRAM\]([\s\S]*?)\[\/DISPLAY_DIAGRAM\]/)
  if (match) {
    let xml = match[1].trim()
    // 如果只有 <root>...</root>，包装成完整的 mxGraphModel
    if (xml.startsWith('<root>') && !xml.includes('<mxGraphModel>')) {
      xml = `<mxGraphModel>${xml}</mxGraphModel>`
    }
    
    // 验证 XML
    if (!validateXml(xml)) {
      console.error('[extractDisplayDiagramXml] 提取的 XML 无效')
      return null
    }
    
    return xml
  }
  return null
}

/**
 * 从 AI 输出中提取 EDIT_DIAGRAM 标记内的编辑指令
 */
const extractEditDiagramCommands = (content) => {
  const match = content.match(/\[EDIT_DIAGRAM\]([\s\S]*?)\[\/EDIT_DIAGRAM\]/)
  if (match) {
    try {
      return JSON.parse(match[1].trim())
    } catch (e) {
      console.warn('[EDIT_DIAGRAM] JSON 解析失败:', e)
    }
  }
  return null
}

/**
 * 应用编辑指令到当前 XML
 * 参考 next-ai-draw-io 项目，支持 7 种匹配策略
 */
const applyEdits = (xml, edits) => {
  if (!xml || !edits || !edits.edits) return xml
  
  let result = xml
  let appliedCount = 0
  
  for (const edit of edits.edits) {
    if (!edit.search || edit.replace === undefined) continue
    
    let matched = false
    
    // 策略1：精确匹配
    if (result.includes(edit.search)) {
      result = result.replace(edit.search, edit.replace)
      matched = true
    }
    
    // 策略2：去除首尾空格匹配
    if (!matched) {
      const trimmedSearch = edit.search.trim()
      if (result.includes(trimmedSearch)) {
        result = result.replace(trimmedSearch, edit.replace.trim())
        matched = true
      }
    }
    
    // 策略3：通过 ID 属性匹配（替换整个 mxCell 元素）
    if (!matched) {
      const idMatch = edit.search.match(/id="([^"]+)"/)
      if (idMatch) {
        const searchId = idMatch[1]
        // 转义 ID 中的特殊字符
        const escapedId = searchId.replace(/[.*+?^${}()|[\]\\]/g, '\\$&')
        // 匹配自闭合或带子元素的 mxCell
        const cellRegex = new RegExp(
          `<mxCell[^>]*id="${escapedId}"[^>]*(?:\\/>|>[\\s\\S]*?<\\/mxCell>)`,
          'g'
        )
        const newResult = result.replace(cellRegex, edit.replace)
        if (newResult !== result) {
          result = newResult
          matched = true
        }
      }
    }
    
    // 策略4：通过 value 属性匹配（替换包含该 value 的 mxCell）
    if (!matched) {
      const valueMatch = edit.search.match(/value="([^"]*)"/)
      if (valueMatch) {
        const searchValue = valueMatch[1]
        // 转义特殊字符
        const escapedValue = searchValue.replace(/[.*+?^${}()|[\]\\]/g, '\\$&')
        const cellRegex = new RegExp(
          `<mxCell[^>]*value="${escapedValue}"[^>]*(?:\\/>|>[\\s\\S]*?<\\/mxCell>)`,
          'g'
        )
        const newResult = result.replace(cellRegex, edit.replace)
        if (newResult !== result) {
          result = newResult
          matched = true
        }
      }
    }
    
    // 策略5：标准化空格匹配
    if (!matched) {
      const normalizeWs = s => s.replace(/\s+/g, ' ').trim()
      const normalizedSearch = normalizeWs(edit.search)
      const normalizedResult = normalizeWs(result)
      
      if (normalizedResult.includes(normalizedSearch)) {
        // 使用正则替换，忽略空格差异
        const regex = new RegExp(
          edit.search
            .replace(/[.*+?^${}()|[\]\\]/g, '\\$&')
            .replace(/\s+/g, '\\s+'),
          'g'
        )
        const newResult = result.replace(regex, edit.replace)
        if (newResult !== result) {
          result = newResult
          matched = true
        }
      }
    }
    
    if (matched) {
      appliedCount++
    } else {
      console.warn('[applyEdits] 未找到匹配:', edit.search.substring(0, 80) + '...')
    }
  }
  
  console.log(`[applyEdits] 应用了 ${appliedCount}/${edits.edits.length} 个编辑`)
  return result
}

/**
 * 保存图表历史
 */
const saveToHistory = (xml, action = 'update') => {
  if (!xml) return
  
  diagramHistory.value.push({
    xml,
    action,
    timestamp: Date.now()
  })
  
  // 限制历史数量
  if (diagramHistory.value.length > MAX_HISTORY) {
    diagramHistory.value.shift()
  }
}

/**
 * 恢复到指定历史版本
 */
const restoreFromHistory = (index) => {
  if (index >= 0 && index < diagramHistory.value.length) {
    const history = diagramHistory.value[index]
    currentDiagramXml.value = history.xml
    if (drawioReady.value) {
      sendToDrawio({ action: 'load', autosave: 0, xml: history.xml })
    }
    ElMessage.success('已恢复到历史版本')
  }
}

/**
 * 提取部分完整的 layers（增量渲染用）
 * 只提取包含实际节点的完整层级
 */
const extractPartialLayers = (content) => {
  const layers = []
  
  // 找到 "layers": [ 的位置
  const layersStart = content.indexOf('"layers"')
  if (layersStart === -1) return layers
  
  const arrayStart = content.indexOf('[', layersStart)
  if (arrayStart === -1) return layers
  
  // 逐个提取完整的 layer 对象
  let pos = arrayStart + 1
  let braceCount = 0
  let layerStart = -1
  let inString = false
  let escaped = false
  
  while (pos < content.length) {
    const char = content[pos]
    
    if (escaped) {
      escaped = false
      pos++
      continue
    }
    if (char === '\\') {
      escaped = true
      pos++
      continue
    }
    if (char === '"') {
      inString = !inString
      pos++
      continue
    }
    
    if (!inString) {
      if (char === '{') {
        if (braceCount === 0) layerStart = pos
        braceCount++
      } else if (char === '}') {
        braceCount--
        if (braceCount === 0 && layerStart !== -1) {
          // 找到一个完整的 layer
          const layerStr = content.substring(layerStart, pos + 1)
          try {
            const layer = JSON.parse(layerStr)
            // 必须有 name，且必须有实际节点（nodes 或 blocks 中有内容）
            const hasNodes = layer.nodes && layer.nodes.length > 0
            const hasBlocks = layer.blocks && layer.blocks.length > 0 && 
                              layer.blocks.some(b => b.nodes && b.nodes.length > 0)
            if (layer.name && (hasNodes || hasBlocks)) {
              layers.push(layer)
            }
          } catch (e) {
            // 解析失败，忽略
          }
          layerStart = -1
        }
      } else if (char === ']' && braceCount === 0) {
        // layers 数组结束
        break
      }
    }
    pos++
  }
  
  return layers
}

// 尝试渲染架构图（支持 XML、JSON、GRAPH_DATA 格式）
const tryRenderJson = () => {
  // 节流：避免频繁检测和渲染
  const now = Date.now()
  if (now - lastTryRenderTime < RENDER_THROTTLE_MS) {
    return
  }
  lastTryRenderTime = now
  
  const content = streamingContent.value
  
  // 检测 [GRAPH_DATA] 格式（ELK 模式）- 不做实时渲染，等 finishMessage 处理
  if (content.includes('[GRAPH_DATA]')) {
    // 只记录日志，最终渲染在 finishMessage 中处理
    if (!lastRenderedJson && content.includes('[/GRAPH_DATA]')) {
      console.log('[Diagram] 检测到完整的 GRAPH_DATA，将在完成时使用 ELK 布局')
    }
    return
  }
  
  // 检测 [DISPLAY_DIAGRAM] 格式
  if (content.includes('[DISPLAY_DIAGRAM]') && content.includes('[/DISPLAY_DIAGRAM]')) {
    const xml = extractDisplayDiagramXml(content)
    if (xml && !lastRenderedJson) {
      console.log('[Diagram] 检测到 DISPLAY_DIAGRAM XML')
      lastRenderedJson = true
      
      if (drawioReady.value) {
        if (renderDebounceTimer) clearTimeout(renderDebounceTimer)
        renderDebounceTimer = setTimeout(() => {
          sendToDrawio({ action: 'load', autosave: 0, xml: xml })
          renderDebounceTimer = null
        }, RENDER_DEBOUNCE_MS)
      }
    }
    return
  }
  
  // 检测 [EDIT_DIAGRAM] 编辑指令
  if (content.includes('[EDIT_DIAGRAM]') && content.includes('[/EDIT_DIAGRAM]')) {
    const edits = extractEditDiagramCommands(content)
    if (edits && !lastRenderedJson) {
      console.log('[Diagram] 检测到 EDIT_DIAGRAM 编辑指令')
      lastRenderedJson = true
      
      const newXml = applyEdits(currentDiagramXml.value, edits)
      if (newXml !== currentDiagramXml.value && drawioReady.value) {
        if (renderDebounceTimer) clearTimeout(renderDebounceTimer)
        renderDebounceTimer = setTimeout(() => {
          sendToDrawio({ action: 'load', autosave: 0, xml: newXml })
          renderDebounceTimer = null
        }, RENDER_DEBOUNCE_MS)
      }
    }
    return
  }
  
  // 兼容旧的 JSON 格式
  const hasNodes = content.includes('"nodes"')
  const hasEdges = content.includes('"edges"') || content.includes('"source"')
  const hasLayout = content.includes('"layout"')
  const isSmartFormat = hasNodes && (hasEdges || hasLayout)
  const isLayered = content.includes('"layers"') && !isSmartFormat
  
  // 旧的分层架构：支持增量渲染
  if (isLayered) {
    const layers = extractPartialLayers(content)
    
    if (layers.length > lastRenderedLayerCount) {
      console.log('[Diagram] 分层增量渲染:', layers.length, '层')
      lastRenderedLayerCount = layers.length
      
      const partialData = { type: 'layered', layers: layers }
      const xml = convertToDrawioXml(partialData)
      
      if (drawioReady.value) {
        if (renderDebounceTimer) clearTimeout(renderDebounceTimer)
        renderDebounceTimer = setTimeout(() => {
          sendToDrawio({ action: 'load', autosave: 0, xml: xml })
          renderDebounceTimer = null
        }, RENDER_DEBOUNCE_MS)
      }
    }
    return
  }
  
  // 新的语义化 JSON 格式
  if (isSmartFormat && isJsonComplete(content)) {
    const jsonData = extractJsonFromContent(content)
    if (jsonData && !lastRenderedJson) {
      console.log('[Diagram] 智能布局渲染，节点数:', jsonData.nodes?.length || 0)
      lastRenderedJson = true
      
      jsonData.type = 'smart'
      const xml = convertToDrawioXml(jsonData)
      if (drawioReady.value) {
        sendToDrawio({ action: 'load', autosave: 0, xml: xml })
      }
    }
  }
}

// 完成消息
const finishMessage = async () => {
  const content = streamingContent.value
  
  console.log('[finishMessage] 开始处理, 内容长度:', content.length)
  
  // 清除防抖定时器
  if (renderDebounceTimer) {
    clearTimeout(renderDebounceTimer)
    renderDebounceTimer = null
  }
  
  let hasRendered = false
  
  // 🔥 优先处理新的 ELK 格式 [GRAPH_DATA]
  if (content.includes('[GRAPH_DATA]') && content.includes('[/GRAPH_DATA]')) {
    console.log('[finishMessage] 检测到 GRAPH_DATA 格式，使用 ELK 布局')
    
    // 先尝试直接提取 JSON
    const graphMatch = content.match(/\[GRAPH_DATA\]([\s\S]*?)\[\/GRAPH_DATA\]/)
    if (!graphMatch) {
      console.warn('[finishMessage] 无法提取 GRAPH_DATA 内容')
    } else {
      console.log('[finishMessage] GRAPH_DATA 内容长度:', graphMatch[1].length)
    }
    
    try {
      const result = await UnifiedProcessor.process(content, currentDiagramXml.value, {
        preserveEdits: editTracker.hasManualEdits()
      })
      
      if (result.success && result.xml) {
        // 保存历史
        if (currentDiagramXml.value) {
          saveToHistory(currentDiagramXml.value, 'before_update')
        }
        
        currentDiagramXml.value = result.xml
        
        // 重置编辑追踪
        editTracker.resetForNewDiagram()
        editTracker.onXmlChanged(result.xml)
        
        // 发送到 Draw.io 渲染
        if (drawioReady.value) {
          sendToDrawio({ action: 'load', autosave: 0, xml: result.xml })
          setTimeout(() => sendToDrawio({ action: 'fit' }), 300)
        }
        ElMessage.success('架构图已渲染 (Draw.io)')
        hasRendered = true
      } else {
        // 如果是格式问题
        console.warn('[finishMessage] ELK 处理失败:', result.error)
        ElMessage.warning('图表数据格式有误，请重试')
      }
    } catch (error) {
      console.error('[finishMessage] ELK 处理异常:', error)
      ElMessage.warning('布局计算失败，请重试')
    }
  }
  
  // 处理现有的 XML 格式 [DISPLAY_DIAGRAM]
  else if (content.includes('[DISPLAY_DIAGRAM]')) {
    const xml = extractDisplayDiagramXml(content)
    if (xml) {
      console.log('[finishMessage] 提取到 DISPLAY_DIAGRAM XML，长度:', xml.length)
      
      // 保存历史
      if (currentDiagramXml.value) {
        saveToHistory(currentDiagramXml.value, 'before_update')
      }
      
      currentDiagramXml.value = xml
      // 重置编辑追踪
      editTracker.resetForNewDiagram()
      editTracker.onXmlChanged(xml)
      
      if (drawioReady.value) {
        sendToDrawio({ action: 'load', autosave: 0, xml: xml })
        // 发送 fit 指令
        setTimeout(() => sendToDrawio({ action: 'fit' }), 300)
        ElMessage.success('架构图已渲染')
      }
      hasRendered = true
    } else {
      console.error('[finishMessage] 无法提取有效的 XML')
      ElMessage.warning('生成的架构图格式有误，请重试')
    }
  }
  
  // 处理编辑指令
  else if (content.includes('[EDIT_DIAGRAM]')) {
    const edits = extractEditDiagramCommands(content)
    if (edits) {
      if (currentDiagramXml.value) {
        console.log('[finishMessage] 应用 EDIT_DIAGRAM 编辑指令，编辑数:', edits.edits?.length || 0)
        
        // 保存历史
        saveToHistory(currentDiagramXml.value, 'before_edit')
        
        const newXml = applyEdits(currentDiagramXml.value, edits)
        if (newXml !== currentDiagramXml.value) {
          currentDiagramXml.value = newXml
          if (drawioReady.value) {
            sendToDrawio({ action: 'load', autosave: 0, xml: newXml })
            ElMessage.success('图表已更新')
          }
          hasRendered = true
        } else {
          ElMessage.warning('编辑未生效，可能是搜索模式不匹配')
        }
      } else {
        ElMessage.warning('当前没有图表，无法应用编辑')
      }
    } else {
      console.error('[finishMessage] 无法解析编辑指令')
      ElMessage.warning('编辑指令格式有误')
    }
  }
  
  // 兼容旧的 JSON 格式
  else {
    const hasGraphData = content.includes('"nodes"') || content.includes('"layers"')
    if (hasGraphData) {
      console.log('[finishMessage] 尝试解析 JSON 格式...')
      const jsonData = extractJsonFromContent(content)
      if (jsonData) {
        const isNewFormat = jsonData.nodes && jsonData.nodes.length > 0
        const isOldFormat = jsonData.layers && jsonData.layers.length > 0
        
        if (isNewFormat || isOldFormat) {
          console.log('[finishMessage] 提取到 JSON，格式:', isNewFormat ? '语义化' : '分层')
          
          if (isNewFormat && !jsonData.type) {
            jsonData.type = 'smart'
          }
          
          // 保存历史
          if (currentDiagramXml.value) {
            saveToHistory(currentDiagramXml.value, 'before_update')
          }
          
          const xml = convertToDrawioXml(jsonData)
          currentDiagramXml.value = xml
          
          // 发送到 Draw.io 渲染
          if (drawioReady.value) {
            sendToDrawio({ action: 'load', autosave: 0, xml: xml })
          }
          ElMessage.success('架构图已渲染')
          hasRendered = true
        }
      }
    }
  }
  
  // 重置状态
  lastRenderedLayerCount = 0
  lastRenderedJson = false
  
  // 如果内容为空，不添加消息
  if (!content || !content.trim()) {
    console.log('[finishMessage] 内容为空，跳过')
    isGenerating.value = false
    streamingContent.value = ''
    return
  }
  
  // 添加助手消息
  messages.value.push({
    role: 'assistant',
    content: content,
    hasXml: hasRendered
  })
  
  isGenerating.value = false
  streamingContent.value = ''
  scrollToBottom()
}

// 格式化消息（支持 Markdown 渲染）
const formatMessage = (text) => {
  if (!text) return ''
  
  let formatted = text
  
  // 移除完整的 GRAPH_DATA 标记
  formatted = formatted.replace(/\[GRAPH_DATA\][\s\S]*?\[\/GRAPH_DATA\]/g, 
    '<div class="xml-rendered">✅ 架构图已渲染</div>')
  
  // 移除完整的 DISPLAY_DIAGRAM 标记
  formatted = formatted.replace(/\[DISPLAY_DIAGRAM\][\s\S]*?\[\/DISPLAY_DIAGRAM\]/g, 
    '<div class="xml-rendered">✅ 架构图已渲染到左侧编辑器</div>')
  
  // 移除完整的 EDIT_DIAGRAM 标记
  formatted = formatted.replace(/\[EDIT_DIAGRAM\][\s\S]*?\[\/EDIT_DIAGRAM\]/g, 
    '<div class="xml-rendered">✅ 图表编辑已应用</div>')
  
  // 检测未完成的 GRAPH_DATA 标记（ELK 模式）
  if (formatted.includes('[GRAPH_DATA]') && !formatted.includes('[/GRAPH_DATA]')) {
    const startIdx = formatted.indexOf('[GRAPH_DATA]')
    const beforeData = formatted.substring(0, startIdx)
    formatted = beforeData + '<div class="xml-generating">⏳ 正在生成图表数据...</div>'
  }
  
  // 检测未完成的 DISPLAY_DIAGRAM 标记
  if (formatted.includes('[DISPLAY_DIAGRAM]') && !formatted.includes('[/DISPLAY_DIAGRAM]')) {
    const startIdx = formatted.indexOf('[DISPLAY_DIAGRAM]')
    const beforeXml = formatted.substring(0, startIdx)
    formatted = beforeXml + '<div class="xml-generating">⏳ 正在生成架构图 XML...</div>'
  }
  
  // 检测未完成的 EDIT_DIAGRAM 标记
  if (formatted.includes('[EDIT_DIAGRAM]') && !formatted.includes('[/EDIT_DIAGRAM]')) {
    const startIdx = formatted.indexOf('[EDIT_DIAGRAM]')
    const beforeEdit = formatted.substring(0, startIdx)
    formatted = beforeEdit + '<div class="xml-generating">⏳ 正在生成编辑指令...</div>'
  }
  
  // 兼容旧的 JSON 代码块
  formatted = formatted.replace(/```json[\s\S]*?```/g, 
    '<div class="xml-rendered">✅ 架构图已渲染到左侧编辑器</div>')
  
  // 检测未完成的 JSON 代码块（正在生成中）
  if (formatted.includes('```json') && !formatted.includes('```', formatted.indexOf('```json') + 7)) {
    const startIdx = formatted.indexOf('```json')
    const beforeJson = formatted.substring(0, startIdx)
    formatted = beforeJson + '<div class="xml-generating">⏳ 正在生成架构图数据...</div>'
  }
  
  // Markdown 转换
  formatted = formatted.replace(/^#{4,} (.*?)$/gm, '<h5 class="md-h5">$1</h5>')
  formatted = formatted.replace(/^### (.*?)$/gm, '<h4 class="md-h4">$1</h4>')
  formatted = formatted.replace(/^## (.*?)$/gm, '<h3 class="md-h3">$1</h3>')
  formatted = formatted.replace(/^# (.*?)$/gm, '<h2 class="md-h2">$1</h2>')
  
  // 列表项
  formatted = formatted.replace(/^- (.*?)$/gm, '<li class="md-li">$1</li>')
  formatted = formatted.replace(/^\* (.*?)$/gm, '<li class="md-li">$1</li>')
  formatted = formatted.replace(/^(\d+)\. (.*?)$/gm, '<li class="md-li-num">$2</li>')
  
  // 内联样式
  formatted = formatted
    .replace(/\*\*(.*?)\*\*/g, '<strong>$1</strong>')
    .replace(/\*(.*?)\*/g, '<em>$1</em>')
    .replace(/`([^`]+)`/g, '<code class="md-code">$1</code>')
  
  // 换行
  formatted = formatted.replace(/\n/g, '<br>')
  
  // 清理多余的 <br>
  formatted = formatted.replace(/<\/h(\d)><br>/g, '</h$1>')
  formatted = formatted.replace(/<\/li><br>/g, '</li>')
  
  return formatted
}

// 用户是否手动滚动了
let userScrolled = false

// 滚动到底部（只有用户在底部附近时才自动滚动）
const scrollToBottom = async (force = false) => {
  await nextTick()
  if (messageList.value) {
    const el = messageList.value
    const isNearBottom = el.scrollHeight - el.scrollTop - el.clientHeight < 100
    
    // 只有强制滚动、用户在底部附近、或用户未手动滚动时才自动滚动
    if (force || isNearBottom || !userScrolled) {
      el.scrollTop = el.scrollHeight
      userScrolled = false
    }
  }
}

// 监听用户滚动
const handleMessageListScroll = () => {
  if (messageList.value) {
    const el = messageList.value
    const isNearBottom = el.scrollHeight - el.scrollTop - el.clientHeight < 100
    userScrolled = !isNearBottom
  }
}

// Draw.io 消息处理
const handleDrawioMessage = (event) => {
  const isAllowed = DRAWIO_ALLOWED_ORIGINS.some(origin => event.origin.startsWith(origin))
  if (!isAllowed) return
  
  try {
    const msg = JSON.parse(event.data)
    
    if (msg.event === 'init') {
      drawioReady.value = true
      // 如果已有架构图，加载它；否则加载空白图
      if (currentDiagramXml.value) {
        sendToDrawio({
          action: 'load',
          autosave: 0,
          xml: currentDiagramXml.value
        })
      } else {
        sendToDrawio({
          action: 'load',
          xml: '<mxGraphModel><root><mxCell id="0"/><mxCell id="1" parent="0"/></root></mxGraphModel>'
        })
      }
    }
    
    if (msg.event === 'save') {
      ElMessage.success('保存成功')
    }
    
    // 处理自动保存事件，同步 XML 到状态
    if (msg.event === 'autosave' && msg.xml) {
      currentDiagramXml.value = msg.xml
      // 追踪用户编辑
      editTracker.onXmlChanged(msg.xml)
      console.log('[Draw.io] 自动同步 XML')
    }
    
    // 处理导出事件
    if (msg.event === 'export' && msg.data) {
      currentDiagramXml.value = msg.data
      console.log('[Draw.io] 导出 XML 已同步')
    }
  } catch (e) {
    // 忽略非 JSON 消息
  }
}

// 发送消息到 Draw.io
const sendToDrawio = (msg) => {
  if (drawioFrame.value?.contentWindow) {
    try {
      drawioFrame.value.contentWindow.postMessage(JSON.stringify(msg), '*')
    } catch (e) {
      console.warn('[Draw.io] 发送消息失败:', e.message)
    }
  } else {
    console.warn('[Draw.io] iframe 未就绪')
  }
}

// 生命周期
onMounted(() => {
  window.addEventListener('message', handleDrawioMessage)
})

onUnmounted(() => {
  window.removeEventListener('message', handleDrawioMessage)
  // 清理拖拽事件
  stopResize()
  // 清理渲染防抖定时器
  if (renderDebounceTimer) {
    clearTimeout(renderDebounceTimer)
    renderDebounceTimer = null
  }
})
</script>

<style scoped>
.diagram-chat {
  display: flex;
  min-height: calc(100vh - 84px);
  background: #f5f5f5;
}

/* 左侧编辑器面板 */
.editor-panel {
  flex: 1;
  display: flex;
  flex-direction: column;
  min-width: 0;
  overflow: hidden;
  position: relative;
}

.drawio-frame {
  flex: 1;
  width: 100%;
  border: none;
}

/* 拖拽时的遮罩层 */
.iframe-overlay {
  position: absolute;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background: transparent;
  z-index: 1000;
  cursor: col-resize;
}

/* 可拖拽分隔条 */
.resize-handle {
  width: 10px;
  background: #e0e0e0;
  cursor: col-resize;
  transition: all 0.2s;
  flex-shrink: 0;
  position: relative;
  display: flex;
  align-items: center;
  justify-content: center;
}

.resize-handle:hover {
  background: #667eea;
}

.resize-handle:active {
  background: #5a67d8;
}

.resize-handle-icon {
  width: 4px;
  height: 50px;
  background: rgba(0, 0, 0, 0.15);
  border-radius: 2px;
  transition: background 0.2s;
}

.resize-handle:hover .resize-handle-icon {
  background: rgba(255, 255, 255, 0.6);
}

/* 右侧聊天面板 */
.chat-panel {
  display: flex;
  flex-direction: column;
  background: #fff;
  flex-shrink: 0;
  border-left: 1px solid #e0e0e0;
}

.chat-header {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 12px 16px;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  color: #fff;
  font-weight: 600;
}

/* 消息列表 */
.message-list {
  flex: 1;
  overflow-y: auto;
  padding: 16px;
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.message {
  display: flex;
  gap: 12px;
}

.message.user {
  flex-direction: row-reverse;
}

.message-avatar {
  width: 36px;
  height: 36px;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
}

.message.assistant .message-avatar {
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  color: #fff;
}

.message.user .message-avatar {
  background: #e8f0fe;
  color: #1a73e8;
}

.message-content {
  max-width: 85%;
}

.message-text {
  padding: 12px 16px;
  border-radius: 12px;
  line-height: 1.6;
  font-size: 14px;
}

.message.assistant .message-text {
  background: #f5f5f5;
  color: #333333;
  border-top-left-radius: 4px;
}

.message.user .message-text {
  background: #e8f0fe;
  color: #1a73e8;
  border-top-right-radius: 4px;
}

.message-text ul {
  margin: 8px 0 0 0;
  padding-left: 20px;
}

.message-text li {
  margin: 4px 0;
}

.message-text code {
  background: #e8e8e8;
  padding: 2px 6px;
  border-radius: 4px;
  font-family: monospace;
}

/* 欢迎消息样式 */
.welcome-text {
  padding: 16px !important;
}

.welcome-title {
  font-size: 16px;
  font-weight: 600;
  margin-bottom: 8px;
  color: #1a1a1a;
}

.welcome-text p {
  margin: 0 0 12px 0;
  color: #666;
}

.welcome-section {
  margin: 12px 0;
}

.section-title {
  font-size: 13px;
  font-weight: 600;
  color: #333;
  margin-bottom: 8px;
}

.welcome-text ul {
  margin: 0;
  padding-left: 18px;
}

.welcome-text li {
  margin: 4px 0;
  font-size: 13px;
  color: #555;
}

.welcome-text li strong {
  color: #667eea;
}

.example-list {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}

.example-item {
  display: inline-block;
  padding: 4px 10px;
  background: #f0f4ff;
  border: 1px solid #d4ddff;
  border-radius: 12px;
  font-size: 12px;
  color: #667eea;
  cursor: pointer;
  transition: all 0.2s;
}

.example-item:hover {
  background: #e0e7ff;
  border-color: #667eea;
}

.welcome-tip {
  margin-top: 12px;
  padding: 8px 12px;
  background: #fef3c7;
  border-radius: 6px;
  font-size: 12px;
  color: #92400e;
}

.welcome-tip code {
  background: #fff;
  border: 1px solid #fcd34d;
}

.render-status {
  display: flex;
  align-items: center;
  gap: 6px;
  margin-top: 8px;
  font-size: 12px;
  color: #52c41a;
}

/* XML 渲染状态 */
.xml-rendered {
  margin: 12px 0;
  padding: 10px 14px;
  background: #f0fdf4;
  border: 1px solid #86efac;
  border-radius: 8px;
  color: #16a34a;
  font-size: 13px;
}

.xml-generating {
  margin: 12px 0;
  padding: 10px 14px;
  background: #fef3c7;
  border: 1px solid #fcd34d;
  border-radius: 8px;
  color: #d97706;
  font-size: 13px;
  animation: pulse 1.5s infinite;
}

@keyframes pulse {
  0%, 100% { opacity: 1; }
  50% { opacity: 0.6; }
}

/* Markdown 样式 */
.message-content :deep(.md-h2) {
  font-size: 16px;
  font-weight: 600;
  color: #1a1a1a;
  margin: 12px 0 8px 0;
  padding-bottom: 4px;
  border-bottom: 1px solid #eee;
}

.message-content :deep(.md-h3) {
  font-size: 14px;
  font-weight: 600;
  color: #333;
  margin: 10px 0 6px 0;
}

.message-content :deep(.md-h4) {
  font-size: 13px;
  font-weight: 600;
  color: #444;
  margin: 8px 0 4px 0;
}

.message-content :deep(.md-h5) {
  font-size: 12px;
  font-weight: 600;
  color: #555;
  margin: 6px 0 4px 0;
}

.message-content :deep(.md-li) {
  display: block;
  padding-left: 16px;
  margin: 4px 0;
  position: relative;
}

.message-content :deep(.md-li)::before {
  content: "•";
  position: absolute;
  left: 4px;
  color: #667eea;
}

.message-content :deep(.md-li-num) {
  display: block;
  padding-left: 20px;
  margin: 4px 0;
}

.message-content :deep(.md-code) {
  background: #f0f0f0;
  padding: 2px 6px;
  border-radius: 4px;
  font-family: 'Monaco', 'Menlo', monospace;
  font-size: 12px;
  color: #333333;
  font-weight: 500;
}

/* 确保消息内容默认黑色 */
.message-content {
  color: #333333 !important;
}

.message-content * {
  color: inherit;
}

.message-content strong {
  color: #1a1a1a;
}

.message-content code {
  color: #333333;
}

/* XML 渲染状态提示 */
.message-content :deep(.xml-rendered) {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  padding: 8px 12px;
  background: linear-gradient(135deg, #f0fff4 0%, #e6ffed 100%);
  border: 1px solid #52c41a;
  border-radius: 8px;
  color: #389e0d;
  font-size: 13px;
  font-weight: 500;
  margin: 8px 0;
}

.message-content :deep(.xml-generating) {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  padding: 8px 12px;
  background: linear-gradient(135deg, #fff7e6 0%, #fffbe6 100%);
  border: 1px solid #faad14;
  border-radius: 8px;
  color: #d48806;
  font-size: 13px;
  font-weight: 500;
  margin: 8px 0;
  animation: pulse 1.5s ease-in-out infinite;
}

@keyframes pulse {
  0%, 100% { opacity: 1; }
  50% { opacity: 0.7; }
}

.typing-cursor {
  animation: blink 1s infinite;
}

@keyframes blink {
  0%, 50% { opacity: 1; }
  51%, 100% { opacity: 0; }
}

/* 输入区域 */
.input-area {
  position: relative;
  padding: 16px;
  border-top: 1px solid #e0e0e0;
  background: #fafafa;
}

.action-bar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 12px;
  gap: 8px;
}

.action-bar .tips {
  display: flex;
  align-items: center;
  gap: 6px;
  font-size: 11px;
  color: #999;
  flex: 1;
  min-width: 0;
  overflow: hidden;
}

.action-bar .tips span {
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.action-bar .tips i {
  color: #faad14;
  flex-shrink: 0;
}

.action-btn {
  display: flex;
  align-items: center;
  gap: 4px;
  padding: 5px 10px;
  border-radius: 14px;
  font-size: 12px;
  cursor: pointer;
  transition: all 0.2s;
  background: #fff;
  border: 1px solid #e0e0e0;
  color: #999;
  white-space: nowrap;
  flex-shrink: 0;
}

.action-btn span {
  display: inline;
}

.action-buttons {
  display: flex;
  gap: 6px;
  flex-shrink: 0;
}

.action-btn.history-btn:hover:not(.disabled) {
  border-color: #667eea;
  color: #667eea;
}

.action-btn.clear-btn:hover:not(.disabled) {
  border-color: #F5222D;
  color: #F5222D;
}

.action-btn.disabled {
  opacity: 0.5;
  cursor: not-allowed;
}

/* 历史版本面板 */
.history-panel {
  position: absolute;
  bottom: 100%;
  left: 16px;
  right: 16px;
  background: #fff;
  border: 1px solid #e0e0e0;
  border-radius: 8px;
  box-shadow: 0 -4px 12px rgba(0, 0, 0, 0.1);
  margin-bottom: 8px;
  max-height: 200px;
  overflow: hidden;
  z-index: 100;
}

.history-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 10px 12px;
  background: #f8f8f8;
  border-bottom: 1px solid #e0e0e0;
  font-size: 13px;
  font-weight: 500;
}

.history-header i {
  cursor: pointer;
  color: #999;
  padding: 4px;
}

.history-header i:hover {
  color: #666;
}

.history-list {
  max-height: 150px;
  overflow-y: auto;
}

.history-item {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 10px 12px;
  cursor: pointer;
  transition: background 0.2s;
  border-bottom: 1px solid #f0f0f0;
}

.history-item:last-child {
  border-bottom: none;
}

.history-item:hover {
  background: #f0f5ff;
}

.history-item i {
  color: #667eea;
  font-size: 14px;
}

.history-item span {
  font-size: 13px;
  color: #333;
}

.history-item small {
  margin-left: auto;
  font-size: 11px;
  color: #999;
}

.input-box {
  display: flex;
  gap: 8px;
}

.input-box :deep(.el-textarea__inner) {
  border-radius: 8px;
  resize: none;
}

.send-btn {
  align-self: flex-end;
  height: 54px;
  width: 54px;
  border-radius: 8px;
}

.input-tip {
  margin-top: 8px;
  font-size: 12px;
  color: #999;
  text-align: right;
}
</style>
