<script setup lang="ts">
import {computed, onMounted, onBeforeUnmount, ref, watch} from 'vue';
import {RouterLink as Link, useRoute, useRouter} from 'vue-router';
import {useHead} from '@vueuse/head';
import {Bookmark, Clock, Gift, Heart, Lock, MessageSquare, Reply, Send, Share2, UserPlus, UserCheck, FileText, ThumbsUp, Users} from 'lucide-vue-next';
import Breadcrumb from '@/components/Breadcrumb.vue';
import SiteFooter from '@/components/SiteFooter.vue';
import MarkdownRenderer from '@/components/MarkdownRenderer.vue';
import BackToTop from '@/components/BackToTop.vue';
import TagList from '@/components/TagList.vue';
import TipModal from '@/components/TipModal.vue';
import RelatedArticleCard from '@/components/RelatedArticleCard.vue';
import AdCard from '@/components/AdCard.vue';
import {useArticleStore} from '@/stores/article';
import {useUserStore} from '@/stores/user';
import {useAuth} from '@/composables/useAuth';
import {useToast} from '@/composables/useToast';
import {generateSeo, generateArticleJsonLd} from '@/utils/seo';
import {sanitizeHTML} from '@/utils/security';
import {formatShortDate} from '@/utils/date';
import {getSafeAvatar} from '@/utils/avatar';
import * as growthApi from '@/api/growth';
import * as followApi from '@/api/follow';
import type {Article, Comment, User, UserStatsVO} from '@/types/api';
import * as articleApi from '@/api/article';
import * as commentApi from '@/api/comment';
import {purchaseArticle} from '@/api/tip';

const route = useRoute();
const router = useRouter();
const articleStore = useArticleStore();
const userStore = useUserStore();
const {requireAuth, withAuthConfirm} = useAuth();
const toast = useToast();
const article = ref<Article | null>(null);
const comments = ref<Comment[]>([]);
const relatedArticles = ref<Article[]>([]);
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

// 作者统计画像（右侧栏作者卡用）
const authorStats = ref<UserStatsVO | null>(null);
const isFollowingAuthor = ref(false); // 是否已关注当前作者
const followLoading = ref(false);

// 检查是否支持原生分享
const supportsNativeShare = computed(() => {
  return typeof navigator !== 'undefined' && typeof navigator.share === 'function';
});

// 以服务端返回的 article.isLiked / article.isBookmarked 为唯一真相源
// 详情接口在登录态下会动态填充这两个字段，避免 localStorage 跨账号污染
const isLiked = computed(() => !!article.value?.isLiked);
const isBookmarked = computed(() => !!article.value?.isBookmarked);

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
  // 监听全局点击，点击分享菜单外部时关闭下拉框
  // （按钮 @blur 不可靠：点击 div/段落等非可聚焦元素时按钮不会失焦）
  document.addEventListener('click', handleClickOutside);
});

onBeforeUnmount(() => {
  document.removeEventListener('click', handleClickOutside);
});

