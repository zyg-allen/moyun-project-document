<template>
  <view class="page" :style="themeVars">
    <NavBar title="仓库报表" />

    <!-- 仓库切换 -->
    <scroll-view scroll-x class="wh-bar" v-if="warehouses.length">
      <view v-for="w in warehouses" :key="w.id" class="wh-tag"
            :class="{ active: String(w.id) === String(warehouseId) }" @tap="switchWh(w)">{{ w.name }}</view>
    </scroll-view>

    <!-- ① 库存概览 -->
    <view class="hero">
      <view class="hero-label">库存总价值</view>
      <view class="hero-value">¥{{ fmt(ov.stockValue) }}</view>
      <view class="hero-sub">{{ ov.totalQuantity || 0 }} 件 · {{ ov.productCount || 0 }} 个商品 · {{ ov.shelfCount || 0 }} 个货架</view>
    </view>

    <!-- ② 本月出入库 -->
    <view class="card">
      <view class="card-title"><text class="flex-1">本月出入库</text></view>
      <view class="kpi-row">
        <view class="kpi">
          <view class="kpi-num in">+{{ ov.monthInQuantity || 0 }}</view>
          <view class="kpi-label">入库</view>
        </view>
        <view class="kpi">
          <view class="kpi-num out">-{{ ov.monthOutQuantity || 0 }}</view>
          <view class="kpi-label">出库</view>
        </view>
        <view class="kpi">
          <view class="kpi-num">{{ ov.monthSaleQuantity || 0 }}</view>
          <view class="kpi-label">销售件数</view>
        </view>
      </view>
      <view class="kpi-row">
        <view class="kpi">
          <view class="kpi-num">¥{{ fmt(ov.monthSaleAmount) }}</view>
          <view class="kpi-label">销售额</view>
        </view>
        <view class="kpi">
          <view class="kpi-num">¥{{ fmt(ov.monthSaleCost) }}</view>
          <view class="kpi-label">销售成本</view>
        </view>
        <view class="kpi">
          <view class="kpi-num" :class="{ in: (ov.monthGrossProfit || 0) >= 0, out: (ov.monthGrossProfit || 0) < 0 }">
            ¥{{ fmt(ov.monthGrossProfit) }}
          </view>
          <view class="kpi-label">毛利</view>
        </view>
      </view>
      <view class="kpi-row">
        <view class="kpi">
          <view class="kpi-num">{{ ov.zeroStockCount || 0 }}</view>
          <view class="kpi-label">零库存商品</view>
        </view>
      </view>
    </view>

    <!-- ②.5 成员业绩（协作仓库：谁卖了多少） -->
    <view class="card" v-if="memberSales.length">
      <view class="sec-title">成员业绩（本月）</view>
      <view v-for="m in memberSales" :key="m.userId" class="mem-row">
        <text class="flex-1 mem-name">{{ m.name }}</text>
        <text class="mem-qty">{{ m.quantity }} 件</text>
        <text class="mem-amt">¥{{ fmt(m.amount) }}</text>
      </view>
    </view>

    <!-- ③ 类目分布 -->
    <view class="card">
      <view class="card-title"><text class="flex-1">类目分布</text></view>
      <view v-if="!catRows.length" class="hint">还没有商品</view>
      <view v-for="c in catRows" :key="c.name" class="bar-row">
        <view class="bar-head">
          <text class="flex-1">{{ c.name }}</text>
          <text class="bar-num">{{ c.count }} 个 · ¥{{ fmt(c.value) }}</text>
        </view>
        <view class="bar-bg">
          <view class="bar-fill" :style="{ width: c.pct + '%' }"></view>
        </view>
      </view>
    </view>

    <!-- ④ 库存风险 -->
    <view class="card">
      <view class="card-title"><text class="flex-1">库存风险</text></view>
      <view class="risk-row" v-for="r in risks" :key="r.key">
        <view class="risk-dot" :class="r.level"></view>
        <view class="flex-1 min-w-0">
          <view class="risk-title">{{ r.title }}</view>
          <view class="risk-desc">{{ r.desc }}</view>
        </view>
        <text class="risk-num">{{ r.count }}</text>
      </view>
    </view>

    <!-- ⑤ 明细列表（可切换维度） -->
    <view class="card">
      <view class="card-title">
        <text class="flex-1">{{ listTitle }}</text>
        <view class="seg">
          <view class="seg-item" :class="{ on: listMode === 'risk' }" @tap="listMode = 'risk'">风险</view>
          <view class="seg-item" :class="{ on: listMode === 'value' }" @tap="listMode = 'value'">价值</view>
        </view>
      </view>
      <view v-if="!listRows.length" class="hint">暂无数据</view>
      <view v-for="p in listRows" :key="p.id" class="p-row">
        <view class="flex-1 min-w-0">
          <view class="p-name">{{ p.name }}</view>
          <view class="p-sub">{{ p.categoryName || '未分类' }} · 库存 {{ p.stockQuantity || 0 }} {{ p.unit || '个' }}<text v-if="p.slowDays !== null && p.slowDays !== undefined"> · {{ p.slowDays }} 天未动销</text></view>
        </view>
        <view class="p-right">
          <view class="p-val">¥{{ fmt((p.costPrice || 0) * (p.stockQuantity || 0)) }}</view>
          <view class="p-tag" :class="riskTagOf(p).cls">{{ riskTagOf(p).text }}</view>
        </view>
      </view>
    </view>

    <view class="foot">数据随商品与流水实时计算，下次进入自动刷新</view>
  </view>
