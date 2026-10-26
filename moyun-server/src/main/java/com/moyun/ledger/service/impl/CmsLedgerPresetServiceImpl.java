package com.moyun.ledger.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.moyun.common.exception.system.ServiceException;
import com.moyun.ledger.domain.entity.LedgerProduct;
import com.moyun.ledger.domain.entity.LedgerProductCategory;
import com.moyun.ledger.domain.entity.LedgerSaleChannel;
import com.moyun.ledger.domain.entity.LedgerSourceChannel;
import com.moyun.ledger.domain.entity.LedgerStockFlow;
import com.moyun.ledger.domain.vo.CmsChannelVO;
import com.moyun.ledger.mapper.LedgerProductCategoryMapper;
import com.moyun.ledger.mapper.LedgerProductMapper;
import com.moyun.ledger.mapper.LedgerSaleChannelMapper;
import com.moyun.ledger.mapper.LedgerSourceChannelMapper;
import com.moyun.ledger.mapper.LedgerStockFlowMapper;
import com.moyun.ledger.service.ICmsLedgerPresetService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * 记账系统预设维护服务实现（商品类目 / 售卖渠道 / 进货渠道）
 *
 * <p>所有写操作都遵循三条红线：
 * <ol>
 *   <li>只维护系统预设（userId=0）；用户自建属用户数据，后台只读；</li>
 *   <li>被业务数据引用时禁止删除（只能停用），避免历史流水/商品悬挂；</li>
 *   <li>写操作幂等：重名直接拒绝，重复提交不会产生重复行。</li>
 * </ol>
 *
 * @author moyun
 */
@Service
public class CmsLedgerPresetServiceImpl implements ICmsLedgerPresetService {

    private static final int NAME_MAX = 30;

    @Autowired
    private LedgerProductCategoryMapper productCategoryMapper;

    @Autowired
    private LedgerSaleChannelMapper saleChannelMapper;

    @Autowired
    private LedgerSourceChannelMapper sourceChannelMapper;

    @Autowired
    private LedgerProductMapper productMapper;

    @Autowired
    private LedgerStockFlowMapper stockFlowMapper;

    // ==================== 商品类目 ====================

    @Override
    public Page<LedgerProductCategory> pageProductCategory(int pageNum, int pageSize,
                                                           String name, String owner, Integer status) {
        LambdaQueryWrapper<LedgerProductCategory> qw = new LambdaQueryWrapper<>();
        qw.and(w -> w.isNull(LedgerProductCategory::getParentId)
                .or().eq(LedgerProductCategory::getParentId, 0L));
        if (name != null && !name.isBlank()) {
            qw.like(LedgerProductCategory::getName, name.trim());
        }
        if ("system".equals(owner)) {
            qw.eq(LedgerProductCategory::getUserId, 0L);
        } else if ("user".equals(owner)) {
            qw.gt(LedgerProductCategory::getUserId, 0L);
        }
        if (status != null) {
            qw.eq(LedgerProductCategory::getStatus, status);
        }
        qw.orderByAsc(LedgerProductCategory::getSortOrder).orderByAsc(LedgerProductCategory::getId);
        return productCategoryMapper.selectPage(new Page<>(pageNum, pageSize), qw);
    }

    @Override
    public void fillProductCategoryStats(List<LedgerProductCategory> roots) {
        if (roots == null || roots.isEmpty()) {
            return;
        }
        List<Long> rootIds = roots.stream().map(LedgerProductCategory::getId).toList();
        List<LedgerProductCategory> children = productCategoryMapper.selectList(
                new LambdaQueryWrapper<LedgerProductCategory>()
                        .in(LedgerProductCategory::getParentId, rootIds)
                        .orderByAsc(LedgerProductCategory::getSortOrder)
                        .orderByAsc(LedgerProductCategory::getId));
        Map<Long, List<LedgerProductCategory>> byParent = children.stream()
                .collect(Collectors.groupingBy(LedgerProductCategory::getParentId));
        roots.forEach(r -> r.setChildren(byParent.getOrDefault(r.getId(), new ArrayList<>())));

        List<Long> allIds = new ArrayList<>(rootIds);
        children.forEach(c -> allIds.add(c.getId()));
        Map<Long, Integer> direct = countProductsByCategory(allIds);

        children.forEach(c -> c.setUsedCount(direct.getOrDefault(c.getId(), 0)));
        roots.forEach(r -> {
            int total = direct.getOrDefault(r.getId(), 0);
            for (LedgerProductCategory child : r.getChildren()) {
                total += direct.getOrDefault(child.getId(), 0);
            }
            r.setUsedCount(total);
        });
    }

