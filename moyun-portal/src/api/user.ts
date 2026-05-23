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
  return httpPost<LoginResponse>('/user/login', params);
};

// 用户注册
export const register = (params: RegisterParams) => {
  return httpPost<RegisterResponse>('/user/register', params);
};

// 退出登录
export const logout = () => {
  return httpPost('/user/logout');
};

// 获取当前用户信息
export const getCurrentUser = () => {
  return httpGet<User>('/user/current');
};

// 更新用户信息
export const updateUserProfile = (params: UpdateUserProfileParams) => {
  return httpPut<User>('/user/profile', params);
};

// 更新密码
export const updatePassword = (params: UpdatePasswordParams) => {
  return httpPut('/user/password', params);
};

// 绑定手机
export const bindPhone = (params: BindPhoneParams) => {
  return httpPost('/user/bind-phone', params);
};

// 发送短信验证码
export const sendSmsCode = (params: SendSmsCodeParams) => {
  return httpPost('/user/send-sms', params);
};

// 上传头像
export const uploadAvatar = (file: File) => {
  return httpUpload<User>('/user/avatar', file);
};

// 获取用户统计信息
export const getUserStats = (userId?: string) => {
  return httpGet<UserStats>(userId ? `/user/${userId}/stats` : '/user/stats');
};

// 获取用户详情
export const getUserById = (userId: string) => {
  return httpGet<User>(`/user/${userId}`);
};

// 获取用户列表
export const getUserList = (params: { page: number; pageSize: number; [key: string]: any }) => {
  return httpGet<any>('/user/list', params);
};