</template>

<script>
import NavBar from '@/components/NavBar/NavBar.vue';
import { useThemeStore } from '@/stores/theme';
import { listWarehouses, listProducts, getWarehouseOverview } from '@/api/ledger';

/** 滞销判定：上架超过 N 天仍有库存 */
const SLOW_DAYS = 30;

export default {
  components: { NavBar },
  data() {
    return {
      warehouses: [],
      warehouseId: null,
      ov: {},
      products: [],
      listMode: 'risk'
    };
  },
  computed: {
    /** 按成员统计的销售业绩（后端概览返回） */
    memberSales() {
      return (this.ov && this.ov.memberSales) || [];
    },
    themeVars() { return useThemeStore().themeVars; },
    /** 类目分布：商品数 + 库存价值 + 占比 */
    catRows() {
      const map = new Map();
      for (const p of this.products) {
        const name = p.categoryName || '未分类';
        const v = (p.costPrice || 0) * (p.stockQuantity || 0);
        if (!map.has(name)) map.set(name, { name, count: 0, value: 0 });
        const row = map.get(name);
        row.count += 1;
        row.value += v;
      }
      const rows = Array.from(map.values()).sort((a, b) => b.value - a.value);
      const max = rows.reduce((m, r) => Math.max(m, r.value), 0) || 1;
      rows.forEach(r => { r.pct = Math.max(Math.round((r.value / max) * 100), 3); });
      return rows;
    },
    /** 风险统计 */
    risks() {
      const zero = this.products.filter(p => (p.stockQuantity || 0) === 0).length;
      const heavy = this.products.filter(p => (p.stockQuantity || 0) >= 50).length;
      const slow = this.products.filter(p => this.isSlow(p)).length;
      return [
        { key: 'zero', level: 'warn', title: '断货风险', desc: '库存已为 0，需要补货', count: zero },
        { key: 'heavy', level: 'warn', title: '压货风险', desc: '单商品库存 ≥ 50 件', count: heavy },
        { key: 'slow', level: 'bad', title: '滞销风险', desc: '上架超过 ' + SLOW_DAYS + ' 天仍有库存', count: slow },
        { key: 'expiring', level: 'warn', title: '临期风险', desc: '30 天内到期', count: this.ov.expiringCount || 0 },
        { key: 'expired', level: 'bad', title: '已过期', desc: '过期未处理，需尽快清理', count: this.ov.expiredCount || 0 }
      ];
    },
    listTitle() {
      return this.listMode === 'risk' ? '按风险排序' : '按价值排序';
    },
    listRows() {
      const list = this.products.slice();
      if (this.listMode === 'value') {
        list.sort((a, b) => ((b.costPrice || 0) * (b.stockQuantity || 0)) - ((a.costPrice || 0) * (a.stockQuantity || 0)));
      } else {
        // 风险优先：滞销 > 零库存 > 压货 > 其它
        const score = (p) => (this.isSlow(p) ? 3 : 0) + ((p.stockQuantity || 0) === 0 ? 2 : 0) + ((p.stockQuantity || 0) >= 50 ? 1 : 0);
        list.sort((a, b) => score(b) - score(a));
      }
      return list.slice(0, 20);
    }
  },
  onShow() {
    useThemeStore().restore();
    this.loadWarehouses();
  },
  methods: {
    fmt(v) { return Number(v || 0).toFixed(2); },
    /** 滞销：优先用后端返回的 slowDays（最后动销距今天数），老数据回落上架时间 */
    isSlow(p) {
      if (!p || (p.stockQuantity || 0) <= 0) return false;
      if (p.slowDays !== null && p.slowDays !== undefined) return p.slowDays > SLOW_DAYS;
      if (!p.createTime) return false;
      const t = new Date(String(p.createTime).replace(/-/g, '/')).getTime();
      if (isNaN(t)) return false;
      return (Date.now() - t) / 86400000 > SLOW_DAYS;
    },
    riskTagOf(p) {
      if ((p.stockQuantity || 0) === 0) return { text: '断货', cls: 'tag-warn' };
      if (this.isSlow(p)) return { text: '滞销', cls: 'tag-bad' };
      if ((p.stockQuantity || 0) >= 50) return { text: '压货', cls: 'tag-warn' };
      return { text: '正常', cls: 'tag-ok' };
    },
    async loadWarehouses() {
      try {
        this.warehouses = (await listWarehouses()) || [];
        if (this.warehouses.length && !this.warehouseId) this.warehouseId = this.warehouses[0].id;
        if (this.warehouseId) await this.refresh();
      } catch (e) { /* 拦截器已提示 */ }
    },
    async switchWh(w) {
      if (String(w.id) === String(this.warehouseId)) return;
      this.warehouseId = w.id;
      await this.refresh();
    },
    async refresh() {
      if (!this.warehouseId) return;
      try {
        const [ov, res] = await Promise.all([
          getWarehouseOverview(this.warehouseId).catch(() => null),
          listProducts(this.warehouseId, { pageSize: 100 }).catch(() => null)
        ]);
        this.ov = ov || {};
        this.products = (res && res.records) || [];
      } catch (e) { /* 拦截器已捕获 */ }
    }
  }
};
</script>

