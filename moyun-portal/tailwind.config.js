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
          bg: "var(--theme-bg)",
          surface: "var(--theme-surface)",
          accent: "var(--theme-accent)",
          border: "var(--theme-border)",
          text: "var(--theme-text)",
          "text-secondary": "var(--theme-text-secondary)",
          danger: "var(--theme-danger)",
          success: "var(--theme-success)",
        },
      },
      // 统一的最大宽度容器（与各页面 max-w-7xl 一致）
      maxWidth: {
        content: "80rem", // 对应 max-w-7xl
      },
    },
  },
  plugins: [],
};
