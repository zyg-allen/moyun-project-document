<script setup lang="ts">
import { ref, computed, onMounted, onUnmounted } from 'vue';
import { useRouter } from 'vue-router';
import { useHead } from '@vueuse/head';
import { Quote, Star, User, Calendar, BookOpen } from 'lucide-vue-next';
import Breadcrumb from '@/components/Breadcrumb.vue';
import { useToast } from '@/composables/useToast';
import SiteFooter from '@/components/SiteFooter.vue';
import LazyImage from '@/components/LazyImage.vue';
import Empty from '@/components/Empty.vue';
import { generateSeo } from '@/utils/seo';
import { getQuoteList, toggleQuoteLike, checkQuoteLike } from '@/api/reading';
import type { BookQuote } from '@/types/api';
import { useUserStore } from '@/stores/user';
import { formatDate } from '@/utils/date';

const router = useRouter();
const toast = useToast();
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
/** 列表加载失败提示（清单 P2）：与"暂无金句摘录"空态区分 */
const loadError = ref<string | null>(null);
/** 点赞在途标记（清单 P2）：按 id 去重，避免连点发出两次 toggle */
const likePending = ref<Record<string, boolean>>({});

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
  loadError.value = null;
  try {
    // 清单 P2：getQuoteList 声明并透传 sort，但后端 Mapper 固定 ORDER BY like_count DESC
    //（后端注释自认排序未实现）。本页也不传 sort，避免"有排序开关却不生效"的误导；
    //「热门/最新」待后端支持后再开放。
    const resp = await getQuoteList({ pageNum: page.value, pageSize });
    if (resp.code !== 200) {
      // 清单 P2：原先没有 else 分支，业务失败与"确实没有数据"都渲染成"暂无金句摘录"
      loadError.value = resp.message || '加载金句失败';
      return;
    }
    if (resp.code === 200 && resp.data) {
      const list = resp.data.list || [];
      // 清单 P2：后端按 like_count DESC 排序，点赞会实时改变顺序；用 offset 分页时
      // 翻页期间若有金句点赞数变化，就会出现**跨页重复/漏项**，重复 id 还会触发 v-for key 冲突。
      // 这里追加前按 id 去重（同名 key 冲突即消失）。
      if (reset) {
        quotes.value = list;
      } else {
        const seen = new Set(quotes.value.map((q) => String(q.id)));
        quotes.value = [...quotes.value, ...list.filter((q) => !seen.has(String(q.id)))];
      }
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
  } catch (e) {
    // 清单 P2：原先静默失败，用户只看到"暂无金句摘录"
    loadError.value = (e as { message?: string })?.message || '加载金句失败，请稍后重试';
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
  // 清单 P2：原实现请求期间不禁用按钮、也不做在途去重 —— 连点两次会发两次 toggle
  // （一次点赞一次取消），响应乱序时 UI 与后端状态相反。这里按 id 在途去重。
  if (likePending.value[quote.id]) return;
  likePending.value[quote.id] = true;
  const wasLiked = !!likeMap.value[quote.id];
  likeMap.value[quote.id] = !wasLiked;
  quote.likeCount = (quote.likeCount || 0) + (wasLiked ? -1 : 1);
  const rollback = () => {
    likeMap.value[quote.id] = wasLiked;
    quote.likeCount = (quote.likeCount || 0) + (wasLiked ? 1 : -1);
  };
  try {
    const resp = await toggleQuoteLike(quote.id);
    if (resp.code === 200 && resp.data) {
      likeMap.value[quote.id] = !!resp.data.liked;
      quote.likeCount = resp.data.likeCount || 0;
    } else {
      // 清单 P2：业务失败（code!==200 / data 为空）原先**既不回滚也不报错**
      rollback();
      toast.error(resp.message || '操作失败，请稍后重试');
    }
  } catch (e) {
    rollback();
    toast.error((e as { message?: string })?.message || '操作失败，请稍后重试');
  } finally {
    likePending.value[quote.id] = false;
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
        <!-- 失败态（清单 P2）：必须排在空态之前 -->
        <div v-if="loadError" class="py-12 text-center rounded-2xl" style="background-color: var(--theme-surface); border: 1px solid var(--theme-border);">
          <p class="mb-4" style="color: var(--theme-text);">{{ loadError }}</p>
          <button
            class="px-5 py-2.5 rounded-xl text-sm font-medium"
            style="background-color: var(--theme-primary); color: white;"
            @click="load(true)"
          >重试</button>
        </div>
        <Empty v-else description="暂无金句摘录" />
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
