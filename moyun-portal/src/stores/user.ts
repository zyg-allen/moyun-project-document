import { defineStore } from 'pinia'
import { ref, computed } from 'vue'
import type { User } from '@/types'
import * as userApi from '@/api/user'
import { getToken, setToken, removeToken } from '@/api/client'

export const useUserStore = defineStore('user', () => {
  // State
  const user = ref<User | null>(null)
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

  async function registerWithApi(params: { username: string; email: string; password: string }) {
    loading.value = true
    try {
      const response = await userApi.register(params)
      if (response.code === 200) {
        setToken(response.data.token)
        user.value = response.data.user as User
        return { success: true }
      }
      return { success: false, message: response.message }
    } catch (error) {
      console.error('注册失败:', error)
      return { success: false, message: '注册失败，请重试' }
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
    }
  }

  async function updateUserWithApi(updates: Partial<User>) {
    loading.value = true
    try {
      const response = await userApi.updateUserProfile(updates as any)
      if (response.code === 200) {
        user.value = response.data as User
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

  return {
    // State
    user,
    isAuthenticated,
    loading,
    // Getters
    username,
    avatar,
    // Actions
    fetchCurrentUser,
    loginWithApi,
    registerWithApi,
    logoutWithApi,
    updateUserWithApi
  }
})
