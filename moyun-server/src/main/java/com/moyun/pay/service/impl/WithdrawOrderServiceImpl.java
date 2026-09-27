package com.moyun.pay.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.moyun.core.mvc.handler.BusinessException;
import com.moyun.pay.channel.PayoutChannel;
import com.moyun.pay.channel.PayoutResult;
import com.moyun.pay.config.PayProperties;
import com.moyun.pay.domain.entity.LedgerEntry;
import com.moyun.pay.domain.entity.UserAccount;
import com.moyun.pay.domain.entity.UserBankCard;
import com.moyun.pay.domain.entity.WithdrawOrder;
import com.moyun.pay.mapper.LedgerEntryMapper;
import com.moyun.pay.mapper.UserBankCardMapper;
import com.moyun.pay.mapper.WithdrawOrderMapper;
import com.moyun.pay.service.IUserAccountService;
import com.moyun.pay.service.IWithdrawOrderService;
import com.moyun.portal.domain.entity.PortalUser;
import com.moyun.portal.mapper.PortalUserMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 提现单服务实现（提现闭环：申请冻结 → 审核 → 打款）
 *
 * <p>资金模型：公账商户号集中真钱，虚拟余额记账。申请即冻结
 * （可用余额 = balance - frozen_amount，原子 SQL 防并发超提）；
 * 审核通过事务内条件更新 auditing→paying（幂等）+ 原子扣减冻结
 * （balance/frozen/total_withdraw 三联动）+ 复式流水 debit + 代付渠道出金；
 * 出金由 {@code PayoutChannel} 实现承担（联调装配 {@code MockPayoutChannel}，生产接入真实实现）；
 * **无可用渠道 Bean 时 fail-closed 拒绝出金并回滚整个审核事务**，不做隐式模拟打款。
 *
 * @author moyun
 */
