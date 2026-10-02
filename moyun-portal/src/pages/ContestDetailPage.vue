<script setup lang="ts">
import { ref, computed, onMounted, watch } from 'vue';
import { formatDate } from '@/utils/date';
import { useRoute, useRouter } from 'vue-router';
import { useHead } from '@vueuse/head';
import {
  Trophy, Calendar, Gift, Heart, FileText, Loader2, Send,
} from 'lucide-vue-next';
import Breadcrumb from '@/components/Breadcrumb.vue';
import { useDictData } from '@/composables/useDictData';
import BackButton from '@/components/BackButton.vue';
import SiteFooter from '@/components/SiteFooter.vue';
import LazyImage from '@/components/LazyImage.vue';
import { generateSeo } from '@/utils/seo';
import { getContestDetail, submitContest, voteSubmission } from '@/api/contest';
import type { WritingContestVO, ContestSubmissionVO } from '@/api/contest';
import { useUserStore } from '@/stores/user';
import { useAuth } from '@/composables/useAuth';
import { useToast } from '@/composables/useToast';

const route = useRoute();
const router = useRouter();
const userStore = useUserStore();
const { requireAuth } = useAuth();
const toast = useToast();

const contestId = computed(() => route.params.id as string);
const loading = ref(false);
const error = ref<string | null>(null);
const contest = ref<WritingContestVO | null>(null);
const submissions = ref<ContestSubmissionVO[]>([]);
const votedIds = ref<Set<string | number>>(new Set());
const hasSubmitted = ref(false);

const breadcrumbs = computed(() => [
  { label: '创作挑战', path: '/contests' },
  { label: contest.value?.title || '挑战详情' },
]);

// 投稿表单
const articleIdInput = ref('');
const submitting = ref(false);

const isLoggedIn = computed(() => !!userStore.user);

useHead(computed(() => generateSeo({
  title: contest.value?.title || '活动详情',
  description: contest.value?.description || contest.value?.theme || '旭林创作挑战活动详情',
  keywords: ['创作挑战', contest.value?.title || '旭林'].filter(Boolean) as string[],
  canonicalPath: `/contest/${contestId.value}`,
})));

onMounted(() => {
  loadDetail();
});

watch(contestId, (newId, oldId) => {
  if (newId && newId !== oldId) {
    contest.value = null;
    submissions.value = [];
    votedIds.value = new Set();
    hasSubmitted.value = false;
    loadDetail();
  }
});

/**
 * 加载活动详情。
 *
 * @param silent 静默刷新（清单 P2）：投稿成功后的刷新不应把整页替换成加载圈
 *               （模板 `v-if="loading"` / `v-else-if="contest"` 互斥，会闪一下）。
 */
