import type { User, Article, Comment } from '@/types';
import type { Tag } from '@/types/api';
import { safeLocalStorage } from '@/utils/security';

const storage = safeLocalStorage();

export const mockUsers: User[] = [
  {
    id: '1',
    username: '张三',
    email: 'zhangsan@example.com',
    avatar: 'https://images.unsplash.com/photo-1472099645785-5658abf4ff4e?w=150&h=150&fit=crop&crop=faces',
    bio: '热爱编程，喜欢分享技术文章。',
    phone: '138****8888',
    wechat: 'zhangsan_wx',
    createdAt: '2023-01-15',
    isPhoneVerified: true,
    isWechatVerified: true,
    twoFactorEnabled: false
  },
  {
    id: '2',
    username: '李四',
    email: 'lisi@example.com',
    avatar: 'https://images.unsplash.com/photo-1507003211169-0a1dd7228f2d?w=150&h=150&fit=crop&crop=faces',
    bio: '前端开发工程师，专注于 Vue 和 React。',
    createdAt: '2023-03-20'
  },
  {
    id: '3',
    username: '王五',
    email: 'wangwu@example.com',
    avatar: 'https://images.unsplash.com/photo-1438761681033-6461ffad8d80?w=150&h=150&fit=crop&crop=faces',
    bio: '全栈开发者，喜欢探索新技术。',
    createdAt: '2023-05-10'
  }
];

