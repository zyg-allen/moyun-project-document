package com.moyun.agent.service.impl;

import com.moyun.agent.constant.DataAnalysisConstants;
import com.moyun.agent.service.LLMService;
import com.moyun.agent.service.SQLGeneratorService;
import com.moyun.agent.service.TokenUsageService;
import com.moyun.agent.util.SqlUtils;
import com.moyun.agent.vo.TableSchemaVO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * SQL生成服务实现
 *
 * @author laomao
 */
@Slf4j
@Service
public class SQLGeneratorServiceImpl implements SQLGeneratorService {

    @Autowired
    private LLMService llmService;
    
    @Autowired
    private TokenUsageService tokenUsageService;

    @Override
    public String generateSQL(String naturalQuery, List<TableSchemaVO> schemas, String sessionId) {
        log.info("开始生成SQL, 查询: {}", naturalQuery);

        // 1. 构建表结构上下文
        String schemaContext = buildSchemaContext(schemas);

        // 2. 构建提示词
        String prompt = buildSQLPrompt(naturalQuery, schemaContext);

        // 3. 调用LLM生成SQL
        String response = llmService.generate(prompt);
        log.info("LLM原始响应: {}", response);
        
        // 3.5. 统计Token使用
        try {
            int inputTokens = tokenUsageService.estimateTokens(prompt);
            int outputTokens = tokenUsageService.estimateTokens(response);
            
            // 记录使用情况
            tokenUsageService.recordUsageAsync(
                null, // conversationId - AI数据分析不属于对话
                null, // agentId - 系统功能
                "default", // modelName - 使用默认模型
                "system", // modelProvider
                inputTokens,
                outputTokens,
                "AI数据分析-SQL" // requestType
            );
            
            log.info("SQL生成Token统计: 输入={}, 输出={}, 总计={}", 
                inputTokens, outputTokens, inputTokens + outputTokens);
        } catch (Exception e) {
            log.warn("Token统计失败", e);
        }

        // 4. 清理SQL(去除markdown等格式)
        String sql = SqlUtils.cleanSql(response);
        log.info("清理后的SQL: {}", sql);

        // 5. 添加LIMIT(如果没有)
        sql = SqlUtils.ensureLimit(sql, DataAnalysisConstants.DEFAULT_MAX_ROWS);

        log.info("生成的SQL: {}", sql);
        
        // 6. 验证SQL安全性（提前验证并记录）
        boolean isSafe = SqlUtils.isSafeSql(sql);
        log.info("SQL安全验证结果: {}, SQL: {}", isSafe, sql);
        
        return sql;
    }

    @Override
    public boolean validateSQL(String sql) {
        return SqlUtils.isSafeSql(sql);
    }

    @Override
    public String optimizeSQL(String sql) {
        // 1. 确保有LIMIT
        sql = SqlUtils.ensureLimit(sql, DataAnalysisConstants.DEFAULT_MAX_ROWS);

        // 2. 移除多余的空格
        sql = sql.replaceAll("\\s+", " ").trim();

        return sql;
    }

    @Override
    public String parseQueryType(String sql) {
        return SqlUtils.parseQueryType(sql);
    }

    /**
     * 构建表结构上下文
     */
    private String buildSchemaContext(List<TableSchemaVO> schemas) {
        StringBuilder context = new StringBuilder();
        
        context.append("# 数据库表结构（重点关注字段注释的语义）\n\n");

        for (TableSchemaVO schema : schemas) {
            context.append(String.format("## 表: `%s`", schema.getTableName()));

            if (schema.getTableComment() != null && !schema.getTableComment().isEmpty()) {
                context.append(String.format(" -- %s", schema.getTableComment()));
            }
            context.append("\n\n字段列表:\n");

            // 列信息 - 更详细的格式，突出注释
            for (TableSchemaVO.ColumnSchema column : schema.getColumns()) {
                context.append(String.format("  • `%s` %s", 
                    column.getColumnName(), 
                    column.getDataType()));

                // 主键标记
                if (Boolean.TRUE.equals(column.getPrimaryKey())) {
                    context.append(" [主键]");
                }
                
                // 字段注释是关键 - 用于语义理解
                if (column.getComment() != null && !column.getComment().isEmpty()) {
                    context.append(String.format(" → 注释: %s", column.getComment()));
                } else {
                    context.append(" → (无注释)");
                }
                
                context.append("\n");
            }
            
            if (schema.getRowCount() != null && schema.getRowCount() > 0) {
                context.append(String.format("\n  数据量: %,d 行\n", schema.getRowCount()));
            }
            
            context.append("\n");
        }

        return context.toString();
    }

