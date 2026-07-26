<script setup lang="ts">
import { ref, computed, onMounted, watch } from 'vue';
import { useRouter } from 'vue-router';
import { useHead } from '@vueuse/head';
import {
  BookOpen, Plus, Pencil, Trash2, CheckCircle2,
  FileText, Users, ChevronLeft, ChevronRight, Settings,
} from 'lucide-vue-next';
import Breadcrumb from '@/components/Breadcrumb.vue';
import SiteFooter from '@/components/SiteFooter.vue';
import LazyImage from '@/components/LazyImage.vue';
import { generateSeo } from '@/utils/seo';
import { getSafeAvatar } from '@/utils/avatar';
import { getMyColumns, getSubscribedColumns, deleteColumn } from '@/api/column';
import { useToast } from '@/composables/useToast';
import type { ColumnListItemVO, ColumnQuery } from '@/types/api';

const router = useRouter();
const toast = useToast();

type TabKey = 'created' | 'subscribed';
const activeTab = ref<TabKey>('created');

const loading = ref(false);
const error = ref<string | null>(null);
const columns = ref<ColumnListItemVO[]>([]);
const total = ref(0);
const page = ref(1);
const pageSize = 12;
const actionId = ref<string | number | null>(null);

const totalPages = computed(() => Math.max(1, Math.ceil(total.value / pageSize)));

const breadcrumbs = computed(() => [
  { label: '专栏广场', path: '/columns' },
  { label: '我的专栏' },
]);

useHead(computed(() => generateSeo({
  title: '我的专栏',
  description: '管理我创建的专栏与我订阅的专栏',
  keywords: ['我的专栏', '专栏管理', '订阅专栏', '墨韵'],
  canonicalPath: '/column/my',
  robots: 'noindex,nofollow',
})));

onMounted(() => {
  loadColumns();
});

watch(page, () => {
  loadColumns();
});

watch(activeTab, () => {
  if (page.value !== 1) {
    page.value = 1;
  } else {
    loadColumns();
  }
});

