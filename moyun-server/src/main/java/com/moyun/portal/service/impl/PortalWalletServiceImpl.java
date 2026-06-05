package com.moyun.portal.service.impl;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.moyun.portal.domain.entity.PortalWallet;
import com.moyun.portal.mapper.PortalWalletMapper;
import com.moyun.portal.service.IPortalWalletService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 门户钱包 业务层处理
 *
 * @author moyun
 */
@Service
public class PortalWalletServiceImpl extends ServiceImpl<PortalWalletMapper, PortalWallet> implements IPortalWalletService {

    @Autowired
    private PortalWalletMapper portalWalletMapper;

    /**
     * 根据条件分页查询钱包列表
     *
     * @param page 分页参数
     * @param portalWallet 钱包信息
     * @return 分页结果
     */
    @Override
    public Page<PortalWallet> selectPortalWalletPage(Page<PortalWallet> page, PortalWallet portalWallet) {
        return baseMapper.selectPortalWalletPage(page, portalWallet);
    }

    /**
     * 根据条件查询钱包列表（不分页，用于导出等场景）
     *
     * @param portalWallet 钱包信息
     * @return 钱包信息集合
     */
    @Override
    public List<PortalWallet> selectPortalWalletList(PortalWallet portalWallet) {
        return baseMapper.selectPortalWalletList(portalWallet);
    }

    /**
     * 通过钱包ID查询钱包
     *
     * @param id 钱包ID
     * @return 钱包对象信息
     */
    @Override
    public PortalWallet selectPortalWalletById(Long id) {
        return portalWalletMapper.selectPortalWalletById(id);
    }

    /**
     * 通过用户ID查询钱包
     *
     * @param userId 用户ID
     * @return 钱包对象信息
     */
    @Override
    public PortalWallet selectPortalWalletByUserId(Long userId) {
        return portalWalletMapper.selectPortalWalletByUserId(userId);
    }

    /**
     * 新增钱包信息
     *
     * @param portalWallet 钱包信息
     * @return 结果
     */
    @Override
    public int insertPortalWallet(PortalWallet portalWallet) {
        return portalWalletMapper.insertPortalWallet(portalWallet);
    }

    /**
     * 修改钱包信息
     *
     * @param portalWallet 钱包信息
     * @return 结果
     */
    @Override
    public int updatePortalWallet(PortalWallet portalWallet) {
        return portalWalletMapper.updatePortalWallet(portalWallet);
    }

    /**
     * 通过钱包ID删除钱包
     *
     * @param id 钱包ID
     * @return 结果
     */
    @Override
    public int deletePortalWalletById(Long id) {
        return portalWalletMapper.deletePortalWalletById(id);
    }

    /**
     * 批量删除钱包信息
     *
     * @param ids 需要删除的钱包ID
     * @return 结果
     */
    @Override
    public int deletePortalWalletByIds(Long[] ids) {
        return portalWalletMapper.deletePortalWalletByIds(ids);
    }
}
