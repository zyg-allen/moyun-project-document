<template>
  <view class="page" :style="themeVars">
    <!-- 未登录引导 -->
    <view class="card login-guide" v-if="!userStore.isLoggedIn">
      <view class="lg-icon">📊</view>
      <view class="lg-title">AI 财务分析</view>
      <view class="lg-desc">登录后即可获得：财务健康体检、收入结构分析、债务风险提示与个性化建议</view>
      <view class="btn-primary" @tap="goLogin">去登录</view>
    </view>

    <template v-else>
      <!-- 用户画像卡 -->
      <view class="card profile-card">
        <view class="pf-left">
          <view class="pf-avatar">👤</view>
          <view class="pf-info">
            <view class="pf-name">
              {{ profile.identityTagLabel || '未设置身份' }}
              <text class="pf-tag" v-if="profile.identityTag">✓</text>
            </view>
            <view class="pf-sub">{{ profile.position || '未填写职位' }}<text v-if="profile.company"> @ {{ profile.company }}</text></view>
          </view>
        </view>
        <view class="pf-edit" @tap="openProfileEdit">编辑</view>
      </view>

      <!-- 财务健康指标（v11.37：维度切换） -->
      <view class="card">
        <view class="range-tabs">
          <view class="range-tab" :class="{ on: range === 'month' }" @tap="switchRange('month')">本月</view>
          <view class="range-tab" :class="{ on: range === '3m' }" @tap="switchRange('3m')">近3月</view>
          <view class="range-tab" :class="{ on: range === '6m' }" @tap="switchRange('6m')">近6月</view>
          <view class="range-tab" :class="{ on: range === 'year' }" @tap="switchRange('year')">近12月</view>
        </view>
        <view class="card-title">财务健康指标<text class="range-hint" v-if="indicators.rangeLabel">（{{ indicators.rangeLabel }}<text v-if="indicators.sampleMonths">，含数据 {{ indicators.sampleMonths }} 个月</text>）</text></view>
        <view class="kpi-grid">
          <view class="kpi">
            <view class="kpi-value" :class="level(debtRatioLevel)">{{ indicators.debtRatio ?? 0 }}<text class="kpi-unit">%</text></view>
            <view class="kpi-label">资产负债率</view>
          </view>
          <view class="kpi">
            <view class="kpi-value" :class="level(savingLevel)">{{ indicators.savingRate ?? 0 }}<text class="kpi-unit">%</text></view>
            <view class="kpi-label">月储蓄率</view>
          </view>
          <view class="kpi">
            <view class="kpi-value" :class="level(pressureLevel)">{{ indicators.repaymentPressure ?? 0 }}<text class="kpi-unit">%</text></view>
            <view class="kpi-label">还款压力</view>
          </view>
        </view>
        <view class="kpi-rows">
          <view class="kpi-row"><text>总资产</text><text>¥ {{ fmt(indicators.totalAsset) }}</text></view>
          <view class="kpi-row"><text>总负债</text><text>¥ {{ fmt(indicators.totalLiability) }}</text></view>
          <view class="kpi-row"><text>月均收入</text><text>¥ {{ fmt(indicators.avgMonthlyIncome) }}</text></view>
          <view class="kpi-row"><text>月均支出</text><text>¥ {{ fmt(indicators.avgMonthlyExpense) }}</text></view>
          <view class="kpi-row" v-if="indicators.monthlyRepayment > 0"><text>月供合计</text><text>¥ {{ fmt(indicators.monthlyRepayment) }}</text></view>
        </view>
      </view>

      <!-- AI 综述（v11.36：健康分 + 报告月份 + 缓存标记） -->
      <view class="card">
        <view class="card-title flex-row">
          <text class="flex-1">AI 财务综述<text class="rp-period" v-if="reportPeriod">（{{ reportPeriod }}）</text></text>
          <text class="cache-badge" v-if="fromCache && range === 'month'">本月报告</text>
          <text class="ai-badge" v-if="aiEnabled">AI 生成</text>
          <text class="ai-badge tpl" v-else>基础分析</text>
        </view>
        <view class="score-row" v-if="reportPeriod">
          <view class="score-num" :class="reportScore >= 70 ? 'good' : reportScore >= 40 ? 'mid' : 'bad'">{{ reportScore }}</view>
          <view class="score-info">
            <view class="score-label">财务健康分</view>
            <view class="score-desc">{{ reportScore >= 70 ? '状况良好，继续保持' : reportScore >= 40 ? '存在隐忧，建议优化' : '风险偏高，需重点改善' }}</view>
          </view>
        </view>
        <view class="ai-summary" v-if="aiSummary">{{ aiSummary }}</view>
        <view class="ai-summary placeholder" v-else>暂无数据，先去记几笔账吧</view>
        <view class="ai-refresh" @tap="load(true)">重新分析</view>
      </view>

      <!-- 历史报告（v11.36） -->
      <view class="card" v-if="reportList.length">
        <view class="card-title flex-row">
          <text class="flex-1">历史报告</text>
          <text class="rp-total">共 {{ reportTotal }} 期</text>
        </view>
        <view class="rp-item" v-for="r in reportList" :key="r.id" @tap="viewReport(r)">
          <view class="rp-main">
            <view class="rp-month">{{ r.period }}</view>
            <view class="rp-snapshot" v-if="r.profileSnapshot">{{ r.profileSnapshot }}</view>
          </view>
          <view class="rp-side">
            <view class="rp-score" :class="r.healthScore >= 70 ? 'good' : r.healthScore >= 40 ? 'mid' : 'bad'">{{ r.healthScore }}分</view>
            <view class="rp-view">查看 ›</view>
          </view>
        </view>
        <view class="rp-more" v-if="reportList.length < reportTotal" @tap="loadMoreReports">加载更多（{{ reportList.length }}/{{ reportTotal }}）</view>
      </view>

      <!-- 收入来源 -->
      <view class="card" v-if="incomeSources.length">
        <view class="card-title">收入来源结构<text class="range-hint" v-if="indicators.rangeLabel">（{{ indicators.rangeLabel }}）</text></view>
        <view class="src-row" v-for="s in incomeSources" :key="s.name">
          <view class="src-name">{{ s.name }}</view>
          <view class="src-bar-wrap">
            <view class="src-bar" :style="{ width: Math.max(4, s.ratio) + '%' }"></view>
          </view>
          <view class="src-val">{{ s.ratio }}%</view>
        </view>
      </view>

      <!-- 债务风险 -->
      <view class="card" v-if="debtRisks.length">
        <view class="card-title">债务风险提示</view>
        <view class="risk-item" v-for="(r, i) in debtRisks" :key="i">
          <view class="risk-dot" :class="r.level"></view>
          <view class="flex-1">
            <view class="risk-title">{{ r.title }}</view>
            <view class="risk-detail">{{ r.detail }}</view>
          </view>
        </view>
      </view>

      <!-- 建议 -->
      <view class="card" v-if="suggestions.length">
        <view class="card-title">给你的建议</view>
        <view class="sug-item" v-for="(s, i) in suggestions" :key="i">
          <view class="sug-icon">{{ s.icon }}</view>
          <view class="flex-1">
            <view class="sug-title">{{ s.title }}</view>
            <view class="sug-detail">{{ s.detail }}</view>
          </view>
        </view>
      </view>

      <!-- 画像编辑弹层 -->
      <view class="mask" v-if="profileEditing" @tap="profileEditing = false">
        <view class="sheet" @tap.stop>
          <view class="sheet-title">编辑个人画像</view>
          <view class="field">
            <text class="field-label">身份标签</text>
            <picker :range="identityLabels" @change="onTagChange">
              <view class="field-picker">{{ editForm.identityTagLabel || '选择身份' }} ▾</view>
            </picker>
          </view>
          <view class="field">
            <text class="field-label">职位</text>
            <input v-model="editForm.position" placeholder="如：产品经理" class="field-input" />
          </view>
          <view class="field">
            <text class="field-label">公司</text>
            <input v-model="editForm.company" placeholder="选填" class="field-input" />
          </view>
          <view class="pf-tip">画像信息与墨韵门户共用（同一账号体系），会同步展示</view>
          <view class="btn-primary" @tap="saveProfile">保存</view>
          <view class="btn-cancel" @tap="profileEditing = false">取消</view>
        </view>
      </view>
    </template>
  </view>
