<template>
  <view class="page" :style="themeVars">

    <view v-if="!enabled" class="locked">
      <view class="locked-icon">📦</view>
      <view class="locked-title">仓库管理未开启</view>
      <view class="locked-btn" @tap="goSettings">去开启</view>
    </view>

    <template v-else>
      <scroll-view scroll-x class="wh-bar" v-if="warehouses.length">
        <view v-for="w in warehouses" :key="w.id" class="wh-tag"
              :class="{ active: String(w.id) === String(warehouseId) }" @tap="switchWh(w)">{{ w.name }}</view>
      </scroll-view>
      <view v-else class="empty-top" @tap="goManage">还没有仓库，去「库存管理」新建一个 ›</view>
      <view class="wh-manage" v-if="warehouseId" @tap="goManage">管理库存 / 货架 ›</view>

      <view class="mode-bar" v-if="warehouseId">
        <view class="mode-chip in" :class="{ on: mode === 'in' }" @tap="mode = 'in'">入库（录入商品）</view>
        <view class="mode-chip out" :class="{ on: mode === 'out' }" @tap="mode = 'out'">出库</view>
      </view>


      <view class="shelf-box" v-if="warehouseId">
        <view class="shelf-title">放在哪个货架</view>
        <view class="shelf-grid">
          <view v-for="s in shelfOptions" :key="s.id" class="shelf-cell"
                :class="{ on: String(shelfId) === String(s.id) }" @tap="shelfId = s.id">
            <text class="shelf-code">{{ s.code || '—' }}</text>
            <text class="shelf-name">{{ s.name }}</text>
          </view>
        </view>
      </view>

      <template v-if="mode === 'in'">
        <view class="card">
          <view class="f-row">
            <text class="f-label req">商品名称</text>
            <input class="f-input" v-model="f.name" placeholder="输入名称，自动查重"
                   @blur="checkSimilar" @confirm="checkSimilar" />
          </view>

          <view v-if="similarList.length && !reuseId" class="similar-box">
            <view class="similar-title">仓库里已有相似商品 · 直接加库存即可</view>
            <view v-for="s in similarList" :key="s.productId" class="similar-item" @tap="pickReuse(s)">
              <view class="flex-1 min-w-0">
                <view class="row-name">{{ s.name }}</view>
                <view class="row-sub">库存 {{ s.stockQuantity }} 件<text v-if="s.shelves && s.shelves.length"> · {{ s.shelves[0].shelfName }}</text></view>
              </view>
              <text class="similar-pick">选它 ›</text>
            </view>
          </view>

          <view v-if="reuseId" class="reuse-box">
            <view class="reuse-head">
              <text class="flex-1">已选：{{ reuseName }}</text>
              <text class="link" @tap="clearReuse">取消</text>
            </view>
            <view class="f-row">
              <text class="f-label">本次数量</text>
              <NumberStepper class="flex-1" v-model="f.quantity" :min="1" :unit="qtyUnit" />
            </view>
            <view class="f-row" v-if="f.packUnit && f.packSize">
              <text class="f-label">录入单位</text>
              <view class="tag-row">
                <view class="tag" :class="{ on: !f.usePack }" @tap="f.usePack = false">{{ qtyUnit }}</view>
                <view class="tag" :class="{ on: f.usePack }" @tap="f.usePack = true">{{ f.packUnit }}</view>
              </view>
            </view>
            <view v-if="f.usePack && f.packSize" class="hint-box">
              ＝ {{ qtyBase }} {{ qtyUnit }}<text v-if="unitPriceHint"> · 单价 {{ unitPriceHint }} 元/{{ qtyUnit }}</text>
            </view>
            <view class="reuse-tip">保存后在「{{ shelfLabel }}」上增加库存，不再新建商品档案</view>
          </view>

          <template v-else>
            <view class="f-row" @tap="pickCategory">
              <text class="f-label req">商品分类</text>
              <text class="f-value">{{ categoryLabel || '选择分类' }}</text>
              <text class="unit">▾</text>
            </view>
            <view class="f-row">
              <text class="f-label">单位</text>
              <view class="tag-row wrap">
                <view v-for="u in commonUnits" :key="u" class="tag" :class="{ on: f.unit === u }" @tap="f.unit = u">{{ u }}</view>
                <view class="tag add" :class="{ on: commonUnits.indexOf(f.unit) < 0 }" @tap="moreUnits">{{ commonUnits.indexOf(f.unit) < 0 ? f.unit : '更多' }}</view>
              </view>
            </view>
            <view class="f-row">
              <text class="f-label">商品图片</text>
              <view class="img-wrap">
                <image v-if="f.imageUrl" :src="f.imageUrl" class="thumb" mode="aspectFill" @tap="previewImage(f.imageUrl)" />
                <text class="link" @tap="uploadImage">{{ f.imageUrl ? '重拍' : '拍照 / 相册' }}</text>
              </view>
            </view>
            <view class="amount-area">
              <text class="amount-label req">数量</text>
              <NumberStepper class="flex-1" v-model="f.quantity" :min="1" :unit="qtyUnit" />
            </view>
            <!-- 来源：影响成本怎么算，所以放在成本上方 -->
            <view class="f-row col">
              <view class="src-head">
                <text class="f-label">来源</text>
                <text class="src-hint">{{ sourceHint }}</text>
              </view>
              <view class="tag-row wrap">
                <view v-for="s in sources" :key="s.key" class="tag"
                      :class="{ on: f.sourceType === s.key }" @tap="f.sourceType = s.key">{{ s.label }}</view>
              </view>
            </view>
            <view class="f-row" v-if="f.sourceType === 'purchase'">
              <text class="f-label">记一笔支出</text>
              <switch :checked="f.createTxn" color="#5FA77A" @change="e => f.createTxn = e.detail.value" />
            </view>
            <view class="f-row">
              <text class="f-label">成本单价</text>
              <input class="f-input" :class="{ dis: costFree }" type="digit" v-model="f.costPrice" :disabled="costFree" :placeholder="costFree ? '默认 0' : '0.00'" />
              <text class="unit">元/{{ qtyUnit }}</text>
            </view>
            <view class="f-row">
              <text class="f-label">成本总额</text>
              <input class="f-input" :class="{ dis: costFree }" type="digit" v-model="f.costTotal" :disabled="costFree" :placeholder="costFree ? '无需填写（成本为 0）' : (f.costPrice ? '≈ ' + (Number(f.costPrice) * qtyBase).toFixed(2) + '（单价 × 数量）' : '本次一共花了多少，留空则按单价×数量')" />
              <text class="unit">元</text>
            </view>
            <view class="f-row" v-if="f.createTxn">
              <text class="f-label req">支出分类</text>
              <CategoryPicker v-model="f.txnCategoryId" type="expense" />
            </view>
            <view class="f-row" v-if="f.createTxn" @tap="pickTxnAccount">
              <text class="f-label req">资金账户</text>
              <text class="f-value">{{ txnAccountLabel || '选择账户' }} ▾</text>
            </view>
            <view class="f-row">
              <text class="f-label">包装单位</text>
              <input class="f-input" v-model="f.packUnit" placeholder="如 箱（选填）" />
            </view>
            <view class="f-row" v-if="f.packUnit">
              <text class="f-label">换算</text>
              <input class="f-input" type="number" v-model="f.packSize" placeholder="1" />
              <text class="unit">个/箱</text>
            </view>
            <view class="f-row">
              <text class="f-label">建议售价</text>
              <input class="f-input" type="digit" v-model="f.sellPrice" placeholder="0.00" />
              <text class="unit">元/{{ qtyUnit }}</text>
            </view>
            <view class="f-row">
              <text class="f-label">生产日期</text>
              <picker mode="date" :value="f.productionDate" @change="onProduction">
                <text class="f-value">{{ f.productionDate || '选择' }} ▾</text>
              </picker>
            </view>
            <view class="f-row" @tap="pickShelfLife">
              <text class="f-label">保质期</text>
              <text class="f-value">{{ shelfLifeLabel }} ▾</text>
            </view>
            <view class="f-row" v-if="f.shelfLifeKey === 'custom'">
              <text class="f-label">天数</text>
              <input class="f-input" type="number" v-model="f.shelfLifeDays" placeholder="0" @blur="calcExpiry" />
              <text class="unit">天</text>
            </view>
            <view class="f-row" v-if="f.shelfLifeKey !== 'none'">
              <text class="f-label">到期日期</text>
              <view class="date-wrap">
                <picker mode="date" :value="f.expiryDate" @change="onExpiry">
                  <text class="f-value">{{ f.expiryDate || '选择' }} ▾</text>
                </picker>
                <text v-if="expiryHint" class="date-hint" :class="{ bad: expiryHint.indexOf('已过期') === 0 }">{{ expiryHint }}</text>
              </view>
            </view>
            <view class="f-row">
              <text class="f-label">包装类型</text>
              <view class="tag-row">
                <view v-for="p in packTypes" :key="p.key" class="tag"
                      :class="{ on: f.packagingType === p.key }" @tap="f.packagingType = p.key">{{ p.label }}</view>
              </view>
            </view>
            <view class="f-row col">
              <text class="f-label">售卖渠道</text>
              <view class="tag-row wrap">
                <view v-for="c in channels" :key="c.id" class="tag"
                      @longpress="removeChannel(c)"
                      :class="{ on: f.channelIds.includes(c.id) }" @tap="toggleChannel(c.id)">{{ c.name }}</view>
                <view class="tag add" @tap="addChannel">＋</view>
              </view>
            </view>
            <view class="f-row">
              <text class="f-label">标签</text>
              <input class="f-input" v-model="f.tags" placeholder="逗号分隔，如 夏季,爆款" />
            </view>
            <view class="f-row">
              <text class="f-label">描述</text>
              <input class="f-input" v-model="f.description" placeholder="选填" />
            </view>
          </template>
        </view>
      </template>

      <template v-else>
        <view class="card">
          <view class="f-row" @tap="pickOutProduct">
            <text class="f-label req">出库商品</text>
            <text class="f-value">{{ outProductLabel || '从库存里选' }} ▾</text>
          </view>
          <view class="f-row">
            <text class="f-label">出库类型</text>
            <view class="tag-row">
              <view v-for="t in outTypes" :key="t.key" class="tag"
                    :class="{ on: f.outType === t.key }" @tap="f.outType = t.key">{{ t.label }}</view>
            </view>
          </view>
          <view class="amount-area">
            <text class="amount-label req">数量</text>
            <NumberStepper class="flex-1" v-model="f.outQuantity" :min="1" :max="outStock" :unit="qtyUnit" />
          </view>

          <template v-if="f.outType === 'sale'">
            <view class="f-row">
              <text class="f-label">成交单价</text>
              <input class="f-input" type="digit" v-model="f.salePrice" placeholder="0.00" />
            </view>
            <view class="f-row col">
              <text class="f-label">卖出渠道</text>
              <view class="tag-row wrap">
                <view v-for="c in channels" :key="c.id" class="tag"
              @longpress="removeChannel(c)"
                      :class="{ on: String(f.channelId) === String(c.id) }" @tap="f.channelId = c.id">{{ c.name }}</view>
              </view>
            </view>
          </template>
          <view v-else class="hint-box">
            {{ f.outType === 'self_use'
              ? '自用：库存减少，记 ¥0，渠道记「自用」，仍计入出库统计'
              : '损耗：过期/损坏等不可抗拒损失，库存减少，记 ¥0，不计销售额' }}
          </view>

          <view class="f-row">
            <text class="f-label">备注</text>
            <input class="f-input" v-model="f.remark" placeholder="选填" />
          </view>
        </view>
      </template>

      <view class="submit-bar" v-if="warehouseId">
        <view class="submit-btn" :class="{ disabled: submitting }" @tap="submit">
          {{ submitting ? '提交中…' : (editingId ? '保存修改' : (mode === 'in' ? (reuseId ? '加库存' : '入库建档') : '确认出库')) }}
        </view>
      </view>

      <!-- 支出分类：两级树 + 图标 -->
      <view class="mask" v-if="txnCatSheet" @tap="txnCatSheet = false">
        <view class="sheet" @tap.stop>
          <view class="sheet-title">选择支出分类</view>
          <scroll-view scroll-y class="cat-tree">
            <view v-if="!txnCategories.length" class="empty" @tap="txnCatSheet = false; goCategories()">
              还没有记账分类，点这里去添加 ›
            </view>
            <block v-for="c in txnCategories" :key="c.id">
              <view class="ct-row lv1" :class="{ on: String(f.txnCategoryId) === String(c.id) }"
                    @tap="pickCatNode(c)">
                <image v-if="isImgIcon(c.icon)" :src="c.icon" class="ct-icon-img" mode="aspectFill" />
                <text v-else class="ct-icon">{{ c.icon || '📁' }}</text>
                <text class="ct-name">{{ c.name }}</text>
              </view>
              <view v-for="k in (c.children || [])" :key="k.id" class="ct-row lv2"
                    :class="{ on: String(f.txnCategoryId) === String(k.id) }"
                    @tap="pickCatNode(k)">
                <image v-if="isImgIcon(k.icon)" :src="k.icon" class="ct-icon-img" mode="aspectFill" />
                <text v-else class="ct-icon">{{ k.icon || '·' }}</text>
                <text class="ct-name">{{ k.name }}</text>
              </view>
            </block>
          </scroll-view>
          <view class="picker-cancel" @tap="txnCatSheet = false">取消</view>
        </view>
      </view>

      <view class="mask" v-if="picker.visible" @tap="picker.visible = false">
        <view class="sheet" @tap.stop>
          <view class="sheet-title">{{ picker.title }}</view>
          <scroll-view scroll-y class="picker-list">
            <view v-if="!picker.items.length" class="empty" @tap="picker.mode === 'category' && goCategories()">{{ picker.empty }}</view>
            <view v-for="it in picker.items" :key="it.id" class="picker-item"
                  :class="{ sub: it.level === 2, active: String(picker.selected) === String(it.id) }" @tap="confirmPick(it)">
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
import { useThemeStore } from '@/stores/theme';
import { useUserStore } from '@/stores/user';
import { isWarehouseEnabled, applyWarehouseTabBar } from '@/utils/warehouseSwitch';
import { getWarehouseCfg } from '@/utils/warehouseConfig';
import NumberStepper from '@/components/NumberStepper/NumberStepper.vue';
import CategoryPicker from '@/components/CategoryPicker/CategoryPicker.vue';
import {
  listWarehouses, listShelves, listProducts, listProductCategories, listSaleChannels,
  createSaleChannel, deleteSaleChannel, createProduct, updateProduct, stockIn, stockOut, getProductDetail,
  listCategories, listAssets,
  uploadFile, attachProductImage, findSimilarProducts
} from '@/api/ledger';

