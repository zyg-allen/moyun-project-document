<template>
  <div class="message-center-wrap">
    <!-- 顶部主 Tab：私信 / 通知 -->
    <el-tabs v-model="activeMainTab" class="msg-tabs" @tab-change="onMainTabChange">
      <el-tab-pane name="message">
        <template #label>
          <span>私信</span>
          <el-badge
            v-if="msgUnreadTotal > 0"
            :value="msgUnreadTotal > 99 ? '99+' : msgUnreadTotal"
            class="tab-badge"
          />
        </template>

        <!-- 私信：原三栏布局 -->
        <div class="message-center">
          <!-- 左侧：会话列表 -->
          <div class="session-panel">
            <div class="panel-header">
              <span class="panel-title">私信会话</span>
              <el-badge
                :value="msgUnreadTotal"
                :max="99"
                :hidden="msgUnreadTotal === 0"
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
      </el-tab-pane>

      <el-tab-pane name="notification">
        <template #label>
          <span>通知</span>
          <el-badge
            v-if="inboxUnread > 0"
            :value="inboxUnread > 99 ? '99+' : inboxUnread"
            class="tab-badge"
          />
        </template>

        <!-- 通知中心：我的通知 / 全站通知 -->
        <div class="notification-center">
          <div class="notif-toolbar">
            <el-radio-group v-model="notifSubTab" @change="onNotifSubChange">
              <el-radio-button label="inbox">我的通知</el-radio-button>
              <el-radio-button label="all">全站通知</el-radio-button>
            </el-radio-group>
            <div class="notif-actions">
              <el-button
                v-if="notifSubTab === 'inbox' && inboxUnread > 0"
                type="primary"
                plain
                size="small"
                @click="markAllRead"
              >全部已读</el-button>
              <el-tooltip content="刷新" placement="top">
                <el-button :icon="Refresh" circle size="small" @click="loadNotifList" />
              </el-tooltip>
            </div>
          </div>

          <div v-loading="notifLoading" class="notif-list">
            <div v-if="notifList.length === 0 && !notifLoading" class="notif-empty">
              <el-empty description="暂无通知" />
            </div>
            <div
              v-for="n in notifList"
              :key="n.id"
              class="notif-item"
              :class="{ unread: !n.isRead && notifSubTab === 'inbox' }"
              @click="openNotif(n)"
            >
              <div class="notif-item-icon" :class="'type-' + (n.type || 'system')">
                <el-icon><Bell /></el-icon>
              </div>
              <div class="notif-item-body">
                <div class="notif-item-top">
                  <span class="notif-item-title">{{ n.title || '系统通知' }}</span>
                  <span class="notif-item-time">{{ formatTime(n.createTime) }}</span>
                </div>
                <div class="notif-item-content">{{ n.content || '' }}</div>
                <div class="notif-item-meta">
                  <el-tag size="small" :type="typeTagType(n.type)" effect="plain">{{ typeText(n.type) }}</el-tag>
                  <el-tag v-if="n.scope === 'all'" size="small" type="warning" effect="plain">广播</el-tag>
                  <el-tag v-if="n.userType === 'portal'" size="small" type="info" effect="plain">门户用户</el-tag>
                  <el-tag v-else-if="n.userType === 'sys'" size="small" type="success" effect="plain">系统用户</el-tag>
                </div>
              </div>
              <span v-if="!n.isRead && notifSubTab === 'inbox'" class="unread-dot"></span>
            </div>
          </div>
        </div>
      </el-tab-pane>
    </el-tabs>

    <!-- 通知详情弹窗 -->
    <el-dialog v-model="notifDetailVisible" title="通知详情" width="560px" append-to-body>
      <div v-if="currentNotif" class="notif-detail">
        <h3 class="notif-detail-title">{{ currentNotif.title || '系统通知' }}</h3>
        <div class="notif-detail-meta">
          <el-tag size="small" :type="typeTagType(currentNotif.type)" effect="plain">{{ typeText(currentNotif.type) }}</el-tag>
          <el-tag v-if="currentNotif.scope === 'all'" size="small" type="warning" effect="plain">广播</el-tag>
          <span class="notif-detail-time">{{ formatTime(currentNotif.createTime) }}</span>
        </div>
        <div class="notif-detail-content">{{ currentNotif.content || '（无内容）' }}</div>
      </div>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, computed, onMounted, onUnmounted, nextTick, watch } from 'vue'
