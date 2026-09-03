<template>
  <view class="page">
    <view class="card" v-if="txn">
      <view class="info-row">
        <text class="info-label">类型</text>
        <text class="info-value">{{ typeText(txn.type) }}</text>
      </view>
      <view class="info-row">
        <text class="info-label">金额(元)</text>
        <input v-model="amountYuan" type="digit" class="info-input" />
      </view>
      <view class="info-row">
        <text class="info-label">备注</text>
        <input v-model="txn.description" placeholder="选填" class="info-input" />
      </view>
      <view class="info-row">
        <text class="info-label">商户</text>
        <input v-model="txn.merchant" placeholder="选填" class="info-input" />
      </view>
      <view class="info-row">
        <text class="info-label">日期</text>
        <picker mode="date" :value="txn.transactionDate" @change="(e) => txn.transactionDate = e.detail.value">
          <view class="info-value">{{ txn.transactionDate }} ▾</view>
        </picker>
      </view>
      <view class="info-row">
        <text class="info-label">凭证</text>
        <view class="voucher-area">
          <image v-if="txn.voucherUrl" :src="txn.voucherUrl" mode="aspectFill" class="voucher-thumb"
                 @tap="previewVoucher" />
          <view v-else class="voucher-none" @tap="chooseVoucher">未设置，点击上传</view>
          <view v-if="txn.voucherUrl" class="voucher-ops">
            <text class="voucher-op" @tap="chooseVoucher">更换</text>
            <text class="voucher-op danger" @tap="removeVoucher">移除</text>
          </view>
        </view>
      </view>
      <view class="tip">提示：修改将冲正原记录并按新参数重放，余额以事务方式联动更新</view>
      <view class="btn-primary" @tap="save">保存修改</view>
      <view class="btn-danger" @tap="remove">删除此流水</view>
    </view>
  </view>
</template>

<script>
import { pageTransactions, updateTransaction, deleteTransaction, uploadVoucher } from '@/api/ledger';
import { centToYuan, yuanToCent, typeText } from '@/utils/money';

export default {
  data() {
    return {
      id: null,
      txn: null,
      amountYuan: ''
    };
  },
  onLoad(options) {
    this.id = options.id;
    this.load();
  },
  methods: {
    typeText,
    async load() {
      try {
        const res = await pageTransactions({ pageNum: 1, pageSize: 50 });
        const list = (res && res.records) || [];
        const txn = list.find(t => String(t.id) === String(this.id));
        if (!txn) {
          uni.showToast({ title: '流水不存在或已删除', icon: 'none' });
          setTimeout(() => uni.navigateBack(), 1200);
          return;
        }
        this.txn = txn;
        this.amountYuan = centToYuan(txn.amount);
      } catch (e) { /* 拦截器已提示 */ }
    },
    async save() {
      const cent = yuanToCent(this.amountYuan);
      if (this.txn.type === 'adjust' ? cent === 0 : cent <= 0) {
        uni.showToast({ title: '金额不合法', icon: 'none' });
        return;
      }
      try {
        await updateTransaction(this.id, {
          type: this.txn.type,
          amount: cent,
          categoryId: this.txn.categoryId,
          accountId: this.txn.accountId,
          liabilityId: this.txn.liabilityId,
          targetAccountId: this.txn.targetAccountId,
          description: this.txn.description || null,
          merchant: this.txn.merchant || null,
          voucherUrl: this.txn.voucherUrl || '',
          transactionDate: this.txn.transactionDate
        });
        uni.showToast({ title: '已修改', icon: 'success' });
        setTimeout(() => uni.navigateBack(), 800);
      } catch (e) { /* 拦截器已提示 */ }
    },
    // ---------------- 凭证 ----------------
    chooseVoucher() {
      uni.chooseImage({
        count: 1,
        sizeType: ['compressed'],
        sourceType: ['album', 'camera'],
        success: (res) => {
          uni.showLoading({ title: '上传中', mask: true });
          uploadVoucher(res.tempFilePaths[0]).then((url) => {
            uni.hideLoading();
            this.txn.voucherUrl = url;
          }).catch(() => uni.hideLoading());
        }
      });
    },
    previewVoucher() {
      if (!this.txn.voucherUrl) return;
      uni.previewImage({ urls: [this.txn.voucherUrl] });
    },
    removeVoucher() {
      this.txn.voucherUrl = '';
    },
    remove() {
      uni.showModal({
        title: '删除确认',
        content: '将冲正余额并归档该流水，是否删除？',
        success: async (r) => {
          if (r.confirm) {
            await deleteTransaction(this.id);
            uni.showToast({ title: '已删除', icon: 'success' });
            setTimeout(() => uni.navigateBack(), 800);
          }
        }
      });
    }
  }
};
</script>

<style scoped>
.page { padding: 24rpx; }
.card { background: #fff; border-radius: 20rpx; padding: 24rpx 32rpx; }
.info-row {
  display: flex; align-items: center; padding: 28rpx 0; border-bottom: 1rpx solid #f5f5f7;
}
.info-label { width: 180rpx; color: #666; font-size: 28rpx; }
.info-value { flex: 1; text-align: right; font-size: 28rpx; }
.info-input { flex: 1; text-align: right; font-size: 28rpx; }
.tip { font-size: 22rpx; color: #999; padding: 24rpx 0; line-height: 1.6; }
.voucher-area { flex: 1; display: flex; justify-content: flex-end; align-items: center; }
.voucher-thumb { width: 110rpx; height: 110rpx; border-radius: 12rpx; border: 1rpx solid #eee; }
.voucher-none { font-size: 24rpx; color: #6a4fd4; }
.voucher-ops { display: flex; flex-direction: column; margin-left: 16rpx; gap: 8rpx; }
.voucher-op { font-size: 22rpx; color: #6a4fd4; line-height: 1.4; }
.voucher-op.danger { color: #e74c3c; }
.btn-primary { margin-top: 24rpx; }
.btn-danger {
  margin-top: 24rpx; text-align: center; color: #e74c3c; font-size: 28rpx;
  border: 1rpx solid #e74c3c; border-radius: 44rpx; height: 80rpx; line-height: 80rpx;
}
</style>
