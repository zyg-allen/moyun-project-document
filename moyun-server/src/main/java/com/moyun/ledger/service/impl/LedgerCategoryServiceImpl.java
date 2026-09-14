package com.moyun.ledger.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.moyun.ledger.domain.entity.LedgerCategory;
import com.moyun.ledger.domain.entity.LedgerTransaction;
import com.moyun.ledger.mapper.LedgerCategoryMapper;
import com.moyun.ledger.mapper.LedgerTransactionMapper;
import com.moyun.ledger.service.ILedgerCategoryService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 记账分类服务实现
 *
 * @author moyun
 */
@Service
public class LedgerCategoryServiceImpl extends ServiceImpl<LedgerCategoryMapper, LedgerCategory>
        implements ILedgerCategoryService {

    @Autowired
    private LedgerTransactionMapper transactionMapper;

    @Override
    public List<LedgerCategory> listAvailable(Long userId, String type) {
        LambdaQueryWrapper<LedgerCategory> qw = new LambdaQueryWrapper<>();
        qw.and(w -> w.eq(LedgerCategory::getUserId, 0L).or().eq(LedgerCategory::getUserId, userId))
                .eq(LedgerCategory::getStatus, LedgerCategory.STATUS_ENABLED);
        if (type != null && !type.isEmpty()) {
            qw.eq(LedgerCategory::getType, type);
        }
        qw.orderByAsc(LedgerCategory::getSortOrder).orderByAsc(LedgerCategory::getId);
        List<LedgerCategory> list = list(qw);
        fillUsedCount(userId, list);
        return list;
    }

    /** 回填当前用户各分类的使用流水笔数（记一笔页"最常用排序"用） */
    private void fillUsedCount(Long userId, List<LedgerCategory> categories) {
        if (userId == null || categories.isEmpty()) {
            return;
        }
        QueryWrapper<LedgerTransaction> qw = new QueryWrapper<>();
        qw.select("category_id", "COUNT(*) AS cnt")
                .eq("user_id", userId)
                .eq("status", 1)
                .isNotNull("category_id")
                .groupBy("category_id");
        List<Map<String, Object>> rows = transactionMapper.selectMaps(qw);
        Map<Long, Long> countMap = rows.stream().collect(Collectors.toMap(
                r -> Long.valueOf(String.valueOf(r.get("category_id"))),
                r -> Long.valueOf(String.valueOf(r.get("cnt")))
        ));
        categories.forEach(c -> c.setUsedCount(countMap.getOrDefault(c.getId(), 0L)));
    }

    @Override
    public LedgerCategory createCategory(Long userId, LedgerCategory category) {
        category.setId(null);
        category.setUserId(userId);
        category.setIsSystem(0);
        if (category.getStatus() == null) {
            category.setStatus(LedgerCategory.STATUS_ENABLED);
        }
        if (category.getSortOrder() == null) {
            category.setSortOrder(0);
        }
        save(category);
        return category;
    }

    @Override
    public void updateCategory(Long userId, LedgerCategory category) {
        LedgerCategory exist = getOwnedCustom(userId, category.getId());
        if (exist == null) {
            throw new IllegalArgumentException("分类不存在或为系统预设，不可修改");
        }
        category.setUserId(userId);
        category.setIsSystem(0);
        updateById(category);
    }

    @Override
    public void deleteCategory(Long userId, Long categoryId) {
        LedgerCategory exist = getOwnedCustom(userId, categoryId);
        if (exist == null) {
            throw new IllegalArgumentException("分类不存在或为系统预设，不可删除");
        }
        assertCategoryDeletable(categoryId);
        removeById(categoryId);
    }

    @Override
    public void assertCategoryDeletable(Long categoryId) {
        // 已绑定有效流水 → 不能删除（资金链路可追溯红线）
        Long txnCount = transactionMapper.selectCount(new LambdaQueryWrapper<LedgerTransaction>()
                .eq(LedgerTransaction::getCategoryId, categoryId)
                .eq(LedgerTransaction::getStatus, LedgerTransaction.STATUS_NORMAL));
        if (txnCount != null && txnCount > 0) {
            throw new IllegalArgumentException("该分类已绑定 " + txnCount + " 笔流水，不能删除（可将其停用）");
        }
        // 存在子分类 → 不能删除（避免悬挂引用）
        Long childCount = count(new LambdaQueryWrapper<LedgerCategory>()
                .eq(LedgerCategory::getParentId, categoryId));
        if (childCount != null && childCount > 0) {
            throw new IllegalArgumentException("该分类下有子分类，请先处理子分类");
        }
    }

    /** 仅取当前用户自定义分类（排除系统预设） */
    private LedgerCategory getOwnedCustom(Long userId, Long categoryId) {
        if (categoryId == null) {
            return null;
        }
        LambdaQueryWrapper<LedgerCategory> qw = new LambdaQueryWrapper<>();
        qw.eq(LedgerCategory::getId, categoryId)
                .eq(LedgerCategory::getUserId, userId)
                .eq(LedgerCategory::getIsSystem, 0);
        return getOne(qw);
    }
}
