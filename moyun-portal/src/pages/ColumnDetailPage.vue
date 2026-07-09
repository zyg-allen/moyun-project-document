<script setup lang="ts">
import { ref, computed, onMounted, watch } from 'vue';
import { useRoute, useRouter } from 'vue-router';
import { useHead } from '@vueuse/head';
import {
  ArrowLeft, BookOpen, Users, Eye, FileText, Heart, Clock,
  Plus, Trash2, ArrowUp, ArrowDown, Save, CheckCircle2,
  Bell, BellOff, Pencil, Loader2, ChevronRight,
  Gift, X,
} from 'lucide-vue-next';
import SiteFooter from '@/components/SiteFooter.vue';
import LazyImage from '@/components/LazyImage.vue';
import { generateSeo } from '@/utils/seo';
import { getSafeAvatar } from '@/utils/avatar';
import { formatShortDate } from '@/utils/date';
import {
  getColumnDetail, toggleSubscribe, toggleColumnFinish, deleteColumn,
  addArticle, removeArticle, sortArticles,
} from '@/api/column';
import { tipTarget } from '@/api/tip';
import { useUserStore } from '@/stores/user';
import { useAuth } from '@/composables/useAuth';
import type { ColumnVO, ArticleSimpleVO, ColumnArticleSortItem } from '@/types/api';

const route = useRoute();
const router = useRouter();
const userStore = useUserStore();
const { requireAuth } = useAuth();

const columnId = computed(() => route.params.id as string);
const loading = ref(false);
const error = ref<string | null>(null);
const column = ref<ColumnVO | null>(null);

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

// Toast
const toast = ref<{ message: string; type: 'success' | 'error' } | null>(null);
let toastTimer: number | null = null;
function showToast(message: string, type: 'success' | 'error' = 'success') {
  toast.value = { message, type };
  if (toastTimer) window.clearTimeout(toastTimer);
  toastTimer = window.setTimeout(() => { toast.value = null; }, 3000);
}

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
      showToast(isSubscribed.value ? '订阅成功' : '已取消订阅', 'success');
    } else {
      showToast(res.message || '操作失败', 'error');
    }
  } catch (err) {
    const e = err as { message?: string };
    showToast(e?.message || '操作失败，请稍后重试', 'error');
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
      showToast(`${action}成功`, 'success');
    } else {
      showToast(res.message || `${action}失败`, 'error');
    }
  } catch (err) {
    const e = err as { message?: string };
    showToast(e?.message || `${action}失败`, 'error');
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
      showToast('删除成功', 'success');
      router.push('/columns');
    } else {
      showToast(res.message || '删除失败', 'error');
    }
  } catch (err) {
    const e = err as { message?: string };
    showToast(e?.message || '删除失败，请稍后重试', 'error');
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
      showToast('已移出专栏', 'success');
    } else {
      showToast(res.message || '移出失败', 'error');
    }
  } catch (err) {
    const e = err as { message?: string };
    showToast(e?.message || '移出失败，请稍后重试', 'error');
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
      showToast('排序已保存', 'success');
    } else {
      showToast(res.message || '保存排序失败', 'error');
    }
  } catch (err) {
    const e = err as { message?: string };
    showToast(e?.message || '保存排序失败，请稍后重试', 'error');
  }
}

async function handleAddArticle() {
  if (!column.value) return;
  const aid = articleToAdd.value.trim();
  if (!aid) {
    showToast('请输入文章ID', 'error');
    return;
  }
  addingArticle.value = true;
  try {
    const res = await addArticle(column.value.id, aid);
    if (res.code === 200) {
      articleToAdd.value = '';
      showToast('加入成功', 'success');
      await loadDetail();
    } else {
      showToast(res.message || '加入失败', 'error');
    }
  } catch (err) {
    const e = err as { message?: string };
    showToast(e?.message || '加入失败，请稍后重试', 'error');
  } finally {
    addingArticle.value = false;
  }
}

// ============ 打赏 ============
const showTipModal = ref(false);
const tipAmount = ref<number>(5);
const tipMessage = ref('');
const tipping = ref(false);
const tipPresetAmounts = [2, 5, 10, 20, 50, 100];

function openTipModal() {
  if (!requireAuth(router.currentRoute.value.fullPath)) return;
  if (!column.value) return;
  tipAmount.value = 5;
  tipMessage.value = '';
  showTipModal.value = true;
}

function closeTipModal() {
  if (tipping.value) return;
  showTipModal.value = false;
}

function selectTipAmount(amount: number) {
  tipAmount.value = amount;
}

