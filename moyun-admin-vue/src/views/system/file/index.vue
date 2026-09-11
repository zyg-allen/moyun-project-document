<template>
  <div class="app-container">
    <!-- 搜索表单 -->
    <el-form :model="queryParams" ref="queryRef" :inline="true" v-show="showSearch">
      <el-form-item label="文件名称" prop="fileName">
        <el-input
          v-model="queryParams.fileName"
          placeholder="请输入文件名称"
          clearable
          style="width: 240px"
          @keyup.enter="handleQuery"
        />
      </el-form-item>
      <el-form-item label="文件类型" prop="fileType">
        <el-select v-model="queryParams.fileType" placeholder="文件类型" clearable style="width: 160px">
          <el-option label="图片" value="image" />
          <el-option label="文档" value="document" />
          <el-option label="视频" value="video" />
          <el-option label="音频" value="audio" />
          <el-option label="其他" value="other" />
        </el-select>
      </el-form-item>
      <el-form-item>
        <el-button type="primary" icon="Search" @click="handleQuery">搜索</el-button>
        <el-button icon="Refresh" @click="resetQuery">重置</el-button>
      </el-form-item>
    </el-form>

    <!-- 操作按钮 -->
    <el-row :gutter="10" class="mb8">
      <el-col :span="1.5">
        <el-upload action="#" :http-request="handleUpload" :show-file-list="false" accept="*">
          <el-button type="primary" plain icon="Plus" v-hasPermi="['system:file:add']">上传文件</el-button>
        </el-upload>
      </el-col>
      <el-col :span="1.5">
        <el-button
          type="danger"
          plain
          icon="Delete"
          :disabled="multiple"
          @click="handleDelete()"
          v-hasPermi="['system:file:remove']"
        >删除</el-button>
      </el-col>
      <right-toolbar v-model:showSearch="showSearch" @queryTable="getList"></right-toolbar>
    </el-row>

    <!-- 数据表格 -->
    <el-table v-loading="loading" :data="fileList" @selection-change="handleSelectionChange">
      <el-table-column type="selection" width="55" align="center" />
      <el-table-column label="文件ID" prop="id" align="center" width="80" />
      <el-table-column label="文件名称" prop="fileName" align="center" :show-overflow-tooltip="true" />
      <el-table-column label="文件类型" prop="fileType" align="center" width="100">
        <template #default="scope">
          <el-tag v-if="scope.row.fileType === 'image'" type="success" size="small">图片</el-tag>
          <el-tag v-else-if="scope.row.fileType === 'document'" type="info" size="small">文档</el-tag>
          <el-tag v-else-if="scope.row.fileType === 'video'" type="warning" size="small">视频</el-tag>
          <el-tag v-else-if="scope.row.fileType === 'audio'" type="danger" size="small">音频</el-tag>
          <el-tag v-else type="info" size="small">其他</el-tag>
        </template>
      </el-table-column>
      <el-table-column label="文件大小" prop="fileSize" align="center" width="110">
        <template #default="scope">
          {{ formatFileSize(scope.row.fileSize) }}
        </template>
      </el-table-column>
      <el-table-column label="存储类型" prop="storageType" align="center" width="100">
        <template #default="scope">
          <el-tag :type="scope.row.storageType === 'minio' ? 'success' : 'info'" size="small">{{ scope.row.storageType }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column label="上传用户" prop="uploadUserName" align="center" width="120" />
      <el-table-column label="业务类型" prop="businessType" align="center" width="120" />
      <el-table-column label="创建时间" prop="createTime" align="center" width="180">
        <template #default="scope">
          <span>{{ parseTime(scope.row.createTime) }}</span>
        </template>
      </el-table-column>
      <el-table-column label="操作" align="right" width="200" class-name="small-padding fixed-width">
        <template #default="scope">
          <el-button link type="primary" icon="Download" @click="handleDownload(scope.row)">下载</el-button>
          <el-button link type="primary" icon="View" @click="handlePreview(scope.row)">预览</el-button>
          <el-button link type="primary" icon="Delete" @click="handleDelete(scope.row)" v-hasPermi="['system:file:remove']">删除</el-button>
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

    <!-- 图片预览 -->
    <el-image-viewer
      v-if="previewVisible"
      :url-list="[previewUrl]"
      @close="previewVisible = false"
    />
  </div>
</template>

<script setup lang="ts" name="File">
import { ref, reactive, onMounted } from 'vue';
import { ElMessage, ElMessageBox } from 'element-plus';
import { listFile, delFilesByUrl, uploadFile } from '@/api/system/file';

const loading = ref(false);
const showSearch = ref(true);
// 选中数组
const ids = ref<Array<number>>([]);
// 选中的完整行数据（用于按 URL 删除时取 fileUrl）
const selectedRows = ref<any[]>([]);
const single = ref(true);
const multiple = ref(true);
const total = ref(0);
const fileList = ref<any[]>([]);
// 图片预览
const previewVisible = ref(false);
const previewUrl = ref('');

const queryRef = ref();

const queryParams = reactive({
  pageNum: 1,
  pageSize: 10,
  fileName: null as string | null,
  fileType: null as string | null
});

/** 查询文件列表 */
async function getList() {
  loading.value = true;
  try {
    const params: any = {
      pageNum: queryParams.pageNum,
      pageSize: queryParams.pageSize
    };
    if (queryParams.fileName) params.fileName = queryParams.fileName;
    if (queryParams.fileType) params.fileType = queryParams.fileType;
    const res: any = await listFile(params);
    // 兼容三种返回：拦截器已把 Page.records 转为 data.rows
    fileList.value = res.rows || res.data?.rows || res.data?.records || [];
    total.value = res.total ?? res.data?.total ?? 0;
  } catch (error) {
    console.error('加载文件列表失败:', error);
  } finally {
    loading.value = false;
  }
}

/** 格式化文件大小 */
function formatFileSize(size: any) {
  const n = Number(size);
  if (!n) return '0 B';
  const k = 1024;
  const sizes = ['B', 'KB', 'MB', 'GB', 'TB'];
  const i = Math.min(Math.floor(Math.log(n) / Math.log(k)), sizes.length - 1);
  return (n / Math.pow(k, i)).toFixed(2) + ' ' + sizes[i];
}

/** 搜索按钮操作 */
function handleQuery() {
  queryParams.pageNum = 1;
  getList();
}

/** 重置按钮操作 */
function resetQuery() {
  queryRef.value?.resetFields();
  queryParams.fileName = null;
  queryParams.fileType = null;
  handleQuery();
}

/** 多选框选中数据 */
function handleSelectionChange(selection: any[]) {
  selectedRows.value = selection;
  ids.value = selection.map((item: any) => item.id);
  single.value = selection.length !== 1;
  multiple.value = !selection.length;
}

/** 上传按钮操作 */
async function handleUpload(param: any) {
  const formData = new FormData();
  formData.append('file', param.file);
  loading.value = true;
  try {
    await uploadFile(formData);
    ElMessage.success('上传成功');
    getList();
  } catch (error: any) {
    ElMessage.error(error?.message || '上传失败');
  } finally {
    loading.value = false;
  }
}

/** 下载按钮操作 */
function handleDownload(row: any) {
  window.open(row.fileUrl);
}

/** 预览按钮操作 */
function handlePreview(row: any) {
  if (row.fileType === 'image') {
    previewUrl.value = row.fileUrl;
    previewVisible.value = true;
  } else {
    ElMessage.info('当前文件类型不支持预览');
  }
}

/**
 * 删除按钮操作（单行 / 批量）
 * 统一走按 URL 删除（与各附件组件删除/替换路径一致，后端按 fileUrl 查记录→删存储→删DB）
 */
function handleDelete(row?: any) {
  const isSingle = row && row.id;
  const rows = isSingle ? [row] : selectedRows.value;
  const fileUrls = rows.map((r: any) => r.fileUrl).filter(Boolean);
  if (!fileUrls.length) {
    ElMessage.warning('请选择要删除的文件');
    return;
  }
  const tip = isSingle
    ? '是否确认删除该文件？删除后将同时清除存储与记录，且无法恢复。'
    : '是否确认删除选中的 ' + fileUrls.length + ' 个文件？删除后将同时清除存储与记录，且无法恢复。';
  ElMessageBox.confirm(tip, '警告', {
    confirmButtonText: '确定',
    cancelButtonText: '取消',
    type: 'warning'
  }).then(async () => {
    try {
      // 并行删除，任一失败不阻断其余
      const results = await delFilesByUrl(fileUrls);
      const failed = Array.isArray(results) ? results.filter((r: any) => !r.ok) : [];
      getList();
      if (failed.length > 0) {
        ElMessage.warning('删除完成，其中 ' + failed.length + ' 个文件清理失败，请稍后重试');
      } else {
        ElMessage.success('删除成功');
      }
    } catch (error: any) {
      ElMessage.error(error?.message || '删除失败');
    }
  }).catch(() => {});
}

onMounted(() => {
  getList();
});
</script>
