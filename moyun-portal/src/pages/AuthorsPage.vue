<script setup lang="ts">
import { ref, computed, onMounted } from 'vue';
import { RouterLink as Link, useRouter } from 'vue-router';
import { useHead } from '@vueuse/head';
import { 
  User as UserIcon, Search, Filter, ChevronDown, Star, Heart, Clock, 
  ArrowRight, Calendar, Trophy, Award, Medal
} from 'lucide-vue-next';
import SiteFooter from '@/components/SiteFooter.vue';
import Breadcrumb from '@/components/Breadcrumb.vue';
import { getArticles, mockUsers, getCurrentUser } from '@/data/mockData';
import { generateSeo } from '@/utils/seo';
import type { Article, User } from '@/types';
import * as userApi from '@/api/user';

const router = useRouter();

const searchQuery = ref('');
const sortBy = ref('popular');
const isLoading = ref(false);
const users = ref<User[]>([]);

const sortOptions = [
  { label: '最受欢迎', value: 'popular' },
  { label: '最新加入', value: 'newest' },
  { label: '作品最多', value: 'works' },
  { label: '粉丝最多', value: 'fans' }
];

// 模拟统计数据
const getUserStats = (userId: string) => {
  const userArticles = getArticles().filter(a => a.author.id === userId);
  const totalViews = userArticles.reduce((sum, a) => sum + a.views, 0);
  const totalLikes = userArticles.reduce((sum, a) => sum + a.likes, 0);
  
  return {
    articles: userArticles.length,
    views: totalViews,
    likes: totalLikes,
    following: Math.floor(Math.random() * 200) + 50,
    followers: Math.floor(Math.random() * 500) + 100
  };
};

const filteredUsers = computed(() => {
  let result = [...users.value];
  
  if (searchQuery.value) {
    const query = searchQuery.value.toLowerCase();
    result = result.filter(user => 
      user.username.toLowerCase().includes(query) ||
      (user.bio?.toLowerCase().includes(query))
    );
  }
  
  switch (sortBy.value) {
    case 'newest':
      return result.sort((a, b) => new Date(b.createdAt).getTime() - new Date(a.createdAt).getTime());
    case 'works':
      return result.sort((a, b) => {
        const statsA = getUserStats(a.id);
        const statsB = getUserStats(b.id);
        return statsB.articles - statsA.articles;
      });
    case 'fans':
      return result.sort((a, b) => {
        const statsA = getUserStats(a.id);
        const statsB = getUserStats(b.id);
        return statsB.followers - statsA.followers;
      });
    default: // popular
      return result.sort((a, b) => {
        const statsA = getUserStats(a.id);
        const statsB = getUserStats(b.id);
        return (statsB.views + statsB.likes * 10) - (statsA.views + statsA.likes * 10);
      });
  }
});

onMounted(() => {
  loadUsers();
});

async function loadUsers() {
  isLoading.value = true;
  try {
    // 直接使用模拟数据，避免 API 调用失败
    users.value = mockUsers;
  } catch (error) {
    console.error('加载用户列表失败:', error);
    users.value = mockUsers;
  } finally {
    isLoading.value = false;
  }
}

function goToAuthor(userId: string) {
  router.push(`/author/${userId}`);
}

useHead(
  generateSeo({
    title: '名家录',
    description: '墨韵名家录 - 探索优秀创作者的精彩世界。',
    keywords: ['名家录', '作者', '创作者', '作家'],
    type: 'website'
  })
);
</script>

