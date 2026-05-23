package com.moyun.community.wallet.controller.app;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.moyun.common.core.controller.BaseController;
import com.moyun.common.core.domain.AjaxResult;
import com.moyun.common.core.domain.TableDataInfo;
import com.moyun.common.utils.SecurityUtils;
import com.moyun.community.wallet.entity.CmsWallet;
import com.moyun.community.wallet.entity.CmsTransaction;
import com.moyun.community.wallet.entity.CmsRecharge;
import com.moyun.community.wallet.entity.CmsWithdraw;
import com.moyun.community.wallet.service.ICmsWalletService;
import com.moyun.community.wallet.service.ICmsTransactionService;
import com.moyun.community.wallet.service.ICmsRechargeService;
import com.moyun.community.wallet.service.ICmsWithdrawService;
import com.moyun.community.content.entity.CmsArticle;
import com.moyun.community.content.service.ICmsArticleService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/app/wallet")
public class WalletAppController extends BaseController {

    @Autowired
    private ICmsWalletService walletService;

    @Autowired
    private ICmsTransactionService transactionService;

    @Autowired
    private ICmsRechargeService rechargeService;

    @Autowired
    private ICmsWithdrawService withdrawService;

    @Autowired
    private ICmsArticleService articleService;

    @GetMapping("/balance")
    public AjaxResult balance() {
        Long userId = SecurityUtils.getUserId();
        CmsWallet wallet = walletService.getById(userId);
        if (wallet == null) {
            wallet = new CmsWallet();
            wallet.setUserId(userId);
            wallet.setBalance(BigDecimal.ZERO);
            wallet.setFrozenBalance(BigDecimal.ZERO);
            wallet.setStatus("normal");
            walletService.save(wallet);
        }
        return success(wallet);
    }

    @GetMapping("/transaction/list")
    public TableDataInfo transactionList() {
        Long userId = SecurityUtils.getUserId();
        startPage();
        List<CmsTransaction> list = transactionService.list(
            new LambdaQueryWrapper<CmsTransaction>()
                .eq(CmsTransaction::getUserId, userId)
                .orderByDesc(CmsTransaction::getCreateTime)
        );
        return getDataTable(list);
    }

    @Transactional
    @PostMapping("/recharge")
    public AjaxResult recharge(@RequestParam Double amount) {
        if (amount <= 0) {
            return error("充值金额必须大于0");
        }

        Long userId = SecurityUtils.getUserId();

        CmsWallet wallet = walletService.getById(userId);
        if (wallet == null) {
            wallet = new CmsWallet();
            wallet.setUserId(userId);
            wallet.setBalance(BigDecimal.ZERO);
            wallet.setFrozenBalance(BigDecimal.ZERO);
            wallet.setStatus("normal");
            walletService.save(wallet);
        }

        BigDecimal rechargeAmount = BigDecimal.valueOf(amount);
        wallet.setBalance(wallet.getBalance().add(rechargeAmount));
        walletService.updateById(wallet);

        CmsRecharge recharge = new CmsRecharge();
        recharge.setUserId(userId);
        recharge.setAmount(rechargeAmount);
        recharge.setStatus("completed");
        recharge.setOrderNo("RC" + System.currentTimeMillis() + UUID.randomUUID().toString().substring(0, 8));
        recharge.setPayTime(LocalDateTime.now());
        rechargeService.save(recharge);

        CmsTransaction transaction = new CmsTransaction();
        transaction.setUserId(userId);
        transaction.setType("recharge");
        transaction.setAmount(rechargeAmount);
        transaction.setBalanceBefore(wallet.getBalance().subtract(rechargeAmount));
        transaction.setBalanceAfter(wallet.getBalance());
        transaction.setStatus("completed");
        transaction.setRemark("充值 " + rechargeAmount + " 墨币");
        transactionService.save(transaction);

        return success(wallet);
    }

