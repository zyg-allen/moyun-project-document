import { defineConfig } from 'vite'
import vue from '@vitejs/plugin-vue'
import basicSsl from '@vitejs/plugin-basic-ssl'
import path from 'path'
import traeBadgePlugin from 'vite-plugin-trae-solo-badge'

// https://vite.dev/config/
export default defineConfig({
  build: {
    sourcemap: false,  // 生产关闭 sourcemap
    rollupOptions: {
      output: {
        manualChunks: {
          'vue-vendor': ['vue', 'vue-router', 'pinia'],
          'ui-vendor': ['lucide-vue-next'],
          'editor-vendor': ['marked', 'quill', '@vueup/vue-quill'],
          'utils-vendor': ['dayjs', '@vueuse/core', '@vueuse/head'],
          // Monaco 单独成块，按需加载
          'monaco-vendor': ['monaco-editor'],
        },
      },
    },
  },
  server: {
    host: true,   // 监听 0.0.0.0，手机等局域网设备可访问
    port: 3000,
    // HTTPS：手机浏览器 getUserMedia（麦克风）仅在 https/localhost 下可用。
    // 自签证书首次访问会有安全警告，属预期；生产环境应使用正规证书。
    https: {},
    proxy: {
      '/api': {
        target: 'http://localhost:8080',
        changeOrigin: true,
        ws: true,  // 透传 WebSocket 升级（wss → /api/ws-asr 流式转写）
        // 后端无 context-path，剥离 /api 前缀再转发
        rewrite: (p) => p.replace(/^\/api/, ''),
      }
    }
  },
  plugins: [
    vue(),
    basicSsl(),
    traeBadgePlugin({
      variant: 'dark',
      position: 'bottom-right',
      prodOnly: true,
      clickable: true,
      clickUrl: 'https://www.trae.ai/solo?showJoin=1',
      autoTheme: true,
      autoThemeTarget: '#app',
    }),
  ],
  resolve: {
    alias: {
      '@': path.resolve(__dirname, './src'), // ✅ 定义 @ = src
    },
  },
})
