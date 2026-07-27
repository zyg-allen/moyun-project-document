import { defineConfig, loadEnv } from 'vite'
import vue from '@vitejs/plugin-vue'
import { fileURLToPath, URL } from 'node:url'

// https://vitejs.dev/config/
export default defineConfig(({ mode }) => {
  const env = loadEnv(mode, process.cwd())
  const API_BASE_URL = env.VITE_API_BASE_URL || 'http://localhost:8080'

  return {
    base: '/lynx-ai/',
    plugins: [vue()],
    server: {
      port: 5173,
      proxy: {
        // 所有 /api 开头的请求都代理到后端
        '/api': {
          target: API_BASE_URL,
          changeOrigin: true,
          // SSE 需要的配置
          configure: (proxy, options) => {
            proxy.on('proxyReq', (proxyReq, req, res) => {
              // 对于 SSE 请求，禁用缓冲
              if (req.url.includes('/stream')) {
                proxyReq.setHeader('Cache-Control', 'no-cache')
                proxyReq.setHeader('Connection', 'keep-alive')
              }
            })
          }
        },
      },
    },
    resolve: {
      alias: {
        '@': fileURLToPath(new URL('./src', import.meta.url)),
      },
    },
  }
})
