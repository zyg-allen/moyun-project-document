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
          <view class="t-btn warn" @tap="showLogs(t)">执行日志</view>
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

    <!-- 执行日志弹层
         列表只展示「当月」记录：后端已 LIMIT 50 且按记账日倒序，
         而当月最多 31 条，必然落在这 50 条内，故前端按月份前缀过滤即可，无需改后端。
         点击某条进入详情，详情态由 logDetail 切换（标题栏出现「返回」）。 -->
    <view class="log-mask" v-if="logsVisible" @tap="closeLogs">
      <view class="log-panel" @tap.stop>
        <!-- 标题栏 -->
        <view class="log-head">
          <text class="log-back" v-if="logDetail" @tap="logDetail = null">‹ 返回</text>
          <text class="log-title">{{ logDetail ? '执行详情' : '执行日志' }}</text>
          <text class="log-x" @tap="closeLogs">✕</text>
        </view>

        <!-- 列表态：当月日志，按记账日倒序 -->
        <template v-if="!logDetail">
          <view class="log-hint">{{ logTaskName }} · {{ monthLabel }} · 共 {{ monthLogs.length }} 条</view>
          <scroll-view scroll-y class="log-scroll">
            <view class="log-empty" v-if="!monthLogs.length">{{ monthLabel }}暂无执行记录</view>
            <view class="log-item" v-for="l in monthLogs" :key="l.id" @tap="logDetail = l">
              <view class="log-line">
                <text class="log-date">{{ l.execDate }}</text>
                <text class="log-status" :class="l.status === 1 ? 'ok' : 'fail'">{{ l.status === 1 ? '成功' : '失败' }}</text>
                <text class="log-amount">¥{{ toFixedYuan(l.amount) }}</text>
                <text class="log-more">›</text>
              </view>
              <view class="log-reason" v-if="l.status === 2">
                <text class="log-fail">{{ l.failReason || '未知原因' }}</text>
                <text class="log-retry" @tap.stop="retry(l)">重试</text>
              </view>
              <view class="log-sub" v-else-if="l.retryCount">
                <text>重试 {{ l.retryCount }} 次后成功</text>
              </view>
            </view>
          </scroll-view>
        </template>

        <!-- 详情态 -->
        <scroll-view scroll-y class="log-scroll" v-else>
          <!-- 任务侧信息：日志表本身只存金额/状态/失败原因，类型与账户来自任务，
               这里一并展示，才能说清「钱从哪个账户扣、扣了多少、为什么」 -->
          <view class="d-row">
            <text class="d-k">类型</text>
            <text class="d-v">{{ logTask ? typeText(logTask.type) : '—' }}</text>
          </view>
          <view class="d-row">
            <text class="d-k">{{ logTask && logTask.type === 'income' ? '入账账户' : '扣款账户' }}</text>
            <text class="d-v">{{ (logTask && logTask.accountName) || '—' }}</text>
          </view>
          <view class="d-row" v-if="logTask && logTask.categoryName">
            <text class="d-k">分类</text>
            <text class="d-v">{{ logTask.categoryName }}</text>
          </view>
          <view class="d-row">
            <text class="d-k">金额</text>
            <text class="d-v">¥{{ toFixedYuan(logDetail.amount) }}</text>
          </view>
          <view class="d-row" v-if="logTask && logTask.description">
            <text class="d-k">备注</text>
            <text class="d-v">{{ logTask.description }}</text>
          </view>
          <view class="d-row">
            <text class="d-k">记账日期</text>
            <text class="d-v">{{ logDetail.execDate }}</text>
          </view>
          <view class="d-row">
            <text class="d-k">执行结果</text>
            <text class="d-v" :class="logDetail.status === 1 ? 'ok' : 'fail'">{{ logDetail.status === 1 ? '记账成功' : '记账失败' }}</text>
          </view>
          <view class="d-row">
            <text class="d-k">关联流水</text>
            <text class="d-v">{{ logDetail.transactionId ? ('#' + logDetail.transactionId) : '未生成' }}</text>
          </view>
          <view class="d-row">
            <text class="d-k">重试次数</text>
            <text class="d-v">{{ logDetail.retryCount || 0 }} 次</text>
          </view>
          <view class="d-row">
            <text class="d-k">记录时间</text>
            <text class="d-v">{{ fmtLogTime(logDetail.createTime) }}</text>
          </view>
          <view class="d-block" v-if="logDetail.status === 2">
            <text class="d-k">失败原因</text>
            <text class="d-fail">{{ logDetail.failReason || '未知原因' }}</text>
            <text class="log-retry d-retry" @tap="retry(logDetail)">立即重试</text>
          </view>
        </scroll-view>

        <view class="log-close" @tap="closeLogs">关闭</view>
      </view>
    </view>
  </view>
