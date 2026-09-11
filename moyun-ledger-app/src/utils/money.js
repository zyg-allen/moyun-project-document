/**
 * 金额工具：全链路统一"元"（后端 BigDecimal 序列化为数值/字符串），前端直接以"元"为单位运算与展示。
 * v11.31 金额单位统一后，"分"概念已彻底移除，本文件不包含任何 ×100/÷100 换算。
 */

/**
 * 转 Number（字符串/Number 一律转 Number；NaN/空/null → 0）
 */
export const toNum = (v) => {
  if (v === null || v === undefined || v === '') return 0;
  const n = Number(v);
  return Number.isFinite(n) ? n : 0;
};

/** 元 → 2 位小数字符串（专用于 <input> 回显）：1234.56 */
export const toFixedYuan = (yuan) => Number(toNum(yuan)).toFixed(2);

/**
 * 元 → 千分位金额展示（保留负号）：1,234.56  或  -1,234.56
 * 用于总资产 / 总负债 / 净资产 / 结余 / 账户余额 等可能出现负数的展示场景
 */
export const formatAmount = (yuan) => {
  const fixed = Number(toNum(yuan)).toFixed(2);
  const [intPart, fracPart] = fixed.split('.');
  const sign = intPart.charAt(0) === '-' ? '-' : '';
  const absInt = sign ? intPart.slice(1) : intPart;
  const withCommas = absInt.replace(/\B(?=(\d{3})+(?!\d))/g, ',');
  return sign + withCommas + '.' + fracPart;
};

/**
 * 元 → 无符号千分位展示（绝对值）：1,234.56
 * 用于收入/支出 调用方自行拼接 +/- 前缀，或 "欠 ¥xxx" 语义上绝对金额展示
 */
export const formatAbsAmount = (yuan) => formatAmount(Math.abs(toNum(yuan)));

/** 元 → 显式 +/- 千分位展示：+1,234.56 / -1,234.56 / 0.00 */
export const formatSigned = (yuan) => {
  const v = toNum(yuan);
  const abs = formatAbsAmount(v);
  return v > 0 ? '+' + abs : v < 0 ? '-' + abs : abs;
};

/**
 * 安全求和（元单位直接相加，防字符串拼接 / 浮点 / NaN）
 * @param  {Array} items    源数组
 * @param  {Function|string} picker 取值函数或属性名，返回"元"单位
 * @returns {number} 元之和
 */
export const safeSum = (items, picker) => {
  if (!Array.isArray(items) || items.length === 0) return 0;
  const get = typeof picker === 'function'
    ? picker
    : (x) => (picker == null ? x : x[picker]);
  return items.reduce((s, item) => s + toNum(get(item)), 0);
};

/** 记账类型中文 */
export const typeText = (type) => {
  const map = {
    income: '收入', expense: '支出', transfer: '转账',
    repayment: '还款', borrow: '借款', adjust: '校准'
  };
  return map[type] || type;
};