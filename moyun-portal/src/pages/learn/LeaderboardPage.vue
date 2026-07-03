<script setup lang="ts">
import { ref, computed, onMounted, watch } from 'vue';
import { useHead } from '@vueuse/head';
import { Trophy, Crown, Medal, Award, Loader2, ChevronLeft, ListChecks, Star, Swords } from 'lucide-vue-next';
import SiteFooter from '@/components/SiteFooter.vue';
import { generateSeo } from '@/utils/seo';
import { getSafeAvatar } from '@/utils/avatar';
import { getLeaderboard } from '@/api/learnStats';
import type { Leaderboard, LeaderboardType } from '@/api/learnStats';

useHead(computed(() => generateSeo({
  title: '刷题排行榜',
  description: '墨韵智库刷题排行榜 - 通过题目数与刷题积分双榜，看看谁在领跑。',
  canonicalPath: '/learn/leaderboard',
})));

// ==================== 状态 ====================
const loading = ref(true);
const error = ref<string | null>(null);
const data = ref<Leaderboard | null>(null);
const activeType = ref<LeaderboardType>('question');

const tabs: { type: LeaderboardType; label: string; icon: typeof ListChecks; unit: string }[] = [
  { type: 'question', label: '通过题目数', icon: ListChecks, unit: '题' },
  { type: 'score', label: '刷题积分', icon: Star, unit: '分' },
];

async function loadLeaderboard() {
  loading.value = true;
  error.value = null;
  try {
    const res = await getLeaderboard(activeType.value, 100);
    if (res.code === 200) {
      data.value = res.data;
    } else {
      error.value = res.message || '加载排行榜失败';
    }
  } catch (err) {
    const e = err as { message?: string };
    error.value = e?.message || '加载排行榜失败，请稍后重试';
  } finally {
    loading.value = false;
  }
}

watch(activeType, loadLeaderboard);
onMounted(loadLeaderboard);

// ==================== 视图派生 ====================
const list = computed(() => data.value?.list || []);

const topThree = computed(() => list.value.slice(0, 3));
const restList = computed(() => list.value.slice(3));

function rankStyle(rank: number) {
  switch (rank) {
    case 1:
      return { bg: 'from-yellow-400 to-amber-500', icon: Crown, ring: 'ring-yellow-400', label: '冠军' };
    case 2:
      return { bg: 'from-gray-300 to-gray-400', icon: Medal, ring: 'ring-gray-400', label: '亚军' };
    case 3:
      return { bg: 'from-orange-400 to-orange-600', icon: Award, ring: 'ring-orange-400', label: '季军' };
    default:
      return null;
  }
}

const myInfo = computed(() => {
  if (!data.value) return null;
  if (data.value.myRank == null && data.value.myValue == null) return null;
  return {
    rank: data.value.myRank,
    value: data.value.myValue ?? 0,
    submitCount: data.value.mySubmitCount ?? 0,
    passedCount: data.value.myPassedCount ?? 0,
    score: data.value.myScore ?? 0,
  };
});

const currentUnit = computed(() => tabs.find((t) => t.type === activeType.value)?.unit || '');
</script>

