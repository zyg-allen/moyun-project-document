<template>
  <view class="page" :style="themeVars">
    <NavBar title="赞赏" />

    <view class="tip-card">
      <view class="tip-row">
        <text class="tip-label">赞赏金额（元）</text>
        <input class="tip-input" type="digit" v-model="amount" placeholder="请输入金额，单位：元（如 5.00）" placeholder-style="color:#ccc" />
      </view>
      <view class="tip-row">
        <text class="tip-label">累计赞赏金额：</text>
        <text class="tip-total">¥ {{ totalAmount }}</text>
      </view>
    </view>

    <view class="pay-title">支付</view>
    <view class="pay-card">
      <view class="pay-row" @tap="payWay = 'wechat'">
        <view class="pay-left">
          <view class="pay-icon wechat">
            <text>💬</text>
          </view>
          <text class="pay-name">微信支付</text>
        </view>
        <view class="pay-check" :class="{ checked: payWay === 'wechat' }">
          <text v-if="payWay === 'wechat'">✓</text>
        </view>
      </view>
      <view class="pay-row" @tap="payWay = 'alipay'">
        <view class="pay-left">
          <view class="pay-icon alipay">
            <text>支</text>
          </view>
          <text class="pay-name">支付宝支付</text>
        </view>
        <view class="pay-check" :class="{ checked: payWay === 'alipay' }">
          <text v-if="payWay === 'alipay'">✓</text>
        </view>
      </view>
    </view>

    <view class="tip-note">
      <view>提示：赞赏金额累计达到 9.9 元即可免除广告</view>
      <view>注意：本功能为演示，不会发起真实支付</view>
    </view>

    <view class="pay-btn" @tap="doTip">立即赞赏</view>
  </view>
</template>

<script>
import NavBar from '@/components/NavBar/NavBar.vue';
import { useThemeStore } from '@/stores/theme';
import { useUserStore } from '@/stores/user';
import { getTipTotal, createTip } from '@/api/ledger';
import { toFixedYuan } from '@/utils/money';

export default {
  components: { NavBar },
  data() {
    return {
      amount: '',
      payWay: 'wechat',
      totalAmount: '0.00'
    };
  },
  computed: {
    themeVars() { return useThemeStore().themeVars; }
  },
  onShow() {
    useThemeStore().restore();
    this.loadTotal();
  },
  methods: {
    async loadTotal() {
      if (!useUserStore().isLoggedIn) { this.totalAmount = '0.00'; return; }
      try {
        const data = await getTipTotal() || {};
        this.totalAmount = toFixedYuan(data.totalAmount);
      } catch (e) { /* 拦截器已提示 */ }
    },
    doTip() {
      if (!useUserStore().isLoggedIn) {
        uni.showToast({ title: '请先在「我的」页登录', icon: 'none' });
        return;
      }
      const num = parseFloat(this.amount);
      if (!num || num <= 0) {
        uni.showToast({ title: '请输入有效金额', icon: 'none' });
        return;
      }
      uni.showModal({
        title: '赞赏',
        content: `确认赞赏 ¥${num.toFixed(2)}？（${this.payWay === 'wechat' ? '微信支付' : '支付宝支付'}）`,
        success: async (r) => {
          if (!r.confirm) return;
          try {
            await createTip({ amount: num, payWay: this.payWay, target: 'developer' });
            this.amount = '';
            this.loadTotal();
            uni.showToast({ title: '赞赏成功，感谢支持！', icon: 'success' });
          } catch (e) { /* 拦截器已提示 */ }
        }
      });
    }
  }
};
</script>

<style scoped>
.page { padding-bottom: 160rpx; min-height: 100vh; background: #f5f6f8; }
.tip-card {
  background: #fff; margin: 24rpx; border-radius: 20rpx; padding: 8rpx 32rpx;
}
.tip-row {
  display: flex; align-items: center; justify-content: space-between;
  padding: 28rpx 0; border-bottom: 1rpx solid #f5f5f7;
}
.tip-row:last-child { border-bottom: none; }
.tip-label { font-size: 28rpx; color: #333; }
.tip-input { flex: 1; text-align: right; font-size: 28rpx; color: var(--primary-strong); }
.tip-total { font-size: 28rpx; color: #333; }

.pay-title { font-size: 28rpx; color: #333; padding: 16rpx 32rpx 8rpx; }
.pay-card { background: #fff; margin: 0 24rpx; border-radius: 20rpx; padding: 8rpx 32rpx; }
.pay-row {
  display: flex; align-items: center; justify-content: space-between;
  padding: 28rpx 0; border-bottom: 1rpx solid #f5f5f7;
}
.pay-row:last-child { border-bottom: none; }
.pay-left { display: flex; align-items: center; }
.pay-icon {
  width: 64rpx; height: 64rpx; border-radius: 14rpx;
  display: flex; align-items: center; justify-content: center;
  color: #fff; font-size: 30rpx; margin-right: 20rpx;
}
.pay-icon.wechat { background: #07c160; }
.pay-icon.alipay { background: #1677ff; }
.pay-name { font-size: 28rpx; color: #333; }
.pay-check {
  width: 36rpx; height: 36rpx; border-radius: 18rpx;
  border: 2rpx solid #ccc; display: flex; align-items: center; justify-content: center;
  color: #fff; font-size: 22rpx;
}
.pay-check.checked { background: var(--primary-strong); border-color: var(--primary-strong); }

.tip-note {
  margin: 24rpx 32rpx; font-size: 22rpx; color: #999; line-height: 1.8;
}

.pay-btn {
  position: fixed; bottom: 40rpx; left: 24rpx; right: 24rpx;
  height: 88rpx; line-height: 88rpx; text-align: center;
  background: var(--primary-strong); color: #fff;
  border-radius: 44rpx; font-size: 30rpx; font-weight: 600;
}
.pay-btn:active { opacity: 0.85; }
</style>
