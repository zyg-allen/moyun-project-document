package com.moyun.portal.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.moyun.portal.domain.entity.PortalBookListBookmark;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

/**
 * 书单收藏 Mapper
 *
 * @author moyun
 */
@Mapper
public interface PortalBookListBookmarkMapper extends BaseMapper<PortalBookListBookmark> {

    /**
     * 查询用户是否已收藏某书单
     */
    PortalBookListBookmark selectBookmark(@Param("booklistId") Long booklistId, @Param("userId") Long userId);

    /**
     * 统计书单的收藏数
     */
    long countByBooklist(@Param("booklistId") Long booklistId);
}
