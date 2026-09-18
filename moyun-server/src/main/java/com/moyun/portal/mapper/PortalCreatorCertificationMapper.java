package com.moyun.portal.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.moyun.portal.domain.entity.PortalCreatorCertification;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

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

    int countPendingByUserId(@Param("userId") Long userId);

    /**
     * 查询用户最近一条认证记录（按 id 倒序，用于"我的认证状态"展示）
     */

    PortalCreatorCertification selectLatestByUserId(@Param("userId") Long userId);

    /**
     * 统计用户名下已通过认证记录数量
     */

    int countApprovedByUserId(@Param("userId") Long userId);
}
