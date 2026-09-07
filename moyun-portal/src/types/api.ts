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
  /** 是否为认证创作者（0=否 / 1=是），认证通过后由审核流程维护。控制文章/专栏/面经等高价值创作权限 */
  isCertifiedCreator?: number | boolean;
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
  /** 是否公开主页（是否在名家录/作者列表展示） */
  privacyProfile?: boolean;
  /** 关联后台用户ID（非空表示已绑定系统用户，可查看待办通知） */
  userId?: string | number;
}

export interface LoginParams {
  username: string;
  password: string;
  code?: string;
  uuid?: string;
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
  code?: string;
  uuid?: string;
  /** 邮箱验证码（注册时校验邮箱真实性） */
  emailCode?: string;
}

export interface RegisterResponse {
  token: string;
  refreshToken: string;
  user: User;
}

// 图形验证码（/captchaImage 返回，字段位于响应顶层而非 data 内）
export interface CaptchaImage {
  captchaEnabled: boolean;
  uuid: string;
  /** base64 编码的验证码图片（可直接作为 <img :src="base64"> 使用） */
  img: string;
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
  /** 是否公开主页（是否在名家录/作者列表展示） */
  privacyProfile?: boolean;
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

// 发送邮箱验证码
export interface SendEmailCodeParams {
  email: string;
  /** 场景：register 注册校验邮箱 / reset_password 找回密码 */
  type: 'register' | 'reset_password';
}

// 找回密码（邮箱验证码重置）
export interface ResetPasswordParams {
  email: string;
  code: string;
  newPassword: string;
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
  /** 编辑会话标识（一次编辑会话唯一，草稿/发布幂等去重） */
  sessionToken?: string;
  createdAt?: string;
  createTime?: string;
  updatedAt?: string;
  publishedAt?: string;
  remark?: string; // 审核意见（rejected 时存拒绝原因）
  /** 是否付费阅读 0=免费 1=付费 */
  isPaid?: number;
  /** 付费内容（购买后可见；未购买时后端会清空该字段） */
  paidContent?: string;
  /** 试读字数（未购买可预览的字数） */
  previewLength?: number;
  /** 付费价格，0=免费 */
  price?: number;
  /** 当前用户是否已购买该付费文章（详情接口动态填充） */
  isPurchased?: boolean;
  /** 当前用户是否已点赞（详情/列表接口动态填充，未登录为 false） */
  isLiked?: boolean;
  /** 当前用户是否已收藏（详情/列表接口动态填充，未登录为 false） */
  isBookmarked?: boolean;
}

// 打赏目标类型：article=文章打赏，column=专栏打赏，article_paid=付费阅读购买
export type TipTargetType = 'article' | 'column' | 'article_paid';

/**
 * 打赏/付费阅读购买订单
 * 复用 portal_tip_order 表，target_type='article_paid' 表示付费阅读购买记录
 */
export interface PortalTipOrder {
  id?: number | string;
  /** 打赏者ID */
  userId?: number | string;
  /** 被打赏者（作者）ID */
  authorId?: number | string;
  /** 目标类型：article/column/article_paid */
  targetType?: TipTargetType;
  /** 目标ID（文章ID/专栏ID） */
  targetId?: number | string;
  /** 金额（元） */
  amount?: number;
  /** 留言（打赏时附带） */
  message?: string;
  /** 订单状态：pending/paid */
  status?: string;
  /** 支付方式（简化版固定 wallet） */
  payMethod?: string;
  /** 支付时间 */
  paidTime?: string;
  /** 创建时间 */
  createdTime?: string;
  /** 打赏者昵称（JOIN 填充） */
  userNickname?: string;
  /** 打赏者头像（JOIN 填充） */
  userAvatar?: string;
  /** 作者昵称（JOIN 填充） */
  authorNickname?: string;
  /** 作者头像（JOIN 填充） */
  authorAvatar?: string;
}

/** 发起打赏请求体 */
export interface TipTargetBody {
  /** 金额（元） */
  amount: number;
  /** 留言 */
  message?: string;
}

/** 打赏列表分页查询参数 */
export interface TipQuery {
  pageNum?: number;
  pageSize?: number;
}

export interface ArticleListParams {
  page?: number;
  pageSize?: number;
  category?: string;
  categoryId?: string;
  /** 分类名（后端 ArticleQuery.categoryName，按分类名筛选） */
  categoryName?: string;
  /** 一级分类ID（后端 ArticleQuery.rootCategoryId，按一级分类查询其下全部子分类文章） */
  rootCategoryId?: string;
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
  /** 文章ID（草稿转发布/编辑时传入，沿用同一条记录） */
  id?: string;
  /** 编辑会话标识（一次编辑会话唯一，用于草稿/发布幂等去重） */
  sessionToken?: string;
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
  /** 是否在头部栏目展示（0否/1是） */
  showInNav?: number;
  /** 路由类型（home/category/static/external） */
  navRouteType?: string;
  /** 静态/外链路由路径（仅 static/external 类型使用） */
  navRoutePath?: string;
  /** 导航徽章（NEW/HOT，仅 Mega Menu 展示） */
  navBadge?: string;
  /** 栏目类型：article=文章栏目（可发布文章） special=特殊页面（不参与排行榜/发布） */
  categoryType?: string;
  /** 是否需要登录（0否/1是） */
  requiresAuth?: number;
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
  status?: string;
}

export interface UploadFileParams {
  file: File;
  businessType?: string;
}

// 通知相关类型
export interface Notification {
  id: string | number;
  userId?: string | number;
  type: 'comment' | 'like' | 'follow' | 'system' | 'order' | 'notice' | 'announcement' | 'todo';
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
  /**
   * 章节完成标记（v1.1 阅读闭环）
   * 前端检测到用户已读到章节底部时上报 true，后端据此将整书 status 置为 finished
   * 并触发成长事件 + Feed 动态。仅最后一章上报才有意义。
   */
  chapterFinished?: boolean;
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
  userNickname?: string;
  userAvatar?: string;
  bookId: string;
  bookTitle?: string;
  bookAuthor?: string;
  bookCover?: string;
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

/** 面试岗位字典 VO（v5.9 阶段1：驱动模拟面试岗位选择与画像抽题） */
export interface InterviewPositionVO {
  id: string | number;
  /** 岗位编码（如 java_backend） */
  code: string;
  /** 岗位名称（如 Java后端工程师，与后端 findByName 精确匹配） */
  name: string;
  /** 所属行业 */
  industry?: string;
  /** 岗位级别 junior/mid/senior */
  level?: string;
  /** 必备技能 JSON 数组字符串（如 ["Spring","MySQL"]），前端按需 JSON.parse */
  requiredSkills?: string;
  /** 热门公司 JSON 数组字符串 */
  hotCompanies?: string;
  /** 岗位描述 */
  description?: string;
  sort?: number;
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
  /** 题目类型：bagwen 八股 / algorithm 算法 / system_design 系统设计 / project 项目 / hr HR（v6.3 题目结构化） */
  questionType?: QuestionType | string;
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
  /** 推荐理由（仅画像推荐接口返回：weak_tag/required_skill/hot） */
  recommendReason?: string;
  /** 推荐匹配的标签/技能名（仅画像推荐接口返回） */
  recommendTag?: string;
  createTime?: string;
  updateTime?: string;
}

/**
 * 题目类型枚举（v6.3 题目结构化）
 * - bagwen 八股：基础理论，文本作答
 * - algorithm 算法：编程题，代码作答
 * - system_design 系统设计：架构方案，文本/图示作答
 * - project 项目：项目深挖，文本作答
 * - hr HR：行为面试，文本作答
 */
export type QuestionType = 'bagwen' | 'algorithm' | 'system_design' | 'project' | 'hr';

/** 评分标准单条项（v6.3 题目结构化） */
export interface ScoringCriterionItem {
  /** 评分维度名称，如"完整性"、"深度"、"代码质量" */
  dimension: string;
  /** 权重（百分比 0-100，前端按权重展示进度条） */
  weight?: number;
  /** 评分说明 */
  description?: string;
}

export interface InterviewQuestionDetailVO extends InterviewQuestionVO {
  hint?: string;
  /** 参考代码片段（algorithm 类型使用，兼容旧字段） */
  solution?: string;
  mySubmissions?: InterviewSubmissionVO[];

