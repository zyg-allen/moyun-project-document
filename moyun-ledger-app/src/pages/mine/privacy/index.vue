<template>
  <view class="page" :style="themeVars">
    <NavBar title="隐私与授权" />

    <view class="card">
      <view class="notice-title">为什么需要授权？</view>
      <view class="notice-text">{{ policy.notice || '授权后，平台客服仅在你选择的范围内查看数据；每次查看都会留痕，到期自动失效，可随时撤回。' }}</view>
      <view class="notice-version">告知版本：{{ policy.version || 'v1.0' }}</view>
    </view>

    <!-- 当前授权状态 -->
    <view class="card" v-if="current.granted">
      <view class="card-title">当前授权</view>
      <view class="row">
        <text class="row-label">范围</text>
        <text class="row-value">{{ currentLabels }}</text>
      </view>
      <view class="row">
        <text class="row-label">用途</text>
        <text class="row-value">{{ current.purpose || '—' }}</text>
      </view>
      <view class="row">
        <text class="row-label">到期</text>
        <text class="row-value">{{ current.expireAt || '长期' }}</text>
      </view>
      <view class="btn danger" @tap="onRevoke">撤回授权</view>
    </view>

    <!-- 选择范围 -->
    <view class="card">
      <view class="card-title">选择授权范围</view>
      <view class="scope" v-for="s in policy.scopes" :key="s.key" @tap="toggleScope(s.key)">
        <view class="scope-check" :class="{ on: selected.indexOf(s.key) >= 0 }">
          <text v-if="selected.indexOf(s.key) >= 0">✓</text>
        </view>
        <view class="flex-1">
          <view class="scope-label">{{ s.label }}</view>
          <view class="scope-desc">{{ s.desc }}</view>
        </view>
      </view>
      <view class="hint">默认不勾选；只勾选本次确实需要的范围。</view>
    </view>

    <!-- 用途与期限 -->
    <view class="card">
      <view class="card-title">用途说明（后台可见）</view>
      <input class="purpose" v-model="purpose" placeholder="如：客服核对 10 月账单" maxlength="200" />
      <view class="card-title mt">有效期限</view>
      <view class="pills">
        <view class="pill" :class="{ on: hours === 24 }" @tap="hours = 24">24 小时</view>
        <view class="pill" :class="{ on: hours === 168 }" @tap="hours = 168">7 天</view>
      </view>
      <view class="hint">到期后自动失效，无需手动撤回。</view>
    </view>

    <view class="btn primary" :class="{ disabled: saving }" @tap="onGrant">
      {{ saving ? '提交中…' : '确认授权' }}
    </view>
    <view class="foot">授权记录与每一次查看都会写入审计；后台不能代替你授权，也不能延长期限。</view>
  </view>
</template>

<script>
import NavBar from '@/components/NavBar/NavBar.vue';
import { useThemeStore } from '@/stores/theme';
import { getDataAuthPolicy, getDataAuth, grantDataAuth, revokeDataAuth } from '@/api/ledger';

export default {
  components: { NavBar },
  data() {
    return {
      policy: { version: '', scopes: [], notice: '' },
      current: { granted: false, scopes: [], purpose: '', expireAt: '' },
      selected: [],
      purpose: '',
      hours: 24,
      saving: false
    };
  },
  computed: {
    themeVars() { return useThemeStore().themeVars; },
    currentLabels() {
      const map = {};
      (this.policy.scopes || []).forEach(s => { map[s.key] = s.label; });
      const list = (this.current.scopes || []).map(k => map[k] || k);
      return list.length ? list.join('、') : '—';
    }
  },
  onShow() {
    useThemeStore().restore();
    this.loadPolicy();
    this.loadCurrent();
  },
  methods: {
    async loadPolicy() {
      try {
        const p = await getDataAuthPolicy() || {};
        this.policy = {
          version: p.version || '',
          scopes: Array.isArray(p.scopes) ? p.scopes : [],
          notice: p.notice || ''
        };
      } catch (e) { /* 网络异常时沿用空策略，页面仍可展示说明 */ }
    },
    async loadCurrent() {
      try {
        const d = await getDataAuth() || {};
        this.current = {
          granted: !!d.granted,
          scopes: Array.isArray(d.scopes) ? d.scopes : [],
          purpose: d.purpose || '',
          expireAt: d.expireAt || ''
        };
        if (this.current.granted && !this.selected.length) {
          this.selected = this.current.scopes.slice();
          this.purpose = this.current.purpose || '';
        }
      } catch (e) { /* 未登录/异常：保持未授权态 */ }
    },
    toggleScope(key) {
      const i = this.selected.indexOf(key);
      if (i >= 0) this.selected.splice(i, 1);
      else this.selected.push(key);
    },
    async onGrant() {
      if (this.saving) return;
      if (!this.selected.length) {
        uni.showToast({ title: '请至少选择一个授权范围', icon: 'none' });
        return;
      }
      if (!this.purpose.trim()) {
        uni.showToast({ title: '请填写授权用途', icon: 'none' });
        return;
      }
      this.saving = true;
      try {
        await grantDataAuth({
          scopes: this.selected,
          purpose: this.purpose.trim(),
          hours: this.hours
        });
        uni.showToast({ title: '授权成功', icon: 'success' });
        await this.loadCurrent();
      } catch (e) { /* 错误提示由 request 统一处理 */ }
      finally { this.saving = false; }
    },
    onRevoke() {
      uni.showModal({
        title: '撤回授权',
        content: '撤回后后台将无法再查看你的数据（已产生的访问日志保留）。确定撤回？',
        success: async (r) => {
          if (!r.confirm) return;
          try {
            await revokeDataAuth('用户主动撤回');
            uni.showToast({ title: '已撤回', icon: 'success' });
            this.selected = [];
            await this.loadCurrent();
          } catch (e) { /* 统一错误提示 */ }
        }
      });
    }
  }
};
</script>

