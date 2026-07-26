<script setup lang="ts">
import { ref, computed, onMounted, onUnmounted, watch, nextTick } from 'vue';
import { useRouter } from 'vue-router';
import { useHead } from '@vueuse/head';
import {
  Flame, Users, ArrowRight, Eye, FileText, BookOpen,
  Briefcase, Sparkles, Bell,
} from 'lucide-vue-next';
import SiteFooter from '@/components/SiteFooter.vue';
import Breadcrumb from '@/components/Breadcrumb.vue';
import LazyImage from '@/components/LazyImage.vue';
import { generateSeo } from '@/utils/seo';
import { getSafeAvatar } from '@/utils/avatar';
import { formatRelativeTime } from '@/utils/date';
import { getFollowingFeed, getHotFeed } from '@/api/feed';
import { useUserStore } from '@/stores/user';
import type { FeedEventVO, FeedEventType } from '@/types/api';

const router = useRouter();
const userStore = useUserStore();

type TabKey = 'following' | 'hot';
const activeTab = ref<TabKey>('hot');

const loading = ref(false);
const loadingMore = ref(false);
const error = ref<string | null>(null);
const list = ref<FeedEventVO[]>([]);
const page = ref(1);
const pageSize = 10;
const total = ref(0);
const hasMore = computed(() => list.value.length < total.value);

const breadcrumbs = computed(() => [{ label: '动态广场' }]);

// 滚动加载哨兵
const sentinelRef = ref<HTMLElement | null>(null);
let observer: IntersectionObserver | null = null;

// eventType 中文映射
const eventTypeText: Record<string, string> = {
  publish_article: '发布了文章',
  publish_experience: '发布了面经',
  new_column: '创建了专栏',
  checkin: '打卡了',
};

// eventType 图标映射（动态卡片动作）
function actionIcon(ev: FeedEventType) {
  switch (ev) {
    case 'publish_article':
      return FileText;
    case 'publish_experience':
      return Briefcase;
    case 'new_column':
      return BookOpen;
    case 'checkin':
      return Sparkles;
    default:
      return Bell;
  }
}

function actionText(ev: FeedEventType): string {
  return eventTypeText[ev] || '有了新动态';
}

// 跳转目标
function targetPath(ev: FeedEventVO): string | null {
  if (ev.targetId === undefined || ev.targetId === null || ev.targetId === '') return null;
  switch (ev.targetType) {
    case 'article':
      return `/article/${ev.targetId}`;
    case 'experience':
      return `/interview/experience/${ev.targetId}`;
    case 'column':
      return `/column/${ev.targetId}`;
    default:
      return null;
  }
}

function gotoTarget(ev: FeedEventVO) {
  const path = targetPath(ev);
  if (path) router.push(path);
}

useHead(computed(() => generateSeo({
  title: '动态广场',
  description: '墨韵动态广场，关注创作者的最新文章、面经与专栏，发现全站热门动态',
  keywords: ['动态', '关注', '热门', '创作者', '墨韵'],
  canonicalPath: '/feed',
})));

onMounted(async () => {
  // 默认进入热门；若已登录则进入关注
  if (userStore.isAuthenticated) {
    activeTab.value = 'following';
  }
  await loadList(true);
  setupObserver();
});

onUnmounted(() => {
  destroyObserver();
});

watch(activeTab, async () => {
  await loadList(true);
  await nextTick();
  setupObserver();
});

function destroyObserver() {
  if (observer) {
    observer.disconnect();
    observer = null;
  }
}

function setupObserver() {
  destroyObserver();
  if (!sentinelRef.value) return;
  observer = new IntersectionObserver((entries) => {
    const entry = entries[0];
    if (entry.isIntersecting && hasMore.value && !loadingMore.value && !loading.value) {
      void loadMore();
    }
  }, { rootMargin: '200px' });
  observer.observe(sentinelRef.value);
}

async function loadList(reset = false) {
  if (reset) {
    page.value = 1;
    list.value = [];
    error.value = null;
  }
  // 关注 Tab 登录态检查
  if (activeTab.value === 'following' && !userStore.isAuthenticated) {
    router.push({ name: 'login', query: { redirect: router.currentRoute.value.fullPath } });
    return;
  }
  loading.value = true;
  try {
    const params = { pageNum: page.value, pageSize };
    const res = activeTab.value === 'following'
      ? await getFollowingFeed(params)
      : await getHotFeed(params);
    if (res.code === 200 && res.data) {
      const data = res.data;
      list.value = reset ? (data.list || []) : list.value.concat(data.list || []);
      total.value = data.total || 0;
    } else {
      error.value = res.message || '加载失败';
    }
  } catch (err) {
    const e = err as { message?: string };
    error.value = e?.message || '加载失败，请稍后重试';
  } finally {
    loading.value = false;
  }
}

