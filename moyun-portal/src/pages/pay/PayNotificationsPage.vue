<template>
  <div class="min-h-screen py-8 px-4" style="background-color: var(--theme-bg);">
    <div class="max-w-2xl mx-auto">
      <div class="flex items-center justify-between mb-6">
        <h1 class="text-2xl font-bold" style="color: var(--theme-text);">支付通知</h1>
        <span
          v-if="unreadCount > 0"
          class="px-2.5 py-1 rounded-full text-xs font-medium"
          style="background-color: rgba(239, 68, 68, 0.12); color: #ef4444;"
        >
          {{ unreadCount }} 条未读
        </span>
      </div>

      <div v-if="loading" class="text-center py-16 text-sm" style="color: var(--theme-text-secondary);">加载中…</div>

      <div v-else-if="notifications.length === 0" class="rounded-2xl border py-16 text-center" style="background-color: var(--theme-surface); border-color: var(--theme-border);">
        <Bell class="w-10 h-10 mx-auto mb-3" style="color: var(--theme-text-secondary);" />
        <p class="text-sm" style="color: var(--theme-text-secondary);">暂无支付通知</p>
      </div>

      <div v-else class="space-y-3">
        <div
          v-for="item in notifications"
          :key="String(item.id)"
          class="rounded-2xl border p-4 cursor-pointer transition-colors"
          :style="{
            backgroundColor: item.readFlag === 0 ? 'var(--theme-surface)' : 'var(--theme-bg)',
            borderColor: item.readFlag === 0 ? 'var(--theme-primary)' : 'var(--theme-border)',
          }"
          @click="handleRead(item)"
        >
          <div class="flex items-start justify-between gap-3">
            <div class="flex-1 min-w-0">
              <div class="flex items-center gap-2">
                <span
                  v-if="item.readFlag === 0"
                  class="w-2 h-2 rounded-full flex-shrink-0"
                  style="background-color: var(--theme-primary);"
                ></span>
                <p class="font-medium text-sm truncate" style="color: var(--theme-text);">{{ item.title }}</p>
              </div>
              <p class="text-sm mt-1.5 leading-relaxed" style="color: var(--theme-text-secondary);">{{ item.content }}</p>
              <div class="flex items-center gap-3 mt-2 text-xs" style="color: var(--theme-text-secondary);">
                <span v-if="item.refNo">单号：{{ item.refNo }}</span>
                <span>{{ item.createTime }}</span>
              </div>
            </div>
            <Check v-if="item.readFlag === 1" class="w-4 h-4 flex-shrink-0 mt-1" style="color: var(--theme-text-secondary);" />
          </div>
        </div>

        <!-- 加载更多 -->
        <div v-if="hasMore" class="text-center pt-2">
          <button
            class="px-5 py-2 rounded-lg text-sm border transition-colors"
            style="color: var(--theme-text); border-color: var(--theme-border);"
            :disabled="loadingMore"
            @click="loadMore"
          >
            {{ loadingMore ? '加载中…' : '加载更多' }}
          </button>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { useToast } from '@/composables/useToast';
import { ref, computed, onMounted } from 'vue';
import { Bell, Check } from 'lucide-vue-next';
import { getPayNotifications, markNotificationRead } from '@/api/pay';
import type { PayNotification } from '@/types/api';


const toast = useToast();

const notifications = ref<PayNotification[]>([]);
const loading = ref(true);
const loadingMore = ref(false);
const current = ref(1);
const total = ref(0);
const unreadCount = ref(0);

const hasMore = computed(() => notifications.value.length < total.value);

const load = async (page: number) => {
  const res = await getPayNotifications({ current: page, size: 20 });
  const data = res.data;
  if (data) {
    if (page === 1) {
      notifications.value = data.records ?? [];
    } else {
      notifications.value.push(...(data.records ?? []));
    }
    total.value = Number(data.total ?? 0);
    unreadCount.value = Number(data.unreadCount ?? 0);
  }
};

const loadMore = async () => {
  loadingMore.value = true;
  try {
    current.value += 1;
    await load(current.value);
  } catch {
    current.value -= 1;
    toast.error('加载失败');
  } finally {
    loadingMore.value = false;
  }
};

const handleRead = async (item: PayNotification) => {
  if (item.readFlag === 1) return;
  try {
    await markNotificationRead(item.id);
    item.readFlag = 1;
    unreadCount.value = Math.max(0, unreadCount.value - 1);
  } catch {
    // 静默失败，下次刷新恢复
  }
};

onMounted(async () => {
  try {
    await load(1);
  } catch {
    toast.error('通知加载失败');
  } finally {
    loading.value = false;
  }
});
</script>
