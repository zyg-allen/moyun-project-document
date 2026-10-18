<template>
  <view class="page" :style="themeVars">
    <NavBar title="仓库配置" />

    <!-- 自动保存提示（本页没有保存按钮，改动即时生效） -->
    <view class="auto-tip">
      <text class="auto-tip-text">改动自动保存，无需手动提交</text>
      <text class="auto-tip-ok" v-if="savedTip">✓ 已保存</text>
    </view>

    <!-- 常用配置（仓库相关，真正作用到商品列表与出入库表单） -->
    <view class="card">
      <view class="card-title"><text class="flex-1">常用配置</text></view>
      <view class="cfg-row">
        <text class="cfg-label">低库存提醒阈值</text>
        <input class="cfg-input" type="number" v-model="cfg.lowStock" placeholder="0" @blur="saveCfg" @confirm="saveCfg" />
        <text class="cfg-unit">件</text>
      </view>
      <view class="cfg-row">
        <text class="flex-1 cfg-label">在列表显示库存价值</text>
        <switch :checked="cfg.showValue" color="#5FA77A" @change="e => { cfg.showValue = e.detail.value; saveCfg(); }" />
      </view>
      <view class="cfg-row">
        <text class="flex-1 cfg-label">列表显示标签</text>
        <switch :checked="cfg.showTags" color="#5FA77A" @change="e => { cfg.showTags = e.detail.value; saveCfg(); }" />
      </view>
      <view class="cfg-row">
        <text class="flex-1 cfg-label">列表显示到期日期</text>
        <switch :checked="cfg.showExpiry" color="#5FA77A" @change="e => { cfg.showExpiry = e.detail.value; saveCfg(); }" />
      </view>
      <view class="cfg-row">
        <text class="flex-1 cfg-label">商品列表每页条数</text>
        <picker :range="pageSizeOptions" @change="onPageSizeChange">
          <text class="cfg-value">{{ cfg.pageSize }} 条 ▾</text>
        </picker>
      </view>
      <view class="cfg-row">
        <text class="flex-1 cfg-label">出入库默认原因</text>
        <picker :range="reasonOptions" @change="onDefaultReasonChange">
          <text class="cfg-value">{{ cfg.defaultReasonLabel }} ▾</text>
        </picker>
      </view>
      <view class="hint">配置已存到账号（ledger_warehouse_config），换设备自动同步；离线时用本机缓存。</view>
    </view>

  </view>
</template>

<script>
import NavBar from '@/components/NavBar/NavBar.vue';
import { useThemeStore } from '@/stores/theme';
import { isWarehouseEnabled, setWarehouseEnabled, syncWarehouseSwitch, applyWarehouseTabBar } from '@/utils/warehouseSwitch';

import { getWarehouseCfg, saveWarehouseCfg, loadWarehouseCfg } from '@/utils/warehouseConfig';
const REASONS = [['purchase', '采购'], ['sale', '销售'], ['gift', '赠送'], ['other', '其他']];

export default {
  components: { NavBar },
  data() {
    return {
      enabled: true,
      cfg: getWarehouseCfg(),
      savedTip: false,
    };
  },
  computed: {
    themeVars() { return useThemeStore().themeVars; },
    reasonOptions() { return REASONS.map(r => r[1]); },
    pageSizeOptions() { return ['10 条', '20 条', '50 条']; },
    cfgDefaultReasonLabel() {
      const r = REASONS.find(x => x[0] === this.cfg.defaultReason);
      return r ? r[1] : '采购';
    }
  },
  onShow() {
    useThemeStore().restore();
    this.enabled = isWarehouseEnabled();
    syncWarehouseSwitch().then(() => { this.enabled = isWarehouseEnabled(); });
    this.loadCfg();
  },
  methods: {
    async loadCfg() { this.cfg = await loadWarehouseCfg(); },
    /** 改动即存：本机立即生效 + 异步推后端，同时给出「已保存」反馈 */
    saveCfg() {
      this.cfg = saveWarehouseCfg(this.cfg);
      this.savedTip = true;
      clearTimeout(this._tipTimer);
      this._tipTimer = setTimeout(() => { this.savedTip = false; }, 1500);
    },
    /** 每页条数 */
    onPageSizeChange(e) {
      const arr = [10, 20, 50];
      this.cfg.pageSize = arr[Number(e.detail.value)] || 20;
      this.saveCfg();
    },
    onToggle(e) {
      this.enabled = !!e.detail.value;
      setWarehouseEnabled(this.enabled);
      uni.showToast({ title: this.enabled ? '已开启仓库管理' : '已关闭，退回纯记账', icon: 'none' });
    },
    onDefaultReasonChange(e) {
      const r = REASONS[Number(e.detail.value)];
      if (!r) return;
      this.cfg.defaultReason = r[0];
      this.saveCfg();
    }
  }
};
</script>

