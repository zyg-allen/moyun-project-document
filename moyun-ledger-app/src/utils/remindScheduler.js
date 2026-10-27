/**
 * 本地提醒调度（备忘录）
 *
 * 说明：
 * - H5：使用浏览器 Notification，仅在页面存活期间有效；刷新/关闭后需重新调度。
 * - App（APP-PLUS）：使用 plus.push.createMessage 本地通知。
 * - 小程序：受平台限制，无法后台本地闹铃；此处仅在前台用 toast 提示，
 *   真正的后台提醒需要「订阅消息 + 服务端下发」（需小程序模板与 AppID 配置）。
 *
 * 服务端仍会通过定时任务发送站内通知与邮件，本地调度只是"到点弹一下"的补充。
 */

const RULE_MINUTES = {
  on_time: 0,
  advance_30m: 30,
  advance_1h: 60,
  advance_2h: 120,
  advance_1d: 24 * 60
};

const timers = new Map();

function parseTime(v) {
  if (!v) return 0;
  const s = String(v).replace('T', ' ').replace(/-/g, '/');
  const d = new Date(s);
  return Number.isNaN(d.getTime()) ? 0 : d.getTime();
}

function calcRemindAt(eventTime, rule) {
  const t = parseTime(eventTime);
  if (!t) return 0;
  if (rule === 'advance_1d_9am') {
    const d = new Date(t);
    d.setDate(d.getDate() - 1);
    d.setHours(9, 0, 0, 0);
    return d.getTime();
  }
  return t - (RULE_MINUTES[rule] || 0) * 60000;
}

function show(title, body) {
  // #ifdef H5
  try {
    if (typeof Notification !== 'undefined' && Notification.permission === 'granted') {
      // eslint-disable-next-line no-new
      new Notification(title, { body, tag: 'moyun-memo' });
      return;
    }
  } catch (e) { /* ignore */ }
  // #endif

  // #ifdef APP-PLUS
  try {
    plus.push.createMessage(body, '', { title, when: new Date() });
    return;
  } catch (e) { /* ignore */ }
  // #endif

  // #ifdef MP-WEIXIN
  uni.showToast({ title: title + '：' + body, icon: 'none', duration: 4000 });
  // #endif
}

/** 请求通知权限（H5） */
export async function ensureNotifyPermission() {
  // #ifdef H5
  try {
    if (typeof Notification === 'undefined') return false;
    if (Notification.permission === 'granted') return true;
    if (Notification.permission === 'denied') return false;
    const p = await Notification.requestPermission();
    return p === 'granted';
  } catch (e) {
    return false;
  }
  // #endif
  // #ifdef APP-PLUS
  return true;
  // #endif
  // #ifdef MP-WEIXIN
  return false;
  // #endif
}

/** 清空已排期的本地提醒 */
export function clearMemoReminders() {
  timers.forEach((id) => clearTimeout(id));
  timers.clear();
}

/**
 * 按待办列表重新排期（只排未来 24 小时内到点的，页面每次加载都会重排）。
 * @param {Array} todos 备忘录列表
 */
export function scheduleMemoReminders(todos) {
  clearMemoReminders();
  if (!Array.isArray(todos)) return;
  const now = Date.now();
  const MAX_AHEAD = 24 * 3600 * 1000;
  todos.forEach((t) => {
    if (!t || t.done || Number(t.remindEnabled) !== 1 || !t.eventTime) return;
    const at = calcRemindAt(t.eventTime, t.remindRule || 'on_time');
    const delta = at - now;
    if (delta <= 0 || delta > MAX_AHEAD) return;
    const title = '待办提醒';
    const body = (t.title || t.content || '未命名待办') + ' 时间到了';
    const id = setTimeout(() => {
      show(title, body);
      timers.delete(t.id);
    }, delta);
    timers.set(t.id, id);
  });
}
