package com.moyun.ledger.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.moyun.common.exception.system.ServiceException;
import com.moyun.core.base.AjaxResult;
import com.moyun.ext.file.domain.entity.SysFile;
import com.moyun.ext.file.mapper.SysFileMapper;
import com.moyun.ext.file.service.ISysFileService;
import com.moyun.ledger.domain.entity.LedgerProduct;
import com.moyun.ledger.domain.entity.LedgerShelf;
import com.moyun.ledger.mapper.LedgerProductMapper;
import com.moyun.ledger.mapper.LedgerWarehouseMapper;
import com.moyun.ledger.service.ILedgerInventoryService;
import com.moyun.ledger.service.ILedgerShelfService;
import com.moyun.ledger.domain.entity.LedgerWarehouse;
import com.moyun.portal.util.PortalSecurityUtils;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 门户-仓库货架（存放位置）+ 图片 1:N。
 *
 * <p>图片不落本模块的表：统一写 {@code sys_file}
 * （business_type = ledger_shelf / ledger_product，business_id = 实体 id），
 * 按 id 升序首张即封面。上传走既有 {@code POST /portal/file/upload}，
 * 这里只负责「把已上传的 URL 挂到实体上」和「列出/删除」。</p>
 *
 * @author moyun
 */
@Tag(name = "门户-仓库货架")
@RestController
@RequestMapping("/portal/ledger/inventory")
public class PortalLedgerShelfController {

    @Autowired
    private ILedgerShelfService shelfService;

    @Autowired
    private ILedgerInventoryService inventoryService;

    @Autowired
    private SysFileMapper sysFileMapper;

    @Autowired
    private ISysFileService sysFileService;

    @Autowired
    private LedgerProductMapper productMapper;

    @Autowired
    private LedgerWarehouseMapper warehouseMapper;

    // ---------------- 货架 ----------------

    @Operation(summary = "货架列表（含商品数/件数/成本价值）")
    @GetMapping("/warehouses/{warehouseId}/shelves")
    public AjaxResult list(@PathVariable("warehouseId") Long warehouseId) {
        return AjaxResult.success(shelfService.listShelves(PortalSecurityUtils.getUserId(), warehouseId));
    }

    @Operation(summary = "新增货架")
    @PostMapping("/shelves")
    public AjaxResult create(@RequestBody LedgerShelf bo) {
        return AjaxResult.success(shelfService.createShelf(PortalSecurityUtils.getUserId(), bo));
    }

    @Operation(summary = "修改货架")
    @PutMapping("/shelves/{id}")
    public AjaxResult update(@PathVariable("id") Long id, @RequestBody LedgerShelf bo) {
        shelfService.updateShelf(PortalSecurityUtils.getUserId(), id, bo);
        return AjaxResult.success();
    }

    @Operation(summary = "删除货架（仍有库存时拒绝）")
    @DeleteMapping("/shelves/{id}")
    public AjaxResult remove(@PathVariable("id") Long id) {
        shelfService.deleteShelf(PortalSecurityUtils.getUserId(), id);
        return AjaxResult.success();
    }

    @Operation(summary = "货架详情（含商品明细 + 图片）")
    @GetMapping("/shelves/{id}")
    public AjaxResult detail(@PathVariable("id") Long id) {
        Long userId = PortalSecurityUtils.getUserId();
        Map<String, Object> data = shelfService.shelfDetail(userId, id);
        data.put("images", listImages(LedgerShelf.FILE_BIZ_TYPE, id));
        return AjaxResult.success(data);
    }

    // ---------------- 图片（1:N，走 sys_file） ----------------

    @Operation(summary = "货架图片列表")
    @GetMapping("/shelves/{id}/images")
    public AjaxResult shelfImages(@PathVariable("id") Long id) {
        return AjaxResult.success(listImages(LedgerShelf.FILE_BIZ_TYPE, id));
    }

    @Operation(summary = "商品详情（图片 + 各货架库存分布）")
    @GetMapping("/products/{id}")
    public AjaxResult productDetail(@PathVariable("id") Long id) {
        Long userId = PortalSecurityUtils.getUserId();
        Map<String, Object> data = inventoryService.productDetail(userId, id);
        data.put("images", listImages(LedgerShelf.PRODUCT_FILE_BIZ_TYPE, id));
        return AjaxResult.success(data);
    }

    @Operation(summary = "商品图片列表")
    @GetMapping("/products/{id}/images")
    public AjaxResult productImages(@PathVariable("id") Long id) {
        return AjaxResult.success(listImages(LedgerShelf.PRODUCT_FILE_BIZ_TYPE, id));
    }

