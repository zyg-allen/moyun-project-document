<script setup lang="ts">
import { ref, computed, onMounted, watch } from 'vue';
import { RouterLink as Link, useRouter } from 'vue-router';
import { useHead } from '@vueuse/head';
import {
  User,
  Edit,
  LogOut,
  BookOpen,
  Heart,
  Clock,
  FileText,
  Settings,
  ChevronRight,
  Crown,
  Wallet,
  Users,
  Bell,
  ShoppingBag,
  Shield,
  Smartphone,
  MessageSquare,
  Key,
  AlertTriangle,
  Award,
  CheckCircle2,
  Star,
  Zap,
  Flame
} from 'lucide-vue-next';
import { getArticles } from '@/data/mockData';
import { useArticleStore } from '@/stores/article';
import { useUserStore } from '@/stores/user';
import { useAuth } from '@/composables/useAuth';
import { generateSeo } from '@/utils/seo';
import ArticleCard from '@/components/ArticleCard.vue';
import SiteFooter from '@/components/SiteFooter.vue';
import Breadcrumb from '@/components/Breadcrumb.vue';
import Pagination from '@/components/Pagination.vue';
import type { Article, User as UserType } from '@/types';

const router = useRouter();
const articleStore = useArticleStore();
const userStore = useUserStore();
const { requireAuth } = useAuth();

// 数据
const currentUser = ref<UserType | null>(null);
const userArticles = ref<Article[]>([]);
const userDrafts = ref<Article[]>([]);
const activeTab = ref('articles');
const isLoading = ref(false);
const currentPage = ref(1);
const itemsPerPage = ref(10);

// 模拟数据
const userStats = ref({
  following: 128,
  followers: 256,
  todayVisitors: 42,
  totalViews: 12580,
  totalLikes: 892,
  totalArticles: 24
});

const memberInfo = ref({
  level: 'VIP会员',
  levelIcon: 'diamond',
  expireDate: '2026-12-31',
  privileges: ['无限浏览', '专属标识', '优先推荐', '客服优先']
});

const walletInfo = ref({
  balance: 1288.50,
  coupons: 5
});

// 成就徽章数据
const badges = ref([
  { name: '创作新星', desc: '发布10篇文章', icon: Star, color: 'from-yellow-400 to-orange-500' },
  { name: '人气作者', desc: '获得100个赞', icon: Flame, color: 'from-red-400 to-pink-500' },
  { name: '连续打卡', desc: '连续7天打卡', icon: CheckCircle2, color: 'from-green-400 to-emerald-500' },
  { name: '活跃用户', desc: '累计登录30天', icon: Zap, color: 'from-blue-400 to-cyan-500' },
  { name: '精品作者', desc: '3篇文章获推荐', icon: Award, color: 'from-purple-400 to-violet-500' },
  { name: '粉丝达人', desc: '拥有100个粉丝', icon: Users, color: 'from-pink-400 to-rose-500' }
]);

// 模态框状态
const showEditModal = ref(false);
const showRechargeModal = ref(false);
const showSettingsModal = ref(false);
const showToast = ref(false);
const toastMessage = ref('');

// 表单数据
const editProfileForm = ref({
  username: '',
  bio: '',
  email: '',
  position: '高级前端工程师'
});

const rechargeOptions = [
  { amount: 50, bonus: 5 },
  { amount: 100, bonus: 15 },
  { amount: 200, bonus: 40 },
  { amount: 500, bonus: 120 },
  { amount: 1000, bonus: 300 }
];
const selectedRechargeAmount = ref(100);

