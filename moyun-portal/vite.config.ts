import { defineConfig, type Plugin } from 'vite'
import vue from '@vitejs/plugin-vue'
import basicSsl from '@vitejs/plugin-basic-ssl'
import path from 'path'
import fs from 'node:fs'
import traeBadgePlugin from 'vite-plugin-trae-solo-badge'

/**
 * 站点域名注入插件（v13.9）
 *
 * <p>背景：canonical / og:url / JSON-LD / robots.txt / sitemap.xml 都需要**绝对站点地址**，
 * 而它们是静态内容（HTML 模板与 public/ 文件），拿不到运行时环境变量。
 * 历史做法是"把 `xulin.example.com` 写死 + 注释提醒部署时替换"——结果占位域名随构建产物上线
 * （实测：`dist/index.html`、`dist/robots.txt`、`dist/sitemap.xml` 里都有）。</p>
 *
 * <p>本插件把域名变成**构建期注入**：</p>
 * <ol>
 *   <li>`%SITE_URL%` / `%OG_IMAGE%` 模板变量 ← `VITE_SITE_URL` / `VITE_DEFAULT_OG_IMAGE`；</li>
 *   <li>开发/预览用当前源（`http://localhost:<port>`）替代，不影响本地 SEO 调试；</li>
 *   <li>**生产构建未配置 `VITE_SITE_URL` 直接失败**（fail-closed）——宁可让部署者补一个域名，
 *       也不要让占位/相对地址静默上线（相对 canonical 在搜索引擎侧是无效的）；</li>
 *   <li>构建结束自检产物：残留 `%SITE_URL%` 或占位域名即失败。</li>
 * </ol>
 */
function siteUrlPlugin(): Plugin {
  const DEFAULT_OG_IMAGE =
    'https://images.unsplash.com/photo-1499750310107-5fef28a66643?w=1200&h=630&fit=crop'

  let siteUrl = ''
  let ogImage = DEFAULT_OG_IMAGE
  let devOrigin = 'http://localhost:5173'
  let isProductionBuild = false

  const fill = (text: string) =>
    text.replace(/%SITE_URL%/g, siteUrl || devOrigin).replace(/%OG_IMAGE%/g, ogImage)

  return {
    name: 'moyun:site-url',
    configResolved(config) {
      siteUrl = String(config.env.VITE_SITE_URL || '').replace(/\/+$/, '')
      ogImage = String(config.env.VITE_DEFAULT_OG_IMAGE || '') || DEFAULT_OG_IMAGE
      devOrigin = `http://localhost:${config.server?.port ?? 5173}`
      isProductionBuild = config.command === 'build' && config.mode === 'production'

      if (isProductionBuild && !siteUrl) {
        throw new Error(
          '\n[site-url] 生产构建缺少 VITE_SITE_URL。\n' +
          '  canonical / og:url / JSON-LD / robots.txt / sitemap.xml 需要绝对站点地址，\n' +
          '  留空会让占位域名或相对地址上线（SEO 无效）。\n' +
          '  请在 moyun-portal/.env.production 填写真实域名，例如：\n' +
          '    VITE_SITE_URL=https://你的域名\n',
        )
      }
    },
    transformIndexHtml: {
      order: 'pre',
      handler: (html: string) => fill(html),
    },
    closeBundle() {
      const dist = path.resolve(__dirname, 'dist')
      for (const file of ['robots.txt', 'sitemap.xml']) {
        const target = path.join(dist, file)
        if (fs.existsSync(target)) {
          fs.writeFileSync(target, fill(fs.readFileSync(target, 'utf8')))
        }
      }
      // 自检：产物里不得残留模板变量或历史占位域名
      const residue: string[] = []
      for (const file of ['index.html', 'robots.txt', 'sitemap.xml']) {
        const target = path.join(dist, file)
        if (!fs.existsSync(target)) continue
        const content = fs.readFileSync(target, 'utf8')
        if (content.includes('%SITE_URL%') || content.includes('%OG_IMAGE%')) {
          residue.push(`${file} 残留模板变量`)
        }
        if (content.includes('xulin.example.com')) {
          residue.push(`${file} 残留占位域名 xulin.example.com`)
        }
      }
      if (residue.length > 0) {
        throw new Error(`\n[site-url] 构建产物自检失败：\n  ${residue.join('\n  ')}\n`)
      }
    },
  }
}

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
      },
      // MinIO 图片代理：HTTPS 页面加载 HTTP 图片会被浏览器拦截（混合内容），
      // 且手机端 127.0.0.1 不可达。前端用 normalizeFileUrl() 把 MinIO 绝对 URL
      // 转成 /moyun/xxx 相对路径，由该代理转发到 MinIO 服务。
      '/moyun': {
        target: 'http://127.0.0.1:9001',
        changeOrigin: true,
      }
    }
  },
  plugins: [
    siteUrlPlugin(),
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
