package com.moyun.system.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.moyun.system.domain.entity.SysSensitiveWordLog;
import org.apache.ibatis.annotations.Mapper;

/**
 * 敏感词命中记录 Mapper
 *
 * @author moyun
 */
@Mapper
public interface SysSensitiveWordLogMapper extends BaseMapper<SysSensitiveWordLog> {
}
