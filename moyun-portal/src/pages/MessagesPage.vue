<script setup lang="ts">
import { ref, computed, onMounted, watch } from 'vue';
import { useRoute, useRouter } from 'vue-router';
import { useHead } from '@vueuse/head';
import {
    Bell, MessageSquare, Heart, UserPlus, CheckCheck, Loader2, Inbox
} from 'lucide-vue-next';
import type { Notification, MessageSessionVO, PeerUser } from '@/types/api';
import * as notificationApi from '@/api/notification';
import * as messageApi from '@/api/message';
import { useUserStore } from '@/stores/user';
import { useMessageStore } from '@/stores/message';
import { getSafeAvatar } from '@/utils/avatar';
import { useToast } from '@/composables/useToast';
import MessageChat from '@/components/MessageChat.vue';
import Breadcrumb from '@/components/Breadcrumb.vue';
import { generateSeo } from '@/utils/seo';

const route = useRoute();
const router = useRouter();
const userStore = useUserStore();
const messageStore = useMessageStore();
const toast = useToast();

useHead(
    generateSeo({
        title: '消息中心',
        description: '通知与私信',
        keywords: ['消息中心', '通知', '私信'],
        type: 'website'
    })
);

// 是否为聊天详情模式（路由 /messages/chat/:sessionId）
const isChatMode = computed(() => !!route.params.sessionId);
const chatSessionId = computed(() => String(route.params.sessionId || ''));

const activeTab = ref<'notification' | 'message'>('notification');

// ============ 通知相关 ============
const notifications = ref<Notification[]>([]);
const notifLoading = ref(false);
const notifFilter = ref<string>(''); // 全部为空
// 通知未读数从消息 store 取，与 Navbar 跨组件同步
const notifUnreadCount = computed(() => messageStore.notifUnreadCount);

const notifFilterOptions = [
    { label: '全部', value: '' },
    { label: '评论', value: 'comment' },
    { label: '点赞', value: 'like' },
    { label: '关注', value: 'follow' },
    { label: '系统', value: 'system' },
];

const filteredNotifications = computed(() => {
    if (!notifFilter.value) return notifications.value;
    return notifications.value.filter((n) => n.type === notifFilter.value);
});

function getNotifIcon(type?: string) {
    switch (type) {
        case 'comment':
            return MessageSquare;
        case 'like':
            return Heart;
        case 'follow':
            return UserPlus;
        default:
            return Bell;
    }
}

function getNotifIconColor(type?: string): string {
    switch (type) {
        case 'comment':
            return '#3b82f6';
        case 'like':
            return '#ef4444';
        case 'follow':
            return '#10b981';
        default:
            return 'var(--theme-primary)';
    }
}

function formatRelativeTime(time?: string): string {
    if (!time) return '';
    const d = new Date(time);
    if (Number.isNaN(d.getTime())) return time;
    const now = Date.now();
    const diff = now - d.getTime();
    const minute = 60 * 1000;
    const hour = 60 * minute;
    const day = 24 * hour;
    if (diff < minute) return '刚刚';
    if (diff < hour) return `${Math.floor(diff / minute)}分钟前`;
    if (diff < day) return `${Math.floor(diff / hour)}小时前`;
    if (diff < 7 * day) return `${Math.floor(diff / day)}天前`;
    const pad = (n: number) => String(n).padStart(2, '0');
    return `${d.getMonth() + 1}/${d.getDate()} ${pad(d.getHours())}:${pad(d.getMinutes())}`;
}

async function loadNotifications() {
    notifLoading.value = true;
    try {
        const resp = await notificationApi.getNotificationList({ pageNum: 1, pageSize: 50 });
        if (resp.code === 200 && resp.data) {
            notifications.value = resp.data.list || [];
        }
    } catch (error) {
        console.error('加载通知失败:', error);
        toast.error('加载通知失败，请稍后重试');
    } finally {
        notifLoading.value = false;
    }
}

async function loadNotifUnread() {
    await messageStore.loadNotifUnread();
}

async function markNotifRead(n: Notification) {
    if (n.isRead) return;
    try {
        const id = String(n.id);
        await notificationApi.markAsRead({ id });
        n.isRead = true;
        // 本地未读数 -1（store 同步给 Navbar）
        messageStore.decNotifUnread();
    } catch (error) {
        console.error('标记已读失败:', error);
        toast.error('标记已读失败');
    }
}

