# 功能测试问题清单

## BUG-001: [首页]

- **模块**：  前台首页-获取热门标签列表接口 http://localhost:8080/portal/tag/hot?limit=20
- **严重程度**： 🟡 一般
- **复现步骤**：
  1. 打开 首页页面
  2. 点击热门标签更多 按钮
- **预期结果**：应该 引用次数最多的top 20
- **实际结果**：返回空
- sql: select * from portal_tag WHERE (reference_count is null or reference_count > 0) order by reference_count desc, sort asc, id asc limit 20
  
  问题描述和要求：reference_count引用次数统计没有实现，现在是作为几个模块公共标签，请在使用的模块实现新增修改删除等方法中加入绑定和解绑的动作，并追加或减少这个字段，注意要做好线程安全
  
  


