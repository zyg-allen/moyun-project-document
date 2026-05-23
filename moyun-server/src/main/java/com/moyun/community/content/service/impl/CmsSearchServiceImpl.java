package com.moyun.community.content.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.moyun.community.content.entity.CmsArticle;
import com.moyun.community.content.entity.CmsSearchHistory;
import com.moyun.community.content.entity.CmsSearchHotword;
import com.moyun.community.content.mapper.CmsArticleMapper;
import com.moyun.community.content.mapper.CmsSearchHistoryMapper;
import com.moyun.community.content.mapper.CmsSearchHotwordMapper;
import com.moyun.community.content.service.ICmsSearchService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class CmsSearchServiceImpl extends ServiceImpl<CmsSearchHotwordMapper, CmsSearchHotword> implements ICmsSearchService {

    @Autowired
    private CmsSearchHotwordMapper hotwordMapper;

    @Autowired
    private CmsSearchHistoryMapper historyMapper;

    @Autowired
    private CmsArticleMapper articleMapper;

    @Override
    public List<CmsSearchHotword> selectHotwordList() {
        LambdaQueryWrapper<CmsSearchHotword> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(CmsSearchHotword::getStatus, "0")
                .orderByAsc(CmsSearchHotword::getSort)
                .last("LIMIT 10");
        return hotwordMapper.selectList(wrapper);
    }

    @Override
    public List<CmsArticle> searchArticles(String keyword, Long categoryId) {
        LambdaQueryWrapper<CmsArticle> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(CmsArticle::getStatus, "published")
                .like(keyword != null, CmsArticle::getTitle, keyword)
                .or()
                .like(keyword != null, CmsArticle::getSummary, keyword)
                .eq(categoryId != null, CmsArticle::getCategoryId, categoryId)
                .orderByDesc(CmsArticle::getPublishTime);
        return articleMapper.selectList(wrapper);
    }

    @Override
    public List<CmsSearchHistory> selectSearchHistoryList(Long userId) {
        return historyMapper.selectSearchHistoryList(userId);
    }

    @Override
    @Transactional
    public boolean addSearchHistory(CmsSearchHistory history) {
        return historyMapper.insert(history) > 0;
    }

    @Override
    @Transactional
    public boolean deleteUserSearchHistory(Long userId) {
        return historyMapper.deleteUserSearchHistory(userId) > 0;
    }

    @Override
    @Transactional
    public boolean incrementSearchCount(Long hotwordId) {
        return hotwordMapper.incrementSearchCount(hotwordId) > 0;
    }
}