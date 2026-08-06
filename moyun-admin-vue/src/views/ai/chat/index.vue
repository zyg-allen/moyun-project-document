<template>
  <div :class="['chat-page', { 'is-mobile': isMobile }]">
    <div class="chat-header">
      <div class="header-left">
        <!-- H5模式显示菜单按钮 -->
        <el-button v-if="isMobile" class="menu-btn" @click="showSidebar = !showSidebar" text>
          <i class="fa-solid fa-bars"></i>
        </el-button>
        <h2>💬 智能对话</h2>
        <div class="current-agent" v-if="currentAgent && !isMobile">
          <i class="fa-solid fa-robot"></i>
          <span class="agent-name">{{ currentAgent.name }}</span>
        </div>
      </div>
      <div class="header-actions">
        <el-tooltip content="清空当前对话" placement="bottom" v-if="messages.length > 0">
          <el-button @click="clearCurrentChat" :size="isMobile ? 'small' : 'default'">
            <i class="fa-solid fa-eraser"></i>
          </el-button>
        </el-tooltip>
        <el-button @click="createNewConversation" type="primary" :size="isMobile ? 'small' : 'default'">
          <i class="fa-solid fa-plus"></i>
          <span v-if="!isMobile">&nbsp;新会话</span>
        </el-button>
      </div>
    </div>
    <div class="chat-main">
      <!-- H5模式遮罩层 -->
      <div v-if="isMobile && showSidebar" class="sidebar-overlay" @click="showSidebar = false"></div>
      
      <!-- 左侧会话列表 -->
      <div :class="['conversation-sidebar', { 'sidebar-open': showSidebar }]">
        <div class="sidebar-header">
          <h3>历史会话</h3>
        </div>
        <div class="conversation-list">
          <div
            v-for="conv in conversations"
            :key="conv.id"
            :class="['conversation-item', { active: conv.id === currentConversationId }]"
            @click="switchConversation(conv.id); if(isMobile) showSidebar = false"
          >
            <div class="conversation-title">
              <i class="fa-solid fa-message"></i>
              {{ conv.title }}
            </div>
            <div class="conversation-meta">
              <span class="message-count">
                <i class="fa-solid fa-comments"></i>
                {{ conv.messageCount || 0 }}条
              </span>
              <el-button
                size="small"
                text
                @click.stop="deleteConversation(conv.id)"
                class="delete-btn"
              >
                <i class="fa-solid fa-trash"></i>
              </el-button>
            </div>
          </div>
          <div v-if="conversations.length === 0" class="empty-state">
            <i class="fa-solid fa-inbox"></i>
            <p>暂无历史会话</p>
          </div>
        </div>
      </div>
      
      <!-- 右侧对话区域 -->
      <div 
        class="chat-container" 
        @dragover.prevent="onDragOver"
        @dragleave="onDragLeave"
        @drop.prevent="onDrop"
        :class="{ 'drag-over': isDragging }"
      >
        <div class="message-list" ref="messaggListRef" @scroll="onScroll">
          <!-- 开场白和预设问题（仅在没有消息且不在加载中时显示） -->
          <div v-if="messages.length === 0 && currentAgent && !isLoadingMessages" class="welcome-section">
            <div class="welcome-avatar">
              <i class="fa-solid fa-robot"></i>
            </div>
            <div class="welcome-content">
              <h3>{{ currentAgent.name }}</h3>
              <p class="welcome-message">{{ currentAgent.welcomeMessage || '你好！我是你的智能助手，有什么可以帮助你的吗？' }}</p>
              <div v-if="suggestedQuestions.length > 0" class="suggested-questions">
                <p class="suggested-title">你可以这样问我：</p>
                <div class="question-list">
                  <button 
                    v-for="(q, idx) in suggestedQuestions" 
                    :key="idx" 
                    class="question-btn"
                    @click="askSuggestedQuestion(q)"
                  >
                    <i class="fa-solid fa-lightbulb"></i>
                    {{ q }}
                  </button>
                </div>
              </div>
            </div>
          </div>
          
          <div
            v-for="(message, index) in messages"
            :key="index"
            :class="
              message.isUser ? 'message user-message' : 'message bot-message'
            "
          >
            <!-- 会话图标 -->
            <i
              :class="
                message.isUser
                  ? 'fa-solid fa-user message-icon'
                  : 'fa-solid fa-robot message-icon'
              "
            ></i>
            <!-- 会话内容 -->
            <div class="message-content">
              <div
                v-if="!message.isUser"
                class="markdown-body"
                v-html="renderMarkdown(message.content)"
              ></div>
              <div v-else class="user-text">
                {{ message.content }}
                <!-- 显示用户上传的图片 -->
                <div v-if="message.images && message.images.length > 0" class="user-images">
                  <img 
                    v-for="(img, imgIdx) in message.images" 
                    :key="imgIdx" 
                    :src="img" 
                    class="user-uploaded-image"
                    @click="previewImage(img)"
                  />
                </div>
              </div>
              
              <!-- 参考来源按钮（历史消息，与实时回答格式一致） -->
              <div v-if="showCitationsEnabled && !message.isUser && message.referenceSources && message.referenceSources.length > 0" class="references-section" style="margin-top: 16px; padding-top: 12px; border-top: 1px solid #e4e7ed;">
                <span style="color: #909399; font-size: 13px; margin-right: 8px;">📚 参考来源：</span>
                <span
                  v-for="(source, idx) in message.referenceSources"
                  :key="idx"
                  style="display: inline-block; margin: 0 6px 6px 0;"
                >
                  <button
                    class="reference-source-btn"
                    style="padding: 4px 12px; font-size: 13px; color: #409eff; background: #ecf5ff; border: 1px solid #b3d8ff; border-radius: 4px 0 0 4px; cursor: pointer; transition: all 0.3s;"
                    :data-index="idx + 1"
                    :data-type="source.type || 'text'"
                    :data-text="(source.text || '').replace(/\n/g, '\\n')"
                    :data-filename="source.fileName"
                    :data-filetype="source.fileType || ''"
                    :data-segment="source.segmentIndex || '0'"
                    :data-kb-id="source.knowledgeBaseId || ''"
                    :data-page="source.pageNumber"
                    :data-image-path="source.imagePath || ''"
                    @click="handleReferenceClick($event.target)"
                    @mouseover="$event.target.style.background='#409eff'; $event.target.style.color='white';"
                    @mouseout="$event.target.style.background='#ecf5ff'; $event.target.style.color='#409eff';"
                  >
                    来源{{ idx + 1 }}
                  </button>
                  <button
                    class="feedback-btn"
                    style="padding: 4px 8px; margin-left: -1px; font-size: 12px; color: #909399; background: #f5f7fa; border: 1px solid #dcdfe6; border-radius: 0 4px 4px 0; cursor: pointer; transition: all 0.3s;"
                    @mouseover="$event.target.style.background='#f56c6c'; $event.target.style.color='white'; $event.target.style.borderColor='#f56c6c';"
                    @mouseout="$event.target.style.background='#f5f7fa'; $event.target.style.color='#909399'; $event.target.style.borderColor='#dcdfe6';"
                    title="标记此来源不准确"
                  >
                    <i class="fa-regular fa-thumbs-down"></i>
                  </button>
                </span>
              </div>
              
              <!-- loading -->
              <span
                class="loading-dots"
                v-if="message.isThinking || message.isTyping"
              >
                <span class="dot"></span>
                <span class="dot"></span>
              </span>
              
              <!-- AI消息操作按钮 -->
              <div v-if="!message.isUser && !message.isTyping && message.content" class="message-actions" style="margin-top: 8px; display: flex; gap: 8px;">
                <el-button 
                  size="small" 
                  text 
                  @click="copyMessage(message.content)"
                  title="复制内容"
                >
                  <i class="fa-regular fa-copy"></i>
                </el-button>
                <el-button 
                  size="small" 
                  text 
                  @click="regenerateMessage(index)"
                  title="重新生成"
                  :disabled="isSending"
                >
                  <i class="fa-solid fa-rotate"></i>
                </el-button>
              </div>
            </div>
          </div>
        </div>
        <div class="input-container">
          <!-- 图片预览区域 -->
          <div v-if="uploadedImages.length > 0" class="image-preview-area">
            <div v-for="(img, idx) in uploadedImages" :key="idx" class="preview-item">
              <img :src="img.url" :alt="img.name" @click="previewImage(img.url)" />
              <el-button class="remove-btn" size="small" circle @click.stop="removeImage(idx)">
                <i class="fa-solid fa-xmark"></i>
              </el-button>
            </div>
          </div>
          <div class="input-row">
            <el-input
              v-model="inputMessage"
              type="textarea"
              :rows="2"
              :placeholder="isSending ? 'AI正在回答中...' : '输入消息，支持粘贴图片'"
              @keyup.enter.exact="sendMessage"
              @paste="handlePaste"
              class="message-input"
              resize="none"
            ></el-input>
            <!-- 右侧操作区 -->
            <div class="input-actions-right">
              <!-- 图片上传按钮 -->
              <el-tooltip content="上传图片" placement="top">
                <el-button @click="triggerImageUpload" :disabled="isSending" class="upload-btn">
                  <i class="fa-solid fa-image"></i>
                </el-button>
              </el-tooltip>
              <input 
                type="file" 
                ref="imageInputRef" 
                accept="image/*" 
                multiple 
                @change="handleImageSelect" 
                style="display: none"
              />
              <!-- 发送/停止按钮 -->
              <el-button
                v-if="!isSending"
                @click="sendMessage"
                type="primary"
                class="send-btn"
              >
                <i class="fa-solid fa-paper-plane"></i>
                发送
              </el-button>
              <el-button
                v-else
                @click="stopGeneration"
                type="danger"
                class="stop-btn"
              >
                <i class="fa-solid fa-stop"></i>
                停止
              </el-button>
            </div>
          </div>
        </div>
    </div>
    </div>

    <!-- 图片预览弹窗 -->
    <el-dialog
      v-model="showImagePreview"
      :show-close="true"
      :close-on-click-modal="true"
      :modal="true"
      class="image-preview-dialog"
      destroy-on-close
    >
      <img :src="previewImageUrl" alt="预览图片" class="preview-full-image" @click="showImagePreview = false" />
    </el-dialog>

    <!-- 文件预览弹窗 -->
    <el-dialog
      v-model="showFileDialog"
      :title="`📄 原文文档：${currentReference.fileName}`"
      :width="isMobile ? '100%' : '900px'"
      :class="['file-preview-dialog', { 'mobile-dialog': isMobile }]"
      :close-on-click-modal="false"
      :modal="true"
      :append-to-body="true"
      destroy-on-close
      :top="isMobile ? '0' : '1vh'"
      :fullscreen="isMobile"
    >
      <div class="file-preview-container" v-loading="isLoadingFile" element-loading-text="加载文件中...">
        <div class="preview-tips">
          <div class="tips-left">
            <i class="fa-solid fa-info-circle"></i>
            <span v-if="currentReference.type === 'image'">引用位置：第 {{ currentReference.pageNumber }} 页（图片）</span>
            <span v-else>引用位置：第 {{ currentReference.segment || currentReference.pageNumber }} 段</span>
            <span v-if="fileType === 'pdf'" class="tip-badge">已跳转到第 {{ currentReference.pageNumber || currentReference.segment }} 页</span>
          </div>
          <div class="tips-center">
            <el-button-group size="small">
              <el-button @click="prevPage" :disabled="currentPage <= 1">
                <i class="fa-solid fa-chevron-left"></i>
              </el-button>
              <el-button disabled>
                {{ currentPage }} / {{ totalPages }}
              </el-button>
              <el-button @click="nextPage" :disabled="currentPage >= totalPages">
                <i class="fa-solid fa-chevron-right"></i>
              </el-button>
            </el-button-group>
          </div>
          <div class="tips-right">
            <el-button size="small" @click="refreshFile" :loading="isLoadingFile">
              <i class="fa-solid fa-refresh"></i> 刷新
            </el-button>
          </div>
        </div>
       <div class="pdf-preview-wrapper">
         <!-- PDF 预览区域 -->
         <div class="pdf-viewer">
           <div class="pdf-container" v-if="filePreviewUrl">
             <vue-pdf-embed
               :source="filePreviewUrl"
               :page="currentPage"
               @loaded="onPdfLoaded"
               @loading-failed="onFileError"
               class="pdf-canvas"
             />
           </div>
           <div v-else class="empty-state">
             <el-empty description="无法预览此文件，请刷新重试" />
           </div>
         </div>
       </div>
      </div>
      <template #footer>
        <div class="dialog-footer">
          <el-button @click="openInNewTab" size="large">
            <i class="fa-solid fa-external-link-alt"></i> 在新标签页打开
          </el-button>
          <el-button type="primary" @click="showFileDialog = false" size="large">
            <i class="fa-solid fa-xmark"></i> 关闭
          </el-button>
        </div>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import cache from '@/plugins/cache'

