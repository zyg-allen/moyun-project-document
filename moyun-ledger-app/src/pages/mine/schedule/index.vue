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
        <!-- ===== 信息区（上） ===== -->
        <view class="t-head">
          <view class="t-name">{{ t.name }}</view>
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

        <!-- ===== 操作区（下） =====
             开关置于最前（放「立即执行」之前），其后依次为 立即执行 / 查看 / 编辑 / 删除 -->
        <view class="t-actions">
          <switch
            class="t-switchbox"
            :checked="t.enabled === 1"
            :color="primaryStrong"
            @change="onSwitchChange(t, $event)"
          />
          <view class="t-btn" @tap="runNow(t)">立即执行</view>
          <view class="t-btn warn" @tap="showLogs(t)">查看</view>
          <view class="t-btn" @tap="goEdit(t)">编辑</view>
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

    <!-- 日志弹层：列表按记账日倒序，后端 LIMIT 50（只取最近记录，避免长年任务无限拉取） -->
    <view class="log-mask" v-if="logsVisible" @tap="logsVisible = false">
      <view class="log-panel" @tap.stop>
        <view class="log-title">{{ logTitle }}</view>
        <view class="log-hint">按记账日倒序 · 仅显示最近 {{ LOG_LIMIT }} 条</view>
        <scroll-view scroll-y class="log-scroll">
          <view class="log-empty" v-if="!logs.length">暂无执行记录</view>
          <view class="log-item" v-for="l in logs" :key="l.id">
            <view class="log-line">
              <text class="log-date">{{ l.execDate }}</text>
              <text class="log-status" :class="l.status === 1 ? 'ok' : 'fail'">{{ l.status === 1 ? '成功' : '失败' }}</text>
              <text class="log-amount">¥{{ toFixedYuan(l.amount) }}</text>
            </view>
            <view class="log-reason" v-if="l.status === 2">
              <text class="log-fail">{{ l.failReason || '未知原因' }}</text>
              <text class="log-retry" @tap="retry(l)">重试</text>
            </view>
            <view class="log-sub" v-else-if="l.retryCount">
              <text>重试 {{ l.retryCount }} 次后成功</text>
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

const WEEK_LABELS = ['', '一', '二', '三', '四', '五', '六', '日'];

export default {
  components: { NavBar },
  data() {
    return {
      tasks: [],
      enabledCount: 0,
      logs: [],
      logsVisible: false,
      logTaskName: '',
      // 与后端 listLogs 的 LIMIT 50 保持一致：日志列表只取最近记录
      LOG_LIMIT: 50
    };
  },
  computed: {
    themeVars() { return useThemeStore().themeVars; },
    /** uni switch 的 color 需要具体色值（不走 CSS 变量求值），故从主题取强调色的实值 */
    primaryStrong() { return useThemeStore().theme['--primary-strong']; },
    logTitle() { return this.logTaskName ? ('执行日志 · ' + this.logTaskName) : '执行日志'; }
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
      try { await toggleScheduleTask(t.id); } catch (e) { /* 拦截器已提示 */ }
      // 无论成败都以服务端为准重新拉取：否则开关的显示会和实际启用状态不一致
      this.load();
    },
    /** 开关变更：与当前状态一致则忽略（避免重复请求），否则走 toggle 接口 */
    onSwitchChange(t, e) {
      const want = !!e.detail.value;
      if (want === (t.enabled === 1)) return;
      this.toggle(t);
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
        this.logTaskName = t.name;
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
    goAdd() { uni.navigateTo({ url: '/pages/mine/schedule/add' }); },
    /** 编辑：把任务 id 传给添加页，该页 onLoad 收到 id 后进入编辑模式并回填 */
    goEdit(t) { uni.navigateTo({ url: '/pages/mine/schedule/add?id=' + t.id }); }
  }
};
</script>

<style scoped>
.page { padding-bottom: 160rpx; min-height: 100vh; background: var(--surface); }
.stat-bar { padding: 24rpx 32rpx 0; font-size: 24rpx; color: var(--text-sub); }

