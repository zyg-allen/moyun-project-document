<script setup lang="ts">
import { ref, computed, onMounted, onUnmounted, watch } from 'vue';
import { RouterLink as Link, useRouter, useRoute } from 'vue-router';
import {
  Search, Plus, LogOut, Menu, X, Palette, Sun, Moon, Eye,
  ChevronDown, ChevronRight, Settings, UserCircle, BookMarked, MessageSquare,
  HelpCircle, Lock
} from 'lucide-vue-next';
import { setTheme, getCurrentTheme, type Theme, themes } from '@/utils/theme';
import { useUserStore } from '@/stores/user';
import { useMessageStore } from '@/stores/message';
import { getSafeAvatar } from '@/utils/avatar';
import { useAuth } from '@/composables/useAuth';
import NotificationBell from './NotificationBell.vue';
import * as notificationApi from '@/api/notification';

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
const notifications = ref<any[]>([]);
// 通知未读数从消息 store 取，与 MessagesPage 跨组件同步
const unreadCount = computed(() => messageStore.notifUnreadCount);
// 私信未读数从消息 store 取，用于头部"消息中心"按钮徽章
const msgUnreadCount = computed(() => messageStore.msgUnreadCount);
// 头部消息中心按钮徽章：仅显示私信未读数（通知未读数由铃铛单独展示，避免重复计数）
const msgBadgeCount = computed(() => messageStore.msgUnreadCount);
const isUserMenuOpen = ref(false);

// 导航项统一类型：所有属性可选（除 name/key/children），避免联合类型访问报错
interface NavItem {
  name: string;
  key: string;
  path?: string;
  externalUrl?: string | null;
  isExternal?: boolean;
  children: { name: string; path?: string; isExternal?: boolean; requiresAuth?: boolean }[];
}

// 导航数据结构 - 7 个一级菜单：首页 / 分类 / 读书 / 面试（含学习中心） / 创作 / 互动 / 我的
// 学习中心已并入面试（数据全部来自面试题库），社区已拆解（FeedPage→我的、Authors→创作、排行/成就→我的）
const navItems = computed<NavItem[]>(() => {
  const isLoggedIn = userStore.isAuthenticated;
  return [
    // 1. 首页 - 平台总入口
    {
      name: '首页',
      key: 'home',
      path: '/',
      children: []
    },
    // 2. 分类（静态，文章内容分类聚合）
    {
      name: '分类',
      key: 'category',
      children: [
        { name: '散文天地', path: '/category/' + encodeURIComponent('散文天地') },
        { name: '技术笔记', path: '/category/' + encodeURIComponent('技术笔记') },
        { name: '技能工坊', path: '/category/' + encodeURIComponent('技能工坊') },
      ]
    },
    // 3. 读书空间 - 文学爱好者内容消费与沉淀
    {
      name: '读书',
      key: 'reading',
      path: '/reading',
      children: [
        { name: '读书首页', path: '/reading' },
        { name: '发现好书', path: '/reading/discover' },
        { name: '共读活动', path: '/reading/club' },
        { name: '我的书架', path: '/reading/bookshelf', requiresAuth: true },
      ]
    },
    // 4. 面试 - 求职者面试闭环（已合并原"学习中心"7 个页面，因为数据全部来自面试题库）
    {
      name: '面试',
      key: 'interview',
      path: '/interview',
      children: [
        { name: '面试题库', path: '/interview' },
        { name: '面试经验', path: '/interview/experiences' },
        { name: '简历模板', path: '/interview/resume-templates' },
        { name: '在招职位', path: '/interview/jobs' },
        { name: 'AI 模拟面试', path: '/interview/mock', requiresAuth: true },
        // 学习中心子模块（合并自原 /learn 菜单）
        { name: '学习中心', path: '/learn' },
        { name: '知识图谱', path: '/learn/knowledge' },
        { name: '刷题排行榜', path: '/learn/leaderboard' },
        { name: '学习计划', path: '/learn/plan', requiresAuth: true },
        { name: '错题本', path: '/learn/wrong', requiresAuth: true },
        { name: '刷题日历', path: '/learn/calendar', requiresAuth: true },
      ]
    },
    // 5. 创作 - 创作者内容生态
    {
      name: '创作',
      key: 'create',
      children: [
        { name: '发布文章', path: '/publish', requiresAuth: true },
        { name: '我的文章', path: '/my/articles', requiresAuth: true },
        { name: '专栏广场', path: '/columns' },
        { name: '我的专栏', path: '/column/my', requiresAuth: true },
        { name: '创作挑战', path: '/contests' },
        { name: '创作者认证', path: '/creator/certification', requiresAuth: true },
        { name: '创作者列表', path: '/authors' },
      ]
    },
    // 6. 互动 - 社区互动聚合（话题/动态/挑战）
    {
      name: '互动',
      key: 'interaction',
      children: [
        { name: '话题广场', path: '/topics' },
        { name: '动态广场', path: '/feed' },
        { name: '创作挑战', path: '/contests' },
      ]
    },
    // 7. 我的 - 个人中心聚合（原"社区"已拆解到此）
    {
      name: '我的',
      key: 'mine',
      children: [
        { name: '个人中心', path: '/user', requiresAuth: true },
        { name: '成长时间线', path: '/growth/timeline', requiresAuth: true },
        { name: '动态广场', path: '/feed' },
        { name: '我的话题', path: '/topic/my/topics', requiresAuth: true },
        { name: '我的观点', path: '/topic/my/posts', requiresAuth: true },
        { name: '成长排行榜', path: '/ranking' },
        { name: '成就徽章', path: '/achievements', requiresAuth: true },
      ]
    },
  ].filter(item => {
    // 未登录用户：过滤掉所有子项都需登录的菜单（避免空菜单）
    if (!isLoggedIn && item.children.length > 0) {
      const hasPublicChild = item.children.some(c => !c.requiresAuth);
      return hasPublicChild;
    }
    return true;
  });
});

