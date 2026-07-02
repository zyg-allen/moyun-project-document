package com.moyun.portal.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Update;

import com.moyun.ext.cms.domain.vo.BookClubActivityVO;
import com.moyun.portal.domain.entity.PortalBookClubActivity;

/**
 * 共读活动表 数据层
 *
 * @author moyun
 */
@Mapper
public interface PortalBookClubActivityMapper extends BaseMapper<PortalBookClubActivity> {

    /**
     * 分页查询活动列表（含参与人数、记录数聚合，及当前用户是否已加入标记）
     *
     * @param page          分页对象
     * @param currentUserId 当前登录用户ID（可为 null，未登录时 isJoined 返回 false）
     * @return 活动分页（含聚合统计）
     */
    Page<BookClubActivityVO> selectListWithCount(Page<BookClubActivityVO> page,
                                                 @Param("currentUserId") Long currentUserId);

    /**
     * 原子递增当前参与人数（避免并发丢失更新）
     */
    @Update("update portal_book_club_activity set current_participants = current_participants + 1 where id = #{id}")
    int incrementCurrentParticipants(@Param("id") Long id);

    /**
     * 原子递减当前参与人数（带非负保护，避免出现负数）
     */
    @Update("update portal_book_club_activity set current_participants = current_participants - 1 where id = #{id} and current_participants > 0")
    int decrementCurrentParticipants(@Param("id") Long id);
}
