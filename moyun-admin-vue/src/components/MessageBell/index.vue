<template>
  <div class="message-bell-container" ref="bellRef" @mouseenter="openOnHover" @mouseleave="closeOnHover">
    <el-tooltip content="点击进入消息中心" effect="dark" placement="bottom">
      <div class="bell-wrapper hover-effect" @click="goAll">
        <el-badge :value="badgeValue" :max="99" :hidden="totalUnread === 0" class="bell-badge">
          <el-icon class="bell-icon"><Bell /></el-icon>
        </el-badge>
      </div>
    </el-tooltip>

    <!-- 下拉预览面板：分私信/通知两类 -->
    <div v-if="showDropdown" class="dropdown-panel" @click.stop>
      <div class="dropdown-tabs">
        <div
          class="tab-item"
          :class="{ active: activeTab === 'message' }"
          @click.stop="switchTab('message')"
        >
          <span>私信</span>
          <span v-if="msgUnread > 0" class="tab-badge">{{ msgUnread > 99 ? '99+' : msgUnread }}</span>
        </div>
        <div
          class="tab-item"
          :class="{ active: activeTab === 'notification' }"
          @click.stop="switchTab('notification')"
        >
          <span>通知</span>
          <span v-if="notifUnread > 0" class="tab-badge">{{ notifUnread > 99 ? '99+' : notifUnread }}</span>
        </div>
      </div>

      <!-- 私信预览 -->
      <div v-loading="loading" class="dropdown-body">
        <template v-if="activeTab === 'message'">
          <div v-if="sessions.length === 0 && !loading" class="empty-tip">暂无私信</div>
          <div
            v-for="s in sessions"
            :key="s.id"
            class="session-item"
            @click="goChat(s)"
          >
            <el-avatar :size="36" :src="s.peerUser && s.peerUser.avatar">
              <span v-if="!(s.peerUser && s.peerUser.avatar)">{{ peerInitial(s) }}</span>
            </el-avatar>
            <div class="session-main">
              <div class="session-top">
                <span class="peer-name">{{ peerName(s) }}</span>
                <span class="session-time">{{ formatTime(s.lastMessageTime) }}</span>
              </div>
              <div class="session-preview">
                <span class="preview-text">{{ s.lastMessageContent || '暂无消息' }}</span>
                <span v-if="s.unreadCount > 0" class="unread-dot">{{ s.unreadCount > 99 ? '99+' : s.unreadCount }}</span>
              </div>
            </div>
          </div>
        </template>

        <!-- 通知预览 -->
        <template v-else>
          <div v-if="notifications.length === 0 && !loading" class="empty-tip">暂无通知</div>
          <div
            v-for="n in notifications"
            :key="n.id"
            class="session-item"
            :class="{ 'is-unread': !n.isRead }"
            @click="goNotification(n)"
          >
            <div class="notif-icon-wrap">
              <el-icon class="notif-type-icon"><Bell /></el-icon>
            </div>
            <div class="session-main">
              <div class="session-top">
                <span class="peer-name">{{ n.title || '系统通知' }}</span>
                <span class="session-time">{{ formatTime(n.createTime) }}</span>
              </div>
              <div class="session-preview">
                <span class="preview-text">{{ n.content || '' }}</span>
                <span v-if="!n.isRead" class="unread-dot"></span>
              </div>
            </div>
          </div>
        </template>
      </div>

      <div class="dropdown-footer" @click.stop="goAll">
        <span v-if="activeTab === 'message'">查看全部私信</span>
        <span v-else>查看全部通知</span>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, onMounted, onUnmounted, watch } from 'vue'
import { useRouter } from 'vue-router'
import { Bell } from '@element-plus/icons-vue'
import { listSessions, markSessionRead } from '@/api/system/message'
import { listInboxNotification, markInboxAsRead } from '@/api/system/notification'
import useMessageStore from '@/store/modules/message'

