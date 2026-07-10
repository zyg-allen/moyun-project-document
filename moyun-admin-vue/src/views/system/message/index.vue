<template>
  <div class="message-center">
    <!-- 左侧：会话列表 -->
    <div class="session-panel">
      <div class="panel-header">
        <span class="panel-title">私信会话</span>
        <el-badge
          :value="totalUnread"
          :max="99"
          :hidden="totalUnread === 0"
          type="danger"
          class="header-badge"
        />
      </div>
      <div class="search-box">
        <el-input
          v-model="keyword"
          placeholder="搜索昵称"
          clearable
          :prefix-icon="Search"
          size="small"
        />
      </div>
      <div v-loading="loadingSessions" class="session-list">
        <div v-if="filteredSessions.length === 0 && !loadingSessions" class="empty-tip">
          暂无会话
        </div>
        <div
          v-for="s in filteredSessions"
          :key="s.id"
          class="session-item"
          :class="{ active: s.id === activeSessionId }"
          @click="selectSession(s)"
        >
          <el-avatar :size="40" :src="peerAvatar(s)">
            {{ peerInitial(s) }}
          </el-avatar>
          <div class="session-info">
            <div class="session-top">
              <span class="peer-name">
                {{ peerName(s) }}
                <el-tag
                  size="small"
                  :type="peerType(s) === 'sys' ? 'info' : 'success'"
                  effect="plain"
                  class="peer-type-tag"
                >{{ peerType(s) === 'sys' ? '管理员' : '用户' }}</el-tag>
              </span>
              <span class="session-time">{{ formatTime(s.lastMessageTime) }}</span>
            </div>
            <div class="session-bottom">
              <span class="preview-text">{{ s.lastMessageContent || '暂无消息' }}</span>
              <span v-if="s.unreadCount > 0" class="unread-dot">
                {{ s.unreadCount > 99 ? '99+' : s.unreadCount }}
              </span>
            </div>
          </div>
        </div>
      </div>
    </div>

    <!-- 中间：聊天详情 -->
    <div class="chat-panel">
      <template v-if="activeSession">
        <div class="chat-header">
          <div class="peer-info">
            <el-avatar :size="36" :src="peerAvatar(activeSession)">
              {{ peerInitial(activeSession) }}
            </el-avatar>
            <div class="peer-meta">
              <span class="peer-name">{{ peerName(activeSession) }}</span>
              <el-tag
                size="small"
                :type="peerType(activeSession) === 'sys' ? 'info' : 'success'"
                effect="plain"
              >{{ peerType(activeSession) === 'sys' ? '管理员' : '门户用户' }}</el-tag>
            </div>
          </div>
          <div class="chat-actions">
            <el-tooltip content="刷新消息" placement="bottom">
              <el-button :icon="Refresh" circle size="small" @click="refreshActive" />
            </el-tooltip>
          </div>
        </div>

        <div ref="scrollContainer" class="message-list" v-loading="loadingHistory">
          <template v-for="(msg, idx) in messages" :key="msg.id">
            <div v-if="shouldShowDateDivider(messages[idx - 1], msg)" class="date-divider">
              <span>{{ dateGroupLabel(msg.createTime) }}</span>
            </div>
            <div class="msg-row" :class="{ mine: isMine(msg) }">
              <el-avatar
                v-if="!isMine(msg)"
                :size="32"
                :src="msg.senderAvatar"
              />
              <div class="bubble">
                <div class="bubble-content">{{ msg.content }}</div>
                <div class="bubble-time">{{ formatTime(msg.createTime) }}</div>
              </div>
            </div>
          </template>
          <div v-if="messages.length === 0 && !loadingHistory" class="empty-msg">
            还没有消息，发送第一条私信吧
          </div>
        </div>

        <div class="input-area">
          <el-input
            v-model="inputText"
            type="textarea"
            :rows="2"
            placeholder="输入回复内容，Enter 发送，Shift+Enter 换行"
            resize="none"
            @keydown="onKeydown"
          />
          <el-button
            type="primary"
            :loading="sending"
            :disabled="!inputText.trim()"
            @click="send"
          >发送</el-button>
        </div>
      </template>
      <div v-else class="no-session">
        <el-empty description="请从左侧选择一个会话开始回复" />
      </div>
    </div>

    <!-- 右侧：AI 辅助回复（mock，不接真实 AI 服务） -->
    <div class="ai-panel">
      <div class="ai-header">
        <span class="ai-title">AI 辅助回复</span>
        <el-tag size="small" type="warning" effect="plain">模拟</el-tag>
      </div>
      <div class="ai-tip">
        基于对方最近消息生成建议，点击"采纳"填入输入框，可二次编辑后发送。
      </div>
      <div class="ai-suggestions">
        <div v-for="(s, i) in aiSuggestions" :key="i" class="suggestion-item">
          <div class="suggestion-text">{{ s }}</div>
          <el-button size="small" type="primary" link @click="adoptSuggestion(s)">采纳</el-button>
        </div>
        <div v-if="aiSuggestions.length === 0" class="ai-empty">
          选择会话后将生成建议
        </div>
      </div>
      <div class="ai-footer">
        <el-button
          size="small"
          :loading="aiLoading"
          :disabled="!activeSession"
          @click="regenerateSuggestions"
        >
          <el-icon><Refresh /></el-icon>
          <span>刷新建议</span>
        </el-button>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, onMounted, onUnmounted, nextTick, watch } from 'vue'
