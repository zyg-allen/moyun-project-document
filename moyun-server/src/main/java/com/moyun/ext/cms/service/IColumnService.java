package com.moyun.ext.cms.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.moyun.core.base.page.PageDomain;
import com.moyun.ext.cms.domain.query.ColumnQuery;
import com.moyun.ext.cms.domain.vo.ColumnArticleSortItem;
import com.moyun.ext.cms.domain.vo.ColumnListItemVO;
import com.moyun.ext.cms.domain.vo.ColumnVO;

import java.util.List;

/**
 * 专栏 Service 接口（创作者天堂核心）
 *
 * @author moyun
 */
public interface IColumnService {

    /**
     * 专栏公开列表（分页，仅 published）
     */
    Page<ColumnListItemVO> listColumns(ColumnQuery query);

    /**
     * 专栏详情（公开，含作者信息、文章目录、当前用户是否订阅）
     *
     * @param id            专栏ID
     * @param currentUserId 当前登录用户ID（未登录传 null）
     */
    ColumnVO getColumnDetail(Long id, Long currentUserId);

    /**
     * 创建/修改专栏。
     * 创建时校验同用户名下专栏数量上限 10 个；修改时校验归属。
     *
     * @return 专栏ID
     */
    Long saveColumn(ColumnVO vo, Long userId);

    /**
     * 完结/恢复连载（切换 is_finished）
     *
     * @return 影响行数
     */
    int toggleFinish(Long id, Long userId);

    /**
     * 作者**主动提交审核**：draft / rejected → pending，并创建统一审核任务。
     *
     * <p>为什么必须有：门户新建专栏被强制置为 {@code draft}（防绕过审核），但此前**没有任何门户侧送审入口**
     * ——只有"命中敏感词"才会被动转 pending。正常内容的专栏因此永远停在 draft，他人不可见、
     * 后台审核中心也没有待办（清单 #15）。</p>
     *
     * @param id     专栏ID
     * @param userId 当前用户（须为作者）
     * @return 送审后的专栏详情
     */
    ColumnVO submitForAudit(Long id, Long userId);

    /**
     * 删除专栏（仅作者本人，级联删除关联与订阅）
     *
     * @return 影响行数
     */
    int deleteColumn(Long id, Long userId);

    /**
     * 订阅/取消订阅（toggle，原子更新 subscribeCount）
     *
     * @return 操作后的订阅状态：true=已订阅，false=已取消
     */
    boolean toggleSubscribe(Long columnId, Long userId);

    /**
     * 我创建的专栏（分页）
     */
    Page<ColumnListItemVO> listMyColumns(Long userId, PageDomain query);

    /**
     * 我订阅的专栏（分页）
     */
    Page<ColumnListItemVO> listSubscribedColumns(Long userId, PageDomain query);

    /**
     * 将文章加入专栏（校验专栏归属、文章归属；原子更新 articleCount）
     *
     * @return 影响行数
     */
    int addArticle(Long columnId, Long articleId, Long userId);

    /**
     * 将文章移出专栏（原子更新 articleCount）
     *
     * @return 影响行数
     */
    int removeArticle(Long columnId, Long articleId, Long userId);

    /**
     * 批量排序专栏内文章
     *
     * @param list 排序项（id=文章ID, sortOrder=新顺序）
     * @return 影响行数
     */
    int sortArticles(Long columnId, List<ColumnArticleSortItem> list, Long userId);
}
