<script setup lang="ts">
import { ref, computed, onMounted } from 'vue';
import { formatDate } from '@/utils/date';
import { useRouter } from 'vue-router';
import { useHead } from '@vueuse/head';
import { Flag, Plus, Eye, X } from 'lucide-vue-next';
import SiteFooter from '@/components/SiteFooter.vue';
import Breadcrumb from '@/components/Breadcrumb.vue';
import Pagination from '@/components/Pagination.vue';
import Empty from '@/components/Empty.vue';
import LoadingSpinner from '@/components/LoadingSpinner.vue';
import { useDictData } from '@/composables/useDictData';
import { generateSeo } from '@/utils/seo';
import { useToast } from '@/composables/useToast';
import { useAuth } from '@/composables/useAuth';
import { getMyReports, parseImages, type MyReportRecord, type HandleStatus, type ReportType } from '@/api/report';

const router = useRouter();
const toast = useToast();
const { requireAuth } = useAuth();

useHead(
  generateSeo({
    title: '我的举报',
    description: '查看我提交的举报记录与处理进度。',
    keywords: ['我的举报', '举报进度'],
    type: 'website'
  })
);

const loading = ref(false);
const reportList = ref<MyReportRecord[]>([]);
const total = ref(0);
const pageNum = ref(1);
const pageSize = ref(10);

const statusFilter = ref<HandleStatus | ''>('');
const typeFilter = ref<ReportType | ''>('');

const detailOpen = ref(false);
const detailRecord = ref<MyReportRecord | null>(null);
const previewImage = ref<string | null>(null);

// 处理状态：字典 cms_handle_status 驱动（v13.91，字典此前**有类型无数据**，已同期补齐）
// 颜色仍取本地色板：状态色属展示样式，字典 list_class 映射的是徽章样式类，两者口径不同，不强行合并。
const STATUS_COLOR_FALLBACK: Record<string, string> = {
  pending: '#f59e0b',
  processing: '#3b82f6',
  resolved: '#10b981',
  rejected: '#6b7280',
};
const handlesDict = useDictData(['cms_handle_status', 'cms_report_type']);
const STATUS_LABEL_FALLBACK: Record<string, string> = {
  pending: '待处理',
  processing: '处理中',
  resolved: '已解决',
  rejected: '已驳回',
};
const statusOptions = computed<{ value: HandleStatus; label: string; color: string }[]>(() => {
  const items = handlesDict['cms_handle_status'];
  const pairs = items && items.length > 0
    ? items.map(i => ({ value: i.dictValue as HandleStatus, label: i.dictLabel }))
    : Object.entries(STATUS_LABEL_FALLBACK).map(([value, label]) => ({ value: value as HandleStatus, label }));
  return pairs.map(p => ({ ...p, color: STATUS_COLOR_FALLBACK[p.value] || '#6b7280' }));
});

// 举报类型：字典 cms_report_type 驱动（v13.91），本地兜底与「意见反馈」页同口径
const TYPE_LABEL_FALLBACK: Record<string, string> = {
  spam: '垃圾内容',
  inappropriate: '不当内容',
  infringement: '侵权内容',
  fraud: '欺诈行为',
  other: '其他问题',
};
const typeOptions = computed<{ value: ReportType; label: string }[]>(() => {
  const items = handlesDict['cms_report_type'];
  if (items && items.length > 0) {
    return items.map(i => ({ value: i.dictValue as ReportType, label: i.dictLabel }));
  }
  return Object.entries(TYPE_LABEL_FALLBACK).map(([value, label]) => ({ value: value as ReportType, label }));
});

const totalPages = computed(() => Math.ceil(total.value / pageSize.value) || 1);

function getStatusMeta(status: HandleStatus) {
  // statusOptions 已改为 computed（字典驱动）⇒ 脚本内需 .value
  return statusOptions.value.find(s => s.value === status) || { value: status, label: status, color: '#6b7280' };
}

function getTypeLabel(type: ReportType) {
  return typeOptions.value.find(t => t.value === type)?.label || type;
}

function getImages(record: MyReportRecord): string[] {
  return parseImages(record.images);
}