import { onMounted, onBeforeUnmount, ref, watch, computed, onUnmounted, nextTick } from 'vue'
import { useRoute } from 'vue-router'
import request from '@/utils/request'
import { fetchStream } from '@/utils/stream'
import { v4 as uuidv4 } from 'uuid'
import { marked } from 'marked'
import hljs from 'highlight.js'
import 'highlight.js/styles/github-dark.css'
import { ElMessage, ElMessageBox } from 'element-plus'
import VuePdfEmbed from 'vue-pdf-embed'

const route = useRoute()

// ==================== 响应式布局 ====================
const isMobile = ref(false)
const showSidebar = ref(false)  // H5模式下控制侧边栏显示

// 检测设备类型
const checkMobile = () => {
  isMobile.value = window.innerWidth <= 768
  // PC端默认显示侧边栏
  if (!isMobile.value) {
    showSidebar.value = false
  }
}

const messaggListRef = ref()
const isSending = ref(false)
const uuid = ref()
const inputMessage = ref('')
const messages = ref([])
const isLoadingMessages = ref(false)  // 加载消息中，避免欢迎页闪烁
const agentList = ref([])
// 多模态支持 - 图片上传
const uploadedImages = ref([])  // { url: base64, name: string, file: File }
const imageInputRef = ref(null)
const isDragging = ref(false)  // 拖拽状态
const selectedAgentId = ref(null)
const baseUuid = ref()  // 基础UUID
const conversations = ref([])  // 会话列表
const currentConversationId = ref(null)  // 当前会话ID