async function handleTip() {
  if (!column.value) return;
  if (!tipAmount.value || tipAmount.value <= 0) {
    showToast('请输入有效的打赏金额', 'error');
    return;
  }
  tipping.value = true;
  try {
    const res = await tipTarget('column', column.value.id, {
      amount: tipAmount.value,
      message: tipMessage.value,
    });
    if (res.code === 200) {
      showToast('打赏成功，感谢支持！', 'success');
      showTipModal.value = false;
    } else {
      showToast(res.message || '打赏失败', 'error');
    }
  } catch (err) {
    const e = err as { message?: string };
    showToast(e?.message || '打赏失败，请稍后重试', 'error');
  } finally {
    tipping.value = false;
  }
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
          返回专栏广场
        </button>
        <span class="text-sm font-medium" style="color: var(--theme-text);">专栏详情</span>
        <span class="w-20"></span>
      </div>
    </div>

    <!-- Toast -->
    <div
      v-if="toast"
      class="fixed top-20 left-1/2 -translate-x-1/2 z-50 px-4 py-2 rounded-lg shadow-lg text-sm"
      :class="toast.type === 'success' ? 'bg-green-500 text-white' : 'bg-red-500 text-white'"
    >
      {{ toast.message }}
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

            <!-- 加入文章 -->
            <div class="flex items-center gap-2 mb-4 p-3 rounded-lg" style="background-color: var(--theme-bg);">
              <Plus class="w-4 h-4 flex-shrink-0" style="color: var(--theme-text-secondary);" />
              <input
                v-model="articleToAdd"
                @keyup.enter="handleAddArticle"
                type="text"
                placeholder="输入文章ID加入专栏..."
                class="flex-1 px-2 py-1.5 rounded-md text-sm focus:outline-none"
                style="background-color: var(--theme-surface); color: var(--theme-text); border: 1px solid var(--theme-border);"
              />
              <button
                @click="handleAddArticle"
                :disabled="addingArticle"
                class="inline-flex items-center px-3 py-1.5 rounded-md text-xs font-medium text-white transition hover:opacity-90 disabled:opacity-50"
                style="background-color: var(--theme-primary);"
              >
                加入
              </button>
            </div>

            <p v-if="sortedArticles.length === 0" class="text-sm text-center py-4" style="color: var(--theme-text-secondary);">
              专栏还没有文章，输入文章ID加入吧
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

    <!-- 打赏弹窗 -->
    <div
      v-if="showTipModal"
      class="fixed inset-0 z-50 flex items-center justify-center p-4"
      style="background-color: rgba(0, 0, 0, 0.5);"
      @click.self="closeTipModal"
    >
      <div
        class="w-full max-w-md rounded-2xl shadow-xl"
        style="background-color: var(--theme-surface); border: 1px solid var(--theme-border);"
        role="dialog"
        aria-modal="true"
        aria-labelledby="tip-modal-title"
      >
        <!-- 头部 -->
        <div class="flex items-center justify-between p-5 border-b" style="border-color: var(--theme-border);">
          <h3 id="tip-modal-title" class="text-lg font-bold flex items-center gap-2" style="color: var(--theme-text);">
            <Gift class="w-5 h-5" style="color: var(--theme-primary);" />
            打赏专栏作者
          </h3>
          <button
            @click="closeTipModal"
            :disabled="tipping"
            class="p-1 rounded-lg transition hover:opacity-70 disabled:opacity-40"
            style="color: var(--theme-text-secondary);"
            aria-label="关闭"
          >
            <X class="w-5 h-5" />
          </button>
        </div>

        <!-- 内容 -->
        <div class="p-5">
          <!-- 作者信息 -->
          <div v-if="column" class="flex items-center gap-3 mb-5">
            <img
              :src="getSafeAvatar(column.authorAvatar, String(column.userId))"
              :alt="column.authorName || '作者'"
              class="w-10 h-10 rounded-full object-cover"
              loading="lazy"
            />
            <div class="min-w-0">
              <p class="font-medium truncate" style="color: var(--theme-text);">
                {{ column.authorName || '匿名作者' }}
              </p>
              <p class="text-xs truncate" style="color: var(--theme-text-secondary);">
                {{ column.title }}
              </p>
            </div>
          </div>

          <!-- 快捷金额 -->
          <div class="mb-4">
            <p class="text-sm mb-2" style="color: var(--theme-text-secondary);">选择金额（元）</p>
            <div class="grid grid-cols-3 gap-2">
              <button
                v-for="amt in tipPresetAmounts"
                :key="amt"
                @click="selectTipAmount(amt)"
                class="py-2 rounded-lg text-sm font-medium transition"
                :style="tipAmount === amt
                  ? 'background-color: var(--theme-primary); color: white;'
                  : 'background-color: var(--theme-accent); color: var(--theme-text);'"
              >
                ¥{{ amt }}
              </button>
            </div>
          </div>

          <!-- 自定义金额 -->
          <div class="mb-4">
            <label for="col-tip-amount-input" class="text-sm mb-2 block" style="color: var(--theme-text-secondary);">自定义金额</label>
            <input
              id="col-tip-amount-input"
              v-model.number="tipAmount"
              type="number"
              min="0.01"
              step="0.01"
              placeholder="请输入金额"
              class="w-full px-3 py-2 rounded-lg text-sm focus:outline-none"
              style="background-color: var(--theme-bg); color: var(--theme-text); border: 1px solid var(--theme-border);"
            />
          </div>

          <!-- 留言 -->
          <div class="mb-5">
            <label for="col-tip-message-input" class="text-sm mb-2 block" style="color: var(--theme-text-secondary);">留言（选填）</label>
            <textarea
              id="col-tip-message-input"
              v-model="tipMessage"
              placeholder="说点什么鼓励一下作者..."
              rows="2"
              maxlength="100"
              class="w-full px-3 py-2 rounded-lg text-sm resize-none focus:outline-none"
              style="background-color: var(--theme-bg); color: var(--theme-text); border: 1px solid var(--theme-border);"
            />
          </div>

          <!-- 确认按钮 -->
          <button
            @click="handleTip"
            :disabled="tipping || !tipAmount || tipAmount <= 0"
            class="w-full py-3 rounded-xl font-medium text-sm transition-colors disabled:opacity-50 flex items-center justify-center gap-2"
            style="background-color: var(--theme-primary); color: white;"
          >
            <Loader2 v-if="tipping" class="w-4 h-4 animate-spin" />
            <Gift v-else class="w-4 h-4" />
            {{ tipping ? '处理中...' : `打赏 ¥${Number(tipAmount || 0).toFixed(2)}` }}
          </button>
          <p class="text-xs text-center mt-3" style="color: var(--theme-text-secondary);">
            打赏后不支持退款，请确认金额
          </p>
        </div>
      </div>
    </div>

    <SiteFooter />
  </div>
</template>
