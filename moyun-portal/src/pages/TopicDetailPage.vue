<script setup lang="ts">
import { ref, computed, reactive, onMounted, watch } from 'vue';
import { useRoute, useRouter } from 'vue-router';
import { useHead } from '@vueuse/head';
import {
  MessageCircle, Eye, Heart, MessageSquare, Send,
  Pin, ChevronLeft, ChevronRight, Trash2, Loader2, Pencil, BadgeCheck,
  ChevronDown, Reply, X,
} from 'lucide-vue-next';
import Breadcrumb from '@/components/Breadcrumb.vue';
import SiteFooter from '@/components/SiteFooter.vue';
import LazyImage from '@/components/LazyImage.vue';
import MarkdownEditor from '@/components/MarkdownEditor.vue';
import MarkdownRenderer from '@/components/MarkdownRenderer.vue';
import Empty from '@/components/Empty.vue';
import { generateSeo } from '@/utils/seo';
import { getSafeAvatar } from '@/utils/avatar';
import { formatShortDate, formatRelativeTime } from '@/utils/date';
import { formatNumber } from '@/utils/number';
import {
  getTopicDetail, toggleTopicLike,
  getTopicPosts, createTopicPost, deleteTopicPost, toggleTopicPostLike,
  getTopicComments, createTopicComment, deleteTopicComment, toggleTopicCommentLike,
} from '@/api/topic';
import { useUserStore } from '@/stores/user';
import { useAuth } from '@/composables/useAuth';
import { useToast } from '@/composables/useToast';
import type { Topic, TopicPost, TopicComment } from '@/types/api';

const route = useRoute();
const router = useRouter();
const userStore = useUserStore();
const { requireAuth } = useAuth();
const toast = useToast();

const topicId = computed(() => route.params.id as string);

const loading = ref(false);
const error = ref<string | null>(null);
const topic = ref<Topic | null>(null);

// 观点列表
const postsLoading = ref(false);
const posts = ref<TopicPost[]>([]);
const postsTotal = ref(0);
const postsPage = ref(1);
const postsPageSize = 10;
const postsTotalPages = computed(() => Math.max(1, Math.ceil(postsTotal.value / postsPageSize)));

// 发表观点
const newPostContent = ref('');
const submitting = ref(false);
const actionPostId = ref<number | null>(null);
// 编辑器折叠 + 模式切换（plain=纯文本 / richtext=富文本 Markdown）
const showPostEditor = ref(false);
const postEditorMode = ref<'plain' | 'richtext'>('plain');

function togglePostEditor() {
  showPostEditor.value = !showPostEditor.value;
  if (!showPostEditor.value) {
    // 折叠时清空内容
    newPostContent.value = '';
  }
}

function switchPostEditorMode(mode: 'plain' | 'richtext') {
  postEditorMode.value = mode;
}

// 话题点赞中
const likingTopic = ref(false);

const isOwner = computed(() => {
  if (!topic.value || !userStore.user) return false;
  return String(topic.value.creatorId) === String(userStore.user.id);
});

useHead(computed(() => generateSeo({
  title: topic.value?.title ? `${topic.value.title} - 话题详情` : '话题详情',
  description: topic.value?.description || '墨韵话题详情，参与话题讨论，发表你的观点',
  keywords: ['话题', '讨论', '观点', topic.value?.title].filter(Boolean) as string[],
  canonicalPath: `/topic/${topicId.value}`,
})));

// 面包屑
const breadcrumbs = computed(() => [
  { label: '话题广场', path: '/topics' },
  { label: topic.value?.title || '话题详情' },
]);

onMounted(() => {
  loadAll();
});

watch(topicId, () => {
  loadAll();
});

watch(postsPage, () => {
  loadPosts();
});

async function loadAll() {
  await Promise.all([loadTopic(), loadPosts()]);
  // 加载话题讨论区评论
  if (topic.value) {
    loadComments('topic', topic.value.id);
  }
}

async function loadTopic() {
  loading.value = true;
  error.value = null;
  try {
    const res = await getTopicDetail(topicId.value);
    if (res.code === 200 && res.data) {
      topic.value = res.data;
    } else {
      error.value = res.message || '话题不存在或已被删除';
    }
  } catch (err) {
    const e = err as { message?: string };
    error.value = e?.message || '加载失败，请稍后重试';
  } finally {
    loading.value = false;
  }
}

async function loadPosts() {
  postsLoading.value = true;
  try {
    const res = await getTopicPosts(topicId.value, {
      pageNum: postsPage.value,
      pageSize: postsPageSize,
    });
    if (res.code === 200 && res.data) {
      posts.value = res.data.list || [];
      postsTotal.value = res.data.total || 0;
    } else {
      posts.value = [];
      postsTotal.value = 0;
    }
  } catch (err) {
    console.error('加载观点失败:', err);
    posts.value = [];
    postsTotal.value = 0;
  } finally {
    postsLoading.value = false;
  }
}

async function handleToggleTopicLike() {
  if (!topic.value) return;
  if (!requireAuth(route.fullPath)) return;
  if (likingTopic.value) return;
  likingTopic.value = true;
  try {
    const res = await toggleTopicLike(topic.value.id);
    if (res.code === 200 && res.data) {
      topic.value.isLiked = res.data.isLiked;
      topic.value.likeCount = res.data.likeCount;
    }
  } catch (err) {
    const e = err as { message?: string };
    toast.error(e?.message || '操作失败');
  } finally {
    likingTopic.value = false;
  }
}

async function handleSubmitPost() {
  if (!topic.value) return;
  if (!requireAuth(route.fullPath)) return;
  const content = newPostContent.value.trim();
  if (!content) {
    toast.warning('请输入观点内容');
    return;
  }
  submitting.value = true;
  try {
    const res = await createTopicPost(topic.value.id, { content });
    if (res.code === 200 && res.data) {
      newPostContent.value = '';
      // 提交成功后折叠编辑器，下次需重新点击展开
      showPostEditor.value = false;
      toast.success('观点已发表');
      // 关键修复：先更新总数，再计算最后一页
      postsTotal.value += 1;
      const newLastPage = Math.ceil(postsTotal.value / postsPageSize);
      const oldPage = postsPage.value;
      postsPage.value = newLastPage;
      // 如果页码没变（比如当前就在最后一页），手动加载；否则 watch(postsPage) 会自动触发 loadPosts
      if (oldPage === newLastPage) {
        await loadPosts();
      }
      // 话题统计 +1
      topic.value.postCount = (topic.value.postCount || 0) + 1;
    } else {
      toast.error(res.message || '发表失败');
    }
  } catch (err) {
    const e = err as { message?: string };
    toast.error(e?.message || '发表失败');
  } finally {
    submitting.value = false;
  }
}

