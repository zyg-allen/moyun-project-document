<script setup lang="ts">
import { ref, computed, onMounted, onUnmounted, nextTick, watch } from 'vue';
import { ArrowLeft, Send, Loader2, ChevronUp } from 'lucide-vue-next';
import type { MessageVO, PeerUser, MessageType } from '@/types/api';
import * as messageApi from '@/api/message';
import { useUserStore } from '@/stores/user';
import { getSafeAvatar } from '@/utils/avatar';
import { getToken } from '@/api/client';
import { MessageWebSocket } from '@/utils/websocket';
import { useToast } from '@/composables/useToast';

interface Props {
    sessionId: string;
    peerUser?: PeerUser | null;
}

const props = withDefaults(defineProps<Props>(), {
    peerUser: null,
});

const emit = defineEmits<{
    back: [];
    sent: [msg: MessageVO];
    read: [sessionId: string];
}>();

const userStore = useUserStore();
const toast = useToast();

const messages = ref<MessageVO[]>([]);
const peer = ref<PeerUser | null>(props.peerUser);
const inputText = ref('');
const sending = ref(false);
const loadingHistory = ref(false);
const loadingMore = ref(false);
const historyPage = ref(1);
const historyTotal = ref(0);
const scrollContainer = ref<HTMLElement | null>(null);
const inputEl = ref<HTMLTextAreaElement | null>(null);

let wsClient: MessageWebSocket | null = null;

const currentUserId = computed(() => String(userStore.userId || ''));
const hasMoreHistory = computed(() => messages.value.length < historyTotal.value);

const peerDisplayName = computed(() => peer.value?.nickname || peer.value?.username || '私信');

const peerAvatar = computed(() => getSafeAvatar(peer.value?.avatar, peer.value?.id || ''));

function isMine(msg: MessageVO): boolean {
    if (msg.isMine !== undefined) return msg.isMine;
    return String(msg.senderId) === currentUserId.value;
}

function formatTime(time?: string): string {
    if (!time) return '';
    const d = new Date(time);
    if (Number.isNaN(d.getTime())) return time;
    const now = new Date();
    const sameDay = d.toDateString() === now.toDateString();
    const pad = (n: number) => String(n).padStart(2, '0');
    if (sameDay) {
        return `${pad(d.getHours())}:${pad(d.getMinutes())}`;
    }
    const sameYear = d.getFullYear() === now.getFullYear();
    if (sameYear) {
        return `${d.getMonth() + 1}/${d.getDate()} ${pad(d.getHours())}:${pad(d.getMinutes())}`;
    }
    return `${d.getFullYear()}/${d.getMonth() + 1}/${d.getDate()} ${pad(d.getHours())}:${pad(d.getMinutes())}`;
}

/**
 * 取消息所在"日历日"的分组标签。
 * 用于在历史消息列表中插入"今天/昨天/具体日期"分隔线，便于回顾。
 */
function dateGroupLabel(time?: string): string {
    if (!time) return '';
    const d = new Date(time);
    if (Number.isNaN(d.getTime())) return '';
    const now = new Date();
    const pad = (n: number) => String(n).padStart(2, '0');
    const startOfToday = new Date(now.getFullYear(), now.getMonth(), now.getDate()).getTime();
    const dayMs = 24 * 60 * 60 * 1000;
    const diffDays = Math.floor((startOfToday - new Date(d.getFullYear(), d.getMonth(), d.getDate()).getTime()) / dayMs);
    if (diffDays <= 0) return '今天';
    if (diffDays === 1) return '昨天';
    if (diffDays === 2) return '前天';
    if (d.getFullYear() === now.getFullYear()) {
        return `${d.getMonth() + 1}月${d.getDate()}日`;
    }
    return `${d.getFullYear()}年${d.getMonth() + 1}月${d.getDate()}日`;
}

/**
 * 判断相邻两条消息是否跨"日历日"，跨日则在中间插入日期分隔线。
 */
function shouldShowDateDivider(prev?: MessageVO, curr?: MessageVO): boolean {
    if (!prev || !curr) return true;
    const pt = prev.createdTime || prev.createTime;
    const ct = curr.createdTime || curr.createTime;
    if (!pt || !ct) return true;
    const a = new Date(pt);
    const b = new Date(ct);
    if (Number.isNaN(a.getTime()) || Number.isNaN(b.getTime())) return false;
    return a.toDateString() !== b.toDateString();
}

