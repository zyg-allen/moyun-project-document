<script setup lang="ts">
import { ref, computed, onMounted } from 'vue';
import { RouterLink as Link, useRouter } from 'vue-router';
import { useHead } from '@vueuse/head';
import { 
  Search, ChevronRight, ChevronLeft, Star, Flame, 
  User, Heart, Eye, Clock, Tag, BookOpen, Calendar, 
  MessageSquare, Quote, MoreHorizontal, ArrowRight, Sparkles, ChevronDown
} from 'lucide-vue-next';
import ArticleCard from '@/components/ArticleCard.vue';
import LazyImage from '@/components/LazyImage.vue';
import SiteFooter from '@/components/SiteFooter.vue';
import { generateSeo } from '@/utils/seo';
import { categories } from '@/data/categories';
import { getFriendLinks } from '@/api/friendLink';
import { getArticleList, getHotArticles, getFeaturedArticles } from '@/api/article';
import type { Article as ApiArticle } from '@/types/api';
import type { FriendLink } from '@/api/friendLink';

// 前台类型
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

const router = useRouter();

const openPublishPage = () => {
  window.open('/publish', '_blank');
};

const articles = ref<Article[]>([]);
const currentHeroIndex = ref(0);
const friendLinks = ref<FriendLink[]>([]);
const loading = ref(true);
const error = ref<string | null>(null);

// 加载友情链接
const loadFriendLinks = async () => {
  try {
    const response = await getFriendLinks();
    if (response.code === 200) {
      friendLinks.value = response.data;
    }
  } catch (error) {
    console.error('加载友情链接失败:', error);
  }
};

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

// 加载文章
const loadArticles = async () => {
  try {
    loading.value = true;
    const [listResponse, hotResponse, featuredResponse] = await Promise.all([
      getArticleList({ pageSize: 20 }),
      getHotArticles(8),
      getFeaturedArticles(6)
    ]);
    
    // 先使用mock数据，待API完善后切换
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
      },
      {
        id: '4',
        title: '深入理解 JavaScript 闭包',
        content: '<p>闭包是 JavaScript 中一个非常重要的概念...</p>',
        excerpt: '闭包是 JavaScript 中一个非常重要的概念。让我们深入理解它的工作原理。',
        cover: 'https://images.unsplash.com/photo-1516116216624-53e697fedbea?w=800&h=400&fit=crop',
        author: mockUser('1', '张三'),
        category: '技术栈手册',
        tags: ['JavaScript', '编程', '前端'],
        views: 1520,
        likes: 115,
        createdAt: '2024-01-18'
      },
      {
        id: '5',
        title: '2024 年前端技术趋势展望',
        content: '<p>新的一年已经到来，让我们一起展望 2024 年前端技术的发展趋势...</p>',
        excerpt: '新的一年已经到来，让我们一起展望 2024 年前端技术的发展趋势。',
        cover: 'https://images.unsplash.com/photo-1451187580459-43490279c0fa?w=800&h=400&fit=crop',
        author: mockUser('2', '李四'),
        category: '技术栈手册',
        tags: ['前端', '趋势', '技术'],
        views: 2341,
        likes: 178,
        createdAt: '2024-01-20'
      },
      {
        id: '6',
        title: '响应式设计最佳实践',
        content: '<p>响应式设计是现代 Web 开发的必备技能...</p>',
        excerpt: '响应式设计是现代 Web 开发的必备技能。让我们分享一些最佳实践。',
        cover: 'https://images.unsplash.com/photo-1512941937669-90a1b58e7e9c?w=800&h=400&fit=crop',
        author: mockUser('3', '王五'),
        category: '技术栈手册',
        tags: ['设计', '响应式', 'CSS'],
        views: 987,
        likes: 67,
        createdAt: '2024-01-22'
      },
      {
        id: '7',
        title: 'TypeScript 高级类型体操',
        content: '<p>TypeScript 的类型系统非常强大，本文将带你深入了解高级类型技巧...</p>',
        excerpt: '深入探索 TypeScript 的高级类型系统，掌握类型编程的精髓。',
        author: mockUser('1', '张三'),
        category: '技术栈手册',
        tags: ['TypeScript', 'JavaScript', '类型系统'],
        views: 856,
        likes: 73,
        createdAt: '2024-01-25'
      },
      {
        id: '8',
        title: 'Node.js 性能优化实战',
        content: '<p>Node.js 应用性能优化是后端开发中的重要课题...</p>',
        excerpt: '掌握 Node.js 性能优化的核心技巧，提升应用响应速度。',
        author: mockUser('2', '李四'),
        category: '架构札记',
        tags: ['Node.js', '性能', '后端'],
        views: 1123,
        likes: 92,
        createdAt: '2024-01-28'
      }
    ];
    
    articles.value = mockArticles;
    
    // 如果API返回了数据，尝试使用
    if (listResponse.code === 200 && listResponse.data.list.length > 0) {
      articles.value = listResponse.data.list.map(transformArticle);
    }
    
  } catch (err) {
    console.error('加载文章失败:', err);
    error.value = '加载文章失败';
    
    // 使用备用数据
    articles.value = [
      {
        id: '1',
        title: 'Vue 3 组合式 API 完全指南',
        content: '<p>Vue 3 的组合式 API 为我们提供了一种全新的方式来组织和复用代码...</p>',
        excerpt: 'Vue 3 的组合式 API 为我们提供了一种全新的方式来组织和复用代码。',
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
        excerpt: 'Tailwind CSS 3.0 带来了许多令人兴奋的新特性。',
        cover: 'https://images.unsplash.com/photo-1507721999472-8ed4421c4af2?w=800&h=400&fit=crop',
        author: mockUser('2', '李四'),
        category: '技术栈手册',
        tags: ['CSS', 'Tailwind', '前端'],
        views: 892,
        likes: 56,
        createdAt: '2024-01-12'
      }
    ];
  } finally {
    loading.value = false;
  }
};

