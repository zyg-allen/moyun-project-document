package com.moyun.ledger.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.moyun.ledger.domain.entity.LedgerAssetAccount;

import java.math.BigDecimal;
import java.util.List;
import java.util.Set;

/**
 * 资产账户服务
 *
 * @author moyun
 */
public interface ILedgerAssetAccountService extends IService<LedgerAssetAccount> {

    /**
     * 新增资产账户（初始余额自动生成 adjust 校准流水，保证全明细追溯）
     */
    LedgerAssetAccount createAccount(Long userId, LedgerAssetAccount account, BigDecimal initialBalance);

    /**
     * 修改资产账户（不含 balance，余额校准必须走 adjust 记账）
     *
     * <p>等价于 {@code updateAccount(userId, account, Set.of())}：只更新传入的非 null 字段，
     * <b>不清空</b>任何可空列（服务端内部调用用这个重载）。</p>
     */
    void updateAccount(Long userId, LedgerAssetAccount account);

    /**
     * 修改资产账户（列级更新；可空列支持"显式清空"）
     *
     * <p>{@code providedFields} 为请求体中**显式出现**的字段名（通常为 {@code body.keySet()}），
     * 用于区分"未传该字段"与"传了 null"：对**可空业务列**（如 {@code valuation}/{@code icon}），
     * 显式提供即以传入值为准（{@code null} = 清空）；未提供则保持原值。
     * NOT NULL 列（name/type/includeInTotal/hideBalance/sortOrder）始终"非 null 才更新"。
     * {@code balance}/{@code version}/{@code user_id}/{@code status} 永不在此接口写入。</p>
     */
    void updateAccount(Long userId, LedgerAssetAccount account, Set<String> providedFields);

    /**
     * 删除资产账户（status=0 停用归档，流水永久保留）
     */
    void deleteAccount(Long userId, Long accountId);

    /**
     * 用户资产账户列表（默认启用中；含归档可选）
     */
    List<LedgerAssetAccount> listByUser(Long userId, boolean includeArchived);
}
