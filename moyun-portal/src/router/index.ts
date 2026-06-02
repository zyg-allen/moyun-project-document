import { createRouter, createWebHistory } from 'vue-router'

const HomePage = () => import('@/pages/HomePage.vue')
const ArticleDetailPage = () => import('@/pages/ArticleDetailPage.vue')
const SearchPage = () => import('@/pages/SearchPage.vue')
const ListPage = () => import('@/pages/ListPage.vue')
const UserPage = () => import('@/pages/UserPage.vue')
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

// 定义路由配置
const routes = [
  {
    path: '/',
    name: 'home',
    component: HomePage,
  },
  {
    path: '/reading',
    name: 'reading',
    component: ReadingPage,
  },
  {
    path: '/interview',
    name: 'interview',
    component: InterviewPage,
  },
  {
    path: '/article/:id',
    name: 'article',
    component: ArticleDetailPage,
  },
  {
    path: '/search',
    name: 'search',
    component: SearchPage,
  },
  {
    path: '/list',
    name: 'list',
    component: ListPage,
  },
  {
    path: '/login',
    name: 'login',
    component: LoginPage,
  },
  {
    path: '/register',
    name: 'register',
    component: RegisterPage,
  },
  {
    path: '/user',
    name: 'user',
    component: UserPage,
  },
  {
    path: '/author/:id',
    name: 'author',
    component: AuthorPage,
  },
  {
    path: '/authors',
    name: 'authors',
    component: AuthorsPage,
  },
  {
    path: '/help',
    name: 'help',
    component: HelpCenterPage,
  },
  {
    path: '/about',
    name: 'about',
    component: AboutUsPage,
  },
  {
    path: '/agreement',
    name: 'agreement',
    component: UserAgreementPage,
  },
  {
    path: '/report',
    name: 'report',
    component: ReportFeedbackPage,
  },
  {
    path: '/publish',
    name: 'publish',
    component: PublishPage,
  },
  {
    path: '/:pathMatch(.*)*',
    name: 'not-found',
    component: NotFoundPage,
  },
]

// 创建路由实例
const router = createRouter({
  history: createWebHistory(),
  routes,
})

export default router
