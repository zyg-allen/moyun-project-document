<script setup lang="ts">
import { ref, onMounted, watch, computed } from 'vue';
import { useRouter } from 'vue-router';
import { useHead } from '@vueuse/head';
import {
  Star, BookOpen, Bookmark, ChevronLeft, ChevronRight,
} from 'lucide-vue-next';
import SiteFooter from '@/components/SiteFooter.vue';
import Breadcrumb from '@/components/Breadcrumb.vue';
import { generateSeo } from '@/utils/seo';
import { getMyBookmarkList } from '@/api/interview';
import type { InterviewQuestionVO } from '@/types/api';

const router = useRouter();

const loading = ref(false);
const error = ref<string | null>(null);
const bookmarks = ref<InterviewQuestionVO[]>([]);
const total = ref(0);
const page = ref(1);
const pageSize = 10;

const totalPages = computed(() => Math.max(1, Math.ceil(total.value / pageSize)));

const difficultyMap: Record<string, { label: string; class: string }> = {
  easy: { label: '简单', class: 'bg-green-100 text-green-700' },
  medium: { label: '中等', class: 'bg-yellow-100 text-yellow-700' },
  hard: { label: '困难', class: 'bg-red-100 text-red-700' },
};

useHead(computed(() => generateSeo({
  title: '我的收藏',
  description: '查看我收藏的面试题目，随时回顾重点题目，高效备战面试',
  keywords: ['我的收藏', '收藏题目', '面试题', '面试收藏'],
  canonicalPath: '/interview/my/bookmarks',
  robots: 'noindex,nofollow',
})));

// 面包屑
const breadcrumbs = computed(() => [
  { label: '个人空间', path: '/user' },
  { label: '我的收藏' },
]);

onMounted(() => {
  loadBookmarks();
});

watch(page, () => {
  loadBookmarks();
});

async function loadBookmarks() {
  try {
    loading.value = true;
    error.value = null;
    const res = await getMyBookmarkList({ pageNum: page.value, pageSize });
    if (res.code === 200 && res.data) {
      bookmarks.value = res.data.list || [];
      total.value = res.data.total || 0;
    } else {
      error.value = res.message || '加载收藏失败';
    }
  } catch (err: any) {
    error.value = err?.message || '加载收藏失败，请稍后重试';
  } finally {
    loading.value = false;
  }
}

function gotoQuestion(id: string | number) {
  router.push(`/interview/question/${id}`);
}

function gotoPage(p: number) {
  if (p < 1 || p > totalPages.value) return;
  page.value = p;
  window.scrollTo({ top: 0, behavior: 'smooth' });
}

// 收藏时间：后端可能在题目上附带 bookmarkTime，否则回退到 createTime
function bookmarkTime(item: any): string {
  return item.bookmarkTime || item.createTime || '';
}

function diffLabel(item: InterviewQuestionVO) {
  return difficultyMap[item.difficulty]?.label || item.difficulty || '未知';
}

function diffClass(item: InterviewQuestionVO) {
  return difficultyMap[item.difficulty]?.class || 'bg-gray-100 text-gray-700';
}
</script>

