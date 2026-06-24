<script setup lang="ts">
import { ref, computed, onMounted } from 'vue';
import { useHead } from '@vueuse/head';
import { Search, ChevronDown, BookOpen, HelpCircle, MessageSquare, Shield, Loader2 } from 'lucide-vue-next';
import SiteFooter from '@/components/SiteFooter.vue';
import Breadcrumb from '@/components/Breadcrumb.vue';
import { generateSeo } from '@/utils/seo';
import { getHelpHome, searchHelpArticles, type HelpCategory, type HelpArticle } from '@/api/help';

// 图标映射（lucide 图标名 → 组件）
const iconMap: Record<string, any> = {
  BookOpen,
  HelpCircle,
  MessageSquare,
  Shield,
};

const loading = ref(false);
const error = ref<string | null>(null);
const searchQuery = ref('');
const searchLoading = ref(false);
const expandedFaq = ref<number | null>(null);
const activeCategoryId = ref<number | null>(null);

const categories = ref<HelpCategory[]>([]);
const featuredArticles = ref<HelpArticle[]>([]);
const searchResults = ref<HelpArticle[]>([]);

onMounted(() => {
  loadHome();
});

async function loadHome() {
  try {
    loading.value = true;
    error.value = null;
    const res = await getHelpHome();
    if (res.code === 200 && res.data) {
      categories.value = res.data.categories || [];
      featuredArticles.value = res.data.featuredArticles || [];
    } else {
      error.value = res.message || '加载失败';
    }
  } catch (err) {
    console.error('加载帮助中心失败:', err);
    error.value = '加载失败，请稍后重试';
  } finally {
    loading.value = false;
  }
}

async function handleSearch() {
  if (!searchQuery.value.trim()) {
    searchResults.value = [];
    return;
  }
  try {
    searchLoading.value = true;
    const res = await searchHelpArticles(searchQuery.value);
    if (res.code === 200) {
      searchResults.value = res.data || [];
    }
  } catch (err) {
    console.error('搜索失败:', err);
  } finally {
    searchLoading.value = false;
  }
}

function toggleFaq(id: number) {
  expandedFaq.value = expandedFaq.value === id ? null : id;
}

// 展示的文章列表：搜索时显示搜索结果，否则显示精选
const displayArticles = computed(() => {
  if (searchQuery.value.trim()) {
    return searchResults.value;
  }
  return featuredArticles.value;
});

// 分类图标组件
function getCategoryIcon(iconName: string) {
  return iconMap[iconName] || HelpCircle;
}

useHead(
  generateSeo({
    title: '帮助中心',
    description: '墨韵帮助中心，解答您在使用过程中遇到的各种问题。',
    keywords: ['帮助中心', 'FAQ', '常见问题'],
    type: 'website'
  })
);
</script>

