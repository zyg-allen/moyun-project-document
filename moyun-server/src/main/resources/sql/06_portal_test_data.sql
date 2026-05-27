-- =============================================
-- 墨韵智库 - 门户测试数据脚本
-- 版本: v2.0
-- 创建时间: 2026-05-28
-- 描述: 包含用户、文章等测试数据（分类和标签请执行 05_moyun_v2_init.sql）
-- =============================================

-- ----------------------------
-- 1. 测试用户（不同权限等级）
-- 密码都是：123456（BCrypt加密后）
-- ----------------------------
INSERT INTO `portal_user` (`username`, `nickname`, `email`, `phone`, `password`, `avatar`, `bio`, `position`, `role`, `status`, `create_by`) VALUES
('admin', '墨韵管理员', 'admin@moyun.com', '13800138000', '$2a$10$7JB720yubVSZvUI0rEqK/.VqGOZTH.ulu33dHOiBE8ByOhJIbtS7O', 'https://api.dicebear.com/7.x/avataaars/svg?seed=admin', '墨韵智库管理员，致力于打造优质内容社区', '产品经理', 'admin', '0', 'admin'),
('zhangsan', '张三', 'zhangsan@moyun.com', '13800138001', '$2a$10$7JB720yubVSZvUI0rEqK/.VqGOZTH.ulu33dHOiBE8ByOhJIbtS7O', 'https://api.dicebear.com/7.x/avataaars/svg?seed=zhangsan', '热爱技术，喜欢分享，前端开发工程师', '前端工程师', 'user', '0', 'admin'),
('lisi', '李四', 'lisi@moyun.com', '13800138002', '$2a$10$7JB720yubVSZvUI0rEqK/.VqGOZTH.ulu33dHOiBE8ByOhJIbtS7O', 'https://api.dicebear.com/7.x/avataaars/svg?seed=lisi', 'Java后端开发，专注微服务架构', '后端工程师', 'user', '0', 'admin'),
('wangwu', '王五', 'wangwu@moyun.com', '13800138003', '$2a$10$7JB720yubVSZvUI0rEqK/.VqGOZTH.ulu33dHOiBE8ByOhJIbtS7O', 'https://api.dicebear.com/7.x/avataaars/svg?seed=wangwu', '文学爱好者，喜欢写散文', '自由职业', 'user', '0', 'admin');

