import { createRouter, createWebHistory, type RouteRecordRaw } from 'vue-router'
import { useUserStore } from '@/stores/user'

// ============ 页面组件导入 ============
const HomePage = () => import('@/pages/HomePage.vue')
const ArticleDetailPage = () => import('@/pages/ArticleDetailPage.vue')
const SearchPage = () => import('@/pages/SearchPage.vue')
const ListPage = () => import('@/pages/ListPage.vue')
const UserPage = () => import('@/pages/UserPage.vue')
const UserProfilePage = () => import('@/pages/UserProfilePage.vue')
const UserSettingsPage = () => import('@/pages/UserSettingsPage.vue')
const AuthorPage = () => import('@/pages/AuthorPage.vue')
const AuthorsPage = () => import('@/pages/AuthorsPage.vue')
const HelpCenterPage = () => import('@/pages/HelpCenter.vue')
const AboutUsPage = () => import('@/pages/AboutUs.vue')
const UserAgreementPage = () => import('@/pages/UserAgreement.vue')
const ReportFeedbackPage = () => import('@/pages/ReportFeedback.vue')
const LoginPage = () => import('@/pages/LoginPage.vue')
const RegisterPage = () => import('@/pages/RegisterPage.vue')
const PublishPage = () => import('@/pages/PublishPage.vue')
const NotFoundPage = () => import('@/pages/NotFoundPage.vue')
const ReadingPage = () => import('@/pages/ReadingPage.vue')
const InterviewPage = () => import('@/pages/InterviewPage.vue')
const QuestionDetailPage = () => import('@/pages/interview/QuestionDetailPage.vue')
const ExperienceDetailPage = () => import('@/pages/interview/ExperienceDetailPage.vue')
const ResumeTemplatePage = () => import('@/pages/interview/ResumeTemplatePage.vue')
const GrowthRankingPage = () => import('@/pages/GrowthRankingPage.vue')
const AchievementsPage = () => import('@/pages/AchievementsPage.vue')

// ============ 路由配置 ============

// 路由Meta类型定义
declare module 'vue-router' {
  interface RouteMeta {
    /**
     * 是否需要登录才能访问
     * @default false
     */
    requiresAuth?: boolean
    /**
     * 页面标题
     */
    title?: string
    /**
     * 是否为公开页面（登录后不需要重定向）
     * @default false
     */
    isPublic?: boolean
    /**
     * robots 元标签，默认允许收录
     */
    robots?: 'noindex,nofollow' | 'noindex,follow' | 'index,follow'
  }
}

