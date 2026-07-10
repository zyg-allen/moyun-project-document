import { defineStore } from 'pinia';
import { ref, computed } from 'vue';
import * as notificationApi from '@/api/notification';
import * as messageApi from '@/api/message';

/**
 * 消息中心状态存储
 *
 * 用于跨组件同步未读状态：
 * - Navbar 头部铃铛（通知）+ 消息中心按钮（私信）需要展示未读数
 * - MessagesPage 消息中心页操作已读/收到新消息后，需通知 Navbar 更新
 * - MessageChat 聊天页标记已读/发送消息后，需同步会话列表与未读数
 *
 * 未读总数 = 通知未读 + 私信未读，用于 Navbar 消息中心按钮徽章。
 */
export const useMessageStore = defineStore('message', () => {
  // 通知未读数（系统通知：评论/点赞/关注/系统消息）
  const notifUnreadCount = ref(0);
  // 私信未读数（私信会话未读消息总数）
  const msgUnreadCount = ref(0);
  // 是否正在加载（避免并发重复请求）
  const loading = ref(false);

  // 未读总数：Navbar 消息中心按钮徽章
  const totalUnread = computed(() => notifUnreadCount.value + msgUnreadCount.value);

  /** 加载通知未读数（从后端同步） */
  async function loadNotifUnread() {
    try {
      const resp = await notificationApi.getUnreadCount();
      if (resp.code === 200) {
        notifUnreadCount.value = resp.data || 0;
      }
    } catch {
      /* ignore */
    }
  }

  /** 加载私信未读数（从后端同步） */
  async function loadMsgUnread() {
    try {
      const resp = await messageApi.getUnreadMessageCount();
      if (resp.code === 200) {
        msgUnreadCount.value = resp.data || 0;
      }
    } catch {
      /* ignore */
    }
  }

  /** 加载全部未读数（通知 + 私信） */
  async function loadAllUnread() {
    if (loading.value) return;
    loading.value = true;
    try {
      await Promise.all([loadNotifUnread(), loadMsgUnread()]);
    } finally {
      loading.value = false;
    }
  }

  /** 通知：标记单条已读后，本地未读数 -1（不会小于 0） */
  function decNotifUnread(n = 1) {
    notifUnreadCount.value = Math.max(0, notifUnreadCount.value - n);
  }

  /** 通知：全部标记已读 */
  function clearNotifUnread() {
    notifUnreadCount.value = 0;
  }

  /** 私信：某会话标记已读后，本地私信未读数重置（从后端重新拉取最准确） */
  async function refreshMsgUnread() {
    await loadMsgUnread();
  }

  /** 重置（登出时调用） */
  function reset() {
    notifUnreadCount.value = 0;
    msgUnreadCount.value = 0;
  }

  return {
    notifUnreadCount,
    msgUnreadCount,
    totalUnread,
    loading,
    loadNotifUnread,
    loadMsgUnread,
    loadAllUnread,
    decNotifUnread,
    clearNotifUnread,
    refreshMsgUnread,
    reset,
  };
});
