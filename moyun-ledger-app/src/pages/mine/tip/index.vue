<template>
  <view class="page" :style="themeVars">
    <NavBar title="赞赏" />

    <view class="tip-card">
      <view class="amount-label">赞赏金额（元）</view>
      <!-- 快捷金额单选：2/5/10/20/50 + 其他（自定义输入） -->
      <view class="amount-grid">
        <view v-for="a in presetAmounts" :key="a" class="amount-cell" :class="{ active: selectedAmount === a }" @tap="pickAmount(a)">
          <text class="amount-num">¥{{ a }}</text>
        </view>
        <view class="amount-cell" :class="{ active: selectedAmount === 'custom' }" @tap="pickCustom">
          <text class="amount-num">其他</text>
        </view>
      </view>
      <!-- 其他：自定义金额输入 -->
      <view v-if="selectedAmount === 'custom'" class="tip-row">
        <text class="tip-label">自定义金额</text>
        <input class="tip-input" type="digit" v-model="amount" placeholder="请输入金额，单位：元（如 5.00）" placeholder-style="color:#ccc" />
      </view>
      <view class="tip-row">
        <text class="tip-label">累计赞赏金额：</text>
        <text class="tip-total">¥ {{ totalAmount }}</text>
      </view>
    </view>

    <view class="pay-title">支付</view>
    <view class="pay-card">
      <view class="pay-row" @tap="payChannel = 'wechat'">
        <view class="pay-left">
          <view class="pay-icon wechat">
            <text>💬</text>
          </view>
          <text class="pay-name">微信支付</text>
        </view>
        <view class="pay-check" :class="{ checked: payChannel === 'wechat' }">
          <text v-if="payChannel === 'wechat'">✓</text>
        </view>
      </view>
    </view>

    <view class="tip-note">
      <view>创作不易，感谢打赏鼓励 ❤</view>
      <view class="note-feedback" @tap="goFeedback">和投诉建议一起，提出你的宝贵意见，与平台大家共同成长 →</view>
    </view>

    <view class="pay-btn" @tap="doTip">立即赞赏</view>

    <!-- 我的赞赏记录 -->
    <view class="history-card" v-if="history.length > 0">
      <view class="history-title">我的赞赏记录</view>
      <view v-for="h in history" :key="h.id" class="history-row">
        <view class="history-left">
          <text class="history-amount">¥{{ h.amount }}</text>
          <text class="history-time">{{ formatTime(h.paidTime || h.createTime) }}</text>
        </view>
        <text class="history-status" :class="h.status">{{ statusName(h.status) }}</text>
      </view>
    </view>

    <!-- 收银台弹层（V11.80 公共支付通道） -->
    <view v-if="cashier.visible" class="cashier-mask" @tap="closeCashier(false)">
      <view class="cashier-panel" @tap.stop>
        <view class="cashier-title">微信支付</view>
        <view class="cashier-amount">¥{{ cashier.amount }}</view>
        <view class="cashier-qr-wrap">
          <canvas :id="'tipQr' + cashier.tipOrderId" :canvas-id="'tipQr' + cashier.tipOrderId" class="cashier-qr"></canvas>
        </view>
        <view class="cashier-tip">请使用微信扫一扫完成支付</view>
        <view class="cashier-status" :class="{ paid: cashier.payStatus === 'PAID' }">
          {{ cashier.payStatus === 'PAID' ? '支付成功 ✓' : '等待支付中…' }}
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
import { getTipTotal, listMyTips, createTip, getPayStatus, mockPay } from '@/api/ledger';
import { toFixedYuan } from '@/utils/money';
import QRCode from 'qrcode';