async function handleTogglePostLike(post: TopicPost) {
  if (!requireAuth(route.fullPath)) return;
  if (actionPostId.value) return;
  actionPostId.value = post.id;
  try {
    const res = await toggleTopicPostLike(post.id);
    if (res.code === 200 && res.data) {
      post.isLiked = res.data.isLiked;
      post.likeCount = res.data.likeCount;
    }
  } catch (err) {
    const e = err as { message?: string };
    toast.error(e?.message || '操作失败');
  } finally {
    actionPostId.value = null;
  }
}

async function handleDeletePost(post: TopicPost) {
  if (!requireAuth(route.fullPath)) return;
  if (!window.confirm('确定删除这条观点吗？删除后不可恢复。')) return;
  if (actionPostId.value) return;
  actionPostId.value = post.id;
  try {
    const res = await deleteTopicPost(post.id);
    if (res.code === 200) {
      toast.success('删除成功');
      posts.value = posts.value.filter(p => p.id !== post.id);
      postsTotal.value = Math.max(0, postsTotal.value - 1);
      if (topic.value) {
        topic.value.postCount = Math.max(0, (topic.value.postCount || 0) - 1);
      }
      // 修复：如果当前页已空且不是第 1 页，回退到上一页（watch 会自动触发 loadPosts）
      if (posts.value.length === 0 && postsPage.value > 1) {
        postsPage.value -= 1;
      }
    } else {
      toast.error(res.message || '删除失败');
    }
  } catch (err) {
    const e = err as { message?: string };
    toast.error(e?.message || '删除失败');
  } finally {
    actionPostId.value = null;
  }
}

function gotoEdit() {
  if (!topic.value) return;
  router.push(`/topic/edit/${topic.value.id}`);
}

function gotoPostsPage(p: number) {
  if (p < 1 || p > postsTotalPages.value) return;
  postsPage.value = p;
  // 平滑滚动到观点区
  const el = document.getElementById('topic-posts');
  if (el) {
    el.scrollIntoView({ behavior: 'smooth', block: 'start' });
  }
}

// ==================== 评论功能 ====================

interface CommentState {
  list: TopicComment[];
  loading: boolean;
  total: number;
  expanded: boolean;
  newContent: string;
  replyingRoot: TopicComment | null;
  replyingTo: TopicComment | null;
  replyContent: string;
  actioningId: number | null;
}

const commentStates = reactive(new Map<string, CommentState>());

function commentKey(targetType: string, targetId: number | string): string {
  return `${targetType}_${targetId}`;
}

function getCommentState(targetType: string, targetId: number | string): CommentState {
  const key = commentKey(targetType, targetId);
  if (!commentStates.has(key)) {
    commentStates.set(key, {
      list: [],
      loading: false,
      total: 0,
      expanded: false,
      newContent: '',
      replyingRoot: null,
      replyingTo: null,
      replyContent: '',
      actioningId: null,
    });
  }
  return commentStates.get(key)!;
}

function postCommentState(post: TopicPost): CommentState {
  return getCommentState('post', post.id);
}

function topicCommentState(): CommentState {
  return getCommentState('topic', topic.value?.id || 0);
}

function canDeleteComment(comment: TopicComment): boolean {
  const userId = userStore.user?.id;
  if (!userId) return false;
  if (String(comment.authorId) === String(userId)) return true;
  return userStore.user?.role === 'admin';
}

function getCommentAuthorName(comment: TopicComment): string {
  return comment.author?.nickname || '匿名用户';
}

function getCommentAuthorAvatar(comment: TopicComment): string {
  return getSafeAvatar(comment.author?.avatar, String(comment.authorId));
}

function getReplyToName(comment: TopicComment): string {
  return comment.replyToUser?.nickname || '';
}

async function loadComments(targetType: string, targetId: number | string) {
  const state = getCommentState(targetType, targetId);
  state.loading = true;
  try {
    const res = await getTopicComments({
      targetType,
      targetId,
      pageNum: 1,
      pageSize: 10,
    });
    if (res.code === 200 && res.data) {
      state.list = (res.data.list || []).map(c => {
        if (!c.replies) c.replies = [];
        return c;
      });
      state.total = res.data.total || 0;
    } else {
      state.list = [];
      state.total = 0;
    }
  } catch (err) {
    console.error('加载评论失败:', err);
    state.list = [];
    state.total = 0;
  } finally {
    state.loading = false;
  }
}

function toggleComments(targetType: string, targetId: number | string) {
  const state = getCommentState(targetType, targetId);
  state.expanded = !state.expanded;
  if (state.expanded && state.list.length === 0 && !state.loading) {
    loadComments(targetType, targetId);
  }
}

async function handleSubmitComment(targetType: string, targetId: number | string) {
  if (!requireAuth(route.fullPath)) return;
  const state = getCommentState(targetType, targetId);
  const content = state.newContent.trim();
  if (!content) {
    toast.warning('请输入评论内容');
    return;
  }
  try {
    const res = await createTopicComment({ targetType, targetId, content });
    if (res.code === 200) {
      state.newContent = '';
      toast.success('评论成功');
      await loadComments(targetType, targetId);
      if (targetType === 'post') {
        const post = posts.value.find(p => p.id === targetId);
        if (post) post.commentCount = (post.commentCount || 0) + 1;
      } else if (targetType === 'topic' && topic.value) {
        topic.value.commentCount = (topic.value.commentCount || 0) + 1;
      }
    } else {
      toast.error(res.message || '评论失败');
    }
  } catch (err) {
    const e = err as { message?: string };
    toast.error(e?.message || '评论失败');
  }
}

function startReply(
  targetType: string,
  targetId: number | string,
  rootComment: TopicComment,
  replyComment?: TopicComment,
) {
  if (!requireAuth(route.fullPath)) return;
  const state = getCommentState(targetType, targetId);
  state.replyingRoot = rootComment;
  state.replyingTo = replyComment || rootComment;
  state.replyContent = '';
}

function cancelReply(targetType: string, targetId: number | string) {
  const state = getCommentState(targetType, targetId);
  state.replyingRoot = null;
  state.replyingTo = null;
  state.replyContent = '';
}

