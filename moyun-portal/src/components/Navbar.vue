<script setup lang="ts">
import { ref, computed, onMounted } from 'vue';
import { RouterLink as Link, useRouter, useRoute } from 'vue-router';
import {
  Search, User, Plus, LogOut, Menu, X, Palette, Sun, Moon, Eye, Home,
  ChevronDown, ChevronUp, Settings, UserCircle
} from 'lucide-vue-next';
import { getStoredTheme, setTheme, getCurrentTheme, type Theme, themes } from '@/utils/theme';
import { useUserStore } from '@/stores/user';
import { getSafeAvatar } from '@/utils/avatar';
import { useAuth } from '@/composables/useAuth';
import NotificationBell from './NotificationBell.vue';
import * as notificationApi from '@/api/notification';
import * as categoryApi from '@/api/category';
import type { Category } from '@/types/api';

const router = useRouter();
const route = useRoute();
const userStore = useUserStore();
const { requireAuth, isAuthenticated } = useAuth();

const isMenuOpen = ref(false);
const searchQuery = ref('');
const currentTheme = ref<Theme>(getCurrentTheme());
const isThemeMenuOpen = ref(false);
const activeNavItem = ref<string | null>(null);
const notifications = ref<any[]>([]);
const notificationsLoading = ref(false);
const isUserMenuOpen = ref(false);

const categories = ref<Category[]>([]);
const isSpecialPageName = (name: string) => ['读书空间', '面试指南'].includes(name);

// 导航数据结构 - 使用统一的分类数据（过滤特殊页面）
// 路径统一使用语义化格式 /category/:name
const navItems = computed(() => [
  {
    name: '名家录',
    key: 'authors',
    children: []
  },
  ...categories.value
    .filter(cat => !isSpecialPageName(cat.name))
    .map(cat => ({
      name: cat.name,
      key: cat.id,
      children: (cat.children || []).map(sub => ({
        name: sub.name,
        path: `/category/${encodeURIComponent(sub.name)}`
      }))
    })),
  {
    name: '读书空间',
    key: 'reading',
    children: [
      { name: '书单推荐', path: '/reading' },
      { name: '书籍详情', path: '/reading/books' },
    ]
  },
  {
    name: '面试指南',
    key: 'interview',
    children: [
      { name: '面试题库', path: '/interview' },
      { name: '面试经验', path: '/interview/experiences' },
    ]
  },
  {
    name: '帮助中心',
    key: 'help',
    children: [
      { name: '常见问题', path: '/help' },
      { name: '关于我们', path: '/about' },
      { name: '用户协议', path: '/agreement' },
      { name: '举报反馈', path: '/report' }
    ]
  },
]);

const currentUser = computed(() => userStore.user)
const unreadCount = computed(() =>
    notifications.value.filter(n => !n.isRead).length
);

// 模拟通知数据
const mockNotifications = [
  {
    id: '1',
    title: '您的文章被收藏了',
    content: '恭喜！您的文章《听松看云》被用户墨韵收藏了。',
    time: '2026-05-20 09:30',
    isRead: false
  },
  {
    id: '2',
    title: '系统更新通知',
    content: '为了更好的用户体验，我们进行了系统更新，新增了消息提醒功能！',
    time: '2026-05-19 18:00',
    isRead: false
  },
  {
    id: '3',
    title: '社区活动邀请',
    content: '墨韵社区将于本周六举办线上文学沙龙活动，诚邀您的参与。',
    time: '2026-05-18 14:20',
    isRead: true
  },
  {
    id: '4',
    title: '评论回复',
    content: '您的评论收到了用户"清风"的回复。',
    time: '2026-05-17 10:15',
    isRead: true
  },
];

onMounted(async () => {
  currentTheme.value = getCurrentTheme();
  await loadCategories();
  await loadNotifications();
  await userStore.fetchCurrentUser();
});

async function loadCategories() {
  try {
    const response = await categoryApi.getCategoryTree();
    if (response.code === 200) {
      categories.value = response.data || [];
    }
  } catch (error) {
    console.error('加载分类失败:', error);
  }
}

