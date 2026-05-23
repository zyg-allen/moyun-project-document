// 定义简化的类型，与mock数据匹配
export interface User {
  id: string
  username: string
  email: string
  avatar: string
  bio: string
  phone?: string
  wechat?: string
  createdAt: string
  isPhoneVerified?: boolean
  isWechatVerified?: boolean
  twoFactorEnabled?: boolean
  // 用户统计数据
  stats?: {
    articlesCount: number
    followersCount: number
    followingCount: number
    likesCount: number
    viewsCount: number
  }
  // 用户等级和会员信息
  level?: number
  isVIP?: boolean
  vipExpireAt?: string
}

export type EditorMode = 'richtext' | 'markdown'

export interface Article {
  id: string
  title: string
  content: string  // HTML 内容
  contentMarkdown?: string  // Markdown 原始内容
  excerpt: string
  cover?: string
  author: User
  category: string
  tags: string[]
  views: number
  likes: number
  shareCount?: number
  createdAt: string
  updatedAt?: string
  isFeatured?: boolean
  editorMode?: EditorMode  // 编辑器类型（可选）
  // 文章状态
  status?: 'draft' | 'published' | 'archived'
  // 评论数
  commentsCount?: number
}

export interface Comment {
  id: string
  articleId: string
  author: User
  content: string
  createdAt: string
  parentId?: string
  replies?: Comment[]
  likeCount?: number
  isLiked?: boolean
}

export interface Bookmark {
  id: string
  userId: string
  articleId: string
  createdAt: string
}

export interface Like {
  id: string
  userId: string
  articleId: string
  createdAt: string
}

// 通知相关类型
export type NotificationType = 
  | 'like' 
  | 'comment' 
  | 'follow' 
  | 'reply' 
  | 'system'

export interface Notification {
  id: string
  type: NotificationType
  userId: string
  actorId?: string
  actor?: User
  articleId?: string
  article?: Article
  commentId?: string
  content?: string
  isRead: boolean
  createdAt: string
}

// 用户成就和徽章
export interface Achievement {
  id: string
  name: string
  description: string
  icon: string
  type: 'bronze' | 'silver' | 'gold' | 'platinum'
  unlockedAt?: string
}

// 文章系列/专栏
export interface Series {
  id: string
  title: string
  description: string
  cover?: string
  authorId: string
  author?: User
  articlesCount: number
  articles: Article[]
  createdAt: string
  updatedAt: string
}

// 分类扩展
export interface Category {
  id: string
  name: string
  key: string
  description?: string
  icon?: string
  articlesCount?: number
  sortOrder?: number
}
