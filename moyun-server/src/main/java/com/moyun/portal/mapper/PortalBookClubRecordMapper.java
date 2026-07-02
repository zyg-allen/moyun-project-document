package com.moyun.portal.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Update;

import com.moyun.ext.cms.domain.vo.BookClubRecordVO;
import com.moyun.portal.domain.entity.PortalBookClubRecord;

/**
 * 共读打卡记录表 数据层
 *
 * @author moyun
 */
@Mapper
public interface PortalBookClubRecordMapper extends BaseMapper<PortalBookClubRecord> {

    /**
     * 分页查询某活动的打卡记录列表（联表作者信息，含当前用户是否已点赞标记）
     *
     * @param page          分页对象
     * @param activityId    活动ID
     * @param currentUserId 当前登录用户ID（可为 null，未登录时 isLiked 返回 false）
     * @return 记录分页（含作者信息与点赞标记）
     */
    Page<BookClubRecordVO> selectListByActivity(Page<BookClubRecordVO> page,
                                                 @Param("activityId") Long activityId,
                                                 @Param("currentUserId") Long currentUserId);

    /**
     * 原子递增点赞数（避免并发丢失更新）
     */
    @Update("update portal_book_club_record set like_count = like_count + 1 where id = #{id}")
    int incrementLikeCount(@Param("id") Long id);

    /**
     * 原子递减点赞数（带非负保护，避免出现负数）
     */
    @Update("update portal_book_club_record set like_count = like_count - 1 where id = #{id} and like_count > 0")
    int decrementLikeCount(@Param("id") Long id);
}
