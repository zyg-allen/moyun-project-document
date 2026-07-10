import {httpGet} from './client';

// 自研广告位
export interface AdSlot {
    id: number;
    slotKey: string;
    title: string;
    image: string;
    link: string;
    content: string;
    sort: number;
    status: string; // 0启用 1停用
}

// 按广告位标识拉取广告列表（公开接口）
export const getAdList = (slotKey: string) => {
    return httpGet<AdSlot[]>('/portal/ad/list', {slotKey});
};
