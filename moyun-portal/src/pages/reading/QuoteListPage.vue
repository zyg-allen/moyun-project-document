<script setup lang="ts">
import { ref, computed, onMounted, onUnmounted } from 'vue';
import { useRouter } from 'vue-router';
import { useHead } from '@vueuse/head';
import { Quote, Star, User, Calendar, BookOpen } from 'lucide-vue-next';
import Breadcrumb from '@/components/Breadcrumb.vue';
import SiteFooter from '@/components/SiteFooter.vue';
import LazyImage from '@/components/LazyImage.vue';
import Empty from '@/components/Empty.vue';
import { generateSeo } from '@/utils/seo';
import { getQuoteList, toggleQuoteLike, checkQuoteLike } from '@/api/reading';
import type { BookQuote } from '@/types/api';
import { useUserStore } from '@/stores/user';
import { formatDate } from '@/utils/date';

const router = useRouter();
const userStore = useUserStore();
useHead(generateSeo({ title: '金句摘录', description: '精选书籍金句，与书友一起品味文字', keywords: ['金句摘录', '读书金句', '书籍名言'], type: 'website' }));

const breadcrumbs = computed(() => [
  { label: '读书空间', path: '/reading' },
  { label: '金句摘录' },
]);

const loading = ref(false);
const quotes = ref<BookQuote[]>([]);
const page = ref(1);
const pageSize = 20;
const total = ref(0);
const loadingMore = ref(false);
const noMore = ref(false);
const likeMap = ref<Record<string, boolean>>({});

async function load(reset = false) {
  if (reset) {
    page.value = 1;
    quotes.value = [];
    noMore.value = false;
  }
  if (noMore.value) return;

  if (reset) {
    loading.value = true;
  } else {
    loadingMore.value = true;
  }
  try {
    const resp = await getQuoteList({ pageNum: page.value, pageSize });
    if (resp.code === 200 && resp.data) {
      const list = resp.data.list || [];
      quotes.value = reset ? list : [...quotes.value, ...list];
      total.value = resp.data.total || 0;
      if (list.length < pageSize) {
        noMore.value = true;
      } else {
        page.value += 1;
      }
      // 批量检查点赞状态
      if (userStore.isAuthenticated) {
        const ids = list.map((q: BookQuote) => q.id);
        for (const id of ids) {
          try {
            const res = await checkQuoteLike(id);
            if (res.code === 200 && res.data) {
              likeMap.value[id] = !!res.data.liked;
            }
          } catch {}
        }
      }
    }
  } catch {
    // 静默失败
  } finally {
    loading.value = false;
    loadingMore.value = false;
  }
}

async function handleLike(quote: BookQuote, e: Event) {
  e.stopPropagation();
  if (!userStore.isAuthenticated) {
    router.push('/login');
    return;
  }
  const wasLiked = !!likeMap.value[quote.id];
  likeMap.value[quote.id] = !wasLiked;
  quote.likeCount = (quote.likeCount || 0) + (wasLiked ? -1 : 1);
  try {
    const resp = await toggleQuoteLike(quote.id);
    if (resp.code === 200 && resp.data) {
      likeMap.value[quote.id] = !!resp.data.liked;
      quote.likeCount = resp.data.likeCount || 0;
    }
  } catch {
    // 回滚
    likeMap.value[quote.id] = wasLiked;
    quote.likeCount = (quote.likeCount || 0) + (wasLiked ? 1 : -1);
  }
}

function goBookDetail(bookId: string | number, e: Event) {
  e.stopPropagation();
  router.push(`/reading/book/${bookId}`);
}

function handleScroll() {
  if (loading.value || loadingMore.value || noMore.value) return;
  const scrollTop = window.scrollY;
  const clientHeight = window.innerHeight;
  const scrollHeight = document.documentElement.scrollHeight;
  if (scrollTop + clientHeight >= scrollHeight - 200) {
    load(false);
  }
}

onMounted(() => {
  load(true);
  window.addEventListener('scroll', handleScroll);
});

