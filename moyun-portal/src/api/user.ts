import { httpGet, httpPost, httpPut, httpUpload } from './client';
import type {
  User,
  UserProfileVO,
  LoginParams,
  LoginResponse,
  RegisterParams,
  RegisterResponse,
  UpdateUserProfileParams,
  UpdatePasswordParams,
  UserStats,
  UserDashboard,
  CaptchaImage,
  SendEmailCodeParams,
  ResetPasswordParams,
} from '@/types/api';

// 用户登录
export const login = (params: LoginParams) => {
  return httpPost<LoginResponse>('/portal/login', params);
};

// 用户注册
export const register = (params: RegisterParams) => {
  return httpPost<RegisterResponse>('/portal/register', params);
};

// 发送邮箱验证码（注册 / 找回密码两种场景）
export const sendEmailCode = (params: SendEmailCodeParams) => {
  return httpPost('/portal/email/code', params);
};

// 找回密码（邮箱验证码重置密码）
export const resetPassword = (params: ResetPasswordParams) => {
  return httpPost('/portal/email/reset-password', params);
};

// 发送短信验证码（注册 / 找回密码场景；scene=register|reset_password|bankcard|member）
export const sendSmsCode = (params: { phone: string; scene: string; code?: string; uuid?: string }) => {
  return httpPost('/portal/sms/code/send', params);
};

// 找回密码（短信验证码重置密码，手机号注册用户专用）
export const resetPasswordBySms = (params: { phone: string; code: string; newPassword: string; confirmPassword?: string }) => {
  return httpPost('/portal/sms/reset-password', params);
};

// 获取图形验证码
// 注意：/captchaImage 返回的 captchaEnabled/uuid/img 位于响应顶层（非 data 内），
// 不能复用 httpGet（其仅取 data 字段），故直接用 fetch 解析顶层字段。
export const getCaptchaImage = async (): Promise<CaptchaImage> => {
  const API_BASE_URL = import.meta.env.VITE_API_BASE_URL || '/api';
  const response = await fetch(`${API_BASE_URL}/captchaImage`, { method: 'GET' });
  if (!response.ok) {
    throw new Error(`验证码接口异常 (${response.status})`);
  }
  const data = await response.json().catch(() => null);
  const enabled = data?.captchaEnabled ?? true;
  // 开关为开时 uuid/img 必须齐备：缺一即为不可用，抛错交给页面展示"重试"，
  // 而不是用 ?? 兜底出一个"看似成功但实际不可用"的对象（那会让登录必然失败且无提示）。
  if (enabled && (!data?.uuid || !data?.img)) {
    throw new Error('验证码数据不完整，请重试');
  }
  return {
    captchaEnabled: enabled,
    uuid: data?.uuid || '',
    img: data?.img ? `data:image/jpeg;base64,${data.img}` : ''
  };
};

// 退出登录
export const logout = () => {
  return httpPost('/portal/logout');
};

// 获取当前用户信息
export const getCurrentUser = () => {
  return httpGet<User>('/portal/user/me');
};

// 更新用户信息
export const updateUserProfile = (params: UpdateUserProfileParams) => {
  return httpPut<User>('/portal/user/profile', params);
};

// 更新密码
export const updatePassword = (params: UpdatePasswordParams) => {
  return httpPut('/portal/user/password', params);
};

// 注销账号（软删除）
export const deactivateAccount = (confirmText: string) => {
  return httpPut('/portal/user/deactivate', { confirmText });
};

// 上传头像
export const uploadAvatar = (file: File) => {
  return httpUpload<User>('/portal/user/avatar', file);
};

// 获取当前登录用户统计信息
export const getUserStats = () => {
  return httpGet<UserStats>('/portal/user/stats');
};

// 获取个人中心 Dashboard 聚合数据（文章/收藏/书架/答题/面经/简历/关注/粉丝/专栏/未读消息/成长等级）
export const getMyDashboard = () => {
  return httpGet<UserDashboard>('/portal/user/me/dashboard');
};

// 获取用户详情（**公开**资料：后端返回 UserProfileVO 白名单，不含邮箱/手机/登录信息）
export const getUserById = (userId: string) => {
  return httpGet<UserProfileVO>(`/portal/user/${userId}`);
};

// 获取名家列表
/**
 * 获取名家列表（清单 P2）。
 *
 * <p>原先只接受 `limit`，前端只能一次拉 100 条再在浏览器内搜索/排序/分页 ⇒
 * **第 101 位之后的作者永不出现**，且"最受欢迎/粉丝最多"只在前 100 人子集内排序。
 * 现支持服务端分页：传对象即分页（返回 `{ list, total, pageNum, pageSize }`），
 * 传数字保持旧行为（返回数组）。</p>
 */
export const getAuthors = (
  params: number | { pageNum?: number; pageSize?: number; keyword?: string; sort?: string } = 10,
) => {
  if (typeof params === 'number') {
    return httpGet<any[]>('/portal/user/authors', { limit: params });
  }
  return httpGet<{ list: any[]; total: number; pageNum: number; pageSize: number }>(
    '/portal/user/authors',
    params as Record<string, unknown>,
  );
};
