package com.moyun.community.content.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.moyun.community.content.entity.CmsLike;
import com.moyun.community.content.mapper.CmsLikeMapper;
import com.moyun.community.content.service.ICmsLikeService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class CmsLikeServiceImpl extends ServiceImpl<CmsLikeMapper, CmsLike> implements ICmsLikeService {

    @Override
    @Transactional
    public boolean toggleLike(Long userId, String targetType, Long targetId) {
        CmsLike existingLike = baseMapper.selectByUserAndTarget(userId, targetType, targetId);

        if (existingLike != null) {
            if (existingLike.getStatus() == 1) {
                existingLike.setStatus(0);
                baseMapper.updateStatus(existingLike.getId(), 0);
            } else {
                existingLike.setStatus(1);
                baseMapper.updateStatus(existingLike.getId(), 1);
            }
            return true;
        } else {
            CmsLike newLike = new CmsLike();
            newLike.setUserId(userId);
            newLike.setTargetType(targetType);
            newLike.setTargetId(targetId);
            newLike.setStatus(1);
            return save(newLike);
        }
    }

    @Override
    public List<CmsLike> selectLikeListByUser(Long userId) {
        LambdaQueryWrapper<CmsLike> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(CmsLike::getUserId, userId)
               .eq(CmsLike::getStatus, 1)
               .orderByDesc(CmsLike::getCreateTime);
        return list(wrapper);
    }

    @Override
    public boolean isLiked(Long userId, String targetType, Long targetId) {
        CmsLike like = baseMapper.selectByUserAndTarget(userId, targetType, targetId);
        return like != null && like.getStatus() == 1;
    }
}