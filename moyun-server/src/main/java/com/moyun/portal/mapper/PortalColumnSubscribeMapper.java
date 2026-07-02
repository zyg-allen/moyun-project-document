package com.moyun.portal.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.moyun.portal.domain.entity.PortalColumnSubscribe;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

/**
 * 专栏订阅 Mapper
 *
 * @author moyun
 */
@Mapper
public interface PortalColumnSubscribeMapper extends BaseMapper<PortalColumnSubscribe> {

    /**
     * 查询用户对某专栏的订阅记录
     */
    @Select("SELECT * FROM portal_column_subscribe WHERE column_id = #{columnId} AND user_id = #{userId}")
    PortalColumnSubscribe selectByColumnAndUser(@Param("columnId") Long columnId, @Param("userId") Long userId);

    /**
     * 统计专栏订阅数
     */
    @Select("SELECT COUNT(*) FROM portal_column_subscribe WHERE column_id = #{columnId}")
    int countByColumn(@Param("columnId") Long columnId);
}