    private Map<Long, Integer> countProductsByCategory(List<Long> categoryIds) {
        Map<Long, Integer> result = new HashMap<>();
        if (categoryIds.isEmpty()) {
            return result;
        }
        List<Map<String, Object>> rows = productMapper.selectMaps(new QueryWrapper<LedgerProduct>()
                .select("category_id", "COUNT(*) AS cnt")
                .eq("status", 1)
                .in("category_id", categoryIds)
                .groupBy("category_id"));
        for (Map<String, Object> row : rows) {
            Object id = row.get("category_id");
            Object cnt = row.get("cnt");
            if (id != null) {
                result.put(Long.valueOf(id.toString()), Integer.parseInt(cnt.toString()));
            }
        }
        return result;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void createProductCategory(LedgerProductCategory bo) {
        String name = normalizeName(bo.getName());
        Long parentId = normalizeParentId(bo.getParentId());
        assertParentValid(parentId, null);
        assertCategoryNameUnique(parentId, name, null);

        LedgerProductCategory entity = new LedgerProductCategory();
        entity.setUserId(0L);
        entity.setName(name);
        entity.setParentId(parentId);
        entity.setIcon(trimToNull(bo.getIcon()));
        entity.setSortOrder(bo.getSortOrder() == null ? 0 : bo.getSortOrder());
        entity.setIsSystem(1);
        entity.setStatus(bo.getStatus() == null ? LedgerProductCategory.STATUS_ENABLED : bo.getStatus());
        productCategoryMapper.insert(entity);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateProductCategory(Long id, LedgerProductCategory bo) {
        LedgerProductCategory exist = requireSystemProductCategory(id);
        String name = normalizeName(bo.getName());
        Long parentId = normalizeParentId(bo.getParentId());
        if (Objects.equals(parentId, id)) {
            throw new ServiceException("父类目不能是自己");
        }
        assertParentValid(parentId, id);
        assertCategoryNameUnique(parentId, name, id);
        boolean hasChildren = productCategoryMapper.selectCount(new LambdaQueryWrapper<LedgerProductCategory>()
                .eq(LedgerProductCategory::getParentId, id)) > 0;
        if (hasChildren && parentId > 0) {
            throw new ServiceException("该类目下已有子类目，不能再挂到其他类目下（最多两级）");
        }
        productCategoryMapper.update(null, new LambdaUpdateWrapper<LedgerProductCategory>()
                .eq(LedgerProductCategory::getId, exist.getId())
                .set(LedgerProductCategory::getName, name)
                .set(LedgerProductCategory::getParentId, parentId)
                .set(LedgerProductCategory::getIcon, trimToNull(bo.getIcon()))
                .set(LedgerProductCategory::getSortOrder, bo.getSortOrder() == null ? 0 : bo.getSortOrder())
                .set(LedgerProductCategory::getStatus,
                        bo.getStatus() == null ? exist.getStatus() : bo.getStatus()));
    }

    @Override
    public void changeProductCategoryStatus(Long id, Integer status) {
        LedgerProductCategory exist = requireSystemProductCategory(id);
        productCategoryMapper.update(null, new LambdaUpdateWrapper<LedgerProductCategory>()
                .eq(LedgerProductCategory::getId, exist.getId())
                .set(LedgerProductCategory::getStatus,
                        status != null && status == LedgerProductCategory.STATUS_ENABLED
                                ? LedgerProductCategory.STATUS_ENABLED
                                : LedgerProductCategory.STATUS_DISABLED));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteProductCategory(Long id) {
        LedgerProductCategory exist = requireSystemProductCategory(id);
        Long children = productCategoryMapper.selectCount(new LambdaQueryWrapper<LedgerProductCategory>()
                .eq(LedgerProductCategory::getParentId, id));
        if (children != null && children > 0) {
            throw new ServiceException("该类目下有 " + children + " 个子类目，请先处理子类目");
        }
        Long used = productMapper.selectCount(new LambdaQueryWrapper<LedgerProduct>()
                .eq(LedgerProduct::getCategoryId, id)
                .eq(LedgerProduct::getStatus, 1));
        if (used != null && used > 0) {
            throw new ServiceException("该类目已被 " + used + " 个商品引用，不能删除（可改为停用）");
        }
        productCategoryMapper.deleteById(exist.getId());
    }

    private LedgerProductCategory requireSystemProductCategory(Long id) {
        if (id == null) {
            throw new ServiceException("类目不存在");
        }
        LedgerProductCategory exist = productCategoryMapper.selectById(id);
        if (exist == null) {
            throw new ServiceException("类目不存在");
        }
        if (exist.getUserId() != null && exist.getUserId() > 0) {
            throw new ServiceException("用户自建类目不归后台维护，只读");
        }
        return exist;
    }

    private void assertParentValid(Long parentId, Long selfId) {
        if (parentId == null || parentId == 0L) {
            return;
        }
        LedgerProductCategory parent = productCategoryMapper.selectById(parentId);
        if (parent == null) {
            throw new ServiceException("父类目不存在");
        }
        if (parent.getUserId() != null && parent.getUserId() > 0) {
            throw new ServiceException("不能挂在用户自建类目下");
        }
        if (parent.getParentId() != null && parent.getParentId() > 0) {
            throw new ServiceException("最多两级：不能在二级类目下继续新建");
        }
        if (Objects.equals(parentId, selfId)) {
            throw new ServiceException("父类目不能是自己");
        }
    }

    private void assertCategoryNameUnique(Long parentId, String name, Long excludeId) {
        LambdaQueryWrapper<LedgerProductCategory> qw = new LambdaQueryWrapper<LedgerProductCategory>()
                .eq(LedgerProductCategory::getUserId, 0L)
                .eq(LedgerProductCategory::getParentId, parentId)
                .eq(LedgerProductCategory::getName, name);
        if (excludeId != null) {
            qw.ne(LedgerProductCategory::getId, excludeId);
        }
        if (productCategoryMapper.selectCount(qw) > 0) {
            throw new ServiceException("同层级下已存在「" + name + "」类目");
        }
    }

    // ==================== 渠道 ====================

    @Override
    public Page<CmsChannelVO> pageChannel(String kind, int pageNum, int pageSize,
                                          String name, String owner, Integer status) {
        String k = normalizeKind(kind);
        Page<CmsChannelVO> result = new Page<>(pageNum, pageSize);
        if (CmsChannelVO.KIND_SALE.equals(k)) {
            LambdaQueryWrapper<LedgerSaleChannel> qw = saleWrapper(name, owner, status);
            Page<LedgerSaleChannel> page = saleChannelMapper.selectPage(new Page<>(pageNum, pageSize), qw);
            List<CmsChannelVO> rows = page.getRecords().stream().map(this::toVO).toList();
            fillSaleUsedCount(rows);
            result.setRecords(rows);
            result.setTotal(page.getTotal());
        } else {
            LambdaQueryWrapper<LedgerSourceChannel> qw = sourceWrapper(name, owner, status);
            Page<LedgerSourceChannel> page = sourceChannelMapper.selectPage(new Page<>(pageNum, pageSize), qw);
            List<CmsChannelVO> rows = page.getRecords().stream().map(this::toVO).toList();
            fillSourceUsedCount(rows);
            result.setRecords(rows);
            result.setTotal(page.getTotal());
        }
        return result;
    }

    private LambdaQueryWrapper<LedgerSaleChannel> saleWrapper(String name, String owner, Integer status) {
        LambdaQueryWrapper<LedgerSaleChannel> qw = new LambdaQueryWrapper<>();
        if (name != null && !name.isBlank()) {
            qw.like(LedgerSaleChannel::getName, name.trim());
        }
        if ("system".equals(owner)) {
            qw.eq(LedgerSaleChannel::getUserId, 0L);
        } else if ("user".equals(owner)) {
            qw.gt(LedgerSaleChannel::getUserId, 0L);
        }
        if (status != null) {
            qw.eq(LedgerSaleChannel::getStatus, status);
        }
        qw.orderByAsc(LedgerSaleChannel::getSortOrder).orderByAsc(LedgerSaleChannel::getId);
        return qw;
    }

    private LambdaQueryWrapper<LedgerSourceChannel> sourceWrapper(String name, String owner, Integer status) {
        LambdaQueryWrapper<LedgerSourceChannel> qw = new LambdaQueryWrapper<>();
        if (name != null && !name.isBlank()) {
            qw.like(LedgerSourceChannel::getName, name.trim());
        }
        if ("system".equals(owner)) {
            qw.eq(LedgerSourceChannel::getUserId, 0L);
        } else if ("user".equals(owner)) {
            qw.gt(LedgerSourceChannel::getUserId, 0L);
        }
        if (status != null) {
            qw.eq(LedgerSourceChannel::getStatus, status);
        }
        qw.orderByAsc(LedgerSourceChannel::getSortOrder).orderByAsc(LedgerSourceChannel::getId);
        return qw;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void createChannel(String kind, CmsChannelVO bo) {
        String k = normalizeKind(kind);
        String name = normalizeName(bo.getName());
        assertChannelNameUnique(k, name, null);
        int sort = bo.getSortOrder() == null ? 0 : bo.getSortOrder();
        int status = bo.getStatus() == null ? 1 : bo.getStatus();
        if (CmsChannelVO.KIND_SALE.equals(k)) {
            LedgerSaleChannel entity = new LedgerSaleChannel();
            entity.setUserId(0L);
            entity.setName(name);
            entity.setIcon(trimToNull(bo.getIcon()));
            entity.setSortOrder(sort);
            entity.setIsSystem(1);
            entity.setStatus(status);
            saleChannelMapper.insert(entity);
        } else {
            LedgerSourceChannel entity = new LedgerSourceChannel();
            entity.setUserId(0L);
            entity.setName(name);
            entity.setIcon(trimToNull(bo.getIcon()));
            entity.setSortOrder(sort);
            entity.setIsSystem(1);
            entity.setStatus(status);
            sourceChannelMapper.insert(entity);
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateChannel(String kind, Long id, CmsChannelVO bo) {
        String k = normalizeKind(kind);
        String name = normalizeName(bo.getName());
        int sort = bo.getSortOrder() == null ? 0 : bo.getSortOrder();
        Integer status = bo.getStatus();
        if (CmsChannelVO.KIND_SALE.equals(k)) {
            LedgerSaleChannel exist = requireSystemSaleChannel(id);
            assertChannelNameUnique(k, name, id);
            saleChannelMapper.update(null, new LambdaUpdateWrapper<LedgerSaleChannel>()
                    .eq(LedgerSaleChannel::getId, exist.getId())
                    .set(LedgerSaleChannel::getName, name)
                    .set(LedgerSaleChannel::getIcon, trimToNull(bo.getIcon()))
                    .set(LedgerSaleChannel::getSortOrder, sort)
                    .set(LedgerSaleChannel::getStatus, status == null ? exist.getStatus() : status));
        } else {
            LedgerSourceChannel exist = requireSystemSourceChannel(id);
            assertChannelNameUnique(k, name, id);
            sourceChannelMapper.update(null, new LambdaUpdateWrapper<LedgerSourceChannel>()
                    .eq(LedgerSourceChannel::getId, exist.getId())
                    .set(LedgerSourceChannel::getName, name)
                    .set(LedgerSourceChannel::getIcon, trimToNull(bo.getIcon()))
                    .set(LedgerSourceChannel::getSortOrder, sort)
                    .set(LedgerSourceChannel::getStatus, status == null ? exist.getStatus() : status));
        }
    }

    @Override
    public void changeChannelStatus(String kind, Long id, Integer status) {
        String k = normalizeKind(kind);
        int next = status != null && status == 1 ? 1 : 0;
        if (CmsChannelVO.KIND_SALE.equals(k)) {
            LedgerSaleChannel exist = requireSystemSaleChannel(id);
            saleChannelMapper.update(null, new LambdaUpdateWrapper<LedgerSaleChannel>()
                    .eq(LedgerSaleChannel::getId, exist.getId())
                    .set(LedgerSaleChannel::getStatus, next));
        } else {
            LedgerSourceChannel exist = requireSystemSourceChannel(id);
            sourceChannelMapper.update(null, new LambdaUpdateWrapper<LedgerSourceChannel>()
                    .eq(LedgerSourceChannel::getId, exist.getId())
                    .set(LedgerSourceChannel::getStatus, next));
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteChannel(String kind, Long id) {
        String k = normalizeKind(kind);
        if (CmsChannelVO.KIND_SALE.equals(k)) {
            LedgerSaleChannel exist = requireSystemSaleChannel(id);
            Long used = stockFlowMapper.selectCount(new LambdaQueryWrapper<LedgerStockFlow>()
                    .eq(LedgerStockFlow::getChannelId, id)
                    .eq(LedgerStockFlow::getStatus, 1));
            if (used != null && used > 0) {
                throw new ServiceException("该渠道已被 " + used + " 笔出库流水引用，不能删除（可改为停用）");
            }
            saleChannelMapper.deleteById(exist.getId());
        } else {
            LedgerSourceChannel exist = requireSystemSourceChannel(id);
            Long used = productMapper.selectCount(new LambdaQueryWrapper<LedgerProduct>()
                    .eq(LedgerProduct::getSourceChannel, exist.getName())
                    .eq(LedgerProduct::getStatus, 1));
            if (used != null && used > 0) {
                throw new ServiceException("该渠道已被 " + used + " 个商品引用，不能删除（可改为停用）");
            }
            sourceChannelMapper.deleteById(exist.getId());
        }
    }

    private void fillSaleUsedCount(List<CmsChannelVO> rows) {
        if (rows.isEmpty()) {
            return;
        }
        List<Long> ids = rows.stream().map(CmsChannelVO::getId).toList();
        List<Map<String, Object>> counts = stockFlowMapper.selectMaps(new QueryWrapper<LedgerStockFlow>()
                .select("channel_id", "COUNT(*) AS cnt")
                .eq("status", 1)
                .in("channel_id", ids)
                .groupBy("channel_id"));
        Map<Long, Integer> map = new HashMap<>();
        for (Map<String, Object> row : counts) {
            if (row.get("channel_id") != null) {
                map.put(Long.valueOf(row.get("channel_id").toString()),
                        Integer.parseInt(row.get("cnt").toString()));
            }
        }
        rows.forEach(r -> r.setUsedCount(map.getOrDefault(r.getId(), 0)));
    }

    private void fillSourceUsedCount(List<CmsChannelVO> rows) {
        if (rows.isEmpty()) {
            return;
        }
        List<String> names = rows.stream().map(CmsChannelVO::getName).filter(Objects::nonNull).toList();
        if (names.isEmpty()) {
            return;
        }
        List<Map<String, Object>> counts = productMapper.selectMaps(new QueryWrapper<LedgerProduct>()
                .select("source_channel", "COUNT(*) AS cnt")
                .eq("status", 1)
                .in("source_channel", names)
                .groupBy("source_channel"));
        Map<String, Integer> map = new HashMap<>();
        for (Map<String, Object> row : counts) {
            if (row.get("source_channel") != null) {
                map.put(row.get("source_channel").toString(), Integer.parseInt(row.get("cnt").toString()));
            }
        }
        rows.forEach(r -> r.setUsedCount(map.getOrDefault(r.getName(), 0)));
    }

    private CmsChannelVO toVO(LedgerSaleChannel entity) {
        CmsChannelVO vo = new CmsChannelVO();
        vo.setId(entity.getId());
        vo.setUserId(entity.getUserId());
        vo.setName(entity.getName());
        vo.setIcon(entity.getIcon());
        vo.setSortOrder(entity.getSortOrder());
        vo.setIsSystem(entity.getIsSystem());
        vo.setStatus(entity.getStatus());
        return vo;
    }

    private CmsChannelVO toVO(LedgerSourceChannel entity) {
        CmsChannelVO vo = new CmsChannelVO();
        vo.setId(entity.getId());
        vo.setUserId(entity.getUserId());
        vo.setName(entity.getName());
        vo.setIcon(entity.getIcon());
        vo.setSortOrder(entity.getSortOrder());
        vo.setIsSystem(entity.getIsSystem());
        vo.setStatus(entity.getStatus());
        return vo;
    }

    private LedgerSaleChannel requireSystemSaleChannel(Long id) {
        LedgerSaleChannel exist = id == null ? null : saleChannelMapper.selectById(id);
        if (exist == null) {
            throw new ServiceException("渠道不存在");
        }
        if (exist.getUserId() != null && exist.getUserId() > 0) {
            throw new ServiceException("用户自建渠道不归后台维护，只读");
        }
        return exist;
    }

    private LedgerSourceChannel requireSystemSourceChannel(Long id) {
        LedgerSourceChannel exist = id == null ? null : sourceChannelMapper.selectById(id);
        if (exist == null) {
            throw new ServiceException("渠道不存在");
        }
        if (exist.getUserId() != null && exist.getUserId() > 0) {
            throw new ServiceException("用户自建渠道不归后台维护，只读");
        }
        return exist;
    }

    private void assertChannelNameUnique(String kind, String name, Long excludeId) {
        if (CmsChannelVO.KIND_SALE.equals(kind)) {
            LambdaQueryWrapper<LedgerSaleChannel> qw = new LambdaQueryWrapper<LedgerSaleChannel>()
                    .eq(LedgerSaleChannel::getUserId, 0L)
                    .eq(LedgerSaleChannel::getName, name);
            if (excludeId != null) {
                qw.ne(LedgerSaleChannel::getId, excludeId);
            }
            if (saleChannelMapper.selectCount(qw) > 0) {
                throw new ServiceException("系统预设中已存在渠道「" + name + "」");
            }
        } else {
            LambdaQueryWrapper<LedgerSourceChannel> qw = new LambdaQueryWrapper<LedgerSourceChannel>()
                    .eq(LedgerSourceChannel::getUserId, 0L)
                    .eq(LedgerSourceChannel::getName, name);
            if (excludeId != null) {
                qw.ne(LedgerSourceChannel::getId, excludeId);
            }
            if (sourceChannelMapper.selectCount(qw) > 0) {
                throw new ServiceException("系统预设中已存在渠道「" + name + "」");
            }
        }
    }

    private String normalizeKind(String kind) {
        if (CmsChannelVO.KIND_SALE.equals(kind)) {
            return CmsChannelVO.KIND_SALE;
        }
        if (CmsChannelVO.KIND_SOURCE.equals(kind)) {
            return CmsChannelVO.KIND_SOURCE;
        }
        throw new ServiceException("渠道类型不合法（sale/source）");
    }

    private String normalizeName(String name) {
        String n = name == null ? "" : name.trim();
        if (n.isEmpty()) {
            throw new ServiceException("名称不能为空");
        }
        if (n.length() > NAME_MAX) {
            throw new ServiceException("名称不能超过 " + NAME_MAX + " 个字");
        }
        return n;
    }

    private Long normalizeParentId(Long parentId) {
        return parentId == null || parentId == 0L ? 0L : parentId;
    }

    private String trimToNull(String text) {
        if (text == null) {
            return null;
        }
        String t = text.trim();
        return t.isEmpty() ? null : t;
    }
}
