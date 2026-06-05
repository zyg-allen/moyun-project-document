import { defineStore } from 'pinia'
import { ref, computed } from 'vue'
import type { User } from '@/types/api'
import * as userApi from '@/api/user'
import { getToken, setToken, removeToken } from '@/api/client'

// 用户状态存储键名
const USER_STORAGE_KEY = 'moyun_user'

// 从localStorage恢复用户信息
function restoreUserFromStorage(): User | null {
  try {
    const userData = localStorage.getItem(USER_STORAGE_KEY)
    if (userData) {
      return JSON.parse(userData) as User
    }
  } catch (e) {
    console.error('恢复用户信息失败:', e)
  }
  return null
}

// 持久化用户信息到localStorage
function persistUserToStorage(user: User | null) {
  if (user) {
    localStorage.setItem(USER_STORAGE_KEY, JSON.stringify(user))
  } else {
    localStorage.removeItem(USER_STORAGE_KEY)
  }
}

export const useUserStore = defineStore('user', () => {
  // ==================== State ====================
  const user = ref<User | null>(restoreUserFromStorage())
  const isAuthenticated = computed(() => user.value !== null)
  const isLoading = ref(false)
  const isUserInitialized = ref(false)

  // ==================== Getters ====================
  const userId = computed(() => user.value?.id || '')
  const username = computed(() => user.value?.username || '')
  const nickname = computed(() => user.value?.nickname || user.value?.username || '')
  const avatar = computed(() => user.value?.avatar || '')
  const bio = computed(() => user.value?.bio || '')
  const email = computed(() => user.value?.email || '')

  // ==================== Actions ====================

  /**
   * 初始化用户状态 - 应用启动时调用
   * 如果有token，尝试获取用户信息
   */
  async function initializeUser(): Promise<void> {
    if (isUserInitialized.value) {
      return
    }

    const token = getToken()
    if (!token) {
      user.value = null
      persistUserToStorage(null)
      isUserInitialized.value = true
      return
    }

    // 尝试从本地存储恢复用户信息
    if (user.value) {
      isUserInitialized.value = true
      return
    }

    // 从服务端获取最新用户信息
    try {
      await fetchCurrentUser()
    } catch (e) {
      // 获取失败时，清除无效的token
      console.warn('初始化用户信息失败:', e)
      user.value = null
      persistUserToStorage(null)
    } finally {
      isUserInitialized.value = true
    }
  }

  /**
   * 从服务端获取当前登录用户信息
   */
  async function fetchCurrentUser(): Promise<User | null> {
    if (!getToken()) {
      return null
    }

    isLoading.value = true
    try {
      const response = await userApi.getCurrentUser()
      if (response.code === 200) {
        const userData = response.data as User
        user.value = userData
        persistUserToStorage(userData)
        return userData
      }
      // 获取失败时清除用户信息
      user.value = null
      persistUserToStorage(null)
      return null
    } catch (error) {
      console.error('获取用户信息失败:', error)
      user.value = null
      persistUserToStorage(null)
      return null
    } finally {
      isLoading.value = false
    }
  }

  /**
   * 登录
   */
  async function loginWithApi(params: { username: string; password: string }): Promise<{ success: boolean; message?: string }> {
    isLoading.value = true
    try {
      const response = await userApi.login(params)
      if (response.code === 200) {
        setToken(response.data.token)
        const userData = response.data.user as User
        user.value = userData
        persistUserToStorage(userData)
        return { success: true }
      }
      return { success: false, message: response.message }
    } catch (error) {
      console.error('登录失败:', error)
      return { success: false, message: '登录失败，请重试' }
    } finally {
      isLoading.value = false
    }
  }

  /**
   * 注册
   */
  async function registerWithApi(params: { username: string; email: string; password: string; confirmPassword?: string }): Promise<{ success: boolean; message?: string }> {
    isLoading.value = true
    try {
      // 如果没有提供 confirmPassword，则使用 password 作为默认值
      const registerParams = {
        ...params,
        confirmPassword: params.confirmPassword || params.password
      }
      const response = await userApi.register(registerParams)
      if (response.code === 200) {
        setToken(response.data.token)
        const userData = response.data.user as User
        user.value = userData
        persistUserToStorage(userData)
        return { success: true }
      }
      return { success: false, message: response.message }
    } catch (error) {
      console.error('注册失败:', error)
      return { success: false, message: '注册失败，请重试' }
    } finally {
      isLoading.value = false
    }
  }

  /**
   * 退出登录
   */
  async function logoutWithApi(): Promise<void> {
    try {
      await userApi.logout()
    } catch (error) {
      console.error('退出登录API调用失败:', error)
    } finally {
      // 无论API是否成功，都清除本地状态
      user.value = null
      persistUserToStorage(null)
      removeToken()
    }
  }

  /**
   * 更新用户信息
   */
  async function updateUserWithApi(updates: Partial<User>): Promise<{ success: boolean; message?: string }> {
    isLoading.value = true
    try {
      const response = await userApi.updateUserProfile(updates as any)
      if (response.code === 200) {
        const userData = response.data as User
        user.value = userData
        persistUserToStorage(userData)
        return { success: true }
      }
      return { success: false, message: response.message }
    } catch (error) {
      console.error('更新用户信息失败:', error)
      return { success: false, message: '更新失败，请重试' }
    } finally {
      isLoading.value = false
    }
  }

  return {
    // State
    user,
    isAuthenticated,
    isLoading,
    isUserInitialized,
    // Getters
    userId,
    username,
    nickname,
    avatar,
    bio,
    email,
    // Actions
    initializeUser,
    fetchCurrentUser,
    loginWithApi,
    registerWithApi,
    logoutWithApi,
    updateUserWithApi
  }
})
