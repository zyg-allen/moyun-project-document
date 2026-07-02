package com.moyun.portal.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;

import com.moyun.portal.domain.entity.PortalBookListLike;

/**
 * 书单点赞表 数据层
 *
 * @author moyun
 */
@Mapper
public interface PortalBookListLikeMapper extends BaseMapper<PortalBookListLike>
{
}
