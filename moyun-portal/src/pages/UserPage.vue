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
  ThumbsUp,
  Calendar,
  TrendingUp,
  Trophy,
  Lock
} from 'lucide-vue-next';
import { useArticleStore } from '@/stores/article';
import { useUserStore } from '@/stores/user';
import { generateSeo } from '@/utils/seo';
import ArticleCard from '@/components/ArticleCard.vue';
import Breadcrumb from '@/components/Breadcrumb.vue';
import * as userApi from '@/api/user';
import * as growthApi from '@/api/growth';
import { getMyArticles } from '@/api/article';
import type { Article, User as UserType, UserStats, UserGrowthVO, UserBadgeVO, UserStatsVO, CheckinResult } from '@/types/api';
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

// 成长体系数据
const myGrowth = ref<UserGrowthVO | null>(null);
const myStats = ref<UserStatsVO | null>(null);
const myBadges = ref<UserBadgeVO[]>([]);
const checkinResult = ref<CheckinResult | null>(null);
const checkinLoading = ref(false);
const hasCheckedInToday = ref(false);

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

// 成长等级进度（粗略估算：每级 100 成长值）
const levelProgress = computed(() => {
  if (!myGrowth.value) return { percent: 0, current: 0, required: 100 };
  const value = myGrowth.value.growthValue || 0;
  const level = myGrowth.value.level || 1;
  const base = (level - 1) * 100;
  const current = value - base;
  const required = 100;
  return {
    percent: Math.min(100, Math.round((current / required) * 100)),
    current,
    required
  };
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

  // 加载用户文章（仅已发布的，用于个人中心展示）
  try {
    const articlesResp = await getMyArticles({
      pageNum: 1,
      pageSize: 10,
      status: 'published'
    });
    if (articlesResp.code === 200 && articlesResp.data) {
      const page = articlesResp.data as any;
      userArticles.value = page.records || page.list || [];
    }
  } catch (error) {
    console.warn('获取用户文章失败:', error);
  }

  // 并行加载成长体系数据
  try {
    const [growthResp, statsResp, badgesResp] = await Promise.all([
      growthApi.getMyGrowth(),
      growthApi.getMyStats(),
      growthApi.getMyBadges(),
    ]);
    if (growthResp.code === 200 && growthResp.data) {
      myGrowth.value = growthResp.data;
    }
    if (statsResp.code === 200 && statsResp.data) {
      myStats.value = statsResp.data;
      // 通过 checkinStreak > 0 且 lastCheckinDate 推断今日是否已签到
      // 后端 checkin 接口会返回 "今日已签到" 消息，这里首次加载不阻塞
    }
    if (badgesResp.code === 200 && badgesResp.data) {
      myBadges.value = badgesResp.data;
    }
  } catch (error) {
    console.warn('获取成长体系数据失败:', error);
  }
}

// 每日签到
async function handleCheckin() {
  if (checkinLoading.value || hasCheckedInToday.value) return;
  checkinLoading.value = true;
  try {
    const resp = await growthApi.checkin();
    if (resp.code === 200 && resp.data) {
      checkinResult.value = resp.data;
      if (resp.data.success) {
        hasCheckedInToday.value = true;
        // 刷新成长信息
        try {
          const growthResp = await growthApi.getMyGrowth();
          if (growthResp.code === 200 && growthResp.data) {
            myGrowth.value = growthResp.data;
          }
        } catch (e) { /* ignore */ }
      } else {
        // 已签到
        hasCheckedInToday.value = true;
      }
    }
  } catch (error) {
    console.error('签到失败:', error);
    checkinResult.value = { success: false, message: '签到失败，请稍后重试' };
  } finally {
    checkinLoading.value = false;
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
                      <div class="flex items-center gap-3 flex-wrap mb-2">
                        <h1 class="text-2xl sm:text-3xl font-bold" style="color: var(--theme-text);">
                          {{ currentUser.nickname || currentUser.username }}
                        </h1>
                        <!-- 成长等级徽章 -->
                        <span v-if="myGrowth" class="inline-flex items-center gap-1 px-3 py-1 rounded-full text-xs sm:text-sm font-medium" style="background: linear-gradient(135deg, #f59e0b 0%, #ef4444 100%); color: white;">
                          <Star class="w-3 h-3 sm:w-4 sm:h-4" />
                          Lv.{{ myGrowth.level || 1 }} · {{ myGrowth.title || '初出茅庐' }}
                        </span>
                      </div>
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

                    <!-- 签到按钮 -->
                    <div class="flex-shrink-0">
                      <button
                        @click="handleCheckin"
                        :disabled="checkinLoading || hasCheckedInToday"
                        class="flex items-center gap-2 px-4 sm:px-5 py-2 sm:py-2.5 rounded-xl text-sm font-medium transition-all"
                        :style="hasCheckedInToday
                          ? 'background-color: var(--theme-accent); color: var(--theme-text-secondary); cursor: not-allowed;'
                          : 'background-color: var(--theme-primary); color: white;' "
                      >
                        <Calendar class="w-4 h-4" />
                        {{ hasCheckedInToday ? '今日已签' : (checkinLoading ? '签到中...' : '每日签到') }}
                      </button>
                      <p v-if="checkinResult" class="mt-2 text-xs text-right" :style="{ color: checkinResult.success ? '#10b981' : 'var(--theme-text-secondary)' }">
                        {{ checkinResult.message }}
                        <span v-if="checkinResult.growth">+{{ checkinResult.growth }}</span>
                      </p>
                    </div>
                  </div>

                  <!-- 成长值进度条 -->
                  <div v-if="myGrowth" class="mt-4">
                    <div class="flex items-center justify-between text-xs sm:text-sm mb-1">
                      <span style="color: var(--theme-text-secondary);">
                        成长值 {{ myGrowth.growthValue || 0 }}
                        <span v-if="myGrowth.seasonRank" class="ml-2">· 本季第 {{ myGrowth.seasonRank }} 名</span>
                      </span>
                      <span v-if="myGrowth.nextLevelTitle" style="color: var(--theme-text-secondary);">
                        下一级：{{ myGrowth.nextLevelTitle }}
                      </span>
                    </div>
                    <div class="w-full h-2 rounded-full overflow-hidden" style="background-color: var(--theme-accent);">
                      <div
                        class="h-full rounded-full transition-all"
                        style="background: linear-gradient(90deg, #f59e0b 0%, #ef4444 100%);"
                        :style="{ width: levelProgress.percent + '%' }"
                      ></div>
                    </div>
                  </div>
                </div>
              </div>

              <!-- 统计数据 - 扩展为更全面的成长统计 -->
              <div class="mt-8 grid grid-cols-3 sm:grid-cols-4 lg:grid-cols-6 gap-4">
                <div class="text-center p-4 rounded-xl" style="background-color: var(--theme-accent);">
                  <div class="text-xl sm:text-2xl font-bold mb-1" style="color: var(--theme-text);">{{ myStats?.articles ?? userStats?.articles ?? 0 }}</div>
                  <div class="text-xs sm:text-sm" style="color: var(--theme-text-secondary);">文章</div>
                </div>
                <div class="text-center p-4 rounded-xl" style="background-color: var(--theme-accent);">
                  <div class="text-xl sm:text-2xl font-bold mb-1" style="color: var(--theme-text);">{{ myStats?.views ?? userStats?.views ?? 0 }}</div>
                  <div class="text-xs sm:text-sm" style="color: var(--theme-text-secondary);">浏览</div>
                </div>
                <div class="text-center p-4 rounded-xl" style="background-color: var(--theme-accent);">
                  <div class="text-xl sm:text-2xl font-bold mb-1" style="color: var(--theme-text);">{{ myStats?.totalLikes ?? userStats?.likes ?? 0 }}</div>
                  <div class="text-xs sm:text-sm" style="color: var(--theme-text-secondary);">获赞</div>
                </div>
                <div class="text-center p-4 rounded-xl" style="background-color: var(--theme-accent);">
                  <div class="text-xl sm:text-2xl font-bold mb-1" style="color: var(--theme-text);">{{ myStats?.following ?? userStats?.following ?? 0 }}</div>
                  <div class="text-xs sm:text-sm" style="color: var(--theme-text-secondary);">关注</div>
                </div>
                <div class="text-center p-4 rounded-xl" style="background-color: var(--theme-accent);">
                  <div class="text-xl sm:text-2xl font-bold mb-1" style="color: var(--theme-text);">{{ myStats?.followers ?? userStats?.followers ?? 0 }}</div>
                  <div class="text-xs sm:text-sm" style="color: var(--theme-text-secondary);">粉丝</div>
                </div>
                <div class="text-center p-4 rounded-xl" style="background-color: var(--theme-accent);">
                  <div class="text-xl sm:text-2xl font-bold mb-1" style="color: var(--theme-text);">{{ myStats?.bookmarks ?? userStats?.bookmarks ?? 0 }}</div>
                  <div class="text-xs sm:text-sm" style="color: var(--theme-text-secondary);">收藏</div>
                </div>
              </div>

              <!-- 扩展统计：读书/面试/创作 -->
              <div v-if="myStats" class="mt-4 grid grid-cols-2 sm:grid-cols-4 lg:grid-cols-6 gap-4">
                <div class="text-center p-3 rounded-xl" style="background-color: var(--theme-accent);">
                  <div class="text-lg sm:text-xl font-bold mb-1" style="color: var(--theme-text);">{{ myStats.wordCount || 0 }}</div>
                  <div class="text-xs" style="color: var(--theme-text-secondary);">创作字数</div>
                </div>
                <div class="text-center p-3 rounded-xl" style="background-color: var(--theme-accent);">
                  <div class="text-lg sm:text-xl font-bold mb-1" style="color: var(--theme-text);">{{ myStats.bookFinished || 0 }}</div>
                  <div class="text-xs" style="color: var(--theme-text-secondary);">读完的书</div>
                </div>
                <div class="text-center p-3 rounded-xl" style="background-color: var(--theme-accent);">
                  <div class="text-lg sm:text-xl font-bold mb-1" style="color: var(--theme-text);">{{ myStats.quoteCount || 0 }}</div>
                  <div class="text-xs" style="color: var(--theme-text-secondary);">金句</div>
                </div>
                <div class="text-center p-3 rounded-xl" style="background-color: var(--theme-accent);">
                  <div class="text-lg sm:text-xl font-bold mb-1" style="color: var(--theme-text);">{{ myStats.questionSolved || 0 }}</div>
                  <div class="text-xs" style="color: var(--theme-text-secondary);">解题数</div>
                </div>
                <div class="text-center p-3 rounded-xl" style="background-color: var(--theme-accent);">
                  <div class="text-lg sm:text-xl font-bold mb-1" style="color: var(--theme-text);">{{ myStats.noteCount || 0 }}</div>
                  <div class="text-xs" style="color: var(--theme-text-secondary);">笔记数</div>
                </div>
                <div class="text-center p-3 rounded-xl" style="background-color: var(--theme-accent);">
                  <div class="text-lg sm:text-xl font-bold mb-1" style="color: var(--theme-text);">{{ myStats.checkinStreak || 0 }}</div>
                  <div class="text-xs" style="color: var(--theme-text-secondary);">连续签到</div>
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

              <!-- 成就徽章 - 使用真实数据 -->
              <div class="mt-6">
                <div class="flex items-center justify-between mb-4">
                  <h3 class="text-base font-semibold" style="color: var(--theme-text);">我的徽章</h3>
                  <Link to="/achievements" class="text-xs sm:text-sm flex items-center gap-1" style="color: var(--theme-primary);">
                    查看全部
                    <ChevronRight class="w-3 h-3 sm:w-4 sm:h-4" />
                  </Link>
                </div>
                <div v-if="myBadges.length > 0" class="flex gap-3 overflow-x-auto pb-2">
                  <div v-for="badge in myBadges" :key="badge.id" class="text-center p-3 rounded-xl flex-shrink-0 w-32" style="background-color: var(--theme-accent); border: 1px solid var(--theme-border);">
                    <div class="w-10 h-10 mx-auto mb-2 rounded-xl flex items-center justify-center" style="background: linear-gradient(135deg, #f59e0b 0%, #ef4444 100%);">
                      <img
                        v-if="badge.icon"
                        :src="badge.icon"
                        :alt="badge.name"
                        class="w-6 h-6 object-contain"
                        @error="(e: Event) => (e.target as HTMLImageElement).style.display = 'none'"
                      />
                      <Award v-else class="w-6 h-6 text-white" />
                    </div>
                    <p class="text-sm font-medium mb-1 truncate" style="color: var(--theme-text);">{{ badge.name }}</p>
                    <p class="text-xs line-clamp-2" style="color: var(--theme-text-secondary);">{{ badge.description || '' }}</p>
                  </div>
                </div>
                <div v-else class="p-6 rounded-xl text-center" style="background-color: var(--theme-accent); border: 1px dashed var(--theme-border);">
                  <Award class="w-10 h-10 mx-auto mb-2" style="color: var(--theme-text-secondary);" />
                  <p class="text-sm mb-3" style="color: var(--theme-text-secondary);">还没有徽章，继续努力解锁成就吧</p>
                  <Link to="/achievements" class="inline-flex items-center gap-1 text-xs sm:text-sm" style="color: var(--theme-primary);">
                    查看成就列表
                    <ChevronRight class="w-3 h-3 sm:w-4 sm:h-4" />
                  </Link>
                </div>
              </div>

              <!-- 快捷入口 -->
              <div class="mt-6 flex flex-wrap gap-3">
                <Link to="/ranking" class="inline-flex items-center gap-2 px-4 py-2 rounded-xl text-sm font-medium transition-colors" style="background-color: var(--theme-accent); color: var(--theme-text);">
                  <Trophy class="w-4 h-4" style="color: #f59e0b;" />
                  成长排行榜
                </Link>
                <Link to="/achievements" class="inline-flex items-center gap-2 px-4 py-2 rounded-xl text-sm font-medium transition-colors" style="background-color: var(--theme-accent); color: var(--theme-text);">
                  <Award class="w-4 h-4" style="color: #8b5cf6;" />
                  成就中心
                </Link>
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
                <div class="flex items-center gap-3">
                  <Link to="/my/articles" class="inline-flex items-center gap-1 px-3 py-2 rounded-xl font-medium hover:opacity-80 transition-colors text-sm" style="color: var(--theme-primary);">
                    查看全部
                    <ChevronRight class="w-4 h-4" />
                  </Link>
                  <Link to="/publish" target="_blank" class="inline-flex items-center gap-2 px-4 sm:px-5 py-2 sm:py-2.5 rounded-xl font-medium hover:opacity-90 transition-colors text-sm" style="background-color: var(--theme-primary); color: white;">
                    <Edit class="w-4 h-4" />
                    写文章
                  </Link>
                </div>
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