async function loadMore() {
  if (!hasMore.value || loadingMore.value) return;
  page.value += 1;
  loadingMore.value = true;
  try {
    const params = { pageNum: page.value, pageSize };
    const res = activeTab.value === 'following'
      ? await getFollowingFeed(params)
      : await getHotFeed(params);
    if (res.code === 200 && res.data) {
      const data = res.data;
      list.value = list.value.concat(data.list || []);
      total.value = data.total || 0;
    }
  } catch (err) {
    // 失败回退页码，便于下次重试
    page.value = Math.max(1, page.value - 1);
    const e = err as { message?: string };
    error.value = e?.message || '加载更多失败';
  } finally {
    loadingMore.value = false;
  }
}

function switchTab(tab: TabKey) {
  if (activeTab.value === tab) return;
  activeTab.value = tab;
}

function goDiscoverAuthors() {
  router.push('/authors');
}

function avatarUrl(ev: FeedEventVO): string {
  return getSafeAvatar(ev.userAvatar, String(ev.userId));
}
</script>

<template>
  <div class="min-h-screen flex flex-col" style="background-color: var(--theme-bg);">
    <!-- 顶部面包屑栏 -->
    <div class="border-b sticky top-0 z-30 backdrop-blur-sm py-3" style="background-color: var(--theme-surface); border-color: var(--theme-border);">
      <div class="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 flex items-center justify-between gap-4">
        <Breadcrumb :items="breadcrumbs" />
        <div class="flex items-center gap-2"></div>
      </div>
    </div>

    <!-- Tab 切换 -->
    <div class="border-b" style="background-color: var(--theme-surface); border-color: var(--theme-border);">
      <div class="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 flex items-center gap-1">
        <button
          @click="switchTab('hot')"
          class="flex items-center px-5 py-3 text-sm font-medium transition relative"
          :style="{
            color: activeTab === 'hot' ? 'var(--theme-primary)' : 'var(--theme-text-secondary)',
          }"
        >
          <Flame class="w-4 h-4 mr-1.5" />
          热门
          <span
            v-if="activeTab === 'hot'"
            class="absolute left-3 right-3 bottom-0 h-0.5 rounded-full"
            style="background-color: var(--theme-primary);"
          ></span>
        </button>
        <button
          @click="switchTab('following')"
          class="flex items-center px-5 py-3 text-sm font-medium transition relative"
          :style="{
            color: activeTab === 'following' ? 'var(--theme-primary)' : 'var(--theme-text-secondary)',
          }"
        >
          <Users class="w-4 h-4 mr-1.5" />
          关注
          <span
            v-if="activeTab === 'following'"
            class="absolute left-3 right-3 bottom-0 h-0.5 rounded-full"
            style="background-color: var(--theme-primary);"
          ></span>
        </button>
      </div>
    </div>

    <!-- 内容区 -->
    <div class="flex-1 py-6">
      <div class="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8">
        <!-- 加载状态（首屏） -->
        <div v-if="loading && list.length === 0" class="flex flex-col items-center justify-center py-20">
          <div
            class="animate-spin rounded-full h-10 w-10 border-2"
            style="border-color: var(--theme-border); border-top-color: var(--theme-primary);"
          ></div>
          <p class="mt-4 text-sm" style="color: var(--theme-text-secondary);">加载中...</p>
        </div>

        <!-- 错误状态 -->
        <div
          v-else-if="error && list.length === 0"
          class="rounded-xl border p-8 max-w-md mx-auto text-center"
          style="background-color: var(--theme-surface); border-color: var(--theme-border);"
        >
          <p class="mb-4 text-sm" style="color: var(--theme-text);">{{ error }}</p>
          <button
            @click="loadList(true)"
            class="px-4 py-2 text-white rounded-lg text-sm transition hover:opacity-90"
            style="background-color: var(--theme-primary);"
          >
            重试
          </button>
        </div>

        <!-- 空状态：关注 Tab 没有数据 -->
        <div
          v-else-if="list.length === 0 && activeTab === 'following'"
          class="rounded-xl border p-12 text-center"
          style="background-color: var(--theme-surface); border-color: var(--theme-border);"
        >
          <Users class="w-12 h-12 mx-auto mb-3" style="color: var(--theme-text-secondary); opacity: 0.5;" />
          <p class="text-sm mb-4" style="color: var(--theme-text-secondary);">还没有关注的作者，去发现更多创作者</p>
          <button
            @click="goDiscoverAuthors"
            class="inline-flex items-center px-4 py-2 text-white rounded-lg text-sm transition hover:opacity-90"
            style="background-color: var(--theme-primary);"
          >
            发现创作者
            <ArrowRight class="w-4 h-4 ml-1" />
          </button>
        </div>

        <!-- 空状态：热门 Tab 没有数据 -->
        <div
          v-else-if="list.length === 0"
          class="rounded-xl border p-12 text-center"
          style="background-color: var(--theme-surface); border-color: var(--theme-border);"
        >
          <Flame class="w-12 h-12 mx-auto mb-3" style="color: var(--theme-text-secondary); opacity: 0.5;" />
          <p class="text-sm" style="color: var(--theme-text-secondary);">暂无热门动态</p>
        </div>

        <!-- 动态卡片列表 -->
        <template v-else>
          <div class="space-y-4">
            <div
              v-for="ev in list"
              :key="ev.eventId"
              class="rounded-xl border shadow-sm hover:shadow-md transition overflow-hidden"
              style="background-color: var(--theme-surface); border-color: var(--theme-border);"
            >
              <!-- 顶部：作者 + 动作 + 时间 -->
              <div class="flex items-center gap-3 p-4 pb-3">
                <img
                  :src="avatarUrl(ev)"
                  :alt="ev.userNickname || '匿名用户'"
                  class="w-10 h-10 rounded-full object-cover flex-shrink-0"
                  loading="lazy"
                />
                <div class="flex-1 min-w-0">
                  <div class="flex items-center gap-1.5 flex-wrap">
                    <span class="text-sm font-medium truncate" style="color: var(--theme-text);">
                      {{ ev.userNickname || '匿名用户' }}
                    </span>
                    <span class="inline-flex items-center text-xs px-1.5 py-0.5 rounded-full" style="background-color: var(--theme-accent); color: var(--theme-primary);">
                      <component :is="actionIcon(ev.eventType)" class="w-3 h-3 mr-1" />
                      {{ actionText(ev.eventType) }}
                    </span>
                  </div>
                  <div class="text-xs mt-0.5" style="color: var(--theme-text-secondary);">
                    {{ ev.createdTime ? formatRelativeTime(ev.createdTime) : '' }}
                  </div>
                </div>
              </div>

              <!-- 中部：标题 + 摘要 + 封面 -->
              <div
                v-if="ev.title || ev.summary || ev.cover"
                class="px-4 pb-4"
              >
                <div class="flex gap-4">
                  <div class="flex-1 min-w-0">
                    <h3
                      v-if="ev.title"
                      class="text-base font-semibold mb-1.5 line-clamp-2"
                      style="color: var(--theme-text);"
                    >
                      {{ ev.title }}
                    </h3>
                    <p
                      v-if="ev.summary"
                      class="text-sm line-clamp-3"
                      style="color: var(--theme-text-secondary);"
                    >
                      {{ ev.summary }}
                    </p>
                  </div>
                  <div
                    v-if="ev.cover"
                    class="w-24 h-24 rounded-lg overflow-hidden flex-shrink-0"
                    style="background-color: var(--theme-bg);"
                  >
                    <LazyImage
                      :src="ev.cover"
                      :alt="ev.title || ''"
                      class="w-full h-full object-cover"
                    />
                  </div>
                </div>
              </div>

              <!-- 底部：跳转按钮 -->
              <div
                v-if="targetPath(ev)"
                class="px-4 pb-4"
              >
                <button
                  @click="gotoTarget(ev)"
                  class="inline-flex items-center px-3 py-1.5 rounded-lg text-xs font-medium transition hover:opacity-90"
                  style="background-color: var(--theme-accent); color: var(--theme-primary);"
                >
                  查看详情
                  <ArrowRight class="w-3.5 h-3.5 ml-1" />
                </button>
              </div>
            </div>
          </div>

          <!-- 加载更多 / 哨兵 -->
          <div ref="sentinelRef" class="py-6">
            <div v-if="loadingMore" class="flex flex-col items-center justify-center">
              <div
                class="animate-spin rounded-full h-8 w-8 border-2"
                style="border-color: var(--theme-border); border-top-color: var(--theme-primary);"
              ></div>
              <p class="mt-3 text-xs" style="color: var(--theme-text-secondary);">加载中...</p>
            </div>
            <div v-else-if="!hasMore" class="text-center text-xs py-4" style="color: var(--theme-text-secondary);">
              <Eye class="w-4 h-4 inline mr-1" />
              没有更多了 · 共 {{ total }} 条
            </div>
          </div>
        </template>
      </div>
    </div>

    <SiteFooter />
  </div>
</template>
