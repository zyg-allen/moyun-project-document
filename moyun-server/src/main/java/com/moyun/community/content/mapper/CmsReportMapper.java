package com.moyun.community.content.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.moyun.community.content.entity.CmsReport;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface CmsReportMapper extends BaseMapper<CmsReport> {

    List<CmsReport> selectReportList();

    List<CmsReport> selectPendingReportList();

    int updateReportStatus(@Param("reportId") Long reportId, @Param("status") String status,
                          @Param("handlerId") Long handlerId, @Param("handleResult") String handleResult);
}