export const mockArticles: Article[] = [
  {
    id: '1',
    title: 'Vue 3 组合式 API 完全指南',
    content: `<p>Vue 3 的组合式 API 为我们提供了一种全新的方式来组织和复用代码。在这篇文章中，我们将深入探讨如何使用组合式 API 来构建更好的 Vue 应用。</p>
<h3>什么是组合式 API？</h3>
<p>组合式 API 是 Vue 3 引入的一组新的 API，它允许我们使用函数来组织组件逻辑。与选项式 API 不同，组合式 API 可以让我们更灵活地组织代码。</p>
<h3>setup 函数</h3>
<p>setup 函数是组合式 API 的入口点。在这个函数中，我们可以定义响应式数据、计算属性、方法等。</p>
<pre><code>import { ref, computed } from 'vue'

export default {
  setup() {
    const count = ref(0)
    const doubled = computed(() => count.value * 2)
    
    function increment() {
      count.value++
    }
    
    return { count, doubled, increment }
  }
}</code></pre>
<h3>响应式引用 (ref)</h3>
<p>ref 用于创建一个响应式的引用值。它可以接收任意类型的值，并返回一个响应式的对象。</p>`,
    excerpt: 'Vue 3 的组合式 API 为我们提供了一种全新的方式来组织和复用代码。在这篇文章中，我们将深入探讨如何使用组合式 API。',
    cover: 'https://images.unsplash.com/photo-1555066931-4365d14bab8c?w=800&h=400&fit=crop',
    author: mockUsers[0],
    category: '新手入门',
    tags: ['Vue', 'JavaScript', '前端'],
    views: 1256,
    likes: 89,
    createdAt: '2024-01-10',
    updatedAt: '2024-01-15'
  },
  {
    id: '2',
    title: 'Tailwind CSS 3.0 新特性详解',
    content: `<p>Tailwind CSS 3.0 带来了许多令人兴奋的新特性。在这篇文章中，我们将介绍这些新特性以及如何在项目中使用它们。</p>
<h3>JIT 模式</h3>
<p>Tailwind CSS 3.0 默认启用 JIT 模式，这使得构建速度更快，生成的 CSS 文件更小。</p>
<h3>新的颜色系统</h3>
<p>新版本带来了全新的颜色系统，提供了更多的颜色选择和更好的对比度。</p>`,
    excerpt: 'Tailwind CSS 3.0 带来了许多令人兴奋的新特性。让我们一起来看看这些新特性吧。',
    cover: 'https://images.unsplash.com/photo-1507721999472-8ed4421c4af2?w=800&h=400&fit=crop',
    author: mockUsers[1],
    category: '技术栈手册',
    tags: ['CSS', 'Tailwind', '前端'],
    views: 892,
    likes: 56,
    createdAt: '2024-01-12',
    updatedAt: '2024-01-12'
  },
  {
    id: '3',
    title: 'Vite 5.0 快速上手教程',
    content: `<p>Vite 是新一代的前端构建工具，它提供了极快的开发体验。在这篇教程中，我们将学习如何使用 Vite 5.0 来构建项目。</p>
<h3>为什么选择 Vite？</h3>
<p>Vite 利用浏览器原生 ES 模块支持，实现了极速的冷启动和热更新。</p>
<h3>创建新项目</h3>
<p>使用 Vite 创建项目非常简单，只需要一条命令。</p>`,
    excerpt: 'Vite 是新一代的前端构建工具，提供了极快的开发体验。让我们一起来学习吧。',
    cover: 'https://images.unsplash.com/photo-1498050108023-c5249f4df085?w=800&h=400&fit=crop',
    author: mockUsers[2],
    category: '技术栈手册',
    tags: ['Vite', 'JavaScript', '工具'],
    views: 743,
    likes: 42,
    createdAt: '2024-01-15',
    updatedAt: '2024-01-15'
  },
  {
    id: '4',
    title: '深入理解 JavaScript 闭包',
    content: `<p>闭包是 JavaScript 中一个非常重要的概念。在这篇文章中，我们将深入探讨闭包的工作原理和实际应用。</p>
<h3>什么是闭包？</h3>
<p>闭包是指有权访问另一个函数作用域中变量的函数。</p>
<h3>闭包的用途</h3>
<p>闭包可以用于数据私有化、函数柯里化等场景。</p>`,
    excerpt: '闭包是 JavaScript 中一个非常重要的概念。让我们深入理解它的工作原理。',
    cover: 'https://images.unsplash.com/photo-1516116216624-53e697fedbea?w=800&h=400&fit=crop',
    author: mockUsers[0],
    category: '技术栈手册',
    tags: ['JavaScript', '编程', '前端'],
    views: 1520,
    likes: 115,
    createdAt: '2024-01-18',
    updatedAt: '2024-01-20'
  },
  {
    id: '5',
    title: '2024 年前端技术趋势展望',
    content: `<p>新的一年已经到来，让我们一起展望 2024 年前端技术的发展趋势。</p>
<h3>人工智能与前端</h3>
<p>AI 将在前端开发中扮演越来越重要的角色。</p>
<h3>Web 应用的未来</h3>
<p>PWA、WebAssembly 等技术将继续发展。</p>`,
    excerpt: '新的一年已经到来，让我们一起展望 2024 年前端技术的发展趋势。',
    cover: 'https://images.unsplash.com/photo-1451187580459-43490279c0fa?w=800&h=400&fit=crop',
    author: mockUsers[1],
    category: '技术栈手册',
    tags: ['前端', '趋势', '技术'],
    views: 2341,
    likes: 178,
    createdAt: '2024-01-20',
    updatedAt: '2024-01-22'
  },
  {
    id: '6',
    title: '响应式设计最佳实践',
    content: `<p>响应式设计是现代 Web 开发的必备技能。在这篇文章中，我们将分享响应式设计的最佳实践。</p>
<h3>移动优先</h3>
<p>采用移动优先的设计策略，从小屏幕开始设计。</p>
<h3>弹性布局</h3>
<p>使用 Flexbox 和 Grid 来创建灵活的布局。</p>`,
    excerpt: '响应式设计是现代 Web 开发的必备技能。让我们分享一些最佳实践。',
    cover: 'https://images.unsplash.com/photo-1512941937669-90a1b58e7e9c?w=800&h=400&fit=crop',
    author: mockUsers[2],
    category: '技术栈手册',
    tags: ['设计', '响应式', 'CSS'],
    views: 987,
    likes: 67,
    createdAt: '2024-01-22',
    updatedAt: '2024-01-23'
  },
  {
    id: '7',
    title: 'TypeScript 高级类型体操',
    content: `<p>TypeScript 的类型系统非常强大，本文将带你深入了解高级类型技巧。</p>
<h3>条件类型</h3>
<p>条件类型可以根据其他类型来推导类型。</p>
<h3>映射类型</h3>
<p>映射类型可以从现有类型创建新类型。</p>`,
    excerpt: '深入探索 TypeScript 的高级类型系统，掌握类型编程的精髓。',
    author: mockUsers[0],
    category: '技术栈手册',
    tags: ['TypeScript', 'JavaScript', '类型系统'],
    views: 856,
    likes: 73,
    createdAt: '2024-01-25'
  },
  {
    id: '8',
    title: 'Node.js 性能优化实战',
    content: `<p>Node.js 应用性能优化是后端开发中的重要课题。本文将分享实用的优化技巧。</p>
<h3>内存管理</h3>
<p>了解 V8 引擎的垃圾回收机制，避免内存泄漏。</p>
<h3>事件循环</h3>
<p>深入理解 Node.js 的事件循环机制。</p>`,
    excerpt: '掌握 Node.js 性能优化的核心技巧，提升应用响应速度。',
    author: mockUsers[1],
    category: '架构札记',
    tags: ['Node.js', '性能', '后端'],
    views: 1123,
    likes: 92,
    createdAt: '2024-01-28'
  },
  {
    id: '9',
    title: 'Docker 容器化部署完全指南',
    content: `<p>Docker 已经成为现代应用部署的标准。本文将详细介绍 Docker 的使用方法。</p>
<h3>镜像构建</h3>
<p>学习如何编写高效的 Dockerfile。</p>
<h3>容器编排</h3>
<p>使用 Docker Compose 管理多容器应用。</p>`,
    excerpt: '从零开始掌握 Docker 容器化技术，实现应用的快速部署。',
    author: mockUsers[2],
    category: '工具指南',
    tags: ['Docker', 'DevOps', '部署'],
    views: 1654,
    likes: 134,
    createdAt: '2024-02-01'
  },
  {
    id: '10',
    title: 'Kubernetes 集群管理入门',
    content: `<p>Kubernetes 是容器编排的事实标准。本文将带你入门 K8s。</p>
<h3>核心概念</h3>
<p>理解 Pod、Service、Deployment 等核心资源。</p>
<h3>实际部署</h3>
<p>将应用部署到 Kubernetes 集群。</p>`,
    excerpt: '深入学习 Kubernetes，掌握云原生时代的容器编排技术。',
    author: mockUsers[0],
    category: '工具指南',
    tags: ['Kubernetes', 'K8s', '容器'],
    views: 1876,
    likes: 156,
    createdAt: '2024-02-05'
  },
  {
    id: '11',
    title: 'React Hooks 深入浅出',
    content: `<p>React Hooks 彻底改变了 React 组件的开发方式。本文深入解析常用 Hooks。</p>
<h3>useState 和 useEffect</h3>
<p>掌握最基本的两个 Hooks。</p>
<h3>自定义 Hooks</h3>
<p>学会抽取和复用逻辑。</p>`,
    excerpt: '全面掌握 React Hooks，提升组件开发效率。',
    author: mockUsers[1],
    category: '技术栈手册',
    tags: ['React', 'Hooks', '前端'],
    views: 1432,
    likes: 108,
    createdAt: '2024-02-08'
  },
  {
    id: '12',
    title: 'GraphQL 与 REST API 对比',
    content: `<p>GraphQL 作为 API 查询语言，与 REST 相比有哪些优势？本文进行全面对比。</p>
<h3>数据获取</h3>
<p>一次请求获取所有需要的数据。</p>
<h3>类型系统</h3>
<p>强类型带来的开发体验提升。</p>`,
    excerpt: '深入对比 GraphQL 和 REST API，帮助你做出正确的技术选择。',
    author: mockUsers[2],
    category: '架构札记',
    tags: ['GraphQL', 'API', '后端'],
    views: 987,
    likes: 67,
    createdAt: '2024-02-10'
  },
  {
    id: '13',
    title: '移动端 React Native 开发指南',
    content: `<p>使用 React Native 可以用 React 开发原生移动应用。本文介绍 RN 开发要点。</p>
<h3>跨平台组件</h3>
<p>编写同时支持 iOS 和 Android 的代码。</p>
<h3>原生模块</h3>
<p>在需要时调用平台原生能力。</p>`,
    excerpt: '掌握 React Native，用前端技术开发跨平台移动应用。',
    author: mockUsers[0],
    category: '技术栈手册',
    tags: ['React Native', '移动开发', '跨平台'],
    views: 1123,
    likes: 89,
    createdAt: '2024-02-12'
  },
  {
    id: '14',
    title: 'Flutter 跨平台开发实战',
    content: `<p>Flutter 以其优秀的性能和精美的 UI 成为跨平台开发的热门选择。</p>
<h3>Dart 语言基础</h3>
<p>快速掌握 Dart 语言。</p>
<h3>Widget 系统</h3>
<p>Flutter 的核心——Widget 介绍。</p>`,
    excerpt: '深入学习 Flutter，打造高性能跨平台移动应用。',
    author: mockUsers[1],
    category: '技术栈手册',
    tags: ['Flutter', '移动开发', 'Dart'],
    views: 1345,
    likes: 112,
    createdAt: '2024-02-15'
  },
  {
    id: '15',
    title: '机器学习入门：Python 实现基础算法',
    content: `<p>机器学习入门教程，使用 Python 实现几个经典的机器学习算法。</p>
<h3>线性回归</h3>
<p>从最简单的算法开始。</p>
<h3>决策树</h3>
<p>理解基于树的模型。</p>`,
    excerpt: '零基础入门机器学习，用 Python 实现经典算法。',
    author: mockUsers[2],
    category: 'AI编程',
    tags: ['Python', '机器学习', 'AI'],
    views: 2341,
    likes: 201,
    createdAt: '2024-02-18'
  },
  {
    id: '16',
    title: '深度学习框架 PyTorch 教程',
    content: `<p>PyTorch 是目前最流行的深度学习框架之一。本文介绍其核心概念。</p>
<h3>张量操作</h3>
<p>PyTorch 的基本数据结构。</p>
<h3>自动求导</h3>
<p>理解 PyTorch 的核心特性。</p>`,
    excerpt: '掌握 PyTorch 框架，开启深度学习之旅。',
    author: mockUsers[0],
    category: 'AI编程',
    tags: ['PyTorch', '深度学习', 'AI'],
    views: 1876,
    likes: 167,
    createdAt: '2024-02-20'
  },
  {
    id: '17',
    title: 'Git 工作流最佳实践',
    content: `<p>Git 是现代开发必备的版本控制工具。本文分享团队协作的最佳实践。</p>
<h3>分支策略</h3>
<p>Git Flow vs GitHub Flow。</p>
<h3>提交规范</h3>
<p>编写有意义的提交信息。</p>`,
    excerpt: '掌握 Git 工作流，提升团队协作效率。',
    author: mockUsers[1],
    category: '开源日志',
    tags: ['Git', '版本控制', '协作'],
    views: 1654,
    likes: 143,
    createdAt: '2024-02-22'
  },
  {
    id: '18',
    title: 'MySQL 数据库优化指南',
    content: `<p>数据库性能直接影响应用响应速度。本文介绍 MySQL 优化的关键技术。</p>
<h3>索引优化</h3>
<p>创建合适的索引提升查询性能。</p>
<h3>慢查询分析</h3>
<p>找出影响性能的罪魁祸首。</p>`,
    excerpt: '深入理解 MySQL 优化，打造高性能数据库应用。',
    author: mockUsers[2],
    category: '架构札记',
    tags: ['MySQL', '数据库', '后端'],
    views: 1543,
    likes: 128,
    createdAt: '2024-02-25'
  },
  {
    id: '19',
    title: 'Redis 缓存实战技巧',
    content: `<p>Redis 是高性能缓存的首选。本文分享 Redis 在生产环境中的使用技巧。</p>
<h3>数据结构</h3>
<p>选择合适的数据结构解决问题。</p>
<h3>持久化策略</h3>
<p>平衡性能和数据安全。</p>`,
    excerpt: '掌握 Redis 缓存技术，提升应用响应速度。',
    author: mockUsers[0],
    category: '架构札记',
    tags: ['Redis', '缓存', '后端'],
    views: 1432,
    likes: 119,
    createdAt: '2024-02-28'
  },
  {
    id: '20',
    title: '开源项目贡献指南',
    content: `<p>参与开源项目是提升技术能力的绝佳方式。本文分享如何参与开源贡献。</p>
<h3>选择项目</h3>
<p>找到适合自己的开源项目。</p>
<h3>提交流程</h3>
<p>从 Fork 到 Merge 的完整流程。</p>`,
    excerpt: '学习如何参与开源项目，成为开源社区的一员。',
    author: mockUsers[1],
    category: '开源日志',
    tags: ['开源', 'GitHub', '社区'],
    views: 987,
    likes: 87,
    createdAt: '2024-03-01'
  },
  {
    id: '21',
    title: '微服务架构设计与实践',
    content: `<p>微服务是现代后端架构的主流选择。本文介绍微服务的核心概念和实践。</p>
<h3>服务拆分</h3>
<p>如何合理地拆分服务。</p>
<h3>服务通信</h3>
<p>同步与异步通信方式。</p>`,
    excerpt: '掌握微服务架构，构建可扩展的后端系统。',
    author: mockUsers[2],
    category: '架构札记',
    tags: ['微服务', '架构', '后端'],
    views: 1876,
    likes: 156,
    createdAt: '2024-03-05'
  },
  {
    id: '22',
    title: '梅雨时节的乡愁',
    content: `<p>雨丝斜斜地落在青石板上，溅起一朵朵细小的水花。这是江南特有的梅雨季节，也是我心中最柔软的时光。</p>
<h3>童年的雨巷</h3>
<p>记忆中总有一条悠长的雨巷，母亲撑着油纸伞在巷口等我放学归来。</p>`,
    excerpt: '又是一年梅雨季，思绪随着雨丝飘向远方的故乡。',
    author: mockUsers[0],
    category: '人间烟火',
    tags: ['故乡', '童年', '江南'],
    views: 2341,
    likes: 234,
    createdAt: '2024-03-08'
  },
  {
    id: '23',
    title: '深夜食堂的一碗面',
    content: `<p>凌晨两点的东京街头，我走进一家还亮着灯的小店。老板是个沉默寡言的老者，却能记住每一位常客的口味。</p>
<h3>一碗猪骨拉面</h3>
<p>浓郁的汤底，劲道的面条，这是一天中最温暖的时刻。</p>`,
    excerpt: '每个人都有属于自己的深夜食堂，那里有食物，也有故事。',
    author: mockUsers[1],
    category: '山河行吟',
    tags: ['美食', '东京', '人生'],
    views: 1876,
    likes: 178,
    createdAt: '2024-03-10'
  },
  {
    id: '24',
    title: '生命的意义在于过程',
    content: `<p>我们总是在追寻人生的意义，却忽略了过程本身才是生命最美的风景。</p>
<h3>放下执念</h3>
<p>不必执着于终点，沿途的风景同样值得珍惜。</p>`,
    excerpt: '生命的意义不在于抵达终点，而在于沿途的风景。',
    author: mockUsers[2],
    category: '心灵独白',
    tags: ['人生', '哲理', '思考'],
    views: 1543,
    likes: 145,
    createdAt: '2024-03-12'
  },
  {
    id: '25',
    title: '背影中的父爱',
    content: `<p>朱自清先生的《背影》写尽了父子深情。那个蹒跚翻越站台的背影，承载了几代人的记忆。</p>
<h3>父爱无言</h3>
<p>父爱如山，不善表达却从未缺席。</p>`,
    excerpt: '重读经典，感悟父爱的深沉与厚重。',
    author: mockUsers[0],
    category: '心灵独白',
    tags: ['经典', '父爱', '阅读'],
    views: 3456,
    likes: 312,
    createdAt: '2024-03-15'
  },
  {
    id: '26',
    title: '致橡树',
    content: `<p>我必须是你近旁的一株木棉，作为树的形象和你站在一起。根，紧握在地下；叶，相触在云里。</p>
<h3>平等的爱情</h3>
<p>舒婷笔下的爱情，是平等独立、相互成就的。</p>`,
    excerpt: '经典诗歌，重温舒婷对爱情的理解与追求。',
    author: mockUsers[1],
    category: '心灵独白',
    tags: ['诗歌', '爱情', '经典'],
    views: 2876,
    likes: 267,
    createdAt: '2024-03-18'
  },
  {
    id: '27',
    title: '社区创作者的成长之路',
    content: `<p>从一个默默无闻的写作者，到拥有上万粉丝的社区达人，这条路充满了挑战和收获。</p>
<h3>坚持输出</h3>
<p>内容创作最重要的品质是坚持。</p>`,
    excerpt: '分享社区创作的经验与心得，助你成为优秀的创作者。',
    author: mockUsers[2],
    category: '话题讨论',
    tags: ['创作', '社区', '成长'],
    views: 1234,
    likes: 98,
    createdAt: '2024-03-20'
  },
  {
    id: '28',
    title: 'CSS Grid 布局完全指南',
    content: `<p>CSS Grid 是二维布局的利器。本文全面介绍 Grid 的使用方法和实用技巧。</p>
<h3>网格容器</h3>
<p>掌握 grid-template-columns 和 grid-template-rows。</p>
<h3>网格项目</h3>
<p>学会控制网格项目的位置和大小。</p>`,
    excerpt: '掌握 CSS Grid 布局技术，实现复杂的网页布局。',
    cover: 'https://images.unsplash.com/photo-1507721999472-8ed4421c4af2?w=800&h=400&fit=crop',
    author: mockUsers[0],
    category: '技术栈手册',
    tags: ['CSS', '布局', '前端'],
    views: 1654,
    likes: 134,
    createdAt: '2024-03-22'
  },
  {
    id: '29',
    title: 'NestJS 企业级后端框架',
    content: `<p>NestJS 是 Node.js 生态中最成熟的企业级框架。本文介绍其核心概念。</p>
<h3>模块化架构</h3>
<p>NestJS 基于模块组织代码。</p>
<h3>依赖注入</h3>
<p>理解控制反转和依赖注入。</p>`,
    excerpt: '使用 NestJS 构建可维护的企业级后端应用。',
    author: mockUsers[1],
    category: '架构札记',
    tags: ['NestJS', 'Node.js', '后端'],
    views: 1432,
    likes: 119,
    createdAt: '2024-03-25'
  },
  {
    id: '30',
    title: 'CI/CD 流水线构建实战',
    content: `<p>持续集成和持续部署是现代 DevOps 的核心实践。本文演示完整的 CI/CD 流程。</p>
<h3>自动化测试</h3>
<p>在每次提交时自动运行测试。</p>
<h3>自动部署</h3>
<p>测试通过后自动部署到生产环境。</p>`,
    excerpt: '构建高效的 CI/CD 流水线，实现持续交付。',
    author: mockUsers[2],
    category: '工具指南',
    tags: ['CI/CD', 'DevOps', '自动化'],
    views: 1876,
    likes: 156,
    createdAt: '2024-03-28'
  }
];

