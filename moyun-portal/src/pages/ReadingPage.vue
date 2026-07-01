<script setup lang="ts">
import { ref, computed, onMounted } from 'vue';
import { useRouter } from 'vue-router';
import { useHead } from '@vueuse/head';
import { BookOpen, Star, ArrowRight, Users, Bookmark, BookmarkCheck, Quote, Compass, Clock, CheckCircle2 } from 'lucide-vue-next';
import SiteFooter from '@/components/SiteFooter.vue';
import LazyImage from '@/components/LazyImage.vue';

import { getReadingHome, toggleBookListBookmark, checkBookListBookmark, getRanking } from '@/api/reading';
import { generateSeo } from '@/utils/seo';
import { useUserStore } from '@/stores/user';
import type { Book, BookList, Quote as QuoteType, RankingType } from '@/types/api';

const router = useRouter();
const userStore = useUserStore();
const loading = ref(false);
const error = ref<string | null>(null);
const bookLists = ref<BookList[]>([]);
const books = ref<Book[]>([]);
const quotes = ref<QuoteType[]>([]);
// 第三阶段：排行榜区块（最近更新 / 连载中 / 完结好评）
const recentUpdate = ref<Book[]>([]);
const ongoingNovels = ref<Book[]>([]);
const completedBooks = ref<Book[]>([]);
// 书单收藏状态：{ [bookListId]: boolean }
const bookmarkStates = ref<Record<string | number, boolean>>({});
const bookmarkLoading = ref<Record<string | number, boolean>>({});

onMounted(() => {
  loadReadingHome();
  loadRankingSections();
});

async function loadReadingHome() {
  try {
    loading.value = true;
    error.value = null;

    const response = await getReadingHome();

    if (response.code === 200 && response.data) {
      bookLists.value = response.data.bookLists || [];
      books.value = response.data.books || [];
      quotes.value = response.data.quotes || [];
      // 登录用户批量查询收藏状态
      if (userStore.isAuthenticated) {
        bookLists.value.forEach(bl => {
          checkBookListBookmark(bl.id).then(resp => {
            if (resp.code === 200 && resp.data) {
              bookmarkStates.value[bl.id] = !!(resp.data as any).bookmarked;
            }
          }).catch(() => { /* ignore */ });
        });
      }
    } else {
      error.value = response.message || '加载数据失败';
    }
  } catch (err) {
    console.error('加载读书空间失败:', err);
    error.value = '加载数据失败，请稍后重试';
  } finally {
    loading.value = false;
  }
}

// 第三阶段：并行加载排行榜区块（最近更新 / 连载中 / 完结好评）
async function loadRankingSections() {
  const sections: { type: RankingType; target: typeof recentUpdate }[] = [
    { type: 'new', target: recentUpdate },
    { type: 'word_count', target: ongoingNovels },
    { type: 'completed', target: completedBooks },
  ];
  await Promise.all(
      sections.map(async (sec) => {
        try {
          const resp = await getRanking(sec.type, 6);
          if (resp.code === 200 && resp.data) {
            sec.target.value = (resp.data as any).list || [];
          }
        } catch (err) {
          // 静默忽略单个区块加载失败，不影响主页面
          console.error(`加载排行榜区块[${sec.type}]失败:`, err);
        }
      })
  );
}

async function handleToggleBookmark(bookListId: string | number) {
  if (!userStore.isAuthenticated) {
    router.push({ name: 'login', query: { redirect: router.currentRoute.value.fullPath } });
    return;
  }
  if (bookmarkLoading.value[bookListId]) return;
  bookmarkLoading.value[bookListId] = true;
  try {
    const resp = await toggleBookListBookmark(bookListId);
    if (resp.code === 200 && resp.data) {
      bookmarkStates.value[bookListId] = !!(resp.data as any).bookmarked;
    }
  } catch (err) {
    console.error('书单收藏失败:', err);
  } finally {
    bookmarkLoading.value[bookListId] = false;
  }
}

useHead(
    computed(() => {
      return generateSeo({
        title: '读书空间',
        description: '墨韵·智库读书空间，发现好书，分享阅读'
      });
    })
);
</script>

