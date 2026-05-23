<script setup lang="ts">
import { ref, computed, onMounted, watch } from 'vue';
import { useRoute, useRouter } from 'vue-router';
import { useHead } from '@vueuse/head';
import { Search as SearchIcon, TrendingUp, Clock, Award, ChevronRight } from 'lucide-vue-next';
import ArticleCard from '@/components/ArticleCard.vue';
import Pagination from '@/components/Pagination.vue';
import Breadcrumb from '@/components/Breadcrumb.vue';
import SiteFooter from '@/components/SiteFooter.vue';

import { getArticles, searchArticles, getArticlesByCategory } from '@/data/mockData';
import { generateSeo } from '@/utils/seo';
import type { Article } from '@/types';

const route = useRoute();
const router = useRouter();
const searchQuery = ref('');
const selectedCategory = ref('全部');
const sortBy = ref('最新');
const allArticles = ref<Article[]>([]);
const currentPage = ref(1);
const itemsPerPage = ref(10);

onMounted(() => {
  searchQuery.value = (route.query.q as string) || '';
  selectedCategory.value = (route.query.category as string) || '全部';
  currentPage.value = 1;
  performSearch();
});

watch(() => route.query, (newQuery) => {
  searchQuery.value = (newQuery.q as string) || '';
  selectedCategory.value = (newQuery.category as string) || '全部';
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
  (selectedCategory.value && selectedCategory.value !== '全部')
);

function performSearch() {
  let result: Article[] = [];
  
  // 没有查询条件时不加载内容
  if (!hasQuery.value) {
    allArticles.value = [];
    return;
  }
  
  // 按标签搜索
  const tagParam = route.query.tag as string;
  if (tagParam) {
    result = searchArticles(tagParam);
  }
  // 按关键词搜索
  else if (searchQuery.value.trim()) {
    result = searchArticles(searchQuery.value);
  }
  // 按分类搜索
  else if (selectedCategory.value !== '全部') {
    result = getArticlesByCategory(selectedCategory.value);
    if (result.length === 0) {
      // 如果没有精确匹配，按标签搜索
      result = getArticles().filter(a => 
        a.tags.includes(selectedCategory.value)
      );
    }
  }
  
  // 排序
  if (sortBy.value === '热门') {
    result = [...result].sort((a, b) => b.views - a.views);
  } else if (sortBy.value === '最新') {
    result = [...result].sort((a, b) => new Date(b.createdAt).getTime() - new Date(a.createdAt).getTime());
  }
  
  allArticles.value = result;
}

const totalItems = computed(() => allArticles.value.length);
const totalPages = computed(() => Math.ceil(totalItems.value / itemsPerPage.value));

const paginatedArticles = computed(() => {
  const start = (currentPage.value - 1) * itemsPerPage.value;
  const end = start + itemsPerPage.value;
  return allArticles.value.slice(start, end);
});

function handlePageChange(page: number) {
  currentPage.value = page;
  window.scrollTo({ top: 0, behavior: 'smooth' });
}

function handleSearch() {
  currentPage.value = 1;
  if (searchQuery.value.trim()) {
    router.push({ path: '/search', query: { q: searchQuery.value } });
  } else {
    router.push('/search');
  }
  performSearch();
}

function handleTagClick(tag: string) {
  router.push({ path: '/search', query: { tag } });
}

function handleClearMouseEnter(event: MouseEvent) {
  (event.target as HTMLElement).style.color = 'var(--theme-primary)';
}

function handleClearMouseLeave(event: MouseEvent) {
  (event.target as HTMLElement).style.color = 'var(--theme-text-secondary)';
}

function handleTagMouseEnter(event: MouseEvent) {
  (event.target as HTMLElement).style.opacity = '0.8';
}

function handleTagMouseLeave(event: MouseEvent) {
  (event.target as HTMLElement).style.opacity = '1';
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
      <div class="border-b py-4 sm:py-5" style="background-color: var(--theme-bg); border-color: var(--theme-border);">
        <div class="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8">
          <!-- Search Bar -->
          <div class="flex items-center">
            <div class="flex-1 relative">
              <div class="absolute inset-y-0 left-0 pl-4 sm:pl-5 flex items-center pointer-events-none">
                <SearchIcon class="w-5 h-5" style="color: var(--theme-text-secondary);" />
              </div>
              <input
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
        </div>
      </div>



      <!-- Results Header -->
      <div class="border-b py-3 sm:py-4" v-if="hasQuery" style="background-color: var(--theme-bg); border-color: var(--theme-border);">
        <div class="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8">
          <div class="flex items-center justify-between gap-4">
            <Breadcrumb :items="breadcrumbs" />
            <div class="flex items-center space-x-2 flex-shrink-0">
              <span class="text-xs sm:text-sm hidden sm:inline" style="color: var(--theme-text-secondary);">排序：</span>
              <select
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
      <div class="py-8" v-if="hasQuery">
        <div class="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8">
          <!-- Results Vertical List -->
          <div v-if="paginatedArticles.length > 0" class="space-y-4 sm:space-y-6 mb-6">
            <ArticleCard 
              v-for="article in paginatedArticles" 
              :key="article.id" 
              :article="article"
            />
          </div>

          <!-- Pagination -->
          <div class="flex justify-center mt-8">
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
          <div v-if="paginatedArticles.length === 0" class="text-center py-16">
            <div class="w-24 h-24 rounded-full flex items-center justify-center mx-auto mb-6" style="background-color: var(--theme-surface);">
              <SearchIcon class="w-12 h-12" style="color: var(--theme-text-secondary);" />
            </div>
            <h3 class="text-xl font-bold mb-2" style="color: var(--theme-text);">未找到相关内容</h3>
            <p class="mb-6" style="color: var(--theme-text-secondary);">尝试使用不同的关键词或浏览其他分类</p>
            <button
              @click="searchQuery = ''; selectedCategory = '全部'; performSearch()"
              class="px-6 py-2 rounded-full font-medium transition-colors"
              style="background-color: var(--theme-primary); color: white;"
            >
              浏览全部
            </button>
          </div>
        </div>
      </div>

      <!-- Trending Tags -->
      <div class="py-8 sm:py-10" style="background-color: var(--theme-bg);">
        <div class="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8">
          <h2 class="text-xl font-bold mb-6 flex items-center space-x-2" style="color: var(--theme-text);">
            <TrendingUp class="w-6 h-6" style="color: var(--theme-primary);" />
            <span>热门标签</span>
          </h2>
          <div class="flex flex-wrap gap-3">
            <span 
              v-for="tag in ['文学', '散文', '随笔', 'Vue', 'JavaScript', 'React', '前端开发', 'TypeScript', '设计', 'Python', 'Go', '旅行', '乡情', '怀旧', '读书笔记', '写作指导']" 
              :key="tag"
              @click="handleTagClick(tag)"
              @mouseenter="handleTagMouseEnter($event)"
              @mouseleave="handleTagMouseLeave($event)"
              class="px-4 py-2 rounded-full text-sm font-medium cursor-pointer transition-colors"
              style="background-color: var(--theme-accent); color: var(--theme-primary);"
            >
              #{{ tag }}
            </span>
          </div>
        </div>
      </div>

    <!-- 公共Footer组件 -->
    <div class="mt-8 sm:mt-10 lg:mt-12">
      <SiteFooter />
    </div>
  </div>
</template>
