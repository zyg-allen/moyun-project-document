<template>
  <view class="page" :style="themeVars">
    <NavBar title="意见反馈" />

    <!-- Tab 切换 -->
    <view class="tab-bar">
      <view class="tab" :class="{ active: tab === 'form' }" @tap="tab = 'form'">提交反馈</view>
      <view class="tab" :class="{ active: tab === 'history' }" @tap="switchHistory">
        我的反馈<text v-if="total > 0" class="tab-badge">{{ total }}</text>
      </view>
    </view>

    <!-- ===== 提交表单 ===== -->
    <view class="form-card" v-if="tab === 'form'">
      <view class="f-row">
        <text class="f-label">反馈类型</text>
        <view class="type-group">
          <view class="type-tag" :class="{ active: form.feedbackType === k }" v-for="(lbl, k) in typeMap" :key="k" @tap="form.feedbackType = k">{{ lbl }}</view>
        </view>
      </view>
      <view class="f-row col">
        <text class="f-label">主题</text>
        <input class="f-input full" v-model="form.subject" placeholder="一句话概括问题或建议（选填）" maxlength="100" />
      </view>
      <view class="f-row col">
        <text class="f-label">详细描述 <text class="required">*</text></text>
        <textarea class="f-textarea" v-model="form.description" placeholder="请详细描述您遇到的问题或建议，我们会尽快处理（5-2000字）" maxlength="2000" />
      </view>
      <view class="f-row col">
        <text class="f-label">联系方式</text>
        <input class="f-input full" v-model="form.contact" placeholder="邮箱/QQ/微信（选填，便于回复您）" maxlength="50" />
      </view>
      <view class="f-tip">反馈将同步到墨韵门户后台，处理结果可在「我的反馈」中查看</view>
      <view class="submit-btn" @tap="submit">提交反馈</view>
    </view>

    <!-- ===== 历史列表 ===== -->
    <view class="history" v-if="tab === 'history'">
      <view class="fb-list" v-if="records.length">
        <view class="fb-card" v-for="f in records" :key="f.id" @tap="toggleExpand(f)">
          <view class="fb-head">
            <text class="fb-type" :class="f.feedbackType">{{ typeMap[f.feedbackType] || f.feedbackType }}</text>
            <text class="fb-status" :class="f.status">{{ statusText(f.status) }}</text>
          </view>
          <view class="fb-subject">{{ f.subject || f.description }}</view>
          <view class="fb-desc" v-if="f.subject && expandedId === f.id">{{ f.description }}</view>
          <view class="fb-time">{{ f.createTime }}</view>
          <!-- 处理结果 -->
          <view class="fb-result" v-if="expandedId === f.id && f.handleResult">
            <text class="fb-result-label">处理回复：</text>
            <text class="fb-result-text">{{ f.handleResult }}</text>
            <view class="fb-result-time" v-if="f.handleTime">处理时间：{{ f.handleTime }}</view>
          </view>
          <view class="fb-expand">{{ expandedId === f.id ? '收起 ▲' : '展开查看 ▼' }}</view>
        </view>
        <view class="load-more" v-if="records.length < total" @tap="loadMore">加载更多（{{ records.length }}/{{ total }}）</view>
      </view>
      <view class="empty" v-else>
        <text class="empty-icon">💬</text>
        <text class="empty-text">暂无反馈记录</text>
        <view class="empty-btn" @tap="tab = 'form'">去提交反馈</view>
      </view>
    </view>
  </view>
</template>

<script>
import NavBar from '@/components/NavBar/NavBar.vue';
import { useThemeStore } from '@/stores/theme';
import { useUserStore } from '@/stores/user';
import { submitFeedback, listMyFeedback } from '@/api/ledger';

const TYPE_MAP = { suggestion: '功能建议', bug: 'Bug反馈', experience: '体验问题', other: '其他' };

export default {
  components: { NavBar },
  data() {
    return {
      tab: 'form',
      typeMap: TYPE_MAP,
      form: { feedbackType: 'suggestion', subject: '', description: '', contact: '' },
      records: [],
      total: 0,
      page: 1,
      pageSize: 10,
      expandedId: null,
      submitting: false
    };
  },
  computed: {
    themeVars() { return useThemeStore().themeVars; }
  },
  onShow() {
    useThemeStore().restore();
  },
  methods: {
    switchHistory() {
      this.tab = 'history';
      if (useUserStore().isLoggedIn && this.records.length === 0) this.load(true);
    },
    async load(reset) {
      if (!useUserStore().isLoggedIn) {
        uni.showToast({ title: '请先在「我的」页登录', icon: 'none' });
        return;
      }
      if (reset) { this.page = 1; this.records = []; }
      try {
        const data = await listMyFeedback({ page: this.page, pageSize: this.pageSize }) || {};
        const list = data.records || [];
        this.records = reset ? list : this.records.concat(list);
        this.total = Number(data.total || 0);
      } catch (e) { /* 拦截器已提示 */ }
    },
    loadMore() { this.page++; this.load(false); },
    toggleExpand(f) { this.expandedId = this.expandedId === f.id ? null : f.id; },
    statusText(s) {
      return { pending: '待处理', processing: '处理中', resolved: '已解决', rejected: '已驳回' }[s] || s;
    },
    async submit() {
      if (!useUserStore().isLoggedIn) {
        uni.showToast({ title: '请先在「我的」页登录', icon: 'none' });
        return;
      }
      const desc = this.form.description.trim();
      if (desc.length < 5) { uni.showToast({ title: '请至少输入5个字的描述', icon: 'none' }); return; }
      if (this.submitting) return;
      this.submitting = true;
      try {
        await submitFeedback({
          feedbackType: this.form.feedbackType,
          subject: this.form.subject.trim() || null,
          description: desc,
          contact: this.form.contact.trim() || null
        });
        uni.showToast({ title: '提交成功，感谢反馈', icon: 'success' });
        this.form = { feedbackType: 'suggestion', subject: '', description: '', contact: '' };
        this.records = []; this.total = 0; // 清缓存，下次进历史重新拉
      } catch (e) { /* 拦截器已提示 */ }
      this.submitting = false;
    }
  }
};
</script>

