<script setup lang="ts">
import { ref, computed, onMounted } from 'vue';
import { RouterLink as Link, useRoute, useRouter } from 'vue-router';
import { useHead } from '@vueuse/head';
import { Award, Lock, CheckCircle2, Sparkles, Trophy, BookOpen, BookMarked, Briefcase, Layers } from 'lucide-vue-next';
import Breadcrumb from '@/components/Breadcrumb.vue';
import { generateSeo } from '@/utils/seo';
import { getSafeAvatar } from '@/utils/avatar';
import { getMyAchievements, getUserAchievements, getMyGrowth, getUserGrowth } from '@/api/growth';
import { useUserStore } from '@/stores/user';
import type { AchievementVO, UserGrowthVO } from '@/types/api';

const route = useRoute();
const router = useRouter();
const userStore = useUserStore();

const isLoading = ref(false);
const achievements = ref<AchievementVO[]>([]);
const growth = ref<UserGrowthVO | null>(null);
const activeModule = ref<string>('all');

// 路由参数决定查看自己还是他人
const targetUserId = computed(() => {
  const id = route.query.userId as string | undefined;
  return id || null;
});
const isMyself = computed(() => !targetUserId.value);

// SEO
useHead(
  generateSeo({
    title: '成就徽章',
    description: '墨韵智库成就体系，记录每一次成长与突破。',
    keywords: ['成就', '徽章', '成长', '勋章'],
    type: 'website'
  })
);

// 模块筛选
const moduleOptions = [
  { value: 'all', label: '全部', icon: Layers },
  { value: 'article', label: '文章', icon: BookOpen },
  { value: 'reading', label: '读书', icon: BookMarked },
  { value: 'interview', label: '面试', icon: Briefcase },
];

const filteredAchievements = computed(() => {
  if (activeModule.value === 'all') return achievements.value;
  return achievements.value.filter(a => a.module === activeModule.value);
});

// 统计
const earnedCount = computed(() => achievements.value.filter(a => a.earned).length);
const totalCount = computed(() => achievements.value.length);
const earnedReward = computed(() => achievements.value.filter(a => a.earned).reduce((sum, a) => sum + (a.growthReward || 0), 0));

// 模块图标映射
const moduleIconMap: Record<string, any> = {
  article: BookOpen,
  reading: BookMarked,
  interview: Briefcase,
  all: Sparkles,
};

function getModuleIcon(module?: string) {
  return moduleIconMap[module || 'all'] || Award;
}

async function loadData() {
  isLoading.value = true;
  try {
    const [achResp, growthResp] = await Promise.all([
      targetUserId.value ? getUserAchievements(targetUserId.value) : getMyAchievements(),
      targetUserId.value ? getUserGrowth(targetUserId.value) : getMyGrowth(),
    ]);
    if (achResp.code === 200 && achResp.data) {
      achievements.value = achResp.data;
    }
    if (growthResp.code === 200 && growthResp.data) {
      growth.value = growthResp.data;
    }
  } catch (error) {
    console.error('加载成就数据失败:', error);
  } finally {
    isLoading.value = false;
  }
}

onMounted(() => {
  loadData();
});

// 监听路由参数变化
import { watch } from 'vue';
watch(targetUserId, () => {
  loadData();
});
</script>

