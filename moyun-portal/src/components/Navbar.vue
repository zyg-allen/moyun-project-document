<script setup lang="ts">
import { ref, computed, onMounted, onUnmounted, watch } from 'vue';
import { RouterLink as Link, useRouter, useRoute } from 'vue-router';
import {
  Search, Plus, LogOut, Menu, X, Palette, Sun, Moon, Eye,
  ChevronDown, ChevronRight, Settings, UserCircle, BookMarked,
  HelpCircle, Lock, Bell, Flag, MessageSquare, Mic, FileText, Wallet
} from 'lucide-vue-next';
import { setTheme, getCurrentTheme, type Theme, themes } from '@/utils/theme';
import { useUserStore } from '@/stores/user';
import { useMessageStore } from '@/stores/message';
import { getSafeAvatar } from '@/utils/avatar';
import { useAuth } from '@/composables/useAuth';
import { getNavTree, getNavRouteTarget, isNavRequiresAuth } from '@/api/category';
import type { Category } from '@/types/api';
import IconRender from './IconRender.vue';

const router = useRouter();
const route = useRoute();
const userStore = useUserStore();
const messageStore = useMessageStore();
const { requireAuth } = useAuth();

const isMenuOpen = ref(false);
const searchQuery = ref('');
const currentTheme = ref<Theme>(getCurrentTheme());
const isThemeMenuOpen = ref(false);
const activeNavItem = ref<string | null>(null);
// 移动端二级展开状态（key 格式：一级key + '/' + 二级name）
const activeSubNavItem = ref<string | null>(null);
// 消息中心未读总数（通知 + 私信），用于头部单一消息入口徽章
const totalUnread = computed(() => messageStore.totalUnread);
const isUserMenuOpen = ref(false);
// 点击头像后菜单钉住：鼠标移出不关闭，点击外部/再次点击头像才关闭
const isUserMenuPinned = ref(false);

/** 切换用户菜单：已钉住则收起，否则钉住展开 */
function toggleUserMenu() {
  if (isUserMenuOpen.value && isUserMenuPinned.value) {
    isUserMenuPinned.value = false;
    closeUserMenu();
  } else {
    isUserMenuPinned.value = true;
    isUserMenuOpen.value = true;
  }
}

/** 关闭用户菜单（含钉住状态复位） */
function closeUserMenu() {
  isUserMenuPinned.value = false;
  isUserMenuOpen.value = false;
}

// 导航子项（递归类型，支持任意层级）
interface NavSubItem {
  name: string;
  path?: string;
  externalUrl?: string | null;
  isExternal?: boolean;
  requiresAuth?: boolean;
  icon?: string;
  description?: string;
  badge?: string;
  children?: NavSubItem[];
}

// 导航项统一类型：所有属性可选（除 name/key/children），避免联合类型访问报错
interface NavItem {
  name: string;
  key: string;
  path?: string;
  externalUrl?: string | null;
  isExternal?: boolean;
  icon?: string;
  description?: string;
  children: NavSubItem[];
}

// 从后端 /portal/category/nav/tree 加载的原始分类树
const navCategories = ref<Category[]>([]);

/**
 * 递归把后端 Category 转换为前端 NavSubItem（支持任意层级 children）。
 *
 * 子项根据 nav_route_type 计算 path：
 *   - home      : 不应出现在子级，兜底 /
 *   - category  : /category/<encodeURIComponent(name)>
 *   - static    : nav_route_path
 *   - external  : nav_route_path（标记 isExternal，新窗口打开）
 */
function categoryToSubItem(cat: Category): NavSubItem {
  const target = getNavRouteTarget(cat);
  const subChildren = (cat.children || []).map(child => categoryToSubItem(child));
  return {
    name: cat.name,
    path: target.path,
    isExternal: target.type === 'external',
    requiresAuth: isNavRequiresAuth(cat),
    icon: cat.icon,
    description: cat.description,
    badge: cat.navBadge,
    children: subChildren.length > 0 ? subChildren : undefined,
  };
}

/**
 * 把后端 Category 转换为前端 NavItem。
 *
 * 一级栏目根据 nav_route_type 决定行为：
 *   - home      : 直接跳转 /（模板用 key==='home' 特判）
 *   - static    : 一级可点（path=nav_route_path），同时悬浮展开子菜单
 *   - external  : 外部链接
 *   - category  : 仅展开子菜单，无 path
 */
function categoryToNavItem(cat: Category): NavItem {
  const routeType = (cat.navRouteType || 'category').toLowerCase();
  // 递归构建子项（支持多级）
  const children: NavSubItem[] = (cat.children || []).map(child => categoryToSubItem(child));

  if (routeType === 'home') {
    return { name: cat.name, key: cat.slug || String(cat.id), path: '/', icon: cat.icon, description: cat.description, children: [] };
  }
  if (routeType === 'external') {
    return {
      name: cat.name,
      key: cat.slug || String(cat.id),
      externalUrl: cat.navRoutePath || '#',
      isExternal: true,
      icon: cat.icon,
      description: cat.description,
      children: [],
    };
  }
  if (routeType === 'static') {
    // 一级可点 + 展开子菜单
    return {
      name: cat.name,
      key: cat.slug || String(cat.id),
      path: cat.navRoutePath || '/',
      icon: cat.icon,
      description: cat.description,
      children,
    };
  }
  // category 类型：一级仅展开子菜单，无 path
  return { name: cat.name, key: cat.slug || String(cat.id), icon: cat.icon, description: cat.description, children };
}

