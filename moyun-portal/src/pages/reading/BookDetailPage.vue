<script setup lang="ts">
import { ref, onMounted, watch, computed } from 'vue';
import { useRoute, useRouter } from 'vue-router';
import { useHead } from '@vueuse/head';
import { BookOpen, Star, ArrowLeft, Users, Calendar, FileText, Tag, Quote, Heart, List, ChevronRight } from 'lucide-vue-next';
import LazyImage from '@/components/LazyImage.vue';
import SiteFooter from '@/components/SiteFooter.vue';
import BookshelfButton from '@/components/reading/BookshelfButton.vue';
import { generateSeo } from '@/utils/seo';
import { getBookDetail, getBookChapterList, getReadingProgress } from '@/api/reading';
import { useAuth } from '@/composables/useAuth';
import { formatShortDate } from '@/utils/date';
import type { Book, BookChapter, BookQuote, ReadingProgress } from '@/types/api';

const route = useRoute();
const router = useRouter();
const { isAuthenticated } = useAuth();

const loading = ref(false);
const error = ref<string | null>(null);
const book = ref<Book | null>(null);
const quotes = ref<BookQuote[]>([]);
// v1.0 新增：章节目录（仅 novel/longform 类型加载）
const chapters = ref<BookChapter[]>([]);
const chaptersLoading = ref(false);
// v1.0 第二阶段：阅读进度（用于"继续阅读"入口）
const readingProgress = ref<ReadingProgress | null>(null);

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
      // v1.0：网络小说/长文文章类型，加载章节目录
      loadChapters(bookId);
      // v1.0 第二阶段：登录用户加载阅读进度（用于"继续阅读"）
      loadReadingProgress(bookId);
    } else {
      error.value = response.message || '加载书籍详情失败';
      book.value = null;
      quotes.value = [];
      chapters.value = [];
    }
  } catch (err) {
    console.error('加载书籍详情失败:', err);
    error.value = '加载书籍详情失败，请稍后重试';
    book.value = null;
    quotes.value = [];
    chapters.value = [];
  } finally {
    loading.value = false;
  }
}

// v1.0 新增：加载章节目录
async function loadChapters(bookId: string | number) {
  chaptersLoading.value = true;
  try {
    const resp = await getBookChapterList(bookId);
    if (resp.code === 200 && resp.data) {
      chapters.value = Array.isArray(resp.data) ? resp.data : [];
    } else {
      chapters.value = [];
    }
  } catch (err) {
    console.warn('加载章节目录失败:', err);
    chapters.value = [];
  } finally {
    chaptersLoading.value = false;
  }
}

// v1.0 第二阶段：加载阅读进度（登录用户）
async function loadReadingProgress(bookId: string | number) {
  readingProgress.value = null;
  if (!isAuthenticated()) return;
  try {
    const resp = await getReadingProgress(bookId);
    if (resp.code === 200 && resp.data) {
      readingProgress.value = resp.data as ReadingProgress;
    }
  } catch (err) {
    // 静默忽略
  }
}

// 继续阅读的章节 ID（有进度且 currentChapterId 存在时）
const continueChapterId = computed<string | number | null>(() => {
  const p = readingProgress.value;
  if (!p || !p.currentChapterId) return null;
  return p.currentChapterId;
});

// 是否展示章节目录与阅读入口
// 修复：原逻辑仅允许 novel/longform 显示按钮，导致 published 类型（且 26号种子未填 type）整书无阅读入口
// 改为：只要存在已发布章节即展示，type 仅用于文案区分
const hasChapters = computed(() => {
  return chapters.value.length > 0;
});

// 第一章（用于"开始阅读"按钮）
const firstChapter = computed(() => {
  return chapters.value.length > 0 ? chapters.value[0] : null;
});

// 最新章节（用于显示"最新更新"）
const latestChapter = computed(() => {
  if (chapters.value.length === 0) return null;
  return chapters.value[chapters.value.length - 1];
});

// 书籍类型显示文本
const bookTypeText = computed(() => {
  const t = book.value?.type;
  if (t === 'novel') return '网络小说';
  if (t === 'longform') return '长文文章';
  if (t === 'published') return '出版书籍';
  return '';
});

// 连载状态显示文本
const serialStatusText = computed(() => {
  const s = book.value?.serialStatus;
  if (s === 'ongoing') return '连载中';
  if (s === 'completed') return '已完结';
  if (s === 'hiatus') return '暂停更新';
  return '';
});

