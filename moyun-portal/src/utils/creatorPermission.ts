/**
 * 实名认证策略前端工具（v10.10）
 *
 * 与后端策略对齐：
 * - 强制实名：打赏、积分兑换/消费等敏感场景 → requireRealName()（未实名拦截并引导认证）
 * - 提示实名：发布文章/面经、创建专栏等创作行为 → promptRealNameOptional()（弹窗提示可跳过，不强制）
 *
 * 实名判定：优先调用 /portal/creator/certification/my 查询身份认证（certType=identity 且 approved）；
 * 接口异常时降级用 isCertifiedCreator（创作者认证含实名信息）兜底，避免误拦。
 */
import { useUserStore } from '@/stores/user';
import { useConfirmModal } from '@/composables/useConfirmModal';
import { useToast } from '@/composables/useToast';
import router from '@/router';
import { getMyCertification } from '@/api/certification';

const confirmModal = useConfirmModal();

/** 跳转登录页（带 redirect 回跳） */
function gotoLogin(): boolean {
  const toast = useToast();
  toast.info('请先登录');
  router.push({ path: '/login', query: { redirect: router.currentRoute.value.fullPath } });
  return false;
}

/** 跳转实名认证页（带 redirect 回跳） */
function gotoCertification(): void {
  router.push({ path: '/creator/certification', query: { redirect: router.currentRoute.value.fullPath } });
}

/**
 * 查询当前用户是否已完成实名（身份认证 approved）
 * 接口失败时降级用 isCertifiedCreator 兜底（创作者认证申请含实名信息）
 */
async function isRealNameVerified(): Promise<boolean> {
  const userStore = useUserStore();
  try {
    const res = await getMyCertification();
    if (res.code === 200 && res.data) {
      const cert = res.data as { certType?: string; status?: string };
      if (cert.certType === 'identity' && cert.status === 'approved') return true;
    }
  } catch {
    // 查询失败降级：不阻塞主流程，用本地创作者标识兜底
  }
  const v = userStore.user?.isCertifiedCreator;
  return v === 1 || v === true;
}

/**
 * 强制实名校验（打赏/积分消费等敏感场景）
 * - 未登录：跳转登录页，返回 false
 * - 未实名：toast 提示并跳转认证页，返回 false
 * - 已实名：返回 true
 */
export async function requireRealName(): Promise<boolean> {
  const userStore = useUserStore();
  if (!userStore.user) return gotoLogin();
  if (await isRealNameVerified()) return true;
  const toast = useToast();
  toast.info('该操作需要先完成实名认证，正在前往认证页');
  setTimeout(() => gotoCertification(), 1200);
  return false;
}

/**
 * 提示实名（发布文章/面经/创建专栏等创作行为，不强制）
 * - 未登录：跳转登录页，返回 false
 * - 已实名：静默通过，返回 true
 * - 未实名：弹窗确认「是否前往实名认证？」
 *   - 确认 → 跳转认证页，返回 false（终止本次操作）
 *   - 取消 → 跳过提示直接继续发布，返回 true
 */
export async function promptRealNameOptional(): Promise<boolean> {
  const userStore = useUserStore();
  if (!userStore.user) return gotoLogin();
  if (await isRealNameVerified()) return true;
  const goCert = await confirmModal.confirm(
    '建议完成实名认证后发布，有助于提升内容可信度与曝光。\n\n是否现在前往实名认证？\n（点击「取消」将跳过认证，直接继续发布，未实名不影响发布）',
    { title: '确认操作' }
  );
  if (goCert) {
    gotoCertification();
    return false;
  }
  return true;
}
