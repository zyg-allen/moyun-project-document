<script setup lang="ts">
import { ref, computed, onMounted, watch } from 'vue';
import { useRouter } from 'vue-router';
import { useHead } from '@vueuse/head';
import {
  BookOpen, Plus, Search, FileText, Users,
  ArrowLeft, ChevronLeft, ChevronRight, CheckCircle2,
} from 'lucide-vue-next';
import SiteFooter from '@/components/SiteFooter.vue';
import LazyImage from '@/components/LazyImage.vue';
import { generateSeo } from '@/utils/seo';
import { getSafeAvatar } from '@/utils/avatar';
import { getColumnList } from '@/api/column';
import { useAuth } from '@/composables/useAuth';
import type { ColumnListItemVO, ColumnQuery } from '@/types/api';

const router = useRouter();
const { requireAuth } = useAuth();

const loading = ref(false);
const error = ref<string | null>(null);
const columns = ref<ColumnListItemVO[]>([]);
const total = ref(0);
const page = ref(1);
const pageSize = 12;

const keyword = ref('');
const searchInput = ref('');
const sortBy = ref<NonNullable<ColumnQuery['sortBy']>>('latest');

const sortOptions: { value: NonNullable<ColumnQuery['sortBy']>; label: string }[] = [
  { value: 'latest', label: '最新' },
  { value: 'popular', label: '热门' },
  { value: 'subscribe', label: '订阅数' },
];

const totalPages = computed(() => Math.max(1, Math.ceil(total.value / pageSize)));

useHead(computed(() => generateSeo({
  title: '专栏广场',
  description: '墨韵专栏广场，发现优质专栏，按主题聚合文章，持续连载，订阅追更',
  keywords: ['专栏', '专栏订阅', '连载', '文章合集', '墨韵'],
  canonicalPath: '/columns',
})));

onMounted(() => {
  loadColumns();
});

watch(page, () => {
  loadColumns();
});

function doSearch() {
  keyword.value = searchInput.value.trim();
  if (page.value !== 1) {
    page.value = 1;
  } else {
    loadColumns();
  }
}

function changeSort(s: NonNullable<ColumnQuery['sortBy']>) {
  if (sortBy.value === s) return;
  sortBy.value = s;
  if (page.value !== 1) {
    page.value = 1;
  } else {
    loadColumns();
  }
}

async function loadColumns() {
  loading.value = true;
  error.value = null;
  try {
    const params: ColumnQuery = { pageNum: page.value, pageSize, sortBy: sortBy.value };
    if (keyword.value) params.keyword = keyword.value;
    const res = await getColumnList(params);
    if (res.code === 200 && res.data) {
      columns.value = res.data.list || [];
      total.value = res.data.total || 0;
    } else {
      error.value = res.message || '加载专栏失败';
    }
  } catch (err) {
    const e = err as { message?: string };
    error.value = e?.message || '加载专栏失败，请稍后重试';
  } finally {
    loading.value = false;
  }
}

function gotoDetail(id: string | number) {
  router.push(`/column/${id}`);
}

function gotoCreate() {
  if (!requireAuth('/column/create')) return;
  router.push('/column/create');
}

function goBack() {
  router.push('/');
}

function gotoMy() {
  if (!requireAuth('/column/my')) return;
  router.push('/column/my');
}

function formatNumber(n?: number) {
  const v = n || 0;
  if (v >= 10000) return (v / 10000).toFixed(1) + 'w';
  if (v >= 1000) return (v / 1000).toFixed(1) + 'k';
  return String(v);
}

function gotoPage(p: number) {
  if (p < 1 || p > totalPages.value) return;
  page.value = p;
  window.scrollTo({ top: 0, behavior: 'smooth' });
}
</script>

