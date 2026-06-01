<script setup lang="ts">
import {ref, computed, onMounted} from 'vue'
import {RouterLink as Link, useRouter} from 'vue-router'
import {useHead} from '@vueuse/head'
import {
  Search, ChevronRight, ChevronLeft, Star, Flame,
  User, Heart, Eye, Clock, Tag, BookOpen, Calendar,
  MessageSquare, Quote, MoreHorizontal, ArrowRight, Sparkles, ChevronDown,
  Mic, Code, Book, Briefcase, Users, Pen, MessageCircle, Upload, CheckCircle
} from 'lucide-vue-next'
import ArticleCard from '@/components/ArticleCard.vue'
import LazyImage from '@/components/LazyImage.vue'
import SiteFooter from '@/components/SiteFooter.vue'
import {generateSeo} from '@/utils/seo'
import {categories} from '@/data/categories'
import {getArticleList} from '@/api/article'
import {getFriendLinks} from '@/api/friendLink'
import type {Article as ApiArticle} from '@/types/api'
import type {FriendLink} from '@/api/friendLink'

const router = useRouter()

// 轮播图数据
interface CarouselItem {
  id: string
  image: string
  title: string
  subtitle: string
  author: string
  tag: string
  articleId: string
  tags: string[]
  type: 'prose' | 'tech'
}

// 轮播图
const carouselItems: CarouselItem[] = [
  {
    id: 'p1',
    image: 'https://images.unsplash.com/photo-1506905925346-21bda4d32df4?w=800&h=500&fit=crop',
    title: '听松看云：山水间的文人情结',
    subtitle: '在浮躁的现代都市中，寻找那一抹属于中国文人的山水禅意，在字里行间感受万物生息。',
    author: '文 / 墨客',
    tag: '今日主题',
    articleId: '1',
    tags: ['散文', '山水', '禅意'],
    type: 'prose'
  },
  {
    id: 'p2',
    image: 'https://images.unsplash.com/photo-1470071459604-3b5ec3a7fe05?w=800&h=500&fit=crop',
    title: '月下独酌：古典诗词的现代诠释',
    subtitle: '穿越千年时光，品味古人的情感与哲思，感受诗词之美。',
    author: '文 / 诗韵',
    tag: '特别推荐',
    articleId: '2',
    tags: ['诗词', '古典', '文学'],
    type: 'prose'
  },
  {
    id: 'p3',
    image: 'https://images.unsplash.com/photo-1507525428034-b723cf961d3e?w=800&h=500&fit=crop',
    title: '笔墨丹青：传统艺术的当代传承',
    subtitle: '探寻中国画的意境之美，感受笔墨间流淌的东方美学。',
    author: '文 / 丹青',
    tag: '文化传承',
    articleId: '3',
    tags: ['艺术', '传统', '文化'],
    type: 'prose'
  }
]

const currentIndex = ref(0)
const friendLinks = ref<FriendLink[]>([])
const articles = ref<any[]>([])
const loading = ref(true)
const error = ref<string | null>(null)

// 转换API文章为前台格式
const transformArticle = (apiArticle: any): any => {
  return {
    id: String(apiArticle.id),
    title: apiArticle.title,
    content: apiArticle.content || '',
    excerpt: apiArticle.excerpt || '',
    cover: apiArticle.cover,
    author: {
      id: String(apiArticle.authorId || '1'),
      username: apiArticle.authorNickname || apiArticle.authorUsername || '作者',
      nickname: apiArticle.authorNickname || apiArticle.authorUsername || '作者',
      avatar: apiArticle.authorAvatar || 'https://images.unsplash.com/photo-1535713875002-d1d0cf377fde?w=150&h=150&fit=crop',
      bio: apiArticle.authorBio || '热爱写作，分享生活'
    },
    category: apiArticle.categoryName || '技术',
    tags: apiArticle.tagNames || [],
    views: Number(apiArticle.views || 0),
    likes: Number(apiArticle.likes || 0),
    comments: Number(apiArticle.comments || 0),
    createdAt: apiArticle.createdAt || apiArticle.createTime || new Date().toISOString().split('T')[0]
  }
}

// 加载数据
const loadArticles = async () => {
  try {
    loading.value = true
    const listResponse = await getArticleList({pageSize: 20})

    if (listResponse.code === 200 && listResponse.data && listResponse.data.list) {
      articles.value = listResponse.data.list.map(transformArticle)
    } else {
      // 模拟数据
      articles.value = generateMockArticles()
    }
  } catch (err) {
    console.error('加载文章失败:', err)
    // 降级使用模拟数据
    articles.value = generateMockArticles()
    error.value = '加载文章失败，请稍后重试'
  } finally {
    loading.value = false
  }
}

// 生成模拟文章数据
const generateMockArticles = () => {
  const mockTitles = [
    'Spring Boot 3.0 新特性详解',
    'Vue3 组合式 API 实践指南',
    '我的故乡，我的童年',
    '算法面试必备：动态规划入门',
    '如何写出优雅的代码',
    '那座山，那条河',
    'React Hooks 深度解析',
    '独处的艺术',
    '微服务架构设计模式',
    '读书心得：《活着》',
    'Git 工作流最佳实践',
    '春日的记忆'
  ]

  return mockTitles.map((title, index) => ({
    id: String(index + 1),
    title: title,
    content: '这是文章内容...',
    excerpt: '这是文章摘要，描述了文章的主要内容...',
    cover: `https://images.unsplash.com/photo-${1500000000000 + index * 10000}?w=400&h=300&fit=crop`,
    author: {
      id: String((index % 7) + 1),
      username: '作者' + (index + 1),
      nickname: '作者' + (index + 1),
      avatar: 'https://images.unsplash.com/photo-1535713875002-d1d0cf377fde?w=150&h=150&fit=crop',
      bio: '热爱写作，分享生活'
    },
    category: index % 2 === 0 ? '技术' : '散文',
    tags: ['技术', '编程'],
    views: Math.floor(Math.random() * 10000),
    likes: Math.floor(Math.random() * 500),
    comments: Math.floor(Math.random() * 100),
    createdAt: '2024-01-' + String(15 - (index % 10)).padStart(2, '0')
  }))
}

