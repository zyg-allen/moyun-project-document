// 需要排除的特殊页面名称（独立页面，不作为普通分类展示，仅首页可进入）
export const SPECIAL_PAGE_NAMES = ['读书空间', '面试指南'];
// 检查是否为特殊页面名称
export function isSpecialPageName(name) {
    return SPECIAL_PAGE_NAMES.includes(name);
}
// 统一的分类数据（二级联动）
// 注意：读书空间、面试指南是独立特殊页面，不作为文章分类出现在此处，仅首页可进入
export const categories = [
    {
        id: 'featured',
        name: '精选推荐',
        key: 'featured',
        children: [
            { id: 'featured_all', name: '精选推荐', path: '/list?category=精选推荐' },
            { id: 'hot', name: '热门文章', path: '/list?category=热门文章' }
        ]
    },
    {
        id: 'prose',
        name: '散文天地',
        key: 'prose',
        children: [
            { id: 'daily', name: '人间烟火', path: '/list?category=人间烟火' },
            { id: 'travel', name: '山河行吟', path: '/list?category=山河行吟' },
            { id: 'mind', name: '心灵独白', path: '/list?category=心灵独白' },
            { id: 'city', name: '城市笔记', path: '/list?category=城市笔记' },
            { id: 'season', name: '四季专栏', path: '/list?category=四季专栏' },
            { id: 'audio', name: '声音散文', path: '/list?category=声音散文' },
            { id: 'letters', name: '读者来信', path: '/list?category=读者来信' }
        ]
    },
    {
        id: 'tech',
        name: '技术笔记',
        key: 'tech',
        children: [
            { id: 'beginner', name: '新手入门', path: '/list?category=新手入门' },
            { id: 'stack', name: '技术栈手册', path: '/list?category=技术栈手册' },
            { id: 'architecture', name: '架构札记', path: '/list?category=架构札记' },
            { id: 'performance', name: '性能日志', path: '/list?category=性能日志' },
            { id: 'ai', name: 'AI编程', path: '/list?category=AI编程' },
            { id: 'opensource', name: '开源日志', path: '/list?category=开源日志' }
        ]
    },
    {
        id: 'skills',
        name: '技能工坊',
        key: 'skills',
        children: [
            { id: 'writing', name: '写作技巧', path: '/list?category=写作技巧' },
            { id: 'coding', name: '代码技巧', path: '/list?category=代码技巧' },
            { id: 'learning', name: '学习方法', path: '/list?category=学习方法' },
            { id: 'tools', name: '工具指南', path: '/list?category=工具指南' },
            { id: 'practice', name: '输出训练', path: '/list?category=输出训练' }
        ]
    },
    {
        id: 'community',
        name: '社区互动',
        key: 'community',
        children: [
            { id: 'discussion', name: '话题讨论', path: '/list?category=话题讨论' },
            { id: 'contribution', name: '投稿征集', path: '/list?category=投稿征集' },
            { id: 'activity', name: '用户动态', path: '/list?category=用户动态' },
            { id: 'review', name: '互评圈', path: '/list?category=互评圈' },
            { id: 'checkin', name: '成长打卡', path: '/list?category=成长打卡' }
        ]
    }
];
// 获取所有一级分类名称列表（用于文章编辑页的一级选择）
export function getParentCategoryNames() {
    return categories.map(cat => cat.name);
}
// 根据一级分类名称获取对应的二级分类列表
export function getSubCategories(parentName) {
    const parent = categories.find(cat => cat.name === parentName);
    return parent ? parent.children : [];
}
// 根据二级分类名称查找对应的完整路径信息
export function findSubCategory(name) {
    for (const cat of categories) {
        const sub = cat.children.find(sub => sub.name === name);
        if (sub)
            return sub;
    }
    return null;
}
