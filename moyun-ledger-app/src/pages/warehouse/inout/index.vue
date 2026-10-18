<template>
  <view class="page" :style="themeVars">
    <NavBar title="入库/出库" />

    <!-- 未开启仓库管理 -->
    <view v-if="!enabled" class="locked">
      <view class="locked-icon">📦</view>
      <view class="locked-title">仓库管理未开启</view>
      <view class="locked-sub">开启后可在这里快速记入库/出库，并查看库存流水</view>
      <view class="locked-btn" @tap="goSettings">去开启</view>
    </view>

    <template v-else>
      <!-- 仓库切换 -->
      <scroll-view scroll-x class="wh-bar" v-if="warehouses.length">
        <view v-for="w in warehouses" :key="w.id" class="wh-tag"
              :class="{ active: String(w.id) === String(warehouseId) }" @tap="switchWh(w)">
          {{ w.name }}
        </view>
      </scroll-view>
      <view v-else class="empty-top" @tap="goManage">还没有仓库，去「库存管理」新建一个 ›</view>

      <!-- 快捷操作 -->
      <view class="quick-bar" v-if="warehouseId">
        <view class="quick-btn in" @tap="openForm('in')">入库 +</view>
        <view class="quick-btn out" @tap="openForm('out')">出库 −</view>
        <view class="quick-btn adj" @tap="openForm('adjust')">校准</view>
      </view>

      <!-- 库存流水（按日期分组，类似记账流水） -->
      <view class="flow-wrap" v-if="warehouseId">
        <view v-if="!flowGroups.length && !loading" class="empty">还没有流水，点上面的「入库 +」记一笔</view>
        <view v-for="g in flowGroups" :key="g.date" class="flow-group">
          <view class="flow-date">
            <text class="flex-1">{{ g.date }}</text>
            <text class="flow-count">{{ g.items.length }} 笔</text>
          </view>
          <view v-for="f in g.items" :key="f.id" class="flow-row">
            <view class="fl-badge" :class="{ inn: f.quantityChange > 0 }">{{ f.flowTypeLabel || f.flowType }}</view>
            <view class="flex-1 min-w-0">
              <view class="fl-name">
                {{ f.productName }}
                <text v-if="f.reasonLabel" class="fl-reason"> · {{ f.reasonLabel }}</text>
              </view>
              <view class="fl-meta">
                {{ f.shelfName }}<text v-if="f.toShelfName"> → {{ f.toShelfName }}</text>
                <text v-if="f.remark"> · {{ f.remark }}</text>
              </view>
            </view>
            <view class="fl-right">
              <view class="fl-qty" :class="{ inn: f.quantityChange > 0 }">
                {{ f.quantityChange > 0 ? '+' : '' }}{{ f.quantityChange }}
              </view>
              <view class="fl-time">{{ (f.createTime || '').slice(11, 16) }}</view>
            </view>
          </view>
        </view>
      </view>

      <!-- 底部固定入口 -->
      <view class="fab" v-if="warehouseId" @tap="openForm('in')">＋ 记一笔</view>

      <!-- 快速记账弹层 -->
      <view class="mask" v-if="form.visible" @tap="form.visible = false">
        <view class="sheet" @tap.stop>
          <view class="sheet-title">{{ submitText }}</view>

          <!-- 类型（预选，可改） -->
          <view class="type-bar">
            <view v-for="t in types" :key="t.key" class="type-chip"
                  :class="{ on: form.mode === t.key }" @tap="form.mode = t.key">{{ t.label }}</view>
          </view>

          <view class="f-row" @tap="pickShelf">
            <text class="f-label">货架</text>
            <text class="f-value">{{ shelfLabel }} ▾</text>
          </view>
          <view class="f-row" @tap="pickProduct">
            <text class="f-label">商品</text>
            <text class="f-value">{{ productLabel || '选择商品' }} ▾</text>
          </view>
          <view class="f-row">
            <text class="f-label">{{ form.mode === 'adjust' ? '实际数量' : '数量' }}</text>
            <input class="f-input" type="number" v-model="form.quantity" placeholder="0" />
          </view>
          <view class="f-row" v-if="isSaleOut">
            <text class="f-label">成交单价</text>
            <input class="f-input" type="digit" v-model="form.salePrice" placeholder="选填，填了可算毛利" />
          </view>
          <view class="f-row" v-if="form.mode !== 'adjust'" @tap="pickReason">
            <text class="f-label">原因</text>
            <text class="f-value">{{ reasonLabel }} ▾</text>
          </view>
          <view class="f-row">
            <text class="f-label">备注</text>
            <input class="f-input" v-model="form.remark" placeholder="选填" />
          </view>
          <view class="f-row">
            <text class="f-label">拍照</text>
            <view class="photo-wrap">
              <image v-if="form.photoUrl" :src="form.photoUrl" class="photo" mode="aspectFill" />
              <text class="link" @tap="takePhoto">{{ form.photoUrl ? '重拍' : '拍照 / 相册' }}</text>
            </view>
          </view>

          <view class="sheet-actions">
            <view class="btn ghost" @tap="form.visible = false">取消</view>
            <view class="btn" :class="{ disabled: submitting }" @tap="submit">{{ submitting ? '提交中…' : '确认' }}</view>
          </view>
        </view>
      </view>

      <!-- 通用选择器 -->
      <view class="mask" v-if="picker.visible" @tap="picker.visible = false">
        <view class="sheet" @tap.stop>
          <view class="sheet-title">{{ picker.title }}</view>
          <scroll-view scroll-y class="picker-list">
            <view v-if="!picker.items.length" class="empty">{{ picker.empty }}</view>
            <view v-for="it in picker.items" :key="it.id" class="picker-item"
                  :class="{ active: String(picker.selected) === String(it.id) }" @tap="confirmPick(it)">
              <text>{{ it.name }}</text>
              <text v-if="it.sub" class="picker-sub">{{ it.sub }}</text>
            </view>
          </scroll-view>
          <view class="picker-cancel" @tap="picker.visible = false">取消</view>
        </view>
      </view>
    </template>
  </view>