import { useRoute } from 'vue-router'
import { Search, Refresh } from '@element-plus/icons-vue'
import { ElMessage } from 'element-plus'
import {
  listSessions,
  getHistory,
  sendMessage,
  markSessionRead
} from '@/api/system/message'
import useMessageStore from '@/store/modules/message'

const route = useRoute()
const messageStore = useMessageStore()

// ===== 会话列表 =====
const sessions = ref([])
const loadingSessions = ref(false)
const keyword = ref('')
const activeSessionId = ref(null)
const activeSession = computed(() =>
  sessions.value.find(s => s.id === activeSessionId.value) || null
)

const totalUnread = computed(() =>
  sessions.value.reduce((sum, s) => sum + (s.unreadCount || 0), 0)
)

const filteredSessions = computed(() => {
  const kw = keyword.value.trim().toLowerCase()
  if (!kw) return sessions.value
  return sessions.value.filter(s => {
    const name = (s.peerUser && (s.peerUser.nickname || s.peerUser.username)) || ''
    return name.toLowerCase().includes(kw)
  })
})

async function loadSessions() {
  loadingSessions.value = true
  try {
    const res = await listSessions({ pageNum: 1, pageSize: 50 })
    if (res.code === 200 && res.data) {
      sessions.value = res.data.rows || res.data.records || []
    }
  } catch (e) {
    // 静默失败
  } finally {
    loadingSessions.value = false
  }
}

// ===== 聊天历史 =====
const messages = ref([])
const loadingHistory = ref(false)
const sending = ref(false)
const inputText = ref('')
const scrollContainer = ref(null)
// 请求序号：快速切换会话时只接受最新请求的结果，避免旧历史覆盖新历史
let historyReqSeq = 0

// 当前管理员为 sys 发送者
function isMine(msg) {
  return msg && msg.senderType === 'sys'
}

function peerName(s) {
  const peer = s && s.peerUser ? s.peerUser : {}
  return peer.nickname || peer.username || '未知用户'
}

function peerType(s) {
  return s && s.peerUser && s.peerUser.userType ? s.peerUser.userType : 'portal'
}

function peerAvatar(s) {
  return s && s.peerUser && s.peerUser.avatar ? s.peerUser.avatar : ''
}

function peerInitial(s) {
  const name = peerName(s)
  return name ? name.charAt(0).toUpperCase() : '?'
}

function formatTime(time) {
  if (!time) return ''
  const d = new Date(time)
  if (Number.isNaN(d.getTime())) return ''
  const now = new Date()
  const pad = n => String(n).padStart(2, '0')
  const sameDay = d.toDateString() === now.toDateString()
  if (sameDay) return `${pad(d.getHours())}:${pad(d.getMinutes())}`
  const sameYear = d.getFullYear() === now.getFullYear()
  if (sameYear) return `${d.getMonth() + 1}/${d.getDate()} ${pad(d.getHours())}:${pad(d.getMinutes())}`
  return `${d.getFullYear()}/${d.getMonth() + 1}/${d.getDate()} ${pad(d.getHours())}:${pad(d.getMinutes())}`
}

