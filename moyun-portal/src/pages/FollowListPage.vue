<script setup lang="ts">
import { ref, computed, onMounted, watch } from 'vue';
import { useRoute, useRouter, RouterLink as Link } from 'vue-router';
import { useHead } from '@vueuse/head';
import {
    UserPlus, ArrowLeft, Loader2, Users, Heart, UserCheck
} from 'lucide-vue-next';
import type { User as UserType, FollowUserItem } from '@/types/api';
import * as userApi from '@/api/user';
import * as followApi from '@/api/follow';
import { getSafeAvatar } from '@/utils/avatar';
import { useToast } from '@/composables/useToast';
import Breadcrumb from '@/components/Breadcrumb.vue';
import { generateSeo } from '@/utils/seo';

type ListType = 'following' | 'followers';

const route = useRoute();
const router = useRouter();
const toast = useToast();

// 根据 route.path 判断默认 Tab 类型：/user/:id/following 或 /user/:id/followers
const activeType = ref<ListType>(route.path.endsWith('/followers') ? 'followers' : 'following');

const userId = computed(() => String(route.params.id || ''));

const targetUser = ref<UserType | null>(null);
const isLoadingUser = ref(false);
const notFound = ref(false);

// 列表数据
const list = ref<FollowUserItem[]>([]);
const pageNum = ref(1);
const pageSize = 20;
const total = ref(0);
const isLoadingList = ref(false);
const loadingMore = ref(false);

// 关注操作中的用户ID集合（防止重复点击）
const pendingIds = ref<Set<string>>(new Set());

const currentUserId = ref<string>('');

const isOwnPage = computed(() => {
    return currentUserId.value && userId.value && String(currentUserId.value) === String(userId.value);
});

const hasMore = computed(() => list.value.length < total.value);

interface CrumbItem {
    label: string;
    path?: string;
}

const breadcrumbItems = computed<CrumbItem[]>(() => {
    const items: CrumbItem[] = [{ label: '首页', path: '/' }];
    if (targetUser.value) {
        items.push({ label: targetUser.value.nickname || targetUser.value.username, path: `/author/${targetUser.value.id}` });
    } else {
        items.push({ label: '用户' });
    }
    items.push({ label: activeType.value === 'followers' ? '粉丝' : '关注' });
    return items;
});

useHead(
    generateSeo({
        title: activeType.value === 'followers' ? '粉丝列表' : '关注列表',
        description: '查看用户的关注与粉丝',
        keywords: ['关注', '粉丝', '用户列表'],
        type: 'website'
    })
);

onMounted(async () => {
    await loadTargetUser();
    await loadList(true);
});

// 路由 path 变化时（同一组件不同路径）：
//  - 类型变化：仅同步 activeType，由 watch(activeType) 触发 loadList，避免重复调用
watch(
    () => route.path,
    (newPath) => {
        const newType: ListType = newPath.endsWith('/followers') ? 'followers' : 'following';
        if (newType !== activeType.value) {
            activeType.value = newType;
        }
        // 同类型不同用户的场景由下方 watch(userId) 处理
    }
);

// activeType 变化时重新加载列表
watch(activeType, () => {
    loadList(true);
});

// userId 变化（同类型不同用户，如 /user/A/following → /user/B/following）时重新加载
watch(userId, (newId, oldId) => {
    if (newId && newId !== oldId) {
        loadTargetUser();
        // 若路由变化仅是用户切换但 activeType 未变，watch(activeType) 不会触发，这里手动加载；
        // 若同时 Tab 也切换了，watch(activeType) 会触发，可能出现重复加载，
        // loadList 内部 reset 时会清空列表，重复调用对结果无影响（幂等）。
        loadList(true);
    }
});

async function loadTargetUser() {
    if (!userId.value) return;
    isLoadingUser.value = true;
    try {
        // 并行获取当前登录用户与目标用户信息
        const [meResp, userResp] = await Promise.all([
            userApi.getCurrentUser().catch(() => null),
            userApi.getUserById(userId.value),
        ]);
        if (meResp && meResp.code === 200 && meResp.data) {
            currentUserId.value = meResp.data.id;
        }
        if (userResp.code === 200 && userResp.data) {
            targetUser.value = userResp.data;
        } else {
            notFound.value = true;
        }
    } catch (error) {
        console.error('加载用户信息失败:', error);
        notFound.value = true;
    } finally {
        isLoadingUser.value = false;
    }
}

async function loadList(reset = false) {
    if (!userId.value) return;
    if (reset) {
        pageNum.value = 1;
        list.value = [];
        isLoadingList.value = true;
    } else {
        loadingMore.value = true;
    }
    try {
        const params = { pageNum: pageNum.value, pageSize };
        const resp =
            activeType.value === 'following'
                ? await followApi.getFollowingList(userId.value, params)
                : await followApi.getFollowerList(userId.value, params);
        if (resp.code === 200 && resp.data) {
            const items = resp.data.list || [];
            if (reset) {
                list.value = items;
            } else {
                list.value.push(...items);
            }
            total.value = resp.data.total || 0;
        } else if (reset) {
            list.value = [];
            total.value = 0;
        }
    } catch (error) {
        console.error('加载列表失败:', error);
        if (reset) {
            list.value = [];
        }
    } finally {
        isLoadingList.value = false;
        loadingMore.value = false;
    }
}

