package com.moyun.ext.cms.controller;

import com.moyun.core.base.AjaxResult;
import com.moyun.core.base.BaseController;
import com.moyun.pay.config.PayProperties;
import com.moyun.system.domain.entity.SysConfig;
import com.moyun.system.service.ISysConfigService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;

/**
 * CMS 支付配置管理 Controller（V11.0）
 *
 * <p>配置总览（商户参数仅返回是否已配置的布尔状态，绝不下发密钥/私钥）
 * + 平台抽成费率运行时调整（写 sys_config，运行时优先于 yaml）。
 *
 * @author moyun
 */
@Tag(name = "CMS支付配置管理", description = "支付通道配置总览与费率调整")
@RestController
@RequestMapping("/cms/pay/config")
public class CmsPayConfigController extends BaseController {

    private static final String FEE_RATE_CONFIG_KEY = "pay.platform.fee-rate";

    @Autowired
    private PayProperties payProperties;

    @Autowired
    private ISysConfigService configService;

    @Operation(summary = "支付配置总览", description = "通道开关/超时/费率/微信商户参数配置状态（脱敏，仅布尔）")
    @PreAuthorize("@ss.hasPermi('pay:config:view')")
    @GetMapping("/view")
    public AjaxResult view() {
        PayProperties.Wechat wechat = payProperties.getWechat();
        Map<String, Object> data = new HashMap<>();
        data.put("enabled", payProperties.isEnabled());
        data.put("orderExpireMinutes", payProperties.getOrderExpireMinutes());
        data.put("platformFeeRate", resolveFeeRate());
        data.put("bankCardMaxCount", payProperties.getSecurity().getBankCardMaxCount());
        // 微信商户参数：仅返回是否已配置，绝不下发值
        Map<String, Object> wechatStatus = new HashMap<>();
        wechatStatus.put("mockEnabled", wechat.isMockEnabled());
        wechatStatus.put("appIdConfigured", isConfigured(wechat.getAppId()));
        wechatStatus.put("mchIdConfigured", isConfigured(wechat.getMchId()));
        wechatStatus.put("merchantSerialConfigured", isConfigured(wechat.getMerchantSerial()));
        wechatStatus.put("privateKeyPathConfigured", isConfigured(wechat.getPrivateKeyPath()));
        wechatStatus.put("apiV3KeyConfigured", isConfigured(wechat.getApiV3Key()));
        wechatStatus.put("notifyUrlConfigured", isConfigured(wechat.getNotifyUrl()));
        data.put("wechat", wechatStatus);
        return success(data);
    }

    @Operation(summary = "调整平台抽成费率", description = "运行时调整（写 sys_config），范围 [0, 0.5]，立即生效于新订单")
    @PreAuthorize("@ss.hasPermi('pay:config:edit')")
    @PostMapping("/fee-rate")
    public AjaxResult updateFeeRate(@RequestBody Map<String, Object> body) {
        Object rateObj = body.get("platformFeeRate");
        if (rateObj == null) {
            return error("费率不能为空");
        }
        double rate;
        try {
            rate = Double.parseDouble(String.valueOf(rateObj));
        } catch (NumberFormatException e) {
            return error("费率格式不正确");
        }
        if (rate < 0 || rate > 0.5) {
            return error("费率范围必须在 0 ~ 0.5 之间");
        }
        // 存在则更新，不存在则新增
        String existing = configService.selectConfigByKey(FEE_RATE_CONFIG_KEY);
        SysConfig config = new SysConfig();
        config.setConfigKey(FEE_RATE_CONFIG_KEY);
        config.setConfigValue(String.valueOf(rate));
        if (existing == null || existing.isBlank()) {
            config.setConfigName("平台抽成费率");
            config.setConfigType("Y");
            config.setRemark("V11.0 支付分账平台抽成率（运行时优先于 yaml）");
            configService.insertConfig(config);
        } else {
            config.setConfigName("平台抽成费率");
            config.setRemark("V11.0 支付分账平台抽成率（运行时优先于 yaml）");
            configService.updateConfig(config);
        }
        return success(Map.of("platformFeeRate", rate));
    }

    private double resolveFeeRate() {
        try {
            String configValue = configService.selectConfigByKey(FEE_RATE_CONFIG_KEY);
            if (configValue != null && !configValue.isBlank()) {
                double parsed = Double.parseDouble(configValue.trim());
                if (parsed >= 0 && parsed < 1) {
                    return parsed;
                }
            }
        } catch (Exception ignore) {
            // 回退 yaml
        }
        return payProperties.getPlatformFeeRate();
    }

    private boolean isConfigured(String value) {
        return value != null && !value.isBlank() && !value.startsWith("todo-");
    }
}
