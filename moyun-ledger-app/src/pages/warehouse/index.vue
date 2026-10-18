<template>
  <view class="page" :style="themeVars">
    <NavBar title="仓库" />

    <!-- 仓库切换（个人/家庭/合伙） -->
    <scroll-view scroll-x class="wh-bar" v-if="warehouses.length">
      <view v-for="w in warehouses" :key="w.id" class="wh-tag"
            :class="{ active: w.id === currentId }" @tap="switchWh(w)">
        {{ w.name }} · {{ whTypeText(w.warehouseType) }}
      </view>
      <view class="wh-tag add" @tap="openWhSheet()">＋新建</view>
    </scroll-view>
    <view v-else class="empty" @tap="openWhSheet()">还没有仓库，点此新建第一个仓库</view>

    <!-- 商品列表（带库存） -->
    <view class="card" v-if="currentId">
      <view class="card-title">
        <text class="flex-1">商品</text>
        <text class="link" @tap="openOrderSheet()">下单</text>
        <text class="link" style="margin-left: 16rpx" @tap="openProductSheet()">＋新增商品</text>
      </view>
      <view v-if="!products.length" class="empty">还没有商品，点「＋新增商品」添加</view>
      <view v-for="p in products" :key="p.id" class="row" @tap="openStock(p)">
        <view class="flex-1 min-w-0">
          <view class="row-name">{{ p.name }}</view>
          <view class="row-sub">{{ p.category || '未分类' }} · {{ p.unit || '个' }}</view>
        </view>
        <view class="row-right">
          <view class="qty" :class="{ warn: (p.stockQuantity || 0) === 0 }">库存 {{ p.stockQuantity || 0 }}</view>
          <view class="price" v-if="p.sellPrice">¥{{ fmt(p.sellPrice) }}</view>
        </view>
      </view>
    </view>

    <!-- 新建仓库弹层 -->
    <view class="mask" v-if="whSheet" @tap="whSheet = false">
      <view class="sheet" @tap.stop>
        <view class="sheet-title">新建仓库</view>
        <view class="f-row"><text class="f-label">名称</text>
          <input class="f-input" v-model="whForm.name" placeholder="如：我的小店" /></view>
        <view class="f-row"><text class="f-label">模式</text>
          <picker :range="whTypeNames" @change="onWhTypeChange">
            <view class="f-value">{{ whTypeNames[whTypeIndex] }} ▾</view>
          </picker></view>
        <view class="sheet-actions">
          <view class="btn ghost" @tap="whSheet = false">取消</view>
          <view class="btn" @tap="saveWh">保存</view>
        </view>
      </view>
    </view>

    <!-- 新增商品弹层 -->
    <view class="mask" v-if="productSheet" @tap="productSheet = false">
      <view class="sheet" @tap.stop>
        <view class="sheet-title">新增商品</view>
        <view class="f-row"><text class="f-label">名称</text>
          <input class="f-input" v-model="pForm.name" placeholder="商品名" /></view>
        <view class="f-row"><text class="f-label">分类</text>
          <input class="f-input" v-model="pForm.category" placeholder="选填" /></view>
        <view class="f-row"><text class="f-label">成本价</text>
          <input class="f-input" type="digit" v-model="pForm.costPrice" placeholder="选填" /></view>
        <view class="f-row"><text class="f-label">售价</text>
          <input class="f-input" type="digit" v-model="pForm.sellPrice" placeholder="选填" /></view>
        <view class="sheet-actions">
          <view class="btn ghost" @tap="productSheet = false">取消</view>
          <view class="btn" @tap="saveProduct">保存</view>
        </view>
      </view>
    </view>

    <!-- 出入库弹层 -->
    <view class="mask" v-if="stockTarget" @tap="stockTarget = null">
      <view class="sheet" @tap.stop>
        <view class="sheet-title">出入库 · {{ stockTarget.name }}</view>
        <view class="sheet-sub">当前库存 {{ stockTarget.stockQuantity || 0 }}</view>
        <view class="f-row">
          <text class="f-label">方向</text>
          <view class="seg">
            <view class="seg-item" :class="{ on: stockMode === 'in' }" @tap="stockMode = 'in'">入库 +</view>
            <view class="seg-item" :class="{ on: stockMode === 'out' }" @tap="stockMode = 'out'">出库 −</view>
          </view>
        </view>
        <view class="f-row">
          <text class="f-label">数量</text>
          <input class="f-input" type="number" v-model="stockQty" placeholder="0" />
        </view>
        <view class="f-row">
          <text class="f-label">备注</text>
          <input class="f-input" v-model="stockRemark" placeholder="选填" />
        </view>
        <view class="sheet-actions">
          <view class="btn ghost" @tap="stockTarget = null">取消</view>
          <view class="btn" @tap="saveStock">确认</view>
        </view>
      </view>
    </view>

    <!-- 下单弹层（赊销自动建应收 / 赊购自动建应付 + 库存联动） -->
    <view class="mask" v-if="orderSheet" @tap="orderSheet = false">
      <view class="sheet" @tap.stop>
        <view class="sheet-title">下单</view>
        <view class="f-row">
          <text class="f-label">类型</text>
          <view class="seg">
            <view class="seg-item" :class="{ on: orderForm.mode === 'sale' }" @tap="orderForm.mode = 'sale'">销售</view>
            <view class="seg-item" :class="{ on: orderForm.mode === 'purchase' }" @tap="orderForm.mode = 'purchase'">采购</view>
          </view>
        </view>
        <view class="f-row">
          <text class="f-label">{{ orderForm.mode === 'sale' ? '客户' : '供应商' }}</text>
          <input class="f-input" v-model="orderForm.party"
            :placeholder="orderForm.mode === 'sale' ? '客户名称（建应收用）' : '供应商名称（建应付用）'" />
        </view>

        <!-- 商品明细 -->
        <view v-for="(it, idx) in orderForm.items" :key="idx" class="order-line">
          <picker :range="productNames" @change="onOrderItemProduct(idx, $event)">
            <view class="f-value order-pick">{{ productName(it.productId) || '选商品' }} ▾</view>
          </picker>
          <input class="f-input order-qty" type="number" v-model="it.quantity" placeholder="数量" />
          <input class="f-input order-price" type="digit" v-model="it.unitPrice" placeholder="单价" />
          <text class="order-del" @tap="removeOrderItem(idx)">×</text>
        </view>
        <view class="f-row">
          <text class="f-label">合计</text>
          <text class="f-value">¥{{ orderTotalText }}</text>
        </view>
        <view class="sheet-sub">赊账下单：发货/收货后自动生成{{ orderForm.mode === 'sale' ? '应收款' : '应付款' }}，可到资产负债页查看</view>

        <view class="sheet-actions">
          <view class="btn ghost" @tap="addOrderItem">＋ 加明细</view>
          <view class="btn" @tap="submitOrder">提交下单</view>
        </view>
      </view>
    </view>
  </view>
