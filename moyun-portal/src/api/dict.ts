import { httpGet } from './client';

/** 免登录字典项 */
export interface DictItem {
  dictLabel: string;
  dictValue: string;
  /** 徽章样式标识（default/primary/success/warning/danger 等） */
  listClass?: string;
  /** 排序号 */
  dictSort?: number;
}

/**
 * 批量获取免登录字典数据
 * GET /portal/dict/types?types=typeA,typeB → { typeA: [DictItem...], typeB: [...] }
 * 仅支持 portal_/cms_ 前缀字典类型；失败时抛错，由调用方决定兜底策略
 */
export const fetchDictTypes = async (types: string[]): Promise<Record<string, DictItem[]>> => {
  if (!types || types.length === 0) return {};
  const res = await httpGet<Record<string, DictItem[]>>('/portal/dict/types', {
    types: types.join(','),
  });
  if (res.code === 200 && res.data) {
    return res.data;
  }
  return {};
};
