<script setup lang="ts">
import { ref, onMounted, computed } from 'vue';
import { useRoute, RouterLink as Link, useRouter } from 'vue-router';
import { useHead } from '@vueuse/head';
import { ArrowLeft, Heart, Share2, MessageSquare, Eye, Clock, User as UserIcon, Tag, Send, Bookmark, MoreHorizontal, Reply } from 'lucide-vue-next';

import ArticleCard from '@/components/ArticleCard.vue';
import LazyImage from '@/components/LazyImage.vue';
import Breadcrumb from '@/components/Breadcrumb.vue';
import SiteFooter from '@/components/SiteFooter.vue';
import MarkdownRenderer from '@/components/MarkdownRenderer.vue';
import { useArticleStore } from '@/stores/article';
import { useUserStore } from '@/stores/user';
import { useAuth } from '@/composables/useAuth';
import { generateSeo } from '@/utils/seo';
import { sanitizeHTML } from '@/utils/security';
import { formatShortDate } from '@/utils/date';
import type { Article, Comment, User } from '@/types/api';
import * as articleApi from '@/api/article';
import * as commentApi from '@/api/comment';

const route = useRoute();
const router = useRouter();
const articleStore = useArticleStore();
const userStore = useUserStore();
const { isAuthenticated, requireAuth, withAuth } = useAuth();
const article = ref<Article | null>(null);
const comments = ref<Comment[]>([]);
const newComment = ref('');
const currentUser = computed(() => userStore.user);
const replyingTo = ref<Comment | null>(null);
const replyContent = ref('');
const displayedCount = ref(3);
const expandedReplies = ref<Set<string>>(new Set());
const loading = ref(false);
const submitting = ref(false);
const error = ref<string | null>(null);

const isLiked = computed(() => article.value ? articleStore.likedArticleIds.includes(article.value.id) : false);
const isBookmarked = computed(() => article.value ? articleStore.bookmarkedArticleIds.includes(article.value.id) : false);

// 获取文章作者信息
const articleAuthor = computed(() => {
  if (!article.value) return null;
  if (article.value.author) return article.value.author;
  // 如果没有 author 对象，从其他字段构造
  return {
    id: article.value.authorId || '',
    username: article.value.authorUsername || '',
    nickname: article.value.authorNickname || article.value.authorUsername || '',
    avatar: article.value.authorAvatar || '',
    bio: article.value.authorBio || '',
    createdAt: ''
  } as User;
});

// 获取文章分类
const articleCategory = computed(() => {
  if (!article.value) return '';
  return article.value.category || article.value.categoryName || '';
});

// 获取文章标签
const articleTags = computed(() => {
  if (!article.value) return [];
  if (Array.isArray(article.value.tags)) return article.value.tags;
  if ('tagNames' in article.value && Array.isArray(article.value.tagNames)) return article.value.tagNames;
  return [];
});

// 获取文章阅读量
const articleViews = computed(() => {
  return article.value ? article.value.views || 0 : 0;
});

// 获取文章点赞数
const articleLikes = computed(() => {
  return article.value ? article.value.likes || 0 : 0;
});

// 获取文章分享数
const articleShareCount = computed(() => {
  return article.value ? article.value.shareCount || 0 : 0;
});

// 获取文章摘要
const articleExcerpt = computed(() => {
  return article.value ? article.value.excerpt || '' : '';
});

const breadcrumbs = computed(() => {
  if (!article.value) return [];
  return [
    { label: articleCategory.value, path: `/list?category=${articleCategory.value}` },
    { label: article.value.title }
  ];
});

const sanitizedContent = computed(() =>
    article.value ? sanitizeHTML(article.value.content) : ''
);

const articleDate = computed(() => {
  if (!article.value) return '';
  const dateStr = article.value.createdAt || article.value.createTime;
  return dateStr ? formatShortDate(dateStr) : '';
});

const articleUpdateDate = computed(() => {
  if (!article.value) return '';
  const dateStr = article.value.updatedAt || article.value.createTime;
  return dateStr ? formatShortDate(dateStr) : '';
});

