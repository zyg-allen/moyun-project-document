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

      <!-- 二级：点了带子类的一级后，在这一级下面展开 -->
      <view v-if="showSub && subList.length" class="sub-wrap">
        <view class="sub-head">
          <text class="sub-title">{{ currentTop ? currentTop.name : '' }} · 二级</text>
          <text class="sub-close" @tap="showSub = false">收起 ▴</text>
        </view>
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
      </view>
    </scroll-view>
  </view>
</template>

<script>
import { listCategories } from '@/api/ledger';
import { categoryIcon, softColor as toSoftColor, isImageIcon } from '@/utils/categoryIcon';

/**
 * 记账分类选择器 —— 与 pages/record/index 的分类区一致。
 *
 * 交互：
 *   默认收起，只显示「选择分类 + 已选名称 + 展开」。
 *   展开 → 一级图标宫格（4 列）。
 *   点一级：
 *     带子类 → 在该一级下方**内联展开**二级宫格（含「不细分」）
 *     无子类 → 直接选中
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
      list: [],
      innerTopId: null,
      subId: null,
      /** 二级宫格是否展开（点带子类的一级才展开） */
      showSub: false,
      /** 一级宫格默认收起 */
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
          this.showSub = false;
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
    /** 点一级：有子类就展开二级，没有就直接选中并收起 */
    pickTop(c) {
      this.innerTopId = c.id;
      this.subId = null;
      const hasKids = (c.children || []).length > 0;
      this.emit(c.id, c.id);
      if (hasKids) {
        this.showSub = true;
      } else {
        this.showSub = false;
        this.collapsed = true;
      }
    },
    /** 点二级（或「不细分」）：选中并收起 */
    pickSub(k) {
      this.subId = k ? k.id : null;
      this.showSub = false;
      this.collapsed = true;
      this.emit(k ? k.id : this.innerTopId, this.innerTopId);
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
/* ==== 与 record 页对齐 ==== */
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
.cat-empty { width: 100%; text-align: center; color: var(--text-muted); font-size: 26rpx; padding: 40rpx 0; }

/* 二级内联展开 */
.sub-wrap { margin-top: 8rpx; padding-top: 8rpx; border-top: 1rpx dashed var(--border); }
.sub-head { display: flex; justify-content: space-between; align-items: center; padding: 8rpx 16rpx; }
.sub-title { font-size: 22rpx; color: var(--text-sub); }
.sub-close { font-size: 21rpx; color: var(--primary); }
</style>