const PACK_TYPES = [
  { key: 'none', label: '无需' },
  { key: 'standard', label: '标准' },
  { key: 'fragile', label: '防摔' },
  { key: 'custom', label: '特殊' }
];
// 商品单位：论件、论斤都可以，不做「一斤=一件」的强行换算
// 商品单位：中文计量 + 英文/公制单位，不做任何换算（1斤≠1件）
const UNITS = [
  '件', '个', '只', '条', '张', '份', '支', '台', '套', '双',
  '斤', '公斤', '克', '吨',
  '毫升', '升',
  '米', '厘米', '平方米', '立方米',
  '箱', '瓶', '包', '袋', '盒', '桶', '打'
];

// 保质期预设：统一折算成天数存库（30/365 天口径）
const SHELF_LIFE_OPTIONS = [
  { key: 'none', label: '无保质期（长期有效）', days: 0 },
  { key: '7d', label: '7 天', days: 7 },
  { key: '30d', label: '1 个月', days: 30 },
  { key: '90d', label: '3 个月', days: 90 },
  { key: '180d', label: '6 个月', days: 180 },
  { key: '365d', label: '12 个月（1 年）', days: 365 },
  { key: '730d', label: '24 个月（2 年）', days: 730 },
  { key: '1095d', label: '36 个月（3 年）', days: 1095 },
  { key: 'custom', label: '自定义天数…', days: null }
];

