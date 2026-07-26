<script setup lang="ts">
import { ref, onMounted, watch, computed } from 'vue';
import { useRouter } from 'vue-router';
import { useHead } from '@vueuse/head';
import {
  FileText, Plus, Pencil, Trash2, Copy, Download, Star, Clock, CheckCircle,
  Send, Archive, History, X,
} from 'lucide-vue-next';
import SiteFooter from '@/components/SiteFooter.vue';
import Breadcrumb from '@/components/Breadcrumb.vue';
import { generateSeo } from '@/utils/seo';
import {
  getMyResumeList, deleteResume, copyResume, exportResumePdf, scoreResume,
  updateResumeStatus, getResumeVersions,
} from '@/api/interview';
import { getToken } from '@/api/client';
import type { UserResumeVO } from '@/types/api';
import { useToast } from '@/composables/useToast';

const router = useRouter();
const toast = useToast();

const loading = ref(false);
const error = ref<string | null>(null);
const resumes = ref<UserResumeVO[]>([]);
const total = ref(0);
const page = ref(1);
const pageSize = 12;
const actionId = ref<string | number | null>(null);

// 版本历史模态框
const versionModal = ref<{ open: boolean; loading: boolean; list: UserResumeVO[] }>({
  open: false, loading: false, list: [],
});

const totalPages = computed(() => Math.max(1, Math.ceil(total.value / pageSize)));

// 状态映射：草稿=info色，已发布=success色，已归档=warning色
const statusMap: Record<string, { label: string; class: string }> = {
  draft: { label: '草稿', class: 'bg-blue-100 text-blue-700' },
  published: { label: '已发布', class: 'bg-green-100 text-green-700' },
  archived: { label: '已归档', class: 'bg-yellow-100 text-yellow-700' },
};

useHead(computed(() => generateSeo({
  title: '我的简历',
  description: '管理我的简历，支持创建、编辑、复制版本、导出 PDF 与 AI 评分',
  keywords: ['我的简历', '简历管理', '导出PDF', '简历评分', '墨韵'],
  canonicalPath: '/interview/my/resumes',
  robots: 'noindex,nofollow',
})));

// 面包屑
const breadcrumbs = computed(() => [
  { label: '个人空间', path: '/user' },
  { label: '我的简历' },
]);

onMounted(() => {
  loadResumes();
});

watch(page, () => {
  loadResumes();
});

async function loadResumes() {
  try {
    loading.value = true;
    error.value = null;
    const res = await getMyResumeList({ pageNum: page.value, pageSize });
    if (res.code === 200 && res.data) {
      resumes.value = res.data.list || [];
      total.value = res.data.total || 0;
    } else {
      error.value = res.message || '加载简历失败';
    }
  } catch (err: any) {
    error.value = err?.message || '加载简历失败，请稍后重试';
  } finally {
    loading.value = false;
  }
}

function gotoCreate() {
  router.push('/interview/resume/edit');
}

function gotoEdit(id: string | number | undefined) {
  if (id === undefined || id === null) {
    toast.error('简历数据异常，无法编辑');
    return;
  }
  router.push(`/interview/resume/edit/${id}`);
}

function statusLabel(r: UserResumeVO) {
  const s = r.status || '';
  return statusMap[s]?.label || s || '草稿';
}

function statusClass(r: UserResumeVO) {
  const s = r.status || '';
  return statusMap[s]?.class || 'bg-gray-100 text-gray-600';
}

function formatTime(t?: string) {
  if (!t) return '-';
  return t.slice(0, 16).replace('T', ' ');
}

async function handleCopy(r: UserResumeVO) {
  if (!r.id || actionId.value) return;
  try {
    actionId.value = r.id;
    await copyResume(r.id);
    toast.success('已复制为新版本');
    page.value = 1;
    await loadResumes();
  } catch (err: any) {
    toast.error(err?.message || '复制失败，请稍后重试');
  } finally {
    actionId.value = null;
  }
}

