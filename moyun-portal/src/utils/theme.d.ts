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
export declare const themes: Record<Theme, ThemeConfig>;
export declare function getStoredTheme(): Theme;
export declare function getThemeFromId(id: string): Theme;
export declare function setTheme(theme: Theme, updateUrl?: boolean): void;
export declare function applyTheme(theme: Theme): void;
export declare function initTheme(): void;
export declare function getCurrentTheme(): Theme;
export {};
