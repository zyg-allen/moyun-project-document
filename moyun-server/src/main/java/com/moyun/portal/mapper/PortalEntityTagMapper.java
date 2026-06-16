package com.moyun.portal.mapper;

import java.util.List;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import com.moyun.portal.domain.entity.PortalEntityTag;

@Mapper
public interface PortalEntityTagMapper extends BaseMapper<PortalEntityTag> {

    int insertBatch(@Param("list") List<PortalEntityTag> entityTags);

    int deleteByEntity(@Param("entityType") String entityType, @Param("entityId") Long entityId);

    List<PortalEntityTag> selectByEntity(@Param("entityType") String entityType, @Param("entityId") Long entityId);

    List<Long> selectTagIdsByEntity(@Param("entityType") String entityType, @Param("entityId") Long entityId);

    List<PortalEntityTag> selectByTagId(@Param("tagId") Long tagId);
}