// SEO
useHead(
  generateSeo({
    title: '个人中心',
    description: '管理你的个人资料、账号设置和安全选项',
    keywords: ['个人中心', '账号设置', '用户中心', '设置'],
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

// 监听标签切换，重置页码
watch(() => activeTab.value, () => {
  currentPage.value = 1;
});

onMounted(async () => {
  // 等待用户状态初始化
  if (!userStore.isUserInitialized) {
    await userStore.initializeUser();
  }

  // 检查是否登录
  if (!userStore.isAuthenticated) {
    requireAuth();
    return;
  }

  // 使用 store 中的用户数据
  const user = userStore.user;
  if (user) {
    currentUser.value = user;
    editProfileForm.value = {
      username: user.username,
      bio: user.bio || '',
      email: user.email,
      position: '高级前端工程师'
    };
  }

  // 获取用户文章
  const allArticles = getArticles();
  userArticles.value = allArticles.filter((_, i) => i % 2 === 0);

  // 模拟草稿数据
  userDrafts.value = [
    {
      id: 'draft-1',
      title: 'Vue 3 组合式 API 技巧分享（未完成）',
      content: '这是一篇未完成的文章...',
      excerpt: '这是一篇未完成的文章草稿...',
      cover: '',
      author: user,
      category: '技术分享',
      tags: ['Vue', '前端'],
      views: 0,
      likes: 0,
      createdAt: '2024-01-20',
      updatedAt: '2024-01-22'
    },
    {
      id: 'draft-2',
      title: 'React 与 Vue 对比分析',
      content: '正在撰写中...',
      excerpt: '正在撰写中...',
      cover: '',
      author: user,
      category: '技术分享',
      tags: ['React', 'Vue', '对比'],
      views: 0,
      likes: 0,
      createdAt: '2024-01-25',
      updatedAt: '2024-01-25'
    }
  ] as Article[];
});

// Toast 提示
function showToastMessage(message: string) {
  toastMessage.value = message;
  showToast.value = true;
  setTimeout(() => {
    showToast.value = false;
  }, 3000);
}

// 退出登录
async function handleLogout() {
  await userStore.logoutWithApi()
  router.push('/')
}

// 保存个人资料
async function saveProfile() {
  if (!editProfileForm.value.username.trim()) {
    showToastMessage('请输入用户名');
    return;
  }

  isLoading.value = true;

  try {
    const result = await userStore.updateUserWithApi(editProfileForm.value);
    if (result.success) {
      showEditModal.value = false;
      showToastMessage('资料更新成功！');
    } else {
      showToastMessage(result.message || '更新失败');
    }
  } catch (error) {
    showToastMessage('更新失败，请稍后重试');
  } finally {
    isLoading.value = false;
  }
}

// 充值
function handleRecharge() {
  isLoading.value = true;

  setTimeout(() => {
    walletInfo.value.balance += selectedRechargeAmount.value;
    showRechargeModal.value = false;
    showToastMessage(`充值成功！获得 ${selectedRechargeAmount.value} 余额`);
    isLoading.value = false;
  }, 800);
}
</script>

<template>
  <div class="min-h-screen flex flex-col" style="background-color: var(--theme-bg);">
    <!-- Toast -->
    <Teleport to="body">
      <div v-if="showToast" class="fixed top-4 right-4 z-50 px-4 py-3 rounded-lg shadow-lg" style="background-color: var(--theme-primary); color: white;">
        {{ toastMessage }}
      </div>
    </Teleport>

    <!-- 面包屑 - 与列表页和详情页一致 -->
    <div class="border-b py-3 sm:py-4" style="background-color: var(--theme-bg); border-color: var(--theme-border);">
      <div class="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8">
        <div class="flex items-center justify-between gap-4">
          <Breadcrumb :items="[{ label: '首页', path: '/' }, { label: '个人中心' }]" />
          <div class="flex gap-3">
            <button
              @click="showSettingsModal = true"
              class="flex items-center gap-2 px-4 py-2 rounded-xl text-sm font-medium transition-colors"
              style="background-color: var(--theme-surface); border: 1px solid var(--theme-border); color: var(--theme-text);"
            >
              <Settings class="w-4 h-4" />
              设置
            </button>
            <button
              @click="handleLogout"
              class="flex items-center gap-2 px-4 py-2 rounded-xl text-sm font-medium transition-colors"
              style="background-color: #fef2f2; border: 1px solid #fecaca; color: #dc2626;"
            >
              <LogOut class="w-4 h-4" />
              退出
            </button>
          </div>
        </div>
      </div>
    </div>

    <!-- 主内容区域 - 与列表页和详情页保持一致的布局结构 -->
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
                <div class="flex-shrink-0">
                  <div class="relative">
                    <div class="w-24 h-24 sm:w-32 sm:h-32 rounded-2xl overflow-hidden">
                      <img :src="currentUser.avatar" :alt="currentUser.username" class="w-full h-full object-cover">
                    </div>
                    <button
                      @click="showEditModal = true"
                      class="absolute -bottom-2 -right-2 w-10 h-10 rounded-xl flex items-center justify-center shadow-lg"
                      style="background-color: var(--theme-primary);"
                    >
                      <Edit class="w-5 h-5 text-white" />
                    </button>
                  </div>
                </div>

                <!-- 用户信息 -->
                <div class="flex-1 min-w-0">
                  <div class="flex items-start justify-between gap-4">
                    <div class="min-w-0">
                      <h1 class="text-2xl sm:text-3xl font-bold mb-2" style="color: var(--theme-text);">{{ currentUser.username }}</h1>
                      <p class="text-sm sm:text-base mb-2" style="color: var(--theme-text-secondary);">{{ editProfileForm.position }}</p>
                      <p class="text-sm sm:text-base mb-3" style="color: var(--theme-text-secondary);">{{ currentUser.bio || '这个人很懒，什么都没写~' }}</p>
                      <div class="flex items-center gap-3 flex-wrap">
                        <span class="text-xs sm:text-sm px-3 py-1 rounded-full" style="background-color: var(--theme-accent); color: var(--theme-text-secondary);">
                          {{ currentUser.email }}
                        </span>
                      </div>
                    </div>
                  </div>
                </div>
              </div>

              <!-- 统计数据 -->
              <div class="mt-8 grid grid-cols-2 sm:grid-cols-3 lg:grid-cols-6 gap-4">
                <div class="text-center p-4 rounded-xl" style="background-color: var(--theme-accent);">
                  <div class="text-xl sm:text-2xl font-bold mb-1" style="color: var(--theme-text);">{{ userStats.totalArticles }}</div>
                  <div class="text-xs sm:text-sm" style="color: var(--theme-text-secondary);">文章</div>
                </div>
                <div class="text-center p-4 rounded-xl" style="background-color: var(--theme-accent);">
                  <div class="text-xl sm:text-2xl font-bold mb-1" style="color: var(--theme-text);">{{ userStats.totalViews }}</div>
                  <div class="text-xs sm:text-sm" style="color: var(--theme-text-secondary);">浏览</div>
                </div>
                <div class="text-center p-4 rounded-xl" style="background-color: var(--theme-accent);">
                  <div class="text-xl sm:text-2xl font-bold mb-1" style="color: var(--theme-text);">{{ userStats.totalLikes }}</div>
                  <div class="text-xs sm:text-sm" style="color: var(--theme-text-secondary);">获赞</div>
                </div>
                <div class="text-center p-4 rounded-xl" style="background-color: var(--theme-accent);">
                  <div class="text-xl sm:text-2xl font-bold mb-1" style="color: var(--theme-text);">{{ userStats.following }}</div>
                  <div class="text-xs sm:text-sm" style="color: var(--theme-text-secondary);">关注</div>
                </div>
                <div class="text-center p-4 rounded-xl" style="background-color: var(--theme-accent);">
                  <div class="text-xl sm:text-2xl font-bold mb-1" style="color: var(--theme-text);">{{ userStats.followers }}</div>
                  <div class="text-xs sm:text-sm" style="color: var(--theme-text-secondary);">粉丝</div>
                </div>
                <div class="text-center p-4 rounded-xl" style="background-color: var(--theme-accent);">
                  <div class="text-xl sm:text-2xl font-bold mb-1" style="color: var(--theme-text);">{{ userStats.todayVisitors }}</div>
                  <div class="text-xs sm:text-sm" style="color: var(--theme-text-secondary);">今日访客</div>
                </div>
              </div>

              <!-- 会员与余额 -->
              <div class="mt-6 grid grid-cols-1 sm:grid-cols-2 gap-4">
                <!-- 会员信息 -->
                <div class="p-4 rounded-xl" style="background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);">
                  <div class="flex items-center gap-3">
                    <Crown class="w-8 h-8 text-yellow-300" />
                    <div class="flex-1 min-w-0">
                      <h3 class="font-semibold text-white">{{ memberInfo.level }}</h3>
                      <p class="text-xs text-white/80">到期时间：{{ memberInfo.expireDate }}</p>
                    </div>
                    <button class="px-4 py-2 rounded-lg bg-white/20 hover:bg-white/30 text-white text-sm font-medium transition-colors">
                      续费
                    </button>
                  </div>
                </div>

                <!-- 余额信息 -->
                <div class="p-4 rounded-xl" style="background-color: var(--theme-accent); border: 1px solid var(--theme-border);">
                  <div class="flex items-center gap-3">
                    <Wallet class="w-8 h-8" style="color: var(--theme-primary);" />
                    <div class="flex-1 min-w-0">
                      <div class="flex items-center gap-2 mb-1">
                        <h3 class="font-semibold" style="color: var(--theme-text);">我的余额</h3>
                        <span class="text-xs px-2 py-0.5 rounded-full" style="background-color: var(--theme-primary); color: white;">{{ walletInfo.coupons }}张优惠券</span>
                      </div>
                      <p class="text-2xl font-bold" style="color: var(--theme-text);">¥{{ walletInfo.balance.toFixed(2) }}</p>
                    </div>
                    <button
                      @click="showRechargeModal = true"
                      class="px-4 py-2 rounded-lg text-sm font-medium transition-colors"
                      style="background-color: var(--theme-primary); color: white;"
                    >
                      充值
                    </button>
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
                  { id: 'drafts', label: '草稿箱', icon: FileText },
                  { id: 'saved', label: '我的收藏', icon: Heart },
                  { id: 'social', label: '关注粉丝', icon: Users }
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
                <Pagination
                  :current-page="currentPage"
                  :total-pages="totalPages"
                  :total-items="userArticles.length"
                  :items-per-page="itemsPerPage"
                  @update:current-page="currentPage = $event"
                />
              </div>
            </div>

            <!-- 草稿箱 -->
            <div v-else-if="activeTab === 'drafts'">
              <div class="flex items-center justify-between mb-6">
                <h2 class="text-xl sm:text-2xl font-bold" style="color: var(--theme-text);">草稿箱</h2>
                <Link to="/publish" target="_blank" class="inline-flex items-center gap-2 px-4 sm:px-5 py-2 sm:py-2.5 rounded-xl font-medium hover:opacity-90 transition-colors text-sm" style="background-color: var(--theme-primary); color: white;">
                  <Edit class="w-4 h-4" />
                  写新文章
                </Link>
              </div>

              <div v-if="userDrafts.length > 0" class="space-y-4 sm:space-y-6">
                <div
                  v-for="draft in userDrafts"
                  :key="draft.id"
                  class="p-4 sm:p-6 rounded-2xl"
                  style="background-color: var(--theme-surface); border: 1px solid var(--theme-border);"
                >
                  <div class="flex items-start justify-between gap-4">
                    <div class="flex-1 min-w-0">
                      <h3 class="text-base sm:text-lg font-medium mb-2" style="color: var(--theme-text);">{{ draft.title }}</h3>
                      <p class="text-xs sm:text-sm mb-3" style="color: var(--theme-text-secondary);">{{ draft.excerpt }}</p>
                      <div class="flex items-center gap-3 text-xs" style="color: var(--theme-text-secondary);">
                        <span>最后更新: {{ draft.updatedAt }}</span>
                      </div>
                    </div>
                    <div class="flex gap-2 flex-shrink-0">
                      <Link
                        to="/publish"
                        target="_blank"
                        class="px-3 py-1.5 rounded-lg text-xs sm:text-sm font-medium transition-colors"
                        style="background-color: var(--theme-primary); color: white;"
                      >
                        继续编辑
                      </Link>
                      <button
                        class="px-3 py-1.5 rounded-lg text-xs sm:text-sm font-medium border transition-colors"
                        style="border-color: var(--theme-border); color: var(--theme-text-secondary);"
                      >
                        删除
                      </button>
                    </div>
                  </div>
                </div>
              </div>
              <div v-else class="p-8 sm:p-12 rounded-2xl text-center" style="background-color: var(--theme-surface); border: 1px solid var(--theme-border);">
                <FileText class="w-12 h-12 sm:w-16 sm:h-16 mx-auto mb-4" style="color: var(--theme-text-secondary);" />
                <h3 class="text-lg font-medium mb-2" style="color: var(--theme-text);">暂无草稿</h3>
                <p class="mb-6" style="color: var(--theme-text-secondary);">开始创作，你的草稿会自动保存在这里</p>
                <Link to="/publish" target="_blank" class="inline-flex items-center gap-2 px-5 py-2.5 rounded-xl font-medium hover:opacity-90 transition-colors text-sm" style="background-color: var(--theme-primary); color: white;">
                  开始创作
                  <ChevronRight class="w-4 h-4" />
                </Link>
              </div>
            </div>

            <!-- 我的收藏 -->
            <div v-else-if="activeTab === 'saved'">
              <h2 class="text-xl sm:text-2xl font-bold mb-6" style="color: var(--theme-text);">我的收藏</h2>

              <div v-if="articleStore.bookmarkedArticles.length > 0" class="space-y-4 sm:space-y-6">
                <ArticleCard v-for="article in articleStore.bookmarkedArticles" :key="article.id" :article="article" />
              </div>
              <div v-else class="p-8 sm:p-12 rounded-2xl text-center" style="background-color: var(--theme-surface); border: 1px solid var(--theme-border);">
                <Heart class="w-12 h-12 sm:w-16 sm:h-16 mx-auto mb-4" style="color: var(--theme-text-secondary);" />
                <h3 class="text-lg font-medium mb-2" style="color: var(--theme-text);">收藏夹为空</h3>
                <p style="color: var(--theme-text-secondary);">浏览文章并收藏你喜欢的内容</p>
              </div>
            </div>

            <!-- 关注与粉丝 -->
            <div v-else-if="activeTab === 'social'">
              <h2 class="text-xl sm:text-2xl font-bold mb-6" style="color: var(--theme-text);">关注与粉丝</h2>

              <div class="space-y-4 sm:space-y-6">
                <!-- 关注列表 -->
                <div class="p-4 sm:p-6 rounded-2xl" style="background-color: var(--theme-surface); border: 1px solid var(--theme-border);">
                  <h3 class="font-semibold mb-4" style="color: var(--theme-text);">我的关注 ({{ userStats.following }})</h3>
                  <div class="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-3 gap-4">
                    <div v-for="i in 6" :key="i" class="flex items-center gap-3 p-3 rounded-xl" style="background-color: var(--theme-bg);">
                      <div class="w-10 h-10 rounded-full bg-gradient-to-br from-purple-400 to-pink-500"></div>
                      <div class="min-w-0 flex-1">
                        <p class="text-sm font-medium truncate" style="color: var(--theme-text);">用户{{ i }}</p>
                        <p class="text-xs" style="color: var(--theme-text-secondary);">前端开发者</p>
                      </div>
                      <button class="text-xs px-3 py-1 rounded-full border transition-colors" style="border-color: var(--theme-border); color: var(--theme-primary);">
                        已关注
                      </button>
                    </div>
                  </div>
                </div>

                <!-- 粉丝列表 -->
                <div class="p-4 sm:p-6 rounded-2xl" style="background-color: var(--theme-surface); border: 1px solid var(--theme-border);">
                  <h3 class="font-semibold mb-4" style="color: var(--theme-text);">我的粉丝 ({{ userStats.followers }})</h3>
                  <div class="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-3 gap-4">
                    <div v-for="i in 6" :key="i" class="flex items-center gap-3 p-3 rounded-xl" style="background-color: var(--theme-bg);">
                      <div class="w-10 h-10 rounded-full bg-gradient-to-br from-blue-400 to-cyan-500"></div>
                      <div class="min-w-0 flex-1">
                        <p class="text-sm font-medium truncate" style="color: var(--theme-text);">粉丝{{ i }}</p>
                        <p class="text-xs" style="color: var(--theme-text-secondary);">UI设计师</p>
                      </div>
                      <button class="text-xs px-3 py-1 rounded-full transition-colors" style="background-color: var(--theme-primary); color: white;">
                        关注
                      </button>
                    </div>
                  </div>
                </div>

                <!-- 今日访客 -->
                <div class="p-4 sm:p-6 rounded-2xl" style="background-color: var(--theme-surface); border: 1px solid var(--theme-border);">
                  <h3 class="font-semibold mb-4" style="color: var(--theme-text);">今日访客 ({{ userStats.todayVisitors }})</h3>
                  <div class="grid grid-cols-3 sm:grid-cols-4 lg:grid-cols-6 gap-4">
                    <div v-for="i in 12" :key="i" class="text-center">
                      <div class="w-12 h-12 sm:w-14 sm:h-14 mx-auto mb-1 rounded-full bg-gradient-to-br from-orange-400 to-red-500"></div>
                      <p class="text-xs truncate" style="color: var(--theme-text);">访客{{ i }}</p>
                    </div>
                  </div>
                </div>
              </div>
            </div>
          </div>
        </template>
      </div>
    </div>

    <!-- Footer -->
    <SiteFooter />

    <!-- Modals -->
    <!-- 编辑个人资料模态框 -->
    <Teleport to="body">
      <div v-if="showEditModal" class="fixed inset-0 z-50 flex items-center justify-center p-4" style="background-color: rgba(0, 0, 0, 0.5);">
        <div class="rounded-2xl w-full max-w-md p-6" style="background-color: var(--theme-bg);">
          <h3 class="text-lg font-semibold mb-4" style="color: var(--theme-text);">编辑个人资料</h3>
          <div class="space-y-4">
            <div>
              <label class="block text-sm font-medium mb-1" style="color: var(--theme-text);">用户名</label>
              <input
                v-model="editProfileForm.username"
                type="text"
                class="w-full px-4 py-2 rounded-lg border focus:outline-none focus:ring-2"
                style="background-color: var(--theme-surface); border-color: var(--theme-border); color: var(--theme-text);"
              >
            </div>
            <div>
              <label class="block text-sm font-medium mb-1" style="color: var(--theme-text);">职位</label>
              <input
                v-model="editProfileForm.position"
                type="text"
                class="w-full px-4 py-2 rounded-lg border focus:outline-none focus:ring-2"
                style="background-color: var(--theme-surface); border-color: var(--theme-border); color: var(--theme-text);"
              >
            </div>
            <div>
              <label class="block text-sm font-medium mb-1" style="color: var(--theme-text);">邮箱</label>
              <input
                v-model="editProfileForm.email"
                type="email"
                class="w-full px-4 py-2 rounded-lg border focus:outline-none focus:ring-2"
                style="background-color: var(--theme-surface); border-color: var(--theme-border); color: var(--theme-text);"
              >
            </div>
            <div>
              <label class="block text-sm font-medium mb-1" style="color: var(--theme-text);">个人简介</label>
              <textarea
                v-model="editProfileForm.bio"
                rows="3"
                class="w-full px-4 py-2 rounded-lg border focus:outline-none focus:ring-2"
                style="background-color: var(--theme-surface); border-color: var(--theme-border); color: var(--theme-text);"
              ></textarea>
            </div>
          </div>
          <div class="flex gap-3 mt-6">
            <button
              @click="showEditModal = false"
              class="flex-1 px-4 py-2 rounded-lg font-medium border transition-colors"
              style="border-color: var(--theme-border); color: var(--theme-text-secondary);"
            >
              取消
            </button>
            <button
              @click="saveProfile"
              :disabled="isLoading"
              class="flex-1 px-4 py-2 rounded-lg font-medium transition-colors"
              style="background-color: var(--theme-primary); color: white;"
            >
              {{ isLoading ? '保存中...' : '保存' }}
            </button>
          </div>
        </div>
      </div>
    </Teleport>

    <!-- 充值模态框 -->
    <Teleport to="body">
      <div v-if="showRechargeModal" class="fixed inset-0 z-50 flex items-center justify-center p-4" style="background-color: rgba(0, 0, 0, 0.5);">
        <div class="rounded-2xl w-full max-w-md p-6" style="background-color: var(--theme-bg);">
          <h3 class="text-lg font-semibold mb-4" style="color: var(--theme-text);">账户充值</h3>
          <div class="mb-4">
            <p class="text-sm mb-2" style="color: var(--theme-text-secondary);">当前余额</p>
            <p class="text-2xl font-bold" style="color: var(--theme-text);">¥{{ walletInfo.balance.toFixed(2) }}</p>
          </div>
          <div class="grid grid-cols-3 gap-3 mb-6">
            <button
              v-for="option in rechargeOptions"
              :key="option.amount"
              @click="selectedRechargeAmount = option.amount"
              class="p-3 rounded-xl border-2 text-center transition-colors"
              :style="selectedRechargeAmount === option.amount ? 'border-color: var(--theme-primary); background-color: var(--theme-accent);' : 'border-color: var(--theme-border);'"
            >
              <p class="font-semibold" style="color: var(--theme-text);">¥{{ option.amount }}</p>
              <p class="text-xs" style="color: var(--theme-primary);">+送{{ option.bonus }}</p>
            </button>
          </div>
          <div class="flex gap-3">
            <button
              @click="showRechargeModal = false"
              class="flex-1 px-4 py-2 rounded-lg font-medium border transition-colors"
              style="border-color: var(--theme-border); color: var(--theme-text-secondary);"
            >
              取消
            </button>
            <button
              @click="handleRecharge"
              :disabled="isLoading"
              class="flex-1 px-4 py-2 rounded-lg font-medium transition-colors"
              style="background-color: var(--theme-primary); color: white;"
            >
              {{ isLoading ? '充值中...' : '立即充值' }}
            </button>
          </div>
        </div>
      </div>
    </Teleport>

    <!-- 设置模态框 -->
    <Teleport to="body">
      <div v-if="showSettingsModal" class="fixed inset-0 z-50 flex items-center justify-center p-4" style="background-color: rgba(0, 0, 0, 0.5);">
        <div class="rounded-2xl w-full max-w-lg max-h-[80vh] overflow-y-auto p-6" style="background-color: var(--theme-bg);">
          <div class="flex items-center justify-between mb-6">
            <h3 class="text-lg font-semibold" style="color: var(--theme-text);">设置</h3>
            <button @click="showSettingsModal = false" class="p-2 rounded-lg hover:bg-gray-100 dark:hover:bg-gray-800">
              <svg class="w-5 h-5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M6 18L18 6M6 6l12 12" />
              </svg>
            </button>
          </div>

          <div class="space-y-4">
            <!-- 通知设置 -->
            <div class="p-4 rounded-xl" style="background-color: var(--theme-surface); border: 1px solid var(--theme-border);">
              <h4 class="font-medium mb-3" style="color: var(--theme-text);">通知设置</h4>
              <div class="space-y-3">
                <label class="flex items-center justify-between">
                  <span class="text-sm" style="color: var(--theme-text);">新评论通知</span>
                  <input type="checkbox" checked class="w-4 h-4 rounded" style="color: var(--theme-primary);">
                </label>
                <label class="flex items-center justify-between">
                  <span class="text-sm" style="color: var(--theme-text);">点赞通知</span>
                  <input type="checkbox" checked class="w-4 h-4 rounded" style="color: var(--theme-primary);">
                </label>
                <label class="flex items-center justify-between">
                  <span class="text-sm" style="color: var(--theme-text);">系统通知</span>
                  <input type="checkbox" class="w-4 h-4 rounded" style="color: var(--theme-primary);">
                </label>
              </div>
            </div>

            <!-- 隐私设置 -->
            <div class="p-4 rounded-xl" style="background-color: var(--theme-surface); border: 1px solid var(--theme-border);">
              <h4 class="font-medium mb-3" style="color: var(--theme-text);">隐私设置</h4>
              <div class="space-y-3">
                <label class="flex items-center justify-between">
                  <span class="text-sm" style="color: var(--theme-text);">公开我的收藏</span>
                  <input type="checkbox" class="w-4 h-4 rounded" style="color: var(--theme-primary);">
                </label>
                <label class="flex items-center justify-between">
                  <span class="text-sm" style="color: var(--theme-text);">允许他人关注我</span>
                  <input type="checkbox" checked class="w-4 h-4 rounded" style="color: var(--theme-primary);">
                </label>
              </div>
            </div>

            <!-- 安全设置 -->
            <div class="p-4 rounded-xl" style="background-color: var(--theme-surface); border: 1px solid var(--theme-border);">
              <h4 class="font-medium mb-3" style="color: var(--theme-text);">安全设置</h4>
              <div class="space-y-3">
                <button class="w-full flex items-center justify-between p-3 rounded-lg" style="background-color: var(--theme-accent);">
                  <span class="text-sm" style="color: var(--theme-text);">修改密码</span>
                  <ChevronRight class="w-4 h-4" style="color: var(--theme-text-secondary);" />
                </button>
                <button class="w-full flex items-center justify-between p-3 rounded-lg" style="background-color: var(--theme-accent);">
                  <span class="text-sm" style="color: var(--theme-text);">账号绑定</span>
                  <ChevronRight class="w-4 h-4" style="color: var(--theme-text-secondary);" />
                </button>
              </div>
            </div>

            <!-- 危险区域 -->
            <div class="p-4 rounded-xl border-2" style="border-color: #ef4444; background-color: #fef2f2;">
              <div class="flex items-start gap-4">
                <div class="w-10 h-10 rounded-xl flex items-center justify-center bg-red-100 flex-shrink-0">
                  <AlertTriangle class="w-5 h-5 text-red-600" />
                </div>
                <div class="flex-1 min-w-0">
                  <h4 class="font-medium mb-2 text-red-700">注销账号</h4>
                  <p class="text-sm text-red-600 mb-4">注销账号后，你的所有数据将被永久删除，无法恢复。</p>
                  <button class="px-4 py-2 rounded-lg text-sm font-medium bg-red-600 text-white hover:bg-red-700">
                    注销账号
                  </button>
                </div>
              </div>
            </div>
          </div>
        </div>
      </div>
    </Teleport>
  </div>
</template>
