<script setup lang="ts">
import { ref, onMounted, computed } from 'vue';
import { useRoute, useRouter } from 'vue-router';
import { useHead } from '@vueuse/head';
import {
  ChevronLeft, ThumbsUp, MessageSquare, Eye, Calendar, Building2,
  Briefcase, User as UserIcon, Send, Tag as TagIcon
} from 'lucide-vue-next';
import SiteFooter from '@/components/SiteFooter.vue';
import LazyImage from '@/components/LazyImage.vue';
import MarkdownRenderer from '@/components/MarkdownRenderer.vue';
import { generateSeo } from '@/utils/seo';
import { getSafeAvatar } from '@/utils/avatar';
import {
  getExperienceDetail, toggleExperienceLike,
  getCommentList, publishComment, toggleCommentLike,
} from '@/api/interview';
import type { InterviewExperienceVO, InterviewCommentVO } from '@/types/api';

const route = useRoute();
const router = useRouter();
const experienceId = computed(() => route.params.id as string);

const loading = ref(false);
const commentLoading = ref(false);
const submittingComment = ref(false);
const experience = ref<InterviewExperienceVO | null>(null);
const comments = ref<InterviewCommentVO[]>([]);
const newComment = ref('');
const replyToComment = ref<InterviewCommentVO | null>(null);
const replyContent = ref('');

const toast = ref<{ message: string; type: 'success' | 'error' } | null>(null);
let toastTimer: number | null = null;

function showToast(message: string, type: 'success' | 'error' = 'success') {
  toast.value = { message, type };
  if (toastTimer) window.clearTimeout(toastTimer);
  toastTimer = window.setTimeout(() => { toast.value = null; }, 3000);
}

function displayName(vo: InterviewExperienceVO | InterviewCommentVO | null | undefined) {
  if (!vo) return '用户';
  const u = (vo as any).user;
  if (u?.nickname) return u.nickname;
  if ((vo as any).userNickname) return (vo as any).userNickname;
  return '用户';
}

function displayAvatar(vo: InterviewExperienceVO | InterviewCommentVO | null | undefined) {
  if (!vo) return getSafeAvatar('', String((vo as any)?.id || ''));
  const u = (vo as any).user;
  if (u?.avatar) return u.avatar;
  if ((vo as any).userAvatar) return (vo as any).userAvatar;
  return getSafeAvatar('', String((vo as any)?.id || ''));
}

onMounted(async () => {
  await loadExperience();
  await loadComments();
});

async function loadExperience() {
  try {
    loading.value = true;
    const res = await getExperienceDetail(experienceId.value);
    if (res.code === 200 && res.data) {
      experience.value = res.data;
    } else {
      showToast(res.message || '加载失败', 'error');
    }
  } catch (err: any) {
    showToast(err?.message || '加载面经失败，请稍后重试', 'error');
  } finally {
    loading.value = false;
  }
}

async function loadComments() {
  try {
    commentLoading.value = true;
    const res = await getCommentList({ experienceId: experienceId.value, pageSize: 50 });
    if (res.code === 200 && res.data) {
      comments.value = (res.data as any).list || [];
    }
  } catch (err: any) {
    console.error('加载评论失败:', err);
  } finally {
    commentLoading.value = false;
  }
}

async function handleLike() {
  if (!experience.value) return;
  try {
    const res = await toggleExperienceLike(experience.value.id);
    if (res.code === 200 && res.data) {
      experience.value.liked = res.data.liked;
      experience.value.likeCount = res.data.likeCount;
      showToast(res.data.liked ? '点赞成功' : '已取消点赞', 'success');
    } else {
      showToast(res.message || '操作失败', 'error');
    }
  } catch (err: any) {
    showToast(err?.message || '操作失败', 'error');
  }
}

async function handleSubmitComment() {
  if (!experience.value || !newComment.value.trim()) {
    showToast('请输入评论内容', 'error');
    return;
  }
  try {
    submittingComment.value = true;
    const res = await publishComment({
      experienceId: experience.value.id,
      content: newComment.value.trim(),
    });
    if (res.code === 200 && res.data) {
      comments.value = [res.data, ...comments.value];
      newComment.value = '';
      showToast('评论已发布', 'success');
    } else {
      showToast(res.message || '评论失败', 'error');
    }
  } catch (err: any) {
    showToast(err?.message || '评论失败', 'error');
  } finally {
    submittingComment.value = false;
  }
}

