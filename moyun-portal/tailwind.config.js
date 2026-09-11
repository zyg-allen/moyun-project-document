/** @type {import('tailwindcss').Config} */

export default {
  darkMode: "class",
  content: ["./index.html", "./src/**/*.{js,ts,vue}"],
  theme: {
    container: {
      center: true,
    },
    extend: {
      colors: {
        // 主题色映射，支持 bg-theme-primary / text-theme-text 等用法
        // 颜色值由 CSS 变量驱动，自动跟随 light/dark/eye 主题切换
        theme: {
          primary: "var(--theme-primary)",
          "primary-hover": "var(--theme-primary-hover)",
          "primary-active": "var(--theme-primary-active)",
          "primary-soft": "var(--theme-primary-soft)",
          "primary-soft-hover": "var(--theme-primary-soft-hover)",
          "on-primary": "var(--theme-on-primary)",

          bg: "var(--theme-bg)",
          "bg-elevated": "var(--theme-bg-elevated)",
          surface: "var(--theme-surface)",
          "surface-elevated": "var(--theme-surface-elevated)",
          "surface-highlight": "var(--theme-surface-highlight)",
          accent: "var(--theme-accent)",
          "accent-hover": "var(--theme-accent-hover)",

          text: "var(--theme-text)",
          "text-secondary": "var(--theme-text-secondary)",
          "text-tertiary": "var(--theme-text-tertiary)",
          "text-disabled": "var(--theme-text-disabled)",
          "text-inverse": "var(--theme-text-inverse)",

          border: "var(--theme-border)",
          "border-strong": "var(--theme-border-strong)",
          "border-focus": "var(--theme-border-focus)",

          overlay: "var(--theme-overlay)",
          "overlay-soft": "var(--theme-overlay-soft)",

          danger: "var(--theme-danger)",
          "danger-hover": "var(--theme-danger-hover)",
          "danger-bg": "var(--theme-danger-bg)",
          "danger-bg-strong": "var(--theme-danger-bg-strong)",
          success: "var(--theme-success)",
          "success-hover": "var(--theme-success-hover)",
          "success-bg": "var(--theme-success-bg)",
          "success-bg-strong": "var(--theme-success-bg-strong)",
          warning: "var(--theme-warning)",
          "warning-hover": "var(--theme-warning-hover)",
          "warning-bg": "var(--theme-warning-bg)",
          "warning-bg-strong": "var(--theme-warning-bg-strong)",
          info: "var(--theme-info)",
          "info-hover": "var(--theme-info-hover)",
          "info-bg": "var(--theme-info-bg)",
          "info-bg-strong": "var(--theme-info-bg-strong)",
        },
      },
      spacing: {
        // 接入设计 token，使 p-theme-3 / gap-theme-4 / m-theme-6 等可用
        "theme-0": "var(--space-0)",
        "theme-1": "var(--space-1)",
        "theme-2": "var(--space-2)",
        "theme-3": "var(--space-3)",
        "theme-4": "var(--space-4)",
        "theme-5": "var(--space-5)",
        "theme-6": "var(--space-6)",
        "theme-8": "var(--space-8)",
        "theme-10": "var(--space-10)",
        "theme-12": "var(--space-12)",
        "theme-16": "var(--space-16)",
      },
      borderRadius: {
        // 接入设计 token，使 rounded-theme-lg 等可用
        "theme-none": "var(--radius-none)",
        "theme-xs": "var(--radius-xs)",
        "theme-sm": "var(--radius-sm)",
        "theme-md": "var(--radius-md)",
        "theme-lg": "var(--radius-lg)",
        "theme-xl": "var(--radius-xl)",
        "theme-2xl": "var(--radius-2xl)",
        "theme-full": "var(--radius-full)",
      },
      fontFamily: {
        sans: ["var(--font-sans)"],
        heading: ["var(--font-heading)"],
        mono: ["var(--font-mono)"],
      },
      fontSize: {
        "theme-xs": ["0.75rem", { lineHeight: "1rem" }],
        "theme-sm": ["0.8125rem", { lineHeight: "1.25rem" }],
        "theme-base": ["0.9375rem", { lineHeight: "1.5rem" }],
        "theme-lg": ["1.0625rem", { lineHeight: "1.5rem" }],
        "theme-xl": ["1.25rem", { lineHeight: "1.75rem" }],
        "theme-2xl": ["1.5rem", { lineHeight: "2rem" }],
        "theme-3xl": ["2rem", { lineHeight: "2.5rem" }],
      },
      boxShadow: {
        "theme-sm": "var(--shadow-sm)",
        "theme-md": "var(--shadow-md)",
        "theme-lg": "var(--shadow-lg)",
        "theme-xl": "var(--shadow-xl)",
        "theme-glow": "var(--shadow-glow)",
      },
      transitionDuration: {
        "theme-fast": "120ms",
        "theme-base": "180ms",
        "theme-slow": "260ms",
      },
      maxWidth: {
        // 统一的最大宽度容器（与各页面 max-w-7xl 一致）
        content: "80rem", // 对应 max-w-7xl
      },
    },
  },
  plugins: [],
};
