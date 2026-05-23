import { defineStore } from 'pinia'
import { ref, computed } from 'vue'
import type { User } from '@/types'
import { getCurrentUser as getMockCurrentUser, setCurrentUser as setMockCurrentUser } from '@/data/mockData'
import * as userApi from '@/api/user'
import { getToken, setToken, removeToken } from '@/api/client'

export const useUserStore = defineStore('user', () => {
  // State
  const user = ref<User | null>(getMockCurrentUser())
  const isAuthenticated = computed(() => user.value !== null)
  const loading = ref(false)

  // Getters
  const username = computed(() => user.value?.username || '')
  const avatar = computed(() => user.value?.avatar || '')

  // Actions
  async function fetchCurrentUser() {
    if (!getToken()) {
      return null
    }
    
    loading.value = true
    try {
      const response = await userApi.getCurrentUser()
      if (response.code === 200) {
        user.value = response.data as User
        setMockCurrentUser(response.data as any)
        return response.data
      }
      return null
    } catch (error) {
      console.error('获取用户信息失败:', error)
      return null
    } finally {
      loading.value = false
    }
  }

  async function loginWithApi(params: { username: string; password: string }) {
    loading.value = true
    try {
      const response = await userApi.login(params)
      if (response.code === 200) {
        setToken(response.data.token)
        user.value = response.data.user as User
        setMockCurrentUser(response.data.user as any)
        return { success: true }
      }
      return { success: false, message: response.message }
    } catch (error) {
      console.error('登录失败:', error)
      return { success: false, message: '登录失败，请重试' }
    } finally {
      loading.value = false
    }
  }

  async function logoutWithApi() {
    try {
      await userApi.logout()
    } catch (error) {
      console.error('退出登录API调用失败:', error)
    } finally {
      user.value = null
      removeToken()
      setMockCurrentUser(null)
    }
  }

  function login(userData: User) {
    user.value = userData
    setMockCurrentUser(userData)
  }

  function logout() {
    user.value = null
    setMockCurrentUser(null)
  }

  async function updateUserWithApi(updates: Partial<User>) {
    loading.value = true
    try {
      const response = await userApi.updateUserProfile(updates as any)
      if (response.code === 200) {
        user.value = response.data as User
        setMockCurrentUser(response.data as any)
        return { success: true }
      }
      return { success: false, message: response.message }
    } catch (error) {
      console.error('更新用户信息失败:', error)
      return { success: false, message: '更新失败，请重试' }
    } finally {
      loading.value = false
    }
  }

  function updateUser(updates: Partial<User>) {
    if (user.value) {
      user.value = { ...user.value, ...updates }
      setMockCurrentUser(user.value)
    }
  }

  return {
    // State
    user,
    isAuthenticated,
    loading,
    // Getters
    username,
    avatar,
    // Actions
    login,
    logout,
    updateUser,
    fetchCurrentUser,
    loginWithApi,
    logoutWithApi,
    updateUserWithApi
  }
})
