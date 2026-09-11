<template>
  <view class="page" :style="themeVars">
    <NavBar title="新建计划" />

    <view class="form-card">
      <view class="form-row">
        <text class="form-label">计划名称</text>
        <input class="form-input" v-model="form.name" placeholder="例如：买房基金" />
      </view>
      <view class="form-row">
        <text class="form-label">目标金额</text>
        <input class="form-input" type="digit" v-model="form.targetAmount" placeholder="请输入目标金额" />
      </view>
      <view class="form-row">
        <text class="form-label">存钱方式</text>
        <view class="method-group">
          <view class="method-tag" :class="{ active: form.method === '52week' }" @tap="form.method = '52week'">52周存钱法</view>
          <view class="method-tag" :class="{ active: form.method === 'fixed' }" @tap="form.method = 'fixed'">固定金额</view>
          <view class="method-tag" :class="{ active: form.method === 'monthly' }" @tap="form.method = 'monthly'">每月固定存</view>
          <view class="method-tag" :class="{ active: form.method === 'custom' }" @tap="form.method = 'custom'">自定义递增</view>
        </view>
      </view>
      <view class="form-row" v-if="form.method === 'fixed' || form.method === 'monthly'">
        <text class="form-label">每期金额</text>
        <input class="form-input" type="digit" v-model="form.periodAmount" placeholder="每期存入金额" />
      </view>
      <block v-if="form.method === 'custom'">
        <view class="form-row">
          <text class="form-label">首期金额</text>
          <input class="form-input" type="digit" v-model="form.periodAmount" placeholder="第1期存入金额" />
        </view>
        <view class="form-row">
          <text class="form-label">总期数</text>
          <input class="form-input" type="number" v-model="form.periodCount" placeholder="如 24" />
        </view>
        <view class="form-row">
          <text class="form-label">每期递增</text>
          <input class="form-input" type="digit" v-model="form.increaseStep" placeholder="每期比上期多存（可为0）" />
        </view>
      </block>
      <view class="form-row">
        <text class="form-label">开始日期</text>
        <picker mode="date" :value="form.startDate" @change="(e) => form.startDate = e.detail.value">
          <view class="form-value">{{ form.startDate }} ›</view>
        </picker>
      </view>
      <view class="form-row">
        <text class="form-label">结束日期</text>
        <picker mode="date" :value="form.endDate" @change="(e) => form.endDate = e.detail.value">
          <view class="form-value">{{ form.endDate || '不填表示不限期' }} ›</view>
        </picker>
      </view>
      <view class="form-row">
        <text class="form-label">备注</text>
        <input class="form-input" v-model="form.remark" placeholder="选填" />
      </view>
    </view>

    <!-- 52周存钱法说明 -->
    <view class="tip-card" v-if="form.method === '52week'">
      <text class="tip-title">52周存钱法</text>
      <text class="tip-desc">第1周存¥10，第2周存¥20，……第52周存¥520，累计¥13,780。保存后自动生成52期待存期次。</text>
      <view class="gen-btn" @tap="gen52Week">按此规则填入目标 ¥13,780</view>
    </view>
    <view class="tip-card" v-else-if="form.method === 'custom'">
      <text class="tip-title">自定义递增存钱法</text>
      <text class="tip-desc">例如：首期¥100，每期递增¥50，共12期 → 累计¥4,500。最后一期自动按目标金额凑整。</text>
    </view>

    <view class="submit-btn" @tap="save">保存计划</view>
  </view>
</template>

<script>
import NavBar from '@/components/NavBar/NavBar.vue';
import { useThemeStore } from '@/stores/theme';
import { createSavingPlan } from '@/api/ledger';