const displayedComments = computed(() => comments.value.slice(0, displayedCount.value));
const hasMoreComments = computed(() => displayedCount.value < comments.value.length);
const totalCommentsCount = computed(() => {
  let count = 0;
  const countReplies = (cmts: Comment[]) => {
    cmts.forEach(c => {
      count++;
      if (c.replies) countReplies(c.replies);
    });
  };
  countReplies(comments.value);
  return count;
});

onMounted(async () => {
  await loadArticle();
});

async function loadArticle() {
  loading.value = true;
  error.value = null;
  try {
    const articleId = route.params.id as string;

    const response = await articleApi.getArticleDetail({ id: articleId });
    if (response.code === 200) {
      article.value = response.data as Article;
    } else {
      error.value = response.message || '加载文章失败';
    }

    await loadComments(articleId);

    await articleApi.incrementView(articleId);
  } catch (err) {
    console.error('加载文章失败:', err);
    error.value = '加载文章失败，请稍后重试';
  } finally {
    loading.value = false;
  }
}

async function loadComments(articleId: string) {
  try {
    const response = await commentApi.getCommentList({ articleId });
    if (response.code === 200) {
      comments.value = response.data.list as Comment[];
    }
  } catch (error) {
    console.error('加载评论失败:', error);
  }
}

async function handleLike() {
  if (!article.value) return;
  // 使用 withAuth 包装，确保用户登录后才能执行点赞
  await withAuth(async () => {
    await articleStore.likeArticleWithApi(article.value.id);
  });
}

async function handleBookmark() {
  if (!article.value) return;
  // 使用 withAuth 包装，确保用户登录后才能执行收藏
  await withAuth(async () => {
    await articleStore.bookmarkArticleWithApi(article.value.id);
  });
}

function handleShare() {
  if (!article.value) return;
  articleStore.shareArticle(article.value.id);

  if (navigator.share) {
    navigator.share({
      title: article.value.title,
      text: articleExcerpt.value,
      url: window.location.href,
    }).catch(console.error);
  } else {
    navigator.clipboard.writeText(window.location.href);
    alert('链接已复制到剪贴板');
  }
}

async function handleSubmitComment() {
  if (!newComment.value.trim() || !article.value) return;
  
  // 使用 withAuth 包装，确保用户登录后才能发表评论
  const result = await withAuth(async () => {
    submitting.value = true;
    try {
      const response = await commentApi.addComment({
        articleId: article.value.id,
        content: newComment.value
      });

      if (response.code === 200) {
        comments.value.unshift(response.data as Comment);
      }
      newComment.value = '';
    } catch (error) {
      console.error('发表评论失败:', error);
    } finally {
      submitting.value = false;
    }
  });
}

function handleReply(comment: Comment) {
  // 检查是否登录
  if (!requireAuth()) {
    return;
  }
  replyingTo.value = comment;
  replyContent.value = '';
}

function handleCancelReply() {
  replyingTo.value = null;
  replyContent.value = '';
}

async function handleSubmitReply() {
  if (!replyContent.value.trim() || !currentUser.value || !article.value || !replyingTo.value) return;

  submitting.value = true;
  try {
    const response = await commentApi.addComment({
      articleId: article.value.id,
      content: replyContent.value,
      replyTo: replyingTo.value.id
    });

    if (response.code === 200) {
      if (!replyingTo.value.replies) replyingTo.value.replies = [];
      replyingTo.value.replies.push(response.data as Comment);
    }
    replyingTo.value = null;
    replyContent.value = '';
  } catch (error) {
    console.error('发表回复失败:', error);
  } finally {
    submitting.value = false;
  }
}

async function handleLikeComment(commentId: string) {
  try {
    await commentApi.likeComment(commentId);
  } catch (error) {
    console.error('点赞评论失败:', error);
  }
}

