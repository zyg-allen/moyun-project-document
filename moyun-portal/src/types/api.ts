// API通用响应类型
export interface ApiResponse<T = any> {
  code: number;
  message: string;
  data: T;
}

export interface PaginationParams {
  page: number;
  pageSize: number;
}

export interface PaginationResponse<T> {
  list: T[];
  total: number;
  page: number;
  pageSize: number;
}

// 用户相关类型
export interface User {
  id: string;
  username: string;
  nickname?: string;
  email: string;
  avatar: string;
  bio?: string;
  phone?: string;
  wechat?: string;
  position?: string;
  role?: 'user' | 'admin' | 'vip';
  vipExpireAt?: string;
  createdAt: string;
  updatedAt?: string;
  isPhoneVerified?: boolean;
  isWechatVerified?: boolean;
  twoFactorEnabled?: boolean;
  status?: 'active' | 'banned' | 'inactive';
}

export interface LoginParams {
  username: string;
  password: string;
  captcha?: string;
}

export interface LoginResponse {
  token: string;
  refreshToken: string;
  user: User;
}

export interface RegisterParams {
  username: string;
  email: string;
  password: string;
  confirmPassword: string;
  captcha?: string;
}

export interface RegisterResponse {
  token: string;
  refreshToken: string;
  user: User;
}

export interface UpdateUserProfileParams {
  nickname?: string;
  bio?: string;
  avatar?: string;
  position?: string;
}

export interface UpdatePasswordParams {
  oldPassword: string;
  newPassword: string;
  confirmPassword: string;
}

export interface BindPhoneParams {
  phone: string;
  code: string;
}

export interface SendSmsCodeParams {
  phone: string;
  type: 'register' | 'login' | 'bind' | 'reset_password';
}

// 文章相关类型
export interface Article {
  id: string;
  title: string;
  content: string;
  contentMarkdown?: string;
  excerpt?: string;
  cover?: string;
  author?: User;
  authorId: string;
  // 后端返回的字段
  authorUsername?: string;
  authorNickname?: string;
  authorAvatar?: string;
  authorBio?: string;
  category?: string;
  categoryId?: string;
  categoryName?: string;
  tags?: string[];
  tagNames?: string[];
  views?: number;
  likes?: number;
  comments?: number;
  shareCount?: number;
  bookmarkCount?: number;
  isFeatured?: boolean;
  isTop?: boolean;
  isCarousel?: boolean;
  status?: 'draft' | 'published' | 'archived';
  editorMode?: 'richtext' | 'markdown';
  createdAt?: string;
  createTime?: string;
  updatedAt?: string;
  publishedAt?: string;
}

export interface ArticleListParams {
  page?: number;
  pageSize?: number;
  category?: string;
  categoryId?: string;
  tag?: string;
  keyword?: string;
  authorId?: string;
  isFeatured?: boolean;
  status?: 'draft' | 'published' | 'archived';
  sortBy?: 'createdAt' | 'views' | 'likes' | 'comments';
  sortOrder?: 'asc' | 'desc';
}

export interface ArticleDetailParams {
  id: string;
}

export interface CreateArticleParams {
  title: string;
  content: string;
  contentMarkdown?: string;
  excerpt?: string;
  cover?: string;
  category?: string;
  categoryId?: string;
  tags?: string[];
  status?: 'draft' | 'published';
  editorMode?: 'richtext' | 'markdown';
}

export interface UpdateArticleParams {
  id: string;
  title?: string;
  content?: string;
  contentMarkdown?: string;
  excerpt?: string;
  cover?: string;
  category?: string;
  categoryId?: string;
  tags?: string[];
  status?: 'draft' | 'published';
  editorMode?: 'richtext' | 'markdown';
}

// 评论相关类型
export interface Comment {
  id: string;
  articleId: string;
  author: User;
  authorId: string;
  content: string;
  parentId?: string;
  replyTo?: string;
  replyToUser?: User;
  replies?: Comment[];
  likeCount: number;
  isLiked?: boolean;
  createdAt: string;
  updatedAt?: string;
}

