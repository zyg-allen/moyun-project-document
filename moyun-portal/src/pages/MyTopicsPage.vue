<script setup lang="ts">
import { ref, computed, onMounted, watch } from 'vue';
import { useRouter } from 'vue-router';
import { useHead } from '@vueuse/head';
import {
  ArrowLeft, MessageCircle, Eye, Heart, MessageSquare, Plus,
  ChevronLeft, ChevronRight, Pencil, Pin,
} from 'lucide-vue-next';
import SiteFooter from '@/components/SiteFooter.vue';
import LazyImage from '@/components/LazyImage.vue';
import Empty from '@/components/Empty.vue';
import { generateSeo } from '@/utils/seo';
import { getSafeAvatar } from '@/utils/avatar';
import { formatRelativeTime } from '@/utils/date';
import { formatNumber } from '@/utils/number';
import { getMyTopics } from '@/api/topic';
import type { Topic } from '@/types/api';

const router = useRouter();

const loading = ref(false);
const error = ref<string | null>(null);
const topics = ref<Topic[]>([]);
const total = ref(0);
const page = ref(1);
const pageSize = 12;

const totalPages = computed(() => Math.max(1, Math.ceil(total.value / pageSize)));

useHead(computed(() => generateSeo({
  title: '我的话题',
  description: '管理我发起的话题',
  keywords: ['我的话题', '话题管理', '墨韵'],
  canonicalPath: '/topic/my/topics',
  robots: 'noindex,nofollow',
})));

onMounted(() => {
  loadTopics();
});

watch(page, () => {
  loadTopics();
});

async function loadTopics() {
  loading.value = true;
  error.value = null;
  try {
    const res = await getMyTopics({ pageNum: page.value, pageSize });
    if (res.code === 200 && res.data) {
      topics.value = res.data.list || [];
      total.value = res.data.total || 0;
    } else {
      error.value = res.message || '加载失败';
    }
  } catch (err) {
    const e = err as { message?: string };
    error.value = e?.message || '加载失败，请稍后重试';
  } finally {
    loading.value = false;
  }
}

function gotoDetail(t: Topic) {
  router.push(`/topic/${t.id}`);
}

function gotoEdit(t: Topic) {
  router.push(`/topic/edit/${t.id}`);
}

function gotoCreate() {
  router.push('/topic/create');
}

function gotoPage(p: number) {
  if (p < 1 || p > totalPages.value) return;
  page.value = p;
  window.scrollTo({ top: 0, behavior: 'smooth' });
}

function goBack() {
  if (window.history.length > 1) {
    router.back();
  } else {
    router.push('/topics');
  }
}
</script>