@Service
public class WithdrawOrderServiceImpl extends ServiceImpl<WithdrawOrderMapper, WithdrawOrder>
        implements IWithdrawOrderService {

    private static final Logger log = LoggerFactory.getLogger(WithdrawOrderServiceImpl.class);
    private static final SecureRandom RANDOM = new SecureRandom();

    @Autowired
    private IUserAccountService userAccountService;

    @Autowired
    private UserBankCardMapper bankCardMapper;

    @Autowired
    private LedgerEntryMapper ledgerEntryMapper;

    @Autowired
    private PortalUserMapper portalUserMapper;

    @Autowired
    private PayProperties payProperties;

    /**
     * 代付渠道实现（Spring 按 {@code moyun.pay.payout.mock-enabled} 条件装配）。
     *
     * <p>联调环境装配 MockPayoutChannel；接入真实通道后新增实现并在 mock 关闭时装配；
     * 两者都不满足时该集合为空 → 出金明确拒绝（不隐式模拟）。</p>
     */
    @Autowired
    private List<PayoutChannel> payoutChannels;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public WithdrawOrder apply(Long userId, BigDecimal amount, Long bankCardId) {
        if (userId == null) {
            throw new BusinessException("USER_NOT_LOGIN", "请先登录");
        }
        if (amount == null || amount.compareTo(new BigDecimal("1")) < 0) {
            throw new BusinessException("WITHDRAW_AMOUNT_INVALID", "提现金额不可低于 1 元");
        }
        if (amount.compareTo(new BigDecimal("50000")) > 0) {
            throw new BusinessException("WITHDRAW_AMOUNT_INVALID", "单笔提现不可超过 50000 元");
        }
        // 1. 绑卡校验：必须为本人已核实卡
        UserBankCard card = bankCardId == null ? null : bankCardMapper.selectById(bankCardId);
        if (card == null || !Objects.equals(card.getUserId(), userId)) {
            throw new BusinessException("WITHDRAW_CARD_INVALID", "请选择本人绑定的银行卡");
        }
        if (!"VERIFIED".equals(card.getVerifyStatus())) {
            throw new BusinessException("WITHDRAW_CARD_NOT_VERIFIED", "该银行卡尚未核实通过");
        }
        // 2. 可用余额校验（可用 = balance - frozen_amount，扣除审核中单据占用的冻结金额）
        UserAccount account = userAccountService.getOrCreate(userId);
        BigDecimal frozen = account.getFrozenAmount() == null ? BigDecimal.ZERO : account.getFrozenAmount();
        BigDecimal available = account.getBalance().subtract(frozen);
        if (available.compareTo(amount) < 0) {
            throw new BusinessException("WITHDRAW_BALANCE_INSUFFICIENT",
                    "可提现余额不足（可用 " + available + " 元，含审核中占用 " + frozen + " 元）");
        }

        // 3. 原子冻结（条件：balance - frozen_amount >= amount，防并发重复申请超提；失败即拒绝）
        if (!userAccountService.freeze(userId, amount)) {
            throw new BusinessException("WITHDRAW_BALANCE_INSUFFICIENT", "可提现余额不足（并发变动），请稍后重试");
        }

        // 4. 落 auditing 提现单（手续费 0，后续可配置）
        // 从用户记录获取注册来源端
        PortalUser portalUser = portalUserMapper.selectById(userId);
        String platformCode = portalUser != null ? portalUser.getPlatformCode() : null;
        WithdrawOrder order = new WithdrawOrder();
        order.setWithdrawNo(generateWithdrawNo());
        order.setUserId(userId);
        order.setAmount(amount);
        order.setFee(BigDecimal.ZERO);
        order.setBankCardId(bankCardId);
        order.setPlatformCode(platformCode);
        order.setStatus(WithdrawOrder.STATUS_AUDITING);
        order.setCreateTime(LocalDateTime.now());
        this.save(order);
        log.info("[withdraw] 用户{}发起提现 {}元（已冻结），单号{}", userId, amount, order.getWithdrawNo());
        return order;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void auditPass(Long withdrawId) {
        WithdrawOrder order = requireAuditing(withdrawId);
        // 1. 条件更新 auditing → paying（幂等：并发重复审核仅一次成功，0 行即已被处理）
        boolean advanced = this.update(new LambdaUpdateWrapper<WithdrawOrder>()
                .eq(WithdrawOrder::getId, withdrawId)
                .eq(WithdrawOrder::getStatus, WithdrawOrder.STATUS_AUDITING)
                .set(WithdrawOrder::getStatus, WithdrawOrder.STATUS_PAYING)
                .set(WithdrawOrder::getAuditTime, LocalDateTime.now()));
        if (!advanced) {
            throw new BusinessException("WITHDRAW_STATUS_INVALID", "提现单已处理（当前状态：" + order.getStatus() + "）");
        }

        // 2. 原子扣减冻结（balance -= amount、frozen -= amount、totalWithdraw += amount 三联动；
        //    申请时已冻结，此处条件不满足即为数据异常，回滚后单据回到 auditing 重新审核）
        boolean debited = userAccountService.debitFrozen(order.getUserId(), order.getAmount());
        if (!debited) {
            throw new BusinessException("WITHDRAW_BALANCE_INSUFFICIENT",
                    "冻结金额与单据不一致（并发变动），已回滚，请核对后重新审核");
        }

        // 3. 写资金流水（USER/debit，payNo=withdrawNo，balanceAfter 取扣减后账户）
        UserAccount account = userAccountService.getOrCreate(order.getUserId());
        LedgerEntry entry = new LedgerEntry();
        entry.setPayNo(order.getWithdrawNo());
        entry.setBizType("withdraw");
        entry.setBizNo(String.valueOf(order.getId()));
        entry.setAccountRole(LedgerEntry.ROLE_USER);
        entry.setUserId(order.getUserId());
        entry.setDirection(LedgerEntry.DIRECTION_DEBIT);
        entry.setAmount(order.getAmount());
        entry.setBalanceAfter(account.getBalance());
        entry.setSummary("提现出金-打款至绑定银行卡");
        entry.setPlatformCode(order.getPlatformCode());
        entry.setCreateTime(LocalDateTime.now());
        ledgerEntryMapper.insert(entry);

        // 4. 代付渠道出金（联调 mock / 生产真实 PayoutChannel）。
        //    受理失败或无可用渠道时 payout() 抛异常，使整个 auditPass 事务回滚
        //    （上面的余额扣减/冻结/流水一并退回）。
        payout(order);
    }

    /**
     * 代付通道出金（审核通过后、事务内调用）
     *
     * <p>口径与 {@code PayChannel} 对称：**出金由 {@link PayoutChannel} 实现承担**，
     * 本方法只负责"路由渠道 → 按结果推进状态"：</p>
     * <ul>
     *   <li>受理成功 → 单据推进 {@code paying → paid}（条件更新，幂等），记录通道流水号；</li>
     *   <li>受理失败 → 抛异常使整个审核事务回滚（余额/冻结/流水一并退回），单据回 {@code auditing}；</li>
     *   <li>**无可用渠道 Bean → 明确拒绝**，绝不做隐式模拟打款。</li>
     * </ul>
     *
     * <p><b>与历史实现的区别</b>：历史上"未配置通道即模拟打款成功"是写在业务方法里的**隐式降级**，
     * 生产漏配也会走到假打款。现改为渠道抽象 + 显式装配（{@code moyun.pay.payout.mock-enabled}）：
     * 联调环境装配 {@code MockPayoutChannel}（日志显式标注"资金未实际划出"），
     * 生产接入真实实现后替换即可，业务代码零改动。</p>
     */
    private void payout(WithdrawOrder order) {
        PayoutChannel channel = resolvePayoutChannel();
        UserBankCard card = bankCardMapper.selectById(order.getBankCardId());
        // 收款要素（服务端 AES-GCM 解密，仅打款组装使用，禁止外泄/落日志/回传前端）：
        // String cardNo = AesGcmUtils.decrypt(card.getCardNoEncrypted(),
        //         payProperties.getSecurity().getBankCardEncryptKey());

        // todo：配置第三方：代付通道（银行/三方代付）配置
        // 真实接入方式：新增 PayoutChannel 实现（通道商编号/网关地址/签名密钥或证书从配置注入），
        // 在 moyun.pay.payout.mock-enabled=false 时装配，即可自动替换本处路由到的渠道。实现要点：
        //   1. 组装代付请求：out_biz_no = withdrawNo（通道幂等键，重试防重复出金）、
        //      pay_amount = amount（元，按通道口径换算）、收款人 card.getHolderName()、
        //      收款卡号 cardNo（解密后）、银行编码 card.getBankCode()、异步通知地址（打款结果回调入口）
        //   2. 发起代付调用：HTTP(S) + 报文签名（RSA2/HMAC-SM3 等，按通道规范），
        //      设置连接/读超时（如 5s/15s），超时视为"受理未知"，以查单接口核对终态，禁止盲目重发
        //   3. 响应解析：受理成功 → PayoutResult.accepted(...)（本方法保持 paying，等异步回执推进 paid）；
        //      明确失败 → PayoutResult.failed(...)（本方法抛异常回滚）
        //   4. 打款结果回调：新增回调入口验签 → 条件更新 paying → paid（成功）/ paying → auditing
        //      并回补余额与流水（失败退回，需冲正 debit 流水）——与支付回调同幂等规范
        PayoutResult result = channel.pay(order.getWithdrawNo(), order.getAmount(),
                order.getBankCardId(), order.getUserId());
        if (!result.success()) {
            // 受理失败：抛异常整体回滚，避免"余额已扣、实际未出金"
            throw new BusinessException("WITHDRAW_PAYOUT_REJECTED",
                    "代付通道受理失败：" + result.message());
        }

        // 受理成功：paying → paid（条件更新，幂等）
        boolean rows = this.update(new LambdaUpdateWrapper<WithdrawOrder>()
                .eq(WithdrawOrder::getId, order.getId())
                .eq(WithdrawOrder::getStatus, WithdrawOrder.STATUS_PAYING)
                .set(WithdrawOrder::getStatus, WithdrawOrder.STATUS_PAID)
                .set(WithdrawOrder::getPaidTime, LocalDateTime.now()));
        if (!rows) {
            throw new BusinessException("WITHDRAW_STATUS_INVALID", "提现单打款状态推进失败：" + order.getWithdrawNo());
        }
        log.info("[withdraw] 出金受理完成，单号{} 用户{} 出金{}元 渠道={} 通道流水={} 卡={} 收款人={}",
                order.getWithdrawNo(), order.getUserId(), order.getAmount(),
                channel.channelCode(), result.channelOrderNo(),
                card == null ? "-" : card.getCardNoMasked(), card == null ? "-" : card.getHolderName());
    }

    /**
     * 选择代付渠道：Spring 按条件装配通常只有一个实现；
     * 若有多个（联调 mock + 真实通道并存）则按 {@code moyun.pay.payout.channel} 指定，缺省取第一个。
     *
     * <p>无可用渠道 → 明确拒绝出金（fail-closed），绝不隐式模拟打款。</p>
     */
    private PayoutChannel resolvePayoutChannel() {
        if (payoutChannels == null || payoutChannels.isEmpty()) {
            // 无渠道 Bean：未接入真实通道且 mock 已关闭
            throw new BusinessException("WITHDRAW_PAYOUT_DISABLED",
                    "代付通道未开通，无法出金（请接入真实代付通道，或联调环境开启 moyun.pay.payout.mock-enabled）");
        }
        String preferred = payProperties.getPayout() != null ? payProperties.getPayout().getChannel() : null;
        if (preferred != null && !preferred.isBlank()) {
            for (PayoutChannel c : payoutChannels) {
                if (preferred.equalsIgnoreCase(c.channelCode())) {
                    return c;
                }
            }
            log.warn("[withdraw] 指定代付渠道 {} 未装配，回退到可用渠道 {}",
                    preferred, payoutChannels.get(0).channelCode());
        }
        return payoutChannels.get(0);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void auditReject(Long withdrawId, String reason) {
        WithdrawOrder order = requireAuditing(withdrawId);
        String rejectReason = reason == null || reason.isBlank() ? "不符合提现条件" : reason;
        // 1. 条件更新 auditing → rejected（幂等：并发重复驳回仅一次成功）
        boolean rows = this.update(new LambdaUpdateWrapper<WithdrawOrder>()
                .eq(WithdrawOrder::getId, withdrawId)
                .eq(WithdrawOrder::getStatus, WithdrawOrder.STATUS_AUDITING)
                .set(WithdrawOrder::getStatus, WithdrawOrder.STATUS_REJECTED)
                .set(WithdrawOrder::getAuditTime, LocalDateTime.now())
                .set(WithdrawOrder::getRejectReason, rejectReason));
        if (!rows) {
            throw new BusinessException("WITHDRAW_STATUS_INVALID", "提现单已处理（当前状态：" + order.getStatus() + "）");
        }
        // 2. 解冻（frozen -= amount，金额回可用余额；失败即数据异常，回滚保持 auditing）
        if (!userAccountService.unfreeze(order.getUserId(), order.getAmount())) {
            throw new IllegalStateException("解冻失败（冻结金额与单据不一致）：" + order.getWithdrawNo());
        }
        log.info("[withdraw] 提现单{}被驳回（已解冻 {}元）：{}", order.getWithdrawNo(), order.getAmount(), rejectReason);
    }

    private WithdrawOrder requireAuditing(Long withdrawId) {
        WithdrawOrder order = this.getById(withdrawId);
        if (order == null) {
            throw new BusinessException("WITHDRAW_NOT_FOUND", "提现单不存在");
        }
        if (!WithdrawOrder.STATUS_AUDITING.equals(order.getStatus())) {
            throw new BusinessException("WITHDRAW_STATUS_INVALID", "提现单已处理（当前状态：" + order.getStatus() + "）");
        }
        return order;
    }

    @Override
    public IPage<WithdrawOrder> myWithdrawals(Long userId, long current, long size) {
        return this.page(new Page<>(current, size), new QueryWrapper<WithdrawOrder>()
                .eq("user_id", userId)
                .orderByDesc("id"));
    }

    @Override
    public Map<String, Object> adminList(String status, Long userId, String platformCode, long current, long size) {
        Page<WithdrawOrder> page = new Page<>(current, size);
        IPage<WithdrawOrder> result = this.page(page, new QueryWrapper<WithdrawOrder>()
                .eq(status != null && !status.isBlank(), "status", status)
                .eq(userId != null, "user_id", userId)
                .eq(platformCode != null && !platformCode.isBlank(), "platform_code", platformCode)
                .orderByDesc("id"));

        // 批量回填昵称 + 银行卡脱敏（一次 IN，避免 N+1）
        List<WithdrawOrder> records = result.getRecords();
        if (!records.isEmpty()) {
            Set<Long> userIds = records.stream().map(WithdrawOrder::getUserId).filter(Objects::nonNull).collect(Collectors.toSet());
            Set<Long> cardIds = records.stream().map(WithdrawOrder::getBankCardId).filter(Objects::nonNull).collect(Collectors.toSet());
            Map<Long, String> nicknameMap = userIds.isEmpty() ? Map.of() : portalUserMapper.selectBatchIds(userIds).stream()
                    .filter(Objects::nonNull)
                    .collect(Collectors.toMap(PortalUser::getId, u -> u.getNickname() == null ? "" : u.getNickname(), (a, b) -> a));
            Map<Long, UserBankCard> cardMap = cardIds.isEmpty() ? Map.of() : bankCardMapper.selectBatchIds(cardIds).stream()
                    .filter(Objects::nonNull)
                    .collect(Collectors.toMap(UserBankCard::getId, c -> c, (a, b) -> a));
            for (WithdrawOrder o : records) {
                o.setNickname(nicknameMap.getOrDefault(o.getUserId(), o.getUserId() == null ? "" : "用户" + o.getUserId()));
                UserBankCard card = o.getBankCardId() == null ? null : cardMap.get(o.getBankCardId());
                o.setBankCardDesc(card == null ? "" : (card.getBankName() == null ? "" : card.getBankName()) + " " + card.getCardNoMasked());
            }
        }

        // 汇总卡片（SQL 聚合）
        Map<String, Object> summary = new LinkedHashMap<>();
        summary.put("auditingAmount", sumStatus(WithdrawOrder.STATUS_AUDITING));
        summary.put("paidAmount", sumStatus(WithdrawOrder.STATUS_PAID));
        summary.put("rejectedAmount", sumStatus(WithdrawOrder.STATUS_REJECTED));

        Map<String, Object> data = new HashMap<>();
        data.put("page", result);
        data.put("summary", summary);
        return data;
    }

    private BigDecimal sumStatus(String status) {
        List<Map<String, Object>> rows = this.listMaps(new QueryWrapper<WithdrawOrder>()
                .select("COALESCE(SUM(amount), 0) AS total", "COUNT(*) AS cnt")
                .eq("status", status));
        if (rows == null || rows.isEmpty() || rows.get(0) == null || rows.get(0).get("total") == null) {
            return BigDecimal.ZERO;
        }
        return new BigDecimal(rows.get(0).get("total").toString());
    }

    @Override
    public Map<String, Object> userSummary(Long userId) {
        UserAccount account = userAccountService.getOrCreate(userId);
        List<Map<String, Object>> rows = this.listMaps(new QueryWrapper<WithdrawOrder>()
                .select("COALESCE(SUM(amount), 0) AS total")
                .eq("user_id", userId)
                .eq("status", WithdrawOrder.STATUS_AUDITING));
        BigDecimal auditing = (rows == null || rows.isEmpty() || rows.get(0) == null || rows.get(0).get("total") == null)
                ? BigDecimal.ZERO : new BigDecimal(rows.get(0).get("total").toString());
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("balance", account.getBalance());
        data.put("totalIncome", account.getTotalIncome());
        data.put("totalWithdraw", account.getTotalWithdraw());
        data.put("auditingAmount", auditing);
        return data;
    }

    private String generateWithdrawNo() {
        return "WD" + java.time.format.DateTimeFormatter.ofPattern("yyyyMMddHHmmss").format(LocalDateTime.now())
                + String.format("%06d", RANDOM.nextInt(1000000));
    }
}
