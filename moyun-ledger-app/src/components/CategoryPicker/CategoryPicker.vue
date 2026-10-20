<template>
  <!-- 表单里只占一行：已选名称 + ▾，点它弹底部面板 -->
  <view class="cp-line" @tap="sheet = true">
    <text class="cp-value" :class="{ empty: !selectedName }">{{ selectedName || '选择分类' }}</text>
    <text class="cp-arrow">▾</text>
  </view>

  <view v-if="sheet" class="cp-mask" @tap="sheet = false">
    <view class="cp-sheet" @tap.stop>
      <view class="cp-sheet-title">选择支出分类</view>

      <scroll-view scroll-y class="cp-body">
        <!-- 一级 -->
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

        <!-- 二级：选中带子类的一级后，在同一面板下方展开 -->
        <view v-if="innerTopId && subList.length" class="cp-sub">
          <view class="cp-sub-title">{{ currentTop ? currentTop.name : '' }} · 二级</view>
          <view class="cat-grid">
            <view class="cat-cell" @tap="pickSub(null)">
              <view class="cat-icon" :class="{ selected: !subId }" style="background: var(--surface)"><text>∅</text></view>
              <text class="cat-name" :class="{ selected: !subId }">不细分</text>
            </view>
            <view v-for="s in subList" :key="s.id" class="cat-cell" @tap="pickSub(s)">
              <view class="cat-icon" :class="{ selected: String(subId) === String(s.id) }"
                    :style="String(subId) === String(s.id) ? '' : 'background:' + softColor(s.color)">
                <image v-if="isImg(s.icon)" :src="s.icon" class="grid-icon-img" mode="aspectFill" />
                <text v-else>{{ iconOf(s.icon) }}</text>
              </view>
              <text class="cat-name" :class="{ selected: String(subId) === String(s.id) }">{{ s.name }}</text>
            </view>
          </view>
        </view>
      </scroll-view>

      <view class="cp-sheet-actions">
        <view class="cp-btn ghost" @tap="sheet = false">关闭</view>
      </view>
    </view>
  </view>
</template>

<script>
import { listCategories } from '@/api/ledger';
import { categoryIcon, softColor as toSoftColor, isImageIcon } from '@/utils/categoryIcon';

/**
 * 记账分类选择器。
 * 数据是**扁平列表**，子分类靠 parentId 关联。
 *
 * 表单里只占一行：「已选名称 ▾」。
 * 点开底部面板：一级宫格 → 点带子类的一级，面板下方接着出二级宫格。
 */
export default {
  name: 'CategoryPicker',
  props: {
    modelValue: { type: [Number, String], default: null },
    type: { type: String, default: 'expense' }
  },
  data() {
    return { list: [], innerTopId: null, subId: null, sheet: false };
  },
  computed: {
    topList() {
      return this.list.filter(c => !c.parentId || String(c.parentId) === '0');
    },
    currentTop() {
      return this.list.find(c => String(c.id) === String(this.innerTopId)) || null;
    },
    subList() {
      if (!this.innerTopId) return [];
      return this.list.filter(c => String(c.parentId) === String(this.innerTopId));
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
        if (!v) { this.innerTopId = null; this.subId = null; return; }
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
      // 有二级就让用户接着选；没有就关面板
      if (!hasKids) this.sheet = false;
    },
    pickSub(s) {
      this.subId = s ? s.id : null;
      this.sheet = false;
      this.emit(s ? s.id : this.innerTopId, this.innerTopId);
    },
    emit(value, topId) {
      this.$emit('update:modelValue', value);
      this.$emit('change', { id: value, topId });
    }
  }
};
</script>

<style scoped>
/* 表单里的一行 */
.cp-line { flex: 1; display: flex; align-items: center; justify-content: flex-end; gap: 8rpx; }
.cp-value { font-size: 26rpx; color: var(--text-main); }
.cp-value.empty { color: var(--text-muted); }
.cp-arrow { font-size: 20rpx; color: var(--text-sub); }

/* 底部面板 */
.cp-mask { position: fixed; inset: 0; background: rgba(0,0,0,.45); z-index: 999; display: flex; align-items: flex-end; }
.cp-sheet { width: 100%; max-height: 72vh; background: var(--card); border-radius: 24rpx 24rpx 0 0; padding: 24rpx 16rpx 40rpx; }
.cp-sheet-title { font-size: 30rpx; font-weight: 700; text-align: center; color: var(--text-main); padding-bottom: 16rpx; }
.cp-body { max-height: 52vh; }

.cat-grid { display: flex; flex-wrap: wrap; }
.cat-cell { width: 25%; display: flex; flex-direction: column; align-items: center; padding: 12rpx 0; }
.cat-icon {
  width: 72rpx; height: 72rpx; border-radius: 18rpx;
  display: flex; align-items: center; justify-content: center;
  font-size: 34rpx; background: var(--surface); overflow: hidden;
  box-shadow: 0 4rpx 10rpx rgba(0,0,0,.08);
}
.grid-icon-img { width: 100%; height: 100%; }
.cat-icon.selected { background: var(--primary) !important; transform: scale(1.06); }
.cat-name { font-size: 22rpx; color: var(--text-sub); margin-top: 8rpx; }
.cat-name.selected { color: var(--primary); font-weight: 600; }
.cat-empty { width: 100%; text-align: center; color: var(--text-muted); font-size: 26rpx; padding: 30rpx 0; }

.cp-sub { margin-top: 12rpx; padding-top: 12rpx; border-top: 1rpx dashed var(--border); }
.cp-sub-title { font-size: 22rpx; color: var(--text-sub); padding: 4rpx 12rpx 8rpx; }

.cp-sheet-actions { display: flex; gap: 16rpx; padding: 16rpx 12rpx 0; }
.cp-btn { flex: 1; text-align: center; padding: 20rpx 0; border-radius: 12rpx; background: var(--primary); color: #fff; font-size: 26rpx; }
.cp-btn.ghost { background: transparent; color: var(--text-sub); border: 1rpx solid var(--border); }
</style>