// 商品来源：固定枚举，决定成本怎么算（用户不可自定义）
const SOURCES = [
  { key: 'purchase', label: '采购' },
  { key: 'self_made', label: '自产' },
  { key: 'used_resale', label: '自用转卖' },
  { key: 'gift', label: '赠品' }
];

const OUT_TYPES = [
  { key: 'sale', label: '卖出' },
  { key: 'self_use', label: '自己用' },
  { key: 'loss', label: '损耗' }
];

export default {
  components: { NumberStepper, CategoryPicker },
  data() {
    return {
      enabled: true,
      mode: 'in',
      cfg: getWarehouseCfg(),
      warehouses: [], shelves: [], products: [], categories: [], channels: [],
      warehouseId: null,
      shelfId: 0,
      similarList: [],
      reuseId: null,
      reuseName: '',
      outProductId: null,
      editingId: null,
      pendingEdit: null,
      pendingOut: null,
      submitting: false,
      sources: SOURCES,
      txnCategories: [], txnAccounts: [],
      txnCatSheet: false,
      packTypes: PACK_TYPES,
      outTypes: OUT_TYPES,
      f: this.blankForm(),
      picker: { visible: false, mode: '', title: '', items: [], selected: null, empty: '' }
    };
  },
  computed: {
    /** 非采购来源：成本默认 0（钱已在别处记过），成本字段置灰 */
    costFree() {
      return this.f.sourceType !== 'purchase';
    },
    /** 当前来源的成本计算说明（右侧提示，随选择变化） */
    sourceHint() {
      const m = {
        purchase: '实付总额 ÷ 数量',
        self_made: '原料已记账 · 成本默认 0',
        used_resale: '当年已记账 · 成本默认 0',
        gift: '未花钱 · 成本默认 0'
      };
      return m[this.f.sourceType] || '';
    },
    /** 已选支出分类名 */
    txnCategoryLabel() {
      for (const c of this.txnCategories) {
        if (String(c.id) === String(this.f.txnCategoryId)) return c.name;
        const k = (c.children || []).find(x => String(x.id) === String(this.f.txnCategoryId));
        if (k) return c.name + ' / ' + k.name;
      }
      return '';
    },
    /** 已选账户名 */
    txnAccountLabel() {
      const a = this.txnAccounts.find(x => String(x.id) === String(this.f.txnAccountId));
      return a ? a.name : '';
    },
    /** 提交用的基础单位数量：选了包装单位就 × 换算系数 */
    qtyBase() {
      const q = Number(this.f.quantity) || 0;
      const size = Number(this.f.packSize) || 0;
      return this.f.usePack && size > 0 ? q * size : q;
    },
    /** 换算后的单价（元/基础单位） */
    unitPriceHint() {
      const base = this.qtyBase;
      const total = Number(this.f.costTotal) || 0;
      if (!base || !total) return '';
      return (total / base).toFixed(2);
    },
    /** 出库上限：所选商品的当前库存 */
    outStock() {
      const p = this.products.find(x => String(x.id) === String(this.outProductId));
      return p ? Number(p.stockQuantity || 0) : null;
    },
    themeVars() { return useThemeStore().themeVars; },
    units() { return UNITS; },
    /** 宫格只铺常用几个，其余走「更多」（27 个全铺太占位） */
    commonUnits() { return ['件', '个', '条', '斤', '公斤', '克', '升', '箱', '瓶', '包']; },
    /** 数量单位：入库用表单选的，出库用所选商品的单位 */
    qtyUnit() {
      if (this.mode === 'out') {
        const p = this.products.find(x => String(x.id) === String(this.outProductId));
        return (p && p.unit) || '件';
      }
      return this.f.unit || '件';
    },
    shelfLifeOptions() { return SHELF_LIFE_OPTIONS; },
    shelfLifeNames() { return SHELF_LIFE_OPTIONS.map(o => o.label); },
    shelfLifeLabel() {
      const o = SHELF_LIFE_OPTIONS.find(x => x.key === this.f.shelfLifeKey);
      return o ? o.label : '无保质期（长期有效）';
    },
    /** 到期提示：临期/已过期 */
    expiryHint() {
      if (!this.f.expiryDate) return '';
      const d = new Date(String(this.f.expiryDate).replace(/-/g, '/')).getTime();
      if (isNaN(d)) return '';
      const days = Math.ceil((d - Date.now()) / 86400000);
      if (days < 0) return '已过期 ' + Math.abs(days) + ' 天';
      if (days <= 30) return '还有 ' + days + ' 天到期（临期）';
      return '还有 ' + days + ' 天到期';
    },
    shelfOptions() {
      // 只列真实货架：不再提供「未上架」这种模糊选项
      return this.shelves.map(s => ({ id: s.id, code: s.code, name: s.name }));
    },
    shelfLabel() {
      const s = this.shelfOptions.find(x => String(x.id) === String(this.shelfId));
      return s ? s.name : '请选择货架';
    },
    categoryLabel() {
      for (const p of this.categories) {
        if (String(p.id) === String(this.f.categoryId)) return p.name;
        const k = (p.children || []).find(x => String(x.id) === String(this.f.categoryId));
        if (k) return p.name + ' / ' + k.name;
      }
      return '';
    },
    /** 分类选项（树形：一级 + 缩进的二级） */
    categoryPickerItems() {
      const out = [];
      for (const c of this.categories) {
        out.push({ id: c.id, name: c.name, level: 1 });
        for (const k of (c.children || [])) out.push({ id: k.id, name: k.name, level: 2 });
      }
      return out;
    },
    outProductLabel() {
      const p = this.products.find(x => String(x.id) === String(this.outProductId));
      return p ? p.name + '（库存 ' + (p.stockQuantity || 0) + '）' : '';
    }
  },
  onLoad(options) {
    this.pendingEdit = (options && options.edit) || null;
    this.pendingOut = (options && options.out) || null;
    if (this.pendingOut) this.mode = 'out';
  },
  onShow() {
    // 从仓库页 switchTab 过来：读一次性交接参数（tab 页不能带 query）
    const handoff = uni.getStorageSync('moyun_inout_handoff');
    if (handoff && (handoff.edit || handoff.out || handoff.create)) {
      uni.removeStorageSync('moyun_inout_handoff');
      if (handoff.warehouseId) { this.warehouseId = handoff.warehouseId; }
      if (handoff.out) { this.mode = 'out'; this.pendingOut = handoff.out; }
      if (handoff.edit) { this.pendingEdit = handoff.edit; }
      if (handoff.create) { this.mode = 'in'; }
    }
    this.cfg = getWarehouseCfg();
    useThemeStore().restore();
    this.enabled = isWarehouseEnabled();
    applyWarehouseTabBar();
    if (!this.enabled || !useUserStore().isLoggedIn) return;
    this.loadAll();
  },
  methods: {
    blankForm() {
      return {
        name: '', categoryId: null, imageUrl: '', unit: '件', quantity: '',
        costPrice: '', sellPrice: '',
        sourceType: 'purchase', sourceParty: '', costTotal: '', extraCost: '',
        packUnit: '', packSize: '', usePack: false,
        createTxn: false, txnCategoryId: null, txnAccountId: null,
        productionDate: '', shelfLifeDays: '', shelfLifeKey: 'none', expiryDate: '',
        packagingType: 'none', tags: '', description: '', channelIds: [],
        outType: 'sale', outQuantity: '', salePrice: '', channelId: null, remark: ''
      };
    },
    goSettings() { uni.navigateTo({ url: '/pages/mine/warehouse-settings/index' }); },
    goManage() {
      // 带上当前仓库和货架，过去后默认选中
      const q = [];
      if (this.warehouseId) q.push('warehouseId=' + this.warehouseId);
      if (this.shelfId) q.push('shelfId=' + this.shelfId);
      uni.navigateTo({ url: '/pages/warehouse/index' + (q.length ? '?' + q.join('&') : '') });
    },
    async loadAll() {
      try {
        this.warehouses = (await listWarehouses()) || [];
        if (this.warehouses.length && !this.warehouseId) this.warehouseId = this.warehouses[0].id;
      } catch (e) { /* 忽略 */ }
      try { this.categories = (await listProductCategories()) || []; } catch (e) { /* 忽略 */ }
      try { this.channels = (await listSaleChannels()) || []; } catch (e) { /* 忽略 */ }
      await this.loadWhData();
      this.applyPending();
    },
    /** 带参数进来时：编辑商品 / 指定出库商品 */
    applyPending() {
      if (this.pendingEdit) {
        const p = this.products.find(x => String(x.id) === String(this.pendingEdit));
        if (p) this.fillFromProduct(p);
        this.pendingEdit = null;
      }
      if (this.pendingOut) {
        this.mode = 'out';
        this.outProductId = this.pendingOut;
        this.pendingOut = null;
      }
    },
    /** 把商品数据填进表单（编辑模式） */
    fillFromProduct(p) {
      this.mode = 'in';
      this.editingId = p.id;
      this.f = Object.assign(this.blankForm(), {
        name: p.name || '',
        categoryId: p.categoryId || null,
        unit: p.unit || '件',
        costPrice: p.costPrice == null ? '' : String(p.costPrice),
        sellPrice: p.sellPrice == null ? '' : String(p.sellPrice),
        productionDate: p.productionDate || '',
        shelfLifeDays: p.shelfLifeDays == null ? '' : String(p.shelfLifeDays),
        shelfLifeKey: p.shelfLifeDays ? 'custom' : 'none',
        expiryDate: p.expiryDate || '',
        packagingType: p.packagingType || 'none',
        tags: p.tags || '',
        description: p.description || ''
      });
    },
    /** 懒加载记账分类与资金账户（只在勾选「记一笔支出」时用） */
    async loadTxnOptions() {
      if (this.txnCategories.length && this.txnAccounts.length) return;
      try {
        // 该接口返回 { records: [...] }，不是裸数组
        const res = await listCategories('expense');
        this.txnCategories = (res && res.records) || [];
      } catch (e) { this.txnCategories = []; }
      try {
        const list = (await listAssets()) || [];
        this.txnAccounts = Array.isArray(list) ? list : (list.records || []);
      } catch (e) { /* 忽略 */ }
    },
    async pickTxnCategory() {
      await this.loadTxnOptions();
      this.txnCatSheet = true;
    },
    /** 选中一级或二级分类 */
    pickCatNode(node) {
      this.f.txnCategoryId = node.id;
      this.txnCatSheet = false;
    },
    /** 图标是图片 URL 还是 emoji */
    isImgIcon(icon) {
      return !!icon && (icon.indexOf('http') === 0 || icon.indexOf('/') === 0 || icon.indexOf('data:') === 0);
    },
    async pickTxnAccount() {
      await this.loadTxnOptions();
      this.openPicker('txnAccount', '选择资金账户',
        this.txnAccounts.map(a => ({ id: a.id, name: a.name, sub: a.balance === undefined ? '' : '余额 ' + a.balance })),
        this.f.txnAccountId, '还没有资金账户');
    },

    async loadWhData() {
      if (!this.warehouseId) return;
      try { this.shelves = (await listShelves(this.warehouseId)) || []; } catch (e) { /* 忽略 */ }
      // 自动落在第一个货架；一个都没有时不预设，提交会提示先建货架
      if (this.shelves.length && !this.shelves.some(x => String(x.id) === String(this.shelfId))) {
        this.shelfId = this.shelves[0].id;
      }
      try {
        const r = (await listProducts(this.warehouseId, { pageSize: 100 })) || {};
        this.products = r.records || [];
      } catch (e) { /* 忽略 */ }
    },
    async switchWh(w) {
      if (String(w.id) === String(this.warehouseId)) return;
      this.warehouseId = w.id;
      this.shelfId = 0;
      this.clearReuse();
      this.outProductId = null;
      await this.loadWhData();
    },
    async checkSimilar() {
      const name = (this.f.name || '').trim();
      if (name.length < 2 || !this.warehouseId) { this.similarList = []; return; }
      try {
        this.similarList = (await findSimilarProducts(this.warehouseId, name, null)) || [];
      } catch (e) { this.similarList = []; }
    },
    pickReuse(s) {
      this.reuseId = s.productId;
      this.reuseName = s.name;
      this.f.quantity = '';
      this.similarList = [];
    },
    clearReuse() {
      this.reuseId = null;
      this.reuseName = '';
      this.similarList = [];
    },
    /** 按仓库设置里的「默认出入库原因」预选出库类型 */
    applyDefaultReason() {
      const r = (this.cfg && this.cfg.defaultReason) || '';
      if (['sale', 'self_use', 'loss', 'gift', 'other'].includes(r)) {
        this.f.outType = r;
      }
    },
    goCategories() { uni.navigateTo({ url: '/pages/mine/product-categories/index' }); },
    pickCategory() {
      this.openPicker('category', '选择商品分类', this.categoryPickerItems, this.f.categoryId,
        '还没有商品分类，点这里去添加 ›');
    },
    toggleChannel(id) {
      const i = this.f.channelIds.indexOf(id);
      if (i >= 0) this.f.channelIds.splice(i, 1);
      else this.f.channelIds.push(id);
    },
    addChannel() {
      uni.showModal({
        title: '新增售卖渠道', editable: true, placeholderText: '如 快手',
        success: async (r) => {
          if (!r.confirm) return;
          const name = (r.content || '').trim();
          if (!name) return;
          try {
            await createSaleChannel({ name });
            this.channels = (await listSaleChannels()) || [];
          } catch (e) { /* 忽略 */ }
        }
      });
    },
    /** 长按渠道：删除自建渠道（系统预设不可删，后端也会拦） */
    removeChannel(c) {
      if (!c) return;
      if (c.isSystem === 1 || c.isSystem === true) {
        uni.showToast({ title: '系统预设渠道不可删除', icon: 'none' });
        return;
      }
      uni.showModal({
        title: '删除渠道',
        content: '删除「' + c.name + '」？已用该渠道的历史流水不受影响',
        confirmColor: '#E8654A',
        success: async (r) => {
          if (!r.confirm) return;
          try {
            await deleteSaleChannel(c.id);
            this.channels = (await listSaleChannels()) || [];
            const idx = this.f.channelIds.indexOf(c.id);
            if (idx >= 0) this.f.channelIds.splice(idx, 1);
            if (String(this.f.channelId) === String(c.id)) this.f.channelId = null;
            uni.showToast({ title: '已删除', icon: 'success' });
          } catch (e) { /* 拦截器已提示 */ }
        }
      });
    },
    /** 保质期预设：选完自动折成天数并算到期日 */
    pickShelfLife() {
      uni.showActionSheet({
        itemList: this.shelfLifeNames,
        success: (r) => {
          const o = SHELF_LIFE_OPTIONS[r.tapIndex];
          if (!o) return;
          this.f.shelfLifeKey = o.key;
          if (o.days !== null) {
            this.f.shelfLifeDays = o.days > 0 ? o.days : '';
            this.calcExpiry();
          } else {
            this.f.shelfLifeDays = '';
          }
        }
      });
    },
    onProduction(e) { this.f.productionDate = e.detail.value; this.calcExpiry(); },
    onExpiry(e) { this.f.expiryDate = e.detail.value; },
    calcExpiry() {
      const d = Number(this.f.shelfLifeDays);
      if (!this.f.productionDate || !d) return;
      const t = new Date(String(this.f.productionDate).replace(/-/g, '/'));
      t.setDate(t.getDate() + d);
      const pad = (n) => String(n).padStart(2, '0');
      this.f.expiryDate = t.getFullYear() + '-' + pad(t.getMonth() + 1) + '-' + pad(t.getDate());
    },
    /** 完整单位列表（含英文/公制） */
    moreUnits() {
      uni.showActionSheet({
        itemList: this.units,
        success: (r) => { this.f.unit = this.units[r.tapIndex] || this.f.unit; }
      });
    },
    previewImage(url) {
      if (url) uni.previewImage({ urls: [url] });
    },
    uploadImage() {
      uni.chooseImage({
        count: 1, sizeType: ['compressed'],
        success: async (res) => {
          const path = res.tempFilePaths && res.tempFilePaths[0];
          if (!path) return;
          uni.showLoading({ title: '上传中', mask: true });
          try { this.f.imageUrl = await uploadFile(path, 'ledger_product'); uni.hideLoading(); }
          catch (e) { uni.hideLoading(); }
        }
      });
    },
    /**
     * 选了出库商品后，自动把货架切到「该商品库存最多」的那个。
     * 否则用户选了 A 货架、商品却在 B 货架 → 后端报「该货架暂无此商品库存」。
     */
    async autoPickShelf(productId) {
      try {
        const d = await getProductDetail(productId);
        const list = (d && d.shelves) || [];
        if (!list.length) return;
        const top = list.slice().sort((a, b) => (b.quantity || 0) - (a.quantity || 0))[0];
        if (top && top.shelfId) this.shelfId = top.shelfId;
      } catch (e) { /* 忽略：保留当前货架 */ }
    },
    pickOutProduct() {
      const list = this.products.filter(p => (p.stockQuantity || 0) > 0);
      this.openPicker('outProduct', '从库存里选商品',
        list.map(p => ({ id: p.id, name: p.name, sub: '库存 ' + p.stockQuantity })),
        this.outProductId, '该仓库没有可出库的库存');
    },
    openPicker(mode, title, items, selected, empty) {
      this.picker = { visible: true, mode, title, items, selected, empty: empty || '暂无数据' };
    },
    confirmPick(it) {
      const m = this.picker.mode;
      this.picker.visible = false;
      if (m === 'category') this.f.categoryId = it.id;
      else if (m === 'txnCategory') this.f.txnCategoryId = it.id;
      else if (m === 'txnAccount') this.f.txnAccountId = it.id;
      else if (m === 'outProduct') { this.outProductId = it.id; this.autoPickShelf(it.id); }
    },
    async submit() {
      if (this.submitting) return;
      if (!this.warehouseId) { uni.showToast({ title: '请选择仓库', icon: 'none' }); return; }
      if (!this.shelfId) { uni.showToast({ title: '请先在「库存管理」建一个货架', icon: 'none' }); return; }
      this.submitting = true;
      try {
        if (this.mode === 'in') await this.doStockIn();
        else await this.doStockOut();
      } catch (e) { /* 拦截器已提示 */ }
      this.submitting = false;
    },
    async doStockIn() {
      const qty = Number(this.f.quantity);
      if (!qty || qty <= 0) { uni.showToast({ title: '请输入数量', icon: 'none' }); return; }

      if (this.reuseId) {
        await stockIn({
          warehouseId: this.warehouseId, productId: this.reuseId,
          shelfId: this.shelfId || 0, reason: 'purchase', quantity: this.qtyBase
        });
        uni.showToast({ title: '已加库存', icon: 'success' });
        this.clearReuse();
        this.f = this.blankForm();
        await this.loadWhData();
        return;
      }

      if (!(this.f.name || '').trim()) { uni.showToast({ title: '请填商品名称', icon: 'none' }); return; }
      if (!this.f.categoryId) { uni.showToast({ title: '请选择商品分类', icon: 'none' }); return; }
      if (!Number(this.f.quantity) || Number(this.f.quantity) <= 0) { uni.showToast({ title: '请填数量', icon: 'none' }); return; }
      // 勾选「记一笔支出」时必须选分类和账户，否则后端会生成一条无分类流水
      if (this.f.createTxn) {
        if (!this.f.txnCategoryId) { uni.showToast({ title: '请选择支出分类', icon: 'none' }); return; }
        if (!this.f.txnAccountId) { uni.showToast({ title: '请选择资金账户', icon: 'none' }); return; }
      }
      // 编辑模式：只改商品属性，不产生入库流水
      if (this.editingId) {
        await updateProduct(this.editingId, {
          name: this.f.name.trim(),
          unit: this.f.unit || '件',
          categoryId: this.f.categoryId || null,
          costPrice: this.f.costPrice ? Number(this.f.costPrice) : null,
          sellPrice: this.f.sellPrice ? Number(this.f.sellPrice) : null,
          productionDate: this.f.productionDate || null,
          shelfLifeDays: this.f.shelfLifeDays ? Number(this.f.shelfLifeDays) : null,
          expiryDate: this.f.expiryDate || null,
          packagingType: this.f.packagingType || 'none',
          tags: this.f.tags || null,
          description: this.f.description || null
        });
        if (this.f.imageUrl) {
          try { await attachProductImage(this.editingId, this.f.imageUrl, 'product'); } catch (e) { /* 忽略 */ }
        }
        uni.showToast({ title: '已保存', icon: 'success' });
        this.editingId = null;
        this.f = this.blankForm();
        await this.loadWhData();
        return;
      }
      const pid = await createProduct({
        warehouseId: this.warehouseId,
        name: this.f.name.trim(),
        unit: this.f.unit || '件',
        categoryId: this.f.categoryId || null,
        costPrice: this.f.costPrice ? Number(this.f.costPrice) : null,
        sellPrice: this.f.sellPrice ? Number(this.f.sellPrice) : null,
        productionDate: this.f.productionDate || null,
        shelfLifeDays: this.f.shelfLifeDays ? Number(this.f.shelfLifeDays) : null,
        expiryDate: this.f.expiryDate || null,
        packagingType: this.f.packagingType || 'none',
        tags: this.f.tags || null,
        description: this.f.description || null
      });
      if (!pid) { uni.showToast({ title: '建档失败', icon: 'none' }); return; }
      if (this.f.imageUrl) {
        try { await attachProductImage(pid, this.f.imageUrl, 'product'); } catch (e) { /* 忽略 */ }
      }
      await stockIn({
        warehouseId: this.warehouseId, productId: pid,
        shelfId: this.shelfId || 0, reason: 'purchase', quantity: this.qtyBase,
        costTotal: this.f.costTotal !== '' ? Number(this.f.costTotal) : (this.f.costPrice ? Number(this.f.costPrice) * this.qtyBase : null),
        createTxn: this.f.createTxn === true,
        txnCategoryId: this.f.txnCategoryId || undefined,
        txnAccountId: this.f.txnAccountId || undefined
      });
      uni.showToast({ title: '已入库建档', icon: 'success' });
      this.f = this.blankForm();
      await this.loadWhData();
    },
    async doStockOut() {
      if (!this.outProductId) { uni.showToast({ title: '请选择出库商品', icon: 'none' }); return; }
      const qty = Number(this.f.outQuantity);
      if (!qty || qty <= 0) { uni.showToast({ title: '请输入数量', icon: 'none' }); return; }
      const isSale = this.f.outType === 'sale';
      await stockOut({
        warehouseId: this.warehouseId,
        productId: this.outProductId,
        shelfId: this.shelfId || 0,
        reason: this.f.outType,
        quantity: qty,
        salePrice: isSale && this.f.salePrice !== '' ? Number(this.f.salePrice) : undefined,
        channelId: isSale ? (this.f.channelId || undefined) : undefined,
        remark: this.f.remark || undefined
      });
      uni.showToast({ title: '已出库', icon: 'success' });
      this.f.outQuantity = '';
      this.f.salePrice = '';
      this.f.remark = '';
      this.outProductId = null;
      await this.loadWhData();
    }
  }
};
</script>

