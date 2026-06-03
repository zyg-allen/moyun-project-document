package com.moyun.portal.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.moyun.portal.domain.entity.PortalBook;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

/**
 * 门户书籍表 数据层
 *
 * @author moyun
 */
@Mapper
public interface PortalBookMapper extends BaseMapper<PortalBook>
{
    /**
     * 根据条件分页查询书籍列表
     *
     * @param portalBook 书籍信息
     * @return 书籍信息集合信息
     */
    public List<PortalBook> selectPortalBookList(PortalBook portalBook);

    /**
     * 通过书籍ID查询书籍
     *
     * @param id 书籍ID
     * @return 书籍对象信息
     */
    public PortalBook selectPortalBookById(Long id);

    /**
     * 新增书籍信息
     *
     * @param portalBook 书籍信息
     * @return 结果
     */
    public int insertPortalBook(PortalBook portalBook);

    /**
     * 修改书籍信息
     *
     * @param portalBook 书籍信息
     * @return 结果
     */
    public int updatePortalBook(PortalBook portalBook);

    /**
     * 通过书籍ID删除书籍
     *
     * @param id 书籍ID
     * @return 结果
     */
    public int deletePortalBookById(Long id);

    /**
     * 批量删除书籍信息
     *
     * @param ids 需要删除的书籍ID
     * @return 结果
     */
    public int deletePortalBookByIds(Long[] ids);
}