    @Transactional
    @PostMapping("/withdraw")
    public AjaxResult withdraw(@RequestParam Double amount) {
        if (amount <= 0) {
            return error("提现金额必须大于0");
        }

        Long userId = SecurityUtils.getUserId();

        CmsWallet wallet = walletService.getById(userId);
        if (wallet == null) {
            return error("钱包不存在");
        }

        BigDecimal withdrawAmount = BigDecimal.valueOf(amount);
        if (wallet.getBalance().compareTo(withdrawAmount) < 0) {
            return error("余额不足");
        }

        wallet.setBalance(wallet.getBalance().subtract(withdrawAmount));
        walletService.updateById(wallet);

        CmsWithdraw withdraw = new CmsWithdraw();
        withdraw.setUserId(userId);
        withdraw.setAmount(withdrawAmount);
        withdraw.setFee(withdrawAmount.multiply(BigDecimal.valueOf(0.01)));
        withdraw.setActualAmount(withdrawAmount.subtract(withdraw.getFee()));
        withdraw.setStatus("pending");
        withdraw.setOrderNo("WD" + System.currentTimeMillis() + UUID.randomUUID().toString().substring(0, 8));
        withdrawService.save(withdraw);

        CmsTransaction transaction = new CmsTransaction();
        transaction.setUserId(userId);
        transaction.setType("withdraw");
        transaction.setAmount(withdrawAmount.negate());
        transaction.setBalanceBefore(wallet.getBalance().add(withdrawAmount));
        transaction.setBalanceAfter(wallet.getBalance());
        transaction.setStatus("pending");
        transaction.setRemark("申请提现 " + withdrawAmount + " 墨币");
        transactionService.save(transaction);

        return success(withdraw);
    }

    @Transactional
    @PostMapping("/reward")
    public AjaxResult reward(@RequestParam Long articleId, @RequestParam Double amount) {
        if (amount <= 0) {
            return error("打赏金额必须大于0");
        }

        Long userId = SecurityUtils.getUserId();
        CmsArticle article = articleService.getById(articleId);
        if (article == null) {
            return error("文章不存在");
        }

        CmsWallet wallet = walletService.getById(userId);
        if (wallet == null || wallet.getBalance().compareTo(BigDecimal.valueOf(amount)) < 0) {
            return error("余额不足");
        }

        CmsWallet authorWallet = walletService.getById(article.getAuthorId());
        if (authorWallet == null) {
            authorWallet = new CmsWallet();
            authorWallet.setUserId(article.getAuthorId());
            authorWallet.setBalance(BigDecimal.ZERO);
            authorWallet.setFrozenBalance(BigDecimal.ZERO);
            authorWallet.setStatus("normal");
            walletService.save(authorWallet);
        }

        BigDecimal rewardAmount = BigDecimal.valueOf(amount);
        BigDecimal fee = rewardAmount.multiply(BigDecimal.valueOf(0.05));
        BigDecimal actualAmount = rewardAmount.subtract(fee);

        wallet.setBalance(wallet.getBalance().subtract(rewardAmount));
        walletService.updateById(wallet);

        authorWallet.setBalance(authorWallet.getBalance().add(actualAmount));
        walletService.updateById(authorWallet);

        CmsTransaction rewardTransaction = new CmsTransaction();
        rewardTransaction.setUserId(userId);
        rewardTransaction.setType("reward_out");
        rewardTransaction.setAmount(rewardAmount.negate());
        rewardTransaction.setBalanceBefore(wallet.getBalance().add(rewardAmount));
        rewardTransaction.setBalanceAfter(wallet.getBalance());
        rewardTransaction.setStatus("completed");
        rewardTransaction.setRemark("打赏文章《" + article.getTitle() + "》-" + rewardAmount + "墨币");
        rewardTransaction.setRelatedId(articleId);
        transactionService.save(rewardTransaction);

        CmsTransaction incomeTransaction = new CmsTransaction();
        incomeTransaction.setUserId(article.getAuthorId());
        incomeTransaction.setType("reward_in");
        incomeTransaction.setAmount(actualAmount);
        incomeTransaction.setBalanceBefore(authorWallet.getBalance().subtract(actualAmount));
        incomeTransaction.setBalanceAfter(authorWallet.getBalance());
        incomeTransaction.setStatus("completed");
        incomeTransaction.setRemark("收到打赏《" + article.getTitle() + "》+" + actualAmount + "墨币");
        incomeTransaction.setRelatedId(articleId);
        transactionService.save(incomeTransaction);

        return success(actualAmount);
    }
}
