<script setup lang="ts">
import { ref, computed, onMounted } from 'vue';
import { RouterLink as Link, useRouter } from 'vue-router';
import { useHead } from '@vueuse/head';
import { ArrowLeft, Heart, Gift, Clock, Loader2, ChevronLeft } from 'lucide-vue-next';
import Breadcrumb from '@/components/Breadcrumb.vue';
import { generateSeo } from '@/utils/seo';
import { getSafeAvatar } from '@/utils/avatar';
import { formatShortDate } from '@/utils/date';
import { getMyGivenTips, getMyReceivedTips } from '@/api/tip';
import type { PortalTipOrder, TipTargetType } from '@/types/api';

const router = useRouter();

const activeTab = ref<'given' | 'received'>('given');
const loading = ref(false);
const givenList = ref<PortalTipOrder[]>([]);
const givenTotal = ref(0);
const receivedList = ref<PortalTipOrder[]>([]);
const receivedTotal = ref(0);
const pageNum = ref(1);
const pageSize = ref(10);

useHead(
  generateSeo({
    title: '我的消费记录',
    description: '查看我的打赏记录与收到的打赏',
    keywords: ['消费记录', '打赏', '我的打赏'],
    type: 'website'
  })
);

const tabs = [
  { id: 'given' as const, label: '我打赏的', icon: Gift },
  { id: 'received' as const, label: '我收到的', icon: Heart },
];

const currentList = computed(() =>
  activeTab.value === 'given' ? givenList.value : receivedList.value
);
const currentTotal = computed(() =>
  activeTab.value === 'given' ? givenTotal.value : receivedTotal.value
);
const totalPages = computed(() =>
  Math.max(1, Math.ceil(currentTotal.value / pageSize.value))
);

function targetTypeLabel(targetType?: TipTargetType): string {
  if (targetType === 'article') return '文章打赏';
  if (targetType === 'column') return '专栏打赏';
  if (targetType === 'article_paid') return '付费阅读';
  return '打赏';
}

async function loadData() {
  loading.value = true;
  try {
    const params = { pageNum: pageNum.value, pageSize: pageSize.value };
    if (activeTab.value === 'given') {
      const res = await getMyGivenTips(params);
      if (res.code === 200 && res.data) {
        givenList.value = res.data.list || [];
        givenTotal.value = res.data.total || 0;
      }
    } else {
      const res = await getMyReceivedTips(params);
      if (res.code === 200 && res.data) {
        receivedList.value = res.data.list || [];
        receivedTotal.value = res.data.total || 0;
      }
    }
  } catch (err) {
    const e = err as { message?: string };
    console.error('加载消费记录失败:', e?.message || err);
  } finally {
    loading.value = false;
  }
}

function handleTabChange(tab: 'given' | 'received') {
  if (activeTab.value === tab) return;
  activeTab.value = tab;
  pageNum.value = 1;
  loadData();
}

function handlePageChange(next: number) {
  if (next < 1 || next > totalPages.value || loading.value) return;
  pageNum.value = next;
  loadData();
}

function goBack() {
  if (window.history.length > 1) {
    router.back();
  } else {
    router.push('/user');
  }
}

onMounted(() => {
  loadData();
});
</script>