</template>

<script>
import NavBar from '@/components/NavBar/NavBar.vue';
import { useThemeStore } from '@/stores/theme';
import { useUserStore } from '@/stores/user';
import { isWarehouseEnabled, applyWarehouseTabBar } from '@/utils/warehouseSwitch';
import {
  listWarehouses, listShelves, listProducts, listStockFlows,
  stockIn, stockOut, stocktake, uploadFile, attachProductImage
} from '@/api/ledger';

const TYPES = [
  { key: 'in', label: '入库 +' },
  { key: 'out', label: '出库 −' },
  { key: 'adjust', label: '校准' }
];
const IN_REASONS = [['purchase', '采购'], ['gift', '赠送'], ['other', '其他']];
const OUT_REASONS = [['sale', '销售'], ['self_use', '自用'], ['loss', '损耗'], ['transfer', '转出'], ['gift', '赠送'], ['other', '其他']];

export default {
  components: { NavBar },
  data() {
    return {
      enabled: true,
      warehouses: [],
      shelves: [],
      products: [],
      warehouseId: null,
      flows: [],
      loading: false,
      submitting: false,
      form: { visible: false, mode: 'in', shelfId: 0, productId: null, quantity: '', reason: '', salePrice: '', remark: '', photoUrl: '' },
      picker: { visible: false, mode: '', title: '', items: [], selected: null, empty: '' }
    };
  },
  computed: {
    themeVars() { return useThemeStore().themeVars; },
    types() { return TYPES; },
    /** 流水按日期分组（类似记账流水的日期小标题） */
    flowGroups() {
      const map = new Map();
      for (const f of this.flows) {
        const d = (f.createTime || '').slice(0, 10) || '未知日期';
        if (!map.has(d)) map.set(d, []);
        map.get(d).push(f);
      }
      return Array.from(map.entries()).map(([date, items]) => ({ date, items }));
    },
    reasonList() { return this.form.mode === 'in' ? IN_REASONS : OUT_REASONS; },
    /** 销售出库才需要成交单价（选了其它原因就不显示） */
    isSaleOut() { return this.form.mode === 'out' && (!this.form.reason || this.form.reason === 'sale'); },
    reasonLabel() {
      const r = this.reasonList.find(x => x[0] === this.form.reason);
      if (r) return r[1];
      return this.form.mode === 'in' ? '采购' : '销售';
    },
    shelfLabel() {
      if (!this.form.shelfId) return '未上架';
      const s = this.shelves.find(x => String(x.id) === String(this.form.shelfId));
      return s ? s.name + (s.code ? ' · ' + s.code : '') : '未上架';
    },
    productLabel() {
      const p = this.products.find(x => String(x.id) === String(this.form.productId));
      return p ? p.name : '';
    },
    submitText() {
      if (this.form.mode === 'in') return '记一笔入库';
      if (this.form.mode === 'out') return '记一笔出库';
      return '校准库存';
    }
  },
  onShow() {
    useThemeStore().restore();
    this.enabled = isWarehouseEnabled();
    applyWarehouseTabBar();
    if (!this.enabled) return;
    if (!useUserStore().isLoggedIn) return;
    this.loadWarehouses();
  },
  methods: {
    goSettings() { uni.navigateTo({ url: '/pages/mine/warehouse-settings/index' }); },
    goManage() { uni.navigateTo({ url: '/pages/warehouse/index' }); },
    async loadWarehouses() {
      try {
        this.warehouses = (await listWarehouses()) || [];
        if (this.warehouses.length && !this.warehouseId) {
          this.warehouseId = this.warehouses[0].id;
        }
        if (this.warehouseId) await this.refreshAll();
      } catch (e) { /* 拦截器已提示 */ }
    },
    async refreshAll() {
      await Promise.all([this.loadShelves(), this.loadProducts()]);
      await this.loadFlows();
    },
    async switchWh(w) {
      if (String(w.id) === String(this.warehouseId)) return;
      this.warehouseId = w.id;
      await this.refreshAll();
    },
    async loadShelves() {
      if (!this.warehouseId) { this.shelves = []; return; }
      try { this.shelves = (await listShelves(this.warehouseId)) || []; } catch (e) { /* 忽略 */ }
    },
    async loadProducts() {
      if (!this.warehouseId) { this.products = []; return; }
      try {
        const res = (await listProducts(this.warehouseId, { pageSize: 100 })) || {};
        this.products = res.records || [];
      } catch (e) { /* 忽略 */ }
    },
    async loadFlows() {
      if (!this.warehouseId) { this.flows = []; return; }
      this.loading = true;
      try {
        const res = (await listStockFlows(this.warehouseId, { pageNum: 1, pageSize: 50 })) || {};
        this.flows = res.records || [];
      } catch (e) { /* 忽略 */ }
      this.loading = false;
    },
    openForm(mode) {
      this.form = { visible: true, mode, shelfId: 0, productId: null, quantity: '', reason: '', salePrice: '', remark: '', photoUrl: '' };
    },
    openPicker(mode, title, items, selected, empty) {
      this.picker = { visible: true, mode, title, items, selected, empty: empty || '暂无数据' };
    },
    pickShelf() {
      const items = [{ id: 0, name: '未上架' }].concat(
        this.shelves.map(s => ({ id: s.id, name: s.name, sub: s.code })));
      this.openPicker('shelf', '选择货架', items, this.form.shelfId);
    },
    pickProduct() {
      this.openPicker('product', '选择商品',
        this.products.map(p => ({ id: p.id, name: p.name, sub: '库存 ' + (p.stockQuantity || 0) })),
        this.form.productId, '该仓库还没有商品，先去「库存管理」新增');
    },
    pickReason() {
      this.openPicker('reason', '选择原因',
        this.reasonList.map(x => ({ id: x[0], name: x[1] })), this.form.reason);
    },
    confirmPick(it) {
      const m = this.picker.mode;
      this.picker.visible = false;
      if (m === 'shelf') this.form.shelfId = it.id;
      else if (m === 'product') this.form.productId = it.id;
      else if (m === 'reason') this.form.reason = it.id;
    },
    takePhoto() {
      uni.chooseImage({
        count: 1, sizeType: ['compressed'],
        success: async (res) => {
          const path = res.tempFilePaths && res.tempFilePaths[0];
          if (!path) return;
          uni.showLoading({ title: '上传中', mask: true });
          try {
            this.form.photoUrl = await uploadFile(path, 'ledger_voucher');
            uni.hideLoading();
          } catch (e) { uni.hideLoading(); }
        }
      });
    },
    async submit() {
      if (this.submitting) return;
      const f = this.form;
      if (!f.productId) { uni.showToast({ title: '请选择商品', icon: 'none' }); return; }
      const qty = Number(f.quantity);
      if (f.quantity === '' || isNaN(qty) || (f.mode !== 'adjust' && qty <= 0) || (f.mode === 'adjust' && qty < 0)) {
        uni.showToast({ title: f.mode === 'adjust' ? '请输入实际数量' : '请输入数量', icon: 'none' });
        return;
      }
      this.submitting = true;
      try {
        if (f.mode === 'adjust') {
          await stocktake({
            warehouseId: this.warehouseId, shelfId: f.shelfId || 0,
            items: [{ productId: f.productId, actualQuantity: qty }],
            remark: f.remark || '校准'
          });
        } else {
          const fn = f.mode === 'in' ? stockIn : stockOut;
          await fn({
            warehouseId: this.warehouseId, productId: f.productId, shelfId: f.shelfId || 0,
            reason: f.reason || undefined, quantity: qty, remark: f.remark || undefined,
            salePrice: (this.isSaleOut && f.salePrice !== '' && !isNaN(Number(f.salePrice))) ? Number(f.salePrice) : undefined
          });
        }
        if (f.photoUrl) {
          try { await attachProductImage(f.productId, f.photoUrl, 'voucher'); } catch (e) { /* 忽略 */ }
        }
        uni.showToast({ title: f.mode === 'in' ? '已入库' : (f.mode === 'out' ? '已出库' : '已校准'), icon: 'success' });
        this.form.visible = false;
        await this.refreshAll();
      } catch (e) { /* 拦截器已提示 */ }
      this.submitting = false;
    }
  }
};
</script>

