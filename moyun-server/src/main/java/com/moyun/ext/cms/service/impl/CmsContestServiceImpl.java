package com.moyun.ext.cms.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.moyun.ext.cms.service.ICmsContestService;
import com.moyun.portal.domain.entity.PortalWritingContest;
import com.moyun.portal.mapper.PortalWritingContestMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Arrays;

/**
 * CMS 创作挑战/征文活动管理 Service 实现
 *
 * @author moyun
 */
@Service
public class CmsContestServiceImpl implements ICmsContestService {

    @Autowired
    private PortalWritingContestMapper contestMapper;

    @Override
    public Page<PortalWritingContest> selectContestPage(Page<PortalWritingContest> page, PortalWritingContest contest) {
        LambdaQueryWrapper<PortalWritingContest> wrapper = new LambdaQueryWrapper<>();
        if (contest != null) {
            if (contest.getTitle() != null && !contest.getTitle().isEmpty()) {
                wrapper.like(PortalWritingContest::getTitle, contest.getTitle());
            }
            if (contest.getStatus() != null && !contest.getStatus().isEmpty()) {
                wrapper.eq(PortalWritingContest::getStatus, contest.getStatus());
            }
        }
        wrapper.orderByDesc(PortalWritingContest::getCreatedTime);
        return contestMapper.selectPage(page, wrapper);
    }

    @Override
    public PortalWritingContest selectContestById(Long id) {
        return contestMapper.selectById(id);
    }

    @Override
    public int insertContest(PortalWritingContest contest) {
        return contestMapper.insert(contest);
    }

    @Override
    public int updateContest(PortalWritingContest contest) {
        return contestMapper.updateById(contest);
    }

    @Override
    public int deleteContestByIds(Long[] ids) {
        return contestMapper.deleteBatchIds(Arrays.asList(ids));
    }
}