/**
 * 加载失败提示（清单 P2）。
 *
 * <p>原先失败只在 catch 弹 toast、页面没有 error 状态 —— 失败后 reportList 为空，
 * 模板直接落到 Empty 空态显示「暂无举报记录 / 您还没有提交过举报」，把"加载失败"说成了"确实没有"。</p>
 */
const loadError = ref<string | null>(null);

async function loadList() {
  loading.value = true;
  loadError.value = null;
  try {
    const params: any = { pageNum: pageNum.value, pageSize: pageSize.value };
    if (statusFilter.value) params.status = statusFilter.value;
    if (typeFilter.value) params.reportType = typeFilter.value;
    const res = await getMyReports(params);
    reportList.value = res.data.list;
    total.value = res.data.total;
  } catch (e: any) {
    loadError.value = e?.message || '加载举报列表失败，请稍后重试';
    toast.error(e?.message || '加载举报列表失败');
  } finally {
    loading.value = false;
  }
}

function handleQuery() {
  pageNum.value = 1;
  loadList();
}

function resetQuery() {
  statusFilter.value = '';
  typeFilter.value = '';
  handleQuery();
}

function handlePageChange(page: number) {
  pageNum.value = page;
  loadList();
  window.scrollTo({ top: 0, behavior: 'smooth' });
}

function openDetail(record: MyReportRecord) {
  detailRecord.value = record;
  detailOpen.value = true;
}

function closeDetail() {
  detailOpen.value = false;
  detailRecord.value = null;
}

function goSubmit() {
  router.push('/report');
}

onMounted(() => {
  if (!requireAuth('/my/reports')) return;
  loadList();
});
</script>

