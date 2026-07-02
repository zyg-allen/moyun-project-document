package com.moyun.portal.mapper;

import java.util.List;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import com.moyun.portal.domain.entity.PortalBookQuote;
import com.moyun.portal.domain.query.BookQuoteQuery;

/**
 * 金句摘录表 数据层
 *
 * @author moyun
 */
@Mapper
public interface PortalBookQuoteMapper extends BaseMapper<PortalBookQuote>
{
    Page<PortalBookQuote> selectPortalBookQuotePage(Page<PortalBookQuote> page, @Param("query") BookQuoteQuery query);

    List<PortalBookQuote> selectPortalBookQuoteList(@Param("query") BookQuoteQuery query);

    PortalBookQuote selectPortalBookQuoteById(@Param("id") Long id);

    int insertPortalBookQuote(PortalBookQuote portalBookQuote);

    int updatePortalBookQuote(PortalBookQuote portalBookQuote);

    int deletePortalBookQuoteById(@Param("id") Long id);

    int deletePortalBookQuoteByIds(@Param("ids") Long[] ids);

    /**
     * 原子递增点赞数（避免并发丢失更新）
     */
    int incrementLikeCount(@Param("id") Long id);

    /**
     * 原子递减点赞数（避免并发丢失更新，不低于 0）
     */
    int decrementLikeCount(@Param("id") Long id);
}
