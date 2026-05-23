import { safeLocalStorage } from './security'

export type Theme = 'light' | 'dark' | 'eye';

interface ThemeConfig {
  name: string;
  icon: string;
  id: string;
  colors: {
    primary: string;
    bg: string;
    text: string;
    textSecondary: string;
    border: string;
    surface: string;
    accent: string;
  };
}

export const themes: Record<Theme, ThemeConfig> = {
  light: {
    name: '日间主题',
    icon: 'sun',
    id: 'light',
    colors: {
      primary: '#DC2626',
      bg: '#FFFFFF',
      text: '#111827',
      textSecondary: '#4B5563',
      border: '#E5E7EB',
      surface: '#F9FAFB',
      accent: '#FEE2E2',
    },
  },
  dark: {
    name: '夜间主题',
    icon: 'moon',
    id: 'dark',
    colors: {
      primary: '#EF4444',
      bg: '#0F172A',
      text: '#F3F4F6',
      textSecondary: '#94A3B8',
      border: '#1E293B',
      surface: '#1E293B',
      accent: '#1E293B',
    },
  },
  eye: {
    name: '护眼主题',
    icon: 'eye',
    id: 'eye',
    colors: {
      primary: '#065F46',
      bg: '#F7F3E3',
      text: '#1F2937',
      textSecondary: '#4B5563',
      border: '#D1D5DB',
      surface: '#FAF8F0',
      accent: '#D1FAE5',
    },
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

export function applyTheme(theme: Theme): void {
  const root = document.documentElement;
  const config = themes[theme];

  root.style.setProperty('--theme-primary', config.colors.primary);
  root.style.setProperty('--theme-bg', config.colors.bg);
  root.style.setProperty('--theme-text', config.colors.text);
  root.style.setProperty('--theme-text-secondary', config.colors.textSecondary);
  root.style.setProperty('--theme-border', config.colors.border);
  root.style.setProperty('--theme-surface', config.colors.surface);
  root.style.setProperty('--theme-accent', config.colors.accent);

  root.classList.remove('light', 'dark', 'eye');
  root.classList.add(theme);

  document.body.style.backgroundColor = config.colors.bg;
  document.body.style.color = config.colors.text;
  document.body.style.transition = 'background-color 0.3s ease, color 0.3s ease';
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