<template>
  <div class="min-h-screen flex flex-col" style="background-color: var(--theme-bg);">
    <!-- 顶部返回栏 -->
    <div
      class="border-b sticky top-0 z-30 backdrop-blur-sm"
      style="background-color: var(--theme-surface); border-color: var(--theme-border);"
    >
      <div class="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 py-3 flex items-center justify-between">
        <button
          @click="goBack"
          class="flex items-center text-sm transition hover:opacity-80"
          style="color: var(--theme-text-secondary);"
        >
          <ArrowLeft class="w-4 h-4 mr-1" />
          返回个人中心
        </button>
        <span class="text-sm font-medium" style="color: var(--theme-text);">我的消费记录</span>
        <span class="w-24"></span>
      </div>
    </div>

    <div class="py-8 flex-1">
      <div class="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8">
        <!-- 面包屑 -->
        <div class="mb-6">
          <Breadcrumb :items="[{ label: '首页', path: '/' }, { label: '个人中心', path: '/user' }, { label: '我的消费记录' }]" />
        </div>

        <h1 class="text-xl sm:text-2xl font-bold mb-6" style="color: var(--theme-text);">我的消费记录</h1>

        <!-- 子 Tab -->
        <div class="flex gap-1 mb-6 overflow-x-auto scrollbar-hide">
          <button
            v-for="tab in tabs"
            :key="tab.id"
            @click="handleTabChange(tab.id)"
            class="inline-flex items-center gap-2 px-4 py-2 rounded-xl text-sm font-medium transition-colors whitespace-nowrap"
            :style="activeTab === tab.id
              ? 'background-color: var(--theme-primary); color: white;'
              : 'background-color: var(--theme-accent); color: var(--theme-text-secondary);'"
          >
            <component :is="tab.icon" class="w-4 h-4" />
            {{ tab.label }}
            <span class="ml-1 text-xs opacity-80">({{ tab.id === 'given' ? givenTotal : receivedTotal }})</span>
          </button>
        </div>

        <!-- 加载状态 -->
        <div v-if="loading" class="flex flex-col items-center justify-center py-20">
          <Loader2 class="w-10 h-10 animate-spin" style="color: var(--theme-primary);" />
          <p class="mt-4 text-sm" style="color: var(--theme-text-secondary);">加载中...</p>
        </div>

        <!-- 空状态 -->
        <div
          v-else-if="currentList.length === 0"
          class="rounded-2xl p-8 sm:p-12 text-center"
          style="background-color: var(--theme-surface); border: 1px solid var(--theme-border);"
        >
          <component
            :is="activeTab === 'given' ? Gift : Heart"
            class="w-12 h-12 sm:w-16 sm:h-16 mx-auto mb-4"
            style="color: var(--theme-text-secondary);"
          />
          <h3 class="text-lg font-medium mb-2" style="color: var(--theme-text);">
            {{ activeTab === 'given' ? '还没有打赏记录' : '还没有收到的打赏' }}
          </h3>
          <p style="color: var(--theme-text-secondary);">
            {{ activeTab === 'given' ? '去浏览感兴趣的内容并打赏作者吧' : '持续创作优质内容，吸引更多打赏' }}
          </p>
          <Link
            to="/"
            class="inline-flex items-center gap-2 mt-6 px-5 py-2.5 rounded-xl font-medium text-sm"
            style="background-color: var(--theme-primary); color: white;"
          >
            去逛逛
          </Link>
        </div>

        <!-- 列表 -->
        <div v-else class="space-y-3">
          <div
            v-for="item in currentList"
            :key="item.id"
            class="flex items-center gap-4 p-4 rounded-xl"
            style="background-color: var(--theme-surface); border: 1px solid var(--theme-border);"
          >
            <!-- 头像 -->
            <img
              :src="getSafeAvatar(
                activeTab === 'given' ? item.authorAvatar : item.userAvatar,
                String((activeTab === 'given' ? item.authorId : item.userId) || '')
              )"
              :alt="(activeTab === 'given' ? item.authorNickname : item.userNickname) || '用户'"
              class="w-12 h-12 rounded-full object-cover flex-shrink-0"
              loading="lazy"
            />

            <!-- 主体信息 -->
            <div class="flex-1 min-w-0">
              <div class="flex items-center gap-2 mb-1 flex-wrap">
                <span class="font-medium truncate" style="color: var(--theme-text);">
                  {{ (activeTab === 'given' ? item.authorNickname : item.userNickname) || '匿名用户' }}
                </span>
                <span
                  class="text-xs px-2 py-0.5 rounded-full"
                  style="background-color: var(--theme-accent); color: var(--theme-text-secondary);"
                >
                  {{ targetTypeLabel(item.targetType) }}
                </span>
                <span
                  v-if="item.status"
                  class="text-xs px-2 py-0.5 rounded-full"
                  :style="item.status === 'paid'
                    ? 'background-color: #dcfce7; color: #16a34a;'
                    : 'background-color: #fef3c7; color: #d97706;'"
                >
                  {{ item.status === 'paid' ? '已支付' : '待支付' }}
                </span>
              </div>
              <p v-if="item.message" class="text-sm mb-1 truncate" style="color: var(--theme-text-secondary);">
                "{{ item.message }}"
              </p>
              <div class="flex items-center gap-3 text-xs flex-wrap" style="color: var(--theme-text-secondary);">
                <span v-if="item.createdTime" class="flex items-center">
                  <Clock class="w-3 h-3 mr-0.5" />{{ formatShortDate(item.createdTime) }}
                </span>
                <span v-if="item.targetType === 'article_paid'">文章 #{{ item.targetId }}</span>
                <span v-else-if="item.targetType === 'article'">文章 #{{ item.targetId }}</span>
                <span v-else-if="item.targetType === 'column'">专栏 #{{ item.targetId }}</span>
              </div>
            </div>

            <!-- 金额 -->
            <div class="flex-shrink-0 text-right">
              <div class="text-lg font-bold" style="color: var(--theme-primary);">
                <span v-if="activeTab === 'given'">-</span><span v-else>+</span>¥{{ Number(item.amount || 0).toFixed(2) }}
              </div>
              <div v-if="item.payMethod" class="text-xs" style="color: var(--theme-text-secondary);">
                {{ item.payMethod === 'wallet' ? '钱包' : item.payMethod }}
              </div>
            </div>
          </div>
        </div>

        <!-- 分页 -->
        <div v-if="!loading && currentList.length > 0 && totalPages > 1" class="flex items-center justify-center gap-2 mt-8">
          <button
            @click="handlePageChange(pageNum - 1)"
            :disabled="pageNum === 1"
            class="inline-flex items-center px-3 py-2 rounded-lg text-sm font-medium transition disabled:opacity-40 disabled:cursor-not-allowed"
            style="background-color: var(--theme-surface); color: var(--theme-text); border: 1px solid var(--theme-border);"
          >
            <ChevronLeft class="w-4 h-4" />
            上一页
          </button>
          <span class="text-sm px-3" style="color: var(--theme-text-secondary);">
            {{ pageNum }} / {{ totalPages }}
          </span>
          <button
            @click="handlePageChange(pageNum + 1)"
            :disabled="pageNum === totalPages"
            class="inline-flex items-center px-3 py-2 rounded-lg text-sm font-medium transition disabled:opacity-40 disabled:cursor-not-allowed"
            style="background-color: var(--theme-surface); color: var(--theme-text); border: 1px solid var(--theme-border);"
          >
            下一页
            <ChevronLeft class="w-4 h-4 rotate-180" />
          </button>
        </div>
      </div>
    </div>
  </div>
</template>

<style scoped>
/* 隐藏横向滚动条 */
.scrollbar-hide {
  -ms-overflow-style: none;
  scrollbar-width: none;
}
.scrollbar-hide::-webkit-scrollbar {
  display: none;
}
</style>
