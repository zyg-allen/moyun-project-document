<script setup lang="ts">
import { ref, computed, onMounted, watch } from 'vue';
import { useRouter } from 'vue-router';
import { useHead } from '@vueuse/head';
import {
  MessageCircle, Plus, Search, Eye, Heart, MessageSquare,
  Clock, Flame, TrendingUp, ChevronLeft, ChevronRight, Pin,
} from 'lucide-vue-next';
import SiteFooter from '@/components/SiteFooter.vue';
import LazyImage from '@/components/LazyImage.vue';
import Empty from '@/components/Empty.vue';
import { generateSeo } from '@/utils/seo';
import { getSafeAvatar } from '@/utils/avatar';
import { formatRelativeTime } from '@/utils/date';
import { formatNumber } from '@/utils/number';
import { getTopicList } from '@/api/topic';
import { useAuth } from '@/composables/useAuth';
import type { Topic } from '@/types/api';

const router = useRouter();
const { requireAuth } = useAuth();

type SortKey = 'latest' | 'hot' | 'active';

const loading = ref(false);
const error = ref<string | null>(null);
const topics = ref<Topic[]>([]);
const total = ref(0);
const page = ref(1);
const pageSize = 12;

const sort = ref<SortKey>('latest');
const keyword = ref('');
const searchInput = ref('');

const sortOptions: { value: SortKey; label: string; icon: typeof Clock }[] = [
  { value: 'latest', label: '最新', icon: Clock },
  { value: 'hot', label: '热门', icon: Flame },
  { value: 'active', label: '活跃', icon: TrendingUp },
];

const totalPages = computed(() => Math.max(1, Math.ceil(total.value / pageSize)));

useHead(computed(() => generateSeo({
  title: '话题广场',
  description: '墨韵话题广场，发起讨论、表达观点，与社区成员一起交流思想',
  keywords: ['话题', '讨论', '社区', '观点', '墨韵'],
  canonicalPath: '/topics',
})));

onMounted(() => {
  loadTopics();
});

watch(page, () => {
  loadTopics();
});