export interface CommentListParams {
  articleId: string;
  page?: number;
  pageSize?: number;
  parentId?: string;
}

export interface CreateCommentParams {
  articleId: string;
  content: string;
  parentId?: string;
  replyTo?: string;
}

export interface DeleteCommentParams {
  id: string;
}

// 点赞相关类型
export interface LikeArticleParams {
  articleId: string;
}

export interface LikeCommentParams {
  commentId: string;
}

// 收藏相关类型
export interface Bookmark {
  id: string;
  userId: string;
  articleId: string;
  article?: Article;
  createdAt: string;
}

export interface BookmarkListParams {
  page?: number;
  pageSize?: number;
}

export interface CreateBookmarkParams {
  articleId: string;
}

export interface DeleteBookmarkParams {
  id: string;
}

export interface CheckBookmarkParams {
  articleId: string;
}

export interface CheckBookmarkResponse {
  bookmarked: boolean;
  bookmarkId?: string;
}

// 关注相关类型
export interface FollowUserParams {
  userId: string;
}

export interface UnfollowUserParams {
  userId: string;
}

export interface CheckFollowParams {
  userId: string;
}

export interface CheckFollowResponse {
  following: boolean;
}

export interface Follower {
  id: string;
  user: User;
  userId: string;
  createdAt: string;
}

export interface Following {
  id: string;
  user: User;
  userId: string;
  createdAt: string;
}

export interface FollowerListParams {
  userId: string;
  page?: number;
  pageSize?: number;
}

export interface FollowingListParams {
  userId: string;
  page?: number;
  pageSize?: number;
}

// 用户统计相关
export interface UserStats {
  followers: number;
  following: number;
  articles: number;
  likes: number;
  views: number;
  bookmarks: number;
  todayVisitors?: number;
  totalVisitors?: number;
}

// 分类相关类型
export interface Category {
  id: string;
  name: string;
  slug: string;
  description?: string;
  icon?: string;
  articleCount?: number;
  sort?: number;
  parentId?: string;
  children?: Category[];
  createdAt: string;
}

export interface CategoryListParams {
  parentId?: string;
  includeChildren?: boolean;
}

// 标签相关类型
export interface Tag {
  id: string;
  name: string;
  slug: string;
  articleCount?: number;
  createdAt: string;
}

export interface TagListParams {
  page?: number;
  pageSize?: number;
  keyword?: string;
}

// 搜索相关类型
export interface SearchParams {
  keyword: string;
  type?: 'article' | 'user' | 'all';
  page?: number;
  pageSize?: number;
}

export interface SearchResponse {
  articles?: PaginationResponse<Article>;
  users?: PaginationResponse<User>;
}

// 文件上传相关类型
export interface UploadFileResponse {
  url: string;
  filename: string;
  size: number;
  mimeType: string;
}

// 通知相关类型
export interface Notification {
  id: string;
  userId: string;
  type: 'comment' | 'like' | 'follow' | 'system' | 'order';
  title: string;
  content: string;
  data?: Record<string, any>;
  read: boolean;
  createdAt: string;
}

export interface NotificationListParams {
  page?: number;
  pageSize?: number;
  type?: string;
  unreadOnly?: boolean;
}

export interface MarkNotificationReadParams {
  id?: string;
  all?: boolean;
}

export interface NotificationStats {
  total: number;
  unread: number;
}

// 订单相关类型
export interface Order {
  id: string;
  userId: string;
  orderNo: string;
  type: 'vip' | 'recharge' | 'product';
  amount: number;
  status: 'pending' | 'paid' | 'cancelled' | 'refunded';
  payMethod?: string;
  paidAt?: string;
  createdAt: string;
  updatedAt?: string;
}

export interface OrderListParams {
  page?: number;
  pageSize?: number;
  status?: string;
  type?: string;
}

export interface CreateOrderParams {
  type: 'vip' | 'recharge' | 'product';
  productId?: string;
  amount: number;
}

