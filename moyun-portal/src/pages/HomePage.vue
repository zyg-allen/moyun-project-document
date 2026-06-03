<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { RouterLink as Link, useRouter } from 'vue-router'
import { useHead } from '@vueuse/head'
import {
  Search, ChevronRight, ChevronLeft, Star, Flame,
  User, Heart, Eye, Clock, Tag, BookOpen, Calendar,
  MessageSquare, Quote, MoreHorizontal, ArrowRight, Sparkles,
  Mic, Code, Book, Briefcase, Users, Pen, MessageCircle, Upload, CheckCircle
} from 'lucide-vue-next'
import LazyImage from '@/components/LazyImage.vue'
import SiteFooter from '@/components/SiteFooter.vue'
import { generateSeo } from '@/utils/seo'
import { getHomeData, getTagList } from '@/api/article'
import { getFriendLinks } from '@/api/friendLink'
import { getAuthors } from '@/api/user'
import { getCategoryList } from '@/api/category'
import { useUserStore } from '@/stores/user'
import type { FriendLink } from '@/api/friendLink'

const router = useRouter()
const userStore = useUserStore()

interface HeroItem {
  id: string
  image: string
  title: string
  subtitle: string
  author: string
  tag: string
  articleId: string
  tags: string[]
}

const heroImages = ref<HeroItem[]>([])
const currentHeroIndex = ref(0)
const friendLinks = ref<FriendLink[]>([])
const carouselArticles = ref<any[]>([])
const featuredArticles = ref<any[]>([])
const hotArticles = ref<any[]>([])
const latestArticles = ref<any[]>([])
const tags = ref<any[]>([])
const categories = ref<any[]>([])
const authors = ref<any[]>([])
const loading = ref(true)
const error = ref<string | null>(null)

const transformArticle = (apiArticle: any): any => {
  return {
    id: String(apiArticle.id),
    title: apiArticle.title,
    content: apiArticle.content || '',
    excerpt: apiArticle.excerpt || apiArticle.content?.substring(0, 100) || '',
    cover: apiArticle.cover || apiArticle.thumbnail,
    author: {
      id: String(apiArticle.authorId || apiArticle.userId || '1'),
      username: apiArticle.authorNickname || apiArticle.authorUsername || apiArticle.nickname || '作者',
      nickname: apiArticle.authorNickname || apiArticle.authorUsername || apiArticle.nickname || '作者',
      avatar: apiArticle.authorAvatar || 'https://images.unsplash.com/photo-1535713875002-d1d0cf377fde?w=150&h=150&fit=crop',
      bio: apiArticle.authorBio || '热爱写作，分享生活'
    },
    category: apiArticle.categoryName || apiArticle.category || '散文',
    tags: apiArticle.tagNames || apiArticle.tags || [],
    views: Number(apiArticle.views || 0),
    likes: Number(apiArticle.likes || 0),
    comments: Number(apiArticle.comments || 0),
    createdAt: apiArticle.createdAt || apiArticle.createTime || apiArticle.publishedAt || new Date().toISOString().split('T')[0]
  }
}

const loadHomeData = async () => {
  try {
    loading.value = true
    const homeResponse = await getHomeData()
    if (homeResponse.code === 200 && homeResponse.data) {
      carouselArticles.value = homeResponse.data.carouselArticles?.map(transformArticle) || []
      featuredArticles.value = homeResponse.data.featuredArticles?.map(transformArticle) || []
      hotArticles.value = homeResponse.data.hotArticles?.map(transformArticle) || []
      latestArticles.value = homeResponse.data.latestArticles?.map(transformArticle) || []

      if (carouselArticles.value.length > 0) {
        heroImages.value = carouselArticles.value.map((article, index) => ({
          id: String(article.id),
          image: article.cover || `https://images.unsplash.com/photo-${1500000000000 + index * 10000}?w=1200&h=600&fit=crop`,
          title: article.title,
          subtitle: article.excerpt,
          author: '文 / ' + (article.author?.nickname || article.author?.username || '作者'),
          tag: index === 0 ? '今日推荐' : '精选文章',
          articleId: String(article.id),
          tags: article.tags || []
        }))
      }
    }
  } catch (err) {
    console.error('加载首页数据失败:', err)
  }
}

const loadCategories = async () => {
  try {
    const response = await getCategoryList()
    if (response.code === 200 && response.data) {
      categories.value = response.data
    }
  } catch (err) {
    console.error('加载分类失败:', err)
  }
}

