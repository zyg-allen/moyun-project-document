<script setup lang="ts">
import { ref, reactive, computed, onMounted } from 'vue';
import { RouterLink as Link, useRouter } from 'vue-router';
import { useHead } from '@vueuse/head';
import {
  Edit,
  BookOpen,
  Heart,
  Settings,
  ChevronRight,
  Crown,
  Award,
  Star,
  Calendar,
  Trophy,
  Clock,
  BookMarked,
  GraduationCap,
  Briefcase,
  Users,
  FileText,
  MessageSquare,
  Flag,
  Receipt,
  AlertCircle,
  CheckCircle2,
} from 'lucide-vue-next';
import { useUserStore } from '@/stores/user';
import { generateSeo } from '@/utils/seo';
import ArticleCard from '@/components/ArticleCard.vue';
import Breadcrumb from '@/components/Breadcrumb.vue';
import * as userApi from '@/api/user';
import * as growthApi from '@/api/growth';
import * as followApi from '@/api/follow';
import { getMyArticles, getMyBookmarks } from '@/api/article';
import {
  getMyBookmarkList as getMyQuestionBookmarks,
  getMySubmissionList,
  getMyExperienceList,
  getMyResumeList,
} from '@/api/interview';
import { getMyBookshelf } from '@/api/reading';
import type {
  Article,
  User as UserType,
  UserStats,
  UserGrowthVO,
  UserBadgeVO,
  UserStatsVO,
  CheckinResult,
  UserDashboard,
  InterviewQuestionVO,
  InterviewSubmissionVO,
  InterviewExperienceVO,
  UserResumeVO,
  AchievementVO,
  BookshelfItem,
  FollowUserItem,
} from '@/types/api';
import { getSafeAvatar } from '@/utils/avatar';

const router = useRouter();
const userStore = useUserStore();

// ============ 基础数据 ============
const currentUser = ref<UserType | null>(null);
const userStats = ref<UserStats | null>(null);
const activeTab = ref('articles');
const isLoading = ref(false);
const pendingCount = ref(0);

// Dashboard 聚合数据（顶部数据卡片 + Tab 角标）
const dashboard = ref<UserDashboard | null>(null);

// 成长体系数据（用于头部等级展示与进度条）
const myGrowth = ref<UserGrowthVO | null>(null);
const myStats = ref<UserStatsVO | null>(null);
const myBadges = ref<UserBadgeVO[]>([]);
const myAchievements = ref<AchievementVO[]>([]);
const checkinResult = ref<CheckinResult | null>(null);
const checkinLoading = ref(false);
const hasCheckedInToday = ref(false);

// ============ 各 Tab 数据（懒加载） ============
// 文章 Tab
const userArticles = ref<Article[]>([]);
const currentPage = ref(1);
const itemsPerPage = ref(10);

// 收藏 Tab - 文章收藏（已有逻辑复用）
const bookmarkedArticles = ref<Article[]>([]);
// 收藏 Tab - 题目收藏
const bookmarkedQuestions = ref<InterviewQuestionVO[]>([]);

// 阅读 Tab
const bookshelfItems = ref<BookshelfItem[]>([]);
const bookshelfTotal = ref(0);

// 学习 Tab
const mySubmissions = ref<InterviewSubmissionVO[]>([]);

// 面试 Tab
const myExperiences = ref<InterviewExperienceVO[]>([]);
const myResumes = ref<UserResumeVO[]>([]);

// 关注 Tab
const followingList = ref<FollowUserItem[]>([]);
const followersList = ref<FollowUserItem[]>([]);

// 子 Tab 状态
const savedSubTab = ref<'articles' | 'questions' | 'booklists' | 'quotes'>('articles');
const followSubTab = ref<'following' | 'followers'>('following');

// 已加载标记（懒加载：仅首次切换到某 Tab 时请求）
const tabLoaded = reactive<Record<string, boolean>>({});

// SEO
useHead(
  generateSeo({
    title: '个人中心',
    description: '管理你的个人资料、文章、收藏、阅读、学习与成就',
    keywords: ['个人中心', '用户中心', '我的文章', '我的收藏'],
    type: 'website'
  })
);

// ============ Tab 配置 ============
interface TabConfig {
  id: string;
  label: string;
  icon: typeof BookOpen;
  /** dashboard 中对应的计数字段；为 null 则不显示角标 */
  countKey: keyof UserDashboard | null;
}

const tabs: TabConfig[] = [
  { id: 'articles', label: '文章', icon: BookOpen, countKey: 'articles' },
  { id: 'saved', label: '收藏', icon: Heart, countKey: 'bookmarks' },
  { id: 'reading', label: '阅读', icon: BookMarked, countKey: 'bookshelf' },
  { id: 'learn', label: '学习', icon: GraduationCap, countKey: 'questions' },
  { id: 'interview', label: '面试', icon: Briefcase, countKey: 'experiences' },
  { id: 'follow', label: '关注', icon: Users, countKey: 'following' },
  { id: 'achievements', label: '成就', icon: Award, countKey: null },
  { id: 'account', label: '账号', icon: Settings, countKey: null },
];

const savedSubTabs = [
  { id: 'articles' as const, label: '文章收藏' },
  { id: 'questions' as const, label: '题目收藏' },
  { id: 'booklists' as const, label: '书单收藏' },
  { id: 'quotes' as const, label: '金句收藏' },
];

const followSubTabs = [
  { id: 'following' as const, label: '我关注的人' },
  { id: 'followers' as const, label: '我的粉丝' },
];

// ============ 计算属性 ============
function tabCount(tab: TabConfig): number | null {
  if (!tab.countKey || !dashboard.value) return null;
  return (dashboard.value[tab.countKey] as number) ?? 0;
}

const totalPages = computed(() => Math.ceil(userArticles.value.length / itemsPerPage.value));
const paginatedArticles = computed(() => {
  const start = (currentPage.value - 1) * itemsPerPage.value;
  const end = start + itemsPerPage.value;
  return userArticles.value.slice(start, end);
});

// 成长等级进度（粗略估算：每级 100 成长值）
const levelProgress = computed(() => {
  const growth = myGrowth.value;
  if (!growth) {
    // 退化为 dashboard 数据
    if (dashboard.value) {
      const value = dashboard.value.growthValue || 0;
      const level = dashboard.value.growthLevel || 1;
      const base = (level - 1) * 100;
      return {
        percent: Math.min(100, Math.round(((value - base) / 100) * 100)),
        current: value - base,
        required: 100
      };
    }
    return { percent: 0, current: 0, required: 100 };
  }
  const value = growth.growthValue || 0;
  const level = growth.level || 1;
  const base = (level - 1) * 100;
  const current = value - base;
  const required = 100;
  return {
    percent: Math.min(100, Math.round((current / required) * 100)),
    current,
    required
  };
});

// ============ 数据加载 ============
async function loadDashboard() {
  try {
    const resp = await userApi.getMyDashboard();
    if (resp && resp.code === 200 && resp.data) {
      dashboard.value = resp.data;
    }
  } catch (error) {
    console.warn('获取 Dashboard 数据失败，使用默认值');
  }
}

