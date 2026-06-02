package com.moyun.portal.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.moyun.portal.domain.entity.PortalBookList;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

@Mapper
public interface PortalBookListMapper extends BaseMapper<PortalBookList>
{
    public List<PortalBookList> selectPortalBookListList(PortalBookList portalBookList);

    public PortalBookList selectPortalBookListById(Long id);

    public int insertPortalBookList(PortalBookList portalBookList);

    public int updatePortalBookList(PortalBookList portalBookList);

    public int deletePortalBookListById(Long id);

    public int deletePortalBookListByIds(Long[] ids);
}