<style scoped>
.page { min-height: 100vh; padding-top: 24rpx; padding-bottom: 120rpx; }
.locked { padding: 120rpx 40rpx; text-align: center; }
.locked-icon { font-size: 80rpx; }
.locked-title { font-size: 34rpx; font-weight: 700; color: var(--text-main); margin-top: 20rpx; }
.locked-btn { display: inline-block; margin-top: 32rpx; padding: 20rpx 60rpx; background: var(--primary); color: #fff; border-radius: 999rpx; font-size: 28rpx; }

.wh-bar { white-space: nowrap; padding: 8rpx 24rpx 0; text-align: center; }
.wh-tag { display: inline-block; padding: 12rpx 26rpx; margin-right: 12rpx; border-radius: 999rpx; font-size: 26rpx; color: var(--text-sub); background: var(--card); border: 1rpx solid var(--border); }
.wh-tag.active { color: #fff; background: var(--primary); border-color: var(--primary); }
.empty-top { padding: 40rpx 24rpx; font-size: 26rpx; color: var(--text-sub); }

.mode-bar { display: flex; gap: 10rpx; padding: 14rpx 20rpx 0; }
.mode-chip { flex: 1; text-align: center; padding: 14rpx 0; border-radius: 10rpx; font-size: 26rpx; color: var(--text-sub); background: var(--card); border: 1rpx solid var(--border); }
.mode-chip.on.in { color: #fff; background: #0EA37A; border-color: #0EA37A; font-weight: 600; }
.mode-chip.on.out { color: #fff; background: #E8654A; border-color: #E8654A; font-weight: 600; }

.shelf-box { margin: 10rpx 20rpx 0; padding: 8rpx 14rpx; background: var(--card); border: 1rpx solid var(--border); border-radius: 10rpx; }
.shelf-title { font-size: 22rpx; color: var(--text-sub); padding-bottom: 6rpx; }
.shelf-grid { display: flex; flex-wrap: wrap; gap: 8rpx; }
.shelf-cell { display: inline-flex; align-items: center; gap: 6rpx; padding: 6rpx 14rpx; border-radius: 8rpx; border: 1rpx solid var(--border); background: var(--bg); }
.shelf-cell.on { border-color: var(--primary); background: rgba(95,167,122,.12); }
.shelf-code { font-size: 22rpx; font-weight: 700; color: var(--primary); }
.shelf-name { font-size: 22rpx; color: var(--text-sub); }
.card { margin: 12rpx 20rpx 0; padding: 4rpx 18rpx; background: var(--card); border: 1rpx solid var(--border); border-radius: 12rpx; }
.f-row { display: flex; align-items: center; padding: 16rpx 0; border-bottom: 1rpx solid var(--border); }
.f-row:last-child { border-bottom: none; }
.f-row.col { flex-direction: column; align-items: flex-start; gap: 10rpx; }
.f-label { font-size: 26rpx; color: var(--text-sub); min-width: 130rpx; flex: 0 0 auto; white-space: nowrap; padding-right: 8rpx; }
.f-input { flex: 1; text-align: right; font-size: 26rpx; color: var(--text-main); }
.f-value { flex: 1; text-align: right; font-size: 26rpx; color: var(--text-main); }
.img-wrap { flex: 1; display: flex; align-items: center; justify-content: flex-end; gap: 12rpx; }
.thumb { width: 90rpx; height: 90rpx; border-radius: 8rpx; }

.tag-row { display: flex; gap: 8rpx; flex: 1; justify-content: flex-end; flex-wrap: wrap; }
.tag-row.wrap { justify-content: flex-start; }
.tag { padding: 5rpx 12rpx; border-radius: 6rpx; font-size: 22rpx; line-height: 1.5; color: var(--text-sub); background: var(--bg); border: 1rpx solid var(--border); }
.tag.on { color: #fff; background: var(--primary); border-color: var(--primary); }
.tag.add { color: var(--primary); border-style: dashed; }

.similar-box { margin: 12rpx 0; padding: 14rpx; border-radius: 12rpx; background: rgba(95,167,122,.08); border: 1rpx solid rgba(95,167,122,.3); }
.similar-title { font-size: 23rpx; color: var(--primary); padding-bottom: 8rpx; }
.similar-item { display: flex; align-items: center; padding: 12rpx 0; border-top: 1rpx solid var(--border); }
.row-name { font-size: 26rpx; color: var(--text-main); }
.row-sub { font-size: 22rpx; color: var(--text-sub); margin-top: 2rpx; }
.similar-pick { font-size: 24rpx; color: var(--primary); }
.reuse-box { padding: 12rpx 0; }
.reuse-head { display: flex; align-items: center; font-size: 26rpx; color: var(--text-main); padding: 10rpx 0; }
.reuse-tip { font-size: 22rpx; color: var(--text-sub); padding: 10rpx 0; }
.hint-box { margin: 12rpx 0; padding: 16rpx; border-radius: 10rpx; background: var(--bg); font-size: 23rpx; color: var(--text-sub); line-height: 1.6; }

.submit-bar { margin: 18rpx 20rpx 0; padding-bottom: 36rpx; }
.submit-btn { text-align: center; padding: 24rpx 0; border-radius: 12rpx; background: var(--primary); color: #fff; font-size: 28rpx; font-weight: 600; }
.submit-btn.disabled { opacity: .6; }

.mask { position: fixed; inset: 0; background: rgba(0,0,0,.45); z-index: 999; display: flex; align-items: flex-end; }
.sheet { width: 100%; max-height: 74vh; background: var(--card); border-radius: 24rpx 24rpx 0 0; padding: 24rpx 24rpx 40rpx; }
.sheet-title { font-size: 32rpx; font-weight: 700; text-align: center; color: var(--text-main); padding-bottom: 16rpx; }
.picker-list { max-height: 54vh; }
.picker-item { display: flex; align-items: center; justify-content: space-between; padding: 24rpx 8rpx; border-bottom: 1rpx solid var(--border); font-size: 28rpx; }
.picker-item.active { color: var(--primary); font-weight: 600; }
.picker-sub { font-size: 22rpx; color: var(--text-sub); }
.picker-cancel { text-align: center; padding: 24rpx 0 8rpx; font-size: 28rpx; color: var(--text-sub); }
.empty { text-align: center; padding: 60rpx 0; font-size: 26rpx; color: var(--text-sub); }
.link { font-size: 26rpx; color: var(--primary); }
.flex-1 { flex: 1; }
.min-w-0 { min-width: 0; }
.amount-area {
  display: flex; align-items: center; gap: 16rpx;
  margin: 16rpx 24rpx 0; padding: 26rpx 28rpx;
  background: var(--card); border: 1rpx solid var(--border); border-radius: 16rpx;
}
.amount-label { font-size: 24rpx; color: var(--text-sub); }
.amount-input { flex: 1; text-align: right; font-size: 40rpx; font-weight: 700; color: var(--text-main); }
.unit { font-size: 22rpx; color: var(--text-sub); margin-left: 6rpx; }
.amount-unit { font-size: 24rpx; color: var(--text-sub); }
.date-wrap { flex: 1; display: flex; flex-direction: column; align-items: flex-end; }
.date-hint { font-size: 22rpx; color: var(--text-sub); }
.date-hint.bad { color: #E8654A; }
.picker-item.sub { padding-left: 46rpx; font-size: 24rpx; color: var(--text-sub); }
/* ===== 排版统一：标题 / 单位 / 关键数字 ===== */
/* 卡片与弹层标题 */
.card-title, .sheet-title { font-size: var(--fs-title); font-weight: 700; color: var(--text-main); }
/* 条目标题 */
.row-name { font-size: var(--fs-subtitle); font-weight: 600; color: var(--text-main); }
/* 单位与标签：比正文小一档、颜色降级 */
.unit, .amount-unit, .cfg-unit, .tree-num, .prod-row .row-sub, .row-sub2 { font-size: var(--fs-unit); color: var(--text-sub); }
/* 关键数字：库存数量、概览数字 */
.qty { font-size: var(--fs-h2); font-weight: 700; }
.ov-num { font-size: var(--fs-title); font-weight: 700; }
/* 次要说明 */
.row-sub, .hint, .ov-label { font-size: var(--fs-sub); color: var(--text-sub); }
.wh-manage { font-size: 22rpx; color: var(--primary); padding: 6rpx 20rpx 0; text-align: right; }
/* 来源行：左侧标签 + 右侧计算方式提示 */
.src-head { display: flex; align-items: center; justify-content: space-between; width: 100%; }
.src-hint { font-size: 21rpx; color: var(--primary); }
/* 非采购来源时成本字段置灰 */
.f-input.dis { color: var(--text-muted); opacity: .6; }

/* 支出分类树（两级 + 图标） */
.cat-tree { max-height: 56vh; }
.ct-row { display: flex; align-items: center; gap: 12rpx; padding: 18rpx 12rpx; border-bottom: 1rpx solid var(--border); }
.ct-row.lv2 { padding-left: 52rpx; }
.ct-row.on { background: rgba(95,167,122,.10); }
.ct-row.on .ct-name { color: var(--primary); font-weight: 600; }
.ct-icon { width: 44rpx; text-align: center; font-size: 30rpx; flex: 0 0 auto; }
.ct-icon-img { width: 44rpx; height: 44rpx; border-radius: 8rpx; flex: 0 0 auto; }
.ct-name { font-size: 26rpx; color: var(--text-main); }
/* 必填星号 */
.req-star { color: #E8654A; font-size: 24rpx; margin-left: 4rpx; }
.f-row .f-label.req::after { content: ' *'; color: #E8654A; }
.amount-label.req::after { content: ' *'; color: #E8654A; }
</style>
