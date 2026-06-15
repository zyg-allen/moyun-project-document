package com.moyun.portal.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import com.moyun.portal.domain.entity.PortalBookQuote;
import com.moyun.portal.domain.query.BookQuoteQuery;

import java.util.List;

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
}
