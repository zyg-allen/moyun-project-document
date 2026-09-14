package com.moyun.ledger.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.moyun.ledger.domain.entity.LedgerCategory;

import java.util.List;

/**
 * 记账分类服务（系统预设 + 用户自定义合并返回）
 *
 * @author moyun
 */
public interface ILedgerCategoryService extends IService<LedgerCategory> {

    /**
     * 用户可用分类列表：系统预设（user_id=0 启用）+ 自定义（user_id=当前用户）
     *
     * @param userId 门户用户ID
     * @param type   类型筛选：income/expense（null=全部）
     */
    List<LedgerCategory> listAvailable(Long userId, String type);

    /**
     * 新增自定义分类
     */
    LedgerCategory createCategory(Long userId, LedgerCategory category);

    /**
     * 修改自定义分类（系统预设不可改）
     */
    void updateCategory(Long userId, LedgerCategory category);

    /**
     * 删除自定义分类（系统预设不可删）
     *
     * <p>v11.76：真删除；已绑定有效流水的分类不能删除（IllegalArgumentException）。
     */
    void deleteCategory(Long userId, Long categoryId);

    /**
     * 删除前置校验（v11.76，门户/后台共用）：
     * <ul>
     *   <li>已绑定有效流水（ledger_transaction.category_id 且 status=1）→ 不能删除</li>
     *   <li>存在子分类（parent_id 引用）→ 不能删除</li>
     * </ul>
     */
    void assertCategoryDeletable(Long categoryId);
}