</template>

<script>
import NavBar from '@/components/NavBar/NavBar.vue';
import { useThemeStore } from '@/stores/theme';
import { listWarehouses, createWarehouse, listProducts, createProduct, stockIn, stockOut,
  createSaleOrder, createPurchaseOrder } from '@/api/ledger';

const WH_TYPES = [
  ['personal', '个人仓'],
  ['family', '家庭仓'],
  ['partner', '合伙仓']
];

export default {
  components: { NavBar },
  data() {
    return {
      warehouses: [],
      currentId: null,
      products: [],
      whSheet: false,
      productSheet: false,
      whForm: { name: '', type: 'personal' },
      whTypeIndex: 0,
      pForm: { name: '', category: '', costPrice: '', sellPrice: '' },
      stockTarget: null,
      stockMode: 'in',
      stockQty: '',
      stockRemark: '',
      // 下单（销售/采购）：赊账自动建应收/应付 + 库存联动
      orderSheet: false,
      orderForm: { mode: 'sale', party: '', items: [] }
    };
  },
  computed: {
    themeVars() { return useThemeStore().themeVars; },
    whTypeNames() { return WH_TYPES.map(t => t[1]); },
    productNames() { return this.products.map(p => p.name); },
    orderTotalText() {
      const sum = (this.orderForm.items || []).reduce((s, it) => {
        return s + Number(it.quantity || 0) * Number(it.unitPrice || 0);
      }, 0);
      return Number(sum).toFixed(2);
    }
  },
  onShow() {
    useThemeStore().restore();
    this.loadWarehouses();
  },
  methods: {
    fmt(v) { return Number(v || 0).toFixed(2); },
    // ===== 下单 =====
    /** 打开下单弹层（默认带一行明细） */
    openOrderSheet() {
      if (!this.currentId) { uni.showToast({ title: '请先新建/选择仓库', icon: 'none' }); return; }
      if (!this.products.length) { uni.showToast({ title: '请先添加商品', icon: 'none' }); return; }
      this.orderForm = { mode: 'sale', party: '', items: [{ productId: null, quantity: '', unitPrice: '' }] };
      this.orderSheet = true;
    },
    addOrderItem() {
      this.orderForm.items.push({ productId: null, quantity: '', unitPrice: '' });
    },
    removeOrderItem(idx) {
      this.orderForm.items.splice(idx, 1);
    },
    onOrderItemProduct(idx, e) {
      const p = this.products[e.detail.value];
      if (!p) return;
      const it = this.orderForm.items[idx];
      it.productId = p.id;
      // 首次选商品时用商品售价/成本价兜底，仍可手改
      if (!it.unitPrice) {
        it.unitPrice = this.orderForm.mode === 'sale' ? (p.sellPrice || p.costPrice || '') : (p.costPrice || '');
      }
    },
    productName(id) {
      const p = this.products.find(x => String(x.id) === String(id));
      return p ? p.name : '';
    },
    /** 提交下单：销售→自动建应收；采购→自动建应付；同时联动库存 */
    async submitOrder() {
      const party = (this.orderForm.party || '').trim();
      if (!party) { uni.showToast({ title: '请填' + (this.orderForm.mode === 'sale' ? '客户' : '供应商') + '名称', icon: 'none' }); return; }
      const items = (this.orderForm.items || [])
        .filter(it => it.productId && Number(it.quantity) > 0)
        .map(it => ({ productId: it.productId, quantity: Number(it.quantity), unitPrice: Number(it.unitPrice || 0) }));
      if (!items.length) { uni.showToast({ title: '请至少填一行有效明细', icon: 'none' }); return; }
      try {
        if (this.orderForm.mode === 'sale') {
          await createSaleOrder({ warehouseId: this.currentId, customerName: party, paymentType: 'credit', items });
        } else {
          await createPurchaseOrder({ warehouseId: this.currentId, supplierName: party, paymentType: 'credit', items });
        }
        uni.showToast({ title: '下单成功', icon: 'success' });
        this.orderSheet = false;
        // 库存已变 → 刷新商品；应收/应付账户由后端生成
        this.loadProducts();
      } catch (e) { /* 拦截器已提示 */ }
    },
    whTypeText(t) { const x = WH_TYPES.find(i => i[0] === t); return x ? x[1] : ''; },
    async loadWarehouses() {
      try {
        const res = await listWarehouses() || [];
        this.warehouses = res || [];
        if (this.warehouses.length && (!this.currentId || !this.warehouses.find(w => w.id === this.currentId))) {
          this.currentId = this.warehouses[0].id;
          this.loadProducts();
        } else if (!this.warehouses.length) {
          this.currentId = null;
          this.products = [];
        }
      } catch (e) { /* 拦截器已提示 */ }
    },
    switchWh(w) {
      this.currentId = w.id;
      this.loadProducts();
    },
    async loadProducts() {
      try {
        const res = await listProducts(this.currentId, { pageSize: 100 }) || {};
        this.products = res.records || [];
      } catch (e) { /* 拦截器已提示 */ }
    },
    openWhSheet() { this.whForm = { name: '', type: 'personal' }; this.whTypeIndex = 0; this.whSheet = true; },
    openStock(p) {
      this.stockTarget = p;
      this.stockMode = 'in';
      this.stockQty = '';
      this.stockRemark = '';
    },
    async saveStock() {
      const qty = parseInt(this.stockQty, 10);
      if (!qty || qty <= 0) { uni.showToast({ title: '请输入数量', icon: 'none' }); return; }
      try {
        const fn = this.stockMode === 'in' ? stockIn : stockOut;
        await fn({
          warehouseId: this.currentId,
          productId: this.stockTarget.id,
          quantity: qty,
          remark: this.stockRemark || undefined
        });
        uni.showToast({ title: this.stockMode === 'in' ? '已入库' : '已出库', icon: 'success' });
        this.stockTarget = null;
        this.loadProducts();
      } catch (e) { /* 拦截器已提示 */ }
    },
    openProductSheet() { this.pForm = { name: '', category: '', costPrice: '', sellPrice: '' }; this.productSheet = true; },
    onWhTypeChange(e) { this.whTypeIndex = e.detail.value; this.whForm.type = WH_TYPES[e.detail.value][0]; },
    async saveWh() {
      if (!this.whForm.name.trim()) { uni.showToast({ title: '请填名称', icon: 'none' }); return; }
      try {
        await createWarehouse({ name: this.whForm.name.trim(), warehouseType: this.whForm.type });
        uni.showToast({ title: '已创建', icon: 'success' });
        this.whSheet = false;
        this.loadWarehouses();
      } catch (e) { /* 拦截器已提示 */ }
    },
    async saveProduct() {
      if (!this.pForm.name.trim()) { uni.showToast({ title: '请填商品名', icon: 'none' }); return; }
      try {
        await createProduct({
          warehouseId: this.currentId,
          name: this.pForm.name.trim(),
          category: this.pForm.category || null,
          costPrice: this.pForm.costPrice ? Number(this.pForm.costPrice) : null,
          sellPrice: this.pForm.sellPrice ? Number(this.pForm.sellPrice) : null
        });
        uni.showToast({ title: '已添加', icon: 'success' });
        this.productSheet = false;
        this.loadProducts();
      } catch (e) { /* 拦截器已提示 */ }
    }
  }
};
</script>