<template>
  <div class="min-h-screen flex flex-col" style="background-color: var(--theme-bg);">
    <!-- 顶部返回栏 -->
    <div
      class="border-b sticky top-0 z-30 backdrop-blur-sm"
      style="background-color: var(--theme-surface); border-color: var(--theme-border);"
    >
      <div class="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 py-3 flex items-center justify-between">
        <button
          @click="goBack"
          class="flex items-center text-sm transition hover:opacity-80"
          style="color: var(--theme-text-secondary);"
        >
          <ArrowLeft class="w-4 h-4 mr-1" />
          返回话题广场
        </button>
        <span class="text-sm font-medium" style="color: var(--theme-text);">我的话题</span>
        <button
          @click="gotoCreate"
          class="flex items-center text-sm text-white px-3 py-1.5 rounded-lg transition hover:opacity-90"
          style="background-color: var(--theme-primary);"
        >
          <Plus class="w-4 h-4 mr-1" />
          发起话题
        </button>
      </div>
    </div>

    <!-- Hero 区 -->
    <div class="py-6 sm:py-8">
      <div class="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8">
        <div class="relative overflow-hidden rounded-2xl text-white" style="background-image: radial-gradient(circle at 20% 50%, rgba(190, 24, 93, 0.3) 0%, transparent 50%), radial-gradient(circle at 80% 30%, rgba(124, 58, 237, 0.3) 0%, transparent 50%), linear-gradient(135deg, #be185d 0%, #a21caf 50%, #7c3aed 100%);">
          <div class="relative px-6 py-8 sm:px-10 sm:py-10 text-center">
            <div class="inline-flex items-center bg-white/10 backdrop-blur-sm px-4 py-1.5 rounded-full text-sm mb-4">
              <MessageCircle class="w-4 h-4 mr-2" /> 墨韵 · 我的话题
            </div>
            <h1 class="text-3xl md:text-4xl font-bold mb-3">我的话题</h1>
            <p class="text-sm opacity-90">管理我发起的话题，查看参与讨论情况</p>
          </div>
        </div>
      </div>
    </div>

    <!-- 内容区 -->
    <div class="flex-1 py-8">
      <div class="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8">
        <!-- 加载状态 -->
        <div v-if="loading" class="flex flex-col items-center justify-center py-20">
          <div
            class="animate-spin rounded-full h-12 w-12 border-b-2"
            style="border-color: var(--theme-primary);"
          ></div>
          <p class="mt-4 text-sm" style="color: var(--theme-text-secondary);">加载中...</p>
        </div>

        <!-- 错误状态 -->
        <div
          v-else-if="error"
          class="rounded-xl border p-8 max-w-md mx-auto text-center"
          style="background-color: var(--theme-surface); border-color: var(--theme-border);"
        >
          <p class="mb-4 text-sm" style="color: var(--theme-text);">{{ error }}</p>
          <button
            @click="loadTopics"
            class="px-4 py-2 text-white rounded-lg text-sm transition hover:opacity-90"
            style="background-color: var(--theme-primary);"
          >
            重试
          </button>
        </div>

        <!-- 空状态 -->
        <Empty
          v-else-if="topics.length === 0"
          title="还没有发起话题"
          description="立即发起第一个话题，邀请社区成员参与讨论"
          size="lg"
        >
          <template #action>
            <button
              @click="gotoCreate"
              class="inline-flex items-center px-4 py-2 text-white rounded-lg text-sm transition hover:opacity-90"
              style="background-color: var(--theme-primary);"
            >
              <Plus class="w-4 h-4 mr-1" />
              发起话题
            </button>
          </template>
        </Empty>

        <!-- 话题卡片列表 -->
        <template v-else>
          <div class="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-5 mb-8">
            <div
              v-for="t in topics"
              :key="t.id"
              class="rounded-xl overflow-hidden border shadow-sm hover:shadow-md transition flex flex-col"
              style="background-color: var(--theme-surface); border-color: var(--theme-border);"
            >
              <!-- 封面 -->
              <div
                @click="gotoDetail(t)"
                class="relative h-32 cursor-pointer"
                style="background-color: var(--theme-bg);"
              >
                <LazyImage
                  v-if="t.cover"
                  :src="t.cover"
                  :alt="t.title"
                  class="w-full h-full object-cover"
                />
                <div
                  v-else
                  class="w-full h-full flex items-center justify-center"
                  style="background: linear-gradient(135deg, var(--theme-accent), color-mix(in srgb, var(--theme-accent) 50%, #c4b5fd));"
                >
                  <MessageCircle class="w-10 h-10" style="color: var(--theme-primary); opacity: 0.6;" />
                </div>
                <span
                  v-if="t.pinned === 1"
                  class="absolute top-3 left-3 inline-flex items-center px-2 py-0.5 rounded text-xs font-medium text-white"
                  style="background-color: #f59e0b;"
                >
                  <Pin class="w-3 h-3 mr-1" />置顶
                </span>
              </div>

              <div class="p-5 flex flex-col flex-1">
                <h3
                  @click="gotoDetail(t)"
                  class="text-base font-semibold mb-2 line-clamp-1 cursor-pointer hover:underline"
                  style="color: var(--theme-text);"
                >
                  {{ t.title }}
                </h3>
                <p
                  v-if="t.description"
                  class="text-xs mb-3 line-clamp-2"
                  style="color: var(--theme-text-secondary);"
                >
                  {{ t.description }}
                </p>
                <div class="flex-1"></div>

                <!-- 发起人 -->
                <div class="flex items-center mb-3">
                  <img
                    :src="getSafeAvatar(t.creator?.avatar, String(t.creatorId))"
                    :alt="t.creator?.nickname || '发起人'"
                    class="w-5 h-5 rounded-full object-cover mr-2 flex-shrink-0"
                    loading="lazy"
                  />
                  <span class="text-xs truncate flex-1" style="color: var(--theme-text);">
                    {{ t.creator?.nickname || '匿名用户' }}
                  </span>
                  <span
                    v-if="t.lastPostTime"
                    class="text-xs flex items-center flex-shrink-0"
                    style="color: var(--theme-text-secondary);"
                  >
                    {{ formatRelativeTime(t.lastPostTime) }}
                  </span>
                </div>

                <!-- 统计 -->
                <div
                  class="flex items-center justify-between pt-3 border-t text-xs mb-3"
                  style="border-color: var(--theme-border); color: var(--theme-text-secondary);"
                >
                  <span class="flex items-center">
                    <Eye class="w-3 h-3 mr-0.5" />{{ formatNumber(t.viewCount) }}
                  </span>
                  <span class="flex items-center">
                    <MessageSquare class="w-3 h-3 mr-0.5" />{{ formatNumber(t.postCount) }}
                  </span>
                  <span class="flex items-center">
                    <Heart class="w-3 h-3 mr-0.5" />{{ formatNumber(t.likeCount) }}
                  </span>
                </div>

                <!-- 操作 -->
                <div class="flex items-center gap-1.5">
                  <button
                    @click="gotoDetail(t)"
                    class="inline-flex items-center px-2.5 py-1.5 rounded-lg text-xs transition hover:opacity-80 flex-1 justify-center"
                    style="background-color: var(--theme-bg); color: var(--theme-text); border: 1px solid var(--theme-border);"
                  >
                    查看
                  </button>
                  <button
                    @click="gotoEdit(t)"
                    class="inline-flex items-center px-2.5 py-1.5 rounded-lg text-xs transition hover:opacity-80"
                    style="background-color: var(--theme-bg); color: var(--theme-primary); border: 1px solid var(--theme-border);"
                  >
                    <Pencil class="w-3 h-3 mr-1" />编辑
                  </button>
                </div>
              </div>
            </div>
          </div>

          <!-- 分页 -->
          <div v-if="totalPages > 1" class="flex flex-wrap items-center justify-center gap-2 mt-8">
            <button
              @click="gotoPage(page - 1)"
              :disabled="page === 1"
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
              class="px-3 py-2 rounded-lg text-sm transition disabled:opacity-40 disabled:cursor-not-allowed flex items-center"
              style="background-color: var(--theme-surface); border: 1px solid var(--theme-border); color: var(--theme-text);"
            >
              下一页
              <ChevronRight class="w-4 h-4" />
            </button>
            <span class="ml-2 text-xs" style="color: var(--theme-text-secondary);">共 {{ total }} 个话题</span>
          </div>
        </template>
      </div>
    </div>

    <SiteFooter />
  </div>
</template>
