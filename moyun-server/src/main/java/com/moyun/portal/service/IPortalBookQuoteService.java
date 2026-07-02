package com.moyun.portal.service;

import java.util.List;
import java.util.Map;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;

import com.moyun.portal.domain.entity.PortalBookQuote;
import com.moyun.portal.domain.query.BookQuoteQuery;

/**
 * 金句摘录 业务层接口
 *
 * @author moyun
 */
public interface IPortalBookQuoteService extends IService<PortalBookQuote> {

    Page<PortalBookQuote> selectPortalBookQuotePage(Page<PortalBookQuote> page, BookQuoteQuery query);

    List<PortalBookQuote> selectPortalBookQuoteList(BookQuoteQuery query);

    PortalBookQuote selectPortalBookQuoteById(Long id);

    int insertPortalBookQuote(PortalBookQuote portalBookQuote);

    int updatePortalBookQuote(PortalBookQuote portalBookQuote);

    int deletePortalBookQuoteById(Long id);

    int deletePortalBookQuoteByIds(Long[] ids);

    /**
     * 查询精选金句
     */
    List<PortalBookQuote> selectFeaturedQuotes(int limit);

    /**
     * 点赞金句
     */
    void incrementLikeCount(Long id);

    /**
     * 查询某本书的公开金句
     */
    List<PortalBookQuote> selectQuotesByBookId(Long bookId, int limit);

    /**
     * 切换金句点赞（toggle，已点赞则取消，未点赞则新增）
     * @return 包含 liked(Boolean) 与 likeCount(Long)
     */
    Map<String, Object> toggleQuoteLike(Long quoteId, Long userId);

    /**
     * 查询当前用户是否已点赞该金句
     */
    boolean isQuoteLiked(Long quoteId, Long userId);
}
