import { useRouter } from 'vue-router'
import { useUserStore } from '@/stores/user'

/**
 * 使用权限检查
 */
export function useAuth() {
  const router = useRouter()
  const userStore = useUserStore()

  /**
   * 检查是否已登录
   */
  function isAuthenticated(): boolean {
    return userStore.isAuthenticated
  }

  /**
   * 检查是否已登录，如果未登录则跳转到登录页
   * @param redirectPath 登录后要跳转的路径，默认为当前路径
   */
  function requireAuth(redirectPath?: string): boolean {
    if (!isAuthenticated()) {
      router.push({
        name: 'login',
        query: { redirect: redirectPath || router.currentRoute.value.fullPath }
      })
      return false
    }
    return true
  }

  /**
   * 权限检查 - 仅用于按钮/功能级别
   * 如果未登录，显示提示消息（需要在组件中实现显示消息）
   */
  function checkAuth(): boolean {
    return isAuthenticated()
  }

  /**
   * 执行需要登录的操作
   * @param action 要执行的操作函数
   * @param onUnauthenticated 未登录时的回调（可选）
   */
  async function withAuth<T>(
    action: () => Promise<T>,
    onUnauthenticated?: () => void
  ): Promise<T | null> {
    if (!isAuthenticated()) {
      if (onUnauthenticated) {
        onUnauthenticated()
      } else {
        // 默认行为：跳转到登录页
        requireAuth()
      }
      return null
    }
    return await action()
  }

  return {
    isAuthenticated,
    requireAuth,
    checkAuth,
    withAuth
  }
}