<style scoped>
.page { min-height: 100vh; padding-bottom: 60rpx; }
.card {
  margin: 16rpx 24rpx 0; padding: 20rpx 24rpx;
  background: var(--card); border: 1rpx solid var(--border); border-radius: 16rpx;
}
.card-title { display: flex; align-items: center; font-size: 30rpx; font-weight: 600; color: var(--text-main); padding: 8rpx 0; }
.hint { font-size: 23rpx; color: var(--text-sub); line-height: 1.6; padding: 12rpx 0; }
.link { font-size: 26rpx; color: var(--primary); }

.cat-row { display: flex; align-items: center; padding: 20rpx 0; border-top: 1rpx solid var(--border); }
.cat-name { font-size: 28rpx; color: var(--text-main); }
.cat-tag { font-size: 20rpx; color: var(--text-sub); border: 1rpx solid var(--border); border-radius: 6rpx; padding: 0 8rpx; margin-left: 8rpx; }
.cat-sub { font-size: 22rpx; color: var(--text-sub); margin-top: 4rpx; }
.cat-del { font-size: 24rpx; color: #E8654A; padding: 8rpx 12rpx; }

.cfg-row { display: flex; align-items: center; padding: 22rpx 0; border-top: 1rpx solid var(--border); font-size: 28rpx; }
.cfg-label { color: var(--text-sub); }
.cfg-input { flex: 1; text-align: right; font-size: 28rpx; color: var(--text-main); }
.cfg-unit { font-size: 24rpx; color: var(--text-sub); margin-left: 8rpx; }
.cfg-value { font-size: 28rpx; color: var(--text-main); }

.mask { position: fixed; inset: 0; background: rgba(0,0,0,.45); z-index: 999; display: flex; align-items: flex-end; }
.sheet { width: 100%; background: var(--card); border-radius: 24rpx 24rpx 0 0; padding: 24rpx 24rpx 40rpx; }
.sheet-title { font-size: 32rpx; font-weight: 700; text-align: center; color: var(--text-main); padding-bottom: 16rpx; }
.f-row { display: flex; align-items: center; padding: 24rpx 0; border-bottom: 1rpx solid var(--border); }
.f-label { font-size: 28rpx; color: var(--text-sub); width: 140rpx; }
.f-input { flex: 1; font-size: 28rpx; color: var(--text-main); }
.f-value { flex: 1; text-align: right; font-size: 28rpx; color: var(--text-main); }
.sheet-actions { display: flex; gap: 16rpx; padding-top: 28rpx; }
.btn { flex: 1; text-align: center; padding: 22rpx 0; border-radius: 12rpx; background: var(--primary); color: #fff; font-size: 28rpx; }
.btn.ghost { background: var(--bg); color: var(--text-sub); border: 1rpx solid var(--border); }
.flex-1 { flex: 1; }
.min-w-0 { min-width: 0; }
/* 自动保存提示条 */
.auto-tip {
  display: flex; align-items: center; justify-content: space-between;
  margin: 14rpx 24rpx 0; padding: 12rpx 18rpx;
  border-radius: 10rpx; background: rgba(95,167,122,.10);
}
.auto-tip-text { font-size: 22rpx; color: var(--text-sub); }
.auto-tip-ok { font-size: 22rpx; color: var(--primary); font-weight: 600; }
/* ===== 排版统一 ===== */
.card-title { font-size: var(--fs-title); font-weight: 700; }
.cfg-label { font-size: var(--fs-body); color: var(--text-main); }
.cfg-unit, .cfg-value { font-size: var(--fs-unit); color: var(--text-sub); }
.auto-tip-text, .hint { font-size: var(--fs-unit); color: var(--text-sub); }
</style>