-- ----------------------------
-- 2. 测试文章（技术笔记 - 15篇，用于分页测试）
-- 注意: 执行前请确保已执行 07_moyun_v2_init.sql 创建分类数据
-- ----------------------------
INSERT INTO `portal_article` (`title`, `content`, `excerpt`, `cover`, `author_id`, `category_id`, `status`, `is_featured`, `is_top`, `is_carousel`, `views`, `likes`, `comments`, `published_at`, `create_by`) VALUES
('Vue3 组合式 API 完全指南', '<h2>什么是组合式 API</h2><p>Vue3 引入了组合式 API，这是一种新的编写组件逻辑的方式。</p><pre><code>import { ref, computed, onMounted } from \'vue\'\nexport default {\n  setup() {\n    const count = ref(0)\n    const doubled = computed(() => count.value * 2)\n    \n    onMounted(() => {\n      console.log(\'组件已挂载\')\n    })\n    \n    return { count, doubled }\n  }\n}</code></pre>', 'Vue3 组合式 API 完全指南，带你从零开始掌握 setup 函数、ref、reactive、computed 等核心概念。', 'https://picsum.photos/seed/vue3-1/800/400', 2, 10, 'published', 1, 1, 1, 1520, 256, 32, NOW(), 'admin'),
('深入理解 Vue3 响应式原理', '<h2>Proxy vs Object.defineProperty</h2><p>Vue3 使用 Proxy 替代了 Object.defineProperty 来实现响应式。</p>', '深入解析 Vue3 的响应式原理，对比 Proxy 和 Object.defineProperty 的差异。', 'https://picsum.photos/seed/vue3-2/800/400', 2, 10, 'published', 1, 0, 0, 980, 178, 21, NOW(), 'admin'),
('Vue3 生命周期钩子详解', '<h2>生命周期钩子</h2><p>onBeforeMount, onMounted, onBeforeUpdate, onUpdated, onBeforeUnmount, onUnmounted</p>', 'Vue3 生命周期钩子的完整解析和最佳实践。', 'https://picsum.photos/seed/vue3-3/800/400', 2, 10, 'published', 0, 0, 0, 756, 123, 15, NOW(), 'admin'),
('Pinia 状态管理实战', '<h2>Pinia 简介</h2><p>Pinia 是 Vue3 官方推荐的状态管理库。</p>', 'Pinia 状态管理入门到实战，包含完整的项目示例。', 'https://picsum.photos/seed/pinia/800/400', 2, 10, 'published', 0, 0, 0, 623, 98, 12, NOW(), 'admin'),
('VueRouter4 新特性解析', '<h2>VueRouter4 新特性</h2><p>动态路由、路由守卫、组合式 API 支持</p>', 'VueRouter4 新特性完全解析，助力你的 Vue3 项目。', 'https://picsum.photos/seed/router/800/400', 2, 10, 'published', 0, 0, 0, 543, 87, 8, NOW(), 'admin'),
('TypeScript 在 Vue3 中的最佳实践', '<h2>为什么用 TypeScript</h2><p>类型安全、更好的 IDE 支持、更易维护的代码</p>', 'TypeScript 与 Vue3 结合的最佳实践指南。', 'https://picsum.photos/seed/ts/800/400', 2, 10, 'published', 0, 0, 0, 432, 65, 5, NOW(), 'admin'),
('React Hooks 深入理解', '<h2>Hooks 解决的问题</h2><p>在不编写 class 的情况下使用 state 以及其他的 React 特性</p>', 'React Hooks 从入门到精通，包含 useState, useEffect 等核心 Hook 的详解。', 'https://picsum.photos/seed/react-1/800/400', 2, 10, 'published', 0, 0, 0, 890, 145, 18, NOW(), 'admin'),
('React 性能优化指南', '<h2>性能优化策略</h2><p>memo, useMemo, useCallback, 懒加载</p>', 'React 应用性能优化的完整指南。', 'https://picsum.photos/seed/react-2/800/400', 2, 10, 'published', 0, 0, 0, 678, 112, 10, NOW(), 'admin'),
('Java8 Stream API 完全指南', '<h2>什么是 Stream</h2><p>Stream 是 Java8 中处理集合的关键抽象概念。</p>', 'Java8 Stream API 的完全教程，让你的代码更优雅。', 'https://picsum.photos/seed/java-1/800/400', 3, 11, 'published', 1, 0, 0, 1234, 201, 25, NOW(), 'admin'),
('Spring Boot 微服务架构实践', '<h2>微服务架构</h2><p>将单体应用拆分为多个小型服务</p>', '基于 Spring Boot 的微服务架构实战指南。', 'https://picsum.photos/seed/spring-1/800/400', 3, 11, 'published', 1, 0, 0, 1567, 289, 35, NOW(), 'admin'),
('MyBatis-Plus 高级用法', '<h2>MyBatis-Plus</h2><p>MyBatis 的增强工具，在 MyBatis 的基础上只做增强不做改变</p>', 'MyBatis-Plus 高级用法详解，提高开发效率。', 'https://picsum.photos/seed/mybatis/800/400', 3, 11, 'published', 0, 0, 0, 789, 134, 16, NOW(), 'admin'),
('Spring Security JWT 认证', '<h2>JWT 认证流程</h2><p>登录 → 获取 Token → 携带 Token 访问</p>', 'Spring Security 集成 JWT 认证完整教程。', 'https://picsum.photos/seed/jwt/800/400', 3, 11, 'published', 0, 0, 0, 654, 102, 12, NOW(), 'admin'),
('MySQL 索引优化实战', '<h2>索引类型</h2><p>普通索引、唯一索引、主键索引、联合索引</p>', 'MySQL 索引优化实战，提升查询性能。', 'https://picsum.photos/seed/mysql/800/400', 3, 11, 'published', 0, 0, 0, 823, 145, 18, NOW(), 'admin'),
('Redis 高性能缓存实践', '<h2>Redis 数据结构</h2><p>String, Hash, List, Set, ZSet</p>', 'Redis 在项目中的高性能缓存实践。', 'https://picsum.photos/seed/redis/800/400', 3, 11, 'published', 0, 0, 0, 712, 115, 13, NOW(), 'admin'),
('Docker 容器化部署指南', '<h2>什么是 Docker</h2><p>Docker 是一个开源的容器化平台</p>', 'Docker 容器化部署从入门到实战。', 'https://picsum.photos/seed/docker/800/400', 3, 11, 'published', 0, 0, 0, 567, 89, 7, NOW(), 'admin');

