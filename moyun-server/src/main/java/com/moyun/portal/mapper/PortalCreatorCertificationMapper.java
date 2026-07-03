package com.moyun.portal.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.moyun.portal.domain.entity.PortalCreatorCertification;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

/**
 * 创作者认证 Mapper
 *
 * @author moyun
 */
@Mapper
public interface PortalCreatorCertificationMapper extends BaseMapper<PortalCreatorCertification> {

    /**
     * 统计用户名下处于 pending 状态的认证申请数量
     */
    @Select("SELECT COUNT(*) FROM portal_creator_certification WHERE user_id = #{userId} AND status = 'pending'")
    int countPendingByUserId(@Param("userId") Long userId);

    /**
     * 查询用户最近一条认证记录（按 id 倒序，用于"我的认证状态"展示）
     */
    @Select("SELECT * FROM portal_creator_certification WHERE user_id = #{userId} ORDER BY id DESC LIMIT 1")
    PortalCreatorCertification selectLatestByUserId(@Param("userId") Long userId);

    /**
     * 统计用户名下已通过认证记录数量
     */
    @Select("SELECT COUNT(*) FROM portal_creator_certification WHERE user_id = #{userId} AND status = 'approved'")
    int countApprovedByUserId(@Param("userId") Long userId);
}