</template>

<script>
import { getAiAnalysis, getAiProfile, updateAiProfile, listAiReports } from '@/api/ledger';
import { formatAmount } from '@/utils/money';
import { useUserStore } from '@/stores/user';
import { useThemeStore } from '@/stores/theme';

export default {
  data() {
    return {
      profile: {},
      indicators: {},
      incomeSources: [],
      debtRisks: [],
      suggestions: [],
      aiSummary: '',
      aiEnabled: false,
      identityOptions: [],
      profileEditing: false,
      reportList: [],
      reportTotal: 0,
      reportPage: 1,
      reportPageSize: 5,
      reportPeriod: '',
      range: 'month',
      reportScore: 0,
      fromCache: false,
      editForm: { identityTag: '', identityTagLabel: '', position: '', company: '' },
      loading: false
    };
  },
  computed: {
    themeVars() { return useThemeStore().themeVars; },
    userStore() { return useUserStore(); },
    identityLabels() { return this.identityOptions.map(o => o.label); },
    // 分级着色（规则与后端一致）
    debtRatioLevel() {
      const v = this.indicators.debtRatio || 0;
      if (v >= 80) return 'bad';
      if (v >= 50) return 'mid';
      return 'good';
    },
    savingLevel() {
      const v = this.indicators.savingRate || 0;
      if (v >= 20) return 'good';
      if (v >= 10) return 'mid';
      return 'bad';
    },
    pressureLevel() {
      const v = this.indicators.repaymentPressure || 0;
      if (v >= 50) return 'bad';
      if (v >= 30) return 'mid';
      return 'good';
    }
  },
  onShow() {
    useThemeStore().restore();
    if (useUserStore().isLoggedIn) this.load();
  },
  onPullDownRefresh() {
    this.load().finally(() => uni.stopPullDownRefresh());
  },
  methods: {
    async load(force) {
      if (!useUserStore().isLoggedIn || this.loading) return;
      this.loading = true;
      // v11.36：画像独立请求，先返回先渲染（不等分析）
      getAiProfile().then((pf) => {
        if (pf) {
          this.profile = pf;
          this.identityOptions = pf.identityOptions || [];
        }
      }).catch(() => {});
      if (force) uni.showLoading({ title: '分析中…' });
      try {
        // force=true 走 refresh 强制重新分析（烧 token）；默认"本月"命中快照零 token
        const params = force ? { range: this.range, refresh: true } : { range: this.range };
        const report = await getAiAnalysis(params);
        this.applyReport(report);
        this.loadReports(true);
      } catch (e) { /* 拦截器已提示 */ }
      finally {
        this.loading = false;
        if (force) uni.hideLoading();
      }
    },
    switchRange(r) {
      if (this.range === r) return;
      this.range = r;
      this.load(false);
    },
    applyReport(report) {
      this.indicators = (report && report.indicators) || {};
      this.incomeSources = (report && report.incomeSources) || [];
      this.debtRisks = (report && report.debtRisks) || [];
      this.suggestions = (report && report.suggestions) || [];
      this.aiSummary = (report && report.aiSummary) || '';
      this.aiEnabled = !!report && !!report.aiEnabled;
      this.reportPeriod = (report && report.period) || '';
      this.reportScore = (report && report.healthScore) || 0;
      this.fromCache = !!report && !!report.fromCache;
    },
    // 历史报告分页
    async loadReports(reset) {
      if (!useUserStore().isLoggedIn) return;
      if (reset) { this.reportPage = 1; this.reportList = []; }
      try {
        const data = await listAiReports({ page: this.reportPage, pageSize: this.reportPageSize }) || {};
        const list = data.list || [];
        this.reportList = reset ? list : this.reportList.concat(list);
        this.reportTotal = Number(data.total || 0);
      } catch (e) { /* 静默 */ }
    },
    loadMoreReports() {
      if (this.reportList.length >= this.reportTotal) return;
      this.reportPage++;
      this.loadReports(false);
    },
    // 查看某期历史报告
    viewReport(r) {
      if (!r || !r.aiSummary) return;
      uni.showModal({
        title: r.period + ' 报告（' + (r.healthScore || 0) + ' 分）',
        content: r.aiSummary,
        showCancel: false,
        confirmText: '关闭'
      });
    },
    fmt(cents) { return formatAmount(cents || 0); },
    level(l) { return l; },
    goLogin() { uni.switchTab({ url: '/pages/mine/index' }); },
    openProfileEdit() {
      this.editForm = {
        identityTag: this.profile.identityTag || '',
        identityTagLabel: this.profile.identityTagLabel || '',
        position: this.profile.position || '',
        company: this.profile.company || ''
      };
      this.profileEditing = true;
    },
    onTagChange(e) {
      const idx = Number(e.detail.value);
      const opt = this.identityOptions[idx];
      if (opt) {
        this.editForm.identityTag = opt.value;
        this.editForm.identityTagLabel = opt.label;
      }
    },
    async saveProfile() {
      try {
        const pf = await updateAiProfile({
          identityTag: this.editForm.identityTag || '',
          position: this.editForm.position || '',
          company: this.editForm.company || ''
        });
        this.profile = pf || this.profile;
        this.identityOptions = (pf && pf.identityOptions) || this.identityOptions;
        this.profileEditing = false;
        uni.showToast({ title: '已保存', icon: 'success' });
        this.load(true); // 画像变化后重新分析
      } catch (e) { /* 拦截器已提示 */ }
    }
  }
};
</script>

