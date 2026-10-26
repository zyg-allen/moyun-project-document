/** 时间选择用的固定选项（00~23 / 00~59） */
export const pad2 = (n) => (n < 10 ? '0' + n : '' + n);
export const HOURS = Array.from({ length: 24 }, (_, i) => pad2(i));
export const MINUTES = Array.from({ length: 60 }, (_, i) => pad2(i));
