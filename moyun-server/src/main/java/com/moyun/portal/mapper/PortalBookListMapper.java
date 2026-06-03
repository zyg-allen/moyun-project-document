package com.moyun.portal.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.moyun.portal.domain.entity.PortalBookList;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

/**
 * 门户书单表 数据层
 *
 * @author moyun
 */
@Mapper
public interface PortalBookListMapper extends BaseMapper<PortalBookList>
{
    /**
     * 根据条件分页查询书单列表
     *
     * @param portalBookList 书单信息
     * @return 书单信息集合信息
     */
    public List<PortalBookList> selectPortalBookListList(PortalBookList portalBookList);

    /**
     * 通过书单ID查询书单
     *
     * @param id 书单ID
     * @return 书单对象信息
     */
    public PortalBookList selectPortalBookListById(Long id);

    /**
     * 新增书单信息
     *
     * @param portalBookList 书单信息
     * @return 结果
     */
    public int insertPortalBookList(PortalBookList portalBookList);

    /**
     * 修改书单信息
     *
     * @param portalBookList 书单信息
     * @return 结果
     */
    public int updatePortalBookList(PortalBookList portalBookList);

    /**
     * 通过书单ID删除书单
     *
     * @param id 书单ID
     * @return 结果
     */
    public int deletePortalBookListById(Long id);

    /**
     * 批量删除书单信息
     *
     * @param ids 需要删除的书单ID
     * @return 结果
     */
    public int deletePortalBookListByIds(Long[] ids);
}