<script setup lang="ts">
import { ref, computed, onMounted, watch } from 'vue';
import { useRoute } from 'vue-router';
import { useHead } from '@vueuse/head';
import { TrendingUp, Clock, Award, ChevronRight } from 'lucide-vue-next';
import ArticleCard from '@/components/ArticleCard.vue';
import Pagination from '@/components/Pagination.vue';
import Breadcrumb from '@/components/Breadcrumb.vue';
import SiteFooter from '@/components/SiteFooter.vue';
import BackToTop from '@/components/BackToTop.vue';

import * as articleApi from '@/api/article';
import * as categoryApi from '@/api/category';
import { generateSeo, buildCanonicalUrl, fromSlug } from '@/utils/seo';
import type { Article as ApiArticle } from '@/types/api';

const isSpecialPageName = (name: string) => ['读书空间', '面试指南'].includes(name);

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
const listType = ref<'category' | 'tag' | 'all'>('all');
const selectedCategory = ref('全部');
const sortBy = ref('最新');
const isCategoryRecommended = ref(false); // 是否只显示分类推荐
const allArticles = ref<Article[]>([]);
const currentPage = ref(1);
const itemsPerPage = ref(10);
const loading = ref(false);
const totalItems = ref(0);
const error = ref<string | null>(null);

// 从路由参数解析列表类型和关键词
function parseRouteParams() {
  const routeName = route.name as string;
  const paramName = route.params.name as string | undefined;
  const queryCategory = route.query.category as string | undefined;

  if (routeName === 'category' && paramName) {
    listType.value = 'category';
    selectedCategory.value = fromSlug(paramName);
  } else if (routeName === 'tag' && paramName) {
    listType.value = 'tag';
    selectedCategory.value = fromSlug(paramName);
  } else if (queryCategory) {
    // 兼容旧路径：/list?category=xxx
    listType.value = 'category';
    selectedCategory.value = queryCategory;
  } else {
    listType.value = 'all';
    selectedCategory.value = '全部';
  }

  isCategoryRecommended.value = route.query.categoryRecommended === 'true';
}

// 转换API文章为前台格式
const transformArticle = (apiArticle: any): Article => {
  return {
    id: String(apiArticle.id),
    title: apiArticle.title,
    content: apiArticle.content || '',
    excerpt: apiArticle.excerpt || '',
    cover: apiArticle.cover,
    author: {
      id: String(apiArticle.authorId || apiArticle.author?.id || '1'),
      username: apiArticle.authorName || apiArticle.author?.username || '作者',
      avatar: apiArticle.authorAvatar || apiArticle.author?.avatar || 'https://images.unsplash.com/photo-1535713875002-d1d0cf377fde?w=150&h=150&fit=crop',
      bio: apiArticle.authorBio || apiArticle.author?.bio || '热爱写作，分享生活'
    },
    category: apiArticle.category || '技术',
    tags: apiArticle.tags || [],
    views: Number(apiArticle.views || 0),
    likes: Number(apiArticle.likes || 0),
    createdAt: apiArticle.createTime || apiArticle.createdAt || new Date().toISOString().split('T')[0]
  };
};

onMounted(() => {
  parseRouteParams();
  currentPage.value = 1;
  loadArticles();
});

watch(() => route.fullPath, () => {
  parseRouteParams();
  currentPage.value = 1;
  loadArticles();
}, { deep: true });

const breadcrumbs = computed(() => {
  const items = [];

  if (listType.value === 'tag') {
    items.push({ label: '首页', path: '/' });
    items.push({ label: '标签' });
    items.push({ label: selectedCategory.value, path: `/tag/${encodeURIComponent(selectedCategory.value)}` });
  } else if (selectedCategory.value && selectedCategory.value !== '全部') {
    items.push({ label: '首页', path: '/' });
    items.push({ label: selectedCategory.value, path: `/category/${encodeURIComponent(selectedCategory.value)}` });
  } else {
    items.push({ label: '首页', path: '/' });
    items.push({ label: '文章列表' });
  }

  return items;
});

