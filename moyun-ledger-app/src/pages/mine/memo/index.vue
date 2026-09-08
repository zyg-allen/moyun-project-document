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
    <view class="form-mask" v-if="formVisible" @tap="closeForm">
      <view class="form-panel" @tap.stop>
        <view class="form-title">{{ editingId ? '编辑待办' : '添加待办' }}</view>
        <scroll-view scroll-y class="form-scroll">
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
            <picker mode="date" :value="form.date" @change="(e) => form.date = e.detail.value">
              <view class="f-value" :class="{ placeholder: !form.date }">{{ form.date || '选择日期' }}</view>
            </picker>
            <picker mode="time" :value="form.time" @change="(e) => form.time = e.detail.value" style="margin-left: 20rpx;">
              <view class="f-value" :class="{ placeholder: !form.time }">{{ form.time || '选择时间' }}</view>
            </picker>
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
            <picker :range="remindLabels" :value="remindIndex" @change="(e) => form.remindRule = remindKeys[e.detail.value]">
              <view class="f-value">{{ remindLabel(form.remindRule) }} ›</view>
            </picker>
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
        </scroll-view>
        <view class="form-actions">
          <view class="f-btn cancel" @tap="closeForm">取消</view>
          <view class="f-btn ok" @tap="saveTodo">保存</view>
        </view>
      </view>
    </view>
  </view>
</template>

<script>
import NavBar from '@/components/NavBar/NavBar.vue';
import { useThemeStore } from '@/stores/theme';
import { useUserStore } from '@/stores/user';
import { listMemos, createMemo, updateMemo, toggleMemo, deleteMemo } from '@/api/ledger';

const IMPORTANCE_MAP = { urgent: '紧急', high: '重要', normal: '一般', low: '不重要' };
const REMIND_KEYS = ['on_time', 'advance_30m', 'advance_1h', 'advance_2h', 'advance_1d', 'advance_1d_9am'];
const REMIND_LABELS = { on_time: '准时提醒', advance_30m: '提前30分钟', advance_1h: '提前1小时', advance_2h: '提前2小时', advance_1d: '提前1天', advance_1d_9am: '提前一天上午9点' };