export const mockComments: Comment[] = [
  {
    id: '1',
    articleId: '1',
    author: mockUsers[1] as User,
    content: '写得很好！组合式 API 确实让代码更清晰了。',
    createdAt: '2024-01-11',
    likeCount: 12,
    replies: [
      {
        id: '1-1',
        articleId: '1',
        parentId: '1',
        author: mockUsers[0] as User,
        content: '感谢支持！后续会更新更多 Vue 3 的教程。',
        createdAt: '2024-01-11',
        likeCount: 5
      },
      {
        id: '1-2',
        articleId: '1',
        parentId: '1',
        author: mockUsers[2] as User,
        content: '赞同！我也觉得组合式 API 更灵活。',
        createdAt: '2024-01-12',
        likeCount: 3
      }
    ]
  },
  {
    id: '2',
    articleId: '1',
    author: mockUsers[2] as User,
    content: '期待更多 Vue 3 的教程！',
    createdAt: '2024-01-12',
    likeCount: 8
  },
  {
    id: '3',
    articleId: '1',
    author: mockUsers[1] as User,
    content: '请问 setup 函数中如何处理异步数据呢？',
    createdAt: '2024-01-13',
    likeCount: 15,
    replies: [
      {
        id: '3-1',
        articleId: '1',
        parentId: '3',
        author: mockUsers[0] as User,
        content: '可以使用 onMounted + async 函数，或者用 vue-query 这样的库来管理异步数据。',
        createdAt: '2024-01-13',
        likeCount: 10
      }
    ]
  },
  {
    id: '4',
    articleId: '1',
    author: mockUsers[2] as User,
    content: '响应式引用和响应式对象有什么区别呢？',
    createdAt: '2024-01-14',
    likeCount: 6
  },
  {
    id: '5',
    articleId: '1',
    author: mockUsers[1] as User,
    content: '文章结构清晰，一步步引导，非常适合初学者！',
    createdAt: '2024-01-15',
    likeCount: 20
  },
  {
    id: '6',
    articleId: '1',
    author: mockUsers[0] as User,
    content: '感谢大家的反馈！后续会继续更新更多优质内容。',
    createdAt: '2024-01-16',
    likeCount: 25
  },
  {
    id: '7',
    articleId: '1',
    author: mockUsers[2] as User,
    content: '能不能出一期关于自定义 Hooks 的教程呢？',
    createdAt: '2024-01-17',
    likeCount: 18,
    replies: [
      {
        id: '7-1',
        articleId: '1',
        parentId: '7',
        author: mockUsers[0] as User,
        content: '安排！下一期就讲自定义 Hooks 的实践。',
        createdAt: '2024-01-17',
        likeCount: 12
      }
    ]
  },
  {
    id: '8',
    articleId: '1',
    author: mockUsers[1] as User,
    content: '代码示例很完整，直接就能拿来用！',
    createdAt: '2024-01-18',
    likeCount: 9
  },
  {
    id: '9',
    articleId: '1',
    author: mockUsers[2] as User,
    content: 'Vue 3 的类型推导做得真的好，配合 TypeScript 太爽了。',
    createdAt: '2024-01-19',
    likeCount: 22
  },
  {
    id: '10',
    articleId: '1',
    author: mockUsers[0] as User,
    content: '感谢分享，已收藏！',
    createdAt: '2024-01-20',
    likeCount: 14
  },
  {
    id: '11',
    articleId: '1',
    author: mockUsers[1] as User,
    content: '从 Vue 2 迁移到 Vue 3，感觉代码简洁了很多。',
    createdAt: '2024-01-21',
    likeCount: 16
  },
  {
    id: '12',
    articleId: '1',
    author: mockUsers[2] as User,
    content: 'Composition API 让逻辑复用变得更简单了！',
    createdAt: '2024-01-22',
    likeCount: 11
  },
  {
    id: '3',
    articleId: '2',
    author: mockUsers[0] as User,
    content: 'Tailwind 3.0 确实好用！',
    createdAt: '2024-01-13'
  }
];

