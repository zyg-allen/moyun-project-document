<template>
  <div class="query-templates">
    <el-dialog
      v-model="visible"
      title="📚 查询模板库"
      width="800px"
      :close-on-click-modal="false">
      
      <div class="templates-container">
        <!-- 分类标签 -->
        <el-tabs v-model="activeCategory" type="border-card">
          <el-tab-pane
            v-for="category in categories"
            :key="category.name"
            :label="category.label"
            :name="category.name">
            
            <div class="templates-list">
              <el-card
                v-for="template in category.templates"
                :key="template.name"
                class="template-card"
                shadow="hover"
                @click="selectTemplate(template)">
                
                <div class="template-header">
                  <span class="template-icon">{{ template.icon }}</span>
                  <span class="template-name">{{ template.name }}</span>
                  <el-tag size="small" :type="template.complexity === 'simple' ? 'success' : 'warning'">
                    {{ template.complexity === 'simple' ? '简单' : '复杂' }}
                  </el-tag>
                </div>
                
                <div class="template-description">
                  {{ template.description }}
                </div>
                
                <div class="template-sql">
                  <el-text size="small" type="info">查询示例：</el-text>
                  <pre>{{ template.queryExample }}</pre>
                </div>
                
                <div class="template-actions">
                  <el-button size="small" type="primary" @click.stop="useTemplate(template)">
                    使用此模板
                  </el-button>
                  <el-button size="small" @click.stop="previewSQL(template)">
                    查看SQL
                  </el-button>
                </div>
              </el-card>
            </div>
          </el-tab-pane>
        </el-tabs>
      </div>
      
      <template #footer>
        <el-button @click="visible = false">关闭</el-button>
      </template>
    </el-dialog>
    
    <!-- SQL预览对话框 -->
    <el-dialog
      v-model="showSQLPreview"
      title="SQL预览"
      width="600px">
      <pre class="sql-preview">{{ previewSQLContent }}</pre>
      <template #footer>
        <el-button type="primary" @click="copySQLToClipboard">复制SQL</el-button>
        <el-button @click="showSQLPreview = false">关闭</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, defineEmits, defineExpose } from 'vue'
import { ElMessage } from 'element-plus'

const emit = defineEmits(['use-template'])

const visible = ref(false)
const activeCategory = ref('user')
const showSQLPreview = ref(false)
const previewSQLContent = ref('')

