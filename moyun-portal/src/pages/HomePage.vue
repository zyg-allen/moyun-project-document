<script setup lang="ts">
import { ref, computed, onMounted, onUnmounted } from 'vue'
import { useRouter } from 'vue-router'
import { useHead } from '@vueuse/head'
import {
  ChevronRight, ChevronLeft, Star, Flame,
  User, Eye, Tag, BookOpen,
  Quote, ArrowRight, Sparkles,
  Book, Briefcase,
  AlertCircle, RefreshCw,
  Network, TrendingUp,
  MessageCircle, Activity, Crown, Target,
  Brain, Trophy, Calendar, Zap, LogIn,
  Link as LinkIcon
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
const mainDataError = ref<string | null>(null)
const categoryArticles = ref<Record<string, any[]>>({})

const readingBooks = ref<any[]>([])
const readingBookLists = ref<any[]>([])
const readingQuotes = ref<any[]>([])

const interviewQuestions = ref<any[]>([])
const interviewExperiences = ref<any[]>([])
const interviewCategories = ref<any[]>([])
const interviewTotalQuestions = ref(0)

const leaderboardTop3 = ref<any[]>([])

const hotFeedList = ref<any[]>([])

const loadHomeData = async () => {
  try {
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
    } else {
      mainDataError.value = homeResponse.message || '主内容加载失败'
    }
  } catch (err) {
    console.error('加载首页主数据失败:', err)
    mainDataError.value = '推荐内容加载失败，可点击重试'
  }
}

const retryLoad = async () => {
  error.value = null
  mainDataError.value = null
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
      hotFeedList.value = (response.data.list || []).slice(0, 3)
    }
  } catch (err) {
    console.error('加载社区动态失败:', err)
    hotFeedList.value = []
  }
}

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
const isLoggedIn = computed(() => userStore.isAuthenticated)

