<template>
  <view class="page" :style="themeVars">
    <!-- 用户卡 -->
    <view class="user-card">
      <view class="avatar">{{ userStore.isLoggedIn ? '墨' : '?' }}</view>
      <view class="flex-1">
        <view class="user-name">{{ userStore.isLoggedIn ? (userStore.username || '已登录') : '未登录' }}</view>
        <view class="user-sub">最近备份：{{ lastBackup }} · 本地</view>
      </view>
      <view class="user-right">
        <view class="sign-btn" @tap="doSign">{{ signed ? '已签到' : '签到' }}</view>
        <view class="setting-btn" @tap="go('/pages/mine/settings/index')">⚙</view>
      </view>
    </view>

    <!-- 登录表单（未登录时显示） -->
    <view class="card" v-if="!userStore.isLoggedIn">
      <view class="field">
        <text class="field-label">账号</text>
        <input v-model="loginForm.username" placeholder="门户账号/邮箱" class="field-input" />
      </view>
      <view class="field">
        <text class="field-label">密码</text>
        <input v-model="loginForm.password" type="password" placeholder="密码" class="field-input" />
      </view>
      <view class="field" v-if="captcha.enabled">
        <text class="field-label">验证码</text>
        <view class="captcha-row">
          <input v-model="captcha.code" placeholder="请输入验证码" class="field-input captcha-input" />
          <image v-if="captcha.img" :src="captcha.img" mode="aspectFit" class="captcha-img" @tap="loadCaptcha" />
          <view v-else class="captcha-img captcha-loading" @tap="loadCaptcha"><text>加载中</text></view>
        </view>
      </view>
      <view class="btn-primary" @tap="doLogin">登录</view>
      <view class="login-tip">与墨韵门户共用账号体系，首次使用请先在门户注册</view>
    </view>

    <!-- 功能宫格（已登录） -->
    <block v-if="userStore.isLoggedIn">
      <view class="grid-card">
        <view class="grid-cell" v-for="m in mainMenus" :key="m.key" @tap="onMenuTap(m)">
          <view class="cell-icon" :style="{ color: m.color }">{{ m.icon }}</view>
          <text class="cell-label">{{ m.label }}</text>
          <text v-if="m.badge" class="cell-badge">{{ m.badge }}</text>
        </view>
      </view>

      <!-- 推荐小功能 -->
      <view class="section-title">推荐小功能</view>
      <view class="grid-card">
        <view class="grid-cell" v-for="m in recommendMenus" :key="m.key" @tap="onMenuTap(m)">
          <view class="cell-icon" :style="{ color: m.color }">{{ m.icon }}</view>
          <text class="cell-label">{{ m.label }}</text>
        </view>
      </view>

      <!-- 退出 -->
      <view class="card" style="margin-top: 24rpx;">
        <view class="menu-item logout" @tap="doLogout">退出登录</view>
      </view>
    </block>
  </view>
</template>

<script>
import { login, getCaptchaImage } from '@/api/ledger';
import { useUserStore } from '@/stores/user';
import { useThemeStore } from '@/stores/theme';
import { storage } from '@/utils/storage';

