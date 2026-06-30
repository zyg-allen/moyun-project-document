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

/** 搜索帮助文章 */
export const searchHelpArticles = (keyword: string) => {
  return httpGet<HelpArticle[]>(`/portal/help/search?keyword=${encodeURIComponent(keyword)}`);
};
