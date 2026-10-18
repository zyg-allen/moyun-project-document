package com.moyun.ledger.service;

import com.moyun.ledger.domain.entity.LedgerShelf;

import java.util.List;
import java.util.Map;

/**
 * 货架服务：仓库下的存放位置（区/排/层/位）+ 归属 + 图片。
 *
 * <p>库存按货架拆分（ledger_stock.shelf_id），所以货架不是纯展示信息：
 * 删除前必须确认该货架已无库存，否则货会「消失」。</p>
 *
 * @author moyun
 */
public interface ILedgerShelfService {

    /** 货架列表（含商品种类数 / 库存件数 / 成本价值聚合） */
    List<LedgerShelf> listShelves(Long userId, Long warehouseId);

    /** 新增货架（同仓内 code 唯一，locationText 后端拼装） */
    Long createShelf(Long userId, LedgerShelf bo);

    /** 修改货架 */
    void updateShelf(Long userId, Long id, LedgerShelf bo);

    /** 删除货架（仍有库存时拒绝） */
    void deleteShelf(Long userId, Long id);

    /** 货架详情：基本信息 + 该货架上的商品明细（含图片由 Controller 补） */
    Map<String, Object> shelfDetail(Long userId, Long id);
}
