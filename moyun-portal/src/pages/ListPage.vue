<script setup lang="ts">
import { ref, computed, onMounted, watch } from 'vue';
import { useRoute } from 'vue-router';
import { useHead } from '@vueuse/head';
import { TrendingUp, Clock, Award, ChevronRight } from 'lucide-vue-next';
import ArticleCard from '@/components/ArticleCard.vue';
import Pagination from '@/components/Pagination.vue';
import Breadcrumb from '@/components/Breadcrumb.vue';
import SiteFooter from '@/components/SiteFooter.vue';

import { getArticleList } from '@/api/article';
import { getCategoryList } from '@/api/category';
import { generateSeo } from '@/utils/seo';
import { categories } from '@/data/categories';
import type { Article as ApiArticle } from '@/types/api';

// 前台Article类型
interface User {
  id: string;
  username: string;
  avatar: string;
  bio?: string;
}

interface Article {
  id: string;
  title: string;
  content: string;
  excerpt: string;
  cover?: string;
  author: User;
  category: string;
  tags: string[];
  views: number;
  likes: number;
  createdAt: string;
}

const route = useRoute();
const selectedCategory = ref('全部');
const sortBy = ref('最新');
const allArticles = ref<Article[]>([]);
const currentPage = ref(1);
const itemsPerPage = ref(10);
const loading = ref(false);
const totalItems = ref(0);

// 模拟用户数据
const mockUser = (id: string, name: string): User => ({
  id,
  username: name,
  avatar: name.charAt(0),
  bio: '热爱写作，分享生活'
});

// 转换API文章为前台格式
const transformArticle = (apiArticle: any): Article => {
  return {
    id: String(apiArticle.id),
    title: apiArticle.title,
    content: apiArticle.content || '',
    excerpt: apiArticle.excerpt || '',
    cover: apiArticle.cover,
    author: mockUser(String(apiArticle.authorId || 1), '作者'),
    category: '技术',
    tags: [],
    views: Number(apiArticle.views || 0),
    likes: Number(apiArticle.likes || 0),
    createdAt: apiArticle.createTime || new Date().toISOString().split('T')[0]
  };
};

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

async function loadArticles() {
  try {
    loading.value = true;
    
    // 先使用mock数据作为备选
    const mockArticles = [
      {
        id: '1',
        title: 'Vue 3 组合式 API 完全指南',
        content: '<p>Vue 3 的组合式 API 为我们提供了一种全新的方式来组织和复用代码...</p>',
        excerpt: 'Vue 3 的组合式 API 为我们提供了一种全新的方式来组织和复用代码。在这篇文章中，我们将深入探讨如何使用组合式 API 来构建更好的 Vue 应用。',
        cover: 'https://images.unsplash.com/photo-1555066931-4365d14bab8c?w=800&h=400&fit=crop',
        author: mockUser('1', '张三'),
        category: '技术栈手册',
        tags: ['Vue', 'JavaScript', '前端'],
        views: 1256,
        likes: 89,
        createdAt: '2024-01-10'
      },
      {
        id: '2',
        title: 'Tailwind CSS 3.0 新特性详解',
        content: '<p>Tailwind CSS 3.0 带来了许多令人兴奋的新特性...</p>',
        excerpt: 'Tailwind CSS 3.0 带来了许多令人兴奋的新特性。让我们一起来看看这些新特性吧。',
        cover: 'https://images.unsplash.com/photo-1507721999472-8ed4421c4af2?w=800&h=400&fit=crop',
        author: mockUser('2', '李四'),
        category: '技术栈手册',
        tags: ['CSS', 'Tailwind', '前端'],
        views: 892,
        likes: 56,
        createdAt: '2024-01-12'
      },
      {
        id: '3',
        title: 'Vite 5.0 快速上手教程',
        content: '<p>Vite 是新一代的前端构建工具，它提供了极快的开发体验...</p>',
        excerpt: 'Vite 是新一代的前端构建工具，提供了极快的开发体验。让我们一起来学习吧。',
        cover: 'https://images.unsplash.com/photo-1498050108023-c5249f4df085?w=800&h=400&fit=crop',
        author: mockUser('3', '王五'),
        category: '技术栈手册',
        tags: ['Vite', 'JavaScript', '工具'],
        views: 743,
        likes: 42,
        createdAt: '2024-01-15'
      }
    ];
    
    // 尝试从API获取
    try {
      const params: any = {
        page: currentPage.value,
        pageSize: itemsPerPage.value
      };
      
      const response = await getArticleList(params);
      
      if (response.code === 200 && response.data) {
        const apiArticles = response.data.list || [];
        allArticles.value = apiArticles.map(transformArticle);
        totalItems.value = response.data.total || apiArticles.length;
      } else {
        // 如果API失败，使用mock数据
        allArticles.value = mockArticles;
        totalItems.value = mockArticles.length;
      }
    } catch (apiError) {
      // API调用失败，使用mock数据
      console.log('API调用失败，使用mock数据', apiError);
      allArticles.value = mockArticles;
      totalItems.value = mockArticles.length;
    }
    
  } catch (error) {
    console.error('加载文章失败:', error);
  } finally {
    loading.value = false;
  }
}

const totalPages = computed(() => Math.ceil(totalItems.value / itemsPerPage.value));

const paginatedArticles = computed(() => {
  return allArticles.value; // 因为API已经做了分页
});

function handlePageChange(page: number) {
  currentPage.value = page;
  loadArticles();
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
