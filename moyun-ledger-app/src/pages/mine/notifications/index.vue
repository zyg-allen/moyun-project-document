<template>
  <view class="page" :style="themeVars">
    <NavBar title="消息" />

    <view class="filters">
      <view
        class="chip"
        :class="{ active: filter === '' }"
        @tap="changeFilter('')"
      >全部</view>
      <view
        class="chip"
        :class="{ active: filter === 'system' }"
        @tap="changeFilter('system')"
      >系统</view>
    </view>

    <view class="list" v-if="list.length">
      <view class="msg" v-for="n in list" :key="n.id" @tap="open(n)">
        <view class="dot" v-if="!n.isRead"></view>
        <view class="m-body">
          <view class="m-title" :class="{ unread: !n.isRead }">{{ n.title }}</view>
          <view class="m-content">{{ n.content }}</view>
          <view class="m-time">{{ fmt(n.createTime) }}</view>
        </view>
      </view>
    </view>
    <PageState
      v-else
      :loading="loading"
      :error="error"
      empty-text="暂无消息"
      empty-icon="🔔"
      error-text="消息加载失败"
      @retry="load(true)"
    />
    <view class="more" v-if="list.length && list.length >= total">{{ total ? '没有更多了' : '' }}</view>
  </view>
</template>

<script>
import NavBar from '@/components/NavBar/NavBar.vue';
import PageState from '@/components/PageState/PageState.vue';
import { useThemeStore } from '@/stores/theme';
import { listNotifications, markNotificationRead } from '@/api/ledger';
import { listOf, totalOf } from '@/utils/response';

export default {
  components: { NavBar, PageState },
  data() {
    return { list: [], pageNum: 1, pageSize: 20, total: 0, loading: false, error: false, filter: '' };
  },
  computed: {
    themeVars() { return useThemeStore().themeVars; }
  },
  onShow() {
    useThemeStore().restore();
    this.load(true);
  },
  onReachBottom() {
    if (this.list.length < this.total) this.load(false);
  },
  methods: {
    fmt(v) {
      if (!v) return '';
      return String(v).replace('T', ' ').slice(5, 16);
    },
    changeFilter(t) {
      this.filter = t;
      this.load(true);
    },
    async load(reset) {
      if (this.loading) return;
      this.loading = true;
      if (reset) { this.pageNum = 1; this.list = []; this.error = false; }
      try {
        const data = await listNotifications({
          pageNum: this.pageNum,
          pageSize: this.pageSize,
          type: this.filter || undefined
        }) || {};
        const rows = listOf(data);
        this.total = totalOf(data, rows.length);
        this.list = reset ? rows : this.list.concat(rows);
        if (rows.length) this.pageNum += 1;
      } catch (e) { this.error = true; }
      this.loading = false;
    },
    async open(n) {
      if (!n.isRead) {
        try { await markNotificationRead(n.id); n.isRead = true; } catch (e) { /* ignore */ }
      }
      let payload = {};
      try { payload = n.data ? JSON.parse(n.data) : {}; } catch (e) { payload = {}; }
      const biz = payload.bizType;
      if (biz === 'saving' && payload.planId) {
        uni.navigateTo({ url: '/pages/mine/savings/detail?id=' + payload.planId });
        return;
      }
      if (biz === 'feedback') {
        uni.navigateTo({ url: '/pages/mine/feedback/index' });
        return;
      }
      if (biz === 'memo') {
        uni.navigateTo({ url: '/pages/mine/memo/index' });
        return;
      }
      if (biz === 'store' || biz === 'inventory') {
        uni.navigateTo({ url: '/pages/warehouse/index' });
      }
    }
  }
};
</script>

<style scoped>
.page { min-height: 100vh; background: var(--surface); padding-bottom: 40rpx; }
.filters { display: flex; gap: 16rpx; padding: 24rpx; }
.chip {
  padding: 10rpx 28rpx; border-radius: 999rpx; font-size: 25rpx;
  background: var(--card); color: var(--text-sub); border: 1rpx solid var(--border);
}
.chip.active { background: var(--primary-soft); color: var(--primary-text); border-color: var(--primary); font-weight: 600; }
.list { padding: 0 24rpx; }
.msg {
  position: relative; display: flex; gap: 16rpx;
  background: var(--card); border-radius: 18rpx; padding: 26rpx; margin-bottom: 16rpx;
}
.dot { width: 14rpx; height: 14rpx; border-radius: 50%; background: var(--expense); margin-top: 10rpx; flex-shrink: 0; }
.m-body { flex: 1; min-width: 0; }
.m-title { font-size: 28rpx; color: var(--text-main); font-weight: 500; }
.m-title.unread { font-weight: 700; }
.m-content { font-size: 24rpx; color: var(--text-sub); line-height: 1.6; margin-top: 8rpx; }
.m-time { font-size: 21rpx; color: var(--text-muted); margin-top: 10rpx; }
.empty { text-align: center; color: var(--text-muted); font-size: 26rpx; padding: 140rpx 0; }\n.retry { display: inline-block; margin-top: 20rpx; padding: 10rpx 32rpx; border-radius: 999rpx; background: var(--primary-soft); color: var(--primary-text); font-size: 24rpx; }
.more { text-align: center; color: var(--text-muted); font-size: 22rpx; padding: 20rpx 0 40rpx; }
</style>
