package com.moyun.system.service.impl;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.moyun.core.base.entity.SysDictData;
import com.moyun.core.mvc.handler.BusinessException;
import com.moyun.system.mapper.SysDictDataMapper;
import com.moyun.system.service.ISysDictDataService;
import com.moyun.util.cache.DictUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 字典 业务层处理
 *
 * @author allen-zyg
 */
@Service
public class SysDictDataServiceImpl implements ISysDictDataService {

    @Autowired
    private SysDictDataMapper dictDataMapper;

    /**
     * 根据条件分页查询字典数据（MyBatis-Plus 分页）
     *
     * @param page      分页对象
     * @param dictData  字典数据信息
     * @return 字典数据分页数据
     */
    @Override
    public IPage<SysDictData> selectDictDataPage(IPage<SysDictData> page, SysDictData dictData) {
        return dictDataMapper.selectDictDataPage(page, dictData);
    }

    /**
     * 根据条件分页查询字典数据
     *
     * @param dictData 字典数据信息
     * @return 字典数据集合信息
     */
    @Override
    public List<SysDictData> selectDictDataList(SysDictData dictData) {
        return dictDataMapper.selectDictDataList(dictData);
    }

    /**
     * 根据字典类型和字典键值查询字典数据信息
     *
     * @param dictType  字典类型
     * @param dictValue 字典键值
     * @return 字典标签
     */
    @Override
    public String selectDictLabel(String dictType, String dictValue) {
        return dictDataMapper.selectDictLabel(dictType, dictValue);
    }

    /**
     * 根据字典数据ID查询信息
     *
     * @param dictCode 字典数据ID
     * @return 字典数据
     */
    @Override
    public SysDictData selectDictDataById(Long dictCode) {
        return dictDataMapper.selectDictDataById(dictCode);
    }

    /**
     * 批量删除字典数据信息
     *
     * @param dictCodes 需要删除的字典数据ID
     */
    @Override
    public void deleteDictDataByIds(Long[] dictCodes) {
        for (Long dictCode : dictCodes) {
            SysDictData data = selectDictDataById(dictCode);
            if (data == null) {
                continue;
            }
            if (dictDataMapper.countDictDataByParent(dictCode) > 0) {
                throw new BusinessException(String.format("「%s」存在子节点，不能删除", data.getDictLabel()));
            }
            dictDataMapper.deleteDictDataById(dictCode);
            List<SysDictData> dictDatas = dictDataMapper.selectDictDataByType(data.getDictType());
            DictUtils.setDictCache(data.getDictType(), dictDatas);
            DictUtils.removeDictTreeCache(data.getDictType());
        }
    }

    /**
     * 新增保存字典数据信息
     *
     * @param data 字典数据信息
     * @return 结果
     */
    @Override
    public int insertDictData(SysDictData data) {
        int row = dictDataMapper.insertDictData(data);
        if (row > 0) {
            List<SysDictData> dictDatas = dictDataMapper.selectDictDataByType(data.getDictType());
            DictUtils.setDictCache(data.getDictType(), dictDatas);
            DictUtils.removeDictTreeCache(data.getDictType());
        }
        return row;
    }

    /**
     * 修改保存字典数据信息
     *
     * @param data 字典数据信息
     * @return 结果
     */
    @Override
    public int updateDictData(SysDictData data) {
        // 停用前校验：存在启用中的子孙节点时禁止停用（逐级：父停用须先停用全部子节点）
        if ("1".equals(data.getStatus())) {
            SysDictData existing = selectDictDataById(data.getDictCode());
            if (existing != null && dictDataMapper.countEnabledDescendants(data.getDictCode()) > 0) {
                throw new BusinessException(String.format("「%s」存在启用中的子节点，不能停用", existing.getDictLabel()));
            }
        }
        int row = dictDataMapper.updateDictData(data);
        if (row > 0) {
            List<SysDictData> dictDatas = dictDataMapper.selectDictDataByType(data.getDictType());
            DictUtils.setDictCache(data.getDictType(), dictDatas);
            DictUtils.removeDictTreeCache(data.getDictType());
        }
        return row;
    }
}
