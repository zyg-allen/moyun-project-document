/**
 * 墨韵智库后台管理系统 - 社区模块路由配置
 */

// 社区管理模块路由配置（示例）
// 这些路由可以在实际的后台管理系统中通过数据库菜单配置或动态路由加载

export const communityRoutes = [
  {
    path: '/community',
    component: 'Layout',
    redirect: '/community/article',
    name: 'Community',
    meta: {
      title: '社区管理',
      icon: 'community',
      roles: ['admin', 'editor']
    },
    children: [
      {
        path: 'article',
        component: () => import('@/views/community/article/index'),
        name: 'CommunityArticle',
        meta: {
          title: '文章管理',
          icon: 'article',
          permissions: ['content:article:list', 'content:article:query']
        }
      },
      {
        path: 'audit',
        component: () => import('@/views/community/audit/index'),
        name: 'CommunityAudit',
        meta: {
          title: '审核管理',
          icon: 'audit',
          permissions: ['content:article:audit']
        }
      },
      {
        path: 'category',
        component: () => import('@/views/community/category/index'),
        name: 'CommunityCategory',
        meta: {
          title: '分类管理',
          icon: 'category',
          permissions: ['content:category:list', 'content:category:query']
        }
      },
      {
        path: 'comment',
        component: () => import('@/views/community/comment/index'),
        name: 'CommunityComment',
        meta: {
          title: '评论管理',
          icon: 'comment',
          permissions: ['content:comment:list', 'content:comment:query']
        }
      },
      {
        path: 'tag',
        component: () => import('@/views/community/tag/index'),
        name: 'CommunityTag',
        meta: {
          title: '标签管理',
          icon: 'tag',
          permissions: ['content:tag:list', 'content:tag:query']
        }
      },
      {
        path: 'user',
        component: () => import('@/views/community/user/index'),
        name: 'CommunityUser',
        meta: {
          title: '用户管理',
          icon: 'user',
          permissions: ['community:user:list', 'community:user:query']
        }
      },
      {
        path: 'friendlink',
        component: () => import('@/views/community/friendlink/index'),
        name: 'CommunityFriendlink',
        meta: {
          title: '友情链接',
          icon: 'link',
          permissions: ['content:friendlink:list', 'content:friendlink:query']
        }
      },
      {
        path: 'growth',
        component: () => import('@/views/community/growth/index'),
        name: 'CommunityGrowth',
        meta: {
          title: '成长体系',
          icon: 'growth',
          permissions: ['community:growth:list', 'community:growth:query']
        }
      },
      {
        path: 'vip',
        component: () => import('@/views/community/vip/index'),
        name: 'CommunityVip',
        meta: {
          title: 'VIP管理',
          icon: 'vip',
          permissions: ['community:vip:list', 'community:vip:query']
        }
      },
      {
        path: 'wallet',
        component: () => import('@/views/community/wallet/index'),
        name: 'CommunityWallet',
        meta: {
          title: '钱包管理',
          icon: 'wallet',
          permissions: ['community:wallet:list', 'community:wallet:query']
        }
      }
    ]
  }
]

// 菜单数据（用于数据库初始化参考）
export const communityMenus = [
  {
    menuName: '社区管理',
    icon: 'community',
    orderNum: 5,
    perms: '',
    menuType: 'M',
    visible: '0',
    status: '0',
    children: [
      {
        menuName: '文章管理',
        icon: 'article',
        orderNum: 1,
        perms: 'content:article:list',
        menuType: 'C',
        visible: '0',
        status: '0'
      },
      {
        menuName: '审核管理',
        icon: 'audit',
        orderNum: 2,
        perms: 'content:article:audit',
        menuType: 'C',
        visible: '0',
        status: '0'
      },
      {
        menuName: '分类管理',
        icon: 'category',
        orderNum: 3,
        perms: 'content:category:list',
        menuType: 'C',
        visible: '0',
        status: '0'
      },
      {
        menuName: '评论管理',
        icon: 'comment',
        orderNum: 4,
        perms: 'content:comment:list',
        menuType: 'C',
        visible: '0',
        status: '0'
      },
      {
        menuName: '标签管理',
        icon: 'tag',
        orderNum: 5,
        perms: 'content:tag:list',
        menuType: 'C',
        visible: '0',
        status: '0'
      },
      {
        menuName: '用户管理',
        icon: 'user',
        orderNum: 6,
        perms: 'community:user:list',
        menuType: 'C',
        visible: '0',
        status: '0'
      },
      {
        menuName: '友情链接',
        icon: 'link',
        orderNum: 7,
        perms: 'content:friendlink:list',
        menuType: 'C',
        visible: '0',
        status: '0'
      },
      {
        menuName: '成长体系',
        icon: 'growth',
        orderNum: 8,
        perms: 'community:growth:list',
        menuType: 'C',
        visible: '0',
        status: '0'
      },
      {
        menuName: 'VIP管理',
        icon: 'vip',
        orderNum: 9,
        perms: 'community:vip:list',
        menuType: 'C',
        visible: '0',
        status: '0'
      },
      {
        menuName: '钱包管理',
        icon: 'wallet',
        orderNum: 10,
        perms: 'community:wallet:list',
        menuType: 'C',
        visible: '0',
        status: '0'
      }
    ]
  }
]