// 当前智能体对象
const currentAgent = computed(() => {
  return agentList.value.find(agent => agent.id === selectedAgentId.value)
})

// 预设问题列表
const suggestedQuestions = computed(() => {
  if (!currentAgent.value?.suggestedQuestions) return []
  try {
    return JSON.parse(currentAgent.value.suggestedQuestions)
  } catch {
    return []
  }
})

// 是否显示引用来源
const showCitationsEnabled = computed(() => {
  return currentAgent.value?.showCitations !== false
})

// 点击预设问题
const askSuggestedQuestion = (question) => {
  inputMessage.value = question
  sendMessage()
}

const isUserAtBottom = ref(true)  // 用户是否在底部
const currentReference = ref({
  index: '',
  text: '',
  fileName: '',
  segment: '',
  kbId: ''
})
const showFileDialog = ref(false)
const filePreviewUrl = ref('')
const fileType = ref('')
const isLoadingFile = ref(false)
const currentPage = ref(1)
const totalPages = ref(0)

// 配置 marked
const renderer = new marked.Renderer()
const originalCodeRenderer = renderer.code.bind(renderer)

renderer.code = function(code, language) {
  const validLanguage = language && hljs.getLanguage(language) ? language : 'plaintext'
  const highlightedCode = hljs.highlight(code, { language: validLanguage }).value

  return `
    <div class="code-block-wrapper">
      <div class="code-block-header">
        <span class="code-language">${validLanguage}</span>
        <button class="copy-code-btn" onclick="copyCode(this)" title="复制代码">
          <i class="fa-regular fa-copy"></i> 复制代码
        </button>
      </div>
      <pre><code class="hljs language-${validLanguage}">${highlightedCode}</code></pre>
    </div>
  `
}

marked.setOptions({
  renderer: renderer,
  breaks: true,
  gfm: true,
  sanitize: false,  // 允许 HTML 标签
  mangle: false,
  headerIds: false
})

// 全局显示参考资料函数
window.showReferenceSource = function(button) {
  const index = button.getAttribute('data-index')
  const type = button.getAttribute('data-type') || 'text'
  const text = button.getAttribute('data-text')?.replace(/\\n/g, '\n') || ''
  const fileName = button.getAttribute('data-filename')
  const fileType = button.getAttribute('data-filetype')
  const segment = button.getAttribute('data-segment')
  const kbId = button.getAttribute('data-kb-id')
  const pageNumber = button.getAttribute('data-page')
  const lineStart = button.getAttribute('data-line-start')
  const lineEnd = button.getAttribute('data-line-end')
  const imagePath = button.getAttribute('data-image-path')

  // 触发 Vue 组件的事件
  const event = new CustomEvent('show-reference', {
    detail: { 
      index,
      type,
      text, 
      fileName, 
      fileType,
      segment, 
      kbId,
      pageNumber,
      lineStart,
      lineEnd,
      imagePath
    }
  })
  window.dispatchEvent(event)
}

// 全局提交反馈函数
window.submitFeedback = async function(button, feedbackType) {
  const kbId = button.getAttribute('data-kb-id')
  const fileName = button.getAttribute('data-filename')
  const pageNumber = button.getAttribute('data-page')
  const segment = button.getAttribute('data-segment')
  const rerankScore = button.getAttribute('data-rerank-score')
  const vectorScore = button.getAttribute('data-vector-score')
  
  // 获取当前查询（从最后一条用户消息中获取）
  const messagesDiv = document.querySelector('.message-list')
  const userMessages = messagesDiv.querySelectorAll('.message.user')
  const lastUserMessage = userMessages[userMessages.length - 1]
  const userQuery = lastUserMessage ? lastUserMessage.querySelector('.message-content').textContent : ''
  
  // 获取当前agentId和memoryId
  const event = new CustomEvent('get-chat-context')
  window.dispatchEvent(event)
  
  try {
    const response = await request({ url: '/cms/ai/reference-feedback/reference', method: 'post', data: {
      knowledgeBaseId: parseInt(kbId),
      fileName: fileName,
      pageNumber: pageNumber ? parseInt(pageNumber) : null,
      segmentIndex: segment ? parseInt(segment) : null,
      userQuery: userQuery,
      rerankScore: rerankScore ? parseFloat(rerankScore) : null,
      vectorScore: vectorScore ? parseFloat(vectorScore) : null,
      feedbackType: feedbackType,
      agentId: window.currentAgentId || null,
      memoryId: window.currentMemoryId || null
    }})
    
    // 改变按钮样式表示已反馈
    button.innerHTML = '<i class="fa-solid fa-check"></i>'
    button.disabled = true
    button.style.background = '#67c23a'
    button.style.color = 'white'
    button.style.borderColor = '#67c23a'
    button.style.cursor = 'not-allowed'

    ElMessage.success('感谢反馈！我们会持续优化')
  } catch (error) {
    console.error('提交反馈失败:', error)
    ElMessage.error('提交失败，请稍后重试')
  }
}

// 全局复制代码函数
window.copyCode = function(button) {
  const codeBlock = button.closest('.code-block-wrapper').querySelector('code')
  const code = codeBlock.textContent

  navigator.clipboard.writeText(code).then(() => {
    const originalText = button.innerHTML
    button.innerHTML = '<i class="fa-solid fa-check"></i> 已复制'
    button.classList.add('copied')

    setTimeout(() => {
      button.innerHTML = originalText
      button.classList.remove('copied')
    }, 2000)
  }).catch(err => {
    console.error('复制失败:', err)
  })
}

// 渲染 Markdown
const renderMarkdown = (content) => {
  if (!content) return ''
  try {
    const html = marked.parse(content)
    // 添加点击事件监听
    setTimeout(() => {
      attachReferenceClickHandlers()
    }, 100)
    return html
  } catch (e) {
    console.error('Markdown 渲染错误:', e)
    return content
  }
}

// 附加参考资料点击事件
const attachReferenceClickHandlers = () => {
  const tags = document.querySelectorAll('.reference-tag')
  tags.forEach(tag => {
    if (!tag.hasAttribute('data-click-attached')) {
      tag.addEventListener('click', (e) => {
        e.preventDefault()
        const target = e.currentTarget
        currentReference.value = {
          index: target.getAttribute('data-index'),
          text: target.getAttribute('data-text').replace(/\\n/g, '\n'),
          fileName: target.getAttribute('data-filename'),
          segment: target.getAttribute('data-segment')
        }
        showReferenceDialog.value = true
      })
      tag.setAttribute('data-click-attached', 'true')
    }
  })
}

