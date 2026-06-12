<script setup lang="ts">
import { ref, computed, onMounted } from 'vue';
import { RouterLink as Link, useRouter } from 'vue-router';
import { useHead } from '@vueuse/head';
import {
  User,
  Edit,
  BookOpen,
  Heart,
  FileText,
  Settings,
  ChevronRight,
  Crown,
  Wallet,
  Users,
  Award,
  CheckCircle2,
  Star,
  Zap,
  Flame,
  Eye,
  ThumbsUp
} from 'lucide-vue-next';
import { useArticleStore } from '@/stores/article';
import { useUserStore } from '@/stores/user';
import { generateSeo } from '@/utils/seo';
import ArticleCard from '@/components/ArticleCard.vue';
import Breadcrumb from '@/components/Breadcrumb.vue';
import * as userApi from '@/api/user';
import type { Article, User as UserType, UserStats } from '@/types/api';
import { getSafeAvatar } from '@/utils/avatar';

const router = useRouter();
const articleStore = useArticleStore();
const userStore = useUserStore();

// 数据
const currentUser = ref<UserType | null>(null);
const userArticles = ref<Article[]>([]);
const userStats = ref<UserStats | null>(null);
const activeTab = ref('articles');
const isLoading = ref(false);
const currentPage = ref(1);
const itemsPerPage = ref(10);

// 成就徽章数据
const badges = ref([
  { name: '创作新星', desc: '发布10篇文章', icon: Star, color: 'from-yellow-400 to-orange-500' },
  { name: '人气作者', desc: '获得100个赞', icon: Flame, color: 'from-red-400 to-pink-500' },
  { name: '连续打卡', desc: '连续7天打卡', icon: CheckCircle2, color: 'from-green-400 to-emerald-500' },
  { name: '活跃用户', desc: '累计登录30天', icon: Zap, color: 'from-blue-400 to-cyan-500' },
  { name: '精品作者', desc: '3篇文章获推荐', icon: Award, color: 'from-purple-400 to-violet-500' },
  { name: '粉丝达人', desc: '拥有100个粉丝', icon: Users, color: 'from-pink-400 to-rose-500' }
]);

// SEO
useHead(
  generateSeo({
    title: '个人中心',
    description: '管理你的个人资料、文章和收藏',
    keywords: ['个人中心', '用户中心', '我的文章'],
    type: 'website'
  })
);

// 分页计算属性
const totalPages = computed(() => Math.ceil(userArticles.value.length / itemsPerPage.value));
const paginatedArticles = computed(() => {
  const start = (currentPage.value - 1) * itemsPerPage.value;
  const end = start + itemsPerPage.value;
  return userArticles.value.slice(start, end);
});

// 加载用户信息和统计
async function loadUserData() {
  if (!userStore.isAuthenticated) {
    return;
  }
  currentUser.value = userStore.user;

  // 尝试调用后端 API 获取用户统计
  try {
    const statsResponse = await userApi.getUserStats();
    if (statsResponse && statsResponse.code === 200 && statsResponse.data) {
      userStats.value = statsResponse.data;
    }
  } catch (error) {
    console.warn('获取用户统计失败，使用默认值');
  }
}

onMounted(async () => {
  // 等待用户状态初始化
  if (!userStore.isUserInitialized) {
    await userStore.initializeUser();
  }
  await loadUserData();
});

// 跳转到编辑资料
function goToProfile() {
  router.push('/user/profile');
}

// 跳转到设置
function goToSettings() {
  router.push('/user/settings');
}
</script>

