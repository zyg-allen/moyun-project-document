<template>
  <view class="page" :style="themeVars">
    <!-- ==================== 头部：月份维度切换 ==================== -->
    <view class="month-bar">
      <view class="month-arrow" @tap="shiftMonth(-1)">‹</view>
      <view class="month-text">{{ periodLabel }}</view>
      <view class="month-arrow" :class="{ disabled: isCurrentMonth }" @tap="shiftMonth(1)">›</view>
    </view>

    <!-- 未登录引导 -->
    <view class="card login-guide" v-if="!userStore.isLoggedIn">
      <view class="lg-icon">📊</view>
      <view class="lg-title">财务分析报告</view>
      <view class="lg-desc">登录后即可按月生成财务分析报告：基础实时统计、健康评分、关键比率与 AI 深度解读</view>
      <view class="btn-primary" @tap="goLogin">去登录</view>
    </view>

    <template v-else>
      <!-- ==================== ① 基础实时统计（当月存在报告时实时计算展示） ==================== -->
      <template v-if="monthReports.length">
        <!-- 核心指标 -->
        <view class="kpi-grid">
          <view class="fin-kpi">
            <view class="label">净资产</view>
            <view class="value blue">¥{{ fmt(bs.netAssets) }}</view>
          </view>
          <view class="fin-kpi">
            <view class="label">{{ periodShort }}结余</view>
            <view class="value" :class="surplusClass">¥{{ fmt(is_.surplus) }}</view>
          </view>
          <view class="fin-kpi">
            <view class="label">{{ periodShort }}收入</view>
            <view class="value green">¥{{ fmt(is_.totalIncome) }}</view>
          </view>
          <view class="fin-kpi">
            <view class="label">{{ periodShort }}支出</view>
            <view class="value red">¥{{ fmt(is_.totalExpense) }}</view>
          </view>
        </view>

        <!-- 健康评分（规则引擎，实时） -->
        <view class="card">
          <view class="card-title flex-row">
            <text class="flex-1">财务健康评分</text>
            <text class="badge">实时计算</text>
          </view>
          <view class="score-box">
            <view class="score-big" :class="scoreClass">{{ score.total }}</view>
            <view class="score-info">
              <view class="grade">{{ score.grade }} · {{ score.gradeLabel }}</view>
              <view class="grade-desc">{{ score.desc }}</view>
            </view>
          </view>
        </view>

        <!-- 关键比率（6 项） -->
        <view class="card">
          <view class="card-title">关键财务比率</view>
          <view class="ratio-grid">
            <view class="ratio" v-for="r in ratioList" :key="r.name">
              <view class="r-name">{{ r.name }}</view>
              <view class="r-val">{{ r.text }}</view>
              <view class="r-hint" :class="r.levelClass">{{ r.hint }}</view>
            </view>
          </view>
        </view>
      </template>

      <!-- ==================== ② 深度报告分析（按钮 + 可关闭说明） ==================== -->
      <view class="card">
        <view class="card-title">深度报告分析</view>
        <!-- 作用说明（可关闭，本地记忆） -->
        <view class="hint-bar" v-if="!hintClosed">
          <text class="flex-1">在基础统计之上，由 AI 结合用户画像生成本月整体评价、风险点与个性化建议；生成约需 1 分钟，期间可离开页面。同月重新分析将覆盖更新原报告。</text>
          <text class="hint-close" @tap="closeHint">✕</text>
        </view>

        <!-- 当月：生成/重新分析 -->
        <template v-if="isCurrentMonth">
          <view class="gen-status" v-if="analyzing">
            <view class="gen-spinner"></view>
            <text class="flex-1">AI 分析执行中…（约 1 分钟，完成后自动刷新，可离开此页）</text>
          </view>
          <view class="gen-btn" :class="{ disabled: analyzing }" v-else @tap="!analyzing && startAnalysis()">
            {{ monthReports.length ? '重新分析（覆盖本月报告）' : '生成深度分析报告' }}
          </view>
        </template>
        <!-- 历史月：后端仅支持当月生成 -->
        <view class="his-tip" v-else>历史月份报告为当月生成时的快照，仅支持查看与删除，不支持重新生成</view>
      </view>

      <!-- ==================== ③ 分析报告列表（该月） ==================== -->
      <view class="card" v-if="monthReports.length">
        <view class="card-title flex-row">
          <text class="flex-1">分析报告列表</text>
          <text class="sub">{{ monthReports.length }} 份</text>
        </view>
        <view class="rp-item" v-for="r in monthReports" :key="r.id">
          <view class="rp-main" @tap="openPreview(r)">
            <view class="rp-month">
              {{ r.period }}
              <text class="rp-range">{{ r.rangeLabel }}</text>
              <text class="rp-ai" :class="{ tpl: !r.aiEnabled }">{{ r.aiEnabled ? 'AI 生成' : '基础分析' }}</text>
            </view>
            <view class="rp-time">生成时间：{{ r.updateTime }}</view>
            <view class="rp-snapshot" v-if="r.profileSnapshot">{{ r.profileSnapshot }}</view>
          </view>
          <view class="rp-side">
            <view class="rp-score" :class="scoreNumClass(r.healthScore)">{{ r.healthScore }}分</view>
            <view class="rp-view" @tap="openPreview(r)">查看 ›</view>
            <view class="rp-del" @tap="removeReport(r)">删除</view>
          </view>
        </view>
      </view>

      <!-- 无报告空态 -->
      <view class="card empty" v-else>
        <view class="empty-icon">📋</view>
        <view class="empty-title">{{ periodLabel }}暂无分析报告</view>
        <view class="empty-desc">{{ isCurrentMonth ? '点击上方「生成深度分析报告」开始闭环' : '该月未生成过报告，历史月份不支持补生成' }}</view>
      </view>

      <!-- ==================== 报告预览（查看报告闭环） ==================== -->
      <view class="mask" v-if="preview.show" @tap="closePreview">
        <view class="preview-sheet" @tap.stop>
          <template v-if="preview.data">
            <view class="pv-head">
              <view class="pv-title">报告预览</view>
              <text class="hint-close" @tap="closePreview">✕</text>
            </view>
            <scroll-view scroll-y class="pv-body">
              <!-- 报告元信息：月份 / 范围 / 版本（覆盖式）/ 有效时间 -->
              <view class="pv-meta">
                <view class="meta-row"><text class="k">月份</text><text class="v">{{ preview.data.period }}</text></view>
                <view class="meta-row"><text class="k">范围</text><text class="v">{{ preview.data.rangeLabel || '本月' }}</text></view>
                <view class="meta-row"><text class="k">健康分</text><text class="v strong" :class="scoreNumClass(preview.data.healthScore)">{{ preview.data.healthScore ?? '—' }} 分</text></view>
                <view class="meta-row"><text class="k">生成时间</text><text class="v">{{ preview.data.updateTime || '—' }}</text></view>
                <view class="meta-row" v-if="preview.data.profileSnapshot"><text class="k">画像快照</text><text class="v">{{ preview.data.profileSnapshot }}</text></view>
              </view>

              <!-- 核心指标 -->
              <view class="pv-sec" v-if="pvIndicators">
                <view class="pv-sec-title">核心指标</view>
                <view class="pv-kv"><text>总资产</text><text>¥ {{ fmt(pvIndicators.totalAsset) }}</text></view>
                <view class="pv-kv"><text>总负债</text><text>¥ {{ fmt(pvIndicators.totalLiability) }}</text></view>
                <view class="pv-kv"><text>资产负债率</text><text>{{ pvIndicators.debtRatio ?? 0 }}%</text></view>
                <view class="pv-kv"><text>月储蓄率</text><text>{{ pvIndicators.savingRate ?? 0 }}%</text></view>
                <view class="pv-kv" v-if="pvIndicators.emergencyFundMonths != null"><text>应急基金</text><text>{{ pvIndicators.emergencyFundMonths }} 个月支出</text></view>
              </view>

              <!-- AI 综述 -->
              <view class="pv-sec" v-if="preview.data.aiSummary">
                <view class="pv-sec-title">整体评价</view>
                <view class="pv-p">{{ preview.data.aiSummary }}</view>
              </view>

              <!-- 风险点 -->
              <view class="pv-sec" v-if="preview.data.debtRisks && preview.data.debtRisks.length">
                <view class="pv-sec-title">⚠️ 风险提示</view>
                <view class="pv-li" v-for="(r, i) in preview.data.debtRisks" :key="'r' + i">
                  <text class="dot warn"></text>
                  <text class="flex-1">{{ r.title }}{{ r.detail ? '：' + r.detail : '' }}</text>
                </view>
              </view>

              <!-- 建议 -->
              <view class="pv-sec" v-if="preview.data.suggestions && preview.data.suggestions.length">
                <view class="pv-sec-title">💡 给你的建议</view>
                <view class="pv-li" v-for="(s, i) in preview.data.suggestions" :key="'s' + i">
                  <text class="dot good"></text>
                  <text class="flex-1">{{ s.title }}{{ s.detail ? '：' + s.detail : '' }}<text v-if="s.expectedImpact" class="pv-impact">（预期：{{ s.expectedImpact }}）</text></text>
                </view>
              </view>
            </scroll-view>
            <!-- 预览操作区：删除（PDF 导出后端暂未支持，待开放） -->
            <view class="pv-foot">
              <view class="pv-del" @tap="removeReport(preview.current)">删除此报告</view>
              <view class="pv-close" @tap="closePreview">关闭</view>
            </view>
          </template>
          <view class="pv-loading" v-else>报告加载中…</view>
        </view>
      </view>
    </template>

    <!-- ==================== 脚部：数据说明 ==================== -->
    <view class="page-footer">
      报告统计基于记账数据实时计算；深度分析由 AI 结合用户画像生成
      <view class="footer-sub">同月同范围报告为覆盖式更新（重新分析即最新版本）</view>
    </view>
  </view>
