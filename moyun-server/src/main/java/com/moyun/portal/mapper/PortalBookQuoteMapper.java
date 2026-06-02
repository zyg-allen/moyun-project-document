package com.moyun.portal.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.moyun.portal.domain.entity.PortalBookQuote;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

@Mapper
public interface PortalBookQuoteMapper extends BaseMapper<PortalBookQuote>
{
    public List<PortalBookQuote> selectPortalBookQuoteList(PortalBookQuote portalBookQuote);

    public PortalBookQuote selectPortalBookQuoteById(Long id);

    public int insertPortalBookQuote(PortalBookQuote portalBookQuote);

    public int updatePortalBookQuote(PortalBookQuote portalBookQuote);

    public int deletePortalBookQuoteById(Long id);

    public int deletePortalBookQuoteByIds(Long[] ids);
}