function dateGroupLabel(time) {
  if (!time) return ''
  const d = new Date(time)
  if (Number.isNaN(d.getTime())) return ''
  const now = new Date()
  const dayMs = 24 * 60 * 60 * 1000
  const startOfToday = new Date(now.getFullYear(), now.getMonth(), now.getDate()).getTime()
  const diff = Math.floor((startOfToday - new Date(d.getFullYear(), d.getMonth(), d.getDate()).getTime()) / dayMs)
  if (diff <= 0) return '今天'
  if (diff === 1) return '昨天'
  if (diff === 2) return '前天'
  if (d.getFullYear() === now.getFullYear()) return `${d.getMonth() + 1}月${d.getDate()}日`
  return `${d.getFullYear()}年${d.getMonth() + 1}月${d.getDate()}日`
}

function shouldShowDateDivider(prev, curr) {
  if (!prev || !curr) return true
  const a = new Date(prev.createTime)
  const b = new Date(curr.createTime)
  if (Number.isNaN(a.getTime()) || Number.isNaN(b.getTime())) return false
  return a.toDateString() !== b.toDateString()
}

async function loadHistory(sessionId) {
  // 请求序号递增，异步返回后比对，丢弃过时请求的结果
  const seq = ++historyReqSeq
  loadingHistory.value = true
  messages.value = []
  try {
    const res = await getHistory(sessionId, { pageNum: 1, pageSize: 100 })
    // 快速切换会话期间若有更新的 loadHistory 发起，丢弃本次结果
    if (seq !== historyReqSeq) return
    if (res.code === 200 && res.data) {
      // 后端 ORDER BY id ASC，直接按返回顺序展示（旧→新）
      messages.value = res.data.rows || res.data.records || []
      scrollToBottom()
    }
  } catch (e) {
    if (seq !== historyReqSeq) return
    // 静默失败
  } finally {
    if (seq === historyReqSeq) {
      loadingHistory.value = false
    }
  }
}

function scrollToBottom(smooth = false) {
  nextTick(() => {
    const el = scrollContainer.value
    if (!el) return
    el.scrollTo({ top: el.scrollHeight, behavior: smooth ? 'smooth' : 'auto' })
  })
}

async function selectSession(s) {
  if (!s) return
  activeSessionId.value = s.id
  await loadHistory(s.id)
  // 进入会话即标记已读，清零未读并同步头部铃铛
  if (s.unreadCount && s.unreadCount > 0) {
    try {
      await markSessionRead(s.id)
      s.unreadCount = 0
      messageStore.loadUnread()
    } catch (e) {
      // 标记失败：仍乐观清零当前会话红点（管理员已在查看），并同步铃铛避免长期不一致
      s.unreadCount = 0
      messageStore.loadUnread()
    }
  }
  regenerateSuggestions()
}

async function refreshActive() {
  if (!activeSessionId.value) return
  await loadHistory(activeSessionId.value)
}

// ===== 发送消息 =====
async function send() {
  const content = inputText.value.trim()
  if (!content || sending.value) return
  const session = activeSession.value
  if (!session || !session.peerUser) {
    ElMessage.warning('无法获取对方信息')
    return
  }
  sending.value = true
  try {
    const res = await sendMessage({
      receiverId: session.peerUser.id,
      // 显式回传对方类型，避免后端默认按 portal 处理
      receiverType: peerType(session),
      content,
      msgType: 'text'
    })
    if (res.code === 200 && res.data) {
      messages.value.push(res.data)
      // 更新会话最后消息预览并置顶
      session.lastMessageContent = content.length > 50 ? content.slice(0, 50) : content
      session.lastMessageTime = res.data.createTime
      moveToTop(session)
      inputText.value = ''
      scrollToBottom(true)
    } else {
      ElMessage.error(res.msg || '发送失败')
    }
  } catch (e) {
    ElMessage.error('发送失败，请重试')
  } finally {
    sending.value = false
  }
}

function moveToTop(session) {
  const idx = sessions.value.findIndex(s => s.id === session.id)
  if (idx > 0) {
    sessions.value.splice(idx, 1)
    sessions.value.unshift(session)
  }
}

function onKeydown(e) {
  if (e.key === 'Enter' && !e.shiftKey) {
    e.preventDefault()
    send()
  }
}

// ===== 轮询：感知门户用户回复（管理员无 WS 通道，靠轮询） =====
let pollTimer = null
const POLL_INTERVAL = 15000

