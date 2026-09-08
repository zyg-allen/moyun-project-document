<template>
  <view class="page" :style="themeVars">
    <NavBar title="定时记账" />

    <!-- 汇总 -->
    <view class="stat-bar" v-if="tasks.length">
      <text>共 {{ tasks.length }} 个任务 · 启用中 {{ enabledCount }} 个</text>
    </view>

    <!-- 任务列表 -->
    <view class="task-list" v-if="tasks.length">
      <view class="task-card" v-for="t in tasks" :key="t.id">
        <view class="t-head">
          <view class="t-name">{{ t.name }}</view>
          <view class="t-switch" :class="{ off: !t.enabled }" @tap="toggle(t)">{{ t.enabled ? '已启用' : '已停用' }}</view>
        </view>
        <view class="t-meta">
          <text class="t-amount" :class="t.type">{{ t.type === 'income' ? '+' : '-' }}¥{{ toFixedYuan(t.amount) }}</text>
          <text class="t-cycle">{{ cycleText(t) }}</text>
        </view>
        <view class="t-info">
          <text v-if="t.categoryName">分类：{{ t.categoryName }}</text>
          <text v-if="t.accountName">账户：{{ t.accountName }}</text>
        </view>
        <view class="t-info next">
          <text v-if="t.enabled && t.nextExecDate">下次记账：{{ t.nextExecDate }} {{ t.execTime }}</text>
          <text v-else-if="!t.enabled">已停用，不会再自动记账</text>
          <text v-else>已完成</text>
        </view>
        <view class="t-actions">
          <view class="t-btn" @tap="runNow(t)">立即执行</view>
          <view class="t-btn warn" @tap="showLogs(t)">执行日志</view>
          <view class="t-btn danger" @tap="del(t)">删除</view>
        </view>
      </view>
    </view>

    <!-- 空状态 -->
    <view class="empty" v-else>
      <text class="empty-icon">⏰</text>
      <text class="empty-text">暂无数据</text>
      <text class="empty-sub">添加定时计划，到点自动帮你记账</text>
    </view>

    <!-- 悬浮添加按钮 -->
    <view class="fab" @tap="goAdd">＋</view>

    <!-- 日志弹层 -->
    <view class="log-mask" v-if="logsVisible" @tap="logsVisible = false">
      <view class="log-panel" @tap.stop>
        <view class="log-title">执行日志</view>
        <scroll-view scroll-y class="log-scroll">
          <view class="log-empty" v-if="!logs.length">暂无执行记录</view>
          <view class="log-item" v-for="l in logs" :key="l.id">
            <view class="log-line">
              <text class="log-date">{{ l.execDate }}</text>
              <text class="log-status" :class="l.status === 1 ? 'ok' : 'fail'">{{ l.status === 1 ? '成功' : '失败' }}</text>
              <text class="log-amount">¥{{ toFixedYuan(l.amount) }}</text>
            </view>
            <view class="log-reason" v-if="l.status === 2">
              <text>{{ l.failReason || '未知原因' }}</text>
              <text class="log-retry" @tap="retry(l)">重试</text>
            </view>
          </view>
        </scroll-view>
        <view class="log-close" @tap="logsVisible = false">关闭</view>
      </view>
    </view>
  </view>
</template>

<script>
import NavBar from '@/components/NavBar/NavBar.vue';
import { useThemeStore } from '@/stores/theme';
import { useUserStore } from '@/stores/user';
import { listScheduleTasks, listScheduleLogs, toggleScheduleTask, runScheduleTask, retryScheduleLog, deleteScheduleTask } from '@/api/ledger';
import { toFixedYuan } from '@/utils/money';

const CYCLE_LABELS = { daily: '每天', weekly: '每周', monthly: '每月', interval: '每N天' };
const WEEK_LABELS = ['', '一', '二', '三', '四', '五', '六', '日'];

export default {
  components: { NavBar },
  data() {
    return { tasks: [], enabledCount: 0, logs: [], logsVisible: false };
  },
  computed: {
    themeVars() { return useThemeStore().themeVars; }
  },
  onShow() {
    useThemeStore().restore();
    if (useUserStore().isLoggedIn) this.load();
  },
  methods: {
    toFixedYuan,
    cycleText(t) {
      if (t.cycle === 'weekly') return '每周' + (WEEK_LABELS[t.dayOfWeek] || '');
      if (t.cycle === 'monthly') return '每月' + t.dayOfMonth + '号';
      if (t.cycle === 'interval') return '每' + t.intervalDays + '天';
      return '每天';
    },
    async load() {
      try {
        const data = await listScheduleTasks() || {};
        this.tasks = data.tasks || [];
        this.enabledCount = data.enabledCount || 0;
      } catch (e) { /* 拦截器已提示 */ }
    },
    async toggle(t) {
      try { await toggleScheduleTask(t.id); this.load(); } catch (e) { }
    },
    async runNow(t) {
      uni.showModal({
        title: '立即执行', content: '按下次记账日立即记一笔？',
        success: async (r) => {
          if (!r.confirm) return;
          try {
            await runScheduleTask(t.id);
            uni.showToast({ title: '执行成功', icon: 'success' });
            this.load();
          } catch (e) { this.load(); }
        }
      });
    },
    async showLogs(t) {
      try {
        const data = await listScheduleLogs(t.id) || {};
        this.logs = data.records || [];
        this.logsVisible = true;
      } catch (e) { }
    },
    async retry(l) {
      try {
        await retryScheduleLog(l.id);
        uni.showToast({ title: '重试成功', icon: 'success' });
        this.logs = this.logs.map(x => x.id === l.id ? { ...x, status: 1 } : x);
        this.load();
      } catch (e) { }
    },
    del(t) {
      uni.showModal({
        title: '删除任务', content: '删除「' + t.name + '」？已生成的流水不受影响',
        success: async (r) => {
          if (!r.confirm) return;
          try { await deleteScheduleTask(t.id); this.load(); } catch (e) { }
        }
      });
    },
    goAdd() { uni.navigateTo({ url: '/pages/mine/schedule/add' }); }
  }
};
</script>

