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
     * <p>真删除；已绑定有效流水的分类不能删除（IllegalArgumentException）。
     */
    void deleteCategory(Long userId, Long categoryId);

    /**
     * 删除前置校验（门户/后台共用）：
     * <ul>
     *   <li>已绑定有效流水（ledger_transaction.category_id 且 status=1）→ 不能删除</li>
     *   <li>存在子分类（parent_id 引用）→ 不能删除</li>
     * </ul>
     */
    void assertCategoryDeletable(Long categoryId);

    /**
     * 同层级同名校验（幂等）：同一归属 + 同一大类 + 同一父级下不允许重名。
     *
     * <p>网络重试/重复提交时直接抛 IllegalArgumentException，避免静默产生重复分类。</p>
     *
     * @param userId    归属（0=系统预设，>0=用户自定义）
     * @param type      大类（income/expense/...）
     * @param parentId  父分类（0 或 null = 一级分类）
     * @param name      分类名
     * @param excludeId 修改时排除自身（新增传 null）
     */
    void assertNameUnique(Long userId, String type, Long parentId, String name, Long excludeId);

    /**
     * 父分类合法性校验：父必须存在、同大类、且本身是一级分类（最多两级）。
     *
     * @param parentId 父分类ID（null/0 表示一级，直接放行）
     * @param type     子分类所属大类
     */
    void assertParentValid(Long parentId, String type);
}