<style scoped>
.page { padding-bottom: 40rpx; }
.wh-bar { white-space: nowrap; padding: 20rpx 24rpx 0; }
.wh-tag { display: inline-block; padding: 12rpx 28rpx; margin-right: 16rpx; border-radius: 32rpx; font-size: 24rpx; background: var(--card); color: var(--text-sub); }
.wh-tag.active { background: var(--primary-soft); color: var(--primary-text); font-weight: 600; }
.wh-tag.add { color: var(--primary); border: 1rpx dashed var(--primary); background: transparent; }
.empty { text-align: center; color: var(--text-muted); padding: 80rpx 0; font-size: 26rpx; }
.card { background: var(--card); border-radius: 20rpx; margin: 20rpx 24rpx 0; padding: 8rpx 32rpx; }
.card-title { display: flex; align-items: center; padding: 20rpx 0; font-size: 28rpx; color: var(--text-main); font-weight: 600; }
.flex-1 { flex: 1; }
.link { color: var(--primary); font-size: 24rpx; }
.row { display: flex; align-items: center; padding: 22rpx 0; border-bottom: 1rpx solid var(--border); }
.row:last-child { border-bottom: none; }
.row-name { font-size: 28rpx; color: var(--text-main); }
.row-sub { margin-top: 4rpx; font-size: 22rpx; color: var(--text-muted); }
.row-right { text-align: right; flex-shrink: 0; margin-left: 16rpx; }
.qty { font-size: 26rpx; font-weight: 600; color: var(--income); }
.qty.warn { color: var(--text-muted); }
.price { font-size: 20rpx; color: var(--text-sub); margin-top: 4rpx; }
.mask { position: fixed; top: 0; right: 0; bottom: 0; left: 0; background: rgba(0,0,0,0.45); z-index: 999; display: flex; align-items: center; justify-content: center; }
.sheet { width: 620rpx; background: var(--card); border-radius: 24rpx; padding: 32rpx; }
.sheet-title { font-size: 30rpx; font-weight: 600; text-align: center; color: var(--text-main); margin-bottom: 16rpx; }
.sheet-sub { font-size: 24rpx; color: var(--text-sub); text-align: center; margin-bottom: 12rpx; }
.seg { display: flex; background: var(--surface); border-radius: 12rpx; overflow: hidden; }
.seg-item { flex: 1; text-align: center; padding: 14rpx 0; font-size: 26rpx; color: var(--text-sub); }
.seg-item.on { background: var(--primary-soft); color: var(--primary-text); font-weight: 600; }
.f-row { display: flex; align-items: center; padding: 20rpx 0; border-bottom: 1rpx solid var(--border); }
.f-label { width: 150rpx; font-size: 26rpx; color: var(--text-main); flex-shrink: 0; }
.f-input { flex: 1; min-width: 0; font-size: 26rpx; text-align: right; color: var(--text-main); }
.f-value { font-size: 26rpx; color: var(--text-main); }
.sheet-actions { display: flex; gap: 20rpx; margin-top: 28rpx; }
.btn { flex: 1; text-align: center; padding: 22rpx 0; border-radius: 44rpx; font-size: 28rpx; background: var(--primary); color: var(--on-primary); }
.btn.ghost { background: var(--surface); color: var(--text-sub); }
/* ===== 下单明细行 ===== */
.order-line {
  display: flex; align-items: center; gap: 10rpx;
  padding: 12rpx 0; border-bottom: 1rpx solid var(--border);
}
.order-pick { flex: 1.4; font-size: 24rpx; }
.order-qty { flex: 0.7; }
.order-price { flex: 0.9; }
.order-del {
  font-size: 32rpx; color: #E85D5D; padding: 0 8rpx; line-height: 1;
}
</style>
