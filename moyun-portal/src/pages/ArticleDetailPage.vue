<script setup lang="ts">
import {computed, onMounted, ref} from 'vue';
import {RouterLink as Link, useRoute, useRouter} from 'vue-router';
import {useHead} from '@vueuse/head';
import {Bookmark, Clock, Eye, Heart, MessageSquare, Reply, Send, Share2, Tag} from 'lucide-vue-next';
import Breadcrumb from '@/components/Breadcrumb.vue';
import SiteFooter from '@/components/SiteFooter.vue';
import MarkdownRenderer from '@/components/MarkdownRenderer.vue';
import BackToTop from '@/components/BackToTop.vue';
import TagList from '@/components/TagList.vue';
import {useArticleStore} from '@/stores/article';
import {useUserStore} from '@/stores/user';
import {useAuth} from '@/composables/useAuth';
import {useToast} from '@/composables/useToast';
import {generateSeo, generateArticleJsonLd} from '@/utils/seo';
import {sanitizeHTML} from '@/utils/security';
import {formatShortDate} from '@/utils/date';
import {getSafeAvatar} from '@/utils/avatar';
import type {Article, Comment, User} from '@/types/api';
import * as articleApi from '@/api/article';
import * as commentApi from '@/api/comment';

const route = useRoute();
const router = useRouter();
const articleStore = useArticleStore();
const userStore = useUserStore();
const {isAuthenticated, requireAuth, withAuth, withAuthConfirm} = useAuth();
const toast = useToast();
const article = ref<Article | null>(null);
const comments = ref<Comment[]>([]);
const newComment = ref('');
const currentUser = computed(() => userStore.user);
const replyingTo = ref<Comment | null>(null);
const replyingRootComment = ref<Comment | null>(null); // 保存根评论用于回复
const replyingToRoot = ref(false); // 是否是直接回复根评论
const replyContent = ref('');
// 后端已实现分页，无需本地分页变量
const loading = ref(false);
const commentsLoading = ref(false); // 评论加载状态
const commentsPageNum = ref(1); // 当前页码
const commentsHasMore = ref(false); // 是否有更多评论
const submitting = ref(false);
const error = ref<string | null>(null);
const isShareMenuOpen = ref(false); // 分享菜单是否展开

// 检查是否支持原生分享
const supportsNativeShare = computed(() => {
  return typeof navigator !== 'undefined' && typeof navigator.share === 'function';
});

const isLiked = computed(() => article.value ? articleStore.likedArticleIds.includes(article.value.id) : false);
const isBookmarked = computed(() => article.value ? articleStore.bookmarkedArticleIds.includes(article.value.id) : false);

// 获取评论作者的显示名称
function getCommentAuthorName(comment: Comment): string {
  return comment.authorNickname || comment.authorUsername || comment.author?.nickname || comment.author?.username || '匿名用户';
}

// 获取评论作者的头像
function getCommentAuthorAvatar(comment: Comment): string {
  const authorId = comment.authorId || comment.author?.id || '';
  return getSafeAvatar(comment.authorAvatar || comment.author?.avatar, authorId);
}

// 获取被回复用户的显示名称
function getReplyToName(comment: Comment): string {
  return comment.replyToNickname || comment.replyToUsername || '';
}