  // ===== 结构化字段（v6.3 题目结构化） =====
  /** 考察点列表 */
  examinePoints?: string[];
  /** 答题大纲（Markdown） */
  answerOutline?: string;
  /** 评分标准列表 */
  scoringCriteria?: ScoringCriterionItem[];
  /** 官方参考答案（Markdown，八股/设计/项目/HR 类完整答案） */
  referenceAnswer?: string;
  /** 前置题目 ID 列表（用于学习路径推荐） */
  prerequisiteIds?: (string | number)[];

  // ===== 练习模式扩展字段（v10.6 题库重构·阶段2） =====
  /** 练习模式：reading 展示阅读 / choice 选择题 / coding 编程题 */
  practiceMode?: string;
  /** 选择题选项（JSON 字符串，前端 JSON.parse 为 [{label,text,is_correct}]） */
  options?: string;
  /** 正确答案（选择题：选项 label 如 B） */
  correctAnswer?: string;
  /** 题目解析（做题后展示） */
  analysis?: string;
  /** 知识点标签（逗号分隔） */
  knowledgeTags?: string;
}

export interface InterviewQuestionQuery {
  pageNum?: number;
  pageSize?: number;
  categoryId?: string | number;
  difficulty?: string;
  /** 题目类型筛选（v6.3 题目结构化） */
  questionType?: QuestionType | string;
  /** 练习模式筛选（v10.6 题库重构·阶段2）：reading/choice/coding */
  practiceMode?: string;
  keyword?: string;
  companyId?: string | number;
}

/** 相邻题目导航（v12.0 做题页上一题/下一题） */
export interface InterviewQuestionNeighborVO {
  /** 上一题 ID（结果集首位时为 null） */
  prevId?: string | number | null;
  prevTitle?: string | null;
  /** 下一题 ID（结果集末位时为 null） */
  nextId?: string | number | null;
  nextTitle?: string | null;
  /** 当前题目在筛选结果集中的序号（1 起，不在结果集时为 null） */
  currentIndex?: number | null;
  /** 筛选结果集总数 */
  total?: number;
}

export interface InterviewSubmissionVO {
  id: string | number;
  questionId: string | number;
  userId: string | number;
  code?: string;
  content?: string;
  language?: string;
  answerType?: 'choice' | 'code' | 'text' | 'design' | 'reading';
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
  // ===== OJ 判题字段（v6.3） =====
  passedCaseCount?: number;
  totalCaseCount?: number;
  failedCaseId?: string | number;
  failedCaseInput?: string;
  failedCaseExpected?: string;
  failedCaseActual?: string;
  errorMessage?: string;
  // ===== 选择题服务端权威判分字段（v9.1） =====
  /** 是否通过（服务端权威判分结果，与 isSuccess 同源） */
  passed?: boolean;
  /** 练习模式：reading/choice/coding */
  practiceMode?: string;
  /** 正确答案（提交后下发，用于复盘） */
  correctAnswer?: string;
  /** 答案解析（提交后下发） */
  analysis?: string;
}

// ============ OJ 判题系统类型（v6.3） ============

/** 判题状态码 */
export type JudgeStatusCode =
  | 'AC' | 'WA' | 'TLE' | 'MLE' | 'RE' | 'CE' | 'SE' | 'PENDING';

/** 支持的编程语言 */
export type JudgeLanguage =
  | 'javascript' | 'typescript' | 'python'
  | 'java' | 'go' | 'cpp' | 'rust';

/** 测试用例 VO */
export interface TestCaseVO {
  id: string | number;
  questionId: string | number;
  /** 标准输入（样例可见，隐藏用例为 null） */
  input?: string;
  /** 期望输出（样例可见，隐藏用例为 null） */
  expectedOutput?: string;
  isSample?: boolean;
  orderNum?: number;
  explanation?: string;
}

/** 单用例判题结果 */
export interface CaseResultItem {
  caseId: string | number;
  caseIndex: number;
  isSample?: boolean;
  passed: boolean;
  runtime?: number;
  /** 实际输出（仅失败且为样例时回填） */
  actualOutput?: string;
  /** 错误信息（仅 RE/TLE 时回填） */
  errorMessage?: string;
}

/** 判题结果 VO */
export interface JudgeResultVO {
  submissionId?: string | number;
  /** 判题状态码（AC/WA/TLE/MLE/RE/CE/SE/PENDING） */
  status: JudgeStatusCode | string;
  statusName?: string;
  accepted?: boolean;
  passedCount: number;
  totalCount: number;
  maxRuntime?: number;
  maxMemory?: number;
  failedCaseId?: string | number;
  failedCaseInput?: string;
  failedCaseExpected?: string;
  failedCaseActual?: string;
  errorMessage?: string;
  caseResults?: CaseResultItem[];
}

/** 提交判题参数 */
export interface JudgeSubmitParams {
  questionId: string | number;
  code: string;
  language: JudgeLanguage | string;
  /**
   * 提交意图：run=运行（仅样例自测，不落记录不计成长）
   * submit=提交（全量判定+落记录+成长闭环），缺省按 submit 处理
   */
  mode?: 'run' | 'submit';
}

/** 用例新增/修改参数（CMS 后台） */
export interface TestCaseUpsertParams {
  questionId: string | number;
  input?: string;
  expectedOutput: string;
  isSample?: boolean;
  orderNum?: number;
  explanation?: string;
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
  /** 审核备注（驳回原因，rejected 时展示） */
  auditRemark?: string;
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
  /** 状态筛选（我的面经支持：draft/pending/published/rejected，空=全部） */
  status?: string;
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
  /** 预览图 JSON 数组字符串（多图，问题3图片列表展示） */
  previewImages?: string;
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
  /**
   * 模板结构化示例数据（JSON 字符串，v10.18 阶段一模板套用打通）
   * 字段：name/phone/email/city/avatar/jobIntention/educations/works/projects/skills/selfIntro
   * 后端 PortalInterviewResumeTemplate.sampleData 直传；前端 fillFromTemplate 解析后填充编辑页
   */
  sampleData?: string;
}

export interface InterviewResumeTemplateQuery {
  pageNum?: number;
  pageSize?: number;
  category?: string;
  keyword?: string;
  fileType?: string;
}

// ==================== 用户简历（面试空间第2期）====================

export interface UserResumeJobIntention {
  position?: string;
  city?: string;
  salaryMin?: number;
  salaryMax?: number;
  jobType?: string;
  availableTime?: string;
}

export interface UserResumeEducationItem {
  school?: string;
  major?: string;
  degree?: string;
  startDate?: string;
  endDate?: string;
  description?: string;
}

export interface UserResumeWorkItem {
  company?: string;
  position?: string;
  startDate?: string;
  endDate?: string;
  description?: string;
}

export interface UserResumeProjectItem {
  name?: string;
  role?: string;
  startDate?: string;
  endDate?: string;
  description?: string;
  url?: string;
}

export interface UserResumeSkillItem {
  name?: string;
  level?: string;
  category?: string;
}

export interface UserResumeScoreItem {
  item: string;
  maxScore: number;
  score: number;
  message?: string;
  /** 子项明细（v5.9 阶段2：用于岗位匹配度等维度，可为空） */
  subItems?: UserResumeSubScoreItem[];
}

/** 评分子项（如岗位必备技能匹配明细：Spring 已掌握 / MySQL 缺失） */
export interface UserResumeSubScoreItem {
  name: string;
  hit: boolean;
  message?: string;
}

export interface UserResumeVO {
  id?: string | number;
  userId?: string | number;
  title?: string;
  parentId?: string | number | null;
  versionNo?: number;
  name?: string;
  gender?: string;
  birthDate?: string;
  phone?: string;
  email?: string;
  avatar?: string;
  jobIntention?: UserResumeJobIntention | null;
  educations?: UserResumeEducationItem[];
  works?: UserResumeWorkItem[];
  projects?: UserResumeProjectItem[];
  skills?: UserResumeSkillItem[];
  selfIntro?: string;
  score?: number;
  scoreDetail?: UserResumeScoreItem[];
  scoredTime?: string;
  fileUrl?: string;
  exportTime?: string;
  status?: string;
  mine?: boolean;
  createTime?: string;
  updateTime?: string;
  /** 简历来源类型（v10.22）：online=在线简历（默认），attachment=附件简历 */
  sourceType?: 'online' | 'attachment' | string;
  /** 附件源文件 URL（sourceType=attachment 时有值，认证下载流） */
  sourceFileUrl?: string;
  /** 附件源文件名（sourceType=attachment 时有值） */
  sourceFileName?: string;
}

/** 简历 AI 改进建议 VO（v5.9 阶段2） */
export interface ResumeAiAdviceVO {
  resumeId?: string | number;
  /** 当前评分（0-115） */
  score?: number;
  /** 评分等级 A/B/C/D */
  grade?: string;
  /** 整体总结 */
  summary?: string;
  /** 改进建议列表（分点） */
  advices?: ResumeAiAdviceItem[];
  /** 缺失岗位必备技能列表 */
  missingSkills?: string[];
  /** 是否启用 AI 模型（当前 false 规则化，后期接入 AI 置 true） */
  aiPowered?: boolean;
  generatedTime?: string;
}

/** 岗位目标（v10.13，与后端 PortalResumeJobTarget 对齐） */
export interface ResumeJobTarget {
  id?: number | string;
  position: string;
  company?: string;
  city?: string;
  jobType?: string;
  /** 岗位描述/JD 原文（匹配分析核心输入） */
  jdText: string;
  jdKeywords?: string;
  isDefault?: number;
  createTime?: string;
  updateTime?: string;
}

/** 匹配报告维度评分（与后端 dimensions JSON 对齐） */
export interface JobMatchDimension {
  score: number;
  suggestions?: string[];
}

/** 岗位匹配报告（与后端 PortalResumeJobMatch 对齐） */
export interface ResumeJobMatchReport {
  id?: number | string;
  resumeId?: number | string;
  jobTargetId?: number | string;
  matchScore: number;
  grade?: 'excellent' | 'good' | 'medium' | 'poor' | string;
  matchedKeywords?: string;
  missingKeywords?: string;
  dimensions?: {
    keywordMatch?: JobMatchDimension;
    experienceMatch?: JobMatchDimension;
    skillMatch?: JobMatchDimension;
    structureMatch?: JobMatchDimension;
  };
  summary?: string;
  aiPowered?: number | boolean;
  createTime?: string;
}

/** 优化历史记录（与后端 PortalResumeOptimizeHistory 对齐，v10.15 评分闭环） */
export interface ResumeOptimizeHistory {
  id?: number | string;
  resumeId?: number | string;
  jobTargetId?: number | string;
  /** 优化前总分（规则评分，apply 时后端自动落库） */
  scoreBefore?: number;
  /** 优化后总分（规则评分，apply 时后端自动落库） */
  scoreAfter?: number;
  /** 优化前岗位匹配分（取最近一次匹配报告） */
  matchScoreBefore?: number;
  /** 优化后岗位匹配分（当前留空，需手动重跑匹配） */
  matchScoreAfter?: number;
  /** 采纳的建议项 ID 列表 */
  adoptedItems?: string;
  /** 优化说明摘要 */
  summary?: string;
  createTime?: string;
}

/** 深度优化建议单项（与后端 ResumeDeepOptimizeVO.OptimizeItem 对齐） */
export interface ResumeOptimizeItem {
  /** objective/education/work/project/skills/selfIntro */
  section: string;
  index?: number;
  field?: string;
  original?: string;
  optimized: string;
  reason?: string;
}

/** 深度优化结果（与后端 ResumeDeepOptimizeVO 对齐） */
export interface ResumeDeepOptimizeVO {
  resumeId?: number | string;
  jobTargetId?: number | string;
  summary?: string;
  items: ResumeOptimizeItem[];
  aiPowered?: boolean;
}

/**
 * 评分报告（与后端 PortalResumeScoreReport 对齐，v10.18 阶段五）
 * 每次评分（单独评分 / 优化后重新评分 / 模板套用评分）结果存档，可追溯历史评分
 */
export interface ResumeScoreReport {
  id?: number | string;
  userId?: number | string;
  resumeId: number | string;
  /** 关联岗位目标ID（可选，纯规则评分时为空） */
  jobTargetId?: number | string;
  /** 评分时的目标岗位快照（便于报告独立解读） */
  positionSnapshot?: string;
  /** 综合评分 0-100 */
  score: number;
  /** 各维度评分明细 JSON 字符串（与 portal_user_resume.score_detail 字段格式一致，复用前端解析） */
  scoreDetail?: string;
  /** 评分来源：manual 单独评分 / optimize 优化后重新评分 / template 模板套用评分 */
  source?: 'manual' | 'optimize' | 'template' | string;
  createTime?: string;
}

/** 简历附件解析结果（v10.12，与后端 ResumeParseVO 对齐，字段语义同 UserResumeVO） */
export interface ResumeParseVO {
  name?: string;
  gender?: string;
  /** yyyy-MM-dd（原文只有年月时后端补 01） */
  birthDate?: string;
  phone?: string;
  email?: string;
  title?: string;
  jobIntention?: UserResumeJobIntention;
  educations?: UserResumeEducationItem[];
  works?: UserResumeWorkItem[];
  projects?: UserResumeProjectItem[];
  skills?: UserResumeSkillItem[];
  selfIntro?: string;
  /** 是否由 LLM 结构化解析（false = 规则粗解析，字段覆盖度低） */
  aiPowered?: boolean;
  /** 附件抽取文本长度 */
  textLength?: number;
  /** v10.22：后端创建的附件简历记录 ID（上传解析成功后返回，前端据此跳转编辑页） */
  attachmentResumeId?: string | number;
  /** v10.22：附件源文件 URL（认证下载流） */
  sourceFileUrl?: string;
  /** v10.22：附件源文件名 */
  sourceFileName?: string;
}

/** 单条改进建议 */
export interface ResumeAiAdviceItem {
  /** 建议维度（如 "基本信息"、"岗位匹配度"） */
  dimension?: string;
  /** 优先级 high/medium/low */
  priority?: 'high' | 'medium' | 'low' | string;
  /** AI 优化结果（可直接采纳的优化后文本，按 dimension 映射填充到简历对应字段） */
  optimized?: string;
  /** 建议内容 */
  content?: string;
  /** 建议类型 fill（补充缺失）/ refine（优化已有）/ match（岗位匹配） */
  type?: 'fill' | 'refine' | 'match' | string;
}

export interface UserResumeQuery {
  pageNum?: number;
  pageSize?: number;
  keyword?: string;
  status?: string;
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
  /** 最后签到日期（YYYY-MM-DD，前端据此判断今日是否已签到） */
  lastCheckinDate?: string;
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

/** 成长时间线条目 */
export interface GrowthTimelineItem {
  id: string;
  userId?: string;
  module: string;
  action: string;
  entityType?: string;
  entityId?: string;
  growthDelta?: number;
  description?: string;
  createTime?: string;
  targetTitle?: string;
  targetCover?: string;
  targetUrl?: string;
  icon?: string;
  actionLabel?: string;
}

// ==================== Feed 流 ====================

/** Feed 动态事件类型（后端枚举值） */
export type FeedEventType =
    | 'publish_article'
    | 'publish_experience'
    | 'new_column'
    | 'checkin'
    | string;

/** Feed 动态目标类型（用于跳转） */
export type FeedTargetType = 'article' | 'experience' | 'column' | string;

/** Feed 动态事件 VO */
export interface FeedEventVO {
  /** 事件唯一ID */
  eventId: string | number;
  userId: string | number;
  userNickname?: string;
  userAvatar?: string;
  /** 事件类型：publish_article / publish_experience / new_column / checkin 等 */
  eventType: FeedEventType;
  /** 目标类型：article / experience / column */
  targetType: FeedTargetType;
  /** 目标ID（文章/面经/专栏ID） */
  targetId: string | number;
  title?: string;
  summary?: string;
  cover?: string;
  createdTime?: string;
}

// ==================== 专栏 ====================

/** 专栏内文章条目（目录列表） */
export interface ArticleSimpleVO {
  id: string | number;
  title: string;
  cover?: string;
  excerpt?: string;
  viewCount?: number;
  likeCount?: number;
  createdTime?: string;
  /** 排序值（升序） */
  sortOrder?: number;
}

/** 专栏列表项 */
export interface ColumnListItemVO {
  id: string | number;
  userId: string | number;
  title: string;
  subtitle?: string;
  cover?: string;
  description?: string;
  articleCount?: number;
  subscribeCount?: number;
  viewCount?: number;
  /** 是否完结：true 已完结 / false 连载中 */
  isFinished?: boolean;
  status?: string;
  createdTime?: string;
  authorName?: string;
  authorAvatar?: string;
}

/** 专栏详情 VO */
export interface ColumnVO {
  id: string | number;
  userId: string | number;
  title: string;
  subtitle?: string;
  description?: string;
  cover?: string;
  categoryId?: string | number;
  status?: string;
  articleCount?: number;
  subscribeCount?: number;
  viewCount?: number;
  isFinished?: boolean;
  price?: number;
  createdTime?: string;
  updatedTime?: string;
  authorName?: string;
  authorAvatar?: string;
  authorBio?: string;
  /** 当前用户是否已订阅（未登录为 false） */
  isSubscribed?: boolean;
  /** 专栏文章目录 */
  articles?: ArticleSimpleVO[];
}

/** 专栏列表查询参数 */
export interface ColumnQuery {
  /** 当前页码（对应后端 PageDomain.pageNum） */
  pageNum?: number;
  pageSize?: number;
  keyword?: string;
  status?: string;
  categoryId?: string | number;
  /** 排序方式：latest 最新 / popular 热门 / subscribe 订阅数 */
  sortBy?: 'latest' | 'popular' | 'subscribe';
}

/** 专栏保存（创建/修改）请求体 */
export interface ColumnSaveBody {
  id?: string | number;
  title: string;
  subtitle?: string;
  description?: string;
  cover?: string;
  categoryId?: string | number;
  // 后端 ColumnVO.isFinished 为 Integer（DB tinyint），传 boolean 会反序列化失败，必须 0/1
  isFinished?: number;
  price?: number;
}

/** 订阅切换返回 */
export interface SubscribeToggleResult {
  subscribed: boolean;
  subscribeCount: number;
}

/** 批量排序条目 */
export interface ColumnArticleSortItem {
  id: string | number;
  sortOrder: number;
}

// ==================== 私信相关类型 ====================

/** 聊天对方用户信息 */
export interface PeerUser {
  id: string;
  username?: string;
  nickname?: string;
  avatar?: string;
  bio?: string;
}

/** 消息类型：文本/图片等 */
export type MessageType = 'text' | 'image' | 'system';

/** 私信会话（列表项 VO） */
export interface MessageSessionVO {
  id: string;
  /** 会话对方用户信息 */
  peerUser?: PeerUser;
  /** 对方用户ID */
  peerId?: string;
  peerNickname?: string;
  peerAvatar?: string;
  /** 最后一条消息预览 */
  lastMessage?: string;
  /** 最后一条消息内容 */
  lastContent?: string;
  /** 最后消息时间 */
  lastMessageTime?: string;
  /** 最后消息发送者ID */
  lastSenderId?: string;
  /** 未读消息数 */
  unreadCount?: number;
  /** 会话创建/更新时间 */
  createTime?: string;
  updateTime?: string;
}

/** 单条消息 VO */
export interface MessageVO {
  id: string;
  /** 所属会话ID */
  sessionId: string;
  /** 发送者ID */
  senderId: string;
  senderNickname?: string;
  senderAvatar?: string;
  /** 接收者ID */
  receiverId?: string;
  /** 消息内容 */
  content: string;
  /** 消息类型 */
  msgType?: MessageType;
  /** 是否为当前用户发送 */
  isMine?: boolean;
  /** 消息创建时间 */
  createdTime?: string;
  createTime?: string;
}

/** 历史消息兼容别名 */
export type Message = MessageVO;

/** 私信会话（前端使用，等价于 MessageSessionVO） */
export type MessageSession = MessageSessionVO;

/** 发送私信参数 */
export interface SendMessageParams {
  receiverId: string;
  content: string;
  msgType?: MessageType;
}

/** 会话列表查询参数 */
export interface MessageSessionListParams {
  pageNum?: number;
  pageSize?: number;
}

/** 历史消息查询参数 */
export interface MessageHistoryParams {
  pageNum?: number;
  pageSize?: number;
}

// ==================== 个人中心 Dashboard ====================

/**
 * 个人中心顶部数据聚合接口返回结构（GET /portal/user/me/dashboard）
 * 各字段后端可能不全部返回，前端统一以可选 + 默认 0 处理。
 */
export interface UserDashboard {
  /** 文章数（已发布） */
  articles: number;
  /** 收藏总数（文章/题目/书单/金句合计） */
  bookmarks: number;
  /** 书架数 */
  bookshelf: number;
  /** 答题数 */
  questions: number;
  /** 面经数 */
  experiences: number;
  /** 简历数 */
  resumes: number;
  /** 关注数 */
  following: number;
  /** 粉丝数 */
  followers: number;
  /** 专栏数（专栏作者） */
  columns: number;
  /** 未读消息数 */
  unreadMessages: number;
  /** 成长等级 */
  growthLevel: number;
  /** 累计成长值 */
  growthValue: number;
  /** 当前等级头衔 */
  growthTitle?: string;
}

/** 关注/粉丝列表中的用户项（公开用户基础信息） */
export interface FollowUserItem {
  id: string | number;
  username?: string;
  nickname?: string;
  avatar?: string;
  bio?: string;
  position?: string;
  /** 是否被当前用户关注（粉丝列表用） */
  following?: boolean;
  /** 是否互相关注（对方也关注当前登录用户） */
  mutualFollow?: boolean;
  /** 是否为当前登录用户本人 */
  isMe?: boolean;
  createdAt?: string;
}

// ==================== 在线代码运行（任务 3.6）====================

/** 代码运行记录 VO */
export interface CodeRunVO {
  id: string | number;
  userId?: string | number;
  /** java/python/javascript */
  language: string;
  code: string;
  stdin?: string;
  output?: string;
  errorMsg?: string;
  /** running/success/failed/timeout */
  status: string;
  runtimeMs?: number;
  memKb?: number;
  createTime?: string;
}

// ==================== 用户画像快照（v5.9 阶段0：画像驱动抽题） ====================

/** 薄弱知识点条目 */
export interface WeakTagItem {
  /** 标签ID */
  tagId: number;
  /** 标签名（如 Spring） */
  tagName: string;
  /** 该标签下用户答过的题目总数 */
  total: number;
  /** 通过数 */
  solved: number;
  /** 失败率 0.0-1.0（solved/total） */
  failRate: number;
}

/** 用户画像快照 VO（v5.9 阶段0） */
export interface UserProfileSnapshotVO {
  userId?: number;
  /** 目标岗位 */
  position?: string;
  /** 面试场景（如 算法/系统设计） */
  scene?: string;
  /** 岗位必备技能（来自岗位字典 required_skills JSON 数组） */
  requiredSkills?: string[];
  /** 薄弱知识点列表（按 failRate 降序） */
  weakTags?: WeakTagItem[];
  /** 是否命中画像驱动（薄弱点 ≥ 1 或必备技能 ≥ 1） */
  personalized: boolean;
}

// ==================== 话题模块 ====================

/** 话题 VO */
export interface Topic {
  id: number;
  title: string;
  description?: string;
  cover?: string;
  creatorId: number;
  creator?: { id: number; nickname: string; avatar?: string; isCertifiedCreator?: boolean };
  /**
   * 状态：pending 待审核 / active 活跃 / archived 归档 / deleted 删除 / rejected 审核驳回
   * 新话题默认 pending，审核通过后 active
   */
  status: string;
  /** 审核人ID（审核时写入） */
  auditorId?: number;
  /** 审核意见/驳回原因 */
  auditRemark?: string;
  /** 审核时间 */
  auditTime?: string;
  pinned: number;
  viewCount: number;
  postCount: number;
  likeCount: number;
  commentCount: number;
  lastPostTime?: string;
  lastPosterId?: number;
  createdTime: string;
  updatedTime?: string;
  isLiked?: boolean;
  isOwner?: boolean;
}

/** 话题观点 VO */
export interface TopicPost {
  id: number;
  topicId: number;
  userId: number;
  user?: { id: number; nickname: string; avatar?: string };
  content: string;
  images?: string[];
  parentPostId?: number;
  replyToUserId?: number;
  replyToUser?: { id: number; nickname: string };
  floor: number;
  likeCount: number;
  commentCount: number;
  isDeleted: number;
  createdTime: string;
  isLiked?: boolean;
  isOwner?: boolean;
}

/** 话题评论 VO */
export interface TopicComment {
  id: number;
  targetType: string;
  targetId: number;
  authorId: number;
  author?: { id: number; nickname: string; avatar?: string };
  content: string;
  parentId: number;
  rootId: number;
  replyTo?: number;
  replyToContent?: string;
  replyToUser?: { id: number; nickname: string };
  likeCount: number;
  replyCount: number;
  isDeleted: number;
  createdTime: string;
  isLiked?: boolean;
  replies?: TopicComment[];
}

// ==================== V11.0 支付中心类型 ====================

/** 微信打赏下单返回（收银台参数） */
export interface PayCashierResult {
  /** 支付单号 */
  payNo: string;
  /** 微信 native 支付二维码链接 */
  codeUrl: string;
  /** 金额（元） */
  amount: number;
  /** 订单过期时间 */
  expireTime?: string;
  /** 打赏单ID */
  tipOrderId?: number | string;
  /** mock 模拟支付开关 */
  mockEnabled?: boolean;
}

/** 支付状态轮询结果 */
export interface PayStatusResult {
  payNo: string;
  /** CREATED/PAID/SETTLED/CLOSED */
  status: string;
  /** 金额（分） */
  amount: number;
  /** 金额（元） */
  amountYuan: number;
  expireTime?: string;
  codeUrl?: string;
  mockEnabled?: boolean;
}

/** 账户总览 */
export interface PayAccountOverview {
  userId: number | string;
  /** 余额（分） */
  balance: number;
  /** 余额（元） */
  balanceYuan: number;
  totalIncome: number;
  totalIncomeYuan: number;
  totalWithdraw: number;
  totalWithdrawYuan: number;
}

/** 资金流水条目 */
export interface PayLedgerEntry {
  id?: number | string;
  payNo?: string;
  bizType?: string;
  bizNo?: string;
  /** PLATFORM/USER */
  accountRole?: string;
  userId?: number | string;
  /** credit=收入 debit=支出 */
  direction?: string;
  /** 金额（分） */
  amount?: number;
  /** 金额（元） */
  amountYuan?: number;
  balanceAfter?: number;
  balanceAfterYuan?: number;
  summary?: string;
  createTime?: string;
}

/** 资金流水分页结果 */
export interface PayLedgerListResult {
  records: PayLedgerEntry[];
  total: number;
  current: number;
  size: number;
}

/** 银行卡（脱敏，密文从不下发） */
export interface UserBankCard {
  id: number | string;
  holderName: string;
  cardNoMasked: string;
  bankCode?: string;
  bankName?: string;
  /** PENDING/VERIFIED/REJECTED */
  verifyStatus?: string;
  isDefault?: number;
}

/** 绑定银行卡表单 */
export interface BankCardForm {
  holderName: string;
  cardNo: string;
  phone: string;
  bankCode?: string;
  bankName?: string;
  /** 短信验证码（V11.1 银行卡绑定强校验） */
  smsCode?: string;
}

/** 支付站内通知 */
export interface PayNotification {
  id: number | string;
  userId?: number | string;
  /** pay/withdraw/account */
  notifyType?: string;
  refNo?: string;
  title: string;
  content?: string;
  /** 0=未读 1=已读 */
  readFlag?: number;
  createTime?: string;
}

/** 支付通知分页结果 */
export interface PayNotificationListResult {
  records: PayNotification[];
  total: number;
  current: number;
  size: number;
  unreadCount?: number;
}