function toggleReplies(commentId: string) {
  if (expandedReplies.value.has(commentId)) {
    expandedReplies.value.delete(commentId);
  } else {
    expandedReplies.value.add(commentId);
  }
}

function loadMoreComments() {
  displayedCount.value += 10;
}

function getLikeButtonStyle() {
  if (isLiked.value) {
    return { backgroundColor: 'var(--theme-accent)', color: 'var(--theme-primary)' };
  }
  return { backgroundColor: 'var(--theme-surface)', color: 'var(--theme-text-secondary)' };
}

function getBookmarkButtonStyle() {
  if (isBookmarked.value) {
    return { backgroundColor: 'var(--theme-accent)', color: 'var(--theme-primary)' };
  }
  return { backgroundColor: 'var(--theme-surface)', color: 'var(--theme-text-secondary)' };
}

function getCommentLikeButtonStyle(isLiked: boolean) {
  if (isLiked) {
    return { color: 'var(--theme-primary)' };
  }
  return { color: 'var(--theme-text-secondary)' };
}

const head = useHead(
    computed(() => {
      if (!article.value) {
        return generateSeo({
          title: '文章详情',
          description: '阅读精彩文章',
          type: 'article'
        })
      }
      return generateSeo({
        title: article.value.title,
        description: article.value.excerpt,
        image: article.value.cover,
        type: 'article',
        keywords: article.value.tags,
        author: article.value.author?.username || article.value.authorUsername || '',
        publishedTime: article.value.createdAt,
        modifiedTime: article.value.updatedAt
      })
    })
)
</script>

