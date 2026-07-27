import { defineStore } from 'pinia'
import { ref, computed } from 'vue'
import type { Article } from '@/types/api'
import * as articleApi from '@/api/article'

/**
 * 文章 Store（v5.9 重构）
 *
 * 设计变更：
 * 不再用 localStorage 缓存点赞 / 收藏的 articleId 数组。
 * 原因：localStorage 是设备级共享的，多账号切换时会跨账号污染
 *   （A 账号点赞后切到 B 账号，B 看到自己也"已点赞"，但服务端实际无此记录）。
 * 现在严格以服务端返回的 article.isLiked / article.isBookmarked 为唯一真相源，
 * 详情接口在登录态下会动态填充这两个字段；点赞 / 收藏操作后用接口返回值更新 article 对象本身。
 *
 * 后端契约：
 *   - GET /portal/article/{id} → ArticleVO（含 isLiked / isBookmarked）
 *   - POST /portal/article/{id}/like → { isLiked, likeCount }
 *   - POST /portal/bookmark/{articleId}/toggle → { isBookmarked, articleId }
 */
export const useArticleStore = defineStore('article', () => {
    const articles = ref<Article[]>([])
    const loading = ref(false)

    const bookmarkedArticles = computed(() =>
        articles.value.filter((a) => a.isBookmarked === true)
    )

    /** 当前用户是否已点赞某文章（依据 articles 列表中的状态） */
    function isArticleLiked(id: string): boolean {
        const target = articles.value.find((a) => a.id === id)
        return !!target?.isLiked
    }

    /** 当前用户是否已收藏某文章（依据 articles 列表中的状态） */
    function isArticleBookmarked(id: string): boolean {
        const target = articles.value.find((a) => a.id === id)
        return !!target?.isBookmarked
    }

    /**
     * 点赞 / 取消点赞（服务端 toggle）
     * 用 API 返回的 isLiked / likeCount 更新本地 article 对象，避免不同步
     */
    async function likeArticleWithApi(article: Article): Promise<{ success: boolean; isLiked?: boolean; likeCount?: number }> {
        try {
            const response = await articleApi.toggleLikeArticle(article.id)
            if (response.code === 200 && response.data) {
                const data = response.data as { isLiked?: boolean; likeCount?: number }
                // 用服务端返回值更新 article 对象（真相源）
                article.isLiked = !!data.isLiked
                if (data.likeCount !== undefined) {
                    article.likes = data.likeCount
                }
                // 同步更新列表缓存（若存在）
                const inList = articles.value.find((a) => a.id === article.id)
                if (inList) {
                    inList.isLiked = article.isLiked
                    if (data.likeCount !== undefined) inList.likes = data.likeCount
                }
                return { success: true, ...data }
            }
            return { success: false, isLiked: false, likeCount: 0 }
        } catch (error) {
            console.error('点赞失败:', error)
            return { success: false, isLiked: false, likeCount: 0 }
        }
    }

    /**
     * 收藏 / 取消收藏（服务端 toggle）
     * 用 API 返回的 isBookmarked 更新本地 article 对象
     */
    async function bookmarkArticleWithApi(article: Article): Promise<{ success: boolean; isBookmarked?: boolean }> {
        try {
            const response = await articleApi.toggleBookmarkArticle(article.id)
            if (response.code === 200 && response.data) {
                const data = response.data as { isBookmarked?: boolean }
                article.isBookmarked = !!data.isBookmarked
                // 同步更新列表缓存（若存在）
                const inList = articles.value.find((a) => a.id === article.id)
                if (inList) {
                    inList.isBookmarked = article.isBookmarked
                }
                return { success: true, isBookmarked: data.isBookmarked }
            }
            return { success: false, isBookmarked: false }
        } catch (error) {
            console.error('收藏失败:', error)
            return { success: false, isBookmarked: false }
        }
    }

    /** 分享计数（仅本地乐观更新，无服务端状态） */
    function shareArticle(id: string) {
        const target = articles.value.find((a) => a.id === id)
        if (target) {
            target.shareCount = (target.shareCount || 0) + 1
        }
    }

    async function fetchArticlesWithApi(params?: any) {
        loading.value = true
        try {
            const response = await articleApi.getArticleList(params)
            if (response.code === 200) {
                const data: any = response.data
                articles.value = (data.list || data || []) as Article[]
            }
        } catch (error) {
            console.error('获取文章列表失败:', error)
        } finally {
            loading.value = false
        }
    }

    async function fetchArticleDetailWithApi(id: string) {
        loading.value = true
        try {
            const response = await articleApi.getArticleDetail({ id })
            if (response.code === 200) {
                return response.data
            }
            return null
        } catch (error) {
            console.error('获取文章详情失败:', error)
            return null
        } finally {
            loading.value = false
        }
    }

    async function fetchArticles(params?: any) {
        return fetchArticlesWithApi(params)
    }

    async function fetchArticleDetail(id: string) {
        return fetchArticleDetailWithApi(id)
    }

    return {
        articles,
        loading,
        bookmarkedArticles,
        isArticleLiked,
        isArticleBookmarked,
        shareArticle,
        likeArticleWithApi,
        bookmarkArticleWithApi,
        fetchArticlesWithApi,
        fetchArticleDetailWithApi,
        fetchArticles,
        fetchArticleDetail
    }
})