onUnmounted(() => {
  window.removeEventListener('scroll', handleScroll);
});
</script>

<template>
  <div class="min-h-screen flex flex-col" style="background-color: var(--theme-bg);">
    <div class="border-b sticky top-0 z-30 backdrop-blur-sm py-3" style="background-color: var(--theme-surface); border-color: var(--theme-border);">
      <div class="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 flex items-center justify-between gap-4">
        <Breadcrumb :items="breadcrumbs" />
      </div>
    </div>

    <main class="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 py-6 pb-20">
      <div v-if="loading && quotes.length === 0" class="text-center py-16" style="color: var(--theme-text-secondary);">加载中...</div>
      <div v-else-if="quotes.length === 0">
        <Empty description="暂无金句摘录" />
      </div>
      <template v-else>
        <div class="grid grid-cols-1 md:grid-cols-2 gap-6">
          <article
            v-for="quote in quotes"
            :key="quote.id"
            class="rounded-xl p-6 shadow-sm hover:shadow-md transition cursor-pointer"
            style="background-color: var(--theme-surface); border: 1px solid var(--theme-border);"
            @click="goBookDetail(quote.bookId, $event)"
          >
            <Quote class="w-8 h-8 mb-4 opacity-30" style="color: var(--theme-primary);" />
            <p class="text-lg italic mb-5 leading-relaxed" style="color: var(--theme-text);">
              "{{ quote.content }}"
            </p>

            <!-- 出处（书籍信息） -->
            <div class="flex items-center justify-between mb-4">
              <div class="flex items-center min-w-0" @click="goBookDetail(quote.bookId, $event)">
                <div v-if="quote.bookCover" class="w-10 h-14 rounded overflow-hidden mr-3 flex-shrink-0">
                  <LazyImage
                    :src="quote.bookCover"
                    :alt="quote.bookTitle"
                    class="w-full h-full object-cover"
                  />
                </div>
                <div v-else class="w-10 h-14 rounded mr-3 flex-shrink-0 flex items-center justify-center" style="background-color: var(--theme-bg);">
                  <BookOpen class="w-5 h-5" style="color: var(--theme-text-secondary);" />
                </div>
                <div class="min-w-0">
                  <p class="text-sm font-medium truncate" style="color: var(--theme-text);">{{ quote.bookTitle || '未知书籍' }}</p>
                  <p class="text-xs truncate" style="color: var(--theme-text-secondary);">{{ quote.bookAuthor || '佚名' }}</p>
                </div>
              </div>
            </div>

            <!-- 摘录人 + 时间 + 点赞 -->
            <div class="flex items-center justify-between pt-3 border-t" style="border-color: var(--theme-border);">
              <div class="flex items-center gap-4 text-xs" style="color: var(--theme-text-secondary);">
                <span class="flex items-center gap-1">
                  <User class="w-3.5 h-3.5" />
                  {{ quote.userNickname || '匿名用户' }}
                </span>
                <span class="flex items-center gap-1">
                  <Calendar class="w-3.5 h-3.5" />
                  {{ formatDate(quote.createTime) }}
                </span>
              </div>
              <button
                class="flex items-center gap-1 text-sm transition hover:opacity-80"
                :style="{ color: likeMap[quote.id] ? 'var(--theme-primary)' : 'var(--theme-text-secondary)' }"
                @click="handleLike(quote, $event)"
              >
                <Star class="w-4 h-4" :class="{ 'fill-current': likeMap[quote.id] }" />
                {{ quote.likeCount || 0 }}
              </button>
            </div>
          </article>
        </div>

        <div v-if="loadingMore" class="text-center py-8 text-sm" style="color: var(--theme-text-secondary);">加载更多...</div>
        <div v-else-if="noMore && quotes.length > 0" class="text-center py-8 text-xs" style="color: var(--theme-text-secondary);">—— 没有更多了 ——</div>
      </template>
    </main>

    <SiteFooter />
  </div>
</template>
