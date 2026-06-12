import { defineStore } from 'pinia'
import { ref, computed } from 'vue'
import type { Article } from '@/types/api'
import { safeLocalStorage } from '@/utils/security'
import * as articleApi from '@/api/article'

const storage = safeLocalStorage()
const LIKED_KEY = 'article:likedIds'
const BOOKMARKED_KEY = 'article:bookmarkedIds'

export const useArticleStore = defineStore('article', () => {
    const articles = ref<Article[]>([])
    const loading = ref(false)
    const likedArticleIds = ref<string[]>(storage.getJSON<string[]>(LIKED_KEY, []))
    const bookmarkedArticleIds = ref<string[]>(storage.getJSON<string[]>(BOOKMARKED_KEY, []))

    const bookmarkedArticles = computed(() =>
        articles.value.filter((a) => bookmarkedArticleIds.value.includes(a.id))
    )

    function persistLiked() {
        storage.setJSON(LIKED_KEY, likedArticleIds.value)
    }

    function persistBookmarked() {
        storage.setJSON(BOOKMARKED_KEY, bookmarkedArticleIds.value)
    }

    function isArticleLiked(id: string): boolean {
        return likedArticleIds.value.includes(id)
    }

    function isArticleBookmarked(id: string): boolean {
        return bookmarkedArticleIds.value.includes(id)
    }

    function likeArticle(articleId: string) {
        const idx = likedArticleIds.value.indexOf(articleId)
        if (idx === -1) {
            likedArticleIds.value.push(articleId)
        } else {
            likedArticleIds.value.splice(idx, 1)
        }
        persistLiked()
    }

    function bookmarkArticle(id: string) {
        const idx = bookmarkedArticleIds.value.indexOf(id)
        if (idx === -1) {
            bookmarkedArticleIds.value.push(id)
        } else {
            bookmarkedArticleIds.value.splice(idx, 1)
        }
        persistBookmarked()
    }

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

    async function likeArticleWithApi(articleId: string): Promise<{ success: boolean; isLiked?: boolean; likeCount?: number }> {
        try {
            const response = await articleApi.toggleLikeArticle(articleId)
            if (response.code === 200 && response.data) {
                likeArticle(articleId)
                return { success: true, ...(response.data as any) }
            }
            return { success: false, isLiked: false, likeCount: 0 }
        } catch (error) {
            console.error('点赞失败:', error)
            return { success: false, isLiked: false, likeCount: 0 }
        }
    }

    async function bookmarkArticleWithApi(articleId: string): Promise<{ success: boolean }> {
        try {
            bookmarkArticle(articleId)
            return { success: true }
        } catch (error) {
            console.error('收藏失败:', error)
            return { success: false }
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
        likedArticleIds,
        bookmarkedArticleIds,
        bookmarkedArticles,
        likeArticle,
        isArticleLiked,
        bookmarkArticle,
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
