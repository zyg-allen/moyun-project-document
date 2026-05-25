package com.moyun.portal.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.moyun.portal.domain.entity.PortalWallet;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

/**
 * 门户钱包表 数据层
 *
 * @author moyun
 */
@Mapper
public interface PortalWalletMapper extends BaseMapper<PortalWallet> {

    /**
     * 根据条件分页查询钱包列表
     *
     * @param portalWallet 钱包信息
     * @return 钱包信息集合信息
     */
    public List<PortalWallet> selectPortalWalletList(PortalWallet portalWallet);

    /**
     * 通过钱包ID查询钱包
     *
     * @param id 钱包ID
     * @return 钱包对象信息
     */
    public PortalWallet selectPortalWalletById(Long id);

    /**
     * 通过用户ID查询钱包
     *
     * @param userId 用户ID
     * @return 钱包对象信息
     */
    public PortalWallet selectPortalWalletByUserId(Long userId);

    /**
     * 新增钱包信息
     *
     * @param portalWallet 钱包信息
     * @return 结果
     */
    public int insertPortalWallet(PortalWallet portalWallet);

    /**
     * 修改钱包信息
     *
     * @param portalWallet 钱包信息
     * @return 结果
     */
    public int updatePortalWallet(PortalWallet portalWallet);

    /**
     * 通过钱包ID删除钱包
     *
     * @param id 钱包ID
     * @return 结果
     */
    public int deletePortalWalletById(Long id);

    /**
     * 批量删除钱包信息
     *
     * @param ids 需要删除的钱包ID
     * @return 结果
     */
    public int deletePortalWalletByIds(Long[] ids);
}