async function handleReplySubmit(parent: InterviewCommentVO) {
  if (!experience.value || !replyContent.value.trim()) {
    showToast('请输入回复内容', 'error');
    return;
  }
  try {
    submittingComment.value = true;
    const res = await publishComment({
      experienceId: experience.value.id,
      content: replyContent.value.trim(),
      parentId: parent.id,
    });
    if (res.code === 200 && res.data) {
      if (!parent.replies) parent.replies = [];
      parent.replies.push(res.data);
      replyContent.value = '';
      replyToComment.value = null;
      showToast('回复已发布', 'success');
    } else {
      showToast(res.message || '回复失败', 'error');
    }
  } catch (err: any) {
    showToast(err?.message || '回复失败', 'error');
  } finally {
    submittingComment.value = false;
  }
}

async function handleCommentLike(comment: InterviewCommentVO) {
  try {
    const res = await toggleCommentLike(comment.id);
    if (res.code === 200 && res.data) {
      comment.liked = res.data.liked;
      comment.likeCount = res.data.likeCount;
    } else {
      showToast(res.message || '操作失败', 'error');
    }
  } catch (err: any) {
    showToast(err?.message || '操作失败', 'error');
  }
}

useHead(
  computed(() => generateSeo({
    title: experience.value?.title || '面经详情',
    description: experience.value?.summary || experience.value?.content?.slice(0, 120) || '面试经验分享',
  }))
);
</script>