function sortByTime(list: MessageVO[]): MessageVO[] {
    return [...list].sort((a, b) => {
        const ta = new Date(a.createdTime || a.createTime || 0).getTime();
        const tb = new Date(b.createdTime || b.createTime || 0).getTime();
        return ta - tb;
    });
}

function scrollToBottom(smooth = false) {
    nextTick(() => {
        const el = scrollContainer.value;
        if (!el) return;
        el.scrollTo({ top: el.scrollHeight, behavior: smooth ? 'smooth' : 'auto' });
    });
}

async function loadPeerInfo() {
    // 若未传入 peerUser，从会话列表中查找该会话的对方信息
    if (props.peerUser) {
        peer.value = props.peerUser;
        return;
    }
    try {
        const resp = await messageApi.getSessionList({ pageNum: 1, pageSize: 50 });
        if (resp.code === 200 && resp.data) {
            const session = resp.data.list.find((s) => String(s.id) === String(props.sessionId));
            if (session) {
                peer.value = session.peerUser || {
                    id: session.peerId || '',
                    nickname: session.peerNickname,
                    avatar: session.peerAvatar,
                };
            }
        }
    } catch (error) {
        console.warn('获取会话信息失败:', error);
    }
}

async function loadHistory(reset = false) {
    if (reset) {
        loadingHistory.value = true;
        historyPage.value = 1;
    } else {
        loadingMore.value = true;
        historyPage.value++;
    }
    try {
        const resp = await messageApi.getMessageHistory(props.sessionId, {
            pageNum: historyPage.value,
            pageSize: 20,
        });
        if (resp.code === 200 && resp.data) {
            const items = resp.data.list || [];
            historyTotal.value = resp.data.total || 0;
            if (reset) {
                messages.value = sortByTime(items);
                scrollToBottom();
            } else {
                // 上拉加载更早消息，前置插入并保持滚动位置
                const prevHeight = scrollContainer.value?.scrollHeight || 0;
                messages.value = sortByTime([...items, ...messages.value]);
                nextTick(() => {
                    const el = scrollContainer.value;
                    if (el) {
                        el.scrollTop = el.scrollHeight - prevHeight;
                    }
                });
            }
        }
    } catch (error) {
        console.error('加载历史消息失败:', error);
    } finally {
        loadingHistory.value = false;
        loadingMore.value = false;
    }
}

async function handleScrollToTop() {
    const el = scrollContainer.value;
    if (!el || loadingMore.value || !hasMoreHistory.value) return;
    if (el.scrollTop <= 20) {
        await loadMoreHistory();
    }
}

async function loadMoreHistory() {
    if (loadingMore.value || !hasMoreHistory.value) return;
    await loadHistory(false);
}

function isDuplicate(msg: MessageVO): boolean {
    return messages.value.some((m) => String(m.id) === String(msg.id));
}

function appendMessage(msg: MessageVO) {
    // 仅处理当前会话的消息
    if (msg.sessionId && String(msg.sessionId) !== String(props.sessionId)) {
        return;
    }
    if (isDuplicate(msg)) return;
    messages.value.push(msg);
    scrollToBottom(true);
}

function handleIncomingMessage(msg: MessageVO) {
    appendMessage(msg);
}

/**
 * 轮询降级：基于本地已知最大消息 ID 增量拉取新消息，避免与 loadHistory 重复拉取第一页
 */
async function pollNewMessages() {
    if (messages.value.length === 0) return;
    try {
        // 取本地最大的消息 ID 作为游标，仅拉取比它更新的消息
        const maxId = messages.value.reduce((max, m) => {
            const id = Number(m.id);
            return !isNaN(id) && id > max ? id : max;
        }, 0);
        const resp = await messageApi.getMessageHistory(props.sessionId, {
            pageNum: 1,
            pageSize: 50,
        });
        if (resp.code === 200 && resp.data) {
            const items = resp.data.list || [];
            // 仅合并且 ID 大于本地最大值的真正新消息
            const newOnes = items.filter((m) => {
                const id = Number(m.id);
                return !isNaN(id) && id > maxId && !isDuplicate(m);
            });
            if (newOnes.length > 0) {
                messages.value = sortByTime([...messages.value, ...newOnes]);
                scrollToBottom(true);
            }
        }
    } catch {
        /* 轮询失败忽略 */
    }
}