-- ----------------------------
-- 3. 测试文章（散文天地 - 8篇）
-- ----------------------------
INSERT INTO `portal_article` (`title`, `content`, `excerpt`, `cover`, `author_id`, `category_id`, `status`, `is_featured`, `is_top`, `is_carousel`, `views`, `likes`, `comments`, `published_at`, `create_by`) VALUES
('春日里的杭州西湖', '<h2>西湖美景</h2><p>清明时节，西湖岸边柳絮纷飞，正是春游好时节。</p>', '春日里游杭州西湖，感受江南水乡的诗意与浪漫。', 'https://picsum.photos/seed/westlake/800/400', 4, 12, 'published', 1, 0, 1, 2345, 456, 67, NOW(), 'admin'),
('城市夜归人', '<h2>深夜的城市</h2><p>夜晚十点的地铁，载着疲惫的人们回家。</p>', '记录城市里普通人为生活打拼的日常故事。', 'https://picsum.photos/seed/night/800/400', 4, 15, 'published', 0, 0, 0, 1876, 345, 45, NOW(), 'admin'),
('我的读书时光', '<h2>阅读的乐趣</h2><p>在这个信息爆炸的时代，静下心来读书是一种奢侈。</p>', '分享我的读书心得和阅读方法。', 'https://picsum.photos/seed/reading/800/400', 4, 20, 'published', 0, 0, 0, 1234, 234, 34, NOW(), 'admin'),
('故乡的四季', '<h2>故乡的回忆</h2><p>故乡的春夏秋冬，每一季都有独特的风景。</p>', '回忆故乡的四季变化，那些美好的童年时光。', 'https://picsum.photos/seed/hometown/800/400', 4, 14, 'published', 0, 0, 0, 1567, 289, 41, NOW(), 'admin'),
('咖啡馆里的下午', '<h2>慢时光</h2><p>一杯咖啡，一本书，一个慵懒的下午。</p>', '在咖啡馆里度过的美好时光。', 'https://picsum.photos/seed/coffee/800/400', 4, 13, 'published', 0, 0, 0, 987, 178, 23, NOW(), 'admin'),
('雨中漫步', '<h2>雨的诗意</h2><p>下雨了，撑一把伞，在雨中漫步。</p>', '感受雨中的宁静与美好。', 'https://picsum.photos/seed/rain/800/400', 4, 13, 'published', 0, 0, 0, 654, 123, 15, NOW(), 'admin'),
('一封家书', '<h2>致远方的家人</h2><p>亲爱的爸爸妈妈，见字如面。</p>', '一封写给远方家人的信。', 'https://picsum.photos/seed/letter/800/400', 4, 16, 'published', 0, 0, 0, 543, 98, 12, NOW(), 'admin'),
('秋意浓', '<h2>秋天来了</h2><p>树叶变黄，天气渐凉，秋天是一个诗意的季节。</p>', '描写秋天的美景和感受。', 'https://picsum.photos/seed/autumn/800/400', 4, 14, 'published', 0, 0, 0, 789, 134, 18, NOW(), 'admin');

-- ----------------------------
-- 4. 测试文章（读书空间 - 6篇）
-- ----------------------------
INSERT INTO `portal_article` (`title`, `content`, `excerpt`, `cover`, `author_id`, `category_id`, `status`, `is_featured`, `is_top`, `is_carousel`, `views`, `likes`, `comments`, `published_at`, `create_by`) VALUES
('《活着》读后感', '<h2>活着的意义</h2><p>余华的《活着》讲述了福贵一生的苦难与希望。</p>', '读《活着》有感，探讨生命的意义与价值。', 'https://picsum.photos/seed/huozhe/800/400', 4, 20, 'published', 1, 0, 0, 1456, 234, 28, NOW(), 'admin'),
('程序员必读的10本书', '<h2>推荐书单</h2><p>作为程序员，这些书值得一读再读。</p>', '推荐程序员必读的10本经典书籍。', 'https://picsum.photos/seed/books10/800/400', 2, 21, 'published', 0, 0, 0, 987, 167, 19, NOW(), 'admin'),
('如何高效阅读', '<h2>阅读方法</h2><p>掌握正确的阅读方法，让阅读更高效。</p>', '分享高效阅读的方法和技巧。', 'https://picsum.photos/seed/read/800/400', 3, 22, 'published', 0, 0, 0, 765, 123, 14, NOW(), 'admin'),
('《深入理解计算机系统》笔记', '<h2>CSAPP</h2><p>这本书是计算机专业必读的经典之作。</p>', '《深入理解计算机系统》读书笔记。', 'https://picsum.photos/seed/csapp/800/400', 3, 21, 'published', 0, 0, 0, 654, 98, 11, NOW(), 'admin'),
('唐诗三百首赏析', '<h2>唐诗之美</h2><p>品读唐诗，感受古典文学的魅力。</p>', '唐诗三百首经典作品赏析。', 'https://picsum.photos/seed/tangshi/800/400', 4, 20, 'published', 0, 0, 0, 543, 87, 9, NOW(), 'admin'),
('我的2024年读书清单', '<h2>年终总结</h2><p>2024年读了50本书，分享我的阅读清单。</p>', '2024年读书总结与推荐。', 'https://picsum.photos/seed/2024books/800/400', 2, 23, 'published', 0, 0, 0, 432, 65, 7, NOW(), 'admin');

