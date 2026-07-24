<script setup lang="ts">
import { ref, onMounted, watch, computed } from 'vue';
import { useRouter } from 'vue-router';
import { useHead } from '@vueuse/head';
import {
  ArrowLeft, Plus, Edit3, Trash2, Eye, Star, Briefcase,
  ChevronLeft, ChevronRight, BookOpen,
} from 'lucide-vue-next';
import SiteFooter from '@/components/SiteFooter.vue';
import { generateSeo } from '@/utils/seo';
import { getMyExperienceList, deleteExperience } from '@/api/interview';
import type { InterviewExperienceVO } from '@/types/api';
import { useToast } from '@/composables/useToast';

const router = useRouter();
const toast = useToast();

const loading = ref(false);
const error = ref<string | null>(null);
const experiences = ref<InterviewExperienceVO[]>([]);
const total = ref(0);
const page = ref(1);
const pageSize = 10;
const deletingId = ref<string | number | null>(null);

const totalPages = computed(() => Math.max(1, Math.ceil(total.value / pageSize)));

// 状态映射
const statusMap: Record<string, { label: string; class: string }> = {
  draft: { label: '草稿', class: 'bg-gray-100 text-gray-600' },
  pending: { label: '待审核', class: 'bg-yellow-100 text-yellow-700' },
  published: { label: '已发布', class: 'bg-green-100 text-green-700' },
  rejected: { label: '已驳回', class: 'bg-red-100 text-red-700' },
};

useHead(computed(() => generateSeo({
  title: '我的面经',
  description: '管理我发布的面试经验，查看审核状态、浏览量与点赞数，持续沉淀面试内容',
  keywords: ['我的面经', '面经管理', '面试经验', '草稿', '审核'],
  canonicalPath: '/interview/my/experiences',
  robots: 'noindex,nofollow',
})));

onMounted(() => {
  loadExperiences();
});

watch(page, () => {
  loadExperiences();
});

async function loadExperiences() {
  try {
    loading.value = true;
    error.value = null;
    const res = await getMyExperienceList({ pageNum: page.value, pageSize });
    if (res.code === 200 && res.data) {
      experiences.value = res.data.list || [];
      total.value = res.data.total || 0;
    } else {
      error.value = res.message || '加载面经失败';
    }
  } catch (err: any) {
    error.value = err?.message || '加载面经失败，请稍后重试';
  } finally {
    loading.value = false;
  }
}

function goBack() {
  router.push('/interview');
}

function gotoPublish() {
  router.push('/interview/experience/publish');
}

function gotoDetail(id: string | number) {
  router.push(`/interview/experience/${id}`);
}

function gotoEdit(id: string | number) {
  router.push(`/interview/experience/edit/${id}`);
}

function statusLabel(exp: InterviewExperienceVO) {
  const s = exp.status || '';
  return statusMap[s]?.label || s || '未知';
}

function statusClass(exp: InterviewExperienceVO) {
  const s = exp.status || '';
  return statusMap[s]?.class || 'bg-gray-100 text-gray-600';
}

function formatNumber(n: number) {
  if (n >= 10000) return (n / 10000).toFixed(1) + 'w';
  if (n >= 1000) return (n / 1000).toFixed(1) + 'k';
  return String(n || 0);
}

function publishTime(exp: InterviewExperienceVO) {
  return exp.createTime || exp.updateTime || '';
}

