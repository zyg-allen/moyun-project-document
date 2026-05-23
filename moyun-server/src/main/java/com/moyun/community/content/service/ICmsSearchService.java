package com.moyun.community.content.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.moyun.community.content.entity.CmsArticle;
import com.moyun.community.content.entity.CmsSearchHotword;
import com.moyun.community.content.entity.CmsSearchHistory;

import java.util.List;

public interface ICmsSearchService extends IService<CmsSearchHotword> {

    List<CmsSearchHotword> selectHotwordList();

    List<CmsArticle> searchArticles(String keyword, Long categoryId);

    List<CmsSearchHistory> selectSearchHistoryList(Long userId);

    boolean addSearchHistory(CmsSearchHistory history);

    boolean deleteUserSearchHistory(Long userId);

    boolean incrementSearchCount(Long hotwordId);
}