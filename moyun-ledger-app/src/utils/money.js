/**
 * 金额工具：后端统一"分"（整数），前端展示"元"
 * 所有输入输出转换必须经过本模块，禁止页面内手写 *100 /100
 */

/** 元（字符串/数字）→ 分（整数，四舍五入防浮点误差） */
export const yuanToCent = (yuan) => {
  const num = Number(yuan);
  if (isNaN(num)) return 0;
  return Math.round(num * 100);
};

/** 分 → 元（保留2位小数的字符串） */
export const centToYuan = (cent) => {
  const c = Number(cent) || 0;
  return (c / 100).toFixed(2);
};

/** 分 → 带符号展示：+1,234.56 / -1,234.56 */
export const centToSigned = (cent) => {
  const c = Number(cent) || 0;
  const abs = Math.abs(c) / 100;
  const fixed = abs.toLocaleString('zh-CN', { minimumFractionDigits: 2, maximumFractionDigits: 2 });
  return (c > 0 ? '+' : c < 0 ? '-' : '') + fixed;
};

/** 分 → 千分位展示：1,234.56 */
export const centToAmount = (cent) => {
  const c = Number(cent) || 0;
  return (Math.abs(c) / 100).toLocaleString('zh-CN', {
    minimumFractionDigits: 2,
    maximumFractionDigits: 2
  });
};

/** 记账类型 → 中文 */
export const typeText = (type) => {
  const map = {
    income: '收入', expense: '支出', transfer: '转账',
    repayment: '还款', borrow: '借款', adjust: '校准'
  };
  return map[type] || type;
};
