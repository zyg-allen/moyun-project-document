package com.moyun.community.content.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.moyun.community.content.entity.CmsArticle;
import com.moyun.community.content.entity.CmsCollect;

import java.util.List;

public interface ICmsCollectService extends IService<CmsCollect> {

    boolean toggleCollect(Long userId, Long articleId);

    List<CmsArticle> selectCollectedArticles(Long userId);

    boolean isCollected(Long userId, Long articleId);
}