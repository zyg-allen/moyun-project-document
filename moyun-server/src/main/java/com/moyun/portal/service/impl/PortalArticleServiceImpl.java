package com.moyun.portal.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.moyun.portal.domain.PortalArticle;
import com.moyun.portal.mapper.PortalArticleMapper;
import com.moyun.portal.service.IPortalArticleService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 门户文章 业务层处理
 *
 * @author moyun
 */
@Service
public class PortalArticleServiceImpl extends ServiceImpl<PortalArticleMapper, PortalArticle> implements IPortalArticleService {

    @Autowired
    private PortalArticleMapper portalArticleMapper;

    /**
     * 根据条件分页查询文章列表
     *
     * @param portalArticle 文章信息
     * @return 文章信息集合信息
     */
    @Override
    public List<PortalArticle> selectPortalArticleList(PortalArticle portalArticle) {
        return portalArticleMapper.selectPortalArticleList(portalArticle);
    }

    /**
     * 通过文章ID查询文章
     *
     * @param id 文章ID
     * @return 文章对象信息
     */
    @Override
    public PortalArticle selectPortalArticleById(Long id) {
        return portalArticleMapper.selectPortalArticleById(id);
    }

    /**
     * 新增文章信息
     *
     * @param portalArticle 文章信息
     * @return 结果
     */
    @Override
    public int insertPortalArticle(PortalArticle portalArticle) {
        return portalArticleMapper.insertPortalArticle(portalArticle);
    }

    /**
     * 修改文章信息
     *
     * @param portalArticle 文章信息
     * @return 结果
     */
    @Override
    public int updatePortalArticle(PortalArticle portalArticle) {
        return portalArticleMapper.updatePortalArticle(portalArticle);
    }

    /**
     * 通过文章ID删除文章
     *
     * @param id 文章ID
     * @return 结果
     */
    @Override
    public int deletePortalArticleById(Long id) {
        return portalArticleMapper.deletePortalArticleById(id);
    }

    /**
     * 批量删除文章信息
     *
     * @param ids 需要删除的文章ID
     * @return 结果
     */
    @Override
    public int deletePortalArticleByIds(Long[] ids) {
        return portalArticleMapper.deletePortalArticleByIds(ids);
    }
}