async function markAllNotifRead() {
    const unread = notifications.value.filter((n) => !n.isRead);
    if (unread.length === 0) {
        toast.info('没有未读通知');
        return;
    }
    try {
        await Promise.all(
            unread.map((n) => notificationApi.markAsRead({ id: String(n.id) }).catch(() => null))
        );
        unread.forEach((n) => (n.isRead = true));
        // 清空通知未读数（store 同步给 Navbar）
        messageStore.clearNotifUnread();
        toast.success('已全部标记为已读');
    } catch (error) {
        console.error('全部已读失败:', error);
        toast.error('操作失败');
    }
}

// ============ 私信会话相关 ============
const sessions = ref<MessageSessionVO[]>([]);
const sessionLoading = ref(false);
// 私信未读数从消息 store 取，与 Navbar 跨组件同步
const msgUnreadCount = computed(() => messageStore.msgUnreadCount);
const activeSession = ref<MessageSessionVO | null>(null);

const totalUnread = computed(() => messageStore.totalUnread);

async function loadSessions() {
    sessionLoading.value = true;
    try {
        const resp = await messageApi.getSessionList({ pageNum: 1, pageSize: 50 });
        if (resp.code === 200 && resp.data) {
            sessions.value = resp.data.list || [];
        }
    } catch (error) {
        console.error('加载会话列表失败:', error);
        toast.error('加载会话列表失败，请稍后重试');
    } finally {
        sessionLoading.value = false;
    }
}

async function loadMsgUnread() {
    await messageStore.loadMsgUnread();
}

function openChat(session: MessageSessionVO) {
    activeSession.value = session;
    router.push(`/messages/chat/${session.id}`);
}

function sessionPeer(session: MessageSessionVO): PeerUser {
    return (
        session.peerUser || {
            id: session.peerId || '',
            nickname: session.peerNickname,
            avatar: session.peerAvatar,
        }
    );
}

function sessionLastPreview(session: MessageSessionVO): string {
    return session.lastMessage || session.lastContent || '暂无消息';
}

function sessionTime(session: MessageSessionVO): string {
    return formatRelativeTime(session.lastMessageTime || session.updateTime || session.createTime);
}

// 聊天面板：发送消息后刷新未读数与会话列表
function handleChatSent() {
    loadMsgUnread();
}

function handleChatRead(sessionId: string) {
    const s = sessions.value.find((item) => String(item.id) === String(sessionId));
    if (s) {
        s.unreadCount = 0;
    }
    loadMsgUnread();
}

function exitChat() {
    activeSession.value = null;
    router.push('/messages');
    // 退出聊天后刷新会话列表与未读数
    loadSessions();
    loadMsgUnread();
}

// 切换 Tab
function switchTab(tab: 'notification' | 'message') {
    activeTab.value = tab;
    if (tab === 'message' && sessions.value.length === 0) {
        loadSessions();
    }
}

// 面包屑
const breadcrumbItems = computed(() => [
    { label: '消息中心' },
]);

onMounted(async () => {
    if (!userStore.isAuthenticated) {
        router.push({ name: 'login', query: { redirect: route.fullPath } });
        return;
    }
    // 聊天模式直接展示聊天，无需加载 tab
    if (isChatMode.value) {
        // 预加载会话列表以匹配对方信息
        await loadSessions();
        const match = sessions.value.find((s) => String(s.id) === chatSessionId.value);
        if (match) activeSession.value = match;
        return;
    }
    await Promise.all([loadNotifications(), loadNotifUnread(), loadSessions(), loadMsgUnread()]);
});

// 监听路由进入/退出聊天模式
watch(isChatMode, (isChat) => {
    if (isChat) {
        // 进入聊天模式，匹配会话
        const match = sessions.value.find((s) => String(s.id) === chatSessionId.value);
        if (match) activeSession.value = match;
    } else {
        activeSession.value = null;
    }
});
</script>

