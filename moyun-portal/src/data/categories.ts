export interface Category {
  id: string;
  name: string;
  key: string;
  children: SubCategory[];
}

export interface SubCategory {
  id: string;
  name: string;
  path: string;
}

// 统一的分类数据（二级联动）
export const categories: Category[] = [
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
    id: 'books',
    name: '读书空间',
    key: 'books',
    children: [
      { id: 'classic', name: '人文经典', path: '/list?category=人文经典' },
      { id: 'techbooks', name: '技术书籍', path: '/list?category=技术书籍' },
      { id: 'readtogether', name: '共读计划', path: '/list?category=共读计划' },
      { id: 'booklist', name: '书单推荐', path: '/list?category=书单推荐' },
      { id: 'quotes', name: '金句摘录', path: '/list?category=金句摘录' }
    ]
  },
  {
    id: 'interview',
    name: '面试指南',
    key: 'interview',
    children: [
      { id: 'questions', name: '真题库', path: '/list?category=真题库' },
      { id: 'experience', name: '面经复盘', path: '/list?category=面经复盘' },
      { id: 'resume', name: '简历优化', path: '/list?category=简历优化' },
      { id: 'mock', name: '模拟面试', path: '/list?category=模拟面试' },
      { id: 'career', name: '职业规划', path: '/list?category=职业规划' }
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
export function getParentCategoryNames(): string[] {
  return categories.map(cat => cat.name);
}

// 根据一级分类名称获取对应的二级分类列表
export function getSubCategories(parentName: string): SubCategory[] {
  const parent = categories.find(cat => cat.name === parentName);
  return parent ? parent.children : [];
}

// 根据二级分类名称查找对应的完整路径信息
export function findSubCategory(name: string): SubCategory | null {
  for (const cat of categories) {
    const sub = cat.children.find(sub => sub.name === name);
    if (sub) return sub;
  }
  return null;
}
