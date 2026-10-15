<script>
import { useThemeStore } from '@/stores/theme';

export default {
  onLaunch() {
    console.log('墨韵记账 App Launch');
    // 恢复主题（含 TabBar 选中色联动）
    useThemeStore().restore();
  }
};
</script>

<style>
/* 全局基础样式 */
page {
  /* 底色与文字跟随外观变量（未注入时由下方 page 变量块兜底） */
  background-color: var(--bg);
  color: var(--text-main);
  font-size: 28rpx;
  font-family: -apple-system, 'PingFang SC', 'Helvetica Neue', sans-serif;
}
/* 主题变量默认值（页面根元素 :style 由 stores/theme.js 的 themeVars 注入覆盖）
   两层结构：
     外观层（--bg / --card / --text-main 等）随日间/暗色/护眼切换；
     强调色层（--primary 系列）随 5 套强调色切换。
   默认值取「日间 + 薄荷绿」，与改造前视觉一致。

   ⚠️ 选择器必须是 :root（html），**不能是 page**：
   page 在 H5 下就是 uni-page-body 本身。若把默认变量写在它身上，就会
   「就近覆盖」掉从 body 继承下来的运行时主题变量 —— body 上注入的暗色 --bg
   永远生效不了，切换页面时新建的 uni-page-body 仍按日间默认绘制，
   表现为一闪而过的白底。放 :root 后：
     · 它是"JS 还没跑"时的兜底；
     · body 上的内联变量更近，运行时主题照常胜出；
     · uni-page-body 自身不再有本地声明，一路继承 body → 一出生就是当前外观。 */
:root {
  /* ---- 外观层默认（日间） ---- */
  --bg: #F7F8FA;
  --card: #FFFFFF;
  --surface: #F0F2F6;
  --border: #E5E8EF;
  --border-strong: #D3D8E2;
  --text-main: #1A1D26;
  --text-sub: #6B7280;
  --text-muted: #9CA3AF;
  --income: #0A7F5F;
  --income-bg: #E6F5EF;
  --expense: #E8654A;
  --expense-bg: #FDEEE9;

  /* ---- 强调色层默认（薄荷绿） ---- */
  --primary: #7FBF94;
  --primary-strong: #5FA77A;
  --primary-soft: #E9F5EE;
  --primary-shadow: rgba(127, 191, 148, 0.35);

  /* 最外层容器（H5 下即 uni-page-body）此前**没有任何 background / color 声明**，
     于是背景用浏览器默认白色、文字用默认黑色。
     主题变量只注入在各页根视图上，它拿不到 —— 切到暗色后：
       · 页尾/回弹区露出白底；
       · **凡是没显式声明 color 的元素（如卡片标题 .card-title）都继承黑色，
         在深色卡面上直接看不见**。
     这里给出基础色，运行时再由 theme store 的 applyToPageBody() 覆盖
     （H5 走 DOM 变量，小程序走 uni.setBackgroundColor）。 */
  background: var(--bg);
  color: var(--text-main);
}
/* 通用工具类 */
/* 页面根容器：全局统一兜底底色与最小高度。
 *
 * 为什么必须全局给：此前只靠「各页自己写 .page { background: var(--bg) }」，
 * 于是那些**漏写背景**的页面（settings / categories / budget / report /
 * record/edit）只能靠下层 uni-page-body 画底色 —— 而切换页面时新建的
 * uni-page-body 会先按日间默认值绘制一帧，随后才被主题覆盖，
 * 视觉上就是「由白转暗」的极短暂闪白。同一件事在不同页面表现不一致，
 * 根源就在这：**有的页面自己画了底色，有的没有**。
 *
 * 放到全局后：所有 .page 一律自带当前外观的底色，不再依赖下层容器，
 * 新页面一出现就是正确颜色 —— 也不必再逐个页面去补 background。
 *
 * 注意：页面自身 <style scoped> 里的 .page 规则带 [data-v-xxx] 属性选择器，
 * 优先级高于本全局规则，所以自带背景/内边距的页面不会被这里覆盖。 */
