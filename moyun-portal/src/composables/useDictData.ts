import { reactive, onMounted } from 'vue';
import { fetchDictTypes } from '@/api/dict';
import type { DictItem } from '@/api/dict';

// 模块级缓存：同一字典类型全应用只拉取一次（失败也缓存空数组，调用方用本地默认兜底）
const dictCache = new Map<string, DictItem[]>();
// 串行化加载队列：并发调用时按序复查缓存，避免同类型重复请求
let loadChain: Promise<void> = Promise.resolve();

function loadDictTypes(types: string[]): Promise<void> {
  loadChain = loadChain.then(async () => {
    const missing = types.filter(t => !dictCache.has(t));
    if (missing.length === 0) return;
    try {
      const data = await fetchDictTypes(missing);
      Object.entries(data || {}).forEach(([type, items]) => {
        dictCache.set(type, Array.isArray(items) ? items : []);
      });
      // 后端未返回的类型同样标记为空，避免后续重复请求
      missing.forEach(t => {
        if (!dictCache.has(t)) dictCache.set(t, []);
      });
    } catch {
      // 加载失败静默：缓存空数组，调用方用"字典项 || 本地默认"兜底
      missing.forEach(t => dictCache.set(t, []));
    }
  });
  return loadChain;
}

/** 字典 listClass → tailwind 徽章色类；无 listClass 或未匹配时返回空串 */
const LIST_CLASS_BADGE: Record<string, string> = {
  default: 'bg-gray-100 text-gray-700',
  primary: 'bg-blue-100 text-blue-700',
  success: 'bg-green-100 text-green-700',
  warning: 'bg-yellow-100 text-yellow-700',
  danger: 'bg-red-100 text-red-700',
  info: 'bg-sky-100 text-sky-700',
};

export function dictBadgeClass(listClass?: string): string {
  if (!listClass) return '';
  return LIST_CLASS_BADGE[listClass] || '';
}

/**
 * 免登录字典数据组合式函数
 *
 * 用法：const dictMap = useDictData(['portal_question_difficulty', 'portal_question_type'])
 * 返回 reactive 对象：dictMap['portal_question_difficulty'] → DictItem[]（未加载/失败为 []）
 * onMounted 时发起请求；同类型全局只拉取一次（模块级缓存），失败静默返回空
 */
export function useDictData(types: string[]) {
  const dictMap = reactive<Record<string, DictItem[]>>({});
  types.forEach(t => {
    dictMap[t] = dictCache.get(t) || [];
  });

  onMounted(async () => {
    await loadDictTypes(types);
    types.forEach(t => {
      dictMap[t] = dictCache.get(t) || [];
    });
  });

  return dictMap;
}