const loadTags = async () => {
  try {
    const response = await getTagList()
    if (response.code === 200 && response.data) {
      tags.value = response.data.list || response.data || []
    }
  } catch (err) {
    console.error('加载标签失败:', err)
  }
}

const loadAuthors = async () => {
  try {
    const response = await getAuthors(10)
    if (response.code === 200 && response.data) {
      authors.value = response.data.map((user: any) => ({
        id: String(user.id),
        name: user.nickname || user.username,
        avatar: (user.nickname || user.username || 'A').charAt(0),
        works: Math.floor(Math.random() * 200) + 10,
        likes: Math.floor(Math.random() * 5000) + 100,
        days: Math.floor(Math.random() * 365) + 30
      }))
    }
  } catch (err) {
    console.error('加载名家失败:', err)
  }
}

const loadFriendLinks = async () => {
  try {
    const response = await getFriendLinks()
    if (response.code === 200 && response.data && response.data.list) {
      friendLinks.value = response.data.list
    }
  } catch (error) {
    console.error('加载友情链接失败:', error)
  }
}

const prevHero = () => {
  currentHeroIndex.value = (currentHeroIndex.value - 1 + heroImages.value.length) % heroImages.value.length
}

const nextHero = () => {
  currentHeroIndex.value = (currentHeroIndex.value + 1) % heroImages.value.length
}

const themes = computed(() => {
  return categories.value.map((cat) => ({
    id: String(cat.id),
    name: cat.name,
    key: cat.slug || cat.name
  }))
})

const activeTheme = ref('')

const trendingArticles = computed(() => hotArticles.value.slice(0, 6))

onMounted(async () => {
  await Promise.all([
    loadHomeData(),
    loadCategories(),
    loadTags(),
    loadAuthors(),
    loadFriendLinks()
  ])
  if (categories.value.length > 0) {
    activeTheme.value = categories.value[0].name
  } else {
    activeTheme.value = '散文'
  }
  loading.value = false

  if (heroImages.value.length === 0) {
    setInterval(() => {
      nextHero()
    }, 5000)
  }
})

const selectTheme = (themeId: string, themeName: string) => {
  activeTheme.value = themeName
}

const viewMore = (themeName: string) => {
  router.push({ path: '/list', query: { category: themeName } })
}

const goToAuthor = (id: string) => {
  router.push(`/author/${id}`)
}

