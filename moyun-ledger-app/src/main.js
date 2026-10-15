import App from './App';
import { createPinia } from 'pinia';
import { createSSRApp } from 'vue';
import { useThemeStore } from '@/stores/theme';

export function createApp() {
  const app = createSSRApp(App);
  app.use(createPinia());

  /**
   * 挂载【之前】先把主题变量写到 html/body 上。
   *
   * 变量由 CSS 继承，因此之后任何页面容器一创建就自带当前外观，
   * 不必等 onShow / 观察器 —— 这是消除「切页瞬间极短暂白闪」的根本手段
   * （命令式注入永远晚于浏览器可能的第一次绘制）。
   */
  useThemeStore().applyToPageBody();

  /**
   * 全局主题注入 —— 每次进入页面都把当前外观写进该页最外层容器。
   *
   * 为什么必须全局挂：uni-app H5 **每个页面各自创建一个 `uni-page-body`**。
   * 而 `applyToPageBody()` 是命令式地改"当时那一个"元素 —— 挂在 onLaunch 上时，
   * 它只改了启动那一刻的那一个 body。之后从「更多」跳到「记账设置」，
   * 新页面的 body 是**全新元素**，依旧吃 App.vue 里 `page{}` 的**日间静态默认值**，
   * 于是暗色下新页面背景发白；而整页刷新会在当前路由重新走 onLaunch，
   * 恰好改到当前这个 body —— 这就是"刷新一下就正常"的原因。
   *
   * 挂在全局 mixin 的 `onShow`，任何页面（含 tab 页、二级页）进入都会重新注入，
   * 与该页是第几个、从哪跳进来都无关。
   *
   * 只调 applyToPageBody() 而不是 restore()：后者还会同步 TabBar，
   * 在非 tab 页上是多余且无意义的调用。
   */
  app.mixin({
    onShow() {
      useThemeStore().applyToPageBody();
    }
  });

  return { app };
}