export default {
  data() {
    return {
      loginForm: { username: '', password: '' },
      captcha: { enabled: true, uuid: '', img: '', code: '' },
      signed: false,
      lastBackup: '从未',
      // 主功能宫格
      mainMenus: [
        { key: 'auto', icon: '🗒️', label: '自动记账', color: '#7fbf94' },
        { key: 'backup', icon: '☁️', label: '数据备份', color: '#7fbf94' },
        { key: 'import', icon: '⬇️', label: '导入数据', color: '#7fbf94' },
        { key: 'export', icon: '⬆️', label: '导出数据', color: '#7fbf94' },
        { key: 'widget', icon: '▦', label: '小组件', color: '#7fbf94' },
        { key: 'personalize', icon: '🎨', label: '个性化', color: '#e57373', badge: 'NEW' },
        { key: 'catIcon', icon: '🎭', label: '分类图标', color: '#7fbf94', badge: 'NEW' },
        { key: 'category', icon: '☰', label: '分类管理', color: '#7fbf94' },
        { key: 'tag', icon: '🏷️', label: '标签管理', color: '#7fbf94' },
        { key: 'setting', icon: '⚙️', label: '记账设置', color: '#7fbf94' },
        { key: 'remind', icon: '🔔', label: '记账提醒', color: '#7fbf94' },
        { key: 'reimburse', icon: '🧾', label: '报销账单', color: '#7fbf94' },
        { key: 'savings', icon: '🏦', label: '存钱计划', color: '#7fbf94' },
        { key: 'schedule', icon: '⏰', label: '定时记账', color: '#7fbf94' },
        { key: 'feedback', icon: '💬', label: '意见反馈', color: '#7fbf94' },
        { key: 'share', icon: '📤', label: '分享应用', color: '#7fbf94', badge: 'NEW' },
        { key: 'rate', icon: '⭐', label: '给个好评', color: '#7fbf94' },
        { key: 'tip', icon: '🎁', label: '赞赏', color: '#7fbf94' },
        { key: 'qq', icon: '👥', label: 'QQ群', color: '#7fbf94' }
      ],
      // 推荐小功能
      recommendMenus: [
        { key: 'memo', icon: '📝', label: '备忘录', color: '#7fbf94' },
        { key: 'translate', icon: '文A', label: '翻译', color: '#7fbf94' },
        { key: 'list', icon: '☑', label: '清单', color: '#7fbf94' },
        { key: 'stock', icon: '📦', label: '库存管理', color: '#7fbf94' },
        { key: 'gold', icon: '💰', label: '记黄金', color: '#7fbf94' },
        { key: 'coupon', icon: '🎫', label: '优惠券', color: '#7fbf94' }
      ]
    };
  },
  computed: {
    themeVars() { return useThemeStore().themeVars; },
    userStore() { return useUserStore(); }
  },
  onShow() {
    useThemeStore().restore();
    if (!useUserStore().isLoggedIn && this.captcha.enabled && !this.captcha.img) {
      this.loadCaptcha();
    }
    // 签到状态 & 备份时间
    const today = new Date().toDateString();
    this.signed = storage.get('signed_date') === today;
    this.lastBackup = storage.get('last_backup') || '从未';
  },
  methods: {
    async loadCaptcha() {
      try {
        const data = await getCaptchaImage();
        this.captcha.enabled = data.captchaEnabled !== false;
        this.captcha.uuid = data.uuid || '';
        this.captcha.img = data.img ? 'data:image/jpeg;base64,' + data.img : '';
        this.captcha.code = '';
      } catch (e) { this.captcha.img = ''; }
    },
    async doLogin() {
      if (!this.loginForm.username || !this.loginForm.password) {
        uni.showToast({ title: '请输入账号密码', icon: 'none' }); return;
      }
      if (this.captcha.enabled && !this.captcha.code) {
        uni.showToast({ title: '请输入验证码', icon: 'none' }); return;
      }
      try {
        const payload = { ...this.loginForm };
        if (this.captcha.enabled) { payload.code = this.captcha.code; payload.uuid = this.captcha.uuid; }
        const data = await login(payload);
        const token = data && data.token ? data.token : data;
        if (!token) { uni.showToast({ title: '登录失败：未获取到令牌', icon: 'none' }); return; }
        this.userStore.setToken(token);
        uni.showToast({ title: '登录成功', icon: 'success' });
      } catch (e) { this.loadCaptcha(); }
    },
    doLogout() {
      uni.showModal({ title: '提示', content: '确定退出登录？', success: (r) => { if (r.confirm) this.userStore.logout(); } });
    },
    doSign() {
      const today = new Date().toDateString();
      if (storage.get('signed_date') === today) {
        uni.showToast({ title: '今日已签到', icon: 'none' });
        return;
      }
      storage.set('signed_date', today);
      this.signed = true;
      uni.showToast({ title: '签到成功', icon: 'success' });
    },
    onMenuTap(m) {
      const routes = {
        category: '/pages/mine/categories/index',
        budget: '/pages/mine/budget/index',
        list: '/pages/record/list',
        report: '/pages/report/index',
        setting: '/pages/mine/settings/index',
        savings: '/pages/mine/savings/index',
        schedule: '/pages/mine/schedule/index',
        tip: '/pages/mine/tip/index',
        memo: '/pages/mine/memo/index'
      };
      const url = routes[m.key];
      if (url) { uni.navigateTo({ url }); return; }
      // 占位功能提示
      const todo = ['auto', 'backup', 'import', 'export', 'widget', 'personalize', 'catIcon',
        'tag', 'remind', 'reimburse', 'feedback', 'share', 'rate', 'qq',
        'translate', 'list', 'stock', 'gold', 'coupon'];
      if (todo.includes(m.key)) {
        uni.showToast({ title: m.label + ' · 开发中', icon: 'none' });
      }
    },
    go(url) { uni.navigateTo({ url }); }
  }
};
</script>

