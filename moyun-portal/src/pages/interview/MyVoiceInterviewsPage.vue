<script setup lang="ts">
import { ref, onMounted, onUnmounted, computed } from 'vue';
import { formatDate } from '@/utils/date';
import { useRouter } from 'vue-router';
import { useHead } from '@vueuse/head';
import { Mic, Clock, ChevronLeft, ChevronRight, RefreshCw, PlayCircle, FileText, Target, Loader2 } from 'lucide-vue-next';
import Breadcrumb from '@/components/Breadcrumb.vue';
import SiteFooter from '@/components/SiteFooter.vue';
import { generateSeo } from '@/utils/seo';
import { getMyVoiceInterviewList, getVoiceAnalysisStatus } from '@/api/voiceInterview';
import type { VoiceInterviewVO } from '@/api/voiceInterview';
import { useToast } from '@/composables/useToast';
import { useConfirmModal } from '@/composables/useConfirmModal';

const router = useRouter();
const toast = useToast();

const loading = ref(false);
const list = ref<VoiceInterviewVO[]>([]);
const total = ref(0);
const page = ref(1);
const pageSize = 10;

const totalPages = computed(() => Math.max(1, Math.ceil(total.value / pageSize)));

useHead(computed(() => generateSeo({
  title: '我的面试记录',
  description: '查看历史 AI 语音面试记录，回看完整面试对话、评分报告与逐题点评',
  keywords: ['面试记录', '语音面试', '面试复盘', 'AI 面试'],
  canonicalPath: '/interview/voice/history',
  robots: 'noindex,nofollow',
})));

const breadcrumbs = computed(() => [
  { label: '个人空间', path: '/user' },
  { label: '我的面试记录' },
]);

// 状态映射：进行中=info，已完成=success，已中止=warning
const statusMeta: Record<string, { label: string; class: string }> = {
  in_progress: { label: '进行中', class: 'bg-theme-info-bg text-theme-info' },
  finished: { label: '已完成', class: 'bg-theme-success-bg text-theme-success' },
  terminated: { label: '已中止', class: 'bg-theme-warning-bg text-theme-warning' },
};

const difficultyMeta: Record<string, string> = {
  easy: '简单',
  medium: '中等',
  hard: '困难',
};

function statusOf(v: VoiceInterviewVO) {
  return statusMeta[v.status] || { label: v.status || '-', class: 'bg-theme-surface text-theme-text-secondary' };
}

/** 报告生成中（已结束但 analysisStatus<2，历史页显示生成进度并轮询） */
function isGenerating(v: VoiceInterviewVO) {
  // analysisStatus 语义（后端 VoiceInterviewServiceImpl）：
  //   0 = 从未提交分析（面试创建时置 0）  1 = 分析进行中  2 = 分析完成
  // 原实现用 "(analysisStatus ?? 0) < 2"：把 **0（从未分析）** 与 null 都当成"生成中"
  // ⇒ 这些记录会永远显示"报告生成中"并持续轮询（清单 P1）。只认 1 才是"进行中"。
  return v.status === 'finished' && v.analysisStatus === 1;
}

/** 生成中项的轮询（5s 批量刷新进度；全部完成后重载列表拿最终分数/摘要） */
let generatingPollHandle: ReturnType<typeof setInterval> | null = null;

/**
 * 轮询上限（清单 P2）：原先 setInterval 无最大轮数/超时，也没有失败退出 ——
 * 单条查询异常被空 catch 吞掉后只会**无限轮询**。
 * 5s × 60 = 5 分钟；超时或连续失败即停止并明确告知用户。
 */
const MAX_POLL_ROUNDS = 60;
const MAX_POLL_FAILURES = 5;
let generatingPollRounds = 0;
let generatingPollFailures = 0;

function stopGeneratingPoll() {
  if (generatingPollHandle) {
    clearInterval(generatingPollHandle);
    generatingPollHandle = null;
  }
}

