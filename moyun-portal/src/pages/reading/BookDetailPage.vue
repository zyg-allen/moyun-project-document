<script setup lang="ts">
import { ref, onMounted, watch, computed } from 'vue';
import { useRoute, useRouter } from 'vue-router';
import { useHead } from '@vueuse/head';
import { BookOpen, Star, ArrowLeft, Users, Calendar, FileText, Tag, Quote, Heart } from 'lucide-vue-next';
import LazyImage from '@/components/LazyImage.vue';
import SiteFooter from '@/components/SiteFooter.vue';
import { generateSeo } from '@/utils/seo';
import { getBookDetail } from '@/api/reading';
import { formatShortDate } from '@/utils/date';
import type { Book, BookQuote } from '@/types/api';

const route = useRoute();
const router = useRouter();

const loading = ref(false);
const error = ref<string | null>(null);
const book = ref<Book | null>(null);
const quotes = ref<BookQuote[]>([]);

// 书籍标签数组（Book.tags 是字符串，需要拆分）
const bookTags = computed<string[]>(() => {
  if (!book.value?.tags) return [];
  return book.value.tags
    .split(/[,，;；|]/)
    .map(t => t.trim())
    .filter(Boolean);
});

// 评分星星（满 5 颗，按 rating 四舍五入显示）
const fullStars = computed(() => {
  if (!book.value) return 0;
  return Math.round(book.value.rating || 0);
});

// 加载书籍详情
async function loadBookDetail() {
  const bookId = route.params.id as string;
  if (!bookId) {
    error.value = '缺少书籍 ID';
    return;
  }

  loading.value = true;
  error.value = null;
  try {
    const response = await getBookDetail(bookId);
    if (response.code === 200 && response.data) {
      book.value = response.data.book;
      quotes.value = response.data.quotes || [];
    } else {
      error.value = response.message || '加载书籍详情失败';
      book.value = null;
      quotes.value = [];
    }
  } catch (err) {
    console.error('加载书籍详情失败:', err);
    error.value = '加载书籍详情失败，请稍后重试';
    book.value = null;
    quotes.value = [];
  } finally {
    loading.value = false;
  }
}

// 返回读书空间
function goBack() {
  router.push('/reading');
}

// SEO
useHead(
  computed(() => {
    if (!book.value) {
      return generateSeo({
        title: '书籍详情',
        description: '墨韵·智库读书空间 - 发现好书，分享阅读',
        type: 'article',
        canonicalPath: '/reading'
      });
    }
    const canonicalPath = `/reading/book/${book.value.id}`;
    return generateSeo({
      title: book.value.title,
      description: book.value.summary || book.value.description || '墨韵·智库读书空间',
      image: book.value.cover,
      type: 'article',
      keywords: bookTags.value,
      author: book.value.author || '',
      publishedTime: book.value.publishDate,
      canonicalPath,
      jsonLd: {
        '@context': 'https://schema.org',
        '@type': 'Book',
        name: book.value.title,
        author: {
          '@type': 'Person',
          name: book.value.author || ''
        },
        description: book.value.summary || book.value.description || '',
        isbn: book.value.isbn || '',
        publisher: {
          '@type': 'Organization',
          name: book.value.publisher || ''
        },
        numberOfPages: book.value.pageCount || 0,
        image: book.value.cover || '',
        url: canonicalPath
      }
    });
  })
);

onMounted(() => {
  loadBookDetail();
});

// 路由参数变化时重新加载
watch(
  () => route.params.id,
  (newId, oldId) => {
    if (newId && newId !== oldId) {
      loadBookDetail();
    }
  }
);
</script>

