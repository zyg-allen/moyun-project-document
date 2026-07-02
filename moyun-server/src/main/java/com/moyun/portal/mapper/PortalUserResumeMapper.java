package com.moyun.portal.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.moyun.portal.domain.entity.PortalUserResume;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * 用户简历 Mapper
 *
 * @author moyun
 */
@Mapper
public interface PortalUserResumeMapper extends BaseMapper<PortalUserResume> {

    /**
     * 查询用户的简历列表（按更新时间倒序，排除已归档）
     */
    @Select("SELECT * FROM portal_user_resume WHERE user_id = #{userId} AND status <> 'archived' ORDER BY update_time DESC")
    List<PortalUserResume> selectListByUserId(@Param("userId") Long userId);

    /**
     * 查询某个简历的所有历史版本（按版本号正序）
     */
    @Select("SELECT * FROM portal_user_resume WHERE (id = #{id} OR parent_id = #{id}) ORDER BY version_no ASC")
    List<PortalUserResume> selectVersionHistory(@Param("id") Long id);

    /**
     * 统计用户的简历数量（排除已归档）
     */
    @Select("SELECT COUNT(*) FROM portal_user_resume WHERE user_id = #{userId} AND status <> 'archived'")
    int countByUserId(@Param("userId") Long userId);
}
