import { httpGet } from './client';
import type { ApiResponse } from '@/types/api';

// 友情链接类型
export interface FriendLink {
  id: string;
  name: string;
  url: string;
  description?: string;
  logo?: string;
  sort: number;
  status: 'active' | 'inactive';
  createdAt: string;
  updatedAt: string;
}

// 模拟友情链接数据
const mockFriendLinks: FriendLink[] = [
  {
    id: '1',
    name: '中国作家网',
    url: 'https://www.chinawriter.com.cn',
    description: '中国作家协会官方网站',
    logo: 'https://images.unsplash.com/photo-1481627834876-b7833e8f5570?w=100&h=100&fit=crop',
    sort: 1,
    status: 'active',
    createdAt: '2024-01-01',
    updatedAt: '2024-01-01',
  },
  {
    id: '2',
    name: '散文在线',
    url: 'https://www.sanwen.net',
    description: '散文爱好者的聚集地',
    logo: 'https://images.unsplash.com/photo-1456513080510-7bf3a84b82f8?w=100&h=100&fit=crop',
    sort: 2,
    status: 'active',
    createdAt: '2024-01-01',
    updatedAt: '2024-01-01',
  },
  {
    id: '3',
    name: '豆瓣读书',
    url: 'https://book.douban.com',
    description: '发现好书的地方',
    logo: 'https://images.unsplash.com/photo-1495446815901-a7297e633e8d?w=100&h=100&fit=crop',
    sort: 3,
    status: 'active',
    createdAt: '2024-01-01',
    updatedAt: '2024-01-01',
  },
  {
    id: '4',
    name: '简书文学',
    url: 'https://www.jianshu.com',
    description: '创作你的故事',
    sort: 4,
    status: 'active',
    createdAt: '2024-01-01',
    updatedAt: '2024-01-01',
  },
  {
    id: '5',
    name: '起点中文',
    url: 'https://www.qidian.com',
    description: '原创文学门户',
    logo: 'https://images.unsplash.com/photo-1532012197267-da84d127e765?w=100&h=100&fit=crop',
    sort: 5,
    status: 'active',
    createdAt: '2024-01-01',
    updatedAt: '2024-01-01',
  },
  {
    id: '6',
    name: '榕树下',
    url: 'https://www.rongshuxia.com',
    description: '华语文学门户',
    sort: 6,
    status: 'active',
    createdAt: '2024-01-01',
    updatedAt: '2024-01-01',
  },
  {
    id: '7',
    name: '诗词名句',
    url: 'https://www.shicimingju.com',
    description: '诗词名句大全',
    logo: 'https://images.unsplash.com/photo-1532012197267-da84d127e765?w=100&h=100&fit=crop',
    sort: 7,
    status: 'active',
    createdAt: '2024-01-01',
    updatedAt: '2024-01-01',
  },
  {
    id: '8',
    name: '片刻网',
    url: 'https://www.pianke.me',
    description: '记录你的片刻',
    sort: 8,
    status: 'active',
    createdAt: '2024-01-01',
    updatedAt: '2024-01-01',
  },
];

// 获取友情链接列表
export const getFriendLinks = async (): Promise<ApiResponse<FriendLink[]>> => {
  // 模拟API延迟
  await new Promise((resolve) => setTimeout(resolve, 200));
  
  // 后期对接真实API时，取消下面的注释，移除上面的模拟数据
  // return httpGet<FriendLink[]>('/friend-links');
  
  // 返回模拟数据
  return {
    code: 200,
    message: '获取成功',
    data: mockFriendLinks,
  };
};
