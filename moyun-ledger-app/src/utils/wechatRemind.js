/**
 * 小程序微信订阅消息（待办提醒）
 *
 * 流程：读取后端配置 → uni.login 拿 code → 绑定 openid → requestSubscribeMessage。
 * 只有用户在小程序内点击授权过，服务端才能在该次订阅额度内下发一条提醒。
 * 未配置 AppID/模板时后端返回 enabled=false，这里静默跳过。
 */
import { getWechatConfig, bindWechat } from '@/api/ledger';

let bound = false;

function login() {
  return new Promise((resolve, reject) => {
    uni.login({
      provider: 'weixin',
      success: (res) => resolve(res.code),
      fail: reject
    });
  });
}

function requestSubscribe(tmplId) {
  return new Promise((resolve) => {
    uni.requestSubscribeMessage({
      tmplIds: [tmplId],
      success: (res) => resolve(res && res[tmplId] === 'accept'),
      fail: () => resolve(false)
    });
  });
}

/**
 * 确保当前用户已绑定 openid 并完成一次订阅授权。
 * @returns {Promise<boolean>} true=用户同意订阅
 */
export async function ensureWechatReminder() {
  // #ifdef MP-WEIXIN
  try {
    const cfg = await getWechatConfig();
    if (!cfg || !cfg.enabled || !cfg.memoTemplateId) {
      return false;
    }
    if (!bound) {
      const code = await login();
      if (!code) return false;
      await bindWechat(code);
      bound = true;
    }
    return await requestSubscribe(cfg.memoTemplateId);
  } catch (e) {
    return false;
  }
  // #endif
  return false;
}
