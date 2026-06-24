import { httpGet } from './client';

// ==================== 类型定义 ====================

export interface HelpCategory {
  id: number;
  name: string;
  icon: string;
  description: string;
  sort: number;
  status: string;
}

export interface HelpArticle {
  id: number;
  categoryId: number;
  title: string;
  content: string;
  viewCount: number;
  likeCount: number;
  sort: number;
  isFeatured: number;
  status: string;
  createTime: string;
}

export interface HelpHomeData {
  categories: HelpCategory[];
  featuredArticles: HelpArticle[];
}

// ==================== API ====================

/** 帮助中心首页数据（分类 + 精选问题） */
export const getHelpHome = () => {
  return httpGet<HelpHomeData>('/portal/help/home');
};

/** 查询所有分类 */
export const getHelpCategories = () => {
  return httpGet<HelpCategory[]>('/portal/help/categories');
};

/** 按分类查询文章 */
export const getHelpArticlesByCategory = (categoryId: number) => {
  return httpGet<HelpArticle[]>(`/portal/help/category/${categoryId}`);
};

/** 查询精选文章 */
export const getHelpFeatured = (limit = 5) => {
  return httpGet<HelpArticle[]>(`/portal/help/featured?limit=${limit}`);
};

/** 搜索帮助文章 */
export const searchHelpArticles = (keyword: string) => {
  return httpGet<HelpArticle[]>(`/portal/help/search?keyword=${encodeURIComponent(keyword)}`);
};

/** 查询文章详情 */
export const getHelpArticleDetail = (id: number) => {
  return httpGet<HelpArticle>(`/portal/help/article/${id}`);
};
