package com.moyun.system.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.moyun.system.domain.entity.SysSensitiveWord;
import org.apache.ibatis.annotations.Mapper;

/**
 * 敏感词库 Mapper
 *
 * @author moyun
 */
@Mapper
public interface SysSensitiveWordMapper extends BaseMapper<SysSensitiveWord> {
}
