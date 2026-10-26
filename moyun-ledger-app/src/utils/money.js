/**
 * 金额工具：全链路统一"元"（后端 BigDecimal 序列化为数值/字符串），前端直接以"元"为单位运算与展示。
 * 金额单位统一后，"分"概念已彻底移除，本文件不包含任何 ×100/÷100 换算。
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
/** 金额 + 元单位（统一展示口径） */
export const formatYuan = (v) => formatAmount(v) + ' 元';
export const formatAbsYuan = (v) => formatAbsAmount(v) + ' 元';

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

/**
 * 金额输入净化（用于 <input type="digit"> 的 @input）—— 返回"应当显示"的合法文本。
 *
 * ## 为什么必须前端做
 *
 * uni-app 的 `type="digit"` 在 H5 只是 `inputmode="decimal"`（**只影响键盘布局，不阻止输入**），
 * 用户仍可粘贴或输入 `-`、多个小数点、超长数字；而这些最终由后端校验兜底，
 * 用户看到的是服务端错误串（如"初始余额超出允许范围（0 ~ 100 亿元）"）而不是行内纠正。
 *
 * ## 规则
 *
 * - 只保留数字与小数点，去掉其它字符（含 `-`，除非 allowNegative）
 * - 只保留**第一个**小数点，小数位截断到 decimalPlaces
 * - 去掉多余前导 0（保留 "0." 形态）
 * - 超过 max 时截断为 max
 * - 保留"中间态"（如 "12." 与 "."），否则用户无法继续输入小数
 *
 * @param {string|number} value 输入框当前值
 * @param {object} [opts]
 * @param {number} [opts.decimalPlaces=2] 最多小数位
 * @param {number} [opts.max] 最大值（超出即截断）
 * @param {boolean} [opts.allowNegative=false] 是否允许负号（如"每期比上期多存"需允许负数）
 * @returns {string} 净化后的文本
 */
export const sanitizeAmountInput = (value, opts) => {
  const o = opts || {};
  const dp = o.decimalPlaces == null ? 2 : o.decimalPlaces;
  const allowNegative = !!o.allowNegative;
  let v = String(value == null ? '' : value);

  // 负号只允许出现在首位
  const negative = allowNegative && v.trim().charAt(0) === '-';
  v = v.replace(/[^0-9.]/g, '');

  const firstDot = v.indexOf('.');
  if (firstDot >= 0) {
    v = v.slice(0, firstDot + 1) + v.slice(firstDot + 1).replace(/\./g, '');
    if (dp === 0) {
      v = v.slice(0, firstDot);
    } else {
      v = v.slice(0, firstDot) + '.' + v.slice(firstDot + 1, firstDot + 1 + dp);
    }
  }

  // 去掉多余前导 0（"007" → "7"，但 "0." 与 "0" 保留）
  if (/^0[0-9]/.test(v)) v = v.replace(/^0+/, '');

  if (o.max != null && v !== '' && v !== '.') {
    const n = Number(v);
    if (Number.isFinite(n) && n > o.max) v = String(o.max);
  }

  return (negative && v !== '' ? '-' : '') + v;
};

/**
 * 把净化后的值回写到输入框（仅在确实被改动时）。
 *
 * `@input` 里用 `v-model` 时不能直接改 `e.detail.value`，
 * 必须通过 `event.target.value`（H5）回写，且要保持光标位置，否则光标会跳到末尾。
 *
 * @param {Event} event uni-app input 事件
 * @param {object} vm 组件实例（this）
 * @param {string} prop v-model 绑定的字段名（支持 'form.amount' 点路径）
 * @param {object} [opts] 同 sanitizeAmountInput
 */
export const applyAmountInput = (event, vm, prop, opts) => {
  const raw = (event && event.detail && event.detail.value) || '';
  const clean = sanitizeAmountInput(raw, opts);
  if (clean === raw) return;
  // 点路径写入
  const parts = String(prop).split('.');
  let target = vm;
  for (let i = 0; i < parts.length - 1; i++) target = target[parts[i]];
  target[parts[parts.length - 1]] = clean;
  // 回写输入框并保持光标（H5 才有效；小程序端由框架在下一帧同步）
  const el = event && event.target;
  if (el && 'value' in el) {
    const pos = clean.length;
    el.value = clean;
    if (typeof el.setSelectionRange === 'function') {
      try { el.setSelectionRange(pos, pos); } catch (e) { /* 非文本输入框忽略 */ }
    }
  }
};
