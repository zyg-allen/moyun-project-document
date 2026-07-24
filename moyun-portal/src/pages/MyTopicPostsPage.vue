<script setup lang="ts">
import { ref, computed, onMounted, watch } from 'vue';
import { useRouter } from 'vue-router';
import { useHead } from '@vueuse/head';
import {
  ArrowLeft, MessageCircle, Heart, MessageSquare,
  ChevronLeft, ChevronRight,
} from 'lucide-vue-next';
import SiteFooter from '@/components/SiteFooter.vue';
import LazyImage from '@/components/LazyImage.vue';
import MarkdownRenderer from '@/components/MarkdownRenderer.vue';
import Empty from '@/components/Empty.vue';
import { generateSeo } from '@/utils/seo';
import { getSafeAvatar } from '@/utils/avatar';
import { formatShortDate, formatRelativeTime } from '@/utils/date';
import { formatNumber } from '@/utils/number';
import { getMyTopicPosts } from '@/api/topic';
import type { TopicPost } from '@/types/api';

const router = useRouter();

const loading = ref(false);
const error = ref<string | null>(null);
const posts = ref<TopicPost[]>([]);
const total = ref(0);
const page = ref(1);
const pageSize = 10;

const totalPages = computed(() => Math.max(1, Math.ceil(total.value / pageSize)));

useHead(computed(() => generateSeo({
  title: '我的观点',
  description: '查看我在话题中发表的观点',
  keywords: ['我的观点', '话题观点', '墨韵'],
  canonicalPath: '/topic/my/posts',
  robots: 'noindex,nofollow',
})));

onMounted(() => {
  loadPosts();
});

watch(page, () => {
  loadPosts();
});

async function loadPosts() {
  loading.value = true;
  error.value = null;
  try {
    const res = await getMyTopicPosts({ pageNum: page.value, pageSize });
    if (res.code === 200 && res.data) {
      posts.value = res.data.list || [];
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

function gotoTopic(post: TopicPost) {
  router.push(`/topic/${post.topicId}`);
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
        <span class="text-sm font-medium" style="color: var(--theme-text);">我的观点</span>
        <span class="text-sm" style="color: transparent;">占位</span>
      </div>
    </div>

    <!-- Hero 区 -->
    <div class="py-6 sm:py-8">
      <div class="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8">
        <div class="relative overflow-hidden rounded-2xl text-white" style="background-image: radial-gradient(circle at 20% 50%, rgba(190, 24, 93, 0.3) 0%, transparent 50%), radial-gradient(circle at 80% 30%, rgba(124, 58, 237, 0.3) 0%, transparent 50%), linear-gradient(135deg, #be185d 0%, #a21caf 50%, #7c3aed 100%);">
          <div class="relative px-6 py-8 sm:px-10 sm:py-10 text-center">
            <div class="inline-flex items-center bg-white/10 backdrop-blur-sm px-4 py-1.5 rounded-full text-sm mb-4">
              <MessageCircle class="w-4 h-4 mr-2" /> 墨韵 · 我的观点
            </div>
            <h1 class="text-3xl md:text-4xl font-bold mb-3">我的观点</h1>
            <p class="text-sm opacity-90">回顾你在话题中发表的所有观点</p>
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
            @click="loadPosts"
            class="px-4 py-2 text-white rounded-lg text-sm transition hover:opacity-90"
            style="background-color: var(--theme-primary);"
          >
            重试
          </button>
        </div>

        <!-- 空状态 -->
        <Empty
          v-else-if="posts.length === 0"
          title="还没有发表过观点"
          description="去话题广场找一个感兴趣的话题，发表你的观点吧"
          size="lg"
        >
          <template #action>
            <button
              @click="router.push('/topics')"
              class="inline-flex items-center px-4 py-2 text-white rounded-lg text-sm transition hover:opacity-90"
              style="background-color: var(--theme-primary);"
            >
              逛话题广场
            </button>
          </template>
        </Empty>

        <!-- 观点列表 -->
        <template v-else>
          <div class="space-y-4 mb-8">
            <div
              v-for="post in posts"
              :key="post.id"
              class="rounded-xl border p-5 transition hover:shadow-md"
              style="background-color: var(--theme-surface); border-color: var(--theme-border);"
            >
              <!-- 头部：楼层 + 时间 -->
              <div class="flex items-center justify-between mb-3">
                <div class="flex items-center">
                  <img
                    :src="getSafeAvatar(post.user?.avatar, String(post.userId))"
                    :alt="post.user?.nickname || '我'"
                    class="w-8 h-8 rounded-full object-cover mr-2 flex-shrink-0"
                    loading="lazy"
                  />
                  <div>
                    <div class="text-sm font-medium" style="color: var(--theme-text);">
                      {{ post.user?.nickname || '我' }}
                    </div>
                    <div class="text-xs" style="color: var(--theme-text-secondary);">
                      {{ formatRelativeTime(post.createdTime) }} · {{ formatShortDate(post.createdTime) }}
                    </div>
                  </div>
                </div>
                <span
                  class="text-xs px-2 py-1 rounded"
                  style="color: var(--theme-text-secondary); background-color: var(--theme-accent);"
                >
                  #{{ post.floor }} 楼
                </span>
              </div>

              <!-- 观点内容（统一用 markdown 模式渲染，支持图片/格式化） -->
              <div
                class="text-sm leading-relaxed mb-3"
                style="color: var(--theme-text);"
              >
                <MarkdownRenderer
                  v-if="post.content"
                  editor-mode="markdown"
                  :content-markdown="post.content"
                />
                <p v-else style="color: var(--theme-text-secondary);">（观点内容为空）</p>
              </div>

              <!-- 图片 -->
              <div
                v-if="post.images && post.images.length > 0"
                class="grid grid-cols-2 sm:grid-cols-3 gap-2 mb-3"
              >
                <LazyImage
                  v-for="(img, idx) in post.images"
                  :key="idx"
                  :src="img"
                  :alt="`图片${idx + 1}`"
                  class="rounded-lg object-cover w-full h-24"
                />
              </div>

              <!-- 统计 + 跳转 -->
              <div
                class="flex items-center justify-between pt-3 border-t text-xs"
                style="border-color: var(--theme-border); color: var(--theme-text-secondary);"
              >
                <div class="flex items-center gap-3">
                  <span class="flex items-center">
                    <Heart class="w-3.5 h-3.5 mr-1" />{{ formatNumber(post.likeCount) }}
                  </span>
                  <span class="flex items-center">
                    <MessageSquare class="w-3.5 h-3.5 mr-1" />{{ formatNumber(post.commentCount) }}
                  </span>
                </div>
                <button
                  @click="gotoTopic(post)"
                  class="transition hover:opacity-80"
                  style="color: var(--theme-primary);"
                >
                  查看话题 →
                </button>
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
            <span class="ml-2 text-xs" style="color: var(--theme-text-secondary);">共 {{ total }} 条观点</span>
          </div>
        </template>
      </div>
    </div>

    <SiteFooter />
  </div>
</template>