<template>
  <div class="min-h-screen flex flex-col" style="background-color: var(--theme-bg);">
    <!-- 面包屑 -->
    <div class="border-b py-3 sm:py-4" style="border-color: var(--theme-border);">
      <div class="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8">
        <Breadcrumb :items="breadcrumbItems" />
      </div>
    </div>

    <!-- 聊天详情模式 -->
    <template v-if="isChatMode">
      <div class="flex-1 flex flex-col" style="height: calc(100vh - 60px);">
        <div class="max-w-7xl mx-auto w-full flex-1 flex flex-col">
          <MessageChat
            :session-id="chatSessionId"
            :peer-user="activeSession ? sessionPeer(activeSession) : null"
            @back="exitChat"
            @sent="handleChatSent"
            @read="handleChatRead"
          />
        </div>
      </div>
    </template>

    <!-- 消息中心列表模式 -->
    <template v-else>
      <div class="py-6 sm:py-8 flex-1">
        <div class="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8">
          <!-- 标题 -->
          <div class="flex items-center justify-between mb-5">
            <h1 class="text-xl sm:text-2xl font-bold" style="color: var(--theme-text);">消息中心</h1>
            <div v-if="totalUnread > 0" class="text-sm px-3 py-1 rounded-full" style="background-color: var(--theme-accent); color: var(--theme-primary);">
              {{ totalUnread }} 未读
            </div>
          </div>

          <!-- Tab 切换 -->
          <div class="mb-5 border-b" style="border-color: var(--theme-border);">
            <nav class="flex gap-1">
              <button
                @click="switchTab('notification')"
                class="flex items-center gap-2 px-4 sm:px-6 py-3 text-sm sm:text-base font-medium border-b-2 transition-colors relative"
                :style="activeTab === 'notification'
                  ? 'border-color: var(--theme-primary); color: var(--theme-primary);'
                  : 'border-color: transparent; color: var(--theme-text-secondary);'"
              >
                <Bell class="w-4 h-4" />
                通知
                <span v-if="notifUnreadCount > 0" class="absolute -top-1 -right-1 min-w-[18px] h-[18px] px-1 rounded-full text-xs flex items-center justify-center" style="background-color: #ef4444; color: white;">
                  {{ notifUnreadCount > 99 ? '99+' : notifUnreadCount }}
                </span>
              </button>
              <button
                @click="switchTab('message')"
                class="flex items-center gap-2 px-4 sm:px-6 py-3 text-sm sm:text-base font-medium border-b-2 transition-colors relative"
                :style="activeTab === 'message'
                  ? 'border-color: var(--theme-primary); color: var(--theme-primary);'
                  : 'border-color: transparent; color: var(--theme-text-secondary);'"
              >
                <MessageSquare class="w-4 h-4" />
                私信
                <span v-if="msgUnreadCount > 0" class="absolute -top-1 -right-1 min-w-[18px] h-[18px] px-1 rounded-full text-xs flex items-center justify-center" style="background-color: #ef4444; color: white;">
                  {{ msgUnreadCount > 99 ? '99+' : msgUnreadCount }}
                </span>
              </button>
            </nav>
          </div>

          <!-- 通知 Tab -->
          <div v-if="activeTab === 'notification'">
            <!-- 类型筛选 + 全部已读 -->
            <div class="flex items-center justify-between mb-4 gap-2 flex-wrap">
              <div class="flex gap-2 flex-wrap">
                <button
                  v-for="opt in notifFilterOptions"
                  :key="opt.value"
                  @click="notifFilter = opt.value"
                  class="px-3 py-1.5 rounded-full text-xs sm:text-sm transition-colors"
                  :style="notifFilter === opt.value
                    ? { backgroundColor: 'var(--theme-primary)', color: 'white' }
                    : { backgroundColor: 'var(--theme-surface)', color: 'var(--theme-text-secondary)', border: '1px solid var(--theme-border)' }"
                >
                  {{ opt.label }}
                </button>
              </div>
              <button
                @click="markAllNotifRead"
                class="flex items-center gap-1 text-xs sm:text-sm px-3 py-1.5 rounded-full transition-colors"
                style="color: var(--theme-primary); background-color: var(--theme-surface); border: 1px solid var(--theme-border);"
              >
                <CheckCheck class="w-4 h-4" />
                全部已读
              </button>
            </div>

            <!-- 通知列表 -->
            <div v-if="notifLoading" class="text-center py-12">
              <Loader2 class="w-8 h-8 mx-auto animate-spin" style="color: var(--theme-primary);" />
            </div>
            <div v-else-if="filteredNotifications.length === 0" class="py-16 text-center rounded-2xl" style="background-color: var(--theme-surface); border: 1px solid var(--theme-border);">
              <Inbox class="w-12 h-12 mx-auto mb-3" style="color: var(--theme-text-secondary);" />
              <p class="text-sm" style="color: var(--theme-text-secondary);">暂无通知</p>
            </div>
            <div v-else class="space-y-2">
              <button
                v-for="n in filteredNotifications"
                :key="n.id"
                @click="markNotifRead(n)"
                class="w-full text-left flex items-start gap-3 p-4 rounded-2xl transition-colors"
                :style="{
                  backgroundColor: 'var(--theme-surface)',
                  border: '1px solid var(--theme-border)',
                  opacity: n.isRead ? 0.7 : 1
                }"
              >
                <div class="w-9 h-9 rounded-full flex items-center justify-center flex-shrink-0" :style="{ backgroundColor: 'var(--theme-accent)' }">
                  <component :is="getNotifIcon(n.type)" class="w-4 h-4" :style="{ color: getNotifIconColor(n.type) }" />
                </div>
                <div class="flex-1 min-w-0">
                  <div class="flex items-center gap-2">
                    <span v-if="!n.isRead" class="w-2 h-2 rounded-full flex-shrink-0" style="background-color: #ef4444;"></span>
                    <span class="font-medium text-sm truncate" style="color: var(--theme-text);">{{ n.title }}</span>
                  </div>
                  <p class="text-sm mt-1 line-clamp-2" style="color: var(--theme-text-secondary);">{{ n.content }}</p>
                  <p class="text-xs mt-1.5" style="color: var(--theme-text-secondary);">{{ formatRelativeTime(n.createTime) }}</p>
                </div>
              </button>
            </div>
          </div>

          <!-- 私信 Tab -->
          <div v-else-if="activeTab === 'message'">
            <div v-if="sessionLoading" class="text-center py-12">
              <Loader2 class="w-8 h-8 mx-auto animate-spin" style="color: var(--theme-primary);" />
            </div>
            <div v-else-if="sessions.length === 0" class="py-16 text-center rounded-2xl" style="background-color: var(--theme-surface); border: 1px solid var(--theme-border);">
              <MessageSquare class="w-12 h-12 mx-auto mb-3" style="color: var(--theme-text-secondary);" />
              <p class="text-sm" style="color: var(--theme-text-secondary);">暂无私信会话</p>
            </div>
            <div v-else class="space-y-2">
              <button
                v-for="s in sessions"
                :key="s.id"
                @click="openChat(s)"
                class="w-full text-left flex items-center gap-3 p-4 rounded-2xl transition-colors"
                style="background-color: var(--theme-surface); border: 1px solid var(--theme-border);"
              >
                <div class="relative flex-shrink-0">
                  <img
                    :src="getSafeAvatar(s.peerAvatar || s.peerUser?.avatar, s.peerId || '')"
                    :alt="s.peerNickname || s.peerUser?.nickname || ''"
                    class="w-12 h-12 rounded-full object-cover"
                    @error="(e: Event) => (e.target as HTMLImageElement).src = getSafeAvatar(null, s.peerId || '')"
                  />
                </div>
                <div class="flex-1 min-w-0">
                  <div class="flex items-center justify-between gap-2">
                    <span class="font-medium text-sm truncate" style="color: var(--theme-text);">
                      {{ s.peerNickname || s.peerUser?.nickname || s.peerUser?.username || '未知用户' }}
                    </span>
                    <span class="text-xs flex-shrink-0" style="color: var(--theme-text-secondary);">{{ sessionTime(s) }}</span>
                  </div>
                  <p class="text-sm mt-1 line-clamp-1" style="color: var(--theme-text-secondary);">{{ sessionLastPreview(s) }}</p>
                </div>
                <span v-if="s.unreadCount && s.unreadCount > 0" class="min-w-[20px] h-5 px-1.5 rounded-full text-xs flex items-center justify-center flex-shrink-0" style="background-color: #ef4444; color: white;">
                  {{ s.unreadCount > 99 ? '99+' : s.unreadCount }}
                </span>
              </button>
            </div>
          </div>
        </div>
      </div>
    </template>
  </div>
</template>
