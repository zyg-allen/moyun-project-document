<script setup lang="ts">
import { ref, computed, onMounted } from 'vue';
import { RouterLink as Link, useRouter } from 'vue-router';
import { useHead } from '@vueuse/head';
import { Trophy, Crown, Medal, Award, TrendingUp, Sparkles, ChevronRight } from 'lucide-vue-next';
import Breadcrumb from '@/components/Breadcrumb.vue';
import { generateSeo } from '@/utils/seo';
import { getSafeAvatar } from '@/utils/avatar';
import { getGrowthRanking, getMyGrowth } from '@/api/growth';
import { useUserStore } from '@/stores/user';
import type { GrowthRankingItem, UserGrowthVO } from '@/types/api';

const router = useRouter();
const userStore = useUserStore();

const isLoading = ref(false);
const ranking = ref<GrowthRankingItem[]>([]);
const myGrowth = ref<UserGrowthVO | null>(null);
const limit = ref(50);

// SEO
useHead(
  generateSeo({
    title: '成长排行榜',
    description: '墨韵智库用户成长值排行榜，看看本季最活跃的创作者。',
    keywords: ['排行榜', '成长', '等级', '创作者'],
    type: 'website'
  })
);

// 前三名
const topThree = computed(() => ranking.value.slice(0, 3));
// 第4名及以后
const restList = computed(() => ranking.value.slice(3));

// 排名样式
function getRankStyle(rank: number) {
  switch (rank) {
    case 1:
      return {
        bg: 'from-yellow-400 to-amber-500',
        icon: Crown,
        label: '冠军',
        ring: 'ring-yellow-400'
      };
    case 2:
      return {
        bg: 'from-gray-300 to-gray-400',
        icon: Medal,
        label: '亚军',
        ring: 'ring-gray-400'
      };
    case 3:
      return {
        bg: 'from-orange-400 to-orange-600',
        icon: Award,
        label: '季军',
        ring: 'ring-orange-400'
      };
    default:
      return null;
  }
}

async function loadRanking() {
  isLoading.value = true;
  try {
    const response = await getGrowthRanking(limit.value);
    if (response.code === 200 && response.data) {
      ranking.value = response.data as GrowthRankingItem[];
    }
  } catch (error) {
    console.error('加载排行榜失败:', error);
  } finally {
    isLoading.value = false;
  }
}

async function loadMyGrowth() {
  if (!userStore.isAuthenticated) return;
  try {
    const response = await getMyGrowth();
    if (response.code === 200 && response.data) {
      myGrowth.value = response.data;
    }
  } catch (error) {
    console.warn('获取我的成长信息失败:', error);
  }
}

function goToAuthor(userId: string | number) {
  router.push(`/author/${userId}`);
}

onMounted(() => {
  loadRanking();
  loadMyGrowth();
});
</script>

