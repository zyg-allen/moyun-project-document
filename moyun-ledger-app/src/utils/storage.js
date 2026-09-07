/**
 * 本地存储工具（存钱计划 / 定时记账 / 备忘录 / 打赏 等小功能的前端持久化）
 * 统一 key 前缀，避免与其他模块冲突
 */
const PREFIX = 'ledger_';

export const storage = {
  get(key, def = null) {
    try {
      const v = uni.getStorageSync(PREFIX + key);
      return v === '' || v === null || v === undefined ? def : v;
    } catch (e) { return def; }
  },
  set(key, val) {
    try { uni.setStorageSync(PREFIX + key, val); } catch (e) { /* ignore */ }
  },
  remove(key) {
    try { uni.removeStorageSync(PREFIX + key); } catch (e) { /* ignore */ }
  }
};

/** 生成唯一 ID */
export const uid = () => Date.now().toString(36) + Math.random().toString(36).slice(2, 8);
