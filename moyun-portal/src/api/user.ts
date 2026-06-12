import { httpGet, httpPost, httpPut, httpUpload } from './client';
import type {
  User,
  LoginParams,
  LoginResponse,
  RegisterParams,
  RegisterResponse,
  UpdateUserProfileParams,
  UpdatePasswordParams,
  BindPhoneParams,
  SendSmsCodeParams,
  UserStats,
} from '@/types/api';

// 用户登录
export const login = (params: LoginParams) => {
  return httpPost<LoginResponse>('/portal/login', params);
};

// 用户注册
export const register = (params: RegisterParams) => {
  return httpPost<RegisterResponse>('/portal/register', params);
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

// 绑定手机
export const bindPhone = (params: BindPhoneParams) => {
  return httpPost('/portal/user/bind-phone', params);
};

// 发送短信验证码
export const sendSmsCode = (params: SendSmsCodeParams) => {
  return httpPost('/portal/user/send-sms', params);
};

// 上传头像
export const uploadAvatar = (file: File) => {
  return httpUpload<User>('/portal/user/avatar', file);
};

// 获取用户统计信息
export const getUserStats = (userId?: string) => {
  return httpGet<UserStats>(userId ? `/portal/user/${userId}/stats` : '/portal/user/stats');
};

// 获取用户详情
export const getUserById = (userId: string) => {
  return httpGet<User>(`/portal/user/${userId}`);
};

// 获取用户列表
export const getUserList = (params: { page: number; pageSize: number; [key: string]: any }) => {
  return httpGet<any>('/portal/user/list', params);
};

// 获取名家列表
export const getAuthors = (limit = 10) => {
  return httpGet<any[]>('/portal/user/authors', { limit });
};
