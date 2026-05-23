package com.moyun.community.content.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.moyun.community.content.entity.Category;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface CategoryMapper extends BaseMapper<Category> {

    @Select("SELECT * FROM cms_category WHERE parent_id = 0 AND status = '0' ORDER BY sort ASC")
    List<Category> selectRootCategories();

    @Select("SELECT * FROM cms_category WHERE parent_id = #{parentId} AND status = '0' ORDER BY sort ASC")
    List<Category> selectByParentId(Long parentId);

    @Select("SELECT * FROM cms_category WHERE status = '0' AND is_show = 1 ORDER BY sort ASC")
    List<Category> selectShowInHome();

    @Select("SELECT * FROM cms_category WHERE category_type = #{categoryType} AND status = '0' ORDER BY sort ASC")
    List<Category> selectByType(String categoryType);
}
