<script setup lang="ts">
import { ref, computed, onMounted, watch } from 'vue';
import { useHead } from '@vueuse/head';
import { Swords, Loader2, ChevronLeft, Send, CheckCircle2, XCircle, Plus, Trophy, Building2 } from 'lucide-vue-next';
import SiteFooter from '@/components/SiteFooter.vue';
import { generateSeo } from '@/utils/seo';
import { getSafeAvatar } from '@/utils/avatar';
import {
  createPkChallenge,
  acceptPkChallenge,
  declinePkChallenge,
  submitPkAnswer,
  getMyPkChallenges,
  getPkDetail,
  getCompanyPkLeaderboard,
} from '@/api/learnStats';
import type { PkChallenge, PkStatus, PkScene, CompanyPkLeaderboardItem } from '@/api/learnStats';
import { useUserStore } from '@/stores/user';

useHead(computed(() => generateSeo({
  title: 'PK 对战',
  description: '墨韵智库 PK 对战 - 与好友异步答题对战，挑战公司题目榜。',
  canonicalPath: '/learn/pk',
})));

const userStore = useUserStore();
const myUserId = computed(() => userStore.user?.id ?? null);

/** 当前用户ID比较（后端返回数字ID，前端 User.id 为字符串，统一 String 比较） */
function isMe(id: number | null | undefined): boolean {
  if (myUserId.value == null || id == null) return false;
  return String(myUserId.value) === String(id);
}

// ==================== 主 Tab ====================
type MainTab = 'mine' | 'company';
const mainTab = ref<MainTab>('mine');

// ==================== 我的对战 ====================
const loadingList = ref(false);
const listError = ref<string | null>(null);
const myList = ref<PkChallenge[]>([]);
const statusFilter = ref<PkStatus | ''>('');

async function loadMyList() {
  loadingList.value = true;
  listError.value = null;
  try {
    const res = await getMyPkChallenges(statusFilter.value || undefined);
    if (res.code === 200) {
      myList.value = res.data || [];
    } else {
      listError.value = res.message || '加载对战列表失败';
    }
  } catch (err) {
    const e = err as { message?: string };
    listError.value = e?.message || '加载对战列表失败，请稍后重试';
  } finally {
    loadingList.value = false;
  }
}

watch(statusFilter, loadMyList);
watch(mainTab, (t) => {
  if (t === 'mine') loadMyList();
  else loadCompany();
});

// ==================== 发起挑战 ====================
const showCreate = ref(false);
const createForm = ref<{ opponentId: string; scene: PkScene; companyId: string }>({
  opponentId: '',
  scene: '1v1',
  companyId: '',
});
const creating = ref(false);
const createError = ref<string | null>(null);

function openCreate() {
  createError.value = null;
  createForm.value = { opponentId: '', scene: '1v1', companyId: '' };
  showCreate.value = true;
}

async function doCreate() {
  createError.value = null;
  const opponentId = Number(createForm.value.opponentId);
  if (!opponentId || Number.isNaN(opponentId)) {
    createError.value = '请输入对手用户ID';
    return;
  }
  if (isMe(opponentId)) {
    createError.value = '不能与自己对战';
    return;
  }
  if (createForm.value.scene === 'company' && !createForm.value.companyId) {
    createError.value = '公司挑战需指定公司ID';
    return;
  }
  creating.value = true;
  try {
    const res = await createPkChallenge({
      opponentId,
      scene: createForm.value.scene,
      companyId: createForm.value.scene === 'company' ? Number(createForm.value.companyId) : null,
    });
    if (res.code === 200) {
      showCreate.value = false;
      await loadMyList();
      // 直接进入答题
      if (res.data) openPlay(res.data);
    } else {
      createError.value = res.message || '发起挑战失败';
    }
  } catch (err) {
    const e = err as { message?: string };
    createError.value = e?.message || '发起挑战失败';
  } finally {
    creating.value = false;
  }
}