export default {
  components: { NavBar },
  data() {
    return {
      todos: [],
      formVisible: false,
      editingId: null,
      form: this.emptyForm(),
      importanceMap: IMPORTANCE_MAP,
      remindKeys: REMIND_KEYS,
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
    useThemeStore().restore();
    if (useUserStore().isLoggedIn) this.load();
  },
  methods: {
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
    remindLabel(v) { return REMIND_LABELS[v || 'on_time'] || v; },
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
        uni.showToast({ title: '请先在「我的」页登录', icon: 'none' });
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
.page { padding-bottom: 160rpx; min-height: 100vh; background: #f5f6f8; }
.todo-section, .done-section { margin: 24rpx; }
.sec-title { font-size: 28rpx; font-weight: 600; color: #333; margin-bottom: 16rpx; }
.sec-count { font-size: 22rpx; color: #999; font-weight: 400; margin-left: 8rpx; }

.todo-list { background: #fff; border-radius: 16rpx; overflow: hidden; }
.todo-item {
  display: flex; align-items: flex-start; padding: 24rpx; border-bottom: 1rpx solid #f5f5f7;
}
.todo-item:last-child { border-bottom: none; }
.todo-title { font-size: 28rpx; color: #333; font-weight: 600; flex: 1; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }
.todo-tags { display: flex; gap: 12rpx; flex-wrap: wrap; margin-top: 10rpx; align-items: center; }
.time-tag { font-size: 20rpx; color: #666; background: #f5f6f8; padding: 2rpx 14rpx; border-radius: 14rpx; }
.todo-main { flex: 1; min-width: 0; display: flex; align-items: center; }
.todo-title-row { flex: 1; min-width: 0; display: flex; align-items: center; }
.todo-btns { display: flex; flex-direction: row; gap: 14rpx; flex-shrink: 0; margin-left: 16rpx; }
.done-btn {
  font-size: 22rpx; padding: 8rpx 28rpx; border-radius: 26rpx;
  background: var(--primary-strong); color: #fff;
}
.del-btn {
  font-size: 22rpx; padding: 8rpx 20rpx; border-radius: 26rpx;
  background: #fff1f0; color: #e57373;
}
.undo-btn {
  font-size: 22rpx; padding: 8rpx 20rpx; border-radius: 26rpx;
  background: #f5f6f8; color: #666; border: 1rpx solid #e5e6e8;
}
.todo-item.done .todo-title { color: #bbb; text-decoration: line-through; font-weight: 400; }
.todo-text { font-size: 24rpx; color: #666; margin-top: 6rpx; display: block; }
.todo-meta { display: flex; gap: 16rpx; flex-wrap: wrap; margin-top: 10rpx; font-size: 20rpx; color: #999; }
.remind-tag { color: #d48806; }
.reminded-tag { color: #52c41a; }
.imp-tag {
  font-size: 20rpx; padding: 2rpx 14rpx; border-radius: 14rpx; flex-shrink: 0;
}
.imp-tag.urgent { color: #fff; background: #e57373; }
.imp-tag.high { color: #d48806; background: #fff7e6; }
.imp-tag.normal { color: #666; background: #f5f5f5; }
.imp-tag.low { color: #999; background: #f5f5f5; }

.empty-mini { text-align: center; color: #bbb; padding: 60rpx 0; font-size: 26rpx; }

.fab {
  position: fixed; right: 40rpx; bottom: calc(env(safe-area-inset-bottom) + 60rpx);
  width: 108rpx; height: 108rpx; border-radius: 54rpx;
  background: var(--primary-strong); color: #fff; font-size: 52rpx; line-height: 100rpx; text-align: center;
  box-shadow: 0 8rpx 24rpx rgba(0, 0, 0, 0.2);
}
.fab:active { transform: scale(0.95); }

.form-mask {
  position: fixed; inset: 0; background: rgba(0,0,0,0.45); z-index: 99;
  display: flex; align-items: center; justify-content: center;
}
.form-panel {
  width: 660rpx; max-height: 80vh; background: #fff; border-radius: 24rpx;
  padding: 32rpx; display: flex; flex-direction: column;
}
.form-title { font-size: 30rpx; font-weight: 600; text-align: center; margin-bottom: 20rpx; }
.form-scroll { max-height: 55vh; }
.f-row {
  display: flex; align-items: center; padding: 24rpx 0; border-bottom: 1rpx solid #f5f5f7;
}
.f-row:last-child { border-bottom: none; }
.f-row.col { flex-direction: column; align-items: flex-start; gap: 16rpx; }
.f-label { width: 150rpx; font-size: 26rpx; color: #333; flex-shrink: 0; }
.f-row.col .f-label { width: auto; }
.f-input { flex: 1; font-size: 26rpx; }
.f-value { font-size: 26rpx; color: #333; }
.f-value.placeholder { color: #bbb; }
.f-textarea {
  width: 100%; min-height: 100rpx; font-size: 26rpx; background: #f8f9fa;
  border-radius: 12rpx; padding: 16rpx; box-sizing: border-box;
}
.switch-group { display: flex; gap: 16rpx; }
.switch-tag {
  font-size: 24rpx; padding: 8rpx 28rpx; border-radius: 28rpx;
  background: #f5f5f5; color: #666;
}
.switch-tag.active { background: var(--primary-strong); color: #fff; }
.imp-group { flex: 1; display: flex; gap: 12rpx; justify-content: flex-end; flex-wrap: wrap; }
.imp-tag.active { outline: 2rpx solid var(--primary-strong); }
.f-tip { font-size: 22rpx; color: #d48806; padding: 16rpx 0; }
.form-actions { display: flex; gap: 20rpx; margin-top: 24rpx; }
.f-btn {
  flex: 1; text-align: center; padding: 18rpx; border-radius: 36rpx; font-size: 28rpx;
}
.f-btn.cancel { background: #f5f6f8; color: #666; }
.f-btn.ok { background: var(--primary-strong); color: #fff; }
</style>
