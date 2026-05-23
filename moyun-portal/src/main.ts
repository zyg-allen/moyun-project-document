import { createApp } from 'vue'
import { createPinia } from 'pinia'
import { createHead } from '@vueuse/head'
import './style.css'
import App from './App.vue'
import router from './router'
import { initTheme } from './utils/theme'

// 初始化主题系统
initTheme()

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
