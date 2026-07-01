// API通用响应类型
export interface ApiResponse<T = any> {
  code: number;
  message: string;
  data: T;
}

export interface PaginationParams {
  page: number;
  pageSize: number;
  orderByColumn?: string;
  isAsc?: string;
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
  gender?: string;
  birthday?: string;
  location?: string;
  website?: string;
  github?: string;
  company?: string;
  school?: string;
  language?: string;
  timezone?: string;
  notifyLike?: boolean;
  notifyComment?: boolean;
  notifyFollow?: boolean;
  notifySystem?: boolean;
  privacyFollow?: boolean;
  privacyBookmark?: boolean;
  privacyEmail?: boolean;
  privacyPhone?: boolean;
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
  username?: string;
  phone?: string;
  wechat?: string;
  email?: string;
  gender?: string;
  birthday?: string;
  location?: string;
  website?: string;
  github?: string;
  company?: string;
  school?: string;
  language?: string;
  timezone?: string;
  notifyLike?: boolean;
  notifyComment?: boolean;
  notifyFollow?: boolean;
  notifySystem?: boolean;
  privacyFollow?: boolean;
  privacyBookmark?: boolean;
  privacyEmail?: boolean;
  privacyPhone?: boolean;
}

export interface UpdatePasswordParams {
  oldPassword: string;
  newPassword: string;
  confirmPassword: string;
}

export interface SendSmsCodeParams {
  phone: string;
  type: 'register' | 'login' | 'bind' | 'reset_password';
}

// 文章相关类型
export interface Article {
  id: string;
  title: string;
  /** 文章URL别名，用于SEO语义化路径 */
  slug?: string;
  content: string;
  contentMarkdown?: string;
  excerpt?: string;
  cover?: string;
  author?: User;
  authorId?: string;
  // 后端返回的字段
  authorUsername?: string;
  authorNickname?: string;
  authorAvatar?: string;
  authorBio?: string;
  /** 通用别名，部分页面使用 */
  authorName?: string;
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
  status?: 'draft' | 'pending' | 'published' | 'rejected' | 'archived';
  editorMode?: 'richtext' | 'markdown';
  createdAt?: string;
  createTime?: string;
  updatedAt?: string;
  publishedAt?: string;
  remark?: string; // 审核意见（rejected 时存拒绝原因）
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
  coverImage?: string;
  category?: string;
  categoryId?: string;
  tags?: string[];
  tagNames?: string[];
  status?: 'draft' | 'published' | string;
  editorMode?: 'richtext' | 'markdown';
  /** 文章URL别名（SEO语义化路径），为空时后端自动生成 */
  slug?: string;
  /** 外部链接 */
  link?: string;
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
  tagNames?: string[];
  status?: 'draft' | 'pending' | 'published' | 'rejected' | 'archived';
  editorMode?: 'richtext' | 'markdown';
}