<template>
  <div class="min-h-screen flex flex-col" style="background-color: var(--theme-bg);">
    <!-- 吸顶面包屑栏 -->
    <div
      class="border-b sticky top-0 z-30 backdrop-blur-sm py-3"
      style="background-color: var(--theme-surface); border-color: var(--theme-border);"
    >
      <div class="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 flex items-center justify-between gap-4">
        <Breadcrumb :items="breadcrumbs" />
        <span class="w-12"></span>
      </div>
    </div>

    <!-- 内容区 -->
    <div class="flex-1 py-8">
      <div class="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8">
        <!-- 加载状态 -->
        <div v-if="loading" class="text-center py-16">
          <div
            class="animate-spin rounded-full h-10 w-10 border-2 mx-auto"
            style="border-color: var(--theme-border); border-top-color: var(--theme-primary);"
          ></div>
          <p class="mt-4 text-sm" style="color: var(--theme-text-secondary);">加载中...</p>
        </div>

        <!-- 错误状态 -->
        <div
          v-else-if="error"
          class="rounded-xl border p-8 text-center"
          style="background-color: var(--theme-surface); border-color: var(--theme-border);"
        >
          <p class="mb-4 text-sm" style="color: var(--theme-text);">{{ error }}</p>
          <button
            @click="loadBookmarks"
            class="px-4 py-2 text-white rounded-lg text-sm transition hover:opacity-90"
            style="background-color: var(--theme-primary);"
          >
            重试
          </button>
        </div>

        <!-- 空状态 -->
        <div
          v-else-if="bookmarks.length === 0"
          class="rounded-xl border p-12 text-center"
          style="background-color: var(--theme-surface); border-color: var(--theme-border);"
        >
          <Bookmark class="w-12 h-12 mx-auto mb-3" style="color: var(--theme-text-secondary); opacity: 0.5;" />
          <p class="text-sm mb-4" style="color: var(--theme-text-secondary);">还没有收藏任何题目</p>
          <button
            @click="router.push('/interview/questions')"
            class="px-4 py-2 text-white rounded-lg text-sm transition hover:opacity-90"
            style="background-color: var(--theme-primary);"
          >
            去题库逛逛
          </button>
        </div>

        <!-- 收藏列表 -->
        <template v-else>
          <div class="space-y-4">
            <div
              v-for="item in bookmarks"
              :key="item.id"
              @click="gotoQuestion(item.id)"
              class="rounded-xl shadow-sm hover:shadow-md transition cursor-pointer p-5"
              style="background-color: var(--theme-surface); border: 1px solid var(--theme-border);"
            >
              <!-- 标签行 -->
              <div class="flex items-center flex-wrap gap-2 mb-2">
                <span
                  class="px-2.5 py-1 rounded-full text-xs font-medium"
                  :class="diffClass(item)"
                >
                  {{ diffLabel(item) }}
                </span>
                <span
                  v-if="item.categoryName"
                  class="inline-flex items-center px-2.5 py-1 rounded-full text-xs font-medium"
                  style="background-color: var(--theme-bg); color: var(--theme-text-secondary);"
                >
                  <BookOpen class="w-3 h-3 mr-1" />
                  {{ item.categoryName }}
                </span>
                <span
                  v-for="tag in (item.tags || []).slice(0, 3)"
                  :key="tag"
                  class="px-2 py-1 rounded text-xs"
                  style="background-color: var(--theme-bg); color: var(--theme-text-secondary);"
                >
                  #{{ tag }}
                </span>
              </div>

              <!-- 标题 -->
              <h3 class="text-base font-semibold mb-1" style="color: var(--theme-text);">
                {{ item.title }}
              </h3>

              <!-- 描述 -->
              <p
                v-if="item.description"
                class="text-sm line-clamp-2 mb-3"
                style="color: var(--theme-text-secondary);"
              >
                {{ item.description }}
              </p>

              <!-- 底部信息 -->
              <div class="flex items-center justify-between pt-3 border-t text-xs" style="border-color: var(--theme-border); color: var(--theme-text-secondary);">
                <span class="flex items-center">
                  <Star class="w-3 h-3 mr-1" style="color: var(--theme-primary);" />
                  {{ item.likeCount || 0 }} 点赞 · 通过率 {{ item.acceptanceRate || 0 }}%
                </span>
                <span v-if="bookmarkTime(item)" class="flex items-center">
                  <Bookmark class="w-3 h-3 mr-1" />
                  收藏于 {{ bookmarkTime(item) }}
                </span>
              </div>
            </div>
          </div>

          <!-- 分页 -->
          <div v-if="totalPages > 1" class="flex flex-wrap items-center justify-center gap-2 mt-8">
            <button
              @click="gotoPage(page - 1)"
              :disabled="page === 1"
              :aria-label="`第 ${page - 1} 页`"
              class="px-3 py-2 rounded-lg text-sm transition disabled:opacity-40 disabled:cursor-not-allowed flex items-center"
              style="background-color: var(--theme-surface); border: 1px solid var(--theme-border); color: var(--theme-text);"
            >
              <ChevronLeft class="w-4 h-4" />
              上一页
            </button>
            <span class="px-4 py-2 text-sm" style="color: var(--theme-text-secondary);">
              第 {{ page }} / {{ totalPages }} 页
            </span>
            <button
              @click="gotoPage(page + 1)"
              :disabled="page === totalPages"
              :aria-label="`第 ${page + 1} 页`"
              class="px-3 py-2 rounded-lg text-sm transition disabled:opacity-40 disabled:cursor-not-allowed flex items-center"
              style="background-color: var(--theme-surface); border: 1px solid var(--theme-border); color: var(--theme-text);"
            >
              下一页
              <ChevronRight class="w-4 h-4" />
            </button>
            <span class="ml-2 text-xs" style="color: var(--theme-text-secondary);">共 {{ total }} 条</span>
          </div>
        </template>
      </div>
    </div>

    <SiteFooter />
  </div>
</template>
