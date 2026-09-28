
/*!40101 SET @OLD_CHARACTER_SET_CLIENT=@@CHARACTER_SET_CLIENT */;
/*!40101 SET @OLD_CHARACTER_SET_RESULTS=@@CHARACTER_SET_RESULTS */;
/*!40101 SET @OLD_COLLATION_CONNECTION=@@COLLATION_CONNECTION */;
/*!50503 SET NAMES utf8mb4 */;
/*!40103 SET @OLD_TIME_ZONE=@@TIME_ZONE */;
/*!40103 SET TIME_ZONE='+00:00' */;
/*!40014 SET @OLD_UNIQUE_CHECKS=@@UNIQUE_CHECKS, UNIQUE_CHECKS=0 */;
/*!40014 SET @OLD_FOREIGN_KEY_CHECKS=@@FOREIGN_KEY_CHECKS, FOREIGN_KEY_CHECKS=0 */;
/*!40101 SET @OLD_SQL_MODE=@@SQL_MODE, SQL_MODE='NO_AUTO_VALUE_ON_ZERO' */;
/*!40111 SET @OLD_SQL_NOTES=@@SQL_NOTES, SQL_NOTES=0 */;

/*!40000 ALTER TABLE `portal_task` DISABLE KEYS */;
INSERT INTO `portal_task` (`id`, `code`, `name`, `description`, `task_type`, `reward_points`, `target_count`, `icon`, `status`, `create_by`, `create_time`, `update_by`, `update_time`, `remark`, `del_flag`) VALUES (1,'daily_checkin','每日签到','每天签到一次，保持活跃','daily',10,1,NULL,'active','admin','2026-07-28 16:29:45','','2026-07-28 16:29:45',NULL,'0');
INSERT INTO `portal_task` (`id`, `code`, `name`, `description`, `task_type`, `reward_points`, `target_count`, `icon`, `status`, `create_by`, `create_time`, `update_by`, `update_time`, `remark`, `del_flag`) VALUES (2,'daily_publish','每日发文','每日发布 1 篇文章','daily',20,1,NULL,'active','admin','2026-07-28 16:29:45','','2026-07-28 16:29:45',NULL,'0');
INSERT INTO `portal_task` (`id`, `code`, `name`, `description`, `task_type`, `reward_points`, `target_count`, `icon`, `status`, `create_by`, `create_time`, `update_by`, `update_time`, `remark`, `del_flag`) VALUES (3,'daily_comment','每日互动','每日评论 3 次','daily',15,3,NULL,'active','admin','2026-07-28 16:29:45','','2026-07-28 16:29:45',NULL,'0');
INSERT INTO `portal_task` (`id`, `code`, `name`, `description`, `task_type`, `reward_points`, `target_count`, `icon`, `status`, `create_by`, `create_time`, `update_by`, `update_time`, `remark`, `del_flag`) VALUES (4,'daily_like','每日点赞','每日点赞 5 次','daily',10,5,NULL,'active','admin','2026-07-28 16:29:45','','2026-07-28 16:29:45',NULL,'0');
INSERT INTO `portal_task` (`id`, `code`, `name`, `description`, `task_type`, `reward_points`, `target_count`, `icon`, `status`, `create_by`, `create_time`, `update_by`, `update_time`, `remark`, `del_flag`) VALUES (5,'daily_solve','每日刷题','每日解答 3 道面试题','daily',20,3,NULL,'active','admin','2026-07-28 16:29:45','','2026-07-28 16:29:45',NULL,'0');
INSERT INTO `portal_task` (`id`, `code`, `name`, `description`, `task_type`, `reward_points`, `target_count`, `icon`, `status`, `create_by`, `create_time`, `update_by`, `update_time`, `remark`, `del_flag`) VALUES (6,'first_article','初露锋芒','发布第一篇文章','achievement',50,1,NULL,'active','admin','2026-07-28 16:29:45','','2026-07-28 16:29:45',NULL,'0');
INSERT INTO `portal_task` (`id`, `code`, `name`, `description`, `task_type`, `reward_points`, `target_count`, `icon`, `status`, `create_by`, `create_time`, `update_by`, `update_time`, `remark`, `del_flag`) VALUES (7,'solve_50','刷题能手','累计解答 50 道面试题','achievement',200,50,NULL,'active','admin','2026-07-28 16:29:45','','2026-07-28 16:29:45',NULL,'0');
/*!40000 ALTER TABLE `portal_task` ENABLE KEYS */;
/*!40103 SET TIME_ZONE=@OLD_TIME_ZONE */;

/*!40101 SET SQL_MODE=@OLD_SQL_MODE */;
/*!40014 SET FOREIGN_KEY_CHECKS=@OLD_FOREIGN_KEY_CHECKS */;
/*!40014 SET UNIQUE_CHECKS=@OLD_UNIQUE_CHECKS */;
/*!40101 SET CHARACTER_SET_CLIENT=@OLD_CHARACTER_SET_CLIENT */;
/*!40101 SET CHARACTER_SET_RESULTS=@OLD_CHARACTER_SET_RESULTS */;
/*!40101 SET COLLATION_CONNECTION=@OLD_COLLATION_CONNECTION */;
/*!40111 SET SQL_NOTES=@OLD_SQL_NOTES */;