import { useRoute } from 'vue-router'
import { Search, Refresh, Bell } from '@element-plus/icons-vue'
import { ElMessage } from 'element-plus'
import {
  listSessions,
  getHistory,
  sendMessage,
  markSessionRead
} from '@/api/system/message'
import {
  listInboxNotification,
  listNotification,
  markInboxAsRead,
  markInboxAllAsRead
} from '@/api/system/notification'
import useMessageStore from '@/store/modules/message'

const route = useRoute()
const messageStore = useMessageStore()

// ===== 主 Tab：私信 / 通知 =====
const activeMainTab = ref('message')

function onMainTabChange(tab) {
  if (tab === 'notification') {
    loadNotifList()
  }
}

// ===== 私信：会话列表 =====
const sessions = ref([])
const loadingSessions = ref(false)
const keyword = ref('')
const activeSessionId = ref(null)
const activeSession = computed(() =>
  sessions.value.find(s => s.id === activeSessionId.value) || null
)

const msgUnreadTotal = computed(() => messageStore.msgUnreadCount)
const inboxUnread = computed(() => messageStore.notifUnreadCount)

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
let historyReqSeq = 0

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
  const seq = ++historyReqSeq
  loadingHistory.value = true
  messages.value = []
  try {
    const res = await getHistory(sessionId, { pageNum: 1, pageSize: 100 })
    if (seq !== historyReqSeq) return
    if (res.code === 200 && res.data) {
      messages.value = res.data.rows || res.data.records || []
      scrollToBottom()
    }
  } catch (e) {
    if (seq !== historyReqSeq) return
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
  if (s.unreadCount && s.unreadCount > 0) {
    try {
      await markSessionRead(s.id)
      s.unreadCount = 0
      messageStore.loadMsgUnread()
    } catch (e) {
      s.unreadCount = 0
      messageStore.loadMsgUnread()
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
      receiverType: peerType(session),
      content,
      msgType: 'text'
    })
    if (res.code === 200 && res.data) {
      messages.value.push(res.data)
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

// ===== 轮询：感知门户用户回复 =====
let pollTimer = null
const POLL_INTERVAL = 15000

function isNearBottom() {
  const el = scrollContainer.value
  if (!el) return true
  return el.scrollHeight - el.scrollTop - el.clientHeight < 120
}

async function pollNewMessages() {
  if (!activeSessionId.value) return
  if (activeMainTab.value !== 'message') return
  const sid = activeSessionId.value
  try {
    const maxId = messages.value.reduce((max, m) => {
      const id = Number(m.id)
      return !isNaN(id) && id > max ? id : max
    }, 0)
    const res = await getHistory(sid, { pageNum: 1, pageSize: 100 })
    if (sid !== activeSessionId.value) return
    if (res.code === 200 && res.data) {
      const items = res.data.rows || res.data.records || []
      const newOnes = items.filter(m => Number(m.id) > maxId)
      if (newOnes.length > 0) {
        messages.value.push(...newOnes)
        if (isNearBottom()) {
          scrollToBottom(true)
        }
        await markSessionRead(sid)
        const s = sessions.value.find(x => x.id === sid)
        if (s) {
          s.unreadCount = 0
          s.lastMessageContent = newOnes[newOnes.length - 1].content
          s.lastMessageTime = newOnes[newOnes.length - 1].createTime
        }
        messageStore.loadMsgUnread()
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

// ===== AI 辅助回复（mock） =====
const aiSuggestions = ref([])
const aiLoading = ref(false)
let aiTimer = null

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

  if (pool.length === 0) {
    pool.push('收到，我们正在核实您的问题，请稍等。')
    pool.push('请您提供更多详细信息，以便我们准确处理。')
    pool.push('感谢您的反馈，我们会尽快处理并回复您。')
  }

  return pool.sort(() => Math.random() - 0.5).slice(0, 3)
}

function regenerateSuggestions() {
  if (aiTimer) {
    clearTimeout(aiTimer)
    aiTimer = null
  }
  if (!activeSessionId.value) {
    aiSuggestions.value = []
    return
  }
  const sid = activeSessionId.value
  aiLoading.value = true
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

// ===== 通知 Tab =====
const notifSubTab = ref('inbox')
const notifList = ref([])
const notifLoading = ref(false)
const notifDetailVisible = ref(false)
const currentNotif = ref(null)

function onNotifSubChange() {
  notifList.value = []
  loadNotifList()
}

async function loadNotifList() {
  notifLoading.value = true
  try {
    let res
    if (notifSubTab.value === 'inbox') {
      res = await listInboxNotification({ pageNum: 1, pageSize: 50 })
    } else {
      // 全站通知台账（管理员视角，只读浏览）
      res = await listNotification({ pageNum: 1, pageSize: 50 })
    }
    if (res.code === 200 && res.data) {
      notifList.value = res.data.rows || res.data.records || []
    }
  } catch (e) {
    // 静默失败
  } finally {
    notifLoading.value = false
  }
}

async function openNotif(n) {
  currentNotif.value = n
  notifDetailVisible.value = true
  // "我的通知"中未读的，标记已读
  if (notifSubTab.value === 'inbox' && !n.isRead) {
    try {
      await markInboxAsRead(n.id)
      n.isRead = true
      messageStore.loadNotifUnread()
    } catch (e) {
      // 标记失败不阻断查看
    }
  }
}

async function markAllRead() {
  try {
    const res = await markInboxAllAsRead()
    if (res.code === 200) {
      ElMessage.success('已全部标记为已读')
      notifList.value.forEach(n => { n.isRead = true })
      messageStore.clearNotifUnread()
    } else {
      ElMessage.error(res.msg || '操作失败')
    }
  } catch (e) {
    ElMessage.error('操作失败，请重试')
  }
}

// 通知类型展示
function typeText(type) {
  const map = {
    system: '系统',
    comment: '评论',
    like: '点赞',
    follow: '关注',
    order: '订单',
    notice: '通知',
    announcement: '公告',
    mention: '提及'
  }
  return map[type] || '通知'
}

function typeTagType(type) {
  const map = {
    system: 'info',
    comment: '',
    like: 'danger',
    follow: 'success',
    order: 'warning',
    notice: 'info',
    announcement: 'warning',
    mention: 'success'
  }
  return map[type] || 'info'
}

// ===== 生命周期 =====
onMounted(async () => {
  // 支持从头部铃铛带 tab 参数跳转
  const qTab = route.query.tab
  if (qTab === 'notification') {
    activeMainTab.value = 'notification'
  }
  await loadSessions()
  messageStore.loadAllUnread()
  startPolling()

  // 私信会话定位
  const sid = route.query.session
  if (sid && activeMainTab.value === 'message') {
    const target = sessions.value.find(s => String(s.id) === String(sid))
    if (target) {
      await selectSession(target)
    }
  }
  // 通知定位
  if (activeMainTab.value === 'notification') {
    await loadNotifList()
    const nid = route.query.nid
    if (nid) {
      const target = notifList.value.find(n => String(n.id) === String(nid))
      if (target) {
        openNotif(target)
      }
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

// 路由 query 变化（铃铛再次进入）时重新定位
watch(() => route.query, async (q) => {
  if (q.tab === 'notification' && activeMainTab.value !== 'notification') {
    activeMainTab.value = 'notification'
    await loadNotifList()
  } else if (q.tab === 'message' && activeMainTab.value !== 'message') {
    activeMainTab.value = 'message'
  }
  if (q.session && String(q.session) !== String(activeSessionId.value || '')) {
    const target = sessions.value.find(s => String(s.id) === String(q.session))
    if (target) {
      await selectSession(target)
    }
  }
}, { deep: true })
</script>

<style lang="scss" scoped>
.message-center-wrap {
  background: #fff;
  border-radius: 4px;
  padding: 0 12px;

  .msg-tabs {
    :deep(.el-tabs__header) {
      margin-bottom: 0;
      padding: 0 8px;
      background: #fff;
    }

    .tab-badge {
      margin-left: 6px;
      margin-top: -2px;
    }
  }
}

.message-center {
  display: flex;
  height: calc(100vh - 130px);
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

// ===== 通知中心 =====
.notification-center {
  height: calc(100vh - 130px);
  display: flex;
  flex-direction: column;
  background: #fff;

  .notif-toolbar {
    display: flex;
    align-items: center;
    justify-content: space-between;
    padding: 12px 16px;
    border-bottom: 1px solid #f0f0f0;

    .notif-actions {
      display: flex;
      gap: 8px;
      align-items: center;
    }
  }

  .notif-list {
    flex: 1;
    overflow-y: auto;
    padding: 8px 16px;

    .notif-empty {
      padding: 60px 0;
    }

    .notif-item {
      display: flex;
      align-items: flex-start;
      gap: 12px;
      padding: 14px 12px;
      border-bottom: 1px solid #f5f5f5;
      cursor: pointer;
      transition: background 0.2s;
      border-radius: 6px;

      &:hover {
        background: #f5f7fa;
      }

      &.unread {
        background: #fef0f0;

        &:hover {
          background: #fee;
        }
      }

      .notif-item-icon {
        width: 40px;
        height: 40px;
        border-radius: 50%;
        display: flex;
        align-items: center;
        justify-content: center;
        flex-shrink: 0;
        background: #ecf5ff;
        color: #409eff;
        font-size: 18px;

        &.type-system { background: #ecf5ff; color: #409eff; }
        &.type-comment { background: #fdf6ec; color: #e6a23c; }
        &.type-like { background: #fef0f0; color: #f56c6c; }
        &.type-follow { background: #f0f9eb; color: #67c23a; }
        &.type-mention { background: #f0f9eb; color: #67c23a; }
        &.type-announcement { background: #fdf6ec; color: #e6a23c; }
      }

      .notif-item-body {
        flex: 1;
        min-width: 0;

        .notif-item-top {
          display: flex;
          justify-content: space-between;
          align-items: center;
          margin-bottom: 6px;

          .notif-item-title {
            font-size: 14px;
            font-weight: 600;
            color: #303133;
            overflow: hidden;
            text-overflow: ellipsis;
            white-space: nowrap;
            max-width: 60%;
          }

          .notif-item-time {
            font-size: 12px;
            color: #909399;
            flex-shrink: 0;
          }
        }

        .notif-item-content {
          font-size: 13px;
          color: #606266;
          line-height: 1.5;
          overflow: hidden;
          text-overflow: ellipsis;
          display: -webkit-box;
          -webkit-line-clamp: 2;
          -webkit-box-orient: vertical;
          margin-bottom: 8px;
        }

        .notif-item-meta {
          display: flex;
          gap: 6px;
          flex-wrap: wrap;
        }
      }

      .unread-dot {
        width: 8px;
        height: 8px;
        background: #f56c6c;
        border-radius: 50%;
        flex-shrink: 0;
        margin-top: 16px;
      }
    }
  }
}

// ===== 通知详情弹窗 =====
.notif-detail {
  .notif-detail-title {
    margin: 0 0 12px 0;
    font-size: 18px;
    color: #303133;
  }

  .notif-detail-meta {
    display: flex;
    align-items: center;
    gap: 8px;
    margin-bottom: 16px;

    .notif-detail-time {
      font-size: 12px;
      color: #909399;
    }
  }

  .notif-detail-content {
    font-size: 14px;
    color: #303133;
    line-height: 1.8;
    white-space: pre-wrap;
    word-break: break-word;
    background: #f5f7fa;
    padding: 16px;
    border-radius: 4px;
  }
}
</style>