async function handleSend() {
    const content = inputText.value.trim();
    if (!content || sending.value) return;
    if (!peer.value?.id) {
        toast.error('无法获取对方信息');
        return;
    }
    sending.value = true;
    const tempId = `temp-${Date.now()}`;
    // 乐观追加一条本地消息
    const optimistic: MessageVO = {
        id: tempId,
        sessionId: props.sessionId,
        senderId: currentUserId.value,
        content,
        msgType: 'text' as MessageType,
        createdTime: new Date().toISOString(),
        isMine: true,
    };
    messages.value.push(optimistic);
    scrollToBottom(true);
    inputText.value = '';

    try {
        const resp = await messageApi.sendMessage({
            receiverId: peer.value.id,
            content,
            msgType: 'text',
        });
        if (resp.code === 200 && resp.data) {
            // 用真实消息替换临时消息
            // 注意：发送期间 WS 推送/轮询可能已把真实消息投递进列表，需先去重避免重复
            const idx = messages.value.findIndex((m) => m.id === tempId);
            if (idx >= 0) {
                if (!isDuplicate(resp.data)) {
                    messages.value.splice(idx, 1, resp.data);
                } else {
                    // 真实消息已由 WS/轮询投递，仅移除临时消息
                    messages.value.splice(idx, 1);
                }
            } else {
                appendMessage(resp.data);
            }
            emit('sent', resp.data);
        } else {
            // 发送失败，移除临时消息
            removeTemp(tempId);
            toast.error('发送失败');
        }
    } catch (error) {
        console.error('发送消息失败:', error);
        removeTemp(tempId);
        toast.error('发送失败，请重试');
    } finally {
        sending.value = false;
        nextTick(() => inputEl.value?.focus());
    }
}

function removeTemp(id: string) {
    const idx = messages.value.findIndex((m) => m.id === id);
    if (idx >= 0) messages.value.splice(idx, 1);
}

function onKeydown(e: KeyboardEvent) {
    if (e.key === 'Enter' && !e.shiftKey) {
        e.preventDefault();
        handleSend();
    }
}

async function markRead() {
    // 在 await 前捕获当前 sessionId，避免 await 期间切换会话后 emit 出错误的 sessionId
    const sid = props.sessionId;
    try {
        await messageApi.markSessionRead(sid);
        emit('read', sid);
    } catch (error) {
        console.warn('标记已读失败:', error);
    }
}

function connectWebSocket() {
    const token = getToken();
    if (!token) return;
    wsClient = new MessageWebSocket({ pollInterval: 5000 });
    wsClient.connect(token, handleIncomingMessage, pollNewMessages);
}

function goBack() {
    emit('back');
}

onMounted(async () => {
    await loadPeerInfo();
    await loadHistory(true);
    await markRead();
    connectWebSocket();
    nextTick(() => inputEl.value?.focus());
});

onUnmounted(() => {
    wsClient?.disconnect();
    wsClient = null;
});

// peerUser 变化时同步
watch(
    () => props.peerUser,
    (val) => {
        if (val) peer.value = val;
    }
);

// sessionId 变化时重新加载
watch(
    () => props.sessionId,
    async (val, oldVal) => {
        if (val && val !== oldVal) {
            messages.value = [];
            await loadPeerInfo();
            await loadHistory(true);
            await markRead();
            // 重连 WebSocket 以更新订阅上下文
            wsClient?.disconnect();
            connectWebSocket();
        }
    }
);
</script>