const heroImages = [
  {
    url: 'https://images.unsplash.com/photo-1506905925346-21bda4d32df4?w=1920&h=500&fit=crop',
    title: '听松看云：山水间的文人情结',
    subtitle: '在浮躁的现代都市中，寻找那一抹属于中国文人的山水禅意，在字里行间感受万物生息。',
    author: '文 / 墨客',
    tag: '今日主题',
    articleId: '1',
    tags: ['散文', '山水', '禅意']
  },
  {
    url: 'https://images.unsplash.com/photo-1470071459604-3b5ec3a7fe05?w=1920&h=500&fit=crop',
    title: '月下独酌：古典诗词的现代诠释',
    subtitle: '穿越千年时光，品味古人的情感与哲思，感受诗词之美。',
    author: '文 / 诗韵',
    tag: '特别推荐',
    articleId: '2',
    tags: ['诗词', '古典', '文学']
  },
  {
    url: 'https://images.unsplash.com/photo-1507525428034-b723cf961d3e?w=1920&h=500&fit=crop',
    title: '笔墨丹青：传统艺术的当代传承',
    subtitle: '探寻中国画的意境之美，感受笔墨间流淌的东方美学。',
    author: '文 / 丹青',
    tag: '文化传承',
    articleId: '3',
    tags: ['艺术', '传统', '文化']
  }
];

// 使用统一的分类数据作为首页主题标签
const themes = categories.filter(cat => cat.key !== 'help').map((cat, index) => ({
  id: cat.key,
  name: cat.name,
  key: cat.key
}));

// 设置默认主题为第一个分类的名称
const activeTheme = ref(themes.length > 0 ? themes[0].name : '精选推荐');

const authors = [
  { id: '1', name: '林清玄', avatar: '林', works: 318, likes: '5.6w', days: 365 },
  { id: '2', name: '三毛', avatar: '三', works: 152, likes: '4.2w', days: 280 },
  { id: '3', name: '史铁生', avatar: '史', works: 86, likes: '3.8w', days: 120 },
  { id: '4', name: '张爱玲', avatar: '张', works: 168, likes: '6.1w', days: 98 },
  { id: '5', name: '木心', avatar: '木', works: 46, likes: '2.3w', days: 45 },
  { id: '6', name: '季羡林', avatar: '季', works: 256, likes: '5.9w', days: 520 },
  { id: '7', name: '余光中', avatar: '余', works: 198, likes: '4.8w', days: 380 }
];