/** 判断用户是否在消息列表底部附近，用于决定新消息到达时是否自动滚动 */
function isNearBottom() {
  const el = scrollContainer.value
  if (!el) return true
  // 距底部 120px 以内视为在底部
  return el.scrollHeight - el.scrollTop - el.clientHeight < 120
}

async function pollNewMessages() {
  // 仅当有选中会话时轮询当前会话新消息（无消息也轮询，感知对方发来的第一条）
  if (!activeSessionId.value) return
  const sid = activeSessionId.value
  try {
    const maxId = messages.value.reduce((max, m) => {
      const id = Number(m.id)
      return !isNaN(id) && id > max ? id : max
    }, 0)
    const res = await getHistory(sid, { pageNum: 1, pageSize: 100 })
    // 切换会话期间丢弃过时结果
    if (sid !== activeSessionId.value) return
    if (res.code === 200 && res.data) {
      const items = res.data.rows || res.data.records || []
      const newOnes = items.filter(m => Number(m.id) > maxId)
      if (newOnes.length > 0) {
        messages.value.push(...newOnes)
        // 仅当用户在底部时才自动滚动，避免打断用户向上翻看历史
        if (isNearBottom()) {
          scrollToBottom(true)
        }
        // 管理员正在查看，自动标记已读，同步铃铛
        await markSessionRead(sid)
        const s = sessions.value.find(x => x.id === sid)
        if (s) {
          s.unreadCount = 0
          s.lastMessageContent = newOnes[newOnes.length - 1].content
          s.lastMessageTime = newOnes[newOnes.length - 1].createTime
        }
        messageStore.loadUnread()
        regenerateSuggestions()
      }
    }
  } catch (e) {
    // 轮询失败忽略
  }
}

function startPolling() {
  stopPolling()
  pollTimer = setInterval(pollNewMessages, POLL_INTERVAL)
}

function stopPolling() {
  if (pollTimer) {
    clearInterval(pollTimer)
    pollTimer = null
  }
}

// ===== AI 辅助回复（mock，不接真实 AI 服务） =====
const aiSuggestions = ref([])
const aiLoading = ref(false)
// 保存 AI 建议生成的 setTimeout 句柄，快速切会话时取消上一个，避免竞态污染
let aiTimer = null

/**
 * 根据对方最近一条消息内容，按关键词命中匹配模板生成建议。
 * 后续接入真实 AI 时，替换此方法为后端 /system/message/ai-suggest 调用即可。
 */
function generateSuggestions() {
  const lastIncoming = [...messages.value].reverse().find(m => !isMine(m))
  const text = (lastIncoming && lastIncoming.content ? lastIncoming.content : '').toLowerCase()
  const pool = []

  if (/(你好|您好|hi|hello|在吗|在不在)/.test(text)) {
    pool.push('您好，我是客服，请问有什么可以帮您？')
    pool.push('您好，很高兴为您服务，请描述您遇到的问题。')
  }
  if (/(投诉|举报|违规|不良)/.test(text)) {
    pool.push('收到您的反馈，我们已记录并会尽快核实处理，请提供相关链接或订单号。')
    pool.push('非常抱歉给您带来不好的体验，我们会在 24 小时内反馈处理结果。')
  }
  if (/(退款|退钱|退货|退款进度)/.test(text)) {
    pool.push('您的退款申请我们已收到，核实后会在 1-3 个工作日内原路退回。')
    pool.push('请提供订单号，我们帮您加急核实退款进度。')
  }
  if (/(密码|登录|登不上|无法登录|登入)/.test(text)) {
    pool.push('建议您先尝试点击登录页的"忘记密码"重置，若仍无法登录请提供账号名我们排查。')
  }
  if (/(谢谢|感谢|多谢)/.test(text)) {
    pool.push('不客气，感谢您的反馈，祝您生活愉快！')
  }
  if (/(怎么|如何|请问|帮忙|帮助)/.test(text)) {
    pool.push('请您提供更多详细信息，以便我们准确为您处理。')
  }

  // 兜底通用建议
  if (pool.length === 0) {
    pool.push('收到，我们正在核实您的问题，请稍等。')
    pool.push('请您提供更多详细信息，以便我们准确处理。')
    pool.push('感谢您的反馈，我们会尽快处理并回复您。')
  }

  // 随机取 3 条
  return pool.sort(() => Math.random() - 0.5).slice(0, 3)
}

