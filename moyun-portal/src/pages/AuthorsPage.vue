<script setup lang="ts">
import { ref, computed, onMounted, watch } from 'vue';
import { formatDate } from '@/utils/date';
import { RouterLink as Link, useRouter } from 'vue-router';
import { useHead } from '@vueuse/head';
import {
  Search, ChevronDown,
  ArrowRight, Calendar, AlertCircle, RefreshCw
} from 'lucide-vue-next';
import SiteFooter from '@/components/SiteFooter.vue';
import Breadcrumb from '@/components/Breadcrumb.vue';
import Empty from '@/components/Empty.vue';
import { generateSeo } from '@/utils/seo';
import type { User } from '@/types';
import * as userApi from '@/api/user';
import { getSafeAvatar } from '@/utils/avatar';
import { useAuth } from '@/composables/useAuth';
import { useToast } from '@/composables/useToast';
import * as followApi from '@/api/follow';

const router = useRouter();
const { requireAuth, isAuthenticated } = useAuth();
const toast = useToast();

const searchQuery = ref('');
const sortBy = ref('popular');
const isLoading = ref(false);
const errorMsg = ref<string | null>(null);
const users = ref<User[]>([]);
// 关注状态缓存：userId -> isFollowing
const followingMap = ref<Record<string, boolean>>({});
// 关注操作中：userId -> true（避免重复点击）
const followingLoading = ref<Record<string, boolean>>({});

// 分页：**服务端分页**（清单 P2：原先前端 getAuthors(100) 后在浏览器内搜索/排序/分页
// ⇒ 第 101 位之后的作者永不出现，且排序只在已加载的 100 人子集内生效）
const currentPage = ref(1);
/** 服务端返回的作者总数（清单 P2：用于总页数，原先按已加载条数算） */
const totalCount = ref(0);

// 搜索词/排序变化必须回到第 1 页：否则在已翻到第 N 页时过滤结果变短，
// pagedUsers 会切片出空数组 → 误显"没有找到作者"，且分页控件因 totalPages<=1 被隐藏。
// 清单 P2：关键词/排序变化 → 回到第 1 页并**重新请求**（过滤与排序都在服务端完成）
watch([searchQuery, sortBy], () => {
  currentPage.value = 1;
  void loadUsers();
});
const pageSize = 12;

const sortOptions = [
  { label: '最受欢迎', value: 'popular' },
  { label: '最新加入', value: 'newest' },
  { label: '作品最多', value: 'works' },
  { label: '粉丝最多', value: 'fans' }
];

// 预计算统计并缓存，避免模板中每张卡片重复调用
interface UserWithStats extends User {
  createTime?: string;
  _stats: {
    articles: number;
    views: number;
    likes: number;
    following: number;
    followers: number;
  };
}

const usersWithStats = computed<UserWithStats[]>(() => {
  return users.value.map(user => {
    const u = user as any;
    return {
      ...user,
      _stats: {
        // 后端 /portal/user/authors 返回字段：works / views / likes / fansCount / followCount
        // 修复：原代码读取 articleCount/viewCount/likeCount 与后端字段不匹配，导致始终为 0
        articles: u?.works || 0,
        views: u?.views || 0,
        likes: u?.likes || 0,
        following: u?.followCount || 0,
        followers: u?.fansCount || 0
      }
    };
  });
});

// 清单 P2：关键词过滤与排序**已下沉到后端 SQL**（见 PortalUserMapper.selectAuthorsPage）。
// 原先在这里对"一次拉取的 100 条"做过滤/排序 ⇒ 搜索找不到第 101 位之后的作者、排序也不代表全站。

// 分页后的当前页数据
// 清单 P2：总页数用**服务端 total**（原先按已加载的 100 条算，永远只有 ≤9 页）
const totalPages = computed(() => Math.max(1, Math.ceil(totalCount.value / pageSize)));

// 清单 P2：服务端已分页，前端不再二次切片（原先是在浏览器内对 100 条做切片）
const pagedUsers = computed(() => usersWithStats.value);

function gotoPage(p: number) {
  if (p < 1 || p > totalPages.value) return;
  currentPage.value = p;
  // 翻页后回到列表顶部
  window.scrollTo({ top: 0, behavior: 'smooth' });
  // 清单 P2：服务端分页，翻页需重新请求
  void loadUsers();
}

onMounted(() => {
  loadUsers();
});