// 加载智能体列表
const loadAgents = async () => {
  try {
    const response = await request({ url: '/cms/ai/chat/agents', method: 'get' })
    // 后端返回的是 ListResponse 格式：{ list: [], total: n }
    agentList.value = response.data?.list || []

    // 检查URL参数中是否指定了智能体ID
    const urlAgentId = route.query.agentId

    if (urlAgentId) {
      // 使用URL指定的智能体
      const agentIdNum = parseInt(urlAgentId)
      const agent = agentList.value.find(a => a.id === agentIdNum)

      if (agent) {
        selectedAgentId.value = agentIdNum
        console.log('从URL参数加载智能体:', agentIdNum)
        await loadConversations()
      } else {
        ElMessage.warning('指定的智能体不存在，已切换到默认智能体')
        // 使用默认智能体
        if (agentList.value.length > 0) {
          selectedAgentId.value = agentList.value[0].id
          await loadConversations()
        }
      }
    } else {
      // 默认选择第一个智能体
      if (agentList.value.length > 0 && !selectedAgentId.value) {
        selectedAgentId.value = agentList.value[0].id
        await loadConversations()
      }
    }
  } catch (error) {
    console.error('加载智能体列表失败:', error)
  }
}

// 加载会话列表
const loadConversations = async () => {
  if (!selectedAgentId.value) return
  
  isLoadingMessages.value = true  // 开始加载，避免欢迎页闪烁
  
  try {
    const response = await request({ url: '/cms/ai/conversation/list', method: 'get', params: {
        agentId: selectedAgentId.value,
        userId: null  // 暂不支持多用户
      }
    })
    
    // 后端返回的是 ListResponse 格式：{ list: [], total: n }
    conversations.value = response.data?.list || []
    console.log(`加载了 ${conversations.value.length} 个会话`)

    // 如果有会话，自动选择第一个
    if (conversations.value.length > 0 && !currentConversationId.value) {
      await switchConversation(conversations.value[0].id)
    } else if (conversations.value.length === 0) {
      // 没有会话，显示欢迎页
      isLoadingMessages.value = false
    }
  } catch (error) {
    console.error('加载会话列表失败:', error)
    isLoadingMessages.value = false
  }
}

// 清空当前对话
const clearCurrentChat = async () => {
  if (messages.value.length === 0) return
  
  try {
    await ElMessageBox.confirm('确定要清空当前对话吗？', '提示', {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      type: 'warning'
    })
    
    messages.value = []
    ElMessage.success('已清空对话')
  } catch {
    // 用户取消
  }
}

// 创建新会话
const createNewConversation = async () => {
  if (!selectedAgentId.value) {
    ElMessage.warning('请先选择智能体')
    return
  }
  
  try {
    const response = await request({ url: '/cms/ai/conversation/create', method: 'post'})
    
    const newConv = response.data
    conversations.value.unshift(newConv)  // 添加到列表开头
    currentConversationId.value = newConv.id
    uuid.value = newConv.id  // 使用会话ID作为memoryId
    messages.value = []  // 清空消息

    console.log(`创建新会话: ${newConv.id}`)
    ElMessage.success('已创建新会话')
    // 不再自动发送问候，保持空白显示欢迎页面
  } catch (error) {
    console.error('创建会话失败:', error)
    ElMessage.error('创建会话失败')
  }
}

// 切换会话
const switchConversation = async (conversationId) => {
  if (currentConversationId.value === conversationId) return
  
  currentConversationId.value = conversationId
  uuid.value = conversationId  // 使用会话ID作为memoryId
  isLoadingMessages.value = true  // 开始加载
  
  try {
    // 加载会话的历史消息
    const response = await request({ url: `/cms/ai/conversation/${conversationId}/messages`, method: 'get' })
    
    // 后端返回的是 ListResponse 格式：{ list: [], total: n }
    const history = response.data?.list || []
    messages.value = history.map(msg => {
      let content = msg.content
      let referenceSources = null

      // 过滤掉工具调用标记（历史消息回显时）
      if (content) {
        content = content.replace(/\[TOOL_CALL\][\s\S]*?\[\/TOOL_CALL\]/g, '').trim()
        // 如果过滤后为空，显示友好提示
        if (!content && msg.role === 'assistant') {
          content = '已为您查询相关信息，请查看上方结果。'
        }
      }

      // 如果是AI回复且有参考来源，解析并单独保存
      if (msg.role === 'assistant' && msg.referenceSources) {
        try {
          referenceSources = JSON.parse(msg.referenceSources)
          console.log(`📚 历史消息加载参考来源: ${referenceSources.length}个`, referenceSources)

          // 处理历史消息中的图片占位符和参考来源HTML
          content = processHistoricalMessage(content, referenceSources)
        } catch (e) {
          console.error('❌ 解析参考来源失败:', e)
          console.error('📋 原始JSON:', msg.referenceSources)

          // JSON解析失败时的容错处理：尝试修复常见问题
          try {
            // 尝试修复：替换单反斜杠为双反斜杠
            const fixedJson = msg.referenceSources.replace(/\\/g, '\\\\').replace(/\\\\\\\\/g, '\\\\')
            referenceSources = JSON.parse(fixedJson)
            console.log(`✅ 修复后成功解析，参考来源: ${referenceSources.length}个`)
            content = processHistoricalMessage(content, referenceSources)
          } catch (e2) {
            console.error('❌ 修复后仍然解析失败，跳过此消息的参考来源')
            // 移除content中的图片占位符，避免显示[[IMAGE_1]]
            content = content.replace(/\[\[IMAGE_\d+\]\]/g, '（图片加载失败）')
          }
        }
      }

      return {
        isUser: msg.role === 'user',
        content: content,
        referenceSources: referenceSources,  // 单独保存参考来源数据
        isTyping: false,
        isThinking: false
      }
    })

    console.log(`切换到会话 ${conversationId}，加载了 ${messages.value.length} 条消息`)
    // 切换会话后强制滚动到底部（等待DOM更新）
    await nextTick()
    setTimeout(() => scrollToBottom(true), 50)
  } catch (error) {
    console.error('加载会话消息失败:', error)
    ElMessage.error('加载消息失败')
  } finally {
    isLoadingMessages.value = false  // 加载完成
    // 再次确保滚动到底部
    await nextTick()
    setTimeout(() => scrollToBottom(true), 100)
  }
}

// 删除会话
const deleteConversation = async (conversationId) => {
  try {
    await ElMessageBox.confirm('确定删除此会话吗？删除后无法恢复。', '确认删除', {
      confirmButtonText: '删除',
      cancelButtonText: '取消',
      type: 'warning'
    })
    
    const response = await request({ url: `/cms/ai/conversation/${conversationId}`, method: 'delete'})
    
    // 从列表中移除
    conversations.value = conversations.value.filter(c => c.id !== conversationId)

    // 如果删除的是当前会话，切换到第一个会话或创建新会话
    if (currentConversationId.value === conversationId) {
      if (conversations.value.length > 0) {
        await switchConversation(conversations.value[0].id)
      } else {
        await createNewConversation()
      }
    }

    ElMessage.success('会话已删除')
  } catch (error) {
    if (error !== 'cancel') {
      console.error('删除会话失败:', error)
      ElMessage.error('删除失败')
    }
  }
}