</template>

<script>
import NavBar from '@/components/NavBar/NavBar.vue';
import { useThemeStore } from '@/stores/theme';
import { useUserStore } from '@/stores/user';
import { listScheduleTasks, listScheduleLogs, toggleScheduleTask, runScheduleTask, retryScheduleLog, deleteScheduleTask } from '@/api/ledger';
import { toFixedYuan, typeText } from '@/utils/money';

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
      logTaskId: null,
      // 详情态要展示「类型/扣款账户/备注」，这些在任务上而不在日志表里
      logTask: null,
      // 非空即进入「执行详情」态（列表态与详情态共用同一个弹层）
      logDetail: null
    };
  },
  computed: {
    themeVars() { return useThemeStore().themeVars; },
    /** uni switch 的 color 需要具体色值（不走 CSS 变量求值），故从主题取强调色的实值 */
    primaryStrong() { return useThemeStore().theme['--primary-strong']; },
    /** 当前月份的 YYYY-MM 前缀，用于过滤出「当月」日志 */
    monthPrefix() {
      const d = new Date();
      return d.getFullYear() + '-' + String(d.getMonth() + 1).padStart(2, '0');
    },
    monthLabel() {
      const d = new Date();
      return d.getFullYear() + ' 年 ' + (d.getMonth() + 1) + ' 月';
    },
    /** 当月日志（后端已按记账日倒序返回，过滤后仍是最新在前） */
    monthLogs() {
      return this.logs.filter(l => String(l.execDate || '').startsWith(this.monthPrefix));
    }
  },
  onShow() {
    useThemeStore().restore();
    if (useUserStore().isLoggedIn) this.load();
  },
  methods: {
    toFixedYuan,
    typeText,
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
        this.logTaskId = t.id;
        this.logTask = t;        // 详情态要展示类型/扣款账户/备注
        this.logDetail = null;   // 每次打开都从列表态开始
        this.logsVisible = true;
      } catch (e) { }
    },
    closeLogs() { this.logsVisible = false; this.logDetail = null; },
    /** 后端 LocalDateTime 序列化为 2026-10-14T11:40:00，展示时换成空格 */
    fmtLogTime(s) { return s ? String(s).replace('T', ' ').slice(0, 19) : '—'; },
    async retry(l) {
      try {
        await retryScheduleLog(l.id);
        uni.showToast({ title: '重试成功', icon: 'success' });
        // 重新拉取，让状态/重试次数与后端一致（本地改字段会与 retryCount 不同步）
        const data = await listScheduleLogs(this.logTaskId) || {};
        this.logs = data.records || [];
        // 详情态若正开着同一条，同步刷新；若已不在列表则退回列表态
        if (this.logDetail) {
          this.logDetail = this.logs.find(x => x.id === this.logDetail.id) || null;
        }
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
.t-cycle { font-size: 22rpx; color: var(--primary-text); background: var(--primary-soft); padding: 4rpx 16rpx; border-radius: 16rpx; }
.t-info { font-size: 22rpx; color: var(--text-sub); margin-top: 10rpx; display: flex; gap: 24rpx; flex-wrap: wrap; }
.t-info.next { color: var(--text-sub); }
/* 操作区（下）：与上方信息区用分隔线隔开；开关 + 4 个按钮。
   按钮文字改回「执行日志」（4 字）后重新配宽：
   57(开关) + 64 + 64 + 42 + 42 + 24(间隙) = 293px < 375px 屏可用 323px；
   窄屏由 flex-wrap 自动折行 */
.t-actions {
  display: flex; align-items: center; flex-wrap: wrap;
  gap: 12rpx; margin-top: 20rpx; padding-top: 20rpx;
  border-top: 1rpx solid var(--border);
}
.t-switchbox {
  /* uni switch 默认 52×32px，且内部 .uni-switch-input 自带 5px 右外边距；
     这里抵消掉，使开关与按钮的间距和 gap 保持一致 */
  margin-right: -10rpx;
}
.t-btn {
  font-size: 22rpx; padding: 8rpx 20rpx; border-radius: 26rpx;
  background: var(--primary-soft); color: var(--primary-text);
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
  background: var(--primary); color: var(--on-primary); font-size: 52rpx; line-height: 100rpx; text-align: center;
  box-shadow: 0 8rpx 24rpx var(--primary-shadow);
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
/* 弹层标题栏：列表态为「标题 + ✕」；详情态左侧出现「返回」 */
.log-head { display: flex; align-items: center; min-height: 72rpx; }
.log-back { font-size: 26rpx; color: var(--primary-text); padding-right: 20rpx; }
.log-title { flex: 1; font-size: 30rpx; font-weight: 600; text-align: center; }
.log-x {
  width: 60rpx; height: 72rpx; line-height: 72rpx; text-align: center;
  font-size: 28rpx; color: var(--text-muted);
}
/* 详情态时标题左移，避免右侧 ✕ 把标题挤偏 */
.log-back + .log-title { text-align: left; }
.log-hint { font-size: 20rpx; color: var(--text-muted); text-align: center; margin: 6rpx 0 20rpx; }
.log-scroll { max-height: 50vh; }
.log-item { padding: 16rpx 0; border-bottom: 1rpx solid var(--border); }
.log-more { font-size: 28rpx; color: var(--text-muted); margin-left: 12rpx; }
.log-line { display: flex; justify-content: space-between; align-items: center; font-size: 24rpx; }
/* 日期占满左侧，状态/金额/箭头统一贴右（4 个子项用 space-between 会被摊开） */
.log-date { color: var(--text-sub); flex: 1; }
.log-status { margin-right: 24rpx; }
.log-status.ok { color: var(--income); }
.log-status.fail { color: var(--expense); }
.log-amount { color: var(--text-main); font-weight: 600; }
.log-reason { display: flex; justify-content: space-between; align-items: center; font-size: 22rpx; color: var(--expense); margin-top: 8rpx; }
.log-fail { flex: 1; min-width: 0; margin-right: 16rpx; }
.log-sub { font-size: 20rpx; color: var(--text-muted); margin-top: 6rpx; }
.log-retry { color: var(--primary-text); padding: 12rpx 20rpx; background: var(--primary-soft); border-radius: 8rpx; font-size: 22rpx; min-height: 88rpx; display: inline-flex; align-items: center; }
.log-empty { text-align: center; color: var(--text-muted); padding: 60rpx 0; font-size: 26rpx; }
.log-close {
  margin-top: 24rpx; text-align: center; padding: 18rpx;
  background: var(--surface); border-radius: 36rpx; font-size: 26rpx; color: var(--text-sub);
}

/* ===== 执行详情态 ===== */
.d-row {
  display: flex; align-items: center; justify-content: space-between;
  padding: 22rpx 0; border-bottom: 1rpx solid var(--border);
  font-size: 26rpx;
}
.d-k { color: var(--text-sub); flex-shrink: 0; margin-right: 24rpx; }
.d-v { color: var(--text-main); text-align: right; word-break: break-all; }
.d-v.ok { color: var(--income); font-weight: 600; }
.d-v.fail { color: var(--expense); font-weight: 600; }
.d-block { display: flex; flex-direction: column; padding: 22rpx 0; }
.d-fail { font-size: 24rpx; color: var(--expense); line-height: 1.6; margin-top: 12rpx; word-break: break-all; }
.d-retry { align-self: flex-start; margin-top: 20rpx; }
</style>
