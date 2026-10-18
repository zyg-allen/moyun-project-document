<template>
  <view class="page" :style="themeVars">
    <NavBar title="仓库设置" />

    <!-- ① 总开关 -->
    <view class="card">
      <view class="card-title">
        <text class="flex-1">使用仓库管理</text>
        <switch :checked="enabled" color="#5FA77A" @change="onToggle" />
      </view>
      <view class="hint">
        开启后底部导航出现「入库/出库」，可以管货架、商品和库存；关闭则退回纯记账。
      </view>
    </view>

    <!-- ② 商品类目维护 -->
    <view class="card">
      <view class="card-title">
        <text class="flex-1">商品类目</text>
        <text class="link" @tap="goCategories()">管理 ›</text>
      </view>
      <view v-if="!categories.length" class="hint">还没有类目，点「＋新增」建一个</view>
      <view v-for="c in categories" :key="c.id" class="cat-row">
        <view class="flex-1 min-w-0">
          <view class="cat-name">
            {{ c.name }}
            <text v-if="c.system" class="cat-tag">预设</text>
          </view>
          <view class="cat-sub">{{ c.usedCount || 0 }} 个商品<text v-if="c.children && c.children.length"> · {{ c.children.length }} 个子类</text></view>
        </view>
        <text v-if="!c.system" class="cat-del" @tap.stop="removeCategory(c)">删除</text>
      </view>
    </view>

    <!-- ③ 配置项 -->
    <view class="card">
      <view class="card-title"><text class="flex-1">常用配置</text></view>
      <view class="cfg-row">
        <text class="cfg-label">低库存提醒阈值</text>
        <input class="cfg-input" type="number" v-model="cfg.lowStock" placeholder="0" @blur="saveCfg" />
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
      <view class="hint">配置目前存本机，后续接入后端用户配置。</view>
    </view>

    <!-- 新增类目弹层 -->
    <view class="mask" v-if="catSheet" @tap="catSheet = false">
      <view class="sheet" @tap.stop>
        <view class="sheet-title">新增商品类目</view>
        <view class="f-row">
          <text class="f-label">名称</text>
          <input class="f-input" v-model="newCatName" placeholder="如 零食、工具" />
        </view>
        <view class="f-row">
          <text class="f-label">父类目</text>
          <picker :range="parentNames" @change="onParentChange">
            <text class="f-value">{{ parentName || '无（一级类目）' }} ▾</text>
          </picker>
        </view>
        <view class="sheet-actions">
          <view class="btn ghost" @tap="catSheet = false">取消</view>
          <view class="btn" @tap="addCategory">保存</view>
        </view>
      </view>
    </view>
  </view>
</template>

<script>
import NavBar from '@/components/NavBar/NavBar.vue';
import { useThemeStore } from '@/stores/theme';
import { isWarehouseEnabled, setWarehouseEnabled, syncWarehouseSwitch, applyWarehouseTabBar } from '@/utils/warehouseSwitch';
import { listProductCategories, createProductCategory, deleteProductCategory } from '@/api/ledger';

import { getWarehouseCfg, saveWarehouseCfg, loadWarehouseCfg } from '@/utils/warehouseConfig';
const REASONS = [['purchase', '采购'], ['sale', '销售'], ['gift', '赠送'], ['other', '其他']];

export default {
  components: { NavBar },
  data() {
    return {
      enabled: true,
      categories: [],
      catSheet: false,
      newCatName: '',
      parentId: 0,
      parentName: '',
      cfg: getWarehouseCfg()
    };
  },
  computed: {
    themeVars() { return useThemeStore().themeVars; },
    reasonOptions() { return REASONS.map(r => r[1]); },
    pageSizeOptions() { return ['10 条', '20 条', '50 条']; },
    parentNames() { return ['无（一级类目）'].concat(this.categories.map(c => c.name)); },
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
    this.loadCategories();
  },
  methods: {
    async loadCfg() { this.cfg = await loadWarehouseCfg(); },
    saveCfg() { this.cfg = saveWarehouseCfg(this.cfg); },
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
    async loadCategories() {
      try {
        this.categories = (await listProductCategories()) || [];
      } catch (e) { /* 拦截器已提示 */ }
    },
    /** 进独立的商品分类管理页（两级树 + 图标，与记账分类同体验） */
    goCategories() { uni.navigateTo({ url: '/pages/mine/product-categories/index' }); },
    openCatSheet() {
      this.newCatName = '';
      this.parentId = 0;
      this.parentName = '';
      this.catSheet = true;
    },
    onParentChange(e) {
      const i = Number(e.detail.value);
      if (i <= 0) {
        this.parentId = 0;
        this.parentName = '';
      } else {
        const c = this.categories[i - 1];
        this.parentId = c.id;
        this.parentName = c.name;
      }
    },
    async addCategory() {
      const name = (this.newCatName || '').trim();
      if (!name) { uni.showToast({ title: '请输入类目名', icon: 'none' }); return; }
      try {
        await createProductCategory({ name, parentId: this.parentId || 0 });
        this.catSheet = false;
        await this.loadCategories();
        uni.showToast({ title: '已添加', icon: 'success' });
      } catch (e) { /* 拦截器已提示 */ }
    },
    removeCategory(c) {
      uni.showModal({
        title: '删除类目',
        content: '删除「' + c.name + '」？有子类目或商品引用时会被拒绝',
        success: async (r) => {
          if (!r.confirm) return;
          try {
            await deleteProductCategory(c.id);
            await this.loadCategories();
            uni.showToast({ title: '已删除', icon: 'success' });
          } catch (e) { /* 拦截器已提示 */ }
        }
      });
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
</style>
