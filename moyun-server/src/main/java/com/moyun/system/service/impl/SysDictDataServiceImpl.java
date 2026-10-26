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
import java.util.Objects;
import java.util.Set;

/**
 * 字典 业务层处理
 *
 * @author allen-zyg
 */
@Service
public class SysDictDataServiceImpl implements ISysDictDataService {

    /**
     * 记账字典：dict_value 参与历史数据匹配（流水类型/账户类型/单位等），
     * 改值会让历史数据对不上，因此<b>一律禁止改值</b>，只允许改标签/排序/启停。
     */
    private static final Set<String> LEDGER_VALUE_LOCKED = Set.of(
            "ledger_txn_type", "ledger_account_category", "ledger_identity_tag",
            "ledger_warehouse_type", "ledger_shelf_type",
            "ledger_stock_in_reason", "ledger_stock_out_reason",
            "ledger_product_source", "ledger_out_type", "ledger_packaging_type",
            "ledger_product_unit", "ledger_shelf_life", "ledger_condition_grade",
            "ledger_saving_method", "ledger_schedule_cycle", "ledger_remind_rule");

    /**
     * 语义锁定：除"不可改值"外，还<b>不允许新增取值</b>——这些值参与资金/统计计算口径
     * （如新增一个交易类型，后端计算分支并不认识它）。
     */
    private static final Set<String> LEDGER_SEMANTIC_LOCKED = Set.of(
            "ledger_txn_type", "ledger_account_category");

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
        assertLedgerDictWritable(data, true);
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
        assertLedgerDictWritable(data, false);
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

    /**
     * 记账字典写入约束：
     * <ol>
     *   <li>新增：语义锁定类型禁止新增取值（避免计算口径漂移）；</li>
     *   <li>修改：任何记账字典都禁止改 dict_value（历史数据按值匹配）。</li>
     * </ol>
     * 非 ledger_ 前缀（系统/门户字典）不受影响。
     */
    private void assertLedgerDictWritable(SysDictData data, boolean insert) {
        String type = data.getDictType();
        if (type == null || !type.startsWith("ledger_")) {
            return;
        }
        if (insert && LEDGER_SEMANTIC_LOCKED.contains(type)) {
            throw new BusinessException("「" + type + "」是记账计算口径字典，不允许新增取值（可修改标签/排序/启停）");
        }
        if (!insert && LEDGER_VALUE_LOCKED.contains(type)) {
            SysDictData exist = data.getDictCode() == null ? null : selectDictDataById(data.getDictCode());
            if (exist != null && !Objects.equals(exist.getDictValue(), data.getDictValue())) {
                throw new BusinessException("记账字典的取值（" + exist.getDictValue() + "）不允许修改，"
                        + "否则历史数据将无法匹配；请改为停用后新增");
            }
        }
    }
}
