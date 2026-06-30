import { httpPost } from './client';
// 上传文件
export const uploadPortalFile = (file, businessType, businessId) => {
    const formData = new FormData();
    formData.append('file', file);
    if (businessType) {
        formData.append('businessType', businessType);
    }
    if (businessId) {
        formData.append('businessId', businessId);
    }
    return httpPost('/portal/file/upload', formData);
};