async function loadUserData() {
  if (!userStore.isAuthenticated) {
    return;
  }
  currentUser.value = userStore.user;

  // 旧版用户统计（兼容，部分头部展示仍可能用到）
  try {
    const statsResponse = await userApi.getUserStats();
    if (statsResponse && statsResponse.code === 200 && statsResponse.data) {
      userStats.value = statsResponse.data;
    }
  } catch (error) {
    console.warn('获取用户统计失败，使用默认值');
  }

  // Dashboard 聚合数据（顶部数据卡片 + Tab 角标）
  await loadDashboard();

  // 并行加载成长体系数据（头部等级 + 进度条 + 徽章）
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
      // 根据后端返回的最后签到日期恢复"今日已签到"状态（刷新后不丢失）
      const lastDate = statsResp.data.lastCheckinDate;
      if (lastDate) {
        const today = new Date().toISOString().slice(0, 10); // YYYY-MM-DD
        hasCheckedInToday.value = lastDate === today;
      }
    }
    if (badgesResp.code === 200 && badgesResp.data) {
      myBadges.value = badgesResp.data;
    }
  } catch (error) {
    console.warn('获取成长体系数据失败:', error);
  }

  // 默认 Tab（文章）数据加载
  await loadTabData('articles');
}

// 各 Tab 懒加载入口
async function loadTabData(tabId: string) {
  if (tabLoaded[tabId]) return;
  tabLoaded[tabId] = true;
  try {
    switch (tabId) {
      case 'articles':
        await loadArticlesTab();
        break;
      case 'saved':
        await loadSavedSubTab(savedSubTab.value);
        break;
      case 'reading':
        await loadReadingTab();
        break;
      case 'learn':
        await loadLearnTab();
        break;
      case 'interview':
        await loadInterviewTab();
        break;
      case 'follow':
        await loadFollowSubTab(followSubTab.value);
        break;
      case 'achievements':
        await loadAchievementsTab();
        break;
      // account Tab 无需异步数据
      default:
        break;
    }
  } catch (error) {
    // 失败则允许下次重试
    tabLoaded[tabId] = false;
    console.warn(`加载 Tab [${tabId}] 数据失败:`, error);
  }
}

// ---- 文章 Tab ----
// 后端 /portal/article/my 可能返回两种格式：
//   1) MyBatis-Plus Page：{ records: [], total }
//   2) RuoYi TableDataInfo：{ rows: [], total }
// 这里统一适配两种结构，避免因后端切换导致列表为空
function extractArticlePage(data: any): { list: Article[]; total: number } {
  if (!data) return { list: [], total: 0 };
  const list = data.records || data.rows || data.list || [];
  const total = data.total ?? 0;
  return { list, total };
}

async function loadArticlesTab() {
  // 已发布文章
  try {
    const articlesResp = await getMyArticles({
      pageNum: 1,
      pageSize: 10,
      status: 'published'
    });
    if (articlesResp.code === 200 && articlesResp.data) {
      const { list } = extractArticlePage(articlesResp.data);
      userArticles.value = list;
    }
  } catch (error) {
    console.warn('获取用户文章失败:', error);
  }

  // 待审核数量
  try {
    const pendingResp = await getMyArticles({
      pageNum: 1,
      pageSize: 1,
      status: 'pending'
    });
    if (pendingResp.code === 200 && pendingResp.data) {
      const { total } = extractArticlePage(pendingResp.data);
      pendingCount.value = total;
    } else {
      pendingCount.value = 0;
    }
  } catch (e) {
    pendingCount.value = 0;
  }
}

// ---- 收藏 Tab（按子 Tab 切换加载） ----
async function loadSavedSubTab(sub: 'articles' | 'questions' | 'booklists' | 'quotes') {
  if (sub === 'articles') {
    try {
      const bookmarkResp = await getMyBookmarks();
      if (bookmarkResp.code === 200 && bookmarkResp.data) {
        bookmarkedArticles.value = bookmarkResp.data.list || [];
      } else {
        bookmarkedArticles.value = [];
      }
    } catch (e) {
      console.warn('获取我的文章收藏失败:', e);
      bookmarkedArticles.value = [];
    }
  } else if (sub === 'questions') {
    try {
      const resp = await getMyQuestionBookmarks({ pageNum: 1, pageSize: 20 });
      if (resp.code === 200 && resp.data) {
        bookmarkedQuestions.value = resp.data.list || [];
      } else {
        bookmarkedQuestions.value = [];
      }
    } catch (e) {
      console.warn('获取我的题目收藏失败:', e);
      bookmarkedQuestions.value = [];
    }
  }
  // booklists / quotes 暂无对应列表接口，保持空状态由模板占位提示
}

// ---- 阅读 Tab ----
async function loadReadingTab() {
  try {
    const resp = await getMyBookshelf({ pageNum: 1, pageSize: 20 });
    if (resp.code === 200 && resp.data) {
      bookshelfItems.value = resp.data.records || [];
      bookshelfTotal.value = resp.data.total || 0;
    } else {
      bookshelfItems.value = [];
      bookshelfTotal.value = 0;
    }
  } catch (e) {
    console.warn('获取我的书架失败:', e);
    bookshelfItems.value = [];
    bookshelfTotal.value = 0;
  }
}

// ---- 学习 Tab ----
async function loadLearnTab() {
  try {
    const resp = await getMySubmissionList({ pageNum: 1, pageSize: 20 });
    if (resp.code === 200 && resp.data) {
      mySubmissions.value = resp.data.list || [];
    } else {
      mySubmissions.value = [];
    }
  } catch (e) {
    console.warn('获取我的答题记录失败:', e);
    mySubmissions.value = [];
  }
}

// ---- 面试 Tab ----
async function loadInterviewTab() {
  try {
    const [expResp, resumeResp] = await Promise.all([
      getMyExperienceList({ pageNum: 1, pageSize: 20 }),
      getMyResumeList({ pageNum: 1, pageSize: 20 }),
    ]);
    if (expResp.code === 200 && expResp.data) {
      myExperiences.value = expResp.data.list || [];
    }
    if (resumeResp.code === 200 && resumeResp.data) {
      myResumes.value = resumeResp.data.list || [];
    }
  } catch (e) {
    console.warn('获取面试数据失败:', e);
  }
}

// ---- 关注 Tab（按子 Tab 切换加载） ----
async function loadFollowSubTab(sub: 'following' | 'followers') {
  const me = currentUser.value;
  if (!me) return;
  try {
    if (sub === 'following') {
      const resp = await followApi.getFollowingList(me.id, { pageNum: 1, pageSize: 50 });
      if (resp.code === 200 && resp.data) {
        followingList.value = resp.data.list || [];
      } else {
        followingList.value = [];
      }
    } else {
      const resp = await followApi.getFollowersList(me.id, { pageNum: 1, pageSize: 50 });
      if (resp.code === 200 && resp.data) {
        followersList.value = resp.data.list || [];
      } else {
        followersList.value = [];
      }
    }
  } catch (e) {
    console.warn(`获取${sub === 'following' ? '关注' : '粉丝'}列表失败:`, e);
  }
}