.task-list { padding: 0 24rpx; }
.task-card { background: var(--card); border-radius: 20rpx; padding: 28rpx; margin-top: 20rpx; }
.t-head { display: flex; justify-content: space-between; align-items: center; }
.t-name { font-size: 28rpx; font-weight: 600; color: var(--text-main); }
.t-meta { display: flex; justify-content: space-between; align-items: center; margin-top: 16rpx; }
.t-amount { font-size: 34rpx; font-weight: 700; }
.t-amount.income { color: var(--income); }
.t-amount.expense { color: var(--expense); }
.t-cycle { font-size: 22rpx; color: var(--primary-strong); background: var(--primary-soft); padding: 4rpx 16rpx; border-radius: 16rpx; }
.t-info { font-size: 22rpx; color: var(--text-sub); margin-top: 10rpx; display: flex; gap: 24rpx; flex-wrap: wrap; }
.t-info.next { color: var(--text-sub); }
/* 操作区（下）：与上方信息区用分隔线隔开；开关 + 4 个按钮，
   375px 屏实测约 295px < 可用 323px，窄屏则由 flex-wrap 自动折行 */
.t-actions {
  display: flex; align-items: center; flex-wrap: wrap;
  gap: 16rpx; margin-top: 20rpx; padding-top: 20rpx;
  border-top: 1rpx solid var(--border);
}
.t-switchbox {
  /* uni switch 默认 52×32px，且内部 .uni-switch-input 自带 5px 右外边距；
     这里抵消掉，使开关与按钮的间距和 gap 保持一致 */
  margin-right: -10rpx;
}
.t-btn {
  font-size: 22rpx; padding: 8rpx 24rpx; border-radius: 26rpx;
  background: var(--primary-soft); color: var(--primary-strong);
}
.t-btn.warn { background: var(--warning-bg); color: var(--warning); }
.t-btn.danger { background: var(--expense-bg); color: var(--expense); }

.empty { display: flex; flex-direction: column; align-items: center; padding: 160rpx 0; }
.empty-icon { font-size: 100rpx; opacity: 0.5; margin-bottom: 24rpx; }
.empty-text { font-size: 28rpx; color: var(--text-sub); }
.empty-sub { font-size: 22rpx; color: var(--text-muted); margin-top: 12rpx; }

.fab {
  position: fixed; right: 40rpx; bottom: calc(env(safe-area-inset-bottom) + 60rpx);
  width: 108rpx; height: 108rpx; border-radius: 54rpx;
  background: #ff9f43; color: #fff; font-size: 52rpx; line-height: 100rpx; text-align: center;
  box-shadow: 0 8rpx 24rpx rgba(255, 159, 67, 0.4);
}
.fab:active { transform: scale(0.95); }

.log-mask {
  position: fixed; top: 0; right: 0; bottom: 0; left: 0; background: rgba(0,0,0,0.45); z-index: 999;
  display: flex; align-items: center; justify-content: center;
}
.log-panel {
  width: 640rpx; max-height: 70vh; background: var(--card); border-radius: 24rpx;
  padding: 32rpx; display: flex; flex-direction: column;
}
.log-title { font-size: 30rpx; font-weight: 600; text-align: center; }
.log-hint { font-size: 20rpx; color: var(--text-muted); text-align: center; margin: 6rpx 0 20rpx; }
.log-scroll { max-height: 50vh; }
.log-item { padding: 16rpx 0; border-bottom: 1rpx solid #f5f5f7; }
.log-line { display: flex; justify-content: space-between; align-items: center; font-size: 24rpx; }
.log-date { color: var(--text-sub); }
.log-status.ok { color: var(--income); }
.log-status.fail { color: var(--expense); }
.log-amount { color: var(--text-main); font-weight: 600; }
.log-reason { display: flex; justify-content: space-between; align-items: center; font-size: 22rpx; color: var(--expense); margin-top: 8rpx; }
.log-fail { flex: 1; min-width: 0; margin-right: 16rpx; }
.log-sub { font-size: 20rpx; color: var(--text-muted); margin-top: 6rpx; }
.log-retry { color: var(--primary-strong); padding: 12rpx 20rpx; background: var(--primary-soft); border-radius: 8rpx; font-size: 22rpx; min-height: 88rpx; display: inline-flex; align-items: center; }
.log-empty { text-align: center; color: var(--text-muted); padding: 60rpx 0; font-size: 26rpx; }
.log-close {
  margin-top: 24rpx; text-align: center; padding: 18rpx;
  background: var(--surface); border-radius: 36rpx; font-size: 26rpx; color: var(--text-sub);
}
</style>
