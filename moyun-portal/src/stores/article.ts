import { defineStore } from 'pinia'
import { ref, computed } from 'vue'
import { safeLocalStorage } from '@/utils/security'
import { getArticles, addArticle as addMockArticle, getArticleById, searchArticles, getArticlesByCategory } from '@/data/mockData'
import type { Article } from '@/types'
import * as articleApi from '@/api/article'
import * as bookmarkApi from '@/api/bookmark'

const storage = safeLocalStorage()

export const useArticleStore = defineStore('article', () => {
  const articles = ref<Article[]>(getArticles())
  const loading = ref(false)
  const searchQuery = ref('')
  const selectedCategory = ref('')
  const likedArticleIds = ref<string[]>(storage.getJSON('likedArticles', []))
  const bookmarkedArticleIds = ref<string[]>(storage.getJSON('bookmarkedArticles', []))

  const featuredArticles = computed(() => 
    articles.value.filter(a => a.isFeatured).slice(0, 5)
  )

  const hotArticles = computed(() => 
    [...articles.value].sort((a, b) => b.views - a.views).slice(0, 5)
  )

  const latestArticles = computed(() => 
    [...articles.value].sort((a, b) => 
      new Date(b.createdAt).getTime() - new Date(a.createdAt).getTime()
    )
  )

  const filteredArticles = computed(() => {
    let result = articles.value
    
    if (searchQuery.value) {
      return searchArticles(searchQuery.value)
    }
    
    if (selectedCategory.value) {
      result = getArticlesByCategory(selectedCategory.value)
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
        articles.value = response.data.list as Article[]
      }
    } catch (error) {
      console.error('获取文章列表失败:', error)
      // Fallback to mock data
      articles.value = getArticles()
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
      return getArticleById(id)
    } finally {
      loading.value = false
    }
  }

  async function likeArticleWithApi(articleId: string) {
    try {
      const isLiked = likedArticleIds.value.includes(articleId)
      if (isLiked) {
        await articleApi.unlikeArticle(articleId)
      } else {
        await articleApi.likeArticle(articleId)
      }
      likeArticle(articleId)
      return { success: true }
    } catch (error) {
      console.error('点赞失败:', error)
      // Still update UI even if API fails
      likeArticle(articleId)
      return { success: false }
    }
  }

  async function bookmarkArticleWithApi(articleId: string) {
    try {
      const isBookmarked = bookmarkedArticleIds.value.includes(articleId)
      if (isBookmarked) {
        await bookmarkApi.removeBookmark({ id: articleId })
      } else {
        await bookmarkApi.addBookmark({ articleId })
      }
      bookmarkArticle(articleId)
      return { success: true }
    } catch (error) {
      console.error('收藏失败:', error)
      bookmarkArticle(articleId)
      return { success: false }
    }
  }

  function fetchArticles() {
    loading.value = true
    setTimeout(() => {
      articles.value = getArticles()
      loading.value = false
    }, 500)
  }

  function getArticle(id: string) {
    return getArticleById(id)
  }

  function addArticle(article: Omit<Article, 'id' | 'views' | 'likes' | 'commentCount' | 'createdAt' | 'updatedAt' | 'isFeatured' | 'shareCount'>) {
    const newArticle = addMockArticle(article)
    articles.value.unshift(newArticle)
    return newArticle
  }

  function updateArticle(id: string, updates: Partial<Article>) {
    const index = articles.value.findIndex(a => a.id === id)
    if (index !== -1) {
      articles.value[index] = { ...articles.value[index], ...updates }
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
        article.likes++
      } else {
        likedArticleIds.value.splice(index, 1)
        article.likes--
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