// 语义化路径用于 canonical（不包含分页参数）
const canonicalPath = computed(() => {
  if (listType.value === 'tag') {
    return `/tag/${encodeURIComponent(selectedCategory.value)}`;
  }
  if (listType.value === 'category' && selectedCategory.value !== '全部') {
    return `/category/${encodeURIComponent(selectedCategory.value)}`;
  }
  return '/category';
});

const pageTitle = computed(() => {
  let title = '文章列表';
  if (listType.value === 'tag') {
    title = `标签：${selectedCategory.value}`;
  } else if (selectedCategory.value && selectedCategory.value !== '全部') {
    title = isCategoryRecommended.value
        ? `${selectedCategory.value} · 本栏推荐`
        : selectedCategory.value;
  } else if (isCategoryRecommended.value) {
    title = '本栏推荐';
  }
  return title;
});

const pageDescription = computed(() => {
  let desc = '浏览所有文章';
  if (listType.value === 'tag') {
    desc = `标签「${selectedCategory.value}」下的所有文章`;
  } else if (selectedCategory.value && selectedCategory.value !== '全部') {
    if (isCategoryRecommended.value) {
      desc = `浏览${selectedCategory.value}分类下的推荐文章`;
    } else {
      desc = `浏览${selectedCategory.value}分类下的所有文章`;
    }
  } else if (isCategoryRecommended.value) {
    desc = '浏览所有分类的推荐文章';
  }
  return desc;
});

async function loadArticles() {
  try {
    loading.value = true;
    error.value = null;

    const params: any = {
      pageNum: currentPage.value,
      pageSize: itemsPerPage.value,
      categoryName: selectedCategory.value !== '全部' ? selectedCategory.value : undefined,
      isCategoryRecommended: isCategoryRecommended.value ? true : undefined
    };

    const response = await articleApi.getArticleList(params);

    if (response.code === 200 && response.data) {
      const apiArticles = response.data.list || [];
      allArticles.value = apiArticles.map(transformArticle);
      totalItems.value = response.data.total || apiArticles.length;
    } else {
      error.value = response.message || '加载文章失败';
      allArticles.value = [];
    }
  } catch (err) {
    console.error('加载文章失败:', err);
    error.value = '加载文章失败，请稍后重试';
    allArticles.value = [];
  } finally {
    loading.value = false;
  }
}

const totalPages = computed(() => Math.ceil(totalItems.value / itemsPerPage.value));

const paginatedArticles = computed(() => {
  return allArticles.value as any[]; // 因为API已经做了分页
});

function handlePageChange(page: number) {
  currentPage.value = page;
  loadArticles();
  window.scrollTo({ top: 0, behavior: 'smooth' });
}

// SEO - 动态更新（使用语义化 canonical 路径，避免分页参数产生重复内容）
useHead(
    computed(() => {
      return generateSeo({
        title: pageTitle.value,
        description: pageDescription.value,
        keywords: [
          selectedCategory.value !== '全部' ? selectedCategory.value : '',
          isCategoryRecommended.value ? '推荐' : '',
          listType.value === 'tag' ? '标签' : '文章',
          listType.value === 'tag' ? selectedCategory.value : ''
        ].filter(Boolean),
        type: 'website',
        canonicalPath: canonicalPath.value
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

    <!-- 页面标题 -->
    <div class="py-6" style="background-color: var(--theme-bg);">
      <div class="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8">
        <div class="flex items-center gap-3">
          <h1 class="text-2xl font-bold" style="color: var(--theme-text);">{{ pageTitle }}</h1>
          <span v-if="isCategoryRecommended" class="px-2 py-1 text-xs rounded-full" style="background-color: var(--theme-primary); color: white;">
            本栏推荐
          </span>
        </div>
      </div>
    </div>

    <!-- 文章列表 -->
    <div class="py-4 flex-1">
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

    <!-- 返回顶部按钮 -->
    <BackToTop />
  </div>
</template>