// 跳转到章节阅读
function goReadChapter(chapterId: string | number) {
  const bookId = route.params.id as string;
  if (bookId && chapterId) {
    router.push(`/reading/book/${bookId}/chapter/${chapterId}`);
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
              <div v-if="bookTags.length > 0 || bookTypeText || serialStatusText" class="flex flex-wrap gap-2 mb-4">
                <span
                    v-if="bookTypeText"
                    class="inline-flex items-center px-3 py-1 rounded-full text-xs font-medium"
                    style="background-color: rgba(255, 255, 255, 0.3); color: #ffffff;"
                >
                  {{ bookTypeText }}
                </span>
                <span
                    v-if="serialStatusText"
                    class="inline-flex items-center px-3 py-1 rounded-full text-xs font-medium"
                    style="background-color: rgba(255, 255, 255, 0.3); color: #ffffff;"
                >
                  {{ serialStatusText }}
                </span>
                <span
                    v-if="book.chapterCount"
                    class="inline-flex items-center px-3 py-1 rounded-full text-xs font-medium"
                    style="background-color: rgba(255, 255, 255, 0.3); color: #ffffff;"
                >
                  {{ book.chapterCount }} 章
                </span>
                <span
                    v-if="book.wordCount"
                    class="inline-flex items-center px-3 py-1 rounded-full text-xs font-medium"
                    style="background-color: rgba(255, 255, 255, 0.3); color: #ffffff;"
                >
                  {{ book.wordCount }} 字
                </span>
                <span
                    v-for="tag in bookTags"
                    :key="tag"
                    class="inline-flex items-center px-3 py-1 rounded-full text-xs font-medium"
                    style="background-color: rgba(255, 255, 255, 0.2); color: #ffffff;"
                >
                  {{ tag }}
                </span>
              </div>

              <!-- v1.0 新增：开始阅读/继续阅读 + 加入书架 + 最新章节 -->
              <div v-if="hasChapters" class="flex flex-wrap items-center gap-3">
                <!-- 继续阅读（有阅读进度时优先显示） -->
                <button
                    v-if="continueChapterId"
                    type="button"
                    class="inline-flex items-center gap-2 px-6 py-2.5 rounded-lg font-medium text-sm transition-all hover:shadow-lg focus:outline-none"
                    style="background-color: #ffffff; color: var(--theme-primary);"
                    @click="goReadChapter(continueChapterId)"
                >
                  <BookOpen class="w-4 h-4" aria-hidden="true" />
                  <span>继续阅读</span>
                </button>
                <!-- 开始阅读（无阅读进度时显示） -->
                <button
                    v-else-if="firstChapter"
                    type="button"
                    class="inline-flex items-center gap-2 px-6 py-2.5 rounded-lg font-medium text-sm transition-all hover:shadow-lg focus:outline-none"
                    style="background-color: #ffffff; color: var(--theme-primary);"
                    @click="goReadChapter(firstChapter.id)"
                >
                  <BookOpen class="w-4 h-4" aria-hidden="true" />
                  <span>开始阅读</span>
                </button>
                <!-- 加入书架 -->
                <BookshelfButton
                    v-if="book.id"
                    :book-id="book.id"
                    size="md"
                    variant="solid"
                />
                <div v-if="latestChapter" class="text-sm w-full sm:w-auto" style="color: rgba(255, 255, 255, 0.9);">
                  <span class="opacity-80">最新：</span>
                  <button
                      type="button"
                      class="font-medium underline-offset-2 hover:underline focus:outline-none"
                      style="color: #ffffff;"
                      @click="goReadChapter(latestChapter.id)"
                  >
                    {{ latestChapter.title }}
                  </button>
                </div>
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

          <!-- v1.0 新增：章节目录 -->
          <section
              v-if="hasChapters"
              class="rounded-xl p-6 sm:p-8 shadow-sm hover:shadow-md transition"
              style="background-color: var(--theme-surface); border: 1px solid var(--theme-border);"
          >
            <div class="flex items-center justify-between mb-4">
              <h2 class="text-xl font-bold flex items-center gap-2" style="color: var(--theme-text);">
                <List class="w-5 h-5" style="color: var(--theme-primary);" aria-hidden="true" />
                <span>章节目录</span>
                <span class="text-sm font-normal" style="color: var(--theme-text-secondary);">
                  （共 {{ chapters.length }} 章）
                </span>
              </h2>
              <button
                  v-if="firstChapter"
                  type="button"
                  class="inline-flex items-center gap-1 text-sm font-medium transition-colors hover:opacity-80 focus:outline-none"
                  style="color: var(--theme-primary);"
                  @click="goReadChapter(firstChapter.id)"
              >
                <span>开始阅读</span>
                <ChevronRight class="w-4 h-4" aria-hidden="true" />
              </button>
            </div>

            <div v-if="chaptersLoading" class="py-6 text-center text-sm" style="color: var(--theme-text-secondary);">
              加载中...
            </div>
            <ul v-else class="grid grid-cols-1 sm:grid-cols-2 gap-1">
              <li
                  v-for="chapter in chapters.slice(0, 12)"
                  :key="chapter.id"
              >
                <button
                    type="button"
                    class="w-full flex items-center gap-2 px-3 py-2 rounded-lg text-left text-sm transition-colors hover:bg-opacity-50"
                    style="color: var(--theme-text);"
                    @click="goReadChapter(chapter.id)"
                    @mouseenter="($event.currentTarget as HTMLElement).style.backgroundColor = 'var(--theme-bg)'"
                    @mouseleave="($event.currentTarget as HTMLElement).style.backgroundColor = 'transparent'"
                >
                  <span
                      class="flex-shrink-0 inline-flex items-center justify-center w-8 text-xs font-mono"
                      style="color: var(--theme-text-secondary);"
                  >
                    {{ chapter.chapterNo }}
                  </span>
                  <span class="flex-1 min-w-0 truncate">{{ chapter.title }}</span>
                  <span
                      v-if="chapter.isFree === false"
                      class="flex-shrink-0 px-1.5 py-0.5 text-[10px] rounded font-medium"
                      style="background-color: var(--theme-accent); color: var(--theme-primary);"
                  >
                    VIP
                  </span>
                </button>
              </li>
            </ul>
            <div v-if="chapters.length > 12" class="mt-4 text-center">
              <button
                  v-if="firstChapter"
                  type="button"
                  class="inline-flex items-center gap-1 px-4 py-2 rounded-lg text-sm font-medium transition-colors focus:outline-none"
                  style="background-color: var(--theme-bg); color: var(--theme-text); border: 1px solid var(--theme-border);"
                  @click="goReadChapter(firstChapter.id)"
              >
                <span>查看全部 {{ chapters.length }} 章</span>
                <ChevronRight class="w-4 h-4" aria-hidden="true" />
              </button>
            </div>
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
    <SiteFooter />
  </div>
</template>