// 路由配置
const routes: RouteRecordRaw[] = [
  // ============ 公开页面（无需登录） ============
  {
    path: '/',
    name: 'home',
    component: HomePage,
    meta: { title: '首页', isPublic: true }
  },
  {
    path: '/reading',
    name: 'reading',
    component: ReadingPage,
    meta: { title: '读书空间', isPublic: true }
  },
  {
    path: '/interview',
    name: 'interview',
    component: InterviewPage,
    meta: { title: '面试指南', isPublic: true }
  },
  {
    path: '/interview/question/:id',
    name: 'interview-question',
    component: QuestionDetailPage,
    meta: { title: '题目详情', isPublic: true }
  },
  {
    path: '/interview/experience/:id',
    name: 'interview-experience',
    component: ExperienceDetailPage,
    meta: { title: '面经详情', isPublic: true }
  },
  {
    path: '/interview/resume-templates',
    name: 'interview-resume-templates',
    component: ResumeTemplatePage,
    meta: { title: '简历模板', isPublic: true }
  },
  {
    path: '/article/:id/:slug?',
    name: 'article',
    component: ArticleDetailPage,
    meta: { title: '文章详情', isPublic: true }
  },
  {
    path: '/search',
    name: 'search',
    component: SearchPage,
    meta: { title: '搜索', isPublic: true }
  },
  // ============ 语义化列表页路由（SEO 优先） ============
  {
    path: '/category/:name(.*)?',
    name: 'category',
    component: ListPage,
    meta: { title: '分类文章', isPublic: true }
  },
  {
    path: '/tag/:name(.*)?',
    name: 'tag',
    component: ListPage,
    meta: { title: '标签文章', isPublic: true }
  },
  // 兼容旧路径（保留，不再作为主要入口）
  {
    path: '/list',
    name: 'list',
    component: ListPage,
    meta: { title: '文章列表', isPublic: true }
  },
  {
    path: '/author/:id',
    name: 'author',
    component: AuthorPage,
    meta: { title: '作者主页', isPublic: true }
  },
  {
    path: '/authors',
    name: 'authors',
    component: AuthorsPage,
    meta: { title: '作者列表', isPublic: true }
  },
  {
    path: '/ranking',
    name: 'ranking',
    component: GrowthRankingPage,
    meta: { title: '成长排行榜', isPublic: true }
  },
  {
    path: '/achievements',
    name: 'achievements',
    component: AchievementsPage,
    meta: { title: '成就徽章', isPublic: true }
  },
  {
    path: '/help',
    name: 'help',
    component: HelpCenterPage,
    meta: { title: '帮助中心', isPublic: true }
  },
  {
    path: '/about',
    name: 'about',
    component: AboutUsPage,
    meta: { title: '关于我们', isPublic: true }
  },
  {
    path: '/agreement',
    name: 'agreement',
    component: UserAgreementPage,
    meta: { title: '用户协议', isPublic: true }
  },
  {
    path: '/report',
    name: 'report',
    component: ReportFeedbackPage,
    meta: { title: '问题反馈', isPublic: true }
  },
  // ============ 认证页面（登录后重定向） ============
  {
    path: '/login',
    name: 'login',
    component: LoginPage,
    meta: { title: '登录', isPublic: true, robots: 'noindex,follow' }
  },
  {
    path: '/register',
    name: 'register',
    component: RegisterPage,
    meta: { title: '注册', isPublic: true, robots: 'noindex,follow' }
  },
  // ============ 需要登录的页面 ============
  {
    path: '/user',
    name: 'user',
    component: UserPage,
    meta: { requiresAuth: true, title: '个人中心', robots: 'noindex,nofollow' }
  },
  {
    path: '/user/profile',
    name: 'user-profile',
    component: UserProfilePage,
    meta: { requiresAuth: true, title: '编辑个人资料', robots: 'noindex,nofollow' }
  },
  {
    path: '/user/settings',
    name: 'user-settings',
    component: UserSettingsPage,
    meta: { requiresAuth: true, title: '账号设置', robots: 'noindex,nofollow' }
  },
  {
    path: '/publish',
    name: 'publish',
    component: PublishPage,
    meta: { requiresAuth: true, title: '发布文章', robots: 'noindex,nofollow' }
  },
  // ============ 404 页面 ============
  {
    path: '/:pathMatch(.*)*',
    name: 'not-found',
    component: NotFoundPage,
    meta: { title: '页面未找到', isPublic: true, robots: 'noindex,follow' }
  }
]

// 创建路由实例
const router = createRouter({
  history: createWebHistory(),
  routes,
  scrollBehavior(_to, _from, savedPosition) {
    if (savedPosition) {
      return savedPosition
    } else {
      return { top: 0 }
    }
  }
})

// ============ 路由守卫 ============

/**
 * 全局前置守卫
 */
router.beforeEach(async (to, _from, next) => {
  const userStore = useUserStore()

  // 1. 初始化用户状态（确保登录状态正确）
  if (!userStore.isUserInitialized) {
    await userStore.initializeUser()
  }

  // 2. 设置页面标题
  if (to.meta.title) {
    document.title = `${to.meta.title} - 墨韵智库`
  }

  // 3. 登录后访问登录/注册页：重定向到首页
  if (userStore.isAuthenticated && (to.name === 'login' || to.name === 'register')) {
    return next('/')
  }

  // 4. 检查是否需要登录
  if (to.meta.requiresAuth) {
    if (!userStore.isAuthenticated) {
      // 未登录，跳转到登录页，并保存重定向地址
      return next({
        name: 'login',
        query: { redirect: to.fullPath }
      })
    }
  }

  // 5. 正常访问
  next()
})

export default router
