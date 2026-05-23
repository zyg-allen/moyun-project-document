import { z } from 'zod';

// 登录表单验证规则
export const loginSchema = z.object({
  username: z
    .string()
    .min(3, '用户名至少3个字符')
    .max(20, '用户名最多20个字符')
    .regex(/^[a-zA-Z0-9_]+$/, '用户名只能包含字母、数字和下划线'),
  password: z
    .string()
    .min(6, '密码至少6个字符')
    .max(20, '密码最多20个字符')
});

// 注册表单验证规则
export const registerSchema = z.object({
  username: z
    .string()
    .min(3, '用户名至少3个字符')
    .max(20, '用户名最多20个字符')
    .regex(/^[a-zA-Z0-9_]+$/, '用户名只能包含字母、数字和下划线'),
  email: z
    .string()
    .email('请输入有效的邮箱地址'),
  password: z
    .string()
    .min(6, '密码至少6个字符')
    .max(20, '密码最多20个字符')
    .regex(/^(?=.*[a-z])(?=.*[A-Z])(?=.*\d)/, '密码必须包含大小写字母和数字'),
  confirmPassword: z
    .string()
}).refine((data) => data.password === data.confirmPassword, {
  message: '两次输入的密码不一致',
  path: ['confirmPassword'],
});

// 文章发布验证规则
export const articleSchema = z.object({
  title: z
    .string()
    .min(5, '标题至少5个字符')
    .max(100, '标题最多100个字符'),
  content: z
    .string()
    .min(50, '内容至少50个字符')
    .max(50000, '内容最多50000个字符'),
  category: z
    .string()
    .min(1, '请选择分类'),
  tags: z
    .array(z.string())
    .min(1, '至少添加一个标签')
    .max(5, '最多添加5个标签'),
  excerpt: z
    .string()
    .max(200, '摘要最多200个字符')
    .optional(),
});

// 举报反馈验证规则
export const reportSchema = z.object({
  reportType: z
    .string()
    .min(1, '请选择举报类型'),
  targetUrl: z
    .string()
    .url('请输入有效的链接地址')
    .optional()
    .or(z.literal('')),
  description: z
    .string()
    .min(10, '请详细描述问题，至少10个字符')
    .max(1000, '描述最多1000个字符'),
  contact: z
    .string()
    .optional(),
});

// 反馈验证规则
export const feedbackSchema = z.object({
  feedbackType: z
    .string()
    .min(1, '请选择反馈类型'),
  subject: z
    .string()
    .min(5, '主题至少5个字符')
    .max(50, '主题最多50个字符'),
  description: z
    .string()
    .min(10, '请详细描述，至少10个字符')
    .max(1000, '描述最多1000个字符'),
  contact: z
    .string()
    .optional(),
});

// 修改密码验证规则
export const changePasswordSchema = z.object({
  oldPassword: z
    .string()
    .min(6, '密码至少6个字符')
    .max(20, '密码最多20个字符'),
  newPassword: z
    .string()
    .min(6, '新密码至少6个字符')
    .max(20, '新密码最多20个字符')
    .regex(/^(?=.*[a-z])(?=.*[A-Z])(?=.*\d)/, '密码必须包含大小写字母和数字'),
  confirmPassword: z
    .string()
}).refine((data) => data.newPassword === data.confirmPassword, {
  message: '两次输入的密码不一致',
  path: ['confirmPassword'],
});

// 绑定手机号验证规则
export const bindPhoneSchema = z.object({
  phone: z
    .string()
    .regex(/^1[3-9]\d{9}$/, '请输入有效的手机号'),
  code: z
    .string()
    .length(6, '验证码必须是6位数字'),
});

// 验证帮助函数
export function validateForm<T>(schema: z.ZodSchema<T>, data: unknown) {
  try {
    const result = schema.parse(data);
    return {
      success: true,
      data: result,
      errors: {}
    };
  } catch (error) {
    if (error instanceof z.ZodError) {
      const errors: Record<string, string> = {};
      error.issues.forEach((issue) => {
        const path = issue.path.join('.');
        if (!errors[path]) {
          errors[path] = issue.message;
        }
      });
      return {
        success: false,
        data: null,
        errors
      };
    }
    return {
      success: false,
      data: null,
      errors: { form: '验证失败，请重试' }
    };
  }
}
