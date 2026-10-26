package com.moyun.ledger.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.moyun.ledger.domain.entity.LedgerProductCategory;
import com.moyun.ledger.domain.vo.CmsChannelVO;

import java.util.List;

/**
 * 记账系统预设维护服务（后台专用）
 *
 * <p>覆盖"有表但此前无后台入口"的三类预设：
 * 商品类目（两级树）、售卖渠道、进货渠道。</p>
 *
 * <p>与门户端接口完全独立：本服务只服务管理端，避免后台与 App 的
 * 读写口径、归属校验、删除红线相互耦合。</p>
 *
 * @author moyun
 */
public interface ICmsLedgerPresetService {

    // ---------------- 商品类目 ----------------

    Page<LedgerProductCategory> pageProductCategory(int pageNum, int pageSize,
                                                    String name, String owner, Integer status);

    /** 回填二级类目与引用商品数（父类目含子类目汇总） */
    void fillProductCategoryStats(List<LedgerProductCategory> roots);

    void createProductCategory(LedgerProductCategory bo);

    void updateProductCategory(Long id, LedgerProductCategory bo);

    void changeProductCategoryStatus(Long id, Integer status);

    void deleteProductCategory(Long id);

    // ---------------- 渠道（售卖 / 进货） ----------------

    Page<CmsChannelVO> pageChannel(String kind, int pageNum, int pageSize,
                                   String name, String owner, Integer status);

    void createChannel(String kind, CmsChannelVO bo);

    void updateChannel(String kind, Long id, CmsChannelVO bo);

    void changeChannelStatus(String kind, Long id, Integer status);

    void deleteChannel(String kind, Long id);
}
