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
     外观层（--bg/--card/--text-*/--border/--income/--expense）随日间/暗色/护眼切换；
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
</style>
