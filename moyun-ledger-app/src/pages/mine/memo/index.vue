<template>
  <view class="page" :style="themeVars">
    <NavBar title="备忘录" />

    <!-- 待办列表 -->
    <view class="todo-section">
      <view class="sec-title">待办事项 <text class="sec-count">{{ pendingTodos.length }}</text></view>
      <view class="todo-list" v-if="pendingTodos.length">
        <view class="todo-item" v-for="t in pendingTodos" :key="t.id">
          <view class="todo-check" @tap="toggleTodo(t)">
            <text v-if="t.done">✓</text>
          </view>
          <view class="todo-content" @tap="editTodo(t)">
            <text class="todo-text">{{ t.text }}</text>
            <text class="todo-date" v-if="t.date">{{ t.date }}</text>
          </view>
          <view class="todo-del" @tap="delTodo(t)">×</view>
        </view>
      </view>
      <view class="empty-mini" v-else>暂无待办，添加一个吧</view>
    </view>

    <!-- 已完成 -->
    <view class="done-section" v-if="doneTodos.length">
      <view class="sec-title">已完成 <text class="sec-count">{{ doneTodos.length }}</text></view>
      <view class="todo-list">
        <view class="todo-item done" v-for="t in doneTodos" :key="t.id">
          <view class="todo-check checked" @tap="toggleTodo(t)"><text>✓</text></view>
          <view class="todo-content">
            <text class="todo-text">{{ t.text }}</text>
          </view>
          <view class="todo-del" @tap="delTodo(t)">×</view>
        </view>
      </view>
    </view>

    <!-- 添加输入栏 -->
    <view class="add-bar">
      <input class="add-input" v-model="newText" placeholder="添加待办事项..." confirm-type="done" @confirm="addTodo" />
      <view class="add-btn" @tap="addTodo">添加</view>
    </view>
  </view>
</template>

<script>
import NavBar from '@/components/NavBar/NavBar.vue';
import { useThemeStore } from '@/stores/theme';
import { storage, uid } from '@/utils/storage';

export default {
  components: { NavBar },
  data() { return { todos: [], newText: '' }; },
  computed: {
    themeVars() { return useThemeStore().themeVars; },
    pendingTodos() { return this.todos.filter(t => !t.done); },
    doneTodos() { return this.todos.filter(t => t.done); }
  },
  onShow() {
    useThemeStore().restore();
    this.todos = storage.get('memo_todos', []) || [];
  },
  methods: {
    addTodo() {
      const text = this.newText.trim();
      if (!text) return;
      this.todos.unshift({
        id: uid(), text, done: false,
        date: new Date().toISOString().slice(0, 10)
      });
      this.newText = '';
      this.save();
    },
    toggleTodo(t) { t.done = !t.done; this.save(); },
    editTodo(t) {
      uni.showModal({
        title: '编辑待办', editable: true, placeholderText: t.text,
        success: (r) => {
          if (r.confirm && r.content) { t.text = r.content.trim(); this.save(); }
        }
      });
    },
    delTodo(t) {
      this.todos = this.todos.filter(x => x.id !== t.id);
      this.save();
    },
    save() { storage.set('memo_todos', this.todos); }
  }
};
</script>

<style scoped>
.page { padding-bottom: 140rpx; min-height: 100vh; background: #f5f6f8; }
.todo-section, .done-section { margin: 24rpx; }
.sec-title { font-size: 28rpx; font-weight: 600; color: #333; margin-bottom: 16rpx; }
.sec-count { font-size: 22rpx; color: #999; font-weight: 400; margin-left: 8rpx; }

.todo-list { background: #fff; border-radius: 16rpx; overflow: hidden; }
.todo-item {
  display: flex; align-items: center; padding: 24rpx; border-bottom: 1rpx solid #f5f5f7;
}
.todo-item:last-child { border-bottom: none; }
.todo-check {
  width: 40rpx; height: 40rpx; border-radius: 8rpx; border: 2rpx solid #ccc;
  display: flex; align-items: center; justify-content: center;
  color: #fff; font-size: 24rpx; margin-right: 20rpx; flex-shrink: 0;
}
.todo-check.checked { background: var(--primary-strong); border-color: var(--primary-strong); }
.todo-content { flex: 1; min-width: 0; }
.todo-text { font-size: 28rpx; color: #333; }
.todo-item.done .todo-text { color: #bbb; text-decoration: line-through; }
.todo-date { font-size: 20rpx; color: #bbb; margin-top: 4rpx; display: block; }
.todo-del {
  width: 48rpx; height: 48rpx; line-height: 44rpx; text-align: center;
  color: #ccc; font-size: 36rpx;
}

.empty-mini { text-align: center; color: #bbb; padding: 60rpx 0; font-size: 26rpx; }

.add-bar {
  position: fixed; bottom: 0; left: 0; right: 0;
  display: flex; align-items: center; padding: 16rpx 24rpx;
  background: #fff; border-top: 1rpx solid #eee;
  padding-bottom: calc(env(safe-area-inset-bottom) + 16rpx);
}
.add-input {
  flex: 1; height: 72rpx; background: #f5f6f8;
  border-radius: 36rpx; padding: 0 28rpx; font-size: 26rpx;
}
.add-btn {
  margin-left: 16rpx; padding: 16rpx 32rpx;
  background: var(--primary-strong); color: #fff; border-radius: 36rpx; font-size: 26rpx;
}
</style>
