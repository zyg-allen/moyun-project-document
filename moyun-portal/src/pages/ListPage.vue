<script setup lang="ts">
import { ref, computed, onMounted, watch } from 'vue';
import { useRoute } from 'vue-router';
import { useHead } from '@vueuse/head';
import { TrendingUp, Clock, Award, ChevronRight } from 'lucide-vue-next';
import ArticleCard from '@/components/ArticleCard.vue';
import Pagination from '@/components/Pagination.vue';
import Breadcrumb from '@/components/Breadcrumb.vue';
import SiteFooter from '@/components/SiteFooter.vue';

import { getArticles, getArticlesByCategory } from '@/data/mockData';
import { generateSeo } from '@/utils/seo';
import { categories } from '@/data/categories';
import type { Article } from '@/types';

const route = useRoute();
const selectedCategory = ref('全部');
const sortBy = ref('最新');
const allArticles = ref<Article[]>([]);
const currentPage = ref(1);
const itemsPerPage = ref(10);

onMounted(() => {
  selectedCategory.value = (route.query.category as string) || '全部';
  currentPage.value = 1;
  loadArticles();
});

watch(() => route.query, (newQuery) => {
  selectedCategory.value = (newQuery.category as string) || '全部';
  currentPage.value = 1;
  loadArticles();
}, { deep: true });

const breadcrumbs = computed(() => {
  const items = [];
  
  if (selectedCategory.value && selectedCategory.value !== '全部') {
    items.push({ label: '分类', path: '/' });
    items.push({ label: selectedCategory.value });
  } else {
    items.push({ label: '文章列表' });
  }
  
  return items;
});

function loadArticles() {
  let result: Article[] = [];
  
  if (selectedCategory.value && selectedCategory.value !== '全部') {
    result = getArticlesByCategory(selectedCategory.value);
    if (result.length === 0) {
      // 如果没有精确匹配，按标签搜索
      result = getArticles().filter(a => 
        a.tags.includes(selectedCategory.value)
      );
    }
  } else {
    // 没有分类时显示全部
    result = getArticles();
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

// SEO - 动态更新
useHead(
  computed(() => {
    let title = '文章列表'
    let description = '浏览所有文章'
    
    if (selectedCategory.value && selectedCategory.value !== '全部') {
      title = selectedCategory.value
      description = `浏览${selectedCategory.value}分类下的所有文章`
    }
    
    return generateSeo({
      title,
      description,
      keywords: [selectedCategory.value, '文章', '分类'],
      type: 'website'
    })
  })
)
</script>

<template>
  <div class="min-h-screen flex flex-col" style="background-color: var(--theme-bg);">
    <!-- 面包屑 + 排序 -->
    <div class="border-b py-3" style="background-color: var(--theme-bg); border-color: var(--theme-border);">
      <div class="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8">
        <div class="flex items-center justify-between gap-4">
          <Breadcrumb :items="breadcrumbs" />
          <div class="flex items-center space-x-2">
            <span class="text-sm hidden sm:inline" style="color: var(--theme-text-secondary);">排序：</span>
            <select
              v-model="sortBy"
              @change="loadArticles"
              class="text-sm border rounded-lg px-3 py-2 focus:outline-none focus:ring-2"
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

    <!-- 文章列表 -->
    <div class="py-8 flex-1">
      <div class="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8">
        <!-- 文章卡片列表 -->
        <div v-if="paginatedArticles.length > 0" class="space-y-4 sm:space-y-6 mb-6">
          <ArticleCard 
            v-for="article in paginatedArticles" 
            :key="article.id" 
            :article="article"
          />
        </div>

        <!-- Pagination -->
        <div class="flex justify-center mt-8" v-if="totalPages > 1 && paginatedArticles.length > 0">
          <Pagination 
            :current-page="currentPage"
            :total-pages="totalPages"
            :total-items="totalItems"
            :items-per-page="itemsPerPage"
            @page-change="handlePageChange"
          />
        </div>

        <!-- 无结果 -->
        <div v-if="paginatedArticles.length === 0" class="text-center py-16">
          <div class="w-24 h-24 rounded-full flex items-center justify-center mx-auto mb-6" style="background-color: var(--theme-surface);">
            <Clock class="w-12 h-12" style="color: var(--theme-text-secondary);" />
          </div>
          <h3 class="text-xl font-bold mb-2" style="color: var(--theme-text);">暂无文章</h3>
          <p class="mb-6" style="color: var(--theme-text-secondary);">该分类下还没有文章，敬请期待</p>
        </div>
      </div>
    </div>

    <!-- 公共Footer组件 -->
    <div class="mt-8">
      <SiteFooter />
    </div>
  </div>
</template>
