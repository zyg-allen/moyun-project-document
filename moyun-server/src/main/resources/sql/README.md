## 墨韵智库 - 数据库初始化脚本

### 执行顺序

请按以下顺序依次执行 SQL 脚本：

| 序号 | 文件名 | 说明 | 执行时机 |
|------|--------|------|----------|
| 01 | **01_moyun_init.sql** | 系统管理基础脚本 | 最先执行 |
| 02 | **02_workflow_init.sql** | 工作流与定时任务脚本 | 在 01 之后 |
| 03 | **03_portal_init.sql** | 门户系统表结构脚本 | 在 02 之后 |
| 04 | **04_cms_menu_init.sql** | CMS菜单和权限初始化脚本 | 在 03 之后 |
| 05 | **05_moyun_v2_init.sql** | 完整分类体系和标签体系 | 在 04 之后 |
| 06 | **06_portal_test_data.sql** | 用户和文章测试数据脚本 | 在 05 之后（依赖分类ID） |
| 07 | **07_reading_interview_init.sql** | 读书空间与面试空间表结构 | 在 06 之后 |
| 08 | **08_add_category_recommended_field.sql** | 分类推荐字段 | 在 07 之后 |
| 09 | **09_modify_cover_field.sql** | 封面字段修改 | 在 08 之后 |
| 10 | **10_add_category_path_field.sql** | 分类路径字段 | 在 09 之后 |
| 11 | **11_create_sys_file_table.sql** | 文件表 | 在 10 之后 |
| 12 | **12_add_comment_fields.sql** | 评论字段 | 在 11 之后 |
| 13 | **13_create_article_view_table.sql** | 文章浏览表 | 在 12 之后 |
| 14 | **14_V1.7_portal_user_add_extend_fields.sql** | 用户扩展字段 | 在 13 之后 |
| 15 | **15_add_article_slug_field.sql** | 文章 slug 字段 | 在 14 之后 |
| 16 | **16_reading_space_add_fields.sql** | 读书空间字段 | 在 15 之后 |
| 17 | **17_portal_book_menu_init.sql** | 读书空间菜单 | 在 16 之后 |
| 18 | **18_interview_menu_init.sql** | 面试空间菜单 | 在 17 之后 |
| 19 | **19_growth_system_init.sql** | 成长体系建表+初始化数据 | 在 18 之后 |
| 20 | **20_resume_template_like.sql** | 简历模板点赞表 | 在 19 之后 |
| 21 | **21_submission_featured.sql** | 提交笔记精选字段 | 在 20 之后 |
| 22 | **22_booklist_bookmark.sql** | 书单收藏表 | 在 21 之后 |
| 23 | **23_growth_admin_menu.sql** | 成长体系后台管理菜单（NEW） | 在 22 之后 |
| 24 | **24_featured_note_menu.sql** | 精选笔记管理菜单（NEW） | 在 23 之后 |
| 25 | **25_fix_operlog_path-new.sql** | 修复操作日志路径 | 在 24 之后 |
| 26 | **26_reading_interview_test_data.sql** | 读书空间与面试空间测试数据 | 在 25 之后 |
| 27 | **27_help_center_init.sql** | 帮助中心初始化 | 在 26 之后 |
| 28 | **28_alter_tables_add_base_fields.sql** | 表补充 BaseEntity 字段（首批） | 在 27 之后 |
| 29 | **29_alter_all_tables_base_fields.sql** | 全表补充 BaseEntity 字段 | 在 28 之后 |
| 30 | **30_alter_portal_tag_fields.sql** | portal_tag 表补充 module/reference_count 字段 | 在 29 之后 |
| 31 | **31_create_portal_entity_tag.sql** | 通用实体标签关联表 | 在 30 之后 |
| 32 | **32_cleanup_tag_name_prefix.sql** | 清理标签名 `#` 前缀 | 在 31 之后 |
| 33 | **33_alter_article_status_enum.sql** | 文章 status 字段枚举注释 | 在 32 之后 |
| 34 | **34_merge_notification_tables.sql** | 通知表合并（portal_notification + sys_notice → sys_notification + sys_notification_read） | 在 33 之后 |
| 35 | **35_add_notification_user_type.sql** | 通知表新增 user_type 字段 | 在 34 之后 |
| 36 | ~~36_init_notification_center_menu.sql~~ | **已废弃删除**（v4.0，见 40_fix_bugs_v4.sql） | - |
| 37 | **37_init_dashboard_config.sql** | Dashboard 配置初始化 | 在 35 之后 |
| 38 | **38_init_report_feedback_menu.sql** | 举报/反馈菜单及建表 | 在 37 之后 |
| 39 | **39_init_flowable_menu.sql** | 流程管理菜单初始化 | 在 38 之后 |
| 40 | **40_fix_bugs_v4.sql** | v4.0 综合 Bug 修复（友情链接 status + 通知菜单清理 + 评论菜单修复 + PortalReport 列长升级） | 在 39 之后 |