// 图标彩色色板：根据索引轮换，让每个图标方块有不同彩色背景
const iconColorPalette = [
  'linear-gradient(135deg, #6366f1, #8b5cf6)', // 靛紫
  'linear-gradient(135deg, #ec4899, #f43f5e)', // 粉红
  'linear-gradient(135deg, #f59e0b, #f97316)', // 琥珀橙
  'linear-gradient(135deg, #10b981, #059669)', // 翠绿
  'linear-gradient(135deg, #3b82f6, #06b6d4)', // 海蓝
  'linear-gradient(135deg, #8b5cf6, #d946ef)', // 紫罗兰
  'linear-gradient(135deg, #ef4444, #f97316)', // 红橙
  'linear-gradient(135deg, #14b8a6, #0ea5e9)', // 青碧
];
function getIconColor(idx: number = 0): string {
  return iconColorPalette[idx % iconColorPalette.length];
}

// 导航项（从后端动态加载 + 登录态过滤）
const navItems = computed<NavItem[]>(() => {
  const isLoggedIn = userStore.isAuthenticated;
  const items = navCategories.value
    .map(categoryToNavItem)
    .filter(item => {
      // 未登录用户：过滤掉所有子项都需登录的菜单（避免空菜单）
      if (!isLoggedIn && item.children.length > 0) {
        const hasPublicChild = item.children.some(c => !c.requiresAuth);
        return hasPublicChild;
      }
      return true;
    });

  // V10.4：AI 语音面试官入口已由 portal_category(interview-voice) 数据驱动，不再硬编码注入

  return items;
});

// 加载导航栏目树（带内存缓存，由 category.ts 统一管理）
async function loadNavCategories() {
  try {
    const response = await getNavTree();
    if (response.code === 200 && response.data) {
      navCategories.value = response.data;
    }
  } catch (error) {
    console.error('加载导航栏目失败:', error);
    navCategories.value = [];
  }
}

const currentUser = computed(() => userStore.user)

onMounted(async () => {
  currentTheme.value = getCurrentTheme();
  // 加载头部导航栏目树（所有用户都需要，未登录也能看到公开栏目）
  await loadNavCategories();
  if (userStore.isAuthenticated) {
    await messageStore.loadAllUnread();
  }
});

watch(
  () => userStore.isAuthenticated,
  (isAuth) => {
    if (isAuth) {
      messageStore.loadAllUnread();
    } else {
      messageStore.reset();
    }
  }
);

function toggleNav(key: string) {
  if (activeNavItem.value === key) {
    activeNavItem.value = null;
  } else {
    activeNavItem.value = key;
  }
  // 切换一级时关闭所有二级
  activeSubNavItem.value = null;
}

/** 移动端：切换二级折叠（三级展开/收起） */
function toggleSubNav(key: string) {
  if (activeSubNavItem.value === key) {
    activeSubNavItem.value = null;
  } else {
    activeSubNavItem.value = key;
  }
}

function selectTheme(theme: Theme) {
  currentTheme.value = theme;
  setTheme(theme, true);

  const currentQuery = { ...route.query };
  currentQuery.theme = theme;
  router.replace({
    path: route.path,
    query: currentQuery
  });

  isThemeMenuOpen.value = false;
}

function handleLogout() {
  userStore.logoutWithApi();
  closeUserMenu();
  router.push('/');
}

function handleGoToProfile() {
  closeUserMenu();
  // 检查是否登录，未登录则跳转到登录页
  if (!requireAuth('/user')) {
    return;
  }
  router.push('/user');
}

function handleGoToSettings() {
  closeUserMenu();
  // 检查是否登录，未登录则跳转到登录页
  if (!requireAuth('/user/settings')) {
    return;
  }
  router.push('/user/settings');
}

function handleGoToWallet() {
  closeUserMenu();
  if (!requireAuth('/pay/wallet')) {
    return;
  }
  router.push('/pay/wallet');
}

function handleGoToBookshelf() {
  closeUserMenu();
  if (!requireAuth('/reading/bookshelf')) {
    return;
  }
  router.push('/reading/bookshelf');
}

function handleGoToMyReports() {
  closeUserMenu();
  if (!requireAuth('/my/reports')) {
    return;
  }
  router.push('/my/reports');
}

function handleGoToMyAttempts() {
  closeUserMenu();
  if (!requireAuth('/interview/my/attempts')) return;
  router.push('/interview/my/attempts');
}

