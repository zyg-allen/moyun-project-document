package com.moyun.ext.cms.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.moyun.portal.domain.entity.PortalWritingContest;

/**
 * CMS 创作挑战/征文活动管理 Service 接口
 *
 * 仅提供基础 CRUD，不接入评审流程。
 *
 * @author moyun
 */
public interface ICmsContestService {

    Page<PortalWritingContest> selectContestPage(Page<PortalWritingContest> page, PortalWritingContest contest);

    PortalWritingContest selectContestById(Long id);

    int insertContest(PortalWritingContest contest);

    int updateContest(PortalWritingContest contest);

    int deleteContestByIds(Long[] ids);
}
