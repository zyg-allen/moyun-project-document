<script setup lang="ts">
import { ref, computed, onMounted, watch } from 'vue';
import Pagination from '@/components/Pagination.vue';
import { RouterLink as Link, useRoute, useRouter } from 'vue-router';
import { useHead } from '@vueuse/head';
import { 
  User, BookOpen, Heart, Star, Users, ChevronRight, UserPlus, UserMinus, MessageSquare, Calendar, Clock, Award, Trophy
} from 'lucide-vue-next';
import { generateSeo } from '@/utils/seo';
import ArticleCard from '@/components/ArticleCard.vue';
import SiteFooter from '@/components/SiteFooter.vue';
import Breadcrumb from '@/components/Breadcrumb.vue';
import type { Article, User as UserType, UserGrowthVO, AchievementVO } from '@/types';
import * as userApi from '@/api/user';
import * as articleApi from '@/api/article';
import * as followApi from '@/api/follow';
import * as growthApi from '@/api/growth';
import { getSafeAvatar } from '@/utils/avatar';

const route = useRoute();
const router = useRouter();

// 数据
const author = ref<UserType | null>(null);
const authorArticles = ref<Article[]>([]);
const authorGrowth = ref<UserGrowthVO | null>(null);
const authorAchievements = ref<AchievementVO[]>([]);
const activeTab = ref('articles');
const currentPage = ref(1);
const itemsPerPage = ref(10);
const isFollowing = ref(false);
const isLoading = ref(false);
const notFound = ref(false);
const currentUser = ref<UserType | null>(null);
const loadingArticles = ref(false);

// 真实统计数据
const authorStats = ref({
  following: 0,
  followers: 0,
  articles: 0,
  totalViews: 0,
  totalLikes: 0,
  joinDate: ''
});

// 检查是否是自己的主页
const isOwnProfile = computed(() => {
  return currentUser.value && author.value && String(currentUser.value.id) === String(author.value.id);
});

// SEO
useHead(
  generateSeo({
    title: author.value ? `${author.value.username}的主页` : '用户主页',
    description: author.value ? author.value.bio : '查看用户的文章和资料',
    keywords: ['用户主页', '作者', '文章'],
    type: 'website'
  })
);

onMounted(() => {
  loadAuthorData();
});

watch(() => activeTab.value, () => {
  currentPage.value = 1;
});

async function loadAuthorData() {
  // 获取当前登录用户
  try {
    const userResp = await userApi.getCurrentUser();
    if (userResp.code === 200 && userResp.data) {
      currentUser.value = userResp.data;
    }
  } catch (e) {
    // 未登录也不影响查看作者主页
  }

  // 获取作者ID
  const authorId = route.params.id as string;

  isLoading.value = true;
  try {
    // 获取作者基本信息
    const authorResp = await userApi.getUserById(authorId);
    if (authorResp.code === 200 && authorResp.data) {
      author.value = authorResp.data;
      authorStats.value.joinDate = (author.value as any).createTime || (author.value as any).createdAt || '';
    } else {
      notFound.value = true;
      return;
    }

    // 获取作者文章列表
    loadingArticles.value = true;
    try {
      const articlesResp = await articleApi.getArticleList({ authorId: authorId, pageSize: 100 });
      if (articlesResp.code === 200 && articlesResp.data) {
        const list = (articlesResp.data as any).list || articlesResp.data || [];
        authorArticles.value = list;
        // 文章数先以列表为准，后续 stats 接口会覆盖真实值
        authorStats.value.articles = list.length;
      }
    } catch (err) {
      console.error('加载作者文章失败:', err);
    } finally {
      loadingArticles.value = false;
    }

    // 获取作者统计信息（使用 /portal/growth/stats 真实聚合接口）
    try {
      const statsResp = await growthApi.getUserStatsById(authorId);
      if (statsResp.code === 200 && statsResp.data) {
        const stats = statsResp.data as any;
        authorStats.value.followers = stats.followers || stats.fansCount || stats.followerCount || authorStats.value.followers;
        authorStats.value.following = stats.following || stats.followCount || authorStats.value.following;
        // 后端 UserStatsVO 字段：articles / views / likes / totalLikes
        if (stats.articles !== undefined) authorStats.value.articles = stats.articles;
        if (stats.views !== undefined) authorStats.value.totalViews = stats.views;
        if (stats.totalLikes !== undefined) {
          authorStats.value.totalLikes = stats.totalLikes;
        } else if (stats.likes !== undefined) {
          authorStats.value.totalLikes = stats.likes;
        }
      }
    } catch (err) {
      console.error('加载作者统计失败:', err);
    }

    // 获取作者成长信息（等级/头衔/赛季排名）
    try {
      const growthResp = await growthApi.getUserGrowth(authorId);
      if (growthResp.code === 200 && growthResp.data) {
        authorGrowth.value = growthResp.data;
      }
    } catch (err) {
      console.error('加载作者成长信息失败:', err);
    }

    // 获取作者成就达成列表
    try {
      const achResp = await growthApi.getUserAchievements(authorId);
      if (achResp.code === 200 && achResp.data) {
        authorAchievements.value = achResp.data;
      }
    } catch (err) {
      console.error('加载作者成就失败:', err);
    }
  } catch (error) {
    console.error('加载作者信息失败:', error);
    router.push('/404');
  } finally {
    isLoading.value = false;
  }
}

