package com.moyun.ledger.mapper;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * 记账用户维度查询（后台专用）
 *
 * <p>后台「用户数据」列表需要覆盖"只用库存/只建账户/只做预算"的用户，
 * 因此以记账域各业务表的 user_id 并集为准，而不是只看流水表。</p>
 *
 * <p>与门户端接口完全独立：本 Mapper 只给管理端使用，避免与 App 查询口径交叉。</p>
 *
 * @author moyun
 */
@Mapper
public interface CmsLedgerUserQueryMapper {

    /**
     * 记账域出现过的全部用户 id（去重升序）
     *
     * @return 用户 id 列表
     */
    @Select("""
            SELECT DISTINCT user_id FROM (
                SELECT user_id FROM ledger_transaction
                UNION SELECT user_id FROM ledger_asset_account
                UNION SELECT user_id FROM ledger_liability_account
                UNION SELECT user_id FROM ledger_budget
                UNION SELECT user_id FROM ledger_saving_plan
                UNION SELECT user_id FROM ledger_schedule_task
                UNION SELECT user_id FROM ledger_memo
                UNION SELECT user_id FROM ledger_warehouse
                UNION SELECT user_id FROM ledger_product
            ) t WHERE user_id IS NOT NULL ORDER BY user_id
            """)
    List<Long> selectLedgerUserIds();
}