<style scoped>
.page { min-height: 100vh; padding-bottom: 60rpx; }

.wh-bar { white-space: nowrap; padding: 16rpx 24rpx 0; }
.wh-tag {
  display: inline-block; padding: 12rpx 26rpx; margin-right: 12rpx;
  border-radius: 999rpx; font-size: 26rpx; color: var(--text-sub);
  background: var(--card); border: 1rpx solid var(--border);
}
.wh-tag.active { color: #fff; background: var(--primary); border-color: var(--primary); }

.hero {
  margin: 20rpx 24rpx 0; padding: 32rpx 28rpx; border-radius: 18rpx;
  background: linear-gradient(135deg, #5FA77A, #3D8B62); color: #fff;
}
.hero-label { font-size: 24rpx; opacity: .9; }
.hero-value { font-size: 56rpx; font-weight: 700; margin: 8rpx 0 6rpx; }
.hero-sub { font-size: 23rpx; opacity: .9; }

.card {
  margin: 16rpx 24rpx 0; padding: 20rpx 24rpx;
  background: var(--card); border: 1rpx solid var(--border); border-radius: 16rpx;
}
.card-title { display: flex; align-items: center; font-size: 30rpx; font-weight: 600; color: var(--text-main); padding: 8rpx 0 12rpx; }
.hint { font-size: 23rpx; color: var(--text-sub); padding: 12rpx 0; }

.kpi-row { display: flex; padding: 8rpx 0; }
.kpi { flex: 1; text-align: center; padding: 10rpx 0; }
.kpi-num { font-size: 32rpx; font-weight: 700; color: var(--text-main); }
.kpi-num.in { color: #0EA37A; }
.kpi-num.out { color: #E8654A; }
.kpi-label { font-size: 22rpx; color: var(--text-sub); margin-top: 4rpx; }

.bar-row { padding: 10rpx 0; }
.bar-head { display: flex; font-size: 24rpx; color: var(--text-main); padding-bottom: 6rpx; }
.bar-num { font-size: 22rpx; color: var(--text-sub); }
.bar-bg { height: 10rpx; border-radius: 999rpx; background: var(--bg); overflow: hidden; }
.bar-fill { height: 100%; border-radius: 999rpx; background: var(--primary); }

.risk-row { display: flex; align-items: center; gap: 12rpx; padding: 16rpx 0; border-top: 1rpx solid var(--border); }
.risk-dot { width: 12rpx; height: 12rpx; border-radius: 50%; background: #E8A33D; }
.risk-dot.bad { background: #E8654A; }
.risk-title { font-size: 26rpx; color: var(--text-main); }
.risk-desc { font-size: 22rpx; color: var(--text-sub); margin-top: 2rpx; }
.risk-num { font-size: 30rpx; font-weight: 700; color: var(--text-main); }

.seg { display: flex; gap: 6rpx; background: var(--bg); border-radius: 999rpx; padding: 4rpx; }
.seg-item { padding: 6rpx 20rpx; border-radius: 999rpx; font-size: 22rpx; color: var(--text-sub); }
.seg-item.on { background: var(--primary); color: #fff; }

.p-row { display: flex; align-items: center; padding: 16rpx 0; border-top: 1rpx solid var(--border); }
.p-name { font-size: 26rpx; color: var(--text-main); }
.p-sub { font-size: 22rpx; color: var(--text-sub); margin-top: 2rpx; }
.p-right { text-align: right; }
.p-val { font-size: 26rpx; font-weight: 600; color: var(--text-main); }
.p-tag { display: inline-block; margin-top: 4rpx; font-size: 20rpx; padding: 2rpx 10rpx; border-radius: 6rpx; }
.tag-ok { color: #0EA37A; background: rgba(14,163,122,.12); }
.tag-warn { color: #E8A33D; background: rgba(232,163,61,.14); }
.tag-bad { color: #E8654A; background: rgba(232,101,74,.12); }

.foot { text-align: center; font-size: 22rpx; color: var(--text-sub); padding: 30rpx 0 10rpx; }
.flex-1 { flex: 1; }
.min-w-0 { min-width: 0; }
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
/* 成员业绩 */
.mem-row { display: flex; align-items: center; padding: 14rpx 0; border-bottom: 1rpx solid var(--border); }
.mem-name { font-size: 26rpx; color: var(--text-main); font-weight: 600; }
.mem-qty { font-size: 22rpx; color: var(--text-sub); margin-right: 16rpx; }
.mem-amt { font-size: 26rpx; color: var(--text-main); font-weight: 700; }
</style>