<template>
  <div class="min-h-screen flex flex-col" style="background-color: var(--theme-bg);">
    <!-- 面包屑 -->
    <div class="border-b py-3 sm:py-4" style="background-color: var(--theme-bg); border-color: var(--theme-border);">
      <div class="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8">
        <Breadcrumb :items="[{ label: '首页', path: '/' }, { label: '成就徽章' }]" />
      </div>
    </div>

    <!-- 主内容 -->
    <div class="flex-1 max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 py-6 sm:py-8 w-full">
      <!-- 页面标题 -->
      <div class="mb-6 sm:mb-8">
        <div class="flex items-center gap-3 mb-2">
          <div class="w-10 h-10 rounded-xl flex items-center justify-center" style="background: linear-gradient(135deg, #8b5cf6 0%, #ec4899 100%);">
            <Award class="w-6 h-6 text-white" />
          </div>
          <h1 class="text-2xl sm:text-3xl font-bold" style="color: var(--theme-text);">
            {{ isMyself ? '我的成就' : 'TA的成就' }}
          </h1>
        </div>
        <p class="text-sm sm:text-base" style="color: var(--theme-text-secondary);">
          每一枚徽章都记录着成长的足迹
        </p>
      </div>

      <!-- 成长概览卡片 -->
      <div v-if="growth" class="mb-6 sm:mb-8 rounded-2xl p-5 sm:p-6" style="background: linear-gradient(135deg, #8b5cf6 0%, #ec4899 100%);">
        <div class="grid grid-cols-2 sm:grid-cols-4 gap-4 sm:gap-6">
          <div class="text-center">
            <p class="text-white/80 text-xs sm:text-sm mb-1">当前等级</p>
            <p class="text-white text-xl sm:text-2xl font-bold">Lv.{{ growth.level || 1 }}</p>
          </div>
          <div class="text-center">
            <p class="text-white/80 text-xs sm:text-sm mb-1">头衔</p>
            <p class="text-white text-xl sm:text-2xl font-bold truncate">{{ growth.title || '—' }}</p>
          </div>
          <div class="text-center">
            <p class="text-white/80 text-xs sm:text-sm mb-1">累计成长值</p>
            <p class="text-white text-xl sm:text-2xl font-bold">{{ growth.growthValue || 0 }}</p>
          </div>
          <div class="text-center">
            <p class="text-white/80 text-xs sm:text-sm mb-1">本季排名</p>
            <p class="text-white text-xl sm:text-2xl font-bold">第 {{ growth.seasonRank || '—' }} 名</p>
          </div>
        </div>
      </div>

      <!-- 进度统计 -->
      <div class="mb-6 sm:mb-8 grid grid-cols-1 sm:grid-cols-3 gap-4">
        <div class="rounded-xl p-4 sm:p-5" style="background-color: var(--theme-surface); border: 1px solid var(--theme-border);">
          <div class="flex items-center gap-3">
            <div class="w-10 h-10 rounded-lg flex items-center justify-center" style="background-color: var(--theme-accent);">
              <Trophy class="w-5 h-5" style="color: var(--theme-primary);" />
            </div>
            <div>
              <p class="text-xs sm:text-sm" style="color: var(--theme-text-secondary);">已达成</p>
              <p class="text-lg sm:text-xl font-bold" style="color: var(--theme-text);">
                {{ earnedCount }} / {{ totalCount }}
              </p>
            </div>
          </div>
        </div>
        <div class="rounded-xl p-4 sm:p-5" style="background-color: var(--theme-surface); border: 1px solid var(--theme-border);">
          <div class="flex items-center gap-3">
            <div class="w-10 h-10 rounded-lg flex items-center justify-center" style="background-color: var(--theme-accent);">
              <CheckCircle2 class="w-5 h-5" style="color: #10b981;" />
            </div>
            <div>
              <p class="text-xs sm:text-sm" style="color: var(--theme-text-secondary);">完成率</p>
              <p class="text-lg sm:text-xl font-bold" style="color: var(--theme-text);">
                {{ totalCount > 0 ? Math.round((earnedCount / totalCount) * 100) : 0 }}%
              </p>
            </div>
          </div>
        </div>
        <div class="rounded-xl p-4 sm:p-5" style="background-color: var(--theme-surface); border: 1px solid var(--theme-border);">
          <div class="flex items-center gap-3">
            <div class="w-10 h-10 rounded-lg flex items-center justify-center" style="background-color: var(--theme-accent);">
              <Sparkles class="w-5 h-5" style="color: #f59e0b;" />
            </div>
            <div>
              <p class="text-xs sm:text-sm" style="color: var(--theme-text-secondary);">奖励成长值</p>
              <p class="text-lg sm:text-xl font-bold" style="color: var(--theme-text);">+{{ earnedReward }}</p>
            </div>
          </div>
        </div>
      </div>

      <!-- 模块筛选 -->
      <div class="mb-6 flex gap-2 overflow-x-auto pb-2">
        <button
          v-for="opt in moduleOptions"
          :key="opt.value"
          @click="activeModule = opt.value"
          class="flex items-center gap-2 px-4 py-2 rounded-xl text-sm font-medium whitespace-nowrap transition-colors"
          :style="activeModule === opt.value
            ? 'background-color: var(--theme-primary); color: white;'
            : 'background-color: var(--theme-surface); border: 1px solid var(--theme-border); color: var(--theme-text-secondary);'"
        >
          <component :is="opt.icon" class="w-4 h-4" />
          {{ opt.label }}
        </button>
      </div>

      <!-- 加载状态 -->
      <div v-if="isLoading" class="text-center py-12">
        <div class="inline-block w-10 h-10 border-4 border-t-4 border-gray-300 rounded-full animate-spin" style="border-top-color: var(--theme-primary);"></div>
        <p class="mt-4" style="color: var(--theme-text-secondary);">加载中...</p>
      </div>

      <!-- 成就网格 -->
      <div v-else-if="filteredAchievements.length > 0" class="grid grid-cols-2 sm:grid-cols-3 lg:grid-cols-4 gap-4 sm:gap-6">
        <div
          v-for="ach in filteredAchievements"
          :key="ach.id"
          class="rounded-2xl p-4 sm:p-5 text-center transition-all"
          :style="{
            backgroundColor: 'var(--theme-surface)',
            border: ach.earned ? '1px solid var(--theme-primary)' : '1px solid var(--theme-border)',
            opacity: ach.earned ? 1 : 0.6
          }"
        >
          <!-- 徽章图标 -->
          <div class="relative inline-block mb-3">
            <div
              class="w-16 h-16 sm:w-20 sm:h-20 mx-auto rounded-full flex items-center justify-center"
              :style="ach.earned
                ? 'background: linear-gradient(135deg, #f59e0b 0%, #ef4444 100%);'
                : 'background-color: var(--theme-accent);'"
            >
              <img
                v-if="ach.icon"
                :src="ach.icon"
                :alt="ach.name"
                class="w-8 h-8 sm:w-10 sm:h-10 object-contain"
                @error="(e: Event) => (e.target as HTMLImageElement).style.display = 'none'"
              />
              <component
                v-else
                :is="ach.earned ? Award : Lock"
                class="w-8 h-8 sm:w-10 sm:h-10"
                :class="ach.earned ? 'text-white' : ''"
                :style="!ach.earned ? 'color: var(--theme-text-secondary);' : ''"
              />
            </div>
            <!-- 达成标记 -->
            <div
              v-if="ach.earned"
              class="absolute -bottom-1 -right-1 w-6 h-6 rounded-full flex items-center justify-center"
              style="background-color: #10b981;"
            >
              <CheckCircle2 class="w-4 h-4 text-white" />
            </div>
          </div>

          <!-- 名称 -->
          <h3 class="font-bold text-sm sm:text-base mb-1 truncate" style="color: var(--theme-text);">
            {{ ach.name }}
          </h3>

          <!-- 描述 -->
          <p class="text-xs sm:text-sm mb-2 line-clamp-2" style="color: var(--theme-text-secondary); min-height: 2.5em;">
            {{ ach.description || '神秘成就' }}
          </p>

          <!-- 奖励 -->
          <div class="inline-flex items-center gap-1 px-2 py-0.5 rounded-full text-xs" style="background-color: var(--theme-accent); color: var(--theme-text-secondary);">
            <Sparkles class="w-3 h-3" />
            +{{ ach.growthReward || 0 }}
          </div>

          <!-- 达成时间 -->
          <p v-if="ach.earned && ach.earnedTime" class="mt-2 text-xs" style="color: var(--theme-text-secondary);">
            {{ ach.earnedTime }} 达成
          </p>
          <p v-else class="mt-2 text-xs" style="color: var(--theme-text-secondary);">未达成</p>
        </div>
      </div>

      <!-- 空状态 -->
      <div v-else class="p-8 sm:p-12 rounded-2xl text-center" style="background-color: var(--theme-surface); border: 1px solid var(--theme-border);">
        <Award class="w-12 h-12 sm:w-16 sm:h-16 mx-auto mb-4" style="color: var(--theme-text-secondary);" />
        <h3 class="text-lg font-medium mb-2" style="color: var(--theme-text);">暂无成就数据</h3>
        <p style="color: var(--theme-text-secondary);">成就体系正在筹备中，敬请期待</p>
      </div>

      <!-- 查看排行榜入口 -->
      <div v-if="isMyself" class="mt-8 text-center">
        <Link to="/ranking" class="inline-flex items-center gap-2 px-5 py-2.5 rounded-xl font-medium text-sm transition-colors" style="background-color: var(--theme-surface); border: 1px solid var(--theme-border); color: var(--theme-text);">
          <Trophy class="w-4 h-4" style="color: #f59e0b;" />
          查看成长排行榜
        </Link>
      </div>
    </div>
  </div>
</template>