const loadFriendLinks = async () => {
  try {
    const response = await getFriendLinks()
    if (response.code === 200 && response.data && response.data.list) {
      friendLinks.value = response.data.list
    }
  } catch (error) {
    console.error('加载友情链接失败:', error)
    // 模拟数据
    friendLinks.value = [
      {
        id: '1',
        name: '中国作家网',
        url: 'https://www.chinawriter.com.cn',
        description: '中国作家协会官网',
        sort: 1,
        status: 'active',
        createdAt: '2024-01-01',
        updatedAt: '2024-01-01'
      },
      {
        id: '2',
        name: '掘金',
        url: 'https://juejin.cn',
        description: '帮助开发者成长的社区',
        sort: 2,
        status: 'active',
        createdAt: '2024-01-01',
        updatedAt: '2024-01-01'
      },
      {
        id: '3',
        name: '思否',
        url: 'https://segmentfault.com',
        description: '开发者社区',
        sort: 3,
        status: 'active',
        createdAt: '2024-01-01',
        updatedAt: '2024-01-01'
      }
    ]
  }
}

// 轮播图控制
const nextSlide = () => {
  currentIndex.value = (currentIndex.value + 1) % carouselItems.length
}

const prevSlide = () => {
  currentIndex.value = (currentIndex.value - 1 + carouselItems.length) % carouselItems.length
}

// 使用一级分类作为主题
const themes = categories.filter(cat => cat.key !== 'featured').map((cat, index) => ({
  id: cat.key,
  name: cat.name,
  key: cat.key
}))

const activeTheme = ref(themes.length > 0 ? themes[0].name : '散文天地')

// 模拟作者数据
const authors = [
  {id: '1', name: '林清玄', avatar: '林', works: 318, likes: '5.6w', days: 365},
  {id: '2', name: '三毛', avatar: '三', works: 152, likes: '4.2w', days: 280},
  {id: '3', name: '史铁生', avatar: '史', works: 86, likes: '3.8w', days: 120},
  {id: '4', name: '张爱玲', avatar: '张', works: 168, likes: '6.1w', days: 98},
  {id: '5', name: '木心', avatar: '木', works: 46, likes: '2.3w', days: 45},
  {id: '6', name: '季羡林', avatar: '季', works: 256, likes: '5.9w', days: 520},
  {id: '7', name: '余光中', avatar: '余', works: 198, likes: '4.8w', days: 380}
]

// 精选文章和热门文章
const featuredArticles = computed(() => articles.value.slice(0, 6))
const trendingArticles = computed(() => articles.value.slice(6, 14))

onMounted(() => {
  loadArticles()
  loadFriendLinks()
  // 自动轮播
  setInterval(() => {
    nextSlide()
  }, 5000)
})

const selectTheme = (themeId: string, themeName: string) => {
  activeTheme.value = themeName
}

const viewMore = (themeName: string) => {
  router.push({path: '/list', query: {category: themeName}})
}

const goToAuthor = (id: string) => {
  router.push(`/author/${id}`)
}

const getThemeArticles = (themeName: string): any[] => {
  // 根据主题筛选模拟文章
  const filtered = articles.value.filter((_, index) => {
    if (themeName === '散文天地') return index % 2 === 0
    if (themeName === '技术笔记') return index % 2 === 1
    return true
  })
  return filtered.length > 0 ? filtered : articles.value
}

const getTrendingItemStyle = (index: number) => {
  if (index < 3) {
    return {
      backgroundColor: 'var(--theme-primary)',
      color: 'white'
    }
  }
  return {
    backgroundColor: 'var(--theme-bg)',
    color: 'var(--theme-text-secondary)'
  }
}

const getThemeTabStyle = (themeName: string) => {
  if (activeTheme.value === themeName) {
    return {
      backgroundColor: 'var(--theme-primary)',
      color: 'white'
    }
  }
  return {
    backgroundColor: 'var(--theme-surface)',
    color: 'var(--theme-text-secondary)'
  }
}

// 获取文章作者名称
const getArticleAuthorName = (article: any): string => {
  if (article.author?.nickname) return article.author.nickname;
  if (article.author?.username) return article.author.username;
  if (article.authorNickname) return article.authorNickname;
  if (article.authorUsername) return article.authorUsername;
  return '';
}

// 格式化文章日期
const formatArticleDate = (article: any): string => {
  const dateStr = article.createdAt || article.createTime;
  if (!dateStr) return '';
  const date = new Date(dateStr);
  const year = date.getFullYear();
  const month = String(date.getMonth() + 1).padStart(2, '0');
  const day = String(date.getDate()).padStart(2, '0');
  return `${year}-${month}-${day}`;
}

useHead(
    generateSeo({
      title: '首页 - 墨韵智库',
      description: '墨韵智库 - 为文学爱好者和技术开发者提供一个纯净的创作与阅读空间。',
      keywords: ['文学', '散文', '创作', '技术', '阅读', '社区', '墨韵智库'],
      type: 'website'
    })
)
</script>

