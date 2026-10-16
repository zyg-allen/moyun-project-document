package com.moyun.system.service.impl;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.moyun.common.constant.CacheConstants;
import com.moyun.core.base.entity.SysDictData;
import com.moyun.core.base.entity.SysDictType;
import com.moyun.core.config.redis.RedisCache;
import com.moyun.core.mvc.handler.BusinessException;
import com.moyun.system.mapper.SysDictDataMapper;
import com.moyun.system.mapper.SysDictTypeMapper;
import com.moyun.system.service.ISysDictTypeService;
import com.moyun.util.cache.DictUtils;
import com.moyun.util.string.StringUtils;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 字典 业务层处理
 *
 * @author allen-zyg
 */
@Service
public class SysDictTypeServiceImpl implements ISysDictTypeService {

    @Autowired
    private SysDictTypeMapper dictTypeMapper;

    @Autowired
    private SysDictDataMapper dictDataMapper;

    @Autowired
    private RedisCache redisCache;

    /**
     * 项目启动时，初始化字典到缓存
     */
    @PostConstruct
    public void init() {
        loadingDictCache();
    }

    /**
     * 根据条件分页查询字典类型（MyBatis-Plus 分页）
     *
     * @param page      分页对象
     * @param dictType  字典类型信息
     * @return 字典类型分页数据
     */
    @Override
    public IPage<SysDictType> selectDictTypePage(IPage<SysDictType> page, SysDictType dictType) {
        return dictTypeMapper.selectDictTypePage(page, dictType);
    }

    /**
     * 根据条件分页查询字典类型
     *
     * @param dictType 字典类型信息
     * @return 字典类型集合信息
     */
    @Override
    public List<SysDictType> selectDictTypeList(SysDictType dictType) {
        return dictTypeMapper.selectDictTypeList(dictType);
    }

    /**
     * 根据所有字典类型
     *
     * @return 字典类型集合信息
     */
    @Override
    public List<SysDictType> selectDictTypeAll() {
        return dictTypeMapper.selectDictTypeAll();
    }

    /**
     * 根据字典类型查询字典数据
     *
     * @param dictType 字典类型
     * @return 字典数据集合信息
     */
    @Override
    public List<SysDictData> selectDictDataByType(String dictType) {
        List<SysDictData> dictDatas = DictUtils.getDictCache(dictType);
        if (StringUtils.isNotEmpty(dictDatas)) {
            return dictDatas;
        }
        dictDatas = dictDataMapper.selectDictDataByType(dictType);
        DictUtils.setDictCache(dictType, dictDatas);
        return dictDatas;
    }

    /**
     * 根据字典类型查询树形字典数据（公共查询，带缓存，仅启用节点）
     */
    @Override
    public List<SysDictData> selectDictTreeByType(String dictType) {
        List<SysDictData> tree = DictUtils.getDictTreeCache(dictType);
        if (StringUtils.isNotNull(tree)) {
            return tree;
        }
        List<SysDictData> flatList = selectDictDataByType(dictType);
        tree = buildDictTree(flatList);
        DictUtils.setDictTreeCache(dictType, tree);
        return tree;
    }

    /**
     * 将平铺字典数据按 parentDictId 组装成树。
     * <p>parentDictId 为空或 0 视为一级；父节点缺失的孤儿节点提升为一级，避免数据丢失。</p>
     */
    private List<SysDictData> buildDictTree(List<SysDictData> flatList) {
        List<SysDictData> roots = new ArrayList<>();
        if (flatList == null || flatList.isEmpty()) {
            return roots;
        }
        Set<Long> ids = flatList.stream().map(SysDictData::getDictCode)
                .filter(Objects::nonNull).collect(Collectors.toSet());
        Map<Long, List<SysDictData>> childrenMap = flatList.stream()
                .filter(d -> d.getParentDictId() != null && d.getParentDictId() != 0L)
                .collect(Collectors.groupingBy(SysDictData::getParentDictId));
        for (SysDictData node : flatList) {
            Long parentId = node.getParentDictId();
            if (parentId == null || parentId == 0L || !ids.contains(parentId)) {
                roots.add(node);
            }
        }
        Comparator<SysDictData> sortComparator = Comparator.comparing(
                SysDictData::getDictSort, Comparator.nullsLast(Long::compareTo));
        roots.sort(sortComparator);
        for (List<SysDictData> children : childrenMap.values()) {
            children.sort(sortComparator);
        }
        for (SysDictData node : flatList) {
            List<SysDictData> children = childrenMap.get(node.getDictCode());
            if (children != null && !children.isEmpty()) {
                node.setChildren(children);
            }
        }
        return roots;
    }

