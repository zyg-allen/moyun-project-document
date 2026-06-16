package com.moyun.portal.service.impl;

import java.time.LocalDateTime;
import java.util.List;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.moyun.portal.domain.entity.PortalBookQuote;
import com.moyun.portal.domain.query.BookQuoteQuery;
import com.moyun.portal.mapper.PortalBookQuoteMapper;
import com.moyun.portal.service.IPortalBookQuoteService;

/**
 * 金句摘录 业务层实现
 *
 * @author moyun
 */
@Service
public class PortalBookQuoteServiceImpl extends ServiceImpl<PortalBookQuoteMapper, PortalBookQuote> implements IPortalBookQuoteService {

    @Autowired
    private PortalBookQuoteMapper portalBookQuoteMapper;

    @Override
    public Page<PortalBookQuote> selectPortalBookQuotePage(Page<PortalBookQuote> page, BookQuoteQuery query) {
        return portalBookQuoteMapper.selectPortalBookQuotePage(page, query);
    }

    @Override
    public List<PortalBookQuote> selectPortalBookQuoteList(BookQuoteQuery query) {
        return portalBookQuoteMapper.selectPortalBookQuoteList(query);
    }

    @Override
    public PortalBookQuote selectPortalBookQuoteById(Long id) {
        return portalBookQuoteMapper.selectPortalBookQuoteById(id);
    }

    @Override
    public int insertPortalBookQuote(PortalBookQuote portalBookQuote) {
        if (portalBookQuote.getCreateTime() == null) {
            portalBookQuote.setCreateTime(LocalDateTime.now());
        }
        if (portalBookQuote.getIsPublic() == null) {
            portalBookQuote.setIsPublic(true);
        }
        if (portalBookQuote.getLikeCount() == null) {
            portalBookQuote.setLikeCount(0L);
        }
        return portalBookQuoteMapper.insertPortalBookQuote(portalBookQuote);
    }

    @Override
    public int updatePortalBookQuote(PortalBookQuote portalBookQuote) {
        portalBookQuote.setUpdateTime(LocalDateTime.now());
        return portalBookQuoteMapper.updatePortalBookQuote(portalBookQuote);
    }

    @Override
    public int deletePortalBookQuoteById(Long id) {
        return portalBookQuoteMapper.deletePortalBookQuoteById(id);
    }

    @Override
    public int deletePortalBookQuoteByIds(Long[] ids) {
        return portalBookQuoteMapper.deletePortalBookQuoteByIds(ids);
    }

    @Override
    public List<PortalBookQuote> selectFeaturedQuotes(int limit) {
        BookQuoteQuery query = new BookQuoteQuery();
        query.setIsFeatured(true);
        query.setIsPublic(true);
        Page<PortalBookQuote> page = new Page<>(1, limit);
        Page<PortalBookQuote> result = portalBookQuoteMapper.selectPortalBookQuotePage(page, query);
        return result.getRecords();
    }

    @Override
    public void incrementLikeCount(Long id) {
        PortalBookQuote quote = portalBookQuoteMapper.selectPortalBookQuoteById(id);
        if (quote != null) {
            quote.setLikeCount((quote.getLikeCount() == null ? 0L : quote.getLikeCount()) + 1L);
            quote.setUpdateTime(LocalDateTime.now());
            portalBookQuoteMapper.updatePortalBookQuote(quote);
        }
    }

    @Override
    public List<PortalBookQuote> selectQuotesByBookId(Long bookId, int limit) {
        BookQuoteQuery query = new BookQuoteQuery();
        query.setBookId(bookId);
        query.setIsPublic(true);
        Page<PortalBookQuote> page = new Page<>(1, limit);
        Page<PortalBookQuote> result = portalBookQuoteMapper.selectPortalBookQuotePage(page, query);
        return result.getRecords();
    }
}
