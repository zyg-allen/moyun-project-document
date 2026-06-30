/**
 * 使用权限检查
 */
export declare function useAuth(): {
    isAuthenticated: () => boolean;
    requireAuth: (redirectPath?: string) => boolean;
    checkAuth: () => boolean;
    withAuth: <T>(action: () => Promise<T>, onUnauthenticated?: () => void) => Promise<T | null>;
    withAuthConfirm: <T>(action: () => Promise<T>, actionName?: string) => Promise<T | null>;
};
