<script setup lang="ts">
import { ref, computed, onMounted, watch } from 'vue';
import { formatDate } from '@/utils/date';
import { useRouter } from 'vue-router';
import { useHead } from '@vueuse/head';
import {
  Trophy, ChevronLeft, ChevronRight, Calendar, Gift, Loader2,
} from 'lucide-vue-next';
import Breadcrumb from '@/components/Breadcrumb.vue';
import SiteFooter from '@/components/SiteFooter.vue';
import LazyImage from '@/components/LazyImage.vue';
import { generateSeo } from '@/utils/seo';
import { useDictData } from '@/composables/useDictData';
import { getContestList } from '@/api/contest';
import type { WritingContestVO, ContestListQuery } from '@/api/contest';

const router = useRouter();

const loading = ref(false);
const error = ref<string | null>(null);
const contests = ref<WritingContestVO[]>([]);
const total = ref(0);
const page = ref(1);
const pageSize = 12;

const statusFilter = ref<string>('');

// 征文状态：字典 cms_contest_status 驱动（v14.02；该字典此前"有类型无数据"，已由
// 20261001-04 补齐）。同一份枚举原先在本页与 ContestDetailPage 各写死一遍，后台却用字典 —— 三处口径不一致。
//
// 颜色仍取本地**语义色板**：色值属展示样式，字典 list_class 映射的是徽章类名，两者口径不同，不强行合并。
const CONTEST_STATUS_COLORS: Record<string, string> = {
  draft: '#9ca3af',
  collecting: '#16a34a',
  voting: '#d97706',
  ended: '#6b7280',
};
const CONTEST_STATUS_LABELS: Record<string, string> = {
  draft: '草稿',
  collecting: '征稿中',
  voting: '投票中',
  ended: '已结束',
};
const contestDict = useDictData(['cms_contest_status']);
const statusOptions = computed<{ value: string; label: string; color: string }[]>(() => {
  const items = contestDict['cms_contest_status'];
  const rest = items && items.length > 0
    ? items.map(i => ({ value: i.dictValue, label: i.dictLabel }))
    : Object.entries(CONTEST_STATUS_LABELS).map(([value, label]) => ({ value, label }));
  // 列表接口不返回 draft（草稿不对外），故筛选项里也不放 draft
  return [
    { value: '', label: '全部', color: 'var(--theme-text-secondary)' },
    ...rest
      .filter(o => o.value !== 'draft')
      .map(o => ({ ...o, color: CONTEST_STATUS_COLORS[o.value] || 'var(--theme-text-secondary)' })),
  ];
});

const totalPages = computed(() => Math.max(1, Math.ceil(total.value / pageSize)));

const breadcrumbs = computed(() => [{ label: '创作挑战' }]);

useHead(computed(() => generateSeo({
  title: '创作挑战',
  description: '旭林创作挑战广场，参加主题征文活动，赢取奖品，结识同好',
  keywords: ['创作挑战', '征文', '写作比赛', '旭林'],
  canonicalPath: '/contests',
})));

onMounted(() => {
  loadContests();
});

watch(page, () => {
  loadContests();
});

async function loadContests() {
  loading.value = true;
  error.value = null;
  try {
    const params: ContestListQuery = { pageNum: page.value, pageSize };
    if (statusFilter.value) params.status = statusFilter.value;
    const res = await getContestList(params);
    if (res.code === 200 && res.data) {
      contests.value = res.data.list || [];
      total.value = res.data.total || 0;
    } else {
      error.value = res.message || '加载活动失败';
    }
  } catch (err) {
    const e = err as { message?: string };
    error.value = e?.message || '加载活动失败，请稍后重试';
  } finally {
    loading.value = false;
  }
}

function changeStatus(s: string) {
  if (statusFilter.value === s) return;
  statusFilter.value = s;
  if (page.value !== 1) {
    page.value = 1;
  } else {
    loadContests();
  }
}

function gotoDetail(id: string | number) {
  router.push(`/contest/${id}`);
}

function gotoPage(p: number) {
  if (p < 1 || p > totalPages.value) return;
  page.value = p;
  window.scrollTo({ top: 0, behavior: 'smooth' });
}

/**
 * 状态徽章元数据。
 *
 * <p>原实现找不到匹配时 `return statusOptions[0]`（即「全部」）⇒ 一旦出现未登记状态
 * （如 draft 或将来新增状态），卡片会被标成"全部"，语义完全错乱（清单 P2 #34）。
 * 现改为：命中则用字典/兜底文案，未命中则**原样显示状态码**并给中性色，便于发现漏配。</p>
 */