<template>
  <div class="min-h-screen flex flex-col" style="background-color: var(--theme-bg);">
    <!-- 顶部返回栏 -->
    <div
      class="border-b sticky top-0 z-30 backdrop-blur-sm"
      style="background-color: var(--theme-surface); border-color: var(--theme-border);"
    >
      <div class="max-w-7xl mx-auto px-4 py-3 flex items-center justify-between">
        <button
          @click="goBack"
          class="flex items-center text-sm transition hover:opacity-80"
          style="color: var(--theme-text-secondary);"
        >
          <ArrowLeft class="w-4 h-4 mr-1" />
          返回首页
        </button>
        <span class="text-sm font-medium" style="color: var(--theme-text);">专栏广场</span>
        <button
          @click="gotoMy"
          class="text-sm transition hover:opacity-80"
          style="color: var(--theme-primary);"
        >
          我的专栏
        </button>
      </div>
    </div>

    <!-- Hero 区 -->
    <div
      class="relative overflow-hidden text-white py-14"
      style="background: linear-gradient(135deg, var(--theme-primary), color-mix(in srgb, var(--theme-primary) 60%, #4338ca 100%));"
    >
      <div class="absolute inset-0 opacity-10 pointer-events-none">
        <div class="absolute top-8 left-10 w-48 h-48 rounded-full bg-white"></div>
        <div class="absolute bottom-8 right-16 w-72 h-72 rounded-full bg-white"></div>
      </div>
      <div class="relative max-w-7xl mx-auto px-4 text-center">
        <div class="inline-flex items-center bg-white/10 backdrop-blur-sm px-4 py-1.5 rounded-full text-sm mb-5">
          <BookOpen class="w-4 h-4 mr-2" /> 墨韵 · 专栏
        </div>
        <h1 class="text-4xl md:text-5xl font-bold tracking-tight mb-4">专栏广场</h1>
        <p class="text-base md:text-lg text-white/90 max-w-2xl mx-auto mb-8">
          按主题聚合文章，持续连载，订阅追更，构建你的知识体系
        </p>
        <!-- 搜索框 -->
        <div class="max-w-xl mx-auto rounded-xl p-2 flex items-center shadow-lg" style="background-color: var(--theme-bg);">
          <Search class="w-5 h-5 ml-2 flex-shrink-0" style="color: var(--theme-text-secondary);" />
          <input
            v-model="searchInput"
            @keyup.enter="doSearch"
            type="text"
            placeholder="搜索专栏标题、副标题..."
            class="flex-1 px-3 py-2 focus:outline-none text-sm"
            style="color: var(--theme-text);"
          />
          <button
            @click="doSearch"
            class="px-5 py-2 rounded-lg text-sm font-medium text-white transition hover:opacity-90"
            style="background-color: var(--theme-primary);"
          >
            搜索
          </button>
        </div>
        <!-- 创建按钮 -->
        <div class="mt-6">
          <button
            @click="gotoCreate"
            class="inline-flex items-center px-5 py-2.5 rounded-lg text-sm font-medium text-white transition hover:opacity-90"
            style="background-color: rgba(255,255,255,0.2);"
          >
            <Plus class="w-4 h-4 mr-1.5" />
            创建专栏
          </button>
        </div>
      </div>
    </div>

    <!-- 筛选条 -->
    <div class="border-b" style="background-color: var(--theme-surface); border-color: var(--theme-border);">
      <div class="max-w-7xl mx-auto px-4 py-3 flex items-center justify-between flex-wrap gap-3">
        <div class="flex items-center gap-2">
          <span class="text-xs" style="color: var(--theme-text-secondary);">排序：</span>
          <button
            v-for="opt in sortOptions"
            :key="opt.value"
            @click="changeSort(opt.value)"
            class="px-3 py-1.5 rounded-lg text-xs font-medium transition"
            :style="{
              backgroundColor: sortBy === opt.value ? 'var(--theme-primary)' : 'var(--theme-bg)',
              color: sortBy === opt.value ? '#fff' : 'var(--theme-text-secondary)',
              border: '1px solid var(--theme-border)',
            }"
          >
            {{ opt.label }}
          </button>
        </div>
        <div class="text-xs" style="color: var(--theme-text-secondary);">
          共 {{ total }} 个专栏
        </div>
      </div>
    </div>

    <!-- 内容区 -->
    <div class="flex-1 py-8">
      <div class="max-w-7xl mx-auto px-4">
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
            @click="loadColumns"
            class="px-4 py-2 text-white rounded-lg text-sm transition hover:opacity-90"
            style="background-color: var(--theme-primary);"
          >
            重试
          </button>
        </div>

        <!-- 空状态 -->
        <div
          v-else-if="columns.length === 0"
          class="rounded-xl border p-12 text-center"
          style="background-color: var(--theme-surface); border-color: var(--theme-border);"
        >
          <BookOpen class="w-12 h-12 mx-auto mb-3" style="color: var(--theme-text-secondary); opacity: 0.5;" />
          <p class="text-sm" style="color: var(--theme-text-secondary);">
            {{ keyword ? `没有找到与「${keyword}」相关的专栏` : '暂无专栏' }}
          </p>
        </div>

        <!-- 专栏卡片网格 -->
        <template v-else>
          <div class="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-5 mb-8">
            <div
              v-for="col in columns"
              :key="col.id"
              @click="gotoDetail(col.id)"
              class="rounded-xl overflow-hidden border shadow-sm hover:shadow-lg hover:-translate-y-1 transition cursor-pointer flex flex-col"
              style="background-color: var(--theme-surface); border-color: var(--theme-border);"
            >
              <!-- 封面图 -->
              <div class="relative h-40" style="background-color: var(--theme-bg);">
                <LazyImage
                  v-if="col.cover"
                  :src="col.cover"
                  :alt="col.title"
                  class="w-full h-full object-cover"
                />
                <div v-else class="w-full h-full flex items-center justify-center" style="background: linear-gradient(135deg, var(--theme-accent), color-mix(in srgb, var(--theme-accent) 50%, #c4b5fd));">
                  <BookOpen class="w-10 h-10" style="color: var(--theme-primary); opacity: 0.6;" />
                </div>
                <!-- 完结标识 -->
                <span
                  v-if="col.isFinished"
                  class="absolute top-3 left-3 inline-flex items-center px-2 py-0.5 rounded text-xs font-medium text-white"
                  style="background-color: #16a34a;"
                >
                  <CheckCircle2 class="w-3 h-3 mr-1" />
                  完结
                </span>
                <span
                  v-else
                  class="absolute top-3 left-3 inline-flex items-center px-2 py-0.5 rounded text-xs font-medium text-white"
                  style="background-color: var(--theme-primary);"
                >
                  连载中
                </span>
              </div>

              <div class="p-5 flex flex-col flex-1">
                <!-- 标题 -->
                <h3 class="text-lg font-semibold mb-1 line-clamp-1" style="color: var(--theme-text);">
                  {{ col.title }}
                </h3>
                <!-- 副标题 -->
                <p
                  v-if="col.subtitle"
                  class="text-sm mb-2 line-clamp-1"
                  style="color: var(--theme-text-secondary);"
                >
                  {{ col.subtitle }}
                </p>
                <!-- 描述 -->
                <p
                  v-if="col.description"
                  class="text-xs mb-3 line-clamp-2 flex-1"
                  style="color: var(--theme-text-secondary);"
                >
                  {{ col.description }}
                </p>
                <div v-else class="mb-3 flex-1"></div>

                <!-- 作者信息 -->
                <div class="flex items-center pt-3 border-t" style="border-color: var(--theme-border);">
                  <img
                    :src="getSafeAvatar(col.authorAvatar, String(col.userId))"
                    :alt="col.authorName || '作者'"
                    class="w-6 h-6 rounded-full object-cover mr-2 flex-shrink-0"
                    loading="lazy"
                  />
                  <span class="text-xs font-medium truncate flex-1" style="color: var(--theme-text);">
                    {{ col.authorName || '匿名作者' }}
                  </span>
                  <div class="flex items-center gap-2 text-xs flex-shrink-0" style="color: var(--theme-text-secondary);">
                    <span class="flex items-center">
                      <FileText class="w-3 h-3 mr-0.5" />
                      {{ formatNumber(col.articleCount) }}
                    </span>
                    <span class="flex items-center">
                      <Users class="w-3 h-3 mr-0.5" />
                      {{ formatNumber(col.subscribeCount) }}
                    </span>
                  </div>
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
          </div>
        </template>
      </div>
    </div>

    <SiteFooter />
  </div>
</template>
