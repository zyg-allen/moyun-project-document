package com.moyun.portal.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.moyun.portal.domain.entity.PortalInterviewResumeTemplateLike;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

/**
 * 简历模板点赞 Mapper
 *
 * @author moyun
 */
@Mapper
public interface PortalInterviewResumeTemplateLikeMapper extends BaseMapper<PortalInterviewResumeTemplateLike> {

    /**
     * 查询用户是否已点赞某简历模板
     */
    PortalInterviewResumeTemplateLike selectLike(@Param("templateId") Long templateId, @Param("userId") Long userId);
}
