/**
 * 列表展示用的文本截断工具
 *
 * 为什么需要：备注（description）是自由文本，用户可能粘贴很长的内容
 * （例如从别处复制的整段说明）。此前总览页的流水行既没有 `text-overflow`
 * 也没有 `min-width: 0`，长备注会把金额直接挤出屏幕；
 * 而用户期望的是「只显示前面 8~10 个字」，不是按容器宽度弹性伸缩。
 * 因此这里做**硬截断**，各行 CSS 的 `ellipsis` 作为第二道防线。
 */

/**
 * 截断文本，超长时保留前 max 个字符并追加省略号。
 *
 * 用 `Array.from` 迭代而不是 `slice`：避免把 emoji / 代理对从中间截断
 * （`'😀😀'.slice(0,1)` 会得到半个字符，渲染成乱码方块）。
 *
 * @param {string} text 原始文本
 * @param {number} max  最多保留的字符数，默认 10
 * @returns {string}
 */
export const truncate = (text, max = 10) => {
  if (text === null || text === undefined) return '';
  const chars = Array.from(String(text));
  if (chars.length <= max) return chars.join('');
  return chars.slice(0, max).join('') + '…';
};
