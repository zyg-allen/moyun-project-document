<script setup lang="ts">
import { ref, computed, onMounted } from 'vue';
import { useRouter } from 'vue-router';
import { useHead } from '@vueuse/head';
import { BookOpen, ArrowLeft, Trash2, BookMarked, RefreshCw } from 'lucide-vue-next';
import LazyImage from '@/components/LazyImage.vue';
import SiteFooter from '@/components/SiteFooter.vue';
import Pagination from '@/components/Pagination.vue';
import Empty from '@/components/Empty.vue';
import { generateSeo } from '@/utils/seo';
import { getMyBookshelf, removeFromBookshelf, getBookDetail } from '@/api/reading';
import { useAuth } from '@/composables/useAuth';
import { useToast } from '@/composables/useToast';
import type { BookshelfItem, Book } from '@/types/api';

const router = useRouter();
const { requireAuth } = useAuth();
const toast = useToast();

const loading = ref(false);
const bookshelfList = ref<(BookshelfItem & { book?: Book })[]>([]);
const total = ref(0);
const pageNum = ref(1);
const pageSize = ref(12);
const orderBy = ref<'latest' | 'recent' | 'sort'>('latest');

// SEO
useHead(
  computed(() => generateSeo({
    title: '我的书架',
    description: '墨韵·智库读书空间 - 我的书架，收藏的好书都在这里',
    type: 'article',
    canonicalPath: '/reading/bookshelf',
  }))
);

async function loadBookshelf() {
  loading.value = true;
  try {
    const resp = await getMyBookshelf({
      pageNum: pageNum.value,
      pageSize: pageSize.value,
      orderBy: orderBy.value,
    });
    if (resp.code === 200 && resp.data) {
      const items = resp.data.records || [];
      total.value = resp.data.total || 0;
      // 批量加载书籍详情（简化方案：逐个加载，缓存避免重复）
      bookshelfList.value = await Promise.all(items.map(async (item) => {
        try {
          const bookResp = await getBookDetail(item.bookId);
          if (bookResp.code === 200 && bookResp.data) {
            return { ...item, book: bookResp.data.book };
          }
        } catch (err) {
          // 静默忽略
        }
        return { ...item, book: undefined };
      }));
    } else {
      bookshelfList.value = [];
      total.value = 0;
    }
  } catch (err) {
    console.error('加载书架失败:', err);
    bookshelfList.value = [];
    total.value = 0;
  } finally {
    loading.value = false;
  }
}

async function handleRemove(item: BookshelfItem & { book?: Book }) {
  if (!confirm(`确认将《${item.book?.title || '未知书籍'}》移出书架？`)) return;
  try {
    const resp = await removeFromBookshelf(item.bookId);
    if (resp.code === 200) {
      toast.success('已移出书架');
      // 重新加载当前页
      loadBookshelf();
    }
  } catch (err) {
    toast.error('操作失败，请稍后重试');
  }
}

function goReadBook(item: BookshelfItem & { book?: Book }) {
  if (item.lastChapterId) {
    // 续读：跳到上次阅读的章节
    router.push(`/reading/book/${item.bookId}/chapter/${item.lastChapterId}`);
  } else {
    // 跳到书籍详情页
    router.push(`/reading/book/${item.bookId}`);
  }
}

function goBookDetail(bookId: string) {
  router.push(`/reading/book/${bookId}`);
}

function changeOrder(o: 'latest' | 'recent' | 'sort') {
  orderBy.value = o;
  pageNum.value = 1;
  loadBookshelf();
}

function handlePageChange(page: number) {
  pageNum.value = page;
  loadBookshelf();
  // 滚动到顶部
  window.scrollTo({ top: 0, behavior: 'smooth' });
}

function goBack() {
  router.push('/reading');
}

onMounted(() => {
  // 需要登录
  if (!requireAuth('/reading/bookshelf')) return;
  loadBookshelf();
});
</script>

