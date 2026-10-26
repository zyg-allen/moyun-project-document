<template>
  <view class="page" :style="themeVars">
    <NavBar title="备忘录" />

    <!-- 待办列表 -->
    <view class="todo-section">
      <view class="sec-title">待办事项 <text class="sec-count">{{ pendingTodos.length }}</text></view>
      <view class="todo-list" v-if="pendingTodos.length">
        <view class="todo-item" v-for="t in pendingTodos" :key="t.id" @tap="editTodo(t)">
          <view class="todo-main">
            <view class="todo-title-row">
              <text class="todo-title">{{ t.title }}</text>
            </view>
            <view class="todo-tags">
              <text class="imp-tag" :class="t.importance || 'normal'">{{ importanceLabel(t.importance) }}</text>
              <text class="time-tag" v-if="t.eventTime">🕐 {{ formatTime(t.eventTime) }}</text>
              <text class="remind-tag" v-if="t.remindEnabled == 1">🔔 {{ remindLabel(t.remindRule) }}</text>
              <text class="reminded-tag" v-if="t.remindEnabled == 1 && t.reminded == 1">已提醒</text>
            </view>
          </view>
          <view class="todo-btns">
            <view class="done-btn" @tap.stop="toggleTodo(t)">完成</view>
            <view class="del-btn" @tap.stop="delTodo(t)">删除</view>
          </view>
        </view>
      </view>
      <view class="empty-mini" v-else>暂无待办，点击右下角添加</view>
    </view>

    <!-- 已完成 -->
    <view class="done-section" v-if="doneTodos.length">
      <view class="sec-title">已完成 <text class="sec-count">{{ doneTodos.length }}</text></view>
      <view class="todo-list">
        <view class="todo-item done" v-for="t in doneTodos" :key="t.id">
          <view class="todo-main">
            <text class="todo-title">{{ t.title }}</text>
          </view>
          <view class="todo-btns">
            <view class="undo-btn" @tap.stop="toggleTodo(t)">取消完成</view>
            <view class="del-btn" @tap.stop="delTodo(t)">删除</view>
          </view>
        </view>
      </view>
    </view>

    <!-- 悬浮添加按钮 -->
    <view class="fab" @tap="openForm">＋</view>

    <!-- 添加/编辑弹层 -->
    <view class="form-mask" :class="{ 'kb-open': kbHeight > 0 }" v-if="formVisible" @tap="closeForm">
      <view class="form-panel" @tap.stop :style="kbMaxHeightStyle">
        <view class="form-title">{{ editingId ? '编辑待办' : '添加待办' }}</view>
        <!-- 不用 scroll-view：内层滚动会限制高度，长内容（如长备注）被从中间截断
             并出现独立滚动条，手机上要"在弹层里再滑一次"，体验差。
             改为普通容器由外层遮罩统一滚动（见 .form-mask）。 -->
        <view class="form-scroll">
          <view class="f-row">
            <text class="f-label">事项</text>
            <input class="f-input" v-model="form.title" placeholder="请输入事项标题，如：信用卡还款" maxlength="50" />
          </view>
          <view class="f-row col">
            <text class="f-label">内容</text>
            <textarea class="f-textarea" v-model="form.content" placeholder="补充内容（选填）" maxlength="200" />
          </view>
          <view class="f-row">
            <text class="f-label">时间</text>
            <CalendarPicker class="fill-picker" :value="form.date" @change="(e) => form.date = e.detail.value">
              <view class="f-value" :class="{ placeholder: !form.date }">{{ form.date || '选择日期' }}</view>
            </CalendarPicker>
            <view class="fill-picker" style="margin-left: 20rpx;" @tap="openTime">
              <view class="f-value" :class="{ placeholder: !form.time }">{{ form.time || '选择时间' }}</view>
            </view>
          </view>
          <view class="f-row">
            <text class="f-label">是否提醒</text>
            <view class="switch-group">
              <view class="switch-tag" :class="{ active: form.remindEnabled === 0 }" @tap="form.remindEnabled = 0">不提醒</view>
              <view class="switch-tag" :class="{ active: form.remindEnabled === 1 }" @tap="form.remindEnabled = 1">提醒</view>
            </view>
          </view>
          <view class="f-row" v-if="form.remindEnabled === 1">
            <text class="f-label">提醒方式</text>
            <view class="fill-picker" @tap="openPs('remind')">
              <view class="f-value">{{ remindLabel(form.remindRule) }} ›</view>
            </view>
          </view>
          <view class="f-row">
            <text class="f-label">重要程度</text>
            <view class="imp-group">
              <view class="imp-tag" :class="[{ active: form.importance === k }, k]" v-for="(lbl, k) in importanceMap" :key="k" @tap="form.importance = k">{{ lbl }}</view>
            </view>
          </view>
          <view class="f-tip" v-if="form.remindEnabled === 1 && !form.date">
            开启提醒需要先设置事项时间
          </view>
        </view>
        <view class="form-actions">
          <view class="f-btn cancel" @tap="closeForm">取消</view>
          <view class="f-btn ok" :class="{ disabled: submitting }" @tap="!submitting && saveTodo()">保存</view>
        </view>
      </view>
    </view>
    <PickerSheet
      :visible="ps.visible"
      :title="ps.title"
      :options="ps.options"
      :selected="ps.selected"
      @close="ps.visible = false"
      @select="onPsSelect"
    />

  
  <PickerWheel
    :visible="timeSheet"
    title="选择时间"
    :columns="[hours, minutes]"
    :value="timeDraft"
    @close="timeSheet = false"
    @confirm="onTimeConfirm"
  />

  

  </view>
