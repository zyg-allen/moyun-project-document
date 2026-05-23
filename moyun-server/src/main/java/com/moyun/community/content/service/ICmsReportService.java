package com.moyun.community.content.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.moyun.community.content.entity.CmsReport;

import java.util.List;

public interface ICmsReportService extends IService<CmsReport> {

    List<CmsReport> selectPendingReportList();

    boolean addReport(CmsReport report);

    boolean handleReport(Long reportId, String status, String handleResult, Long handlerId);
}