const getThemeArticles = (themeName: string): any[] => {
  const filtered = latestArticles.value.filter(article => {
    return article.category === themeName || themeName === ''
  })
  return filtered.length > 0 ? filtered.slice(0, 8) : latestArticles.value.slice(0, 8)
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

const getThemeCode = (themeName: string) => {
  return themeName.substring(0, 2)
}

const handleWrite = () => {
  if (!userStore.isAuthenticated) {
    router.push('/login')
    return
  }
  router.push('/publish')
}

useHead(
    generateSeo({
      title: '首页 - 一纸墨',
      description: '一纸墨 - 在浮躁的世界，留一页纸给灵魂。学习、记录、散文、分享。',
      keywords: ['文学', '散文', '创作', '技术', '阅读', '社区', '一纸墨'],
      type: 'website'
    })
)
</script>

<template>
  <div style="background-color: var(--theme-bg); min-height: 100vh;" class="flex flex-col">
    <!-- 1. 首屏轮播区 -->
    <div class="py-4 sm:py-6" style="background-color: var(--theme-bg);">
      <div class="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8">
        <!-- 品牌口号 -->
        <div class="text-center mb-4 sm:mb-6">
          <p class="text-sm sm:text-base text-gray-600">
            在浮躁的世界，留一页纸给灵魂。
          </p>
          <p class="text-xs sm:text-sm text-gray-400 mt-1">
            我有一纸墨，足以慰风尘。让生活更有趣味。
          </p>
        </div>

        <div class="relative h-[280px] sm:h-[320px] md:h-[380px] overflow-hidden rounded-xl shadow-lg">
          <div v-if="heroImages.length > 0">
            <div class="absolute inset-0 bg-gradient-to-b from-transparent via-transparent to-black/70"></div>
            <LazyImage
                :key="currentHeroIndex"
                :src="heroImages[currentHeroIndex].image"
                :alt="heroImages[currentHeroIndex].title"
                class="w-full h-full object-cover"
            />
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
          <div v-else class="w-full h-full flex items-center justify-center bg-gray-200">
            <p class="text-gray-500">暂无轮播文章</p>
          </div>
        </div>
      </div>
    </div>

    <!-- 2. 名言提示和写作入口 -->
    <div>
      <div class="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8">
        <div class="grid grid-cols-1 md:grid-cols-2 gap-0">
          <div class="py-3 sm:py-4 px-4" style="background-color: var(--theme-surface);">
            <div class="flex items-start gap-2 sm:gap-3">
              <Quote class="w-5 h-5 sm:w-6 sm:h-6 opacity-30 flex-shrink-0 mt-0.5" style="color: var(--theme-primary);" />
              <div>
                <p class="text-sm sm:text-base italic" style="color: var(--theme-text);">
                  "世间所有的相遇，都是久别重逢。"
                </p>
                <p class="text-xs mt-1" style="color: var(--theme-text-secondary);">—— 木心</p>
              </div>
            </div>
          </div>
          <button
              @click="handleWrite"
              class="flex items-center justify-between py-3 sm:py-4 px-4 text-left hover:opacity-90 transition-opacity"
              style="background-color: #dc2626;"
          >
            <div>
              <p class="text-white font-semibold text-sm sm:text-base">今天，写点什么？</p>
              <p class="text-red-100 text-xs">写下即是沉淀，分享即是力量。</p>
            </div>
            <div class="w-7 h-7 sm:w-8 sm:h-8 bg-white/20 rounded-full flex items-center justify-center">
              <Sparkles class="w-3.5 h-3.5 sm:w-4 sm:h-4 text-white" />
            </div>
          </button>
        </div>
      </div>
    </div>

    <!-- 3. 本栏推荐 + 热门推荐 -->
    <div class="py-4 sm:py-6" style="background-color: var(--theme-bg);">
      <div class="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8">
        <div class="grid lg:grid-cols-[2fr_1fr] gap-4 sm:gap-6">
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
                  v-for="(article, index) in featuredArticles.slice(0, 8)"
                  :key="article.id"
                  @click="router.push('/article/' + article.id)"
                  class="group flex gap-2 sm:gap-3 p-2 sm:p-3 rounded-lg cursor-pointer transition-colors"
                  :style="{ backgroundColor: 'var(--theme-surface)' }"
              >
                <div class="relative w-20 h-14 sm:w-24 sm:h-16 flex-shrink-0">
                  <LazyImage
                      :src="article.cover || 'https://images.unsplash.com/photo-1504198453319-5ce911bafcde?w=200&h=150&fit=crop'"
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
                    <span>{{ article.author?.username || article.author?.nickname || '作者' }}</span>
                    <span>{{ article.createdAt }}</span>
                    <span>{{ article.views }} 阅读</span>
                  </div>
                </div>
              </div>
            </div>
          </div>

          <div class="space-y-4 sm:space-y-6">
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

            <div class="grid grid-cols-2 gap-3">
              <button
                  @click="router.push('/reading')"
                  class="p-4 rounded-xl flex flex-col items-center justify-center gap-2 text-center transition-all hover:scale-105"
                  style="background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);"
              >
                <BookOpen class="w-6 h-6 text-white" />
                <span class="text-white text-sm font-medium">读书空间</span>
                <span class="text-white/80 text-xs">共读计划</span>
              </button>
              <button
                  @click="router.push('/interview')"
                  class="p-4 rounded-xl flex flex-col items-center justify-center gap-2 text-center transition-all hover:scale-105"
                  style="background: linear-gradient(135deg, #f093fb 0%, #f5576c 100%);"
              >
                <Briefcase class="w-6 h-6 text-white" />
                <span class="text-white text-sm font-medium">面试指南</span>
                <span class="text-white/80 text-xs">大厂面经</span>
              </button>
            </div>
          </div>
        </div>
      </div>
    </div>

    <!-- 4. 读书空间 -->
    <div class="py-4 sm:py-6" style="background-color: var(--theme-bg);">
      <div class="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8">
        <div class="p-4 sm:p-5 rounded-xl" style="background-color: var(--theme-surface);">
          <div class="flex items-center justify-between mb-4">
            <div class="flex items-center gap-2">
              <div class="w-8 h-8 rounded-lg bg-green-100 flex items-center justify-center">
                <Book class="w-4 h-4 text-green-600" />
              </div>
              <div>
                <h3 class="font-semibold text-sm sm:text-base" style="color: var(--theme-text);">读书空间</h3>
                <p class="text-xs" style="color: var(--theme-text-secondary);">每月共读一本书</p>
              </div>
            </div>
            <button @click="router.push('/reading')" class="flex items-center gap-1 text-xs sm:text-sm font-medium" style="color: var(--theme-primary);">
              <span>进入读书空间</span>
              <ArrowRight class="w-3 h-3 sm:w-4 sm:h-4" />
            </button>
          </div>

          <div class="grid grid-cols-1 md:grid-cols-4 gap-3 sm:gap-4">
            <div class="relative h-28 sm:h-32 rounded-xl overflow-hidden">
              <div class="absolute inset-0 bg-gradient-to-br from-green-600 to-green-800"></div>
              <div class="absolute inset-0 p-3 sm:p-4">
                <span class="inline-block px-2 py-0.5 bg-white/20 backdrop-blur text-white text-xs rounded mb-2">本月共读</span>
                <h4 class="text-white font-bold text-sm sm:text-base mb-1">《代码整洁之道》</h4>
                <p class="text-white/80 text-xs mb-3">Robert C. Martin 著</p>
                <button class="px-3 py-1 bg-white text-green-700 rounded-full text-xs font-medium">立即加入</button>
              </div>
            </div>

            <div class="p-3 sm:p-4 rounded-xl" style="background-color: var(--theme-bg);">
              <div class="flex items-center gap-2 mb-3">
                <div class="w-7 h-7 rounded-lg bg-orange-100 flex items-center justify-center">
                  <Flame class="w-3.5 h-3.5 text-orange-500" />
                </div>
                <span class="font-medium text-xs sm:text-sm" style="color: var(--theme-text);">热门书单</span>
              </div>
              <div class="space-y-2.5">
                <div class="flex items-center gap-2">
                  <div class="w-1.5 h-1.5 rounded-full bg-orange-500"></div>
                  <span class="text-xs" style="color: var(--theme-text-secondary);">程序员必读书单</span>
                </div>
                <div class="flex items-center gap-2">
                  <div class="w-1.5 h-1.5 rounded-full bg-orange-400"></div>
                  <span class="text-xs" style="color: var(--theme-text-secondary);">经典散文集</span>
                </div>
                <div class="flex items-center gap-2">
                  <div class="w-1.5 h-1.5 rounded-full bg-blue-400"></div>
                  <span class="text-xs" style="color: var(--theme-text-secondary);">AI时代生存指南</span>
                </div>
              </div>
            </div>

            <div class="p-3 sm:p-4 rounded-xl" style="background-color: var(--theme-bg);">
              <div class="flex items-center gap-2 mb-3">
                <div class="w-7 h-7 rounded-lg bg-purple-100 flex items-center justify-center">
                  <Quote class="w-3.5 h-3.5 text-purple-500" />
                </div>
                <span class="font-medium text-xs sm:text-sm" style="color: var(--theme-text);">金句摘录</span>
              </div>
              <p class="text-xs sm:text-sm italic" style="color: var(--theme-text-secondary);">
                "代码是写给人看的，顺便给机器执行。"
              </p>
              <p class="text-xs mt-2" style="color: var(--theme-text-secondary);">——《代码整洁之道》</p>
            </div>

            <div class="p-3 sm:p-4 rounded-xl" style="background-color: var(--theme-bg);">
              <div class="flex items-center gap-2 mb-3">
                <div class="w-7 h-7 rounded-lg bg-pink-100 flex items-center justify-center">
                  <Users class="w-3.5 h-3.5 text-pink-500" />
                </div>
                <span class="font-medium text-xs sm:text-sm" style="color: var(--theme-text);">共读进度</span>
              </div>
              <div class="text-center">
                <p class="text-2xl sm:text-3xl font-bold text-red-600">1,234</p>
                <p class="text-xs mt-1" style="color: var(--theme-text-secondary);">人正在共读</p>
              </div>
            </div>
          </div>
        </div>
      </div>
    </div>

    <!-- 5. 面试指南 -->
    <div class="py-4 sm:py-6" style="background-color: var(--theme-bg);">
      <div class="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8">
        <div class="p-4 sm:p-5 rounded-xl" style="background-color: var(--theme-surface);">
          <div class="flex items-center justify-between mb-4">
            <div class="flex items-center gap-2">
              <div class="w-8 h-8 rounded-lg bg-purple-100 flex items-center justify-center">
                <Briefcase class="w-4 h-4 text-purple-600" />
              </div>
              <div>
                <h3 class="font-semibold text-sm sm:text-base" style="color: var(--theme-text);">面试指南</h3>
                <p class="text-xs" style="color: var(--theme-text-secondary);">助力职场进阶</p>
              </div>
            </div>
            <button @click="router.push('/interview')" class="flex items-center gap-1 text-xs sm:text-sm font-medium" style="color: var(--theme-primary);">
              <span>进入面试指南</span>
              <ArrowRight class="w-3 h-3 sm:w-4 sm:h-4" />
            </button>
          </div>

          <div class="grid grid-cols-1 md:grid-cols-3 gap-3 sm:gap-4">
            <div class="p-3 sm:p-4 rounded-xl" style="background-color: var(--theme-bg);">
              <div class="flex items-center justify-between mb-3">
                <span class="font-medium text-xs sm:text-sm" style="color: var(--theme-text);">真题库</span>
                <span class="px-2 py-0.5 bg-red-100 text-red-600 rounded text-xs">hot</span>
              </div>
              <div class="space-y-2.5">
                <div class="flex items-center justify-between">
                  <span class="text-xs" style="color: var(--theme-text-secondary);">算法精选</span>
                  <span class="text-xs" style="color: var(--theme-text-secondary);">1,280道</span>
                </div>
                <div class="flex items-center justify-between">
                  <span class="text-xs" style="color: var(--theme-text-secondary);">系统设计题</span>
                  <span class="text-xs" style="color: var(--theme-text-secondary);">45道</span>
                </div>
                <div class="flex items-center justify-between">
                  <span class="text-xs" style="color: var(--theme-text-secondary);">行为面试题</span>
                  <span class="text-xs" style="color: var(--theme-text-secondary);">36道</span>
                </div>
              </div>
            </div>

            <div class="p-3 sm:p-4 rounded-xl" style="background-color: var(--theme-bg);">
              <div class="flex items-center justify-between mb-3">
                <span class="font-medium text-xs sm:text-sm" style="color: var(--theme-text);">面经复盘</span>
                <span class="px-2 py-0.5 bg-blue-100 text-blue-600 rounded text-xs">new</span>
              </div>
              <div class="space-y-2.5">
                <div class="flex items-center justify-between">
                  <span class="text-xs" style="color: var(--theme-text-secondary);">字节跳动面经</span>
                  <span class="text-xs" style="color: var(--theme-text-secondary);">2024</span>
                </div>
                <div class="flex items-center justify-between">
                  <span class="text-xs" style="color: var(--theme-text-secondary);">阿里巴巴面经</span>
                  <span class="text-xs" style="color: var(--theme-text-secondary);">2024</span>
                </div>
                <div class="flex items-center justify-between">
                  <span class="text-xs" style="color: var(--theme-text-secondary);">腾讯面试复盘</span>
                  <span class="text-xs" style="color: var(--theme-text-secondary);">2024</span>
                </div>
              </div>
            </div>

            <div class="p-3 sm:p-4 rounded-xl" style="background-color: var(--theme-bg);">
              <div class="flex items-center justify-between mb-3">
                <span class="font-medium text-xs sm:text-sm" style="color: var(--theme-text);">简历优化</span>
              </div>
              <div class="space-y-2.5">
                <div class="flex items-center justify-between">
                  <span class="text-xs" style="color: var(--theme-text-secondary);">技术亮点提炼</span>
                </div>
                <div class="flex items-center justify-between">
                  <span class="text-xs" style="color: var(--theme-text-secondary);">STAR法则书写</span>
                </div>
                <div class="flex items-center justify-between">
                  <span class="text-xs" style="color: var(--theme-text-secondary);">简历模板下载</span>
                </div>
              </div>
            </div>
          </div>
        </div>
      </div>
    </div>

    <!-- 6. 按主题探索 -->
    <div class="py-4 sm:py-6 border-t" style="background-color: var(--theme-bg); border-color: var(--theme-border);">
      <div class="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8">
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

        <div class="flex flex-wrap gap-1.5 sm:gap-2 mb-3 sm:mb-4">
          <button
              v-for="theme in (themes.length > 0 ? themes : [{ id: '1', name: '散文', key: 'prose' }])"
              :key="theme.id"
              @click="selectTheme(theme.id, theme.name)"
              class="px-3 sm:px-4 py-1 sm:py-1.5 rounded-full text-xs sm:text-sm font-medium transition-all"
              :style="getThemeTabStyle(theme.name)"
          >
            {{ theme.name }}
          </button>
        </div>

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

    <!-- 7. 墨韵名家录 -->
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
              v-for="author in (authors.length > 0 ? authors : [{ id: '1', name: '林清玄', avatar: '林', works: 318, likes: '5.6w', days: 365 }])"
              :key="author.id"
              @click="goToAuthor(author.id)"
              class="text-center p-3 sm:p-4 rounded-xl cursor-pointer transition-colors"
              :style="{ backgroundColor: 'var(--theme-surface)' }"
          >
            <div class="w-10 h-10 sm:w-12 sm:h-12 mx-auto mb-2 rounded-full bg-gradient-to-br from-red-100 to-orange-100 flex items-center justify-center">
              <span class="text-sm font-bold" style="color: var(--theme-primary);">{{ author.avatar }}</span>
            </div>
            <p class="font-medium text-xs sm:text-sm mb-1" style="color: var(--theme-text);">{{ author.name }}</p>
            <p class="text-xs" style="color: var(--theme-text-secondary);">已创作 {{ author.works }} 篇</p>
            <p class="text-xs" style="color: var(--theme-text-secondary);">{{ author.likes }} 喜欢</p>
            <p class="text-xs" style="color: var(--theme-text-secondary);">坚持 {{ author.days }} 天</p>
            <button class="mt-2 px-3 py-1 rounded-full text-xs border" style="border-color: var(--theme-primary); color: var(--theme-primary);">
              关注作者
            </button>
          </div>
        </div>
      </div>
    </div>

    <!-- 8. 热门标签 & 友情链接 -->
    <div class="py-4 sm:py-6 border-t" style="background-color: var(--theme-surface); border-color: var(--theme-border);">
      <div class="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8">
        <div class="space-y-6 sm:space-y-8">
          <div class="rounded-xl p-4 sm:p-5" style="background-color: var(--theme-bg);">
            <div class="flex items-center gap-2 mb-3">
              <Star class="w-4 h-4 sm:w-5 sm:h-5 text-yellow-500" />
              <h3 class="font-semibold text-sm sm:text-base" style="color: var(--theme-text);">热门标签</h3>
            </div>
            <nav class="flex flex-wrap gap-1.5 sm:gap-2">
              <span
                  v-for="tag in (tags.length > 0 ? tags : [{ id: '1', name: '文学' }, { id: '2', name: '散文' }, { id: '3', name: '随笔' }])"
                  :key="tag.id || tag"
                  @click="router.push('/list')"
                  class="inline-flex items-center gap-1 px-2.5 sm:px-3 py-1 sm:py-1.5 rounded-full text-xs cursor-pointer transition-all hover:opacity-80"
                  :style="{ backgroundColor: 'var(--theme-accent)', color: 'var(--theme-primary)' }"
              >
                <Tag class="w-3 h-3" />
                {{ tag.name || tag }}
              </span>
            </nav>
          </div>

          <div class="rounded-xl p-4 sm:p-5" style="background-color: var(--theme-bg);">
            <div class="flex items-center gap-2 mb-3">
              <BookOpen class="w-4 h-4 sm:w-5 sm:h-5" style="color: var(--theme-primary);" />
              <h3 class="font-semibold text-sm sm:text-base" style="color: var(--theme-text);">友情链接</h3>
            </div>
            <nav class="flex flex-wrap gap-2 sm:gap-3">
              <a
                  v-for="link in (friendLinks.length > 0 ? friendLinks : [{ id: '1', name: '中国作家网', url: '#' }])"
                  :key="link.id"
                  :href="link.url"
                  target="_blank"
                  rel="noopener noreferrer"
                  class="inline-flex items-center gap-2 px-3 sm:px-4 py-2 sm:py-2.5 rounded-lg text-xs sm:text-sm cursor-pointer border transition-all hover:opacity-80"
                  :style="{ backgroundColor: 'var(--theme-surface)', borderColor: 'var(--theme-border)', color: 'var(--theme-text-secondary)' }"
              >
                <span>{{ link.name }}</span>
              </a>
            </nav>
          </div>
        </div>
      </div>
    </div>

    <div class="mt-4 sm:mt-6">
      <SiteFooter />
    </div>
  </div>
</template>

<style scoped>
</style>
