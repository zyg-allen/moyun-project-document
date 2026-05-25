package com.moyun.portal.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.moyun.portal.domain.PortalTag;
import com.moyun.portal.mapper.PortalTagMapper;
import com.moyun.portal.service.IPortalTagService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 门户标签 业务层处理
 *
 * @author moyun
 */
@Service
public class PortalTagServiceImpl extends ServiceImpl<PortalTagMapper, PortalTag> implements IPortalTagService {

    @Autowired
    private PortalTagMapper portalTagMapper;

    /**
     * 根据条件分页查询标签列表
     *
     * @param portalTag 标签信息
     * @return 标签信息集合信息
     */
    @Override
    public List<PortalTag> selectPortalTagList(PortalTag portalTag) {
        return portalTagMapper.selectPortalTagList(portalTag);
    }

    /**
     * 通过标签ID查询标签
     *
     * @param id 标签ID
     * @return 标签对象信息
     */
    @Override
    public PortalTag selectPortalTagById(Long id) {
        return portalTagMapper.selectPortalTagById(id);
    }

    /**
     * 新增标签信息
     *
     * @param portalTag 标签信息
     * @return 结果
     */
    @Override
    public int insertPortalTag(PortalTag portalTag) {
        return portalTagMapper.insertPortalTag(portalTag);
    }

    /**
     * 修改标签信息
     *
     * @param portalTag 标签信息
     * @return 结果
     */
    @Override
    public int updatePortalTag(PortalTag portalTag) {
        return portalTagMapper.updatePortalTag(portalTag);
    }

    /**
     * 通过标签ID删除标签
     *
     * @param id 标签ID
     * @return 结果
     */
    @Override
    public int deletePortalTagById(Long id) {
        return portalTagMapper.deletePortalTagById(id);
    }

    /**
     * 批量删除标签信息
     *
     * @param ids 需要删除的标签ID
     * @return 结果
     */
    @Override
    public int deletePortalTagByIds(Long[] ids) {
        return portalTagMapper.deletePortalTagByIds(ids);
    }
}