<template>
  <div style="background-color: var(--theme-bg); min-height: 100vh;" class="flex flex-col">
    <!-- 面包屑 -->
    <div class="border-b py-3 sm:py-4" style="background-color: var(--theme-bg); border-color: var(--theme-border);">
      <div class="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8">
        <div class="flex items-center justify-between gap-4">
          <Breadcrumb :items="[{ label: '首页', path: '/' }, { label: '名家录' }]" />
        </div>
      </div>
    </div>

    <!-- 主内容 -->
    <div class="flex-1 max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 py-6 sm:py-8">
      <!-- 页面标题 -->
      <div class="mb-6 sm:mb-8">
        <h1 class="text-2xl sm:text-3xl font-bold mb-2" style="color: var(--theme-text);">墨韵名家录</h1>
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
        <div class="inline-block w-10 h-10 border-4 border-t-4 border-gray-300 rounded-full animate-spin" style="border-top-color: var(--theme-primary);"></div>
        <p class="mt-4" style="color: var(--theme-text-secondary);">加载中...</p>
      </div>

      <!-- 作者列表 -->
      <div v-else-if="filteredUsers.length > 0" class="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-3 xl:grid-cols-4 gap-4 sm:gap-6">
        <div 
          v-for="user in filteredUsers" 
          :key="user.id"
          @click="goToAuthor(user.id)"
          class="rounded-xl p-5 sm:p-6 cursor-pointer transition-all duration-300 hover:shadow-lg hover:-translate-y-1 border"
          :style="{ backgroundColor: 'var(--theme-surface)', borderColor: 'var(--theme-border)' }"
        >
          <!-- 作者头像和基本信息 -->
          <div class="flex items-start gap-3 sm:gap-4 mb-4">
            <div class="w-14 h-14 sm:w-16 sm:h-16 rounded-xl overflow-hidden flex-shrink-0">
              <img 
                :src="user.avatar" 
                :alt="user.username"
                class="w-full h-full object-cover"
                loading="lazy"
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
                加入于 {{ user.createdAt }}
              </p>
            </div>
          </div>

          <!-- 统计数据 -->
          <div class="grid grid-cols-4 gap-2 mb-4 text-center">
            <div class="p-2 rounded-lg" style="background-color: var(--theme-bg);">
              <div class="text-sm sm:text-base font-bold" style="color: var(--theme-text);">
                {{ getUserStats(user.id).articles }}
              </div>
              <div class="text-xs" style="color: var(--theme-text-secondary);">作品</div>
            </div>
            <div class="p-2 rounded-lg" style="background-color: var(--theme-bg);">
              <div class="text-sm sm:text-base font-bold" style="color: var(--theme-text);">
                {{ getUserStats(user.id).views }}
              </div>
              <div class="text-xs" style="color: var(--theme-text-secondary);">浏览</div>
            </div>
            <div class="p-2 rounded-lg" style="background-color: var(--theme-bg);">
              <div class="text-sm sm:text-base font-bold" style="color: var(--theme-text);">
                {{ getUserStats(user.id).likes }}
              </div>
              <div class="text-xs" style="color: var(--theme-text-secondary);">获赞</div>
            </div>
            <div class="p-2 rounded-lg" style="background-color: var(--theme-bg);">
              <div class="text-sm sm:text-base font-bold" style="color: var(--theme-text);">
                {{ getUserStats(user.id).followers }}
              </div>
              <div class="text-xs" style="color: var(--theme-text-secondary);">粉丝</div>
            </div>
          </div>

          <!-- 成就展示 -->
          <div class="flex items-center gap-2 mb-4">
            <div class="flex -space-x-2">
              <div 
                v-for="i in [1, 2, 3]" 
                :key="i"
                class="w-6 h-6 sm:w-7 sm:h-7 rounded-full flex items-center justify-center text-white text-xs"
                :style="{ background: 'linear-gradient(135deg, #667eea 0%, #764ba2 100%)' }"
              >
                <Star class="w-3 h-3 sm:w-3.5 sm:h-3.5" />
              </div>
            </div>
            <span class="text-xs" style="color: var(--theme-text-secondary);">已获得多个成就</span>
          </div>

          <!-- 操作按钮 -->
          <div class="flex gap-2">
            <button 
              class="flex-1 py-2 rounded-lg text-sm font-medium transition-colors"
              style="background-color: var(--theme-primary); color: white;"
            >
              关注作者
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
      <div v-else class="text-center py-12">
        <UserIcon class="w-16 h-16 sm:w-20 sm:h-20 mx-auto mb-4" style="color: var(--theme-text-secondary);" />
        <h3 class="text-lg font-medium mb-2" style="color: var(--theme-text);">没有找到作者</h3>
        <p style="color: var(--theme-text-secondary);">尝试换一个搜索词或筛选条件</p>
      </div>
    </div>

    <!-- Footer -->
    <SiteFooter />
  </div>
</template>