<template>
  <div class="min-h-screen flex flex-col" style="background-color: var(--theme-bg);">

    <!-- Header - 与ListPage和SearchPage一致的面包屑结构 -->
    <div class="border-b py-3 sm:py-4" style="background-color: var(--theme-bg); border-color: var(--theme-border);">
      <div class="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8">
        <div class="flex items-center justify-between gap-4">
          <Breadcrumb :items="breadcrumbs" />
        </div>
      </div>
    </div>

    <!-- Loading State -->
    <div v-if="loading" class="flex-1 py-16">
      <div class="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8">
        <div class="text-center">
          <div class="inline-block w-12 h-12 border-4 border-t-4 border-gray-300 rounded-full animate-spin" style="border-top-color: var(--theme-primary);"></div>
          <p class="mt-4" style="color: var(--theme-text-secondary);">加载中...</p>
        </div>
      </div>
    </div>

    <!-- Error State -->
    <div v-else-if="error" class="flex-1 py-16">
      <div class="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8">
        <div class="text-center">
          <p class="text-lg mb-4" style="color: var(--theme-text);">{{ error }}</p>
          <button @click="loadArticle" class="px-6 py-2 rounded-lg font-medium transition-colors" style="background-color: var(--theme-primary); color: white;">
            重试
          </button>
        </div>
      </div>
    </div>

    <!-- 文章内容区域 - 与列表页保持一致的宽度和间距 -->
    <div v-else class="py-8 flex-1">
      <div class="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8">
        <!-- Article exists - show content -->
        <template v-if="article">
          <article
              class="rounded-xl mb-4 min-h-[400px] sm:min-h-[500px] md:min-h-[600px] w-full"
              style="background-color: var(--theme-surface); border: 1px solid var(--theme-border);"
              role="article"
              :aria-labelledby="'article-title-' + article.id"
          >
            <div class="p-4 sm:p-6 md:p-8 w-full">

              <div class="text-center mb-6">
                <h1
                    :id="'article-title-' + article.id"
                    class="text-xl md:text-2xl lg:text-3xl font-bold leading-tight"
                    style="color: var(--theme-text);"
                >
                  {{ article.title }}
                </h1>
              </div>

              <div class="flex items-center gap-4 py-4 border-b mb-6 overflow-x-auto justify-end" style="border-color: var(--theme-border);">
                <Link
                    v-if="articleAuthor"
                    :to="`/author/${articleAuthor.id || article?.authorId}`"
                    class="flex items-center gap-3 hover:opacity-80 transition-opacity flex-shrink-0"
                >
                  <img
                      :src="articleAuthor.avatar"
                      :alt="articleAuthor.username"
                      class="w-10 h-10 rounded-full"
                      loading="lazy"
                  />
                  <span class="font-medium text-base whitespace-nowrap" style="color: var(--theme-text);">{{ articleAuthor.nickname || articleAuthor.username }}</span>
                </Link>

                <div class="flex items-center gap-3 text-sm flex-shrink-0" style="color: var(--theme-text-secondary);">
                  <span>{{ articleDate }}</span>
                  <span>·</span>
                  <span>{{ articleViews }} 阅读</span>
                </div>

                <div v-if="articleTags.length > 0" class="flex items-center gap-2 flex-1 min-w-0 justify-end" role="list" aria-label="文章标签">
                <span
                    v-for="tag in articleTags"
                    :key="tag"
                    class="inline-flex px-3 py-1 text-sm rounded-full whitespace-nowrap"
                    style="background-color: var(--theme-accent); color: var(--theme-primary);"
                    role="listitem"
                >
                  {{ tag }}
                </span>
                </div>
              </div>

              <MarkdownRenderer
                  :content="article.content"
                  :content-markdown="article.contentMarkdown"
                  :editor-mode="article.editorMode"
              />

              <div class="flex items-center justify-between pt-3 mt-3 border-t gap-2" style="border-color: var(--theme-border);">
                <div class="flex items-center gap-2">
                  <button
                      @click="handleLike"
                      class="flex items-center gap-1.5 px-3 py-1.5 rounded-full transition-all focus:outline-none text-sm"
                      :style="getLikeButtonStyle()"
                      :aria-pressed="isLiked"
                      :aria-label="isLiked ? '取消点赞' : '点赞文章'"
                  >
                    <Heart class="w-4 h-4" :class="{ 'fill-current': isLiked }" aria-hidden="true" />
                    <span class="font-medium">{{ articleLikes }}</span>
                  </button>
                  <button
                      @click="handleBookmark"
                      class="flex items-center gap-1.5 px-3 py-1.5 rounded-full transition-all focus:outline-none text-sm"
                      :style="getBookmarkButtonStyle()"
                      :aria-pressed="isBookmarked"
                      :aria-label="isBookmarked ? '取消收藏' : '收藏文章'"
                  >
                    <Bookmark class="w-4 h-4" :class="{ 'fill-current': isBookmarked }" aria-hidden="true" />
                    <span class="font-medium">收藏</span>
                  </button>
                  <button
                      @click="handleShare"
                      class="flex items-center gap-1.5 px-3 py-1.5 rounded-full transition-colors focus:outline-none text-sm"
                      style="background-color: var(--theme-surface); color: var(--theme-text-secondary);"
                      :aria-label="'分享文章'"
                  >
                    <Share2 class="w-4 h-4" aria-hidden="true" />
                    <span class="font-medium">{{ articleShareCount }}</span>
                  </button>
                </div>
                <div class="text-xs" style="color: var(--theme-text-secondary);">
                  {{ articleUpdateDate }}
                </div>
              </div>
            </div>
          </article>

          <section class="rounded-xl p-4 sm:p-6 md:p-8 mb-4" style="background-color: var(--theme-surface); border: 1px solid var(--theme-border);" aria-labelledby="comments-heading">
            <h2 id="comments-heading" class="text-lg font-bold mb-6 flex items-center space-x-3" style="color: var(--theme-text);">
              <MessageSquare class="w-6 h-6" style="color: var(--theme-primary);" aria-hidden="true" />
              <span>评论 ({{ totalCommentsCount }})</span>
            </h2>

            <div v-if="currentUser" class="mb-6">
              <div class="flex gap-3">
                <img
                    :src="currentUser?.avatar"
                    :alt="currentUser?.username"
                    class="w-10 h-10 rounded-full flex-shrink-0"
                    loading="lazy"
                />
                <div class="flex-1 flex gap-3 items-end">
                  <label for="comment-input" class="sr-only">写下你的评论</label>
                  <textarea
                      id="comment-input"
                      v-model="newComment"
                      placeholder="写评论..."
                      class="flex-1 p-3 border rounded-xl text-base resize-none focus:outline-none focus:ring-2"
                      style="border-color: var(--theme-border); background-color: var(--theme-bg); color: var(--theme-text);"
                      rows="1"
                  />
                  <button
                      @click="handleSubmitComment"
                      :disabled="!newComment.trim() || submitting"
                      class="px-6 py-3 rounded-full font-medium text-sm transition-colors flex items-center gap-2 disabled:opacity-50 focus:outline-none flex-shrink-0"
                      style="background-color: var(--theme-primary); color: white;"
                  >
                    <Send class="w-4 h-4" aria-hidden="true" />
                    <span>发表评论</span>
                  </button>
                </div>
              </div>
            </div>
            <div v-else class="mb-8 p-6 rounded-xl text-center" style="background-color: var(--theme-accent);">
              <p class="text-base mb-4" style="color: var(--theme-text-secondary);">登录后发表评论</p>
              <Link
                  to="/login"
                  class="inline-flex items-center px-6 py-2 rounded-full font-medium text-base transition-colors focus:outline-none focus:ring-2 focus:ring-offset-2"
                  style="background-color: var(--theme-primary); color: white;"
              >
                立即登录
              </Link>
            </div>

            <div class="space-y-6" role="list" aria-label="评论列表">
              <div
                  v-for="comment in displayedComments"
                  :key="comment.id"
                  class="flex gap-3"
                  role="listitem"
              >
                <img
                    :src="comment.author?.avatar"
                    :alt="comment.author?.username"
                    class="w-10 h-10 rounded-full flex-shrink-0"
                    loading="lazy"
                />
                <div class="flex-1 min-w-0">
                  <div class="flex items-center gap-3 mb-2">
                    <span class="font-medium text-base" style="color: var(--theme-text);">{{ comment.author?.username || '匿名用户' }}</span>
                    <span class="text-sm" style="color: var(--theme-text-secondary);">{{ comment.createdAt }}</span>
                  </div>
                  <p class="text-base mb-3" style="color: var(--theme-text-secondary);">{{ comment.content }}</p>
                  <div class="flex items-center gap-4 mb-3">
                    <button
                        @click="handleLikeComment(comment.id)"
                        class="flex items-center gap-2 transition-colors focus:outline-none text-sm"
                        :style="getCommentLikeButtonStyle(!!comment.isLiked)"
                        :aria-label="comment.isLiked ? '取消点赞' : '点赞评论'"
                    >
                      <Heart class="w-4 h-4" :class="{ 'fill-current': comment.isLiked }" aria-hidden="true" />
                      <span>{{ comment.likeCount || 0 }}</span>
                    </button>
                    <button
                        @click="handleReply(comment)"
                        class="flex items-center gap-2 transition-colors focus:outline-none text-sm"
                        style="color: var(--theme-text-secondary);"
                        :aria-label="'回复评论'"
                    >
                      <Reply class="w-4 h-4" aria-hidden="true" />
                      <span>回复</span>
                    </button>
                    <button
                        v-if="comment.replies && comment.replies.length > 0"
                        @click="toggleReplies(comment.id)"
                        class="flex items-center gap-2 transition-colors focus:outline-none text-sm"
                        style="color: var(--theme-primary);"
                    >
                      <span>{{ expandedReplies.has(comment.id) ? '收起' : '展开' }} {{ comment.replies.length }} 回复</span>
                    </button>
                  </div>

                  <div v-if="replyingTo?.id === comment.id" class="mb-4 p-4 rounded-xl" style="background-color: var(--theme-bg);">
                    <div class="flex gap-3">
                      <img
                          :src="currentUser?.avatar"
                          :alt="currentUser?.username"
                          class="w-8 h-8 rounded-full flex-shrink-0"
                          loading="lazy"
                      />
                      <div class="flex-1">
                        <div class="text-sm mb-2" style="color: var(--theme-text-secondary);">回复 {{ comment.author?.username || '匿名用户' }}</div>
                        <textarea
                            v-model="replyContent"
                            placeholder="写回复..."
                            class="w-full p-3 border rounded-xl text-base resize-none focus:outline-none focus:ring-2"
                            style="border-color: var(--theme-border); background-color: var(--theme-bg); color: var(--theme-text);"
                            rows="2"
                        />
                        <div class="flex justify-end mt-3 gap-3">
                          <button
                              @click="handleCancelReply"
                              class="px-4 py-2 rounded-full font-medium text-sm transition-colors focus:outline-none"
                              style="color: var(--theme-text-secondary);"
                          >
                            取消
                          </button>
                          <button
                              @click="handleSubmitReply"
                              :disabled="!replyContent.trim() || submitting"
                              class="px-4 py-2 rounded-full font-medium text-sm transition-colors flex items-center gap-2 disabled:opacity-50 focus:outline-none"
                              style="background-color: var(--theme-primary); color: white;"
                          >
                            回复
                          </button>
                        </div>
                      </div>
                    </div>
                  </div>

                  <div v-if="comment.replies && comment.replies.length > 0 && expandedReplies.has(comment.id)" class="space-y-4 ml-0 mt-4">
                    <div
                        v-for="reply in comment.replies"
                        :key="reply.id"
                        class="flex gap-3"
                    >
                      <div class="w-1 rounded-full ml-5 mt-2" style="background-color: var(--theme-border);"></div>
                      <img
                          :src="reply.author?.avatar"
                          :alt="reply.author?.username"
                          class="w-8 h-8 rounded-full flex-shrink-0"
                          loading="lazy"
                      />
                      <div class="flex-1 min-w-0">
                        <div class="flex items-center gap-3 mb-2">
                          <span class="font-medium text-sm" style="color: var(--theme-text);">{{ reply.author?.username || '匿名用户' }}</span>
                          <span class="text-sm" style="color: var(--theme-text-secondary);">{{ reply.createdAt }}</span>
                        </div>
                        <p class="text-sm mb-3" style="color: var(--theme-text-secondary);">{{ reply.content }}</p>
                        <div class="flex items-center gap-3">
                          <button
                              @click="handleLikeComment(reply.id)"
                              class="flex items-center gap-2 transition-colors focus:outline-none text-sm"
                              :style="getCommentLikeButtonStyle(!!reply.isLiked)"
                          >
                            <Heart class="w-4 h-4" :class="{ 'fill-current': reply.isLiked }" aria-hidden="true" />
                            <span>{{ reply.likeCount || 0 }}</span>
                          </button>
                          <button
                              @click="handleReply(comment)"
                              class="flex items-center gap-2 transition-colors focus:outline-none text-sm"
                              style="color: var(--theme-text-secondary);"
                          >
                            <Reply class="w-4 h-4" aria-hidden="true" />
                            <span>回复</span>
                          </button>
                        </div>
                      </div>
                    </div>
                  </div>
                </div>
              </div>
            </div>

            <div v-if="hasMoreComments" class="text-center mt-6">
              <button
                  @click="loadMoreComments"
                  class="px-6 py-2 rounded-full font-medium text-sm transition-colors focus:outline-none"
                  style="background-color: var(--theme-surface); color: var(--theme-text-secondary);"
              >
                查看更多评论 ({{ comments.length - displayedCount }} 条)
              </button>
            </div>
          </section>
        </template>

        <!-- No data state -->
        <template v-else>
          <div class="text-center py-16">
            <p class="text-lg" style="color: var(--theme-text-secondary);">暂无数据</p>
          </div>
        </template>

      </div>
    </div>

    <!-- 公共Footer组件 -->
    <div class="mt-8 sm:mt-10 lg:mt-12">
      <SiteFooter />
    </div>
  </div>
</template>
