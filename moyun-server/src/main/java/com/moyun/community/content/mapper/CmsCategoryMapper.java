package com.moyun.community.content.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.moyun.community.content.entity.CmsCategory;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface CmsCategoryMapper extends BaseMapper<CmsCategory> {

    List<CmsCategory> selectCategoryList(@Param("category") CmsCategory category);

    List<CmsCategory> selectCategoryByType(@Param("categoryType") String categoryType);

    List<CmsCategory> selectShowCategoryList();

    List<CmsCategory> selectChildrenCategory(@Param("parentId") Long parentId);
}