let currentUser: User | null = null;

export function getCurrentUser(): User | null {
  if (currentUser) return currentUser;
  const saved = storage.getItem('currentUser');
  if (saved) {
    currentUser = JSON.parse(saved);
    return currentUser;
  }
  return null;
}

export function setCurrentUser(user: User | null) {
  currentUser = user;
  if (user) {
    const { password, ...safeUser } = user as any;
    storage.setJSON('currentUser', safeUser);
  } else {
    storage.removeItem('currentUser');
  }
}

export function getArticles(): Article[] {
  return mockArticles;
}

export function getArticleById(id: string): Article | undefined {
  return mockArticles.find(a => a.id === id);
}

export function searchArticles(query: string): Article[] {
  const q = query.toLowerCase();
  return mockArticles.filter(a => 
    a.title.toLowerCase().includes(q) || 
    (a.excerpt ?? '').toLowerCase().includes(q) ||
    (a.tags ?? []).some(t => t.toLowerCase().includes(q))
  );
}

export function getArticlesByCategory(category: string): Article[] {
  return mockArticles.filter(a => a.category === category);
}

export function getArticlesByUser(userId: string): Article[] {
  return mockArticles.filter(a => a.author?.id === userId);
}

export function getCommentsByArticleId(articleId: string): Comment[] {
  return mockComments
    .filter(c => c.articleId === articleId && !c.parentId)
    .sort((a, b) => new Date(b.createdAt).getTime() - new Date(a.createdAt).getTime());
}