function regenerateSuggestions() {
  // 取消上一个未触发的 setTimeout，避免快速切会话时旧回调污染新会话的建议列表
  if (aiTimer) {
    clearTimeout(aiTimer)
    aiTimer = null
  }
  if (!activeSessionId.value) {
    aiSuggestions.value = []
    return
  }
  // 捕获当前会话 ID，回调时比对，丢弃切换后的过时结果
  const sid = activeSessionId.value
  aiLoading.value = true
  // 模拟 AI 思考延迟，真实接入后由网络请求耗时体现
  aiTimer = setTimeout(() => {
    aiTimer = null
    if (sid !== activeSessionId.value) return
    aiSuggestions.value = generateSuggestions()
    aiLoading.value = false
  }, 400)
}

function adoptSuggestion(text) {
  inputText.value = text
}

// ===== 生命周期 =====
onMounted(async () => {
  await loadSessions()
  messageStore.loadUnread()
  startPolling()
  // 支持从头部铃铛带 ?session=id 跳转，自动定位会话
  const sid = route.query.session
  if (sid) {
    const target = sessions.value.find(s => String(s.id) === String(sid))
    if (target) {
      await selectSession(target)
    }
  }
})

onUnmounted(() => {
  stopPolling()
  if (aiTimer) {
    clearTimeout(aiTimer)
    aiTimer = null
  }
})

// 路由 query 变化（同一页面内通过铃铛再次进入）时重新定位
watch(() => route.query.session, async (sid) => {
  if (sid && String(sid) !== String(activeSessionId.value || '')) {
    const target = sessions.value.find(s => String(s.id) === String(sid))
    if (target) {
      await selectSession(target)
    }
  }
})
</script>

<style lang="scss" scoped>
.message-center {
  display: flex;
  height: calc(100vh - 84px);
  background: #fff;
  border-radius: 4px;
  overflow: hidden;
}

// ===== 会话列表 =====
.session-panel {
  width: 280px;
  flex-shrink: 0;
  border-right: 1px solid #e4e7ed;
  display: flex;
  flex-direction: column;
  background: #fafafa;

  .panel-header {
    display: flex;
    align-items: center;
    justify-content: space-between;
    padding: 12px 16px;
    border-bottom: 1px solid #e4e7ed;
    background: #fff;

    .panel-title {
      font-size: 14px;
      font-weight: 600;
      color: #303133;
    }
  }

  .search-box {
    padding: 10px 12px;
    background: #fff;
    border-bottom: 1px solid #f0f0f0;
  }

  .session-list {
    flex: 1;
    overflow-y: auto;

    .empty-tip {
      padding: 40px 0;
      text-align: center;
      color: #909399;
      font-size: 13px;
    }

    .session-item {
      display: flex;
      align-items: flex-start;
      gap: 10px;
      padding: 12px 14px;
      cursor: pointer;
      border-bottom: 1px solid #f5f5f5;
      transition: background 0.2s;

      &:hover {
        background: #ecf5ff;
      }

      &.active {
        background: #d9ecff;
      }

      .session-info {
        flex: 1;
        min-width: 0;

        .session-top {
          display: flex;
          justify-content: space-between;
          align-items: center;
          margin-bottom: 4px;

          .peer-name {
            font-size: 13px;
            font-weight: 500;
            color: #303133;
            overflow: hidden;
            text-overflow: ellipsis;
            white-space: nowrap;
            max-width: 140px;
            display: inline-flex;
            align-items: center;
            gap: 4px;

            .peer-type-tag {
              transform: scale(0.85);
              transform-origin: left center;
            }
          }

          .session-time {
            font-size: 11px;
            color: #909399;
            flex-shrink: 0;
          }
        }

        .session-bottom {
          display: flex;
          justify-content: space-between;
          align-items: center;

          .preview-text {
            font-size: 12px;
            color: #909399;
            overflow: hidden;
            text-overflow: ellipsis;
            white-space: nowrap;
            flex: 1;
            margin-right: 8px;
          }

          .unread-dot {
            background: #f56c6c;
            color: #fff;
            font-size: 11px;
            min-width: 18px;
            height: 18px;
            line-height: 18px;
            border-radius: 9px;
            text-align: center;
            padding: 0 5px;
            flex-shrink: 0;
          }
        }
      }
    }
  }
}