const loadAll = async () => {
  try {
    loading.value = true
    await Promise.allSettled([
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
    if (mainDataError.value && !carouselArticles.value.length && !featuredArticles.value.length) {
      error.value = mainDataError.value
    }
  } catch (e) {
    console.error('加载首页数据失败:', e)
    if (!error.value) {
      error.value = '加载首页数据失败，请稍后重试'
    }
  } finally {
    loading.value = false
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
  const theme = themes.value.find(t => t.id === themeId)
  if (theme && theme.isExternal && theme.path) {
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
    return { backgroundColor: 'var(--theme-primary)', color: 'white' }
  }
  return { backgroundColor: 'var(--theme-bg)', color: 'var(--theme-text-secondary)' }
}

const getThemeTabStyle = (themeName: string) => {
  if (activeTheme.value === themeName) {
    return { backgroundColor: 'var(--theme-primary)', color: 'white' }
  }
  return { backgroundColor: 'var(--theme-surface)', color: 'var(--theme-text-secondary)' }
}

const handleWrite = () => {
  if (!requireAuth('/publish')) {
    return;
  }
  router.push('/publish');
}

const handleStartInterview = () => {
  if (!requireAuth('/interview/mock')) {
    return;
  }
  router.push('/interview/mock');
}

const handleStartLearn = () => {
  if (!requireAuth('/learn')) {
    return;
  }
  router.push('/learn');
}

const handleRegister = () => {
  router.push('/register');
}

// 成长指标（登录用户）
const growthStats = computed(() => [
  { label: '今日刷题', value: '0/5', icon: Target, color: 'text-blue-500' },
  { label: '面试均分', value: '--', icon: Brain, color: 'text-purple-500' },
  { label: '阅读进度', value: '--', icon: BookOpen, color: 'text-green-500' },
  { label: '成长等级', value: 'Lv.1', icon: Crown, color: 'text-orange-500' }
])

// 游客价值展示
const guestFeatures = [
  { icon: Brain, title: 'AI 模拟面试', desc: '真实对话场景，AI 智能评分' },
  { icon: Target, title: '面试题库', desc: '海量题目 + OJ 在线判题' },
  { icon: TrendingUp, title: '成长轨迹', desc: '可视化时间线，进步看得见' }
]

const siteStats = computed(() => [
  { label: '原创文章', value: latestArticles.value.length, suffix: '篇' },
  { label: '名家展示', value: authors.value.length, suffix: '位' },
  { label: '热门标签', value: tags.value.length, suffix: '个' }
])

useHead(
    generateSeo({
      title: '首页',
      description: '墨韵·智库 - AI 驱动的个人成长平台，学习、刷题、面试、记录，让成长有迹可循',
      keywords: ['AI面试', '成长平台', '学习', '刷题', '面试', '文学', '散文', '创作', '阅读'],
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

    <!-- ═══════════════════════════════════════════════════════════════
         第 1 屏：Hero 区（双态：游客看价值 / 登录用户看成长仪表盘）
    ════════════════════════════════════════════════════════════════ -->
    <div class="py-6 sm:py-8" style="background-color: var(--theme-bg);">
      <div class="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8">

        <!-- 游客态：平台价值展示 -->
        <div v-if="!isLoggedIn" class="text-center mb-5">
          <h1 class="text-2xl sm:text-3xl md:text-4xl font-bold tracking-tight" style="color: var(--theme-text);">
            AI 驱动的<span style="color: var(--theme-primary);">个人成长平台</span>
          </h1>
          <p class="text-xs sm:text-sm md:text-base mt-2" style="color: var(--theme-text-secondary);">
            学习 · 刷题 · 面试 · 记录，让成长有迹可循
          </p>

          <!-- 三大核心价值 -->
          <div class="grid grid-cols-3 gap-3 sm:gap-6 mt-6 max-w-2xl mx-auto">
            <div v-for="feature in guestFeatures" :key="feature.title" class="flex flex-col items-center gap-1.5">
              <div class="w-10 h-10 sm:w-12 sm:h-12 rounded-xl flex items-center justify-center" style="background-color: var(--theme-accent);">
                <component :is="feature.icon" class="w-5 h-5 sm:w-6 sm:h-6" style="color: var(--theme-primary);" />
              </div>
              <h3 class="text-xs sm:text-sm font-semibold" style="color: var(--theme-text);">{{ feature.title }}</h3>
              <p class="text-xs hidden sm:block" style="color: var(--theme-text-secondary);">{{ feature.desc }}</p>
            </div>
          </div>

          <div class="flex items-center justify-center gap-3 mt-6">
            <button @click="handleStartInterview" class="inline-flex items-center gap-2 px-5 sm:px-6 py-2.5 rounded-full text-sm font-medium text-white" style="background-color: var(--theme-primary);">
              <Brain class="w-4 h-4" />
              开始 AI 模拟面试
            </button>
            <button @click="router.push('/interview')" class="inline-flex items-center gap-2 px-5 sm:px-6 py-2.5 rounded-full text-sm font-medium border" style="border-color: var(--theme-border); color: var(--theme-text);">
              浏览题库
            </button>
          </div>

          <!-- 平台数据 -->
          <div class="flex items-center justify-center gap-6 sm:gap-8 mt-6">
            <div v-for="stat in siteStats" :key="stat.label" class="text-center">
              <span class="text-lg sm:text-xl font-bold" style="color: var(--theme-primary);">{{ stat.value }}</span>
              <span class="text-xs" style="color: var(--theme-text-secondary);">{{ stat.suffix }}</span>
              <p class="text-xs mt-0.5" style="color: var(--theme-text-secondary);">{{ stat.label }}</p>
            </div>
          </div>
        </div>

        <!-- 登录态：成长仪表盘 -->
        <div v-else class="mb-5">
          <div class="rounded-2xl p-4 sm:p-6 shadow-lg" style="background: linear-gradient(135deg, var(--theme-primary) 0%, var(--theme-primary-dark, #4f46e5) 100%);">
            <div class="flex items-center justify-between mb-4">
              <div>
                <h2 class="text-lg sm:text-xl font-bold text-white">
                  {{ userStore.userInfo?.nickname || userStore.userInfo?.username || '你好' }}，继续成长吧
                </h2>
                <p class="text-white/80 text-xs sm:text-sm mt-1">你已连续学习 0 天，保持节奏</p>
              </div>
              <Zap class="w-6 h-6 text-yellow-300" />
            </div>

            <div class="grid grid-cols-2 sm:grid-cols-4 gap-3">
              <div v-for="stat in growthStats" :key="stat.label" class="bg-white/15 backdrop-blur rounded-xl p-3 text-center">
                <component :is="stat.icon" class="w-5 h-5 mx-auto mb-1" :class="stat.color" />
                <p class="text-lg sm:text-xl font-bold text-white">{{ stat.value }}</p>
                <p class="text-white/70 text-xs">{{ stat.label }}</p>
              </div>
            </div>

            <div class="flex items-center gap-3 mt-4">
              <button @click="handleStartLearn" class="flex-1 py-2.5 rounded-xl text-sm font-medium text-white bg-white/20 backdrop-blur hover:bg-white/30 transition-colors flex items-center justify-center gap-2">
                <Target class="w-4 h-4" />
                继续今日学习
              </button>
              <button @click="handleStartInterview" class="flex-1 py-2.5 rounded-xl text-sm font-medium bg-white hover:bg-gray-100 transition-colors flex items-center justify-center gap-2" style="color: var(--theme-primary);">
                <Brain class="w-4 h-4" />
                AI 模拟面试
              </button>
            </div>
          </div>
        </div>

        <!-- 名言 + 写作 CTA -->
        <div class="grid grid-cols-1 md:grid-cols-2 gap-0 rounded-xl shadow-lg overflow-hidden border" style="border-color: var(--theme-border);">
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
          <button @click="handleWrite" class="flex items-center justify-between py-3 sm:py-4 px-4 text-left hover:opacity-90 transition-opacity" style="background-color: var(--theme-primary);">
            <div>
              <p class="text-white font-semibold text-sm sm:text-base">写下今天的成长</p>
              <p class="text-red-100 text-xs">写下即是沉淀，分享即是力量。</p>
            </div>
            <div class="w-7 h-7 sm:w-8 sm:h-8 bg-white/20 rounded-full flex items-center justify-center">
              <Sparkles class="w-3.5 h-3.5 sm:w-4 sm:h-4 text-white" />
            </div>
          </button>
        </div>
      </div>
    </div>

    <!-- ═══════════════════════════════════════════════════════════════
         第 2 屏：成长工具（面试 + 学习，左右并列）
    ════════════════════════════════════════════════════════════════ -->
    <div class="py-6 sm:py-8" style="background-color: var(--theme-bg);">
      <div class="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8">
        <div class="grid lg:grid-cols-2 gap-4 sm:gap-6">

          <!-- 面试成长 -->
          <div class="p-4 sm:p-5 rounded-xl" style="background-color: var(--theme-surface);">
            <div class="flex items-center justify-between mb-4">
              <div class="flex items-center gap-2">
                <div class="w-8 h-8 rounded-lg bg-purple-100 flex items-center justify-center">
                  <Briefcase class="w-4 h-4 text-purple-600" />
                </div>
                <div>
                  <h3 class="font-semibold text-sm sm:text-base" style="color: var(--theme-text);">面试成长</h3>
                  <p class="text-xs" style="color: var(--theme-text-secondary);">AI 面试官 + 题库 + 面经</p>
                </div>
              </div>
              <button @click="router.push('/interview')" class="flex items-center gap-1 text-xs sm:text-sm font-medium" style="color: var(--theme-primary);">
                <span>进入</span>
                <ArrowRight class="w-3 h-3 sm:w-4 sm:h-4" />
              </button>
            </div>

            <!-- AI 模拟面试入口 -->
            <button @click="handleStartInterview" class="w-full p-3 rounded-xl mb-3 flex items-center justify-between text-left transition-all hover:scale-[1.02]" style="background: linear-gradient(135deg, #6366f1 0%, #8b5cf6 100%);">
              <div>
                <div class="flex items-center gap-2 mb-1">
                  <Brain class="w-4 h-4 text-white" />
                  <span class="text-white font-semibold text-sm">AI 模拟面试</span>
                </div>
                <p class="text-white/80 text-xs">真实对话场景 · AI 智能评分 · 弱项分析</p>
              </div>
              <div class="px-3 py-1.5 bg-white/20 backdrop-blur rounded-full">
                <span class="text-white text-xs font-medium">开始</span>
              </div>
            </button>

            <div class="grid grid-cols-2 gap-3">
              <!-- 热门题目 -->
              <div class="p-3 rounded-lg" style="background-color: var(--theme-bg);">
                <div class="flex items-center justify-between mb-2">
                  <span class="font-medium text-xs" style="color: var(--theme-text);">热门题目</span>
                  <span class="px-1.5 py-0.5 bg-red-100 text-red-600 rounded text-xs">hot</span>
                </div>
                <div class="space-y-2">
                  <button v-for="q in interviewQuestions" :key="q.id" @click="router.push(`/interview/question/${q.id}`)" class="flex items-center justify-between w-full text-left cursor-pointer hover:text-blue-500 transition-colors">
                    <span class="text-xs line-clamp-1" style="color: var(--theme-text-secondary);">{{ q.title }}</span>
                    <span class="text-xs flex-shrink-0 ml-1" style="color: var(--theme-text-secondary);">{{ q.submissionCount || 0 }}提</span>
                  </button>
                  <p v-if="interviewQuestions.length === 0" class="text-xs" style="color: var(--theme-text-secondary);">暂无题目</p>
                </div>
              </div>

              <!-- 面经复盘 -->
              <div class="p-3 rounded-lg" style="background-color: var(--theme-bg);">
                <div class="flex items-center justify-between mb-2">
                  <span class="font-medium text-xs" style="color: var(--theme-text);">面经复盘</span>
                  <span class="px-1.5 py-0.5 bg-blue-100 text-blue-600 rounded text-xs">new</span>
                </div>
                <div class="space-y-2">
                  <button v-for="exp in interviewExperiences" :key="exp.id" @click="router.push(`/interview/experience/${exp.id}`)" class="flex items-center justify-between w-full text-left cursor-pointer hover:text-blue-500 transition-colors">
                    <span class="text-xs line-clamp-1" style="color: var(--theme-text-secondary);">{{ exp.title }}</span>
                    <span v-if="exp.company" class="text-xs flex-shrink-0 ml-1" style="color: var(--theme-text-secondary);">{{ exp.company }}</span>
                  </button>
                  <p v-if="interviewExperiences.length === 0" class="text-xs" style="color: var(--theme-text-secondary);">暂无面经</p>
                </div>
              </div>
            </div>
          </div>

          <!-- 学习工具 -->
          <div class="p-4 sm:p-5 rounded-xl" style="background-color: var(--theme-surface);">
            <div class="flex items-center justify-between mb-4">
              <div class="flex items-center gap-2">
                <div class="w-8 h-8 rounded-lg bg-blue-100 flex items-center justify-center">
                  <Target class="w-4 h-4 text-blue-600" />
                </div>
                <div>
                  <h3 class="font-semibold text-sm sm:text-base" style="color: var(--theme-text);">学习工具</h3>
                  <p class="text-xs" style="color: var(--theme-text-secondary);">刷题 · 日历 · 排行榜</p>
                </div>
              </div>
              <button @click="router.push('/learn')" class="flex items-center gap-1 text-xs sm:text-sm font-medium" style="color: var(--theme-primary);">
                <span>进入</span>
                <ArrowRight class="w-3 h-3 sm:w-4 sm:h-4" />
              </button>
            </div>

            <div class="grid grid-cols-2 gap-3">
              <!-- 刷题日历 -->
              <button @click="router.push('/learn')" class="p-3 rounded-lg text-left transition-all hover:scale-[1.02]" style="background-color: var(--theme-bg);">
                <Calendar class="w-5 h-5 text-green-500 mb-1.5" />
                <h4 class="font-medium text-xs sm:text-sm" style="color: var(--theme-text);">刷题日历</h4>
                <p class="text-xs mt-0.5" style="color: var(--theme-text-secondary);">连续打卡，坚持可见</p>
              </button>

              <!-- 知识图谱 -->
              <button @click="router.push('/learn/knowledge-map')" class="p-3 rounded-lg text-left transition-all hover:scale-[1.02]" style="background-color: var(--theme-bg);">
                <Network class="w-5 h-5 text-purple-500 mb-1.5" />
                <h4 class="font-medium text-xs sm:text-sm" style="color: var(--theme-text);">知识图谱</h4>
                <p class="text-xs mt-0.5" style="color: var(--theme-text-secondary);">发现薄弱点</p>
              </button>
            </div>

            <!-- 排行榜 Top3 -->
            <div class="mt-3 p-3 rounded-lg" style="background-color: var(--theme-bg);">
              <div class="flex items-center gap-2 mb-2">
                <Trophy class="w-4 h-4 text-yellow-500" />
                <span class="font-medium text-xs" style="color: var(--theme-text);">排行榜 Top3</span>
              </div>
              <div class="space-y-1.5">
                <div v-for="(item, index) in leaderboardTop3" :key="item.userId || index" class="flex items-center gap-2">
                  <span class="w-5 h-5 rounded-full flex items-center justify-center text-xs font-bold flex-shrink-0" :style="getTrendingItemStyle(index)">
                    {{ index + 1 }}
                  </span>
                  <span class="text-xs flex-1 line-clamp-1" style="color: var(--theme-text-secondary);">{{ item.nickname || item.username || '匿名用户' }}</span>
                  <span class="text-xs flex-shrink-0" style="color: var(--theme-text-secondary);">{{ item.questionCount || item.score || 0 }}题</span>
                </div>
                <p v-if="leaderboardTop3.length === 0" class="text-xs" style="color: var(--theme-text-secondary);">暂无数据</p>
              </div>
            </div>
          </div>

        </div>
      </div>
    </div>

    <!-- ═══════════════════════════════════════════════════════════════
         第 3 屏：内容空间（读书 + 文章 + 话题，三栏）
    ════════════════════════════════════════════════════════════════ -->
    <div class="py-6 sm:py-8" style="background-color: var(--theme-bg);">
      <div class="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8">
        <div class="grid lg:grid-cols-3 gap-4 sm:gap-6">

          <!-- 读书空间 -->
          <div class="p-4 rounded-xl" style="background-color: var(--theme-surface);">
            <div class="flex items-center justify-between mb-3">
              <div class="flex items-center gap-2">
                <Book class="w-4 h-4 text-green-600" />
                <h3 class="font-semibold text-sm" style="color: var(--theme-text);">读书空间</h3>
              </div>
              <button @click="router.push('/reading')" class="text-xs font-medium" style="color: var(--theme-primary);">进入 →</button>
            </div>

            <button v-if="readingBooks.length > 0" @click="router.push(`/reading/book/${readingBooks[0].id}`)" class="w-full relative h-24 rounded-lg overflow-hidden mb-2 cursor-pointer">
              <LazyImage :src="readingBooks[0].cover" :alt="readingBooks[0].title" class="absolute inset-0 w-full h-full object-cover" />
              <div class="absolute inset-0 bg-gradient-to-br from-green-600/80 to-green-800/80 p-2.5">
                <span class="text-white text-xs">{{ readingBooks[0].title }}</span>
                <p class="text-white/70 text-xs">{{ readingBooks[0].author }}</p>
              </div>
            </button>
            <div v-else class="w-full h-24 rounded-lg mb-2 flex items-center justify-center bg-green-50">
              <p class="text-xs text-green-400">暂无推荐</p>
            </div>

            <div class="space-y-1.5">
              <button v-for="bl in readingBookLists" :key="bl.id" @click="router.push(`/reading/book-list/${bl.id}`)" class="flex items-center gap-1.5 w-full text-left cursor-pointer hover:text-green-500 transition-colors">
                <div class="w-1 h-1 rounded-full bg-green-500"></div>
                <span class="text-xs line-clamp-1" style="color: var(--theme-text-secondary);">{{ bl.title }}</span>
              </button>
            </div>

            <p v-if="readingQuotes.length > 0" class="text-xs italic mt-2 pt-2 border-t" style="border-color: var(--theme-border); color: var(--theme-text-secondary);">
              "{{ readingQuotes[0].content }}"
            </p>
          </div>

          <!-- 文章精选 -->
          <div class="p-4 rounded-xl" style="background-color: var(--theme-surface);">
            <div class="flex items-center justify-between mb-3">
              <div class="flex items-center gap-2">
                <Star class="w-4 h-4 text-yellow-500" />
                <h3 class="font-semibold text-sm" style="color: var(--theme-text);">文章精选</h3>
              </div>
              <button @click="router.push('/category')" class="text-xs font-medium" style="color: var(--theme-primary);">更多 →</button>
            </div>

            <div class="space-y-2">
              <button v-for="article in featuredArticles.slice(0, 5)" :key="article.id" @click="router.push('/article/' + article.id)" class="flex gap-2 p-1.5 rounded-lg cursor-pointer transition-colors w-full text-left" style="background-color: var(--theme-bg);">
                <LazyImage v-if="article.cover" :src="article.cover" :alt="article.title" class="w-14 h-10 rounded object-cover flex-shrink-0" />
                <div class="flex-1 min-w-0">
                  <h4 class="font-medium text-xs line-clamp-1" style="color: var(--theme-text);">{{ article.title }}</h4>
                  <p class="text-xs mt-0.5 line-clamp-1" style="color: var(--theme-text-secondary);">{{ article.author?.username || '作者' }} · {{ article.views }} 阅读</p>
                </div>
              </button>
              <p v-if="featuredArticles.length === 0" class="text-xs" style="color: var(--theme-text-secondary);">暂无文章</p>
            </div>
          </div>

          <!-- 话题讨论 -->
          <div class="p-4 rounded-xl" style="background-color: var(--theme-surface);">
            <div class="flex items-center justify-between mb-3">
              <div class="flex items-center gap-2">
                <MessageCircle class="w-4 h-4 text-blue-500" />
                <h3 class="font-semibold text-sm" style="color: var(--theme-text);">话题讨论</h3>
              </div>
              <button @click="router.push('/topic')" class="text-xs font-medium" style="color: var(--theme-primary);">更多 →</button>
            </div>

            <div class="space-y-2">
              <button @click="router.push('/topic')" class="w-full p-2 rounded-lg text-left cursor-pointer transition-colors" style="background-color: var(--theme-bg);">
                <p class="text-xs font-medium" style="color: var(--theme-text);"># 技术与文学的交叉点</p>
                <p class="text-xs mt-0.5" style="color: var(--theme-text-secondary);">32 人参与讨论</p>
              </button>
              <button @click="router.push('/topic')" class="w-full p-2 rounded-lg text-left cursor-pointer transition-colors" style="background-color: var(--theme-bg);">
                <p class="text-xs font-medium" style="color: var(--theme-text);"># 你的面试复盘</p>
                <p class="text-xs mt-0.5" style="color: var(--theme-text-secondary);">18 人参与讨论</p>
              </button>
              <button @click="router.push('/topic')" class="w-full p-2 rounded-lg text-left cursor-pointer transition-colors" style="background-color: var(--theme-bg);">
                <p class="text-xs font-medium" style="color: var(--theme-text);"># 本周读了什么书</p>
                <p class="text-xs mt-0.5" style="color: var(--theme-text-secondary);">15 人参与讨论</p>
              </button>
            </div>

            <button @click="router.push('/topic/create')" class="w-full mt-2 py-2 rounded-lg text-xs font-medium border border-dashed transition-colors hover:opacity-80" style="border-color: var(--theme-border); color: var(--theme-text-secondary);">
              + 发起话题
            </button>
          </div>

        </div>
      </div>
    </div>

    <!-- ═══════════════════════════════════════════════════════════════
         第 4 屏：成长时间线（情感锚点）
    ════════════════════════════════════════════════════════════════ -->
    <div class="py-6 sm:py-8" style="background-color: var(--theme-surface);">
      <div class="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8">
        <div class="rounded-2xl p-5 sm:p-6" style="background: linear-gradient(135deg, #f0f4ff 0%, #e0e7ff 100%);">

          <div class="flex items-center justify-between mb-4">
            <div class="flex items-center gap-2">
              <TrendingUp class="w-5 h-5" style="color: var(--theme-primary);" />
              <h3 class="font-bold text-base sm:text-lg" style="color: var(--theme-text);">成长轨迹</h3>
            </div>
            <button v-if="isLoggedIn" @click="router.push('/growth')" class="flex items-center gap-1 text-xs sm:text-sm font-medium" style="color: var(--theme-primary);">
              <span>完整时间线</span>
              <ArrowRight class="w-3 h-3 sm:w-4 sm:h-4" />
            </button>
          </div>

          <!-- 登录用户：成长数据预览 -->
          <div v-if="isLoggedIn" class="grid grid-cols-3 gap-4 mb-4">
            <div class="text-center">
              <p class="text-2xl font-bold" style="color: var(--theme-primary);">0</p>
              <p class="text-xs" style="color: var(--theme-text-secondary);">本月解题</p>
            </div>
            <div class="text-center">
              <p class="text-2xl font-bold" style="color: var(--theme-primary);">0</p>
              <p class="text-xs" style="color: var(--theme-text-secondary);">本月阅读</p>
            </div>
            <div class="text-center">
              <p class="text-2xl font-bold" style="color: var(--theme-primary);">0</p>
              <p class="text-xs" style="color: var(--theme-text-secondary);">本月写作</p>
            </div>
          </div>

          <!-- 时间线可视化 -->
          <div v-if="isLoggedIn" class="flex items-center justify-between gap-1 mb-2 overflow-x-auto pb-2">
            <div class="flex flex-col items-center gap-1 flex-shrink-0">
              <div class="w-3 h-3 rounded-full" style="background-color: var(--theme-primary);"></div>
              <span class="text-xs whitespace-nowrap" style="color: var(--theme-text-secondary);">8/1</span>
            </div>
            <div class="flex-1 h-0.5" style="background-color: var(--theme-border);"></div>
            <div class="flex flex-col items-center gap-1 flex-shrink-0">
              <div class="w-3 h-3 rounded-full" style="background-color: var(--theme-primary);"></div>
              <span class="text-xs whitespace-nowrap" style="color: var(--theme-text-secondary);">8/5</span>
            </div>
            <div class="flex-1 h-0.5" style="background-color: var(--theme-border);"></div>
            <div class="flex flex-col items-center gap-1 flex-shrink-0">
              <div class="w-3 h-3 rounded-full" style="background-color: var(--theme-primary);"></div>
              <span class="text-xs whitespace-nowrap" style="color: var(--theme-text-secondary);">8/10</span>
            </div>
            <div class="flex-1 h-0.5" style="background-color: var(--theme-border);"></div>
            <div class="flex flex-col items-center gap-1 flex-shrink-0">
              <div class="w-3 h-3 rounded-full" style="background-color: var(--theme-accent); border: 2px solid var(--theme-primary);"></div>
              <span class="text-xs whitespace-nowrap font-medium" style="color: var(--theme-primary);">今天</span>
            </div>
          </div>

          <!-- 游客：引导注册 -->
          <div v-else class="text-center py-4">
            <p class="text-sm" style="color: var(--theme-text-secondary);">注册后，你的每一步成长都会被记录</p>
            <button @click="handleRegister" class="inline-flex items-center gap-2 mt-3 px-5 py-2.5 rounded-full text-sm font-medium text-white" style="background-color: var(--theme-primary);">
              <LogIn class="w-4 h-4" />
              立即注册
            </button>
          </div>

        </div>
      </div>
    </div>

    <!-- ═══════════════════════════════════════════════════════════════
         第 5 屏：社区动态 + 热门标签 + 友情链接（精简收尾）
    ════════════════════════════════════════════════════════════════ -->
    <div class="py-6 sm:py-8" style="background-color: var(--theme-bg);">
      <div class="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8">

        <!-- 社区动态 -->
        <div v-if="hotFeedList.length > 0" class="mb-6">
          <div class="flex items-center gap-2 mb-3">
            <Activity class="w-4 h-4 sm:w-5 sm:h-5" style="color: var(--theme-primary);" />
            <h3 class="font-semibold text-sm sm:text-base" style="color: var(--theme-text);">墨韵动态</h3>
          </div>
          <div class="grid grid-cols-1 sm:grid-cols-3 gap-3">
            <button v-for="item in hotFeedList" :key="item.id || item.eventType" @click="router.push(getFeedTargetPath(item))" class="p-3 rounded-xl cursor-pointer transition-colors text-left" style="background-color: var(--theme-surface);">
              <p class="text-xs" style="color: var(--theme-text-secondary);">
                <span class="font-medium" style="color: var(--theme-text);">{{ item.username || item.nickname || '用户' }}</span>
                {{ getFeedActionText(item) }}
              </p>
            </button>
          </div>
        </div>

        <!-- 热门标签 -->
        <div v-if="tags.length > 0" class="mb-6">
          <div class="flex items-center gap-2 mb-3">
            <Tag class="w-4 h-4" style="color: var(--theme-primary);" />
            <span class="font-semibold text-sm" style="color: var(--theme-text);">热门标签</span>
          </div>
          <div class="flex flex-wrap gap-2">
            <button v-for="tag in tags" :key="tag.id || tag.name" @click="router.push(`/tag/${encodeURIComponent(tag.name)}`)" class="px-3 py-1 rounded-full text-xs font-medium transition-colors" style="background-color: var(--theme-surface); color: var(--theme-text-secondary);">
              {{ tag.name }}
            </button>
          </div>
        </div>

        <!-- 友情链接 -->
        <div v-if="friendLinks.length > 0" class="pt-4 border-t" style="border-color: var(--theme-border);">
          <div class="flex items-center gap-2 mb-2">
            <LinkIcon class="w-3.5 h-3.5" style="color: var(--theme-text-secondary);" />
            <span class="text-xs font-medium" style="color: var(--theme-text-secondary);">友情链接</span>
          </div>
          <div class="flex flex-wrap gap-3">
            <a v-for="link in friendLinks" :key="link.id" :href="link.url" target="_blank" rel="noopener noreferrer" class="text-xs hover:underline" style="color: var(--theme-text-secondary);">
              {{ link.name }}
            </a>
          </div>
        </div>

      </div>
    </div>

    <SiteFooter />
    <BackToTop />
    </template>
  </div>
</template>
