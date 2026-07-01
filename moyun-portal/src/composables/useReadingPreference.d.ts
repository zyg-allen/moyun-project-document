import type { ReadingPreference } from '@/types/api';
/** 默认偏好 */
export declare const DEFAULT_PREFERENCE: ReadingPreference;
/**
 * 阅读偏好 composable
 *
 * 设计要点：
 * 1. 本地优先：先读 localStorage 立即生效，再异步同步服务端（多端一致）
 * 2. 响应式：preference 为 ref，外部 watch 即可联动 CSS 变量
 * 3. 未登录：仅本地存储，不调用服务端
 * 4. 节流保存：改动后 800ms 防抖保存到服务端
 * 5. 防竞态：服务端覆盖本地时，取消 pending 的防抖保存定时器，避免旧 local 回写覆盖新 serverData
 */
export declare function useReadingPreference(): {
    preference: any;
    loading: any;
    updatePreference: (patch: Partial<ReadingPreference>) => void;
    resetPreference: () => void;
    reloadFromServer: () => Promise<void>;
};
