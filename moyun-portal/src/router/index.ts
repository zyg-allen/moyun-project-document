import { createRouter, createWebHistory, type RouteRecordRaw } from 'vue-router'
import { useUserStore } from '@/stores/user'
import { hasPendingAiRequests } from '@/api/client'

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
const MyReportsPage = () => import('@/pages/MyReportsPage.vue')
const MyFeedbackPage = () => import('@/pages/MyFeedbackPage.vue')
const LoginPage = () => import('@/pages/LoginPage.vue')
const RegisterPage = () => import('@/pages/RegisterPage.vue')
const ForgotPasswordPage = () => import('@/pages/ForgotPasswordPage.vue')
const PublishPage = () => import('@/pages/PublishPage.vue')
const MyArticlesPage = () => import('@/pages/MyArticlesPage.vue')
const NotFoundPage = () => import('@/pages/NotFoundPage.vue')
const ReadingPage = () => import('@/pages/ReadingPage.vue')
const BookDetailPage = () => import('@/pages/reading/BookDetailPage.vue')
const BookListDetailPage = () => import('@/pages/reading/BookListDetailPage.vue')
const ChapterReaderPage = () => import('@/pages/reading/ChapterReaderPage.vue')
const MyBookshelfPage = () => import('@/pages/reading/MyBookshelfPage.vue')
const DiscoverPage = () => import('@/pages/reading/DiscoverPage.vue')
const QuoteListPage = () => import('@/pages/reading/QuoteListPage.vue')
const GrowthTimelinePage = () => import('@/pages/GrowthTimelinePage.vue')
const InterviewPage = () => import('@/pages/InterviewPage.vue')
const QuestionDetailPage = () => import('@/pages/learn/QuestionDetailPage.vue')
const ExperienceDetailPage = () => import('@/pages/interview/ExperienceDetailPage.vue')
const ResumeTemplatePage = () => import('@/pages/interview/ResumeTemplatePage.vue')
const QuestionListPage = () => import('@/pages/learn/QuestionListPage.vue')
const ExperienceListPage = () => import('@/pages/interview/ExperienceListPage.vue')
const MyResumesPage = () => import('@/pages/interview/MyResumesPage.vue')
const ResumeOptimizePage = () => import('@/pages/interview/ResumeOptimizePage.vue')
const ResumeEditPage = () => import('@/pages/interview/ResumeEditPage.vue')
const CompanyPage = () => import('@/pages/interview/CompanyPage.vue')

