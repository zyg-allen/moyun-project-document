import { httpUpload } from './client';
// 上传文件
export const uploadFile = (file) => {
    return httpUpload('/upload/file', file);
};
// 上传图片
export const uploadImage = (file) => {
    return httpUpload('/upload/image', file);
};
