# Spring Security 认证授权系统使用说明

## 一、系统概述

本项目已集成 Spring Security + JWT 实现完整的用户认证与授权流程，主要特性包括：

- 基于 JWT 的无状态认证
- BCrypt 密码加密
- 统一的异常处理
- 灵活的权限配置

## 二、核心组件

### 1. 配置类
- `SecurityConfig.java` - Spring Security 核心配置
- `JwtAuthenticationFilter.java` - JWT 认证过滤器

### 2. 工具类
- `JwtUtils.java` - JWT 生成与解析工具

### 3. 处理器
- `CustomAuthenticationEntryPoint.java` - 认证失败处理
- `CustomAccessDeniedHandler.java` - 权限不足处理
- `CustomLogoutHandler.java` - 登出成功处理

### 4. 实体与 DTO
- `SysUser.java` - 用户实体
- `LoginRequest.java` - 登录请求 DTO
- `LoginResponse.java` - 登录响应 DTO

## 三、接口说明

### 1. 登录接口
```
POST /api/v1/auth/login
Content-Type: application/json

请求体：
{
    "username": "admin",
    "password": "admin123"
}

响应：
{
    "code": 200,
    "message": "操作成功",
    "data": {
        "token": "eyJhbGciOiJIUzI1NiJ9...",
        "username": "admin",
        "nickname": "系统管理员"
    }
}
```

### 2. 登出接口
```
POST /api/v1/auth/logout
Authorization: Bearer {token}

响应：
{
    "code": 200,
    "message": "操作成功",
    "data": null
}
```

### 3. 获取当前用户信息
```
GET /api/v1/auth/user-info
Authorization: Bearer {token}

响应：
{
    "code": 200,
    "message": "操作成功",
    "data": {
        "id": 1,
        "username": "admin",
        "nickname": "系统管理员",
        "email": "admin@example.com"
    }
}
```

## 四、配置说明

### 1. application.yaml 配置
```yaml
# JWT 配置
jwt:
  secret: your-256-bit-secret-key-must-be-at-least-256-bits-long
  expiration: 86400000  # 24小时（毫秒）
  header: Authorization
  prefix: "Bearer "
```

### 2. 数据库初始化
执行 `src/main/resources/sql/sys_user.sql` 创建用户表和初始数据。

默认账号：
- 管理员：admin / admin123
- 测试用户：user / user123

## 五、前端集成

### 1. 登录流程
1. 调用登录接口获取 Token
2. 将 Token 存储到 localStorage 或 sessionStorage
3. 后续请求在 Header 中携带 Token

### 2. 请求示例
```javascript
// 登录
const login = async (username, password) => {
    const response = await fetch('/api/v1/auth/login', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ username, password })
    });
    const data = await response.json();
    if (data.code === 200) {
        localStorage.setItem('token', data.data.token);
    }
    return data;
};

// 携带 Token 的请求
const fetchWithToken = async (url, options = {}) => {
    const token = localStorage.getItem('token');
    return fetch(url, {
        ...options,
        headers: {
            ...options.headers,
            'Authorization': `Bearer ${token}`
        }
    });
};
```

### 3. 错误处理
- 401：Token 无效或已过期，需重新登录
- 403：权限不足，无权访问该资源

## 六、安全建议

1. **生产环境配置**
   - 修改 JWT 密钥为强密码
   - 配置 HTTPS
   - 设置合理的 Token 过期时间

2. **密码安全**
   - 使用 BCrypt 加密存储
   - 建议密码强度要求
   - 定期更换密码

3. **Token 管理**
   - Token 存储在 localStorage 或 sessionStorage
   - 前端需处理 Token 过期情况
   - 可实现 Token 刷新机制

## 七、扩展说明

### 1. 添加权限控制
在 `SecurityConfig` 中配置路径权限：
```java
.requestMatchers("/api/v1/admin/**").hasRole("ADMIN")
.requestMatchers("/api/v1/user/**").hasAnyRole("USER", "ADMIN")
```

### 2. 方法级权限
使用注解进行方法级权限控制：
```java
@PreAuthorize("hasRole('ADMIN')")
public void adminOnly() {
    // 仅管理员可访问
}

@PreAuthorize("hasAuthority('user:delete')")
public void deleteUser() {
    // 需要 user:delete 权限
}
```

### 3. 自定义用户详情
实现 `UserDetailsService` 接口自定义用户加载逻辑。