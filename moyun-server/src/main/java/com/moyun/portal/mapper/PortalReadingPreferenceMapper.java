package com.moyun.portal.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import com.moyun.portal.domain.entity.PortalReadingPreference;

/**
 * 用户阅读偏好 数据层
 *
 * @author moyun
 */
@Mapper
public interface PortalReadingPreferenceMapper extends BaseMapper<PortalReadingPreference> {

    /**
     * 按 user_id 查询偏好（每用户单条）
     */
    PortalReadingPreference selectByUserId(@Param("userId") Long userId);

    int insertPreference(PortalReadingPreference preference);

    int updatePreference(PortalReadingPreference preference);

    /**
     * upsert：存在则更新，不存在则插入
     */
    int upsertPreference(PortalReadingPreference preference);
}