</template>

<script>
import { getFinancialReport, listAiReports, getAiReportDetail, deleteAiReport,
         submitAiAnalysisTask, getAiAnalysisTask } from '@/api/ledger';
import { formatAmount, toNum } from '@/utils/money';
import { useUserStore } from '@/stores/user';
import { useThemeStore } from '@/stores/theme';

const HINT_KEY = 'moyun_fin_hint_closed';

/** 比率评级 → 展示（hint 文案与配色） */
const LEVEL_HINT = { A: { text: '优秀', cls: 'good' }, B: { text: '良好', cls: 'warn' }, C: { text: '预警', cls: 'bad' } };

export default {
  data() {
    return {
      /** 当前选中月份（yyyy-MM），页面以月为维度 */
      period: '',
      /** 该月全部报告（存在性 = length > 0；一次 listReports 拉取后按月过滤） */
      monthReports: [],
      allReports: [],
      /** 基础实时统计（确定性计算，任意月份可用） */
      finReport: { period: '', asOfDate: '', balanceSheet: {}, incomeStatement: {}, ratios: {}, score: {} },
      /** 深度分析说明提示是否已关闭（本地记忆） */
      hintClosed: false,
      /** 异步任务状态（运行中保留显示执行状态） */
      analyzing: false,
      pollTimer: null,
      pollCount: 0,
      loading: false,
      /** 报告预览弹层 */
      preview: { show: false, data: null, current: null }
    };
  },
  computed: {
    themeVars() { return useThemeStore().themeVars; },
    userStore() { return useUserStore(); },
    bs() { return this.finReport.balanceSheet || {}; },
    is_() { return this.finReport.incomeStatement || {}; },
    score() { return this.finReport.score || {}; },
    periodLabel() { return (this.period || this.currentPeriod()).replace('-', '年') + '月'; },
    periodShort() { return this.period === this.currentPeriod() ? '本月' : (this.period.split('-')[1] || '') + '月'; },
    isCurrentMonth() { return this.period === this.currentPeriod(); },
    surplusClass() { return toNum(this.is_.surplus) >= 0 ? 'green' : 'red'; },
    scoreClass() {
      const t = this.score.total || 0;
      return t >= 70 ? 'good' : t >= 40 ? 'mid' : 'bad';
    },
    ratioList() {
      const r = this.finReport.ratios || {};
      const mk = (name, value, lvl, suffix, scale1) => {
        if (value == null) return { name, text: '—', hint: '无数据', levelClass: 'none' };
        const hint = LEVEL_HINT[lvl] || { text: '无数据', cls: 'none' };
        return {
          name,
          text: (scale1 ? Math.round(value * 10) / 10 : Math.round(value)) + suffix,
          hint: hint.text,
          levelClass: hint.cls
        };
      };
      return [
        mk('资产负债率', r.debtToAsset, r.debtToAssetLevel, '%'),
        mk('紧急预备金', r.emergencyFundMonths, r.emergencyFundLevel, '月', true),
        mk('结余率', r.surplusRatio, r.surplusLevel, '%'),
        mk('负债收入比', r.debtToIncome, r.debtToIncomeLevel, '%'),
        mk('被动收入占比', r.passiveIncomeRatio, r.passiveIncomeRatioLevel, '%'),
        mk('投资资产占比', r.investmentAssetRatio, r.investmentAssetRatioLevel, '%')
      ];
    },
    /** 预览报告指标（字段缺失时隐藏区块） */
    pvIndicators() {
      const ind = this.preview.data && this.preview.data.indicators;
      return ind && (ind.totalAsset != null || ind.debtRatio != null) ? ind : null;
    }
  },
  onShow() {
    useThemeStore().restore();
    if (!this.period) this.period = this.currentPeriod();
    this.hintClosed = !!uni.getStorageSync(HINT_KEY);
    if (useUserStore().isLoggedIn) this.load();
  },
  onHide() { this.stopPoll(); },
  onUnload() { this.stopPoll(); },
  onPullDownRefresh() {
    this.load().finally(() => uni.stopPullDownRefresh());
  },
  methods: {
    currentPeriod() {
      const d = new Date();
      return d.getFullYear() + '-' + String(d.getMonth() + 1).padStart(2, '0');
    },
    /** 进入页面/切月第一步：检查该月是否存在报告 + 存在则加载基础实时统计 */
    async load() {
      if (!useUserStore().isLoggedIn || this.loading) return;
      this.loading = true;
      try {
        // ① 拉取报告列表并按选中月过滤（存在性 + 列表一次完成）
        const data = await listAiReports({ page: 1, pageSize: 100 }) || {};
        this.allReports = data.list || [];
        this.monthReports = this.allReports.filter(r => r.period === this.period);
        // ② 存在报告 → 展示基础实时统计数据（实时确定性计算，不依赖快照）
        if (this.monthReports.length) {
          const fin = await getFinancialReport(this.period) || {};
          this.finReport = fin;
        }
      } catch (e) { /* 拦截器已提示 */ }
      this.loading = false;
    },
    shiftMonth(delta) {
      const [y, m] = this.period.split('-').map(Number);
      const d = new Date(y, m - 1 + delta, 1);
      const next = d.getFullYear() + '-' + String(d.getMonth() + 1).padStart(2, '0');
      if (next > this.currentPeriod()) return; // 不允许未来月
      if (next === this.period) return;
      this.stopPoll();
      this.period = next;
      this.load();
    },
    /** 关闭深度分析说明（本地记忆，不再提示） */
    closeHint() {
      this.hintClosed = true;
      uni.setStorageSync(HINT_KEY, true);
    },
    /** 点击分析：提交异步任务并轮询；运行中保留执行状态，成功/失败后状态消失并刷新列表 */
    async startAnalysis() {
      if (this.analyzing) return;
      this.analyzing = true;
      this.pollCount = 0;
      try {
        const task = await submitAiAnalysisTask({ range: 'month' }) || {};
        if (task.taskId) {
          this.pollTask(task.taskId);
        } else {
          this.analyzing = false;
        }
      } catch (e) {
        this.analyzing = false; /* 拦截器已提示 */
      }
    },
    pollTask(taskId) {
      this.stopPoll();
      this.pollTimer = setTimeout(async () => {
        try {
          const t = await getAiAnalysisTask(taskId) || {};
          if (t.status === 'success') {
            this.analyzing = false;
            uni.showToast({ title: '分析完成', icon: 'success' });
            this.load(); // 刷新存在性 + 基础统计 + 列表
            return;
          }
          if (t.status === 'failed' || t.status === 'not_found') {
            this.analyzing = false;
            uni.showToast({ title: t.error || '分析失败，请稍后重试', icon: 'none' });
            return;
          }
          // pending/running 继续（间隔 5s，最长 10 分钟）
          if (++this.pollCount > 120) {
            this.analyzing = false;
            uni.showToast({ title: '生成超时，可下拉刷新查看结果', icon: 'none' });
            return;
          }
          this.pollTask(taskId);
        } catch (e) {
          this.analyzing = false; /* 网络异常终止轮询 */
        }
      }, 5000);
    },
    stopPoll() {
      if (this.pollTimer) { clearTimeout(this.pollTimer); this.pollTimer = null; }
    },
    /** 报告预览：点击查看进入 */
    async openPreview(r) {
      if (!r || !r.id) return;
      this.preview = { show: true, data: null, current: r };
      try {
        const detail = await getAiReportDetail(r.id) || {};
        this.preview.data = detail;
      } catch (e) {
        this.preview = { show: false, data: null, current: null }; /* 拦截器已提示 */
      }
    },
    closePreview() {
      this.preview = { show: false, data: null, current: null };
    },
    /** 删除报告（闭环终点，预览中可直达） */
    removeReport(r) {
      if (!r || !r.id) return;
      uni.showModal({
        title: '删除报告',
        content: '确定删除 ' + (r.period || '') + '（' + (r.rangeLabel || '本月') + '）这份报告吗？删除后不可恢复。',
        success: async (res) => {
          if (!res.confirm) return;
          try {
            await deleteAiReport(r.id);
            if (this.preview.current && this.preview.current.id === r.id) this.closePreview();
            this.monthReports = this.monthReports.filter(x => x.id !== r.id);
            this.allReports = this.allReports.filter(x => x.id !== r.id);
            uni.showToast({ title: '已删除', icon: 'success' });
          } catch (e) { /* 拦截器已提示 */ }
        }
      });
    },
    scoreNumClass(v) {
      const n = Number(v || 0);
      return n >= 70 ? 'good' : n >= 40 ? 'mid' : 'bad';
    },
    fmt(v) { return formatAmount(v || 0); },
    goLogin() { uni.switchTab({ url: '/pages/mine/index' }); }
  }
};
</script>