const themeArticles: Record<string, Article[]> = {};

onMounted(() => {
  loadArticles();
  themes.forEach(theme => {
    themeArticles[theme.id] = articles.value.filter((_, index) => index % 8 === themes.indexOf(theme)).slice(0, 8);
  });
  setInterval(() => {
    currentHeroIndex.value = (currentHeroIndex.value + 1) % heroImages.length;
  }, 5000);
  loadFriendLinks();
});

const featuredArticles = computed(() => articles.value.slice(0, 6));
const trendingArticles = computed(() => articles.value.slice(6, 14));

function nextHero() {
  currentHeroIndex.value = (currentHeroIndex.value + 1) % heroImages.length;
}

function prevHero() {
  currentHeroIndex.value = (currentHeroIndex.value - 1 + heroImages.length) % heroImages.length;
}

function selectTheme(themeId: string, themeName: string) {
  activeTheme.value = themeName;
}

function viewMore(themeName: string) {
  router.push({ path: '/list', query: { category: themeName } });
}

function goToAuthor(id: string) {
  router.push(`/author/${id}`);
}

function getThemeCode(themeName: string): string {
  // 返回主题的名称前两个字
  return themeName.substring(0, 2);
}

function getThemeArticles(themeName: string): Article[] {
  const theme = themes.find(t => t.name === themeName);
  return themeArticles[theme?.id] || articles.value.slice(0, 8);
}

function getTrendingItemStyle(index: number) {
  if (index < 3) {
    return {
      backgroundColor: 'var(--theme-primary)',
      color: 'white'
    };
  }
  return {
    backgroundColor: 'var(--theme-bg)',
    color: 'var(--theme-text-secondary)'
  };
}

function getThemeTabStyle(themeName: string) {
  if (activeTheme.value === themeName) {
    return {
      backgroundColor: 'var(--theme-primary)',
      color: 'white'
    };
  }
  return {
    backgroundColor: 'var(--theme-surface)',
    color: 'var(--theme-text-secondary)'
  };
}

useHead(
  generateSeo({
    title: '首页',
    description: '墨韵·智库 - 为文学爱好者提供一个纯净的创作与阅读空间。',
    keywords: ['文学', '散文', '创作', '阅读', '分享', '社区'],
    type: 'website'
  })
)
</script>

