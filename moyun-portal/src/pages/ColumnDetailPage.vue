<script setup lang="ts">
import { ref, computed, onMounted, watch } from 'vue';
import { useRoute, useRouter } from 'vue-router';
import { useHead } from '@vueuse/head';
import {
  BookOpen, Users, Eye, FileText, Heart, Clock,
  Plus, Trash2, ArrowUp, ArrowDown, Save, CheckCircle2,
  Bell, BellOff, Pencil, Loader2, ChevronRight,
  Gift,
} from 'lucide-vue-next';
import Breadcrumb from '@/components/Breadcrumb.vue';
import SiteFooter from '@/components/SiteFooter.vue';
import LazyImage from '@/components/LazyImage.vue';
import TipModal from '@/components/TipModal.vue';
import MyArticlePicker from '@/components/MyArticlePicker.vue';
import { generateSeo } from '@/utils/seo';
import { getSafeAvatar } from '@/utils/avatar';
import { formatShortDate } from '@/utils/date';
import {
  getColumnDetail, toggleSubscribe, toggleColumnFinish, deleteColumn,
  addArticle, removeArticle, sortArticles,
} from '@/api/column';
import { useUserStore } from '@/stores/user';
import { useAuth } from '@/composables/useAuth';
import { useToast } from '@/composables/useToast';
import type { ColumnVO, ArticleSimpleVO, ColumnArticleSortItem, Article } from '@/types/api';

const route = useRoute();
const router = useRouter();
const userStore = useUserStore();
const { requireAuth } = useAuth();
const toast = useToast();

const columnId = computed(() => route.params.id as string);
const loading = ref(false);
const error = ref<string | null>(null);
const column = ref<ColumnVO | null>(null);

const breadcrumbs = computed(() => [
  { label: '专栏广场', path: '/columns' },
  { label: column.value?.title || '专栏详情' },
]);

// 订阅
const subscribing = ref(false);
const isSubscribed = ref(false);
const subscribeCount = ref(0);

// 作者管理
const showManage = ref(false);
const managing = ref(false);
const sortDirty = ref(false);
const articleToAdd = ref('');
const addingArticle = ref(false);

const isOwner = computed(() => {
  if (!column.value || !userStore.user) return false;
  return String(column.value.userId) === String(userStore.user.id);
});

// 按 sortOrder 升序的文章列表
const sortedArticles = computed<ArticleSimpleVO[]>(() => {
  const arr = (column.value?.articles || []).slice();
  arr.sort((a, b) => (a.sortOrder ?? 0) - (b.sortOrder ?? 0));
  return arr;
});

useHead(computed(() => generateSeo({
  title: column.value?.title || '专栏详情',
  description: column.value?.description || column.value?.subtitle || '墨韵专栏，持续连载，订阅追更',
  keywords: ['专栏', column.value?.title || '墨韵'].filter(Boolean) as string[],
  canonicalPath: `/column/${columnId.value}`,
})));

onMounted(() => {
  loadDetail();
});

// 路由参数变化时（同一组件复用，如从 /column/A 跳转 /column/B）重新加载
watch(columnId, (newId, oldId) => {
  if (newId && newId !== oldId) {
    // 重置本地状态，避免显示旧专栏数据
    column.value = null;
    isSubscribed.value = false;
    subscribeCount.value = 0;
    showManage.value = false;
    sortDirty.value = false;
    loadDetail();
  }
});