async function loadTopics() {
  loading.value = true;
  error.value = null;
  try {
    const res = await getTopicList({
      pageNum: page.value,
      pageSize,
      sort: sort.value,
      keyword: keyword.value || undefined,
    });
    if (res.code === 200 && res.data) {
      topics.value = res.data.list || [];
      total.value = res.data.total || 0;
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

function switchSort(s: SortKey) {
  if (sort.value === s) return;
  sort.value = s;
  if (page.value !== 1) {
    page.value = 1;
  } else {
    loadTopics();
  }
}

function doSearch() {
  keyword.value = searchInput.value.trim();
  if (page.value !== 1) {
    page.value = 1;
  } else {
    loadTopics();
  }
}

function gotoDetail(t: Topic) {
  router.push(`/topic/${t.id}`);
}

function gotoCreate() {
  if (!requireAuth('/topic/create')) return;
  router.push('/topic/create');
}

function gotoPage(p: number) {
  if (p < 1 || p > totalPages.value) return;
  page.value = p;
  window.scrollTo({ top: 0, behavior: 'smooth' });
}
</script>

<template>
  <div class="min-h-screen flex flex-col" style="background-color: var(--theme-bg);">
    <!-- Hero 区 -->
    <div class="py-6 sm:py-8">
      <div class="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8">
        <div class="relative overflow-hidden rounded-2xl text-white" style="background-image: radial-gradient(circle at 20% 50%, rgba(190, 24, 93, 0.3) 0%, transparent 50%), radial-gradient(circle at 80% 30%, rgba(124, 58, 237, 0.3) 0%, transparent 50%), linear-gradient(135deg, #be185d 0%, #a21caf 50%, #7c3aed 100%);">
          <div class="absolute inset-0 opacity-10 pointer-events-none" aria-hidden="true">
            <MessageCircle class="absolute top-6 left-8 w-32 h-32 text-white" />
            <MessageSquare class="absolute bottom-4 right-10 w-40 h-40 text-white" />
          </div>
          <div class="relative px-6 py-8 sm:px-10 sm:py-10 text-center">
            <div class="inline-flex items-center bg-white/10 backdrop-blur-sm px-4 py-1.5 rounded-full text-sm mb-4">
              <MessageCircle class="w-4 h-4 mr-2" /> 墨韵 · 话题广场
            </div>
            <h1 class="text-3xl md:text-4xl font-bold mb-3">话题广场</h1>
            <p class="text-sm opacity-90">发起讨论，表达观点，与社区成员一起交流思想</p>
          </div>
        </div>
      </div>
    </div>

    <!-- 工具栏：排序 + 搜索 + 发起 -->
    <div class="border-b" style="background-color: var(--theme-surface); border-color: var(--theme-border);">
      <div class="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 py-3 flex flex-wrap items-center gap-3">
        <!-- 排序 Tab -->
        <div class="flex items-center gap-1">
          <button
            v-for="opt in sortOptions"
            :key="opt.value"
            @click="switchSort(opt.value)"
            class="inline-flex items-center px-3 py-1.5 rounded-lg text-sm font-medium transition"
            :style="{
              color: sort === opt.value ? 'white' : 'var(--theme-text-secondary)',
              backgroundColor: sort === opt.value ? 'var(--theme-primary)' : 'transparent',
            }"
          >
            <component :is="opt.icon" class="w-3.5 h-3.5 mr-1" />
            {{ opt.label }}
          </button>
        </div>

        <!-- 搜索框 -->
        <div class="flex-1 min-w-[200px] max-w-md">
          <div
            class="flex items-center rounded-lg border px-3"
            style="background-color: var(--theme-bg); border-color: var(--theme-border);"
          >
            <Search class="w-4 h-4 flex-shrink-0" style="color: var(--theme-text-secondary);" />
            <input
              v-model="searchInput"
              type="text"
              placeholder="搜索话题标题..."
              class="flex-1 bg-transparent px-2 py-2 text-sm outline-none"
              style="color: var(--theme-text);"
              @keyup.enter="doSearch"
            />
            <button
              @click="doSearch"
              class="text-xs px-2 py-1 rounded transition hover:opacity-80"
              style="color: white; background-color: var(--theme-primary);"
            >
              搜索
            </button>
          </div>
        </div>

        <!-- 发起话题按钮 -->
        <button
          @click="gotoCreate"
          class="inline-flex items-center px-4 py-2 text-sm text-white rounded-lg transition hover:opacity-90"
          style="background-color: var(--theme-primary);"
        >
          <Plus class="w-4 h-4 mr-1" />
          发起话题
        </button>
      </div>
    </div>

    <!-- 内容区 -->
    <div class="flex-1 py-8">
      <div class="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8">
        <!-- 加载状态 -->
        <div v-if="loading" class="flex flex-col items-center justify-center py-20">
          <div
            class="animate-spin rounded-full h-12 w-12 border-b-2"
            style="border-color: var(--theme-primary);"
          ></div>
          <p class="mt-4 text-sm" style="color: var(--theme-text-secondary);">加载中...</p>
        </div>

        <!-- 错误状态 -->
        <div
          v-else-if="error"
          class="rounded-xl border p-8 max-w-md mx-auto text-center"
          style="background-color: var(--theme-surface); border-color: var(--theme-border);"
        >
          <p class="mb-4 text-sm" style="color: var(--theme-text);">{{ error }}</p>
          <button
            @click="loadTopics"
            class="px-4 py-2 text-white rounded-lg text-sm transition hover:opacity-90"
            style="background-color: var(--theme-primary);"
          >
            重试
          </button>
        </div>

        <!-- 空状态 -->
        <Empty
          v-else-if="topics.length === 0"
          title="暂无话题"
          description="快来发起第一个话题，与大家一起讨论吧"
          size="lg"
        >
          <template #action>
            <button
              @click="gotoCreate"
              class="inline-flex items-center px-4 py-2 text-white rounded-lg text-sm transition hover:opacity-90"
              style="background-color: var(--theme-primary);"
            >
              <Plus class="w-4 h-4 mr-1" />
              发起话题
            </button>
          </template>
        </Empty>

        <!-- 话题卡片列表 -->
        <template v-else>
          <div class="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-5 mb-8">
            <div
              v-for="t in topics"
              :key="t.id"
              @click="gotoDetail(t)"
              class="rounded-xl overflow-hidden border shadow-sm hover:shadow-md transition cursor-pointer flex flex-col"
              style="background-color: var(--theme-surface); border-color: var(--theme-border);"
            >
              <!-- 封面 -->
              <div
                class="relative h-32"
                style="background-color: var(--theme-bg);"
              >
                <LazyImage
                  v-if="t.cover"
                  :src="t.cover"
                  :alt="t.title"
                  class="w-full h-full object-cover"
                />
                <div
                  v-else
                  class="w-full h-full flex items-center justify-center"
                  style="background: linear-gradient(135deg, var(--theme-accent), color-mix(in srgb, var(--theme-accent) 50%, #c4b5fd));"
                >
                  <MessageCircle class="w-10 h-10" style="color: var(--theme-primary); opacity: 0.6;" />
                </div>
                <span
                  v-if="t.pinned === 1"
                  class="absolute top-3 left-3 inline-flex items-center px-2 py-0.5 rounded text-xs font-medium text-white"
                  style="background-color: #f59e0b;"
                >
                  <Pin class="w-3 h-3 mr-1" />置顶
                </span>
              </div>

              <div class="p-5 flex flex-col flex-1">
                <h3
                  class="text-base font-semibold mb-2 line-clamp-1"
                  style="color: var(--theme-text);"
                >
                  {{ t.title }}
                </h3>
                <p
                  v-if="t.description"
                  class="text-xs mb-3 line-clamp-2"
                  style="color: var(--theme-text-secondary);"
                >
                  {{ t.description }}
                </p>
                <div class="flex-1"></div>

                <!-- 发起人 -->
                <div class="flex items-center mb-3">
                  <img
                    :src="getSafeAvatar(t.creator?.avatar, String(t.creatorId))"
                    :alt="t.creator?.nickname || '发起人'"
                    class="w-5 h-5 rounded-full object-cover mr-2 flex-shrink-0"
                    loading="lazy"
                  />
                  <span class="text-xs truncate flex-1" style="color: var(--theme-text);">
                    {{ t.creator?.nickname || '匿名用户' }}
                  </span>
                  <span
                    v-if="t.lastPostTime"
                    class="text-xs flex items-center flex-shrink-0"
                    style="color: var(--theme-text-secondary);"
                  >
                    <Clock class="w-3 h-3 mr-0.5" />
                    {{ formatRelativeTime(t.lastPostTime) }}
                  </span>
                </div>

                <!-- 统计 -->
                <div
                  class="flex items-center justify-between pt-3 border-t text-xs"
                  style="border-color: var(--theme-border); color: var(--theme-text-secondary);"
                >
                  <span class="flex items-center">
                    <Eye class="w-3 h-3 mr-0.5" />{{ formatNumber(t.viewCount) }}
                  </span>
                  <span class="flex items-center">
                    <MessageSquare class="w-3 h-3 mr-0.5" />{{ formatNumber(t.postCount) }}
                  </span>
                  <span class="flex items-center">
                    <Heart class="w-3 h-3 mr-0.5" />{{ formatNumber(t.likeCount) }}
                  </span>
                </div>
              </div>
            </div>
          </div>

          <!-- 分页 -->
          <div v-if="totalPages > 1" class="flex flex-wrap items-center justify-center gap-2 mt-8">
            <button
              @click="gotoPage(page - 1)"
              :disabled="page === 1"
              class="px-3 py-2 rounded-lg text-sm transition disabled:opacity-40 disabled:cursor-not-allowed flex items-center"
              style="background-color: var(--theme-surface); border: 1px solid var(--theme-border); color: var(--theme-text);"
            >
              <ChevronLeft class="w-4 h-4" />
              上一页
            </button>
            <span class="px-4 py-2 text-sm" style="color: var(--theme-text-secondary);">
              第 {{ page }} / {{ totalPages }} 页
            </span>
            <button
              @click="gotoPage(page + 1)"
              :disabled="page === totalPages"
              class="px-3 py-2 rounded-lg text-sm transition disabled:opacity-40 disabled:cursor-not-allowed flex items-center"
              style="background-color: var(--theme-surface); border: 1px solid var(--theme-border); color: var(--theme-text);"
            >
              下一页
              <ChevronRight class="w-4 h-4" />
            </button>
            <span class="ml-2 text-xs" style="color: var(--theme-text-secondary);">共 {{ total }} 个话题</span>
          </div>
        </template>
      </div>
    </div>

    <SiteFooter />
  </div>
</template>