<template>
  <div class="flex flex-col h-full" style="background-color: var(--theme-bg);">
    <!-- 顶部：对方信息 -->
    <div class="flex items-center gap-3 px-4 py-3 border-b" style="background-color: var(--theme-surface); border-color: var(--theme-border);">
      <button
        @click="goBack"
        class="flex items-center gap-1.5 px-3 py-2 rounded-lg transition-colors flex-shrink-0 font-medium text-sm hover:opacity-90"
        style="background-color: var(--theme-primary); color: white;"
        aria-label="返回消息列表"
        title="返回消息列表"
      >
        <ArrowLeft class="w-4 h-4" />
        <span class="hidden sm:inline">返回</span>
      </button>
      <img
        :src="peerAvatar"
        :alt="peerDisplayName"
        class="w-10 h-10 rounded-full object-cover flex-shrink-0"
        @error="(e: Event) => (e.target as HTMLImageElement).src = getSafeAvatar(null, peer?.id || '')"
      />
      <div class="flex-1 min-w-0">
        <p class="font-medium truncate" style="color: var(--theme-text);">{{ peerDisplayName }}</p>
        <p class="text-xs flex items-center gap-1" style="color: var(--theme-text-secondary);">
          <span class="w-1.5 h-1.5 rounded-full" style="background-color: #22c55e;"></span>
          在线
        </p>
      </div>
    </div>

    <!-- 中部：消息列表 -->
    <div
      ref="scrollContainer"
      class="flex-1 overflow-y-auto px-4 py-4 space-y-3"
      @scroll="handleScrollToTop"
    >
      <!-- 加载更早 -->
      <div v-if="hasMoreHistory" class="text-center">
        <button
          @click="loadMoreHistory"
          :disabled="loadingMore"
          class="inline-flex items-center gap-1 text-xs px-3 py-1.5 rounded-full transition-colors"
          style="color: var(--theme-text-secondary); background-color: var(--theme-surface);"
        >
          <Loader2 v-if="loadingMore" class="w-3 h-3 animate-spin" />
          <ChevronUp v-else class="w-3 h-3" />
          {{ loadingMore ? '加载中...' : '加载更早消息' }}
        </button>
      </div>

      <!-- 加载中（首次） -->
      <div v-if="loadingHistory && messages.length === 0" class="text-center py-8">
        <Loader2 class="w-6 h-6 mx-auto animate-spin" style="color: var(--theme-primary);" />
        <p class="mt-2 text-xs" style="color: var(--theme-text-secondary);">加载消息中...</p>
      </div>

      <!-- 空状态 -->
      <div v-else-if="!loadingHistory && messages.length === 0" class="text-center py-12">
        <p class="text-sm" style="color: var(--theme-text-secondary);">还没有消息，发送第一条私信吧</p>
      </div>

      <!-- 消息气泡 -->
      <template v-for="(msg, idx) in messages" :key="msg.id">
        <!-- 日期分组分隔线：首条或跨"日历日"时插入"今天/昨天/具体日期"标签 -->
        <div
          v-if="shouldShowDateDivider(messages[idx - 1], msg)"
          class="flex justify-center my-3"
        >
          <span
            class="text-xs px-3 py-1 rounded-full"
            style="background-color: var(--theme-surface); color: var(--theme-text-secondary); border: 1px solid var(--theme-border);"
          >
            {{ dateGroupLabel(msg.createdTime || msg.createTime) }}
          </span>
        </div>
        <div
          class="flex items-end gap-2"
          :class="isMine(msg) ? 'flex-row-reverse' : 'flex-row'"
        >
          <img
            v-if="!isMine(msg)"
            :src="peerAvatar"
            :alt="peerDisplayName"
            class="w-8 h-8 rounded-full object-cover flex-shrink-0"
            @error="(e: Event) => (e.target as HTMLImageElement).src = getSafeAvatar(null, peer?.id || '')"
          />
          <div
            class="max-w-[85%] sm:max-w-[70%] px-3.5 py-2 rounded-2xl text-sm break-words"
            :style="isMine(msg)
              ? { backgroundColor: 'var(--theme-primary)', color: 'white', borderBottomRightRadius: '4px' }
              : { backgroundColor: 'var(--theme-surface)', color: 'var(--theme-text)', border: '1px solid var(--theme-border)', borderBottomLeftRadius: '4px' }"
          >
            {{ msg.content }}
            <div
              class="text-[10px] mt-1"
              :style="isMine(msg) ? { color: 'rgba(255,255,255,0.8)' } : { color: 'var(--theme-text-secondary)' }"
            >
              {{ formatTime(msg.createdTime || msg.createTime) }}
            </div>
          </div>
        </div>
      </template>
    </div>

    <!-- 底部：输入区 -->
    <div class="px-4 py-3 border-t" style="background-color: var(--theme-surface); border-color: var(--theme-border);">
      <div class="flex items-end gap-2">
        <textarea
          ref="inputEl"
          v-model="inputText"
          rows="1"
          placeholder="输入消息，回车发送，Shift+回车换行"
          class="flex-1 resize-none px-3 py-2 rounded-xl text-sm outline-none max-h-32"
          style="background-color: var(--theme-bg); border: 1px solid var(--theme-border); color: var(--theme-text);"
          @keydown="onKeydown"
        ></textarea>
        <button
          @click="handleSend"
          :disabled="!inputText.trim() || sending"
          class="flex items-center gap-1 px-4 py-2 rounded-xl text-sm font-medium transition-colors flex-shrink-0 disabled:opacity-50"
          style="background-color: var(--theme-primary); color: white;"
        >
          <Loader2 v-if="sending" class="w-4 h-4 animate-spin" />
          <Send v-else class="w-4 h-4" />
          <span class="hidden sm:inline">发送</span>
        </button>
      </div>
    </div>
  </div>
</template>
