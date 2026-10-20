<template>
  <!-- 与 pages/record/index 完全一致的分类选择区 -->
  <view class="cat-section">
    <view class="cat-section-title">
      <text>选择分类</text>
      <view class="cat-title-right" @tap="collapsed = !collapsed">
        <text class="cat-count">{{ selectedName || '请选择' }}</text>
        <text class="cat-toggle">{{ collapsed ? '展开 ▾' : '收起 ▴' }}</text>
      </view>
    </view>

    <scroll-view v-show="!collapsed" scroll-y class="cat-grid-wrap">
      <view class="cat-grid">
        <view v-for="c in list" :key="c.id" class="cat-cell" @tap="pickTop(c)">
          <view class="cat-icon" :class="{ selected: String(innerTopId) === String(c.id) }"
                :style="String(innerTopId) === String(c.id) ? '' : 'background:' + softColor(c.color)">
            <image v-if="isImg(c.icon)" :src="c.icon" class="grid-icon-img" mode="aspectFill" />
            <text v-else>{{ iconOf(c.icon) }}</text>
          </view>
          <text class="cat-name" :class="{ selected: String(innerTopId) === String(c.id) }">{{ c.name }}</text>
        </view>
        <view v-if="!list.length" class="cat-empty">还没有分类，去「记账分类」添加</view>
      </view>
    </scroll-view>

    <!-- 二级分类：选中大类后常驻一行，点开弹层选择，选完自动收起 -->
    <view v-if="innerTopId" class="pick-row" @tap="subSheet = true">
      <text class="pick-label">二级</text>
      <text class="pick-value">{{ subLabel }} ▾</text>
    </view>

    <!-- 二级宫格弹层（仿分类宫格：图标 + 名称） -->
    <view v-if="subSheet" class="cp-mask" @tap="subSheet = false">
      <view class="cp-sheet" @tap.stop>
        <view class="cp-sheet-title">{{ currentTop ? currentTop.name : '' }}</view>
        <scroll-view scroll-y class="cp-sheet-body">
          <view class="cat-grid">
            <view class="cat-cell" @tap="pickSub(null)">
              <view class="cat-icon" :class="{ selected: !subId }" style="background: var(--surface)">
                <text>—</text>
              </view>
              <text class="cat-name" :class="{ selected: !subId }">不细分</text>
            </view>
            <view v-for="k in subList" :key="k.id" class="cat-cell" @tap="pickSub(k)">
              <view class="cat-icon" :class="{ selected: String(subId) === String(k.id) }"
                    :style="String(subId) === String(k.id) ? '' : 'background:' + softColor(k.color)">
                <image v-if="isImg(k.icon)" :src="k.icon" class="grid-icon-img" mode="aspectFill" />
                <text v-else>{{ iconOf(k.icon) }}</text>
              </view>
              <text class="cat-name" :class="{ selected: String(subId) === String(k.id) }">{{ k.name }}</text>
            </view>
          </view>
        </scroll-view>
        <view class="cp-sheet-actions">
          <view class="cp-btn ghost" @tap="subSheet = false">关闭</view>
        </view>
      </view>
    </view>
  </view>
</template>

<script>
import { listCategories } from '@/api/ledger';
import { categoryIcon, softColor as toSoftColor, isImageIcon } from '@/utils/categoryIcon';

/**
 * 记账分类选择器 —— 与 pages/record/index 的分类区**完全一致**：
 *   标题行（已选名称 + 展开/收起）→ 一级图标宫格 → 二级行 → 二级宫格弹层
 * 默认收起，只显示标题行（已选名称可见）。
 *
 * v-model 传出最终分类 id（选了二级就是二级的 id）。
 */
