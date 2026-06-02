package com.moyun.portal.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.moyun.portal.domain.entity.PortalBook;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

@Mapper
public interface PortalBookMapper extends BaseMapper<PortalBook>
{
    public List<PortalBook> selectPortalBookList(PortalBook portalBook);

    public PortalBook selectPortalBookById(Long id);

    public int insertPortalBook(PortalBook portalBook);

    public int updatePortalBook(PortalBook portalBook);

    public int deletePortalBookById(Long id);

    public int deletePortalBookByIds(Long[] ids);
}