<template>
  <div class="min-h-screen flex flex-col" style="background-color: var(--theme-bg);">
    <!-- 面包屑 - 与列表页和详情页一致 -->
    <div class="border-b py-3 sm:py-4" style="background-color: var(--theme-bg); border-color: var(--theme-border);">
      <div class="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8">
        <div class="flex items-center justify-between gap-4">
          <Breadcrumb :items="[{ label: '首页', path: '/' }, { label: '个人中心' }]" />
          <div class="flex gap-3">
            <button
              @click="goToProfile"
              class="flex items-center gap-2 px-4 py-2 rounded-xl text-sm font-medium transition-colors"
              style="background-color: var(--theme-surface); border: 1px solid var(--theme-border); color: var(--theme-text);"
            >
              <Edit class="w-4 h-4" />
              编辑资料
            </button>
            <button
              @click="goToSettings"
              class="flex items-center gap-2 px-4 py-2 rounded-xl text-sm font-medium transition-colors"
              style="background-color: var(--theme-surface); border: 1px solid var(--theme-border); color: var(--theme-text);"
            >
              <Settings class="w-4 h-4" />
              设置
            </button>
          </div>
        </div>
      </div>
    </div>

    <!-- 主内容区域 -->
    <div class="py-8 flex-1">
      <div class="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8">
        <!-- 加载状态 -->
        <div v-if="!currentUser" class="text-center py-12">
          <div class="inline-block w-12 h-12 border-4 border-t-4 border-gray-300 rounded-full animate-spin" style="border-top-color: var(--theme-primary);"></div>
          <p class="mt-4" style="color: var(--theme-text-secondary);">加载中...</p>
        </div>

        <template v-else>
          <!-- 用户头部信息 -->
          <div class="mb-8">
            <div class="rounded-2xl p-6 sm:p-8" style="background-color: var(--theme-surface); border: 1px solid var(--theme-border);">
              <div class="flex flex-col lg:flex-row gap-6 items-start">
                <!-- 头像 -->
                <div class="flex-shrink-0 relative">
                  <img
                    :src="getSafeAvatar(currentUser.avatar, currentUser.id)"
                    :alt="currentUser.nickname || currentUser.username"
                    class="w-24 h-24 sm:w-32 sm:h-32 rounded-2xl object-cover"
                    @error="(e: Event) => (e.target as HTMLImageElement).src = getSafeAvatar(null, currentUser.id)"
                  />
                </div>

                <!-- 用户信息 -->
                <div class="flex-1 min-w-0">
                  <div class="flex items-start justify-between gap-4">
                    <div class="min-w-0 flex-1">
                      <h1 class="text-2xl sm:text-3xl font-bold mb-2" style="color: var(--theme-text);">
                        {{ currentUser.nickname || currentUser.username }}
                      </h1>
                      <p class="text-sm sm:text-base mb-2" style="color: var(--theme-text-secondary);">
                        {{ currentUser.position || '暂无职位' }}
                      </p>
                      <p class="text-sm sm:text-base mb-3" style="color: var(--theme-text-secondary);">
                        {{ currentUser.bio || '这个人很懒，什么都没写~' }}
                      </p>
                      <div class="flex items-center gap-3 flex-wrap">
                        <span
                          v-if="currentUser.email"
                          class="text-xs sm:text-sm px-3 py-1 rounded-full"
                          style="background-color: var(--theme-accent); color: var(--theme-text-secondary);"
                        >
                          {{ currentUser.email }}
                        </span>
                        <span
                          v-if="currentUser.location"
                          class="text-xs sm:text-sm px-3 py-1 rounded-full"
                          style="background-color: var(--theme-accent); color: var(--theme-text-secondary);"
                        >
                          {{ currentUser.location }}
                        </span>
                      </div>
                    </div>
                  </div>
                </div>
              </div>

              <!-- 统计数据 -->
              <div class="mt-8 grid grid-cols-3 sm:grid-cols-4 lg:grid-cols-6 gap-4">
                <div class="text-center p-4 rounded-xl" style="background-color: var(--theme-accent);">
                  <div class="text-xl sm:text-2xl font-bold mb-1" style="color: var(--theme-text);">{{ userStats?.articles || 0 }}</div>
                  <div class="text-xs sm:text-sm" style="color: var(--theme-text-secondary);">文章</div>
                </div>
                <div class="text-center p-4 rounded-xl" style="background-color: var(--theme-accent);">
                  <div class="text-xl sm:text-2xl font-bold mb-1" style="color: var(--theme-text);">{{ userStats?.views || 0 }}</div>
                  <div class="text-xs sm:text-sm" style="color: var(--theme-text-secondary);">浏览</div>
                </div>
                <div class="text-center p-4 rounded-xl" style="background-color: var(--theme-accent);">
                  <div class="text-xl sm:text-2xl font-bold mb-1" style="color: var(--theme-text);">{{ userStats?.likes || 0 }}</div>
                  <div class="text-xs sm:text-sm" style="color: var(--theme-text-secondary);">获赞</div>
                </div>
                <div class="text-center p-4 rounded-xl" style="background-color: var(--theme-accent);">
                  <div class="text-xl sm:text-2xl font-bold mb-1" style="color: var(--theme-text);">{{ userStats?.following || 0 }}</div>
                  <div class="text-xs sm:text-sm" style="color: var(--theme-text-secondary);">关注</div>
                </div>
                <div class="text-center p-4 rounded-xl" style="background-color: var(--theme-accent);">
                  <div class="text-xl sm:text-2xl font-bold mb-1" style="color: var(--theme-text);">{{ userStats?.followers || 0 }}</div>
                  <div class="text-xs sm:text-sm" style="color: var(--theme-text-secondary);">粉丝</div>
                </div>
                <div class="text-center p-4 rounded-xl" style="background-color: var(--theme-accent);">
                  <div class="text-xl sm:text-2xl font-bold mb-1" style="color: var(--theme-text);">{{ userStats?.bookmarks || 0 }}</div>
                  <div class="text-xs sm:text-sm" style="color: var(--theme-text-secondary);">收藏</div>
                </div>
              </div>

              <!-- 会员信息 -->
              <div v-if="currentUser.role === 'vip'" class="mt-6 p-4 rounded-xl" style="background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);">
                <div class="flex items-center gap-3">
                  <Crown class="w-8 h-8 text-yellow-300" />
                  <div class="flex-1 min-w-0">
                    <h3 class="font-semibold text-white">VIP会员</h3>
                    <p class="text-xs text-white/80">到期时间：{{ currentUser.vipExpireAt || '永久' }}</p>
                  </div>
                </div>
              </div>

              <!-- 成就徽章 -->
              <div class="mt-6">
                <h3 class="text-base font-semibold mb-4" style="color: var(--theme-text);">我的徽章</h3>
                <div class="flex gap-3 overflow-x-auto pb-2">
                  <div v-for="badge in badges" :key="badge.name" class="text-center p-3 rounded-xl flex-shrink-0 w-32" style="background-color: var(--theme-accent); border: 1px solid var(--theme-border);">
                    <div class="w-10 h-10 mx-auto mb-2 rounded-xl flex items-center justify-center" :style="{ background: `linear-gradient(135deg, var(--badge-color))` }">
                      <component :is="badge.icon" class="w-6 h-6 text-white" />
                    </div>
                    <p class="text-sm font-medium mb-1" style="color: var(--theme-text);">{{ badge.name }}</p>
                    <p class="text-xs" style="color: var(--theme-text-secondary);">{{ badge.desc }}</p>
                  </div>
                </div>
              </div>
            </div>
          </div>

          <!-- 标签导航 -->
          <div class="mb-6 border-b" style="border-color: var(--theme-border);">
            <nav class="flex gap-1 overflow-x-auto pb-0">
              <button
                v-for="tab in [
                  { id: 'articles', label: '我的文章', icon: BookOpen },
                  { id: 'saved', label: '我的收藏', icon: Heart }
                ]"
                :key="tab.id"
                @click="activeTab = tab.id"
                class="flex items-center gap-2 px-4 sm:px-6 py-3 sm:py-4 text-sm sm:text-base font-medium border-b-2 transition-colors whitespace-nowrap"
                :style="activeTab === tab.id
                  ? 'border-color: var(--theme-primary); color: var(--theme-primary);'
                  : 'border-color: transparent; color: var(--theme-text-secondary);'"
              >
                <component :is="tab.icon" class="w-4 h-4 sm:w-5 sm:h-5" />
                {{ tab.label }}
              </button>
            </nav>
          </div>

          <!-- 标签内容 -->
          <div class="min-h-[500px]">
            <!-- 我的文章 -->
            <div v-if="activeTab === 'articles'">
              <div class="flex items-center justify-between mb-6">
                <h2 class="text-xl sm:text-2xl font-bold" style="color: var(--theme-text);">我的文章</h2>
                <Link to="/publish" target="_blank" class="inline-flex items-center gap-2 px-4 sm:px-5 py-2 sm:py-2.5 rounded-xl font-medium hover:opacity-90 transition-colors text-sm" style="background-color: var(--theme-primary); color: white;">
                  <Edit class="w-4 h-4" />
                  写文章
                </Link>
              </div>

              <div v-if="userArticles.length > 0" class="space-y-4 sm:space-y-6">
                <ArticleCard v-for="article in paginatedArticles" :key="article.id" :article="article" />
              </div>
              <div v-else class="p-8 sm:p-12 rounded-2xl text-center" style="background-color: var(--theme-surface); border: 1px solid var(--theme-border);">
                <BookOpen class="w-12 h-12 sm:w-16 sm:h-16 mx-auto mb-4" style="color: var(--theme-text-secondary);" />
                <h3 class="text-lg font-medium mb-2" style="color: var(--theme-text);">还没有文章</h3>
                <p class="mb-6" style="color: var(--theme-text-secondary);">开始创作你的第一篇文章吧</p>
                <Link to="/publish" target="_blank" class="inline-flex items-center gap-2 px-5 py-2.5 rounded-xl font-medium hover:opacity-90 transition-colors text-sm" style="background-color: var(--theme-primary); color: white;">
                  开始创作
                  <ChevronRight class="w-4 h-4" />
                </Link>
              </div>

              <!-- 分页 -->
              <div v-if="userArticles.length > itemsPerPage" class="mt-8">
                <!-- 这里可以后续添加分页组件 -->
              </div>
            </div>

            <!-- 我的收藏 -->
            <div v-else-if="activeTab === 'saved'">
              <h2 class="text-xl sm:text-2xl font-bold mb-6" style="color: var(--theme-text);">我的收藏</h2>

              <div v-if="articleStore.bookmarkedArticles && articleStore.bookmarkedArticles.length > 0" class="space-y-4 sm:space-y-6">
                <ArticleCard v-for="article in articleStore.bookmarkedArticles" :key="article.id" :article="article" />
              </div>
              <div v-else class="p-8 sm:p-12 rounded-2xl text-center" style="background-color: var(--theme-surface); border: 1px solid var(--theme-border);">
                <Heart class="w-12 h-12 sm:w-16 sm:h-16 mx-auto mb-4" style="color: var(--theme-text-secondary);" />
                <h3 class="text-lg font-medium mb-2" style="color: var(--theme-text);">收藏夹为空</h3>
                <p style="color: var(--theme-text-secondary);">浏览文章并收藏你喜欢的内容</p>
              </div>
            </div>
          </div>
        </template>
      </div>
    </div>
  </div>
</template>
