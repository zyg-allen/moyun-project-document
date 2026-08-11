/**
 * 身份证号校验工具
 *
 * 与后端 com.moyun.util.string.IdCardUtil 保持一致算法：
 *   - 18 位格式（前 17 数字，末位数字或 X）
 *   - 出生日期段合法（不晚于今天）
 *   - 校验位 ISO 7064:1983 MOD 11-2
 *
 * 旧版 15 位身份证号已停发，一律视为非法。
 */

/** MOD 11-2 加权因子 */
const WEIGHT = [7, 9, 10, 5, 8, 4, 2, 1, 6, 3, 7, 9, 10, 5, 8, 4, 2];

/** MOD 11-2 校验位字符表（按余数 0-10 索引） */
const CHECK_CODE = ['1', '0', 'X', '9', '8', '7', '6', '5', '4', '3', '2'];

/** 18 位身份证号格式（前 17 位数字，末位数字或 X） */
const ID_18_PATTERN = /^\d{17}[\dXx]$/;

/**
 * 校验身份证号合法性
 * @param idCard 身份证号
 * @returns true 合法；false 非法
 */
export function isValidIdCard(idCard: string | null | undefined): boolean {
  if (!idCard) return false;
  const value = idCard.trim();
  if (!ID_18_PATTERN.test(value)) return false;
  if (!isValidBirthDate(value.substring(6, 14))) return false;
  return checkCodeMatches(value);
}

/**
 * 校验身份证号合法性，非法时返回错误原因
 * @param idCard 身份证号
 * @returns null 合法；非空字符串为错误原因
 */
export function validateIdCard(idCard: string | null | undefined): string | null {
  if (!idCard || !idCard.trim()) return '身份证号不能为空';
  const value = idCard.trim();
  if (value.length !== 18) return '身份证号长度必须为 18 位';
  if (!ID_18_PATTERN.test(value)) {
    return '身份证号格式错误：前 17 位必须为数字，第 18 位为数字或 X';
  }
  if (!isValidBirthDate(value.substring(6, 14))) {
    return '身份证号中的出生日期非法';
  }
  if (!checkCodeMatches(value)) {
    return '身份证号校验位错误';
  }
  return null;
}

/**
 * 从身份证号中解析出生日期（YYYY-MM-DD）
 */
export function getBirthDateFromIdCard(idCard: string | null | undefined): string | null {
  if (!isValidIdCard(idCard)) return null;
  const value = idCard!.trim();
  const y = value.substring(6, 10);
  const m = value.substring(10, 12);
  const d = value.substring(12, 14);
  return `${y}-${m}-${d}`;
}

/**
 * 从身份证号中解析性别（奇数=男，偶数=女）
 * @returns 'M' 男 / 'F' 女 / null 非法
 */
export function getGenderFromIdCard(idCard: string | null | undefined): 'M' | 'F' | null {
  if (!isValidIdCard(idCard)) return null;
  const genderDigit = parseInt(idCard!.charAt(16), 10);
  return genderDigit % 2 === 1 ? 'M' : 'F';
}

function isValidBirthDate(yyyymmdd: string): boolean {
  const y = parseInt(yyyymmdd.substring(0, 4), 10);
  const m = parseInt(yyyymmdd.substring(4, 6), 10);
  const d = parseInt(yyyymmdd.substring(6, 8), 10);
  if (m < 1 || m > 12) return false;
  if (d < 1 || d > 31) return false;
  const date = new Date(`${y}-${String(m).padStart(2, '0')}-${String(d).padStart(2, '0')}T00:00:00`);
  if (Number.isNaN(date.getTime())) return false;
  // 校验日历合法性（防止 02-31 这类无效日期被 new Date 容错接受）
  if (date.getFullYear() !== y || date.getMonth() + 1 !== m || date.getDate() !== d) {
    return false;
  }
  // 不允许未来日期
  const today = new Date();
  today.setHours(0, 0, 0, 0);
  return date.getTime() <= today.getTime();
}

function checkCodeMatches(idCard: string): boolean {
  const expected = calculateCheckCode(idCard.substring(0, 17));
  const actual = idCard.charAt(17).toUpperCase();
  return expected === actual;
}

function calculateCheckCode(idCard17: string): string {
  let sum = 0;
  for (let i = 0; i < 17; i++) {
    sum += parseInt(idCard17.charAt(i), 10) * WEIGHT[i];
  }
  return CHECK_CODE[sum % 11];
}