<template>
  <div class="min-h-screen flex flex-col" style="background-color: var(--theme-bg);">
    <!-- 页面头部：精简品牌区（与下方区块对齐，避免满屏渐变超出与文案重复） -->
    <div class="py-8 sm:py-10">
      <div class="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8">
        <div class="flex items-center justify-between flex-wrap gap-4">
          <div class="flex items-center gap-3">
            <div
                class="w-10 h-10 rounded-lg flex items-center justify-center"
                style="background-color: var(--theme-primary);"
            >
              <BookOpen class="w-5 h-5 text-white" />
            </div>
            <div>
              <h1 class="text-xl sm:text-2xl font-bold" style="color: var(--theme-text);">读书空间</h1>
              <p class="text-xs sm:text-sm" style="color: var(--theme-text-secondary);">发现好书，分享阅读，让知识流动起来</p>
            </div>
          </div>
          <button
              @click="router.push('/reading/discover')"
              class="inline-flex items-center gap-1 px-3 py-1.5 rounded-lg text-sm font-medium transition-colors hover:opacity-80"
              style="color: var(--theme-primary); background-color: var(--theme-accent);"
          >
            <Compass class="w-4 h-4" />
            <span>发现好书</span>
            <ArrowRight class="w-4 h-4" />
          </button>
        </div>
      </div>
    </div>

    <!-- 主要内容 -->
    <div class="flex-1 pb-8">
      <div class="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8">
        <div v-if="loading" class="text-center py-12">
          <div class="animate-spin rounded-full h-12 w-12 border-b-2 mx-auto" style="border-bottom-color: var(--theme-primary);"></div>
          <p class="mt-4" style="color: var(--theme-text-secondary);">加载中...</p>
        </div>

        <div v-else-if="error" class="text-center py-12">
          <div class="bg-red-50 border border-red-200 rounded-lg p-6 max-w-md mx-auto">
            <p class="text-red-600">{{ error }}</p>
            <button
                @click="loadReadingHome"
                class="mt-4 px-4 py-2 bg-red-500 text-white rounded-lg hover:bg-red-600 transition"
            >
              重试
            </button>
          </div>
        </div>

        <template v-else>
          <!-- 精选书单 -->
          <div v-if="bookLists.length > 0" class="mb-12">
            <div class="flex items-center justify-between mb-6">
              <h2 class="text-2xl font-bold flex items-center" style="color: var(--theme-text);">
                <BookOpen class="w-6 h-6 mr-2" style="color: var(--theme-primary);" />
                精选书单
              </h2>
              <button class="font-medium flex items-center hover:opacity-80" style="color: var(--theme-primary);">
                查看更多
                <ArrowRight class="w-4 h-4 ml-1" />
              </button>
            </div>

            <div class="grid grid-cols-1 md:grid-cols-2 gap-6">
              <div
                  v-for="(bookList, index) in bookLists"
                  :key="bookList.id"
                  @click="router.push(`/reading/book-list/${bookList.id}`)"
                  class="rounded-xl overflow-hidden shadow-sm hover:shadow-md transition cursor-pointer" style="background-color: var(--theme-surface); border: 1px solid var(--theme-border);"
              >
                <div class="h-48 relative" style="background: linear-gradient(135deg, var(--theme-accent), color-mix(in srgb, var(--theme-accent) 50%, #c4b5fd));">
                  <LazyImage
                      :src="bookList.cover"
                      :alt="bookList.title"
                      class="w-full h-full object-cover"
                  />
                </div>
                <div class="p-6">
                  <div class="flex items-start justify-between gap-3 mb-2">
                    <h3 class="text-lg font-semibold flex-1" style="color: var(--theme-text);">{{ bookList.title }}</h3>
                    <button
                        @click.stop="handleToggleBookmark(bookList.id)"
                        :disabled="bookmarkLoading[bookList.id]"
                        class="flex-shrink-0 p-1.5 rounded-lg transition-colors disabled:opacity-50 hover:bg-[var(--theme-accent)]"
                        :title="bookmarkStates[bookList.id] ? '取消收藏' : '收藏书单'"
                    >
                      <BookmarkCheck v-if="bookmarkStates[bookList.id]" class="w-5 h-5" style="color: var(--theme-primary);" />
                      <Bookmark v-else class="w-5 h-5" style="color: var(--theme-text-secondary);" />
                    </button>
                  </div>
                  <p class="text-sm mb-4 line-clamp-2" style="color: var(--theme-text-secondary);">{{ bookList.description }}</p>
                  <div class="flex items-center text-sm" style="color: var(--theme-text-secondary);">
                    <span class="flex items-center mr-4">
                      <BookOpen class="w-4 h-4 mr-1" />
                      {{ bookList.bookCount }} 本
                    </span>
                    <span class="flex items-center mr-4">
                      <Users class="w-4 h-4 mr-1" />
                      {{ bookList.viewCount }} 浏览
                    </span>
                    <span class="flex items-center">
                      <Star class="w-4 h-4 mr-1" />
                      {{ bookList.likeCount }}
                    </span>
                  </div>
                </div>
              </div>
            </div>
          </div>

          <!-- 热门书籍 -->
          <div v-if="books.length > 0" class="mb-12">
            <div class="flex items-center justify-between mb-6">
              <h2 class="text-2xl font-bold flex items-center" style="color: var(--theme-text);">
                <BookOpen class="w-6 h-6 mr-2" style="color: var(--theme-primary);" />
                热门书籍
              </h2>
              <button
                  @click="router.push('/reading')"
                  class="font-medium flex items-center hover:opacity-80" style="color: var(--theme-primary);"
              >
                读书空间专属页面
              </button>
            </div>

            <div class="grid grid-cols-2 md:grid-cols-3 lg:grid-cols-5 gap-6">
              <div
                  v-for="(book, index) in books"
                  :key="book.id"
                  @click="router.push(`/reading/book/${book.id}`)"
                  class="group cursor-pointer"
              >
                <div class="aspect-[3/4] rounded-lg overflow-hidden shadow-sm mb-3 group-hover:shadow-md transition">
                  <LazyImage
                      :src="book.cover"
                      :alt="book.title"
                      class="w-full h-full object-cover group-hover:scale-105 transition"
                  />
                </div>
                <h3 class="font-medium text-sm mb-1 line-clamp-2" style="color: var(--theme-text);">{{ book.title }}</h3>
                <p class="text-xs mb-2" style="color: var(--theme-text-secondary);">{{ book.author }}</p>
                <div class="flex items-center">
                  <Star class="w-3 h-3 text-yellow-400 mr-1" />
                  <span class="text-xs" style="color: var(--theme-text-secondary);">{{ book.rating }}</span>
                </div>
              </div>
            </div>
          </div>

          <!-- 最近更新 -->
          <div v-if="recentUpdate.length > 0" class="mb-12">
            <div class="flex items-center justify-between mb-6">
              <h2 class="text-2xl font-bold flex items-center" style="color: var(--theme-text);">
                <Clock class="w-6 h-6 mr-2" style="color: var(--theme-primary);" />
                最近更新
              </h2>
              <button
                  @click="router.push('/reading/discover')"
                  class="font-medium flex items-center hover:opacity-80" style="color: var(--theme-primary);"
              >
                查看更多
                <ArrowRight class="w-4 h-4 ml-1" />
              </button>
            </div>
            <div class="grid grid-cols-2 md:grid-cols-3 lg:grid-cols-6 gap-6">
              <div
                  v-for="book in recentUpdate"
                  :key="book.id"
                  @click="router.push(`/reading/book/${book.id}`)"
                  class="group cursor-pointer"
              >
                <div class="aspect-[3/4] rounded-lg overflow-hidden shadow-sm mb-3 group-hover:shadow-md transition">
                  <LazyImage
                      :src="book.cover"
                      :alt="book.title"
                      class="w-full h-full object-cover group-hover:scale-105 transition"
                  />
                </div>
                <h3 class="font-medium text-sm mb-1 line-clamp-2" style="color: var(--theme-text);">{{ book.title }}</h3>
                <p class="text-xs" style="color: var(--theme-text-secondary);">{{ book.author }}</p>
              </div>
            </div>
          </div>

          <!-- 连载中 -->
          <div v-if="ongoingNovels.length > 0" class="mb-12">
            <div class="flex items-center justify-between mb-6">
              <h2 class="text-2xl font-bold flex items-center" style="color: var(--theme-text);">
                <BookOpen class="w-6 h-6 mr-2" style="color: var(--theme-primary);" />
                连载中
              </h2>
              <button
                  @click="router.push('/reading/discover')"
                  class="font-medium flex items-center hover:opacity-80" style="color: var(--theme-primary);"
              >
                查看更多
                <ArrowRight class="w-4 h-4 ml-1" />
              </button>
            </div>
            <div class="grid grid-cols-2 md:grid-cols-3 lg:grid-cols-6 gap-6">
              <div
                  v-for="book in ongoingNovels"
                  :key="book.id"
                  @click="router.push(`/reading/book/${book.id}`)"
                  class="group cursor-pointer"
              >
                <div class="aspect-[3/4] rounded-lg overflow-hidden shadow-sm mb-3 group-hover:shadow-md transition relative">
                  <div class="absolute top-2 left-2 z-10 px-2 py-0.5 rounded text-xs font-bold text-white" style="background-color: var(--theme-primary);">
                    连载中
                  </div>
                  <LazyImage
                      :src="book.cover"
                      :alt="book.title"
                      class="w-full h-full object-cover group-hover:scale-105 transition"
                  />
                </div>
                <h3 class="font-medium text-sm mb-1 line-clamp-2" style="color: var(--theme-text);">{{ book.title }}</h3>
                <p class="text-xs" style="color: var(--theme-text-secondary);">{{ book.author }}</p>
              </div>
            </div>
          </div>

          <!-- 完结好评 -->
          <div v-if="completedBooks.length > 0" class="mb-12">
            <div class="flex items-center justify-between mb-6">
              <h2 class="text-2xl font-bold flex items-center" style="color: var(--theme-text);">
                <CheckCircle2 class="w-6 h-6 mr-2" style="color: var(--theme-primary);" />
                完结好评
              </h2>
              <button
                  @click="router.push('/reading/discover')"
                  class="font-medium flex items-center hover:opacity-80" style="color: var(--theme-primary);"
              >
                查看更多
                <ArrowRight class="w-4 h-4 ml-1" />
              </button>
            </div>
            <div class="grid grid-cols-2 md:grid-cols-3 lg:grid-cols-6 gap-6">
              <div
                  v-for="book in completedBooks"
                  :key="book.id"
                  @click="router.push(`/reading/book/${book.id}`)"
                  class="group cursor-pointer"
              >
                <div class="aspect-[3/4] rounded-lg overflow-hidden shadow-sm mb-3 group-hover:shadow-md transition relative">
                  <div class="absolute top-2 left-2 z-10 px-2 py-0.5 rounded text-xs font-bold text-white" style="background-color: #16a34a;">
                    完结
                  </div>
                  <LazyImage
                      :src="book.cover"
                      :alt="book.title"
                      class="w-full h-full object-cover group-hover:scale-105 transition"
                  />
                </div>
                <h3 class="font-medium text-sm mb-1 line-clamp-2" style="color: var(--theme-text);">{{ book.title }}</h3>
                <div class="flex items-center">
                  <Star class="w-3 h-3 text-yellow-400 mr-1" />
                  <span class="text-xs" style="color: var(--theme-text-secondary);">{{ book.rating }}</span>
                </div>
              </div>
            </div>
          </div>

          <!-- 金句摘录 -->
          <div v-if="quotes.length > 0">
            <div class="flex items-center justify-between mb-6">
              <h2 class="text-2xl font-bold flex items-center" style="color: var(--theme-text);">
                <Quote class="w-6 h-6 mr-2" style="color: var(--theme-primary);" />
                金句摘录
              </h2>
              <button class="font-medium flex items-center hover:opacity-80" style="color: var(--theme-primary);">
                查看更多
                <ArrowRight class="w-4 h-4 ml-1" />
              </button>
            </div>

            <div class="grid grid-cols-1 md:grid-cols-2 gap-6">
              <div
                  v-for="(quote, index) in quotes"
                  :key="quote.id"
                  class="rounded-xl p-6 shadow-sm hover:shadow-md transition" style="background-color: var(--theme-surface); border: 1px solid var(--theme-border);"
              >
                <Quote class="w-8 h-8 mb-4 opacity-30" style="color: var(--theme-primary);" />
                <p class="text-lg italic mb-6 leading-relaxed" style="color: var(--theme-text);">
                  "{{ quote.content }}"
                </p>
                <div class="flex items-center justify-between">
                  <div v-if="quote.book" class="flex items-center">
                    <div class="w-12 h-16 rounded overflow-hidden mr-3">
                      <LazyImage
                          :src="quote.book.cover"
                          :alt="quote.book.title"
                          class="w-full h-full object-cover"
                      />
                    </div>
                    <div>
                      <p class="text-sm font-medium" style="color: var(--theme-text);">{{ quote.book.title }}</p>
                      <p class="text-xs" style="color: var(--theme-text-secondary);">{{ quote.book.author }}</p>
                    </div>
                  </div>
                  <div class="flex items-center text-sm" style="color: var(--theme-text-secondary);">
                    <Star class="w-4 h-4 mr-1" />
                    {{ quote.likeCount }}
                  </div>
                </div>
              </div>
            </div>
          </div>
        </template>
      </div>R
    </div>

    <!-- 页脚 -->
    <SiteFooter />
  </div>
</template>
