package com.moyun.community.auth.model.vo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

/**
 * 登录响应VO
 *
 * @author moyun
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LoginResponseVo implements Serializable {

    private static final long serialVersionUID = 1L;

    private String token;
    private String refreshToken;
    private PortalUserVo user;
}
