<script setup lang="ts">
import { ref, computed, onMounted } from 'vue';
import { useRouter } from 'vue-router';
import { useHead } from '@vueuse/head';
import {
  BookOpen, Star, ArrowRight, Flame, Clock, CheckCircle2,
  TrendingUp, Gift, Calendar, ChevronRight
} from 'lucide-vue-next';
import SiteFooter from '@/components/SiteFooter.vue';
import LazyImage from '@/components/LazyImage.vue';

import { getDiscoverData, getRanking } from '@/api/reading';
import { generateSeo } from '@/utils/seo';
import type { Book, BookRecommend, DiscoverData, RankingType, RankingResult } from '@/types/api';

const router = useRouter();
const loading = ref(false);
const error = ref<string | null>(null);

// 发现页聚合数据
const banners = ref<BookRecommend[]>([]);
const hotRanking = ref<Book[]>([]);
const limitFree = ref<BookRecommend[]>([]);
const recentUpdate = ref<Book[]>([]);

// 排行榜 Tab
const rankingTabs: { key: RankingType; name: string; icon: typeof Flame }[] = [
  { key: 'hot', name: '热门榜', icon: Flame },
  { key: 'new', name: '新书榜', icon: TrendingUp },
  { key: 'completed', name: '完结榜', icon: CheckCircle2 },
  { key: 'word_count', name: '字数榜', icon: BookOpen },
];
const activeRankingTab = ref<RankingType>('hot');
const rankingList = ref<Book[]>([]);
const rankingLoading = ref(false);

const hasBanner = computed(() => banners.value.length > 0);
const currentBannerIndex = ref(0);
const currentBanner = computed(() => banners.value[currentBannerIndex.value] || null);

onMounted(() => {
  loadDiscoverData();
  loadRanking('hot');
});

async function loadDiscoverData() {
  try {
    loading.value = true;
    error.value = null;
    const response = await getDiscoverData();
    if (response.code === 200 && response.data) {
      const data = response.data as DiscoverData;
      banners.value = data.banners || [];
      hotRanking.value = data.hotRanking || [];
      limitFree.value = data.limitFree || [];
      recentUpdate.value = data.recentUpdate || [];
    } else {
      error.value = response.message || '加载发现页失败';
    }
  } catch (err) {
    console.error('加载发现页失败:', err);
    error.value = '加载数据失败，请稍后重试';
  } finally {
    loading.value = false;
  }
}

async function loadRanking(type: RankingType) {
  activeRankingTab.value = type;
  rankingLoading.value = true;
  try {
    const response = await getRanking(type, 10);
    if (response.code === 200 && response.data) {
      const result = response.data as RankingResult;
      rankingList.value = result.list || [];
    } else {
      rankingList.value = [];
    }
  } catch (err) {
    console.error('加载排行榜失败:', err);
    rankingList.value = [];
  } finally {
    rankingLoading.value = false;
  }
}

function switchBanner(idx: number) {
  currentBannerIndex.value = idx;
}

function formatWordCount(wordCount?: number): string {
  if (!wordCount) return '0';
  if (wordCount >= 10000) return (wordCount / 10000).toFixed(1) + '万字';
  return wordCount + '字';
}

function formatTime(time?: string): string {
  if (!time) return '';
  // 简化的相对时间展示：仅取日期
  return time.substring(0, 10);
}

useHead(
    computed(() => {
      return generateSeo({
        title: '发现好书',
        description: '墨韵智库发现页，热门排行、限免专区、最近更新，发现你的下一本好书'
      });
    })
);
</script>

