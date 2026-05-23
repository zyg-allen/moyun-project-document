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
}

export interface Article {
  id: string
  title: string
  content: string
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
