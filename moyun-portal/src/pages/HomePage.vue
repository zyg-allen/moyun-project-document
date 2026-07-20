<script setup lang="ts">
import { ref, computed, onMounted, onUnmounted } from 'vue'
import { RouterLink as Link, useRouter } from 'vue-router'
import { useHead } from '@vueuse/head'
import {
  ChevronRight, ChevronLeft, Star, Flame,
  User, Eye, Tag, BookOpen,
  Quote, ArrowRight, Sparkles,
  Book, Briefcase, Users,
  AlertCircle, RefreshCw,
  BarChart3, Network, TrendingUp,
  MessageCircle, Activity, Crown, Target
} from 'lucide-vue-next'
import LazyImage from '@/components/LazyImage.vue'
import SiteFooter from '@/components/SiteFooter.vue'
import BackToTop from '@/components/BackToTop.vue'
import { generateSeo } from '@/utils/seo'
import { transformArticle } from '@/utils/articleTransform'
import * as articleApi from '@/api/article'
import * as categoryApi from '@/api/category'
import { filterCategoryTree, getCategoryTarget } from '@/api/category'
import * as tagApi from '@/api/tag'
import { getFriendLinks } from '@/api/friendLink'
import { getAuthors } from '@/api/user'
import { getReadingHome } from '@/api/reading'
import { getInterviewHome } from '@/api/interview'
import { getHotFeed } from '@/api/feed'
import { getLeaderboard } from '@/api/learnStats'
import { useUserStore } from '@/stores/user'
import { useAuth } from '@/composables/useAuth'
import type { Category } from '@/types/api'

const router = useRouter()
const userStore = useUserStore()
const { requireAuth } = useAuth()

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
const friendLinks = ref<any[]>([])
const carouselArticles = ref<any[]>([])
const featuredArticles = ref<any[]>([])
const hotArticles = ref<any[]>([])
const latestArticles = ref<any[]>([])
const tags = ref<any[]>([])
const categories = ref<Category[]>([])
const authors = ref<any[]>([])
const loading = ref(true)
const error = ref<string | null>(null)
const categoryArticles = ref<Record<string, any[]>>({})

// 读书空间首页数据
const readingBooks = ref<any[]>([])
const readingBookLists = ref<any[]>([])
const readingQuotes = ref<any[]>([])

// 面试空间首页数据
const interviewQuestions = ref<any[]>([])
const interviewExperiences = ref<any[]>([])
const interviewCategories = ref<any[]>([])
const interviewTotalQuestions = ref(0)

// 学习中心首页数据（阶段三核心展示）
const leaderboardTop3 = ref<any[]>([])

// 社区动态预览数据（营造社区氛围）
const hotFeedList = ref<any[]>([])