function statusMeta(status?: string) {
  const opt = statusOptions.value.find(o => o.value === status);
  if (opt) return opt;
  return { value: status || '', label: status || '未知', color: 'var(--theme-text-secondary)' };
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

    <!-- 筛选条 -->
    <div class="border-b" style="background-color: var(--theme-surface); border-color: var(--theme-border);">
      <div class="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 py-3 flex items-center justify-between flex-wrap gap-3">
        <div class="flex items-center gap-2 flex-wrap">
          <span class="text-xs" style="color: var(--theme-text-secondary);">状态：</span>
          <button
            v-for="opt in statusOptions"
            :key="opt.value"
            @click="changeStatus(opt.value)"
            class="px-3 py-1.5 rounded-lg text-xs font-medium transition"
            :style="{
              backgroundColor: statusFilter === opt.value ? 'var(--theme-primary)' : 'var(--theme-bg)',
              color: statusFilter === opt.value ? '#fff' : 'var(--theme-text-secondary)',
              border: '1px solid var(--theme-border)',
            }"
          >
            {{ opt.label }}
          </button>
        </div>
        <div class="text-xs" style="color: var(--theme-text-secondary);">
          共 {{ total }} 个活动
        </div>
      </div>
    </div>

    <!-- 内容区 -->
    <div class="flex-1 py-8">
      <div class="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8">
        <!-- 加载状态 -->
        <div v-if="loading" class="flex flex-col items-center justify-center py-20">
          <Loader2 class="w-10 h-10 animate-spin" style="color: var(--theme-primary);" />
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
            @click="loadContests"
            class="px-4 py-2 text-white rounded-lg text-sm transition hover:opacity-90"
            style="background-color: var(--theme-primary);"
          >
            重试
          </button>
        </div>

        <!-- 空状态 -->
        <div
          v-else-if="contests.length === 0"
          class="rounded-xl border p-12 text-center"
          style="background-color: var(--theme-surface); border-color: var(--theme-border);"
        >
          <Trophy class="w-12 h-12 mx-auto mb-3" style="color: var(--theme-text-secondary); opacity: 0.5;" />
          <p class="text-sm" style="color: var(--theme-text-secondary);">暂无活动</p>
        </div>

        <!-- 活动卡片网格 -->
        <template v-else>
          <div class="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-5 mb-8">
            <div
              v-for="c in contests"
              :key="c.id"
              @click="gotoDetail(c.id)"
              class="rounded-xl overflow-hidden border shadow-sm hover:shadow-lg hover:-translate-y-1 transition cursor-pointer flex flex-col"
              style="background-color: var(--theme-surface); border-color: var(--theme-border);"
            >
              <!-- 封面图 -->
              <div class="relative h-40" style="background-color: var(--theme-bg);">
                <LazyImage
                  v-if="c.cover"
                  :src="c.cover"
                  :alt="c.title"
                  class="w-full h-full object-cover"
                />
                <div v-else class="w-full h-full flex items-center justify-center" style="background: linear-gradient(135deg, var(--theme-accent), color-mix(in srgb, var(--theme-accent) 50%, #c4b5fd));">
                  <Trophy class="w-10 h-10" style="color: var(--theme-primary); opacity: 0.6;" />
                </div>
                <!-- 状态标识 -->
                <span
                  class="absolute top-3 left-3 inline-flex items-center px-2 py-0.5 rounded text-xs font-medium text-white"
                  :style="{ backgroundColor: statusMeta(c.status).color }"
                >
                  {{ statusMeta(c.status).label }}
                </span>
              </div>

              <div class="p-5 flex flex-col flex-1">
                <!-- 主题 -->
                <div v-if="c.theme" class="text-xs mb-1" style="color: var(--theme-primary);">
                  主题：{{ c.theme }}
                </div>
                <!-- 标题 -->
                <h3 class="text-lg font-semibold mb-2 line-clamp-1" style="color: var(--theme-text);">
                  {{ c.title }}
                </h3>
                <!-- 描述 -->
                <p
                  v-if="c.description"
                  class="text-xs mb-3 line-clamp-2 flex-1"
                  style="color: var(--theme-text-secondary);"
                >
                  {{ c.description }}
                </p>
                <div v-else class="mb-3 flex-1"></div>

                <!-- 时间与奖品 -->
                <div class="flex items-center justify-between pt-3 border-t text-xs flex-wrap gap-2" style="border-color: var(--theme-border); color: var(--theme-text-secondary);">
                  <span v-if="c.startTime || c.endTime" class="flex items-center">
                    <Calendar class="w-3 h-3 mr-0.5" />
                    {{ formatDate(c.startTime, 'YYYY-MM-DD') }} ~ {{ formatDate(c.endTime, 'YYYY-MM-DD') }}
                  </span>
                  <span v-if="c.prize" class="flex items-center" style="color: #d97706;">
                    <Gift class="w-3 h-3 mr-0.5" />
                    有奖品
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
          </div>
        </template>
      </div>
    </div>

    <SiteFooter />
  </div>
</template>
