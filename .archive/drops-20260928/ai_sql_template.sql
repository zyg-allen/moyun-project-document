
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

/*!40000 ALTER TABLE `ai_sql_template` DISABLE KEYS */;
INSERT INTO `ai_sql_template` (`id`, `template_name`, `natural_query`, `ai_sql_template`, `query_type`, `complexity`, `table_pattern`, `usage_count`, `success_rate`, `enabled`, `create_time`, `update_time`) VALUES (1,'简单查询-TOP N','查询销售额最高的10个产品','SELECT * FROM {table} ORDER BY {metric_field} DESC LIMIT {limit}','ranking','simple',NULL,0,0.00,1,'2025-11-29 14:21:54','2025-11-29 14:21:54');
INSERT INTO `ai_sql_template` (`id`, `template_name`, `natural_query`, `ai_sql_template`, `query_type`, `complexity`, `table_pattern`, `usage_count`, `success_rate`, `enabled`, `create_time`, `update_time`) VALUES (2,'时间范围查询','查询上个月的数据','SELECT * FROM {table} WHERE {time_field} >= DATE_SUB(CURDATE(), INTERVAL 1 MONTH) AND {time_field} < CURDATE()','time_range','simple',NULL,0,0.00,1,'2025-11-29 14:21:54','2025-11-29 14:21:54');
INSERT INTO `ai_sql_template` (`id`, `template_name`, `natural_query`, `ai_sql_template`, `query_type`, `complexity`, `table_pattern`, `usage_count`, `success_rate`, `enabled`, `create_time`, `update_time`) VALUES (3,'聚合统计','统计每个类别的总数','SELECT {category_field}, COUNT(*) as count FROM {table} GROUP BY {category_field}','aggregate','simple',NULL,0,0.00,1,'2025-11-29 14:21:54','2025-11-29 14:21:54');
INSERT INTO `ai_sql_template` (`id`, `template_name`, `natural_query`, `ai_sql_template`, `query_type`, `complexity`, `table_pattern`, `usage_count`, `success_rate`, `enabled`, `create_time`, `update_time`) VALUES (4,'平均值计算','计算平均销售额','SELECT AVG({metric_field}) as avg_value FROM {table}','aggregate','simple',NULL,0,0.00,1,'2025-11-29 14:21:54','2025-11-29 14:21:54');
INSERT INTO `ai_sql_template` (`id`, `template_name`, `natural_query`, `ai_sql_template`, `query_type`, `complexity`, `table_pattern`, `usage_count`, `success_rate`, `enabled`, `create_time`, `update_time`) VALUES (5,'多条件筛选','查询价格大于100且库存小于50的商品','SELECT * FROM {table} WHERE {field1} > {value1} AND {field2} < {value2}','filter','medium',NULL,0,0.00,1,'2025-11-29 14:21:54','2025-11-29 14:21:54');
/*!40000 ALTER TABLE `ai_sql_template` ENABLE KEYS */;
/*!40103 SET TIME_ZONE=@OLD_TIME_ZONE */;

/*!40101 SET SQL_MODE=@OLD_SQL_MODE */;
/*!40014 SET FOREIGN_KEY_CHECKS=@OLD_FOREIGN_KEY_CHECKS */;
/*!40014 SET UNIQUE_CHECKS=@OLD_UNIQUE_CHECKS */;
/*!40101 SET CHARACTER_SET_CLIENT=@OLD_CHARACTER_SET_CLIENT */;
/*!40101 SET CHARACTER_SET_RESULTS=@OLD_CHARACTER_SET_RESULTS */;
/*!40101 SET COLLATION_CONNECTION=@OLD_COLLATION_CONNECTION */;
/*!40111 SET SQL_NOTES=@OLD_SQL_NOTES */;

