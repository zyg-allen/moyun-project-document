<script setup lang="ts">
import { ref, onMounted, watch, computed } from 'vue';
import { useRoute, useRouter } from 'vue-router';
import { useHead } from '@vueuse/head';
import { BookOpen, Star, ArrowLeft, Users, Heart, Bookmark, BookmarkCheck, Eye } from 'lucide-vue-next';
import LazyImage from '@/components/LazyImage.vue';
import SiteFooter from '@/components/SiteFooter.vue';
import { generateSeo } from '@/utils/seo';
import { getBookListDetail, toggleBookListBookmark, checkBookListBookmark } from '@/api/reading';
import { useUserStore } from '@/stores/user';
import type { Book, BookList } from '@/types/api';

const route = useRoute();
const router = useRouter();
const userStore = useUserStore();

const loading = ref(false);
const error = ref<string | null>(null);
const bookList = ref<BookList | null>(null);
const books = ref<Book[]>([]);
const bookmarked = ref(false);
const bookmarkLoading = ref(false);

const listId = computed(() => route.params.id as string);

// 书单标签（tags 为逗号分隔字符串）
const tagList = computed<string[]>(() => {
  if (!bookList.value?.tags) return [];
  return bookList.value.tags
    .split(',')
    .map(t => t.trim())
    .filter(Boolean);
});

async function loadDetail() {
  if (!listId.value) return;
  try {
    loading.value = true;
    error.value = null;
    bookList.value = null;
    books.value = [];
    bookmarked.value = false;

    const response = await getBookListDetail(listId.value);
    if (response.code === 200 && response.data) {
      bookList.value = response.data.bookList || null;
      books.value = response.data.books || [];
      // 登录用户检查收藏状态
      if (userStore.isAuthenticated && bookList.value) {
        checkBookListBookmark(bookList.value.id)
          .then(resp => {
            if (resp.code === 200 && resp.data) {
              bookmarked.value = !!(resp.data as any).bookmarked;
            }
          })
          .catch(() => { /* ignore */ });
      }
    } else {
      error.value = response.message || '加载书单失败';
    }
  } catch (err) {
    console.error('加载书单详情失败:', err);
    error.value = '加载书单失败，请稍后重试';
  } finally {
    loading.value = false;
  }
}

async function handleToggleBookmark() {
  if (!bookList.value) return;
  if (!userStore.isAuthenticated) {
    router.push({ name: 'login', query: { redirect: router.currentRoute.value.fullPath } });
    return;
  }
  if (bookmarkLoading.value) return;
  bookmarkLoading.value = true;
  try {
    const resp = await toggleBookListBookmark(bookList.value.id);
    if (resp.code === 200 && resp.data) {
      bookmarked.value = !!(resp.data as any).bookmarked;
    }
  } catch (err) {
    console.error('书单收藏失败:', err);
  } finally {
    bookmarkLoading.value = false;
  }
}

function goBack() {
  router.push({ name: 'reading' });
}

function goToBook(book: Book) {
  router.push({ name: 'book-detail', params: { id: book.id } });
}

useHead(
  computed(() => {
    if (!bookList.value) {
      return generateSeo({
        title: '书单详情',
        description: '墨韵·智库读书空间书单详情，发现精选好书',
        canonicalPath: '/reading'
      });
    }
    return generateSeo({
      title: bookList.value.title,
      description: bookList.value.description || '墨韵·智库读书空间书单详情',
      image: bookList.value.cover,
      type: 'article',
      keywords: tagList.value,
      canonicalPath: `/reading/book-list/${bookList.value.id}`
    });
  })
);

onMounted(() => {
  loadDetail();
});

// 路由参数变化时重新加载
watch(listId, (newId, oldId) => {
  if (newId && newId !== oldId) {
    loadDetail();
  }
});
</script>