<template>
  <div class="min-h-screen flex flex-col" style="background-color: var(--theme-bg);">
    <!-- 页面头部：精简品牌区（与下方区块对齐，避免重复文案） -->
    <div class="py-8 sm:py-10">
      <div class="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8">
        <div class="flex items-center justify-between flex-wrap gap-4">
          <div class="flex items-center gap-3">
            <div
                class="w-10 h-10 rounded-lg flex items-center justify-center"
                style="background-color: var(--theme-primary);"
            >
              <BookOpen class="w-5 h-5 text-white" />
            </div>
            <div>
              <h1 class="text-xl sm:text-2xl font-bold" style="color: var(--theme-text);">发现好书</h1>
              <p class="text-xs sm:text-sm" style="color: var(--theme-text-secondary);">为你精选，每周更新</p>
            </div>
          </div>
          <button
              @click="router.push('/reading')"
              class="inline-flex items-center gap-1 px-3 py-1.5 rounded-lg text-sm font-medium transition-colors hover:opacity-80"
              style="color: var(--theme-primary); background-color: var(--theme-accent);"
          >
            <span>返回读书空间</span>
            <ArrowRight class="w-4 h-4" />
          </button>
        </div>
      </div>
    </div>

    <!-- 主要内容 -->
    <div class="flex-1 pb-8">
      <div class="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8">
        <div v-if="loading" class="text-center py-12">
          <div class="animate-spin rounded-full h-12 w-12 border-b-2 mx-auto" style="border-bottom-color: var(--theme-primary);"></div>
          <p class="mt-4" style="color: var(--theme-text-secondary);">加载中...</p>
        </div>

        <div v-else-if="error" class="text-center py-12">
          <div class="bg-red-50 border border-red-200 rounded-lg p-6 max-w-md mx-auto">
            <p class="text-red-600">{{ error }}</p>
            <button
                @click="loadDiscoverData"
                class="mt-4 px-4 py-2 bg-red-500 text-white rounded-lg hover:bg-red-600 transition"
            >
              重试
            </button>
          </div>
        </div>

        <template v-else>
          <!-- Banner 轮播 -->
          <div v-if="hasBanner" class="mb-12">
            <div class="rounded-2xl overflow-hidden shadow-lg relative" style="background: linear-gradient(135deg, var(--theme-accent), color-mix(in srgb, var(--theme-accent) 50%, #c4b5fd));">
              <div
                  v-for="(banner, idx) in banners"
                  :key="banner.id"
                  v-show="idx === currentBannerIndex"
                  @click="banner.bookId && router.push(`/reading/book/${banner.bookId}`)"
                  class="cursor-pointer relative h-64 sm:h-80"
              >
                <div class="absolute inset-0 flex items-center justify-center p-8">
                  <div class="text-center text-white">
                    <BookOpen class="w-12 h-12 mx-auto mb-4 opacity-80" />
                    <h2 class="text-2xl sm:text-3xl font-bold mb-3">{{ banner.bookTitle || '精选好书' }}</h2>
                    <p class="text-white/80">点击查看详情</p>
                  </div>
                </div>
              </div>
              <!-- 轮播指示器 -->
              <div v-if="banners.length > 1" class="absolute bottom-4 left-1/2 -translate-x-1/2 flex space-x-2">
                <button
                    v-for="(banner, idx) in banners"
                    :key="banner.id"
                    @click.stop="switchBanner(idx)"
                    class="w-2.5 h-2.5 rounded-full transition-all"
                    :class="idx === currentBannerIndex ? 'bg-white w-8' : 'bg-white/50 hover:bg-white/80'"
                ></button>
              </div>
            </div>
          </div>

          <!-- 排行榜 Tab 区 -->
          <div class="mb-12">
            <div class="flex items-center justify-between mb-6">
              <h2 class="text-2xl font-bold flex items-center" style="color: var(--theme-text);">
                <TrendingUp class="w-6 h-6 mr-2" style="color: var(--theme-primary);" />
                排行榜
              </h2>
            </div>

            <!-- Tab 切换 -->
            <div class="flex flex-wrap gap-2 mb-6">
              <button
                  v-for="tab in rankingTabs"
                  :key="tab.key"
                  @click="loadRanking(tab.key)"
                  class="flex items-center px-4 py-2 rounded-lg font-medium transition-all"
                  :style="{
                  color: activeRankingTab === tab.key ? 'white' : 'var(--theme-text-secondary)',
                  backgroundColor: activeRankingTab === tab.key ? 'var(--theme-primary)' : 'var(--theme-surface)'
                }"
              >
                <component :is="tab.icon" class="w-4 h-4 mr-1.5" />
                {{ tab.name }}
              </button>
            </div>

            <!-- 排行榜列表 -->
            <div v-if="rankingLoading" class="text-center py-8">
              <div class="animate-spin rounded-full h-8 w-8 border-b-2 mx-auto" style="border-bottom-color: var(--theme-primary);"></div>
            </div>
            <div v-else-if="rankingList.length > 0" class="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-5 gap-6">
              <div
                  v-for="(book, index) in rankingList"
                  :key="book.id"
                  @click="router.push(`/reading/book/${book.id}`)"
                  class="group cursor-pointer"
              >
                <div class="flex items-start gap-3">
                  <div
                      class="flex-shrink-0 w-8 h-8 rounded-full flex items-center justify-center font-bold text-sm"
                      :style="{
                      backgroundColor: index < 3 ? 'var(--theme-primary)' : 'var(--theme-surface)',
                      color: index < 3 ? 'white' : 'var(--theme-text-secondary)'
                    }"
                  >
                    {{ index + 1 }}
                  </div>
                  <div class="flex-1 min-w-0">
                    <h3 class="font-medium text-sm mb-1 line-clamp-2 group-hover:opacity-80" style="color: var(--theme-text);">{{ book.title }}</h3>
                    <p class="text-xs mb-1" style="color: var(--theme-text-secondary);">{{ book.author }}</p>
                    <div class="flex items-center gap-3 text-xs" style="color: var(--theme-text-secondary);">
                      <span class="flex items-center">
                        <Star class="w-3 h-3 text-yellow-400 mr-0.5" />
                        {{ book.rating }}
                      </span>
                      <span v-if="book.wordCount">{{ formatWordCount(book.wordCount) }}</span>
                    </div>
                  </div>
                </div>
              </div>
            </div>
            <div v-else class="text-center py-8" style="color: var(--theme-text-secondary);">
              暂无排行数据
            </div>
          </div>

          <!-- 限免专区 -->
          <div v-if="limitFree.length > 0" class="mb-12">
            <div class="flex items-center justify-between mb-6">
              <h2 class="text-2xl font-bold flex items-center" style="color: var(--theme-text);">
                <Gift class="w-6 h-6 mr-2" style="color: var(--theme-primary);" />
                限免专区
              </h2>
            </div>
            <div class="grid grid-cols-2 md:grid-cols-3 lg:grid-cols-5 gap-6">
              <div
                  v-for="item in limitFree"
                  :key="item.id"
                  @click="item.bookId && router.push(`/reading/book/${item.bookId}`)"
                  class="group cursor-pointer"
              >
                <div class="aspect-[3/4] rounded-lg overflow-hidden shadow-sm mb-3 group-hover:shadow-md transition relative">
                  <div class="absolute top-2 left-2 z-10 px-2 py-0.5 rounded text-xs font-bold text-white" style="background-color: var(--theme-danger);">
                    限免
                  </div>
                  <LazyImage
                      :src="''"
                      :alt="item.bookTitle || ''"
                      class="w-full h-full object-cover group-hover:scale-105 transition"
                  />
                </div>
                <h3 class="font-medium text-sm mb-1 line-clamp-2 group-hover:opacity-80" style="color: var(--theme-text);">{{ item.bookTitle }}</h3>
                <p v-if="item.endTime" class="text-xs" style="color: var(--theme-text-secondary);">
                  截止：{{ formatTime(item.endTime) }}
                </p>
              </div>
            </div>
          </div>

          <!-- 最近更新 -->
          <div v-if="recentUpdate.length > 0" class="mb-12">
            <div class="flex items-center justify-between mb-6">
              <h2 class="text-2xl font-bold flex items-center" style="color: var(--theme-text);">
                <Calendar class="w-6 h-6 mr-2" style="color: var(--theme-primary);" />
                最近更新
              </h2>
            </div>
            <div class="grid grid-cols-2 md:grid-cols-3 lg:grid-cols-5 gap-6">
              <div
                  v-for="book in recentUpdate"
                  :key="book.id"
                  @click="router.push(`/reading/book/${book.id}`)"
                  class="group cursor-pointer"
              >
                <div class="aspect-[3/4] rounded-lg overflow-hidden shadow-sm mb-3 group-hover:shadow-md transition">
                  <LazyImage
                      :src="book.cover"
                      :alt="book.title"
                      class="w-full h-full object-cover group-hover:scale-105 transition"
                  />
                </div>
                <h3 class="font-medium text-sm mb-1 line-clamp-2 group-hover:opacity-80" style="color: var(--theme-text);">{{ book.title }}</h3>
                <p class="text-xs mb-1" style="color: var(--theme-text-secondary);">{{ book.author }}</p>
                <div class="flex items-center justify-between text-xs" style="color: var(--theme-text-secondary);">
                  <span v-if="book.latestChapterTitle" class="truncate mr-2">{{ book.latestChapterTitle }}</span>
                  <span v-if="book.lastUpdateTime">{{ formatTime(book.lastUpdateTime) }}</span>
                </div>
              </div>
            </div>
          </div>

          <!-- 空状态 -->
          <div
              v-if="!hasBanner && hotRanking.length === 0 && limitFree.length === 0 && recentUpdate.length === 0"
              class="text-center py-16"
          >
            <BookOpen class="w-16 h-16 mx-auto mb-4 opacity-30" style="color: var(--theme-text-secondary);" />
            <p style="color: var(--theme-text-secondary);">暂无发现内容，请稍后再来</p>
          </div>
        </template>
      </div>
    </div>

    <!-- 页脚 -->
    <SiteFooter />
  </div>
</template>