function handleGoToMyFeedback() {
  closeUserMenu();
  if (!requireAuth('/my/feedback')) {
    return;
  }
  router.push('/my/feedback');
}

function handlePublish() {
  // 检查是否登录，未登录则跳转到登录页
  if (!requireAuth('/publish')) {
    return;
  }
  router.push('/publish');
}

/** 语音面试官：固定功能入口（需登录，直接由路由 requiresAuth 兜底） */
function handleGoVoiceInterview() {
  if (!requireAuth('/interview/voice')) return;
  router.push('/interview/voice');
}

function closeAllMenus() {
  isThemeMenuOpen.value = false;
  closeUserMenu();
  isMenuOpen.value = false;
  activeNavItem.value = null;
  activeSubNavItem.value = null;
}

const handleDocumentClick = (e: MouseEvent) => {
  const target = e.target as HTMLElement;
  // 点击不在任何菜单触发按钮和菜单内时，关闭所有下拉
  if (!target.closest('[data-menu-trigger]') && !target.closest('[data-menu-content]')) {
    closeAllMenus();
  }
};

onMounted(() => document.addEventListener('click', handleDocumentClick));
onUnmounted(() => document.removeEventListener('click', handleDocumentClick));
</script>

<template>
  <header class="sticky top-0 z-50 bg-theme-bg/95 backdrop-blur-md border-b border-theme-border shadow-theme-sm" @keydown.esc="closeAllMenus">
    <div class="content-container">
      <!-- 单行头部：Logo | 主导航 | 操作区（原型格局） -->
      <div class="flex items-center justify-between h-16 gap-2 sm:gap-4">
        <!-- 左侧：Logo + 今日主题 -->
        <Link to="/" class="flex items-center gap-2 sm:gap-3 flex-shrink-0 group">
          <div class="w-8 h-8 sm:w-9 sm:h-9 flex-shrink-0" aria-label="旭林知行">
            <svg viewBox="0 0 64 64" class="w-full h-full" xmlns="http://www.w3.org/2000/svg">
              <defs>
                <linearGradient id="nav-logo-bg" x1="0" y1="0" x2="64" y2="64" gradientUnits="userSpaceOnUse">
                  <stop stop-color="#fb923c"/>
                  <stop offset="1" stop-color="#dc2626"/>
                </linearGradient>
              </defs>
              <circle cx="32" cy="32" r="32" fill="url(#nav-logo-bg)"/>
              <path d="M25.5 44 Q 26 34.5 32 34 Q 38 34.5 38.5 44 Z" fill="#ffffff"/>
              <circle cx="32" cy="26" r="6" fill="#ffffff"/>
              <path d="M13 43 L 32 38 L 51 43 L 51 47 L 32 42 L 13 47 Z" fill="#ffffff" opacity="0.95"/>
              <path d="M13 43 L 32 38 L 51 43" fill="none" stroke="#fde68a" stroke-width="1.4" stroke-linecap="round" stroke-linejoin="round"/>
              <rect x="39" y="10" width="14" height="10" rx="5" fill="#ffffff"/>
              <path d="M43 20 L 43 24 L 47 20 Z" fill="#ffffff"/>
              <circle cx="42.5" cy="15" r="1.4" fill="#dc2626"/>
              <circle cx="46" cy="15" r="1.4" fill="#dc2626"/>
              <circle cx="49.5" cy="15" r="1.4" fill="#dc2626"/>
            </svg>
          </div>
          <div class="flex items-baseline gap-2">
            <h1 class="text-base sm:text-lg font-bold text-theme-text font-heading">旭林知行</h1>
            <span class="hidden xl:inline text-theme-text-tertiary">|</span>
            <span class="hidden xl:inline text-xs text-theme-primary font-medium">AI 驱动的求职助手</span>
          </div>
        </Link>

        <!-- 中间：主导航（PC 端） -->
        <nav class="hidden lg:flex items-center gap-0.5 xl:gap-1">
          <template v-for="(item) in navItems" :key="item.key">
            <!-- 首页直接跳转 -->
            <template v-if="item.key === 'home'">
              <Link
                  :to="item.path"
                  class="px-3 xl:px-4 py-2 text-sm font-medium rounded-theme-lg text-theme-text hover:text-theme-primary hover:bg-theme-primary-soft transition-theme-fast"
              >
                {{ item.name }}
              </Link>
            </template>
            <!-- 有 path 但无子菜单：直接跳转（动态分类等） -->
            <template v-else-if="item.path && item.children.length === 0">
              <Link
                  :to="item.path"
                  class="px-3 xl:px-4 py-2 text-sm font-medium rounded-theme-lg text-theme-text hover:text-theme-primary hover:bg-theme-primary-soft transition-theme-fast"
              >
                {{ item.name }}
              </Link>
            </template>
            <!-- 外部链接项（isExternal=true） -->
            <template v-else-if="item.isExternal">
              <a
                  :href="item.externalUrl"
                  target="_blank"
                  rel="noopener noreferrer"
                  class="px-3 xl:px-4 py-2 text-sm font-medium rounded-theme-lg text-theme-text hover:text-theme-primary hover:bg-theme-primary-soft transition-theme-fast"
              >
                {{ item.name }} ↗
              </a>
            </template>
            <!-- 其他有子菜单的项 -->
            <template v-else>
              <div class="relative" @click.stop>
                <button
                    @click="toggleNav(item.key)"
                    :class="[
                      'inline-flex items-center gap-1 px-3 xl:px-4 py-2 text-sm font-medium rounded-theme-lg transition-theme-fast',
                      activeNavItem === item.key ? 'bg-theme-primary text-theme-on-primary' : 'text-theme-text hover:text-theme-primary hover:bg-theme-primary-soft'
                    ]"
                    data-menu-trigger
                >
                  <span>{{ item.name }}</span>
                  <ChevronDown class="w-3.5 h-3.5 transition-transform duration-200" :class="{ 'rotate-180': activeNavItem === item.key }" />
                </button>
                <!-- PC端 Mega Menu -->
                <div
                    v-if="activeNavItem === item.key"
                    class="absolute top-full left-0 mt-2 theme-panel p-4 z-50 max-h-[85vh] overflow-y-auto min-w-[540px]"
                    data-menu-content
                >
                  <!-- Mega Menu 头部 -->
                  <div class="flex items-center gap-2 pb-3 mb-3 border-b border-theme-border">
                    <div
                        class="w-7 h-7 rounded-theme-md flex items-center justify-center text-xs flex-shrink-0 text-white"
                        style="background: linear-gradient(135deg, var(--theme-primary), #B91C1C);"
                    >
                      <IconRender :icon="item.icon" fallback="📁" />
                    </div>
                    <div class="min-w-0">
                      <div class="font-semibold text-sm text-theme-text">{{ item.name }}</div>
                      <div v-if="item.description" class="text-xs truncate text-theme-text-secondary">{{ item.description }}</div>
                    </div>
                  </div>
                  <!-- 内容区 -->
                  <div class="grid grid-cols-[120px_1fr] gap-x-5 gap-y-3">
                    <template v-for="(child, cidx) in item.children" :key="child.name">
                      <!-- 二级有三级子项 -->
                      <template v-if="child.children && child.children.length > 0">
                        <div class="flex items-start gap-1.5 pt-0.5">
                          <span v-if="child.icon" class="w-4 h-4 rounded-theme-sm flex items-center justify-center text-[10px] text-white flex-shrink-0 mt-0.5" :style="{ background: getIconColor(cidx) }"><IconRender :icon="child.icon" /></span>
                          <span class="text-sm font-semibold leading-tight text-theme-text-secondary">{{ child.name }}</span>
                        </div>
                        <div class="flex flex-wrap gap-x-3 gap-y-1.5">
                          <template v-for="(grandchild, gidx) in child.children" :key="grandchild.name">
                            <a
                                v-if="grandchild.isExternal"
                                :href="grandchild.path"
                                target="_blank"
                                rel="noopener noreferrer"
                                @click="activeNavItem = null"
                                class="inline-flex items-center gap-1 px-1.5 py-0.5 rounded-theme-md text-xs font-medium text-theme-text hover:bg-theme-surface-highlight transition-theme-fast group"
                            >
                              <span class="w-4 h-4 rounded-theme-sm flex items-center justify-center text-[10px] text-white flex-shrink-0" :style="{ background: getIconColor(gidx) }"><IconRender :icon="grandchild.icon" fallback="🔗" /></span>
                              <span class="whitespace-nowrap">{{ grandchild.name }} ↗</span>
                            </a>
                            <Link
                                v-else
                                :to="grandchild.path"
                                @click="activeNavItem = null"
                                class="inline-flex items-center gap-1 px-1.5 py-0.5 rounded-theme-md text-xs font-medium text-theme-text hover:bg-theme-surface-highlight transition-theme-fast group"
                            >
                              <span class="w-4 h-4 rounded-theme-sm flex items-center justify-center text-[10px] text-white flex-shrink-0" :style="{ background: getIconColor(gidx) }"><IconRender :icon="grandchild.icon" fallback="📄" /></span>
                              <span class="whitespace-nowrap">{{ grandchild.name }}</span>
                              <span v-if="grandchild.badge === 'NEW'" class="theme-badge bg-theme-danger text-white">NEW</span>
                              <span v-else-if="grandchild.badge === 'HOT'" class="theme-badge bg-theme-warning text-white">HOT</span>
                              <Lock v-if="grandchild.requiresAuth" class="w-2.5 h-2.5 opacity-50 group-hover:opacity-100 transition-opacity text-theme-primary" />
                            </Link>
                          </template>
                        </div>
                      </template>
                      <!-- 二级无三级子项（末级） -->
                      <template v-else>
                        <a
                            v-if="child.isExternal"
                            :href="child.path"
                            target="_blank"
                            rel="noopener noreferrer"
                            @click="activeNavItem = null"
                            class="col-span-2 inline-flex items-center gap-1.5 px-1.5 py-1 rounded-theme-md text-sm font-semibold text-theme-text hover:bg-theme-surface-highlight transition-theme-fast group"
                        >
                          <span class="w-4 h-4 rounded-theme-sm flex items-center justify-center text-[10px] text-white flex-shrink-0" :style="{ background: getIconColor(cidx) }"><IconRender :icon="child.icon" fallback="🔗" /></span>
                          <span class="whitespace-nowrap">{{ child.name }} ↗</span>
                          <span v-if="child.badge === 'NEW'" class="theme-badge bg-theme-danger text-white">NEW</span>
                          <span v-else-if="child.badge === 'HOT'" class="theme-badge bg-theme-warning text-white">HOT</span>
                          <Lock v-if="child.requiresAuth" class="w-3 h-3 opacity-50 group-hover:opacity-100 transition-opacity text-theme-primary" />
                        </a>
                        <Link
                            v-else
                            :to="child.path"
                            @click="activeNavItem = null"
                            class="col-span-2 inline-flex items-center gap-1.5 px-1.5 py-1 rounded-theme-md text-sm font-semibold text-theme-text hover:bg-theme-surface-highlight transition-theme-fast group"
                        >
                          <span class="w-4 h-4 rounded-theme-sm flex items-center justify-center text-[10px] text-white flex-shrink-0" :style="{ background: getIconColor(cidx) }"><IconRender :icon="child.icon" fallback="📄" /></span>
                          <span class="whitespace-nowrap">{{ child.name }}</span>
                          <span v-if="child.badge === 'NEW'" class="theme-badge bg-theme-danger text-white">NEW</span>
                          <span v-else-if="child.badge === 'HOT'" class="theme-badge bg-theme-warning text-white">HOT</span>
                          <Lock v-if="child.requiresAuth" class="w-3 h-3 opacity-50 group-hover:opacity-100 transition-opacity text-theme-primary" />
                        </Link>
                      </template>
                    </template>
                  </div>
                </div>
              </div>
            </template>
          </template>
        </nav>

        <!-- 右侧：功能按钮 + 用户区 -->
        <div class="flex items-center gap-0.5 sm:gap-1.5 flex-shrink-0">
          <!-- 帮助中心入口 -->
          <Link
              to="/help"
              class="w-9 h-9 rounded-theme-lg flex items-center justify-center text-theme-text-secondary hover:text-theme-primary hover:bg-theme-primary-soft transition-theme-fast"
              title="帮助中心"
              aria-label="帮助中心"
          >
            <HelpCircle class="w-4 h-4 sm:w-5 sm:h-5" />
          </Link>

          <!-- 搜索按钮 -->
          <button
              @click="router.push('/search')"
              class="w-9 h-9 rounded-theme-lg flex items-center justify-center text-theme-text-secondary hover:text-theme-primary hover:bg-theme-primary-soft transition-theme-fast"
              title="搜索"
          >
            <Search class="w-4 h-4 sm:w-5 sm:h-5" />
          </button>

          <!-- 消息中心入口（含未读徽章） -->
          <Link
              to="/messages"
              class="w-9 h-9 rounded-theme-lg flex items-center justify-center text-theme-text-secondary hover:text-theme-primary hover:bg-theme-primary-soft transition-theme-fast relative"
              title="消息中心"
              aria-label="消息中心"
          >
            <Bell class="w-4 h-4 sm:w-5 sm:h-5" />
            <span
                v-if="totalUnread > 0"
                class="absolute -top-0.5 -right-0.5 min-w-[18px] h-[18px] px-1 rounded-full text-xs flex items-center justify-center bg-theme-danger text-white font-mono"
            >
              {{ totalUnread > 99 ? '99+' : totalUnread }}
            </span>
          </Link>

          <!-- 主题切换 -->
          <div class="relative">
            <button
                @click="isThemeMenuOpen = !isThemeMenuOpen"
                class="w-9 h-9 rounded-theme-lg flex items-center justify-center text-theme-text-secondary hover:text-theme-primary hover:bg-theme-primary-soft transition-theme-fast"
                title="切换主题"
                data-menu-trigger
            >
              <Palette class="w-4 h-4 sm:w-5 sm:h-5" />
            </button>
            <div
                v-if="isThemeMenuOpen"
                class="absolute right-0 mt-2 w-44 theme-panel py-2 z-50"
                data-menu-content
            >
              <button
                  v-for="theme in ['light', 'dark', 'eye'] as Theme[]"
                  :key="theme"
                  @click="selectTheme(theme)"
                  class="w-full flex items-center gap-2 px-3 py-2 text-left text-sm text-theme-text hover:bg-theme-surface-highlight transition-theme-fast"
              >
                <Sun v-if="theme === 'light'" class="w-4 h-4 text-theme-warning" />
                <Moon v-else-if="theme === 'dark'" class="w-4 h-4 text-theme-info" />
                <Eye v-else-if="theme === 'eye'" class="w-4 h-4 text-theme-success" />
                <span>{{ themes[theme].name }}</span>
              </button>
            </div>
          </div>

          <div class="hidden sm:block h-5 w-px bg-theme-border mx-0.5 sm:mx-1"></div>

          <!-- 用户操作（已登录：仅头像，悬停/点击展开下拉） -->
          <template v-if="currentUser">
            <div
                class="relative"
                @mouseenter="isUserMenuOpen = true"
                @mouseleave="!isUserMenuPinned && (isUserMenuOpen = false)"
            >
              <button
                  @click="toggleUserMenu"
                  class="w-9 h-9 rounded-theme-full flex items-center justify-center hover:bg-theme-surface-highlight transition-theme-fast flex-shrink-0"
                  data-menu-trigger
                  :aria-label="currentUser.username"
                  :title="(currentUser as any).nickname || currentUser.username"
              >
                <img
                    :src="getSafeAvatar(currentUser.avatar, currentUser.id)"
                    :alt="currentUser.username"
                    class="w-7 h-7 rounded-theme-full"
                    loading="lazy"
                    @error="(e: Event) => (e.target as HTMLImageElement).src = getSafeAvatar(null, currentUser.id)"
                />
              </button>

                <!-- 用户下拉菜单 -->
                <div
                    v-if="isUserMenuOpen"
                    class="absolute right-0 mt-2 w-52 theme-panel py-2 z-50"
                    data-menu-content
                >
                  <div class="px-3 py-2 border-b border-theme-border mb-1">
                    <p class="text-sm font-medium text-theme-text truncate">
                      {{ (currentUser as any).nickname || currentUser.username }}
                    </p>
                    <p class="text-xs text-theme-text-tertiary truncate">
                      {{ currentUser.email || '' }}
                    </p>
                  </div>

                  <button
                      @click="handleGoToProfile"
                      class="w-full flex items-center gap-2 px-3 py-2 text-left text-sm text-theme-text hover:bg-theme-surface-highlight transition-theme-fast"
                  >
                    <UserCircle class="w-4 h-4 text-theme-text-secondary" />
                    <span>个人中心</span>
                  </button>

                  <button
                      @click="handleGoToBookshelf"
                      class="w-full flex items-center gap-2 px-3 py-2 text-left text-sm text-theme-text hover:bg-theme-surface-highlight transition-theme-fast"
                  >
                    <BookMarked class="w-4 h-4 text-theme-text-secondary" />
                    <span>我的书架</span>
                  </button>

                  <button
                      @click="handleGoToWallet"
                      class="w-full flex items-center gap-2 px-3 py-2 text-left text-sm text-theme-text hover:bg-theme-surface-highlight transition-theme-fast"
                  >
                    <Wallet class="w-4 h-4 text-theme-text-secondary" />
                    <span>我的钱包</span>
                  </button>

                  <button
                      @click="handleGoToMyAttempts"
                      class="w-full flex items-center justify-between px-3 py-2 text-left text-sm text-theme-text hover:bg-theme-surface-highlight transition-theme-fast"
                  >
                    <span class="flex items-center gap-2">
                      <FileText class="w-4 h-4 text-theme-text-secondary" />
                      <span>我的面试答题</span>
                    </span>
                    <span class="theme-badge bg-theme-danger text-white">NEW</span>
                  </button>

                  <button
                      @click="handleGoToMyReports"
                      class="w-full flex items-center gap-2 px-3 py-2 text-left text-sm text-theme-text hover:bg-theme-surface-highlight transition-theme-fast"
                  >
                    <Flag class="w-4 h-4 text-theme-text-secondary" />
                    <span>我的举报</span>
                  </button>

                  <button
                      @click="handleGoToMyFeedback"
                      class="w-full flex items-center gap-2 px-3 py-2 text-left text-sm text-theme-text hover:bg-theme-surface-highlight transition-theme-fast"
                  >
                    <MessageSquare class="w-4 h-4 text-theme-text-secondary" />
                    <span>我的反馈</span>
                  </button>

                  <button
                      @click="handleGoToSettings"
                      class="w-full flex items-center gap-2 px-3 py-2 text-left text-sm text-theme-text hover:bg-theme-surface-highlight transition-theme-fast"
                  >
                    <Settings class="w-4 h-4 text-theme-text-secondary" />
                    <span>账号设置</span>
                  </button>

                  <div class="border-t border-theme-border my-1"></div>

                  <button
                      @click="handleLogout"
                      class="w-full flex items-center gap-2 px-3 py-2 text-left text-sm text-theme-danger hover:bg-theme-danger-bg transition-theme-fast"
                  >
                    <LogOut class="w-4 h-4" />
                    <span>退出登录</span>
                  </button>
                </div>
              </div>
            </template>
            <!-- 未登录：登录/注册合并入口（默认跳转登录页） -->
            <template v-else>
              <Link
                  to="/login"
                  class="px-3 sm:px-4 py-1.5 sm:py-2 rounded-theme-lg text-sm font-semibold bg-theme-primary text-theme-on-primary hover:bg-theme-primary-hover shadow-theme-md transition-theme-fast flex-shrink-0"
              >
                登录/注册
              </Link>
            </template>

            <!-- 移动端菜单按钮 -->
            <button
                @click="isMenuOpen = !isMenuOpen"
                class="lg:hidden w-9 h-9 rounded-theme-lg flex items-center justify-center text-theme-text hover:bg-theme-surface-highlight transition-theme-fast"
                data-menu-trigger
            >
              <Menu v-if="!isMenuOpen" class="w-5 h-5" />
              <X v-else class="w-5 h-5" />
            </button>
          </div>
        </div>
      </div>

    <!-- 移动端菜单 -->
    <div
        v-if="isMenuOpen"
        class="lg:hidden border-t border-theme-border bg-theme-bg max-h-[85vh] overflow-y-auto overscroll-contain"
        data-menu-content
    >
      <div class="content-container py-2 space-y-1.5">
        <!-- 移动端顶部功能入口（固定） -->
        <Link
            to="/help"
            @click="isMenuOpen = false"
            class="flex items-center justify-between border border-theme-primary rounded-theme-lg px-4 py-2.5 bg-theme-surface text-theme-primary"
        >
          <span class="font-semibold text-sm flex items-center gap-2">
            <HelpCircle class="w-4 h-4" />
            帮助中心
          </span>
          <ChevronRight class="w-4 h-4" />
        </Link>

        <div v-for="(item, iidx) in navItems" :key="item.key">
          <!-- 首页直接跳转 -->
          <Link
              v-if="item.key === 'home'"
              :to="item.path"
              @click="isMenuOpen = false"
              class="flex items-center gap-2 border border-theme-border rounded-theme-lg px-4 py-2.5 text-theme-text bg-theme-surface"
          >
            <span v-if="item.icon" class="w-4 h-4 rounded-theme-sm flex items-center justify-center text-[10px] text-white flex-shrink-0" :style="{ background: getIconColor(iidx) }"><IconRender :icon="item.icon" /></span>
            <span class="font-semibold text-sm">{{ item.name }}</span>
          </Link>
          <!-- 外部链接项 -->
          <a
              v-else-if="item.isExternal"
              :href="item.externalUrl"
              target="_blank"
              rel="noopener noreferrer"
              @click="isMenuOpen = false"
              class="flex items-center gap-2 border border-theme-border rounded-theme-lg px-4 py-2.5 text-theme-text bg-theme-surface"
          >
            <span v-if="item.icon" class="w-4 h-4 rounded-theme-sm flex items-center justify-center text-[10px] text-white flex-shrink-0" :style="{ background: getIconColor(iidx) }"><IconRender :icon="item.icon" /></span>
            <span class="font-semibold text-sm">{{ item.name }} ↗</span>
          </a>
          <!-- 有 path 但无子菜单：直接跳转 -->
          <Link
              v-else-if="item.path && item.children.length === 0"
              :to="item.path"
              @click="isMenuOpen = false"
              class="flex items-center gap-2 border border-theme-border rounded-theme-lg px-4 py-2.5 text-theme-text bg-theme-surface"
          >
            <span v-if="item.icon" class="w-4 h-4 rounded-theme-sm flex items-center justify-center text-[10px] text-white flex-shrink-0" :style="{ background: getIconColor(iidx) }"><IconRender :icon="item.icon" /></span>
            <span class="font-semibold text-sm">{{ item.name }}</span>
          </Link>
          <!-- 有子菜单：点击展开/折叠 -->
          <div v-else class="border border-theme-border rounded-theme-lg overflow-hidden bg-theme-surface">
            <button
                @click="toggleNav(item.key)"
                class="w-full flex items-center justify-between px-4 py-2.5 text-theme-text"
                :class="activeNavItem === item.key ? 'bg-theme-surface-highlight' : ''"
            >
              <span class="flex items-center gap-2">
                <span v-if="item.icon" class="w-4 h-4 rounded-theme-sm flex items-center justify-center text-[10px] text-white flex-shrink-0" :style="{ background: getIconColor(iidx) }"><IconRender :icon="item.icon" /></span>
                <span class="font-semibold text-sm">{{ item.name }}</span>
              </span>
              <ChevronDown
                  class="w-4 h-4 transition-transform duration-200"
                  :class="{ 'rotate-180': activeNavItem === item.key }"
              />
            </button>
            <!-- 移动端二级菜单 -->
            <div
                v-if="activeNavItem === item.key"
                class="border-t border-theme-border bg-theme-bg"
            >
              <template v-for="(child, idx) in item.children" :key="child.name">
                <!-- 二级有三级子项 -->
                <div
                    v-if="child.children && child.children.length > 0"
                    :class="idx > 0 ? 'border-t border-theme-border' : ''"
                >
                  <button
                      @click="toggleSubNav(item.key + '/' + child.name)"
                      class="w-full flex items-center justify-between px-5 py-2 text-xs text-theme-text-secondary font-semibold"
                      :class="activeSubNavItem === item.key + '/' + child.name ? 'bg-theme-bg-elevated' : ''"
                  >
                    <span class="flex items-center gap-1.5">
                      <span v-if="child.icon" class="w-5 h-5 rounded-theme-sm flex items-center justify-center text-[10px] text-white" :style="{ background: getIconColor(idx) }"><IconRender :icon="child.icon" /></span>
                      <span>{{ child.name }}</span>
                    </span>
                    <ChevronDown
                        class="w-3.5 h-3.5 transition-transform duration-200"
                        :class="{ 'rotate-180': activeSubNavItem === item.key + '/' + child.name }"
                    />
                  </button>
                  <!-- 三级项列表 -->
                  <div v-if="activeSubNavItem === item.key + '/' + child.name" class="bg-theme-bg">
                    <template v-for="(grandchild, gidx) in child.children" :key="grandchild.name">
                      <a
                          v-if="grandchild.isExternal"
                          :href="grandchild.path"
                          target="_blank"
                          rel="noopener noreferrer"
                          @click="isMenuOpen = false"
                          class="flex items-center gap-2 px-7 py-2 text-xs text-theme-text"
                          :class="gidx > 0 ? 'border-t border-theme-border' : ''"
                      >
                        <span class="w-4 h-4 rounded-theme-sm flex items-center justify-center text-[9px] text-white flex-shrink-0" :style="{ background: getIconColor(gidx) }"><IconRender :icon="grandchild.icon" fallback="🔗" /></span>
                        <span class="flex-1 truncate">{{ grandchild.name }} ↗</span>
                      </a>
                      <Link
                          v-else
                          :to="grandchild.path"
                          @click="isMenuOpen = false"
                          class="flex items-center gap-2 px-7 py-2 text-xs text-theme-text group"
                          :class="gidx > 0 ? 'border-t border-theme-border' : ''"
                      >
                        <span class="w-4 h-4 rounded-theme-sm flex items-center justify-center text-[9px] text-white flex-shrink-0" :style="{ background: getIconColor(gidx) }"><IconRender :icon="grandchild.icon" fallback="📄" /></span>
                        <span class="flex-1 truncate">{{ grandchild.name }}</span>
                        <span
                            v-if="grandchild.badge === 'NEW'"
                            class="theme-badge bg-theme-danger text-white"
                        >NEW</span>
                        <span
                            v-else-if="grandchild.badge === 'HOT'"
                            class="theme-badge bg-theme-warning text-white"
                        >HOT</span>
                        <Lock
                            v-if="grandchild.requiresAuth"
                            class="w-3 h-3 opacity-60 group-hover:opacity-100 transition-opacity text-theme-primary"
                        />
                      </Link>
                    </template>
                  </div>
                </div>
                <!-- 二级无三级子项：外部链接 -->
                <a
                    v-else-if="child.isExternal"
                    :href="child.path"
                    target="_blank"
                    rel="noopener noreferrer"
                    @click="isMenuOpen = false"
                    class="flex items-center gap-2 px-5 py-2 text-xs text-theme-text"
                    :class="idx > 0 ? 'border-t border-theme-border' : ''"
                >
                  <span class="w-5 h-5 rounded-theme-sm flex items-center justify-center text-[10px] text-white flex-shrink-0" :style="{ background: getIconColor(idx) }"><IconRender :icon="child.icon" fallback="🔗" /></span>
                  <span class="flex-1 truncate">{{ child.name }} ↗</span>
                </a>
                <!-- 二级无三级子项：内部路由 -->
                <Link
                    v-else
                    :to="child.path"
                    @click="isMenuOpen = false"
                    class="flex items-center gap-2 px-5 py-2 text-xs text-theme-text group"
                    :class="idx > 0 ? 'border-t border-theme-border' : ''"
                >
                  <span class="w-5 h-5 rounded-theme-sm flex items-center justify-center text-[10px] text-white flex-shrink-0" :style="{ background: getIconColor(idx) }"><IconRender :icon="child.icon" fallback="📄" /></span>
                  <span class="flex-1 truncate">{{ child.name }}</span>
                  <span
                      v-if="child.badge === 'NEW'"
                      class="theme-badge bg-theme-danger text-white"
                  >NEW</span>
                  <span
                      v-else-if="child.badge === 'HOT'"
                      class="theme-badge bg-theme-warning text-white"
                  >HOT</span>
                  <Lock
                      v-if="child.requiresAuth"
                      class="w-3 h-3 opacity-60 group-hover:opacity-100 transition-opacity text-theme-primary"
                  />
                </Link>
              </template>
            </div>
          </div>
        </div>
      </div>
    </div>
  </header>
</template>

<style scoped>
</style>