.page {
  min-height: 100vh;
  background: var(--bg);
  box-sizing: border-box;
}
.flex-row { display: flex; flex-direction: row; align-items: center; }
.flex-col { display: flex; flex-direction: column; }
.flex-1 { flex: 1; }
/* flex:1 的默认 min-width 是 auto —— 弹性子项**无法收缩到内容宽度以下**，
   于是长文本（如很长的备注）会把整行撑破，子元素上的
   `overflow:hidden; text-overflow:ellipsis` 也因此失效（元素宽度根本没被约束）。
   `record/list.vue` 早就在用 `class="flex-1 min-w-0"`，但该类**从未定义**，
   所以那里的省略号其实一直没生效。这里补上定义，并配合 .flex-1 使用。 */
.min-w-0 { min-width: 0; }
.text-muted { color: var(--text-sub); }
.card {
  background: var(--card);
  border-radius: 20rpx;
  padding: 24rpx 32rpx;
  margin: 0 24rpx 24rpx;
  box-shadow: 0 2rpx 12rpx rgba(0, 0, 0, 0.04);
}
.btn-primary {
  background: var(--primary);
  color: var(--on-primary);
  border-radius: 44rpx;
  height: 88rpx;
  line-height: 88rpx;
  text-align: center;
  font-size: 32rpx;
  font-weight: 500;
}
.btn-primary.disabled { opacity: 0.5; }
/* 收支语义色 */
.text-income { color: var(--income); }
.text-expense { color: var(--expense); }

/* ============================================================
 * 全局日期/时间选择器放大（uni-app H5 自定义滚轮）
 *
 * 背景：mode="time" 与移动端 mode="date" 走 uni-app 自绘滚轮，
 * 默认单项高 34px / 字号 16px，滚轮列偏窄、手指点选/滑动不易命中。
 * 这里统一放大到更舒适的尺寸，覆盖所有页面（记一笔/备忘录/明细/报表等）。
 * ============================================================ */
.uni-picker-container .uni-picker-item {
  height: 46px;
  line-height: 46px;
  font-size: 20px;
  padding: 0 10px;
}
.uni-picker-container .uni-picker-content {
  height: 264px;
}
/* 桌面端原生日历的透明点击区（chrome）：放大覆盖整个触发元素，点击更易命中 */
.uni-picker-system > .uni-picker-system_input.chrome {
  width: 100%;
  height: 100%;
  font-size: 32px;
}

/* ============================================================
 * 底部 TabBar 样式
 *
 * 字号与高度由 pages.json 的 fontSize / height 控制（10px→12px、50px→46px，
 * 「字体大一点 + 区域略缩小」）；底色/文字色/选中色由 stores/theme.js 的
 * syncTabBar() 经 setTabBarStyle 注入，随外观与强调色联动。
 *
 * 这里只补 uni 配置项表达不了的部分：
 *  - uni 未给选中项加 class（只改内联 color），故选中态仍靠颜色区分，
 *    字重统一加粗以保证小字号下的可读性；
 *  - 去掉硬边线，改用柔和上阴影做分隔，观感更轻。
 * ============================================================ */
.uni-tabbar__label {
  font-weight: 600;
  letter-spacing: 0.5px;
}
.uni-tabbar-border { display: none; }
.uni-tabbar-bottom .uni-tabbar {
  box-shadow: 0 -6px 16px rgba(0, 0, 0, 0.06);
}

/* ============================================================
 * 「标签 + 值」行里的选择器外壳对齐
 *
 * 问题：uni-picker 默认 `display:block` 且**没有 flex**；CalendarPicker 的根
 * `.cp-root` 是 `inline-flex`。二者作为 flex 行（标签 + 值）的子项时都只有
 * **内容宽度**，值不会贴到行右边缘 —— 而同一行里 <input>/<text> 类的值都是
 * `flex:1 + text-align:right`，于是出现「一行居中、一行靠右」的不一致
 * （日期/时间行尤其明显）。
 *
 * 处理：加 `.fill-picker` 后外壳占满剩余宽度，值自然右对齐。
 * **显式 opt-in**，不直接改 uni-picker —— 表头、筛选栏等按内容排布的
 * picker 不能被撑开；记一笔「日期时间」行的时间选择器更是特意用
 * `.pick-picker-time { flex: none }` 保持内容宽度。
 * ============================================================ */
.fill-picker {
  flex: 1;
  min-width: 0;
}
</style>
