import { defineStore } from 'pinia'
import { ref, computed } from 'vue'
import { getUnreadCount } from '@/api/system/message'
import { getInboxUnreadCount } from '@/api/system/notification'

/**
 * 后台消息中心 store（私信 + 通知）
 *
 * 用于头部 MessageBell 铃铛与消息中心菜单页之间的未读数同步：
 * - 铃铛组件挂载/轮询时调用 loadAllUnread
 * - 私信页标记已读/发送消息后调用 loadMsgUnread 刷新徽章
 * - 通知页标记已读后调用 loadNotifUnread 刷新徽章
 *
 * 对齐前台门户 stores/message.ts 的模式（msgUnreadCount + notifUnreadCount + totalUnread）。
 * 兼容：保留 unreadCount / loadUnread 作为合计的别名，旧调用方无需改动。
 */
const useMessageStore = defineStore('admin-message', () => {
  /** 当前管理员私信总未读数 */
  const msgUnreadCount = ref(0)
  /** 当前管理员通知总未读数（个人 + 广播） */
  const notifUnreadCount = ref(0)
  /** 合计未读数（私信 + 通知） */
  const totalUnread = computed(() => msgUnreadCount.value + notifUnreadCount.value)
  /** 兼容旧调用：合计未读数 */
  const unreadCount = computed(() => totalUnread.value)

  /** 拉取私信未读数 */
  async function loadMsgUnread() {
    try {
      const res = await getUnreadCount()
      if (res && res.code === 200) {
        // 强制数值类型，避免后端返回字符串导致 el-badge 比较异常
        msgUnreadCount.value = Number(res.data) || 0
      }
    } catch (e) {
      // 静默失败，不打断业务流程
    }
  }

  /** 拉取通知未读数 */
  async function loadNotifUnread() {
    try {
      const res = await getInboxUnreadCount()
      if (res && res.code === 200) {
        notifUnreadCount.value = Number(res.data) || 0
      }
    } catch (e) {
      // 静默失败
    }
  }

  /** 并发拉取私信 + 通知未读数 */
  async function loadAllUnread() {
    await Promise.all([loadMsgUnread(), loadNotifUnread()])
  }

  /** 兼容旧调用：拉取全部未读数 */
  const loadUnread = loadAllUnread

  /** 通知已读后递减本地未读数（乐观更新，避免整页轮询） */
  function decNotifUnread(n = 1) {
    notifUnreadCount.value = Math.max(0, notifUnreadCount.value - n)
  }

  /** 清零通知未读数（全部已读后调用） */
  function clearNotifUnread() {
    notifUnreadCount.value = 0
  }

  return {
    msgUnreadCount,
    notifUnreadCount,
    totalUnread,
    // 兼容旧调用
    unreadCount,
    loadMsgUnread,
    loadNotifUnread,
    loadAllUnread,
    loadUnread,
    decNotifUnread,
    clearNotifUnread
  }
})

export default useMessageStore
