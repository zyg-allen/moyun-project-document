/**
 * 创作者权限前端校验
 *
 * 与后端 CreatorPermissionChecker 对齐：发布文章 / 创建专栏 / 发布面经 需要"创作者认证"。
 * 话题创建、评论、点赞、收藏等对任何登录用户开放，无需调用本工具。
 *
 * 用法：在创作入口（按钮点击 / 路由进入）调用，未认证则提示并引导跳转认证页。
 */
import { useUserStore } from '@/stores/user';
import { useToast } from '@/composables/useToast';
import router from '@/router';

/**
 * 校验当前登录用户是否为认证创作者。
 * - 未登录：跳转登录页（带 redirect）
 * - 已登录未认证：toast 提示并跳转认证页
 * - 已认证：返回 true
 *
 * @returns 是否已认证创作者
 */
export function requireCreator(): boolean {
  const userStore = useUserStore();
  const toast = useToast();

  // 未登录 → 跳登录
  if (!userStore.user) {
    toast.info('请先登录');
    router.push({ path: '/login', query: { redirect: router.currentRoute.value.fullPath } });
    return false;
  }

  // 已登录但未认证创作者 → 跳认证页
  // isCertifiedCreator 后端为 Integer(0/1)，前端类型兼容 number|boolean
  const isCertified = userStore.user.isCertifiedCreator;
  const certified = isCertified === 1 || isCertified === true;
  if (!certified) {
    toast.info('该操作需要创作者认证，即将跳转认证页');
    setTimeout(() => router.push('/creator/certification'), 1200);
    return false;
  }

  return true;
}
