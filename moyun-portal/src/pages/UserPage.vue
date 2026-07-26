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
  AlertCircle,
  CheckCircle2,
  // 数据看板相关图标
  BarChart3,
  Eye,
  Bookmark,
  UserPlus,
  MapPin,
  Loader2,
  PenSquare,
  RefreshCw,
  // v1.1 读者画像扩展图标
  Venus,           // 性别-女
  Mars,            // 性别-男
  UserCircle,      // 性别-其他/未知
  Cake,            // 年龄段
  Info,
} from 'lucide-vue-next';
import { useRoute } from 'vue-router';
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
import {
  getCreatorDashboard,
  getCreatorCalendar,
  getReaderProfile,
} from '@/api/creator';
import type {
  CreatorDashboard,
  CalendarCell,
  ReaderProfile,
} from '@/api/creator';
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
const route = useRoute();
const userStore = useUserStore();

// ============ 基础数据 ============
const currentUser = ref<UserType | null>(null);
const userStats = ref<UserStats | null>(null);
// 默认进入"数据看板"，让创作者数据成为入口
const initialTab = ((): string => {
  const q = route.query.tab;
  if (typeof q === 'string' && ['dashboard','articles','saved','reading','learn','interview','follow','achievements','account'].includes(q)) {
    return q;
  }
  return 'dashboard';
})();
const activeTab = ref(initialTab);
const isLoading = ref(false);
const pendingCount = ref(0);

// ============ 数据看板 Tab（创作者数据） ============
const creatorTrend = ref<CreatorDashboard | null>(null);
const creatorCalendar = ref<CalendarCell[]>([]);
const readerProfile = ref<ReaderProfile | null>(null);
const creatorLoading = ref(false);
const creatorError = ref<string | null>(null);

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
  { id: 'dashboard', label: '数据看板', icon: BarChart3, countKey: null },
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

  // 默认 Tab（数据看板）数据加载
  await loadTabData(activeTab.value);
}

