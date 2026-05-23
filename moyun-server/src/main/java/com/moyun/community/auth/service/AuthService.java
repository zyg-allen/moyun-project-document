package com.moyun.community.auth.service;

import com.moyun.community.auth.model.dto.LoginDto;
import com.moyun.community.auth.model.dto.RegisterDto;
import com.moyun.community.auth.model.vo.UserVo;

import java.util.Map;

public interface AuthService {

    Map<String, Object> login(LoginDto dto);

    Map<String, Object> register(RegisterDto dto);

    void logout();

    UserVo getCurrentUser();
}