</template>

<script>
import CalendarPicker from '@/components/CalendarPicker/CalendarPicker.vue';
import submitLock from '@/mixins/submitLock';
import keyboardAware from '@/mixins/keyboardAware';
import NavBar from '@/components/NavBar/NavBar.vue';
import PickerSheet from '@/components/PickerSheet/PickerSheet.vue';
import PickerWheel from '@/components/PickerWheel/PickerWheel.vue';
import { HOURS, MINUTES, pad2 } from '@/utils/timeOptions';
import { useThemeStore } from '@/stores/theme';
import { useUserStore } from '@/stores/user';
import { listMemos, createMemo, updateMemo, toggleMemo, deleteMemo } from '@/api/ledger';

const IMPORTANCE_MAP = { urgent: '紧急', high: '重要', normal: '一般', low: '不重要' };
const REMIND_KEYS = ['on_time', 'advance_30m', 'advance_1h', 'advance_2h', 'advance_1d', 'advance_1d_9am'];
const REMIND_LABELS = { on_time: '准时提醒', advance_30m: '提前30分钟', advance_1h: '提前1小时', advance_2h: '提前2小时', advance_1d: '提前1天', advance_1d_9am: '提前一天上午9点' };

import { loadDicts } from '@/utils/ledgerDict';

export default {
  mixins: [keyboardAware, submitLock],
  components: { PickerSheet, PickerWheel, CalendarPicker,  NavBar },
  data() {
    return {
      todos: [],
      formVisible: false,
      editingId: null,
      form: this.emptyForm(),
      importanceMap: IMPORTANCE_MAP,
      ps: { visible: false, key: '', title: '', options: [], selected: -1 },
      timeSheet: false,
      timeDraft: [0, 0],
      hours: HOURS,
      minutes: MINUTES,
      remindKeys: REMIND_KEYS,
      remindLabelsMap: { ...REMIND_LABELS },
      remindLabels: REMIND_KEYS.map(k => REMIND_LABELS[k])
    };
  },
  computed: {
    themeVars() { return useThemeStore().themeVars; },
    pendingTodos() { return this.todos.filter(t => !t.done); },
    doneTodos() { return this.todos.filter(t => t.done); },
    remindIndex() { return Math.max(0, REMIND_KEYS.indexOf(this.form.remindRule)); }
  },
  onShow() {
    this.loadRemindDict();
    useThemeStore().restore();
    if (useUserStore().isLoggedIn) this.load();
  },
  methods: {
    openPs(key) {
      this.ps = {
        visible: true,
        key,
        title: '提醒规则',
        options: this.remindLabels,
        selected: this.remindIndex
      };
    },
    openTime() {
      const parts = String(this.form.time || '08:00').split(':');
      this.timeDraft = [Number(parts[0]) || 0, Number(parts[1]) || 0];
      this.timeSheet = true;
    },
    onTimeConfirm(idx) {
      this.form.time = pad2(idx[0]) + ':' + pad2(idx[1]);
      this.timeSheet = false;
    },
    onPsSelect(idx) {
      this.form.remindRule = this.remindKeys[idx];
      this.ps.visible = false;
    },
    /** 提醒规则标签走后台字典（失败保留内置文案） */
    async loadRemindDict() {
      const d = await loadDicts(['ledger_remind_rule']);
      const items = d.ledger_remind_rule || [];
      if (!items.length) return;
      const map = { ...REMIND_LABELS };
      items.forEach(i => { map[i.value] = i.label; });
      this.remindLabelsMap = map;
      this.remindLabels = REMIND_KEYS.map(k => map[k]);
    },
    emptyForm() {
      const d = new Date();
      const pad = (n) => String(n).padStart(2, '0');
      return {
        title: '', content: '',
        date: '', time: '',
        remindEnabled: 0, remindRule: 'on_time', importance: 'normal'
      };
    },
    importanceLabel(v) { return IMPORTANCE_MAP[v || 'normal'] || v; },
    remindLabel(v) { return this.remindLabelsMap[v || 'on_time'] || REMIND_LABELS[v || 'on_time'] || v; },
    formatTime(v) {
      if (!v) return '';
      return String(v).replace('T', ' ').slice(5, 16);
    },
    async load() {
      try {
        const data = await listMemos() || {};
        this.todos = (data.records || []).map(t => ({ ...t, done: !!t.done }));
      } catch (e) { /* 拦截器已提示 */ }
    },
    openForm() {
      if (!useUserStore().isLoggedIn) {
        uni.showToast({ title: '请先登录后再操作', icon: 'none' });
        return;
      }
      this.editingId = null;
      this.form = this.emptyForm();
      this.formVisible = true;
    },
    editTodo(t) {
      this.editingId = t.id;
      const dt = t.eventTime ? String(t.eventTime).replace('T', ' ') : '';
      this.form = {
        title: t.title || '',
        content: t.content || '',
        date: dt ? dt.slice(0, 10) : '',
        time: dt ? dt.slice(11, 16) : '',
        remindEnabled: t.remindEnabled == 1 ? 1 : 0,
        remindRule: t.remindRule || 'on_time',
        importance: t.importance || 'normal'
      };
      this.formVisible = true;
    },
    closeForm() { this.formVisible = false; },
    async saveTodo() {
      return this.withSubmitLock(async () => {
              const title = this.form.title.trim();
              const content = this.form.content.trim();
              if (!title && !content) { uni.showToast({ title: '请输入事项或内容', icon: 'none' }); return; }
              if (this.form.remindEnabled === 1 && !this.form.date) {
                uni.showToast({ title: '开启提醒需设置事项时间', icon: 'none' });
                return;
              }
              const eventTime = this.form.date
                ? `${this.form.date} ${this.form.time || '09:00'}:00`
                : null;
              const payload = {
                title: title || null,
                content: content || null,
                eventTime,
                remindEnabled: this.form.remindEnabled,
                remindRule: this.form.remindEnabled === 1 ? this.form.remindRule : null,
                importance: this.form.importance
              };
              try {
                if (this.editingId) {
                  await updateMemo(this.editingId, payload);
                } else {
                  await createMemo(payload);
                }
                this.formVisible = false;
                uni.showToast({ title: '保存成功', icon: 'success' });
                this.load();
              } catch (e) { /* 拦截器已提示 */ }
      });
    },
    async toggleTodo(t) {
      try { await toggleMemo(t.id); this.load(); } catch (e) { }
    },
    delTodo(t) {
      uni.showModal({
        title: '提示', content: '删除「' + (t.title || t.content) + '」？',
        success: async (r) => {
          if (!r.confirm) return;
          try { await deleteMemo(t.id); this.load(); } catch (e) { }
        }
      });
    }
  }
};
</script>

