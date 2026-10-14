<template>
  <view class="cp-root" @tap="openPanel">
    <!-- 触发器：默认显示日期 + ▾；调用方也可用插槽自定义（如 .pick-value / .f-value） -->
    <slot>
      <text class="cp-text">{{ current || placeholder }}</text>
      <text class="cp-caret">▾</text>
    </slot>

    <!-- 月历弹层 -->
    <view v-if="visible" class="cp-mask" @tap.stop="close">
      <view class="cp-panel" @tap.stop>
        <!-- 年月 + 切月 -->
        <view class="cp-head">
          <view class="cp-nav" :class="{ disabled: !canPrev }" @tap="canPrev && shiftMonth(-1)">‹</view>
          <text class="cp-title">{{ viewYear }} 年 {{ viewMonth }} 月</text>
          <view class="cp-nav" :class="{ disabled: !canNext }" @tap="canNext && shiftMonth(1)">›</view>
        </view>

        <!-- 星期表头 -->
        <view class="cp-week">
          <text v-for="w in WEEK" :key="w" class="cp-week-cell">{{ w }}</text>
        </view>

        <!-- 日期网格 -->
        <view class="cp-grid">
          <view
            v-for="cell in cells"
            :key="cell.key"
            class="cp-cell"
            :class="{
              blank: cell.blank,
              disabled: !cell.blank && !cell.enabled,
              today: cell.isToday,
              selected: cell.value && cell.value === current
            }"
            @tap="cell.enabled && pick(cell)"
          >{{ cell.day }}</view>
        </view>

        <!-- 底部操作 -->
        <view class="cp-foot">
          <text class="cp-foot-btn" :class="{ disabled: !todayEnabled }" @tap="todayEnabled && pickToday()">今天</text>
          <text class="cp-foot-btn plain" @tap="close">关闭</text>
        </view>
      </view>
    </view>
  </view>
</template>

<script>
/**
 * 月历选择器（自绘，替代 uni-app 的 <picker mode="date">）
 *
 * ## 为什么自绘
 *
 * uni-app 的 `mode="date"` 在桌面端会走**浏览器原生 <input type="date">**（小日历，
 * 格子仅约 20px，手指点不中、样式不可控）；不传 fields 也无法绕开。
 * 而 `mode="time"` 走的是自绘滚轮，两者尺寸/交互完全不一致。
 * 本组件自绘月历：格子 88rpx(44px) 达移动端可点尺寸，样式跟随主题变量。
 *
 * ## 用法（兼容原 <picker> 的 :value + @change 写法，替换标签即可）
 *
 * ```html
 * <CalendarPicker :value="form.date" :end="todayStr" @change="onDateChange">
 *   <view class="pick-value">{{ form.date }} ▾</view>
 * </CalendarPicker>
 * ```
 *
 * - 传 `v-model` 亦可（内部同时 emit `update:modelValue`）；
 * - `@change` 事件形如 `{ detail: { value } }`，与 uni-app picker 一致，
 *   因此原有的 `(e) => form.date = e.detail.value` 回调无需改动。
 */
const WEEK = ['日', '一', '二', '三', '四', '五', '六'];

const pad2 = (n) => String(n).padStart(2, '0');
const toDateStr = (y, m, d) => y + '-' + pad2(m) + '-' + pad2(d);