const GrowthRankingPage = () => import('@/pages/GrowthRankingPage.vue')
const AchievementsPage = () => import('@/pages/AchievementsPage.vue')
const FollowListPage = () => import('@/pages/FollowListPage.vue')
const MessagesPage = () => import('@/pages/MessagesPage.vue')
const FeedPage = () => import('@/pages/FeedPage.vue')
const ColumnsPage = () => import('@/pages/ColumnsPage.vue')
const ColumnDetailPage = () => import('@/pages/ColumnDetailPage.vue')
const ColumnEditPage = () => import('@/pages/ColumnEditPage.vue')
const MyColumnsPage = () => import('@/pages/MyColumnsPage.vue')
const ContestListPage = () => import('@/pages/ContestListPage.vue')
const ContestDetailPage = () => import('@/pages/ContestDetailPage.vue')
const CreatorCertificationPage = () => import('@/pages/CreatorCertificationPage.vue')
const LearnCenterPage = () => import('@/pages/learn/LearnCenterPage.vue')
const StudyPlanPage = () => import('@/pages/learn/StudyPlanPage.vue')
const WrongBookPage = () => import('@/pages/learn/WrongBookPage.vue')
const StudyCalendarPage = () => import('@/pages/learn/StudyCalendarPage.vue')
const KnowledgeGraphPage = () => import('@/pages/learn/KnowledgeGraphPage.vue')
const LeaderboardPage = () => import('@/pages/learn/LeaderboardPage.vue')
const PracticeChoiceListPage = () => import('@/pages/learn/PracticeChoiceListPage.vue')
const PracticeCodingListPage = () => import('@/pages/learn/PracticeCodingListPage.vue')
const PracticeCenterPage = () => import('@/pages/learn/PracticeCenterPage.vue')
const ChoicePracticePage = () => import('@/pages/learn/ChoicePracticePage.vue')
const CodingPracticePage = () => import('@/pages/learn/CodingPracticePage.vue')
const CodeRunnerPage = () => import('@/pages/tools/CodeRunnerPage.vue')
const VoiceEngineDemoPage = () => import('@/pages/interview/VoiceEngineDemoPage.vue')
const VoiceInterviewPage = () => import('@/pages/interview/VoiceInterviewPage.vue')
const SharedReportPage = () => import('@/pages/interview/SharedReportPage.vue')
const PayCashierPage = () => import('@/pages/pay/PayCashierPage.vue')
const WalletPage = () => import('@/pages/pay/WalletPage.vue')
const MyVoiceInterviewsPage = () => import('@/pages/interview/MyVoiceInterviewsPage.vue')

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
    path: '/reading/book/:id',
    name: 'book-detail',
    component: BookDetailPage,
    meta: { title: '书籍详情', isPublic: true }
  },
  {
    path: '/reading/book/:bookId/chapter/:chapterId',
    name: 'chapter-reader',
    component: ChapterReaderPage,
    meta: { title: '章节阅读', isPublic: true }
  },
  {
    path: '/reading/book-list/:id',
    name: 'book-list-detail',
    component: BookListDetailPage,
    meta: { title: '书单详情', isPublic: true }
  },
  {
    path: '/reading/bookshelf',
    name: 'my-bookshelf',
    component: MyBookshelfPage,
    meta: { requiresAuth: true, title: '我的书架', robots: 'noindex,nofollow' }
  },
  {
    path: '/reading/discover',
    name: 'reading-discover',
    component: DiscoverPage,
    meta: { title: '发现好书', isPublic: true }
  },
  {
    path: '/reading/quotes',
    name: 'reading-quotes',
    component: QuoteListPage,
    meta: { title: '金句摘录', isPublic: true }
  },
  {
    path: '/growth/timeline',
    name: 'growth-timeline',
    component: GrowthTimelinePage,
    meta: { title: '成长时间线', requiresAuth: true }
  },
  {
    path: '/interview',
    name: 'interview',
    component: InterviewPage,
    meta: { title: '面试指南', isPublic: true }
  },
  // ============ 学习中心（阶段三 3.1 / 3.2 / 3.3） ============
  {
    path: '/learn',
    name: 'learn-center',
    component: LearnCenterPage,
    meta: { title: '学习中心', isPublic: true }
  },
  {
    path: '/learn/plan',
    name: 'learn-plan',
    component: StudyPlanPage,
    meta: { requiresAuth: true, title: '学习计划', robots: 'noindex,nofollow' }
  },
  {
    path: '/learn/wrong',
    name: 'learn-wrong',
    component: WrongBookPage,
    meta: { requiresAuth: true, title: '错题本', robots: 'noindex,nofollow' }
  },
  // ============ 学习统计（阶段三 3.4 / 3.5 / 3.7） ============
  {
    path: '/learn/calendar',
    name: 'learn-calendar',
    component: StudyCalendarPage,
    meta: { requiresAuth: true, title: '刷题日历', robots: 'noindex,nofollow' }
  },
  {
    path: '/learn/knowledge',
    name: 'learn-knowledge',
    component: KnowledgeGraphPage,
    meta: { title: '知识图谱', isPublic: true }
  },
  {
    path: '/learn/leaderboard',
    name: 'learn-leaderboard',
    component: LeaderboardPage,
    meta: { title: '刷题排行榜', isPublic: true }
  },
  // ============ 刷题中心：选择题 / 编程题在线练习 ============
  {
    path: '/learn/practice',
    name: 'learn-practice',
    component: PracticeCenterPage,
    meta: { title: '刷题中心', isPublic: true }
  },
  {
    path: '/learn/practice/choice',
    name: 'learn-practice-choice',
    component: PracticeChoiceListPage,
    meta: { title: '选择题练习', isPublic: true }
  },
  {
    path: '/learn/practice/choice/:id',
    name: 'learn-practice-choice-do',
    component: ChoicePracticePage,
    meta: { title: '选择题做题', isPublic: true }
  },
  {
    path: '/learn/practice/coding',
    name: 'learn-practice-coding',
    component: PracticeCodingListPage,
    meta: { title: '编程题练习', isPublic: true }
  },
  {
    path: '/learn/practice/coding/:id',
    name: 'learn-practice-coding-do',
    component: CodingPracticePage,
    meta: { title: '编程题做题', isPublic: true }
  },
  {
    path: '/interview/questions',
    redirect: '/learn/questions'
  },
  // 学习中心「面试题库」主路由（与 portal_category.nav_route_path 对齐；v11.13 归属学习中心）
  {
    path: '/learn/questions',
    name: 'interview-questions',
    component: QuestionListPage,
    meta: { title: '题目列表', isPublic: true }
  },
  {
    path: '/interview/experiences',
    name: 'interview-experiences',
    component: ExperienceListPage,
    meta: { title: '面经列表', isPublic: true }
  },
  {
    path: '/interview/question/:id',
    name: 'interview-question',
    component: QuestionDetailPage,
    meta: { title: '题目详情', isPublic: true }
  },
  {
    path: '/interview/experience/publish',
    name: 'interview-experience-publish',
    component: () => import('@/pages/interview/ExperiencePublishPage.vue'),
    meta: { title: '发布面经', requiresAuth: true }
  },
  {
    path: '/interview/experience/edit/:id',
    name: 'interview-experience-edit',
    component: () => import('@/pages/interview/ExperiencePublishPage.vue'),
    meta: { title: '编辑面经', requiresAuth: true }
  },
  {
    path: '/interview/experience/:id',
    name: 'interview-experience',
    component: ExperienceDetailPage,
    meta: { title: '面经详情', isPublic: true }
  },
  {
    path: '/interview/my/attempts',
    name: 'interview-my-attempts',
    component: () => import('@/pages/interview/MyAttemptsPage.vue'),
    meta: { title: '我的答题', requiresAuth: true }
  },
  {
    path: '/interview/my/bookmarks',
    name: 'interview-my-bookmarks',
    component: () => import('@/pages/interview/MyBookmarksPage.vue'),
    meta: { title: '我的收藏题目', requiresAuth: true }
  },
  {
    path: '/interview/my/experiences',
    name: 'interview-my-experiences',
    component: () => import('@/pages/interview/MyExperiencesPage.vue'),
    meta: { title: '我的面经', requiresAuth: true }
  },
  {
    path: '/interview/resume-templates',
    name: 'interview-resume-templates',
    component: ResumeTemplatePage,
    meta: { title: '简历模板', isPublic: true }
  },
  {
    path: '/interview/my/resumes',
    name: 'interview-my-resumes',
    component: MyResumesPage,
    meta: { title: '我的简历', requiresAuth: true, robots: 'noindex,nofollow' }
  },
  {
    path: '/interview/resume/edit',
    name: 'interview-resume-editor-create',
    component: ResumeEditPage,
    meta: { title: '创建简历', requiresAuth: true, robots: 'noindex,nofollow' }
  },
  {
    path: '/interview/resume/optimize',
    name: 'interview-resume-optimize',
    component: ResumeOptimizePage,
    meta: { title: 'AI 简历优化工作台', requiresAuth: true, robots: 'noindex,nofollow' }
  },
  {
    path: '/interview/resume/edit/:id',
    name: 'interview-resume-editor-edit',
    component: ResumeEditPage,
    meta: { title: '编辑简历', requiresAuth: true, robots: 'noindex,nofollow' }
  },
  // ============ 公司主页（阶段三 3.8） ============
  {
    path: '/interview/company/:id',
    name: 'interview-company',
    component: CompanyPage,
    meta: { title: '公司主页', isPublic: true }
  },
  // ============ 语音引擎验证页（V10.0 临时验证） ============
  {
    path: '/interview/voice-demo',
    name: 'interview-voice-demo',
    component: VoiceEngineDemoPage,
    meta: { requiresAuth: true, title: '语音引擎验证', robots: 'noindex,nofollow' }
  },
  // ============ 语音面试官（V10.1 MVP） ============
  {
    path: '/interview/voice',
    name: 'interview-voice',
    component: VoiceInterviewPage,
    meta: { requiresAuth: true, title: 'AI 语音面试官', robots: 'noindex,nofollow' }
  },
  // ============ 面试报告分享页（v11.30.5，免登录公开） ============
  {
    path: '/interview/share/:token',
    name: 'interview-share',
    component: SharedReportPage,
    meta: { requiresAuth: false, title: '面试报告分享', robots: 'noindex,nofollow' }
  },
  // ============ 我的面试记录（V10.3） ============
  {
    path: '/interview/voice/history',
    name: 'interview-voice-history',
    component: MyVoiceInterviewsPage,
    meta: { requiresAuth: true, title: '我的面试记录', robots: 'noindex,nofollow' }
  },
  // ============ 在线代码运行（阶段三 3.6） ============
  {
    path: '/tools/code',
    name: 'tools-code',
    component: CodeRunnerPage,
    meta: { requiresAuth: true, title: '在线代码运行', robots: 'noindex,nofollow' }
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
  // ============ Feed 流 / 专栏 ============
  {
    path: '/feed',
    name: 'feed',
    component: FeedPage,
    meta: { title: '动态广场', isPublic: true }
  },
  // ============ 话题模块 ============
  // 注意：静态/带参路由必须在 /topic/:id 动态路由之前定义，否则 create/edit/my 会被当作 :id
  {
    path: '/topics',
    name: 'topic-list',
    component: () => import('@/pages/TopicListPage.vue'),
    meta: { title: '话题广场', isPublic: true }
  },
  {
    path: '/topic/create',
    name: 'topic-create',
    component: () => import('@/pages/TopicCreatePage.vue'),
    meta: { title: '发起话题', requiresAuth: true }
  },
  {
    path: '/topic/edit/:id',
    name: 'topic-edit',
    component: () => import('@/pages/TopicEditPage.vue'),
    meta: { title: '编辑话题', requiresAuth: true }
  },
  {
    path: '/topic/my/topics',
    name: 'my-topics',
    component: () => import('@/pages/MyTopicsPage.vue'),
    meta: { title: '我的话题', requiresAuth: true }
  },
  {
    path: '/topic/my/posts',
    name: 'my-topic-posts',
    component: () => import('@/pages/MyTopicPostsPage.vue'),
    meta: { title: '我的观点', requiresAuth: true }
  },
  {
    path: '/topic/:id',
    name: 'topic-detail',
    component: () => import('@/pages/TopicDetailPage.vue'),
    meta: { title: '话题详情', isPublic: true }
  },
  {
    path: '/columns',
    name: 'columns',
    component: ColumnsPage,
    meta: { title: '专栏广场', isPublic: true }
  },
  // 注意：静态/带参路由必须在 /column/:id 动态路由之前定义，否则 create/edit/my 会被当作 :id
  {
    path: '/column/create',
    name: 'column-create',
    component: ColumnEditPage,
    meta: { requiresAuth: true, title: '创建专栏', robots: 'noindex,nofollow' }
  },
  {
    path: '/column/edit/:id',
    name: 'column-edit',
    component: ColumnEditPage,
    meta: { requiresAuth: true, title: '编辑专栏', robots: 'noindex,nofollow' }
  },
  {
    path: '/column/my',
    name: 'column-my',
    component: MyColumnsPage,
    meta: { requiresAuth: true, title: '我的专栏', robots: 'noindex,nofollow' }
  },
  // ============ 创作者中心（已合并到个人中心 dashboard Tab） ============
  {
    path: '/creator',
    redirect: { path: '/user', query: { tab: 'dashboard' } }
  },
  // ============ 创作者认证 ============
  {
    path: '/creator/certification',
    name: 'creator-certification',
    component: CreatorCertificationPage,
    meta: { requiresAuth: true, title: '创作者认证', robots: 'noindex,nofollow' }
  },
  {
    path: '/column/:id',
    name: 'column-detail',
    component: ColumnDetailPage,
    meta: { title: '专栏详情', isPublic: true }
  },
  // ============ 创作挑战 / 征文活动 ============
  {
    path: '/contests',
    name: 'contests',
    component: ContestListPage,
    meta: { title: '创作挑战', isPublic: true }
  },
  {
    path: '/contest/:id',
    name: 'contest-detail',
    component: ContestDetailPage,
    meta: { title: '活动详情', isPublic: true }
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
    path: '/user/:id/following',
    name: 'user-following',
    component: FollowListPage,
    meta: { title: '关注列表', isPublic: true, robots: 'noindex,follow' }
  },
  {
    path: '/user/:id/followers',
    name: 'user-followers',
    component: FollowListPage,
    meta: { title: '粉丝列表', isPublic: true, robots: 'noindex,follow' }
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
    // 公告已合并到消息中心（/messages?tab=announcement），保留旧路径重定向兼容外链
    path: '/announcements',
    redirect: { path: '/messages', query: { tab: 'announcement' } }
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
  // 我的举报/反馈进度列表（需登录，仅本人可见）
  {
    path: '/my/reports',
    name: 'my-reports',
    component: MyReportsPage,
    meta: { requiresAuth: true, title: '我的举报', robots: 'noindex,nofollow' }
  },
  {
    path: '/my/feedback',
    name: 'my-feedback',
    component: MyFeedbackPage,
    meta: { requiresAuth: true, title: '我的反馈', robots: 'noindex,nofollow' }
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
  {
    path: '/forgot-password',
    name: 'forgot-password',
    component: ForgotPasswordPage,
    meta: { title: '找回密码', isPublic: true, robots: 'noindex,follow' }
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
  {
    path: '/my/articles',
    name: 'my-articles',
    component: MyArticlesPage,
    meta: { requiresAuth: true, title: '我的文章', robots: 'noindex,nofollow' }
  },
  {
    // 消息中心：游客可查看公告 Tab，通知/私信 Tab 需登录（页面内 onMounted 按需加载）
    path: '/messages',
    name: 'messages',
    component: MessagesPage,
    meta: { title: '消息中心', isPublic: true, robots: 'noindex,nofollow' }
  },
  {
    path: '/messages/chat/:sessionId',
    name: 'messages-chat',
    component: MessagesPage,
    meta: { requiresAuth: true, title: '私信', robots: 'noindex,nofollow' }
  },
  // ============ V11.0 支付中心 ============
  {
    path: '/pay/cashier',
    name: 'pay-cashier',
    component: PayCashierPage,
    meta: { requiresAuth: true, title: '收银台', robots: 'noindex,nofollow' }
  },
  {
    path: '/pay/wallet',
    name: 'pay-wallet',
    component: WalletPage,
    meta: { requiresAuth: true, title: '我的钱包', robots: 'noindex,nofollow' }
  },
  // V11.3：支付通知整合进 /messages 消息中心（?tab=pay），独立页面已移除
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
    document.title = `${to.meta.title} - 旭林知行`
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

// ============ v10.23：AI 慢请求离开确认 ============
// 仅拦截页面间跳转（首次进入 from.name 为空不拦）；
// AI 慢请求（附件解析上传、字段辅助、草稿/匹配/深度优化同步接口、语音面试 LLM 调用等）
// 进行中时离开将中断当前生成，需用户确认。
router.beforeEach((_to, from) => {
  if (from.name && hasPendingAiRequests()) {
    const leave = window.confirm('AI 任务正在进行中，离开将中断当前生成，确定离开吗？')
    if (!leave) return false
  }
  return true
})

export default router
