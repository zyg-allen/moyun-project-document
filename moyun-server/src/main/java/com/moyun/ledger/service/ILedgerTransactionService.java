package com.moyun.ledger.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.moyun.ledger.domain.dto.TransactionCreateDTO;
import com.moyun.ledger.domain.entity.LedgerTransaction;
import com.moyun.ledger.domain.query.TransactionQuery;

import java.util.Map;

/**
 * 记账流水服务（核心联动逻辑：创建/修改/删除 均在单事务内完成余额双边更新）
 *
 * @author moyun
 */
public interface ILedgerTransactionService extends IService<LedgerTransaction> {

    /**
     * 新增记账：按类型联动更新资产/负债余额，写入余额快照，并刷新当日净资产快照
     *
     * @param userId 门户用户ID
     * @param dto   记账参数
     * @return 流水ID
     */
    Long createTransaction(Long userId, TransactionCreateDTO dto);

    /**
     * 记账后站内预算提醒（≥80% 即将超支 / ≥100% 已超支，每阈值每自然月一次，Redis 去重）
     *
     * @param userId 门户用户ID
     * @return 提示文案；无需提示返回 null
     */
    String checkBudgetAlert(Long userId);

    /**
     * 修改记账：旧记录反向冲正 → 新记录正向重放 → 更新快照
     *
     * @param userId 门户用户ID
     * @param id    流水ID
     * @param dto   新记账参数
     */
    void updateTransaction(Long userId, Long id, TransactionCreateDTO dto);

    /**
     * 删除记账：旧记录反向冲正 → 逻辑删除（status=0）→ 快照保留在流水行
     *
     * @param userId 门户用户ID
     * @param id    流水ID
     */
    void deleteTransaction(Long userId, Long id);

    /**
     * 流水分页查询（仅 status=1，多条件筛选）
     *
     * @param userId 门户用户ID
     * @param query  查询条件
     * @return 分页结果（records/total）
     */
    Map<String, Object> pageTransactions(Long userId, TransactionQuery query);
}
