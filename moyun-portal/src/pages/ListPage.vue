<script setup lang="ts">
import { ref, computed, onMounted, watch } from 'vue';
import { useRoute, useRouter, RouterLink as Link } from 'vue-router';
import { useHead } from '@vueuse/head';
import { Clock, Flame, Tag, ArrowRight, PenLine } from 'lucide-vue-next';
import ArticleCard from '@/components/ArticleCard.vue';
import Pagination from '@/components/Pagination.vue';
import Breadcrumb from '@/components/Breadcrumb.vue';
import SiteFooter from '@/components/SiteFooter.vue';
import BackToTop from '@/components/BackToTop.vue';

import * as articleApi from '@/api/article';
import * as categoryApi from '@/api/category';
import { getHotTags } from '@/api/tag';
import { generateSeo, buildCanonicalUrl, fromSlug } from '@/utils/seo';
import { transformArticle } from '@/utils/articleTransform';
import { useAuth } from '@/composables/useAuth';
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
const router = useRouter();
const { requireAuth } = useAuth();
const listType = ref<'category' | 'tag' | 'all'>('all');
const selectedCategory = ref('全部');
const isCategoryRecommended = ref(false); // 是否只显示分类推荐
const allArticles = ref<Article[]>([]);
const currentPage = ref(1);
const itemsPerPage = ref(10);
const loading = ref(false);
const totalItems = ref(0);
const error = ref<string | null>(null);

// 侧栏：热门标签
const hotTags = ref<any[]>([]);

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

// 转换API文章为前台格式：使用 @/utils/articleTransform 的统一实现

onMounted(() => {
  parseRouteParams();
  currentPage.value = 1;
  Promise.all([loadArticles(), loadHotTags()]);
});

// 加载侧栏热门标签
async function loadHotTags() {
  try {
    const response = await getHotTags(15);
    if (response.code === 200 && response.data) {
      hotTags.value = response.data;
    }
  } catch (err) {
    console.error('加载热门标签失败:', err);
    hotTags.value = [];
  }
}

// 跳转到标签列表
function goToTag(tagName: string) {
  router.push(`/tag/${encodeURIComponent(tagName)}`);
}

// 跳转到创作页（需登录）
function goToPublish() {
  if (!requireAuth('/publish')) return;
  router.push('/publish');
}

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
      allArticles.value = apiArticles.map(transformArticle) as unknown as Article[];
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
    <!-- 面包屑 -->
    <div class="border-b py-3" style="background-color: var(--theme-bg); border-color: var(--theme-border);">
      <div class="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8">
        <Breadcrumb :items="breadcrumbs" />
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
        <div class="grid lg:grid-cols-[1fr_300px] gap-6 lg:gap-8">
          <!-- 主列表区 -->
          <div class="min-w-0">
            <!-- 加载状态 -->
            <div v-if="loading" class="py-16 flex items-center justify-center">
              <div class="flex flex-col items-center gap-3">
                <div class="w-10 h-10 border-2 rounded-full animate-spin" style="border-color: var(--theme-accent); border-top-color: var(--theme-primary);"></div>
                <p class="text-sm" style="color: var(--theme-text-secondary);">加载中...</p>
              </div>
            </div>

            <!-- 错误状态 -->
            <div v-else-if="error" class="py-16 flex flex-col items-center justify-center text-center">
              <div class="w-20 h-20 rounded-full flex items-center justify-center mx-auto mb-6" style="background-color: var(--theme-accent);">
                <Clock class="w-10 h-10" style="color: var(--theme-text-secondary);" />
              </div>
              <h3 class="text-xl font-bold mb-2" style="color: var(--theme-text);">加载失败</h3>
              <p class="mb-6" style="color: var(--theme-text-secondary);">{{ error }}</p>
              <button @click="loadArticles" class="px-4 py-2 rounded-lg text-sm font-medium text-white" style="background-color: var(--theme-primary);">重试</button>
            </div>

            <!-- 文章列表 -->
            <div v-else-if="paginatedArticles.length > 0" class="space-y-4 sm:space-y-6 mb-6">
              <ArticleCard
                  v-for="article in paginatedArticles"
                  :key="article.id"
                  :article="article"
              />
            </div>

            <!-- Pagination -->
            <div class="flex justify-center mt-8" v-if="!loading && !error && totalPages > 1 && paginatedArticles.length > 0">
              <Pagination
                  :current-page="currentPage"
                  :total-pages="totalPages"
                  :total-items="totalItems"
                  :items-per-page="itemsPerPage"
                  @page-change="handlePageChange"
              />
            </div>

            <!-- 空状态 -->
            <div v-else-if="!loading && !error && paginatedArticles.length === 0" class="text-center py-16">
              <div class="w-24 h-24 rounded-full flex items-center justify-center mx-auto mb-6" style="background-color: var(--theme-accent);">
                <Clock class="w-12 h-12" style="color: var(--theme-text-secondary);" />
              </div>
              <h3 class="text-xl font-bold mb-2" style="color: var(--theme-text);">暂无文章</h3>
              <p class="mb-6" style="color: var(--theme-text-secondary);">该分类下还没有文章，敬请期待</p>
              <Link to="/category" class="inline-flex items-center gap-2 px-4 py-2 rounded-lg text-sm font-medium text-white" style="background-color: var(--theme-primary);">浏览全站</Link>
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
                在浮躁的世界，留一页纸给灵魂。分享即是力量。
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

            <!-- 热门标签 -->
            <div v-if="hotTags.length > 0" class="rounded-xl p-5" style="background-color: var(--theme-surface); border: 1px solid var(--theme-border);">
              <div class="flex items-center gap-2 mb-4">
                <Flame class="w-4 h-4" style="color: var(--theme-primary);" />
                <h3 class="font-semibold text-base" style="color: var(--theme-text);">热门标签</h3>
              </div>
              <nav class="flex flex-wrap gap-2">
                <button
                    v-for="tag in hotTags"
                    :key="tag.id || tag.name"
                    @click="goToTag(tag.name)"
                    class="inline-flex items-center gap-1 px-3 py-1.5 rounded-full text-xs transition-colors hover:opacity-80"
                    style="background-color: var(--theme-accent); color: var(--theme-primary);"
                >
                  <Tag class="w-3 h-3" />
                  {{ tag.name }}
                </button>
              </nav>
            </div>

            <!-- 浏览其他分类 -->
            <div class="rounded-xl p-5" style="background-color: var(--theme-surface); border: 1px solid var(--theme-border);">
              <div class="flex items-center gap-2 mb-3">
                <ArrowRight class="w-4 h-4" style="color: var(--theme-primary);" />
                <h3 class="font-semibold text-base" style="color: var(--theme-text);">浏览全站</h3>
              </div>
              <div class="space-y-1">
                <Link to="/category" class="block py-2 text-sm transition-colors hover:opacity-80" style="color: var(--theme-text-secondary);">全部文章</Link>
                <Link to="/authors" class="block py-2 text-sm transition-colors hover:opacity-80" style="color: var(--theme-text-secondary);">名家录</Link>
                <Link to="/reading" class="block py-2 text-sm transition-colors hover:opacity-80" style="color: var(--theme-text-secondary);">读书空间</Link>
                <Link to="/interview" class="block py-2 text-sm transition-colors hover:opacity-80" style="color: var(--theme-text-secondary);">面试指南</Link>
              </div>
            </div>
          </aside>
        </div>
      </div>
    </div>

    <!-- 公共Footer组件 -->
    <SiteFooter />

    <!-- 返回顶部按钮 -->
    <BackToTop />
  </div>
</template>
