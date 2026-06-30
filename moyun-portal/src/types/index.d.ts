export * from './api';
export type EditorMode = 'richtext' | 'markdown';
export type NotificationType = 'like' | 'comment' | 'follow' | 'system' | 'order' | 'notice' | 'announcement';
export interface Achievement {
    id: string;
    name: string;
    description: string;
    icon: string;
    type: 'bronze' | 'silver' | 'gold' | 'platinum';
    unlockedAt?: string;
}
export interface Series {
    id: string;
    title: string;
    description: string;
    cover?: string;
    authorId: string;
    author?: any;
    articlesCount: number;
    articles: any[];
    createdAt: string;
    updatedAt: string;
}