// ---- 成就 Tab ----
async function loadAchievementsTab() {
  // 徽章已在头部加载，此处补全成就列表
  if (myAchievements.value.length === 0) {
    try {
      const resp = await growthApi.getMyAchievements();
      if (resp.code === 200 && resp.data) {
        myAchievements.value = resp.data;
      }
    } catch (e) {
      console.warn('获取成就列表失败:', e);
    }
  }
}

// ============ 交互 ============
function handleTabChange(tabId: string) {
  if (activeTab.value === tabId) return;
  activeTab.value = tabId;
  loadTabData(tabId);
}

function handleSavedSubTab(sub: 'articles' | 'questions' | 'booklists' | 'quotes') {
  if (savedSubTab.value === sub) return;
  savedSubTab.value = sub;
  // 子 Tab 数据懒加载（booklists/quotes 无接口，仅 articles/questions 触发）
  if (sub === 'articles' && bookmarkedArticles.value.length === 0 && !tabLoaded['saved:articles']) {
    tabLoaded['saved:articles'] = true;
    loadSavedSubTab('articles');
  } else if (sub === 'questions' && bookmarkedQuestions.value.length === 0 && !tabLoaded['saved:questions']) {
    tabLoaded['saved:questions'] = true;
    loadSavedSubTab('questions');
  }
}

function handleFollowSubTab(sub: 'following' | 'followers') {
  if (followSubTab.value === sub) return;
  followSubTab.value = sub;
  if (sub === 'followers' && followersList.value.length === 0 && !tabLoaded['follow:followers']) {
    tabLoaded['follow:followers'] = true;
    loadFollowSubTab('followers');
  } else if (sub === 'following' && followingList.value.length === 0 && !tabLoaded['follow:following']) {
    tabLoaded['follow:following'] = true;
    loadFollowSubTab('following');
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
        try {
          const growthResp = await growthApi.getMyGrowth();
          if (growthResp.code === 200 && growthResp.data) {
            myGrowth.value = growthResp.data;
          }
        } catch (e) { /* ignore */ }
      } else {
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
  if (!userStore.isUserInitialized) {
    await userStore.initializeUser();
  }
  await loadUserData();
});

// 跳转
function goToProfile() {
  router.push('/user/profile');
}
function goToSettings() {
  router.push('/user/settings');
}

// dashboard 卡片项
const dashboardCards = computed(() => {
  const d = dashboard.value;
  return [
    { label: '文章', value: d?.articles ?? 0, color: '#3b82f6' },
    { label: '收藏', value: d?.bookmarks ?? 0, color: '#ec4899' },
    { label: '书架', value: d?.bookshelf ?? 0, color: '#10b981' },
    { label: '答题', value: d?.questions ?? 0, color: '#f59e0b' },
    { label: '面经', value: d?.experiences ?? 0, color: '#8b5cf6' },
    { label: '简历', value: d?.resumes ?? 0, color: '#06b6d4' },
    { label: '关注', value: d?.following ?? 0, color: '#6366f1' },
    { label: '粉丝', value: d?.followers ?? 0, color: '#ef4444' },
    { label: '专栏', value: d?.columns ?? 0, color: '#14b8a6' },
    { label: '未读消息', value: d?.unreadMessages ?? 0, color: '#f97316' },
    { label: '成长等级', value: d?.growthLevel ?? (myGrowth.value?.level ?? 0), color: '#dc2626', suffix: '级' },
  ];
});
</script>

