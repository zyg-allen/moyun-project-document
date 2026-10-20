<template>
  <view class="cp">
    <!-- 一级分类宫格（与记一笔同款：图标 + 名称 + 选中高亮） -->
    <view class="cp-grid">
      <view v-for="c in list" :key="c.id" class="cp-cell" @tap="pickTop(c)">
        <view class="cp-icon" :class="{ selected: String(innerTopId) === String(c.id) }"
              :style="String(innerTopId) === String(c.id) ? '' : 'background:' + softColor(c.color)">
          <image v-if="isImg(c.icon)" :src="c.icon" class="cp-img" mode="aspectFill" />
          <text v-else>{{ iconOf(c.icon) }}</text>
        </view>
        <text class="cp-name" :class="{ selected: String(innerTopId) === String(c.id) }">{{ c.name }}</text>
      </view>
      <view v-if="!list.length" class="cp-empty">还没有分类，去「记账分类」添加</view>
    </view>

    <!-- 二级：选中的一级如果带子类，才出现这一行 -->
    <view v-if="currentTop" class="cp-sub-row" @tap="openSub">
      <text class="cp-sub-label">二级</text>
      <text class="cp-sub-value">{{ subLabel }}</text>
      <text class="cp-sub-arrow">▾</text>
    </view>

    <!-- 二级选择弹层 -->
    <view v-if="subSheet" class="cp-mask" @tap="subSheet = false">
      <view class="cp-sheet" @tap.stop>
        <view class="cp-sheet-title">{{ currentTop ? currentTop.name : '' }}</view>
        <scroll-view scroll-y class="cp-sheet-body">
          <view class="cp-sub-grid">
            <view class="cp-sub-cell" @tap="pickSub(null)">
              <view class="cp-sub-icon" :class="{ selected: !subId }"><text>—</text></view>
              <text class="cp-sub-name" :class="{ selected: !subId }">不细分</text>
            </view>
            <view v-for="k in subList" :key="k.id" class="cp-sub-cell" @tap="pickSub(k)">
              <view class="cp-sub-icon" :class="{ selected: String(subId) === String(k.id) }"
                    :style="String(subId) === String(k.id) ? '' : 'background:' + softColor(k.color)">
                <image v-if="isImg(k.icon)" :src="k.icon" class="cp-img" mode="aspectFill" />
                <text v-else>{{ iconOf(k.icon) }}</text>
              </view>
              <text class="cp-sub-name" :class="{ selected: String(subId) === String(k.id) }">{{ k.name }}</text>
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
 * 记账分类选择器（与 pages/record/index 同款交互）。
 *
 * 一级：图标宫格，点选即生效。
 * 二级：选中的一级若有子类，下方出现「二级」行，点开弹层选择；
 *       不选二级则用一级本身的 id。
 *
 * v-model 传出的是最终选中的分类 id（二级优先）。
 */
export default {
  name: 'CategoryPicker',
  props: {
    modelValue: { type: [Number, String], default: null },
    /** 分类类型：expense / income */
    type: { type: String, default: 'expense' }
  },
  data() {
    return {
      list: [],
      innerTopId: null,
      subId: null,
      subSheet: false
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
    /** 根据传入的 id 反推它属于哪个一级 */
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
      // 有子类时先不提交，等用户决定是否细分；没有子类直接提交
      this.emit(hasKids ? null : c.id, c.id);
      if (hasKids) this.subSheet = true;
    },
    pickSub(k) {
      this.subId = k ? k.id : null;
      this.subSheet = false;
      this.emit(k ? k.id : this.innerTopId, this.innerTopId);
    },
    emit(value, topId) {
      this.$emit('update:modelValue', value);
      this.$emit('change', { id: value, topId });
    },
    openSub() {
      this.subSheet = true;
    }
  }
};
</script>

<style scoped>
.cp-grid { display: flex; flex-wrap: wrap; gap: 14rpx; }
.cp-cell { width: 100rpx; display: flex; flex-direction: column; align-items: center; }
.cp-icon {
  width: 72rpx; height: 72rpx; display: flex; align-items: center; justify-content: center;
  border-radius: 16rpx; font-size: 34rpx; overflow: hidden;
  border: 2rpx solid transparent;
}
.cp-icon.selected { border-color: var(--primary); background: rgba(95,167,122,.14); }
.cp-img { width: 100%; height: 100%; }
.cp-name { font-size: 21rpx; color: var(--text-main); margin-top: 6rpx; text-align: center; }
.cp-name.selected { color: var(--primary); font-weight: 600; }
.cp-empty { font-size: 22rpx; color: var(--text-sub); padding: 16rpx 0; }

.cp-sub-row {
  display: flex; align-items: center; gap: 10rpx;
  margin-top: 12rpx; padding: 12rpx 16rpx;
  border: 1rpx solid var(--border); border-radius: 10rpx; background: var(--bg);
}
.cp-sub-label { font-size: 22rpx; color: var(--text-sub); }
.cp-sub-value { flex: 1; text-align: right; font-size: 24rpx; color: var(--text-main); }
.cp-sub-arrow { font-size: 20rpx; color: var(--text-sub); }

.cp-mask { position: fixed; inset: 0; background: rgba(0,0,0,.45); z-index: 999; display: flex; align-items: flex-end; }
.cp-sheet { width: 100%; max-height: 70vh; background: var(--card); border-radius: 24rpx 24rpx 0 0; padding: 24rpx 24rpx 40rpx; }
.cp-sheet-title { font-size: 30rpx; font-weight: 700; text-align: center; color: var(--text-main); padding-bottom: 16rpx; }
.cp-sheet-body { max-height: 48vh; }
.cp-sub-grid { display: flex; flex-wrap: wrap; gap: 16rpx; padding: 8rpx 0; }
.cp-sub-cell { width: 120rpx; display: flex; flex-direction: column; align-items: center; }
.cp-sub-icon {
  width: 84rpx; height: 84rpx; display: flex; align-items: center; justify-content: center;
  border-radius: 16rpx; font-size: 32rpx; background: var(--bg); overflow: hidden;
  border: 2rpx solid transparent;
}
.cp-sub-icon.selected { border-color: var(--primary); background: rgba(95,167,122,.14); }
.cp-sub-name { font-size: 21rpx; color: var(--text-main); margin-top: 6rpx; text-align: center; }
.cp-sub-name.selected { color: var(--primary); font-weight: 600; }
.cp-sheet-actions { display: flex; gap: 16rpx; padding-top: 16rpx; }
.cp-btn { flex: 1; text-align: center; padding: 20rpx 0; border-radius: 12rpx; background: var(--primary); color: #fff; font-size: 26rpx; }
.cp-btn.ghost { background: transparent; color: var(--text-sub); border: 1rpx solid var(--border); }
</style>
