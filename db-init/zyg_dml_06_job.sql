-- ==============================================================================
-- 墨云项目 · 数据库初始化 DML：定时任务（sys_job）
-- 覆盖范围：sys_job
-- 前置条件：已按顺序执行 zyg_ddl_01_sys_ai.sql、zyg_ddl_02_biz_job.sql
-- 幂等性：全部使用 REPLACE INTO（按主键 / 唯一键覆盖），可反复执行；
--         重复执行会以本脚本为准覆盖同主键行，手工改动过的预置行会被重置。
-- 字符集：文件为 UTF-8 无 BOM，请用 mysql --default-character-set=utf8mb4 导入
-- ==============================================================================
SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;
SET SQL_MODE = 'NO_AUTO_VALUE_ON_ZERO';

-- ############################################################################
-- # 一、后台定时任务 sys_job（invoke_target 直接决定任务能否执行，务必与本脚本一致）
-- ############################################################################

-- ----------------------------------------------------------------------------
-- sys_job · 定时任务调度表（12 行）
-- 12 个任务；Quartz 的 qrtz_* 由应用启动时按本表同步重建，不纳入初始化数据
-- ----------------------------------------------------------------------------
REPLACE INTO `sys_job` (`job_id`, `job_name`, `job_group`, `invoke_target`, `cron_expression`, `misfire_policy`, `concurrent`, `status`, `create_by`, `create_time`, `update_by`, `update_time`, `remark`, `del_flag`) VALUES (1,'系统默认（无参）','DEFAULT','ryTask.ryNoParams','0/10 * * * * ?','3','1','1','admin','2026-07-28 15:42:36','',NULL,'','0'),(2,'系统默认（有参）','DEFAULT','ryTask.ryParams(\'ry\')','0/15 * * * * ?','3','1','1','admin','2026-07-28 15:42:36','',NULL,'','0'),(3,'系统默认（多参）','DEFAULT','ryTask.ryMultipleParams(\'ry\', true, 2000, 316.50, 100)','0/20 * * * * ?','3','1','1','admin','2026-07-28 15:42:36','',NULL,'','0'),(4,'敏感词扫描-话题','DEFAULT','sensitiveScanTask.scanTopics()','0 0 3 * * ?','3','1','0','admin','2026-08-19 18:01:41','',NULL,'定时扫描话题内容，命中则转待审核','0'),(5,'敏感词扫描-观点','DEFAULT','sensitiveScanTask.scanTopicPosts()','0 5 3 * * ?','3','1','0','admin','2026-08-19 18:01:41','',NULL,'定时扫描话题观点，命中则标记','0'),(6,'缓存清理-表结构','DEFAULT','cacheCleanupTask.cleanupExpiredCache()','0 */5 * * * ?','3','1','0','admin','2026-08-19 18:01:42','',NULL,'每5分钟清理 DataSourceService 的过期表结构缓存（原 CacheCleanupTask @Scheduled，迁移至 Quartz 统一调度）','0'),(7,'缓存清理-限流器','DEFAULT','cacheCleanupTask.cleanupRateLimiter()','0 */2 * * * ?','3','1','0','admin','2026-08-19 18:01:42','',NULL,'每2分钟清理 RateLimiter 过期数据（原 CacheCleanupTask @Scheduled，迁移至 Quartz 统一调度）','0'),(8,'会话清理-数据分析','DEFAULT','dataAnalysisConversationServiceImpl.cleanExpiredSessions()','0 */30 * * * ?','3','1','0','admin','2026-08-19 18:01:42','',NULL,'每30分钟清理超过1小时未访问的数据分析对话会话（原 DataAnalysisConversationServiceImpl @Scheduled，迁移至 Quartz 统一调度）','0'),(9,'日志落盘-Token使用','DEFAULT','tokenUsageServiceImpl.flushLogsToDB()','0 * * * * ?','1','1','0','admin','2026-08-19 18:01:42','',NULL,'每1分钟将 Redis 中的 Token 使用日志批量写入 DB（原 TokenUsageServiceImpl @Scheduled，迁移至 Quartz 统一调度；misfire=1 立即补偿避免日志丢失）','0'),(10,'敏感词扫描-文章评论','DEFAULT','sensitiveScanTask.scanArticleComments()','0 10 3 * * ?','3','1','0','admin','2026-08-19 18:01:45','',NULL,'扫描已发布文章评论(portal_comment.status=1)，命中敏感词转驳回(status=2)并通知作者','0'),(11,'敏感词扫描-面经评论','DEFAULT','sensitiveScanTask.scanInterviewComments()','0 15 3 * * ?','3','1','0','admin','2026-08-19 18:01:45','',NULL,'扫描已发布面经评论(portal_interview_comment.status=published)，命中敏感词转rejected并通知作者','0'),(12,'AI每日写作Prompt生成','DEFAULT','writingPromptTask.generateDailyPrompt()','0 10 0 * * ?','3','1','0','admin','2026-08-25 00:00:00','',NULL,'每天00:10为今天+明天生成写作提示（结合节日节气，AI失败回退内置主题池）','0');


-- ==============================================================================
-- 定时任务（sys_job） 结束
-- ==============================================================================