<style scoped>
.page { padding: 20rpx 0 60rpx; }

/* ==================== 头部：月份切换 ==================== */
.month-bar { display: flex; align-items: center; justify-content: center; gap: 32rpx; padding: 8rpx 0 20rpx; }
.month-arrow {
  width: 64rpx; height: 64rpx; border-radius: 32rpx; background: #fff;
  display: flex; align-items: center; justify-content: center;
  font-size: 36rpx; color: var(--primary-strong);
}
.month-arrow.disabled { color: #d5d9de; }
.month-text { font-size: 32rpx; font-weight: 700; color: #333; min-width: 220rpx; text-align: center; }

/* 未登录引导 */
.login-guide { text-align: center; padding: 80rpx 40rpx; }
.lg-icon { font-size: 80rpx; margin-bottom: 20rpx; }
.lg-title { font-size: 36rpx; font-weight: 700; margin-bottom: 12rpx; }
.lg-desc { font-size: 26rpx; color: #999; margin-bottom: 40rpx; line-height: 1.6; }
.btn-primary {
  height: 84rpx; line-height: 84rpx; text-align: center; margin: 0 40rpx;
  background: var(--primary-strong); color: #fff; border-radius: 42rpx; font-size: 30rpx; font-weight: 600;
}

/* 通用卡片 */
.card { background: #fff; border-radius: 20rpx; padding: 24rpx; margin: 0 24rpx 20rpx; }
.card-title { font-size: 30rpx; font-weight: 600; margin-bottom: 16rpx; }
.flex-row { display: flex; align-items: center; }
.flex-1 { flex: 1; min-width: 0; }
.sub { font-size: 22rpx; color: #999; }
.badge { font-size: 20rpx; color: #fff; background: #8a94a6; border-radius: 8rpx; padding: 4rpx 12rpx; }

/* ① 基础实时统计 */
.kpi-grid { display: grid; grid-template-columns: repeat(2, 1fr); gap: 16rpx; margin: 0 24rpx 20rpx; }
.fin-kpi { background: #fff; border-radius: 16rpx; padding: 24rpx; box-shadow: 0 1rpx 3rpx rgba(0,0,0,0.04); }
.fin-kpi .label { font-size: 22rpx; color: #888; }
.fin-kpi .value { font-size: 36rpx; font-weight: 700; margin-top: 4rpx; }
.fin-kpi .value.green { color: #10b981; }
.fin-kpi .value.red { color: #ef4444; }
.fin-kpi .value.blue { color: #3b82f6; }

.score-box { display: flex; align-items: center; gap: 24rpx; }
.score-big { font-size: 72rpx; font-weight: 700; min-width: 110rpx; text-align: center; line-height: 1; }
.score-big.good { color: #10b981; }
.score-big.mid { color: #f59e0b; }
.score-big.bad { color: #ef4444; }
.score-info .grade { font-size: 28rpx; font-weight: 600; }
.score-info .grade-desc { font-size: 24rpx; color: #666; margin-top: 8rpx; line-height: 1.6; }

.ratio-grid { display: grid; grid-template-columns: repeat(3, 1fr); gap: 14rpx; }
.ratio { text-align: center; padding: 18rpx 8rpx; background: #f9fafb; border-radius: 12rpx; }
.r-name { font-size: 20rpx; color: #888; }
.r-val { font-size: 30rpx; font-weight: 700; margin: 4rpx 0; }
.r-hint { font-size: 20rpx; }
.r-hint.good { color: #10b981; }
.r-hint.warn { color: #f59e0b; }
.r-hint.bad { color: #ef4444; }
.r-hint.none { color: #b6bcc4; }

/* ② 深度分析卡 */
.hint-bar {
  display: flex; align-items: flex-start; gap: 16rpx;
  background: #f0f7ff; border-radius: 12rpx; padding: 18rpx 20rpx;
  font-size: 22rpx; color: #4a6a8f; line-height: 1.7; margin-bottom: 20rpx;
}
.hint-close { font-size: 26rpx; color: #9aa7b8; padding: 0 8rpx; flex-shrink: 0; }
.gen-btn {
  height: 84rpx; line-height: 84rpx; text-align: center;
  background: var(--primary-strong); color: #fff; border-radius: 42rpx;
  font-size: 28rpx; font-weight: 600;
}
.gen-btn.disabled { opacity: 0.6; }
.gen-status {
  display: flex; align-items: center; gap: 16rpx;
  background: #fff8e6; border-radius: 14rpx; padding: 20rpx;
  font-size: 24rpx; color: #9a6b1f; line-height: 1.6;
}
.gen-spinner { width: 28rpx; height: 28rpx; border: 4rpx solid #f3e3c3; border-top-color: #f59e0b; border-radius: 50%; animation: gen-spin 0.8s linear infinite; flex-shrink: 0; }
@keyframes gen-spin { to { transform: rotate(360deg); } }
.his-tip { font-size: 24rpx; color: #999; line-height: 1.6; padding: 8rpx 0; }

/* ③ 报告列表 */
.rp-item { display: flex; align-items: center; justify-content: space-between; padding: 22rpx 0; border-bottom: 1rpx solid #f5f5f7; }
.rp-item:last-of-type { border-bottom: none; }
.rp-main { flex: 1; min-width: 0; }
.rp-month { font-size: 30rpx; font-weight: 600; display: flex; align-items: center; flex-wrap: wrap; gap: 8rpx; }
.rp-range { font-size: 22rpx; font-weight: 400; color: var(--primary-strong); }
.rp-ai { font-size: 20rpx; color: #fff; background: var(--primary); border-radius: 8rpx; padding: 2rpx 10rpx; }
.rp-ai.tpl { background: #bbb; }
.rp-time { font-size: 22rpx; color: #999; margin-top: 8rpx; }
.rp-snapshot { font-size: 20rpx; color: #aaa; margin-top: 6rpx; }
.rp-side { display: flex; align-items: center; gap: 20rpx; margin-left: 16rpx; flex-shrink: 0; }
.rp-score { font-size: 26rpx; font-weight: 700; }
.rp-score.good { color: #52c41a; }
.rp-score.mid { color: #faad14; }
.rp-score.bad { color: #e57373; }
.rp-view { font-size: 22rpx; color: var(--primary-strong); }
.rp-del { font-size: 22rpx; color: #e5964f; }

/* 空态 */
.empty { text-align: center; padding: 60rpx 40rpx; }
.empty-icon { font-size: 72rpx; margin-bottom: 16rpx; }
.empty-title { font-size: 30rpx; font-weight: 600; margin-bottom: 10rpx; }
.empty-desc { font-size: 24rpx; color: #999; line-height: 1.6; }

/* 报告预览弹层 */
.mask { position: fixed; inset: 0; background: rgba(0,0,0,0.5); z-index: 99; display: flex; align-items: flex-end; }
.preview-sheet {
  width: 100%; max-height: 86vh; background: #fff; border-radius: 32rpx 32rpx 0 0;
  display: flex; flex-direction: column; padding: 32rpx 32rpx calc(24rpx + env(safe-area-inset-bottom));
}
.pv-head { display: flex; align-items: center; justify-content: space-between; margin-bottom: 20rpx; }
.pv-title { font-size: 34rpx; font-weight: 700; }
.pv-body { flex: 1; min-height: 0; max-height: 60vh; }
.pv-loading { text-align: center; padding: 80rpx 0; color: #999; font-size: 26rpx; }
.pv-meta { background: #f9fafb; border-radius: 12rpx; padding: 8rpx 20rpx; margin-bottom: 20rpx; }
.meta-row { display: flex; justify-content: space-between; gap: 20rpx; padding: 12rpx 0; font-size: 24rpx; border-bottom: 1rpx solid #f0f1f4; }
.meta-row:last-child { border-bottom: none; }
.meta-row .k { color: #888; flex-shrink: 0; }
.meta-row .v { color: #333; text-align: right; word-break: break-all; }
.meta-row .v.strong { font-weight: 700; }
.meta-row .v.strong.good { color: #52c41a; }
.meta-row .v.strong.mid { color: #faad14; }
.meta-row .v.strong.bad { color: #e57373; }
.pv-sec { margin-bottom: 24rpx; }
.pv-sec-title { font-size: 28rpx; font-weight: 600; margin-bottom: 12rpx; color: #2c4a6e; }
.pv-kv { display: flex; justify-content: space-between; padding: 12rpx 0; font-size: 24rpx; color: #555; border-bottom: 1rpx solid #f5f5f7; }
.pv-p { font-size: 25rpx; color: #444; line-height: 1.8; }
.pv-li { display: flex; gap: 12rpx; margin-bottom: 10rpx; font-size: 24rpx; color: #555; line-height: 1.7; }
.pv-impact { color: #27ae60; }
.dot { width: 14rpx; height: 14rpx; border-radius: 7rpx; margin-top: 12rpx; flex-shrink: 0; }
.dot.warn { background: #f59e0b; }
.dot.good { background: #10b981; }
.pv-foot { display: flex; gap: 20rpx; margin-top: 8rpx; }
.pv-del {
  flex: 1; height: 80rpx; line-height: 80rpx; text-align: center;
  border: 1rpx solid #e5964f; color: #e5964f; border-radius: 40rpx; font-size: 26rpx;
}
.pv-close {
  flex: 1; height: 80rpx; line-height: 80rpx; text-align: center;
  background: var(--primary-strong); color: #fff; border-radius: 40rpx; font-size: 26rpx; font-weight: 600;
}

/* 脚部 */
.page-footer {
  padding: 32rpx 40rpx calc(24rpx + env(safe-area-inset-bottom));
  text-align: center; font-size: 22rpx; color: #c3c8cf; line-height: 1.7;
}
.footer-sub { margin-top: 6rpx; color: #cdd2d8; }
</style>