const router = useRouter()
const messageStore = useMessageStore()
const bellRef = ref(null)
const showDropdown = ref(false)
const loading = ref(false)
const activeTab = ref('message')
// 悬停关闭延迟（ms），避免鼠标移动间隙导致面板闪烁
const HOVER_CLOSE_DELAY = 200
let hoverCloseTimer = null

// 未读数来自 store，私信页/通知页标记已读后可即时同步徽章
const msgUnread = computed(() => messageStore.msgUnreadCount)
const notifUnread = computed(() => messageStore.notifUnreadCount)
const totalUnread = computed(() => messageStore.totalUnread)
// el-badge 的 value：合计为 0 时隐藏徽章
const badgeValue = computed(() => totalUnread.value)

const sessions = ref([])
const notifications = ref([])

// 轮询间隔（30s），与门户端轮询频率一致，保持头部铃铛实时性
let pollTimer = null
const POLL_INTERVAL = 30000

// 鼠标悬停时展开预览面板
function openOnHover() {
  if (hoverCloseTimer) {
    clearTimeout(hoverCloseTimer)
    hoverCloseTimer = null
  }
  if (!showDropdown.value) {
    showDropdown.value = true
    loadCurrent()
  }
}

// 鼠标移出时延迟关闭预览面板（给用户移动鼠标留余量）
function closeOnHover() {
  if (hoverCloseTimer) clearTimeout(hoverCloseTimer)
  hoverCloseTimer = setTimeout(() => {
    showDropdown.value = false
  }, HOVER_CLOSE_DELAY)
}

function switchTab(tab) {
  if (activeTab.value === tab) return
  activeTab.value = tab
  loadCurrent()
}

async function loadCurrent() {
  if (activeTab.value === 'message') {
    await loadSessions()
  } else {
    await loadNotifications()
  }
}

// 未读数拉取委托给 store，私信页/通知页也可调用以刷新徽章
const loadUnread = () => messageStore.loadAllUnread()

async function loadSessions() {
  loading.value = true
  try {
    const res = await listSessions({ pageNum: 1, pageSize: 8 })
    if (res.code === 200 && res.data) {
      sessions.value = res.data.records || res.data.rows || res.data.list || []
    }
  } catch (e) {
    // 静默失败
  } finally {
    loading.value = false
  }
}

async function loadNotifications() {
  loading.value = true
  try {
    const res = await listInboxNotification({ pageNum: 1, pageSize: 8 })
    if (res.code === 200 && res.data) {
      notifications.value = res.data.rows || res.data.records || []
    }
  } catch (e) {
    // 静默失败
  } finally {
    loading.value = false
  }
}

function peerName(s) {
  const peer = s.peerUser || {}
  return peer.nickname || peer.username || '未知用户'
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
  const sameDay = d.toDateString() === now.toDateString()
  const pad = (n) => String(n).padStart(2, '0')
  if (sameDay) return `${pad(d.getHours())}:${pad(d.getMinutes())}`
  return `${d.getMonth() + 1}/${d.getDate()} ${pad(d.getHours())}:${pad(d.getMinutes())}`
}

function goAll() {
  showDropdown.value = false
  // 点击铃铛直接跳转消息中心，并定位到当前 Tab
  router.push({ path: '/system/message', query: { tab: activeTab.value } })
}

async function goChat(s) {
  // 点击下拉中的会话：标记已读后跳转聊天详情
  if (s.unreadCount && s.unreadCount > 0) {
    try {
      await markSessionRead(s.id)
      s.unreadCount = 0
      await messageStore.loadMsgUnread()
    } catch (e) {
      // 标记失败不阻断跳转
    }
  }
  showDropdown.value = false
  router.push({ path: '/system/message', query: { session: s.id, tab: 'message' } })
}

async function goNotification(n) {
  // 点击下拉中的通知：标记已读后跳转通知详情
  if (!n.isRead) {
    try {
      await markInboxAsRead(n.id)
      n.isRead = true
      await messageStore.loadNotifUnread()
    } catch (e) {
      // 标记失败不阻断跳转
    }
  }
  showDropdown.value = false
  router.push({ path: '/system/message', query: { tab: 'notification', nid: n.id } })
}