async function loadNotifications() {
  // 直接使用模拟数据，避免API调用失败
  notifications.value = mockNotifications;
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

function markAsRead(id: string) {
  const notification = notifications.value.find(n => n.id === id);
  if (notification) {
    notification.isRead = true;
  }
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

function handlePublish() {
  // 检查是否登录，未登录则跳转到登录页
  if (!requireAuth('/publish')) {
    return;
  }
  router.push('/publish');
}
</script>

<template>
  <header class="sticky top-0 z-50 shadow-md" style="background-color: var(--theme-bg);">
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
            <!-- 搜索图标按钮 -->
            <button
                @click="router.push('/search')"
                class="p-1.5 sm:p-2 rounded-lg transition-colors"
                style="color: var(--theme-text-secondary);"
                title="搜索"
            >
              <Search class="w-4 h-4 sm:w-5 sm:h-5" />
            </button>

            <!-- 消息铃铛 -->
            <NotificationBell
                :notifications="notifications"
                @read="markAsRead"
                @show-detail="(n) => console.log(n)"
            />

            <!-- 主题切换 -->
            <div class="relative">
              <button
                  @click="isThemeMenuOpen = !isThemeMenuOpen"
                  class="p-1.5 rounded-full transition-colors"
                  style="color: var(--theme-text);"
              >
                <Palette class="w-4 h-4" />
              </button>
              <div
                  v-if="isThemeMenuOpen"
                  class="absolute right-0 mt-2 w-40 rounded-lg shadow-lg border py-2 z-50"
                  style="background-color: var(--theme-bg); border-color: var(--theme-border);"
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
                    <LogOut class="w-4 h-4" style="color: #dc2626;" />
                    <span class="text-sm" style="color: #dc2626;">退出登录</span>
                  </button>
                </div>
              </div>
            </template>
            <template v-else>
              <Link
                  to="/login"
                  class="px-2 sm:px-3 py-1.5 font-medium transition-colors flex-shrink-0"
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
            <!-- 首页链接 -->
            <Link
                to="/"
                class="px-5 py-2.5 text-base font-semibold transition-all duration-200 rounded-lg hover:scale-105 hover:shadow-sm"
                style="color: var(--theme-text);"
                active-class=""
            >
              首页
            </Link>
            <template v-for="item in navItems" :key="item.key">
              <!-- 名家录直接跳转 -->
              <template v-if="item.key === 'authors'">
                <Link
                    to="/authors"
                    class="px-5 py-2.5 text-base font-semibold transition-all duration-200 rounded-lg hover:scale-105 hover:shadow-sm"
                    style="color: var(--theme-text);"
                >
                  {{ item.name }}
                </Link>
              </template>
              <!-- 其他有子菜单的项 -->
              <template v-else>
                <div class="relative" @click.stop>
                  <button
                      @click="toggleNav(item.key)"
                      :class="[
                      'px-5 py-2.5 text-base font-semibold transition-all duration-200 rounded-lg',
                      activeNavItem === item.key ? 'shadow-md scale-105' : 'hover:scale-105 hover:shadow-sm'
                    ]"
                      :style="{
                      color: activeNavItem === item.key ? 'white' : 'var(--theme-text)',
                      backgroundColor: activeNavItem === item.key ? 'var(--theme-primary)' : 'transparent'
                    }"
                  >
                    {{ item.name }}
                  </button>
                  <!-- PC端二级菜单 -->
                  <div
                      v-if="activeNavItem === item.key"
                      class="absolute top-full left-0 mt-2 w-72 shadow-xl border rounded-xl py-3 z-50 transform transition-all duration-200"
                      style="background-color: var(--theme-bg); border-color: var(--theme-border);"
                  >
                    <Link
                        v-for="(child, idx) in item.children"
                        :key="child.name"
                        :to="child.path"
                        @click="activeNavItem = null"
                        class="block px-5 py-3 text-base hover:scale-105 transition-all duration-150"
                        :style="{
                        color: 'var(--theme-text)',
                        borderTop: idx > 0 ? '1px solid var(--theme-border)' : 'none'
                      }"
                    >
                      {{ child.name }}
                    </Link>
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
        class="lg:hidden border-t"
        style="background-color: var(--theme-bg); border-color: var(--theme-border);"
    >
      <div class="px-4 py-3 space-y-2">
        <!-- 移动端首页链接 -->
        <Link
            to="/"
            @click="isMenuOpen = false"
            class="block border rounded-xl px-5 py-4 mb-2"
            style="color: var(--theme-text); border-color: var(--theme-border);"
        >
          <span class="font-semibold text-lg">首页</span>
        </Link>
        <div v-for="item in navItems" :key="item.key" class="mb-2">
          <Link
            :to="item.key === 'authors' ? '/authors' : `/category/${encodeURIComponent(item.name)}`"
            @click="isMenuOpen = false"
            class="block border rounded-xl px-5 py-4"
            style="color: var(--theme-text); border-color: var(--theme-border);"
          >
            <span class="font-semibold text-lg">{{ item.name }}</span>
          </Link>
        </div>
        <!-- 更多按钮 -->
        <Link
            to="/category"
            @click="isMenuOpen = false"
            class="block border rounded-xl px-5 py-4 text-center"
            style="color: var(--theme-text); border-color: var(--theme-border); background-color: var(--theme-surface);"
        >
          <span class="font-semibold text-lg">查看更多</span>
        </Link>
      </div>
    </div>
  </header>
</template>

<style scoped>
</style>