<template>
  <div class="min-h-screen flex flex-col bg-gray-50">
    <!-- 顶部导航 -->
    <div class="bg-white border-b border-gray-200 sticky top-0 z-30">
      <div class="max-w-5xl mx-auto px-4 py-3 flex items-center justify-between">
        <button
          @click="router.back()"
          class="flex items-center text-gray-600 hover:text-blue-600 transition text-sm"
        >
          <ChevronLeft class="w-4 h-4 mr-1" />
          返回
        </button>
        <span class="text-sm text-gray-500">面试经验</span>
        <span class="w-12"></span>
      </div>
    </div>

    <!-- Toast -->
    <div
      v-if="toast"
      class="fixed top-20 left-1/2 -translate-x-1/2 z-50 px-4 py-2 rounded-lg shadow-lg text-sm transition"
      :class="toast.type === 'success' ? 'bg-green-500 text-white' : 'bg-red-500 text-white'"
    >
      {{ toast.message }}
    </div>

    <div class="flex-1 py-8">
      <div class="max-w-5xl mx-auto px-4">
        <!-- Loading -->
        <div v-if="loading && !experience" class="text-center py-12">
          <div class="animate-spin rounded-full h-12 w-12 border-b-2 border-blue-600 mx-auto"></div>
          <p class="mt-4 text-gray-600">加载中...</p>
        </div>

        <template v-else-if="experience">
          <!-- 封面 / 标题区 -->
          <div class="bg-white rounded-xl shadow-sm overflow-hidden mb-6">
            <div v-if="experience.coverImage" class="h-56 bg-gray-100">
              <LazyImage :src="experience.coverImage" :alt="experience.title" class="w-full h-full object-cover" />
            </div>
            <div class="p-6">
              <div class="flex items-center flex-wrap gap-2 mb-3">
                <span class="inline-flex items-center px-3 py-1 bg-indigo-50 text-indigo-700 rounded-full text-xs font-medium">
                  <Building2 class="w-3 h-3 mr-1" />
                  {{ experience.company }}
                </span>
                <span v-if="experience.position" class="inline-flex items-center px-3 py-1 bg-blue-50 text-blue-700 rounded-full text-xs font-medium">
                  <Briefcase class="w-3 h-3 mr-1" />
                  {{ experience.position }}
                </span>
                <span
                  v-for="tag in experience.tags"
                  :key="tag"
                  class="inline-flex items-center px-2 py-1 bg-gray-100 text-gray-600 rounded text-xs"
                >
                  <TagIcon class="w-3 h-3 mr-1" />
                  {{ tag }}
                </span>
              </div>
              <h1 class="text-3xl font-bold text-gray-900 mb-4">{{ experience.title }}</h1>
              <div class="flex items-center flex-wrap gap-4 text-sm text-gray-500">
                <div class="flex items-center">
                  <div class="w-8 h-8 rounded-full overflow-hidden mr-2 bg-gray-200">
                    <LazyImage :src="displayAvatar(experience)" :alt="displayName(experience)" class="w-full h-full object-cover" />
                  </div>
                  <span class="font-medium text-gray-700">{{ displayName(experience) }}</span>
                </div>
                <span v-if="experience.year || experience.month" class="flex items-center">
                  <Calendar class="w-4 h-4 mr-1" />
                  {{ experience.year }}{{ experience.month ? ' 年 ' + experience.month + ' 月' : ' 年' }}
                </span>
                <span class="flex items-center"><Eye class="w-4 h-4 mr-1" />{{ experience.viewCount }} 阅读</span>
                <span class="flex items-center"><ThumbsUp class="w-4 h-4 mr-1" />{{ experience.likeCount }} 点赞</span>
                <span class="flex items-center"><MessageSquare class="w-4 h-4 mr-1" />{{ experience.commentCount }} 评论</span>
              </div>
            </div>
          </div>

          <!-- 正文 -->
          <div class="bg-white rounded-xl shadow-sm p-8 mb-6">
            <div v-if="experience.summary" class="text-gray-600 italic mb-6 p-4 bg-gray-50 rounded-lg border-l-4 border-blue-500">
              {{ experience.summary }}
            </div>
            <MarkdownRenderer
              :content="experience.content || ''"
              :content-markdown="experience.content"
              editor-mode="markdown"
              class="article-content"
            />
            <div class="mt-8 pt-6 border-t border-gray-100 flex items-center justify-center">
              <button
                @click="handleLike"
                class="inline-flex items-center px-6 py-2.5 rounded-full transition font-medium"
                :class="experience.liked ? 'bg-blue-600 text-white' : 'bg-blue-50 text-blue-600 hover:bg-blue-100'"
              >
                <ThumbsUp class="w-4 h-4 mr-2" />
                {{ experience.liked ? '已点赞 · ' : '点赞文章 · ' }}{{ experience.likeCount }}
              </button>
            </div>
          </div>

          <!-- 评论区 -->
          <div class="bg-white rounded-xl shadow-sm p-6">
            <h2 class="text-xl font-bold text-gray-900 mb-4 flex items-center">
              <MessageSquare class="w-5 h-5 mr-2 text-blue-500" />
              评论区 ({{ comments.length }})
            </h2>

            <!-- 发表评论 -->
            <div class="mb-6 bg-gray-50 p-4 rounded-lg">
              <textarea
                v-model="newComment"
                class="w-full p-3 border border-gray-200 rounded-lg text-sm focus:outline-none focus:ring-2 focus:ring-blue-500"
                rows="3"
                placeholder="写下你的想法、疑问或补充..."
              ></textarea>
              <div class="flex justify-end mt-2">
                <button
                  @click="handleSubmitComment"
                  :disabled="submittingComment"
                  class="inline-flex items-center px-4 py-2 bg-blue-600 text-white rounded-lg text-sm hover:bg-blue-700 disabled:bg-gray-400 transition"
                >
                  <Send class="w-4 h-4 mr-1" />
                  {{ submittingComment ? '发布中...' : '发布评论' }}
                </button>
              </div>
            </div>

            <!-- 评论列表 -->
            <div v-if="commentLoading" class="py-6 text-center text-sm text-gray-500">加载评论中...</div>
            <div v-else-if="comments.length === 0" class="py-8 text-center text-sm text-gray-500">
              还没有评论，来抢沙发吧~
            </div>
            <div v-else class="space-y-4">
              <div
                v-for="comment in comments"
                :key="comment.id"
                class="border border-gray-100 rounded-lg p-4 hover:shadow-sm transition"
              >
                <div class="flex items-start">
                  <div class="w-8 h-8 rounded-full overflow-hidden mr-3 bg-gray-200 flex-shrink-0">
                    <LazyImage
                      :src="displayAvatar(comment)"
                      :alt="displayName(comment)"
                      class="w-full h-full object-cover"
                    />
                  </div>
                  <div class="flex-1">
                    <div class="flex items-center justify-between mb-1">
                      <span class="font-medium text-sm text-gray-800">{{ displayName(comment) }}</span>
                      <span class="text-xs text-gray-400">{{ comment.createTime }}</span>
                    </div>
                    <div class="text-sm text-gray-700 mb-2 whitespace-pre-wrap">{{ comment.content }}</div>
                    <div class="flex items-center gap-3 text-xs text-gray-500">
                      <button
                        @click="handleCommentLike(comment)"
                        class="flex items-center hover:text-blue-500 transition"
                        :class="comment.liked ? 'text-blue-600' : ''"
                      >
                        <ThumbsUp class="w-3 h-3 mr-1" />
                        {{ comment.likeCount }}
                      </button>
                      <button
                        @click="replyToComment = replyToComment?.id === comment.id ? null : comment"
                        class="hover:text-blue-500 transition"
                      >
                        回复
                      </button>
                    </div>

                    <!-- 回复输入框 -->
                    <div v-if="replyToComment?.id === comment.id" class="mt-3">
                      <textarea
                        v-model="replyContent"
                        class="w-full p-2 border border-gray-200 rounded-lg text-sm focus:outline-none focus:ring-2 focus:ring-blue-500"
                        rows="2"
                        :placeholder="'回复 @' + displayName(comment)"
                      ></textarea>
                      <div class="flex justify-end gap-2 mt-2">
                        <button
                          @click="replyToComment = null"
                          class="px-3 py-1 text-sm text-gray-500 hover:text-gray-700"
                        >
                          取消
                        </button>
                        <button
                          @click="handleReplySubmit(comment)"
                          class="px-3 py-1 text-sm bg-blue-600 text-white rounded-lg hover:bg-blue-700"
                        >
                          发送回复
                        </button>
                      </div>
                    </div>

                    <!-- 子评论 -->
                    <div v-if="comment.replies && comment.replies.length > 0" class="mt-3 pl-4 border-l-2 border-gray-100 space-y-3">
                      <div
                        v-for="reply in comment.replies"
                        :key="reply.id"
                        class="text-sm"
                      >
                        <div class="flex items-start">
                          <div class="w-6 h-6 rounded-full overflow-hidden mr-2 bg-gray-200 flex-shrink-0">
                            <LazyImage :src="displayAvatar(reply)" :alt="displayName(reply)" class="w-full h-full object-cover" />
                          </div>
                          <div class="flex-1">
                            <div class="flex items-center">
                              <span class="font-medium text-gray-800 text-xs">{{ displayName(reply) }}</span>
                              <span v-if="reply.replyToUser" class="text-gray-400 text-xs mx-1">回复</span>
                              <span v-if="reply.replyToUser" class="font-medium text-gray-600 text-xs">{{ reply.replyToUser.nickname }}</span>
                            </div>
                            <div class="text-gray-700 mt-1 whitespace-pre-wrap">{{ reply.content }}</div>
                            <div class="text-xs text-gray-400 mt-1">{{ reply.createTime }}</div>
                          </div>
                        </div>
                      </div>
                    </div>
                  </div>
                </div>
              </div>
            </div>
          </div>
        </template>

        <div v-else class="text-center py-12">
          <p class="text-gray-600">未找到面经信息</p>
          <button
            @click="router.push('/interview')"
            class="mt-4 px-4 py-2 bg-blue-500 text-white rounded-lg text-sm hover:bg-blue-600 transition"
          >
            返回面试指南
          </button>
        </div>
      </div>
    </div>

    <SiteFooter />
  </div>
