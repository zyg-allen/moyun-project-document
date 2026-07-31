<script setup lang="ts">
import { ref, computed, onMounted, watch } from 'vue';
import { useRoute, useRouter } from 'vue-router';
import { useHead } from '@vueuse/head';
import { Search as SearchIcon, TrendingUp, Flame, PenLine, ArrowRight, Eye, Megaphone } from 'lucide-vue-next';
import { RouterLink as Link } from 'vue-router';
import ArticleCard from '@/components/ArticleCard.vue';
import Pagination from '@/components/Pagination.vue';
import Breadcrumb from '@/components/Breadcrumb.vue';
import SiteFooter from '@/components/SiteFooter.vue';

import * as articleApi from '@/api/article';
import * as tagApi from '@/api/tag';
import { generateSeo } from '@/utils/seo';
import { transformArticle } from '@/utils/articleTransform';
import { useAuth } from '@/composables/useAuth';
import type { Article } from '@/types';

const route = useRoute();
const router = useRouter();
const { requireAuth } = useAuth();
const searchQuery = ref('');
const selectedCategory = ref('');
const sortBy = ref('最新');
// 服务端分页：只拉当前页 10 条，避免一次性拉 100 条导致首屏卡顿
const allArticles = ref<Article[]>([]);
const currentPage = ref(1);
const itemsPerPage = ref(10);
const totalItems = ref(0);
const hotTags = ref<string[]>([]);
const hotArticles = ref<Article[]>([]);
const isLoading = ref(false);

onMounted(() => {
  searchQuery.value = (route.query.q as string) || '';
  selectedCategory.value = (route.query.category as string) || '';
  currentPage.value = 1;
  performSearch();
  loadHotTags();
  loadHotArticles();
});

watch(() => route.query, (newQuery) => {
  searchQuery.value = (newQuery.q as string) || '';
  selectedCategory.value = (newQuery.category as string) || '';
  currentPage.value = 1;
  performSearch();
}, { deep: true });

const breadcrumbs = computed(() => {
  const items = [];

  if (route.query.q) {
    items.push({ label: '搜索', path: '/search' });
    items.push({ label: `"${route.query.q}"` });
  } else if (route.query.tag) {
    items.push({ label: '标签', path: '/search' });
    items.push({ label: `#${route.query.tag}` });
  } else if (route.query.category) {
    items.push({ label: '分类', path: '/search' });
    items.push({ label: route.query.category as string });
  } else {
    items.push({ label: '搜索' });
  }

  return items;
});

const hasQuery = computed(() =>
  (route.query.tag && route.query.tag !== '') ||
  searchQuery.value.trim() ||
  (selectedCategory.value && selectedCategory.value !== '')
);

// 加载热门标签
async function loadHotTags() {
  try {
    const response = await tagApi.getHotTags(30);
    if (response.code === 200 && response.data) {
      hotTags.value = response.data.map((t: any) => t.name || String(t));
    }
  } catch (err) {
    console.error('加载热门标签失败:', err);
  }
}

// 服务端分页搜索：只拉当前页 10 条，避免一次性拉 100 条导致首屏卡顿
async function performSearch() {
  // 没有查询条件时不加载内容
  if (!hasQuery.value) {
    allArticles.value = [];
    totalItems.value = 0;
    return;
  }

  isLoading.value = true;
  try {
    let response: any;
    const tagParam = route.query.tag as string;
    // 公共分页参数
    const pageParams = {
      pageNum: currentPage.value,
      pageSize: itemsPerPage.value,
      sortBy: (sortBy.value === '热门' ? 'views' : 'createdAt') as 'views' | 'createdAt'
    };

    if (tagParam) {
      // 按标签搜索
      response = await articleApi.getArticleList({ tag: tagParam, ...pageParams });
    } else if (searchQuery.value.trim()) {
      // 按关键词搜索
      response = await articleApi.getArticleList({ keyword: searchQuery.value.trim(), ...pageParams });
    } else if (selectedCategory.value) {
      // 按分类搜索
      response = await articleApi.getArticleList({ category: selectedCategory.value, ...pageParams });
    }

    if (response && response.code === 200 && response.data) {
      const list = (response.data as any).list || response.data || [];
      allArticles.value = list.map(transformArticle) as unknown as Article[];
      totalItems.value = (response.data as any).total || list.length;
    } else {
      allArticles.value = [];
      totalItems.value = 0;
    }
  } catch (err) {
    console.error('搜索失败:', err);
    allArticles.value = [];
    totalItems.value = 0;
  } finally {
    isLoading.value = false;
  }
}