    @Operation(summary = "给货架挂一张已上传的图片")
    @PostMapping("/shelves/{id}/images")
    public AjaxResult attachShelfImage(@PathVariable("id") Long id, @RequestBody Map<String, Object> body) {
        Long userId = PortalSecurityUtils.getUserId();
        shelfService.shelfDetail(userId, id); // 归属校验：货架必须属于当前用户
        return AjaxResult.success(attachImage(userId, LedgerShelf.FILE_BIZ_TYPE, id, body));
    }

    @Operation(summary = "给商品挂一张已上传的图片")
    @PostMapping("/products/{id}/images")
    public AjaxResult attachProductImage(@PathVariable("id") Long id, @RequestBody Map<String, Object> body) {
        Long userId = PortalSecurityUtils.getUserId();
        getOwnedProduct(userId, id); // 归属校验
        return AjaxResult.success(attachImage(userId, LedgerShelf.PRODUCT_FILE_BIZ_TYPE, id, body));
    }

    @Operation(summary = "删除图片（同时删存储与记录）")
    @DeleteMapping("/images/{fileId}")
    public AjaxResult removeImage(@PathVariable("fileId") Long fileId) {
        Long userId = PortalSecurityUtils.getUserId();
        SysFile f = sysFileMapper.selectById(fileId);
        if (f == null) {
            throw new ServiceException("图片不存在");
        }
        boolean mine = f.getUploadUserId() == null || userId.equals(f.getUploadUserId());
        if (!mine) {
            throw new ServiceException("无权删除他人上传的图片");
        }
        // 挂在本模块实体上的图片才允许在这里删，避免误删其它模块附件
        if (!LedgerShelf.FILE_BIZ_TYPE.equals(f.getBusinessType())
                && !LedgerShelf.PRODUCT_FILE_BIZ_TYPE.equals(f.getBusinessType())) {
            throw new ServiceException("该附件不属于仓库模块");
        }
        sysFileService.deleteFileById(fileId);
        return AjaxResult.success();
    }

    // ---------------- 内部 ----------------

    /** 列出某实体挂着的图片（按 id 升序，首张即封面） */
    private List<SysFile> listImages(String bizType, Long bizId) {
        return sysFileMapper.selectList(new LambdaQueryWrapper<SysFile>()
                .eq(SysFile::getBusinessType, bizType)
                .eq(SysFile::getBusinessId, String.valueOf(bizId))
                .orderByAsc(SysFile::getId));
    }

    /**
     * 把「已上传的图片 URL」挂到实体上。
     *
     * <p>兼容两种入参：{@code {url}} 或 {@code {fileUrl}}。
     * 若 url 对应不到 sys_file 记录（例如外部链接），则直接写一条新记录，
     * 保证列表能查到。</p>
     */
    private SysFile attachImage(Long userId, String bizType, Long bizId, Map<String, Object> body) {
        String url = str(body, "url");
        if (url == null) {
            url = str(body, "fileUrl");
        }
        if (url == null) {
            throw new ServiceException("请提供图片地址");
        }
        SysFile exist = sysFileMapper.selectOne(new LambdaQueryWrapper<SysFile>()
                .eq(SysFile::getFileUrl, url)
                .last("LIMIT 1"));
        if (exist == null) {
            SysFile f = new SysFile();
            f.setFileName(str(body, "fileName") == null ? "image" : str(body, "fileName"));
            f.setFileUrl(url);
            f.setBusinessType(bizType);
            f.setBusinessId(String.valueOf(bizId));
            f.setStatus("0");
            f.setUploadUserId(userId);
            sysFileMapper.insert(f);
            return f;
        }
        if (exist.getUploadUserId() != null && !userId.equals(exist.getUploadUserId())) {
            throw new ServiceException("无权使用他人上传的图片");
        }
        exist.setBusinessType(bizType);
        exist.setBusinessId(String.valueOf(bizId));
        sysFileMapper.updateById(exist);
        return exist;
    }

    private LedgerProduct getOwnedProduct(Long userId, Long productId) {
        LedgerProduct p = productMapper.selectById(productId);
        if (p == null) {
            throw new ServiceException("商品不存在");
        }
        LedgerWarehouse w = warehouseMapper.selectById(p.getWarehouseId());
        if (w == null || !userId.equals(w.getUserId())) {
            throw new ServiceException("商品不属于当前用户");
        }
        return p;
    }

    private String str(Map<String, Object> m, String k) {
        Object v = m.get(k);
        if (v == null) {
            return null;
        }
        String s = String.valueOf(v).trim();
        return s.isEmpty() ? null : s;
    }
}
