# 墨韵智库 API 接口文档

## 目录

- [通用说明](#通用说明)
- [用户模块](#用户模块)
- [文章模块](#文章模块)
- [评论模块](#评论模块)
- [收藏模块](#收藏模块)
- [关注模块](#关注模块)
- [分类模块](#分类模块)
- [标签模块](#标签模块)
- [搜索模块](#搜索模块)
- [通知模块](#通知模块)
- [友情链接模块](#友情链接模块)
- [订单模块](#订单模块)
- [VIP模块](#vip模块)
- [钱包模块](#钱包模块)
- [文件上传](#文件上传)
- [系统配置](#系统配置)

---

## 通用说明

### 基础信息

- **Base URL**: `https://api.moyunzhiku.com/api` (生产环境)
- **协议**: HTTPS
- **数据格式**: JSON
- **字符编码**: UTF-8

### 通用响应格式

所有接口返回统一格式：

```json
{
  "code": 200,
  "message": "success",
  "data": {}
}
```

### 状态码说明

| 状态码 | 说明 |
|--------|------|
| 200 | 请求成功 |
| 400 | 请求参数错误 |
| 401 | 未授权/Token无效 |
| 403 | 没有权限 |
| 404 | 资源不存在 |
| 500 | 服务器错误 |

### 认证说明

需要认证的接口需在请求头中携带 Token：

```
Authorization: Bearer {token}
```

### 分页参数

分页接口统一参数：

| 参数名 | 类型 | 必填 | 说明 |
|--------|------|------|------|
| pageNum | number | 否 | 页码，默认 1 |
| pageSize | number | 否 | 每页数量，默认 10 |

分页响应格式（MyBatis-Plus Page对象）：

```json
{
  "records": [],
  "total": 100,
  "size": 10,
  "current": 1,
  "pages": 10
}
```

**字段说明**：
- `records`：数据列表
- `total`：总记录数
- `size`：每页大小
- `current`：当前页码
- `pages`：总页数

**兼容性说明**：
- 前端 `httpGetList` 函数兼容多种分页响应格式，优先顺序为：`records` > `rows` > `list`
- 新接口统一使用 `records` 字段

---

## 用户模块

### 1. 用户登录

**接口地址**: `POST /user/login`

**请求参数**:

| 参数名 | 类型 | 必填 | 说明 |
|--------|------|------|------|
| username | string | 是 | 用户名/邮箱 |
| password | string | 是 | 密码 |
| captcha | string | 否 | 验证码 |

**响应数据**:

```json
{
  "token": "eyJhbGciOiJIUzI1NiIs...",
  "refreshToken": "eyJhbGciOiJIUzI1NiIs...",
  "user": {
    "id": "1",
    "username": "zhangsan",
    "nickname": "张三",
    "email": "zhangsan@example.com",
    "avatar": "https://example.com/avatar.jpg",
    "bio": "热爱编程",
    "role": "user",
    "vipExpireAt": "2025-12-31",
    "createdAt": "2024-01-01T00:00:00Z"
  }
}
```

---

### 2. 用户注册

**接口地址**: `POST /user/register`

**请求参数**:

| 参数名 | 类型 | 必填 | 说明 |
|--------|------|------|------|
| username | string | 是 | 用户名 |
| email | string | 是 | 邮箱 |
| password | string | 是 | 密码 |
| confirmPassword | string | 是 | 确认密码 |
| captcha | string | 否 | 验证码 |

**响应数据**:

同用户登录接口。

---

### 3. 退出登录

**接口地址**: `POST /user/logout`

**需要认证**: 是

**请求参数**: 无

**响应数据**: 无

---

### 4. 获取当前用户信息

**接口地址**: `GET /user/current`

**需要认证**: 是

**请求参数**: 无

**响应数据**:

```json
{
  "id": "1",
  "username": "zhangsan",
  "nickname": "张三",
  "email": "zhangsan@example.com",
  "avatar": "https://example.com/avatar.jpg",
  "bio": "热爱编程",
  "phone": "138****8888",
  "wechat": "wechat_id",
  "position": "前端工程师",
  "role": "user",
  "vipExpireAt": "2025-12-31",
  "isPhoneVerified": true,
  "isWechatVerified": false,
  "twoFactorEnabled": false,
  "createdAt": "2024-01-01T00:00:00Z",
  "updatedAt": "2024-01-02T00:00:00Z"
}
```

---

### 5. 更新用户信息

**接口地址**: `PUT /user/profile`

**需要认证**: 是

**请求参数**:

| 参数名 | 类型 | 必填 | 说明 |
|--------|------|------|------|
| nickname | string | 否 | 昵称 |
| bio | string | 否 | 个人简介 |
| avatar | string | 否 | 头像URL |
| position | string | 否 | 职位 |

**响应数据**:

同获取当前用户信息。

---

### 6. 更新密码

**接口地址**: `PUT /user/password`

**需要认证**: 是

**请求参数**:

| 参数名 | 类型 | 必填 | 说明 |
|--------|------|------|------|
| oldPassword | string | 是 | 原密码 |
| newPassword | string | 是 | 新密码 |
| confirmPassword | string | 是 | 确认密码 |

**响应数据**: 无

---

### 7. 绑定手机号

**接口地址**: `POST /user/bind-phone`

**需要认证**: 是

**请求参数**:

| 参数名 | 类型 | 必填 | 说明 |
|--------|------|------|------|
| phone | string | 是 | 手机号 |
| code | string | 是 | 验证码 |

**响应数据**: 无

---

### 8. 发送短信验证码

**接口地址**: `POST /user/send-sms`

**需要认证**: 否

**请求参数**:

| 参数名 | 类型 | 必填 | 说明 |
|--------|------|------|------|
| phone | string | 是 | 手机号 |
| type | string | 是 | 类型：register/login/bind/reset_password |

**响应数据**: 无

---

### 9. 上传头像

**接口地址**: `POST /user/avatar`

**需要认证**: 是

**请求格式**: multipart/form-data

**请求参数**:

| 参数名 | 类型 | 必填 | 说明 |
|--------|------|------|------|
| file | File | 是 | 头像文件 |

**响应数据**:

```json
{
  "id": "1",
  "username": "zhangsan",
  "avatar": "https://example.com/new-avatar.jpg"
  // ... 其他用户信息
}
```

---

### 10. 获取用户统计信息

**接口地址**: `GET /user/stats` 或 `GET /user/{userId}/stats`

**需要认证**: 否（获取他人信息不需要认证）

**路径参数**:

| 参数名 | 类型 | 必填 | 说明 |
|--------|------|------|------|
| userId | string | 否 | 用户ID，不传则获取当前用户 |

**响应数据**:

```json
{
  "followers": 100,
  "following": 50,
  "articles": 20,
  "likes": 500,
  "views": 10000,
  "bookmarks": 30,
  "todayVisitors": 15,
  "totalVisitors": 1000
}
```

---

### 11. 获取用户详情

**接口地址**: `GET /user/{userId}`

**需要认证**: 否

**路径参数**:

| 参数名 | 类型 | 必填 | 说明 |
|--------|------|------|------|
| userId | string | 是 | 用户ID |

**响应数据**:

同获取当前用户信息。

---

## 文章模块

### 1. 获取文章列表

**接口地址**: `GET /article/list`

**需要认证**: 否

**请求参数**:

| 参数名 | 类型 | 必填 | 说明 |
|--------|------|------|------|
| page | number | 否 | 页码 |
| pageSize | number | 否 | 每页数量 |
| category | string | 否 | 分类名称 |
| categoryId | string | 否 | 分类ID |
| tag | string | 否 | 标签名称 |
| keyword | string | 否 | 搜索关键词 |
| authorId | string | 否 | 作者ID |
| isFeatured | boolean | 否 | 是否精选 |
| status | string | 否 | 状态：draft/published/archived |
| sortBy | string | 否 | 排序：createdAt/views/likes/comments |
| sortOrder | string | 否 | 排序方向：asc/desc |

**响应数据**:

```json
{
  "records": [
    {
      "id": "1",
      "title": "Vue3 入门教程",
      "content": "<p>文章内容...</p>",
      "excerpt": "文章摘要...",
      "cover": "https://example.com/cover.jpg",
      "author": {
        "id": "1",
        "username": "zhangsan",
        "avatar": "https://example.com/avatar.jpg"
      },
      "authorId": "1",
      "category": "技术",
      "categoryId": "1",
      "tags": ["Vue3", "前端"],
      "views": 1000,
      "likes": 50,
      "comments": 10,
      "shareCount": 5,
      "bookmarkCount": 20,
      "isFeatured": true,
      "isTop": false,
      "status": "published",
      "createdAt": "2024-01-01T00:00:00Z",
      "updatedAt": "2024-01-02T00:00:00Z",
      "publishedAt": "2024-01-01T00:00:00Z"
    }
  ],
  "total": 100,
  "size": 10,
  "current": 1,
  "pages": 10
}
```

---

### 2. 获取文章详情

**接口地址**: `GET /article/{id}`

**需要认证**: 否

**路径参数**:

| 参数名 | 类型 | 必填 | 说明 |
|--------|------|------|------|
| id | string | 是 | 文章ID |

**响应数据**:

同文章列表中的单个文章对象。

---

### 3. 创建文章

**接口地址**: `POST /article`

**需要认证**: 是

**请求参数**:

| 参数名 | 类型 | 必填 | 说明 |
|--------|------|------|------|
| title | string | 是 | 标题 |
| content | string | 是 | 内容（HTML格式） |
| excerpt | string | 否 | 摘要 |
| cover | string | 否 | 封面URL |
| category | string | 否 | 分类名称 |
| categoryId | string | 否 | 分类ID |
| tags | string[] | 否 | 标签列表 |
| status | string | 否 | 状态：draft/published，默认 draft |

**响应数据**:

同文章详情。

---

### 4. 更新文章

**接口地址**: `PUT /article/{id}`

**需要认证**: 是（仅作者本人可更新）

**路径参数**:

| 参数名 | 类型 | 必填 | 说明 |
|--------|------|------|------|
| id | string | 是 | 文章ID |

**请求参数**:

同创建文章，所有参数可选。

**响应数据**:

同文章详情。

---

### 5. 删除文章

**接口地址**: `DELETE /article/{id}`

**需要认证**: 是（仅作者本人可删除）

**路径参数**:

| 参数名 | 类型 | 必填 | 说明 |
|--------|------|------|------|
| id | string | 是 | 文章ID |

**响应数据**: 无

---

### 6. 文章点赞

**接口地址**: `POST /article/{id}/like`

**需要认证**: 是

**路径参数**:

| 参数名 | 类型 | 必填 | 说明 |
|--------|------|------|------|
| id | string | 是 | 文章ID |

**响应数据**: 无

---

### 7. 取消点赞

**接口地址**: `DELETE /article/{id}/like`

**需要认证**: 是

**路径参数**:

| 参数名 | 类型 | 必填 | 说明 |
|--------|------|------|------|
| id | string | 是 | 文章ID |

**响应数据**: 无

---

### 8. 增加文章浏览量

**接口地址**: `POST /article/{id}/view`

**需要认证**: 否

**路径参数**:

| 参数名 | 类型 | 必填 | 说明 |
|--------|------|------|------|
| id | string | 是 | 文章ID |

**响应数据**: 无

---

### 9. 获取相关推荐文章

**接口地址**: `GET /article/{id}/related`

**需要认证**: 否

**路径参数**:

| 参数名 | 类型 | 必填 | 说明 |
|--------|------|------|------|
| id | string | 是 | 文章ID |

**请求参数**:

| 参数名 | 类型 | 必填 | 说明 |
|--------|------|------|------|
| limit | number | 否 | 数量，默认 5 |

**响应数据**:

同文章列表。

---

### 10. 获取热门文章

**接口地址**: `GET /article/hot`

**需要认证**: 否

**请求参数**:

| 参数名 | 类型 | 必填 | 说明 |
|--------|------|------|------|
| limit | number | 否 | 数量，默认 10 |

**响应数据**:

同文章列表。

---

### 11. 获取精选文章

**接口地址**: `GET /article/featured`

**需要认证**: 否

**请求参数**:

| 参数名 | 类型 | 必填 | 说明 |
|--------|------|------|------|
| limit | number | 否 | 数量，默认 10 |

**响应数据**:

同文章列表。

---

### 12. 获取轮播文章

**接口地址**: `GET /article/carousel`

**需要认证**: 否

**请求参数**: 无

**响应数据**:

同文章列表，但直接返回数组。

---

## 评论模块

### 1. 获取评论列表

**接口地址**: `GET /comment/list`

**需要认证**: 否

**请求参数**:

| 参数名 | 类型 | 必填 | 说明 |
|--------|------|------|------|
| articleId | string | 是 | 文章ID |
| page | number | 否 | 页码 |
| pageSize | number | 否 | 每页数量 |
| parentId | string | 否 | 父评论ID（获取回复时使用） |

**响应数据**:

```json
{
  "records": [
    {
      "id": "1",
      "articleId": "1",
      "author": {
        "id": "1",
        "username": "zhangsan",
        "avatar": "https://example.com/avatar.jpg"
      },
      "authorId": "1",
      "content": "写得很好！",
      "parentId": null,
      "replyTo": null,
      "replyToUser": null,
      "replies": [],
      "likeCount": 5,
      "isLiked": false,
      "createdAt": "2024-01-01T00:00:00Z",
      "updatedAt": "2024-01-02T00:00:00Z"
    }
  ],
  "total": 50,
  "size": 10,
  "current": 1,
  "pages": 5
}
```

---

### 2. 创建评论

**接口地址**: `POST /comment`

**需要认证**: 是

**请求参数**:

| 参数名 | 类型 | 必填 | 说明 |
|--------|------|------|------|
| articleId | string | 是 | 文章ID |
| content | string | 是 | 评论内容 |
| parentId | string | 否 | 父评论ID（回复时使用） |
| replyTo | string | 否 | 回复的用户ID |

**响应数据**:

同评论列表中的单个评论对象。

---

### 3. 删除评论

**接口地址**: `DELETE /comment/{id}`

**需要认证**: 是（仅作者本人或管理员可删除）

**路径参数**:

| 参数名 | 类型 | 必填 | 说明 |
|--------|------|------|------|
| id | string | 是 | 评论ID |

**响应数据**: 无

---

### 4. 评论点赞

**接口地址**: `POST /comment/{id}/like`

**需要认证**: 是

**路径参数**:

| 参数名 | 类型 | 必填 | 说明 |
|--------|------|------|------|
| id | string | 是 | 评论ID |

**响应数据**: 无

---

### 5. 取消评论点赞

**接口地址**: `DELETE /comment/{id}/like`

**需要认证**: 是

**路径参数**:

| 参数名 | 类型 | 必填 | 说明 |
|--------|------|------|------|
| id | string | 是 | 评论ID |

**响应数据**: 无

---

## 收藏模块

### 1. 获取收藏列表

**接口地址**: `GET /bookmark/list`

**需要认证**: 是

**请求参数**:

| 参数名 | 类型 | 必填 | 说明 |
|--------|------|------|------|
| page | number | 否 | 页码 |
| pageSize | number | 否 | 每页数量 |

**响应数据**:

```json
{
  "records": [
    {
      "id": "1",
      "userId": "1",
      "articleId": "1",
      "article": {
        "id": "1",
        "title": "Vue3 入门教程"
        // ... 文章信息
      },
      "createdAt": "2024-01-01T00:00:00Z"
    }
  ],
  "total": 50,
  "size": 10,
  "current": 1,
  "pages": 5
}
```

---

### 2. 创建收藏

**接口地址**: `POST /bookmark`

**需要认证**: 是

**请求参数**:

| 参数名 | 类型 | 必填 | 说明 |
|--------|------|------|------|
| articleId | string | 是 | 文章ID |

**响应数据**:

同收藏列表中的单个收藏对象。

---

### 3. 删除收藏

**接口地址**: `DELETE /bookmark/{id}`

**需要认证**: 是

**路径参数**:

| 参数名 | 类型 | 必填 | 说明 |
|--------|------|------|------|
| id | string | 是 | 收藏ID |

**响应数据**: 无

---

### 4. 检查是否已收藏

**接口地址**: `GET /bookmark/check`

**需要认证**: 是

**请求参数**:

| 参数名 | 类型 | 必填 | 说明 |
|--------|------|------|------|
| articleId | string | 是 | 文章ID |

**响应数据**:

```json
{
  "bookmarked": true,
  "bookmarkId": "1"
}
```

---

## 关注模块

### 1. 关注用户

**接口地址**: `POST /follow/{userId}`

**需要认证**: 是

**路径参数**:

| 参数名 | 类型 | 必填 | 说明 |
|--------|------|------|------|
| userId | string | 是 | 要关注的用户ID |

**响应数据**: 无

---

### 2. 取消关注

**接口地址**: `DELETE /follow/{userId}`

**需要认证**: 是

**路径参数**:

| 参数名 | 类型 | 必填 | 说明 |
|--------|------|------|------|
| userId | string | 是 | 要取消关注的用户ID |

**响应数据**: 无

---

### 3. 检查是否已关注

**接口地址**: `GET /follow/check/{userId}`

**需要认证**: 是

**路径参数**:

| 参数名 | 类型 | 必填 | 说明 |
|--------|------|------|------|
| userId | string | 是 | 要检查的用户ID |

**响应数据**:

```json
{
  "following": true
}
```

---

### 4. 获取粉丝列表

**接口地址**: `GET /follow/{userId}/followers`

**需要认证**: 否

**路径参数**:

| 参数名 | 类型 | 必填 | 说明 |
|--------|------|------|------|
| userId | string | 是 | 用户ID |

**请求参数**:

| 参数名 | 类型 | 必填 | 说明 |
|--------|------|------|------|
| page | number | 否 | 页码 |
| pageSize | number | 否 | 每页数量 |

**响应数据**:

```json
{
  "records": [
    {
      "id": "1",
      "user": {
        "id": "2",
        "username": "lisi",
        "avatar": "https://example.com/avatar.jpg"
      },
      "userId": "2",
      "createdAt": "2024-01-01T00:00:00Z"
    }
  ],
  "total": 100,
  "size": 10,
  "current": 1,
  "pages": 10
}
```

---

### 5. 获取关注列表

**接口地址**: `GET /follow/{userId}/following`

**需要认证**: 否

**路径参数**:

| 参数名 | 类型 | 必填 | 说明 |
|--------|------|------|------|
| userId | string | 是 | 用户ID |

**请求参数**:

| 参数名 | 类型 | 必填 | 说明 |
|--------|------|------|------|
| page | number | 否 | 页码 |
| pageSize | number | 否 | 每页数量 |

**响应数据**:

```json
{
  "records": [
    {
      "id": "1",
      "user": {
        "id": "2",
        "username": "lisi",
        "avatar": "https://example.com/avatar.jpg"
      },
      "userId": "2",
      "createdAt": "2024-01-01T00:00:00Z"
    }
  ],
  "total": 50,
  "size": 10,
  "current": 1,
  "pages": 5
}
```

---

## 分类模块

### 1. 获取分类列表

**接口地址**: `GET /category/list`

**需要认证**: 否

**请求参数**:

| 参数名 | 类型 | 必填 | 说明 |
|--------|------|------|------|
| parentId | string | 否 | 父分类ID |
| includeChildren | boolean | 否 | 是否包含子分类 |

**响应数据**:

```json
[
  {
    "id": "1",
    "name": "技术",
    "slug": "tech",
    "description": "技术类文章",
    "icon": "https://example.com/icon.png",
    "articleCount": 100,
    "sort": 1,
    "parentId": null,
    "children": [
      {
        "id": "2",
        "name": "前端",
        "slug": "frontend",
        "articleCount": 50,
        "parentId": "1"
      }
    ],
    "createdAt": "2024-01-01T00:00:00Z"
  }
]
```

---

### 2. 获取分类详情

**接口地址**: `GET /category/{id}`

**需要认证**: 否

**路径参数**:

| 参数名 | 类型 | 必填 | 说明 |
|--------|------|------|------|
| id | string | 是 | 分类ID |

**响应数据**:

同分类列表中的单个分类对象。

---

## 标签模块

### 1. 获取标签列表

**接口地址**: `GET /tag/list`

**需要认证**: 否

**请求参数**:

| 参数名 | 类型 | 必填 | 说明 |
|--------|------|------|------|
| page | number | 否 | 页码 |
| pageSize | number | 否 | 每页数量 |
| keyword | string | 否 | 搜索关键词 |

**响应数据**:

```json
{
  "records": [
    {
      "id": "1",
      "name": "Vue3",
      "slug": "vue3",
      "articleCount": 50,
      "createdAt": "2024-01-01T00:00:00Z"
    }
  ],
  "total": 100,
  "size": 10,
  "current": 1,
  "pages": 10
}
```

---

### 2. 获取热门标签

**接口地址**: `GET /tag/hot`

**需要认证**: 否

**请求参数**:

| 参数名 | 类型 | 必填 | 说明 |
|--------|------|------|------|
| limit | number | 否 | 数量，默认 20 |

**响应数据**:

同标签列表，但直接返回数组。

---

### 3. 获取标签详情

**接口地址**: `GET /tag/{id}`

**需要认证**: 否

**路径参数**:

| 参数名 | 类型 | 必填 | 说明 |
|--------|------|------|------|
| id | string | 是 | 标签ID |

**响应数据**:

同标签列表中的单个标签对象。

---

### 4. 搜索标签

**接口地址**: `GET /tag/search`

**需要认证**: 否

**请求参数**:

| 参数名 | 类型 | 必填 | 说明 |
|--------|------|------|------|
| keyword | string | 是 | 搜索关键词 |

**响应数据**:

```json
[
  {
    "id": "1",
    "name": "Vue3",
    "slug": "vue3",
    "articleCount": 50,
    "createdAt": "2024-01-01T00:00:00Z"
  }
]
```

---

### 5. 创建新标签

**接口地址**: `POST /tag/create`

**需要认证**: 是

**请求参数**:

| 参数名 | 类型 | 必填 | 说明 |
|--------|------|------|------|
| name | string | 是 | 标签名称 |

**响应数据**:

```json
{
  "id": "1",
  "name": "新标签",
  "slug": "xin-biao-qian",
  "articleCount": 0,
  "createdAt": "2024-01-01T00:00:00Z"
}
```

---

### 6. 获取标签推荐

**接口地址**: `GET /tag/recommend`

**需要认证**: 否

**请求参数**:

| 参数名 | 类型 | 必填 | 说明 |
|--------|------|------|------|
| title | string | 是 | 文章标题 |
| category | string | 否 | 文章分类 |

**响应数据**:

```json
[
  {
    "id": "1",
    "name": "Vue3",
    "slug": "vue3",
    "articleCount": 50,
    "createdAt": "2024-01-01T00:00:00Z"
  }
]
```

---

## 搜索模块

### 1. 搜索

**接口地址**: `GET /search`

**需要认证**: 否

**请求参数**:

| 参数名 | 类型 | 必填 | 说明 |
|--------|------|------|------|
| keyword | string | 是 | 搜索关键词 |
| type | string | 否 | 类型：article/user/all，默认 all |
| page | number | 否 | 页码 |
| pageSize | number | 否 | 每页数量 |

**响应数据**:

```json
{
  "articles": {
    "list": [],
    "total": 100,
    "page": 1,
    "pageSize": 10
  },
  "users": {
    "list": [],
    "total": 50,
    "page": 1,
    "pageSize": 10
  }
}
```

---

## 通知模块

### 1. 获取通知列表

**接口地址**: `GET /notifications`

**需要认证**: 是

**请求参数**:

| 参数名 | 类型 | 必填 | 说明 |
|--------|------|------|------|
| page | number | 否 | 页码 |
| pageSize | number | 否 | 每页数量 |
| type | string | 否 | 类型：comment/like/follow/system/order |
| isRead | boolean | 否 | 是否只看已读 |

**响应数据**:

```json
{
  "records": [
    {
      "id": "1",
      "userId": "1",
      "type": "like",
      "title": "点赞通知",
      "content": "有人点赞了你的文章",
      "data": {
        "articleId": "1",
        "userId": "2"
      },
      "read": false,
      "createdAt": "2024-01-01T00:00:00Z"
    }
  ],
  "total": 50,
  "size": 10,
  "current": 1,
  "pages": 5
}
```

---

### 2. 获取未读数量

**接口地址**: `GET /notifications/unread-count`

**需要认证**: 是

**请求参数**: 无

**响应数据**:

```json
{
  "count": 10
}
```

---

### 3. 标记已读

**接口地址**: `POST /notifications/mark-all-read` 或 `POST /notifications/{id}/read`

**需要认证**: 是

**请求参数**:

| 参数名 | 类型 | 必填 | 说明 |
|--------|------|------|------|
| id | string | 否 | 通知ID（路径参数） |

**请求方式说明**:
- 标记全部已读: `POST /notifications/mark-all-read`
- 标记单个已读: `POST /notifications/{id}/read`

**响应数据**: 无

---

### 4. 删除通知

**接口地址**: `DELETE /notifications/{id}`

**需要认证**: 是

**路径参数**:

| 参数名 | 类型 | 必填 | 说明 |
|--------|------|------|------|
| id | string | 是 | 通知ID |

**响应数据**: 无

---

## 友情链接模块

### 1. 获取友情链接列表

**接口地址**: `GET /friend-links`

**需要认证**: 否

**请求参数**: 无

**响应数据**:

```json
[
  {
    "id": "1",
    "name": "中国作家网",
    "url": "https://www.chinawriter.com.cn",
    "description": "中国作家协会官方网站",
    "logo": "https://example.com/logo.png",
    "sort": 1,
    "status": "active",
    "createdAt": "2024-01-01T00:00:00Z",
    "updatedAt": "2024-01-01T00:00:00Z"
  }
]
```

---

## 订单模块

### 1. 获取订单列表

**接口地址**: `GET /order/list`

**需要认证**: 是

**请求参数**:

| 参数名 | 类型 | 必填 | 说明 |
|--------|------|------|------|
| page | number | 否 | 页码 |
| pageSize | number | 否 | 每页数量 |
| status | string | 否 | 状态：pending/paid/cancelled/refunded |
| type | string | 否 | 类型：vip/recharge/product |

**响应数据**:

```json
{
  "records": [
    {
      "id": "1",
      "userId": "1",
      "orderNo": "ORD202401010001",
      "type": "vip",
      "amount": 99.00,
      "status": "paid",
      "payMethod": "wechat",
      "paidAt": "2024-01-01T00:00:00Z",
      "createdAt": "2024-01-01T00:00:00Z",
      "updatedAt": "2024-01-01T00:00:00Z"
    }
  ],
  "total": 50,
  "size": 10,
  "current": 1,
  "pages": 5
}
```

---

### 2. 创建订单

**接口地址**: `POST /order`

**需要认证**: 是

**请求参数**:

| 参数名 | 类型 | 必填 | 说明 |
|--------|------|------|------|
| type | string | 是 | 类型：vip/recharge/product |
| productId | string | 否 | 商品ID |
| amount | number | 是 | 金额 |

**响应数据**:

同订单列表中的单个订单对象。

---

## VIP模块

### 1. 获取VIP套餐列表

**接口地址**: `GET /vip/package/list`

**需要认证**: 否

**请求参数**:

| 参数名 | 类型 | 必填 | 说明 |
|--------|------|------|------|
| status | string | 否 | 状态：active/inactive，默认 active |

**响应数据**:

```json
[
  {
    "id": "1",
    "name": "月卡",
    "price": 29.00,
    "originalPrice": 39.00,
    "duration": 30,
    "description": "一个月VIP会员",
    "features": ["无限浏览", "专属标识", "优先推荐", "客服优先"],
    "popular": true,
    "sort": 1,
    "status": "active"
  }
]
```

---

## 钱包模块

### 1. 获取钱包信息

**接口地址**: `GET /wallet`

**需要认证**: 是

**请求参数**: 无

**响应数据**:

```json
{
  "id": "1",
  "userId": "1",
  "balance": 100.00,
  "frozenBalance": 0,
  "totalRecharge": 500.00,
  "totalWithdraw": 200.00,
  "createdAt": "2024-01-01T00:00:00Z",
  "updatedAt": "2024-01-02T00:00:00Z"
}
```

---

### 2. 获取钱包交易记录

**接口地址**: `GET /wallet/transaction/list`

**需要认证**: 是

**请求参数**:

| 参数名 | 类型 | 必填 | 说明 |
|--------|------|------|------|
| page | number | 否 | 页码 |
| pageSize | number | 否 | 每页数量 |
| type | string | 否 | 类型：recharge/consume/refund/withdraw |

**响应数据**:

```json
{
  "records": [
    {
      "id": "1",
      "userId": "1",
      "type": "recharge",
      "amount": 100.00,
      "balanceBefore": 50.00,
      "balanceAfter": 150.00,
      "description": "充值",
      "orderId": "1",
      "createdAt": "2024-01-01T00:00:00Z"
    }
  ],
  "total": 50,
  "size": 10,
  "current": 1,
  "pages": 5
}
```

---

## 文件上传

### 1. 上传文件

**接口地址**: `POST /upload/file`

**需要认证**: 是

**请求格式**: multipart/form-data

**请求参数**:

| 参数名 | 类型 | 必填 | 说明 |
|--------|------|------|------|
| file | File | 是 | 文件 |

**响应数据**:

```json
{
  "url": "https://example.com/files/file.pdf",
  "filename": "file.pdf",
  "size": 1024000,
  "mimeType": "application/pdf"
}
```

---

### 2. 上传图片

**接口地址**: `POST /upload/image`

**需要认证**: 是

**请求格式**: multipart/form-data

**请求参数**:

| 参数名 | 类型 | 必填 | 说明 |
|--------|------|------|------|
| file | File | 是 | 图片文件（支持 jpg/png/gif/webp） |

**响应数据**:

```json
{
  "url": "https://example.com/images/image.jpg",
  "filename": "image.jpg",
  "size": 500000,
  "mimeType": "image/jpeg"
}
```

---

## 系统配置

### 1. 获取系统配置

**接口地址**: `GET /config`

**需要认证**: 否

**请求参数**: 无

**响应数据**:

```json
{
  "config": {
    "siteName": "墨韵智库",
    "siteDescription": "网络文学阅读分享平台",
    "siteLogo": "https://example.com/logo.png",
    "siteIcp": "京ICP备xxxxxxxx号-1",
    "contactEmail": "contact@moyun.com",
    "copyright": "Copyright © 2024 墨韵智库"
  }
}
```

---

## 错误处理

所有接口在出错时返回：

```json
{
  "code": 400,
  "message": "错误信息",
  "data": null
}
```

常见错误码：

- `400`: 请求参数错误
- `401`: 未授权/Token无效
- `403`: 没有权限
- `404`: 资源不存在
- `500`: 服务器错误

---

## 附录

### 前端调用示例

已在前端项目中封装好API调用，参考：

- `src/api/client.ts` - HTTP请求客户端
- `src/api/user.ts` - 用户模块API
- `src/api/article.ts` - 文章模块API
- ... 其他模块

TypeScript类型定义在 `src/types/api.ts`。

### 数据模型

完整的数据模型定义参考 `src/types/api.ts`。

---

**文档版本**: v1.2  
**最后更新**: 2026-05-28  
**维护团队**: 墨韵智库后端团队
