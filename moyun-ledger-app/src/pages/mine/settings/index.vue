<template>
  <view class="page" :style="themeVars">
    <NavBar title="设置" />
    <!-- 主题切换 -->
    <view class="card">
      <view class="setting-title">主题风格</view>
      <view class="setting-sub">切换全局配色，立即生效并记住选择</view>
      <view class="theme-grid">
        <view v-for="t in themeList" :key="t.key" class="theme-cell" @tap="pickTheme(t.key)">
          <view class="theme-dot" :class="{ selected: current === t.key }" :style="{ background: t.color }">
            <text v-if="current === t.key" class="theme-check">✓</text>
          </view>
          <text class="theme-name" :class="{ selected: current === t.key }">{{ t.name }}</text>
        </view>
      </view>
    </view>

    <!-- 外观模式（日间/暗色/护眼）：注入 --bg/--card/--text-* 等外观层变量 -->
    <view class="card">
      <view class="setting-title">外观模式</view>
      <view class="setting-sub">日间 / 暗色 / 护眼，立即生效并记住选择</view>
      <view class="appearance-grid">
        <view
          v-for="a in appearanceList"
          :key="a.key"
          class="appearance-cell"
          :class="{ selected: appearanceKey === a.key }"
          @tap="pickAppearance(a.key)"
        >
          <view class="appearance-swatch" :class="'sw-' + a.key">
            <text v-if="appearanceKey === a.key" class="appearance-check">✓</text>
          </view>
          <text class="appearance-name" :class="{ selected: appearanceKey === a.key }">{{ a.name }}</text>
        </view>
      </view>
    </view>

    <view class="card">
      <view class="setting-row">
        <view class="flex-1">
          <view class="setting-name">隐私模式</view>
          <view class="setting-sub">首页与列表中金额显示为 ****</view>
        </view>
        <switch :checked="privacy" @change="onPrivacyChange" style="transform: scale(0.85)" />
      </view>
    </view>

    <!-- 邮件提醒：定时记账执行结果的邮件推送 -->
    <view class="card">
      <view class="setting-row">
        <view class="flex-1">
          <view class="setting-name">邮件提醒</view>
          <view class="setting-sub">定时记账执行成功或失败（如余额不足）时发邮件通知</view>
        </view>
        <switch :checked="mailNotify" @change="onMailNotifyChange" style="transform: scale(0.85)" />
      </view>

      <view class="mail-row">
        <text class="mail-label">接收邮箱</text>
        <text class="mail-value" :class="{ empty: !email }">{{ email || '未绑定' }}</text>
      </view>
      <view class="mail-tip" v-if="!email">未绑定邮箱则收不到提醒，请先绑定</view>

      <view class="mail-edit" v-if="editingMail">
        <input class="mail-input" v-model="emailInput" placeholder="请输入邮箱地址" />
        <view class="mail-btn" @tap="saveEmail">保存</view>
        <view class="mail-btn ghost" @tap="cancelEditMail">取消</view>
      </view>
      <view class="mail-edit" v-else>
        <view class="mail-btn" @tap="startEditMail">{{ email ? '修改邮箱' : '绑定邮箱' }}</view>
      </view>
    </view>

    <view class="card">
      <view class="about">{{ appVersionText }}</view>
      <view class="about-sub">个人资产管理 · 一个数字看清全部身家</view>
      <view class="about-sub">数据与墨韵门户账号云端同步</view>
    </view>
  </view>
</template>

<script>
import { useThemeStore } from '@/stores/theme';
import { useUserStore } from '@/stores/user';
import { APP_VERSION_TEXT } from '@/utils/appInfo';
import { getUserInfo, updateProfile } from '@/api/ledger';

