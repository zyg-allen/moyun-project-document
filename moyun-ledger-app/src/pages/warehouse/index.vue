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

    <!-- 货架 / 商品 切换 -->
    <view class="seg-tabs" v-if="currentId">
      <view class="seg-tab" :class="{ on: tab === 'shelf' }" @tap="switchTab('shelf')">货架</view>
      <view class="seg-tab" :class="{ on: tab === 'goods' }" @tap="switchTab('goods')">商品</view>
    </view>

    <!-- 货架列表（存放位置：归属仓库 + 区域/排/层/位 + 图片） -->
    <view class="card" v-if="currentId && tab === 'shelf'">
      <view class="card-title">
        <text class="flex-1">货架 · 存放位置</text>
        <text class="link" @tap="openShelfSheet()">＋新增货架</text>
      </view>
      <view v-if="!shelves.length" class="empty">还没有货架，点「＋新增货架」添加存放位置</view>
      <view class="shelf-grid">
        <view v-for="s in shelves" :key="s.id" class="shelf-cell" @tap="openShelfDetail(s)">
          <view class="shelf-cover">
            <image v-if="s.coverUrl" :src="s.coverUrl" class="shelf-img" mode="aspectFill" />
            <text v-else class="shelf-code">{{ s.code }}</text>
          </view>
          <view class="shelf-name">{{ s.name }}</view>
          <view class="shelf-pos">{{ s.locationText || '未设置位置' }}</view>
          <view class="shelf-meta">{{ s.productCount || 0 }} 种 · {{ s.stockQuantity || 0 }} 件</view>
        </view>
      </view>
    </view>

    <!-- 商品列表（带库存） -->
    <view class="card" v-if="currentId && tab === 'goods'">
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
          <text class="f-label">货架</text>
          <picker :range="stockShelfNames" @change="onStockShelfChange">
            <view class="f-value">{{ stockShelfLabel }} ▾</view>
          </picker>
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

    <!-- 货架新增/编辑 -->
    <view class="mask" v-if="shelfSheet" @tap="shelfSheet = false">
      <view class="sheet" @tap.stop>
        <view class="sheet-title">{{ shelfForm.id ? '编辑货架' : '新增货架' }}</view>
        <view class="f-row"><text class="f-label">编号</text>
          <input class="f-input" v-model="shelfForm.code" placeholder="如 A-01（同仓唯一）" /></view>
        <view class="f-row"><text class="f-label">名称</text>
          <input class="f-input" v-model="shelfForm.name" placeholder="如 A区第1排" /></view>
        <view class="f-row"><text class="f-label">区域</text>
          <input class="f-input" v-model="shelfForm.zone" placeholder="如 A区 / 冷藏区" /></view>
        <view class="f-row">
          <text class="f-label">排层位</text>
          <input class="f-input" type="number" v-model="shelfForm.rowNo" placeholder="排" />
          <input class="f-input" type="number" v-model="shelfForm.layerNo" placeholder="层" />
          <input class="f-input" type="number" v-model="shelfForm.positionNo" placeholder="位" />
        </view>
        <view class="f-row"><text class="f-label">类型</text>
          <picker :range="shelfTypeNames" @change="onShelfTypeChange">
            <view class="f-value">{{ shelfTypeNameOf(shelfForm.shelfType) }} ▾</view>
          </picker></view>
        <view class="f-row"><text class="f-label">容量</text>
          <input class="f-input" type="number" v-model="shelfForm.capacity" placeholder="选填，件" /></view>
        <view class="f-row"><text class="f-label">备注</text>
          <input class="f-input" v-model="shelfForm.remark" placeholder="选填" /></view>
        <view class="sheet-actions">
          <view class="btn ghost" @tap="shelfSheet = false">取消</view>
          <view class="btn" @tap="saveShelf">保存</view>
        </view>
      </view>
    </view>

    <!-- 货架详情（图片 / 位置 / 归属 / 商品明细） -->
    <view class="mask" v-if="shelfDetail.visible" @tap="shelfDetail.visible = false">
      <view class="sheet" @tap.stop>
        <view class="sheet-title">{{ shelfDetail.shelf.name }} · {{ shelfDetail.shelf.code }}</view>

        <!-- 图片（1:N，首张封面） -->
        <scroll-view scroll-x class="detail-imgs">
          <view v-for="(img, i) in shelfDetail.images" :key="img.id" class="detail-img-wrap">
            <image :src="img.fileUrl" class="detail-img" mode="aspectFill" />
            <text v-if="i === 0" class="detail-img-cover">封面</text>
            <text class="detail-img-del" @tap.stop="removeShelfImage(img)">×</text>
          </view>
          <view class="detail-img-add" @tap="uploadShelfImage">＋</view>
        </scroll-view>

        <view class="f-row"><text class="f-label">位置</text>
          <text class="f-value">{{ shelfDetail.shelf.locationText || '未设置' }}</text></view>
        <view class="f-row"><text class="f-label">归属</text>
          <text class="f-value">{{ shelfDetail.shelf.warehouseName }} · {{ shelfTypeNameOf(shelfDetail.shelf.shelfType) }}</text></view>
        <view class="f-row"><text class="f-label">容量</text>
          <text class="f-value">{{ shelfDetail.shelf.capacity ? shelfDetail.shelf.capacity + ' 件' : '不限' }}</text></view>
        <view class="f-row" v-if="shelfDetail.overCapacity">
          <text class="f-label"></text>
          <text class="over-tip">已超容量（仅提示，不影响使用）</text>
        </view>

        <view class="sheet-sub">本货架 · {{ shelfDetail.totalQuantity }} 件 · ¥{{ fmt(shelfDetail.totalCostValue) }}</view>
        <scroll-view scroll-y class="detail-items">
          <view v-if="!shelfDetail.items.length" class="empty">该货架暂无库存</view>
          <view v-for="it in shelfDetail.items" :key="it.productId" class="row">
            <view class="flex-1 min-w-0">
              <view class="row-name">{{ it.productName }}</view>
              <view class="row-sub">{{ it.unit || '个' }} · 成本 ¥{{ fmt(it.costPrice) }}</view>
            </view>
            <view class="row-right">
              <view class="qty">{{ it.quantity }} 件</view>
              <view class="price">¥{{ fmt(it.costValue) }}</view>
            </view>
          </view>
        </scroll-view>

        <view class="sheet-actions">
          <view class="btn ghost" @tap="editShelfFromDetail">编辑</view>
          <view class="btn ghost" @tap="openTransfer">移库</view>
          <view class="btn" @tap="deleteShelfFromDetail">删除货架</view>
        </view>
      </view>
    </view>

    <!-- 移库（货架之间搬运） -->
    <view class="mask" v-if="transferForm.visible" @tap="transferForm.visible = false">
      <view class="sheet" @tap.stop>
        <view class="sheet-title">移库</view>
        <view class="f-row"><text class="f-label">商品</text>
          <picker :range="transferItemNames" @change="onTransferProduct">
            <view class="f-value">{{ transferForm.itemName || '选择本货架商品' }} ▾</view>
          </picker></view>
        <view class="f-row"><text class="f-label">从</text>
          <text class="f-value">{{ shelfDetail.shelf.name }}</text></view>
        <view class="f-row"><text class="f-label">到</text>
          <picker :range="transferTargetNames" @change="onTransferTarget">
            <view class="f-value">{{ transferForm.targetName || '选择目标货架' }} ▾</view>
          </picker></view>
        <view class="f-row"><text class="f-label">数量</text>
          <input class="f-input" type="number" v-model="transferForm.quantity" placeholder="0" /></view>
        <view class="sheet-actions">
          <view class="btn ghost" @tap="transferForm.visible = false">取消</view>
          <view class="btn" @tap="saveTransfer">确认移库</view>
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
  createSaleOrder, createPurchaseOrder,
  listShelves, createShelf, updateShelf, deleteShelf, getShelfDetail,
  attachShelfImage, deleteInventoryImage, uploadFile, transferStock } from '@/api/ledger';

