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

    /**
     * 批量查询指定用户已收藏的书单 id 集合（避免逐个书单查询造成 N+1）。
     *
     * @param userId       用户ID
     * @param booklistIds  待检查的书单ID集合（调用方需保证非空）
     * @return 已收藏的书单ID列表
     */
    java.util.List<Long> selectBookmarkedIds(@Param("userId") Long userId,
                                             @Param("booklistIds") java.util.Collection<Long> booklistIds);
}