<template>
  <div class="min-h-screen flex flex-col" style="background-color: var(--theme-bg);">
    <!-- 顶部返回按钮 -->
    <div class="border-b py-3 sm:py-4" style="background-color: var(--theme-bg); border-color: var(--theme-border);">
      <div class="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8">
        <button
          @click="goBack"
          class="inline-flex items-center gap-2 text-sm font-medium transition-colors hover:opacity-80 focus:outline-none"
          style="color: var(--theme-text-secondary);"
          aria-label="返回读书空间"
        >
          <ArrowLeft class="w-4 h-4" aria-hidden="true" />
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
            @click="loadBookDetail"
            class="px-6 py-2 rounded-lg font-medium transition-colors focus:outline-none"
            style="background-color: var(--theme-primary); color: white;"
          >
            重试
          </button>
        </div>
      </div>
    </div>

    <!-- 空数据状态 -->
    <div v-else-if="!book" class="flex-1 py-16">
      <div class="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8">
        <div class="text-center">
          <BookOpen class="w-12 h-12 mx-auto mb-4" style="color: var(--theme-text-secondary);" aria-hidden="true" />
          <p class="text-lg" style="color: var(--theme-text-secondary);">暂无数据</p>
        </div>
      </div>
    </div>

    <!-- 书籍内容 -->
    <div v-else class="flex-1">
      <!-- Hero 区：渐变背景 -->
      <section
        class="py-8 sm:py-12"
        style="background: linear-gradient(135deg, var(--theme-primary) 0%, var(--theme-accent) 100%);"
      >
        <div class="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8">
          <div class="flex flex-col md:flex-row gap-8 items-start">
            <!-- 左侧：书籍封面 -->
            <div class="flex-shrink-0 mx-auto md:mx-0">
              <div
                class="w-48 sm:w-56 md:w-64 aspect-[3/4] rounded-xl overflow-hidden shadow-md"
                style="background-color: var(--theme-surface);"
              >
                <LazyImage
                  :src="book.cover"
                  :alt="book.title"
                  class="w-full h-full"
                />
              </div>
            </div>

            <!-- 右侧：书籍信息 -->
            <div class="flex-1 min-w-0 w-full">
              <h1
                class="text-2xl sm:text-3xl md:text-4xl font-bold leading-tight mb-3"
                style="color: #ffffff;"
              >
                {{ book.title }}
              </h1>

              <p class="text-base sm:text-lg mb-6" style="color: rgba(255, 255, 255, 0.9);">
                作者：{{ book.author || '未知' }}
              </p>

              <!-- 元信息网格 -->
              <div class="grid grid-cols-2 sm:grid-cols-3 gap-4 mb-6">
                <div v-if="book.publisher" class="flex items-center gap-2" style="color: rgba(255, 255, 255, 0.9);">
                  <BookOpen class="w-4 h-4 flex-shrink-0" aria-hidden="true" />
                  <span class="text-sm truncate">{{ book.publisher }}</span>
                </div>
                <div v-if="book.publishDate" class="flex items-center gap-2" style="color: rgba(255, 255, 255, 0.9);">
                  <Calendar class="w-4 h-4 flex-shrink-0" aria-hidden="true" />
                  <span class="text-sm">{{ formatShortDate(book.publishDate) }}</span>
                </div>
                <div v-if="book.pageCount" class="flex items-center gap-2" style="color: rgba(255, 255, 255, 0.9);">
                  <FileText class="w-4 h-4 flex-shrink-0" aria-hidden="true" />
                  <span class="text-sm">{{ book.pageCount }} 页</span>
                </div>
                <div v-if="book.isbn" class="flex items-center gap-2" style="color: rgba(255, 255, 255, 0.9);">
                  <Tag class="w-4 h-4 flex-shrink-0" aria-hidden="true" />
                  <span class="text-sm truncate">ISBN: {{ book.isbn }}</span>
                </div>
                <div class="flex items-center gap-2" style="color: rgba(255, 255, 255, 0.9);">
                  <Users class="w-4 h-4 flex-shrink-0" aria-hidden="true" />
                  <span class="text-sm">{{ book.readingCount || 0 }} 阅读</span>
                </div>
                <div class="flex items-center gap-2" style="color: rgba(255, 255, 255, 0.9);">
                  <Star class="w-4 h-4 flex-shrink-0" aria-hidden="true" />
                  <span class="text-sm">{{ book.rating || 0 }} 评分</span>
                </div>
              </div>

              <!-- 评分星星 -->
              <div class="flex items-center gap-2 mb-4">
                <div class="flex items-center gap-1">
                  <Star
                    v-for="n in 5"
                    :key="n"
                    class="w-5 h-5"
                    :class="n <= fullStars ? 'fill-current' : ''"
                    style="color: #ffd700;"
                    aria-hidden="true"
                  />
                </div>
                <span class="text-sm" style="color: rgba(255, 255, 255, 0.9);">
                  {{ book.rating || 0 }} / 5
                </span>
              </div>

              <!-- 标签 -->
              <div v-if="bookTags.length > 0" class="flex flex-wrap gap-2">
                <span
                  v-for="tag in bookTags"
                  :key="tag"
                  class="inline-flex items-center px-3 py-1 rounded-full text-xs font-medium"
                  style="background-color: rgba(255, 255, 255, 0.2); color: #ffffff;"
                >
                  {{ tag }}
                </span>
              </div>
            </div>
          </div>
        </div>
      </section>

      <!-- 内容区 -->
      <div class="py-8">
        <div class="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 space-y-6">
          <!-- 书籍描述 -->
          <section
            v-if="book.description"
            class="rounded-xl p-6 sm:p-8 shadow-sm hover:shadow-md transition"
            style="background-color: var(--theme-surface); border: 1px solid var(--theme-border);"
          >
            <h2 class="text-xl font-bold mb-4 flex items-center gap-2" style="color: var(--theme-text);">
              <BookOpen class="w-5 h-5" style="color: var(--theme-primary);" aria-hidden="true" />
              <span>内容简介</span>
            </h2>
            <p class="leading-relaxed whitespace-pre-line" style="color: var(--theme-text);">
              {{ book.description }}
            </p>
          </section>

          <!-- 书籍摘要 -->
          <section
            v-if="book.summary"
            class="rounded-xl p-6 sm:p-8 shadow-sm hover:shadow-md transition"
            style="background-color: var(--theme-surface); border: 1px solid var(--theme-border);"
          >
            <h2 class="text-xl font-bold mb-4 flex items-center gap-2" style="color: var(--theme-text);">
              <FileText class="w-5 h-5" style="color: var(--theme-primary);" aria-hidden="true" />
              <span>摘要</span>
            </h2>
            <p class="leading-relaxed italic whitespace-pre-line" style="color: var(--theme-text-secondary);">
              {{ book.summary }}
            </p>
          </section>

          <!-- 作者简介 -->
          <section
            v-if="book.authorBio"
            class="rounded-xl p-6 sm:p-8 shadow-sm hover:shadow-md transition"
            style="background-color: var(--theme-surface); border: 1px solid var(--theme-border);"
          >
            <h2 class="text-xl font-bold mb-4 flex items-center gap-2" style="color: var(--theme-text);">
              <Users class="w-5 h-5" style="color: var(--theme-primary);" aria-hidden="true" />
              <span>作者简介</span>
            </h2>
            <div class="flex items-start gap-4">
              <div
                class="flex-shrink-0 w-12 h-12 rounded-full flex items-center justify-center"
                style="background-color: var(--theme-accent); color: var(--theme-primary);"
              >
                <Users class="w-6 h-6" aria-hidden="true" />
              </div>
              <div class="flex-1 min-w-0">
                <p class="font-medium mb-2" style="color: var(--theme-text);">
                  {{ book.author || '未知作者' }}
                </p>
                <p class="leading-relaxed whitespace-pre-line" style="color: var(--theme-text-secondary);">
                  {{ book.authorBio }}
                </p>
              </div>
            </div>
          </section>

          <!-- 相关金句列表 -->
          <section
            v-if="quotes.length > 0"
            class="rounded-xl p-6 sm:p-8 shadow-sm hover:shadow-md transition"
            style="background-color: var(--theme-surface); border: 1px solid var(--theme-border);"
          >
            <h2 class="text-xl font-bold mb-6 flex items-center gap-2" style="color: var(--theme-text);">
              <Quote class="w-5 h-5" style="color: var(--theme-primary);" aria-hidden="true" />
              <span>相关金句 ({{ quotes.length }})</span>
            </h2>

            <div class="grid grid-cols-1 md:grid-cols-2 gap-4">
              <article
                v-for="quote in quotes"
                :key="quote.id"
                class="rounded-xl p-5 shadow-sm hover:shadow-md transition"
                style="background-color: var(--theme-bg); border: 1px solid var(--theme-border);"
              >
                <Quote class="w-6 h-6 mb-3" style="color: var(--theme-primary);" aria-hidden="true" />
                <p class="leading-relaxed italic mb-4" style="color: var(--theme-text);">
                  "{{ quote.content }}"
                </p>
                <div class="flex items-center justify-between flex-wrap gap-2 text-sm" style="color: var(--theme-text-secondary);">
                  <div class="flex items-center gap-3 flex-wrap">
                    <span v-if="quote.chapter" class="flex items-center gap-1">
                      <BookOpen class="w-3.5 h-3.5" aria-hidden="true" />
                      <span>{{ quote.chapter }}</span>
                    </span>
                    <span v-if="quote.page" class="flex items-center gap-1">
                      <FileText class="w-3.5 h-3.5" aria-hidden="true" />
                      <span>第 {{ quote.page }} 页</span>
                    </span>
                  </div>
                  <span class="flex items-center gap-1">
                    <Heart class="w-3.5 h-3.5" aria-hidden="true" />
                    <span>{{ quote.likeCount || 0 }}</span>
                  </span>
                </div>
              </article>
            </div>
          </section>
        </div>
      </div>
    </div>

    <!-- 页脚 -->
    <div class="mt-auto">
      <SiteFooter />
    </div>
  </div>
</template>
