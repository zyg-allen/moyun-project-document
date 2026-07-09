package com.moyun.portal.service.impl;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.moyun.portal.domain.entity.PortalBookQuote;
import com.moyun.portal.domain.entity.PortalBookQuoteLike;
import com.moyun.portal.domain.query.BookQuoteQuery;
import com.moyun.portal.domain.vo.BookQuoteVO;
import com.moyun.portal.mapper.PortalBookQuoteLikeMapper;
import com.moyun.portal.mapper.PortalBookQuoteMapper;
import com.moyun.portal.service.IPortalBookQuoteService;
import com.moyun.portal.service.IPortalGrowthService;
import com.moyun.ext.cms.service.IFeedService;

/**
 * 金句摘录 业务层实现
 *
 * @author moyun
 */
@Service
public class PortalBookQuoteServiceImpl extends ServiceImpl<PortalBookQuoteMapper, PortalBookQuote> implements IPortalBookQuoteService {

    @Autowired
    private PortalBookQuoteMapper portalBookQuoteMapper;

    @Autowired
    private PortalBookQuoteLikeMapper portalBookQuoteLikeMapper;

    @Autowired
    private IPortalGrowthService portalGrowthService;

    @Autowired
    private IFeedService feedService;

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
        int rows = portalBookQuoteMapper.insertPortalBookQuote(portalBookQuote);

        // 记录发布金句成长事件
        if (rows > 0 && portalBookQuote.getUserId() != null) {
            portalGrowthService.recordEvent("reading", "write_quote",
                    portalBookQuote.getUserId(), "quote", portalBookQuote.getId());

            // 发布动态事件（Feed 流）
            try {
                String contentPreview = portalBookQuote.getContent();
                if (contentPreview != null && contentPreview.length() > 50) {
                    contentPreview = contentPreview.substring(0, 50) + "...";
                }
                feedService.publishEvent(portalBookQuote.getUserId(), "write_quote", "quote",
                        portalBookQuote.getId(), contentPreview,
                        portalBookQuote.getChapter(), null);
            } catch (Exception e) {
                org.slf4j.LoggerFactory.getLogger(PortalBookQuoteServiceImpl.class)
                        .error("[Feed] 金句发布动态事件失败：quoteId={}", portalBookQuote.getId(), e);
            }
        }

        return rows;
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

            // 为金句发布者记录被赞成长事件
            if (quote.getUserId() != null) {
                portalGrowthService.recordEvent("reading", "quote_liked",
                        quote.getUserId(), "quote", quote.getId());
            }
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

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Map<String, Object> toggleQuoteLike(Long quoteId, Long userId) {
        PortalBookQuoteLike existing = portalBookQuoteLikeMapper.selectOne(
                new LambdaQueryWrapper<PortalBookQuoteLike>()
                        .eq(PortalBookQuoteLike::getQuoteId, quoteId)
                        .eq(PortalBookQuoteLike::getUserId, userId));
        Map<String, Object> result = new HashMap<>();
        if (existing != null) {
            // 已点赞 → 取消点赞
            portalBookQuoteLikeMapper.deleteById(existing.getId());
            portalBookQuoteMapper.decrementLikeCount(quoteId);
            result.put("liked", false);
        } else {
            // 未点赞 → 新增点赞（并发兜底：唯一键冲突视为已点赞）
            PortalBookQuoteLike like = new PortalBookQuoteLike();
            like.setQuoteId(quoteId);
            like.setUserId(userId);
            like.setCreateTime(LocalDateTime.now());
            try {
                portalBookQuoteLikeMapper.insert(like);
                portalBookQuoteMapper.incrementLikeCount(quoteId);
                result.put("liked", true);
            } catch (DuplicateKeyException e) {
                // 并发下另一事务已点赞，视为已点赞（不再重复计数）
                result.put("liked", true);
            }
        }
        // 返回最新点赞数
        PortalBookQuote quote = portalBookQuoteMapper.selectPortalBookQuoteById(quoteId);
        result.put("likeCount", quote != null && quote.getLikeCount() != null ? quote.getLikeCount() : 0L);
        return result;
    }

    @Override
    public boolean isQuoteLiked(Long quoteId, Long userId) {
        Long count = portalBookQuoteLikeMapper.selectCount(
                new LambdaQueryWrapper<PortalBookQuoteLike>()
                        .eq(PortalBookQuoteLike::getQuoteId, quoteId)
                        .eq(PortalBookQuoteLike::getUserId, userId));
        return count != null && count > 0;
    }

    @Override
    public Page<BookQuoteVO> selectQuoteVOPage(Page<BookQuoteVO> page, BookQuoteQuery query) {
        return portalBookQuoteMapper.selectQuoteVOPage(page, query);
    }

    @Override
    public BookQuoteVO selectQuoteVOById(Long id) {
        return portalBookQuoteMapper.selectQuoteVOById(id);
    }

    @Override
    public List<BookQuoteVO> selectFeaturedQuoteVOs(int limit) {
        BookQuoteQuery query = new BookQuoteQuery();
        query.setIsFeatured(true);
        query.setIsPublic(true);
        Page<BookQuoteVO> page = new Page<>(1, limit);
        Page<BookQuoteVO> result = portalBookQuoteMapper.selectQuoteVOPage(page, query);
        return result.getRecords();
    }
}