function startGeneratingPoll() {
  stopGeneratingPoll();
  const generating = list.value.filter(isGenerating);
  if (generating.length === 0) return;
  generatingPollRounds = 0;
  generatingPollFailures = 0;
  generatingPollHandle = setInterval(async () => {
    generatingPollRounds += 1;
    if (generatingPollRounds > MAX_POLL_ROUNDS) {
      stopGeneratingPoll();
      toast.warning('报告生成超时（已等待约 5 分钟），请稍后刷新页面查看结果');
      return;
    }
    let stillGenerating = false;
    let roundFailed = false;
    for (const item of generating) {
      if (!item.id) continue;
      try {
        const res = await getVoiceAnalysisStatus(item.id);
        const data = res.data;
        if (data && (data.analysisStatus ?? 0) >= 2) {
          // 单条完成：标记并等本轮结束统一重载（拿最终分数与摘要）
          item.analysisStatus = 2;
        } else if (data) {
          item.analysisStatus = data.analysisStatus ?? 1;
          item.analysisProgress = data.analysisProgress ?? item.analysisProgress;
          stillGenerating = true;
        }
      } catch {
        roundFailed = true;
        stillGenerating = true; // 单次失败不中断本轮
      }
    }
    // 连续多轮全部失败：停止轮询并告知（原实现会无限静默轮询）
    if (roundFailed) {
      generatingPollFailures += 1;
      if (generatingPollFailures >= MAX_POLL_FAILURES) {
        stopGeneratingPoll();
        toast.error('查询报告进度连续失败，已停止自动刷新，请稍后手动刷新页面');
        return;
      }
    } else {
      generatingPollFailures = 0;
    }
    if (!stillGenerating) {
      stopGeneratingPoll();
      loadList();
      toast.success('面试报告已生成，点击卡片即可查看完整报告');
    }
  }, 5000);
}

const confirmModal = useConfirmModal();

async function loadList() {
  loading.value = true;
  try {
    const res = await getMyVoiceInterviewList({ pageNum: page.value, pageSize });
    const data = res.data;
    list.value = (data?.records as VoiceInterviewVO[]) || [];
    total.value = data?.total || 0;
    // 存在报告生成中的记录则启动进度轮询（完成后自动重载列表并提示）
    startGeneratingPoll();
  } catch (err: any) {
    loadError.value = err?.message || '加载面试记录失败，请稍后重试';
    toast.error(err?.message || '加载面试记录失败，请稍后重试');
  } finally {
    loading.value = false;
  }
}

function viewReport(v: VoiceInterviewVO) {
  if (!v.id) return;
  router.push({ path: '/interview/voice', query: { id: String(v.id) } });
}

/** 重新生成报告——跳转报告页并携带 regenerate 参数自动触发（仅已出报告的场次） */
/**
/** 列表加载失败提示（清单 P2：原只有 loading 与空态，失败被当成"还没有面试记录"） */
const loadError = ref<string | null>(null);

/**
 * 重新生成报告（清单 P2）。
 *
 * <p>原先这里只做 `router.push(..., regenerate:'1')`，报告页收到该参数后走 `handleRegenerateReport(true)`
 * 的 `skipConfirm` 分支（其注释写"入口处已确认过"）—— **但本页从未弹过任何确认**。
 * 而该操作会**清空原报告后重跑**、后端还带 `@RateLimiter(key="voice:regenerate", count=10/小时)`，
 * 属覆盖性 + 有成本 + 有限流的操作，必须在入口确认。</p>
 */
async function regenerateReport(v: VoiceInterviewVO) {
  const ok = await confirmModal.confirm(
    '重新生成会覆盖当前报告并重新分析全部问答，且每小时最多 10 次。确认继续？',
    { title: '重新生成报告', confirmText: '确认重新生成', danger: true },
  );
  if (!ok) return;
  if (!v.id) return;
  router.push({ path: '/interview/voice', query: { id: String(v.id), regenerate: '1' } });
}

function startNew() {
  router.push('/interview/voice');
}

function gotoPage(p: number) {
  if (p < 1 || p > totalPages.value) return;
  page.value = p;
  loadList();
  window.scrollTo({ top: 0, behavior: 'smooth' });
}

onMounted(() => {
  loadList();
});