const currentUser = computed(() => userStore.user)

onMounted(async () => {
  currentTheme.value = getCurrentTheme();
  if (userStore.isAuthenticated) {
    await loadNotifications();
    await messageStore.loadAllUnread();
  }
});

watch(
  () => userStore.isAuthenticated,
  (isAuth) => {
    if (isAuth) {
      loadNotifications();
      messageStore.loadAllUnread();
    } else {
      notifications.value = [];
      messageStore.reset();
    }
  }
);

async function loadNotifications() {
  try {
    const response = await notificationApi.getNotificationList({ pageNum: 1, pageSize: 10 });
    if (response.code === 200 && response.data) {
      notifications.value = response.data.list.map((item: any) => ({
        id: String(item.id),
        title: item.title,
        content: item.content,
        time: item.createTime,
        isRead: item.isRead
      }));
    }
  } catch (error) {
    console.error('加载通知失败:', error);
    notifications.value = [];
  }
}

async function markAsRead(id: string) {
  try {
    const response = await notificationApi.markAsRead({ id });
    if (response.code === 200) {
      const notification = notifications.value.find(n => n.id === id);
      if (notification) {
        notification.isRead = true;
      }
      // 本地未读数 -1（store 同步给 MessagesPage）
      messageStore.decNotifUnread();
    }
  } catch (error) {
    console.error('标记已读失败:', error);
  }
}

