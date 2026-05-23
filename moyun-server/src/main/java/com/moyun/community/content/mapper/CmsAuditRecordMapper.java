package com.moyun.community.content.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.moyun.community.content.entity.CmsAuditRecord;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface CmsAuditRecordMapper extends BaseMapper<CmsAuditRecord> {

    List<CmsAuditRecord> selectAuditList(@Param("audit") CmsAuditRecord audit);

    List<CmsAuditRecord> selectAuditByArticleId(@Param("articleId") Long articleId);
}
