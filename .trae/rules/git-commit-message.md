---
alwaysApply: true
scene: git_message
---
提交信息规范：

1. **格式**：`<type>(<scope>): <subject>`，如 `feat(auth): 添加登录功能`

2. **type 类型**：
   - `feat`: 新功能
   - `fix`: 修复 bug
   - `docs`: 文档更新
   - `style`: 代码格式（不影响功能）
   - `refactor`: 重构
   - `perf`: 性能优化
   - `test`: 测试相关
   - `chore`: 构建/工具变更

3. **subject 规则**：
   - 使用中文，简洁明了
   - 不超过 50 个字符
   - 不以句号结尾
   - 描述做了什么，而非怎么做的

4. **body 部分**（可选）：
   - 详细说明变更原因和内容
   - 每行不超过 200 个字符

5. **footer 部分**（可选）：
   - 关联 issue：`Closes #123`
   - 其他重要说明
