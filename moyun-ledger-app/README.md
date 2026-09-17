# 墨韵记账 - 个人资产管理 App (moyun-ledger-app)

**项目版本**: v11.98
**最后更新**: 2026-09-17

---

## 一、项目简介

**墨韵记账**是旭林知行平台的个人资产管理 App，基于 **Vue 3 + uni-app** 构建，一套代码编译三端（**微信小程序 / H5 / App**），与主站共享后端（moyun-server）与统一钱包账户体系。

核心能力：

- **记一笔**：多类型记账（支出/收入/转账/借款/还款），语义化分类标签与提示，凭证截图
- **资产负债**：账户/资产负债总览，列表一键查关联流水，借款自动建负债
- **报表中心**：多维统计（分类/趋势/账户），CSV 导出
- **预算与提醒**：分类预算、站内预算提醒、新手引导
- **AI 财务分析**：月/季/年维度快照（多版本留存）、四维度分析、风险/建议量化依据（evidence/expectedImpact）
- **个人工具**：备忘录（提醒方式/重要程度）、定时记账、储蓄目标
- **我的**：登录注册、个人设置、意见反馈、VIP 订阅（接入公共支付通道）、赞赏

## 二、技术栈

| 技术 | 版本 | 用途 |
|------|------|------|
| Vue | 3.4.21 | 前端框架 |
| uni-app | 3.0（dcloudio） | 跨端框架（微信小程序/H5/App） |
| Pinia | 2.0.36 | 状态管理 |
| Vite | 5.2.8 | 构建工具 |

## 三、项目结构

```
moyun-ledger-app/
├── src/
│   ├── api/                # 后端接口封装（统一走 moyun-server /api/portal/ledger/**）
│   ├── components/         # 公共组件
│   ├── pages/              # 页面（uni-app pages.json 注册）
│   │   ├── dashboard/      # 首页总览（资产负债/本月收支/快速记账入口）
│   │   ├── record/         # 记一笔（多类型/分类选择/凭证）
│   │   ├── asset/          # 资产（账户/资产列表/关联流水）
│   │   ├── liability/      # 负债（借款/还款）
│   │   ├── report/         # 报表中心（多维统计/CSV 导出）
│   │   ├── analysis/       # AI 财务分析（快照多版本/轮询进度）
│   │   ├── portfolio/      # 资产组合视图
│   │   ├── webview/        # 内嵌页（VIP 订阅/支付等 H5 复用）
│   │   └── mine/           # 我的
│   │       ├── login/ + register/   # 登录/注册
│   │       ├── budget/             # 预算管理
│   │       ├── categories/         # 自定义分类
│   │       ├── memo/               # 备忘录
│   │       ├── savings/            # 储蓄目标
│   │       ├── schedule/          # 定时记账
│   │       ├── tip/               # 赞赏
│   │       ├── vip/               # VIP 订阅
│   │       ├── feedback/          # 意见反馈
│   │       └── settings/          # 设置
│   ├── stores/             # Pinia 状态管理
│   ├── utils/              # 工具函数（金额格式化/日期/请求封装…）
│   ├── static/             # 静态资源
│   ├── App.vue             # 应用入口
│   ├── main.js             # 入口文件
│   ├── pages.json          # 页面路由注册（uni-app）
│   └── manifest.json       # 应用配置（AppID/权限/各端配置）
├── .env.development / .env.production   # 环境变量（API 地址）
├── vite.config.js
└── package.json
```

## 四、快速开始

```bash
# 环境：Node.js 18+
npm install

npm run dev:h5          # H5 端（浏览器直接调试）
npm run dev:mp-weixin   # 微信小程序端（用微信开发者工具导入 dist/dev/mp-weixin）

npm run build:h5         # H5 生产构建
npm run build:mp-weixin  # 小程序生产构建（上传 dist/build/mp-weixin）
```

> 需先启动后端 moyun-server（默认 http://localhost:8080），API 地址在 `.env.development` 中配置。

## 五、开发约定

- **金额单位**：全链路统一 RMB 元（后端 DECIMAL(18,2)，前端不乘除 100）
- **路由**：页面统一在 `src/pages.json` 注册；深层页面跳转使用 uni-app `navigateTo`
- **请求**：统一走 `src/utils` 请求封装（Token 注入/未登录引导/错误提示）
- **主题**：全局主题系统（跟随主站视觉，CSS 变量）

## 六、相关文档

| 文档 | 路径 | 说明 |
|------|------|------|
| 项目总览 | [../README.md](../README.md) | 项目整体介绍 |
| 项目文档索引 | [../docs/README.md](../docs/README.md) | 全量文档索引 |
| 记账方案文档 | [../docs/05-方案设计-分模块](../docs/05-方案设计-分模块) | 记账模块设计（分类体系/AI 分析契约） |
| devlog | [../docs/07-变更日志/devlog.md](../docs/07-变更日志/devlog.md) | 版本变更日志 |

---

**项目维护者**: 旭林知行开发团队
**最后更新**: 2026-09-17