// 认证下载 PDF（fetch blob + a 标签，避免 window.open 无法携带 token）
async function downloadPdfAuth(url: string, id: string | number) {
  const token = getToken();
  const baseUrl = import.meta.env.VITE_API_BASE_URL || '/api';
  const resp = await fetch(baseUrl + url, {
    headers: token ? { Authorization: `Bearer ${token}` } : {},
  });
  if (!resp.ok) {
    const txt = await resp.text().catch(() => '');
    throw new Error(txt || `下载失败 (${resp.status})`);
  }
  const blob = await resp.blob();
  const a = document.createElement('a');
  a.href = URL.createObjectURL(blob);
  a.download = `resume_${id}.pdf`;
  document.body.appendChild(a);
  a.click();
  document.body.removeChild(a);
  URL.revokeObjectURL(a.href);
}

async function handleExportPdf(r: UserResumeVO) {
  if (!r.id || actionId.value) return;
  try {
    actionId.value = r.id;
    const res = await exportResumePdf(r.id);
    if (res.code === 200 && res.data?.fileUrl) {
      // fileUrl 为认证下载端点，需带 token 下载
      await downloadPdfAuth(res.data.fileUrl, r.id);
      toast.success('PDF 导出成功');
      // 更新列表项的导出信息
      const idx = resumes.value.findIndex(item => item.id === r.id);
      if (idx !== -1) {
        resumes.value[idx] = {
          ...resumes.value[idx],
          fileUrl: res.data.fileUrl,
          exportTime: res.data.exportTime,
        };
      }
    } else {
      toast.error(res.message || '导出失败，请稍后重试');
    }
  } catch (err: any) {
    toast.error(err?.message || '导出失败，请稍后重试');
  } finally {
    actionId.value = null;
  }
}

async function handleScore(r: UserResumeVO) {
  if (!r.id || actionId.value) return;
  try {
    actionId.value = r.id;
    const res = await scoreResume(r.id);
    if (res.code === 200 && res.data) {
      const score = res.data.score;
      toast.success(`评分完成：${score} 分`);
      // 更新当前列表项的评分显示
      const idx = resumes.value.findIndex(item => item.id === r.id);
      if (idx !== -1) {
        resumes.value[idx] = {
          ...resumes.value[idx],
          score,
          scoreDetail: res.data.scoreDetail,
          scoredTime: res.data.scoredTime,
        };
      }
    } else {
      toast.error(res.message || '评分失败，请稍后重试');
    }
  } catch (err: any) {
    toast.error(err?.message || '评分失败，请稍后重试');
  } finally {
    actionId.value = null;
  }
}

// 发布 / 归档 / 恢复（统一走 updateResumeStatus）
async function handleToggleStatus(r: UserResumeVO, target: 'published' | 'archived' | 'draft') {
  if (!r.id || actionId.value) return;
  const actionText = target === 'published' ? '发布' : (target === 'archived' ? '归档' : '恢复为草稿');
  if (!window.confirm(`确定${actionText}简历「${r.title || r.name || ''}」吗？`)) return;
  try {
    actionId.value = r.id;
    await updateResumeStatus(r.id, target);
    toast.success(`${actionText}成功`);
    // 更新列表项状态
    const idx = resumes.value.findIndex(item => item.id === r.id);
    if (idx !== -1) {
      resumes.value[idx] = { ...resumes.value[idx], status: target };
    }
  } catch (err: any) {
    toast.error(err?.message || `${actionText}失败，请稍后重试`);
  } finally {
    actionId.value = null;
  }
}

// 版本历史
async function handleShowVersions(r: UserResumeVO) {
  if (!r.id || actionId.value) return;
  versionModal.value = { open: true, loading: true, list: [] };
  try {
    actionId.value = r.id;
    const res = await getResumeVersions(r.id);
    if (res.code === 200 && res.data) {
      versionModal.value.list = res.data;
    } else {
      toast.error(res.message || '加载版本历史失败');
    }
  } catch (err: any) {
    toast.error(err?.message || '加载版本历史失败');
  } finally {
    versionModal.value.loading = false;
    actionId.value = null;
  }
}

function closeVersionModal() {
  versionModal.value.open = false;
}