<style scoped>
.page { min-height: 100vh; padding-bottom: 60rpx; }
.card {
  margin: 16rpx 24rpx 0; padding: 20rpx 24rpx;
  background: var(--card); border: 1rpx solid var(--border); border-radius: 16rpx;
}
.card-title { font-size: var(--fs-title); font-weight: 700; color: var(--text-main); padding: 8rpx 0; }
.notice-title { font-size: var(--fs-body); font-weight: 600; color: var(--text-main); }
.notice-text { font-size: var(--fs-unit); color: var(--text-sub); line-height: 1.7; margin-top: 8rpx; }
.notice-version { font-size: var(--fs-unit); color: var(--text-sub); margin-top: 8rpx; opacity: .7; }
.row { display: flex; align-items: flex-start; padding: 14rpx 0; border-top: 1rpx solid var(--border); }
.row-label { width: 110rpx; font-size: var(--fs-unit); color: var(--text-sub); }
.row-value { flex: 1; font-size: var(--fs-body); color: var(--text-main); word-break: break-all; }
.scope { display: flex; align-items: flex-start; padding: 18rpx 0; border-top: 1rpx solid var(--border); }
.scope:first-of-type { border-top: none; }
.scope-check {
  width: 40rpx; height: 40rpx; border-radius: 10rpx; border: 2rpx solid var(--border);
  margin-right: 16rpx; margin-top: 4rpx; display: flex; align-items: center;
  justify-content: center; font-size: 26rpx; color: #fff;
}
.scope-check.on { background: var(--primary); border-color: var(--primary); }
.scope-label { font-size: var(--fs-body); color: var(--text-main); font-weight: 500; }
.scope-desc { font-size: var(--fs-unit); color: var(--text-sub); line-height: 1.6; margin-top: 4rpx; }
.purpose {
  margin-top: 12rpx; padding: 18rpx; border: 1rpx solid var(--border);
  border-radius: 12rpx; font-size: var(--fs-body); color: var(--text-main); background: var(--bg);
}
.mt { margin-top: 24rpx; }
.pills { display: flex; gap: 16rpx; margin-top: 12rpx; }
.pill {
  padding: 12rpx 28rpx; border: 1rpx solid var(--border); border-radius: 999rpx;
  font-size: var(--fs-unit); color: var(--text-sub);
}
.pill.on { border-color: var(--primary); color: var(--primary); background: var(--primary-soft); }
.hint { font-size: var(--fs-unit); color: var(--text-sub); margin-top: 12rpx; line-height: 1.6; }
.btn {
  margin: 28rpx 24rpx 0; padding: 24rpx 0; border-radius: 14rpx;
  text-align: center; font-size: var(--fs-body); font-weight: 600;
}
.btn.primary { background: var(--primary); color: var(--on-primary); }
.btn.danger { background: transparent; color: var(--expense); border: 1rpx solid var(--expense); margin-top: 20rpx; }
.btn.disabled { opacity: .6; }
.foot { margin: 20rpx 32rpx; font-size: var(--fs-unit); color: var(--text-sub); line-height: 1.7; text-align: center; }
</style>
