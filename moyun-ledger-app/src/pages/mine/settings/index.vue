<template>
  <view class="page" :style="themeVars">
    <NavBar title="系统设置" />
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

    <!-- 功能开关：仓库管理（关闭后底部不显示「入库/出库」） -->
    <view class="card">
      <view class="setting-row">
        <view class="flex-1">
          <view class="setting-name">仓库管理</view>
          <view class="setting-sub">开启后底部出现「入库/出库」，可管理库存、货架、商品</view>
        </view>
        <switch :checked="warehouseOn" @change="onWarehouseChange" style="transform: scale(0.85)" />
      </view>
      <view class="setting-row" v-if="warehouseOn" @tap="goWarehouseCfg">
        <view class="flex-1">
          <view class="setting-name">仓库配置</view>
          <view class="setting-sub">商品类目 · 低库存阈值 · 列表显示项</view>
        </view>
        <text class="row-arrow">›</text>
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
      <view class="setting-row" @tap="goDataAuth">
        <view class="flex-1">
          <view class="setting-name">隐私与授权</view>
          <view class="setting-sub">授权客服查看指定范围数据（默认关闭，可随时撤回）</view>
        </view>
        <text class="row-arrow">›</text>
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

      <view class="mail-edit right">
        <view class="mail-btn" @tap="startEditMail">{{ email ? '修改邮箱' : '绑定邮箱' }}</view>
      </view>

      <!-- 服务状态 / 授权码帮助：URL 抽在 utils/appInfo.js，为空则不渲染，避免死链 -->
      <view class="mail-help" v-if="mailHelpUrl" @tap="openMailHelp">
        <text>如何获取 / 更新邮箱授权码？</text>
        <text class="mail-help-arrow">›</text>
      </view>
    </view>

    <view class="card">
      <view class="about">{{ appVersionText }}</view>
      <view class="about-sub">个人资产管理 · 一个数字看清全部身家</view>
      <view class="about-sub">数据与墨韵门户账号云端同步</view>
      <view class="legal-links">
        <text class="legal-link" @tap="openLegal('terms')">用户协议</text>
        <text class="legal-sep">·</text>
        <text class="legal-link" @tap="openLegal('privacy')">隐私政策</text>
      </view>
    </view>
    <!-- 修改接收邮箱：底部弹层（长表单可滚动） -->
    <view class="sheet-mask" v-if="mailSheet" @tap="cancelEditMail">
      <view class="sheet" @tap.stop>
        <view class="sheet-title">接收邮箱</view>
        <view class="sheet-tip">用于接收定时记账执行结果提醒</view>
        <input class="mail-input" v-model="emailInput" placeholder="请输入邮箱地址" />
        <view class="sheet-actions">
          <view class="mail-btn ghost" @tap="cancelEditMail">取消</view>
          <view class="mail-btn primary" @tap="saveEmail">保存</view>
        </view>
      </view>
    </view>

    <!-- 数据与账号（合规） -->
    <view class="card">
      <view class="setting-row" @tap="exportAll">
        <view class="flex-1">
          <view class="setting-name">导出全部数据</view>
          <view class="setting-sub">导出流水为 CSV，可用于备份或迁移</view>
        </view>
        <text class="row-arrow">›</text>
      </view>
      <view class="setting-row" @tap="goPermissions">
        <view class="flex-1">
          <view class="setting-name">权限管理</view>
          <view class="setting-sub">通知 / 麦克风 / 相册 / 定位</view>
        </view>
        <text class="row-arrow">›</text>
      </view>
      <view class="setting-row" @tap="closeAccount">
        <view class="flex-1">
          <view class="setting-name danger">注销账号</view>
          <view class="setting-sub">注销后手机号 / 邮箱 / 微信解绑并匿名化</view>
        </view>
        <text class="row-arrow">›</text>
      </view>
    </view>

  </view>
</template>

