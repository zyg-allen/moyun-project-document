import { getSafeAvatar } from '@/utils/avatar';
import type { Article } from '@/types/api';

/**
 * 统一的 API 文章数据 → 前台 Article 格式转换
 *
 * 解决 HomePage / ListPage 等页面各自实现 transformArticle 导致字段不一致的问题。
 * 合并了 HomePage（字段更全）与 ListPage（嵌套访问）两份实现的回退策略。
 */
export function transformArticle(apiArticle: any): Article {
  const authorId = String(apiArticle.authorId || apiArticle.author?.id || apiArticle.userId || '1');
  const authorNickname =
    apiArticle.authorNickname ||
    apiArticle.authorUsername ||
    apiArticle.author?.nickname ||
    apiArticle.author?.username ||
    apiArticle.nickname ||
    '作者';
  const authorAvatar = apiArticle.authorAvatar || apiArticle.author?.avatar;

  return {
    id: String(apiArticle.id),
    title: apiArticle.title,
    content: apiArticle.content || '',
    excerpt:
      apiArticle.excerpt ||
      apiArticle.content?.substring(0, 100) ||
      '',
    cover: apiArticle.cover || apiArticle.thumbnail,
    author: {
      id: authorId,
      username: authorNickname,
      avatar: getSafeAvatar(authorAvatar, authorId),
      bio: apiArticle.authorBio || apiArticle.author?.bio || '热爱写作，分享生活',
    } as Article['author'],
    authorId,
    authorName: authorNickname,
    authorAvatar: getSafeAvatar(authorAvatar, authorId),
    authorNickname,
    category: apiArticle.categoryName || apiArticle.category || '散文',
    tags: apiArticle.tagNames || apiArticle.tags || [],
    views: Number(apiArticle.views || 0),
    likes: Number(apiArticle.likes || 0),
    comments: Number(apiArticle.comments || 0),
    createdAt:
      apiArticle.createdAt ||
      apiArticle.createTime ||
      apiArticle.publishedAt ||
      new Date().toISOString().split('T')[0],
    slug: apiArticle.slug,
  };
}

/**
 * 批量转换文章列表
 */
export function transformArticleList(apiArticles: any[] = []): Article[] {
  return (apiArticles || []).map(transformArticle);
}

export default { transformArticle, transformArticleList };