// 加载右侧栏热门文章（复用首页热门数据接口，取 5 条）
async function loadHotArticles() {
  try {
    const response = await articleApi.getHomeData();
    if (response.code === 200 && response.data) {
      const list = (response.data as any).hotArticles || [];
      hotArticles.value = list.slice(0, 5).map(transformArticle) as unknown as Article[];
    }
  } catch (err) {
    console.error('加载热门文章失败:', err);
    hotArticles.value = [];
  }
}

const totalPages = computed(() => Math.ceil(totalItems.value / itemsPerPage.value));

const paginatedArticles = computed(() => {
  return allArticles.value;
});

function handlePageChange(page: number) {
  currentPage.value = page;
  performSearch();
  window.scrollTo({ top: 0, behavior: 'smooth' });
}

// 跳转到创作页（需登录）
function goToPublish() {
  if (!requireAuth('/publish')) return;
  router.push('/publish');
}

function handleSearch() {
  currentPage.value = 1;
  if (searchQuery.value.trim()) {
    router.push({ path: '/search', query: { q: searchQuery.value } });
  } else {
    router.push('/search');
  }
  // 不在此处调用 performSearch：router.push 改变 route.query 后，
  // 上方 watch(() => route.query, ...) 会自动触发 performSearch，避免重复请求
}

function handleTagClick(tag: string) {
  router.push({ path: '/search', query: { tag } });
}

// SEO - 动态更新
useHead(
  computed(() => {
    let title = '搜索'
    let description = '搜索墨韵·智库中的文章、标签和作者'

    if (searchQuery.value) {
      title = `搜索: ${searchQuery.value}`
      description = `搜索关于"${searchQuery.value}"的文章和内容`
    } else if (route.query.category) {
      title = `${route.query.category}`
      description = `浏览${route.query.category}分类下的所有文章`
    } else if (route.query.tag) {
      title = `标签: ${route.query.tag}`
      description = `查看带有"${route.query.tag}"标签的所有文章`
    }

    return generateSeo({
      title,
      description,
      keywords: ['搜索', '文章', '内容', '发现'],
      type: 'website'
    })
  })
)
</script>