export function addComment(articleId: string, author: User, content: string, parentId?: string): Comment {
  const comment: Comment = {
    id: Date.now().toString(),
    articleId,
    author,
    content,
    createdAt: new Date().toISOString().split('T')[0] ?? '',
    parentId,
    likeCount: 0,
    isLiked: false,
    replies: []
  };
  mockComments.push(comment);
  return comment;
}

export function toggleCommentLike(commentId: string): void {
  const comment = mockComments.find(c => c.id === commentId);
  if (comment) {
    comment.isLiked = !comment.isLiked;
    comment.likeCount = (comment.likeCount || 0) + (comment.isLiked ? 1 : -1);
  }
}

export function addArticle(article: Partial<Article>): Article {
  const newArticle: Article = {
    id: Date.now().toString(),
    title: article.title || '',
    content: article.content || '',
    excerpt: article.excerpt || '',
    cover: article.cover || '',
    author: article.author || mockUsers[0],
    category: article.category || '技术栈手册',
    tags: article.tags || [],
    views: 0,
    likes: 0,
    createdAt: new Date().toISOString().split('T')[0],
    updatedAt: new Date().toISOString().split('T')[0],
    editorMode: article.editorMode || 'richtext',
    contentMarkdown: article.contentMarkdown || undefined
  };
  mockArticles.unshift(newArticle);
  return newArticle;
}

