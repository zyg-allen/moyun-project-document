/**
 * 金额工具：后端统一"元"（BigDecimal，序列化为数值/字符串），前端直接以"元"为单位运算与展示。
 * 全链路不再有"分"概念，所有函数名保留 cent/yuan 仅为向后兼容，实际均按"元"处理。
 */

/**
 * 转 Number（字符串/Number 一律转 Number；NaN/空/null → 0）
 */
export const toNum = (v) => {
  if (v === null || v === undefined || v === '') return 0;
  const n = Number(v);
  return Number.isFinite(n) ? n : 0;
};

/**
 * 元 → 元（恒等映射，仅为向后兼容保留函数名，不再 ×100）
 */
export const yuanToCent = (yuan) => toNum(yuan);

/** 元 → 无千分位 2 位小数字符串（专用于 <input> 回显）：1234.56 */
export const centToYuan = (cent) => Number(toNum(cent)).toFixed(2);

/**
 * 元 → 千分位金额展示（保留负号）：1,234.56  或  -1,234.56
 * 用于总资产 / 总负债 / 净资产 / 结余 / 账户余额 等可能出现负数的展示场景
 */
export const centToAmount = (cent) => {
  const fixed = Number(toNum(cent)).toFixed(2);
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
export const centToAbsAmount = (cent) => centToAmount(Math.abs(toNum(cent)));

/** 元 → 显式 +/- 千分位展示：+1,234.56 / -1,234.56 / 0.00 */
export const centToSigned = (cent) => {
  const c = toNum(cent);
  const abs = centToAbsAmount(c);
  return c > 0 ? '+' + abs : c < 0 ? '-' + abs : abs;
};

/**
 * 安全求和（元单位直接相加，防字符串拼接 / 浮点 / NaN）
 * @param  {Array} items    源数组
 * @param  {Function|string} picker 取值函数或属性名，返回"元"单位
 * @returns {number} 元之和
 */
export const safeSumCents = (items, picker) => {
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
