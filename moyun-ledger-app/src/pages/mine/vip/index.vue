<template>
  <view class="page" :style="themeVars">
    <NavBar title="记账VIP" />

    <!-- 会员状态卡 -->
    <view class="status-card" :class="{ vip: vip.isVip }">
      <view class="status-left">
        <text class="status-icon">{{ vip.isVip ? '👑' : '🔒' }}</text>
        <view class="status-text">
          <text class="status-title">{{ vip.isVip ? 'VIP 会员生效中' : '尚未开通 VIP' }}</text>
          <text class="status-sub">{{ vip.isVip ? '权益有效期至 ' + formatDay(vip.vipExpire) : '开通解锁全部高级功能' }}</text>
        </view>
      </view>
    </view>

    <!-- 权益清单（骨架：v11.78 收入规划功能项） -->
    <view class="benefit-card">
      <view class="benefit-title">VIP 权益</view>
      <view class="benefit-row" v-for="b in benefits" :key="b">
        <text class="benefit-check">✓</text>
        <text class="benefit-name">{{ b }}</text>
      </view>
      <text class="benefit-note">注：权益功能随版本逐步解锁，订阅即刻生效、续费顺延不折损。</text>
    </view>

    <!-- 套餐列表（后台可配价格） -->
    <view class="pkg-title">选择套餐</view>
    <view class="pkg-card" v-for="p in packages" :key="p.id"
          :class="{ active: selectedId === p.id, popular: p.popular }" @tap="selectedId = p.id">
      <view v-if="p.popular" class="pkg-popular">推荐</view>
      <view class="pkg-name">{{ p.name }}</view>
      <view class="pkg-duration">{{ p.durationDays }} 天</view>
      <view class="pkg-price">
        <text class="pkg-price-num">¥{{ p.price }}</text>
        <text v-if="p.originalPrice" class="pkg-price-original">¥{{ p.originalPrice }}</text>
      </view>
      <view v-if="p.description" class="pkg-desc">{{ p.description }}</view>
    </view>
    <view v-if="!packages.length && !loading" class="pkg-empty">暂无在售套餐</view>

    <view class="pay-btn" @tap="doSubscribe">立即订阅（微信支付）</view>

    <!-- 收银台弹层（V11.81 公共支付通道） -->
    <view v-if="cashier.visible" class="cashier-mask" @tap="closeCashier(false)">
      <view class="cashier-panel" @tap.stop>
        <view class="cashier-title">微信支付</view>
        <view class="cashier-pkg">{{ cashier.packageName }}</view>
        <view class="cashier-amount">¥{{ cashier.amount }}</view>
        <view class="cashier-qr-wrap">
          <canvas :id="'vipQr' + cashier.vipOrderId" :canvas-id="'vipQr' + cashier.vipOrderId" class="cashier-qr"></canvas>
        </view>
        <view class="cashier-tip">请使用微信扫一扫完成支付</view>
        <view class="cashier-status" :class="{ paid: cashier.payStatus === 'PAID' }">
          {{ cashier.payStatus === 'PAID' ? '订阅成功 ✓' : '等待支付中…' }}
        </view>
        <view class="cashier-actions">
          <view class="cashier-btn ghost" @tap="closeCashier(false)">取消</view>
          <view v-if="cashier.mockEnabled && cashier.payStatus !== 'PAID'" class="cashier-btn primary" @tap="doMockPay">
            {{ mockPaying ? '支付中…' : '模拟支付成功' }}
          </view>
          <view v-if="cashier.payStatus === 'PAID'" class="cashier-btn primary" @tap="closeCashier(true)">完成</view>
        </view>
      </view>
    </view>
  </view>
</template>

<script>
import NavBar from '@/components/NavBar/NavBar.vue';
import { useThemeStore } from '@/stores/theme';
import { useUserStore } from '@/stores/user';
import { listVipPackages, getVipStatus, subscribeVip, getPayStatus, mockPay } from '@/api/ledger';
import QRCode from 'qrcode';