export default {
  computed: {
    themeVars() { return useThemeStore().themeVars; },
    themeList() { return useThemeStore().themeList; },
    appearanceList() { return useThemeStore().appearanceList; },
    appearanceKey() { return useThemeStore().appearanceKey; },
    current() { return useThemeStore().themeKey; },
    /** 版本号统一来自 utils/appInfo.js（此前这里与「更多」页各写一份且不一致） */
    appVersionText() { return APP_VERSION_TEXT; }
  },
  data() {
    return {
      privacy: false,
      // 邮件提醒（定时记账执行结果推送）
      email: '',
      mailNotify: false,
      editingMail: false,
      emailInput: ''
    };
  },
  onShow() {
    // uni-app H5 每个页面各自新建 uni-page-body：必须每次进入都重新注入主题，
    // 否则从别的页跳进来时，本页最外层仍是 page{} 的日间默认（暗色下发白）。
    useThemeStore().applyToPageBody();
    this.privacy = uni.getStorageSync('ledger_privacy') === '1';
    this.loadMailSettings();
  },
  methods: {
    /**
     * 读取邮箱与邮件提醒开关。
     * 必须现调 /portal/user/me：stores/user 只持久化 token，userInfo 冷启动即丢失，
     * 不能依赖登录时那一份（否则会误显示"未绑定"）。
     */
    async loadMailSettings() {
      if (!useUserStore().isLoggedIn) return;
      try {
        const u = await getUserInfo();
        if (!u) return;
        this.email = u.email || '';
        this.mailNotify = u.notifySchedule === true;
      } catch (e) { /* 拦截器已提示 */ }
    },
    async onMailNotifyChange(e) {
      const on = e.detail.value;
      // 未绑邮箱时不允许打开开关 ——「显示已开启却收不到」是最糟的状态，
      // 直接展开输入框引导绑定（开关由 :checked 受控，不更新就不会亮）
      if (on && !this.email) {
        this.editingMail = true;
        uni.showToast({ title: '请先绑定接收邮箱', icon: 'none' });
        return;
      }
      await this.saveNotify(on);
    },
    async saveNotify(on) {
      try {
        await updateProfile({ notifySchedule: on });
        this.mailNotify = on;
        uni.showToast({ title: on ? '已开启邮件提醒' : '已关闭邮件提醒', icon: 'none' });
      } catch (e) { /* 拦截器已提示 */ }
    },
    startEditMail() {
      this.emailInput = this.email;
      this.editingMail = true;
    },
    cancelEditMail() {
      this.editingMail = false;
      this.emailInput = '';
    },
    async saveEmail() {
      const v = (this.emailInput || '').trim();
      if (!/^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(v)) {
        uni.showToast({ title: '邮箱格式不正确', icon: 'none' });
        return;
      }
      try {
        await updateProfile({ email: v });
        this.email = v;
        this.editingMail = false;
        // 用户点「绑定邮箱」本就是为了收提醒：绑定成功后顺手开启（仅在未开时）
        if (!this.mailNotify) {
          await this.saveNotify(true);
          uni.showToast({ title: '已绑定并开启提醒', icon: 'success' });
        } else {
          uni.showToast({ title: '已保存', icon: 'success' });
        }
      } catch (e) { /* 拦截器已提示（含邮箱已被占用） */ }
    },
    pickTheme(key) {
      useThemeStore().setTheme(key);
      uni.showToast({ title: '已切换主题', icon: 'none' });
    },
    /** 切换外观模式（日间/暗色/护眼）；页面根元素由 themeVars 自动跟随 */
    pickAppearance(key) {
      useThemeStore().setAppearance(key);
      uni.showToast({ title: '已切换外观', icon: 'none' });
    },
    onPrivacyChange(e) {
      this.privacy = e.detail.value;
      uni.setStorageSync('ledger_privacy', e.detail.value ? '1' : '0');
      uni.showToast({ title: e.detail.value ? '已开启' : '已关闭', icon: 'none' });
    }
  }
};
</script>

