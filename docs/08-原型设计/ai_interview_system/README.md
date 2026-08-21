# AI 智能面试系统 - 前端原型

> 统一红色主题 (#DC2626)，纯静态 HTML 演示页面，可直接浏览器打开运行。

## 页面清单

| 文件 | 模块 | 说明 |
|------|------|------|
| `ai_voice_interview_demo.html` | AI 语音面试 | 完整面试流程：配置面试 → 实时语音问答 → 智能追问 → 多维评估报告 |
| `resume_optimizer_page.html` | 简历优化系统 | 双模式：简历维护（表单编辑）+ AI 智能优化（一键润色 + PDF 导出） |
| `choice_question_page.html` | 选择题做题 | 计算机基础选择题库，即时判定 + 详细题解 + 答题卡导航 + 成绩分析 |
| `code_question_page.html` | 编程题做题 | 仿 LeetCode 风格，多语言编辑器 + 语法预检查 + 沙箱执行 + 提交记录 |
| `nav_redesign_proposal.html` | 前台导航重构方案 | 7→6 个一级栏目重组 + Mega Menu 下拉 + 手机端 5 Tab 设计 |
| `admin_redesign_proposal.html` | 后台管理优化方案 | 菜单从代码模块导向改为业务运营导向，7 个一级菜单 |

## Vue 开发规格文档

| 文件 | 说明 |
|------|------|
| `vue_interview_spec.html` | 面试模块 Vue 技术规格（详细版） |
| `vue_interview_spec_simple.md` | 面试模块 Vue 技术规格（精简版） |
| `vue_resume_spec.md` | 简历模块 Vue 技术规格 |

## 设计规范

- **主色**: #DC2626 / #EF4444 / #B91C1C
- **字体**: -apple-system, BlinkMacSystemFont, PingFang SC, Microsoft YaHei
- **图标**: Font Awesome 6.4.0 CDN
- **按钮反馈**: scale(0.97) 按压动效
- **代码编辑器**: 暗色主题 #1E1E2E
- **圆角体系**: 6px / 10px / 14px / 20px

## 技术说明

- 所有页面为独立 HTML 文件，零依赖可直接打开
- 交互逻辑使用原生 JavaScript 实现
- 数据为前端 Mock 数据，接入后端 API 即可替换
- 支持响应式布局（桌面 + 平板 + 手机）
