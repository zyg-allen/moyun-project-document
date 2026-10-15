/**
 * 应用元信息（单一来源）
 *
 * ⚠️ 版本号必须与 `src/manifest.json` 的 `versionName` 保持一致：
 * 打包以 manifest 为准，这里只供页面展示；**改版本时两处同改**。
 *
 * 为什么要有这个文件：此前「我的」页写死 `v11.76`、「设置」页写死 `v1.0.0`，
 * 同一个 App 里两处版本号自相矛盾（manifest 实际是 1.0.0）。
 */
export const APP_NAME = '墨韵记账';
export const APP_VERSION = '1.0.0';
export const APP_VERSION_TEXT = APP_NAME + ' v' + APP_VERSION;

/**
 * 邮件提醒「查看服务状态 / 获取授权码」的帮助链接（单一来源，改这里即可）。
 *
 * ⚠️ 留空则设置页**不渲染该链接**（避免给用户一个点不开的死链）。
 *
 * 三端打开方式由调用方按平台分流（见 pages/mine/settings 的 openMailHelp）：
 *   H5        → window.open 新开（web-view 在 H5 是 iframe，跨域受限）
 *   App/小程序 → navigateTo 到 pages/webview/index?src=<encodeURIComponent>
 *
 * ⚠️ 小程序端：该域名必须先在「微信公众平台 → 开发管理 → 业务域名」中备案，
 *    否则 web-view 会白屏/报"无法打开该页面"。
 */
export const MAIL_HELP_URL = '';