// 查询模板分类
const categories = ref([
  {
    name: 'user',
    label: '👥 用户分析',
    templates: [
      {
        name: '用户总数统计',
        icon: '📊',
        description: '统计系统中的用户总数',
        queryExample: '用户总数是多少',
        complexity: 'simple',
        sql: 'SELECT COUNT(*) as total FROM users'
      },
      {
        name: '用户增长趋势',
        icon: '📈',
        description: '查看每天的用户注册趋势',
        queryExample: '分析用户注册趋势',
        complexity: 'simple',
        sql: 'SELECT DATE(created_at) as date, COUNT(*) as count FROM users GROUP BY DATE(created_at) ORDER BY date'
      },
      {
        name: '用户活跃分析',
        icon: '🔥',
        description: '统计最近7天的活跃用户数',
        queryExample: '最近7天活跃用户有多少',
        complexity: 'simple',
        sql: 'SELECT COUNT(DISTINCT user_id) as active_users FROM user_activities WHERE activity_date >= DATE_SUB(NOW(), INTERVAL 7 DAY)'
      },
      {
        name: '用户流失分析',
        icon: '⚠️',
        description: '找出30天未活跃的用户',
        queryExample: '查找流失用户',
        complexity: 'complex',
        sql: 'SELECT * FROM users WHERE id NOT IN (SELECT DISTINCT user_id FROM user_activities WHERE activity_date >= DATE_SUB(NOW(), INTERVAL 30 DAY))'
      },
      {
        name: '用户地区分布',
        icon: '🌍',
        description: '统计各地区的用户数量',
        queryExample: '用户地区分布情况',
        complexity: 'simple',
        sql: 'SELECT region, COUNT(*) as count FROM users GROUP BY region ORDER BY count DESC'
      }
    ]
  },
  {
    name: 'order',
    label: '📦 订单分析',
    templates: [
      {
        name: '订单量统计',
        icon: '📊',
        description: '统计总订单数量',
        queryExample: '订单总数是多少',
        complexity: 'simple',
        sql: 'SELECT COUNT(*) as total_orders FROM orders'
      },
      {
        name: '销售额趋势',
        icon: '💰',
        description: '查看每天的销售额变化',
        queryExample: '最近30天销售额趋势',
        complexity: 'simple',
        sql: 'SELECT DATE(order_date) as date, SUM(amount) as total_amount FROM orders WHERE order_date >= DATE_SUB(NOW(), INTERVAL 30 DAY) GROUP BY DATE(order_date) ORDER BY date'
      },
      {
        name: 'TOP10商品',
        icon: '🏆',
        description: '销量最高的10个商品',
        queryExample: '销量前10的商品',
        complexity: 'simple',
        sql: 'SELECT product_name, COUNT(*) as order_count, SUM(amount) as total_sales FROM orders GROUP BY product_name ORDER BY order_count DESC LIMIT 10'
      },
      {
        name: '客单价分析',
        icon: '💵',
        description: '计算平均客单价',
        queryExample: '平均客单价是多少',
        complexity: 'simple',
        sql: 'SELECT AVG(amount) as avg_order_amount, MIN(amount) as min_amount, MAX(amount) as max_amount FROM orders'
      },
      {
        name: '订单状态分布',
        icon: '📋',
        description: '统计各状态的订单数量',
        queryExample: '订单状态分布情况',
        complexity: 'simple',
        sql: 'SELECT status, COUNT(*) as count FROM orders GROUP BY status'
      },
      {
        name: '本月vs上月对比',
        icon: '📊',
        description: '对比本月和上月的订单量和销售额',
        queryExample: '对比本月和上月的销售情况',
        complexity: 'complex',
        sql: 'SELECT SUM(CASE WHEN MONTH(order_date) = MONTH(NOW()) THEN 1 ELSE 0 END) as this_month_orders, SUM(CASE WHEN MONTH(order_date) = MONTH(NOW())-1 THEN 1 ELSE 0 END) as last_month_orders, SUM(CASE WHEN MONTH(order_date) = MONTH(NOW()) THEN amount ELSE 0 END) as this_month_sales, SUM(CASE WHEN MONTH(order_date) = MONTH(NOW())-1 THEN amount ELSE 0 END) as last_month_sales FROM orders WHERE YEAR(order_date) = YEAR(NOW())'
      }
    ]
  },
  {
    name: 'finance',
    label: '💰 财务分析',
    templates: [
      {
        name: '收入统计',
        icon: '💵',
        description: '统计总收入金额',
        queryExample: '总收入是多少',
        complexity: 'simple',
        sql: 'SELECT SUM(amount) as total_revenue FROM transactions WHERE type = "income"'
      },
      {
        name: '成本分析',
        icon: '📉',
        description: '统计总成本',
        queryExample: '总成本是多少',
        complexity: 'simple',
        sql: 'SELECT SUM(amount) as total_cost FROM transactions WHERE type = "expense"'
      },
      {
        name: '利润趋势',
        icon: '📈',
        description: '查看每月的利润变化',
        queryExample: '每月利润趋势',
        complexity: 'complex',
        sql: 'SELECT YEAR(date) as year, MONTH(date) as month, SUM(CASE WHEN type="income" THEN amount ELSE -amount END) as profit FROM transactions GROUP BY YEAR(date), MONTH(date) ORDER BY year, month'
      },
      {
        name: '收支平衡分析',
        icon: '⚖️',
        description: '对比收入和支出',
        queryExample: '收支平衡情况',
        complexity: 'simple',
        sql: 'SELECT SUM(CASE WHEN type="income" THEN amount ELSE 0 END) as total_income, SUM(CASE WHEN type="expense" THEN amount ELSE 0 END) as total_expense, SUM(CASE WHEN type="income" THEN amount ELSE -amount END) as balance FROM transactions'
      }
    ]
  },
  {
    name: 'product',
    label: '📦 商品分析',
    templates: [
      {
        name: '商品总数',
        icon: '📊',
        description: '统计商品总数',
        queryExample: '商品总数是多少',
        complexity: 'simple',
        sql: 'SELECT COUNT(*) as total_products FROM products'
      },
      {
        name: '库存预警',
        icon: '⚠️',
        description: '查找库存不足的商品',
        queryExample: '库存不足的商品',
        complexity: 'simple',
        sql: 'SELECT * FROM products WHERE stock < 10 ORDER BY stock'
      },
      {
        name: '价格分布',
        icon: '💰',
        description: '分析商品价格分布',
        queryExample: '商品价格分布情况',
        complexity: 'simple',
        sql: 'SELECT CASE WHEN price < 50 THEN "0-50" WHEN price < 100 THEN "50-100" WHEN price < 200 THEN "100-200" ELSE "200+" END as price_range, COUNT(*) as count FROM products GROUP BY price_range'
      },
      {
        name: '畅销商品',
        icon: '🔥',
        description: '找出销量最好的商品',
        queryExample: '销量最好的商品',
        complexity: 'complex',
        sql: 'SELECT p.name, p.price, COUNT(o.id) as order_count, SUM(o.amount) as total_sales FROM products p LEFT JOIN orders o ON p.id = o.product_id GROUP BY p.id ORDER BY order_count DESC LIMIT 20'
      }
    ]
  }
])