// 切换关注状态
async function toggleFollow() {
  if (!author.value || !currentUser.value) return;
  
  try {
    if (isFollowing.value) {
      await followApi.unfollowUser({ userId: author.value.id });
      isFollowing.value = false;
      authorStats.value.followers--;
    } else {
      await followApi.followUser({ userId: author.value.id });
      isFollowing.value = true;
      authorStats.value.followers++;
    }
  } catch (error) {
    console.error('操作失败:', error);
    // 本地模拟
    isFollowing.value = !isFollowing.value;
    if (isFollowing.value) {
      authorStats.value.followers++;
    } else {
      authorStats.value.followers--;
    }
  }
}

// 跳转到自己的个人中心
function goToOwnProfile() {
  router.push('/user');
}

const totalItems = computed(() => authorArticles.value.length);
const totalPages = computed(() => Math.ceil(totalItems.value / itemsPerPage.value));

const paginatedArticles = computed(() => {
  const start = (currentPage.value - 1) * itemsPerPage.value;
  const end = start + itemsPerPage.value;
  return authorArticles.value.slice(start, end);
});

// 已达成的成就列表
const earnedAchievements = computed(() => authorAchievements.value.filter(a => a.earned));
const achievementProgress = computed(() => {
  const total = authorAchievements.value.length;
  const earned = earnedAchievements.value.length;
  return {
    total,
    earned,
    percent: total > 0 ? Math.round((earned / total) * 100) : 0
  };
});

function handlePageChange(page: number) {
  currentPage.value = page;
  window.scrollTo({ top: 0, behavior: 'smooth' });
}
</script>