export default {
  components: { NavBar },
  data() {
    return {
      presetAmounts: [2, 5, 10, 20, 50],
      selectedAmount: null, // 数字=快捷金额，'custom'=其他自定义
      amount: '',
      payChannel: 'wechat', // v11.80 统一命名（当前公共通道仅开通微信）
      totalAmount: '0.00',
      history: [],
      // 收银台（V11.80）
      cashier: {
        visible: false,
        tipOrderId: null,
        payNo: '',
        amount: '0.00',
        mockEnabled: false,
        payStatus: 'CREATED'
      },
      mockPaying: false,
      submitting: false
    };
  },
  computed: {
    themeVars() { return useThemeStore().themeVars; },
    /** 当前生效的赞赏金额（快捷选中或自定义输入） */
    effectiveAmount() {
      if (typeof this.selectedAmount === 'number') return this.selectedAmount;
      const num = parseFloat(this.amount);
      return !isNaN(num) && num > 0 ? num : null;
    }
  },
  onShow() {
    useThemeStore().restore();
    this.loadTotal();
    this.loadHistory();
  },
  onHide() {
    this.stopPolling();
  },
  onUnload() {
    this.stopPolling();
  },
  methods: {
    pickAmount(a) {
      this.selectedAmount = a;
    },
    pickCustom() {
      this.selectedAmount = 'custom';
    },
    goFeedback() {
      uni.navigateTo({ url: '/pages/mine/feedback/index' });
    },
    async loadTotal() {
      if (!useUserStore().isLoggedIn) { this.totalAmount = '0.00'; return; }
      try {
        const data = await getTipTotal() || {};
        this.totalAmount = toFixedYuan(data.totalAmount);
      } catch (e) { /* 拦截器已提示 */ }
    },
    async loadHistory() {
      if (!useUserStore().isLoggedIn) { this.history = []; return; }
      try {
        const data = await listMyTips() || {};
        this.history = (data.records || []).slice(0, 10);
      } catch (e) { /* 拦截器已提示 */ }
    },
    statusName(status) {
      const map = { pending: '待支付', paid: '已支付', refunded: '已退款', closed: '已关闭' };
      return map[status] || status || '-';
    },
    formatTime(t) {
      if (!t) return '';
      return String(t).replace('T', ' ').substring(0, 16);
    },
    /** 生成客户端幂等号（防重复提交） */
    genClientUuid() {
      return 'tip-' + Date.now() + '-' + Math.random().toString(36).substring(2, 10);
    },
    async doTip() {
      if (!useUserStore().isLoggedIn) {
        uni.showToast({ title: '请先在「我的」页登录', icon: 'none' });
        return;
      }
      const num = this.effectiveAmount;
      if (!num || num <= 0) {
        uni.showToast({ title: this.selectedAmount === 'custom' ? '请输入有效金额' : '请选择或输入赞赏金额', icon: 'none' });
        return;
      }
      if (this.submitting) return;
      uni.showModal({
        title: '赞赏',
        content: `确认赞赏 ¥${num.toFixed(2)}？（微信支付）`,
        success: async (r) => {
          if (!r.confirm) return;
          this.submitting = true;
          try {
            // V11.80 公共通道：下单拿收银台参数（pending 单 + 网关 codeUrl）
            const cashier = await createTip({
              amount: num,
              payChannel: this.payChannel,
              target: 'developer',
              clientUuid: this.genClientUuid()
            }) || {};
            this.openCashier(cashier);
          } catch (e) { /* 拦截器已提示 */ } finally {
            this.submitting = false;
          }
        }
      });
    },
    // ===== 收银台 =====
    openCashier(cashier) {
      this.cashier = {
        visible: true,
        tipOrderId: cashier.tipOrderId,
        payNo: cashier.payNo || '',
        amount: toFixedYuan(cashier.amount),
        mockEnabled: !!cashier.mockEnabled,
        payStatus: 'CREATED'
      };
      this.$nextTick(() => this.renderQr(cashier.codeUrl));
      this.startPolling();
    },
    async renderQr(codeUrl) {
      if (!codeUrl) return;
      // H5 canvas 渲染二维码（qrcode 包，与门户收银台一致；小程序端后续适配）
      // #ifdef H5
      try {
        const host = document.getElementById('tipQr' + this.cashier.tipOrderId);
        // uni-app H5 编译后 canvas 外层是宿主元素，需取内部原生 canvas
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
            this.cashier.payStatus = 'PAID';
            this.stopPolling();
            uni.showToast({ title: '赞赏成功，感谢支持！', icon: 'success' });
            this.loadTotal();
            this.loadHistory();
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
        this.cashier.payStatus = 'PAID';
        this.stopPolling();
        uni.showToast({ title: '赞赏成功，感谢支持！', icon: 'success' });
        this.loadTotal();
        this.loadHistory();
      } catch (e) { /* 拦截器已提示 */ } finally {
        this.mockPaying = false;
      }
    },
    closeCashier(paid) {
      this.stopPolling();
      this.cashier.visible = false;
      if (!paid) this.loadHistory(); // 取消后刷新（可能有 pending 单）
    }
  }
};
</script>

