package com.moyun.portal.util;

import com.moyun.portal.domain.entity.PortalArticle;
import com.moyun.portal.domain.vo.ArticleVO;
import org.springframework.beans.BeanUtils;

/**
 * 文章对象转换工具类
 *
 * @author moyun
 */
public class ArticleConvertUtil {

    public static ArticleVO toArticleVO(PortalArticle article) {
        if (article == null) {
            return null;
        }
        ArticleVO vo = new ArticleVO();
        BeanUtils.copyProperties(article, vo);
        // 确保createdAt与createTime保持一致
        vo.setCreatedAt(article.getCreateTime());
        // 为数值类型设置默认值，避免空指针
        if (vo.getViews() == null) vo.setViews(0L);
        if (vo.getLikes() == null) vo.setLikes(0L);
        if (vo.getComments() == null) vo.setComments(0L);
        if (vo.getShareCount() == null) vo.setShareCount(0L);
        if (vo.getBookmarkCount() == null) vo.setBookmarkCount(0L);
        return vo;
    }

    public static java.util.List<ArticleVO> toArticleVOList(java.util.List<PortalArticle> articles) {
        if (articles == null) {
            return java.util.Collections.emptyList();
        }
        java.util.List<ArticleVO> voList = new java.util.ArrayList<>();
        for (PortalArticle article : articles) {
            voList.add(toArticleVO(article));
        }
        return voList;
    }
}
