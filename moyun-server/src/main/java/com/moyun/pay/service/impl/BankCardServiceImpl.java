package com.moyun.pay.service.impl;

import com.moyun.util.crypto.AesGcmUtils;
import com.moyun.util.string.IdCardUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.moyun.pay.config.PayProperties;
import com.moyun.pay.domain.entity.UserBankCard;
import com.moyun.pay.mapper.UserBankCardMapper;
import com.moyun.pay.service.IBankCardService;
import com.moyun.portal.service.realname.FourElementsRealNameVerifier;
import com.moyun.portal.service.realname.RealNameVerifyResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 用户银行卡服务实现
 *
 * <p>安全红线：
 * <ul>
 *   <li>卡号/手机号 AES-GCM 加密落库，密钥来自 moyun.pay.security.bank-card-encrypt-key</li>
 *   <li>身份证号仅四要素核验过程内存使用，不落库、不落日志</li>
 *   <li>对外实体必须剥离 cardNoEncrypted/phoneEncrypted（Controller 层执行）</li>
 *   <li>仅支持借记卡（DEBIT）</li>
 *   <li>卡数上限（默认 5 张）+ 同用户同卡防重</li>
 * </ul>
 *
 * <p>四要素实名核验（姓名+身份证+银行卡+手机号）：见
 * {@link FourElementsRealNameVerifier}，真实第三方核验点以
 * {@code todo：配置第三方：} 注释标识；通道未配置时绑定卡落
 * PENDING 转人工核实，不伪装核验通过。
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

    @Autowired
    private com.moyun.core.sms.SmsCodeService smsCodeService;

    @Autowired
    private FourElementsRealNameVerifier fourElementsRealNameVerifier;

    @Override
    public UserBankCard bind(Long userId, String holderName, String certNo, String cardNo, String phone,
                             String bankCode, String bankName, String smsCode) {
        if (holderName == null || holderName.isBlank()) {
            throw new IllegalArgumentException("持卡人姓名不能为空");
        }
        if (cardNo == null || !cardNo.matches("\\d{12,32}")) {
            throw new IllegalArgumentException("卡号格式不正确");
        }
        if (phone == null || !phone.matches("1\\d{10}")) {
            throw new IllegalArgumentException("手机号格式不正确");
        }
        // 身份证号本地校验（提供时才校验：四要素核验前置，非法直接拒绝，不浪费通道调用）
        if (certNo != null && !certNo.isBlank() && IdCardUtil.validate(certNo) != null) {
            throw new IllegalArgumentException("身份证号格式错误：" + IdCardUtil.validate(certNo));
        }
        // 短信验证码闭环（开关开启时强校验；一次性消费+防枚举见 SmsCodeServiceImpl）
        if (payProperties.getSecurity().isBankCardSmsVerify()) {
            if (smsCode == null || smsCode.isBlank()) {
                throw new IllegalArgumentException("请输入短信验证码");
            }
            if (!smsCodeService.verifyCode(phone, "bankcard", smsCode)) {
                throw new IllegalArgumentException("短信验证码错误或已过期");
            }
        }

        String encryptKey = payProperties.getSecurity().getBankCardEncryptKey();

        // 同卡防重：同一用户不可重复绑定同一张卡（密文不可比对的场景逐张解密比对）
        rejectDuplicatedCard(userId, cardNo, encryptKey);

        // 卡数上限
        Long count = bankCardMapper.selectCount(new LambdaQueryWrapper<UserBankCard>()
                .eq(UserBankCard::getUserId, userId));
        int max = payProperties.getSecurity().getBankCardMaxCount();
        if (count != null && count >= max) {
            throw new IllegalStateException("绑定银行卡已达上限（" + max + " 张）");
        }

        // 四要素实名核验（姓名+身份证+银行卡+手机号）：
        // - 核验通过 → VERIFIED
        // - 通道未开通/核验异常 → PENDING（人工核实兜底）
        // - 核验明确不一致 → REJECTED
        // - 未提供身份证号 → 无法发起四要素，落 PENDING 转人工
        String verifyStatus;
        if (certNo == null || certNo.isBlank()) {
            verifyStatus = "PENDING";
            log.warn("[bank-card] 未提供身份证号，四要素核验未发起，落 PENDING 转人工 userId={} cardNo={}",
                    userId, maskCardNo(cardNo));
        } else {
            RealNameVerifyResult verifyResult = fourElementsRealNameVerifier.verify(
                    userId, holderName, certNo, cardNo, phone);
            if (verifyResult.isSuccess()) {
                verifyStatus = "VERIFIED";
            } else if (verifyResult.isChannelUnavailable()) {
                verifyStatus = "PENDING";
                log.warn("[bank-card] 实名通道未开通/未接入，绑定卡落 PENDING 转人工核实 userId={} cardNo={} reason={}",
                        userId, maskCardNo(cardNo), verifyResult.getMessage());
            } else {
                verifyStatus = "REJECTED";
                log.warn("[bank-card] 四要素核验未通过 userId={} cardNo={} reason={}",
                        userId, maskCardNo(cardNo), verifyResult.getMessage());
            }
        }

        UserBankCard card = new UserBankCard();
        card.setUserId(userId);
        card.setHolderName(holderName);
        card.setCardNoEncrypted(AesGcmUtils.encrypt(cardNo, encryptKey));
        card.setCardNoMasked(maskCardNo(cardNo));
        card.setPhoneEncrypted(AesGcmUtils.encrypt(phone, encryptKey));
        card.setBankCode(bankCode);
        card.setBankName(bankName);
        card.setVerifyStatus(verifyStatus);
        card.setIsDefault(count == null || count == 0 ? 1 : 0);
        card.setCreateTime(LocalDateTime.now());
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
    @Transactional(rollbackFor = Exception.class)
    public void setDefault(Long userId, Long cardId) {
        UserBankCard card = getOwnedCard(userId, cardId);
        // 先清空同用户默认标记，再置当前卡（两步写操作，同一事务保证一致性）
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
     * 同卡防重：解密本人已绑卡号逐一比对，重复直接拒绝。
     * <p>单张解密失败（历史密钥变更等）跳过该张继续比对，不阻断主流程。
     */
    private void rejectDuplicatedCard(Long userId, String cardNo, String encryptKey) {
        List<UserBankCard> existing = bankCardMapper.selectList(new LambdaQueryWrapper<UserBankCard>()
                .eq(UserBankCard::getUserId, userId));
        if (existing == null) {
            return;
        }
        for (UserBankCard item : existing) {
            try {
                if (cardNo.equals(AesGcmUtils.decrypt(item.getCardNoEncrypted(), encryptKey))) {
                    throw new IllegalArgumentException("该银行卡已绑定，请勿重复绑定");
                }
            } catch (IllegalArgumentException e) {
                throw e;
            } catch (Exception e) {
                log.warn("[bank-card] 防重比对解密失败，跳过该卡 cardId={} err={}", item.getId(), e.getMessage());
            }
        }
    }

    private String maskCardNo(String cardNo) {
        if (cardNo == null || cardNo.length() < 10) {
            return "****";
        }
        return cardNo.substring(0, 4) + " **** **** " + cardNo.substring(cardNo.length() - 4);
    }

}
