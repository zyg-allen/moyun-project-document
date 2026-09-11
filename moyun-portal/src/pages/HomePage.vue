<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { RouterLink as Link, useRouter } from 'vue-router'
import { useHead } from '@vueuse/head'
import {
  Star, Flame,
  User, Tag, BookOpen,
  Quote, ArrowRight, Sparkles,
  Book, Briefcase,
  AlertCircle, RefreshCw,
  Network, TrendingUp,
  MessageCircle, Activity, Crown, Target,
  Mic, PlayCircle, Clock,
  FileText, Zap, ClipboardList,
  PenLine, Trophy,
  CalendarCheck, XCircle, GraduationCap, Flame as FlameIcon,
  Code2, LayoutTemplate, Users
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
import { getAuthors } from '@/api/user'
import { getReadingHome } from '@/api/reading'
import { getInterviewHome, getResumeTemplateList } from '@/api/interview'
import { getHotFeed } from '@/api/feed'
import { getLeaderboard } from '@/api/learnStats'
import { useUserStore } from '@/stores/user'
import { useAuth } from '@/composables/useAuth'
import type { Category } from '@/types/api'

const router = useRouter()
const userStore = useUserStore()
const { requireAuth } = useAuth()

// ==================== 登录态（V1/V2 双形态首页） ====================
const isLoggedIn = computed(() => userStore.isAuthenticated)

const latestArticles = ref<any[]>([])
const tags = ref<any[]>([])
const categories = ref<Category[]>([])
const authors = ref<any[]>([])
const loading = ref(true)
const error = ref<string | null>(null)
// 主数据（推荐/轮播等）局部错误，独立于其他 section，避免单点失败阻塞整页
const mainDataError = ref<string | null>(null)
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

// 简历模板总数（Hero 数据条）
const resumeTemplateTotal = ref(0)

// 学习中心首页数据（阶段三核心展示）
const leaderboardTop3 = ref<any[]>([])

// 社区动态预览数据（营造社区氛围）
const hotFeedList = ref<any[]>([])

// ==================== 已登录：问候区（V11.2 精简，仅问候 + 快捷入口） ====================
// 问候语（按时段 + 场景化提示语；凌晨深夜关怀休息，白天按节奏激励）
const greetingInfo = computed(() => {
  const h = new Date().getHours()
  if (h < 6) return { text: '夜深了', emoji: '🌙', tip: '这么晚还在努力，注意休息，别熬太久' }
  if (h < 9) return { text: '早上好', emoji: '☀️', tip: '一日之计在于晨，从一道题目开始今天' }
  if (h < 12) return { text: '上午好', emoji: '🌤', tip: '上午思路最清晰，适合攻克难题' }
  if (h < 14) return { text: '中午好', emoji: '☀️', tip: '午间小憩片刻，下午继续保持状态' }
  if (h < 18) return { text: '下午好', emoji: '🌤', tip: '下午茶时间，刷几道题提提神' }
  return { text: '晚上好', emoji: '🌙', tip: '晚间复盘黄金时段，沉淀今天所学' }
})

// 加入平台天数（兼容 createdAt / createTime 两种字段；注册当天为第 1 天）
const joinDays = computed(() => {
  const u: any = userStore.user
  const created = u?.createdAt || u?.createTime
  if (!created) return 1
  const d = new Date(created)
  if (isNaN(d.getTime())) return 1
  const diff = Math.floor((Date.now() - d.getTime()) / 86400000) + 1
  return diff > 0 ? diff : 1
})

// ==================== 首页公共数据 ====================
const loadHomeData = async () => {
  try {
    const homeResponse = await articleApi.getHomeData()
    if (homeResponse.code === 200 && homeResponse.data) {
      latestArticles.value = homeResponse.data.latestArticles?.map(transformArticle) || []
    } else {
      // 接口返回非 200，标记主数据错误但不影响其他 section
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

// 简历模板总数（仅取 total，pageSize=1 减少传输）
const loadResumeTemplateCount = async () => {
  try {
    const response = await getResumeTemplateList({ pageNum: 1, pageSize: 1 })
    if (response.code === 200 && response.data) {
      resumeTemplateTotal.value = Number((response.data as any).total || 0)
    }
  } catch (err) {
    console.error('加载简历模板数失败:', err)
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

const loadAll = async () => {
  try {
    loading.value = true
    // 所有 section 并行加载，各自的错误已在 loadXxx 内部 try-catch 处理
    await Promise.allSettled([
      loadHomeData(),
      loadCategories(),
      loadTags(),
      loadAuthors(),
      loadReadingData(),
      loadInterviewData(),
      loadResumeTemplateCount(),
      loadLeaderboardData(),
      loadHotFeedData()
    ])
    if (themes.value.length > 0) {
      activeTheme.value = themes.value[0].name
      await loadCategoryArticles(themes.value[0].name, themes.value[0].id)
    } else {
      activeTheme.value = '散文'
    }
    // 仅当主数据出错且其他 section 也都为空时才显示全局错误
    if (mainDataError.value && !latestArticles.value.length) {
      error.value = mainDataError.value
    }
  } catch (e) {
    console.error('加载首页数据失败:', e)
    if (!error.value) {
      error.value = '加载首页数据失败，请稍后重试'
    }
  } finally {
    loading.value = false
  }
}

onMounted(() => {
  loadAll()
})

const selectTheme = async (themeId: string, themeName: string) => {
  const theme = themes.value.find(t => t.id === themeId)
  if (theme && theme.isExternal && theme.path) {
    window.open(theme.path, '_blank', 'noopener,noreferrer')
    return
  }
  activeTheme.value = themeName
  if (!categoryArticles.value[themeName]) {
    await loadCategoryArticles(themeName, themeId)
  }
}

const loadCategoryArticles = async (themeName: string, themeId?: string) => {
  try {
    // themeId 为一级分类ID：后端按 root_category_id 匹配，覆盖其下全部子分类文章
    const params: any = { limit: 8 }
    if (themeId) params.rootCategoryId = themeId
    const response = await articleApi.getCategoryRecommendedArticles(themeName, params, 8)
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
  // 兜底：从最新文章里按分类名过滤（category 为文章直属分类名；一级分类名无法匹配子分类文章，仅作最后回退）
  const filtered = latestArticles.value.filter(article => article.category === themeName)
  return filtered.slice(0, 8)
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

const goVoiceInterview = () => {
  if (!requireAuth('/interview/voice')) return;
  router.push('/interview/voice');
};

const goResumeOptimize = () => {
  if (!requireAuth('/interview/resume/optimize')) return;
  router.push('/interview/resume/optimize');
};

const goRegister = () => {
  router.push('/register');
};

// ============ Hero 区：站点核心数据（真实接口数据，非虚构指标） ============
// 名家未满 10 位时隐藏该项（避免冷启动数据削弱信任感）
const heroStats = computed(() => {
  const stats: Array<{ label: string; value: string; suffix: string }> = [
    { label: '面试题库', value: `${interviewTotalQuestions.value}+`, suffix: '道' }
  ]
  if (resumeTemplateTotal.value > 0) {
    stats.push({ label: '简历模板', value: `${resumeTemplateTotal.value}`, suffix: '套' })
  }
  if (authors.value.length >= 10) {
    stats.push({ label: '入驻名家', value: `${authors.value.length}`, suffix: '位' })
  }
  const bookCount = readingBooks.value.length + readingBookLists.value.length
  if (bookCount > 0) {
    stats.push({ label: '精选好书', value: `${bookCount}`, suffix: '本' })
  }
  if (tags.value.length > 0) {
    stats.push({ label: '热门话题标签', value: `${tags.value.length}`, suffix: '个' })
  }
  return stats
})

// ============ 五大主线锚点导航（Hero → 各区块平滑滚动） ============
const mainLines = [
  { id: 'home-learn', label: '学习', desc: '刷题备战 · 薄弱点强化', icon: GraduationCap },
  { id: 'home-resume', label: '简历', desc: 'AI 诊断 · 3 分钟出报告', icon: FileText },
  { id: 'home-interview', label: '面试', desc: 'AI 对练 · 实时评分', icon: Mic },
  { id: 'home-reading', label: '阅读', desc: '书籍与文章', icon: BookOpen },
  { id: 'home-community', label: '社区', desc: '创作互动', icon: MessageCircle },
]

// 学习区功能宫格（主线一：题库/在线编程/知识图谱/错题本/学习计划等）
const learnTools = [
  { title: '题库修炼', desc: '精选面试题库', path: '/learn/questions', icon: Zap, iconBg: 'bg-blue-500' },
  { title: '在线刷题', desc: '选择题型练习', path: '/learn/practice/choice', icon: ClipboardList, iconBg: 'bg-cyan-500' },
  { title: '在线编程', desc: '代码实时判题', path: '/learn/practice/coding', icon: Code2, iconBg: 'bg-indigo-500' },
  { title: '知识图谱', desc: '可视化知识结构', path: '/learn/knowledge', icon: Network, iconBg: 'bg-teal-500' },
  { title: '错题本', desc: '薄弱点针对强化', path: '/learn/wrong', icon: XCircle, iconBg: 'bg-red-500' },
  { title: '学习计划', desc: '定制冲刺节奏', path: '/learn/plan', icon: CalendarCheck, iconBg: 'bg-emerald-500' },
  { title: '刷题日历', desc: '坚持打卡可见', path: '/learn/calendar', icon: Activity, iconBg: 'bg-pink-500' },
  { title: '排行榜', desc: '与同伴比学赶超', path: '/learn/leaderboard', icon: Trophy, iconBg: 'bg-amber-500' },
]

// 简历区功能宫格（主线二：模板/岗位维护/AI 评分/AI 优化）
const resumeTools = [
  { title: '简历模板库', desc: '大量精选专业模板', path: '/interview/resume-templates', icon: LayoutTemplate, iconBg: 'bg-blue-500' },
  { title: '简历维护', desc: '针对岗位要求管理', path: '/interview/my/resumes', icon: ClipboardList, iconBg: 'bg-cyan-500' },
  { title: 'AI 简历评分', desc: '多维度智能打分', path: '/interview/resume/optimize', icon: Target, iconBg: 'bg-violet-500' },
  { title: 'AI 简历优化', desc: '一键生成优化建议', path: '/interview/resume/optimize', icon: Sparkles, iconBg: 'bg-orange-500' },
]

useHead(
    generateSeo({
      title: '首页',
      description: '旭林知行 - AI 驱动的求职面试与学习成长平台，为求职者提供面试技巧、题库刷题、简历优化与成长路径',
      keywords: ['面试', '求职', '刷题', '算法', '简历', '面经', '题库', '学习', '成长'],
      type: 'website',
      canonicalPath: '/'
    })
)
</script>

<template>
  <div class="min-h-screen flex flex-col bg-theme-bg">
    <!-- 加载状态 -->
    <div v-if="loading" class="min-h-[60vh] flex items-center justify-center bg-theme-bg">
      <div class="flex flex-col items-center gap-4">
        <div class="w-10 h-10 rounded-full animate-spin border-[3px] border-theme-border border-t-theme-primary"></div>
        <p class="meta-text">正在加载首页内容...</p>
      </div>
    </div>

    <!-- 错误状态 -->
    <div v-else-if="error" class="min-h-[60vh] flex items-center justify-center px-4 bg-theme-bg">
      <div class="text-center max-w-md">
        <div class="w-20 h-20 rounded-full flex items-center justify-center mx-auto mb-4 bg-theme-accent">
          <AlertCircle class="w-10 h-10 text-theme-primary" />
        </div>
        <h3 class="section-title mb-2">加载失败</h3>
        <p class="body-text mb-6 text-theme-text-secondary">{{ error }}</p>
        <button @click="retryLoad" class="theme-btn theme-btn-primary px-5 py-2.5 rounded-xl text-sm font-medium">
          <RefreshCw class="w-4 h-4" />
          重新加载
        </button>
      </div>
    </div>

    <template v-else>
      <!-- ================================================================
           综述区（Hero + 五大主线锚点导航，登录与否均展示，
           保持品牌叙事一致性；已登录时上方叠加问候带）
           ================================================================ -->
        <!-- Hero：左侧文案 + 右侧简历诊断示例卡 -->
        <section class="order-first relative overflow-hidden bg-gradient-to-br from-theme-primary-soft via-theme-bg to-theme-bg">
          <div class="absolute inset-0 pointer-events-none">
            <div class="absolute top-10 right-1/4 w-64 h-64 bg-theme-primary/10 rounded-full blur-3xl"></div>
            <div class="absolute bottom-0 left-1/4 w-80 h-80 bg-theme-primary/5 rounded-full blur-3xl"></div>
          </div>
          <div class="content-container relative py-8 sm:py-10 lg:py-12">
            <div class="grid lg:grid-cols-2 gap-10 lg:gap-14 items-center">
              <!-- 左侧文案 -->
              <div class="space-y-6 sm:space-y-8">
                <div class="inline-flex items-center gap-2 px-4 py-1.5 rounded-full bg-theme-surface border border-theme-border">
                  <span class="w-2 h-2 bg-theme-primary rounded-full animate-pulse"></span>
                  <span class="meta-text font-medium text-theme-primary">{{ interviewTotalQuestions }}+ 道精选面试题持续更新</span>
                </div>

                <div class="space-y-5 sm:space-y-6">
                  <h1 class="text-3xl sm:text-4xl lg:text-5xl font-black text-theme-text leading-relaxed text-balance">
                    简历改 3 遍，<br class="hidden sm:block">
                    <span class="block mt-3 sm:mt-4 text-theme-primary">面试机会翻 1 倍</span>
                  </h1>
                  <p class="body-text text-theme-text-secondary leading-relaxed max-w-lg">
                    AI 一站式求职助手：3 分钟诊断简历薄弱点，模拟面试实战对练，智能刷题查漏补缺，助你拿到更满意的 offer
                  </p>
                </div>

                <div class="flex flex-wrap gap-3 sm:gap-4">
                  <button
                    @click="goResumeOptimize"
                    class="inline-flex items-center gap-2 px-6 sm:px-8 py-3 sm:py-3.5 bg-theme-primary hover:bg-theme-primary-hover text-theme-on-primary font-semibold rounded-xl shadow-theme-lg transition-all hover:-translate-y-0.5"
                  >
                    <FileText class="w-5 h-5" />
                    免费简历诊断
                  </button>
                  <button
                    @click="goVoiceInterview"
                    class="inline-flex items-center gap-2 px-6 sm:px-8 py-3 sm:py-3.5 bg-theme-surface hover:bg-theme-surface-highlight text-theme-text font-semibold rounded-xl border border-theme-border shadow-theme-sm transition-all hover:-translate-y-0.5"
                  >
                    <Mic class="w-5 h-5 text-theme-primary" />
                    开始模拟面试
                  </button>
                </div>

                <!-- 核心数据 -->
                <div class="flex items-center gap-5 sm:gap-8 pt-2">
                  <template v-for="(stat, idx) in heroStats" :key="stat.label">
                    <div v-if="idx > 0" class="w-px h-10 bg-theme-border"></div>
                    <div>
                      <div class="flex items-baseline gap-0.5">
                        <span class="text-xl sm:text-2xl font-black text-theme-text stat-number">{{ stat.value }}</span>
                        <span class="meta-text">{{ stat.suffix }}</span>
                      </div>
                      <div class="meta-text mt-0.5">{{ stat.label }}</div>
                    </div>
                  </template>
                </div>
              </div>

              <!-- 右侧：AI 简历诊断示例卡 -->
              <div class="relative lg:pl-6">
                <div class="relative z-10 rounded-2xl p-5 sm:p-6 shadow-theme-xl border border-theme-border bg-theme-surface home-float">
                  <div class="flex items-center justify-between mb-4 sm:mb-5">
                    <div class="flex items-center gap-3">
                      <div class="w-10 h-10 rounded-xl bg-theme-primary flex items-center justify-center">
                        <FileText class="w-5 h-5 text-theme-on-primary" />
                      </div>
                      <div>
                        <div class="card-title">AI 简历诊断报告</div>
                        <div class="meta-text">多维度智能分析 · 示例</div>
                      </div>
                    </div>
                    <span class="px-2.5 py-1 bg-green-50 text-green-700 meta-text font-medium rounded-full">已完成</span>
                  </div>

                  <!-- 评分圆环 + 维度条 -->
                  <div class="flex items-center gap-5 sm:gap-6 mb-4 sm:mb-5">
                    <div class="relative w-20 h-20 sm:w-24 sm:h-24 flex-shrink-0">
                      <svg class="w-full h-full -rotate-90" viewBox="0 0 100 100">
                        <circle cx="50" cy="50" r="42" fill="none" class="stroke-theme-border" stroke-width="8"/>
                        <circle cx="50" cy="50" r="42" fill="none" stroke="var(--theme-primary)" stroke-width="8" stroke-linecap="round"
                          stroke-dasharray="264" stroke-dashoffset="66" class="progress-ring" />
                      </svg>
                      <div class="absolute inset-0 flex flex-col items-center justify-center">
                        <span class="text-xl sm:text-2xl font-black text-theme-text">75</span>
                        <span class="caption-text text-theme-text-tertiary">综合评分</span>
                      </div>
                    </div>
                    <div class="flex-1 space-y-2.5">
                      <div>
                        <div class="flex justify-between caption-text mb-1">
                          <span class="text-theme-text-secondary">内容匹配度</span>
                          <span class="text-theme-text font-medium">82%</span>
                        </div>
                        <div class="h-1.5 bg-theme-accent rounded-full overflow-hidden">
                          <div class="h-full bg-theme-primary rounded-full" style="width: 82%"></div>
                        </div>
                      </div>
                      <div>
                        <div class="flex justify-between caption-text mb-1">
                          <span class="text-theme-text-secondary">关键词优化</span>
                          <span class="text-theme-text font-medium">68%</span>
                        </div>
                        <div class="h-1.5 bg-theme-accent rounded-full overflow-hidden">
                          <div class="h-full bg-amber-500 rounded-full" style="width: 68%"></div>
                        </div>
                      </div>
                      <div>
                        <div class="flex justify-between caption-text mb-1">
                          <span class="text-theme-text-secondary">排版规范性</span>
                          <span class="text-theme-text font-medium">91%</span>
                        </div>
                        <div class="h-1.5 bg-theme-accent rounded-full overflow-hidden">
                          <div class="h-full bg-green-500 rounded-full" style="width: 91%"></div>
                        </div>
                      </div>
                    </div>
                  </div>

                  <!-- 优化建议 -->
                  <div class="space-y-2">
                    <div class="flex items-start gap-2 caption-text">
                      <Sparkles class="w-4 h-4 text-amber-500 flex-shrink-0 mt-0.5" />
                      <span class="text-theme-text-secondary">建议补充项目成果量化数据，使用 STAR 法则描述经历</span>
                    </div>
                    <div class="flex items-start gap-2 caption-text">
                      <TrendingUp class="w-4 h-4 text-theme-primary flex-shrink-0 mt-0.5" />
                      <span class="text-theme-text-secondary">技术栈关键词与目标岗位匹配度可进一步提升</span>
                    </div>
                  </div>

                  <button @click="goResumeOptimize" class="w-full mt-4 sm:mt-5 py-2.5 bg-theme-primary-soft hover:bg-theme-primary hover:text-theme-on-primary text-theme-primary meta-text font-medium rounded-lg transition-colors">
                    生成我的诊断报告
                  </button>
                </div>

                <!-- 浮动小卡片 -->
                <div class="hidden sm:block absolute -bottom-4 -left-3 z-20 rounded-xl p-3 shadow-theme-lg border border-theme-border bg-theme-surface home-float-delayed">
                  <div class="flex items-center gap-2">
                    <div class="w-8 h-8 rounded-lg bg-green-50 flex items-center justify-center">
                      <Briefcase class="w-4 h-4 text-green-600" />
                    </div>
                    <div>
                      <div class="caption-text font-semibold text-theme-text">AI 模拟面试中</div>
                      <div class="text-[10px] text-theme-text-tertiary">第 3 轮 · 技术深度</div>
                    </div>
                  </div>
                </div>
                <div class="hidden sm:block absolute -top-3 -right-3 z-20 rounded-xl p-3 shadow-theme-lg border border-theme-border bg-theme-surface home-float">
                  <div class="flex items-center gap-2">
                    <div class="w-8 h-8 rounded-lg bg-orange-50 flex items-center justify-center">
                      <FlameIcon class="w-4 h-4 text-orange-600" />
                    </div>
                    <div>
                      <div class="caption-text font-semibold text-theme-text">成长 +15</div>
                      <div class="text-[10px] text-theme-text-tertiary">连续打卡 · 天天可见</div>
                    </div>
                  </div>
                </div>
              </div>
            </div>

            <!-- 五大主线导航（锚点直达，V11.3 升格为图标卡片带悬浮特效） -->
            <div class="relative mt-8 sm:mt-10 pt-5 sm:pt-6 border-t border-theme-border/60">
              <div class="grid grid-cols-5 gap-2 sm:gap-3">
                <a
                  v-for="(line, idx) in mainLines"
                  :key="line.id"
                  :href="'#' + line.id"
                  class="home-mainline-card group"
                  :style="{ animationDelay: `${idx * 0.08}s` }"
                >
                  <div class="home-mainline-icon">
                    <component :is="line.icon" class="w-5 h-5 sm:w-6 sm:h-6" />
                  </div>
                  <div class="home-mainline-text">
                    <span class="font-semibold text-theme-text">{{ line.label }}</span>
                    <span class="hidden lg:inline text-theme-text-tertiary">· {{ line.desc }}</span>
                  </div>
                  <span class="home-mainline-arrow">
                    <ArrowRight class="w-3.5 h-3.5" />
                  </span>
                </a>
              </div>
            </div>
          </div>
        </section>

      <!-- ================================================================
           已登录：问候带（位于 Hero 综述区之下、五大章节之上）
           欢迎语 + 快捷入口；个人统计与足迹请前往成长时间线
           ================================================================ -->
      <template v-if="isLoggedIn">
        <div class="home-greeting-band">
        <!-- 欢迎语 + 快捷入口（V11.2：登录态首屏精简为问候带，个人统计请前往成长时间线） -->
        <section class="content-container pt-4 sm:pt-6 pb-5">
          <div class="home-greeting-card">
            <div class="flex items-center gap-2.5 mb-1">
              <h1 class="text-xl sm:text-2xl font-bold text-theme-text">{{ greetingInfo.text }}，{{ userStore.nickname || userStore.username }}</h1>
              <span class="home-greeting-emoji text-xl sm:text-2xl">{{ greetingInfo.emoji }}</span>
            </div>
            <p class="meta-text text-theme-text-secondary">
              今天是加入旭林知行的第 <span class="home-join-days">{{ joinDays }}</span> 天，{{ greetingInfo.tip }}
            </p>

            <div class="flex flex-wrap gap-2 sm:gap-2.5 mt-3 mb-1">
              <button @click="router.push('/learn')" class="home-shortcut-btn">
                <PlayCircle class="w-4 h-4 text-theme-primary" />继续学习
              </button>
              <button @click="goVoiceInterview" class="home-shortcut-btn">
                <Mic class="w-4 h-4 text-theme-primary" />开始模拟面试
              </button>
              <button @click="router.push('/learn/practice')" class="home-shortcut-btn">
                <Zap class="w-4 h-4 text-theme-primary" />去刷题
              </button>
              <button @click="handleWrite" class="home-shortcut-btn">
                <PenLine class="w-4 h-4 text-theme-primary" />写篇文章
              </button>
            </div>
          </div>
        </section>
        </div>
      </template>

      <!-- ================================================================
           公共模块（两种状态共享，保持现有真实数据源）
           ================================================================ -->

      <!-- 学习（主线一：题库刷题/在线编程/知识图谱/错题本/学习计划） -->
      <div id="home-learn" class="py-6 sm:py-10 bg-theme-bg scroll-mt-20">
        <div class="content-container">
          <!-- 章节头 -->
          <div class="home-chapter-head">
            <div class="flex items-center gap-3">
              <span class="home-chapter-no">01</span>
              <div class="w-px h-9 bg-theme-border"></div>
              <div>
                <h2 class="text-lg sm:text-xl font-bold text-theme-text">学习中心</h2>
                <p class="meta-text text-theme-text-tertiary">题库刷题 · 在线编程 · 知识图谱 · 错题本</p>
              </div>
            </div>
            <button
              @click="router.push('/learn')"
              class="home-chapter-link"
            >
              <span>进入学习中心</span>
              <ArrowRight class="w-3 h-3 sm:w-4 sm:h-4" />
            </button>
          </div>

          <div class="p-4 sm:p-5 rounded-2xl bg-theme-surface border border-theme-border shadow-theme-sm">
            <!-- 功能宫格（完整展示） -->
            <div class="grid grid-cols-2 md:grid-cols-4 gap-3">
              <button
                v-for="tool in learnTools"
                :key="tool.title"
                @click="router.push(tool.path)"
                class="home-card-lift group flex items-start gap-3 p-3 sm:p-4 rounded-xl bg-theme-bg border border-theme-border hover:border-theme-primary/40 transition-colors text-left"
              >
                <div class="w-9 h-9 rounded-lg flex items-center justify-center flex-shrink-0" :class="tool.iconBg">
                  <component :is="tool.icon" class="w-4 h-4 text-white" />
                </div>
                <div class="min-w-0">
                  <p class="card-title truncate">{{ tool.title }}</p>
                  <p class="caption-text text-theme-text-tertiary mt-0.5 truncate">{{ tool.desc }}</p>
                </div>
              </button>
            </div>

            <!-- 排行榜速览 -->
            <div v-if="leaderboardTop3.length > 0" class="flex items-center gap-3 mt-4 pt-3 border-t border-theme-border">
              <Crown class="w-4 h-4 text-amber-500 flex-shrink-0" />
              <div class="flex items-center gap-4 sm:gap-6 overflow-hidden flex-1 min-w-0">
                <button
                  v-for="(item, idx) in leaderboardTop3"
                  :key="item.userId"
                  @click="router.push('/learn/leaderboard')"
                  class="flex items-center gap-1.5 meta-text text-theme-text-secondary hover:text-theme-primary transition-colors whitespace-nowrap"
                >
                  <span
                    class="w-5 h-5 rounded-full flex items-center justify-center text-[10px] font-bold flex-shrink-0"
                    :class="idx === 0 ? 'bg-amber-400 text-amber-900' : idx === 1 ? 'bg-gray-300 text-gray-800' : 'bg-orange-400 text-orange-900'"
                  >{{ idx + 1 }}</span>
                  <span class="truncate max-w-[80px]">{{ item.nickname }}</span>
                  <span class="text-theme-text-tertiary">{{ item.value }}题</span>
                </button>
              </div>
              <button @click="router.push('/learn/leaderboard')" class="meta-text text-theme-text-tertiary hover:text-theme-primary flex-shrink-0 whitespace-nowrap">
                完整榜单 →
              </button>
            </div>
          </div>
        </div>
      </div>

      <!-- 简历（主线二：模板库/岗位维护/AI 评分/AI 优化） -->
      <div id="home-resume" class="py-6 sm:py-10 bg-theme-accent/40 scroll-mt-20">
        <div class="content-container">
          <!-- 章节头 -->
          <div class="home-chapter-head">
            <div class="flex items-center gap-3">
              <span class="home-chapter-no">02</span>
              <div class="w-px h-9 bg-theme-border"></div>
              <div>
                <h2 class="text-lg sm:text-xl font-bold text-theme-text">简历工坊</h2>
                <p class="meta-text text-theme-text-tertiary">模板精选 · 岗位定制 · AI 评分优化</p>
              </div>
            </div>
            <button @click="goResumeOptimize" class="home-chapter-link">
              <span>AI 简历诊断</span>
              <ArrowRight class="w-3 h-3 sm:w-4 sm:h-4" />
            </button>
          </div>

          <div class="p-4 sm:p-5 rounded-2xl bg-theme-surface border border-theme-border shadow-theme-sm">
            <div class="grid grid-cols-2 lg:grid-cols-4 gap-3 sm:gap-4">
              <button
                v-for="tool in resumeTools"
                :key="tool.title"
                @click="router.push(tool.path)"
                class="home-card-lift flex items-start gap-3 p-4 rounded-xl bg-theme-bg border border-theme-border hover:border-theme-primary/40 transition-colors text-left"
              >
                <div class="w-10 h-10 rounded-xl flex items-center justify-center flex-shrink-0 text-white shadow-theme-md" :class="tool.iconBg">
                  <component :is="tool.icon" class="w-5 h-5" />
                </div>
                <div class="min-w-0">
                  <p class="card-title truncate">{{ tool.title }}</p>
                  <p class="caption-text text-theme-text-tertiary mt-0.5">{{ tool.desc }}</p>
                </div>
              </button>
            </div>
          </div>
        </div>
      </div>

      <!-- 面试（主线三：基于简历的 AI 语音面试/复盘/面经共享） -->
      <div id="home-interview" class="py-6 sm:py-10 bg-gradient-to-b from-theme-primary-soft/50 via-theme-bg to-theme-bg scroll-mt-20">
        <div class="content-container">
          <!-- 章节头 -->
          <div class="home-chapter-head">
            <div class="flex items-center gap-3">
              <span class="home-chapter-no">03</span>
              <div class="w-px h-9 bg-theme-border"></div>
              <div>
                <h2 class="text-lg sm:text-xl font-bold text-theme-text">面试专区</h2>
                <p class="meta-text text-theme-text-tertiary">基于简历的 AI 模拟 · 复盘沉淀 · 面经共享</p>
              </div>
            </div>
            <button @click="router.push('/interview')" class="home-chapter-link">
              <span>进入面试专区</span>
              <ArrowRight class="w-3 h-3 sm:w-4 sm:h-4" />
            </button>
          </div>

          <div class="p-4 sm:p-5 rounded-2xl bg-theme-surface border border-theme-border shadow-theme-sm">
            <!-- AI 语音面试官 · 首页入口 CTA（浅色柔和版，与主题相映衬） -->
            <button
              type="button"
              @click="goVoiceInterview"
              class="w-full mb-4 sm:mb-5 overflow-hidden rounded-xl text-left transition-all hover:-translate-y-0.5 hover:shadow-theme-md bg-theme-primary-soft border border-theme-primary/25"
            >
              <div class="flex items-center justify-between gap-4 px-4 sm:px-6 py-3.5 sm:py-4">
                <div class="flex items-center gap-3 sm:gap-4 min-w-0">
                  <div class="shrink-0 w-10 h-10 sm:w-12 sm:h-12 rounded-2xl flex items-center justify-center bg-theme-primary text-theme-on-primary shadow-theme-sm">
                    <Mic class="w-5 h-5 sm:w-6 sm:h-6" />
                  </div>
                  <div class="min-w-0">
                    <div class="flex items-center gap-2 mb-0.5">
                      <h4 class="card-title truncate">AI 语音面试官</h4>
                      <span class="inline-flex items-center px-1.5 py-0.5 rounded-full caption-text font-bold text-theme-primary shrink-0 bg-theme-primary/10">NEW</span>
                    </div>
                    <p class="meta-text truncate text-theme-text-secondary">
                      🎙 基于你的简历 · 麦克风对练 · 实时追问 · 五维雷达报告
                    </p>
                  </div>
                </div>
                <div class="hidden sm:flex items-center gap-5 shrink-0 meta-text text-theme-text-secondary">
                  <div class="text-center">
                    <div class="flex items-center gap-1"><PlayCircle class="w-3.5 h-3.5" /> 1次</div>
                    <div class="text-theme-text-tertiary mt-0.5">约15分钟/场</div>
                  </div>
                  <div class="text-center">
                    <div class="flex items-center gap-1"><Clock class="w-3.5 h-3.5" /> 5主问+追问</div>
                    <div class="text-theme-text-tertiary mt-0.5">完赛预计</div>
                  </div>
                  <span class="inline-flex items-center gap-1.5 px-4 py-2 rounded-xl font-bold bg-theme-primary text-theme-on-primary meta-text hover:opacity-90 transition-opacity">
                    立即体验
                    <ArrowRight class="w-4 h-4" />
                  </span>
                </div>
                <ArrowRight class="sm:hidden w-5 h-5 shrink-0 text-theme-primary" />
              </div>
            </button>

            <div class="grid grid-cols-1 md:grid-cols-3 gap-3 sm:gap-4">
              <!-- 热门题目 -->
              <div class="p-3 sm:p-4 rounded-xl bg-theme-bg">
                <div class="flex items-center justify-between mb-3">
                  <h4 class="card-title">热门题目</h4>
                  <span class="px-2 py-0.5 bg-theme-danger-bg text-theme-danger rounded caption-text">hot</span>
                </div>
                <div class="space-y-2.5">
                  <button
                    type="button"
                    v-for="q in interviewQuestions"
                    :key="q.id"
                    class="flex items-center justify-between cursor-pointer hover:text-theme-primary transition-colors w-full text-left"
                    @click="router.push(`/interview/question/${q.id}`)"
                  >
                    <span class="card-summary line-clamp-1">{{ q.title }}</span>
                    <span class="meta-text flex-shrink-0 ml-2">{{ q.submissionCount || 0 }}提交</span>
                  </button>
                  <div v-if="interviewQuestions.length === 0" class="meta-text">暂无题目</div>
                </div>
              </div>

              <!-- 热门面经 -->
              <div class="p-3 sm:p-4 rounded-xl bg-theme-bg">
                <div class="flex items-center justify-between mb-3">
                  <h4 class="card-title">面经复盘</h4>
                  <span class="px-2 py-0.5 bg-theme-info-bg text-theme-info rounded caption-text">new</span>
                </div>
                <div class="space-y-2.5">
                  <button
                    type="button"
                    v-for="exp in interviewExperiences"
                    :key="exp.id"
                    class="flex items-center justify-between cursor-pointer hover:text-theme-primary transition-colors w-full text-left"
                    @click="router.push(`/interview/experience/${exp.id}`)"
                  >
                    <span class="card-summary line-clamp-1">{{ exp.title }}</span>
                    <span v-if="exp.company" class="meta-text flex-shrink-0 ml-2">{{ exp.company }}</span>
                  </button>
                  <div v-if="interviewExperiences.length === 0" class="meta-text">暂无面经</div>
                </div>
              </div>

              <!-- 题目分类 -->
              <div class="p-3 sm:p-4 rounded-xl bg-theme-bg">
                <div class="flex items-center justify-between mb-3">
                  <h4 class="card-title">题目分类</h4>
                </div>
                <div class="space-y-2.5">
                  <button
                    type="button"
                    v-for="cat in interviewCategories"
                    :key="cat.id"
                    class="flex items-center justify-between cursor-pointer hover:text-theme-primary transition-colors w-full text-left"
                    @click="router.push('/learn/questions')"
                  >
                    <span class="card-summary line-clamp-1">{{ cat.name }}</span>
                    <span class="meta-text flex-shrink-0 ml-2">{{ cat.questionCount || 0 }}道</span>
                  </button>
                  <div v-if="interviewCategories.length === 0" class="meta-text">暂无分类</div>
                </div>
              </div>
            </div>
          </div>
        </div>
      </div>

      <!-- 阅读（主线四：书籍/书单/金句/文章内容流/标签） -->
      <div id="home-reading" class="py-6 sm:py-10 bg-theme-accent/40 scroll-mt-20">
        <div class="content-container">
          <!-- 章节头 -->
          <div class="home-chapter-head">
            <div class="flex items-center gap-3">
              <span class="home-chapter-no">04</span>
              <div class="w-px h-9 bg-theme-border"></div>
              <div>
                <h2 class="text-lg sm:text-xl font-bold text-theme-text">阅读空间</h2>
                <p class="meta-text text-theme-text-tertiary">书籍阅读 · 散文随笔 · 技术笔记</p>
              </div>
            </div>
            <button @click="router.push('/reading')" class="home-chapter-link">
              <span>进入读书空间</span>
              <ArrowRight class="w-3 h-3 sm:w-4 sm:h-4" />
            </button>
          </div>

          <div class="p-4 sm:p-5 rounded-2xl bg-theme-surface border border-theme-border shadow-theme-sm">

            <div class="grid grid-cols-1 md:grid-cols-4 gap-3 sm:gap-4">
              <!-- 精选书籍 -->
              <button
                type="button"
                v-if="readingBooks.length > 0"
                class="relative h-28 sm:h-32 rounded-xl overflow-hidden cursor-pointer w-full text-left border border-theme-border"
                @click="router.push(`/reading/book/${readingBooks[0].id}`)"
              >
                <LazyImage
                  :src="readingBooks[0].cover"
                  :alt="readingBooks[0].title"
                  class="absolute inset-0 w-full h-full object-cover"
                />
                <div class="absolute inset-0 bg-theme-primary-soft/95 p-3 sm:p-4">
                  <span class="inline-block px-2 py-0.5 bg-theme-primary/15 text-theme-primary caption-text rounded mb-2 font-medium">精选好书</span>
                  <h4 class="text-theme-text card-title mb-1 line-clamp-1">{{ readingBooks[0].title }}</h4>
                  <p class="text-theme-text-secondary meta-text mb-3">{{ readingBooks[0].author }}</p>
                  <span class="px-3 py-1 bg-theme-primary text-theme-on-primary rounded-full caption-text font-medium">立即阅读</span>
                </div>
              </button>
              <div v-else class="relative h-28 sm:h-32 rounded-xl overflow-hidden border border-theme-border bg-theme-primary-soft">
                <div class="absolute inset-0 p-3 sm:p-4">
                  <span class="inline-block px-2 py-0.5 bg-theme-primary/15 text-theme-primary caption-text rounded mb-2 font-medium">精选好书</span>
                  <h4 class="text-theme-text card-title mb-1">暂无推荐</h4>
                </div>
              </div>

              <!-- 热门书单 -->
              <div class="p-3 sm:p-4 rounded-xl bg-theme-bg">
                <div class="flex items-center gap-2 mb-3">
                  <div class="w-7 h-7 rounded-lg bg-theme-primary-soft flex items-center justify-center">
                    <Flame class="w-3.5 h-3.5 text-theme-primary" />
                  </div>
                  <h4 class="card-title">热门书单</h4>
                </div>
                <div class="space-y-2.5">
                  <button
                    type="button"
                    v-for="bl in readingBookLists"
                    :key="bl.id"
                    class="flex items-center gap-2 cursor-pointer hover:text-theme-primary transition-colors w-full text-left"
                    @click="router.push(`/reading/book-list/${bl.id}`)"
                  >
                    <div class="w-1.5 h-1.5 rounded-full bg-theme-primary"></div>
                    <span class="card-summary line-clamp-1">{{ bl.title }}</span>
                  </button>
                  <div v-if="readingBookLists.length === 0" class="meta-text">暂无书单</div>
                </div>
              </div>

              <!-- 金句摘录 -->
              <div class="p-3 sm:p-4 rounded-xl bg-theme-bg">
                <div class="flex items-center gap-2 mb-3">
                  <div class="w-7 h-7 rounded-lg bg-theme-primary-soft flex items-center justify-center">
                    <Quote class="w-3.5 h-3.5 text-theme-primary" />
                  </div>
                  <h4 class="card-title">金句摘录</h4>
                </div>
                <p v-if="readingQuotes.length > 0" class="card-summary italic line-clamp-3">
                  "{{ readingQuotes[0].content }}"
                </p>
                <p v-else class="meta-text">暂无金句</p>
              </div>

              <!-- 读书统计 -->
              <div class="p-3 sm:p-4 rounded-xl bg-theme-bg">
                <div class="flex items-center gap-2 mb-3">
                  <div class="w-7 h-7 rounded-lg bg-theme-primary-soft flex items-center justify-center">
                    <Users class="w-3.5 h-3.5 text-theme-primary" />
                  </div>
                  <h4 class="card-title">读书统计</h4>
                </div>
                <div class="text-center">
                  <p class="text-2xl sm:text-3xl font-bold text-theme-primary">{{ readingBooks.length + readingBookLists.length }}</p>
                  <p class="meta-text mt-1">本精选好书</p>
                </div>
              </div>
            </div>
          </div>

          <!-- 散文随笔 · 技术笔记（原"按主题探索"并入阅读主线） -->
          <div class="mt-3 sm:mt-4">
            <div class="flex items-center justify-between mb-3 sm:mb-4">
              <div class="flex items-center gap-2">
                <PenLine class="w-4 h-4 sm:w-5 sm:h-5 text-theme-primary" />
                <div>
                  <h3 class="section-title">散文 · 技术笔记</h3>
                  <p class="meta-text">按主题浏览社区创作的内容流</p>
                </div>
              </div>
            </div>

            <div class="flex flex-wrap gap-1.5 sm:gap-2 mb-3 sm:mb-4">
              <button
                v-for="theme in (themes.length > 0 ? themes : [{ id: '1', name: '散文', key: 'prose' }])"
                :key="theme.id"
                @click="selectTheme(theme.id, theme.name)"
                class="px-3 sm:px-4 py-1 sm:py-1.5 rounded-full text-xs sm:text-sm font-medium transition-all"
                :class="activeTheme === theme.name ? 'bg-theme-primary text-theme-on-primary' : 'bg-theme-surface text-theme-text-secondary hover:bg-theme-surface-highlight'"
              >
                {{ theme.name }}
              </button>
            </div>

            <div class="p-3 sm:p-4 rounded-xl bg-theme-surface border border-theme-border">
              <div class="flex items-center justify-between mb-3">
                <h4 class="card-title">
                  <span class="text-theme-primary">{{ getThemeCode(activeTheme) }}</span>
                  {{ activeTheme }}精选
                </h4>
                <button @click="viewMore(activeTheme)" class="flex items-center gap-1 meta-text text-theme-text-secondary hover:text-theme-primary transition-colors">
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
                  class="flex items-center gap-2 cursor-pointer w-full text-left group"
                >
                  <div class="w-1 h-1 rounded-full bg-theme-primary"></div>
                  <span class="card-title line-clamp-1 flex-1 group-hover:text-theme-primary transition-colors">
                    {{ article.title }}
                  </span>
                  <span class="meta-text flex-shrink-0">{{ article.createdAt }}</span>
                </button>
              </div>
            </div>
          </div>
        </div>
      </div>

      <!-- 社区创作（主线五：动态广场/名家创作/话题标签） -->
      <div id="home-community" class="py-6 sm:py-10 bg-theme-bg scroll-mt-20">
        <div class="content-container">
          <!-- 章节头 -->
          <div class="home-chapter-head">
            <div class="flex items-center gap-3">
              <span class="home-chapter-no">05</span>
              <div class="w-px h-9 bg-theme-border"></div>
              <div>
                <h2 class="text-lg sm:text-xl font-bold text-theme-text">社区创作</h2>
                <p class="meta-text text-theme-text-tertiary">散文发布 · 技术文档 · 话题讨论 · 名家动态</p>
              </div>
            </div>
            <div class="flex items-center gap-2 sm:gap-3">
              <button @click="handleWrite" class="hidden sm:flex items-center gap-1.5 px-3 py-1.5 rounded-full meta-text font-medium bg-theme-primary text-theme-on-primary hover:opacity-90 transition-opacity">
                <PenLine class="w-3.5 h-3.5" />
                去创作
              </button>
              <button @click="router.push('/feed')" class="home-chapter-link">
                <span>动态广场</span>
                <ArrowRight class="w-3 h-3 sm:w-4 sm:h-4" />
              </button>
            </div>
          </div>

          <div v-if="hotFeedList.length > 0" class="grid grid-cols-1 md:grid-cols-3 gap-3 sm:gap-4">
            <button
              v-for="feed in hotFeedList"
              :key="feed.eventId"
              @click="router.push(getFeedTargetPath(feed))"
              class="p-4 rounded-xl border text-left transition-all hover:scale-105 hover:shadow-theme-md bg-theme-surface border-theme-border"
            >
              <div class="flex items-center gap-2 mb-2">
                <div class="w-8 h-8 rounded-full bg-theme-primary-soft flex items-center justify-center flex-shrink-0">
                  <span class="text-xs font-bold text-theme-primary">
                    {{ (feed.userNickname || 'A').charAt(0) }}
                  </span>
                </div>
                <div class="flex-1 min-w-0">
                  <p class="card-title truncate">
                    {{ feed.userNickname || '匿名' }}
                  </p>
                  <p class="meta-text">
                    {{ getFeedActionText(feed) }}
                  </p>
                </div>
              </div>
              <p v-if="feed.summary" class="card-summary line-clamp-2 mt-2">
                {{ feed.summary }}
              </p>
            </button>
          </div>
          <div v-else class="p-6 sm:p-8 rounded-xl text-center bg-theme-surface border border-theme-border">
            <MessageCircle class="w-8 h-8 mx-auto mb-2 opacity-30 text-theme-text-secondary" />
            <p class="card-title mb-1">社区还很安静</p>
            <p class="meta-text">第一批创作者正在赶来，期待他们的故事</p>
            <button
              @click="handleWrite"
              class="mt-3 inline-flex items-center gap-1 px-4 py-2 rounded-full text-xs font-medium bg-theme-primary text-theme-on-primary hover:opacity-90 transition-opacity"
            >
              <Sparkles class="w-3.5 h-3.5" />
              成为第一位创作者
            </button>
          </div>

          <!-- 名家创作 -->
          <div class="mt-4 sm:mt-6">
            <div class="flex items-center justify-between mb-3 sm:mb-4">
              <div class="flex items-center gap-2">
                <User class="w-4 h-4 sm:w-5 sm:h-5 text-theme-primary" />
                <div>
                  <h3 class="section-title">旭林名家录</h3>
                  <p class="meta-text">活跃创作者与他们的作品</p>
                </div>
              </div>
              <Link to="/authors" class="flex items-center gap-1.5 meta-text text-theme-text-secondary hover:text-theme-primary transition-colors">
                <span>全部作者</span>
                <ArrowRight class="w-3 h-3 sm:w-4 sm:h-4" />
              </Link>
            </div>

            <div v-if="authors.length > 0" class="grid grid-cols-2 sm:grid-cols-4 lg:grid-cols-7 gap-3 sm:gap-4">
              <button
                type="button"
                v-for="author in authors"
                :key="author.id"
                @click="goToAuthor(author.id)"
                class="text-center p-3 sm:p-4 rounded-xl cursor-pointer transition-colors w-full bg-theme-surface hover:bg-theme-surface-highlight border border-theme-border"
              >
                <div class="w-10 h-10 sm:w-12 sm:h-12 mx-auto mb-2 rounded-full bg-theme-primary-soft flex items-center justify-center">
                  <span class="text-sm font-bold text-theme-primary">{{ author.avatar }}</span>
                </div>
                <p class="card-title mb-1">{{ author.name }}</p>
                <p class="meta-text">已创作 {{ author.works }} 篇</p>
                <p class="meta-text">{{ author.likes }} 喜欢</p>
                <p class="meta-text">坚持 {{ author.days }} 天</p>
              </button>
            </div>
            <div v-else class="py-8 text-center">
              <p class="meta-text">暂无名家数据</p>
            </div>
          </div>

          <!-- 话题标签 -->
          <div class="mt-4 sm:mt-6 rounded-xl p-4 sm:p-5 bg-theme-surface border border-theme-border">
            <div class="flex items-center gap-2 mb-3">
              <Star class="w-4 h-4 sm:w-5 sm:h-5 text-theme-primary" />
              <h3 class="section-title">热门话题标签</h3>
            </div>
            <nav class="flex flex-wrap gap-1.5 sm:gap-2">
              <button
                type="button"
                v-for="tag in (tags.length > 0 ? tags : [{ id: '1', name: '文学' }, { id: '2', name: '散文' }, { id: '3', name: '随笔' }])"
                :key="tag.id || tag"
                @click="router.push(`/tag/${encodeURIComponent(tag.name || tag)}`)"
                class="inline-flex items-center gap-1 px-2.5 sm:px-3 py-1 sm:py-1.5 rounded-full cursor-pointer transition-all hover:opacity-80 bg-theme-accent text-theme-primary meta-text"
              >
                <Tag class="w-3 h-3" />
                {{ tag.name || tag }}
              </button>
            </nav>
          </div>
        </div>
      </div>

      <!-- 底部 CTA（按登录态区分文案与动作） -->
      <section class="py-6 sm:py-8 bg-theme-bg">
        <div class="content-container">
          <!-- 未登录：注册转化 CTA（浅色柔和版） -->
          <div v-if="!isLoggedIn" class="relative overflow-hidden rounded-2xl bg-theme-primary-soft border border-theme-primary/25 shadow-theme-sm">
            <div class="relative px-6 py-10 sm:py-12 text-center">
              <h2 class="text-2xl sm:text-3xl font-black text-theme-text mb-3">准备好开启你的求职之旅了吗？</h2>
              <p class="text-theme-text-secondary meta-text sm:body-text mb-6 max-w-lg mx-auto">开始诊断你的简历，3 分钟出报告，AI 自动生成针对性面试题</p>
              <div class="flex flex-wrap justify-center gap-3 sm:gap-4">
                <button @click="goRegister" class="px-6 sm:px-8 py-3 bg-theme-primary text-theme-on-primary font-bold rounded-xl shadow-theme-md hover:opacity-90 transition-opacity">
                  开始诊断你的简历
                </button>
                <button @click="router.push('/about')" class="px-6 sm:px-8 py-3 bg-theme-surface border border-theme-border text-theme-text font-semibold rounded-xl hover:bg-theme-surface-highlight transition-colors">
                  了解更多功能
                </button>
              </div>
              <p class="text-theme-text-tertiary caption-text mt-5">免费注册 · 无需绑定支付方式 · 随时注销</p>
            </div>
          </div>

          <!-- 已登录：行动 CTA（浅色柔和版） -->
          <div v-else class="relative overflow-hidden rounded-2xl bg-theme-primary-soft border border-theme-primary/25 shadow-theme-sm">
            <div class="relative px-6 py-8 sm:py-10 text-center">
              <h3 class="text-xl sm:text-2xl font-bold text-theme-text mb-2">距离你的 Dream Offer 还有多远？</h3>
              <p class="text-theme-text-secondary meta-text mb-5">3 分钟诊断简历薄弱点，再来一场 AI 模拟面试找准方向</p>
              <div class="flex flex-wrap items-center justify-center gap-3 sm:gap-4">
                <button @click="goResumeOptimize" class="inline-flex items-center gap-2 px-5 sm:px-6 py-2.5 bg-theme-primary text-theme-on-primary rounded-xl meta-text font-semibold hover:opacity-90 transition-opacity shadow-theme-md">
                  <FileText class="w-4 h-4" />完善简历
                </button>
                <button @click="goVoiceInterview" class="inline-flex items-center gap-2 px-5 sm:px-6 py-2.5 bg-theme-surface border border-theme-border text-theme-text rounded-xl meta-text font-semibold hover:bg-theme-surface-highlight transition-colors">
                  <Mic class="w-4 h-4" />预约模拟面试
                </button>
              </div>
            </div>
          </div>
        </div>
      </section>

      <div class="mt-2 sm:mt-4">
        <SiteFooter />
      </div>

      <BackToTop />
    </template>
  </div>
</template>

<style scoped>
/* 首页动效（与原型语义一致，基于主题变量实现） */
@keyframes homeFloat {
  0%, 100% { transform: translateY(0px); }
  50% { transform: translateY(-8px); }
}
.home-float { animation: homeFloat 4s ease-in-out infinite; }
.home-float-delayed { animation: homeFloat 4s ease-in-out 1.5s infinite; }

@keyframes homeCardLift {
  from { transform: translateY(0); box-shadow: var(--shadow-md, 0 4px 6px rgba(0,0,0,0.05)); }
  to { transform: translateY(-4px); }
}
.home-card-lift { transition: all 0.3s cubic-bezier(0.4, 0, 0.2, 1); }
.home-card-lift:hover { transform: translateY(-4px); box-shadow: var(--shadow-lg, 0 10px 30px rgba(0,0,0,0.1)); }

/* ============== 问候带（V11.3 样式优化 + 特效） ============== */
.home-greeting-band {
  background: linear-gradient(135deg, var(--theme-primary-soft) 0%, transparent 60%), var(--theme-bg);
  position: relative;
  overflow: hidden;
}
.home-greeting-band::before {
  content: '';
  position: absolute;
  top: 0; left: 0;
  width: 4px; height: 100%;
  background: var(--theme-primary);
  opacity: 0.6;
}
.home-greeting-card {
  position: relative;
  padding: 1rem 1.25rem;
  background: var(--theme-surface-elevated);
  border: 1px solid var(--theme-border);
  border-radius: 1rem;
  box-shadow: var(--shadow-sm);
  transition: box-shadow 0.3s ease;
}
.home-greeting-card:hover {
  box-shadow: var(--shadow-md);
}
.home-greeting-emoji {
  display: inline-block;
  animation: homeEmojiFloat 3s ease-in-out infinite;
}
@keyframes homeEmojiFloat {
  0%, 100% { transform: translateY(0) rotate(0deg); }
  50% { transform: translateY(-4px) rotate(5deg); }
}
.home-join-days {
  font-size: 1.125rem;
  font-weight: 700;
  color: var(--theme-primary);
  font-variant-numeric: tabular-nums;
  text-shadow: 0 0 12px var(--theme-primary-soft);
  animation: homeDaysPulse 2.5s ease-in-out infinite;
}
@keyframes homeDaysPulse {
  0%, 100% { opacity: 1; }
  50% { opacity: 0.8; }
}
@media (prefers-reduced-motion: reduce) {
  .home-greeting-emoji,
  .home-join-days { animation: none; }
}

/* 仪表盘快捷入口按钮（原型 shortcut-btn，V11.3 加特效） */
.home-shortcut-btn {
  height: 2.5rem;
  padding: 0 1.25rem;
  background: var(--theme-surface);
  border: 1px solid var(--theme-border);
  border-radius: 0.75rem;
  font-size: 0.875rem;
  font-weight: 500;
  color: var(--theme-text);
  display: inline-flex;
  align-items: center;
  gap: 0.5rem;
  box-shadow: var(--shadow-sm, 0 1px 2px rgba(0,0,0,0.05));
  transition: transform 0.2s ease, box-shadow 0.2s ease, background 0.2s ease, color 0.2s ease, border-color 0.2s ease;
  cursor: pointer;
  position: relative;
  overflow: hidden;
}
.home-shortcut-btn::after {
  content: '';
  position: absolute;
  inset: 0;
  background: linear-gradient(120deg, transparent 30%, var(--theme-primary-soft) 50%, transparent 70%);
  transform: translateX(-100%);
  transition: transform 0.5s ease;
  pointer-events: none;
}
.home-shortcut-btn:hover {
  background: var(--theme-primary);
  color: var(--theme-on-primary);
  border-color: var(--theme-primary);
  transform: translateY(-2px);
  box-shadow: var(--shadow-md), 0 0 16px var(--theme-primary-soft);
}
.home-shortcut-btn:hover::after {
  transform: translateX(100%);
}
.home-shortcut-btn:hover svg {
  color: var(--theme-on-primary);
  transform: scale(1.15);
  transition: transform 0.2s ease;
}
.home-shortcut-btn:active {
  transform: translateY(0px);
  box-shadow: var(--shadow-sm);
}
.home-shortcut-btn svg {
  transition: transform 0.2s ease;
}
@media (prefers-reduced-motion: reduce) {
  .home-shortcut-btn::after,
  .home-shortcut-btn:hover svg { animation: none; transition: none; }
  .home-shortcut-btn:hover { transform: none; }
}

.stat-number { font-variant-numeric: tabular-nums; }
.progress-ring { transition: stroke-dashoffset 0.5s ease; }

/* 章节头：编号 + 竖分隔线 + 标题，建立页面叙事层次（V11.1 首页重构） */
.home-chapter-head {
  display: flex;
  align-items: flex-end;
  justify-content: space-between;
  gap: 0.75rem;
  margin-bottom: 0.75rem;
}
.home-chapter-no {
  font-size: 1.375rem;
  font-weight: 900;
  line-height: 1;
  color: var(--theme-primary);
  opacity: 0.4;
  font-variant-numeric: tabular-nums;
  letter-spacing: 0.05em;
}
.home-chapter-link {
  display: inline-flex;
  align-items: center;
  gap: 0.25rem;
  font-size: 0.875rem;
  font-weight: 500;
  color: var(--theme-primary);
  transition: opacity 0.2s ease;
  flex-shrink: 0;
  padding-bottom: 0.125rem;
}
.home-chapter-link:hover { opacity: 0.8; }

/* 五大主线导航卡片（V11.3：渐变描边 + 悬浮抬升 + 图标弹性 + 入场动画） */
.home-mainline-card {
  position: relative;
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 0.375rem;
  padding: 0.875rem 0.5rem 0.75rem;
  border-radius: 1rem;
  background: color-mix(in srgb, var(--theme-surface) 92%, transparent);
  border: 1px solid var(--theme-border);
  text-decoration: none;
  overflow: hidden;
  isolation: isolate;
  /* 入场：轻微上浮淡入，逐个错峰 */
  animation: mainline-rise 0.5s ease both;
  transition: transform 0.25s ease, box-shadow 0.25s ease, border-color 0.25s ease;
}
.home-mainline-card::before {
  /* 悬浮时底部溢出的品牌光晕 */
  content: '';
  position: absolute;
  inset: auto -30% -60% -30%;
  height: 70%;
  background: radial-gradient(closest-side, color-mix(in srgb, var(--theme-primary) 22%, transparent), transparent);
  opacity: 0;
  z-index: -1;
  transition: opacity 0.3s ease;
}
.home-mainline-card:hover {
  transform: translateY(-4px);
  border-color: color-mix(in srgb, var(--theme-primary) 45%, var(--theme-border));
  box-shadow: 0 12px 24px -12px color-mix(in srgb, var(--theme-primary) 35%, transparent);
}
.home-mainline-card:hover::before { opacity: 1; }
.home-mainline-card:active { transform: translateY(-1px); }

.home-mainline-icon {
  position: relative;
  display: flex;
  align-items: center;
  justify-content: center;
  width: 2.75rem;
  height: 2.75rem;
  border-radius: 0.875rem;
  color: var(--theme-primary);
  background: var(--theme-primary-soft);
  transition: transform 0.3s cubic-bezier(0.34, 1.56, 0.64, 1), background-color 0.25s ease, color 0.25s ease, box-shadow 0.25s ease;
}
.home-mainline-card:hover .home-mainline-icon {
  transform: scale(1.12) rotate(-4deg);
  background: var(--theme-primary);
  color: var(--theme-on-primary);
  box-shadow: 0 6px 14px -6px color-mix(in srgb, var(--theme-primary) 60%, transparent);
}

.home-mainline-text {
  display: flex;
  align-items: baseline;
  gap: 0.25rem;
  font-size: 0.8125rem;
  line-height: 1.2;
  white-space: nowrap;
  transition: color 0.2s ease;
}
.home-mainline-card:hover .home-mainline-text { color: var(--theme-primary); }

.home-mainline-arrow {
  position: absolute;
  top: 0.5rem;
  right: 0.5rem;
  display: flex;
  align-items: center;
  justify-content: center;
  width: 1.25rem;
  height: 1.25rem;
  border-radius: 9999px;
  color: var(--theme-primary);
  background: var(--theme-primary-soft);
  opacity: 0;
  transform: translate(-4px, 4px) scale(0.6);
  transition: opacity 0.25s ease, transform 0.25s ease;
}
.home-mainline-card:hover .home-mainline-arrow {
  opacity: 1;
  transform: translate(0, 0) scale(1);
}

@keyframes mainline-rise {
  from { opacity: 0; transform: translateY(14px); }
  to { opacity: 1; transform: translateY(0); }
}

/* 窄屏：5 列过挤时收紧内边距与图标 */
@media (max-width: 480px) {
  .home-mainline-card { padding: 0.625rem 0.25rem 0.5rem; border-radius: 0.875rem; }
  .home-mainline-icon { width: 2.25rem; height: 2.25rem; border-radius: 0.75rem; }
  .home-mainline-text { font-size: 0.75rem; }
  .home-mainline-arrow { display: none; }
}

@media (prefers-reduced-motion: reduce) {
  .home-mainline-card { animation: none; }
  .home-mainline-card:hover { transform: none; }
  .home-mainline-card:hover .home-mainline-icon { transform: none; }
}

</style>