async function handleDelete(r: UserResumeVO) {
  if (!r.id || actionId.value) return;
  if (!window.confirm(`确定删除简历「${r.title || r.name || ''}」吗？删除后不可恢复，且会同时删除其所有历史版本。`)) return;
  try {
    actionId.value = r.id;
    await deleteResume(r.id);
    toast.success('删除成功');
    if (resumes.value.length === 1 && page.value > 1) {
      page.value -= 1;
    } else {
      loadResumes();
    }
  } catch (err: any) {
    toast.error(err?.message || '删除失败，请稍后重试');
  } finally {
    actionId.value = null;
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
    <!-- 吸顶面包屑栏 -->
    <div
      class="border-b sticky top-0 z-30 backdrop-blur-sm py-3"
      style="background-color: var(--theme-surface); border-color: var(--theme-border);"
    >
      <div class="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 flex items-center justify-between gap-4">
        <Breadcrumb :items="breadcrumbs" />
        <button
          @click="gotoCreate"
          class="flex items-center text-sm text-white px-3 py-1.5 rounded-lg transition hover:opacity-90 flex-shrink-0"
          style="background-color: var(--theme-primary);"
        >
          <Plus class="w-4 h-4 mr-1" />
          创建简历
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
            @click="loadResumes"
            class="px-4 py-2 text-white rounded-lg text-sm transition hover:opacity-90"
            style="background-color: var(--theme-primary);"
          >
            重试
          </button>
        </div>

        <!-- 空状态 -->
        <div
          v-else-if="resumes.length === 0"
          class="rounded-xl border p-12 text-center"
          style="background-color: var(--theme-surface); border-color: var(--theme-border);"
        >
          <FileText class="w-12 h-12 mx-auto mb-3" style="color: var(--theme-text-secondary); opacity: 0.5;" />
          <p class="text-sm mb-4" style="color: var(--theme-text-secondary);">还没有简历，立即创建</p>
          <button
            @click="gotoCreate"
            class="inline-flex items-center px-4 py-2 text-white rounded-lg text-sm transition hover:opacity-90"
            style="background-color: var(--theme-primary);"
          >
            <Plus class="w-4 h-4 mr-1" />
            创建第一份简历
          </button>
        </div>

        <!-- 简历列表 -->
        <template v-else>
          <div class="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-5">
            <div
              v-for="r in resumes"
              :key="r.id"
              class="rounded-xl shadow-sm hover:shadow-md transition flex flex-col p-5"
              style="background-color: var(--theme-surface); border: 1px solid var(--theme-border);"
            >
              <!-- 头部：标题 + 状态 -->
              <div class="flex items-start justify-between gap-2 mb-2">
                <h3
                  @click="gotoEdit(r.id)"
                  class="text-base font-semibold leading-snug cursor-pointer hover:underline line-clamp-2"
                  style="color: var(--theme-text);"
                >
                  {{ r.title || '未命名简历' }}
                </h3>
                <span
                  class="shrink-0 px-2.5 py-1 rounded-full text-xs font-medium"
                  :class="statusClass(r)"
                >
                  {{ statusLabel(r) }}
                </span>
              </div>

              <!-- 姓名 -->
              <div class="flex items-center text-sm mb-2" style="color: var(--theme-text-secondary);">
                <FileText class="w-3.5 h-3.5 mr-1.5" />
                <span v-if="r.name">姓名：{{ r.name }}</span>
                <span v-else class="opacity-60">未填写姓名</span>
              </div>

              <!-- 评分 -->
              <div
                v-if="r.score != null"
                class="flex items-center text-sm mb-2"
                style="color: var(--theme-primary);"
              >
                <Star class="w-3.5 h-3.5 mr-1.5" />
                评分 {{ r.score }} 分
              </div>

              <!-- 求职意向摘要 -->
              <p
                v-if="r.jobIntention?.position"
                class="text-sm line-clamp-1 mb-3"
                style="color: var(--theme-text-secondary);"
              >
                意向：{{ r.jobIntention.position }}
                <span v-if="r.jobIntention.city">· {{ r.jobIntention.city }}</span>
              </p>
              <div v-else class="mb-3"></div>

              <!-- 底部信息 -->
              <div class="flex flex-wrap items-center gap-x-3 gap-y-1 text-xs pt-3 border-t mb-3" style="border-color: var(--theme-border); color: var(--theme-text-secondary);">
                <span class="flex items-center">
                  <Clock class="w-3 h-3 mr-1" />
                  {{ formatTime(r.updateTime || r.createTime) }}
                </span>
                <span class="flex items-center">
                  <CheckCircle class="w-3 h-3 mr-1" />
                  版本 v{{ r.versionNo || 1 }}
                </span>
              </div>

              <!-- 操作 -->
              <div class="flex flex-wrap items-center gap-1.5 mt-auto">
                <button
                  @click="gotoEdit(r.id)"
                  class="inline-flex items-center px-2.5 py-1.5 rounded-lg text-xs transition hover:opacity-80"
                  style="background-color: var(--theme-bg); color: var(--theme-text); border: 1px solid var(--theme-border);"
                >
                  <Pencil class="w-3 h-3 mr-1" />编辑
                </button>
                <button
                  @click="handleScore(r)"
                  :disabled="actionId === r.id"
                  class="inline-flex items-center px-2.5 py-1.5 rounded-lg text-xs transition hover:opacity-80 disabled:opacity-50 disabled:cursor-not-allowed"
                  style="background-color: var(--theme-bg); color: var(--theme-primary); border: 1px solid var(--theme-border);"
                >
                  <Star class="w-3 h-3 mr-1" />评分
                </button>
                <button
                  @click="handleExportPdf(r)"
                  :disabled="actionId === r.id"
                  class="inline-flex items-center px-2.5 py-1.5 rounded-lg text-xs transition hover:opacity-80 disabled:opacity-50 disabled:cursor-not-allowed"
                  style="background-color: var(--theme-bg); color: var(--theme-text); border: 1px solid var(--theme-border);"
                >
                  <Download class="w-3 h-3 mr-1" />导出
                </button>
                <button
                  @click="handleCopy(r)"
                  :disabled="actionId === r.id"
                  class="inline-flex items-center px-2.5 py-1.5 rounded-lg text-xs transition hover:opacity-80 disabled:opacity-50 disabled:cursor-not-allowed"
                  style="background-color: var(--theme-bg); color: var(--theme-text); border: 1px solid var(--theme-border);"
                >
                  <Copy class="w-3 h-3 mr-1" />复制
                </button>
                <button
                  @click="handleShowVersions(r)"
                  :disabled="actionId === r.id"
                  class="inline-flex items-center px-2.5 py-1.5 rounded-lg text-xs transition hover:opacity-80 disabled:opacity-50 disabled:cursor-not-allowed"
                  style="background-color: var(--theme-bg); color: var(--theme-text); border: 1px solid var(--theme-border);"
                >
                  <History class="w-3 h-3 mr-1" />版本
                </button>
                <!-- 状态切换按钮：根据当前状态显示可用操作 -->
                <button
                  v-if="r.status === 'draft'"
                  @click="handleToggleStatus(r, 'published')"
                  :disabled="actionId === r.id"
                  class="inline-flex items-center px-2.5 py-1.5 rounded-lg text-xs text-white transition hover:opacity-80 disabled:opacity-50 disabled:cursor-not-allowed"
                  style="background-color: #16a34a;"
                >
                  <Send class="w-3 h-3 mr-1" />发布
                </button>
                <button
                  v-if="r.status === 'published'"
                  @click="handleToggleStatus(r, 'archived')"
                  :disabled="actionId === r.id"
                  class="inline-flex items-center px-2.5 py-1.5 rounded-lg text-xs text-white transition hover:opacity-80 disabled:opacity-50 disabled:cursor-not-allowed"
                  style="background-color: #d97706;"
                >
                  <Archive class="w-3 h-3 mr-1" />归档
                </button>
                <button
                  v-if="r.status === 'archived'"
                  @click="handleToggleStatus(r, 'draft')"
                  :disabled="actionId === r.id"
                  class="inline-flex items-center px-2.5 py-1.5 rounded-lg text-xs text-white transition hover:opacity-80 disabled:opacity-50 disabled:cursor-not-allowed"
                  style="background-color: #2563eb;"
                >
                  <Archive class="w-3 h-3 mr-1" />恢复
                </button>
                <button
                  @click="handleDelete(r)"
                  :disabled="actionId === r.id"
                  class="inline-flex items-center px-2.5 py-1.5 rounded-lg text-xs text-white transition hover:opacity-80 disabled:opacity-50 disabled:cursor-not-allowed"
                  style="background-color: #ef4444;"
                >
                  <Trash2 class="w-3 h-3 mr-1" />删除
                </button>
              </div>
            </div>
          </div>

          <!-- 分页 -->
          <div v-if="totalPages > 1" class="flex flex-wrap items-center justify-center gap-2 mt-8">
            <button
              @click="gotoPage(page - 1)"
              :disabled="page === 1"
              class="px-3 py-2 rounded-lg text-sm transition disabled:opacity-40 disabled:cursor-not-allowed"
              style="background-color: var(--theme-surface); border: 1px solid var(--theme-border); color: var(--theme-text);"
            >
              上一页
            </button>
            <span class="px-4 py-2 text-sm" style="color: var(--theme-text-secondary);">
              第 {{ page }} / {{ totalPages }} 页
            </span>
            <button
              @click="gotoPage(page + 1)"
              :disabled="page === totalPages"
              class="px-3 py-2 rounded-lg text-sm transition disabled:opacity-40 disabled:cursor-not-allowed"
              style="background-color: var(--theme-surface); border: 1px solid var(--theme-border); color: var(--theme-text);"
            >
              下一页
            </button>
            <span class="ml-2 text-xs" style="color: var(--theme-text-secondary);">共 {{ total }} 份</span>
          </div>
        </template>
      </div>
    </div>

    <!-- 版本历史模态框 -->
    <div
      v-if="versionModal.open"
      class="fixed inset-0 z-50 flex items-center justify-center p-4"
      style="background-color: rgba(0,0,0,0.5);"
      @click.self="closeVersionModal"
    >
      <div
        class="rounded-xl shadow-xl w-full max-w-2xl max-h-[80vh] flex flex-col"
        style="background-color: var(--theme-surface); border: 1px solid var(--theme-border);"
      >
        <!-- 头部 -->
        <div class="flex items-center justify-between p-4 border-b" style="border-color: var(--theme-border);">
          <h3 class="text-base font-semibold flex items-center" style="color: var(--theme-text);">
            <History class="w-4 h-4 mr-2" />版本历史
          </h3>
          <button
            @click="closeVersionModal"
            class="transition hover:opacity-70"
            style="color: var(--theme-text-secondary);"
          >
            <X class="w-5 h-5" />
          </button>
        </div>
        <!-- 内容 -->
        <div class="flex-1 overflow-y-auto p-4">
          <div v-if="versionModal.loading" class="text-center py-8">
            <div class="animate-spin rounded-full h-8 w-8 border-2 mx-auto" style="border-color: var(--theme-border); border-top-color: var(--theme-primary);"></div>
            <p class="mt-3 text-sm" style="color: var(--theme-text-secondary);">加载中...</p>
          </div>
          <div v-else-if="versionModal.list.length === 0" class="text-center py-8">
            <p class="text-sm" style="color: var(--theme-text-secondary);">暂无历史版本</p>
          </div>
          <ul v-else class="space-y-2">
            <li
              v-for="v in versionModal.list"
              :key="v.id"
              class="rounded-lg p-3 flex items-center justify-between gap-3"
              style="background-color: var(--theme-bg); border: 1px solid var(--theme-border);"
            >
              <div class="flex-1 min-w-0">
                <div class="flex items-center gap-2 mb-1">
                  <span class="text-sm font-medium" style="color: var(--theme-text);">
                    v{{ v.versionNo || 1 }}
                  </span>
                  <span
                    class="px-2 py-0.5 rounded-full text-xs font-medium"
                    :class="statusClass(v)"
                  >
                    {{ statusLabel(v) }}
                  </span>
                </div>
                <p class="text-sm line-clamp-1" style="color: var(--theme-text-secondary);">
                  {{ v.title || '未命名简历' }}
                </p>
                <p class="text-xs mt-0.5" style="color: var(--theme-text-secondary);">
                  更新：{{ formatTime(v.updateTime || v.createTime) }}
                </p>
              </div>
              <button
                @click="gotoEdit(v.id); closeVersionModal()"
                class="shrink-0 inline-flex items-center px-2.5 py-1.5 rounded-lg text-xs transition hover:opacity-80"
                style="background-color: var(--theme-surface); color: var(--theme-primary); border: 1px solid var(--theme-border);"
              >
                <Pencil class="w-3 h-3 mr-1" />编辑
              </button>
            </li>
          </ul>
        </div>
      </div>
    </div>

    <SiteFooter />
  </div>
</template>
