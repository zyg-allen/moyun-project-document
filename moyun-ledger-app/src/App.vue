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
   默认值取「日间 + 薄荷绿」，与改造前视觉一致。 */
page {
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
}
/* 通用工具类 */
.flex-row { display: flex; flex-direction: row; align-items: center; }
.flex-col { display: flex; flex-direction: column; }
.flex-1 { flex: 1; }
.text-muted { color: var(--text-sub); }
.card {
  background: var(--card);
  border-radius: 20rpx;
  padding: 24rpx;
  margin: 0 24rpx 24rpx;
  box-shadow: 0 2rpx 12rpx rgba(0, 0, 0, 0.04);
}
.btn-primary {
  background: var(--primary);
  color: #fff;
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
</style>
