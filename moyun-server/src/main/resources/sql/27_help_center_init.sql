-- =====================================================
-- 墨韵·智库 - 帮助中心模块
-- 版本: v1.0
-- 日期: 2026-06-22
-- 说明: 帮助分类 + 帮助文章，可重复执行
-- =====================================================

SET @OLD_FOREIGN_KEY_CHECKS=@@FOREIGN_KEY_CHECKS;
SET FOREIGN_KEY_CHECKS=0;

-- =====================================================
-- 一、帮助分类表
-- =====================================================
DROP TABLE IF EXISTS `portal_help_category`;
CREATE TABLE `portal_help_category` (
  `id`           bigint       NOT NULL AUTO_INCREMENT COMMENT '分类ID',
  `name`         varchar(100) NOT NULL COMMENT '分类名称',
  `icon`         varchar(100) DEFAULT NULL COMMENT '图标（lucide 图标名）',
  `description`  varchar(500) DEFAULT NULL COMMENT '分类描述',
  `sort`         int          DEFAULT 0 COMMENT '排序（升序）',
  `status`       varchar(20)  DEFAULT 'active' COMMENT '状态：active/inactive',
  `create_by`    varchar(64)  DEFAULT '' COMMENT '创建者',
  `create_time`  datetime     DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_by`    varchar(64)  DEFAULT '' COMMENT '更新者',
  `update_time`  datetime     DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `remark`       varchar(500) DEFAULT NULL COMMENT '备注',
  PRIMARY KEY (`id`),
  KEY `idx_status` (`status`),
  KEY `idx_sort` (`sort`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='帮助中心分类表';

-- =====================================================
-- 二、帮助文章表
-- =====================================================
DROP TABLE IF EXISTS `portal_help_article`;
CREATE TABLE `portal_help_article` (
  `id`           bigint       NOT NULL AUTO_INCREMENT COMMENT '文章ID',
  `category_id`  bigint       NOT NULL COMMENT '分类ID',
  `title`        varchar(200) NOT NULL COMMENT '问题标题',
  `content`      text         NOT NULL COMMENT '答案内容（支持纯文本）',
  `view_count`   int          DEFAULT 0 COMMENT '查看次数',
  `like_count`   int          DEFAULT 0 COMMENT '点赞次数',
  `sort`         int          DEFAULT 0 COMMENT '排序（升序）',
  `is_featured`  tinyint      DEFAULT 0 COMMENT '是否精选：0=否 1=是',
  `status`       varchar(20)  DEFAULT 'published' COMMENT '状态：published/draft',
  `create_by`    varchar(64)  DEFAULT '' COMMENT '创建者',
  `create_time`  datetime     DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_by`    varchar(64)  DEFAULT '' COMMENT '更新者',
  `update_time`  datetime     DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `remark`       varchar(500) DEFAULT NULL COMMENT '备注',
  PRIMARY KEY (`id`),
  KEY `idx_category_id` (`category_id`),
  KEY `idx_status` (`status`),
  KEY `idx_is_featured` (`is_featured`),
  KEY `idx_sort` (`sort`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='帮助中心文章表';

-- =====================================================
-- 三、测试数据 - 分类（4个）
-- =====================================================
INSERT INTO `portal_help_category` (`id`, `name`, `icon`, `description`, `sort`, `status`) VALUES
(1, '发布与编辑', 'BookOpen',   '文章发布、编辑、删除等操作指南', 1, 'active'),
(2, '账号与安全', 'HelpCircle', '登录、注册、密码、安全设置',    2, 'active'),
(3, '互动功能',   'MessageSquare', '评论、点赞、关注等互动功能', 3, 'active'),
(4, '社区规则',   'Shield',     '使用规范、违规处理、隐私政策',   4, 'active');

-- =====================================================
-- 四、测试数据 - 文章（12篇）
-- =====================================================
INSERT INTO `portal_help_article` (`id`, `category_id`, `title`, `content`, `view_count`, `like_count`, `sort`, `is_featured`, `status`) VALUES
-- 分类1：发布与编辑
(1, 1, '如何发布文章？',
 '点击页面右上角的"创作"按钮，进入文章编辑页面。填写标题、内容、分类、标签等信息后，点击"发布文章"按钮即可发布。\n\n支持 Markdown 和富文本两种编辑模式，可插入图片、代码块、链接等元素。',
 1250, 89, 1, 1, 'published'),
(2, 1, '如何编辑或删除已发布的文章？',
 '进入个人中心 → 我的文章，找到目标文章：\n\n1. 编辑：点击"编辑"按钮，修改内容后保存即可\n2. 删除：点击"删除"按钮，确认后文章将被永久删除\n\n注意：删除操作不可恢复，请谨慎操作。',
 890, 56, 2, 0, 'published'),
(3, 1, '文章支持哪些编辑格式？',
 '墨韵智库支持两种编辑模式：\n\n1. Markdown 模式：适合技术用户，支持代码高亮、表格、列表等\n2. 富文本模式：适合文学用户，所见即所得，支持图片、视频嵌入\n\n两种模式可在编辑页面顶部切换。',
 670, 34, 3, 0, 'published'),

-- 分类2：账号与安全
(4, 2, '如何注册账号？',
 '点击页面右上角的"注册"按钮，填写用户名、邮箱、密码即可完成注册。\n\n注册后请完善个人资料，包括昵称、头像、个人简介等，有助于其他用户了解你。',
 1560, 102, 1, 1, 'published'),
(5, 2, '如何修改密码？',
 '进入个人中心 → 账号设置 → 修改密码，输入旧密码和新密码后保存即可。\n\n建议定期修改密码，使用包含字母、数字、特殊字符的强密码，提高账号安全性。',
 980, 67, 2, 0, 'published'),
(6, 2, '忘记密码怎么办？',
 '在登录页面点击"忘记密码"链接，输入注册邮箱，系统会发送重置链接到你的邮箱。\n\n点击邮件中的链接，设置新密码即可。链接有效期为 24 小时。',
 1340, 78, 3, 1, 'published'),
(7, 2, '如何修改个人资料？',
 '进入个人中心，点击"编辑个人资料"，在弹出的窗口中修改昵称、头像、个人简介等信息后保存即可。\n\n个人资料会展示在你的作者主页和文章详情页。',
 760, 45, 4, 0, 'published'),

-- 分类3：互动功能
(8, 3, '如何关注其他用户？',
 '在作者主页或文章详情页，点击作者头像或用户名进入作者主页，然后点击"关注"按钮即可。\n\n关注后，该用户的新文章会出现在你的动态中。',
 890, 56, 1, 0, 'published'),
(9, 3, '如何点赞和收藏文章？',
 '在文章详情页底部，可以找到点赞和收藏按钮：\n\n1. 点赞：点击爱心图标，再次点击取消\n2. 收藏：点击书签图标，可在个人中心 → 我的收藏查看\n\n点赞和收藏是对作者最好的支持。',
 1120, 89, 2, 1, 'published'),
(10, 3, '如何发表评论？',
 '在文章详情页底部评论区，输入你的评论内容后点击"发表"按钮即可。\n\n支持回复其他用户的评论，形成讨论串。请遵守社区规范，文明评论。',
 780, 34, 3, 0, 'published'),

-- 分类4：社区规则
(11, 4, '文章审核需要多长时间？',
 '文章提交后，通常会在 1-3 个工作日内完成审核。审核通过后文章即可公开发布。\n\n审核标准：内容原创、无违规信息、格式规范。如审核未通过，会通过站内信通知原因。',
 1450, 95, 1, 1, 'published'),
(12, 4, '如何举报违规内容？',
 '在文章详情页或用户主页，点击"举报"按钮，选择举报原因并填写说明后提交即可。\n\n举报类型包括：垃圾广告、色情低俗、违法违规、抄袭侵权、人身攻击等。我们会在 24 小时内处理。',
 670, 45, 2, 0, 'published');

SET FOREIGN_KEY_CHECKS=@OLD_FOREIGN_KEY_CHECKS;

-- =====================================================
-- 初始化完成
-- 统计：4 个分类 + 12 篇文章
-- =====================================================
