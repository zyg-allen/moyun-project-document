package com.moyun.ledger.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.moyun.ledger.domain.entity.LedgerAssetAccount;

import java.util.List;

/**
 * 资产账户服务
 *
 * @author moyun
 */
public interface ILedgerAssetAccountService extends IService<LedgerAssetAccount> {

    /**
     * 新增资产账户（初始余额自动生成 adjust 校准流水，保证全明细追溯）
     */
    LedgerAssetAccount createAccount(Long userId, LedgerAssetAccount account, Long initialBalance);

    /**
     * 修改资产账户（不含 balance，余额校准必须走 adjust 记账）
     */
    void updateAccount(Long userId, LedgerAssetAccount account);

    /**
     * 删除资产账户（status=0 停用归档，流水永久保留）
     */
    void deleteAccount(Long userId, Long accountId);

    /**
     * 用户资产账户列表（默认启用中；含归档可选）
     */
    List<LedgerAssetAccount> listByUser(Long userId, boolean includeArchived);
}
