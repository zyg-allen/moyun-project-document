<script setup lang="ts">
import { computed } from 'vue';
import { useRoute, useRouter } from 'vue-router';
import { useUserStore } from '@/stores/user';
import { useAuth } from '@/composables/useAuth';

interface TabItem {
  key: string;
  label: string;
  /** 跳转路径 */
  path: string;
  /** 命中当前路由的前缀（startsWith 判断高亮） */
  matchPrefix: string[];
  /** 是否需要凸起样式（创作 Tab 中央凸起） */
  raised?: boolean;
}

const route = useRoute();
const router = useRouter();
const userStore = useUserStore();
const { requireAuth } = useAuth();

const tabs: TabItem[] = [
  {
    key: 'home',
    label: '首页',
    path: '/',
    matchPrefix: ['/'],
  },
  {
    key: 'reading',
    label: '读书',
    path: '/reading',
    matchPrefix: ['/reading'],
  },
  {
    key: 'publish',
    label: '创作',
    path: '/publish',
    matchPrefix: ['/publish', '/my/articles'],
    raised: true,
  },
  {
    key: 'learn',
    label: '学习',
    path: '/learn',
    matchPrefix: ['/learn', '/interview'],
  },
  {
    key: 'user',
    label: '我的',
    path: '/user',
    matchPrefix: ['/user'],
  },
];

// 当前路由高亮判断（首页仅精确匹配，其他用 startsWith）
function isActive(tab: TabItem): boolean {
  if (tab.key === 'home') {
    return route.path === '/' || route.path === '';
  }
  return tab.matchPrefix.some((p) => route.path.startsWith(p));
}

const activeKey = computed(() => {
  const found = tabs.find((t) => isActive(t));
  return found?.key ?? '';
});

function handleTabClick(tab: TabItem) {
  // 创作与"我的"需要登录
  if (tab.key === 'publish' || tab.key === 'user') {
    if (!userStore.isAuthenticated) {
      if (!requireAuth(tab.path)) return;
    }
  }
  // 已在当前页则不重复跳转
  if (isActive(tab) && route.path === tab.path) return;
  router.push(tab.path);
}
</script>

<template>
  <nav
    class="md:hidden fixed bottom-0 left-0 right-0 z-40 flex items-stretch justify-around border-t"
    style="background-color: var(--theme-bg); border-color: var(--theme-border); padding-bottom: env(safe-area-inset-bottom);"
    aria-label="底部导航"
  >
    <button
      v-for="tab in tabs"
      :key="tab.key"
      type="button"
      @click="handleTabClick(tab)"
      class="relative flex-1 flex flex-col items-center justify-center pt-1.5 pb-1 transition-colors"
      :style="tab.raised ? 'margin-top: -18px;' : ''"
    >
      <!-- 凸起 Tab：圆形背景 -->
      <span
        v-if="tab.raised"
        class="flex items-center justify-center w-12 h-12 rounded-full shadow-lg mb-0.5"
        :style="activeKey === tab.key
          ? 'background-color: var(--theme-primary); color: white;'
          : 'background: linear-gradient(135deg, var(--theme-primary) 0%, var(--theme-accent) 100%); color: white;'"
      >
        <!-- pen 图标 -->
        <svg xmlns="http://www.w3.org/2000/svg" width="24" height="24" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
          <path d="M12 20h9" />
          <path d="M16.5 3.5a2.121 2.121 0 0 1 3 3L7 19l-4 1 1-4Z" />
        </svg>
      </span>

      <!-- 普通图标 -->
      <span v-else class="mb-0.5" :style="{ color: activeKey === tab.key ? 'var(--theme-primary)' : 'var(--theme-text-secondary)' }">
        <!-- 首页 house -->
        <svg v-if="tab.key === 'home'" xmlns="http://www.w3.org/2000/svg" width="24" height="24" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
          <path d="m3 9 9-7 9 7v11a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2z" />
          <polyline points="9 22 9 12 15 12 15 22" />
        </svg>
        <!-- 读书 book -->
        <svg v-else-if="tab.key === 'reading'" xmlns="http://www.w3.org/2000/svg" width="24" height="24" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
          <path d="M4 19.5v-15A2.5 2.5 0 0 1 6.5 2H20v20H6.5a2.5 2.5 0 0 1 0-5H20" />
        </svg>
        <!-- 学习 graduation-cap -->
        <svg v-else-if="tab.key === 'learn'" xmlns="http://www.w3.org/2000/svg" width="24" height="24" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
          <path d="M22 10v6M2 10l10-5 10 5-10 5z" />
          <path d="M6 12v5c3 3 9 3 12 0v-5" />
        </svg>
        <!-- 我的 user -->
        <svg v-else-if="tab.key === 'user'" xmlns="http://www.w3.org/2000/svg" width="24" height="24" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
          <path d="M19 21v-2a4 4 0 0 0-4-4H9a4 4 0 0 0-4 4v2" />
          <circle cx="12" cy="7" r="4" />
        </svg>
      </span>

      <span
        v-if="!tab.raised"
        class="text-xs font-medium"
        :style="{ color: activeKey === tab.key ? 'var(--theme-primary)' : 'var(--theme-text-secondary)' }"
      >
        {{ tab.label }}
      </span>
    </button>
  </nav>
</template>
