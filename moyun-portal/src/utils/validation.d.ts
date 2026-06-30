import { z } from 'zod';
export declare const loginSchema: any;
export declare const registerSchema: any;
export declare const articleSchema: any;
export declare const reportSchema: any;
export declare const feedbackSchema: any;
export declare const changePasswordSchema: any;
export declare function validateForm<T>(schema: z.ZodSchema<T>, data: unknown): {
    success: boolean;
    data: any;
    errors: {};
} | {
    success: boolean;
    data: T;
    errors: Record<string, string>;
};