### 脚本详细说明

#### 1. 01_moyun_init.sql - 系统管理基础脚本
- **部门表** (sys_dept)
- **用户信息表** (sys_user)
- **岗位信息表** (sys_post)
- **角色信息表** (sys_role)
- **菜单权限表** (sys_menu)
- **字典类型表** (sys_dict_type)
- **字典数据表** (sys_dict_data)
- **系统配置表** (sys_config)
- **操作日志表** (sys_oper_log)
- **在线用户表** (sys_user_online)

#### 2. 02_workflow_init.sql - 工作流与定时任务脚本
- **Quartz 定时任务表**：QRTZ_JOB_DETAILS、QRTZ_TRIGGERS 等
- **Flowable 工作流表**：act_* 相关表

#### 3. 03_portal_init.sql - 门户系统表结构脚本
- **门户用户表** (portal_user)
- **门户分类表** (portal_category)
- **门户标签表** (portal_tag)
- **门户文章表** (portal_article)
- **门户评论表** (portal_comment)
- **门户收藏表** (portal_bookmark)
- **门户关注表** (portal_follow)
- **门户点赞表** (portal_like)
- **门户通知表** (portal_notification)
- **门户文章标签关联表** (portal_article_tag)
- **门户VIP表** (portal_vip)
- **门户订单表** (portal_order)
- **门户钱包表** (portal_wallet)
- **门户钱包交易表** (portal_wallet_transaction)
- **门户友情链接表** (portal_friend_link)

#### 4. 04_cms_menu_init.sql - CMS菜单和权限初始化脚本
- 创建"内容管理"一级菜单
- 创建"门户用户"、"文章管理"、"分类管理"、"标签管理"、"评论管理"二级菜单
- 配置相应的权限标识和按钮权限

#### 5. 05_moyun_v2_init.sql - 完整分类体系和标签体系脚本
- **7个一级栏目**：
  - 首页、散文天地、技术笔记、读书空间、面试指南、技能工坊、社区互动
- **32个二级分类**：
  - 每个一级栏目下有4-7个二级分类
  - 例如：散文天地下有人间烟火、山河行吟、心灵独白、城市笔记、四季专栏、声音散文、读者来信
- **28个标签**：
  - 人文类标签（8个）：生活哲思、城市记忆、自然写作、情感随笔、人间烟火、乡愁记忆、孤独成长、四季感悟
  - 技术类标签（12个）：SpringBoot实战、React Hooks、AI辅助开发、算法突破、Java并发、Vue3实践、微服务架构、MySQL优化、Git协作、前端性能、JVM调优、系统设计
  - 通用类标签（8个）：新手入门、进阶提升、面试备战、读书心得、写作技巧、学习方法、职场经验、个人成长

#### 6. 06_portal_test_data.sql - 用户和文章测试数据脚本
- **测试用户**：4个用户（1个管理员 + 3个普通用户）
- **测试文章**：43篇文章覆盖所有分类
- **测试评论**：多条测试评论
- **测试通知**：多条测试通知
- **注意**：此脚本依赖 05_moyun_v2_init.sql 创建的分类ID，必须在 05 之后执行

