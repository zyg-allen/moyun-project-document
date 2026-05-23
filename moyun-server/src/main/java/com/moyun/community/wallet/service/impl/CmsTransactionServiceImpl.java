package com.moyun.community.wallet.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.moyun.community.wallet.entity.CmsTransaction;
import com.moyun.community.wallet.mapper.CmsTransactionMapper;
import com.moyun.community.wallet.service.ICmsTransactionService;
import org.springframework.stereotype.Service;

@Service
public class CmsTransactionServiceImpl extends ServiceImpl<CmsTransactionMapper, CmsTransaction> implements ICmsTransactionService {
}
