import { httpGet } from './client';
import type {
  Category,
  CategoryListParams,
} from '@/types/api';

// 后台响应类型（与client.ts保持一致）
interface BackendResponse<T = any> {
  code: number;
  msg: string;
  data?: T;
}

// 分类数据缓存
let categoryTreeCache: Category[] | null = null;
let categoryTreeLoading: Promise<Category[] | null> | null = null;

/**
 * 判断分类是否应该展示在首页文章分类卡片（统一过滤逻辑）
 *
 * 过滤条件（优先使用 categoryType 字段，降级兼容历史数据）：
 * 1. status 字段：0 展示  1 隐藏（后端维护）
 * 2. category_type 字段：article=文章栏目（展示） special=特殊页面（隐藏，如首页/外链等）
 *    —— 历史数据已由升级脚本回填，若字段缺失则降级到名称/路由类型兜底逻辑
 * 降级兜底（仅当 categoryType 为空时生效）：
 *    - 名称黑名单：排除首页、读书空间等非文章一级栏目
 *    - nav_route_type：只保留 category 类型，过滤 static/home/external 占位项
 *
 * 注意：Navbar 已改为读 /portal/category/nav/tree 动态渲染，不受此函数影响。
 *       本函数仅用于 HomePage / PublishPage 等读 /public/tree 的场景。
 *
 * @param cat 分类对象
 * @returns true=展示 false=隐藏
 */
export function shouldShowCategory(cat: Category | null | undefined): boolean {
  if (!cat) return false;

  // 条件1：status 字段控制 - 0 展示 1 隐藏（同时兼容字符串和数字类型）
  const status = String(cat.status ?? '0');
  if (status === '1') return false;

  const name = (cat.name || '').trim();
  if (!name) return false;

  // 条件2：优先使用 category_type 字段（新增字段，标识文章栏目 vs 特殊页面）
  const categoryType = (cat.categoryType || '').toLowerCase();
  if (categoryType) {
    // 字段已回填：article 展示，其余（special）隐藏
    return categoryType === 'article';
  }

  // 降级兜底：categoryType 缺失时（历史脏数据），沿用名称黑名单 + 路由类型过滤
  const blockedNames = ['首页', '读书空间', '面试指南', '社区互动', '创作者中心', '个人空间'];
  if (blockedNames.includes(name)) return false;

  const routeType = (cat.navRouteType || 'category').toLowerCase();
  if (routeType !== 'category') return false;

  return true;
}

/**
 * 过滤分类树（同时过滤父节点和子节点）
 * @param categories 原始分类数组
 * @returns 过滤后的分类数组
 */
export function filterCategoryTree(categories: Category[] | null | undefined): Category[] {
  if (!categories || !Array.isArray(categories)) return [];
  return categories
      .filter(shouldShowCategory)
      .map(cat => ({
        ...cat,
        children: cat.children && cat.children.length > 0
            ? filterCategoryTree(cat.children)
            : []
      }));
}

/**
 * 根据分类计算跳转目标（首页分类卡片使用）
 *
 * 兼容两套字段：
 *   - 新字段：nav_route_type + nav_route_path（推荐，与 Navbar 一致）
 *   - 旧字段：linkType + externalUrl（兼容历史数据）
 *
 * 由于 shouldShowCategory 已过滤掉非 category 类型，
 * 这里正常只会命中 category 分支（拼 /category/<name>）。
 * 其他分支作为兜底，防止脏数据导致跳转异常。
 *
 * @param cat 分类对象
 * @returns { type: 'internal' | 'external', path: string }
 */
export function getCategoryTarget(cat: Category): { type: 'internal' | 'external'; path: string } {
  // 优先使用新字段 nav_route_type
  const routeType = (cat.navRouteType || '').toLowerCase();
  if (routeType) {
    return getNavRouteTarget(cat);
  }
  // 兜底：兼容旧字段 linkType + externalUrl
  const linkType = String(cat.linkType ?? '0');
  if (linkType === '1' && cat.externalUrl) {
    return { type: 'external', path: cat.externalUrl };
  }
  // 默认：本地路由跳转到栏目
  return { type: 'internal', path: `/category/${encodeURIComponent(cat.name)}` };
}