async function doAccept(c: PkChallenge) {
  try {
    const res = await acceptPkChallenge(c.id);
    if (res.code === 200) {
      await loadMyList();
      openPlay(c);
    }
  } catch (err) {
    listError.value = (err as { message?: string })?.message || '操作失败';
  }
}

async function doDecline(c: PkChallenge) {
  try {
    const res = await declinePkChallenge(c.id);
    if (res.code === 200) await loadMyList();
  } catch (err) {
    listError.value = (err as { message?: string })?.message || '操作失败';
  }
}

// ==================== 答题对战 ====================
const playing = ref<PkChallenge | null>(null);
const playLoading = ref(false);
// 每题答案与提交结果（按题目ID索引）
const answers = ref<Record<number, string>>({});
const results = ref<Record<number, { isSuccess: boolean; submitted: boolean }>>({});
const submittingId = ref<number | null>(null);

async function openPlay(c: PkChallenge) {
  playLoading.value = true;
  playing.value = c;
  answers.value = {};
  results.value = {};
  try {
    const res = await getPkDetail(c.id);
    if (res.code === 200 && res.data) {
      playing.value = res.data;
    }
  } catch (err) {
    // 忽略，使用列表数据
  } finally {
    playLoading.value = false;
  }
}

function closePlay() {
  playing.value = null;
  loadMyList();
}

async function doSubmit(qId: number) {
  if (!playing.value) return;
  const ans = answers.value[qId] || '';
  submittingId.value = qId;
  try {
    const res = await submitPkAnswer(playing.value.id, qId, ans);
    if (res.code === 200 && res.data) {
      results.value[qId] = { isSuccess: res.data.isSuccess, submitted: true };
      // 刷新详情以同步双方得分
      const detail = await getPkDetail(playing.value.id);
      if (detail.code === 200 && detail.data) {
        playing.value = detail.data;
      }
    }
  } catch (err) {
    listError.value = (err as { message?: string })?.message || '提交失败';
  } finally {
    submittingId.value = null;
  }
}

const myScore = computed(() => {
  if (!playing.value) return 0;
  return isMe(playing.value.challengerId)
    ? playing.value.challengerScore
    : playing.value.opponentScore;
});

const oppScore = computed(() => {
  if (!playing.value) return 0;
  return isMe(playing.value.challengerId)
    ? playing.value.opponentScore
    : playing.value.challengerScore;
});

const answeredCount = computed(() => {
  return Object.values(results.value).filter((r) => r.submitted).length;
});

const isFinished = computed(() => playing.value?.status === 'finished');

const myIsWinner = computed(() => {
  if (!playing.value) return false;
  return isMe(playing.value.winnerId);
});

const isDraw = computed(() => playing.value?.status === 'finished' && playing.value.winnerId == null);

// ==================== 公司挑战榜 ====================
const companyLoading = ref(false);
const companyError = ref<string | null>(null);
const companyList = ref<CompanyPkLeaderboardItem[]>([]);
const companyFilter = ref<string>('');

async function loadCompany() {
  companyLoading.value = true;
  companyError.value = null;
  try {
    const cid = companyFilter.value ? Number(companyFilter.value) : undefined;
    const res = await getCompanyPkLeaderboard(cid, 100);
    if (res.code === 200) {
      companyList.value = res.data || [];
    } else {
      companyError.value = res.message || '加载挑战榜失败';
    }
  } catch (err) {
    companyError.value = (err as { message?: string })?.message || '加载挑战榜失败';
  } finally {
    companyLoading.value = false;
  }
}

// ==================== 视图辅助 ====================
function statusLabel(s: PkStatus): string {
  switch (s) {
    case 'pending': return '待应战';
    case 'accepted': return '已接受';
    case 'declined': return '已拒绝';
    case 'ongoing': return '进行中';
    case 'finished': return '已结束';
    default: return s;
  }
}

function statusColor(s: PkStatus): string {
  switch (s) {
    case 'pending': return '#f59e0b';
    case 'declined': return '#6b7280';
    case 'ongoing': return '#10b981';
    case 'finished': return '#3b82f6';
    default: return '#6b7280';
  }
}

