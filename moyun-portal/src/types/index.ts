// 重新导出 api 类型，保持向后兼容
export * from './api'

// 保持原有的一些类型别名和额外类型
export type EditorMode = 'richtext' | 'markdown'

// 通知相关类型
export type NotificationType = 
  | 'like' 
  | 'comment' 
  | 'follow' 
  | 'reply' 
  | 'system'

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
  author?: any
  articlesCount: number
  articles: any[]
  createdAt: string
  updatedAt: string
}
