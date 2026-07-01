import { ref, onUnmounted } from 'vue';
import { reportReadingProgress, getReadingProgress } from '@/api/reading';
import { useAuth } from '@/composables/useAuth';
/**
 * 阅读进度 composable
 *
 * 设计要点：
 * 1. 节流上报：30s 上报一次，章节切换时强制上报一次
 * 2. 续读恢复：进入章节时调用 restoreProgress 获取上次阅读位置
 * 3. 未登录静默：未登录用户不上报，仅本地保留
 * 4. 容错：上报失败静默忽略，不阻塞阅读
 */
export function useReadingProgress(bookId) {
    const { isAuthenticated } = useAuth();
    const currentChapterId = ref(null);
    const currentChapterNo = ref(0);
    const chapterOffset = ref(0);
    const readingDurationMs = ref(0);
    const lastSavedOffset = ref(-1); // 上次已上报的 offset，避免重复上报相同位置
    let throttleTimer = null;
    let durationTimer = null;
    const THROTTLE_MS = 30000; // 30s 节流
    const DURATION_TICK_MS = 5000; // 5s 累计一次时长
    /**
     * 启动定时上报（章节加载完成后调用）
     */
    function startReporting(chapterId, chapterNo) {
        stopReporting();
        currentChapterId.value = chapterId;
        currentChapterNo.value = chapterNo;
        chapterOffset.value = 0;
        lastSavedOffset.value = -1;
        readingDurationMs.value = 0;
        if (!isAuthenticated())
            return;
        // 节流上报定时器
        throttleTimer = setInterval(() => {
            doReport(false);
        }, THROTTLE_MS);
        // 时长累计定时器
        durationTimer = setInterval(() => {
            readingDurationMs.value += DURATION_TICK_MS;
        }, DURATION_TICK_MS);
    }
    /**
     * 停止定时上报（章节切换/离开页面前调用，会触发最后一次上报）
     * 注意：上报后清空 currentChapterId，防止页面 onUnmounted + composable onUnmounted
     * 双触发时重复调用 doReport(true) 产生冗余上报。
     */
    function stopReporting() {
        if (throttleTimer) {
            clearInterval(throttleTimer);
            throttleTimer = null;
        }
        if (durationTimer) {
            clearInterval(durationTimer);
            durationTimer = null;
        }
        // 章节切换时强制上报一次（带上累计时长）
        if (currentChapterId.value !== null && isAuthenticated()) {
            doReport(true);
            // 上报后清空，防止重复 stopReporting 调用再次触发 doReport
            currentChapterId.value = null;
        }
    }
    /**
     * 更新当前滚动位置（由阅读器滚动事件调用，高频，不触发上报）
     */
    function updateOffset(offset) {
        chapterOffset.value = offset;
    }
    /**
     * 恢复进度：进入章节时调用，返回上次的滚动偏移
     */
    async function restoreProgress() {
        if (!isAuthenticated() || !bookId.value)
            return 0;
        try {
            const resp = await getReadingProgress(bookId.value);
            if (resp.code === 200 && resp.data) {
                const p = resp.data;
                // 仅当上次阅读章节与当前章节一致时恢复 offset
                if (p.currentChapterId !== null && p.currentChapterId !== undefined
                    && String(p.currentChapterId) === String(currentChapterId.value)) {
                    return p.chapterOffset || 0;
                }
            }
        }
        catch (err) {
            // 静默忽略
            console.warn('恢复阅读进度失败:', err);
        }
        return 0;
    }
    /**
     * 实际上报逻辑
     * 上报语义：readingDurationMs 上报"自上次上报以来的增量"，后端累加得到整书累计时长。
     * 因此每次上报成功后必须清零，避免下次上报重复累加。
     * @param force 强制上报（章节切换时）
     */
    async function doReport(force) {
        if (!isAuthenticated() || !bookId.value || currentChapterId.value === null)
            return;
        // 节流：非强制时，若 offset 与上次相同且无新增时长则跳过
        if (!force && chapterOffset.value === lastSavedOffset.value && readingDurationMs.value === 0)
            return;
        const deltaMs = readingDurationMs.value;
        try {
            await reportReadingProgress({
                bookId: bookId.value,
                currentChapterId: currentChapterId.value,
                currentChapterNo: currentChapterNo.value,
                chapterOffset: chapterOffset.value,
                readingDurationMs: deltaMs,
                status: 'reading',
            });
            lastSavedOffset.value = chapterOffset.value;
            // 上报成功后清零累计时长（上报的是增量 delta，后端负责累加）
            readingDurationMs.value = 0;
        }
        catch (err) {
            // 静默忽略上报失败，时长不清零，下次上报时一并补上
        }
    }
    onUnmounted(() => {
        stopReporting();
    });
    return {
        startReporting,
        stopReporting,
        updateOffset,
        restoreProgress,
    };
}
