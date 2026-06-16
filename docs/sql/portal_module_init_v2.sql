-- =====================================================
-- 墨韵·智库 - 面试空间 + 通用标签初始化数据 v2.0
-- 生成时间: 2026-06-15
-- 编码: utf8mb4
-- =====================================================

-- =====================================================
-- 一、面试分类 portal_interview_category
-- =====================================================
INSERT INTO `portal_interview_category` (`id`, `name`, `slug`, `description`, `icon`, `sort`, `question_count`, `status`, `create_time`) VALUES
(1, 'Java 后端',  'java-backend',     'Java 后端开发常见面试题',     'java.png',   1, 0, '0', NOW()),
(2, 'Go 语言',    'golang',           'Golang 基础与并发编程',       'go.png',     2, 0, '0', NOW()),
(3, '前端开发',   'frontend',         'HTML/CSS/JS/Vue/React 面试',  'frontend.png', 3, 0, '0', NOW()),
(4, '数据库',     'database',         'MySQL / Redis / MongoDB',     'database.png', 4, 0, '0', NOW()),
(5, '系统设计',   'system-design',    '高并发、分布式、架构设计',    'architecture.png', 5, 0, '0', NOW()),
(6, '计算机网络', 'network',          'TCP/IP、HTTP、网络模型',       'network.png', 6, 0, '0', NOW()),
(7, '算法与数据结构', 'algorithm',    '常见算法题与数据结构',         'algorithm.png', 7, 0, '0', NOW()),
(8, '安全',       'security',         'Web 安全、加密、鉴权',         'security.png', 8, 0, '0', NOW());


-- =====================================================
-- 二、面试公司 portal_interview_company
-- =====================================================
INSERT INTO `portal_interview_company` (`id`, `name`, `slug`, `logo`, `description`, `industry`, `question_count`, `sort`, `status`, `create_time`) VALUES
(1,  '阿里巴巴', 'alibaba',   'alibaba.png',   '互联网电商 / 云计算',     '互联网', 0, 1,  '0', NOW()),
(2,  '腾讯',     'tencent',   'tencent.png',   '社交 / 游戏 / 云',        '互联网', 0, 2,  '0', NOW()),
(3,  '百度',     'baidu',     'baidu.png',     '搜索 / AI / 云',          '互联网', 0, 3,  '0', NOW()),
(4,  '字节跳动', 'bytedance', 'bytedance.png', '短视频 / 资讯 / AI',       '互联网', 0, 4,  '0', NOW()),
(5,  '美团',     'meituan',   'meituan.png',  '本地生活 / 外卖',         '互联网', 0, 5,  '0', NOW()),
(6,  '京东',     'jd',               'jd.png',            '零售 / 物流',           '互联网', 0, 6,  '0', NOW()),
(7,  '华为',     'huawei',           'huawei.png',        '通信 / 终端 / 云',       '通信设备', 0, 7,  '0', NOW()),
(8,  '小米',     'xiaomi',           'xiaomi.png',        '智能硬件 / 互联网',     '智能硬件', 0, 8,  '0', NOW()),
(9,  'Bilibili', 'bilibili',         'bilibili.png',      '视频 / 游戏',            '互联网', 0, 9,  '0', NOW()),
(10, '滴滴',     'didi',             'didi.png',          '出行',                    '互联网', 0, 10, '0', NOW()),
(11, '快手',     'kuaishou',         'kuaishou.png',      '短视频 / 直播',          '互联网', 0, 11, '0', NOW()),
(12, '微软',     'microsoft',        'microsoft.png',     '操作系统 / 云 / AI',     '跨国科技', 0, 12, '0', NOW());


-- =====================================================
-- 三、通用标签 portal_tag（module=common）
-- =====================================================
INSERT INTO `portal_tag` (`id`, `name`, `slug`, `sort`, `status`, `module`, `reference_count`) VALUES
(1,  '高频题',     'high-frequency',   1,  '0', 'common', 0),
(2,  '经典题',     'classic',          2,  '0', 'common', 0),
(3,  '中等难度',   'medium',           3,  '0', 'common', 0),
(4,  '实战',       'hands-on',         4,  '0', 'common', 0),
(5,  '算法',       'algo',             5,  '0', 'common', 0),
(6,  '系统设计',   'sys-design',       6,  '0', 'common', 0),
(7,  '分布式',     'distributed',      7,  '0', 'common', 0),
(8,  'Java',       'java',             8,  '0', 'common', 0),
(9,  'MySQL',      'mysql',            9,  '0', 'common', 0),
(10, 'Redis',      'redis',            10, '0', 'common', 0),
(11, '并发',       'concurrent',       11, '0', 'common', 0),
(12, '面试官爱问', 'interviewer-fav',  12, '0', 'common', 0),
(13, '八股文',     'eight-part',       13, '0', 'common', 0),
(14, '真题',       'real-question',    14, '0', 'common', 0),
(15, '大厂',       'big-tech',         15, '0', 'common', 0);


-- =====================================================
-- 初始化脚本结束
-- =====================================================
