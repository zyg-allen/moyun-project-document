package com.moyun.community.content.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.moyun.community.content.entity.CmsLike;

import java.util.List;

public interface ICmsLikeService extends IService<CmsLike> {

    boolean toggleLike(Long userId, String targetType, Long targetId);

    List<CmsLike> selectLikeListByUser(Long userId);

    boolean isLiked(Long userId, String targetType, Long targetId);
}