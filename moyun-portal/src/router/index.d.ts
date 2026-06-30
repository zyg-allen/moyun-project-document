declare module 'vue-router' {
    interface RouteMeta {
        /**
         * 是否需要登录才能访问
         * @default false
         */
        requiresAuth?: boolean;
        /**
         * 页面标题
         */
        title?: string;
        /**
         * 是否为公开页面（登录后不需要重定向）
         * @default false
         */
        isPublic?: boolean;
        /**
         * robots 元标签，默认允许收录
         */
        robots?: 'noindex,nofollow' | 'noindex,follow' | 'index,follow';
    }
}
declare const router: any;
export default router;