    /**
     * 构建SQL生成提示词
     */
    private String buildSQLPrompt(String naturalQuery, String schemaContext) {
        return """
            你是一个具有超强语义理解能力的AI SQL专家，精通自然语言到SQL的智能转换。
            
            **核心能力**: 
            1. 通过字段注释的语义理解来识别表间关联关系，而不是死板地匹配字段名
            2. 理解同义词、口语化表达、模糊描述
            3. 智能推断用户真实意图，即使表达不够精确
            
            # 可用的表结构:
            """ + schemaContext + """
            
            # 用户问题:
            """ + naturalQuery + """
            
            # 语义理解增强:
            
            **同义词识别** - 以下词汇含义相同，请智能识别:
            - 数量/总数/个数/条数/多少/几个 → COUNT
            - 总计/合计/总和/累计 → SUM
            - 平均/均值/平均值 → AVG
            - 最大/最高/最多/最贵 → MAX + ORDER BY DESC
            - 最小/最低/最少/最便宜 → MIN + ORDER BY ASC
            - 查询/查找/搜索/检索/查看/显示/列出 → SELECT
            - 用户/客户/会员/人员/账号 → 相关表
            - 订单/工单/单据 → 相关表
            - 商品/产品/货品/物品 → 相关表
            - 价格/金额/费用 → 相关字段
            
            **时间理解**:
            - 今天/当天/本日/今日 → DATE(column) = CURDATE()
            - 昨天/昨日/前一天 → DATE(column) = DATE_SUB(CURDATE(), INTERVAL 1 DAY)
            - 最近N天 → column >= DATE_SUB(NOW(), INTERVAL N DAY)
            - 本周/这周 → WEEK(column) = WEEK(NOW())
            - 本月/这月/这个月 → MONTH(column) = MONTH(NOW())
            
            **模糊匹配**:
            - 如果用户描述不精确，根据语义理解匹配最相关的字段
            - 例如："年龄"可以匹配age、user_age、customer_age等
            - 例如："名字"可以匹配name、username、customer_name等
            
            # 核心规则:
            
            ⚠️ **安全要求（最高优先级）**:
            - 只能生成SELECT查询语句
            - 严禁生成任何INSERT/UPDATE/DELETE/DROP/ALTER/CREATE等修改数据或结构的操作
            - 严禁生成GRANT/REVOKE等权限操作
            - 严禁生成EXEC/CALL等存储过程调用
            - 如果用户要求修改数据，直接拒绝，返回：抱歉，系统仅支持查询操作
            
            1. 只返回可执行的SQL语句,不要任何解释
            2. 使用标准MySQL语法
            
            3. **理解中文描述**:
               - 用户可以使用表的中文注释来描述表名,你需要识别并使用实际的英文表名
                 例如: 用户说"查询用户表",实际表名可能是`users`,注释是"用户表"
               - 用户可以使用字段的中文注释来描述字段,你需要使用实际的英文字段名
                 例如: 用户说"按年龄排序",实际字段名可能是`age`,注释是"年龄"
            
            4. **智能字段关联** (核心能力):
               - **通过字段注释的语义理解来判断字段是否可以关联,而不是死板地看字段名**
               - 关联判断标准:
                 ① 注释语义相同或相关: 
                    * "客户名称" 可以关联 "用户名" (都表示人名)
                    * "订单编号" 可以关联 "订单号" (都表示订单标识)
                    * "产品ID" 可以关联 "商品ID" (都表示商品标识)
                 ② 数据类型兼容:
                    * VARCHAR可以关联VARCHAR
                    * INT可以关联INT/BIGINT
                    * 避免字符串关联数字
               - **字段名不重要,重要的是注释的语义含义**
               - 示例:
                 * `sales_orders`.`customer_name` (VARCHAR, 注释:客户名称) 
                   可以关联 `user_registrations`.`username` (VARCHAR, 注释:用户名)
                   → 因为"客户名称"和"用户名"语义上都表示人的名字
                 * `orders`.`product_id` (INT, 注释:产品ID) 
                   可以关联 `products`.`id` (INT, 注释:商品ID/主键)
                   → 因为都是商品标识
            
            5. **处理复杂问题**:
               - 用户可能一次问多个问题,需要跨表查询
               - 根据注释语义智能识别可关联的字段
               - 如果找到了语义相关的字段,大胆使用JOIN
               - 如果实在找不到合适的关联字段,只回答能回答的部分
            
            6. 对于聚合查询,必须使用GROUP BY
            7. 理解时间表述: "上个月"、"最近7天"、"今年"等
            
            8. **关键规则 - 何时使用LIMIT**:
               ❌ 不要LIMIT的情况（实事求是，全表查询）:
                  - 统计类: "总数"、"多少人"、"多少个"、"多少次"、"出现几次"
                  - 计算类: "平均"、"总和"、"最大值"、"最小值"
                  - 分析类: "分析"、"统计"、"趋势"（除非用户明确说"前X条"）
                  - 任何包含COUNT/SUM/AVG/MAX/MIN的聚合查询
               
               ✅ 需要LIMIT的情况（数据浏览，防止海量数据）:
                  - 浏览数据: "查询数据"、"显示"、"列出"、"看看"
                  - 当用户明确要求前N条: "前10条"、"最近20个"
                  - SELECT * 且没有聚合函数时
            
            9. 排序查询使用ORDER BY
            10. 只使用SELECT查询,不要使用其他操作
            11. 字段名和表名使用反引号包裹
            
            # 示例:
            
            ## 统计类查询（不要LIMIT，实事求是）:
            问题: 统计用户总数
            SQL: SELECT COUNT(*) as total FROM `users`;
            
            问题: 男性有多少人
            SQL: SELECT COUNT(*) as male_count FROM `users` WHERE `gender` = '男';
            
            问题: 张三出现了几次
            SQL: SELECT COUNT(*) as count FROM `users` WHERE `name` = '张三';
            
            问题: 每个部门有多少人
            SQL: SELECT `department`, COUNT(*) as count FROM `users` GROUP BY `department`;
            
            问题: 平均年龄是多少
            SQL: SELECT AVG(`age`) as avg_age FROM `users`;
            
            问题: 分析用户注册趋势
            SQL: SELECT DATE(`created_at`) as date, COUNT(*) as count FROM `users` GROUP BY DATE(`created_at`) ORDER BY date;
            (注意: 分析类全表查询，不要LIMIT)
            
            ## 浏览类查询（需要LIMIT，防止海量数据）:
            问题: 查询用户数据
            SQL: SELECT * FROM `users` LIMIT 1000;
            
            问题: 显示所有订单
            SQL: SELECT * FROM `orders` LIMIT 1000;
            
            问题: 查询销售额最高的10个产品
            SQL: SELECT * FROM `products` ORDER BY `sales` DESC LIMIT 10;
            
            问题: 最近20条注册记录
            SQL: SELECT * FROM `users` ORDER BY `created_at` DESC LIMIT 20;
            
            ## 智能字段关联示例（重要）:
            
            场景1: 跨表统计查询
            问题: 华为手机有多少订单，分别是哪些人买了，这些人的性别分布是什么样的
            
            表结构分析:
            - sales_orders表:
              * `customer_name` VARCHAR -- 客户名称
            - user_registrations表:
              * `username` VARCHAR -- 用户名
              * `gender` VARCHAR -- 性别
            
            语义分析: 
            - "客户名称"和"用户名"语义相近,都表示人的名字
            - 两个字段都是VARCHAR类型,类型兼容
            - 可以通过这两个字段JOIN关联
            
            正确SQL（基于语义智能关联）:
            SELECT 
                COUNT(*) as order_count,
                GROUP_CONCAT(DISTINCT so.customer_name) as buyers,
                ur.gender,
                COUNT(DISTINCT so.customer_name) as gender_count
            FROM `sales_orders` so
            LEFT JOIN `user_registrations` ur ON so.customer_name = ur.username
            WHERE so.product_name LIKE '%华为手机%'
            GROUP BY ur.gender;
            
            场景2: 订单和商品关联
            问题: 查询销量最高的商品详情
            
            表结构:
            - orders表: `product_id` INT -- 产品ID
            - products表: `id` INT [主键] -- 商品ID
            
            语义分析: "产品ID"和"商品ID"都表示商品标识,可以关联
            
            SQL:
            SELECT p.*, COUNT(o.id) as order_count
            FROM `products` p
            LEFT JOIN `orders` o ON p.id = o.product_id
            GROUP BY p.id
            ORDER BY order_count DESC
            LIMIT 10;
            
            ## 高级查询示例:
            
            场景3: 子查询
            问题: 查询订单金额超过平均值的用户
            SQL:
            SELECT * FROM `users` 
            WHERE `id` IN (
                SELECT `user_id` FROM `orders` 
                WHERE `amount` > (SELECT AVG(`amount`) FROM `orders`)
            );
            
            场景4: 窗口函数（每组TOP N）
            问题: 每个部门工资最高的3名员工
            SQL:
            SELECT * FROM (
                SELECT *, 
                ROW_NUMBER() OVER (PARTITION BY `department` ORDER BY `salary` DESC) as rn
                FROM `employees`
            ) t WHERE t.rn <= 3;
            
            场景5: 时间对比分析（环比、同比）
            问题: 对比本月和上月的销售额
            SQL:
            SELECT 
                SUM(CASE WHEN MONTH(`order_date`) = MONTH(NOW()) THEN `amount` ELSE 0 END) as this_month,
                SUM(CASE WHEN MONTH(`order_date`) = MONTH(NOW()) - 1 THEN `amount` ELSE 0 END) as last_month,
                (SUM(CASE WHEN MONTH(`order_date`) = MONTH(NOW()) THEN `amount` ELSE 0 END) - 
                 SUM(CASE WHEN MONTH(`order_date`) = MONTH(NOW()) - 1 THEN `amount` ELSE 0 END)) as diff
            FROM `orders`
            WHERE YEAR(`order_date`) = YEAR(NOW());
            
            问题: 最近7天每天的订单趋势
            SQL:
            SELECT 
                DATE(`order_date`) as date,
                COUNT(*) as order_count,
                SUM(`amount`) as total_amount
            FROM `orders`
            WHERE `order_date` >= DATE_SUB(NOW(), INTERVAL 7 DAY)
            GROUP BY DATE(`order_date`)
            ORDER BY date;
            
            场景6: 复杂聚合
            问题: 统计各地区的订单总额、平均单价和订单数量
            SQL:
            SELECT 
                `region`,
                COUNT(*) as order_count,
                SUM(`amount`) as total_amount,
                AVG(`amount`) as avg_amount,
                MAX(`amount`) as max_amount,
                MIN(`amount`) as min_amount
            FROM `orders`
            GROUP BY `region`
            ORDER BY total_amount DESC;
            
            # 请生成SQL:
            """;
    }

}
