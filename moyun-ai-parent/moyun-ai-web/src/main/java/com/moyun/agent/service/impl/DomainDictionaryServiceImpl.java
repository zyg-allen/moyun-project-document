package com.moyun.agent.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.moyun.agent.entity.DomainDictionary;
import com.moyun.agent.mapper.DomainDictionaryMapper;
import com.moyun.agent.service.DomainDictionaryService;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 领域词典服务实现
 */
@Service
public class DomainDictionaryServiceImpl extends ServiceImpl<DomainDictionaryMapper, DomainDictionary>
        implements DomainDictionaryService {

    @Override
    public List<DomainDictionary> listGlobal() {
        QueryWrapper<DomainDictionary> wrapper = new QueryWrapper<>();
        wrapper.eq("is_global", true);
        wrapper.eq("enabled", true);
        wrapper.orderByDesc("priority");
        return list(wrapper);
    }

    @Override
    public List<DomainDictionary> listProfessional() {
        QueryWrapper<DomainDictionary> wrapper = new QueryWrapper<>();
        wrapper.eq("is_global", false);
        wrapper.eq("enabled", true);
        wrapper.orderByDesc("priority");
        return list(wrapper);
    }
}