<style scoped>
.page { min-height: 100vh; padding-bottom: 160rpx; }

.locked { padding: 120rpx 40rpx; text-align: center; }
.locked-icon { font-size: 90rpx; }
.locked-title { font-size: 34rpx; font-weight: 700; color: var(--text-main); margin-top: 20rpx; }
.locked-sub { font-size: 26rpx; color: var(--text-sub); margin-top: 12rpx; line-height: 1.6; }
.locked-btn { display: inline-block; margin-top: 32rpx; padding: 20rpx 60rpx; background: var(--primary); color: #fff; border-radius: 999rpx; font-size: 28rpx; }

.wh-bar { white-space: nowrap; padding: 16rpx 24rpx 0; }
.wh-tag {
  display: inline-block; padding: 12rpx 26rpx; margin-right: 12rpx;
  border-radius: 999rpx; font-size: 26rpx; color: var(--text-sub);
  background: var(--card); border: 1rpx solid var(--border);
}
.wh-tag.active { color: #fff; background: var(--primary); border-color: var(--primary); }
.empty-top { padding: 40rpx 24rpx; font-size: 26rpx; color: var(--text-sub); }

.quick-bar { display: flex; gap: 12rpx; padding: 20rpx 24rpx 4rpx; }
.quick-btn {
  flex: 1; text-align: center; padding: 24rpx 0; border-radius: 14rpx;
  font-size: 28rpx; font-weight: 600; color: #fff;
}
.quick-btn.in { background: #0EA37A; }
.quick-btn.out { background: #E8654A; }
.quick-btn.adj { background: var(--primary); }

.flow-wrap { padding: 12rpx 24rpx 0; }
.flow-group { margin-top: 16rpx; }
.flow-date {
  display: flex; align-items: center; font-size: 23rpx; color: var(--text-sub);
  padding: 10rpx 4rpx;
}
.flow-count { font-size: 22rpx; color: var(--text-sub); }
.flow-row {
  display: flex; align-items: center; gap: 12rpx;
  padding: 20rpx 16rpx; margin-bottom: 10rpx;
  background: var(--card); border: 1rpx solid var(--border); border-radius: 14rpx;
}
.fl-badge {
  flex: 0 0 auto; padding: 6rpx 12rpx; border-radius: 6rpx;
  font-size: 20rpx; color: #E8654A; background: rgba(232,101,74,.12);
}
.fl-badge.inn { color: #0EA37A; background: rgba(14,163,122,.12); }
.fl-name { font-size: 28rpx; color: var(--text-main); }
.fl-reason { font-size: 22rpx; color: var(--text-sub); }
.fl-meta { font-size: 22rpx; color: var(--text-sub); margin-top: 4rpx; }
.fl-right { text-align: right; }
.fl-qty { font-size: 30rpx; font-weight: 700; color: #E8654A; }
.fl-qty.inn { color: #0EA37A; }
.fl-time { font-size: 20rpx; color: var(--text-sub); margin-top: 2rpx; }

.fab {
  position: fixed; left: 24rpx; right: 24rpx; bottom: 30rpx;
  text-align: center; padding: 26rpx 0; border-radius: 14rpx;
  background: var(--primary); color: #fff; font-size: 30rpx; font-weight: 600;
  box-shadow: 0 8rpx 24rpx rgba(0,0,0,.12);
}

.mask { position: fixed; inset: 0; background: rgba(0,0,0,.45); z-index: 999; display: flex; align-items: flex-end; }
.sheet { width: 100%; max-height: 84vh; overflow-y: auto; background: var(--card); border-radius: 24rpx 24rpx 0 0; padding: 24rpx 24rpx 40rpx; }
.sheet-title { font-size: 32rpx; font-weight: 700; text-align: center; color: var(--text-main); padding-bottom: 16rpx; }
.type-bar { display: flex; gap: 10rpx; padding-bottom: 12rpx; }
.type-chip {
  flex: 1; text-align: center; padding: 14rpx 0; border-radius: 10rpx;
  font-size: 26rpx; color: var(--text-sub); background: var(--bg); border: 1rpx solid var(--border);
}
.type-chip.on { color: #fff; background: var(--primary); border-color: var(--primary); font-weight: 600; }
.f-row { display: flex; align-items: center; padding: 22rpx 0; border-bottom: 1rpx solid var(--border); }
.f-label { font-size: 28rpx; color: var(--text-sub); width: 150rpx; }
.f-value { flex: 1; text-align: right; font-size: 28rpx; color: var(--text-main); }
.f-input { flex: 1; text-align: right; font-size: 28rpx; color: var(--text-main); }
.photo-wrap { flex: 1; display: flex; align-items: center; justify-content: flex-end; gap: 12rpx; }
.photo { width: 90rpx; height: 90rpx; border-radius: 8rpx; }
.sheet-actions { display: flex; gap: 16rpx; padding-top: 26rpx; }
.btn { flex: 1; text-align: center; padding: 22rpx 0; border-radius: 12rpx; background: var(--primary); color: #fff; font-size: 28rpx; }
.btn.ghost { background: var(--bg); color: var(--text-sub); border: 1rpx solid var(--border); }
.btn.disabled { opacity: .6; }

.picker-list { max-height: 50vh; }
.picker-item { display: flex; align-items: center; justify-content: space-between; padding: 24rpx 8rpx; border-bottom: 1rpx solid var(--border); font-size: 28rpx; }
.picker-item.active { color: var(--primary); font-weight: 600; }
.picker-sub { font-size: 22rpx; color: var(--text-sub); }
.picker-cancel { text-align: center; padding: 24rpx 0 8rpx; font-size: 28rpx; color: var(--text-sub); }
.empty { text-align: center; padding: 60rpx 0; font-size: 26rpx; color: var(--text-sub); }
.link { font-size: 26rpx; color: var(--primary); }
.flex-1 { flex: 1; }
.min-w-0 { min-width: 0; }
</style>
