<template>
  <div class="message-bell-container" ref="bellRef">
    <el-tooltip content="私信中心" effect="dark" placement="bottom">
      <div class="bell-wrapper hover-effect" @click="toggleDropdown">
        <el-badge :value="unreadCount" :max="99" :hidden="unreadCount === 0" class="bell-badge">
          <bell class="bell-icon" />
        </el-badge>
      </div>
    </el-tooltip>

    <!-- 下拉预览面板 -->
    <div v-if="showDropdown" class="dropdown-panel">
      <div class="dropdown-header">
        <span class="dropdown-title">私信</span>
        <el-button
          v-if="unreadCount > 0"
          type="primary"
          link
          size="small"
          @click="goAll"
        >全部 ({{ unreadCount }})</el-button>
      </div>
      <div v-loading="loading" class="dropdown-body">
        <div v-if="sessions.length === 0 && !loading" class="empty-tip">
          暂无私信
        </div>
        <div
          v-for="s in sessions"
          :key="s.id"
          class="session-item"
          @click="goChat(s)"
        >
          <el-avatar :size="36" :src="s.peerUser && s.peerUser.avatar">
            <span v-if="!(s.peerUser && s.peerUser.avatar)">
              {{ peerInitial(s) }}
            </span>
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
      </div>
      <div class="dropdown-footer" @click="goAll">查看全部私信</div>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, onMounted, onUnmounted, watch } from 'vue'
import { useRouter } from 'vue-router'
import { Bell } from '@element-plus/icons-vue'
import { listSessions, markSessionRead } from '@/api/system/message'
import useMessageStore from '@/store/modules/message'

const router = useRouter()
const messageStore = useMessageStore()
const bellRef = ref(null)
const showDropdown = ref(false)
const loading = ref(false)
// 未读数来自 store，便于私信页标记已读/发送后即时同步徽章
const unreadCount = computed(() => messageStore.unreadCount)
const sessions = ref([])

// 轮询间隔（30s），与门户端轮询频率一致，保持头部铃铛实时性
let pollTimer = null
const POLL_INTERVAL = 30000

function toggleDropdown() {
  showDropdown.value = !showDropdown.value
  if (showDropdown.value) {
    loadSessions()
  }
}

// 未读数拉取委托给 store，私信页也可调用以刷新徽章
const loadUnread = () => messageStore.loadUnread()

async function loadSessions() {
  loading.value = true
  try {
    const res = await listSessions({ pageNum: 1, pageSize: 8 })
    if (res.code === 200 && res.data) {
      sessions.value = res.data.rows || res.data.list || []
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
  router.push('/system/message')
}

async function goChat(s) {
  // 点击下拉中的会话：标记已读后跳转聊天详情
  if (s.unreadCount && s.unreadCount > 0) {
    try {
      await markSessionRead(s.id)
      s.unreadCount = 0
      await loadUnread()
    } catch (e) {
      // 标记失败不阻断跳转
    }
  }
  showDropdown.value = false
  router.push({ path: '/system/message', query: { session: s.id } })
}

function handleClickOutside(e) {
  if (bellRef.value && !bellRef.value.contains(e.target)) {
    showDropdown.value = false
  }
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

// 暴露刷新方法（聊天页发送/已读后可调用以同步徽章）
defineExpose({ refreshUnread: loadUnread })

onMounted(() => {
  loadUnread()
  startPolling()
  document.addEventListener('click', handleClickOutside)
})

onUnmounted(() => {
  stopPolling()
  document.removeEventListener('click', handleClickOutside)
})

// 路由切换时刷新未读数（从聊天页返回时同步徽章）
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

  .dropdown-header {
    display: flex;
    justify-content: space-between;
    align-items: center;
    padding: 12px 16px;
    border-bottom: 1px solid #f0f0f0;

    .dropdown-title {
      font-size: 14px;
      font-weight: 600;
      color: #303133;
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