export default {
  components: { NavBar },
  data() {
    return {
      benefits: ['AI 分析报告', '多账本', 'Excel 导出', '云备份'],
      packages: [],
      selectedId: null,
      loading: false,
      vip: { isVip: false, vipExpire: null },
      // 收银台（V11.81 公共通道）
      cashier: {
        visible: false,
        vipOrderId: null,
        payNo: '',
        packageName: '',
        amount: '0.00',
        mockEnabled: false,
        payStatus: 'CREATED'
      },
      mockPaying: false,
      submitting: false
    };
  },
  computed: {
    themeVars() { return useThemeStore().themeVars; }
  },
  onShow() {
    useThemeStore().restore();
    this.loadPackages();
    this.loadVipStatus();
  },
  onHide() { this.stopPolling(); },
  onUnload() { this.stopPolling(); },
  methods: {
    async loadPackages() {
      this.loading = true;
      try {
        const data = await listVipPackages() || {};
        this.packages = data.records || [];
        if (!this.selectedId && this.packages.length) {
          const popular = this.packages.find(p => p.popular);
          this.selectedId = (popular || this.packages[0]).id;
        }
      } catch (e) { /* 拦截器已提示 */ } finally {
        this.loading = false;
      }
    },
    async loadVipStatus() {
      if (!useUserStore().isLoggedIn) return;
      try {
        this.vip = await getVipStatus() || {};
      } catch (e) { /* 未登录/异常保持默认 */ }
    },
    formatDay(t) {
      if (!t) return '';
      return String(t).replace('T', ' ').substring(0, 10);
    },
    genClientUuid() {
      return 'vip-' + Date.now() + '-' + Math.random().toString(36).substring(2, 10);
    },
    doSubscribe() {
      if (!useUserStore().isLoggedIn) {
        uni.showToast({ title: '请先在「我的」页登录', icon: 'none' });
        return;
      }
      if (!this.selectedId) {
        uni.showToast({ title: '请选择套餐', icon: 'none' });
        return;
      }
      if (this.submitting) return;
      const pkg = this.packages.find(p => p.id === this.selectedId);
      uni.showModal({
        title: '订阅确认',
        content: `「${pkg.name}」¥${pkg.price}（${pkg.durationDays}天，微信支付）`,
        success: async (r) => {
          if (!r.confirm) return;
          this.submitting = true;
          try {
            // V11.81 公共通道：下单拿收银台参数（pending 单 + 网关 codeUrl）
            const cashier = await subscribeVip({
              packageId: this.selectedId,
              clientUuid: this.genClientUuid()
            }) || {};
            this.openCashier(cashier);
          } catch (e) { /* 拦截器已提示 */ } finally {
            this.submitting = false;
          }
        }
      });
    },
    // ===== 收银台（与打赏页同构） =====
    openCashier(cashier) {
      this.cashier = {
        visible: true,
        vipOrderId: cashier.vipOrderId,
        payNo: cashier.payNo || '',
        packageName: cashier.packageName || '',
        amount: cashier.amount != null ? String(cashier.amount) : '0.00',
        mockEnabled: !!cashier.mockEnabled,
        payStatus: 'CREATED'
      };
      this.$nextTick(() => this.renderQr(cashier.codeUrl));
      this.startPolling();
    },
    async renderQr(codeUrl) {
      if (!codeUrl) return;
      // #ifdef H5
      try {
        const host = document.getElementById('vipQr' + this.cashier.vipOrderId);
        const el = host instanceof HTMLCanvasElement ? host : (host && host.querySelector('canvas'));
        if (el) await QRCode.toCanvas(el, codeUrl, { width: 180, height: 180 });
      } catch (e) { /* 二维码渲染失败时可用 mock 支付 */ }
      // #endif
    },
    startPolling() {
      this.stopPolling();
      this.pollTimer = setInterval(async () => {
        if (!this.cashier.payNo || this.cashier.payStatus === 'PAID') return;
        try {
          const st = await getPayStatus(this.cashier.payNo) || {};
          if (st.status === 'PAID') {
            this.onPaid();
          }
        } catch (e) { /* 网络异常继续轮询 */ }
      }, 3000);
    },
    stopPolling() {
      if (this.pollTimer) {
        clearInterval(this.pollTimer);
        this.pollTimer = null;
      }
    },
    async doMockPay() {
      if (this.mockPaying || !this.cashier.payNo) return;
      this.mockPaying = true;
      try {
        await mockPay(this.cashier.payNo);
        this.onPaid();
      } catch (e) { /* 拦截器已提示 */ } finally {
        this.mockPaying = false;
      }
    },
    onPaid() {
      this.cashier.payStatus = 'PAID';
      this.stopPolling();
      uni.showToast({ title: '订阅成功，感谢支持！', icon: 'success' });
      this.loadVipStatus();
    },
    closeCashier(paid) {
      this.stopPolling();
      this.cashier.visible = false;
    }
  }
};
</script>

