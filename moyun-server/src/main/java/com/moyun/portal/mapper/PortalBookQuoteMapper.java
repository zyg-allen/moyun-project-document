package com.moyun.portal.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.moyun.portal.domain.entity.PortalBookQuote;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

/**
 * 门户书籍语录表 数据层
 *
 * @author moyun
 */
@Mapper
public interface PortalBookQuoteMapper extends BaseMapper<PortalBookQuote>
{
    /**
     * 根据条件分页查询语录列表
     *
     * @param portalBookQuote 语录信息
     * @return 语录信息集合信息
     */
    public List<PortalBookQuote> selectPortalBookQuoteList(PortalBookQuote portalBookQuote);

    /**
     * 通过语录ID查询语录
     *
     * @param id 语录ID
     * @return 语录对象信息
     */
    public PortalBookQuote selectPortalBookQuoteById(Long id);

    /**
     * 新增语录信息
     *
     * @param portalBookQuote 语录信息
     * @return 结果
     */
    public int insertPortalBookQuote(PortalBookQuote portalBookQuote);

    /**
     * 修改语录信息
     *
     * @param portalBookQuote 语录信息
     * @return 结果
     */
    public int updatePortalBookQuote(PortalBookQuote portalBookQuote);

    /**
     * 通过语录ID删除语录
     *
     * @param id 语录ID
     * @return 结果
     */
    public int deletePortalBookQuoteById(Long id);

    /**
     * 批量删除语录信息
     *
     * @param ids 需要删除的语录ID
     * @return 结果
     */
    public int deletePortalBookQuoteByIds(Long[] ids);
}