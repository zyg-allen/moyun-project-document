import { defineConfig } from 'vite'
import vue from '@vitejs/plugin-vue'
import path from 'path'

/**
 * 【临时文件】UI 走查专用配置：http 协议 + 3001 端口。
 *
 * 背景：主配置(vite.config.ts)的 https 自签证书会阻塞自动化浏览器走查
 * （net::ERR_AUTHORITY_INVALID 且无法点击"继续前往"）。
 * 本配置仅用于 PC/移动端 UI 走查（http://localhost:3001），
 * 走查结束后删除本文件。与主配置差异：
 *   1. 无 https（PC 上 localhost 本就是安全上下文，麦克风仍可用）
 *   2. 端口 3001，避免与用户运行中的 3000 实例冲突
 *   3. 保留 /api 与 /moyun 代理（走查需后端数据与 MinIO 图片）
 *   4. 省略 SEO 注入/SSL/徽章插件（对视觉走查无影响）
 */
export default defineConfig({
  server: {
    host: true,
    port: 3001,
    strictPort: true,
    proxy: {
      '/api': {
        target: 'http://localhost:8080',
        changeOrigin: true,
        ws: true,
        rewrite: (p) => p.replace(/^\/api/, ''),
      },
      '/moyun': {
        target: 'http://127.0.0.1:9001',
        changeOrigin: true,
      },
    },
  },
  plugins: [vue()],
  resolve: {
    alias: {
      '@': path.resolve(__dirname, './src'),
    },
  },
})