onUnmounted(() => {
  stopGeneratingPoll();
});
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
          @click="startNew"
          class="inline-flex items-center gap-1.5 px-3.5 py-1.5 text-sm font-medium transition-colors"
          style="background-color: var(--theme-primary); color: var(--theme-primary-contrast, #fff); border-radius: var(--theme-radius-md);"
        >
          <RotateCw :size="15" />
          再来一场
        </button>
      </div>
    </div>

    <main class="flex-1 max-w-7xl mx-auto w-full px-4 sm:px-6 lg:px-8 py-8">
      <!-- 页头 -->
      <div class="mb-6 flex items-center gap-3">
        <div
          class="w-11 h-11 flex items-center justify-center"
          style="background-color: var(--theme-primary-bg); border-radius: var(--theme-radius-lg); color: var(--theme-primary);"
        >
          <Mic :size="22" />
        </div>
        <div>
          <h1 class="text-xl font-bold" style="color: var(--theme-text-primary);">我的面试记录</h1>
          <p class="text-sm mt-0.5" style="color: var(--theme-text-secondary);">
            共 {{ total }} 场面试 · 完整保留对话内容、面试岗位与评分报告
          </p>
        </div>
      </div>

      <!-- 加载中 -->
      <div v-if="loading" class="flex flex-col items-center justify-center py-24 gap-3" style="color: var(--theme-text-secondary);">
        <RefreshCw :size="28" class="animate-spin" />
        <span class="text-sm">加载中...</span>
      </div>

      <!-- 空态 -->
      <div v-else-if="list.length === 0" class="flex flex-col items-center justify-center py-24 gap-4">
        <div
          class="w-16 h-16 flex items-center justify-center"
          style="background-color: var(--theme-primary-bg); border-radius: var(--theme-radius-full); color: var(--theme-primary);"
        >
          <Mic :size="30" />
        </div>
        <div class="text-center">
          <p class="font-medium" style="color: var(--theme-text-primary);">还没有面试记录</p>
          <p class="text-sm mt-1" style="color: var(--theme-text-secondary);">
            完成第一场 AI 语音面试后，对话与评分报告会保存在这里
          </p>
        </div>
        <button
          @click="startNew"
          class="inline-flex items-center gap-2 px-5 py-2.5 text-sm font-medium transition-colors"
          style="background-color: var(--theme-primary); color: var(--theme-primary-contrast, #fff); border-radius: var(--theme-radius-md);"
        >
          <PlayCircle :size="16" />
          开始第一场面试
        </button>
      </div>

      <!-- 记录卡片列表 -->
      <div v-else class="space-y-4">
        <div
          v-for="item in list"
          :key="item.id"
          class="border p-5 transition-all hover:shadow-md cursor-pointer group"
          style="background-color: var(--theme-surface); border-color: var(--theme-border); border-radius: var(--theme-radius-lg);"
          @click="viewReport(item)"
        >
          <div class="flex items-start justify-between gap-4">
            <div class="flex-1 min-w-0">
              <!-- 岗位 + 状态 -->
              <div class="flex items-center gap-2.5 flex-wrap">
                <h3 class="font-semibold truncate group-hover:underline" style="color: var(--theme-text-primary);">
                  {{ item.position || '未指定岗位' }}
                </h3>
                <span
                  class="inline-flex items-center px-2 py-0.5 text-xs font-medium"
                  :class="statusOf(item).class"
                  style="border-radius: var(--theme-radius-full);"
                >
                  {{ statusOf(item).label }}
                </span>
                <!-- 报告生成中进度徽标（异步任务可见进度） -->
                <span
                  v-if="isGenerating(item)"
                  class="inline-flex items-center gap-1 px-2 py-0.5 text-xs font-medium"
                  style="border-radius: var(--theme-radius-full); background-color: var(--theme-primary-bg); color: var(--theme-primary);"
                >
                  <Loader2 :size="11" class="animate-spin" />
                  报告生成中 {{ item.analysisProgress ?? 0 }}%
                </span>
              </div>

              <!-- 元信息 -->
              <div class="flex items-center gap-4 mt-2 text-xs flex-wrap" style="color: var(--theme-text-secondary);">
                <span class="inline-flex items-center gap-1">
                  <Clock :size="12" />
                  {{ formatDate(item.createTime, 'YYYY-MM-DD HH:mm') }}
                </span>
                <span class="inline-flex items-center gap-1">
                  <Target :size="12" />
                  {{ difficultyMeta[item.difficulty || ''] || '中等' }}
                </span>
                <span class="inline-flex items-center gap-1">
                  <FileText :size="12" />
                  {{ item.totalQa || 0 }} 个问答
                </span>
              </div>

              <!-- 摘要 -->
              <p v-if="item.summary" class="mt-2.5 text-sm line-clamp-2" style="color: var(--theme-text-secondary);">
                {{ item.summary }}
              </p>
            </div>

            <!-- 得分环（生成中显示进度百分比，完成后显示综合分） -->
            <div class="flex flex-col items-center shrink-0">
              <div
                v-if="isGenerating(item)"
                class="w-14 h-14 flex items-center justify-center border-2"
                style="border-color: var(--theme-primary); color: var(--theme-primary); border-radius: var(--theme-radius-full);"
              >
                <span class="text-sm font-bold">{{ item.analysisProgress ?? 0 }}%</span>
              </div>
              <div
                v-else
                class="w-14 h-14 flex items-center justify-center border-2"
                :style="{
                  borderColor: (item.score ?? 0) >= 80 ? 'var(--theme-success)' : (item.score ?? 0) >= 60 ? 'var(--theme-warning)' : 'var(--theme-error)',
                  color: (item.score ?? 0) >= 80 ? 'var(--theme-success)' : (item.score ?? 0) >= 60 ? 'var(--theme-warning)' : 'var(--theme-error)',
                  borderRadius: 'var(--theme-radius-full)',
                }"
              >
                <span class="text-lg font-bold">{{ item.score ?? '-' }}</span>
              </div>
              <span class="text-xs mt-1" style="color: var(--theme-text-secondary);">{{ isGenerating(item) ? '生成中' : '综合分' }}</span>
            </div>
          </div>

          <!-- 悬停操作提示 -->
          <div class="mt-3 pt-3 border-t flex items-center justify-between text-xs" style="border-color: var(--theme-border); color: var(--theme-text-secondary);">
            <span>查看完整对话 · 逐题点评 · 面试报告</span>
            <span class="inline-flex items-center gap-3">
              <!-- 重新生成报告（跳转报告页自动触发，复用报告生成进度链路） -->
              <button
                v-if="item.status === 'finished' && !isGenerating(item)"
                class="inline-flex items-center gap-1 px-2.5 py-1 font-medium transition-colors"
                style="border: 1px solid var(--theme-border); border-radius: var(--theme-radius-md); color: var(--theme-text-secondary); background-color: var(--theme-surface);"
                @click.stop="regenerateReport(item)"
              >
                <RotateCw :size="12" />
                重新生成报告
              </button>
              <span class="inline-flex items-center gap-1 group-hover:translate-x-0.5 transition-transform" style="color: var(--theme-primary);">
                进入复盘
                <ChevronRight :size="13" />
              </span>
            </span>
          </div>
        </div>
      </div>

      <!-- 分页 -->
      <div v-if="totalPages > 1" class="flex items-center justify-center gap-2 mt-8">
        <button
          :disabled="page <= 1"
          @click="gotoPage(page - 1)"
          class="p-2 transition-colors disabled:opacity-40 disabled:cursor-not-allowed"
          style="background-color: var(--theme-surface); border: 1px solid var(--theme-border); border-radius: var(--theme-radius-md); color: var(--theme-text-primary);"
        >
          <ChevronLeft :size="16" />
        </button>
        <span class="text-sm px-3" style="color: var(--theme-text-secondary);">
          {{ page }} / {{ totalPages }}
        </span>
        <button
          :disabled="page >= totalPages"
          @click="gotoPage(page + 1)"
          class="p-2 transition-colors disabled:opacity-40 disabled:cursor-not-allowed"
          style="background-color: var(--theme-surface); border: 1px solid var(--theme-border); border-radius: var(--theme-radius-md); color: var(--theme-text-primary);"
        >
          <ChevronRight :size="16" />
        </button>
      </div>
    </main>
  </div>
  <!-- 站点尾部（模板根级，宽度与首页一致，与语音面试页统一结构） -->
  <SiteFooter />
</template>
