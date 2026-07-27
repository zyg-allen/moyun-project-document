import { createApp } from 'vue';
import App from './App.vue';
import ElementPlus from 'element-plus';
import zhCn from 'element-plus/dist/locale/zh-cn.mjs';
import 'element-plus/dist/index.css';
import router from './router';
import axios from 'axios';

// 引入 Font Awesome 图标
import '@fortawesome/fontawesome-free/css/all.min.css';

// 配置 axios 拦截器
axios.interceptors.request.use(
  (config) => {
    const token = localStorage.getItem('token')
    if (token) {
      config.headers['Authorization'] = token
    }
    return config
  },
  (error) => Promise.reject(error)
)

axios.interceptors.response.use(
  (response) => response,
  (error) => {
    if (error.response?.status === 401) {
      localStorage.removeItem('token')
      localStorage.removeItem('username')
      localStorage.removeItem('nickname')
      // 使用 ElementPlus 的消息提示
      import('element-plus').then(({ ElMessage }) => {
        ElMessage.warning('登录已过期，请重新登录')
      })
      router.push('/login')
    }
    return Promise.reject(error)
  }
)

const app = createApp(App);
app.use(ElementPlus, {
  locale: zhCn,
});
app.use(router);
app.mount('#app');