// 评论相关类型
export interface Comment {
  id: string;
  articleId: string;
  author?: User;
  authorId?: string;
  authorUsername?: string;
  authorNickname?: string;
  authorAvatar?: string;
  content: string;
  parentId?: string;
  rootId?: string;
  replyTo?: string;
  replyToUsername?: string;
  replyToNickname?: string;
  replyToContent?: string;
  replies?: Comment[];
  likeCount?: number;
  status?: string;
  createTime?: string;
  createdAt?: string;
  updateTime?: string;
  updatedAt?: string;
  isLiked?: boolean;
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
  /** 展示状态：0=展示 1=隐藏（后端维护） */
  status?: string | number;
  /** 跳转类型：0=本地路由跳转到栏目 1=跳转到外部链接 */
  linkType?: string | number;
  /** 外部链接地址（当 linkType=1 时生效） */
  externalUrl?: string;
  /** 类型标识：home=首页 category=普通栏目 special=特殊页面（如读书空间、面试指南） */
  type?: string;
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
  name?: string;
  status?: string;
  orderByColumn?: string;
  isAsc?: string;
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

// 文件管理相关类型
export interface FileInfo {
  id: string;
  fileName: string;
  fileExt?: string;
  fileType?: string;
  fileSize?: number;
  fileUrl: string;
  filePath?: string;
  storageType?: string;
  bucketName?: string;
  objectName?: string;
  fileMd5?: string;
  uploadUserId?: string;
  uploadUserName?: string;
  status?: string;
  businessType?: string;
  businessId?: string;
  createTime?: string;
  updateTime?: string;
}

export interface FileListParams {
  page?: number;
  pageSize?: number;
  fileName?: string;
  fileType?: string;
  storageType?: string;
  businessType?: string;
  businessId?: string;
  status?: string;
}

export interface UploadFileParams {
  file: File;
  businessType?: string;
  businessId?: string;
}

// 通知相关类型
export interface Notification {
  id: string | number;
  userId?: string | number;
  type: 'comment' | 'like' | 'follow' | 'system' | 'order' | 'notice' | 'announcement';
  title: string;
  content: string;
  data?: string | Record<string, any>;
  /** 通知范围：user=个人通知 all=全局广播 */
  scope?: 'user' | 'all';
  /** 通知子类型（RuoYi 公告类型：1=通知 2=公告） */
  noticeType?: string;
  /** 状态：0=正常 1=停用 */
  status?: string;
  /** 是否已读（后端通过 sys_notification_read 计算） */
  isRead?: boolean;
  /** 接收用户昵称（scope=user 时后端关联返回） */
  userNickname?: string;
  createBy?: string;
  createTime?: string;
  updateBy?: string;
  updateTime?: string;
  remark?: string;
}

export interface NotificationListParams {
  pageNum?: number;
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
  summary?: string;
  isbn?: string;
  publisher?: string;
  publishDate?: string;
  pageCount?: number;
  categoryId?: string;
  tags?: string;
  rating: number;
  readingCount: number;
  status: string;
  // 商业化预留字段
  accessLevel?: 'free' | 'vip' | 'preview';
  previewRatio?: number;
  price?: number;
  isFeatured?: boolean;
  isRecommended?: boolean;
  authorBio?: string;
  createTime?: string;
  updateTime?: string;
  // v1.0 章节/连载扩展字段
  /** 书籍类型：published 出版书籍 / novel 网络小说 / longform 长文文章（兼容面试空间） */
  type?: 'published' | 'novel' | 'longform';
  /** 连载状态：ongoing 连载中 / completed 已完结 / hiatus 暂停 */
  serialStatus?: 'ongoing' | 'completed' | 'hiatus';
  /** 总字数（已发布章节累计） */
  wordCount?: number;
  /** 已发布章节数 */
  chapterCount?: number;
  /** 最新章节 ID */
  latestChapterId?: string;
  /** 最新章节标题 */
  latestChapterTitle?: string;
  /** 最后更新时间（章节维度） */
  lastUpdateTime?: string;
  /** 是否完结 */
  isFinished?: boolean;
}

/** 章节元信息（列表/目录用，不含正文） */
export interface BookChapter {
  id: string;
  bookId: string;
  title: string;
  /** 正文 HTML（详情接口返回，列表接口不返回） */
  content?: string;
  /** 正文 Markdown（编辑器为 markdown 时返回） */
  contentMarkdown?: string;
  /** 编辑器模式：richtext 富文本 / markdown */
  editorMode?: 'richtext' | 'markdown';
  wordCount?: number;
  /** 章节序号（排序用） */
  chapterNo: number;
  /** 分卷 ID（可选） */
  volumeId?: string;
  /** 是否免费：true 免费 / false VIP */
  isFree?: boolean;
  price?: number;
  /** 是否已发布 */
  isPublished?: boolean;
  publishTime?: string;
  viewCount?: number;
  createTime?: string;
  updateTime?: string;
}

/** 章节导航（上一章/下一章） */
export interface BookChapterNav {
  prev: { id: string; chapterNo: number; title: string } | null;
  next: { id: string; chapterNo: number; title: string } | null;
  current: { id: string; chapterNo: number; title: string; bookId: string };
}

// =====================================================
// v1.0 第二阶段新增：阅读进度 / 书架 / 阅读偏好
// =====================================================

/** 阅读进度（章节级） */
export interface ReadingProgress {
  id?: string;
  userId?: string;
  bookId: string | number;
  /** 阅读状态: want_to_read, reading, finished */
  status?: string;
  /** 阅读进度百分比（0-100，整书维度，可空） */
  progress?: number;
  /** 已读页数（旧字段，保留） */
  pagesRead?: number;
  /** 当前阅读章节ID */
  currentChapterId?: string | number | null;
  /** 当前章节序号 */
  currentChapterNo?: number;
  /** 章节内滚动偏移（像素，用于续读恢复） */
  chapterOffset?: number;
  /** 最后阅读时间 */
  lastReadTime?: string;
  /** 累计阅读时长（毫秒） */
  readingDurationMs?: number;
  startTime?: string;
  finishTime?: string;
  createTime?: string;
  updateTime?: string;
}

/** 书架项 */
export interface BookshelfItem {
  id: string;
  userId: string;
  bookId: string;
  /** 最后阅读章节ID */
  lastChapterId?: string | null;
  /** 最后阅读章节序号 */
  lastChapterNo?: number;
  /** 排序值 */
  sort?: number;
  createTime?: string;
  updateTime?: string;
}

/** 书架收藏检查结果 */
export interface BookshelfCheckResult {
  inBookshelf: boolean;
  lastChapterId?: string | null;
  lastChapterNo?: number;
}

/** 阅读偏好 */
export interface ReadingPreference {
  id?: string;
  userId?: string;
  /** 正文字号（px，12-32） */
  fontSize: number;
  /** 行距（倍，1.2-3.0） */
  lineHeight: number;
  /** 阅读主题：default / light / dark / sepia */
  theme: 'default' | 'light' | 'dark' | 'sepia';
  /** 字体：system / serif / song / hei */
  fontFamily: 'system' | 'serif' | 'song' | 'hei';
  /** 字间距（px） */
  letterSpacing: number;
  /** 段间距（em） */
  paragraphSpacing: number;
  createTime?: string;
  updateTime?: string;
}

export interface BookList {
  id: string;
  title: string;
  description: string;
  cover: string;
  userId?: string;
  categoryId?: string;
  isPublic?: boolean;
  bookCount: number;
  viewCount: number;
  likeCount: number;
  status: string;
  isFeatured?: boolean;
  accessLevel?: string;
  tags?: string;
  createTime?: string;
  updateTime?: string;
}

export interface BookQuote {
  id: string;
  userId?: string;
  bookId: string;
  content: string;
  page?: string;
  chapter?: string;
  location?: string;
  likeCount: number;
  isPublic?: boolean;
  isFeatured?: boolean;
  book?: Book;
  createTime?: string;
  updateTime?: string;
}

// 保持向后兼容的 Quote 别名
export type Quote = BookQuote;

export interface ReadingHomeResponse {
  bookLists: BookList[];
  books: Book[];
  quotes: BookQuote[];
  bookCount?: number;
  bookListCount?: number;
  quoteCount?: number;
}

export interface BookListParams {
  page?: number;
  pageSize?: number;
  categoryId?: string;
  keyword?: string;
}

// 通用分页响应别名（兼容接口返回
export interface PagedResponse<T> {
  list: T[];
  total: number;
  page: number;
  pageSize: number;
}

// =====================================================
// v1.0 第三阶段新增：发现与运营（推荐位 / 发现页 / 排行榜 / 限免）
// =====================================================

/**
 * 推荐位（运营位）
 * 对应后端 PortalBookRecommend 实体，关联 portal_book 查询时附带 bookTitle
 */
export interface BookRecommend {
  id: string | number;
  /** 书籍ID */
  bookId: string | number;
  /** 推荐位置：home_banner / home_hot / category_top / limit_free / discover_banner */
  position: string;
  /** 排序（越小越靠前，默认 0） */
  sort?: number;
  /** 生效开始时间（为空表示立即生效） */
  startTime?: string;
  /** 生效结束时间（为空表示长期有效） */
  endTime?: string;
  /** 是否启用 */
  isActive?: boolean;
  /** 书名（JOIN portal_book 查询返回，非表字段） */
  bookTitle?: string;
  createTime?: string;
  updateTime?: string;
  /** 备注（后台维护用，预留） */
  remark?: string;
}

/** 排行榜类型 */
export type RankingType = 'hot' | 'new' | 'completed' | 'word_count';

/** 排行榜单项（复用 Book，附加排名序号由前端渲染） */
export type RankingItem = Book;

/** 排行榜聚合结果 */
export interface RankingResult {
  /** 排行类型 */
  type: RankingType | string;
  /** 书籍列表 */
  list: RankingItem[];
  /** 总数 */
  total: number;
}

/**
 * 发现页聚合数据
 * 单次请求返回 Banner + 热门排行 + 限免 + 最近更新
 */
export interface DiscoverData {
  /** 发现页 Banner（position=discover_banner） */
  banners: BookRecommend[];
  /** 热门排行 Top10（按 reading_count desc） */
  hotRanking: Book[];
  /** 限免专区（position=limit_free） */
  limitFree: BookRecommend[];
  /** 最近更新 Top10（按 last_update_time desc，仅 novel 类型） */
  recentUpdate: Book[];
}

// ========================= 面试指南相关类型（对齐后端 VO） =========================
export interface InterviewCategoryVO {
  id: string | number;
  name: string;
  slug?: string;
  description?: string;
  icon?: string;
  sort: number;
  questionCount?: number;
  status: string;
  createTime?: string;
  updateTime?: string;
}

export interface InterviewCompanyVO {
  id: string | number;
  name: string;
  slug?: string;
  logo?: string;
  description?: string;
  industry?: string;
  questionCount?: number;
  sort: number;
  status: string;
  createTime?: string;
  updateTime?: string;
}

export interface InterviewQuestionVO {
  id: string | number;
  title: string;
  description?: string;
  difficulty: 'easy' | 'medium' | 'hard';
  categoryId?: string | number;
  categoryName?: string;
  tags?: string[];
  tagList?: TagVO[];
  companies?: InterviewCompanyVO[];
  acceptanceRate: number;
  submissionCount: number;
  likeCount: number;
  liked?: boolean;
  bookmarked?: boolean;
  attemptStatus?: 'not_attempted' | 'attempted' | 'solved' | string;
  sort: number;
  status: string;
  createTime?: string;
  updateTime?: string;
}

export interface InterviewQuestionDetailVO extends InterviewQuestionVO {
  hint?: string;
  solution?: string;
  mySubmissions?: InterviewSubmissionVO[];
}

export interface InterviewQuestionQuery {
  pageNum?: number;
  pageSize?: number;
  categoryId?: string | number;
  difficulty?: string;
  keyword?: string;
  companyId?: string | number;
}

export interface InterviewSubmissionVO {
  id: string | number;
  questionId: string | number;
  userId: string | number;
  code?: string;
  content?: string;
  language?: string;
  answerType?: 'code' | 'text' | 'design';
  status?: string;
  isSuccess?: boolean;
  runtime?: number;
  memoryUsage?: number;
  note?: string;
  isFeatured?: boolean;
  featuredTime?: string;
  userNickname?: string;
  userAvatar?: string;
  createTime?: string;
}

export interface InterviewExperienceVO {
  id: string | number;
  userId: string | number;
  title: string;
  company: string;
  position?: string;
  year?: number;
  month?: number;
  summary?: string;
  content?: string;
  coverImage?: string;
  tags?: string[];
  tagList?: TagVO[];
  isTop?: boolean;
  viewCount: number;
  likeCount: number;
  commentCount: number;
  status?: string;
  liked?: boolean;
  userNickname?: string;
  userAvatar?: string;
  user?: { id: string; nickname: string; avatar?: string };
  createTime?: string;
  updateTime?: string;
}

export interface InterviewExperienceQuery {
  pageNum?: number;
  pageSize?: number;
  company?: string;
  keyword?: string;
  year?: number;
  userId?: string | number;
}

export interface InterviewCommentVO {
  id: string | number;
  experienceId: string | number;
  userId: string | number;
  parentId?: string | number;
  replyToUserId?: string | number;
  content: string;
  likeCount: number;
  liked?: boolean;
  status?: string;
  userNickname?: string;
  userAvatar?: string;
  user?: { id: string; nickname: string; avatar?: string };
  replyToUser?: { id: string; nickname: string };
  replies?: InterviewCommentVO[];
  createTime?: string;
}

export interface InterviewResumeTemplateVO {
  id: string | number;
  title: string;
  description?: string;
  cover?: string;
  downloadUrl?: string;
  category?: string;
  fileType?: string;
  fileSize?: number;
  isPremium?: boolean;
  usageGuide?: string;
  likeCount: number;
  downloadCount: number;
  tags?: string[];
  tagList?: TagVO[];
  sort: number;
  status: string;
  liked?: boolean;
  createTime?: string;
}

export interface InterviewResumeTemplateQuery {
  pageNum?: number;
  pageSize?: number;
  category?: string;
  keyword?: string;
  fileType?: string;
}

export interface InterviewBookmarkVO {
  id: string | number;
  questionId: string | number;
  userId: string | number;
  note?: string;
  question?: InterviewQuestionVO;
  createTime?: string;
}

export interface InterviewAttemptVO {
  id: string | number;
  questionId: string | number;
  userId: string | number;
  attemptCount: number;
  status: 'not_attempted' | 'attempted' | 'solved' | string;
  lastAttemptAt?: string;
  firstSolvedAt?: string;
  lastSolvedAt?: string;
  question?: InterviewQuestionVO;
}

export interface InterviewHomeDataVO {
  categories: InterviewCategoryVO[];
  hotQuestions: InterviewQuestionVO[];
  hotExperiences: InterviewExperienceVO[];
  resumeTemplates: InterviewResumeTemplateVO[];
  hotCompanies: InterviewCompanyVO[];
  totalQuestionCount?: number;
  totalSubmissionCount?: number;
}

export interface PageResult<T> {
  list: T[];
  total: number;
  page?: number;
  pageSize?: number;
}

// ==================== 通用标签（通用标签系统）====================

export interface TagVO {
  id?: string | number;
  name: string;
  slug?: string;
  module?: string;
  referenceCount?: number;
  sort?: number;
  status?: string;
  createTime?: string;
}

// ==================== 用户成长体系 ====================

/** 用户成长信息 */
export interface UserGrowthVO {
  userId: string | number;
  /** 累计成长值 */
  growthValue: number;
  /** 当前等级 */
  level: number;
  /** 当前头衔 */
  title: string;
  /** 本季成长值 */
  seasonValue: number;
  /** 距离下一级所需成长值 */
  nextLevelGrowth?: number;
  /** 下一级头衔 */
  nextLevelTitle?: string;
  /** 本季排名 */
  seasonRank?: number;
  updateTime?: string;
}

/** 用户统计信息（成长体系扩展） */
export interface UserStatsVO {
  userId: string | number;
  // 文章模块
  articles: number;
  views: number;
  likes: number;
  bookmarks: number;
  wordCount: number;
  // 读书空间
  bookFinished: number;
  booklistCount: number;
  quoteCount: number;
  readingMinutes: number;
  // 面试空间
  questionSolved: number;
  noteCount: number;
  experienceCount: number;
  noteAdopted: number;
  // 通用
  followers: number;
  following: number;
  comments: number;
  totalLikes: number;
  checkinStreak: number;
}

/** 用户徽章 */
export interface UserBadgeVO {
  id: string | number;
  achievementId: string | number;
  code: string;
  name: string;
  description?: string;
  icon?: string;
  module?: string;
  growthReward?: number;
  createTime?: string;
}

/** 成就展示（含用户达成状态） */
export interface AchievementVO {
  id: string | number;
  code: string;
  name: string;
  description?: string;
  icon?: string;
  /** 所属模块: article/reading/interview/all */
  module?: string;
  growthReward?: number;
  sort?: number;
  /** 当前用户是否已达成 */
  earned?: boolean;
  /** 达成时间（未达成为 null） */
  earnedTime?: string;
}

/** 成长排行榜项（复用 UserGrowthVO，附加用户基础信息） */
export interface GrowthRankingItem extends UserGrowthVO {
  nickname?: string;
  avatar?: string;
}

/** 每日签到结果 */
export interface CheckinResult {
  success: boolean;
  message: string;
  /** 连续签到天数 */
  streak?: number;
  /** 本次签到获得成长值 */
  growth?: number;
}