// VIP相关类型
export interface VipPackage {
  id: string;
  name: string;
  price: number;
  originalPrice?: number;
  duration: number; // 天数
  description?: string;
  features?: string[];
  popular?: boolean;
  sort?: number;
  status: 'active' | 'inactive';
}

export interface VipPackageListParams {
  status?: string;
}

// 充值金额选项
export interface RechargeOption {
  id: string;
  amount: number;
  bonus?: number;
  popular?: boolean;
  sort?: number;
  status: 'active' | 'inactive';
}

export interface RechargeOptionListParams {
  status?: string;
}

// 钱包相关
export interface Wallet {
  id: string;
  userId: string;
  balance: number;
  frozenBalance: number;
  totalRecharge: number;
  totalWithdraw: number;
  createdAt: string;
  updatedAt: string;
}

export interface WalletTransaction {
  id: string;
  userId: string;
  type: 'recharge' | 'consume' | 'refund' | 'withdraw';
  amount: number;
  balanceBefore: number;
  balanceAfter: number;
  description?: string;
  orderId?: string;
  createdAt: string;
}

export interface WalletTransactionListParams {
  page?: number;
  pageSize?: number;
  type?: string;
}

// 系统配置
export interface SystemConfig {
  siteName: string;
  siteDescription: string;
  siteLogo?: string;
  siteIcp?: string;
  contactEmail?: string;
  copyright?: string;
}

export interface SystemConfigResponse {
  config: SystemConfig;
}

// 今日访客相关
export interface Visitor {
  id: string;
  userId: string;
  visitorUser: User;
  visitorId?: string;
  visitorIp?: string;
  visitDate: string;
  visitCount: number;
  createdAt: string;
}

export interface TodayVisitorListParams {
  page?: number;
  pageSize?: number;
}

export interface TodayVisitorStats {
  total: number;
  list: Visitor[];
}

// 读书空间相关类型
export interface Book {
  id: string;
  title: string;
  author: string;
  cover: string;
  description: string;
  isbn?: string;
  publisher?: string;
  publishDate?: string;
  pageCount?: number;
  categoryId?: string;
  tags?: string[];
  rating: number;
  readingCount: number;
  status: string;
  createTime?: string;
  updateTime?: string;
}

export interface BookList {
  id: string;
  title: string;
  description: string;
  cover: string;
  bookCount: number;
  viewCount: number;
  likeCount: number;
  createTime?: string;
}

export interface Quote {
  id: string;
  content: string;
  page?: string;
  chapter?: string;
  likeCount: number;
  book?: Book;
  createTime?: string;
}

export interface ReadingHomeResponse {
  bookLists: BookList[];
  books: Book[];
  quotes: Quote[];
}

export interface BookListParams {
  page?: number;
  pageSize?: number;
  categoryId?: string;
}

// 面试指南相关类型
export interface InterviewCategory {
  id: string;
  name: string;
  slug: string;
  description: string;
  icon: string;
  sort: number;
  questionCount: number;
  status: string;
}

export interface InterviewQuestion {
  id: string;
  title: string;
  description: string;
  difficulty: 'easy' | 'medium' | 'hard';
  categoryId: string;
  tags: string[];
  companies: string[];
  acceptanceRate: number;
  submissionCount: number;
  likeCount: number;
  hint?: string;
  solution?: string;
  sort: number;
  status: string;
  createTime?: string;
}

export interface InterviewExperience {
  id: string;
  title: string;
  company: string;
  position: string;
  year: number;
  month: number;
  content: string;
  viewCount: number;
  likeCount: number;
  commentCount: number;
  user: User;
  createTime?: string;
}

export interface ResumeTemplate {
  id: string;
  title: string;
  description: string;
  cover: string;
  category: string;
  likeCount: number;
  downloadCount: number;
}

export interface InterviewHomeResponse {
  categories: InterviewCategory[];
  hotQuestions: InterviewQuestion[];
  experiences: InterviewExperience[];
  resumeTemplates: ResumeTemplate[];
}

export interface QuestionListParams {
  page?: number;
  pageSize?: number;
  categoryId?: string;
  difficulty?: string;
}