<script>
import { useThemeStore } from '@/stores/theme';
import { useUserStore } from '@/stores/user';
import { APP_VERSION_TEXT, MAIL_HELP_URL } from '@/utils/appInfo';
import { getUserInfo, updateProfile, deactivateAccount, exportTransactionsCsv } from '@/api/ledger';
import { isWarehouseEnabled, setWarehouseEnabled } from '@/utils/warehouseSwitch';

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
      warehouseOn: true,
      // 邮件提醒（定时记账执行结果推送）
      email: '',
      mailNotify: false,
      mailSheet: false,
      emailInput: '',
      // 邮件帮助外链（utils/appInfo.js 单一来源；为空则不渲染入口）
      mailHelpUrl: MAIL_HELP_URL
    };
  },
  onShow() {
    this.warehouseOn = isWarehouseEnabled();
    // uni-app H5 每个页面各自新建 uni-page-body：必须每次进入都重新注入主题，
    // 否则从别的页跳进来时，本页最外层仍是 page{} 的日间默认（暗色下发白）。
    useThemeStore().applyToPageBody();
    this.privacy = uni.getStorageSync('ledger_privacy') === '1';
    this.loadMailSettings();
  },
  methods: {
    onWarehouseChange(e) {
      this.warehouseOn = e.detail.value;
      setWarehouseEnabled(this.warehouseOn);
    },
    goWarehouseCfg() { uni.navigateTo({ url: '/pages/mine/warehouse-settings/index' }); },
    /** 隐私与授权：用户主动授权后台在限定范围内查看（合规入口） */
    goDataAuth() { uni.navigateTo({ url: '/pages/mine/privacy/index' }); },
    openLegal(type) { uni.navigateTo({ url: '/pages/mine/agreement/index?type=' + type }); },
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
        this.mailSheet = true;
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
    /**
     * 打开邮件帮助外链（授权码获取 / 服务状态）。
     *
     * 平台分流与 mine/index.vue 的链接型入口**保持一致**，
     * 这样后续接入 App / 小程序无需改动这里：
     *   H5         → window.open（web-view 在 H5 是 iframe，跨域受限，直开外链更可靠）
     *   App/小程序  → pages/webview/index?src=...
     */
    openMailHelp() {
      const url = this.mailHelpUrl;
      if (!url) return;
      // #ifdef H5
      window.open(url, '_blank');
      return;
      // #endif
      // #ifndef H5
      uni.navigateTo({ url: '/pages/webview/index?src=' + encodeURIComponent(url) });
      // #endif
    },
    startEditMail() {
      this.emailInput = this.email;
      this.mailSheet = true;
    },
    cancelEditMail() {
      this.mailSheet = false;
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
        this.mailSheet = false;
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
    },
    async exportAll() {
      try {
        await exportTransactionsCsv(null, null);
        uni.showToast({ title: '已开始导出', icon: 'none' });
      } catch (e) { /* 拦截器已提示 */ }
    },
    goPermissions() {
      uni.navigateTo({ url: '/pages/mine/permissions/index' });
    },
    closeAccount() {
      uni.showModal({
        title: '注销账号',
        content: '注销后不可恢复，手机号/邮箱/微信将解绑并匿名化。',
        editable: true,
        placeholderText: '请输入“注销账号”确认',
        success: async (r) => {
          if (!r.confirm) return;
          if ((r.content || '').trim() !== '注销账号') {
            uni.showToast({ title: '请输入“注销账号”确认', icon: 'none' });
            return;
          }
          try {
            await deactivateAccount('注销账号');
            uni.showToast({ title: '账号已注销', icon: 'success' });
            setTimeout(() => {
              useUserStore().logout();
              uni.reLaunch({ url: '/pages/mine/login/index' });
            }, 800);
          } catch (e) { /* 拦截器已提示 */ }
        }
      });
    },
  }
};
</script>

<style scoped>
/* 与全站约定完全一致：页面根**不设任何内边距**。
   左右留白由 .card 的 margin: 0 24rpx（App.vue 全局 .card）承担，
   上下由 .card 的 margin-bottom 与列表自身间距承担。
   这里原先是 padding: 24rpx，与 .card 的左右 margin 叠加成 48rpx（比其它页窄一圈），
   上边还额外多出 24rpx，使 NavBar 与卡片之间凭空多一段 —— 与 budget / categories /
   record-list 等所有带 NavBar 的页面都不一致。故整条规则去掉。 */
.card { background: var(--card); border-radius: 20rpx; padding: 24rpx 32rpx; margin-bottom: 24rpx; }
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
  flex-shrink: 0; font-size: 24rpx; padding: 10rpx 26rpx; border-radius: 999rpx;
  background: var(--primary-soft); color: var(--primary-text);
  border: 1rpx solid var(--primary);
}
.mail-btn.primary { background: var(--primary); color: var(--on-primary); border-color: transparent; }
.mail-edit.right { justify-content: flex-end; }
.mail-btn.ghost { background: var(--surface); color: var(--text-sub); }
/* 邮件帮助外链入口 */
.mail-help {
  display: flex; align-items: center; justify-content: space-between;
  padding-top: 20rpx; margin-top: 20rpx; border-top: 1rpx solid var(--border);
  font-size: 24rpx; color: var(--primary-text);
}
.mail-help-arrow { color: var(--text-muted); font-size: 28rpx; }
.setting-name.danger { color: var(--expense); }
.row-arrow { color: var(--text-muted); font-size: 32rpx; margin-left: 12rpx; }

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
.row-arrow { font-size: 32rpx; color: var(--text-sub); }
.legal-links { display: flex; align-items: center; justify-content: center; gap: 10rpx; margin-top: 16rpx; }
.legal-link { font-size: 23rpx; color: var(--primary-text); }
.legal-sep { font-size: 23rpx; color: var(--text-muted); }
/* 邮箱修改弹层 */
.sheet-mask { position: fixed; top: 0; right: 0; bottom: 0; left: 0; background: rgba(0,0,0,.45); z-index: 999; display: flex; align-items: flex-end; }
.sheet {
  width: 100%; max-height: 80vh; overflow-y: auto; -webkit-overflow-scrolling: touch;
  background: var(--card); border-radius: 32rpx 32rpx 0 0;
  padding: 24rpx 28rpx calc(28rpx + env(safe-area-inset-bottom)); box-sizing: border-box;
}
.sheet-title { font-size: 30rpx; font-weight: 700; text-align: center; color: var(--text-main); }
.sheet-tip { font-size: 22rpx; color: var(--text-muted); text-align: center; margin: 8rpx 0 20rpx; }
.sheet-actions { display: flex; gap: 16rpx; margin-top: 24rpx; }
.sheet-actions .mail-btn { flex: 1; text-align: center; padding: 18rpx 0; border-radius: 14rpx; font-size: 26rpx; }
@media (min-width: 768px) {
  .sheet { width: 560px; margin: 0 auto; border-radius: 24rpx 24rpx 0 0; }
}
</style>