export default {
  components: { NavBar },
  data() {
    const today = new Date();
    return {
      form: {
        name: '',
        targetAmount: '',
        method: '52week',
        periodAmount: '',
        periodCount: '',
        increaseStep: '',
        startDate: `${today.getFullYear()}-${String(today.getMonth()+1).padStart(2,'0')}-${String(today.getDate()).padStart(2,'0')}`,
        endDate: '',
        remark: ''
      }
    };
  },
  computed: { themeVars() { return useThemeStore().themeVars; } },
  onShow() { useThemeStore().restore(); },
  methods: {
    gen52Week() {
      this.form.targetAmount = '13780';
      uni.showToast({ title: '已填入目标¥13,780', icon: 'none' });
    },
    async save() {
      if (!this.form.name) { uni.showToast({ title: '请输入计划名称', icon: 'none' }); return; }
      const target = parseFloat(this.form.targetAmount);
      if (!target || target <= 0) { uni.showToast({ title: '请输入目标金额', icon: 'none' }); return; }
      const payload = {
        name: this.form.name.trim(),
        targetAmount: target,
        method: this.form.method,
        startDate: this.form.startDate,
        endDate: this.form.endDate || null,
        remark: this.form.remark || null
      };
      if (this.form.method === 'fixed' || this.form.method === 'monthly' || this.form.method === 'custom') {
        const period = parseFloat(this.form.periodAmount);
        if (!period || period <= 0) {
          uni.showToast({ title: this.form.method === 'custom' ? '请输入首期金额' : '请输入每期金额', icon: 'none' });
          return;
        }
        payload.periodAmount = period;
      }
      if (this.form.method === 'custom') {
        const count = parseInt(this.form.periodCount);
        if (!count || count <= 0) { uni.showToast({ title: '请输入总期数', icon: 'none' }); return; }
        payload.periodCount = count;
        payload.increaseStep = parseFloat(this.form.increaseStep) || 0;
      }
      try {
        await createSavingPlan(payload);
        uni.showToast({ title: '创建成功', icon: 'success' });
        setTimeout(() => uni.navigateBack(), 600);
      } catch (e) { /* 拦截器已提示 */ }
    }
  }
};
</script>

<style scoped>
.page { padding-bottom: 160rpx; min-height: 100vh; background: #f5f6f8; }
.form-card { background: #fff; margin: 24rpx; border-radius: 20rpx; padding: 8rpx 32rpx; }
.form-row {
  display: flex; align-items: center; padding: 28rpx 0; border-bottom: 1rpx solid #f5f5f7;
}
.form-row:last-child { border-bottom: none; }
.form-label { width: 180rpx; font-size: 28rpx; color: #333; flex-shrink: 0; }
.form-input { flex: 1; text-align: right; font-size: 28rpx; }
.form-value { font-size: 28rpx; color: #333; }
.method-group { flex: 1; display: flex; gap: 16rpx; justify-content: flex-end; flex-wrap: wrap; }
.method-tag {
  font-size: 24rpx; padding: 8rpx 20rpx; border-radius: 24rpx;
  background: #f5f6f8; color: #666;
}
.method-tag.active { background: var(--primary-soft); color: var(--primary-strong); }

.tip-card {
  background: #fffbe6; border: 1rpx solid #ffe58f;
  margin: 0 24rpx; border-radius: 16rpx; padding: 24rpx;
}
.tip-title { font-size: 26rpx; font-weight: 600; color: #d48806; }
.tip-desc { display: block; font-size: 22rpx; color: #8c6d1f; line-height: 1.6; margin-top: 8rpx; }
.gen-btn {
  margin-top: 16rpx; display: inline-block; padding: 8rpx 24rpx;
  background: var(--primary-strong); color: #fff; border-radius: 24rpx; font-size: 24rpx;
}

.submit-btn {
  position: fixed; bottom: 40rpx; left: 24rpx; right: 24rpx;
  height: 88rpx; line-height: 88rpx; text-align: center;
  background: var(--primary-strong); color: #fff;
  border-radius: 44rpx; font-size: 30rpx; font-weight: 600;
}
</style>