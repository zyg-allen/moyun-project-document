<template>
  <!-- 与 pages/record/index 一致的分类选择区 -->
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
        <view v-for="c in topList" :key="c.id" class="cat-cell" @tap="pickTop(c)">
          <view class="cat-icon" :class="{ selected: String(innerTopId) === String(c.id) }"
                :style="String(innerTopId) === String(c.id) ? '' : 'background:' + softColor(c.color)">
            <image v-if="isImg(c.icon)" :src="c.icon" class="grid-icon-img" mode="aspectFill" />
            <text v-else>{{ iconOf(c.icon) }}</text>
          </view>
          <text class="cat-name" :class="{ selected: String(innerTopId) === String(c.id) }">{{ c.name }}</text>
        </view>
        <view v-if="!topList.length" class="cat-empty">还没有分类，去「记账分类」添加</view>
      </view>
    </scroll-view>

    <!-- 二级：选中大类后常驻一行，点开底部弹层选择，选完自动收起 -->
    <view v-if="innerTopId" class="pick-row" @tap="subSheet = true">
      <text class="pick-label">二级</text>
      <text class="pick-value">{{ subLabel }} ▾</text>
    </view>

    <!-- 二级底部弹层（与记账页同款） -->
    <view v-if="subSheet" class="cp-mask" @tap="subSheet = false">
      <view class="cp-sheet" @tap.stop>
        <view class="cp-sheet-title">{{ currentTop ? currentTop.name : '' }}</view>
        <scroll-view scroll-y class="cp-sheet-body">
          <view class="subsheet-grid">
            <view class="subsheet-cell" :class="{ on: !subId }" @tap="pickSub(null)">
              <view class="subsheet-icon" :class="{ on: !subId }"><text>∅</text></view>
              <text class="subsheet-name" :class="{ on: !subId }">不细分</text>
            </view>
            <view v-for="s in subList" :key="s.id" class="subsheet-cell" @tap="pickSub(s)">
              <view class="subsheet-icon" :class="{ on: String(subId) === String(s.id) }"
                    :style="String(subId) === String(s.id) ? '' : 'background:' + softColor(s.color)">
                <image v-if="isImg(s.icon)" :src="s.icon" class="grid-icon-img" mode="aspectFill" />
                <text v-else>{{ iconOf(s.icon) }}</text>
              </view>
              <text class="subsheet-name" :class="{ on: String(subId) === String(s.id) }">{{ s.name }}</text>
            </view>
            <view v-if="!subList.length" class="cat-empty">这个大类还没有二级分类</view>
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
 * 记账分类选择器 —— 与 pages/record/index 的交互一致。
 *
 * 数据形态：**扁平列表**，子分类靠 parentId 关联（不是嵌套 children）。
 *
 * 交互：
 *   默认收起，只显示「选择分类 + 已选名称 + 展开」。
 *   展开 → 一级宫格（4 列，parentId 为空/0）。
 *   点一级：有二级 → 选中一级并**底部弹出二级层**；无二级 → 直接选中收起。
 *   点二级 / 不细分 → 选中并收起。
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
      /** 接口返回的扁平分类列表 */
      list: [],
      innerTopId: null,
      subId: null,
      subSheet: false,
      collapsed: true
    };
  },
  computed: {
    /** 一级：没有 parentId 或 parentId 为 0 */
    topList() {
      return this.list.filter(c => !c.parentId || String(c.parentId) === '0');
    },
    currentTop() {
      return this.list.find(c => String(c.id) === String(this.innerTopId)) || null;
    },
    /** 二级：parentId 等于当前一级 id */
    subList() {
      if (!this.innerTopId) return [];
      return this.list.filter(c => String(c.parentId) === String(this.innerTopId));
    },
    subLabel() {
      if (!this.subId) return '不细分（大类本身）';
      const k = this.list.find(x => String(x.id) === String(this.subId));
      return k ? k.name : '不细分（大类本身）';
    },
    selectedName() {
      const top = this.currentTop;
      if (!top) return '';
      if (this.subId) {
        const k = this.list.find(x => String(x.id) === String(this.subId));
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
          return;
        }
        this.locate(v);
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
    /** 根据 id 反推它是一级还是某个一级下的二级 */
    locate(id) {
      const hit = this.list.find(c => String(c.id) === String(id));
      if (!hit) return;
      if (!hit.parentId || String(hit.parentId) === '0') {
        this.innerTopId = hit.id;
        this.subId = null;
      } else {
        this.innerTopId = hit.parentId;
        this.subId = hit.id;
      }
    },
    pickTop(c) {
      this.innerTopId = c.id;
      this.subId = null;
      const hasKids = this.list.some(x => String(x.parentId) === String(c.id));
      this.emit(c.id, c.id);
      if (hasKids) {
        // 有二级：底部弹出让人选细分
        this.subSheet = true;
      } else {
        this.collapsed = true;
      }
    },
    pickSub(s) {
      this.subId = s ? s.id : null;
      this.subSheet = false;
      this.collapsed = true;
      this.emit(s ? s.id : this.innerTopId, this.innerTopId);
    },
    /** 对外抛出选中结果 */
    emit(value, topId) {
      this.$emit('update:modelValue', value);
      this.$emit('change', { id: value, topId });
    }
  }
};
</script>

<style scoped>
.cat-section { background: var(--card); margin: 0; border-radius: 12rpx; padding: 16rpx 8rpx 8rpx; }
.cat-section-title {
  display: flex; justify-content: space-between; align-items: center;
  padding: 0 16rpx 12rpx; font-size: 26rpx; color: var(--text-sub);
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
.cat-empty { width: 100%; text-align: center; color: var(--text-muted); font-size: 26rpx; padding: 30rpx 0; }

.pick-row { display: flex; align-items: center; padding: 20rpx 16rpx; border-top: 1rpx solid var(--border); }
.pick-label { width: 120rpx; color: var(--text-sub); font-size: 26rpx; }
.pick-value { flex: 1; text-align: right; color: var(--text-main); font-size: 26rpx; }

/* 二级底部弹层 */
.cp-mask { position: fixed; inset: 0; background: rgba(0,0,0,.45); z-index: 999; display: flex; align-items: flex-end; }
.cp-sheet { width: 100%; max-height: 70vh; background: var(--card); border-radius: 24rpx 24rpx 0 0; padding: 24rpx 16rpx 40rpx; }
.cp-sheet-title { font-size: 30rpx; font-weight: 700; text-align: center; color: var(--text-main); padding-bottom: 16rpx; }
.cp-sheet-body { max-height: 48vh; }
.subsheet-grid { display: flex; flex-wrap: wrap; padding: 8rpx 0; }
.subsheet-cell { width: 25%; display: flex; flex-direction: column; align-items: center; padding: 12rpx 0; }
.subsheet-icon {
  width: 72rpx; height: 72rpx; border-radius: 18rpx;
  display: flex; align-items: center; justify-content: center; font-size: 34rpx;
  background: var(--surface); overflow: hidden;
}
.subsheet-icon.on { background: var(--primary) !important; }
.subsheet-name { font-size: 22rpx; color: var(--text-sub); margin-top: 8rpx; }
.subsheet-name.on { color: var(--primary); font-weight: 600; }
.cp-sheet-actions { display: flex; gap: 16rpx; padding: 16rpx 12rpx 0; }
.cp-btn { flex: 1; text-align: center; padding: 20rpx 0; border-radius: 12rpx; background: var(--primary); color: #fff; font-size: 26rpx; }
.cp-btn.ghost { background: transparent; color: var(--text-sub); border: 1rpx solid var(--border); }
</style>
