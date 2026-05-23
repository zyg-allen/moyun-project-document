package com.moyun.community.content.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import java.util.Date;

@Data
@TableName("cms_article_tag")
public class CmsArticleTag {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long articleId;

    private Long tagId;

    private Date createTime;
}