<template>
  <div style="background-color: var(--theme-bg); min-height: 100vh;" class="flex flex-col">
    <!-- 面包屑 -->
    <div class="border-b py-3 sm:py-4" style="background-color: var(--theme-bg); border-color: var(--theme-border);">
      <div class="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8">
        <Breadcrumb :items="[{ label: '首页', path: '/' }, { label: '帮助中心' }]" />
      </div>
    </div>

    <!-- 主内容 -->
    <div class="flex-1 max-w-5xl mx-auto px-4 sm:px-6 lg:px-8 py-8 sm:py-12 w-full">
      <!-- 页面标题 -->
      <div class="text-center mb-8 sm:mb-12">
        <h1 class="text-2xl sm:text-3xl lg:text-4xl font-bold mb-3 sm:mb-4" style="color: var(--theme-text);">帮助中心</h1>
        <p class="text-sm sm:text-base" style="color: var(--theme-text-secondary);">找到您需要的答案，让使用更轻松</p>
      </div>

      <!-- 搜索框 -->
      <div class="mb-8 sm:mb-12">
        <div class="relative max-w-2xl mx-auto">
          <Search class="absolute left-4 top-1/2 -translate-y-1/2 w-5 h-5 sm:w-6 sm:h-6" style="color: var(--theme-text-secondary);" />
          <input
            v-model="searchQuery"
            type="text"
            placeholder="搜索您的问题..."
            class="w-full pl-12 sm:pl-14 pr-12 py-3 sm:py-4 rounded-2xl border focus:outline-none focus:ring-2 focus:ring-primary transition-all"
            style="background-color: var(--theme-surface); border-color: var(--theme-border); color: var(--theme-text);"
            @input="handleSearch"
          />
          <Loader2 v-if="searchLoading" class="absolute right-4 top-1/2 -translate-y-1/2 w-5 h-5 animate-spin" style="color: var(--theme-text-secondary);" />
        </div>
      </div>

      <!-- 加载中 -->
      <div v-if="loading" class="text-center py-12">
        <Loader2 class="w-10 h-10 mx-auto animate-spin" style="color: var(--theme-primary);" />
        <p class="mt-4 text-sm" style="color: var(--theme-text-secondary);">加载中...</p>
      </div>

      <!-- 加载失败 -->
      <div v-else-if="error" class="text-center py-12">
        <p class="text-sm mb-4" style="color: var(--theme-text-secondary);">{{ error }}</p>
        <button
          @click="loadHome"
          class="px-4 py-2 rounded-xl text-sm font-medium"
          style="background-color: var(--theme-primary); color: white;"
        >重试</button>
      </div>

      <template v-else>
        <!-- 分类卡片 -->
        <div v-if="categories.length > 0" class="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-4 gap-4 sm:gap-6 mb-10 sm:mb-14">
          <div
            v-for="category in categories"
            :key="category.id"
            class="p-4 sm:p-6 rounded-2xl border cursor-pointer hover:shadow-lg transition-all duration-300 hover:-translate-y-1"
            style="background-color: var(--theme-surface); border-color: var(--theme-border);"
          >
            <div class="w-10 h-10 sm:w-12 sm:h-12 rounded-xl flex items-center justify-center mb-3 sm:mb-4" style="background-color: var(--theme-accent);">
              <component :is="getCategoryIcon(category.icon)" class="w-5 h-5 sm:w-6 sm:h-6" style="color: var(--theme-primary);" />
            </div>
            <h3 class="font-semibold text-sm sm:text-base mb-1.5" style="color: var(--theme-text);">{{ category.name }}</h3>
            <p class="text-xs sm:text-sm" style="color: var(--theme-text-secondary);">{{ category.description }}</p>
          </div>
        </div>

        <!-- 常见问题 -->
        <div class="mb-8 sm:mb-12">
          <h2 class="text-xl sm:text-2xl font-bold mb-6 sm:mb-8" style="color: var(--theme-text);">
            {{ searchQuery.trim() ? '搜索结果' : '常见问题' }}
          </h2>

          <div v-if="displayArticles.length > 0" class="space-y-3 sm:space-y-4">
            <div
              v-for="article in displayArticles"
              :key="article.id"
              class="p-4 sm:p-6 rounded-2xl border"
              style="background-color: var(--theme-surface); border-color: var(--theme-border);"
            >
              <button
                @click="toggleFaq(article.id)"
                class="w-full text-left flex items-center justify-between gap-4"
              >
                <span class="font-medium text-sm sm:text-base" style="color: var(--theme-text);">{{ article.title }}</span>
                <ChevronDown
                  class="w-4 h-4 sm:w-5 sm:h-5 transition-transform duration-200 flex-shrink-0"
                  :class="{ 'rotate-180': expandedFaq === article.id }"
                  style="color: var(--theme-text-secondary);"
                />
              </button>
              <div
                v-if="expandedFaq === article.id"
                class="mt-4 text-sm whitespace-pre-line"
                style="color: var(--theme-text-secondary);"
              >
                {{ article.content }}
              </div>
            </div>
          </div>

          <div v-else class="text-center py-8 sm:py-12" style="background-color: var(--theme-surface); border: 1px solid var(--theme-border); border-radius: 1rem;">
            <Search class="w-10 h-10 sm:w-12 sm:h-12 mx-auto mb-4" style="color: var(--theme-text-secondary);" />
            <p class="text-sm sm:text-base" style="color: var(--theme-text-secondary);">
              {{ searchQuery.trim() ? '没有找到相关问题' : '暂无常见问题' }}
            </p>
          </div>
        </div>

        <!-- 联系我们 -->
        <div class="p-6 sm:p-8 rounded-2xl text-center" style="background-color: var(--theme-accent);">
          <h3 class="text-lg sm:text-xl font-semibold mb-2" style="color: var(--theme-text);">没有找到答案？</h3>
          <p class="text-xs sm:text-sm mb-5" style="color: var(--theme-text-secondary);">我们很乐意为您提供帮助</p>
          <button
            class="inline-flex items-center gap-2 px-5 sm:px-6 py-2.5 sm:py-3 rounded-xl font-medium transition-all hover:opacity-90"
            style="background-color: var(--theme-primary); color: white;"
          >
            <MessageSquare class="w-4 h-4 sm:w-5 sm:h-5" />
            联系客服
          </button>
        </div>
      </template>
    </div>

    <!-- Footer -->
    <SiteFooter />
  </div>
</template>