function startPolling() {
  stopPolling()
  pollTimer = setInterval(loadUnread, POLL_INTERVAL)
}

function stopPolling() {
  if (pollTimer) {
    clearInterval(pollTimer)
    pollTimer = null
  }
}

// 暴露刷新方法（聊天页/通知页发送/已读后可调用以同步徽章）
defineExpose({ refreshUnread: loadUnread })

onMounted(() => {
  loadUnread()
  startPolling()
})

onUnmounted(() => {
  stopPolling()
  if (hoverCloseTimer) clearTimeout(hoverCloseTimer)
})

// 路由切换时刷新未读数（从消息中心返回时同步徽章）
watch(() => router.currentRoute.value.path, () => {
  loadUnread()
})
</script>

<style lang="scss" scoped>
.message-bell-container {
  position: relative;
  display: inline-block;
  height: 100%;
}

.bell-wrapper {
  display: flex;
  align-items: center;
  justify-content: center;
  height: 100%;
  padding: 0 8px;
  cursor: pointer;
  transition: background 0.3s;

  &:hover {
    background: rgba(0, 0, 0, 0.025);
  }

  .bell-icon {
    font-size: 18px;
    color: #5a5e66;
  }
}

.dropdown-panel {
  position: absolute;
  top: 50px;
  right: 0;
  width: 360px;
  // 移动端窄屏限制：避免下拉面板横向溢出视口
  max-width: calc(100vw - 20px);
  background: #fff;
  border: 1px solid #e4e7ed;
  border-radius: 4px;
  box-shadow: 0 2px 12px 0 rgba(0, 0, 0, 0.1);
  z-index: 2000;

  .dropdown-tabs {
    display: flex;
    border-bottom: 1px solid #f0f0f0;

    .tab-item {
      flex: 1;
      padding: 12px 0;
      text-align: center;
      font-size: 13px;
      color: #606266;
      cursor: pointer;
      transition: color 0.2s;
      position: relative;

      &:hover {
        color: #409eff;
      }

      &.active {
        color: #409eff;
        font-weight: 600;

        &::after {
          content: '';
          position: absolute;
          bottom: -1px;
          left: 30%;
          width: 40%;
          height: 2px;
          background: #409eff;
        }
      }

      .tab-badge {
        display: inline-block;
        margin-left: 4px;
        background: #f56c6c;
        color: #fff;
        font-size: 11px;
        min-width: 16px;
        height: 16px;
        line-height: 16px;
        border-radius: 8px;
        text-align: center;
        padding: 0 4px;
      }
    }
  }

  .dropdown-body {
    max-height: 360px;
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
      padding: 10px 16px;
      cursor: pointer;
      transition: background 0.2s;

      &:hover {
        background: #f5f7fa;
      }

      &.is-unread {
        background: #fef0f0;
      }

      .notif-icon-wrap {
        width: 36px;
        height: 36px;
        border-radius: 50%;
        background: #ecf5ff;
        display: flex;
        align-items: center;
        justify-content: center;
        flex-shrink: 0;

        .notif-type-icon {
          font-size: 18px;
          color: #409eff;
        }
      }

      .session-main {
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
            max-width: 180px;
          }

          .session-time {
            font-size: 11px;
            color: #909399;
            flex-shrink: 0;
          }
        }

        .session-preview {
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

            &:empty {
              min-width: 8px;
              height: 8px;
              line-height: 8px;
              border-radius: 50%;
              padding: 0;
            }
          }
        }
      }
    }
  }

  .dropdown-footer {
    padding: 10px;
    text-align: center;
    font-size: 13px;
    color: #409eff;
    cursor: pointer;
    border-top: 1px solid #f0f0f0;

    &:hover {
      background: #f5f7fa;
    }
  }
}
</style>