async function loadDetail(silent = false) {
  // 清单 P2：contestId 未校验格式，非数字 id 会拼进后端路径变量触发类型转换异常
  if (!/^\d+$/.test(contestId.value)) {
    loading.value = false;
    error.value = '活动不存在或已结束';
    return;
  }
  if (!silent) loading.value = true;
  error.value = null;
  try {
    const res = await getContestDetail(contestId.value);
    if (res.code === 200 && res.data) {
      contest.value = res.data.contest;
      submissions.value = res.data.submissions || [];
      votedIds.value = new Set(res.data.votedSubmissionIds || []);
      hasSubmitted.value = !!res.data.hasSubmitted;
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

async function handleSubmit() {
  if (!requireAuth(router.currentRoute.value.fullPath)) return;
  if (!contest.value || submitting.value) return;
  const aid = articleIdInput.value.trim();
  if (!aid) {
    toast.error('请输入文章ID');
    return;
  }
  // 清单 P2：原先"文章ID"是纯文本框，无格式校验，非数字会被拼进后端 Long 参数触发转换异常。
  // 这里先做格式校验（可在「我的文章」里复制文章 ID）。
  if (!/^\d+$/.test(aid)) {
    toast.error('文章ID 必须是数字，可在「我的文章」中查看');
    return;
  }
  submitting.value = true;
  try {
    const res = await submitContest(contest.value.id, aid);
    if (res.code === 200) {
      articleIdInput.value = '';
      hasSubmitted.value = true;
      toast.success('投稿成功');
      await loadDetail(true);   // 静默刷新：不闪加载态
    } else {
      toast.error(res.message || '投稿失败');
    }
  } catch (err) {
    const e = err as { message?: string };
    toast.error(e?.message || '投稿失败，请稍后重试');
  } finally {
    submitting.value = false;
  }
}

/**
 * 当前是否可投票：与后端 toggleVote 的门禁同一口径
 * （活动 status 必须为 voting，且未过 voteEndTime）。
 * 页面此前已在展示"投票截止"，但按钮不做任何判断 ⇒ 未开始/已结束的活动也能点、后端也放行。
 */
const canVote = computed(() => {
  const c = contest.value;
  if (!c || c.status !== 'voting') return false;
  if (c.voteEndTime) {
    const end = new Date(String(c.voteEndTime).replace(/-/g, '/')).getTime();
    if (!Number.isNaN(end) && Date.now() > end) return false;
  }
  return true;
});

const voteClosedHint = computed(() => {
  const c = contest.value;
  if (!c) return '';
  if (c.status === 'ended') return '活动已结束，投票已关闭';
  if (c.status === 'collecting') return '征集阶段暂未开放投票';
  if (c.status === 'draft') return '活动尚未开始';
  if (!canVote.value) return '投票已截止';
  return '';
});

/** 投票在途锁（清单 P2）：原投票按钮只绑 canVote，连点/双击会发出两次 toggle 请求 */
const votingId = ref<string | number | null>(null);

/**
 * 是否为当前用户自己的投稿（清单 P2）。
 *
 * <p>后端 `toggleVote` 只按 submissionId 查重投票记录，**没有任何"不能给自己投票"的限制**，
 * 前端原先也未处理。这里先行拦截（后端补门禁另见对账记录），避免自投把票数刷高。</p>
 */
function isOwnSubmission(sub: ContestSubmissionVO): boolean {
  const uid = userStore.user?.id;
  return uid != null && sub.userId != null && String(sub.userId) === String(uid);
}

async function handleVote(sub: ContestSubmissionVO) {
  if (!requireAuth(router.currentRoute.value.fullPath)) return;
  if (!sub.id) return;
  if (votingId.value === sub.id) return;   // 该条正在投票：忽略连点
  if (isOwnSubmission(sub)) {
    toast.warning('不能给自己的投稿投票');
    return;
  }
  votingId.value = sub.id;
  // 前端先行拦截（后端同样有门禁，此处避免无谓请求与困惑）
  if (!canVote.value) {
    toast.warning(voteClosedHint.value || '当前不可投票');
    return;
  }
  try {
    const res = await voteSubmission(sub.id);
    if (res.code === 200 && res.data) {
      // 同步本地状态
      if (res.data.voted) {
        votedIds.value.add(sub.id);
      } else {
        votedIds.value.delete(sub.id);
      }
      sub.voteCount = res.data.voteCount;
    } else {
      toast.error(res.message || '投票失败');
    }
  } catch (err) {
    const e = err as { message?: string };
    toast.error(e?.message || '投票失败，请稍后重试');
  } finally {
    votingId.value = null;
  }
}

function gotoArticle(id: string | number) {
  router.push(`/article/${id}`);
}

function isVoted(sub: ContestSubmissionVO) {
  return sub.id != null && votedIds.value.has(sub.id);
}

// 活动状态文案：字典 cms_contest_status 驱动（v13.92；该字典此前"有类型无数据"，已同期补齐）
// 颜色保留本地色板：色值属展示样式，字典 list_class 映射的是徽章类名，两者口径不同，不强行合并。
const CONTEST_STATUS_COLORS: Record<string, string> = {
  collecting: '#16a34a',
  voting: '#d97706',
  ended: '#6b7280',
  draft: '#9ca3af',
};
const CONTEST_STATUS_LABELS: Record<string, string> = {
  collecting: '征稿中',
  voting: '投票中',
  ended: '已结束',
  draft: '草稿',
};
const contestDict = useDictData(['cms_contest_status']);
const contestStatusLabels = computed<Record<string, string>>(() => {
  const m: Record<string, string> = { ...CONTEST_STATUS_LABELS };
  for (const i of contestDict['cms_contest_status'] || []) {
    m[i.dictValue] = i.dictLabel;
  }
  return m;
});

function statusMeta(status?: string) {
  const key = status || '';
  return {
    label: contestStatusLabels.value[key] || '进行中',
    color: CONTEST_STATUS_COLORS[key] || 'var(--theme-primary)'
  };
}

function submissionStatusMeta(status?: string) {
  switch (status) {
    case 'shortlisted': return { label: '入围', color: '#16a34a' };
    case 'winner': return { label: '获奖', color: '#d97706' };
    case 'eliminated': return { label: '淘汰', color: '#6b7280' };
    default: return { label: '待评审', color: 'var(--theme-text-secondary)' };
  }
}

// 投稿阶段（可投稿）判断
const canSubmit = computed(() => {
  if (!contest.value) return false;
  const s = contest.value.status;
  return s === 'collecting' || s === 'voting';
});
</script>

<template>
  <div class="min-h-screen flex flex-col" style="background-color: var(--theme-bg);">
    <div class="border-b sticky top-0 z-30 backdrop-blur-sm py-3" style="background-color: var(--theme-surface); border-color: var(--theme-border);">
      <div class="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 flex items-center justify-between gap-4">
        <Breadcrumb :items="breadcrumbs" />
        <div class="flex items-center gap-2">
          <BackButton fallback="/contests" />
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
        @click="loadDetail()"
        class="px-4 py-2 text-white rounded-lg text-sm transition hover:opacity-90"
        style="background-color: var(--theme-primary);"
      >
        重试
      </button>
    </div>

    <!-- 活动内容 -->
    <template v-else-if="contest">
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
            <div class="w-40 h-44 rounded-lg overflow-hidden shadow-lg flex-shrink-0 mx-auto md:mx-0" style="background-color: var(--theme-bg);">
              <LazyImage
                v-if="contest.cover"
                :src="contest.cover"
                :alt="contest.title"
                class="w-full h-full object-cover"
              />
              <div v-else class="w-full h-full flex items-center justify-center" style="background: linear-gradient(135deg, var(--theme-accent), color-mix(in srgb, var(--theme-accent) 50%, #c4b5fd));">
                <Trophy class="w-12 h-12" style="color: var(--theme-primary); opacity: 0.6;" />
              </div>
            </div>
            <!-- 信息 -->
            <div class="flex-1 min-w-0 text-center md:text-left">
              <div class="mb-2">
                <span
                  class="inline-flex items-center px-2.5 py-0.5 rounded-full text-xs font-medium"
                  :style="{ backgroundColor: statusMeta(contest.status).color }"
                >
                  {{ statusMeta(contest.status).label }}
                </span>
              </div>
              <h1 class="text-2xl md:text-3xl font-bold mb-2">{{ contest.title }}</h1>
              <p v-if="contest.theme" class="text-sm md:text-base opacity-90 mb-3">
                主题：{{ contest.theme }}
              </p>
              <!-- 时间 -->
              <div class="flex items-center justify-center md:justify-start gap-4 mb-3 text-sm flex-wrap">
                <span v-if="contest.startTime || contest.endTime" class="flex items-center">
                  <Calendar class="w-4 h-4 mr-1.5" />
                  {{ formatDate(contest.startTime, 'YYYY-MM-DD') }} ~ {{ formatDate(contest.endTime, 'YYYY-MM-DD') }}
                </span>
                <span v-if="contest.voteEndTime" class="flex items-center">
                  <Calendar class="w-4 h-4 mr-1.5" />
                  投票截止：{{ formatDate(contest.voteEndTime, 'YYYY-MM-DD HH:mm') }}
                </span>
              </div>
              <!-- 奖品 -->
              <div v-if="contest.prize" class="inline-flex items-center px-3 py-1 rounded-full text-sm" style="background-color: rgba(255,255,255,0.2);">
                <Gift class="w-4 h-4 mr-1.5" />{{ contest.prize }}
              </div>
            </div>
          </div>
        </div>
          </div>
        </div>
      </div>

      <!-- 内容区 -->
      <div class="flex-1 py-8">
        <div class="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8">
          <!-- 活动描述 -->
          <div
            v-if="contest.description"
            class="rounded-xl border p-5 mb-6"
            style="background-color: var(--theme-surface); border-color: var(--theme-border);"
          >
            <h2 class="text-base font-semibold mb-3 flex items-center" style="color: var(--theme-text);">
              <FileText class="w-4 h-4 mr-2" style="color: var(--theme-primary);" />活动详情
            </h2>
            <div class="text-sm whitespace-pre-wrap" style="color: var(--theme-text);">
              {{ contest.description }}
            </div>
          </div>

          <!-- 投稿 -->
          <div
            class="rounded-xl border p-5 mb-6"
            style="background-color: var(--theme-surface); border-color: var(--theme-border);"
          >
            <div class="flex items-center justify-between mb-3 flex-wrap gap-2">
              <h2 class="text-base font-semibold flex items-center" style="color: var(--theme-text);">
                <Send class="w-4 h-4 mr-2" style="color: var(--theme-primary);" />我要投稿
              </h2>
              <span v-if="hasSubmitted" class="text-xs" style="color: #16a34a;">你已投稿本次活动</span>
            </div>

            <div v-if="!canSubmit" class="text-xs py-2" style="color: var(--theme-text-secondary);">
              当前活动状态不在征稿期内，暂不可投稿
            </div>
            <div v-else-if="hasSubmitted" class="text-xs py-2" style="color: var(--theme-text-secondary);">
              你已投稿过该活动，每个活动仅可投稿一篇
            </div>
            <div v-else-if="!isLoggedIn" class="text-xs py-2" style="color: var(--theme-text-secondary);">
              请先登录后再投稿
            </div>
            <div v-else class="flex items-center gap-2 flex-wrap">
              <input
                v-model="articleIdInput"
                @keyup.enter="handleSubmit"
                type="text"
                placeholder="输入你的文章ID进行投稿..."
                class="flex-1 min-w-[200px] px-3 py-2 rounded-md text-sm focus:outline-none"
                style="background-color: var(--theme-bg); color: var(--theme-text); border: 1px solid var(--theme-border);"
              />
              <button
                @click="handleSubmit"
                :disabled="submitting"
                class="inline-flex items-center px-4 py-2 rounded-md text-sm font-medium text-white transition hover:opacity-90 disabled:opacity-50"
                style="background-color: var(--theme-primary);"
              >
                <Send class="w-4 h-4 mr-1.5" />
                {{ submitting ? '提交中...' : '投稿' }}
              </button>
            </div>
          </div>

          <!-- 投稿列表 -->
          <div class="mb-6">
            <h2 class="text-lg font-semibold mb-4 flex items-center" style="color: var(--theme-text);">
              <Trophy class="w-5 h-5 mr-2" style="color: var(--theme-primary);" />
              投稿列表
              <span class="ml-2 text-xs font-normal" style="color: var(--theme-text-secondary);">
                共 {{ submissions.length }} 篇
              </span>
            </h2>

            <!-- 空状态 -->
            <div
              v-if="submissions.length === 0"
              class="rounded-xl border p-10 text-center"
              style="background-color: var(--theme-surface); border-color: var(--theme-border);"
            >
              <FileText class="w-10 h-10 mx-auto mb-2" style="color: var(--theme-text-secondary); opacity: 0.5;" />
              <p class="text-sm" style="color: var(--theme-text-secondary);">还没有人投稿，快来抢沙发</p>
            </div>

            <!-- 列表 -->
            <div v-else class="space-y-3">
              <div
                v-for="(sub, idx) in submissions"
                :key="sub.id"
                class="rounded-xl border p-4 flex items-center gap-3 transition hover:shadow-sm"
                style="background-color: var(--theme-surface); border-color: var(--theme-border);"
              >
                <!-- 排名 -->
                <div class="text-sm font-mono w-8 text-center flex-shrink-0" style="color: var(--theme-text-secondary);">
                  {{ idx + 1 }}
                </div>

                <!-- 内容 -->
                <div class="flex-1 min-w-0">
                  <div class="flex items-center gap-2 mb-1 flex-wrap">
                    <h3
                      class="text-sm font-semibold cursor-pointer hover:underline"
                      style="color: var(--theme-text);"
                      @click="gotoArticle(sub.articleId)"
                    >
                      文章 #{{ sub.articleId }}
                    </h3>
                    <span
                      class="inline-flex items-center px-1.5 py-0.5 rounded text-xs font-medium"
                      :style="{
                        backgroundColor: submissionStatusMeta(sub.status).color + '22',
                        color: submissionStatusMeta(sub.status).color,
                      }"
                    >
                      {{ submissionStatusMeta(sub.status).label }}
                    </span>
                    <span v-if="sub.remark" class="text-xs" style="color: var(--theme-text-secondary);">
                      {{ sub.remark }}
                    </span>
                  </div>
                  <div class="flex items-center gap-3 text-xs flex-wrap" style="color: var(--theme-text-secondary);">
                    <span>用户 #{{ sub.userId }}</span>
                    <span v-if="sub.createdTime">{{ formatDate(sub.createdTime, 'YYYY-MM-DD HH:mm') }}</span>
                  </div>
                </div>

                <!-- 投票按钮 -->
                <div class="flex items-center gap-2 flex-shrink-0">
                  <button
                    @click="handleVote(sub)"
                    :disabled="!canVote || votingId === sub.id || isOwnSubmission(sub)"
                    class="inline-flex items-center px-3 py-1.5 rounded-lg text-xs font-medium transition hover:opacity-80 disabled:opacity-50 disabled:cursor-not-allowed"
                    :style="{
                      backgroundColor: isVoted(sub) ? '#ef4444' : 'var(--theme-bg)',
                      color: isVoted(sub) ? '#fff' : 'var(--theme-text-secondary)',
                      border: '1px solid var(--theme-border)',
                    }"
                    :title="isOwnSubmission(sub) ? '不能给自己的投稿投票' : (!canVote ? (voteClosedHint || '当前不可投票') : (isVoted(sub) ? '取消投票' : '投一票'))"
                  >
                    <Heart class="w-3.5 h-3.5 mr-1" :fill="isVoted(sub) ? 'currentColor' : 'none'" />
                    {{ sub.voteCount || 0 }}
                  </button>
                  <span v-if="!canVote" class="text-[11px]" style="color: var(--theme-text-secondary);">
                    {{ voteClosedHint }}
                  </span>
                  <button
                    @click="gotoArticle(sub.articleId)"
                    class="inline-flex items-center px-2.5 py-1.5 rounded-lg text-xs font-medium transition hover:opacity-80"
                    style="background-color: var(--theme-accent); color: var(--theme-primary);"
                  >
                    阅读
                  </button>
                </div>
              </div>
            </div>
          </div>
        </div>
      </div>
    </template>

    <SiteFooter />
  </div>
</template>
