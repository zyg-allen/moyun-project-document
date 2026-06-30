import { ref, watchEffect, onMounted, computed } from 'vue';
export function useTheme() {
    const theme = ref('light');
    const getPreferredTheme = () => {
        const saved = localStorage.getItem('theme');
        if (saved === 'light' || saved === 'dark')
            return saved;
        return window.matchMedia('(prefers-color-scheme: dark)').matches
            ? 'dark'
            : 'light';
    };
    const applyTheme = (t) => {
        document.documentElement.classList.remove('light', 'dark');
        document.documentElement.classList.add(t);
        localStorage.setItem('theme', t);
    };
    const toggleTheme = () => {
        theme.value = theme.value === 'light' ? 'dark' : 'light';
    };
    onMounted(() => {
        theme.value = getPreferredTheme();
        applyTheme(theme.value);
    });
    watchEffect(() => {
        applyTheme(theme.value);
    });
    return {
        theme,
        toggleTheme,
        isDark: computed(() => theme.value === 'dark'),
    };
}
