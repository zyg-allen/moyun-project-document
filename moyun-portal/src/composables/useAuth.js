import { useRouter } from 'vue-router';
import { useUserStore } from '@/stores/user';
import { showLoginConfirmDialog } from '@/utils/authDialog';
/**
 * 使用权限检查
 */
export function useAuth() {
    const router = useRouter();
    const userStore = useUserStore();
    /**
     * 检查是否已登录
     */
    function isAuthenticated() {
        return userStore.isAuthenticated;
    }
    /**
     * 检查是否已登录，如果未登录则跳转到登录页
     * @param redirectPath 登录后要跳转的路径，默认为当前路径
     */
    function requireAuth(redirectPath) {
        if (!isAuthenticated()) {
            router.push({
                name: 'login',
                query: { redirect: redirectPath || router.currentRoute.value.fullPath }
            });
            return false;
        }
        return true;
    }
    /**
     * 权限检查 - 仅用于按钮/功能级别
     * 如果未登录，显示提示消息（需要在组件中实现显示消息）
     */
    function checkAuth() {
        return isAuthenticated();
    }
    /**
     * 执行需要登录的操作
     * @param action 要执行的操作函数
     * @param onUnauthenticated 未登录时的回调（可选）
     */
    async function withAuth(action, onUnauthenticated) {
        if (!isAuthenticated()) {
            if (onUnauthenticated) {
                onUnauthenticated();
            }
            else {
                // 默认行为：跳转到登录页
                requireAuth();
            }
            return null;
        }
        return await action();
    }
    /**
     * 执行需要登录的操作（带确认框）
     * 如果未登录，弹出确认框询问是否跳转登录
     * @param action 要执行的操作函数
     * @param actionName 操作名称（用于提示框显示）
     */
    async function withAuthConfirm(action, actionName = '此操作') {
        if (!isAuthenticated()) {
            // 显示确认框（复用 authDialog.ts 的统一实现）
            const confirmed = await showLoginConfirmDialog(actionName);
            if (confirmed) {
                // 用户确认，跳转到登录页，并记住当前路径
                router.push({
                    name: 'login',
                    query: { redirect: router.currentRoute.value.fullPath }
                });
            }
            return null;
        }
        return await action();
    }
    return {
        isAuthenticated,
        requireAuth,
        checkAuth,
        withAuth,
        withAuthConfirm
    };
}