function loadMore() {
    if (!hasMore.value || loadingMore.value) return;
    pageNum.value++;
    loadList(false);
}

function switchType(type: ListType) {
    if (type === activeType.value) return;
    activeType.value = type;
    // 同步路由路径，保持 URL 与 Tab 一致
    const newPath = `/user/${userId.value}/${type}`;
    if (route.path !== newPath) {
        router.replace(newPath);
    }
}

function gotoAuthor(id: string | number) {
    router.push(`/author/${id}`);
}

async function toggleFollow(item: FollowUserItem) {
    if (!currentUserId.value) {
        toast.warning('请先登录');
        router.push({ name: 'login', query: { redirect: route.fullPath } });
        return;
    }
    if (item.isMe) return;
    const itemId = String(item.id);
    if (pendingIds.value.has(itemId)) return;
    pendingIds.value.add(itemId);
    const wasFollowing = item.following === true;
    try {
        if (wasFollowing) {
            await followApi.unfollowUser({ userId: itemId });
            item.following = false;
            item.mutualFollow = false;
        } else {
            await followApi.followUser({ userId: itemId });
            item.following = true;
        }
    } catch (error) {
        console.error('操作关注失败:', error);
        // 失败时回滚本地状态
        item.following = wasFollowing;
        toast.error('操作失败，请重试');
    } finally {
        pendingIds.value.delete(itemId);
    }
}

function displayName(item: FollowUserItem): string {
    return item.nickname || item.username || `用户${item.id}`;
}
</script>

