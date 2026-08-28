package com.moyun.pay.controller;


import com.moyun.core.base.AjaxResult;
import com.moyun.pay.domain.entity.UserBankCard;
import com.moyun.pay.service.IBankCardService;
import com.moyun.portal.util.PortalSecurityUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 门户银行卡控制器（V11.0）
 *
 * <p>安全红线：所有出参一律剥离密文字段（cardNoEncrypted/phoneEncrypted），
 * 仅下发脱敏 cardNoMasked。
 *
 * @author moyun
 */
@RestController
@RequestMapping("/portal/pay/bank-card")
public class PortalBankCardController {

    @Autowired
    private IBankCardService bankCardService;

    /** 绑定银行卡 */
    @PostMapping
    public AjaxResult bind(@RequestBody Map<String, String> body) {
        Long userId = PortalSecurityUtils.getUserId();
        UserBankCard card = bankCardService.bind(userId,
                body.get("holderName"), body.get("cardNo"), body.get("phone"),
                body.get("bankCode"), body.get("bankName"), body.get("smsCode"));
        return AjaxResult.success(stripEncrypted(card));
    }

    /** 我的银行卡列表 */
    @GetMapping("/list")
    public AjaxResult list() {
        Long userId = PortalSecurityUtils.getUserId();
        List<UserBankCard> cards = bankCardService.listByUser(userId);
        List<Map<String, Object>> safeCards = new java.util.ArrayList<>();
        for (UserBankCard card : cards) {
            safeCards.add(stripEncrypted(card));
        }
        Map<String, Object> data = new HashMap<>();
        data.put("records", safeCards);
        data.put("total", safeCards.size());
        return AjaxResult.success(data);
    }

    /** 删除银行卡 */
    @DeleteMapping("/{cardId}")
    public AjaxResult remove(@PathVariable Long cardId) {
        Long userId = PortalSecurityUtils.getUserId();
        bankCardService.remove(userId, cardId);
        return AjaxResult.success("删除成功");
    }

    /** 设为默认卡 */
    @PutMapping("/{cardId}/default")
    public AjaxResult setDefault(@PathVariable Long cardId) {
        Long userId = PortalSecurityUtils.getUserId();
        bankCardService.setDefault(userId, cardId);
        return AjaxResult.success("设置成功");
    }

    /** 剥离密文，只保留脱敏字段 */
    private Map<String, Object> stripEncrypted(UserBankCard card) {
        Map<String, Object> safe = new HashMap<>();
        safe.put("id", card.getId());
        safe.put("holderName", card.getHolderName());
        safe.put("cardNoMasked", card.getCardNoMasked());
        safe.put("bankCode", card.getBankCode());
        safe.put("bankName", card.getBankName());
        safe.put("verifyStatus", card.getVerifyStatus());
        safe.put("isDefault", card.getIsDefault());
        return safe;
    }
}
