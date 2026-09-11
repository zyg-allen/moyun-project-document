package com.moyun.ext.cms.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.moyun.core.base.AjaxResult;
import com.moyun.ext.cms.domain.query.ColumnQuery;
import com.moyun.ext.cms.domain.vo.ArticleSimpleVO;
import com.moyun.ext.cms.domain.vo.ColumnListItemVO;
import com.moyun.portal.domain.entity.PortalColumn;

/**
 * CMS 专栏后台管理 Service 接口
 *
 * <p>提供专栏列表/详情/创建/更新/删除/审核（状态流转 draft→published→archived）。</p>
 *
 * @author moyun
 */
public interface ICmsColumnService {

    /**
     * 后台分页查询专栏（含所有状态、作者信息）
     */
    Page<ColumnListItemVO> selectColumnPage(Page<ColumnListItemVO> page, ColumnQuery query);

    /**
     * 根据ID获取专栏详情
     */
    PortalColumn selectColumnById(Long id);

    /**
     * 新增专栏
     */
    int insertColumn(PortalColumn column);

    /**
     * 修改专栏
     */
    int updateColumn(PortalColumn column);

    /**
     * 更新专栏状态（状态流转：archived 等普通流转，不走审核字段写入）
     */
    int updateColumnStatus(Long id, String status);

    /**
     * CMS 审核专栏（draft/pending → published / rejected）
     * <p>乐观锁：仅 draft 或 pending 状态可审核；
     * 审核通过时写入审核字段并推送 Feed（new_column）；
     * 审核驳回时记录驳回原因；审核结果通过站内信通知作者。</p>
     *
     * @param id          专栏ID
     * @param status      新状态：published=通过 / rejected=驳回
     * @param auditRemark 审核意见（驳回必填，通过选填）
     * @param auditorId   审核人ID（系统用户ID）
     */
    void auditColumn(Long id, String status, String auditRemark, Long auditorId);

    /**
     * 批量删除专栏
     */
    int deleteColumnByIds(Long[] ids);

    /**
     * CMS后台：批量绑定文章到专栏（不校验作者归属，管理员可操作任意文章）
     *
     * @param columnId   专栏ID
     * @param articleIds 文章ID列表
     * @return 成功绑定的条数
     */
    AjaxResult batchBindArticles(Long columnId, java.util.List<Long> articleIds);

    /**
     * CMS后台：将文章移出专栏（不校验作者归属）
     *
     * @param columnId  专栏ID
     * @param articleId 文章ID
     * @return 影响行数
     */
    int removeColumnArticle(Long columnId, Long articleId);

    /**
     * CMS后台：分页查询专栏已绑定的文章列表
     *
     * @param page     分页参数
     * @param columnId 专栏ID
     * @param keyword  文章标题关键词（可选）
     * @return 分页结果
     */
    Page<ArticleSimpleVO> selectColumnArticlesPage(
            Page<ArticleSimpleVO> page, Long columnId, String keyword);
}