#### 19. 19_growth_system_init.sql - 全局用户成长体系建表+初始化数据
- **6张核心表**：
  - `portal_user_growth`：用户成长值总表（成长值、等级、头衔、赛季值）
  - `portal_growth_log`：成长事件流水表
  - `portal_user_stats`：用户统计聚合表（文章数、浏览量、获赞数、解题数、笔记数等）
  - `portal_growth_rule`：成长规则配置表（含 note_adopted、booklist_bookmarked 等规则）
  - `portal_achievement`：成就定义表（含 22 个成就）
  - `portal_user_badge`：用户徽章记录表
- **初始化数据**：20 条成长规则 + 22 个成就定义

#### 21. 21_submission_featured.sql - 提交笔记精选字段
- 为 `portal_interview_submission` 表新增 `is_featured` 和 `featured_time` 字段
- 支持 note_adopted 成长事件的后台管理功能

#### 22. 22_booklist_bookmark.sql - 书单收藏表
- 创建 `portal_book_list_bookmark` 表
- 支持 booklist_bookmarked 成长事件

#### 23. 23_growth_admin_menu.sql - 成长体系后台管理菜单（NEW）
- 创建"成长体系"一级菜单（与"面试空间"同级）
- 5个二级菜单：成长规则、成就管理、用户成长、用户徽章、成长流水
- 每个二级菜单含查询/新增/修改/删除按钮权限
- 为管理员角色分配所有权限
- **注意**：对应的前端页面（cms/growth/*）和后台 Controller 需后续创建

#### 24. 24_featured_note_menu.sql - 精选笔记管理菜单（NEW）
- 在"面试空间"下创建"精选笔记"二级菜单
- 含查询/采纳精选/取消精选按钮权限
- 采纳精选时自动触发 note_adopted 成长事件（+50 成长值）
- 为管理员角色分配所有权限
- **依赖**：18_interview_menu_init.sql（面试空间一级菜单）、21 创建的精选字段、19 创建的 note_adopted 成长规则
- **注意**：对应的前端页面（cms/interview/submission/index）和后台 Controller 接口需后续创建

#### 25. 25_fix_operlog_path-new.sql - 修复操作日志路径
- 修复操作日志中路径记录不准确的问题

#### 26. 26_reading_interview_test_data.sql - 读书空间与面试空间测试数据
- 为读书空间和面试空间模块补充测试数据

#### 27. 27_help_center_init.sql - 帮助中心初始化
- 创建帮助中心菜单及基础数据

#### 28-29. 28/29_alter_*_base_fields.sql - 全表补充 BaseEntity 字段
- 28：首批表补充 BaseEntity 审计字段（create_by/create_time/update_by/update_time/remark）
- 29：全表补充，确保所有业务表统一审计字段

#### 30-32. 标签体系改造
- **30_alter_portal_tag_fields.sql**：portal_tag 表补充 `module`（所属模块）和 `reference_count`（引用次数）字段及索引
- **31_create_portal_entity_tag.sql**：创建通用实体标签关联表 `portal_entity_tag`（tag_id + entity_type + entity_id）
- **32_cleanup_tag_name_prefix.sql**：清理标签名中的 `#` 前缀（数据库存纯文本，前端按需拼接）

#### 33. 33_alter_article_status_enum.sql - 文章状态枚举注释
- 补充 `portal_article.status` 字段枚举说明：`draft/pending/published/rejected/archived`
- 状态流转：draft → pending → published/rejected → pending（重新提交）→ archived

#### 34. 34_merge_notification_tables.sql - 通知表合并
- **背景**：原有两套通知表（portal_notification per-user 模型 + sys_notice 全局广播模型），模型冲突
- **方案**：采用主流平台两表结构
  - `sys_notification`（通知主体表）：scope=user 个人 / scope=all 广播
  - `sys_notification_read`（用户已读关系表）：NOT EXISTS 计算未读
- **操作**：迁移历史数据 → 旧表重命名为 `_bak` 备份（不直接 DROP）
- **详见**：`docs/bugs/bug-list.md` BUG-004

#### 35. 35_add_notification_user_type.sql - 通知表新增 user_type 字段
- `sys_notification` 和 `sys_notification_read` 新增 `user_type` 字段（portal/sys）
- 调整唯一索引为 `uk_notification_user_type`（notification_id + user_id + user_type）
- **详见**：`docs/bugs/bug-list.md` BUG-005

#### 36. ~~36_init_notification_center_menu.sql~~ - 已废弃删除
- **v4.0 已废弃并删除**：后台通知中心菜单（`/system/notification`）已合并到 `cms/notification`（通知管理）
- **详见**：`docs/bugs/bug-list.md` BUG-008

#### 37. 37_init_dashboard_config.sql - Dashboard 配置初始化
- 创建 Dashboard 首页配置数据

#### 38. 38_init_report_feedback_menu.sql - 举报/反馈菜单及建表
- 创建 `portal_report`（举报）和 `portal_feedback`（反馈）表
- 创建后台管理菜单及按钮权限（cms:report:* / cms:feedback:*）
- **v4.0 修订**：`portal_report.description` 列长从 `varchar(1000)` 改为 `varchar(2000)`，与实体 `@Size(max=2000)` 对齐

#### 39. 39_init_flowable_menu.sql - 流程管理菜单初始化
- 创建流程管理相关菜单

#### 40. 40_fix_bugs_v4.sql - v4.0 综合 Bug 修复脚本
- **1. 友情链接 status 统一**：`ALTER TABLE portal_friend_link MODIFY status varchar(20) DEFAULT '0'`，迁移 `active→0`、`inactive→1`
- **2. 通知菜单清理**：DELETE 残留的通知公告（menu_id=107）及通知中心菜单 + 角色授权
- **3. 评论管理修复**：修正菜单 path 为 `comment`、统一 perms `cms:comment:audit → cms:comment:edit`、补齐菜单及按钮、授权 admin 角色
- **4. PortalReport 列长升级**：`ALTER TABLE portal_report MODIFY COLUMN description varchar(2000)`
- **详见**：`docs/bugs/bug-list.md` BUG-007/008/009/011

### 注意事项

1. **数据库要求**：所有脚本均针对 MySQL 8.0+ 数据库
2. **字符集**：脚本使用 utf8mb4 字符集
3. **执行前准备**：
   - 确保数据库已创建并具有相应权限
   - 建议在执行前备份现有数据
   - 按顺序执行，不要跳跃执行
4. **测试数据密码**：所有测试用户的密码为 `123456`（BCrypt加密）
5. **数据依赖**：
   - 06 脚本依赖 05 创建的分类ID，必须确保先执行 05 再执行 06
   - 23 脚本依赖 19 创建的成长体系表结构
   - 24 脚本依赖 18 创建的面试空间一级菜单、21 创建的精选字段、19 创建的 note_adopted 成长规则
   - 34 脚本依赖 01 中的 sys_notice 表和 03 中的 portal_notification 表做数据迁移
   - 35 脚本依赖 34 创建的 sys_notification / sys_notification_read 表
   - 40 脚本依赖 03（portal_friend_link）、04（cms 菜单）、38（portal_report）的已有结构
6. **NEW 标识**：文件名带 `-new` 后缀的脚本为最新新增脚本，执行后可去掉 `-new` 后缀重命名
7. **已废弃脚本**：36_init_notification_center_menu.sql 已在 v4.0 删除，全新部署无需执行；已有部署需执行 40_fix_bugs_v4.sql 清理其创建的菜单

### 初始化后访问信息

- **后台管理系统地址**：`http://localhost:80`
- **后台管理员账号**：`admin`
- **后台管理员密码**：`admin123`
- **前台地址**：`http://localhost:5173` 或配置的端口
- **前台测试账号**：`admin` / `123456` 或 `zhangsan` / `123456`

### 归档文件

- `现有数据库的表结构.sql` - 旧版数据库导出文件，已废弃，仅供参考