<style scoped>
.page { padding-bottom: 160rpx; min-height: 100vh; background: #f5f6f8; }

/* 状态卡 */
.status-card {
  background: #fff; margin: 24rpx; border-radius: 20rpx; padding: 32rpx;
  display: flex; align-items: center;
}
.status-card.vip {
  background: linear-gradient(135deg, #2b2b33, #4a4030);
}
.status-left { display: flex; align-items: center; gap: 20rpx; }
.status-icon { font-size: 56rpx; }
.status-text { display: flex; flex-direction: column; gap: 6rpx; }
.status-title { font-size: 32rpx; color: #333; font-weight: 600; }
.status-card.vip .status-title { color: #e6c07a; }
.status-sub { font-size: 24rpx; color: #999; }
.status-card.vip .status-sub { color: #b3a58a; }

/* 权益 */
.benefit-card {
  background: #fff; margin: 0 24rpx 24rpx; border-radius: 20rpx; padding: 24rpx 32rpx;
}
.benefit-title { font-size: 28rpx; color: #333; font-weight: 600; margin-bottom: 12rpx; }
.benefit-row { display: flex; align-items: center; gap: 12rpx; padding: 12rpx 0; }
.benefit-check { color: #07c160; font-size: 26rpx; font-weight: 700; }
.benefit-name { font-size: 26rpx; color: #555; }
.benefit-note { display: block; font-size: 22rpx; color: #999; margin-top: 8rpx; line-height: 1.6; }

/* 套餐 */
.pkg-title { font-size: 28rpx; color: #333; padding: 8rpx 32rpx; }
.pkg-card {
  position: relative; background: #fff; margin: 0 24rpx 20rpx; border-radius: 20rpx;
  padding: 28rpx 32rpx; border: 3rpx solid transparent;
}
.pkg-card.active { border-color: var(--primary-strong); }
.pkg-popular {
  position: absolute; top: 0; right: 0; background: #e6a23c; color: #fff;
  font-size: 20rpx; padding: 6rpx 16rpx; border-radius: 0 16rpx 0 16rpx;
}
.pkg-name { font-size: 32rpx; color: #333; font-weight: 600; }
.pkg-duration { font-size: 24rpx; color: #999; margin-top: 4rpx; }
.pkg-price { display: flex; align-items: baseline; gap: 12rpx; margin-top: 12rpx; }
.pkg-price-num { font-size: 44rpx; color: var(--primary-strong); font-weight: 700; }
.pkg-price-original { font-size: 24rpx; color: #ccc; text-decoration: line-through; }
.pkg-desc { font-size: 24rpx; color: #777; margin-top: 8rpx; }
.pkg-empty { text-align: center; font-size: 26rpx; color: #999; padding: 40rpx 0; }

.pay-btn {
  position: fixed; bottom: 40rpx; left: 24rpx; right: 24rpx;
  height: 88rpx; line-height: 88rpx; text-align: center;
  background: var(--primary-strong); color: #fff;
  border-radius: 44rpx; font-size: 30rpx; font-weight: 600;
}
.pay-btn:active { opacity: 0.85; }

/* 收银台（与打赏页同构） */
.cashier-mask {
  position: fixed; inset: 0; background: rgba(0, 0, 0, 0.55);
  display: flex; align-items: center; justify-content: center; z-index: 999;
}
.cashier-panel {
  width: 560rpx; background: #fff; border-radius: 24rpx; padding: 40rpx 32rpx;
  display: flex; flex-direction: column; align-items: center;
}
.cashier-title { font-size: 32rpx; color: #333; font-weight: 600; }
.cashier-pkg { font-size: 24rpx; color: #999; margin-top: 8rpx; }
.cashier-amount { font-size: 48rpx; color: #333; font-weight: 700; margin: 12rpx 0 24rpx; }
.cashier-qr-wrap {
  width: 360rpx; height: 360rpx; background: #fafafa;
  border-radius: 16rpx; display: flex; align-items: center; justify-content: center;
}
.cashier-qr { width: 180px; height: 180px; }
.cashier-tip { font-size: 24rpx; color: #999; margin-top: 20rpx; }
.cashier-status { font-size: 26rpx; color: #e6a23c; margin-top: 12rpx; }
.cashier-status.paid { color: #07c160; }
.cashier-actions { display: flex; gap: 24rpx; margin-top: 32rpx; width: 100%; }
.cashier-btn {
  flex: 1; height: 76rpx; line-height: 76rpx; text-align: center;
  border-radius: 38rpx; font-size: 28rpx;
}
.cashier-btn.ghost { background: #f2f3f5; color: #666; }
.cashier-btn.primary { background: #07c160; color: #fff; font-weight: 500; }
.cashier-btn:active { opacity: 0.85; }
</style>
