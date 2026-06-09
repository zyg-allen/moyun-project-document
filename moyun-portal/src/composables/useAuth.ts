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
        backdrop-filter: blur(4px);
      `

            const confirmDialog = document.createElement('div')
            confirmDialog.style.cssText = `
        background-color: #ffffff;
        border-radius: 16px;
        padding: 28px;
        max-width: 420px;
        width: 90%;
        box-shadow: 0 20px 60px rgba(0, 0, 0, 0.3);
        animation: slideUp 0.3s ease;
      `

            const iconContainer = document.createElement('div')
            iconContainer.style.cssText = `
        width: 56px;
        height: 56px;
        background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
        border-radius: 50%;
        display: flex;
        align-items: center;
        justify-content: center;
        margin: 0 auto 20px;
      `
            
            const lockIcon = document.createElement('span')
            lockIcon.innerHTML = '🔒'
            lockIcon.style.cssText = 'font-size: 28px;'
            iconContainer.appendChild(lockIcon)

            const title = document.createElement('h3')
            title.textContent = '提示'
            title.style.cssText = `
        margin: 0 0 12px 0;
        font-size: 20px;
        font-weight: 600;
        color: #1a1a1a;
        text-align: center;
      `

            const message = document.createElement('p')
            message.textContent = `${actionName}需要先登录哦，登录后可以享受更多精彩功能~`
            message.style.cssText = `
        margin: 0 0 24px 0;
        font-size: 15px;
        color: #666666;
        line-height: 1.6;
        text-align: center;
      `

            const buttonGroup = document.createElement('div')
            buttonGroup.style.cssText = `
        display: flex;
        gap: 12px;
        justify-content: center;
      `

            const cancelButton = document.createElement('button')
            cancelButton.textContent = '继续浏览'
            cancelButton.style.cssText = `
        padding: 12px 28px;
        border: 2px solid #e0e0e0;
        border-radius: 10px;
        background-color: #ffffff;
        color: #666666;
        font-size: 15px;
        font-weight: 500;
        cursor: pointer;
        transition: all 0.2s;
      `
            cancelButton.addEventListener('mouseenter', () => {
                cancelButton.style.backgroundColor = '#f5f5f5'
                cancelButton.style.borderColor = '#d0d0d0'
            })
            cancelButton.addEventListener('mouseleave', () => {
                cancelButton.style.backgroundColor = '#ffffff'
                cancelButton.style.borderColor = '#e0e0e0'
            })
            cancelButton.addEventListener('click', () => {
                document.body.removeChild(confirmOverlay)
                resolve(false)
            })

            const confirmButton = document.createElement('button')
            confirmButton.textContent = '去登录'
            confirmButton.style.cssText = `
        padding: 12px 28px;
        border: none;
        border-radius: 10px;
        background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
        color: #ffffff;
        font-size: 15px;
        font-weight: 600;
        cursor: pointer;
        transition: all 0.2s;
        box-shadow: 0 4px 12px rgba(102, 126, 234, 0.4);
      `
            confirmButton.addEventListener('mouseenter', () => {
                confirmButton.style.transform = 'translateY(-2px)'
                confirmButton.style.boxShadow = '0 6px 16px rgba(102, 126, 234, 0.5)'
            })
            confirmButton.addEventListener('mouseleave', () => {
                confirmButton.style.transform = 'translateY(0)'
                confirmButton.style.boxShadow = '0 4px 12px rgba(102, 126, 234, 0.4)'
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
          from { opacity: 0; transform: translateY(30px) scale(0.95); }
          to { opacity: 1; transform: translateY(0) scale(1); }
        }
      `
            document.head.appendChild(styleSheet)

            buttonGroup.appendChild(cancelButton)
            buttonGroup.appendChild(confirmButton)
            confirmDialog.appendChild(iconContainer)
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