const WH_TYPES = [
  ['personal', '个人仓'],
  ['family', '家庭仓'],
  ['partner', '合伙仓']
];

/** 货架类型（与后端 LedgerShelf.TYPE_* 对齐） */
const SHELF_TYPES = [
  ['normal', '普通货架'],
  ['refrigerated', '冷藏/冷冻'],
  ['locked', '保险柜/上锁'],
  ['temp', '临时暂存']
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
      orderForm: { mode: 'sale', party: '', items: [] },
      // ===== 货架（存放位置）=====
      tab: 'goods',
      shelves: [],
      shelfSheet: false,
      shelfForm: {
        id: null, code: '', name: '', zone: '',
        rowNo: '', layerNo: '', positionNo: '',
        shelfType: 'normal', capacity: '', remark: ''
      },
      shelfDetail: {
        visible: false, shelf: {}, items: [], images: [],
        totalQuantity: 0, totalCostValue: 0, overCapacity: false
      },
      transferForm: { visible: false, productId: null, itemName: '', targetShelfId: null, targetName: '', quantity: '' },
      // 出入库所选货架（0/null = 未上架）
      stockShelfId: 0
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
    },
    shelfTypeNames() { return SHELF_TYPES.map(t => t[1]); },
    /** 移库可选商品（本货架上还有货的） */
    transferItemNames() { return (this.shelfDetail.items || []).map(it => it.productName); },
    /** 移库可选目标货架（排除当前货架） */
    transferTargetList() {
      const cur = this.shelfDetail.shelf && this.shelfDetail.shelf.id;
      return this.shelves.filter(s => String(s.id) !== String(cur));
    },
    transferTargetNames() {
      return this.transferTargetList.map(s => s.name + (s.code ? ' · ' + s.code : ''));
    },
    /** 出入库货架选择器（首项=未上架） */
    stockShelfNames() {
      return ['未上架'].concat(this.shelves.map(s => s.name + (s.code ? ' · ' + s.code : '')));
    },
    stockShelfLabel() {
      if (!this.stockShelfId) return '未上架';
      const s = this.shelves.find(x => String(x.id) === String(this.stockShelfId));
      return s ? s.name + (s.code ? ' · ' + s.code : '') : '未上架';
    }
  },
  onShow() {
    useThemeStore().restore();
    this.loadWarehouses();
  },
  methods: {
    fmt(v) { return Number(v || 0).toFixed(2); },
    // ===== 货架（存放位置）=====
    switchTab(t) {
      this.tab = t;
      if (t === 'shelf') this.loadShelves();
      else this.loadProducts();
    },
    shelfTypeNameOf(t) {
      const x = SHELF_TYPES.find(i => i[0] === t);
      return x ? x[1] : '普通货架';
    },
    onShelfTypeChange(e) {
      const x = SHELF_TYPES[e.detail.value];
      if (x) this.shelfForm.shelfType = x[0];
    },
    /** 出入库所选货架（首项=未上架） */
    onStockShelfChange(e) {
      const i = Number(e.detail.value);
      this.stockShelfId = i <= 0 ? 0 : this.shelves[i - 1].id;
    },
    async loadShelves() {
      if (!this.currentId) { this.shelves = []; return; }
      try {
        this.shelves = (await listShelves(this.currentId)) || [];
      } catch (e) { /* 拦截器已提示 */ }
    },
    /** 打开新增/编辑货架（传 s 即编辑） */
    openShelfSheet(s) {
      if (!this.currentId) { uni.showToast({ title: '请先新建/选择仓库', icon: 'none' }); return; }
      this.shelfForm = s ? {
        id: s.id, code: s.code || '', name: s.name || '', zone: s.zone || '',
        rowNo: s.rowNo == null ? '' : s.rowNo,
        layerNo: s.layerNo == null ? '' : s.layerNo,
        positionNo: s.positionNo == null ? '' : s.positionNo,
        shelfType: s.shelfType || 'normal',
        capacity: s.capacity == null ? '' : s.capacity,
        remark: s.remark || ''
      } : {
        id: null, code: '', name: '', zone: '',
        rowNo: '', layerNo: '', positionNo: '',
        shelfType: 'normal', capacity: '', remark: ''
      };
      this.shelfSheet = true;
    },
    async saveShelf() {
      const f = this.shelfForm;
      if (!(f.code || '').trim()) { uni.showToast({ title: '请填货架编号', icon: 'none' }); return; }
      if (!(f.name || '').trim()) { uni.showToast({ title: '请填货架名称', icon: 'none' }); return; }
      const payload = {
        warehouseId: this.currentId,
        code: f.code.trim(),
        name: f.name.trim(),
        zone: (f.zone || '').trim() || null,
        rowNo: f.rowNo === '' ? null : Number(f.rowNo),
        layerNo: f.layerNo === '' ? null : Number(f.layerNo),
        positionNo: f.positionNo === '' ? null : Number(f.positionNo),
        shelfType: f.shelfType,
        capacity: f.capacity === '' ? null : Number(f.capacity),
        remark: (f.remark || '').trim() || null
      };
      try {
        if (f.id) await updateShelf(f.id, payload);
        else await createShelf(payload);
        uni.showToast({ title: f.id ? '已保存' : '已创建', icon: 'success' });
        this.shelfSheet = false;
        await this.loadShelves();
        if (this.shelfDetail.visible && f.id) await this.openShelfDetail({ id: f.id });
      } catch (e) { /* 拦截器已提示 */ }
    },
    /** 货架详情（含图片 + 商品明细） */
    async openShelfDetail(s) {
      try {
        const d = (await getShelfDetail(s.id)) || {};
        this.shelfDetail = {
          visible: true,
          shelf: d.shelf || {},
          items: d.items || [],
          images: d.images || [],
          totalQuantity: d.totalQuantity || 0,
          totalCostValue: d.totalCostValue || 0,
          overCapacity: !!d.overCapacity
        };
      } catch (e) { /* 拦截器已提示 */ }
    },
    editShelfFromDetail() {
      const s = this.shelfDetail.shelf;
      if (!s || !s.id) return;
      this.shelfDetail.visible = false;
      this.openShelfSheet(s);
    },
    deleteShelfFromDetail() {
      const s = this.shelfDetail.shelf;
      if (!s || !s.id) return;
      uni.showModal({
        title: '删除货架',
        content: '删除「' + s.name + '」？仍有库存时会被拒绝，请先移库',
        success: async (r) => {
          if (!r.confirm) return;
          try {
            await deleteShelf(s.id);
            this.shelfDetail.visible = false;
            await this.loadShelves();
            uni.showToast({ title: '已删除', icon: 'success' });
          } catch (e) { /* 拦截器已提示 */ }
        }
      });
    },
    /** 上传货架图片（先传文件拿 URL，再挂到货架上，1:N） */
    uploadShelfImage() {
      const id = this.shelfDetail.shelf && this.shelfDetail.shelf.id;
      if (!id) return;
      uni.chooseImage({
        count: 1,
        sizeType: ['compressed'],
        success: async (res) => {
          const path = res.tempFilePaths && res.tempFilePaths[0];
          if (!path) return;
          uni.showLoading({ title: '上传中', mask: true });
          try {
            const url = await uploadFile(path, 'ledger_shelf');
            if (url) {
              await attachShelfImage(id, url);
              await this.openShelfDetail({ id });
              await this.loadShelves();
            }
            uni.hideLoading();
          } catch (e) {
            uni.hideLoading();
          }
        }
      });
    },
    removeShelfImage(img) {
      const id = this.shelfDetail.shelf && this.shelfDetail.shelf.id;
      if (!img || !img.id) return;
      uni.showModal({
        title: '删除图片',
        content: '确定删除这张图片？',
        success: async (r) => {
          if (!r.confirm) return;
          try {
            await deleteInventoryImage(img.id);
            await this.openShelfDetail({ id });
            await this.loadShelves();
          } catch (e) { /* 拦截器已提示 */ }
        }
      });
    },
    openTransfer() {
      if (!this.shelfDetail.items.length) {
        uni.showToast({ title: '该货架暂无库存，无法移库', icon: 'none' }); return;
      }
      if (!this.transferTargetList.length) {
        uni.showToast({ title: '请先再建一个货架作为目标', icon: 'none' }); return;
      }
      this.transferForm = { visible: true, productId: null, itemName: '', targetShelfId: null, targetName: '', quantity: '' };
    },
    onTransferProduct(e) {
      const it = this.shelfDetail.items[e.detail.value];
      if (!it) return;
      this.transferForm.productId = it.productId;
      this.transferForm.itemName = it.productName;
    },
    onTransferTarget(e) {
      const s = this.transferTargetList[e.detail.value];
      if (!s) return;
      this.transferForm.targetShelfId = s.id;
      this.transferForm.targetName = s.name + (s.code ? ' · ' + s.code : '');
    },
    async saveTransfer() {
      const f = this.transferForm;
      if (!f.productId) { uni.showToast({ title: '请选择商品', icon: 'none' }); return; }
      if (!f.targetShelfId) { uni.showToast({ title: '请选择目标货架', icon: 'none' }); return; }
      const qty = Number(f.quantity);
      if (!qty || qty <= 0) { uni.showToast({ title: '请输入数量', icon: 'none' }); return; }
      try {
        await transferStock({
          warehouseId: this.currentId,
          productId: f.productId,
          fromShelfId: this.shelfDetail.shelf.id,
          toShelfId: f.targetShelfId,
          quantity: qty
        });
        uni.showToast({ title: '已移库', icon: 'success' });
        f.visible = false;
        await this.openShelfDetail({ id: this.shelfDetail.shelf.id });
        await this.loadShelves();
        await this.loadProducts();
      } catch (e) { /* 拦截器已提示 */ }
    },
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
          this.loadShelves();
        } else if (!this.warehouses.length) {
          this.currentId = null;
          this.products = [];
          this.shelves = [];
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
      this.stockShelfId = 0;
      this.loadShelves();
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
          shelfId: this.stockShelfId || 0,
          quantity: qty,
          remark: this.stockRemark || undefined
        });
        uni.showToast({ title: this.stockMode === 'in' ? '已入库' : '已出库', icon: 'success' });
        this.stockTarget = null;
        this.loadProducts();
        this.loadShelves();
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
/* ===== 货架 / 商品 切换 ===== */
.seg-tabs {
  display: flex; gap: 12rpx; padding: 16rpx 24rpx 0;
}
.seg-tab {
  flex: 1; text-align: center; padding: 16rpx 0; border-radius: 12rpx;
  font-size: 28rpx; color: var(--text-sub); background: var(--card);
  border: 1rpx solid var(--border);
}
.seg-tab.on {
  color: #fff; background: var(--primary); border-color: var(--primary); font-weight: 600;
}

