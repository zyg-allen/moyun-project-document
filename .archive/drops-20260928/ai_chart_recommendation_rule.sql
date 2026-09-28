
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

/*!40000 ALTER TABLE `ai_chart_recommendation_rule` DISABLE KEYS */;
INSERT INTO `ai_chart_recommendation_rule` (`id`, `rule_name`, `data_pattern`, `field_types`, `data_characteristics`, `recommended_chart`, `priority`, `reason`, `min_data_points`, `max_data_points`, `enabled`, `create_time`) VALUES (1,'时间序列-折线图','time_series',NULL,NULL,'line',95,'时间趋势最适合用折线图展示',2,999999,1,'2025-11-29 14:21:54');
INSERT INTO `ai_chart_recommendation_rule` (`id`, `rule_name`, `data_pattern`, `field_types`, `data_characteristics`, `recommended_chart`, `priority`, `reason`, `min_data_points`, `max_data_points`, `enabled`, `create_time`) VALUES (2,'分类占比-饼图','category',NULL,NULL,'pie',85,'少量分类适合饼图',2,6,1,'2025-11-29 14:21:54');
INSERT INTO `ai_chart_recommendation_rule` (`id`, `rule_name`, `data_pattern`, `field_types`, `data_characteristics`, `recommended_chart`, `priority`, `reason`, `min_data_points`, `max_data_points`, `enabled`, `create_time`) VALUES (3,'分类对比-柱状图','category',NULL,NULL,'bar',90,'多分类对比适合柱状图',3,999999,1,'2025-11-29 14:21:54');
INSERT INTO `ai_chart_recommendation_rule` (`id`, `rule_name`, `data_pattern`, `field_types`, `data_characteristics`, `recommended_chart`, `priority`, `reason`, `min_data_points`, `max_data_points`, `enabled`, `create_time`) VALUES (4,'数值分布-直方图','distribution',NULL,NULL,'histogram',90,'数值分布最适合用直方图',10,999999,1,'2025-11-29 14:21:54');
INSERT INTO `ai_chart_recommendation_rule` (`id`, `rule_name`, `data_pattern`, `field_types`, `data_characteristics`, `recommended_chart`, `priority`, `reason`, `min_data_points`, `max_data_points`, `enabled`, `create_time`) VALUES (5,'排名-条形图','ranking',NULL,NULL,'bar',90,'排名对比适合条形图',3,50,1,'2025-11-29 14:21:54');
INSERT INTO `ai_chart_recommendation_rule` (`id`, `rule_name`, `data_pattern`, `field_types`, `data_characteristics`, `recommended_chart`, `priority`, `reason`, `min_data_points`, `max_data_points`, `enabled`, `create_time`) VALUES (6,'相关性-散点图','correlation',NULL,NULL,'scatter',85,'相关性分析适合散点图',10,999999,1,'2025-11-29 14:21:54');
INSERT INTO `ai_chart_recommendation_rule` (`id`, `rule_name`, `data_pattern`, `field_types`, `data_characteristics`, `recommended_chart`, `priority`, `reason`, `min_data_points`, `max_data_points`, `enabled`, `create_time`) VALUES (7,'多维对比-雷达图','multi_dimension',NULL,NULL,'radar',75,'多维度对比适合雷达图',3,8,1,'2025-11-29 14:21:54');
/*!40000 ALTER TABLE `ai_chart_recommendation_rule` ENABLE KEYS */;
/*!40103 SET TIME_ZONE=@OLD_TIME_ZONE */;

/*!40101 SET SQL_MODE=@OLD_SQL_MODE */;
/*!40014 SET FOREIGN_KEY_CHECKS=@OLD_FOREIGN_KEY_CHECKS */;
/*!40014 SET UNIQUE_CHECKS=@OLD_UNIQUE_CHECKS */;
/*!40101 SET CHARACTER_SET_CLIENT=@OLD_CHARACTER_SET_CLIENT */;
/*!40101 SET CHARACTER_SET_RESULTS=@OLD_CHARACTER_SET_RESULTS */;
/*!40101 SET COLLATION_CONNECTION=@OLD_COLLATION_CONNECTION */;
/*!40111 SET SQL_NOTES=@OLD_SQL_NOTES */;