// 获取文章作者信息
const articleAuthor = computed(() => {
  if (!article.value) return null;
  if (article.value.author) return article.value.author;
  // 如果没有 author 对象，从其他字段构造
  return {
    id: article.value.authorId || '',
    username: article.value.authorUsername || '',
    nickname: article.value.authorNickname || article.value.authorUsername || '',
    avatar: getSafeAvatar(article.value.authorAvatar, article.value.authorId || ''),
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
    {label: articleCategory.value, path: `/list?category=${articleCategory.value}`},
    {label: article.value.title}
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

const displayedComments = computed(() => comments.value); // 后端已分页，直接使用所有评论
const hasMoreComments = computed(() => commentsHasMore.value); // 使用后端返回的 hasMore

// 审核状态进度条
const auditStep = computed(() => {
  const s = article.value?.status;
  if (s === 'draft') return 1;
  if (s === 'pending') return 2;
  if (s === 'rejected') return 2; // 驳回仍停留在审核步骤
  if (s === 'published') return 3;
  return 1;
});
const auditStatusText = computed(() => {
  const s = article.value?.status;
  if (s === 'draft') return '草稿未提交';
  if (s === 'pending') return '审核中，请耐心等待';
  if (s === 'rejected') return '审核未通过';
  if (s === 'archived') return '已归档';
  return '';
});
const auditStatusColor = computed(() => {
  const s = article.value?.status;
  if (s === 'rejected') return '#dc2626';
  if (s === 'pending') return '#d97706';
  if (s === 'draft') return 'var(--theme-text-secondary)';
  return 'var(--theme-primary)';
});
const auditStatusBarStyle = computed(() => {
  const s = article.value?.status;
  if (s === 'rejected') return { backgroundColor: '#fef2f2', borderLeft: '4px solid #dc2626' };
  if (s === 'pending') return { backgroundColor: '#fffbeb', borderLeft: '4px solid #d97706' };
  return { backgroundColor: 'var(--theme-accent)', borderLeft: '4px solid var(--theme-primary)' };
});

const totalCommentsCount = computed(() => {
  let count = 0;
  const countReplies = (cmts: Comment[]) => {
    cmts.forEach(c => {
      count++;
      if (c.replies && c.replies.length > 0) {
        count += c.replies.length;
      }
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

    // 三个独立请求并行：详情、评论、阅读量
    const [response, , viewResponse] = await Promise.all([
      articleApi.getArticleDetail({id: articleId}),
      loadComments(articleId),
      // 增加阅读量（支持防刷逻辑），失败不应阻断详情加载
      articleApi.incrementView(articleId).catch((viewError) => {
        console.error('增加阅读量失败:', viewError);
        return null;
      }),
    ]);

    if (response.code === 200) {
      article.value = response.data as Article;
    } else {
      error.value = response.message || '加载文章失败';
    }

    // 更新文章阅读量
    if (viewResponse && viewResponse.code === 200 && viewResponse.data && article.value) {
      article.value.views = viewResponse.data.views;
    }
  } catch (err) {
    console.error('加载文章失败:', err);
    error.value = '加载文章失败，请稍后重试';
  } finally {
    loading.value = false;
  }
}

async function loadComments(articleId: string) {
  try {
    commentsLoading.value = true;
    const response = await commentApi.getArticleComments(articleId, commentsPageNum.value, 20);
    if (response.code === 200 && response.data) {
      const result = response.data;
      // 确保每个评论都有完整的结构
      const commentList = (result.list || []) as Comment[];
      comments.value = commentList.map(comment => {
        if (!comment.replies) {
          comment.replies = [];
        }
        return comment;
      });
      // 更新分页信息
      commentsHasMore.value = result.hasMore || false;
    }
  } catch (error) {
    console.error('加载评论失败:', error);
    comments.value = [];
  } finally {
    commentsLoading.value = false;
  }
}

async function handleLike() {
  if (!article.value) return;
  // 使用 withAuthConfirm 包装，未登录时弹出确认框
  await withAuthConfirm(async () => {
    const result = await articleStore.likeArticleWithApi(article.value.id);
    // 如果点赞成功，更新文章的点赞数和状态
    if (result.success && article.value) {
      article.value.likes = result.likeCount;
    }
  }, '点赞');
}

async function handleBookmark() {
  if (!article.value) return;
  // 使用 withAuthConfirm 包装，未登录时弹出确认框
  await withAuthConfirm(async () => {
    const result = await articleStore.bookmarkArticleWithApi(article.value.id);
    // 收藏成功后，前端状态会自动同步到 store
  }, '收藏');
}

function handleShare() {
  if (!article.value) return;
  articleStore.shareArticle(article.value.id);
  isShareMenuOpen.value = !isShareMenuOpen.value;
}

// 获取分享链接
function getShareUrl(): string {
  return window.location.href;
}

// 复制链接到剪贴板
async function copyLink() {
  const url = getShareUrl();
  try {
    await navigator.clipboard.writeText(url);
    toast.success('链接已复制到剪贴板');
  } catch (err) {
    // 降级处理：使用传统方法
    const textArea = document.createElement('textarea');
    textArea.value = url;
    textArea.style.position = 'fixed';
    textArea.style.left = '-999999px';
    document.body.appendChild(textArea);
    textArea.select();
    try {
      document.execCommand('copy');
      toast.success('链接已复制到剪贴板');
    } catch (e) {
      toast.error('复制失败，请手动复制链接');
    }
    document.body.removeChild(textArea);
  }
  isShareMenuOpen.value = false;
}

// 分享到微信好友
function shareToWechat() {
  // 判断是否在微信内打开
  if (isWechatBrowser()) {
    // 微信内打开，提示用户使用微信自带分享功能
    toast.info('请点击右上角"..."，选择"发送给朋友"或"分享到朋友圈"', 4000);
  } else {
    // 非微信浏览器，复制链接并提示
    copyLinkAndNotify('微信');
  }
  isShareMenuOpen.value = false;
}

// 分享到微信朋友圈
function shareToWechatMoments() {
  // 判断是否在微信内打开
  if (isWechatBrowser()) {
    // 微信内打开，提示用户使用微信自带分享功能
    toast.info('请点击右上角"..."，选择"分享到朋友圈"', 4000);
  } else {
    // 非微信浏览器，复制链接并提示
    copyLinkAndNotify('微信朋友圈');
  }
  isShareMenuOpen.value = false;
}

// 判断是否在微信浏览器中
function isWechatBrowser(): boolean {
  const ua = navigator.userAgent.toLowerCase();
  return ua.includes('micromessenger');
}

// 复制链接并提示分享到指定平台
function copyLinkAndNotify(platform: string) {
  const url = getShareUrl();
  try {
    navigator.clipboard.writeText(url);
    toast.success(`链接已复制到剪贴板，请打开${platform}粘贴分享`);
  } catch (err) {
    const textArea = document.createElement('textarea');
    textArea.value = url;
    textArea.style.position = 'fixed';
    textArea.style.left = '-999999px';
    document.body.appendChild(textArea);
    textArea.select();
    try {
      document.execCommand('copy');
      toast.success(`链接已复制到剪贴板，请打开${platform}粘贴分享`);
    } catch (e) {
      toast.error(`复制失败，请手动复制链接`);
    }
    document.body.removeChild(textArea);
  }
}

// 分享到微博
function shareToWeibo() {
  if (!article.value) return;
  const url = `https://service.weibo.com/share/share.php?title=${encodeURIComponent(article.value.title + ' - ' + articleExcerpt.value)}&url=${encodeURIComponent(getShareUrl())}`;
  window.open(url, '_blank', 'width=600,height=500');
  isShareMenuOpen.value = false;
}

// 分享到QQ
function shareToQQ() {
  if (!article.value) return;
  const url = `https://connect.qq.com/widget/shareqq/index.html?title=${encodeURIComponent(article.value.title)}&desc=${encodeURIComponent(articleExcerpt.value)}&url=${encodeURIComponent(getShareUrl())}`;
  window.open(url, '_blank', 'width=600,height=500');
  isShareMenuOpen.value = false;
}

// 分享到QQ空间
function shareToQzone() {
  if (!article.value) return;
  const url = `https://sns.qzone.qq.com/cgi-bin/qzshare/cgi_qzshare_onekey?title=${encodeURIComponent(article.value.title)}&desc=${encodeURIComponent(articleExcerpt.value)}&url=${encodeURIComponent(getShareUrl())}`;
  window.open(url, '_blank', 'width=600,height=500');
  isShareMenuOpen.value = false;
}

// 原生分享（移动端）
async function nativeShare() {
  if (supportsNativeShare.value) {
    try {
      await navigator.share({
        title: article.value.title,
        text: articleExcerpt.value,
        url: getShareUrl(),
      });
    } catch (err) {
      // 用户取消分享不报错
      if ((err as Error).name !== 'AbortError') {
        console.error('分享失败:', err);
      }
    }
  } else {
    toast.warning('您的浏览器不支持原生分享功能');
  }
  isShareMenuOpen.value = false;
}

// 点击其他地方关闭分享菜单
function handleClickOutside(event: MouseEvent) {
  const target = event.target as HTMLElement;
  if (!target.closest('.share-menu-container')) {
    isShareMenuOpen.value = false;
  }
}

async function handleSubmitComment() {
  if (!newComment.value.trim() || !article.value) return;

  // 使用 withAuthConfirm 包装，未登录时弹出确认框
  await withAuthConfirm(async () => {
    submitting.value = true;
    try {
      const response = await commentApi.addComment({
        articleId: article.value.id,
        content: newComment.value
      });

      if (response.code === 200) {
        // 发表评论成功后，重新加载评论列表（重置到第一页）
        commentsPageNum.value = 1;
        comments.value = []; // 清空旧评论
        await loadComments(article.value.id);
        newComment.value = '';
      }
    } catch (error) {
      console.error('发表评论失败:', error);
    } finally {
      submitting.value = false;
    }
  }, '发表评论');
}

async function handleReply(rootComment: Comment, replyComment?: Comment) {
  // 使用 withAuthConfirm 包装，未登录时弹出确认框
  await withAuthConfirm(() => {
    replyingRootComment.value = rootComment;
    replyingTo.value = replyComment || rootComment;
    replyingToRoot.value = !replyComment; // 没有第二个参数就是直接回复根评论
    replyContent.value = '';
    return Promise.resolve(null);
  }, '回复评论');
}

function handleCancelReply() {
  replyingTo.value = null;
  replyingRootComment.value = null;
  replyingToRoot.value = false;
  replyContent.value = '';
}

async function handleSubmitReply() {
  if (!replyContent.value.trim() || !currentUser.value || !article.value || !replyingRootComment.value) return;

  submitting.value = true;
  try {
    const response = await commentApi.addComment({
      articleId: article.value.id,
      content: replyContent.value,
      parentId: replyingRootComment.value.id // 始终回复到根评论
    });

    if (response.code === 200) {
      // 回复成功后，重新加载评论列表（重置到第一页）
      commentsPageNum.value = 1;
      comments.value = []; // 清空旧评论
      await loadComments(article.value.id);
      handleCancelReply();
    }
  } catch (error) {
    console.error('发表回复失败:', error);
  } finally {
    submitting.value = false;
  }
}

async function handleLikeComment(commentId: string) {
  // 使用 withAuthConfirm 包装，未登录时弹出确认框
  await withAuthConfirm(async () => {
    try {
      await commentApi.likeComment(commentId);
    } catch (error) {
      console.error('点赞评论失败:', error);
    }
  }, '点赞评论');
}

async function loadMoreComments() {
  if (commentsLoading.value || !commentsHasMore.value) return;
  
  try {
    commentsLoading.value = true;
    commentsPageNum.value += 1;
    const articleId = route.params.id as string;
    const response = await commentApi.getArticleComments(articleId, commentsPageNum.value, 20);
    
    if (response.code === 200 && response.data) {
      const result = response.data;
      const newComments = (result.list || []) as Comment[];
      // 添加新评论，确保结构完整
      const processedComments = newComments.map(comment => {
        if (!comment.replies) {
          comment.replies = [];
        }
        return comment;
      });
      comments.value = [...comments.value, ...processedComments];
      // 更新分页信息
      commentsHasMore.value = result.hasMore || false;
    }
  } catch (error) {
    console.error('加载更多评论失败:', error);
    commentsPageNum.value -= 1; // 加载失败，恢复页码
  } finally {
    commentsLoading.value = false;
  }
}

function getLikeButtonStyle() {
  if (isLiked.value) {
    return {backgroundColor: 'var(--theme-accent)', color: 'var(--theme-primary)'};
  }
  return {backgroundColor: 'var(--theme-surface)', color: 'var(--theme-text-secondary)'};
}

function getBookmarkButtonStyle() {
  if (isBookmarked.value) {
    return {backgroundColor: 'var(--theme-accent)', color: 'var(--theme-primary)'};
  }
  return {backgroundColor: 'var(--theme-surface)', color: 'var(--theme-text-secondary)'};
}

function getCommentLikeButtonStyle(isLiked: boolean) {
  if (isLiked) {
    return {color: 'var(--theme-primary)'};
  }
  return {color: 'var(--theme-text-secondary)'};
}

const head = useHead(
    computed(() => {
      if (!article.value) {
        return generateSeo({
          title: '文章详情',
          description: '阅读精彩文章',
          type: 'article',
          canonicalPath: '/article'
        })
      }
      const canonicalPath = `/article/${article.value.id}`
      return generateSeo({
        title: article.value.title,
        description: article.value.excerpt,
        image: article.value.cover,
        type: 'article',
        keywords: article.value.tags,
        author: article.value.author?.username || article.value.authorUsername || '',
        publishedTime: article.value.createdAt,
        modifiedTime: article.value.updatedAt,
        canonicalPath,
        jsonLd: generateArticleJsonLd({
          title: article.value.title,
          description: article.value.excerpt,
          image: article.value.cover,
          url: canonicalPath,
          author: article.value.author?.username || article.value.authorUsername || '',
          publishedTime: article.value.createdAt,
          modifiedTime: article.value.updatedAt
        })
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
          <Breadcrumb :items="breadcrumbs"/>
        </div>
      </div>
    </div>

    <!-- Loading State -->
    <div v-if="loading" class="flex-1 py-16">
      <div class="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8">
        <div class="text-center">
          <div class="inline-block w-12 h-12 border-4 border-t-4 border-gray-300 rounded-full animate-spin"
               style="border-top-color: var(--theme-primary);"></div>
          <p class="mt-4" style="color: var(--theme-text-secondary);">加载中...</p>
        </div>
      </div>
    </div>

    <!-- Error State -->
    <div v-else-if="error" class="flex-1 py-16">
      <div class="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8">
        <div class="text-center">
          <p class="text-lg mb-4" style="color: var(--theme-text);">{{ error }}</p>
          <button @click="loadArticle" class="px-6 py-2 rounded-lg font-medium transition-colors"
                  style="background-color: var(--theme-primary); color: white;">
            重试
          </button>
        </div>
      </div>
    </div>

    <!-- 文章内容区域 -->
    <div v-else class="py-6 flex-1">
      <div class="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8">
        <!-- Article exists - show content -->
        <template v-if="article">
          <article
              class="rounded-xl mb-4 w-full"
              style="background-color: var(--theme-surface); border: 1px solid var(--theme-border);"
              role="article"
              :aria-labelledby="'article-title-' + article.id"
          >
            <div class="p-4 sm:p-6 md:p-8 w-full flex flex-col">

              <!-- 审核状态进度条（仅非 published 状态显示） -->
              <div v-if="article.status && article.status !== 'published'" class="mb-4 p-4 rounded-lg" :style="auditStatusBarStyle">
                <div class="flex items-center gap-3 mb-2">
                  <Clock class="w-5 h-5 flex-shrink-0" :style="{ color: auditStatusColor }" />
                  <span class="font-medium" :style="{ color: auditStatusColor }">{{ auditStatusText }}</span>
                </div>
                <!-- 进度条 -->
                <div class="flex items-center gap-2 text-xs">
                  <div class="flex-1 flex items-center gap-1">
                    <div class="w-6 h-6 rounded-full flex items-center justify-center text-white text-xs" style="background-color: var(--theme-primary);">1</div>
                    <span style="color: var(--theme-text-secondary);">撰写</span>
                  </div>
                  <div class="flex-1 h-0.5" :style="{ backgroundColor: auditStep >= 2 ? 'var(--theme-primary)' : 'var(--theme-border)' }"></div>
                  <div class="flex-1 flex items-center gap-1">
                    <div class="w-6 h-6 rounded-full flex items-center justify-center text-xs" :style="{ backgroundColor: auditStep >= 2 ? 'var(--theme-primary)' : 'var(--theme-border)', color: auditStep >= 2 ? 'white' : 'var(--theme-text-secondary)' }">2</div>
                    <span :style="{ color: auditStep >= 2 ? 'var(--theme-text)' : 'var(--theme-text-secondary)' }">审核中</span>
                  </div>
                  <div class="flex-1 h-0.5" :style="{ backgroundColor: auditStep >= 3 ? 'var(--theme-primary)' : 'var(--theme-border)' }"></div>
                  <div class="flex-1 flex items-center gap-1">
                    <div class="w-6 h-6 rounded-full flex items-center justify-center text-xs" :style="{ backgroundColor: auditStep >= 3 ? 'var(--theme-primary)' : 'var(--theme-border)', color: auditStep >= 3 ? 'white' : 'var(--theme-text-secondary)' }">3</div>
                    <span :style="{ color: auditStep >= 3 ? 'var(--theme-text)' : 'var(--theme-text-secondary)' }">已发布</span>
                  </div>
                </div>
                <p v-if="article.status === 'rejected' && article.remark" class="mt-3 text-sm" style="color: var(--theme-text-secondary);">
                  审核意见：{{ article.remark }}
                </p>
                <p v-if="article.status === 'rejected'" class="mt-2">
                  <Link to="/publish" class="text-sm font-medium" style="color: var(--theme-primary);">去修改并重新提交 →</Link>
                </p>
              </div>

              <!-- 标题区域 -->
              <div class="text-center mb-3">
                <h1
                    :id="'article-title-' + article.id"
                    class="text-xl md:text-2xl lg:text-3xl font-bold leading-tight"
                    style="color: var(--theme-text);"
                >
                  {{ article.title }}
                </h1>
              </div>

              <!-- 文章信息行 - 左边信息，右边标签 -->
              <div class="flex items-center justify-between py-4 mb-6 border-t border-b flex-wrap gap-4"
                   style="border-color: var(--theme-border);">
                <!-- 左边：发布人、时间、阅读量 -->
                <div class="flex items-center gap-6">
                  <!-- 作者信息 - 可点击跳转作者中心（仅当 authorId 有效时） -->
                  <Link
                      v-if="articleAuthor && (articleAuthor.id || article?.authorId)"
                      :to="`/author/${articleAuthor.id || article?.authorId}`"
                      class="flex items-center gap-3 hover:opacity-80 transition-opacity"
                  >
                    <img
                        :src="getSafeAvatar(articleAuthor.avatar, articleAuthor.id)"
                        :alt="articleAuthor.username"
                        class="w-10 h-10 rounded-full"
                        loading="lazy"
                        @error="(e: Event) => (e.target as HTMLImageElement).src = getSafeAvatar(null, articleAuthor.id)"
                    />
                    <span class="font-medium text-base" style="color: var(--theme-text);">
                      {{ articleAuthor.nickname || articleAuthor.username || '匿名作者' }}
                    </span>
                  </Link>
                  <!-- 作者信息 - 无 authorId 时仅展示不可点击 -->
                  <div v-else-if="articleAuthor" class="flex items-center gap-3">
                    <img
                        :src="getSafeAvatar(articleAuthor.avatar, articleAuthor.id)"
                        :alt="articleAuthor.username"
                        class="w-10 h-10 rounded-full"
                        loading="lazy"
                    />
                    <span class="font-medium text-base" style="color: var(--theme-text);">
                      {{ articleAuthor.nickname || articleAuthor.username || '匿名作者' }}
                    </span>
                  </div>

                  <!-- 时间 -->
                  <span class="text-base" style="color: var(--theme-text-secondary);">
                    {{ articleDate }}
                  </span>

                  <!-- 阅读量 -->
                  <span class="text-base" style="color: var(--theme-text-secondary);">
                    {{ articleViews }} 阅读
                  </span>
                </div>

                <!-- 右边：标签列表 -->
                <TagList v-if="articleTags.length > 0" :tags="articleTags" :max-visible="3" />
              </div>

              <!-- 内容区域 - 自适应 -->
              <div class="flex-1 py-6">
                <MarkdownRenderer
                    :content="article.content"
                    :content-markdown="article.contentMarkdown"
                    :editor-mode="article.editorMode"
                />
              </div>

              <!-- 互动区域 -->
              <div class="flex items-center justify-center pt-4 mt-auto border-t flex-wrap"
                   style="border-color: var(--theme-border);">
                <div class="flex items-center gap-4">
                  <button
                      @click="handleLike"
                      class="flex items-center gap-2 px-4 py-2 rounded-full transition-all hover:scale-105 focus:outline-none"
                      :style="getLikeButtonStyle()"
                      :aria-pressed="isLiked"
                      :aria-label="isLiked ? '取消点赞' : '点赞文章'"
                  >
                    <Heart class="w-5 h-5 transition-transform" :class="{ 'fill-current': isLiked }"
                           aria-hidden="true"/>
                    <span class="font-medium text-sm">{{ articleLikes }}</span>
                  </button>
                  <button
                      @click="handleBookmark"
                      class="flex items-center gap-2 px-4 py-2 rounded-full transition-all hover:scale-105 focus:outline-none"
                      :style="getBookmarkButtonStyle()"
                      :aria-pressed="isBookmarked"
                      :aria-label="isBookmarked ? '取消收藏' : '收藏文章'"
                  >
                    <Bookmark class="w-5 h-5 transition-transform" :class="{ 'fill-current': isBookmarked }"
                              aria-hidden="true"/>
                    <span class="font-medium text-sm">收藏</span>
                  </button>
                  <button
                      @click="handleShare"
                      class="flex items-center gap-2 px-4 py-2 rounded-full transition-all hover:scale-105 focus:outline-none relative share-menu-container"
                      style="background-color: var(--theme-surface); color: var(--theme-text-secondary);"
                      :aria-label="'分享文章'"
                      @blur="handleClickOutside"
                  >
                    <Share2 class="w-5 h-5 transition-transform" aria-hidden="true"/>
                    <span class="font-medium text-sm">{{ articleShareCount }}</span>
                    <!-- 分享菜单 -->
                    <div
                        v-if="isShareMenuOpen"
                        class="absolute right-0 top-full mt-2 w-56 rounded-xl shadow-xl border py-2 z-50"
                        style="background-color: var(--theme-bg); border-color: var(--theme-border);"
                        @click.stop
                    >
                      <!-- 复制链接 -->
                      <button
                          @click="copyLink"
                          class="w-full flex items-center space-x-3 px-4 py-3 text-left transition-colors hover:opacity-80"
                          :style="{ backgroundColor: 'var(--theme-surface)' }"
                      >
                        <span class="text-lg">🔗</span>
                        <span class="text-sm" style="color: var(--theme-text);">复制链接</span>
                      </button>
                      <!-- 原生分享（移动端） -->
                      <button
                          v-if="supportsNativeShare"
                          @click="nativeShare"
                          class="w-full flex items-center space-x-3 px-4 py-3 text-left transition-colors hover:opacity-80"
                      >
                        <span class="text-lg">📱</span>
                        <span class="text-sm" style="color: var(--theme-text);">原生分享</span>
                      </button>
                      <div class="border-t my-1" style="border-color: var(--theme-border);"></div>
                      <!-- 分享到微信好友 -->
                      <button
                          @click="shareToWechat"
                          class="w-full flex items-center space-x-3 px-4 py-3 text-left transition-colors hover:opacity-80"
                      >
                        <span class="text-lg">💬</span>
                        <span class="text-sm" style="color: var(--theme-text);">微信好友</span>
                      </button>
                      <!-- 分享到微信朋友圈 -->
                      <button
                          @click="shareToWechatMoments"
                          class="w-full flex items-center space-x-3 px-4 py-3 text-left transition-colors hover:opacity-80"
                      >
                        <span class="text-lg">📷</span>
                        <span class="text-sm" style="color: var(--theme-text);">微信朋友圈</span>
                      </button>
                      <!-- 分享到微博 -->
                      <button
                          @click="shareToWeibo"
                          class="w-full flex items-center space-x-3 px-4 py-3 text-left transition-colors hover:opacity-80"
                      >
                        <span class="text-lg">📝</span>
                        <span class="text-sm" style="color: var(--theme-text);">分享到微博</span>
                      </button>
                      <!-- 分享到QQ -->
                      <button
                          @click="shareToQQ"
                          class="w-full flex items-center space-x-3 px-4 py-3 text-left transition-colors hover:opacity-80"
                      >
                        <span class="text-lg">💬</span>
                        <span class="text-sm" style="color: var(--theme-text);">分享到QQ</span>
                      </button>
                      <!-- 分享到QQ空间 -->
                      <button
                          @click="shareToQzone"
                          class="w-full flex items-center space-x-3 px-4 py-3 text-left transition-colors hover:opacity-80"
                      >
                        <span class="text-lg">🌐</span>
                        <span class="text-sm" style="color: var(--theme-text);">分享到QQ空间</span>
                      </button>
                    </div>
                  </button>
                </div>
              </div>
            </div>
          </article>

          <section class="rounded-xl p-4 sm:p-6 md:p-8 mb-4"
                   style="background-color: var(--theme-surface); border: 1px solid var(--theme-border);"
                   aria-labelledby="comments-heading">
            <h2 id="comments-heading" class="text-lg font-bold mb-4 flex items-center space-x-3"
                style="color: var(--theme-text);">
              <MessageSquare class="w-6 h-6" style="color: var(--theme-primary);" aria-hidden="true"/>
              <span>评论 ({{ totalCommentsCount }})</span>
            </h2>

            <div v-if="currentUser" class="mb-6">
              <div class="flex gap-3">
                <img
                    :src="getSafeAvatar(currentUser?.avatar, currentUser?.id)"
                    :alt="currentUser?.username"
                    class="w-10 h-10 rounded-full flex-shrink-0"
                    loading="lazy"
                    @error="(e: Event) => (e.target as HTMLImageElement).src = getSafeAvatar(null, currentUser?.id)"
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
                    <Send class="w-4 h-4" aria-hidden="true"/>
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
                  v-for="rootComment in displayedComments"
                  :key="rootComment.id"
                  class="comment-root"
                  role="listitem"
              >
                <!-- 一级评论 -->
                <div class="flex gap-3">
                  <img
                      :src="getCommentAuthorAvatar(rootComment)"
                      :alt="getCommentAuthorName(rootComment)"
                      class="w-10 h-10 rounded-full flex-shrink-0"
                      loading="lazy"
                  />
                  <div class="flex-1 min-w-0">
                    <div class="flex items-center gap-3 mb-2">
                      <span class="font-medium text-base"
                            style="color: var(--theme-text);">{{ getCommentAuthorName(rootComment) }}</span>
                      <span class="text-sm" style="color: var(--theme-text-secondary);">
                        {{ formatShortDate(rootComment.createTime || rootComment.createdAt || '') }}
                      </span>
                    </div>
                    <p class="text-base mb-3" style="color: var(--theme-text);">{{ rootComment.content }}</p>
                    <div class="flex items-center gap-4 mb-3">
                      <button
                          @click="handleLikeComment(rootComment.id)"
                          class="flex items-center gap-2 transition-colors focus:outline-none text-sm"
                          :style="getCommentLikeButtonStyle(!!rootComment.isLiked)"
                          :aria-label="rootComment.isLiked ? '取消点赞' : '点赞评论'"
                      >
                        <Heart class="w-4 h-4" :class="{ 'fill-current': rootComment.isLiked }" aria-hidden="true"/>
                        <span>{{ rootComment.likeCount || 0 }}</span>
                      </button>
                      <button
                          @click="handleReply(rootComment)"
                          class="flex items-center gap-2 transition-colors focus:outline-none text-sm"
                          style="color: var(--theme-text-secondary);"
                          :aria-label="'回复评论'"
                      >
                        <Reply class="w-4 h-4" aria-hidden="true"/>
                        <span>回复</span>
                      </button>
                    </div>
                  </div>
                </div>

                <!-- 回复输入框 -->
                <div v-if="replyingTo?.id === rootComment.id && replyingToRoot" class="mt-4 mb-4 ml-13 p-4 rounded-xl"
                     style="background-color: var(--theme-bg);">
                  <div class="flex gap-3">
                    <img
                        :src="getSafeAvatar(currentUser?.avatar, currentUser?.id)"
                        :alt="currentUser?.username"
                        class="w-8 h-8 rounded-full flex-shrink-0"
                        loading="lazy"
                        @error="(e: Event) => (e.target as HTMLImageElement).src = getSafeAvatar(null, currentUser?.id)"
                    />
                    <div class="flex-1">
                      <div class="text-sm mb-2" style="color: var(--theme-text-secondary);">回复
                        {{ getCommentAuthorName(rootComment) }}
                      </div>
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

                <!-- 回复列表 -->
                <div v-if="rootComment.replies && rootComment.replies.length > 0" class="reply-list mt-4">
                  <div
                      v-for="reply in rootComment.replies"
                      :key="reply.id"
                      class="reply-item"
                  >
                    <div class="flex gap-3">
                      <div class="w-1 rounded-full ml-5 mt-2 flex-shrink-0"
                           style="background-color: var(--theme-border);"></div>
                      <img
                          :src="getCommentAuthorAvatar(reply)"
                          :alt="getCommentAuthorName(reply)"
                          class="w-8 h-8 rounded-full flex-shrink-0"
                          loading="lazy"
                      />
                      <div class="flex-1 min-w-0">
                        <div class="flex items-center gap-3 mb-2 flex-wrap">
                          <span class="font-medium text-sm"
                                style="color: var(--theme-text);">{{ getCommentAuthorName(reply) }}</span>
                          <span v-if="getReplyToName(reply)" class="text-sm reply-to"
                                style="color: var(--theme-primary);">
                            回复 @{{ getReplyToName(reply) }}
                          </span>
                          <span class="text-sm" style="color: var(--theme-text-secondary);">
                            {{ formatShortDate(reply.createTime || reply.createdAt || '') }}
                          </span>
                        </div>
                        <!-- 引用的内容 -->
                        <div v-if="reply.replyToContent" class="reply-quote mb-2">
                          引用: {{ reply.replyToContent }}
                        </div>
                        <p class="text-sm mb-3" style="color: var(--theme-text);">{{ reply.content }}</p>
                        <div class="flex items-center gap-3">
                          <button
                              @click="handleLikeComment(reply.id)"
                              class="flex items-center gap-2 transition-colors focus:outline-none text-sm"
                              :style="getCommentLikeButtonStyle(!!reply.isLiked)"
                          >
                            <Heart class="w-4 h-4" :class="{ 'fill-current': reply.isLiked }" aria-hidden="true"/>
                            <span>{{ reply.likeCount || 0 }}</span>
                          </button>
                          <button
                              @click="handleReply(rootComment, reply)"
                              class="flex items-center gap-2 transition-colors focus:outline-none text-sm"
                              style="color: var(--theme-text-secondary);"
                          >
                            <Reply class="w-4 h-4" aria-hidden="true"/>
                            <span>回复</span>
                          </button>
                        </div>
                      </div>
                    </div>

                    <!-- 回复输入框 - 在回复列表中 -->
                    <div v-if="replyingTo?.id === reply.id && !replyingToRoot" class="mt-3 mb-3 ml-13 p-3 rounded-lg"
                         style="background-color: var(--theme-bg);">
                      <div class="flex gap-3">
                        <img
                            :src="getSafeAvatar(currentUser?.avatar, currentUser?.id)"
                            :alt="currentUser?.username"
                            class="w-7 h-7 rounded-full flex-shrink-0"
                            loading="lazy"
                            @error="(e: Event) => (e.target as HTMLImageElement).src = getSafeAvatar(null, currentUser?.id)"
                        />
                        <div class="flex-1">
                          <div class="text-xs mb-2" style="color: var(--theme-text-secondary);">
                            回复 {{ getReplyToName(reply) || getCommentAuthorName(reply) }}
                          </div>
                          <textarea
                              v-model="replyContent"
                              placeholder="写回复..."
                              class="w-full p-2 border rounded-lg text-sm resize-none focus:outline-none focus:ring-2"
                              style="border-color: var(--theme-border); background-color: var(--theme-bg); color: var(--theme-text);"
                              rows="2"
                          />
                          <div class="flex justify-end mt-2 gap-2">
                            <button
                                @click="handleCancelReply"
                                class="px-3 py-1 rounded-full text-xs transition-colors focus:outline-none"
                                style="color: var(--theme-text-secondary);"
                            >
                              取消
                            </button>
                            <button
                                @click="handleSubmitReply"
                                :disabled="!replyContent.trim() || submitting"
                                class="px-3 py-1 rounded-full text-xs transition-colors flex items-center gap-1 disabled:opacity-50 focus:outline-none"
                                style="background-color: var(--theme-primary); color: white;"
                            >
                              回复
                            </button>
                          </div>
                        </div>
                      </div>
                    </div>
                  </div>
                </div>
              </div>
            </div>

            <div v-if="hasMoreComments" class="text-center mt-6">
              <button
                  v-if="hasMoreComments"
                  @click="loadMoreComments"
                  :disabled="commentsLoading"
                  class="px-6 py-2 rounded-full font-medium text-sm transition-colors focus:outline-none disabled:opacity-50 disabled:cursor-not-allowed"
                  style="background-color: var(--theme-surface); color: var(--theme-text-secondary);"
              >
                {{ commentsLoading ? '加载中...' : '加载更多评论' }}
              </button>
              <p v-else-if="comments.length > 0" class="text-center text-sm" style="color: var(--theme-text-secondary);">
                已显示全部评论
              </p>
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
    <SiteFooter />

    <!-- 返回顶部按钮 -->
    <BackToTop/>
  </div>
</template>

<style scoped>
.comment-root {
  margin-bottom: 24px;
  border-bottom: 1px solid var(--theme-border);
  padding-bottom: 16px;
}

.reply-list {
  margin-left: 52px;
  margin-top: 12px;
  background-color: var(--theme-bg);
  border-radius: 8px;
  padding: 12px;
}

.reply-item {
  margin-bottom: 12px;
  padding-bottom: 12px;
  border-bottom: 1px solid var(--theme-border);
}

.reply-item:last-child {
  margin-bottom: 0;
  padding-bottom: 0;
  border-bottom: none;
}

.reply-quote {
  font-size: 13px;
  color: var(--theme-text-secondary);
  background: var(--theme-surface);
  padding: 4px 8px;
  border-radius: 4px;
  margin: 6px 0;
  border-left: 2px solid var(--theme-primary);
}

.reply-to {
  color: var(--theme-primary);
}

.ml-13 {
  margin-left: 52px;
}
</style>