async function loadUsers() {
  isLoading.value = true;
  errorMsg.value = null;
  try {
    // 清单 P2：服务端分页 + 关键词 + 排序
    const response = await userApi.getAuthors({
      pageNum: currentPage.value,
      pageSize,
      keyword: searchQuery.value.trim() || undefined,
      sort: sortBy.value,
    });
    if (response.code === 200 && response.data) {
      const payload = response.data as unknown as { list?: User[]; total?: number };
      if (Array.isArray(payload)) {
        users.value = payload;
        totalCount.value = payload.length;
      } else {
        users.value = payload.list || [];
        totalCount.value = payload.total || 0;
      }
      // 批量检查关注状态（已登录才有意义；未登录则全 false）
      await loadFollowingStates();
    } else {
      errorMsg.value = response.message || '加载作者列表失败';
    }
  } catch (error) {
    const e = error as { message?: string };
    errorMsg.value = e?.message || '加载失败，请稍后重试';
  } finally {
    isLoading.value = false;
  }
}

/**
 * 批量加载作者关注状态。
 *
 * <p>原实现是**空壳**：只把每位作者写成 false，从未调用早已存在的
 * {@code GET /portal/follow/check/{userId}} ⇒ 已关注的作者在列表里仍显示"关注"，
 * 用户点击后由后端按"已关注"处理，观感上像"点了没用"（清单 P1）。</p>
 *
 * <p>实现要点：未登录直接跳过（关注状态按当前登录用户判定，游客无意义）；
 * 分批并发（每批 FOLLOW_CHECK_BATCH 个）控制瞬时请求数；
 * 单个作者查询失败不影响其它项（保持 false）。</p>
 */
const FOLLOW_CHECK_BATCH = 8;

async function loadFollowingStates() {
  if (!isAuthenticated()) {
    // 未登录：保持全 false（模板此时不展示关注按钮）
    return;
  }
  const ids = users.value.map(u => String(u.id)).filter(Boolean);
  for (let i = 0; i < ids.length; i += FOLLOW_CHECK_BATCH) {
    const batch = ids.slice(i, i + FOLLOW_CHECK_BATCH);
    await Promise.all(batch.map(async (id) => {
      try {
        const res = await followApi.checkFollow({ userId: id });
        if (res.code === 200 && res.data) {
          followingMap.value[id] = !!res.data.following;
        }
      } catch {
        // 单个作者查询失败：保持默认 false，不影响列表其它项
      }
    }));
  }
}

async function handleToggleFollow(userId: string) {
  if (!requireAuth(`/authors`)) return;
  if (followingLoading.value[userId]) return;
  followingLoading.value[userId] = true;
  const isFollowing = followingMap.value[userId];
  try {
    if (isFollowing) {
      await followApi.unfollowUser({ userId });
      followingMap.value[userId] = false;
      toast.success('已取消关注');
    } else {
      // 以**服务端返回**为准（清单 P2）：后端对"关注自己"等情形返回 code=200 + data.followed=false
      // （不抛异常），原实现忽略返回值直接置 true ⇒ 界面显示"已关注"但实际没关注上。
      const resp = await followApi.followUser({ userId });
      const followed = (resp.data as { followed?: boolean } | null)?.followed;
      const msg = (resp.data as { message?: string } | null)?.message;
      if (followed === false) {
        followingMap.value[userId] = false;
        toast.warning(msg || '未能关注该用户');
      } else {
        followingMap.value[userId] = true;
        toast.success('关注成功');
      }
    }
  } catch (error) {
    const e = error as { message?: string };
    toast.error(e?.message || '操作失败');
  } finally {
    followingLoading.value[userId] = false;
  }
}

function goToAuthor(userId: string) {
  router.push(`/author/${userId}`);
}

useHead(
  generateSeo({
    title: '名家录',
    description: '旭林名家录 - 探索优秀创作者的精彩世界。',
    keywords: ['名家录', '作者', '创作者', '作家'],
    type: 'website'
  })
);
</script>