<style scoped>
.page { padding-bottom: 160rpx; min-height: 100vh; background: var(--surface); }
.todo-section, .done-section { margin: 24rpx; }
.sec-title { font-size: 28rpx; font-weight: 600; color: var(--text-main); margin-bottom: 16rpx; }
.sec-count { font-size: 22rpx; color: var(--text-sub); font-weight: 400; margin-left: 8rpx; }

.todo-list { background: var(--card); border-radius: 16rpx; overflow: hidden; }
.todo-item {
  display: flex; align-items: flex-start; padding: 24rpx; border-bottom: 1rpx solid var(--border);
}
.todo-item:last-child { border-bottom: none; }
.todo-title { font-size: 28rpx; color: var(--text-main); font-weight: 600; flex: 1; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }
.todo-tags { display: flex; gap: 12rpx; flex-wrap: wrap; margin-top: 10rpx; align-items: center; }
.time-tag { font-size: 20rpx; color: var(--text-sub); background: var(--surface); padding: 2rpx 14rpx; border-radius: 14rpx; }
.todo-main { flex: 1; min-width: 0; display: flex; align-items: center; }
.todo-title-row { flex: 1; min-width: 0; display: flex; align-items: center; }
.todo-btns { display: flex; flex-direction: row; gap: 14rpx; flex-shrink: 0; margin-left: 16rpx; }
.done-btn {
  font-size: 22rpx; padding: 8rpx 28rpx; border-radius: 26rpx;
  background: var(--primary-strong); color: var(--on-primary);
}
.del-btn {
  font-size: 22rpx; padding: 8rpx 20rpx; border-radius: 26rpx;
  background: var(--expense-bg); color: var(--expense);
}
.undo-btn {
  font-size: 22rpx; padding: 8rpx 20rpx; border-radius: 26rpx;
  background: var(--surface); color: var(--text-sub); border: 1rpx solid var(--border);
}
.todo-item.done .todo-title { color: var(--text-muted); text-decoration: line-through; font-weight: 400; }
.todo-text { font-size: 24rpx; color: var(--text-sub); margin-top: 6rpx; display: block; }
.todo-meta { display: flex; gap: 16rpx; flex-wrap: wrap; margin-top: 10rpx; font-size: 20rpx; color: var(--text-sub); }
.remind-tag { color: var(--warning); }
.reminded-tag { color: var(--income); }
.imp-tag {
  font-size: 20rpx; padding: 2rpx 14rpx; border-radius: 14rpx; flex-shrink: 0;
}
.imp-tag.urgent { color: var(--on-expense); background: var(--expense); }
.imp-tag.high { color: var(--warning); background: var(--warning-bg); }
.imp-tag.normal { color: var(--text-sub); background: var(--surface); }
.imp-tag.low { color: var(--text-sub); background: var(--surface); }

