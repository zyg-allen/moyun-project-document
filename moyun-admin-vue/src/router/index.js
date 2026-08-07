import { createWebHistory, createRouter } from 'vue-router'
/* Layout */
import Layout from '@/layout'

/**
 * Note: 路由配置项
 *
 * hidden: true                     // 当设置 true 的时候该路由不会再侧边栏出现 如401，login等页面，或者如一些编辑页面/edit/1
 * alwaysShow: true                 // 当你一个路由下面的 children 声明的路由大于1个时，自动会变成嵌套的模式--如组件页面
 *                                  // 只有一个时，会将那个子路由当做根路由显示在侧边栏--如引导页面
 *                                  // 若你想不管路由下面的 children 声明的个数都显示你的根路由
 *                                  // 你可以设置 alwaysShow: true，这样它就会忽略之前定义的规则，一直显示根路由
 * redirect: noRedirect             // 当设置 noRedirect 的时候该路由在面包屑导航中不可被点击
 * name:'router-name'               // 设定路由的名字，一定要填写不然使用<keep-alive>时会出现各种问题
 * query: '{"id": 1, "name": "ry"}' // 访问路由的默认传递参数
 * roles: ['admin', 'common']       // 访问路由的角色权限
 * permissions: ['a:a:a', 'b:b:b']  // 访问路由的菜单权限
 * meta : {
    noCache: true                   // 如果设置为true，则不会被 <keep-alive> 缓存(默认 false)
    title: 'title'                  // 设置该路由在侧边栏和面包屑中展示的名字
    icon: 'svg-name'                // 设置该路由的图标，对应路径src/assets/icons/svg
    breadcrumb: false               // 如果设置为false，则不会在breadcrumb面包屑中显示
    activeMenu: '/system/user'      // 当路由设置了该属性，则会高亮相对应的侧边栏。
  }
 */

// 公共路由
export const constantRoutes = [
  {
    path: '/redirect',
    component: Layout,
    hidden: true,
    children: [
      {
        path: '/redirect/:path(.*)',
        component: () => import('@/views/redirect/index.vue')
      }
    ]
  },
  {
    path: '/login',
    component: () => import('@/views/login'),
    hidden: true
  },
  {
    path: '/register',
    component: () => import('@/views/register'),
    hidden: true
  },
  {
    path: "/:pathMatch(.*)*",
    component: () => import('@/views/error/404'),
    hidden: true
  },
  {
    path: '/401',
    component: () => import('@/views/error/401'),
    hidden: true
  },
  {
    path: '',
    component: Layout,
    redirect: '/index',
    children: [
      {
        path: '/index',
        component: () => import('@/views/index'),
        name: 'Index',
        meta: { title: '首页', icon: 'dashboard', affix: true }
      }
    ]
  },
  {
    path: '/user',
    component: Layout,
    hidden: true,
    redirect: 'noredirect',
    children: [
      {
        path: 'profile',
        component: () => import('@/views/system/user/profile/index'),
        name: 'Profile',
        meta: { title: '个人中心', icon: 'user' }
      }
    ]
  },
  // 暂时注释：表单构建器功能未实现
  // {
  //   path: '/tool',
  //   component: Layout,
  //   hidden: true,
  //   children: [
  //     {
  //       path: 'build/index',
  //       component: () => import('@/views/tool/build/index'),
  //       name: 'FormBuild',
  //       meta: { title: '表单配置', icon: '' }
  //     }
  //   ]
  // },
]