<style scoped>
.page { padding: 24rpx; }
.card { background: var(--card); border-radius: 20rpx; padding: 32rpx; margin-bottom: 24rpx; }
.setting-title { font-size: 30rpx; font-weight: 600; }
.setting-sub { font-size: 24rpx; color: var(--text-sub); margin-top: 8rpx; }
.setting-row { display: flex; align-items: center; padding: 8rpx 0; }
.setting-name { font-size: 30rpx; font-weight: 500; }
.theme-grid { display: flex; flex-wrap: wrap; margin-top: 28rpx; }
.theme-cell { width: 20%; display: flex; flex-direction: column; align-items: center; }
.theme-dot {
  width: 84rpx; height: 84rpx; border-radius: 50%;
  display: flex; align-items: center; justify-content: center;
  box-shadow: 0 4rpx 12rpx rgba(0,0,0,0.12);
}
.theme-dot.selected { border: 6rpx solid #fff; outline: 3rpx solid var(--primary-strong); }
/* 打勾位于「主题色圆点」上（底色即强调色）→ 用 --on-primary 深色勾，白勾在浅强调色上仅 2.1~3.8:1 */
.theme-check { color: var(--on-primary); font-size: 36rpx; font-weight: 700; }
.theme-name { font-size: 22rpx; color: var(--text-sub); margin-top: 12rpx; }
.theme-name.selected { color: var(--primary-text); font-weight: 600; }
.about { text-align: center; font-size: 30rpx; font-weight: 600; padding: 24rpx 0 12rpx; }
.about-sub { text-align: center; font-size: 22rpx; color: var(--text-sub); padding: 4rpx 0; }

/* ---- 邮件提醒 ---- */
.mail-row {
  display: flex; align-items: center; justify-content: space-between;
  padding-top: 20rpx; margin-top: 16rpx; border-top: 1rpx solid var(--border);
}
.mail-label { font-size: 26rpx; color: var(--text-sub); }
.mail-value { font-size: 26rpx; color: var(--text-main); }
.mail-value.empty { color: var(--text-muted); }
.mail-tip { font-size: 22rpx; color: var(--warning); padding-top: 10rpx; }
.mail-edit { display: flex; align-items: center; gap: 16rpx; padding-top: 20rpx; }
.mail-input {
  flex: 1; min-width: 0; font-size: 26rpx; padding: 16rpx 20rpx;
  background: var(--surface); border-radius: 12rpx; color: var(--text-main);
}
.mail-btn {
  flex-shrink: 0; font-size: 26rpx; padding: 16rpx 28rpx; border-radius: 12rpx;
  background: var(--primary); color: var(--on-primary);
}
.mail-btn.ghost { background: var(--surface); color: var(--text-sub); }

/* ---------- 外观模式选择（日间/暗色/护眼） ---------- */
.appearance-grid { display: flex; gap: 24rpx; margin-top: 24rpx; }
.appearance-cell {
  flex: 1; display: flex; flex-direction: column; align-items: center; gap: 12rpx;
  /* 热区 88rpx 以上，避免点不中 */
  min-height: 88rpx; justify-content: center;
}
.appearance-swatch {
  width: 96rpx; height: 96rpx; border-radius: 20rpx;
  display: flex; align-items: center; justify-content: center;
  border: 3rpx solid var(--border);
  position: relative;
}
.appearance-cell.selected .appearance-swatch { border-color: var(--primary-strong); }
/* 用真实外观色做预览，用户一眼可辨 */
.appearance-swatch.sw-light { background: #F7F8FA; }
.appearance-swatch.sw-dark { background: #0F1419; }
.appearance-swatch.sw-eye { background: #F4F1EA; }
.appearance-check { font-size: 40rpx; font-weight: 700; color: var(--primary-text); }
.appearance-swatch.sw-dark .appearance-check { color: #5B8DEF; }
.appearance-name { font-size: 24rpx; color: var(--text-sub); }
.appearance-name.selected { color: var(--primary-text); font-weight: 600; }
</style>