<template>
  <div class="min-h-screen flex flex-col" style="background-color: var(--theme-bg);">
    <!-- 面包屑 -->
    <div class="border-b py-3 sm:py-4" style="background-color: var(--theme-bg); border-color: var(--theme-border);">
      <div class="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8">
        <div class="flex items-center justify-between gap-4">
          <Breadcrumb :items="[{ label: '首页', path: '/' }, { label: author?.username || '用户主页' }]" />
        </div>
      </div>
    </div>

    <!-- 主内容区域 - 与列表页和详情页保持一致的布局结构 -->
    <div class="py-8 flex-1">
      <div class="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8">
        <!-- 加载状态 -->
        <div v-if="isLoading" class="text-center py-12">
          <div class="inline-block w-12 h-12 border-4 border-t-4 border-gray-300 rounded-full animate-spin" style="border-top-color: var(--theme-primary);"></div>
          <p class="mt-4" style="color: var(--theme-text-secondary);">加载中...</p>
        </div>

        <!-- 用户不存在 -->
        <div v-else-if="notFound" class="text-center py-16">
          <p class="text-5xl mb-4">404</p>
          <p class="text-lg mb-2" style="color: var(--theme-text);">该用户不存在</p>
          <p class="text-sm mb-6" style="color: var(--theme-text-secondary);">可能该用户已注销或账号已迁移</p>
          <button @click="router.push('/')" class="px-6 py-2 rounded-lg text-white" style="background-color: var(--theme-primary);">返回首页</button>
        </div>

        <template v-else-if="author">
        <!-- 用户头部信息 -->
        <div class="mb-8">
          <div class="rounded-2xl p-6 sm:p-8" style="background-color: var(--theme-surface); border: 1px solid var(--theme-border);">
            <div class="flex flex-col lg:flex-row gap-6 items-start">
              <!-- 头像 -->
              <div class="flex-shrink-0">
                <div class="w-24 h-24 sm:w-32 sm:h-32 rounded-2xl overflow-hidden">
                  <img 
                    :src="getSafeAvatar(author.avatar, author.id)" 
                    :alt="author.username" 
                    class="w-full h-full object-cover"
                    @error="(e: Event) => (e.target as HTMLImageElement).src = getSafeAvatar(null, author.id)"
                  >
                </div>
              </div>
              
              <!-- 基本信息 -->
              <div class="flex-1 min-w-0">
                <div class="flex flex-col sm:flex-row sm:items-start sm:justify-between gap-4 mb-4">
                  <div class="min-w-0">
                    <div class="flex items-center gap-2 sm:gap-3 flex-wrap mb-2">
                      <h1 class="text-2xl sm:text-3xl font-bold" style="color: var(--theme-text);">{{ author.username }}</h1>
                      <!-- 成长等级徽章 -->
                      <span
                        v-if="authorGrowth"
                        class="inline-flex items-center gap-1 px-2.5 py-1 rounded-full text-xs font-medium"
                        style="background: linear-gradient(135deg, #667eea 0%, #764ba2 100%); color: white;"
                      >
                        <Award class="w-3.5 h-3.5" />
                        Lv.{{ authorGrowth.level }} {{ authorGrowth.title }}
                      </span>
                    </div>
                    <p class="text-sm sm:text-base mb-3" style="color: var(--theme-text-secondary);">{{ author.bio || '这个人很懒，什么都没写~' }}</p>
                    <div class="flex items-center gap-4 text-xs sm:text-sm flex-wrap" style="color: var(--theme-text-secondary);">
                      <div class="flex items-center gap-1">
                        <Calendar class="w-4 h-4" />
                        <span>加入于 {{ authorStats.joinDate }}</span>
                      </div>
                      <div v-if="authorGrowth" class="flex items-center gap-1">
                        <Trophy class="w-4 h-4" />
                        <span>成长值 {{ authorGrowth.growthValue }}</span>
                      </div>
                      <div v-if="authorGrowth && authorGrowth.seasonRank" class="flex items-center gap-1">
                        <Trophy class="w-4 h-4" />
                        <span>赛季排名 #{{ authorGrowth.seasonRank }}</span>
                      </div>
                    </div>
                  </div>
                  
                  <!-- 操作按钮 -->
                  <div class="flex gap-3 flex-shrink-0">
                    <template v-if="isOwnProfile">
                      <button 
                        @click="goToOwnProfile"
                        class="px-4 sm:px-5 py-2.5 rounded-xl font-medium transition-colors text-sm flex items-center gap-2"
                        style="background-color: var(--theme-primary); color: white;"
                      >
                        <User class="w-4 h-4" />
                        管理个人中心
                      </button>
                    </template>
                    <template v-else>
                      <button 
                        @click="toggleFollow"
                        class="px-4 sm:px-5 py-2.5 rounded-xl font-medium transition-colors text-sm flex items-center gap-2"
                        :style="isFollowing 
                          ? { border: '1px solid var(--theme-border)', color: 'var(--theme-text-secondary)' }
                          : { backgroundColor: 'var(--theme-primary)', color: 'white' }"
                      >
                        <component :is="isFollowing ? UserMinus : UserPlus" class="w-4 h-4" />
                        {{ isFollowing ? '已关注' : '关注' }}
                      </button>
                      <button 
                        class="px-4 sm:px-5 py-2.5 rounded-xl font-medium border transition-colors text-sm flex items-center gap-2"
                        style="border-color: var(--theme-border); color: var(--theme-text);"
                      >
                        <MessageSquare class="w-4 h-4" />
                        私信
                      </button>
                    </template>
                  </div>
                </div>
                
                <!-- 统计数据 -->
                <div class="flex flex-wrap gap-6 sm:gap-8 pt-4 border-t" style="border-color: var(--theme-border);">
                  <div class="text-center">
                    <div class="text-xl sm:text-2xl font-bold" style="color: var(--theme-text);">{{ authorStats.articles }}</div>
                    <div class="text-xs sm:text-sm" style="color: var(--theme-text-secondary);">文章</div>
                  </div>
                  <div class="text-center">
                    <div class="text-xl sm:text-2xl font-bold" style="color: var(--theme-text);">{{ authorStats.totalViews }}</div>
                    <div class="text-xs sm:text-sm" style="color: var(--theme-text-secondary);">总浏览</div>
                  </div>
                  <div class="text-center">
                    <div class="text-xl sm:text-2xl font-bold" style="color: var(--theme-text);">{{ authorStats.totalLikes }}</div>
                    <div class="text-xs sm:text-sm" style="color: var(--theme-text-secondary);">获赞</div>
                  </div>
                  <div class="text-center">
                    <div class="text-xl sm:text-2xl font-bold" style="color: var(--theme-text);">{{ authorStats.followers }}</div>
                    <div class="text-xs sm:text-sm" style="color: var(--theme-text-secondary);">粉丝</div>
                  </div>
                  <div class="text-center">
                    <div class="text-xl sm:text-2xl font-bold" style="color: var(--theme-text);">{{ authorStats.following }}</div>
                    <div class="text-xs sm:text-sm" style="color: var(--theme-text-secondary);">关注</div>
                  </div>
                </div>
              </div>
            </div>
          </div>
        </div>

        <!-- 页签导航 -->
        <div v-if="!isLoading && author" class="mb-6">
          <div class="flex gap-2 sm:gap-4 overflow-x-auto pb-2" style="border-bottom: 1px solid var(--theme-border);">
            <button 
              v-for="tab in [
                { id: 'articles', label: '文章', icon: BookOpen },
                { id: 'liked', label: '获赞', icon: Heart },
                { id: 'achievements', label: '成就', icon: Star }
              ]" 
              :key="tab.id"
              @click="activeTab = tab.id"
              class="flex items-center gap-2 px-4 sm:px-6 py-3 rounded-t-xl font-medium text-sm sm:text-base transition-colors whitespace-nowrap"
              :style="activeTab === tab.id 
                ? { backgroundColor: 'var(--theme-surface)', color: 'var(--theme-primary)', borderBottom: '2px solid var(--theme-primary)' }
                : { color: 'var(--theme-text-secondary)' }"
            >
              <component :is="tab.icon" class="w-4 h-4 sm:w-5 sm:h-5" />
              {{ tab.label }}
            </button>
          </div>
        </div>

        <!-- 页签内容 -->
        <div v-if="!isLoading && author" class="rounded-2xl overflow-hidden" style="background-color: var(--theme-bg); border: 1px solid var(--theme-border);">
          <!-- 文章 Tab -->
          <div v-if="activeTab === 'articles'" class="p-4 sm:p-6">
            <div v-if="loadingArticles" class="text-center py-8">
              <div class="inline-block w-8 h-8 border-2 border-t-2 border-gray-300 rounded-full animate-spin" style="border-top-color: var(--theme-primary);"></div>
              <p class="mt-2 text-sm" style="color: var(--theme-text-secondary);">加载文章中...</p>
            </div>
            <div v-else-if="paginatedArticles.length > 0" class="space-y-4 sm:space-y-6 mb-6">
              <ArticleCard v-for="article in paginatedArticles" :key="article.id" :article="article" />
            </div>
            <div v-else-if="authorArticles.length === 0" class="p-8 sm:p-12 text-center">
              <BookOpen class="w-12 h-12 sm:w-16 sm:h-16 mx-auto mb-4" style="color: var(--theme-text-secondary);" />
              <h3 class="text-lg font-medium mb-2" style="color: var(--theme-text);">暂无文章</h3>
              <p style="color: var(--theme-text-secondary);">{{ author?.username }}还没有发布任何文章</p>
            </div>
            
            <!-- Pagination -->
            <div class="flex justify-center mt-8" v-if="totalPages > 1 && paginatedArticles.length > 0">
              <Pagination 
                :current-page="currentPage"
                :total-pages="totalPages"
                :total-items="totalItems"
                :items-per-page="itemsPerPage"
                @page-change="handlePageChange"
              />
            </div>
          </div>

          <!-- 获赞 Tab -->
          <div v-else-if="activeTab === 'liked'" class="p-4 sm:p-6">
            <div class="space-y-4 sm:space-y-6">
              <!-- 获赞统计卡片 -->
              <div class="p-3 sm:p-4 rounded-xl" style="background-color: var(--theme-surface);">
                <h3 class="font-medium mb-3" style="color: var(--theme-text);">获赞统计</h3>
                <div class="grid grid-cols-2 sm:grid-cols-4 gap-3">
                  <div class="text-center p-3 rounded-lg" style="background-color: var(--theme-bg);">
                    <div class="text-2xl font-bold mb-1" style="color: var(--theme-primary);">{{ authorStats.totalLikes }}</div>
                    <div class="text-xs" style="color: var(--theme-text-secondary);">总获赞</div>
                  </div>
                  <div class="text-center p-3 rounded-lg" style="background-color: var(--theme-bg);">
                    <div class="text-2xl font-bold mb-1" style="color: var(--theme-primary);">{{ Math.round(authorStats.totalLikes / (authorStats.articles || 1)) }}</div>
                    <div class="text-xs" style="color: var(--theme-text-secondary);">平均获赞</div>
                  </div>
                  <div class="text-center p-3 rounded-lg" style="background-color: var(--theme-bg);">
                    <div class="text-2xl font-bold mb-1" style="color: var(--theme-primary);">{{ authorArticles.length > 0 ? Math.max(...authorArticles.map(a => a.likes)) : 0 }}</div>
                    <div class="text-xs" style="color: var(--theme-text-secondary);">最高获赞</div>
                  </div>
                  <div class="text-center p-3 rounded-lg" style="background-color: var(--theme-bg);">
                    <div class="text-2xl font-bold mb-1" style="color: var(--theme-primary);">{{ authorStats.totalViews > 0 ? Math.round((authorStats.totalLikes / authorStats.totalViews) * 100) : 0 }}%</div>
                    <div class="text-xs" style="color: var(--theme-text-secondary);">点赞率</div>
                  </div>
                </div>
              </div>

              <!-- 获赞最多的文章 -->
              <div v-if="authorArticles.length > 0" class="p-3 sm:p-4 rounded-xl" style="background-color: var(--theme-surface);">
                <h3 class="font-medium mb-3" style="color: var(--theme-text);">最受欢迎的文章</h3>
                <div class="space-y-4">
                  <ArticleCard 
                    v-for="article in [...authorArticles].sort((a, b) => b.likes - a.likes).slice(0, 5)" 
                    :key="article.id" 
                    :article="article" 
                  />
                </div>
              </div>
            </div>
          </div>

          <!-- 成就 Tab -->
          <div v-else-if="activeTab === 'achievements'" class="p-4 sm:p-6">
            <div class="space-y-4 sm:space-y-6">
              <!-- 成长概览 -->
              <div v-if="authorGrowth" class="p-4 sm:p-6 rounded-xl" style="background-color: var(--theme-surface);">
                <div class="flex items-center justify-between mb-4">
                  <h3 class="font-medium" style="color: var(--theme-text);">成长概览</h3>
                  <Link
                    :to="`/achievements?userId=${author?.id}`"
                    class="text-xs flex items-center gap-1 hover:underline"
                    style="color: var(--theme-primary);"
                  >
                    查看全部成就
                    <ChevronRight class="w-3 h-3" />
                  </Link>
                </div>
                <div class="grid grid-cols-2 sm:grid-cols-4 gap-3">
                  <div class="text-center p-3 rounded-lg" style="background-color: var(--theme-bg);">
                    <div class="text-2xl font-bold mb-1" style="color: var(--theme-primary);">Lv.{{ authorGrowth.level }}</div>
                    <div class="text-xs" style="color: var(--theme-text-secondary);">成长等级</div>
                  </div>
                  <div class="text-center p-3 rounded-lg" style="background-color: var(--theme-bg);">
                    <div class="text-2xl font-bold mb-1" style="color: var(--theme-primary);">{{ authorGrowth.growthValue }}</div>
                    <div class="text-xs" style="color: var(--theme-text-secondary);">成长值</div>
                  </div>
                  <div class="text-center p-3 rounded-lg" style="background-color: var(--theme-bg);">
                    <div class="text-2xl font-bold mb-1" style="color: var(--theme-primary);">{{ authorGrowth.seasonValue }}</div>
                    <div class="text-xs" style="color: var(--theme-text-secondary);">赛季成长值</div>
                  </div>
                  <div class="text-center p-3 rounded-lg" style="background-color: var(--theme-bg);">
                    <div class="text-2xl font-bold mb-1" style="color: var(--theme-primary);">{{ authorGrowth.seasonRank ? '#' + authorGrowth.seasonRank : '-' }}</div>
                    <div class="text-xs" style="color: var(--theme-text-secondary);">赛季排名</div>
                  </div>
                </div>
                <div class="mt-3 p-3 rounded-lg flex items-center gap-2" style="background-color: var(--theme-bg);">
                  <Award class="w-4 h-4 flex-shrink-0" style="color: var(--theme-primary);" />
                  <span class="text-sm" style="color: var(--theme-text);">当前头衔：</span>
                  <span class="text-sm font-medium" style="color: var(--theme-primary);">{{ authorGrowth.title }}</span>
                  <span v-if="authorGrowth.nextLevelTitle" class="text-xs ml-auto" style="color: var(--theme-text-secondary);">
                    下一阶段：{{ authorGrowth.nextLevelTitle }}
                  </span>
                </div>
              </div>

              <!-- 成就进度 -->
              <div v-if="authorAchievements.length > 0" class="p-4 sm:p-6 rounded-xl" style="background-color: var(--theme-surface);">
                <div class="flex items-center justify-between mb-4">
                  <h3 class="font-medium" style="color: var(--theme-text);">成就进度</h3>
                  <span class="text-xs" style="color: var(--theme-text-secondary);">
                    {{ achievementProgress.earned }}/{{ achievementProgress.total }} 已达成
                  </span>
                </div>
                <!-- 进度条 -->
                <div class="w-full h-2 rounded-full mb-4" style="background-color: var(--theme-bg);">
                  <div
                    class="h-2 rounded-full transition-all"
                    :style="{ width: achievementProgress.percent + '%', backgroundColor: 'var(--theme-primary)' }"
                  ></div>
                </div>
                <!-- 成就徽章网格 -->
                <div class="grid grid-cols-3 sm:grid-cols-4 md:grid-cols-6 gap-3">
                  <div
                    v-for="ach in authorAchievements"
                    :key="ach.id"
                    class="text-center p-3 rounded-xl"
                    :style="ach.earned
                      ? { backgroundColor: 'var(--theme-bg)' }
                      : { backgroundColor: 'var(--theme-bg)', opacity: 0.5 }"
                  >
                    <div
                      class="w-10 h-10 mx-auto mb-2 rounded-xl flex items-center justify-center"
                      :style="ach.earned
                        ? { background: 'linear-gradient(135deg, #667eea 0%, #764ba2 100%)' }
                        : { backgroundColor: 'var(--theme-border)' }"
                    >
                      <component :is="ach.earned ? Award : Star" class="w-5 h-5 text-white" />
                    </div>
                    <p class="text-xs font-medium mb-1 truncate" style="color: var(--theme-text);">{{ ach.name }}</p>
                    <p v-if="ach.earned && ach.earnedTime" class="text-xs" style="color: var(--theme-text-secondary);">
                      {{ ach.earnedTime.substring(0, 10) }}
                    </p>
                    <p v-else class="text-xs" style="color: var(--theme-text-secondary);">未达成</p>
                  </div>
                </div>
              </div>

              <!-- 空状态 -->
              <div v-if="!authorGrowth && authorAchievements.length === 0" class="p-8 sm:p-12 text-center">
                <Star class="w-12 h-12 sm:w-16 sm:h-16 mx-auto mb-4" style="color: var(--theme-text-secondary);" />
                <h3 class="text-lg font-medium mb-2" style="color: var(--theme-text);">暂无成就数据</h3>
                <p style="color: var(--theme-text-secondary);">{{ author?.username }}还没有成长数据</p>
              </div>
            </div>
          </div>
        </div>
        </template>
      </div>
    </div>

    <!-- Footer -->
    <SiteFooter />
  </div>
</template>