// 获取树形分类（带缓存）
export const getCategoryTree = async (params?: CategoryListParams): Promise<BackendResponse<Category[]>> => {
  // 如果已有缓存，直接返回
  if (categoryTreeCache) {
    return { code: 200, msg: '操作成功', data: categoryTreeCache };
  }

  // 如果正在加载，返回同一个Promise
  if (categoryTreeLoading) {
    const data = await categoryTreeLoading;
    return { code: 200, msg: '操作成功', data };
  }

  // 开始加载
  categoryTreeLoading = new Promise(async (resolve) => {
    try {
      const response = await httpGet<Category[]>('/portal/category/public/tree', params);
      if (response.code === 200) {
        categoryTreeCache = response.data || [];
        resolve(categoryTreeCache);
      } else {
        resolve(null);
      }
    } catch (error) {
      console.error('加载分类树失败:', error);
      resolve(null);
    } finally {
      categoryTreeLoading = null;
    }
  });

  const data = await categoryTreeLoading;
  return { code: 200, msg: '操作成功', data };
};

// 清除缓存（用于刷新场景）
export const clearCategoryTreeCache = () => {
  categoryTreeCache = null;
  navTreeCache = null;
};

// ============ Navbar 导航栏目树 ============

// Navbar 导航树缓存（独立于分类树缓存，避免互相干扰）
let navTreeCache: Category[] | null = null;
let navTreeLoading: Promise<Category[] | null> | null = null;

/**
 * 获取头部导航栏目树（仅 show_in_nav=1 且 status=0 的分类）
 * 供 Navbar 动态渲染使用，返回的是后端 /portal/category/nav/tree 的数据。
 */
export const getNavTree = async (): Promise<BackendResponse<Category[]>> => {
  if (navTreeCache) {
    return { code: 200, msg: '操作成功', data: navTreeCache };
  }
  if (navTreeLoading) {
    const data = await navTreeLoading;
    return { code: 200, msg: '操作成功', data };
  }
  navTreeLoading = new Promise(async (resolve) => {
    try {
      const response = await httpGet<Category[]>('/portal/category/nav/tree');
      if (response.code === 200) {
        navTreeCache = response.data || [];
        resolve(navTreeCache);
      } else {
        resolve(null);
      }
    } catch (error) {
      console.error('加载导航栏目树失败:', error);
      resolve(null);
    } finally {
      navTreeLoading = null;
    }
  });
  const data = await navTreeLoading;
  return { code: 200, msg: '操作成功', data };
};

/**
 * 根据 nav_route_type 计算导航项的最终跳转目标。
 *
 * 路由类型说明：
 *   - home      : 首页，固定跳 /
 *   - category  : 动态分类栏目，拼装为 /category/<encodeURIComponent(name)>
 *   - static    : 静态路由，path = nav_route_path
 *   - external  : 外部链接，path = nav_route_path，新窗口打开
 *
 * @param cat 分类对象
 * @returns { type: 'internal' | 'external', path: string }
 */
export function getNavRouteTarget(cat: Category): { type: 'internal' | 'external'; path: string } {
  const routeType = (cat.navRouteType || 'category').toLowerCase();
  switch (routeType) {
    case 'home':
      return { type: 'internal', path: '/' };
    case 'static':
      return { type: 'internal', path: cat.navRoutePath || '/' };
    case 'external':
      return { type: 'external', path: cat.navRoutePath || '#' };
    case 'category':
    default:
      // 动态分类：拼装为 /category/<encodeURIComponent(name)>
      return { type: 'internal', path: `/category/${encodeURIComponent(cat.name)}` };
  }
}

/**
 * 判断导航项是否需要登录才能访问。
 */
export function isNavRequiresAuth(cat: Category): boolean {
  return Number(cat.requiresAuth ?? 0) === 1;
}
