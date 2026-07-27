<template>
  <div class="app-container" v-loading="loading">
    <!-- 加载失败 -->
    <div v-if="error" class="error-state">
      <i class="fa-solid fa-circle-exclamation"></i>
      <h2>{{ error }}</h2>
      <p>请检查链接是否正确</p>
    </div>

    <!-- 应用主体 -->
    <div v-else-if="appInfo" class="app-main">
      <!-- 头部 -->
      <div class="app-header">
        <div class="app-info">
          <div class="app-icon">
            <i class="fa-solid fa-robot"></i>
          </div>
          <div class="app-title">
            <h1>{{ appInfo.name }}</h1>
            <p v-if="appInfo.description">{{ appInfo.description }}</p>
          </div>
        </div>
      </div>

      <!-- 聊天区域 -->
      <div class="chat-area" ref="chatArea">
        <!-- 欢迎消息 -->
        <div v-if="messages.length === 0" class="welcome-section">
          <div class="welcome-message" v-if="appInfo.welcomeMessage">
            <i class="fa-solid fa-hand-sparkles"></i>
            <p>{{ appInfo.welcomeMessage }}</p>
          </div>

          <!-- 预设问题 -->
          <div class="suggested-questions" v-if="suggestedQuestions.length > 0">
            <p class="questions-title">您可以这样问我：</p>
            <div class="questions-list">
              <button
                v-for="(q, index) in suggestedQuestions"
                :key="index"
                class="question-btn"
                @click="sendSuggestedQuestion(q)"
              >
                {{ q }}
              </button>
            </div>
          </div>
        </div>

        <!-- 消息列表 -->
        <div v-for="(msg, index) in messages" :key="index" :class="['message', msg.role]">
          <div class="message-avatar">
            <i :class="msg.role === 'user' ? 'fa-solid fa-user' : 'fa-solid fa-robot'"></i>
          </div>
          <div class="message-content" v-html="formatMessage(msg.content)"></div>
        </div>

        <!-- 加载中 -->
        <div v-if="isLoading" class="message assistant">
          <div class="message-avatar">
            <i class="fa-solid fa-robot"></i>
          </div>
          <div class="message-content loading">
            <span class="dot"></span>
            <span class="dot"></span>
            <span class="dot"></span>
          </div>
        </div>
      </div>

      <!-- 输入区域 -->
      <div class="input-area">
        <input
          v-model="inputMessage"
          type="text"
          placeholder="输入消息..."
          @keyup.enter="sendMessage"
          :disabled="isLoading"
        />
        <button @click="sendMessage" :disabled="isLoading || !inputMessage.trim()">
          <i class="fa-solid fa-paper-plane"></i>
        </button>
      </div>

      <!-- 底部品牌 -->
      <div class="app-footer">
        <span>Powered by Lynx AI</span>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, onMounted, nextTick } from 'vue'
import { useRoute } from 'vue-router'
import axios from 'axios'

const route = useRoute()
const token = computed(() => route.params.token)

const loading = ref(true)
const error = ref('')
const appInfo = ref(null)
const messages = ref([])
const inputMessage = ref('')
const isLoading = ref(false)
const sessionId = ref('')
const chatArea = ref(null)

// 解析预设问题
const suggestedQuestions = computed(() => {
  if (!appInfo.value?.suggestedQuestions) return []
  try {
    return JSON.parse(appInfo.value.suggestedQuestions)
  } catch {
    return []
  }
})

// 加载应用信息
const loadAppInfo = async () => {
  try {
    const response = await axios.get(`/api/agent/app/${token.value}/info`)
    if (response.data.success) {
      appInfo.value = response.data.data
      sessionId.value = 'session-' + Date.now()
    } else {
      error.value = response.data.message || '应用不存在'
    }
  } catch (err) {
    console.error('加载应用信息失败:', err)
    if (err.response?.status === 401) {
      error.value = '登录已失效，请重新登录'
    } else {
      error.value = '加载失败，请稍后重试'
    }
  } finally {
    loading.value = false
  }
}

// 发送消息
const sendMessage = async () => {
  const msg = inputMessage.value.trim()
  if (!msg || isLoading.value) return

  inputMessage.value = ''
  messages.value.push({ role: 'user', content: msg })

  await nextTick()
  scrollToBottom()

  isLoading.value = true

  try {
    const response = await fetch(`/api/agent/app/${token.value}/chat`, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ message: msg, sessionId: sessionId.value })
    })

    const reader = response.body.getReader()
    const decoder = new TextDecoder()
    let assistantMessage = ''

    messages.value.push({ role: 'assistant', content: '' })
    const msgIndex = messages.value.length - 1

    while (true) {
      const { done, value } = await reader.read()
      if (done) break

      const chunk = decoder.decode(value)
      const lines = chunk.split('\n')

      for (const line of lines) {
        if (line.startsWith('data: ')) {
          const data = line.substring(6).replace(/\\n/g, '\n')
          assistantMessage += data
          messages.value[msgIndex].content = assistantMessage
          await nextTick()
          scrollToBottom()
        }
      }
    }
  } catch (err) {
    console.error('发送消息失败:', err)
    messages.value.push({ role: 'assistant', content: '抱歉，发生了错误，请稍后重试。' })
  } finally {
    isLoading.value = false
  }
}

// 发送预设问题
const sendSuggestedQuestion = (question) => {
  inputMessage.value = question
  sendMessage()
}

// 滚动到底部
const scrollToBottom = () => {
  if (chatArea.value) {
    chatArea.value.scrollTop = chatArea.value.scrollHeight
  }
}

// 格式化消息（简单的markdown支持）
const formatMessage = (content) => {
  if (!content) return ''
  return content
    .replace(/\n/g, '<br>')
    .replace(/\*\*(.*?)\*\*/g, '<strong>$1</strong>')
    .replace(/\*(.*?)\*/g, '<em>$1</em>')
}

onMounted(() => {
  loadAppInfo()
})
</script>

<style scoped src="@/styles/app-view.css"></style>
