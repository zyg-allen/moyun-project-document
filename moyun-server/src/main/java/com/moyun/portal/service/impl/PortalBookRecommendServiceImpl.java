package com.moyun.portal.service.impl;

import java.time.LocalDateTime;
import java.util.List;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.moyun.portal.domain.entity.PortalBookRecommend;
import com.moyun.portal.domain.query.BookRecommendQuery;
import com.moyun.portal.mapper.PortalBookRecommendMapper;
import com.moyun.portal.service.IPortalBookRecommendService;

/**
 * 书籍推荐 业务层实现
 *
 * @author moyun
 */
@Service
public class PortalBookRecommendServiceImpl
        extends ServiceImpl<PortalBookRecommendMapper, PortalBookRecommend>
        implements IPortalBookRecommendService {

    @Autowired
    private PortalBookRecommendMapper bookRecommendMapper;

    @Override
    public Page<PortalBookRecommend> selectBookRecommendPage(Page<PortalBookRecommend> page, BookRecommendQuery query) {
        return bookRecommendMapper.selectBookRecommendPage(page, query);
    }

    @Override
    public List<PortalBookRecommend> selectBookRecommendList(BookRecommendQuery query) {
        return bookRecommendMapper.selectBookRecommendList(query);
    }

    @Override
    public PortalBookRecommend selectBookRecommendById(Long id) {
        return bookRecommendMapper.selectBookRecommendById(id);
    }

    @Override
    public int insertBookRecommend(PortalBookRecommend bookRecommend) {
        if (bookRecommend.getCreateTime() == null) {
            bookRecommend.setCreateTime(LocalDateTime.now());
        }
        if (bookRecommend.getSort() == null) {
            bookRecommend.setSort(0);
        }
        if (bookRecommend.getIsActive() == null) {
            bookRecommend.setIsActive(true);
        }
        return bookRecommendMapper.insertBookRecommend(bookRecommend);
    }

    @Override
    public int updateBookRecommend(PortalBookRecommend bookRecommend) {
        bookRecommend.setUpdateTime(LocalDateTime.now());
        return bookRecommendMapper.updateBookRecommend(bookRecommend);
    }

    @Override
    public int deleteBookRecommendById(Long id) {
        return bookRecommendMapper.deleteBookRecommendById(id);
    }

    @Override
    public int deleteBookRecommendByIds(Long[] ids) {
        return bookRecommendMapper.deleteBookRecommendByIds(ids);
    }

    @Override
    public List<PortalBookRecommend> selectActiveByPosition(String position) {
        return bookRecommendMapper.selectActiveByPosition(position);
    }

    @Override
    public int batchUpdateSort(List<PortalBookRecommend> list) {
        return bookRecommendMapper.batchUpdateSort(list);
    }

    @Override
    public int toggleActive(Long id, Boolean isActive) {
        return bookRecommendMapper.toggleActive(id, isActive);
    }
}
