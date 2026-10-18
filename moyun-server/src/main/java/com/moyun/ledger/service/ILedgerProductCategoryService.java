package com.moyun.ledger.service;

import com.moyun.ledger.domain.entity.LedgerProductCategory;

import java.util.List;

/**
 * 商品类目服务（两级树）。
 *
 * <p>系统预设（user_id=0）对所有用户可见但只读；
 * 用户自建类目只能由本人改/删，且有商品引用时不能删。</p>
 *
 * @author moyun
 */
public interface ILedgerProductCategoryService {

    /** 类目树：系统预设 + 本人自建，一级带 children */
    List<LedgerProductCategory> listTree(Long userId);

    /** 新增类目（parentId=0 为一级） */
    Long create(Long userId, LedgerProductCategory bo);

    /** 修改类目（仅本人自建） */
    void update(Long userId, Long id, LedgerProductCategory bo);

    /** 删除类目（仅本人自建；有子类目或被商品引用时拒绝） */
    void delete(Long userId, Long id);
}
