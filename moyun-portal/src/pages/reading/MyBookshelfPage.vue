<script setup lang="ts">
import { ref, computed, onMounted } from 'vue';
import { useRouter } from 'vue-router';
import { useHead } from '@vueuse/head';
import { BookOpen, Trash2, RefreshCw } from 'lucide-vue-next';
import LazyImage from '@/components/LazyImage.vue';
import Breadcrumb from '@/components/Breadcrumb.vue';
import SiteFooter from '@/components/SiteFooter.vue';
import Pagination from '@/components/Pagination.vue';
import Empty from '@/components/Empty.vue';
import { generateSeo } from '@/utils/seo';
import { useConfirmModal } from '@/composables/useConfirmModal';
import { getMyBookshelf, removeFromBookshelf } from '@/api/reading';
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
    description: '旭林知行读书空间 - 我的书架，收藏的好书都在这里',
    type: 'article',
    canonicalPath: '/reading/bookshelf',
    // 清单 P2：私有页必须显式输出 robots —— 路由 meta 里的 robots 全站**无消费方**
    //（守卫只读 requiresAuth/title），只有 generateSeo 的 robots 参数才会真正渲染。
    robots: 'noindex,nofollow',
  }))
);

// 面包屑
const breadcrumbs = computed(() => [
  { label: '读书空间', path: '/reading' },
  { label: '我的书架' },
]);

/**
 * 加载书架。
 *
 * <p>清单 P2 两处修复：</p>
 * <ol>
 *   <li><b>消除 N+1 与"虚增阅读量"</b>：原先对当前页每条记录再调一次
 *       `GET /portal/reading/books/{id}` —— 既造成 N+1，又会**虚增每本书的阅读量**
 *       （该接口内部 incrementReadingCount 并落库）。后端现已随列表批量下发
 *       `bookTitle/bookCover/bookAuthor`，前端直接使用。</li>
 *   <li><b>区分失败态与空态</b>：原先 catch 里把列表与 total 一起清零，
 *       失败会被渲染成「书架空空如也」，用户以为收藏全丢了。</li>
 * </ol>
 */
/** 加载失败提示（清单 P2：失败不再冒充"书架空空如也"） */
const loadError = ref<string | null>(null);
const confirmModal = useConfirmModal();

async function loadBookshelf() {
  loading.value = true;
  loadError.value = null;
  try {
    const resp = await getMyBookshelf({
      pageNum: pageNum.value,
      pageSize: pageSize.value,
      orderBy: orderBy.value,
    });
    if (resp.code === 200 && resp.data) {
      const items = resp.data.records || [];
      total.value = resp.data.total || 0;
      bookshelfList.value = items.map((item) => ({
        ...item,
        book: {
          ...(item as unknown as { book?: Book }).book,
          title: item.bookTitle || (item as unknown as { book?: Book }).book?.title,
          cover: item.bookCover || (item as unknown as { book?: Book }).book?.cover,
          author: item.bookAuthor || (item as unknown as { book?: Book }).book?.author,
        } as Book,
      }));
      // 移出后若当前页已被清空（末页最后一条），回退一页（清单 P2）
      if (bookshelfList.value.length === 0 && pageNum.value > 1 && total.value > 0) {
        pageNum.value -= 1;
        await loadBookshelf();
        return;
      }
    } else {
      loadError.value = resp.message || '加载书架失败';
      bookshelfList.value = [];
      total.value = 0;
    }
  } catch (err) {
    console.error('加载书架失败:', err);
    loadError.value = (err as Error)?.message || '加载书架失败，请稍后重试';
    bookshelfList.value = [];
    total.value = 0;
  } finally {
    loading.value = false;
  }
}