async function loadDetail() {
  loading.value = true;
  error.value = null;
  try {
    const res = await getColumnDetail(columnId.value);
    if (res.code === 200 && res.data) {
      column.value = res.data;
      isSubscribed.value = !!res.data.isSubscribed;
      subscribeCount.value = res.data.subscribeCount || 0;
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

async function handleSubscribe() {
  if (!requireAuth(router.currentRoute.value.fullPath)) return;
  if (subscribing.value || !column.value) return;
  subscribing.value = true;
  try {
    const res = await toggleSubscribe(column.value.id);
    if (res.code === 200 && res.data) {
      isSubscribed.value = !!res.data.subscribed;
      subscribeCount.value = res.data.subscribeCount || 0;
      toast.success(isSubscribed.value ? '订阅成功' : '已取消订阅');
    } else {
      toast.error(res.message || '操作失败');
    }
  } catch (err) {
    const e = err as { message?: string };
    toast.error(e?.message || '操作失败，请稍后重试');
  } finally {
    subscribing.value = false;
  }
}

async function handleToggleFinish() {
  if (!column.value || managing.value) return;
  const action = column.value.isFinished ? '恢复连载' : '完结专栏';
  if (!window.confirm(`确定${action}「${column.value.title}」吗？`)) return;
  managing.value = true;
  try {
    const res = await toggleColumnFinish(column.value.id);
    if (res.code === 200 && res.data) {
      column.value = res.data;
      // toggleFinish 返回的是后端最新专栏详情，本地未保存的排序已被覆盖
      sortDirty.value = false;
      toast.success(`${action}成功`);
    } else {
      toast.error(res.message || `${action}失败`);
    }
  } catch (err) {
    const e = err as { message?: string };
    toast.error(e?.message || `${action}失败`);
  } finally {
    managing.value = false;
  }
}

async function handleDelete() {
  if (!column.value) return;
  if (!window.confirm(`确定删除专栏「${column.value.title}」吗？删除后不可恢复。`)) return;
  managing.value = true;
  try {
    const res = await deleteColumn(column.value.id);
    if (res.code === 200) {
      toast.success('删除成功');
      router.push('/columns');
    } else {
      toast.error(res.message || '删除失败');
    }
  } catch (err) {
    const e = err as { message?: string };
    toast.error(e?.message || '删除失败，请稍后重试');
  } finally {
    managing.value = false;
  }
}

function gotoEdit() {
  if (!column.value) return;
  router.push(`/column/edit/${column.value.id}`);
}

function gotoArticle(a: ArticleSimpleVO) {
  router.push(`/article/${a.id}`);
}

function toggleManage() {
  showManage.value = !showManage.value;
  if (!showManage.value) sortDirty.value = false;
}

function moveUp(index: number) {
  if (index <= 0) return;
  const arr = column.value?.articles;
  if (!arr) return;
  const tmp = arr[index];
  arr[index] = arr[index - 1];
  arr[index - 1] = tmp;
  // 重新分配 sortOrder
  reassignSortOrder();
  sortDirty.value = true;
}

function moveDown(index: number) {
  const arr = column.value?.articles;
  if (!arr) return;
  if (index >= arr.length - 1) return;
  const tmp = arr[index];
  arr[index] = arr[index + 1];
  arr[index + 1] = tmp;
  reassignSortOrder();
  sortDirty.value = true;
}

function reassignSortOrder() {
  const arr = column.value?.articles;
  if (!arr) return;
  // 按当前数组顺序重新分配 sortOrder（升序）
  arr.forEach((item, idx) => {
    item.sortOrder = idx;
  });
}

async function handleRemoveArticle(a: ArticleSimpleVO) {
  if (!column.value) return;
  if (!window.confirm(`确定将文章「${a.title}」移出专栏吗？`)) return;
  try {
    const res = await removeArticle(column.value.id, a.id);
    if (res.code === 200) {
      // 本地移除
      if (column.value.articles) {
        column.value.articles = column.value.articles.filter(x => String(x.id) !== String(a.id));
        reassignSortOrder();
      }
      column.value.articleCount = Math.max(0, (column.value.articleCount || 0) - 1);
      toast.success('已移出专栏');
    } else {
      toast.error(res.message || '移出失败');
    }
  } catch (err) {
    const e = err as { message?: string };
    toast.error(e?.message || '移出失败，请稍后重试');
  }
}

async function handleSaveSort() {
  if (!column.value || !column.value.articles) return;
  // 使用 sortedArticles（按 sortOrder 升序的显示顺序）作为保存源，
  // 确保保存的顺序与用户在页面上看到的顺序一致；
  // 同时按显示顺序重新分配连续的 sortOrder（0,1,2,...），避免稀疏值
  const list: ColumnArticleSortItem[] = sortedArticles.value.map((item, idx) => ({
    id: item.id,
    sortOrder: idx,
  }));
  try {
    const res = await sortArticles(column.value.id, list);
    if (res.code === 200) {
      // 同步本地 sortOrder，使 sortedArticles 与 column.value.articles 顺序一致
      sortedArticles.value.forEach((item, idx) => {
        item.sortOrder = idx;
      });
      sortDirty.value = false;
      toast.success('排序已保存');
    } else {
      toast.error(res.message || '保存排序失败');
    }
  } catch (err) {
    const e = err as { message?: string };
    toast.error(e?.message || '保存排序失败，请稍后重试');
  }
}

async function handleAddArticle() {
  if (!column.value) return;
  const aid = articleToAdd.value.trim();
  if (!aid) {
    toast.error('请输入文章ID');
    return;
  }
  addingArticle.value = true;
  try {
    const res = await addArticle(column.value.id, aid);
    if (res.code === 200) {
      articleToAdd.value = '';
      toast.success('加入成功');
      await loadDetail();
    } else {
      toast.error(res.message || '加入失败');
    }
  } catch (err) {
    const e = err as { message?: string };
    toast.error(e?.message || '加入失败，请稍后重试');
  } finally {
    addingArticle.value = false;
  }
}

// v1.1.3 新增：从 MyArticlePicker 选择文章后增量加入（替代旧的"输入文章ID"）
async function handleSelectArticle(article: Article) {
  if (!column.value) return;
  addingArticle.value = true;
  try {
    const res = await addArticle(column.value.id, article.id);
    if (res.code === 200) {
      toast.success(`已加入「${article.title}」`);
      await loadDetail();
    } else {
      toast.error(res.message || '加入失败');
    }
  } catch (err) {
    const e = err as { message?: string };
    toast.error(e?.message || '加入失败，请稍后重试');
  } finally {
    addingArticle.value = false;
  }
}

// v1.1.3 新增：候选文章列表中要排除已在专栏中的文章 ID
const existingArticleIds = computed(() => {
  return (column.value?.articles || []).map(a => a.id);
});

// ============ 打赏 ============
const showTipModal = ref(false);

function openTipModal() {
  if (!requireAuth(router.currentRoute.value.fullPath)) return;
  if (!column.value) return;
  showTipModal.value = true;
}

function onTipSuccess() {
  toast.success('鼓励成功，感谢支持创作者！');
  showTipModal.value = false;
}

function onTipError(message: string) {
  toast.error(message || '鼓励失败');
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
        </div>
      </div>
    </div>

    <!-- 加载状态 -->
    <div v-if="loading" class="flex flex-col items-center justify-center py-24">
      <Loader2 class="w-10 h-10 animate-spin" style="color: var(--theme-primary);" />
      <p class="mt-4 text-sm" style="color: var(--theme-text-secondary);">加载中...</p>
    </div>

    <!-- 错误状态 -->
    <div
      v-else-if="error"
      class="rounded-xl border p-8 max-w-md mx-auto text-center mt-12"
      style="background-color: var(--theme-surface); border-color: var(--theme-border);"
    >
      <p class="mb-4 text-sm" style="color: var(--theme-text);">{{ error }}</p>
      <button
        @click="loadDetail"
        class="px-4 py-2 text-white rounded-lg text-sm transition hover:opacity-90"
        style="background-color: var(--theme-primary);"
      >
        重试
      </button>
    </div>

    <!-- 专栏内容 -->
    <template v-else-if="column">
      <!-- Hero 区 -->
      <div class="py-6 sm:py-8">
        <div class="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8">
          <div class="relative overflow-hidden rounded-2xl text-white" style="background-image: radial-gradient(circle at 20% 50%, rgba(190, 24, 93, 0.3) 0%, transparent 50%), radial-gradient(circle at 80% 30%, rgba(124, 58, 237, 0.3) 0%, transparent 50%), linear-gradient(135deg, #be185d 0%, #a21caf 50%, #7c3aed 100%);">
        <div class="absolute inset-0 opacity-10 pointer-events-none" aria-hidden="true">
          <svg class="absolute top-6 left-8 w-32 h-32 text-white" viewBox="0 0 24 24" fill="currentColor"><path d="M3 17.25V21h3.75L17.81 9.94l-3.75-3.75L3 17.25zM20.71 7.04c.39-.39.39-1.02 0-1.41l-2.34-2.34c-.39-.39-1.02-.39-1.41 0l-1.83 1.83 3.75 3.75 1.83-1.83z"/></svg>
          <svg class="absolute bottom-4 right-10 w-40 h-40 text-white" viewBox="0 0 24 24" fill="currentColor"><path d="M19 3H5c-1.1 0-2 .9-2 2v14c0 1.1.9 2 2 2h14c1.1 0 2-.9 2-2V5c0-1.1-.9-2-2-2zm-2 10H7v-2h10v2zm0-4H7V7h10v2z"/></svg>
        </div>
        <div class="relative px-6 py-8 sm:px-10 sm:py-10">
          <div class="flex flex-col md:flex-row gap-6 items-start">
            <!-- 封面 -->
            <div class="w-32 h-44 md:w-40 md:h-56 rounded-lg overflow-hidden shadow-lg flex-shrink-0 mx-auto md:mx-0" style="background-color: var(--theme-bg);">
              <LazyImage
                v-if="column.cover"
                :src="column.cover"
                :alt="column.title"
                class="w-full h-full object-cover"
              />
              <div v-else class="w-full h-full flex items-center justify-center" style="background: linear-gradient(135deg, var(--theme-accent), color-mix(in srgb, var(--theme-accent) 50%, #c4b5fd));">
                <BookOpen class="w-12 h-12" style="color: var(--theme-primary); opacity: 0.6;" />
              </div>
            </div>
            <!-- 信息 -->
            <div class="flex-1 min-w-0 text-center md:text-left">
              <!-- 完结标识 -->
              <div class="mb-2">
                <span
                  v-if="column.isFinished"
                  class="inline-flex items-center px-2.5 py-0.5 rounded-full text-xs font-medium"
                  style="background-color: rgba(255,255,255,0.2);"
                >
                  <CheckCircle2 class="w-3 h-3 mr-1" />已完结
                </span>
                <span
                  v-else
                  class="inline-flex items-center px-2.5 py-0.5 rounded-full text-xs font-medium"
                  style="background-color: rgba(255,255,255,0.2);"
                >
                  <BookOpen class="w-3 h-3 mr-1" />连载中
                </span>
              </div>
              <h1 class="text-2xl md:text-3xl font-bold mb-2">{{ column.title }}</h1>
              <p v-if="column.subtitle" class="text-sm md:text-base opacity-90 mb-3">{{ column.subtitle }}</p>
              <!-- 作者信息 -->
              <div class="flex items-center justify-center md:justify-start gap-2 mb-3">
                <img
                  :src="getSafeAvatar(column.authorAvatar, String(column.userId))"
                  :alt="column.authorName || '作者'"
                  class="w-8 h-8 rounded-full object-cover"
                  loading="lazy"
                />
                <div class="text-left">
                  <div class="text-sm font-medium">{{ column.authorName || '匿名作者' }}</div>
                  <div v-if="column.authorBio" class="text-xs opacity-80 line-clamp-1">{{ column.authorBio }}</div>
                </div>
              </div>
              <!-- 简介 -->
              <p v-if="column.description" class="text-sm opacity-90 mb-4 line-clamp-3">{{ column.description }}</p>
              <!-- 操作 -->
              <div class="flex items-center justify-center md:justify-start gap-2 flex-wrap">
                <button
                  @click="handleSubscribe"
                  :disabled="subscribing"
                  class="inline-flex items-center px-4 py-2 rounded-lg text-sm font-medium transition hover:opacity-90 disabled:opacity-50"
                  :style="{
                    backgroundColor: isSubscribed ? 'rgba(255,255,255,0.2)' : '#fff',
                    color: isSubscribed ? '#fff' : 'var(--theme-primary)',
                  }"
                >
                  <component :is="isSubscribed ? BellOff : Bell" class="w-4 h-4 mr-1.5" />
                  {{ isSubscribed ? '已订阅' : '+ 订阅' }}
                </button>
                <button
                  v-if="!isOwner"
                  @click="openTipModal"
                  class="inline-flex items-center px-4 py-2 rounded-lg text-sm font-medium transition hover:opacity-90"
                  style="background-color: rgba(255,255,255,0.2); color: #fff;"
                >
                  <Gift class="w-4 h-4 mr-1.5" />
                  打赏
                </button>
                <button
                  v-if="isOwner"
                  @click="gotoEdit"
                  class="inline-flex items-center px-3 py-2 rounded-lg text-sm font-medium transition hover:opacity-90"
                  style="background-color: rgba(255,255,255,0.2); color: #fff;"
                >
                  <Pencil class="w-4 h-4 mr-1.5" />编辑
                </button>
                <button
                  v-if="isOwner"
                  @click="toggleManage"
                  class="inline-flex items-center px-3 py-2 rounded-lg text-sm font-medium transition hover:opacity-90"
                  :style="{
                    backgroundColor: showManage ? '#fff' : 'rgba(255,255,255,0.2)',
                    color: showManage ? 'var(--theme-primary)' : '#fff',
                  }"
                >
                  <FileText class="w-4 h-4 mr-1.5" />管理文章
                </button>
              </div>
            </div>
          </div>
        </div>
          </div>
        </div>
      </div>

      <!-- 统计栏 -->
      <div class="border-b" style="background-color: var(--theme-surface); border-color: var(--theme-border);">
        <div class="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 py-4 flex items-center justify-center md:justify-start gap-6 flex-wrap">
          <span class="flex items-center text-sm" style="color: var(--theme-text-secondary);">
            <FileText class="w-4 h-4 mr-1.5" style="color: var(--theme-primary);" />
            {{ formatNumber(column.articleCount) }} 篇文章
          </span>
          <span class="flex items-center text-sm" style="color: var(--theme-text-secondary);">
            <Users class="w-4 h-4 mr-1.5" style="color: var(--theme-primary);" />
            {{ formatNumber(subscribeCount) }} 订阅
          </span>
          <span class="flex items-center text-sm" style="color: var(--theme-text-secondary);">
            <Eye class="w-4 h-4 mr-1.5" style="color: var(--theme-primary);" />
            {{ formatNumber(column.viewCount) }} 浏览
          </span>
        </div>
      </div>

      <!-- 内容区 -->
      <div class="flex-1 py-8">
        <div class="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8">
          <!-- 作者管理面板 -->
          <div
            v-if="isOwner && showManage"
            class="rounded-xl border p-5 mb-6"
            style="background-color: var(--theme-surface); border-color: var(--theme-border);"
          >
            <div class="flex items-center justify-between mb-4 flex-wrap gap-2">
              <h3 class="text-base font-semibold flex items-center" style="color: var(--theme-text);">
                <FileText class="w-4 h-4 mr-2" style="color: var(--theme-primary);" />管理文章
              </h3>
              <div class="flex items-center gap-2 flex-wrap">
                <button
                  @click="handleSaveSort"
                  :disabled="!sortDirty"
                  class="inline-flex items-center px-3 py-1.5 rounded-lg text-xs font-medium text-white transition hover:opacity-90 disabled:opacity-40 disabled:cursor-not-allowed"
                  style="background-color: var(--theme-primary);"
                >
                  <Save class="w-3.5 h-3.5 mr-1" />保存排序
                </button>
                <button
                  @click="handleToggleFinish"
                  :disabled="managing"
                  class="inline-flex items-center px-3 py-1.5 rounded-lg text-xs font-medium text-white transition hover:opacity-90"
                  style="background-color: #16a34a;"
                >
                  <CheckCircle2 class="w-3.5 h-3.5 mr-1" />
                  {{ column.isFinished ? '恢复连载' : '完结专栏' }}
                </button>
                <button
                  @click="handleDelete"
                  :disabled="managing"
                  class="inline-flex items-center px-3 py-1.5 rounded-lg text-xs font-medium text-white transition hover:opacity-90"
                  style="background-color: #ef4444;"
                >
                  <Trash2 class="w-3.5 h-3.5 mr-1" />删除专栏
                </button>
              </div>
            </div>

            <!-- v1.1.3 改造：从"输入文章ID"升级为"我的文章选择器"，与编辑页统一组件 -->
            <div class="mb-4 p-3 rounded-lg" style="background-color: var(--theme-bg);">
              <div class="flex items-center gap-2 mb-2">
                <Plus class="w-4 h-4 flex-shrink-0" style="color: var(--theme-text-secondary);" />
                <span class="text-xs" style="color: var(--theme-text-secondary);">从我的已发布文章中选择加入</span>
                <Loader2 v-if="addingArticle" class="w-3.5 h-3.5 animate-spin ml-auto" style="color: var(--theme-primary);" />
              </div>
              <MyArticlePicker
                :model-value="[]"
                :exclude-ids="existingArticleIds"
                :multiple="false"
                placeholder="搜索文章标题加入专栏..."
                @select="handleSelectArticle"
              />
            </div>

            <p v-if="sortedArticles.length === 0" class="text-sm text-center py-4" style="color: var(--theme-text-secondary);">
              专栏还没有文章，从上方搜索选择加入吧
            </p>
          </div>

          <!-- 文章目录 -->
          <div class="mb-6">
            <h2 class="text-lg font-semibold mb-4 flex items-center" style="color: var(--theme-text);">
              <BookOpen class="w-5 h-5 mr-2" style="color: var(--theme-primary);" />
              文章目录
              <span class="ml-2 text-xs font-normal" style="color: var(--theme-text-secondary);">
                共 {{ sortedArticles.length }} 篇
              </span>
            </h2>

            <!-- 空状态 -->
            <div
              v-if="sortedArticles.length === 0"
              class="rounded-xl border p-10 text-center"
              style="background-color: var(--theme-surface); border-color: var(--theme-border);"
            >
              <FileText class="w-10 h-10 mx-auto mb-2" style="color: var(--theme-text-secondary); opacity: 0.5;" />
              <p class="text-sm" style="color: var(--theme-text-secondary);">专栏还没有文章</p>
            </div>

            <!-- 文章列表 -->
            <div v-else class="space-y-3">
              <div
                v-for="(a, idx) in sortedArticles"
                :key="a.id"
                class="rounded-xl border p-4 flex items-center gap-3 transition hover:shadow-sm"
                style="background-color: var(--theme-surface); border-color: var(--theme-border);"
              >
                <!-- 管理模式下的排序按钮 -->
                <div v-if="isOwner && showManage" class="flex flex-col gap-1 flex-shrink-0">
                  <button
                    @click="moveUp(idx)"
                    :disabled="idx === 0"
                    class="p-1 rounded transition hover:opacity-80 disabled:opacity-30 disabled:cursor-not-allowed"
                    style="color: var(--theme-text-secondary);"
                    title="上移"
                  >
                    <ArrowUp class="w-3.5 h-3.5" />
                  </button>
                  <button
                    @click="moveDown(idx)"
                    :disabled="idx === sortedArticles.length - 1"
                    class="p-1 rounded transition hover:opacity-80 disabled:opacity-30 disabled:cursor-not-allowed"
                    style="color: var(--theme-text-secondary);"
                    title="下移"
                  >
                    <ArrowDown class="w-3.5 h-3.5" />
                  </button>
                </div>

                <!-- 序号 -->
                <div class="text-sm font-mono w-6 text-center flex-shrink-0" style="color: var(--theme-text-secondary);">
                  {{ idx + 1 }}
                </div>

                <!-- 内容 -->
                <div class="flex-1 min-w-0">
                  <h3
                    class="text-sm font-semibold mb-1 line-clamp-1 cursor-pointer hover:underline"
                    style="color: var(--theme-text);"
                    @click="gotoArticle(a)"
                  >
                    {{ a.title }}
                  </h3>
                  <p
                    v-if="a.excerpt"
                    class="text-xs line-clamp-1 mb-1"
                    style="color: var(--theme-text-secondary);"
                  >
                    {{ a.excerpt }}
                  </p>
                  <div class="flex items-center gap-3 text-xs flex-wrap" style="color: var(--theme-text-secondary);">
                    <span v-if="a.viewCount != null" class="flex items-center">
                      <Eye class="w-3 h-3 mr-0.5" />{{ formatNumber(a.viewCount) }}
                    </span>
                    <span v-if="a.likeCount != null" class="flex items-center">
                      <Heart class="w-3 h-3 mr-0.5" />{{ formatNumber(a.likeCount) }}
                    </span>
                    <span v-if="a.createdTime" class="flex items-center">
                      <Clock class="w-3 h-3 mr-0.5" />{{ formatShortDate(a.createdTime) }}
                    </span>
                  </div>
                </div>

                <!-- 操作 -->
                <div class="flex items-center gap-2 flex-shrink-0">
                  <button
                    @click="gotoArticle(a)"
                    class="inline-flex items-center px-2.5 py-1.5 rounded-lg text-xs font-medium transition hover:opacity-80"
                    style="background-color: var(--theme-accent); color: var(--theme-primary);"
                  >
                    阅读
                    <ChevronRight class="w-3 h-3 ml-0.5" />
                  </button>
                  <button
                    v-if="isOwner && showManage"
                    @click="handleRemoveArticle(a)"
                    class="inline-flex items-center px-2 py-1.5 rounded-lg text-xs transition hover:opacity-80"
                    style="color: #ef4444; border: 1px solid var(--theme-border);"
                    title="移出专栏"
                  >
                    <Trash2 class="w-3.5 h-3.5" />
                  </button>
                </div>
              </div>
            </div>
          </div>
        </div>
      </div>
    </template>

    <!-- 打赏弹窗（积分打赏 MVP） -->
    <TipModal
      :show="showTipModal"
      target-type="column"
      :target-id="column?.id || ''"
      :author-avatar="column?.authorAvatar"
      :author-name="column?.authorName"
      :target-title="column?.title"
      @close="showTipModal = false"
      @success="onTipSuccess"
      @error="onTipError"
    />

    <SiteFooter />
  </div>
</template>