<template>
  <div class="min-h-screen flex flex-col" style="background-color: var(--theme-bg);">
    <!-- 顶部返回栏 -->
    <div class="border-b" style="background-color: var(--theme-bg); border-color: var(--theme-border);">
      <div class="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 py-3 sm:py-4">
        <button
          @click="goBack"
          class="inline-flex items-center gap-2 text-sm font-medium transition-colors hover:opacity-80 focus:outline-none"
          style="color: var(--theme-text-secondary);"
        >
          <ArrowLeft class="w-4 h-4" />
          <span>返回读书空间</span>
        </button>
      </div>
    </div>

    <!-- 加载状态 -->
    <div v-if="loading" class="flex-1 py-16">
      <div class="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8">
        <div class="text-center">
          <div
            class="inline-block w-12 h-12 border-4 border-t-4 rounded-full animate-spin"
            style="border-color: var(--theme-border); border-top-color: var(--theme-primary);"
          ></div>
          <p class="mt-4" style="color: var(--theme-text-secondary);">加载中...</p>
        </div>
      </div>
    </div>

    <!-- 错误状态 -->
    <div v-else-if="error" class="flex-1 py-16">
      <div class="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8">
        <div class="text-center">
          <p class="text-lg mb-4" style="color: var(--theme-text);">{{ error }}</p>
          <button
            @click="loadDetail"
            class="px-6 py-2 rounded-lg font-medium transition-colors"
            style="background-color: var(--theme-primary); color: white;"
          >
            重试
          </button>
        </div>
      </div>
    </div>

    <!-- 书单内容 -->
    <template v-else-if="bookList">
      <!-- Hero 区 -->
      <section class="relative overflow-hidden" style="background-color: var(--theme-primary);">
        <!-- 渐变叠加层，增加层次感 -->
        <div
          class="absolute inset-0 pointer-events-none"
          style="background: linear-gradient(135deg, rgba(255,255,255,0.12) 0%, rgba(0,0,0,0.20) 100%);"
        ></div>
        <div class="relative max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 py-10 sm:py-14">
          <div class="flex flex-col md:flex-row gap-6 sm:gap-8 items-start">
            <!-- 书单封面 -->
            <div class="flex-shrink-0 mx-auto md:mx-0">
              <div
                class="w-40 h-52 sm:w-48 sm:h-64 rounded-xl overflow-hidden shadow-lg"
                style="background-color: rgba(255,255,255,0.2);"
              >
                <LazyImage
                  :src="bookList.cover"
                  :alt="bookList.title"
                  class="w-full h-full object-cover"
                />
              </div>
            </div>

            <!-- 书单信息 -->
            <div class="flex-1 min-w-0 text-center md:text-left">
              <h1 class="text-2xl sm:text-3xl lg:text-4xl font-bold text-white leading-tight break-words">
                {{ bookList.title }}
              </h1>

              <p
                v-if="bookList.description"
                class="mt-4 text-base sm:text-lg text-white/90 leading-relaxed max-w-3xl"
              >
                {{ bookList.description }}
              </p>

              <!-- 标签 -->
              <div v-if="tagList.length > 0" class="mt-4 flex flex-wrap gap-2 justify-center md:justify-start">
                <span
                  v-for="tag in tagList"
                  :key="tag"
                  class="px-3 py-1 rounded-full text-xs font-medium"
                  style="background-color: rgba(255,255,255,0.2); color: white;"
                >
                  {{ tag }}
                </span>
              </div>

              <!-- 统计信息 -->
              <div class="mt-6 flex flex-wrap items-center gap-4 sm:gap-6 justify-center md:justify-start text-white/90">
                <span class="flex items-center text-sm sm:text-base">
                  <BookOpen class="w-4 h-4 sm:w-5 sm:h-5 mr-1.5" />
                  {{ bookList.bookCount }} 本
                </span>
                <span class="flex items-center text-sm sm:text-base">
                  <Eye class="w-4 h-4 sm:w-5 sm:h-5 mr-1.5" />
                  {{ bookList.viewCount }} 浏览
                </span>
                <span class="flex items-center text-sm sm:text-base">
                  <Heart class="w-4 h-4 sm:w-5 sm:h-5 mr-1.5" />
                  {{ bookList.likeCount }} 点赞
                </span>
              </div>

              <!-- 收藏按钮 -->
              <div class="mt-6 flex justify-center md:justify-start">
                <button
                  @click="handleToggleBookmark"
                  :disabled="bookmarkLoading"
                  class="inline-flex items-center gap-2 px-5 py-2.5 rounded-full font-medium text-sm transition-all hover:scale-105 focus:outline-none disabled:opacity-50 disabled:cursor-not-allowed"
                  :style="bookmarked
                    ? { backgroundColor: 'rgba(255,255,255,0.95)', color: 'var(--theme-primary)' }
                    : { backgroundColor: 'rgba(255,255,255,0.2)', color: 'white', border: '1px solid rgba(255,255,255,0.5)' }"
                  :aria-pressed="bookmarked"
                  :aria-label="bookmarked ? '取消收藏' : '收藏书单'"
                >
                  <BookmarkCheck v-if="bookmarked" class="w-5 h-5" />
                  <Bookmark v-else class="w-5 h-5" />
                  <span>{{ bookmarked ? '已收藏' : '收藏书单' }}</span>
                </button>
              </div>
            </div>
          </div>
        </div>
      </section>

      <!-- 书籍列表区 -->
      <div class="flex-1 py-8 sm:py-10">
        <div class="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8">
          <div class="flex items-center justify-between mb-6">
            <h2 class="text-xl sm:text-2xl font-bold flex items-center" style="color: var(--theme-text);">
              <BookOpen class="w-5 h-5 sm:w-6 sm:h-6 mr-2" style="color: var(--theme-primary);" />
              书单书籍
              <span class="ml-2 text-base font-normal" style="color: var(--theme-text-secondary);">
                ({{ books.length }} 本)
              </span>
            </h2>
          </div>

          <!-- 空数据状态 -->
          <div v-if="books.length === 0" class="text-center py-16">
            <BookOpen class="w-12 h-12 mx-auto mb-4" style="color: var(--theme-text-secondary); opacity: 0.5;" />
            <p class="text-lg" style="color: var(--theme-text-secondary);">暂无书籍</p>
          </div>

          <!-- 书籍网格 -->
          <div v-else class="grid grid-cols-2 sm:grid-cols-3 md:grid-cols-4 lg:grid-cols-5 gap-4 sm:gap-6">
            <div
              v-for="book in books"
              :key="book.id"
              class="group cursor-pointer"
              @click="goToBook(book)"
            >
              <div
                class="aspect-[3/4] rounded-xl overflow-hidden shadow-sm mb-3 transition group-hover:shadow-md"
                style="background-color: var(--theme-surface);"
              >
                <LazyImage
                  :src="book.cover"
                  :alt="book.title"
                  class="w-full h-full object-cover transition group-hover:scale-105"
                />
              </div>
              <h3 class="font-medium text-sm mb-1 line-clamp-2" style="color: var(--theme-text);">
                {{ book.title }}
              </h3>
              <p class="text-xs mb-2 line-clamp-1" style="color: var(--theme-text-secondary);">
                {{ book.author }}
              </p>
              <div class="flex items-center">
                <Star class="w-3.5 h-3.5 mr-1" style="color: #FBBF24;" />
                <span class="text-xs" style="color: var(--theme-text-secondary);">
                  {{ book.rating ?? '-' }}
                </span>
              </div>
            </div>
          </div>
        </div>
      </div>
    </template>

    <!-- 兜底空数据 -->
    <div v-else class="flex-1 py-16">
      <div class="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 text-center">
        <p class="text-lg" style="color: var(--theme-text-secondary);">暂无数据</p>
      </div>
    </div>

    <!-- 页脚 -->
    <div class="mt-auto">
      <SiteFooter />
    </div>
  </div>
</template>