// 快捷键处理
const handleKeydown = (e) => {
  // Ctrl/Cmd + Enter 发送消息
  if ((e.ctrlKey || e.metaKey) && e.key === 'Enter') {
    e.preventDefault()
    if (inputMessage.value.trim() && !isSending.value) {
      sendMessage()
    }
  }
  
  // Ctrl/Cmd + N 新建会话
  if ((e.ctrlKey || e.metaKey) && e.key === 'n') {
    e.preventDefault()
    createNewConversation()
  }
  
  // Escape 停止生成或关闭弹窗
  if (e.key === 'Escape') {
    if (isSending.value) {
      // 优先停止生成
      e.preventDefault()
      stopGeneration()
    } else if (showFileDialog.value) {
      showFileDialog.value = false
    }
  }
}

onMounted(() => {
  // 响应式布局初始化
  checkMobile()
  window.addEventListener('resize', checkMobile)
  
  // 注册快捷键
  window.addEventListener('keydown', handleKeydown)
  
  initUUID()
  loadAgents()  // hello() 会在 loadAgents 完成后自动调用
  
  // 智能滚动：只在用户位于底部时自动滚动
  watch(messages, () => {
    // 使用nextTick确保DOM更新后再滚动
    setTimeout(() => scrollToBottom(false), 50)
    // 异步加载需要鉴权的图片（data-auth-image 标记，浏览器原生 <img> 无法携带 token）
    nextTick(() => loadAuthImages())
  }, { deep: true })
  
  // 同步agentId和memoryId到window对象，供反馈功能使用
  watch(selectedAgentId, (newId) => {
    window.currentAgentId = newId
  }, { immediate: true })
  
  watch(uuid, (newId) => {
    window.currentMemoryId = newId
  }, { immediate: true })

  // 监听文件预览弹窗关闭，释放Blob URL避免内存泄漏
  watch(showFileDialog, (newVal, oldVal) => {
    if (oldVal && !newVal) {
      // 弹窗关闭时释放Blob URL
      if (filePreviewUrl.value && filePreviewUrl.value.startsWith('blob:')) {
        console.log('🗑️ 释放Blob URL:', filePreviewUrl.value)
        URL.revokeObjectURL(filePreviewUrl.value)
        filePreviewUrl.value = ''
      }
    }
  })

  // 监听全局show-reference事件
  window.addEventListener('show-reference', async (e) => {
    currentReference.value = e.detail
    
    // 如果是图片类型，直接显示图片
    if (e.detail.type === 'image') {
      showImageReference(e.detail)
    } else {
      await openOriginalFile(e.detail.kbId, e.detail.segment)
    }
  })
})

// 组件卸载前清理
onBeforeUnmount(() => {
  if (filePreviewUrl.value && filePreviewUrl.value.startsWith('blob:')) {
    console.log('🗑️ 组件卸载，释放Blob URL')
    URL.revokeObjectURL(filePreviewUrl.value)
  }
  // 移除事件监听
  window.removeEventListener('resize', checkMobile)
  window.removeEventListener('keydown', handleKeydown)
})

// 处理历史消息中的图片占位符和参考来源HTML
const processHistoricalMessage = (content, referenceSources) => {
  console.log('🔍 开始处理历史消息，参考来源数量:', referenceSources?.length)
  
  if (!referenceSources || referenceSources.length === 0) {
    console.log('⚠️ 没有参考来源，跳过处理')
    return content
  }
  
  let processedContent = content
  
  // 打印参考来源详情
  referenceSources.forEach((source, idx) => {
    console.log(`  [${idx+1}] type: ${source.type}, imagePath: ${source.imagePath ? '有' : '无'}, fileName: ${source.fileName}`)
  })
  
  // 1. 收集所有图片
  const images = referenceSources.filter(s => s.type === 'image' && s.imagePath)
  console.log(`📷 找到 ${images.length} 张图片`)
  
  // 2. 构建所有图片的HTML
  const imageHtmls = images.map((source, index) => {
    const imageIndex = index + 1
    console.log(`🖼️ 构建图片HTML: 图${imageIndex}, 路径: ${source.imagePath}`)
    
    return `
<div class='inline-image-card' style='margin: 16px 0; max-width: 500px;'>
<div style='display: flex; align-items: center; gap: 8px; margin-bottom: 8px; font-size: 13px; color: #606266;'>
<span style='background: #67c23a; color: white; padding: 2px 8px; border-radius: 4px; font-size: 12px;'>📷 图${imageIndex}</span>
<span>${source.fileName} · 第${source.pageNumber}页</span>
</div>
<img data-auth-image='${encodeURIComponent(source.imagePath)}' 
     src='data:image/svg+xml,%3Csvg xmlns=%22http://www.w3.org/2000/svg%22/%3E'
     style='max-width: 100%; border-radius: 6px; cursor: pointer; box-shadow: 0 2px 8px rgba(0,0,0,0.1);' 
     onerror="this.style.display='none'; this.nextElementSibling.style.display='block'" />
<div style='display:none; color: #f56c6c; padding: 20px; text-align: center;'>图片加载失败</div>
</div>
`
  })
  
  // 3. 在内容末尾添加图片（因为后端已清理占位符，不需要替换）
  if (imageHtmls.length > 0) {
    processedContent = processedContent + '\n\n' + imageHtmls.join('\n')
    console.log(`✅ 添加了 ${imageHtmls.length} 张图片到内容末尾`)
  }
  
  console.log(`✅ 历史消息处理完成`)
  return processedContent
}

// 处理参考来源按钮点击（Vue组件调用）
const handleReferenceClick = (button) => {
  // 调用全局函数处理点击事件
  window.showReferenceSource(button)
}

// 鉴权图片缓存：path -> blobUrl，避免历史消息滚动时重复请求
const authImageCache = new Map()

// 异步加载需要鉴权的图片（<img data-auth-image> 标记由 processHistoricalMessage 生成）
// 浏览器原生 <img src> 无法携带 Authorization header，因此先用占位图，渲染后用 request 获取 blob URL 替换
const loadAuthImages = async () => {
  const imgs = document.querySelectorAll('.message-list img[data-auth-image]:not([data-loaded])')
  for (const img of imgs) {
    img.setAttribute('data-loaded', 'true')
    const path = decodeURIComponent(img.getAttribute('data-auth-image'))
    try {
      let blobUrl = authImageCache.get(path)
      if (!blobUrl) {
        const blob = await request({
          url: '/cms/ai/image',
          method: 'get',
          params: { path },
          responseType: 'blob'
        })
        blobUrl = URL.createObjectURL(blob)
        authImageCache.set(path, blobUrl)
      }
      img.src = blobUrl
      img.addEventListener('click', () => window.open(blobUrl, '_blank'))
    } catch (e) {
      console.error('鉴权图片加载失败:', path, e)
      img.style.display = 'none'
      if (img.nextElementSibling) img.nextElementSibling.style.display = 'block'
    }
  }
}

