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
  background-color: #f5f6f8;
  font-size: 28rpx;
  color: #333;
  font-family: -apple-system, 'PingFang SC', 'Helvetica Neue', sans-serif;
}
/* 主题变量默认值（页面根元素 :style 会覆盖） */
page {
  --primary: #7FBF94;
  --primary-strong: #5FA77A;
  --primary-soft: #E9F5EE;
  --primary-shadow: rgba(127, 191, 148, 0.35);
  --income: #3EB575;
  --expense: #F0564A;
}
/* 通用工具类 */
.flex-row { display: flex; flex-direction: row; align-items: center; }
.flex-col { display: flex; flex-direction: column; }
.flex-1 { flex: 1; }
.text-muted { color: #999; }
.card {
  background: #fff;
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