// ===== 聊天详情 =====
.chat-panel {
  flex: 1;
  min-width: 0;
  display: flex;
  flex-direction: column;
  background: #fff;

  .chat-header {
    display: flex;
    align-items: center;
    justify-content: space-between;
    padding: 12px 20px;
    border-bottom: 1px solid #e4e7ed;

    .peer-info {
      display: flex;
      align-items: center;
      gap: 10px;

      .peer-meta {
        display: flex;
        align-items: center;
        gap: 8px;

        .peer-name {
          font-size: 15px;
          font-weight: 600;
          color: #303133;
        }
      }
    }
  }

  .message-list {
    flex: 1;
    overflow-y: auto;
    padding: 16px 20px;
    background: #f5f7fa;

    .date-divider {
      display: flex;
      justify-content: center;
      margin: 12px 0;

      span {
        font-size: 12px;
        color: #909399;
        background: #e9e9eb;
        padding: 2px 12px;
        border-radius: 10px;
      }
    }

    .msg-row {
      display: flex;
      align-items: flex-end;
      gap: 8px;
      margin-bottom: 12px;

      .bubble {
        max-width: 60%;
        padding: 8px 12px;
        border-radius: 8px;
        background: #fff;
        border: 1px solid #e4e7ed;
        border-bottom-left-radius: 2px;

        .bubble-content {
          font-size: 14px;
          color: #303133;
          word-break: break-word;
          white-space: pre-wrap;
        }

        .bubble-time {
          font-size: 11px;
          color: #909399;
          margin-top: 4px;
          text-align: right;
        }
      }

      &.mine {
        flex-direction: row-reverse;

        .bubble {
          background: #409eff;
          border-color: #409eff;
          border-bottom-left-radius: 8px;
          border-bottom-right-radius: 2px;

          .bubble-content {
            color: #fff;
          }

          .bubble-time {
            color: rgba(255, 255, 255, 0.8);
          }
        }
      }
    }

    .empty-msg {
      text-align: center;
      padding: 60px 0;
      color: #909399;
      font-size: 13px;
    }
  }

  .input-area {
    display: flex;
    align-items: flex-end;
    gap: 10px;
    padding: 12px 20px;
    border-top: 1px solid #e4e7ed;
    background: #fff;

    :deep(.el-textarea__inner) {
      font-family: inherit;
    }
  }

  .no-session {
    flex: 1;
    display: flex;
    align-items: center;
    justify-content: center;
  }
}

// ===== AI 建议侧栏 =====
.ai-panel {
  width: 280px;
  flex-shrink: 0;
  border-left: 1px solid #e4e7ed;
  display: flex;
  flex-direction: column;
  background: #fafbfc;

  .ai-header {
    display: flex;
    align-items: center;
    justify-content: space-between;
    padding: 12px 16px;
    border-bottom: 1px solid #e4e7ed;
    background: #fff;

    .ai-title {
      font-size: 14px;
      font-weight: 600;
      color: #303133;
    }
  }

  .ai-tip {
    padding: 10px 16px;
    font-size: 12px;
    color: #909399;
    line-height: 1.5;
    background: #fff;
    border-bottom: 1px solid #f0f0f0;
  }

  .ai-suggestions {
    flex: 1;
    overflow-y: auto;
    padding: 12px;

    .suggestion-item {
      background: #fff;
      border: 1px solid #e4e7ed;
      border-radius: 6px;
      padding: 10px 12px;
      margin-bottom: 10px;

      .suggestion-text {
        font-size: 13px;
        color: #303133;
        line-height: 1.6;
        margin-bottom: 8px;
        white-space: pre-wrap;
      }
    }

    .ai-empty {
      text-align: center;
      padding: 40px 0;
      color: #909399;
      font-size: 13px;
    }
  }

  .ai-footer {
    padding: 10px 16px;
    border-top: 1px solid #e4e7ed;
    background: #fff;

    .el-button {
      width: 100%;
    }
  }
}

// 窄屏：隐藏 AI 侧栏，保留会话列表与聊天
@media (max-width: 1100px) {
  .ai-panel {
    display: none;
  }
}

@media (max-width: 768px) {
  .session-panel {
    width: 220px;
  }
}
</style>