<template>
  <div style="background-color: var(--theme-bg); min-height: 100vh;" class="flex flex-col">
    <!-- 1. 首屏轮播区 -->
    <div class="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 py-6">
      <div class="relative h-[380px] sm:h-[420px] lg:h-[480px] overflow-hidden rounded-xl shadow-lg group">
        <div class="absolute inset-0 bg-gradient-to-b from-transparent to-black/70 z-10"></div>
        <LazyImage
            :key="currentIndex"
            :src="carouselItems[currentIndex].image"
            :alt="carouselItems[currentIndex].title"
            class="absolute inset-0 w-full h-full object-cover transition-transform duration-700 group-hover:scale-105"
        />
        <div class="absolute inset-0 z-20 flex flex-col justify-end p-8 sm:p-10 lg:p-12">
          <div
              class="inline-flex items-center space-x-2 bg-gradient-to-r from-red-600 to-red-500 text-white px-3 py-1 rounded-full text-xs font-medium mb-3 sm:mb-4 w-fit">
            <Mic class="w-3 h-3"/>
            <span>{{ carouselItems[currentIndex].tag }}</span>
          </div>
          <h2 class="text-2xl sm:text-3xl lg:text-4xl font-bold text-white mb-3 sm:mb-4 leading-tight">
            {{ carouselItems[currentIndex].title }}
          </h2>
          <p class="text-base sm:text-lg text-gray-200 mb-5 sm:mb-6 line-clamp-2">
            {{ carouselItems[currentIndex].subtitle }}
          </p>
          <div class="flex flex-wrap gap-2 mb-5 sm:mb-6">
            <span
                v-for="(tag, index) in carouselItems[currentIndex].tags"
                :key="index"
                class="px-2.5 py-1 text-xs rounded-full bg-white/20 backdrop-blur text-white"
            >
              {{ tag }}
            </span>
          </div>
          <div class="flex items-center justify-between">
            <span class="text-gray-300 text-sm">{{ carouselItems[currentIndex].author }}</span>
            <button
                @click="router.push(`/article/${carouselItems[currentIndex].articleId}`)"
                class="inline-flex items-center space-x-2 px-5 sm:px-6 py-2.5 sm:py-3 bg-white text-red-600 rounded-full text-sm sm:text-base font-medium hover:bg-gray-100 transition-colors"
            >
              <span>阅读全文</span>
              <ChevronRight class="w-4 h-4 sm:w-5 sm:h-5"/>
            </button>
          </div>
        </div>
        <!-- 轮播控制按钮 -->
        <button
            @click="prevSlide"
            class="absolute left-4 sm:left-6 top-1/2 -translate-y-1/2 z-30 w-10 h-10 sm:w-12 sm:h-12 bg-black/30 backdrop-blur text-white rounded-full flex items-center justify-center hover:bg-black/50 transition-all opacity-0 group-hover:opacity-100"
        >
          <ChevronLeft class="w-5 h-5 sm:w-6 sm:h-6"/>
        </button>
        <button
            @click="nextSlide"
            class="absolute right-4 sm:right-6 top-1/2 -translate-y-1/2 z-30 w-10 h-10 sm:w-12 sm:h-12 bg-black/30 backdrop-blur text-white rounded-full flex items-center justify-center hover:bg-black/50 transition-all opacity-0 group-hover:opacity-100"
        >
          <ChevronRight class="w-5 h-5 sm:w-6 sm:h-6"/>
        </button>
        <!-- 指示器 -->
        <div class="absolute bottom-6 right-8 sm:bottom-8 sm:right-10 z-30 flex space-x-3">
          <button
              v-for="(_, index) in carouselItems"
              :key="index"
              @click="currentIndex = index"
              :class="['w-2.5 h-2.5 sm:w-3 sm:h-3 rounded-full transition-all', currentIndex === index ? 'bg-white w-8 sm:w-10' : 'bg-white/50 hover:bg-white/70']"
          ></button>
        </div>
      </div>
    </div>

    <!-- 2. 名言提示和写作入口 -->
    <div class="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 py-6 sm:py-8 lg:py-10">
      <div
          class="grid grid-cols-1 md:grid-cols-2 gap-0 bg-gradient-to-r from-amber-50 to-orange-50 border border-amber-100 rounded-xl">
        <!-- 名言 -->
        <div class="py-4 sm:py-6 px-4 sm:px-6">
          <div class="flex items-start gap-3">
            <Quote class="w-6 h-6 sm:w-8 sm:h-8 text-amber-500 opacity-60 flex-shrink-0 mt-0.5"/>
            <div>
              <p class="text-sm sm:text-base text-amber-900 italic">
                "世间所有的相遇，都是久别重逢。"
              </p>
              <p class="text-xs sm:text-sm mt-1.5 text-amber-700">—— 木心</p>
            </div>
          </div>
        </div>
        <!-- 写作入口 -->
        <button
            @click="router.push('/publish')"
            class="flex items-center justify-between py-4 sm:py-6 px-4 sm:px-6 text-left hover:opacity-90 transition-opacity bg-gradient-to-r from-red-600 to-red-500"
        >
          <div>
            <p class="text-white font-semibold text-sm sm:text-base">今天，写点什么？</p>
            <p class="text-red-100 text-xs sm:text-sm mt-1">把感动化为文字，与千万读者分享</p>
          </div>
          <div
              class="w-8 h-8 sm:w-10 sm:h-10 bg-white/20 rounded-full flex items-center justify-center flex-shrink-0 ml-4">
            <Sparkles class="w-4 h-4 sm:w-5 sm:h-5 text-white"/>
          </div>
        </button>
      </div>
    </div>

    <!-- 3. 内容精选区 -->
    <div class="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 py-6 sm:py-8 lg:py-10">
      <div class="border border-gray-200 rounded-xl p-4 sm:p-6" style="background-color: var(--theme-bg);">
        <div class="grid lg:grid-cols-[2fr_1fr] gap-6 sm:gap-8">
          <!-- 左侧：本周热门散文 -->
          <div class="flex items-center justify-between mb-4 sm:mb-6">
            <div class="flex items-center gap-2 sm:gap-3">
              <div
                  class="w-10 h-10 sm:w-12 sm:h-12 bg-gradient-to-br from-red-100 to-orange-100 rounded-xl flex items-center justify-center">
                <Pen class="w-5 h-5 sm:w-6 sm:h-6 text-red-600"/>
              </div>
              <div>
                <h2 class="text-lg sm:text-xl font-bold" style="color: var(--theme-text);">本周热门散文</h2>
                <p class="text-xs sm:text-sm" style="color: var(--theme-text-secondary);">发现动人的文字</p>
              </div>
            </div>
            <button @click="viewMore('散文天地')" class="flex items-center gap-1.5 text-xs sm:text-sm font-medium"
                    style="color: var(--theme-text-secondary);">
              <span>更多</span>
              <ArrowRight class="w-3.5 h-3.5 sm:w-4 sm:h-4"/>
            </button>
          </div>

          <div class="space-y-3 sm:space-y-4">
            <div
                v-for="(article, index) in featuredArticles.slice(0, 4)"
                :key="article.id"
                @click="router.push('/article/' + article.id)"
                class="group flex gap-4 p-3 sm:p-4 rounded-xl cursor-pointer transition-all hover:translate-x-1"
                style="background-color: var(--theme-surface);"
            >
              <div v-if="index < 2" class="relative w-28 h-20 sm:w-36 sm:h-24 flex-shrink-0 rounded-lg overflow-hidden">
                <LazyImage
                    :src="article.cover || 'https://images.unsplash.com/photo-1504198453319-5ce911bafcde?w=200&h=150&fit=crop'"
                    :alt="article.title"
                    class="w-full h-full object-cover transition-transform duration-300 group-hover:scale-110"
                />
                <div v-if="index === 0"
                     class="absolute top-1.5 left-1.5 px-2 py-0.5 bg-red-600 text-white text-xs rounded">置顶
                </div>
              </div>
              <div class="flex-1 min-w-0 flex flex-col justify-between">
                <div>
                  <h3 class="font-medium text-sm sm:text-base line-clamp-2 mb-1.5" style="color: var(--theme-text);">
                    {{ article.title }}
                  </h3>
                  <p class="text-xs line-clamp-1" style="color: var(--theme-text-secondary);">
                    {{ article.excerpt || '' }}
                  </p>
                </div>
                <div class="flex items-center justify-between gap-2 mt-2">
                  <div class="flex items-center gap-2">
                    <span class="text-xs" style="color: var(--theme-text-secondary);">{{
                        getArticleAuthorName(article)
                      }}</span>
                    <span class="text-xs" style="color: var(--theme-text-secondary);">{{
                        formatArticleDate(article)
                      }}</span>
                  </div>
                  <div class="flex items-center gap-3">
                    <span class="flex items-center gap-1 text-xs" style="color: var(--theme-text-secondary);">
                      <Eye class="w-3 h-3"/>
                      {{ article.views || 0 }}
                    </span>
                    <span class="flex items-center gap-1 text-xs" style="color: var(--theme-text-secondary);">
                      <Heart class="w-3 h-3"/>
                      {{ article.likes || 0 }}
                    </span>
                  </div>
                </div>
              </div>
            </div>
          </div>
        </div>
        <!-- 右侧：本周热门技术文 -->
        <div class="space-y-6">
          <!-- 热门技术文 -->
          <div class="p-4 sm:p-5 rounded-2xl" style="background-color: var(--theme-surface);">
            <div class="flex items-center gap-2 sm:gap-3 mb-4">
              <div
                  class="w-10 h-10 sm:w-11 sm:h-11 bg-gradient-to-br from-blue-100 to-indigo-100 rounded-xl flex items-center justify-center">
                <Code class="w-5 h-5 sm:w-5.5 sm:h-5.5 text-blue-600"/>
              </div>
              <div>
                <h3 class="font-semibold text-sm sm:text-base" style="color: var(--theme-text);">热门技术文</h3>
                <p class="text-xs" style="color: var(--theme-text-secondary);">技术前沿与实战</p>
              </div>
            </div>
            <div class="space-y-3">
              <div
                  v-for="(article, index) in trendingArticles.slice(0, 6)"
                  :key="article.id"
                  @click="router.push('/article/' + article.id)"
                  class="flex items-start gap-3 cursor-pointer group"
              >
                <span
                    class="w-6 h-6 sm:w-7 sm:h-7 rounded-full flex items-center justify-center text-xs font-bold flex-shrink-0"
                    :style="getTrendingItemStyle(index)"
                >
                  {{ index + 1 }}
                </span>
                <div class="flex-1 min-w-0">
                  <h4 class="font-medium text-xs sm:text-sm line-clamp-2 text-left mb-1"
                      style="color: var(--theme-text);">
                    {{ article.title }}
                  </h4>
                  <div class="flex items-center gap-3">
                    <span class="flex items-center gap-1 text-xs" style="color: var(--theme-text-secondary);">
                      <Eye class="w-3 h-3"/>
                      {{ article.views || 0 }}
                    </span>
                    <span class="flex items-center gap-1 text-xs" style="color: var(--theme-text-secondary);">
                      <Heart class="w-3 h-3"/>
                      {{ article.likes || 0 }}
                    </span>
                  </div>
                </div>
              </div>
            </div>
          </div>

          <!-- 快捷功能入口 -->
          <div class="grid grid-cols-2 gap-3">
            <button
                @click="viewMore('读书空间')"
                class="p-4 rounded-xl flex flex-col items-center justify-center gap-2 text-center transition-all hover:scale-105"
                style="background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);"
            >
              <BookOpen class="w-6 h-6 text-white"/>
              <span class="text-white text-sm font-medium">读书空间</span>
              <span class="text-white/80 text-xs">共读计划</span>
            </button>
            <button
                @click="viewMore('面试指南')"
                class="p-4 rounded-xl flex flex-col items-center justify-center gap-2 text-center transition-all hover:scale-105"
                style="background: linear-gradient(135deg, #f093fb 0%, #f5576c 100%);"
            >
              <Briefcase class="w-6 h-6 text-white"/>
              <span class="text-white text-sm font-medium">面试指南</span>
              <span class="text-white/80 text-xs">大厂面经</span>
            </button>
          </div>
        </div>
      </div>
    </div>

    <!-- 4. 专题推荐区 -->
    <div class="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 py-6 sm:py-8 lg:py-10">
      <div class="border border-gray-200 rounded-xl p-4 sm:p-6" style="background-color: var(--theme-bg);">
        <!-- 读书空间专题 -->
        <div class="mb-8 sm:mb-10">
          <div class="flex items-center justify-between mb-4 sm:mb-5">
            <div class="flex items-center gap-2 sm:gap-3">
              <div
                  class="w-10 h-10 sm:w-11 sm:h-11 bg-gradient-to-br from-emerald-100 to-teal-100 rounded-xl flex items-center justify-center">
                <Book class="w-5 h-5 sm:w-5.5 sm:h-5.5 text-emerald-600"/>
              </div>
              <div>
                <h2 class="text-lg sm:text-xl font-bold" style="color: var(--theme-text);">读书空间</h2>
                <p class="text-xs sm:text-sm" style="color: var(--theme-text-secondary);">每月共读一本书</p>
              </div>
            </div>
            <button @click="viewMore('读书空间')" class="flex items-center gap-1.5 text-xs sm:text-sm font-medium"
                    style="color: var(--theme-text-secondary);">
              <span>进入读书空间</span>
              <ArrowRight class="w-3.5 h-3.5"/>
            </button>
          </div>
          <div class="grid sm:grid-cols-2 lg:grid-cols-4 gap-4 sm:gap-5">
            <div class="p-4 sm:p-5 rounded-xl relative overflow-hidden"
                 style="background: linear-gradient(135deg, #134e5e 0%, #71b280 100%);">
              <div class="absolute -right-4 -top-4 w-24 h-24 bg-white/10 rounded-full"></div>
              <div class="relative">
                <span class="text-emerald-200 text-xs">本月共读</span>
                <h3 class="text-white text-lg font-bold mt-1 mb-2">《代码整洁之道》</h3>
                <p class="text-emerald-100 text-sm mb-4">Robert C. Martin 著</p>
                <button
                    class="px-4 py-2 bg-white text-emerald-700 rounded-full text-sm font-medium hover:bg-emerald-50 transition-colors">
                  立即加入
                </button>
              </div>
            </div>
            <div class="p-4 sm:p-5 rounded-xl" style="background-color: var(--theme-surface);">
              <div class="flex items-center gap-2 mb-3">
                <div class="w-8 h-8 bg-amber-100 rounded-lg flex items-center justify-center">
                  <CheckCircle class="w-4 h-4 text-amber-600"/>
                </div>
                <span class="text-xs font-medium" style="color: var(--theme-text);">热门书单</span>
              </div>
              <ul class="space-y-2 text-sm" style="color: var(--theme-text);">
                <li class="flex items-center gap-2">
                  <span class="w-1.5 h-1.5 rounded-full bg-amber-500"></span>
                  <span>程序员必读书单</span>
                </li>
                <li class="flex items-center gap-2">
                  <span class="w-1.5 h-1.5 rounded-full bg-emerald-500"></span>
                  <span>经典散文集</span>
                </li>
                <li class="flex items-center gap-2">
                  <span class="w-1.5 h-1.5 rounded-full bg-blue-500"></span>
                  <span>AI时代生存指南</span>
                </li>
              </ul>
            </div>
            <div class="p-4 sm:p-5 rounded-xl" style="background-color: var(--theme-surface);">
              <div class="flex items-center gap-2 mb-3">
                <div class="w-8 h-8 bg-blue-100 rounded-lg flex items-center justify-center">
                  <Quote class="w-4 h-4 text-blue-600"/>
                </div>
                <span class="text-xs font-medium" style="color: var(--theme-text);">金句摘录</span>
              </div>
              <p class="text-sm italic" style="color: var(--theme-text);">"代码是写给人看的，顺便给机器执行。"</p>
              <p class="text-xs mt-2" style="color: var(--theme-text-secondary);">——《代码整洁之道》</p>
            </div>
            <div class="p-4 sm:p-5 rounded-xl" style="background-color: var(--theme-surface);">
              <div class="flex items-center gap-2 mb-3">
                <div class="w-8 h-8 bg-purple-100 rounded-lg flex items-center justify-center">
                  <Users class="w-4 h-4 text-purple-600"/>
                </div>
                <span class="text-xs font-medium" style="color: var(--theme-text);">共读进度</span>
              </div>
              <div class="text-center py-2">
                <p class="text-2xl font-bold" style="color: var(--theme-primary);">1,234</p>
                <p class="text-xs" style="color: var(--theme-text-secondary);">人正在共读</p>
              </div>
            </div>
          </div>
        </div>

        <!-- 面试指南专题 -->
        <div>
          <div class="flex items-center justify-between mb-4 sm:mb-5">
            <div class="flex items-center gap-2 sm:gap-3">
              <div
                  class="w-10 h-10 sm:w-11 sm:h-11 bg-gradient-to-br from-purple-100 to-pink-100 rounded-xl flex items-center justify-center">
                <Briefcase class="w-5 h-5 sm:w-5.5 sm:h-5.5 text-purple-600"/>
              </div>
              <div>
                <h2 class="text-lg sm:text-xl font-bold" style="color: var(--theme-text);">面试指南</h2>
                <p class="text-xs sm:text-sm" style="color: var(--theme-text-secondary);">助力职场进阶</p>
              </div>
            </div>
            <button @click="viewMore('面试指南')" class="flex items-center gap-1.5 text-xs sm:text-sm font-medium"
                    style="color: var(--theme-text-secondary);">
              <span>进入面试指南</span>
              <ArrowRight class="w-3.5 h-3.5"/>
            </button>
          </div>
          <div class="grid sm:grid-cols-3 gap-4 sm:gap-5">
            <div class="p-4 sm:p-5 rounded-xl" style="background-color: var(--theme-surface);">
              <div class="flex items-center justify-between mb-3">
                <span class="text-sm font-medium" style="color: var(--theme-text);">真题库</span>
                <span class="text-xs px-2 py-0.5 rounded-full bg-red-100 text-red-600">hot</span>
              </div>
              <ul class="space-y-2">
                <li class="flex items-center justify-between text-sm" style="color: var(--theme-text);">
                  <span>算法题精选</span>
                  <span class="text-xs" style="color: var(--theme-text-secondary);">128道</span>
                </li>
                <li class="flex items-center justify-between text-sm" style="color: var(--theme-text);">
                  <span>系统设计题</span>
                  <span class="text-xs" style="color: var(--theme-text-secondary);">45道</span>
                </li>
                <li class="flex items-center justify-between text-sm" style="color: var(--theme-text);">
                  <span>行为面试题</span>
                  <span class="text-xs" style="color: var(--theme-text-secondary);">36道</span>
                </li>
              </ul>
            </div>
            <div class="p-4 sm:p-5 rounded-xl" style="background-color: var(--theme-surface);">
              <div class="flex items-center justify-between mb-3">
                <span class="text-sm font-medium" style="color: var(--theme-text);">面经复盘</span>
                <span class="text-xs px-2 py-0.5 rounded-full bg-blue-100 text-blue-600">new</span>
              </div>
              <ul class="space-y-2">
                <li class="flex items-center justify-between text-sm" style="color: var(--theme-text);">
                  <span>字节跳动面经</span>
                  <span class="text-xs" style="color: var(--theme-text-secondary);">2024</span>
                </li>
                <li class="flex items-center justify-between text-sm" style="color: var(--theme-text);">
                  <span>阿里巴巴面经</span>
                  <span class="text-xs" style="color: var(--theme-text-secondary);">2024</span>
                </li>
                <li class="flex items-center justify-between text-sm" style="color: var(--theme-text);">
                  <span>腾讯面试复盘</span>
                  <span class="text-xs" style="color: var(--theme-text-secondary);">2024</span>
                </li>
              </ul>
            </div>
            <div class="p-4 sm:p-5 rounded-xl" style="background-color: var(--theme-surface);">
              <div class="flex items-center justify-between mb-3">
                <span class="text-sm font-medium" style="color: var(--theme-text);">简历优化</span>
              </div>
              <ul class="space-y-2">
                <li class="text-sm" style="color: var(--theme-text);">技术亮点提炼</li>
                <li class="text-sm" style="color: var(--theme-text);">STAR 法则书写</li>
                <li class="text-sm" style="color: var(--theme-text);">简历模板下载</li>
              </ul>
            </div>
          </div>
        </div>
      </div>
    </div>

    <!-- 5. 按主题探索 -->
    <div class="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 py-6 sm:py-8 lg:py-10">
      <div class="border border-gray-200 rounded-xl p-4 sm:p-6" style="background-color: var(--theme-bg);">
        <div class="flex items-center justify-between mb-4 sm:mb-5">
          <div class="flex items-center gap-2 sm:gap-3">
            <div
                class="w-10 h-10 sm:w-11 sm:h-11 bg-gradient-to-br from-amber-100 to-orange-100 rounded-xl flex items-center justify-center">
              <Tag class="w-5 h-5 sm:w-5.5 sm:h-5.5 text-amber-600"/>
            </div>
            <div>
              <h2 class="text-lg sm:text-xl font-bold" style="color: var(--theme-text);">按主题探索</h2>
              <p class="text-xs sm:text-sm" style="color: var(--theme-text-secondary);">发现更多精彩内容</p>
            </div>
          </div>
        </div>

        <!-- 主题Tab -->
        <div class="flex flex-wrap gap-2 sm:gap-2.5 mb-4 sm:mb-5 overflow-x-auto pb-2">
          <button
              v-for="theme in themes"
              :key="theme.id"
              @click="selectTheme(theme.id, theme.name)"
              class="px-3 sm:px-4 py-1.5 sm:py-2 rounded-full text-xs sm:text-sm font-medium whitespace-nowrap transition-all"
              :style="getThemeTabStyle(theme.name)"
          >
            {{ theme.name }}
          </button>
        </div>

        <!-- 主题内容 -->
        <div class="p-4 sm:p-5 rounded-2xl" style="background-color: var(--theme-surface);">
          <div class="flex items-center justify-between mb-4">
            <h3 class="font-semibold text-sm sm:text-base" style="color: var(--theme-text);">
              <span style="color: var(--theme-primary);">{{ activeTheme.slice(0, 2) }}</span>
              {{ activeTheme }}精选
            </h3>
            <button @click="viewMore(activeTheme)" class="flex items-center gap-1.5 text-xs sm:text-sm"
                    style="color: var(--theme-text-secondary);">
              <span>查看更多</span>
              <ArrowRight class="w-3.5 h-3.5"/>
            </button>
          </div>
          <div class="grid sm:grid-cols-2 gap-4 sm:gap-5">
            <div
                v-for="(article, index) in getThemeArticles(activeTheme).slice(0, 6)"
                :key="article.id || index"
                @click="router.push('/article/' + (article.id || '1'))"
                class="flex items-center gap-3 cursor-pointer group p-3 rounded-lg hover:bg-gray-50/50 transition-colors"
            >
              <div class="w-1.5 h-1.5 rounded-full flex-shrink-0" style="background-color: var(--theme-primary);"></div>
              <div class="flex-1 min-w-0">
                <span class="text-xs sm:text-sm line-clamp-2" style="color: var(--theme-text);">
                  {{ article.title || `这是${activeTheme}的第${index + 1}篇精选文章，内容精彩值得一读` }}
                </span>
              </div>
              <span class="text-xs flex-shrink-0"
                    style="color: var(--theme-text-secondary);">{{ article.createdAt || '2024-01-15' }}</span>
            </div>
          </div>
        </div>
      </div>
    </div>

    <!-- 6. 墨韵名家录 -->
    <div class="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 py-6 sm:py-8 lg:py-10">
      <div class="border border-gray-200 rounded-xl p-4 sm:p-6" style="background-color: var(--theme-bg);">
        <div class="flex items-center justify-between mb-4 sm:mb-5">
          <div class="flex items-center gap-2 sm:gap-3">
            <div
                class="w-10 h-10 sm:w-11 sm:h-11 bg-gradient-to-br from-cyan-100 to-blue-100 rounded-xl flex items-center justify-center">
              <User class="w-5 h-5 sm:w-5.5 sm:h-5.5 text-blue-600"/>
            </div>
            <div>
              <h2 class="text-lg sm:text-xl font-bold" style="color: var(--theme-text);">墨韵名家录</h2>
              <p class="text-xs sm:text-sm" style="color: var(--theme-text-secondary);">发现优秀创作者</p>
            </div>
          </div>
          <button @click="router.push('/authors')" class="flex items-center gap-1.5 text-xs sm:text-sm"
                  style="color: var(--theme-text-secondary);">
            <span>全部作者</span>
            <ArrowRight class="w-3.5 h-3.5"/>
          </button>
        </div>

        <div class="grid grid-cols-2 sm:grid-cols-3 lg:grid-cols-7 gap-3 sm:gap-4">
          <div
              v-for="author in authors"
              :key="author.id"
              @click="goToAuthor(author.id)"
              class="text-center p-4 sm:p-5 rounded-2xl cursor-pointer transition-all hover:scale-105"
              style="background-color: var(--theme-surface);"
          >
            <div
                class="w-11 h-11 sm:w-12 sm:h-12 mx-auto mb-3 rounded-full bg-gradient-to-br from-red-100 to-orange-100 flex items-center justify-center">
              <span class="text-base sm:text-lg font-bold" style="color: var(--theme-primary);">{{
                  author.avatar
                }}</span>
            </div>
            <p class="font-medium text-xs sm:text-sm mb-1" style="color: var(--theme-text);">{{ author.name }}</p>
            <p class="text-xs mb-1" style="color: var(--theme-text-secondary);">已创作 {{ author.works }} 篇</p>
            <p class="text-xs" style="color: var(--theme-text-secondary);">{{ author.likes }} 喜欢</p>
            <p class="text-xs" style="color: var(--theme-text-secondary);">坚持 {{ author.days }} 天</p>
            <button class="mt-3 px-3 py-1.5 rounded-full text-xs border transition-colors hover:bg-gray-50"
                    style="border-color: var(--theme-primary); color: var(--theme-primary);">
              关注作者
            </button>
          </div>
        </div>
      </div>
    </div>

    <!-- 7. 底部互动区 -->
    <div class="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 py-6 sm:py-8 lg:py-10">
      <div class="border border-gray-200 rounded-xl p-4 sm:p-6" style="background-color: var(--theme-bg);">
        <div class="grid lg:grid-cols-2 gap-6 sm:gap-8">
          <!-- 话题讨论 -->
          <div class="p-4 sm:p-5 rounded-2xl" style="background-color: var(--theme-surface);">
            <div class="flex items-center gap-2 sm:gap-3 mb-4">
              <div
                  class="w-10 h-10 sm:w-11 sm:h-11 bg-gradient-to-br from-pink-100 to-rose-100 rounded-xl flex items-center justify-center">
                <MessageCircle class="w-5 h-5 sm:w-5.5 sm:h-5.5 text-rose-600"/>
              </div>
              <div>
                <h3 class="font-semibold text-sm sm:text-base" style="color: var(--theme-text);">话题讨论</h3>
                <p class="text-xs" style="color: var(--theme-text-secondary);">每周热门话题</p>
              </div>
            </div>
            <div class="space-y-3">
              <div
                  class="p-3 sm:p-4 rounded-xl bg-gradient-to-r from-rose-50 to-pink-50 cursor-pointer hover:from-rose-100 hover:to-pink-100 transition-colors">
                <div class="flex items-start justify-between mb-2">
                  <h4 class="font-medium text-sm" style="color: var(--theme-text);">你最难忘的一顿饭</h4>
                  <span class="text-xs px-2 py-0.5 rounded-full bg-rose-100 text-rose-600">进行中</span>
                </div>
                <p class="text-xs mb-3" style="color: var(--theme-text-secondary);">
                  分享关于美食的记忆，讲述背后的故事</p>
                <div class="flex items-center justify-between">
                  <div class="flex items-center gap-2 text-xs" style="color: var(--theme-text-secondary);">
                    <span>🔥 3.2k 参与</span>
                    <span>💬 856 回复</span>
                  </div>
                  <button class="text-xs text-rose-600 font-medium">参与讨论 →</button>
                </div>
              </div>
              <div class="p-3 sm:p-4 rounded-xl cursor-pointer hover:bg-gray-50 transition-colors"
                   style="background-color: var(--theme-bg);">
                <h4 class="font-medium text-sm mb-1" style="color: var(--theme-text);">2024 我的技术成长计划</h4>
                <div class="flex items-center gap-2 text-xs" style="color: var(--theme-text-secondary);">
                  <span>1.8k 参与</span>
                  <span>423 回复</span>
                </div>
              </div>
              <div class="p-3 sm:p-4 rounded-xl cursor-pointer hover:bg-gray-50 transition-colors"
                   style="background-color: var(--theme-bg);">
                <h4 class="font-medium text-sm mb-1" style="color: var(--theme-text);">你心中的年度好书</h4>
                <div class="flex items-center gap-2 text-xs" style="color: var(--theme-text-secondary);">
                  <span>2.5k 参与</span>
                  <span>678 回复</span>
                </div>
              </div>
            </div>
          </div>

          <!-- 成长打卡 -->
          <div class="p-4 sm:p-5 rounded-2xl" style="background-color: var(--theme-surface);">
            <div class="flex items-center gap-2 sm:gap-3 mb-4">
              <div
                  class="w-10 h-10 sm:w-11 sm:h-11 bg-gradient-to-br from-green-100 to-emerald-100 rounded-xl flex items-center justify-center">
                <CheckCircle class="w-5 h-5 sm:w-5.5 sm:h-5.5 text-emerald-600"/>
              </div>
              <div>
                <h3 class="font-semibold text-sm sm:text-base" style="color: var(--theme-text);">成长打卡</h3>
                <p class="text-xs" style="color: var(--theme-text-secondary);">见证每一天的进步</p>
              </div>
            </div>
            <div class="space-y-3">
              <div class="p-3 sm:p-4 rounded-xl bg-gradient-to-r from-emerald-50 to-green-50">
                <div class="flex items-center justify-between mb-2">
                  <span class="text-xs font-medium text-emerald-700">今日打卡排行</span>
                  <span class="text-xs text-emerald-600">已有 892 人打卡</span>
                </div>
                <div class="space-y-2">
                  <div class="flex items-center gap-3">
                    <span
                        class="w-6 h-6 rounded-full bg-gradient-to-br from-amber-400 to-yellow-500 text-white text-xs flex items-center justify-center font-bold">1</span>
                    <span class="text-sm" style="color: var(--theme-text);">坚持写作 365 天</span>
                    <span class="text-xs ml-auto" style="color: var(--theme-text-secondary);">墨客</span>
                  </div>
                  <div class="flex items-center gap-3">
                    <span
                        class="w-6 h-6 rounded-full bg-gradient-to-br from-gray-300 to-gray-400 text-white text-xs flex items-center justify-center font-bold">2</span>
                    <span class="text-sm" style="color: var(--theme-text);">每日刷题 180 天</span>
                    <span class="text-xs ml-auto" style="color: var(--theme-text-secondary);">代码君</span>
                  </div>
                  <div class="flex items-center gap-3">
                    <span
                        class="w-6 h-6 rounded-full bg-gradient-to-br from-amber-600 to-orange-500 text-white text-xs flex items-center justify-center font-bold">3</span>
                    <span class="text-sm" style="color: var(--theme-text);">阅读打卡 256 天</span>
                    <span class="text-xs ml-auto" style="color: var(--theme-text-secondary);">读书人</span>
                  </div>
                </div>
              </div>
              <button
                  @click="viewMore('社区互动')"
                  class="w-full py-3 sm:py-3.5 rounded-xl font-medium text-sm transition-all hover:opacity-90"
                  style="background: linear-gradient(135deg, #667eea 0%, #764ba2 100%); color: white;"
              >
                立即打卡
              </button>
            </div>
          </div>
        </div>
      </div>
    </div>

    <!-- 8. 热门标签 & 友情链接 -->
    <div class="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 py-6 sm:py-8 lg:py-10">
      <div class="border border-gray-200 rounded-xl p-4 sm:p-6" style="background-color: var(--theme-bg);">
        <div class="space-y-6 sm:space-y-8">
          <!-- 热门标签 -->
          <div class="rounded-2xl p-4 sm:p-5"
               style="background-color: var(--theme-surface); border: 1px solid var(--theme-border);">
            <div class="flex items-center gap-2 mb-4">
              <Star class="w-5 h-5 sm:w-5.5 sm:h-5.5 text-yellow-500"/>
              <h3 class="font-semibold text-sm sm:text-base" style="color: var(--theme-text);">热门标签</h3>
            </div>
            <nav class="flex flex-wrap gap-2">
              <span
                  v-for="tag in ['#生活哲思', '#城市记忆', '#自然写作', '#SpringBoot实战', '#AI辅助开发', '#新手入门', '#面试备战', '#读书心得', '#写作技巧', '#学习方法']"
                  :key="tag"
                  @click="router.push('/list')"
                  class="inline-flex items-center gap-1 px-2.5 sm:px-3 py-1.5 rounded-full text-xs cursor-pointer transition-all hover:opacity-80"
                  style="background-color: var(--theme-accent); color: var(--theme-primary);"
              >
              <Tag class="w-3 h-3"/>
              {{ tag }}
            </span>
            </nav>
          </div>

          <!-- 友情链接 -->
          <div class="rounded-2xl p-4 sm:p-5"
               style="background-color: var(--theme-surface); border: 1px solid var(--theme-border);">
            <div class="flex items-center gap-2 mb-4">
              <BookOpen class="w-5 h-5 sm:w-5.5 sm:h-5.5" style="color: var(--theme-primary);"/>
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
                  style="background-color: var(--theme-bg); border-color: var(--theme-border); color: var(--theme-text-secondary);"
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
    <div class="mt-6 sm:mt-8 lg:mt-10">
      <SiteFooter/>
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