<template>
  <div class="min-h-screen flex flex-col" style="background-color: var(--theme-bg);">
    <!-- 面包屑 -->
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
    <div class="py-8 flex-1 pb-24 md:pb-8">
      <div class="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8">
        <!-- 加载状态 -->
        <div v-if="!currentUser" class="text-center py-12">
          <div class="inline-block w-12 h-12 border-4 border-t-4 border-gray-300 rounded-full animate-spin" style="border-top-color: var(--theme-primary);"></div>
          <p class="mt-4" style="color: var(--theme-text-secondary);">加载中...</p>
        </div>

        <template v-else>
          <!-- 用户头部信息 -->
          <div class="mb-6">
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
                        <span v-if="myGrowth || dashboard" class="inline-flex items-center gap-1 px-3 py-1 rounded-full text-xs sm:text-sm font-medium" style="background: linear-gradient(135deg, #f59e0b 0%, #ef4444 100%); color: white;">
                          <Star class="w-3 h-3 sm:w-4 sm:h-4" />
                          Lv.{{ myGrowth?.level || dashboard?.growthLevel || 1 }} · {{ myGrowth?.title || dashboard?.growthTitle || '初出茅庐' }}
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
                      <p v-if="checkinResult" class="mt-2 text-xs text-right" :style="{ color: checkinResult.success ? 'var(--theme-success)' : 'var(--theme-text-secondary)' }">
                        {{ checkinResult.message }}
                        <span v-if="checkinResult.growth">+{{ checkinResult.growth }}</span>
                      </p>
                    </div>
                  </div>

                  <!-- 成长值进度条 -->
                  <div v-if="myGrowth || dashboard" class="mt-4">
                    <div class="flex items-center justify-between text-xs sm:text-sm mb-1">
                      <span style="color: var(--theme-text-secondary);">
                        成长值 {{ myGrowth?.growthValue ?? dashboard?.growthValue ?? 0 }}
                        <span v-if="myGrowth?.seasonRank" class="ml-2">· 本季第 {{ myGrowth.seasonRank }} 名</span>
                      </span>
                      <span v-if="myGrowth?.nextLevelTitle" style="color: var(--theme-text-secondary);">
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

              <!-- Dashboard 数据卡片（聚合接口） -->
              <div class="mt-8">
                <h4 class="text-xs font-semibold uppercase tracking-wider mb-3" style="color: var(--theme-text-secondary);">我的数据中心</h4>
                <div class="grid grid-cols-3 sm:grid-cols-4 lg:grid-cols-6 xl:grid-cols-11 gap-3">
                  <div
                    v-for="card in dashboardCards"
                    :key="card.label"
                    class="text-center p-3 rounded-xl"
                    style="background-color: var(--theme-accent);"
                  >
                    <div class="text-lg sm:text-2xl font-bold mb-1" :style="{ color: card.color }">
                      {{ card.value }}<span v-if="card.suffix" class="text-xs ml-0.5">{{ card.suffix }}</span>
                    </div>
                    <div class="text-xs" style="color: var(--theme-text-secondary);">{{ card.label }}</div>
                  </div>
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

          <!-- 标签导航：横向滚动 + 数字角标 -->
          <div class="mb-6 border-b sticky top-0 z-10" style="background-color: var(--theme-bg); border-color: var(--theme-border);">
            <nav class="flex gap-1 overflow-x-auto pb-0 scrollbar-hide">
              <button
                v-for="tab in tabs"
                :key="tab.id"
                @click="handleTabChange(tab.id)"
                class="relative flex items-center gap-2 px-4 sm:px-6 py-3 sm:py-4 text-sm sm:text-base font-medium border-b-2 transition-colors whitespace-nowrap"
                :style="activeTab === tab.id
                  ? 'border-color: var(--theme-primary); color: var(--theme-primary);'
                  : 'border-color: transparent; color: var(--theme-text-secondary);'"
              >
                <component :is="tab.icon" class="w-4 h-4 sm:w-5 sm:h-5" />
                {{ tab.label }}
                <span
                  v-if="tabCount(tab) !== null && tabCount(tab)! > 0"
                  class="inline-flex items-center justify-center min-w-[1.25rem] h-5 px-1.5 rounded-full text-xs font-semibold"
                  :style="activeTab === tab.id
                    ? 'background-color: var(--theme-primary); color: white;'
                    : 'background-color: var(--theme-accent); color: var(--theme-text-secondary);'"
                >
                  {{ tabCount(tab) }}
                </span>
              </button>
            </nav>
          </div>

          <!-- 标签内容（带过渡动画） -->
          <div class="min-h-[500px]">
            <transition name="fade" mode="out-in">
              <div :key="activeTab">
                <!-- ============ 文章 ============ -->
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

                  <!-- 待审核提示卡 -->
                  <div v-if="pendingCount > 0" class="mb-4 p-4 rounded-lg flex items-center justify-between" style="background-color: var(--theme-accent); border-left: 4px solid var(--theme-primary);">
                    <div class="flex items-center gap-2">
                      <Clock class="w-5 h-5" style="color: var(--theme-primary);" />
                      <span class="text-sm" style="color: var(--theme-text);">您有 <strong>{{ pendingCount }}</strong> 篇文章正在审核中</span>
                    </div>
                    <Link to="/my/articles?status=pending" class="text-sm font-medium" style="color: var(--theme-primary);">
                      查看详情 →
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
                </div>

                <!-- ============ 收藏（子 Tab） ============ -->
                <div v-else-if="activeTab === 'saved'">
                  <h2 class="text-xl sm:text-2xl font-bold mb-4" style="color: var(--theme-text);">我的收藏</h2>

                  <!-- 子 Tab -->
                  <div class="flex gap-1 mb-6 overflow-x-auto scrollbar-hide">
                    <button
                      v-for="sub in savedSubTabs"
                      :key="sub.id"
                      @click="handleSavedSubTab(sub.id)"
                      class="px-4 py-2 rounded-xl text-sm font-medium transition-colors whitespace-nowrap"
                      :style="savedSubTab === sub.id
                        ? 'background-color: var(--theme-primary); color: white;'
                        : 'background-color: var(--theme-accent); color: var(--theme-text-secondary);'"
                    >
                      {{ sub.label }}
                    </button>
                  </div>

                  <!-- 文章收藏 -->
                  <div v-if="savedSubTab === 'articles'">
                    <div v-if="bookmarkedArticles.length > 0" class="space-y-4 sm:space-y-6">
                      <ArticleCard v-for="article in bookmarkedArticles" :key="article.id" :article="article" />
                    </div>
                    <div v-else class="p-8 sm:p-12 rounded-2xl text-center" style="background-color: var(--theme-surface); border: 1px solid var(--theme-border);">
                      <Heart class="w-12 h-12 sm:w-16 sm:h-16 mx-auto mb-4" style="color: var(--theme-text-secondary);" />
                      <h3 class="text-lg font-medium mb-2" style="color: var(--theme-text);">文章收藏为空</h3>
                      <p style="color: var(--theme-text-secondary);">浏览文章并收藏你喜欢的内容</p>
                    </div>
                  </div>

                  <!-- 题目收藏 -->
                  <div v-else-if="savedSubTab === 'questions'">
                    <div v-if="bookmarkedQuestions.length > 0" class="space-y-3">
                      <Link
                        v-for="q in bookmarkedQuestions"
                        :key="q.id"
                        :to="`/interview/question/${q.id}`"
                        class="block p-4 rounded-xl transition-colors hover:opacity-80"
                        style="background-color: var(--theme-surface); border: 1px solid var(--theme-border);"
                      >
                        <div class="flex items-start justify-between gap-3">
                          <div class="min-w-0 flex-1">
                            <h4 class="font-medium mb-1 truncate" style="color: var(--theme-text);">{{ q.title }}</h4>
                            <div class="flex items-center gap-2 flex-wrap text-xs" style="color: var(--theme-text-secondary);">
                              <span v-if="q.categoryName" class="px-2 py-0.5 rounded-full" style="background-color: var(--theme-accent);">{{ q.categoryName }}</span>
                              <span>通过率 {{ Math.round((q.acceptanceRate || 0) * 100) }}%</span>
                              <span>· {{ q.submissionCount || 0 }} 次提交</span>
                            </div>
                          </div>
                          <ChevronRight class="w-5 h-5 flex-shrink-0" style="color: var(--theme-text-secondary);" />
                        </div>
                      </Link>
                    </div>
                    <div v-else class="p-8 sm:p-12 rounded-2xl text-center" style="background-color: var(--theme-surface); border: 1px solid var(--theme-border);">
                      <Heart class="w-12 h-12 sm:w-16 sm:h-16 mx-auto mb-4" style="color: var(--theme-text-secondary);" />
                      <h3 class="text-lg font-medium mb-2" style="color: var(--theme-text);">题目收藏为空</h3>
                      <p class="mb-4" style="color: var(--theme-text-secondary);">在面试题库中收藏常考题目</p>
                      <Link to="/interview" class="inline-flex items-center gap-2 px-4 py-2 rounded-xl text-sm font-medium" style="background-color: var(--theme-primary); color: white;">
                        浏览题库
                        <ChevronRight class="w-4 h-4" />
                      </Link>
                    </div>
                  </div>

                  <!-- 书单收藏（暂无列表接口，占位） -->
                  <div v-else-if="savedSubTab === 'booklists'" class="p-8 sm:p-12 rounded-2xl text-center" style="background-color: var(--theme-surface); border: 1px solid var(--theme-border);">
                    <BookMarked class="w-12 h-12 sm:w-16 sm:h-16 mx-auto mb-4" style="color: var(--theme-text-secondary);" />
                    <h3 class="text-lg font-medium mb-2" style="color: var(--theme-text);">书单收藏</h3>
                    <p class="mb-4" style="color: var(--theme-text-secondary);">书单收藏列表功能即将上线</p>
                    <Link to="/reading/discover" class="inline-flex items-center gap-2 px-4 py-2 rounded-xl text-sm font-medium" style="background-color: var(--theme-primary); color: white;">
                      去发现好书
                      <ChevronRight class="w-4 h-4" />
                    </Link>
                  </div>

                  <!-- 金句收藏（暂无列表接口，占位） -->
                  <div v-else-if="savedSubTab === 'quotes'" class="p-8 sm:p-12 rounded-2xl text-center" style="background-color: var(--theme-surface); border: 1px solid var(--theme-border);">
                    <Star class="w-12 h-12 sm:w-16 sm:h-16 mx-auto mb-4" style="color: var(--theme-text-secondary);" />
                    <h3 class="text-lg font-medium mb-2" style="color: var(--theme-text);">金句收藏</h3>
                    <p class="mb-4" style="color: var(--theme-text-secondary);">金句收藏列表功能即将上线</p>
                    <Link to="/reading" class="inline-flex items-center gap-2 px-4 py-2 rounded-xl text-sm font-medium" style="background-color: var(--theme-primary); color: white;">
                      去读书空间
                      <ChevronRight class="w-4 h-4" />
                    </Link>
                  </div>
                </div>

                <!-- ============ 阅读 ============ -->
                <div v-else-if="activeTab === 'reading'">
                  <div class="flex items-center justify-between mb-6">
                    <h2 class="text-xl sm:text-2xl font-bold" style="color: var(--theme-text);">我的阅读</h2>
                    <Link to="/reading/bookshelf" class="inline-flex items-center gap-1 px-3 py-2 rounded-xl font-medium text-sm" style="color: var(--theme-primary);">
                      查看全部
                      <ChevronRight class="w-4 h-4" />
                    </Link>
                  </div>

                  <!-- 阅读统计卡片 -->
                  <div class="grid grid-cols-2 sm:grid-cols-4 gap-3 mb-6">
                    <div class="text-center p-4 rounded-xl" style="background-color: var(--theme-surface); border: 1px solid var(--theme-border);">
                      <div class="text-2xl font-bold mb-1" style="color: var(--theme-primary);">{{ myStats?.bookFinished ?? 0 }}</div>
                      <div class="text-xs" style="color: var(--theme-text-secondary);">读完的书</div>
                    </div>
                    <div class="text-center p-4 rounded-xl" style="background-color: var(--theme-surface); border: 1px solid var(--theme-border);">
                      <div class="text-2xl font-bold mb-1" style="color: var(--theme-primary);">{{ bookshelfTotal }}</div>
                      <div class="text-xs" style="color: var(--theme-text-secondary);">书架藏书</div>
                    </div>
                    <div class="text-center p-4 rounded-xl" style="background-color: var(--theme-surface); border: 1px solid var(--theme-border);">
                      <div class="text-2xl font-bold mb-1" style="color: var(--theme-primary);">{{ myStats?.readingMinutes ?? 0 }}</div>
                      <div class="text-xs" style="color: var(--theme-text-secondary);">阅读分钟</div>
                    </div>
                    <div class="text-center p-4 rounded-xl" style="background-color: var(--theme-surface); border: 1px solid var(--theme-border);">
                      <div class="text-2xl font-bold mb-1" style="color: var(--theme-primary);">{{ myStats?.quoteCount ?? 0 }}</div>
                      <div class="text-xs" style="color: var(--theme-text-secondary);">收藏金句</div>
                    </div>
                  </div>

                  <!-- 书架列表 -->
                  <div v-if="bookshelfItems.length > 0" class="space-y-3">
                    <div
                      v-for="item in bookshelfItems"
                      :key="item.id"
                      class="flex items-center justify-between p-4 rounded-xl"
                      style="background-color: var(--theme-surface); border: 1px solid var(--theme-border);"
                    >
                      <div class="min-w-0 flex-1">
                        <p class="font-medium mb-1 truncate" style="color: var(--theme-text);">书籍 #{{ item.bookId }}</p>
                        <p class="text-xs" style="color: var(--theme-text-secondary);">
                          <span v-if="item.lastChapterNo">已读到第 {{ item.lastChapterNo }} 章</span>
                          <span v-else>尚未开始阅读</span>
                        </p>
                      </div>
                      <Link
                        :to="item.lastChapterId ? `/reading/book/${item.bookId}/chapter/${item.lastChapterId}` : `/reading/book/${item.bookId}`"
                        class="inline-flex items-center gap-1 px-3 py-1.5 rounded-lg text-sm font-medium"
                        style="background-color: var(--theme-primary); color: white;"
                      >
                        {{ item.lastChapterId ? '继续阅读' : '开始阅读' }}
                      </Link>
                    </div>
                  </div>
                  <div v-else class="p-8 sm:p-12 rounded-2xl text-center" style="background-color: var(--theme-surface); border: 1px solid var(--theme-border);">
                    <BookMarked class="w-12 h-12 sm:w-16 sm:h-16 mx-auto mb-4" style="color: var(--theme-text-secondary);" />
                    <h3 class="text-lg font-medium mb-2" style="color: var(--theme-text);">书架空空如也</h3>
                    <p class="mb-6" style="color: var(--theme-text-secondary);">把喜欢的书加入书架，随时继续阅读</p>
                    <Link to="/reading/discover" class="inline-flex items-center gap-2 px-5 py-2.5 rounded-xl font-medium text-sm" style="background-color: var(--theme-primary); color: white;">
                      发现好书
                      <ChevronRight class="w-4 h-4" />
                    </Link>
                  </div>
                </div>

                <!-- ============ 学习 ============ -->
                <div v-else-if="activeTab === 'learn'">
                  <h2 class="text-xl sm:text-2xl font-bold mb-6" style="color: var(--theme-text);">我的学习</h2>

                  <!-- 学习入口（错题本 / 学习计划） -->
                  <div class="grid grid-cols-1 sm:grid-cols-2 gap-4 mb-6">
                    <button
                      @click="router.push('/learn/wrong')"
                      class="flex items-center gap-4 p-5 rounded-2xl text-left transition-colors hover:opacity-90"
                      style="background-color: var(--theme-surface); border: 1px solid var(--theme-border);"
                    >
                      <div class="w-12 h-12 rounded-xl flex items-center justify-center flex-shrink-0" style="background-color: #fef2f2;">
                        <AlertCircle class="w-6 h-6" style="color: #ef4444;" />
                      </div>
                      <div class="min-w-0">
                        <p class="font-semibold mb-1" style="color: var(--theme-text);">错题本</p>
                        <p class="text-xs" style="color: var(--theme-text-secondary);">回顾答错的题目，针对性复习</p>
                      </div>
                      <ChevronRight class="w-5 h-5 ml-auto flex-shrink-0" style="color: var(--theme-text-secondary);" />
                    </button>
                    <button
                      @click="router.push('/learn/plan')"
                      class="flex items-center gap-4 p-5 rounded-2xl text-left transition-colors hover:opacity-90"
                      style="background-color: var(--theme-surface); border: 1px solid var(--theme-border);"
                    >
                      <div class="w-12 h-12 rounded-xl flex items-center justify-center flex-shrink-0" style="background-color: #eff6ff;">
                        <Calendar class="w-6 h-6" style="color: #3b82f6;" />
                      </div>
                      <div class="min-w-0">
                        <p class="font-semibold mb-1" style="color: var(--theme-text);">学习计划</p>
                        <p class="text-xs" style="color: var(--theme-text-secondary);">制定你的专属刷题计划</p>
                      </div>
                      <ChevronRight class="w-5 h-5 ml-auto flex-shrink-0" style="color: var(--theme-text-secondary);" />
                    </button>
                  </div>

                  <!-- 我的答题列表 -->
                  <div class="flex items-center justify-between mb-4">
                    <h3 class="text-lg font-semibold" style="color: var(--theme-text);">我的答题</h3>
                    <Link to="/interview/my/attempts" class="inline-flex items-center gap-1 text-sm" style="color: var(--theme-primary);">
                      查看全部
                      <ChevronRight class="w-4 h-4" />
                    </Link>
                  </div>

                  <div v-if="mySubmissions.length > 0" class="space-y-3">
                    <Link
                      v-for="sub in mySubmissions"
                      :key="sub.id"
                      :to="`/interview/question/${sub.questionId}`"
                      class="block p-4 rounded-xl transition-colors hover:opacity-80"
                      style="background-color: var(--theme-surface); border: 1px solid var(--theme-border);"
                    >
                      <div class="flex items-center justify-between gap-3">
                        <div class="min-w-0 flex-1">
                          <p class="font-medium mb-1 truncate" style="color: var(--theme-text);">题目 #{{ sub.questionId }}</p>
                          <div class="flex items-center gap-2 flex-wrap text-xs" style="color: var(--theme-text-secondary);">
                            <span v-if="sub.answerType" class="px-2 py-0.5 rounded-full" style="background-color: var(--theme-accent);">{{ sub.answerType }}</span>
                            <span v-if="sub.language">{{ sub.language }}</span>
                            <span v-if="sub.createTime">{{ sub.createTime }}</span>
                          </div>
                        </div>
                        <span
                          v-if="sub.isSuccess !== undefined"
                          class="inline-flex items-center gap-1 px-2 py-1 rounded-full text-xs font-medium"
                          :style="sub.isSuccess ? 'background-color: #dcfce7; color: #16a34a;' : 'background-color: #fef2f2; color: #dc2626;'"
                        >
                          <component :is="sub.isSuccess ? CheckCircle2 : AlertCircle" class="w-3 h-3" />
                          {{ sub.isSuccess ? '通过' : '未通过' }}
                        </span>
                      </div>
                    </Link>
                  </div>
                  <div v-else class="p-8 sm:p-12 rounded-2xl text-center" style="background-color: var(--theme-surface); border: 1px solid var(--theme-border);">
                    <GraduationCap class="w-12 h-12 sm:w-16 sm:h-16 mx-auto mb-4" style="color: var(--theme-text-secondary);" />
                    <h3 class="text-lg font-medium mb-2" style="color: var(--theme-text);">还没有答题记录</h3>
                    <p class="mb-6" style="color: var(--theme-text-secondary);">开始练习，记录你的成长轨迹</p>
                    <Link to="/interview" class="inline-flex items-center gap-2 px-5 py-2.5 rounded-xl font-medium text-sm" style="background-color: var(--theme-primary); color: white;">
                      开始刷题
                      <ChevronRight class="w-4 h-4" />
                    </Link>
                  </div>
                </div>

                <!-- ============ 面试 ============ -->
                <div v-else-if="activeTab === 'interview'">
                  <h2 class="text-xl sm:text-2xl font-bold mb-6" style="color: var(--theme-text);">我的面试</h2>

                  <!-- 我的面经 -->
                  <div class="flex items-center justify-between mb-4">
                    <h3 class="text-lg font-semibold" style="color: var(--theme-text);">我的面经</h3>
                    <Link to="/interview/my/experiences" class="inline-flex items-center gap-1 text-sm" style="color: var(--theme-primary);">
                      查看全部
                      <ChevronRight class="w-4 h-4" />
                    </Link>
                  </div>
                  <div v-if="myExperiences.length > 0" class="space-y-3 mb-8">
                    <Link
                      v-for="exp in myExperiences"
                      :key="exp.id"
                      :to="`/interview/experience/${exp.id}`"
                      class="block p-4 rounded-xl transition-colors hover:opacity-80"
                      style="background-color: var(--theme-surface); border: 1px solid var(--theme-border);"
                    >
                      <div class="flex items-start justify-between gap-3">
                        <div class="min-w-0 flex-1">
                          <p class="font-medium mb-1 truncate" style="color: var(--theme-text);">{{ exp.title }}</p>
                          <div class="flex items-center gap-2 flex-wrap text-xs" style="color: var(--theme-text-secondary);">
                            <span class="px-2 py-0.5 rounded-full" style="background-color: var(--theme-accent);">{{ exp.company }}</span>
                            <span v-if="exp.position">{{ exp.position }}</span>
                            <span v-if="exp.status">{{ exp.status }}</span>
                          </div>
                        </div>
                        <ChevronRight class="w-5 h-5 flex-shrink-0" style="color: var(--theme-text-secondary);" />
                      </div>
                    </Link>
                  </div>
                  <div v-else class="p-6 rounded-2xl text-center mb-8" style="background-color: var(--theme-surface); border: 1px solid var(--theme-border);">
                    <FileText class="w-10 h-10 mx-auto mb-3" style="color: var(--theme-text-secondary);" />
                    <p class="text-sm mb-3" style="color: var(--theme-text-secondary);">还没有发布面经</p>
                    <Link to="/interview/experience/publish" class="inline-flex items-center gap-2 px-4 py-2 rounded-xl text-sm font-medium" style="background-color: var(--theme-primary); color: white;">
                      分享面经
                    </Link>
                  </div>

                  <!-- 我的简历 -->
                  <div class="flex items-center justify-between mb-4">
                    <h3 class="text-lg font-semibold" style="color: var(--theme-text);">我的简历</h3>
                    <Link to="/interview/my/resumes" class="inline-flex items-center gap-1 text-sm" style="color: var(--theme-primary);">
                      查看全部
                      <ChevronRight class="w-4 h-4" />
                    </Link>
                  </div>
                  <div v-if="myResumes.length > 0" class="space-y-3">
                    <Link
                      v-for="resume in myResumes"
                      :key="resume.id"
                      :to="`/interview/resume/edit/${resume.id}`"
                      class="block p-4 rounded-xl transition-colors hover:opacity-80"
                      style="background-color: var(--theme-surface); border: 1px solid var(--theme-border);"
                    >
                      <div class="flex items-center justify-between gap-3">
                        <div class="min-w-0 flex-1">
                          <p class="font-medium mb-1 truncate" style="color: var(--theme-text);">{{ resume.title || resume.name || '未命名简历' }}</p>
                          <div class="flex items-center gap-2 flex-wrap text-xs" style="color: var(--theme-text-secondary);">
                            <span v-if="resume.versionNo">v{{ resume.versionNo }}</span>
                            <span v-if="resume.status">{{ resume.status }}</span>
                            <span v-if="resume.updateTime">{{ resume.updateTime }}</span>
                          </div>
                        </div>
                        <ChevronRight class="w-5 h-5 flex-shrink-0" style="color: var(--theme-text-secondary);" />
                      </div>
                    </Link>
                  </div>
                  <div v-else class="p-6 rounded-2xl text-center" style="background-color: var(--theme-surface); border: 1px solid var(--theme-border);">
                    <FileText class="w-10 h-10 mx-auto mb-3" style="color: var(--theme-text-secondary);" />
                    <p class="text-sm mb-3" style="color: var(--theme-text-secondary);">还没有创建简历</p>
                    <Link to="/interview/resume/edit" class="inline-flex items-center gap-2 px-4 py-2 rounded-xl text-sm font-medium" style="background-color: var(--theme-primary); color: white;">
                      创建简历
                    </Link>
                  </div>
                </div>

                <!-- ============ 关注（子 Tab） ============ -->
                <div v-else-if="activeTab === 'follow'">
                  <h2 class="text-xl sm:text-2xl font-bold mb-4" style="color: var(--theme-text);">关注与粉丝</h2>

                  <!-- 子 Tab -->
                  <div class="flex gap-1 mb-6">
                    <button
                      v-for="sub in followSubTabs"
                      :key="sub.id"
                      @click="handleFollowSubTab(sub.id)"
                      class="px-4 py-2 rounded-xl text-sm font-medium transition-colors whitespace-nowrap"
                      :style="followSubTab === sub.id
                        ? 'background-color: var(--theme-primary); color: white;'
                        : 'background-color: var(--theme-accent); color: var(--theme-text-secondary);'"
                    >
                      {{ sub.label }}
                      <span class="ml-1 text-xs opacity-80">({{ sub.id === 'following' ? (dashboard?.following ?? 0) : (dashboard?.followers ?? 0) }})</span>
                    </button>
                  </div>

                  <!-- 我关注的人 -->
                  <div v-if="followSubTab === 'following'">
                    <div v-if="followingList.length > 0" class="grid grid-cols-1 sm:grid-cols-2 gap-3">
                      <Link
                        v-for="u in followingList"
                        :key="u.id"
                        :to="`/author/${u.id}`"
                        class="flex items-center gap-3 p-4 rounded-xl transition-colors hover:opacity-80"
                        style="background-color: var(--theme-surface); border: 1px solid var(--theme-border);"
                      >
                        <img
                          :src="getSafeAvatar(u.avatar, String(u.id))"
                          :alt="u.nickname || u.username || ''"
                          class="w-12 h-12 rounded-full object-cover flex-shrink-0"
                          @error="(e: Event) => (e.target as HTMLImageElement).src = getSafeAvatar(null, String(u.id))"
                        />
                        <div class="min-w-0 flex-1">
                          <p class="font-medium mb-0.5 truncate" style="color: var(--theme-text);">{{ u.nickname || u.username || '匿名用户' }}</p>
                          <p class="text-xs truncate" style="color: var(--theme-text-secondary);">{{ u.position || u.bio || '暂无简介' }}</p>
                        </div>
                      </Link>
                    </div>
                    <div v-else class="p-8 sm:p-12 rounded-2xl text-center" style="background-color: var(--theme-surface); border: 1px solid var(--theme-border);">
                      <Users class="w-12 h-12 sm:w-16 sm:h-16 mx-auto mb-4" style="color: var(--theme-text-secondary);" />
                      <h3 class="text-lg font-medium mb-2" style="color: var(--theme-text);">还没有关注的人</h3>
                      <p class="mb-6" style="color: var(--theme-text-secondary);">关注喜欢的作者，获取最新动态</p>
                      <Link to="/authors" class="inline-flex items-center gap-2 px-5 py-2.5 rounded-xl font-medium text-sm" style="background-color: var(--theme-primary); color: white;">
                        发现作者
                        <ChevronRight class="w-4 h-4" />
                      </Link>
                    </div>
                  </div>

                  <!-- 我的粉丝 -->
                  <div v-else>
                    <div v-if="followersList.length > 0" class="grid grid-cols-1 sm:grid-cols-2 gap-3">
                      <Link
                        v-for="u in followersList"
                        :key="u.id"
                        :to="`/author/${u.id}`"
                        class="flex items-center gap-3 p-4 rounded-xl transition-colors hover:opacity-80"
                        style="background-color: var(--theme-surface); border: 1px solid var(--theme-border);"
                      >
                        <img
                          :src="getSafeAvatar(u.avatar, String(u.id))"
                          :alt="u.nickname || u.username || ''"
                          class="w-12 h-12 rounded-full object-cover flex-shrink-0"
                          @error="(e: Event) => (e.target as HTMLImageElement).src = getSafeAvatar(null, String(u.id))"
                        />
                        <div class="min-w-0 flex-1">
                          <p class="font-medium mb-0.5 truncate" style="color: var(--theme-text);">{{ u.nickname || u.username || '匿名用户' }}</p>
                          <p class="text-xs truncate" style="color: var(--theme-text-secondary);">{{ u.position || u.bio || '暂无简介' }}</p>
                        </div>
                      </Link>
                    </div>
                    <div v-else class="p-8 sm:p-12 rounded-2xl text-center" style="background-color: var(--theme-surface); border: 1px solid var(--theme-border);">
                      <Users class="w-12 h-12 sm:w-16 sm:h-16 mx-auto mb-4" style="color: var(--theme-text-secondary);" />
                      <h3 class="text-lg font-medium mb-2" style="color: var(--theme-text);">还没有粉丝</h3>
                      <p style="color: var(--theme-text-secondary);">持续创作优质内容，吸引更多粉丝</p>
                    </div>
                  </div>
                </div>

                <!-- ============ 成就 ============ -->
                <div v-else-if="activeTab === 'achievements'">
                  <h2 class="text-xl sm:text-2xl font-bold mb-6" style="color: var(--theme-text);">我的成就</h2>

                  <!-- 等级权益 -->
                  <div class="p-5 rounded-2xl mb-6" style="background: linear-gradient(135deg, #f59e0b 0%, #ef4444 100%); color: white;">
                    <div class="flex items-center gap-3">
                      <Crown class="w-10 h-10 text-yellow-300" />
                      <div>
                        <p class="text-lg font-bold">Lv.{{ myGrowth?.level || dashboard?.growthLevel || 1 }} · {{ myGrowth?.title || dashboard?.growthTitle || '初出茅庐' }}</p>
                        <p class="text-sm text-white/90">累计成长值 {{ myGrowth?.growthValue ?? dashboard?.growthValue ?? 0 }}</p>
                      </div>
                    </div>
                  </div>

                  <!-- 我的徽章 -->
                  <div class="mb-6">
                    <div class="flex items-center justify-between mb-4">
                      <h3 class="text-base font-semibold" style="color: var(--theme-text);">我的徽章（{{ myBadges.length }}）</h3>
                      <Link to="/achievements" class="text-sm flex items-center gap-1" style="color: var(--theme-primary);">
                        查看全部
                        <ChevronRight class="w-4 h-4" />
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
                      <p class="text-sm" style="color: var(--theme-text-secondary);">还没有徽章，继续努力解锁成就吧</p>
                    </div>
                  </div>

                  <!-- 成就列表 -->
                  <div>
                    <h3 class="text-base font-semibold mb-4" style="color: var(--theme-text);">成就列表</h3>
                    <div v-if="myAchievements.length > 0" class="space-y-3">
                      <div
                        v-for="ach in myAchievements"
                        :key="ach.id"
                        class="flex items-center gap-3 p-4 rounded-xl"
                        :style="ach.earned
                          ? 'background-color: var(--theme-surface); border: 1px solid var(--theme-primary);'
                          : 'background-color: var(--theme-accent); border: 1px solid var(--theme-border); opacity: 0.7;'"
                      >
                        <div
                          class="w-10 h-10 rounded-xl flex items-center justify-center flex-shrink-0"
                          :style="ach.earned ? 'background: linear-gradient(135deg, #f59e0b 0%, #ef4444 100%);' : 'background-color: var(--theme-border);'"
                        >
                          <Award class="w-5 h-5" :style="ach.earned ? 'color: white;' : 'color: var(--theme-text-secondary);'" />
                        </div>
                        <div class="flex-1 min-w-0">
                          <p class="font-medium mb-0.5" style="color: var(--theme-text);">{{ ach.name }}</p>
                          <p class="text-xs truncate" style="color: var(--theme-text-secondary);">{{ ach.description || '' }}</p>
                        </div>
                        <span
                          v-if="ach.earned"
                          class="inline-flex items-center gap-1 px-2 py-1 rounded-full text-xs font-medium"
                          style="background-color: #dcfce7; color: #16a34a;"
                        >
                          <CheckCircle2 class="w-3 h-3" />
                          已达成
                        </span>
                      </div>
                    </div>
                    <div v-else class="p-6 rounded-xl text-center" style="background-color: var(--theme-accent); border: 1px dashed var(--theme-border);">
                      <p class="text-sm" style="color: var(--theme-text-secondary);">成就列表加载中或为空</p>
                    </div>
                  </div>
                </div>

                <!-- ============ 账号 ============ -->
                <div v-else-if="activeTab === 'account'">
                  <h2 class="text-xl sm:text-2xl font-bold mb-6" style="color: var(--theme-text);">账号与设置</h2>

                  <div class="grid grid-cols-1 sm:grid-cols-2 gap-3">
                    <!-- 编辑资料 -->
                    <button
                      @click="router.push('/user/profile')"
                      class="flex items-center gap-4 p-5 rounded-2xl text-left transition-colors hover:opacity-90"
                      style="background-color: var(--theme-surface); border: 1px solid var(--theme-border);"
                    >
                      <div class="w-12 h-12 rounded-xl flex items-center justify-center flex-shrink-0" style="background-color: #eff6ff;">
                        <Edit class="w-6 h-6" style="color: #3b82f6;" />
                      </div>
                      <div class="min-w-0">
                        <p class="font-semibold mb-1" style="color: var(--theme-text);">编辑资料</p>
                        <p class="text-xs" style="color: var(--theme-text-secondary);">修改头像、昵称、个人简介</p>
                      </div>
                      <ChevronRight class="w-5 h-5 ml-auto flex-shrink-0" style="color: var(--theme-text-secondary);" />
                    </button>

                    <!-- 账号设置 -->
                    <button
                      @click="router.push('/user/settings')"
                      class="flex items-center gap-4 p-5 rounded-2xl text-left transition-colors hover:opacity-90"
                      style="background-color: var(--theme-surface); border: 1px solid var(--theme-border);"
                    >
                      <div class="w-12 h-12 rounded-xl flex items-center justify-center flex-shrink-0" style="background-color: #f5f3ff;">
                        <Settings class="w-6 h-6" style="color: #8b5cf6;" />
                      </div>
                      <div class="min-w-0">
                        <p class="font-semibold mb-1" style="color: var(--theme-text);">账号设置</p>
                        <p class="text-xs" style="color: var(--theme-text-secondary);">密码、通知、隐私等设置</p>
                      </div>
                      <ChevronRight class="w-5 h-5 ml-auto flex-shrink-0" style="color: var(--theme-text-secondary);" />
                    </button>

                    <!-- 我的举报 -->
                    <button
                      @click="router.push('/report')"
                      class="flex items-center gap-4 p-5 rounded-2xl text-left transition-colors hover:opacity-90"
                      style="background-color: var(--theme-surface); border: 1px solid var(--theme-border);"
                    >
                      <div class="w-12 h-12 rounded-xl flex items-center justify-center flex-shrink-0" style="background-color: #fef2f2;">
                        <Flag class="w-6 h-6" style="color: #ef4444;" />
                      </div>
                      <div class="min-w-0">
                        <p class="font-semibold mb-1" style="color: var(--theme-text);">我的举报</p>
                        <p class="text-xs" style="color: var(--theme-text-secondary);">查看举报记录与处理进度</p>
                      </div>
                      <ChevronRight class="w-5 h-5 ml-auto flex-shrink-0" style="color: var(--theme-text-secondary);" />
                    </button>

                    <!-- 我的反馈 -->
                    <button
                      @click="router.push('/report')"
                      class="flex items-center gap-4 p-5 rounded-2xl text-left transition-colors hover:opacity-90"
                      style="background-color: var(--theme-surface); border: 1px solid var(--theme-border);"
                    >
                      <div class="w-12 h-12 rounded-xl flex items-center justify-center flex-shrink-0" style="background-color: #ecfdf5;">
                        <MessageSquare class="w-6 h-6" style="color: #10b981;" />
                      </div>
                      <div class="min-w-0">
                        <p class="font-semibold mb-1" style="color: var(--theme-text);">我的反馈</p>
                        <p class="text-xs" style="color: var(--theme-text-secondary);">意见反馈与帮助中心</p>
                      </div>
                      <ChevronRight class="w-5 h-5 ml-auto flex-shrink-0" style="color: var(--theme-text-secondary);" />
                    </button>

                    <!-- 消费记录 -->
                    <button
                      @click="router.push('/my/consumption')"
                      class="flex items-center gap-4 p-5 rounded-2xl text-left transition-colors hover:opacity-90"
                      style="background-color: var(--theme-surface); border: 1px solid var(--theme-border);"
                    >
                      <div class="w-12 h-12 rounded-xl flex items-center justify-center flex-shrink-0" style="background-color: #fffbeb;">
                        <Receipt class="w-6 h-6" style="color: #f59e0b;" />
                      </div>
                      <div class="min-w-0">
                        <p class="font-semibold mb-1" style="color: var(--theme-text);">我的打赏/消费记录</p>
                        <p class="text-xs" style="color: var(--theme-text-secondary);">查看我打赏的与收到的打赏</p>
                      </div>
                      <ChevronRight class="w-5 h-5 ml-auto flex-shrink-0" style="color: var(--theme-text-secondary);" />
                    </button>
                  </div>
                </div>
              </div>
            </transition>
          </div>
        </template>
      </div>
    </div>
  </div>
</template>

<style scoped>
/* Tab 切换淡入淡出过渡 */
.fade-enter-active,
.fade-leave-active {
  transition: opacity 0.2s ease, transform 0.2s ease;
}
.fade-enter-from {
  opacity: 0;
  transform: translateY(8px);
}
.fade-leave-to {
  opacity: 0;
  transform: translateY(-8px);
}
/* 隐藏横向滚动条（移动端 Tab 友好） */
.scrollbar-hide {
  -ms-overflow-style: none;
  scrollbar-width: none;
}
.scrollbar-hide::-webkit-scrollbar {
  display: none;
}
</style>