// 标签相关数据
export const mockTags: Tag[] = [
  { id: '1', name: 'Vue', slug: 'vue', articleCount: 5, createdAt: '2024-01-01' },
  { id: '2', name: 'JavaScript', slug: 'javascript', articleCount: 8, createdAt: '2024-01-01' },
  { id: '3', name: '前端', slug: 'frontend', articleCount: 12, createdAt: '2024-01-01' },
  { id: '4', name: 'CSS', slug: 'css', articleCount: 4, createdAt: '2024-01-01' },
  { id: '5', name: 'Tailwind', slug: 'tailwind', articleCount: 2, createdAt: '2024-01-01' },
  { id: '6', name: 'Vite', slug: 'vite', articleCount: 3, createdAt: '2024-01-01' },
  { id: '7', name: 'React', slug: 'react', articleCount: 4, createdAt: '2024-01-01' },
  { id: '8', name: 'Node.js', slug: 'nodejs', articleCount: 5, createdAt: '2024-01-01' },
  { id: '9', name: '后端', slug: 'backend', articleCount: 7, createdAt: '2024-01-01' },
  { id: '10', name: 'TypeScript', slug: 'typescript', articleCount: 3, createdAt: '2024-01-01' },
  { id: '11', name: 'Docker', slug: 'docker', articleCount: 2, createdAt: '2024-01-01' },
  { id: '12', name: 'Kubernetes', slug: 'kubernetes', articleCount: 2, createdAt: '2024-01-01' },
  { id: '13', name: 'Git', slug: 'git', articleCount: 2, createdAt: '2024-01-01' },
  { id: '14', name: 'MySQL', slug: 'mysql', articleCount: 2, createdAt: '2024-01-01' },
  { id: '15', name: 'Redis', slug: 'redis', articleCount: 2, createdAt: '2024-01-01' },
  { id: '16', name: '微服务', slug: 'microservices', articleCount: 2, createdAt: '2024-01-01' },
  { id: '17', name: '架构', slug: 'architecture', articleCount: 3, createdAt: '2024-01-01' },
  { id: '18', name: '性能', slug: 'performance', articleCount: 2, createdAt: '2024-01-01' },
  { id: '19', name: 'DevOps', slug: 'devops', articleCount: 3, createdAt: '2024-01-01' },
  { id: '20', name: '设计', slug: 'design', articleCount: 2, createdAt: '2024-01-01' },
  { id: '21', name: '响应式', slug: 'responsive', articleCount: 2, createdAt: '2024-01-01' },
  { id: '22', name: 'Python', slug: 'python', articleCount: 2, createdAt: '2024-01-01' },
  { id: '23', name: '机器学习', slug: 'machine-learning', articleCount: 2, createdAt: '2024-01-01' },
  { id: '24', name: 'AI', slug: 'ai', articleCount: 2, createdAt: '2024-01-01' },
  { id: '25', name: 'Flutter', slug: 'flutter', articleCount: 2, createdAt: '2024-01-01' },
  { id: '26', name: 'React Native', slug: 'react-native', articleCount: 2, createdAt: '2024-01-01' },
  { id: '27', name: '移动开发', slug: 'mobile', articleCount: 3, createdAt: '2024-01-01' },
  { id: '28', name: '开源', slug: 'opensource', articleCount: 2, createdAt: '2024-01-01' },
  { id: '29', name: 'GitHub', slug: 'github', articleCount: 2, createdAt: '2024-01-01' },
  { id: '30', name: '社区', slug: 'community', articleCount: 2, createdAt: '2024-01-01' },
  { id: '31', name: '人生', slug: 'life', articleCount: 3, createdAt: '2024-01-01' },
  { id: '32', name: '思考', slug: 'thinking', articleCount: 2, createdAt: '2024-01-01' },
  { id: '33', name: '故乡', slug: 'hometown', articleCount: 1, createdAt: '2024-01-01' },
  { id: '34', name: '童年', slug: 'childhood', articleCount: 1, createdAt: '2024-01-01' },
  { id: '35', name: '美食', slug: 'food', articleCount: 1, createdAt: '2024-01-01' },
  { id: '36', name: '经典', slug: 'classic', articleCount: 2, createdAt: '2024-01-01' },
  { id: '37', name: '诗歌', slug: 'poetry', articleCount: 1, createdAt: '2024-01-01' },
  { id: '38', name: '爱情', slug: 'love', articleCount: 1, createdAt: '2024-01-01' },
  { id: '39', name: '工具', slug: 'tool', articleCount: 3, createdAt: '2024-01-01' },
  { id: '40', name: '部署', slug: 'deployment', articleCount: 2, createdAt: '2024-01-01' }
];