    /**
     * 根据字典类型ID查询信息
     *
     * @param dictId 字典类型ID
     * @return 字典类型
     */
    @Override
    public SysDictType selectDictTypeById(Long dictId) {
        return dictTypeMapper.selectDictTypeById(dictId);
    }

    /**
     * 根据字典类型查询信息
     *
     * @param dictType 字典类型
     * @return 字典类型
     */
    @Override
    public SysDictType selectDictTypeByType(String dictType) {
        return dictTypeMapper.selectDictTypeByType(dictType);
    }

    /**
     * 批量删除字典类型信息
     *
     * @param dictIds 需要删除的字典ID
     */
    @Override
    public void deleteDictTypeByIds(Long[] dictIds) {
        for (Long dictId : dictIds) {
            SysDictType dictType = selectDictTypeById(dictId);
            if (StringUtils.isNull(dictType)) {
                continue;
            }
            if (dictDataMapper.countDictDataByType(dictType.getDictType()) > 0) {
                throw new BusinessException(String.format("%1$s已分配,不能删除", dictType.getDictName()));
            }
            dictTypeMapper.deleteDictTypeById(dictId);
            DictUtils.removeDictCache(dictType.getDictType());
            DictUtils.removeDictTreeCache(dictType.getDictType());
        }
    }

    /**
     * 加载字典缓存数据
     */
    @Override
    public void loadingDictCache() {
        SysDictData dictData = new SysDictData();
        dictData.setStatus("0");
        Map<String, List<SysDictData>> dictMap = dictDataMapper.selectDictDataList(dictData).stream().collect(Collectors.groupingBy(SysDictData::getDictType));
        for (Map.Entry<String, List<SysDictData>> entry : dictMap.entrySet()) {
            DictUtils.setDictCache(entry.getKey(), entry.getValue());
        }
    }

    /**
     * 清空字典缓存数据
     */
    @Override
    public void clearDictCache() {
        Collection<String> keys = redisCache.keys(CacheConstants.SYS_DICT_KEY + "*");
        if (StringUtils.isNotEmpty(keys)) {
            redisCache.deleteObject(keys);
        }
        Collection<String> treeKeys = redisCache.keys(CacheConstants.SYS_DICT_TREE_KEY + "*");
        if (StringUtils.isNotEmpty(treeKeys)) {
            redisCache.deleteObject(treeKeys);
        }
    }

    /**
     * 重置字典缓存数据
     */
    @Override
    public void resetDictCache() {
        clearDictCache();
        loadingDictCache();
    }

    /**
     * 新增保存字典类型信息
     *
     * @param dict 字典类型信息
     * @return 结果
     */
    @Override
    public int insertDictType(SysDictType dict) {
        int row = dictTypeMapper.insertDictType(dict);
        if (row > 0) {
            // 新增字典类型时，无需设置缓存，查询时会自动加载
            // 或者预置空列表缓存
            DictUtils.setDictCache(dict.getDictType(), List.of());
        }
        return row;
    }

    /**
     * 修改保存字典类型信息
     *
     * @param dict 字典类型信息
     * @return 结果
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public int updateDictType(SysDictType dict) {
        SysDictType oldDict = dictTypeMapper.selectDictTypeById(dict.getDictId());
        if (StringUtils.isNull(oldDict)) {
            throw new BusinessException("修改失败，字典类型不存在");
        }
        String oldDictType = oldDict.getDictType();
        String newDictType = dict.getDictType();

        // 更新字典数据中的字典类型字段
        dictDataMapper.updateDictDataType(oldDictType, newDictType);
        int row = dictTypeMapper.updateDictType(dict);

        if (row > 0) {
            // 清除旧缓存
            DictUtils.removeDictCache(oldDictType);
            DictUtils.removeDictTreeCache(oldDictType);
            // 加载新缓存
            List<SysDictData> dictDatas = dictDataMapper.selectDictDataByType(newDictType);
            DictUtils.setDictCache(newDictType, dictDatas);
            DictUtils.removeDictTreeCache(newDictType);
        }
        return row;
    }

    /**
     * 校验字典类型称是否唯一
     *
     * @param dict 字典类型
     * @return 结果
     */
    @Override
    public boolean checkDictTypeUnique(SysDictType dict) {
        Long dictId = StringUtils.isNull(dict.getDictId()) ? -1L : dict.getDictId();
        SysDictType dictType = dictTypeMapper.checkDictTypeUnique(dict.getDictType());
        if (StringUtils.isNotNull(dictType) && dictType.getDictId().longValue() != dictId.longValue()) {
            return false;
        }
        return true;
    }
}