const loadHomeData = async () => {
  try {
    loading.value = true
    const homeResponse = await articleApi.getHomeData()
    if (homeResponse.code === 200 && homeResponse.data) {
      carouselArticles.value = homeResponse.data.carouselArticles?.map(transformArticle) || []
      featuredArticles.value = homeResponse.data.featuredArticles?.map(transformArticle) || []
      hotArticles.value = homeResponse.data.hotArticles?.map(transformArticle) || []
      latestArticles.value = homeResponse.data.latestArticles?.map(transformArticle) || []

      if (carouselArticles.value.length > 0) {
        heroImages.value = carouselArticles.value.map((article, index) => ({
          id: String(article.id),
          image: article.cover || '',
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
    error.value = '加载首页数据失败，请稍后重试'
  }
}

const retryLoad = async () => {
  error.value = null
  await loadAll()
}

const loadCategories = async () => {
  try {
    const response = await categoryApi.getCategoryTree()
    if (response.code === 200 && response.data) {
      categories.value = response.data
    }
  } catch (err) {
    console.error('加载分类失败:', err)
  }
}

const loadTags = async () => {
  try {
    const response = await tagApi.getHotTags()
    if (response.code === 200 && response.data) {
      tags.value = response.data
    }
  } catch (err) {
    console.error('加载标签失败:', err)
  }
}

const loadAuthors = async () => {
  try {
    const response = await getAuthors(10)
    if (response.code === 200 && response.data) {
      // 后端 /portal/user/authors 已返回真实统计字段 works/views/likes/days
      authors.value = response.data.map((user: any) => ({
        id: String(user.id),
        name: user.nickname || user.username,
        avatar: (user.nickname || user.username || 'A').charAt(0),
        works: Number(user.works || 0),
        likes: Number(user.likes || 0),
        days: Number(user.days || 0)
      }))
    }
  } catch (err) {
    console.error('加载名家失败:', err)
    authors.value = []
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

const loadReadingData = async () => {
  try {
    const response = await getReadingHome()
    if (response.code === 200 && response.data) {
      readingBookLists.value = (response.data.bookLists || []).slice(0, 3)
      readingBooks.value = (response.data.books || []).slice(0, 4)
      readingQuotes.value = (response.data.quotes || []).slice(0, 1)
    }
  } catch (err) {
    console.error('加载读书空间数据失败:', err)
  }
}

const loadInterviewData = async () => {
  try {
    const response = await getInterviewHome()
    if (response.code === 200 && response.data) {
      const d: any = response.data
      interviewCategories.value = (d.categories || []).slice(0, 3)
      interviewQuestions.value = (d.hotQuestions || []).slice(0, 3)
      interviewExperiences.value = (d.hotExperiences || []).slice(0, 3)
      interviewTotalQuestions.value = d.totalQuestionCount || 0
    }
  } catch (err) {
    console.error('加载面试空间数据失败:', err)
  }
}

const loadLeaderboardData = async () => {
  try {
    const response = await getLeaderboard('question', 3)
    if (response.code === 200 && response.data) {
      // 接口返回 Leaderboard，含 list 字段
      const list = (response.data as any).list || response.data || []
      leaderboardTop3.value = Array.isArray(list) ? list.slice(0, 3) : []
    }
  } catch (err) {
    console.error('加载刷题排行榜失败:', err)
    leaderboardTop3.value = []
  }
}

const loadHotFeedData = async () => {
  try {
    const response = await getHotFeed({ pageNum: 1, pageSize: 3 })
    if (response.code === 200 && response.data) {
      // httpGetList 已统一返回 { list, total, page, pageSize }
      hotFeedList.value = (response.data.list || []).slice(0, 3)
    }
  } catch (err) {
    // 游客或冷启动可能无数据，静默失败
    console.error('加载社区动态失败:', err)
    hotFeedList.value = []
  }
}

// 动态卡片的目标跳转路径
const getFeedTargetPath = (item: any): string => {
  if (!item) return '/feed'
  const t = item.targetType || item.eventType
  const id = item.targetId
  if (t === 'article' && id) return `/article/${id}`
  if (t === 'experience' && id) return `/interview/experience/${id}`
  if (t === 'column' && id) return `/column/${id}`
  if (t === 'book' && id) return `/reading/book/${id}`
  return '/feed'
}

// 动态事件类型展示文案
const getFeedActionText = (item: any): string => {
  const t = item.eventType
  switch (t) {
    case 'publish_article': return '发布了文章'
    case 'publish_experience': return '分享了面经'
    case 'new_column': return '创建了专栏'
    case 'checkin': return '完成了签到'
    case 'pass_question': return '通过了一道题'
    default: return '有了新动态'
  }
}

const prevHero = () => {
  currentHeroIndex.value = (currentHeroIndex.value - 1 + heroImages.value.length) % heroImages.value.length
}

const nextHero = () => {
  currentHeroIndex.value = (currentHeroIndex.value + 1) % heroImages.value.length
}

// 轮播定时器（仅当有多张图时启动）
let heroTimer: ReturnType<typeof setInterval> | null = null

const startHeroAutoplay = () => {
  if (heroTimer || heroImages.value.length <= 1) return
  heroTimer = setInterval(() => {
    nextHero()
  }, 5000)
}

const stopHeroAutoplay = () => {
  if (heroTimer) {
    clearInterval(heroTimer)
    heroTimer = null
  }
}

const themes = computed(() => {
  return filterCategoryTree(categories.value)
    .map((cat: Category) => {
      const target = getCategoryTarget(cat)
      return {
        id: String(cat.id),
        name: cat.name,
        key: cat.slug || cat.name,
        path: target.path,
        isExternal: target.type === 'external'
      }
    })
})

const activeTheme = ref('')

const trendingArticles = computed(() => hotArticles.value.slice(0, 6))

const loadAll = async () => {
  try {
    loading.value = true
    await Promise.all([
      loadHomeData(),
      loadCategories(),
      loadTags(),
      loadAuthors(),
      loadFriendLinks(),
      loadReadingData(),
      loadInterviewData(),
      loadLeaderboardData(),
      loadHotFeedData()
    ])
    if (themes.value.length > 0) {
      activeTheme.value = themes.value[0].name
      await loadCategoryArticles(themes.value[0].name)
    } else {
      activeTheme.value = '散文'
    }
  } catch (e) {
    console.error('加载首页数据失败:', e)
    error.value = '加载首页数据失败，请稍后重试'
  } finally {
    loading.value = false
    // 数据加载完成后启动轮播（仅当有多张图时）
    startHeroAutoplay()
  }
}

onMounted(() => {
  loadAll()
})

onUnmounted(() => {
  stopHeroAutoplay()
})

const selectTheme = async (themeId: string, themeName: string) => {
  // 检查是否为外部链接
  const theme = themes.value.find(t => t.id === themeId)
  if (theme && theme.isExternal && theme.path) {
    // 外部链接直接跳转
    window.open(theme.path, '_blank', 'noopener,noreferrer')
    return
  }
  activeTheme.value = themeName
  if (!categoryArticles.value[themeName]) {
    await loadCategoryArticles(themeName)
  }
}

const loadCategoryArticles = async (themeName: string) => {
  try {
    const response = await articleApi.getCategoryRecommendedArticles(themeName, undefined, 8)
    if (response.code === 200 && response.data) {
      const list = (response.data as any).list || response.data || []
      categoryArticles.value[themeName] = list.map(transformArticle)
    }
  } catch (err) {
    console.error('加载分类推荐文章失败:', err)
    categoryArticles.value[themeName] = []
  }
}

const viewMore = (themeName: string) => {
  router.push(`/category/${encodeURIComponent(themeName)}`)
}

const goToAuthor = (id: string) => {
  router.push(`/author/${id}`)
}

const getThemeArticles = (themeName: string): any[] => {
  if (categoryArticles.value[themeName] && categoryArticles.value[themeName].length > 0) {
    return categoryArticles.value[themeName]
  }
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
  if (!requireAuth('/publish')) {
    return;
  }
  router.push('/publish');
}

useHead(
    generateSeo({
      title: '首页',
      description: '墨韵·智库 - 为文学爱好者和技术开发者提供一个纯净的创作与阅读空间，在这里分享技术与生活之美',
      keywords: ['文学', '散文', '技术', '编程', '创作', '阅读', '分享'],
      type: 'website',
      canonicalPath: '/'
    })
)
</script>

<template>
  <div class="min-h-screen flex flex-col" style="background-color: var(--theme-bg);">
    <div v-if="loading" class="min-h-[60vh] flex items-center justify-center" style="background-color: var(--theme-bg);">
      <div class="flex flex-col items-center gap-4">
        <div class="w-10 h-10 rounded-full animate-spin" style="border-width: 3px; border-style: solid; border-color: var(--theme-border); border-top-color: var(--theme-primary);"></div>
        <p class="text-sm" style="color: var(--theme-text-secondary);">正在加载首页内容...</p>
      </div>
    </div>
    <div v-else-if="error" class="min-h-[60vh] flex items-center justify-center px-4" style="background-color: var(--theme-bg);">
      <div class="text-center max-w-md">
        <div class="w-20 h-20 rounded-full flex items-center justify-center mx-auto mb-4" style="background-color: var(--theme-accent);">
          <AlertCircle class="w-10 h-10" style="color: var(--theme-primary);" />
        </div>
        <h3 class="text-xl font-bold mb-2" style="color: var(--theme-text);">加载失败</h3>
        <p class="text-sm mb-6" style="color: var(--theme-text-secondary);">{{ error }}</p>
        <button @click="retryLoad" class="inline-flex items-center gap-2 px-5 py-2.5 rounded-xl text-sm font-medium text-white" style="background-color: var(--theme-primary);">
          <RefreshCw class="w-4 h-4" />
          重新加载
        </button>
      </div>
    </div>
    <template v-else>
    <div class="py-6 sm:py-8" style="background-color: var(--theme-bg);">
      <div class="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8">
        <div class="text-center mb-4 sm:mb-6">
          <p class="text-sm sm:text-base text-gray-600">
            在浮躁的世界，留一页纸给灵魂。
          </p>
          <p class="text-xs sm:text-sm text-gray-400 mt-1">
            我有一纸墨，足以慰风尘。让生活更有趣味。
          </p>
        </div>

        <div class="relative h-[280px] sm:h-[320px] md:h-[380px] overflow-hidden rounded-xl shadow-lg" style="background-color: var(--theme-accent);">
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
                  <div class="inline-flex items-center space-x-2 px-3 py-1 rounded-full text-xs font-medium mb-3 sm:mb-4" style="background-color: var(--theme-primary); color: white;">
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
                        class="inline-flex items-center space-x-2 px-4 sm:px-5 py-2 sm:py-2.5 rounded-full text-sm font-medium hover:opacity-90 transition-opacity"
                        style="background-color: var(--theme-primary); color: white;"
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
                class="absolute left-2 sm:left-3 top-1/2 -translate-y-1/2 w-11 h-11 bg-black/30 backdrop-blur text-white rounded-full flex items-center justify-center hover:bg-black/50 transition-colors"
            >
              <ChevronLeft class="w-4 h-4 sm:w-5 sm:h-5" />
            </button>
            <button
                @click="nextHero"
                class="absolute right-2 sm:right-3 top-1/2 -translate-y-1/2 w-11 h-11 bg-black/30 backdrop-blur text-white rounded-full flex items-center justify-center hover:bg-black/50 transition-colors"
            >
              <ChevronRight class="w-4 h-4 sm:w-5 sm:h-5" />
            </button>
            <div class="absolute bottom-2 right-4 sm:bottom-3 sm:right-6 flex space-x-1.5">
              <button
                  v-for="(_, index) in heroImages"
                  :key="index"
                  @click="currentHeroIndex = index"
                  class="p-2 flex items-center justify-center"
                  :aria-label="`切换到第 ${index + 1} 张`"
              >
                <span
                    :class="['block rounded-full transition-all', currentHeroIndex === index ? 'w-5 h-1.5' : 'w-1.5 h-1.5 bg-white/50 hover:bg-white/70']"
                    :style="currentHeroIndex === index ? { backgroundColor: 'var(--theme-primary)' } : {}"
                ></span>
              </button>
            </div>
          </div>
          <div v-else class="w-full h-full flex items-center justify-center bg-gray-200">
            <p class="text-gray-500">暂无轮播文章</p>
          </div>
        </div>
      </div>
    </div>

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
              style="background-color: var(--theme-primary);"
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

    <div class="py-6 sm:py-8" style="background-color: var(--theme-bg);">
      <div class="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8">
        <div class="grid lg:grid-cols-[2fr_1fr] gap-4 sm:gap-6">
          <div>
            <div class="flex items-center justify-between mb-3 sm:mb-4">
              <div class="flex items-center gap-2">
                <Star class="w-4 h-4 sm:w-5 sm:h-5 text-yellow-500" />
                <h2 class="text-base sm:text-lg font-bold" style="color: var(--theme-text);">本栏推荐</h2>
              </div>
              <button @click="router.push('/category')" class="flex items-center gap-1 text-xs sm:text-sm font-medium" style="color: var(--theme-text-secondary);">
                <span>更多</span>
                <ArrowRight class="w-3 h-3 sm:w-4 sm:h-4" />
              </button>
            </div>
            <div class="space-y-2 sm:space-y-3">
              <button
                  type="button"
                  v-for="(article, index) in featuredArticles.slice(0, 8)"
                  :key="article.id"
                  @click.stop="router.push('/article/' + article.id)"
                  class="group flex gap-2 sm:gap-3 p-2 sm:p-3 rounded-lg cursor-pointer transition-colors w-full text-left"
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
                      class="absolute top-1 left-1 px-1.5 py-0.5 text-white text-xs rounded"
                      style="background-color: var(--theme-primary);"
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
              </button>
            </div>
          </div>

          <div class="space-y-4 sm:space-y-6">
            <div class="p-3 sm:p-4 rounded-xl" style="background-color: var(--theme-surface);">
              <div class="flex items-center gap-2 mb-3 sm:mb-4">
                <Flame class="w-4 h-4 sm:w-5 sm:h-5 text-orange-500" />
                <h3 class="font-semibold text-sm sm:text-base" style="color: var(--theme-text);">热门推荐</h3>
              </div>
              <div class="space-y-2 sm:space-y-3">
                <button
                    type="button"
                    v-for="(article, index) in trendingArticles"
                    :key="article.id"
                    @click.stop="router.push('/article/' + article.id)"
                    class="flex items-start gap-2 cursor-pointer w-full text-left"
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
                </button>
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

    <div class="py-6 sm:py-8" style="background-color: var(--theme-bg);">
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
            <!-- 本月共读 / 精选书籍 -->
            <button
                type="button"
                v-if="readingBooks.length > 0"
                class="relative h-28 sm:h-32 rounded-xl overflow-hidden cursor-pointer w-full text-left"
                @click="router.push(`/reading/book/${readingBooks[0].id}`)"
            >
              <LazyImage
                  :src="readingBooks[0].cover"
                  :alt="readingBooks[0].title"
                  class="absolute inset-0 w-full h-full object-cover"
              />
              <div class="absolute inset-0 bg-gradient-to-br from-green-600/80 to-green-800/80 p-3 sm:p-4">
                <span class="inline-block px-2 py-0.5 bg-white/20 backdrop-blur text-white text-xs rounded mb-2">本月共读</span>
                <h4 class="text-white font-bold text-sm sm:text-base mb-1 line-clamp-1">{{ readingBooks[0].title }}</h4>
                <p class="text-white/80 text-xs mb-3">{{ readingBooks[0].author }}</p>
                <span class="px-3 py-1 bg-white text-green-700 rounded-full text-xs font-medium">立即阅读</span>
              </div>
            </button>
            <div v-else class="relative h-28 sm:h-32 rounded-xl overflow-hidden">
              <div class="absolute inset-0 bg-gradient-to-br from-green-600 to-green-800"></div>
              <div class="absolute inset-0 p-3 sm:p-4">
                <span class="inline-block px-2 py-0.5 bg-white/20 backdrop-blur text-white text-xs rounded mb-2">本月共读</span>
                <h4 class="text-white font-bold text-sm sm:text-base mb-1">暂无推荐</h4>
              </div>
            </div>

            <!-- 热门书单 -->
            <div class="p-3 sm:p-4 rounded-xl" style="background-color: var(--theme-bg);">
              <div class="flex items-center gap-2 mb-3">
                <div class="w-7 h-7 rounded-lg bg-orange-100 flex items-center justify-center">
                  <Flame class="w-3.5 h-3.5 text-orange-500" />
                </div>
                <span class="font-medium text-xs sm:text-sm" style="color: var(--theme-text);">热门书单</span>
              </div>
              <div class="space-y-2.5">
                <button
                    type="button"
                    v-for="bl in readingBookLists"
                    :key="bl.id"
                    class="flex items-center gap-2 cursor-pointer hover:text-orange-500 transition-colors w-full text-left"
                    @click="router.push(`/reading/book-list/${bl.id}`)"
                >
                  <div class="w-1.5 h-1.5 rounded-full bg-orange-500"></div>
                  <span class="text-xs line-clamp-1" style="color: var(--theme-text-secondary);">{{ bl.title }}</span>
                </button>
                <div v-if="readingBookLists.length === 0" class="text-xs" style="color: var(--theme-text-secondary);">暂无书单</div>
              </div>
            </div>

            <!-- 金句摘录 -->
            <div class="p-3 sm:p-4 rounded-xl" style="background-color: var(--theme-bg);">
              <div class="flex items-center gap-2 mb-3">
                <div class="w-7 h-7 rounded-lg bg-purple-100 flex items-center justify-center">
                  <Quote class="w-3.5 h-3.5 text-purple-500" />
                </div>
                <span class="font-medium text-xs sm:text-sm" style="color: var(--theme-text);">金句摘录</span>
              </div>
              <p v-if="readingQuotes.length > 0" class="text-xs sm:text-sm italic line-clamp-3" style="color: var(--theme-text-secondary);">
                "{{ readingQuotes[0].content }}"
              </p>
              <p v-else class="text-xs" style="color: var(--theme-text-secondary);">暂无金句</p>
            </div>

            <!-- 共读统计 -->
            <div class="p-3 sm:p-4 rounded-xl" style="background-color: var(--theme-bg);">
              <div class="flex items-center gap-2 mb-3">
                <div class="w-7 h-7 rounded-lg bg-pink-100 flex items-center justify-center">
                  <Users class="w-3.5 h-3.5 text-pink-500" />
                </div>
                <span class="font-medium text-xs sm:text-sm" style="color: var(--theme-text);">读书统计</span>
              </div>
              <div class="text-center">
                <p class="text-2xl sm:text-3xl font-bold text-green-600">{{ readingBooks.length + readingBookLists.length }}</p>
                <p class="text-xs mt-1" style="color: var(--theme-text-secondary);">本精选好书</p>
              </div>
            </div>
          </div>
        </div>
      </div>
    </div>

    <div class="py-6 sm:py-8" style="background-color: var(--theme-bg);">
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
            <!-- 热门题目 -->
            <div class="p-3 sm:p-4 rounded-xl" style="background-color: var(--theme-bg);">
              <div class="flex items-center justify-between mb-3">
                <span class="font-medium text-xs sm:text-sm" style="color: var(--theme-text);">热门题目</span>
                <span class="px-2 py-0.5 bg-red-100 text-red-600 rounded text-xs">hot</span>
              </div>
              <div class="space-y-2.5">
                <button
                    type="button"
                    v-for="q in interviewQuestions"
                    :key="q.id"
                    class="flex items-center justify-between cursor-pointer hover:text-blue-500 transition-colors w-full text-left"
                    @click="router.push(`/interview/question/${q.id}`)"
                >
                  <span class="text-xs line-clamp-1" style="color: var(--theme-text-secondary);">{{ q.title }}</span>
                  <span class="text-xs flex-shrink-0 ml-2" style="color: var(--theme-text-secondary);">{{ q.submissionCount || 0 }}提交</span>
                </button>
                <div v-if="interviewQuestions.length === 0" class="text-xs" style="color: var(--theme-text-secondary);">暂无题目</div>
              </div>
            </div>

            <!-- 热门面经 -->
            <div class="p-3 sm:p-4 rounded-xl" style="background-color: var(--theme-bg);">
              <div class="flex items-center justify-between mb-3">
                <span class="font-medium text-xs sm:text-sm" style="color: var(--theme-text);">面经复盘</span>
                <span class="px-2 py-0.5 bg-blue-100 text-blue-600 rounded text-xs">new</span>
              </div>
              <div class="space-y-2.5">
                <button
                    type="button"
                    v-for="exp in interviewExperiences"
                    :key="exp.id"
                    class="flex items-center justify-between cursor-pointer hover:text-blue-500 transition-colors w-full text-left"
                    @click="router.push(`/interview/experience/${exp.id}`)"
                >
                  <span class="text-xs line-clamp-1" style="color: var(--theme-text-secondary);">{{ exp.title }}</span>
                  <span v-if="exp.company" class="text-xs flex-shrink-0 ml-2" style="color: var(--theme-text-secondary);">{{ exp.company }}</span>
                </button>
                <div v-if="interviewExperiences.length === 0" class="text-xs" style="color: var(--theme-text-secondary);">暂无面经</div>
              </div>
            </div>

            <!-- 题目分类 -->
            <div class="p-3 sm:p-4 rounded-xl" style="background-color: var(--theme-bg);">
              <div class="flex items-center justify-between mb-3">
                <span class="font-medium text-xs sm:text-sm" style="color: var(--theme-text);">题目分类</span>
              </div>
              <div class="space-y-2.5">
                <button
                    type="button"
                    v-for="cat in interviewCategories"
                    :key="cat.id"
                    class="flex items-center justify-between cursor-pointer hover:text-blue-500 transition-colors w-full text-left"
                    @click="router.push('/interview/questions')"
                >
                  <span class="text-xs line-clamp-1" style="color: var(--theme-text-secondary);">{{ cat.name }}</span>
                  <span class="text-xs flex-shrink-0 ml-2" style="color: var(--theme-text-secondary);">{{ cat.questionCount || 0 }}道</span>
                </button>
                <div v-if="interviewCategories.length === 0" class="text-xs" style="color: var(--theme-text-secondary);">暂无分类</div>
              </div>
            </div>
          </div>
        </div>
      </div>
    </div>

    <div class="py-6 sm:py-8" style="background-color: var(--theme-bg);">
      <div class="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8">
        <div class="rounded-2xl overflow-hidden shadow-lg" style="background: linear-gradient(135deg, #4f46e5 0%, #7c3aed 100%);">
          <!-- 学习中心 标题栏 -->
          <div class="flex items-center justify-between px-5 sm:px-7 py-4 sm:py-5">
            <div class="flex items-center gap-2 sm:gap-3">
              <div class="w-9 h-9 sm:w-10 sm:h-10 rounded-xl bg-white/20 backdrop-blur flex items-center justify-center">
                <Target class="w-5 h-5 text-white" />
              </div>
              <div>
                <h2 class="text-base sm:text-lg font-bold text-white">学习中心</h2>
                <p class="text-white/80 text-xs">刷题有计划 · 学习有同伴 · 成长看得见</p>
              </div>
            </div>
            <button
                @click="router.push('/learn')"
                class="inline-flex items-center gap-1 px-3 sm:px-4 py-1.5 sm:py-2 rounded-full text-xs sm:text-sm font-medium bg-white text-indigo-700 hover:bg-indigo-50 transition-colors"
            >
              <span>进入学习中心</span>
              <ArrowRight class="w-3 h-3 sm:w-4 sm:h-4" />
            </button>
          </div>

          <!-- 三栏卡片 -->
          <div class="grid grid-cols-1 md:grid-cols-3 gap-0 bg-white/5 backdrop-blur">
            <!-- 1. 刷题排行榜 Top3 -->
            <div class="p-4 sm:p-5 border-t md:border-t-0 md:border-r border-white/10">
              <div class="flex items-center gap-2 mb-3">
                <Crown class="w-4 h-4 text-yellow-300" />
                <h3 class="font-semibold text-sm text-white">刷题排行榜</h3>
              </div>
              <div v-if="leaderboardTop3.length > 0" class="space-y-2">
                <button
                    v-for="(item, idx) in leaderboardTop3"
                    :key="item.userId"
                    @click="router.push(`/learn/leaderboard`)"
                    class="w-full flex items-center gap-3 px-2 py-2 rounded-lg hover:bg-white/10 transition-colors text-left"
                >
                  <span
                      class="w-6 h-6 rounded-full flex items-center justify-center text-xs font-bold flex-shrink-0"
                      :class="idx === 0 ? 'bg-yellow-400 text-yellow-900' : idx === 1 ? 'bg-gray-300 text-gray-800' : 'bg-orange-400 text-orange-900'"
                  >
                    {{ idx + 1 }}
                  </span>
                  <span class="text-sm text-white flex-1 truncate">{{ item.nickname }}</span>
                  <span class="text-xs text-white/70 flex-shrink-0">{{ item.value }} 题</span>
                </button>
              </div>
              <div v-else class="py-6 text-center">
                <p class="text-white/60 text-xs">榜单空缺中，等你来登顶</p>
                <button
                    @click="router.push('/learn/leaderboard')"
                    class="mt-2 inline-flex items-center gap-1 px-3 py-1.5 rounded-full text-xs bg-white/20 text-white hover:bg-white/30 transition-colors"
                >
                  查看完整榜单
                  <ArrowRight class="w-3 h-3" />
                </button>
              </div>
            </div>

            <!-- 2. 知识图谱入口 -->
            <button
                @click="router.push('/learn/knowledge')"
                class="p-4 sm:p-5 border-t md:border-t-0 md:border-r border-white/10 text-left hover:bg-white/10 transition-colors group"
            >
              <div class="flex items-center gap-2 mb-3">
                <Network class="w-4 h-4 text-cyan-300" />
                <h3 class="font-semibold text-sm text-white">知识图谱</h3>
              </div>
              <p class="text-white/80 text-xs leading-relaxed mb-3">
                可视化你的知识结构，发现薄弱点，按图谱强化复习
              </p>
              <div class="flex items-center gap-2">
                <BarChart3 class="w-3.5 h-3.5 text-cyan-300" />
                <span class="text-xs text-cyan-200 group-hover:underline">查看我的知识网络 →</span>
              </div>
            </button>

            <!-- 3. 刷题日历入口（登录后可见热力图，未登录引导登录） -->
            <button
                @click="router.push(userStore.isAuthenticated ? '/learn/calendar' : '/login')"
                class="p-4 sm:p-5 border-t md:border-t-0 border-white/10 text-left hover:bg-white/10 transition-colors group"
            >
              <div class="flex items-center gap-2 mb-3">
                <Activity class="w-4 h-4 text-pink-300" />
                <h3 class="font-semibold text-sm text-white">刷题日历</h3>
              </div>
              <p class="text-white/80 text-xs leading-relaxed mb-3">
                连续打卡，让坚持可见。每一次提交都是成长的足迹
              </p>
              <div class="flex items-center gap-2">
                <TrendingUp class="w-3.5 h-3.5 text-pink-300" />
                <span class="text-xs text-pink-200 group-hover:underline">
                  {{ userStore.isAuthenticated ? '查看我的热力图 →' : '登录开启打卡记录 →' }}
                </span>
              </div>
            </button>
          </div>
        </div>
      </div>
    </div>

    <div class="py-6 sm:py-8 border-t" style="background-color: var(--theme-bg); border-color: var(--theme-border);">
      <div class="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8">
        <div class="flex items-center justify-between mb-3 sm:mb-4">
          <div class="flex items-center gap-2">
            <BookOpen class="w-4 h-4 sm:w-5 sm:h-5" style="color: var(--theme-primary);" />
            <h2 class="text-base sm:text-lg font-bold" style="color: var(--theme-text);">按主题探索</h2>
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
            <button
                type="button"
                v-for="article in getThemeArticles(activeTheme)"
                :key="article.id"
                @click.stop="router.push('/article/' + article.id)"
                class="flex items-center gap-2 cursor-pointer w-full text-left"
            >
              <div class="w-1 h-1 rounded-full" style="background-color: var(--theme-primary);"></div>
              <span class="text-xs sm:text-sm line-clamp-1 flex-1" style="color: var(--theme-text);">
                {{ article.title }}
              </span>
              <span class="text-xs flex-shrink-0" style="color: var(--theme-text-secondary);">{{ article.createdAt }}</span>
            </button>
          </div>
        </div>
      </div>
    </div>

    <div class="py-6 sm:py-8" style="background-color: var(--theme-bg);">
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
          </div>
        </div>

        <div v-if="authors.length > 0" class="grid grid-cols-2 sm:grid-cols-4 lg:grid-cols-7 gap-3 sm:gap-4">
          <button
              type="button"
              v-for="author in authors"
              :key="author.id"
              @click="goToAuthor(author.id)"
              class="text-center p-3 sm:p-4 rounded-xl cursor-pointer transition-colors w-full"
              :style="{ backgroundColor: 'var(--theme-surface)' }"
          >
            <div class="w-10 h-10 sm:w-12 sm:h-12 mx-auto mb-2 rounded-full bg-gradient-to-br from-red-100 to-orange-100 flex items-center justify-center">
              <span class="text-sm font-bold" style="color: var(--theme-primary);">{{ author.avatar }}</span>
            </div>
            <p class="font-medium text-xs sm:text-sm mb-1" style="color: var(--theme-text);">{{ author.name }}</p>
            <p class="text-xs" style="color: var(--theme-text-secondary);">已创作 {{ author.works }} 篇</p>
            <p class="text-xs" style="color: var(--theme-text-secondary);">{{ author.likes }} 喜欢</p>
            <p class="text-xs" style="color: var(--theme-text-secondary);">坚持 {{ author.days }} 天</p>
          </button>
        </div>
        <div v-else class="py-8 text-center" style="color: var(--theme-text-secondary);">
          <p class="text-sm">暂无名家数据</p>
        </div>
      </div>
    </div>

    <!-- 社区动态预览：营造"有人正在创作/学习"的社区氛围 -->
    <div class="py-6 sm:py-8" style="background-color: var(--theme-bg);">
      <div class="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8">
        <div class="flex items-center justify-between mb-3 sm:mb-4">
          <div class="flex items-center gap-2">
            <MessageCircle class="w-4 h-4 sm:w-5 sm:h-5" style="color: var(--theme-primary);" />
            <div>
              <h2 class="text-base sm:text-lg font-bold" style="color: var(--theme-text);">墨韵动态</h2>
              <p class="text-xs" style="color: var(--theme-text-secondary);">看看大家都在做什么</p>
            </div>
          </div>
          <button @click="router.push('/feed')" class="flex items-center gap-1 text-xs sm:text-sm font-medium" style="color: var(--theme-primary);">
            <span>动态广场</span>
            <ArrowRight class="w-3 h-3 sm:w-4 sm:h-4" />
          </button>
        </div>

        <div v-if="hotFeedList.length > 0" class="grid grid-cols-1 md:grid-cols-3 gap-3 sm:gap-4">
          <button
              v-for="feed in hotFeedList"
              :key="feed.eventId"
              @click="router.push(getFeedTargetPath(feed))"
              class="p-4 rounded-xl border text-left transition-all hover:scale-105 hover:shadow-md"
              style="background-color: var(--theme-surface); border-color: var(--theme-border);"
          >
            <div class="flex items-center gap-2 mb-2">
              <div class="w-8 h-8 rounded-full bg-gradient-to-br from-red-100 to-orange-100 flex items-center justify-center flex-shrink-0">
                <span class="text-xs font-bold" style="color: var(--theme-primary);">
                  {{ (feed.userNickname || 'A').charAt(0) }}
                </span>
              </div>
              <div class="flex-1 min-w-0">
                <p class="text-sm font-medium truncate" style="color: var(--theme-text);">
                  {{ feed.userNickname || '匿名' }}
                </p>
                <p class="text-xs" style="color: var(--theme-text-secondary);">
                  {{ getFeedActionText(feed) }}
                </p>
              </div>
            </div>
            <p v-if="feed.summary" class="text-xs line-clamp-2 mt-2" style="color: var(--theme-text-secondary);">
              {{ feed.summary }}
            </p>
          </button>
        </div>
        <div v-else class="p-6 sm:p-8 rounded-xl text-center" style="background-color: var(--theme-surface);">
          <MessageCircle class="w-8 h-8 mx-auto mb-2 opacity-30" style="color: var(--theme-text-secondary);" />
          <p class="text-sm mb-1" style="color: var(--theme-text);">社区还很安静</p>
          <p class="text-xs" style="color: var(--theme-text-secondary);">第一批创作者正在赶来，期待他们的故事</p>
          <button
              @click="handleWrite"
              class="mt-3 inline-flex items-center gap-1 px-4 py-2 rounded-full text-xs font-medium text-white"
              style="background-color: var(--theme-primary);"
          >
            <Sparkles class="w-3.5 h-3.5" />
            成为第一位创作者
          </button>
        </div>
      </div>
    </div>

    <div class="py-6 sm:py-8 border-t" style="background-color: var(--theme-surface); border-color: var(--theme-border);">
      <div class="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8">
        <div class="space-y-6 sm:space-y-8">
          <div class="rounded-xl p-4 sm:p-5" style="background-color: var(--theme-bg);">
            <div class="flex items-center gap-2 mb-3">
              <Star class="w-4 h-4 sm:w-5 sm:h-5 text-yellow-500" />
              <h3 class="font-semibold text-sm sm:text-base" style="color: var(--theme-text);">热门标签</h3>
            </div>
            <nav class="flex flex-wrap gap-1.5 sm:gap-2">
              <button
                  type="button"
                  v-for="tag in (tags.length > 0 ? tags : [{ id: '1', name: '文学' }, { id: '2', name: '散文' }, { id: '3', name: '随笔' }])"
                  :key="tag.id || tag"
                  @click="router.push(`/tag/${encodeURIComponent(tag.name || tag)}`)"
                  class="inline-flex items-center gap-1 px-2.5 sm:px-3 py-1 sm:py-1.5 rounded-full text-xs cursor-pointer transition-all hover:opacity-80"
                  :style="{ backgroundColor: 'var(--theme-accent)', color: 'var(--theme-primary)' }"
              >
                <Tag class="w-3 h-3" />
                {{ tag.name || tag }}
              </button>
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

    <div class="mt-6 sm:mt-8">
      <SiteFooter />
    </div>

    <BackToTop />
    </template>
  </div>
</template>

<style scoped>
</style>