<style scoped>
.page { padding-bottom: 60rpx; min-height: 100vh; background: #f5f6f8; }

.tab-bar {
  display: flex; background: #fff; margin: 24rpx 24rpx 0; border-radius: 20rpx; padding: 8rpx;
}
.tab {
  flex: 1; text-align: center; padding: 20rpx 0; font-size: 28rpx; color: #666;
  border-radius: 16rpx; position: relative;
}
.tab.active { background: var(--primary-strong); color: #fff; font-weight: 600; }
.tab-badge {
  position: absolute; top: 8rpx; right: 24rpx; min-width: 32rpx; height: 32rpx;
  line-height: 32rpx; border-radius: 16rpx; background: #e57373; color: #fff;
  font-size: 20rpx; padding: 0 8rpx;
}

.form-card { background: #fff; margin: 24rpx; border-radius: 20rpx; padding: 8rpx 32rpx 32rpx; }
.f-row { padding: 24rpx 0; border-bottom: 1rpx solid #f5f5f7; }
.f-row:last-child { border-bottom: none; }
.f-row.col { display: flex; flex-direction: column; gap: 16rpx; }
.f-label { font-size: 26rpx; color: #333; }
.required { color: #e57373; }
.type-group { display: flex; gap: 14rpx; margin-top: 16rpx; flex-wrap: wrap; }
.type-tag {
  font-size: 24rpx; padding: 10rpx 28rpx; border-radius: 28rpx;
  background: #f5f6f8; color: #666;
}
.type-tag.active { background: var(--primary-strong); color: #fff; }
.f-input { font-size: 26rpx; background: #f8f9fa; border-radius: 12rpx; height: 72rpx; line-height: 72rpx; padding: 0 20rpx; box-sizing: border-box; }
.f-input.full { width: 100%; box-sizing: border-box; }
.f-textarea {
  width: 100%; min-height: 200rpx; font-size: 26rpx; background: #f8f9fa;
  border-radius: 12rpx; padding: 20rpx; box-sizing: border-box;
}
.f-tip { font-size: 22rpx; color: #999; padding: 20rpx 0; }
.submit-btn {
  height: 84rpx; line-height: 84rpx; text-align: center;
  background: var(--primary-strong); color: #fff; border-radius: 42rpx;
  font-size: 28rpx; font-weight: 600;
}
.submit-btn:active { opacity: 0.85; }

.fb-list { padding: 0 24rpx; }
.fb-card { background: #fff; border-radius: 16rpx; padding: 24rpx; margin-bottom: 16rpx; }
.fb-head { display: flex; justify-content: space-between; align-items: center; }
.fb-type {
  font-size: 20rpx; padding: 4rpx 16rpx; border-radius: 14rpx;
  background: var(--primary-soft); color: var(--primary-strong);
}
.fb-type.bug { background: #fff1f0; color: #e57373; }
.fb-status { font-size: 22rpx; }
.fb-status.pending { color: #faad14; }
.fb-status.processing { color: #1677ff; }
.fb-status.resolved { color: #52c41a; }
.fb-status.rejected { color: #999; }
.fb-subject {
  font-size: 28rpx; color: #333; font-weight: 600; margin-top: 14rpx;
  overflow: hidden; text-overflow: ellipsis; white-space: nowrap;
}
.fb-desc { font-size: 24rpx; color: #666; margin-top: 12rpx; line-height: 1.6; }
.fb-time { font-size: 20rpx; color: #bbb; margin-top: 12rpx; }
.fb-result {
  margin-top: 16rpx; background: #f6ffed; border: 1rpx solid #b7eb8f;
  border-radius: 12rpx; padding: 16rpx 20rpx;
}
.fb-result-label { font-size: 24rpx; color: #52c41a; font-weight: 600; }
.fb-result-text { font-size: 24rpx; color: #333; line-height: 1.6; }
.fb-result-time { font-size: 20rpx; color: #999; margin-top: 8rpx; }
.fb-expand { font-size: 22rpx; color: var(--primary-strong); text-align: center; margin-top: 14rpx; }

.load-more { text-align: center; color: #999; font-size: 24rpx; padding: 24rpx 0; }

.empty { display: flex; flex-direction: column; align-items: center; padding: 120rpx 0; }
.empty-icon { font-size: 100rpx; opacity: 0.5; margin-bottom: 24rpx; }
.empty-text { font-size: 28rpx; color: #999; margin-bottom: 32rpx; }
.empty-btn {
  padding: 16rpx 48rpx; background: var(--primary-strong); color: #fff;
  border-radius: 32rpx; font-size: 26rpx;
}
</style>