-- ----------------------------
-- 5. 测试文章（面试指南 - 5篇）
-- ----------------------------
INSERT INTO `portal_article` (`title`, `content`, `excerpt`, `cover`, `author_id`, `category_id`, `status`, `is_featured`, `is_top`, `is_carousel`, `views`, `likes`, `comments`, `published_at`, `create_by`) VALUES
('Java面试题大全', '<h2>面试必备</h2><p>Java开发常见面试题整理。</p>', 'Java开发面试题整理，助力你拿到心仪offer。', 'https://picsum.photos/seed/javainterview/800/400', 3, 25, 'published', 1, 1, 0, 2345, 356, 42, NOW(), 'admin'),
('我的面试经历分享', '<h2>面试复盘</h2><p>分享我的春招面试经历和心得。</p>', '真实面试经历分享，希望对大家有帮助。', 'https://picsum.photos/seed/interviewexp/800/400', 2, 26, 'published', 0, 0, 0, 1876, 289, 35, NOW(), 'admin'),
('如何写一份优秀的简历', '<h2>简历技巧</h2><p>简历是求职的第一步，很重要。</p>', '简历优化技巧，让你的简历脱颖而出。', 'https://picsum.photos/seed/resume/800/400', 3, 27, 'published', 0, 0, 0, 1234, 201, 25, NOW(), 'admin'),
('前端面试高频考点', '<h2>前端面试</h2><p>前端开发常见面试题总结。</p>', '前端面试高频考点整理。', 'https://picsum.photos/seed/frontendinterview/800/400', 2, 25, 'published', 0, 0, 0, 987, 156, 18, NOW(), 'admin'),
('程序员职业规划', '<h2>职业发展</h2><p>技术人的职业发展路径规划。</p>', '分享程序员职业规划的一些思考。', 'https://picsum.photos/seed/career/800/400', 3, 29, 'published', 0, 0, 0, 765, 123, 14, NOW(), 'admin');

-- ----------------------------
-- 6. 测试文章（技能工坊 - 5篇）
-- ----------------------------
INSERT INTO `portal_article` (`title`, `content`, `excerpt`, `cover`, `author_id`, `category_id`, `status`, `is_featured`, `is_top`, `is_carousel`, `views`, `likes`, `comments`, `published_at`, `create_by`) VALUES
('如何提升写作能力', '<h2>写作技巧</h2><p>好的写作能力需要长期练习和积累。</p>', '分享提升写作能力的方法和技巧。', 'https://picsum.photos/seed/writing/800/400', 4, 30, 'published', 1, 0, 0, 1456, 234, 28, NOW(), 'admin'),
('Git高级技巧', '<h2>Git</h2><p>Git是程序员必备的版本控制工具。</p>', 'Git高级使用技巧，提高开发效率。', 'https://picsum.photos/seed/git/800/400', 3, 31, 'published', 0, 0, 0, 1123, 187, 22, NOW(), 'admin'),
('高效学习方法', '<h2>学习方法</h2><p>掌握正确的学习方法，事半功倍。</p>', '分享高效学习的方法和心得。', 'https://picsum.photos/seed/learn/800/400', 2, 32, 'published', 0, 0, 0, 987, 156, 18, NOW(), 'admin'),
('VSCode插件推荐', '<h2>效率工具</h2><p>好的工具能大大提升开发效率。</p>', 'VSCode必备插件推荐。', 'https://picsum.photos/seed/vscode/800/400', 2, 33, 'published', 0, 0, 0, 876, 134, 16, NOW(), 'admin'),
('坚持写作100天', '<h2>输出训练</h2><p>写作是最好的思考方式。</p>', '分享我坚持写作100天的收获。', 'https://picsum.photos/seed/100days/800/400', 4, 34, 'published', 0, 0, 0, 654, 98, 11, NOW(), 'admin');