function difficultyColor(d: string | null | undefined): string {
  switch (d) {
    case 'easy': return '#10b981';
    case 'medium': return '#f59e0b';
    case 'hard': return '#ef4444';
    default: return '#6b7280';
  }
}

function safeName(n: string | null | undefined, id: number): string {
  return n && n.length ? n : `用户${id}`;
}

onMounted(loadMyList);
</script>

<template>
  <div class="min-h-screen flex flex-col" style="background-color: var(--theme-bg);">
    <!-- 顶部条 -->
    <div class="border-b" style="background-color: var(--theme-surface); border-color: var(--theme-border);">
      <div class="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8">
        <div class="flex items-center gap-3 h-16">
          <router-link to="/learn/leaderboard" class="p-2 rounded-lg transition-colors hover:bg-gray-100" style="color: var(--theme-text-secondary);">
            <ChevronLeft class="w-5 h-5" />
          </router-link>
          <h1 class="text-lg sm:text-xl font-semibold flex items-center gap-2" style="color: var(--theme-text);">
            <Swords class="w-5 h-5" style="color: #ef4444;" />
            PK 对战
          </h1>
          <div class="ml-auto flex gap-1 p-1 rounded-lg" style="background-color: var(--theme-bg);">
            <button
              @click="mainTab = 'mine'"
              class="flex items-center gap-1 px-3 py-1.5 rounded-md text-sm transition-colors"
              :style="mainTab === 'mine'
                ? 'background-color: var(--theme-surface); color: var(--theme-primary); font-weight: 600;'
                : 'color: var(--theme-text-secondary);'"
            >
              <Swords class="w-4 h-4" />
              <span class="hidden sm:inline">我的对战</span>
            </button>
            <button
              @click="mainTab = 'company'"
              class="flex items-center gap-1 px-3 py-1.5 rounded-md text-sm transition-colors"
              :style="mainTab === 'company'
                ? 'background-color: var(--theme-surface); color: var(--theme-primary); font-weight: 600;'
                : 'color: var(--theme-text-secondary);'"
            >
              <Building2 class="w-4 h-4" />
              <span class="hidden sm:inline">公司挑战榜</span>
            </button>
          </div>
        </div>
      </div>
    </div>

    <!-- 主内容 -->
    <div class="flex-1 py-8">
      <div class="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 space-y-6">
        <!-- ============ 我的对战 ============ -->
        <template v-if="mainTab === 'mine'">
          <!-- 操作栏 -->
          <div class="flex flex-wrap items-center gap-3">
            <button
              @click="openCreate"
              class="flex items-center gap-1.5 px-4 py-2 rounded-lg text-sm font-medium text-white transition-opacity hover:opacity-90"
              style="background-color: var(--theme-primary);"
            >
              <Plus class="w-4 h-4" />
              发起挑战
            </button>
            <div class="flex gap-1 p-1 rounded-lg" style="background-color: var(--theme-surface);">
              <button
                v-for="s in [{ v: '', l: '全部' }, { v: 'pending', l: '待应战' }, { v: 'ongoing', l: '进行中' }, { v: 'finished', l: '已结束' }] as const"
                :key="s.v"
                @click="statusFilter = s.v as PkStatus | ''"
                class="px-3 py-1 rounded-md text-sm transition-colors"
                :style="statusFilter === s.v
                  ? 'background-color: var(--theme-primary); color: #fff; font-weight: 600;'
                  : 'color: var(--theme-text-secondary);'"
              >
                {{ s.l }}
              </button>
            </div>
          </div>

          <!-- 加载中 -->
          <div v-if="loadingList" class="flex flex-col items-center justify-center py-20" style="color: var(--theme-text-secondary);">
            <Loader2 class="w-8 h-8 animate-spin mb-3" />
            <span>正在加载对战...</span>
          </div>

          <!-- 错误 -->
          <div v-else-if="listError" class="rounded-lg p-6 text-center" style="background-color: var(--theme-surface); border: 1px solid var(--theme-border); color: var(--theme-text-secondary);">
            {{ listError }}
            <button @click="loadMyList" class="ml-3 underline">重试</button>
          </div>

          <!-- 列表 -->
          <div v-else class="space-y-3">
            <div
              v-for="c in myList"
              :key="c.id"
              class="rounded-lg p-4 flex flex-wrap items-center gap-4"
              style="background-color: var(--theme-surface); border: 1px solid var(--theme-border);"
            >
              <!-- 双方 -->
              <div class="flex items-center gap-3 flex-1 min-w-[220px]">
                <div class="flex flex-col items-center">
                  <img :src="getSafeAvatar(c.challengerAvatar, String(c.challengerId))" class="w-10 h-10 rounded-full object-cover" :alt="safeName(c.challengerNickname, c.challengerId)" />
                  <span class="text-xs mt-1 max-w-[80px] truncate" style="color: var(--theme-text);">{{ safeName(c.challengerNickname, c.challengerId) }}</span>
                </div>
                <div class="text-center px-2">
                  <div class="text-xl font-bold" style="color: var(--theme-primary);">{{ c.challengerScore }} : {{ c.opponentScore }}</div>
                  <span
                    class="text-xs px-2 py-0.5 rounded-full"
                    :style="`background-color: ${statusColor(c.status)}22; color: ${statusColor(c.status)};`"
                  >{{ statusLabel(c.status) }}</span>
                </div>
                <div class="flex flex-col items-center">
                  <img :src="getSafeAvatar(c.opponentAvatar, String(c.opponentId))" class="w-10 h-10 rounded-full object-cover" :alt="safeName(c.opponentNickname, c.opponentId)" />
                  <span class="text-xs mt-1 max-w-[80px] truncate" style="color: var(--theme-text);">{{ safeName(c.opponentNickname, c.opponentId) }}</span>
                </div>
              </div>

              <!-- 场景 -->
              <div class="text-xs" style="color: var(--theme-text-secondary);">
                <div>{{ c.scene === 'company' ? '公司挑战' : '好友 PK' }}</div>
                <div class="mt-0.5">{{ c.createdTime }}</div>
              </div>

              <!-- 操作 -->
              <div class="flex gap-2">
                <!-- 应战方待应战 -->
                <template v-if="c.status === 'pending' && isMe(c.opponentId)">
                  <button @click="doAccept(c)" class="px-3 py-1.5 rounded-md text-sm text-white" style="background-color: #10b981;">接受</button>
                  <button @click="doDecline(c)" class="px-3 py-1.5 rounded-md text-sm" style="background-color: var(--theme-bg); color: var(--theme-text-secondary); border: 1px solid var(--theme-border);">拒绝</button>
                </template>
                <!-- 进行中：进入答题 -->
                <button
                  v-if="c.status === 'ongoing'"
                  @click="openPlay(c)"
                  class="px-3 py-1.5 rounded-md text-sm text-white"
                  style="background-color: var(--theme-primary);"
                >进入答题</button>
                <!-- 已结束：查看详情 -->
                <button
                  v-if="c.status === 'finished'"
                  @click="openPlay(c)"
                  class="px-3 py-1.5 rounded-md text-sm"
                  style="background-color: var(--theme-bg); color: var(--theme-primary); border: 1px solid var(--theme-primary);"
                >查看详情</button>
              </div>
            </div>

            <!-- 空状态 -->
            <div v-if="!loadingList && myList.length === 0" class="rounded-lg p-8 text-center" style="background-color: var(--theme-surface); border: 1px solid var(--theme-border);">
              <p class="text-sm" style="color: var(--theme-text-secondary);">还没有对战记录，点击「发起挑战」开始第一场 PK 吧。</p>
            </div>
          </div>
        </template>

        <!-- ============ 公司挑战榜 ============ -->
        <template v-if="mainTab === 'company'">
          <div class="flex flex-wrap items-center gap-3">
            <input
              v-model="companyFilter"
              type="number"
              placeholder="公司ID（可选）"
              class="px-3 py-2 rounded-lg text-sm w-48"
              style="background-color: var(--theme-surface); border: 1px solid var(--theme-border); color: var(--theme-text);"
            />
            <button
              @click="loadCompany"
              class="px-4 py-2 rounded-lg text-sm font-medium text-white"
              style="background-color: var(--theme-primary);"
            >查询</button>
          </div>

          <div v-if="companyLoading" class="flex flex-col items-center justify-center py-20" style="color: var(--theme-text-secondary);">
            <Loader2 class="w-8 h-8 animate-spin mb-3" />
            <span>正在加载挑战榜...</span>
          </div>

          <div v-else-if="companyError" class="rounded-lg p-6 text-center" style="background-color: var(--theme-surface); border: 1px solid var(--theme-border); color: var(--theme-text-secondary);">
            {{ companyError }}
            <button @click="loadCompany" class="ml-3 underline">重试</button>
          </div>

          <section v-else-if="companyList.length" class="rounded-lg overflow-hidden" style="background-color: var(--theme-surface); border: 1px solid var(--theme-border);">
            <table class="w-full text-sm">
              <thead>
                <tr style="background-color: var(--theme-bg); color: var(--theme-text-secondary);">
                  <th class="text-left px-4 py-3 font-medium w-16">名次</th>
                  <th class="text-left px-4 py-3 font-medium">用户</th>
                  <th class="text-left px-4 py-3 font-medium hidden sm:table-cell">公司</th>
                  <th class="text-right px-4 py-3 font-medium">通过题数</th>
                </tr>
              </thead>
              <tbody>
                <tr
                  v-for="item in companyList"
                  :key="`${item.userId}-${item.companyId}`"
                  class="border-t"
                  style="border-color: var(--theme-border);"
                >
                  <td class="px-4 py-3" style="color: var(--theme-text-secondary);">
                    <span v-if="item.rank <= 3" class="font-bold" :style="`color: ${item.rank === 1 ? '#f59e0b' : item.rank === 2 ? '#9ca3af' : '#f97316'};`">#{{ item.rank }}</span>
                    <span v-else>{{ item.rank }}</span>
                  </td>
                  <td class="px-4 py-3">
                    <router-link :to="`/author/${item.userId}`" class="flex items-center gap-2 group">
                      <img :src="getSafeAvatar(item.avatar, String(item.userId))" :alt="item.nickname" class="w-7 h-7 rounded-full object-cover flex-shrink-0" />
                      <span class="truncate group-hover:underline" style="color: var(--theme-text);">{{ item.nickname }}</span>
                    </router-link>
                  </td>
                  <td class="px-4 py-3 hidden sm:table-cell" style="color: var(--theme-text-secondary);">{{ item.companyName || '—' }}</td>
                  <td class="px-4 py-3 text-right font-semibold" style="color: var(--theme-primary);">{{ item.passedCount }}</td>
                </tr>
              </tbody>
            </table>
          </section>

          <div v-else class="rounded-lg p-8 text-center" style="background-color: var(--theme-surface); border: 1px solid var(--theme-border);">
            <p class="text-sm" style="color: var(--theme-text-secondary);">暂无挑战榜数据。</p>
          </div>
        </template>
      </div>
    </div>

    <SiteFooter />

    <!-- ============ 发起挑战 弹窗 ============ -->
    <div v-if="showCreate" class="fixed inset-0 z-50 flex items-center justify-center p-4" style="background-color: rgba(0,0,0,0.5);" @click.self="showCreate = false">
      <div class="rounded-lg w-full max-w-md p-6" style="background-color: var(--theme-surface); border: 1px solid var(--theme-border);">
        <h3 class="text-lg font-semibold mb-4" style="color: var(--theme-text);">发起 PK 挑战</h3>
        <div class="space-y-4">
          <div>
            <label class="block text-sm mb-1" style="color: var(--theme-text-secondary);">对手用户ID</label>
            <input
              v-model="createForm.opponentId"
              type="number"
              placeholder="输入对手的用户ID"
              class="w-full px-3 py-2 rounded-md text-sm"
              style="background-color: var(--theme-bg); border: 1px solid var(--theme-border); color: var(--theme-text);"
            />
          </div>
          <div>
            <label class="block text-sm mb-1" style="color: var(--theme-text-secondary);">场景</label>
            <div class="flex gap-2">
              <button
                @click="createForm.scene = '1v1'"
                class="flex-1 px-3 py-2 rounded-md text-sm"
                :style="createForm.scene === '1v1' ? 'background-color: var(--theme-primary); color: #fff;' : 'background-color: var(--theme-bg); color: var(--theme-text-secondary); border: 1px solid var(--theme-border);'"
              >好友 PK</button>
              <button
                @click="createForm.scene = 'company'"
                class="flex-1 px-3 py-2 rounded-md text-sm"
                :style="createForm.scene === 'company' ? 'background-color: var(--theme-primary); color: #fff;' : 'background-color: var(--theme-bg); color: var(--theme-text-secondary); border: 1px solid var(--theme-border);'"
              >公司挑战</button>
            </div>
          </div>
          <div v-if="createForm.scene === 'company'">
            <label class="block text-sm mb-1" style="color: var(--theme-text-secondary);">公司ID</label>
            <input
              v-model="createForm.companyId"
              type="number"
              placeholder="输入公司ID"
              class="w-full px-3 py-2 rounded-md text-sm"
              style="background-color: var(--theme-bg); border: 1px solid var(--theme-border); color: var(--theme-text);"
            />
          </div>
          <p v-if="createError" class="text-sm" style="color: #ef4444;">{{ createError }}</p>
        </div>
        <div class="flex gap-3 mt-6">
          <button @click="showCreate = false" class="flex-1 px-4 py-2 rounded-md text-sm" style="background-color: var(--theme-bg); color: var(--theme-text-secondary); border: 1px solid var(--theme-border);">取消</button>
          <button
            @click="doCreate"
            :disabled="creating"
            class="flex-1 px-4 py-2 rounded-md text-sm text-white flex items-center justify-center gap-2 disabled:opacity-60"
            style="background-color: var(--theme-primary);"
          >
            <Loader2 v-if="creating" class="w-4 h-4 animate-spin" />
            发起
          </button>
        </div>
      </div>
    </div>

    <!-- ============ 答题对战 抽屉 ============ -->
    <div v-if="playing" class="fixed inset-0 z-50 flex items-center justify-center p-4" style="background-color: rgba(0,0,0,0.5);" @click.self="closePlay">
      <div class="rounded-lg w-full max-w-2xl max-h-[90vh] overflow-y-auto p-6" style="background-color: var(--theme-surface); border: 1px solid var(--theme-border);">
        <!-- 头部：得分 -->
        <div v-if="playLoading" class="flex items-center justify-center py-12" style="color: var(--theme-text-secondary);">
          <Loader2 class="w-6 h-6 animate-spin" />
        </div>
        <template v-else>
          <div class="flex items-center justify-between mb-4">
            <h3 class="text-lg font-semibold flex items-center gap-2" style="color: var(--theme-text);">
              <Swords class="w-5 h-5" style="color: #ef4444;" />
              答题对战
            </h3>
            <button @click="closePlay" class="text-sm" style="color: var(--theme-text-secondary);">关闭</button>
          </div>

          <!-- 比分 -->
          <div class="rounded-lg p-4 mb-4 flex items-center justify-around" style="background-color: var(--theme-bg);">
            <div class="text-center">
              <div class="text-3xl font-bold" style="color: var(--theme-primary);">{{ myScore }}</div>
              <div class="text-xs mt-1" style="color: var(--theme-text-secondary);">我的得分</div>
            </div>
            <div class="text-xl" style="color: var(--theme-text-secondary);">VS</div>
            <div class="text-center">
              <div class="text-3xl font-bold" style="color: var(--theme-text);">{{ oppScore }}</div>
              <div class="text-xs mt-1" style="color: var(--theme-text-secondary);">对手得分</div>
            </div>
          </div>

          <!-- 结算横幅 -->
          <div v-if="isFinished" class="rounded-lg p-4 mb-4 flex items-center gap-3" :style="`background-color: ${(myIsWinner || isDraw) ? '#10b98122' : '#ef444422'};`">
            <Trophy class="w-6 h-6" :style="`color: ${myIsWinner ? '#10b981' : isDraw ? '#f59e0b' : '#ef4444'};`" />
            <div>
              <div class="font-semibold" style="color: var(--theme-text);">
                {{ myIsWinner ? '恭喜获胜！' : isDraw ? '平局' : '惜败' }}
              </div>
              <div class="text-xs" style="color: var(--theme-text-secondary);">共 {{ playing.questions?.length || 0 }} 题，已答 {{ answeredCount }} 题</div>
            </div>
          </div>

          <!-- 题目列表 -->
          <div v-if="playing.status === 'pending'" class="rounded-lg p-6 text-center text-sm" style="background-color: var(--theme-bg); color: var(--theme-text-secondary);">
            对战待应战，对手接受后即可开始答题。
          </div>
          <div v-else-if="playing.status === 'declined'" class="rounded-lg p-6 text-center text-sm" style="background-color: var(--theme-bg); color: var(--theme-text-secondary);">
            对手已拒绝挑战。
          </div>
          <div v-else class="space-y-4">
            <div
              v-for="(q, idx) in playing.questions || []"
              :key="q.id"
              class="rounded-lg p-4"
              style="background-color: var(--theme-bg);"
            >
              <div class="flex items-start gap-2 mb-3">
                <span class="text-sm font-medium flex-shrink-0" style="color: var(--theme-text-secondary);">Q{{ idx + 1 }}</span>
                <div class="flex-1">
                  <div class="text-sm font-medium" style="color: var(--theme-text);">{{ q.title }}</div>
                  <span
                    v-if="q.difficulty"
                    class="text-xs px-1.5 py-0.5 rounded"
                    :style="`color: ${difficultyColor(q.difficulty)}; background-color: ${difficultyColor(q.difficulty)}22;`"
                  >{{ q.difficulty }}</span>
                </div>
                <component
                  v-if="results[q.id]?.submitted"
                  :is="results[q.id]?.isSuccess ? CheckCircle2 : XCircle"
                  class="w-5 h-5 flex-shrink-0"
                  :style="`color: ${results[q.id]?.isSuccess ? '#10b981' : '#ef4444'};`"
                />
              </div>
              <textarea
                v-model="answers[q.id]"
                :disabled="results[q.id]?.submitted || isFinished"
                rows="3"
                placeholder="输入你的答案..."
                class="w-full px-3 py-2 rounded-md text-sm resize-y"
                style="background-color: var(--theme-surface); border: 1px solid var(--theme-border); color: var(--theme-text);"
              />
              <div class="flex justify-end mt-2">
                <button
                  v-if="!results[q.id]?.submitted && !isFinished"
                  @click="doSubmit(q.id)"
                  :disabled="submittingId === q.id"
                  class="px-4 py-1.5 rounded-md text-sm text-white flex items-center gap-1.5 disabled:opacity-60"
                  style="background-color: var(--theme-primary);"
                >
                  <Loader2 v-if="submittingId === q.id" class="w-4 h-4 animate-spin" />
                  <Send v-else class="w-4 h-4" />
                  提交
                </button>
                <span v-else-if="results[q.id]?.submitted" class="text-xs" :style="`color: ${results[q.id].isSuccess ? '#10b981' : '#ef4444'};`">
                  {{ results[q.id].isSuccess ? '已通过' : '未通过' }}
                </span>
              </div>
            </div>
          </div>
        </template>
      </div>
    </div>
  </div>
</template>
