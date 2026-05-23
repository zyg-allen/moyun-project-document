package com.moyun.community.content.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.moyun.community.content.entity.CmsComment;

import java.util.List;

public interface ICmsCommentService extends IService<CmsComment> {

    List<CmsComment> selectCommentListByArticle(Long articleId);

    List<CmsComment> selectCommentListByUser(Long userId);

    boolean addComment(CmsComment comment);

    boolean replyComment(CmsComment comment);

    boolean deleteComment(Long commentId);

    boolean toggleLike(Long commentId);
}