// 动态路由，基于用户权限动态去加载
export const dynamicRoutes = [
  {
    path: '/system/user-auth',
    component: Layout,
    hidden: true,
    permissions: ['system:user:edit'],
    children: [
      {
        path: 'role/:userId(\\d+)',
        component: () => import('@/views/system/user/authRole'),
        name: 'AuthRole',
        meta: { title: '分配角色', activeMenu: '/system/user' }
      }
    ]
  },
  {
    path: '/system/role-auth',
    component: Layout,
    hidden: true,
    permissions: ['system:role:edit'],
    children: [
      {
        path: 'user/:roleId(\\d+)',
        component: () => import('@/views/system/role/authUser'),
        name: 'AuthUser',
        meta: { title: '分配用户', activeMenu: '/system/role' }
      }
    ]
  },
  {
    path: '/system/dict-data',
    component: Layout,
    hidden: true,
    permissions: ['system:dict:list'],
    children: [
      {
        path: 'index/:dictId(\\d+)',
        component: () => import('@/views/system/dict/data'),
        name: 'Data',
        meta: { title: '字典数据', activeMenu: '/system/dict' }
      }
    ]
  },
  {
    path: '/monitor/job-log',
    component: Layout,
    hidden: true,
    permissions: ['monitor:job:list'],
    children: [
      {
        path: 'index/:jobId(\\d+)',
        component: () => import('@/views/monitor/job/log'),
        name: 'JobLog',
        meta: { title: '调度日志', activeMenu: '/monitor/job' }
      }
    ]
  },
  {
    path: '/tool/gen-edit',
    component: Layout,
    hidden: true,
    permissions: ['tool:gen:edit'],
    children: [
      {
        path: 'index/:tableId(\\d+)',
        component: () => import('@/views/tool/gen/editTable'),
        name: 'GenEdit',
        meta: { title: '修改生成配置', activeMenu: '/tool/gen' }
      }
    ]
  },
  {
    path: '/cms',
    component: Layout,
    hidden: true,
    permissions: ['cms:article:list'],
    children: [
      {
        path: 'article/edit',
        component: () => import('@/views/cms/article/edit'),
        name: 'ArticleEdit',
        meta: { title: '编辑文章', activeMenu: '/cms/article' }
      },
      {
        path: 'article/edit/:id(\\d+)',
        component: () => import('@/views/cms/article/edit'),
        name: 'ArticleEditWithId',
        meta: { title: '编辑文章', activeMenu: '/cms/article' }
      },
      {
        path: 'article/audit',
        component: () => import('@/views/cms/article/audit'),
        name: 'ArticleAudit',
        meta: { title: '文章审核', activeMenu: '/cms/article' }
      }
    ]
  },
  // 读书空间-章节管理（动态路由，权限由后台菜单控制；此处仅作为常驻页面入口备份）
  {
    path: '/portal',
    component: Layout,
    hidden: true,
    permissions: ['portal:bookChapter:list'],
    children: [
      {
        path: 'bookChapter',
        component: () => import('@/views/portal/bookChapter/index'),
        name: 'BookChapter',
        meta: { title: '章节管理', activeMenu: '/portal/book' }
      },
      {
        path: 'bookChapter/:bookId(\\d+)',
        component: () => import('@/views/portal/bookChapter/index'),
        name: 'BookChapterWithBook',
        meta: { title: '章节管理', activeMenu: '/portal/book' }
      }
    ]
  },
  // 读书空间-书架管理 / 推荐位管理 已整合到「书单&推荐位」和「用户内容」Tab 容器，
  //   不再需要独立隐藏路由（由 sys_menu 动态路由 + Tab 容器嵌入承载）

  // ========== AI 模块隐藏子路由 ==========
  // chat（智能对话）是独立全屏页面，由"开始对话"按钮 window.open 打开，
  // 不使用 Layout 包裹避免双栏嵌套。权限标识用 :list 后缀对齐菜单注册。
  // dashboard（概览大屏）和 diagram（架构图生成）走动态路由（Layout 包裹），
  //   由侧边栏菜单进入，页面 CSS 自身处理高度撑满。
  {
    path: '/ai/chat/index/:agentId?',
    component: () => import('@/views/ai/chat/index'),
    name: 'AiChatView',
    hidden: true,
    permissions: ['cms:ai:chat:list'],
    meta: { title: '智能对话', activeMenu: '/ai/agent' }
  }
]

const router = createRouter({
  history: createWebHistory(),
  routes: constantRoutes,
  scrollBehavior(to, from, savedPosition) {
    if (savedPosition) {
      return savedPosition
    } else {
      return { top: 0 }
    }
  },
});

export default router;
