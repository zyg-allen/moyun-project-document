package com.moyun.community.content.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.moyun.community.content.entity.CmsReport;
import com.moyun.community.content.mapper.CmsReportMapper;
import com.moyun.community.content.service.ICmsReportService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class CmsReportServiceImpl extends ServiceImpl<CmsReportMapper, CmsReport> implements ICmsReportService {

    @Override
    public List<CmsReport> selectPendingReportList() {
        return baseMapper.selectPendingReportList();
    }

    @Override
    @Transactional
    public boolean addReport(CmsReport report) {
        report.setStatus("pending");
        return save(report);
    }

    @Override
    @Transactional
    public boolean handleReport(Long reportId, String status, String handleResult, Long handlerId) {
        return baseMapper.updateReportStatus(reportId, status, handlerId, handleResult) > 0;
    }
}