function toggleNav(key: string) {
  if (activeNavItem.value === key) {
    activeNavItem.value = null;
  } else {
    activeNavItem.value = key;
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
  isUserMenuOpen.value = false;
  router.push('/');
}

function handleGoToProfile() {
  isUserMenuOpen.value = false;
  // 检查是否登录，未登录则跳转到登录页
  if (!requireAuth('/user')) {
    return;
  }
  router.push('/user');
}

function handleGoToSettings() {
  isUserMenuOpen.value = false;
  // 检查是否登录，未登录则跳转到登录页
  if (!requireAuth('/user/settings')) {
    return;
  }
  router.push('/user/settings');
}

function handleGoToBookshelf() {
  isUserMenuOpen.value = false;
  if (!requireAuth('/reading/bookshelf')) {
    return;
  }
  router.push('/reading/bookshelf');
}

function handlePublish() {
  // 检查是否登录，未登录则跳转到登录页
  if (!requireAuth('/publish')) {
    return;
  }
  router.push('/publish');
}

function closeAllMenus() {
  isThemeMenuOpen.value = false;
  isUserMenuOpen.value = false;
  isMenuOpen.value = false;
  activeNavItem.value = null;
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
  <header class="sticky top-0 z-50 shadow-md" style="background-color: var(--theme-bg);" @keydown.esc="closeAllMenus">
    <!-- 顶部栏 -->
    <div class="border-b" style="background: linear-gradient(135deg, var(--theme-surface) 0%, var(--theme-bg) 100%); border-color: var(--theme-border);">
      <div class="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8">
        <div class="flex items-center justify-between py-2 sm:py-3 text-sm">
          <!-- 左侧：Logo + 今日主题 -->
          <div class="flex items-center gap-2 sm:gap-3">
            <Link to="/" class="flex items-center space-x-1 sm:space-x-2 flex-shrink-0">
              <div class="w-6 h-6 sm:w-10 sm:h-10 bg-gradient-to-br from-red-600 to-orange-500 rounded-full flex items-center justify-center">
                <span class="text-white font-bold text-xs sm:text-lg">墨</span>
              </div>
              <div>
                <h1 class="hidden sm:block text-base sm:text-xl font-bold" style="color: var(--theme-text);">墨韵</h1>
              </div>
            </Link>

            <span class="hidden sm:inline" style="color: var(--theme-text-secondary);">|</span>
            <span class="text-xs sm:text-sm" style="color: var(--theme-text);">
              <span class="sm:hidden">听松看云</span>
              <span class="hidden sm:inline">今日主题：听松看云</span>
            </span>
          </div>

          <!-- 右侧：搜索和操作 -->
          <div class="flex items-center space-x-1 sm:space-x-3">
            <!-- 帮助中心入口（醒目但不占主导航位置） -->
            <Link
                to="/help"
                class="p-2.5 rounded-lg transition-all duration-200 hover:scale-110 relative group"
                style="color: var(--theme-primary);"
                title="帮助中心"
                aria-label="帮助中心"
            >
              <HelpCircle class="w-4 h-4 sm:w-5 sm:h-5" />
              <span class="absolute -top-0.5 -right-0.5 w-2 h-2 rounded-full animate-pulse" style="background-color: var(--theme-primary);"></span>
            </Link>

            <!-- 搜索图标按钮 -->
            <button
                @click="router.push('/search')"
                class="p-2.5 rounded-lg transition-colors"
                style="color: var(--theme-text-secondary);"
                title="搜索"
            >
              <Search class="w-4 h-4 sm:w-5 sm:h-5" />
            </button>

            <!-- 消息铃铛 -->
            <NotificationBell
                :notifications="notifications"
                :unread-count="unreadCount"
                @read="markAsRead"
            />

            <!-- 消息中心入口 -->
            <Link
                v-if="currentUser"
                to="/messages"
                class="p-2.5 rounded-lg transition-colors relative"
                style="color: var(--theme-text-secondary);"
                title="消息中心"
                aria-label="消息中心"
            >
              <MessageSquare class="w-4 h-4 sm:w-5 sm:h-5" />
              <span
                v-if="msgBadgeCount > 0"
                class="absolute -top-1 -right-1 min-w-[18px] h-[18px] px-1 rounded-full text-xs flex items-center justify-center"
                style="background-color: #ef4444; color: white;"
              >
                {{ msgBadgeCount > 99 ? '99+' : msgBadgeCount }}
              </span>
            </Link>

            <!-- 主题切换 -->
            <div class="relative">
              <button
                  @click="isThemeMenuOpen = !isThemeMenuOpen"
                  class="p-2.5 rounded-full transition-colors"
                  style="color: var(--theme-text);"
                  data-menu-trigger
              >
                <Palette class="w-4 h-4" />
              </button>
              <div
                  v-if="isThemeMenuOpen"
                  class="absolute right-0 mt-2 w-40 rounded-lg shadow-lg border py-2 z-50"
                  style="background-color: var(--theme-bg); border-color: var(--theme-border);"
                  data-menu-content
              >
                <button
                    v-for="theme in ['light', 'dark', 'eye'] as Theme[]"
                    :key="theme"
                    @click="selectTheme(theme)"
                    class="w-full flex items-center space-x-2 px-3 py-2 text-left transition-colors"
                    :style="{ backgroundColor: 'var(--theme-surface)' }"
                >
                  <Sun v-if="theme === 'light'" class="w-4 h-4 text-yellow-500" />
                  <Moon v-else-if="theme === 'dark'" class="w-4 h-4 text-indigo-500" />
                  <Eye v-else-if="theme === 'eye'" class="w-4 h-4 text-green-600" />
                  <span class="text-sm" style="color: var(--theme-text);">
                    {{ themes[theme].name }}
                  </span>
                </button>
              </div>
            </div>

            <!-- 用户操作 -->
            <template v-if="currentUser">
              <button
                  @click="handlePublish"
                  class="flex items-center space-x-1 px-2 sm:px-3 py-1.5 rounded-full text-sm font-medium transition-colors flex-shrink-0"
                  style="background-color: var(--theme-primary); color: white;"
              >
                <Plus class="w-4 h-4" />
                <span class="hidden sm:inline">创作</span>
              </button>

              <!-- 用户头像下拉菜单 -->
              <div class="relative">
                <button
                    @click="isUserMenuOpen = !isUserMenuOpen"
                    class="flex items-center space-x-2 hover:opacity-80 transition-opacity flex-shrink-0"
                    data-menu-trigger
                >
                  <img
                      :src="getSafeAvatar(currentUser.avatar, currentUser.id)"
                      :alt="currentUser.username"
                      class="w-8 h-8 rounded-full"
                      loading="lazy"
                      @error="(e: Event) => (e.target as HTMLImageElement).src = getSafeAvatar(null, currentUser.id)"
                  />
                  <span class="text-sm font-medium hidden sm:inline" style="color: var(--theme-text);">
                    {{ (currentUser as any).nickname || currentUser.username }}
                  </span>
                  <ChevronDown class="w-3 h-3 hidden sm:block" style="color: var(--theme-text-secondary);" />
                </button>

                <!-- 用户下拉菜单 -->
                <div
                    v-if="isUserMenuOpen"
                    class="absolute right-0 mt-2 w-48 rounded-lg shadow-lg border py-2 z-50"
                    style="background-color: var(--theme-bg); border-color: var(--theme-border);"
                    data-menu-content
                >
                  <div class="px-3 py-2 border-b" style="border-color: var(--theme-border);">
                    <p class="text-sm font-medium" style="color: var(--theme-text);">
                      {{ (currentUser as any).nickname || currentUser.username }}
                    </p>
                    <p class="text-xs" style="color: var(--theme-text-secondary);">
                      {{ currentUser.email || '' }}
                    </p>
                  </div>

                  <button
                      @click="handleGoToProfile"
                      class="w-full flex items-center space-x-2 px-3 py-2 text-left transition-colors hover:opacity-80"
                      :style="{ backgroundColor: 'var(--theme-surface)' }"
                  >
                    <UserCircle class="w-4 h-4" style="color: var(--theme-text-secondary);" />
                    <span class="text-sm" style="color: var(--theme-text);">个人中心</span>
                  </button>

                  <button
                      @click="handleGoToBookshelf"
                      class="w-full flex items-center space-x-2 px-3 py-2 text-left transition-colors hover:opacity-80"
                  >
                    <BookMarked class="w-4 h-4" style="color: var(--theme-text-secondary);" />
                    <span class="text-sm" style="color: var(--theme-text);">我的书架</span>
                  </button>

                  <button
                      @click="handleGoToSettings"
                      class="w-full flex items-center space-x-2 px-3 py-2 text-left transition-colors hover:opacity-80"
                  >
                    <Settings class="w-4 h-4" style="color: var(--theme-text-secondary);" />
                    <span class="text-sm" style="color: var(--theme-text);">账号设置</span>
                  </button>

                  <div class="border-t my-1" style="border-color: var(--theme-border);"></div>

                  <button
                      @click="handleLogout"
                      class="w-full flex items-center space-x-2 px-3 py-2 text-left transition-colors hover:opacity-80"
                  >
                    <LogOut class="w-4 h-4" style="color: var(--theme-danger);" />
                    <span class="text-sm" style="color: var(--theme-danger);">退出登录</span>
                  </button>
                </div>
              </div>
            </template>
            <template v-else>
              <Link
                  to="/login"
                  class="px-3 py-2.5 font-medium transition-colors flex-shrink-0 inline-flex items-center"
                  style="color: var(--theme-text);"
              >
                <span class="hidden sm:inline">登录/注册</span>
                <span class="sm:hidden">登录</span>
              </Link>
            </template>
          </div>
        </div>
      </div>
    </div>

    <!-- 导航区 -->
    <div class="border-b" style="background-color: var(--theme-bg); border-color: var(--theme-border);">
      <div class="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8">
        <div class="flex items-center justify-between py-3">
          <!-- PC端导航 -->
          <nav class="hidden lg:flex items-center space-x-2">
            <template v-for="item in navItems" :key="item.key">
              <!-- 首页直接跳转 -->
              <template v-if="item.key === 'home'">
                <Link
                    :to="item.path"
                    class="px-5 py-2.5 text-base font-semibold transition-all duration-200 rounded-lg hover:scale-105 hover:shadow-sm"
                    style="color: var(--theme-text);"
                >
                  {{ item.name }}
                </Link>
              </template>
              <!-- 有 path 但无子菜单：直接跳转（动态分类等） -->
              <template v-else-if="item.path && item.children.length === 0">
                <Link
                    :to="item.path"
                    class="px-5 py-2.5 text-base font-semibold transition-all duration-200 rounded-lg hover:scale-105 hover:shadow-sm"
                    style="color: var(--theme-text);"
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
                    class="px-5 py-2.5 text-base font-semibold transition-all duration-200 rounded-lg hover:scale-105 hover:shadow-sm"
                    style="color: var(--theme-text);"
                >
                  {{ item.name }} ↗
                </a>
              </template>
              <!-- 其他有子菜单的项（读书/面试/学习/创作/社区 + 动态分类） -->
              <template v-else>
                <div class="relative" @click.stop>
                  <button
                      @click="toggleNav(item.key)"
                      :class="[
                      'inline-flex items-center gap-1 px-5 py-2.5 text-base font-semibold transition-all duration-200 rounded-lg',
                      activeNavItem === item.key ? 'shadow-md scale-105' : 'hover:scale-105 hover:shadow-sm'
                    ]"
                      :style="{
                      color: activeNavItem === item.key ? 'white' : 'var(--theme-text)',
                      backgroundColor: activeNavItem === item.key ? 'var(--theme-primary)' : 'transparent'
                    }"
                      data-menu-trigger
                  >
                    <span>{{ item.name }}</span>
                    <ChevronDown class="w-3.5 h-3.5 transition-transform duration-200" :class="{ 'rotate-180': activeNavItem === item.key }" />
                  </button>
                  <!-- PC端二级菜单 -->
                  <div
                      v-if="activeNavItem === item.key"
                      class="absolute top-full left-0 mt-2 w-72 shadow-xl border rounded-xl py-3 z-50 transform transition-all duration-200"
                      style="background-color: var(--theme-bg); border-color: var(--theme-border);"
                      data-menu-content
                  >
                    <template v-for="(child, idx) in item.children" :key="child.name">
                      <!-- 子菜单外部链接 -->
                      <a
                          v-if="child.isExternal"
                          :href="child.path"
                          target="_blank"
                          rel="noopener noreferrer"
                          @click="activeNavItem = null"
                          class="flex items-center justify-between px-5 py-3 text-base hover:scale-105 transition-all duration-150"
                          :style="{
                            color: 'var(--theme-text)',
                            borderTop: idx > 0 ? '1px solid var(--theme-border)' : 'none'
                          }"
                      >
                        <span>{{ child.name }} ↗</span>
                      </a>
                      <!-- 子菜单内部路由 -->
                      <Link
                          v-else
                          :to="child.path"
                          @click="activeNavItem = null"
                          class="flex items-center justify-between px-5 py-3 text-base hover:scale-105 transition-all duration-150 group"
                          :style="{
                            color: 'var(--theme-text)',
                            borderTop: idx > 0 ? '1px solid var(--theme-border)' : 'none'
                          }"
                      >
                        <span>{{ child.name }}</span>
                        <!-- 需登录的子项：醒目锁标记 -->
                        <Lock
                            v-if="child.requiresAuth"
                            class="w-3.5 h-3.5 opacity-60 group-hover:opacity-100 transition-opacity"
                            style="color: var(--theme-primary);"
                        />
                      </Link>
                    </template>
                  </div>
                </div>
              </template>
            </template>
          </nav>

          <!-- 移动端菜单按钮 -->
          <button
              @click="isMenuOpen = !isMenuOpen"
              class="lg:hidden p-2 rounded-lg transition-colors"
              style="color: var(--theme-text);"
              data-menu-trigger
          >
            <Menu v-if="!isMenuOpen" class="w-6 h-6" />
            <X v-else class="w-6 h-6" />
          </button>
        </div>
      </div>
    </div>

    <!-- 移动端菜单 -->
    <div
        v-if="isMenuOpen"
        class="lg:hidden border-t max-h-[80vh] overflow-y-auto"
        style="background-color: var(--theme-bg); border-color: var(--theme-border);"
        data-menu-content
    >
      <div class="px-4 py-3 space-y-2">
        <!-- 帮助中心入口（醒目置顶） -->
        <Link
            to="/help"
            @click="isMenuOpen = false"
            class="flex items-center justify-between border rounded-xl px-5 py-4"
            style="color: var(--theme-primary); border-color: var(--theme-primary); background-color: var(--theme-surface);"
        >
          <span class="font-semibold text-base flex items-center gap-2">
            <HelpCircle class="w-4 h-4" />
            帮助中心
          </span>
          <ChevronRight class="w-4 h-4" />
        </Link>

        <div v-for="item in navItems" :key="item.key" class="mb-2">
          <!-- 首页直接跳转 -->
          <Link
              v-if="item.key === 'home'"
              :to="item.path"
              @click="isMenuOpen = false"
              class="block border rounded-xl px-5 py-4"
              style="color: var(--theme-text); border-color: var(--theme-border);"
          >
            <span class="font-semibold text-lg">{{ item.name }}</span>
          </Link>
          <!-- 外部链接项 -->
          <a
              v-else-if="item.isExternal"
              :href="item.externalUrl"
              target="_blank"
              rel="noopener noreferrer"
              @click="isMenuOpen = false"
              class="block border rounded-xl px-5 py-4"
              style="color: var(--theme-text); border-color: var(--theme-border);"
          >
            <span class="font-semibold text-lg">{{ item.name }} ↗</span>
          </a>
          <!-- 有 path 但无子菜单：直接跳转 -->
          <Link
              v-else-if="item.path && item.children.length === 0"
              :to="item.path"
              @click="isMenuOpen = false"
              class="block border rounded-xl px-5 py-4"
              style="color: var(--theme-text); border-color: var(--theme-border);"
          >
            <span class="font-semibold text-lg">{{ item.name }}</span>
          </Link>
          <!-- 有子菜单：点击展开/折叠 -->
          <div v-else class="border rounded-xl overflow-hidden" style="border-color: var(--theme-border);">
            <button
                @click="toggleNav(item.key)"
                class="w-full flex items-center justify-between px-5 py-4"
                :style="{
                  color: 'var(--theme-text)',
                  backgroundColor: activeNavItem === item.key ? 'var(--theme-surface)' : 'transparent'
                }"
            >
              <span class="font-semibold text-lg">{{ item.name }}</span>
              <ChevronDown
                  class="w-4 h-4 transition-transform duration-200"
                  :class="{ 'rotate-180': activeNavItem === item.key }"
              />
            </button>
            <!-- 移动端二级菜单 -->
            <div
                v-if="activeNavItem === item.key"
                class="border-t"
                style="border-color: var(--theme-border); background-color: var(--theme-surface);"
            >
              <template v-for="(child, idx) in item.children" :key="child.name">
                <!-- 子菜单外部链接 -->
                <a
                    v-if="child.isExternal"
                    :href="child.path"
                    target="_blank"
                    rel="noopener noreferrer"
                    @click="isMenuOpen = false"
                    class="flex items-center justify-between px-7 py-3 text-sm"
                    :style="{
                      color: 'var(--theme-text)',
                      borderTop: idx > 0 ? '1px solid var(--theme-border)' : 'none'
                    }"
                >
                  <span>{{ child.name }} ↗</span>
                </a>
                <!-- 子菜单内部路由 -->
                <Link
                    v-else
                    :to="child.path"
                    @click="isMenuOpen = false"
                    class="flex items-center justify-between px-7 py-3 text-sm group"
                    :style="{
                      color: 'var(--theme-text)',
                      borderTop: idx > 0 ? '1px solid var(--theme-border)' : 'none'
                    }"
                >
                  <span>{{ child.name }}</span>
                  <Lock
                      v-if="child.requiresAuth"
                      class="w-3 h-3 opacity-60 group-hover:opacity-100 transition-opacity"
                      style="color: var(--theme-primary);"
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