/* ===== 货架网格 ===== */
.shelf-grid {
  display: grid; grid-template-columns: repeat(2, 1fr); gap: 16rpx;
  padding: 8rpx 0;
}
.shelf-cell {
  border: 1rpx solid var(--border); border-radius: 14rpx; overflow: hidden;
  background: var(--card);
}
.shelf-cover {
  height: 150rpx; display: flex; align-items: center; justify-content: center;
  background: var(--bg);
}
.shelf-img { width: 100%; height: 100%; }
.shelf-code {
  font-size: 34rpx; font-weight: 700; color: var(--primary); letter-spacing: 1rpx;
}
.shelf-name {
  font-size: 27rpx; font-weight: 600; color: var(--text-main);
  padding: 10rpx 14rpx 2rpx; overflow: hidden; text-overflow: ellipsis; white-space: nowrap;
}
.shelf-pos {
  font-size: 22rpx; color: var(--text-sub); padding: 0 14rpx;
  overflow: hidden; text-overflow: ellipsis; white-space: nowrap;
}
.shelf-meta {
  font-size: 22rpx; color: var(--text-sub); padding: 4rpx 14rpx 12rpx;
}

/* ===== 货架详情 ===== */
.detail-imgs { white-space: nowrap; padding: 10rpx 0 16rpx; }
.detail-img-wrap { display: inline-block; position: relative; margin-right: 14rpx; }
.detail-img {
  width: 160rpx; height: 160rpx; border-radius: 12rpx; display: block;
}
.detail-img-cover {
  position: absolute; left: 0; bottom: 0; padding: 2rpx 10rpx;
  font-size: 20rpx; color: #fff; background: rgba(0,0,0,.55);
  border-bottom-left-radius: 12rpx; border-top-right-radius: 10rpx;
}
.detail-img-del {
  position: absolute; top: -6rpx; right: -6rpx;
  width: 34rpx; height: 34rpx; line-height: 30rpx; text-align: center;
  border-radius: 50%; background: #E85D5D; color: #fff; font-size: 24rpx;
}
.detail-img-add {
  display: inline-block; width: 160rpx; height: 160rpx; line-height: 160rpx;
  text-align: center; font-size: 44rpx; color: var(--text-sub);
  border: 1rpx dashed var(--border); border-radius: 12rpx; vertical-align: top;
}
.detail-items { max-height: 460rpx; }
.over-tip { font-size: 22rpx; color: #E8654A; }
</style>