<template>
  <div style="background-color: var(--theme-bg); min-height: 100vh;" class="flex flex-col">
    <!-- 1. 轮播图 -->
    <div aria-label="轮播展示" class="pt-3 sm:pt-4">
      <div class="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8">
        <div class="relative h-[280px] sm:h-[320px] md:h-[380px] overflow-hidden rounded-xl">
          <transition name="fade" mode="out-in">
            <LazyImage 
              :key="currentHeroIndex"
              :src="heroImages[currentHeroIndex].url" 
              :alt="heroImages[currentHeroIndex].title"
              class="w-full h-full object-cover rounded-xl"
            />
          </transition>
          <div class="absolute inset-0 bg-gradient-to-t from-black/70 via-black/30 to-transparent rounded-xl"></div>
          
          <div class="absolute inset-0 flex flex-col justify-end pb-8 sm:pb-10 md:pb-12">
            <div class="px-6 sm:px-8 md:px-10">
              <div class="max-w-3xl">
                <div class="inline-flex items-center space-x-2 bg-red-600 text-white px-3 py-1 rounded-full text-xs font-medium mb-3 sm:mb-4">
                  <span class="w-2 h-2 bg-yellow-400 rounded-full animate-pulse"></span>
                  {{ heroImages[currentHeroIndex].tag }}
                </div>
                <h1 class="text-lg sm:text-xl md:text-2xl lg:text-3xl font-bold text-white mb-2 sm:mb-3">
                  {{ heroImages[currentHeroIndex].title }}
                </h1>
                <p class="text-sm sm:text-base text-gray-200 mb-3 sm:mb-4 line-clamp-2">
                  {{ heroImages[currentHeroIndex].subtitle }}
                </p>
                <!-- 标签显示 -->
                <div v-if="heroImages[currentHeroIndex].tags && heroImages[currentHeroIndex].tags.length > 0" class="flex items-center space-x-2 mb-3 sm:mb-4">
                  <span 
                    v-for="(tag, index) in heroImages[currentHeroIndex].tags" 
                    :key="index"
                    class="inline-flex px-2.5 py-1 text-xs rounded-full bg-white/20 backdrop-blur text-white"
                  >
                    {{ tag }}
                  </span>
                </div>
                <div class="flex items-center justify-between">
                  <span class="text-gray-300 text-sm">{{ heroImages[currentHeroIndex].author }}</span>
                  <button
                    @click="router.push(`/article/${heroImages[currentHeroIndex].articleId}`)"
                    class="inline-flex items-center space-x-2 px-4 sm:px-5 py-2 sm:py-2.5 bg-red-600 text-white rounded-full text-sm font-medium hover:bg-red-700 transition-colors"
                  >
                    <span>阅读全文</span>
                    <ChevronRight class="w-4 h-4" />
                  </button>
                </div>
              </div>
            </div>
          </div>

          <button 
            @click="prevHero"
            class="absolute left-2 sm:left-3 top-1/2 -translate-y-1/2 w-7 h-7 sm:w-8 sm:h-8 bg-black/30 backdrop-blur text-white rounded-full flex items-center justify-center hover:bg-black/50 transition-colors"
          >
            <ChevronLeft class="w-4 h-4 sm:w-5 sm:h-5" />
          </button>
          <button 
            @click="nextHero"
            class="absolute right-2 sm:right-3 top-1/2 -translate-y-1/2 w-7 h-7 sm:w-8 sm:h-8 bg-black/30 backdrop-blur text-white rounded-full flex items-center justify-center hover:bg-black/50 transition-colors"
          >
            <ChevronRight class="w-4 h-4 sm:w-5 sm:h-5" />
          </button>

          <div class="absolute bottom-2 right-4 sm:bottom-3 sm:right-6 flex space-x-1.5">
            <button
              v-for="(_, index) in heroImages"
              :key="index"
              @click="currentHeroIndex = index"
              :class="['w-1.5 h-1.5 rounded-full transition-all', currentHeroIndex === index ? 'bg-red-600 w-5' : 'bg-white/50 hover:bg-white/70']"
            ></button>
          </div>
        </div>
      </div>
    </div>

    <!-- 2. 名言提示和写作入口 -->
    <div>
      <div class="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8">
        <div class="grid grid-cols-1 md:grid-cols-2 gap-0">
          <!-- 名言 -->
          <div class="py-3 sm:py-4 px-4" style="background-color: var(--theme-surface);">
            <div class="flex items-start gap-2 sm:gap-3">
              <Quote class="w-5 h-5 sm:w-6 sm:h-6 opacity-30 flex-shrink-0 mt-0.5" style="color: var(--theme-primary)" />
              <div>
                <p class="text-sm sm:text-base italic" style="color: var(--theme-text);">
                  "世间所有的相遇，都是久别重逢。"
                </p>
                <p class="text-xs mt-1" style="color: var(--theme-text-secondary);">—— 木心</p>
              </div>
            </div>
          </div>
          <!-- 写作入口 -->
          <button 
            @click="openPublishPage"
            class="flex items-center justify-between py-3 sm:py-4 px-4 text-left hover:opacity-90 transition-opacity"
            style="background-color: #dc2626;"
          >
            <div>
              <p class="text-white font-semibold text-sm sm:text-base">今天，写点什么？</p>
              <p class="text-red-100 text-xs">把感动化为文字，与千万读者分享</p>
            </div>
            <div class="w-7 h-7 sm:w-8 sm:h-8 bg-white/20 rounded-full flex items-center justify-center">
              <Sparkles class="w-3.5 h-3.5 sm:w-4 sm:h-4 text-white" />
            </div>
          </button>
        </div>
      </div>
    </div>

    <!-- 3. 本栏推荐 + 热门推荐（左右布局） -->
    <div class="py-4 sm:py-6" style="background-color: var(--theme-bg);">
      <div class="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8">
        <div class="grid lg:grid-cols-[2fr_1fr] gap-4 sm:gap-6">
          <!-- 本栏推荐 -->
          <div>
            <div class="flex items-center justify-between mb-3 sm:mb-4">
              <div class="flex items-center gap-2">
                <Star class="w-4 h-4 sm:w-5 sm:h-5 text-yellow-500" />
                <h2 class="text-base sm:text-lg font-bold" style="color: var(--theme-text);">本栏推荐</h2>
              </div>
              <button @click="router.push('/list')" class="flex items-center gap-1 text-xs sm:text-sm font-medium" style="color: var(--theme-text-secondary);">
                <span>更多</span>
                <ArrowRight class="w-3 h-3 sm:w-4 sm:h-4" />
              </button>
            </div>

            <div class="space-y-2 sm:space-y-3">
              <div 
                v-for="(article, index) in featuredArticles" 
                :key="article.id"
                @click="router.push('/article/' + article.id)"
                class="group flex gap-2 sm:gap-3 p-2 sm:p-3 rounded-lg cursor-pointer transition-colors"
                :style="{ backgroundColor: 'var(--theme-surface)' }"
              >
                <div class="relative w-20 h-14 sm:w-24 sm:h-16 flex-shrink-0">
                  <LazyImage 
                    :src="article.cover" 
                    :alt="article.title"
                    class="w-full h-full object-cover rounded-lg"
                  />
                  <span 
                    v-if="index === 0"
                    class="absolute top-1 left-1 px-1.5 py-0.5 bg-red-600 text-white text-xs rounded"
                  >
                    置顶
                  </span>
                </div>
                <div class="flex-1 min-w-0 flex flex-col justify-between">
                  <div>
                    <h3 class="font-medium text-xs sm:text-sm line-clamp-1" style="color: var(--theme-text);">
                      {{ article.title }}
                    </h3>
                    <p class="text-xs mt-0.5 line-clamp-1" style="color: var(--theme-text-secondary);">
                      {{ article.excerpt }}
                    </p>
                  </div>
                  <div class="flex items-center justify-end gap-1.5 sm:gap-2 mt-1 text-xs" style="color: var(--theme-text-secondary);">
                    <span>{{ article.author.username }}</span>
                    <span>{{ article.createdAt }}</span>
                    <span>{{ article.views }} 阅读</span>
                  </div>
                </div>
              </div>
            </div>
          </div>

          <!-- 热门推荐 -->
          <div class="p-3 sm:p-4 rounded-xl" style="background-color: var(--theme-surface);">
            <div class="flex items-center gap-2 mb-3 sm:mb-4">
              <Flame class="w-4 h-4 sm:w-5 sm:h-5 text-orange-500" />
              <h3 class="font-semibold text-sm sm:text-base" style="color: var(--theme-text);">热门推荐</h3>
            </div>
            <div class="space-y-2 sm:space-y-3">
              <div 
                v-for="(article, index) in trendingArticles" 
                :key="article.id"
                @click="router.push('/article/' + article.id)"
                class="flex items-start gap-2 cursor-pointer"
              >
                <span 
                  class="w-5 h-5 rounded-full flex items-center justify-center text-xs font-bold flex-shrink-0"
                  :style="getTrendingItemStyle(index)"
                >
                  {{ index + 1 }}
                </span>
                <div class="flex-1 min-w-0">
                  <div class="flex items-center justify-between gap-2">
                    <h4 class="font-medium text-xs sm:text-sm line-clamp-1 flex-1 text-left" style="color: var(--theme-text);">
                      {{ article.title }}
                    </h4>
                    <span class="text-xs flex items-center gap-1 flex-shrink-0" style="color: var(--theme-text-secondary);">
                      <Eye class="w-3 h-3" />
                      {{ article.views }}
                    </span>
                  </div>
                </div>
              </div>
            </div>
          </div>
        </div>
      </div>
    </div>

    <!-- 4. 按主题探索 -->
    <div class="py-4 sm:py-6 border-t" style="background-color: var(--theme-bg); border-color: var(--theme-border);">
      <div class="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8">
        <!-- 主题标签 -->
        <div class="flex items-center justify-between mb-3 sm:mb-4">
          <div class="flex items-center gap-2">
            <BookOpen class="w-4 h-4 sm:w-5 sm:h-5" style="color: var(--theme-primary);" />
            <h2 class="text-base sm:text-lg font-bold" style="color: var(--theme-text);">按主题探索</h2>
          </div>
          <div class="flex items-center gap-1.5">
            <button class="w-5 h-5 sm:w-6 sm:h-6 rounded border flex items-center justify-center" style="border-color: var(--theme-border);">
              <ChevronLeft class="w-3 h-3 sm:w-4 sm:h-4" />
            </button>
            <button class="w-5 h-5 sm:w-6 sm:h-6 rounded border flex items-center justify-center" style="border-color: var(--theme-border);">
              <ChevronRight class="w-3 h-3 sm:w-4 sm:h-4" />
            </button>
          </div>
        </div>

        <!-- 主题Tab -->
        <div class="flex flex-wrap gap-1.5 sm:gap-2 mb-3 sm:mb-4">
          <button
            v-for="theme in themes"
            :key="theme.id"
            @click="selectTheme(theme.id, theme.name)"
            class="px-3 sm:px-4 py-1 sm:py-1.5 rounded-full text-xs sm:text-sm font-medium transition-all"
            :style="getThemeTabStyle(theme.name)"
          >
            {{ theme.name }}
          </button>
        </div>

        <!-- 主题内容 -->
        <div class="p-3 sm:p-4 rounded-xl" style="background-color: var(--theme-surface);">
          <div class="flex items-center justify-between mb-3">
            <h3 class="font-semibold text-sm sm:text-base" style="color: var(--theme-text);">
              <span style="color: var(--theme-primary);">{{ getThemeCode(activeTheme) }}</span>
              {{ activeTheme }}精选
            </h3>
            <button @click="viewMore(activeTheme)" class="flex items-center gap-1 text-xs sm:text-sm" style="color: var(--theme-text-secondary);">
              <span>查看更多</span>
              <ArrowRight class="w-3 h-3 sm:w-4 sm:h-4" />
            </button>
          </div>
          <div class="grid sm:grid-cols-2 gap-4 sm:gap-6">
            <div 
              v-for="article in getThemeArticles(activeTheme)" 
              :key="article.id"
              @click="router.push('/article/' + article.id)"
              class="flex items-center gap-2 cursor-pointer"
            >
              <div class="w-1 h-1 rounded-full" style="background-color: var(--theme-primary);"></div>
              <span class="text-xs sm:text-sm line-clamp-1 flex-1" style="color: var(--theme-text);">
                {{ article.title }}
              </span>
              <span class="text-xs flex-shrink-0" style="color: var(--theme-text-secondary);">{{ article.createdAt }}</span>
            </div>
          </div>
        </div>
      </div>
    </div>

    <!-- 5. 墨韵名家录 -->
    <div class="py-4 sm:py-6" style="background-color: var(--theme-bg);">
      <div class="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8">
        <div class="flex items-center justify-between mb-3 sm:mb-4">
          <div class="flex items-center gap-2">
            <User class="w-4 h-4 sm:w-5 sm:h-5 text-blue-600" />
            <h2 class="text-base sm:text-lg font-bold" style="color: var(--theme-text);">墨韵名家录</h2>
          </div>
          <div class="flex items-center gap-1.5 sm:gap-2">
            <Link to="/authors" class="flex items-center gap-1.5 text-xs sm:text-sm" style="color: var(--theme-text-secondary);">
              <span>全部作者</span>
              <ArrowRight class="w-3 h-3 sm:w-4 sm:h-4" />
            </Link>
            <div class="flex items-center gap-1">
              <button class="w-5 h-5 sm:w-6 sm:h-6 rounded border flex items-center justify-center text-xs" style="border-color: var(--theme-border);">4</button>
              <button class="w-5 h-5 sm:w-6 sm:h-6 rounded border flex items-center justify-center text-xs" style="border-color: var(--theme-border);">></button>
            </div>
          </div>
        </div>

        <div class="grid grid-cols-2 sm:grid-cols-4 lg:grid-cols-7 gap-3 sm:gap-4">
          <div 
            v-for="author in authors" 
            :key="author.id"
            @click="goToAuthor(author.id)"
            class="text-center p-3 sm:p-4 rounded-xl cursor-pointer transition-colors"
            :style="{ backgroundColor: 'var(--theme-surface)' }"
          >
            <div class="w-10 h-10 sm:w-12 sm:h-12 mx-auto mb-2 rounded-full bg-gradient-to-br from-red-100 to-orange-100 flex items-center justify-center">
              <span class="text-sm font-bold" style="color: var(--theme-primary);">{{ author.avatar }}</span>
            </div>
            <p class="font-medium text-xs sm:text-sm mb-1" style="color: var(--theme-text);">{{ author.name }}</p>
            <p class="text-xs" style="color: var(--theme-text-secondary);">已创作 {{ author.works }} 篇散文</p>
            <p class="text-xs" style="color: var(--theme-text-secondary);">{{ author.likes }} 喜欢</p>
            <p class="text-xs" style="color: var(--theme-text-secondary);">坚持 {{ author.days }} 天</p>
            <button class="mt-2 px-3 py-1 rounded-full text-xs border" style="border-color: var(--theme-primary); color: var(--theme-primary);">
              关注作者
            </button>
          </div>
        </div>
      </div>
    </div>

    <!-- 6. 热门标签 & 友情链接 -->
    <div class="py-4 sm:py-6 border-t" style="background-color: var(--theme-surface); border-color: var(--theme-border);">
      <div class="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8">
        <div class="space-y-6 sm:space-y-8">
          <!-- 热门标签区域 -->
          <div class="rounded-xl p-4 sm:p-5" style="background-color: var(--theme-surface); border: 1px solid var(--theme-border);">
            <div class="flex items-center gap-2 mb-3">
              <Star class="w-4 h-4 sm:w-5 sm:h-5 text-yellow-500" />
              <h3 class="font-semibold text-sm sm:text-base" style="color: var(--theme-text);">热门标签</h3>
            </div>
            <nav class="flex flex-wrap gap-1.5 sm:gap-2">
              <span 
                v-for="tag in ['文学', '散文', '随笔', '游记', '乡情', '怀旧', '经典名篇', '读书', '写作指导', '人生感悟', '故乡', '重逢', '黄昏', '旅行日记', '光影记忆']" 
                :key="tag"
                @click="router.push('/list')"
                class="inline-flex items-center gap-1 px-2.5 sm:px-3 py-1 sm:py-1.5 rounded-full text-xs cursor-pointer transition-all hover:opacity-80"
                :style="{ backgroundColor: 'var(--theme-accent)', color: 'var(--theme-primary)' }"
              >
                <Tag class="w-3 h-3" />
                {{ tag }}
              </span>
            </nav>
          </div>

          <!-- 友情链接区域 -->
          <div class="rounded-xl p-4 sm:p-5" style="background-color: var(--theme-surface); border: 1px solid var(--theme-border);">
            <div class="flex items-center gap-2 mb-3">
              <BookOpen class="w-4 h-4 sm:w-5 sm:h-5" style="color: var(--theme-primary);" />
              <h3 class="font-semibold text-sm sm:text-base" style="color: var(--theme-text);">友情链接</h3>
            </div>
            <nav class="flex flex-wrap gap-2 sm:gap-3">
              <a 
                v-for="link in friendLinks" 
                :key="link.id"
                :href="link.url"
                target="_blank"
                rel="noopener noreferrer"
                class="inline-flex items-center gap-2 px-3 sm:px-4 py-2 sm:py-2.5 rounded-lg text-xs sm:text-sm cursor-pointer border transition-all hover:opacity-80 hover:scale-105"
                :style="{ backgroundColor: 'var(--theme-bg)', borderColor: 'var(--theme-border)', color: 'var(--theme-text-secondary)' }"
                :title="link.description"
              >
                <img 
                  v-if="link.logo" 
                  :src="link.logo" 
                  :alt="link.name"
                  class="w-6 h-6 sm:w-7 sm:h-7 rounded flex-shrink-0"
                  loading="lazy"
                />
                <span>{{ link.name }}</span>
              </a>
            </nav>
          </div>
        </div>
      </div>
    </div>

    <!-- 公共Footer组件 -->
    <div class="mt-8 sm:mt-10 lg:mt-12">
      <SiteFooter />
    </div>
  </div>
</template>

<style scoped>
.fade-enter-active,
.fade-leave-active {
  transition: opacity 0.5s ease;
}

.fade-enter-from,
.fade-leave-to {
  opacity: 0;
}
</style>
