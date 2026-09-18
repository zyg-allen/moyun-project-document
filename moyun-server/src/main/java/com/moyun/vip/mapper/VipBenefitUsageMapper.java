package com.moyun.vip.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.moyun.vip.domain.entity.VipBenefitUsage;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDate;

/**
 * 权益使用记录 Mapper
 *
 * @author moyun
 */
@Mapper
public interface VipBenefitUsageMapper extends BaseMapper<VipBenefitUsage> {

    /**
     * 原子累加使用次数（Redis 计数异步落库；唯一键冲突即累加）
     *
     * @return 影响行数
     */

    int upsertUsage(@Param("userId") Long userId, @Param("platformCode") String platformCode,
                    @Param("benefitCode") String benefitCode, @Param("usageDate") LocalDate usageDate);
}
