package com.moyun.portal.service.impl;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.moyun.portal.domain.entity.PortalLike;
import com.moyun.portal.domain.query.LikeQuery;
import com.moyun.portal.mapper.PortalLikeMapper;
import com.moyun.portal.service.IPortalLikeService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 门户点赞 业务层处理
 *
 * @author moyun
 */
@Service
public class PortalLikeServiceImpl extends ServiceImpl<PortalLikeMapper, PortalLike> implements IPortalLikeService {

    @Autowired
    private PortalLikeMapper portalLikeMapper;

    /**
     * 根据条件分页查询点赞列表
     *
     * @param page 分页参数
     * @param query 查询条件
     * @return 分页结果
     */
    @Override
    public Page<PortalLike> selectPortalLikePage(Page<PortalLike> page, LikeQuery query) {
        return baseMapper.selectPortalLikePage(page, query);
    }

    /**
     * 根据条件查询点赞列表（不分页，用于导出等场景）
     *
     * @param query 查询条件
     * @return 点赞信息集合
     */
    @Override
    public List<PortalLike> selectPortalLikeList(LikeQuery query) {
        return baseMapper.selectPortalLikeList(query);
    }

    /**
     * 通过点赞ID查询点赞
     *
     * @param id 点赞ID
     * @return 点赞对象信息
     */
    @Override
    public PortalLike selectPortalLikeById(Long id) {
        return portalLikeMapper.selectPortalLikeById(id);
    }

    /**
     * 新增点赞信息
     *
     * @param portalLike 点赞信息
     * @return 结果
     */
    @Override
    public int insertPortalLike(PortalLike portalLike) {
        return portalLikeMapper.insertPortalLike(portalLike);
    }

    /**
     * 修改点赞信息
     *
     * @param portalLike 点赞信息
     * @return 结果
     */
    @Override
    public int updatePortalLike(PortalLike portalLike) {
        return portalLikeMapper.updatePortalLike(portalLike);
    }

    /**
     * 通过点赞ID删除点赞
     *
     * @param id 点赞ID
     * @return 结果
     */
    @Override
    public int deletePortalLikeById(Long id) {
        return portalLikeMapper.deletePortalLikeById(id);
    }

    /**
     * 批量删除点赞信息
     *
     * @param ids 需要删除的点赞ID
     * @return 结果
     */
    @Override
    public int deletePortalLikeByIds(Long[] ids) {
        return portalLikeMapper.deletePortalLikeByIds(ids);
    }
}
