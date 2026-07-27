import { createRouter, createWebHistory } from 'vue-router'
import ChatWindow from '@/components/ChatWindow.vue'
import KnowledgeLibraryManage from '@/components/KnowledgeLibraryManage.vue'
import AgentManage from '@/components/AgentManage.vue'
import ModelConfigManage from '@/components/ModelConfigManage.vue'
import DomainDictionaryManage from '@/components/DomainDictionaryManage.vue'
import ToolManage from '@/components/ToolManage.vue'
import TokenUsageStats from '@/components/TokenUsageStats.vue'
import LoginPage from '@/components/LoginPage.vue'
import Dashboard from '@/components/Dashboard.vue'
import WorkflowManage from '@/components/WorkflowManage.vue'
import DataSourceManage from '@/components/DataSourceManage.vue'
import IntelligentQuery from '@/components/IntelligentQuery.vue'
import DiagramGenerator from '@/components/DiagramGenerator.vue'
import DiagramChat from '@/components/DiagramChat.vue'
import AppView from '@/views/AppView.vue'

const routes = [
  {
    path: '/login',
    name: 'Login',
    component: LoginPage,
    meta: { title: '登录', requiresAuth: false, hidden: true }
  },
  {
    path: '/',
    name: 'Agent',
    component: AgentManage,
    meta: { title: '智能体管理', icon: 'fa-solid fa-robot', requiresAuth: true }
  },
  {
    path: '/knowledge',
    name: 'Knowledge',
    component: KnowledgeLibraryManage,
    meta: { title: '知识库管理', icon: 'fa-solid fa-book', requiresAuth: true }
  },
  {
    path: '/workflow',
    name: 'Workflow',
    component: WorkflowManage,
    meta: { title: 'AI工作流', icon: 'fa-solid fa-diagram-project', requiresAuth: true }
  },
  {
    path: '/workflow/share/:id',
    name: 'WorkflowShare',
    component: WorkflowManage,
    meta: { title: '工作流分享', requiresAuth: false, hidden: true, isShare: true }
  },
  // {
  //   path: '/diagram',
  //   name: 'DiagramChat',
  //   component: DiagramChat,
  //   meta: { title: 'AI架构图', icon: 'fa-solid fa-sitemap', requiresAuth: true }
  // },
  // {
  //   path: '/diagram-old',
  //   name: 'DiagramGenerator',
  //   component: DiagramGenerator,
  //   meta: { title: 'AI架构图(旧版)', icon: 'fa-solid fa-sitemap', requiresAuth: true, hidden: true }
  // },
  {
    path: '/data-query',
    name: 'IntelligentQuery',
    component: IntelligentQuery,
    meta: { title: 'AI数据分析', icon: 'fa-solid fa-chart-pie', requiresAuth: true }
  },
  {
    path: '/datasource',
    name: 'DataSource',
    component: DataSourceManage,
    meta: { title: '数据源管理', icon: 'fa-solid fa-database', requiresAuth: true }
  },
  {
    path: '/model',
    name: 'ModelConfig',
    component: ModelConfigManage,
    meta: { title: '模型配置', icon: 'fa-solid fa-brain', requiresAuth: true }
  },
  {
    path: '/tool',
    name: 'ToolManage',
    component: ToolManage,
    meta: { title: '工具管理', icon: 'fa-solid fa-wrench', requiresAuth: true }
  },
  {
    path: '/domain-dictionary',
    name: 'DomainDictionary',
    component: DomainDictionaryManage,
    meta: { title: '领域词典', icon: 'fa-solid fa-book-open', requiresAuth: true }
  },
  {
    path: '/token-usage',
    name: 'TokenUsage',
    component: TokenUsageStats,
    meta: { title: 'Token统计', icon: 'fa-solid fa-chart-line', requiresAuth: true }
  },
  {
    path: '/dashboard',
    name: 'Dashboard',
    component: Dashboard,
    meta: { title: '数据大屏', icon: 'fa-solid fa-tv', requiresAuth: true, hideSidebar: true, openInNewTab: true }
  },
  {
    path: '/chat',
    name: 'Chat',
    component: ChatWindow,
    meta: { title: '智能对话', requiresAuth: true, hidden: true, hideSidebar: true }
  },
  {
    path: '/app/:token',
    name: 'App',
    component: AppView,
    meta: { title: '应用', requiresAuth: false, hidden: true, hideSidebar: true }
  }
]

const router = createRouter({
  history: createWebHistory('/lynx-ai/'),
  routes
})

// 路由守卫
router.beforeEach((to, from, next) => {
  // 设置页面标题
  document.title = to.meta.title ? `${to.meta.title} - Lynx AI` : 'Lynx AI'

  // 检查是否需要登录
  if (to.meta.requiresAuth !== false) {
    const token = localStorage.getItem('token')
    if (!token) {
      // 未登录，跳转到登录页
      next({ name: 'Login', query: { redirect: to.fullPath } })
      return
    }
  }

  // 已登录用户访问登录页，跳转到首页
  if (to.name === 'Login') {
    const token = localStorage.getItem('token')
    if (token) {
      next({ name: 'Agent' })
      return
    }
  }

  next()
})

export default router
