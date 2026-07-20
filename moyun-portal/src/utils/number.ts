/**
 * 格式化数字显示（如 1.2k、12k）
 */
export function formatNumber(n: number | undefined | null): string {
  if (!n) return '0';
  if (n < 1000) return String(n);
  if (n < 10000) return (n / 1000).toFixed(1) + 'k';
  return (n / 10000).toFixed(1) + 'w';
}
