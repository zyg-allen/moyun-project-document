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

const KEY = 'ledger_theme';

export const useThemeStore = defineStore('theme', {
  state: () => ({
    current: uni.getStorageSync(KEY) || 'mint'
  }),
  getters: {
    themeKey(state) { return THEMES[state.current] ? state.current : 'mint'; },
    theme() { return THEMES[this.themeKey]; },
    /** 页面根元素 :style 绑定 */
    themeVars() { return this.theme; },
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
    restore() { this.setTheme(this.current); }
  }
});
