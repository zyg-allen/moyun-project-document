package com.moyun.pay.service.impl;

import com.moyun.util.crypto.AesGcmUtils;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.moyun.pay.config.PayProperties;
import com.moyun.pay.domain.entity.UserBankCard;
import com.moyun.pay.mapper.UserBankCardMapper;
import com.moyun.pay.service.IBankCardService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 用户银行卡服务实现（V11.0）
 *
 * <p>安全红线：
 * <ul>
 *   <li>卡号/手机号 AES-GCM 加密落库，密钥来自 moyun.pay.security.bank-card-encrypt-key</li>
 *   <li>对外实体必须剥离 cardNoEncrypted/phoneEncrypted（Controller 层执行）</li>
 *   <li>仅支持借记卡（DEBIT）</li>
 *   <li>卡数上限（默认 5 张）</li>
 * </ul>
 *
 * @author moyun
 */
@Service
public class BankCardServiceImpl implements IBankCardService {

    private static final Logger log = LoggerFactory.getLogger(BankCardServiceImpl.class);

    @Autowired
    private UserBankCardMapper bankCardMapper;

    @Autowired
    private PayProperties payProperties;

    @Override
    public UserBankCard bind(Long userId, String holderName, String cardNo, String phone,
                             String bankCode, String bankName) {
        if (holderName == null || holderName.isBlank()) {
            throw new IllegalArgumentException("持卡人姓名不能为空");
        }
        if (cardNo == null || !cardNo.matches("\\d{12,32}")) {
            throw new IllegalArgumentException("卡号格式不正确");
        }
        if (phone == null || !phone.matches("1\\d{10}")) {
            throw new IllegalArgumentException("手机号格式不正确");
        }
        // 卡数上限
        Long count = bankCardMapper.selectCount(new LambdaQueryWrapper<UserBankCard>()
                .eq(UserBankCard::getUserId, userId));
        int max = payProperties.getSecurity().getBankCardMaxCount();
        if (count != null && count >= max) {
            throw new IllegalStateException("绑定银行卡已达上限（" + max + " 张）");
        }

        // 实名预校验（真实四要素/二要素 TODO；当前放行为 PENDING）
        boolean realNameOk = realNameChecker(holderName, cardNo);

        String encryptKey = payProperties.getSecurity().getBankCardEncryptKey();
        UserBankCard card = new UserBankCard();
        card.setUserId(userId);
        card.setHolderName(holderName);
        card.setCardNoEncrypted(AesGcmUtils.encrypt(cardNo, encryptKey));
        card.setCardNoMasked(maskCardNo(cardNo));
        card.setPhoneEncrypted(AesGcmUtils.encrypt(phone, encryptKey));
        card.setPhoneMasked(maskPhone(phone));
        card.setBankCode(bankCode);
        card.setBankName(bankName);
        card.setCardType("DEBIT");
        card.setVerifyStatus(realNameOk ? "VERIFIED" : "PENDING");
        card.setIsDefault(count == null || count == 0 ? 1 : 0);
        card.setCreateTime(LocalDateTime.now());
        card.setUpdateTime(LocalDateTime.now());
        bankCardMapper.insert(card);
        log.info("[bank-card] 绑卡成功 userId={} cardId={} masked={} verify={}",
                userId, card.getId(), card.getCardNoMasked(), card.getVerifyStatus());
        return card;
    }

    @Override
    public List<UserBankCard> listByUser(Long userId) {
        return bankCardMapper.selectList(new LambdaQueryWrapper<UserBankCard>()
                .eq(UserBankCard::getUserId, userId)
                .orderByDesc(UserBankCard::getIsDefault)
                .orderByDesc(UserBankCard::getId));
    }

    @Override
    public void remove(Long userId, Long cardId) {
        UserBankCard card = getOwnedCard(userId, cardId);
        bankCardMapper.deleteById(card.getId());
        log.info("[bank-card] 删卡 userId={} cardId={}", userId, cardId);
    }

    @Override
    public void setDefault(Long userId, Long cardId) {
        UserBankCard card = getOwnedCard(userId, cardId);
        // 先清空同用户默认标记，再置当前卡
        bankCardMapper.update(null, new com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper<UserBankCard>()
                .eq(UserBankCard::getUserId, userId)
                .set(UserBankCard::getIsDefault, 0)
                .set(UserBankCard::getUpdateTime, LocalDateTime.now()));
        bankCardMapper.update(null, new com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper<UserBankCard>()
                .eq(UserBankCard::getId, card.getId())
                .set(UserBankCard::getIsDefault, 1)
                .set(UserBankCard::getUpdateTime, LocalDateTime.now()));
        log.info("[bank-card] 设默认卡 userId={} cardId={}", userId, cardId);
    }

    private UserBankCard getOwnedCard(Long userId, Long cardId) {
        UserBankCard card = bankCardMapper.selectById(cardId);
        if (card == null || !userId.equals(card.getUserId())) {
            throw new IllegalArgumentException("银行卡不存在或无权操作");
        }
        return card;
    }

    /**
     * 实名校验（TODO 真实通道）：当前简单规则放行为 PENDING，由后台人工/通道后续核实
     */
    private boolean realNameChecker(String holderName, String cardNo) {
        // TODO(生产接入)：调用银行/三方实名验证通道（二要素：姓名+卡号），
        //   通过返回 true（VERIFIED）；失败返回 false（PENDING，转人工核实）。
        log.info("[bank-card] 实名校验 TODO 放行 holderName={} cardNo={}",
                holderName, maskCardNo(cardNo));
        return false;
    }

    private String maskCardNo(String cardNo) {
        if (cardNo == null || cardNo.length() < 10) {
            return "****";
        }
        return cardNo.substring(0, 4) + " **** **** " + cardNo.substring(cardNo.length() - 4);
    }

    private String maskPhone(String phone) {
        if (phone == null || phone.length() != 11) {
            return "****";
        }
        return phone.substring(0, 3) + "****" + phone.substring(7);
    }
}
