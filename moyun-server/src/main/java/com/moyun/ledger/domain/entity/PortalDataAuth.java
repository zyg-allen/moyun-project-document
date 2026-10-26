package com.moyun.ledger.domain.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 用户数据授权（portal_data_auth）
 *
 * <p>记账域的用户个体数据（流水/账户/预算/库存/AI 报告）默认对后台保密；
 * 仅当用户在 App 内主动授权后，后台才可在授权范围与有效期内查看，且每次查看都要落审计。</p>
 *
 * <p>本表是"C 档用户数据"的唯一准入凭证，后台只读，不能代用户写入授权。</p>
 *
 * @author moyun
 */
@Data
@TableName("portal_data_auth")
public class PortalDataAuth implements Serializable {

    @java.io.Serial
    private static final long serialVersionUID = 1L;

    /** 业务域：记账 */
    public static final String DOMAIN_LEDGER = "ledger";

    /** 授权范围（逗号分隔） */
    public static final String SCOPE_TRANSACTION = "transaction";
    public static final String SCOPE_ACCOUNT = "account";
    public static final String SCOPE_BUDGET = "budget";
    public static final String SCOPE_INVENTORY = "inventory";
    public static final String SCOPE_AI = "ai";
    public static final String SCOPE_PROFILE = "profile";

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    /** 授权人 portal_user.id */
    private Long userId;

    /** 业务域：ledger/portal/cms */
    private String bizDomain;

    /** 授权范围，逗号分隔：transaction,account,budget,inventory,ai,profile */
    private String scopes;

    /** 用途（用户可见，后台展示） */
    private String purpose;

    /** 发起渠道：app/web */
    private String channel;

    /** user_initiated=用户主动授权 / requested=后台申请后用户确认 */
    private String grantType;

    /** 隐私政策版本 */
    private String docVersion;

    private LocalDateTime grantedAt;

    /** 到期自动失效；null=长期（用户可随时撤回） */
    private LocalDateTime expireAt;

    private LocalDateTime revokedAt;

    private String revokeReason;

    private LocalDateTime createTime;

    /** 是否仍有效：未撤回且未过期 */
    public boolean isActive(LocalDateTime now) {
        if (revokedAt != null) {
            return false;
        }
        return expireAt == null || expireAt.isAfter(now);
    }

    /** 是否覆盖某范围 */
    public boolean covers(String scope) {
        if (scope == null || scopes == null) {
            return false;
        }
        for (String s : scopes.split(",")) {
            if (scope.equals(s.trim())) {
                return true;
            }
        }
        return false;
    }
}
