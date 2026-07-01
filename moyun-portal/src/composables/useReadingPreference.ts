import { ref, watch, onMounted, onUnmounted } from 'vue';
import { getReadingPreference, saveReadingPreference } from '@/api/reading';
import { useAuth } from '@/composables/useAuth';
import type { ReadingPreference } from '@/types/api';

const STORAGE_KEY = 'moyun_reading_preference';

/** 默认偏好 */
export const DEFAULT_PREFERENCE: ReadingPreference = {
  fontSize: 18,
  lineHeight: 1.8,
  theme: 'default',
  fontFamily: 'system',
  letterSpacing: 0,
  paragraphSpacing: 1.2,
};

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
export function useReadingPreference() {
  const { isAuthenticated } = useAuth();
  const preference = ref<ReadingPreference>({ ...DEFAULT_PREFERENCE });
  const loading = ref(false);
  let saveTimer: ReturnType<typeof setTimeout> | null = null;
  let skipNextWatch = false; // 标记是否跳过下一次 watch（服务端同步覆盖时使用）

  /** 从 localStorage 读取偏好 */
  function loadFromLocal(): ReadingPreference | null {
    try {
      const raw = localStorage.getItem(STORAGE_KEY);
      if (raw) {
        return { ...DEFAULT_PREFERENCE, ...JSON.parse(raw) };
      }
    } catch (err) {
      console.warn('读取本地阅读偏好失败:', err);
    }
    return null;
  }

  /** 保存到 localStorage */
  function saveToLocal(pref: ReadingPreference) {
    try {
      localStorage.setItem(STORAGE_KEY, JSON.stringify(pref));
    } catch (err) {
      console.warn('保存本地阅读偏好失败:', err);
    }
  }

  /** 从服务端加载偏好（登录用户） */
  async function loadFromServer() {
    if (!isAuthenticated()) return;
    loading.value = true;
    try {
      const resp = await getReadingPreference();
      if (resp.code === 200 && resp.data) {
        // 关键：取消 pending 的防抖保存定时器，避免旧 local 回写覆盖新 serverData
        if (saveTimer) {
          clearTimeout(saveTimer);
          saveTimer = null;
        }
        skipNextWatch = true;
        preference.value = { ...DEFAULT_PREFERENCE, ...resp.data };
        saveToLocal(preference.value);
      }
    } catch (err) {
      console.warn('加载服务端阅读偏好失败:', err);
    } finally {
      loading.value = false;
    }
  }

  /** 防抖保存到服务端 */
  function debouncedSaveToServer(pref: ReadingPreference) {
    if (!isAuthenticated()) return;
    if (saveTimer) clearTimeout(saveTimer);
    saveTimer = setTimeout(async () => {
      saveTimer = null;
      try {
        await saveReadingPreference(pref);
      } catch (err) {
        // 静默忽略
      }
    }, 800);
  }

  /** 更新偏好（外部调用） */
  function updatePreference(patch: Partial<ReadingPreference>) {
    preference.value = { ...preference.value, ...patch };
  }

  /** 重置为默认偏好 */
  function resetPreference() {
    preference.value = { ...DEFAULT_PREFERENCE };
  }

  // 监听偏好变化：本地立即保存 + 防抖同步服务端
  watch(
    preference,
    (newPref) => {
      if (skipNextWatch) {
        skipNextWatch = false;
        return;
      }
      saveToLocal(newPref);
      debouncedSaveToServer(newPref);
    },
    { deep: true }
  );

  onMounted(async () => {
    // 1. 先读本地，立即生效
    const local = loadFromLocal();
    if (local) {
      skipNextWatch = true; // 避免 loadFromLocal 赋值触发 watch 又写回 localStorage
      preference.value = local;
    }
    // 2. 异步同步服务端（登录用户）
    await loadFromServer();
  });

  // 清理 pending 的防抖定时器，避免离开页面后产生孤儿请求
  onUnmounted(() => {
    if (saveTimer) {
      clearTimeout(saveTimer);
      saveTimer = null;
    }
  });

  return {
    preference,
    loading,
    updatePreference,
    resetPreference,
    reloadFromServer: loadFromServer,
  };
}