<style scoped>
.page { padding-bottom: 40rpx; }
.user-card {
  display: flex; align-items: center; background: var(--primary);
  color: #fff; padding: 60rpx 40rpx 32rpx;
  padding-top: calc(env(safe-area-inset-top) + 60rpx);
}
.avatar {
  width: 110rpx; height: 110rpx; border-radius: 55rpx; background: rgba(255,255,255,0.25);
  display: flex; align-items: center; justify-content: center; font-size: 44rpx; margin-right: 24rpx;
}
.user-name { font-size: 34rpx; font-weight: 600; }
.user-sub { font-size: 22rpx; opacity: 0.85; margin-top: 8rpx; }
.user-right { display: flex; align-items: center; gap: 16rpx; }
.sign-btn {
  font-size: 24rpx; padding: 8rpx 24rpx; border-radius: 28rpx;
  background: rgba(255,255,255,0.25); color: #fff;
}
.setting-btn {
  width: 56rpx; height: 56rpx; border-radius: 28rpx;
  background: rgba(255,255,255,0.25); display: flex; align-items: center; justify-content: center;
  font-size: 30rpx;
}

.card { background: #fff; border-radius: 20rpx; padding: 8rpx 32rpx; margin: 0 24rpx 24rpx; }
.field { display: flex; align-items: center; padding: 28rpx 0; border-bottom: 1rpx solid #f5f5f7; }
.field-label { width: 140rpx; font-size: 28rpx; color: #666; }
.field-input { flex: 1; font-size: 28rpx; text-align: right; }
.captcha-row { display: flex; align-items: center; gap: 16rpx; flex: 1; }
.captcha-input { flex: 1; }
.captcha-img { width: 200rpx; height: 72rpx; border-radius: 10rpx; background: #f5f5f5; flex-shrink: 0; }
.captcha-loading { display: flex; align-items: center; justify-content: center; color: #999; font-size: 22rpx; }
.btn-primary { margin: 32rpx 0 16rpx; }
.login-tip { font-size: 22rpx; color: #999; padding-bottom: 16rpx; text-align: center; }

/* 功能宫格 */
.grid-card {
  background: #fff; border-radius: 20rpx; padding: 24rpx 12rpx;
  margin: 24rpx 24rpx 0;
}
.grid-cell {
  width: 25%; display: flex; flex-direction: column; align-items: center;
  padding: 20rpx 0; position: relative;
}
.cell-icon {
  width: 80rpx; height: 80rpx; border-radius: 20rpx;
  display: flex; align-items: center; justify-content: center;
  font-size: 38rpx; background: var(--primary-soft);
}
.cell-label { font-size: 24rpx; color: #555; margin-top: 12rpx; }
.cell-badge {
  position: absolute; top: 12rpx; right: 16rpx;
  font-size: 18rpx; color: #fff; background: #e57373;
  border-radius: 16rpx; padding: 0 8rpx; line-height: 28rpx;
}

.section-title {
  font-size: 28rpx; font-weight: 600; color: #333;
  padding: 32rpx 32rpx 12rpx;
}

.menu-item {
  display: flex; justify-content: center; align-items: center;
  padding: 32rpx 0; font-size: 28rpx; color: #e74c3c;
}
</style>