// 显示图片参考资料
const showImageReference = async (detail) => {
  console.log('显示图片参考资料:', detail)

  // 后端 /cms/ai/image 需鉴权，浏览器原生 <img> 无法携带 token，先获取 blob URL
  let blobUrl = authImageCache.get(detail.imagePath)
  if (!blobUrl) {
    try {
      const blob = await request({
        url: '/cms/ai/image',
        method: 'get',
        params: { path: detail.imagePath },
        responseType: 'blob'
      })
      blobUrl = URL.createObjectURL(blob)
      authImageCache.set(detail.imagePath, blobUrl)
      console.log('图片加载成功，blob URL:', blobUrl)
    } catch (e) {
      console.error('图片加载失败:', e)
      ElMessage.error('图片加载失败: ' + (e.message || '未知错误'))
      return
    }
  }

  ElMessageBox({
    title: `📷 图片参考 - ${detail.fileName} (第${detail.pageNumber}页)`,
    message: `
      <div style="text-align: center; padding: 20px;">
        <div style="margin-bottom: 15px; color: #666; font-size: 14px;">
          📍 位置：第 ${detail.pageNumber} 页
        </div>
        <div style="position: relative; display: inline-block;">
          <img src="${blobUrl}"
               style="max-width: 100%; max-height: 500px; border-radius: 8px; box-shadow: 0 4px 12px rgba(0,0,0,0.15);" />
        </div>
        <div style="margin-top: 15px; font-size: 12px; color: #999;">
          点击"查看原文"可定位到PDF文档
        </div>
      </div>
    `,
    dangerouslyUseHTMLString: true,
    confirmButtonText: '关闭',
    showCancelButton: true,
    cancelButtonText: '📄 查看原文',
    customClass: 'image-reference-dialog',
    beforeClose: (action, instance, done) => {
      if (action === 'cancel') {
        done()
        console.log('打开原文定位，知识库ID:', detail.kbId, '页码:', detail.pageNumber)
        // 确保currentReference有完整的detail信息
        currentReference.value = detail
        openOriginalFile(detail.kbId, detail.pageNumber || detail.segment)
      } else {
        done()
      }
    }
  })
}

// 打开原始文件
const openOriginalFile = async (knowledgeBaseId, segmentIndex) => {
  console.log('📂 打开原始文件 - knowledgeBaseId:', knowledgeBaseId, 'segmentIndex:', segmentIndex)
  
  if (!knowledgeBaseId) {
    console.error('❌ knowledgeBaseId 为空')
    ElMessage.error('知识库ID缺失')
    return
  }

  // 🔧 优化：先不释放旧URL，等新文件加载完再释放，避免闪烁
  const oldBlobUrl = filePreviewUrl.value

  isLoadingFile.value = true

  try {
    const url = `/cms/ai/knowledge-base/${knowledgeBaseId}/file-info`
    console.log('🌐 请求URL:', url)
    
    // 获取文件信息
    const infoResponse = await request({ url: url, method: 'get' })
    console.log('📋 文件信息响应:', infoResponse.data)
    const fileInfo = infoResponse.data
    // 防御性检查：确保 fileType 存在
    if (fileInfo.fileType && typeof fileInfo.fileType === 'string') {
      fileType.value = fileInfo.fileType.toLowerCase()
    } else {
      console.warn('⚠️ fileType 字段缺失或格式错误，使用默认值 pdf')
      fileType.value = 'pdf'  // 默认为 PDF
    }

    // 获取精确定位信息
    const pageNumber = currentReference.value.pageNumber
    const lineStart = currentReference.value.lineStart

    // 使用预览接口获取 PDF 文件
    const fileResponse = await request({ url: `/cms/ai/knowledge-base/${knowledgeBaseId}/preview`, method: 'get', responseType: 'blob'})

    console.log('参考原文下载响应:', {
      status: fileResponse.status,
      contentType: fileResponse.headers['content-type'],
      size: fileResponse.data.size
    })

    // 创建 Blob URL（所有文件都转换为 PDF）
    const blob = new Blob([fileResponse.data], {
      type: 'application/pdf'
    })
    const blobUrl = URL.createObjectURL(blob)

    // 设置URL（不需要#page=参数，vue-pdf-embed通过:page属性控制）
    filePreviewUrl.value = blobUrl

    // 🔧 新URL设置成功后，延迟释放旧URL（避免闪烁）
    if (oldBlobUrl && oldBlobUrl.startsWith('blob:') && oldBlobUrl !== blobUrl) {
      setTimeout(() => {
        console.log('🗑️ 释放旧的Blob URL:', oldBlobUrl.substring(0, 30) + '...')
        URL.revokeObjectURL(oldBlobUrl)
      }, 500)  // 延迟500ms释放，确保新文件开始渲染
    }

    // 重置PDF状态
    currentPage.value = 1
    totalPages.value = 0

    console.log('PDF 预览 URL:', filePreviewUrl.value)

    // 显示弹窗（无需message提示，弹窗本身已经很直观）
    showFileDialog.value = true

    // 设置超时，如果10秒后还没加载完成，自动关闭loading
    setTimeout(() => {
      if (isLoadingFile.value) {
        console.warn('参考原文加载超时，自动关闭loading')
        isLoadingFile.value = false
      }
    }, 10000)
  } catch (error) {
    console.error('打开文件失败:', error)
    
    // 根据错误类型给出更具体的提示
    let errorMessage = '打开文件失败'
    if (error.response) {
      if (error.response.status === 404) {
        errorMessage = '文件未找到或后端服务未启动（404）'
        console.error('❌ 请检查：1. 后端服务是否运行在 http://localhost:8080  2. 文件是否存在于知识库')
      } else if (error.response.status === 500) {
        errorMessage = '服务器内部错误（500）'
      } else {
        errorMessage = `请求失败（${error.response.status}）`
      }
    } else if (error.request) {
      errorMessage = '无法连接到后端服务，请检查后端是否启动'
      console.error('❌ 后端服务地址: http://localhost:8080')
    } else {
      errorMessage = error.message || '未知错误'
    }
    
    ElMessage.error(errorMessage)
    isLoadingFile.value = false
  }
}

// PDF加载完成
const onPdfLoaded = (pdf) => {
  isLoadingFile.value = false
  totalPages.value = pdf.numPages
  console.log(`PDF加载完成，共 ${totalPages.value} 页`)
  
  // 跳转到引用页码
  const pageNumber = currentReference.value.pageNumber || currentReference.value.segment
  console.log('📍 引用信息:', {
    pageNumber: currentReference.value.pageNumber,
    segment: currentReference.value.segment,
    type: currentReference.value.type,
    fileName: currentReference.value.fileName
  })
  
  if (pageNumber) {
    // vue-pdf-embed 的 page 属性是 1-indexed，直接使用存储的页码
    const targetPage = parseInt(pageNumber)
    console.log(`📄 目标页码: ${targetPage}`)
    currentPage.value = targetPage
    console.log(`✅ 跳转到第 ${currentPage.value} 页`)
  }
}

// 文件加载错误
const onFileError = (error) => {
  isLoadingFile.value = false
  console.error('文件加载失败:', error)
  ElMessage.error('文件预览失败，请尝试下载后查看')
}

// PDF工具栏控制（暂时隐藏缩放功能，使用自适应宽度）
const zoomIn = () => {
  // 自适应宽度模式下不需要缩放
}

const zoomOut = () => {
  // 自适应宽度模式下不需要缩放
}

const resetZoom = () => {
  // 自适应宽度模式下不需要缩放
}

