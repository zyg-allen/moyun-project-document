import { safeLocalStorage } from './security';
export const themes = {
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
export function getStoredTheme() {
    const stored = storage.getItem(THEME_KEY);
    return stored || 'light';
}
export function getThemeFromId(id) {
    if (['light', 'dark', 'eye'].includes(id)) {
        return id;
    }
    return 'light';
}
export function setTheme(theme, updateUrl = false) {
    storage.setItem(THEME_KEY, theme);
    applyTheme(theme);
    if (updateUrl) {
        const currentUrl = new URL(window.location.href);
        currentUrl.searchParams.set('theme', theme);
        window.history.replaceState({}, '', currentUrl.toString());
    }
}
export function applyTheme(theme) {
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
export function initTheme() {
    const urlParams = new URLSearchParams(window.location.search);
    const themeParam = urlParams.get('theme');
    let theme;
    if (themeParam && ['light', 'dark', 'eye'].includes(themeParam)) {
        theme = themeParam;
        storage.setItem(THEME_KEY, theme);
    }
    else {
        theme = getStoredTheme();
    }
    applyTheme(theme);
}
export function getCurrentTheme() {
    const urlParams = new URLSearchParams(window.location.search);
    const themeParam = urlParams.get('theme');
    if (themeParam && ['light', 'dark', 'eye'].includes(themeParam)) {
        return themeParam;
    }
    return getStoredTheme();
}