<style scoped>
.page { padding-bottom: 160rpx; min-height: 100vh; background: #f5f6f8; }
.tip-card {
  background: #fff; margin: 24rpx; border-radius: 20rpx; padding: 24rpx 32rpx 8rpx;
}
.amount-label { font-size: 28rpx; color: #333; padding: 8rpx 0 24rpx; }
.amount-grid {
  display: flex; flex-wrap: wrap; gap: 20rpx;
  padding-bottom: 24rpx; border-bottom: 1rpx solid #f5f5f7;
}
.amount-cell {
  width: calc((100% - 60rpx) / 4); height: 88rpx;
  border-radius: 16rpx; background: #f7f8fa;
  display: flex; align-items: center; justify-content: center;
  border: 2rpx solid transparent; box-sizing: border-box;
}
.amount-cell.active {
  background: rgba(64, 128, 255, 0.08); border-color: var(--primary-strong);
}
.amount-num { font-size: 28rpx; color: #333; font-weight: 500; }
.amount-cell.active .amount-num { color: var(--primary-strong); font-weight: 600; }
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
.note-feedback { color: var(--primary-strong); }

.pay-btn {
  position: fixed; bottom: 40rpx; left: 24rpx; right: 24rpx;
  height: 88rpx; line-height: 88rpx; text-align: center;
  background: var(--primary-strong); color: #fff;
  border-radius: 44rpx; font-size: 30rpx; font-weight: 600;
}
.pay-btn:active { opacity: 0.85; }

/* ===== 赞赏历史 ===== */
.history-card {
  background: #fff; margin: 0 24rpx 24rpx; border-radius: 20rpx; padding: 24rpx 32rpx;
}
.history-title { font-size: 28rpx; color: #333; font-weight: 600; margin-bottom: 8rpx; }
.history-row {
  display: flex; align-items: center; justify-content: space-between;
  padding: 20rpx 0; border-bottom: 1rpx solid #f5f5f7;
}
.history-row:last-child { border-bottom: none; }
.history-left { display: flex; flex-direction: column; gap: 4rpx; }
.history-amount { font-size: 28rpx; color: #333; font-weight: 500; }
.history-time { font-size: 22rpx; color: #999; }
.history-status { font-size: 24rpx; }
.history-status.paid { color: #07c160; }
.history-status.pending { color: #e6a23c; }
.history-status.refunded, .history-status.closed { color: #999; }

/* ===== 收银台弹层 ===== */
.cashier-mask {
  position: fixed; inset: 0; background: rgba(0, 0, 0, 0.55);
  display: flex; align-items: center; justify-content: center; z-index: 999;
}
.cashier-panel {
  width: 560rpx; background: #fff; border-radius: 24rpx; padding: 40rpx 32rpx;
  display: flex; flex-direction: column; align-items: center;
}
.cashier-title { font-size: 32rpx; color: #333; font-weight: 600; }
.cashier-amount { font-size: 48rpx; color: #333; font-weight: 700; margin: 16rpx 0 24rpx; }
.cashier-qr-wrap {
  width: 360rpx; height: 360rpx; background: #fafafa;
  border-radius: 16rpx; display: flex; align-items: center; justify-content: center;
}
.cashier-qr { width: 180px; height: 180px; }
.cashier-tip { font-size: 24rpx; color: #999; margin-top: 20rpx; }
.cashier-status { font-size: 26rpx; color: #e6a23c; margin-top: 12rpx; }
.cashier-status.paid { color: #07c160; }
.cashier-actions {
  display: flex; gap: 24rpx; margin-top: 32rpx; width: 100%;
}
.cashier-btn {
  flex: 1; height: 76rpx; line-height: 76rpx; text-align: center;
  border-radius: 38rpx; font-size: 28rpx;
}
.cashier-btn.ghost { background: #f2f3f5; color: #666; }
.cashier-btn.primary { background: #07c160; color: #fff; font-weight: 500; }
.cashier-btn:active { opacity: 0.85; }
</style>
