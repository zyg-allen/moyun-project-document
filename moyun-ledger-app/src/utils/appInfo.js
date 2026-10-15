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