<template>
  <div class="min-h-screen flex flex-col" style="background-color: var(--theme-bg);">
    <!-- 面包屑 -->
    <div class="border-b py-3 sm:py-4" style="background-color: var(--theme-bg); border-color: var(--theme-border);">
      <div class="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8">
        <Breadcrumb :items="[{ label: '首页', path: '/' }, { label: '成长排行榜' }]" />
      </div>
    </div>

    <!-- 主内容 -->
    <div class="flex-1 max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 py-6 sm:py-8 w-full">
      <!-- 页面标题 -->
      <div class="mb-6 sm:mb-8">
        <div class="flex items-center gap-3 mb-2">
          <div class="w-10 h-10 rounded-xl flex items-center justify-center" style="background: linear-gradient(135deg, #f59e0b 0%, #ef4444 100%);">
            <Trophy class="w-6 h-6 text-white" />
          </div>
          <h1 class="text-2xl sm:text-3xl font-bold" style="color: var(--theme-text);">成长排行榜</h1>
        </div>
        <p class="text-sm sm:text-base" style="color: var(--theme-text-secondary);">
          本季成长值排名前 {{ limit }} 的创作者，持续创作即可上榜
        </p>
      </div>

      <!-- 我的排名卡片 -->
      <div v-if="myGrowth" class="mb-6 sm:mb-8 rounded-2xl p-5 sm:p-6" style="background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);">
        <div class="flex items-center justify-between flex-wrap gap-4">
          <div class="flex items-center gap-3">
            <Sparkles class="w-6 h-6 text-white" />
            <div>
              <p class="text-white/80 text-xs sm:text-sm">我的本季排名</p>
              <p class="text-white text-xl sm:text-2xl font-bold">第 {{ myGrowth.seasonRank || '—' }} 名</p>
            </div>
          </div>
          <div class="flex items-center gap-6 sm:gap-8">
            <div class="text-center">
              <p class="text-white/80 text-xs">本季成长</p>
              <p class="text-white text-lg sm:text-xl font-bold">{{ myGrowth.seasonValue || 0 }}</p>
            </div>
            <div class="text-center">
              <p class="text-white/80 text-xs">累计成长</p>
              <p class="text-white text-lg sm:text-xl font-bold">{{ myGrowth.growthValue || 0 }}</p>
            </div>
            <div class="text-center">
              <p class="text-white/80 text-xs">等级</p>
              <p class="text-white text-lg sm:text-xl font-bold">Lv.{{ myGrowth.level || 1 }}</p>
            </div>
            <div class="hidden sm:block text-center">
              <p class="text-white/80 text-xs">头衔</p>
              <p class="text-white text-lg sm:text-xl font-bold">{{ myGrowth.title || '—' }}</p>
            </div>
          </div>
        </div>
      </div>

      <!-- 加载状态 -->
      <div v-if="isLoading" class="text-center py-12">
        <div class="inline-block w-10 h-10 border-4 border-t-4 border-gray-300 rounded-full animate-spin" style="border-top-color: var(--theme-primary);"></div>
        <p class="mt-4" style="color: var(--theme-text-secondary);">加载中...</p>
      </div>

      <template v-else>
        <!-- 前三名展示 -->
        <div v-if="topThree.length > 0" class="mb-6 sm:mb-8 grid grid-cols-1 sm:grid-cols-3 gap-4 sm:gap-6">
          <div
            v-for="item in topThree"
            :key="item.userId"
            class="rounded-2xl p-5 sm:p-6 text-center cursor-pointer transition-all hover:-translate-y-1"
            :class="item.seasonRank === 1 ? 'sm:mt-0' : 'sm:mt-8'"
            style="background-color: var(--theme-surface); border: 1px solid var(--theme-border);"
            @click="goToAuthor(item.userId)"
          >
            <div class="relative inline-block mb-3">
              <img
                :src="getSafeAvatar(item.avatar, String(item.userId))"
                :alt="item.nickname || `用户${item.userId}`"
                class="w-20 h-20 sm:w-24 sm:h-24 rounded-full object-cover ring-4"
                :class="getRankStyle(item.seasonRank || 0)?.ring"
                @error="(e: Event) => (e.target as HTMLImageElement).src = getSafeAvatar(null, String(item.userId))"
              />
              <div
                class="absolute -top-2 -right-2 w-8 h-8 rounded-full flex items-center justify-center text-white shadow-lg"
                :class="`bg-gradient-to-br ${getRankStyle(item.seasonRank || 0)?.bg}`"
              >
                <component :is="getRankStyle(item.seasonRank || 0)?.icon" class="w-5 h-5" />
              </div>
            </div>
            <h3 class="font-bold text-base sm:text-lg mb-1 truncate" style="color: var(--theme-text);">
              {{ item.nickname || `用户${item.userId}` }}
            </h3>
            <p class="text-xs sm:text-sm mb-3" style="color: var(--theme-text-secondary);">
              {{ getRankStyle(item.seasonRank || 0)?.label }} · Lv.{{ item.level }}
            </p>
            <div class="inline-flex items-center gap-1 px-3 py-1 rounded-full text-xs sm:text-sm" style="background-color: var(--theme-accent); color: var(--theme-text);">
              <TrendingUp class="w-3 h-3 sm:w-4 sm:h-4" />
              {{ item.seasonValue || 0 }} 成长值
            </div>
            <p v-if="item.title" class="mt-2 text-xs" style="color: var(--theme-text-secondary);">{{ item.title }}</p>
          </div>
        </div>

        <!-- 第4名及以后列表 -->
        <div v-if="restList.length > 0" class="rounded-2xl overflow-hidden" style="background-color: var(--theme-surface); border: 1px solid var(--theme-border);">
          <div
            v-for="item in restList"
            :key="item.userId"
            class="flex items-center gap-3 sm:gap-4 p-4 sm:p-5 cursor-pointer transition-colors hover:bg-opacity-50"
            style="border-bottom: 1px solid var(--theme-border);"
            @click="goToAuthor(item.userId)"
          >
            <!-- 排名 -->
            <div class="w-10 h-10 sm:w-12 sm:h-12 rounded-xl flex items-center justify-center font-bold text-sm sm:text-base flex-shrink-0" style="background-color: var(--theme-accent); color: var(--theme-text-secondary);">
              {{ item.seasonRank }}
            </div>

            <!-- 头像 -->
            <img
              :src="getSafeAvatar(item.avatar, String(item.userId))"
              :alt="item.nickname || `用户${item.userId}`"
              class="w-10 h-10 sm:w-12 sm:h-12 rounded-full object-cover flex-shrink-0"
              @error="(e: Event) => (e.target as HTMLImageElement).src = getSafeAvatar(null, String(item.userId))"
            />

            <!-- 信息 -->
            <div class="flex-1 min-w-0">
              <div class="flex items-center gap-2 mb-1">
                <h4 class="font-medium text-sm sm:text-base truncate" style="color: var(--theme-text);">
                  {{ item.nickname || `用户${item.userId}` }}
                </h4>
                <span class="text-xs px-2 py-0.5 rounded-full flex-shrink-0" style="background-color: var(--theme-accent); color: var(--theme-text-secondary);">
                  Lv.{{ item.level }}
                </span>
              </div>
              <p class="text-xs sm:text-sm truncate" style="color: var(--theme-text-secondary);">
                {{ item.title || '—' }}
              </p>
            </div>

            <!-- 成长值 -->
            <div class="text-right flex-shrink-0">
              <div class="flex items-center gap-1 text-sm sm:text-base font-bold" style="color: var(--theme-primary);">
                <TrendingUp class="w-3 h-3 sm:w-4 sm:h-4" />
                {{ item.seasonValue || 0 }}
              </div>
              <p class="text-xs" style="color: var(--theme-text-secondary);">本季成长</p>
            </div>

            <ChevronRight class="w-4 h-4 sm:w-5 sm:h-5 flex-shrink-0" style="color: var(--theme-text-secondary);" />
          </div>
        </div>

        <!-- 空状态 -->
        <div v-if="!isLoading && ranking.length === 0" class="p-8 sm:p-12 rounded-2xl text-center" style="background-color: var(--theme-surface); border: 1px solid var(--theme-border);">
          <Trophy class="w-12 h-12 sm:w-16 sm:h-16 mx-auto mb-4" style="color: var(--theme-text-secondary);" />
          <h3 class="text-lg font-medium mb-2" style="color: var(--theme-text);">暂无排行数据</h3>
          <p style="color: var(--theme-text-secondary);">成为第一个上榜的创作者吧</p>
          <Link to="/publish" class="inline-flex items-center gap-2 mt-4 px-5 py-2.5 rounded-xl font-medium text-sm" style="background-color: var(--theme-primary); color: white;">
            开始创作
            <ChevronRight class="w-4 h-4" />
          </Link>
        </div>
      </template>
    </div>
  </div>
</template>