async function handleDelete(exp: InterviewExperienceVO) {
  if (!window.confirm(`确定删除面经「${exp.title}」吗？删除后不可恢复。`)) return;
  try {
    deletingId.value = exp.id;
    await deleteExperience(exp.id);
    toast.success('删除成功');
    // 删除后若当前页只剩一条且非第一页，回退一页
    if (experiences.value.length === 1 && page.value > 1) {
      page.value -= 1;
    } else {
      loadExperiences();
    }
  } catch (err: any) {
    toast.error(err?.message || '删除失败，请稍后重试');
  } finally {
    deletingId.value = null;
  }
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
      <div class="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 py-3 flex items-center justify-between">
        <button
            @click="goBack"
            class="flex items-center text-sm transition hover:opacity-80"
            style="color: var(--theme-text-secondary);"
        >
          <ArrowLeft class="w-4 h-4 mr-1" />
          返回面试指南
        </button>
        <span class="text-sm font-medium" style="color: var(--theme-text);">我的面经</span>
        <button
            @click="gotoPublish"
            class="flex items-center text-sm text-white px-3 py-1.5 rounded-lg transition hover:opacity-90"
            style="background-color: var(--theme-primary);"
        >
          <Plus class="w-4 h-4 mr-1" />
          发布面经
        </button>
      </div>
    </div>

    <!-- 内容区 -->
    <div class="flex-1 py-8">
      <div class="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8">
        <!-- 加载状态 -->
        <div v-if="loading" class="text-center py-16">
          <div
              class="animate-spin rounded-full h-10 w-10 border-2 mx-auto"
              style="border-color: var(--theme-border); border-top-color: var(--theme-primary);"
          ></div>
          <p class="mt-4 text-sm" style="color: var(--theme-text-secondary);">加载中...</p>
        </div>

        <!-- 错误状态 -->
        <div
            v-else-if="error"
            class="rounded-xl border p-8 text-center"
            style="background-color: var(--theme-surface); border-color: var(--theme-border);"
        >
          <p class="mb-4 text-sm" style="color: var(--theme-text);">{{ error }}</p>
          <button
              @click="loadExperiences"
              class="px-4 py-2 text-white rounded-lg text-sm transition hover:opacity-90"
              style="background-color: var(--theme-primary);"
          >
            重试
          </button>
        </div>

        <!-- 空状态 -->
        <div
            v-else-if="experiences.length === 0"
            class="rounded-xl border p-12 text-center"
            style="background-color: var(--theme-surface); border-color: var(--theme-border);"
        >
          <BookOpen class="w-12 h-12 mx-auto mb-3" style="color: var(--theme-text-secondary); opacity: 0.5;" />
          <p class="text-sm mb-4" style="color: var(--theme-text-secondary);">还没有发布任何面经</p>
          <button
              @click="gotoPublish"
              class="inline-flex items-center px-4 py-2 text-white rounded-lg text-sm transition hover:opacity-90"
              style="background-color: var(--theme-primary);"
          >
            <Plus class="w-4 h-4 mr-1" />
            发布第一篇面经
          </button>
        </div>

        <!-- 面经列表 -->
        <template v-else>
          <div class="space-y-4">
            <div
                v-for="exp in experiences"
                :key="exp.id"
                class="rounded-xl shadow-sm hover:shadow-md transition p-5"
                style="background-color: var(--theme-surface); border: 1px solid var(--theme-border);"
            >
              <!-- 标签行 -->
              <div class="flex items-center flex-wrap gap-2 mb-2">
                <span
                    class="px-2.5 py-1 rounded-full text-xs font-medium"
                    :class="statusClass(exp)"
                >
                  {{ statusLabel(exp) }}
                </span>
                <span
                    v-if="exp.company"
                    class="inline-flex items-center px-2.5 py-1 rounded-full text-xs font-medium"
                    style="background-color: var(--theme-bg); color: var(--theme-primary);"
                >
                  <Briefcase class="w-3 h-3 mr-1" />
                  {{ exp.company }}
                </span>
                <span
                    v-if="exp.position"
                    class="inline-flex items-center px-2.5 py-1 rounded-full text-xs font-medium"
                    style="background-color: var(--theme-bg); color: var(--theme-text-secondary);"
                >
                  {{ exp.position }}
                </span>
                <span
                    v-if="exp.year"
                    class="text-xs"
                    style="color: var(--theme-text-secondary);"
                >
                  {{ exp.year }}年{{ exp.month ? exp.month + '月' : '' }}
                </span>
              </div>

              <!-- 标题 -->
              <h3
                  @click="gotoDetail(exp.id)"
                  class="text-base font-semibold mb-1 cursor-pointer hover:underline"
                  style="color: var(--theme-text);"
              >
                {{ exp.title }}
              </h3>

              <!-- 摘要 -->
              <p
                  v-if="exp.summary || exp.content"
                  class="text-sm line-clamp-2 mb-3"
                  style="color: var(--theme-text-secondary);"
              >
                {{ exp.summary || exp.content }}
              </p>

              <!-- 底部：统计 + 操作 -->
              <div class="flex items-center justify-between pt-3 border-t" style="border-color: var(--theme-border);">
                <div class="flex items-center gap-4 text-xs" style="color: var(--theme-text-secondary);">
                  <span class="flex items-center">
                    <Eye class="w-3 h-3 mr-1" />
                    {{ formatNumber(exp.viewCount) }} 浏览
                  </span>
                  <span class="flex items-center">
                    <Star class="w-3 h-3 mr-1" style="color: var(--theme-primary);" />
                    {{ formatNumber(exp.likeCount) }} 点赞
                  </span>
                  <span v-if="publishTime(exp)" class="flex items-center">
                    {{ publishTime(exp) }}
                  </span>
                </div>
                <div class="flex items-center gap-2">
                  <button
                      @click="gotoEdit(exp.id)"
                      class="inline-flex items-center px-3 py-1.5 rounded-lg text-xs transition hover:opacity-80"
                      style="background-color: var(--theme-bg); color: var(--theme-text); border: 1px solid var(--theme-border);"
                  >
                    <Edit3 class="w-3 h-3 mr-1" />
                    编辑
                  </button>
                  <button
                      @click="handleDelete(exp)"
                      :disabled="deletingId === exp.id"
                      class="inline-flex items-center px-3 py-1.5 rounded-lg text-xs text-white transition hover:opacity-80 disabled:opacity-50 disabled:cursor-not-allowed"
                      style="background-color: #ef4444;"
                  >
                    <Trash2 class="w-3 h-3 mr-1" />
                    删除
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
                :aria-label="`第 ${page - 1} 页`"
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
                :aria-label="`第 ${page + 1} 页`"
                class="px-3 py-2 rounded-lg text-sm transition disabled:opacity-40 disabled:cursor-not-allowed flex items-center"
                style="background-color: var(--theme-surface); border: 1px solid var(--theme-border); color: var(--theme-text);"
            >
              下一页
              <ChevronRight class="w-4 h-4" />
            </button>
            <span class="ml-2 text-xs" style="color: var(--theme-text-secondary);">共 {{ total }} 篇</span>
          </div>
        </template>
      </div>
    </div>

    <SiteFooter />
  </div>
</template>