<template>
  <div class="min-h-screen flex flex-col" style="background-color: var(--theme-bg);">
    <!-- 面包屑 -->
    <div
      class="border-b sticky top-0 z-30 backdrop-blur-sm py-3"
      style="background-color: var(--theme-surface); border-color: var(--theme-border);"
    >
      <div class="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 flex items-center justify-between gap-4">
        <Breadcrumb :items="[{ label: '个人中心', path: '/user' }, { label: '我的举报' }]" />
      </div>
    </div>

    <div class="flex-1 py-6 sm:py-8">
      <div class="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8">
        <!-- 标题 + 新建 -->
        <div class="flex items-center justify-between mb-6">
          <div class="flex items-center gap-3">
            <div class="w-10 h-10 rounded-xl flex items-center justify-center" style="background-color: #fef2f2;">
              <Flag class="w-5 h-5" style="color: #ef4444;" />
            </div>
            <div>
              <h1 class="text-xl sm:text-2xl font-bold" style="color: var(--theme-text);">我的举报</h1>
              <p class="text-xs mt-0.5" style="color: var(--theme-text-secondary);">查看举报记录与处理进度</p>
            </div>
          </div>
          <button
            @click="goSubmit"
            class="flex items-center gap-1.5 px-4 py-2 rounded-xl text-sm font-medium transition-all hover:opacity-90"
            style="background-color: var(--theme-primary); color: white;"
          >
            <Plus class="w-4 h-4" />
            <span class="hidden sm:inline">提交举报</span>
          </button>
        </div>

        <!-- 筛选 -->
        <div class="flex flex-wrap items-center gap-3 mb-5 p-4 rounded-2xl" style="background-color: var(--theme-surface); border: 1px solid var(--theme-border);">
          <div class="flex items-center gap-2">
            <span class="text-sm" style="color: var(--theme-text-secondary);">状态</span>
            <select
              v-model="statusFilter"
              class="px-3 py-1.5 rounded-lg border text-sm focus:outline-none focus:ring-2 focus:ring-primary"
              style="background-color: var(--theme-bg); border-color: var(--theme-border); color: var(--theme-text);"
            >
              <option value="">全部</option>
              <option v-for="s in statusOptions" :key="s.value" :value="s.value">{{ s.label }}</option>
            </select>
          </div>
          <div class="flex items-center gap-2">
            <span class="text-sm" style="color: var(--theme-text-secondary);">类型</span>
            <select
              v-model="typeFilter"
              class="px-3 py-1.5 rounded-lg border text-sm focus:outline-none focus:ring-2 focus:ring-primary"
              style="background-color: var(--theme-bg); border-color: var(--theme-border); color: var(--theme-text);"
            >
              <option value="">全部</option>
              <option v-for="t in typeOptions" :key="t.value" :value="t.value">{{ t.label }}</option>
            </select>
          </div>
          <button @click="handleQuery" class="px-4 py-1.5 rounded-lg text-sm font-medium" style="background-color: var(--theme-primary); color: white;">筛选</button>
          <button @click="resetQuery" class="px-4 py-1.5 rounded-lg text-sm" style="background-color: var(--theme-bg); color: var(--theme-text-secondary); border: 1px solid var(--theme-border);">重置</button>
        </div>

        <!-- 列表 -->
        <LoadingSpinner v-if="loading" />

        <!-- 失败态（清单 P2）：必须排在空态之前，否则"加载失败"会被说成"暂无记录" -->
        <div
          v-else-if="loadError"
          class="text-center py-12 rounded-2xl"
          style="background-color: var(--theme-surface); border: 1px solid var(--theme-border);"
        >
          <p class="text-sm mb-4" style="color: var(--theme-text);">{{ loadError }}</p>
          <button
            class="px-4 py-2 rounded-xl text-sm font-medium"
            style="background-color: var(--theme-primary); color: white;"
            @click="loadList()"
          >重试</button>
        </div>

        <Empty v-else-if="reportList.length === 0" title="暂无举报记录" description="您还没有提交过举报">
          <template #action>
            <button @click="goSubmit" class="px-5 py-2 rounded-xl text-sm font-medium" style="background-color: var(--theme-primary); color: white;">去提交举报</button>
          </template>
        </Empty>
        <div v-else class="space-y-3">
          <div
            v-for="item in reportList"
            :key="item.id"
            class="p-4 sm:p-5 rounded-2xl cursor-pointer transition-all hover:shadow-md"
            style="background-color: var(--theme-surface); border: 1px solid var(--theme-border);"
            @click="openDetail(item)"
          >
            <div class="flex items-start justify-between gap-3 mb-2">
              <div class="flex items-center gap-2 flex-wrap">
                <span class="text-xs px-2 py-0.5 rounded-md font-medium" style="background-color: var(--theme-accent); color: var(--theme-text-secondary);">{{ getTypeLabel(item.reportType) }}</span>
                <span class="text-xs font-mono" style="color: var(--theme-text-secondary);">#{{ item.id }}</span>
              </div>
              <span
                class="text-xs px-2.5 py-1 rounded-full font-medium flex-shrink-0"
                :style="{ backgroundColor: getStatusMeta(item.status).color + '20', color: getStatusMeta(item.status).color }"
              >{{ getStatusMeta(item.status).label }}</span>
            </div>
            <p class="text-sm line-clamp-2 mb-2" style="color: var(--theme-text);">{{ item.description }}</p>
            <div class="flex items-center justify-between text-xs" style="color: var(--theme-text-secondary);">
              <span>{{ formatDate(item.createTime, 'YYYY-MM-DD HH:mm', 'YYYY-MM-DD') }}</span>
              <span v-if="item.status !== 'pending'" class="flex items-center gap-1">
                <Eye class="w-3 h-3" /> 查看进度
              </span>
            </div>
          </div>
        </div>

        <!-- 分页 -->
        <div v-if="total > pageSize" class="mt-6">
          <Pagination
            :current-page="pageNum"
            :total-pages="totalPages"
            :total-items="total"
            :items-per-page="pageSize"
            @page-change="handlePageChange"
          />
        </div>
      </div>
    </div>

    <!-- 详情弹窗 -->
    <div v-if="detailOpen && detailRecord" class="fixed inset-0 z-50 flex items-center justify-center p-4" @click.self="closeDetail">
      <div class="absolute inset-0 bg-black/50"></div>
      <div class="relative w-full max-w-lg rounded-2xl shadow-2xl max-h-[90vh] overflow-y-auto" style="background-color: var(--theme-surface);">
        <div class="flex items-center justify-between p-5 border-b sticky top-0" style="border-color: var(--theme-border); background-color: var(--theme-surface);">
          <h3 class="text-lg font-bold" style="color: var(--theme-text);">举报详情</h3>
          <button @click="closeDetail" class="p-1 rounded-lg hover:opacity-70" style="color: var(--theme-text-secondary);">
            <X class="w-5 h-5" />
          </button>
        </div>
        <div class="p-5 space-y-4">
          <div class="flex items-center gap-2 flex-wrap">
            <span class="text-xs px-2 py-0.5 rounded-md font-medium" style="background-color: var(--theme-accent); color: var(--theme-text-secondary);">{{ getTypeLabel(detailRecord.reportType) }}</span>
            <span class="text-xs font-mono" style="color: var(--theme-text-secondary);">#{{ detailRecord.id }}</span>
            <span
              class="text-xs px-2.5 py-0.5 rounded-full font-medium"
              :style="{ backgroundColor: getStatusMeta(detailRecord.status).color + '20', color: getStatusMeta(detailRecord.status).color }"
            >{{ getStatusMeta(detailRecord.status).label }}</span>
          </div>

          <div v-if="detailRecord.targetUrl">
            <p class="text-xs mb-1" style="color: var(--theme-text-secondary);">目标链接</p>
            <a :href="detailRecord.targetUrl" target="_blank" rel="noopener noreferrer" class="text-sm break-all" style="color: var(--theme-primary);">{{ detailRecord.targetUrl }}</a>
          </div>

          <div>
            <p class="text-xs mb-1" style="color: var(--theme-text-secondary);">问题描述</p>
            <p class="text-sm leading-relaxed whitespace-pre-wrap" style="color: var(--theme-text);">{{ detailRecord.description }}</p>
          </div>

          <div v-if="getImages(detailRecord).length > 0">
            <p class="text-xs mb-2" style="color: var(--theme-text-secondary);">图片证据</p>
            <div class="grid grid-cols-3 gap-2">
              <img
                v-for="(img, idx) in getImages(detailRecord)"
                :key="idx"
                :src="img"
                :alt="`证据图${idx + 1}`"
                class="w-full aspect-square object-cover rounded-lg cursor-pointer hover:opacity-80 transition-opacity"
                @click="previewImage = img"
              />
            </div>
          </div>

          <div v-if="detailRecord.contact">
            <p class="text-xs mb-1" style="color: var(--theme-text-secondary);">联系方式</p>
            <p class="text-sm" style="color: var(--theme-text);">{{ detailRecord.contact }}</p>
          </div>

          <!-- 处理进度 -->
          <div class="pt-4 border-t" style="border-color: var(--theme-border);">
            <p class="text-xs mb-3 font-medium" style="color: var(--theme-text);">处理进度</p>
            <div class="space-y-2 text-sm">
              <div class="flex justify-between">
                <span style="color: var(--theme-text-secondary);">提交时间</span>
                <span style="color: var(--theme-text);">{{ formatDate(detailRecord.createTime, 'YYYY-MM-DD HH:mm', 'YYYY-MM-DD') }}</span>
              </div>
              <div v-if="detailRecord.handler" class="flex justify-between">
                <span style="color: var(--theme-text-secondary);">处理人</span>
                <span style="color: var(--theme-text);">{{ detailRecord.handler }}</span>
              </div>
              <div v-if="detailRecord.handleTime" class="flex justify-between">
                <span style="color: var(--theme-text-secondary);">处理时间</span>
                <span style="color: var(--theme-text);">{{ formatDate(detailRecord.handleTime, 'YYYY-MM-DD HH:mm', 'YYYY-MM-DD') }}</span>
              </div>
              <div v-if="detailRecord.handleResult">
                <p class="mb-1" style="color: var(--theme-text-secondary);">处理结果</p>
                <p class="text-sm p-3 rounded-lg" style="background-color: var(--theme-bg); color: var(--theme-text);">{{ detailRecord.handleResult }}</p>
              </div>
            </div>
          </div>
        </div>
      </div>
    </div>

    <!-- 图片预览 -->
    <div v-if="previewImage" class="fixed inset-0 z-[60] flex items-center justify-center p-4" @click="previewImage = null">
      <div class="absolute inset-0 bg-black/80"></div>
      <img :src="previewImage" class="relative max-w-full max-h-full rounded-lg" alt="预览" />
      <button class="absolute top-4 right-4 p-2 rounded-full bg-black/50 text-white" @click="previewImage = null">
        <X class="w-6 h-6" />
      </button>
    </div>

    <SiteFooter />
  </div>
</template>