async function handleRemove(item: BookshelfItem & { book?: Book }) {
  // 清单 P2/P3：统一使用全局确认弹窗（与 ColumnDetailPage/MyResumesPage 等处一致），
  // 不再用原生 window.confirm（样式与无障碍不一致）。
  const ok = await confirmModal.confirm(`确认将《${item.book?.title || '未知书籍'}》移出书架？`, {
    title: '移出书架',
    confirmText: '确认移出',
    danger: true,
  });
  if (!ok) return;
  try {
    const resp = await removeFromBookshelf(item.bookId);
    if (resp.code === 200) {
      toast.success('已移出书架');
      // 清单 P2：删掉末页最后一条时 pageNum 仍指向越界页 ⇒ 后端返回空页并被当成"书架为空"。
      // 这里先按"本页可能已空"回退一页，再重新加载（loadBookshelf 内还有兜底校正）。
      if (pageNum.value > 1 && bookshelfList.value.length <= 1) {
        pageNum.value -= 1;
      }
      loadBookshelf();
    }
  } catch (err) {
    toast.error((err as Error)?.message || '操作失败，请稍后重试');
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

onMounted(() => {
  // 需要登录
  if (!requireAuth('/reading/bookshelf')) return;
  loadBookshelf();
});
</script>

<template>
  <div class="min-h-screen flex flex-col" style="background-color: var(--theme-bg);">
    <!-- 吸顶面包屑栏 -->
    <div class="border-b sticky top-0 z-30 backdrop-blur-sm py-3" style="background-color: var(--theme-surface); border-color: var(--theme-border);">
      <div class="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 flex items-center justify-between gap-4">
        <Breadcrumb :items="breadcrumbs" />
        <button
          @click="loadBookshelf"
          class="inline-flex items-center gap-1.5 px-3 py-1.5 rounded-lg text-sm font-medium transition-colors hover:opacity-80 flex-shrink-0"
          style="background-color: var(--theme-bg); color: var(--theme-text);"
        >
          <RefreshCw class="w-4 h-4" :class="loading ? 'animate-spin' : ''" />
          <span>刷新</span>
        </button>
      </div>
    </div>

    <!-- 内容 -->
    <div class="flex-1 py-6">
      <div class="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8">
        <!-- 工具栏：数量 + 排序 -->
        <div class="flex items-center justify-between flex-wrap gap-3 mb-6">
          <span class="text-sm" style="color: var(--theme-text-secondary);">共 {{ total }} 本</span>
          <div class="flex gap-2">
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
        <!-- 加载中 -->
        <div v-if="loading && bookshelfList.length === 0" class="text-center py-16">
          <div
            class="inline-block w-12 h-12 border-4 border-t-4 rounded-full animate-spin"
            style="border-color: var(--theme-border); border-top-color: var(--theme-primary);"
          ></div>
          <p class="mt-4" style="color: var(--theme-text-secondary);">加载中...</p>
        </div>

        <!-- 空状态 -->
        <div
          v-if="loadError && !loading"
          class="py-16 text-center rounded-2xl mb-6"
          style="background-color: var(--theme-surface); border: 1px solid var(--theme-border);"
        >
          <p class="mb-4" style="color: var(--theme-text);">{{ loadError }}</p>
          <button
            class="px-5 py-2 rounded-xl text-sm font-medium text-white"
            style="background-color: var(--theme-primary);"
            @click="loadBookshelf()"
          >重试</button>
        </div>

        <!--
          清单 P2：原写法 `<Empty action-text="去发现" @action="..." />` 属**组件 API 误用** ——
          Empty.vue 只声明 title/description/size 三个 props，动作区是名为 action 的**插槽**，
          既无 actionText 属性也无 action 事件 ⇒ CTA 永远不会渲染、点击也永远不触发。
        -->
        <Empty
          v-else-if="bookshelfList.length === 0 && !loadError"
          title="书架空空如也"
          description="去读书空间发现更多好书吧"
        >
          <!-- 清单 P2：动作区必须用 Empty 的 action **插槽**（组件没有 actionText/action 事件） -->
          <template #action>
            <button
              class="px-5 py-2 rounded-xl text-sm font-medium text-white"
              style="background-color: var(--theme-primary);"
              @click="router.push('/reading')"
            >去发现</button>
          </template>
        </Empty>

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
