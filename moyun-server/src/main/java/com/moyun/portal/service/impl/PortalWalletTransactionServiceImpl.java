package com.moyun.portal.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.moyun.portal.domain.PortalWalletTransaction;
import com.moyun.portal.mapper.PortalWalletTransactionMapper;
import com.moyun.portal.service.IPortalWalletTransactionService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 门户钱包交易 业务层处理
 *
 * @author moyun
 */
@Service
public class PortalWalletTransactionServiceImpl extends ServiceImpl<PortalWalletTransactionMapper, PortalWalletTransaction> implements IPortalWalletTransactionService {

    @Autowired
    private PortalWalletTransactionMapper portalWalletTransactionMapper;

    /**
     * 根据条件分页查询钱包交易列表
     *
     * @param portalWalletTransaction 钱包交易信息
     * @return 钱包交易信息集合信息
     */
    @Override
    public List<PortalWalletTransaction> selectPortalWalletTransactionList(PortalWalletTransaction portalWalletTransaction) {
        return portalWalletTransactionMapper.selectPortalWalletTransactionList(portalWalletTransaction);
    }

    /**
     * 通过钱包交易ID查询钱包交易
     *
     * @param id 钱包交易ID
     * @return 钱包交易对象信息
     */
    @Override
    public PortalWalletTransaction selectPortalWalletTransactionById(Long id) {
        return portalWalletTransactionMapper.selectPortalWalletTransactionById(id);
    }

    /**
     * 新增钱包交易信息
     *
     * @param portalWalletTransaction 钱包交易信息
     * @return 结果
     */
    @Override
    public int insertPortalWalletTransaction(PortalWalletTransaction portalWalletTransaction) {
        return portalWalletTransactionMapper.insertPortalWalletTransaction(portalWalletTransaction);
    }

    /**
     * 通过钱包交易ID删除钱包交易
     *
     * @param id 钱包交易ID
     * @return 结果
     */
    @Override
    public int deletePortalWalletTransactionById(Long id) {
        return portalWalletTransactionMapper.deletePortalWalletTransactionById(id);
    }

    /**
     * 批量删除钱包交易信息
     *
     * @param ids 需要删除的钱包交易ID
     * @return 结果
     */
    @Override
    public int deletePortalWalletTransactionByIds(Long[] ids) {
        return portalWalletTransactionMapper.deletePortalWalletTransactionByIds(ids);
    }
}
