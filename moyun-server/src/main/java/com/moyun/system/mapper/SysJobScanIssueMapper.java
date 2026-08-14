package com.moyun.system.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;

import com.moyun.system.domain.entity.SysJobScanIssue;

/**
 * 定时任务扫描结果表 sys_job_scan_issue 数据层（v8.1）
 *
 * @author moyun
 */
@Mapper
public interface SysJobScanIssueMapper extends BaseMapper<SysJobScanIssue> {
}
