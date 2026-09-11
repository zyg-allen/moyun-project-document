import { ref, readonly } from 'vue';
import { getInterviewHint, getInterviewKeywords } from '@/api/voiceInterview';
import type { HintLevel, HintVO } from '@/api/voiceInterview';
import { useApiCall } from '@/composables/useApiCall';

/**
 * 面试提示引擎 Composable —— V10.0 语音面试官
 *
 * <p>封装 HintEngine 后端接口调用，提供：
 * <ul>
 *   <li>分级提示获取：fetchHint(questionId, level)</li>
 *   <li>关键词获取：fetchKeywords(questionId)</li>
 *   <li>本地缓存：同 questionId+level 的提示只请求一次，避免重复调用</li>
 *   <li>逐级升级：upgradeHint(questionId) 从 level1 依次升级到 level3</li>
 *   <li>降级提示级别：downgradeHint(questionId) 从当前级别降一级</li>
 * </ul>
 *
 * <p>缓存策略：Map<questionId, Map<level, HintVO>>，组件销毁时随之回收。
 * 若题目内容变更（后台编辑），需调用 clearCache(questionId) 清除。
 *
 * @author moyun
 */
export function useInterviewHint() {
  const { run } = useApiCall();

  /** 缓存：questionId -> (level -> HintVO) */
  const hintCache = new Map<string | number, Map<number, HintVO>>();
  /** 关键词缓存：questionId -> string[] */
  const keywordCache = new Map<string | number, string[]>();
  /** 当前题目的提示级别（用于升级/降级） */
  const currentLevel = ref<HintLevel>(1);
  /** 当前提示对象 */
  const currentHint = ref<HintVO | null>(null);
  /** 加载状态 */
  const loading = ref(false);
  /** 错误信息 */
  const error = ref('');

  /** 从缓存读取 */
  function getCachedHint(questionId: string | number, level: number): HintVO | null {
    const levelMap = hintCache.get(questionId);
    return levelMap ? levelMap.get(level) || null : null;
  }

  /** 写入缓存 */
  function setCachedHint(questionId: string | number, hint: HintVO) {
    if (!hintCache.has(questionId)) {
      hintCache.set(questionId, new Map());
    }
    hintCache.get(questionId)!.set(hint.level, hint);
  }

  /**
   * 获取分级提示（带缓存）
   * @param questionId 题目 ID
   * @param level      提示级别 1~3
   * @param force      是否强制刷新缓存
   */
  async function fetchHint(
    questionId: string | number,
    level: HintLevel = 1,
    force = false,
  ): Promise<HintVO | null> {
    if (!force) {
      const cached = getCachedHint(questionId, level);
      if (cached) {
        currentLevel.value = level;
        currentHint.value = cached;
        return cached;
      }
    }
    loading.value = true;
    error.value = '';
    const { data: resp, success } = await run(() => getInterviewHint(questionId, level), {
      silent: true,
      errorToast: '提示获取失败',
    });
    loading.value = false;
    if (success && resp?.data) {
      const hint = resp.data;
      setCachedHint(questionId, hint);
      currentLevel.value = level;
      currentHint.value = hint;
      return hint;
    }
    error.value = '提示获取失败';
    return null;
  }

  /**
   * 获取关键词（带缓存）
   */
  async function fetchKeywords(
    questionId: string | number,
    force = false,
  ): Promise<string[] | null> {
    if (!force && keywordCache.has(questionId)) {
      return keywordCache.get(questionId)!;
    }
    loading.value = true;
    error.value = '';
    const { data: resp, success } = await run(() => getInterviewKeywords(questionId), {
      silent: true,
    });
    loading.value = false;
    if (success && resp?.data) {
      const keywords = resp.data;
      keywordCache.set(questionId, keywords);
      return keywords;
    }
    error.value = '关键词获取失败';
    return null;
  }

  /**
   * 升级提示级别（level+1，最高 3）
   * @param questionId 题目 ID
   */
  async function upgradeHint(questionId: string | number): Promise<HintVO | null> {
    const next = Math.min(3, currentLevel.value + 1) as HintLevel;
    if (next === currentLevel.value) {
      return currentHint.value;
    }
    return fetchHint(questionId, next);
  }

  /**
   * 降级提示级别（level-1，最低 1）
   * @param questionId 题目 ID
   */
  async function downgradeHint(questionId: string | number): Promise<HintVO | null> {
    const prev = Math.max(1, currentLevel.value - 1) as HintLevel;
    if (prev === currentLevel.value) {
      return currentHint.value;
    }
    return fetchHint(questionId, prev);
  }

  /** 清除指定题目的缓存 */
  function clearCache(questionId?: string | number) {
    if (questionId === undefined) {
      hintCache.clear();
      keywordCache.clear();
    } else {
      hintCache.delete(questionId);
      keywordCache.delete(questionId);
    }
  }

  /** 重置当前状态 */
  function reset() {
    currentLevel.value = 1;
    currentHint.value = null;
    error.value = '';
    loading.value = false;
  }

  return {
    currentLevel: readonly(currentLevel),
    currentHint: readonly(currentHint),
    loading: readonly(loading),
    error: readonly(error),
    fetchHint,
    fetchKeywords,
    upgradeHint,
    downgradeHint,
    clearCache,
    reset,
  };
}