<style scoped>
.page { padding-bottom: 160rpx; min-height: 100vh; background: #f5f6f8; }
.stat-bar { padding: 24rpx 32rpx 0; font-size: 24rpx; color: #999; }

.task-list { padding: 0 24rpx; }
.task-card { background: #fff; border-radius: 20rpx; padding: 28rpx; margin-top: 20rpx; }
.t-head { display: flex; justify-content: space-between; align-items: center; }
.t-name { font-size: 28rpx; font-weight: 600; color: #333; }
.t-switch {
  font-size: 22rpx; padding: 6rpx 20rpx; border-radius: 24rpx;
  background: var(--primary-soft); color: var(--primary-strong);
}
.t-switch.off { background: #f5f5f5; color: #bbb; }
.t-meta { display: flex; justify-content: space-between; align-items: center; margin-top: 16rpx; }
.t-amount { font-size: 34rpx; font-weight: 700; }
.t-amount.income { color: #52c41a; }
.t-amount.expense { color: #e57373; }
.t-cycle { font-size: 22rpx; color: var(--primary-strong); background: var(--primary-soft); padding: 4rpx 16rpx; border-radius: 16rpx; }
.t-info { font-size: 22rpx; color: #999; margin-top: 10rpx; display: flex; gap: 24rpx; flex-wrap: wrap; }
.t-info.next { color: #666; }
.t-actions { display: flex; gap: 16rpx; margin-top: 20rpx; justify-content: flex-end; }
.t-btn {
  font-size: 22rpx; padding: 8rpx 24rpx; border-radius: 26rpx;
  background: var(--primary-soft); color: var(--primary-strong);
}
.t-btn.warn { background: #fff7e6; color: #d48806; }
.t-btn.danger { background: #fff1f0; color: #e57373; }

.empty { display: flex; flex-direction: column; align-items: center; padding: 160rpx 0; }
.empty-icon { font-size: 100rpx; opacity: 0.5; margin-bottom: 24rpx; }
.empty-text { font-size: 28rpx; color: #999; }
.empty-sub { font-size: 22rpx; color: #bbb; margin-top: 12rpx; }

.fab {
  position: fixed; right: 40rpx; bottom: calc(env(safe-area-inset-bottom) + 60rpx);
  width: 108rpx; height: 108rpx; border-radius: 54rpx;
  background: #ff9f43; color: #fff; font-size: 52rpx; line-height: 100rpx; text-align: center;
  box-shadow: 0 8rpx 24rpx rgba(255, 159, 67, 0.4);
}
.fab:active { transform: scale(0.95); }

.log-mask {
  position: fixed; inset: 0; background: rgba(0,0,0,0.45); z-index: 99;
  display: flex; align-items: center; justify-content: center;
}
.log-panel {
  width: 640rpx; max-height: 70vh; background: #fff; border-radius: 24rpx;
  padding: 32rpx; display: flex; flex-direction: column;
}
.log-title { font-size: 30rpx; font-weight: 600; text-align: center; margin-bottom: 20rpx; }
.log-scroll { max-height: 50vh; }
.log-item { padding: 16rpx 0; border-bottom: 1rpx solid #f5f5f7; }
.log-line { display: flex; justify-content: space-between; align-items: center; font-size: 24rpx; }
.log-date { color: #666; }
.log-status.ok { color: #52c41a; }
.log-status.fail { color: #e57373; }
.log-amount { color: #333; font-weight: 600; }
.log-reason { display: flex; justify-content: space-between; align-items: center; font-size: 22rpx; color: #e57373; margin-top: 8rpx; }
.log-retry { color: var(--primary-strong); padding: 4rpx 16rpx; background: var(--primary-soft); border-radius: 18rpx; }
.log-empty { text-align: center; color: #bbb; padding: 60rpx 0; font-size: 26rpx; }
.log-close {
  margin-top: 24rpx; text-align: center; padding: 18rpx;
  background: #f5f6f8; border-radius: 36rpx; font-size: 26rpx; color: #666;
}
</style>