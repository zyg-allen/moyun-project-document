/**
 * 图片懒加载 hook
 */
export declare function useLazyLoad(): {
    observe: (el: HTMLElement) => void;
    unobserve: (el: HTMLElement) => void;
};
/**
 * 防抖函数
 */
export declare function debounce<T extends (...args: any[]) => any>(fn: T, delay: number): (...args: Parameters<T>) => void;
/**
 * 节流函数
 */
export declare function throttle<T extends (...args: any[]) => any>(fn: T, limit: number): (...args: Parameters<T>) => void;
/**
 * 视口尺寸 hook
 */
export declare function useViewport(): {
    width: any;
    height: any;
};
/**
 * 文档标题管理
 */
export declare function useDocumentTitle(defaultTitle?: string): {
    setTitle: (title: string) => void;
};
/**
 * 页面可见性 hook
 */
export declare function usePageVisibility(): {
    isVisible: any;
};
