package com.moyun.community.content.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.moyun.community.content.entity.AuditRecord;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface AuditRecordMapper extends BaseMapper<AuditRecord> {

    @Select("SELECT * FROM cms_audit_record WHERE article_id = #{articleId} ORDER BY create_time DESC LIMIT 1")
    AuditRecord selectLatestByArticleId(@Param("articleId") Long articleId);

    @Select("SELECT * FROM cms_audit_record WHERE auditor_id = #{auditorId} ORDER BY audit_time DESC")
    List<AuditRecord> selectByAuditorId(@Param("auditorId") Long auditorId);

    @Select("SELECT * FROM cms_audit_record WHERE audit_status = #{auditStatus} ORDER BY create_time DESC")
    List<AuditRecord> selectByAuditStatus(@Param("auditStatus") String auditStatus);
}
