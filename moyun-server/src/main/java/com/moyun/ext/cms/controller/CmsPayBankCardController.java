package com.moyun.ext.cms.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.moyun.core.base.AjaxResult;
import com.moyun.core.base.BaseController;
import com.moyun.pay.domain.entity.UserBankCard;
import com.moyun.pay.mapper.UserBankCardMapper;
import com.moyun.util.bean.PageUtils;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * CMS 银行卡后台管理 Controller（V11.0，只读脱敏）
 *
 * <p>安全红线：后台同样禁止下发卡号/手机号密文，仅展示脱敏字段。
 *
 * @author moyun
 */
@Tag(name = "CMS银行卡管理", description = "用户提现银行卡只读管理（脱敏）")
@RestController
@RequestMapping("/cms/pay/bank-card")
public class CmsPayBankCardController extends BaseController {

    @Autowired
    private UserBankCardMapper bankCardMapper;

    @Operation(summary = "银行卡列表", description = "分页查询用户绑定银行卡（脱敏），支持用户ID/核验状态筛选")
    @PreAuthorize("@ss.hasPermi('cms:payBankCard:list')")
    @GetMapping("/list")
    public AjaxResult list(@RequestParam(required = false) Long userId,
                           @RequestParam(required = false) String verifyStatus) {
        Page<UserBankCard> page = PageUtils.startPage();
        bankCardMapper.selectPage(page, new LambdaQueryWrapper<UserBankCard>()
                .eq(userId != null, UserBankCard::getUserId, userId)
                .eq(verifyStatus != null && !verifyStatus.isBlank(), UserBankCard::getVerifyStatus, verifyStatus)
                .orderByDesc(UserBankCard::getId));
        List<Map<String, Object>> safeRecords = new ArrayList<>();
        for (UserBankCard card : page.getRecords()) {
            safeRecords.add(stripEncrypted(card));
        }
        Map<String, Object> data = new HashMap<>();
        data.put("records", safeRecords);
        data.put("total", page.getTotal());
        data.put("current", page.getCurrent());
        data.put("size", page.getSize());
        return success(data);
    }

    @Operation(summary = "银行卡详情（脱敏）", description = "按 ID 查询银行卡（脱敏）")
    @PreAuthorize("@ss.hasPermi('cms:payBankCard:query')")
    @GetMapping("/{cardId}")
    public AjaxResult detail(@PathVariable Long cardId) {
        UserBankCard card = bankCardMapper.selectById(cardId);
        if (card == null) {
            return error("银行卡不存在");
        }
        return success(stripEncrypted(card));
    }

    private Map<String, Object> stripEncrypted(UserBankCard card) {
        Map<String, Object> safe = new HashMap<>();
        safe.put("id", card.getId());
        safe.put("userId", card.getUserId());
        safe.put("holderName", card.getHolderName());
        safe.put("cardNoMasked", card.getCardNoMasked());
        safe.put("bankCode", card.getBankCode());
        safe.put("bankName", card.getBankName());
        safe.put("verifyStatus", card.getVerifyStatus());
        safe.put("isDefault", card.getIsDefault());
        safe.put("createTime", card.getCreateTime());
        return safe;
    }
}
