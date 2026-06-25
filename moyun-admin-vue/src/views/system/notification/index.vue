<template>
  <div class="app-container">
    <!-- 操作按钮 -->
    <el-row :gutter="10" class="mb8">
      <el-col :span="1.5">
        <el-button type="primary" plain icon="Refresh" @click="getList">刷新</el-button>
      </el-col>
      <el-col :span="1.5">
        <el-button
          type="success"
          plain
          icon="Check"
          :disabled="unreadCount === 0"
          @click="handleMarkAllRead"
        >全部已读</el-button>
      </el-col>
      <el-col :span="1.5" class="unread-tip">
        <el-tag v-if="unreadCount > 0" type="danger">{{ unreadCount }} 条未读</el-tag>
        <el-tag v-else type="info">无未读</el-tag>
      </el-col>
    </el-row>

    <!-- 数据表格 -->
    <el-table v-loading="loading" :data="notificationList">
      <el-table-column label="通知编号" align="center" prop="id" width="80" />
      <el-table-column label="通知标题" align="center" prop="title" :show-overflow-tooltip="true" />
      <el-table-column label="通知内容" align="center" prop="content" min-width="200" :show-overflow-tooltip="true" />
      <el-table-column label="通知范围" align="center" prop="scope" width="100">
        <template #default="scope">
          <el-tag :type="scope.row.scope === 'all' ? 'warning' : 'primary'">
            {{ scope.row.scope === 'all' ? '广播' : '个人' }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column label="通知类型" align="center" prop="type" width="100">
        <template #default="scope">
          <el-tag :type="getTypeTagType(scope.row.type)">{{ getTypeText(scope.row.type) }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column label="已读状态" align="center" prop="isRead" width="100">
        <template #default="scope">
          <el-tag :type="scope.row.isRead ? 'info' : 'danger'">
            {{ scope.row.isRead ? '已读' : '未读' }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column label="创建时间" align="center" prop="createTime" width="160">
        <template #default="scope">
          <span>{{ parseTime(scope.row.createTime) }}</span>
        </template>
      </el-table-column>
      <el-table-column label="操作" align="center" class-name="small-padding fixed-width" width="180">
        <template #default="scope">
          <el-button link type="primary" icon="View" @click="handleView(scope.row)">查看</el-button>
          <el-button
            v-if="!scope.row.isRead"
            link
            type="primary"
            icon="Check"
            @click="handleMarkAsRead(scope.row)"
          >标为已读</el-button>
        </template>
      </el-table-column>
    </el-table>

    <!-- 分页 -->
    <pagination
      v-show="total > 0"
      :total="total"
      v-model:page="queryParams.pageNum"
      v-model:limit="queryParams.pageSize"
      @pagination="getList"
    />

    <!-- 查看通知对话框 -->
    <el-dialog title="通知详情" v-model="viewOpen" width="600px" append-to-body>
      <el-descriptions :column="1" border>
        <el-descriptions-item label="通知编号">{{ viewForm.id }}</el-descriptions-item>
        <el-descriptions-item label="通知范围">
          <el-tag :type="viewForm.scope === 'all' ? 'warning' : 'primary'">
            {{ viewForm.scope === 'all' ? '全局广播' : '个人通知' }}
          </el-tag>
        </el-descriptions-item>
        <el-descriptions-item label="通知标题">{{ viewForm.title }}</el-descriptions-item>
        <el-descriptions-item label="通知类型">
          <el-tag :type="getTypeTagType(viewForm.type)">{{ getTypeText(viewForm.type) }}</el-tag>
        </el-descriptions-item>
        <el-descriptions-item label="通知内容">
          <span>{{ viewForm.content }}</span>
        </el-descriptions-item>
        <el-descriptions-item label="创建时间">{{ parseTime(viewForm.createTime) }}</el-descriptions-item>
      </el-descriptions>
      <template #footer>
        <div class="dialog-footer">
          <el-button @click="viewOpen = false">关 闭</el-button>
        </div>
      </template>
    </el-dialog>
  </div>
</template>

<script setup name="SysNotificationCenter">
import { listMyNotification, getUnreadCount, markAsRead, markAllAsRead, getNotification } from "@/api/system/notification";

const { proxy } = getCurrentInstance();

const notificationList = ref([]);
const viewOpen = ref(false);
const loading = ref(true);
const total = ref(0);
const unreadCount = ref(0);
const viewForm = ref({});

const queryParams = ref({
  pageNum: 1,
  pageSize: 10
});

// 获取通知类型标签颜色
function getTypeTagType(type) {
  const typeMap = {
    'system': 'info',
    'comment': 'primary',
    'like': 'success',
    'follow': 'warning',
    'order': 'danger',
    'notice': 'info',
    'announcement': 'success'
  };
  return typeMap[type] || 'info';
}

// 获取通知类型文本
function getTypeText(type) {
  const textMap = {
    'system': '系统通知',
    'comment': '评论通知',
    'like': '点赞通知',
    'follow': '关注通知',
    'order': '订单通知',
    'notice': '通知',
    'announcement': '公告'
  };
  return textMap[type] || type;
}

// 查询通知列表
function getList() {
  loading.value = true;
  listMyNotification(queryParams.value).then(response => {
    notificationList.value = response.data.records || [];
    total.value = response.data.total || 0;
  }).catch(() => {
    proxy.$modal.msgError("加载通知列表失败");
  }).finally(() => {
    loading.value = false;
  });
}

// 加载未读数
function loadUnreadCount() {
  getUnreadCount().then(response => {
    unreadCount.value = response.data || 0;
  }).catch(() => {
    // 静默失败，不影响主流程
  });
}

// 查看通知详情
function handleView(row) {
  getNotification(row.id).then(response => {
    viewForm.value = response.data;
    viewOpen.value = true;
    // 如果是未读通知，查看时自动标记已读
    if (!row.isRead) {
      markAsRead(row.id).then(() => {
        row.isRead = true;
        loadUnreadCount();
      });
    }
  });
}

// 标记单条已读
function handleMarkAsRead(row) {
  markAsRead(row.id).then(() => {
    proxy.$modal.msgSuccess("已标记为已读");
    row.isRead = true;
    loadUnreadCount();
  });
}

// 标记全部已读
function handleMarkAllRead() {
  proxy.$modal.confirm('确认将所有未读通知标记为已读？').then(() => {
    return markAllAsRead();
  }).then(() => {
    proxy.$modal.msgSuccess("已全部标记为已读");
    getList();
    loadUnreadCount();
  }).catch(() => {});
}

// 初始化
getList();
loadUnreadCount();
</script>

<style scoped>
.unread-tip {
  display: flex;
  align-items: center;
  padding-left: 12px;
}
</style>