const nextPage = () => {
  if (currentPage.value < totalPages.value) {
    currentPage.value++
  }
}

const prevPage = () => {
  if (currentPage.value > 1) {
    currentPage.value--
  }
}

// 刷新文件
const refreshFile = () => {
  // 释放旧的Blob URL
  if (filePreviewUrl.value && filePreviewUrl.value.startsWith('blob:')) {
    URL.revokeObjectURL(filePreviewUrl.value)
  }
  
  // 重新获取文件
  const kbId = currentReference.value.kbId || currentReference.value.knowledgeBaseId
  const segment = currentReference.value.segment || currentReference.value.pageNumber
  
  if (kbId) {
    console.log('🔄 刷新文件 - knowledgeBaseId:', kbId)
    openOriginalFile(kbId, segment)
  } else {
    ElMessage.error('无法刷新：缺少知识库ID')
  }
}

// 检查用户是否在底部
const checkIfAtBottom = () => {
  if (!messaggListRef.value) return true
  
  const element = messaggListRef.value
  const threshold = 100 // 距离底部100px内认为是在底部
  const isAtBottom = element.scrollHeight - element.scrollTop - element.clientHeight < threshold
  
  isUserAtBottom.value = isAtBottom
  return isAtBottom
}

// 滚动到底部（只在用户位于底部或强制滚动时）
const scrollToBottom = (force = false) => {
  if (!messaggListRef.value) return
  
  // 如果用户在底部或强制滚动，则滚动到底部
  if (force || isUserAtBottom.value) {
    messaggListRef.value.scrollTop = messaggListRef.value.scrollHeight
    isUserAtBottom.value = true
  }
}

// 监听用户滚动
const onScroll = () => {
  checkIfAtBottom()
}

const hello = async () => {
  await sendRequest('你好', true)  // 第二个参数true表示是系统打招呼
}

// ==================== 多模态图片处理 ====================

// 触发图片上传
const triggerImageUpload = () => {
  imageInputRef.value?.click()
}

// 处理图片选择
const handleImageSelect = (event) => {
  const files = event.target.files
  if (files) {
    processImageFiles(Array.from(files))
  }
  // 清空input以便重复选择同一文件
  event.target.value = ''
}

// 处理粘贴事件（支持粘贴图片）
const handlePaste = (event) => {
  const items = event.clipboardData?.items
  if (!items) return
  
  const imageFiles = []
  for (const item of items) {
    if (item.type.startsWith('image/')) {
      const file = item.getAsFile()
      if (file) imageFiles.push(file)
    }
  }
  
  if (imageFiles.length > 0) {
    event.preventDefault()  // 阻止默认粘贴
    processImageFiles(imageFiles)
  }
}

// 处理图片文件（转为Base64预览）
const processImageFiles = (files) => {
  const maxSize = 10 * 1024 * 1024  // 10MB
  const maxCount = 5  // 最多5张图片
  
  if (uploadedImages.value.length + files.length > maxCount) {
    ElMessage.warning(`最多只能上传${maxCount}张图片`)
    return
  }
  
  files.forEach(file => {
    if (file.size > maxSize) {
      ElMessage.warning(`图片 ${file.name} 超过10MB限制`)
      return
    }
    
    const reader = new FileReader()
    reader.onload = (e) => {
      uploadedImages.value.push({
        url: e.target.result,
        name: file.name,
        file: file
      })
    }
    reader.readAsDataURL(file)
  })
}

// 移除图片
const removeImage = (index) => {
  uploadedImages.value.splice(index, 1)
}

// 预览图片（大图查看）
const previewImageUrl = ref('')
const showImagePreview = ref(false)

const previewImage = (src) => {
  previewImageUrl.value = src
  showImagePreview.value = true
}

// ==================== 拖拽上传 ====================

const onDragOver = () => {
  isDragging.value = true
}

const onDragLeave = () => {
  isDragging.value = false
}

const onDrop = (event) => {
  isDragging.value = false
  const files = event.dataTransfer?.files
  if (files && files.length > 0) {
    const imageFiles = Array.from(files).filter(f => f.type.startsWith('image/'))
    if (imageFiles.length > 0) {
      processImageFiles(imageFiles)
    }
  }
}

// ==================== 消息发送 ====================

const sendMessage = async () => {
  // 如果正在发送，阻止新的发送
  if (isSending.value) {
    ElMessage.warning('请等待AI回答完成后再发送')
    return
  }
  
  // 有文字或有图片都可以发送
  if (inputMessage.value.trim() || uploadedImages.value.length > 0) {
    const text = inputMessage.value.trim() || (uploadedImages.value.length > 0 ? '请描述这张图片' : '')
    const images = uploadedImages.value.map(img => img.url)  // Base64数组
    inputMessage.value = ''
    uploadedImages.value = []  // 先清空，避免重复发送
    await sendRequest(text, false, images)
  }
}

// 停止生成
const stopGeneration = async () => {
  if (!isSending.value || !currentConversationId.value) {
    return
  }
  
  try {
    console.log('⏹️ 请求停止生成...')
    const response = await request({ url: `/cms/ai/chat/abort/${currentConversationId.value}`, method: 'post'})
    
    console.log('✅ 已发送停止信号')
    ElMessage.info('已停止生成')
  } catch (error) {
    console.error('停止生成失败:', error)
  }
}

// 复制消息内容
const copyMessage = async (content) => {
  try {
    // 移除HTML标签和特殊格式
    const plainText = content
      .replace(/<[^>]*>/g, '')
      .replace(/\[\[IMAGE_\d+\]\]/g, '[图片]')
      .trim()
    
    await navigator.clipboard.writeText(plainText)
    ElMessage.success('已复制到剪贴板')
  } catch (error) {
    console.error('复制失败:', error)
    ElMessage.error('复制失败')
  }
}

// 重新生成AI回复
const regenerateMessage = async (messageIndex) => {
  if (isSending.value) {
    ElMessage.warning('请等待当前回复完成')
    return
  }
  
  // 找到对应的用户消息（AI消息的前一条）
  let userMessageIndex = messageIndex - 1
  while (userMessageIndex >= 0 && !messages.value[userMessageIndex].isUser) {
    userMessageIndex--
  }
  
  if (userMessageIndex < 0) {
    ElMessage.warning('找不到对应的用户消息')
    return
  }
  
  const userMessage = messages.value[userMessageIndex].content
  
  // 删除当前AI消息
  messages.value.splice(messageIndex, 1)
  
  // 重新发送请求
  console.log('🔄 重新生成回复:', userMessage)
  await sendRequest(userMessage, true) // true表示不重复显示用户消息
}

