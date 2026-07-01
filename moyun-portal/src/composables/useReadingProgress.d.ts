import { type Ref } from 'vue';
/**
 * 阅读进度 composable
 *
 * 设计要点：
 * 1. 节流上报：30s 上报一次，章节切换时强制上报一次
 * 2. 续读恢复：进入章节时调用 restoreProgress 获取上次阅读位置
 * 3. 未登录静默：未登录用户不上报，仅本地保留
 * 4. 容错：上报失败静默忽略，不阻塞阅读
 */
export declare function useReadingProgress(bookId: Ref<string | number | undefined>): {
    startReporting: (chapterId: string | number, chapterNo: number) => void;
    stopReporting: () => void;
    updateOffset: (offset: number) => void;
    restoreProgress: () => Promise<number>;
};
