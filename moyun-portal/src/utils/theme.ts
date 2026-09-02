import { safeLocalStorage } from './security'

export type Theme = 'light' | 'dark' | 'eye';

interface ThemeConfig {
  name: string;
  icon: string;
  id: string;
}

export const themes: Record<Theme, ThemeConfig> = {
  light: {
    name: '日间主题',
    icon: 'sun',
    id: 'light',
  },
  dark: {
    name: '夜间主题',
    icon: 'moon',
    id: 'dark',
  },
  eye: {
    name: '护眼主题',
    icon: 'eye',
    id: 'eye',
  },
};

const THEME_KEY = 'moyun-theme';
const storage = safeLocalStorage();

export function getStoredTheme(): Theme {
  const stored = storage.getItem(THEME_KEY);
  return (stored as Theme) || 'light';
}

export function getThemeFromId(id: string): Theme {
  if (['light', 'dark', 'eye'].includes(id)) {
    return id as Theme;
  }
  return 'light';
}

export function setTheme(theme: Theme, updateUrl: boolean = false): void {
  storage.setItem(THEME_KEY, theme);
  applyTheme(theme);

  if (updateUrl) {
    const currentUrl = new URL(window.location.href);
    currentUrl.searchParams.set('theme', theme);
    window.history.replaceState({}, '', currentUrl.toString());
  }
}

/**
 * 应用主题。
 *
 * 说明：
 * - 所有主题色、背景、文字、边框等 CSS 变量已统一定义在 style.css 的 :root / .dark / .eye 中。
 * - 切换主题只需给 <html> 添加/移除对应 class，浏览器会自动应用对应变量值。
 * - 这里不再通过 JS 逐个 setProperty，避免覆盖样式表并导致维护困难。
 */
export function applyTheme(theme: Theme): void {
  const root = document.documentElement;

  root.classList.remove('light', 'dark', 'eye');
  root.classList.add(theme);

  // body 背景色/颜色由 CSS 变量驱动，但首次加载或 SSR 兜底时同步一次
  document.body.style.backgroundColor = '';
  document.body.style.color = '';
  document.body.style.transition = 'background-color 180ms ease, color 180ms ease';
}

export function initTheme(): void {
  const urlParams = new URLSearchParams(window.location.search);
  const themeParam = urlParams.get('theme');

  let theme: Theme;
  if (themeParam && ['light', 'dark', 'eye'].includes(themeParam)) {
    theme = themeParam as Theme;
    storage.setItem(THEME_KEY, theme);
  } else {
    theme = getStoredTheme();
  }

  applyTheme(theme);
}

export function getCurrentTheme(): Theme {
  const urlParams = new URLSearchParams(window.location.search);
  const themeParam = urlParams.get('theme');

  if (themeParam && ['light', 'dark', 'eye'].includes(themeParam)) {
    return themeParam as Theme;
  }

  return getStoredTheme();
}
