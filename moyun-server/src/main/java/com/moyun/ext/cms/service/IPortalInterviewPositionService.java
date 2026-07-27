package com.moyun.ext.cms.service;

import com.moyun.portal.domain.entity.PortalInterviewPosition;

import java.util.List;

/**
 * 面试岗位字典 Service 接口
 *
 * @author moyun
 */
public interface IPortalInterviewPositionService {

    /**
     * 查询所有启用的岗位字典（按 sort 升序）
     */
    List<PortalInterviewPosition> listActivePositions();

    /**
     * 根据岗位名称精确查询（如 "Java后端工程师"）。
     * 前端 startMockInterview 时传 position 字符串，可能为岗位字典 name 或裸字符串。
     * 此方法用于"岗位名称 → 岗位字典"的反查。
     *
     * @return 岗位字典对象；不存在时返回 null
     */
    PortalInterviewPosition findByName(String name);

    /**
     * 根据岗位编码精确查询（如 "java_backend"）
     */
    PortalInterviewPosition findByCode(String code);
}
