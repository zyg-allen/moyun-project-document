import auth from '@/plugins/auth'
import router, { constantRoutes, dynamicRoutes } from '@/router'
import { getRouters } from '@/api/menu'
import Layout from '@/layout/index'
import ParentView from '@/components/ParentView'
import InnerLink from '@/layout/components/InnerLink'

// 匹配views里面所有的.vue文件
const modules = import.meta.glob('./../../views/**/*.vue')

const usePermissionStore = defineStore(
    'permission',
    {
      state: () => ({
        routes: [],
        addRoutes: [],
        defaultRoutes: [],
        topbarRouters: [],
        sidebarRouters: []
      }),
      actions: {
        setRoutes(routes) {
          this.addRoutes = routes
          this.routes = constantRoutes.concat(routes)
        },
        setDefaultRoutes(routes) {
          this.defaultRoutes = constantRoutes.concat(routes)
        },
        setTopbarRoutes(routes) {
          this.topbarRouters = routes
        },
        setSidebarRouters(routes) {
          this.sidebarRouters = routes
        },
        generateRoutes(roles) {
          return new Promise(resolve => {
            // 向后端请求路由数据
            getRouters().then(res => {
              const sdata = JSON.parse(JSON.stringify(res.data))
              const rdata = JSON.parse(JSON.stringify(res.data))
              const defaultData = JSON.parse(JSON.stringify(res.data))
              const sidebarRoutes = filterAsyncRouter(sdata)
              const rewriteRoutes = filterAsyncRouter(rdata, false, true)
              const defaultRoutes = filterAsyncRouter(defaultData)
              const asyncRoutes = filterDynamicRoutes(dynamicRoutes)
              asyncRoutes.forEach(route => { router.addRoute(route) })
              // vue-router 4.x 要求所有路由 path 以 "/" 开头
              // RuoYi 后端返回的菜单 path 不带 "/"（如 monitor、job），需统一规范化
              normalizeTopLevelPath(rewriteRoutes)
              normalizeTopLevelPath(sidebarRoutes)
              normalizeTopLevelPath(defaultRoutes)
              this.setRoutes(rewriteRoutes)
              this.setSidebarRouters(constantRoutes.concat(sidebarRoutes))
              this.setDefaultRoutes(sidebarRoutes)
              this.setTopbarRoutes(defaultRoutes)
              resolve(rewriteRoutes)
            })
          })
        }
      }
    })

// vue-router 4.x 要求所有路由 path 以 "/" 开头
// RuoYi 后端返回的菜单 path 不带 "/"（如 monitor、job），filterChildren 展平后需要规范化
function normalizeTopLevelPath(routes) {
  routes.forEach(route => {
    if (route.path && !route.path.startsWith('/') && !isHttp(route.path)) {
      route.path = '/' + route.path
    }
    if (route.children && route.children.length) {
      normalizeTopLevelPath(route.children)
    }
  })
}

// 遍历后台传来的路由字符串，转换为组件对象
function filterAsyncRouter(asyncRouterMap, lastRouter = false, type = false) {
  return asyncRouterMap.filter(route => {
    if (type && route.children) {
      route.children = filterChildren(route.children, route)
    }
    if (route.component) {
      if (route.component === 'Layout') {
        route.component = Layout
      } else if (route.component === 'ParentView') {
        route.component = ParentView
      } else if (route.component === 'InnerLink') {
        route.component = InnerLink
      } else {
        route.component = loadView(route.component)
      }
    }
    // 不再在此处拼接父子 path：
    // - type=true 时 filterChildren 已完成拼接
    // - type=false 时 sidebar 的 resolvePath(basePath + '/' + routePath) 负责拼接
    // - normalizeTopLevelPath 统一补全 "/" 前缀
    if (route.children != null && route.children && route.children.length) {
      route.children = filterAsyncRouter(route.children, false, type)
    } else {
      delete route['children']
      delete route['redirect']
    }
    return true
  })
}

function filterChildren(childrenMap, lastRouter = false) {
  var children = []
  childrenMap.forEach((el, index) => {
    if (el.children && el.children.length) {
      if (el.component === 'ParentView' && !lastRouter) {
        el.children.forEach(c => {
          c.path = el.path + '/' + c.path
          if (c.children && c.children.length) {
            children = children.concat(filterChildren(c.children, c))
            return
          }
          children.push(c)
        })
        return
      }
    }
    if (lastRouter) {
      el.path = lastRouter.path + '/' + el.path
      if (el.children && el.children.length) {
        children = children.concat(filterChildren(el.children, el))
        return
      }
    }
    children = children.concat(el)
  })
  return children
}

// 动态路由遍历，验证是否具备权限
export function filterDynamicRoutes(routes) {
  const res = []
  routes.forEach(route => {
    if (route.permissions) {
      if (auth.hasPermiOr(route.permissions)) {
        res.push(route)
      }
    } else if (route.roles) {
      if (auth.hasRoleOr(route.roles)) {
        res.push(route)
      }
    }
  })
  return res
}

export const loadView = (view) => {
  let res;
  for (const path in modules) {
    const dir = path.split('views/')[1].split('.vue')[0];
    if (dir === view) {
      res = () => modules[path]();
    }
  }
  return res;
}

function isHttp(path) {
  return /^(https?:\/\/)/.test(path)
}

export default usePermissionStore
