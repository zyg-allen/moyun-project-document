package com.moyun.vip.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.moyun.vip.domain.entity.VipApiRegistry;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

/**
 * VIP 接口注册表 Mapper
 *
 * <p>upsert 只更新代码侧字段（platform/benefit/consume/message/scan_time），
 * 运营字段（api_desc/enabled）重启不覆盖。
 *
 * @author moyun
 */
@Mapper
public interface VipApiRegistryMapper extends BaseMapper<VipApiRegistry> {

    /**
     * 幂等注册（唯一键 api_path + http_method）
     */
    @Insert("INSERT INTO vip_api_registry (api_path, http_method, controller_class, method_name, "
            + "platform_code, benefit_code, consume, message, scan_time, create_time, update_time) "
            + "VALUES (#{apiPath}, #{httpMethod}, #{controllerClass}, #{methodName}, "
            + "#{platformCode}, #{benefitCode}, #{consume}, #{message}, NOW(), NOW(), NOW()) "
            + "ON DUPLICATE KEY UPDATE controller_class = VALUES(controller_class), "
            + "method_name = VALUES(method_name), platform_code = VALUES(platform_code), "
            + "benefit_code = VALUES(benefit_code), consume = VALUES(consume), "
            + "message = VALUES(message), scan_time = NOW()")
    int upsert(@Param("apiPath") String apiPath, @Param("httpMethod") String httpMethod,
               @Param("controllerClass") String controllerClass, @Param("methodName") String methodName,
               @Param("platformCode") String platformCode, @Param("benefitCode") String benefitCode,
               @Param("consume") Integer consume, @Param("message") String message);
}