<template>
  <div class="min-h-screen flex flex-col" style="background-color: var(--theme-bg);">
    <!-- 顶部 -->
    <div class="border-b py-4" style="background-color: var(--theme-bg); border-color: var(--theme-border);">
      <div class="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8">
        <div class="flex items-center justify-between">
          <button
            @click="goBack"
            class="inline-flex items-center gap-2 text-sm font-medium transition-colors hover:opacity-80"
            style="color: var(--theme-text-secondary);"
          >
            <ArrowLeft class="w-4 h-4" />
            <span>返回读书空间</span>
          </button>
          <button
            @click="loadBookshelf"
            class="inline-flex items-center gap-1.5 px-3 py-1.5 rounded-lg text-sm font-medium transition-colors hover:opacity-80"
            style="background-color: var(--theme-surface); color: var(--theme-text);"
          >
            <RefreshCw class="w-4 h-4" :class="loading ? 'animate-spin' : ''" />
            <span>刷新</span>
          </button>
        </div>
        <div class="mt-3 flex items-center gap-2">
          <BookMarked class="w-6 h-6" style="color: var(--theme-primary);" />
          <h1 class="text-2xl font-bold" style="color: var(--theme-text);">我的书架</h1>
          <span class="text-sm ml-2" style="color: var(--theme-text-secondary);">共 {{ total }} 本</span>
        </div>

        <!-- 排序选项 -->
        <div class="mt-4 flex gap-2">
          <button
            v-for="o in [
              { value: 'latest', label: '按收藏时间' },
              { value: 'recent', label: '按最近阅读' },
            ]"
            :key="o.value"
            @click="changeOrder(o.value as 'latest' | 'recent')"
            class="px-3 py-1.5 rounded-lg text-xs font-medium transition-all"
            :style="orderBy === o.value
              ? { backgroundColor: 'var(--theme-primary)', color: '#ffffff' }
              : { backgroundColor: 'var(--theme-surface)', color: 'var(--theme-text)', border: '1px solid var(--theme-border)' }"
          >{{ o.label }}</button>
        </div>
      </div>
    </div>

    <!-- 内容 -->
    <div class="flex-1 py-6">
      <div class="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8">
        <!-- 加载中 -->
        <div v-if="loading && bookshelfList.length === 0" class="text-center py-16">
          <div
            class="inline-block w-12 h-12 border-4 border-t-4 rounded-full animate-spin"
            style="border-color: var(--theme-border); border-top-color: var(--theme-primary);"
          ></div>
          <p class="mt-4" style="color: var(--theme-text-secondary);">加载中...</p>
        </div>

        <!-- 空状态 -->
        <Empty
          v-else-if="bookshelfList.length === 0"
          title="书架空空如也"
          description="去读书空间发现更多好书吧"
          action-text="去发现"
          @action="router.push('/reading')"
        />

        <!-- 书架网格 -->
        <div v-else class="grid grid-cols-2 sm:grid-cols-3 md:grid-cols-4 lg:grid-cols-6 gap-4">
          <div
            v-for="item in bookshelfList"
            :key="item.id"
            class="group relative rounded-xl overflow-hidden shadow-sm transition-all hover:shadow-lg"
            style="background-color: var(--theme-surface);"
          >
            <!-- 封面 -->
            <div
              class="aspect-[3/4] cursor-pointer overflow-hidden"
              @click="goBookDetail(item.bookId)"
            >
              <LazyImage
                v-if="item.book?.cover"
                :src="item.book.cover"
                :alt="item.book.title"
                class="w-full h-full transition-transform group-hover:scale-105"
              />
              <div
                v-else
                class="w-full h-full flex items-center justify-center"
                style="background-color: var(--theme-border);"
              >
                <BookOpen class="w-10 h-10" style="color: var(--theme-text-secondary);" />
              </div>
            </div>

            <!-- 信息 -->
            <div class="p-3">
              <h3
                class="text-sm font-medium truncate cursor-pointer"
                style="color: var(--theme-text);"
                :title="item.book?.title"
                @click="goBookDetail(item.bookId)"
              >{{ item.book?.title || '未知书籍' }}</h3>
              <p class="text-xs mt-1 truncate" style="color: var(--theme-text-secondary);">
                {{ item.book?.author || '未知作者' }}
              </p>
              <p v-if="item.lastChapterNo" class="text-xs mt-1" style="color: var(--theme-text-secondary);">
                读到第 {{ item.lastChapterNo }} 章
              </p>
            </div>

            <!-- 操作按钮 -->
            <div class="px-3 pb-3 flex gap-1.5">
              <button
                type="button"
                class="flex-1 inline-flex items-center justify-center gap-1 px-2 py-1.5 rounded-lg text-xs font-medium transition-all"
                style="background-color: var(--theme-primary); color: #ffffff;"
                @click="goReadBook(item)"
              >
                <BookOpen class="w-3 h-3" />
                <span>{{ item.lastChapterId ? '续读' : '开始' }}</span>
              </button>
              <button
                type="button"
                class="inline-flex items-center justify-center p-1.5 rounded-lg transition-all hover:opacity-80"
                style="background-color: var(--theme-border); color: var(--theme-text-secondary);"
                @click="handleRemove(item)"
                aria-label="移出书架"
              >
                <Trash2 class="w-3.5 h-3.5" />
              </button>
            </div>
          </div>
        </div>

        <!-- 分页 -->
        <div v-if="total > pageSize" class="mt-8 flex justify-center">
          <Pagination
            :current-page="pageNum"
            :items-per-page="pageSize"
            :total-items="total"
            :total-pages="Math.ceil(total / pageSize)"
            @page-change="handlePageChange"
          />
        </div>
      </div>
    </div>

    <SiteFooter />
  </div>
</template>
