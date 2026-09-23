package com.moyun.system.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.moyun.system.domain.entity.SysConfigLog;
import org.apache.ibatis.annotations.Mapper;

/**
 * 参数配置变更日志 Mapper
 *
 * @author moyun
 */
@Mapper
public interface SysConfigLogMapper extends BaseMapper<SysConfigLog> {
}
