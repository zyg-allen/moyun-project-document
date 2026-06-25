package com.moyun.portal.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.moyun.portal.domain.entity.PortalReport;
import org.apache.ibatis.annotations.Mapper;

/**
 * 举报记录 Mapper
 *
 * @author moyun
 */
@Mapper
public interface PortalReportMapper extends BaseMapper<PortalReport> {
}