<style scoped>
.page { padding: 20rpx 0 60rpx; }

.login-guide { text-align: center; padding: 80rpx 40rpx; }
.lg-icon { font-size: 80rpx; margin-bottom: 20rpx; }
.lg-title { font-size: 36rpx; font-weight: 700; margin-bottom: 12rpx; }
.lg-desc { font-size: 26rpx; color: #999; margin-bottom: 40rpx; line-height: 1.6; }

.card { background: #fff; border-radius: 20rpx; padding: 24rpx; margin: 0 24rpx 20rpx; }
.card-title { font-size: 30rpx; font-weight: 600; margin-bottom: 16rpx; }

/* 画像卡 */
.profile-card { display: flex; align-items: center; justify-content: space-between; }
.pf-left { display: flex; align-items: center; flex: 1; min-width: 0; }
.pf-avatar {
  width: 88rpx; height: 88rpx; border-radius: 44rpx; background: #f0f4f8;
  display: flex; align-items: center; justify-content: center; font-size: 44rpx; margin-right: 20rpx;
}
.pf-name { font-size: 30rpx; font-weight: 600; display: flex; align-items: center; }
.pf-tag { font-size: 20rpx; color: #fff; background: var(--primary); border-radius: 12rpx; padding: 2rpx 10rpx; margin-left: 10rpx; }
.pf-sub { font-size: 24rpx; color: #999; margin-top: 6rpx; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }
.pf-edit { color: var(--primary-strong); font-size: 26rpx; padding: 8rpx 24rpx; border: 1rpx solid var(--primary-strong); border-radius: 30rpx; }

/* 指标 */
.kpi-grid { display: flex; margin-bottom: 20rpx; }
.kpi { flex: 1; text-align: center; }
.kpi-value { font-size: 44rpx; font-weight: 700; }
.kpi-value.good { color: #27ae60; }
.kpi-value.mid { color: #f39c12; }
.kpi-value.bad { color: #e74c3c; }
.kpi-unit { font-size: 24rpx; margin-left: 2rpx; }
.kpi-label { font-size: 24rpx; color: #999; margin-top: 6rpx; }
.kpi-rows { border-top: 1rpx solid #f5f5f7; }
.kpi-row { display: flex; justify-content: space-between; padding: 14rpx 8rpx; font-size: 26rpx; color: #666; }

/* AI 综述 */
.ai-badge { font-size: 20rpx; color: #fff; background: var(--primary); border-radius: 10rpx; padding: 4rpx 12rpx; }
.ai-badge.tpl { background: #bbb; }
.ai-summary { font-size: 26rpx; color: #444; line-height: 1.8; }
.ai-summary.placeholder { color: #bbb; text-align: center; padding: 30rpx 0; }
.ai-refresh { text-align: center; color: var(--primary-strong); font-size: 24rpx; margin-top: 16rpx; padding: 8rpx 0; }

/* 收入来源 */
.src-row { display: flex; align-items: center; padding: 12rpx 0; }
.src-name { width: 180rpx; font-size: 26rpx; color: #666; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }
.src-bar-wrap { flex: 1; height: 20rpx; background: #f0f4f8; border-radius: 10rpx; margin: 0 16rpx; overflow: hidden; }
.src-bar { height: 100%; background: var(--primary); border-radius: 10rpx; }
.src-val { width: 90rpx; text-align: right; font-size: 24rpx; color: #999; }

/* 风险 */
.risk-item { display: flex; padding: 16rpx 0; border-bottom: 1rpx solid #f5f5f7; }
.risk-dot { width: 16rpx; height: 16rpx; border-radius: 8rpx; margin: 12rpx 16rpx 0 4rpx; flex-shrink: 0; }
.risk-dot.high { background: #e74c3c; }
.risk-dot.mid { background: #f39c12; }
.risk-dot.low { background: #95a5a6; }
.risk-title { font-size: 28rpx; font-weight: 600; }
.risk-detail { font-size: 24rpx; color: #999; margin-top: 6rpx; line-height: 1.6; }

/* 建议 */
.sug-item { display: flex; padding: 16rpx 0; border-bottom: 1rpx solid #f5f5f7; }
.sug-icon { font-size: 40rpx; margin-right: 16rpx; }
.sug-title { font-size: 28rpx; font-weight: 600; }
.sug-detail { font-size: 24rpx; color: #999; margin-top: 6rpx; line-height: 1.6; }

/* 弹层 */
.mask { position: fixed; inset: 0; background: rgba(0,0,0,0.5); z-index: 99; display: flex; align-items: flex-end; }
.sheet { width: 100%; background: #fff; border-radius: 32rpx 32rpx 0 0; padding: 40rpx 32rpx calc(40rpx + env(safe-area-inset-bottom)); }
.sheet-title { font-size: 34rpx; font-weight: 600; text-align: center; margin-bottom: 32rpx; }
.field { display: flex; align-items: center; padding: 24rpx 0; border-bottom: 1rpx solid #f5f5f7; }
.field-label { width: 200rpx; font-size: 28rpx; color: #666; }
.field-input { flex: 1; font-size: 28rpx; text-align: right; }
.field-picker { flex: 1; font-size: 28rpx; text-align: right; color: #333; }
.pf-tip { font-size: 22rpx; color: #bbb; margin-top: 16rpx; }
.btn-primary { margin-top: 40rpx; }
.btn-cancel { margin-top: 20rpx; text-align: center; color: #999; font-size: 26rpx; height: 72rpx; line-height: 72rpx; }
.score-row { display: flex; align-items: center; gap: 24rpx; padding: 20rpx 0 8rpx; }
.score-num { font-size: 72rpx; font-weight: 700; line-height: 1; }
.score-num.good { color: #52c41a; }
.score-num.mid { color: #faad14; }
.score-num.bad { color: #e57373; }
.score-info { flex: 1; }
.score-label { font-size: 26rpx; font-weight: 600; }
.score-desc { font-size: 22rpx; color: #999; margin-top: 6rpx; }
.rp-period { font-size: 22rpx; color: #999; font-weight: 400; }
.cache-badge { font-size: 20rpx; color: #52c41a; background: #f6ffed; border-radius: 8rpx; padding: 4rpx 12rpx; margin-right: 12rpx; }
.rp-total { font-size: 22rpx; color: #999; }
.rp-item { display: flex; align-items: center; justify-content: space-between; padding: 22rpx 0; border-bottom: 1rpx solid #f5f5f7; }
.rp-item:last-of-type { border-bottom: none; }
.rp-month { font-size: 30rpx; font-weight: 600; }
.rp-snapshot { font-size: 20rpx; color: #999; margin-top: 6rpx; }
.rp-side { display: flex; align-items: center; gap: 20rpx; }
.rp-score { font-size: 26rpx; font-weight: 700; }
.rp-score.good { color: #52c41a; }
.rp-score.mid { color: #faad14; }
.rp-score.bad { color: #e57373; }
.rp-view { font-size: 22rpx; color: var(--primary-strong); }
.rp-more { text-align: center; font-size: 24rpx; color: #999; padding: 22rpx 0 6rpx; }
.range-tabs { display: flex; gap: 14rpx; margin-bottom: 20rpx; }
.range-tab { flex: 1; text-align: center; font-size: 24rpx; color: #666; background: #f5f6f8; border-radius: 12rpx; padding: 12rpx 0; }
.range-tab.on { color: #fff; background: var(--primary-strong, #26a69a); font-weight: 600; }
.range-hint { font-size: 22rpx; color: #999; font-weight: 400; }
</style>
