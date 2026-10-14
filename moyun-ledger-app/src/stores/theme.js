import { defineStore } from 'pinia';

/**
 * 主题配置：CSS 变量注入页面根元素，全页面 var(--xxx) 引用
 * primary        按钮底/Tab激活底/选中描边（白字）
 * primary-strong 深一档主色（金额强调/hover/进度）
 * primary-soft   浅背景（hint条/选中背景）
 */
export const THEMES = {
  mint: {
    name: '薄荷绿',
    '--primary': '#7FBF94',
    '--primary-strong': '#5FA77A',
    '--primary-soft': '#E9F5EE',
    '--primary-shadow': 'rgba(127, 191, 148, 0.35)'
  },
  violet: {
    name: '紫罗兰',
    '--primary': '#8A6FE8',
    '--primary-strong': '#6A4FD4',
    '--primary-soft': '#F0EBFB',
    '--primary-shadow': 'rgba(138, 111, 232, 0.35)'
  },
  ocean: {
    name: '海洋蓝',
    '--primary': '#5B9BD5',
    '--primary-strong': '#3F7FBF',
    '--primary-soft': '#E8F1F9',
    '--primary-shadow': 'rgba(91, 155, 213, 0.35)'
  },
  peach: {
    name: '蜜桃橙',
    '--primary': '#F09A5A',
    '--primary-strong': '#D97F35',
    '--primary-soft': '#FBF0E7',
    '--primary-shadow': 'rgba(240, 154, 90, 0.35)'
  },
  sakura: {
    name: '樱花粉',
    '--primary': '#EE8FA9',
    '--primary-strong': '#D96E8C',
    '--primary-soft': '#FBEFF3',
    '--primary-shadow': 'rgba(238, 143, 169, 0.35)'
  }
};

/**
 * 外观主题（明/暗/护眼）——与上面的「强调色」是**正交的两个维度**：
 *   强调色 决定主色（--primary 系列），5 种可选；
 *   外观   决定背景/卡面/文字/边框/收支语义色，3 种可选。
 * 二者叠加（外观先注入、强调色后注入），故 --primary 系列始终以强调色为准，
 * 保证既有 5 套配色与 TabBar 联动行为完全不变。
 *
 * 色值与门户（moyun-portal/src/style.css）保持同一套设计语言，
 * 并已通过 WCAG 对比度校验（正文 >=4.5，语义色 >=3.0）。
 */
export const APPEARANCES = {
  light: {
    name: '日间',
    '--bg': '#F7F8FA',
    '--card': '#FFFFFF',
    '--surface': '#F0F2F6',
    '--border': '#E5E8EF',
    '--border-strong': '#D3D8E2',
    '--text-main': '#1A1D26',
    '--text-sub': '#6B7280',
    '--text-muted': '#9CA3AF',
    '--income': '#0A7F5F',
    '--income-bg': '#E6F5EF',
    '--expense': '#E8654A',
    '--expense-bg': '#FDEEE9'
    '--warning': '#B45309',
    '--warning-bg': '#FEF3C7',
    '--info': '#1D4ED8',
    '--info-bg': '#DBEAFE',
  },
  dark: {
    name: '暗色',
    '--bg': '#0F1419',
    '--card': '#1A2029',
    '--surface': '#232B36',
    '--border': '#37424F',
    '--border-strong': '#47556A',
    '--text-main': '#F1F5F9',
    '--text-sub': '#94A3B8',
    '--text-muted': '#64748B',
    '--income': '#34D399',
    '--income-bg': 'rgba(52, 211, 153, 0.16)',
    '--expense': '#F87171',
    '--expense-bg': 'rgba(248, 113, 113, 0.16)'
    '--warning': '#FBBF24',
    '--warning-bg': 'rgba(251, 191, 36, 0.16)',
    '--info': '#60A5FA',
    '--info-bg': 'rgba(96, 165, 250, 0.16)',
  },
  eye: {
    name: '护眼',
    '--bg': '#F4F1EA',
    '--card': '#FBF9F4',
    '--surface': '#ECE7DA',
    '--border': '#DDD6C8',
    '--border-strong': '#C9C1AE',
    '--text-main': '#2D2A24',
    '--text-sub': '#6E6759',
    '--text-muted': '#9A917F',
    '--income': '#3F7445',
    '--income-bg': '#E6EFE2',
    '--expense': '#B8654A',
    '--expense-bg': '#F6E8E2'
    '--warning': '#8A5A22',
    '--warning-bg': '#F0E6D2',
    '--info': '#3F5E86',
    '--info-bg': '#DFE7F0',
  }
};

const APPEARANCE_KEY = 'ledger_appearance';

const KEY = 'ledger_theme';

export const useThemeStore = defineStore('theme', {
  state: () => ({
    current: uni.getStorageSync(KEY) || 'mint',
    appearance: uni.getStorageSync(APPEARANCE_KEY) || 'light'
  }),
  getters: {
    themeKey(state) { return THEMES[state.current] ? state.current : 'mint'; },
    theme() { return THEMES[this.themeKey]; },
    /** 页面根元素 :style 绑定 */
    appearanceKey(state) { return APPEARANCES[state.appearance] ? state.appearance : 'light'; },
    appearanceTheme() { return APPEARANCES[this.appearanceKey]; },
    appearanceName() { return this.appearanceTheme.name; },
    appearanceList() {
      return Object.keys(APPEARANCES).map(k => ({ key: k, name: APPEARANCES[k].name }));
    },
    /**
     * 页面根元素 :style 绑定。
     * 先注入外观层（背景/卡面/文字/边框/收支），再注入强调色层 ——
     * 后者覆盖 --primary 系列，故强调色始终优先，行为与改造前一致。
     */
    themeVars() { return { ...this.appearanceTheme, ...this.theme }; },
    themeName() { return this.theme.name; },
    themeList() { return Object.keys(THEMES).map(k => ({ key: k, name: THEMES[k].name, color: THEMES[k]['--primary'] })); }
  },
  actions: {
    setTheme(key) {
      if (!THEMES[key]) return;
      this.current = key;
      uni.setStorageSync(KEY, key);
      // TabBar 联动（H5/小程序均支持）；非 TabBar 页调用会失败，静默即可
      // （切回 Tab 页时由各 Tab 页 onShow 的 restore() 重新同步）
      const t = THEMES[key];
      uni.setTabBarStyle({
        selectedColor: t['--primary-strong'],
        fail: () => {}
      });
    },
    /** 启动时恢复 TabBar 选中色 */
    /** 切换外观（日间/暗色/护眼）；页面根元素由 themeVars 自动跟随 */
    setAppearance(key) {
      if (!APPEARANCES[key]) return;
      this.appearance = key;
      uni.setStorageSync(APPEARANCE_KEY, key);
    },
    restore() { this.setTheme(this.current); }
  }
});
