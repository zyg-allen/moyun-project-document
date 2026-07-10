import { defineStore } from 'pinia'
import { ref } from 'vue'
import { getUnreadCount } from '@/api/system/message'

/**
 * 后台私信状态 store
 *
 * 用于头部 MessageBell 铃铛与私信中心菜单页之间的未读数同步：
 * - 铃铛组件挂载/轮询时调用 loadUnread
 * - 私信页标记已读/发送消息后调用 loadUnread 刷新徽章
 *
 * 与门户端 stores/message.ts 保持一致的模式。
 */
const useMessageStore = defineStore('admin-message', () => {
  /** 当前管理员私信总未读数 */
  const unreadCount = ref(0)

  /** 拉取最新未读数 */
  async function loadUnread() {
    try {
      const res = await getUnreadCount()
      if (res && res.code === 200) {
        // 强制数值类型，避免后端返回字符串导致 el-badge 比较异常
        unreadCount.value = Number(res.data) || 0
      }
    } catch (e) {
      // 静默失败，不打断业务流程
    }
  }

  return { unreadCount, loadUnread }
})

export default useMessageStore
