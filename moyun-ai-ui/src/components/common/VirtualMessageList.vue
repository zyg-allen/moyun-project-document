<template>
  <div 
    class="virtual-message-list" 
    ref="containerRef" 
    @scroll="handleScroll"
    :style="{ height: containerHeight }"
  >
    <!-- 上方占位（保持滚动位置） -->
    <div :style="{ height: topPadding + 'px' }"></div>
    
    <!-- 渲染可见消息 -->
    <div
      v-for="(message, index) in visibleMessages"
      :key="startIndex + index"
      :ref="el => setMessageRef(startIndex + index, el)"
      class="message-item"
      :class="message.isUser ? 'user-message' : 'bot-message'"
    >
      <!-- 消息图标 -->
      <i :class="message.isUser ? 'fa-solid fa-user message-icon' : 'fa-solid fa-robot message-icon'"></i>
      
      <!-- 消息内容 -->
      <div class="message-content">
        <div v-if="!message.isUser" class="markdown-body" v-html="renderMarkdown(message.content)"></div>
        <div v-else class="user-text">{{ message.content }}</div>
        
        <!-- 参考来源 -->
        <div v-if="!message.isUser && message.referenceSources?.length" class="references-section">
          <span class="ref-label">📚 参考来源：</span>
          <span v-for="(source, idx) in message.referenceSources" :key="idx" class="ref-item">
            <button 
              class="reference-source-btn"
              @click="$emit('reference-click', source, idx)"
            >
              来源{{ idx + 1 }}
            </button>
          </span>
        </div>
        
        <!-- 加载状态 -->
        <div v-if="message.isThinking" class="thinking-dots">
          <span></span><span></span><span></span>
        </div>
        <div v-if="message.isTyping" class="typing-cursor"></div>
      </div>
    </div>
    
    <!-- 下方占位 -->
    <div :style="{ height: bottomPadding + 'px' }"></div>
  </div>
</template>

<script setup>
import { ref, computed, watch, onMounted, onUnmounted, nextTick } from 'vue'
import { marked } from 'marked'
import hljs from 'highlight.js'

const props = defineProps({
  messages: {
    type: Array,
    default: () => []
  },
  containerHeight: {
    type: String,
    default: '100%'
  },
  estimatedItemHeight: {
    type: Number,
    default: 100  // 预估每条消息的高度
  },
  overscan: {
    type: Number,
    default: 5  // 额外渲染的数量（上下各多渲染几条）
  }
})

const emit = defineEmits(['scroll', 'reference-click'])

// Refs
const containerRef = ref(null)
const messageHeights = ref({})  // 缓存每条消息的实际高度
const scrollTop = ref(0)

// 设置消息元素引用（用于测量高度）
const setMessageRef = (index, el) => {
  if (el) {
    nextTick(() => {
      const height = el.getBoundingClientRect().height
      if (height > 0) {
        messageHeights.value[index] = height
      }
    })
  }
}

// 获取消息高度（优先用缓存，否则用预估值）
const getItemHeight = (index) => {
  return messageHeights.value[index] || props.estimatedItemHeight
}

// 计算总高度
const totalHeight = computed(() => {
  let height = 0
  for (let i = 0; i < props.messages.length; i++) {
    height += getItemHeight(i)
  }
  return height
})

// 计算可见范围
const visibleRange = computed(() => {
  if (!containerRef.value) return { start: 0, end: Math.min(20, props.messages.length) }
  
  const containerHeight = containerRef.value.clientHeight
  let start = 0
  let accumulatedHeight = 0
  
  // 找到起始位置
  for (let i = 0; i < props.messages.length; i++) {
    const itemHeight = getItemHeight(i)
    if (accumulatedHeight + itemHeight > scrollTop.value) {
      start = Math.max(0, i - props.overscan)
      break
    }
    accumulatedHeight += itemHeight
  }
  
  // 找到结束位置
  let end = start
  let visibleHeight = 0
  for (let i = start; i < props.messages.length; i++) {
    const itemHeight = getItemHeight(i)
    visibleHeight += itemHeight
    end = i + 1
    if (visibleHeight > containerHeight + props.overscan * props.estimatedItemHeight) {
      break
    }
  }
  
  return { start, end: Math.min(end + props.overscan, props.messages.length) }
})