<template>
  <div class="min-h-screen flex flex-col" style="background-color: var(--theme-bg);">
    <!-- 面包屑 -->
    <div
      class="border-b sticky top-0 z-30 backdrop-blur-sm py-3"
      style="background-color: var(--theme-surface); border-color: var(--theme-border);"
    >
      <div class="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 flex items-center justify-between gap-4">
        <Breadcrumb :items="[{ label: '名家录' }]" />
      </div>
    </div>

    <!-- 主内容 -->
    <div class="flex-1 max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 py-6 sm:py-8">
      <!-- 页面标题 -->
      <div class="mb-6 sm:mb-8">
        <h1 class="text-2xl sm:text-3xl font-bold mb-2" style="color: var(--theme-text);">旭林名家录</h1>
        <p class="text-sm sm:text-base" style="color: var(--theme-text-secondary);">
          发现优秀创作者，关注他们的精彩作品
        </p>
      </div>

      <!-- 搜索和筛选 -->
      <div class="mb-6 sm:mb-8 flex flex-col sm:flex-row gap-3 sm:gap-4">
        <div class="flex-1 relative">
          <Search class="absolute left-3 top-1/2 -translate-y-1/2 w-4 h-4 sm:w-5 sm:h-5" style="color: var(--theme-text-secondary);" />
          <input
            v-model="searchQuery"
            type="text"
            placeholder="搜索作者..."
            class="w-full pl-10 pr-4 py-2.5 sm:py-3 rounded-xl border focus:outline-none focus:ring-2 transition-all"
            style="background-color: var(--theme-surface); border-color: var(--theme-border); color: var(--theme-text);"
          />
        </div>
        <div class="relative">
          <select
            v-model="sortBy"
            class="appearance-none pl-4 pr-10 py-2.5 sm:py-3 rounded-xl border focus:outline-none focus:ring-2 transition-all cursor-pointer"
            style="background-color: var(--theme-surface); border-color: var(--theme-border); color: var(--theme-text);"
          >
            <option v-for="option in sortOptions" :key="option.value" :value="option.value">
              {{ option.label }}
            </option>
          </select>
          <ChevronDown class="absolute right-3 top-1/2 -translate-y-1/2 w-4 h-4 pointer-events-none" style="color: var(--theme-text-secondary);" />
        </div>
      </div>

      <!-- 加载状态 -->
      <div v-if="isLoading" class="text-center py-12">
        <div
          class="animate-spin rounded-full h-10 w-10 border-2 mx-auto"
          style="border-color: var(--theme-accent); border-top-color: var(--theme-primary);"
        ></div>
        <p class="mt-4 text-sm" style="color: var(--theme-text-secondary);">加载中...</p>
      </div>

      <!-- 错误状态 -->
      <div
        v-else-if="errorMsg"
        class="flex flex-col items-center justify-center py-16 px-4"
      >
        <AlertCircle class="w-12 h-12 mb-4" style="color: var(--theme-danger);" />
        <p class="mb-2 text-base font-medium" style="color: var(--theme-text);">加载失败</p>
        <p class="mb-6 text-sm" style="color: var(--theme-text-secondary);">{{ errorMsg }}</p>
        <button
          @click="loadUsers"
          class="inline-flex items-center px-5 py-2.5 rounded-lg text-white text-sm font-medium transition hover:opacity-90"
          style="background-color: var(--theme-primary);"
        >
          <RefreshCw class="w-4 h-4 mr-2" />
          重新加载
        </button>
      </div>

      <!-- 作者列表 -->
      <div v-else-if="pagedUsers.length > 0" class="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-3 xl:grid-cols-4 gap-4 sm:gap-6">
        <div
          v-for="user in pagedUsers"
          :key="user.id"
          @click="goToAuthor(user.id)"
          class="rounded-2xl p-5 sm:p-6 cursor-pointer transition-all duration-300 hover:shadow-lg hover:-translate-y-1 border"
          :style="{ backgroundColor: 'var(--theme-surface)', borderColor: 'var(--theme-border)' }"
        >
          <!-- 作者头像和基本信息 -->
          <div class="flex items-start gap-3 sm:gap-4 mb-4">
            <div class="w-14 h-14 sm:w-16 sm:h-16 rounded-xl overflow-hidden flex-shrink-0">
              <img
                :src="getSafeAvatar(user.avatar, user.id)"
                :alt="user.username"
                class="w-full h-full object-cover"
                loading="lazy"
                @error="(e: Event) => (e.target as HTMLImageElement).src = getSafeAvatar(null, user.id)"
              />
            </div>
            <div class="flex-1 min-w-0">
              <h3 class="font-bold text-base sm:text-lg mb-1 truncate" style="color: var(--theme-text);">
                {{ user.username }}
              </h3>
              <p class="text-xs sm:text-sm line-clamp-2 mb-2" style="color: var(--theme-text-secondary);">
                {{ user.bio || '这是一位神秘的创作者~' }}
              </p>
              <p class="text-xs" style="color: var(--theme-text-secondary);">
                <Calendar class="w-3 h-3 inline mr-1" />
                加入于 {{ formatDate(user.createTime, 'YYYY-MM-DD') }}
              </p>
            </div>
          </div>

          <!-- 统计数据（使用预计算的 _stats 缓存） -->
          <div class="grid grid-cols-4 gap-2 mb-4 text-center">
            <div class="p-2 rounded-lg" style="background-color: var(--theme-bg);">
              <div class="text-sm sm:text-base font-bold" style="color: var(--theme-text);">
                {{ user._stats.articles }}
              </div>
              <div class="text-xs" style="color: var(--theme-text-secondary);">作品</div>
            </div>
            <div class="p-2 rounded-lg" style="background-color: var(--theme-bg);">
              <div class="text-sm sm:text-base font-bold" style="color: var(--theme-text);">
                {{ user._stats.views }}
              </div>
              <div class="text-xs" style="color: var(--theme-text-secondary);">浏览</div>
            </div>
            <div class="p-2 rounded-lg" style="background-color: var(--theme-bg);">
              <div class="text-sm sm:text-base font-bold" style="color: var(--theme-text);">
                {{ user._stats.likes }}
              </div>
              <div class="text-xs" style="color: var(--theme-text-secondary);">获赞</div>
            </div>
            <div class="p-2 rounded-lg" style="background-color: var(--theme-bg);">
              <div class="text-sm sm:text-base font-bold" style="color: var(--theme-text);">
                {{ user._stats.followers }}
              </div>
              <div class="text-xs" style="color: var(--theme-text-secondary);">粉丝</div>
            </div>
          </div>

          <!-- 操作按钮 -->
          <div class="flex gap-2">
            <button
              @click.stop="handleToggleFollow(user.id)"
              :disabled="followingLoading[user.id]"
              class="flex-1 py-2 rounded-lg text-sm font-medium transition-colors disabled:opacity-50"
              :style="followingMap[user.id]
                ? { backgroundColor: 'var(--theme-bg)', color: 'var(--theme-text)', border: '1px solid var(--theme-border)' }
                : { backgroundColor: 'var(--theme-primary)', color: 'white' }"
            >
              {{ followingLoading[user.id] ? '处理中...' : (followingMap[user.id] ? '已关注' : '+ 关注') }}
            </button>
            <Link
              :to="`/author/${user.id}`"
              @click.stop
              class="px-3 py-2 rounded-lg text-sm font-medium border transition-colors flex items-center gap-1"
              :style="{ borderColor: 'var(--theme-border)', color: 'var(--theme-text)' }"
            >
              <span>主页</span>
              <ArrowRight class="w-3 h-3" />
            </Link>
          </div>
        </div>
      </div>

      <!-- 空状态 -->
      <Empty
        v-else
        title="没有找到作者"
        description="尝试换一个搜索词或筛选条件"
        size="lg"
      />

      <!-- 分页 -->
      <div v-if="totalPages > 1" class="flex flex-wrap items-center justify-center gap-2 mt-8">
        <button
          @click="gotoPage(currentPage - 1)"
          :disabled="currentPage === 1"
          class="px-4 py-2 rounded-lg text-sm transition disabled:opacity-40 disabled:cursor-not-allowed"
          style="background-color: var(--theme-surface); border: 1px solid var(--theme-border); color: var(--theme-text);"
        >
          上一页
        </button>
        <span class="px-4 py-2 text-sm" style="color: var(--theme-text-secondary);">
          第 {{ currentPage }} / {{ totalPages }} 页（共 {{ totalCount }} 位）
        </span>
        <button
          @click="gotoPage(currentPage + 1)"
          :disabled="currentPage === totalPages"
          class="px-4 py-2 rounded-lg text-sm transition disabled:opacity-40 disabled:cursor-not-allowed"
          style="background-color: var(--theme-surface); border: 1px solid var(--theme-border); color: var(--theme-text);"
        >
          下一页
        </button>
      </div>
    </div>

    <!-- Footer -->
    <SiteFooter />
  </div>
</template>