// 各 Tab 懒加载入口
async function loadTabData(tabId: string) {
  if (tabLoaded[tabId]) return;
  tabLoaded[tabId] = true;
  try {
    switch (tabId) {
      case 'dashboard':
        await loadDashboardTab();
        break;
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

// ---- 数据看板 Tab（创作者中心数据） ----
async function loadDashboardTab() {
  creatorLoading.value = true;
  creatorError.value = null;
  try {
    const [dashRes, calRes, readerRes] = await Promise.all([
      getCreatorDashboard(),
      getCreatorCalendar(),
      getReaderProfile(),
    ]);
    if (dashRes.code === 200) creatorTrend.value = dashRes.data;
    if (calRes.code === 200) creatorCalendar.value = calRes.data || [];
    if (readerRes.code === 200) readerProfile.value = readerRes.data;
  } catch (err) {
    const e = err as { message?: string };
    creatorError.value = e?.message || '加载创作者数据失败，请稍后重试';
  } finally {
    creatorLoading.value = false;
  }
}

async function reloadDashboard() {
  tabLoaded['dashboard'] = false;
  await loadDashboardTab();
  tabLoaded['dashboard'] = true;
}

// ============ 数据看板：30 天趋势折线图（SVG） ============
const trendTotals = computed(() => {
  const d = creatorTrend.value;
  if (!d) return { views: 0, likes: 0, bookmarks: 0, followers: 0 };
  // 强制 Number 转换，避免后端返回字符串 "0" 时 0 + "0" 拼接为 "00"
  const sum = (arr: number[] | undefined) =>
    (arr || []).reduce((a, b) => a + Number(b) || 0, 0);
  return {
    views: sum(d.views),
    likes: sum(d.likes),
    bookmarks: sum(d.bookmarks),
    followers: sum(d.followers),
  };
});

const hasTrendData = computed(() => {
  const t = trendTotals.value;
  return t.views + t.likes + t.bookmarks + t.followers > 0;
});

const CHART_W = 760;
const CHART_H = 240;
const PAD_L = 40;
const PAD_R = 16;
const PAD_T = 16;
const PAD_B = 28;

const chartMax = computed(() => {
  const d = creatorTrend.value;
  if (!d) return 10;
  const all = [
    ...(d.views || []),
    ...(d.likes || []),
    ...(d.bookmarks || []),
    ...(d.followers || []),
  ].map((v) => Number(v) || 0);
  const m = Math.max(1, ...all);
  return Math.max(5, Math.ceil(m / 5) * 5);
});

interface Series {
  key: string;
  label: string;
  color: string;
  values: number[];
}

const series = computed<Series[]>(() => {
  const d = creatorTrend.value;
  if (!d) return [];
  const toNum = (arr: number[] | undefined) => (arr || []).map((v) => Number(v) || 0);
  return [
    { key: 'views', label: '阅读', color: 'var(--theme-primary)', values: toNum(d.views) },
    { key: 'likes', label: '点赞', color: '#ef4444', values: toNum(d.likes) },
    { key: 'bookmarks', label: '收藏', color: '#f59e0b', values: toNum(d.bookmarks) },
    { key: 'followers', label: '新增粉丝', color: '#10b981', values: toNum(d.followers) },
  ];
});

const xCount = computed(() => creatorTrend.value?.dates?.length || 30);

function xCoord(i: number): number {
  const innerW = CHART_W - PAD_L - PAD_R;
  const n = Math.max(1, xCount.value - 1);
  return PAD_L + (innerW * i) / n;
}

function yCoord(v: number): number {
  const innerH = CHART_H - PAD_T - PAD_B;
  const max = chartMax.value || 1;
  return PAD_T + innerH - (innerH * v) / max;
}

function buildPath(values: number[]): string {
  if (!values.length) return '';
  return values
    .map((v, i) => `${i === 0 ? 'M' : 'L'} ${xCoord(i).toFixed(1)} ${yCoord(v).toFixed(1)}`)
    .join(' ');
}

const yTicks = computed(() => {
  const max = chartMax.value;
  return [0, max / 4, max / 2, (max * 3) / 4, max].map((v) => Math.round(v));
});

const xLabels = computed(() => {
  const dates = creatorTrend.value?.dates || [];
  const labels: { x: number; text: string }[] = [];
  dates.forEach((date, i) => {
    if (i % 5 === 0 || i === dates.length - 1) {
      labels.push({ x: xCoord(i), text: date.slice(5) });
    }
  });
  return labels;
});

// ============ 数据看板：创作日历热力图（GitHub 风格） ============
interface HeatCell {
  date: string;
  count: number;
  month: number;
}

function toDateStr(d: Date): string {
  const y = d.getFullYear();
  const m = String(d.getMonth() + 1).padStart(2, '0');
  const day = String(d.getDate()).padStart(2, '0');
  return `${y}-${m}-${day}`;
}

const heatColumns = computed<HeatCell[][]>(() => {
  const countMap = new Map<string, number>();
  creatorCalendar.value.forEach((c) => countMap.set(c.date, c.count));

  const today = new Date();
  today.setHours(0, 0, 0, 0);
  const start = new Date(today);
  start.setDate(start.getDate() - 364);
  const dayOfWeek = start.getDay();
  start.setDate(start.getDate() - dayOfWeek);

  const columns: HeatCell[][] = [];
  const cursor = new Date(start);
  while (cursor <= today) {
    const col: HeatCell[] = [];
    for (let d = 0; d < 7; d++) {
      const dateStr = toDateStr(cursor);
      if (cursor > today) {
        col.push({ date: dateStr, count: -1, month: cursor.getMonth() });
      } else {
        col.push({
          date: dateStr,
          count: countMap.get(dateStr) || 0,
          month: cursor.getMonth(),
        });
      }
      cursor.setDate(cursor.getDate() + 1);
    }
    columns.push(col);
  }
  return columns;
});

const heatMax = computed(() => {
  let m = 0;
  heatColumns.value.forEach((col) => col.forEach((c) => { if (c.count > m) m = c.count; }));
  return m;
});

function heatColor(count: number): string {
  if (count < 0) return 'transparent';
  if (count === 0) return 'var(--theme-border)';
  const max = Math.max(1, heatMax.value);
  const ratio = Math.min(1, count / max);
  if (ratio <= 0.25) return '#9be9a8';
  if (ratio <= 0.5) return '#40c463';
  if (ratio <= 0.75) return '#30a14e';
  return '#216e39';
}

const monthLabels = computed(() => {
  const labels: { x: number; text: string }[] = [];
  heatColumns.value.forEach((col, idx) => {
    const first = col.find((c) => c.count >= 0);
    if (!first) return;
    const prevMonth = idx > 0
      ? heatColumns.value[idx - 1].find((c) => c.count >= 0)?.month
      : undefined;
    if (first.month !== prevMonth) {
      labels.push({ x: idx, text: `${first.month + 1}月` });
    }
  });
  return labels;
});

const totalContributions = computed(() =>
  creatorCalendar.value.reduce((sum, c) => sum + c.count, 0)
);

const hasCalendarData = computed(() => totalContributions.value > 0);

// ============ 数据看板：读者画像 ============
const maxRegionValue = computed(() => {
  const regions = readerProfile.value?.regions || [];
  return Math.max(1, ...regions.map((r) => r.value));
});

const maxHourValue = computed(() => {
  const hours = readerProfile.value?.hours || [];
  return Math.max(1, ...hours.map((h) => h.value));
});

// v1.1 性别分布：合计人数（用于百分比兜底展示）
const totalGenderCount = computed(() => {
  const genders = readerProfile.value?.genders || [];
  return genders.reduce((sum, g) => sum + (g.value || 0), 0);
});

// v1.1 性别分布：最大单项值（用于柱状条比例）
const maxGenderValue = computed(() => {
  const genders = readerProfile.value?.genders || [];
  return Math.max(1, ...genders.map((g) => g.value));
});

// v1.1 性别标签映射：male→男 / female→女 / other→其他 / unknown→未知
const genderLabelMap: Record<string, string> = {
  male: '男',
  female: '女',
  other: '其他',
  unknown: '未知',
};

// v1.1 性别图标映射：用于在每行前展示对应图标
function getGenderIcon(gender: string) {
  switch (gender) {
    case 'male': return Mars;
    case 'female': return Venus;
    case 'other': return UserCircle;
    default: return UserCircle;
  }
}

// v1.1 性别图标颜色映射：男=蓝、女=粉、其他=灰、未知=灰
function getGenderColor(gender: string): string {
  switch (gender) {
    case 'male': return '#3B82F6';     // 蓝
    case 'female': return '#EC4899';   // 粉
    case 'other': return '#A855F7';    // 紫
    default: return '#9CA3AF';         // 灰
  }
}

// v1.1 年龄段：合计人数
const totalAgeRangeCount = computed(() => {
  const ages = readerProfile.value?.ageRanges || [];
  return ages.reduce((sum, a) => sum + (a.value || 0), 0);
});

// v1.1 年龄段：最大单项值
const maxAgeRangeValue = computed(() => {
  const ages = readerProfile.value?.ageRanges || [];
  return Math.max(1, ...ages.map((a) => a.value));
});

// v1.1 年龄段标签映射
const ageRangeLabelMap: Record<string, string> = {
  under_18: '18 岁以下',
  '18_24': '18-24 岁',
  '25_30': '25-30 岁',
  '31_35': '31-35 岁',
  '36_45': '36-45 岁',
  over_45: '45 岁以上',
  unknown: '未知',
};

// v1.1 年龄段：按固定顺序排序后的展示列表
const orderedAgeRanges = computed(() => {
  const ages = readerProfile.value?.ageRanges || [];
  const order = ['under_18', '18_24', '25_30', '31_35', '36_45', 'over_45', 'unknown'];
  return order
    .map((k) => ages.find((a) => a.range === k))
    .filter((x): x is NonNullable<typeof x> => !!x);
});

// v1.1 时段分布：高峰时段（占比最高的前 3 个小时）
const peakHours = computed(() => {
  const hours = readerProfile.value?.hours || [];
  if (!hours.length) return [];
  return [...hours]
    .sort((a, b) => b.value - a.value)
    .slice(0, 3)
    .map((h) => h.hour);
});

// v1.1.2 新增：时段分布总读者数（用于空值保护，避免 hours 长度 > 0 但 value 全 0 时仍渲染空柱图）
const totalHourCount = computed(() => {
  const hours = readerProfile.value?.hours || [];
  return hours.reduce((sum, h) => sum + (h.value || 0), 0);
});

function isPeakHour(hour: number): boolean {
  return peakHours.value.includes(hour);
}

// v1.1.2 读者画像：省份热力网格（地图组件的简化版，不引入 echarts）
// 按地理分区（华北/东北/华东/华中/华南/西南/西北/港澳台）组织 34 省份
// 后端返回的 region 是中文省份名（如"北京市"/"广东省"），需要去掉"省/市/自治区"后做归一化匹配
const chinaRegionGroups: { label: string; provinces: { name: string; aliases: string[] }[] }[] = [
  {
    label: '华北',
    provinces: [
      { name: '北京', aliases: ['北京市', '北京'] },
      { name: '天津', aliases: ['天津市', '天津'] },
      { name: '河北', aliases: ['河北省', '河北'] },
      { name: '山西', aliases: ['山西省', '山西'] },
      { name: '内蒙古', aliases: ['内蒙古自治区', '内蒙古'] },
    ],
  },
  {
    label: '东北',
    provinces: [
      { name: '辽宁', aliases: ['辽宁省', '辽宁'] },
      { name: '吉林', aliases: ['吉林省', '吉林'] },
      { name: '黑龙江', aliases: ['黑龙江省', '黑龙江'] },
    ],
  },
  {
    label: '华东',
    provinces: [
      { name: '上海', aliases: ['上海市', '上海'] },
      { name: '江苏', aliases: ['江苏省', '江苏'] },
      { name: '浙江', aliases: ['浙江省', '浙江'] },
      { name: '安徽', aliases: ['安徽省', '安徽'] },
      { name: '福建', aliases: ['福建省', '福建'] },
      { name: '江西', aliases: ['江西省', '江西'] },
      { name: '山东', aliases: ['山东省', '山东'] },
      { name: '台湾', aliases: ['台湾省', '台湾'] },
    ],
  },
  {
    label: '华中',
    provinces: [
      { name: '河南', aliases: ['河南省', '河南'] },
      { name: '湖北', aliases: ['湖北省', '湖北'] },
      { name: '湖南', aliases: ['湖南省', '湖南'] },
    ],
  },
  {
    label: '华南',
    provinces: [
      { name: '广东', aliases: ['广东省', '广东'] },
      { name: '广西', aliases: ['广西壮族自治区', '广西'] },
      { name: '海南', aliases: ['海南省', '海南'] },
      { name: '香港', aliases: ['香港特别行政区', '香港'] },
      { name: '澳门', aliases: ['澳门特别行政区', '澳门'] },
    ],
  },
  {
    label: '西南',
    provinces: [
      { name: '重庆', aliases: ['重庆市', '重庆'] },
      { name: '四川', aliases: ['四川省', '四川'] },
      { name: '贵州', aliases: ['贵州省', '贵州'] },
      { name: '云南', aliases: ['云南省', '云南'] },
      { name: '西藏', aliases: ['西藏自治区', '西藏'] },
    ],
  },
  {
    label: '西北',
    provinces: [
      { name: '陕西', aliases: ['陕西省', '陕西'] },
      { name: '甘肃', aliases: ['甘肃省', '甘肃'] },
      { name: '青海', aliases: ['青海省', '青海'] },
      { name: '宁夏', aliases: ['宁夏回族自治区', '宁夏'] },
      { name: '新疆', aliases: ['新疆维吾尔自治区', '新疆'] },
    ],
  },
];

// 省份名归一化：去掉"省/市/自治区/特别行政区"等后缀，返回简称
function normalizeProvinceName(raw: string): string {
  if (!raw) return '';
  // v1.1.2 修复：JS 正则 | 选择是"先匹配优先"而非"最长匹配优先"，必须把长后缀放前面
  // 否则 "新疆维吾尔自治区" 会先匹配 "自治区" 剥成 "新疆维吾尔" 而非 "新疆"
  return raw
    .replace(/(维吾尔自治区|壮族自治区|回族自治区|特别行政区|自治区|省|市)$/, '')
    .trim();
}

// 把后端 regions 数组转为 Map<省份名, {value, percentage}>
const regionValueMap = computed<Map<string, { value: number; percentage: number }>>(() => {
  const map = new Map<string, { value: number; percentage: number }>();
  const regions = readerProfile.value?.regions || [];
  for (const r of regions) {
    if (!r.region || r.region === '未知') continue;
    const norm = normalizeProvinceName(r.region);
    if (norm) {
      map.set(norm, { value: r.value, percentage: r.percentage });
    }
  }
  return map;
});

// 取某个省份的读者数（按归一化后的省份名匹配），未匹配返回 0
function getProvinceValue(provinceAliases: string[]): number {
  for (const alias of provinceAliases) {
    const norm = normalizeProvinceName(alias);
    const entry = regionValueMap.value.get(norm);
    if (entry) return entry.value;
  }
  return 0;
}

// 省份热力等级（0-4）：根据值在所有省份中的相对位置分级
const maxProvinceValue = computed(() => {
  let max = 0;
  for (const group of chinaRegionGroups) {
    for (const p of group.provinces) {
      const v = getProvinceValue(p.aliases);
      if (v > max) max = v;
    }
  }
  return Math.max(1, max);
});

function getHeatLevel(value: number): number {
  if (value <= 0) return 0;
  const ratio = value / maxProvinceValue.value;
  if (ratio >= 0.75) return 4;
  if (ratio >= 0.5) return 3;
  if (ratio >= 0.25) return 2;
  return 1;
}

// 热力等级 → 颜色（与 var(--theme-primary) 渐变到透明）
function getHeatColor(level: number): string {
  if (level === 0) return 'transparent';
  const opacity = [0, 0.25, 0.5, 0.75, 1][level];
  // 读取 CSS 变量并拼接 opacity，避免硬编码颜色（适配三套主题）
  return `color-mix(in srgb, var(--theme-primary) ${opacity * 100}%, transparent)`;
}

// 全部 34 省份中是否有任意一个匹配到数据（用于判断是否显示热力网格）
const hasAnyProvinceData = computed(() => {
  for (const group of chinaRegionGroups) {
    for (const p of group.provinces) {
      if (getProvinceValue(p.aliases) > 0) return true;
    }
  }
  return false;
});

function goPublish() {
  router.push('/publish');
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
    <!-- 吸顶面包屑栏 -->
    <div
      class="border-b sticky top-0 z-30 backdrop-blur-sm py-3"
      style="background-color: var(--theme-surface); border-color: var(--theme-border);"
    >
      <div class="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 flex items-center justify-between gap-4">
        <Breadcrumb :items="[{ label: '个人中心' }]" />
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
                <!-- ============ 数据看板（创作者中心合并） ============ -->
                <div v-if="activeTab === 'dashboard'">
                  <div class="flex items-center justify-between mb-6">
                    <h2 class="text-xl sm:text-2xl font-bold flex items-center gap-2" style="color: var(--theme-text);">
                      <BarChart3 class="w-6 h-6" style="color: var(--theme-primary);" />
                      数据看板
                    </h2>
                    <button
                      @click="reloadDashboard"
                      :disabled="creatorLoading"
                      class="inline-flex items-center gap-1.5 px-3 py-2 rounded-xl text-sm font-medium transition-colors disabled:opacity-50"
                      style="background-color: var(--theme-surface); border: 1px solid var(--theme-border); color: var(--theme-text);"
                    >
                      <RefreshCw class="w-4 h-4" :class="{ 'animate-spin': creatorLoading }" />
                      刷新
                    </button>
                  </div>

                  <!-- 加载中 -->
                  <div v-if="creatorLoading && !creatorTrend" class="flex flex-col items-center justify-center py-20 rounded-2xl" style="background-color: var(--theme-surface); border: 1px solid var(--theme-border); color: var(--theme-text-secondary);">
                    <Loader2 class="w-8 h-8 animate-spin mb-3" />
                    <span class="text-sm">正在加载创作者数据...</span>
                  </div>

                  <!-- 错误 -->
                  <div v-else-if="creatorError" class="rounded-2xl p-6 text-center"
                    style="background-color: var(--theme-surface); border: 1px solid var(--theme-border); color: var(--theme-text-secondary);"
                  >
                    <p class="mb-4 text-sm">{{ creatorError }}</p>
                    <button @click="reloadDashboard"
                      class="inline-flex items-center gap-2 px-4 py-2 rounded-lg text-sm font-medium text-white"
                      style="background-color: var(--theme-primary);">
                      <RefreshCw class="w-4 h-4" />
                      重新加载
                    </button>
                  </div>

                  <template v-else>
                    <!-- ==================== 顶部：30 天数据趋势 ==================== -->
                    <section class="rounded-2xl p-4 sm:p-6 mb-6"
                      style="background-color: var(--theme-surface); border: 1px solid var(--theme-border);"
                    >
                      <div class="flex flex-wrap items-center justify-between gap-3 mb-4">
                        <h3 class="font-semibold flex items-center gap-2" style="color: var(--theme-text);">
                          <Eye class="w-5 h-5" style="color: var(--theme-primary);" />
                          近 30 天数据趋势
                        </h3>
                        <!-- 汇总 -->
                        <div class="flex flex-wrap gap-3 text-sm">
                          <div class="flex items-center gap-1.5" style="color: var(--theme-text-secondary);">
                            <Eye class="w-4 h-4" style="color: var(--theme-primary);" />
                            阅读 <span class="font-semibold" style="color: var(--theme-text);">{{ trendTotals.views }}</span>
                          </div>
                          <div class="flex items-center gap-1.5" style="color: var(--theme-text-secondary);">
                            <Heart class="w-4 h-4 text-red-500" />
                            点赞 <span class="font-semibold" style="color: var(--theme-text);">{{ trendTotals.likes }}</span>
                          </div>
                          <div class="flex items-center gap-1.5" style="color: var(--theme-text-secondary);">
                            <Bookmark class="w-4 h-4 text-amber-500" />
                            收藏 <span class="font-semibold" style="color: var(--theme-text);">{{ trendTotals.bookmarks }}</span>
                          </div>
                          <div class="flex items-center gap-1.5" style="color: var(--theme-text-secondary);">
                            <UserPlus class="w-4 h-4 text-emerald-500" />
                            新增粉丝 <span class="font-semibold" style="color: var(--theme-text);">{{ trendTotals.followers }}</span>
                          </div>
                        </div>
                      </div>

                      <!-- 有数据：SVG 折线图 -->
                      <div v-if="hasTrendData" class="w-full overflow-x-auto">
                        <svg :viewBox="`0 0 ${CHART_W} ${CHART_H}`" class="w-full h-auto" style="min-width: 560px;">
                          <!-- Y 轴网格线 + 刻度 -->
                          <g>
                            <line
                              v-for="(t, i) in yTicks" :key="`grid-${i}`"
                              :x1="PAD_L" :x2="CHART_W - PAD_R"
                              :y1="yCoord(t)" :y2="yCoord(t)"
                              stroke="var(--theme-border)" stroke-width="1" stroke-dasharray="3 3"
                            />
                            <text
                              v-for="(t, i) in yTicks" :key="`ytick-${i}`"
                              :x="PAD_L - 6" :y="yCoord(t) + 4"
                              text-anchor="end" font-size="10" fill="var(--theme-text-secondary)"
                            >{{ t }}</text>
                          </g>
                          <!-- 折线 -->
                          <g>
                            <path
                              v-for="s in series" :key="`line-${s.key}`"
                              :d="buildPath(s.values)"
                              :stroke="s.color" stroke-width="2" fill="none"
                              stroke-linejoin="round" stroke-linecap="round"
                            />
                          </g>
                          <!-- X 轴标签 -->
                          <g>
                            <text
                              v-for="(lbl, i) in xLabels" :key="`xlabel-${i}`"
                              :x="lbl.x" :y="CHART_H - 8"
                              text-anchor="middle" font-size="10" fill="var(--theme-text-secondary)"
                            >{{ lbl.text }}</text>
                          </g>
                        </svg>

                        <!-- 图例 -->
                        <div class="flex flex-wrap items-center gap-4 mt-3 text-xs" style="color: var(--theme-text-secondary);">
                          <div v-for="s in series" :key="`legend-${s.key}`" class="flex items-center gap-1.5">
                            <span class="inline-block w-3 h-3 rounded-sm" :style="{ backgroundColor: s.color }"></span>
                            {{ s.label }}
                          </div>
                        </div>
                      </div>

                      <!-- 无数据：空状态引导 -->
                      <div v-else class="py-12 text-center">
                        <div class="w-14 h-14 mx-auto rounded-full flex items-center justify-center mb-3"
                          style="background-color: var(--theme-accent);">
                          <BarChart3 class="w-7 h-7" style="color: var(--theme-primary);" />
                        </div>
                        <p class="text-sm mb-1" style="color: var(--theme-text);">暂无数据</p>
                        <p class="text-xs mb-4" style="color: var(--theme-text-secondary);">发布你的第一篇文章，开始积累读者与反馈</p>
                        <button @click="goPublish"
                          class="inline-flex items-center gap-1.5 px-4 py-2 rounded-lg text-sm font-medium text-white"
                          style="background-color: var(--theme-primary);">
                          <PenSquare class="w-4 h-4" />
                          去创作
                        </button>
                      </div>
                    </section>

                    <!-- ==================== 中部：创作日历热力图 ==================== -->
                    <section class="rounded-2xl p-4 sm:p-6 mb-6"
                      style="background-color: var(--theme-surface); border: 1px solid var(--theme-border);"
                    >
                      <div class="flex flex-wrap items-center justify-between gap-3 mb-4">
                        <h3 class="font-semibold flex items-center gap-2" style="color: var(--theme-text);">
                          <Calendar class="w-5 h-5" style="color: var(--theme-primary);" />
                          创作日历
                        </h3>
                        <span class="text-sm" style="color: var(--theme-text-secondary);">
                          近 1 年共 <span class="font-semibold" style="color: var(--theme-text);">{{ totalContributions }}</span> 次创作活动
                        </span>
                      </div>

                      <div v-if="hasCalendarData" class="w-full overflow-x-auto">
                        <div class="inline-block">
                          <!-- 月份标签行 -->
                          <div class="flex ml-7 mb-1 relative h-3">
                            <div
                              v-for="(lbl, i) in monthLabels" :key="`mlbl-${i}`"
                              class="text-[10px] absolute"
                              style="color: var(--theme-text-secondary);"
                              :style="{ marginLeft: (lbl.x * 14) + 'px' }"
                            >{{ lbl.text }}</div>
                          </div>
                          <div class="flex gap-1">
                            <!-- 星期标签 -->
                            <div class="flex flex-col gap-1 mr-1 text-[10px] pt-0.5" style="color: var(--theme-text-secondary);">
                              <div class="h-3 leading-3">&nbsp;</div>
                              <div class="h-3 leading-3">一</div>
                              <div class="h-3 leading-3">&nbsp;</div>
                              <div class="h-3 leading-3">三</div>
                              <div class="h-3 leading-3">&nbsp;</div>
                              <div class="h-3 leading-3">五</div>
                              <div class="h-3 leading-3">&nbsp;</div>
                            </div>
                            <!-- 热力方块 -->
                            <div class="flex gap-1">
                              <div v-for="(col, ci) in heatColumns" :key="`col-${ci}`" class="flex flex-col gap-1">
                                <div
                                  v-for="(cell, ri) in col" :key="`cell-${ci}-${ri}`"
                                  class="w-3 h-3 rounded-sm"
                                  :style="{ backgroundColor: heatColor(cell.count) }"
                                  :title="cell.count >= 0 ? `${cell.date}：${cell.count} 次` : ''"
                                ></div>
                              </div>
                            </div>
                          </div>
                          <!-- 图例 -->
                          <div class="flex items-center gap-2 mt-3 ml-7 text-[10px]" style="color: var(--theme-text-secondary);">
                            <span>少</span>
                            <span class="w-3 h-3 rounded-sm" style="background-color: var(--theme-border);"></span>
                            <span class="w-3 h-3 rounded-sm" style="background-color: #9be9a8;"></span>
                            <span class="w-3 h-3 rounded-sm" style="background-color: #40c463;"></span>
                            <span class="w-3 h-3 rounded-sm" style="background-color: #30a14e;"></span>
                            <span class="w-3 h-3 rounded-sm" style="background-color: #216e39;"></span>
                            <span>多</span>
                          </div>
                        </div>
                      </div>
                      <div v-else class="py-12 text-center">
                        <div class="w-14 h-14 mx-auto rounded-full flex items-center justify-center mb-3"
                          style="background-color: var(--theme-accent);">
                          <Calendar class="w-7 h-7" style="color: var(--theme-primary);" />
                        </div>
                        <p class="text-sm mb-1" style="color: var(--theme-text);">暂无创作记录</p>
                        <p class="text-xs" style="color: var(--theme-text-secondary);">持续创作，让你的创作日历丰富起来</p>
                      </div>
                    </section>

                    <!-- ==================== 底部：读者画像（v1.1 扩展为 4 维度）==================== -->
                    <section class="rounded-2xl p-4 sm:p-6"
                      style="background-color: var(--theme-surface); border: 1px solid var(--theme-border);"
                    >
                      <div class="flex items-center justify-between mb-4 flex-wrap gap-2">
                        <h3 class="font-semibold flex items-center gap-2" style="color: var(--theme-text);">
                          <Users class="w-5 h-5" style="color: var(--theme-primary);" />
                          读者画像（近 30 天）
                        </h3>
                        <div class="flex items-center gap-1 text-xs" style="color: var(--theme-text-secondary);">
                          <Info class="w-3 h-3" />
                          <span>含地域 / 性别 / 年龄段 / 时段四维度</span>
                        </div>
                      </div>

                      <div class="grid grid-cols-1 lg:grid-cols-2 gap-6">
                        <!-- 1. 地域分布 Top10（按省份聚合 + 占比） -->
                        <div class="lg:col-span-2">
                          <h4 class="text-sm font-medium flex items-center gap-2 mb-3" style="color: var(--theme-text-secondary);">
                            <MapPin class="w-4 h-4" />
                            地域分布地图
                            <span class="text-[10px] opacity-70">（按 IP 解析省份聚合 · 全 34 省份热力）</span>
                          </h4>

                          <!-- 1a. 中国省份热力网格（地图组件的简化版，无需引入 echarts） -->
                          <div
                            v-if="hasAnyProvinceData"
                            class="rounded-xl p-4 mb-4"
                            style="background-color: var(--theme-bg); border: 1px solid var(--theme-border);"
                          >
                            <div class="space-y-3">
                              <div v-for="(group, gi) in chinaRegionGroups" :key="`region-group-${gi}`">
                                <div class="flex items-center gap-2 mb-1.5">
                                  <span class="text-[10px] font-medium px-1.5 py-0.5 rounded" style="background-color: var(--theme-accent); color: var(--theme-text-secondary);">
                                    {{ group.label }}
                                  </span>
                                </div>
                                <div class="grid grid-cols-5 sm:grid-cols-8 gap-1">
                                  <div
                                    v-for="(p, pi) in group.provinces"
                                    :key="`prov-${gi}-${pi}`"
                                    class="aspect-square rounded flex flex-col items-center justify-center transition-all cursor-default group relative"
                                    :style="{
                                      backgroundColor: getHeatColor(getHeatLevel(getProvinceValue(p.aliases))),
                                      border: getProvinceValue(p.aliases) > 0 ? '1px solid var(--theme-primary)' : '1px solid var(--theme-border)',
                                    }"
                                    :title="`${p.name}：${getProvinceValue(p.aliases)} 人`"
                                  >
                                    <span
                                      class="text-[10px] sm:text-xs font-medium leading-tight text-center"
                                      :style="{ color: getHeatLevel(getProvinceValue(p.aliases)) >= 3 ? '#fff' : 'var(--theme-text)' }"
                                    >{{ p.name }}</span>
                                    <span
                                      v-if="getProvinceValue(p.aliases) > 0"
                                      class="text-[8px] sm:text-[10px] leading-tight"
                                      :style="{ color: getHeatLevel(getProvinceValue(p.aliases)) >= 3 ? 'rgba(255,255,255,0.85)' : 'var(--theme-text-secondary)' }"
                                    >{{ getProvinceValue(p.aliases) }}</span>
                                  </div>
                                </div>
                              </div>
                            </div>
                            <!-- 图例 -->
                            <div class="flex items-center justify-end gap-2 mt-3 text-[10px]" style="color: var(--theme-text-secondary);">
                              <span>少</span>
                              <div class="w-4 h-3 rounded" :style="{ backgroundColor: getHeatColor(1) }"></div>
                              <div class="w-4 h-3 rounded" :style="{ backgroundColor: getHeatColor(2) }"></div>
                              <div class="w-4 h-3 rounded" :style="{ backgroundColor: getHeatColor(3) }"></div>
                              <div class="w-4 h-3 rounded" :style="{ backgroundColor: getHeatColor(4) }"></div>
                              <span>多</span>
                            </div>
                          </div>

                          <!-- 1b. Top10 排行（保留条形图，展示具体数值） -->
                          <div v-if="(readerProfile?.regions || []).length" class="space-y-2">
                            <div class="text-xs font-medium mb-2" style="color: var(--theme-text-secondary);">Top10 排行</div>
                            <div v-for="(r, i) in readerProfile?.regions" :key="`region-${i}`" class="flex items-center gap-2">
                              <span class="text-xs w-20 truncate" :title="r.region" style="color: var(--theme-text-secondary);">{{ r.region }}</span>
                              <div class="flex-1 h-5 rounded overflow-hidden relative" style="background-color: var(--theme-bg);">
                                <div
                                  class="h-full rounded transition-all flex items-center justify-end pr-2"
                                  :style="{
                                    width: Math.max(2, (r.value / maxRegionValue) * 100) + '%',
                                    backgroundColor: 'var(--theme-primary)',
                                  }"
                                >
                                  <span class="text-[10px] font-medium text-white leading-none">{{ r.percentage }}%</span>
                                </div>
                              </div>
                              <span class="text-xs w-12 text-right font-medium" style="color: var(--theme-text);">{{ r.value }} 人</span>
                            </div>
                          </div>
                          <div v-else-if="!hasAnyProvinceData" class="text-sm py-8 text-center" style="color: var(--theme-text-secondary);">
                            暂无地域分布数据
                          </div>
                        </div>

                        <!-- 2. 性别分布（v1.1 新增） -->
                        <div>
                          <h4 class="text-sm font-medium flex items-center gap-2 mb-3" style="color: var(--theme-text-secondary);">
                            <Users class="w-4 h-4" />
                            性别分布
                            <span class="text-[10px] opacity-70">（仅登录读者）</span>
                          </h4>
                          <div v-if="(readerProfile?.genders || []).length && totalGenderCount > 0" class="space-y-2">
                            <div v-for="(g, i) in readerProfile?.genders" :key="`gender-${i}`" class="flex items-center gap-2">
                              <component :is="getGenderIcon(g.gender)" class="w-4 h-4 flex-shrink-0" :style="{ color: getGenderColor(g.gender) }" />
                              <span class="text-xs w-12" style="color: var(--theme-text-secondary);">{{ genderLabelMap[g.gender] || g.gender }}</span>
                              <div class="flex-1 h-5 rounded overflow-hidden relative" style="background-color: var(--theme-bg);">
                                <div
                                  class="h-full rounded transition-all flex items-center justify-end pr-2"
                                  :style="{
                                    width: Math.max(2, (g.value / maxGenderValue) * 100) + '%',
                                    backgroundColor: getGenderColor(g.gender),
                                  }"
                                >
                                  <span class="text-[10px] font-medium text-white leading-none">{{ g.percentage }}%</span>
                                </div>
                              </div>
                              <span class="text-xs w-12 text-right font-medium" style="color: var(--theme-text);">{{ g.value }} 人</span>
                            </div>
                          </div>
                          <div v-else class="text-sm py-8 text-center" style="color: var(--theme-text-secondary);">
                            暂无性别数据
                          </div>
                        </div>

                        <!-- 3. 年龄段分布（v1.1 新增） -->
                        <div>
                          <h4 class="text-sm font-medium flex items-center gap-2 mb-3" style="color: var(--theme-text-secondary);">
                            <Cake class="w-4 h-4" />
                            年龄段分布
                            <span class="text-[10px] opacity-70">（基于读者自填生日）</span>
                          </h4>
                          <div v-if="orderedAgeRanges.length && totalAgeRangeCount > 0" class="space-y-2">
                            <div v-for="(a, i) in orderedAgeRanges" :key="`age-${i}`" class="flex items-center gap-2">
                              <span class="text-xs w-20 truncate" :title="ageRangeLabelMap[a.range] || a.range" style="color: var(--theme-text-secondary);">{{ ageRangeLabelMap[a.range] || a.range }}</span>
                              <div class="flex-1 h-5 rounded overflow-hidden relative" style="background-color: var(--theme-bg);">
                                <div
                                  class="h-full rounded transition-all flex items-center justify-end pr-2"
                                  :style="{
                                    width: Math.max(2, (a.value / maxAgeRangeValue) * 100) + '%',
                                    backgroundColor: 'var(--theme-primary)',
                                  }"
                                >
                                  <span class="text-[10px] font-medium text-white leading-none">{{ a.percentage }}%</span>
                                </div>
                              </div>
                              <span class="text-xs w-12 text-right font-medium" style="color: var(--theme-text);">{{ a.value }} 人</span>
                            </div>
                          </div>
                          <div v-else class="text-sm py-8 text-center" style="color: var(--theme-text-secondary);">
                            暂无年龄段数据
                          </div>
                        </div>

                        <!-- 4. 时段分布（0-23 时，含高峰时段高亮）v1.1.2：加 lg:col-span-2 占满整行，柱子才不会太挤 -->
                        <div class="lg:col-span-2">
                          <h4 class="text-sm font-medium flex items-center gap-2 mb-3" style="color: var(--theme-text-secondary);">
                            <Clock class="w-4 h-4" />
                            时段分布（0-23 时）
                            <span v-if="peakHours.length" class="text-[10px] opacity-70">高峰：{{ peakHours.map(h => h + '时').join('、') }}</span>
                          </h4>
                          <!-- v1.1.2：增加 totalHourCount > 0 空值保护，避免 hours.length>0 但 value 全 0 时仍渲染空柱图 -->
                          <div v-if="(readerProfile?.hours || []).length && totalHourCount > 0" class="flex items-end gap-1 h-40">
                            <div
                              v-for="h in readerProfile?.hours" :key="`hour-${h.hour}`"
                              class="flex-1 flex flex-col items-center justify-end group relative"
                            >
                              <!-- hover tooltip -->
                              <div
                                class="absolute -top-8 hidden group-hover:block z-10 px-2 py-1 rounded text-[10px] whitespace-nowrap"
                                style="background-color: var(--theme-text); color: var(--theme-bg);"
                              >
                                {{ h.hour }}时 · {{ h.value }} 次 · {{ h.percentage }}%
                              </div>
                              <div
                                class="w-full rounded-t transition-all"
                                :style="{
                                  height: ((h.value / maxHourValue) * 100) + '%',
                                  minHeight: h.value > 0 ? '4px' : '1px',
                                  backgroundColor: isPeakHour(h.hour) ? 'var(--theme-primary)' : 'var(--theme-text-secondary)',
                                  opacity: h.value > 0 ? (isPeakHour(h.hour) ? 1 : 0.55) : 0.2,
                                }"
                                :title="`${h.hour}时：${h.value} 次（${h.percentage}%）`"
                              ></div>
                            </div>
                          </div>
                          <div v-else class="text-sm py-8 text-center" style="color: var(--theme-text-secondary);">
                            暂无时段分布数据
                          </div>
                          <div class="flex justify-between mt-1 text-[10px]" style="color: var(--theme-text-secondary);">
                            <span>0时</span>
                            <span>6时</span>
                            <span>12时</span>
                            <span>18时</span>
                            <span>23时</span>
                          </div>
                        </div>
                      </div>

                      <!-- v1.1 数据局限说明（由后端返回，告知用户数据可能不真实的原因） -->
                      <div
                        v-if="readerProfile?.dataNote"
                        class="mt-4 p-3 rounded-lg flex items-start gap-2 text-xs"
                        style="background-color: var(--theme-bg); border: 1px solid var(--theme-border);"
                      >
                        <Info class="w-3.5 h-3.5 flex-shrink-0 mt-0.5" style="color: var(--theme-text-secondary);" />
                        <div class="flex-1 space-y-1" style="color: var(--theme-text-secondary);">
                          <p><span style="color: var(--theme-text); font-weight: 500;">地域：</span>{{ readerProfile.dataNote.regionNote }}</p>
                          <p><span style="color: var(--theme-text); font-weight: 500;">性别：</span>{{ readerProfile.dataNote.genderNote }}</p>
                          <p><span style="color: var(--theme-text); font-weight: 500;">年龄段：</span>{{ readerProfile.dataNote.ageRangeNote }}</p>
                        </div>
                      </div>
                    </section>
                  </template>
                </div>

                <!-- ============ 文章 ============ -->
                <div v-else-if="activeTab === 'articles'">
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