// 可见消息
const visibleMessages = computed(() => {
  return props.messages.slice(visibleRange.value.start, visibleRange.value.end)
})

const startIndex = computed(() => visibleRange.value.start)

// 上方占位高度
const topPadding = computed(() => {
  let height = 0
  for (let i = 0; i < visibleRange.value.start; i++) {
    height += getItemHeight(i)
  }
  return height
})

// 下方占位高度
const bottomPadding = computed(() => {
  let height = 0
  for (let i = visibleRange.value.end; i < props.messages.length; i++) {
    height += getItemHeight(i)
  }
  return height
})

// 滚动处理
const handleScroll = (e) => {
  scrollTop.value = e.target.scrollTop
  emit('scroll', e)
}

// 滚动到底部
const scrollToBottom = (force = false) => {
  if (!containerRef.value) return
  
  nextTick(() => {
    const container = containerRef.value
    const isNearBottom = container.scrollHeight - container.scrollTop - container.clientHeight < 100
    
    if (force || isNearBottom) {
      container.scrollTop = container.scrollHeight
    }
  })
}

// Markdown 渲染
marked.setOptions({
  highlight: (code, lang) => {
    if (lang && hljs.getLanguage(lang)) {
      return hljs.highlight(code, { language: lang }).value
    }
    return hljs.highlightAuto(code).value
  },
  breaks: true
})

const renderMarkdown = (content) => {
  if (!content) return ''
  try {
    return marked(content)
  } catch (e) {
    return content
  }
}

// 监听消息变化，自动滚动
watch(() => props.messages.length, (newLen, oldLen) => {
  if (newLen > oldLen) {
    scrollToBottom()
  }
}, { flush: 'post' })

// 暴露方法
defineExpose({
  scrollToBottom,
  getContainer: () => containerRef.value
})
</script>

<style scoped>
.virtual-message-list {
  overflow-y: auto;
  overflow-x: hidden;
  scroll-behavior: smooth;
}

.message-item {
  display: flex;
  padding: 12px 16px;
  gap: 12px;
}

.message-item.user-message {
  flex-direction: row-reverse;
  background: #f0f7ff;
}

.message-item.bot-message {
  background: #fff;
}

.message-icon {
  width: 36px;
  height: 36px;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
  font-size: 16px;
}

.user-message .message-icon {
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  color: white;
}

.bot-message .message-icon {
  background: linear-gradient(135deg, #11998e 0%, #38ef7d 100%);
  color: white;
}

.message-content {
  max-width: 80%;
  line-height: 1.6;
}

.user-text {
  background: #409eff;
  color: white;
  padding: 10px 14px;
  border-radius: 12px 12px 4px 12px;
}

.markdown-body {
  background: #f8f9fa;
  padding: 12px 16px;
  border-radius: 4px 12px 12px 12px;
}

.references-section {
  margin-top: 12px;
  padding-top: 10px;
  border-top: 1px solid #eee;
  font-size: 13px;
}

.ref-label {
  color: #909399;
  margin-right: 8px;
}

.ref-item {
  margin-right: 8px;
}

.reference-source-btn {
  padding: 4px 12px;
  font-size: 12px;
  color: #409eff;
  background: #ecf5ff;
  border: 1px solid #b3d8ff;
  border-radius: 4px;
  cursor: pointer;
  transition: all 0.2s;
}

.reference-source-btn:hover {
  background: #409eff;
  color: white;
}

/* 思考动画 */
.thinking-dots {
  display: inline-flex;
  gap: 4px;
  padding: 8px 0;
}

.thinking-dots span {
  width: 8px;
  height: 8px;
  background: #409eff;
  border-radius: 50%;
  animation: bounce 1.4s infinite ease-in-out both;
}

.thinking-dots span:nth-child(1) { animation-delay: -0.32s; }
.thinking-dots span:nth-child(2) { animation-delay: -0.16s; }

@keyframes bounce {
  0%, 80%, 100% { transform: scale(0); }
  40% { transform: scale(1); }
}

/* 打字光标 */
.typing-cursor::after {
  content: '|';
  animation: blink 1s infinite;
  color: #409eff;
}

@keyframes blink {
  0%, 100% { opacity: 0; }
  50% { opacity: 1; }
}
</style>