.empty-mini { text-align: center; color: var(--text-muted); padding: 60rpx 0; font-size: 26rpx; }

.fab {
  position: fixed; right: 40rpx; bottom: calc(env(safe-area-inset-bottom) + 60rpx);
  width: 108rpx; height: 108rpx; border-radius: 54rpx;
  background: var(--primary-strong); color: var(--on-primary); font-size: 52rpx; line-height: 100rpx; text-align: center;
  box-shadow: 0 8rpx 24rpx rgba(0, 0, 0, 0.2);
}
.fab:active { transform: scale(0.95); }

.form-mask {
  /* 四边写法而非 inset：旧 WKWebView 不支持 inset 简写 */
  position: fixed; top: 0; right: 0; bottom: 0; left: 0;
  background: rgba(0,0,0,0.45);
  /* 高于 H5 底部 TabBar(998) 与官方弹窗(999)，否则遮不住 Tab 栏 */
  z-index: 999;
  display: flex; align-items: flex-start; justify-content: center;
  /* 遮罩自身可滚动 —— 弹层高度不再受视口限制，内容自然撑开，
     手机上就是"整屏上下滑动"，不再出现内层滚动条与内容被截断的问题。
     （原来弹层 max-height:80vh + 内层 max-height:55vh，长备注会被从中间切断） */
  overflow-y: auto;
  -webkit-overflow-scrolling: touch;
  padding: calc(var(--status-bar-height) + 40rpx) 0 calc(env(safe-area-inset-bottom) + 40rpx);
  box-sizing: border-box;
  transition: padding-bottom 0.18s ease;
}
.form-mask.kb-open { padding-bottom: 24rpx; }
.form-panel {
  width: 660rpx; background: var(--card); border-radius: 24rpx;
  padding: 32rpx; display: flex; flex-direction: column;
  /* margin:auto 撑开剩余空间：内容短时垂直居中，内容长时可完整滚动
     （比在 flex 容器上用 align-items:center 更安全 —— 后者在内容超高时会裁掉顶部） */
  margin: auto 0;
  flex-shrink: 0;
}
.form-title { font-size: 30rpx; font-weight: 600; text-align: center; margin-bottom: 20rpx; }
/* 高度由内容决定，不再限制（原 max-height:55vh 是截断的根源） */
.form-scroll { flex: 1; }
.f-row {
  display: flex; align-items: center; padding: 24rpx 0; border-bottom: 1rpx solid var(--border);
}
.f-row:last-child { border-bottom: none; }
.f-row.col { flex-direction: column; align-items: flex-start; gap: 16rpx; }
.f-label { width: 150rpx; font-size: 26rpx; color: var(--text-main); flex-shrink: 0; }
.f-row.col .f-label { width: auto; }
.f-input { flex: 1; font-size: 26rpx; }
.f-value { font-size: 26rpx; color: var(--text-main); }
.f-value.placeholder { color: var(--text-muted); }
/* 横向行（标签 + 值）的值统一右对齐，与 budget / profile / portfolio / 记一笔 等页一致。
   用 :not(.col) 限定 —— .col 是纵向布局（内容 textarea），右对齐不适用。 */
