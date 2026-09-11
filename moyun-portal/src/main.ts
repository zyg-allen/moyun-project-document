import { createApp } from 'vue'
import { createPinia } from 'pinia'
import { createHead } from '@vueuse/head'
import './style.css'
import '@fortawesome/fontawesome-free/css/all.min.css'
import App from './App.vue'
import router from './router'
import { initTheme } from './utils/theme'
import { hasPendingAiRequests } from './api/client'

// 初始化主题系统
initTheme()

// v10.23：AI 慢请求进行中，关闭/刷新页面前提醒（浏览器原生确认框）
window.addEventListener('beforeunload', (e) => {
  if (hasPendingAiRequests()) {
    e.preventDefault()
    e.returnValue = ''
  }
})

// 创建Vue应用实例
const app = createApp(App)
const pinia = createPinia()
const head = createHead()

// 使用插件
app.use(pinia)
app.use(router)
app.use(head)

// 挂载应用
app.mount('#app')