const sendRequest = async (message, isGreeting = false, images = []) => {
  // 确保已选择智能体
  if (!selectedAgentId.value) {
    console.warn('未选择智能体，跳过发送')
    return
  }

  // 🔧 修复：如果没有会话，先创建一个
  if (!currentConversationId.value) {
    try {
      console.log('📝 没有会话，自动创建新会话...')
      const response = await request({ url: '/cms/ai/conversation/create', method: 'post'})
      
      const newConv = response.data
      conversations.value.unshift(newConv)
      currentConversationId.value = newConv.id
      uuid.value = newConv.id
      console.log(`✅ 自动创建会话成功: ${newConv.id}`)
    } catch (error) {
      console.error('❌ 创建会话异常:', error)
      ElMessage.error('创建会话失败，请重试')
      return
    }
  }

  console.log('🚀 开始发送消息，isSending设置为true, isGreeting:', isGreeting, '图片数:', images.length)
  isSending.value = true
  
  // 只有非系统打招呼时，才显示用户消息
  if (!isGreeting) {
    const userMsg = {
      isUser: true,
      content: message,
      images: images,  // 保存图片用于显示
      isTyping: false,
      isThinking: false,
    }
    messages.value.push(userMsg)
  }

  // 添加机器人加载消息
  const botMsg = {
    isUser: false,
    content: '', // 增量填充
    isTyping: true, // 显示加载动画
    isThinking: false,
  }
  messages.value.push(botMsg)
  const lastMsg = messages.value[messages.value.length - 1]
  
  // 用于存储图片HTML映射
  let imageHtmlMap = {}
  let imageMapExtracted = false  // 标记是否已提取映射
  
  // 发送新消息时，强制滚动到底部
  setTimeout(() => scrollToBottom(true), 100)

  // 使用框架提供的 fetchStream（基于 fetch + ReadableStream，自动注入 Authorization: Bearer <token>），
  // 替代原裸 axios.post（原代码未导入 axios 且绕过请求拦截器，token 无法注入，导致后端 401）。
  let fullText = ''
  fetchStream('/cms/ai/chat/stream', {
    method: 'POST',
    data: {
      conversationId: currentConversationId.value,
      message,
      agentId: selectedAgentId.value,
      isGreeting: isGreeting,
      images: images
    },
    onMessage: (chunk) => {
      fullText += chunk

      // 🖼️ 提取并处理图片HTML映射（持续检查直到提取成功）
      if (!imageMapExtracted) {
        const imageMapRegex = /\[IMAGE_HTML_MAP\](.*?)\[\/IMAGE_HTML_MAP\]/s
        const imageMapMatch = fullText.match(imageMapRegex)
        if (imageMapMatch) {
          try {
            const mapData = JSON.parse(imageMapMatch[1])
            imageHtmlMap = mapData
            imageMapExtracted = true
            console.log('📤 成功解析图片HTML映射:', Object.keys(imageHtmlMap).length, '张图片', imageHtmlMap)
          } catch (err) {
            console.warn('⚠️ 解析图片HTML映射失败，等待下次尝试:', err.message)
          }
        }
      }

      // 移除IMAGE_HTML_MAP标记
      let displayText = fullText.replace(/\[IMAGE_HTML_MAP\].*?\[\/IMAGE_HTML_MAP\]/gs, '')

      // 🔄 替换图片占位符为实际HTML（后备方案，后端应该已替换）
      let imageReplaced = false
      if (Object.keys(imageHtmlMap).length > 0) {
        let replacedCount = 0
        displayText = displayText.replace(/\[\[IMAGE_(\d+)\]\]/g, (match, index) => {
          const html = imageHtmlMap[index] || imageHtmlMap[parseInt(index)]
          if (html) {
            replacedCount++
            imageReplaced = true
            console.log(`✅ 前端替换图片占位符: ${match}`)
            return html
          }
          console.warn(`⚠️ 找不到图片${index}的HTML映射`)
          return match
        })
        if (replacedCount > 0) {
          console.log(`🎨 前端替换了 ${replacedCount} 个图片占位符（后端应该已处理）`)
        }
      }

      // 更新内容
      lastMsg.content = displayText

      // 🔧 如果替换了图片，强制重新渲染
      if (imageReplaced) {
        setTimeout(() => {
          console.log('🖼️ 图片已替换，强制滚动到底部')
          scrollToBottom(false)
        }, 50)
      }
    },
    onDone: () => {
      // 流结束后隐藏加载动画
      lastMsg.isTyping = false
      console.log('✅ AI回答完成')

      // 刷新会话列表以更新标题
      loadConversations()
    },
    onError: (error) => {
      console.error('流式错误:', error)
      lastMsg.content = '请求失败，请重试'
      lastMsg.isTyping = false
      // 错误时立即重置发送状态
      isSending.value = false
      console.log('🔄 isSending已重置为false（错误分支），可以继续发送')
    }
  })

  // 确保无论成功失败都重置发送状态
  // fetchStream 返回 { abort } 控制器对象而非 Promise，
  // 通过 onDone/onError 回调已处理流结束逻辑，此处用 setTimeout 兜底确保 isSending 重置
  setTimeout(() => {
    isSending.value = false
    console.log('🔄 isSending已重置为false，可以继续发送')
  }, 500)
}

// 初始化 UUID
const initUUID = () => {
  let storedUUID = cache.local.get('user_base_uuid')
  if (!storedUUID) {
    storedUUID = uuidToNumber(uuidv4())
    cache.local.set('user_base_uuid', storedUUID)
  }
  baseUuid.value = storedUUID
  // 初始 uuid 为 baseUuid，后续会根据 agentId 更新
  uuid.value = storedUUID
}

const uuidToNumber = (uuid) => {
  let number = 0
  for (let i = 0; i < uuid.length && i < 6; i++) {
    const hexValue = uuid[i]
    number = number * 16 + (parseInt(hexValue, 16) || 0)
  }
  return number % 1000000
}

// 转换特殊字符 - 先转义HTML特殊字符，再处理换行和空格
const convertStreamOutput = (output) => {
  if (!output) return ''
  return output
    .replace(/&/g, '&amp;')   // 先转义 &
    .replace(/</g, '&lt;')    // 转义 <
    .replace(/>/g, '&gt;')    // 转义 >
    .replace(/"/g, '&quot;')  // 转义 "
    .replace(/'/g, '&#39;')   // 转义 '
    .replace(/\n/g, '<br>')   // 换行符转为 <br>
    .replace(/\t/g, '&nbsp;&nbsp;&nbsp;&nbsp;')  // Tab转为空格
    .replace(/  /g, '&nbsp;&nbsp;')  // 连续空格转为 &nbsp;
}

const newChat = () => {
  createNewConversation()
}

// 在新标签页打开
const openInNewTab = () => {
  if (filePreviewUrl.value) {
    window.open(filePreviewUrl.value, '_blank')
  }
}

</script>

<style scoped src="@/views/ai/styles/chat-window.css"></style>

<!-- 图片预览弹窗样式（全局） -->
<style>
.image-preview-dialog .el-dialog {
  background: transparent !important;
  box-shadow: none !important;
  width: auto !important;
  max-width: 90vw !important;
  margin: 0 auto !important;
}

.image-preview-dialog .el-dialog__header {
  display: none !important;
}

.image-preview-dialog .el-dialog__body {
  padding: 0 !important;
  background: transparent !important;
  display: flex;
  justify-content: center;
  align-items: center;
}

.preview-full-image {
  max-width: 85vw;
  max-height: 85vh;
  object-fit: contain;
  border-radius: 8px;
  box-shadow: 0 8px 32px rgba(0,0,0,0.3);
  cursor: pointer;
}
</style>