.f-row:not(.col) .f-value,
.f-row:not(.col) .f-input { flex: 1; text-align: right; }
.f-textarea {
  width: 100%; min-height: 100rpx; font-size: 26rpx; background: var(--surface);
  border-radius: 12rpx; padding: 16rpx; box-sizing: border-box;
}
.switch-group { display: flex; gap: 16rpx; }
.switch-tag {
  font-size: 24rpx; padding: 8rpx 28rpx; border-radius: 28rpx;
  background: var(--surface); color: var(--text-sub);
}
.switch-tag.active { background: var(--primary-strong); color: var(--on-primary); }
.imp-group { flex: 1; display: flex; gap: 12rpx; justify-content: flex-end; flex-wrap: wrap; }
.imp-tag.active { outline: 2rpx solid var(--primary-strong); }
.f-tip { font-size: 22rpx; color: var(--warning); padding: 16rpx 0; }
.form-actions { display: flex; gap: 20rpx; margin-top: 24rpx; }
.f-btn {
  flex: 1; text-align: center; padding: 18rpx; border-radius: 36rpx; font-size: 28rpx;
}
.f-btn.cancel { background: var(--surface); color: var(--text-sub); }
.f-btn.ok { background: var(--primary-strong); color: var(--on-primary); }
/* 提交锁生效时给出视觉反馈（与全局 .btn-primary.disabled 一致） */
.f-btn.disabled { opacity: 0.5; }
</style>