// 获取所有标签
export function getTags(): Tag[] {
  return mockTags;
}

// 获取热门标签（按文章数排序）
export function getHotTags(limit = 20): Tag[] {
  return [...mockTags]
    .sort((a, b) => (b.articleCount || 0) - (a.articleCount || 0))
    .slice(0, limit);
}

// 根据关键词搜索标签
export function searchTags(keyword: string): Tag[] {
  if (!keyword.trim()) return [];
  const kw = keyword.toLowerCase();
  return mockTags.filter(tag => 
    tag.name.toLowerCase().includes(kw) || 
    tag.slug.toLowerCase().includes(kw)
  );
}

// 创建新标签
export function createTag(name: string): Tag {
  const slug = name.toLowerCase().replace(/\s+/g, '-');
  const newTag: Tag = {
    id: Date.now().toString(),
    name,
    slug,
    articleCount: 0,
    createdAt: new Date().toISOString().split('T')[0] ?? ''
  };
  mockTags.unshift(newTag);
  return newTag;
}

// 从文章中提取标签建议（基于标题和分类）
export function getTagSuggestions(title: string, category: string): Tag[] {
  const suggestions: Tag[] = [];
  const titleLower = title.toLowerCase();
  
  // 首先匹配分类相关的标签
  const categoryKeywords: Record<string, string[]> = {
    '技术栈手册': ['前端', 'JavaScript', 'Vue', 'React', 'CSS', 'TypeScript'],
    '架构札记': ['后端', '架构', '微服务', '性能', 'Node.js'],
    '工具指南': ['工具', 'Docker', 'Git', 'DevOps', '部署'],
    'AI编程': ['AI', 'Python', '机器学习', '深度学习'],
    '开源日志': ['开源', 'GitHub', '社区'],
    '人间烟火': ['人生', '故乡', '美食', '童年'],
    '山河行吟': ['旅行', '美食', '生活'],
    '心灵独白': ['思考', '人生', '经典', '诗歌'],
    '话题讨论': ['社区', '创作', '成长']
  };
  
  // 匹配分类相关标签
  const categoryTags = categoryKeywords[category];
  if (categoryTags) {
    categoryTags.forEach(keyword => {
      const tag = mockTags.find(t => t.name === keyword);
      if (tag) {
        suggestions.push(tag);
      }
    });
  }
  
  // 匹配标题中包含的标签
  mockTags.forEach(tag => {
    if (titleLower.includes(tag.name.toLowerCase()) && !suggestions.find(s => s.id === tag.id)) {
      suggestions.push(tag);
    }
  });
  
  // 如果没有建议，返回热门标签
  if (suggestions.length === 0) {
    return getHotTags(10);
  }
  
  return suggestions.slice(0, 15);
}