async function handleSubmitReply(targetType: string, targetId: number | string) {
  if (!requireAuth(route.fullPath)) return;
  const state = getCommentState(targetType, targetId);
  const content = state.replyContent.trim();
  if (!content) {
    toast.warning('请输入回复内容');
    return;
  }
  if (!state.replyingRoot) return;
  try {
    const res = await createTopicComment({
      targetType,
      targetId,
      content,
      parentId: state.replyingRoot.id,
      replyTo: state.replyingTo?.id,
    });
    if (res.code === 200) {
      state.replyContent = '';
      state.replyingRoot = null;
      state.replyingTo = null;
      toast.success('回复成功');
      await loadComments(targetType, targetId);
    } else {
      toast.error(res.message || '回复失败');
    }
  } catch (err) {
    const e = err as { message?: string };
    toast.error(e?.message || '回复失败');
  }
}

async function handleLikeComment(
  targetType: string,
  targetId: number | string,
  comment: TopicComment,
) {
  if (!requireAuth(route.fullPath)) return;
  const state = getCommentState(targetType, targetId);
  if (state.actioningId) return;
  state.actioningId = comment.id;
  try {
    const res = await toggleTopicCommentLike(comment.id);
    if (res.code === 200 && res.data) {
      comment.isLiked = res.data.isLiked;
      comment.likeCount = res.data.likeCount;
    }
  } catch (err) {
    const e = err as { message?: string };
    toast.error(e?.message || '操作失败');
  } finally {
    state.actioningId = null;
  }
}

async function handleDeleteComment(
  targetType: string,
  targetId: number | string,
  comment: TopicComment,
) {
  if (!requireAuth(route.fullPath)) return;
  if (!window.confirm('确认删除该评论？')) return;
  const state = getCommentState(targetType, targetId);
  if (state.actioningId) return;
  state.actioningId = comment.id;
  try {
    const res = await deleteTopicComment(comment.id);
    if (res.code === 200) {
      toast.success('删除成功');
      await loadComments(targetType, targetId);
      if (targetType === 'post') {
        const post = posts.value.find(p => p.id === targetId);
        if (post) post.commentCount = Math.max(0, (post.commentCount || 0) - 1);
      } else if (targetType === 'topic' && topic.value) {
        topic.value.commentCount = Math.max(0, (topic.value.commentCount || 0) - 1);
      }
    } else {
      toast.error(res.message || '删除失败');
    }
  } catch (err) {
    const e = err as { message?: string };
    toast.error(e?.message || '删除失败');
  } finally {
    state.actioningId = null;
  }
}
</script>

