package com.moyun.ledger.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.moyun.common.exception.system.ServiceException;
import com.moyun.ledger.domain.entity.LedgerProduct;
import com.moyun.ledger.domain.entity.LedgerProductCategory;
import com.moyun.ledger.mapper.LedgerProductCategoryMapper;
import com.moyun.ledger.mapper.LedgerProductMapper;
import com.moyun.ledger.service.ILedgerProductCategoryService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 商品类目服务实现。
 *
 * @author moyun
 */
@Service
public class LedgerProductCategoryServiceImpl implements ILedgerProductCategoryService {

    @Autowired
    private LedgerProductCategoryMapper categoryMapper;

    @Autowired
    private LedgerProductMapper productMapper;

    @Override
    public List<LedgerProductCategory> listTree(Long userId) {
        // 系统预设 + 本人自建，一次取出
        List<LedgerProductCategory> all = categoryMapper.selectList(
                new LambdaQueryWrapper<LedgerProductCategory>()
                        .in(LedgerProductCategory::getUserId, LedgerProductCategory.SYSTEM_USER_ID, userId)
                        .eq(LedgerProductCategory::getStatus, LedgerProductCategory.STATUS_ENABLED)
                        .orderByAsc(LedgerProductCategory::getSortOrder)
                        .orderByAsc(LedgerProductCategory::getId));
        if (all.isEmpty()) {
            return new ArrayList<>();
        }
        // 商品数统计（按 category_id 聚合）
        List<Long> ids = all.stream().map(LedgerProductCategory::getId).collect(Collectors.toList());
        Map<Long, Integer> usedMap = new HashMap<>();
        List<LedgerProduct> products = productMapper.selectList(new LambdaQueryWrapper<LedgerProduct>()
                .in(LedgerProduct::getCategoryId, ids)
                .eq(LedgerProduct::getStatus, LedgerProduct.STATUS_NORMAL)
                .select(LedgerProduct::getId, LedgerProduct::getCategoryId));
        for (LedgerProduct p : products) {
            if (p.getCategoryId() != null) {
                usedMap.merge(p.getCategoryId(), 1, Integer::sum);
            }
        }
        for (LedgerProductCategory c : all) {
            c.setUsedCount(usedMap.getOrDefault(c.getId(), 0));
            c.setSystem(c.getUserId() != null && c.getUserId() == LedgerProductCategory.SYSTEM_USER_ID);
        }
        // 组装：一级挂 children
        Map<Long, List<LedgerProductCategory>> byParent = all.stream()
                .filter(c -> c.getParentId() != null && c.getParentId() != 0)
                .collect(Collectors.groupingBy(LedgerProductCategory::getParentId));
        List<LedgerProductCategory> top = all.stream()
                .filter(c -> c.getParentId() == null || c.getParentId() == 0)
                .collect(Collectors.toList());
        for (LedgerProductCategory c : top) {
            List<LedgerProductCategory> kids = byParent.getOrDefault(c.getId(), new ArrayList<>());
            c.setChildren(kids);
            // 一级的 usedCount 含子类目，便于展示「该类目下有多少商品」
            int sum = c.getUsedCount() == null ? 0 : c.getUsedCount();
            for (LedgerProductCategory k : kids) {
                sum += k.getUsedCount() == null ? 0 : k.getUsedCount();
            }
            c.setUsedCount(sum);
        }
        return top;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long create(Long userId, LedgerProductCategory bo) {
        String name = trim(bo.getName());
        if (name == null) {
            throw new ServiceException("请填写类目名称");
        }
        long parentId = bo.getParentId() == null ? 0L : bo.getParentId();
        if (parentId != 0) {
            LedgerProductCategory parent = categoryMapper.selectById(parentId);
            if (parent == null || parent.getStatus() == null
                    || parent.getStatus() != LedgerProductCategory.STATUS_ENABLED) {
                throw new ServiceException("父类目不存在或已停用");
            }
            if (parent.getParentId() != null && parent.getParentId() != 0) {
                throw new ServiceException("商品类目最多两级");
            }
        }
        assertNameUnique(userId, parentId, name, null);

        LedgerProductCategory c = new LedgerProductCategory();
        c.setUserId(userId);
        c.setName(name);
        c.setParentId(parentId);
        c.setIcon(trim(bo.getIcon()));
        c.setSortOrder(bo.getSortOrder() == null ? 0 : bo.getSortOrder());
        c.setIsSystem(0);
        c.setStatus(LedgerProductCategory.STATUS_ENABLED);
        categoryMapper.insert(c);
        return c.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void update(Long userId, Long id, LedgerProductCategory bo) {
        LedgerProductCategory c = getOwned(userId, id);
        if (bo.getName() != null) {
            String name = trim(bo.getName());
            if (name == null) {
                throw new ServiceException("类目名称不能为空");
            }
            assertNameUnique(c.getUserId(), c.getParentId() == null ? 0L : c.getParentId(), name, id);
            c.setName(name);
        }
        if (bo.getIcon() != null) {
            c.setIcon(trim(bo.getIcon()));
        }
        if (bo.getSortOrder() != null) {
            c.setSortOrder(bo.getSortOrder());
        }
        categoryMapper.updateById(c);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void delete(Long userId, Long id) {
        LedgerProductCategory c = getOwned(userId, id);
        Long kids = categoryMapper.selectCount(new LambdaQueryWrapper<LedgerProductCategory>()
                .eq(LedgerProductCategory::getParentId, id)
                .eq(LedgerProductCategory::getStatus, LedgerProductCategory.STATUS_ENABLED));
        if (kids != null && kids > 0) {
            throw new ServiceException("该类目下还有子类目，请先删除子类目");
        }
        Long used = productMapper.selectCount(new LambdaQueryWrapper<LedgerProduct>()
                .eq(LedgerProduct::getCategoryId, id)
                .eq(LedgerProduct::getStatus, LedgerProduct.STATUS_NORMAL));
        if (used != null && used > 0) {
            throw new ServiceException("该类目下还有 " + used + " 个商品，不能删除");
        }
        c.setStatus(LedgerProductCategory.STATUS_DISABLED);
        categoryMapper.updateById(c);
    }

    // ---------------- 内部 ----------------

    /** 只能改/删本人自建（系统预设只读） */
    private LedgerProductCategory getOwned(Long userId, Long id) {
        LedgerProductCategory c = categoryMapper.selectById(id);
        if (c == null || c.getStatus() == null || c.getStatus() != LedgerProductCategory.STATUS_ENABLED) {
            throw new ServiceException("类目不存在或已停用");
        }
        if (c.getUserId() == null || c.getUserId() == LedgerProductCategory.SYSTEM_USER_ID) {
            throw new ServiceException("系统预设类目不可修改或删除");
        }
        if (!userId.equals(c.getUserId())) {
            throw new ServiceException("无权操作他人类目");
        }
        return c;
    }

    /** 同一归属 + 同一父级下名称唯一 */
    private void assertNameUnique(Long userId, long parentId, String name, Long excludeId) {
        LambdaQueryWrapper<LedgerProductCategory> qw = new LambdaQueryWrapper<LedgerProductCategory>()
                .eq(LedgerProductCategory::getUserId, userId)
                .eq(LedgerProductCategory::getParentId, parentId)
                .eq(LedgerProductCategory::getName, name)
                .eq(LedgerProductCategory::getStatus, LedgerProductCategory.STATUS_ENABLED);
        if (excludeId != null) {
            qw.ne(LedgerProductCategory::getId, excludeId);
        }
        Long cnt = categoryMapper.selectCount(qw);
        if (cnt != null && cnt > 0) {
            throw new ServiceException("该类目已存在");
        }
    }

    private String trim(String s) {
        if (s == null) {
            return null;
        }
        String t = s.trim();
        return t.isEmpty() ? null : t;
    }
}
