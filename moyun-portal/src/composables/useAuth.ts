import {useRouter} from 'vue-router'
import {useUserStore} from '@/stores/user'

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
                query: {redirect: redirectPath || router.currentRoute.value.fullPath}
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

    /**
     * 执行需要登录的操作（带确认框）
     * 如果未登录，弹出确认框询问是否跳转登录
     * @param action 要执行的操作函数
     * @param actionName 操作名称（用于提示框显示）
     */
    async function withAuthConfirm<T>(
        action: () => Promise<T>,
        actionName: string = '此操作'
    ): Promise<T | null> {
        if (!isAuthenticated()) {
            // 显示确认框
            const confirmed = await showLoginConfirm(actionName)
            if (confirmed) {
                // 用户确认，跳转到登录页，并记住当前路径
                router.push({
                    name: 'login',
                    query: {redirect: router.currentRoute.value.fullPath}
                })
            }
            return null
        }
        return await action()
    }

    /**
     * 显示登录确认框
     * @param actionName 操作名称
     */
    async function showLoginConfirm(actionName: string): Promise<boolean> {
        return new Promise((resolve) => {
            // 创建确认框元素
            const confirmOverlay = document.createElement('div')
            confirmOverlay.style.cssText = `
        position: fixed;
        top: 0;
        left: 0;
        right: 0;
        bottom: 0;
        background-color: rgba(0, 0, 0, 0.5);
        display: flex;
        align-items: center;
        justify-content: center;
        z-index: 9999;
        animation: fadeIn 0.2s ease;
      `

            const confirmDialog = document.createElement('div')
            confirmDialog.style.cssText = `
        background-color: #ffffff;
        border-radius: 12px;
        padding: 24px;
        max-width: 400px;
        width: 90%;
        box-shadow: 0 8px 32px rgba(0, 0, 0, 0.15);
        animation: slideUp 0.2s ease;
      `

            const title = document.createElement('h3')
            title.textContent = '提示'
            title.style.cssText = `
        margin: 0 0 12px 0;
        font-size: 18px;
        font-weight: 600;
        color: #1a1a1a;
      `

            const message = document.createElement('p')
            message.textContent = `${actionName}需要先登录，是否前往登录？`
            message.style.cssText = `
        margin: 0 0 20px 0;
        font-size: 14px;
        color: #666666;
        line-height: 1.6;
      `

            const buttonGroup = document.createElement('div')
            buttonGroup.style.cssText = `
        display: flex;
        gap: 12px;
        justify-content: flex-end;
      `

            const cancelButton = document.createElement('button')
            cancelButton.textContent = '继续浏览'
            cancelButton.style.cssText = `
        padding: 10px 24px;
        border: 1px solid #e0e0e0;
        border-radius: 8px;
        background-color: #ffffff;
        color: #666666;
        font-size: 14px;
        cursor: pointer;
        transition: all 0.2s;
      `
            cancelButton.addEventListener('mouseenter', () => {
                cancelButton.style.backgroundColor = '#f5f5f5'
            })
            cancelButton.addEventListener('mouseleave', () => {
                cancelButton.style.backgroundColor = '#ffffff'
            })
            cancelButton.addEventListener('click', () => {
                document.body.removeChild(confirmOverlay)
                resolve(false)
            })

            const confirmButton = document.createElement('button')
            confirmButton.textContent = '跳转登录'
            confirmButton.style.cssText = `
        padding: 10px 24px;
        border: none;
        border-radius: 8px;
        background-color: #1890ff;
        color: #ffffff;
        font-size: 14px;
        cursor: pointer;
        transition: all 0.2s;
      `
            confirmButton.addEventListener('mouseenter', () => {
                confirmButton.style.backgroundColor = '#40a9ff'
            })
            confirmButton.addEventListener('mouseleave', () => {
                confirmButton.style.backgroundColor = '#1890ff'
            })
            confirmButton.addEventListener('click', () => {
                document.body.removeChild(confirmOverlay)
                resolve(true)
            })

            // 添加动画样式
            const styleSheet = document.createElement('style')
            styleSheet.textContent = `
        @keyframes fadeIn {
          from { opacity: 0; }
          to { opacity: 1; }
        }
        @keyframes slideUp {
          from { opacity: 0; transform: translateY(20px); }
          to { opacity: 1; transform: translateY(0); }
        }
      `
            document.head.appendChild(styleSheet)

            buttonGroup.appendChild(cancelButton)
            buttonGroup.appendChild(confirmButton)
            confirmDialog.appendChild(title)
            confirmDialog.appendChild(message)
            confirmDialog.appendChild(buttonGroup)
            confirmOverlay.appendChild(confirmDialog)
            document.body.appendChild(confirmOverlay)

            // 点击遮罩层关闭
            confirmOverlay.addEventListener('click', (e) => {
                if (e.target === confirmOverlay) {
                    document.body.removeChild(confirmOverlay)
                    resolve(false)
                }
            })
        })
    }

    return {
        isAuthenticated,
        requireAuth,
        checkAuth,
        withAuth,
        withAuthConfirm
    }
}