<template>
  <div class="min-h-screen flex flex-col" style="background-color: var(--theme-bg);">
    <!-- 吸顶面包屑栏 -->
    <div
      class="border-b sticky top-0 z-30 backdrop-blur-sm py-3"
      style="background-color: var(--theme-surface); border-color: var(--theme-border);"
    >
      <div class="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 flex items-center justify-between gap-4">
        <Breadcrumb :items="breadcrumbs" />
        <div class="flex items-center gap-2">
          <button
            v-if="isOwner"
            @click="gotoEdit"
            class="flex items-center text-sm px-3 py-1.5 rounded-lg transition hover:opacity-90"
            style="color: var(--theme-primary); border: 1px solid var(--theme-border);"
          >
            <Pencil class="w-4 h-4 mr-1" />
            编辑
          </button>
        </div>
      </div>
    </div>

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
      class="flex-1 flex items-center justify-center py-16"
    >
      <div
        class="rounded-xl border p-8 max-w-md text-center"
        style="background-color: var(--theme-surface); border-color: var(--theme-border);"
      >
        <p class="mb-4 text-sm" style="color: var(--theme-text);">{{ error }}</p>
        <button
          @click="loadTopic"
          class="px-4 py-2 text-white rounded-lg text-sm transition hover:opacity-90"
          style="background-color: var(--theme-primary);"
        >
          重试
        </button>
      </div>
    </div>

    <!-- 主体内容 -->
    <template v-else-if="topic">
      <div class="flex-1 py-6">
        <div class="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8">
          <!-- 话题信息区 -->
          <div
            class="rounded-2xl border overflow-hidden mb-6"
            style="background-color: var(--theme-surface); border-color: var(--theme-border);"
          >
            <div
              v-if="topic.cover"
              class="relative h-48 sm:h-60"
              style="background-color: var(--theme-bg);"
            >
              <LazyImage
                :src="topic.cover"
                :alt="topic.title"
                class="w-full h-full object-cover"
              />
              <div class="absolute inset-0" style="background: linear-gradient(180deg, transparent 50%, rgba(0,0,0,0.5) 100%);"></div>
            </div>
            <div class="p-6">
              <div class="flex items-center gap-2 mb-3 flex-wrap">
                <span
                  v-if="topic.pinned === 1"
                  class="inline-flex items-center px-2 py-0.5 rounded text-xs font-medium text-white"
                  style="background-color: #f59e0b;"
                >
                  <Pin class="w-3 h-3 mr-1" />置顶
                </span>
                <span
                  class="inline-flex items-center px-2 py-0.5 rounded text-xs font-medium"
                  :style="{
                    color: topic.status === 'closed' ? '#ef4444' : 'var(--theme-primary)',
                    backgroundColor: topic.status === 'closed' ? 'rgba(239,68,68,0.1)' : 'var(--theme-accent)',
                  }"
                >
                  {{ topic.status === 'closed' ? '已关闭' : '讨论中' }}
                </span>
              </div>
              <h1 class="text-2xl sm:text-3xl font-bold mb-3" style="color: var(--theme-text);">
                {{ topic.title }}
              </h1>
              <p
                v-if="topic.description"
                class="text-sm mb-4 leading-relaxed"
                style="color: var(--theme-text-secondary);"
              >
                {{ topic.description }}
              </p>

              <!-- 发起人 + 时间 -->
              <div class="flex items-center justify-between flex-wrap gap-2 mb-4">
                <div class="flex items-center">
                  <img
                    :src="getSafeAvatar(topic.creator?.avatar, String(topic.creatorId))"
                    :alt="topic.creator?.nickname || '发起人'"
                    class="w-8 h-8 rounded-full object-cover mr-2 flex-shrink-0"
                    loading="lazy"
                  />
                  <div>
                    <div class="flex items-center text-sm" style="color: var(--theme-text);">
                      <span>{{ topic.creator?.nickname || '匿名用户' }}</span>
                      <BadgeCheck
                        v-if="topic.creator?.isCertifiedCreator"
                        class="w-3.5 h-3.5 ml-1"
                        style="color: var(--theme-primary);"
                      />
                    </div>
                    <div class="text-xs" style="color: var(--theme-text-secondary);">
                      发起于 {{ formatShortDate(topic.createdTime) }}
                    </div>
                  </div>
                </div>

                <!-- 操作按钮 -->
                <div class="flex items-center gap-2">
                  <button
                    @click="handleToggleTopicLike"
                    :disabled="likingTopic"
                    class="inline-flex items-center px-3 py-1.5 rounded-lg text-sm transition hover:opacity-80 disabled:opacity-50"
                    :style="{
                      color: topic.isLiked ? 'white' : 'var(--theme-text)',
                      backgroundColor: topic.isLiked ? 'var(--theme-primary)' : 'var(--theme-bg)',
                      border: `1px solid ${topic.isLiked ? 'var(--theme-primary)' : 'var(--theme-border)'}`,
                    }"
                  >
                    <Loader2 v-if="likingTopic" class="w-4 h-4 mr-1 animate-spin" />
                    <Heart v-else class="w-4 h-4 mr-1" :fill="topic.isLiked ? 'currentColor' : 'none'" />
                    {{ formatNumber(topic.likeCount) }}
                  </button>
                </div>
              </div>

              <!-- 统计 -->
              <div
                class="flex items-center gap-4 pt-3 border-t text-xs"
                style="border-color: var(--theme-border); color: var(--theme-text-secondary);"
              >
                <span class="flex items-center">
                  <Eye class="w-3.5 h-3.5 mr-1" />{{ formatNumber(topic.viewCount) }} 浏览
                </span>
                <span class="flex items-center">
                  <MessageSquare class="w-3.5 h-3.5 mr-1" />{{ formatNumber(topic.postCount) }} 观点
                </span>
                <span class="flex items-center">
                  <Heart class="w-3.5 h-3.5 mr-1" />{{ formatNumber(topic.likeCount) }} 点赞
                </span>
                <span
                  v-if="topic.lastPostTime"
                  class="flex items-center ml-auto"
                >
                  <MessageCircle class="w-3.5 h-3.5 mr-1" />
                  最后回复 {{ formatRelativeTime(topic.lastPostTime) }}
                </span>
              </div>
            </div>
          </div>

          <!-- 发表观点（默认折叠，点击展开） -->
          <div
            class="rounded-2xl border p-5 mb-6"
            style="background-color: var(--theme-surface); border-color: var(--theme-border);"
          >
            <!-- 折叠态：仅显示触发按钮 -->
            <button
              v-if="!showPostEditor"
              @click="togglePostEditor"
              class="w-full flex items-center justify-center gap-2 py-2.5 rounded-lg text-sm font-medium transition hover:opacity-90"
              style="color: var(--theme-primary); border: 1px dashed var(--theme-border);"
            >
              <Send class="w-4 h-4" />
              发表你的观点
            </button>

            <!-- 展开态：模式切换 + 编辑器 + 提交按钮 -->
            <div v-else>
              <div class="flex items-center justify-between mb-3">
                <h2 class="text-base font-semibold" style="color: var(--theme-text);">
                  发表你的观点
                </h2>
                <div class="flex items-center gap-2">
                  <!-- 普通文本 / 富文本切换 -->
                  <div
                    class="inline-flex rounded-lg border overflow-hidden text-xs"
                    style="border-color: var(--theme-border);"
                  >
                    <button
                      type="button"
                      @click="switchPostEditorMode('plain')"
                      :class="postEditorMode === 'plain' ? 'text-white' : ''"
                      :style="{
                        backgroundColor: postEditorMode === 'plain' ? 'var(--theme-primary)' : 'transparent',
                        color: postEditorMode === 'plain' ? 'white' : 'var(--theme-text-secondary)',
                      }"
                      class="px-3 py-1.5 transition"
                    >
                      普通文本
                    </button>
                    <button
                      type="button"
                      @click="switchPostEditorMode('richtext')"
                      :class="postEditorMode === 'richtext' ? 'text-white' : ''"
                      :style="{
                        backgroundColor: postEditorMode === 'richtext' ? 'var(--theme-primary)' : 'transparent',
                        color: postEditorMode === 'richtext' ? 'white' : 'var(--theme-text-secondary)',
                      }"
                      class="px-3 py-1.5 transition"
                    >
                      富文本
                    </button>
                  </div>
                  <button
                    @click="togglePostEditor"
                    class="inline-flex items-center px-2 py-1.5 rounded-lg text-xs transition hover:opacity-80"
                    style="color: var(--theme-text-secondary);"
                    title="收起"
                  >
                    <X class="w-4 h-4" />
                  </button>
                </div>
              </div>

              <!-- 普通文本模式：纯 textarea，无格式 -->
              <textarea
                v-if="postEditorMode === 'plain'"
                v-model="newPostContent"
                placeholder="说点什么，纯文本即可..."
                class="w-full p-3 border rounded-lg text-sm resize-y focus:outline-none"
                style="border-color: var(--theme-border); background-color: var(--theme-bg); color: var(--theme-text); min-height: 120px;"
                rows="5"
              ></textarea>

              <!-- 富文本模式：MarkdownEditor，支持图片上传和格式化 -->
              <MarkdownEditor
                v-else
                v-model="newPostContent"
                placeholder="支持 Markdown 语法，可插入图片..."
              />

              <div class="flex items-center justify-end mt-3 gap-2">
                <button
                  @click="togglePostEditor"
                  class="inline-flex items-center px-4 py-2 text-sm rounded-lg transition hover:opacity-80"
                  style="color: var(--theme-text-secondary);"
                >
                  取消
                </button>
                <button
                  @click="handleSubmitPost"
                  :disabled="submitting || !newPostContent.trim()"
                  class="inline-flex items-center px-4 py-2 text-sm text-white rounded-lg transition hover:opacity-90 disabled:opacity-50"
                  style="background-color: var(--theme-primary);"
                >
                  <Loader2 v-if="submitting" class="w-4 h-4 mr-1 animate-spin" />
                  <Send v-else class="w-4 h-4 mr-1" />
                  发表观点
                </button>
              </div>
            </div>
          </div>

          <!-- 观点列表 -->
          <div id="topic-posts">
            <div class="flex items-center justify-between mb-4">
              <h2 class="text-lg font-semibold" style="color: var(--theme-text);">
                全部观点
                <span class="ml-1 text-sm font-normal" style="color: var(--theme-text-secondary);">
                  ({{ postsTotal }})
                </span>
              </h2>
            </div>

            <!-- 观点加载中 -->
            <div v-if="postsLoading" class="flex justify-center py-12">
              <div
                class="animate-spin rounded-full h-8 w-8 border-b-2"
                style="border-color: var(--theme-primary);"
              ></div>
            </div>

            <Empty
              v-else-if="posts.length === 0"
              title="还没有观点"
              description="来发表第一条观点，开启讨论吧"
              size="md"
            />

            <template v-else>
              <div class="space-y-4 mb-8">
                <div
                  v-for="post in posts"
                  :key="post.id"
                  class="rounded-xl border p-5"
                  style="background-color: var(--theme-surface); border-color: var(--theme-border);"
                >
                  <!-- 头部：楼层 + 用户 -->
                  <div class="flex items-center justify-between mb-3">
                    <div class="flex items-center">
                      <img
                        :src="getSafeAvatar(post.user?.avatar, String(post.userId))"
                        :alt="post.user?.nickname || '用户'"
                        class="w-8 h-8 rounded-full object-cover mr-2 flex-shrink-0"
                        loading="lazy"
                      />
                      <div>
                        <div class="flex items-center text-sm" style="color: var(--theme-text);">
                          <span>{{ post.user?.nickname || '匿名用户' }}</span>
                          <span
                            v-if="topic && String(post.userId) === String(topic.creatorId)"
                            class="ml-2 inline-flex items-center px-1.5 py-0.5 rounded text-xs"
                            style="color: var(--theme-primary); background-color: var(--theme-accent);"
                          >
                            楼主
                          </span>
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

                  <!-- 回复提示 -->
                  <div
                    v-if="post.replyToUser"
                    class="text-xs mb-2 px-2 py-1 rounded"
                    style="color: var(--theme-text-secondary); background-color: var(--theme-accent);"
                  >
                    回复 @{{ post.replyToUser.nickname }}
                  </div>

                  <!-- 观点内容（统一用 markdown 模式渲染，支持图片/格式化） -->
                  <div
                    class="text-sm leading-relaxed"
                    style="color: var(--theme-text);"
                  >
                    <MarkdownRenderer
                      v-if="post.content"
                      editor-mode="markdown"
                      :content-markdown="post.content"
                    />
                    <p v-else style="color: var(--theme-text-secondary);">（该观点内容为空）</p>
                  </div>

                  <!-- 图片（独立 images 字段，富文本编辑器内嵌图片已由 markdown 渲染） -->
                  <div
                    v-if="post.images && post.images.length > 0"
                    class="grid grid-cols-2 sm:grid-cols-3 gap-2 mt-3"
                  >
                    <LazyImage
                      v-for="(img, idx) in post.images"
                      :key="idx"
                      :src="img"
                      :alt="`图片${idx + 1}`"
                      class="rounded-lg object-cover w-full h-24"
                    />
                  </div>

                  <!-- 操作栏 -->
                  <div
                    class="flex items-center justify-between gap-3 mt-3 pt-3 border-t text-xs"
                    style="border-color: var(--theme-border);"
                  >
                    <button
                      @click="toggleComments('post', post.id)"
                      class="inline-flex items-center transition hover:opacity-80"
                      :style="{ color: postCommentState(post).expanded ? 'var(--theme-primary)' : 'var(--theme-text-secondary)' }"
                    >
                      <MessageCircle class="w-3.5 h-3.5 mr-1" />
                      {{ formatNumber(post.commentCount) }} 评论
                      <ChevronDown
                        class="w-3 h-3 ml-1 transition-transform"
                        :class="{ 'rotate-180': postCommentState(post).expanded }"
                      />
                    </button>
                    <div class="flex items-center gap-3">
                      <button
                        @click="handleTogglePostLike(post)"
                        :disabled="actionPostId === post.id"
                        class="inline-flex items-center transition hover:opacity-80 disabled:opacity-50"
                        :style="{ color: post.isLiked ? 'var(--theme-primary)' : 'var(--theme-text-secondary)' }"
                      >
                        <Heart class="w-3.5 h-3.5 mr-1" :fill="post.isLiked ? 'currentColor' : 'none'" />
                        {{ formatNumber(post.likeCount) }}
                      </button>
                      <button
                        v-if="post.isOwner || isOwner"
                        @click="handleDeletePost(post)"
                        :disabled="actionPostId === post.id"
                        class="inline-flex items-center transition hover:opacity-80 disabled:opacity-50"
                        style="color: #ef4444;"
                      >
                        <Trash2 class="w-3.5 h-3.5 mr-1" />
                        删除
                      </button>
                    </div>
                  </div>

                  <!-- 评论区块（默认折叠） -->
                  <div
                    v-if="postCommentState(post).expanded"
                    class="mt-3 pt-3 border-t"
                    style="border-color: var(--theme-border);"
                  >
                    <!-- 加载中 -->
                    <div v-if="postCommentState(post).loading" class="flex justify-center py-4">
                      <div
                        class="animate-spin rounded-full h-6 w-6 border-b-2"
                        style="border-color: var(--theme-primary);"
                      ></div>
                    </div>

                    <template v-else>
                      <!-- 空状态 -->
                      <div
                        v-if="postCommentState(post).list.length === 0"
                        class="text-center py-3 text-xs"
                        style="color: var(--theme-text-secondary);"
                      >
                        暂无评论，快来抢沙发
                      </div>

                      <!-- 评论列表 -->
                      <div v-else class="space-y-3">
                        <div
                          v-for="rootC in postCommentState(post).list"
                          :key="rootC.id"
                        >
                          <!-- 一级评论 -->
                          <div class="flex gap-2">
                            <img
                              :src="getCommentAuthorAvatar(rootC)"
                              :alt="getCommentAuthorName(rootC)"
                              class="w-7 h-7 rounded-full flex-shrink-0"
                              loading="lazy"
                            />
                            <div class="flex-1 min-w-0">
                              <div class="flex items-center gap-2 mb-1">
                                <span class="text-xs font-medium" style="color: var(--theme-text);">
                                  {{ getCommentAuthorName(rootC) }}
                                </span>
                                <span class="text-xs" style="color: var(--theme-text-secondary);">
                                  {{ formatRelativeTime(rootC.createdTime) }}
                                </span>
                              </div>
                              <p class="text-sm mb-1.5" style="color: var(--theme-text);">
                                {{ rootC.content }}
                              </p>
                              <div class="flex items-center gap-3">
                                <button
                                  @click="handleLikeComment('post', post.id, rootC)"
                                  :disabled="postCommentState(post).actioningId === rootC.id"
                                  class="inline-flex items-center text-xs transition hover:opacity-80 disabled:opacity-50"
                                  :style="{ color: rootC.isLiked ? 'var(--theme-primary)' : 'var(--theme-text-secondary)' }"
                                >
                                  <Heart class="w-3 h-3 mr-0.5" :fill="rootC.isLiked ? 'currentColor' : 'none'" />
                                  {{ formatNumber(rootC.likeCount) }}
                                </button>
                                <button
                                  @click="startReply('post', post.id, rootC)"
                                  class="inline-flex items-center text-xs transition hover:opacity-80"
                                  style="color: var(--theme-text-secondary);"
                                >
                                  <Reply class="w-3 h-3 mr-0.5" />
                                  回复
                                </button>
                                <button
                                  v-if="canDeleteComment(rootC)"
                                  @click="handleDeleteComment('post', post.id, rootC)"
                                  :disabled="postCommentState(post).actioningId === rootC.id"
                                  class="inline-flex items-center text-xs transition hover:opacity-80 disabled:opacity-50"
                                  style="color: #ef4444;"
                                >
                                  <Trash2 class="w-3 h-3 mr-0.5" />
                                  删除
                                </button>
                              </div>
                            </div>
                          </div>

                          <!-- 回复输入框（一级评论下） -->
                          <div
                            v-if="postCommentState(post).replyingRoot?.id === rootC.id && postCommentState(post).replyingTo?.id === rootC.id"
                            class="mt-2 ml-9 p-2 rounded-lg"
                            style="background-color: var(--theme-bg);"
                          >
                            <div class="text-xs mb-1" style="color: var(--theme-text-secondary);">
                              回复 {{ getCommentAuthorName(rootC) }}
                            </div>
                            <textarea
                              v-model="postCommentState(post).replyContent"
                              placeholder="写回复..."
                              class="w-full p-2 border rounded-lg text-sm resize-none focus:outline-none"
                              style="border-color: var(--theme-border); background-color: var(--theme-bg); color: var(--theme-text);"
                              rows="2"
                            ></textarea>
                            <div class="flex justify-end mt-1 gap-2">
                              <button
                                @click="cancelReply('post', post.id)"
                                class="px-2 py-1 rounded text-xs transition hover:opacity-80"
                                style="color: var(--theme-text-secondary);"
                              >
                                取消
                              </button>
                              <button
                                @click="handleSubmitReply('post', post.id)"
                                :disabled="!postCommentState(post).replyContent.trim()"
                                class="px-2 py-1 rounded text-xs text-white transition hover:opacity-90 disabled:opacity-50"
                                style="background-color: var(--theme-primary);"
                              >
                                回复
                              </button>
                            </div>
                          </div>

                          <!-- 二级回复列表 -->
                          <div
                            v-if="rootC.replies && rootC.replies.length > 0"
                            class="mt-2 ml-9 space-y-2"
                          >
                            <div
                              v-for="reply in rootC.replies"
                              :key="reply.id"
                            >
                              <div class="flex gap-2">
                                <img
                                  :src="getCommentAuthorAvatar(reply)"
                                  :alt="getCommentAuthorName(reply)"
                                  class="w-6 h-6 rounded-full flex-shrink-0"
                                  loading="lazy"
                                />
                                <div class="flex-1 min-w-0">
                                  <div class="flex items-center gap-2 mb-1 flex-wrap">
                                    <span class="text-xs font-medium" style="color: var(--theme-text);">
                                      {{ getCommentAuthorName(reply) }}
                                    </span>
                                    <span
                                      v-if="getReplyToName(reply)"
                                      class="text-xs"
                                      style="color: var(--theme-primary);"
                                    >
                                      回复 @{{ getReplyToName(reply) }}
                                    </span>
                                    <span class="text-xs" style="color: var(--theme-text-secondary);">
                                      {{ formatRelativeTime(reply.createdTime) }}
                                    </span>
                                  </div>
                                  <p class="text-sm mb-1.5" style="color: var(--theme-text);">
                                    {{ reply.content }}
                                  </p>
                                  <div class="flex items-center gap-3">
                                    <button
                                      @click="handleLikeComment('post', post.id, reply)"
                                      :disabled="postCommentState(post).actioningId === reply.id"
                                      class="inline-flex items-center text-xs transition hover:opacity-80 disabled:opacity-50"
                                      :style="{ color: reply.isLiked ? 'var(--theme-primary)' : 'var(--theme-text-secondary)' }"
                                    >
                                      <Heart class="w-3 h-3 mr-0.5" :fill="reply.isLiked ? 'currentColor' : 'none'" />
                                      {{ formatNumber(reply.likeCount) }}
                                    </button>
                                    <button
                                      @click="startReply('post', post.id, rootC, reply)"
                                      class="inline-flex items-center text-xs transition hover:opacity-80"
                                      style="color: var(--theme-text-secondary);"
                                    >
                                      <Reply class="w-3 h-3 mr-0.5" />
                                      回复
                                    </button>
                                    <button
                                      v-if="canDeleteComment(reply)"
                                      @click="handleDeleteComment('post', post.id, reply)"
                                      :disabled="postCommentState(post).actioningId === reply.id"
                                      class="inline-flex items-center text-xs transition hover:opacity-80 disabled:opacity-50"
                                      style="color: #ef4444;"
                                    >
                                      <Trash2 class="w-3 h-3 mr-0.5" />
                                      删除
                                    </button>
                                  </div>
                                </div>
                              </div>

                              <!-- 回复输入框（二级回复下） -->
                              <div
                                v-if="postCommentState(post).replyingTo?.id === reply.id && postCommentState(post).replyingRoot?.id === rootC.id"
                                class="mt-2 ml-8 p-2 rounded-lg"
                                style="background-color: var(--theme-bg);"
                              >
                                <div class="text-xs mb-1" style="color: var(--theme-text-secondary);">
                                  回复 {{ getReplyToName(reply) || getCommentAuthorName(reply) }}
                                </div>
                                <textarea
                                  v-model="postCommentState(post).replyContent"
                                  placeholder="写回复..."
                                  class="w-full p-2 border rounded-lg text-sm resize-none focus:outline-none"
                                  style="border-color: var(--theme-border); background-color: var(--theme-bg); color: var(--theme-text);"
                                  rows="2"
                                ></textarea>
                                <div class="flex justify-end mt-1 gap-2">
                                  <button
                                    @click="cancelReply('post', post.id)"
                                    class="px-2 py-1 rounded text-xs transition hover:opacity-80"
                                    style="color: var(--theme-text-secondary);"
                                  >
                                    取消
                                  </button>
                                  <button
                                    @click="handleSubmitReply('post', post.id)"
                                    :disabled="!postCommentState(post).replyContent.trim()"
                                    class="px-2 py-1 rounded text-xs text-white transition hover:opacity-90 disabled:opacity-50"
                                    style="background-color: var(--theme-primary);"
                                  >
                                    回复
                                  </button>
                                </div>
                              </div>
                            </div>
                          </div>
                        </div>
                      </div>

                      <!-- 一级评论输入框 -->
                      <div v-if="userStore.user" class="mt-3 flex gap-2">
                        <img
                          :src="getSafeAvatar(userStore.user.avatar, userStore.user.id)"
                          :alt="userStore.user.username"
                          class="w-7 h-7 rounded-full flex-shrink-0"
                          loading="lazy"
                        />
                        <div class="flex-1">
                          <textarea
                            v-model="postCommentState(post).newContent"
                            placeholder="写下你的评论..."
                            class="w-full p-2 border rounded-lg text-sm resize-none focus:outline-none"
                            style="border-color: var(--theme-border); background-color: var(--theme-bg); color: var(--theme-text);"
                            rows="2"
                          ></textarea>
                          <div class="flex justify-end mt-1">
                            <button
                              @click="handleSubmitComment('post', post.id)"
                              :disabled="!postCommentState(post).newContent.trim()"
                              class="inline-flex items-center px-3 py-1 rounded-lg text-xs text-white transition hover:opacity-90 disabled:opacity-50"
                              style="background-color: var(--theme-primary);"
                            >
                              <Send class="w-3 h-3 mr-1" />
                              发表
                            </button>
                          </div>
                        </div>
                      </div>
                    </template>
                  </div>
                </div>
              </div>

              <!-- 分页 -->
              <div v-if="postsTotalPages > 1" class="flex flex-wrap items-center justify-center gap-2 mt-8">
                <button
                  @click="gotoPostsPage(postsPage - 1)"
                  :disabled="postsPage === 1"
                  class="px-3 py-2 rounded-lg text-sm transition disabled:opacity-40 disabled:cursor-not-allowed flex items-center"
                  style="background-color: var(--theme-surface); border: 1px solid var(--theme-border); color: var(--theme-text);"
                >
                  <ChevronLeft class="w-4 h-4" />
                  上一页
                </button>
                <span class="px-4 py-2 text-sm" style="color: var(--theme-text-secondary);">
                  第 {{ postsPage }} / {{ postsTotalPages }} 页
                </span>
                <button
                  @click="gotoPostsPage(postsPage + 1)"
                  :disabled="postsPage === postsTotalPages"
                  class="px-3 py-2 rounded-lg text-sm transition disabled:opacity-40 disabled:cursor-not-allowed flex items-center"
                  style="background-color: var(--theme-surface); border: 1px solid var(--theme-border); color: var(--theme-text);"
                >
                  下一页
                  <ChevronRight class="w-4 h-4" />
                </button>
              </div>
            </template>
          </div>

          <!-- 讨论区（话题级评论） -->
          <div
            id="topic-discussion"
            class="rounded-2xl border p-5 mt-8"
            style="background-color: var(--theme-surface); border-color: var(--theme-border);"
          >
            <h2 class="text-lg font-semibold mb-4 flex items-center" style="color: var(--theme-text);">
              <MessageSquare class="w-5 h-5 mr-2" style="color: var(--theme-primary);" />
              讨论区
              <span class="ml-1 text-sm font-normal" style="color: var(--theme-text-secondary);">
                ({{ formatNumber(topic.commentCount) }})
              </span>
            </h2>

            <!-- 加载中 -->
            <div v-if="topicCommentState().loading" class="flex justify-center py-8">
              <div
                class="animate-spin rounded-full h-8 w-8 border-b-2"
                style="border-color: var(--theme-primary);"
              ></div>
            </div>

            <template v-else>
              <!-- 空状态 -->
              <div
                v-if="topicCommentState().list.length === 0"
                class="text-center py-6 text-sm"
                style="color: var(--theme-text-secondary);"
              >
                暂无讨论，快来发起讨论吧
              </div>

              <!-- 评论列表 -->
              <div v-else class="space-y-4 mb-6">
                <div
                  v-for="rootC in topicCommentState().list"
                  :key="rootC.id"
                >
                  <!-- 一级评论 -->
                  <div class="flex gap-3">
                    <img
                      :src="getCommentAuthorAvatar(rootC)"
                      :alt="getCommentAuthorName(rootC)"
                      class="w-9 h-9 rounded-full flex-shrink-0"
                      loading="lazy"
                    />
                    <div class="flex-1 min-w-0">
                      <div class="flex items-center gap-2 mb-1">
                        <span class="text-sm font-medium" style="color: var(--theme-text);">
                          {{ getCommentAuthorName(rootC) }}
                        </span>
                        <span class="text-xs" style="color: var(--theme-text-secondary);">
                          {{ formatRelativeTime(rootC.createdTime) }}
                        </span>
                      </div>
                      <p class="text-sm mb-2" style="color: var(--theme-text);">
                        {{ rootC.content }}
                      </p>
                      <div class="flex items-center gap-4">
                        <button
                          @click="handleLikeComment('topic', topic.id, rootC)"
                          :disabled="topicCommentState().actioningId === rootC.id"
                          class="inline-flex items-center text-xs transition hover:opacity-80 disabled:opacity-50"
                          :style="{ color: rootC.isLiked ? 'var(--theme-primary)' : 'var(--theme-text-secondary)' }"
                        >
                          <Heart class="w-3.5 h-3.5 mr-1" :fill="rootC.isLiked ? 'currentColor' : 'none'" />
                          {{ formatNumber(rootC.likeCount) }}
                        </button>
                        <button
                          @click="startReply('topic', topic.id, rootC)"
                          class="inline-flex items-center text-xs transition hover:opacity-80"
                          style="color: var(--theme-text-secondary);"
                        >
                          <Reply class="w-3.5 h-3.5 mr-1" />
                          回复
                        </button>
                        <button
                          v-if="canDeleteComment(rootC)"
                          @click="handleDeleteComment('topic', topic.id, rootC)"
                          :disabled="topicCommentState().actioningId === rootC.id"
                          class="inline-flex items-center text-xs transition hover:opacity-80 disabled:opacity-50"
                          style="color: #ef4444;"
                        >
                          <Trash2 class="w-3.5 h-3.5 mr-1" />
                          删除
                        </button>
                      </div>
                    </div>
                  </div>

                  <!-- 回复输入框（一级评论下） -->
                  <div
                    v-if="topicCommentState().replyingRoot?.id === rootC.id && topicCommentState().replyingTo?.id === rootC.id"
                    class="mt-3 ml-12 p-3 rounded-lg"
                    style="background-color: var(--theme-bg);"
                  >
                    <div class="text-xs mb-1" style="color: var(--theme-text-secondary);">
                      回复 {{ getCommentAuthorName(rootC) }}
                    </div>
                    <textarea
                      v-model="topicCommentState().replyContent"
                      placeholder="写回复..."
                      class="w-full p-2 border rounded-lg text-sm resize-none focus:outline-none"
                      style="border-color: var(--theme-border); background-color: var(--theme-bg); color: var(--theme-text);"
                      rows="2"
                    ></textarea>
                    <div class="flex justify-end mt-2 gap-2">
                      <button
                        @click="cancelReply('topic', topic.id)"
                        class="px-3 py-1 rounded-lg text-xs transition hover:opacity-80"
                        style="color: var(--theme-text-secondary);"
                      >
                        取消
                      </button>
                      <button
                        @click="handleSubmitReply('topic', topic.id)"
                        :disabled="!topicCommentState().replyContent.trim()"
                        class="px-3 py-1 rounded-lg text-xs text-white transition hover:opacity-90 disabled:opacity-50"
                        style="background-color: var(--theme-primary);"
                      >
                        回复
                      </button>
                    </div>
                  </div>

                  <!-- 二级回复列表 -->
                  <div
                    v-if="rootC.replies && rootC.replies.length > 0"
                    class="mt-3 ml-12 space-y-3"
                  >
                    <div
                      v-for="reply in rootC.replies"
                      :key="reply.id"
                    >
                      <div class="flex gap-2">
                        <img
                          :src="getCommentAuthorAvatar(reply)"
                          :alt="getCommentAuthorName(reply)"
                          class="w-7 h-7 rounded-full flex-shrink-0"
                          loading="lazy"
                        />
                        <div class="flex-1 min-w-0">
                          <div class="flex items-center gap-2 mb-1 flex-wrap">
                            <span class="text-xs font-medium" style="color: var(--theme-text);">
                              {{ getCommentAuthorName(reply) }}
                            </span>
                            <span
                              v-if="getReplyToName(reply)"
                              class="text-xs"
                              style="color: var(--theme-primary);"
                            >
                              回复 @{{ getReplyToName(reply) }}
                            </span>
                            <span class="text-xs" style="color: var(--theme-text-secondary);">
                              {{ formatRelativeTime(reply.createdTime) }}
                            </span>
                          </div>
                          <p class="text-sm mb-1.5" style="color: var(--theme-text);">
                            {{ reply.content }}
                          </p>
                          <div class="flex items-center gap-3">
                            <button
                              @click="handleLikeComment('topic', topic.id, reply)"
                              :disabled="topicCommentState().actioningId === reply.id"
                              class="inline-flex items-center text-xs transition hover:opacity-80 disabled:opacity-50"
                              :style="{ color: reply.isLiked ? 'var(--theme-primary)' : 'var(--theme-text-secondary)' }"
                            >
                              <Heart class="w-3 h-3 mr-0.5" :fill="reply.isLiked ? 'currentColor' : 'none'" />
                              {{ formatNumber(reply.likeCount) }}
                            </button>
                            <button
                              @click="startReply('topic', topic.id, rootC, reply)"
                              class="inline-flex items-center text-xs transition hover:opacity-80"
                              style="color: var(--theme-text-secondary);"
                            >
                              <Reply class="w-3 h-3 mr-0.5" />
                              回复
                            </button>
                            <button
                              v-if="canDeleteComment(reply)"
                              @click="handleDeleteComment('topic', topic.id, reply)"
                              :disabled="topicCommentState().actioningId === reply.id"
                              class="inline-flex items-center text-xs transition hover:opacity-80 disabled:opacity-50"
                              style="color: #ef4444;"
                            >
                              <Trash2 class="w-3 h-3 mr-0.5" />
                              删除
                            </button>
                          </div>
                        </div>
                      </div>

                      <!-- 回复输入框（二级回复下） -->
                      <div
                        v-if="topicCommentState().replyingTo?.id === reply.id && topicCommentState().replyingRoot?.id === rootC.id"
                        class="mt-2 ml-9 p-2 rounded-lg"
                        style="background-color: var(--theme-bg);"
                      >
                        <div class="text-xs mb-1" style="color: var(--theme-text-secondary);">
                          回复 {{ getReplyToName(reply) || getCommentAuthorName(reply) }}
                        </div>
                        <textarea
                          v-model="topicCommentState().replyContent"
                          placeholder="写回复..."
                          class="w-full p-2 border rounded-lg text-sm resize-none focus:outline-none"
                          style="border-color: var(--theme-border); background-color: var(--theme-bg); color: var(--theme-text);"
                          rows="2"
                        ></textarea>
                        <div class="flex justify-end mt-1 gap-2">
                          <button
                            @click="cancelReply('topic', topic.id)"
                            class="px-2 py-1 rounded text-xs transition hover:opacity-80"
                            style="color: var(--theme-text-secondary);"
                          >
                            取消
                          </button>
                          <button
                            @click="handleSubmitReply('topic', topic.id)"
                            :disabled="!topicCommentState().replyContent.trim()"
                            class="px-2 py-1 rounded text-xs text-white transition hover:opacity-90 disabled:opacity-50"
                            style="background-color: var(--theme-primary);"
                          >
                            回复
                          </button>
                        </div>
                      </div>
                    </div>
                  </div>
                </div>
              </div>

              <!-- 一级评论输入框 -->
              <div v-if="userStore.user" class="flex gap-3 pt-4 border-t" style="border-color: var(--theme-border);">
                <img
                  :src="getSafeAvatar(userStore.user.avatar, userStore.user.id)"
                  :alt="userStore.user.username"
                  class="w-9 h-9 rounded-full flex-shrink-0"
                  loading="lazy"
                />
                <div class="flex-1">
                  <textarea
                    v-model="topicCommentState().newContent"
                    placeholder="参与话题讨论..."
                    class="w-full p-3 border rounded-xl text-sm resize-none focus:outline-none"
                    style="border-color: var(--theme-border); background-color: var(--theme-bg); color: var(--theme-text);"
                    rows="2"
                  ></textarea>
                  <div class="flex justify-end mt-2">
                    <button
                      @click="handleSubmitComment('topic', topic.id)"
                      :disabled="!topicCommentState().newContent.trim()"
                      class="inline-flex items-center px-4 py-1.5 rounded-lg text-sm text-white transition hover:opacity-90 disabled:opacity-50"
                      style="background-color: var(--theme-primary);"
                    >
                      <Send class="w-4 h-4 mr-1" />
                      发表评论
                    </button>
                  </div>
                </div>
              </div>
            </template>
          </div>
        </div>
      </div>
    </template>

    <SiteFooter />
  </div>
</template>
