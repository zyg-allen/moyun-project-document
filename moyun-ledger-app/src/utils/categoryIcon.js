/**
 * 分类图标统一映射：分类 icon 标识 → emoji
 * 供「记一笔-分类选择」「分类管理」「报表」等页面共用，保证图标风格统一
 */
export const ICON_MAP = {
  // 支出
  food: '🍜', transport: '🚌', shopping: '🛍️', home: '🏠', entertainment: '🎮',
  medical: '💊', education: '📚', phone: '📱', daily: '🧻', gift: '🎁',
  pet: '🐾', travel: '✈️', 'house-loan': '🏦', 'car-loan': '🚗', repayment: '💳',
  interest: '📈', other: '🔖',
  // 收入
  salary: '💰', bonus: '🎉', parttime: '💼', invest: '📊', redpacket: '🧧',
  refund: '↩️', 'borrow-in': '🤝', secondhand: '♻️',
  // 转账
  'transfer-self': '🔄', 'transfer-friend': '👥', 'transfer-proxy': '🔀',
  'transfer-refund': '↩️', 'transfer-other': '🔖',
  // 还款
  'repay-card': '💳', 'repay-loan': '🏦', 'repay-personal': '🤝',
  'repay-interest': '📈', 'repay-other': '🔖',
  // 借款
  'borrow-card': '💳', 'borrow-online': '🌐', 'borrow-bank': '🏦',
  'borrow-installment': '📅', 'borrow-personal': '🤝', 'borrow-other': '🔖',
  // 校准
  'adjust-balance': '⚖️', 'adjust-fee': '💸', 'adjust-fx': '💱', 'adjust-other': '🔖'
};

/** 分类 icon 标识 → emoji（未命中返回兜底图标） */
export function categoryIcon(icon) {
  return ICON_MAP[icon] || '🏷️';
}

/**
 * 将分类颜色转为「浅色底」，用于图标容器背景，避免与 emoji 撞色。
 * 例如 #7FBF94 -> rgba(127,191,148,0.18)
 */
export function softColor(hex) {
  if (!hex) return 'rgba(127,191,148,0.18)';
  let h = String(hex).replace('#', '').trim();
  if (h.length === 3) h = h.split('').map(c => c + c).join('');
  if (!/^[0-9a-fA-F]{6}$/.test(h)) return 'rgba(127,191,148,0.18)';
  const r = parseInt(h.slice(0, 2), 16);
  const g = parseInt(h.slice(2, 4), 16);
  const b = parseInt(h.slice(4, 6), 16);
  return `rgba(${r},${g},${b},0.18)`;
}
