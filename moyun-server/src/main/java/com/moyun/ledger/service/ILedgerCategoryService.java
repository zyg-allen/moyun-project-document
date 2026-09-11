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
     * 停用自定义分类（系统预设不可删）
     */
    void deleteCategory(Long userId, Long categoryId);
}