</template>

<style scoped>
.article-content :deep(h1),
.article-content :deep(h2),
.article-content :deep(h3) {
  margin-top: 1.5rem;
  margin-bottom: 0.75rem;
  font-weight: 700;
  color: #1f2937;
}
.article-content :deep(h1) { font-size: 1.75rem; }
.article-content :deep(h2) { font-size: 1.35rem; }
.article-content :deep(h3) { font-size: 1.15rem; }
.article-content :deep(p) { margin-bottom: 1rem; line-height: 1.8; }
.article-content :deep(ul),
.article-content :deep(ol) { margin: 1rem 0; padding-left: 1.5rem; }
.article-content :deep(li) { margin-bottom: 0.35rem; }
.article-content :deep(code) { background: #f3f4f6; padding: 2px 6px; border-radius: 4px; font-size: 0.85em; }
.article-content :deep(pre) { background: #111827; color: #e5e7eb; padding: 1rem; border-radius: 8px; overflow-x: auto; margin: 1rem 0; }
.article-content :deep(pre code) { background: transparent; padding: 0; color: inherit; }
.article-content :deep(blockquote) { border-left: 4px solid #3b82f6; padding: 0.5rem 1rem; color: #6b7280; background: #f9fafb; margin: 1rem 0; border-radius: 0 6px 6px 0; }
</style>
