package com.moyun.portal.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.moyun.portal.domain.PortalWalletTransaction;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

/**
 * 门户钱包交易表 数据层
 *
 * @author moyun
 */
@Mapper
public interface PortalWalletTransactionMapper extends BaseMapper<PortalWalletTransaction> {

    /**
     * 根据条件分页查询钱包交易列表
     *
     * @param portalWalletTransaction 钱包交易信息
     * @return 钱包交易信息集合信息
     */
    public List<PortalWalletTransaction> selectPortalWalletTransactionList(PortalWalletTransaction portalWalletTransaction);

    /**
     * 通过钱包交易ID查询钱包交易
     *
     * @param id 钱包交易ID
     * @return 钱包交易对象信息
     */
    public PortalWalletTransaction selectPortalWalletTransactionById(Long id);

    /**
     * 新增钱包交易信息
     *
     * @param portalWalletTransaction 钱包交易信息
     * @return 结果
     */
    public int insertPortalWalletTransaction(PortalWalletTransaction portalWalletTransaction);

    /**
     * 通过钱包交易ID删除钱包交易
     *
     * @param id 钱包交易ID
     * @return 结果
     */
    public int deletePortalWalletTransactionById(Long id);

    /**
     * 批量删除钱包交易信息
     *
     * @param ids 需要删除的钱包交易ID
     * @return 结果
     */
    public int deletePortalWalletTransactionByIds(Long[] ids);
}
