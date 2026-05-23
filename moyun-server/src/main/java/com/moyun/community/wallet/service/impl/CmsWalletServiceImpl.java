package com.moyun.community.wallet.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.moyun.community.wallet.entity.CmsWallet;
import com.moyun.community.wallet.mapper.CmsWalletMapper;
import com.moyun.community.wallet.service.ICmsWalletService;
import org.springframework.stereotype.Service;

@Service
public class CmsWalletServiceImpl extends ServiceImpl<CmsWalletMapper, CmsWallet> implements ICmsWalletService {
}