-- ----------------------------
-- 7. 测试文章（社区互动 - 5篇）
-- ----------------------------
INSERT INTO `portal_article` (`title`, `content`, `excerpt`, `cover`, `author_id`, `category_id`, `status`, `is_featured`, `is_top`, `is_carousel`, `views`, `likes`, `comments`, `published_at`, `create_by`) VALUES
('欢迎加入墨韵智库', '<h2>社区公告</h2><p>欢迎大家来到墨韵智库，一起交流学习！</p>', '社区公告，欢迎新成员加入。', 'https://picsum.photos/seed/welcome/800/400', 1, 35, 'published', 1, 1, 1, 3456, 567, 78, NOW(), 'admin'),
('征文活动公告', '<h2>活动预告</h2><p>墨韵智库第一届征文活动开始啦！</p>', '社区征文活动公告，期待大家的参与。', 'https://picsum.photos/seed/contest/800/400', 1, 36, 'published', 0, 0, 0, 2345, 345, 45, NOW(), 'admin'),
('2024年度总结', '<h2>年度总结</h2><p>回顾这一年，我们一起成长。</p>', '社区2024年度总结与展望。', 'https://picsum.photos/seed/2024summary/800/400', 1, 37, 'published', 0, 0, 0, 1876, 289, 35, NOW(), 'admin'),
('互评活动规则', '<h2>活动规则</h2><p>社区互评活动规则说明。</p>', '社区互评圈活动规则介绍。', 'https://picsum.photos/seed/reviewrules/800/400', 1, 38, 'published', 0, 0, 0, 1234, 198, 24, NOW(), 'admin'),
('打卡挑战活动', '<h2>打卡活动</h2><p>30天写作打卡挑战开始了！</p>', '社区成长打卡活动介绍。', 'https://picsum.photos/seed/checkin/800/400', 1, 39, 'published', 0, 0, 0, 987, 156, 18, NOW(), 'admin');

-- ----------------------------
-- 8. 测试评论
-- ----------------------------
INSERT INTO `portal_comment` (`article_id`, `author_id`, `content`, `parent_id`, `like_count`, `status`, `create_by`) VALUES
(1, 3, '写得太好了！Vue3 的组合式 API 确实比选项式 API 更灵活。', 0, 25, '0', 'admin'),
(2, 4, '收藏了，慢慢学习。', 0, 18, '0', 'admin'),
(3, 2, '深入浅出，理解了 Proxy 的原理。', 0, 15, '0', 'admin'),
(9, 2, 'Stream API 让代码更简洁了！', 0, 20, '0', 'admin'),
(10, 4, '微服务架构是趋势，学习了。', 0, 16, '0', 'admin'),
(16, 2, '写得太美了，想去西湖看看。', 0, 30, '0', 'admin'),
(17, 3, '江南好，风景旧曾谙。', 0, 22, '0', 'admin'),
(18, 4, '打工人的真实写照。', 0, 18, '0', 'admin'),
(24, 2, '《活着》确实是本好书。', 0, 25, '0', 'admin'),
(30, 4, '面试题整理得很全面！', 0, 28, '0', 'admin'),
(35, 3, '欢迎欢迎！', 0, 35, '0', 'admin');

-- ----------------------------
-- 9. 测试通知
-- ----------------------------
INSERT INTO `portal_notification` (`user_id`, `type`, `title`, `content`, `data`, `is_read`) VALUES
(2, 'comment', '你的文章收到了新评论', '张三评论了你的文章《Vue3 组合式 API 完全指南》', '{"articleId": 1, "commentId": 1}', 0),
(2, 'like', '你的文章被点赞了', '李四点赞了你的文章《Vue3 组合式 API 完全指南》', '{"articleId": 1}', 0),
(3, 'comment', '你的文章收到了新评论', '张三评论了你的文章《React Hooks 深入理解》', '{"articleId": 7, "commentId": 3}', 1),
(4, 'comment', '你的文章收到了新评论', '张三评论了你的文章《春日里的杭州西湖》', '{"articleId": 16, "commentId": 6}', 0),
(2, 'system', '系统通知', '欢迎加入墨韵智库！', NULL, 1),
(3, 'system', '系统通知', '你的文章已通过审核', NULL, 0);

-- ----------------------------
-- 完成！
-- ----------------------------
SELECT '测试数据初始化完成！共 43 篇文章，4 个用户' AS message;
SELECT '注意: 分类和标签数据请通过 07_moyun_v2_init.sql 创建' AS note;
