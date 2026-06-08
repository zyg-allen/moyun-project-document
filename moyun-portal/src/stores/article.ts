import {defineStore} from 'pinia'
import {computed, ref} from 'vue'
import {safeLocalStorage} from '@/utils/security'
import {
    addArticle as addMockArticle,
    getArticleById,
    getArticles,
    getArticlesByCategory,
    searchArticles
} from '@/data/mockData'
import type {Article} from '@/types/api'
import * as articleApi from '@/api/article'
import * as bookmarkApi from '@/api/bookmark'

const storage = safeLocalStorage()

export const useArticleStore = defineStore('article', () => {
    const articles = ref<Article[]>(getArticles() as any)
    const loading = ref(false)
    const searchQuery = ref('')
    const selectedCategory = ref('')
    const likedArticleIds = ref<string[]>(storage.getJSON('likedArticles', []))
    const bookmarkedArticleIds = ref<string[]>(storage.getJSON('bookmarkedArticles', []))

    const featuredArticles = computed(() =>
        articles.value.filter(a => a.isFeatured).slice(0, 5)
    )

    const hotArticles = computed(() =>
        [...articles.value].sort((a, b) => (b.views || 0) - (a.views || 0)).slice(0, 5)
    )

    const latestArticles = computed(() =>
        [...articles.value].sort((a, b) =>
            new Date(b.createdAt || Date.now()).getTime() - new Date(a.createdAt || Date.now()).getTime()
        )
    )

    const filteredArticles = computed(() => {
        let result = articles.value

        if (searchQuery.value) {
            return searchArticles(searchQuery.value) as any
        }

        if (selectedCategory.value) {
            result = getArticlesByCategory(selectedCategory.value) as any
        }

        return result
    })

    const bookmarkedArticles = computed(() =>
        articles.value.filter(a => bookmarkedArticleIds.value.includes(a.id))
    )

    const saveLikedArticles = () => {
        storage.setJSON('likedArticles', likedArticleIds.value)
    }

    const saveBookmarkedArticles = () => {
        storage.setJSON('bookmarkedArticles', bookmarkedArticleIds.value)
    }

    // API Actions
    async function fetchArticlesWithApi(params?: any) {
        loading.value = true
        try {
            const response = await articleApi.getArticleList(params)
            if (response.code === 200) {
                articles.value = (response.data.list || []) as Article[]
            }
        } catch (error) {
            console.error('获取文章列表失败:', error)
            // Fallback to mock data
            articles.value = getArticles() as any
        } finally {
            loading.value = false
        }
    }

    async function fetchArticleDetailWithApi(id: string) {
        loading.value = true
        try {
            const response = await articleApi.getArticleDetail({id})
            if (response.code === 200) {
                return response.data
            }
            return null
        } catch (error) {
            console.error('获取文章详情失败:', error)
            return getArticleById(id)
        } finally {
            loading.value = false
        }
    }

    /**
     * 点赞文章 - 行业标准实现
     * 1. 调用后端统一接口
     * 2. 根据返回结果更新本地状态
     * 3. 返回最新点赞数
     */
    async function likeArticleWithApi(articleId: string) {
        try {
            const response = await articleApi.toggleLikeArticle(articleId)
            if (response.code === 200 && response.data) {
                const {isLiked, likeCount} = response.data

                // 更新本地状态
                const index = likedArticleIds.value.indexOf(articleId)
                if (isLiked && index === -1) {
                    likedArticleIds.value.push(articleId)
                } else if (!isLiked && index !== -1) {
                    likedArticleIds.value.splice(index, 1)
                }
                saveLikedArticles()

                // 更新文章的点赞数
                const article = articles.value.find(a => a.id === articleId)
                if (article) {
                    article.likes = likeCount
                }

                return {success: true, isLiked, likeCount}
            }
            return {success: false}
        } catch (error) {
            console.error('点赞失败:', error)
            return {success: false}
        }
    }

    async function bookmarkArticleWithApi(articleId: string) {
        try {
            const isBookmarked = bookmarkedArticleIds.value.includes(articleId)
            if (isBookmarked) {
                await bookmarkApi.removeBookmark({id: articleId})
            } else {
                await bookmarkApi.addBookmark({articleId})
            }
            bookmarkArticle(articleId)
            return {success: true}
        } catch (error) {
            console.error('收藏失败:', error)
            bookmarkArticle(articleId)
            return {success: false}
        }
    }

    function fetchArticles() {
        loading.value = true
        setTimeout(() => {
            articles.value = getArticles() as any
            loading.value = false
        }, 500)
    }

    function getArticle(id: string) {
        return getArticleById(id) as any
    }

    function addArticle(article: Partial<Article>) {
        const newArticle = addMockArticle(article as any)
        articles.value.unshift(newArticle as any)
        return newArticle
    }

    function updateArticle(id: string, updates: Partial<Article>) {
        const index = articles.value.findIndex(a => a.id === id)
        if (index !== -1) {
            articles.value[index] = {...articles.value[index], ...updates} as Article
        }
    }

    function deleteArticle(id: string) {
        articles.value = articles.value.filter(a => a.id !== id)
    }

    function likeArticle(id: string) {
        const article = articles.value.find(a => a.id === id)
        if (article) {
            const index = likedArticleIds.value.indexOf(id)
            if (index === -1) {
                likedArticleIds.value.push(id)
                article.likes = (article.likes || 0) + 1
            } else {
                likedArticleIds.value.splice(index, 1)
                article.likes = (article.likes || 1) - 1
            }
            saveLikedArticles()
        }
    }

    function isArticleLiked(id: string) {
        return likedArticleIds.value.includes(id)
    }

    function bookmarkArticle(id: string) {
        const index = bookmarkedArticleIds.value.indexOf(id)
        if (index === -1) {
            bookmarkedArticleIds.value.push(id)
        } else {
            bookmarkedArticleIds.value.splice(index, 1)
        }
        saveBookmarkedArticles()
    }

    function isArticleBookmarked(id: string) {
        return bookmarkedArticleIds.value.includes(id)
    }

    function shareArticle(id: string) {
        const article = articles.value.find(a => a.id === id)
        if (article) {
            if (!article.shareCount) article.shareCount = 0
            article.shareCount++
        }
    }

    function setSearchQuery(query: string) {
        searchQuery.value = query
    }

    function setCategory(category: string) {
        selectedCategory.value = category
    }

    return {
        articles,
        loading,
        searchQuery,
        selectedCategory,
        likedArticleIds,
        bookmarkedArticleIds,
        featuredArticles,
        hotArticles,
        latestArticles,
        filteredArticles,
        bookmarkedArticles,
        fetchArticles,
        getArticle,
        addArticle,
        updateArticle,
        deleteArticle,
        likeArticle,
        isArticleLiked,
        bookmarkArticle,
        isArticleBookmarked,
        shareArticle,
        setSearchQuery,
        setCategory,
        // API Methods
        fetchArticlesWithApi,
        fetchArticleDetailWithApi,
        likeArticleWithApi,
        bookmarkArticleWithApi
    }
})