<template>
  <div class="min-h-screen flex flex-col" style="background-color: var(--theme-bg);">
    <!-- 顶部条 -->
    <div class="border-b" style="background-color: var(--theme-surface); border-color: var(--theme-border);">
      <div class="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8">
        <div class="flex items-center gap-3 h-16">
          <router-link to="/learn" class="p-2 rounded-lg transition-colors hover:bg-gray-100" style="color: var(--theme-text-secondary);">
            <ChevronLeft class="w-5 h-5" />
          </router-link>
          <h1 class="text-lg sm:text-xl font-semibold flex items-center gap-2" style="color: var(--theme-text);">
            <Trophy class="w-5 h-5" style="color: #f59e0b;" />
            刷题排行榜
          </h1>
          <div class="ml-auto flex items-center gap-2">
            <router-link
              to="/learn/pk"
              class="flex items-center gap-1 px-3 py-1.5 rounded-md text-sm font-medium text-white transition-opacity hover:opacity-90"
              style="background-color: var(--theme-primary);"
            >
              <Swords class="w-4 h-4" />
              <span class="hidden sm:inline">PK 对战</span>
            </router-link>
            <!-- Tab 切换 -->
            <div class="flex gap-1 p-1 rounded-lg" style="background-color: var(--theme-bg);">
              <button
                v-for="t in tabs"
                :key="t.type"
                @click="activeType = t.type"
                class="flex items-center gap-1 px-3 py-1.5 rounded-md text-sm transition-colors"
                :style="activeType === t.type
                  ? 'background-color: var(--theme-surface); color: var(--theme-primary); font-weight: 600;'
                  : 'color: var(--theme-text-secondary);'"
              >
                <component :is="t.icon" class="w-4 h-4" />
                <span class="hidden sm:inline">{{ t.label }}</span>
              </button>
            </div>
          </div>
        </div>
      </div>
    </div>

    <!-- 主内容 -->
    <div class="flex-1 py-8">
      <div class="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 space-y-6">
        <!-- 加载中 -->
        <div v-if="loading" class="flex flex-col items-center justify-center py-20" style="color: var(--theme-text-secondary);">
          <Loader2 class="w-8 h-8 animate-spin mb-3" />
          <span>正在加载排行榜...</span>
        </div>

        <!-- 错误 -->
        <div v-else-if="error" class="rounded-lg p-6 text-center" style="background-color: var(--theme-surface); border: 1px solid var(--theme-border); color: var(--theme-text-secondary);">
          {{ error }}
          <button @click="loadLeaderboard" class="ml-3 underline">重试</button>
        </div>

        <template v-else>
          <!-- 我的排名卡片 -->
          <section v-if="myInfo" class="rounded-lg p-4 sm:p-6 flex flex-wrap items-center gap-4" style="background: linear-gradient(135deg, var(--theme-surface) 0%, var(--theme-bg) 100%); border: 1px solid var(--theme-border);">
            <div class="flex items-center justify-center w-16 h-16 rounded-full" style="background: linear-gradient(135deg, #f59e0b 0%, #ef4444 100%);">
              <span class="text-2xl font-bold text-white">#{{ myInfo.rank ?? '—' }}</span>
            </div>
            <div class="flex-1 min-w-[180px]">
              <div class="text-sm" style="color: var(--theme-text-secondary);">我的排名</div>
              <div class="text-xl font-bold mt-0.5" style="color: var(--theme-text);">
                {{ activeType === 'score' ? myInfo.score : myInfo.passedCount }} {{ currentUnit }}
              </div>
            </div>
            <div class="flex gap-4 text-sm" style="color: var(--theme-text-secondary);">
              <div class="text-center">
                <div class="text-lg font-semibold" style="color: var(--theme-text);">{{ myInfo.submitCount }}</div>
                <div class="text-xs">提交</div>
              </div>
              <div class="text-center">
                <div class="text-lg font-semibold" style="color: #10b981;">{{ myInfo.passedCount }}</div>
                <div class="text-xs">通过题数</div>
              </div>
              <div class="text-center">
                <div class="text-lg font-semibold" style="color: #3b82f6;">{{ myInfo.score }}</div>
                <div class="text-xs">积分</div>
              </div>
            </div>
          </section>
          <section v-else class="rounded-lg p-4 text-sm text-center" style="background-color: var(--theme-surface); border: 1px solid var(--theme-border); color: var(--theme-text-secondary);">
            <router-link to="/interview/questions" class="underline" style="color: var(--theme-primary);">去刷第一道题</router-link>
            ，登上排行榜吧。
          </section>

          <!-- 前三名 -->
          <section v-if="topThree.length" class="grid grid-cols-1 sm:grid-cols-3 gap-4">
            <div
              v-for="item in topThree"
              :key="`top-${item.userId}`"
              class="rounded-lg p-5 flex flex-col items-center text-center relative"
              :class="`bg-gradient-to-b ${rankStyle(item.rank)?.bg} bg-opacity-10`"
              style="background-color: var(--theme-surface); border: 1px solid var(--theme-border);"
            >
              <div class="absolute top-3 right-3 text-xs px-2 py-0.5 rounded-full" style="background-color: var(--theme-bg); color: var(--theme-text-secondary);">
                {{ rankStyle(item.rank)?.label }}
              </div>
              <div class="w-20 h-20 rounded-full ring-4 overflow-hidden mb-3" :class="`ring-${rankStyle(item.rank)?.ring}`">
                <img :src="getSafeAvatar(item.avatar, String(item.userId))" :alt="item.nickname" class="w-full h-full object-cover" />
              </div>
              <div class="font-semibold truncate max-w-full" style="color: var(--theme-text);">{{ item.nickname }}</div>
              <div class="mt-1 text-2xl font-bold" style="color: var(--theme-primary);">
                {{ item.value }}<span class="text-xs ml-0.5" style="color: var(--theme-text-secondary);">{{ currentUnit }}</span>
              </div>
              <div class="mt-1 text-xs" style="color: var(--theme-text-secondary);">
                通过 {{ item.passedCount }} · 提交 {{ item.submitCount }}
              </div>
            </div>
          </section>

          <!-- 第 4 名及以后 -->
          <section v-if="restList.length" class="rounded-lg overflow-hidden" style="background-color: var(--theme-surface); border: 1px solid var(--theme-border);">
            <table class="w-full text-sm">
              <thead>
                <tr style="background-color: var(--theme-bg); color: var(--theme-text-secondary);">
                  <th class="text-left px-4 py-3 font-medium w-16">名次</th>
                  <th class="text-left px-4 py-3 font-medium">用户</th>
                  <th class="text-right px-4 py-3 font-medium">{{ activeType === 'score' ? '积分' : '通过数' }}</th>
                  <th class="text-right px-4 py-3 font-medium hidden sm:table-cell">提交</th>
                  <th class="text-right px-4 py-3 font-medium hidden sm:table-cell">通过题数</th>
                </tr>
              </thead>
              <tbody>
                <tr
                  v-for="item in restList"
                  :key="`row-${item.userId}`"
                  class="border-t transition-colors hover:bg-opacity-50"
                  style="border-color: var(--theme-border);"
                >
                  <td class="px-4 py-3" style="color: var(--theme-text-secondary);">{{ item.rank }}</td>
                  <td class="px-4 py-3">
                    <router-link :to="`/author/${item.userId}`" class="flex items-center gap-2 group">
                      <img :src="getSafeAvatar(item.avatar, String(item.userId))" :alt="item.nickname" class="w-7 h-7 rounded-full object-cover flex-shrink-0" />
                      <span class="truncate group-hover:underline" style="color: var(--theme-text);">{{ item.nickname }}</span>
                    </router-link>
                  </td>
                  <td class="px-4 py-3 text-right font-semibold" style="color: var(--theme-primary);">{{ item.value }}</td>
                  <td class="px-4 py-3 text-right hidden sm:table-cell" style="color: var(--theme-text-secondary);">{{ item.submitCount }}</td>
                  <td class="px-4 py-3 text-right hidden sm:table-cell" style="color: var(--theme-text-secondary);">{{ item.passedCount }}</td>
                </tr>
              </tbody>
            </table>
          </section>

          <!-- 空状态 -->
          <section v-if="!loading && list.length === 0" class="rounded-lg p-8 text-center" style="background-color: var(--theme-surface); border: 1px solid var(--theme-border);">
            <p class="text-sm" style="color: var(--theme-text-secondary);">暂无排行数据，成为第一个上榜的人吧。</p>
          </section>
        </template>
      </div>
    </div>

    <SiteFooter />
  </div>
</template>