// 路由参数变化时（同组件复用，如从相关推荐点击跳转新文章）重新加载
watch(() => route.params.id, (newId, oldId) => {
  if (newId && newId !== oldId) {
    // 重置状态，避免显示旧文章数据
    article.value = null;
    comments.value = [];
    relatedArticles.value = [];
    authorStats.value = null;
    isFollowingAuthor.value = false;
    commentsPageNum.value = 1;
    error.value = null;
    window.scrollTo({top: 0, behavior: 'smooth'});
    loadArticle();
  }
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

    // 文章加载成功后异步加载相关推荐（不阻塞详情页渲染）
    if (article.value) {
      loadRelatedArticles();
      // 异步加载作者统计画像 + 关注状态（右侧栏作者卡用）
      const authorId = article.value.authorId || article.value.author?.id;
      if (authorId) loadAuthorStats(authorId);
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

// 加载相关推荐（按分类取，过滤当前文章，最多 6 篇）
async function loadRelatedArticles() {
  if (!article.value) return;
  const category = articleCategory.value;
  if (!category) {
    relatedArticles.value = [];
    return;
  }
  try {
    const res = await articleApi.getCategoryRecommendedArticles(category, { page: 1, pageSize: 12 }, 12);
    const list = (res.data?.list || []) as Article[];
    const currentId = String(article.value.id);
    relatedArticles.value = list
      .filter(a => String(a.id) !== currentId)
      .slice(0, 6);
  } catch (err) {
    console.error('加载相关推荐失败:', err);
    relatedArticles.value = [];
  }
}

// 加载作者统计画像 + 关注状态（右侧栏作者卡用）
async function loadAuthorStats(authorId: string | number) {
  if (!authorId) return;
  // 重置状态，避免上一篇文章残留
  authorStats.value = null;
  isFollowingAuthor.value = false;
  try {
    const [statsResp, followResp] = await Promise.all([
      growthApi.getUserStatsById(authorId),
      currentUser.value
        ? followApi.checkFollow({ userId: String(authorId) }).catch(() => null)
        : Promise.resolve(null),
    ]);
    if (statsResp?.code === 200 && statsResp.data) {
      authorStats.value = statsResp.data as UserStatsVO;
    }
    if (followResp?.code === 200 && followResp.data) {
      isFollowingAuthor.value = !!(followResp.data as { following?: boolean }).following;
    }
  } catch (err) {
    console.error('加载作者统计失败:', err);
  }
}

// 关注 / 取消关注作者
async function toggleFollow() {
  if (!articleAuthor.value) return;
  const authorId = String(articleAuthor.value.id || article.value?.authorId || '');
  if (!authorId) return;
  await withAuthConfirm(async () => {
    followLoading.value = true;
    try {
      if (isFollowingAuthor.value) {
        const res = await followApi.unfollowUser({ userId: authorId });
        if (res?.code === 200) {
          isFollowingAuthor.value = false;
          if (authorStats.value && authorStats.value.followers > 0) {
            authorStats.value.followers -= 1;
          }
          toast.success('已取消关注');
        } else {
          toast.error(res?.message || '操作失败');
        }
      } else {
        const res = await followApi.followUser({ userId: authorId });
        if (res?.code === 200) {
          isFollowingAuthor.value = true;
          if (authorStats.value) {
            authorStats.value.followers = (authorStats.value.followers || 0) + 1;
          }
          toast.success('关注成功');
        } else {
          toast.error(res?.message || '操作失败');
        }
      }
    } catch (err) {
      const e = err as { message?: string };
      toast.error(e?.message || '操作失败，请稍后重试');
    } finally {
      followLoading.value = false;
    }
  }, '关注作者');
}

// 是否为当前文章作者本人（作者本人访问自己文章时，隐藏关注按钮）
const isSelfAuthor = computed(() => {
  if (!articleAuthor.value || !currentUser.value) return false;
  return String(articleAuthor.value.id || '') === String(currentUser.value.id || '');
});

async function handleLike() {
  if (!article.value) return;
  // 使用 withAuthConfirm 包装，未登录时弹出确认框
  await withAuthConfirm(async () => {
    // store 内部用 API 返回值更新 article.isLiked / likes，详情页 computed 自动响应
    await articleStore.likeArticleWithApi(article.value);
  }, '点赞');
}

async function handleBookmark() {
  if (!article.value) return;
  // 使用 withAuthConfirm 包装，未登录时弹出确认框
  await withAuthConfirm(async () => {
    // store 内部用 API 返回值更新 article.isBookmarked
    await articleStore.bookmarkArticleWithApi(article.value);
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
        toast.error('分享失败，请稍后重试');
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
        toast.success('评论发表成功');
      } else {
        toast.error(response.message || '发表评论失败');
      }
    } catch (error) {
      console.error('发表评论失败:', error);
      const e = error as { message?: string };
      toast.error(e?.message || '发表评论失败，请稍后重试');
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
      toast.success('回复发表成功');
    } else {
      toast.error(response.message || '发表回复失败');
    }
  } catch (error) {
    console.error('发表回复失败:', error);
    const e = error as { message?: string };
    toast.error(e?.message || '发表回复失败，请稍后重试');
  } finally {
    submitting.value = false;
  }
}

async function handleLikeComment(comment: Comment) {
  // 使用 withAuthConfirm 包装，未登录时弹出确认框
  await withAuthConfirm(async () => {
    try {
      // 后端返回 { isLiked, likeCount }，用于更新本地状态
      // 之前 BUG：只调 API 不更新本地，导致点赞按钮"看起来没反应"
      const res = await commentApi.likeComment(comment.id);
      const data = (res.data || {}) as { isLiked?: boolean; likeCount?: number };
      // 同步更新评论列表中所有该 id 的评论（根评论 + 它在各处的回复引用）
      updateCommentLikeState(comment.id, data.isLiked, data.likeCount);
    } catch (error) {
      console.error('点赞评论失败:', error);
      const e = error as { message?: string };
      toast.error(e?.message || '点赞失败，请稍后重试');
    }
  }, '点赞评论');
}

// 更新本地评论点赞状态：遍历一级评论及其回复，匹配 id 后更新 isLiked 与 likeCount
function updateCommentLikeState(commentId: string, isLiked?: boolean, likeCount?: number) {
  for (const root of comments.value) {
    if (root.id === commentId) {
      if (isLiked !== undefined) root.isLiked = isLiked;
      if (likeCount !== undefined) root.likeCount = likeCount;
    }
    if (root.replies) {
      for (const reply of root.replies) {
        if (reply.id === commentId) {
          if (isLiked !== undefined) reply.isLiked = isLiked;
          if (likeCount !== undefined) reply.likeCount = likeCount;
        }
      }
    }
  }
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

// ============ 打赏 & 付费阅读 ============
const showTipModal = ref(false);
const purchasing = ref(false);

// 是否为当前文章作者（作者本人访问自己的文章）
const isArticleOwner = computed(() => {
  if (!article.value || !currentUser.value) return false;
  return String(article.value.authorId || '') === String(currentUser.value.id || '');
});

// 是否为付费文章
const isPaidArticle = computed(() => article.value?.isPaid === 1);

// 是否需要购买才能阅读全文（付费 + 未购买 + 非作者）
const needPurchase = computed(() =>
  isPaidArticle.value && !article.value?.isPurchased && !isArticleOwner.value
);

// 打赏快捷金额
function openTipModal() {
  if (!requireAuth(router.currentRoute.value.fullPath)) return;
  showTipModal.value = true;
}

function onTipSuccess() {
  toast.success('鼓励成功，感谢支持创作者！');
  showTipModal.value = false;
}

function onTipError(message: string) {
  toast.error(message || '鼓励失败');
}

async function handlePurchase() {
  if (!article.value) return;
  await withAuthConfirm(async () => {
    purchasing.value = true;
    try {
      const res = await purchaseArticle(article.value.id);
      if (res.code === 200) {
        toast.success('购买成功，已解锁全文');
        await loadArticle();
      } else {
        toast.error(res.message || '购买失败');
      }
    } catch (err) {
      const e = err as {message?: string};
      toast.error(e?.message || '购买失败，请稍后重试');
    } finally {
      purchasing.value = false;
    }
  }, '购买付费阅读');
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

    <!-- 吸顶面包屑栏 - 统一详情页顶部样式 -->
    <div class="border-b sticky top-0 z-30 backdrop-blur-sm py-3" style="background-color: var(--theme-surface); border-color: var(--theme-border);">
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
        <!-- 双栏布局：左主区（文章） + 右侧栏（作者画像 + 小广告） -->
        <div class="grid grid-cols-1 lg:grid-cols-[1fr_320px] gap-6 items-start mb-4">
          <!-- 左侧主区 -->
          <div class="min-w-0">
          <article
              class="rounded-2xl mb-4 w-full"
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

              <!-- 内容区域 - 启用阅读列宽约束提升长文体验 -->
              <div class="flex-1 py-6">
                <MarkdownRenderer
                    :content="article.content"
                    :content-markdown="article.contentMarkdown"
                    :editor-mode="article.editorMode"
                    prose-width="normal"
                />
              </div>

              <!-- 付费解锁全文提示（付费文章 + 未购买 + 非作者） -->
              <div
                v-if="needPurchase"
                class="relative rounded-xl overflow-hidden mb-6"
                style="background: linear-gradient(135deg, var(--theme-accent), color-mix(in srgb, var(--theme-accent) 60%, var(--theme-primary))); border: 1px solid var(--theme-border);"
              >
                <div class="p-6 text-center">
                  <div class="w-12 h-12 mx-auto mb-3 rounded-full flex items-center justify-center" style="background-color: var(--theme-primary);">
                    <Lock class="w-6 h-6 text-white" aria-hidden="true" />
                  </div>
                  <h3 class="text-lg font-bold mb-2" style="color: var(--theme-text);">该文章为付费内容</h3>
                  <p class="text-sm mb-4" style="color: var(--theme-text-secondary);">
                    支付 <span class="font-bold" style="color: var(--theme-primary);">¥{{ Number(article.price || 0).toFixed(2) }}</span> 解锁全文
                    <span v-if="article.previewLength">（当前为试读部分）</span>
                  </p>
                  <button
                    @click="handlePurchase"
                    :disabled="purchasing"
                    class="inline-flex items-center gap-2 px-6 py-2.5 rounded-full font-medium text-sm transition-colors disabled:opacity-50"
                    style="background-color: var(--theme-primary); color: white;"
                  >
                    <Lock v-if="!purchasing" class="w-4 h-4" aria-hidden="true" />
                    <svg v-else class="animate-spin w-4 h-4" viewBox="0 0 24 24" fill="none">
                      <circle class="opacity-25" cx="12" cy="12" r="10" stroke="currentColor" stroke-width="4"></circle>
                      <path class="opacity-75" fill="currentColor" d="M4 12a8 8 0 018-8V0C5.373 0 0 5.373 0 12h4z"></path>
                    </svg>
                    {{ purchasing ? '处理中...' : '付费解锁全文' }}
                  </button>
                </div>
              </div>

              <!-- 已购买提示 -->
              <div
                v-else-if="isPaidArticle && article.isPurchased && !isArticleOwner"
                class="rounded-xl p-3 mb-6 text-center text-sm"
                style="background-color: #dcfce7; color: #16a34a;"
              >
                您已购买该文章，可阅读完整内容
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
                      v-if="!isArticleOwner"
                      @click="openTipModal"
                      class="flex items-center gap-2 px-4 py-2 rounded-full transition-all hover:scale-105 focus:outline-none"
                      style="background-color: var(--theme-surface); color: var(--theme-text-secondary);"
                      :aria-label="'打赏作者'"
                  >
                    <Gift class="w-5 h-5 transition-transform" aria-hidden="true"/>
                    <span class="font-medium text-sm">打赏</span>
                  </button>
                  <button
                      @click="handleShare"
                      class="flex items-center gap-2 px-4 py-2 rounded-full transition-all hover:scale-105 focus:outline-none relative share-menu-container"
                      style="background-color: var(--theme-surface); color: var(--theme-text-secondary);"
                      :aria-label="'分享文章'"
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
          </div>
          <!-- /左侧主区 -->

          <!-- 右侧栏：作者画像卡 + 小广告位（桌面端 sticky；移动端不 sticky，自然下移到文章下方） -->
          <aside class="space-y-4 lg:sticky lg:top-20 self-start w-full">
            <!-- 作者画像卡 -->
            <div
                v-if="articleAuthor"
                class="rounded-2xl p-5 author-card"
                style="background-color: var(--theme-surface); border: 1px solid var(--theme-border);"
            >
              <!-- 头像 + 名字 -->
              <div class="flex flex-col items-center text-center">
                <Link
                    v-if="articleAuthor.id || article?.authorId"
                    :to="`/author/${articleAuthor.id || article?.authorId}`"
                    class="block hover:opacity-90 transition-opacity"
                >
                  <img
                      :src="getSafeAvatar(articleAuthor.avatar, articleAuthor.id)"
                      :alt="articleAuthor.username"
                      class="w-20 h-20 rounded-full ring-2"
                      style="--tw-ring-color: var(--theme-accent);"
                      loading="lazy"
                      @error="(e: Event) => (e.target as HTMLImageElement).src = getSafeAvatar(null, articleAuthor.id)"
                  />
                </Link>
                <img
                    v-else
                    :src="getSafeAvatar(articleAuthor.avatar, articleAuthor.id)"
                    :alt="articleAuthor.username"
                    class="w-20 h-20 rounded-full ring-2"
                    style="--tw-ring-color: var(--theme-accent);"
                    loading="lazy"
                />
                <Link
                    v-if="articleAuthor.id || article?.authorId"
                    :to="`/author/${articleAuthor.id || article?.authorId}`"
                    class="mt-3 font-bold text-base hover:underline"
                    style="color: var(--theme-text);"
                >
                  {{ articleAuthor.nickname || articleAuthor.username || '匿名作者' }}
                </Link>
                <span v-else class="mt-3 font-bold text-base" style="color: var(--theme-text);">
                  {{ articleAuthor.nickname || articleAuthor.username || '匿名作者' }}
                </span>
                <!-- 简介 -->
                <p
                    v-if="articleAuthor.bio"
                    class="mt-2 text-sm leading-relaxed line-clamp-3"
                    style="color: var(--theme-text-secondary);"
                >
                  {{ articleAuthor.bio }}
                </p>
              </div>

              <!-- 统计指标 2x2 网格 -->
              <div class="grid grid-cols-2 gap-2 mt-4">
                <div class="stat-cell">
                  <FileText class="w-4 h-4 mb-1" aria-hidden="true"/>
                  <div class="stat-value">{{ authorStats?.articles ?? 0 }}</div>
                  <div class="stat-label">文章</div>
                </div>
                <div class="stat-cell">
                  <ThumbsUp class="w-4 h-4 mb-1" aria-hidden="true"/>
                  <div class="stat-value">{{ authorStats?.totalLikes ?? authorStats?.likes ?? 0 }}</div>
                  <div class="stat-label">被点赞</div>
                </div>
                <div class="stat-cell">
                  <Users class="w-4 h-4 mb-1" aria-hidden="true"/>
                  <div class="stat-value">{{ authorStats?.followers ?? 0 }}</div>
                  <div class="stat-label">粉丝</div>
                </div>
                <div class="stat-cell">
                  <MessageSquare class="w-4 h-4 mb-1" aria-hidden="true"/>
                  <div class="stat-value">{{ authorStats?.comments ?? 0 }}</div>
                  <div class="stat-label">观点</div>
                </div>
              </div>

              <!-- 关注按钮（作者本人不显示） -->
              <button
                  v-if="!isSelfAuthor && (articleAuthor.id || article?.authorId)"
                  @click="toggleFollow"
                  :disabled="followLoading"
                  class="mt-4 w-full flex items-center justify-center gap-2 px-4 py-2 rounded-full font-medium text-sm transition-colors disabled:opacity-60 focus:outline-none"
                  :style="isFollowingAuthor
                    ? { backgroundColor: 'var(--theme-surface)', color: 'var(--theme-text-secondary)', border: '1px solid var(--theme-border)' }
                    : { backgroundColor: 'var(--theme-primary)', color: 'white' }"
                  :aria-pressed="isFollowingAuthor"
                  :aria-label="isFollowingAuthor ? '取消关注作者' : '关注作者'"
              >
                <UserCheck v-if="isFollowingAuthor" class="w-4 h-4" aria-hidden="true"/>
                <UserPlus v-else class="w-4 h-4" aria-hidden="true"/>
                <span>{{ followLoading ? '处理中...' : (isFollowingAuthor ? '已关注' : '关注作者') }}</span>
              </button>

              <!-- 查看主页 -->
              <Link
                  v-if="articleAuthor.id || article?.authorId"
                  :to="`/author/${articleAuthor.id || article?.authorId}`"
                  class="mt-2 block text-center text-sm font-medium hover:underline"
                  style="color: var(--theme-primary);"
              >
                查看作者主页 →
              </Link>
            </div>

            <!-- 右侧栏小广告位（仅桌面端展示，避免移动端与底部广告位重复） -->
            <div class="hidden lg:block">
              <AdCard slot-key="article_detail_sidebar" />
            </div>
          </aside>
        </div>
        <!-- /双栏布局 -->

          <!-- 相关推荐 -->
          <section v-if="relatedArticles.length > 0" class="mb-4">
            <h2 class="text-lg font-bold mb-4 flex items-center space-x-3"
                style="color: var(--theme-text);">
              <span>相关推荐</span>
            </h2>
            <div class="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-3 gap-4">
              <RelatedArticleCard
                v-for="rel in relatedArticles"
                :key="rel.id"
                :article="rel"
              />
            </div>
          </section>

          <!-- 广告位 -->
          <AdCard slot-key="article_detail_bottom" class="mb-4" />

          <section class="rounded-2xl p-4 sm:p-6 md:p-8 mb-4"
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
                          @click="handleLikeComment(rootComment)"
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
                              @click="handleLikeComment(reply)"
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

    <!-- 打赏弹窗（积分打赏 MVP） -->
    <TipModal
      :show="showTipModal"
      target-type="article"
      :target-id="article?.id || ''"
      :author-avatar="articleAuthor?.avatar"
      :author-name="articleAuthor?.nickname || articleAuthor?.username"
      :target-title="article?.title"
      @close="showTipModal = false"
      @success="onTipSuccess"
      @error="onTipError"
    />

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

/* 作者画像卡统计单元 */
.author-card .stat-cell {
  display: flex;
  flex-direction: column;
  align-items: center;
  padding: 10px 6px;
  border-radius: 10px;
  background-color: var(--theme-accent);
  text-align: center;
}

.author-card .stat-cell svg {
  color: var(--theme-primary);
}

.author-card .stat-value {
  font-size: 16px;
  font-weight: 700;
  line-height: 1.2;
  color: var(--theme-text);
}

.author-card .stat-label {
  font-size: 12px;
  color: var(--theme-text-secondary);
  margin-top: 2px;
}

/* 简介 3 行截断 */
.line-clamp-3 {
  display: -webkit-box;
  -webkit-line-clamp: 3;
  -webkit-box-orient: vertical;
  overflow: hidden;
}
</style>