<template>
  <div class="min-h-screen flex flex-col" style="background-color: var(--theme-bg);">
    <!-- 面包屑 -->
    <div class="border-b py-3 sm:py-4" style="border-color: var(--theme-border);">
      <div class="max-w-5xl mx-auto px-4 sm:px-6 lg:px-8">
        <div class="flex items-center justify-between gap-4">
          <Breadcrumb :items="breadcrumbItems" />
          <button
            @click="router.back()"
            class="flex items-center gap-1 text-sm"
            style="color: var(--theme-text-secondary);"
          >
            <ArrowLeft class="w-4 h-4" />
            返回
          </button>
        </div>
      </div>
    </div>

    <div class="py-6 sm:py-8 flex-1">
      <div class="max-w-5xl mx-auto px-4 sm:px-6 lg:px-8">
        <!-- 加载中 -->
        <div v-if="isLoadingUser" class="text-center py-12">
          <Loader2 class="w-8 h-8 mx-auto animate-spin" style="color: var(--theme-primary);" />
          <p class="mt-3 text-sm" style="color: var(--theme-text-secondary);">加载中...</p>
        </div>

        <!-- 用户不存在 -->
        <div v-else-if="notFound" class="text-center py-16">
          <p class="text-4xl mb-3">404</p>
          <p class="text-base mb-2" style="color: var(--theme-text);">该用户不存在</p>
          <button @click="router.push('/')" class="px-5 py-2 rounded-lg text-white text-sm" style="background-color: var(--theme-primary);">返回首页</button>
        </div>

        <template v-else-if="targetUser">
          <!-- 用户信息头部 -->
          <div class="mb-6 rounded-2xl p-5 sm:p-6" style="background-color: var(--theme-surface); border: 1px solid var(--theme-border);">
            <div class="flex items-center gap-4">
              <Link :to="`/author/${targetUser.id}`" class="flex-shrink-0">
                <img
                  :src="getSafeAvatar(targetUser.avatar, targetUser.id)"
                  :alt="targetUser.nickname || targetUser.username"
                  class="w-16 h-16 sm:w-20 sm:h-20 rounded-2xl object-cover"
                  @error="(e: Event) => (e.target as HTMLImageElement).src = getSafeAvatar(null, targetUser.id)"
                />
              </Link>
              <div class="flex-1 min-w-0">
                <h1 class="text-lg sm:text-xl font-bold truncate" style="color: var(--theme-text);">
                  {{ targetUser.nickname || targetUser.username }}
                </h1>
                <p class="text-sm mt-1 line-clamp-2" style="color: var(--theme-text-secondary);">
                  {{ targetUser.bio || '这个人很懒，什么都没写~' }}
                </p>
              </div>
            </div>
          </div>

          <!-- Tab 切换 -->
          <div class="mb-5 border-b" style="border-color: var(--theme-border);">
            <nav class="flex gap-1">
              <button
                v-for="tab in [
                  { id: 'following', label: '关注的人', icon: Users },
                  { id: 'followers', label: '粉丝', icon: Heart }
                ]"
                :key="tab.id"
                @click="switchType(tab.id as ListType)"
                class="flex items-center gap-2 px-4 sm:px-6 py-3 text-sm sm:text-base font-medium border-b-2 transition-colors whitespace-nowrap"
                :style="activeType === tab.id
                  ? 'border-color: var(--theme-primary); color: var(--theme-primary);'
                  : 'border-color: transparent; color: var(--theme-text-secondary);'"
              >
                <component :is="tab.icon" class="w-4 h-4" />
                {{ tab.label }}
                <span class="text-xs px-2 py-0.5 rounded-full" style="background-color: var(--theme-accent); color: var(--theme-text-secondary);">
                  {{ activeType === tab.id ? total : '' }}
                </span>
              </button>
            </nav>
          </div>

          <!-- 列表 -->
          <div v-if="isLoadingList" class="text-center py-12">
            <Loader2 class="w-8 h-8 mx-auto animate-spin" style="color: var(--theme-primary);" />
            <p class="mt-3 text-sm" style="color: var(--theme-text-secondary);">加载中...</p>
          </div>

          <div v-else-if="list.length === 0" class="py-16 text-center rounded-2xl" style="background-color: var(--theme-surface); border: 1px solid var(--theme-border);">
            <Users class="w-12 h-12 mx-auto mb-3" style="color: var(--theme-text-secondary);" />
            <p class="text-base mb-1" style="color: var(--theme-text);">暂无{{ activeType === 'followers' ? '粉丝' : '关注' }}</p>
            <p class="text-sm" style="color: var(--theme-text-secondary);">
              {{ isOwnPage ? '还没有人关注你哦~' : '该用户还没有关注的人' }}
            </p>
          </div>

          <div v-else class="space-y-3">
            <div
              v-for="item in list"
              :key="item.id"
              class="flex items-center gap-3 sm:gap-4 p-4 rounded-2xl transition-colors cursor-pointer"
              style="background-color: var(--theme-surface); border: 1px solid var(--theme-border);"
              @click="gotoAuthor(String(item.id))"
            >
              <!-- 头像 -->
              <img
                :src="getSafeAvatar(item.avatar, String(item.id))"
                :alt="displayName(item)"
                class="w-12 h-12 sm:w-14 sm:h-14 rounded-full object-cover flex-shrink-0"
                @error="(e: Event) => (e.target as HTMLImageElement).src = getSafeAvatar(null, String(item.id))"
              />

              <!-- 信息 -->
              <div class="flex-1 min-w-0">
                <div class="flex items-center gap-2 flex-wrap">
                  <span class="font-medium truncate" style="color: var(--theme-text);">
                    {{ displayName(item) }}
                  </span>
                  <span
                    v-if="item.mutualFollow"
                    class="inline-flex items-center gap-1 px-2 py-0.5 rounded-full text-xs"
                    style="background-color: var(--theme-accent); color: var(--theme-primary);"
                  >
                    <UserCheck class="w-3 h-3" />
                    互相关注
                  </span>
                  <span
                    v-if="item.isMe"
                    class="px-2 py-0.5 rounded-full text-xs"
                    style="background-color: var(--theme-accent); color: var(--theme-text-secondary);"
                  >
                    我自己
                  </span>
                </div>
                <p class="text-sm mt-1 line-clamp-1" style="color: var(--theme-text-secondary);">
                  {{ item.bio || item.position || '暂无简介' }}
                </p>
              </div>

              <!-- 关注按钮 -->
              <button
                v-if="!item.isMe"
                @click.stop="toggleFollow(item)"
                :disabled="pendingIds.has(String(item.id))"
                class="flex items-center gap-1 px-3 sm:px-4 py-2 rounded-xl text-sm font-medium transition-colors flex-shrink-0 disabled:opacity-60"
                :style="item.following
                  ? { border: '1px solid var(--theme-border)', color: 'var(--theme-text-secondary)', backgroundColor: 'var(--theme-surface)' }
                  : { backgroundColor: 'var(--theme-primary)', color: 'white' }"
              >
                <Loader2 v-if="pendingIds.has(String(item.id))" class="w-4 h-4 animate-spin" />
                <component :is="item.following ? UserCheck : UserPlus" v-else class="w-4 h-4" />
                <span class="hidden sm:inline">{{ item.following ? '已关注' : '关注' }}</span>
                <span class="sm:hidden">{{ item.following ? '已关注' : '+ 关注' }}</span>
              </button>
            </div>

            <!-- 加载更多 -->
            <div v-if="hasMore" class="text-center pt-2 pb-4">
              <button
                @click="loadMore"
                :disabled="loadingMore"
                class="inline-flex items-center gap-2 px-6 py-2.5 rounded-xl text-sm font-medium transition-colors"
                style="background-color: var(--theme-surface); border: 1px solid var(--theme-border); color: var(--theme-text);"
              >
                <Loader2 v-if="loadingMore" class="w-4 h-4 animate-spin" />
                {{ loadingMore ? '加载中...' : '加载更多' }}
              </button>
            </div>
            <div v-else-if="list.length > 0" class="text-center py-4 text-xs" style="color: var(--theme-text-secondary);">
              没有更多了
            </div>
          </div>
        </template>
      </div>
    </div>
  </div>
</template>