<template>
  <div class="min-h-screen flex flex-col" style="background-color: var(--theme-bg);">
      <!-- Search Header -->
      <div
        class="border-b sticky top-0 z-30 backdrop-blur-sm py-3"
        style="background-color: var(--theme-surface); border-color: var(--theme-border);"
      >
        <div class="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8">
          <div class="flex items-center justify-between gap-4">
            <Breadcrumb :items="breadcrumbs" />
            <!-- Search Bar -->
            <div class="flex-1 relative">
              <div class="absolute inset-y-0 left-0 pl-4 sm:pl-5 flex items-center pointer-events-none">
                <SearchIcon class="w-5 h-5" style="color: var(--theme-text-secondary);" />
              </div>
              <label for="search-input" class="sr-only">搜索文章</label>
              <input
                id="search-input"
                v-model="searchQuery"
                @keyup.enter="handleSearch"
                type="text"
                placeholder="搜索文章、标签或作者..."
                class="w-full pl-11 sm:pl-12 pr-5 sm:pr-6 py-2.5 sm:py-3 text-sm sm:text-base border rounded-xl focus:outline-none focus:ring-2 transition-all"
                style="background-color: var(--theme-surface); border-color: var(--theme-border); color: var(--theme-text);"
              />
              <button
                @click="handleSearch"
                class="absolute inset-y-0 right-1.5 sm:right-2 my-1.5 sm:my-2 px-3 sm:px-4 font-medium rounded-lg transition-colors text-xs sm:text-sm"
                style="background-color: var(--theme-primary); color: white;"
              >
                搜索
              </button>
            </div>
          </div>

          <!-- 热门标签辅助检索：放在搜索框下方，点击即查 -->
          <div v-if="hotTags.length > 0" class="flex items-center gap-2 mt-3 flex-wrap">
            <span class="text-xs flex-shrink-0 flex items-center gap-1" style="color: var(--theme-text-secondary);">
              <TrendingUp class="w-3 h-3" />
              热门：
            </span>
            <button
              v-for="tag in hotTags.slice(0, 12)"
              :key="tag"
              @click="handleTagClick(tag)"
              class="px-2.5 py-1 rounded-full text-xs transition-colors hover:opacity-80"
              style="background-color: var(--theme-accent); color: var(--theme-primary);"
            >
              #{{ tag }}
            </button>
          </div>
        </div>
      </div>



      <!-- Results Header -->
      <div class="border-b py-3 sm:py-4" v-if="hasQuery" style="background-color: var(--theme-bg); border-color: var(--theme-border);">
        <div class="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8">
          <div class="flex items-center justify-between gap-4">
            <div class="flex items-center space-x-2 flex-shrink-0">
              <span class="text-xs sm:text-sm hidden sm:inline" style="color: var(--theme-text-secondary);">排序：</span>
              <label for="search-sort" class="sr-only">排序方式</label>
              <select
                id="search-sort"
                v-model="sortBy"
                @change="performSearch"
                class="text-xs sm:text-sm border rounded-lg px-2 sm:px-3 py-1 sm:py-1.5 focus:outline-none focus:ring-2"
                style="border-color: var(--theme-border); background-color: var(--theme-bg); color: var(--theme-text);"
              >
                <option>最新</option>
                <option>热门</option>
                <option>推荐</option>
              </select>
            </div>
          </div>
        </div>
      </div>

      <!-- Results -->
      <div class="py-6 sm:py-8 flex-1" v-if="hasQuery">
        <div class="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8">
          <div class="grid lg:grid-cols-[1fr_300px] gap-6 lg:gap-8">
            <!-- 主列表区 -->
            <div class="min-w-0">
              <!-- Loading State -->
              <div v-if="isLoading" class="text-center py-12">
                <div class="inline-block w-10 h-10 border-4 border-t-4 border-gray-300 rounded-full animate-spin" style="border-top-color: var(--theme-primary);"></div>
                <p class="mt-4" style="color: var(--theme-text-secondary);">搜索中...</p>
              </div>

              <!-- Results Vertical List -->
              <div v-else-if="paginatedArticles.length > 0" class="space-y-4 sm:space-y-6 mb-6">
                <ArticleCard
                  v-for="article in paginatedArticles"
                  :key="article.id"
                  :article="article"
                />
              </div>

              <!-- Pagination -->
              <div v-if="!isLoading" class="flex justify-center mt-8">
                <Pagination
                  v-if="totalPages > 1 && paginatedArticles.length > 0"
                  :current-page="currentPage"
                  :total-pages="totalPages"
                  :total-items="totalItems"
                  :items-per-page="itemsPerPage"
                  @page-change="handlePageChange"
                />
              </div>

              <!-- No Results - 只在有查询条件但无结果时显示 -->
              <div v-if="!isLoading && paginatedArticles.length === 0" class="text-center py-16">
                <div class="w-24 h-24 rounded-full flex items-center justify-center mx-auto mb-6" style="background-color: var(--theme-surface);">
                  <SearchIcon class="w-12 h-12" style="color: var(--theme-text-secondary);" />
                </div>
                <h3 class="text-xl font-bold mb-2" style="color: var(--theme-text);">未找到相关内容</h3>
                <p class="mb-6" style="color: var(--theme-text-secondary);">尝试使用不同的关键词或浏览其他分类</p>
                <button
                  @click="searchQuery = ''; selectedCategory = ''; performSearch()"
                  class="px-6 py-2 rounded-full font-medium transition-colors"
                  style="background-color: var(--theme-primary); color: white;"
                >
                  浏览全部
                </button>
              </div>
            </div>

            <!-- 侧栏（lg 屏显示） -->
            <aside class="hidden lg:block space-y-6">
              <!-- 创作引导卡 -->
              <div class="rounded-xl p-5" style="background-color: var(--theme-surface); border: 1px solid var(--theme-border);">
                <div class="flex items-center gap-2 mb-3">
                  <div class="w-8 h-8 rounded-lg flex items-center justify-center" style="background-color: var(--theme-accent);">
                    <PenLine class="w-4 h-4" style="color: var(--theme-primary);" />
                  </div>
                  <h3 class="font-semibold text-base" style="color: var(--theme-text);">写下你的所思</h3>
                </div>
                <p class="text-xs mb-4" style="color: var(--theme-text-secondary);">
                  每天进步一点点，遇见更好的自己。分享即是力量。
                </p>
                <button
                    @click="goToPublish"
                    class="w-full inline-flex items-center justify-center gap-2 px-4 py-2.5 rounded-xl text-sm font-medium transition-colors"
                    style="background-color: var(--theme-primary); color: white;"
                >
                  <PenLine class="w-4 h-4" />
                  开始创作
                </button>
              </div>

              <!-- 热门文章推荐 -->
              <div v-if="hotArticles.length > 0" class="rounded-xl p-5" style="background-color: var(--theme-surface); border: 1px solid var(--theme-border);">
                <div class="flex items-center gap-2 mb-4">
                  <Flame class="w-4 h-4" style="color: var(--theme-primary);" />
                  <h3 class="font-semibold text-base" style="color: var(--theme-text);">热门文章</h3>
                </div>
                <div class="space-y-3">
                  <button
                      v-for="(article, index) in hotArticles"
                      :key="article.id"
                      @click="router.push(`/article/${article.id}`)"
                      class="flex items-start gap-2 cursor-pointer w-full text-left group"
                  >
                    <span
                        class="w-5 h-5 rounded-full flex items-center justify-center text-xs font-bold flex-shrink-0 mt-0.5"
                        :style="index < 3 ? { backgroundColor: 'var(--theme-primary)', color: 'white' } : { backgroundColor: 'var(--theme-accent)', color: 'var(--theme-text-secondary)' }"
                    >
                      {{ index + 1 }}
                    </span>
                    <div class="flex-1 min-w-0">
                      <h4 class="text-sm line-clamp-2 group-hover:opacity-80 transition-opacity" style="color: var(--theme-text);">
                        {{ article.title }}
                      </h4>
                      <div class="flex items-center gap-1 mt-1 text-xs" style="color: var(--theme-text-secondary);">
                        <Eye class="w-3 h-3" />
                        <span>{{ article.views || 0 }}</span>
                      </div>
                    </div>
                  </button>
                </div>
              </div>

              <!-- 小广告位（纯静态占位卡，预留后端接口位置） -->
              <div class="rounded-xl p-5 relative overflow-hidden" style="background: linear-gradient(135deg, #4f46e5 0%, #7c3aed 100%);">
                <div class="flex items-center gap-2 mb-2">
                  <Megaphone class="w-4 h-4 text-white/80" />
                  <span class="text-xs text-white/80 font-medium">合作推广</span>
                </div>
                <h4 class="text-white font-semibold text-sm mb-1">成为认证创作者</h4>
                <p class="text-white/80 text-xs mb-3 leading-relaxed">享受专属权益，让你的创作被更多人看见</p>
                <button
                    @click="router.push('/creator/certification')"
                    class="inline-flex items-center gap-1 px-3 py-1.5 rounded-full text-xs font-medium bg-white text-indigo-700 hover:bg-indigo-50 transition-colors"
                >
                  了解更多
                  <ArrowRight class="w-3 h-3" />
                </button>
              </div>
            </aside>
          </div>
        </div>
      </div>

    <!-- 公共Footer组件 -->
    <SiteFooter />
  </div>
</template>