export default {
  name: 'CategoryPicker',
  props: {
    modelValue: { type: [Number, String], default: null },
    type: { type: String, default: 'expense' }
  },
  data() {
    return {
      list: [],
      innerTopId: null,
      subId: null,
      subSheet: false,
      /** 默认收起 */
      collapsed: true
    };
  },
  computed: {
    currentTop() {
      return this.list.find(c => String(c.id) === String(this.innerTopId)) || null;
    },
    subList() {
      return (this.currentTop && this.currentTop.children) || [];
    },
    subLabel() {
      if (!this.subId) return '不细分（大类本身）';
      const k = this.subList.find(x => String(x.id) === String(this.subId));
      return k ? k.name : '不细分（大类本身）';
    },
    selectedName() {
      const top = this.currentTop;
      if (!top) return '';
      if (this.subId) {
        const k = this.subList.find(x => String(x.id) === String(this.subId));
        return top.name + (k ? ' / ' + k.name : '');
      }
      return top.name;
    }
  },
  watch: {
    modelValue: {
      immediate: true,
      handler(v) {
        if (!v) {
          this.innerTopId = null;
          this.subId = null;
        } else {
          this.locate(v);
        }
      }
    }
  },
  mounted() {
    this.load();
  },
  methods: {
    isImg(icon) { return isImageIcon(icon); },
    iconOf(icon) { return categoryIcon(icon); },
    softColor(c) { return toSoftColor(c); },
    async load() {
      try {
        const res = await listCategories(this.type);
        this.list = (res && res.records) || [];
        if (this.modelValue) this.locate(this.modelValue);
      } catch (e) {
        this.list = [];
      }
    },
    locate(id) {
      for (const c of this.list) {
        if (String(c.id) === String(id)) {
          this.innerTopId = c.id;
          this.subId = null;
          return;
        }
        const k = (c.children || []).find(x => String(x.id) === String(id));
        if (k) {
          this.innerTopId = c.id;
          this.subId = k.id;
          return;
        }
      }
    },
    pickTop(c) {
      this.innerTopId = c.id;
      this.subId = null;
      const hasKids = (c.children || []).length > 0;
      this.emit(c.id, c.id);
      // 有子类时接着弹二级宫格；没有就收起
      if (hasKids) {
        this.subSheet = true;
      } else {
        this.collapsed = true;
      }
    },
    pickSub(k) {
      this.subId = k ? k.id : null;
      this.subSheet = false;
      this.collapsed = true;
      this.emit(k ? k.id : this.innerTopId, this.innerTopId);
    },
    emit(value, topId) {
      this.$emit('update:modelValue', value);
      this.$emit('change', { id: value, topId });
    }
  }
};
</script>

<style scoped>
/* ==== 与 record 页对齐 ==== */
.cat-section { background: var(--card); margin: 20rpx 24rpx; border-radius: 20rpx; padding: 24rpx 12rpx 8rpx; }
.cat-section-title {
  display: flex; justify-content: space-between; align-items: center;
  padding: 0 20rpx 16rpx; font-size: 26rpx; color: var(--text-sub);
}
.cat-title-right { display: flex; align-items: center; }
.cat-count { font-size: 22rpx; color: var(--primary); font-weight: 400; margin-right: 16rpx; }
.cat-toggle {
  font-size: 22rpx; color: var(--text-sub);
  padding: 4rpx 12rpx; background: var(--surface); border-radius: 20rpx;
}
.cat-grid-wrap { max-height: 560rpx; }
.cat-grid { display: flex; flex-wrap: wrap; }
.cat-cell {
  position: relative; width: 25%; display: flex; flex-direction: column;
  align-items: center; padding: 12rpx 0;
}
.cat-icon {
  width: 72rpx; height: 72rpx; border-radius: 18rpx;
  display: flex; align-items: center; justify-content: center;
  font-size: 34rpx;
  box-shadow: 0 4rpx 10rpx rgba(0, 0, 0, 0.08);
  overflow: hidden;
}
.grid-icon-img { width: 100%; height: 100%; }
.cat-icon.selected { background: var(--primary) !important; transform: scale(1.06); }
.cat-name { font-size: 22rpx; color: var(--text-sub); margin-top: 8rpx; }
.cat-name.selected { color: var(--primary); font-weight: 600; }
.cat-empty { width: 100%; text-align: center; color: var(--text-muted); font-size: 26rpx; padding: 40rpx 0; }

.pick-row { display: flex; align-items: center; padding: 24rpx 20rpx; border-bottom: 1rpx solid var(--border); }
.pick-label { width: 200rpx; color: var(--text-sub); font-size: 28rpx; }
.pick-value { flex: 1; text-align: right; color: var(--text-main); font-size: 28rpx; }

/* 二级弹层 */
.cp-mask { position: fixed; inset: 0; background: rgba(0,0,0,.45); z-index: 999; display: flex; align-items: flex-end; }
.cp-sheet { width: 100%; max-height: 70vh; background: var(--card); border-radius: 24rpx 24rpx 0 0; padding: 24rpx 16rpx 40rpx; }
.cp-sheet-title { font-size: 30rpx; font-weight: 700; text-align: center; color: var(--text-main); padding-bottom: 16rpx; }
.cp-sheet-body { max-height: 48vh; }
.cp-sheet-actions { display: flex; gap: 16rpx; padding: 16rpx 12rpx 0; }
.cp-btn { flex: 1; text-align: center; padding: 20rpx 0; border-radius: 12rpx; background: var(--primary); color: #fff; font-size: 26rpx; }
.cp-btn.ghost { background: transparent; color: var(--text-sub); border: 1rpx solid var(--border); }
</style>