async function loadColumns() {
  loading.value = true;
  error.value = null;
  try {
    const params: ColumnQuery = { pageNum: page.value, pageSize };
    const res = activeTab.value === 'created'
      ? await getMyColumns(params)
      : await getSubscribedColumns(params);
    if (res.code === 200 && res.data) {
      columns.value = res.data.list || [];
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

function switchTab(tab: TabKey) {
  if (activeTab.value === tab) return;
  activeTab.value = tab;
}

function gotoDetail(id: string | number) {
  router.push(`/column/${id}`);
}

function gotoCreate() {
  router.push('/column/create');
}

function gotoEdit(id: string | number) {
  router.push(`/column/edit/${id}`);
}

async function handleDelete(col: ColumnListItemVO) {
  if (actionId.value) return;
  if (!window.confirm(`确定删除专栏「${col.title}」吗？删除后不可恢复。`)) return;
  actionId.value = col.id;
  try {
    const res = await deleteColumn(col.id);
    if (res.code === 200) {
      // 本地移除
      columns.value = columns.value.filter(c => String(c.id) !== String(col.id));
      total.value = Math.max(0, total.value - 1);
      toast.success('删除成功');
      if (columns.value.length === 0 && page.value > 1) {
        page.value -= 1;
      }
    } else {
      toast.error(res.message || '删除失败');
    }
  } catch (err) {
    const e = err as { message?: string };
    toast.error(e?.message || '删除失败，请稍后重试');
  } finally {
    actionId.value = null;
  }
}

function gotoPage(p: number) {
  if (p < 1 || p > totalPages.value) return;
  page.value = p;
  window.scrollTo({ top: 0, behavior: 'smooth' });
}

function goBack() {
  if (window.history.length > 1) {
    router.back();
  } else {
    router.push('/columns');
  }
}

function formatNumber(n?: number) {
  const v = n || 0;
  if (v >= 10000) return (v / 10000).toFixed(1) + 'w';
  if (v >= 1000) return (v / 1000).toFixed(1) + 'k';
  return String(v);
}
</script>

<template>
  <div class="min-h-screen flex flex-col" style="background-color: var(--theme-bg);">
    <div class="border-b sticky top-0 z-30 backdrop-blur-sm py-3" style="background-color: var(--theme-surface); border-color: var(--theme-border);">
      <div class="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 flex items-center justify-between gap-4">
        <Breadcrumb :items="breadcrumbs" />
        <div class="flex items-center gap-2">
          <button
            @click="gotoCreate"
            class="flex items-center text-sm text-white px-3 py-1.5 rounded-lg transition hover:opacity-90"
            style="background-color: var(--theme-primary);"
          >
            <Plus class="w-4 h-4 mr-1" />
            创建专栏
          </button>
        </div>
      </div>
    </div>

    <!-- Tab 切换 -->
    <div class="border-b" style="background-color: var(--theme-surface); border-color: var(--theme-border);">
      <div class="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 flex items-center gap-1">
        <button
          @click="switchTab('created')"
          class="flex items-center px-5 py-3 text-sm font-medium transition relative"
          :style="{
            color: activeTab === 'created' ? 'var(--theme-primary)' : 'var(--theme-text-secondary)',
          }"
        >
          <Settings class="w-4 h-4 mr-1.5" />
          我创建的
          <span
            v-if="activeTab === 'created'"
            class="absolute left-3 right-3 bottom-0 h-0.5 rounded-full"
            style="background-color: var(--theme-primary);"
          ></span>
        </button>
        <button
          @click="switchTab('subscribed')"
          class="flex items-center px-5 py-3 text-sm font-medium transition relative"
          :style="{
            color: activeTab === 'subscribed' ? 'var(--theme-primary)' : 'var(--theme-text-secondary)',
          }"
        >
          <BookOpen class="w-4 h-4 mr-1.5" />
          我订阅的
          <span
            v-if="activeTab === 'subscribed'"
            class="absolute left-3 right-3 bottom-0 h-0.5 rounded-full"
            style="background-color: var(--theme-primary);"
          ></span>
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
          <p v-if="activeTab === 'created'" class="text-sm mb-4" style="color: var(--theme-text-secondary);">
            还没有创建专栏，立即创建一个吧
          </p>
          <p v-else class="text-sm mb-4" style="color: var(--theme-text-secondary);">
            还没有订阅任何专栏，去专栏广场发现感兴趣的专栏
          </p>
          <button
            v-if="activeTab === 'created'"
            @click="gotoCreate"
            class="inline-flex items-center px-4 py-2 text-white rounded-lg text-sm transition hover:opacity-90"
            style="background-color: var(--theme-primary);"
          >
            <Plus class="w-4 h-4 mr-1" />
            创建专栏
          </button>
          <button
            v-else
            @click="goBack"
            class="inline-flex items-center px-4 py-2 text-white rounded-lg text-sm transition hover:opacity-90"
            style="background-color: var(--theme-primary);"
          >
            去专栏广场
          </button>
        </div>

        <!-- 专栏卡片列表 -->
        <template v-else>
          <div class="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-5 mb-8">
            <div
              v-for="col in columns"
              :key="col.id"
              class="rounded-xl overflow-hidden border shadow-sm hover:shadow-md transition flex flex-col"
              style="background-color: var(--theme-surface); border-color: var(--theme-border);"
            >
              <!-- 封面（点击进详情） -->
              <div
                @click="gotoDetail(col.id)"
                class="relative h-36 cursor-pointer"
                style="background-color: var(--theme-bg);"
              >
                <LazyImage
                  v-if="col.cover"
                  :src="col.cover"
                  :alt="col.title"
                  class="w-full h-full object-cover"
                />
                <div v-else class="w-full h-full flex items-center justify-center" style="background: linear-gradient(135deg, var(--theme-accent), color-mix(in srgb, var(--theme-accent) 50%, #c4b5fd));">
                  <BookOpen class="w-10 h-10" style="color: var(--theme-primary); opacity: 0.6;" />
                </div>
                <span
                  v-if="col.isFinished"
                  class="absolute top-3 left-3 inline-flex items-center px-2 py-0.5 rounded text-xs font-medium text-white"
                  style="background-color: #16a34a;"
                >
                  <CheckCircle2 class="w-3 h-3 mr-1" />完结
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
                <h3
                  @click="gotoDetail(col.id)"
                  class="text-base font-semibold mb-1 line-clamp-1 cursor-pointer hover:underline"
                  style="color: var(--theme-text);"
                >
                  {{ col.title }}
                </h3>
                <p
                  v-if="col.subtitle"
                  class="text-xs mb-2 line-clamp-1"
                  style="color: var(--theme-text-secondary);"
                >
                  {{ col.subtitle }}
                </p>
                <div class="flex-1"></div>

                <!-- 作者 + 统计 -->
                <div class="flex items-center pt-3 border-t mb-3" style="border-color: var(--theme-border);">
                  <img
                    :src="getSafeAvatar(col.authorAvatar, String(col.userId))"
                    :alt="col.authorName || '作者'"
                    class="w-5 h-5 rounded-full object-cover mr-2 flex-shrink-0"
                    loading="lazy"
                  />
                  <span class="text-xs truncate flex-1" style="color: var(--theme-text);">
                    {{ col.authorName || '匿名作者' }}
                  </span>
                  <div class="flex items-center gap-2 text-xs flex-shrink-0" style="color: var(--theme-text-secondary);">
                    <span class="flex items-center">
                      <FileText class="w-3 h-3 mr-0.5" />{{ formatNumber(col.articleCount) }}
                    </span>
                    <span class="flex items-center">
                      <Users class="w-3 h-3 mr-0.5" />{{ formatNumber(col.subscribeCount) }}
                    </span>
                  </div>
                </div>

                <!-- 操作（仅我创建的 Tab 显示） -->
                <div v-if="activeTab === 'created'" class="flex items-center gap-1.5">
                  <button
                    @click="gotoDetail(col.id)"
                    class="inline-flex items-center px-2.5 py-1.5 rounded-lg text-xs transition hover:opacity-80 flex-1 justify-center"
                    style="background-color: var(--theme-bg); color: var(--theme-text); border: 1px solid var(--theme-border);"
                  >
                    <Settings class="w-3 h-3 mr-1" />管理
                  </button>
                  <button
                    @click="gotoEdit(col.id)"
                    class="inline-flex items-center px-2.5 py-1.5 rounded-lg text-xs transition hover:opacity-80"
                    style="background-color: var(--theme-bg); color: var(--theme-primary); border: 1px solid var(--theme-border);"
                  >
                    <Pencil class="w-3 h-3 mr-1" />编辑
                  </button>
                  <button
                    @click="handleDelete(col)"
                    :disabled="actionId === col.id"
                    class="inline-flex items-center px-2.5 py-1.5 rounded-lg text-xs text-white transition hover:opacity-80 disabled:opacity-50"
                    style="background-color: #ef4444;"
                  >
                    <Trash2 class="w-3 h-3" />
                  </button>
                </div>
                <div v-else class="flex items-center">
                  <button
                    @click="gotoDetail(col.id)"
                    class="inline-flex items-center px-3 py-1.5 rounded-lg text-xs transition hover:opacity-80 w-full justify-center"
                    style="background-color: var(--theme-accent); color: var(--theme-primary);"
                  >
                    查看专栏
                  </button>
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
            <span class="ml-2 text-xs" style="color: var(--theme-text-secondary);">共 {{ total }} 个</span>
          </div>
        </template>
      </div>
    </div>

    <SiteFooter />
  </div>
</template>