// 打开模板库
const open = () => {
  visible.value = true
}

// 选择模板
const selectTemplate = (template) => {
  console.log('选中模板:', template.name)
}

// 使用模板
const useTemplate = (template) => {
  emit('use-template', template.queryExample)
  visible.value = false
  ElMessage.success(`已应用模板: ${template.name}`)
}

// 预览SQL
const previewSQL = (template) => {
  previewSQLContent.value = template.sql
  showSQLPreview.value = true
}

// 复制SQL到剪贴板
const copySQLToClipboard = () => {
  navigator.clipboard.writeText(previewSQLContent.value).then(() => {
    ElMessage.success('SQL已复制到剪贴板')
  }).catch(() => {
    ElMessage.error('复制失败')
  })
}

// 暴露方法给父组件
defineExpose({
  open
})
</script>

<style scoped>
.query-templates {
  /* 样式容器 */
}

.templates-container {
  max-height: 600px;
  overflow-y: auto;
}

.templates-list {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(350px, 1fr));
  gap: 15px;
  padding: 10px;
}

.template-card {
  cursor: pointer;
  transition: all 0.3s ease;
  height: 100%;
}

.template-card:hover {
  transform: translateY(-5px);
  box-shadow: 0 8px 24px rgba(102, 126, 234, 0.2);
}

.template-header {
  display: flex;
  align-items: center;
  gap: 10px;
  margin-bottom: 12px;
}

.template-icon {
  font-size: 24px;
}

.template-name {
  flex: 1;
  font-size: 16px;
  font-weight: 600;
  color: #1e293b;
}

.template-description {
  font-size: 14px;
  color: #64748b;
  margin-bottom: 12px;
  line-height: 1.6;
}

.template-sql {
  background: #f8fafc;
  padding: 10px;
  border-radius: 8px;
  margin-bottom: 12px;
}

.template-sql pre {
  margin: 8px 0 0 0;
  font-size: 12px;
  color: #475569;
  white-space: pre-wrap;
  word-break: break-all;
}

.template-actions {
  display: flex;
  gap: 8px;
  justify-content: flex-end;
}

.sql-preview {
  background: #1e293b;
  color: #10b981;
  padding: 20px;
  border-radius: 8px;
  font-size: 14px;
  line-height: 1.6;
  overflow-x: auto;
  margin: 0;
}

:deep(.el-tabs__content) {
  padding: 0;
}

:deep(.el-tabs--border-card) {
  border: none;
  box-shadow: none;
}
</style>