export default {
  name: 'CalendarPicker',
  props: {
    /** 选中值 YYYY-MM-DD（同时支持 value / modelValue 两种绑法） */
    value: { type: String, default: '' },
    modelValue: { type: String, default: '' },
    /** 可选范围（含端点），YYYY-MM-DD；留空表示不限 */
    start: { type: String, default: '' },
    end: { type: String, default: '' },
    placeholder: { type: String, default: '选择日期' }
  },
  emits: ['update:modelValue', 'change'],
  data() {
    const now = new Date();
    return {
      visible: false,
      viewYear: now.getFullYear(),
      viewMonth: now.getMonth() + 1,
      WEEK
    };
  },
  computed: {
    current() { return this.modelValue || this.value || ''; },
    todayStr() {
      const d = new Date();
      return toDateStr(d.getFullYear(), d.getMonth() + 1, d.getDate());
    },
    /** 当月网格（首日前补空格，使日期与星期对齐） */
    cells() {
      const y = this.viewYear;
      const m = this.viewMonth;
      const firstWeekday = new Date(y, m - 1, 1).getDay(); // 0=周日
      const daysInMonth = new Date(y, m, 0).getDate();
      const list = [];
      for (let i = 0; i < firstWeekday; i++) {
        list.push({ key: 'blank-' + i, day: '', blank: true, enabled: false });
      }
      for (let d = 1; d <= daysInMonth; d++) {
        const value = toDateStr(y, m, d);
        list.push({
          key: value,
          day: d,
          value,
          blank: false,
          isToday: value === this.todayStr,
          enabled: this.inRange(value)
        });
      }
      return list;
    },
    canPrev() {
      if (!this.start) return true;
      const y = this.viewMonth === 1 ? this.viewYear - 1 : this.viewYear;
      const m = this.viewMonth === 1 ? 12 : this.viewMonth - 1;
      return toDateStr(y, m, new Date(y, m, 0).getDate()) >= this.start;
    },
    canNext() {
      if (!this.end) return true;
      const y = this.viewMonth === 12 ? this.viewYear + 1 : this.viewYear;
      const m = this.viewMonth === 12 ? 1 : this.viewMonth + 1;
      return toDateStr(y, m, 1) <= this.end;
    },
    todayEnabled() { return this.inRange(this.todayStr); }
  },
  methods: {
    /** 日期字符串比较（YYYY-MM-DD 可直接按字典序比较） */
    inRange(value) {
      if (this.start && value < this.start) return false;
      if (this.end && value > this.end) return false;
      return true;
    },
    openPanel() {
      // 打开时定位到「已选日期」所在月；未选则定位到本月
      const base = this.current || this.todayStr;
      const parts = String(base).split('-');
      if (parts.length === 3) {
        this.viewYear = Number(parts[0]);
        this.viewMonth = Number(parts[1]);
      }
      this.visible = true;
    },
    close() { this.visible = false; },
    shiftMonth(delta) {
      let y = this.viewYear;
      let m = this.viewMonth + delta;
      if (m < 1) { m = 12; y--; }
      if (m > 12) { m = 1; y++; }
      this.viewYear = y;
      this.viewMonth = m;
    },
    pick(cell) {
      this.$emit('update:modelValue', cell.value);
      // 与 uni-app picker 的事件结构保持一致，调用方回调无需改动
      this.$emit('change', { detail: { value: cell.value } });
      this.close();
    },
    pickToday() { this.pick({ value: this.todayStr }); }
  }
};
</script>

<style scoped>
.cp-root { display: inline-flex; align-items: center; }
.cp-text { color: inherit; font-size: inherit; }
.cp-caret { margin-left: 6rpx; }

/* 弹层：层级与全局遮罩一致（999，高于 H5 TabBar 的 998） */
.cp-mask {
  position: fixed; top: 0; right: 0; bottom: 0; left: 0;
  z-index: 999;
  background: rgba(0, 0, 0, 0.45);
  display: flex; align-items: flex-end;
}
.cp-panel {
  width: 100%;
  background: var(--card);
  border-radius: 28rpx 28rpx 0 0;
  padding: 24rpx 24rpx calc(24rpx + env(safe-area-inset-bottom));
  box-sizing: border-box;
}

.cp-head { display: flex; align-items: center; justify-content: space-between; padding: 8rpx 8rpx 16rpx; }
.cp-title { font-size: 32rpx; font-weight: 600; color: var(--text-main); }
.cp-nav {
  /* 切月箭头：热区 88rpx(=44px) 达移动端可点尺寸 */
  width: 88rpx; height: 88rpx; line-height: 88rpx; text-align: center;
  font-size: 44rpx; color: var(--primary-strong);
  border-radius: 50%;
}
.cp-nav.disabled { color: var(--text-muted); opacity: 0.4; }

.cp-week { display: flex; }
.cp-week-cell {
  flex: 1; text-align: center; font-size: 24rpx; color: var(--text-sub);
  padding-bottom: 8rpx;
}

.cp-grid { display: flex; flex-wrap: wrap; }
.cp-cell {
  /* 7 列 × 宽 14.28%，高 88rpx(=44px)：格子足够大，手指好点 */
  width: 14.28%; height: 88rpx; line-height: 88rpx;
  text-align: center; font-size: 30rpx; color: var(--text-main);
  border-radius: 16rpx;
  box-sizing: border-box;
}
.cp-cell.blank { visibility: hidden; }
.cp-cell.disabled { color: var(--text-muted); opacity: 0.45; }
.cp-cell.today { color: var(--primary-strong); font-weight: 700; }
.cp-cell.selected { background: var(--primary); color: var(--text-main); font-weight: 700; }

.cp-foot { display: flex; gap: 20rpx; margin-top: 20rpx; }
.cp-foot-btn {
  flex: 1; height: 88rpx; line-height: 88rpx; text-align: center;
  font-size: 30rpx; border-radius: 44rpx;
  background: var(--primary-soft); color: var(--primary-strong);
}
.cp-foot-btn.plain { background: var(--surface); color: var(--text-sub); }
.cp-foot-btn.disabled { opacity: 0.45; }
</style>
